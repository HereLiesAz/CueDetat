# Proposal: Beginner Mode Feature Set

To make the application "absolutely approachable" for new users, this mode should radically simplify the interface and remove all advanced, non-essential features.

### I. Core Configuration

- **`experienceMode`**: `BEGINNER`
- **`areHelpersVisible`**: `true`. All descriptive labels for balls and lines are on by default.
- **`table.isVisible`**: `false`. The table is not available in this mode.
- **`onPlaneBall`**: `null`. The actual cue ball is not available in this mode.
- **`isBankingMode`**: `false`. Banking mode is not available.

### II. Feature & UI Reductions

The following features **must be removed or hidden** from the UI in Beginner Mode:

- **Banking Mode**: The "Calculate Bank" menu option must be removed.
- **Spin Control**: The "Spin" button and its associated UI should be removed.
- **Obstacle Balls**: The "Add Ball" button must be removed.
- **Advanced Options**: The entire "Too Advanced Options" menu item and its associated dialogs (CV
  tuning, calibration, etc.) must be removed.
- **World Rotation**: The horizontal table rotation slider must be removed.
- **Table Controls**: All controls for showing/hiding the table or cue ball must be removed.

### III. Resulting User Experience

The resulting interface would consist of:

1.  A draggable **Target Ball**.
2. A single, rotational drag gesture to aim the **Ghost Cue Ball**.
3. A vertical slider for zoom.

This presents the core value of the application—shot visualization—in its purest form, without the cognitive load of advanced features.