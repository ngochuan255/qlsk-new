package com.qlsk;

import com.qlsk.entity.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Expected requirements: failures reproduce defects rather than accepting unsafe behavior. */
class KnownIssuesTest extends IntegrationTestSupport {
    @Test void organizerCannotEditAnotherOrganizersEvent() throws Exception {
        request(otherOrganizer, put("/api/organizer/events/" + event.getId()), Map.of("title", "Unauthorized edit"), 403);
    }
    @Test void organizerCannotDeleteAnotherOrganizersEvent() throws Exception {
        request(otherOrganizer, delete("/api/organizer/events/" + event.getId()), null, 403);
    }
    @Test void organizerCannotChangeAnotherOrganizersEventStatus() throws Exception {
        request(otherOrganizer, patch("/api/organizer/events/" + event.getId() + "/status"), Map.of("status", "CANCELLED"), 403);
    }
    @Test void organizerCannotCheckInAnotherOrganizersAttendee() throws Exception {
        Registration reg = registration(attendee);
        request(otherOrganizer, post("/api/organizer/check-in"), Map.of("eventId", event.getId(), "qrCodeToken", reg.getQrCodeToken()), 403);
    }
    @Test void organizerCannotReadAnotherOrganizersReport() throws Exception {
        request(otherOrganizer, get("/api/reports/event/" + event.getId() + "/financial"), null, 403);
    }
    @Test void organizerCannotChargeAnotherOrganizersEvent() throws Exception {
        request(otherOrganizer, post("/api/organizer/expenses"), Map.of("eventId", event.getId(), "title", "Unauthorized expense",
                "amount", 100000, "expenseDate", "2026-10-15"), 403);
    }
    @Test void readingTicketsMustPreserveCompletedAttendanceHistory() throws Exception {
        Registration reg = registration(attendee);
        event.setStatus(EventStatus.COMPLETED); events.saveAndFlush(event);
        checkIns.saveAndFlush(CheckIn.builder().event(event).registration(reg).checkedInBy(organizer).build());
        request(attendee, get("/api/registrations/my-tickets"), null, 200);
        assertThat(registrations.existsById(reg.getId())).as("Reading tickets must preserve registration history").isTrue();
        assertThat(checkIns.existsByRegistrationId(reg.getId())).as("Attendance must remain available for reports and feedback").isTrue();
    }
    @Test void fullEventMustAdvanceToOngoing() {
        event.setStatus(EventStatus.FULL);
        event.setStartTime(LocalDateTime.now().minusMinutes(5));
        event.setEndTime(LocalDateTime.now().plusHours(1));
        events.saveAndFlush(event);
        eventService.updateEventLifecycle();
        assertThat(event.getStatus()).isEqualTo(EventStatus.ONGOING);
    }
}
