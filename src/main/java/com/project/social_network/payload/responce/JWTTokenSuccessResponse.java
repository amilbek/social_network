package com.project.social_network.payload.responce;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JWTTokenSuccessResponse {

    private boolean success;
    private String token;
}
