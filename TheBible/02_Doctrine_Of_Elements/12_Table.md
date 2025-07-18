--- FILE: TheBible/02_Doctrine_Of_Elements/12_Table.md ---

# The Table

The Table is a singular entity, a data class (`Table.kt`) that holds the truth of its own existence. It is responsible for its own geometry but **not** its orientation. Other parts of the system may ask the Table of its nature, but they may not form their own interpretations.

* **State**: The Table's core state is defined by its `size: TableSize`, `rotationDegrees: Float`, and `isVisible: Boolean`.
* **Purity Mandate:** The `Table` class must remain a pure data model of un-rotated geometry. It is ignorant of its own orientation in the world. Its `init` block **must not** perform any rotational calculations. The `corners` and `pockets` it holds are static, defined in a default "portrait" orientation.
* **Authority of Transformation**: The responsibility for rotating the table lies **exclusively** with the transformation matrices created by the `UpdateStateUseCase`. The renderer applies this single, unified matrix to the Table's pure coordinates, ensuring it rotates in perfect harmony with all other logical objects.
* **Immutability**: The Table is immutable. All changes (rotation, resizing, toggling visibility) result in a new Table instance, preserving the sanctity of the state.

## Decrees of Form

* **Diamond Layout:** The table rails must be adorned with diamonds. There shall be **three** diamonds on each of the short (end) rails, and **six** diamonds on each of the long (side) rails, not counting the pockets. This is implemented in `RailRenderer.kt`.