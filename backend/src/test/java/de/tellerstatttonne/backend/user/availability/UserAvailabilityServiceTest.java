package de.tellerstatttonne.backend.user.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.user.Role;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserAvailabilityServiceTest {

    @Autowired
    private UserAvailabilityService service;

    @Autowired
    private UserAvailabilityRepository repository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanSlate() {
        repository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void replaceAllRoundTrip() {
        Long userId = createUser("Anna", "Schmidt", UserEntity.Status.ACTIVE);

        List<UserAvailability> saved = service.replaceAll(userId, List.of(
            new UserAvailability(null, userId, Partner.Weekday.MONDAY, "14:00", "18:00"),
            new UserAvailability(null, userId, Partner.Weekday.WEDNESDAY, "09:00", "12:00")
        ));

        assertThat(saved).hasSize(2);
        assertThat(service.findByUserId(userId)).hasSize(2);

        service.replaceAll(userId, List.of(
            new UserAvailability(null, userId, Partner.Weekday.FRIDAY, "10:00", "11:00")
        ));
        List<UserAvailability> after = service.findByUserId(userId);
        assertThat(after).hasSize(1);
        assertThat(after.get(0).weekday()).isEqualTo(Partner.Weekday.FRIDAY);
    }

    @Test
    void replaceAllRejectsInvalidTimes() {
        Long userId = createUser("X", "Y", UserEntity.Status.ACTIVE);

        assertThatThrownBy(() -> service.replaceAll(userId, List.of(
            new UserAvailability(null, userId, Partner.Weekday.MONDAY, "18:00", "10:00")
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.replaceAll(userId, List.of(
            new UserAvailability(null, userId, Partner.Weekday.MONDAY, "abc", "10:00")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaceAllDeduplicatesIdenticalEntries() {
        Long userId = createUser("Dupe", "Test", UserEntity.Status.ACTIVE);

        List<UserAvailability> saved = service.replaceAll(userId, List.of(
            new UserAvailability(null, userId, Partner.Weekday.MONDAY, "14:00", "18:00"),
            new UserAvailability(null, userId, Partner.Weekday.MONDAY, "14:00", "18:00")
        ));

        assertThat(saved).hasSize(1);
    }

    @Test
    void countMatchesOnlyFullCoverageAndActiveMembers() {
        Long active1 = createUser("Active", "One", UserEntity.Status.ACTIVE);
        Long active2 = createUser("Active", "Two", UserEntity.Status.ACTIVE);
        Long inactive = createUser("Inactive", "User", UserEntity.Status.INACTIVE);

        service.replaceAll(active1, List.of(
            new UserAvailability(null, active1, Partner.Weekday.MONDAY, "14:00", "18:00")
        ));
        service.replaceAll(active2, List.of(
            new UserAvailability(null, active2, Partner.Weekday.MONDAY, "15:00", "17:00")
        ));
        service.replaceAll(inactive, List.of(
            new UserAvailability(null, inactive, Partner.Weekday.MONDAY, "13:00", "20:00")
        ));

        int count = service.countAvailableForSlot(Partner.Weekday.MONDAY, "14:00", "17:00");
        assertThat(count).isEqualTo(1);

        count = service.countAvailableForSlot(Partner.Weekday.MONDAY, "15:00", "17:00");
        assertThat(count).isEqualTo(2);

        count = service.countAvailableForSlot(Partner.Weekday.TUESDAY, "15:00", "17:00");
        assertThat(count).isZero();
    }

    @Test
    void countDistinctMembersWithMultipleAvailabilitySlots() {
        Long userId = createUser("Multi", "Slot", UserEntity.Status.ACTIVE);
        service.replaceAll(userId, List.of(
            new UserAvailability(null, userId, Partner.Weekday.MONDAY, "08:00", "12:00"),
            new UserAvailability(null, userId, Partner.Weekday.MONDAY, "14:00", "18:00")
        ));

        int count = service.countAvailableForSlot(Partner.Weekday.MONDAY, "09:00", "11:00");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void replaceAllRejectsUnknownUser() {
        assertThatThrownBy(() -> service.replaceAll(-1L, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private Long createUser(String firstName, String lastName, UserEntity.Status status) {
        UserEntity u = new UserEntity();
        u.setEmail(UUID.randomUUID() + "@example.com");
        u.setPasswordHash("dummy");
        u.setRole(Role.RETTER);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setStatus(status);
        u.setOnlineStatus(UserEntity.OnlineStatus.OFFLINE);
        Instant now = Instant.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        userRepository.save(u);
        return u.getId();
    }
}
