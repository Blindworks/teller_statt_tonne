package de.tellerstatttonne.backend;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String[] PUBLIC_SUBDIRS = {"logos", "photos"};

    private final String uploadsDir;

    public WebMvcConfig(@Value("${app.uploads.dir:./uploads}") String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Path.of(uploadsDir).toAbsolutePath().normalize();
        for (String subdir : PUBLIC_SUBDIRS) {
            String location = base.resolve(subdir).toUri().toString();
            registry.addResourceHandler("/uploads/" + subdir + "/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
        }
    }
}
