import os
import re
import shutil

def run_cleanup_script():
    print("Starting OpenCV and old file cleanup script...")

    project_root = os.getcwd()

    # --- 1. Files to Delete ---
    files_to_delete = [
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/element/ProtractorCirclesDrawer.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/element/DeflectionLinesDrawer.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/element/ProtractorAnglesDrawer.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/element/ScreenSpaceTextDrawer.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/element/ProtractorPlaneTextDrawer.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/ProtractorPlaneDrawer.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/GhostBallDrawer.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/HelperTextDrawer.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/ProtractorDrawingCoordinator.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/calculator/ProtractorGeometryCalculator.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/ProtractorState.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/ProtractorPaints.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/ProtractorGestureHandler.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/ProtractorConfig.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/protractor/ProtractorOverlayView.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/tables/TableView.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/tables/TableState.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/system/AppCameraManager.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/system/DevicePitchSensor.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/tracking/ball_detector/HSVRange.kt",
        "app/src/main/java/com/hereliesaz/cuedetat/tracking/utils/YuvToRgbConverter.kt",
        "app/src/main/res/drawable/ic_light_mode_24.xml",
        "app/src/main/res/drawable/ic_dark_mode_24.xml",
        "app/src/main/res/values/ic_launcher_background.xml", # Duplicate file
    ]

    for file_path in files_to_delete:
        abs_path = os.path.join(project_root, file_path)
        if os.path.exists(abs_path):
            os.remove(abs_path)
            print(f"Deleted: {file_path}")
        else:
            print(f"Skipped (not found): {file_path}")

    # --- 2. Files to Modify ---

    # app/src/main/java/com/hereliesaz/cuedetat/ui/theme/Color.kt
    color_kt_path = os.path.join(project_root, "app/src/main/java/com/hereliesaz/cuedetat/ui/theme/Color.kt")
    if os.path.exists(color_kt_path):
        with open(color_kt_path, 'r') as f:
            content = f.read()
        content = re.sub(r"// val AppHelpTextDefault = Color\(0xFFD1C4E9\).*?\n", "", content) # Remove old commented line
        content = re.sub(r"val AppHelpTextDefault = Color\(0xFFD1C4E9\)", r"// val AppHelpTextDefault = Color(0xFFD1C4E9) // Default helper text color", content) # Add new comment
        with open(color_kt_path, 'w') as f:
            f.write(content)
        print(f"Modified: {color_kt_path}")

    # app/src/main/java/com/hereliesaz/cuedetat/MyApplication.kt
    my_application_path = os.path.join(project_root, "app/src/main/java/com/hereliesaz/cuedetat/MyApplication.kt")
    if os.path.exists(my_application_path):
        with open(my_application_path, 'r') as f:
            content = f.read()
        content = re.sub(r"// REMOVE THIS LINE: import org\.opencv\.android\.OpenCVLoader\n", "", content)
        content = re.sub(
            r"// REMOVE THIS BLOCK - No longer needed for OpenCV initialization.*?// Consider showing a user-friendly message or disabling features if OpenCV is crucial\n// }",
            "",
            content, flags=re.DOTALL
        )
        content = content.replace("else {", "") # Clean up trailing else if needed
        content = content.replace("    Log.i(AppConfig.TAG, \"OpenCV initialized successfully.\")\n}\n", "") # And the associated line
        content = re.sub(r"\n\n\n", r"\n\n", content) # Clean up extra newlines
        with open(my_application_path, 'w') as f:
            f.write(content)
        print(f"Modified: {my_application_path}")


    # app/build.gradle.kts
    app_build_gradle_path = os.path.join(project_root, "app/build.gradle.kts")
    if os.path.exists(app_build_gradle_path):
        with open(app_build_gradle_path, 'r') as f:
            content = f.read()
        # Remove externalNativeBuild block in defaultConfig
        content = re.sub(r"// REMOVE THIS BLOCK - No longer needed for OpenCV NDK integration.*?^\s*\}\s*\}\n", "", content, flags=re.DOTALL | re.MULTILINE)
        # Remove ndkVersion line
        content = re.sub(r"^\s*// REMOVE THIS LINE: ndkVersion = \".*?\"\s*$\n", "", content, flags=re.MULTILINE)
        # Remove jni sourceSets
        content = re.sub(r"^\s*// REMOVE THIS BLOCK - No longer needed for OpenCV JNI libs.*?^\s*\}\s*\}\s*$\n", "", content, flags=re.DOTALL | re.MULTILINE)
        # Remove app's externalNativeBuild block
        content = re.sub(r"^\s*// REMOVE THIS BLOCK - No longer needed for app's CMakeLists.txt if no other native code.*?^\s*\}\s*\}\s*$\n", "", content, flags=re.DOTALL | re.MULTILINE)
        # Remove opencv project dependency
        content = re.sub(r"^\s*// REMOVE THIS LINE: implementation\(project\(\":opencv\"\)\)\s*$\n", "", content, flags=re.MULTILINE)
        with open(app_build_gradle_path, 'w') as f:
            f.write(content)
        print(f"Modified: {app_build_gradle_path}")

    # settings.gradle.kts
    settings_gradle_path = os.path.join(project_root, "settings.gradle.kts")
    if os.path.exists(settings_gradle_path):
        with open(settings_gradle_path, 'r') as f:
            content = f.read()
        # Remove opencv include
        content = re.sub(r"^\s*// REMOVE THIS LINE: include\(\":opencv\"\)\s*$\n", "", content, flags=re.MULTILINE)
        # Remove opencv projectDir
        content = re.sub(r"^\s*// REMOVE THIS LINE: project\(\":opencv\"\)\.projectDir = opencvSdkDir\s*$\n", "", content, flags=re.MULTILINE)
        with open(settings_gradle_path, 'w') as f:
            f.write(content)
        print(f"Modified: {settings_gradle_path}")

    # app/src/main/res/values/colors.xml
    colors_xml_path = os.path.join(project_root, "app/src/main/res/values/colors.xml")
    if os.path.exists(colors_xml_path):
        with open(colors_xml_path, 'r') as f:
            content = f.read()
        # Remove original colors block
        content = re.sub(r"<!-- Original colors.*?<!-- Redundant with app_white -->\n", "", content, flags=re.DOTALL)
        with open(colors_xml_path, 'w') as f:
            f.write(content)
        print(f"Modified: {colors_xml_path}")

    # app/src/main/res/values/themes.xml
    themes_xml_path = os.path.join(project_root, "app/src/main/res/values/themes.xml")
    if os.path.exists(themes_xml_path):
        with open(themes_xml_path, 'r') as f:
            content = f.read()
        content = re.sub(r"<item name=\"colorPrimary\">@color/purple_500</item>", r"<item name=\"colorPrimary\">@color/app_black</item>", content)
        content = re.sub(r"<item name=\"colorOnPrimary\">@color/white</item>", r"<item name=\"colorOnPrimary\">@color/app_yellow</item>", content)
        content = re.sub(r"<!-- <item name=\"colorPrimaryVariant\">@color/purple_700</item> -->", "", content)
        content = re.sub(r"<item name=\"colorSecondary\">@color/teal_200</item>", r"<item name=\"colorSecondary\">@color/app_black</item>", content)
        content = re.sub(r"<item name=\"colorOnSecondary\">@color/black</item>", r"<item name=\"colorOnSecondary\">@color/app_yellow</item>", content)
        content = re.sub(r"<!-- <item name=\"colorSecondaryVariant\">@color/teal_700</item> -->", "", content)
        content = re.sub(r"<item name=\"android:statusBarColor\">\?attr/colorPrimary</item>", r"<item name=\"android:statusBarColor\">@color/app_yellow</item>", content)

        # Add new color items
        new_items = """
        <item name=\"colorTertiary\">@color/app_medium_gray</item>
        <item name=\"colorOnTertiary\">@color/app_white</item>

        <item name=\"colorPrimaryContainer\">@color/app_dark_gray</item>
        <item name=\"colorOnPrimaryContainer\">@color/app_yellow</item>
        <item name=\"colorSecondaryContainer\">@color/app_dark_gray</item>
        <item name=\"colorOnSecondaryContainer\">@color/app_yellow</item>
        <item name=\"colorTertiaryContainer\">@color/app_medium_gray</item>
        <item name=\"colorOnTertiaryContainer\">@color/app_white</item>

        <item name=\"android:colorBackground\">@color/app_yellow</item>
        <item name=\"colorSurface\">@color/app_yellow</item>
        <item name=\"colorOnSurface\">@color/app_black</item>
        <item name=\"colorSurfaceVariant\">@color/app_dark_yellow</item>
        <item name=\"colorOnSurfaceVariant\">@color/app_black</item>

        <item name=\"colorError\">@color/app_error_red</item>
        <item name=\"colorOnError\">@color/app_black</item>
        <item name=\"colorErrorContainer\">@color/app_error_red</item>
        <item name=\"colorOnErrorContainer\">@color/app_black</item>

        <item name=\"colorOutline\">@color/app_black</item>
"""
        # Insert new items after colorOnSecondary
        content = re.sub(r"(<item name=\"colorOnSecondary\">@color/app_yellow</item>)", r"\1" + new_items, content, count=1)

        with open(themes_xml_path, 'w') as f:
            f.write(content)
        print(f"Modified: {themes_xml_path}")

    # readme.md
    readme_path = os.path.join(project_root, "readme.md")
    if os.path.exists(readme_path):
        with open(readme_path, 'r') as f:
            content = f.read()
        content = content.replace("It uses your phone's camera and orientation sensors and computer vision (OpenCV) for ball detection to overlay",
                                  "It uses your phone's camera and orientation sensors to overlay")
        content = content.replace("OpenCV for Android: For image processing and ball detection.",
                                  "")
        content = content.replace("Using CV to detect the table, balls, and pockets automatically. For now, you are the CV.",
                                  "Using ML Kit to detect the balls automatically. Table and pocket detection are future delusions.")
        # Add ML Kit to Tech Stack
        content = content.replace("*   **Jetpack Compose:** For Material 3 theming (colors applied to legacy View system).",
                                  "*   **Jetpack Compose:** For Material 3 theming (colors applied to legacy View system).\n*   **Google ML Kit:** For object detection (pool balls).")
        with open(readme_path, 'w') as f:
            f.write(content)
        print(f"Modified: {readme_path}")

    # Remove any empty directories if they exist (only if they are empty after deletions)
    for dir_path in ["app/src/main/java/com/hereliesaz/cuedetat/protractor",
                     "app/src/main/java/com/hereliesaz/cuedetat/protractor/calculator",
                     "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer",
                     "app/src/main/java/com/hereliesaz/cuedetat/protractor/drawer/element",
                     "app/src/main/java/com/hereliesaz/cuedetat/tables",
                     "app/src/main/java/com/hereliesaz/cuedetat/system", # PitchSensor and CameraManager moved to new dir
                     "app/src/main/java/com/hereliesaz/cuedetat/tracking/ball_detector", # Ball.kt and BallDetector.kt are staying
                     "app/src/main/java/com/hereliesaz/cuedetat/tracking/utils"]:
        abs_dir_path = os.path.join(project_root, dir_path)
        if os.path.exists(abs_dir_path) and not os.listdir(abs_dir_path):
            try:
                os.rmdir(abs_dir_path)
                print(f"Removed empty directory: {dir_path}")
            except OSError:
                print(f"Could not remove directory (not empty or permissions): {dir_path}")

    print("\nCleanup script finished.")
    print("Please perform a 'Gradle Sync' and 'Build/Rebuild Project' in Android Studio.")

if __name__ == "__main__":
    run_cleanup_script()