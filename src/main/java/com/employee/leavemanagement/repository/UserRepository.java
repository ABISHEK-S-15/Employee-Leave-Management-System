package com.employee.leavemanagement.repository;

import com.employee.leavemanagement.model.Role;
import com.employee.leavemanagement.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    Page<User> findByRoleOrderByEmpIdAsc(Role role, Pageable pageable);
    Page<User> findByRoleAndFullNameContainingIgnoreCaseOrderByEmpIdAsc(Role role, String fullName, Pageable pageable);

    @Query("SELECT COALESCE(MAX(u.id), 0) FROM User u")
    Long findMaxId();
}
