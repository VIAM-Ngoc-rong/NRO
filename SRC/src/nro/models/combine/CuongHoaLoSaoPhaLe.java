package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public class CuongHoaLoSaoPhaLe {

    private static final int COST = 500_000_000;

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (player.combineNew.itemsCombine.size() == 3) {
                Item item = null, Hematite = null, DuiDuc = null;

                for (Item i : player.combineNew.itemsCombine) {
                    if (CombineSystem.isTrangBiPhaLeHoa(i)) {
                        item = i;
                    } else if (i.template.id == 1423) { // Hematite
                        Hematite = i;
                    } else if (i.template.id == 1438) { // Dùi Đục
                        DuiDuc = i;
                    }
                }

                if (item != null && Hematite != null && DuiDuc != null && Hematite.quantity >= 1 && DuiDuc.quantity >= 1) {
                    InventoryService.sanitizeStarOptions(item);
                    int star = 0;
                    int cuongHoaSlot = 0;
                    for (ItemOption io : item.itemOptions) {
                        if (io == null || io.optionTemplate == null) {
                            continue;
                        }
                        if (io.optionTemplate.id == 107) {
                            star = io.param;
                        } else if (io.optionTemplate.id == 228) {
                            cuongHoaSlot = io.param;
                        }
                    }

                    if (star < 8) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Trang bị phải đục lên 8 hoặc 9 lỗ sao pha lê mới có thể cường hóa!", "Đóng");
                        return;
                    }
                    if (cuongHoaSlot >= star || cuongHoaSlot >= 9) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Trang bị đã cường hóa tối đa số lỗ sao hiện có!", "Đóng");
                        return;
                    }

                    int targetSlot = cuongHoaSlot < 8 ? 8 : 9;
                    String npcSay = item.template.name + "\n|2|";
                    for (ItemOption io : Hematite.itemOptions) {
                        if (io != null && io.optionTemplate != null && io.optionTemplate.id != 73) {
                            npcSay += io.getOptionString() + "\n";
                        }
                    }
                    npcSay += "Cường hóa\nÔ sao pha lê thứ " + targetSlot + "\n" + item.template.name
                            + "\nTỉ lệ thành công: 40%\n"
                            + "|7| Cần 1 " + Hematite.template.name
                            + "\n|7| Cần 1 " + DuiDuc.template.name
                            + "\nCần " + Util.numberToMoney(COST) + " vàng";

                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay, "Cường Hóa", "Từ chối");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Bạn chưa bỏ đủ vật phẩm !!!", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần bỏ đủ vật phẩm yêu cầu", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
        }
    }

    public static void cuongHoaLoSaoPhaLe(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang.");
            return;
        }

        if (player.inventory.gold < COST) {
            Service.gI().sendThongBao(player, "Con cần thêm vàng để cường hóa...");
            return;
        }

        if (player.combineNew.itemsCombine.isEmpty()) {
            Service.gI().sendThongBao(player, "Không có vật phẩm để cường hóa.");
            return;
        }

        Item item = null;
        Item hematite = null;
        Item duiDuc = null;

        for (Item i : player.combineNew.itemsCombine) {
            if (CombineSystem.isTrangBiPhaLeHoa(i)) {
                item = i;
            } else if (i.template.id == 1423) {
                hematite = i;
            } else if (i.template.id == 1438) {
                duiDuc = i;
            }
        }

        if (item == null || hematite == null || duiDuc == null || hematite.quantity < 1 || duiDuc.quantity < 1) {
            Service.gI().sendThongBao(player, "Thiếu vật phẩm hoặc số lượng không đủ.");
            return;
        }

        InventoryService.sanitizeStarOptions(item);
        int star = 0;
        ItemOption opt228 = null;
        boolean hasOption218 = false;

        for (ItemOption io : item.itemOptions) {
            if (io == null || io.optionTemplate == null) {
                continue;
            }
            if (io.optionTemplate.id == 107) {
                star = io.param;
            } else if (io.optionTemplate.id == 228) {
                opt228 = io;
            } else if (io.optionTemplate.id == 218) {
                hasOption218 = true;
            }
        }

        if (star < 8) {
            Service.gI().sendThongBao(player, "Vui lòng nâng cấp trang bị lên 8 hoặc 9 sao trước khi cường hóa.");
            return;
        }

        if (opt228 != null && (opt228.param >= star || opt228.param >= 9)) {
            Service.gI().sendThongBao(player, "Trang bị đã cường hóa tối đa số lỗ sao hiện có.");
            return;
        }

        boolean success = Util.isTrue(40, 100);
        player.inventory.gold -= COST;

        if (opt228 == null || opt228.param < 8) {
            if (success) {
                if (!hasOption218) {
                    item.itemOptions.add(new ItemOption(218, 0));
                }
                if (opt228 == null) {
                    item.itemOptions.add(new ItemOption(228, 8));
                } else {
                    opt228.param = 8;
                }
                CombineService.gI().sendEffectSuccessCombine(player);
                CombineService.gI().baHatMit.npcChat(player, "Chúc mừng con nhé");
            } else {
                CombineService.gI().sendEffectFailCombine(player);
            }
        } else if (opt228.param == 8 && star >= 9) {
            if (success) {
                if (!hasOption218) {
                    item.itemOptions.add(new ItemOption(218, 0));
                }
                opt228.param = 9;
                CombineService.gI().sendEffectSuccessCombine(player);
                CombineService.gI().baHatMit.npcChat(player, "Chúc mừng con nhé");
            } else {
                CombineService.gI().sendEffectFailCombine(player);
            }
        } else {
            Service.gI().sendThongBao(player, "Không thể cường hóa thêm.");
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, hematite, 1);
        InventoryService.gI().subQuantityItemsBag(player, duiDuc, 1);
        Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
        CombineService.gI().sendEffectCombineDB(player, item.template.iconID);
    }

}
