# The Table

The Table is a singular entity, a data class (`Table.kt`) that holds the truth of its own existence. It is responsible for its own geometry and all interactions therewith. Other parts of the system may ask the Table of its nature, but they may not form their own interpretations.

* **State**: The Table's core state is defined by its `size: TableSize`, `rotationDegrees: Float`, `isVisible: Boolean`, and the `referenceRadius` from which its logical scale is derived.
* **Authority**: The `Table` class is the sole authority for all geometric calculations related to its boundaries. It contains its own methods for:
  * Calculating its rotated corner coordinates (`corners`).
  * Determining the logical positions of its pockets (`pockets`).
  * Detecting if a point is within its boundaries (`isPointInside`).
  * Calculating the intersection point and normal vector for a line striking its rails (`findRailIntersectionAndNormal`).
* **Immutability**: The Table is immutable. All changes (rotation, resizing, toggling visibility) result in a new Table instance, preserving the sanctity of the state.

## Old Mandates (Now Enforced by Architecture)

* **Positioning & Movement:** The Table's center is architecturally bound to the logical plane's origin (0,0).
* **Pivot Point:** The Table rotates around its logical center (0,0).
* **Proportionality**: The logical size of the table is derived from the `referenceRadius` of the balls, ensuring perfect scaling.
* **Ball Confinement**: The `snapViolatingBalls` function now asks the one true `Table` for its boundaries, ensuring balls are always constrained to the visible, rendered table.
* **Rendering Doctrine:** All renderers (`TableRenderer`, `RailRenderer`, etc.) **must** derive their geometry from the pre-rotated `corners` provided by the `Table` object. They shall not assume a default (0-degree) orientation and apply their own rotations.