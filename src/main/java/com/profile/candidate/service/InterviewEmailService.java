package com.profile.candidate.service;

import com.profile.candidate.model.InterviewDetails;

import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.Submissions;
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
import java.util.List;
import java.util.stream.Stream;

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

    public void sendEmailToCandidate(String candidateEmail , String subject, String emailBody) {
        if (candidateEmail != null && !candidateEmail.isEmpty()) {
            try {
                logger.info("Sending email to Candidate: {}", candidateEmail);
                sendInterviewNotification(candidateEmail, subject, emailBody);
            } catch (Exception e) {
                logger.error("Failed to send email to Candidate {}: {}", candidateEmail, e.getMessage(), e);
            }
        }
    }
    public void sendEmailToUser(String userEmailId, String subject, String emailBody) {
        if (userEmailId != null && !userEmailId.isEmpty()) {
            try {
                logger.info("Sending email to User: {}", userEmailId);
                sendInterviewNotification(userEmailId, subject, emailBody);
            } catch (Exception e) {
                logger.error("Failed to send email to User {}: {}", userEmailId, e.getMessage(), e);
            }
        }
    }
    public void sendEmailsToClients(List<String> clientEmailList, String subject, String emailBody) {
        if (clientEmailList != null && !clientEmailList.isEmpty()) {
            for (String clientEmail : clientEmailList) {
                if (clientEmail != null && !clientEmail.isEmpty()) {
                    try {
                        logger.info("Sending email to Client: {}", clientEmail);
                        sendInterviewNotification(clientEmail, subject, emailBody);
                    } catch (Exception e) {
                        logger.error("Failed to send email to Client {}: {}", clientEmail, e.getMessage(), e);
                    }
                }
            }
        }
    }


    // The original method for sending an email
    public void sendCandidateNotification(String to, String subject, String body) {
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
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);  // true = send as HTML
            helper.setFrom(senderEmail); // Use the email from properties

            // Send the email
            mailSender.send(message);
            logger.info("Candidate email sent successfully to {}", to);

        } catch (InterviewEmailService.EmailConfigurationException e) {
            logger.error("Invalid sender email configuration: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid email address: {}", e.getMessage());
            throw e;
        } catch (MailException e) {
            logger.error("Failed to send email to {}. Error: {}", to, e.getMessage(), e);
            throw new InterviewEmailService.EmailSendingException("An error occurred while sending the email.", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while sending email to {}. Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Unexpected error occurred while sending email.", e);
        }
    }

    // The overloaded method for sending candidate notifications with additional context
    public void sendCandidateNotification(Submissions submission, String recruiterName, String recruiterEmail, String teamLeadEmail, String actionType) {
        CandidateDetails candidate = submission.getCandidate();

        String subject = "";
        String body = "";

        String jobId = submission.getJobId();
        String skills = submission.getSkills();
        String overallFeedback = submission.getOverallFeedback();

        // Set subject and body based on actionType
        switch (actionType.toLowerCase()) {
            case "submission":
                subject = "New Candidate Submission - Job ID: " + jobId;
                body = String.format(
                        "<p>Dear Team,</p>"
                                + "<p>We are pleased to submit the following candidate for your review and consideration:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "<li><b>Total Experience:</b> %.1f years</li>"
                                + "<li><b>Primary Skills:</b> %s</li>"
                                + "<li><b>Overall Feedback:</b> %s</li>"
                                + "<li><b>Submitted By:</b> %s (%s)</li>"
                                + "</ul>"
                                + "<p>You can log in to the portal to access the full candidate profile and take the next steps in the recruitment process.</p>"
                                + "<p>\n"
                                + "<b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>"
                                + "<p>Best regards,<br>%s</p>",
                        candidate.getCandidateId(), jobId, candidate.getFullName(),
                        candidate.getCandidateEmailId(), candidate.getTotalExperience(), skills,
                        overallFeedback, recruiterName, recruiterEmail, recruiterName
                );
                break;

            case "update":
                subject = "Candidate Profile Updated - Job ID: " + jobId;
                body = String.format(
                        "<p>Dear Team,</p>"
                                + "<p>The following candidate's profile has been updated. Please review the latest details below:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "<li><b>Total Experience:</b> %.1f years</li>"
                                + "<li><b>Primary Skills:</b> %s</li>"
                                + "<li><b>Overall Feedback:</b> %s</li>"
                                + "<li><b>Updated By:</b> %s (%s)</li>"
                                + "</ul>"
                                + "<p>Kindly log in to the portal to review the updated profile.</p>"
                                + "<p>\n"
                                + "<b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>"
                                + "<p>Best regards,<br>%s</p>",
                        candidate.getCandidateId(), jobId, candidate.getFullName(),
                        candidate.getCandidateEmailId(), candidate.getTotalExperience(), skills,
                        overallFeedback, recruiterName, recruiterEmail, recruiterName
                );
                break;

            case "deletion":
                subject = "Candidate Profile Deleted - Job ID: " + jobId;
                body = String.format(
                        "<p>Dear Team,</p>"
                                + "<p>This is to inform you that the following candidate profile has been removed from the system:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "<li><b>Deleted By:</b> %s (%s)</li>"
                                + "</ul>"
                                + "<p>If this was unintentional or further details are required, please don’t hesitate to contact us.</p>"
                                + "<p>\n"
                                + "<b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>"
                                + "<p>Best regards,<br>%s</p>",
                        candidate.getCandidateId(), jobId, candidate.getFullName(),
                        candidate.getCandidateEmailId(), recruiterName, recruiterEmail, recruiterName
                );
                break;

            default:
                throw new IllegalArgumentException("Invalid action type: " + actionType);
        }

// Send email to team lead and recruiter
        sendCandidateNotification(teamLeadEmail, subject, body);  // To Team Lead
        sendCandidateNotification(recruiterEmail, subject, body); // To Recruiter
    }
}
