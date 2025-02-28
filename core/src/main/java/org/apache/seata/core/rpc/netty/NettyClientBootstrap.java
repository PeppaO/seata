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
package org.apache.seata.core.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.PlatformDependent;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.protocol.Protocol;
import org.apache.seata.core.rpc.RemotingBootstrap;
import org.apache.seata.core.rpc.netty.grpc.GrpcDecoder;
import org.apache.seata.core.rpc.netty.grpc.GrpcEncoder;
import org.apache.seata.core.rpc.netty.v1.ProtocolDecoderV1;
import org.apache.seata.core.rpc.netty.v1.ProtocolEncoderV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rpc client.
 *
 */
public class NettyClientBootstrap implements RemotingBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClientBootstrap.class);
    private final NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroupWorker;
    private EventExecutorGroup defaultEventExecutorGroup;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final String THREAD_PREFIX_SPLIT_CHAR = "_";
    private final NettyPoolKey.TransactionRole transactionRole;
    private ChannelHandler[] channelHandlers;

    public NettyClientBootstrap(NettyClientConfig nettyClientConfig, final EventExecutorGroup eventExecutorGroup,
                                NettyPoolKey.TransactionRole transactionRole) {
        if (nettyClientConfig == null) {
            nettyClientConfig = new NettyClientConfig();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("use default netty client config.");
            }
        }
        this.nettyClientConfig = nettyClientConfig;
        int selectorThreadSizeThreadSize = this.nettyClientConfig.getClientSelectorThreadSize();
        this.transactionRole = transactionRole;
        if (NettyServerConfig.enableEpoll()) {
            this.eventLoopGroupWorker = new EpollEventLoopGroup(selectorThreadSizeThreadSize,
                new NamedThreadFactory(getThreadPrefix(this.nettyClientConfig.getClientSelectorThreadPrefix()),
                    selectorThreadSizeThreadSize));
        } else {
            this.eventLoopGroupWorker = new NioEventLoopGroup(selectorThreadSizeThreadSize,
                new NamedThreadFactory(getThreadPrefix(this.nettyClientConfig.getClientSelectorThreadPrefix()),
                    selectorThreadSizeThreadSize));
        }
        this.defaultEventExecutorGroup = eventExecutorGroup;
    }

    /**
     * Sets channel handlers.
     *
     * @param handlers the handlers
     */
    protected void setChannelHandlers(final ChannelHandler... handlers) {
        if (handlers != null) {
            channelHandlers = handlers;
        }
    }

    /**
     * Add channel pipeline last.
     *
     * @param channel  the channel
     * @param handlers the handlers
     */
    private void addChannelPipelineLast(Channel channel, ChannelHandler... handlers) {
        if (channel != null && handlers != null) {
            channel.pipeline().addLast(handlers);
        }
    }

    @Override
    public void start() {
        if (this.defaultEventExecutorGroup == null) {
            this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(),
                new NamedThreadFactory(getThreadPrefix(nettyClientConfig.getClientWorkerThreadPrefix()),
                    nettyClientConfig.getClientWorkerThreads()));
        }
        this.bootstrap.group(this.eventLoopGroupWorker).channel(
            nettyClientConfig.getClientChannelClazz()).option(
            ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true).option(
            ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis()).option(
            ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize()).option(ChannelOption.SO_RCVBUF,
            nettyClientConfig.getClientSocketRcvBufSize());

        if (nettyClientConfig.enableNative()) {
            if (PlatformDependent.isOsx()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("client run on macOS");
                }
            } else {
                bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED)
                    .option(EpollChannelOption.TCP_QUICKACK, true);
            }
        }

        bootstrap.handler(
            new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    if (nettyClientConfig.getProtocol().equals(Protocol.GRPC.value)) {
                        pipeline.addLast(Http2FrameCodecBuilder.forClient().build())
                                .addLast(new Http2MultiplexHandler(new ChannelDuplexHandler()));
                    } else {
                        pipeline.addLast(new IdleStateHandler(nettyClientConfig.getChannelMaxReadIdleSeconds(),
                                nettyClientConfig.getChannelMaxWriteIdleSeconds(),
                                nettyClientConfig.getChannelMaxAllIdleSeconds()));
                        pipeline.addLast(new ProtocolDecoderV1())
                                .addLast(new ProtocolEncoderV1());
                        if (channelHandlers != null) {
                            addChannelPipelineLast(ch, channelHandlers);
                        }
                    }
                }
            });

        if (initialized.compareAndSet(false, true) && LOGGER.isInfoEnabled()) {
            LOGGER.info("NettyClientBootstrap has started");
        }
    }

    @Override
    public void shutdown() {
        try {
            this.eventLoopGroupWorker.shutdownGracefully();
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception exx) {
            LOGGER.error("Failed to shutdown: {}", exx.getMessage());
        }
    }

    /**
     * Gets new channel.
     *
     * @param address the address
     * @return the new channel
     */
    public Channel getNewChannel(InetSocketAddress address) {
        Channel channel;
        ChannelFuture f = this.bootstrap.connect(address);
        try {
            f.await(this.nettyClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (f.isCancelled()) {
                throw new FrameworkException(f.cause(), "connect cancelled, can not connect to services-server.");
            } else if (!f.isSuccess()) {
                throw new FrameworkException(f.cause(), "connect failed, can not connect to services-server.");
            } else {
                channel = f.channel();
            }

            if (nettyClientConfig.getProtocol().equals(Protocol.GRPC.value)) {
                Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(channel);
                bootstrap.handler(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void handlerAdded(ChannelHandlerContext ctx) {
                        Channel channel = ctx.channel();
                        channel.pipeline().addLast(new IdleStateHandler(nettyClientConfig.getChannelMaxReadIdleSeconds(),
                                nettyClientConfig.getChannelMaxWriteIdleSeconds(),
                                nettyClientConfig.getChannelMaxAllIdleSeconds()));
                        channel.pipeline().addLast(new GrpcDecoder());
                        channel.pipeline().addLast(new GrpcEncoder());
                        if (channelHandlers != null) {
                            addChannelPipelineLast(channel, channelHandlers);
                        }
                    }
                });
                channel = bootstrap.open().get();
            }

        } catch (Exception e) {
            throw new FrameworkException(e, "can not connect to services-server.");
        }

        return channel;
    }

    /**
     * Gets thread prefix.
     *
     * @param threadPrefix the thread prefix
     * @return the thread prefix
     */
    private String getThreadPrefix(String threadPrefix) {
        return threadPrefix + THREAD_PREFIX_SPLIT_CHAR + transactionRole.name();
    }
}
