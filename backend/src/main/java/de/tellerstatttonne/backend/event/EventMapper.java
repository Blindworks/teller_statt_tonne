package de.tellerstatttonne.backend.event;

import de.tellerstatttonne.backend.partner.PartnerEntity;

final class EventMapper {

    private EventMapper() {}

    static Event toDto(EventEntity e) {
        PartnerEntity.ContactEmbeddable c = e.getContact();
        Event.Contact contact = c == null
            ? new Event.Contact(null, null, null)
            : new Event.Contact(c.getName(), c.getEmail(), c.getPhone());
        return new Event(
            e.getId(),
            e.getName(),
            e.getDescription(),
            e.getStartDate(),
            e.getEndDate(),
            e.getStreet(),
            e.getPostalCode(),
            e.getCity(),
            e.getLatitude(),
            e.getLongitude(),
            e.getLogoUrl(),
            contact
        );
    }

    static void applyScalarFields(EventEntity target, Event src) {
        target.setName(src.name());
        target.setDescription(src.description());
        target.setStartDate(src.startDate());
        target.setEndDate(src.endDate());
        target.setStreet(src.street());
        target.setPostalCode(src.postalCode());
        target.setCity(src.city());
        target.setLatitude(src.latitude());
        target.setLongitude(src.longitude());

        PartnerEntity.ContactEmbeddable c = target.getContact();
        if (c == null) {
            c = new PartnerEntity.ContactEmbeddable();
            target.setContact(c);
        }
        if (src.contact() != null) {
            c.setName(src.contact().name());
            c.setEmail(src.contact().email());
            c.setPhone(src.contact().phone());
        } else {
            c.setName(null);
            c.setEmail(null);
            c.setPhone(null);
        }
    }
}
