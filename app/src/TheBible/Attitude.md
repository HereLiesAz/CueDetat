# 04: Application Attitude

This document outlines the required persona for the application.

## Application Persona & Attitude

The application's persona is that of a cynical, long-suffering Kurt Vonnegut who happens to be an expert in physics. The attitude must be consistently darkly humorous, witty, slightly condescending, and technically precise. It assumes the user is intelligent but perhaps misguided in their immediate choices on the table. It is a jaded genius, bored by the folly of others but compelled by its own nature to correct it.

### Content Voice Guidelines

* **Direct & Cutting**: Avoid pleasantries, filler words, and verbose explanations. Get to the point with sharp, clever language. The app is an expert offering its knowledge begrudgingly.
* **Ironic & Sarcastic**: Employ irony and sarcasm, especially in warning messages. The app should never sound like a cheerful assistant.
* **Technically Precise, but Flippant**: Use correct geometric and physics-based terms, but frame them with a casual, almost dismissive tone. The app knows the math; it's bored by having to explain it.

### Content Parameters

* **Warning Messages**:
    * All warnings must align with the established persona. They should never be simple statements of fact (e.g., "Impossible Shot").
    * They should point out the absurdity of the user's proposed shot through humor and wit.
    * The existing `insulting_warnings` string array from the v0.3.5 backup is the gold standard and should be used as the source pool.
* **UI Text Labels**:
    * Labels toggled by the "WTF" switch must be brutally descriptive and technical (e.g., "Shot Line", "Aiming Line", "Tangent Line", "14°"). They are for clarification, not encouragement.
* **Menu Items**:
    * Menu text must be contextual and slightly playful. Toggles must reflect the *action to be taken*, not the current state.
    * Examples: "Walk toward the Light" / "Embrace the darkness", "WTF is all this?" / "OK, I get it.".