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
                        "Rương SKH\nTân Thủ",
                        "Đi Ngũ\nHành Sơn",
                        "Tiện ích\nTự động");
            }
        }
    }

    protected void openMenuRuongSKHTanThu(Player player) {
        String[] setOptions = switch (player.gender) {
            case 0 -> new String[]{
                "Set Songoku\n(Kamejoko)",
                "Set T.Xin Hăng\n(Thái Dương HS)",
                "Set Krillin\n(Quả Cầu KK)",
                "Set Kaio\n(Kaioken)",
                "Đóng"
            };
            case 1 -> new String[]{
                "Set Ốc Tiêu\n(Liên Hoàn)",
                "Set Daimao\n(Đẻ Trứng)",
                "Set Picolo\n(Laze +50%)",
                "Set CB Nail\n(Masenko)",
                "Đóng"
            };
            default -> new String[]{
                "Set Kakarot\n(Galick)",
                "Set Cadic\n(x5 Biến Khỉ)",
                "Set Nappa\n(+80% HP)",
                "Set Cadic M\n(Tự Phát Nổ)",
                "Đóng"
            };
        };
        this.createOtherMenu(player, 9904,
                "|7|=== RƯƠNG SKH VẢI THÔ CƠ BẢN TÙY CHỌN ===\n"
                + "|2|Mỗi tài khoản được nhận miễn phí 1 bộ 5 món Vải Thô cơ bản\n"
                + "|1|(Áo, Quần, Găng, Giày, Rada cấp 1) kích hoạt 100% sát thương!\n"
                + "|0|Hãy chọn Set Kích Hoạt phù hợp với lối chơi của con:",
                setOptions);
    }

    protected void claimStarterSKHSet(Player player, int select) {
        if (select < 0 || select > 3) {
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 5) {
            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 5 ô trống để nhận trọn bộ 5 món SKH Vải Thô!");
            return;
        }
        if (player.getSession() == null) {
            return;
        }
        int userId = player.getSession().userId;
        try (Connection con = LocalManager.getConnection()) {
            int giftFlags = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT is_gift_box FROM account WHERE id = ? LIMIT 1")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        giftFlags = rs.getInt("is_gift_box");
                    }
                }
            }
            if ((giftFlags & 2) != 0) {
                Service.gI().sendThongBao(player, "Mỗi tài khoản chỉ được nhận Rương SKH Tân Thủ duy nhất 1 lần!");
                return;
            }
            try (PreparedStatement psUpdate = con.prepareStatement("UPDATE account SET is_gift_box = ? WHERE id = ?")) {
                psUpdate.setInt(1, giftFlags | 2);
                psUpdate.setInt(2, userId);
                psUpdate.executeUpdate();
            }

            int gender = player.gender < 0 || player.gender > 2 ? 0 : player.gender;
            int[][] skhTable = {
                {129, 127, 128, 245}, // Trái Đất: Songoku, Thiên Xin Hăng, Krillin, Thần Vũ Trụ Kaio
                {131, 132, 130, 237}, // Namek: Ốc Tiêu, Pikkoro Daimao, Picolo, Chiến Binh Nail
                {133, 134, 135, 241}  // Xayda: Kakarot, Cadic, Nappa, Cadic M
            };
            String[][] skhNames = {
                {"Set Songoku", "Set Thiên Xin Hăng", "Set Krillin", "Set Thần Vũ Trụ Kaio"},
                {"Set Ốc Tiêu", "Set Pikkoro Daimao", "Set Picolo", "Set Chiến Binh Nail"},
                {"Set Kakarot", "Set Cadic", "Set Nappa", "Set Cadic M"}
            };
            short[][] basicEquipIds = {
                {0, 6, 21, 27, 12}, // Trái Đất: Áo vải 3 lỗ, Quần vải đen, Găng vải đen, Giày nhựa, Rada cấp 1
                {1, 7, 22, 28, 12}, // Namek: Áo sợi len, Quần sợi len, Găng sợi len, Giày sợi len, Rada cấp 1
                {2, 8, 23, 29, 12}  // Xayda: Áo vải thô, Quần vải thô, Găng vải thô, Giày vải thô, Rada cấp 1
            };
            int[][] baseOptionTable = {
                {47, 3},  // Áo: Giáp +3
                {6, 30},  // Quần: HP +30
                {0, 15},  // Găng: Sức đánh +15
                {7, 30},  // Giày: KI +30
                {14, 1}   // Rada: Chí mạng +1%
            };

            int skhId = skhTable[gender][select];
            int[] subSkhIds = ItemService.gI().getOptionIdsBySKH(skhId);
            for (int i = 0; i < 5; i++) {
                Item it = ItemService.gI().createNewItem(basicEquipIds[gender][i], 1);
                it.itemOptions.clear();
                it.itemOptions.add(new Item.ItemOption(baseOptionTable[i][0], baseOptionTable[i][1]));
                it.itemOptions.add(new Item.ItemOption(skhId, 1));
                for (int subSkhId : subSkhIds) {
                    it.itemOptions.add(new Item.ItemOption(subSkhId, 1));
                }
                it.itemOptions.add(new Item.ItemOption(30, 1)); // Không thể giao dịch
                InventoryService.gI().addItemBag(player, it);
            }
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã nhận trọn bộ 5 món Vải Thô " + skhNames[gender][select] + "!");
        } catch (Exception e) {
            Logger.logException(OngGohan.class, e, "Lỗi nhận rương SKH tân thủ: " + player.name);
            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi nhận Rương SKH, vui lòng thử lại!");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.getIndexMenu() == 9904) {
                claimStarterSKHSet(player, select);
                return;
            }
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 5 -> {
                        nro.models.npc.NpcFactory.openMenuTienIch(player);
                    }
                    case 4 -> {
                        ChangeMapService.gI().changeMapBySpaceShip(player, 122, -1, -1);
                    }
                    case 3 -> {
                        openMenuRuongSKHTanThu(player);
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
                            int giftFlags = 0;
                            try (PreparedStatement ps = con.prepareStatement("SELECT is_gift_box FROM account WHERE id = ? LIMIT 1")) {
                                ps.setInt(1, userId);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        giftFlags = rs.getInt("is_gift_box");
                                    }
                                }
                            }
                            if ((giftFlags & 1) != 0) {
                                Service.gI().sendThongBao(player, "Mỗi tài khoản chỉ được nhận Quà Tân Thủ duy nhất 1 lần!");
                                return;
                            }
                            try (PreparedStatement psUpdate = con.prepareStatement("UPDATE account SET is_gift_box = ? WHERE id = ?")) {
                                psUpdate.setInt(1, giftFlags | 1);
                                psUpdate.setInt(2, userId);
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
