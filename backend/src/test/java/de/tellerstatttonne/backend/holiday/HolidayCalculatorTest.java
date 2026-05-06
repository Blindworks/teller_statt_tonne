package de.tellerstatttonne.backend.holiday;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HolidayCalculatorTest {

    @Test
    void easterSunday_matchesKnownDates() {
        assertThat(HolidayCalculator.easterSunday(2025)).isEqualTo(LocalDate.of(2025, 4, 20));
        assertThat(HolidayCalculator.easterSunday(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(HolidayCalculator.easterSunday(2024)).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    void forYear_2026_containsAllExpectedHolidays() {
        List<Holiday> holidays = HolidayCalculator.forYear(2026);

        Map<String, LocalDate> byName = holidays.stream()
            .collect(java.util.stream.Collectors.toMap(Holiday::name, Holiday::date));

        assertThat(byName).containsEntry("Neujahr", LocalDate.of(2026, 1, 1));
        assertThat(byName).containsEntry("Karfreitag", LocalDate.of(2026, 4, 3));
        assertThat(byName).containsEntry("Ostermontag", LocalDate.of(2026, 4, 6));
        assertThat(byName).containsEntry("Tag der Arbeit", LocalDate.of(2026, 5, 1));
        assertThat(byName).containsEntry("Christi Himmelfahrt", LocalDate.of(2026, 5, 14));
        assertThat(byName).containsEntry("Pfingstmontag", LocalDate.of(2026, 5, 25));
        assertThat(byName).containsEntry("Fronleichnam", LocalDate.of(2026, 6, 4));
        assertThat(byName).containsEntry("Tag der Deutschen Einheit", LocalDate.of(2026, 10, 3));
        assertThat(byName).containsEntry("Allerheiligen", LocalDate.of(2026, 11, 1));
        assertThat(byName).containsEntry("1. Weihnachtstag", LocalDate.of(2026, 12, 25));
        assertThat(byName).containsEntry("2. Weihnachtstag", LocalDate.of(2026, 12, 26));
    }

    @Test
    void forYear_marksNrwSpecificHolidays() {
        List<Holiday> holidays = HolidayCalculator.forYear(2026);
        assertThat(holidays).filteredOn(h -> h.name().equals("Fronleichnam"))
            .singleElement().extracting(Holiday::region).isEqualTo("NRW");
        assertThat(holidays).filteredOn(h -> h.name().equals("Allerheiligen"))
            .singleElement().extracting(Holiday::region).isEqualTo("NRW");
        assertThat(holidays).filteredOn(h -> h.name().equals("Neujahr"))
            .singleElement().extracting(Holiday::region).isEqualTo("DE");
    }

    @Test
    void forYear_returnsHolidaysSortedByDate() {
        List<Holiday> holidays = HolidayCalculator.forYear(2026);
        for (int i = 1; i < holidays.size(); i++) {
            assertThat(holidays.get(i).date()).isAfterOrEqualTo(holidays.get(i - 1).date());
        }
    }
}
