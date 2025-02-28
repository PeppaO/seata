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

import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.console.param.GlobalLockParam;
import org.apache.seata.server.console.vo.GlobalLockVO;


/**
 * Global lock service
 */
public interface GlobalLockService {

    /**
     * Query locks by param
     * @param param the param
     * @return the list of GlobalLockVO
     */
    PageResult<GlobalLockVO> query(GlobalLockParam param);

    /**
     * Delete lock by xid and branchId
     *
     * @param param param
     * @return SingleResult<Void>
     */
    SingleResult<Void> deleteLock(GlobalLockParam param);

    /**
     * Check if the lock exist the branch session
     *
     * @param xid      xid
     * @param branchId branchId
     * @return True-exist   False-not exist the branch session
     */
    SingleResult<Boolean> check(String xid, String branchId);
}
