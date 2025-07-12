$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$scriptName = $MyInvocation.MyCommand.Name
$backupFile = "backup_$timestamp.txt"

# Get all files in current directory that:
# - don't start with '.'
# - are not the script itself
# - don't match backup_*.txt pattern
$files = Get-ChildItem -File -LiteralPath . | Where-Object {
    $_.Name -notmatch '^\.' -and
            $_.Name -ne $scriptName -and
            $_.Name -notlike 'backup_*.txt'
}

# Concatenate all file contents into the backup file
Get-Content $files.FullName | Out-File $backupFile -Encoding utf8
