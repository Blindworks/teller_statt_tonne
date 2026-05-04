package de.tellerstatttonne.backend.push;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    List<PushSubscriptionEntity> findAllByUserId(Long userId);

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    @Transactional
    long deleteByEndpoint(String endpoint);

    @Transactional
    long deleteByUserIdAndEndpoint(Long userId, String endpoint);
}
