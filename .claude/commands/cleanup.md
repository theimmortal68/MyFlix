Clean up and optimize the codebase, then commit changes.

Steps to perform:
1. Review all modified files in the current update
2. Remove unused imports from Kotlin files
3. Remove duplicate code
4. Ensure consistent formatting
5. Check for TODO comments that should be addressed
6. Verify no debug logging left in production code
7. **Apply changes equally to TV and mobile apps** - Any feature, fix, or improvement made to one app must be applied to the other. Always check both app-tv and app-mobile when reviewing changes.
8. **Move common code to core modules** - Extract shared logic (models, utilities, formatters, state classes) to appropriate core modules (core/common, core/seerr, etc.) to avoid duplication
9. Build both apps to verify compilation: `./gradlew :app-tv:assembleDebug :app-mobile:assembleDebug`
10. **Stage all modified files and create the git commit** - Do NOT ask for confirmation, just commit
11. **Push to remote** - After committing, push to the remote repository without asking for confirmation

For unused import removal, use:
```bash
# Find files with potential unused imports
find . -name "*.kt" -exec grep -l "^import" {} \;
```

Commit message format (use HEREDOC for proper formatting):
```
feat/fix/refactor: Brief description

- Bullet point summary of changes
- Additional details as needed

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

IMPORTANT: After cleanup is complete, automatically stage all changes, create the commit, and push to remote. Do not ask "Would you like me to commit?" or "Would you like me to push?" - just do it.
