package com.rohon.client.controller;

import com.rohon.client.websocket.WebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
public class WebSocketTestController {

    @Autowired
    private WebSocketClient webSocketClient;

    @PostMapping("/send")
    public String sendMsg2Server(String msg) {
        webSocketClient.send(msg);
        return "success";
    }

    @PostMapping("/send")
    public String sendFile(MultipartFile file) {
//        file
//        webSocketClient.send(msg);
        return "success";
    }

}
