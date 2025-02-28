/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.saga.engine.strategy.impl;

import java.util.List;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.statelang.domain.StateType;
import org.apache.seata.saga.engine.pcext.utils.CompensationHolder;
import org.apache.seata.saga.engine.strategy.StatusDecisionStrategy;
import org.apache.seata.saga.engine.utils.ExceptionUtils;
import org.apache.seata.saga.engine.utils.ExceptionUtils.NetExceptionType;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default state machine execution status decision strategy
 *
 * @see StatusDecisionStrategy
 */
public class DefaultStatusDecisionStrategy implements StatusDecisionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStatusDecisionStrategy.class);

    /**
     * decide machine compensate status
     *
     * @param stateMachineInstance
     * @param compensationHolder
     */
    public static void decideMachineCompensateStatus(StateMachineInstance stateMachineInstance,
                                                     CompensationHolder compensationHolder) {
        if (stateMachineInstance.getStatus() == null || ExecutionStatus.RU.equals(stateMachineInstance.getStatus())) {

            stateMachineInstance.setStatus(ExecutionStatus.UN);
        }
        if (!compensationHolder.getStateStackNeedCompensation().isEmpty()) {

            boolean hasCompensateSUorUN = false;
            for (StateInstance forCompensateState : compensationHolder.getStatesForCompensation().values()) {
                if (ExecutionStatus.UN.equals(forCompensateState.getStatus()) || ExecutionStatus.SU.equals(
                    forCompensateState.getStatus())) {
                    hasCompensateSUorUN = true;
                    break;
                }
            }
            if (hasCompensateSUorUN) {
                stateMachineInstance.setCompensationStatus(ExecutionStatus.UN);
            } else {
                stateMachineInstance.setCompensationStatus(ExecutionStatus.FA);
            }
        } else {

            boolean hasCompensateError = false;
            for (StateInstance forCompensateState : compensationHolder.getStatesForCompensation().values()) {
                if (!ExecutionStatus.SU.equals(forCompensateState.getStatus())) {
                    hasCompensateError = true;
                    break;
                }

            }
            if (hasCompensateError) {
                stateMachineInstance.setCompensationStatus(ExecutionStatus.UN);
            } else {
                stateMachineInstance.setCompensationStatus(ExecutionStatus.SU);
            }
        }
    }

    /**
     * set machine status based on state list
     *
     * @param stateMachineInstance the state machine instance
     * @param stateList the state instance list
     */
    public static void setMachineStatusBasedOnStateListAndException(StateMachineInstance stateMachineInstance,
                                                                    List<StateInstance> stateList, Exception exp) {
        boolean hasSetStatus = false;
        boolean hasSuccessUpdateService = false;
        if (CollectionUtils.isNotEmpty(stateList)) {
            boolean hasUnsuccessService = false;

            for (int i = stateList.size() - 1; i >= 0; i--) {
                StateInstance stateInstance = stateList.get(i);

                if (stateInstance.isIgnoreStatus() || stateInstance.isForCompensation()) {
                    continue;
                }
                if (ExecutionStatus.UN.equals(stateInstance.getStatus())) {
                    stateMachineInstance.setStatus(ExecutionStatus.UN);
                    hasSetStatus = true;
                } else if (ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    if (StateType.SERVICE_TASK.equals(stateInstance.getType())) {
                        if (stateInstance.isForUpdate() && !stateInstance.isForCompensation()) {
                            hasSuccessUpdateService = true;
                        }
                    }
                } else if (ExecutionStatus.SK.equals(stateInstance.getStatus())) {
                    // ignore
                } else {
                    hasUnsuccessService = true;
                }
            }

            if (!hasSetStatus && hasUnsuccessService) {
                if (hasSuccessUpdateService) {
                    stateMachineInstance.setStatus(ExecutionStatus.UN);
                } else {
                    stateMachineInstance.setStatus(ExecutionStatus.FA);
                }
                hasSetStatus = true;
            }
        }

        if (!hasSetStatus) {
            setMachineStatusBasedOnException(stateMachineInstance, exp, hasSuccessUpdateService);
        }
    }

    /**
     * set machine status based on net exception
     *
     * @param stateMachineInstance
     * @param exp
     */
    public static void setMachineStatusBasedOnException(StateMachineInstance stateMachineInstance, Exception exp,
                                                        boolean hasSuccessUpdateService) {
        if (exp == null) {
            stateMachineInstance.setStatus(ExecutionStatus.SU);
        } else if (exp instanceof EngineExecutionException
                && FrameworkErrorCode.StateMachineExecutionTimeout.equals(((EngineExecutionException)exp).getErrcode())) {
            stateMachineInstance.setStatus(ExecutionStatus.UN);
        } else if (hasSuccessUpdateService) {
            stateMachineInstance.setStatus(ExecutionStatus.UN);
        } else {
            NetExceptionType t = ExceptionUtils.getNetExceptionType(exp);
            if (t.equals(NetExceptionType.CONNECT_EXCEPTION) || t.equals(NetExceptionType.CONNECT_TIMEOUT_EXCEPTION)
                || t.equals(NetExceptionType.NOT_NET_EXCEPTION)) {
                stateMachineInstance.setStatus(ExecutionStatus.FA);
            } else if (t.equals(NetExceptionType.READ_TIMEOUT_EXCEPTION)) {
                stateMachineInstance.setStatus(ExecutionStatus.UN);
            }
        }
    }

    @Override
    public void decideOnEndState(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {

        if (ExecutionStatus.RU.equals(stateMachineInstance.getCompensationStatus())) {

            CompensationHolder compensationHolder = CompensationHolder.getCurrent(context, true);
            decideMachineCompensateStatus(stateMachineInstance, compensationHolder);
        } else {
            Object failEndStateFlag = context.getVariable(DomainConstants.VAR_NAME_FAIL_END_STATE_FLAG);
            boolean isComeFromFailEndState = failEndStateFlag != null && (Boolean)failEndStateFlag;
            decideMachineForwardExecutionStatus(stateMachineInstance, exp, isComeFromFailEndState);
        }

        if (stateMachineInstance.getCompensationStatus() != null && DomainConstants.OPERATION_NAME_FORWARD.equals(
            context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME)) && ExecutionStatus.SU.equals(
            stateMachineInstance.getStatus())) {

            stateMachineInstance.setCompensationStatus(ExecutionStatus.FA);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "StateMachine Instance[id:{},name:{}] execute finish with status[{}], compensation status [{}].",
                stateMachineInstance.getId(), stateMachineInstance.getStateMachine().getName(),
                stateMachineInstance.getStatus(), stateMachineInstance.getCompensationStatus());
        }
    }

    @Override
    public void decideOnTaskStateFail(ProcessContext context, StateMachineInstance stateMachineInstance,
                                      Exception exp) {
        if (!decideMachineForwardExecutionStatus(stateMachineInstance, exp, true)) {

            stateMachineInstance.setCompensationStatus(ExecutionStatus.UN);
        }
    }

    /**
     * Determine the forward execution state of the state machine
     *
     * @param stateMachineInstance the state machine instance
     * @param exp the exception
     * @param specialPolicy the special policy
     * @return the boolean
     */
    @Override
    public boolean decideMachineForwardExecutionStatus(StateMachineInstance stateMachineInstance, Exception exp,
                                                       boolean specialPolicy) {
        boolean result = false;

        if (stateMachineInstance.getStatus() == null || ExecutionStatus.RU.equals(stateMachineInstance.getStatus())) {
            result = true;

            List<StateInstance> stateList = stateMachineInstance.getStateList();

            setMachineStatusBasedOnStateListAndException(stateMachineInstance, stateList, exp);

            if (specialPolicy && ExecutionStatus.SU.equals(stateMachineInstance.getStatus())) {
                for (StateInstance stateInstance : stateMachineInstance.getStateList()) {
                    if (!stateInstance.isIgnoreStatus() && (stateInstance.isForUpdate() || stateInstance
                        .isForCompensation())) {
                        stateMachineInstance.setStatus(ExecutionStatus.UN);
                        break;
                    }
                }
                if (ExecutionStatus.SU.equals(stateMachineInstance.getStatus())) {
                    stateMachineInstance.setStatus(ExecutionStatus.FA);
                }
            }
        }
        return result;

    }
}
