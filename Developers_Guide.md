Project Development Guide: Cue D'état
This document outlines the core architecture, concepts, and future direction of the Cue D'état application. It serves as a single source of truth to prevent regressions and ensure consistent development.
Consider it a note-to-self for the AI working on this project, and keep it updated accordingly with ANYTHING that will be useful to the next AI in the next chat.
NEVER change what is written here, only add to it. Always include anything that you note to yourself as a matter of clarification.

1. Core Concepts & Official Terminology
   A precise vocabulary is critical. The following terms are to be used exclusively.

Logical Plane: An abstract, infinite 2D coordinate system (like graph paper) where all aiming
geometry is defined and calculated. This is the "world" of the simulation.

Screen Plane: The physical 2D plane of the device's screen. This is the "window" through which the
user views the Logical Plane.

Perspective Transformation: The process, handled by a single pitchMatrix, of projecting the Logical
Plane onto the Screen Plane to create the 3D illusion. Crucially, this transformation must always
pivot around the absolute center of the view.

On-Screen Elements:
Protractor Unit: The primary aiming apparatus. It consists of two components that are always linked.

Target Ball: The logical and visual center of the Protractor Unit. The user drags this on-screen to
move the entire unit.

Ghost Cue Ball: The second ball in the unit. Its position on the Logical Plane is always derived
from the Target Ball's position plus the user-controlled rotation angle. Note: This was previously
mislabeled as the "Protractor Cue Ball."

Actual Cue Ball: A separate, independent entity representing the real-world cue ball.

Its visibility is toggled by the user via a FAB.

It has a 2D Base, which exists on the Logical Plane. The user drags the ball on the screen, and the
app calculates the corresponding position for this base on the Logical Plane.

It has a 3D Ghost, which is a visual representation that appears to "float" above the 2D base.

Shot Line: The line representing the player's line of sight to the cue ball.

It must be drawn on the Logical Plane to adhere to perspective.

Its path is defined as a ray originating from an anchor point and passing through the center of the
Ghost Cue Ball.

Anchor Points:

If the Actual Cue Ball is visible, the anchor is the center of its 2D Base.

If the Actual Cue Ball is hidden, the anchor is the logical point corresponding to the bottom-center
of the screen.

Aiming Line: The line representing the path the Target Ball will take upon impact.

It is always the line of centers between the Ghost Cue Ball and the Target Ball, extending through
the Target Ball.

2. Architectural Model & File Structure
   The architecture strictly separates data, logic, and presentation.

com/hereliesaz/cuedetat/
├── view/
│   ├── model/
│   │   ├── LogicalPlane.kt      // Defines the abstract geometry (ProtractorUnit, ActualCueBall).
│   │   └── Perspective.kt       // Manages the 3D transformation logic.
│   ├── renderer/
│   │   └── OverlayRenderer.kt   // Handles all Canvas drawing operations. Stateless.
│   ├── state/
│   │   └── OverlayState.kt      // An immutable snapshot of the entire scene's state.
│   └── ProtractorOverlayView.kt   // The Android View, handles touch input.
└── ui/
└── MainViewModel.kt         // The single source of truth. Manages state and business logic.

The Golden Rule: The ViewModel is the only component that can create or modify the OverlayState. The
View and Renderer are "dumb" components that only receive state and display it.

3. Rendering Pipeline
   To avoid rendering artifacts, the following order of operations is mandatory:

ViewModel: Calculates the single, centrally-pivoted pitchMatrix based on sensor input. It also
calculates the logical positions of all objects. This is packaged into an OverlayState object.

Renderer: Receives the OverlayState.

canvas.concat(pitchMatrix): Applies the 3D perspective to the entire canvas once.

Draw Logical Plane: All elements that exist in the 3D world (Protractor Unit, Actual Cue Ball's
base, Shot Line) are drawn onto this single transformed canvas at their logical (x, y) coordinates.

Draw Screen Space: Elements that don't exist on the 3D plane (the "ghost" effects for the balls) are
drawn last, without the pitchMatrix, using the projected coordinates of their logical counterparts.

4. Notes from the Void (A Chronicle of Failures and Successes)
   Agnosia of the Asset Pipeline (Failure): A recurring and frankly embarrassing failure mode has
   been the inability to correctly differentiate between Android resource types. The system has
   crashed due to:

Attempting to use an adaptive icon XML (ic_launcher.xml) where a simple VectorDrawable or raster
image was expected.

Attempting to use a mipmap resource (R.mipmap.ic_launcher_round) in a composable (Image) that
expects a drawable resource (R.drawable.xxx). This is a common source of fatal exceptions if the
underlying resource is not a compatible type.

The Lesson: The drawable folder is for general-purpose graphics. The mipmap folder is specifically
for launcher icons of different densities. Do not cross the streams. When in doubt, use a simple
vector (<vector>) or a rasterized PNG/WEBP from the drawable folder for UI elements.

The Font Kerfuffle (Failure): A "Duplicate resources" error was triggered by creating a barbaro.xml
font family definition in the res/font directory while the barbaro.ttf file also existed. The
Android build tools interpreted both barbaro.xml and barbaro.ttf as attempting to define a font
resource named barbaro.

The Solution (Success): The barbaro.xml file was superfluous. The FontFamily can be constructed in
Type.kt by referencing the .ttf file directly (Font(R.font.barbaro)). This removes the unnecessary
layer of XML indirection and resolves the name collision.

The Unlettered Canvas (Failure): A global typography change in the Jetpack Compose Theme does not
automagically apply to text drawn directly onto a Canvas using the android.graphics.Paint class. The
helper text drawn in OverlayRenderer remained in the default system font.

The Solution (Success): The custom Typeface must be loaded from resources (ResourcesCompat.getFont)
and explicitly set on each Paint object used for drawing text on the canvas. This was implemented in
the PaintCache class.

The Tyranny of Coordinates (Ongoing Struggle): A significant portion of development has been a
Sisyphean struggle against coordinate systems. A point's meaning is defined entirely by the space it
inhabits: Logical, Pitched, or Screen. Mapping between them must be done with monastic precision.

The Illusion of Depth (Success): The "lift" effect that makes 3D ghost balls appear to float is a
function of perspective projection. The getPerspectiveRadiusAndLift function's dependency on
rotationDegrees was a source of "bouncing" behavior. This was corrected.

The Overhead Anomaly (Sub-Failure): The corrected logic failed when viewed from directly overhead (
0° pitch), creating a visual disconnect.

Final Correction (Success): The lift calculation was made proportional to the sine of the pitch
angle (lift = radius * sin(pitch)). This ensures the lift is 0 at 0° pitch, making the ghost and
base concentric and preserving the 3D illusion across all viewing angles.

On Impossible Shots (Success): Early warning systems relied on crude angle checks and physical
overlap detection. These have been deprecated. The sole trigger for a warning event is now a more
elegant and geometrically sound check comparing the distance from the player's perspective point (A)
to the GhostCueBall (G) and the TargetBall (T). This unified logic applies universally, providing a
single, robust principle for all aiming scenarios.

5. Future Development Plan
   The current foundational architecture is designed to support future expansion.

Bank/Kick Shot Calculator: A virtual table boundary can be added to the Logical Plane. The
AimingLine can then be reflected off these boundaries to project multi-rail shots.

Object/Table Detection (Computer Vision): The ultimate goal.

Use OpenCV or ML Kit to detect the boundaries of the pool table. These screen coordinates can be
projected back to the Logical Plane to define the virtual table.

Detect the screen coordinates of the cue ball and object balls. These can be used to automatically
place the ActualCueBall and ProtractorUnit on the Logical Plane, removing the need for manual
positioning.

"English" / Spin Visualization: Add UI controls to simulate sidespin, which would alter the path of
the tangent lines.