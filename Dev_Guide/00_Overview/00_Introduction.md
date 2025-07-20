# 00. Introduction to the Cue d'Etat Development Guide

This guide contains the mandatory specifications and architectural principles for the Cue d'Etat
project. Adherence to these documents is required to maintain codebase consistency, clarity, and
stability.

## Document Hierarchy

To resolve any potential conflicts in the documentation, follow this order of precedence. Later
documents in this list supersede earlier ones.

1. **`04-7_Changelog_And_Issues.md`**: This is the definitive source for the current project status,
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

Dev_Guide/
├── 00_Project_Overview/
│ ├── 00_Introduction.md
│ ├── 01_Project_Persona.md
│ ├── 02_ConductCode.md
│ ├── 03_CoreConcepts.md
│ └── Changelog.md
├── 01_Architecture/
│ ├── 01_Architectural_Mandates.md
│ ├── 02_MVI_Architecture_Overview.md
│ ├── 03_State_Management_OverlayState.md
│ ├── 04_Application_Manifest_And_Build_Rules.md
│ └── 05_Dependencies.md
├── 02_Core_Components/
│ ├── 01_Operational_Modes.md
│ ├── 02_Perspective_And_3D_Transformation.md
│ ├── 03_Table_Component.md
│ ├── 04_Ball_Components.md
│ └── 05_Line_Components.md
├── 03_UI_UX_Guide/
│ ├── 01_Gesture_Interaction_Model.md
│ ├── 02_Warning_System.md
│ ├── 03_Visual_Style_And_Theming.md
│ ├── 04_Menu_Drawer.md
│ ├── 05_Dialogs_And_Overlays.md
│ ├── 06_Button_Specifications.md
│ ├── 07_Icon_Specifications.md
│ ├── 08_Label_Specifications.md
│ └── 09_Slider_Specifications.md
├── 04_Feature_Specs/
│ ├── 01_Feature_Protractor_Mode.md
│ ├── 02_Feature_Banking_Mode.md
│ ├── 03_Feature_Spin_Paths.md
│ ├── 04_Feature_Obstruction_Detection.md
│ ├── 05_Feature_CV_Hybrid_Eye.md
│ └── 06_Changelog_And_Issues.md
└── 05_Lessons_of_the_Changelog/
│ ├── 01_State_And_Event_Management_Lessons.md
│ ├── 02_Rendering_And_Perspective_Lessons.md
│ ├── 03_UI_And_Specification_Lessons.md
│ └── 04_Architectural_And_Process_Lessons.md