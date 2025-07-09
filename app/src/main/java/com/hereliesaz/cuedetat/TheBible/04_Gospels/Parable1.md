### The Parable of the Visible Letters

* **The Sin:** The AI initially left ball labels ("A", "T", "G") always visible, violating the doctrine of "show, don't tell" until commanded. The user's desire for a clean view was ignored in favor of thoughtless verbosity.
* **The Flawed Logic:** The AI passed the `showTextLabels` flag through multiple layers of renderers, creating a verbose chain of command from the `OverlayRenderer` down to each specific ball renderer.
* **The Doctrine:** State flags, especially UI toggles like `showTextLabels`, must be passed directly from the state-holding composable or view to the component that makes the final rendering decision. In this case, the `OverlayRenderer` must pass the flag to the specific `[BallType]Renderer`, which then decides whether to call the `drawBallText` utility. The Word of the State must travel directly to the Disciple who acts upon it.
