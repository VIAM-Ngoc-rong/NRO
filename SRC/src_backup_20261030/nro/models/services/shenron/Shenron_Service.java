package nro.models.services.shenron;

import nro.models.consts.ConstNpc;
import nro.models.services.shenron.Shenron_Event;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.Service;
import nro.models.map.service.NpcService;
import nro.models.services.InventoryService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class Shenron_Service {

    private static Shenron_Service instance;

    public static final short NGOC_RONG_1_SAO = 702;
    public static final short NGOC_RONG_2_SAO = 703;
    public static final short NGOC_RONG_3_SAO = 704;
    public static final short NGOC_RONG_4_SAO = 705;
    public static final short NGOC_RONG_5_SAO = 706;
    public static final short NGOC_RONG_6_SAO = 707;
    public static final short NGOC_RONG_7_SAO = 708;

    public static final short NGOC_RONG_TIM_1_SAO = 1827;
    public static final short NGOC_RONG_TIM_7_SAO = 1833;

    public static final short NGOC_RONG_BANG_1_SAO = 925;
    public static final short NGOC_RONG_BANG_7_SAO = 931;

    public static Shenron_Service gI() {
        if (instance == null) {
            instance = new Shenron_Service();
        }
        return instance;
    }

    public void openMenuSummonShenron(Player pl, int type) {
        pl.idMark.setShenronType(type);
        String title = switch (type) {
            case 1 -> "Bạn có muốn gọi Rồng Thần Tím không ?";
            case 2 -> "Bạn có muốn gọi Rồng Thần Băng Giáng Sinh không ?";
            default -> "Bạn có muốn gọi Rồng Xương Halloween không ?";
        };
        NpcService.gI().createMenuConMeo(pl, ConstNpc.SUMMON_SHENRON_EVENT, -1, title,
                "Đồng ý", "Từ chối");
    }

    public void summonShenron(Player player) {
        int type = player.idMark.getShenronType();
        if (type == 1 || (player.zone.map.mapId != 0 && player.zone.map.mapId != 7 && player.zone.map.mapId != 14)) {
            if (checkShenronBall(player, type)) {
                if (player.isShenronAppear || player.shenronEvent != null) {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                    return;
                }

                if (Util.canDoWithTime(player.lastTimeShenronAppeared, Shenron_Event.timeResummonShenron)) {
                    int startId = switch (type) {
                        case 1 -> NGOC_RONG_TIM_1_SAO;
                        case 2 -> NGOC_RONG_BANG_1_SAO;
                        default -> NGOC_RONG_1_SAO;
                    };
                    int endId = switch (type) {
                        case 1 -> NGOC_RONG_TIM_7_SAO;
                        case 2 -> NGOC_RONG_BANG_7_SAO;
                        default -> NGOC_RONG_7_SAO;
                    };
                    for (int i = startId; i <= endId; i++) {
                        try {
                            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, i), 1);
                        } catch (Exception ex) {
                        }
                    }
                    InventoryService.gI().sendItemBags(player);
                    Shenron_Event shenron = new Shenron_Event();
                    shenron.setPlayer(player);
                    Shenron_Manager.gI().add(shenron);
                    player.shenronEvent = shenron;
                    shenron.setZone(player.zone);
                    shenron.activeShenron(true, Shenron_Event.DRAGON_EVENT);
                    shenron.sendBlackGokuhesShenron();
                } else {
                    int timeLeft = (int) ((Shenron_Event.timeResummonShenron - (System.currentTimeMillis() - player.lastTimeShenronAppeared)) / 1000);
                    Service.gI().sendThongBao(player, "Vui lòng đợi " + (timeLeft < 7200 ? timeLeft + " giây" : timeLeft / 60 + " phút") + " nữa");
                }
            }
        } else {
            Service.gI().sendThongBao(player, "Không thể gọi rồng ở đây");
        }
    }

    private boolean checkShenronBall(Player pl, int type) {
        int startId = switch (type) {
            case 1 -> NGOC_RONG_TIM_1_SAO;
            case 2 -> NGOC_RONG_BANG_1_SAO;
            default -> NGOC_RONG_1_SAO;
        };
        int endId = switch (type) {
            case 1 -> NGOC_RONG_TIM_7_SAO;
            case 2 -> NGOC_RONG_BANG_7_SAO;
            default -> NGOC_RONG_7_SAO;
        };
        for (int i = startId; i <= endId; i++) {
            if (!InventoryService.gI().isExistItemBag(pl, i)) {
                Item it = ItemService.gI().createNewItem((short) i);
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên " + it.template.name);
                return false;
            }
        }
        return true;
    }
}
