package com.qlsk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlsk.entity.*;
import com.qlsk.repository.*;
import com.qlsk.security.UserPrincipal;
import com.qlsk.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MVC/security/service/JPA, isolated H2 database, fixtures rolled back after each test. */
@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = true)
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CategoryRepository categories;
    @Autowired EventRepository events;
    @Autowired SeatRepository seats;
    @Autowired RegistrationRepository registrations;
    @Autowired CheckInRepository checkIns;
    @Autowired PasswordEncoder encoder;
    @Autowired EventService eventService;
    User admin, organizer, otherOrganizer, attendee, otherAttendee;
    Category category;
    Event event;
    Seat seat;

    @BeforeEach void fixtures() {
        String encoded = encoder.encode("Test123456");
        admin = account("test_admin", "ROLE_ADMIN", encoded);
        organizer = account("test_organizer", "ROLE_ORGANIZER", encoded);
        otherOrganizer = account("test_other_organizer", "ROLE_ORGANIZER", encoded);
        attendee = account("test_attendee", "ROLE_ATTENDEE", encoded);
        otherAttendee = account("test_other_attendee", "ROLE_ATTENDEE", encoded);
        category = categories.save(Category.builder().name("Integration testing").build());
        event = events.saveAndFlush(Event.builder().title("Integration event").category(category)
                .organizer(organizer).startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(2)).location("Local test venue")
                .maxAttendees(2).estimatedBudget(new BigDecimal("1000000"))
                .status(EventStatus.OPEN_FOR_REGISTRATION).hasSeatMap(true).seatRows(1).seatCols(2).build());
        seat = seats.saveAndFlush(Seat.builder().event(event).seatCode("A1").seatRow(1).seatCol(1)
                .seatType("STANDARD").isBooked(false).build());
    }
    User account(String name, String role, String encoded) {
        return users.saveAndFlush(User.builder().username(name).email(name + "@example.test")
                .password(encoded).fullName(name).role(roles.findByName(role).orElseThrow()).isActive(true).build());
    }
    JsonNode request(User actor, MockHttpServletRequestBuilder request, Object body, int expected) throws Exception {
        if (actor != null) request.with(user(UserPrincipal.create(actor)));
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        String response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }
    JsonNode ticket(User actor, Long seatId) throws Exception {
        return request(actor, post("/api/registrations"), seatId == null
                ? Map.of("eventId", event.getId()) : Map.of("eventId", event.getId(), "seatId", seatId), 200);
    }
    Registration registration(User actor) {
        return registrations.saveAndFlush(Registration.builder().event(event).user(actor)
                .registrationCode("TEST-" + UUID.randomUUID()).qrCodeToken("QR-" + UUID.randomUUID()).build());
    }
}
