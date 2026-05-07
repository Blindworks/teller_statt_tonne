package de.tellerstatttonne.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired private NotificationService service;
    @Autowired private NotificationRepository repository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @Test
    void createPersistsOneRowPerRecipientAndExcludesActor() {
        Long userA = createUser().getId();
        Long userB = createUser().getId();
        Long actor = createUser().getId();

        service.create(List.of(userA, userB, actor),
            NotificationType.PICKUP_UNASSIGNED,
            "Title", "Body", null, null, actor);

        assertThat(service.list(userA, 50)).hasSize(1);
        assertThat(service.list(userB, 50)).hasSize(1);
        assertThat(service.list(actor, 50)).isEmpty();
    }

    @Test
    void markReadOnlyForOwnNotification() {
        Long owner = createUser().getId();
        Long stranger = createUser().getId();
        service.create(List.of(owner), NotificationType.PICKUP_CANCELLED,
            "T", "B", null, null, null);
        Long id = service.list(owner, 1).get(0).id();

        assertThat(service.markRead(stranger, id)).isFalse();
        assertThat(service.unreadCount(owner)).isEqualTo(1);

        assertThat(service.markRead(owner, id)).isTrue();
        assertThat(service.unreadCount(owner)).isZero();
    }

    @Test
    void markAllReadClearsUnread() {
        Long user = createUser().getId();
        service.create(List.of(user), NotificationType.PICKUP_COMPLETED, "A", "a", null, null, null);
        service.create(List.of(user), NotificationType.PICKUP_COMPLETED, "B", "b", null, null, null);
        assertThat(service.unreadCount(user)).isEqualTo(2);

        service.markAllRead(user);
        assertThat(service.unreadCount(user)).isZero();
    }

    @Test
    void emptyRecipientsIsNoop() {
        service.create(List.of(), NotificationType.PICKUP_UNASSIGNED, "x", "y", null, null, null);
        assertThat(repository.count()).isZero();
    }

    private UserEntity createUser() {
        UserEntity user = new UserEntity();
        user.setEmail("nu-" + System.nanoTime() + "@example.de");
        user.setPasswordHash("dummy");
        user.setRoles(Set.of(roleRepository.findByName("RETTER").orElseThrow()));
        user.setFirstName("First");
        user.setLastName("Last");
        user.setOnlineStatus(UserEntity.OnlineStatus.OFFLINE);
        user.setStatus(UserEntity.Status.ACTIVE);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }
}
