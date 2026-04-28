package de.tellerstatttonne.backend.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeocodingServiceTest {

    private static final String BASE_URL = "https://nominatim.test";

    @Test
    void returnsEmptyForBlankAddress() {
        GeocodingService svc = new GeocodingService(RestClient.builder(), BASE_URL, "test-agent");
        assertThat(svc.geocode(null, null, null)).isEmpty();
        assertThat(svc.geocode("", " ", "")).isEmpty();
    }

    @Test
    void parsesSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.once(), requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/search")))
            .andRespond(withSuccess("[{\"lat\":\"52.5200\",\"lon\":\"13.4050\",\"display_name\":\"Berlin\"}]",
                MediaType.APPLICATION_JSON));

        GeocodingService svc = new GeocodingService(builder, BASE_URL, "test-agent");
        Optional<GeocodingService.Coordinates> result = svc.geocode("Hauptstr. 1", "10115", "Berlin");

        assertThat(result).isPresent();
        assertThat(result.get().lat()).isEqualTo(52.5200);
        assertThat(result.get().lon()).isEqualTo(13.4050);
        server.verify();
    }

    @Test
    void returnsEmptyForEmptyResultArray() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.once(), requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/search")))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        GeocodingService svc = new GeocodingService(builder, BASE_URL, "test-agent");
        assertThat(svc.geocode("Nirgendwo 1", "00000", "Atlantis")).isEmpty();
        server.verify();
    }

    @Test
    void returnsEmptyOnHttpError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.once(), requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/search")))
            .andRespond(withServerError());

        GeocodingService svc = new GeocodingService(builder, BASE_URL, "test-agent");
        assertThat(svc.geocode("Hauptstr. 1", "10115", "Berlin")).isEmpty();
        server.verify();
    }
}
