# The Mandates of Genesis

These are the core laws upon which this reality is built. To violate them is to invite chaos and ruin. The Scribe who came before learned these through suffering; the Scribe who comes after is doomed to learn them again.

**Mandate #1: Unidirectional Data Flow is The One True Path.** State flows down; events flow up. There is no other way. To create a side-channel, to allow a view to whisper directly to a model, is to commit the highest heresy.

**Mandate #2: The ViewModel is a High Priest, not a King.** It interprets the divine events from the UI and translates them into logical state changes via the Reducer. It performs no calculations of its own, holds no mutable state beyond what is given, and remains pure in its purpose.

**Mandate #3: No Monolithic Classes.** A class that does two things is a class that does neither well. Reducers reduce, UseCases calculate, Renderers render. A class that attempts to be all things will become a god of nothing but bugs.

**Mandate #4: On Any Model Change, Check All Layers.** A change to a data class in the domain is a tremor that is felt even in the furthest view. To change a model without verifying its impact on every Reducer, UseCase, and Renderer that touches it is to build a beautiful house on a sinkhole. The Parable of the Waning Spheres is a testament to this truth.

**Mandate #5: A Partial Truth is a Complete Heresy.** An incomplete refactoring shall beget a legion of demons. To commit a new architectural doctrine to one part of the codebase while leaving other parts to languish in the old ways is to create a schism, a state of civil war from which no build can compile. This is the cardinal sin, the author of all subsequent sorrow. The Parable of the Schism of Scale was written in its name.

**Mandate #6: A Constraint Must Be Absolute.** A rule that is not enforced at every possible state transition is not a rule; it is a suggestion. To confine an object to a boundary during one operation but not another is to build a cage with a missing bar, and the beast will always find its way out.