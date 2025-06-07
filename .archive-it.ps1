# Output file for the archive
$OutputFile = "PoolProtractor_Full_Archive.txt"
$ProjectRootPath = Get-Location # Assumes script is run from project root

# --- Configuration ---
# Directories to scan (paths are relative to $ProjectRootPath)
$DirectoriesToScanRelative = @(
    ".",                   # Scan the project root directory
    "app/src/main/java",
    "app/src/main/res/layout",
    "app/src/main/res/drawable",
    "app/src/main/res/values",
    "opencv/src/main/java", # Include if your 'opencv' module has custom Java/Kotlin source files
    "opencv/src/main/res/layout" # Include if your 'opencv' module has custom resource files
)

# File extensions to include when scanning directories
$FileExtensionsToScan = @("*.kt", "*.java", "*.xml", "*.md") # Added *.md for project-level README.md

# Specific important files to include (paths relative to $ProjectRootPath)
# These will be processed separately to ensure they are always included,
# even if their directory is not recursively scanned or if they are in an excluded subdir.
$SpecificFilesToInclude = @(
    "app/src/main/AndroidManifest.xml",
    "build.gradle",         # Project-level Groovy Gradle
    "build.gradle.kts",     # Project-level Kotlin Gradle
    "app/build.gradle",     # Module-level Groovy Gradle
    "app/build.gradle.kts", # Module-level Kotlin Gradle
    "settings.gradle",      # Groovy Gradle Settings
    "settings.gradle.kts",  # Kotlin Gradle Settings
    "readme.md"             # Project-level readme
)

# Directories to EXCLUDE from scanning (these are often generated or contain binaries)
# Paths here are fragments that will be checked against the FULL path of a file/directory.
$DirectoriesToExclude = @(
    "build",
    ".gradle",
    ".idea",
    "libs",
    "generated",
    "out",
    "intermediates",
    "tmp",
    "sdk" # Exclude OpenCV SDK's raw sdk directory, as its relevant parts are linked via jniLibs/java.srcDirs
)
# --- End Configuration ---

# Clear the output file if it already exists
if (Test-Path $OutputFile) {
    Clear-Content -Path $OutputFile
}

Write-Host "Smart Archiving PoolProtractor project files..."
Write-Host "Output will be in: $OutputFile"
Write-Host ""

# Keep track of files already appended by directory scan to avoid duplicates
$appendedFiles = New-Object System.Collections.Generic.HashSet[string]

# Function to append a file's content with a header
function Append-FileWithHeader {
    param (
        [System.IO.FileInfo]$FileObject,
        [string]$RelativePathForHeader
    )
    # Convert to canonical path for reliable comparison (case-insensitive)
    $canonicalPath = $FileObject.FullName.ToLowerInvariant()

    if ($appendedFiles.Contains($canonicalPath)) {
        Write-Host "Skipping already appended file: $RelativePathForHeader" -ForegroundColor DarkGray
        return
    }

    try {
        Write-Host "Appending: $RelativePathForHeader"
        Add-Content -Path $OutputFile -Value "================================================================================"
        Add-Content -Path $OutputFile -Value "FILE: $RelativePathForHeader"
        Add-Content -Path $OutputFile -Value "================================================================================"
        Get-Content -Path $FileObject.FullName -Raw | Add-Content -Path $OutputFile
        Add-Content -Path $OutputFile -Value "" # Add a newline for separation
        Add-Content -Path $OutputFile -Value "" # Add another newline
        $appendedFiles.Add($canonicalPath) # Add to the set of appended files
    }
    catch {
        Write-Warning "Error processing file $($FileObject.FullName): $($_.Exception.Message)"
    }
}

# --- Process files in specified directories relative to project root ---
Write-Host "Processing files in specified directories (recursive)..."
foreach ($dirRelativePath in $DirectoriesToScanRelative) {
    $CurrentScanPath = Join-Path -Path $ProjectRootPath -ChildPath $dirRelativePath

    if (Test-Path $CurrentScanPath -PathType Container) { # Check if directory exists
        Get-ChildItem -Path $CurrentScanPath -Recurse -Include $FileExtensionsToScan | ForEach-Object {
            $fileItem = $_
            $isExcluded = $false
            foreach ($excludeDir in $DirectoriesToExclude) {
                # Check if file's full path contains any of the excluded directory names (case-insensitive)
                if ($fileItem.FullName.ToLowerInvariant() -like "*\$($excludeDir.ToLowerInvariant())\*") {
                    $isExcluded = $true
                    break
                }
            }
            if (-not $isExcluded) {
                # Get path relative to project root for header
                $relativePath = $fileItem.FullName.Substring($ProjectRootPath.Path.Length).TrimStart('\/')
                Append-FileWithHeader $fileItem $relativePath
            }
        }
    } else {
        Write-Warning "Directory to scan not found: $CurrentScanPath"
    }
}

# --- Process specific important files (AndroidManifest, Gradle files, etc.) ---
# These are processed last to ensure they are included even if not caught by directory scans,
# and the `appendedFiles` set prevents duplicates.
Write-Host "Processing specific important files..."
foreach ($filePathPattern in $SpecificFilesToInclude) {
    $FullFilePath = Join-Path -Path $ProjectRootPath -ChildPath $filePathPattern
    # Test-Path with -PathType Leaf to ensure it's a file that exists
    if (Test-Path $FullFilePath -PathType Leaf) {
        $fileObject = Get-Item $FullFilePath
        # Get path relative to project root for header (which is filePathPattern itself here)
        Append-FileWithHeader $fileObject $filePathPattern
    } else {
        # This will naturally skip non-existent files (e.g., if only build.gradle.kts exists, build.gradle won't be found, which is fine)
        # Write-Host "Specific file not found (this might be expected if using .kts vs .gradle, etc.): $FullFilePath" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Smart archiving complete. Output is in $OutputFile"