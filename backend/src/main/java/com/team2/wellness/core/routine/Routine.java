package com.team2.wellness.core.routine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "routines")
public class Routine {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "days_of_week", nullable = false, length = 30)
    private String daysOfWeek;

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Routine() {
    }

    public Routine(
            UUID ownerId,
            String title,
            String category,
            Set<DayOfWeek> daysOfWeek,
            LocalTime reminderTime,
            String timezone,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.title = title;
        this.category = category;
        this.daysOfWeek = serializeDays(daysOfWeek);
        this.reminderTime = reminderTime;
        this.timezone = timezone;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String title,
            String category,
            Set<DayOfWeek> daysOfWeek,
            LocalTime reminderTime,
            String timezone,
            LocalDate startDate,
            LocalDate endDate,
            boolean active
    ) {
        this.title = title;
        this.category = category;
        this.daysOfWeek = serializeDays(daysOfWeek);
        this.reminderTime = reminderTime;
        this.timezone = timezone;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public boolean isScheduledOn(LocalDate date) {
        if (!active || date.isBefore(startDate) || (endDate != null && date.isAfter(endDate))) {
            return false;
        }
        return getDaysOfWeek().contains(date.getDayOfWeek());
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return java.util.Arrays.stream(daysOfWeek.split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    private static String serializeDays(Set<DayOfWeek> days) {
        return days.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }
}
