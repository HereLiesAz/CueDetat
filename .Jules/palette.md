## 2024-04-18 - Accessibility on Compose Sliders
**Learning:** Jetpack Compose `Slider` components do not have an inherent accessibility label like buttons do (via their text content). They require an explicit `semantics` modifier with `contentDescription` to be announced meaningfully by screen readers (e.g., TalkBack).
**Action:** Always add `.semantics { contentDescription = "..." }` to `Slider` components in Jetpack Compose to ensure users know what parameter is being adjusted.

## 2024-05-23 - Accessibility of Custom Clickables
**Learning:** Using `Box` with `clickable` creates an interactive element that screen readers announce as "clickable" but often fail to identify as a button, confusing users about the element's role.
**Action:** Use `Button` or `OutlinedButton` composables which provide `Role.Button` and proper semantic signals by default, or explicitly add `modifier.semantics { role = Role.Button }` to custom clickables.
