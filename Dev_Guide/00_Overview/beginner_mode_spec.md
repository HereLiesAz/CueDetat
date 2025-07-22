# Proposal: Beginner Mode Feature Set

To make the application "absolutely approachable" for new users, this mode should radically simplify the interface and remove all advanced, non-essential features.

### I. Default State Configuration

- **`areHelpersVisible`**: `true`. All descriptive labels for balls and lines are on by default.
- **`table.isVisible`**: `true`. The table provides essential context and should be visible.
- **`onPlaneBall`**: Not `null`. The cue ball is mandatory and visible.
- **Perspective Tilt**: The 3D tilt effect should be disabled or significantly reduced initially to prevent overwhelming new users.

### II. Feature & UI Reductions

The following features should be **removed or hidden** from the UI in Beginner Mode:

- **Banking Mode**: The "Calculate Bank" menu option should be removed.
- **Spin Control**: The "Spin" button and its associated UI should be removed.
- **Obstacle Balls**: The "Add Ball" button should be removed.
- **Advanced Options**: The entire "Too Advanced Options" menu item and its associated dialogs (CV tuning, calibration, etc.) should be removed.
- **World Rotation**: The horizontal table rotation slider should be removed.
- **Pan & Zoom**: Multi-touch gestures for pan and zoom could be disabled to prevent accidental view changes. The vertical zoom slider would remain as the sole method for adjusting magnification.

### III. Resulting User Experience

The resulting interface would consist of:

1.  A draggable **Target Ball**.
2.  A draggable **Cue Ball**.
3.  A single, rotational drag gesture to aim.
4.  A vertical slider for zoom.

This presents the core value of the application—shot visualization—in its purest form, without the cognitive load of advanced features.
