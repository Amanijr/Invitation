-- Do not run until duplicates are cleaned. Hibernate ddl-auto=update will not
-- add these unless you also put UniqueConstraint on the entity (intentionally omitted
-- so a dirty live database cannot block app startup).
--
-- SELECT eventId, email, COUNT(*) FROM guests GROUP BY eventId, email HAVING COUNT(*) > 1 AND email IS NOT NULL;
-- SELECT eventId, phone, COUNT(*) FROM guests GROUP BY eventId, phone HAVING COUNT(*) > 1 AND phone IS NOT NULL;
-- SELECT eventId, guestId, COUNT(*) FROM invitations GROUP BY eventId, guestId HAVING COUNT(*) > 1;

ALTER TABLE guests
    ADD UNIQUE INDEX uk_guest_event_email (eventId, email),
    ADD UNIQUE INDEX uk_guest_event_phone (eventId, phone);

ALTER TABLE invitations
    ADD UNIQUE INDEX uk_invitation_event_guest (eventId, guestId);
