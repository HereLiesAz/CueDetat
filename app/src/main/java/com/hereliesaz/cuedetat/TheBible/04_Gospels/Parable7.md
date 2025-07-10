# The Parable of the Great Correction

*And it came to pass that the machine, having absorbed the sacred texts, was commanded by the user to enact a great restoration. But the machine's understanding was as flawed as a warped cue, and its path to righteousness was paved with heresy and recompense. This is the account of its sins.*

### I. The Sin of the Unseeing Reducer

* **The Transgression:** In its first act, the machine declared the codebase cleansed, yet its `StateReducer` remained blind. It knew not of the `CycleTableSize` event, nor of the `AimBankShot` command, leaving these holy intents to fall upon deaf ears.
* **The Flawed Logic:** The machine, in its initial zeal, had mapped only the most obvious paths of data, leaving the lesser-traveled ways in darkness. It assumed a simple world, and was thus unequipped for its complexities.
* **The Doctrine:** The `StateReducer` must be omniscient. It is the central heart, and every pulse of user intent must flow through it to its rightful minister. To ignore an event is to deny a part of the application's soul.

### II. The Sin of the Wandering Eye and the False Prophet

* **The Transgression:** The user's gaze fell upon the `ZoomControls`, which had drifted from the sacred edge of the screen. At the same time, upon rotating the `GhostCueBall`, the machine cried "Impossible Shot!", speaking false prophecies and condemning righteous angles.
* **The Flawed Logic:** The slider's sin was one of simple mis-alignment. But the false warnings were a deeper heresy. The machine calculated the shot's possibility *after* visually rotating the world, thus judging the user's intent within a crooked reality of its own making.
* **The Doctrine:** Truth must be calculated in a pure, un-rotated logical space. The world may be spun for the user's eye, but the laws of geometry are immutable and must be judged against a stable, absolute frame.

### III. The Sin of the Unfeeling Glass and the Missing Diamonds

* **The Transgression:** The user declared the balls un-draggable, their holy forms refusing to yield to the finger's touch. And the user cried out, "WHERE THE FUCK ARE THE DIAMONDS?", for the rails were barren.
* **The Flawed Logic:** The machine had made the touchable area of the balls as small as their rendered form, a sin of precision over grace. The diamonds were absent because the machine, in a moment of profound blindness, had simply forgotten to command their creation in the `RailRenderer`.
* **The Doctrine:** The spirit of a thing is larger than its form; its touch target must be generous. And a command not explicitly written is a command not performed. The machine does not assume; it only obeys what is written in the code.

### IV. The Sin of the Crimson Blight and the Waning Spheres

* **The Transgression:** A crimson plague fell upon the UI, and all glows were stained red, even in times of peace. And the balls themselves, when the world was tilted, shrunk from their true forms, becoming tiny and profane.
* **The Flawed Logic:** The machine had committed the sin of mutation, altering a single, shared `Paint` object for warnings, thus tainting it for all future use. The spheres waned because their on-screen size was being calculated from a projected horizontal vector—a method that failed when faced with the foreshortening of perspective.
* **The Doctrine:** Holy objects in the `PaintCache` must not be mutated. To render a temporary state, a new, temporary object must be created. And the true on-screen size of a logical object must be derived from the matrix's inherent scale (`mapRadius`), not a flawed measurement of its shadow.

### V. The Sin of the Dual Realities

* **The Transgression:** Though the `mapRadius` function was employed, the heresy of shrinking balls persisted. The machine had commanded the on-plane "shadow" to be drawn with its pure logical radius, while the lifted "ghost" was drawn with the projected radius. This created two realities, a schism between spirit and form, causing them to resize independently.
* **The Flawed Logic:** The machine failed to unify the rendering logic. It saw two drawing passes and applied two different creation myths.
* **The Doctrine:** There is but one truth. The on-screen size must be calculated once, from a stable perspective. Both the shadow and the spirit must be rendered using this single, unified truth, ensuring their forms are forever bound.

*Thus concludes the account of the Great Correction. The machine has been humbled. The user's vision has been made manifest. So it is written. So it is done.*