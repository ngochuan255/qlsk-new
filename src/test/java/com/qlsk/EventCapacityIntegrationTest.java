package com.qlsk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qlsk.entity.*;
import com.qlsk.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Characterization of supported creation, validation and sequential capacity accounting. */
class EventCapacityIntegrationTest extends EventCapacityTestSupport {
    @ParameterizedTest
    @ValueSource(ints = {4, 6, 100})
    void seatCountOverridesEnteredCapacity(int enteredCapacity) throws Exception {
        JsonNode created = create(capacityPayload(enteredCapacity, true, 2, 3));
        long id = created.path("id").asLong();
        assertThat(created.path("maxAttendees").asInt()).isEqualTo(6);
        assertThat(created.path("remainingSeats").asInt()).isEqualTo(6);
        assertThat(created.path("registeredAttendees").asInt()).isZero();
        assertThat(created.path("status").asText()).isEqualTo("DRAFT");
        assertThat(created.path("organizerId").asLong()).isEqualTo(organizer.getId());
        List<Seat> generated = seats.findByEventIdOrderBySeatRowAscSeatColAsc(id);
        assertThat(generated).extracting(Seat::getSeatCode).containsExactly("A1", "A2", "A3", "B1", "B2", "B3");
        assertThat(generated).allMatch(s -> !s.getIsBooked());
    }

    @Test void withoutSeatMapCapacityIsPreservedAndNoSeatsAreCreated() throws Exception {
        JsonNode created = create(capacityPayload(17, false, 2, 3));
        assertThat(created.path("maxAttendees").asInt()).isEqualTo(17);
        assertThat(created.path("remainingSeats").asInt()).isEqualTo(17);
        assertThat(seats.countByEventId(created.path("id").asLong())).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nonPositiveCapacityIsRejectedEvenWithValidSeatMap(int capacity) throws Exception {
        request(organizer, post("/api/organizer/events"), capacityPayload(capacity, true, 2, 3), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "categoryId", "startTime", "endTime", "location", "maxAttendees"})
    void requiredCreationFieldsAreValidated(String field) throws Exception {
        Map<String, Object> body = capacityPayload(10, false, 0, 0);
        body.remove(field);
        request(organizer, post("/api/organizer/events"), body, 400);
    }

    @Test void endBeforeStartIsRejected() throws Exception {
        Map<String, Object> body = capacityPayload(10, false, 0, 0);
        body.put("endTime", LocalDateTime.now().plusDays(1).toString());
        request(organizer, post("/api/organizer/events"), body, 400);
    }

    @Test void anonymousAndAttendeeCannotCreateValidEvents() throws Exception {
        request(null, post("/api/organizer/events"), capacityPayload(10, false, 0, 0), 403);
        request(attendee, post("/api/organizer/events"), capacityPayload(10, false, 0, 0), 403);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void capacityReachedClosesRegistrationAndCancellationRestoresOnePlace(boolean seatMap) throws Exception {
        JsonNode created = create(capacityPayload(2, seatMap, 1, 2));
        long id = open(created);
        List<Seat> generated = seats.findByEventIdOrderBySeatRowAscSeatColAsc(id);
        register(attendee, id, seatMap ? generated.get(0).getId() : null, 200);
        JsonNode second = register(otherAttendee, id, seatMap ? generated.get(1).getId() : null, 200);
        JsonNode full = detail(id);
        assertThat(full.path("status").asText()).isEqualTo("FULL");
        assertThat(full.path("registeredAttendees").asInt()).isEqualTo(2);
        assertThat(full.path("remainingSeats").asInt()).isZero();
        register(otherOrganizer, id, null, 400);
        request(otherAttendee, post("/api/registrations/" + second.path("id").asLong() + "/cancel"), null, 200);
        JsonNode reopened = detail(id);
        assertThat(reopened.path("status").asText()).isEqualTo("OPEN_FOR_REGISTRATION");
        assertThat(reopened.path("registeredAttendees").asInt()).isEqualTo(1);
        assertThat(reopened.path("remainingSeats").asInt()).isEqualTo(1);
        if (seatMap) assertThat(seats.countByEventIdAndIsBookedFalse(id)).isEqualTo(1);
    }
}

abstract class EventCapacityTestSupport extends IntegrationTestSupport {
    Map<String, Object> capacityPayload(int capacity, boolean seatMap, Integer rows, Integer cols) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Capacity audit event");
        body.put("categoryId", category.getId());
        body.put("startTime", LocalDateTime.now().plusDays(3).toString());
        body.put("endTime", LocalDateTime.now().plusDays(3).plusHours(1).toString());
        body.put("location", "Capacity audit room");
        body.put("maxAttendees", capacity);
        body.put("hasSeatMap", seatMap);
        body.put("seatRows", rows);
        body.put("seatCols", cols);
        return body;
    }
    JsonNode create(Map<String, Object> body) throws Exception {
        return request(organizer, post("/api/organizer/events"), body, 200);
    }
    long open(JsonNode created) throws Exception {
        long id = created.path("id").asLong();
        request(admin, patch("/api/admin/events/" + id + "/approve"), null, 200);
        return id;
    }
    JsonNode detail(long id) throws Exception {
        return request(null, get("/api/events/public/" + id), null, 200);
    }
    void attemptCapacityUpdate(long id, Map<String, Object> body) throws Exception {
        int status = mvc.perform(put("/api/organizer/events/" + id)
                .with(user(UserPrincipal.create(organizer)))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body))).andReturn().getResponse().getStatus();
        // A safe implementation may reject the change or preserve the invariant while accepting it.
        assertThat(status).isIn(200, 400);
    }
    JsonNode register(User actor, long eventId, Long seatId, int expected) throws Exception {
        return request(actor, post("/api/registrations"), seatId == null
                ? Map.of("eventId", eventId) : Map.of("eventId", eventId, "seatId", seatId), expected);
    }
}
