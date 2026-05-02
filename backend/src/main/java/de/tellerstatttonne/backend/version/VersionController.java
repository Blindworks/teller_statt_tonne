package de.tellerstatttonne.backend.version;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    private final ObjectProvider<BuildProperties> buildProperties;

    public VersionController(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping
    public Map<String, Object> version() {
        Map<String, Object> result = new LinkedHashMap<>();
        BuildProperties props = buildProperties.getIfAvailable();
        if (props != null) {
            result.put("name", props.getName());
            result.put("version", props.getVersion());
            result.put("time", props.getTime() != null ? props.getTime().toString() : null);
        } else {
            result.put("name", "backend");
            result.put("version", "unknown");
            result.put("time", Instant.now().toString());
        }
        return result;
    }
}
