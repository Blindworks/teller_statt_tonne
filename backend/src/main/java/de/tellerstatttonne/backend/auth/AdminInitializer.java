package de.tellerstatttonne.backend.auth;

import de.tellerstatttonne.backend.user.Role;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        @Value("${app.admin.email:admin@local}") String adminEmail,
        @Value("${app.admin.password:admin1234}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
            existing -> {
                if (existing.getRole() != Role.ADMINISTRATOR) {
                    existing.setRole(Role.ADMINISTRATOR);
                    userRepository.save(existing);
                    log.info("Administrator-Rolle fuer bestehenden Account '{}' gesetzt.", adminEmail);
                }
            },
            () -> {
                UserEntity admin = new UserEntity();
                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ADMINISTRATOR);
                admin.setFirstName("System");
                admin.setLastName("Administrator");
                Instant now = Instant.now();
                admin.setCreatedAt(now);
                admin.setUpdatedAt(now);
                userRepository.save(admin);
                log.warn(
                    "Administrator-Account angelegt: '{}'. Bitte Passwort sofort aendern (app.admin.password).",
                    adminEmail
                );
            }
        );
    }
}
