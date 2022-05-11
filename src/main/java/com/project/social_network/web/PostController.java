package com.project.social_network.web;

import com.project.social_network.dto.PostDTO;
import com.project.social_network.entity.Post;
import com.project.social_network.facade.PostFacade;
import com.project.social_network.payload.responce.MessageResponse;
import com.project.social_network.services.PostService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/posts")
@CrossOrigin
public class PostController {

    public static final Logger LOG = LoggerFactory.getLogger(PostController.class);

    private final PostFacade postFacade;
    private final PostService postService;
    private final ResponseErrorValidation responseErrorValidation;

    @Autowired
    public PostController(PostFacade postFacade,
                          PostService postService,
                          ResponseErrorValidation responseErrorValidation) {
        this.postFacade = postFacade;
        this.postService = postService;
        this.responseErrorValidation = responseErrorValidation;
    }

    @PostMapping("/create")
    public ResponseEntity<Object> createPost(@Valid @RequestBody PostDTO postDTO,
                                             BindingResult bindingResult,
                                             Principal principal) {
        ResponseEntity<Object> errors = responseErrorValidation.mapValidationService(bindingResult);
        if (!ObjectUtils.isEmpty(errors)) {
            LOG.error("Errors during creating Post");
            return errors;
        }

        Post post = postService.createPost(postDTO, principal);
        PostDTO createdPost = postFacade.postToPostDTO(post);

        LOG.info("Creating Post");
        return new ResponseEntity<>(createdPost, HttpStatus.OK);
    }

    @PostMapping("/{postId}/update")
    public ResponseEntity<Object> updatePost(@Valid @RequestBody PostDTO postDTO,
                                             @PathVariable(value = "postId") String postId,
                                             BindingResult bindingResult,
                                             Principal principal) {
        ResponseEntity<Object> errors = responseErrorValidation.mapValidationService(bindingResult);
        if (!ObjectUtils.isEmpty(errors)) {
            LOG.error("Errors during updating post {}", postId);
            return errors;
        }

        Post post = postService.updatePost(postDTO, principal, Long.parseLong(postId));
        PostDTO updatedPost = postFacade.postToPostDTO(post);

        LOG.info("Updating post {}", postId);
        return new ResponseEntity<>(updatedPost, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<List<PostDTO>> getAllPost() {
        List<PostDTO> postDTOList = postService.getAllPosts()
                .stream()
                .map(postFacade::postToPostDTO)
                .collect(Collectors.toList());

        LOG.info("Getting all posts");
        return new ResponseEntity<>(postDTOList, HttpStatus.OK);
    }

    @GetMapping("/my-posts")
    public ResponseEntity<List<PostDTO>> getAllPostsForCurrentUser(Principal principal) {
        List<PostDTO> postDTOList = postService.getAllPostsForCurrentUser(principal)
                .stream()
                .map(postFacade::postToPostDTO)
                .collect(Collectors.toList());

        LOG.info("Getting all posts of current user");
        return new ResponseEntity<>(postDTOList, HttpStatus.OK);
    }

    @GetMapping("/my-posts/{postId}")
    public ResponseEntity<PostDTO> getAllPostsForCurrentUser(@PathVariable("postId") String postId,
                                                                   Principal principal) {
        Post post = postService.getPostByIdAndCurrentUser(Long.parseLong(postId), principal);
        PostDTO postDTO = postFacade.postToPostDTO(post);

        LOG.info("Getting post {} of current user", postId);
        return new ResponseEntity<>(postDTO, HttpStatus.OK);
    }

    @PostMapping("/{postId}/{username}/like")
    public ResponseEntity<PostDTO> likePost(@PathVariable("postId") String postId,
                                            @PathVariable("username") String username) {
        Post post = postService.likePost(Long.parseLong(postId), username);
        PostDTO postDTO = postFacade.postToPostDTO(post);

        LOG.info("Liking post {} by user {}", postId, username);
        return new ResponseEntity<>(postDTO, HttpStatus.OK);
    }

    @GetMapping("/friends-posts")
    public ResponseEntity<List<PostDTO>> getFriendsPosts(Principal principal) {
        List<Post> posts = postService.getAllPostsOfFriends(principal);
        List<PostDTO> postDTOList = new ArrayList<>();

        for (Post post : posts) {
            postDTOList.add(postFacade.postToPostDTO(post));
        }

        LOG.info("Getting posts of friends");
        return new ResponseEntity<>(postDTOList, HttpStatus.OK);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPost(@PathVariable("postId") String postId,
                                           Principal principal) {
        Post post = postService.getPostById(Long.parseLong(postId), principal);
        PostDTO postDTO = postFacade.postToPostDTO(post);

        return new ResponseEntity<>(postDTO, HttpStatus.OK);
    }

    @PostMapping("/{postId}/delete")
    public ResponseEntity<MessageResponse> deletePost(@PathVariable("postId") String postId) {
        postService.inactivePost(Long.parseLong(postId));

        return new ResponseEntity<>(new MessageResponse("Post was deleted"), HttpStatus.OK);
    }
}
