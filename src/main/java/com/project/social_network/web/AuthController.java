package com.project.social_network.web;

import com.project.social_network.entity.User;
import com.project.social_network.entity.enums.EStatus;
import com.project.social_network.payload.request.LoginRequest;
import com.project.social_network.payload.request.SignupRequest;
import com.project.social_network.payload.responce.JWTTokenSuccessResponse;
import com.project.social_network.payload.responce.MessageResponse;
import com.project.social_network.security.JWTTokenProvider;
import com.project.social_network.security.SecurityConstants;
import com.project.social_network.services.UserService;
import com.project.social_network.validations.ResponseErrorValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin
@Controller
@RequestMapping("/auth")
@PreAuthorize("permitAll()")
public class AuthController {

    public static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final ResponseErrorValidation responseErrorValidation;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JWTTokenProvider jwtTokenProvider;

    @Autowired
    public AuthController(ResponseErrorValidation responseErrorValidation,
                          UserService userService,
                          AuthenticationManager authenticationManager,
                          JWTTokenProvider jwtTokenProvider) {
        this.responseErrorValidation = responseErrorValidation;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/register")
    public String getRegistrationPage() {
        return "SignUp";
    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "SignIn";
    }

    @PostMapping("/sign-in")
    public ResponseEntity<Object> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                                   BindingResult bindingResult)  {
        ResponseEntity<Object> errors = responseErrorValidation.mapValidationService(bindingResult);
        if (!ObjectUtils.isEmpty(errors)) {
            LOG.error("Errors in authorization");
            return errors;
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                ));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = SecurityConstants.TOKEN_PREFIX + jwtTokenProvider.generateToken(authentication);
        LOG.info("User Authorization");

        return ResponseEntity.ok(new JWTTokenSuccessResponse(true, jwt));
    }

    @PostMapping("/signup")
    public ResponseEntity<Object> registerUser(@Valid @RequestBody SignupRequest signupRequest,
                                               BindingResult bindingResult) {
        ResponseEntity<Object> errors = responseErrorValidation.mapValidationService(bindingResult);
        if (!ObjectUtils.isEmpty(errors)) {
            LOG.error("Errors in registration");
            return ResponseEntity.ok(new MessageResponse("User already registered"));
        }

        userService.saveUser(signupRequest);
        LOG.info("User Registration");

        return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }
}
