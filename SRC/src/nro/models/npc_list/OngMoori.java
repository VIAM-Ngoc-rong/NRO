package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.player.Player;
import nro.models.services.TaskService;

public class OngMoori extends OngGohan {

    public OngMoori(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Con cố gắng theo Trưởng Lão Guru học thành tài, đừng lo lắng cho ta.\n(Gõ 'auto' vào chat để mở nhanh Menu Tiện Ích ở mọi nơi)",
                        "Nhận\n200k Ngọc",
                        "Nhận Đậu\n10 x99",
                        "Nhận Quà\nTân Thủ",
                        "Đi Ngũ\nHành Sơn",
                        "Tiện ích\nTự động");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        super.confirmMenu(player, select);
    }
}
