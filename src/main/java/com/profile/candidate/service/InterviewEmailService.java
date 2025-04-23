package com.profile.candidate.service;

import com.profile.candidate.model.CandidateDetails;
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
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate(); // Will throw an exception if the email is invalid
            return true;
        } catch (AddressException e) {
            return false;
        }
    }

    public void sendCandidateNotification(CandidateDetails candidateDetails, String recruiterName,
                                          String recruiterEmail,String teamLeadName, String teamLeadEmail, String actionType) {
        String subject = "";
        String bodyForRecruiter = "";
        String bodyForTeamLead = "";

        String jobId = candidateDetails.getJobId();
        String skills = candidateDetails.getSkills();
        String overallFeedback = candidateDetails.getOverallFeedback();
        String candidateName = candidateDetails.getFullName();
        String candidateEmail = candidateDetails.getCandidateEmailId();

        // Set subject and body based on actionType (submission, update, deletion)
        switch (actionType.toLowerCase()) {
            case "submission":
                subject = "New Candidate Submission - Job ID: " + jobId;
                bodyForRecruiter = String.format(
                        "<p>Dear %s,</p>"
                                + "<p>You have successfully submitted the following candidate:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "<li><b>Total Experience:</b> %.1f years</li>"
                                + "<li><b>Primary Skills:</b> %s</li>"
                                + "<li><b>Overall Feedback:</b> %s</li>"
                                + "</ul>"
                                + "<p>Keep up the good work!</p>"
                                + "<p><b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>",
                        recruiterName, candidateDetails.getCandidateId(), jobId, candidateName,
                        candidateEmail, candidateDetails.getTotalExperience(), skills, overallFeedback
                );

                bodyForTeamLead = String.format(
                        "<p>Dear %s,</p>"
                                + "<p>The following candidate has been submitted by <b>%s</b>:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "<li><b>Total Experience:</b> %.1f years</li>"
                                + "<li><b>Primary Skills:</b> %s</li>"
                                + "<li><b>Overall Feedback:</b> %s</li>"
                                + "</ul>"
                                + "<p><b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>",
                        teamLeadName, recruiterName, candidateDetails.getCandidateId(), jobId, candidateName,
                        candidateEmail, candidateDetails.getTotalExperience(), skills, overallFeedback
                );

                break;

            case "update":
                subject = "Candidate Profile Updated - Job ID: " + jobId;
                bodyForRecruiter = String.format(
                        "<p>Dear %s,</p>"
                                + "<p>You have updated the following candidate's profile:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "<li><b>Total Experience:</b> %.1f years</li>"
                                + "<li><b>Primary Skills:</b> %s</li>"
                                + "<li><b>Overall Feedback:</b> %s</li>"
                                + "</ul>"
                                + "<p>The updated profile is now available on the portal.</p>"
                                + "<p><b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>",
                        recruiterName, candidateDetails.getCandidateId(), jobId, candidateName,
                        candidateEmail, candidateDetails.getTotalExperience(), skills, overallFeedback
                );

                bodyForTeamLead = String.format(
                        "<p>Dear %s,</p>"
                                + "<p>The following candidate's profile has been updated by <b>%s</b>:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "<li><b>Total Experience:</b> %.1f years</li>"
                                + "<li><b>Primary Skills:</b> %s</li>"
                                + "<li><b>Overall Feedback:</b> %s</li>"
                                + "</ul>"
                                + "<p>Kindly log in to the portal to review the updated profile.</p>"
                                + "<p><b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>",
                        teamLeadName, recruiterName, candidateDetails.getCandidateId(), jobId, candidateName,
                        candidateEmail, candidateDetails.getTotalExperience(), skills, overallFeedback
                );
                break;

            case "deletion":
                subject = "Candidate Profile Deleted - Job ID: " + jobId;
                bodyForRecruiter = String.format(
                        "<p>Dear %s,</p>"
                                + "<p>You have deleted the following candidate's profile:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "</ul>"
                                + "<p>If this was unintentional, please contact your team lead immediately.</p>"
                                + "<p><b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>",
                        recruiterName, candidateDetails.getCandidateId(), jobId, candidateName, candidateEmail
                );

                bodyForTeamLead = String.format(
                        "<p>Dear %s,</p>"
                                + "<p>The following candidate profile has been deleted by %s:</p>"
                                + "<ul>"
                                + "<li><b>Candidate ID:</b> %s</li>"
                                + "<li><b>Job ID:</b> %s</li>"
                                + "<li><b>Candidate Name:</b> %s</li>"
                                + "<li><b>Email Address:</b> %s</li>"
                                + "</ul>"
                                + "<p>If this was unintentional or further details are required, please follow up with the recruiter.</p>"
                                + "<p><b><span style='font-size:18px;'>Please do not reply to this email address as it is sent from an unmonitored mailbox.</span></b></p>",
                        teamLeadName, recruiterName, candidateDetails.getCandidateId(), jobId, candidateName, candidateEmail
                );
                break;

            default:
                throw new IllegalArgumentException("Invalid action type: " + actionType);
        }

        // Send email to the recruiter and team lead only
        sendInterviewNotification(recruiterEmail, subject, bodyForRecruiter); // To Recruiter
        sendInterviewNotification(teamLeadEmail, subject, bodyForTeamLead);   // To Team Lead
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
