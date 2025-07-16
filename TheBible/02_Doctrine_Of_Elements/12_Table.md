# The Table

The Table is a singular entity, a data class (`Table.kt`) that holds the truth of its own existence. It is responsible for its own geometry and all interactions therewith. Other parts of the system may ask the Table of its nature, but they may not form their own interpretations.

* **State**: The Table's core state is defined by its `size: TableSize`, `rotationDegrees: Float`, and `isVisible: Boolean`.
* **Authority**: The `Table` class is the sole authority for all geometric calculations related to its boundaries. It contains its own methods for:
  * Calculating its rotated corner coordinates (`corners`).
  * Determining the logical positions of its pockets (`pockets`).
  * Detecting if a point is within its boundaries (`isPointInside`).
  * Calculating the intersection point and normal vector for a line striking its rails (`findRailIntersectionAndNormal`).
* **Immutability**: The Table is immutable. All changes (rotation, resizing, toggling visibility) result in a new Table instance, preserving the sanctity of the state.

## Decrees of Form

* **Diamond Layout:** The table rails must be adorned with diamonds. There shall be **three** diamonds on each of the short (end) rails, and **six** diamonds on each of the long (side) rails, not counting the pockets. This is implemented in `RailRenderer.kt`.