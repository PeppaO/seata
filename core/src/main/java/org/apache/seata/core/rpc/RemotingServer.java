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
package org.apache.seata.core.rpc;

import io.netty.channel.Channel;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.apache.seata.core.protocol.MessageType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * The interface Remoting server.
 *
 * @since 1.3.0
 */
public interface RemotingServer {

    /**
     * server send sync request.
     *
     * @param resourceId rm client resourceId
     * @param clientId   rm client id
     * @param msg        transaction message {@code org.apache.seata.core.protocol}
     * @param tryOtherApp try other app
     * @return client result message
     * @throws TimeoutException TimeoutException
     */
    Object sendSyncRequest(String resourceId, String clientId, Object msg, boolean tryOtherApp) throws TimeoutException, IOException;

    /**
     * server send sync request.
     *
     * @param channel client channel
     * @param msg     transaction message {@code org.apache.seata.core.protocol}
     * @return client result message
     * @throws TimeoutException TimeoutException
     */
    Object sendSyncRequest(Channel channel, Object msg) throws TimeoutException, IOException;

    /**
     * server send async request.
     *
     * @param channel client channel
     * @param msg     transaction message {@code org.apache.seata.core.protocol}
     */
    void sendAsyncRequest(Channel channel, Object msg) throws IOException;

    /**
     * server send async response.
     *
     * @param rpcMessage rpc message from client request
     * @param channel    client channel
     * @param msg        transaction message {@code org.apache.seata.core.protocol}
     */
    void sendAsyncResponse(RpcMessage rpcMessage, Channel channel, Object msg);

    /**
     * register processor
     *
     * @param messageType {@link MessageType}
     * @param processor   {@link RemotingProcessor}
     * @param executor    thread pool
     */
    void registerProcessor(final int messageType, final RemotingProcessor processor, final ExecutorService executor);

}
