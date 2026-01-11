# Claude Code Setup for MyFlix

This package adds Claude Code CLI configuration to your existing MyFlix project.

## Contents

```
.claude/
├── settings.json              # Permissions for Claude Code
├── CODE_QUALITY_ADDENDUM.md   # Append to your existing CLAUDE.md
├── skills/                    # Custom skills for MyFlix development
│   ├── myflix-architecture/
│   ├── kotlin-compose-patterns/
│   ├── android-tv-development/
│   └── jellyfin-api/
└── commands/                  # Custom slash commands
    ├── build-tv.md
    ├── build-mobile.md
    ├── cleanup.md
    └── zip-update.md
.mcp.json                      # MCP server configuration
```

## Installation

### 1. Extract to Your MyFlix Project Root

```bash
cd ~/StudioProjects/MyFlix
unzip claude-code-setup-myflix-final.zip
```

This will add/merge files into your existing `.claude/` directory.

### 2. Append Code Quality Rules to CLAUDE.md

```bash
cat .claude/CODE_QUALITY_ADDENDUM.md >> .claude/CLAUDE.md
```

Or manually copy the content from `CODE_QUALITY_ADDENDUM.md` to the end of your existing `.claude/CLAUDE.md`.

### 3. (Optional) Set GitHub Token for MCP

```bash
export GITHUB_TOKEN="your-github-personal-access-token"
```

Add to your shell profile (`~/.bashrc` or `~/.zshrc`) for persistence.

### 4. Install Recommended Plugins

Launch Claude Code in your project directory, then run:

```
/plugin install anthropics/claude-code/plugins/pr-review-toolkit
```

### 5. Restart Claude Code

Exit and relaunch to load the new configuration:

```bash
cd ~/StudioProjects/MyFlix
claude
```

## Verification

After restarting, verify the setup:

1. **Check skills:** Ask "what skills do you have for MyFlix?"
2. **Check commands:** Type `/` and look for `build-tv`, `cleanup`, etc.
3. **Check code quality rules:** Ask "what are the code quality rules?"

## Custom Skills

| Skill | When Used |
|-------|-----------|
| `myflix-architecture` | Creating new features, understanding project structure |
| `kotlin-compose-patterns` | Writing Compose UI, state management, focus handling |
| `android-tv-development` | TV screens, D-pad navigation, hero sections |
| `jellyfin-api` | API calls, caching, image URLs |

## Custom Commands

| Command | Purpose |
|---------|---------|
| `/build-tv` | Build `app-tv` debug APK |
| `/build-mobile` | Build `app-mobile` debug APK |
| `/cleanup` | Review code, remove unused imports, generate commit message |
| `/zip-update` | Create zip file with code changes |

## MCP Servers

| Server | Purpose |
|--------|---------|
| `github` | GitHub API integration (requires GITHUB_TOKEN) |
| `filesystem` | File operations |
| `memory` | Persistent context across sessions |

## Notes

- Skills are automatically referenced based on your task
- The code quality rules enforce proper Detekt fixes (no suppressions except for SDK compatibility)
- Settings.json pre-approves common development commands
