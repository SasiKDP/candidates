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
import com.profile.candidate.model.Submissions;
import com.profile.candidate.repository.CandidateRepository;
import com.profile.candidate.repository.SubmissionRepository;
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

    @Autowired
    private SubmissionRepository submissionRepository;

    // Method to submit a candidate profile
    public CandidateResponseDto submitCandidate(CandidateDetails candidateDetails, Submissions submissionDetails, MultipartFile resumeFile) throws IOException {
        // Step 1: Validate input fields
        validateCandidateDetails(candidateDetails);

        // Step 2: Check for duplicate submissions
        checkForDuplicates(submissionDetails);

        // Step 3: Set default emails if not already provided
        setDefaultEmailsIfMissing(candidateDetails);

        // Step 4: Handle resume file (store as blob + save file path)
        if (resumeFile != null && !resumeFile.isEmpty()) {
            if (!isValidFileType(resumeFile)) {
                throw new InvalidFileTypeException("Invalid file type. Only PDF, DOC, and DOCX files are allowed.");
            }

            byte[] resumeData = resumeFile.getBytes();
            String resumeFilePath = saveResumeToFileSystem(resumeFile);

            submissionDetails.setResume(resumeData);
            submissionDetails.setResumeFilePath(resumeFilePath);
        }

        // Step 5: Check if candidate already exists
        Optional<CandidateDetails> existingCandidateOpt = candidateRepository.findByCandidateEmailId(candidateDetails.getCandidateEmailId());
        CandidateDetails savedCandidate;
        if (existingCandidateOpt.isPresent()) {
            savedCandidate = existingCandidateOpt.get();
            // Optional: update other candidate fields here if needed
        } else {
            candidateDetails.setTimestamp(LocalDateTime.now());
            savedCandidate = candidateRepository.save(candidateDetails);
        }

        // Step 6: Set submission details
        String submissionId = savedCandidate.getCandidateId() + "_" + submissionDetails.getJobId();

        submissionDetails.setCandidate(savedCandidate);
        submissionDetails.setSubmissionId(submissionId);
        submissionDetails.setProfileReceivedDate(LocalDate.now());

        // Step 7: Save the submission
        submissionRepository.save(submissionDetails);

        // Step 8: Fetch team lead and recruiter details
        String teamLeadEmail = candidateRepository.findTeamLeadEmailByJobId(submissionDetails.getJobId());
        String recruiterEmail = savedCandidate.getUserEmail();
        String recruiterName = candidateRepository.findUserNameByEmail(recruiterEmail);

        // Step 9: Send notification if emails are available
        if (recruiterEmail == null || teamLeadEmail == null) {
            logger.warn("Email not sent: recruiterEmail or teamLeadEmail is null.");
        } else {
            String actionType = "submission";
            emailService.sendCandidateNotification(submissionDetails, recruiterName, recruiterEmail, teamLeadEmail, actionType);
        }

        // Step 10: Prepare response payload
        CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(
                savedCandidate.getCandidateId(),
                savedCandidate.getUserId(),
                submissionId
        );

        return new CandidateResponseDto(
                "Success",
                "Candidate profile submitted successfully.",
                payload,
                null
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
    private void checkForDuplicates(Submissions submissions) {
        Submissions existingSubmission =
                submissionRepository.findByCandidate_CandidateEmailIdAndJobId(submissions.getCandidate().getCandidateEmailId(),submissions.getJobId());
        if (existingSubmission!=null) {
            throw new CandidateAlreadyExistsException(
                    "Candidate with email ID " + existingSubmission.getCandidate().getCandidateEmailId()+
                            " has already been submitted for job " + existingSubmission.getJobId()
            );
        }
        Submissions existingContactNumber =
                submissionRepository.findByCandidate_ContactNumberAndJobId(
                        submissions.getCandidate().getContactNumber(),
                        submissions.getJobId());
        if (existingContactNumber!=null) {
            throw new CandidateAlreadyExistsException(
                    "Candidate with contact number " + existingContactNumber.getCandidate().getContactNumber() +
                            " has already been submitted for job " + existingContactNumber.getJobId()
            );
        }
    }
    // Set default values for userEmail and clientEmail if not provided
    private void setDefaultEmailsIfMissing(CandidateDetails candidateDetails) {
        if (candidateDetails.getUserEmail() == null) {
            candidateDetails.setUserEmail(candidateDetails.getUserEmail());  // Set to default or handle differently
        }
    }
    // Set default values for userEmail and clientEmail if not provided
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


    private void saveFile(Submissions submissions, MultipartFile file) throws IOException {
        // Define the path where files will be stored
        Path uploadsDirectory = Paths.get("uploads");
        // Check if the directory exists, if not, create it
        if (Files.notExists(uploadsDirectory)) {
            Files.createDirectories(uploadsDirectory);
            logger.info("Created directory: {}", uploadsDirectory.toString());
        }
        // Generate a filename that combines the candidateId and timestamp
        String filename = submissions.getSubmissionId() + "-" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path targetPath = uploadsDirectory.resolve(filename);  // Save the file inside the "uploads" directory

        try {
            // Log the file saving action
            logger.info("Saving file to path: {}", targetPath);
            // Save the file to the directory
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            // Optionally save the file path in the database (for example, updating the candidate)
            submissions.setResumeFilePath(targetPath.toString());

//            candidate.setResume(resumeFile.getBytes());
//            candidate.setResumeFilePath(resumeFile.getOriginalFilename());

            submissionRepository.save(submissions);

        } catch (IOException e) {
            logger.error("Failed to save file: {}", e.getMessage());
            throw new IOException("Failed to save file to path: " + targetPath, e);  // Throw exception to indicate failure
        }
    }
    public CandidateResponseDto resubmitCandidate(String candidateId, CandidateDetails updatedCandidateDetails,Submissions updatedSubmissionsDetails, MultipartFile resumeFile) {
        try {
            // Fetch the existing candidate from the database
            Optional<CandidateDetails> existingCandidateOpt = candidateRepository.findById(candidateId);
            if (!existingCandidateOpt.isPresent()) {
                // Candidate not found, return error
                throw new CandidateNotFoundException("Candidate Not Exists with candidateId "+candidateId);
            }
            Optional<CandidateDetails> optionalCandidate=candidateRepository.findByCandidateIdAndUserId(candidateId,updatedCandidateDetails.getUserId());

            if(optionalCandidate.isEmpty()){
                throw new CandidateNotFoundException("Candidate Id: "+candidateId+" Not related to UserId: "+updatedCandidateDetails.getUserId());
            }

            Submissions existedSubmission=submissionRepository.findByCandidate_CandidateIdAndJobId(candidateId, updatedSubmissionsDetails.getJobId());
            if(existedSubmission!=null) {
                throw new CandidateAlreadyExistsException("Candidate Already Submitted for JobId "+updatedSubmissionsDetails.getJobId());
            }
            CandidateDetails existingCandidate = existingCandidateOpt.get();
            updateCandidateFields(existingCandidate, updatedCandidateDetails);

            String submissionId=candidateId+"_"+updatedSubmissionsDetails.getJobId();
            Submissions submission=new Submissions();
            submission.setSubmissionId(submissionId);
            submission.setCandidate(existingCandidate);
            submission.setJobId(updatedSubmissionsDetails.getJobId());
            submission.setResumeFilePath(updatedSubmissionsDetails.getResumeFilePath());
            // submission.setResume(updatedSubmissionsDetails.getResume());
            submission.setSkills(updatedSubmissionsDetails.getSkills());
            submission.setPreferredLocation(updatedSubmissionsDetails.getPreferredLocation());
            submission.setProfileReceivedDate(LocalDate.now());
            submission.setCommunicationSkills(updatedSubmissionsDetails.getCommunicationSkills());
            submission.setRequiredTechnologiesRating(updatedSubmissionsDetails.getRequiredTechnologiesRating());
            submission.setOverallFeedback(updatedSubmissionsDetails.getOverallFeedback());
            submission.setSubmittedAt(LocalDateTime.now());

            if (resumeFile != null && !resumeFile.isEmpty()) {
                // Convert the resume file to byte[] and set it in the candidateDetails object
                byte[] resumeData = resumeFile.getBytes();
                //candidateDetails.setResume(resumeData);// Store the resume as binary data in DB
                submission.setResume(resumeData);
                // Save the resume to the file system and store the file path in DB
                String resumeFilePath = saveResumeToFileSystem(resumeFile);
                // candidateDetails.setResumeFilePath(resumeFilePath);// Store the file path in DB
                submission.setResumeFilePath(resumeFilePath);
            }
            // Update candidate fields with the new data (e.g., name, contact, etc.)
            updateCandidateFields(existingCandidate, updatedCandidateDetails);

            // Save the resume file and update the candidate with the new file path
            saveFile(submission, resumeFile);  // This saves the file and updates the candidate's resumeFilePath

            candidateRepository.save(existingCandidate);
            submissionRepository.save(submission);
            // ------------------ 📧 Send Resubmission Notification Email ------------------
            String recruiterEmail = existingCandidate.getUserEmail();
            String recruiterName = candidateRepository.findUserNameByEmail(recruiterEmail);
            String teamLeadEmail = candidateRepository.findTeamLeadEmailByJobId(submission.getJobId());

            if (recruiterEmail != null && teamLeadEmail != null) {
                try {
                    logger.info("Sending candidate resubmission email notification...");
                    emailService.sendCandidateNotification(submission, recruiterName, recruiterEmail, teamLeadEmail, "submission");
                } catch (Exception e) {
                    logger.error("Error sending resubmission email: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("Email not sent. recruiterEmail or teamLeadEmail is null. RecruiterEmail: {}, TeamLeadEmail: {}",
                        recruiterEmail, teamLeadEmail);
            }
// --------------------------------------------------------------------------


            // Return a success response with the updated candidate details
            CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(
                    existingCandidate.getCandidateId(),
                    existingCandidate.getUserId(),
                    submission.getSubmissionId()
            );
            return new CandidateResponseDto(
                    "Success",
                    "Candidate successfully updated",
                    payload,
                    null // No error message since no error occurred
            );
        }
        //catch (CandidateNotFoundException ex) {
//            // Custom handling for CandidateNotFoundException
//            logger.error("Candidate with ID {} not found: {}", candidateId, ex.getMessage());
//            throw ex; // Rethrow to be caught by GlobalExceptionHandler
//        }
        catch (InvalidFileTypeException ex) {
            // Custom handling for InvalidFileTypeException
            logger.error("Invalid file type for resume: {}", ex.getMessage());
            throw ex; // Rethrow to be caught by GlobalExceptionHandler
        } catch (IOException ex) {
            // Specific handling for I/O issues, such as file saving errors
            logger.error("Failed to save resume file: {}", ex.getMessage());
            throw new RuntimeException("An error occurred while saving the resume file", ex);
        }
//        catch (Exception ex) {
//            // General error handling for any unexpected issues
//            logger.error("An unexpected error occurred while resubmitting the candidate: {}", ex.getMessage());
//            throw new RuntimeException("An unexpected error occurred while resubmitting the candidate", ex);
//        }
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



    @Transactional
    public DeleteCandidateResponseDto deleteCandidateById(String candidateId) {
        CandidateDetails candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found with id: " + candidateId));

        // Fetch submission before deleting candidate
        List<Submissions> submissions = submissionRepository.findByCandidate_CandidateId(candidateId);

        String recruiterEmail = candidate.getUserEmail();
        String recruiterName = candidateRepository.findUserNameByEmail(recruiterEmail);

        for (Submissions submission : submissions) {
            String teamLeadEmail = candidateRepository.findTeamLeadEmailByJobId(submission.getJobId()); // customize as needed
            emailService.sendCandidateNotification(submission, recruiterName, recruiterEmail, teamLeadEmail, "deletion");
        }

        candidateRepository.delete(candidate);

        DeleteCandidateResponseDto.Payload payload = new DeleteCandidateResponseDto.Payload(
                candidate.getCandidateId(), candidate.getFullName()
        );

        return new DeleteCandidateResponseDto("Success", "Candidate deleted successfully", payload, null);
    }

    // Method to update the candidate fields with new values
    private void updateCandidateFields(CandidateDetails existingCandidate, CandidateDetails updatedCandidateDetails) {
        //if (updatedCandidateDetails.getJobId() != null) existingCandidate.setJobId(updatedCandidateDetails.getJobId());
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
        if (updatedCandidateDetails.getRelevantExperience() != 0)
            existingCandidate.setRelevantExperience(updatedCandidateDetails.getRelevantExperience());
        if (updatedCandidateDetails.getCurrentOrganization() != null)
            existingCandidate.setCurrentOrganization(updatedCandidateDetails.getCurrentOrganization());

        existingCandidate.setTimestamp(LocalDateTime.now());
    }


}

