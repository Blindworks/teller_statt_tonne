package de.tellerstatttonne.backend.holiday;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HolidayService {

    public List<Holiday> findBetween(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return List.of();
        }
        List<Holiday> result = new ArrayList<>();
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            for (Holiday h : HolidayCalculator.forYear(year)) {
                if (!h.date().isBefore(from) && !h.date().isAfter(to)) {
                    result.add(h);
                }
            }
        }
        return result;
    }
}
