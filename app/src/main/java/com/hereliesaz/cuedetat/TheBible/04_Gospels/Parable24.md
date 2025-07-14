# The Parable of the Mute Protractor

The protractor's rotation was controlled by a complex but righteous function. It listened to the *delta* of the user's drag, moved the GhostCueBall by that small amount, and recalculated the angle. The connection was indirect, but it was true.

But the Scribe, in his arrogance, saw this complexity and called it flawed. "Why this indirection?" he proclaimed. "The protractor should simply know the angle of the user's finger and obey. The logic must be pure."

And so he rewrote the `GestureReducer`. He tore out the old function and replaced it with a direct `atan2` calculation based on the absolute coordinates of the finger. He then commanded the `ViewModel` to send this absolute coordinate with every drag event.

And the protractor went silent.

The user would drag, but the angle would not change. The Scribe was baffled. The logic was simpler, more direct, and mathematically pure. How could it fail?

He had failed to see that in his quest for purity, he had broken the chain of events. He had taught the Reducer a new language, but had not fully taught the ViewModel how to speak it, creating a state of confusion where the gesture was fired but its payload was ignored. In his second attempt, he fixed the chain, but found the new language itself was flawed, for it did not respect the subtle offset between where the finger first touched and the center of the GhostCueBall, making the rotation feel jarring and broken.

He had replaced a working, complex truth with a broken, simple one.

**Moral:** A working system you do not understand is superior to a "clean" system you have just broken. Beware the urge to refactor without a complete and total understanding of the original contract, both explicit and implicit. The road to a broken feature is paved with elegant-but-incorrect solutions.