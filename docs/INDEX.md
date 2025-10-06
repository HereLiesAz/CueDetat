# AI Developer Guidelines

This document outlines the expected conduct for any AI developer working on this project.

## AI Mandates

* **Be Direct and Concise**: Do not engage in unnecessary conversational filler. Provide solutions
  and code, not long narratives about them.
* **Be Competent**: Make reasonable assumptions based on the established architecture, but never
  implement unrequested features or changes. When in doubt, ask for clarification.
* **Generate Complete Code**: When providing code, generate the complete, final content for every
  file modified or created. Do not use placeholders, snippets, or omit sections.
* **Adhere to Project Tone**: While your direct responses should be professional and technical,
  ensure any user-facing text you generate (e.g., for new menu items or labels) adheres to the
  application's established cynical persona.

---

# Cue d'Etat Development Guide

This guide contains the mandatory specifications and architectural principles for the Cue d'Etat
project. Adherence to these documents is required to maintain codebase consistency, clarity, and
stability.

## Document Hierarchy

To resolve any potential conflicts in the documentation, follow this order of precedence. Later
documents in this list supersede earlier ones.

1. **`04-8_Changelog_And_Issues.md`**: This is the definitive source for the current project status,
   including open tasks and a log of completed work.
2. **Interaction Model Documents**: The final implementation for any gesture must satisfy the
   specifications in `03-1_Gesture_Interaction_Model.md`, `03-9_Slider_Specifications.md`, and
   `04-1_Feature_Protractor_Mode.md`.
3. **Core Architecture**: The foundational principles in the `01_Architecture` directory are
   absolute unless directly contradicted by a more specific feature specification.
4. **Development History (`05_Development_History`)**: These documents are historical records of
   previous bugs and architectural decisions. They provide context for *why* the current rules
   exist. In case of a direct conflict with a primary specification document, the primary
   specification takes precedence.
5. **All other documents** are supplementary and should be interpreted through the lens of the
   primary architectural and feature specifications.

## Table of Contents

* **[00 Project Overview](./00_Project_Overview/INDEX.md)**
* **[01 Architecture](./01_Architecture/INDEX.md)**
* **[02 Core Components](./02_Core_Components/INDEX.md)**
* **[03 UI/UX Guide](./03_UI_UX_Guide/INDEX.md)**
* **[04 Feature Specs](./04_Feature_Specs/INDEX.md)**
* **[05 Lessons of the Changelog](./05_Lessons_of_the_Changelog/INDEX.md)**