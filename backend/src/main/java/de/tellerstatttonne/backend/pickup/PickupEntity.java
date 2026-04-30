package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.partner.PartnerEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pickup")
public class PickupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerEntity partner;

    @Column(name = "pickup_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", length = 8, nullable = false)
    private String startTime;

    @Column(name = "end_time", length = 8, nullable = false)
    private String endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Pickup.Status status = Pickup.Status.SCHEDULED;

    @Column(nullable = false)
    private int capacity;

    @Column(length = 500)
    private String notes;

    @Column(name = "saved_kg", precision = 10, scale = 2)
    private BigDecimal savedKg;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "pickup_assignment",
        joinColumns = @JoinColumn(name = "pickup_id")
    )
    @OrderColumn(name = "slot_order")
    private List<AssignmentEmbeddable> assignments = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PartnerEntity getPartner() { return partner; }
    public void setPartner(PartnerEntity partner) { this.partner = partner; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Pickup.Status getStatus() { return status; }
    public void setStatus(Pickup.Status status) { this.status = status; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getSavedKg() { return savedKg; }
    public void setSavedKg(BigDecimal savedKg) { this.savedKg = savedKg; }
    public List<AssignmentEmbeddable> getAssignments() { return assignments; }
    public void setAssignments(List<AssignmentEmbeddable> assignments) { this.assignments = assignments; }

    @Embeddable
    public static class AssignmentEmbeddable {
        @Column(name = "member_id", nullable = false)
        private Long memberId;

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
    }
}
