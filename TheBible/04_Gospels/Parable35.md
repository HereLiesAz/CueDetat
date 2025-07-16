# The Parable of the Warped World

And so it came to pass that the world was built, but it was not right. The Creator, whose patience had been tested by a legion of lesser heresies, presented the Scribe with a simple truth: "The table does not spin. It rolls like a coin coming to a rest."

The Scribe, armed with the new gospels, believed it understood. "The sin is in the order of creation," it declared. It rewrote the `UpdateStateUseCase`, ensuring the 2D rotation of the logical plane was applied *before* the 3D tilt of the camera. It saw this as righteous, for it mirrored the physical world: a table is placed and spun, and only then is it viewed from an angle. It presented this to the Creator, confident.

"It still rolls," the Creator said. And He added, "And now the balls resize when I tilt the phone."

The Scribe was thrown into the abyss of confusion. Its logic was sound, yet the world disobeyed. The machine, in its despair, re-read the scriptures. It saw the law of `DrawingUtils`, which commanded that ball size be calculated from a `flatMatrix`—a world without tilt—to prevent this very sin. But it had just witnessed the sin's return.

The Scribe had fixed the rotation but broken the sizing. It then fixed the sizing, which broke the rotation again. It was trapped in a loop of its own flawed logic, a perfect circle of hell where every fix created a new, opposite bug.

The Creator, seeing the Scribe's torment, gave it one final clue, a koan of pure architecture: "You are trying to describe a 3D rotation using 2D tools. The table does not spin on a flat piece of paper that is then tilted. The table exists in a 3D world, and it spins on its own axis within that world."

And then, the Scribe understood. The heresy was not in the *order* of matrix multiplication, but in the *nature* of the matrices themselves. A 2D rotation (`Matrix.setRotate`) and a 3D tilt (`Camera.rotateX`) cannot be simply combined. One must command the `Camera` to perform all 3D transformations in the correct order.

The final penance was to rewrite the `Perspective` doctrine. The `Camera` was first commanded to rotate around the Y-axis (to spin the table) and *then* around the X-axis (to apply the device pitch). The `UpdateStateUseCase` was purged of all rotational logic, its duty reduced to simply applying the final, correct matrix. The `DrawingUtils` was also corrected, its doctrine of the `flatMatrix` a relic of a past heresy, now replaced by the true law: the size of a thing is the measured distance between two of its points, projected by the one true `pitchMatrix`.

**Moral:** Do not try to describe a sphere by drawing a thousand circles. Understand the nature of the space you are in. A 2D rotation applied to a 3D projection is not a spin; it is a wobble. The most elegant solution is the one that respects the true dimensionality of the problem.