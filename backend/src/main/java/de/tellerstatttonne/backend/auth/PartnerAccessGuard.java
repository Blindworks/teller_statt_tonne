package de.tellerstatttonne.backend.auth;

import de.tellerstatttonne.backend.partner.PartnerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Erlaubt Aktionen an einem Betrieb f&uuml;r Admin/Teamleiter global, f&uuml;r
 * Koordinatoren nur am eigenen Betrieb (Membership in {@code partner_user}).
 */
@Component("partnerAccess")
public class PartnerAccessGuard {

    private final PartnerRepository partnerRepository;

    public PartnerAccessGuard(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    public boolean canManagePartner(Long partnerId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (hasAuthority(authentication, "ROLE_ADMINISTRATOR")
            || hasAuthority(authentication, "ROLE_TEAMLEITER")) {
            return true;
        }
        if (!hasAuthority(authentication, "ROLE_KOORDINATOR")) {
            return false;
        }
        if (partnerId == null) {
            return false;
        }
        Long userId;
        try {
            userId = Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException ex) {
            return false;
        }
        return partnerRepository.findIdsByMemberId(userId).contains(partnerId);
    }

    private static boolean hasAuthority(Authentication auth, String authority) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
