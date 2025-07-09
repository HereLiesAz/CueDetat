# 16: The Table

* **Visibility:** The table is hidden by default. Its visibility is controlled by the `"Toggle Table"` `TextButton` in the menu.
* **Rotation:** The table's default rotation is 90 degrees. Its rotation is controlled by the horizontal rotation slider, which is only visible when the table is visible.
* **Pivot Point:** The table must rotate around its logical center (0,0).
* **Table Size**: The application must support 6', 7', 8', and 9' table sizes. The default size is 7'. The user's last selected size must be persisted across application sessions.

***
## Addendum: Detailed Table Specifications

* **Logical Dimensions**: The table's logical playing surface dimensions are determined by the selected table size and are proportional to the logical radius of the cue ball, maintaining realistic aspect ratios.
* **Rendering**: The table is rendered as a wireframe.
  * The playing surface outline and pockets are drawn on the main `pitchMatrix`.
  * The rails, along with their diamond markers, are drawn on the separate `railPitchMatrix` to give them a "lifted" 3D appearance.
* **Pockets**: Pockets are rendered as stroked circles at the corners and midpoints of the long rails. Their logical radius is proportional to the cue ball's radius (specifically, `1.8f` times the radius).
* **Diamonds**:
  * Diamond markers must be rendered on the rails.
  * The side pockets are to be counted as diamonds in the numbering sequence.
  * When in Banking Mode, the precise point of impact for a bank shot on a rail must be marked.
  * A text label must be displayed above this impact point, indicating the diamond number with one decimal place of precision (e.g., "1.3"). The numbering is clockwise, starting from the top-left corner pocket as diamond 0.