package nro.models.npc_list;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import nro.models.consts.ConstNpc;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.map.service.ChangeMapService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.utils.Logger;

public class OngGohan extends Npc {

    public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Con cố gắng theo Quy Lão Kame học thành tài, đừng lo lắng cho ta.\n(Gõ 'auto' vào chat để mở nhanh Menu Tiện Ích ở mọi nơi)",
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
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 4 -> {
                        nro.models.npc.NpcFactory.openMenuTienIch(player);
                    }
                    case 3 -> {
                        ChangeMapService.gI().changeMapBySpaceShip(player, 122, -1, -1);
                    }
                    case 0 -> {
                        long newGem = Math.min((long) player.inventory.gem + 200_000L, 2_000_000_000L);
                        player.inventory.gem = (int) newGem;
                        Service.gI().sendMoney(player);
                        Service.gI().sendThongBao(player, "Bạn vừa nhận được 200.000 Ngọc xanh!");
                    }
                    case 1 -> {
                        Item dau10 = ItemService.gI().createNewItem((short) 595, 99);
                        dau10.itemOptions.add(new Item.ItemOption(2, 256));
                        if (InventoryService.gI().addItemBag(player, dau10)) {
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Bạn vừa nhận được x99 Đậu thần cấp 10!");
                        } else {
                            Service.gI().sendThongBao(player, "Hành trang đã đầy, vui lòng dọn trống ít nhất 1 ô!");
                        }
                    }
                    case 2 -> {
                        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống để nhận Quà Tân Thủ!");
                            return;
                        }
                        if (player.getSession() == null) {
                            return;
                        }
                        int userId = player.getSession().userId;
                        try (Connection con = LocalManager.getConnection()) {
                            boolean daNhan = false;
                            try (PreparedStatement ps = con.prepareStatement("SELECT is_gift_box FROM account WHERE id = ? LIMIT 1")) {
                                ps.setInt(1, userId);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next() && rs.getInt("is_gift_box") != 0) {
                                        daNhan = true;
                                    }
                                }
                            }
                            if (daNhan) {
                                Service.gI().sendThongBao(player, "Mỗi tài khoản chỉ được nhận Quà Tân Thủ duy nhất 1 lần!");
                                return;
                            }
                            try (PreparedStatement psUpdate = con.prepareStatement("UPDATE account SET is_gift_box = 1 WHERE id = ?")) {
                                psUpdate.setInt(1, userId);
                                psUpdate.executeUpdate();
                            }
                            long newGem = Math.min((long) player.inventory.gem + 1_000_000L, 2_000_000_000L);
                            player.inventory.gem = (int) newGem;
                            Service.gI().sendMoney(player);

                            Item thoiVang = ItemService.gI().createNewItem((short) 457, 99);
                            InventoryService.gI().addItemBag(player, thoiVang);
                            InventoryService.gI().sendItemBags(player);

                            Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã nhận Quà Tân Thủ: x99 Thỏi vàng và 1.000.000 Ngọc!");
                        } catch (Exception e) {
                            Logger.logException(OngGohan.class, e, "Lỗi nhận quà tân thủ: " + player.name);
                            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi nhận quà, vui lòng thử lại!");
                        }
                    }
                }
            }
        }
    }
}
