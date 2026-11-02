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
 *
 * @author By Mr Blue
 */
public class EpSaoTrangBi {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item trangBi = null;
            Item daPhaLe = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineSystem.isTrangBiPhaLeHoa(item)) {
                    trangBi = item;
                } else if (CombineSystem.isDaPhaLe(item)) {
                    daPhaLe = item;
                }
            }
            int star = 0;
            int starEmpty = 0;
            if (trangBi != null && daPhaLe != null) {
                InventoryService.sanitizeStarOptions(trangBi);
                InventoryService.ensureDefaultItemOptions(daPhaLe);
                for (ItemOption io : trangBi.itemOptions) {
                    if (io == null || io.optionTemplate == null) {
                        continue;
                    }
                    if (io.optionTemplate.id == 102) {
                        star = io.param;
                    } else if (io.optionTemplate.id == 107) {
                        starEmpty = io.param;
                    }
                }

                if (star >= starEmpty) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Cần đục thêm lỗ sao pha lê để ép tiếp", "Đóng");
                    return;
                }

                if (starEmpty <= 9) {
                    int nextSlot = star + 1;
                    boolean isNgocRongTim = daPhaLe.template.id >= 1827 && daPhaLe.template.id <= 1833;
                    if (!isNgocRongTim && nextSlot >= 8 && !CombineService.gI().CheckSlot(trangBi, nextSlot)) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Cần cường hóa lỗ sao pha lê thứ " + nextSlot + " này trước", "Đóng");
                        return;
                    }

                    player.combineNew.goldCombine = CombineSystem.getGemEpSao(star) * 1_000_000;
                    String npcSay = trangBi.template.name + "\n|2|";
                    for (ItemOption io : trangBi.itemOptions) {
                        if (io != null && io.optionTemplate != null && io.optionTemplate.id != 102 && io.optionTemplate.id != 73) {
                            npcSay += io.getOptionString() + "\n";
                        }
                    }

                    int optId = CombineSystem.getOptionDaPhaLe(daPhaLe);
                    int optParam = CombineSystem.getParamDaPhaLe(daPhaLe);
                    if (optId >= 0 && optParam > 0 && ItemService.gI().getItemOptionTemplate(optId) != null) {
                        npcSay += "|7|" + ItemService.gI().getItemOptionTemplate(optId).name
                                .replaceAll("#", optParam + "") + "\n";
                    } else if (daPhaLe.template.type == 30) {
                        for (ItemOption io : daPhaLe.itemOptions) {
                            if (io != null && io.optionTemplate != null && io.optionTemplate.id != 30 && io.optionTemplate.id != 73) {
                                npcSay += "|7|" + io.getOptionString() + "\n";
                            }
                        }
                    }
                    npcSay += "|1|Cần " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\ncần " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng",
                            "Ép hết lỗ\nhiện có");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào, và lỗ sao tối đa là 9", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào", "Đóng");
        }
    }

    public static void epSaoTrangBi(Player player, int... numm) {
        int maxAttempts = (numm != null && numm.length > 0) ? numm[0] : 1;
        if (player.combineNew.itemsCombine.size() == 2) {
            Item trangBi = null;
            Item daPhaLe = null;

            for (Item item : player.combineNew.itemsCombine) {
                if (CombineSystem.isTrangBiPhaLeHoa(item)) {
                    trangBi = item;
                } else if (CombineSystem.isDaPhaLe(item)) {
                    daPhaLe = item;
                }
            }

            if (trangBi == null || daPhaLe == null) {
                return;
            }

            InventoryService.sanitizeStarOptions(trangBi);
            InventoryService.ensureDefaultItemOptions(daPhaLe);

            int epCount = 0;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                if (!daPhaLe.isNotNullItem() || daPhaLe.quantity < 1) {
                    break;
                }
                int star = 0;
                int starEmpty = 0;
                ItemOption optionStar = null;

                for (ItemOption io : trangBi.itemOptions) {
                    if (io == null || io.optionTemplate == null) {
                        continue;
                    }
                    if (io.optionTemplate.id == 102) {
                        star = io.param;
                        optionStar = io;
                    } else if (io.optionTemplate.id == 107) {
                        starEmpty = io.param;
                    }
                }

                if (star >= starEmpty) {
                    if (epCount == 0) {
                        Service.gI().sendThongBao(player, "Không thể ép sao cao hơn lỗ");
                    }
                    break;
                }

                int currentSlot = star + 1;
                boolean isNgocRongTim = daPhaLe.template.id >= 1827 && daPhaLe.template.id <= 1833;
                if (!isNgocRongTim && currentSlot >= 8 && !CombineService.gI().CheckSlot(trangBi, currentSlot)) {
                    if (epCount == 0) {
                        Service.gI().sendThongBao(player, "Cần cường hóa lỗ sao pha lê thứ " + currentSlot + " này trước");
                    }
                    break;
                }

                int optionId = CombineSystem.getOptionDaPhaLe(daPhaLe);
                int param = CombineSystem.getParamDaPhaLe(daPhaLe);
                if (optionId < 0 || param <= 0 || ItemService.gI().getItemOptionTemplate(optionId) == null) {
                    if (epCount == 0) {
                        Service.gI().sendThongBao(player, "Đá pha lê này không có chỉ số hợp lệ");
                    }
                    break;
                }

                int gold = CombineSystem.getGemEpSao(star) * 1_000_000;
                if (player.inventory.gold < gold) {
                    if (epCount == 0) {
                        Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                    }
                    break;
                }

                player.inventory.subGold(gold);

                boolean shouldSplitOption = (currentSlot == 8 || currentSlot == 9);

                if (shouldSplitOption) {
                    boolean hasSeparator = false;
                    for (ItemOption io : trangBi.itemOptions) {
                        if (io != null && io.optionTemplate != null && (io.optionTemplate.id == 218 || io.optionTemplate.id == 228)) {
                            hasSeparator = true;
                            break;
                        }
                    }
                    if (!hasSeparator) {
                        trangBi.itemOptions.add(new ItemOption(218, 0));
                    }
                    trangBi.itemOptions.add(new ItemOption(optionId, param));
                } else {
                    boolean merged = false;
                    int separatorIdx = -1;
                    for (int idx = 0; idx < trangBi.itemOptions.size(); idx++) {
                        ItemOption io = trangBi.itemOptions.get(idx);
                        if (io == null || io.optionTemplate == null) {
                            continue;
                        }
                        if (io.optionTemplate.id == 218 || io.optionTemplate.id == 228) {
                            separatorIdx = idx;
                            break;
                        }
                        if (io.optionTemplate.id == optionId) {
                            io.param += param;
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        if (separatorIdx >= 0) {
                            trangBi.itemOptions.add(separatorIdx, new ItemOption(optionId, param));
                        } else {
                            trangBi.itemOptions.add(new ItemOption(optionId, param));
                        }
                    }
                }

                if (optionStar != null) {
                    optionStar.param += 1;
                } else {
                    optionStar = new ItemOption(102, 1);
                    trangBi.itemOptions.add(optionStar);
                }

                InventoryService.gI().subQuantityItemsBag(player, daPhaLe, 1);
                epCount++;
            }

            if (epCount > 0) {
                CombineService.gI().sendEffectSuccessCombine(player);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
                if (epCount > 1) {
                    Service.gI().sendThongBao(player, "Đã ép nhanh thành công " + epCount + " sao pha lê!");
                }
            }
        }
    }

}
