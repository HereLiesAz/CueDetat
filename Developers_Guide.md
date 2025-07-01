# CueDetat Developer's Guide

## Part 1: Foundational Analysis of the Android XR Ecosystem

### 1.1 The New Stack: Deconstructing the Android XR, Jetpack XR, and OpenXR Relationship

A successful migration to Android's new Extended Reality (XR) platform necessitates a precise understanding of its architecture. The platform is not a monolithic framework but a thoughtfully layered stack designed for openness, portability, and developer accessibility. It comprises three distinct but interconnected tiers: OpenXR as the low-level hardware abstraction layer, Android XR as the specialized operating system, and Jetpack XR as the high-level, developer-facing SDK. Comprehending the role and interplay of each layer is the foundational prerequisite for any refactoring effort, as it informs every subsequent architectural decision. This structure represents a strategic commitment by Google to foster a diverse hardware ecosystem while simultaneously lowering the barrier to entry for the vast community of existing Android developers.

#### The Foundation: OpenXR - The Cross-Platform Enabler

At the very base of the Android XR platform lies OpenXR, a royalty-free, open standard managed by the Khronos Group. Google's decision to build its perception stack upon the OpenXR 1.1 specification is a pivotal one. It decouples the application software from proprietary hardware APIs, a move that stands in contrast to more closed XR ecosystems. For developers, this means that applications built for Android XR are inherently designed for portability, capable of running across a wide range of current and future XR devices from various manufacturers without significant code changes. This approach mitigates the risk of platform lock-in and future-proofs development efforts.

Functionally, OpenXR standardizes the interface between an application and an XR runtime. It provides a common set of APIs to access core device capabilities, such as head-mounted display (HMD) characteristics, controller inputs, and positional tracking data. The OpenXR architecture consists of an application, which calls the OpenXR API, and a runtime, provided by the hardware vendor, which implements that API for a specific device. Android XR leverages this standard and extends it with a suite of vendor-specific extensions, prefixed with `XR_ANDROID_*`. These extensions expose advanced, platform-specific features that go beyond the core specification, including high-fidelity hand mesh tracking, face and eye tracking, persistent spatial anchors, scene understanding, and camera passthrough capabilities. This two-pronged approach allows developers to write code against a stable, cross-platform base while still being able to access the cutting-edge features of a particular Android XR device.

#### The Platform: Android XR - The Operating System

Positioned above OpenXR is Android XR, the operating system itself, specifically engineered for immersive computing devices like headsets and smart glasses. Android XR is not merely a library but a full-fledged fork of the Android Open Source Project (AOSP), tailored to manage the unique demands of spatial applications. It governs the user's environment, system-level interactions, and application lifecycle.

A key concept introduced by the Android XR OS is the distinction between two primary application environments: `Home Space` and `Full Space`. The `Home Space` acts as a multitasking hub, analogous to a desktop environment, where multiple 2D or spatialized applications can run side-by-side in distinct windows or panels. This is the default environment for most standard Android apps, which can run on Android XR with little to no modification. In contrast, the `Full Space` is a fully immersive, single-application mode. When an app requests `Full Space`, it takes over the user's entire field of view, gaining exclusive access to the device's 3D and spatial capabilities, hiding all other applications and system UI. This mode is essential for creating truly immersive virtual worlds, complex AR scenarios, and focused entertainment experiences. The operating system, in conjunction with the Google Play Store, manages the distribution of apps, filtering out those that require unsupported hardware features (like telephony) while making most others available in the `Home Space` by default.

#### The High-Level SDK: Jetpack XR - The Developer's Toolkit

For the vast majority of application developers, particularly those working with Kotlin and modern Android practices, the Jetpack XR SDK is the primary and most direct entry point into this new ecosystem. It is a suite of libraries, delivered through the `androidx.*` package hierarchy, that provides high-level, idiomatic APIs for building XR experiences. The fundamental purpose of Jetpack XR is to abstract away the complexities of the underlying Android XR platform and the native OpenXR APIs, allowing developers to leverage their existing skills and familiar tools like Android Studio and Jetpack Compose.

This abstraction is a critical component of Google's strategy. By framing XR development in the context of well-known Jetpack libraries, it significantly shortens the learning curve. Developers can think in terms of `Composable` functions, `ViewModel`s, and `Lifecycle` owners, rather than needing to master the intricacies of OpenXR handles, sessions, and swapchains from day one. The Jetpack XR SDK is not a single library but a collection of specialized modules, each targeting a specific aspect of XR development, from spatial UI and 3D scene management to world perception. The layered architecture—with OpenXR providing hardware portability, Android XR providing the OS foundation, and Jetpack XR providing developer-friendly tools—creates a powerful synergy. It fosters a broad hardware ecosystem through open standards while simultaneously empowering a massive existing developer base to build for that ecosystem using familiar paradigms. For any legacy project, migrating to this stack means adopting a robust, well-supported, and future-proof platform.

### 1.2 The Jetpack XR SDK: A Deep Dive into the Core Libraries

The Jetpack XR SDK is a modular suite of libraries, each designed to address a specific domain within XR application development. A successful migration requires mapping the functionality of a legacy application to the appropriate components within this suite. Understanding the purpose and primary components of each core library is therefore essential for planning the refactoring process.

#### `androidx.xr.compose` (Jetpack Compose for XR): The Spatial UI Framework

This library is arguably the most transformative part of the Jetpack XR SDK for traditional Android developers. Its purpose is to extend the declarative Jetpack Compose UI toolkit into the third dimension, enabling the creation of spatial user interfaces. It allows developers to apply their existing knowledge of Compose—building UIs by describing what they should look like as a function of state—to layouts that exist in 3D space. This dramatically lowers the barrier to entry for creating immersive UI, moving away from the complex, manual 3D geometry and matrix transformations that were previously required.

The key components of `androidx.xr.compose` provide the building blocks for spatial layouts:

* **`Subspace`**: A fundamental container composable. It partitions a region of 3D space where spatial content can be placed. Any UI defined within a `Subspace` is rendered in the 3D environment when spatialization is active, and ignored otherwise, providing a natural fallback for 2D screens.
* **`SpatialPanel`**: Represents a flat, 2D plane within the 3D `Subspace`. This is the primary vehicle for porting existing 2D UI into XR. A standard `@Composable` function, or even a legacy Android View, can be hosted within a `SpatialPanel`, effectively placing it on a virtual surface in the user's environment.
* **`Orbiter`**: A specialized UI component designed for contextual actions or navigation, which "orbits" a parent entity, typically a `SpatialPanel`. It is the spatial equivalent of a `BottomNavigationView` or a floating action button menu.
* **`Volume`**: A composable that defines a 3D bounding box within a `Subspace`, specifically designed to host 3D content from Jetpack SceneCore, such as 3D models.
* **`SubspaceModifier`**: Analogous to the standard `Modifier` in Jetpack Compose, `SubspaceModifier` is used to decorate spatial composables. It provides methods for manipulating objects in 3D space, such as `.offset(x, y, z)`, `.rotate()`, `.scale()`, and for adding behaviors like `.movable()` and `.resizable()`.

#### `androidx.xr.scenecore` (Jetpack SceneCore): The 3D Scene Engine

Jetpack SceneCore is the designated successor to the now-deprecated Google Sceneform library. It is a high-level, Kotlin-first framework for creating, managing, and rendering 3D content within an XR application. SceneCore provides the foundational capabilities for building custom 3D experiences, allowing developers to load 3D models, define environments, place spatial audio sources, and arrange all of these elements relative to each other and the user's environment.

The most significant aspect of SceneCore is its adoption of an **Entity-Component-System (ECS)** architecture. This represents a fundamental paradigm shift away from the traditional object-oriented inheritance model used by Sceneform and many older game engines. In an ECS architecture:

* An **`Entity`** is simply a unique identifier, a lightweight "bag" that holds components. It contains no data or logic itself.
* A **`Component`** is a plain data structure that holds a specific piece of state (e.g., a `TransformComponent` holds position and rotation, a `RenderableComponent` holds a reference to a 3D mesh).
* A **`System`** contains the logic that operates on entities that possess a specific set of components (e.g., a `PhysicsSystem` would iterate over all entities that have both a `TransformComponent` and a `RigidBodyComponent`).

This composition-over-inheritance approach promotes greater flexibility and can lead to significant performance benefits due to more cache-friendly data layouts. Migrating from Sceneform to SceneCore requires not just a one-to-one API replacement, but a re-architecting of the 3D logic to fit this new, more data-oriented paradigm.

#### `androidx.xr.arcore` (ARCore for Jetpack XR): World Perception

This library provides the application with an understanding of the real world, which is essential for any Augmented Reality (AR) experience. It is inspired by the original, standalone ARCore library but has been re-architected for the new XR platform. Its capabilities include robust motion tracking (6DoF tracking of the device), plane detection (identifying horizontal and vertical surfaces like floors and walls), persistent anchors (saving and loading virtual content locations across sessions), hit testing (casting rays to find real-world geometry), and, as of Developer Preview 2, hand tracking.

A crucial architectural detail is that `ARCore for Jetpack XR` is no longer a self-contained perception stack. Instead, it serves as a high-level, developer-friendly API that **leverages the underlying perception capabilities powered by the OpenXR runtime**. This ensures that an application using these AR features will be compatible with any Android XR device that correctly implements the corresponding OpenXR extensions, thereby future-proofing the app and aligning it with the platform's open-standard philosophy.

#### `androidx.xr.runtime` (Jetpack XR Runtime): The Foundational Glue

The `androidx.xr.runtime` library is the foundational layer of the Jetpack XR SDK. It contains the essential, low-level pieces that connect the higher-level libraries (like SceneCore and Compose for XR) to the Android XR operating system. Developers will interact with this library directly, even when using the more abstract components.

Its primary responsibilities and key classes include:

* **Lifecycle and Session Management**: The core `Session` class resides here. It is the main handle to the XR system, providing fine-grained control over the XR processing lifecycle, configuration, and access to all other system capabilities. Recent updates have made `Session` implement `androidx.lifecycle.LifecycleOwner`, simplifying its integration into modern Android app architectures.
* **Capability Discovery and Configuration**: This library handles discovering the capabilities of the device and allows the application to configure the runtime features it wishes to use via a `Config` object passed to `Session.configure()`.
* **Runtime Selection**: It supports specifying multiple runtime implementations at compile time (e.g., `runtime-openxr` for real devices and `runtime-testing` for unit tests), with the correct one being loaded at execution time based on the device's feature set.
* **Fundamental Abstractions**: It provides core mathematical abstractions like `Pose` (a position and orientation in 3D space), `Vector3`, and `Matrix4`, which are used consistently across the entire Jetpack XR API surface.

## Part 2: Pre-Migration Audit and Environment Configuration

### 2.1 Framework for Auditing the Legacy 'CueDetat' Application

Before any code is refactored, a thorough and systematic audit of the existing application is imperative. This process creates a comprehensive inventory of all UI elements, 3D/AR logic, and architectural patterns currently in use. This audit serves as the direct input for the migration plan, allowing for accurate effort estimation and preventing unforeseen architectural roadblocks during development. For the prototypical 'CueDetat' project, this audit is broken down into three critical areas.

#### Checklist 1: UI Layer Analysis

This checklist focuses on deconstructing the existing user interface to understand its structure, navigation, and components.

* **Identify all `Activity` and `Fragment` classes**: List every screen entry point. These will be the primary candidates for being wrapped in or converted to spatial layouts.
* **Categorize UI implementation**: For each screen, determine if it is built with the traditional Android View system (XML layouts and widgets) or with standard, 2D Jetpack Compose (`@Composable` functions). This distinction will guide the UI migration strategy, as Compose-based UIs are more straightforward to spatialize.
* **Map out navigation components**: Document the current navigation flow. Is it using the Android Jetpack Navigation Component, custom `FragmentManager` transactions, or a third-party library? This map will need to be translated to a spatial navigation concept, likely involving `Orbiter`s or interactions between `SpatialPanel`s.
* **List all custom UI components**: Identify any custom views or complex composables (e.g., custom charts, intricate list items). These will require careful consideration to ensure they render correctly and are usable within a `SpatialPanel`.

#### Checklist 2: 3D/AR Engine Analysis (Assuming Sceneform)

This checklist targets the core immersive functionality, assuming the legacy app uses the deprecated Sceneform library.

* **Identify where `ArFragment` or `ArSceneView` is used**: These classes were the main entry points for Sceneform. Locating their usage pinpoints where the new `Session` and `ApplicationSubspace` will need to be integrated.
* **List all `Node` subclasses**: Sceneform's object-oriented approach encouraged subclassing `Node` (e.g., `TransformableNode`, `AnchorNode`) to add custom behavior. Each of these custom classes must be deconstructed and reimplemented using SceneCore's Entity-Component-System (ECS) pattern.
* **Locate all 3D model loading logic**: Find all instances of `ModelRenderable.builder()` or similar constructs. This logic will be replaced by the asynchronous `GltfModelEntity.load()` function in SceneCore.
* **Document all AR logic**: Catalogue all interactions with the AR state. This includes listeners for plane detection, direct calls to `session.createAnchor()`, and any use of `arFrame.hitTest()` for raycasting against the real world. This functionality will be migrated to the new `ARCore for Jetpack XR` APIs, accessed via the unified `Session` object.

#### Checklist 3: State Management and Business Logic

This checklist examines the application's architecture to identify how data flows and where state is managed. A clean separation of concerns is critical for a successful migration to a declarative UI framework.

* **Identify `ViewModel` classes**: List all `ViewModel`s and the `LiveData` or `StateFlow` objects they expose. This is the ideal state-holding pattern and should be preserved and expanded.
* **Trace data flow**: Follow the path of data from its source (network requests, local database) through any business logic layers to the UI. Understanding this flow is key to ensuring the new spatial UI is driven by a single source of truth.
* **Locate tightly coupled logic**: Search for business logic, state variables, or data manipulation that occurs directly within `Activity` or `Fragment` classes (or, in a Compose world, within `@Composable` functions themselves). This logic is a primary candidate for **state hoisting**—moving it out of the UI layer and into a `ViewModel` or other state holder. This is the most critical architectural cleanup to perform during the migration.

### 2.2 Configuring the Development Environment for Android XR

The Android XR SDK is currently in a developer preview phase, which means that setting up the development environment requires specific tools and configurations. Adhering to these requirements is not optional; it is essential for building and testing applications successfully.

#### Android Studio

The development environment must use the specific preview version of Android Studio designated for XR development. As of the current documentation, this is **Android Studio Narwhal Preview** or a newer compatible version. Using a stable, older version of Android Studio will likely result in missing templates, incompatible plugins, and build failures. The preview version includes essential tools like the updated Layout Inspector for spatial UI and the Android XR project template.

#### Android XR Emulator

For developers without access to physical Android XR hardware, the Android XR Emulator is an indispensable tool for development and testing.

* **Creation**: A virtual XR device can be created directly within Android Studio by navigating to `Tools` -> `Device Manager` -> `Create Virtual Device` and selecting the `XR` device category.
* **Controls**: The emulator provides a set of controls that allow navigation and interaction within a 3D virtual space using a standard keyboard and mouse, simulating head movement, controller positions, and button presses.
* **Limitations**: It is crucial to be aware of the emulator's current limitations. It is designed specifically for testing apps built with the **Jetpack XR SDK**. It does **not** support applications that use the native OpenXR C/C++ pathway or the Unity engine integration.

#### Project-Level Configuration

Configuring a project for Android XR involves adding specific dependencies to the `build.gradle.kts` file and declaring necessary features and permissions in the `AndroidManifest.xml`. The following table consolidates these requirements into a single, actionable checklist to minimize setup errors.

| File | Type | Entry | Purpose & Snippet Reference |
| :--- | :--- | :--- | :--- |
| `build.gradle.kts` | Dependency | `implementation("androidx.xr.scenecore:scenecore:1.0.0-alphaXX")` | Adds the Jetpack SceneCore library, the replacement for Sceneform, for managing and rendering 3D content. |
| `build.gradle.kts` | Dependency | `implementation("androidx.xr.compose:compose-ui:1.0.0-alphaXX")` | Adds the Jetpack Compose for XR libraries, which provide the spatial UI components like `Subspace` and `SpatialPanel`. |
| `build.gradle.kts` | Dependency | `implementation("androidx.xr.runtime:runtime-openxr:1.0.0-alphaXX")` | Adds the specific Jetpack XR Runtime implementation that is backed by OpenXR, required for running on physical devices. |
| `build.gradle.kts` | Dependency | `implementation("androidx.xr.arcore:arcore:1.0.0-alphaXX")` | Adds the ARCore for Jetpack XR library, which provides high-level APIs for world perception features like plane and hand tracking. |
| `AndroidManifest.xml` | `uses-feature` | `<uses-feature android:name="android.hardware.xr.spatial" android:required="true" />` | Declares to the system and the Play Store that the application requires spatial XR hardware capabilities to function. |
| `AndroidManifest.xml` | `property` | `<property android:name="android.window.PROPERTY_XR_ACTIVITY_START_MODE" android:value="XR_ACTIVITY_START_MODE_FULL_SPACE_MANAGED" />` | Configures the application's main activity to launch directly into the fully immersive `Full Space` mode by default. |
| `AndroidManifest.xml` | `uses-permission` | `<uses-permission android:name="android.permission.HAND_TRACKING" />` | Requests the necessary runtime permission to access hand tracking data from the perception system. |
| `AndroidManifest.xml` | `uses-permission` | `<uses-permission android:name="android.permission.SCENE_UNDERSTANDING_COARSE" />` | Requests permission for coarse-grained scene understanding, which enables features like plane detection and raycasting. |

## Part 3: The Refactoring Blueprint: A Phased Migration Strategy

This section outlines a structured, three-phase migration plan designed to refactor a legacy Android application onto the Android XR platform. This phased approach deconstructs the complex migration into a series of manageable, verifiable stages. It begins by establishing the core XR foundation, then moves to spatializing the user interface, and finally culminates in rebuilding the 3D and AR logic. This methodology is designed to deliver incremental value and allow for testing at each stage, reducing the overall project risk.

### 3.1 Phase 1: Establishing the Spatial Foundation

The goal of this initial phase is to bootstrap the application with the essential components for any XR experience. This involves integrating the core XR `Session`, managing its lifecycle, and creating the top-level spatial container that will house all subsequent UI and 3D content. This phase does not involve migrating any specific features but rather lays the architectural groundwork upon which all other features will be built.

#### Step 1: Implementing the Core XR `Session`

The `Session` object from the `androidx.xr.runtime` library is the central handle to the entire XR system. The first step is to create and manage an instance of this object within the application's main `Activity`. The `Session` lifecycle must be tightly coupled with the `Activity` lifecycle. The `Session.create()` factory method is used to initialize the session, while its `resume()`, `pause()`, and `destroy()` methods should be called within the corresponding `Activity` lifecycle callbacks (`onResume()`, `onPause()`, `onDestroy()`).

A significant improvement in the recent Jetpack XR Runtime releases is that the `Session` class now implements the `androidx.lifecycle.LifecycleOwner` interface. This enables a more robust and modern approach to lifecycle management. Instead of manually calling the session methods in each `Activity` callback, a custom `Lifecycle-aware component` (a class that implements `DefaultLifecycleObserver`) can be created. This component can observe the `Activity`'s lifecycle and automatically manage the `Session`'s state, leading to cleaner, more encapsulated, and less error-prone code.

#### Step 2: Defining Application Spaces (`Home Space` vs. `Full Space`)

With a `Session` in place, the application must declare which spatial environment it intends to operate in. While standard 2D apps will run by default in the windowed `Home Space`, a truly immersive application must request to enter the `Full Space`. This is achieved by calling `session.requestFullSpaceMode()`. This call is typically made either immediately upon application startup or in response to a user action, such as tapping a "Enter VR" button. Conversely, the application should also provide a mechanism to exit the immersive experience by calling `session.requestHomeSpaceMode()`, which returns the user to the multitasking `Home Space` environment. This logic establishes the fundamental mode of interaction for the user.

#### Step 3: Introducing the `ApplicationSubspace`

The final step in establishing the foundation is to define the primary container for all spatial content. In Jetpack Compose for XR, this is the `ApplicationSubspace` composable. Within the main `Activity`'s `setContent` block, the entire existing UI hierarchy should be wrapped inside this composable.

The `ApplicationSubspace` serves as the top-level partition of 3D space for the app. Any composables placed within it—such as `SpatialPanel`s or `Volume`s—will be rendered into the 3D scene when the application is running in an XR environment. A key feature of `ApplicationSubspace` is that its content is only processed and rendered when spatialization is enabled (i.e., in `Full Space` on an XR device). On a standard phone or tablet, or when the app is in `Home Space`, the content within the subspace is simply ignored. This provides a powerful, built-in mechanism for creating adaptive applications that can gracefully fall back to a 2D presentation without requiring separate code paths.

### 3.2 Phase 2: Migrating the User Interface with Jetpack Compose for XR

This phase focuses on translating the application's audited 2D user interface into a functional and intuitive spatial UI. Jetpack Compose for XR provides a spectrum of tools for this, from simple "lift-and-shift" porting of existing screens to building complex, multi-plane 3D layouts. The following table provides a direct mapping from legacy UI concepts to their new spatial equivalents, serving as a quick-reference guide for the development team.

| Legacy Component/Concept | Android XR Equivalent | Key Implementation Notes & Snippet Reference |
| :--- | :--- | :--- | :--- |
| `Activity`/`Fragment` Screen | `SpatialPanel` within a `Subspace` | A `SpatialPanel` is a 2D plane in 3D space that can host standard `@Composable` UI content. It is the primary tool for bringing existing 2D UIs into an XR environment. Must be called from within a `Subspace`. |
| `LinearLayout` (Vertical) | `SpatialColumn` | A `@SubspaceComposable` that functions like a `Column` but arranges its children vertically in 3D space. Used for creating spatial layouts. |
| `LinearLayout` (Horizontal) | `SpatialRow` | A `@SubspaceComposable` that functions like a `Row` but arranges its children horizontally in 3D space. Used for creating spatial layouts. |
| `ConstraintLayout` / `Modifier.offset` | `SubspaceModifier.offset`, `.rotate`, `.scale` | `SubspaceModifier` is the primary tool for fine-grained positioning, rotation, and scaling of spatial components, replacing complex 2D constraint systems with direct 3D transformations. |
| `BottomNavigationView` / `NavigationRail` | `Orbiter` | An `Orbiter` is a floating UI component, typically for navigation, that is anchored to a `SpatialPanel` or a spatial layout. It is the idiomatic replacement for screen-space navigation bars. |
| `Dialog` / `AlertDialog` | `SpatialDialog` | A spatial version of a dialog that appears in the 3D environment. It is built on the `ElevatedPanel` base component. |

#### Step 1: Porting 2D UI with `SpatialPanel`

The most direct path to spatializing an existing UI is to use the `SpatialPanel` composable. For each screen identified in the audit (whether it's an `Activity`'s content view or a `@Composable` screen function), the action is to wrap that entire UI component inside a `SpatialPanel`. For example: `ApplicationSubspace { SpatialPanel(modifier = SubspaceModifier.size(1024.dp, 720.dp)) { MyExisting2DUIScreen() } }`. This effectively takes the flat UI and places it on a virtual "canvas" in front of the user. It is a mandatory rule that `SpatialPanel`, being a `@SubspaceComposable`, must be called from within a `Subspace` context. This step allows for rapid porting and testing of existing user flows in the new XR environment.

#### Step 2: Constructing 3D Layouts

Once the basic UI is functional on a single panel, the next step is to create more sophisticated and immersive layouts by breaking the UI apart into multiple panels arranged in 3D space. Instead of one large `SpatialPanel`, the layout can be composed of several smaller panels positioned relative to one another using `SpatialRow` and `SpatialColumn`. These composables function like their 2D counterparts but operate in 3D space. Further refinement can be achieved using `SubspaceModifier`. For instance, `SubspaceModifier.offset(z = -50.dp)` can push a panel further away from the user to create a sense of depth, while `SubspaceModifier.rotate(y = 15f)` can angle a side panel towards the user for better ergonomics.

#### Step 3: Implementing Advanced Spatial UI

The final step in UI migration is to replace traditional, screen-locked UI patterns with their native spatial equivalents. For example, a `BottomNavigationView` should be replaced with an `Orbiter`. An `Orbiter` can be declared within a `SpatialPanel` to anchor it to that specific panel, or it can be declared directly within a spatial layout like a `SpatialRow`, in which case it will anchor to the layout itself. This component would typically contain `IconButton`s for navigation. Similarly, modal interactions that used `AlertDialog` should be migrated to use `SpatialDialog`, which presents a floating dialog in 3D space.

This migration path is enabled by a clever architectural design within Jetpack Compose for XR. There is a distinction between "Spatialized components" (like `Orbiter` and `SpatialDialog`) and "Subspace components" (like `SpatialPanel` and `SpatialRow`). Spatialized components are standard `@Composable` functions built upon a base called `ElevatedPanel`. `ElevatedPanel` works by creating a traditional 2D `ComposeView`, rendering the composable's content into it, and then wrapping that View in a SceneCore `PanelEntity` to place it in the 3D world. This allows them to be highly reusable. In contrast, Subspace components are annotated with `@SubspaceComposable` and are part of a native 3D layout system that can only exist inside a `Subspace`. This dual structure provides a smooth migration ramp, from the easy entry point of wrapping a 2D UI in a `SpatialPanel` to the more powerful, fully native 3D layouts.

### 3.3 Phase 3: Rebuilding the 3D and AR Experience

This phase addresses the migration of the core 3D and AR logic from the deprecated Sceneform library to the modern Jetpack SceneCore and ARCore for Jetpack XR libraries. This is the most technically demanding phase, as it involves a significant paradigm shift from Sceneform's object-oriented model to SceneCore's Entity-Component-System (ECS) architecture. The following cheatsheet is a critical resource for developers, providing direct translations from old concepts to new mechanisms.

| Sceneform Concept/Class | SceneCore Equivalent | Implementation Notes & Snippet Reference |
| :--- | :--- | :--- | :--- |
| `ArSceneView` / `ArFragment` | `Session` + `ApplicationSubspace` | There is no single view equivalent. The `Session` object manages the AR state and perception data, while `ApplicationSubspace` serves as the rendering container for all spatial content. |
| `Node` / `TransformableNode` | `Entity` + `MovableComponent` / `ResizableComponent` | SceneCore is ECS-based. An `Entity` is a simple ID. Behavior is added by attaching `Component`s. A `TransformableNode`'s behavior is replicated by adding `MovableComponent` and `ResizableComponent` to a generic `Entity`. |
| `ModelRenderable.builder().setSource(...)` | `GltfModelEntity.load(gltfSource)` | Model loading is now a suspend function called on the `GltfModelEntity` companion object. It should be invoked from within a coroutine. |
| `node.setParent(scene)` | `rootEntity.children.add(myEntity)` | The scene is a hierarchical graph of entities. The root entity of the scene is accessed via the `Session`, and child entities are added to its `children` collection. |
| `arFrame.hitTest(tap)` | `session.hitTest(ray)` | Hit testing against virtual content is now performed directly via a method on the core `Session` object. |
| `Plane` (from ARCore) | `Plane` (from ARCore for Jetpack XR) | Real-world plane detection data is now provided by the `ARCore for Jetpack XR` library and accessed through APIs on the `Session`. |
| `AnimationData` / `ModelAnimator` | `gltfModelEntity.startAnimation(name)` | Animations that are embedded within a GLB/glTF model file can be triggered by name using a simple function call on the `GltfModelEntity` instance. |

#### Step 1: Adopting the Entity-Component-System (ECS) Paradigm

The first and most crucial step is to refactor all Sceneform `Node` hierarchies into SceneCore `Entity` hierarchies. This requires a conceptual shift. Instead of creating a `MyCustomNode` class that inherits from `TransformableNode` and contains custom logic in its methods, the developer must now think in terms of composition. One would create a basic `Entity`, which is just an ID. To give it a position in the world, a `TransformComponent` is attached. To make it visible, a `RenderableComponent` is attached with a reference to a 3D model. To make it movable by the user, a `MovableComponent` is attached. This approach decouples data (Components) from logic (Systems), resulting in a more flexible and often more performant architecture.

#### Step 2: Managing 3D Assets

All 3D model loading logic must be updated. The Sceneform `ModelRenderable.builder()` pattern is replaced by the new asynchronous `GltfModelEntity.load()` suspend function. This function should be called from within a coroutine, which is typically launched using a `LaunchedEffect` or `rememberCoroutineScope` in a composable context. The loaded `GltfModelEntity` is a SceneCore `Entity` that is ready to be added to the scene graph. To integrate this 3D content with a Compose-based spatial UI, the loaded model should be placed inside a `Volume` composable. `Volume` is a special `@SubspaceComposable` that acts as a bridge, hosting a SceneCore `Entity` within the declarative Compose for XR layout hierarchy.

#### Step 3: Integrating World Perception

Finally, all AR-specific logic must be migrated to the new `ARCore for Jetpack XR` APIs. This centralizes AR functionality that was previously spread across different parts of the Sceneform and ARCore libraries. Plane detection, hit testing against real-world geometry, and the creation and management of persistent anchors are now all accessed through methods on the core `Session` object. This provides a unified and consistent API surface for all world-sensing capabilities. The migration also requires ensuring the `AndroidManifest.xml` contains the necessary permissions, such as `android.permission.SCENE_UNDERSTANDING_COARSE` for plane detection and `android.permission.HAND_TRACKING` for hand-based interactions.

---

### **Part 3.5: Navigating Common Migration Pitfalls and Debugging Runtime Issues**

While the phased blueprint represents the ideal path, the practical reality of migrating a legacy application often involves navigating a series of non-obvious runtime issues. This section documents common problems encountered when bridging older AR implementations with the modern Android XR stack, providing concrete diagnostic techniques and robust solutions.

#### **Pitfall 1: The Lifecycle Race Condition (`SessionPausedException`)**

A frequent and critical issue when integrating a traditional `GLSurfaceView` into Jetpack Compose is a lifecycle mismatch that results in a `com.google.ar.core.exceptions.SessionPausedException`.

* **Symptom**: The application crashes on startup or resume. Logcat reveals the renderer thread is attempting to call `session.update()` while the ARCore `Session` is in a `PAUSED` state.
* **Root Cause**: This is a race condition between two different lifecycle management systems. The `Activity`'s `onResume()` callback resumes the ARCore `Session`. Separately, the `GLSurfaceView` within the Compose UI has its own lifecycle, which, if managed by a `DisposableEffect`, is also resumed when the composable enters the screen. The timing of these two `resume` calls is not guaranteed to be synchronized. If the `Activity` is paused and resumed quickly, or due to other system events, the `GLSurfaceView`'s rendering thread may start and call `session.update()` before the `Activity` has had a chance to fully resume the session, leading to the crash.
* **Robust Solution**: The only way to guarantee the correct execution order is to centralize the lifecycle management. The `GLSurfaceView` instance must be owned by the `Activity`, not created within the composable. The `Activity`'s `onResume()` and `onPause()` methods should then explicitly call `onResume()` and `onPause()` on both the `Session` and the `GLSurfaceView` instances, in the correct order. The composable (`MainScreen`) is then simply passed the pre-initialized `GLSurfaceView` to be displayed via an `AndroidView`.

#### **Pitfall 2: Tracking Failures ("Not Scanning for Surfaces")**

A common developer experience is seeing the camera feed but no indication that ARCore is tracking the environment (i.e., no feature points or "dots" appear). This indicates the `Session` has failed to enter the `TRACKING` state.

* **Diagnostic Technique**: The most effective way to debug this is to add logging directly to the `ArRenderer.onDrawFrame` method. On every frame, log the camera's tracking status: `Log.d("AR_DEBUG", "State: ${camera.trackingState}, Reason: ${camera.trackingFailureReason}")`. This will immediately reveal *why* tracking is failing.
* **Common Failure Reasons & Solutions**:
  * `INSUFFICIENT_FEATURES`: The camera is pointed at a surface without enough texture (a blank white wall, a glossy table). The solution is to move the camera to view a more detailed area. The UI should display this instruction to the user.
  * `INSUFFICIENT_LIGHT`: The environment is too dark for the camera to see features. The UI should instruct the user to move to a brighter space.
  * `EXCESSIVE_MOTION`: The device is being moved too quickly. The UI should instruct the user to move the device more slowly.
* **Configuration-Based Solutions**: Logcat analysis can reveal deeper configuration issues.
  * **Depth Errors**: If the logs show `NOT_FOUND: Not able to find any depth measurements`, this indicates a failure in the internal motion tracking algorithm. This can often be resolved by explicitly setting a depth mode in the `Session` configuration: `config.depthMode = Config.DepthMode.AUTOMATIC`.
  * **Missing API Key**: A warning `The API key ... could not be obtained!` should be addressed. While not always the direct cause of tracking failure, it can prevent certain services from initializing correctly. The solution is to add the ARCore API Key to the `AndroidManifest.xml` via a `<meta-data>` tag.

#### **Pitfall 3: UI Layering and Event conflicts**

When overlaying Jetpack Compose UI on top of an AR view, conflicts can arise.

* **Event Interception**: A composable that covers the screen, such as `ModalNavigationDrawer`, can intercept touch events intended for the underlying AR view (`GLSurfaceView`). The solution is to ensure the correct UI hierarchy. The screen's primary content (including the AR view) must be placed within the `content` lambda of the `ModalNavigationDrawer`, not as a sibling to it.
* **Conditional UI Logic**: Complex `if-else if-else` chains used to show or hide UI based on state can easily lead to bugs. For instance, a button that is intended to toggle between "AR Mode" and "Manual Mode" might accidentally be hidden in one of those states due to a faulty condition. Simplifying conditional logic and carefully testing all state transitions is essential.

---

## Part 4: Advanced Implementation and Strategic Best Practices

### 4.1 State Management for a Declarative Spatial UI

The shift to a declarative UI paradigm with Jetpack Compose for XR makes a clean, predictable state management architecture not just a best practice, but a necessity. In a declarative system, the UI is a direct function of its state (`UI = f(state)`). Any time the state changes, the UI is recomposed to reflect that new state. If the state is inconsistent, duplicated, or managed improperly, the UI will inevitably become buggy and unpredictable. The migration process presents an ideal opportunity to enforce a modern, state-driven architecture.

The recommended approach is to adopt the standard patterns from modern Android development:

* **State Hoisting**: This is the fundamental principle of moving state "up" the composable tree from the components that use it to a common ancestor. All spatial composables, such as `SpatialPanel`, `Orbiter`, and custom UI elements, should be designed to be stateless. They should receive data to display as parameters and expose events as lambda functions (e.g., `onItemSelected: (Item) -> Unit`). This makes them highly reusable, testable, and decoupled from business logic.
* **ViewModel and Single Source of Truth**: The hoisted state should reside in a lifecycle-aware `ViewModel`. The `ViewModel` becomes the single source of truth for the screen's state, responsible for holding the data, handling business logic, and processing user events. It should expose the UI state to the composables using an observable data holder, with `kotlinx.coroutines.flow.StateFlow` being the recommended choice for new projects.
* **Unidirectional Data Flow (UDF)**: By combining state hoisting and ViewModels, a UDF pattern is established. State flows down from the `ViewModel` to the composables, and events flow up from the composables to the `ViewModel`. In the UI layer, composables should collect the `StateFlow` using the `collectAsStateWithLifecycle()` extension function. This function is lifecycle-aware, automatically subscribing and unsubscribing to the flow, and it converts the flow into a `State<T>` object that Compose can observe. When the `ViewModel` updates the `StateFlow`, Compose automatically triggers a recomposition of only the composables that read that specific state, ensuring efficient and correct UI updates.

### 4.2 Architecting for Multimodal Input

XR environments are inherently multimodal, supporting a richer set of input methods than traditional mobile devices. The application's architecture must be designed to gracefully handle this variety. Android XR supports input from 6DoF motion controllers, direct hand tracking (including gestures like pinching), eye gaze interaction for selection, and even traditional mouse and keyboard input, which is particularly relevant for the emulator.

A flexible input architecture should distinguish between different types of interaction:

* **UI Input**: For interactions with standard 2D UI elements hosted on a `SpatialPanel`, the existing Jetpack Compose input modifiers work as expected. Modifiers like `Modifier.clickable`, `Modifier.draggable`, and the more advanced `Modifier.pointerInput` can be used to handle taps, drags, and other pointer-based events from controllers or a mouse.
* **Direct 3D Interaction**: For direct manipulation of 3D objects in the scene or for implementing custom gestures, the application will need to interface with lower-level APIs. The `ARCore for Jetpack XR` library, for instance, exposes detailed hand tracking data, providing the positions of 26 hand joints when the `HAND_TRACKING` permission is granted. This raw data can be consumed by the application to build custom gesture recognizers (e.g., detecting a thumbs-up or a pointing gesture).
* **Centralized Event Handling**: To maintain a clean architecture, input events should be hoisted up from the UI layer. A composable that detects a pinch gesture should not contain the logic for what that pinch does. Instead, it should call a lambda function passed in as a parameter (e.g., `onPinch: () -> Unit`). The `ViewModel` or a state holder implements the logic for this event, decoupling the input modality from the resulting action. This allows the same action to be triggered by different inputs (e.g., a controller button press or a hand pinch) without duplicating code.

### 4.3 Performance, Debugging, and Optimization in XR

Performance in XR is paramount. Unlike traditional apps where a stutter might be a minor annoyance, dropped frames in an immersive experience can break the sense of presence and even induce motion sickness in the user. Therefore, continuous monitoring and optimization are critical.

* **Performance Tools and Metrics**: The Android XR platform provides tools for performance analysis. Through OpenXR extensions, an application can query runtime performance metrics, including CPU and GPU frame times, hardware utilization percentages, and the current frames per second (FPS). This data is invaluable for identifying bottlenecks. For UI layout issues, the **Layout Inspector** in the preview versions of Android Studio has been updated to support spatialized UI components. It allows developers to visualize the 3D hierarchy of `SpatialPanel`s and other elements, making it possible to debug complex spatial layouts that would otherwise be opaque.
* **Optimization Techniques**:
  * **Compose Recomposition**: Standard Jetpack Compose performance best practices are directly applicable and even more crucial in XR. This includes ensuring that state objects passed to composables are stable (preferably immutable data classes) to allow Compose to intelligently skip recompositions. For state that is derived from other state, using `remember { derivedStateOf {... } }` can prevent expensive recalculations on every frame.
  * **Lazy Layouts**: When displaying long, scrollable lists of content within a `SpatialPanel`, it is essential to use `LazyColumn` or `LazyRow`. These composables perform "virtualization," meaning they only compose and render the items currently visible to the user, preventing the performance cost of rendering hundreds or thousands of items at once.
  * **Leveraging Hardware Features**: The underlying OpenXR platform exposes powerful hardware-level performance features that applications can leverage. These include **eye-tracked foveated rendering**, a technique that renders the scene at high resolution only at the user's focal point and at lower resolution in the periphery, significantly reducing the GPU load. Another key feature is **SpaceWarp**, which uses motion vectors and depth data to computationally generate intermediate frames, effectively doubling the perceived frame rate and smoothing out a performance hiccups. While these are lower-level features, their support in the runtime is a key benefit of the platform.

## Conclusion and Strategic Roadmap

The migration of a legacy Android application to the Android XR platform is more than a simple technical update; it is a strategic repositioning of the application for the next generation of computing. The refactoring process detailed in this blueprint outlines a transition from the imperative and object-oriented paradigms of Android Views and Sceneform to the declarative, data-driven, and component-based architectures of Jetpack Compose for XR and Jetpack SceneCore. This is a fundamental shift that yields a more modern, maintainable, and robust codebase.

The core of this migration involves embracing three key architectural transformations:
1.  **A Shift to a Declarative, Spatial UI**: By adopting Jetpack Compose for XR, development moves from manually manipulating UI widgets to describing the UI as a function of state. This results in more predictable and less error-prone user interfaces that can be easily adapted from 2D screens to immersive 3D layouts using components like `SpatialPanel` and `Orbiter`.
2.  **Adoption of an Entity-Component-System**: Replacing Sceneform's inheritance-based `Node` system with SceneCore's ECS model decouples data from logic. This fosters greater flexibility and prepares the application for the performance demands of complex 3D scenes.
3.  **Alignment with an Open, Future-Proof Platform**: Building on a foundation of Jetpack, Android XR, and the OpenXR standard ensures the application is not tied to a single proprietary ecosystem. This commitment to open standards future-proofs the investment, guaranteeing compatibility with a growing ecosystem of hardware from diverse manufacturers and aligning the application with the strategic direction of the entire Android platform.

By following the phased migration plan—establishing the core `Session`, spatializing the UI, and rebuilding the 3D/AR logic—development teams can systematically tackle this complex transition. The process provides a crucial opportunity to enforce modern architectural best practices, particularly a clean, unidirectional data flow with `ViewModel`s as the single source of truth, which is essential for the stability of a declarative UI.

Looking forward, this architectural transformation positions the application to seamlessly integrate future advancements within the Android XR ecosystem. As Google continues to enhance the platform, features such as the direct integration of on-device Gemini AI models for intelligent interaction, more sophisticated world-sensing capabilities, and support for new input modalities can be incorporated into this clean, modular architecture with greater ease. The result of this refactoring effort is not merely a ported application, but an application that is architecturally sound, aligned with industry standards, and strategically prepared to thrive in the evolving landscape of spatial computing.