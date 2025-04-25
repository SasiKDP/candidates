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
import jakarta.persistence.Tuple;
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
            byte[] resumeData = resumeFile.getBytes();
            candidateDetails.setResume(resumeData);  // Store the resume as binary data in DB

            String resumeFilePath = saveResumeToFileSystem(resumeFile);
            candidateDetails.setResumeFilePath(resumeFilePath);  // Store the file path in DB
        }

        if (!isValidFileType(resumeFile)) {
            throw new InvalidFileTypeException("Invalid file type. Only PDF, DOC and DOCX files are allowed.");
        }

        // Save the candidate details to the database
        if (candidateDetails.getProfileReceivedDate() == null) {
            candidateDetails.setProfileReceivedDate(LocalDate.now());
        }

        if (candidateDetails.getCandidateId() == null || candidateDetails.getCandidateId().isEmpty()) {
            String newCandidateId = generateCustomCandidateId();
            candidateDetails.setCandidateId(newCandidateId);
        }


        // Save the candidate details to the database
        CandidateDetails savedCandidate = candidateRepository.save(candidateDetails);

        // Update the requirement status after saving the candidate
        candidateRepository.updateRequirementStatus(savedCandidate.getJobId());

        // Get recruiter and team lead emails
        String recruiterEmail = savedCandidate.getUserEmail();
        String teamLeadEmail = candidateRepository.findTeamLeadEmailByJobId(savedCandidate.getJobId());
        String teamLeadName = candidateRepository.findUserNameByEmail(teamLeadEmail);

        // Log more information about what is happening with email fetching
        logger.info("Recruiter email: {}", recruiterEmail);
        logger.info("Team lead email: {}", teamLeadEmail);

        // Send email if both emails are available
        if (recruiterEmail == null || teamLeadEmail == null) {
            logger.warn("Email not sent: recruiterEmail or teamLeadEmail is null. recruiterEmail: {}, teamLeadEmail: {}",
                    recruiterEmail, teamLeadEmail);
        } else {
            String recruiterName = candidateRepository.findUserNameByEmail(recruiterEmail);
            String actionType = "submission";
            emailService.sendCandidateNotification(savedCandidate, recruiterName, recruiterEmail, teamLeadName, teamLeadEmail, actionType);
        }

        // Prepare response payload
        CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(
                savedCandidate.getCandidateId(),
                savedCandidate.getUserId(),
                savedCandidate.getJobId()
        );

        // Return the success response
        return new CandidateResponseDto(
                "Success",
                "Candidate profile submitted successfully.",
                payload,
                null
        );
    }

    private String generateCustomCandidateId() {
        List<Integer> existingNumbers = candidateRepository.findAll().stream()
                .map(CandidateDetails::getCandidateId)
                .filter(id -> id != null && id.matches("CAND\\d{4,}")) // Match CAND0001+ (4+ digits)
                .map(id -> Integer.parseInt(id.replace("CAND", "")))
                .toList();

        int nextNumber = existingNumbers.stream().max(Integer::compare).orElse(0) + 1;

        return String.format("CAND%04d", nextNumber);
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
            Optional<CandidateDetails> existingCandidateOpt = candidateRepository.findById(candidateId);
            if (!existingCandidateOpt.isPresent()) {
                throw new CandidateNotFoundException(candidateId);
            }

            CandidateDetails existingCandidate = existingCandidateOpt.get();

            if (resumeFile != null && !resumeFile.isEmpty()) {
                if (!isValidFileType(resumeFile)) {
                    throw new InvalidFileTypeException("Invalid file type. Only PDF, DOC, and DOCX are allowed.");
                }
                saveFile(existingCandidate, resumeFile);
            }

            // Update fields
            updateCandidateFields(existingCandidate, updatedCandidateDetails);

            // Reset interview status
            existingCandidate.setInterviewStatus("");

            // Save updated candidate
            candidateRepository.save(existingCandidate);

            // ✅ Fetch recruiter and team lead emails
            String recruiterEmail = existingCandidate.getUserEmail();
            String teamLeadEmail = candidateRepository.findTeamLeadEmailByJobId(existingCandidate.getJobId());
            String recruiterName = candidateRepository.findUserNameByEmail(recruiterEmail);
            String teamLeadName = candidateRepository.findUserNameByEmail(teamLeadEmail);


            // ✅ Send email notification if emails are valid
            if (recruiterEmail == null || teamLeadEmail == null) {
                logger.warn("Email not sent during update: recruiterEmail or teamLeadEmail is null. recruiterEmail: {}, teamLeadEmail: {}", recruiterEmail, teamLeadEmail);
            } else {
                String actionType = "update";
                emailService.sendCandidateNotification(existingCandidate, recruiterName, recruiterEmail, teamLeadName, teamLeadEmail,"update");
            }

            CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(
                    existingCandidate.getCandidateId(),
                    existingCandidate.getUserId(),
                    existingCandidate.getJobId()
            );

            return new CandidateResponseDto(
                    "Success",
                    "Candidate successfully updated",
                    payload,
                    null
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
        // Calculate the start and end of the current month
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        // Log the fetching process with the date range
        logger.info("Fetching submissions between {} and {}", startOfMonth, endOfMonth);

        // Fetch candidates whose profileReceivedDate falls within current month
        List<CandidateDetails> candidates =
                candidateRepository.findByProfileReceivedDateBetween(startOfMonth, endOfMonth);

        // Check if there are no submissions
        if (candidates.isEmpty()) {
            logger.warn("No candidate submissions found between {} and {}", startOfMonth, endOfMonth);
            throw new CandidateNotFoundException("No candidate submissions found for the current month.");
        }

        // Log the number of submissions fetched
        logger.info("Fetched {} candidate submissions between {} and {}", candidates.size(), startOfMonth, endOfMonth);

        // Map CandidateDetails to CandidateGetResponseDto
        return candidates.stream()
                .map(CandidateGetResponseDto::new)
                .collect(Collectors.toList());
    }






    // Method to get candidate submissions by userId
    public List<CandidateGetResponseDto> getSubmissionsByUserId(String userId) {
        // ✅ Validate user existence and fetch role
        String role = candidateRepository.findRoleByUserId(userId); // Native query to join user_roles_prod and roles_prod
        if (role == null) {
            throw new ResourceNotFoundException("User ID '" + userId + "' not found or role not assigned.");
        }

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        List<CandidateDetails> candidates;

        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            candidates = candidateRepository.findByUserIdAndProfileReceivedDateBetween(userId, startOfMonth, endOfMonth);
        } else if ("BDM".equalsIgnoreCase(role)) {
            candidates = candidateRepository.findSubmissionsByBdmUserIdAndDateRange(userId, startOfMonth, endOfMonth);
        } else {
            throw new UnsupportedOperationException("Only EMPLOYEE and BDM roles are supported.");
        }

        if (candidates.isEmpty()) {
            throw new CandidateNotFoundException("No submissions found for userId: " + userId + " in the current month.");
        }

        ObjectMapper objectMapper = new ObjectMapper();

        return candidates.stream().map(candidate -> {
            String latestInterviewStatus = "Not Scheduled";
            String interviewStatusJson = candidate.getInterviewStatus();

            try {
                if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                    String trimmed = interviewStatusJson.trim();
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        List<Map<String, Object>> statusHistory = objectMapper.readValue(trimmed, List.class);

                        Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                .filter(entry -> entry.containsKey("timestamp") && entry.containsKey("status"))
                                .max(Comparator.comparing(entry -> OffsetDateTime.parse((String) entry.get("timestamp"))));

                        if (latestStatus.isPresent()) {
                            latestInterviewStatus = (String) latestStatus.get().get("status");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing interview status JSON for candidate " +
                        candidate.getCandidateId() + ": " + e.getMessage());
            }

            Optional<String> clientNameOpt = candidateRepository.findClientNameByJobId(candidate.getJobId());
            String clientName = clientNameOpt.orElse(null);

            CandidateGetResponseDto dto = new CandidateGetResponseDto(candidate);
            dto.setInterviewStatus(latestInterviewStatus);
            dto.setClientName(clientName);
            return dto;
        }).collect(Collectors.toList());
    }


    public List<CandidateGetResponseDto> getSubmissionsByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
        }

        // Fetch role
        String role = candidateRepository.findRoleByUserId(userId); // write this query to join user_roles_prod and roles_prod

        List<CandidateDetails> candidates;

        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            candidates = candidateRepository.findByUserIdAndProfileReceivedDateBetween(userId, startDate, endDate);
        } else if ("BDM".equalsIgnoreCase(role)) {
            candidates = candidateRepository.findSubmissionsByBdmUserIdAndDateRange(userId, startDate, endDate);
        } else {
            throw new UnsupportedOperationException("Only EMPLOYEE and BDM roles are supported.");
        }

        if (candidates.isEmpty()) {
            throw new CandidateNotFoundException("No submissions found for userId: " + userId + " between " + startDate + " and " + endDate);
        }

        ObjectMapper objectMapper = new ObjectMapper();

        return candidates.stream().map(candidate -> {
            String latestInterviewStatus = "Not Scheduled";
            String interviewStatusJson = candidate.getInterviewStatus();

            try {
                if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                    if (interviewStatusJson.trim().startsWith("[") && interviewStatusJson.trim().endsWith("]")) {
                        List<Map<String, Object>> statusHistory = objectMapper.readValue(interviewStatusJson, List.class);

                        if (!statusHistory.isEmpty()) {
                            Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                    .filter(entry -> entry.containsKey("timestamp") && entry.containsKey("status"))
                                    .max(Comparator.comparing(entry -> OffsetDateTime.parse((String) entry.get("timestamp"))));

                            if (latestStatus.isPresent()) {
                                latestInterviewStatus = (String) latestStatus.get().get("status");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing interview status JSON for candidate " +
                        candidate.getCandidateId() + ": " + e.getMessage());
            }

            Optional<String> clientNameOpt = candidateRepository.findClientNameByJobId(candidate.getJobId());
            String clientName = clientNameOpt.orElse(null);

            CandidateGetResponseDto dto = new CandidateGetResponseDto(candidate);
            dto.setInterviewStatus(latestInterviewStatus);
            dto.setClientName(clientName);

            return dto;
        }).collect(Collectors.toList());
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

    public TeamleadSubmissionsDTO getSubmissionsForTeamlead(String userId) {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Calculate the start and end date for the current month
        LocalDate startOfMonth = currentDate.withDayOfMonth(1);  // First day of the current month
        LocalDate endOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());  // Last day of the current month

        // Convert LocalDate to LocalDateTime for query compatibility (starting at the beginning and end of the day)
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(LocalTime.MAX);

        // Log the date range being fetched
        logger.info("Fetching current month submissions for teamlead with userId: {} between {} and {}", userId, startDateTime, endDateTime);

        // Fetch self submissions for the current month
        List<Tuple> selfSubs = candidateRepository.findSelfSubmissionsByTeamleadAndDateRange(userId, startDateTime, endDateTime);

        // Fetch team submissions for the current month
        List<Tuple> teamSubs = candidateRepository.findTeamSubmissionsByTeamleadAndDateRange(userId, startDateTime, endDateTime);
        logger.info("Fetched {} self submissions for teamlead with userId: {} between {} and {}", selfSubs.size(), userId, startDateTime, endDateTime);
        logger.info("Fetched {} team submissions for teamlead with userId: {} between {} and {}", teamSubs.size(), userId, startDateTime, endDateTime);

        // Convert Tuple data to DTO for both self and team submissions
        List<CandidateGetResponseDto> selfSubDtos = mapTuplesToResponseDto(selfSubs);
        List<CandidateGetResponseDto> teamSubDtos = mapTuplesToResponseDto(teamSubs);


        // Return the DTO containing both self and team submissions
        return new TeamleadSubmissionsDTO(selfSubDtos, teamSubDtos);
    }

    public List<CandidateGetResponseDto> mapTuplesToResponseDto(List<Tuple> tuples) {
        return tuples.stream().map(tuple -> {
            CandidateGetResponseDto dto = new CandidateGetResponseDto();

            // Mapping fields from the Tuple to DTO
            dto.setCandidateId(tuple.get("candidate_id", String.class));
            dto.setFullName(tuple.get("full_name", String.class));
            dto.setSkills(tuple.get("skills", String.class)); // Corrected field mapping
            dto.setPreferredLocation(tuple.get("preferred_location", String.class)); // Corrected field mapping
            dto.setJobId(tuple.get("job_id", String.class));
            dto.setUserId(tuple.get("user_id",String.class));
            dto.setUserEmail(tuple.get("user_email", String.class)); // Corrected field mapping
            dto.setClientName(tuple.get("client_name", String.class)); // Corrected field mapping

            // Parsing profileReceivedDate as LocalDate (ensure it comes in a valid format)
            String timestamp = tuple.get("profile_received_date", String.class);  // Assuming timestamp is a string
            if (timestamp != null) {
                try {
                    LocalDate profileReceivedDate = LocalDate.parse(timestamp, DateTimeFormatter.ISO_DATE);
                    dto.setProfileReceivedDate(profileReceivedDate);
                } catch (Exception e) {
                    // Fallback if date parsing fails
                    System.err.println("Error parsing profileReceivedDate: " + e.getMessage());
                }
            }

            // Parse the interview status JSON and extract the latest status
            String interviewStatusJson = tuple.get("interview_status", String.class); // Corrected field mapping
            String latestInterviewStatus = extractLatestInterviewStatus(interviewStatusJson);

            dto.setInterviewStatus(latestInterviewStatus);

            return dto;
        }).collect(Collectors.toList());
    }

    // Helper method to extract the latest interview status from the JSON string
    private String extractLatestInterviewStatus(String interviewStatusJson) {
        try {
            if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                String trimmedStatus = interviewStatusJson.trim();

                if (trimmedStatus.startsWith("[") && trimmedStatus.endsWith("]")) {
                    // Parse the JSON array into a list of status entries
                    List<Map<String, Object>> statusHistory = objectMapper.readValue(trimmedStatus, List.class);

                    // Extract the latest interview status based on the timestamp
                    if (!statusHistory.isEmpty()) {
                        Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                .filter(entry -> entry.containsKey("timestamp") && entry.containsKey("status"))
                                .max(Comparator.comparing(entry -> OffsetDateTime.parse((String) entry.get("timestamp"))));

                        if (latestStatus.isPresent()) {
                            return (String) latestStatus.get().get("status");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing interview status JSON: " + e.getMessage());
        }

        return "Not Scheduled"; // Default fallback status if parsing fails
    }
    public TeamleadInterviewsDTO getTeamleadScheduledInterviews(String userId) {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Calculate the start and end date for the current month
        LocalDate startOfMonth = currentDate.withDayOfMonth(1);  // First day of the current month
        LocalDate endOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());  // Last day of the current month

        // Convert LocalDate to LocalDateTime for query compatibility (starting at the beginning and end of the day)
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(LocalTime.MAX);

        // Fetch self and team interviews for the current month using the updated queries
        List<CandidateDetails> selfInterviewsRaw = candidateRepository.findSelfScheduledInterviewsByTeamleadAndDateRange(userId, startDateTime, endDateTime);
        List<CandidateDetails> teamInterviewsRaw = candidateRepository.findTeamScheduledInterviewsByTeamleadAndDateRange(userId, startDateTime, endDateTime);

        // Log the fetched data for monitoring purposes
        logger.info("Fetched {} self interviews for teamlead with userId: {} between {} and {}",
                selfInterviewsRaw.size(), userId, startDateTime, endDateTime);
        logger.info("Fetched {} team interviews for teamlead with userId: {} between {} and {}",
                teamInterviewsRaw.size(), userId, startDateTime, endDateTime);

        // Parse the raw data into response DTOs
        List<GetInterviewResponseDto> selfInterviews = parseInterviewCandidates(selfInterviewsRaw);
        List<GetInterviewResponseDto> teamInterviews = parseInterviewCandidates(teamInterviewsRaw);

        // Return the DTO with both lists
        return new TeamleadInterviewsDTO(selfInterviews, teamInterviews);
    }


    private List<GetInterviewResponseDto> parseInterviewCandidates(List<CandidateDetails> candidates) {
        List<GetInterviewResponseDto> response = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        for (CandidateDetails interview : candidates) {
            if (interview.getInterviewDateTime() == null) continue;

            String interviewStatusJson = interview.getInterviewStatus();
            String latestInterviewStatus = null;

            try {
                if (interviewStatusJson != null && !interviewStatusJson.trim().isEmpty()) {
                    if (interviewStatusJson.trim().startsWith("[") && interviewStatusJson.trim().endsWith("]")) {
                        List<Map<String, Object>> statusHistory = objectMapper.readValue(interviewStatusJson, List.class);

                        Optional<Map<String, Object>> latestStatus = statusHistory.stream()
                                .max(Comparator.comparing(entry -> OffsetDateTime.parse((String) entry.get("timestamp"))));

                        if (latestStatus.isPresent()) {
                            latestInterviewStatus = (String) latestStatus.get().get("status");
                        }
                    } else {
                        latestInterviewStatus = interviewStatusJson.trim();
                    }
                }
            } catch (Exception e) {
                latestInterviewStatus = "Error Parsing Status";
                e.printStackTrace();
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

            validateInterviewLevel(interviewLevel, clientEmail);

            ObjectMapper objectMapper = new ObjectMapper();
            String existingStatusJson = candidate.getInterviewStatus();
            ArrayNode historyArray = objectMapper.createArrayNode();

            if (existingStatusJson != null && !existingStatusJson.isEmpty()) {
                logger.debug("Attempting to parse interview status JSON: {}", existingStatusJson);
                try {
                    JsonNode node = objectMapper.readTree(existingStatusJson);

                    if (node.isArray()) {
                        historyArray = (ArrayNode) node;
                    } else if (node.isObject()) {
                        historyArray.add(node);
                    } else {
                        logger.warn("Interview status is not a valid JSON array/object. Skipping parse.");
                    }

                    for (JsonNode statusNode : historyArray) {
                        String existingStatus = statusNode.has("status") ? statusNode.get("status").asText() : null;
                        if (existingStatus != null && existingStatus.equalsIgnoreCase(interviewStatus)) {
                            throw new IllegalArgumentException("Interview status is already set to the same value for candidate: " + candidateId);
                        }
                    }
                } catch (JsonProcessingException e) {
                    logger.warn("Invalid interview status format. Skipping history parse. Value: {}", existingStatusJson);
                }
            }

            String latestStatus = "N/A";
            if (interviewStatus != null && !interviewStatus.isEmpty()) {
                int nextStage = historyArray.size() + 1;

                ObjectNode newEntry = objectMapper.createObjectNode();
                newEntry.put("stage", nextStage);
                newEntry.put("status", interviewStatus);
                newEntry.put("timestamp", OffsetDateTime.now().toString());

                historyArray.add(newEntry);
                candidate.setInterviewStatus(objectMapper.writeValueAsString(historyArray));
                latestStatus = interviewStatus;
            }

            String normalizedStatus = latestStatus.trim().toLowerCase();

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

            if (interviewDateTime != null) candidate.setInterviewDateTime(interviewDateTime);
            if (duration != null) candidate.setDuration(duration);
            if (zoomLink != null && !zoomLink.isEmpty()) candidate.setZoomLink(zoomLink);
            if (userEmail != null && !userEmail.isEmpty()) candidate.setUserEmail(userEmail);
            if (clientEmail != null && !clientEmail.isEmpty()) candidate.setClientEmail(clientEmail);
            if (clientName != null && !clientName.isEmpty()) candidate.setClientName(clientName);
            if (interviewLevel != null && !interviewLevel.isEmpty()) candidate.setInterviewLevel(interviewLevel);
            if (externalInterviewDetails != null && !externalInterviewDetails.isEmpty()) candidate.setExternalInterviewDetails(externalInterviewDetails);

            boolean isInternalInterview = "internal".equalsIgnoreCase(interviewLevel)
                    || (interviewLevel == null && clientEmail != null && !clientEmail.trim().isEmpty());

            if (isInternalInterview) {
                if (clientEmail == null || clientEmail.trim().isEmpty()) {
                    throw new IllegalArgumentException("Client email is required for internal interviews.");
                }

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

            if (userEmail != null && !userEmail.isEmpty()) {
                Stream.of(candidate.getCandidateEmailId(), candidate.getClientEmail(), userEmail)
                        .filter(email -> email != null && !email.isEmpty())
                        .forEach(email -> {
                            try {
                                logger.info("Sending email to: {}", email);
                                emailService.sendInterviewNotification(email, subject, emailBody);
                            } catch (Exception e) {
                                logger.error("Failed to send email to {}: {}", email, e.getMessage(), e);
                            }
                        });
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
        // Calculate the start and end of the current month
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        // Use your custom query to fetch scheduled interviews for the current month
        List<CandidateDetails> candidates = candidateRepository
                .findScheduledInterviewsByDateOnly(startOfMonth, endOfMonth);

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
                                    .filter(entry -> entry.containsKey("timestamp") && entry.containsKey("status"))
                                    .max(Comparator.comparing(entry ->
                                            OffsetDateTime.parse((String) entry.get("timestamp"))));
                            if (latestStatus.isPresent()) {
                                latestInterviewStatus = (String) latestStatus.get().get("status");
                            }
                        }
                    } else {
                        latestInterviewStatus = interviewStatusJson;
                    }
                } catch (JsonParseException e) {
                    System.err.println("Error parsing interview status JSON: Invalid JSON format.");
                    latestInterviewStatus = interviewStatusJson;
                } catch (IOException e) {
                    System.err.println("Error reading interview status: " + e.getMessage());
                }
            }

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

        return response;
    }

    public List<GetInterviewResponseDto> getAllScheduledInterviewsByUserId(String userId) {
        // Calculate start and end of current month
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // Convert to LocalDateTime for repository calls
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(LocalTime.MAX);

        logger.info("Fetching interviews for userId: {} between {} and {}", userId, startOfMonth, endOfMonth);

        // Fetch role
        String role = candidateRepository.findRoleByUserId(userId);
        logger.info("User role for userId {}: {}", userId, role);

        List<CandidateDetails> employeeCandidates = new ArrayList<>();
        List<Tuple> bdmCandidates = new ArrayList<>();

        // Fetch data based on role
        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            logger.info("Fetching scheduled interviews for EMPLOYEE userId: {} between {} and {}",
                    userId, startDateTime, endDateTime);
            employeeCandidates = candidateRepository.findScheduledInterviewsByUserIdAndDateRange(
                    userId, startDateTime, endDateTime);

            if (employeeCandidates.isEmpty()) {
                logger.warn("No interviews found for EMPLOYEE userId: {} in the current month",
                        userId);
                return new ArrayList<>();
            }
        } else if ("BDM".equalsIgnoreCase(role)) {
            logger.info("Fetching scheduled interviews for BDM userId: {} between {} and {}",
                    userId, startDateTime, endDateTime);
            bdmCandidates = candidateRepository.findScheduledInterviewsByBdmUserIdAndDateRange(
                    userId, startDateTime, endDateTime);

            if (bdmCandidates.isEmpty()) {
                logger.warn("No interviews found for BDM userId: {} in the current month",
                        userId);
                return new ArrayList<>();
            }
        } else {
            logger.error("Unsupported role {} for userId {}", role, userId);
            throw new UnsupportedOperationException("Only EMPLOYEE and BDM roles are supported.");
        }

        // Process and return results
        List<GetInterviewResponseDto> response = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            logger.info("Processing {} interviews for EMPLOYEE userId: {}", employeeCandidates.size(), userId);

            for (CandidateDetails interview : employeeCandidates) {
                String interviewStatusJson = interview.getInterviewStatus();
                String latestInterviewStatus = processInterviewStatus(interviewStatusJson, objectMapper, interview.getCandidateId());

                // Only add if interviewDateTime is not null
                if (interview.getInterviewDateTime() != null) {
                    logger.debug("Adding interview for candidateId: {} with jobId: {}",
                            interview.getCandidateId(), interview.getJobId());

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
            }
        } else if ("BDM".equalsIgnoreCase(role)) {
            logger.info("Processing {} interviews for BDM userId: {}", bdmCandidates.size(), userId);

            for (Tuple tuple : bdmCandidates) {
                String candidateId = tuple.get("candidate_id", String.class);
                String interviewStatusJson = tuple.get("interview_status", String.class);
                String latestInterviewStatus = processInterviewStatus(interviewStatusJson, objectMapper, candidateId);

                String interviewDateTimeStr = tuple.get("interview_date_time", String.class);
                OffsetDateTime interviewDateTime = null;

                if (interviewDateTimeStr != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                    LocalDateTime localDateTime = LocalDateTime.parse(interviewDateTimeStr, formatter);
                    interviewDateTime = localDateTime.atOffset(ZoneOffset.ofHoursMinutes(5, 30)); // IST
                }

                // Only add if interviewDateTime is not null
                if (interviewDateTime != null) {
                    String timestampStr = tuple.get("timestamp", String.class);
                    LocalDateTime timestamp = null;
                    if (timestampStr != null) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                        timestamp = LocalDateTime.parse(timestampStr, formatter);
                    }

                    logger.debug("Adding interview for candidateId: {} with jobId: {}",
                            candidateId, tuple.get("job_id", String.class));

                    response.add(new GetInterviewResponseDto(
                            tuple.get("job_id", String.class),
                            candidateId,
                            tuple.get("full_name", String.class),
                            tuple.get("contact_number", String.class),
                            tuple.get("candidate_email_id", String.class),
                            tuple.get("user_email", String.class),
                            tuple.get("user_id", String.class),
                            interviewDateTime,
                            tuple.get("duration", Integer.class),
                            tuple.get("zoom_link", String.class),
                            timestamp,
                            tuple.get("client_email", String.class),
                            tuple.get("client_name", String.class),
                            tuple.get("interview_level", String.class),
                            latestInterviewStatus
                    ));
                }
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

    public List<CandidateGetResponseDto> getSubmissionsByDateRange(LocalDate startDate, LocalDate endDate) {

        // 💥 Second check: End date must not be before start date
        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
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
        // Log input validation
        logger.info("Fetching interviews between {} and {}", startDate, endDate);

        if (endDate.isBefore(startDate)) {
            logger.error("End date is before start date: {} and {}", startDate, endDate);
            throw new DateRangeValidationException("End date must not be before the start date.");
        }

        // Log before fetching data
        logger.info("Fetching scheduled interviews between {} and {}", startDate, endDate);

        List<CandidateDetails> candidates = candidateRepository.findScheduledInterviewsByDateOnly(startDate, endDate);

        // Log if no interviews found
        if (candidates.isEmpty()) {
            logger.warn("No interviews found between {} and {}", startDate, endDate);
            throw new CandidateNotFoundException("No interviews found between " + startDate + " and " + endDate);
        }

        // Log interviews found
        logger.info("Fetched {} interviews between {} and {}", candidates.size(), startDate, endDate);

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
                    logger.error("Error parsing interview status for candidateId: {}", interview.getCandidateId(), e);
                }
            }

            // Log each interview added to response
            logger.info("Adding interview for candidateId: {} with jobId: {}", interview.getCandidateId(), interview.getJobId());

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


    public List<GetInterviewResponseDto> getScheduledInterviewsByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        // Log input validation
        logger.info("Fetching interviews for userId: {} between {} and {}", userId, startDate, endDate);

        if (endDate.isBefore(startDate)) {
            logger.error("End date is before start date: {} and {}", startDate, endDate);
            throw new DateRangeValidationException("End date must not be before the start date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Fetch role
        String role = candidateRepository.findRoleByUserId(userId);
        logger.info("User role for userId {}: {}", userId, role);

        List<CandidateDetails> employeeCandidates = new ArrayList<>();
        List<Tuple> bdmCandidates = new ArrayList<>();

        // Fetch data based on role
        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            logger.info("Fetching scheduled interviews for EMPLOYEE userId: {} between {} and {}",
                    userId, startDateTime, endDateTime);
            employeeCandidates = candidateRepository.findScheduledInterviewsByUserIdAndDateRange(
                    userId, startDateTime, endDateTime);

            if (employeeCandidates.isEmpty()) {
                logger.warn("No interviews found for EMPLOYEE userId: {} between {} and {}",
                        userId, startDate, endDate);
                throw new CandidateNotFoundException("No interviews found for EMPLOYEE userId: " +
                        userId + " between " + startDate + " and " + endDate);
            }
        } else if ("BDM".equalsIgnoreCase(role)) {
            logger.info("Fetching scheduled interviews for BDM userId: {} between {} and {}",
                    userId, startDateTime, endDateTime);
            bdmCandidates = candidateRepository.findScheduledInterviewsByBdmUserIdAndDateRange(
                    userId, startDateTime, endDateTime);

            if (bdmCandidates.isEmpty()) {
                logger.warn("No interviews found for BDM userId: {} between {} and {}",
                        userId, startDate, endDate);
                throw new CandidateNotFoundException("No interviews found for BDM userId: " +
                        userId + " between " + startDate + " and " + endDate);
            }
        } else {
            logger.error("Unsupported role {} for userId {}", role, userId);
            throw new UnsupportedOperationException("Only EMPLOYEE and BDM roles are supported.");
        }

        // Process and return results
        List<GetInterviewResponseDto> response = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            logger.info("Processing {} interviews for EMPLOYEE userId: {}", employeeCandidates.size(), userId);

            for (CandidateDetails interview : employeeCandidates) {
                String interviewStatusJson = interview.getInterviewStatus();
                String latestInterviewStatus = processInterviewStatus(interviewStatusJson, objectMapper, interview.getCandidateId());

                logger.debug("Adding interview for candidateId: {} with jobId: {}",
                        interview.getCandidateId(), interview.getJobId());

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
        } else if ("BDM".equalsIgnoreCase(role)) {
            logger.info("Processing {} interviews for BDM userId: {}", bdmCandidates.size(), userId);

            for (Tuple tuple : bdmCandidates) {
                String candidateId = tuple.get("candidate_id", String.class);
                String interviewStatusJson = tuple.get("interview_status", String.class);
                String latestInterviewStatus = processInterviewStatus(interviewStatusJson, objectMapper, candidateId);

                logger.debug("Adding interview for candidateId: {} with jobId: {}",
                        candidateId, tuple.get("job_id", String.class));

                String interviewDateTimeStr = tuple.get("interview_date_time", String.class);
                OffsetDateTime interviewDateTime = null;

                if (interviewDateTimeStr != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                    LocalDateTime localDateTime = LocalDateTime.parse(interviewDateTimeStr, formatter);
                    interviewDateTime = localDateTime.atOffset(ZoneOffset.ofHoursMinutes(5, 30)); // IST
                }
// In the BDM section of the code:
                String timestampStr = tuple.get("timestamp", String.class);
                LocalDateTime timestamp = null;
                if (timestampStr != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                    timestamp = LocalDateTime.parse(timestampStr, formatter);
                }

// Then in the DTO constructor call, you're passing timestampStr (the String)
// when you should be passing timestamp (the LocalDateTime)
                response.add(new GetInterviewResponseDto(
                        tuple.get("job_id", String.class),
                        candidateId,
                        tuple.get("full_name", String.class),
                        tuple.get("contact_number", String.class),
                        tuple.get("candidate_email_id", String.class),
                        tuple.get("user_email", String.class),
                        tuple.get("user_id", String.class),
                        interviewDateTime,
                        tuple.get("duration", Integer.class),
                        tuple.get("zoom_link", String.class),
                        timestamp,  // Change this from timestampStr to timestamp
                        tuple.get("client_email", String.class),
                        tuple.get("client_name", String.class),
                        tuple.get("interview_level", String.class),
                        latestInterviewStatus
                ));
            }
        }

        return response;
    }

    // Helper method to process interview status
    private String processInterviewStatus(String interviewStatusJson, ObjectMapper objectMapper, String candidateId) {
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
                logger.error("Error parsing interview status for candidateId: {}", candidateId, e);
            }
        }

        return latestInterviewStatus;
    }
    public TeamleadInterviewsDTO getTeamleadScheduledInterviewsByDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        // 1. Validate the date range
        if (startDate == null || endDate == null) {
            throw new DateRangeValidationException("Start date and End date must not be null.");
        }
        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
        }

        // 2. Log the date range
        logger.info("Fetching interviews for teamlead with userId: {} between {} and {}", userId, startDate, endDate);

        // 3. Prepare date range (convert LocalDate to LocalDateTime for query accuracy)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 4. Fetch self and team interviews within the date range
        List<CandidateDetails> selfInterviewsRaw = candidateRepository.findSelfScheduledInterviewsByTeamleadAndDateRange(userId, startDateTime, endDateTime);
        List<CandidateDetails> teamInterviewsRaw = candidateRepository.findTeamScheduledInterviewsByTeamleadAndDateRange(userId, startDateTime, endDateTime);

        // 5. Log the number of self and team interviews
        logger.info("Fetched {} self interviews for teamlead with userId: {} between {} and {}",
                selfInterviewsRaw.size(), userId, startDate, endDate);
        logger.info("Fetched {} team interviews for teamlead with userId: {} between {} and {}",
                teamInterviewsRaw.size(), userId, startDate, endDate);

        // 6. Handle empty result
        if (selfInterviewsRaw.isEmpty() && teamInterviewsRaw.isEmpty()) {
            throw new CandidateNotFoundException("No scheduled interviews found between " + startDate + " and " + endDate);
        }

        // 7. Parse the raw data into response DTOs using the updated GetInterviewResponseDto
        List<GetInterviewResponseDto> selfInterviews = parseInterviewCandidates(selfInterviewsRaw);
        List<GetInterviewResponseDto> teamInterviews = parseInterviewCandidates(teamInterviewsRaw);

        // 8. Return the DTO with both lists
        return new TeamleadInterviewsDTO(selfInterviews, teamInterviews);
    }
    public TeamleadSubmissionsDTO getSubmissionsForTeamleadByDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        // 1. Validate the date range
        if (startDate == null || endDate == null) {
            throw new DateRangeValidationException("Start date and End date must not be null.");
        }
        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
        }

        // 2. Convert LocalDate to LocalDateTime for query compatibility (starting at the beginning and end of the day)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 3. Fetch self and team submissions within the date range
        List<Tuple> selfSubs = candidateRepository.findSelfSubmissionsByTeamleadAndDateRange(userId, startDateTime, endDateTime);
        List<Tuple> teamSubs = candidateRepository.findTeamSubmissionsByTeamleadAndDateRange(userId, startDateTime, endDateTime);

        // 4. Log the date range and the number of results
        logger.info("Fetched {} self submissions for teamlead with userId: {} between {} and {}",
                selfSubs.size(), userId, startDateTime, endDateTime);
        logger.info("Fetched {} team submissions for teamlead with userId: {} between {} and {}",
                teamSubs.size(), userId, startDateTime, endDateTime);

        // 5. Convert the raw submission data (Tuples) into DTOs
        List<CandidateGetResponseDto> selfSubDtos = mapTuplesToResponseDto(selfSubs);
        List<CandidateGetResponseDto> teamSubDtos = mapTuplesToResponseDto(teamSubs);

        // 6. Return the DTO containing both self and team submissions
        return new TeamleadSubmissionsDTO(selfSubDtos, teamSubDtos);
    }



}



