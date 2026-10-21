package nro.models.combine;

import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.RewardService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 * Đập đồ Kích Hoạt tại Bà Hạt Mít Đảo Kame:
 * Dùng 3 món đồ kích hoạt + 100 triệu vàng -> ra 1 món đồ kích hoạt mới
 * từ cấp 200tr sức mạnh đến tối đa là Đồ Thần Linh.
 */
public class DapDoKichHoat {

    private static final int GOLD_DAP_DO = 100_000_000;
    private static final int RATIO_DAP_DO = 100;

    private static final int[] VALID_SKH_OPTION_IDS = {
        127, 128, 129, 130, 131, 132, 133, 134, 135,
        136, 137, 138, 139, 140, 141, 142, 143, 144,
        233, 234, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248
    };

    // Trang bị từ bậc ~200tr sức mạnh đến cấp 12 theo hành tinh [gender][type]
    private static final int[][][] HIGH_TIER_ITEMS = {
        { // Trái Đất
            {232, 233}, // Áo
            {244, 245}, // Quần
            {255, 256, 257}, // Găng (255 yêu cầu 200tr SM)
            {268, 269}, // Giày
            {279, 280, 281} // Rada (279 yêu cầu 200tr SM)
        },
        { // Namếc
            {236, 237}, // Áo
            {248, 249}, // Quần
            {259, 260, 261}, // Găng (259 yêu cầu 200tr SM)
            {272, 273}, // Giày
            {279, 280, 281} // Rada
        },
        { // Xayda
            {240, 241}, // Áo
            {252, 253}, // Quần
            {263, 264, 265}, // Găng (263 yêu cầu 200tr SM)
            {276, 277}, // Giày
            {279, 280, 281} // Rada
        }
    };

    // Đồ Thần Linh tối đa theo hành tinh [gender][type]
    private static final int[][] GOD_TIER_ITEMS = {
        {555, 556, 562, 563, 561}, // Trái Đất
        {557, 558, 564, 565, 561}, // Namếc
        {559, 560, 566, 567, 561}  // Xayda
    };

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 3) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần đặt đúng 3 món trang bị Kích Hoạt!", "Đóng");
            return;
        }

        for (Item item : player.combineNew.itemsCombine) {
            if (!isItemKichHoat(item)) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cả 3 vật phẩm đều phải là trang bị Kích Hoạt!", "Đóng");
                return;
            }
        }

        player.combineNew.goldCombine = GOLD_DAP_DO;
        player.combineNew.ratioCombine = RATIO_DAP_DO;

        String npcSay = "|2|Đập 3 món đồ Kích Hoạt thành 1 món đồ Kích Hoạt mới\n"
                + "|2|Trang bị nhận được ngẫu nhiên từ bậc 200 triệu SM đến Đồ Thần Linh\n"
                + "|2|Loại Set Kích Hoạt: Ngẫu nhiên\n"
                + "|2|Tỉ lệ thành công: " + RATIO_DAP_DO + "%\n"
                + "|2|Cần: " + Util.numberToMoney(GOLD_DAP_DO) + " vàng\n";

        if (player.inventory.gold < GOLD_DAP_DO) {
            npcSay += "|7|Còn thiếu " + Util.powerToString(GOLD_DAP_DO - player.inventory.gold) + " vàng\n";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Đập đồ\n" + Util.numberToMoney(GOLD_DAP_DO) + " vàng", "Từ chối");
        }
    }

    public static void thucHienDapDo(Player player) {
        if (player.combineNew.itemsCombine.size() != 3) {
            Service.gI().sendThongBao(player, "Cần đặt đúng 3 món trang bị Kích Hoạt!");
            return;
        }

        for (Item item : player.combineNew.itemsCombine) {
            if (!isItemKichHoat(item) || item.quantity < 1) {
                Service.gI().sendThongBao(player, "Cả 3 vật phẩm đều phải là trang bị Kích Hoạt hợp lệ!");
                return;
            }
        }

        if (player.inventory.gold < GOLD_DAP_DO) {
            Service.gI().sendThongBao(player, "Không đủ 100 triệu vàng để thực hiện!");
            return;
        }

        player.inventory.gold -= GOLD_DAP_DO;
        for (Item item : player.combineNew.itemsCombine) {
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
        }

        int gender = player.gender;
        if (gender < 0 || gender > 2) {
            gender = Util.nextInt(0, 2);
        }
        int type = Util.nextInt(0, 4); // Áo, Quần, Găng, Giày, Rada

        int newItemId;
        // 10% ra Đồ Thần Linh (tối đa), 90% ra đồ từ bậc 200tr SM -> cấp 12
        if (Util.isTrue(10, 100)) {
            newItemId = GOD_TIER_ITEMS[gender][type];
        } else {
            int[] pool = HIGH_TIER_ITEMS[gender][type];
            newItemId = pool[Util.nextInt(0, pool.length - 1)];
        }

        Item newItem = ItemService.gI().createNewItem((short) newItemId, 1);
        newItem.itemOptions.clear();
        RewardService.gI().initBaseOptionClothes(newItem, newItem.template.id, newItem.template.type, newItem.itemOptions);

        int[] skhOptions = ItemService.gI().randOptionItemKichHoat(gender);
        for (int opId : skhOptions) {
            if (opId > 0) {
                newItem.itemOptions.add(new ItemOption(opId, 1));
            }
        }
        newItem.itemOptions.add(new ItemOption(30, 0)); // Không thể giao dịch

        InventoryService.gI().addItemBag(player, newItem);
        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã đập được " + newItem.template.name + " Kích Hoạt!");

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    public static boolean isItemKichHoat(Item item) {
        if (item == null || !item.isNotNullItem() || item.itemOptions == null) {
            return false;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null) {
                for (int validId : VALID_SKH_OPTION_IDS) {
                    if (io.optionTemplate.id == validId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
