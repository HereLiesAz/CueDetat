# Output file for the archive
$OutputFile = "PoolProtractor_Full_Archive.txt"
$ProjectRootPath = Get-Location # Assumes script is run from project root

# --- Configuration ---
# Directories to scan within the app module (relative to $ProjectRootPath/app)
$AppModuleDirectoriesToScan = @(
    "src/main/java",
    "src/main/res/layout",
    "src/main/res/drawable",
    "src/main/res/values" # For strings.xml, colors.xml, themes.xml etc.
)

# File extensions to include when scanning directories
$FileExtensionsToScanInDirs = @("*.kt", "*.java", "*.xml")

# Specific important files to include (paths relative to $ProjectRootPath)
# The script will try both .gradle and .gradle.kts for gradle files
$SpecificFilesToInclude = @(
    "app/src/main/AndroidManifest.xml",
    "build.gradle",      # Project-level Groovy Gradle
    "build.gradle.kts",  # Project-level Kotlin Gradle
    "app/build.gradle",  # Module-level Groovy Gradle
    "app/build.gradle.kts", # Module-level Kotlin Gradle
    "settings.gradle",   # Groovy Gradle Settings
    "settings.gradle.kts" # Kotlin Gradle Settings
)

# Directories to EXCLUDE from scanning (these are often generated or contain binaries)
$DirectoriesToExclude = @(
    "build",
    ".gradle",
    ".idea",
    "libs",
    "generated"
)
# --- End Configuration ---

# Clear the output file if it already exists
if (Test-Path $OutputFile) {
    Clear-Content -Path $OutputFile
}

Write-Host "Smart Archiving PoolProtractor project files..."
Write-Host "Output will be in: $OutputFile"
Write-Host ""

# Function to append a file's content with a header
function Append-FileWithHeader {
    param (
        [System.IO.FileInfo]$FileObject,
        [string]$RelativePathForHeader
    )
    try {
        Write-Host "Appending: $RelativePathForHeader"
        Add-Content -Path $OutputFile -Value "================================================================================"
        Add-Content -Path $OutputFile -Value "FILE: $RelativePathForHeader"
        Add-Content -Path $OutputFile -Value "================================================================================"
        Get-Content -Path $FileObject.FullName -Raw | Add-Content -Path $OutputFile
        Add-Content -Path $OutputFile -Value "" # Add a newline for separation
        Add-Content -Path $OutputFile -Value "" # Add another newline
    }
    catch {
        Write-Warning "Error processing file $($FileObject.FullName): $($_.Exception.Message)"
    }
}

# --- Process files in specified app module directories ---
$AppModulePath = Join-Path -Path $ProjectRootPath -ChildPath "app"
foreach ($dirToScanInApp in $AppModuleDirectoriesToScan) {
    $CurrentScanPath = Join-Path -Path $AppModulePath -ChildPath $dirToScanInApp
    if (Test-Path $CurrentScanPath -PathType Container) { # Check if directory exists
        Get-ChildItem -Path $CurrentScanPath -Recurse -Include $FileExtensionsToScanInDirs | ForEach-Object {
            $fileItem = $_
            $isExcluded = $false
            foreach ($excludeDir in $DirectoriesToExclude) {
                if ($fileItem.FullName -like "*\$($excludeDir)\*") { # Check if path contains excludeDir
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
Write-Host "Processing specific important files (AndroidManifest, Gradle files)..."
foreach ($filePathPattern in $SpecificFilesToInclude) {
    $FullFilePath = Join-Path -Path $ProjectRootPath -ChildPath $filePathPattern
    # Test-Path with -PathType Leaf to ensure it's a file that exists
    if (Test-Path $FullFilePath -PathType Leaf) {
        $fileObject = Get-Item $FullFilePath
        # Get path relative to project root for header (which is filePathPattern itself here)
        Append-FileWithHeader $fileObject $filePathPattern
    } else {
        # This will naturally skip non-existent files (e.g., if only build.gradle.kts exists, build.gradle won't be found, which is fine)
        # You could add a verbose log here if you want to see which specific files are skipped:
        # Write-Host "Specific file not found (this might be expected if using .kts vs .gradle, etc.): $FullFilePath" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Smart archiving complete. Output is in $OutputFile"