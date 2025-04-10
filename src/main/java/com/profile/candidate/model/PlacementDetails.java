package com.profile.candidate.model;

import jakarta.persistence.*;

import lombok.*;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Setter
@Getter
@Entity
@Table(name = "placement_prod")
@NoArgsConstructor
@AllArgsConstructor
public class PlacementDetails {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id; // e.g. PLACEMENT001

    @Column(name = "consultant_name")
    private String consultantName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is mandatory")
    @Column(name = "consultant_email")
    private String consultantEmail;

    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits")
    @NotBlank(message = "Phone number is required")
    @Column(name = "phone")
    private String phone;

    @Column(name = "technology")
    private String technology;

    @Column(name = "client")
    private String client;

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
    @Column(name = "bill_rate_usd")
    private BigDecimal billRateUSD;

    @Column(name = "bill_rate_inr")
    private BigDecimal billRateINR;

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
    private String status;

    @Column(name = "status_message")
    private String statusMessage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public String getConsultantEmail() {
        return consultantEmail;
    }

    public void setConsultantEmail(String consultantEmail) {
        this.consultantEmail = consultantEmail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
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

    public BigDecimal getBillRateUSD() {
        return billRateUSD;
    }

    public void setBillRateUSD(BigDecimal billRateUSD) {
        this.billRateUSD = billRateUSD;
    }

    public BigDecimal getBillRateINR() {
        return billRateINR;
    }

    public void setBillRateINR(BigDecimal billRateINR) {
        this.billRateINR = billRateINR;
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
}

