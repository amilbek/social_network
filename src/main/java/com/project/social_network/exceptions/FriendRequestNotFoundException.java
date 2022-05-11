package com.project.social_network.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FriendRequestNotFoundException extends RuntimeException {

    public FriendRequestNotFoundException(String message) {
        super(message);
    }
}
