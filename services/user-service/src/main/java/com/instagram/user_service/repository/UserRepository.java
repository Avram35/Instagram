package com.instagram.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.instagram.user_service.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByFname(String fname);
    Optional<User> findByLname(String lname);
    List<User> findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase
    (
        String username, String fname, String lname
    );
}
