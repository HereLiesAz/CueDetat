#Requires -Version 5.1

<#
.SYNOPSIS
Backs up an Android project, including specified root files and interactively selected modules.
.DESCRIPTION
This script scans the project's root directory to find main folders/modules and non-trivial root files.
It automatically ignores configuration folders (like .idea, .git) and build output. It then iterates
through the discovered modules, prompting the user to either include or skip each one. A complete
project tree is generated for the entire project, visually indicating the user's choices. The script
then processes only the included modules and root files, concatenating relevant text files and listing
non-text assets. This version does NOT delete previous backups.
.NOTES
Author: Your Name/AI Assistant
Version: 3.7 (Optimized for Token Count)
Place this script in the root of your Android project and run it from there.
#>

param (
    [string]$ProjectRoot = (Get-Location).Path
)

# --- Configuration ---
# Top-level directories to ALWAYS ignore when presenting module choices
$IgnoreModuleDirs = @(
    "build", "gradle", "captures"
)

# Directories to exclude from processing inside selected modules (uses -like)
$ExcludeSubDirsPatterns = @(
    "*\build\*", "*\.cxx\*", "*\generated\*", "*\debug\*", "*\release\*",
    "*\.*" # Exclude any directory starting with a period (e.g., .git, .idea) and its contents.
)

# Specific files or patterns to always exclude (uses -like)
$ExcludeFilesPatterns = @(
    "*.apk", "*.aab", "*.jar", "*.keystore", "*.iml",
    "local.properties", ".DS_Store", "*.log", "*.bak", "*.tmp",
    "output-metadata.json",
    # This script itself and its output
    "*.ps1", "project_context_for_ai_*.txt"
)

# Text file extensions/names to automatically include if found
$IncludeTextExtensionsOrNames = @(
    ".kt", ".java", ".scala", ".xml", ".gradle", "gradle.properties",
    "settings.gradle", ".pro", ".json", ".yaml", ".yml", ".md", ".txt",
    ".gitignore", ".sh", ".bat", "gradlew", "build.gradle.kts", "settings.gradle.kts",
    "gradle-wrapper.properties", "libs.versions.toml", "LICENSE"
)

# Common non-text asset extensions to list if found
$NonTextAssetExtensions = @(
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ttf", ".otf",
    ".woff", ".woff2", ".mp3", ".wav", ".ogg", ".aac", ".mp4", ".mov",
    ".webm", ".zip", ".tar", ".gz", ".rar", ".so", ".dll", ".dylib",
    ".db", ".sqlite"
)
# --- End Configuration ---

# --- Helper Functions ---
function Test-PathAgainstPatterns {
    param([string]$Path, [string[]]$Patterns)
    foreach ($pattern in $Patterns) { if ($Path -like $pattern) { return $true } }
    return $false
}

function Is-TextFileToInclude {
    param([System.IO.FileInfo]$FileItem)
    $name = $FileItem.Name.ToLower()
    $ext = $FileItem.Extension.ToLower()
    foreach ($entry in $IncludeTextExtensionsOrNames) {
        if ($entry.StartsWith(".")) { if ($ext -eq $entry) { return $true } }
        elseif ($name -eq $entry.ToLower()) { return $true }
    }
    return $false
}

function Is-NonTextAssetToList {
    param([System.IO.FileInfo]$FileItem)
    return $NonTextAssetExtensions -contains $FileItem.Extension.ToLower()
}

$projectTreeOutput = [System.Collections.Generic.List[string]]::new()
function Generate-ProjectTree {
    param([string]$Path = $ProjectRoot, [string]$Indent = "", [int]$Depth = 0, [int]$MaxDepth = 20, [hashtable]$FileStatusMap)
    if ($Depth -ge $MaxDepth) { return }
    try {
        $items = Get-ChildItem -Path $Path -Force -ErrorAction SilentlyContinue | Where-Object {
            -not ($_.PSIsContainer -and $_.Name.StartsWith('.'))
        } | Sort-Object -Property @{ Expression = { $_.PSIsContainer }; Descending = $true }, Name
        if ($null -eq $items) { return }
    } catch { return }
    $lastItem = $items[-1]
    foreach ($item in $items) {
        $isLast = $item -eq $lastItem
        $marker = if ($isLast) { "\--" } else { "+--" }
        $connection = if ($isLast) { "   " } else { "|  " }
        $status = $FileStatusMap[$item.FullName]
        $statusMarker = if ($status) { " [$($status)]" } else { "" }
        $projectTreeOutput.Add("$Indent$marker $($item.Name)$statusMarker")

        # *** OPTIMIZATION: Only recurse into directories that are not marked for exclusion. ***
        if ($item.PSIsContainer -and $status -ne "DIR EXCLUDED")
        {
            Generate-ProjectTree -Path $item.FullName -Indent "$Indent$connection" -Depth ($Depth + 1) -MaxDepth $MaxDepth -FileStatusMap $FileStatusMap
        }
    }
}

# --- Main Script ---
Write-Host "Starting Android project backup for AI analysis..." -ForegroundColor Green
Write-Host "Project root: $ProjectRoot"

# --- Step 1: Discover available modules/folders ---
$moduleCandidates = Get-ChildItem -Path $ProjectRoot -Directory -Depth 0 | Where-Object {
    ($_.Name -notin $IgnoreModuleDirs) -and ($_.Name -notlike ".*")
} | Select-Object Name, FullName

if ($moduleCandidates.Count -eq 0) {
    Write-Error "No potential modules/folders found to include in '$ProjectRoot'. Cannot proceed."
    exit
}

# --- Step 2: Interactive Toggling of Modules ---
Write-Host "`nToggle modules to include. Default is (Y)es." -ForegroundColor Cyan
$selectedModules = foreach ($module in $moduleCandidates) {
    $choice = Read-Host -Prompt "  Include module '$($module.Name)'? (Y/n)"
    if ($choice -eq '' -or $choice.ToLower() -eq 'y') {
        Write-Host "    -> INCLUDING $($module.Name)" -ForegroundColor Green
        $module
    } else {
        Write-Host "    -> SKIPPING $($module.Name)" -ForegroundColor Yellow
    }
}

if (-not $selectedModules) {
    Write-Warning "No modules were included. Processing root files only."
}
$selectedModulePaths = if($selectedModules) { $selectedModules.FullName } else { @() }

# --- Step 3: Setup output file ---
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outputFileName = "project_context_for_ai_$timestamp.txt"
$outputFilePath = Join-Path -Path $ProjectRoot -ChildPath $outputFileName
Write-Host "`nOutput file will be: $outputFilePath"

# --- Step 4: Full scan and categorization based on selection ---
Write-Host "`nCategorizing all project files..." -ForegroundColor Cyan
$allItems = Get-ChildItem -Path $ProjectRoot -Recurse -Force -ErrorAction SilentlyContinue

$filesToProcess = [System.Collections.Generic.List[System.IO.FileInfo]]::new()
$nonTextAssetsFound = [System.Collections.Generic.List[System.IO.FileInfo]]::new()
$includedRootFiles = [System.Collections.Generic.List[string]]::new()
$fileStatusMap = @{}

foreach ($item in $allItems) {
    # Determine if the item is inside a selected module
    $isInsideSelectedModule = $false
    foreach ($modulePath in $selectedModulePaths) {
        if ($item.FullName.StartsWith($modulePath)) {
            $isInsideSelectedModule = $true
            break
        }
    }

    # Determine if the item is a file in the root directory
    $isRootFile = (-not $item.PSIsContainer) -and ($item.PSParentPath -eq $ProjectRoot)

    # Set status for top-level modules themselves
    if ($item.PSIsContainer -and $item.PSParentPath -eq $ProjectRoot) {
        if ($item.FullName -in $selectedModulePaths) {
            $fileStatusMap[$item.FullName] = "MODULE INCLUDED"
        } elseif (($item.Name -in $IgnoreModuleDirs) -or ($item.Name -like ".*")) {
            $fileStatusMap[$item.FullName] = "MODULE IGNORED"
        } else {
            $fileStatusMap[$item.FullName] = "MODULE SKIPPED"
        }
    }

    # Set DIR EXCLUDED status for directories matching exclusion patterns
    if ($item.PSIsContainer) {
        if (Test-PathAgainstPatterns -Path $item.FullName -Patterns $ExcludeSubDirsPatterns)
        {
            $fileStatusMap[$item.FullName] = "DIR EXCLUDED"
        }
        continue # Skip further processing for all directories in this initial categorization loop
    }

    # Process files within selected modules OR at the root level
    if ($isInsideSelectedModule -or $isRootFile) {
        $parentDirFullName = Split-Path $item.FullName -Parent
        $isInExcludedSubDir = Test-PathAgainstPatterns -Path $parentDirFullName -Patterns $ExcludeSubDirsPatterns
        $isExcludedFile = Test-PathAgainstPatterns -Path $item.Name -Patterns $ExcludeFilesPatterns

        if ($isInExcludedSubDir -or $isExcludedFile) {
            $fileStatusMap[$item.FullName] = "EXCLUDED"
            continue
        }

        if (Is-TextFileToInclude -FileItem $item) {
            $filesToProcess.Add($item)
            $fileStatusMap[$item.FullName] = "INCLUDED"
            if($isRootFile) { $includedRootFiles.Add($item.Name) }
        } elseif (Is-NonTextAssetToList -FileItem $item) {
            $nonTextAssetsFound.Add($item)
            $fileStatusMap[$item.FullName] = "ASSET"
            if($isRootFile) { $includedRootFiles.Add($item.Name) }
        }
    }
}

Write-Host "Found $($filesToProcess.Count) text files to include."
Write-Host "Found $($nonTextAssetsFound.Count) non-text assets to list."

# --- Step 5: Write output file ---
Set-Content -Path $outputFilePath -Value "Android Project Backup for AI Analysis`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "Generated on: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n" -Encoding UTF8
$includedModuleNames = if ($selectedModules) { $selectedModules.Name -join ', ' } else { "None" }
$includedItemsSummary = $includedModuleNames
if($includedRootFiles.Count -gt 0) {
    $includedItemsSummary += " (and root files: $($includedRootFiles -join ', '))"
}
Add-Content -Path $outputFilePath -Value "Included Modules/Files: $includedItemsSummary" -Encoding UTF8
Add-Content -Path $outputFilePath -Value "`n`n" -Encoding UTF8

Write-Host "Generating comprehensive project tree..." -ForegroundColor Cyan
$projectTreeOutput.Add((Split-Path $ProjectRoot -Leaf))
Generate-ProjectTree -FileStatusMap $fileStatusMap
Add-Content -Path $outputFilePath -Value "--- PROJECT FILE TREE (Status based on module selection) ---`n" -Encoding UTF8
Add-Content -Path $outputFilePath -Value ($projectTreeOutput -join "`n") -Encoding UTF8
Add-Content -Path $outputFilePath -Value "`n`n" -Encoding UTF8

if ($filesToProcess.Count -gt 0) {
    Add-Content -Path $outputFilePath -Value "--- INCLUDED FILE CONTENTS (from selected modules & root) ---`n" -Encoding UTF8
    Add-Content -Path $outputFilePath -Value "Each file begins with a '--- FILE: [relative_path] ---' header.`n`n`n" -Encoding UTF8
    Write-Host "`nWriting included text files to output..." -ForegroundColor Cyan
    foreach ($file in ($filesToProcess | Sort-Object FullName)) {
        $relativeFilePath = $file.FullName.Substring($ProjectRoot.Length).TrimStart("\/").Replace("\", "/")
        Add-Content -Path $outputFilePath -Value "--- FILE: $relativeFilePath ---`n" -Encoding UTF8
        try {
            $fileContent = Get-Content -Path $file.FullName -Raw -Encoding UTF8 -ErrorAction Stop
            $sanitizedContent = $fileContent -replace "\x00", "[NULL_BYTE]"
            Add-Content -Path $outputFilePath -Value $sanitizedContent -Encoding UTF8 -NoNewline
        } catch { Add-Content -Path $outputFilePath -Value "[Error reading file: $($_.Exception.Message)]`n" -Encoding UTF8 }
        Add-Content -Path $outputFilePath -Value "`n`n" -Encoding UTF8
    }
}

if ($nonTextAssetsFound.Count -gt 0) {
    Add-Content -Path $outputFilePath -Value "--- NON-TEXT ASSET FILE LIST (from selected modules & root) ---`n" -Encoding UTF8
    $nonTextAssetsFound | Sort-Object FullName | ForEach-Object {
        $relativeAssetPath = $_.FullName.Substring($ProjectRoot.Length).TrimStart("\/").Replace("\", "/")
        Add-Content -Path $outputFilePath -Value "- $relativeAssetPath`n" -Encoding UTF8
    }
    Add-Content -Path $outputFilePath -Value "`n`n" -Encoding UTF8
}

Add-Content -Path $outputFilePath -Value "--- END OF BACKUP ---`n" -Encoding UTF8
Write-Host "`n✅ Project backup complete!" -ForegroundColor Green
Write-Host "Output saved to: $outputFilePath"