package de.tellerstatttonne.backend.introduction;

import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IntroductionSlotService {

    private final IntroductionSlotRepository slotRepository;
    private final IntroductionBookingRepository bookingRepository;
    private final UserRepository userRepository;

    public IntroductionSlotService(IntroductionSlotRepository slotRepository,
                                   IntroductionBookingRepository bookingRepository,
                                   UserRepository userRepository) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<IntroductionSlotDto> listAvailable(Long currentUserId) {
        List<IntroductionSlotEntity> slots =
            slotRepository.findByDateGreaterThanEqualOrderByDateAscStartTimeAsc(LocalDate.now());
        Optional<IntroductionBookingEntity> myBooking =
            bookingRepository.findByUserIdAndCancelledAtIsNull(currentUserId);
        return slots.stream()
            .map(s -> toDto(s, myBooking.map(IntroductionBookingEntity::getSlotId).orElse(null)))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IntroductionSlotDto> listAll() {
        return slotRepository.findAllByOrderByDateDescStartTimeDesc().stream()
            .map(s -> toDto(s, null))
            .toList();
    }

    public IntroductionSlotDto create(IntroductionSlotRequest req, Long creatorId) {
        validate(req);
        IntroductionSlotEntity slot = new IntroductionSlotEntity();
        applyRequest(slot, req);
        slot.setCreatedByUserId(creatorId);
        return toDto(slotRepository.save(slot), null);
    }

    public IntroductionSlotDto update(Long id, IntroductionSlotRequest req) {
        validate(req);
        IntroductionSlotEntity slot = slotRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("slot not found: " + id));
        applyRequest(slot, req);
        return toDto(slotRepository.save(slot), null);
    }

    public void delete(Long id) {
        if (!slotRepository.existsById(id)) {
            throw new EntityNotFoundException("slot not found: " + id);
        }
        slotRepository.deleteById(id);
    }

    public IntroductionSlotDto book(Long slotId, Long userId) {
        IntroductionSlotEntity slot = slotRepository.findById(slotId)
            .orElseThrow(() -> new EntityNotFoundException("slot not found: " + slotId));
        if (slot.getDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Termin liegt in der Vergangenheit");
        }
        bookingRepository.findByUserIdAndCancelledAtIsNull(userId).ifPresent(b -> {
            throw new IllegalStateException("Es existiert bereits eine aktive Buchung");
        });
        long booked = bookingRepository.countBySlotIdAndCancelledAtIsNull(slotId);
        if (booked >= slot.getCapacity()) {
            throw new IllegalStateException("Termin ist ausgebucht");
        }
        IntroductionBookingEntity booking = new IntroductionBookingEntity();
        booking.setSlotId(slotId);
        booking.setUserId(userId);
        bookingRepository.save(booking);
        return toDto(slot, slotId);
    }

    public void cancelBooking(Long bookingId, Long currentUserId, boolean isAdmin) {
        IntroductionBookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException("booking not found: " + bookingId));
        if (!isAdmin && !booking.getUserId().equals(currentUserId)) {
            throw new IllegalStateException("Nicht berechtigt, diese Buchung zu stornieren");
        }
        if (booking.getCancelledAt() != null) {
            return;
        }
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);
    }

    public void confirmAttendance(Long slotId, Long userId) {
        IntroductionBookingEntity booking = bookingRepository.findByUserIdAndCancelledAtIsNull(userId)
            .orElseThrow(() -> new EntityNotFoundException(
                "keine aktive Buchung fuer Nutzer: " + userId));
        if (!booking.getSlotId().equals(slotId)) {
            throw new IllegalStateException("Buchung gehoert nicht zum angegebenen Slot");
        }
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
        if (user.getIntroductionCompletedAt() == null) {
            user.setIntroductionCompletedAt(Instant.now());
            userRepository.save(user);
        }
    }

    @Transactional(readOnly = true)
    public Optional<IntroductionBookingEntity> findActiveBooking(Long userId) {
        return bookingRepository.findByUserIdAndCancelledAtIsNull(userId);
    }

    private void validate(IntroductionSlotRequest req) {
        if (req.date() == null) throw new IllegalArgumentException("date is required");
        if (req.startTime() == null) throw new IllegalArgumentException("startTime is required");
        if (req.endTime() == null) throw new IllegalArgumentException("endTime is required");
        if (!req.endTime().isAfter(req.startTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (req.capacity() != null && req.capacity() < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
    }

    private void applyRequest(IntroductionSlotEntity slot, IntroductionSlotRequest req) {
        slot.setDate(req.date());
        slot.setStartTime(req.startTime());
        slot.setEndTime(req.endTime());
        slot.setLocation(req.location());
        slot.setCapacity(req.capacity() == null ? 1 : req.capacity());
        slot.setNotes(req.notes());
    }

    private IntroductionSlotDto toDto(IntroductionSlotEntity s, Long myBookedSlotId) {
        long booked = bookingRepository.countBySlotIdAndCancelledAtIsNull(s.getId());
        return new IntroductionSlotDto(
            s.getId(), s.getDate(), s.getStartTime(), s.getEndTime(),
            s.getLocation(), s.getCapacity(), (int) booked, s.getNotes(),
            myBookedSlotId != null && myBookedSlotId.equals(s.getId())
        );
    }
}
