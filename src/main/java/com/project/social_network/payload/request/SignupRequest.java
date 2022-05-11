package com.project.social_network.payload.request;

import com.project.social_network.annotations.PasswordMatches;
import com.project.social_network.annotations.ValidEmail;
import com.project.social_network.annotations.ValidPassword;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
@PasswordMatches
public class SignupRequest {

    @NotBlank(message = "User email is required")
    @ValidEmail
    private String email;
    @NotEmpty(message = "Please enter your name")
    private String firstname;
    @NotEmpty(message = "Please enter your surname")
    private String lastname;
    @NotEmpty(message = "Please enter your username")
    private String username;
    @NotEmpty(message = "Password is required")
    @ValidPassword
    private String password;
    private String confirmPassword;

}
