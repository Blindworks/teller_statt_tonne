package de.tellerstatttonne.backend.partner;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    private final PartnerService service;
    private final PartnerRepository partnerRepository;

    public PartnerController(PartnerService service, PartnerRepository partnerRepository) {
        this.service = service;
        this.partnerRepository = partnerRepository;
    }

    @GetMapping
    public List<Partner> list() {
        return service.findAll();
    }

    @GetMapping("/member-counts")
    public Map<Long, Integer> memberCounts() {
        return partnerRepository.countMembersGroupedByPartner().stream()
            .collect(Collectors.toMap(
                PartnerRepository.MemberCountRow::getPartnerId,
                row -> row.getMemberCount() != null ? row.getMemberCount() : 0
            ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Partner> get(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Partner> create(@RequestBody Partner partner) {
        Partner created = service.create(partner);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Partner> update(@PathVariable Long id, @RequestBody Partner partner) {
        return service.update(id, partner)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/geocode")
    public ResponseEntity<Partner> regeocode(@PathVariable Long id) {
        return service.regeocode(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
