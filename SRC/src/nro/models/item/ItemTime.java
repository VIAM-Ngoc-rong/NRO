package nro.models.item;

import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.services.ItemTimeService;

public class ItemTime {

    public static final byte DOANH_TRAI = 0;
    public static final byte BAN_DO_KHO_BAU = 1;
    public static final byte CON_DUONG_RAN_DOC = 2;
    public static final byte KHI_GAS_HUY_DIET = 3;
    public static final byte TIME_KEO_BUA_BAO = 4;
    public static final byte TEXT_NHAN_BUA_MIEN_PHI = 5;

    public static final int TIME_ITEM = 600000;
    public static final int TIME_OPEN_POWER = 8640000;
    public static final int TIME_MAY_DO = 1800000;
    public static final int TIME_MAY_DO2 = 1800000;
    public static final long TIME_CO_BON_LA = 30 * 60 * 1000L;
    public static final int TIME_KILIS = 3600000;
    public static final int TIME_NUOC_MIA1 = 600_000;
    public static final int TIME_NUOC_MIA2 = 600_000;
    public static final int TIME_NUOC_MIA3 = 600_000;

    public static final int TIME_BUA_SANTA = 1800000;
    public static final int TIME_EAT_MEAL = 600000;
    public static final int TIME_CMS = 3600000;
    public static final int TIME_DK = 1800000;
    public static final int TIME_NCD = 1800000;
    public static final int TIME_AUTO_SAN_BOSS = 3_600_000;
    public static final int ICON_AUTO_SAN_BOSS = 12834;
    public static final short ITEM_ID_AUTO_SAN_BOSS = 1115;
    public static final long COST_GOLD_AUTO_SAN_BOSS = 200_000_000L;

    private Player player;

    public boolean isUseBoHuyet;
    public boolean isUseBoKhi;
    public boolean isUseGiapXen;
    public boolean isUseCuongNo;
    public boolean isUseAnDanh;
    public boolean isUseBoHuyet2;
    public boolean isUseBoKhi2;
    public boolean isUseGiapXen2;
    public boolean isUseCuongNo2;
    public boolean isUseAnDanh2;

    public long lastTimeBoHuyet;
    public long lastTimeBoKhi;
    public long lastTimeGiapXen;
    public long lastTimeCuongNo;
    public long lastTimeAnDanh;

    public long lastTimeBoHuyet2;
    public long lastTimeBoKhi2;
    public long lastTimeGiapXen2;
    public long lastTimeCuongNo2;
    public long lastTimeAnDanh2;

    public boolean isUseMayDo;
    public long lastTimeUseMayDo;
    public boolean isUseKhoBauX2;
    public long lastTimeUseKhoBauX2;
    public boolean isUseBuaSanta;
    public long lastTimeBuaSanta;

    public long lastTimeUseCoBonLa;
    public boolean isUseCoBonLa;

    public boolean isUseKilis;
    public long lastTimeUseKilis;

    public boolean isUseNuocMia1;
    public long lastTimeUseNuocMia1;
    public boolean isUseNuocMia2;
    public long lastTimeUseNuocMia2;
    public boolean isUseNuocMia3;
    public long lastTimeUseNuocMia3;

    public boolean isOpenPower;
    public long lastTimeOpenPower;

    public boolean isUseTDLT;
    public long lastTimeUseTDLT;
    public int timeTDLT;

    public boolean isUseRX;
    public long lastTimeUseRX;
    public int timeRX;

    public boolean isUseCMS;
    public long lastTimeUseCMS;

    public boolean isUseNCD;
    public long lastTimeUseNCD;

    public boolean isUseGTPT;
    public long lastTimeUseGTPT;

    public boolean isUseDK;
    public long lastTimeUseDK;

    public boolean isEatMeal;
    public long lastTimeEatMeal;
    public int iconMeal;

    public boolean isEatMeal2;
    public long lastTimeEatMeal2;
    public int iconMeal2;
    public long lastTimeKhauTrang;
    public boolean isUseKhauTrang;
    public boolean isUseAutoSanBoss;
    public long lastTimeAutoSanBoss;
    public long timeLengthKilis;
    public long totalCoBonLaTime;
    public long timeLengthCoBonLa;

    public ItemTime(Player player) {
        this.player = player;
    }

    public void update() {
        if (isEatMeal) {
            if (Util.canDoWithTime(lastTimeEatMeal, TIME_EAT_MEAL)) {
                isEatMeal = false;
                ItemTimeService.gI().removeItemTime(player, iconMeal);
                Service.gI().point(player);
            }
        }
        if (isEatMeal2) {
            if (Util.canDoWithTime(lastTimeEatMeal2, TIME_EAT_MEAL)) {
                isEatMeal2 = false;
                ItemTimeService.gI().removeItemTime(player, iconMeal2);
                Service.gI().point(player);
            }
        }
        if (isUseBoHuyet) {
            if (Util.canDoWithTime(lastTimeBoHuyet, TIME_ITEM)) {
                isUseBoHuyet = false;
                ItemTimeService.gI().removeItemTime(player, 2755);
                Service.gI().point(player);
            }
        }
        if (isUseBoKhi) {
            if (Util.canDoWithTime(lastTimeBoKhi, TIME_ITEM)) {
                isUseBoKhi = false;
                ItemTimeService.gI().removeItemTime(player, 2756);
                Service.gI().point(player);
            }
        }
        if (isUseGiapXen) {
            if (Util.canDoWithTime(lastTimeGiapXen, TIME_ITEM)) {
                isUseGiapXen = false;
                ItemTimeService.gI().removeItemTime(player, 2757);
            }
        }
        if (isUseCuongNo) {
            if (Util.canDoWithTime(lastTimeCuongNo, TIME_ITEM)) {
                isUseCuongNo = false;
                ItemTimeService.gI().removeItemTime(player, 2754);
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh) {
            if (Util.canDoWithTime(lastTimeAnDanh, TIME_ITEM)) {
                isUseAnDanh = false;
                ItemTimeService.gI().removeItemTime(player, 2760);
            }
        }
        if (isUseBoHuyet2) {
            if (Util.canDoWithTime(lastTimeBoHuyet2, TIME_ITEM)) {
                isUseBoHuyet2 = false;
                ItemTimeService.gI().removeItemTime(player, 10714);
                Service.gI().point(player);
            }
        }
        if (isUseBoKhi2) {
            if (Util.canDoWithTime(lastTimeBoKhi2, TIME_ITEM)) {
                isUseBoKhi2 = false;
                ItemTimeService.gI().removeItemTime(player, 10715);
                Service.gI().point(player);
            }
        }
        if (isUseGiapXen2) {
            if (Util.canDoWithTime(lastTimeGiapXen2, TIME_ITEM)) {
                isUseGiapXen2 = false;
                ItemTimeService.gI().removeItemTime(player, 10712);
            }
        }
        if (isUseCuongNo2) {
            if (Util.canDoWithTime(lastTimeCuongNo2, TIME_ITEM)) {
                isUseCuongNo2 = false;
                ItemTimeService.gI().removeItemTime(player, 10716);
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh2) {
            if (Util.canDoWithTime(lastTimeAnDanh2, TIME_ITEM)) {
                isUseAnDanh2 = false;
                ItemTimeService.gI().removeItemTime(player, 10717);
            }
        }
        if (isUseCMS) {
            if (Util.canDoWithTime(lastTimeUseCMS, TIME_CMS)) {
                isUseCMS = false;
                ItemTimeService.gI().removeItemTime(player, 5829);
            }
        }
        if (isUseNCD) {
            if (Util.canDoWithTime(lastTimeUseNCD, TIME_NCD)) {
                isUseNCD = false;
                ItemTimeService.gI().removeItemTime(player, 11173);
            }
        }
        if (isUseGTPT) {
            if (Util.canDoWithTime(lastTimeUseGTPT, TIME_ITEM)) {
                isUseGTPT = false;
                ItemTimeService.gI().removeItemTime(player, 3778);
            }
        }
        if (isUseDK) {
            if (Util.canDoWithTime(lastTimeUseDK, TIME_DK)) {
                isUseDK = false;
                ItemTimeService.gI().removeItemTime(player, 5072);
            }
        }
        if (isOpenPower) {
            if (Util.canDoWithTime(lastTimeOpenPower, TIME_OPEN_POWER)) {
                player.nPoint.limitPower++;
                if (player.nPoint.limitPower > NPoint.MAX_LIMIT) {
                    player.nPoint.limitPower = NPoint.MAX_LIMIT;
                }
                Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã được tăng lên 1 bậc");
                isOpenPower = false;
                ItemTimeService.gI().removeItemTime(player, 3783);
            }
        }
        if (isUseMayDo) {
            if (Util.canDoWithTime(lastTimeUseMayDo, TIME_MAY_DO)) {
                isUseMayDo = false;
                ItemTimeService.gI().removeItemTime(player, 2758);
            }
        }
        if (isUseCoBonLa) {
            long duration = timeLengthCoBonLa > 0 ? timeLengthCoBonLa : TIME_CO_BON_LA;
            if (Util.canDoWithTime(lastTimeUseCoBonLa, duration)) {
                isUseCoBonLa = false;
                timeLengthCoBonLa = 0;
                ItemTimeService.gI().removeItemTime(player, 13618);
            }
        }
        if (isUseKilis) {
            long duration = timeLengthKilis > 0 ? timeLengthKilis : TIME_KILIS;
            if (Util.canDoWithTime(lastTimeUseKilis, duration)) {
                isUseKilis = false;
                timeLengthKilis = 0;
                ItemTimeService.gI().removeItemTime(player, 15359);
            }
        }
        if (isUseNuocMia1) {
            if (Util.canDoWithTime(lastTimeUseNuocMia1, TIME_NUOC_MIA1)) {
                isUseNuocMia1 = false;
                ItemTimeService.gI().removeItemTime(player, 13462);
                Service.gI().point(player);
            }
        }
        if (isUseNuocMia2) {
            if (Util.canDoWithTime(lastTimeUseNuocMia2, TIME_NUOC_MIA2)) {
                isUseNuocMia2 = false;
                ItemTimeService.gI().removeItemTime(player, 13463);
                Service.gI().point(player);
            }
        }
        if (isUseNuocMia3) {
            if (Util.canDoWithTime(lastTimeUseNuocMia3, TIME_NUOC_MIA3)) {
                isUseNuocMia3 = false;
                ItemTimeService.gI().removeItemTime(player, 13464);
                Service.gI().point(player);
            }
        }
        if (isUseBuaSanta) {
            if (Util.canDoWithTime(lastTimeBuaSanta, TIME_BUA_SANTA)) {
                isUseBuaSanta = false;
                ItemTimeService.gI().removeItemTime(player, 13540);
            }
        }
        if (isUseKhoBauX2) {
            if (Util.canDoWithTime(lastTimeUseKhoBauX2, TIME_MAY_DO2)) {
                isUseKhoBauX2 = false;
            }
        }
        if (isUseTDLT) {
            if (Util.canDoWithTime(lastTimeUseTDLT, timeTDLT)) {
                this.isUseTDLT = false;
                ItemTimeService.gI().sendCanAutoPlay(this.player);
                ItemTimeService.gI().removeItemTime(player, 4387);
            }
        }
        if (isUseRX) {
            if (Util.canDoWithTime(lastTimeUseRX, timeRX)) {
                isUseRX = false;
                ItemTimeService.gI().removeItemTime(player, 6581);
                ItemTimeService.gI().removeItemTime(player, 8579);
                Service.gI().point(player);
            }
        }
        if (isUseKhauTrang) {
            if (Util.canDoWithTime(lastTimeKhauTrang, TIME_ITEM)) {
                isUseKhauTrang = false;
                Service.gI().point(player);
            }
        }
        if (isUseAutoSanBoss) {
            if (Util.canDoWithTime(lastTimeAutoSanBoss, TIME_AUTO_SAN_BOSS)) {
                isUseAutoSanBoss = false;
                ItemTimeService.gI().removeItemTime(player, ICON_AUTO_SAN_BOSS);
                if (player != null && player.isAutoHuntBoss) {
                    player.stopAutoHuntBoss();
                    Service.gI().sendThongBao(player, "Đã hết thời gian Máy Dò Săn Boss! Tự Động Săn Boss đã tạm dừng, vui lòng mua hoặc dùng thêm Máy Dò Săn Boss (200 Tr Vàng/1h) để tiếp tục.");
                } else if (player != null) {
                    Service.gI().sendThongBao(player, "Máy Dò Săn Boss đã hết thời gian hiệu lực!");
                }
            }
        }
    }

    public void dispose() {
        this.player = null;
    }
}
