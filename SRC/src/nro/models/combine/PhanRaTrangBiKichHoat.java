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
 * Phân rã Trang bị Kích Hoạt tại Bà Hạt Mít - Đảo Kame:
 * Nhận Vàng, Hồng Ngọc, Đá Ngũ Sắc, Khoáng Tái Chế và Thức Ăn Hủy Diệt
 * giống Phân rã Đồ Thần Linh nhưng số lượng tùy theo Sức mạnh yêu cầu của trang bị.
 */
public class PhanRaTrangBiKichHoat {

    private static final int[] THUC_AN_IDS = {663, 664, 665, 666, 667};

    private static final int[] VALID_SKH_OPTION_IDS = {
        127, 128, 129, 130, 131, 132, 133, 134, 135,
        136, 137, 138, 139, 140, 141, 142, 143, 144,
        210, 211, 212, 213, 214, 215, 216, 217, 218,
        233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248
    };

    public static class PhanRaTrangBi {

        public static void showInfoCombine(Player player) {
            if (player.combineNew.itemsCombine.isEmpty()) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Hãy chọn ít nhất 1 món Trang bị Kích Hoạt để phân rã!", "Đóng");
                return;
            }

            StringBuilder listInfo = new StringBuilder();
            int countTier1 = 0; // < 5M SM
            int countTier2 = 0; // 5M - < 100M SM
            int countTier3 = 0; // 100M - < 15B SM
            int countTier4 = 0; // 15B - < 40B SM (Thần Linh)
            int countTier5 = 0; // >= 40B SM (Hủy Diệt / Thiên Sứ)

            for (Item item : player.combineNew.itemsCombine) {
                if (item == null || !item.isNotNullItem()) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Vật phẩm không hợp lệ!", "Đóng");
                    return;
                }
                if (!isValidItem(item)) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            item.template.name + " không phải là Trang bị Kích Hoạt hợp lệ!", "Đóng");
                    return;
                }
                long power = getPowerRequire(item);
                int tier = getPowerTier(power);
                listInfo.append("|1|- ").append(item.template.name)
                        .append(" [SM: ").append(Util.powerToString(power)).append("]\n");
                switch (tier) {
                    case 1 -> countTier1++;
                    case 2 -> countTier2++;
                    case 3 -> countTier3++;
                    case 4 -> countTier4++;
                    default -> countTier5++;
                }
            }

            StringBuilder npcSay = new StringBuilder();
            npcSay.append("|2|Phân rã ").append(player.combineNew.itemsCombine.size()).append(" món Đồ Kích Hoạt:\n");
            npcSay.append(listInfo);
            npcSay.append("|2|Phần thưởng nhận được (tùy sức mạnh):\n");
            if (countTier1 > 0) {
                npcSay.append("|0|+ SM < 5Tr (x").append(countTier1).append("): 20-50Tr Vàng, 5-15 Hồng Ngọc, 1 Đá Ngũ Sắc, 1 Khoáng TC, 1-3 Thức Ăn/món\n");
            }
            if (countTier2 > 0) {
                npcSay.append("|0|+ SM 5Tr-100Tr (x").append(countTier2).append("): 50-100Tr Vàng, 15-30 Hồng Ngọc, 1-2 Đá Ngũ Sắc, 1 Khoáng TC, 3-6 Thức Ăn/món\n");
            }
            if (countTier3 > 0) {
                npcSay.append("|0|+ SM 100Tr-15Tỷ (x").append(countTier3).append("): 100-200Tr Vàng, 30-60 Hồng Ngọc, 2-3 Đá Ngũ Sắc, 1-2 Khoáng TC, 5-10 Thức Ăn/món\n");
            }
            if (countTier4 > 0) {
                npcSay.append("|0|+ SM 15Tỷ-40Tỷ (x").append(countTier4).append("): 200-400Tr Vàng, 50-100 Hồng Ngọc, 3-5 Đá Ngũ Sắc, 1-2 Khoáng TC, 10-20 Thức Ăn/món\n");
            }
            if (countTier5 > 0) {
                npcSay.append("|0|+ SM 40Tỷ+ (x").append(countTier5).append("): 400-800Tr Vàng, 100-200 Hồng Ngọc, 5-10 Đá Ngũ Sắc, 2-4 Khoáng TC, 20-40 Thức Ăn/món\n");
            }
            npcSay.append("|2|Chi phí: Miễn phí (Tỉ lệ thành công 100%)");

            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    npcSay.toString(), "Phân rã\nngay", "Từ chối");
        }

        public static void ThucHienPhanRa(Player player) {
            if (player.combineNew.itemsCombine.isEmpty()) {
                Service.gI().sendThongBao(player, "Hãy chọn ít nhất 1 món Trang bị Kích Hoạt!");
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(player) < 3) {
                Service.gI().sendThongBao(player, "Cần ít nhất 3 ô trống trong hành trang để nhận nguyên liệu!");
                return;
            }

            List<Item> validItems = new ArrayList<>();
            for (Item item : player.combineNew.itemsCombine) {
                if (item == null || !item.isNotNullItem() || item.quantity < 1 || !isValidItem(item)) {
                    Service.gI().sendThongBao(player, "Có vật phẩm không phải Trang bị Kích Hoạt trong danh sách!");
                    return;
                }
                validItems.add(item);
            }

            long totalGold = 0;
            int totalRuby = 0;
            int totalDaNguSac = 0;
            int totalKhoang = 0;
            int totalThucAn = 0;

            for (Item item : validItems) {
                long power = getPowerRequire(item);
                int tier = getPowerTier(power);
                InventoryService.gI().subQuantityItemsBag(player, item, 1);

                switch (tier) {
                    case 1 -> { // < 5 Triệu SM
                        totalGold += Util.nextInt(20, 50) * 1_000_000L;
                        totalRuby += Util.nextInt(5, 15);
                        totalDaNguSac += 1;
                        totalKhoang += 1;
                        totalThucAn += Util.nextInt(1, 3);
                    }
                    case 2 -> { // 5 Triệu - < 100 Triệu SM
                        totalGold += Util.nextInt(50, 100) * 1_000_000L;
                        totalRuby += Util.nextInt(15, 30);
                        totalDaNguSac += Util.nextInt(1, 2);
                        totalKhoang += 1;
                        totalThucAn += Util.nextInt(3, 6);
                    }
                    case 3 -> { // 100 Triệu - < 15 Tỷ SM
                        totalGold += Util.nextInt(100, 200) * 1_000_000L;
                        totalRuby += Util.nextInt(30, 60);
                        totalDaNguSac += Util.nextInt(2, 3);
                        totalKhoang += Util.nextInt(1, 2);
                        totalThucAn += Util.nextInt(5, 10);
                    }
                    case 4 -> { // 15 Tỷ - < 40 Tỷ SM (Đồ Thần Linh SKH)
                        totalGold += Util.nextInt(200, 400) * 1_000_000L;
                        totalRuby += Util.nextInt(50, 100);
                        totalDaNguSac += Util.nextInt(3, 5);
                        totalKhoang += Util.nextInt(1, 2);
                        totalThucAn += Util.nextInt(10, 20);
                    }
                    default -> { // >= 40 Tỷ SM (Đồ Hủy Diệt / Thiên Sứ SKH)
                        totalGold += Util.nextInt(400, 800) * 1_000_000L;
                        totalRuby += Util.nextInt(100, 200);
                        totalDaNguSac += Util.nextInt(5, 10);
                        totalKhoang += Util.nextInt(2, 4);
                        totalThucAn += Util.nextInt(20, 40);
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

            Service.gI().sendThongBao(player, "Phân rã Đồ Kích Hoạt thành công! Nhận: "
                    + Util.numberToMoney(totalGold) + " vàng, "
                    + totalRuby + " Hồng Ngọc, "
                    + totalDaNguSac + " Đá Ngũ Sắc, "
                    + totalKhoang + " Khoáng tái chế, "
                    + totalThucAn + " Thức ăn");
        }

        private static long getPowerRequire(Item item) {
            if (item == null || item.template == null) {
                return 0;
            }
            long power = item.template.strRequire;
            if (item.itemOptions != null) {
                for (ItemOption io : item.itemOptions) {
                    if (io != null && io.optionTemplate != null && io.optionTemplate.id == 21) {
                        power = Math.max(power, (long) io.param * 1_000_000_000L);
                    }
                }
            }
            if (item.isDTS() || item.template.level >= 15) {
                power = Math.max(power, 60_000_000_000L);
            } else if (item.isDHD() || item.template.level == 14) {
                power = Math.max(power, 40_000_000_000L);
            } else if (item.isDTL() || item.template.level == 13) {
                power = Math.max(power, 15_000_000_000L);
            }
            return power;
        }

        private static int getPowerTier(long power) {
            if (power < 5_000_000L) {
                return 1;
            } else if (power < 100_000_000L) {
                return 2;
            } else if (power < 15_000_000_000L) {
                return 3;
            } else if (power < 40_000_000_000L) {
                return 4;
            } else {
                return 5;
            }
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

        private static boolean isValidItem(Item item) {
            if (item == null || !item.isNotNullItem() || item.template == null || item.itemOptions == null) {
                return false;
            }
            if (item.template.type < 0 || item.template.type > 4) {
                return false;
            }
            for (ItemOption option : item.itemOptions) {
                if (option != null && option.optionTemplate != null) {
                    for (int validOptionId : VALID_SKH_OPTION_IDS) {
                        if (option.optionTemplate.id == validOptionId) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
