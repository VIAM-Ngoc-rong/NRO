package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.map.service.ChangeMapService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.TaskService;

public class Vados extends Npc {

    public Vados(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (this.mapId == 169) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Chào mừng tới Hành Tinh Hủy Diệt Vũ Trụ 6 — lãnh địa của Thần Hủy Diệt Champa, Siêu Xayda Kefla và Sát Thủ Huyền Thoại Hit!\nNgươi muốn quay về Hành Tinh Bill (Vũ Trụ 7) chứ?",
                            "Quay về\nHành Tinh Bill",
                            "Ở lại");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ta là Thiên Sứ Vados của Vũ Trụ 6.\nTại Hành Tinh Hủy Diệt Vũ Trụ 6, các chiến binh mạnh nhất như Kefla, Thần Champa và Sát Thủ Huyền Thoại Hit đang chờ đợi những kẻ mạnh thực thụ!\n(Hạ Sát Thủ Hit rơi 100% Quả Trứng Hit — ấp Đệ Tử Cấp 4 Hợp Thể +45% chỉ số!)",
                            "Đến Hành Tinh\nVũ Trụ 6",
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
                        if (this.mapId == 169) {
                            ChangeMapService.gI().changeMap(player, 154, -1, 560, 312);
                        } else {
                            ChangeMapService.gI().changeMap(player, 169, -1, 600, 216);
                        }
                    }
                }
            }
        }
    }
}
