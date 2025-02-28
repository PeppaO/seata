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
package org.apache.seata.server.console.service;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.console.vo.BranchSessionVO;
import org.apache.seata.common.result.PageResult;

/**
 * Branch session service
 */
public interface BranchSessionService {

    /**
     * Query branch session by xid
     * @param xid the xid
     * @return the BranchSessionVO list
     */
    PageResult<BranchSessionVO> queryByXid(String xid);

    /**
     * Delete branch transaction retry
     *
     * @param xid      the global transaction
     * @param branchId the branch transaction
     * @return SingleResult<Void>
     */
    SingleResult<Void> deleteBranchSession(String xid, String branchId);

    /**
     * Force delete branch transaction retry
     *
     * @param xid      the global transaction
     * @param branchId the branch transaction
     * @return SingleResult<Void>
     */
    SingleResult<Void> forceDeleteBranchSession(String xid, String branchId);

    /**
     * Start branch transaction retry
     *
     * @param xid      the global transaction
     * @param branchId the branch transaction
     * @return SingleResult<Void>
     */
    SingleResult<Void> stopBranchRetry(String xid, String branchId);

    /**
     * Delete branch transaction
     *
     * @param xid      the global transaction
     * @param branchId the branch transaction
     * @return SingleResult<Void>
     */
    SingleResult<Void> startBranchRetry(String xid, String branchId);
}
