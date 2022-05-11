package com.project.social_network.dto;

import lombok.Data;

import java.util.Set;

@Data
public class PostDTO {

    private Long id;
    private String caption;
    private String username;
    private Integer likes;
    private Set<String> usersLiked;
}
