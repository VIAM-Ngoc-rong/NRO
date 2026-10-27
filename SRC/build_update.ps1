$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "1. Dang bien dich cac file Java..." -ForegroundColor Cyan
javac -proc:none -encoding UTF-8 -d build/classes -cp "build/classes;lib/*" `
    src/nro/models/data/LocalManager.java `
    src/nro/models/data/DataGame.java `
    src/nro/models/data/ItemData.java `
    src/nro/models/database/ShopDAO.java `
    src/nro/models/event/EventManager.java `
    src/nro/models/server/ServerManager.java `
    src/nro/models/server/AdminWebServer.java `
    src/nro/models/server/ServerNotify.java `
    src/nro/models/server/Manager.java `
    src/nro/models/server/Command.java `
    src/nro/models/server/Controller.java `
    src/nro/models/player/Inventory.java `
    src/nro/models/player/NPoint.java `
    src/nro/models/player/SetClothes.java `
    src/nro/models/player/Player.java `
    src/nro/models/mob/Mob.java `
    src/nro/models/map/Zone.java `
    src/nro/models/map/service/ChangeMapService.java `
    src/nro/models/services/EffectSkillService.java `
    src/nro/models/services/InventoryService.java `
    src/nro/models/services/ItemService.java `
    src/nro/models/services/Service.java `
    src/nro/models/services/SkillService.java `
    src/nro/models/services_func/UseItem.java `
    src/nro/models/combine/Combine.java `
    src/nro/models/combine/CombineService.java `
    src/nro/models/combine/ChuyenHoaTrangBi_Vang.java `
    src/nro/models/combine/ChuyenHoaTrangBi_Ngoc.java `
    src/nro/models/combine/TaiCheDoThan.java `
    src/nro/models/combine/PhanRaTrangBiKichHoat.java `
    src/nro/models/combine/PhanRaDoSao.java `
    src/nro/models/combine/DapDoKichHoat.java `
    src/nro/models/npc/NpcFactory.java `
    src/nro/models/npc_list/BaHatMit.java `
    src/nro/models/npc_list/BoMong.java `
    src/nro/models/npc_list/KyGui.java `
    src/nro/models/shop_ky_gui/ConsignShopService.java `
    src/nro/models/task/SideTask.java `
    src/nro/models/services/TaskService.java `
    src/nro/models/boss/Boss.java `
    src/nro/models/boss/Boss_Manager/BossManager.java

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
    "nro\models\database",
    "nro\models\event",
    "nro\models\server",
    "nro\models\player",
    "nro\models\mob",
    "nro\models\map",
    "nro\models\map\service",
    "nro\models\services",
    "nro\models\services_func",
    "nro\models\combine",
    "nro\models\npc",
    "nro\models\npc_list",
    "nro\models\shop_ky_gui",
    "nro\models\task",
    "nro\models\boss",
    "nro\models\boss\Boss_Manager"
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
if (Test-Path "E:\NRO\Server_Local\20.jar") {
    Copy-Item $jarPath "E:\NRO\Server_Local\20.jar" -Force
}

Write-Host "HOAN TAT! Da cap nhat dist/NgocRongOnline.jar va 20.jar thanh cong!" -ForegroundColor Green

