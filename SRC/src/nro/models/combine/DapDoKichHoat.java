package nro.models.combine;

import java.util.ArrayList;
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
 * Đập đồ Kích Hoạt tại Bà Hạt Mít - Đảo Kame:
 * - Nguyên liệu: 3 món Đồ Thần Linh (hoặc 3 Đồ Hủy Diệt / 3 Đồ Thiên Sứ) + 1 món Đồ Kích Hoạt làm Phôi.
 * - Sản phẩm: 1 món trang bị Kích Hoạt mới đúng theo loại & hành tinh của món Phôi (VD: Phôi là Quần KH -> ra Quần KH mới,
 *   Phôi là Găng KH -> ra Găng KH mới) với Cấp sức mạnh ngẫu nhiên.
 * - Cấp sức mạnh càng cao thì tỷ lệ ra càng hiếm:
 *   + Dùng 3 Đồ Thần Linh: Ra đồ Cấp 1 -> Đồ Thần Linh (Mốc Đồ Thần Linh: 5%).
 *   + Dùng 3 Đồ Hủy Diệt: Ra đồ Cấp 1 -> Đồ Hủy Diệt (Mốc Đồ Hủy Diệt: 5%, Đồ Thần Linh: 7%).
 *   + Dùng 3 Đồ Thiên Sứ: Ra đồ Cấp 1 -> Đồ Thiên Sứ (Mốc Đồ Thiên Sứ: 5%, Đồ Hủy Diệt: 6%, Đồ Thần Linh: 8%).
 */
public class DapDoKichHoat {

    public static final int TIER_THAN_LINH = 1;
    public static final int TIER_HUY_DIET = 2;
    public static final int TIER_THIEN_SU = 3;

    private static final long GOLD_THAN_LINH = 500_000_000L;
    private static final long GOLD_HUY_DIET = 1_000_000_000L;
    private static final long GOLD_THIEN_SU = 2_000_000_000L;

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
     * Bảng ID trang bị theo [gender (0:TĐ, 1:NM, 2:XD)][type (0:Áo, 1:Quần, 2:Găng, 3:Giày, 4:Rada/Nhẫn)][tierIndex (0..14)]:
     * - tierIndex 0..11: Cấp 1 -> Cấp 12 (theo mốc sức mạnh yêu cầu tăng dần)
     * - tierIndex 12: Cấp 13 (Đồ Thần Linh)
     * - tierIndex 13: Cấp 14 (Đồ Hủy Diệt)
     * - tierIndex 14: Cấp 15 (Đồ Thiên Sứ)
     */
    private static final int[][][] EQUIP_BY_GENDER_TYPE_TIER = {
        { // Trái Đất (gender = 0)
            {0, 33, 3, 34, 136, 137, 138, 139, 230, 231, 232, 233, 555, 650, 1048},   // Áo
            {6, 35, 9, 36, 140, 141, 142, 143, 242, 243, 244, 245, 556, 651, 1051},   // Quần
            {21, 24, 37, 38, 144, 145, 146, 147, 254, 255, 256, 257, 562, 657, 1054}, // Găng
            {27, 30, 39, 40, 148, 149, 150, 151, 266, 267, 268, 269, 563, 658, 1057}, // Giày
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561, 656, 1060}  // Rada / Nhẫn
        },
        { // Namek (gender = 1)
            {1, 41, 4, 42, 152, 153, 154, 155, 234, 235, 236, 237, 557, 652, 1049},   // Áo
            {7, 43, 10, 44, 156, 157, 158, 159, 246, 247, 248, 249, 558, 653, 1052},  // Quần
            {22, 46, 25, 45, 160, 161, 162, 163, 258, 259, 260, 261, 564, 659, 1055}, // Găng
            {28, 47, 31, 48, 164, 165, 166, 167, 270, 271, 272, 273, 565, 660, 1058}, // Giày
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561, 656, 1061}  // Rada / Nhẫn
        },
        { // Xayda (gender = 2)
            {2, 49, 5, 50, 168, 169, 170, 171, 238, 239, 240, 241, 559, 654, 1050},   // Áo
            {8, 51, 11, 52, 172, 173, 174, 175, 250, 251, 252, 253, 560, 655, 1053},  // Quần
            {23, 53, 26, 54, 176, 177, 178, 179, 262, 263, 264, 265, 566, 661, 1056}, // Găng
            {29, 55, 32, 56, 180, 181, 182, 183, 274, 275, 276, 277, 567, 662, 1059}, // Giày
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561, 656, 1062}  // Rada / Nhẫn
        }
    };

    /**
     * Trọng số xác suất (tổng = 10.000 tương ứng 100.00%) theo cấp sức mạnh tăng dần (cấp càng cao tỷ lệ càng hiếm):
     * - WEGHTS_THAN_LINH (13 mốc: Cấp 1..12 + Đồ Thần Linh 5% = 500/10000)
     * - WEIGHTS_HUY_DIET (14 mốc: Cấp 1..12 + Đồ Thần Linh 7% + Đồ Hủy Diệt 5% = 500/10000)
     * - WEIGHTS_THIEN_SU (15 mốc: Cấp 1..12 + Đồ Thần Linh 8% + Đồ Hủy Diệt 6% + Đồ Thiên Sứ 5% = 500/10000)
     */
    private static final int[] WEIGHTS_THAN_LINH = {
        1400, 1250, 1100, 950, 850, 750, 700, 650, 550, 500, 450, 350,
        500 // Cấp 13: Đồ Thần Linh (5%)
    };

    private static final int[] WEIGHTS_HUY_DIET = {
        1300, 1150, 1000, 900, 800, 700, 650, 600, 500, 450, 400, 350,
        700, // Cấp 13: Đồ Thần Linh (7%)
        500  // Cấp 14: Đồ Hủy Diệt (5%)
    };

    private static final int[] WEIGHTS_THIEN_SU = {
        1200, 1050, 950, 850, 750, 650, 600, 550, 450, 400, 350, 300,
        800, // Cấp 13: Đồ Thần Linh (8%)
        600, // Cấp 14: Đồ Hủy Diệt (6%)
        500  // Cấp 15: Đồ Thiên Sứ (5%)
    };

    public static class CombineRecipe {
        public final Item phoiSkh;
        public final List<Item> materialItems;
        public final int materialTier;

        public CombineRecipe(Item phoiSkh, List<Item> materialItems, int materialTier) {
            this.phoiSkh = phoiSkh;
            this.materialItems = materialItems;
            this.materialTier = materialTier;
        }
    }

    public static int getMaterialTier(Item item) {
        if (!isEquipItem(item)) {
            return 0;
        }
        if (item.isDTS() || item.template.level >= 15) {
            return TIER_THIEN_SU;
        }
        if (item.isDHD() || item.template.level == 14) {
            return TIER_HUY_DIET;
        }
        if (item.isDTL() || item.template.level == 13) {
            return TIER_THAN_LINH;
        }
        return 0;
    }

    public static String getMaterialTierName(int tier) {
        return switch (tier) {
            case TIER_THIEN_SU -> "Đồ Thiên Sứ";
            case TIER_HUY_DIET -> "Đồ Hủy Diệt";
            default -> "Đồ Thần Linh";
        };
    }

    public static long getGoldCostForTier(int tier) {
        return switch (tier) {
            case TIER_THIEN_SU -> GOLD_THIEN_SU;
            case TIER_HUY_DIET -> GOLD_HUY_DIET;
            default -> GOLD_THAN_LINH;
        };
    }

    /**
     * Phân tích 4 vật phẩm người chơi đặt vào thành:
     * - 1 món Đồ Kích Hoạt làm Phôi (phoiSkh)
     * - 3 món trang bị cấp cao cùng bậc (3 Đồ Thần Linh, hoặc 3 Đồ Hủy Diệt, hoặc 3 Đồ Thiên Sứ)
     */
    public static CombineRecipe resolveRecipe(List<Item> items) {
        if (items == null || items.size() != 4) {
            return null;
        }
        for (Item item : items) {
            if (!isEquipItem(item) || item.quantity < 1) {
                return null;
            }
        }

        int[] candidateTiers = {TIER_THAN_LINH, TIER_HUY_DIET, TIER_THIEN_SU};
        for (int tier : candidateTiers) {
            List<Item> matchingTierItems = new ArrayList<>();
            List<Item> otherItems = new ArrayList<>();
            for (Item item : items) {
                if (getMaterialTier(item) == tier) {
                    matchingTierItems.add(item);
                } else {
                    otherItems.add(item);
                }
            }

            if (matchingTierItems.size() == 3 && otherItems.size() == 1) {
                Item candidatePhoi = otherItems.get(0);
                if (isItemKichHoat(candidatePhoi)) {
                    return new CombineRecipe(candidatePhoi, matchingTierItems, tier);
                }
            } else if (matchingTierItems.size() == 4) {
                // Cả 4 món đều cùng bậc (VD: dùng 1 món Thần Linh SKH làm phôi + 3 món Thần Linh khác)
                Item chosenPhoi = null;
                for (Item item : items) {
                    if (isItemKichHoat(item) && !hasUpgradedStarOrLevel(item)) {
                        chosenPhoi = item;
                        break;
                    }
                }
                if (chosenPhoi == null) {
                    for (Item item : items) {
                        if (isItemKichHoat(item)) {
                            chosenPhoi = item;
                            break;
                        }
                    }
                }
                if (chosenPhoi != null) {
                    List<Item> mats = new ArrayList<>();
                    for (Item item : items) {
                        if (item != chosenPhoi) {
                            mats.add(item);
                        }
                    }
                    return new CombineRecipe(chosenPhoi, mats, tier);
                }
            }
        }
        return null;
    }

    public static boolean hasUpgradedStarOrLevel(Item item) {
        if (item == null || !item.isNotNullItem() || item.itemOptions == null) {
            return false;
        }
        for (ItemOption io : item.itemOptions) {
            if (io == null || io.optionTemplate == null) {
                continue;
            }
            int id = io.optionTemplate.id;
            if ((id == 72 && io.param > 0) || (id == 102 && io.param > 0)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidSkhPhoiForBulkSelect(Item item) {
        return isEquipItem(item)
                && isItemKichHoat(item)
                && getMaterialTier(item) == 0
                && !hasUpgradedStarOrLevel(item);
    }

    public static boolean isValidMaterialForBulkSelect(Item item, int targetTier) {
        return isEquipItem(item)
                && getMaterialTier(item) == targetTier
                && !isItemKichHoat(item)
                && !hasUpgradedStarOrLevel(item);
    }

    public static boolean isValidFodderForBulkSelect(Item item) {
        return isValidSkhPhoiForBulkSelect(item) || isValidMaterialForBulkSelect(item, TIER_THAN_LINH);
    }

    private static int resolveTargetGender(Item phoiSkh, int defaultGender) {
        if (phoiSkh != null && phoiSkh.template != null) {
            if (phoiSkh.template.gender >= 0 && phoiSkh.template.gender <= 2) {
                return phoiSkh.template.gender;
            }
            if (phoiSkh.itemOptions != null) {
                for (ItemOption io : phoiSkh.itemOptions) {
                    if (io == null || io.optionTemplate == null) {
                        continue;
                    }
                    int id = io.optionTemplate.id;
                    for (int g = 0; g < PLANET_SKH_SETS.length; g++) {
                        for (int skhId : PLANET_SKH_SETS[g]) {
                            if (id == skhId) {
                                return g;
                            }
                        }
                    }
                }
            }
        }
        return (defaultGender >= 0 && defaultGender <= 2) ? defaultGender : 0;
    }

    private static String getEquipTypeName(int type) {
        return switch (type) {
            case 0 -> "Áo";
            case 1 -> "Quần";
            case 2 -> "Găng";
            case 3 -> "Giày";
            case 4 -> "Rada / Nhẫn";
            default -> "Trang bị";
        };
    }

    private static int rollRandomTierIndex(int materialTier) {
        int[] weights = switch (materialTier) {
            case TIER_THIEN_SU -> WEIGHTS_THIEN_SU;
            case TIER_HUY_DIET -> WEIGHTS_HUY_DIET;
            default -> WEIGHTS_THAN_LINH;
        };
        int totalWeight = 0;
        for (int w : weights) {
            totalWeight += w;
        }
        int roll = Util.nextInt(1, totalWeight);
        int cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (roll <= cumulative) {
                return i;
            }
        }
        return 0;
    }

    private static Item createRandomSkhProduct(int gender, int type, int tierIndex) {
        int safeGender = (gender >= 0 && gender <= 2) ? gender : 0;
        int safeType = (type >= 0 && type <= 4) ? type : 0;
        int[] tierTable = EQUIP_BY_GENDER_TYPE_TIER[safeGender][safeType];
        int safeTierIdx = Math.max(0, Math.min(tierIndex, tierTable.length - 1));
        int targetTempId = tierTable[safeTierIdx];

        Item newItem;
        if (safeTierIdx == 14) {
            // Đồ Thiên Sứ
            newItem = ItemService.gI().DoThienSu(targetTempId, safeGender);
        } else {
            newItem = ItemService.gI().createNewItem((short) targetTempId, 1);
            newItem.itemOptions.clear();
            RewardService.gI().initBaseOptionClothes(newItem, newItem.template.id, newItem.template.type, newItem.itemOptions);
            if (newItem.itemOptions.isEmpty()) {
                List<ItemOption> shopOpts = ItemService.gI().getListOptionItemShop((short) targetTempId);
                if (shopOpts != null && !shopOpts.isEmpty()) {
                    for (ItemOption io : shopOpts) {
                        newItem.itemOptions.add(new ItemOption(io));
                    }
                }
            }
        }

        // Xóa option 30 cũ (nếu có) để gắn lại ở cuối danh sách sau dòng SKH cho đẹp
        newItem.itemOptions.removeIf(io -> io != null && io.optionTemplate != null
                && (io.optionTemplate.id == 30 || isSkhOptionId(io.optionTemplate.id)));

        int[] possibleSets = PLANET_SKH_SETS[safeGender];
        int skhId = possibleSets[Util.nextInt(possibleSets.length)];
        newItem.itemOptions.add(new ItemOption(skhId, 1));
        for (int subOpId : ItemService.gI().getOptionIdsBySKH(skhId)) {
            newItem.itemOptions.add(new ItemOption(subOpId, 1));
        }
        newItem.itemOptions.add(new ItemOption(30, 0)); // Không thể giao dịch
        return newItem;
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 4) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "Cần đặt đúng 4 vật phẩm gồm:\n"
                    + "- 1 món Đồ Kích Hoạt làm Phôi (Áo, Quần, Găng, Giày, Rada)\n"
                    + "- 3 món Đồ Thần Linh (hoặc 3 Đồ Hủy Diệt / 3 Đồ Thiên Sứ)\n"
                    + "|6|Sản phẩm sẽ theo loại của Phôi KH và ngẫu nhiên Cấp sức mạnh (Đồ Thần: 5%)!\n"
                    + "|7|(Mẹo: Bấm 'Chọn nhanh Nguyên liệu' bên dưới để tự chọn 1 Phôi KH + 3 Đồ Thần)",
                    "Đóng", "Chọn nhanh\nNguyên liệu", "Đổi sang\nTrang kế");
            return;
        }

        CombineRecipe recipe = resolveRecipe(player.combineNew.itemsCombine);
        if (recipe == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "Công thức không hợp lệ! Cần đúng:\n"
                    + "- 1 món Đồ Kích Hoạt làm Phôi (Áo, Quần, Găng, Giày, Rada)\n"
                    + "- 3 món cùng bậc: 3 Đồ Thần Linh, hoặc 3 Đồ Hủy Diệt, hoặc 3 Đồ Thiên Sứ!",
                    "Đóng", "Chọn nhanh\nNguyên liệu", "Đổi sang\nTrang kế");
            return;
        }

        Item phoiItem = recipe.phoiSkh;
        StringBuilder matList = new StringBuilder();
        for (Item mat : recipe.materialItems) {
            matList.append("|0|- ").append(mat.template.name).append("\n");
        }

        long goldCost = getGoldCostForTier(recipe.materialTier);
        player.combineNew.goldCombine = (int) Math.min(Integer.MAX_VALUE, goldCost);
        player.combineNew.ratioCombine = 100;

        int targetGender = resolveTargetGender(phoiItem, player.gender);
        String planetName = targetGender == 0 ? "Trái Đất" : (targetGender == 1 ? "Namek" : "Xayda");
        String equipTypeName = getEquipTypeName(phoiItem.template.type);
        String tierName = getMaterialTierName(recipe.materialTier);

        String rateSummary = switch (recipe.materialTier) {
            case TIER_THIEN_SU -> "Cấp 1->12 (81%) | Đồ Thần (8%) | Đồ Hủy Diệt (6%) | Đồ Thiên Sứ (5%)";
            case TIER_HUY_DIET -> "Cấp 1->12 (88%) | Đồ Thần (7%) | Đồ Hủy Diệt (5%)";
            default -> "Cấp 1->12 (95% - cấp càng cao càng hiếm) | Đồ Thần Linh (5%)";
        };

        String npcSay = "|2|Đập 3 " + tierName + " + 1 Phôi Kích Hoạt\n"
                + "|6|Phôi định hình loại trang bị: " + phoiItem.template.name + " (" + equipTypeName + " " + planetName + ")\n"
                + "|2|3 Món " + tierName + " nguyên liệu:\n" + matList
                + "|1|Sản phẩm nhận được: 1 " + equipTypeName + " Kích Hoạt (" + planetName + ") ngẫu nhiên Cấp sức mạnh!\n"
                + "|7|Tỷ lệ ra Cấp sức mạnh: " + rateSummary + "\n"
                + "|2|Chi phí: " + Util.numberToMoney(goldCost) + " vàng\n";

        if (player.inventory.gold < goldCost) {
            npcSay += "|7|Còn thiếu " + Util.powerToString(goldCost - player.inventory.gold) + " vàng\n";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Đập đồ\n" + Util.numberToMoney(goldCost) + " vàng",
                    "Chọn nhanh\nNguyên liệu",
                    "Đổi sang\nTrang kế",
                    "Từ chối");
        }
    }

    public static void thucHienDapDo(Player player) {
        if (player.combineNew.itemsCombine.size() != 4) {
            Service.gI().sendThongBao(player, "Cần đặt đúng 3 món Thần Linh (hoặc Hủy Diệt/Thiên Sứ) + 1 món Đồ Kích Hoạt làm phôi!");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Bạn cần có ít nhất 1 ô trống trong hành trang!");
            return;
        }

        CombineRecipe recipe = resolveRecipe(player.combineNew.itemsCombine);
        if (recipe == null) {
            Service.gI().sendThongBao(player, "Cần đúng 3 món Thần Linh (hoặc 3 Hủy Diệt / 3 Thiên Sứ) + 1 món Đồ Kích Hoạt làm phôi!");
            return;
        }

        long goldCost = getGoldCostForTier(recipe.materialTier);
        if (player.inventory.gold < goldCost) {
            Service.gI().sendThongBao(player, "Không đủ " + Util.numberToMoney(goldCost) + " vàng để thực hiện!");
            return;
        }

        Item phoiItem = recipe.phoiSkh;
        int targetType = phoiItem.template.type;
        int targetGender = resolveTargetGender(phoiItem, player.gender);
        int rolledTierIndex = rollRandomTierIndex(recipe.materialTier);

        Item productItem = createRandomSkhProduct(targetGender, targetType, rolledTierIndex);

        player.inventory.gold -= goldCost;
        InventoryService.gI().subQuantityItemsBag(player, phoiItem, 1);
        for (Item mat : recipe.materialItems) {
            InventoryService.gI().subQuantityItemsBag(player, mat, 1);
        }

        InventoryService.gI().addItemBag(player, productItem);
        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendThongBao(player, "Chúc mừng! Bạn nhận được " + productItem.template.name + " Kích Hoạt (" + productItem.getContent() + ")!");

        player.combineNew.itemsCombine.clear();
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
