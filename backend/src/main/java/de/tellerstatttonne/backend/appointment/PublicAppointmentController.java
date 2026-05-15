package de.tellerstatttonne.backend.appointment;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/appointments")
public class PublicAppointmentController {

    private final AppointmentService service;

    public PublicAppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<AppointmentDtos.PublicAppointment> list() {
        return service.listPublicUpcoming();
    }
}
