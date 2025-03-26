package com.profile.candidate.service;

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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
        // Retrieve candidates by userId from the repository
        List<CandidateDetails> candidates = candidateRepository.findByUserId(userId);

        // If no candidates are found, throw a CandidateNotFoundException
        if (candidates.isEmpty()) {
            throw new CandidateNotFoundException("No submissions found for userId: " + userId);
        }

        // Map the list of CandidateDetails to List<CandidateGetResponseDto>
        List<CandidateGetResponseDto> candidateDtos = candidates.stream()
                .map(CandidateGetResponseDto::new)  // Convert each CandidateDetails to CandidateGetResponseDto
                .collect(Collectors.toList());

        // Return the list of CandidateGetResponseDto
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

        CandidateDetails candidate = candidateRepository.findByCandidateIdAndUserId(candidateId, userId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found for userId: " + userId + " and candidateId: " + candidateId));

        if (candidate.getInterviewDateTime() != null) {
            throw new InterviewAlreadyScheduledException("An interview is already scheduled for candidate ID: " + candidateId);
        }

        // Update candidate details
        candidate.setUserEmail(userEmail);
        candidate.setClientEmail(clientEmail);
        setDefaultEmailsIfMissing(candidate);

        // Determine Interview Type **ONLY IF interviewLevel is NULL or Empty**
        if (interviewLevel == null || interviewLevel.isEmpty()) {
            interviewLevel = determineInterviewType(clientEmail, zoomLink);
        }
        candidate.setInterviewLevel(interviewLevel);

        if ("External".equalsIgnoreCase(interviewLevel)) {
            // For External Interviews, make clientEmail & zoomLink optional
            candidate.setClientEmail(clientEmail);
            candidate.setZoomLink(zoomLink);
        } else {
            // For Internal Interviews, enforce mandatory fields
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

        // **Update Interview Status**
        candidate.setInterviewStatus("SCHEDULED");

        try {
            candidateRepository.save(candidate);
            System.out.println("Candidate saved successfully.");
        } catch (Exception e) {
            throw new RuntimeException("Error while saving candidate data.", e);
        }

        // **Email Notification**
        sendInterviewNotification(candidate);

        // **Return Response**
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

        logger.info("Starting interview update for userId: {} and candidateId: {}", userId, candidateId);

        if (candidateId == null) {
            throw new CandidateNotFoundException("Candidate ID cannot be null for userId: " + userId);
        }

        // Retrieve candidate details
        CandidateDetails candidate = candidateRepository.findByCandidateIdAndUserId(candidateId, userId)
                .orElseThrow(() -> new CandidateNotFoundException(
                        "Candidate not found for userId: " + userId + " and candidateId: " + candidateId));

        if (candidate.getInterviewDateTime() == null) {
            throw new InterviewNotScheduledException("No interview scheduled for candidate ID: " + candidateId);
        }

        // Update fields only if values are provided
        if (interviewDateTime != null) candidate.setInterviewDateTime(interviewDateTime);
        if (duration != null) candidate.setDuration(duration);
        if (zoomLink != null && !zoomLink.isEmpty()) candidate.setZoomLink(zoomLink);
        if (userEmail != null && !userEmail.isEmpty()) candidate.setUserEmail(userEmail);
        if (clientEmail != null && !clientEmail.isEmpty()) candidate.setClientEmail(clientEmail);
        if (clientName != null && !clientName.isEmpty()) candidate.setClientName(clientName);
        if (interviewLevel != null && !interviewLevel.isEmpty()) candidate.setInterviewLevel(interviewLevel);
        if (externalInterviewDetails != null && !externalInterviewDetails.isEmpty()) candidate.setExternalInterviewDetails(externalInterviewDetails);
        if (interviewStatus != null && !interviewStatus.isEmpty()) candidate.setInterviewStatus(interviewStatus); // Update status

        // Determine interview type if interviewLevel is null
        if (candidate.getInterviewLevel() == null) {
            candidate.setInterviewLevel(determineInterviewType(clientEmail, zoomLink));
        }

        // Handle internal vs. external interview constraints
        if ("External".equalsIgnoreCase(candidate.getInterviewLevel())) {
            // External interview: Only update clientEmail and zoomLink if provided, don't nullify
            if (clientEmail != null) candidate.setClientEmail(clientEmail);
            if (zoomLink != null) candidate.setZoomLink(zoomLink);
        } else {
            // Internal interview: Ensure clientEmail and zoomLink are mandatory
            if (clientEmail == null || clientEmail.isEmpty()) {
                throw new IllegalArgumentException("Client email is required for Internal interviews.");
            }
            if (zoomLink == null || zoomLink.isEmpty()) {
                throw new IllegalArgumentException("Zoom link is required for Internal interviews.");
            }
            candidate.setClientEmail(clientEmail);
            candidate.setZoomLink(zoomLink);
        }

        // Update timestamp
        candidate.setTimestamp(LocalDateTime.now());

        // Save updated candidate details
        candidateRepository.save(candidate);
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
                candidate.getFullName(), formattedDate, formattedTime, formattedDuration, formattedZoomLink, candidate.getInterviewStatus());

        String subject = "Interview Update for " + candidate.getFullName();

        // Send email notifications with error handling
        try {
            Stream.of(candidate.getCandidateEmailId(), candidate.getClientEmail(), candidate.getUserEmail())
                    .filter(Objects::nonNull)
                    .forEach(email -> emailService.sendInterviewNotification(email, subject, emailBody));
        } catch (Exception e) {
            logger.error("Failed to send email notification: " + e.getMessage(), e);
        }

        // Return updated interview response
        return new InterviewResponseDto(
                true,
                "Interview updated successfully and notifications sent.",
                new InterviewResponseDto.InterviewPayload(
                        candidate.getCandidateId(),
                        candidate.getUserEmail(),
                        candidate.getCandidateEmailId(),
                        candidate.getClientEmail()
                ),
                null  // No errors
        );
    }

    public List<GetInterviewResponseDto> getAllScheduledInterviews() {
        // Fetch all candidates
        List<CandidateDetails> candidates = candidateRepository.findAll();
        List<GetInterviewResponseDto> response = new ArrayList<>();

        for (CandidateDetails interview : candidates) {
            // Skip candidates where interview is not scheduled
            if (interview.getInterviewDateTime() == null) {
                continue;  // Skip this iteration if interview is not scheduled
            } {
                // Create DTO and add to response list
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
                        "Scheduled" // Since only scheduled interviews are included
                );
                response.add(dto);
            }
        }

        return response;
    }


    public List<GetInterviewResponseDto> getAllScheduledInterviewsByUserId(String userId) {
        List<CandidateDetails> candidates = candidateRepository.findByUserId(userId);
        List<GetInterviewResponseDto> response = new ArrayList<>();

        for (CandidateDetails interview : candidates) {
            // Determine interview status dynamically
            String interviewStatus = (interview.getInterviewDateTime() != null) ? "Scheduled" : "Not Scheduled";

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
                    interviewStatus  // Dynamically assign status
            );
            response.add(dto);
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
        logger.info("Received request to Remove Scheduled Interview Details for candidateId: {}", candidateId);

        Optional<CandidateDetails> optionalCandidate = candidateRepository.findByCandidateId(candidateId);

        if (optionalCandidate.isEmpty()) {
            logger.error("Candidate with ID {} not found in database", candidateId);
            throw new InterviewNotScheduledException("No Scheduled Interview found for candidate ID: " + candidateId);
        }

        CandidateDetails candidate = optionalCandidate.get();
        logger.info("Candidate found: {} (Candidate ID: {}), Checking interviewDateTime: {}",
                candidate.getFullName(), candidate.getCandidateId(), candidate.getInterviewDateTime());

        if (candidate.getInterviewDateTime() == null) {
            logger.warn("No interview scheduled for candidate ID: {}", candidateId);
            throw new InterviewNotScheduledException("No Scheduled Interview found for candidate ID: " + candidateId);
        }

        // ✅ ONLY remove interview-related fields
        candidate.setInterviewDateTime(null);
        candidate.setDuration(null);
        candidate.setZoomLink(null);
        candidate.setClientName(null);
        candidate.setInterviewLevel(null);
        candidate.setClientEmail(null);
        candidate.setTimestamp(null);
        candidate.setExternalInterviewDetails(null);
        candidate.setInterviewStatus("NOT SCHEDULED");

        // ✅ DO NOT DELETE THE ENTIRE CANDIDATE
        candidateRepository.save(candidate);
        logger.info("Scheduled Interview Details is Removed successfully for candidateId: {}", candidateId);
    }

}



