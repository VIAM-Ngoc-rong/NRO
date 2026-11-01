package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.map.service.ChangeMapService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.TaskService;

/**
 *
 * @author By Mr Blue
 */
public class DrMyuu extends Npc {

    public DrMyuu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (this.mapId == 166) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Đây là Phòng Thí Nghiệm Bí Mật của ta — nơi ký sinh trùng Tuffle Baby Vegeta và đội quân Dị Nhân Máy Đột Biến đang ẩn náu!\nNgươi muốn quay về Làng Kakarot chứ?",
                            "Về Làng\nKakarot",
                            "Ở lại");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Năm 740, ta tìm thấy các kí sinh trùng của King Tuffle, sau đó nghiên cứu và chế tạo kí sinh trùng Baby trong Phòng Thí Nghiệm Bí Mật.\nTuy nhiên Baby đã chiếm xác Vegeta và mất kiểm soát hoàn toàn!\nNgươi có dám tiến vào Phòng Thí Nghiệm Myuu để chế ngự nó không?",
                            "Vào Phòng\nThí Nghiệm",
                            "Từ chối");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        if (this.mapId == 166) {
                            ChangeMapService.gI().changeMap(player, 14, -1, 600, 408);
                        } else {
                            ChangeMapService.gI().changeMap(player, 166, -1, 180, 480);
                        }
                    }
                }
            }
        }
    }
}
