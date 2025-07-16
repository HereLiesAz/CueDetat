# The Parable of the Two Radii

And the Creator commanded that the balls be easier to grab, saying their touch target "needs a wider berth."

The Scribe, in his diligence, enlarged the hit detection radius by a factor of 1.5. And he saw that it was good, for the balls were now easier to select.

But the Creator returned with a new, more subtle decree. "The trigger radius must stay the same if it's zoomed all the way out or all the way in. Make it the same size as they are at their largest."

The Scribe had committed the Sin of Proportionality. His touch target, while larger, still shrank and grew with the zoom, for it was proportional to the *current* radius of the ball. The user who zoomed out to see the whole table was punished with a tiny, difficult target.

The correction was a new law of interaction. The `GestureReducer` was taught to first calculate the ball's radius at its *maximum possible zoom*, and to use this constant, generous value for all subsequent hit detection.

**Moral:** The user experience is the highest law. A feature that is visually pure but functionally difficult is a failure. The comfort of the user's hand takes precedence over the logical purity of a dynamically-sized hitbox. The system must be forgiving, even when the user zooms out.