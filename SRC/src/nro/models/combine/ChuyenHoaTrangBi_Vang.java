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
 * Chuyển hóa Trang bị bằng Vàng tại Bà Hạt Mít - Đảo Kame:
 * Chuyển cấp [+], toàn bộ Sao Pha Lê (số lỗ SPL, số SPL đã ép, lỗ cường hóa) và thuộc tính Set Kích Hoạt (SKH).
 */
public class ChuyenHoaTrangBi_Vang {

    private static final int GOLD_CHUYEN_HOA = 2_000_000_000;

    public static void chuyenHoaTrangBiVang(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            Service.gI().sendThongBaoOK(player, "Cần 1 trang bị gốc có cấp từ [+4] và 1 trang bị đích chưa nâng cấp cao hơn 1 bậc");
            return;
        }

        Item trangBiGoc = player.combineNew.itemsCombine.get(0);
        Item trangBiCanChuyenHoa = player.combineNew.itemsCombine.get(1);

        int levelTrangBi = 0;
        int soLanRotCap = 0;

        for (ItemOption io : trangBiGoc.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelTrangBi = io.param;
            } else if (io.optionTemplate.id == 209) {
                soLanRotCap += io.param;
            }
        }

        if (!isTrangBiGoc(trangBiGoc)) {
            Service.gI().sendThongBaoOK(player, "Trang bị gốc phải từ bậc Lưỡng Long, Jean, Zelot hoặc Thần Linh trở lên");
            return;
        }

        if (levelTrangBi < 4) {
            Service.gI().sendThongBaoOK(player, "Trang bị gốc phải có cấp từ [+4] trở lên");
            return;
        }

        if (!isTrangBiChuyenHoa(trangBiCanChuyenHoa)) {
            Service.gI().sendThongBaoOK(player, "Trang bị cần chuyển hóa phải là Đồ Thần Linh hoặc Đồ Hủy Diệt");
            return;
        }

        boolean daNangCapHoacEpSao = trangBiCanChuyenHoa.itemOptions.stream()
                .anyMatch(io -> io.optionTemplate.id == 72 || io.optionTemplate.id == 102);
        if (daNangCapHoacEpSao) {
            Service.gI().sendThongBaoOK(player, "Trang bị cần chuyển hóa phải chưa nâng cấp [+] và chưa ép Sao Pha Lê");
            return;
        }

        if (!isCheckTrungTypevsGender(trangBiGoc, trangBiCanChuyenHoa)) {
            Service.gI().sendThongBaoOK(player, "Trang bị gốc và Trang bị nhập thể phải cùng loại và cùng hành tinh");
            return;
        }

        boolean gocHasSkh = XoaThuocTinhSKH.hasSkhOption(trangBiGoc);
        boolean gocHasSpl = trangBiGoc.itemOptions.stream()
                .anyMatch(io -> io.optionTemplate.id == 107 || io.optionTemplate.id == 102);

        StringBuilder npcSay = new StringBuilder("|2|Hiện tại " + trangBiCanChuyenHoa.getName() + "\n");
        for (ItemOption io : trangBiCanChuyenHoa.itemOptions) {
            if (io.optionTemplate.id != 72) {
                npcSay.append("|0|").append(io.getOptionString()).append("\n");
            }
        }

        npcSay.append("|2|Sau khi chuyển hóa (+" + levelTrangBi + ")\n");
        for (ItemOption io : trangBiCanChuyenHoa.itemOptions) {
            int id = io.optionTemplate.id;
            if (isBaseStatOption(id)) {
                int scaled = scaleStat(io.param, levelTrangBi, soLanRotCap);
                npcSay.append("|1|").append(io.getOptionString(scaled)).append("\n");
            } else if (id != 72 && !(gocHasSpl && (id == 107 || id == 102 || id == 228))
                    && !(gocHasSkh && XoaThuocTinhSKH.isSkhOption(id))) {
                npcSay.append("|1|").append(io.getOptionString()).append("\n");
            }
        }

        for (ItemOption io : trangBiGoc.itemOptions) {
            int id = io.optionTemplate.id;
            if (!isBaseStatOption(id) && id != 30 && id != 73) {
                npcSay.append("|6|").append(io.getOptionString()).append("\n");
            }
        }

        npcSay.append("|1|Chuyển qua toàn bộ: Cấp [+], Số lỗ & Sao Pha Lê, Thuộc tính SKH\n");
        npcSay.append("|2|Cần ").append(Util.numberToMoney(GOLD_CHUYEN_HOA)).append(" vàng");

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                npcSay.toString(), "Chuyển hóa", "Từ chối");
    }

    public static void thucHienChuyenHoa(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            return;
        }
        Item trangBiGoc = player.combineNew.itemsCombine.get(0);
        Item trangBiCanChuyenHoa = player.combineNew.itemsCombine.get(1);

        if (player.inventory.gold < GOLD_CHUYEN_HOA) {
            Service.gI().sendThongBao(player, "Không đủ 2 tỷ vàng để thực hiện chuyển hóa!");
            return;
        }

        int levelTrangBi = 0;
        int soLanRotCap = 0;

        for (ItemOption io : trangBiGoc.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelTrangBi = io.param;
            } else if (io.optionTemplate.id == 209) {
                soLanRotCap += io.param;
            }
        }

        if (!isTrangBiGoc(trangBiGoc) || levelTrangBi < 4 || !isTrangBiChuyenHoa(trangBiCanChuyenHoa)
                || !isCheckTrungTypevsGender(trangBiGoc, trangBiCanChuyenHoa)) {
            Service.gI().sendThongBao(player, "Trang bị không hợp lệ để chuyển hóa!");
            return;
        }

        boolean gocHasSkh = XoaThuocTinhSKH.hasSkhOption(trangBiGoc);
        boolean gocHasSpl = trangBiGoc.itemOptions.stream()
                .anyMatch(io -> io.optionTemplate.id == 107 || io.optionTemplate.id == 102);

        Item newItem = ItemService.gI().createNewItem(trangBiCanChuyenHoa.template.id);
        newItem.itemOptions.clear();

        // 1. Giữ và nâng chỉ số gốc của trang bị đích
        for (ItemOption io : trangBiCanChuyenHoa.itemOptions) {
            int id = io.optionTemplate.id;
            if (id == 72 || id == 209) {
                continue;
            }
            if (gocHasSpl && (id == 107 || id == 102 || id == 228)) {
                continue;
            }
            if (gocHasSkh && XoaThuocTinhSKH.isSkhOption(id)) {
                continue;
            }
            int value = isBaseStatOption(id) ? scaleStat(io.param, levelTrangBi, soLanRotCap) : io.param;
            newItem.itemOptions.add(new ItemOption(io.optionTemplate.id, value));
        }

        // 2. Chuyển toàn bộ cấp [+], số SPL (107, 102, 228), chỉ số SPL và thuộc tính SKH từ trang bị gốc sang
        for (ItemOption io : trangBiGoc.itemOptions) {
            int id = io.optionTemplate.id;
            if (isBaseStatOption(id)) {
                continue;
            }
            if ((id == 30 || id == 73) && hasOption(newItem, id)) {
                continue;
            }
            newItem.itemOptions.add(new ItemOption(io.optionTemplate.id, io.param));
        }

        player.inventory.gold -= GOLD_CHUYEN_HOA;
        Service.gI().sendMoney(player);
        InventoryService.gI().addItemBag(player, newItem);
        InventoryService.gI().subQuantityItemsBag(player, trangBiGoc, 1);
        InventoryService.gI().subQuantityItemsBag(player, trangBiCanChuyenHoa, 1);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendThongBao(player, "Chuyển hóa trang bị thành công!");
    }

    public static int scaleStat(int baseParam, int levelTrangBi, int soLanRotCap) {
        return (int) Math.round(baseParam * Math.pow(1.1, levelTrangBi) * Math.pow(0.9, soLanRotCap));
    }

    public static boolean hasOption(Item item, int optionId) {
        if (item == null || item.itemOptions == null) {
            return false;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optionId) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCheckTrungTypevsGender(Item item, Item item2) {
        return item.template.type == item2.template.type && item.template.gender == item2.template.gender;
    }

    public static boolean isTrangBiGoc(Item item) {
        return isDoLuongLong(item) || isDoJean(item) || isDoZelot(item) || (item != null && item.isDTL());
    }

    public static boolean isTrangBiChuyenHoa(Item item) {
        return item != null && (item.isDTL() || item.isDHD());
    }

    private static boolean isDoLuongLong(Item item) {
        return item != null && item.isNotNullItem()
                && (item.template.id == 241 || item.template.id == 253 || item.template.id == 265
                || item.template.id == 277 || item.template.id == 281);
    }

    private static boolean isDoJean(Item item) {
        return item != null && item.isNotNullItem()
                && (item.template.id == 237 || item.template.id == 249 || item.template.id == 261 || item.template.id == 273);
    }

    private static boolean isDoZelot(Item item) {
        return item != null && item.isNotNullItem()
                && (item.template.id == 233 || item.template.id == 245 || item.template.id == 257 || item.template.id == 269);
    }

    public static boolean isBaseStatOption(int id) {
        return id == 0 || id == 6 || id == 7 || id == 14 || id == 22 || id == 23 || id == 27 || id == 28 || id == 47;
    }
}
