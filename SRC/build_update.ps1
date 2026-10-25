$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "1. Dang bien dich cac file Java..." -ForegroundColor Cyan
javac -proc:none -encoding UTF-8 -d build/classes -cp "build/classes;lib/*" `
    src/nro/models/data/DataGame.java `
    src/nro/models/data/ItemData.java `
    src/nro/models/server/Manager.java `
    src/nro/models/player/Inventory.java `
    src/nro/models/services/InventoryService.java `
    src/nro/models/services/Service.java `
    src/nro/models/services_func/UseItem.java `
    src/nro/models/combine/CombineService.java `
    src/nro/models/npc/NpcFactory.java `
    src/nro/models/boss/Boss.java

if ($LASTEXITCODE -ne 0) {
    throw "Loi bien dich javac!"
}

Write-Host "2. Dang cap nhat dist/NgocRongOnline.jar va 20.jar..." -ForegroundColor Cyan
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$jarPath = Join-Path $PSScriptRoot "dist\NgocRongOnline.jar"
$classRoot = Join-Path $PSScriptRoot "build\classes"

$pkgs = @(
    "nro\models\data",
    "nro\models\server",
    "nro\models\player",
    "nro\models\services",
    "nro\models\services_func",
    "nro\models\combine",
    "nro\models\npc",
    "nro\models\boss"
)

$zip = [System.IO.Compression.ZipFile]::Open($jarPath, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    foreach ($pkg in $pkgs) {
        $dir = Join-Path $classRoot $pkg
        if (Test-Path $dir) {
            Get-ChildItem -Path $dir -Filter "*.class" | ForEach-Object {
                $entryName = ($pkg -replace '\\', '/') + "/" + $_.Name
                $existing = $zip.GetEntry($entryName)
                if ($existing -ne $null) {
                    $existing.Delete()
                }
                [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $entryName) | Out-Null
            }
        }
    }
} finally {
    $zip.Dispose()
}

Copy-Item $jarPath (Join-Path $PSScriptRoot "20.jar") -Force
if (Test-Path "E:\NRO\20.jar") {
    Copy-Item $jarPath "E:\NRO\20.jar" -Force
}

Write-Host "HOAN TAT! Da cap nhat dist/NgocRongOnline.jar va 20.jar thanh cong!" -ForegroundColor Green

