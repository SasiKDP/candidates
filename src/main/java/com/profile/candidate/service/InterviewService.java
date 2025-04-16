package com.profile.candidate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.profile.candidate.dto.GetInterviewResponse;
import com.profile.candidate.dto.InterviewResponseDto;
import com.profile.candidate.exceptions.*;
import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.InterviewDetails;
import com.profile.candidate.repository.CandidateRepository;
import com.profile.candidate.repository.InterviewRepository;
import com.profile.candidate.repository.SubmissionRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class InterviewService {

    @Autowired
    InterviewEmailService emailService;
    @Autowired
    CandidateRepository candidateRepository;
    @Autowired
    InterviewRepository interviewRepository;
    @Autowired
    SubmissionRepository submissionRepository;

    private static final Logger logger = LoggerFactory.getLogger(InterviewService.class);

    public InterviewResponseDto scheduleInterview(String userId, String candidateId, OffsetDateTime interviewDateTime, Integer duration,
                                                  String zoomLink, String userEmail, String clientEmail,
                                                  String clientName, String interviewLevel, String externalInterviewDetails,String jobId,String fullName,
                                                  String contactNumber,String candidateEmailId) throws JsonProcessingException {

        System.out.println("Starting to schedule interview for userId: " + userId + " and candidateId: " + candidateId);
        if (candidateId == null) {
            throw new CandidateNotFoundException("Candidate ID cannot be null for userId: " + userId);
        }
        if (submissionRepository.findByCandidate_CandidateIdAndJobId(candidateId,jobId)==null){
            throw new JobNotFoundException("Candidate Not Applied for Job "+jobId);
        }
        // Retrieve candidate details
         Optional<CandidateDetails> candidateDetails=candidateRepository.findByCandidateIdAndUserId(candidateId, userId);

        InterviewDetails inti = interviewRepository.findByCandidateIdAndUserIdAndClientNameAndJobId(candidateId, userId,clientName,jobId);
        InterviewDetails interviewDetails =new InterviewDetails();

        if(candidateDetails.isEmpty()) new CandidateNotFoundException("Candidate not found for userId: " + userId + " and candidateId: " + candidateId);

        // Ensure no interview is already scheduled
        if (inti!=null) {
            throw new InterviewAlreadyScheduledException("An interview is already scheduled for candidate ID: " + candidateId);
        }
        // Update candidate details with provided information
        interviewDetails.setUserEmail(userEmail);
        interviewDetails.setClientEmail(clientEmail);
        setDefaultEmailsIfMissing(interviewDetails);

        // Determine Interview Type if not provided
        if (interviewLevel == null || interviewLevel.isEmpty()) {
            interviewLevel = determineInterviewType(clientEmail, zoomLink);
        }
        interviewDetails.setInterviewLevel(interviewLevel);
        // Handle external vs internal interview constraints
        if ("External".equalsIgnoreCase(interviewLevel)) {
            interviewDetails.setClientEmail(clientEmail);
            interviewDetails.setZoomLink(zoomLink);
        } else {
            if (clientEmail == null || clientEmail.isEmpty()) {
                throw new IllegalArgumentException("Client email is required for Internal interviews.");
            }
            if (zoomLink == null || zoomLink.isEmpty()) {
                throw new IllegalArgumentException("Zoom link is required for Internal interviews.");
            }
        }
//        String userId, String candidateId, OffsetDateTime interviewDateTime, Integer duration,
//                String zoomLink, String userEmail, String clientEmail,
//                String clientName, String interviewLevel, String externalInterviewDetails

        interviewDetails.setCandidateId(candidateId);
        interviewDetails.setUserId(userId);
        interviewDetails.setUserEmail(userEmail);
        interviewDetails.setInterviewDateTime(interviewDateTime);
        interviewDetails.setDuration(duration);
        interviewDetails.setZoomLink(zoomLink);
        interviewDetails.setClientEmail(clientEmail);
        interviewDetails.setClientName(clientName);
        interviewDetails.setInterviewLevel(interviewLevel);
        interviewDetails.setExternalInterviewDetails(externalInterviewDetails);
        interviewDetails.setFullName(fullName);
        interviewDetails.setContactNumber(contactNumber);
        interviewDetails.setCandidateEmailId(candidateEmailId);
        interviewDetails.setTimestamp(LocalDateTime.now());

        String clientId=interviewRepository.findClientIdByClientName(clientName);
        if(clientId==null) throw new InvalidClientException("No Client With Name :"+clientName);

        interviewDetails.setClientId(clientId);
        String interviewId=candidateId+"_"+clientId+"_"+jobId;
        interviewDetails.setInterviewId(interviewId);
        interviewDetails.setJobId(jobId);

        // Set interview details
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode statusArray = objectMapper.createArrayNode();
        ObjectNode statusEntry = objectMapper.createObjectNode();
        statusEntry.put("stage", 1);
        statusEntry.put("status", "Scheduled");
        statusEntry.put("timestamp", OffsetDateTime.now().toString());
        statusArray.add(statusEntry);
        interviewDetails.setInterviewStatus(objectMapper.writeValueAsString(statusArray));
        // Save candidate details to the database
        try {
            interviewRepository.save(interviewDetails);
            System.out.println("Candidate saved successfully.");
        } catch (Exception e) {
            throw new RuntimeException("Error while saving candidate data.", e);
        }
        // Send email notifications about the interview
        sendInterviewNotification(interviewDetails);

        // Prepare the response with interview details
        InterviewResponseDto.InterviewPayload payload = new InterviewResponseDto.InterviewPayload(
                interviewDetails.getCandidateId(),
                interviewDetails.getUserEmail(),
                interviewDetails.getCandidateEmailId(),
                interviewDetails.getClientEmail()
        );
        return new InterviewResponseDto(true, "Interview scheduled successfully and email notifications sent.", payload, null);
    }
    /**
     * Determines the interview type based on clientEmail and zoomLink.
     */
    private String determineInterviewType(String clientEmail, String zoomLink) {
        return (clientEmail != null && !clientEmail.isEmpty() && zoomLink != null && !zoomLink.isEmpty())
                ? "Internal"
                : "External";
    }
    /**
     * Sends interview notification emails.
     */
    private void sendInterviewNotification(InterviewDetails interviewDetails) {
        String subject = "Interview Scheduled for " + interviewDetails.getFullName();

        String body = "<p>Hello " + interviewDetails.getFullName() + ",</p>"
                + "<p>Hope you are doing well!</p>"
                + "<p>Thank you for your interest in the position <b>" + interviewDetails.getInterviewLevel() + "</b> for our client <b>" + interviewDetails.getClientName() + "</b>.</p>"
                + "<p>We're pleased to inform you that your profile has been shortlisted for screening.</p>"
                + "<p>Interview Details:</p>"
                + "<ul>"
                + "<li><b>Date:</b> " + interviewDetails.getInterviewDateTime().format(DateTimeFormatter.BASIC_ISO_DATE) + "</li>"
                + "<li><b>Time:</b> " + interviewDetails.getInterviewDateTime().format(DateTimeFormatter.ISO_TIME) + "</li>"
                + "<li><b>Duration:</b> Approx. " + interviewDetails.getDuration() + " minutes</li>"
                + (interviewDetails.getZoomLink() != null ? "<li><b>Join Zoom Meeting:</b> <a href='" + interviewDetails.getZoomLink() + "'>Click here</a></li>" : "")
                + "</ul>"
                + "<p>Kindly confirm your availability by replying to this email.</p>"
                + "<p>Best regards,</p>"
                + "<p>The Interview Team</p>";

        // Validate emails before sending
        if (isValidEmail(interviewDetails.getCandidateEmailId())) {
            emailService.sendInterviewNotification(interviewDetails.getCandidateEmailId(), subject, body);
            System.out.println("Interview notification sent to candidate: " + interviewDetails.getCandidateEmailId());
        } else {
            System.err.println("Invalid candidate email: " + interviewDetails.getCandidateEmailId());
        }

        // If client email is provided, validate it and send
        if (interviewDetails.getClientEmail() != null && isValidEmail(interviewDetails.getClientEmail())) {
            emailService.sendInterviewNotification(interviewDetails.getClientEmail(), subject, body);
            System.out.println("Interview notification sent to client: " + interviewDetails.getClientEmail());
        } else if (interviewDetails.getClientEmail() != null) {
            System.err.println("Invalid client email: " + interviewDetails.getClientEmail());
        }

        // Always send to user email if provided and valid
        if (isValidEmail(interviewDetails.getUserEmail())) {
            emailService.sendInterviewNotification(interviewDetails.getUserEmail(), subject, body);
            System.out.println("Interview notification sent to user: " + interviewDetails.getUserEmail());
        } else {
            System.err.println("Invalid user email: " + interviewDetails.getUserEmail());
        }
    }
    /**
     * Helper method to validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;  // Early return if email is null or empty
        }
        try {
            email = email.trim();  // Remove leading/trailing whitespace
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();  // Will throw an exception if invalid
            return true;  // Valid email format
        } catch (AddressException e) {
            return false;  // Invalid email format
        }
    }
    private void setDefaultEmailsIfMissing(InterviewDetails interviewDetails) {
        if (interviewDetails.getUserEmail() == null) {
            interviewDetails.setUserEmail(interviewDetails.getUserEmail());  // Set to default or handle differently
        }
        if (interviewDetails.getClientEmail() == null) {
            interviewDetails.setClientEmail(interviewDetails.getClientEmail());  // Set to default or handle differently
        }
    }
    public boolean isInterviewScheduled(String candidateId, String jobId,OffsetDateTime interviewDateTime) {
        // Query the repository to check if there's already an interview scheduled at that time
        Optional<InterviewDetails> existingInterview = interviewRepository.findByCandidateIdAndJobIdAndInterviewDateTime(candidateId,jobId, interviewDateTime);

        // Return true if an interview already exists, otherwise false
        return existingInterview.isPresent();
    }
    public boolean isCandidateValidForUser(String userId, String candidateId) {
        // Fetch the candidate by candidateId
        CandidateDetails candidateDetails = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found"));

        // Check if the userId associated with the candidate matches the provided userId
        if (!candidateDetails.getUserId().equals(userId)) {
            return false; // Candidate does not belong to the provided userId
        }
        return true; // Candidate is valid for the user
    }
    public InterviewResponseDto updateScheduledInterview(
            String userId,
            String candidateId,
            String jobId,
            OffsetDateTime interviewDateTime,
            Integer duration,
            String zoomLink,
            String userEmail,
            String clientEmail,
            String clientName,
            String interviewLevel,
            String externalInterviewDetails,
            String interviewStatus) {

        logger.info("Starting interview update for userId: {} and candidateId: {}", userId, candidateId);

        if (candidateId == null) {
            throw new CandidateNotFoundException("Candidate ID cannot be null for userId: " + userId);
        }
        // Retrieve candidate details
        InterviewDetails interview = interviewRepository.findByCandidateIdAndUserIdAndJobId(candidateId, userId,jobId);

        if(interview==null) throw new CandidateNotFoundException("No Interview found for userId: " + userId + " and candidateId: " + candidateId+" for JobId: "+jobId);

        InterviewDetails interviewDetails = interviewRepository.findByCandidateIdAndUserIdAndClientName(candidateId, userId,clientName);

        if (interviewDetails== null) {
            throw new InterviewNotScheduledException("No interview scheduled for candidate ID: " + candidateId+" For Client "+clientName);
        }
        if (interviewDateTime != null) interviewDetails.setInterviewDateTime(interviewDateTime);
        if (duration != null) interviewDetails.setDuration(duration);
        if (zoomLink != null && !zoomLink.isEmpty()) interviewDetails.setZoomLink(zoomLink);
        if (userEmail != null && !userEmail.isEmpty()) interviewDetails.setUserEmail(userEmail);
        if (clientEmail != null && !clientEmail.isEmpty()) interviewDetails.setClientEmail(clientEmail);
        if (clientName != null && !clientName.isEmpty()) interviewDetails.setClientName(clientName);
        if (interviewLevel != null && !interviewLevel.isEmpty()) interviewDetails.setInterviewLevel(interviewLevel);
        if (externalInterviewDetails != null && !externalInterviewDetails.isEmpty()) interviewDetails.setExternalInterviewDetails(externalInterviewDetails);

        // Handle the interview status update if provided
        if (interviewStatus != null && !interviewStatus.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode historyArray;
            try {
                String existingStatus = interviewDetails.getInterviewStatus();
                if (existingStatus != null && !existingStatus.isEmpty()) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(existingStatus);
                        if (jsonNode.isArray()) {
                            historyArray = (ArrayNode) jsonNode;
                        } else {
                            logger.error("Existing interviewStatus is not a valid JSON array: {}", existingStatus);
                            historyArray = objectMapper.createArrayNode(); // Reset to new array
                        }
                    } catch (JsonProcessingException e) {
                        logger.error("Error parsing existing interviewStatus JSON for candidate {}: {}",
                                interviewDetails.getCandidateId(), e.getMessage());
                        historyArray = objectMapper.createArrayNode(); // Reset on failure
                    }
                } else {
                    historyArray = objectMapper.createArrayNode(); // Start fresh if no status exists
                }

                // If the status is provided, don't add "Scheduled" unless this is the first entry
                int nextStage = historyArray.size() + 1; // Changed 'round' to 'stage'
                ObjectNode newEntry = objectMapper.createObjectNode();

                // Add the current status (from UI)
                newEntry.put("stage", nextStage);
                newEntry.put("status", interviewStatus); // The status passed from the UI
                newEntry.put("timestamp", OffsetDateTime.now().toString());

                historyArray.add(newEntry);

                // Debugging Log
                logger.info("Updated Interview Status JSON for Candidate {}: {}",
                        interviewDetails.getCandidateId(), objectMapper.writeValueAsString(historyArray));

                interviewDetails.setInterviewStatus(objectMapper.writeValueAsString(historyArray));

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing interview status JSON", e);
            }
        }
        // Determine interview type if interviewLevel is null
        if (interviewDetails.getInterviewLevel() == null) {
            interviewDetails.setInterviewLevel(determineInterviewType(clientEmail, zoomLink));
        }

        // Handle internal vs. external interview constraints
        if ("External".equalsIgnoreCase(interviewDetails.getInterviewLevel())) {
            // External interview: Only update clientEmail and zoomLink if provided, don't nullify
            if (clientEmail != null) interviewDetails.setClientEmail(clientEmail);
            if (zoomLink != null) interviewDetails.setZoomLink(zoomLink);
        } else {
            // Internal interview: Ensure clientEmail and zoomLink are mandatory
            if (clientEmail == null || clientEmail.isEmpty()) {
                throw new IllegalArgumentException("Client email is required for Internal interviews.");
            }
            if (zoomLink == null || zoomLink.isEmpty()) {
                throw new IllegalArgumentException("Zoom link is required for Internal interviews.");
            }
            interviewDetails.setClientEmail(clientEmail);
            interviewDetails.setZoomLink(zoomLink);
        }
        // Update timestamp
        interviewDetails.setTimestamp(LocalDateTime.now());

        // Save updated candidate details
        interviewRepository.save(interviewDetails);
        logger.info("Interview details updated successfully for candidateId: {}", candidateId);


        // Prepare email content
        String formattedDate = (interviewDateTime != null) ? interviewDateTime.format(DateTimeFormatter.BASIC_ISO_DATE) : "N/A";
        String formattedTime = (interviewDateTime != null) ? interviewDateTime.format(DateTimeFormatter.ISO_TIME) : "N/A";
        String formattedDuration = (duration != null) ? duration + " minutes" : "N/A";
        String formattedZoomLink = (zoomLink != null && !zoomLink.isEmpty()) ? "<a href='" + zoomLink + "'>Click here to join</a>" : "N/A";

        String emailBody = String.format(
                "<p>Hello %s,</p>"
                        + "<p>Your interview has been rescheduled.</p>"
                        + "<ul>"
                        + "<li><b>New Date:</b> %s</li>"
                        + "<li><b>New Time:</b> %s</li>"
                        + "<li><b>Duration:</b> Approx. %s</li>"
                        + "<li><b>New Zoom Link:</b> %s</li>"
                        + "<li><b>Status:</b> %s</li>"
                        + "</ul>"
                        + "<p>Please confirm your availability.</p>"
                        + "<p>Best regards,<br>The Interview Team</p>",
                interviewDetails.getFullName(), formattedDate, formattedTime, formattedDuration, formattedZoomLink, interviewDetails.getInterviewStatus());

        String subject = "Interview Update for " + interviewDetails.getFullName();

        // Validate userEmail before sending the email
        String userEmailId = interviewDetails.getUserEmail();
        if (userEmailId == null || userEmailId.isEmpty()) {
            logger.error("User email is null or empty for candidateId: {}", candidateId);
        } else {
            try {
                Stream.of(interviewDetails.getCandidateEmailId(), interviewDetails.getClientEmail(), userEmailId)
                        .filter(email -> email != null && !email.isEmpty())
                        .forEach(email -> {
                            try {
                                logger.info("Sending email to: {}", email);
                                emailService.sendInterviewNotification(email, subject, emailBody);
                            } catch (Exception e) {
                                logger.error("Failed to send email notification to {}: {}", email, e.getMessage(), e);
                            }
                        });
            } catch (Exception e) {
                logger.error("Failed to send email notification: {}", e.getMessage(), e);
            }
        }

        // Return updated interview response
        return new InterviewResponseDto(
                true,
                "Interview updated successfully and notifications sent.",
                new InterviewResponseDto.InterviewPayload(
                        interviewDetails.getCandidateId(),
                        interviewDetails.getUserEmail(),
                        interviewDetails.getCandidateEmailId(),
                        interviewDetails.getClientEmail()
                ),
                null  // No errors
        );
    }
    public GetInterviewResponse getAllInterviews(){

       List<InterviewDetails> interviewDetails=interviewRepository.findAll();

        List<GetInterviewResponse.InterviewPayload> payloadList = interviewDetails.stream()
                .map(i -> new GetInterviewResponse.InterviewPayload(
                        i.getInterviewId(),
                        i.getJobId(),
                        i.getCandidateId(),
                        i.getFullName(),
                        i.getContactNumber(),
                        i.getCandidateEmailId(),
                        i.getUserEmail(),
                        i.getUserId(),
                        i.getInterviewDateTime(),
                        i.getDuration(),
                        i.getZoomLink(),
                        i.getTimestamp(),
                        i.getClientEmail(),
                        i.getClientName(),
                        i.getInterviewLevel(),
                        i.getInterviewStatus()
                ))
                .collect(Collectors.toList());
        return new GetInterviewResponse(true, "Interviews found", payloadList, null);
    }
    public GetInterviewResponse getInterviews(String candidateId){

        List<InterviewDetails> interviewDetails = interviewRepository.findInterviewsByCandidateId(candidateId);
       if(interviewDetails.isEmpty()){
           throw new InterviewNotScheduledException("No Interviews Scheduled For CandidateId "+candidateId);
       }else {
           List<GetInterviewResponse.InterviewPayload> payloadList = interviewDetails.stream()
                   .map(i -> new GetInterviewResponse.InterviewPayload(
                           i.getInterviewId(),
                           i.getJobId(),
                           i.getCandidateId(),
                           i.getFullName(),
                           i.getContactNumber(),
                           i.getCandidateEmailId(),
                           i.getUserEmail(),
                           i.getUserId(),
                           i.getInterviewDateTime(),
                           i.getDuration(),
                           i.getZoomLink(),
                           i.getTimestamp(),
                           i.getClientEmail(),
                           i.getClientName(),
                           i.getInterviewLevel(),
                           i.getInterviewStatus()

                   ))
                   .collect(Collectors.toList());
           return new GetInterviewResponse(true, "Interviews found", payloadList, null);
       }
    }
    @Transactional
    public void deleteInterview(String candidateId,String jobId) {
        logger.info("Received request to remove scheduled interview details for candidateId: {}", candidateId);

        InterviewDetails interview=interviewRepository.findInterviewsByCandidateIdAndJobId(candidateId,jobId);
        if (interview==null) {
            logger.error("Candidate with ID {} not found in database", candidateId);
            throw new NoInterviewsFoundException("No Scheduled Interview found for candidate ID: " + candidateId+" for JobId: "+jobId);
        }
        else  interviewRepository.delete(interview);

        logger.info("Scheduled interview details removed successfully for candidateId: {}", candidateId);
    }
    public GetInterviewResponse getInterviewsById(String interviewId) {

        Optional<InterviewDetails> optionalInterviewDetails = interviewRepository.findById(interviewId);
            if (optionalInterviewDetails.isEmpty()) {
                throw new NoInterviewsFoundException("Invalid Interview Id " + interviewId);
            }
            InterviewDetails i = optionalInterviewDetails.get();
            GetInterviewResponse.InterviewPayload payload = new GetInterviewResponse.InterviewPayload(
                    i.getInterviewId(),
                    i.getJobId(),
                    i.getCandidateId(),
                    i.getFullName(),
                    i.getContactNumber(),
                    i.getCandidateEmailId(),
                    i.getUserEmail(),
                    i.getUserId(),
                    i.getInterviewDateTime(),
                    i.getDuration(),
                    i.getZoomLink(),
                    i.getTimestamp(),
                    i.getClientEmail(),
                    i.getClientName(),
                    i.getInterviewLevel(),
                    i.getInterviewStatus()
            );
            return new GetInterviewResponse(true, "Interview found", List.of(payload), null);
        }
}