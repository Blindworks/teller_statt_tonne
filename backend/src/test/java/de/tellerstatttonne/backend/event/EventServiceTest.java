package de.tellerstatttonne.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EventServiceTest {

    @Autowired private EventService service;
    @Autowired private EventRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void createPersistsEvent() {
        LocalDate today = LocalDate.now();
        Event dto = sample("Sommerfest", today, today.plusDays(2));

        Event created = service.create(dto);

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Sommerfest");
        assertThat(created.startDate()).isEqualTo(today);
        assertThat(created.endDate()).isEqualTo(today.plusDays(2));
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void rejectsEndBeforeStart() {
        LocalDate today = LocalDate.now();
        Event bad = sample("Bad", today.plusDays(2), today);
        assertThatThrownBy(() -> service.create(bad))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankName() {
        LocalDate today = LocalDate.now();
        Event bad = sample("", today, today);
        assertThatThrownBy(() -> service.create(bad))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scopeFiltersActiveAndPast() {
        LocalDate today = LocalDate.now();
        service.create(sample("Past", today.minusDays(10), today.minusDays(5)));
        service.create(sample("Today", today, today));
        service.create(sample("Future", today.plusDays(3), today.plusDays(5)));

        List<Event> active = service.findAll(EventService.Scope.ACTIVE);
        List<Event> past = service.findAll(EventService.Scope.PAST);
        List<Event> all = service.findAll(EventService.Scope.ALL);

        assertThat(active).extracting(Event::name).containsExactlyInAnyOrder("Today", "Future");
        assertThat(past).extracting(Event::name).containsExactly("Past");
        assertThat(all).hasSize(3);
    }

    @Test
    void deleteRemoves() {
        LocalDate today = LocalDate.now();
        Event created = service.create(sample("X", today, today));
        assertThat(service.delete(created.id())).isTrue();
        assertThat(service.findById(created.id())).isEmpty();
    }

    @Test
    void updateAppliesChanges() {
        LocalDate today = LocalDate.now();
        Event created = service.create(sample("X", today, today));
        Event updated = service.update(created.id(),
            new Event(null, "Y", "Beschreibung", today, today.plusDays(1),
                null, null, "Berlin", null, null, null, null)).orElseThrow();
        assertThat(updated.name()).isEqualTo("Y");
        assertThat(updated.endDate()).isEqualTo(today.plusDays(1));
    }

    @Test
    void rejectsMissingLocation() {
        LocalDate today = LocalDate.now();
        Event bad = new Event(null, "Ohne Ort", null, today, today,
            null, null, null, null, null, null, new Event.Contact(null, null, null));
        assertThatThrownBy(() -> service.create(bad))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Lokalität");
    }

    @Test
    void acceptsCoordinatesOnlyAsLocation() {
        LocalDate today = LocalDate.now();
        Event dto = new Event(null, "Pin-only", null, today, today,
            null, null, null, 50.18, 8.74, null, new Event.Contact(null, null, null));
        Event created = service.create(dto);
        assertThat(created.latitude()).isEqualTo(50.18);
        assertThat(created.longitude()).isEqualTo(8.74);
    }

    @Test
    void respectsCoordinatesFromDtoOnUpdate() {
        LocalDate today = LocalDate.now();
        Event created = service.create(sample("Mit Adresse", today, today));
        Event withPin = new Event(created.id(), created.name(), created.description(),
            created.startDate(), created.endDate(),
            created.street(), created.postalCode(), created.city(),
            12.34, 56.78, created.logoUrl(), created.contact());
        Event updated = service.update(created.id(), withPin).orElseThrow();
        assertThat(updated.latitude()).isEqualTo(12.34);
        assertThat(updated.longitude()).isEqualTo(56.78);
    }

    private static Event sample(String name, LocalDate start, LocalDate end) {
        return new Event(null, name, null, start, end,
            "Hauptstraße 1", "10115", "Berlin",
            null, null, null,
            new Event.Contact(null, null, null));
    }
}
