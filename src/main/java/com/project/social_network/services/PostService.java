package com.project.social_network.services;

import com.project.social_network.dto.PostDTO;
import com.project.social_network.entity.Friend;
import com.project.social_network.entity.Post;
import com.project.social_network.entity.User;
import com.project.social_network.entity.enums.EStatus;
import com.project.social_network.exceptions.FriendRequestNotFoundException;
import com.project.social_network.exceptions.PostNotFoundException;
import com.project.social_network.repository.FriendRepository;
import com.project.social_network.repository.PostRepository;
import com.project.social_network.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Methods:
 * Create Post,
 * Update Post,
 * Delete Post,
 * Get Post By Id,
 * Get Posts By User,
 * Get All Posts
 * Like Post
 */

@Service
public class PostService {

    public static final Logger LOG = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final UserService userService;

    @Autowired
    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       FriendRepository friendRepository,
                       UserService userService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.userService = userService;
    }


    public Post createPost(PostDTO postDTO, Principal principal) {
        User user = getUserByPrincipal(principal);
        Post post = new Post();
        post.setUser(user);
        post.setCaption(postDTO.getCaption());
        post.setLikes(0);
        post.setStatus(EStatus.ACTIVE);

        LOG.info("Saving Post for User: {}", user.getUsername());
        return postRepository.save(post);
    }

    public Post updatePost(PostDTO postDTO, Principal principal, Long postId) {
        Post post = getPostByIdAndCurrentUser(postId, principal);
        post.setCaption(postDTO.getCaption());

        LOG.info("Updating Post: {}", post.getId());
        return postRepository.save(post);
    }

    public void inactivePost(Long postId) {
        Post post = getPost(postId);
        LOG.info("Banning Post: {},", postId);
        post.setStatus(EStatus.INACTIVE);
    }

    public Post getPostByIdAndCurrentUser(Long postId, Principal principal) {
        User user = getUserByPrincipal(principal);
        LOG.info("Getting Post {} of user {}", postId, user.getUsername());
        return postRepository.findPostByIdAndUser(postId, user)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID " + postId +
                        " and user " + user.getEmail()));
    }

    public Post getPostById(Long postId, Principal principal) {
        User user = getUserByPrincipal(principal);
        User friend = null;
        Post post = getPost(postId);
        List<Post> posts = postRepository.findAll();
        for (Post p : posts) {
            if (p.equals(post)) {
                 friend = p.getUser();
            }
        }
        if (post.getStatus().equals(EStatus.CLOSED)) {
            if (friendRepository.existsByFirstUserAndSecondUser(user, friend)) {
                return getPost(postId);
            }
        } else if (post.getStatus().equals(EStatus.ACTIVE)) {
            return getPost(postId);
        }
        return null;
    }

    public List<Post> getAllPostsOfFriends(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<User> friendUsers = userService.getFriendsByUser(user.getUsername(), principal);
        List<Post> posts = new ArrayList<>();

        for (User friend : friendUsers) {
            posts.addAll(postRepository.findAllByUser(friend)
                    .stream()
                    .filter(p -> p.getStatus().equals(EStatus.ACTIVE))
                    .collect(Collectors.toList()));
        }
        LOG.info("Getting friends posts of user {}", user.getUsername());
        return posts;
    }

    public List<Post> getAllPostsForCurrentUser(Principal principal) {
        User user = getUserByPrincipal(principal);
        LOG.info("Getting posts of current user {}", user.getUsername());
        return postRepository.findAllByUserOrderByCreatedDateDesc(user);
    }

    public List<Post> getPostsByUsername(String username, Principal principal) {
        User currentUser = getUserByPrincipal(principal);
        User user = getUserByUsername(username);

        List<Post> posts = postRepository.findAllByUserOrderByCreatedDateDesc(user);

        Friend friendRequest1 = getFriendRequest(currentUser, user);
        Friend friendRequest2 = getFriendRequest(user, currentUser);

        if (friendRequest1.getIsAccepted() || friendRequest2.getIsAccepted()) {
            LOG.info("Getting posts of user {}", user.getUsername());
            return posts;
        }
        return Collections.emptyList();
    }

    public List<Post> getAllPosts() {
        LOG.info("Getting all posts");
        return postRepository.findAllByOrderByCreatedDateDesc()
                .stream()
                .filter(p -> p.getStatus().equals(EStatus.ACTIVE))
                .collect(Collectors.toList());
    }

    public Post likePost(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID " + postId));

        Optional<String> userLiked = post.getLikedUsers()
                .stream()
                .filter(u -> u.equals(username)).findAny();

        if (userLiked.isPresent()) {
            post.setLikes(post.getLikes() - 1);
            post.getLikedUsers().remove(username);
        } else {
            post.setLikes(post.getLikes() + 1);
            post.getLikedUsers().add(username);
        }
        LOG.info("User {} likes post {}", username, postId);
        return postRepository.save(post);
    }

    private User getUserByUsername(String username) {
        return userRepository.findUserByUsername(username).
                orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Friend getFriendRequest(User user1, User user2) {
        return friendRepository.findByFirstUserAndSecondUser(user1, user2).
                orElseThrow(() -> new FriendRequestNotFoundException("Friend Request not found"));
    }

    private User getUserByPrincipal(Principal principal) {
        String username = principal.getName();
        return userRepository.findUserByUsername(username).
                orElseThrow(() -> new UsernameNotFoundException("User not found with username " + username));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID " + postId));
    }
}
