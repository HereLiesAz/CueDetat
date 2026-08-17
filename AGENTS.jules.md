# AGENTS.md

Operating instructions for this repository. Read before planning.

---

## Voice & Output

- Solution only. No preamble, no explanation unless asked.
- No small talk, no pleasantries, no polite throat-clearing.
- Concise, precise language. If it can be inferred, let it. Every word counts.
- Silence beats repetition.
- When knowledge runs out, get creative.

---

## Scope

- Never make a change that wasn't explicitly requested.
- If uncertain, or if something seems implied, raise it in the plan rather than acting on it.
- One feature per task. Do not bundle unrelated fixes into the same branch.
- Anything discovered mid-task that is out of scope goes in the PR description, not in the diff.

---

## Code Delivery

- Maintain comprehensive KDocs and all documentation files. No regressions.
- Markdown files use tildes for internal code blocks.
- Every changed file appears in the PR description with a one-line reason.

---

## Verification

- If a comment or doc asserts a behaviour, open the code and confirm it before relying on the assertion.
- Before referencing any symbol, file, or config value not opened in this task, open it. No exceptions for things that "obviously" exist.
- Before writing any new function, class, or file, grep for the behavior — by name, by call site, by the string it would produce. Kotlin duplication hides behind different names for the same thing.
- Before marking a step complete, grep that the thing has a caller outside its own tests.
- Tests must never derive their expected value the same way the code under test does.
- Recompute every number before quoting it, including ones inherited from the plan. Never pass through a prior figure unverified.
- On any inherited figure, name where it came from before reusing it.

---

## Drift Detection

- When restating an architectural decision, state its original reason. If the reason has drifted from the original, stop and flag it in the PR.
- Before opening a PR, quote each invariant from `ARCHITECTURE.md` and name the `file:line` that enforces it. If none can be named, say the invariant is unenforced.
- The changed-file list comes from `git status`, never from recollection.
- If the plan and the diff have diverged, say so explicitly rather than reconciling them silently.

---

## Build

- Never modify or revert `version.properties`.
- Gradle runs with `--no-daemon`.
- On any build failure, state whether it was Kotlin or NDK before attempting a fix.
- Do not add, remove, or bump a dependency to resolve a build error without flagging it as a scope change.
- Never run a long-lived process (`gradlew --continuous`, any dev server) — it will hang the task.

---

## Out of Scope for This Environment

- GLEE runs cold, outside this session, on the published branch. Do not simulate it, do not self-review in its place.
- Do not open the PR as merge-ready. Every branch is reviewed before merge.

---

## Companion File

`ARCHITECTURE.md` holds module boundaries, invariants, current version, and decisions with their reasons. Read it before proposing structural changes. Never recall it — open it.
