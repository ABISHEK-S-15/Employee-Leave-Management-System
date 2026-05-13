package com.employee.leavemanagement.repository;

import com.employee.leavemanagement.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByUserIdAndYear(Long userId, Integer year);
}
