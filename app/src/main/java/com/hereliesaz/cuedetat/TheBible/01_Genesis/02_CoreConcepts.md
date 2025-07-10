# 01: Core Concepts & Terminology

A precise vocabulary is critical. The following terms are to be used exclusively.

* **Logical Plane:** An abstract, infinite 2D coordinate system (like graph paper) where all aiming geometry is defined and calculated. This is the "world" of the simulation. The origin (0,0) of this plane is conceptually at the center of the pool table.

* **Screen Plane:** The physical 2D plane of the device's screen. This is the "window" through which the user views the Logical Plane.

* **Perspective Transformation:** The process, handled by a `pitchMatrix`, of projecting the Logical Plane onto the Screen Plane to create the 3D illusion. This transformation must always pivot around the logical origin (0,0), which is then mapped to the center of the screen view.

* **On-Screen Elements:**
  * **ActualCueBall:** A user-draggable logical ball representing the real cue ball.
  * **TargetBall:** A user-draggable logical ball representing the object ball.
  * **GhostCueBall:** A *calculated* logical ball showing the required impact point on the Target Ball.
  * **BankingBall:** The cue ball's representation in banking mode.
  * **Table Visuals:** A wireframe representation of the pool table.

So marketh the divine motherfucking words of the motherfucking LORD YOUR GOD.

***
## Addendum: Detailed Concepts

* **Global Zoom**: A single zoom factor, controlled by the vertical `ExpressiveSlider` (`zoomSliderPosition`) and mapped via `ZoomMapping.kt`. This factor determines the base logical radius for primary interactive elements like the `ProtractorUnit.radius` and `OnPlaneBall.radius`, ensuring their logical size is consistent relative to the view. **Crucially, zooming does not alter the logical coordinates of any object.** It is a purely visual transformation of the projection.

* **Element Lift & `railPitchMatrix`**: To create a 3D effect, some elements are "lifted" off the logical plane.
  * A separate `railPitchMatrix` is calculated with a vertical translation (`lift`) to render the table rails above the table surface. This lift amount is proportional to the table's logical short-side dimension.
  * The 3D "ghost" effect for balls in Protractor Mode is a screen-space effect. Their Y-offset is calculated from their projected screen radius and the sine of the pitch angle (`radius * sin(pitch)`). This creates the illusion of floating above the plane.