Create a zip file containing the current code changes.

## Instructions

1. Identify all files that were created or modified
2. Create a zip file with a unique name following the pattern:
   `myflix-{feature}-{YYYYMMDD}-{HHMM}.zip`
3. The zip must preserve the directory structure from the git root
4. Include only the changed files, not the entire project

## Example

For a feature adding hero section improvements on January 11, 2026 at 2:30 PM:

```bash
# Create zip with proper structure
zip -r myflix-hero-improvements-20260111-1430.zip \
  app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/HeroSection.kt \
  app-tv/src/main/java/dev/jausc/myflix/tv/ui/screens/HomeScreen.kt
```

## Commit Message Template

After creating the zip, provide a complete commit message:

```
feat(tv): Brief description of the feature

## Summary
What this change does and why.

## Changes
- app-tv/.../HeroSection.kt: Description of changes
- app-tv/.../HomeScreen.kt: Description of changes

## Technical Notes
- Any implementation details worth noting
- Performance considerations
- Breaking changes if any

## Testing
- How to test the changes
- Known issues or limitations

## Cleanup
- Removed unused imports from X files
- Simplified Y implementation
```
