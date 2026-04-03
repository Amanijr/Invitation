# EventType Feature - Database Migration Guide

## Overview
This guide provides the safe migration strategy for adding the `event_type` (NOT NULL) column to the `events` table.

## Migration Strategy

### For Development Environment (Using ddl-auto=create)

Since `application.properties` has `spring.jpa.hibernate.ddl-auto=create`, Hibernate will automatically:
1. Drop existing tables when the application starts
2. Recreate them with the new `event_type` column
3. Fall back to the default value if needed

**No manual migration required** for development environments during restart.

### For Production Environment (Recommended Safe Migration)

Since the feature requires `eventType` to be NOT NULL, follow these steps:

#### Step 1: Deploy with Temporary DDL Strategy
Update `application.properties` temporarily:
```properties
spring.jpa.hibernate.ddl-auto=validate
```

#### Step 2: Run Migration SQL

Execute the following SQL to add the column safely with a default value:

```sql
-- Add the new column with WEDDING as default for existing records
ALTER TABLE events ADD COLUMN event_type VARCHAR(255) DEFAULT 'WEDDING' NOT NULL;

-- Verify the migration
SELECT COUNT(*) as total_events, COUNT(DISTINCT event_type) as event_types FROM events;
```

#### Step 3: Update Specific Records (Optional)

If you want to assign proper types to existing events, update them:
```sql
-- Example: Update events based on naming patterns
UPDATE events SET event_type = 'CONFERENCE' WHERE event_name LIKE '%conference%';
UPDATE events SET event_type = 'CORPORATE' WHERE event_name LIKE '%corporate%';
UPDATE events SET event_type = 'BIRTHDAY' WHERE event_name LIKE '%birthday%';
```

#### Step 4: Verify Migration
```sql
-- Check all event types are populated
SELECT event_type, COUNT(*) as count FROM events GROUP BY event_type;

-- Ensure no NULL values
SELECT COUNT(*) as null_count FROM events WHERE event_type IS NULL;
```

#### Step 5: Keep DDL Strategy as Validate
Update `application.properties` permanently:
```properties
spring.jpa.hibernate.ddl-auto=validate
```

This prevents accidental schema changes on future restarts.

## Backward Compatibility

The application now handles backwards compatibility:

1. **Validation Requirements**:
   - New events MUST have a valid eventType (enforced by @NotNull annotation)
   - Invalid enum values are rejected with clear error messages

2. **Error Handling**:
   - Missing eventType returns HTTP 400 with validation error:
     ```json
     {
       "status": 400,
       "error": "Validation Failed",
       "errors": {
         "eventType": "Event type cannot be null"
       }
     }
     ```
   - Invalid enum value returns HTTP 400 with deserialization error

3. **Service Layer Protection**:
   - EventServiceImpl.updateEvent() safely handles null eventType
   - mapToResponseDto() includes null-safe checks

## Testing the Feature

### Test 1: Valid Request
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "Tech Conference 2026",
    "eventDescription": "Annual tech conference",
    "venue": "Convention Center",
    "eventDate": "2026-06-15T10:00:00",
    "eventType": "CONFERENCE"
  }' \
  -G --data-urlencode "createdBy=12345678-1234-1234-1234-123456789012"
```

### Test 2: Missing eventType (Should Fail)
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "Tech Conference 2026",
    "eventDescription": "Annual tech conference",
    "venue": "Convention Center",
    "eventDate": "2026-06-15T10:00:00"
  }' \
  -G --data-urlencode "createdBy=12345678-1234-1234-1234-123456789012"
```

Expected Response (400 Bad Request):
```json
{
  "timestamp": "2026-03-31T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "eventType": "Event type cannot be null"
  }
}
```

### Test 3: Invalid eventType (Should Fail)
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "Tech Conference 2026",
    "eventDescription": "Annual tech conference",
    "venue": "Convention Center",
    "eventDate": "2026-06-15T10:00:00",
    "eventType": "INVALID_TYPE"
  }' \
  -G --data-urlencode "createdBy=12345678-1234-1234-1234-123456789012"
```

## Enum Values

Supported eventType values:
- `WEDDING` - Wedding events
- `CONFERENCE` - Conference and business meetings
- `BIRTHDAY` - Birthday celebrations
- `FUNERAL` - Funeral services
- `CORPORATE` - Corporate events

## Summary

✅ EventType feature fully integrated with safety checks
✅ Global exception handler provides clear validation errors
✅ Database migration strategy supports both dev and production
✅ Backward compatibility maintained for existing events
✅ Validation ensures data integrity going forward
