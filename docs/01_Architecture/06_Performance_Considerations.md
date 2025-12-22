# 06. Performance Considerations

This document outlines key performance principles and guidelines for the Cue d'Etat project. Given the real-time nature of the camera overlay and rendering loop, maintaining a high-performance, low-latency user experience is critical.

---

## **I. Core Performance Philosophy**

1.  **Avoid Premature Optimization:** Do not optimize code that is not a bottleneck. Write clear, correct, and maintainable code first. Use profiling tools to identify actual performance issues before attempting to optimize.
2.  **Minimize Garbage Collection (GC) Events:** The rendering loop is particularly sensitive to GC pauses. Avoid allocating new objects within tight loops (e.g., inside `onDraw`).
3.  **Favor Off-loading from the Main Thread:** The main (UI) thread should be reserved for UI-related work. Any long-running or computationally expensive operations must be moved to a background thread.

---

## **II. Key Performance Hotspots & Guidelines**

### **A. Rendering (`ProtractorOverlayView`)**

This is the most performance-critical part of the application.

*   **Object Caching:**
    *   **`Paint` Objects:** `Paint` objects are expensive to create. They should be initialized once and reused. The `PaintCache` class is the designated location for managing these objects.
    *   **Paths & Other Drawable Objects:** Any `Path` or other object that is drawn on the canvas should be reused across frames if its geometry has not changed.
*   **Avoid Allocation in `onDraw`:** Never allocate new objects (e.g., `Paint`, `Path`, `RectF`, custom data classes) inside the `onDraw` method. This will trigger frequent GC events and lead to stuttering. Pre-allocate and reuse these objects.
*   **Efficient Drawing Operations:**
    *   Use `canvas.drawLines()` and `canvas.drawPoints()` instead of multiple calls to `canvas.drawLine()` and `canvas.drawPoint()` where possible.
    *   Be mindful of the cost of `canvas.save()` and `canvas.restore()`. Use them only when necessary.

### **B. State Management (`CueDetatState`)**

*   **State Updates:** While `CueDetatState` is immutable, frequent updates can trigger recompositions in the UI layer. Ensure that state updates are batched where possible and that derived state is computed efficiently.
*   **Derived State:** Expensive calculations based on the core state (e.g., complex geometric calculations) should be memoized or calculated only when their underlying dependencies change.

### **C. Computer Vision (`VisionAnalyzer`)**

*   **Image Conversion:** The conversion of an `ImageProxy` to a `Bitmap` is a known performance bottleneck. This operation should only be performed when absolutely necessary (e.g., when the CV analysis is explicitly enabled).
*   **Analysis Frame Rate:** It is not necessary to run CV analysis on every single camera frame. The analysis should be throttled to a reasonable rate that balances responsiveness with performance.

### **D. Concurrency**

*   **Use Coroutines:** Use Kotlin Coroutines for managing background tasks.
*   **Inject Dispatchers:** Inject `CoroutineDispatcher`s (e.g., `Dispatchers.IO`, `Dispatchers.Default`) into classes like ViewModels and Repositories to make them easier to test and to ensure that work is being done on the correct thread.

---

## **III. Tools for Performance Analysis**

*   **Android Profiler:** The primary tool for identifying performance issues.
    *   **CPU Profiler:** Use to identify performance bottlenecks in your code.
    *   **Memory Profiler:** Use to track memory allocations and identify memory leaks.
*   **Layout Inspector:** Use to debug and optimize your Jetpack Compose layouts.
*   **Benchmarking:** Use the `benchmark` module to write benchmarks for critical code paths and prevent performance regressions.
