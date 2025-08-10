#!/bin/bash
# A script to correctly set up a basic Android development environment.
set -euo pipefail

# --- 1. Install Java Development Kit (JDK) ---
echo "➡️ Installing OpenJDK 17..."
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk

# --- 2. Install Android Command Line Tools ---
echo "➡️ Setting up Android SDK..."

# Define paths and a known working URL
ANDROID_SDK_ROOT="$HOME/Android/sdk"
TOOLS_VERSION="11076708"
TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${TOOLS_VERSION}_latest.zip"
TOOLS_ZIP="/tmp/android-tools.zip"

# Download and place the tools in their final destination
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
wget -q "$TOOLS_URL" -O "$TOOLS_ZIP"

# Unzip and restructure the directory
# The zip file contains a single 'cmdline-tools' folder. We unzip it and rename it to 'latest'.
unzip -q "$TOOLS_ZIP" -d "$ANDROID_SDK_ROOT/cmdline-tools"
mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
rm "$TOOLS_ZIP"

# --- 3. Set Environment Variables Permanently ---
echo "➡️ Configuring environment variables in ~/.bashrc..."
BASHRC_FILE="$HOME/.bashrc"

# Add these exports to .bashrc if they aren't already there
grep -qF "export JAVA_HOME" "$BASHRC_FILE" || echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> "$BASHRC_FILE"
grep -qF "export ANDROID_SDK_ROOT" "$BASHRC_FILE" || echo 'export ANDROID_SDK_ROOT=$HOME/Android/sdk' >> "$BASHRC_FILE"
grep -qF "export ANDROID_HOME" "$BASHRC_FILE" || echo 'export ANDROID_HOME=$ANDROID_SDK_ROOT' >> "$BASHRC_FILE" # For backward compatibility

# Add necessary directories to the PATH variable
grep -qF 'PATH=$JAVA_HOME/bin:$PATH' "$BASHRC_FILE" || echo 'export PATH=$JAVA_HOME/bin:$PATH' >> "$BASHRC_FILE"
grep -qF 'PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin' "$BASHRC_FILE" || echo 'export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin' >> "$BASHRC_FILE"
grep -qF 'PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools' "$BASHRC_FILE" || echo 'export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools' >> "$BASHRC_FILE"

echo "✅ Setup complete!"
echo "Run 'source ~/.bashrc' or restart your terminal to apply the changes."
