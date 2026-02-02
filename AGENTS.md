# AGENTS.md — Codex Workflow Rules for MyFlix

This repository uses a **multi-model workflow** for correctness and regression resistance:

- **Claude** is the orchestrator (problem framing + synthesis).
- **Codex CLI** is primarily for Kotlin/Compose implementation details.
- **Gemini** is for trade-offs, performance, and alternative designs.
- **GPT (ChatGPT)** acts as a **skeptical reviewer**: challenge assumptions, spot breakage risk, and sanity-check changes before landing.

If you are an agent operating in this repo, follow the rules below.

---

## Scope

This `AGENTS.md` applies whenever the current working directory is this repository **or any subdirectory**.
Agents MUST treat these rules as active when executed via `codex` or `codex exec` within this project tree.

---

## 0) Non-Negotiables

1. **Do not break TV navigation/focus.** Android TV D-pad behavior is a first-class requirement.
2. **Prefer minimal diffs.** Keep changes local; don’t refactor unrelated code.
3. **Match existing patterns.** Use existing architectures, naming, and state patterns unless explicitly changing them.
4. **No “drive-by” formatting churn.** Avoid mass reformatting/renaming that obscures the meaningful diff.
5. **No assumptions about Jellyfin API shapes.** Validate via tooling when possible (see MCP servers).

---

## Fast-Path Changes (No Consensus Required)

The consensus workflow MAY be skipped for:

- Typos, comments, docs-only tweaks
- Trivial logging
- Import ordering / non-behavioral formatting
- Single-line “obvious” fixes with **zero** focus/state/API impact

If there is any plausible risk to TV focus, playback, or shared models, **do not use the fast-path**.

---

## 1) On Session Start (Kotlin Work)

When working with Kotlin code, start Kotlin LSP first (enables go-to-definition, references, diagnostics).

**Action:**
- Call `mcp__kotlin-lsp__start_server` with `language_id = "kotlin"`.

**Notes:**
- Expect slow startup (~90 seconds) due to Gradle indexing.
- If diagnostics/navigation look wrong, verify the project root path used by the LSP.

---

## 2) Tooling & Capabilities (MCP Servers)

Agents operating in this repository have access to the following MCP servers and SHOULD use them instead of guessing.

### kotlin-lsp

**Purpose:** Kotlin / Jetpack Compose code intelligence.

Use for:
- Go-to-definition
- Find references
- Diagnostics / compile errors
- Symbol search
- Understanding call graphs before refactors
- Verifying focus/state flows before UI changes

Rules:
- Start Kotlin LSP at session start for Kotlin work.
- Before changing function signatures, data models, or navigation/focus logic, query references via kotlin-lsp.
- Do **not** assume file relationships — verify.

### jellyfin-ui

**Purpose:** Jellyfin API schema and live data validation.

Use for:
- Confirming API field names and nullability
- Inspecting `MediaSource` / `MediaStream` shapes
- Verifying playback / user data fields
- Testing assumptions about server responses

Rules:
- Do not hardcode or guess Jellyfin fields.
- When adding UI bound to Jellyfin data, validate via jellyfin-ui first.
- Prefer real responses over documentation assumptions.

### General MCP Rule

If an MCP server can answer the question, **use it instead of reasoning from memory**.
Memory is fallible; MCP responses are authoritative for this repo.

Example prompts:
- “Use kotlin-lsp to find all references to `PlayerViewModel` before changing its contract.”
- “Query jellyfin-ui to confirm the actual `MediaSource` response fields before adding UI.”

---

## 3) Collaboration / Consensus Workflow (MANDATORY)

### Default behavior: consensus before non-trivial changes

For any non-trivial change (bug fixes, focus/navigation changes, multi-file edits, state management changes),
Claude MUST consult **both** Codex and Gemini and then involve GPT (ChatGPT) as a reviewer.

**Process:**
1. **Claude** defines the problem, constraints, and candidate approaches.
2. **Claude → Codex**: request implementation-oriented solution and code patterns.
3. **Claude → Gemini**: request trade-offs/perf analysis and alternatives.
4. **Claude → GPT**: request a skeptical review:
   - challenge assumptions
   - identify breakage/regression risks
   - propose “must-test” scenarios
5. Claude synthesizes and either:
   - produces a single consensus plan, or
   - presents multiple options if disagreement remains.

### Bug Fix Protocol (MANDATORY)

When diagnosing/fixing bugs, Claude MUST:
1. Consult BOTH Codex and Gemini with bug + relevant code context
2. Wait for both responses
3. Achieve consensus on root cause + fix approach
4. Ask GPT to review the plan for regressions/breakage
5. Only implement after consensus is reached

(See “Fast-Path Changes” above for the narrow set of exceptions.)

---

## 4) GPT (ChatGPT) Reviewer Role — “Breakage Check”

When GPT is asked to review a plan or patch, GPT should explicitly check:

- **Focus model correctness**
  - Does any change impact D-pad navigation, focus restoration, or nav rail activation/exit?
  - Are `FocusRequester`s stable (not recreated in loops)?
  - Are `focusProperties { up/down/left/right }` consistent with the intended UX?

- **State & concurrency**
  - Are there race conditions between recomposition, async loading, and focus requests?
  - Is state hoisting consistent and not duplicated?
  - Does backpressure/cancellation exist for high-res images or network calls?

- **API correctness**
  - Are Jellyfin response fields validated (via jellyfin-ui MCP)?
  - Are nullability and fallback paths correct?

- **Regression surface**
  - Does this touch TV + mobile + core? If yes, ensure each has coverage.
  - Does it change data models? Ensure serializers/parsers remain compatible.

GPT should suggest a **test checklist** (manual + automated if available) before merge.

---

## 5) Prompting Guidelines (for Codex/Gemini)

When prompting other models, include:

1. Relevant code context (models, interfaces, existing patterns)
2. Constraints:
   - Android TV D-pad navigation
   - Jellyfin backend
   - Jetpack Compose UI
3. Specific question (avoid open-ended “how should I…”)
4. Desired output format (code, analysis, table)

Template:

```text
Context:
- What screen/module is this?
- What is the current behavior?
- What is the desired behavior?

Relevant code:
- (paste the smallest set of files/snippets needed)

Question:
- (single concrete question)

Constraints:
- Android TV focus & D-pad navigation must not regress
- Jellyfin API fields must be validated via jellyfin-ui
- Prefer minimal diffs; match existing patterns

Deliverable:
- (e.g., “Provide a patch-level plan + specific Kotlin code edits”)
```

---

## 6) Project Structure (Orientation)

- `app-tv/`
  - Compose for TV with explicit focus management
  - NavRail with activation model (Menu key / FocusSentinel)
  - Per-screen exit focus restoration via `LocalExitFocusState`

- `app-mobile/`
  - Standard Jetpack Compose + Material 3 patterns

- `core/`
  - `JellyfinClient` (API)
  - `JellyfinModels.kt` (models: JellyfinItem, MediaSource, MediaStream)
  - Shared repositories + view models

---

## 7) Key Patterns You Must Preserve

### TV Focus: Exit focus registration
- Register screen’s focus target for NavRail exit.
- Update exit focus when internal focus changes.

### Tab focus restoration
- Maintain stable map of FocusRequesters (not inside loops).
- Track last focused tab.
- Content area should route UP to last-focused tab.

### Focus halo effect
- Blur-based glow behind focused elements (don’t block focus or pointer).

### Menu positioning in padded containers
- Adjust anchor coordinates by container offsets when menus live inside padded UI like NavRail.

---

## 8) Build / Install (Reference)

Build TV app (from WSL):
- Use the Windows PowerShell call that sets `JAVA_HOME` to Android Studio JBR and runs `:app-tv:assembleDebug`.

Install:
- Use ADB targets for Shield TV and secondary device, installing the debug APK.

(Exact commands are documented in the project docs and/or `CLAUDE.md`.)

---

## 9) Working Agreement

- Claude orchestrates and synthesizes.
- Codex produces Kotlin/Compose implementation details.
- Gemini challenges architecture and performance assumptions.
- GPT challenges correctness and regression risk.

If a proposed change could plausibly break focus/navigation or playback, you MUST run the multi-model consensus protocol.
