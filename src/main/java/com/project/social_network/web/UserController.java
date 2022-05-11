package com.project.social_network.web;

import com.project.social_network.dto.UserDTO;
import com.project.social_network.entity.User;
import com.project.social_network.facade.UserFacade;
import com.project.social_network.services.UserService;
import com.project.social_network.validations.ResponseErrorValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/users")
public class UserController {

    public static final Logger LOG = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    private final UserFacade userFacade;
    private final ResponseErrorValidation responseErrorValidation;

    @Autowired
    public UserController(UserService userService,
                          UserFacade userFacade,
                          ResponseErrorValidation responseErrorValidation) {
        this.userService = userService;
        this.userFacade = userFacade;
        this.responseErrorValidation = responseErrorValidation;
    }

    @GetMapping("/")
    public ResponseEntity<UserDTO> getCurrentUser(Principal principal) {
        User user = userService.getCurrentUser(principal);
        UserDTO userDTO = userFacade.userToUserDTO(user);

        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<Object> updateUser(@Valid @RequestBody UserDTO userDTO,
                                             BindingResult bindingResult, Principal principal) {
        ResponseEntity<Object> errors = responseErrorValidation.mapValidationService(bindingResult);
        if (!ObjectUtils.isEmpty(errors)) {
            LOG.error("Errors during updating User Information {}", userDTO.getEmail());
            return errors;
        }

        User user = userService.updateUser(userDTO, principal);
        UserDTO updatedUser = userFacade.userToUserDTO(user);

        LOG.info("Updating User {}", userService.getCurrentUser(principal).getUsername());
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @PostMapping("/open-account")
    public ResponseEntity<Object> openAccount(Principal principal) {
        userService.openAccount(principal);
        LOG.info("Make Visible for everyone User {}", userService.getCurrentUser(principal));
        return new ResponseEntity<>("Account " + userService.getCurrentUser(principal).getUsername()
                + " is visible for everyone", HttpStatus.OK);
    }

    @PostMapping("/close-account")
    public ResponseEntity<Object> closeAccount(Principal principal) {
        userService.closeAccount(principal);
        LOG.info("Make Visible only for friends User {}", userService.getCurrentUser(principal));
        return new ResponseEntity<>("Account " + userService.getCurrentUser(principal).getUsername()
                + " is visible only for friends", HttpStatus.OK);
    }

    @PostMapping("/delete")
    public ResponseEntity<Object> deleteAccount(Principal principal) {
        userService.inactiveAccount(principal);
        LOG.info("Deleting User {}", userService.getCurrentUser(principal).getUsername());
        return new ResponseEntity<>("Account is deleted", HttpStatus.OK);
    }

    @GetMapping("/{username}")
    public ResponseEntity<Object> getUser(@PathVariable(value = "username") String username,
                                          Principal principal) {
        User user = userService.getUser(username, principal);
        if (user.equals(userService.getCurrentUser(principal))) {
            UserDTO currentUserDTO = userFacade.userToUserDTO(userService.getCurrentUser(principal));
            return new ResponseEntity<>(currentUserDTO, HttpStatus.NOT_ACCEPTABLE);
        }

        UserDTO userDTO = userFacade.userToUserDTO(user);
        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }

    @PostMapping("/{username}/send-request")
    public ResponseEntity<Object> sendFriendRequest(@PathVariable(value = "username") String username,
                                                    Principal principal) {
        User friend = userService.sendFriendRequest(username, principal);
        return new ResponseEntity<>("User " + userService.getCurrentUser(principal).getUsername() +
                " sent friend request to user " + friend.getUsername(), HttpStatus.OK);
    }

    @PostMapping("/{username}/accept-request")
    public ResponseEntity<Object> acceptFriendRequest(@PathVariable(value = "username") String username,
                                                    Principal principal) {
        User friend = userService.acceptFriendRequest(username, principal);
        return new ResponseEntity<>("User " + userService.getCurrentUser(principal).getUsername() +
                " accepted friend request from user " + friend.getUsername(),  HttpStatus.OK);
    }

    @GetMapping("/all-friends")
    public ResponseEntity<Object> getAllFriendByCurrentUser(Principal principal) {
        List<UserDTO> userDTOList = userService.getFriendsByCurrentUser(principal)
                .stream()
                .map(userFacade::userToUserDTO)
                .collect(Collectors.toList());

        return new ResponseEntity<>(userDTOList, HttpStatus.OK);
    }

    @GetMapping("/{username}/all-friends")
    public ResponseEntity<Object> getAllFriendsByUser(@PathVariable(value = "username")
                                                                  String username, Principal principal) {
        List<UserDTO> userDTOList = userService.getFriendsByUser(username, principal)
                .stream()
                .map(userFacade::userToUserDTO)
                .collect(Collectors.toList());

        return new ResponseEntity<>(userDTOList, HttpStatus.OK);
    }

    @GetMapping("/sent-requests")
    public ResponseEntity<Object> getAllSentFriendRequests(Principal principal) {
        List<UserDTO> userDTOList = userService.getSentFriendRequests(principal)
                .stream()
                .map(userFacade::userToUserDTO)
                .collect(Collectors.toList());

        return new ResponseEntity<>(userDTOList, HttpStatus.OK);
    }

    @GetMapping("/retrieved-requests")
    public ResponseEntity<Object> getAllRetrievedFriendRequests(Principal principal) {
        List<UserDTO> userDTOList = userService.getRetrievedFriendRequests(principal)
                .stream()
                .map(userFacade::userToUserDTO)
                .collect(Collectors.toList());

        return new ResponseEntity<>(userDTOList, HttpStatus.OK);
    }

    @GetMapping("/all-users")
    public ResponseEntity<Object> getAllUsers(Principal principal) {
        User currentUser = userService.getCurrentUser(principal);
        List<UserDTO> userDTOList = userService.getAllUsers()
                .stream()
                .filter(user -> !user.equals(currentUser))
                .map(userFacade::userToUserDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(userDTOList, HttpStatus.OK);
    }
}
