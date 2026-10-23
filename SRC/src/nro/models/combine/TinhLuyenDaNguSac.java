package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 * Tinh Luyện Đá Ngũ Sắc (ID 674) tại Bà Hạt Mít - Đảo Kame:
 * 1. Đổi Quà Đá Ngũ Sắc: Đặt x10 Đá Ngũ Sắc + 100 Tr Vàng -> nhận Ngọc Rồng Tím (5s-7s), Hồng Ngọc, SPL Cấp 2 + Hematite hoặc Capsule Kích Hoạt.
 * 2. Tinh Luyện Cải Trang: Đặt 1 Cải trang vĩnh viễn + x5 Đá Ngũ Sắc + 200 Tr Vàng -> +1~2% SĐ/HP/KI (tối đa 45% mỗi dòng).
 */
public class TinhLuyenDaNguSac {

    private static final int DA_NGU_SAC_ID = 674;
    private static final int COST_EXCHANGE_DNS = 10;
    private static final long GOLD_EXCHANGE = 100_000_000L;

    private static final int COST_UPGRADE_DNS = 5;
    private static final long GOLD_UPGRADE = 200_000_000L;
    private static final int MAX_STAT_PERCENT = 45;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.isEmpty()) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hãy chọn:\n"
                    + "1) Chỉ chọn Đá Ngũ Sắc (x10) để Đổi Quà Quý\n"
                    + "2) Hoặc chọn 1 Cải trang vĩnh viễn + Đá Ngũ Sắc (x5) để Tinh Luyện Cải Trang!", "Đóng");
            return;
        }

        if (player.combineNew.itemsCombine.size() == 1) {
            Item item = player.combineNew.itemsCombine.get(0);
            if (item != null && item.isNotNullItem() && item.template.id == DA_NGU_SAC_ID) {
                StringBuilder npcSay = new StringBuilder();
                npcSay.append("|7|=== ĐỔI QUÀ ĐÁ NGŨ SẮC ===\n");
                npcSay.append("|2|Nguyên liệu yêu cầu:\n");
                npcSay.append(item.quantity >= COST_EXCHANGE_DNS ? "|1|" : "|7|")
                        .append("- Đá Ngũ Sắc: ").append(item.quantity).append(" / ").append(COST_EXCHANGE_DNS).append(" viên\n");
                npcSay.append(player.inventory.gold >= GOLD_EXCHANGE ? "|1|" : "|7|")
                        .append("- Vàng: ").append(Util.numberToMoney(GOLD_EXCHANGE)).append("\n");
                npcSay.append("|2|Phần thưởng ngẫu nhiên (100% trúng):\n");
                npcSay.append("|0|+ Ngọc Rồng Tím 5 sao - 7 sao (35%)\n");
                npcSay.append("|0|+ 50 - 150 Hồng Ngọc (30%)\n");
                npcSay.append("|0|+ 1 Sao Pha Lê Cấp 2 + 2 Đá Hematite (20%)\n");
                npcSay.append("|0|+ 1 Capsule Kích Hoạt (15%)");

                if (item.quantity < COST_EXCHANGE_DNS || player.inventory.gold < GOLD_EXCHANGE) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            npcSay.toString(), "Còn thiếu\nnguyên liệu");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                            npcSay.toString(), "Đổi quà\nngay", "Từ chối");
                }
                return;
            }
        }

        if (player.combineNew.itemsCombine.size() == 2) {
            Item caiTrang = null;
            Item daNguSac = null;
            for (Item it : player.combineNew.itemsCombine) {
                if (it != null && it.isNotNullItem()) {
                    if (it.template.id == DA_NGU_SAC_ID) {
                        daNguSac = it;
                    } else if (it.template.type == 5) {
                        caiTrang = it;
                    }
                }
            }
            if (caiTrang != null && daNguSac != null) {
                if (hasExpireOption(caiTrang)) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Không thể tinh luyện Cải trang có hạn sử dụng!", "Đóng");
                    return;
                }
                int sd = getOptionParam(caiTrang, 50);
                int hp = getOptionParam(caiTrang, 77);
                int ki = getOptionParam(caiTrang, 103);
                if (sd >= MAX_STAT_PERCENT && hp >= MAX_STAT_PERCENT && ki >= MAX_STAT_PERCENT) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Cải trang này đã đạt giới hạn tinh luyện tối đa (" + MAX_STAT_PERCENT + "% SĐ/HP/KI)!", "Đóng");
                    return;
                }

                StringBuilder npcSay = new StringBuilder();
                npcSay.append("|7|=== TINH LUYỆN CẢI TRANG ===\n");
                npcSay.append("|1|").append(caiTrang.template.name).append("\n");
                npcSay.append("|0|Hiện tại: +").append(sd).append("% SĐ | +").append(hp).append("% HP | +").append(ki).append("% KI\n");
                npcSay.append("|2|Sau khi tinh luyện: Tăng thêm +1% đến +2% cho cả SĐ, HP, KI (Tối đa ").append(MAX_STAT_PERCENT).append("%)\n");
                npcSay.append(daNguSac.quantity >= COST_UPGRADE_DNS ? "|1|" : "|7|")
                        .append("- Đá Ngũ Sắc: ").append(daNguSac.quantity).append(" / ").append(COST_UPGRADE_DNS).append(" viên\n");
                npcSay.append(player.inventory.gold >= GOLD_UPGRADE ? "|1|" : "|7|")
                        .append("- Chi phí: ").append(Util.numberToMoney(GOLD_UPGRADE)).append(" vàng\n");
                npcSay.append("|2|Tỉ lệ thành công: 50% (Thất bại không mất Cải trang)");

                if (daNguSac.quantity < COST_UPGRADE_DNS || player.inventory.gold < GOLD_UPGRADE) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            npcSay.toString(), "Còn thiếu\nnguyên liệu");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                            npcSay.toString(), "Tinh luyện\nngay", "Từ chối");
                }
                return;
            }
        }

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                "Công thức không hợp lệ!\n"
                + "- Đổi quà: Chỉ đặt Đá Ngũ Sắc (x10)\n"
                + "- Tinh luyện Cải trang: Đặt 1 Cải trang vĩnh viễn + Đá Ngũ Sắc (x5)", "Đóng");
    }

    public static void thucHienTinhLuyen(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item dns = player.combineNew.itemsCombine.get(0);
            if (dns == null || !dns.isNotNullItem() || dns.template.id != DA_NGU_SAC_ID || dns.quantity < COST_EXCHANGE_DNS) {
                Service.gI().sendThongBao(player, "Cần ít nhất " + COST_EXCHANGE_DNS + " Đá Ngũ Sắc!");
                return;
            }
            if (player.inventory.gold < GOLD_EXCHANGE) {
                Service.gI().sendThongBao(player, "Không đủ " + Util.numberToMoney(GOLD_EXCHANGE) + " vàng!");
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                Service.gI().sendThongBao(player, "Cần ít nhất 2 ô trống trong hành trang!");
                return;
            }

            player.inventory.gold -= GOLD_EXCHANGE;
            InventoryService.gI().subQuantityItemsBag(player, dns, COST_EXCHANGE_DNS);

            int roll = Util.nextInt(1, 100);
            String rewardText;
            if (roll <= 35) {
                int nrTimId = Util.nextInt(1831, 1833);
                Item nrTim = ItemService.gI().createNewItem((short) nrTimId, 1);
                InventoryService.gI().addItemBag(player, nrTim);
                rewardText = "1 " + nrTim.template.name;
            } else if (roll <= 65) {
                int ruby = Util.nextInt(50, 150);
                long newRuby = (long) player.inventory.ruby + ruby;
                player.inventory.ruby = (int) Math.min(newRuby, Integer.MAX_VALUE);
                rewardText = ruby + " Hồng Ngọc";
            } else if (roll <= 85) {
                int idx = Util.nextInt(0, 6);
                Item spl2 = ItemService.gI().createNewItem((short) (1416 + idx), 1);
                spl2.itemOptions.clear();
                switch (idx) {
                    case 0 -> spl2.itemOptions.add(new ItemOption(95, 5));
                    case 1 -> spl2.itemOptions.add(new ItemOption(96, 5));
                    case 2 -> spl2.itemOptions.add(new ItemOption(97, 5));
                    case 3 -> spl2.itemOptions.add(new ItemOption(99, 3));
                    case 4 -> spl2.itemOptions.add(new ItemOption(98, 3));
                    case 5 -> spl2.itemOptions.add(new ItemOption(100, 5));
                    default -> spl2.itemOptions.add(new ItemOption(101, 5));
                }
                Item hematite = ItemService.gI().createNewItem((short) 1423, 2);
                InventoryService.gI().addItemBag(player, spl2);
                InventoryService.gI().addItemBag(player, hematite);
                rewardText = "1 " + spl2.template.name + " và 2 Đá Hematite";
            } else {
                Item capsule = ItemService.gI().createNewItem((short) 1655, 1);
                InventoryService.gI().addItemBag(player, capsule);
                rewardText = "1 Capsule Kích Hoạt";
            }

            CombineService.gI().sendEffectSuccessCombine(player);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
            Service.gI().sendThongBao(player, "Đổi quà Đá Ngũ Sắc thành công! Nhận được: " + rewardText);
            return;
        }

        if (player.combineNew.itemsCombine.size() == 2) {
            Item caiTrang = null;
            Item daNguSac = null;
            for (Item it : player.combineNew.itemsCombine) {
                if (it != null && it.isNotNullItem()) {
                    if (it.template.id == DA_NGU_SAC_ID) {
                        daNguSac = it;
                    } else if (it.template.type == 5) {
                        caiTrang = it;
                    }
                }
            }
            if (caiTrang == null || daNguSac == null || hasExpireOption(caiTrang)) {
                Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ!");
                return;
            }
            if (daNguSac.quantity < COST_UPGRADE_DNS) {
                Service.gI().sendThongBao(player, "Cần ít nhất " + COST_UPGRADE_DNS + " Đá Ngũ Sắc!");
                return;
            }
            if (player.inventory.gold < GOLD_UPGRADE) {
                Service.gI().sendThongBao(player, "Không đủ " + Util.numberToMoney(GOLD_UPGRADE) + " vàng!");
                return;
            }
            int sd = getOptionParam(caiTrang, 50);
            int hp = getOptionParam(caiTrang, 77);
            int ki = getOptionParam(caiTrang, 103);
            if (sd >= MAX_STAT_PERCENT && hp >= MAX_STAT_PERCENT && ki >= MAX_STAT_PERCENT) {
                Service.gI().sendThongBao(player, "Cải trang này đã đạt giới hạn tối đa " + MAX_STAT_PERCENT + "%!");
                return;
            }

            player.inventory.gold -= GOLD_UPGRADE;
            InventoryService.gI().subQuantityItemsBag(player, daNguSac, COST_UPGRADE_DNS);

            if (Util.isTrue(50, 100)) {
                int bonus = Util.nextInt(1, 2);
                addOrUpdateOption(caiTrang, 50, Math.min(MAX_STAT_PERCENT, sd + bonus));
                addOrUpdateOption(caiTrang, 77, Math.min(MAX_STAT_PERCENT, hp + bonus));
                addOrUpdateOption(caiTrang, 103, Math.min(MAX_STAT_PERCENT, ki + bonus));
                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player, "Tinh luyện thành công! Cải trang được tăng thêm +" + bonus + "% SĐ/HP/KI!");
            } else {
                CombineService.gI().sendEffectFailCombine(player);
                Service.gI().sendThongBao(player, "Tinh luyện thất bại! Cải trang của bạn vẫn an toàn.");
            }

            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }

    private static boolean hasExpireOption(Item item) {
        if (item == null || item.itemOptions == null) {
            return false;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == 93) {
                return true;
            }
        }
        return false;
    }

    private static int getOptionParam(Item item, int optionId) {
        if (item == null || item.itemOptions == null) {
            return 0;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                return io.param;
            }
        }
        return 0;
    }

    private static void addOrUpdateOption(Item item, int optionId, int newParam) {
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                io.param = newParam;
                return;
            }
        }
        item.itemOptions.add(new ItemOption(optionId, newParam));
    }
}
