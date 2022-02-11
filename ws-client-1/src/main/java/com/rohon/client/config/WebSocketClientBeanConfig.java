package com.rohon.client.config;

import com.rohon.client.utils.YamlUtil;
import com.rohon.client.websocket.WebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class WebSocketClientBeanConfig {

    @Bean(destroyMethod = "close")
    public WebSocketClient webSocketClient(){
        String serverURL = YamlUtil.getStr("ws.server.url");
        WebSocketClient webSocketClient;
        try {
            webSocketClient = new WebSocketClient(serverURL);
            webSocketClient.run();
            return webSocketClient;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
