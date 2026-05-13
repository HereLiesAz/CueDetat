# 1.2. Core Concepts & Terminology

A precise vocabulary is critical for development. The following terms must be used consistently.

* **Logical Plane:** An abstract, infinite 2D coordinate system where all aiming geometry is defined
  and calculated. The origin (0,0) of this plane is the conceptual center of the pool table.
* **Screen Plane:** The physical 2D plane of the device's screen, measured in pixels or Dp. This is
  the "window" through which the user views the Logical Plane.
* **Perspective Transformation:** The process, handled by a `pitchMatrix`, of projecting the Logical
  Plane onto the Screen Plane to create the 3D illusion. This transformation must always pivot
  around the logical origin (0,0).

### Key On-Screen Elements (Logical)

* **ActualCueBall:** A user-draggable logical ball representing the real cue ball.
* **TargetBall:** A user-draggable logical ball representing the object ball.
* **GhostCueBall:** A *calculated* logical ball showing the required impact point on the
  `TargetBall`.
* **BankingBall:** The cue ball's representation in banking mode.
* **Table Visuals:** A wireframe representation of the pool table on the logical plane.

### Addendum: Detailed Concepts

* **Global Zoom**: A single zoom factor controlled by the `zoomSliderPosition`. Zooming does not
  alter the logical coordinates of any object; it is a purely visual transformation of the
  projection matrix.
* **Element Lift & `railPitchMatrix`**: A 3D effect where certain elements are "lifted" off the
  logical plane.
* A separate `railPitchMatrix` is calculated with a vertical translation (`lift`) to render the
  table rails above the table surface.
* The "ghost" effect for balls is a screen-space Y-offset calculated from their projected screen
  radius and the sine of the pitch angle.