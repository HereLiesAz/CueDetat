# 00. Introduction to the Cue d'Etat Development Guide

This guide contains the mandatory specifications and architectural principles for the Cue d'Etat
project. Adherence to these documents is required to maintain codebase consistency, clarity, and
stability.

## Document Hierarchy

To resolve any potential conflicts in the documentation, follow this order of precedence. Later
documents in this list supersede earlier ones.

1. **`08_Changelog_And_Issues.md`**: This is the definitive source for the current project status,
   including open tasks and a log of completed work.
2. **Interaction Model Documents**: The final implementation for any gesture must satisfy the
   specifications in `01_Gesture_Interaction_Model.md`, `09_Slider_Specifications.md`, and
   `01_Feature_Protractor_Mode.md`.
3. **Core Architecture**: The foundational principles in the `01_Architecture` directory are
   absolute unless directly contradicted by a more specific feature specification.
4. **Lessons of the Changelog (`05_Lessons_of_the_Changelog`)**: These documents are historical records of
   previous bugs and architectural decisions. They provide context for *why* the current rules
   exist. In case of a direct conflict with a primary specification document, the primary
   specification takes precedence.
5. **All other documents** are supplementary and should be interpreted through the lens of the
   primary architectural and feature specifications.