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
package org.apache.seata.saga.engine.utils;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;

/**
 * Exception Utils
 *
 */
public class ExceptionUtils {

    public static final String CONNECT_TIMED_OUT                     = "connect timed out";
    public static final String CONNECT_TIME_OUT_EXCEPTION_CLASS_NAME = "ConnectTimeoutException";
    public static final String READ_TIME_OUT_EXCEPTION_CLASS_NAME    = "ReadTimeoutException";
    public static final String CONNECT_EXCEPTION_CLASS_NAME          = "ConnectException";
    public static final int    MAX_CAUSE_DEP                         = 20;

    public static EngineExecutionException createEngineExecutionException(Exception e, FrameworkErrorCode code,
                                                                          String message,
                                                                          StateMachineInstance stateMachineInstance,
                                                                          StateInstance stateInstance) {
        EngineExecutionException exception = new EngineExecutionException(e, message, code);
        if (stateMachineInstance != null) {
            exception.setStateMachineName(stateMachineInstance.getStateMachine().getAppName());
            exception.setStateMachineInstanceId(stateMachineInstance.getId());
            if (stateInstance != null) {
                exception.setStateName(stateInstance.getName());
                exception.setStateInstanceId(stateInstance.getId());
            }
        }
        return exception;
    }

    public static EngineExecutionException createEngineExecutionException(FrameworkErrorCode code, String message,
                                                                          StateMachineInstance stateMachineInstance,
                                                                          StateInstance stateInstance) {

        return createEngineExecutionException(null, code, message, stateMachineInstance, stateInstance);
    }

    public static EngineExecutionException createEngineExecutionException(Exception e, FrameworkErrorCode code,
                                                                          String message,
                                                                          StateMachineInstance stateMachineInstance,
                                                                          String stateName) {
        EngineExecutionException exception = new EngineExecutionException(e, message, code);
        if (stateMachineInstance != null) {
            exception.setStateMachineName(stateMachineInstance.getStateMachine().getAppName());
            exception.setStateMachineInstanceId(stateMachineInstance.getId());
            exception.setStateName(stateName);
        }
        return exception;
    }

    /**
     * getNetExceptionType
     *
     * @param throwable the throwable
     * @return the net exception type
     */
    public static NetExceptionType getNetExceptionType(Throwable throwable) {

        Throwable currentCause = throwable;

        int dep = MAX_CAUSE_DEP;

        while (currentCause != null && dep > 0) {

            if (currentCause instanceof java.net.SocketTimeoutException) {
                if (CONNECT_TIMED_OUT.equals(currentCause.getMessage())) {
                    return NetExceptionType.CONNECT_TIMEOUT_EXCEPTION;
                } else {
                    return NetExceptionType.READ_TIMEOUT_EXCEPTION;
                }
            } else if (currentCause instanceof java.net.ConnectException) {
                return NetExceptionType.CONNECT_EXCEPTION;
            } else if (currentCause.getClass().getSimpleName().contains(CONNECT_TIME_OUT_EXCEPTION_CLASS_NAME)) {
                return NetExceptionType.CONNECT_TIMEOUT_EXCEPTION;
            } else if (currentCause.getClass().getSimpleName().contains(READ_TIME_OUT_EXCEPTION_CLASS_NAME)) {
                return NetExceptionType.READ_TIMEOUT_EXCEPTION;
            } else if (currentCause.getClass().getSimpleName().contains(CONNECT_EXCEPTION_CLASS_NAME)) {
                return NetExceptionType.CONNECT_EXCEPTION;
            } else {
                Throwable parentCause = currentCause.getCause();
                if (parentCause == null || parentCause == currentCause) {
                    break;
                }
                currentCause = parentCause;
                dep--;
            }
        }
        return NetExceptionType.NOT_NET_EXCEPTION;
    }

    public enum NetExceptionType {
        /**
         * Exception occurred while creating connection
         */
        CONNECT_EXCEPTION,
        /**
         * create connection timeout
         */
        CONNECT_TIMEOUT_EXCEPTION,
        /**
         * read timeout from remote(request has sent)
         */
        READ_TIMEOUT_EXCEPTION,
        /**
         * not a network exception
         */
        NOT_NET_EXCEPTION
    }

    /**
     * Determine if the it is network exception
     * @param throwable the throwable
     * @return the boolean
     */
    public static boolean isNetException(Throwable throwable) {
        NetExceptionType netExceptionType = getNetExceptionType(throwable);
        return netExceptionType != NetExceptionType.NOT_NET_EXCEPTION;
    }
}
