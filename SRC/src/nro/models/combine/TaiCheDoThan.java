package nro.models.combine;

import java.util.ArrayList;
import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 * Tái chế / Phân rã Đồ Thần Linh tại Bà Hạt Mít - Đảo Kame:
 * Đổi trang bị Thần Linh dư thừa lấy Vàng, Hồng Ngọc, Đá Ngũ Sắc, Khoáng Tái Chế và Thức Ăn Hủy Diệt.
 */
public class TaiCheDoThan {

    private static final int[] THUC_AN_IDS = {663, 664, 665, 666, 667};

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.isEmpty()) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "Hãy chọn ít nhất 1 món Đồ Thần Linh để tái chế!\n"
                    + "|7|(Mẹo: Bấm 'Chọn TẤT CẢ' hoặc 'Đổi sang Trang kế' bên dưới để chọn nhanh không lo trùng tên)",
                    "Đóng", "Chọn TẤT CẢ\nở Trang này", "Đổi sang\nTrang kế");
            return;
        }

        StringBuilder listInfo = new StringBuilder();
        int countDTL = 0;
        int countSKH = 0;
        int countSPL = 0;

        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem()) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Vật phẩm không hợp lệ!", "Đóng");
                return;
            }
            if (!item.isDTL()) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        item.template.name + " không phải là Đồ Thần Linh!\nChỉ có thể tái chế trang bị Thần Linh.", "Đóng");
                return;
            }
            boolean hasSkh = DapDoKichHoat.isItemKichHoat(item);
            int star = PhanRaDoSao.getStar(item);
            listInfo.append("|1|- ").append(item.template.name)
                    .append(hasSkh ? " [SKH]" : "")
                    .append(star > 0 ? " [" + star + " sao]" : "")
                    .append("\n");
            countDTL++;
            if (hasSkh) {
                countSKH++;
            }
            if (star > 0) {
                countSPL++;
            }
        }

        StringBuilder npcSay = new StringBuilder();
        npcSay.append("|2|Tái chế ").append(countDTL).append(" món Đồ Thần Linh:\n");
        npcSay.append(listInfo);
        npcSay.append("|2|Phần thưởng nhận được (mỗi món Đồ Thần):\n");
        npcSay.append("|0|+ 100 - 200 Triệu Vàng\n");
        npcSay.append("|0|+ 30 - 60 Hồng Ngọc\n");
        npcSay.append("|0|+ 2 - 4 Đá Ngũ Sắc\n");
        npcSay.append("|0|+ 1 - 2 Khoáng Tái Chế\n");
        npcSay.append("|0|+ 3 - 7 Thức Ăn Hủy Diệt\n");
        if (countSKH > 0) {
            npcSay.append("|6|+ Cộng thêm có SKH (x").append(countSKH).append("): +50-100Tr Vàng, +15-30 Hồng Ngọc, +1-2 ĐNS, +1 Khoáng TC, +2-4 Thức Ăn/món\n");
        }
        if (countSPL > 0) {
            npcSay.append("|7|+ Cộng thêm có Sao Pha Lê (x").append(countSPL).append("): Nhận thêm Vàng, Hồng Ngọc, ĐNS, SPL Cấp 1/2 & Hematite theo số sao!\n");
        }
        npcSay.append("|2|Chi phí: Miễn phí (Tỉ lệ thành công 100%)");

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                npcSay.toString(),
                "Tái chế\nngay (" + countDTL + " món)",
                "Chọn TẤT CẢ\nở Trang này",
                "Đổi sang\nTrang kế",
                "Từ chối");
    }

    public static void thucHienTaiChe(Player player) {
        if (player.combineNew.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player, "Hãy chọn ít nhất 1 món Đồ Thần Linh!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 3) {
            Service.gI().sendThongBao(player, "Cần ít nhất 3 ô trống trong hành trang để nhận nguyên liệu!");
            return;
        }

        List<Item> validItems = new ArrayList<>();
        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem() || item.quantity < 1 || !item.isDTL()) {
                Service.gI().sendThongBao(player, "Có vật phẩm không phải Đồ Thần Linh trong danh sách!");
                return;
            }
            validItems.add(item);
        }

        long totalGold = 0;
        int totalRuby = 0;
        int totalDaNguSac = 0;
        int totalKhoang = 0;
        int totalThucAn = 0;
        int totalSpl1 = 0;
        int totalSpl2 = 0;
        int totalHematite = 0;
        int totalCapsuleKH = 0;

        for (Item item : validItems) {
            boolean hasSkh = DapDoKichHoat.isItemKichHoat(item);
            int star = PhanRaDoSao.getStar(item);
            InventoryService.gI().subQuantityItemsBag(player, item, 1);

            totalGold += Util.nextInt(100, 200) * 1_000_000L;
            totalRuby += Util.nextInt(30, 60);
            totalDaNguSac += Util.nextInt(2, 4);
            totalKhoang += Util.nextInt(1, 2);
            totalThucAn += Util.nextInt(3, 7);

            if (hasSkh) {
                totalGold += Util.nextInt(50, 100) * 1_000_000L;
                totalRuby += Util.nextInt(15, 30);
                totalDaNguSac += Util.nextInt(1, 2);
                totalKhoang += 1;
                totalThucAn += Util.nextInt(2, 4);
            }

            if (star >= 1) {
                if (star <= 2) {
                    totalDaNguSac += 1;
                    totalGold += Util.nextInt(20, 40) * 1_000_000L;
                    totalRuby += Util.nextInt(5, 10);
                    int splCount = Util.nextInt(1, 2);
                    for (int i = 0; i < splCount; i++) {
                        InventoryService.gI().addItemBag(player, PhanRaDoSao.createRandomSaoPhaLeCap1(1));
                        totalSpl1++;
                    }
                } else if (star <= 4) {
                    totalDaNguSac += 2;
                    totalGold += Util.nextInt(50, 100) * 1_000_000L;
                    totalRuby += Util.nextInt(15, 30);
                    int splCount = Util.nextInt(2, 3);
                    for (int i = 0; i < splCount; i++) {
                        InventoryService.gI().addItemBag(player, PhanRaDoSao.createRandomSaoPhaLeCap1(1));
                        totalSpl1++;
                    }
                    if (Util.isTrue(30, 100)) {
                        totalHematite += 1;
                    }
                } else if (star <= 6) {
                    totalDaNguSac += 4;
                    totalGold += Util.nextInt(150, 250) * 1_000_000L;
                    totalRuby += Util.nextInt(40, 80);
                    totalHematite += Util.nextInt(1, 2);
                    InventoryService.gI().addItemBag(player, PhanRaDoSao.createRandomSaoPhaLeCap2(1));
                    totalSpl2++;
                } else {
                    totalDaNguSac += 8;
                    totalGold += Util.nextInt(300, 500) * 1_000_000L;
                    totalRuby += Util.nextInt(100, 200);
                    totalHematite += Util.nextInt(3, 5);
                    int spl2Count = Util.nextInt(1, 2);
                    for (int i = 0; i < spl2Count; i++) {
                        InventoryService.gI().addItemBag(player, PhanRaDoSao.createRandomSaoPhaLeCap2(1));
                        totalSpl2++;
                    }
                    if (Util.isTrue(30, 100)) {
                        totalCapsuleKH += 1;
                    }
                }
            }
        }

        if (totalDaNguSac > 0) {
            Item dns = ItemService.gI().createNewItem((short) 674, totalDaNguSac);
            InventoryService.gI().addItemBag(player, dns);
        }
        if (totalKhoang > 0) {
            Item khoang = ItemService.gI().createNewItem((short) 1656, totalKhoang);
            InventoryService.gI().addItemBag(player, khoang);
        }
        if (totalHematite > 0) {
            Item hematite = ItemService.gI().createNewItem((short) 1423, totalHematite);
            InventoryService.gI().addItemBag(player, hematite);
        }
        if (totalCapsuleKH > 0) {
            Item capsule = ItemService.gI().createNewItem((short) 1655, totalCapsuleKH);
            InventoryService.gI().addItemBag(player, capsule);
        }
        if (totalThucAn > 0) {
            short foodId = findBestThucAnId(player);
            Item food = ItemService.gI().createNewItem(foodId, totalThucAn);
            food.itemOptions.add(new ItemOption(30, 0));
            InventoryService.gI().addItemBag(player, food);
        }
        if (totalGold > 0) {
            player.inventory.gold = Math.min(player.inventory.gold + totalGold, Inventory.LIMIT_GOLD);
        }
        if (totalRuby > 0) {
            long newRuby = (long) player.inventory.ruby + totalRuby;
            player.inventory.ruby = (int) Math.min(newRuby, Integer.MAX_VALUE);
        }

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);

        Service.gI().sendThongBao(player, "Tái chế Đồ Thần thành công! Nhận: "
                + Util.numberToMoney(totalGold) + " vàng, "
                + totalRuby + " Hồng Ngọc, "
                + totalDaNguSac + " Đá Ngũ Sắc, "
                + totalKhoang + " Khoáng tái chế, "
                + totalThucAn + " Thức ăn"
                + (totalSpl1 > 0 ? ", " + totalSpl1 + " SPL cấp 1" : "")
                + (totalSpl2 > 0 ? ", " + totalSpl2 + " SPL cấp 2" : "")
                + (totalHematite > 0 ? ", " + totalHematite + " Đá Hematite" : "")
                + (totalCapsuleKH > 0 ? ", " + totalCapsuleKH + " Capsule KH" : ""));
    }

    private static short findBestThucAnId(Player player) {
        short bestId = (short) THUC_AN_IDS[Util.nextInt(0, THUC_AN_IDS.length - 1)];
        int maxQty = 0;
        for (int id : THUC_AN_IDS) {
            Item existing = InventoryService.gI().findItemBag(player, id);
            if (existing != null && existing.quantity > maxQty) {
                maxQty = existing.quantity;
                bestId = (short) id;
            }
        }
        return bestId;
    }
}
