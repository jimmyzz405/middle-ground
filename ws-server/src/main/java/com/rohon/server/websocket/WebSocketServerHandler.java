package com.rohon.server.websocket;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.rohon.server.request.WebSocketAuthRequest;
import com.rohon.server.util.YamlUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理 / 接收 / 响应客户端 websocket 请求的核心业务处理类
 * 注意：@Sharable注解使该handler能被多个channel共享，必须保证线程安全！
 */
@Slf4j
@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    // 服务名与channel的关系映射
    public static Map<String, Channel> serviceMap = new ConcurrentHashMap<>();

    // 当组中的channel关闭时会自动移除
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public WebSocketServerHandler() {
    }


    /**
     * 读取客户端消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        messageReceived(channelHandlerContext, o);

//        if (o != null) {
//            String className = o.getClass().getName();
//            log.info("客户端[{}]消息类型：{}", channelHandlerContext.channel().remoteAddress(), className);
//            if (o instanceof TextWebSocketFrame) {
//                TextWebSocketFrame frame = (TextWebSocketFrame) o;
//                log.info("收到客户端[{}]发来消息：{}", channelHandlerContext.channel().remoteAddress(), frame.text());
//            }
//            if (o instanceof PingWebSocketFrame) {
//                log.info("收到来自[{}]的ping包", channelHandlerContext.channel().remoteAddress());
//                channelHandlerContext.writeAndFlush(new PongWebSocketFrame());
//                new BinaryWebSocketFrame();
//            }
//        } else {
//            log.info("[{}]解析消息为空", channelHandlerContext.channel().remoteAddress());
//        }

    }

    /**
     * 只要有客户端Channel与服务端连接成功就会执行这个方法
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive, channelId: {}", ctx.channel().id().asLongText());
//        channelGroup.forEach(i -> log.info("add前channelGroup中的ip：{}", i.remoteAddress().toString()));
        channelGroup.add(ctx.channel());
        channelGroup.forEach(i -> log.info("add后channelGroup中的ip：{}", i.remoteAddress().toString()));
        log.info("与客户端[{}]的连接开启", ctx.channel().remoteAddress());
    }



    /**
     * 客户端与服务端断开连接的时候调用
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.remove(ctx.channel());
        log.info("与客户端[{}]的连接关闭", ctx.channel().remoteAddress());
    }

    /**
     * 服务端接收客户端发送过来的数据结束之后调用
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        ctx.writeAndFlush(new TextWebSocketFrame("[server]：已读！"));
    }

    /**
     * 工程出现异常的时候调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server进程发生异常！{}",cause.getMessage());
        ctx.close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.info("handlerAdded, channelId: {}", ctx.channel().id().asLongText());

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        //判断evt是否是IdleStateEvent(用于触发用户事件，包含读空闲/写空闲/读写空闲)
        if(evt instanceof IdleStateEvent){
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if(idleStateEvent.state() == IdleState.READER_IDLE){
                log.info("读空闲...");
            }else if(idleStateEvent.state() == IdleState.WRITER_IDLE){
                log.info("写空闲...");
            }else if(idleStateEvent.state() == IdleState.ALL_IDLE){
                log.info("读写空闲！");
            }
        }

        // 如果server与client握手成功，则触发下面的逻辑
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            log.info("触发HANDSHAKE_COMPLETE事件！");
            String token = handshakeComplete.requestHeaders().get(Headers.TOKEN.getHeader());
            String serviceName = handshakeComplete.requestHeaders().get(Headers.SERVICE_NAME.getHeader());
            log.info("client 请求携带的token:{}", token);
            if (token == null) {
                log.info("token不存在，握手失败！");
                ctx.close();
            } else {
                // token校验
                if (token.equals(YamlUtil.getStr("ws.server.token"))) {
                    log.info("token校验成功！");
                    //todo 认证成功的处理逻辑
                    if (serviceName != null) {
                        if (!serviceMap.containsKey(serviceName)) {
                            serviceMap.put(serviceName, ctx.channel());
                            log.info("成功注册service：{}", serviceName);
                        } else {
                            log.info("service：[{}] 已注册！", serviceName);
                            ctx.close();
                        }
                    } else {
                        log.info("未获取到serviceName！");
                        ctx.close();
                    }
                } else {
                    log.info("token校验失败！");
                    ctx.close();
                }
            }
        }
    }

    /**
     * 服务端处理客户端 websocket 请求的核心方法
     */
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        // 传统http接入 第一次需要使用http建立握手
        if (o instanceof FullHttpRequest) {
            // 处理客户端向服务端发起 http 握手请求的业务
            FullHttpRequest fullHttpRequest = (FullHttpRequest) o;
            log.info("FullHttpRequest： {}", fullHttpRequest);
//            handHttpRequest(channelHandlerContext, fullHttpRequest);
            // WebSocket接入
        } else if (o instanceof WebSocketFrame) {
            // 处理 websocket 连接业务
            handleWebSocketFrame(channelHandlerContext, (WebSocketFrame) o);
        }
    }

    /**
     * 处理客户端向服务端发起 http 握手请求的业务  * * @param ctx  * @param request
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.getDecoderResult().isSuccess() || !("websocket").equals(request.headers().get("Upgrade"))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        log.info("HTTP Request received!");
        ByteBuf content = request.content();
        int length = request.content().readableBytes();
        final byte[] array = new byte[length];
        content.getBytes(content.readerIndex(), array, 0, length);
        log.info("uri: {}", request.uri());
    }

    /**
     * 服务端向客户端响应消息  * * @param ctx  * @param request  * @param response
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, DefaultFullHttpResponse response) {
        if (response.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
        }
        // 服务端向客户端发送数据
        ChannelFuture channelFuture = ctx.channel().writeAndFlush(response);
        if (response.getStatus().code() != 200) {
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }



    private void handleWebSocketFrame(ChannelHandlerContext channelHandlerContext, WebSocketFrame frame)  {
        Channel channel = channelHandlerContext.channel();
        // 判断是否是关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            log.info("关闭与客户端[{}]连接", channel.remoteAddress());
            return;
        }
        // 判断是否是ping消息
        if (frame instanceof PingWebSocketFrame) {
            log.info("Ping消息");
            channel.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 纯文本消息
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            log.info(" 接收到客户端的消息: {}", text);
            channel.write(new TextWebSocketFrame(new Date() + " 服务器将你发的消息原样返回：" + text));
        }
        // 二进制消息
        if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) frame;
            ByteBuf content = binaryWebSocketFrame.content();
            log.info("二进制数据: {}", content);
            int length = content.readableBytes();
            byte[] array = new byte[length];
            content.getBytes(content.readerIndex(), array, 0, length);

        }
        // endregion
    }




}