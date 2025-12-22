# Screens & UI Flow

This document provides an overview of the main screens in the Cue d'Etat application.

---

## **Main Screen (`MainActivity`)**

The application consists of a single activity (`MainActivity`) that hosts all the UI content. The UI is built entirely with Jetpack Compose.

### **Components**

1.  **Camera Preview:** The background of the screen is a live feed from the device's camera.
2.  **Protractor Overlay (`ProtractorOverlayView`):** A custom `View` is drawn on top of the camera preview. This view is responsible for rendering all the aiming guides, lines, balls, and text. It is the core visual component of the application.
3.  **Top App Bar:**
    *   Displays the application title.
    *   Contains a navigation icon to open the main menu drawer.
4.  **Main Menu Drawer:**
    *   Provides access to all the main features and settings.
    *   Contains controls for toggling different visual guides (e.g., protractor, tangent lines).
    *   Allows the user to switch between different operational modes (e.g., Protractor Mode, Banking Mode).
5.  **Bottom Sliders/Controls:**
    *   Context-sensitive controls are displayed at the bottom of the screen.
    *   These include sliders for adjusting parameters like ball size, spin, and shot angle.

### **UI Flow**

The user interacts with the application primarily through:

1.  **On-screen gestures:** Panning and pinching the screen to rotate and zoom the protractor overlay.
2.  **Device movement:** Tilting the phone to change the 3D perspective of the overlay.
3.  **Menu and sliders:** Using the UI controls to change settings and modes.

The screen is designed to be highly interactive and responsive, providing real-time visual feedback to the user's actions. All UI elements are driven by the `CueDetatState`, which is managed by the `MainViewModel`.
