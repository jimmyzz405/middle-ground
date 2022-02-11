package com.rohon.client.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class WebSocketClient {

    private final URI uri;
    private Channel channel;
    private static final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final WebSocketClientHandler handler = new WebSocketClientHandler();



    public WebSocketClient(String uri) {
        this.uri = URI.create(uri);
    }


    public void run() throws InterruptedException {
        bootstrap.channel(NioSocketChannel.class)
                .group(workerGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .remoteAddress(uri.getHost(), uri.getPort())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LoggingHandler(LogLevel.TRACE));
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new HttpObjectAggregator(10240));
                        pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);
                        pipeline.addLast(new WebSocketFrameAggregator(10 * 1024 * 1024));
                        // 这里的handShaker会在原有的握手逻辑上新增请求头，用于后续的认证和服务注册等
                        WebSocketClientHandshaker handShaker = getHandShaker(uri, WebSocketVersion.V13, null, true, null, 65535);
                        pipeline.addLast(new WebSocketClientProtocolHandler(handShaker,10000L));
                        pipeline.addLast(handler);
                    }
                });
        this.channel = bootstrap.connect().sync().channel();
        this.channel.closeFuture().sync();
//        ChannelFuture future = handler.handShakerFuture();//handShakerFuture用于等待握手结果，标识握手是否成功
//        future.sync();//这里处理同步等待，一直等到握手成功，调用setSuccess()方法才会结束，终止等待状态
    }


    /**
     * 正常关闭
     *
     * @throws InterruptedException
     */
    public void close() throws InterruptedException {
        log.info("client bean 销毁！");
        send("client-1 即将断开连接！");
        this.channel.closeFuture().sync();//等待调用close()方法
        workerGroup.shutdownGracefully();
    }

    /**
     * 发送消息
     *
     * @param text 消息内容
     */
    public void send(final String text) {
//        if (this.handler.handShakerFuture().isSuccess()) {
//            this.channel.writeAndFlush(new TextWebSocketFrame(text));
//        } else {
//            System.out.println("没有握手成功！");
//        }

        this.channel.writeAndFlush(new TextWebSocketFrame(text));
    }

//    public void send(File file) {
//        if (this.handler.handShakerFuture().isSuccess()) {
//            this.channel.writeAndFlush(new TextWebSocketFrame(text));
//        } else {
//            System.out.println("没有握手成功！");
//        }
//    }

    public boolean isActive() {
        return channel.isActive();
    }

    public ChannelFuture doConnect() throws InterruptedException {
        ChannelFuture future = bootstrap.connect(uri.getHost(), uri.getPort());
        this.channel = future.sync().channel();
        future.sync();
        return future;
    }

    /**
     * 获取WebSocketClientHandshaker
     * @param webSocketURL
     * @param version
     * @param subprotocol
     * @param allowExtensions
     * @param customHeaders
     * @param maxFramePayloadLength
     * @return
     */
    private WebSocketClientHandshaker getHandShaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength) {
        return new WebSocketClientHandShaker(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
    }

    public static Bootstrap getBootstrap() {
        return bootstrap;
    }


}