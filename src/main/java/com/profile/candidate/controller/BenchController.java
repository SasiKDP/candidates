package com.profile.candidate.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profile.candidate.dto.BenchDetailsDto;
import com.profile.candidate.dto.BenchResponseDto;
import com.profile.candidate.dto.ErrorResponseDto;
import com.profile.candidate.exceptions.DateRangeValidationException;
import com.profile.candidate.model.BenchDetails;
import com.profile.candidate.repository.BenchRepository;
import com.profile.candidate.service.BenchService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

//@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com", "http://localhost:3000", "http://192.168.0.135:8080",
//        "http://192.168.0.135:80",
//        "http://localhost/",
//        "http://mymulya.com:443",
//        "http://182.18.177.16:443",
//        "http://localhost/",
//        "http://192.168.0.135",
//        "http://182.18.177.16"
//})
@RestController
@RequestMapping("/candidate")
public class BenchController {

    private static final Logger logger = LoggerFactory.getLogger(BenchController.class);

    private static final String UPLOAD_DIR = "/your/upload/directory"; // Ensu
    private final BenchService benchService;
    @Autowired
    private BenchRepository benchRepository;

    @Autowired
    public BenchController(BenchService benchService) {
        this.benchService = benchService;
    }
    @PostMapping(value = "/bench/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BenchResponseDto> createBenchDetails(
            @RequestParam(value = "resumeFiles", required = false) MultipartFile resumeFile,
            @RequestParam(value = "fullName") String fullName,
            @RequestParam(value = "email") String email,
            @RequestParam(value = "relevantExperience") BigDecimal relevantExperience,
            @RequestParam(value = "totalExperience") BigDecimal totalExperience,
            @RequestParam(value = "contactNumber") String contactNumber,
            @RequestParam(value = "skills") String skillsJson, // Expecting JSON string
            @RequestParam(value = "linkedin", required = false) String linkedin,
            @RequestParam(value = "referredBy", required = false) String referredBy,
            @RequestParam(value = "technology", required = false) String technology)

    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> skillsList = objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {});

            BenchDetails benchDetails = new BenchDetails();
            benchDetails.setFullName(fullName);
            benchDetails.setEmail(email);
            benchDetails.setRelevantExperience(relevantExperience);
            benchDetails.setTotalExperience(totalExperience);
            benchDetails.setContactNumber(contactNumber);
            benchDetails.setSkills(skillsList);
            benchDetails.setLinkedin(linkedin);
            benchDetails.setReferredBy(referredBy);
            benchDetails.setTechnology(technology);

            // Process resume file
            if (resumeFile != null && !resumeFile.isEmpty()) {
                benchDetails.setResume(resumeFile.getBytes());
            }

            // Check for duplicate email
            if (benchRepository.existsByEmail(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new BenchResponseDto("Error", "Duplicate entry: Email already exists.", null, null)
                );
            }

            // Save bench details
            BenchDetails savedBenchDetails = benchService.saveBenchDetails(benchDetails, resumeFile);

            BenchResponseDto responseDto = new BenchResponseDto(
                    "Success",
                    "Bench details saved successfully",
                    List.of(new BenchResponseDto.Payload(savedBenchDetails.getId(), savedBenchDetails.getFullName())),
                    null
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new BenchResponseDto("Error", "Failed to process resume file: " + e.getMessage(), null, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BenchResponseDto("Error", "Error while saving bench details: " + e.getMessage(), null, null)
            );
        }
    }



    @GetMapping("/bench/getBenchList")
    public ResponseEntity<List<BenchDetailsDto>> getAllBenchDetails() {
        try {
            List<BenchDetails> benchDetailsList = benchService.findAllBenchDetails();

            // ✅ Convert BenchDetails to BenchDetailsDto (excluding resume)
            List<BenchDetailsDto> dtoList = benchDetailsList.stream()
                    .map(bench -> new BenchDetailsDto(
                            bench.getId(),
                            bench.getFullName(),
                            bench.getEmail(),
                            bench.getRelevantExperience(),
                            bench.getTotalExperience(),
                            bench.getContactNumber(),
                            bench.getSkills() != null ? bench.getSkills() : Collections.<String>emptyList(),  // ✅ Ensure skills is a List<String>
                            bench.getLinkedin(),
                            bench.getReferredBy(),
                            bench.getCreatedDate(),
                            bench.getTechnology()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }


    @GetMapping("/bench/filter-by-date")
    public ResponseEntity<?> getBenchDetailsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<BenchDetails> filtered = benchService.findBenchDetailsByDateRange(startDate, endDate);
            logger.info("✅ Fetched {} bench records between {} and {}", filtered.size(), startDate, endDate);

            List<BenchDetailsDto> dtoList = filtered.stream()
                    .map(bench -> new BenchDetailsDto(
                            bench.getId(),
                            bench.getFullName(),
                            bench.getEmail(),
                            bench.getRelevantExperience(),
                            bench.getTotalExperience(),
                            bench.getContactNumber(),
                            bench.getSkills() != null ? bench.getSkills() : Collections.emptyList(),
                            bench.getLinkedin(),
                            bench.getReferredBy(),
                            bench.getCreatedDate(),
                            bench.getTechnology()
                    ))
                    .collect(Collectors.toList());

            if (dtoList.isEmpty()) {
                logger.warn("⚠️ No bench records found in the given date range: {} to {}", startDate, endDate);
                return ResponseEntity.ok(Collections.singletonMap("error", "No bench details found in this date range."));
            }

            return ResponseEntity.ok(dtoList);
        } catch (DateRangeValidationException e) {
            logger.error("❌ Date Range Validation Error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("🔥 Unexpected error while fetching bench details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Something went wrong!"));
        }
    }

    @GetMapping("/getBenchBy/{benchId}")
    public ResponseEntity<BenchDetailsDto> getBenchById(@PathVariable String benchId) {
        BenchDetailsDto dto = benchService.getBenchById(benchId);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/bench/updatebench/{id}")
    public ResponseEntity<Object> updateBenchDetails(
            @PathVariable String id,
            @RequestParam(value = "resumeFiles", required = false) MultipartFile resumeFile,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "relevantExperience", required = false) BigDecimal relevantExperience,
            @RequestParam(value = "totalExperience", required = false) BigDecimal totalExperience,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestPart(value = "skills", required = false) String skillsJson, // Expecting JSON array
            @RequestParam(value = "linkedin", required = false) String linkedin,
            @RequestParam(value = "referredBy", required = false) String referredBy,
            @RequestParam(value = "technology", required = false) String technology

    ) {
        try {
            // 🔹 Log incoming request data
            System.out.println("Updating BenchDetails ID: " + id);
            System.out.println("Received Skills JSON: " + skillsJson);

            // ✅ Convert JSON string to List<String>
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> skillsList = (skillsJson != null && !skillsJson.isBlank())
                    ? objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {})
                    : Collections.emptyList();

            // ✅ Convert MultipartFile to byte array (if provided)
            byte[] resumeData = null;
            if (resumeFile != null && !resumeFile.isEmpty()) {
                resumeData = resumeFile.getBytes();
            }

            // ✅ Create BenchDetails object with provided data
            BenchDetails benchDetails = new BenchDetails();
            benchDetails.setFullName(fullName);
            benchDetails.setEmail(email);
            benchDetails.setRelevantExperience(relevantExperience);
            benchDetails.setTotalExperience(totalExperience);
            benchDetails.setContactNumber(contactNumber);
            benchDetails.setSkills(skillsList); // ✅ Ensure List<String>
            benchDetails.setLinkedin(linkedin);
            benchDetails.setReferredBy(referredBy);
            benchDetails.setResume(resumeData);
            benchDetails.setTechnology(technology);

            // ✅ Call service to update details
            BenchDetails updatedBenchDetails = benchService.updateBenchDetails(id, benchDetails);

            // ✅ Prepare response
            BenchResponseDto.Payload payload = new BenchResponseDto.Payload(
                    updatedBenchDetails.getId().toString(),
                    updatedBenchDetails.getFullName()
            );

            BenchResponseDto responseDto = new BenchResponseDto(
                    "Success",
                    "Bench details updated successfully",
                    List.of(payload),
                    null
            );

            return ResponseEntity.ok(responseDto);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto(false, "Invalid JSON format for skills: " + e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto(false, "Failed to process resume file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto(false, "Failed to update bench details: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto(false, "Error while updating bench details: " + e.getMessage()));
        }
    }

    @DeleteMapping("/bench/deletebench/{id}")
    public ResponseEntity<Object> deleteBenchDetails(@PathVariable String id) {
        try {
            // ✅ Check if the bench ID exists before deleting
            if (!benchRepository.existsById(id)) {
                throw new EntityNotFoundException("Bench Details with ID " + id + " does not exist.");
            }

            // ✅ Proceed with deletion
            benchService.deleteBenchDetailsById(id);

            // ✅ Prepare success response
            BenchResponseDto responseDto = new BenchResponseDto(
                    "Success",
                    "Bench Details with ID " + id + " successfully deleted",
                    null,
                    null
            );

            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            // ❌ Bench ID not found
            ErrorResponseDto errorResponse = new ErrorResponseDto(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            // ❌ Other errors
            ErrorResponseDto errorResponse = new ErrorResponseDto(false, "Error while deleting bench details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/bench/download/{id}")
    public ResponseEntity<byte[]> downloadResume(@PathVariable String id) {
        try {
            Optional<BenchDetails> benchDetailsOptional = benchRepository.findById(id);
            if (benchDetailsOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            BenchDetails benchDetails = benchDetailsOptional.get();
            byte[] resumeFile = benchDetails.getResume();

            if (resumeFile == null || resumeFile.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Tika tika = new Tika();
            String contentType = tika.detect(resumeFile);

            // Map content type to correct file extension
            String extension;
            switch (contentType) {
                case "application/pdf":
                    extension = ".pdf";
                    break;
                case "application/msword":
                    extension = ".doc";
                    break;
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    extension = ".docx";
                    break;
                default:
                    extension = ".bin"; // fallback
                    break;
            }

            String fileName = benchDetails.getFullName();
            if (fileName == null || fileName.isBlank()) {
                fileName = "Resume_" + id;
            }
            fileName = fileName.replaceAll("\\s+", "_") + extension;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resumeFile);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}