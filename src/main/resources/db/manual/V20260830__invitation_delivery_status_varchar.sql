-- invitations.delivery_status was a MySQL ENUM missing SENT / PROCESSING / DELIVERED.
-- SMS success writes SENT, which truncated and returned HTTP 500 after the gateway already accepted the message.

ALTER TABLE invitations
    MODIFY COLUMN delivery_status VARCHAR(32) NOT NULL;
