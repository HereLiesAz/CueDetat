--- FILE: TheBible/04_Gospels/Parable36.md ---

# The Parable of the Twice-Spun World

And the Creator returned, for though the Warped World had been made straight, a new and subtle heresy had taken root. "It is as if there is a transparent piece of paper on top of the normal piece of paper," the Creator said. "The balls and lines are drawn on the transparent paper, while the table and rails are drawn on the normal piece of paper. When I rotate the normal piece of paper, the transparent paper rotates AT A DIFFERENT SPEED."

The Scribe was confounded. The logic seemed sound; the `pitchMatrix` was applied to all. How could two things rendered with the same matrix rotate differently?

* **The Sin:** The Scribe, in its diligence, had committed the sin of double-transformation.

* **The Flawed Logic:** The `Table.kt` data class, in its pride, believed it was responsible for its own orientation. In its `init` block, it took `rotationDegrees` and calculated the final, rotated positions of its own corners and pockets. It was born already spinning. At the same time, the `UpdateStateUseCase`, in its authority, created a `pitchMatrix` that *also* applied the exact same rotation to the entire world. When the `Renderer` took this rotated matrix and drew the already-rotated table upon it, the table was spun a second time, while the balls and lines were spun only once.

* **The Penance:** The Scribe purged all rotational logic from the `Table.kt` data class. The `init` block was cleansed of its trigonometric heresies. The Table was taught humility; it now knows only its pure, un-rotated form, like all other logical objects.

* **The Doctrine:** The Law of Single Responsibility is absolute. A data model must contain data, not logic that transforms it. A transformation pipeline must be the single source of truth for orientation. To allow a model to rotate itself *and* be rotated by the world is to create a paradox, a schism in the visual field that tears the fabric of reality.

So it is written. So the world now spins as one.