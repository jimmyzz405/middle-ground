package com.rohon.client.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.net.ProtocolException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    // 与server的信道
    private Channel channel;


    /**
     * 处理器加入到处理pipeline后，新建握手等待标识Future
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("准备好处理事件");
    }

    /**
     * 连接建立成功后，发起握手请求
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channel = ctx.channel();

        heartBeat();
        log.info("连接成功！" + ctx.name());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("client-1 连接断开！");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("异常处理器捕获异常！", cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        final Channel channel = ctx.channel();
        if (msg instanceof FullHttpResponse) {
            log.info("收到http请求");
            FullHttpResponse response = (FullHttpResponse) msg;
            if (!response.decoderResult().isSuccess()) {
                throw new ProtocolException("响应内容解析失败！");
            }
        }
//        final WebSocketFrame frame = (WebSocketFrame) msg;
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame text = (TextWebSocketFrame) msg;
            log.info("client收到消息：{}", text);

        } else if (msg instanceof BinaryWebSocketFrame) {

            log.info("收到二进制消息");
        }
        // 后面两种frame基本不需要我们自己处理，因为上一层的webscoketHandler都处理过了
//        else if (msg instanceof CloseWebSocketFrame) {
//            channel.close();
//        } else if (msg instanceof PongWebSocketFrame) {
//            log.info(((PongWebSocketFrame) msg).toString());
//        }
    }

    private void heartBeat() {
        Runnable runnable  = () -> {
            log.info("+++++心跳发送任务执行+++++");
            if (channel.isActive()) {
                log.info("sending heart beat to the server...");
                channel.writeAndFlush(new PingWebSocketFrame());
            } else {
                log.error("The connection had broken, cancel the task that will send a heart beat.");
                channel.closeFuture();
                throw new RuntimeException();
            }
        };
        // 延迟2秒立即执行任务，后续每30秒执行一次
        ScheduledFuture<?> scheduledFuture = channel.eventLoop().scheduleAtFixedRate(runnable
        , 2,30, TimeUnit.SECONDS);
        scheduledFuture.addListener((future) -> {
            if (future.isSuccess()) {
                log.info("future success..");
            }
        });
    }
}