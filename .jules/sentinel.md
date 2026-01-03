## 2024-02-12 - [Network Security Config]
**Vulnerability:** The app lacked an explicit Network Security Configuration, relying on default system behavior which might allow cleartext traffic on older Android versions or if defaults change.
**Learning:** Even if `https` is used in code, explicit configuration is needed to prevent accidental regressions or insecure dependencies.
**Prevention:** Added `network_security_config.xml` with `cleartextTrafficPermitted="false"`.
