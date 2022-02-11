package com.rohon.server.websocket;

import cn.hutool.core.util.StrUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketServer {

    private final int port;

    private final WebSocketServerHandler webSocketServerHandler;

    private Channel serverChannel;
 
    public WebSocketServer(int port) {
        this.port = port;
        webSocketServerHandler = new WebSocketServerHandler();
    }
 
    public void start() throws Exception {
        // 线程组
        EventLoopGroup parentGroup = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            //标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            //Netty4使用对象池，重用缓冲区
            bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            //启用心跳保活机制
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
//            //禁止使用Nagle算法，便于小数据即时传输
//            bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
            bootstrap.group(parentGroup, childGroup) // 绑定线程池
                    .channel(NioServerSocketChannel.class) // 指定使用的channel
                    .localAddress(this.port)// 绑定监听端口
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 绑定客户端连接时候触发操作

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            log.info("收到新连接！");
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.TRACE));
                            // websocket协议本身是基于http协议的，所以这边也要使用http解编码器
                            // HttpRequestDecoder和HttpResponseEncoder的一个组合，针对http协议进行编解码
                            ch.pipeline().addLast(new HttpServerCodec());
                            // 以块的方式来写的处理器，防止大文件导致内存溢出 channel.write(new ChunkedFile(new File("bigFile.mkv")))
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            // 将HttpMessage和HttpContents聚合到一个完成的 FullHttpRequest或FullHttpResponse中,具体是FullHttpRequest对象还是FullHttpResponse对象取决于是请求还是响应
                            // 需要放到HttpServerCodec这个处理器后面
                            ch.pipeline().addLast(new HttpObjectAggregator(10240));
                            // webSocket 数据压缩扩展，当添加这个的时候WebSocketServerProtocolHandler的第三个参数需要设置成true
                            ch.pipeline().addLast(new WebSocketServerCompressionHandler());
                            // 聚合 websocket 的数据帧，因为客户端可能分段向服务器端发送数据
                            // https://github.com/netty/netty/issues/1112 https://github.com/netty/netty/pull/1207
                            ch.pipeline().addLast(new WebSocketFrameAggregator(10 * 1024 * 1024));
                            //1. 将http协议升级为websocket保持长连接，自动处理了 websocket 握手以及 Close、Ping、Pong等frame，对于文本和二进制的数据帧需要我们自己处理
                            //2. 设置心跳超时10000ms(默认值)
                            //3. 对于传递比较大的文件，需要修改 maxFrameSize 参数
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws", null, true, 65536 * 10, 10000L));
                            //三个参数分别为读/写/读写的空闲，我们只针对读写空闲检测
                            //当IdleStateEvent 触发后 就会传递给管道的下一个handler去处理 通过回调触发下一个handler的userEventTriggered ,在该方法中去处理Event(读/写空闲)
                            ch.pipeline().addLast(new IdleStateHandler(2, 4, 60));
                            //自定义的handler 处理业务逻辑
                            ch.pipeline().addLast(webSocketServerHandler);
                        }
                    });
            ChannelFuture cf = bootstrap.bind().sync(); // 服务器异步创建绑定
            cf.addListener(f -> {
                if (f.isSuccess()) {
                    log.info("ws-server启动成功，port：{}", port);
                } else {
                    log.error("ws-server启动失败！");
                }
            });
            serverChannel = cf.channel();
            serverChannel.closeFuture().sync(); // 关闭服务器通道
        } catch (Exception e) {
            log.error("ws-server 启动主方法异常！{}", e.getMessage(), e);
        } finally {
            parentGroup.shutdownGracefully().sync(); // 释放线程池资源
            childGroup.shutdownGracefully().sync();
            log.info("ws-server 启动主方法正常结束！");
        }
    }

    /**
     * 正常关闭
     */
    public void close() {
        log.info("ws-server 调用关闭方法");
        sendMsgByAddr("ws-server 即将关闭！", null);
        serverChannel.close();
    }


    /**
     * 发送消息
     * @param msg 消息内容
     */
    public void sendMsgByAddr(final String msg, String clientAddress) {
        ChannelGroup channelGroup = WebSocketServerHandler.channelGroup;
        // 如果uri为null则广播消息
        if (clientAddress == null) {
            channelGroup.writeAndFlush(new TextWebSocketFrame(msg));
        } else {
            channelGroup.forEach(i -> {
                if (i.remoteAddress().toString().equals(clientAddress)) {
                    i.writeAndFlush(new TextWebSocketFrame(msg));
                }
            });
        }

    }

    public void sendMsgByChannel(String msg, Channel channel) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(msg));
        }
    }


    public void sendByServiceName(String msg, String serviceName) {
        if (StrUtil.isBlank(serviceName)) {
            log.error("serviceName 不允许为空！");
            return;
        }
        if (!WebSocketServerHandler.serviceMap.containsKey(serviceName)) {
            log.error("不存在该serviceName！");
        } else {
            Channel channel = WebSocketServerHandler.serviceMap.get(serviceName);
            channel.writeAndFlush(new TextWebSocketFrame(msg));
        }
    }

    public String getServiceList() {
        return WebSocketServerHandler.serviceMap.keySet().toString();
    }

}