package de.tellerstatttonne.backend.stats;

import java.math.BigDecimal;
import java.util.List;

public final class StatsDtos {

    private StatsDtos() {}

    public record Overview(
        BigDecimal totalSavedKg,
        long completedPickupCount,
        List<PartnerEntry> topPartners,
        List<MemberEntry> topMembers
    ) {}

    public record PartnerEntry(Long partnerId, String partnerName, BigDecimal savedKg, long pickupCount) {}

    public record MemberEntry(Long memberId, String memberName, BigDecimal savedKg, long pickupCount) {}
}
