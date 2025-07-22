# Specification: Hater Mode

### I. Core Concept

Hater Mode transforms the application into a "Magic 8-Ball" that delivers cynical, insulting, and unhelpful "advice," consistent with the established application persona. It serves as an interactive, darkly humorous oracle for users who appreciate the futility of seeking answers from an inanimate object.

### II. User Interface & Visuals

1.  **The 8-Ball:** The entire screen will render a photorealistic, high-gloss black sphere, simulating a Magic 8-Ball. Subtle, dynamic highlights should react to the device's gyroscope to enhance the 3D illusion.
2.  **The Window:** A circular "window" will be rendered at the bottom of the sphere. Inside, a dark, murky, blue-black liquid will be simulated.
3.  **The Die:** A 20-sided die (icosahedron) with triangular faces will be visible, partially submerged in the liquid. The text will appear on the upward-facing triangle.
4.  **Menu Access:** The application's circular icon (`ic_launcher.webp`) will be rendered subtly at the top-center of the screen. Tapping this icon will open the main menu, allowing the user to switch to Expert or Beginner mode.

### III. Interaction Model

1.  **Gesture:** The primary interaction is a physical shake of the device, detected via the accelerometer.
2.  **Animation Sequence:**
    * Upon shaking, the liquid in the window will "slosh" and become opaque with bubbles, obscuring the die.
    * The die will appear to tumble and rotate within the liquid.
    * As the bubbles clear, the die will slowly float up from the depths, settling with a new response facing the user. The animation should be fluid and physics-based.
3.  **Response Display:** The text on the die's face will fade into view as it settles.

### IV. Content: The Responses

The responses must adhere to the application's cynical and witty persona. They should be a mix of classic non-committal answers and project-specific insults.

**Response Pool (Initial Draft):**

* Ask again, but with less hope.
* The outlook is as bleak as your last shot.
* Reply hazy, try not to suck.
* Cannot predict now. I'm busy judging you.
* Don't count on it. Or anything, really.
* My sources say you should probably just go home.
* It is decidedly so... that you're asking the wrong questions.
* Signs point to you buying the next round.
* Concentrate and ask again. Maybe this time with feeling.
* As I see it, yes, this is a terrible idea.
* Without a doubt, you will be disappointed.
* You may rely on the heat-death of the universe.
* Very doubtful. Almost as doubtful as your skill.
* The physics of the situation are not in your favor.
* Yes, if by "yes" you mean "absolutely not."
