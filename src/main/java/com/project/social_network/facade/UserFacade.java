package com.project.social_network.facade;

import com.project.social_network.dto.UserDTO;
import com.project.social_network.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserFacade {

    public UserDTO userToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstname(user.getName());
        userDTO.setLastname(user.getLastName());
        userDTO.setEmail(user.getEmail());
        userDTO.setBio(user.getBio());

        return userDTO;
    }
}
