package de.tellerstatttonne.backend.partner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    private final RestClient restClient;

    @Autowired
    public GeocodingService(
        @Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
        @Value("${geocoding.nominatim.user-agent:teller-statt-tonne/1.0 (kontakt@teller-statt-tonne.de)}") String userAgent
    ) {
        this(RestClient.builder(), baseUrl, userAgent);
    }

    GeocodingService(RestClient.Builder builder, String baseUrl, String userAgent) {
        this.restClient = builder
            .baseUrl(baseUrl)
            .defaultHeader("User-Agent", userAgent)
            .build();
    }

    public Optional<Coordinates> geocode(String street, String postalCode, String city) {
        if (isBlank(street) && isBlank(postalCode) && isBlank(city)) {
            return Optional.empty();
        }
        try {
            List<NominatimResult> results = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("street", nullToEmpty(street))
                    .queryParam("postalcode", nullToEmpty(postalCode))
                    .queryParam("city", nullToEmpty(city))
                    .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<NominatimResult>>() {});

            if (results == null || results.isEmpty()) {
                log.info("Geocoding leer fuer Adresse '{} {} {}'", street, postalCode, city);
                return Optional.empty();
            }
            NominatimResult r = results.get(0);
            try {
                return Optional.of(new Coordinates(Double.parseDouble(r.lat()), Double.parseDouble(r.lon())));
            } catch (NumberFormatException ex) {
                log.warn("Geocoding lieferte ungueltige Koordinaten: {} / {}", r.lat(), r.lon());
                return Optional.empty();
            }
        } catch (Exception ex) {
            log.warn("Geocoding fehlgeschlagen fuer '{} {} {}': {}", street, postalCode, city, ex.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public Optional<ReverseResult> reverseGeocode(double lat, double lon) {
        try {
            NominatimReverseResult r = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/reverse")
                    .queryParam("format", "jsonv2")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .build())
                .retrieve()
                .body(NominatimReverseResult.class);

            if (r == null || r.address() == null) {
                log.info("Reverse-Geocoding leer fuer {}/{}", lat, lon);
                return Optional.empty();
            }
            NominatimAddress a = r.address();
            String street = combineStreet(a.road(), a.houseNumber());
            String city = firstNonBlank(a.city(), a.town(), a.village(), a.municipality());
            return Optional.of(new ReverseResult(street, a.postcode(), city, lat, lon));
        } catch (Exception ex) {
            log.warn("Reverse-Geocoding fehlgeschlagen fuer {}/{}: {}", lat, lon, ex.getMessage());
            return Optional.empty();
        }
    }

    private static String combineStreet(String road, String houseNumber) {
        if (isBlank(road)) return null;
        if (isBlank(houseNumber)) return road;
        return road + " " + houseNumber;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return null;
    }

    public record Coordinates(double lat, double lon) {}

    public record ReverseResult(String street, String postalCode, String city, double lat, double lon) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimResult(String lat, String lon) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimReverseResult(NominatimAddress address) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimAddress(
        String road,
        @com.fasterxml.jackson.annotation.JsonProperty("house_number") String houseNumber,
        String postcode,
        String city,
        String town,
        String village,
        String municipality
    ) {}
}
