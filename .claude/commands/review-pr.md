Review a pull request thoroughly and provide actionable feedback.

## Usage
```
/review-pr [PR number or URL]
```

## Review Process

### 1. Fetch PR Information
Get the PR details, changed files, and diff:
- Use `mcp__github__get_pull_request` to get PR metadata
- Use `mcp__github__get_pull_request_files` to list changed files
- Use `mcp__github__get_pull_request_diff` to see actual changes

### 2. Analyze Changes
For each changed file, evaluate:

**Code Quality**
- Does it follow existing patterns in the codebase?
- Are there any Detekt/lint issues? (TooManyFunctions, LongMethod, etc.)
- Are there unnecessary `@Suppress` annotations?
- Is the code readable and well-named?

**Architecture**
- Does it fit the MyFlix module structure?
- Is state management consistent? (remember + mutableStateOf pattern)
- For TV: Is focus management handled correctly?
- Are API calls using Result<T> properly?

**Potential Issues**
- Missing null checks
- Uncaught exceptions
- Memory leaks (unremoved listeners, uncancelled coroutines)
- Performance concerns (unnecessary recomposition, missing keys)

**Testing**
- Are there tests for new functionality?
- Do existing tests still pass?

### 3. Provide Feedback

Structure the review as:

```
## Summary
[One paragraph overview of the PR and its purpose]

## ✅ What's Good
- [Positive observations]

## 🔧 Suggestions
- [Non-blocking improvements]

## ❌ Issues (if any)
- [Blocking problems that should be fixed]

## Files Reviewed
- [List of files with brief notes]
```

### 4. Submit Review (Optional)
If requested, use `mcp__github__create_pull_request_review` to submit the review directly to GitHub.

## MyFlix-Specific Checks

- [ ] No new `@Suppress` annotations (except DEPRECATION for SDK compat)
- [ ] TV screens: FocusRequester patterns correct
- [ ] API calls: Uses JellyfinClient with .onSuccess/.onFailure
- [ ] Images: Uses WebP URLs from jellyfinClient
- [ ] State: Uses remember + mutableStateOf, not ViewModel
- [ ] Card sizes: Uses CardSizes.MediaCardWidth (120.dp) or WideMediaCardWidth (210.dp)
