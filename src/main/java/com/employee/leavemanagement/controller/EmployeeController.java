package com.employee.leavemanagement.controller;

import com.employee.leavemanagement.config.FileStorageProperties;
import com.employee.leavemanagement.model.LeaveBalance;
import com.employee.leavemanagement.model.LeaveRequest;
import com.employee.leavemanagement.model.LeaveStatus;
import com.employee.leavemanagement.model.User;
import com.employee.leavemanagement.repository.UserRepository;
import com.employee.leavemanagement.security.CustomUserDetails;
import com.employee.leavemanagement.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageProperties fileStorageProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, 
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        Long userId = userDetails.getUser().getId();
        LeaveBalance balance = leaveService.getLeaveBalance(userId, LocalDate.now().getYear());
        List<LeaveRequest> history = leaveService.getEmployeeLeaveHistory(userId);
        
        long pendingCount = history.stream().filter(r -> r.getStatus() == LeaveStatus.PENDING).count();
        long approvedThisYear = history.stream()
                .filter(r -> r.getStatus() == LeaveStatus.APPROVED && r.getStartDate().getYear() == LocalDate.now().getYear())
                .mapToInt(LeaveRequest::getTotalDays)
                .sum();

        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("balance", balance);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedThisYear", approvedThisYear);
        model.addAttribute("recentLeaves", leaveService.getEmployeeLeaveHistoryPaginated(userId, PageRequest.of(page, 5)));
        
        return "employee/dashboard";
    }

    @GetMapping("/leave/apply")
    public String applyLeaveForm(Model model) {
        model.addAttribute("leaveRequest", new LeaveRequest());
        return "employee/apply-leave";
    }

    @PostMapping("/leave/apply")
    public String submitLeaveRequest(@ModelAttribute LeaveRequest leaveRequest, 
                                     @RequestParam(value = "document", required = false) MultipartFile document,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            leaveRequest.setUser(userDetails.getUser());
            
            if (document != null && !document.isEmpty()) {
                String fileName = StringUtils.cleanPath(document.getOriginalFilename());
                String uploadDir = fileStorageProperties.getUploadDir() + userDetails.getUser().getId() + "/";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                InputStream inputStream = document.getInputStream();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                leaveRequest.setDocumentPath("/" + uploadDir + fileName);
            }
            
            leaveService.applyLeave(leaveRequest);
            return "redirect:/employee/leave/history?success";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/employee/leave/apply";
        } catch (java.io.IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload document.");
            return "redirect:/employee/leave/apply";
        }
    }

    @PostMapping("/leave/cancel")
    public String cancelLeave(@RequestParam Long leaveId,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        leaveService.cancelLeaveRequest(leaveId, userDetails.getUser().getId());
        return "redirect:/employee/leave/history?cancelled";
    }

    @GetMapping("/leave/history")
    public String leaveHistory(@AuthenticationPrincipal CustomUserDetails userDetails, 
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        Page<LeaveRequest> history = leaveService.getEmployeeLeaveHistoryPaginated(userDetails.getUser().getId(), PageRequest.of(page, 10));
        model.addAttribute("history", history);
        return "employee/leave-history";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails.getUser());
        return "employee/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String phone,
                                @RequestParam String emergencyContact,
                                @RequestParam String address,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        User user = userDetails.getUser();
        user.setPhone(phone);
        user.setEmergencyContact(emergencyContact);
        user.setAddress(address);
        
        userRepository.save(user);
        return "redirect:/employee/profile?updated";
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
        return "redirect:/employee/profile";
    }
}
