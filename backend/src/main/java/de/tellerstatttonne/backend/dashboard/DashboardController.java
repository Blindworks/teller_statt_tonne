package de.tellerstatttonne.backend.dashboard;

import de.tellerstatttonne.backend.auth.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/day")
    public List<DaySlot> day(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return service.findDaySlots(date != null ? date : LocalDate.now(), CurrentUser.requireId());
    }

    @GetMapping("/range")
    public List<DaySlot> range(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(6);
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("'to' must be on or after 'from'");
        }
        if (start.plusDays(31).isBefore(end)) {
            throw new IllegalArgumentException("range must not exceed 31 days");
        }
        return service.findRangeSlots(start, end, CurrentUser.requireId());
    }
}
