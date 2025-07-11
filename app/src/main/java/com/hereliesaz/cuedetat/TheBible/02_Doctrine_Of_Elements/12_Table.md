# The Table

* **Visibility:** The table is hidden by default. Its visibility is controlled by the `"Toggle Table"` `TextButton` in the menu and its dedicated `FloatingActionButton` when not visible.
* **Rotation:** The table's default rotation when shown is 90 degrees (portrait). Its rotation is controlled by both the horizontal rotation slider and a two-finger rotation gesture.
* **Pivot Point:** The table must rotate around its logical center (0,0).
* **Table Size**:
* The application must support 6', 7', 8', 9', and 10' table sizes.
* The default size is 8' Table until the user selects differently.
* The user's last selected size must be persisted across application sessions.
* A display in the top-right corner must indicate the current table size (e.g., "8'"). Tapping this display cycles through the available sizes.
* A menu option ("Table Size") must open a dialog allowing the user to select a specific size directly.
* **Proportionality**: The logical size of the table and its components (rails, pockets) **must** be derived from the logical size of the cue ball. The `TableSize` enum provides a `getTableToBallRatioLong()` function which is the single source of truth for this proportion. This ensures that as the ball's logical radius changes with zoom, the table scales with it perfectly.
* **Ball Confinement**: When the table is visible, all interactive balls (`ActualCueBall`, `TargetBall`, `BankingBall`) **must** be constrained to the logical boundaries of the playing surface.
* **Color-Based Detection**: The system can use color segmentation to isolate the table felt. It will continuously attempt to auto-detect the felt color by sampling the center of the view. A "Lock Color" button allows the user to fix the currently sampled color for more stable detection in varied lighting.

***
## Addendum: Detailed Table Specifications

* **Logical Dimensions**: The table's logical playing surface dimensions are determined by the selected table size and are proportional to the logical radius of the cue ball, maintaining realistic aspect ratios.
* **Rendering**: The table is rendered as a wireframe.
* The playing surface outline and pockets are drawn on the main `pitchMatrix`.
* The rails, along with their diamond markers, are drawn on the separate `railPitchMatrix` to give them a "lifted" 3D appearance.
* **Pockets**: Pockets are rendered as stroked circles at the corners and midpoints of the long rails. Their logical radius is proportional to the cue ball's radius (specifically, `1.8f` times the radius). They must be filled with black, but turn pure white when a shot is aimed or banked into them.
* **Diamonds**:
* Diamond markers must be rendered on the rails. They must be pure **white**.
* The side pockets are to be counted as diamonds in the numbering sequence.
* When in Banking Mode or when a bank is previewed in Protractor Mode, the precise point of impact on a rail must be marked.
* A text label must be displayed **directly at** this impact point, indicating the diamond number with one decimal place of precision (e.g., "1.3"). The numbering is clockwise, starting from the top-left corner pocket as diamond 0.
* **Head Spot**: The `ActualCueBall`, when the table is first shown, must be placed on the head spot, which is geometrically defined as being halfway between the center of the table and the bottom rail.