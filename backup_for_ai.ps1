#Requires -Version 5.1

<#
.SYNOPSIS
Backs up an Android project's relevant text files into a single document for AI analysis.
.DESCRIPTION
This script scans an Android project directory, intelligently includes source code,
resources, and build scripts, while excluding binaries, build artifacts, and
IDE settings to minimize token count. Non-text assets are listed.
The output file is timestamped and the script shows progress.
.NOTES
Author: Your Name/AI Assistant
Version: 1.2
Place this script in the root of your Android project and run it from there.
#>

param (
    [string]$ProjectRoot = (Get-Location).Path
)

# --- Configuration ---
# Directories to completely exclude (uses -like, so wildcards * and ? work)
$ExcludeDirsPatterns = @(
    "*\build\*", "*\.gradle\*", "*\.idea\*", "*\gradle\wrapper\*",
    "*\captures\*", "*\.cxx\*", "*\generated\*"
)

# Specific files or patterns to exclude (uses -like)
$ExcludeFilesPatterns = @(
    "*.apk", "*.aab", "*.jar", "*.keystore", "*.iml",
    "local.properties",
    ".DS_Store", # macOS specific
    "*.log",
    "*.bak", "*.tmp",
    # This script itself and its output
    "Backup-AndroidProjectForAI.ps1",
    "project_context_for_ai_*.txt"
)

# Text file extensions/names to include content for (case-insensitive)
$IncludeTextExtensionsOrNames = @(
    ".kt", ".java", ".scala", # Source code
    ".xml", # Layouts, resources, manifests
    ".gradle", "gradle.properties", "settings.gradle", # Gradle files
    ".pro", # Proguard/R8 rules
    ".json", ".yaml", ".yml", # Config files
    ".md", ".txt", # Documentation
    ".gitignore", # Important for project structure
    ".sh", ".bat", # Shell/batch scripts
    "gradlew"                 # gradlew script (no extension)
)

# Common non-text asset extensions to list (case-insensitive)
$NonTextAssetExtensions = @(
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", # Images
    ".ttf", ".otf", ".woff", ".woff2", # Fonts
    ".mp3", ".wav", ".ogg", ".aac", # Audio
    ".mp4", ".mov", ".webm", # Video
    ".zip", ".tar", ".gz", ".rar", # Archives
    ".so", ".dll", ".dylib", # Native libraries
    ".db", ".sqlite"                                # Databases
)
# --- End Configuration ---

function Test-PathAgainstPatterns
{
    param(
        [string]$Path,
        [string[]]$Patterns
    )
    foreach ($pattern in $Patterns)
    {
        if ($Path -like $pattern)
        {
            return $true
        }
    }
    return $false
}

function Is-TextFileToInclude
{
    param(
        [System.IO.FileInfo]$FileItem
    )
    $fileNameLower = $FileItem.Name.ToLower()
    $fileExtensionLower = $FileItem.Extension.ToLower()

    foreach ($entry in $IncludeTextExtensionsOrNames)
    {
        if ( $entry.StartsWith("."))
        {
            # It's an extension
            if ($fileExtensionLower -eq $entry)
            {
                return $true
            }
        }
        else
        {
            # It's a full name
            if ($fileNameLower -eq $entry.ToLower())
            {
                # Special check for gradlew to ensure it's text-like
                if ($fileNameLower -eq "gradlew")
                {
                    try
                    {
                        $peekContent = Get-Content -Path $FileItem.FullName -TotalCount 5 -ErrorAction SilentlyContinue
                        # If it's mostly printable ASCII or UTF-8, consider it text.
                        # This is a heuristic. A more robust check might involve checking for null bytes
                        # or specific non-text byte sequences.
                        if ($peekContent -join "" -notmatch "[\x00-\x08\x0B\x0C\x0E-\x1F]")
                        {
                            return $true
                        }
                        Write-Verbose "Skipping '$( $FileItem.FullName )' as it seems binary despite being named 'gradlew'."
                        return $false
                    }
                    catch
                    {
                        Write-Verbose "Could not peek into '$( $FileItem.FullName )' to verify if text."
                        return $false # Error reading, assume not text
                    }
                }
                return $true
            }
        }
    }
    return $false
}

function Is-NonTextAssetToList
{
    param(
        [System.IO.FileInfo]$FileItem
    )
    $fileExtensionLower = $FileItem.Extension.ToLower()
    return $NonTextAssetExtensions -contains $fileExtensionLower
}


# --- Main Script ---
Write-Host "Starting Android project backup for AI analysis..." -ForegroundColor Green
Write-Host "Project root: $ProjectRoot"

# --- MODIFIED: Cleanup Step ---
$oldBackupPattern = "project_context_for_ai_*.txt"
$oldBackups = Get-ChildItem -Path $ProjectRoot -Filter $oldBackupPattern -File -ErrorAction SilentlyContinue
if ($null -ne $oldBackups) {
    Write-Host "`nFound $(@($oldBackups).Count) previously generated backup file(s). Cleaning up..." -ForegroundColor Yellow
    foreach ($backup in $oldBackups) {
        Write-Host " - Removing $($backup.Name)"
        Remove-Item -Path $backup.FullName -Force
    }
}
# --- End Modification ---

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outputFileName = "project_context_for_ai_$timestamp.txt"
$outputFilePath = Join-Path -Path $ProjectRoot -ChildPath $outputFileName
Write-Host "`nOutput file will be: $outputFilePath"

# Ensure this script and its potential output are in the exclusion list by full path temporarily for robust exclusion
$scriptFullPath = $MyInvocation.MyCommand.Path
$tempExcludeFilesPatterns = @($ExcludeFilesPatterns) # Create a mutable copy
if ($scriptFullPath -and -not ($tempExcludeFilesPatterns -contains (Split-Path $scriptFullPath -Leaf)))
{
    $tempExcludeFilesPatterns += (Split-Path $scriptFullPath -Leaf)
}
# Add the currently determined output file to exclusions as well
if (-not ($tempExcludeFilesPatterns -contains $outputFileName))
{
    $tempExcludeFilesPatterns += $outputFileName
}


$filesToProcess = [System.Collections.Generic.List[System.IO.FileInfo]]::new()
$nonTextAssetsFound = [System.Collections.Generic.List[System.IO.FileInfo]]::new()

Write-Host "`nScanning project files..." -ForegroundColor Cyan

# Get all files first for progress calculation
$allItems = Get-ChildItem -Path $ProjectRoot -Recurse -Force -ErrorAction SilentlyContinue
$totalItems = $allItems.Count
$currentItem = 0

foreach ($item in $allItems)
{
    $currentItem++
    Write-Progress -Activity "Scanning Files" -Status "$( $item.FullName )" -PercentComplete (($currentItem / $totalItems) * 100) -Id 1

    $relativePath = $item.FullName.Substring($ProjectRoot.Length).TrimStart("\/")

    # Skip if it's a directory (we only process files directly)
    if ($item.PSIsContainer)
    {
        # Check if directory path matches exclusion patterns
        if (Test-PathAgainstPatterns -Path $item.FullName -Patterns $ExcludeDirsPatterns)
        {
            # This check is more illustrative here as GCI -Recurse might already dive in.
            # A more robust exclusion would be to filter out children of excluded dirs.
            # For simplicity, we filter files based on their full path matching dir patterns too.
        }
        continue
    }

    # Check if file's parent directory path matches exclusion patterns
    $parentDirRelativePath = (Split-Path $item.FullName -Parent).Substring($ProjectRoot.Length).TrimStart("\/")
    if ($parentDirRelativePath -ne "")
    {
        # Avoid checking root itself if it becomes empty string
        if (Test-PathAgainstPatterns -Path (Split-Path $item.FullName -Parent) -Patterns $ExcludeDirsPatterns)
        {
            Write-Verbose "Skipping $( $item.FullName ) due to excluded parent directory."
            continue
        }
    }

    # Check against file exclusion patterns (name or full path)
    if ((Test-PathAgainstPatterns -Path $item.Name -Patterns $tempExcludeFilesPatterns) `
  -or (Test-PathAgainstPatterns -Path $item.FullName -Patterns $tempExcludeFilesPatterns))
    {
        # Check full path too for safety
        Write-Verbose "Skipping $( $item.FullName ) due to file exclusion pattern."
        continue
    }

    if (Is-TextFileToInclude -FileItem $item)
    {
        $filesToProcess.Add($item)
    }
    elseif (Is-NonTextAssetToList -FileItem $item)
    {
        $nonTextAssetsFound.Add($item)
    }
}
Write-Progress -Activity "Scanning Files" -Completed -Id 1

Write-Host "Found $( $filesToProcess.Count ) text files to include."
Write-Host "Found $( $nonTextAssetsFound.Count ) non-text assets to list."

if ($filesToProcess.Count -eq 0 -and $nonTextAssetsFound.Count -eq 0)
{
    Write-Host "No relevant files found to back up. Exiting." -ForegroundColor Yellow
    exit
}

# Start writing to the output file
Set-Content -Path $outputFilePath -Value "Android Project Backup for AI Analysis`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "Generated on: $( Get-Date -Format 'yyyy-MM-dd HH:mm:ss' )`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "Project root: $ProjectRoot`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "Platform: $( $env:OS ) / PowerShell $( $PSVersionTable.PSVersion )`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "---`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "This document contains a concatenation of relevant source and configuration files.`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "Each file begins with a '--- FILE: [relative_path] ---' header.`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "A list of non-text assets (e.g., images, fonts) is included at the end.`n`n`n" -Encoding UTF8

Write-Host "`nProcessing text files and writing to output..." -ForegroundColor Cyan
$totalFilesToWrite = $filesToProcess.Count
$filesWritten = 0
foreach ($file in ($filesToProcess | Sort-Object FullName))
{
    $filesWritten++
    Write-Progress -Activity "Writing Text Files" -Status "$( $file.FullName )" -PercentComplete (($filesWritten / $totalFilesToWrite) * 100) -Id 2

    $relativeFilePath = $file.FullName.Substring($ProjectRoot.Length).TrimStart("\/").Replace("\", "/")
    Add-Content -Path $outputFilePath -Value "--- FILE: $relativeFilePath ---`n" -Encoding UTF8
    try
    {
        # Read content, replace null bytes, then add
        $fileContent = Get-Content -Path $file.FullName -Raw -Encoding UTF8 -ErrorAction Stop
        $sanitizedContent = $fileContent -replace "\x00", "[NULL_BYTE]" # Replace null bytes
        Add-Content -Path $outputFilePath -Value $sanitizedContent -Encoding UTF8 -NoNewline
    }
    catch
    {
        Add-Content -Path $outputFilePath -Value "[Error reading file: $( $_.Exception.Message )]`n" -Encoding UTF8
    }
    Add-Content -Path $outputFilePath -Value "`n`n" -Encoding UTF8 # Two newlines for separation
}
Write-Progress -Activity "Writing Text Files" -Completed -Id 2

if ($nonTextAssetsFound.Count -gt 0)
{
    Add-Content -Path $outputFilePath -Value "--- NON-TEXT ASSET FILE LIST (Content not included) ---`n" -Encoding UTF8
    Add-Content -Path $outputFilePath -Value "The following files were found but their content was not included to save space.`n" -Encoding UTF8
    Add-Content -Path $outputFilePath -Value "This list helps the AI understand available resources.`n`n" -Encoding UTF8

    $totalAssetsToList = $nonTextAssetsFound.Count
    $assetsListed = 0
    foreach ($asset in ($nonTextAssetsFound | Sort-Object FullName))
    {
        $assetsListed++
        Write-Progress -Activity "Listing Non-Text Assets" -Status "$( $asset.FullName )" -PercentComplete (($assetsListed / $totalAssetsToList) * 100) -Id 3
        $relativeAssetPath = $asset.FullName.Substring($ProjectRoot.Length).TrimStart("\/").Replace("\", "/")
        Add-Content -Path $outputFilePath -Value "- $relativeAssetPath`n" -Encoding UTF8
    }
    Add-Content -Path $outputFilePath -Value "`n`n" -Encoding UTF8
    Write-Progress -Activity "Listing Non-Text Assets" -Completed -Id 3
}

Add-Content -Path $outputFilePath -Value "--- END OF BACKUP ---`n" -Encoding UTF8

Write-Host "`n✅ Project backup complete!" -ForegroundColor Green
Write-Host "Output saved to: $outputFilePath"
Write-Host "Please review the file for any sensitive information before sharing." -ForegroundColor Yellow