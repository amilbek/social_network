package com.project.social_network.repository;

import com.project.social_network.entity.Comment;
import com.project.social_network.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    //select * from comment where post = 'post'
    List<Comment> findAllByPost(Post post);

    //select * from comment where id = 'commentId' and user_id = 'userId'
    Comment findByIdAndUserId(Long commentId, Long userId);
}
