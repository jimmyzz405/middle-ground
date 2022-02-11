package com.rohon.server.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebSocketAuthRequest {

    private String serviceName;

    private String token;
}
