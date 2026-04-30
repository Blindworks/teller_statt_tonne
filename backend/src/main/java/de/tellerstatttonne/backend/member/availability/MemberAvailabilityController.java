package de.tellerstatttonne.backend.member.availability;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/{memberId}/availabilities")
public class MemberAvailabilityController {

    private final MemberAvailabilityService service;

    public MemberAvailabilityController(MemberAvailabilityService service) {
        this.service = service;
    }

    @GetMapping
    public List<MemberAvailability> list(@PathVariable Long memberId) {
        return service.findByMemberId(memberId);
    }

    @PutMapping
    public List<MemberAvailability> replaceAll(
        @PathVariable Long memberId,
        @RequestBody List<MemberAvailability> items
    ) {
        return service.replaceAll(memberId, items);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
