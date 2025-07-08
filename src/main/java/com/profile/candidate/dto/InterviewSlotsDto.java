package com.profile.candidate.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class InterviewSlotsDto {

    private String userId;

    private List<InterviewDateWithDuration> bookedSlots;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<InterviewDateWithDuration> getBookedSlots() {
        return bookedSlots;
    }

    public void setBookedSlots(List<InterviewDateWithDuration> bookedSlots) {
        this.bookedSlots = bookedSlots;
    }

    public static class InterviewDateWithDuration{

        private OffsetDateTime interviewDateTime;

        private int duration;

        public OffsetDateTime getInterviewDateTime() {
            return interviewDateTime;
        }

        public void setInterviewDateTime(OffsetDateTime interviewDateTime) {
            this.interviewDateTime = interviewDateTime;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }
    }
}