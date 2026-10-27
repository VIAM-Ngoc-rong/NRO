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
     * Tìm món Đồ Phôi trong 4 vật phẩm đưa vào (3 món đồ KH phụ + 1 món đồ Phôi).
     * - Nếu có đúng 3 món SKH và 1 món trang bị thường -> món thường chắc chắn là Đồ Phôi.
     * - Nếu cả 4 món đều là SKH (đập đổi lại SKH cho đồ đang có SKH) -> so sánh theo điểm giá trị thực tế
     *   (Cấp Đồ Thiên Sứ/Hủy Diệt/Thần Linh, Sức mạnh yêu cầu thực tế gồm cả Option 21, Cấp [+], Số Sao Pha Lê, Cấp item),
     *   và nếu bằng điểm thì giữ nguyên món ĐẦU TIÊN người chơi bỏ vào (dùng > thay vì >=).
     */
    private static Item findPhoiItem(List<Item> items) {
        if (items == null || items.size() != 4) {
            return null;
        }
        int skhCount = 0;
        Item nonSkhEquip = null;
        Item highestEquip = null;
        long highestScore = Long.MIN_VALUE;
        for (Item item : items) {
            if (!isEquipItem(item) || item.quantity < 1) {
                return null;
            }
            if (isItemKichHoat(item)) {
                skhCount++;
            } else {
                nonSkhEquip = item;
            }
            long score = getPhoiPriorityScore(item);
            if (highestEquip == null || score > highestScore) {
                highestEquip = item;
                highestScore = score;
            }
        }
        if (skhCount < 3) {
            return null;
        }
        return nonSkhEquip != null ? nonSkhEquip : highestEquip;
    }

    private static long getPhoiPriorityScore(Item item) {
        if (item == null || item.template == null) {
            return 0L;
        }
        long power = item.template.strRequire;
        int upgradeLv = 0;
        int imbuedStars = 0;
        int starSlots = 0;
        if (item.itemOptions != null) {
            for (ItemOption io : item.itemOptions) {
                if (io == null || io.optionTemplate == null) {
                    continue;
                }
                int id = io.optionTemplate.id;
                if (id == 21) {
                    power = Math.max(power, (long) io.param * 1_000_000_000L);
                } else if (id == 72) {
                    upgradeLv = Math.max(upgradeLv, io.param);
                } else if (id == 102) {
                    imbuedStars = Math.max(imbuedStars, io.param);
                } else if (id == 107) {
                    starSlots = Math.max(starSlots, io.param);
                }
            }
        }
        long tierBonus = 0L;
        if (item.isDTS() || item.template.level >= 15) {
            tierBonus = 500_000_000_000L;
        } else if (item.isDHD() || item.template.level == 14) {
            tierBonus = 300_000_000_000L;
        } else if (item.isDTL() || item.template.level == 13) {
            tierBonus = 150_000_000_000L;
        }
        return tierBonus
                + power
                + (long) upgradeLv * 5_000_000_000L
                + (long) imbuedStars * 1_000_000_000L
                + (long) starSlots * 200_000_000L
                + (long) item.template.level * 10_000_000L;
    }

    public static boolean isValuableEquipmentNotForFodder(Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return false;
        }
        if (item.isDTL() || item.isDHD() || item.isDTS() || item.template.level >= 13) {
            return true;
        }
        if (item.itemOptions != null) {
            for (ItemOption io : item.itemOptions) {
                if (io == null || io.optionTemplate == null) {
                    continue;
                }
                int id = io.optionTemplate.id;
                if (id == 21 && io.param >= 15) {
                    return true;
                }
                if (id == 72 && io.param > 0) {
                    return true;
                }
                if (id == 102 && io.param > 0) {
                    return true;
                }
                if (id == 107 && io.param >= 5) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isValidFodderForBulkSelect(Item item) {
        return isItemKichHoat(item) && !isValuableEquipmentNotForFodder(item);
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 4) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "Cần đặt đúng 4 vật phẩm gồm:\n- 1 món Đồ Phôi muốn lên/đổi SKH (Đặt vào ĐẦU TIÊN)\n- 3 món trang bị Kích Hoạt phụ (đồ rác KH)\n"
                    + "|7|(Mẹo: Chọn món Đồ Phôi trước, sau đó bấm 'Chọn nhanh 3 Đồ KH' bên dưới)",
                    "Đóng", "Chọn nhanh\n3 Đồ KH", "Đổi sang\nTrang kế");
            return;
        }

        Item phoiItem = findPhoiItem(player.combineNew.itemsCombine);
        if (phoiItem == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "Vật phẩm không hợp lệ! Cần có đúng 3 món đồ Kích Hoạt + 1 món trang bị Phôi (Áo, Quần, Găng, Giày, Rada/Nhẫn).",
                    "Đóng", "Chọn nhanh\n3 Đồ KH", "Đổi sang\nTrang kế");
            return;
        }

        StringBuilder fodderList = new StringBuilder();
        for (Item item : player.combineNew.itemsCombine) {
            if (item == phoiItem) {
                continue;
            }
            if (isValuableEquipmentNotForFodder(item)) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "CẢNH BÁO BẢO VỆ ĐỒ:\n"
                        + "Món nguyên liệu phụ [" + item.template.name + "] là đồ cấp cao / đã ép sao / đã nâng [+].\n"
                        + "Hệ thống tự động chặn để tránh mất đồ quý! Vui lòng chỉ dùng 3 món đồ KH thường (chưa đập [+], chưa ép sao) làm phôi phụ.",
                        "Đóng");
                return;
            }
            fodderList.append("|0|- ").append(item.template.name).append(" (Nguyên liệu tiêu hao)\n");
        }

        player.combineNew.goldCombine = GOLD_DAP_DO;
        player.combineNew.ratioCombine = RATIO_DAP_DO;

        int targetGender = (phoiItem.template.gender >= 0 && phoiItem.template.gender <= 2)
                ? phoiItem.template.gender : player.gender;
        String planetName = targetGender == 0 ? "Trái Đất" : (targetGender == 1 ? "Namek" : "Xayda");

        String npcSay = "|2|Đập 3 món đồ Kích Hoạt + 1 Đồ Phôi thành Đồ SKH\n"
                + "|6|Đồ Phôi giữ lại (Lên SKH): " + phoiItem.template.name + "\n"
                + "|2|3 Món KH phụ sẽ tiêu hao:\n" + fodderList
                + "|2|Kết quả: Giữ nguyên " + phoiItem.template.name + " + Nhận Set Kích Hoạt (" + planetName + " bất kỳ)\n"
                + "|2|Tỉ lệ thành công: " + RATIO_DAP_DO + "%\n"
                + "|2|Chi phí: " + Util.numberToMoney(GOLD_DAP_DO) + " vàng\n";

        if (player.inventory.gold < GOLD_DAP_DO) {
            npcSay += "|7|Còn thiếu " + Util.powerToString(GOLD_DAP_DO - player.inventory.gold) + " vàng\n";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Đập đồ\n" + Util.numberToMoney(GOLD_DAP_DO) + " vàng",
                    "Chọn nhanh\n3 Đồ KH",
                    "Đổi sang\nTrang kế",
                    "Từ chối");
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

        int fodderCount = 0;
        for (Item item : player.combineNew.itemsCombine) {
            if (item == phoiItem) {
                continue;
            }
            if (item == null || !item.isNotNullItem() || item.quantity < 1 || !isItemKichHoat(item)) {
                Service.gI().sendThongBao(player, "Nguyên liệu phụ không hợp lệ!");
                return;
            }
            if (isValuableEquipmentNotForFodder(item)) {
                Service.gI().sendThongBao(player, "Không thể dùng " + item.template.name + " (đồ quý/đã nâng cấp) làm nguyên liệu phụ!");
                return;
            }
            fodderCount++;
        }
        if (fodderCount != 3) {
            Service.gI().sendThongBao(player, "Cần đúng 3 món đồ Kích Hoạt phụ khác với Đồ Phôi!");
            return;
        }

        if (player.inventory.gold < GOLD_DAP_DO) {
            Service.gI().sendThongBao(player, "Không đủ 100 triệu vàng để thực hiện!");
            return;
        }

        List<ItemOption> preservedOptions = new java.util.ArrayList<>();
        if (phoiItem.itemOptions != null) {
            for (ItemOption io : phoiItem.itemOptions) {
                if (io != null && io.optionTemplate != null && !isSkhOptionId(io.optionTemplate.id)
                        && io.optionTemplate.id != 30 && io.optionTemplate.id != 73) {
                    preservedOptions.add(new ItemOption(io.optionTemplate.id, io.param));
                }
            }
        }
        if (preservedOptions.isEmpty()) {
            RewardService.gI().initBaseOptionClothes(phoiItem, phoiItem.template.id, phoiItem.template.type, preservedOptions);
        }

        int gender = (phoiItem.template.gender >= 0 && phoiItem.template.gender <= 2)
                ? phoiItem.template.gender : player.gender;
        if (gender < 0 || gender > 2) {
            gender = 0;
        }

        int[] possibleSets = PLANET_SKH_SETS[gender];
        int skhId = possibleSets[Util.nextInt(possibleSets.length)];

        // Cập nhật trực tiếp trên chính món Đồ Phôi trong túi đồ để KHÔNG BAO GIỜ bị mất Đồ Phôi
        phoiItem.itemOptions.clear();
        phoiItem.itemOptions.addAll(preservedOptions);
        phoiItem.itemOptions.add(new ItemOption(skhId, 1));
        for (int subOpId : ItemService.gI().getOptionIdsBySKH(skhId)) {
            phoiItem.itemOptions.add(new ItemOption(subOpId, 1));
        }
        phoiItem.itemOptions.add(new ItemOption(30, 0)); // Không thể giao dịch

        player.inventory.gold -= GOLD_DAP_DO;
        // Chỉ trừ 3 món nguyên liệu phụ, giữ nguyên món Đồ Phôi trong túi
        for (Item item : player.combineNew.itemsCombine) {
            if (item != phoiItem) {
                InventoryService.gI().subQuantityItemsBag(player, item, 1);
            }
        }

        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã đập thành công " + phoiItem.template.name + " Kích Hoạt!");

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
