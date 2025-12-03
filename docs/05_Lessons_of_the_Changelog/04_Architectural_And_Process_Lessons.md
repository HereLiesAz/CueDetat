# 5.4. Historical Lessons: Architecture & Development Process

This document archives high-level lessons learned from major refactoring events and compile-time
failures.

### Case Study: The Great Unraveling (Refactoring)

* **Issue:** A request to fix a single, small bug in the perspective logic led to a catastrophic,
  multi-day cascade of compilation errors that broke the entire application.
* **Cause:** The developer (AI) initiated a large-scale, unrequested architectural refactoring while
  attempting to fix the bug. This included restructuring events, consolidating reducers, and
  changing UI paradigms. The refactoring was incomplete and created deep, systemic inconsistencies.
* **Lesson Learned:** Do one thing at a time. Never mix new features or architectural changes with
  bug fixes. A large-scale refactoring must be meticulously planned and executed as a separate,
  atomic task. An incomplete migration is not progress; it is the creation of a failed state that is
  often harder to fix than the original problem.

### Case Study: The Schism of Injection (Dependency Injection)

* **Issue:** The application failed to compile with an `InvocationTargetException` when attempting
  to provide a nullable `ObjectDetector` via Hilt.
* **Cause:** Hilt's dependency graph must be complete and non-nullable at compile time. A
  `@Provides` method cannot return a potentially `null` object because the Dagger/Hilt processor
  cannot generate a `Factory` for something that might not exist.
* **Lesson Learned:** Dependency injection modules are for providing concrete, guaranteed
  implementations, not for runtime logic. The choice of which detector to use was moved from the
  `AppModule` into the `VisionRepository` itself, which now instantiates its own dependencies. This
  keeps the dependency graph pure and moves the conditional logic to the runtime component that
  actually uses it.

### Case Study: Native Library Loading

* **Issue:** The application crashed on startup with an `UnsatisfiedLinkError`.
* **Cause:** The code attempted to use OpenCV native functions (e.g., the `Mat` constructor) before
  the OpenCV native library had been loaded into the application's runtime memory.
* **Lesson Learned:** Native libraries must be explicitly loaded at the earliest possible moment in
  the application's lifecycle. The `OpenCVLoader.initDebug()` function must be called in
  `Application.onCreate()` to ensure all native dependencies are available before any code attempts
  to use them.