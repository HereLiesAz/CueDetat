# The Table

The Table is a singular entity, a data class (`Table.kt`) that holds the truth of its own existence. It is responsible for its own geometry and all interactions therewith. Other parts of the system may ask the Table of its nature, but they may not form their own interpretations.

* **State**: The Table's core state is defined by its `size: TableSize`, `rotationDegrees: Float`, and `isVisible: Boolean`.
* **Authority**: The `Table` class is the sole authority for all geometric calculations related to its boundaries. It contains its own methods for:
  * Calculating its un-rotated corner coordinates (`unrotatedCorners`).
  * Determining the logical positions of its un-rotated pockets (`unrotatedPockets`).
  * Detecting if a point is within its boundaries (`isPointInside`), correctly accounting for its rotation.
  * Calculating the intersection point and normal vector for a line striking its rails (`findRailIntersectionAndNormal`), correctly accounting for its rotation.
* **Immutability**: The Table is immutable. All changes (rotation, resizing, toggling visibility) result in a new Table instance, preserving the sanctity of the state.

## Decrees of Form

* **Diamond Layout:** The table rails must be adorned with diamonds. There shall be **three** diamonds on each of the short (end) rails, and **six** diamonds on each of the long (side) rails, not counting the pockets. This is implemented in `RailRenderer.kt`.
* **Doctrine of Pure Geometry**: The `Table` model **must not** perform its own rotation calculations on its coordinates. It must only store its base, un-rotated geometry. All visual rotation must be handled exclusively by the `Canvas` transformation matrix provided by the `UpdateStateUseCase`.