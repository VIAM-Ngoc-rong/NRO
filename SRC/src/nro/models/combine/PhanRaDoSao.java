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
 * Phân rã Đồ Sao (trang bị có Sao Pha Lê) tại Bà Hạt Mít - Đảo Kame:
 * Giúp người chơi tận dụng đồ sao rơi từ Boss để đổi lấy Vàng, Sao Pha Lê,
 * Đá Hematite, Hồng Ngọc và Capsule Kích Hoạt.
 */
public class PhanRaDoSao {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.isEmpty()) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hãy chọn ít nhất 1 trang bị có Sao Pha Lê (không phải đồ Kích Hoạt) để phân rã!", "Đóng");
            return;
        }

        StringBuilder listInfo = new StringBuilder();
        int count1_2 = 0;
        int count3_4 = 0;
        int count5_6 = 0;
        int count7_9 = 0;

        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem()) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Vật phẩm không hợp lệ!", "Đóng");
                return;
            }
            if (!CombineSystem.isTrangBiPhaLeHoa(item)) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        item.template.name + " không phải là trang bị có thể phân rã!", "Đóng");
                return;
            }
            if (DapDoKichHoat.isItemKichHoat(item)) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        item.template.name + " là trang bị Kích Hoạt!\nVui lòng dùng mục Phân rã Trang bị Kích hoạt.", "Đóng");
                return;
            }
            int star = getStar(item);
            if (star < 1) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        item.template.name + " chưa có Sao Pha Lê (cần ít nhất 1 sao)!", "Đóng");
                return;
            }
            listInfo.append("|1|- ").append(item.template.name).append(" [").append(star).append(" sao]\n");
            if (star <= 2) {
                count1_2++;
            } else if (star <= 4) {
                count3_4++;
            } else if (star <= 6) {
                count5_6++;
            } else {
                count7_9++;
            }
        }

        StringBuilder npcSay = new StringBuilder();
        npcSay.append("|2|Phân rã ").append(player.combineNew.itemsCombine.size()).append(" trang bị Sao Pha Lê:\n");
        npcSay.append(listInfo);
        npcSay.append("|2|Phần thưởng nhận được:\n");
        if (count1_2 > 0) {
            npcSay.append("|0|+ Đồ 1-2s (x").append(count1_2).append("): 20-40Tr Vàng, 1-2 SPL Cấp 1, 5-10 Hồng Ngọc/món\n");
        }
        if (count3_4 > 0) {
            npcSay.append("|0|+ Đồ 3-4s (x").append(count3_4).append("): 50-100Tr Vàng, 2-3 SPL Cấp 1, 15-30 Hồng Ngọc, 30% Đá Hematite/món\n");
        }
        if (count5_6 > 0) {
            npcSay.append("|0|+ Đồ 5-6s (x").append(count5_6).append("): 150-250Tr Vàng, 1-2 Đá Hematite, 1 SPL Cấp 2, 40-80 Hồng Ngọc/món\n");
        }
        if (count7_9 > 0) {
            npcSay.append("|0|+ Đồ 7s+ (x").append(count7_9).append("): 300-500Tr Vàng, 3-5 Đá Hematite, 1-2 SPL Cấp 2, 100-200 Hồng Ngọc, 30% Capsule KH/món\n");
        }
        npcSay.append("|2|Chi phí: Miễn phí (Tỉ lệ thành công 100%)");

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                npcSay.toString(), "Phân rã\nngay", "Từ chối");
    }

    public static void thucHienPhanRa(Player player) {
        if (player.combineNew.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player, "Hãy chọn ít nhất 1 trang bị có Sao Pha Lê!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 3) {
            Service.gI().sendThongBao(player, "Cần ít nhất 3 ô trống trong hành trang để nhận nguyên liệu!");
            return;
        }

        List<Item> validItems = new ArrayList<>();
        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem() || item.quantity < 1
                    || !CombineSystem.isTrangBiPhaLeHoa(item)
                    || DapDoKichHoat.isItemKichHoat(item)
                    || getStar(item) < 1) {
                Service.gI().sendThongBao(player, "Có vật phẩm không hợp lệ trong danh sách phân rã!");
                return;
            }
            validItems.add(item);
        }

        long totalGold = 0;
        int totalRuby = 0;
        int totalSpl1 = 0;
        int totalSpl2 = 0;
        int totalHematite = 0;
        int totalCapsuleKH = 0;

        for (Item item : validItems) {
            int star = getStar(item);
            InventoryService.gI().subQuantityItemsBag(player, item, 1);

            if (star <= 2) {
                totalGold += Util.nextInt(20, 40) * 1_000_000L;
                totalRuby += Util.nextInt(5, 10);
                int splCount = Util.nextInt(1, 2);
                for (int i = 0; i < splCount; i++) {
                    InventoryService.gI().addItemBag(player, createRandomSaoPhaLeCap1(1));
                    totalSpl1++;
                }
            } else if (star <= 4) {
                totalGold += Util.nextInt(50, 100) * 1_000_000L;
                totalRuby += Util.nextInt(15, 30);
                int splCount = Util.nextInt(2, 3);
                for (int i = 0; i < splCount; i++) {
                    InventoryService.gI().addItemBag(player, createRandomSaoPhaLeCap1(1));
                    totalSpl1++;
                }
                if (Util.isTrue(30, 100)) {
                    totalHematite += 1;
                }
            } else if (star <= 6) {
                totalGold += Util.nextInt(150, 250) * 1_000_000L;
                totalRuby += Util.nextInt(40, 80);
                totalHematite += Util.nextInt(1, 2);
                InventoryService.gI().addItemBag(player, createRandomSaoPhaLeCap2(1));
                totalSpl2++;
            } else {
                totalGold += Util.nextInt(300, 500) * 1_000_000L;
                totalRuby += Util.nextInt(100, 200);
                totalHematite += Util.nextInt(3, 5);
                int spl2Count = Util.nextInt(1, 2);
                for (int i = 0; i < spl2Count; i++) {
                    InventoryService.gI().addItemBag(player, createRandomSaoPhaLeCap2(1));
                    totalSpl2++;
                }
                if (Util.isTrue(30, 100)) {
                    totalCapsuleKH += 1;
                }
            }
        }

        if (totalHematite > 0) {
            Item hematite = ItemService.gI().createNewItem((short) 1423, totalHematite);
            InventoryService.gI().addItemBag(player, hematite);
        }
        if (totalCapsuleKH > 0) {
            Item capsule = ItemService.gI().createNewItem((short) 1655, totalCapsuleKH);
            InventoryService.gI().addItemBag(player, capsule);
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

        StringBuilder msg = new StringBuilder("Phân rã thành công! Nhận: ");
        msg.append(Util.numberToMoney(totalGold)).append(" vàng, ").append(totalRuby).append(" Hồng Ngọc");
        if (totalSpl1 > 0) {
            msg.append(", ").append(totalSpl1).append(" SPL cấp 1");
        }
        if (totalSpl2 > 0) {
            msg.append(", ").append(totalSpl2).append(" SPL cấp 2");
        }
        if (totalHematite > 0) {
            msg.append(", ").append(totalHematite).append(" Đá Hematite");
        }
        if (totalCapsuleKH > 0) {
            msg.append(", ").append(totalCapsuleKH).append(" Capsule Kích Hoạt");
        }
        Service.gI().sendThongBao(player, msg.toString());
    }

    public static int getStar(Item item) {
        if (item == null || item.itemOptions == null) {
            return 0;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == 107) {
                return io.param;
            }
        }
        return 0;
    }

    private static Item createRandomSaoPhaLeCap1(int quantity) {
        int tempId = Util.nextInt(441, 447);
        Item item = ItemService.gI().createNewItem((short) tempId, quantity);
        item.itemOptions.clear();
        addSaoPhaLeOption(item, tempId - 441);
        return item;
    }

    private static Item createRandomSaoPhaLeCap2(int quantity) {
        int index = Util.nextInt(0, 6);
        int tempId = 1416 + index;
        Item item = ItemService.gI().createNewItem((short) tempId, quantity);
        item.itemOptions.clear();
        addSaoPhaLeOption(item, index);
        return item;
    }

    private static void addSaoPhaLeOption(Item item, int colorIndex) {
        switch (colorIndex) {
            case 0 -> item.itemOptions.add(new ItemOption(95, 5));  // Đỏ: Hút HP 5%
            case 1 -> item.itemOptions.add(new ItemOption(96, 5));  // Lam: Hút KI 5%
            case 2 -> item.itemOptions.add(new ItemOption(97, 5));  // Hồng: Phản sát thương 5%
            case 3 -> item.itemOptions.add(new ItemOption(99, 3));  // Tím: Xuyên giáp cận chiến 3%
            case 4 -> item.itemOptions.add(new ItemOption(98, 3));  // Cam: Xuyên giáp chưởng 3%
            case 5 -> item.itemOptions.add(new ItemOption(100, 5)); // Vàng: Vàng 5%
            default -> item.itemOptions.add(new ItemOption(101, 5)); // Lục: TNSM 5%
        }
    }
}
