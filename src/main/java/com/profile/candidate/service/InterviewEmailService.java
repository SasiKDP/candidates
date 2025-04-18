package com.profile.candidate.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@Service
public class InterviewEmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Getting the sender email from the properties file
    @Value("${spring.mail.username}")
    private String senderEmail;

    private static final Logger logger = LoggerFactory.getLogger(InterviewEmailService.class);

    public void sendInterviewNotification(String to, String subject, String body) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            // Validate sender email
            if (senderEmail == null || senderEmail.isEmpty()) {
                logger.error("Sender email is not configured correctly.");
                throw new EmailConfigurationException("Sender email is not configured.");
            }

            // Validate recipient email format
            if (!isValidEmail(to.trim())) {
                logger.error("Invalid recipient email: {}", to);
                throw new IllegalArgumentException("Invalid recipient email format.");
            }

            // Create and configure MimeMessageHelper
            helper = new MimeMessageHelper(message, true);

            // Set the recipient, subject, body, and sender
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);  // true = send as HTML
            helper.setFrom(senderEmail); // Use the email from properties

            // Send the email
            mailSender.send(message);

            // Log success
            logger.info("Email sent successfully to {}", to);

        } catch (EmailConfigurationException e) {
            // Log error if sender email is not configured correctly
            logger.error("Invalid sender email configuration: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            // Log error if recipient email format is invalid
            logger.error("Invalid email address: {}", e.getMessage());
            throw e;
        } catch (MailException e) {
            // Log mail-related exceptions
            logger.error("Failed to send email to {}. Error: {}", to, e.getMessage(), e);
            throw new EmailSendingException("An error occurred while sending the email.", e);
        } catch (Exception e) {
            // Log any unexpected exceptions
            logger.error("Unexpected error occurred while sending email to {}. Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Unexpected error occurred while sending email.", e);
        }
    }

    // Helper method to validate email format
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;  // Early return if email is null or empty
        }

        // Trim email to remove any leading/trailing whitespace before validation
        email = email.trim();

        // Regex to check valid email format
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }



    // Custom Exception for Email Configuration Issues
    public static class EmailConfigurationException extends RuntimeException {
        public EmailConfigurationException(String message) {
            super(message);
        }
    }

    // Custom Exception for Email Sending Failures
    public static class EmailSendingException extends RuntimeException {
        public EmailSendingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
