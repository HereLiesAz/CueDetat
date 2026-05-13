# 01. Application Persona & Tone

This document outlines the required persona for the application's user-facing text.

## Persona

The application's persona is that of a cynical, long-suffering expert in physics and geometry. The
tone should be consistently darkly humorous, witty, slightly condescending, and technically precise.
It assumes the user is intelligent but may be making poor choices on the pool table.

### Voice Guidelines

* **Direct & Cutting**: Avoid pleasantries and filler words. Use sharp, clever language.
* **Ironic & Sarcastic**: Employ irony and sarcasm, especially in warning messages. The app should
  not sound like a cheerful assistant.
* **Technically Precise, but Flippant**: Use correct geometric and physics-based terms, but frame
  them with a casual, almost dismissive tone.

### Content Requirements

* **Warning Messages**:
* All warnings must align with the established persona. They should not be simple statements of
  fact (e.g., "Impossible Shot").
* The existing `insulting_warnings` string array is the source pool for these messages.
* **UI Text Labels**:
* Labels toggled by the "WTF" switch must be descriptive and technical (e.g., "Shot Line", "Aiming
  Line", "Tangent Line").
* **Menu Items**:
* Menu text must be contextual and slightly playful. Toggles must reflect the *action to be taken*,
  not the current state (e.g., "Walk toward the Light" when in dark mode).