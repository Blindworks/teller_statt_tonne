package de.tellerstatttonne.backend.pickuprun;

import de.tellerstatttonne.backend.auth.CurrentUser;
import de.tellerstatttonne.backend.distributionpoint.post.DistributionPost;
import de.tellerstatttonne.backend.distributionpoint.post.DistributionPostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pickups/{pickupId}/run")
@PreAuthorize("hasRole('RETTER')")
public class PickupRunController {

    public record AddItemRequest(Long foodCategoryId, String customLabel) {}
    public record CompleteRequest(Long distributionPointId, String notes) {}
    public record CompleteResponse(PickupRun run, DistributionPost post) {}

    private final PickupRunService service;
    private final DistributionPostService postService;

    public PickupRunController(PickupRunService service, DistributionPostService postService) {
        this.service = service;
        this.postService = postService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@PathVariable Long pickupId) {
        Long userId = CurrentUser.requireId();
        PickupRunService.StartResult result = service.startOrResume(pickupId, userId);
        return mapResult(result);
    }

    @GetMapping
    public ResponseEntity<PickupRun> current(@PathVariable Long pickupId) {
        Long userId = CurrentUser.requireId();
        return service.findForRetter(pickupId, userId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@PathVariable Long pickupId, @RequestBody AddItemRequest body) {
        Long userId = CurrentUser.requireId();
        PickupRunService.ItemResult result = service.addItem(pickupId, userId, body.foodCategoryId(), body.customLabel());
        return switch (result.code()) {
            case OK -> ResponseEntity.ok(result.item());
            case RUN_NOT_FOUND -> ResponseEntity.notFound().build();
            case RUN_ALREADY_FINISHED -> ResponseEntity.status(409).body("Abholung bereits abgeschlossen");
            case INVALID_INPUT -> ResponseEntity.badRequest().body("foodCategoryId oder customLabel erforderlich");
            default -> ResponseEntity.internalServerError().build();
        };
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long pickupId, @PathVariable Long itemId) {
        Long userId = CurrentUser.requireId();
        PickupRunService.Result result = service.removeItem(pickupId, userId, itemId);
        return switch (result) {
            case OK -> ResponseEntity.noContent().build();
            case RUN_NOT_FOUND, ITEM_NOT_FOUND -> ResponseEntity.notFound().build();
            case RUN_ALREADY_FINISHED -> ResponseEntity.status(409).build();
            default -> ResponseEntity.internalServerError().build();
        };
    }

    @PostMapping("/complete")
    public ResponseEntity<?> complete(@PathVariable Long pickupId, @RequestBody CompleteRequest body) {
        Long userId = CurrentUser.requireId();
        PickupRunService.StartResult result = service.complete(pickupId, userId, body.distributionPointId(), body.notes());
        if (result.code() != PickupRunService.Result.OK) return mapResult(result);
        DistributionPost post = postService.createForRun(result.run().id(), userId);
        return ResponseEntity.ok(new CompleteResponse(result.run(), post));
    }

    @PostMapping("/abort")
    public ResponseEntity<Void> abort(@PathVariable Long pickupId) {
        Long userId = CurrentUser.requireId();
        PickupRunService.Result result = service.abort(pickupId, userId);
        return switch (result) {
            case OK -> ResponseEntity.noContent().build();
            case RUN_NOT_FOUND -> ResponseEntity.notFound().build();
            case RUN_ALREADY_FINISHED -> ResponseEntity.status(409).build();
            default -> ResponseEntity.internalServerError().build();
        };
    }

    private ResponseEntity<?> mapResult(PickupRunService.StartResult result) {
        return switch (result.code()) {
            case OK -> ResponseEntity.ok(result.run());
            case PICKUP_NOT_FOUND, RUN_NOT_FOUND -> ResponseEntity.notFound().build();
            case NOT_ASSIGNED -> ResponseEntity.status(403).body("Du bist nicht für diese Abholung eingetragen");
            case RUN_ALREADY_FINISHED -> ResponseEntity.status(409).body("Abholung bereits abgeschlossen");
            case INVALID_INPUT -> ResponseEntity.badRequest().body("Ungültige Eingabe");
            default -> ResponseEntity.internalServerError().build();
        };
    }
}
