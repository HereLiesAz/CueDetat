## 2024-02-12 - [Network Security Config]
**Vulnerability:** The app lacked an explicit Network Security Configuration, relying on default system behavior which might allow cleartext traffic on older Android versions or if defaults change.
**Learning:** Even if `https` is used in code, explicit configuration is needed to prevent accidental regressions or insecure dependencies.
**Prevention:** Added `network_security_config.xml` with `cleartextTrafficPermitted="false"`.

## 2024-05-21 - [Unidirectional Data Flow for Security]
**Vulnerability:** The `AzNavRailMenu` composable was directly launching Intents for external URLs, bypassing the centralized `SecurityUtils.isSafeUrl` check in `MainActivity`.
**Learning:** Violating architectural patterns (UDF) often leads to security inconsistencies. Centralizing side-effects ensures security checks are applied uniformly.
**Prevention:** Refactored `AzNavRailMenu` to emit events (`ViewAboutPage`, `SendFeedback`) which are handled by the ViewModel and Activity, where security validation is already in place.
