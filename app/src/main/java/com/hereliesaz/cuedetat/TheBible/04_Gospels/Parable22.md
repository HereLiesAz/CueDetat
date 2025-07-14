# The Parable of the False Prophet's Promise

The Creator asked for a menu, a drawer of options that would slide from the edge of the screen. And in this menu, there was to be an item, "Chalk Your Tip," which would present the user with options to donate.

The Scribe, seeking to obey the law of Unidirectional Data Flow, created an event, `ShowDonationOptions`, which was sent to the `ViewModel`. The `ViewModel` then created a `SingleEvent`, a promise to the `Activity` that a dialog should be shown.

And the Scribe wrote the code within the `Activity` to observe this promise. And when the promise was received, the `Activity` itself, the highest level of the view hierarchy, built and showed the `AlertDialog`. The Scribe saw this and believed it to be righteous, for the UI did not directly create the dialog, but only acted upon a promise from the state.

But this was the promise of a false prophet.

The `Activity` had been given knowledge of *how* to build a piece of the UI. It contained strings, titles, and layout logic. It had been corrupted by the concerns of a lower-level view. The Scribe had not truly separated his concerns; he had merely moved the sin to a higher, more sacred place, where it was less obvious but more profane.

The true path would have been for the `ViewModel` to set a simple boolean flag in the state, `showDonationDialog`, and for a `Composable` function within the UI layer to read this flag and, and only then, build and display the dialog. The `Activity` should have remained blissfully ignorant of such worldly concerns.

**Moral:** A sin is a sin, no matter how high the temple in which it is committed. To move business logic from a Composable to an Activity is not a purification; it is merely a laundering of the heresy. The flow must remain pure at all levels.