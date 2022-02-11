package com.rohon.server.config;

import com.rohon.server.websocket.WebSocketServer;
import com.rohon.server.util.YamlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNullApi;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Configuration
@Slf4j
public class WebSocketServerBeanConfig {

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

//    @Bean(destroyMethod = "close")
//    public WebSocketServer nettyServer() {
//        Integer port = YamlUtil.getInt("ws.server.port");
//        WebSocketServer webSocketServer = new WebSocketServer(port);
//        // 将WebSocketServer放到线程池中异步执行，防止与tomcat冲突
//        ListenableFuture<?> listenableFuture = threadPoolTaskExecutor.submitListenable(() -> {
//            try {
//                webSocketServer.start();
//            } catch (Exception e) {
//                log.error("提交WebSocketServer.start任务 failed！");
//            }
//        });
//        listenableFuture.addCallback(new ListenableFutureCallback<Object>() {
//            @Override
//            public void onFailure(Throwable throwable) {
//                log.error("server线程执行出错！", throwable);
//            }
//
//            @Override
//            public void onSuccess(Object o) {
//                log.info("server线程执行完毕!");
//            }
//        });
//        return webSocketServer;
//    }
}
