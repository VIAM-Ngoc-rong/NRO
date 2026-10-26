$ErrorActionPreference = "Stop"

# Tìm file .sql cần sửa (ưu tiên file backup mới nhất từ Downloads hoặc E:\NRO\database team2026.sql)
$candidates = @()
try {
    if ($env:USERPROFILE -and (Test-Path "$env:USERPROFILE\Downloads" -ErrorAction SilentlyContinue)) {
        $candidates += Get-ChildItem -Path "$env:USERPROFILE\Downloads" -Filter "*team2026*.sql" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending
    }
} catch {}

if (Test-Path "E:\NRO\database team2026.sql" -ErrorAction SilentlyContinue) {
    $candidates += Get-Item "E:\NRO\database team2026.sql"
}

foreach ($file in $candidates) {
    Write-Host "Dang xu ly file SQL: $($file.FullName) ..." -ForegroundColor Cyan
    $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    $fixed = $content `
        -replace 'utf8mb4_0900_ai_ci', 'utf8mb4_general_ci' `
        -replace 'utf8mb4_0900_as_cs', 'utf8mb4_bin' `
        -replace 'utf8mb3_general_ci', 'utf8mb4_general_ci' `
        -replace 'utf8mb3_unicode_ci', 'utf8mb4_unicode_ci' `
        -replace 'CHARACTER SET utf8mb3', 'CHARACTER SET utf8mb4' `
        -replace 'CHARSET=utf8mb3', 'CHARSET=utf8mb4'

    if ($fixed -notmatch 'DROP TABLE IF EXISTS') {
        $fixed = [regex]::Replace($fixed, '(?m)^CREATE TABLE `([^`]+)`', "DROP TABLE IF EXISTS ``$`1``;`r`nCREATE TABLE ``$`1``")
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($file.FullName, $fixed, $utf8NoBom)
    Write-Host "-> Da sua xong collation MySQL 8.0 -> MariaDB (XAMPP) cho: $($file.FullName)" -ForegroundColor Green
}

# Nếu người dùng có bản backup mới nhất trong Downloads, copy luôn đè vào E:\NRO\database team2026.sql
try {
    $latestDownload = Get-ChildItem -Path "$env:USERPROFILE\Downloads" -Filter "*team2026*.sql" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($latestDownload -ne $null -and $latestDownload.FullName -ne "E:\NRO\database team2026.sql") {
        Copy-Item $latestDownload.FullName "E:\NRO\database team2026.sql" -Force
        Write-Host "-> Da cap nhat ban SQL moi nhat tu Downloads vao E:\NRO\database team2026.sql" -ForegroundColor Green
    }
} catch {}

# Tự động nạp vào MySQL XAMPP nếu tìm thấy C:\xampp\mysql\bin\mysql.exe
try {
    $xamppMysql = "C:\xampp\mysql\bin\mysql.exe"
    if (Test-Path $xamppMysql -ErrorAction SilentlyContinue) {
        Write-Host "Dang nap truc tiep E:\NRO\database team2026.sql vao MySQL XAMPP (database: team2026)..." -ForegroundColor Cyan
        & $xamppMysql -u root -e "DROP DATABASE IF EXISTS team2026; CREATE DATABASE team2026 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
        cmd.exe /c "`"$xamppMysql`" -u root --default-character-set=utf8mb4 team2026 < `"E:\NRO\database team2026.sql`""
        if ($LASTEXITCODE -eq 0) {
            Write-Host "HOAN TAT! Da nap thanh cong toan bo database team2026 vao XAMPP!" -ForegroundColor Green
        } else {
            Write-Host "Luu y: Co loi khi nap qua CLI, ban co the Import file E:\NRO\database team2026.sql truc tiep tren phpMyAdmin." -ForegroundColor Yellow
        }
    } else {
        Write-Host "Da sua xong file E:\NRO\database team2026.sql! Ban co the Import lai tren phpMyAdmin." -ForegroundColor Green
    }
} catch {
    Write-Host "Da sua xong file E:\NRO\database team2026.sql! Ban co the Import lai tren phpMyAdmin." -ForegroundColor Green
}

