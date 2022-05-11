package com.project.social_network.repository;

import com.project.social_network.entity.Friend;
import com.project.social_network.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    boolean existsByFirstUserAndSecondUser(User firstUser, User secondUser);

    Optional<Friend> findByFirstUserAndSecondUser(User firstUser, User secondUser);

    List<Friend> findByFirstUser(User user);
    List<Friend> findBySecondUser(User user);
}