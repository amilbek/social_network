package com.project.social_network.services;

import com.project.social_network.dto.CommentDTO;
import com.project.social_network.entity.Comment;
import com.project.social_network.entity.Post;
import com.project.social_network.entity.User;
import com.project.social_network.entity.enums.EStatus;
import com.project.social_network.exceptions.CommentNotFoundException;
import com.project.social_network.exceptions.PostNotFoundException;
import com.project.social_network.repository.CommentRepository;
import com.project.social_network.repository.PostRepository;
import com.project.social_network.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Methods:
 * Create Comment,
 * Delete Comment,
 * Get Comments By Post
 */

@Service
public class CommentService {

    public static final Logger LOG = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public Comment saveComment(Long postId, CommentDTO commentDTO, Principal principal) {
        User user = getUserByPrincipal(principal);
        Post post = getPost(postId);

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUserId(user.getId());
        comment.setUsername(user.getUsername());
        comment.setMessage(commentDTO.getMessage());
        comment.setStatus(EStatus.ACTIVE);

        LOG.info("Saving comment for Post: {}", post.getId());

        return commentRepository.save(comment);
    }

    public List<Comment> getAllCommentsForPost(Long postId) {
        LOG.info("Getting all comments for post {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with ID " + postId));
        return commentRepository.findAllByPost(post)
                .stream()
                .filter(c -> c.getStatus().equals((EStatus.ACTIVE)))
                .collect(Collectors.toList());
    }

    public void deleteComment(Long commentId) {
        Comment comment = getCommentById(commentId);
        LOG.info("Deleting comment {} for post {}", commentId, comment.getPost().getId());
        comment.setStatus(EStatus.INACTIVE);
        commentRepository.save(comment);
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

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId).
                orElseThrow(() -> new CommentNotFoundException("Comment not found with ID " + commentId));
    }
}
