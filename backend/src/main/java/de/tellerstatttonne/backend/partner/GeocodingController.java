package de.tellerstatttonne.backend.partner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private final GeocodingService service;

    public GeocodingController(GeocodingService service) {
        this.service = service;
    }

    @GetMapping("/reverse")
    public ResponseEntity<GeocodingService.ReverseResult> reverse(
        @RequestParam double lat,
        @RequestParam double lon
    ) {
        return service.reverseGeocode(lat, lon)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/forward")
    public ResponseEntity<GeocodingService.Coordinates> forward(
        @RequestParam(required = false) String street,
        @RequestParam(required = false) String postalCode,
        @RequestParam(required = false) String city
    ) {
        return service.geocode(street, postalCode, city)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
