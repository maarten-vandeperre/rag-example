## Summary
Expanded the local development PostgreSQL setup with richer schema initialization, seeded development data, and utility scripts for reset, backup, and status inspection.

## Changes
Updated `infrastructure/database/init-dev.sql` with development-ready tables, constraints, indexes, views, triggers, and helper functions.
Updated `infrastructure/database/sample-dev-data.sql` with realistic users, documents, chat data, references, chunks, and sessions.
Added `infrastructure/database/reset-dev-db.sh`, `infrastructure/database/backup-dev-db.sh`, and `infrastructure/database/status-dev-db.sh` for local database operations.
Updated `README.md` with the new development database management commands.

## Impact
Developers now have a fuller local database bootstrap and repeatable management workflow that better supports development, manual testing, and service demos.

## Verification
Executed `chmod +x infrastructure/database/reset-dev-db.sh infrastructure/database/backup-dev-db.sh infrastructure/database/status-dev-db.sh`.
Executed `bash -n infrastructure/database/reset-dev-db.sh infrastructure/database/backup-dev-db.sh infrastructure/database/status-dev-db.sh`.
Executed a Python validation pass over `infrastructure/database/init-dev.sql` and `infrastructure/database/sample-dev-data.sql`.
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Run these scripts against a live local Podman PostgreSQL instance once the dev services are up, and consider adding smoke tests that exercise the reset and seed flow end to end.
