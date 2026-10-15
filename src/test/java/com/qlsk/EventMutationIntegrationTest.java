package com.qlsk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qlsk.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Exercise HTTP authorization, time guards and registration history against real JPA. */
class EventMutationIntegrationTest extends IntegrationTestSupport {
    @ParameterizedTest
    @CsvSource({"organizer,false", "organizer,true", "admin,false", "admin,true"})
    void futureEventWithoutRegistrationsCanBeEditedOrDeleted(String role, boolean deletion) throws Exception {
        mutate(role, deletion, 200);
        if (deletion) {
            assertThat(events.existsById(event.getId())).isFalse();
            assertThat(seats.countByEventId(event.getId())).isZero();
        } else {
            assertThat(events.findById(event.getId()).orElseThrow().getTitle()).isEqualTo("Updated event");
            assertThat(event.getMaxAttendees()).isEqualTo(2);
        }
    }

    @ParameterizedTest
    @CsvSource({"organizer,false", "organizer,true", "admin,false", "admin,true"})
    void eventAtOrPastStartCannotBeEditedOrDeletedEvenIfStatusStillOpen(String role, boolean deletion) throws Exception {
        event.setStartTime(LocalDateTime.now());
        events.saveAndFlush(event);
        mutate(role, deletion, 400);
        assertUnchanged();
    }

    @ParameterizedTest
    @CsvSource({"organizer,false", "organizer,true", "admin,false", "admin,true"})
    void confirmedRegistrationBlocksEditingAndDeletion(String role, boolean deletion) throws Exception {
        Registration registration = registration(attendee);
        mutate(role, deletion, 400);
        assertUnchanged();
        assertThat(registrations.existsById(registration.getId())).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"organizer,false", "organizer,true", "admin,false", "admin,true"})
    void cancelledRegistrationStillBlocksEditingAndDeletion(String role, boolean deletion) throws Exception {
        Registration registration = registration(attendee);
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrations.saveAndFlush(registration);
        mutate(role, deletion, 400);
        assertUnchanged();
        assertThat(registrations.existsById(registration.getId())).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"ONGOING", "COMPLETED"})
    void startedStatusCannotBeBypassedByFutureDates(EventStatus status) throws Exception {
        event.setStatus(status);
        events.saveAndFlush(event);
        mutate("organizer", false, 400);
        mutate("organizer", true, 400);
        assertUnchanged();
    }

    @Test void editingOldEventCannotBypassGuardByMovingStartIntoFuture() throws Exception {
        event.setStartTime(LocalDateTime.now().minusMinutes(1));
        events.saveAndFlush(event);
        request(organizer, put(url("organizer")), Map.of("startTime", LocalDateTime.now().plusDays(3).toString()), 400);
        assertThat(event.getStartTime()).isBefore(LocalDateTime.now());
    }

    @ParameterizedTest
    @ValueSource(strings = {"organizer", "admin"})
    void capacityCannotBeChangedThroughUpdateApi(String role) throws Exception {
        request(actor(role), put(url(role)), Map.of("maxAttendees", 10), 400);
        assertThat(event.getMaxAttendees()).isEqualTo(2);
    }

    @Test void editingOnlyStartMustStillValidateExistingEnd() throws Exception {
        request(organizer, put(url("organizer")), Map.of("startTime", event.getEndTime().plusHours(1).toString()), 400);
    }

    @Test void editingDatesCannotMoveEventIntoPast() throws Exception {
        request(organizer, put(url("organizer")), Map.of("startTime", LocalDateTime.now().minusHours(1).toString()), 400);
    }

    @Test void equalStartAndEndIsRejectedOnUpdate() throws Exception {
        request(organizer, put(url("organizer")), Map.of("endTime", event.getStartTime().toString()), 400);
    }

    @Test void publicMetadataDisablesMutationAfterCancelledRegistration() throws Exception {
        JsonNode before = request(null, get("/api/events/public/" + event.getId()), null, 200);
        assertThat(before.path("canModify").asBoolean()).isTrue();
        Registration registration = registration(attendee);
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrations.saveAndFlush(registration);
        JsonNode after = request(null, get("/api/events/public/" + event.getId()), null, 200);
        assertThat(after.path("registeredAttendees").asInt()).isZero();
        assertThat(after.path("canModify").asBoolean()).isFalse();
        assertThat(after.path("modificationBlockedReason").asText()).isNotBlank();
    }

    @Test void internalDeletionAlsoPreservesRegistrationHistory() {
        registration(attendee);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> eventService.deleteEvent(event.getId()))
                .isInstanceOf(RuntimeException.class);
        assertThat(events.existsById(event.getId())).isTrue();
    }

    @Test void deletingAttendeeAccountMustNotUnlockPreviouslyRegisteredEvent() throws Exception {
        ticket(attendee, seat.getId());
        request(admin, delete("/api/admin/users/" + attendee.getId()), null, 200);
        JsonNode metadata = request(null, get("/api/events/public/" + event.getId()), null, 200);
        assertThat(metadata.path("canModify").asBoolean()).isFalse();
        mutate("organizer", false, 400);
        mutate("admin", true, 400);
    }

    @Test void deletingLegacyAttendeeAccountMustPreserveExistingRegistrationHistory() throws Exception {
        registration(attendee); // Existing data may predate the durable event marker.
        request(admin, delete("/api/admin/users/" + attendee.getId()), null, 200);
        JsonNode metadata = request(null, get("/api/events/public/" + event.getId()), null, 200);
        assertThat(metadata.path("canModify").asBoolean()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void statusAndApprovalRoutesCannotChangeRegisteredOrStartedEvents(boolean started) throws Exception {
        if (started) event.setStartTime(LocalDateTime.now().minusMinutes(1));
        else registration(attendee);
        events.saveAndFlush(event);
        request(organizer, patch(url("organizer") + "/status"), Map.of("status", "DRAFT"), 400);
        request(admin, patch(url("admin") + "/approve"), null, 400);
        assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN_FOR_REGISTRATION);
    }

    private User actor(String role) { return role.equals("admin") ? admin : organizer; }
    private String url(String role) { return "/api/" + role + "/events/" + event.getId(); }
    private void mutate(String role, boolean deletion, int expected) throws Exception {
        request(actor(role), deletion ? delete(url(role)) : put(url(role)),
                deletion ? null : Map.of("title", "Updated event"), expected);
    }
    private void assertUnchanged() {
        assertThat(events.findById(event.getId()).orElseThrow().getTitle()).isEqualTo("Integration event");
        assertThat(seats.existsById(seat.getId())).isTrue();
    }
}
