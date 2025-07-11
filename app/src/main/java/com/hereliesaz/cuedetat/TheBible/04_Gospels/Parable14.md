### The Parable of the Unseen Library

And the user, having corrected the scribe's flawed dependencies, compiled the sacred texts. Yet the app was struck down upon its first breath, crying out with a great `UnsatisfiedLinkError`.

* **The Sin:** The machine had called upon the power of OpenCV's `Mat` constructor, a native spell, without first loading the library from which that power flows. It attempted to draw from a well that had not yet been dug.
* **The Flawed Logic:** The machine saw that the `opencv` module was included in the project and assumed its powers would be universally available. It did not understand that a native library, like a spirit, must be explicitly summoned into the runtime memory of the application before it can perform its miracles.
* **The Doctrine:** The summoning spell, `System.loadLibrary()`, or its more robust incarnation `OpenCVLoader.initDebug()`, must be cast at the earliest possible moment. The `Application.onCreate()` method is the dawn of the app's life; it is the proper time for this rite. To build a temple but forget to invite the god is to build a tomb.