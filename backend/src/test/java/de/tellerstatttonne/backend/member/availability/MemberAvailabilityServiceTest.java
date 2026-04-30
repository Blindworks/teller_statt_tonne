package de.tellerstatttonne.backend.member.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.member.Member;
import de.tellerstatttonne.backend.member.MemberEntity;
import de.tellerstatttonne.backend.member.MemberRepository;
import de.tellerstatttonne.backend.member.MemberRole;
import de.tellerstatttonne.backend.partner.Partner;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberAvailabilityServiceTest {

    @Autowired
    private MemberAvailabilityService service;

    @Autowired
    private MemberAvailabilityRepository repository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void cleanSlate() {
        repository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void replaceAllRoundTrip() {
        String memberId = createMember("Anna", "Schmidt", Member.Status.ACTIVE);

        List<MemberAvailability> saved = service.replaceAll(memberId, List.of(
            new MemberAvailability(null, memberId, Partner.Weekday.MONDAY, "14:00", "18:00"),
            new MemberAvailability(null, memberId, Partner.Weekday.WEDNESDAY, "09:00", "12:00")
        ));

        assertThat(saved).hasSize(2);
        assertThat(service.findByMemberId(memberId)).hasSize(2);

        // Replacing again wipes previous entries.
        service.replaceAll(memberId, List.of(
            new MemberAvailability(null, memberId, Partner.Weekday.FRIDAY, "10:00", "11:00")
        ));
        List<MemberAvailability> after = service.findByMemberId(memberId);
        assertThat(after).hasSize(1);
        assertThat(after.get(0).weekday()).isEqualTo(Partner.Weekday.FRIDAY);
    }

    @Test
    void replaceAllRejectsInvalidTimes() {
        String memberId = createMember("X", "Y", Member.Status.ACTIVE);

        assertThatThrownBy(() -> service.replaceAll(memberId, List.of(
            new MemberAvailability(null, memberId, Partner.Weekday.MONDAY, "18:00", "10:00")
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.replaceAll(memberId, List.of(
            new MemberAvailability(null, memberId, Partner.Weekday.MONDAY, "abc", "10:00")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaceAllDeduplicatesIdenticalEntries() {
        String memberId = createMember("Dupe", "Test", Member.Status.ACTIVE);

        List<MemberAvailability> saved = service.replaceAll(memberId, List.of(
            new MemberAvailability(null, memberId, Partner.Weekday.MONDAY, "14:00", "18:00"),
            new MemberAvailability(null, memberId, Partner.Weekday.MONDAY, "14:00", "18:00")
        ));

        assertThat(saved).hasSize(1);
    }

    @Test
    void countMatchesOnlyFullCoverageAndActiveMembers() {
        String active1 = createMember("Active", "One", Member.Status.ACTIVE);
        String active2 = createMember("Active", "Two", Member.Status.ACTIVE);
        String inactive = createMember("Inactive", "User", Member.Status.INACTIVE);

        // active1: covers 14-17 (full coverage)
        service.replaceAll(active1, List.of(
            new MemberAvailability(null, active1, Partner.Weekday.MONDAY, "14:00", "18:00")
        ));
        // active2: only partial overlap (15-17 inside slot 14-17 → starts later)
        service.replaceAll(active2, List.of(
            new MemberAvailability(null, active2, Partner.Weekday.MONDAY, "15:00", "17:00")
        ));
        // inactive: would cover but is not ACTIVE
        service.replaceAll(inactive, List.of(
            new MemberAvailability(null, inactive, Partner.Weekday.MONDAY, "13:00", "20:00")
        ));

        // Slot Monday 14:00-17:00: active1 covers fully, active2 starts too late, inactive excluded.
        int count = service.countAvailableForSlot(Partner.Weekday.MONDAY, "14:00", "17:00");
        assertThat(count).isEqualTo(1);

        // Slot Monday 15:00-17:00: active1 covers, active2 covers exactly → 2.
        count = service.countAvailableForSlot(Partner.Weekday.MONDAY, "15:00", "17:00");
        assertThat(count).isEqualTo(2);

        // Slot on a different weekday → 0.
        count = service.countAvailableForSlot(Partner.Weekday.TUESDAY, "15:00", "17:00");
        assertThat(count).isZero();
    }

    @Test
    void countDistinctMembersWithMultipleAvailabilitySlots() {
        String memberId = createMember("Multi", "Slot", Member.Status.ACTIVE);
        service.replaceAll(memberId, List.of(
            new MemberAvailability(null, memberId, Partner.Weekday.MONDAY, "08:00", "12:00"),
            new MemberAvailability(null, memberId, Partner.Weekday.MONDAY, "14:00", "18:00")
        ));

        // Slot 09:00-11:00 falls into the morning window only — still counts member once.
        int count = service.countAvailableForSlot(Partner.Weekday.MONDAY, "09:00", "11:00");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void replaceAllRejectsUnknownMember() {
        assertThatThrownBy(() -> service.replaceAll("does-not-exist", List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private String createMember(String firstName, String lastName, Member.Status status) {
        MemberEntity m = new MemberEntity();
        m.setId(UUID.randomUUID().toString());
        m.setFirstName(firstName);
        m.setLastName(lastName);
        m.setRole(MemberRole.FOODSAVER);
        m.setStatus(status);
        m.setOnlineStatus(Member.OnlineStatus.OFFLINE);
        memberRepository.save(m);
        return m.getId();
    }
}
