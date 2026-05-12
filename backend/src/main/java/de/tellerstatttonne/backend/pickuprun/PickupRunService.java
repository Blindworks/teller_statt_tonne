package de.tellerstatttonne.backend.pickuprun;

import de.tellerstatttonne.backend.pickup.PickupEntity;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PickupRunService {

    public enum Result {
        OK,
        PICKUP_NOT_FOUND,
        NOT_ASSIGNED,
        RUN_NOT_FOUND,
        RUN_ALREADY_FINISHED,
        ITEM_NOT_FOUND,
        INVALID_INPUT
    }

    public record StartResult(Result code, PickupRun run) {}
    public record ItemResult(Result code, PickupRun.PickupRunItem item) {}

    private final PickupRunRepository runRepository;
    private final PickupRunItemRepository itemRepository;
    private final PickupRepository pickupRepository;

    public PickupRunService(PickupRunRepository runRepository,
                            PickupRunItemRepository itemRepository,
                            PickupRepository pickupRepository) {
        this.runRepository = runRepository;
        this.itemRepository = itemRepository;
        this.pickupRepository = pickupRepository;
    }

    public StartResult startOrResume(Long pickupId, Long retterId) {
        Optional<PickupEntity> pickupOpt = pickupRepository.findById(pickupId);
        if (pickupOpt.isEmpty()) return new StartResult(Result.PICKUP_NOT_FOUND, null);
        PickupEntity pickup = pickupOpt.get();
        boolean assigned = pickup.getAssignments().stream()
            .anyMatch(a -> retterId.equals(a.getUserId()));
        if (!assigned) return new StartResult(Result.NOT_ASSIGNED, null);

        PickupRunEntity entity = runRepository
            .findByPickupIdAndRetterId(pickupId, retterId)
            .orElseGet(() -> {
                PickupRunEntity fresh = new PickupRunEntity();
                fresh.setPickupId(pickupId);
                fresh.setRetterId(retterId);
                fresh.setStartedAt(Instant.now());
                fresh.setStatus(PickupRunStatus.IN_PROGRESS);
                return runRepository.save(fresh);
            });
        return new StartResult(Result.OK, toDto(entity));
    }

    @Transactional(readOnly = true)
    public Optional<PickupRun> findForRetter(Long pickupId, Long retterId) {
        return runRepository.findByPickupIdAndRetterId(pickupId, retterId).map(PickupRunService::toDto);
    }

    public ItemResult addItem(Long pickupId, Long retterId, Long foodCategoryId, String customLabel) {
        Optional<PickupRunEntity> runOpt = runRepository.findByPickupIdAndRetterId(pickupId, retterId);
        if (runOpt.isEmpty()) return new ItemResult(Result.RUN_NOT_FOUND, null);
        PickupRunEntity run = runOpt.get();
        if (run.getStatus() != PickupRunStatus.IN_PROGRESS) {
            return new ItemResult(Result.RUN_ALREADY_FINISHED, null);
        }
        boolean hasCategory = foodCategoryId != null;
        boolean hasLabel = customLabel != null && !customLabel.isBlank();
        if (hasCategory == hasLabel) {
            return new ItemResult(Result.INVALID_INPUT, null);
        }
        PickupRunItemEntity item = new PickupRunItemEntity();
        item.setPickupRun(run);
        item.setFoodCategoryId(foodCategoryId);
        item.setCustomLabel(hasLabel ? customLabel.trim() : null);
        run.getItems().add(item);
        runRepository.save(run);
        return new ItemResult(Result.OK, toItemDto(item));
    }

    public Result removeItem(Long pickupId, Long retterId, Long itemId) {
        Optional<PickupRunEntity> runOpt = runRepository.findByPickupIdAndRetterId(pickupId, retterId);
        if (runOpt.isEmpty()) return Result.RUN_NOT_FOUND;
        PickupRunEntity run = runOpt.get();
        if (run.getStatus() != PickupRunStatus.IN_PROGRESS) return Result.RUN_ALREADY_FINISHED;
        boolean removed = run.getItems().removeIf(i -> itemId.equals(i.getId()));
        if (!removed) return Result.ITEM_NOT_FOUND;
        runRepository.save(run);
        return Result.OK;
    }

    public StartResult complete(Long pickupId, Long retterId, Long distributionPointId, String notes) {
        Optional<PickupRunEntity> runOpt = runRepository.findByPickupIdAndRetterId(pickupId, retterId);
        if (runOpt.isEmpty()) return new StartResult(Result.RUN_NOT_FOUND, null);
        PickupRunEntity run = runOpt.get();
        if (run.getStatus() != PickupRunStatus.IN_PROGRESS) {
            return new StartResult(Result.RUN_ALREADY_FINISHED, null);
        }
        if (distributionPointId == null) return new StartResult(Result.INVALID_INPUT, null);
        run.setDistributionPointId(distributionPointId);
        run.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        run.setCompletedAt(Instant.now());
        run.setStatus(PickupRunStatus.COMPLETED);
        return new StartResult(Result.OK, toDto(runRepository.save(run)));
    }

    public Result abort(Long pickupId, Long retterId) {
        Optional<PickupRunEntity> runOpt = runRepository.findByPickupIdAndRetterId(pickupId, retterId);
        if (runOpt.isEmpty()) return Result.RUN_NOT_FOUND;
        PickupRunEntity run = runOpt.get();
        if (run.getStatus() != PickupRunStatus.IN_PROGRESS) return Result.RUN_ALREADY_FINISHED;
        run.setStatus(PickupRunStatus.ABORTED);
        run.setCompletedAt(Instant.now());
        runRepository.save(run);
        return Result.OK;
    }

    static PickupRun toDto(PickupRunEntity e) {
        List<PickupRun.PickupRunItem> items = e.getItems().stream()
            .map(PickupRunService::toItemDto).toList();
        return new PickupRun(
            e.getId(), e.getPickupId(), e.getRetterId(),
            e.getStartedAt(), e.getCompletedAt(), e.getStatus(),
            e.getDistributionPointId(), e.getNotes(), items
        );
    }

    private static PickupRun.PickupRunItem toItemDto(PickupRunItemEntity e) {
        return new PickupRun.PickupRunItem(e.getId(), e.getFoodCategoryId(), e.getCustomLabel(), e.getTakenAt());
    }
}
