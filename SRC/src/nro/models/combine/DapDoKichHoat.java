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
 * Dùng 3 món đồ Kích Hoạt + 1 món Đồ Phôi tương ứng (Áo, Quần, Găng, Giày, Rada/Nhẫn) + 100 triệu vàng
 * -> Ra 1 món trang bị Kích Hoạt mới đúng theo Đồ Phôi với Set Kích Hoạt bất kỳ của hành tinh đó.
 */
public class DapDoKichHoat {

    private static final int GOLD_DAP_DO = 100_000_000;
    private static final int RATIO_DAP_DO = 100;

    private static final int[] VALID_SKH_OPTION_IDS = {
        127, 128, 129, 130, 131, 132, 133, 134, 135,
        136, 137, 138, 139, 140, 141, 142, 143, 144,
        233, 234, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248
    };

    private static final int[][] PLANET_SKH_SETS = {
        {127, 128, 129, 245}, // Trái Đất: Thiên Xin Hăng, Krillin, Songoku, Thần Vũ Trụ Kaio
        {130, 131, 132, 237}, // Namek: Picolo, Ốc Tiêu, Pikkoro Daimao, Chiến Binh Nail
        {133, 134, 135, 241}  // Xayda: Kakarot, Cadic, Nappa, Cadic M
    };

    /**
     * Tìm món Đồ Phôi trong 4 vật phẩm đưa vào (3 món đồ KH + 1 món đồ Phôi).
     * - Nếu có đúng 3 món SKH và 1 món trang bị thường -> món thường là Đồ Phôi.
     * - Nếu cả 4 món đều là SKH (đập lại SKH cho đồ cấp cao) -> món có yêu cầu SM / cấp cao nhất là Đồ Phôi.
     */
    private static Item findPhoiItem(List<Item> items) {
        if (items == null || items.size() != 4) {
            return null;
        }
        int skhCount = 0;
        Item nonSkhEquip = null;
        Item highestEquip = null;
        for (Item item : items) {
            if (!isEquipItem(item) || item.quantity < 1) {
                return null;
            }
            if (isItemKichHoat(item)) {
                skhCount++;
            } else {
                nonSkhEquip = item;
            }
            if (highestEquip == null
                    || item.template.strRequire > highestEquip.template.strRequire
                    || (item.template.strRequire == highestEquip.template.strRequire && item.template.level >= highestEquip.template.level)) {
                highestEquip = item;
            }
        }
        if (skhCount < 3) {
            return null;
        }
        return nonSkhEquip != null ? nonSkhEquip : highestEquip;
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 4) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần đặt đúng 4 vật phẩm gồm:\n- 3 món trang bị Kích Hoạt bất kỳ\n- 1 món Đồ Phôi muốn lên SKH (VD: Găng Thần Linh, Áo Hủy Diệt...)", "Đóng");
            return;
        }

        Item phoiItem = findPhoiItem(player.combineNew.itemsCombine);
        if (phoiItem == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Vật phẩm không hợp lệ! Cần có đúng 3 món đồ Kích Hoạt + 1 món trang bị Phôi (Áo, Quần, Găng, Giày, Rada/Nhẫn).", "Đóng");
            return;
        }

        player.combineNew.goldCombine = GOLD_DAP_DO;
        player.combineNew.ratioCombine = RATIO_DAP_DO;

        int targetGender = (phoiItem.template.gender >= 0 && phoiItem.template.gender <= 2)
                ? phoiItem.template.gender : player.gender;
        String planetName = targetGender == 0 ? "Trái Đất" : (targetGender == 1 ? "Namek" : "Xayda");

        String npcSay = "|2|Đập 3 món đồ Kích Hoạt + 1 Đồ Phôi thành Đồ SKH\n"
                + "|6|Đồ Phôi được chọn: " + phoiItem.template.name + "\n"
                + "|2|Kết quả: 1 " + phoiItem.template.name + " Kích Hoạt (" + planetName + " bất kỳ)\n"
                + "|2|Tỉ lệ thành công: " + RATIO_DAP_DO + "%\n"
                + "|2|Chi phí: " + Util.numberToMoney(GOLD_DAP_DO) + " vàng\n";

        if (player.inventory.gold < GOLD_DAP_DO) {
            npcSay += "|7|Còn thiếu " + Util.powerToString(GOLD_DAP_DO - player.inventory.gold) + " vàng\n";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Đập đồ\n" + Util.numberToMoney(GOLD_DAP_DO) + " vàng", "Từ chối");
        }
    }

    public static void thucHienDapDo(Player player) {
        if (player.combineNew.itemsCombine.size() != 4) {
            Service.gI().sendThongBao(player, "Cần đặt đúng 3 món trang bị Kích Hoạt + 1 món Đồ Phôi!");
            return;
        }

        Item phoiItem = findPhoiItem(player.combineNew.itemsCombine);
        if (phoiItem == null) {
            Service.gI().sendThongBao(player, "Cần đúng 3 món đồ Kích Hoạt và 1 món trang bị Phôi hợp lệ!");
            return;
        }

        if (player.inventory.gold < GOLD_DAP_DO) {
            Service.gI().sendThongBao(player, "Không đủ 100 triệu vàng để thực hiện!");
            return;
        }

        Item newItem = ItemService.gI().createNewItem(phoiItem.template.id, 1);
        newItem.itemOptions.clear();
        if (phoiItem.itemOptions != null) {
            for (ItemOption io : phoiItem.itemOptions) {
                if (io != null && io.optionTemplate != null && !isSkhOptionId(io.optionTemplate.id)
                        && io.optionTemplate.id != 30 && io.optionTemplate.id != 73) {
                    newItem.itemOptions.add(new ItemOption(io.optionTemplate.id, io.param));
                }
            }
        }
        if (newItem.itemOptions.isEmpty()) {
            RewardService.gI().initBaseOptionClothes(newItem, newItem.template.id, newItem.template.type, newItem.itemOptions);
        }

        int gender = (phoiItem.template.gender >= 0 && phoiItem.template.gender <= 2)
                ? phoiItem.template.gender : player.gender;
        if (gender < 0 || gender > 2) {
            gender = 0;
        }

        int[] possibleSets = PLANET_SKH_SETS[gender];
        int skhId = possibleSets[Util.nextInt(possibleSets.length)];
        newItem.itemOptions.add(new ItemOption(skhId, 1));
        for (int subOpId : ItemService.gI().getOptionIdsBySKH(skhId)) {
            newItem.itemOptions.add(new ItemOption(subOpId, 1));
        }
        newItem.itemOptions.add(new ItemOption(30, 0)); // Không thể giao dịch

        player.inventory.gold -= GOLD_DAP_DO;
        for (Item item : player.combineNew.itemsCombine) {
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
        }

        InventoryService.gI().addItemBag(player, newItem);
        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã đập thành công " + newItem.template.name + " Kích Hoạt!");

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static boolean isEquipItem(Item item) {
        return item != null && item.isNotNullItem() && item.template != null
                && item.template.type >= 0 && item.template.type <= 4;
    }

    private static boolean isSkhOptionId(int optId) {
        for (int validId : VALID_SKH_OPTION_IDS) {
            if (optId == validId) {
                return true;
            }
        }
        return false;
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
