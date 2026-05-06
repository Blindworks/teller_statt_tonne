package de.tellerstatttonne.backend.holiday;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class HolidayCalculator {

    private HolidayCalculator() {}

    public static List<Holiday> forYear(int year) {
        List<Holiday> result = new ArrayList<>();
        LocalDate easter = easterSunday(year);

        result.add(new Holiday(LocalDate.of(year, 1, 1), "Neujahr", "DE"));
        result.add(new Holiday(easter.minusDays(2), "Karfreitag", "DE"));
        result.add(new Holiday(easter.plusDays(1), "Ostermontag", "DE"));
        result.add(new Holiday(LocalDate.of(year, 5, 1), "Tag der Arbeit", "DE"));
        result.add(new Holiday(easter.plusDays(39), "Christi Himmelfahrt", "DE"));
        result.add(new Holiday(easter.plusDays(50), "Pfingstmontag", "DE"));
        result.add(new Holiday(easter.plusDays(60), "Fronleichnam", "NRW"));
        result.add(new Holiday(LocalDate.of(year, 10, 3), "Tag der Deutschen Einheit", "DE"));
        result.add(new Holiday(LocalDate.of(year, 11, 1), "Allerheiligen", "NRW"));
        result.add(new Holiday(LocalDate.of(year, 12, 25), "1. Weihnachtstag", "DE"));
        result.add(new Holiday(LocalDate.of(year, 12, 26), "2. Weihnachtstag", "DE"));

        result.sort((a, b) -> a.date().compareTo(b.date()));
        return result;
    }

    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
