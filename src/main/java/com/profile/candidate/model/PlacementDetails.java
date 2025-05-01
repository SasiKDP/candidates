package com.profile.candidate.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Setter
@Getter
@Entity
@Table(name = "placements")
@NoArgsConstructor
@AllArgsConstructor
public class PlacementDetails {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "candidateFullName")
    private String candidateFullName;

    @Pattern(regexp = "^\\d{10}$", message = "contactNumber must be 10 digits")
    @NotBlank(message = "contact number is required")
    @Column(name = "candidateContactNo")
    private String candidateContactNo;

    @Column(name = "technology")
    private String technology;

    @Column(name = "client_name")
    private String clientName;

    @Column(name= "candidateId")
    private String candidateId;

    @Column(name="candidateEmailId")
    private String candidateEmailId;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;


    @Column(name = "recruiter")
    private String recruiter;

    @Column(name = "sales")
    private String sales;

    @DecimalMin(value = "0.0", inclusive = false, message = "Bill Rate must be a positive number")
    @Digits(integer = 10, fraction = 5, message = "Invalid format for Bill Rate")
    @Column(name = "bill_rate")
    private BigDecimal billRate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Pay Rate must be a positive number")
    @Digits(integer = 10, fraction =5, message = "Invalid format for Pay Rate")
    @Column(name = "pay_rate")
    private BigDecimal payRate;

    @Column(name = "gross_profit")
    private BigDecimal grossProfit;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "status")
    private String status = "";

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "created_At")
    private LocalDate createdAt;
    @Column(name= "interview_id")
    private String interviewId;

    // Automatically set the current date
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }
    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCandidateFullName() {
        return candidateFullName;
    }

    public void setCandidateFullName(String candidateFullName) {
        this.candidateFullName = candidateFullName;
    }

    public String getCandidateContactNo() {
        return candidateContactNo;
    }

    public void setCandidateContactNo(String candidateContactNo) {
        this.candidateContactNo = candidateContactNo;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }


    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getCandidateEmailId() {
        return candidateEmailId;
    }

    public void setCandidateEmailId(String candidateEmailId) {
        this.candidateEmailId = candidateEmailId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(String recruiter) {
        this.recruiter = recruiter;
    }

    public String getSales() {
        return sales;
    }

    public void setSales(String sales) {
        this.sales = sales;
    }

    public BigDecimal getBillRate() {
        return billRate;
    }

    public void setBillRate(BigDecimal billRate) {
        this.billRate = billRate;
    }

    public BigDecimal getPayRate() {
        return payRate;
    }

    public void setPayRate(BigDecimal payRate) {
        this.payRate = payRate;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }


    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }
}

