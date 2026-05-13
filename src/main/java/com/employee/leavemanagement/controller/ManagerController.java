package com.employee.leavemanagement.controller;

import com.employee.leavemanagement.model.*;
import com.employee.leavemanagement.repository.LeavePolicyRepository;
import com.employee.leavemanagement.security.CustomUserDetails;
import com.employee.leavemanagement.service.LeaveService;
import com.employee.leavemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<LeaveRequest> requests = leaveService.getAllRequestsForManager();

        long pendingCount = requests.stream().filter(r -> r.getStatus() == LeaveStatus.PENDING).count();
        long approvedCount = requests.stream().filter(r -> r.getStatus() == LeaveStatus.APPROVED
                && r.getStartDate().getYear() == LocalDate.now().getYear()).count();

        long teamMembersCount = userRepository.findByRole(Role.ROLE_EMPLOYEE)
                .size();

        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("requests", requests);
        model.addAttribute("teamMembersCount", teamMembersCount);

        return "manager/dashboard";
    }

    @GetMapping("/employees")
    public String employees(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search,
            Model model) {
        Page<User> teamMembers;
        if (search.isEmpty()) {
            teamMembers = userRepository.findByRoleOrderByEmpIdAsc(
                    Role.ROLE_EMPLOYEE, PageRequest.of(page, 10));
        } else {
            teamMembers = userRepository.findByRoleAndFullNameContainingIgnoreCaseOrderByEmpIdAsc(
                    Role.ROLE_EMPLOYEE, search, PageRequest.of(page, 10));
        }

        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("teamMembers", teamMembers);
        model.addAttribute("search", search);
        model.addAttribute("newUser", new User());
        return "manager/employees";
    }

    @PostMapping("/employee/add")
    public String addEmployee(@ModelAttribute User newUser, @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email already exists");
            return "redirect:/manager/employees?error";
        }
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser.setRole(Role.ROLE_EMPLOYEE);

        Long nextId = userRepository.findMaxId() + 1;
        newUser.setEmpId(String.format("EMP%03d", nextId));

        userRepository.save(newUser);
        return "redirect:/manager/employees?added";
    }

    @PostMapping("/employee/delete")
    public String deleteEmployee(@RequestParam Long employeeId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User employee = userRepository.findById(employeeId).orElseThrow(); // nosuchelementexception
        if (employee.getRole() == Role.ROLE_EMPLOYEE) {
            userRepository.delete(employee);
        }
        return "redirect:/manager/employees?deleted";
    }

    @PostMapping("/leave/process")
    public String processLeave(@RequestParam Long leaveId,
            @RequestParam String action,
            @RequestParam(required = false) String comment) {
        LeaveStatus status = "approve".equalsIgnoreCase(action) ? LeaveStatus.APPROVED : LeaveStatus.REJECTED;
        leaveService.processLeaveRequest(leaveId, status, comment);
        return "redirect:/manager/dashboard?processed";
    }

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<LeaveRequest> historyRequests = leaveService.getProcessedRequestsForManager(PageRequest.of(page, 10));

        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("requests", historyRequests);
        return "manager/history";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails.getUser());
        return "manager/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String phone,
            @RequestParam String emergencyContact,
            @RequestParam String address,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        user.setPhone(phone);
        user.setEmergencyContact(emergencyContact);
        user.setAddress(address);
        userRepository.save(user);
        return "redirect:/manager/profile?updated";
    }

    @PostMapping("/profile/password")
    public String updatePassword(@RequestParam String oldPassword,
            @RequestParam String newPassword,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User user = userDetails.getUser();
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Password updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Incorrect old password!");
        }
        return "redirect:/manager/profile";
    }

    @GetMapping("/policies")
    public String policies(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        LeavePolicy policy = leavePolicyRepository.findAll().stream().findFirst()
                .orElse(new LeavePolicy(null, 18, 12, 6, 0, 5, true));
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("policy", policy);
        return "manager/policies";
    }

    @PostMapping("/policies/update")
    public String updatePolicies(@ModelAttribute LeavePolicy policy) {
        LeavePolicy existing = leavePolicyRepository.findAll().stream()
                .findFirst()
                .orElse(new LeavePolicy());

        existing.setAnnualQuota(policy.getAnnualQuota());
        existing.setSickQuota(policy.getSickQuota());
        existing.setCasualQuota(policy.getCasualQuota());
        existing.setCarryForwardLimit(policy.getCarryForwardLimit());
        existing.setMaxConsecutiveDays(policy.getMaxConsecutiveDays());
        existing.setSkipWeekends(policy.getSkipWeekends() != null ? policy.getSkipWeekends() : false);

        leavePolicyRepository.save(existing);
        return "redirect:/manager/policies?success";
    }
}
