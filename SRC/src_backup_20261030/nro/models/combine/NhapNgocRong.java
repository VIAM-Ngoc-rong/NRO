package nro.models.combine;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

/**
 *
 * @author By Mr Blue
 */

public class NhapNgocRong {

    public static boolean isNgocRongCanCombine(Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return false;
        }
        int id = item.template.id;
        return (id > 14 && id <= 20) || (id > 1827 && id <= 1833);
    }

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (player.combineNew.itemsCombine.size() == 1) {
                Item item = player.combineNew.itemsCombine.get(0);
                if (isNgocRongCanCombine(item) && item.quantity >= 7) {
                    int maxCombine = item.quantity / 7;
                    String nextName = ItemService.gI().getTemplate((short) (item.template.id - 1)).name;
                    String npcSay = "|2|Con có muốn biến 7 " + item.template.name + " thành\n"
                            + "1 viên " + nextName + "\n"
                            + "|7|Cần 7 " + item.template.name
                            + "\n|1|Ghép nhanh: " + (maxCombine * 7) + " viên -> " + maxCombine + " viên " + nextName;
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Làm phép\n(1 viên)",
                            "Ghép nhanh\n(" + maxCombine + " viên)",
                            "Từ chối");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần 7 viên Ngọc Rồng (hoặc Ngọc Rồng Tím) 2 sao trở lên", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần 7 viên Ngọc Rồng (hoặc Ngọc Rồng Tím) 2 sao trở lên", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
        }
    }

    public static void nhapNgocRong(Player player) {
        nhapNgocRong(player, false);
    }

    public static void nhapNgocRong(Player player, boolean quick) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (!player.combineNew.itemsCombine.isEmpty()) {
                Item item = player.combineNew.itemsCombine.get(0);
                if (isNgocRongCanCombine(item) && item.quantity >= 7) {
                    int countCombine = quick ? (item.quantity / 7) : 1;
                    if (countCombine <= 0) {
                        return;
                    }
                    int subQty = countCombine * 7;
                    CombineService.gI().sendEffectCombineDB(player, item.template.iconID);
                    Item nr = ItemService.gI().createNewItem((short) (item.template.id - 1), countCombine);
                    InventoryService.gI().addItemBag(player, nr);
                    InventoryService.gI().subQuantityItemsBag(player, item, subQty);
                    InventoryService.gI().sendItemBags(player);
                    CombineService.gI().reOpenItemCombine(player);
                    if (quick && countCombine > 1) {
                        Service.gI().sendThongBao(player, "Đã ghép nhanh thành công " + countCombine + " viên " + nr.template.name + "!");
                    }
                }
            }
        }
    }

}
