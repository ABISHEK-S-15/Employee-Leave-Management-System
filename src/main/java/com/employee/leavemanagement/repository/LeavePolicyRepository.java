package com.employee.leavemanagement.repository;

import com.employee.leavemanagement.model.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
}
