package com.project.social_network.web;

import com.project.social_network.dto.CommentDTO;
import com.project.social_network.entity.Comment;
import com.project.social_network.entity.User;
import com.project.social_network.facade.CommentFacade;
import com.project.social_network.payload.responce.MessageResponse;
import com.project.social_network.services.CommentService;
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

@RestController
@RequestMapping("/comment")
@CrossOrigin
public class CommentController {

    public static final Logger LOG = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;
    private final CommentFacade commentFacade;
    private final ResponseErrorValidation responseErrorValidation;

    @Autowired
    public CommentController(CommentService commentService,
                             CommentFacade commentFacade,
                             ResponseErrorValidation responseErrorValidation) {
        this.commentService = commentService;
        this.commentFacade = commentFacade;
        this.responseErrorValidation = responseErrorValidation;
    }

    @PostMapping("/{postId}/create")
    public ResponseEntity<Object> createComment(@Valid @RequestBody CommentDTO commentDTO,
                                                @PathVariable(value = "postId") String postId,
                                                BindingResult bindingResult,
                                                Principal principal) {
        ResponseEntity<Object> errors = responseErrorValidation.mapValidationService(bindingResult);
        if (!ObjectUtils.isEmpty(errors)) {
            LOG.error("Error during commenting post");
            return errors;
        }

        Comment comment = commentService.saveComment(Long.parseLong(postId), commentDTO, principal);
        CommentDTO createdComment = commentFacade.commentToCommentDTO(comment);

        LOG.info("Commenting post {}", postId);
        return new ResponseEntity<>(createdComment, HttpStatus.OK);
    }

    @GetMapping("/{postId}/all")
    public ResponseEntity<List<CommentDTO>> getAllCommentsToPost(@PathVariable(value = "postId")
                                                                         String postId) {
        List<CommentDTO> commentDTOList = commentService.getAllCommentsForPost(Long.parseLong(postId))
                .stream()
                .map(commentFacade::commentToCommentDTO)
                .collect(Collectors.toList());
        LOG.info("Getting comments of post {}", postId);
        return new ResponseEntity<>(commentDTOList, HttpStatus.OK);
    }

    @PostMapping("/{commentId}/delete")
    public ResponseEntity<MessageResponse> deleteComment(@PathVariable(value = "commentId")
                                                                     String commentId) {
        commentService.deleteComment(Long.parseLong(commentId));
        LOG.info("Deleting comment {}", commentId);
        return new ResponseEntity<>(new MessageResponse("Comment was deleted"), HttpStatus.OK);
    }
}
