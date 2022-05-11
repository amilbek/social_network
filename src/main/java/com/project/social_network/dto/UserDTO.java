package com.project.social_network.dto;

import com.project.social_network.annotations.ValidEmail;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class UserDTO {

    private Long id;
    @NotEmpty
    private String firstname;
    @NotEmpty
    private String lastname;
    @ValidEmail
    private String email;
    private String bio;
}
