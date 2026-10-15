package com.qlsk;

import com.fasterxml.jackson.databind.JsonNode;
import com.qlsk.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Capacity invariants, including the policy that registered events cannot be edited. */
class EventCapacityKnownIssuesTest extends EventCapacityTestSupport {
    @ParameterizedTest
    @CsvSource(value = {"null,3", "0,3", "-1,3", "2,null", "2,0", "2,-1", "27,1", "1,31"}, nullValues = "null")
    void enabledSeatMapRequiresDimensionsWithinTheFormLimits(Integer rows, Integer cols) throws Exception {
        request(organizer, post("/api/organizer/events"), capacityPayload(10, true, rows, cols), 400);
    }

    @Test void seatMapRegistrationMustSelectASeat() throws Exception {
        long id = open(create(capacityPayload(2, true, 1, 2)));
        register(attendee, id, null, 400);
        assertThat(registrations.countByEventIdAndStatus(id, RegistrationStatus.CONFIRMED)).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void editingCapacityMustKeepItEqualToExistingSeatCount(int requestedCapacity) throws Exception {
        long id = create(capacityPayload(6, true, 2, 3)).path("id").asLong();
        // Rejecting the edit or normalizing it are both acceptable; persisting a mismatch is not.
        attemptCapacityUpdate(id, Map.of("maxAttendees", requestedCapacity));
        assertThat(detail(id).path("maxAttendees").asLong()).isEqualTo(seats.countByEventId(id));
    }

    @Test void editingDimensionsMustKeepMetadataAndPhysicalSeatsConsistent() throws Exception {
        long id = create(capacityPayload(6, true, 2, 3)).path("id").asLong();
        attemptCapacityUpdate(id, Map.of("hasSeatMap", true, "seatRows", 3, "seatCols", 4));
        JsonNode updated = detail(id);
        assertThat(seats.countByEventId(id)).isEqualTo(updated.path("seatRows").asLong() * updated.path("seatCols").asLong());
    }

    @Test void reducingCapacityMustNotGoBelowConfirmedRegistrations() throws Exception {
        long id = open(create(capacityPayload(3, false, 0, 0)));
        register(attendee, id, null, 200);
        register(otherAttendee, id, null, 200);
        attemptCapacityUpdate(id, Map.of("maxAttendees", 1));
        JsonNode updated = detail(id);
        assertThat(updated.path("maxAttendees").asInt()).isGreaterThanOrEqualTo(updated.path("registeredAttendees").asInt());
    }

    @Test void fullRegisteredEventCannotBeEditedToIncreaseCapacity() throws Exception {
        long id = open(create(capacityPayload(1, false, 0, 0)));
        register(attendee, id, null, 200);
        request(organizer, put("/api/organizer/events/" + id), Map.of("maxAttendees", 2), 400);
        assertThat(detail(id).path("status").asText()).isEqualTo("FULL");
        assertThat(detail(id).path("maxAttendees").asInt()).isEqualTo(1);
    }

    @Test void enablingSeatMapMustNotReduceCapacityBelowExistingRegistrations() throws Exception {
        long id = open(create(capacityPayload(3, false, 0, 0)));
        register(attendee, id, null, 200);
        register(otherAttendee, id, null, 200);
        attemptCapacityUpdate(id, Map.of("hasSeatMap", true, "seatRows", 1, "seatCols", 1));
        JsonNode updated = detail(id);
        assertThat(updated.path("maxAttendees").asInt()).isGreaterThanOrEqualTo(updated.path("registeredAttendees").asInt());
    }

    @Test void cancelledSeatMustBeBookableByAnotherAttendee() throws Exception {
        long id = open(create(capacityPayload(1, true, 1, 1)));
        Long seatId = seats.findByEventIdOrderBySeatRowAscSeatColAsc(id).get(0).getId();
        JsonNode ticket = register(attendee, id, seatId, 200);
        request(attendee, post("/api/registrations/" + ticket.path("id").asLong() + "/cancel"), null, 200);
        register(otherAttendee, id, seatId, 200);
        // Flush through the same query used by the public detail endpoint to check DB constraints.
        assertThat(detail(id).path("registeredAttendees").asInt()).isEqualTo(1);
    }
}
