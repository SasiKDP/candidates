package com.profile.candidate.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.profile.candidate.dto.*;
import com.profile.candidate.exceptions.*;
import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.repository.CandidateRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CandidateService {

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private InterviewEmailService emailService;



    // Method to submit a candidate profile
    public CandidateResponseDto submitCandidate(CandidateDetails candidateDetails, MultipartFile resumeFile) throws IOException {
        // Validate input fields
        validateCandidateDetails(candidateDetails);

        // Check for duplicates
        checkForDuplicates(candidateDetails);

        // Optionally set userEmail and clientEmail if not already set
        setDefaultEmailsIfMissing(candidateDetails);

        // Process the resume file and set it as a BLOB
        if (resumeFile != null && !resumeFile.isEmpty()) {
            // Convert the resume file to byte[] and set it in the candidateDetails object
            byte[] resumeData = resumeFile.getBytes();
            candidateDetails.setResume(resumeData);  // Store the resume as binary data in DB

            // Save the resume to the file system and store the file path in DB
            String resumeFilePath = saveResumeToFileSystem(resumeFile);
            candidateDetails.setResumeFilePath(resumeFilePath);  // Store the file path in DB
        }
        if (!isValidFileType(resumeFile)) {
            throw new InvalidFileTypeException("Invalid file type. Only PDF, DOC and DOCX Files are allowed.");
        }

        // Save the candidate details to the database
        CandidateDetails savedCandidate = candidateRepository.save(candidateDetails);

        // ✅ After saving candidate, update the requirement status
        candidateRepository.updateRequirementStatus(savedCandidate.getJobId());

        // Create the payload with candidateId, employeeId, and jobId
        CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(
                savedCandidate.getCandidateId(),
                savedCandidate.getUserId(),
                savedCandidate.getJobId()
        );

// Return the response with status "Success" and the corresponding message
        return new CandidateResponseDto(
                "Success",  // Status
                "Candidate profile submitted successfully.",  // Message
                payload,  // Payload containing the candidateId, employeeId, and jobId
                null  // No error message
        );

    }
    private boolean isValidFileType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            String fileExtension = getFileExtension(fileName).toLowerCase();
            return fileExtension.equals("pdf") || fileExtension.equals("docx") || fileExtension.equals("doc");
        }
        return false;
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index > 0) {
            return fileName.substring(index + 1);
        }
        return "";
    }

    // Validate required candidate fields
    private void validateCandidateDetails(CandidateDetails candidateDetails) {
        if (candidateDetails.getFullName() == null || candidateDetails.getFullName().trim().isEmpty()) {
            throw new CandidateAlreadyExistsException("Full Name is required and cannot be empty.");
        }

        if (candidateDetails.getCandidateEmailId() == null || !candidateDetails.getCandidateEmailId().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new CandidateAlreadyExistsException("Invalid email format.");
        }

        if (candidateDetails.getContactNumber() == null || !candidateDetails.getContactNumber().matches("\\d{10}")) {
            throw new CandidateAlreadyExistsException("Contact number must be 10 digits.");
        }
    }

    // Check for duplicate candidate based on Email ID, Job ID, and Client Name
    private void checkForDuplicates(CandidateDetails candidateDetails) {
        Optional<CandidateDetails> existingCandidate =
                candidateRepository.findByCandidateEmailIdAndJobIdAndClientName(
                        candidateDetails.getCandidateEmailId(),
                        candidateDetails.getJobId(),
                        candidateDetails.getClientName());

        if (existingCandidate.isPresent()) {
            throw new CandidateAlreadyExistsException(
                    "Candidate with email ID " + existingCandidate.get().getCandidateEmailId() +
                            " has already been submitted for job " + existingCandidate.get().getJobId() +
                            " by client " + existingCandidate.get().getClientName()
            );
        }
        Optional<CandidateDetails> existingContactNumber =
                candidateRepository.findByContactNumberAndJobIdAndClientName(
                        candidateDetails.getContactNumber(),
                        candidateDetails.getJobId(),
                        candidateDetails.getClientName());

        if (existingContactNumber.isPresent()) {
            throw new CandidateAlreadyExistsException(
                    "Candidate with contact number " + existingContactNumber.get().getContactNumber() +
                            " has already been submitted for job " + existingContactNumber.get().getJobId() +
                            " by client " + existingContactNumber.get().getClientName()
            );
        }


    }


    // Set default values for userEmail and clientEmail if not provided
    private void setDefaultEmailsIfMissing(CandidateDetails candidateDetails) {
        if (candidateDetails.getUserEmail() == null) {
            candidateDetails.setUserEmail(candidateDetails.getUserEmail());  // Set to default or handle differently
        }

        if (candidateDetails.getClientEmail() == null) {
            candidateDetails.setClientEmail(candidateDetails.getClientEmail());  // Set to default or handle differently
        }
    }

    private String saveResumeToFileSystem(MultipartFile resumeFile) throws IOException {
        // Set the directory where resumes will be stored
        String resumeDirectory = "C:\\Users\\User\\Downloads"; // Ensure the directory path is correct and does not have extra quotes

        // Generate a unique file name using UUID to avoid conflicts
        String fileName = UUID.randomUUID().toString() + "-" + resumeFile.getOriginalFilename();
        Path filePath = Paths.get(resumeDirectory, fileName);

        // Create the directories if they don't exist
        Files.createDirectories(filePath.getParent());

        // Save the file to the disk
        Files.write(filePath, resumeFile.getBytes());

        // Return the path where the file is saved
        return filePath.toString();
    }

    private static final Logger logger = LoggerFactory.getLogger(CandidateService.class);




    private void saveFile(CandidateDetails candidate, MultipartFile file) throws IOException {
        // Define the path where files will be stored
        Path uploadsDirectory = Paths.get("uploads");

        // Check if the directory exists, if not, create it
        if (Files.notExists(uploadsDirectory)) {
            Files.createDirectories(uploadsDirectory);
            logger.info("Created directory: {}", uploadsDirectory.toString());
        }

        // Generate a filename that combines the candidateId and timestamp
        String filename = candidate.getCandidateId() + "-" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path targetPath = uploadsDirectory.resolve(filename);  // Save the file inside the "uploads" directory

        try {
            // Log the file saving action
            logger.info("Saving file to path: {}", targetPath);

            // Save the file to the directory
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Optionally save the file path in the database (for example, updating the candidate)
            candidate.setResumeFilePath(targetPath.toString());
            candidateRepository.save(candidate);

        } catch (IOException e) {
            logger.error("Failed to save file: {}", e.getMessage());
            throw new IOException("Failed to save file to path: " + targetPath, e);  // Throw exception to indicate failure
        }
    }

    public CandidateResponseDto resubmitCandidate(String candidateId, CandidateDetails updatedCandidateDetails, MultipartFile resumeFile) {
        try {
            // Fetch the existing candidate from the database
            Optional<CandidateDetails> existingCandidateOpt = candidateRepository.findById(candidateId);
            if (!existingCandidateOpt.isPresent()) {
                // Candidate not found, return error
                throw new CandidateNotFoundException(candidateId);
            }

            CandidateDetails existingCandidate = existingCandidateOpt.get();

            // ✅ If a resume file is provided, validate and update it
            if (resumeFile != null && !resumeFile.isEmpty()) {
                if (!isValidFileType(resumeFile)) {
                    throw new InvalidFileTypeException("Invalid file type. Only PDF, DOC, and DOCX are allowed.");
                }
                saveFile(existingCandidate, resumeFile);  // Save the file & update resume path
            }


            // Update candidate fields with the new data (e.g., name, contact, etc.)
            updateCandidateFields(existingCandidate, updatedCandidateDetails);

            // Save the resume file and update the candidate with the new file path
            saveFile(existingCandidate, resumeFile);  // This saves the file and updates the candidate's resumeFilePath

            // **Reset interview status**
            existingCandidate.setInterviewStatus(""); // Set it to empty string OR use null: existingCandidate.setInterviewStatus(null);

            // Save the updated candidate details (including the new resume file path)
            candidateRepository.save(existingCandidate);

            // Return a success response with the updated candidate details
            CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(
                    existingCandidate.getCandidateId(),
                    existingCandidate.getUserId(),
                    existingCandidate.getJobId()
            );

            return new CandidateResponseDto(
                    "Success",
                    "Candidate successfully updated",
                    payload,
                    null // No error message since no error occurred
            );

        } catch (CandidateNotFoundException ex) {
            // Custom handling for CandidateNotFoundException
            logger.error("Candidate with ID {} not found: {}", candidateId, ex.getMessage());
            throw ex; // Rethrow to be caught by GlobalExceptionHandler
        } catch (InvalidFileTypeException ex) {
            // Custom handling for InvalidFileTypeException
            logger.error("Invalid file type for resume: {}", ex.getMessage());
            throw ex; // Rethrow to be caught by GlobalExceptionHandler
        } catch (IOException ex) {
            // Specific handling for I/O issues, such as file saving errors
            logger.error("Failed to save resume file: {}", ex.getMessage());
            throw new RuntimeException("An error occurred while saving the resume file", ex);
        } catch (Exception ex) {
            // General error handling for any unexpected issues
            logger.error("An unexpected error occurred while resubmitting the candidate: {}", ex.getMessage());
            throw new RuntimeException("An unexpected error occurred while resubmitting the candidate", ex);
        }
    }


    public List<CandidateGetResponseDto> getAllSubmissions() {
        // Retrieve all candidates from the repository
        List<CandidateDetails> candidates = candidateRepository.findAll();

        // Check if there are no submissions
        if (candidates.isEmpty()) {
            throw new CandidateNotFoundException("No candidate submissions found.");
        }

        // Map CandidateDetails to CandidateGetResponseDto
        return candidates.stream()
                .map(CandidateGetResponseDto::new)  // Use the DTO constructor for mapping
                .collect(Collectors.toList());
    }




    // Method to get candidate submissions by userId
    public List<CandidateGetResponseDto> getSubmissionsByUserId(String userId) {
        List<CandidateDetails> candidates = candidateRepository.findByUserId(userId);

        if (candidates.isEmpty()) {
            throw new CandidateNotFoundException("No submissions found for userId: " + userId);
        }

        ObjectMapper objectMapper = new ObjectMapper();

        List<CandidateGetResponseDto> candidateDtos = candidates.stream().map(candidate -> {
            String latestInterviewStatus = "Not Scheduled"; // Default fallback
            String interviewStatusJson = candidate.getInterviewStatus();

            if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                String trimmedStatus = interviewStatusJson.trim();

                try {
                    // Only proceed if it's a JSON array
                    if (trimmedStatus.startsWith("[") && trimmedStatus.endsWith("]")) {
                        List<Map<String, Object>> statusHistory = objectMapper.readValue(trimmedStatus, List.class);

                        if (!statusHistory.isEmpty()) {
                            Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                    .filter(entry -> entry.containsKey("timestamp") && entry.containsKey("status"))
                                    .max(Comparator.comparing(entry ->
                                            OffsetDateTime.parse((String) entry.get("timestamp"))));

                            if (latestStatus.isPresent()) {
                                latestInterviewStatus = (String) latestStatus.get().get("status");
                            }
                        }
                    }

                    // 🚫 Do NOT accept plain string values anymore
                } catch (Exception e) {
                    System.err.println("Error parsing interview status JSON: " + e.getMessage());
                    // Ignore and keep as "Not Scheduled"
                }
            }

            Optional<String> clientNameOpt = candidateRepository.findClientNameByJobId(candidate.getJobId());
            String clientName = clientNameOpt.orElse(null);

            CandidateGetResponseDto dto = new CandidateGetResponseDto(candidate);
            dto.setInterviewStatus(latestInterviewStatus);
            dto.setClientName(clientName);

            return dto;
        }).collect(Collectors.toList());

        return candidateDtos;
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
    public boolean isInterviewScheduled(String candidateId, OffsetDateTime interviewDateTime) {
        // Query the repository to check if there's already an interview scheduled at that time
        Optional<CandidateDetails> existingInterview = candidateRepository.findByCandidateIdAndInterviewDateTime(candidateId, interviewDateTime);

        // Return true if an interview already exists, otherwise false
        return existingInterview.isPresent();
    }


    // Method to schedule an interview for a candidate

    public InterviewResponseDto scheduleInterview(String userId, String candidateId, OffsetDateTime interviewDateTime, Integer duration,
                                                  String zoomLink, String userEmail, String clientEmail,
                                                  String clientName, String interviewLevel, String externalInterviewDetails) {

        System.out.println("Starting to schedule interview for userId: " + userId + " and candidateId: " + candidateId);

        if (candidateId == null) {
            throw new CandidateNotFoundException("Candidate ID cannot be null for userId: " + userId);
        }

        // Retrieve candidate details
        CandidateDetails candidate = candidateRepository.findByCandidateIdAndUserId(candidateId, userId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found for userId: " + userId + " and candidateId: " + candidateId));

        // Ensure no interview is already scheduled
        if (candidate.getInterviewDateTime() != null) {
            throw new InterviewAlreadyScheduledException("An interview is already scheduled for candidate ID: " + candidateId);
        }

        // Update candidate details with provided information
        candidate.setUserEmail(userEmail);
        candidate.setClientEmail(clientEmail);
        setDefaultEmailsIfMissing(candidate);

        // Determine Interview Type if not provided
        if (interviewLevel == null || interviewLevel.isEmpty()) {
            interviewLevel = determineInterviewType(clientEmail, zoomLink);
        }
        candidate.setInterviewLevel(interviewLevel);

        // Handle external vs internal interview constraints
        if ("External".equalsIgnoreCase(interviewLevel)) {
            candidate.setClientEmail(clientEmail);
            candidate.setZoomLink(zoomLink);
        } else {
            if (clientEmail == null || clientEmail.isEmpty()) {
                throw new IllegalArgumentException("Client email is required for Internal interviews.");
            }
            if (zoomLink == null || zoomLink.isEmpty()) {
                throw new IllegalArgumentException("Zoom link is required for Internal interviews.");
            }
        }

        // Set interview details
        candidate.setInterviewDateTime(interviewDateTime);
        candidate.setDuration(duration);
        candidate.setTimestamp(LocalDateTime.now());
        candidate.setZoomLink(zoomLink);
        candidate.setClientName(clientName);
        candidate.setExternalInterviewDetails(externalInterviewDetails);

        // Do not set the interview status here, leave it as null for future updates

        // Save candidate details to the database
        try {
            candidateRepository.save(candidate);
            System.out.println("Candidate saved successfully.");
        } catch (Exception e) {
            throw new RuntimeException("Error while saving candidate data.", e);
        }

        // Send email notifications about the interview
        sendInterviewNotification(candidate);

        // Prepare the response with interview details
        InterviewResponseDto.InterviewPayload payload = new InterviewResponseDto.InterviewPayload(
                candidate.getCandidateId(),
                candidate.getUserEmail(),
                candidate.getCandidateEmailId(),
                candidate.getClientEmail()
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
    private void sendInterviewNotification(CandidateDetails candidate) {
        String subject = "Interview Scheduled for " + candidate.getFullName();

        String body = "<p>Hello " + candidate.getFullName() + ",</p>"
                + "<p>Hope you are doing well!</p>"
                + "<p>Thank you for your interest in the position <b>" + candidate.getInterviewLevel() + "</b> for our client <b>" + candidate.getClientName() + "</b>.</p>"
                + "<p>We're pleased to inform you that your profile has been shortlisted for screening.</p>"
                + "<p>Interview Details:</p>"
                + "<ul>"
                + "<li><b>Date:</b> " + candidate.getInterviewDateTime().format(DateTimeFormatter.BASIC_ISO_DATE) + "</li>"
                + "<li><b>Time:</b> " + candidate.getInterviewDateTime().format(DateTimeFormatter.ISO_TIME) + "</li>"
                + "<li><b>Duration:</b> Approx. " + candidate.getDuration() + " minutes</li>"
                + (candidate.getZoomLink() != null ? "<li><b>Join Zoom Meeting:</b> <a href='" + candidate.getZoomLink() + "'>Click here</a></li>" : "")
                + "</ul>"
                + "<p>Kindly confirm your availability by replying to this email.</p>"
                + "<p>Best regards,</p>"
                + "<p>The Interview Team</p>";

        // Validate emails before sending
        if (isValidEmail(candidate.getCandidateEmailId())) {
            emailService.sendInterviewNotification(candidate.getCandidateEmailId(), subject, body);
            System.out.println("Interview notification sent to candidate: " + candidate.getCandidateEmailId());
        } else {
            System.err.println("Invalid candidate email: " + candidate.getCandidateEmailId());
        }

        // If client email is provided, validate it and send
        if (candidate.getClientEmail() != null && isValidEmail(candidate.getClientEmail())) {
            emailService.sendInterviewNotification(candidate.getClientEmail(), subject, body);
            System.out.println("Interview notification sent to client: " + candidate.getClientEmail());
        } else if (candidate.getClientEmail() != null) {
            System.err.println("Invalid client email: " + candidate.getClientEmail());
        }

        // Always send to user email if provided and valid
        if (isValidEmail(candidate.getUserEmail())) {
            emailService.sendInterviewNotification(candidate.getUserEmail(), subject, body);
            System.out.println("Interview notification sent to user: " + candidate.getUserEmail());
        } else {
            System.err.println("Invalid user email: " + candidate.getUserEmail());
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

    public InterviewResponseDto updateScheduledInterview(
            String userId,
            String candidateId,
            OffsetDateTime interviewDateTime,
            Integer duration,
            String zoomLink,
            String userEmail,
            String clientEmail,
            String clientName,
            String interviewLevel,
            String externalInterviewDetails,
            String interviewStatus) {

        try {
            logger.info("Starting interview update for userId: {} and candidateId: {}", userId, candidateId);

            if (candidateId == null) {
                throw new CandidateNotFoundException("Candidate ID cannot be null for userId: " + userId);
            }

            CandidateDetails candidate = candidateRepository.findByCandidateIdAndUserId(candidateId, userId)
                    .orElseThrow(() -> new CandidateNotFoundException("Candidate not found for userId: " + userId + " and candidateId: " + candidateId));

            if (candidate.getInterviewDateTime() == null) {
                throw new InterviewNotScheduledException("No interview scheduled for candidate ID: " + candidateId);
            }

            // 1. **Validate the interview level** (clientEmail required for internal interviews)
            validateInterviewLevel(interviewLevel, clientEmail);

            // 🧠 Parse the existing status history (JSON) and check for redundant status
            String existingStatusJson = candidate.getInterviewStatus();
            if (existingStatusJson != null && !existingStatusJson.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    // Convert the JSON string to an array node
                    JsonNode statusArray = objectMapper.readTree(existingStatusJson);

                    // Check if the new status already exists in the history
                    for (JsonNode statusNode : statusArray) {
                        String existingStatus = statusNode.get("status").asText();
                        if (existingStatus != null && existingStatus.equalsIgnoreCase(interviewStatus)) {
                            // If a match is found, throw an exception
                            throw new IllegalArgumentException("Interview status is already set to the same value for candidate: " + candidateId);
                        }
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error processing interview status history JSON", e);
                }
            }

            // 🧠 Update the status history only after validation
            String latestStatus = "N/A";

            if (interviewStatus != null && !interviewStatus.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                ArrayNode historyArray;
                try {
                    // If the history exists, parse it
                    if (existingStatusJson != null && !existingStatusJson.isEmpty()) {
                        JsonNode jsonNode = objectMapper.readTree(existingStatusJson);
                        historyArray = jsonNode.isArray() ? (ArrayNode) jsonNode : objectMapper.createArrayNode();
                    } else {
                        historyArray = objectMapper.createArrayNode();
                    }

                    // Determine the next stage number
                    int nextStage = historyArray.size() + 1;

                    // Add the new entry
                    ObjectNode newEntry = objectMapper.createObjectNode();
                    newEntry.put("stage", nextStage);
                    newEntry.put("status", interviewStatus);
                    newEntry.put("timestamp", OffsetDateTime.now().toString());
                    historyArray.add(newEntry);

                    // Set the new status history
                    candidate.setInterviewStatus(objectMapper.writeValueAsString(historyArray));
                    latestStatus = interviewStatus;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error processing interview status JSON", e);
                }
            }

            // 🧾 Normalize interview status
            String normalizedStatus = latestStatus.trim().toLowerCase();

            // 🚫 Handle Cancelled Interview Status
            if ("cancelled".equals(normalizedStatus)) {
                candidate.setTimestamp(LocalDateTime.now());
                candidateRepository.save(candidate);
                logger.info("Interview marked as cancelled for candidateId: {}", candidateId);

                String subject = "Interview Cancelled for " + candidate.getFullName();
                String emailBody = String.format(
                        "<p>Hello %s,</p><p>We regret to inform you that your interview has been cancelled.</p>"
                                + "<p>If you have any questions, please contact support.</p><p>Best regards,<br>Interview Team</p>",
                        candidate.getFullName());

                String candidateEmail = candidate.getCandidateEmailId();
                if (candidateEmail != null && !candidateEmail.isEmpty()) {
                    try {
                        logger.info("Sending cancellation email to candidate: {}", candidateEmail);
                        emailService.sendInterviewNotification(candidateEmail, subject, emailBody);
                    } catch (Exception e) {
                        logger.error("Failed to send cancellation email to {}: {}", candidateEmail, e.getMessage(), e);
                    }
                }

                return new InterviewResponseDto(
                        true,
                        "Interview cancelled and notification sent to candidate.",
                        new InterviewResponseDto.InterviewPayload(
                                candidate.getCandidateId(),
                                candidate.getUserEmail(),
                                candidate.getCandidateEmailId(),
                                candidate.getClientEmail()
                        ),
                        null
                );
            }

            // ✅ Handle "Placed", "Selected", "Rejected" status-only updates
            if (Set.of("placed", "selected", "rejected").contains(normalizedStatus)) {
                candidate.setTimestamp(LocalDateTime.now());
                candidateRepository.save(candidate);
                logger.info("Only status '{}' updated for candidateId: {}", normalizedStatus, candidateId);

                return new InterviewResponseDto(
                        true,
                        "Interview status updated successfully.",
                        new InterviewResponseDto.InterviewPayload(
                                candidate.getCandidateId(),
                                candidate.getUserEmail(),
                                candidate.getCandidateEmailId(),
                                candidate.getClientEmail()
                        ),
                        null
                );
            }

            // 🛠 Update interview details if applicable
            if (interviewDateTime != null) candidate.setInterviewDateTime(interviewDateTime);
            if (duration != null) candidate.setDuration(duration);
            if (zoomLink != null && !zoomLink.isEmpty()) candidate.setZoomLink(zoomLink);
            if (userEmail != null && !userEmail.isEmpty()) candidate.setUserEmail(userEmail);
            if (clientEmail != null && !clientEmail.isEmpty()) candidate.setClientEmail(clientEmail);
            if (clientName != null && !clientName.isEmpty()) candidate.setClientName(clientName);
            if (interviewLevel != null && !interviewLevel.isEmpty()) candidate.setInterviewLevel(interviewLevel);
            if (externalInterviewDetails != null && !externalInterviewDetails.isEmpty()) candidate.setExternalInterviewDetails(externalInterviewDetails);

            // 🧠 Determine interview type dynamically based on presence of clientEmail
            boolean isInternalInterview = "internal".equalsIgnoreCase(interviewLevel)
                    || (interviewLevel == null && clientEmail != null && !clientEmail.trim().isEmpty());

            // 🔒 Validate based on interview type
            if (isInternalInterview) {
                // Internal interview: clientEmail is required
                if (clientEmail == null || clientEmail.trim().isEmpty()) {
                    throw new IllegalArgumentException("Client email is required for internal interviews.");
                }

                candidate.setClientEmail(clientEmail);
                candidate.setZoomLink((zoomLink != null && !zoomLink.trim().isEmpty()) ? zoomLink : null);
                candidate.setInterviewLevel("internal");

            } else {
                // External interview: both clientEmail and zoomLink are optional
                candidate.setClientEmail(null); // Ensure it's cleared
                candidate.setZoomLink((zoomLink != null && !zoomLink.trim().isEmpty()) ? zoomLink : null);
                candidate.setInterviewLevel("external");
            }

            candidate.setTimestamp(LocalDateTime.now());
            candidateRepository.save(candidate);

            // 📧 Email Notification
            String formattedDate = (interviewDateTime != null) ? interviewDateTime.format(DateTimeFormatter.BASIC_ISO_DATE) : "N/A";
            String formattedTime = (interviewDateTime != null) ? interviewDateTime.format(DateTimeFormatter.ISO_TIME) : "N/A";
            String formattedDuration = (duration != null) ? duration + " minutes" : "N/A";
            String formattedZoomLink = (zoomLink != null && !zoomLink.isEmpty()) ? "<a href='" + zoomLink + "'>Click here to join</a>" : "N/A";

            String emailBody = String.format(
                    "<p>Hello %s,</p>"
                            + "<p>Hope you are doing well!</p>"
                            + "<p>Thank you for your interest in the position <b>%s</b> for our client <b>%s</b>.</p>"
                            + "<p>We’re pleased to inform you that your profile has been shortlisted for screening.</p>"
                            + "<p><b>Interview Details:</b></p>"
                            + "<ul>"
                            + "<li><b>Date:</b> %s</li>"
                            + "<li><b>Time:</b> %s</li>"
                            + "<li><b>Duration:</b> Approx. %s</li>"
                            + "<li><b>Join Zoom Meeting:</b> %s</li>"
                            + "</ul>"
                            + "<p>Kindly confirm your availability by replying to this email.</p>"
                            + "<p>Best regards,<br>The Interview Team</p>",
                    candidate.getFullName(), candidate.getInterviewLevel(), candidate.getClientName(),
                    formattedDate, formattedTime, formattedDuration, formattedZoomLink
            );

            String subject = "Interview Update for " + candidate.getFullName();

            String userEmailId = candidate.getUserEmail();
            if (userEmailId != null && !userEmailId.isEmpty()) {
                try {
                    Stream.of(candidate.getCandidateEmailId(), candidate.getClientEmail(), userEmailId)
                            .filter(email -> email != null && !email.isEmpty())
                            .forEach(email -> {
                                try {
                                    logger.info("Sending email to: {}", email);
                                    emailService.sendInterviewNotification(email, subject, emailBody);
                                } catch (Exception e) {
                                    logger.error("Failed to send email to {}: {}", email, e.getMessage(), e);
                                }
                            });
                } catch (Exception e) {
                    logger.error("Failed to send interview email: {}", e.getMessage(), e);
                }
            }

            return new InterviewResponseDto(
                    true,
                    "Interview updated successfully and notifications sent.",
                    new InterviewResponseDto.InterviewPayload(
                            candidate.getCandidateId(),
                            candidate.getUserEmail(),
                            candidate.getCandidateEmailId(),
                            candidate.getClientEmail()
                    ),
                    null
            );
        } catch (Exception e) {
            logger.error("Error while updating interview: {}", e.getMessage(), e);
            return new InterviewResponseDto(
                    false,
                    "An error occurred while updating the interview. " + e.getMessage(),
                    null,
                    null
            );
        }
    }

    // Method to validate the interview level
    private void validateInterviewLevel(String interviewLevel, String clientEmail) {
        if ("internal".equalsIgnoreCase(interviewLevel)) {
            // For internal interviews, clientEmail is required
            if (clientEmail == null || clientEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("Client email is required for internal interviews.");
            }
        } else if ("external".equalsIgnoreCase(interviewLevel)) {
            // For external interviews, clientEmail is not required
            if (clientEmail != null && !clientEmail.trim().isEmpty()) {
                logger.warn("For external interviews, clientEmail is not required.");
            }
        } else {
            throw new IllegalArgumentException("Invalid interview level: " + interviewLevel);
        }
    }




    public InterviewResponseDto updateScheduledInterviewWithoutUserId(
            String candidateId,
            OffsetDateTime interviewDateTime,
            Integer duration,
            String zoomLink,
            String userEmail,
            String clientEmail,
            String clientName,
            String interviewLevel,
            String externalInterviewDetails,
            String interviewStatus) {

        logger.info("Starting interview update for candidateId: {}", candidateId);

        if (candidateId == null) {
            throw new CandidateNotFoundException("Candidate ID cannot be null.");
        }

        CandidateDetails candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found for candidateId: " + candidateId));

        if (candidate.getInterviewDateTime() == null) {
            throw new InterviewNotScheduledException("No interview scheduled for candidate ID: " + candidateId);
        }

        // 🧠 Determine interview type dynamically before any update
        boolean isInternalInterview = "internal".equalsIgnoreCase(interviewLevel)
                || (interviewLevel == null && clientEmail != null && !clientEmail.trim().isEmpty());

        // 🔒 Validation based on interview type
        if (isInternalInterview && (clientEmail == null || clientEmail.trim().isEmpty())) {
            throw new IllegalArgumentException("Client email is required for internal interviews.");
        }

        // 🧠 Update status history JSON
        String latestStatus = "N/A";
        if (interviewStatus != null && !interviewStatus.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode historyArray;
            try {
                String existingStatus = candidate.getInterviewStatus();
                if (existingStatus != null && !existingStatus.isEmpty()) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(existingStatus);
                        historyArray = jsonNode.isArray() ? (ArrayNode) jsonNode : objectMapper.createArrayNode();
                    } catch (JsonProcessingException e) {
                        historyArray = objectMapper.createArrayNode();
                    }
                } else {
                    historyArray = objectMapper.createArrayNode();
                }

                int nextStage = historyArray.size() + 1;
                ObjectNode newEntry = objectMapper.createObjectNode();
                newEntry.put("stage", nextStage);
                newEntry.put("status", interviewStatus);
                newEntry.put("timestamp", OffsetDateTime.now().toString());
                historyArray.add(newEntry);

                candidate.setInterviewStatus(objectMapper.writeValueAsString(historyArray));
                latestStatus = interviewStatus;
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing interview status JSON", e);
            }
        }

        // Normalize status
        String normalizedStatus = latestStatus.trim().toLowerCase();

        // 🚫 CANCELLED → Email candidate only
        if ("cancelled".equals(normalizedStatus)) {
            candidate.setTimestamp(LocalDateTime.now());
            candidateRepository.save(candidate);
            logger.info("Interview marked as cancelled for candidateId: {}", candidateId);

            String subject = "Interview Cancelled for " + candidate.getFullName();
            String emailBody = String.format(
                    "<p>Hello %s,</p><p>We regret to inform you that your interview has been cancelled.</p>"
                            + "<p>If you have any questions, please contact support.</p><p>Best regards,<br>Interview Team</p>",
                    candidate.getFullName()
            );

            String candidateEmail = candidate.getCandidateEmailId();
            if (candidateEmail != null && !candidateEmail.isEmpty()) {
                try {
                    logger.info("Sending cancellation email to candidate: {}", candidateEmail);
                    emailService.sendInterviewNotification(candidateEmail, subject, emailBody);
                } catch (Exception e) {
                    logger.error("Failed to send cancellation email to {}: {}", candidateEmail, e.getMessage(), e);
                }
            }

            return new InterviewResponseDto(
                    true,
                    "Interview cancelled and notification sent to candidate.",
                    new InterviewResponseDto.InterviewPayload(
                            candidate.getCandidateId(),
                            candidate.getUserEmail(),
                            candidate.getCandidateEmailId(),
                            candidate.getClientEmail()
                    ),
                    null
            );
        }

        // ✅ Status-only updates
        if (Set.of("placed", "selected", "rejected").contains(normalizedStatus)) {
            candidate.setTimestamp(LocalDateTime.now());
            candidateRepository.save(candidate);
            logger.info("Only status '{}' updated for candidateId: {}", normalizedStatus, candidateId);

            return new InterviewResponseDto(
                    true,
                    "Interview status updated successfully.",
                    new InterviewResponseDto.InterviewPayload(
                            candidate.getCandidateId(),
                            candidate.getUserEmail(),
                            candidate.getCandidateEmailId(),
                            candidate.getClientEmail()
                    ),
                    null
            );
        }

        // ✅ Full update for other cases
        if (interviewDateTime != null) candidate.setInterviewDateTime(interviewDateTime);
        if (duration != null) candidate.setDuration(duration);
        if (userEmail != null && !userEmail.isEmpty()) candidate.setUserEmail(userEmail);
        if (clientName != null && !clientName.isEmpty()) candidate.setClientName(clientName);
        if (interviewLevel != null && !interviewLevel.isEmpty()) candidate.setInterviewLevel(interviewLevel);
        if (externalInterviewDetails != null && !externalInterviewDetails.isEmpty()) candidate.setExternalInterviewDetails(externalInterviewDetails);

        // Reapply logic for internal vs external settings
        if (isInternalInterview) {
            candidate.setClientEmail(clientEmail);
            candidate.setZoomLink((zoomLink != null && !zoomLink.trim().isEmpty()) ? zoomLink : null);
            candidate.setInterviewLevel("internal");
        } else {
            candidate.setClientEmail(null);
            candidate.setZoomLink((zoomLink != null && !zoomLink.trim().isEmpty()) ? zoomLink : null);
            candidate.setInterviewLevel("external");
        }

        candidate.setTimestamp(LocalDateTime.now());
        candidateRepository.save(candidate);

        // 📧 Send email
        String formattedDate = (interviewDateTime != null) ? interviewDateTime.format(DateTimeFormatter.BASIC_ISO_DATE) : "N/A";
        String formattedTime = (interviewDateTime != null) ? interviewDateTime.format(DateTimeFormatter.ISO_TIME) : "N/A";
        String formattedDuration = (duration != null) ? duration + " minutes" : "N/A";
        String formattedZoomLink = (candidate.getZoomLink() != null && !candidate.getZoomLink().isEmpty()) ? "<a href='" + candidate.getZoomLink() + "'>Click here to join</a>" : "N/A";

        String emailBody = String.format(
                "<p>Hello %s,</p>"
                        + "<p>Hope you are doing well!</p>"
                        + "<p>Thank you for your interest in the position <b>%s</b> for our client <b>%s</b>.</p>"
                        + "<p>We’re pleased to inform you that your profile has been shortlisted for screening.</p>"
                        + "<p><b>Interview Details:</b></p>"
                        + "<ul>"
                        + "<li><b>Date:</b> %s</li>"
                        + "<li><b>Time:</b> %s</li>"
                        + "<li><b>Duration:</b> Approx. %s</li>"
                        + "<li><b>Join Zoom Meeting:</b> %s</li>"
                        + "</ul>"
                        + "<p>Kindly confirm your availability by replying to this email.</p>"
                        + "<p>Best regards,<br>The Interview Team</p>",
                candidate.getFullName(), candidate.getInterviewLevel(), candidate.getClientName(), formattedDate, formattedTime,
                formattedDuration, formattedZoomLink
        );

        String subject = "Interview Update for " + candidate.getFullName();

        String userEmailId = candidate.getUserEmail();
        if (userEmailId != null && !userEmailId.isEmpty()) {
            try {
                Stream.of(candidate.getCandidateEmailId(), candidate.getClientEmail(), userEmailId)
                        .filter(email -> email != null && !email.isEmpty())
                        .forEach(email -> {
                            try {
                                logger.info("Sending email to: {}", email);
                                emailService.sendInterviewNotification(email, subject, emailBody);
                            } catch (Exception e) {
                                logger.error("Failed to send email to {}: {}", email, e.getMessage(), e);
                            }
                        });
            } catch (Exception e) {
                logger.error("Failed to send interview email: {}", e.getMessage(), e);
            }
        }

        return new InterviewResponseDto(
                true,
                "Interview updated successfully and notifications sent.",
                new InterviewResponseDto.InterviewPayload(
                        candidate.getCandidateId(),
                        candidate.getUserEmail(),
                        candidate.getCandidateEmailId(),
                        candidate.getClientEmail()
                ),
                null
        );
    }


    public List<GetInterviewResponseDto> getAllScheduledInterviews() {
        // Fetch all candidates
        List<CandidateDetails> candidates = candidateRepository.findAll();
        List<GetInterviewResponseDto> response = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        for (CandidateDetails interview : candidates) {
            // Skip candidates where interview is not scheduled (i.e., interviewDateTime is null)
            if (interview.getInterviewDateTime() == null) {
                continue;  // Skip this iteration if interview is not scheduled
            }

            // Get the interview status (which can be a plain string or JSON)
            String interviewStatusJson = interview.getInterviewStatus();
            String latestInterviewStatus = null;

            // Handle interviewStatus as a JSON or plain text
            if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                try {
                    // Check if it's a valid JSON format
                    if (interviewStatusJson.trim().startsWith("{") || interviewStatusJson.trim().startsWith("[")) {
                        // Deserialize the JSON into a List of Maps
                        List<Map<String, Object>> statusHistory = objectMapper.readValue(interviewStatusJson, List.class);
                        // Extract the latest status from the history
                        if (!statusHistory.isEmpty()) {
                            Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                    .max(Comparator.comparing(entry -> (String) entry.get("timestamp")));  // Sorting by timestamp
                            if (latestStatus.isPresent()) {
                                latestInterviewStatus = (String) latestStatus.get().get("status");
                            }
                        }
                    } else {
                        // If it's a plain string, just treat it as the status
                        latestInterviewStatus = interviewStatusJson;
                    }
                } catch (JsonParseException e) {
                    // Handle invalid JSON (in case there's an error parsing the interviewStatus)
                    System.err.println("Error parsing interview status JSON: Invalid JSON format detected.");
                    latestInterviewStatus = interviewStatusJson;  // Treat it as plain string if JSON parsing fails
                } catch (IOException e) {
                    // Handle other IO issues
                    System.err.println("Error reading interview status: " + e.getMessage());
                }
            }

            // Add the candidate's interview details regardless of the status (we do not filter out any specific status)
            GetInterviewResponseDto dto = new GetInterviewResponseDto(
                    interview.getJobId(),
                    interview.getCandidateId(),
                    interview.getFullName(),
                    interview.getContactNumber(),
                    interview.getCandidateEmailId(),
                    interview.getUserEmail(),
                    interview.getUserId(),
                    interview.getInterviewDateTime(),
                    interview.getDuration(),
                    interview.getZoomLink(),
                    interview.getTimestamp(),
                    interview.getClientEmail(),
                    interview.getClientName(),
                    interview.getInterviewLevel(),
                    latestInterviewStatus  // Include the latest interview status (which could be Scheduled, Rescheduled, etc.)
            );
            response.add(dto);
        }

        return response;
    }


    public List<GetInterviewResponseDto> getAllScheduledInterviewsByUserId(String userId) {
        List<CandidateDetails> candidates = candidateRepository.findByUserId(userId);
        List<GetInterviewResponseDto> response = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        for (CandidateDetails interview : candidates) {
            String interviewStatusJson = interview.getInterviewStatus();
            System.out.println("Interview Status JSON (Raw): " + interviewStatusJson);

            String latestInterviewStatus = null;

            if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                try {
                    if (interviewStatusJson.trim().startsWith("[") && interviewStatusJson.trim().endsWith("]")) {
                        List<Map<String, Object>> statusHistory = objectMapper.readValue(interviewStatusJson, List.class);

                        if (!statusHistory.isEmpty()) {
                            Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                    .max(Comparator.comparing(entry ->
                                            OffsetDateTime.parse((String) entry.get("timestamp"))));

                            if (latestStatus.isPresent()) {
                                latestInterviewStatus = (String) latestStatus.get().get("status");
                            }
                        }
                    } else {
                        latestInterviewStatus = interviewStatusJson.trim();
                    }
                } catch (JsonProcessingException e) {
                    System.err.println("Error parsing interview status JSON: " + e.getMessage());
                    e.printStackTrace();
                    latestInterviewStatus = "Error Parsing Status";
                } catch (DateTimeParseException e) {
                    System.err.println("Error parsing timestamp: " + e.getMessage());
                    e.printStackTrace();
                    latestInterviewStatus = "Invalid Timestamp";
                }
            }

            // ✅ Filter only by non-null interviewDateTime
            if (interview.getInterviewDateTime() != null) {
                GetInterviewResponseDto dto = new GetInterviewResponseDto(
                        interview.getJobId(),
                        interview.getCandidateId(),
                        interview.getFullName(),
                        interview.getContactNumber(),
                        interview.getCandidateEmailId(),
                        interview.getUserEmail(),
                        interview.getUserId(),
                        interview.getInterviewDateTime(),
                        interview.getDuration(),
                        interview.getZoomLink(),
                        interview.getTimestamp(),
                        interview.getClientEmail(),
                        interview.getClientName(),
                        interview.getInterviewLevel(),
                        latestInterviewStatus
                );

                response.add(dto);
            }
        }

        return response;
    }


    // Method to update the candidate fields with new values
    private void updateCandidateFields(CandidateDetails existingCandidate, CandidateDetails updatedCandidateDetails) {
        if (updatedCandidateDetails.getJobId() != null) existingCandidate.setJobId(updatedCandidateDetails.getJobId());
        if (updatedCandidateDetails.getUserId() != null) existingCandidate.setUserId(updatedCandidateDetails.getUserId());
        if (updatedCandidateDetails.getFullName() != null) existingCandidate.setFullName(updatedCandidateDetails.getFullName());
        if (updatedCandidateDetails.getCandidateEmailId() != null)
            existingCandidate.setCandidateEmailId(updatedCandidateDetails.getCandidateEmailId());
        if (updatedCandidateDetails.getContactNumber() != null) existingCandidate.setContactNumber(updatedCandidateDetails.getContactNumber());
        if (updatedCandidateDetails.getQualification() != null) existingCandidate.setQualification(updatedCandidateDetails.getQualification());
        if (updatedCandidateDetails.getTotalExperience() != 0)
            existingCandidate.setTotalExperience(updatedCandidateDetails.getTotalExperience());
        if (updatedCandidateDetails.getCurrentCTC() != null) existingCandidate.setCurrentCTC(updatedCandidateDetails.getCurrentCTC());
        if (updatedCandidateDetails.getExpectedCTC() != null)
            existingCandidate.setExpectedCTC(updatedCandidateDetails.getExpectedCTC());
        if (updatedCandidateDetails.getNoticePeriod() != null)
            existingCandidate.setNoticePeriod(updatedCandidateDetails.getNoticePeriod());
        if (updatedCandidateDetails.getCurrentLocation() != null)
            existingCandidate.setCurrentLocation(updatedCandidateDetails.getCurrentLocation());
        if (updatedCandidateDetails.getPreferredLocation() != null)
            existingCandidate.setPreferredLocation(updatedCandidateDetails.getPreferredLocation());
        if (updatedCandidateDetails.getSkills() != null) existingCandidate.setSkills(updatedCandidateDetails.getSkills());
        if (updatedCandidateDetails.getCommunicationSkills() != null)
            existingCandidate.setCommunicationSkills(updatedCandidateDetails.getCommunicationSkills());
        if (updatedCandidateDetails.getRequiredTechnologiesRating() != null)
            existingCandidate.setRequiredTechnologiesRating(updatedCandidateDetails.getRequiredTechnologiesRating());
        if (updatedCandidateDetails.getOverallFeedback() != null)
            existingCandidate.setOverallFeedback(updatedCandidateDetails.getOverallFeedback());
        if (updatedCandidateDetails.getRelevantExperience() != 0)
            existingCandidate.setRelevantExperience(updatedCandidateDetails.getRelevantExperience());
        if (updatedCandidateDetails.getCurrentOrganization() != null)
            existingCandidate.setCurrentOrganization(updatedCandidateDetails.getCurrentOrganization());
    }

    @Transactional
    public DeleteCandidateResponseDto deleteCandidateById(String candidateId) {
        logger.info("Received request to delete candidate with candidateId: {}", candidateId);

        // Fetch candidate details before deletion
        CandidateDetails candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> {
                    logger.error("Candidate with ID {} not found", candidateId);
                    return new CandidateNotFoundException("Candidate not found with id: " + candidateId);
                });

        logger.info("Candidate found: {}, Proceeding with deletion", candidate.getFullName());

        // Store the candidate details before deletion
        String candidateIdBeforeDelete = candidate.getCandidateId();
        String candidateNameBeforeDelete = candidate.getFullName();

        // Delete the candidate from the repository
        candidateRepository.delete(candidate);
        logger.info("Candidate with ID {} deleted successfully", candidateId);

        // Prepare the response with candidate details
        DeleteCandidateResponseDto.Payload payload = new DeleteCandidateResponseDto.Payload(candidateIdBeforeDelete, candidateNameBeforeDelete);

        return new DeleteCandidateResponseDto("Success",
                "Candidate deleted successfully",
                payload,
                null);
    }
    @Transactional
    public void deleteInterview(String candidateId) {
        logger.info("Received request to remove scheduled interview details for candidateId: {}", candidateId);

        // Fetch all candidate records associated with candidateId
        List<CandidateDetails> candidates = candidateRepository.findAllByCandidateId(candidateId);

        if (candidates.isEmpty()) {
            logger.error("Candidate with ID {} not found in database", candidateId);
            throw new InterviewNotScheduledException("No Scheduled Interview found for candidate ID: " + candidateId);
        }

        boolean interviewRemoved = false;

        for (CandidateDetails candidate : candidates) {
            logger.info("Processing Candidate: {} (Candidate ID: {}), Interview DateTime: {}",
                    candidate.getFullName(), candidate.getCandidateId(), candidate.getInterviewDateTime());

            if (candidate.getInterviewDateTime() == null) {
                logger.warn("No interview scheduled for candidate ID: {} and job ID: {}", candidateId, candidate.getJobId());
                continue;
            }

            // ✅ Remove only interview-related fields
            candidate.setInterviewDateTime(null);
            candidate.setDuration(null);
            candidate.setZoomLink(null);
            candidate.setClientName(null);
            candidate.setInterviewLevel(null);
            candidate.setClientEmail(null);
            candidate.setTimestamp(null);
            candidate.setExternalInterviewDetails(null);
            candidate.setInterviewStatus("NOT SCHEDULED");

            interviewRemoved = true;
        }

        if (!interviewRemoved) {
            logger.warn("No scheduled interviews found for candidate ID: {}", candidateId);
            throw new InterviewNotScheduledException("No Scheduled Interview found for candidate ID: " + candidateId);
        }

        // ✅ Save all updated records
        candidateRepository.saveAll(candidates);

        logger.info("Scheduled interview details removed successfully for candidateId: {}", candidateId);
    }

    // Initialize ObjectMapper here
    private final ObjectMapper objectMapper = new ObjectMapper();


    public List<Map<String, Object>> getScheduledCandidates(String candidateId) {
        // Fetch all candidate records for the given candidateId
        List<CandidateDetails> candidates = candidateRepository.findAllByCandidateId(candidateId);

        if (candidates.isEmpty()) {
            logger.warn("No scheduled interview found for candidate ID: {}", candidateId);
            return new ArrayList<>(); // Return an empty list instead of throwing an exception
        }

        // Map to group interview history by jobId
        Map<String, List<Map<String, Object>>> groupedInterviewHistory = new LinkedHashMap<>();
        Map<String, Set<String>> jobIdRounds = new LinkedHashMap<>();  // To track unique rounds for each jobId

        for (CandidateDetails candidate : candidates) {
            String interviewStatusJson = candidate.getInterviewStatus();
            String jobId = candidate.getJobId();  // Extract jobId for the candidate

            // Log interview status JSON for debugging purposes
            logger.debug("Interview Status JSON for Candidate {}: {}", candidateId, interviewStatusJson);

            if (interviewStatusJson != null && !interviewStatusJson.isEmpty()) {
                try {
                    JsonNode rootNode = objectMapper.readTree(interviewStatusJson);

                    if (rootNode.isArray()) {
                        for (JsonNode entry : rootNode) {
                            Map<String, Object> interviewEntry = new LinkedHashMap<>();

                            // Extract values from JSON or fallback to entity values
                            interviewEntry.put("candidateName", getJsonValue(entry, "candidateName", candidate.getFullName()));
                            interviewEntry.put("clientName", getJsonValue(entry, "clientName", candidate.getClientName()));
                            interviewEntry.put("jobId", jobId);  // Ensure jobId is part of the response

                            // Ensure "stage" is handled correctly, use "N/A" if missing
                            String stage = getJsonValue(entry, "stage", "N/A"); // Now checking "stage" instead of "round"
                            interviewEntry.put("stage", stage); // Ensure it's treated as the "stage"

                            interviewEntry.put("interviewLevel", getJsonValue(entry, "interviewLevel", candidate.getInterviewLevel()));
                            interviewEntry.put("interviewStatus", getJsonValue(entry, "status", "N/A"));

                            String timestamp = getJsonValue(entry, "timestamp", OffsetDateTime.now().toString());
                            interviewEntry.put("timestamp", timestamp);

                            // Use the getInterviewDateTime() method to fetch the interview date and time
                            if (candidate.getInterviewDateTime() != null) {
                                // Convert the LocalDateTime to String in ISO-8601 format
                                String interviewDateTime = candidate.getInterviewDateTime().atZoneSameInstant(ZoneId.systemDefault())
                                        .withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                                interviewEntry.put("interviewDateTime", interviewDateTime);
                            } else {
                                interviewEntry.put("interviewDateTime", "N/A"); // Default if interviewDateTime is null
                            }

                            // Add interview entry to the grouped interview history by jobId
                            groupedInterviewHistory
                                    .computeIfAbsent(jobId, k -> new ArrayList<>())  // Ensure list exists for jobId
                                    .add(interviewEntry);

                            // Track unique rounds for each jobId (i.e., unique stages)
                            jobIdRounds.computeIfAbsent(jobId, k -> new HashSet<>()).add(stage);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error parsing interviewStatus JSON for candidate ID {}: {}", candidateId, e.getMessage());
                }
            }
        }

        // Add the total interview rounds count for each jobId
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedInterviewHistory.entrySet()) {
            String jobId = entry.getKey();
            int totalRounds = jobIdRounds.get(jobId).size();  // Get the count of unique rounds

            // Create summary for interview rounds count
            Map<String, Object> interviewSummary = new LinkedHashMap<>();
            interviewSummary.put("totalInterviewRounds", totalRounds);  // Total rounds for this jobId

            // Add the interview rounds summary at the end of each jobId's interview history
            entry.getValue().add(interviewSummary);
        }

        // Flatten the grouped interview history into a final list to return
        List<Map<String, Object>> finalInterviewHistory = new ArrayList<>();
        groupedInterviewHistory.forEach((jobId, history) -> finalInterviewHistory.addAll(history));

        // Improved sorting logic with better timestamp handling
        finalInterviewHistory.sort((entry1, entry2) -> {
            try {
                // If either entry is a summary entry (containing totalInterviewRounds), place it at the end
                if (entry1.containsKey("totalInterviewRounds")) {
                    return 1;
                }
                if (entry2.containsKey("totalInterviewRounds")) {
                    return -1;
                }

                String timestamp1 = (String) entry1.get("timestamp");
                String timestamp2 = (String) entry2.get("timestamp");

                // Log timestamps for debugging
                logger.debug("Sorting - Timestamp 1: {}", timestamp1);
                logger.debug("Sorting - Timestamp 2: {}", timestamp2);

                // Initialize parsed timestamps
                OffsetDateTime parsedTimestamp1 = null;
                OffsetDateTime parsedTimestamp2 = null;

                // Validate and parse timestamp1
                if (timestamp1 != null && !timestamp1.isEmpty()) {
                    try {
                        parsedTimestamp1 = OffsetDateTime.parse(timestamp1);
                    } catch (DateTimeParseException e) {
                        logger.error("Invalid timestamp format: {}", timestamp1);
                        // Keep parsedTimestamp1 as null
                    }
                }

                // Validate and parse timestamp2
                if (timestamp2 != null && !timestamp2.isEmpty()) {
                    try {
                        parsedTimestamp2 = OffsetDateTime.parse(timestamp2);
                    } catch (DateTimeParseException e) {
                        logger.error("Invalid timestamp format: {}", timestamp2);
                        // Keep parsedTimestamp2 as null
                    }
                }

                // Compare based on parsed timestamps
                if (parsedTimestamp1 == null && parsedTimestamp2 == null) {
                    return 0; // Both timestamps are invalid, maintain original order
                } else if (parsedTimestamp1 == null) {
                    return 1; // First timestamp is invalid, move it to the end
                } else if (parsedTimestamp2 == null) {
                    return -1; // Second timestamp is invalid, move it to the end
                } else {
                    return parsedTimestamp1.compareTo(parsedTimestamp2); // Both valid, compare normally
                }
            } catch (Exception e) {
                logger.error("Error in timestamp comparison: {}", e.getMessage());
                return 0; // If comparison fails, maintain the original order
            }
        });

        return finalInterviewHistory;
    }

    private String getJsonValue(JsonNode node, String fieldName, String defaultValue) {
        if (node == null) {
            logger.error("JsonNode is null for field: {}", fieldName); // Log for debugging
            return defaultValue; // Return the default value if the node is null
        }
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return defaultValue;
    }

    public List<CandidateGetResponseDto> getSubmissionsByUserIdAndDateRange(LocalDate startDate, LocalDate endDate) {
        // 💥 First check: Total range must not exceed 1 month
        if (ChronoUnit.DAYS.between(startDate, endDate) > 31) {
            throw new DateRangeValidationException("Date range must not exceed one month.");
        }

        // 💥 Second check: End date must not be before start date
        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
        }

        // 💥 Third check: Start date must be within 1 month from today
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        if (startDate.isBefore(oneMonthAgo)) {
            throw new DateRangeValidationException("Start date must be within the last 1 month.");
        }

        // ✅ Only hit DB after validations pass
        List<CandidateDetails> candidates = candidateRepository.findByProfileReceivedDateBetween(startDate, endDate);

        if (candidates.isEmpty()) {
            throw new CandidateNotFoundException("No submissions found for Candidates between " + startDate + " and " + endDate);
        }

        ObjectMapper objectMapper = new ObjectMapper();

        return candidates.stream().map(candidate -> {
            String latestInterviewStatus = "Not Scheduled";
            String interviewStatusJson = candidate.getInterviewStatus();

            if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                try {
                    if (interviewStatusJson.trim().startsWith("[") || interviewStatusJson.trim().startsWith("{")) {
                        List<Map<String, Object>> statusHistory = objectMapper.readValue(interviewStatusJson, List.class);

                        if (!statusHistory.isEmpty()) {
                            Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                    .max(Comparator.comparing(entry -> (String) entry.get("timestamp")));
                            if (latestStatus.isPresent()) {
                                latestInterviewStatus = (String) latestStatus.get().get("status");
                            }
                        }
                    } else {
                        latestInterviewStatus = interviewStatusJson;
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing interview status JSON: " + e.getMessage());
                    latestInterviewStatus = interviewStatusJson;
                }
            }

            CandidateGetResponseDto dto = new CandidateGetResponseDto(candidate);
            dto.setInterviewStatus(latestInterviewStatus);
            return dto;
        }).collect(Collectors.toList());
    }


    public List<GetInterviewResponseDto> getScheduledInterviewsByDateOnly(LocalDate startDate, LocalDate endDate) {
        // Validation: Ensure date range is within the last 1 month
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        if (startDate.isBefore(oneMonthAgo)) {
            throw new DateRangeValidationException("Start date must be within the last 1 month.");
        }
        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date must not be before the start date.");
        }
        // Check if the date range exceeds one month
        // Use ChronoUnit to count exact days
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 31) { // Strict 1-month cap
            throw new DateRangeValidationException("Date range must not exceed one month.");
        }
        List<CandidateDetails> candidates = candidateRepository.findScheduledInterviewsByDateOnly(startDate, endDate);
        List<GetInterviewResponseDto> response = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        for (CandidateDetails interview : candidates) {
            String interviewStatusJson = interview.getInterviewStatus();
            String latestInterviewStatus = null;

            if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                try {
                    if (interviewStatusJson.trim().startsWith("{") || interviewStatusJson.trim().startsWith("[")) {
                        List<Map<String, Object>> statusHistory = objectMapper.readValue(interviewStatusJson, List.class);
                        if (!statusHistory.isEmpty()) {
                            Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                    .max(Comparator.comparing(entry -> (String) entry.get("timestamp")));
                            latestInterviewStatus = latestStatus.map(status -> (String) status.get("status")).orElse(null);
                        }
                    } else {
                        latestInterviewStatus = interviewStatusJson;
                    }
                } catch (Exception e) {
                    latestInterviewStatus = interviewStatusJson;
                }
            }

            response.add(new GetInterviewResponseDto(
                    interview.getJobId(),
                    interview.getCandidateId(),
                    interview.getFullName(),
                    interview.getContactNumber(),
                    interview.getCandidateEmailId(),
                    interview.getUserEmail(),
                    interview.getUserId(),
                    interview.getInterviewDateTime(),
                    interview.getDuration(),
                    interview.getZoomLink(),
                    interview.getTimestamp(),
                    interview.getClientEmail(),
                    interview.getClientName(),
                    interview.getInterviewLevel(),
                    latestInterviewStatus
            ));
        }

        return response;
    }



}



