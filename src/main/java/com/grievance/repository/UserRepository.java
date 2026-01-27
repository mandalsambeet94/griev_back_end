package com.grievance.repository;

import com.grievance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT COUNT(*) FROM User u WHERE u.role = 'AGENT' AND u.gpAssigned = :adminId")
    int findAgentCountByAdmin(String adminId);

    Optional<User> findByContact(String contact);
    Optional<User> findByName(String name);

    List<User> findByRole(User.Role role);

    List<User> findByIsActiveTrue();

    List<User> findByRoleAndIsActiveTrue(User.Role role);

    @Query("SELECT u FROM User u WHERE u.role = 'AGENT' AND u.blockAssigned = :block")
    List<User> findAgentsByBlock(String block);

    boolean existsByContact(String contact);
}