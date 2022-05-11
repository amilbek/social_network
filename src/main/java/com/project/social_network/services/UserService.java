package com.project.social_network.services;

import com.project.social_network.dto.UserDTO;
import com.project.social_network.entity.Friend;
import com.project.social_network.entity.Post;
import com.project.social_network.entity.User;
import com.project.social_network.entity.enums.ERole;
import com.project.social_network.entity.enums.EStatus;
import com.project.social_network.exceptions.FriendRequestNotFoundException;
import com.project.social_network.exceptions.UserExistException;
import com.project.social_network.payload.request.SignupRequest;
import com.project.social_network.repository.FriendRepository;
import com.project.social_network.repository.PostRepository;
import com.project.social_network.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Methods:
 * Create User,
 * Update User Info,
 * Send Friend Request,
 * Accept Friend Request,
 * Open Account,
 * Close Account,
 * Delete Account,
 * Get User By Id,
 * Get User By Username,
 * Get User By Principal,
 * Get Friends By User,
 * Get Sent Friend Requests By User,
 * Get Retrieved Friend Requests By User,
 * Get All Users
 */

@Service
public class UserService {

    public static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final PostRepository postRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       FriendRepository friendRepository,
                       PostRepository postRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void saveUser(SignupRequest userIn) {
        User user = new User();

        user.setEmail(userIn.getEmail());
        user.setName(userIn.getFirstname());
        user.setLastName(userIn.getLastname());
        user.setUsername(userIn.getUsername());
        user.setPassword(passwordEncoder.encode(userIn.getPassword()));
        user.setStatus(EStatus.ACTIVE);
        user.getRoles().add(ERole.ROLE_USER);

        try {
            LOG.info("Saving User {}", userIn.getEmail());
            userRepository.save(user);
        } catch (Exception ex) {
            LOG.error("Error during registration. {}", ex.getMessage());
            throw new UserExistException("The user " + user.getUsername() + " already exist. " +
                    "Please check credentials");
        }
    }

    public User updateUser(UserDTO userDTO, Principal principal) {
        User user = getUserByPrincipal(principal);

        user.setEmail(userDTO.getEmail());
        user.setName(userDTO.getFirstname());
        user.setLastName(userDTO.getLastname());
        user.setBio(userDTO.getBio());

        try {
            LOG.info("Update User Info {}", userDTO.getEmail());
            userRepository.save(user);
        } catch (Exception ex) {
            LOG.error("Error during updating. {}", ex.getMessage());
            throw new UsernameNotFoundException("The user " + user.getUsername() + " does not exist.");
        }
        return user;
    }

    public void inactiveAccount(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<Post> posts = postRepository.findAllByUserOrderByCreatedDateDesc(user);
        for (Post post : posts) {
            LOG.info("Deleting post {}", post.getId());
            post.setStatus(EStatus.INACTIVE);
        }
        LOG.info("Deleting user account {}", user.getUsername());
        user.setStatus(EStatus.INACTIVE);
    }

    public void openAccount(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<Post> posts = postRepository.findAllByUserOrderByCreatedDateDesc(user);
        for (Post post : posts) {
            LOG.info("Active post {}", post.getId());
            post.setStatus(EStatus.ACTIVE);
        }
        LOG.info("Opening user account {}", user.getUsername());
        user.setStatus(EStatus.ACTIVE);
    }

    public void closeAccount(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<Post> posts = postRepository.findAllByUserOrderByCreatedDateDesc(user);
        for (Post post : posts) {
            LOG.info("Inactive post {}", post.getId());
            post.setStatus(EStatus.CLOSED);
        }
        LOG.info("Closing user account {}", user.getUsername());
        user.setStatus(EStatus.CLOSED);
    }

    public User getUser(String username, Principal principal) {
        User currentUser = getUserByPrincipal(principal);
        User user = getUserByUsername(username);
        List<User> userFriends = getFriendsByUser(username, principal);
        if (user.getStatus().equals(EStatus.CLOSED) && userFriends.contains(currentUser)) {
            return user;
        }
        return currentUser;
    }

    public User sendFriendRequest(String username, Principal principal) {
        User currentUser = getUserByPrincipal(principal);
        User friend = getUserByUsername(username);

        Friend friendRequest = new Friend();

        if (!friendRepository.existsByFirstUserAndSecondUser(currentUser, friend)) {
            friendRequest.setFirstUser(currentUser);
            friendRequest.setSecondUser(friend);
            friendRequest.setIsAccepted(false);

            LOG.info("Sending Friend Request from {} to {}", currentUser.getUsername(),
                    friend.getUsername());
            friendRepository.save(friendRequest);
        }

        return friend;
    }

    public User acceptFriendRequest(String username, Principal principal) {
        User currentUser = getUserByPrincipal(principal);
        User friend = getUserByUsername(username);

        Friend friendRequest = friendRepository.findByFirstUserAndSecondUser(friend, currentUser).
                orElseThrow(() -> new FriendRequestNotFoundException("Friend Request not found"));
        if (Boolean.FALSE.equals(friendRequest.getIsAccepted())) {
            LOG.info("Accepting Friend Request from {} to {}", friend.getUsername(),
                    currentUser.getUsername());
            friendRequest.setIsAccepted(true);
        }
        return friend;
    }

    public List<User> getFriendsByCurrentUser(Principal principal) {
        User currentUser = getUserByPrincipal(principal);
        LOG.info("Getting friends of current user: {}", currentUser.getUsername());
        return getFriends(currentUser);
    }

    public List<User> getFriendsByUser(String username, Principal principal) {
        User user = getUserByUsername(username);
        if (friendRepository.existsByFirstUserAndSecondUser(user, getCurrentUser(principal)) &&
        (getFriendRequest(user, getCurrentUser(principal)).getIsAccepted() ||
                getFriendRequest(getCurrentUser(principal), user).getIsAccepted())) {
            LOG.info("Getting friends of user: {}", user.getUsername());
            return getFriends(user);
        }
        return getFriendsByCurrentUser(principal);
    }

    public List<User> getSentFriendRequests(Principal principal) {
        User currentUser = getUserByPrincipal(principal);

        List<Friend> friendRequests = friendRepository.findByFirstUser(currentUser);
        List<User> friends = new ArrayList<>();

        for (Friend friendRequest : friendRequests) {
            if (Boolean.FALSE.equals(friendRequest.getIsAccepted())) {
                friends.add(friendRequest.getSecondUser());
            }
        }
        LOG.info("Getting friends sent requests of user: {}", currentUser.getUsername());
        return friends;
    }

    public List<User> getRetrievedFriendRequests(Principal principal) {
        User currentUser = getUserByPrincipal(principal);

        List<Friend> friendRequests = friendRepository.findBySecondUser(currentUser);
        List<User> friends = new ArrayList<>();

        for (Friend friendRequest : friendRequests) {
            if (Boolean.FALSE.equals(friendRequest.getIsAccepted())) {
                friends.add(friendRequest.getFirstUser());
            }
        }
        LOG.info("Getting friends retrieved requests of user: {}", currentUser.getUsername());
        return friends;
    }

    public List<User> getAllUsers() {
        LOG.info("Getting all users");
        return userRepository.findAll();
    }

    public User getCurrentUser(Principal principal) {
        return getUserByPrincipal(principal);
    }

    public User getUserByUsername(String username) {
        return userRepository.findUserByUsername(username).
                orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findUserById(id).
                orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private User getUserByPrincipal(Principal principal) {
        String username = principal.getName();
        return userRepository.findUserByUsername(username).
                orElseThrow(() -> new UsernameNotFoundException("User not found with username " + username));
    }

    private Friend getFriendRequest(User user1, User user2) {
        return friendRepository.findByFirstUserAndSecondUser(user1, user2)
                .orElseThrow(() -> new FriendRequestNotFoundException("There is no friendship between " +
                        "user "+ user1.getUsername() + " and " + user2.getUsername() ));
    }

    private List<User> getFriends(User user) {
        List<Friend> friendsByFirstUser = friendRepository.findByFirstUser(user);
        List<Friend> friendsBySecondUser = friendRepository.findBySecondUser(user);
        List<User> friendUsers = new ArrayList<>();

        for (Friend friend : friendsByFirstUser) {
            if (Boolean.TRUE.equals(friend.getIsAccepted())) {
                friendUsers.add(getUserById(friend.getSecondUser().getId()));
            }
        }
        for (Friend friend : friendsBySecondUser) {
            if (Boolean.TRUE.equals(friend.getIsAccepted())) {
                friendUsers.add(getUserById(friend.getFirstUser().getId()));
            }
        }
        return friendUsers;
    }
}
