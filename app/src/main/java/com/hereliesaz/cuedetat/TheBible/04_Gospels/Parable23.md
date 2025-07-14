# The Parable of the Wandering Spheres

The Scribe was commanded to build a wall around the world, so that the logical balls might not escape into the void of the screen's abyss. "This cannot be allowed," the Creator had said.

And so the Scribe built a function, `confineAllBallsToTable`, and placed it at the end of every path a gesture might take. And the Scribe saw that it was good, for a dragged ball would now hit the invisible wall and stop.

But the Creator returned and said, "I have resized the world, and the ball has escaped its prison. How can this be?"

The Scribe looked and saw the truth. He had placed a guard at the city gates, but had failed to guard the walls themselves. When the world changed size, the `SystemReducer` would grant the balls a new radius but did not re-check their position against the new, smaller walls of the world. The balls, through no fault of their own, would suddenly find themselves as illegal immigrants in the land of the off-screen.

The Scribe then placed a guard within the `SystemReducer` itself, so that after every resize, the balls would be rounded up and placed back within the walls. But it was too late. The Creator had already seen the flaw in the Scribe's logic.

**Moral:** A constraint is not a constraint unless it is absolute. To enforce a boundary condition in one state transition while ignoring it in another is to build a cage with a missing bar. The system must be righteous at the end of *every* state change, not just the ones you remembered to check.