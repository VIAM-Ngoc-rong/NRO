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
    public long timeLengthKilis;
    public long totalCoBonLaTime;
    public long timeLengthCoBonLa;

    public ItemTime(Player player) {
        this.player = player;
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (isEatMeal) {
            if (Util.canDoWithTime(lastTimeEatMeal, TIME_EAT_MEAL - 10000)) {
                lastTimeEatMeal = now;
                ItemTimeService.gI().sendItemTime(player, iconMeal, (int) (TIME_EAT_MEAL / 1000));
            }
        }
        if (isEatMeal2) {
            if (Util.canDoWithTime(lastTimeEatMeal2, TIME_EAT_MEAL - 10000)) {
                lastTimeEatMeal2 = now;
                ItemTimeService.gI().sendItemTime(player, iconMeal2, (int) (TIME_EAT_MEAL / 1000));
            }
        }
        if (isUseBoHuyet) {
            if (Util.canDoWithTime(lastTimeBoHuyet, TIME_ITEM - 10000)) {
                lastTimeBoHuyet = now;
                ItemTimeService.gI().sendItemTime(player, 2755, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseBoKhi) {
            if (Util.canDoWithTime(lastTimeBoKhi, TIME_ITEM - 10000)) {
                lastTimeBoKhi = now;
                ItemTimeService.gI().sendItemTime(player, 2756, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseGiapXen) {
            if (Util.canDoWithTime(lastTimeGiapXen, TIME_ITEM - 10000)) {
                lastTimeGiapXen = now;
                ItemTimeService.gI().sendItemTime(player, 2757, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseCuongNo) {
            if (Util.canDoWithTime(lastTimeCuongNo, TIME_ITEM - 10000)) {
                lastTimeCuongNo = now;
                ItemTimeService.gI().sendItemTime(player, 2754, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseAnDanh) {
            if (Util.canDoWithTime(lastTimeAnDanh, TIME_ITEM - 10000)) {
                lastTimeAnDanh = now;
                ItemTimeService.gI().sendItemTime(player, 2760, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseBoHuyet2) {
            if (Util.canDoWithTime(lastTimeBoHuyet2, TIME_ITEM - 10000)) {
                lastTimeBoHuyet2 = now;
                ItemTimeService.gI().sendItemTime(player, 10714, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseBoKhi2) {
            if (Util.canDoWithTime(lastTimeBoKhi2, TIME_ITEM - 10000)) {
                lastTimeBoKhi2 = now;
                ItemTimeService.gI().sendItemTime(player, 10715, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseGiapXen2) {
            if (Util.canDoWithTime(lastTimeGiapXen2, TIME_ITEM - 10000)) {
                lastTimeGiapXen2 = now;
                ItemTimeService.gI().sendItemTime(player, 10712, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseCuongNo2) {
            if (Util.canDoWithTime(lastTimeCuongNo2, TIME_ITEM - 10000)) {
                lastTimeCuongNo2 = now;
                ItemTimeService.gI().sendItemTime(player, 10716, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseAnDanh2) {
            if (Util.canDoWithTime(lastTimeAnDanh2, TIME_ITEM - 10000)) {
                lastTimeAnDanh2 = now;
                ItemTimeService.gI().sendItemTime(player, 10717, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseCMS) {
            if (Util.canDoWithTime(lastTimeUseCMS, TIME_CMS - 10000)) {
                lastTimeUseCMS = now;
                ItemTimeService.gI().sendItemTime(player, 5829, (int) (TIME_CMS / 1000));
            }
        }
        if (isUseNCD) {
            if (Util.canDoWithTime(lastTimeUseNCD, TIME_NCD - 10000)) {
                lastTimeUseNCD = now;
                ItemTimeService.gI().sendItemTime(player, 11173, (int) (TIME_NCD / 1000));
            }
        }
        if (isUseGTPT) {
            if (Util.canDoWithTime(lastTimeUseGTPT, TIME_ITEM - 10000)) {
                lastTimeUseGTPT = now;
                ItemTimeService.gI().sendItemTime(player, 3778, (int) (TIME_ITEM / 1000));
            }
        }
        if (isUseDK) {
            if (Util.canDoWithTime(lastTimeUseDK, TIME_DK - 10000)) {
                lastTimeUseDK = now;
                ItemTimeService.gI().sendItemTime(player, 5072, (int) (TIME_DK / 1000));
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
            }
        }
        if (isUseMayDo) {
            if (Util.canDoWithTime(lastTimeUseMayDo, TIME_MAY_DO - 10000)) {
                lastTimeUseMayDo = now;
                ItemTimeService.gI().sendItemTime(player, 2758, (int) (TIME_MAY_DO / 1000));
            }
        }
        if (isUseCoBonLa) {
            if (timeLengthCoBonLa <= 0) {
                timeLengthCoBonLa = TIME_CO_BON_LA;
            }
            long timeLeft = (lastTimeUseCoBonLa + timeLengthCoBonLa) - now;
            if (timeLeft <= 10000) {
                lastTimeUseCoBonLa = now;
                timeLengthCoBonLa = TIME_CO_BON_LA;
                ItemTimeService.gI().sendItemTime(player, 13618, (int) (TIME_CO_BON_LA / 1000));
            }
        }
        if (isUseKilis) {
            if (timeLengthKilis <= 0) {
                timeLengthKilis = TIME_KILIS;
            }
            if (Util.canDoWithTime(lastTimeUseKilis, TIME_KILIS - 10000)) {
                lastTimeUseKilis = now;
                timeLengthKilis = TIME_KILIS;
                ItemTimeService.gI().sendItemTime(player, 15359, (int) (TIME_KILIS / 1000));
            }
        }
        if (isUseNuocMia1) {
            if (Util.canDoWithTime(lastTimeUseNuocMia1, TIME_NUOC_MIA1 - 10000)) {
                lastTimeUseNuocMia1 = now;
                ItemTimeService.gI().sendItemTime(player, 13462, (int) (TIME_NUOC_MIA1 / 1000));
            }
        }
        if (isUseNuocMia2) {
            if (Util.canDoWithTime(lastTimeUseNuocMia2, TIME_NUOC_MIA2 - 10000)) {
                lastTimeUseNuocMia2 = now;
                ItemTimeService.gI().sendItemTime(player, 13463, (int) (TIME_NUOC_MIA2 / 1000));
            }
        }
        if (isUseNuocMia3) {
            if (Util.canDoWithTime(lastTimeUseNuocMia3, TIME_NUOC_MIA3 - 10000)) {
                lastTimeUseNuocMia3 = now;
                ItemTimeService.gI().sendItemTime(player, 13464, (int) (TIME_NUOC_MIA3 / 1000));
            }
        }
        if (isUseBuaSanta) {
            if (Util.canDoWithTime(lastTimeBuaSanta, TIME_BUA_SANTA - 10000)) {
                lastTimeBuaSanta = now;
                ItemTimeService.gI().sendItemTime(player, 13540, (int) (TIME_BUA_SANTA / 1000));
            }
        }
        if (isUseKhoBauX2) {
            if (Util.canDoWithTime(lastTimeUseKhoBauX2, TIME_MAY_DO2 - 10000)) {
                lastTimeUseKhoBauX2 = now;
            }
        }
        if (isUseTDLT) {
            if (timeTDLT <= 20000) {
                timeTDLT = 30000 * 1000;
            }
            if (Util.canDoWithTime(lastTimeUseTDLT, timeTDLT - 10000)) {
                lastTimeUseTDLT = now;
                timeTDLT = 30000 * 1000;
                ItemTimeService.gI().sendCanAutoPlay(this.player);
                ItemTimeService.gI().sendItemTime(player, 4387, timeTDLT / 1000);
            }
        }
        if (isUseRX) {
            if (timeRX <= 20000) {
                timeRX = 30000 * 1000;
            }
            if (Util.canDoWithTime(lastTimeUseRX, timeRX - 10000)) {
                lastTimeUseRX = now;
                timeRX = 30000 * 1000;
                ItemTimeService.gI().sendItemTime(player, 8579, timeRX / 1000);
            }
        }
    }

    public void dispose() {
        this.player = null;
    }
}
