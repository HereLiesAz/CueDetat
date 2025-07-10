# 24: The Appearance Decrees

This document establishes the Appearance Decree Layer, a system designed for granular control over the aesthetic properties of every rendered element. It supersedes the general settings in `PaintCache` by providing a specific, per-element configuration object.

## Doctrine of Configuration

* **Granularity**: Every distinct visual component (e.g., `ActualCueBall`, `AimingLine`, `Table`) must have its own corresponding data class in the `app/src/main/java/com/hereliesaz/cuedetat/view/config/` package.
* **Hierarchy**: These configuration classes inherit from base interfaces (`VisualProperties`, `BallsConfig`, `LinesConfig`, etc.) to ensure a consistent set of controllable properties.
* **Mandate of Enforcement**: During the rendering process, all renderers (`BallRenderer`, `LineRenderer`, etc.) **must** first consult the specific appearance decree for an element. The properties defined within this object (e.g., `ActualCueBall.strokeColor`) override any default values set in `PaintCache`. This is not optional. A renderer that does not obey its specific config object is a heretical renderer.

## File Structure

The new configuration layer resides in its own package:

app/src/main/java/com/hereliesaz/cuedetat/view/config/
├── base/
│   └── VisualProperties.kt
├── ball/
│   ├── ActualCueBall.kt
│   ├── BankingBall.kt
│   ├── GhostCueBall.kt
│   └── TargetBall.kt
├── line/
│   ├── AimingLine.kt
│   ├── BankLine1.kt
│   ├── BankLine2.kt
│   ├── BankLine3.kt
│   ├── BankLine4.kt
│   ├── ShotGuideLine.kt
│   └── TangentLine.kt
├── table/
│   ├── Diamonds.kt
│   ├── Holes.kt
│   ├── Rail.kt
│   └── Table.kt
└── ui/
└── ProtractorGuides.kt

This structure ensures that the laws governing the appearance of each element are as modular and explicit as the laws governing their logic. So it is written. So it shall be rendered.