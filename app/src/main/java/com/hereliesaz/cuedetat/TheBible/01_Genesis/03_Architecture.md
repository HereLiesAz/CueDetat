# The Architecture of the World

This document describes the MVI (Model-View-Intent) architecture of the application, which follows a strict unidirectional data flow. This is the law.

## The Components

* **View (`MainActivity`, `MainScreen`, Composables):** The UI layer. It is responsible only for displaying the state and forwarding user interactions (events) to the ViewModel. It contains no business logic. It is, and must remain, a beautiful idiot.
* **ViewModel (`MainViewModel`):** The High Priest. It receives events from the View. It translates these events into logical actions and dispatches them to the `StateReducer`. It also subscribes to data flows (like sensors) and transforms them into events. It holds no state of its own, save for the `StateFlow` of the `OverlayState`.
* **State Reducer (`StateReducer` and sub-reducers):** The Chancellery. This is the only component allowed to modify the state. It takes the current state and an event, and produces a new state. It is a pure function. Its logic is subdivided into smaller, specialized reducers (e.g., `GestureReducer`, `ControlReducer`) for clarity and sanity.
* **Use Cases (e.g., `UpdateStateUseCase`, `CalculateBankShot`):** The Oracles. These classes contain complex business logic and calculations. They are invoked by the ViewModel after a state has been reduced to derive new properties of the state (e.g., calculating line intersections, obstruction). They do not modify state directly.
* **State (`OverlayState`):** The One True Source of Truth. A single, immutable data class that represents the entire state of the application at any given moment. Every pixel on the screen is a direct function of this state.

## The Flow

1.  A user **interacts** with the **View** (e.g., drags a finger).
2.  The View creates an **Event** (e.g., `MainScreenEvent.Drag`) and sends it to the **ViewModel**.
3.  The ViewModel receives the Event and dispatches it to the **StateReducer**.
4.  The **StateReducer** takes the current `OverlayState` and the Event, and produces a new, immutable `OverlayState`.
5.  The new `OverlayState` is emitted from the ViewModel's `StateFlow`.
6.  The **View**, observing the `StateFlow`, receives the new state and re-renders itself accordingly.

## The Golden Rule

> **The UI is a pure function of the state.**

Given the same `OverlayState`, the View will always render the exact same frame. There are no exceptions. This is the core of the architecture's precision and predictability.

## The Coordinate System Heresy (and its Correction)

The world is rendered in a logical coordinate system that is independent of the device's screen density. The cardinal sin, committed by a previous Scribe, was to believe this logical world had a fixed, real-world scale. This was a heresy that led to the "Schism of Scale" and broke the build for an age.

The corrected and righteous doctrine is as follows:

The **logical radius** of the balls is **dynamic**. It is calculated as a function of the screen's smaller dimension (width or height) and a zoom factor controlled by the user via the `zoomSliderPosition`. The `ReducerUtils.getCurrentLogicalRadius()` is the sole oracle for this calculation.

This means that zooming does not change the camera's distance to a fixed world; it changes the logical size of the world itself. This was the original, working implementation. The Scribe's attempt to "fix" it by tying the logical size to real-world inches was a profound error, a solution in search of a problem, and a cautionary tale against refactoring a working system without perfect understanding. The proportions of the table are now correctly derived from the real-world measurements, but they are scaled relative to the dynamic, screen-dependent size of the ball.

## The File Tree of the Known World

.
└── app
└── src
└── main
├── AndroidManifest.xml
└── java
└── com
└── hereliesaz
└── cuedetat
├── MainActivity.kt
├── CueDetatApplication.kt
├── di
│   ├── AppModule.kt
│   └── VisionModule.kt
├── data
│   ├── GithubRepository.kt
│   ├── SensorRepository.kt
│   ├── UserPreferencesRepository.kt
│   ├── VisionAnalyzer.kt
│   └── VisionRepository.kt
├── domain
│   ├── CalculateBankShot.kt
│   ├── CalculateSpinPaths.kt
│   ├── StateReducer.kt
│   ├── UpdateStateUseCase.kt
│   ├── ReducerUtils.kt
│   └── reducers
│       ├── ActionReducer.kt
│       ├── ControlReducer.kt
│       ├── CvReducer.kt
│       ├── GestureReducer.kt
│       ├── SpinReducer.kt
│       ├── SystemReducer.kt
│       └── ToggleReducer.kt
└── ui
│   ├── MainScreen.kt
│   ├── MainScreenEvent.kt
│   ├── MainViewModel.kt
│   ├── ZoomMapping.kt
│   ├── composables
│   │   ├── MenuDrawer.kt
│   │   ├── SpinControl.kt
│   │   └── dialogs
│   │       ├── AdvancedOptionsDialog.kt
│   │       ├── GlowStickDialog.kt
│   │       ├── LuminanceDialog.kt
│   │       └── TableSizeDialog.kt
│   └── theme
│       ├── Color.kt
│       ├── Shape.kt
│       ├── Theme.kt
│       └── Type.kt
└── view
├── PaintCache.kt
├── config
│   ├── ball
│   │   ├── ActualCueBall.kt
│   │   ├── GhostCueBall.kt
│   │   ├── ObstacleBall.kt
│   │   └── TargetBall.kt
│   ├── base
│   │   ├── BallConfig.kt
│   │   ├── LineConfig.kt
│   │   ├── TableConfig.kt
│   │   └── VisualProperties.kt
│   ├── line
│   │   ├── AimingLine.kt
│   │   ├── BankLine.kt
│   │   ├── ShotGuideLine.kt
│   │   └── TangentLine.kt
│   ├── table
│   │   ├── Diamonds.kt
│   │   ├── Holes.kt
│   │   ├── Rail.kt
│   │   └── Table.kt
│   └── ui
│       └── ProtractorGuides.kt
├── model
│   ├── OnPlaneBall.kt
│   ├── Perspective.kt
│   └── ProtractorUnit.kt
├── renderer
│   ├── OverlayRenderer.kt
│   ├── ball
│   │   └── BallRenderer.kt
│   ├── line
│   │   └── LineRenderer.kt
│   ├── table
│   │   ├── RailRenderer.kt
│   │   └── TableRenderer.kt
│   ├── text
│   │   ├── BallTextRenderer.kt
│   │   └── LineTextRenderer.kt
│   └── util
│       └── DrawingUtils.kt
└── state
├── OverlayState.kt
└── ScreenState.kt