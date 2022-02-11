package com.rohon.client.websocket;

import com.rohon.client.utils.YamlUtil;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class WebSocketClientHandShaker extends WebSocketClientHandshaker13 {

    public WebSocketClientHandShaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength) {
        super(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
    }

    public WebSocketClientHandShaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch) {
        super(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, performMasking, allowMaskMismatch);
    }

    public WebSocketClientHandShaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength, boolean performMasking, boolean allowMaskMismatch, long forceCloseTimeoutMillis) {
        super(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength, performMasking, allowMaskMismatch, forceCloseTimeoutMillis);
    }

    @Override
    protected FullHttpRequest newHandshakeRequest() {
        log.info("client 握手发起！");
        FullHttpRequest httpRequest = super.newHandshakeRequest();
        httpRequest.headers()
                .set(Headers.TOKEN.getHeader(), YamlUtil.getStr("ws.server.token"))
                .set(Headers.SERVICE_NAME.getHeader(), YamlUtil.getStr("ws.server.service-name"));
        return httpRequest;
    }
}
