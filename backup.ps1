<#
.SYNOPSIS
    The "Strict-Diet" Extraction Script.
.DESCRIPTION
    Inverts the backup paradigm. Instead of blacklisting garbage, it whitelists
    only the vital organs (Source code, build files, docs) required for an LLM
    to understand the project's entire structural reality.
#>

$ScriptName = "backup_precision.ps1"
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$PSScriptRoot = Get-Location
$BackupFile = Join-Path -Path $PSScriptRoot -ChildPath "project_source_only_${Timestamp}.txt"

# The Absolute Whitelist: If it isn't one of these, the machine doesn't need to read it.
$AllowedExtensions = @(
    ".kt", ".java",                                      # Android Logic
    ".cpp", ".hpp", ".h", ".c", ".cc", ".cxx",           # Native Bridge / Engine
    ".xml",                                              # Manifests / Layouts
    ".gradle", ".kts", ".properties", ".toml", ".pro",   # Build / Config
    ".md"                                                # Documentation
)

# Universal Blackholes (To speed up traversal)
$HardExcludedDirs = @(".git", ".gradle", ".idea", "build", ".cxx", "node_modules", ".ralph", "libs")

$CustomExcludedPaths = @()

# --- .aiexclude Assimilation ---
if (Test-Path "$PSScriptRoot\.aiexclude") {
    Write-Host "Parsing .aiexclude..." -ForegroundColor Magenta
    $aiExcludeContent = Get-Content "$PSScriptRoot\.aiexclude"
    foreach ($line in $aiExcludeContent) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) { continue }
        $cleanLine = $line.Trim().Replace('/', [System.IO.Path]::DirectorySeparatorChar).Replace('\', [System.IO.Path]::DirectorySeparatorChar)
        $CustomExcludedPaths += $cleanLine
    }
}

function Test-IsExcluded {
    param ([System.IO.FileInfo]$Item, [string]$RootPath)

    $normalizedPath = $Item.FullName.Replace($RootPath, "").TrimStart("\/")
    $pathParts = $normalizedPath -split "[\\/]"

    # 1. Traversal Optimization: Drop known blackholes immediately
    foreach ($part in $pathParts) {
        if ($HardExcludedDirs -contains $part) { return $true }
    }

    # 2. .aiexclude Checks
    foreach ($customPath in $CustomExcludedPaths) {
        if ($normalizedPath -like "*$customPath*") { return $true }
    }

    # 3. The Strict Whitelist (If it's a file)
    if (-not $Item.PSIsContainer) {
        $ext = $Item.Extension.ToLower()
        # Explicit exception for files without extensions but specific names (like CMakeLists.txt)
        if ($Item.Name -eq "CMakeLists.txt") { return $false }

        if ($AllowedExtensions -notcontains $ext) { return $true }
    }

    return $false
}

Write-Host "Initializing Precision Extraction..." -ForegroundColor Green

Set-Content -Path $BackupFile -Value "# PROJECT SOURCE EXTRACT: $Timestamp"
Add-Content -Path $BackupFile -Value "# NOTE: Strict whitelist enforced. Only core architectural files included."
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
        if ($content.Contains("`0")) { return } # Catch silent binaries

        $relativePath = $file.FullName.Replace($PSScriptRoot, '.')
        Write-Host "  + Extracted: $relativePath" -ForegroundColor Cyan

        Add-Content -Path $BackupFile -Value "`n## FILE: $relativePath"
        Add-Content -Path $BackupFile -Value $content.Trim()
    }
}

Write-Host "---"
Write-Host "Extraction complete. The corpse is cleanly butchered and ready for the next machine." -ForegroundColor Green
