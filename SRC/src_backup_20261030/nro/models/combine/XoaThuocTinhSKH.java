package nro.models.combine;

import java.util.Iterator;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 * Xóa thuộc tính Set Kích Hoạt (SKH) của trang bị tại Bà Hạt Mít - Đảo Kame:
 * Giúp người chơi tẩy sạch dòng SKH trên trang bị, giữ nguyên cấp [+], chỉ số gốc và Sao Pha Lê.
 */
public class XoaThuocTinhSKH {

    private static final int GOLD_XOA_SKH = 200_000_000;

    private static final int[] SKH_OPTION_IDS = {
        127, 128, 129, 130, 131, 132, 133, 134, 135,
        136, 137, 138, 139, 140, 141, 142, 143, 144,
        210, 211, 212, 213, 214, 215, 216, 217, 218,
        233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248
    };

    public static boolean isSkhOption(int optionId) {
        for (int id : SKH_OPTION_IDS) {
            if (id == optionId) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSkhOption(Item item) {
        if (item == null || !item.isNotNullItem() || item.itemOptions == null) {
            return false;
        }
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && isSkhOption(io.optionTemplate.id)) {
                return true;
            }
        }
        return false;
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hãy chọn đúng 1 trang bị có thuộc tính Kích Hoạt (SKH) cần xóa!", "Đóng");
            return;
        }

        Item item = player.combineNew.itemsCombine.get(0);
        if (item == null || !item.isNotNullItem() || !hasSkhOption(item)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Trang bị này không có thuộc tính Set Kích Hoạt (SKH) để xóa!", "Đóng");
            return;
        }

        StringBuilder npcSay = new StringBuilder();
        npcSay.append("|2|Xóa thuộc tính SKH cho: ").append(item.template.name).append("\n");
        npcSay.append("|7|Các dòng SKH sẽ bị xóa:\n");
        for (ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && isSkhOption(io.optionTemplate.id)) {
                npcSay.append("|7|- ").append(io.getOptionString()).append("\n");
            }
        }
        npcSay.append("|1|Giữ nguyên: Cấp [+], chỉ số gốc & toàn bộ Sao Pha Lê\n");
        npcSay.append("|2|Chi phí: ").append(Util.numberToMoney(GOLD_XOA_SKH)).append(" vàng (Tỉ lệ 100%)\n");

        if (player.inventory.gold < GOLD_XOA_SKH) {
            npcSay.append("|7|Còn thiếu ").append(Util.numberToMoney(GOLD_XOA_SKH - player.inventory.gold)).append(" vàng");
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay.toString(), "Đóng");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    npcSay.toString(), "Xóa SKH\n" + Util.numberToMoney(GOLD_XOA_SKH) + " vàng", "Từ chối");
        }
    }

    public static void thucHienXoaSKH(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendThongBao(player, "Hãy chọn đúng 1 trang bị có thuộc tính SKH!");
            return;
        }

        Item item = player.combineNew.itemsCombine.get(0);
        if (item == null || !item.isNotNullItem() || !hasSkhOption(item)) {
            Service.gI().sendThongBao(player, "Trang bị này không có thuộc tính SKH để xóa!");
            return;
        }

        if (player.inventory.gold < GOLD_XOA_SKH) {
            Service.gI().sendThongBao(player, "Không đủ " + Util.numberToMoney(GOLD_XOA_SKH) + " vàng để thực hiện!");
            return;
        }

        player.inventory.gold -= GOLD_XOA_SKH;

        Iterator<ItemOption> it = item.itemOptions.iterator();
        while (it.hasNext()) {
            ItemOption io = it.next();
            if (io != null && io.optionTemplate != null && isSkhOption(io.optionTemplate.id)) {
                it.remove();
            }
        }

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
        Service.gI().sendThongBao(player, "Đã xóa thành công thuộc tính SKH khỏi " + item.template.name + "!");
    }
}
