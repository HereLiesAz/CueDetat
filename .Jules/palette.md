## 2024-04-18 - Accessibility on Compose Sliders
**Learning:** Jetpack Compose `Slider` components do not have an inherent accessibility label like buttons do (via their text content). They require an explicit `semantics` modifier with `contentDescription` to be announced meaningfully by screen readers (e.g., TalkBack).
**Action:** Always add `.semantics { contentDescription = "..." }` to `Slider` components in Jetpack Compose to ensure users know what parameter is being adjusted.
