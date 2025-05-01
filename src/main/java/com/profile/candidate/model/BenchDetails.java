package com.profile.candidate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.GenericGenerator;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bench_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenchDetails {

 @Id
 @Column(name = "id", updatable = false, nullable = false, length = 36)
 private String id;

 @NotBlank(message = "Full name is required")
 @Column(name = "full_name", nullable = false)
 private String fullName;

 @NotBlank(message = "Email is required")
 @Email(message = "Email should be valid")
 @Column(name = "email", unique = true, nullable = false)
 private String email;

 @Column(name = "relevant_experience", precision = 5, scale = 2)
 private BigDecimal relevantExperience;

 @Column(name = "total_experience", precision = 5, scale = 2)
 private BigDecimal totalExperience;

 @NotBlank(message = "Contact number is required")
 @Pattern(regexp = "^\\+?[0-9. ()-]{7,15}$", message = "Contact number must be valid")
 @Column(name = "contact_number", nullable = false)
 private String contactNumber;

 @JsonProperty("skills")  // ✅ Ensure proper mapping
 private List<String> skills;  // ✅ Expecting an array, NOT a string

 @Lob
 @JdbcTypeCode(SqlTypes.JSON)
 private byte[] resume;

 @Column(name = "linkedin", columnDefinition = "TEXT")
 private String linkedin;

 @Column(name = "referred_by")
 private String referredBy;

 @Column(name = "created_date")
 private LocalDate createdDate;


 @Column(name = "technology")
 private String technology;

 public LocalDate getCreatedDate() {
  return createdDate;
 }

 public void setCreatedDate(LocalDate createdDate) {
  this.createdDate = createdDate;
 }



 public List<String> getSkills() {
  return skills;
 }

 public void setSkills(List<String> skills) {
  this.skills = skills;
 }

 public @NotBlank(message = "Full name is required") String getFullName() {
  return fullName;
 }

 public void setFullName(@NotBlank(message = "Full name is required") String fullName) {
  this.fullName = fullName;
 }

 public @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String getEmail() {
  return email;
 }

 public String getId() {
  return id;
 }

 public void setId(String id) {
  this.id = id;
 }

 public void setEmail(@NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email) {
  this.email = email;
 }

 public BigDecimal getRelevantExperience() {
  return relevantExperience;
 }

 public void setRelevantExperience(BigDecimal relevantExperience) {
  this.relevantExperience = relevantExperience;
 }

 public BigDecimal getTotalExperience() {
  return totalExperience;
 }

 public void setTotalExperience(BigDecimal totalExperience) {
  this.totalExperience = totalExperience;
 }

 public @NotBlank(message = "Contact number is required") @Pattern(regexp = "^\\+?[0-9. ()-]{7,15}$", message = "Contact number must be valid") String getContactNumber() {
  return contactNumber;
 }

 public void setContactNumber(@NotBlank(message = "Contact number is required") @Pattern(regexp = "^\\+?[0-9. ()-]{7,15}$", message = "Contact number must be valid") String contactNumber) {
  this.contactNumber = contactNumber;
 }

 public String getTechnology() {
  return technology;
 }

 public void setTechnology(String technology) {
  this.technology = technology;
 }

 public byte[] getResume() {
  return resume;
 }

 public void setResume(byte[] resume) {
  this.resume = resume;
 }

 public String getLinkedin() {
  return linkedin;
 }

 public void setLinkedin(String linkedin) {
  this.linkedin = linkedin;
 }

 public String getReferredBy() {
  return referredBy;
 }

 public void setReferredBy(String referredBy) {
  this.referredBy = referredBy;
 }
}