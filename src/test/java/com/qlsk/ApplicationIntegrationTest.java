package com.qlsk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qlsk.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApplicationIntegrationTest extends IntegrationTestSupport {
    @ParameterizedTest
    @ValueSource(strings = {"/", "/login.html", "/register.html", "/event-detail.html", "/my-tickets.html",
            "/registered-events.html", "/profile.html", "/organizer/index.html", "/organizer/events.html",
            "/organizer/event-list.html", "/organizer/event-form.html", "/organizer/checkin.html",
            "/organizer/reports.html", "/organizer/profile.html", "/admin/index.html"})
    void staticPagesLoad(String path) throws Exception {
        if (path.equals("/")) {
            // MockMvc does not execute the welcome-page servlet forward.
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
            return;
        }
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
    @Test void loginIssuesUsableJwt() throws Exception {
        JsonNode login = request(null, post("/api/auth/login"), Map.of("usernameOrEmail", attendee.getUsername(), "password", "Test123456"), 200);
        assertThat(login.path("accessToken").asText()).isNotBlank();
        mvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + login.path("accessToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(attendee.getUsername()));
    }
    @Test void loginByEmail() throws Exception {
        request(null, post("/api/auth/login"), Map.of("usernameOrEmail", attendee.getEmail(), "password", "Test123456"), 200);
    }
    @Test void wrongPasswordIsRejected() throws Exception {
        request(null, post("/api/auth/login"), Map.of("usernameOrEmail", attendee.getUsername(), "password", "wrong"), 401);
    }
    @Test void blankLoginIsRejected() throws Exception { request(null, post("/api/auth/login"), Map.of(), 400); }
    @Test void registrationCreatesAttendee() throws Exception {
        JsonNode result = request(null, post("/api/auth/register"), Map.of("username", "new_test_user", "password", "Test123456",
                "fullName", "Test User", "email", "new@example.test"), 200);
        assertThat(result.path("role").asText()).isEqualTo("ROLE_ATTENDEE");
        assertThat(result.has("password")).isFalse();
    }
    @Test void invalidRegistrationIsRejected() throws Exception {
        request(null, post("/api/auth/register"), Map.of("username", "a", "password", "x", "fullName", "", "email", "invalid"), 400);
    }
    @Test void duplicateRegistrationIsRejected() throws Exception {
        request(null, post("/api/auth/register"), Map.of("username", attendee.getUsername(), "password", "Test123456",
                "fullName", "Duplicate", "email", "duplicate@example.test"), 400);
    }
    @Test void anonymousCannotReadPrivateProfile() throws Exception { request(null, get("/api/users/profile"), null, 403); }
    @Test void attendeeCannotUseAdminApi() throws Exception { request(attendee, get("/api/admin/users"), null, 403); }
    @Test void attendeeCannotCreateEvents() throws Exception { request(attendee, post("/api/organizer/events"), Map.of(), 403); }
    @Test void publicSearchAndCategoryFilter() throws Exception {
        JsonNode result = request(null, get("/api/events/public").param("keyword", "Integration").param("categoryId", category.getId().toString()), null, 200);
        assertThat(result.size()).isEqualTo(1);
        request(null, get("/api/categories/" + category.getId()), null, 200);
        request(null, get("/api/events/public/" + event.getId() + "/seats"), null, 200);
    }
    @Test void eventCreationGeneratesSeatsAndApprovalOpensRegistration() throws Exception {
        JsonNode created = request(organizer, post("/api/organizer/events"), Map.of("title", "New test event", "categoryId", category.getId(),
                "startTime", LocalDateTime.now().plusDays(3).toString(), "endTime", LocalDateTime.now().plusDays(3).plusHours(1).toString(),
                "location", "Test room", "maxAttendees", 10, "hasSeatMap", true, "seatRows", 2, "seatCols", 3), 200);
        assertThat(created.path("status").asText()).isEqualTo("DRAFT");
        assertThat(created.path("maxAttendees").asInt()).isEqualTo(6);
        assertThat(seats.countByEventId(created.path("id").asLong())).isEqualTo(6);
        JsonNode approved = request(admin, patch("/api/admin/events/" + created.path("id").asLong() + "/approve"), null, 200);
        assertThat(approved.path("status").asText()).isEqualTo("OPEN_FOR_REGISTRATION");
    }
    @Test void invalidEventIsRejected() throws Exception { request(organizer, post("/api/organizer/events"), Map.of(), 400); }
    @Test void ticketBooksSeatAndGeneratesQr() throws Exception {
        JsonNode t = ticket(attendee, seat.getId());
        assertThat(t.path("seatCode").asText()).isEqualTo("A1");
        assertThat(t.path("qrCodeImageBase64").asText()).startsWith("data:image/png;base64,iVBOR");
        assertThat(seats.findById(seat.getId()).orElseThrow().getIsBooked()).isTrue();
    }
    @Test void duplicateTicketIsRejected() throws Exception {
        ticket(attendee, seat.getId()); request(attendee, post("/api/registrations"), Map.of("eventId", event.getId()), 400);
    }
    @Test void bookedSeatIsRejected() throws Exception {
        ticket(attendee, seat.getId()); request(otherAttendee, post("/api/registrations"), Map.of("eventId", event.getId(), "seatId", seat.getId()), 400);
    }
    @Test void draftEventCannotBeBooked() throws Exception {
        event.setStatus(EventStatus.DRAFT); events.saveAndFlush(event);
        request(attendee, post("/api/registrations"), Map.of("eventId", event.getId()), 400);
    }
    @Test void lastTicketMarksFullAndCancellationReopens() throws Exception {
        event.setMaxAttendees(1); events.saveAndFlush(event);
        JsonNode t = ticket(attendee, seat.getId());
        assertThat(event.getStatus()).isEqualTo(EventStatus.FULL);
        request(attendee, post("/api/registrations/" + t.path("id").asLong() + "/cancel"), null, 200);
        assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN_FOR_REGISTRATION);
        assertThat(seat.getIsBooked()).isFalse();
    }
    @Test void anotherAttendeeCannotCancelTicket() throws Exception {
        Registration reg = registration(attendee);
        request(otherAttendee, post("/api/registrations/" + reg.getId() + "/cancel"), null, 400);
        assertThat(reg.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
    }
    @Test void checkInSucceedsAndDuplicateIsRejected() throws Exception {
        Registration reg = registration(attendee);
        Map<String, Object> body = Map.of("eventId", event.getId(), "qrCodeToken", reg.getQrCodeToken());
        JsonNode result = request(organizer, post("/api/organizer/check-in"), body, 200);
        assertThat(result.path("success").asBoolean()).isTrue();
        request(organizer, post("/api/organizer/check-in"), body, 400);
        request(attendee, post("/api/registrations/" + reg.getId() + "/cancel"), null, 400);
    }
    @Test void cancelledQrIsRejected() throws Exception {
        Registration reg = registration(attendee); reg.setStatus(RegistrationStatus.CANCELLED); registrations.saveAndFlush(reg);
        request(organizer, post("/api/organizer/check-in"), Map.of("eventId", event.getId(), "qrCodeToken", reg.getQrCodeToken()), 400);
    }
    @Test void feedbackRequiresAttendanceAndCompletion() throws Exception {
        request(attendee, post("/api/feedbacks"), Map.of("eventId", event.getId(), "rating", 5), 400);
        event.setStatus(EventStatus.COMPLETED); events.saveAndFlush(event);
        Registration reg = registration(attendee);
        request(attendee, post("/api/feedbacks"), Map.of("eventId", event.getId(), "rating", 5), 400);
        checkIns.saveAndFlush(CheckIn.builder().event(event).registration(reg).checkedInBy(organizer).build());
        request(attendee, post("/api/feedbacks"), Map.of("eventId", event.getId(), "rating", 5, "comment", "Good"), 200);
        request(attendee, post("/api/feedbacks"), Map.of("eventId", event.getId(), "rating", 4), 400);
        assertThat(request(null, get("/api/feedbacks/event/" + event.getId()), null, 200).size()).isEqualTo(1);
    }
    @Test void expensesAndReportsUseCorrectTotals() throws Exception {
        JsonNode expense = request(organizer, post("/api/organizer/expenses"), Map.of("eventId", event.getId(), "title", "Test expense",
                "amount", 250000, "expenseDate", "2026-10-15"), 200);
        JsonNode financial = request(organizer, get("/api/reports/event/" + event.getId() + "/financial"), null, 200);
        assertThat(financial.path("remainingBudget").decimalValue()).isEqualByComparingTo("750000");
        assertThat(financial.path("budgetUsedRate").asDouble()).isEqualTo(25);
        request(organizer, delete("/api/organizer/expenses/" + expense.path("id").asLong()), null, 200);
        assertThat(request(organizer, get("/api/reports/event/" + event.getId() + "/attendance"), null, 200).path("attendanceRate").asDouble()).isZero();
        request(organizer, get("/api/reports/event/" + event.getId() + "/feedback"), null, 200);
        request(admin, get("/api/reports/admin/dashboard"), null, 200);
        request(admin, get("/api/reports/admin/charts"), null, 200);
    }
    @Test void negativeExpenseIsRejected() throws Exception {
        request(organizer, post("/api/organizer/expenses"), Map.of("eventId", event.getId(), "title", "Invalid", "amount", -1, "expenseDate", "2026-10-15"), 400);
    }
    @Test void profileUpdateAndPasswordChange() throws Exception {
        request(attendee, put("/api/users/profile"), Map.of("fullName", "Updated Name", "phone", "0900000000"), 200);
        request(attendee, put("/api/users/profile"), Map.of("oldPassword", "wrong", "newPassword", "New123456"), 400);
        request(attendee, put("/api/users/profile"), Map.of("oldPassword", "Test123456", "newPassword", "New123456"), 200);
        assertThat(encoder.matches("New123456", attendee.getPassword())).isTrue();
    }
    @Test void adminCannotDisableSelf() throws Exception { request(admin, patch("/api/admin/users/" + admin.getId() + "/toggle-status"), null, 400); }
    @Test void adminUserCrud() throws Exception {
        JsonNode created = request(admin, post("/api/admin/users"), Map.of("username", "crud_user", "email", "crud@example.test", "password", "Test123456", "role", "ROLE_ATTENDEE"), 200);
        long id = created.path("id").asLong();
        request(admin, put("/api/admin/users/" + id), Map.of("fullName", "Updated User"), 200);
        request(admin, patch("/api/admin/users/" + id + "/role"), Map.of("role", "ROLE_ORGANIZER"), 200);
        request(admin, patch("/api/admin/users/" + id + "/toggle-status"), null, 200);
        request(admin, delete("/api/admin/users/" + id), null, 200);
        assertThat(users.existsById(id)).isFalse();
    }
    @Test void categoryCrud() throws Exception {
        JsonNode created = request(admin, post("/api/categories"), Map.of("name", "Test new category", "description", "Test"), 200);
        long id = created.path("id").asLong();
        request(admin, put("/api/categories/" + id), Map.of("name", "Updated category", "description", "Updated"), 200);
        request(admin, delete("/api/categories/" + id), null, 200);
        assertThat(categories.existsById(id)).isFalse();
    }
    @Test void organizerCannotListAnotherOrganizersRegistrations() throws Exception {
        request(otherOrganizer, get("/api/organizer/events/" + event.getId() + "/registrations"), null, 403);
    }
    @Test void deleteEventRemovesDependencies() throws Exception {
        Registration reg = registration(attendee);
        checkIns.saveAndFlush(CheckIn.builder().event(event).registration(reg).checkedInBy(organizer).build());
        request(admin, delete("/api/admin/events/" + event.getId()), null, 200);
        assertThat(events.existsById(event.getId())).isFalse();
        assertThat(registrations.existsById(reg.getId())).isFalse();
        assertThat(seats.countByEventId(event.getId())).isZero();
    }
    @Test void uploadRejectsEmptyOrNonImageFiles() throws Exception {
        mvc.perform(multipart("/api/upload").file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0])))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/upload").file(new MockMultipartFile("file", "test.txt", "text/plain", "text".getBytes())))
                .andExpect(status().isBadRequest());
    }
}
