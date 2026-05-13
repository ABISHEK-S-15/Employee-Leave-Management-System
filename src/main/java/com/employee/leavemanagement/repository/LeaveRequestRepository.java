package com.employee.leavemanagement.repository;

import com.employee.leavemanagement.model.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status IN ('APPROVED', 'REJECTED') ORDER BY lr.processedAt DESC NULLS LAST")
    Page<LeaveRequest> findProcessedRequestsOrderedByProcessedAt(Pageable pageable);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.user.id = :userId AND lr.status IN ('PENDING', 'APPROVED') AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate))")
    List<LeaveRequest> findOverlappingRequests(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
