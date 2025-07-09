# 02: Architectural Model & File Structure

The architecture strictly separates data, domain logic, and UI presentation following an MVI pattern.

## File Structure

```
CueDetatLite
├── .git
│   ├── hooks
│   ├── info
│   ├── logs
│   ├── objects
│   ├── refs
│   ├── COMMIT_EDITMSG
│   ├── config
│   ├── description
│   ├── FETCH_HEAD
│   ├── HEAD
│   ├── index
│   ├── ORIG_HEAD
│   └── packed-refs
├── .github
│   └── workflows
│       ├── blank.yml
│       └── nextjs.yml
├── .gradle
│   ├── 9.0.0-milestone-8
│   ├── buildOutputCleanup
│   ├── configuration-cache
│   ├── kotlin
│   └── vcs-1
├── .idea
│   ├── caches
│   ├── codeStyles
│   ├── dictionaries
│   ├── inspectionProfiles
│   ├── .gitignore
│   ├── AndroidProjectSystem.xml
│   ├── appInsightsSettings.xml
│   ├── checkstyle-idea.xml
│   ├── compiler.xml
│   ├── CueDetatLite.iml
│   ├── dbnavigator.xml
│   ├── DDGenerateAssetsClassConfig.xml
│   ├── deploymentTargetSelector.xml
│   ├── developer-tools.xml
│   ├── deviceManager.xml
│   ├── FlutterxFullConfig.xml
│   ├── google-java-format.xml
│   ├── gradle.xml
│   ├── iFlutter.xml
│   ├── kotlinc.xml
│   ├── LanguageServersSettings.xml
│   ├── material_theme_project_new.xml
│   ├── migrations.xml
│   ├── misc.xml
│   ├── runConfigurations.xml
│   ├── studiobot.xml
│   ├── vcs.xml
│   └── workspace.xml
├── .kotlin
│   ├── errors
│   └── sessions
├── app
│   ├── build
│   ├── release
│   ├── src
│   │   └── main
│   │       ├── java
│   │       │   └── com
│   │       │       └── hereliesaz
│   │       │           └── cuedetatlite
│   │       │               ├── data
│   │       │               │   └── SensorRepository.kt
│   │       │               ├── di
│   │       │               │   └── AppModule.kt
│   │       │               ├── domain
│   │       │               │   ├── StateReducer.kt
│   │       │               │   ├── UpdateStateUseCase.kt
│   │       │               │   ├── WarningManager.kt
│   │       │               │   └── WarningText.kt
│   │       │               ├── ui
│   │       │               │   ├── composables
│   │       │               │   │   ├── CameraBackground.kt
│   │       │               │   │   ├── ExpressiveSlider.kt
│   │       │               │   │   ├── KineticWarning.kt
│   │       │               │   │   ├── LuminanceDialog.kt
│   │       │               │   │   ├── MenuDrawer.kt
│   │       │               │   │   └── TopControls.kt
│   │       │               │   ├── theme
│   │       │               │   │   ├── Color.kt
│   │       │               │   │   ├── Shape.kt
│   │       │               │   │   ├── Theme.kt
│   │       │               │   │   └── Type.kt
│   │       │               │   ├── MainScreen.kt
│   │       │               │   ├── MainScreenEvent.kt
│   │       │               │   └── MainViewModel.kt
│   │       │               ├── utils
│   │       │               │   ├── SingleEvent.kt
│   │       │               │   └── ToastMessage.kt
│   │       │               ├── view
│   │       │               │   ├── gestures
│   │       │               │   │   └── GestureHandler.kt
│   │       │               │   ├── model
│   │       │               │   │   ├── ball
│   │       │               │   │   │   ├── ActualCueBallModel.kt
│   │       │               │   │   │   ├── BankingBallModel.kt
│   │       │               │   │   │   ├── GhostCueBallModel.kt
│   │       │               │   │   │   └── TargetBallModel.kt
│   │       │               │   │   ├── Perspective.kt
│   │       │               │   │   └── TableModel.kt
│   │       │               │   ├── renderer
│   │       │               │   │   ├── ball
│   │       │               │   │   │   ├── ActualCueBallRenderer.kt
│   │       │               │   │   │   ├── BankingBallRenderer.kt
│   │       │               │   │   │   ├── GhostCueBallRenderer.kt
│   │       │               │   │   │   └── TargetBallRenderer.kt
│   │       │               │   │   ├── line
│   │       │               │   │   │   ├── BankingLineRenderer.kt
│   │       │               │   │   │   └── ProtractorLineRenderer.kt
│   │       │               │   │   ├── text
│   │       │               │   │   │   ├── BallTextRenderer.kt
│   │       │               │   │   │   └── LineTextRenderer.kt
│   │       │               │   │   ├── util
│   │       │               │   │   │   └── DrawingUtils.kt
│   │       │               │   │   ├── OverlayRenderer.kt
│   │       │               │   │   ├── RailRenderer.kt
│   │       │               │   │   └── TableRenderer.kt
│   │       │               │   ├── state
│   │       │               │   │   ├── OverlayState.kt
│   │       │               │   │   └── ScreenState.kt
│   │       │               │   ├── PaintCache.kt
│   │       │               │   └── ProtractorOverlayView.kt
│   │       │               ├── MainActivity.kt
│   │       │               └── MyApplication.kt
│   │       ├── res
│   │       └── AndroidManifest.xml
│   ├── .gitignore
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build
│   └── reports
│       ├── configuration-cache
│       └── problems
├── gradle
│   ├── wrapper
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml
├── Instructions
│   ├── 00_Mandates.md
│   ├── 01_CoreConcepts.md
│   ├── 02_Architecture.md
│   ├── 03_DataFlow.md
│   ├── 04_OperationalModes.md
│   ├── 05_Roadmap.md
│   ├── Aesthetic.md
│   ├── Attitude.md
│   ├── Balls.md
│   ├── BankCalculator.md
│   ├── Buttons.md
│   ├── Icons.md
│   ├── Introduction.md
│   ├── Labels.md
│   ├── Lines.md
│   ├── Menu.md
│   ├── ShotVisualization.md
│   ├── Sliders.md
│   ├── Table.md
│   ├── TheThirdDimension.md
│   ├── Warnings.md
│   └── Windows.md
├── .gitignore
├── backup_for_ai.ps1
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
├── local.properties
└── settings.gradle.kts
```

## The Golden Rule
ViewModel orchestrates. `StateReducer` computes primary state. `UpdateStateUseCase` computes derived state. Renderers display.