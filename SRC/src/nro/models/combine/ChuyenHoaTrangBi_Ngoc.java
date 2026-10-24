package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

/**
 * Chuyển hóa Trang bị bằng Ngọc tại Bà Hạt Mít - Đảo Kame:
 * Chuyển cấp [+], toàn bộ Sao Pha Lê (số lỗ SPL, số SPL đã ép, lỗ cường hóa) và thuộc tính Set Kích Hoạt (SKH).
 */
public class ChuyenHoaTrangBi_Ngoc {

    private static final int GEM_CHUYEN_HOA = 1000;

    public static void chuyenHoaTrangBiNgoc(Player player) {
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

        if (!ChuyenHoaTrangBi_Vang.isTrangBiGoc(trangBiGoc)) {
            Service.gI().sendThongBaoOK(player, "Trang bị gốc phải từ bậc Lưỡng Long, Jean, Zelot hoặc Thần Linh trở lên");
            return;
        }

        if (levelTrangBi < 4) {
            Service.gI().sendThongBaoOK(player, "Trang bị gốc phải có cấp từ [+4] trở lên");
            return;
        }

        if (!ChuyenHoaTrangBi_Vang.isTrangBiChuyenHoa(trangBiCanChuyenHoa)) {
            Service.gI().sendThongBaoOK(player, "Trang bị cần chuyển hóa phải là Đồ Thần Linh hoặc Đồ Hủy Diệt");
            return;
        }

        boolean daNangCapHoacEpSao = trangBiCanChuyenHoa.itemOptions.stream()
                .anyMatch(io -> io.optionTemplate.id == 72 || io.optionTemplate.id == 102);
        if (daNangCapHoacEpSao) {
            Service.gI().sendThongBaoOK(player, "Trang bị cần chuyển hóa phải chưa nâng cấp [+] và chưa ép Sao Pha Lê");
            return;
        }

        if (!ChuyenHoaTrangBi_Vang.isCheckTrungTypevsGender(trangBiGoc, trangBiCanChuyenHoa)) {
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
            if (ChuyenHoaTrangBi_Vang.isBaseStatOption(id)) {
                int scaled = ChuyenHoaTrangBi_Vang.scaleStat(io.param, levelTrangBi, soLanRotCap);
                npcSay.append("|1|").append(io.getOptionString(scaled)).append("\n");
            } else if (id != 72 && !(gocHasSpl && (id == 107 || id == 102 || id == 228))
                    && !(gocHasSkh && XoaThuocTinhSKH.isSkhOption(id))) {
                npcSay.append("|1|").append(io.getOptionString()).append("\n");
            }
        }

        for (ItemOption io : trangBiGoc.itemOptions) {
            int id = io.optionTemplate.id;
            if (!ChuyenHoaTrangBi_Vang.isBaseStatOption(id) && id != 30 && id != 73) {
                npcSay.append("|6|").append(io.getOptionString()).append("\n");
            }
        }

        npcSay.append("|1|Chuyển qua toàn bộ: Cấp [+], Số lỗ & Sao Pha Lê, Thuộc tính SKH\n");
        npcSay.append("|2|Cần ").append(GEM_CHUYEN_HOA).append(" Ngọc");

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                npcSay.toString(), "Chuyển hóa", "Từ chối");
    }

    public static void thucHienChuyenHoa(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            return;
        }
        Item trangBiGoc = player.combineNew.itemsCombine.get(0);
        Item trangBiCanChuyenHoa = player.combineNew.itemsCombine.get(1);

        if (player.inventory.gem < GEM_CHUYEN_HOA) {
            Service.gI().sendThongBao(player, "Không đủ " + GEM_CHUYEN_HOA + " Ngọc để thực hiện chuyển hóa!");
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

        if (!ChuyenHoaTrangBi_Vang.isTrangBiGoc(trangBiGoc) || levelTrangBi < 4
                || !ChuyenHoaTrangBi_Vang.isTrangBiChuyenHoa(trangBiCanChuyenHoa)
                || !ChuyenHoaTrangBi_Vang.isCheckTrungTypevsGender(trangBiGoc, trangBiCanChuyenHoa)) {
            Service.gI().sendThongBao(player, "Trang bị không hợp lệ để chuyển hóa!");
            return;
        }

        boolean gocHasSkh = XoaThuocTinhSKH.hasSkhOption(trangBiGoc);
        boolean gocHasSpl = trangBiGoc.itemOptions.stream()
                .anyMatch(io -> io.optionTemplate.id == 107 || io.optionTemplate.id == 102);

        Item newItem = ItemService.gI().createNewItem(trangBiCanChuyenHoa.template.id);
        newItem.itemOptions.clear();

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
            int value = ChuyenHoaTrangBi_Vang.isBaseStatOption(id)
                    ? ChuyenHoaTrangBi_Vang.scaleStat(io.param, levelTrangBi, soLanRotCap)
                    : io.param;
            newItem.itemOptions.add(new ItemOption(io.optionTemplate.id, value));
        }

        for (ItemOption io : trangBiGoc.itemOptions) {
            int id = io.optionTemplate.id;
            if (ChuyenHoaTrangBi_Vang.isBaseStatOption(id)) {
                continue;
            }
            if ((id == 30 || id == 73) && ChuyenHoaTrangBi_Vang.hasOption(newItem, id)) {
                continue;
            }
            newItem.itemOptions.add(new ItemOption(io.optionTemplate.id, io.param));
        }

        player.inventory.gem -= GEM_CHUYEN_HOA;
        Service.gI().sendMoney(player);
        InventoryService.gI().addItemBag(player, newItem);
        InventoryService.gI().subQuantityItemsBag(player, trangBiGoc, 1);
        InventoryService.gI().subQuantityItemsBag(player, trangBiCanChuyenHoa, 1);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendThongBao(player, "Chuyển hóa trang bị thành công!");
    }
}
