package nro.models.combine;

import java.util.ArrayList;
import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 * Chuyển hóa Trang bị tại Bà Hạt Mít - Đảo Kame:
 * Chuyển đổi toàn bộ thuộc tính: Sao Pha Lê (số lỗ SPL, số SPL đã ép, lỗ cường hóa, chỉ số SPL),
 * Cấp nâng cấp [+], và Thuộc tính Set Kích Hoạt (SKH) từ Trang bị gốc lên Phôi mới.
 * Yêu cầu: 2 tỷ vàng.
 */
public class ChuyenHoaTrangBi_Vang {

    public static final int GOLD_CHUYEN_HOA = 2_000_000_000;

    public static void chuyenHoaTrangBiVang(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "Hãy chọn 1 Trang bị gốc (có Sao Pha Lê, Cấp [+] hoặc thuộc tính SKH) và 1 Phôi trang bị mới cùng loại & cùng hành tinh!\n"
                    + "|7|(Mẹo: Bạn có thể bỏ trực tiếp 2 món trùng tên vào, hoặc chọn 1 món trước rồi bấm 'Chọn nhanh Đồ cùng loại' bên dưới)",
                    "Đóng", "Chọn nhanh\nĐồ cùng loại", "Đổi sang\nTrang kế");
            return;
        }

        Item item0 = player.combineNew.itemsCombine.get(0);
        Item item1 = player.combineNew.itemsCombine.get(1);

        if (!isEquipItem(item0) || !isEquipItem(item1)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cả 2 vật phẩm phải là trang bị (Áo, Quần, Găng, Giày hoặc Rada)!",
                    "Đóng");
            return;
        }

        if (!isCheckTrungTypevsGender(item0, item1)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Trang bị gốc và Phôi mới phải cùng loại (Áo, Quần, Găng, Giày, Rada) và cùng hành tinh!",
                    "Đóng");
            return;
        }

        Item[] resolved = resolveSourceAndTarget(item0, item1);
        Item trangBiGoc = resolved[0];
        Item trangBiCanChuyenHoa = resolved[1];

        if (!hasTransferableAttributes(trangBiGoc)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Trang bị gốc phải có ít nhất 1 trong các thuộc tính: Sao Pha Lê, Cấp [+] hoặc Thuộc tính SKH để chuyển hóa!",
                    "Đóng");
            return;
        }

        if (XoaThuocTinhSKH.hasSkhOption(trangBiGoc) && isDoHuyDiet(trangBiCanChuyenHoa)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Thuộc tính SKH chỉ được chuyển hóa từ đồ Thần Linh trở xuống, không thể chuyển lên đồ Hủy Diệt!",
                    "Đóng");
            return;
        }

        int levelTrangBi = Math.max(getLevel(trangBiGoc), getLevel(trangBiCanChuyenHoa));
        List<ItemOption> previewOptions = buildTransferredOptions(trangBiGoc, trangBiCanChuyenHoa);

        StringBuilder npcSay = new StringBuilder();
        npcSay.append("|2|Trang bị gốc (tiêu hao): ").append(trangBiGoc.getName());
        if (getLevel(trangBiGoc) > 0) {
            npcSay.append(" [+").append(getLevel(trangBiGoc)).append("]");
        }
        npcSay.append("\n|2|Phôi mới nhận toàn bộ thuộc tính: ").append(trangBiCanChuyenHoa.getName());
        if (levelTrangBi > 0) {
            npcSay.append(" [+").append(levelTrangBi).append("]");
        }
        npcSay.append("\n");

        for (ItemOption io : previewOptions) {
            if (io == null || io.optionTemplate == null) {
                continue;
            }
            int id = io.optionTemplate.id;
            if (id == 72) {
                continue;
            }
            if (isBaseStatOption(id)) {
                npcSay.append("|1|").append(io.getOptionString()).append("\n");
            } else if (id == 209) {
                npcSay.append("|7|").append(io.getOptionString()).append("\n");
            } else if (XoaThuocTinhSKH.isSkhOption(id)) {
                npcSay.append("|6|").append(io.getOptionString()).append("\n");
            } else {
                npcSay.append("|0|").append(io.getOptionString()).append("\n");
            }
        }

        npcSay.append("|1|Chuyển qua Phôi mới: Sao Pha Lê, Cấp [+] & Thuộc tính SKH\n");
        npcSay.append("|2|Chi phí: ").append(Util.numberToMoney(GOLD_CHUYEN_HOA)).append(" vàng\n");

        if (player.inventory.gold < GOLD_CHUYEN_HOA) {
            npcSay.append("|7|Còn thiếu ").append(Util.numberToMoney(GOLD_CHUYEN_HOA - player.inventory.gold)).append(" vàng");
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    npcSay.toString(), "Đóng");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    npcSay.toString(), "Chuyển hóa\n" + Util.numberToMoney(GOLD_CHUYEN_HOA) + " vàng", "Từ chối");
        }
    }

    public static void thucHienChuyenHoa(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            return;
        }
        Item item0 = player.combineNew.itemsCombine.get(0);
        Item item1 = player.combineNew.itemsCombine.get(1);

        if (!isEquipItem(item0) || !isEquipItem(item1) || !isCheckTrungTypevsGender(item0, item1)) {
            Service.gI().sendThongBao(player, "Trang bị gốc và Phôi mới phải cùng loại và cùng hành tinh!");
            return;
        }

        Item[] resolved = resolveSourceAndTarget(item0, item1);
        Item trangBiGoc = resolved[0];
        Item trangBiCanChuyenHoa = resolved[1];

        if (!hasTransferableAttributes(trangBiGoc)) {
            Service.gI().sendThongBao(player, "Trang bị gốc chưa có Sao Pha Lê, Cấp [+] hoặc SKH để chuyển hóa!");
            return;
        }

        if (XoaThuocTinhSKH.hasSkhOption(trangBiGoc) && isDoHuyDiet(trangBiCanChuyenHoa)) {
            Service.gI().sendThongBao(player, "SKH chỉ được chuyển hóa từ đồ Thần Linh trở xuống, không thể chuyển lên đồ Hủy Diệt!");
            return;
        }

        if (player.inventory.gold < GOLD_CHUYEN_HOA) {
            Service.gI().sendThongBao(player, "Không đủ 2 tỷ vàng để thực hiện chuyển hóa!");
            return;
        }

        List<ItemOption> transferredOptions = buildTransferredOptions(trangBiGoc, trangBiCanChuyenHoa);

        // Cập nhật trực tiếp lên trangBiCanChuyenHoa trong túi để giữ nguyên vật phẩm mới, chỉ tiêu hao trangBiGoc
        trangBiCanChuyenHoa.itemOptions.clear();
        trangBiCanChuyenHoa.itemOptions.addAll(transferredOptions);

        player.inventory.gold -= GOLD_CHUYEN_HOA;
        Service.gI().sendMoney(player);
        InventoryService.gI().subQuantityItemsBag(player, trangBiGoc, 1);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendThongBao(player, "Chuyển hóa toàn bộ thuộc tính sang " + trangBiCanChuyenHoa.getName() + " thành công!");
    }

    /**
     * Tự động nhận diện Trang bị gốc (có SPL / Cấp [+] / SKH) và Phôi mới.
     * Nếu người chơi bỏ Phôi mới vào trước và Trang bị gốc vào sau thì hệ thống vẫn tự hiểu đúng.
     */
    public static Item[] resolveSourceAndTarget(Item item0, Item item1) {
        boolean core0 = hasCoreUpgrade(item0);
        boolean core1 = hasCoreUpgrade(item1);
        if (core0 && !core1) {
            return new Item[]{item0, item1};
        }
        if (!core0 && core1) {
            return new Item[]{item1, item0};
        }
        boolean attr0 = hasTransferableAttributes(item0);
        boolean attr1 = hasTransferableAttributes(item1);
        if (attr0 && !attr1) {
            return new Item[]{item0, item1};
        }
        if (!attr0 && attr1) {
            return new Item[]{item1, item0};
        }
        return new Item[]{item0, item1};
    }

    public static List<ItemOption> buildTransferredOptions(Item trangBiGoc, Item phoiMoi) {
        List<ItemOption> result = new ArrayList<>();
        int levelGoc = getLevel(trangBiGoc);
        int rotCapGoc = getRotCap(trangBiGoc);
        int levelPhoi = getLevel(phoiMoi);
        int rotCapPhoi = getRotCap(phoiMoi);

        int finalLevel = Math.max(levelGoc, levelPhoi);
        int finalRotCap = (levelGoc >= levelPhoi) ? rotCapGoc : rotCapPhoi;

        int starSlotsGoc = getOptionParam(trangBiGoc, 107);
        int starSlotsPhoi = getOptionParam(phoiMoi, 107);
        int imbuedStarsGoc = getOptionParam(trangBiGoc, 102);
        int imbuedStarsPhoi = getOptionParam(phoiMoi, 102);
        int cuongHoaGoc = getOptionParam(trangBiGoc, 228);
        int cuongHoaPhoi = getOptionParam(phoiMoi, 228);

        // Giữ số lỗ Sao Pha Lê cao nhất giữa đồ gốc và phôi mới (VD: 6s chuyển sang phôi 7s -> thành 7s với 6 sao đã ép + 1 ô trống)
        int finalStarSlots = Math.max(starSlotsGoc, starSlotsPhoi);
        int finalImbuedStars = imbuedStarsGoc > 0 ? imbuedStarsGoc : imbuedStarsPhoi;
        finalStarSlots = Math.max(finalStarSlots, finalImbuedStars);
        int finalCuongHoa = Math.max(cuongHoaGoc, cuongHoaPhoi);

        boolean gocHasSkh = XoaThuocTinhSKH.hasSkhOption(trangBiGoc);
        boolean useGocImbuedSplStats = imbuedStarsGoc > 0 || (imbuedStarsPhoi == 0 && hasSplStatOptions(trangBiGoc));

        // 1. Giữ chỉ số gốc của Phôi mới (tính theo cấp [+] cao nhất)
        for (ItemOption io : phoiMoi.itemOptions) {
            if (io == null || io.optionTemplate == null) {
                continue;
            }
            int id = io.optionTemplate.id;
            if (id == 72 || id == 209 || id == 107 || id == 102 || id == 228) {
                continue;
            }
            if (useGocImbuedSplStats && isSplStatOption(id)) {
                continue;
            }
            if (gocHasSkh && XoaThuocTinhSKH.isSkhOption(id)) {
                continue;
            }
            int value = io.param;
            if (isBaseStatOption(id)) {
                int rawBase = levelPhoi > 0 ? unscaleStat(io.param, levelPhoi, rotCapPhoi) : io.param;
                value = finalLevel > 0 ? scaleStat(rawBase, finalLevel, finalRotCap) : rawBase;
            }
            result.add(new ItemOption(id, value));
        }

        // 2. Chuyển Cấp [+], số lần rớt cấp (209)
        if (finalLevel > 0) {
            result.add(new ItemOption(72, finalLevel));
        }
        if (finalRotCap > 0) {
            result.add(new ItemOption(209, finalRotCap));
        }

        // 3. Chuyển các dòng chỉ số Sao Pha Lê đã ép và thuộc tính SKH từ Trang bị gốc sang Phôi mới
        for (ItemOption io : trangBiGoc.itemOptions) {
            if (io == null || io.optionTemplate == null) {
                continue;
            }
            int id = io.optionTemplate.id;
            // Bỏ qua chỉ số gốc, cấp [+], rớt cấp, số lỗ sao (107/102/228 sẽ thêm chuẩn ở dưới), dòng ký gửi (86, 87) và yêu cầu SM của đồ gốc
            if (isBaseStatOption(id) || id == 72 || id == 209 || id == 107 || id == 102 || id == 228
                    || id == 21 || id == 15 || id == 41 || id == 86 || id == 87) {
                continue;
            }
            if (!useGocImbuedSplStats && isSplStatOption(id)) {
                continue;
            }
            if ((id == 30 || id == 73) && hasOptionList(result, id)) {
                continue;
            }
            result.add(new ItemOption(id, io.param));
        }

        // 4. Gắn số SPL đã ép (102), lỗ cường hóa (228) và tổng số lỗ SPL (107 = max(gốc, phôi))
        if (finalImbuedStars > 0) {
            result.add(new ItemOption(102, finalImbuedStars));
        }
        if (finalCuongHoa > 0) {
            result.add(new ItemOption(228, finalCuongHoa));
        }
        if (finalStarSlots > 0) {
            result.add(new ItemOption(107, finalStarSlots));
        }

        return result;
    }

    public static boolean isDoHuyDiet(Item item) {
        return item != null && item.template != null && item.template.id >= 650 && item.template.id <= 662;
    }

    public static boolean isEquipItem(Item item) {
        return item != null && item.isNotNullItem() && item.template != null
                && item.template.type >= 0 && item.template.type <= 4;
    }

    public static boolean hasCoreUpgrade(Item item) {
        if (!isEquipItem(item) || item.itemOptions == null) {
            return false;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null) {
                int id = io.optionTemplate.id;
                if ((id == 72 && io.param > 0) || (id == 102 && io.param > 0) || XoaThuocTinhSKH.isSkhOption(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasTransferableAttributes(Item item) {
        if (!isEquipItem(item) || item.itemOptions == null) {
            return false;
        }
        return hasCoreUpgrade(item) || hasSplAttributes(item);
    }

    public static boolean hasSplAttributes(Item item) {
        if (item == null || item.itemOptions == null) {
            return false;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null) {
                int id = io.optionTemplate.id;
                if ((id == 107 && io.param > 0) || (id == 102 && io.param > 0) || id == 228) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSplRelatedOption(int id) {
        return id == 107 || id == 102 || id == 228 || isSplStatOption(id);
    }

    public static boolean isSplStatOption(int id) {
        return id == 50 || id == 77 || id == 103 || id == 80 || id == 81
                || id == 94 || id == 95 || id == 96 || id == 97 || id == 98
                || id == 99 || id == 100 || id == 101 || id == 108 || id == 153 || id == 160;
    }

    public static boolean hasSplStatOptions(Item item) {
        if (item == null || item.itemOptions == null) {
            return false;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && isSplStatOption(io.optionTemplate.id)) {
                return true;
            }
        }
        return false;
    }

    public static int getOptionParam(Item item, int optionId) {
        if (item == null || item.itemOptions == null) {
            return 0;
        }
        int maxParam = 0;
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                maxParam = Math.max(maxParam, io.param);
            }
        }
        return maxParam;
    }

    public static int getLevel(Item item) {
        if (item == null || item.itemOptions == null) {
            return 0;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == 72) {
                return io.param;
            }
        }
        return 0;
    }

    public static int getRotCap(Item item) {
        if (item == null || item.itemOptions == null) {
            return 0;
        }
        int rot = 0;
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == 209) {
                rot += io.param;
            }
        }
        return rot;
    }

    public static int scaleStat(int baseParam, int levelTrangBi, int soLanRotCap) {
        return (int) Math.round(baseParam * Math.pow(1.1, levelTrangBi) * Math.pow(0.9, soLanRotCap));
    }

    public static int unscaleStat(int scaledParam, int levelTrangBi, int soLanRotCap) {
        double factor = Math.pow(1.1, levelTrangBi) * Math.pow(0.9, soLanRotCap);
        if (factor <= 0) {
            return scaledParam;
        }
        return (int) Math.round(scaledParam / factor);
    }

    public static boolean hasOption(Item item, int optionId) {
        return item != null && hasOptionList(item.itemOptions, optionId);
    }

    private static boolean hasOptionList(List<ItemOption> options, int optionId) {
        if (options == null) {
            return false;
        }
        for (ItemOption io : options) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCheckTrungTypevsGender(Item item, Item item2) {
        if (item == null || item2 == null || item.template == null || item2.template == null) {
            return false;
        }
        if (item.template.type != item2.template.type) {
            return false;
        }
        return item.template.type == 4 || item.template.gender == item2.template.gender;
    }

    public static boolean isBaseStatOption(int id) {
        return id == 0 || id == 6 || id == 7 || id == 14 || id == 22 || id == 23 || id == 27 || id == 28 || id == 47;
    }
}
