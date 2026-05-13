package com.employee.leavemanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    JavaMailSender mailSender;

    public void sendApprovalRequest(String managerEmail, String managerName, String employeeName, String dates,
            int days, String type, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(managerEmail);
            message.setSubject("ACTION REQUIRED: Leave request from " + employeeName);
            message.setText("Hi " + managerName + ",\n\n" +
                    employeeName + " has requested leave for " + dates + ".\n" +
                    "Total Days: " + days + "\n" +
                    "Leave Type: " + type + "\n" +
                    "Reason: " + reason + "\n\n" +
                    "Please log in to the Leave Management System to Approve or Reject this request.");
            mailSender.send(message);
            logger.info("Real email sent to manager: {}", managerEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to manager: {}", e.getMessage());
        }
    }

    public void sendStatusNotification(String employeeEmail, String employeeName, String status, String dates,
            String managerName, String comment) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(employeeEmail);
            message.setSubject("Leave request [" + status + "] - " + dates);
            message.setText("Dear " + employeeName + ",\n\n" +
                    "Your leave request for " + dates + " has been " + status + ".\n" +
                    "Processed By: " + managerName + "\n" +
                    "Comment: " + (comment != null && !comment.isEmpty() ? comment : "None") + "\n\n" +
                    "Please log in to the Leave Management System to view details.");
            mailSender.send(message);
            logger.info("Real email sent to employee: {}", employeeEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to employee: {}", e.getMessage());
        }
    }
}
