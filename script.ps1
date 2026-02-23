# ──────────────────────────────────────────────────────────
# Force rewrite ALL project files as UTF-8 without BOM
# Run from C:\pkmn\pokemon-inventory:
#   powershell -ExecutionPolicy Bypass -File fix-bom-force.ps1
# ──────────────────────────────────────────────────────────

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$count = 0

Get-ChildItem -Recurse -Include *.java, *.properties, *.xml | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)

    # Strip BOM if present (EF BB BF)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $bytes = $bytes[3..($bytes.Length - 1)]
    }

    [System.IO.File]::WriteAllBytes($_.FullName, $bytes)
    Write-Host "Rewrote: $($_.FullName)" -ForegroundColor Yellow
    $script:count++
}

Write-Host ""
Write-Host "Done! Rewrote $count file(s) as UTF-8 without BOM." -ForegroundColor Green
Write-Host ""
Write-Host "Now in IntelliJ:" -ForegroundColor Cyan
Write-Host "  1. File -> Invalidate Caches -> check all boxes -> Invalidate and Restart" -ForegroundColor Cyan
Write-Host "  2. After restart: Build -> Rebuild Project" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"