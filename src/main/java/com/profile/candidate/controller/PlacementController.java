package com.profile.candidate.controller;

import com.profile.candidate.exceptions.ResourceNotFoundException;
import com.profile.candidate.model.PlacementDetails;
import com.profile.candidate.service.PlacementService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com", "http://localhost:3000", "http://192.168.0.135:8080",
        "http://192.168.0.135:80",
        "http://localhost/",
        "http://mymulya.com:443",
        "http://182.18.177.16:443",
        "http://localhost/",
})
@RestController
@RequestMapping("/candidate")
public class PlacementController {

    @Autowired
    private PlacementService service;

    // Helper method to format response payload
    private Map<String, Object> buildPlacementPayload(PlacementDetails placement) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", placement.getId());
        payload.put("consultantName", placement.getConsultantName());
        payload.put("phone", placement.getPhone());
        payload.put("email", placement.getConsultantEmail());
        return payload;
    }

    // Save placement
    @PostMapping("/placement/create-placement")
    public ResponseEntity<?> savePlacement(@Valid @RequestBody PlacementDetails placement) {
        PlacementDetails savedPlacement = service.savePlacement(placement);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Placement saved successfully");
        response.put("timestamp", LocalDateTime.now());
        response.put("data", buildPlacementPayload(savedPlacement));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Update placement by ID
    @PutMapping("/placement/update-placement/{id}")
    public ResponseEntity<?> updatePlacement(@PathVariable String id, @Valid @RequestBody PlacementDetails placement) {
        try {
            PlacementDetails updated = service.updatePlacement(id, placement);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Placement updated successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("data", buildPlacementPayload(updated));

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    // Delete placement by ID
    @DeleteMapping("/placement/delete-placement/{id}")
    public ResponseEntity<?> deletePlacement(@PathVariable String id) {
        try {
            service.deletePlacement(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Placement with ID " + id + " deleted successfully",
                    "timestamp", LocalDateTime.now()
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    // Get all placements (full details)
    @GetMapping("/placement/placements-list")
    public ResponseEntity<?> getAllPlacements() {
        List<PlacementDetails> placements = service.getAllPlacements();

        return ResponseEntity.ok(Map.of(

                "data", placements
        ));
    }
    // Get placement by ID
    @GetMapping("/placement/{id}")
    public ResponseEntity<?> getPlacementById(@PathVariable String id) {
        try {
            PlacementDetails placement = service.getPlacementById(id);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Placement fetched successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("data", buildPlacementPayload(placement));
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

}
