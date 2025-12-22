# Project Roadmap & Future Vision

This document outlines the high-level goals and potential future directions for the Cue d'Etat project. For a detailed list of immediate tasks, known issues, and planned features, see the official [Changelog & Issues](./04_Feature_Specs/08_Changelog_And_Issues.md).

---

## **Core Mission**

To provide a genuinely useful, delightfully cynical, and technically robust tool for billiards players of all skill levels. The application should not only assist with aiming but also deepen the user's understanding of the underlying physics and geometry of the game.

---

## **Future Development Themes**

### **1. Enhanced Computer Vision & Automation**

The ultimate goal is to automate as much of the setup process as possible, allowing the user to focus on the shot.

*   **Automatic Table Detection:** Use CV to identify the boundaries of the pool table.
*   **Automatic Ball Detection:** Use CV to locate the cue ball and all object balls on the table.
*   **Pocket Detection:** Identify the location of the pockets to enable more sophisticated shot analysis.
*   **Obstruction Analysis:** Automatically determine if a shot path is blocked by another ball.

### **2. Deeper Physics Simulation**

Move beyond simple geometric calculations to provide more accurate and comprehensive shot previews.

*   **Advanced English/Spin Simulation:** Accurately model the effects of sidespin, topspin, and draw on both the cue ball and object ball trajectories.
*   **Collision with Other Balls:** Simulate multi-ball collisions and their outcomes.
*   **Rail Compression & Rebound:** Model the effect of rail elasticity on bank shots more accurately.

### **3. UI/UX Refinement & User Engagement**

Improve the user experience and provide more ways for users to learn and interact with the app.

*   **Gamification & Training Modules:** Create structured drills and exercises to help users practice specific skills (e.g., cut shots, bank shots).
*   **Shot Recording & Analysis:** Allow users to save and review their shots, with data overlays showing what went right or wrong.
*   **Community Features:** Potentially allow users to share challenging shots or solutions with others.

### **4. Performance & Technical Excellence**

Continue to optimize the application for a smooth and responsive experience on a wide range of devices.

*   **GPU Acceleration:** Move computationally intensive rendering and CV tasks to the GPU using OpenGL ES or a similar graphics library.
*   **Advanced Calibration Models:** Implement more sophisticated camera calibration techniques to handle a wider variety of lens distortions.
*   **Codebase Modernization:** Continuously adopt modern Android development practices and libraries to ensure the project remains maintainable and scalable.
