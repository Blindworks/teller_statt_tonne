package de.tellerstatttonne.backend.distributionpoint.post;

import de.tellerstatttonne.backend.pickup.PickupEntity;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.pickuprun.PickupRunEntity;
import de.tellerstatttonne.backend.pickuprun.PickupRunRepository;
import de.tellerstatttonne.backend.storage.ImageStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class DistributionPostService {

    private final DistributionPostRepository repository;
    private final PickupRunRepository pickupRunRepository;
    private final PickupRepository pickupRepository;
    private final ImageStorageService imageStorageService;

    public DistributionPostService(DistributionPostRepository repository,
                                   PickupRunRepository pickupRunRepository,
                                   PickupRepository pickupRepository,
                                   ImageStorageService imageStorageService) {
        this.repository = repository;
        this.pickupRunRepository = pickupRunRepository;
        this.pickupRepository = pickupRepository;
        this.imageStorageService = imageStorageService;
    }

    public DistributionPost createForRun(Long pickupRunId, Long postedById) {
        Optional<DistributionPostEntity> existing = repository.findByPickupRunId(pickupRunId);
        if (existing.isPresent()) return toDto(existing.get());

        PickupRunEntity run = pickupRunRepository.findById(pickupRunId)
            .orElseThrow(() -> new IllegalArgumentException("PickupRun not found"));
        if (run.getDistributionPointId() == null) {
            throw new IllegalArgumentException("Run has no distribution point");
        }
        PickupEntity pickup = pickupRepository.findById(run.getPickupId()).orElse(null);

        DistributionPostEntity entity = new DistributionPostEntity();
        entity.setDistributionPointId(run.getDistributionPointId());
        entity.setPickupRunId(run.getId());
        entity.setPartnerId(pickup != null && pickup.getPartner() != null ? pickup.getPartner().getId() : null);
        entity.setPostedById(postedById);
        entity.setStatus(DistributionPostStatus.FRESH);
        entity.setNotes(run.getNotes());
        return toDto(repository.save(entity));
    }

    public DistributionPost addPhoto(Long postId, MultipartFile file, Long uploadedById) {
        DistributionPostEntity post = repository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        String url = imageStorageService.store("distribution-posts", postId.toString(), file, null);

        DistributionPostPhotoEntity photo = new DistributionPostPhotoEntity();
        photo.setPost(post);
        photo.setImageUrl(url);
        photo.setUploadedById(uploadedById);
        photo.setUploadedAt(Instant.now());
        post.getPhotos().add(photo);
        return toDto(repository.save(post));
    }

    @Transactional(readOnly = true)
    public Optional<DistributionPost> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<DistributionPost> findByDistributionPoint(Long distributionPointId, DistributionPostStatus status) {
        List<DistributionPostEntity> rows = status == null
            ? repository.findByDistributionPointIdOrderByCreatedAtDesc(distributionPointId)
            : repository.findByDistributionPointIdAndStatusOrderByCreatedAtDesc(distributionPointId, status);
        return rows.stream().map(this::toDto).toList();
    }

    private DistributionPost toDto(DistributionPostEntity e) {
        List<DistributionPost.Photo> photos = e.getPhotos().stream()
            .map(p -> new DistributionPost.Photo(p.getId(), p.getImageUrl(), p.getUploadedById(), p.getUploadedAt()))
            .toList();
        List<DistributionPost.Item> items = pickupRunRepository.findById(e.getPickupRunId())
            .map(run -> run.getItems().stream()
                .map(i -> new DistributionPost.Item(i.getId(), i.getFoodCategoryId(), i.getCustomLabel(), i.getTakenAt()))
                .toList())
            .orElseGet(List::of);
        return new DistributionPost(
            e.getId(), e.getDistributionPointId(), e.getPickupRunId(), e.getPartnerId(),
            e.getPostedById(), e.getCreatedAt(), e.getUpdatedAt(),
            e.getStatus(), e.getNotes(), photos, items
        );
    }
}
