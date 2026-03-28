<#
.SYNOPSIS
    The "Strict-Diet" Extraction Script - Scoped Edition.
.DESCRIPTION
    Inverts the backup paradigm. Instead of blacklisting garbage, it whitelists
    only the vital organs required for an LLM to understand the project.
    Supports targeting specific scopes to save tokens.
.PARAMETER Scope
    A comma-separated list of functional areas to include.
    Available Scopes: Global (Default), UI, Physics, CV, Domain, Data, All.
#>

param (
    [Parameter(Mandatory=$false)]
    [string]$Scope = "Global"
)

$ScriptName = "backup_scoped.ps1"
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$PSScriptRoot = Get-Location
$BackupFile = Join-Path -Path $PSScriptRoot -ChildPath "project_source_scoped_${Timestamp}.txt"

# The Absolute Whitelist: Extensions allowed for source files
$AllowedExtensions = @(
    ".kt", ".java",                                      # Android Logic
    ".cpp", ".hpp", ".h", ".c", ".cc", ".cxx",           # Native Bridge / Engine
    ".xml",                                              # Manifests / Layouts
    ".gradle", ".kts", ".properties", ".toml", ".pro",   # Build / Config
    ".md", ".ipynb", ".json"                             # Docs / Notebooks / Config
)

# Universal Blackholes (To speed up traversal)
$HardExcludedDirs = @(".git", ".gradle", ".idea", "build", ".cxx", "node_modules", ".ralph", "libs", ".claude", ".vscode", ".superpowers", ".worktrees")

$MaxFileSizeKB = 1024

# Scope Definitions (Mappings to file/directory paths)
$Scopes = @{
    "Global"  = @(
        "docs\*",
        "AGENTS.md", "README.md", "version.properties", "build.gradle.kts", "settings.gradle.kts",
        "gradle\libs.versions.toml", "local.properties"
    )
    "UI"      = @("app\src\main\java\com\hereliesaz\cuedetat\ui\*", "app\src\main\res\*")
    "Physics" = @(
        "app\src\main\java\com\hereliesaz\cuedetat\domain\Masse*",
        "app\src\main\java\com\hereliesaz\cuedetat\domain\Spin*",
        "app\src\main\java\com\hereliesaz\cuedetat\ui\hatemode\*"
    )
    "CV"      = @(
        "app\src\main\java\com\hereliesaz\cuedetat\data\Vision*",
        "app\src\main\java\com\hereliesaz\cuedetat\data\TableScan*",
        "app\src\main\java\com\hereliesaz\cuedetat\ui\composables\tablescan\*",
        "ml\*"
    )
    "Domain"  = @("app\src\main\java\com\hereliesaz\cuedetat\domain\*")
    "Data"    = @(
        "app\src\main\java\com\hereliesaz\cuedetat\data\*",
        "app\src\main\java\com\hereliesaz\cuedetat\network\*"
    )
}

$RequestedScopes = $Scope.Split(",") | ForEach-Object { $_.Trim() }
if ($RequestedScopes -contains "All") {
    $RequestedScopes = $Scopes.Keys
}

Write-Host "Initializing Scoped Precision Extraction..." -ForegroundColor Green
Write-Host "Target Scopes: $($RequestedScopes -join ', ')" -ForegroundColor Cyan

function Test-IsExcluded {
    param ([System.IO.FileInfo]$Item, [string]$RootPath)

    # Use system-independent normalization (slash only)
    $normalizedPath = $Item.FullName.Replace($RootPath, "").Replace("\", "/").Trim("/")
    $pathParts = $normalizedPath -split "/"

    # 1. Traversal Optimization: Drop known blackholes immediately
    foreach ($part in $pathParts) {
        if ($HardExcludedDirs -contains $part) { return $true }
    }

    # 2. Scope Matching
    $isInScope = $false
    foreach ($reqScope in $RequestedScopes) {
        if ($Scopes.ContainsKey($reqScope)) {
            foreach ($pattern in $Scopes[$reqScope]) {
                $p = $pattern.Replace("\", "/")
                # Using -like for wildcard matching
                if ($normalizedPath -like "*$p*") {
                    $isInScope = $true
                    break
                }
            }
        }
        if ($isInScope) { break }
    }
    if (-not $isInScope) { return $true }

    # 3. Whitelist & Quality Checks
    if (-not $Item.PSIsContainer) {
        # File Size Limit
        if ($Item.Length -gt ($MaxFileSizeKB * 1024)) { return $true }

        $ext = $Item.Extension.ToLower()

        # Explicit exception for specific files
        if ($Item.Name -eq "CMakeLists.txt" -or $Item.Name -eq "AGENTS.md") { return $false }

        if ($AllowedExtensions -notcontains $ext) { return $true }

        # Explicit exclusion for binaries matching extensions
        if ($ext -eq ".tflite" -or $ext -eq ".pb" -or $ext -eq ".bin") { return $true }
    }

    return $false
}

Set-Content -Path $BackupFile -Value "# PROJECT SOURCE EXTRACT: $Timestamp"
Add-Content -Path $BackupFile -Value "# NOTE: Scoped extraction active. Scopes: $($RequestedScopes -join ', ')"
Add-Content -Path $BackupFile -Value "# NOTE: File size limit: $MaxFileSizeKB KB."
Add-Content -Path $BackupFile -Value "`n# --- FILE CONTENTS ---"

Get-ChildItem -Path $PSScriptRoot -Recurse -File | ForEach-Object {
    $file = $_

    if (Test-IsExcluded -Item $file -RootPath $PSScriptRoot) { return }

    try {
        $content = Get-Content $file.FullName -Raw
    } catch {
        Write-Warning "Locked: $($file.Name)"
        return
    }

    if (-not [string]::IsNullOrWhiteSpace($content)) {
        if ($content.Contains("`0")) { return } # Binaries check

        $relativePath = $file.FullName.Replace($PSScriptRoot, '.')
        Write-Host "  + Extracted: $relativePath" -ForegroundColor Cyan

        Add-Content -Path $BackupFile -Value "`n## FILE: $relativePath"
        Add-Content -Path $BackupFile -Value $content.Trim()
    }
}

Write-Host "---"
Write-Host "Extraction complete: $BackupFile" -ForegroundColor Green
