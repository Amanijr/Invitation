-- Phase X: event-centric invitations, template versioning, SINGLE/DOUBLE admission.
-- The running app uses spring.jpa.hibernate.ddl-auto=update, which adds these columns
-- automatically. Use this script only on databases that do not run Hibernate schema update.

ALTER TABLE events
    ADD COLUMN currentTemplateId CHAR(36) NULL,
    ADD COLUMN currentTemplateVersion INT NULL;

ALTER TABLE templates
    ADD COLUMN version INT NOT NULL DEFAULT 1;

ALTER TABLE invitations
    ADD COLUMN templateVersion INT NOT NULL DEFAULT 1,
    ADD COLUMN admissionType VARCHAR(16) NOT NULL DEFAULT 'SINGLE',
    ADD COLUMN admissionLimit INT NOT NULL DEFAULT 1,
    ADD COLUMN usedAdmissions INT NOT NULL DEFAULT 0,
    ADD COLUMN revoked TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN revokedAt DATETIME NULL;

UPDATE invitations
SET usedAdmissions = admissionLimit
WHERE used = 1 AND usedAdmissions = 0;
