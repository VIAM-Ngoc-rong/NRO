$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "1. Dang bien dich cac file Java..." -ForegroundColor Cyan
javac -proc:none -encoding UTF-8 -d build/classes -cp "build/classes;lib/*" `
    src/nro/models/data/LocalManager.java `
    src/nro/models/consts/ConstTask.java `
    src/nro/models/data/DataGame.java `
    src/nro/models/data/ItemData.java `
    src/nro/models/network/Network.java `
    src/nro/models/network/MessageSendCollect.java `
    src/nro/models/database/ShopDAO.java `
    src/nro/models/database/MrBlue.java `
    src/nro/models/database/PlayerDAO.java `
    src/nro/models/event/EventManager.java `
    src/nro/models/event/VuaHung.java `
    src/nro/models/server/ServerManager.java `
    src/nro/models/server/AutoMaintenance.java `
    src/nro/models/server/Maintenance.java `
    src/nro/models/server/AdminWebServer.java `
    src/nro/models/server/ServerNotify.java `
    src/nro/models/server/Manager.java `
    src/nro/models/server/Command.java `
    src/nro/models/server/Controller.java `
    src/nro/models/player/Inventory.java `
    src/nro/models/player/NPoint.java `
    src/nro/models/player/SetClothes.java `
    src/nro/models/player/Player.java `
    src/nro/models/player/Pet.java `
    src/nro/models/player_badges/Badges.java `
    src/nro/models/player_badges/BadgesData.java `
    src/nro/models/player_badges/BagesTemplate.java `
    src/nro/models/player_badges/BadgesService.java `
    src/nro/models/task/BadgesTaskService.java `
    src/nro/models/mob/Mob.java `
    src/nro/models/mob/MobMe.java `
    src/nro/models/mob_bigboss/MayDoSucManh.java `
    src/nro/models/map/Zone.java `
    src/nro/models/map/service/ChangeMapService.java `
    src/nro/models/map/service/MapService.java `
    src/nro/models/services/EffectSkillService.java `
    src/nro/models/services/InventoryService.java `
    src/nro/models/services/ItemService.java `
    src/nro/models/services/Service.java `
    src/nro/models/services/SkillService.java `
    src/nro/models/services_func/UseItem.java `
    src/nro/models/services_func/Trade.java `
    src/nro/models/services_func/TransactionService.java `
    src/nro/models/combine/Combine.java `
    src/nro/models/combine/CombineSystem.java `
    src/nro/models/combine/CombineService.java `
    src/nro/models/combine/PhaLeHoaTrangBi.java `
    src/nro/models/combine/NangCapVatPham.java `
    src/nro/models/combine/ChuyenHoaTrangBi_Vang.java `
    src/nro/models/combine/ChuyenHoaTrangBi_Ngoc.java `
    src/nro/models/combine/TaiCheDoThan.java `
    src/nro/models/combine/PhanRaTrangBiKichHoat.java `
    src/nro/models/combine/PhanRaDoSao.java `
    src/nro/models/combine/DapDoKichHoat.java `
    src/nro/models/combine/CheTaoTrangBiThienSu.java `
    src/nro/models/combine/TinhLuyenDaNguSac.java `
    src/nro/models/npc/NpcFactory.java `
    src/nro/models/npc_list/BaHatMit.java `
    src/nro/models/npc_list/Bill.java `
    src/nro/models/npc_list/BoMong.java `
    src/nro/models/npc_list/KyGui.java `
    src/nro/models/npc_list/QuyLaoKame.java `
    src/nro/models/npc_list/ToSuKaio.java `
    src/nro/models/npc_list/OngGohan.java `
    src/nro/models/npc_list/OngMoori.java `
    src/nro/models/npc_list/OngParagus.java `
    src/nro/models/utils/SkillUtil.java `
    src/nro/models/shop/ShopService.java `
    src/nro/models/matches/dai_hoi_vo_thuat/ClanMartialArtsService.java `
    src/nro/models/shop_ky_gui/ConsignShopService.java `
    src/nro/models/task/SideTask.java `
    src/nro/models/services/TaskService.java `
    src/nro/models/boss/Boss.java `
    src/nro/models/boss/Broly/Broly.java `
    src/nro/models/boss/Broly/SuperBroly.java `
    src/nro/models/boss/Broly/Broly18h.java `
    src/nro/models/boss/Broly/SieuBroly18h.java `
    src/nro/models/boss/Broly/KidBuWorld.java `
    src/nro/models/boss/Broly/MabuMapWorld.java `
    src/nro/models/boss/yardrat/Yardart.java `
    src/nro/models/boss/vo_dai_hat_mit/DeathOrAliveArena.java `
    src/nro/models/boss/vo_dai_hat_mit/BongBang.java `
    src/nro/models/boss/vo_dai_hat_mit/Dracula.java `
    src/nro/models/boss/vo_dai_hat_mit/NguoiVoHinh.java `
    src/nro/models/boss/vo_dai_hat_mit/ThoDauBac.java `
    src/nro/models/boss/vo_dai_hat_mit/VuaQuySaTang.java `
    src/nro/models/boss/trai_dat/BIDO.java `
    src/nro/models/boss/trai_dat/BOJACK.java `
    src/nro/models/boss/trai_dat/BUJIN.java `
    src/nro/models/boss/trai_dat/KOGU.java `
    src/nro/models/boss/trai_dat/SUPER_BOJACK.java `
    src/nro/models/boss/trai_dat/ZANGYA.java `
    src/nro/models/boss/event/Halloween/BiMa.java `
    src/nro/models/boss/event/Halloween/Doi.java `
    src/nro/models/boss/event/Halloween/MaTroi.java `
    src/nro/models/boss/Boss_mini/Odo.java `
    src/nro/models/boss/cumber/Cumber.java `
    src/nro/models/boss/MajinBuu_12h/BuiBui.java `
    src/nro/models/boss/MajinBuu_12h/BuiBui2.java `
    src/nro/models/boss/MajinBuu_12h/Cadic.java `
    src/nro/models/boss/MajinBuu_12h/Drabura.java `
    src/nro/models/boss/MajinBuu_12h/Drabura2.java `
    src/nro/models/boss/MajinBuu_12h/Drabura3.java `
    src/nro/models/boss/MajinBuu_12h/Goku.java `
    src/nro/models/boss/MajinBuu_12h/Mabu.java `
    src/nro/models/boss/MajinBuu_12h/Yacon.java `
    src/nro/models/map/phoban/RedRibbonHQ.java `
    src/nro/models/boss/doanh_trai/TrungUyTrang.java `
    src/nro/models/boss/doanh_trai/TrungUyXanhLo.java `
    src/nro/models/boss/doanh_trai/TrungUyThep.java `
    src/nro/models/boss/doanh_trai/NinjaAoTim.java `
    src/nro/models/boss/doanh_trai/NinjaClone.java `
    src/nro/models/boss/doanh_trai/RobotVeSi.java `
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
    "nro\models\consts",
    "nro\models\data",
    "nro\models\network",
    "nro\models\database",
    "nro\models\event",
    "nro\models\server",
    "nro\models\player",
    "nro\models\player_badges",
    "nro\models\mob",
    "nro\models\mob_bigboss",
    "nro\models\map",
    "nro\models\map\service",
    "nro\models\map\phoban",
    "nro\models\services",
    "nro\models\services_func",
    "nro\models\combine",
    "nro\models\npc",
    "nro\models\npc_list",
    "nro\models\utils",
    "nro\models\shop",
    "nro\models\matches\dai_hoi_vo_thuat",
    "nro\models\shop_ky_gui",
    "nro\models\task",
    "nro\models\boss",
    "nro\models\boss\Broly",
    "nro\models\boss\yardrat",
    "nro\models\boss\vo_dai_hat_mit",
    "nro\models\boss\trai_dat",
    "nro\models\boss\event\Halloween",
    "nro\models\boss\Boss_mini",
    "nro\models\boss\cumber",
    "nro\models\boss\MajinBuu_12h",
    "nro\models\boss\doanh_trai",
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

Write-Host "3. Dang day code len GitHub..." -ForegroundColor Cyan
Set-Location "E:\NRO"
git add -A
git commit -m "Update"
git push origin main
Set-Location $PSScriptRoot
Write-Host "DA DAY LEN GITHUB THANH CONG!" -ForegroundColor Green


