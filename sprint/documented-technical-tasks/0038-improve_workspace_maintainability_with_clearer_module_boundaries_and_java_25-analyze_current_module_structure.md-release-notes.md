## Summary
Added a four-part module-structure analysis that documents the current backend/frontend layout, dependency pressure points, product-area mapping, and recommended refactoring sequence for future modularization.

## Changes
Added `docs/analysis/current-module-structure.md`.
Added `docs/analysis/dependency-analysis.md`.
Added `docs/analysis/product-area-mapping.md`.
Added `docs/analysis/refactoring-recommendations.md`.

## Impact
The repository now has an explicit baseline for reorganizing the workspace into clearer product modules without changing runtime behavior.

## Verification
Executed `./gradlew --no-daemon test` from the repository root.

## Follow-ups
Use these analysis documents to drive the upcoming module extraction tasks for shared kernel, user management, document management, and chat system boundaries.
