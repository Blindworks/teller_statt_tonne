package de.tellerstatttonne.backend.partner;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partners/{partnerId}/members")
public class PartnerMemberController {

    private final PartnerMemberService service;

    public PartnerMemberController(PartnerMemberService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<StoreMember>> list(@PathVariable Long partnerId) {
        return service.list(partnerId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<Void> assign(
        @PathVariable Long partnerId,
        @PathVariable Long memberId
    ) {
        return switch (service.assign(partnerId, memberId)) {
            case OK -> ResponseEntity.noContent().build();
            case PARTNER_NOT_FOUND, MEMBER_NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> unassign(
        @PathVariable Long partnerId,
        @PathVariable Long memberId
    ) {
        return switch (service.unassign(partnerId, memberId)) {
            case OK -> ResponseEntity.noContent().build();
            case PARTNER_NOT_FOUND, MEMBER_NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }
}
