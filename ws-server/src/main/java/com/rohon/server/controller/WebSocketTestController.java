package com.rohon.server.controller;

import com.rohon.server.websocket.WebSocketServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Api(tags = {"websocket测试"})
public class WebSocketTestController {

//    @Autowired
//    private WebSocketServer webSocketServer;
//
//    @ApiOperation("向客户端发送消息")
//    @PostMapping("/send")
//    public String sendMsg(@RequestParam(required = false) String clientAddr,
//                          @RequestParam(required = false) String serviceName,
//                          @RequestParam String msg){
//        if (clientAddr != null) {
//            webSocketServer.sendMsgByAddr(msg, clientAddr);
//        } else {
//            webSocketServer.sendByServiceName(msg, serviceName);
//        }
//        return "success";
//    }
//
//    @ApiOperation("拉取服务列表")
//    @GetMapping("/service/list/get")
//    public String getServiceList() {
//        return webSocketServer.getServiceList();
//    }
}
