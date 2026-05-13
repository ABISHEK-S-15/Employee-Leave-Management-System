package com.employee.leavemanagement.service;

import com.employee.leavemanagement.model.*;
import com.employee.leavemanagement.repository.LeaveBalanceRepository;
import com.employee.leavemanagement.repository.LeavePolicyRepository;
import com.employee.leavemanagement.repository.LeaveRequestRepository;
import com.employee.leavemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private EmailService emailService;

    public int calculateTotalDays(LocalDate start, LocalDate end, boolean skipWeekends) {
        int days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (skipWeekends) {
                if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    days++;
                }
            } else {
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }

    public LeaveBalance getLeaveBalance(Long userId, int year) {
        LeavePolicy policy = leavePolicyRepository.findAll().stream().findFirst()
                .orElse(new LeavePolicy(null, 18, 12, 6, 0, 5, true));

        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndYear(userId, year).orElseGet(() -> {
            User user = userRepository.findById(userId).orElseThrow();

            // Calculate carry forward from previous year
            int carryForwardDays = 0;
            Optional<LeaveBalance> prevYearOpt = leaveBalanceRepository.findByUserIdAndYear(userId, year - 1);
            if (prevYearOpt.isPresent()) {
                LeaveBalance prev = prevYearOpt.get();
                int unusedAnnual = (prev.getAnnualQuota()
                        + (prev.getCarriedForward() != null ? prev.getCarriedForward() : 0)) - prev.getAnnualUsed();
                if (unusedAnnual > 0) {
                    carryForwardDays = Math.min(policy.getCarryForwardLimit(), unusedAnnual);
                }
            }

            LeaveBalance b = LeaveBalance.builder()
                    .user(user)
                    .year(year)
                    .annualUsed(0)
                    .sickUsed(0)
                    .casualUsed(0)
                    .carriedForward(carryForwardDays)
                    .build();
            return b;
        });

        balance.setAnnualQuota(policy.getAnnualQuota());
        balance.setSickQuota(policy.getSickQuota());
        balance.setCasualQuota(policy.getCasualQuota());

        return leaveBalanceRepository.save(balance);
    }

    public LeaveRequest applyLeave(LeaveRequest request) {
        // Overlap validation
        List<LeaveRequest> overlaps = leaveRequestRepository.findOverlappingRequests(
                request.getUser().getId(), request.getStartDate(), request.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException(
                    "Overlapping dates detected. You have already requested leave for these days.");
        }
        int totalDays = calculateTotalDays(request.getStartDate(), request.getEndDate(), true);
        request.setTotalDays(totalDays);

        // Policy validation
        if (request.getType() != LeaveType.UNPAID) {
            LeaveBalance balance = getLeaveBalance(request.getUser().getId(), request.getStartDate().getYear());
            int remaining = 0;
            String leaveTypeName = "";

            // Calculate pending days for this specific leave type in the current year
            int pendingDays = leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(request.getUser().getId())
                    .stream()
                    .filter(r -> r.getStatus() == LeaveStatus.PENDING
                            && r.getType() == request.getType()
                            && r.getStartDate().getYear() == request.getStartDate().getYear())
                    .mapToInt(LeaveRequest::getTotalDays)
                    .sum();

            switch (request.getType()) {
                case ANNUAL:
                    remaining = (balance.getAnnualQuota()
                            + (balance.getCarriedForward() != null ? balance.getCarriedForward() : 0))
                            - balance.getAnnualUsed() - pendingDays;
                    leaveTypeName = "Annual";
                    break;
                case SICK:
                    remaining = balance.getSickQuota() - balance.getSickUsed() - pendingDays;
                    leaveTypeName = "Sick";
                    break;
                case CASUAL:
                    remaining = balance.getCasualQuota() - balance.getCasualUsed() - pendingDays;
                    leaveTypeName = "Casual";
                    break;
                default:
                    // Other types like MATERNITY_PATERNITY might not have strict limits in this
                    // demo, or are handled differently.
                    remaining = 999;
            }

            if (totalDays > remaining) {
                throw new IllegalArgumentException("Insufficient " + leaveTypeName
                        + " leave balance. You are requesting " + totalDays + " days but only have "
                        + Math.max(0, remaining) + " days left. Please consider applying for UNPAID leave.");
            }
        }

        // Max consecutive days check (applies to all leave types)
        LeavePolicy policy = leavePolicyRepository.findAll().stream().findFirst()
                .orElse(new LeavePolicy(null, 18, 12, 6, 0, 5, true));

        // We calculate calendar days for the consecutive days check, to prevent long
        // unbroken absences
        int calendarDays = calculateTotalDays(request.getStartDate(), request.getEndDate(), false);

        if (calendarDays > policy.getMaxConsecutiveDays() && request.getType() != LeaveType.MATERNITY_PATERNITY) {
            throw new IllegalArgumentException("You cannot request more than " + policy.getMaxConsecutiveDays()
                    + " consecutive calendar days off at a time per company policy.");
        }
        LeaveRequest saved = leaveRequestRepository.save(request);

        List<User> managers = userRepository.findByRole(Role.ROLE_MANAGER);
        if (!managers.isEmpty()) {
            User manager = managers.get(0);
            String dates = request.getStartDate().toString() + " to " + request.getEndDate().toString();
            emailService.sendApprovalRequest(
                    manager.getEmail(),
                    manager.getFullName(),
                    request.getUser().getFullName(),
                    dates,
                    totalDays,
                    request.getType().name(),
                    request.getReason());
        }
        return saved;
    }

    public List<LeaveRequest> getEmployeeLeaveHistory(Long userId) {
        return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<LeaveRequest> getEmployeeLeaveHistoryPaginated(Long userId, Pageable pageable) {
        return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<LeaveRequest> getAllRequestsForManager() {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<LeaveRequest> getProcessedRequestsForManager(Pageable pageable) {
        return leaveRequestRepository.findProcessedRequestsOrderedByProcessedAt(pageable);
    }

    public void processLeaveRequest(Long leaveId, LeaveStatus status, String comment) {
        LeaveRequest request = leaveRequestRepository.findById(leaveId).orElseThrow();
        request.setStatus(status);
        request.setManagerComment(comment);
        request.setProcessedAt(LocalDateTime.now());

        if (status == LeaveStatus.APPROVED) {
            LeaveBalance balance = getLeaveBalance(request.getUser().getId(), request.getStartDate().getYear());
            switch (request.getType()) {
                case ANNUAL:
                    balance.setAnnualUsed(balance.getAnnualUsed() + request.getTotalDays());
                    break;
                case SICK:
                    balance.setSickUsed(balance.getSickUsed() + request.getTotalDays());
                    break;
                case CASUAL:
                    balance.setCasualUsed(balance.getCasualUsed() + request.getTotalDays());
                    break;
                default:
                    break;
            }
            leaveBalanceRepository.save(balance);
        }

        leaveRequestRepository.save(request);

        String dates = request.getStartDate().toString() + " to " + request.getEndDate().toString();
        List<User> managers = userRepository.findByRole(Role.ROLE_MANAGER);
        String managerName = managers.isEmpty() ? "Admin" : managers.get(0).getFullName();

        emailService.sendStatusNotification(
                request.getUser().getEmail(),
                request.getUser().getFullName(),
                status.name(),
                dates,
                managerName,
                comment != null ? comment : "");
    }

    public void cancelLeaveRequest(Long leaveId, Long userId) {
        LeaveRequest request = leaveRequestRepository.findById(leaveId).orElseThrow();
        if (!request.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized cancellation attempt.");
        }
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be cancelled.");
        }
        leaveRequestRepository.delete(request);
    }
}
