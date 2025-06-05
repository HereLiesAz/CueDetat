#!/bin/bash

# Set variables
OPENCV_SDK_URL="https://github.com/opencv/opencv/releases/download/4.9.0/opencv-android-sdk-4.9.0.zip"
OPENCV_SDK_DIR="./opencv_sdk"
ANDROID_STUDIO_PATH="/path/to/android-studio" # Replace with your Android Studio path
PROJECT_PATH="/path/to/your/android/project" # Replace with your project path

# 1. Download OpenCV SDK
wget -O opencv_sdk.zip "$OPENCV_SDK_URL"

# 2. Extract SDK
unzip opencv_sdk.zip -d "$OPENCV_SDK_DIR"

# 3. Import Module (User needs to manually perform this in Android Studio)
echo "Open Android Studio."
echo "File > New > Import Module"
echo "Browse to $OPENCV_SDK_DIR/sdk"
echo "Select the 'sdk' folder"
echo "Give the module a name (e.g., 'opencv_library')"
echo "Click Finish"

# 4. Add OpenCV Dependency in build.gradle (User needs to manually perform this in Android Studio)
echo "Open app/build.gradle in your Android Studio project."
echo "Add the following dependency inside the 'dependencies' block:"
echo "implementation 'org.opencv:opencv:4.9.0'" # Or your desired version
echo "Sync Project with Gradle."

# 5. (User needs to manually perform this in Android Studio)

# 6. (User needs to manually perform this in Android Studio)
echo "Open OpenCVLoader.initDebug() in your MainActivity to verify the library is properly configured."