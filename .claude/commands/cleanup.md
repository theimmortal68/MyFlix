Clean up and optimize the codebase for commit.

Steps to perform:
1. Review all modified files in the current update
2. Remove unused imports from Kotlin files
3. Remove duplicate code
4. Ensure consistent formatting
5. Check for TODO comments that should be addressed
6. Verify no debug logging left in production code
7. **Apply changes equally to TV and mobile apps** - Any feature, fix, or improvement made to one app must be applied to the other
8. **Move common code to core modules** - Extract shared logic (models, utilities, formatters) to appropriate core modules
9. Generate a comprehensive git commit message

For unused import removal, use:
```bash
# Find files with potential unused imports
find . -name "*.kt" -exec grep -l "^import" {} \;
```

Commit message format:
```
feat/fix/refactor: Brief description

## Changes
- List of changes

## Technical Details
- Implementation notes

## Cleanup
- Removed unused imports
- Refactored X for clarity
```
