package nro.models.services_func;

import nro.models.boss.Boss;
import nro.models.boss.Boss_mini.SoiHecQuyn;
import nro.models.services.shenron.SummonDragon;
import nro.models.combine.CombineService;
import nro.models.boss.BossID;
import nro.models.consts.ConstItem;
import nro.models.radar.Card;
import nro.models.services.RadarService;
import nro.models.radar.RadarCard;
import nro.models.consts.ConstMap;
import nro.models.item.Item;
import nro.models.item.ItemTime;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.services.shenron.Shenron_Service;
import nro.models.item.Item.ItemOption;
import static java.awt.SystemColor.text;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import nro.models.consts.ConstTaskBadges;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Inventory;
import nro.models.map.service.NpcService;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.network.Message;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.SkillUtil;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.network.MySession;
import nro.models.npc.MabuEgg;
import nro.models.services.ItemService;
import nro.models.services.ItemTimeService;
import nro.models.services.PetService;
import nro.models.services.RewardService;
import nro.models.services.PlayerService;
import nro.models.services.TaskService;
import nro.models.services.InventoryService;
import nro.models.map.service.MapService;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.map.service.ItemMapService;
import nro.models.npc.DuaHauEgg;
import nro.models.server.Manager;
import static nro.models.server.Manager.isTopSukienChanged;
import nro.models.services.ChatGlobalService;
import nro.models.services.EffectSkillService;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Logger;

/**
 *
 * @author By Mr Blue
 *
 */
public class UseItem {

    private static final int ITEM_BOX_TO_BODY_OR_BAG = 0;
    private static final int ITEM_BAG_TO_BOX = 1;
    private static final int ITEM_BODY_TO_BOX = 3;
    private static final int ITEM_BAG_TO_BODY = 4;
    private static final int ITEM_BODY_TO_BAG = 5;
    private static final int ITEM_BAG_TO_PET_BODY = 6;
    private static final int ITEM_BODY_PET_TO_BAG = 7;
    private static final int ITEM_BAG_TO_RUONG = 8;

    private static final byte DO_USE_ITEM = 0;
    private static final byte DO_THROW_ITEM = 1;
    private static final byte ACCEPT_THROW_ITEM = 2;
    private static final byte ACCEPT_USE_ITEM = 3;

    private static UseItem instance;
    private static final Random rand = new Random();

    private UseItem() {

    }

    public static UseItem gI() {
        if (instance == null) {
            instance = new UseItem();
        }
        return instance;
    }

    private boolean handleVirtualBagSlotClick(Player player, int index) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null || index < 0) {
            return false;
        }
        int bagIndex = player.inventory.resolveBagIndex(index);
        if (bagIndex <= -101 && bagIndex >= -105) {
            int targetTab = -101 - bagIndex;
            player.inventory.switchBagTab(targetTab);
            InventoryService.gI().sendItemBags(player);
            InventoryService.gI().sendItemBody(player);
            if (player.combineNew != null && !player.combineNew.itemsCombine.isEmpty()) {
                nro.models.combine.CombineService.gI().reOpenItemCombine(player);
            }
            int sSlot = targetTab * nro.models.player.Inventory.BAG_PAGE_SIZE + 1;
            int eSlot = (targetTab + 1) * nro.models.player.Inventory.BAG_PAGE_SIZE;
            String subInfo = player.inventory.currentBagSubPage > 0 ? " [Nửa sau: 101-200]" : "";
            Service.gI().sendThongBao(player, "Đã chuyển đầu túi sang Trang " + (targetTab + 1) + "/5 (Ô "
                    + sSlot + "-" + eSlot + subInfo + ", 200 ô/Trang - Dùng chung 1 bể)!");
            return true;
        }
        if (bagIndex == -106) {
            nro.models.npc.NpcFactory.openMenuBagTab(player);
            return true;
        }
        if (!player.inventory.displayedBagIndices.isEmpty()) {
            if (index >= player.inventory.displayedBagIndices.size()) {
                nro.models.npc.NpcFactory.openMenuBagTab(player);
                return true;
            }
            if (bagIndex < 0 || bagIndex >= player.inventory.itemsBag.size()) {
                nro.models.npc.NpcFactory.openMenuBagTab(player);
                return true;
            }
            Item item = player.inventory.itemsBag.get(bagIndex);
            if (item == null || !item.isNotNullItem() || nro.models.data.ItemData.isQuickTabButtonTemplateId(item.template.id)) {
                nro.models.npc.NpcFactory.openMenuBagTab(player);
                return true;
            }
            return false;
        }
        if (index >= player.inventory.itemsBag.size()) {
            nro.models.npc.NpcFactory.openMenuBagTab(player);
            return true;
        }
        Item item = player.inventory.itemsBag.get(index);
        if (item == null || !item.isNotNullItem() || nro.models.data.ItemData.isQuickTabButtonTemplateId(item.template.id)) {
            nro.models.npc.NpcFactory.openMenuBagTab(player);
            return true;
        }
        return false;
    }

    private void cycleBagQuickTab(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }
        int nextTab = (player.inventory.currentBagTab + 1) % nro.models.player.Inventory.NUM_QUICK_TABS;
        player.inventory.switchBagTab(nextTab);
        InventoryService.gI().sendItemBags(player);
        InventoryService.gI().sendItemBody(player);
        if (player.combineNew != null && !player.combineNew.itemsCombine.isEmpty()) {
            nro.models.combine.CombineService.gI().reOpenItemCombine(player);
        }
        Service.gI().sendThongBao(player, "Đã chuyển đầu túi sang Trang " + (nextTab + 1) + "/"
                + nro.models.player.Inventory.NUM_QUICK_TABS + " (Dùng chung 1 bể)!");
    }

    public void getItem(MySession session, Message msg) {
        Player player = session.player;
        if (player == null) {
            return;
        }
        TransactionService.gI().cancelTrade(player);
        try {
            int type = msg.reader().readByte();
            int index = msg.reader().readByte();
            if (index == -1) {
                return;
            }
            if (type == ITEM_BODY_TO_BAG || type == ITEM_BODY_TO_BOX) {
                if (index < 0 || index >= player.inventory.itemsBody.size()
                        || !player.inventory.itemsBody.get(index).isNotNullItem()) {
                    cycleBagQuickTab(player);
                    return;
                }
            }
            if ((type == ITEM_BAG_TO_BOX || type == ITEM_BAG_TO_BODY || type == ITEM_BAG_TO_PET_BODY)
                    && handleVirtualBagSlotClick(player, index)) {
                return;
            }
            int bagIndex = player.inventory.resolveBagIndex(index);
            switch (type) {
                case ITEM_BOX_TO_BODY_OR_BAG:
                    InventoryService.gI().itemBoxToBodyOrBag(player, index);
                    TaskService.gI().checkDoneTaskGetItemBox(player);
                    break;
                case ITEM_BAG_TO_BOX:
                    InventoryService.gI().itemBagToBox(player, bagIndex);
                    break;
                case ITEM_BODY_TO_BOX:
                    InventoryService.gI().itemBodyToBox(player, index);
                    break;
                case ITEM_BAG_TO_BODY:
                    InventoryService.gI().itemBagToBody(player, bagIndex);
                    break;
                case ITEM_BODY_TO_BAG:
                    InventoryService.gI().itemBodyToBag(player, index);
                    break;
                case ITEM_BAG_TO_PET_BODY:
                    InventoryService.gI().itemBagToPetBody(player, bagIndex);
                    break;
                case ITEM_BODY_PET_TO_BAG:
                    InventoryService.gI().itemPetBodyToBag(player, index);
                    break;
            }
            if (player.setClothes != null) {
                player.setClothes.setup();
            }
            if (player.pet != null) {
                player.pet.setClothes.setup();
            }
            player.setClanMember();
            Service.gI().sendFlagBag(player);
            Service.gI().point(player);
            Service.gI().sendSpeedPlayer(player, -1);
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);

        }
    }

    public Item finditem(Player player, int iditem) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == iditem) {
                return item;
            }
        }
        return null;
    }

    public void doItem(Player player, Message _msg) {
        TransactionService.gI().cancelTrade(player);
        Message msg = null;
        byte type;
        try {
            type = _msg.reader().readByte();
            int where = _msg.reader().readByte();
            int index = _msg.reader().readByte();
            switch (type) {
                case DO_USE_ITEM -> {
                    if (player != null && player.inventory != null) {
                        if (index != -1) {
                            if (index < 0) {
                                return;
                            }
                            if (handleVirtualBagSlotClick(player, index)) {
                                return;
                            }
                            int bagIndex = player.inventory.resolveBagIndex(index);
                            if (bagIndex < 0 || bagIndex >= player.inventory.itemsBag.size()) {
                                return;
                            }
                            Item item = player.inventory.itemsBag.get(bagIndex);
                            if (item.isNotNullItem()) {
                                if (nro.models.data.ItemData.isQuickTabButtonTemplateId(item.template.id)) {
                                    nro.models.npc.NpcFactory.openMenuBagTab(player);
                                    return;
                                }
                                if (item.template.type == 7) {
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc chắn học " + item.template.name + "?");
                                    player.sendMessage(msg);
                                } else if (item.template.id == 570) {
                                    if (!Util.isAfterMidnight(player.lastTimeRewardWoodChest)) {
                                        Service.gI().sendThongBao(player, "Hãy chờ đến ngày mai");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn mở\n" + item.template.name + " ?");
                                    player.sendMessage(msg);
                                } else if (item.template.type == 22) {
                                    if (player.zone.items.stream().filter(it -> it != null && it.itemTemplate.type == 22).count() > 2) {
                                        Service.gI().sendThongBaoOK(player, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn dùng\n" + item.template.name + " ?");
                                    player.sendMessage(msg);
                                } else {
                                    UseItem.gI().useItem(player, item, bagIndex);
                                }
                            }
                        } else {
                            int iditem = _msg.reader().readShort();
                            int baseBtn = nro.models.server.Manager.ITEM_TEMPLATES.size();
                            if ((iditem >= 24990 && iditem <= 24994) || (baseBtn > 0 && iditem >= baseBtn && iditem < baseBtn + 5)) {
                                int targetTab = (iditem >= 24990 && iditem <= 24994) ? (iditem - 24990) : (iditem - baseBtn);
                                player.inventory.switchBagTab(targetTab);
                                InventoryService.gI().sendItemBags(player);
                                InventoryService.gI().sendItemBody(player);
                                Service.gI().sendThongBao(player, "Đã chuyển đầu túi sang Trang " + (targetTab + 1) + "/5 (Dùng chung 1 bể)!");
                                return;
                            }
                            if (nro.models.data.ItemData.isQuickTabButtonTemplateId(iditem)) {
                                nro.models.npc.NpcFactory.openMenuBagTab(player);
                                return;
                            }
                            Item item = finditem(player, iditem);
                            UseItem.gI().useItem(player, item, index);
                        }
                    }
                }
                case DO_THROW_ITEM -> {
                    if (!(player.zone.map.mapId == 21 || player.zone.map.mapId == 22 || player.zone.map.mapId == 23)) {
                        Item item = null;
                        if (index < 0) {
                            return;
                        }
                        if (where == 0) {
                            if (index >= player.inventory.itemsBody.size()
                                    || !player.inventory.itemsBody.get(index).isNotNullItem()) {
                                cycleBagQuickTab(player);
                                return;
                            }
                            item = player.inventory.itemsBody.get(index);
                        } else {
                            if (handleVirtualBagSlotClick(player, index)) {
                                return;
                            }
                            int bagIndex = player.inventory.resolveBagIndex(index);
                            if (bagIndex < 0 || bagIndex >= player.inventory.itemsBag.size()) {
                                return;
                            }
                            item = player.inventory.itemsBag.get(bagIndex);
                        }

                        if (item.isNotNullItem() && item.template.id == 570) {
                            Service.gI().sendThongBao(player, "Không thể bỏ vật phẩm này.");
                            return;
                        }
                        if (!item.isNotNullItem()) {
                            return;
                        }
                        msg = new Message(-43);
                        msg.writer().writeByte(type);
                        msg.writer().writeByte(where);
                        msg.writer().writeByte(index);
                        msg.writer().writeUTF("Bạn chắc chắn muốn vứt " + item.template.name + "?");
                        player.sendMessage(msg);
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }
                }
                case ACCEPT_THROW_ITEM -> {
                    if (where != 0 && handleVirtualBagSlotClick(player, index)) {
                        return;
                    }
                    int throwIndex = (where == 0) ? index : player.inventory.resolveBagIndex(index);
                    if (throwIndex < 0) {
                        return;
                    }
                    InventoryService.gI().throwItem(player, where, throwIndex);
                    Service.gI().point(player);
                    InventoryService.gI().sendItemBags(player);
                }
                case ACCEPT_USE_ITEM -> {
                    if (handleVirtualBagSlotClick(player, index)) {
                        return;
                    }
                    int bagIndex = player.inventory.resolveBagIndex(index);
                    if (bagIndex < 0 || bagIndex >= player.inventory.itemsBag.size()) {
                        return;
                    }
                    UseItem.gI().useItem(player, player.inventory.itemsBag.get(bagIndex), bagIndex);
                }
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void useItem(Player pl, Item item, int indexBag) {
        if (item != null && item.isNotNullItem()) {

            if (item.template.id == 570) {
                if (!Util.isAfterMidnight(pl.lastTimeRewardWoodChest)) {
                    Service.gI().sendThongBao(pl, "Hãy chờ đến ngày mai");
                } else {
                    openRuongGo(pl);
                }
                return;
            }
            if (item.template.strRequire <= pl.nPoint.power) {
                switch (item.template.type) {
                    case 33: //card
                        UseCard(pl, item);
                        break;
                    case 7: //sách học, nâng skill
                        learnSkill(pl, item);
                        break;
                    case 6: //đậu thần
                        this.eatPea(pl);
                        break;
                    case 12: //ngọc rồng các loại
                        controllerCallRongThan(pl, item);
                        break;
                    case 23: //thú cưỡi mới
                    case 24: //thú cưỡi cũ
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        break;
                    case 11: //item bag
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendFlagBag(pl);
                        break;
                    case 25: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                    }
                    default:
                        switch (item.template.id) {
                            case 992: // Nhan thoi khong
                                pl.type = 2;
                                pl.maxTime = 5;
                                Service.gI().Transport(pl);
                                break;
                            case 361:
                                pl.idGo = (short) Util.nextInt(0, 6);
                                NgocRongNamecService.gI().menuCheckTeleNamekBall(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                break;
                            case 892:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 882, 883, 884);
                                Service.gI().point(pl);
                                break;
                            case 893:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 885, 886, 887);
                                Service.gI().point(pl);
                                break;
                            case 908:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 891, 892, 893);
                                Service.gI().point(pl);
                                break;
                            case 909:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 894, 895, 896);
                                Service.gI().point(pl);
                                break;
                            case 910:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 897, 898, 899);
                                Service.gI().point(pl);
                                break;
                            case 916:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 925, 926, 927);
                                Service.gI().point(pl);
                                break;
                            case 917:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 928, 929, 930);
                                Service.gI().point(pl);
                                break;
                            case 918:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 931, 932, 933);
                                Service.gI().point(pl);
                                break;
                            case 919:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 934, 935, 936);
                                Service.gI().point(pl);
                                break;
                            case 936:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 718, 719, 720);
                                Service.gI().point(pl);
                                break;
                            case 942:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 966, 967, 968);
                                Service.gI().point(pl);
                                break;
                            case 943:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 969, 970, 971);
                                Service.gI().point(pl);
                                break;
                            case 944:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 972, 973, 974);
                                Service.gI().point(pl);
                                break;
                            case 967:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1050, 1051, 1052);
                                Service.gI().point(pl);
                                break;
                            case 1008:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1074, 1075, 1076);
                                Service.gI().point(pl);
                                break;
                            case 1039:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1089, 1090, 1091);
                                Service.gI().point(pl);
                                break;
                            case 1040:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1092, 1093, 1094);
                                Service.gI().point(pl);
                                break;
                            case 1046:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, -1, -1, -1);
                                Service.gI().point(pl);
                                break;
                            case 1107:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1155, 1156, 1157);
                                Service.gI().point(pl);
                                break;
                            case 1114:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1158, 1159, 1160);
                                Service.gI().point(pl);
                                break;
                            case 1188:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1183, 1184, 1185);
                                Service.gI().point(pl);
                                break;
                            case 1202:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1201, 1202, 1203);
                                Service.gI().point(pl);
                                break;
                            case 1203:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1201, 1202, 1203);
                                Service.gI().point(pl);
                                break;
                            case 1207:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1077, 1078, 1079);
                                Service.gI().point(pl);
                                break;
                            case 1224:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1227, 1228, 1229);
                                Service.gI().point(pl);
                                break;
                            case 1225:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1233, 1234, 1235);
                                Service.gI().point(pl);
                                break;
                            case 1226:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1230, 1231, 1232);
                                Service.gI().point(pl);
                                break;
                            case 1243:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1245, 1246, 1247);
                                Service.gI().point(pl);
                                break;
                            case 1244:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1248, 1249, 1250);
                                Service.gI().point(pl);
                                break;
                            case 1256:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1267, 1268, 1269);
                                Service.gI().point(pl);
                                break;
                            case 1318:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1299, 1300, 1301);
                                Service.gI().point(pl);
                                break;
                            case 1347:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1302, 1303, 1304);
                                Service.gI().point(pl);
                                break;
                            case 1414:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1341, 1342, 1343);
                                Service.gI().point(pl);
                                break;
                            case 1435:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1347, 1348, 1349);
                                Service.gI().point(pl);
                                break;
                            case 1452:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1365, 1366, 1367);
                                Service.gI().point(pl);
                                break;
                            case 1458:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1368, 1369, 1370);
                                Service.gI().point(pl);
                                break;
                            case 1482:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1398, 1399, 1400);
                                Service.gI().point(pl);
                                break;
                            case 1497:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1401, 1402, 1403);
                                Service.gI().point(pl);
                                break;
                            case 1550:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1428, 1429, 1430);
                                Service.gI().point(pl);
                                break;
                            case 1551:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1425, 1426, 1427);
                                Service.gI().point(pl);
                                break;
                            case 1564:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1437, 1438, 1439);
                                Service.gI().point(pl);
                                break;
                            case 1568:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1443, 1444, 1445);
                                Service.gI().point(pl);
                                break;
                            case 1573:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1446, 1447, 1448);
                                Service.gI().point(pl);
                                break;
                            case 1596:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1473, 1474, 1475);
                                Service.gI().point(pl);
                                break;
                            case 1597:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1473, 1474, 1475);
                                Service.gI().point(pl);
                                break;
                            case 1611:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1488, 1494, 1495);
                                Service.gI().point(pl);
                                break;
                            case 1620:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1496, 1497, 1498);
                                Service.gI().point(pl);
                                break;
                            case 1621:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1496, 1497, 1498);
                                Service.gI().point(pl);
                                break;
                            case 1622:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1488, 1489, 1490);
                                Service.gI().point(pl);
                                break;
                            case 1629:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1505, 1506, 1507);
                                Service.gI().point(pl);
                                break;
                            case 1630:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1508, 1509, 1510);
                                Service.gI().point(pl);
                                break;
                            case 1631:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1513, 1516, 1517);
                                Service.gI().point(pl);
                                break;
                            case 1633:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1523, 1524, 1525);
                                Service.gI().point(pl);
                                break;
                            case 1654:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1526, 1529, 1530);
                                Service.gI().point(pl);
                                break;
                            case 1668:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1550, 1551, 1552);
                                Service.gI().point(pl);
                                break;
                            case 1682:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1558, 1559, 1560);
                                Service.gI().point(pl);
                                break;
                            case 1683:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1561, 1562, 1563);
                                Service.gI().point(pl);
                                break;
                            case 1686:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1572, 1573, 1574);
                                Service.gI().point(pl);
                                break;
                            case 1750:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1464, 1465, 1466);
                                Service.gI().point(pl);
                                break;
                            case 1765:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1662, 1663, 1664);
                                Service.gI().point(pl);
                                break;
                            case 1789:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1724, 1725, 1726);
                                Service.gI().point(pl);
                                break;
                            case 1727:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1616, 1617, 1618);
                                Service.gI().point(pl);
                                break;
                            case 1729:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1621, 1622, 1623);
                                Service.gI().point(pl);
                                break;
                            case 1766:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1665, 1666, 1667);
                                Service.gI().point(pl);
                                break;
                            case 1767:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1668, 1669, 1670);
                                Service.gI().point(pl);
                                break;
                            case 1768:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1671, 1672, 1673);
                                Service.gI().point(pl);
                                break;
                            case 1769:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1674, 1675, 1676);
                                Service.gI().point(pl);
                                break;
                            case 1770:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1677, 1678, 1679);
                                Service.gI().point(pl);
                                break;
                            case 1771:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1680, 1681, 1682);
                                Service.gI().point(pl);
                                break;
                            case 1655: // capule kich hoat
                                UseItem.gI().CapsuleKichHoat(pl, item);
                                break;
                            case 1787: // capule kich hoat
                                UseItem.gI().MapRiengTu(pl, item);
                                break;
                            case 718: // vé tặng ngọc
                                UseItem.gI().VeTangNgoc(pl, item);
                                break;
                            case 1575: // pháo bômg
                                UseItem.gI().PhaoBong(pl, item);
                                break;
                            case 1576: // pháo bômg
                                UseItem.gI().PhaoBongVip(pl, item);
                                break;
                            case 1609: // kem trai cay
                                UseItem.gI().KemTraiCay(pl, item);
                                break;
                            case 1608:
                                UseItem.gI().QuaThieuNhi(pl, item);
                                break;
                            case 1822: // rada ngọc rồng
                                UseItem.gI().RadaNgocRong(pl, item.template.id);
                                break;
                            case 1823: // rada ngọc rồng vip
                                UseItem.gI().RadaNgocRongVip(pl, item.template.id);
                                break;
                            case 1569: // kho báu hải tặc
                                UseItem.gI().KhoBauHaiTac(pl, item.template.id);
                                break;
                            case 1798:
                            case 1801:
                            case 1799:
                            case 1800:
                            case 1802:
                                UseItem.gI().ThucAnChoThan(pl, item);
                                break;
                            case 1652: // Sử dụng item Loa Thế Giới
                               try {
                                UseItem.gI().LoaTheGioi(pl, item);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                            case 1776:
                                UseItem.gI().OpenHopQuaThuong(pl, item.template.id);
                                break;
                            case 1777:
                                UseItem.gI().OpenHopQuaVip(pl, item.template.id);
                                break;
                            case 1592:
                                UseItem.gI().OpenHopQuaGokuDay(pl, item.template.id);
                                break;
                            case 1594:
                            case 1591:
                                UseItem.gI().OpenHopQuaGokuDayVangNgoc(pl, item.template.id);
                                break;
                            case 1840:
                                UseItem.gI().OpenHopQuaGokuDayVip(pl, item.template.id);
                                break;
                            case 1757:
                                UseItem.gI().OpenHopQuaCadic(pl, item.template.id);
                                break;
                            case 1821:
                                UseItem.gI().OpenTrungRongNhi(pl, item.template.id);
                                break;
                            case 1635: // co bon la
                                UseItem.gI().useItemTime(pl, item);
                                break;
                            case 1505:
                                Item gm = null;
                                if (item.isNotNullItem()) {
                                    if (item.isGiayMau()) {
                                        gm = item;
                                    }
                                }
                                NpcService.gI().createMenuConMeo(pl, ConstNpc.event3, -1,
                                        "|1|Gói Hộp đựng quà\n"
                                        + "|2|Giấy màu " + gm.quantity + "/99\n"
                                        + "|2|Giá vàng 2.000.000",
                                        "Đồng ý", "Đóng");

                                break;
                            case 1506:
                            case 1507:
                            case 1508:
                            case 1509:
                                NpcService.gI().createMenuConMeo(pl, ConstNpc.event3_1, -1,
                                        "Bạn muốn gói loại nào",
                                        "Gói Hộp quà nhẹ nhàng", "Gói Hộp quà chỉnh chu", "Từ chối");
                                break;
                            case 211: //nho tím
                            case 212: //nho xanh
                                eatGrapes(pl, item);
                                break;
                            case 460:
                                XuongCho(pl, item);
                                break;
                            case 962:
                                C5(pl, item);
                                break;
                            case 963:
                                C7(pl, item);
                                break;
                            case 342:
                            case 343:
                            case 344:
                            case 345:
                                if (pl.zone.items.stream().filter(it -> it != null && it.itemTemplate.type == 22).count() < 3) {
                                    Service.gI().dropSatellite(pl, item, pl.zone, pl.location.x, pl.location.y);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                } else {
                                    Service.gI().sendThongBaoOK(pl, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                }
                                break;
                            case 380: //cskb
                                openCSKB(pl, item);
                                break;
                            case 457: //thỏi vàng
                                useThoiVang(pl, item);
                                break;
                            case 1614:
                            case 1615:
                            case 1616:
                            case 381: //cuồng nộ
                            case 382: //bổ huyết
                            case 383: //bổ khí
                            case 384: //giáp xên
                            case 385: //ẩn danh
                            case 379: //máy dò capsule
                            case 638: //commeson                           
                            case 579:
                            case 1045: //đuôi khỉ
                            case 663: //bánh pudding
                            case 664: //xúc xíc
                            case 665: //kem dâu
                            case 666: //mì ly
                            case 667: //sushi
                            case 764: //Khau Trang
                            case 1150:
                            case 1151:
                            case 1152:
                            case 1153:
                            case 1154:
                            case 1233:
                            case 1532:
                            case 1628:

                                useItemTime(pl, item);
                                break;
                            case 880:
                            case 881:
                            case 882:
                                useItemTime(pl, item);
                                break;
                            case 521: //tdlt
                                useTDLT(pl, item);
                                break;
                            case 454: //bông tai
                                UseItem.gI().usePorata(pl);
                                break;
                            case 921: //bông tai
                                UseItem.gI().usePorata2(pl);
                                break;
                                case 1819: //bông tai
                                UseItem.gI().usePorata3(pl);
                                break;
                            case 193: //gói 10 viên capsule
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                openCapsuleUI(pl);
                                break;
                            case 194: //capsule đặc biệt
                                openCapsuleUI(pl);
                                break;
                            case 401: //đổi đệ tử
                                changePet(pl, item);
                                break;
                            case 402: //sách nâng chiêu 1 đệ tử
                            case 403: //sách nâng chiêu 2 đệ tử
                            case 404: //sách nâng chiêu 3 đệ tử
                            case 759: //sách nâng chiêu 4 đệ tử
                                upSkillPet(pl, item);
                                break;
                            case 726:
                                UseItem.gI().ItemManhGiay(pl, item);
                                break;
                            case 727:
                            case 728:
                                UseItem.gI().ItemSieuThanThuy(pl, item);
                                break;
                            case 1775:
                                UseItem.gI().OpenHopThanlinh(pl, item.template.id);
                                break;
                            case 648:
                                UseItem.gI().NoelItemBox(pl, item);
                                break;
                            case 649:
                                UseItem.gI().useTatVoGiangSinh(pl, item);
                                break;
                            case 533:
                                UseItem.gI().useKeoGiangSinh(pl, item);
                                break;
                            case 585:
                            case 901:
                            case 902:
                                UseItem.gI().useBiNgoHalloween(pl, item);
                                break;
                            case 717:
                                UseItem.gI().useBaoLiXi(pl, item);
                                break;
                            case 589:
                            case 610:
                                UseItem.gI().useBongHoa(pl, item);
                                break;
                            case 1542:
                            case 1556:
                                UseItem.gI().useBanhChungBanhDay(pl, item);
                                break;
                            case 1544:
                            case 1545:
                            case 1546:
                            case 1547:
                            case 1548:
                            case 1549:
                                UseItem.gI().useNguyenLieuNauBanh(pl, item);
                                break;
                            case 1612:
                            case 1613:
                                UseItem.gI().useNguyenLieuNuocMia(pl);
                                break;
                            case 569:
                            case 1558:
                                UseItem.gI().useDuaHauTem(pl);
                                break;
                            case 1220:
                            case 1221:
                            case 1222:
                                UseItem.gI().useSinhLeHungVuong(pl);
                                break;
                            case 1171:
                                UseItem.gI().ChuLunBox(pl, item);
                                break;
                            case 1560:
                                if (InventoryService.gI().findItem(pl.inventory.itemsBag, 1561) != null) {
                                    UseItem.gI().RuongNgocRong(pl, item);
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn không có chía khoá vàng!");
                                }
                                break;

                            case 1170:
                                UseItem.gI().BlackGokuItemBoxEventNoel(pl, item);
                                break;
                            case 736:
                                ItemService.gI().OpenItem736(pl, item);
                                break;
                            case 987:
                                Service.gI().sendThongBao(pl, "Bảo vệ trang bị không bị rớt cấp"); //đá bảo vệ
                                break;
                            case 568: //quả trứng mabư
                                if (pl.pet == null) {
                                    Service.gI().sendThongBao(pl, "Bắt buộc phải có Đệ tử trước mới sử dụng được Quả Trứng Mabư!");
                                    break;
                                }
                                if (pl.mabuEgg == null) {
                                    MabuEgg.createMabuEgg(pl);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    Service.gI().sendThongBao(pl, "Đã đặt Quả Trứng Mabư tại nhà! Hãy về nhà để đợi 1 ngày hoặc mở ngay bằng 1 tỷ vàng.");
                                    if (pl.zone.map.mapId == (21 + pl.gender)) {
                                        if (pl.mabuEgg != null) {
                                            pl.mabuEgg.sendMabuEgg();
                                        }
                                    }
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn đang có Quả Trứng ở nhà nên không thể sử dụng thêm!");
                                }
                                break;
                            case 1826: //quả trứng broly
                                if (pl.pet == null || pl.pet.typePet != 1) {
                                    Service.gI().sendThongBao(pl, "Bắt buộc phải có Đệ tử Mabư trước mới sử dụng được Quả Trứng Broly!");
                                    break;
                                }
                                if (pl.mabuEgg == null) {
                                    MabuEgg.createBrolyEgg(pl);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    Service.gI().sendThongBao(pl, "Đã đặt Quả Trứng Broly tại nhà! Hãy về nhà để đợi 1 ngày hoặc mở ngay bằng 1 tỷ vàng.");
                                    if (pl.zone.map.mapId == (21 + pl.gender)) {
                                        if (pl.mabuEgg != null) {
                                            pl.mabuEgg.sendMabuEgg();
                                        }
                                    }
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn đang có Quả Trứng ở nhà nên không thể sử dụng thêm!");
                                }
                                break;
                            case 1999:
                            case 2006:
                                Input.gI().createFormChangeNameByItem(pl);
                                break;
                            case 1758: {
                                Player player = pl;
                                if (player.pet != null) {
                                    if (player.pet.playerSkill.skills.get(1).skillId != -1) {
                                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                        player.pet.openSkill2();
                                        Service.gI().showInfoPet(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 2 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                            }
                            break;
                            case 1759: {
                                Player player = pl;
                                if (player.pet != null) {
                                    if (player.pet.playerSkill.skills.get(2).skillId != -1) {
                                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                        player.pet.openSkill3();
                                        Service.gI().showInfoPet(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 3 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                            }
                            break;
                            case 1795: //đổi đệ tử
                                changePetRamdom(pl, item);
                                break;
                            case 1760: {
                                Player player = pl;
                                if (player.pet != null) {
                                    if (player.pet.playerSkill.skills.get(3).skillId != -1) {
                                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                        player.pet.openSkill4();
                                        Service.gI().showInfoPet(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 4 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                            }
                            break;
                        }
                        break;
                }
                TaskService.gI().checkDoneTaskUseItem(pl, item);
                InventoryService.gI().sendItemBags(pl);
            } else {
                Service.gI().sendThongBaoOK(pl, "Sức mạnh không đủ yêu cầu");
            }
        }
    }

    public void openRuongGo(Player player) {
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE, 1);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE1, 1);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE2, 1);
        Item ruongGo = InventoryService.gI().findItemBag(player, 570);
        if (ruongGo != null) {
            int level = InventoryService.gI().getParam(player, 72, 570);
            int requiredSlots = calculateRequiredEmptySlots(level);
            if (InventoryService.gI().getCountEmptyBag(player) < requiredSlots) {
                Service.gI().sendThongBao(player, "Cần ít nhất " + (requiredSlots - InventoryService.gI().getCountEmptyBag(player)) + " ô trống trong hành trang");
            } else {
                player.itemsWoodChest.clear();
                if (level == 0) {
                    InventoryService.gI().subQuantityItemsBag(player, ruongGo, 1);
                    InventoryService.gI().sendItemBags(player);
                    Item item = ItemService.gI().createNewItem((short) 190);
                    item.quantity = 1;
                    InventoryService.gI().addItemBag(player, item);
                    InventoryService.gI().sendItemBags(player);

                    Service.gI().sendThongBao(player, "reward");
                    return;
                }
                int baseGoldAmount = 100 * level;
                int randomFactor = Util.nextInt(-15, 15);
                int goldAmount = baseGoldAmount + (baseGoldAmount * randomFactor / 100);

                Item itemGold = ItemService.gI().createNewItem((short) 190);
                itemGold.quantity = goldAmount * 1000;
                player.itemsWoodChest.add(itemGold);
                if (level >= 9) {
                    int quantity = 100 + (level - 9) * 20;
                    Item item77 = ItemService.gI().createNewItem((short) 77);
                    item77.quantity = quantity;
                    player.itemsWoodChest.add(item77);
                }

                // Phần thưởng đồ tại rương
                int clothesCount = 1;
                if (level >= 5 && level <= 8) {
                    clothesCount = 2;  // Nếu cấp độ từ 5 đến 8, thưởng 2 món đồ
                } else if (level >= 10 && level <= 12) {
                    clothesCount = 3;  // Nếu cấp độ từ 10 đến 12, thưởng 3 món đồ
                }

                // Tạo đồ thưởng (clothes) và thêm vào phần thưởng
                for (int i = 0; i < clothesCount; i++) {
                    int randItemId = randClothes(level);  // Lấy ID ngẫu nhiên của món đồ
                    Item rewardItem = ItemService.gI().createNewItem((short) randItemId);
                    List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) randItemId);
                    if (ops != null && !ops.isEmpty()) {
                        rewardItem.itemOptions.addAll(ops);  // Thêm thuộc tính item
                    }
                    rewardItem.quantity = 1;  // Số lượng món đồ là 1
                    player.itemsWoodChest.add(rewardItem);  // Thêm món đồ vào phần thưởng
                }

                // Phần thưởng item ngẫu nhiên (từ rewardItems)
                int[] rewardItems = {17, 18, 19, 20, 380, 381, 382, 383, 384, 385, 1229};
                int rewardCount = 2;  // Số lượng item mặc định

                // Thay đổi số lượng phần thưởng tùy theo cấp độ
                if (level >= 5 && level <= 8) {
                    rewardCount = 3;  // Nếu cấp độ từ 5 đến 8, thưởng 3 item ngẫu nhiên
                } else if (level >= 10 && level <= 12) {
                    rewardCount = 4;  // Nếu cấp độ từ 10 đến 12, thưởng 4 item ngẫu nhiên
                }

                // Thêm item ngẫu nhiên vào phần thưởng
                Set<Integer> selectedItems = new HashSet<>();
                while (selectedItems.size() < rewardCount) {
                    int randItemId = rewardItems[Util.nextInt(0, rewardItems.length - 1)];
                    if (!selectedItems.contains(randItemId)) {
                        selectedItems.add(randItemId);
                        Item rewardItem = ItemService.gI().createNewItem((short) randItemId);
                        rewardItem.quantity = Util.nextInt(1, level);  // Số lượng item phụ thuộc vào cấp độ
                        player.itemsWoodChest.add(rewardItem);  // Thêm item vào phần thưởng
                    }
                }

                // Phần thưởng sao pha lê (nâng cấp)
                int saoPhaLeCount = (level > 9) ? 2 : 1;  // Nếu cấp độ > 9, thêm 2 sao phá lệ
                for (int i = 0; i < saoPhaLeCount; i++) {
                    int rand = Util.nextInt(0, 6);
                    Item level1 = ItemService.gI().createNewItem((short) (441 + rand));
                    level1.itemOptions.add(new Item.ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));
                    level1.quantity = Util.nextInt(1, 3);  // Số lượng sao phá lệ
                    player.itemsWoodChest.add(level1);  // Thêm sao phá lệ vào phần thưởng
                }
                int dncCount = (level > 9) ? 2 : 1;
                for (int i = 0; i < dncCount; i++) {
                    int rand = Util.nextInt(0, 4);
                    Item dnc = ItemService.gI().createNewItem((short) (220 + rand));
                    dnc.itemOptions.add(new Item.ItemOption(71 - rand, 0));
                    dnc.quantity = Util.nextInt(1, level * 2);  // Số lượng đá nâng cấp phụ thuộc vào cấp độ
                    player.itemsWoodChest.add(dnc);  // Thêm đá nâng cấp vào phần thưởng            
                }

                // Trừ 1 rương gỗ
                InventoryService.gI().subQuantityItemsBag(player, ruongGo, 1);
                InventoryService.gI().sendItemBags(player);

                // Thêm các phần thưởng vào hành trang
                for (Item it : player.itemsWoodChest) {
                    InventoryService.gI().addItemBag(player, it);
                }
                InventoryService.gI().sendItemBags(player);

                // Cập nhật chỉ số rương gỗ
                player.indexWoodChest = player.itemsWoodChest.size() - 1;
                int i = player.indexWoodChest;
                if (i < 0) {
                    return;
                }
                Item itemWoodChest = player.itemsWoodChest.get(i);
                player.indexWoodChest--;
                String info = "|1|" + itemWoodChest.template.name;
                if (itemWoodChest.quantity > 1) {
                    info += " (x" + itemWoodChest.quantity + ")";
                }

                String info2 = "\n|2|";
                if (!itemWoodChest.itemOptions.isEmpty()) {
                    for (Item.ItemOption io : itemWoodChest.itemOptions) {
                        if (io.optionTemplate.id != 102 && io.optionTemplate.id != 73) {
                            info2 += io.getOptionString() + "\n";
                        }
                    }
                }
                info = (info2.length() > "\n|2|".length() ? (info + info2).trim() : info.trim()) + "\n|0|" + itemWoodChest.template.description;
                NpcService.gI().createMenuConMeo(player, ConstNpc.RUONG_GO, -1, "Bạn nhận được\n"
                        + info.trim(), "OK" + (i > 0 ? " [" + i + "]" : ""));
            }
        }
    }

    public int calculateRequiredEmptySlots(int level) {
        // Khởi tạo số ô trống cần thiết
        int requiredSlots = 0;

        // Tính số lượng vàng
        int baseGoldAmount = 100 * level;
        int randomFactor = Util.nextInt(-15, 15);
        int goldAmount = baseGoldAmount + (baseGoldAmount * randomFactor / 100);

        // Vàng có ID 190, không tính vào số ô trống yêu cầu
        if (goldAmount > 0) {
            requiredSlots++;
        }

        // Tính phần thưởng quần áo
        int clothesCount = 1;
        if (level >= 5 && level <= 8) {
            clothesCount = 2;
        } else if (level >= 10 && level <= 12) {
            clothesCount = 3;
        }
        // Đếm số phần thưởng quần áo
        requiredSlots += clothesCount;

        // Tính phần thưởng item hỗ trợ
        int[] rewardItems = {17, 18, 19, 20, 380, 381, 382, 383, 384, 385, 1229};
        int rewardCount = 2;

        if (level >= 5 && level <= 8) {
            rewardCount = 3;
        } else if (level >= 10 && level <= 12) {
            rewardCount = 4;
        }
        // Đếm phần thưởng item hỗ trợ
        requiredSlots += rewardCount;

        // Tính sao pha lê (Số lượng 2 nếu level > 9)
        int saoPhaLeCount = (level > 9) ? 2 : 1;
        requiredSlots += saoPhaLeCount;

        // Tính đá nâng cấp (Số lượng 2 nếu level > 9)
        int dncCount = (level > 9) ? 2 : 1;
        requiredSlots += dncCount;

        // Trả về tổng số ô trống cần thiết
        return requiredSlots;
    }
    
    public void RadaNgocRong(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1822);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {1579, 1580, 1581, 1582, 1583, 1584, 1585};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(10, 17);
            int hp = randomInRange(10, 17);
            int ki = randomInRange(10, 17);
            int khongthegiaodich = randomInRange(0, 0);

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 15));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
        }
    }
    
    public void RadaNgocRongVip(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1823);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {1586, 1809, 1810, 1811, 1812, 1813, 1814};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(10, 20);
            int hp = randomInRange(10, 20);
            int ki = randomInRange(10, 20);
            int khongthegiaodich = randomInRange(0, 0);

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(210, 3));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 15));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
        }
    }
    
    public void KhoBauHaiTac(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1569);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {618, 619, 620, 621, 622, 623, 624, 625,626};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(10, 15);
            int hp = randomInRange(10, 15);
            int ki = randomInRange(10, 15);
            int khongthegiaodich = randomInRange(0, 0);

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(210, 2));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 15));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
        }
    }

    private int randClothes(int level) {
        int result = level - Util.nextInt(2, 4);
        if (result < 1) {
            result = 1;
        }
        return ConstItem.LIST_ITEM_CLOTHES[Util.nextInt(0, 2)][Util.nextInt(0, 4)][result];
    }

    private void changePet(Player player, Item item) {
        if (player.pet != null) {
            int gender = player.pet.gender + 1;
            if (gender > 2) {
                gender = 0;
            }
            PetService.gI().changeNormalPet(player, gender);
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    private void changePetRamdom(Player player, Item item) {
        if (item.template.id != 1795) {
            Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ!");
            return;
        }

        if (!item.hasOption(250, 3000)) {
            Service.gI().sendThongBao(player, "Cần ít nhất 3000 sức mạnh Kilis để mở!");
            return;
        }

        if (player.pet == null || player.pet.typePet != 1 || player.pet.nPoint.power < 40_000_000_000L) {
            Service.gI().sendThongBao(player, "Cần có đệ Mabư đạt 40 tỷ sức mạnh để thực hiện!");
            return;
        }

        int[] petTypes = {2, 3, 4};
        int randomType = petTypes[rand.nextInt(petTypes.length)];
        switch (randomType) {
            case 2:
                PetService.gI().createUubPet(player);
                break;
            case 3:
                PetService.gI().createKidBeerPet(player);
                break;
            case 4:
                PetService.gI().createJirenPet(player);
                break;
        }

        InventoryService.gI().removeItemBag(player, item);
        InventoryService.gI().sendItemBags(player);

        Service.gI().sendThongBao(player, "Bạn đã nhận được đệ tử mới!");
    }

    private void eatGrapes(Player pl, Item item) {
        int percentCurrentStatima = pl.nPoint.stamina * 100 / pl.nPoint.maxStamina;
        if (percentCurrentStatima > 50) {
            Service.gI().sendThongBao(pl, "Thể lực vẫn còn trên 50%");
            return;
        } else if (item.template.id == 211) {
            pl.nPoint.stamina = pl.nPoint.maxStamina;
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 100%");
        } else if (item.template.id == 212) {
            pl.nPoint.stamina += (pl.nPoint.maxStamina * 20 / 100);
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 20%");
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        PlayerService.gI().sendCurrentStamina(pl);
    }

    private void PhaoBong(Player pl, Item item) {
        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.XSMAX, 1);
        int goldGain = Util.nextInt(500_000, 2_000_000);
        pl.inventory.addGold(goldGain);
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 15000);
        Service.gI().LogicEffect(pl, 63, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);

        pl.point_sukien1 += 1;
        pl.event.addEventPoint(1);
        Manager.isTopSukien1Changed = true;
        Item removeItem = InventoryService.gI().findItemBag(pl, 1575);
        if (removeItem != null) {
            InventoryService.gI().subQuantityItemsBag(pl, removeItem, 1);
        }

        PlayerService.gI().sendInfoHpMpMoney(pl);
        InventoryService.gI().sendItemBags(pl);
        Service.gI().sendThongBao(pl, "Bắn Pháo Bông nhận được " + Util.formatNumber(goldGain) + " Vàng (+1 điểm sự kiện)!");
    }

    private void PhaoBongVip(Player pl, Item item) {
        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.XSMAX, 1);
        int goldGain = Util.nextInt(2_000_000, 5_000_000);
        int gemGain = Util.nextInt(10, 30);
        pl.inventory.addGold(goldGain);
        pl.inventory.gem += gemGain;
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 15000);
        Service.gI().LogicEffect(pl, 63, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);

        pl.point_sukien += 1;
        pl.event.addEventPoint(1);
        Manager.isTopSukienChanged = true;
        Item removeItem = InventoryService.gI().findItemBag(pl, 1576);
        if (removeItem != null) {
            InventoryService.gI().subQuantityItemsBag(pl, removeItem, 1);
        }

        PlayerService.gI().sendInfoHpMpMoney(pl);
        InventoryService.gI().sendItemBags(pl);
        Service.gI().sendThongBao(pl, "Bắn Pháo Bông VIP nhận được " + Util.formatNumber(goldGain) + " Vàng & " + gemGain + " Ngọc Xanh (+1 điểm sự kiện)!");
    }

    private void KemTraiCay(Player player, Item item) {
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.XSMAX, 1);
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1609);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {1803, 1804, 1806};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(20, 28);
            int hp = randomInRange(20, 30);
            int ki = randomInRange(20, 30);
            int khongthegiaodich = 0;

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 15));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            player.point_sukien2 += 1;
            player.event.addEventPoint(1);
            if (!Manager.isTopSukien2Changed) {
                Manager.isTopSukien2Changed = true;
            }

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Thưởng thức Kem Trái Cây nhận được " + newItem.template.name + " (+1 điểm đua Top Kem Trái Cây)!");
        } else {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang!");
        }
    }

    private void QuaThieuNhi(Player player, Item item) {
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.XSMAX, 1);
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1608);
            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {1807, 1599, 1600, 1601, 1602};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            if (newItem == null) {
                Service.gI().sendThongBao(player, "Không thể tạo vật phẩm. Liên hệ admin.");
                return;
            }

            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(20, 25);
            int hp = randomInRange(20, 25);
            int ki = randomInRange(20, 25);
            int khongthegiaodich = 0;

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(210, 4));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));
            if (Math.random() >= 0.5) {
                newItem.itemOptions.add(new ItemOption(93, 15));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            player.point_sukien += 1;
            player.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Mở Hộp Quà Thiếu Nhi nhận được " + newItem.template.name + " (+1 điểm đua Top Thiếu Nhi)!");
        } else {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang!");
        }
    }

    private void ThucAnChoThan(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            Item itemUsed = InventoryService.gI().findItemBag(player, item.template.id);

            if (itemUsed == null || itemUsed.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int id = itemUsed.template.id;
            if ((id == 1789 || id == 1799 || id == 1800 || id == 1801 || id == 1802) && itemUsed.quantity >= 99) {

                InventoryService.gI().subQuantityItemsBag(player, itemUsed, 99);

                Item newItem = ItemService.gI().createNewItem((short) 1805);
                InventoryService.gI().addItemBag(player, newItem);

                Service.gI().sendThongBao(player, "Bạn đã nhận được 1 " + newItem.template.name + "!");
            } else {
                Service.gI().sendThongBao(player, "Số lượng vật phẩm không đủ hoặc không đúng loại!");
                return;
            }

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
        } else {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
        }
    }

    private void useThoiVang(Player pl, Item item) {
        if (pl.inventory.gold >= Inventory.LIMIT_GOLD) {
            Service.gI().sendThongBao(pl, "Số vàng của bạn đã đạt giới hạn tối đa!");
            return;
        }
        long goldAdd = 500_000_000L;
        if (pl.inventory.gold + goldAdd > Inventory.LIMIT_GOLD) {
            goldAdd = Inventory.LIMIT_GOLD - pl.inventory.gold;
        }
        pl.inventory.gold += goldAdd;
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        PlayerService.gI().sendInfoHpMpMoney(pl);
        Service.gI().sendThongBao(pl, "Bạn vừa dùng 1 Thỏi vàng và nhận được " + Util.numberToMoney(goldAdd) + " vàng!");
    }

    private void openCSKB(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] temp = {76, 188, 189, 190, 381, 382, 383, 384, 385};
            int[][] gold = {{5000, 20000}};
            int countToOpen = (item.quantity >= 10 && InventoryService.gI().getCountEmptyBag(pl) >= 5) ? 10 : 1;
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            int totalGold = 0;
            int totalItems = 0;
            for (int c = 0; c < countToOpen; c++) {
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                if (index <= 3) {
                    int g = Util.nextInt(gold[0][0], gold[0][1]);
                    totalGold += g;
                    pl.inventory.gold += g;
                    if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                        pl.inventory.gold = Inventory.LIMIT_GOLD;
                    }
                    icon[1] = 930;
                } else {
                    Item it = ItemService.gI().createNewItem(temp[index]);
                    it.itemOptions.add(new ItemOption(73, 0));
                    InventoryService.gI().addItemBag(pl, it);
                    icon[1] = it.template.iconID;
                    totalItems++;
                }
            }
            PlayerService.gI().sendInfoHpMpMoney(pl);
            InventoryService.gI().subQuantityItemsBag(pl, item, countToOpen);
            InventoryService.gI().sendItemBags(pl);

            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
            if (countToOpen > 1) {
                Service.gI().sendThongBao(pl, "Đã mở nhanh x" + countToOpen + " CSKB (Nhận " + totalItems + " item & " + Util.numberToMoney(totalGold) + " vàng)");
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void useItemTime(Player pl, Item item) {
        long now = System.currentTimeMillis();
        switch (item.template.id) {
            case 379: // máy dò capsule
                if (pl.itemTime.isUseMayDo) {
                    pl.itemTime.lastTimeUseMayDo += ItemTime.TIME_MAY_DO;
                } else {
                    pl.itemTime.lastTimeUseMayDo = now;
                    pl.itemTime.isUseMayDo = true;
                }
                break;
            case 1635: // co bon la
                if (pl.itemTime.isUseCoBonLa) {
                    pl.itemTime.lastTimeUseCoBonLa += ItemTime.TIME_CO_BON_LA;
                } else {
                    pl.itemTime.lastTimeUseCoBonLa = now;
                    pl.itemTime.timeLengthCoBonLa = ItemTime.TIME_CO_BON_LA;
                    pl.itemTime.isUseCoBonLa = true;
                }
                break;
            case 1614: // nuoc mia 1
                if (pl.itemTime.isUseNuocMia2 || pl.itemTime.isUseNuocMia3) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseNuocMia1) {
                    pl.itemTime.lastTimeUseNuocMia1 += ItemTime.TIME_NUOC_MIA1;
                } else {
                    pl.itemTime.lastTimeUseNuocMia1 = now;
                    pl.itemTime.isUseNuocMia1 = true;
                }
                Service.gI().point(pl);
                break;
            case 1615: // nuoc mia 2
                if (pl.itemTime.isUseNuocMia1 || pl.itemTime.isUseNuocMia3) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseNuocMia2) {
                    pl.itemTime.lastTimeUseNuocMia2 += ItemTime.TIME_NUOC_MIA2;
                } else {
                    pl.itemTime.lastTimeUseNuocMia2 = now;
                    pl.itemTime.isUseNuocMia2 = true;
                }
                Service.gI().point(pl);
                break;
            case 1616: // nuoc mia 3
                if (pl.itemTime.isUseNuocMia1 || pl.itemTime.isUseNuocMia2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseNuocMia3) {
                    pl.itemTime.lastTimeUseNuocMia3 += ItemTime.TIME_NUOC_MIA3;
                } else {
                    pl.itemTime.lastTimeUseNuocMia3 = now;
                    pl.itemTime.isUseNuocMia3 = true;
                }
                Service.gI().point(pl);
                break;
            case 381: // cuồng nộ
                if (pl.itemTime.isUseCuongNo2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseCuongNo) {
                    pl.itemTime.lastTimeCuongNo += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeCuongNo = now;
                    pl.itemTime.isUseCuongNo = true;
                }
                Service.gI().point(pl);
                break;
            case 382: // bổ huyết
                if (pl.itemTime.isUseBoHuyet2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseBoHuyet) {
                    pl.itemTime.lastTimeBoHuyet += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeBoHuyet = now;
                    pl.itemTime.isUseBoHuyet = true;
                }
                Service.gI().point(pl);
                break;
            case 383: // bổ khí
                if (pl.itemTime.isUseBoKhi2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseBoKhi) {
                    pl.itemTime.lastTimeBoKhi += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeBoKhi = now;
                    pl.itemTime.isUseBoKhi = true;
                }
                Service.gI().point(pl);
                break;
            case 384: // giáp xên
                if (pl.itemTime.isUseGiapXen2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseGiapXen) {
                    pl.itemTime.lastTimeGiapXen += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeGiapXen = now;
                    pl.itemTime.isUseGiapXen = true;
                }
                Service.gI().point(pl);
                break;
            case 385: // ẩn danh
                if (pl.itemTime.isUseAnDanh) {
                    pl.itemTime.lastTimeAnDanh += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeAnDanh = now;
                    pl.itemTime.isUseAnDanh = true;
                }
                break;
            case 764: // Khau trang
                if (pl.itemTime.isUseKhauTrang) {
                    pl.itemTime.lastTimeKhauTrang += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeKhauTrang = now;
                    pl.itemTime.isUseKhauTrang = true;
                }
                break;
            case 1150: // cuồng nộ 2
                if (pl.itemTime.isUseCuongNo) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseCuongNo2) {
                    pl.itemTime.lastTimeCuongNo2 += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeCuongNo2 = now;
                    pl.itemTime.isUseCuongNo2 = true;
                }
                Service.gI().point(pl);
                break;

            case 1151: // bổ khí 2
                if (pl.itemTime.isUseBoKhi) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseBoKhi2) {
                    pl.itemTime.lastTimeBoKhi2 += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeBoKhi2 = now;
                    pl.itemTime.isUseBoKhi2 = true;
                }
                Service.gI().point(pl);
                break;

            case 1152: // bổ huyết 2
                if (pl.itemTime.isUseBoHuyet) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseBoHuyet2) {
                    pl.itemTime.lastTimeBoHuyet2 += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeBoHuyet2 = now;
                    pl.itemTime.isUseBoHuyet2 = true;
                }
                Service.gI().point(pl);
                break;

            case 1153: // giáp xên 2
                if (pl.itemTime.isUseGiapXen) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                if (pl.itemTime.isUseGiapXen2) {
                    pl.itemTime.lastTimeGiapXen2 += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeGiapXen2 = now;
                    pl.itemTime.isUseGiapXen2 = true;
                }
                Service.gI().point(pl);
                break;

            case 1154: // an danh 2
                if (pl.itemTime.isUseAnDanh2) {
                    pl.itemTime.lastTimeAnDanh2 += ItemTime.TIME_ITEM;
                } else {
                    pl.itemTime.lastTimeAnDanh2 = now;
                    pl.itemTime.isUseAnDanh2 = true;
                }
                break;

            case 638: // Commeson
                if (pl.itemTime.isUseCMS) {
                    pl.itemTime.lastTimeUseCMS += ItemTime.TIME_CMS;
                } else {
                    pl.itemTime.lastTimeUseCMS = now;
                    pl.itemTime.isUseCMS = true;
                }
                break;
            case 1233: // Nồi cơm điện
                if (pl.itemTime.isUseNCD) {
                    pl.itemTime.lastTimeUseNCD += ItemTime.TIME_NCD;
                } else {
                    pl.itemTime.lastTimeUseNCD = now;
                    pl.itemTime.isUseNCD = true;
                }
                break;
            case 579:
            case 1045: // Đuôi khỉ
                if (pl.itemTime.isUseDK) {
                    pl.itemTime.lastTimeUseDK += ItemTime.TIME_DK;
                } else {
                    pl.itemTime.lastTimeUseDK = now;
                    pl.itemTime.isUseDK = true;
                }
                break;
            case 663: // bánh pudding
            case 664: // xúc xích
            case 665: // kem dâu
            case 666: // mì ly
            case 667: // sushi
                if (pl.itemTime.isEatMeal && pl.itemTime.iconMeal == item.template.iconID) {
                    pl.itemTime.lastTimeEatMeal += ItemTime.TIME_EAT_MEAL;
                } else {
                    pl.itemTime.lastTimeEatMeal = now;
                    pl.itemTime.isEatMeal = true;
                    ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal);
                    pl.itemTime.iconMeal = item.template.iconID;
                }
                break;
            case 880:
            case 881:
            case 882:
                if (pl.itemTime.isEatMeal2 && pl.itemTime.iconMeal2 == item.template.iconID) {
                    pl.itemTime.lastTimeEatMeal2 += ItemTime.TIME_EAT_MEAL;
                } else {
                    pl.itemTime.lastTimeEatMeal2 = now;
                    pl.itemTime.isEatMeal2 = true;
                    ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal2);
                    pl.itemTime.iconMeal2 = item.template.iconID;
                }
                break;

            case 1532: // máy dò đồ
                if (pl.itemTime.isUseKhoBauX2) {
                    pl.itemTime.lastTimeUseKhoBauX2 += ItemTime.TIME_MAY_DO2;
                } else {
                    pl.itemTime.lastTimeUseKhoBauX2 = now;
                    pl.itemTime.isUseKhoBauX2 = true;
                }
                break;
            case 1628: // bùa santa
                if (pl.itemTime.isUseBuaSanta) {
                    pl.itemTime.lastTimeBuaSanta += 1_800_000; // 30p
                } else {
                    pl.itemTime.lastTimeBuaSanta = now + 1;
                    pl.itemTime.isUseBuaSanta = true;
                }
                break;

        }
        Service.gI().point(pl);
        ItemTimeService.gI().sendAllItemTime(pl);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
    }

    private void controllerCallRongThan(Player pl, Item item) {
        int tempId = item.template.id;
        if (tempId >= SummonDragon.NGOC_RONG_1_SAO && tempId <= SummonDragon.NGOC_RONG_7_SAO) {
            switch (tempId) {
                case SummonDragon.NGOC_RONG_1_SAO:
                case SummonDragon.NGOC_RONG_2_SAO:
                case SummonDragon.NGOC_RONG_3_SAO:
                    SummonDragon.gI().openMenuSummonShenron(pl, (byte) (tempId - 13));
                    break;
                default:
                    NpcService.gI().createMenuConMeo(pl, ConstNpc.TUTORIAL_SUMMON_DRAGON,
                            -1, "Bạn chỉ có thể gọi rồng từ ngọc 3 sao, 2 sao, 1 sao", "Hướng\ndẫn thêm\n(mới)", "OK");
                    break;
            }
        } else if (tempId >= Shenron_Service.NGOC_RONG_1_SAO && tempId <= Shenron_Service.NGOC_RONG_7_SAO) {
            Shenron_Service.gI().openMenuSummonShenron(pl, 0);
        } else if (tempId >= Shenron_Service.NGOC_RONG_BANG_1_SAO && tempId <= Shenron_Service.NGOC_RONG_BANG_7_SAO) {
            Shenron_Service.gI().openMenuSummonShenron(pl, 2);
        } else if (tempId >= ConstItem.NGOC_RONG_TIM_1_SAO && tempId <= ConstItem.NGOC_RONG_TIM_7_SAO) {
            if (tempId == ConstItem.NGOC_RONG_TIM_1_SAO) {
                Shenron_Service.gI().openMenuSummonShenron(pl, 1);
            } else {
                Service.gI().sendThongBao(pl, "Bạn chỉ có thể gọi Rồng Thần Tím từ Ngọc Rồng Tím 1 sao (cần đủ bộ 1-7 sao Tím)!");
            }
        }
    }

    private void learnSkill(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.gender == pl.gender || item.template.gender == 3) {
                String[] subName = item.template.name.split("");
                byte level = Byte.parseByte(subName[subName.length - 1]);
                Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                if (curSkill.point == 7) {
                    Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                } else {
                    if (curSkill.point == 0) {
                        if (level == 1) {//Hoc skill moi
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 23);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else { // neu chua hoc ma hoc lv cao
                            Skill skillNeed = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            Service.gI().sendThongBao(pl, "Vui lòng học " + skillNeed.template.name + " cấp " + skillNeed.point + " trước!");
                        }
                    } else {
                        if (curSkill.point + 1 == level) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            pl.BoughtSkill.add((int) item.template.id);
                            //System.out.println(curSkill.template.name + " - " + curSkill.point);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 62);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else {
                            Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " cấp " + (curSkill.point + 1) + " trước!");
                        }
                    }
                    InventoryService.gI().sendItemBags(pl);
                }
            } else {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        }
    }

    private void useTDLT(Player pl, Item item) {
        if (pl.itemTime.isUseTDLT) {
            ItemTimeService.gI().turnOffTDLT(pl, item);
        } else {
            ItemTimeService.gI().turnOnTDLT(pl, item);
        }
    }

    private void usePorata2(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusion2(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }
    private void usePorata3(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusion3(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void usePorata(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusion(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void openCapsuleUI(Player pl) {
        pl.idMark.setTypeChangeMap(ConstMap.CHANGE_CAPSULE);
        ChangeMapService.gI().openChangeMapTab(pl);
    }

    public void choseMapCapsule(Player pl, int index) {

        if (pl.idNRNM != -1) {
            Service.gI().sendThongBao(pl, "Không thể mang ngọc rồng này lên Phi thuyền");
            Service.gI().hideWaitDialog(pl);
            return;
        }

        int zoneId = -1;
        if (index > pl.mapCapsule.size() - 1 || index < 0) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            Service.gI().hideWaitDialog(pl);
            return;
        }
        Zone zoneChose = pl.mapCapsule.get(index);
        //Kiểm tra số lượng người trong khu

        if (zoneChose.getNumOfPlayers() > 25
                || MapService.gI().isMapDoanhTrai(zoneChose.map.mapId)
                || MapService.gI().isMapMaBu(zoneChose.map.mapId)
                || MapService.gI().isMapHuyDiet(zoneChose.map.mapId)) {
            Service.gI().sendThongBao(pl, "Hiện tại không thể vào được khu!");
            return;
        }
        boolean isReturningToOldMap = (index == 0 && pl.mapBeforeCapsule != null
                && zoneChose.map.mapId == pl.mapBeforeCapsule.map.mapId);
        if (isReturningToOldMap) {
            zoneId = pl.mapBeforeCapsule.zoneId;
            pl.mapBeforeCapsule = null;
        } else {
            if (pl.zone != null && !MapService.gI().isHome(pl.zone.map.mapId)
                    && !MapService.gI().isMapPhoBan(pl.zone.map.mapId)
                    && !MapService.gI().isMapMaBu(pl.zone.map.mapId)
                    && !MapService.gI().isMapBlackBallWar(pl.zone.map.mapId)
                    && !MapService.gI().isMapOffline(pl.zone.map.mapId)) {
                pl.mapBeforeCapsule = pl.zone;
            }
            String bossNames = nro.models.boss.Boss_Manager.BossManager.gI().getAliveWorldBossNamesInMap(zoneChose.map.mapId);
            if (!bossNames.isEmpty()) {
                zoneId = 0;
            }
        }
        pl.changeMapVIP = true;
        ChangeMapService.gI().changeMapBySpaceShip(pl, zoneChose.map.mapId, zoneId, -1);
    }

    public void eatPea(Player player) {
        if (player.zone != null && player.zone.map != null && player.zone.map.mapId == 134
                && (player.isPKDHVT || player.location.y <= 312)) {
            Service.gI().sendThongBao(player, "Không thể sử dụng Đậu thần khi đang thi đấu trên võ đài Bang Hội!");
            return;
        }
        if (!Util.canDoWithTime(player.lastTimeEatPea, 1000)) {
            return;
        }
        player.lastTimeEatPea = System.currentTimeMillis();
        Item pea = null;
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.type == 6) {
                pea = item;
                break;
            }
        }
        if (pea == null && player.magicTree != null) {
            player.magicTree.update();
            if (player.magicTree.currPeas > 0) {
                player.magicTree.harvestPea();
                for (Item item : player.inventory.itemsBag) {
                    if (item.isNotNullItem() && item.template.type == 6) {
                        pea = item;
                        break;
                    }
                }
            }
        }
        boolean fromBox = false;
        if (pea == null && player.inventory.itemsBox != null) {
            for (Item item : player.inventory.itemsBox) {
                if (item.isNotNullItem() && item.template.type == 6) {
                    pea = item;
                    fromBox = true;
                    break;
                }
            }
        }
        if (pea != null) {
            int hpKiHoiPhuc = 0;
            int lvPea = Integer.parseInt(pea.template.name.substring(13));
            for (Item.ItemOption io : pea.itemOptions) {
                if (io.optionTemplate.id == 2) {
                    hpKiHoiPhuc = io.param * 1000;
                    break;
                }
                if (io.optionTemplate.id == 48) {
                    hpKiHoiPhuc = io.param;
                    break;
                }
            }
            player.nPoint.setHp(player.nPoint.hp + hpKiHoiPhuc);
            player.nPoint.setMp(player.nPoint.mp + hpKiHoiPhuc);
            player.nPoint.stamina += (short) (100 * lvPea);
            if (player.nPoint.stamina > player.nPoint.maxStamina) {
                player.nPoint.stamina = player.nPoint.maxStamina;
            }
            PlayerService.gI().sendInfoHpMp(player);
            Service.gI().sendInfoPlayerEatPea(player);
            if (player.pet != null && player.zone != null && player.zone.equals(player.pet.zone) && !player.pet.isDie()) {
                int statima = 100 * lvPea;
                player.pet.nPoint.stamina += statima;
                if (player.pet.nPoint.stamina > player.pet.nPoint.maxStamina) {
                    player.pet.nPoint.stamina = player.pet.nPoint.maxStamina;
                }
                player.pet.nPoint.setHp(player.pet.nPoint.hp + hpKiHoiPhuc);
                player.pet.nPoint.setMp(player.pet.nPoint.mp + hpKiHoiPhuc);
                Service.gI().sendInfoPlayerEatPea(player.pet);
                Service.gI().chatJustForMe(player, player.pet, "Cám ơn sư phụ");
                Service.gI().showInfoPet(player);
            }

            if (fromBox) {
                InventoryService.gI().subQuantityItemsBox(player, pea, 1);
                InventoryService.gI().sendItemBoxSilent(player);
            } else {
                InventoryService.gI().subQuantityItemsBag(player, pea, 1);
                InventoryService.gI().sendItemBags(player);
            }
        }
    }

    private void upSkillPet(Player pl, Item item) {
        if (pl.pet == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        try {
            switch (item.template.id) {
                case 402: //skill 1
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 0)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 403: //skill 2
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 1)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 404: //skill 3
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 2)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 759: //skill 4 & skill 5
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 3)
                            || SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 4)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                        Service.gI().showInfoPet(pl);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;

            }

        } catch (Exception e) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    public void OpenHopThanlinh(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[][] itemsByGenderAndType = {
                {555, 556, 562, 563, 561},
                {557, 558, 564, 565, 561},
                {559, 560, 566, 567, 561}
            };

            List<Item> allPreInitializedItems = new ArrayList<>();

            for (int genderIndex = 0; genderIndex < itemsByGenderAndType.length; genderIndex++) {
                short aoId = (short) itemsByGenderAndType[genderIndex][0];
                short quanId = (short) itemsByGenderAndType[genderIndex][1];
                short gangId = (short) itemsByGenderAndType[genderIndex][2];
                short giayId = (short) itemsByGenderAndType[genderIndex][3];
                short nhanId = (short) itemsByGenderAndType[genderIndex][4];

                Item aotl = ItemService.gI().createNewItem(aoId);
                RewardService.gI().initChiSoItem(aotl);
                aotl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(aotl);

                Item wTl = ItemService.gI().createNewItem(quanId);
                RewardService.gI().initChiSoItem(wTl);
                wTl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(wTl);

                Item gTl = ItemService.gI().createNewItem(gangId);
                RewardService.gI().initChiSoItem(gTl);
                gTl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(gTl);

                Item jayTl = ItemService.gI().createNewItem(giayId);
                RewardService.gI().initChiSoItem(jayTl);
                jayTl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(jayTl);

                Item RdTl = ItemService.gI().createNewItem(nhanId);
                RewardService.gI().initChiSoItem(RdTl);
                RdTl.itemOptions.add(new ItemOption(30, 1));
                allPreInitializedItems.add(RdTl);
            }

            Random random = new Random();
            Item chosenItem = allPreInitializedItems.get(random.nextInt(allPreInitializedItems.size()));
            InventoryService.gI().addItemBag(player, chosenItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn vừa nhận được 1 " + chosenItem.template.name + " Thần linh!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có ít nhất 1 ô trống hành trang");
        }
    }

    public void OpenHopQuaThuong(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1772, 1773};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 10));   // HP
            newItem.itemOptions.add(new ItemOption(77, 10));   // KI
            newItem.itemOptions.add(new ItemOption(103, 10)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(14, 10));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }
            ItemService.gI().randomizeCostumeStats(newItem, new ArrayList<>(newItem.itemOptions));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            player.point_sukien += 1;
            player.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name + " (+1 điểm sự kiện)!");
        } else {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang!");
        }
    }

    public void OpenTrungRongNhi(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {1765, 1766, 1767, 1768, 1769, 1770, 1771};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            int hp = randomInRange(10, 18);
            int ki = randomInRange(5, 10);
            int damage = randomInRange(5, 10);
            int hsd = randomInRange(0, 1);

            newItem.itemOptions.add(new ItemOption(50, hp));
            newItem.itemOptions.add(new ItemOption(5, ki));
            newItem.itemOptions.add(new ItemOption(14, damage));
            newItem.itemOptions.add(new ItemOption(30, hsd));

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 15));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được vật phẩm ngẫu nhiên!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có ít nhất 5 ô trống trong hành trang.");
        }
    }

    private int randomInRange(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    public void OpenHopQuaVip(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1761, 1762, 1763, 1764, 1731, 1732};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 28));   // SĐ
            newItem.itemOptions.add(new ItemOption(77, 28));   // HP
            newItem.itemOptions.add(new ItemOption(103, 28));  // KI
            newItem.itemOptions.add(new ItemOption(236, 10));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }
            ItemService.gI().randomizeCostumeStats(newItem, new ArrayList<>(newItem.itemOptions));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            player.point_sukien += 1;
            player.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name + " (+1 điểm sự kiện)!");
        } else {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang!");
        }
    }

    public void OpenHopQuaGokuDay(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 1) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1588, 1589, 1595, 1587, 1593, 1590};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 25));   // HP
            newItem.itemOptions.add(new ItemOption(77, 25));   // KI
            newItem.itemOptions.add(new ItemOption(103, 25)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(210, 4));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }
            ItemService.gI().randomizeCostumeStats(newItem, new ArrayList<>(newItem.itemOptions));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name + " (chỉ số ngẫu nhiên)!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có 5 ô trống trong hành trang.");
        }
    }

    public void OpenHopQuaGokuDayVangNgoc(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 1) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1588, 1589, 1595, 1587, 1593, 1590};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 20));   // HP
            newItem.itemOptions.add(new ItemOption(77, 20));   // KI
            newItem.itemOptions.add(new ItemOption(103, 20)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(210, 2));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }
            ItemService.gI().randomizeCostumeStats(newItem, new ArrayList<>(newItem.itemOptions));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name + " (chỉ số ngẫu nhiên)!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có 5 ô trống trong hành trang.");
        }
    }

    public void OpenHopQuaGokuDayVip(Player player, int itemUseId) {
        if (InventoryService.gI().getCountEmptyBag(player) >= 5) {
            Item itemUsed = InventoryService.gI().findItemBag(player, itemUseId);

            if (itemUsed == null) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần sử dụng.");
                return;
            }

            int[] itemIds = {1588, 1589, 1595, 1587, 1593, 1590};
            int randomIndex = Util.nextInt(0, itemIds.length - 1);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            // Thêm các option cố định
            newItem.itemOptions.add(new ItemOption(50, 27));   // HP
            newItem.itemOptions.add(new ItemOption(77, 27));   // KI
            newItem.itemOptions.add(new ItemOption(103, 27));  // Sức đánh
            newItem.itemOptions.add(new ItemOption(210, 4));   // Chỉ số ẩn

            int randomOption = Util.nextInt(0, 100);
            if (randomOption >= 50) {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }
            ItemService.gI().randomizeCostumeStats(newItem, new ArrayList<>(newItem.itemOptions));

            // Thêm vào túi và cập nhật
            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemUsed, 1);
            InventoryService.gI().sendItemBags(player);

            Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name + " (chỉ số ngẫu nhiên)!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có ít nhất 5 ô trống trong hành trang.");
        }
    }

    public void OpenHopQuaCadic(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 1) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1741, 1742, 1743, 1744, 1745, 1746};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 27));   // HP
            newItem.itemOptions.add(new ItemOption(77, 27));   // KI
            newItem.itemOptions.add(new ItemOption(103, 27)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(210, 4));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption >= 1) {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }
            ItemService.gI().randomizeCostumeStats(newItem, new ArrayList<>(newItem.itemOptions));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name + " (chỉ số ngẫu nhiên)!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có 5 ô trống trong hành trang.");
        }
    }

    private void ItemManhGiay(Player pl, Item item) {
        if (pl.winSTT && !Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            Service.gI().sendThongBao(pl, "Hãy gặp thần mèo Karin để sử dụng");
            return;
        } else if (pl.winSTT && Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            pl.winSTT = false;
            pl.callBossPocolo = false;
            pl.zoneSieuThanhThuy = null;
        }
        NpcService.gI().createMenuConMeo(pl, item.template.id, 564, "Đây chính là dấu hiệu riêng của...\nĐại Ma Vương Pôcôlô\nĐó là một tên quỷ dữ đội lốt người, một kẻ đại gian ác\ncó sức mạnh vô địch và lòng tham không đáy...\nĐối phó với hắn không phải dễ\nCon có chắc chắn muốn tìm hắn không?", "Đồng ý", "Từ chối");
    }

    private void XuongCho(Player pl, Item item) {
        List<Player> bosses = pl.zone.getBosses();
        boolean checkSoi = false;

        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.SOI_HEC_QUYN && !pl.isDie()) {
                    checkSoi = true;
                }
            }
        }

        if (!checkSoi) {
            Service.gI().sendThongBao(pl, "Không tìm thấy Sói hẹc quyn");
            return;
        }

        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.SOI_HEC_QUYN) {
                    Boss soihecQuyn = (Boss) bossPlayer;
                    if (soihecQuyn != null) {
                        if (((SoiHecQuyn) soihecQuyn).KiemTraNhatXuong()) {
                            Service.gI().sendThongBao(pl, "Sói đã no rồi");
                            continue;
                        } else {
                            ((SoiHecQuyn) soihecQuyn).NhatXuong();
                            Service.gI().chat(soihecQuyn, "Ê, Cục xương ngon quá");
                        }

                        ItemMap itemMap = null;
                        int x = pl.location.x;
                        if (x < 0 || x >= pl.zone.map.mapWidth) {
                            return;
                        }
                        int y = pl.zone.map.yPhysicInTop(x, pl.location.y - 24);
                        itemMap = new ItemMap(pl.zone, 460, 1, x, y, pl.id);
                        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.KE_THAO_TUNG_SOI, 1);
                        itemMap.isPickedUp = true;
                        itemMap.createTime -= 23000;
                        if (itemMap != null) {
                            Service.gI().dropItemMap(pl.zone, itemMap);
                        }

                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                        InventoryService.gI().sendItemBags(pl);

                        int roll = Util.nextInt(100);
                        if (roll < 55) { // 55% cơ hội nhận Sao Pha Lê
                            int rand = Util.nextInt(0, 6); // Random từ 0 đến 6
                            short idItem = (short) (rand + 441); // Item 441 + rand
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));

                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        } else if (roll < 85) { // 30% cơ hội nhận Cải trang Sói Hẹc Quyn
                            short idItem = 461; // Cải trang Sói Hẹc Quyn
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(50, 20));
                            it.itemOptions.add(new Item.ItemOption(77, 18));
                            it.itemOptions.add(new Item.ItemOption(103, 18));
                            it.itemOptions.add(new Item.ItemOption(114, 20));
                            it.itemOptions.add(new Item.ItemOption(94, 10));
                            if (Util.isTrue(70, 100)) {
                                it.itemOptions.add(new Item.ItemOption(93, Util.nextInt(3, 7)));
                            }
                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được Cải trang Sói Hẹc Quyn!");
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        } else { // 15% cơ hội nhận Phiếu giảm giá
                            short idItem = 459; // Item 459
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(112, 80));
                            it.itemOptions.add(new Item.ItemOption(93, 90));
                            it.itemOptions.add(new Item.ItemOption(20, Util.nextInt(10000)));
                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        }

                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        ItemMapService.gI().removeItemMapAndSendClient(itemMap);
                        ((SoiHecQuyn) soihecQuyn).leaveMapNew();
                    }

                }
            }

            InventoryService.gI().sendItemBags(pl);
        }

    }

    public void useBiNgoHalloween(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        if (pl.effectSkill != null && pl.effectSkill.isHalloween) {
            EffectSkillService.gI().removeHalloween(pl);
        }
        int roll = Util.nextInt(1, 100);
        if (roll <= 35) {
            int gold = Util.nextInt(2_000_000, 8_000_000);
            pl.inventory.addGold(gold);
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Dùng " + item.template.name + " nhận được " + Util.formatNumber(gold) + " Vàng (+1 điểm sự kiện)!");
        } else if (roll <= 60) {
            int gem = Util.nextInt(10, 30);
            pl.inventory.gem += gem;
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Dùng " + item.template.name + " nhận được " + gem + " Ngọc Xanh (+1 điểm sự kiện)!");
        } else if (roll <= 80) {
            int ruby = Util.nextInt(5, 20);
            pl.inventory.ruby += ruby;
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Dùng " + item.template.name + " nhận được " + ruby + " Hồng Ngọc (+1 điểm sự kiện)!");
        } else if (roll <= 92) {
            short[] items = {1150, 1151, 1152, 1153, 1154, 1635};
            Item rw = ItemService.gI().createNewItem(items[Util.nextInt(0, items.length - 1)], 1);
            InventoryService.gI().addItemBag(pl, rw);
            Service.gI().sendThongBao(pl, "Dùng " + item.template.name + " nhận được " + rw.template.name + " (+1 điểm sự kiện)!");
        } else {
            short[] costumes = {586, 587, 588, 739};
            Item ct = ItemService.gI().createNewItem(costumes[Util.nextInt(0, costumes.length - 1)], 1);
            ct.itemOptions.add(new ItemOption(50, Util.nextInt(18, 25)));
            ct.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
            ct.itemOptions.add(new ItemOption(103, Util.nextInt(18, 25)));
            ct.itemOptions.add(new ItemOption(93, Util.nextInt(3, 15)));
            InventoryService.gI().addItemBag(pl, ct);
            Service.gI().sendThongBao(pl, "Dùng " + item.template.name + " nhận được Cải trang Halloween " + ct.template.name + " (+1 điểm sự kiện)!");
        }
        pl.point_sukien += 1;
        pl.event.addEventPoint(1);
        Manager.isTopSukienChanged = true;
        InventoryService.gI().sendItemBags(pl);
    }

    public void useKeoGiangSinh(Player pl, Item item) {
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        int roll = Util.nextInt(1, 100);
        if (roll <= 45) {
            int gold = Util.nextInt(1_000_000, 5_000_000);
            pl.inventory.addGold(gold);
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Thưởng thức Kẹo Giáng Sinh nhận được " + Util.formatNumber(gold) + " Vàng (+1 điểm sự kiện)!");
        } else if (roll <= 80) {
            int gem = Util.nextInt(5, 15);
            pl.inventory.gem += gem;
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Thưởng thức Kẹo Giáng Sinh nhận được " + gem + " Ngọc Xanh (+1 điểm sự kiện)!");
        } else {
            int ruby = Util.nextInt(5, 15);
            pl.inventory.ruby += ruby;
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Thưởng thức Kẹo Giáng Sinh nhận được " + ruby + " Hồng Ngọc (+1 điểm sự kiện)!");
        }
        pl.point_sukien += 1;
        pl.event.addEventPoint(1);
        Manager.isTopSukienChanged = true;
        InventoryService.gI().sendItemBags(pl);
    }

    public void useTatVoGiangSinh(Player pl, Item item) {
        if (item.quantity >= 10) {
            if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
                Service.gI().sendThongBao(pl, "Cần ít nhất 1 ô trống trong hành trang!");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 10);
            Item hopQua = ItemService.gI().createNewItem((short) 648, 1);
            InventoryService.gI().addItemBag(pl, hopQua);
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Đã ghép 10 Tất, vớ giáng sinh thành 1 Hộp quà giáng sinh!");
            return;
        }
        Item hopQuaNoel = InventoryService.gI().findItemBag(pl, 648);
        if (hopQuaNoel != null && hopQuaNoel.quantity >= 1) {
            NoelItemBox(pl, hopQuaNoel);
            return;
        }
        Service.gI().sendThongBao(pl, "Giữ Tất, vớ giáng sinh trong túi để nhặt Hộp quà giáng sinh dưới đất, hoặc gom đủ 10 Tất vớ bấm Sử dụng để ghép 1 Hộp quà!");
    }

    public void useBaoLiXi(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000);

        int roll = Util.nextInt(1, 100);
        if (roll <= 35) {
            int gold = Util.nextInt(2_000_000, 10_000_000);
            pl.inventory.addGold(gold);
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Mở Bao Lì Xì nhận được " + Util.formatNumber(gold) + " Vàng!");
        } else if (roll <= 65) {
            int gem = Util.nextInt(10, 50);
            pl.inventory.gem += gem;
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Mở Bao Lì Xì nhận được " + gem + " Ngọc Xanh!");
        } else if (roll <= 85) {
            int ruby = Util.nextInt(5, 25);
            pl.inventory.ruby += ruby;
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Mở Bao Lì Xì nhận được " + ruby + " Hồng Ngọc!");
        } else {
            short[] tetRewards = {457, 1635, 1150, 1151, 1152, 1153, 1154, 674};
            short rwId = tetRewards[Util.nextInt(0, tetRewards.length - 1)];
            Item rw = ItemService.gI().createNewItem(rwId, 1);
            InventoryService.gI().addItemBag(pl, rw);
            Service.gI().sendThongBao(pl, "Mở Bao Lì Xì nhận được " + rw.template.name + "!");
        }
        pl.point_sukien += 1;
        pl.event.addEventPoint(1);
        Manager.isTopSukienChanged = true;
        InventoryService.gI().sendItemBags(pl);
    }

    public void useBongHoa(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }
        if (item.quantity >= 10) {
            InventoryService.gI().subQuantityItemsBag(pl, item, 10);
            int roll = Util.nextInt(1, 100);
            if (roll <= 30) {
                int gem = Util.nextInt(20, 60);
                pl.inventory.gem += gem;
                PlayerService.gI().sendInfoHpMpMoney(pl);
                Service.gI().sendThongBao(pl, "Kết bó 10 Bông hoa nhận được " + gem + " Ngọc Xanh!");
            } else if (roll <= 55) {
                int ruby = Util.nextInt(10, 30);
                pl.inventory.ruby += ruby;
                PlayerService.gI().sendInfoHpMpMoney(pl);
                Service.gI().sendThongBao(pl, "Kết bó 10 Bông hoa nhận được " + ruby + " Hồng Ngọc!");
            } else if (roll <= 85) {
                short[] rewards = {1635, 1150, 1151, 1152, 1153, 1776};
                short rwId = rewards[Util.nextInt(0, rewards.length - 1)];
                Item rw = ItemService.gI().createNewItem(rwId, 1);
                InventoryService.gI().addItemBag(pl, rw);
                Service.gI().sendThongBao(pl, "Kết bó 10 Bông hoa nhận được " + rw.template.name + "!");
            } else {
                Item rw = ItemService.gI().createNewItem((short) 1777, 1);
                InventoryService.gI().addItemBag(pl, rw);
                Service.gI().sendThongBao(pl, "Kết bó 10 Bông hoa nhận được " + rw.template.name + "!");
            }
            pl.point_sukien += 1;
            pl.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;
        } else {
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            int gold = Util.nextInt(1_000_000, 3_000_000);
            pl.inventory.addGold(gold);
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().sendThongBao(pl, "Bạn nhận được " + Util.formatNumber(gold) + " Vàng! (Gom đủ 10 Bông hoa khi dùng sẽ nhận quà lớn hơn)");
        }
        InventoryService.gI().sendItemBags(pl);
    }

    public void useBanhChungBanhDay(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }
        boolean isBanhChung = item.template.id == 1556;
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        int[] possibleItems = isBanhChung
                ? new int[]{1150, 1151, 1152, 1153, 1635, 1154, 457, 674}
                : new int[]{381, 382, 383, 384, 385, 1635};
        short rwId = (short) possibleItems[Util.nextInt(0, possibleItems.length - 1)];
        Item rw = ItemService.gI().createNewItem(rwId, 1);
        InventoryService.gI().addItemBag(pl, rw);
        pl.point_sukien += 1;
        pl.event.addEventPoint(1);
        Manager.isTopSukienChanged = true;
        InventoryService.gI().sendItemBags(pl);
        Service.gI().sendThongBao(pl, "Thưởng thức " + item.template.name + " nhận được " + rw.template.name + "!");
    }

    public void useNguyenLieuNauBanh(Player pl, Item itemUsed) {
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }
        int usedId = itemUsed != null && itemUsed.template != null ? itemUsed.template.id : 1546;
        boolean preferBanhDay = (usedId == 1544 || usedId == 1545 || usedId == 1547);

        Item comnep = InventoryService.gI().findItemBag(pl, 1546);
        Item dauxanh = InventoryService.gI().findItemBag(pl, 1548);
        Item thittuoi = InventoryService.gI().findItemBag(pl, 1549);
        Item botgao = InventoryService.gI().findItemBag(pl, 1547);
        Item muoitieu = InventoryService.gI().findItemBag(pl, 1545);
        Item chalua = InventoryService.gI().findItemBag(pl, 1544);

        boolean canBanhChung = comnep != null && comnep.quantity >= 10
                && dauxanh != null && dauxanh.quantity >= 10
                && thittuoi != null && thittuoi.quantity >= 10;
        boolean canBanhDay = comnep != null && comnep.quantity >= 10
                && botgao != null && botgao.quantity >= 5
                && muoitieu != null && muoitieu.quantity >= 5
                && chalua != null && chalua.quantity >= 1;

        if (preferBanhDay && canBanhDay) {
            InventoryService.gI().subQuantityItemsBag(pl, comnep, 10);
            InventoryService.gI().subQuantityItemsBag(pl, botgao, 5);
            InventoryService.gI().subQuantityItemsBag(pl, muoitieu, 5);
            InventoryService.gI().subQuantityItemsBag(pl, chalua, 1);
            Item banhDay = ItemService.gI().createNewItem((short) 1542, 1);
            banhDay.itemOptions.add(new ItemOption(93, 30));
            InventoryService.gI().addItemBag(pl, banhDay);
            pl.point_sukien += 1;
            pl.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Đã nấu thành công 1 Bánh Dầy (+1 điểm sự kiện)! Bấm Sử dụng Bánh Dầy hoặc dâng Vua Hùng để nhận quà.");
            return;
        }
        if (canBanhChung) {
            InventoryService.gI().subQuantityItemsBag(pl, comnep, 10);
            InventoryService.gI().subQuantityItemsBag(pl, dauxanh, 10);
            InventoryService.gI().subQuantityItemsBag(pl, thittuoi, 10);
            Item banhChung = ItemService.gI().createNewItem((short) 1556, 1);
            banhChung.itemOptions.add(new ItemOption(93, 30));
            InventoryService.gI().addItemBag(pl, banhChung);
            pl.point_sukien += 1;
            pl.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Đã nấu thành công 1 Bánh Chưng Lang Liêu (+1 điểm sự kiện)! Bấm Sử dụng Bánh Chưng hoặc dâng Vua Hùng để nhận quà.");
            return;
        }
        if (canBanhDay) {
            InventoryService.gI().subQuantityItemsBag(pl, comnep, 10);
            InventoryService.gI().subQuantityItemsBag(pl, botgao, 5);
            InventoryService.gI().subQuantityItemsBag(pl, muoitieu, 5);
            InventoryService.gI().subQuantityItemsBag(pl, chalua, 1);
            Item banhDay = ItemService.gI().createNewItem((short) 1542, 1);
            banhDay.itemOptions.add(new ItemOption(93, 30));
            InventoryService.gI().addItemBag(pl, banhDay);
            pl.point_sukien += 1;
            pl.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Đã nấu thành công 1 Bánh Dầy (+1 điểm sự kiện)! Bấm Sử dụng Bánh Dầy hoặc dâng Vua Hùng để nhận quà.");
            return;
        }
        Service.gI().sendThongBao(pl, "Nấu Bánh Chưng cần: 10 Cơm nếp + 10 Đậu xanh + 10 Thịt tươi.\nNấu Bánh Dầy cần: 10 Cơm nếp + 5 Bột gạo + 5 Muối tiêu + 1 Chả lụa.");
    }

    public void useNguyenLieuNuocMia(Player pl) {
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }
        Item khucMia = InventoryService.gI().findItemBag(pl, 1612);
        Item nuocDa = InventoryService.gI().findItemBag(pl, 1613);
        if (khucMia != null && khucMia.quantity >= 5 && nuocDa != null && nuocDa.quantity >= 2 && pl.inventory.gold >= 5_000_000) {
            InventoryService.gI().subQuantityItemsBag(pl, khucMia, 5);
            InventoryService.gI().subQuantityItemsBag(pl, nuocDa, 2);
            pl.inventory.gold -= 5_000_000;
            short[] waterItems = {1614, 1615, 1616};
            Item itemRandom = ItemService.gI().createNewItem(waterItems[Util.nextInt(0, waterItems.length - 1)], 1);
            InventoryService.gI().addItemBag(pl, itemRandom);
            pl.point_sukien1 += 1;
            pl.event.addEventPoint(1);
            Manager.isTopSukien1Changed = true;
            Service.gI().sendMoney(pl);
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Đã ép thành công 1 " + itemRandom.template.name + " (+1 điểm sự kiện Nước Mía)!");
        } else {
            Service.gI().sendThongBao(pl, "Cần 5 Khúc mía + 2 Nước đá + 5.000.000 Vàng để ép 1 Ly Nước Mía (hoặc gặp Xe Nước Mía)!");
        }
    }

    public void useDuaHauTem(Player pl) {
        Item duaHau = InventoryService.gI().findItemBag(pl, 569);
        Item tem = InventoryService.gI().findItemBag(pl, 1558);
        if (duaHau != null && duaHau.quantity >= 1 && tem != null && tem.quantity >= 1) {
            InventoryService.gI().subQuantityItemsBag(pl, duaHau, 1);
            InventoryService.gI().subQuantityItemsBag(pl, tem, 1);
            int gem = Util.nextInt(10, 25);
            pl.inventory.gem += gem;
            pl.point_sukien += 1;
            pl.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;
            PlayerService.gI().sendInfoHpMpMoney(pl);
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Đổi 1 Dưa Hấu + 1 Tem nhận được " + gem + " Ngọc Xanh!");
        } else {
            Service.gI().sendThongBao(pl, "Cần 1 Dưa Hấu + 1 Tem để đổi Ngọc Xanh (hoặc gặp Vua Hùng tại Làng Aru)!");
        }
    }

    public void useSinhLeHungVuong(Player pl) {
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }
        Item ngaVoi = InventoryService.gI().findItemBag(pl, 1220);
        Item cuaGa = InventoryService.gI().findItemBag(pl, 1221);
        Item hongMao = InventoryService.gI().findItemBag(pl, 1222);
        if (ngaVoi != null && ngaVoi.quantity >= 9
                && cuaGa != null && cuaGa.quantity >= 9
                && hongMao != null && hongMao.quantity >= 9
                && pl.inventory.gold >= 1_000_000) {
            InventoryService.gI().subQuantityItemsBag(pl, ngaVoi, 9);
            InventoryService.gI().subQuantityItemsBag(pl, cuaGa, 9);
            InventoryService.gI().subQuantityItemsBag(pl, hongMao, 9);
            pl.inventory.gold -= 1_000_000;
            short boxId = (short) (Util.isTrue(30, 100) ? 1777 : 1776);
            Item hopQua = ItemService.gI().createNewItem(boxId, 1);
            InventoryService.gI().addItemBag(pl, hopQua);
            pl.point_sukien += 1;
            pl.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;
            Service.gI().sendMoney(pl);
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Dâng sính lễ thành công! Nhận được 1 " + hopQua.template.name + "!");
        } else {
            Service.gI().sendThongBao(pl, "Cần đủ bộ Sính Lễ: 9 Ngà Voi + 9 Cựa Gà + 9 Hồng Mao + 1.000.000 Vàng để đổi Hộp Quà Hùng Vương!");
        }
    }

    public void NoelItemBox(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int spl = Util.nextInt(441, 445);
            int dnc = Util.nextInt(381, 384);
            int nr = Util.nextInt(17, 20);
            int nrBang = Util.nextInt(925, 931);
            int mts = Util.nextInt(1066, 1070);
            int ctNoel = Util.nextInt(387, 393);

            pl.point_sukien += 1;
            pl.event.addEventPoint(1);
            Manager.isTopSukienChanged = true;

            if (Util.isTrue(15, 100)) {
                int gem = Util.nextInt(10, 25);
                pl.inventory.gem += gem;
                PlayerService.gI().sendInfoHpMpMoney(pl);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().sendItemBags(pl);
                Service.gI().sendThongBao(pl, "Mở Hộp quà Giáng Sinh nhận được " + gem + " Ngọc Xanh (+1 điểm sự kiện)!");
            } else {
                int[] temp = {spl, dnc, nr, nrBang, mts, 533, 380, ctNoel, 936, 822, 746};
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                short[] icon = new short[2];
                icon[0] = item.template.iconID;
                Item it = ItemService.gI().createNewItem((short) temp[index]);
                /*
                # Item Vặt
                 */
                if (temp[index] >= 441 && temp[index] <= 443) {// sao pha le
                    it.itemOptions.add(new ItemOption(temp[index] - 346, 5));
                    it.quantity = Util.nextInt(1, 5);
                } else if (temp[index] >= 444 && temp[index] <= 445) {
                    it.itemOptions.add(new ItemOption(temp[index] - 346, 3));
                    it.quantity = Util.nextInt(1, 5);
                } else if (temp[index] >= 381 && temp[index] <= 384) { // da nang cap
                    it.quantity = Util.nextInt(1, 5);
                } else if (temp[index] >= 1066 && temp[index] <= 1070) { // da nang cap
                    it.quantity = Util.nextInt(1, 5);

                    /*
                   # Item Cải Trang
                     */
                } else if (temp[index] >= 387 && temp[index] <= 393) { // mu noel do
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(30, 40)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(30, 40)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(30, 40)));
                    it.itemOptions.add(new ItemOption(80, Util.nextInt(10, 20)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                    it.itemOptions.add(new ItemOption(199, 0));
                    /*
                    # Item Pet
                  # Nếu Muốn Random Item Thì:  } else if (temp[index] == 936 && temp[index] == 936) {
                     */
                } else if (temp[index] == 936) { // tuan loc
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(5, 10)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(5, 10)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(5, 10)));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(3, 30)));
                    /*
                    # Item Flag
                    # Nếu Muốn Random Item Thì:  } else if (temp[index] == 822 && temp[index] == 822) {
                     */
                } else if (temp[index] == 822) { // cay thong noel
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(10, 20)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(10, 20)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(10, 20)));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(3, 30)));
                    it.itemOptions.add(new ItemOption(30, 0));
                    it.itemOptions.add(new ItemOption(74, 0));
                    /*
                      # Item VanBay
                      # Nếu Muốn Random Item Thì:  } else if (temp[index] == 746 && temp[index] == 746) {
                     */
                } else if (temp[index] == 746) { // xe truot tuyet
                    it.itemOptions.add(new ItemOption(74, 0));
                    it.itemOptions.add(new ItemOption(30, 0));
                    if (Util.isTrue(99, 100)) {
                        it.itemOptions.add(new ItemOption(93, Util.nextInt(30, 360)));
                    }

                } else if (temp[index] == 821) {
                    it.itemOptions.add(new ItemOption(30, 0));
                } else {
                    it.itemOptions.add(new ItemOption(73, 0));
                }
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                icon[1] = it.template.iconID;
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBags(pl);
                Service.gI().sendThongBao(pl, "Mở Hộp quà Giáng Sinh nhận được " + it.template.name + " x" + it.quantity + " (+1 điểm sự kiện)!");
            }
        } else {
            Service.gI().sendThongBao(pl, "Hành trang đã đầy!");
        }
    }

    public void HopQuaChinhChu(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int tst = Util.nextInt(637, 642);
            int itc1 = Util.nextInt(1150, 1154);

            {
                int[] temp = {tst, itc1};
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                short[] icon = new short[2];
                icon[0] = item.template.iconID;
                Item it = ItemService.gI().createNewItem((short) temp[index]);

                /*
                   # Item Cải Trang
                 */
                if (temp[index] == 1503 && temp[index] == 1504) { //oy tiu nun
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(19, 25)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(17, 25)));
                    it.itemOptions.add(new ItemOption(47, Util.nextInt(13, 12)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));

                } else if (temp[index] == 681) { // tuan loc
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(19, 25)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(17, 25)));
                    it.itemOptions.add(new ItemOption(47, Util.nextInt(13, 12)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                icon[1] = it.template.iconID;
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBags(pl);
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void HopQuaNheNhang(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int tst = Util.nextInt(381, 385);
            int itc1 = Util.nextInt(628, 636);

            {
                int[] temp = {tst, itc1};
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                short[] icon = new short[2];
                icon[0] = item.template.iconID;
                Item it = ItemService.gI().createNewItem((short) temp[index]);

                /*
                   # Item Cải Trang
                 */
                if (temp[index] == 1503 && temp[index] == 1504) { //oy tiu nun
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(19, 25)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(17, 25)));
                    it.itemOptions.add(new ItemOption(47, Util.nextInt(13, 12)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));

                } else if (temp[index] == 681) { // tuan loc
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(19, 25)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(17, 25)));
                    it.itemOptions.add(new ItemOption(47, Util.nextInt(13, 12)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                icon[1] = it.template.iconID;
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBags(pl);
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void BlackGokuItemBoxEventNoel(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int hanhtinh = pl.gender;
            int[] caitrangdietquy = {1087, 1088, 1089, 1090, 1091}; // cải trang diệt wỹ

            int itemBlackGoku = Util.nextInt(0, 3) == 0 ? 746
                    : // 1/3%
                    Util.nextInt(0, 3) == 1 ? (hanhtinh == 0 ? 1155 : hanhtinh == 1 ? 1157 : 1156)
                    : // Cải trang Noel
                    Util.nextInt(0, 3) == 2 ? (hanhtinh == 0 ? 1018 : hanhtinh == 1 ? 1019 : 1020)
                    : // Cải trang Broly
                    caitrangdietquy[Util.nextInt(0, caitrangdietquy.length - 1)]; // Cải Trang Diệt Wỹ

            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            if (itemBlackGoku == 746) { // Xe trượt tuyết
                if (Util.isTrue(1, 100)) { // 1% ra HSD vĩnh viễn
                    it.itemOptions.add(new ItemOption(73, 0)); // Option 73 với giá trị 0: HSD vĩnh viễn
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30))); // 99% ra HSD 7-30 ngày
                }
            } else if (itemBlackGoku == 1155 || itemBlackGoku == 1156 || itemBlackGoku == 1157
                    || itemBlackGoku == 1018 || itemBlackGoku == 1019 || itemBlackGoku == 1020) { // Cải trang Noel hoặc Broly
                it.itemOptions.add(new ItemOption(50, 23));
                it.itemOptions.add(new ItemOption(77, 23));
                it.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3))); // 90% ra option 93
                } else {
                    it.itemOptions.add(new ItemOption(73, 0)); // 10% ra option 73
                }
            } else if (itemBlackGoku >= 1087 && itemBlackGoku <= 1191) { // Diệt Quỷ
                it.itemOptions.add(new ItemOption(50, 22));
                it.itemOptions.add(new ItemOption(77, 21));
                it.itemOptions.add(new ItemOption(103, 21));
                if (Util.isTrue(95, 100)) {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3))); // 90% ra HSD (1-3 ngày)
                } else {
                    it.itemOptions.add(new ItemOption(73, 0)); // 5% không ra HSD
                }
            }

            // Cập nhật túi đồ
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // send
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void C5(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int[] itemList = {1087, 1088, 1089, 1090, 1091};

            int itemBlackGoku = itemList[Util.nextInt(0, itemList.length - 1)];
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 16))); //
            it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 17))); // 
            it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 15))); // 
            it.itemOptions.add(new ItemOption(95, Util.nextInt(1, 5))); //
            it.itemOptions.add(new ItemOption(96, Util.nextInt(1, 5))); // 

            int[] options = {94, 97, 108};
            int randomOption = options[Util.nextInt(0, options.length - 1)];
            it.itemOptions.add(new ItemOption(randomOption, Util.nextInt(3, 5)));
            if (Util.isTrue(98, 100)) {
                it.itemOptions.add(new ItemOption(93, 5));
            }
            // Cập nhật túi đồ
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // Gửi cập nhật túi đồ
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void C7(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int[] itemList = {1087, 1088, 1089, 1090, 1091};

            int itemBlackGoku = itemList[Util.nextInt(0, itemList.length - 1)];
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 16))); //
            it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 17))); // 
            it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 15))); // 
            it.itemOptions.add(new ItemOption(95, Util.nextInt(1, 5))); //
            it.itemOptions.add(new ItemOption(96, Util.nextInt(1, 5))); // 

            int[] options = {94, 97, 108};
            int randomOption = options[Util.nextInt(0, options.length - 1)];
            it.itemOptions.add(new ItemOption(randomOption, Util.nextInt(3, 5)));
            if (Util.isTrue(100, 100)) {
                it.itemOptions.add(new ItemOption(93, 7));
            }
            // Cập nhật túi đồ
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // Gửi cập nhật túi đồ
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void ChuLunBox(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            // Mảng vật phẩm không giới hạn hành tinh
            int[] itemList = {1158, 1159, 1160, 1161, 1162, 1163, 1164};

            // Chọn ngẫu nhiên một vật phẩm từ danh sách itemList
            int itemBlackGoku = itemList[Util.nextInt(0, itemList.length - 1)];

            // Tạo vật phẩm mới
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);

            // Thêm các item options cố định
            it.itemOptions.add(new ItemOption(50, 11)); // Option 50 = 11
            it.itemOptions.add(new ItemOption(77, 13)); // Option 77 = 13
            it.itemOptions.add(new ItemOption(103, 13)); // Option 103 = 13        

            int[] options = {94, 97, 108}; // Mảng chứa các option
            int randomOption = options[Util.nextInt(0, options.length - 1)]; // Chọn ngẫu nhiên một option từ mảng
            it.itemOptions.add(new ItemOption(randomOption, Util.nextInt(3, 5))); // Thêm option ngẫu nhiên
            if (Util.isTrue(98, 100)) {
                it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3))); // 90% ra HSD (1-3 ngày)
            } else {
                it.itemOptions.add(new ItemOption(73, 0)); // 5% không ra HSD
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // Gửi cập nhật túi đồ
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void NonNoelDo(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int hanhtinh = pl.gender;

            // Chỉ giữ lại cải trang Noel
            int itemBlackGoku = (hanhtinh == 0 ? 387 : hanhtinh == 1 ? 390 : 393);

            // Tạo vật phẩm
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            it.itemOptions.add(new ItemOption(50, Util.nextInt(10, 20)));
            it.itemOptions.add(new ItemOption(80, Util.nextInt(30, 50)));
            it.itemOptions.add(new ItemOption(103, Util.nextInt(10, 20)));
            it.itemOptions.add(new ItemOption(106, 1));

            // 95% ra HSD 1-3 ngày, 5% ra vĩnh viễn
            if (Util.isTrue(95, 100)) {
                it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3))); // 95% ra option 93
            } else {
                it.itemOptions.add(new ItemOption(73, 0)); // 5% ra option 73
            }
            // Cập nhật túi đồ
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // send
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void CapsuleKichHoat(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Hãy chọn một món quà", "Áo", "Quần", "Găng", "Giày", "Rada");
    }

    private void MapRiengTu(Player pl, Item item) {
        ChangeMapService.gI().changeMapBySpaceShip(pl, 164, -1, 870);
    }

    private void VeTangNgoc(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, 900, -1, "Nhập tên người chơi mà bạn muốn tặng ngọc", "Tặng", "Từ chối");
    }

    public void LoaTheGioi(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, 901, -1, "Nhập Nội Dung Muốn Chát", "Ok", "Từ chối");
    }

    public void TuiVang(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            pl.inventory.gold += Util.nextInt(100000, 10000000);
            int itemBlackGoku = 190;
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            int randomValue = Util.nextInt(3, 333);
            it.itemOptions.add(new ItemOption(1, randomValue));
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().addItemBag(pl, it);
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Happy");
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void ItemSieuThanThuy(Player pl, Item item) {
        long tnsm = 5_000_000;
        int n = 0;
        switch (item.template.id) {
            case 727:
                n = 2;
                break;
            case 728:
                n = 10;
                break;
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        if (Util.isTrue(90, 100)) {
            Service.gI().sendThongBao(pl, "Bạn đã bị chết vì độc của thuốc tăng lực siêu thần thủy.");
            pl.setDie();
        } else {
            for (int i = 0; i < n; i++) {
                Service.gI().addSMTN(pl, (byte) 2, tnsm, true);
            }
        }
    }

    public void UseCard(Player pl, Item item) {
        RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(c -> c.Id == item.template.id).findFirst().orElse(null);
        if (radarTemplate == null) {
            return;
        }
        if (radarTemplate.Require != -1) {
            RadarCard radarRequireTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(r -> r.Id == radarTemplate.Require).findFirst().orElse(null);
            if (radarRequireTemplate == null) {
                return;
            }
            Card cardRequire = pl.Cards.stream().filter(r -> r.Id == radarRequireTemplate.Id).findFirst().orElse(null);
            if (cardRequire == null || cardRequire.Level < radarTemplate.RequireLevel) {
                Service.gI().sendThongBao(pl, "Bạn cần sưu tầm " + radarRequireTemplate.Name + " ở cấp độ " + radarTemplate.RequireLevel + " mới có thể sử dụng thẻ này");
                return;
            }
        }
        Card card = pl.Cards.stream().filter(r -> r.Id == item.template.id).findFirst().orElse(null);
        if (card == null) {
            Card newCard = new Card(item.template.id, (byte) 1, radarTemplate.Max, (byte) -1, radarTemplate.Options);
            pl.Cards.add(newCard);
            RadarService.gI().RadarSetAmount(pl, newCard.Id, newCard.Amount, newCard.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, newCard.Id, newCard.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);
        } else {
            if (card.Level >= 2) {
                Service.gI().sendThongBao(pl, "Thẻ này đã đạt cấp tối đa");
                return;
            }
            card.Amount++;
            if (card.Amount >= card.MaxAmount) {
                card.Amount = 0;
                if (card.Level == -1) {
                    card.Level = 1;
                } else {
                    card.Level++;
                }
                Service.gI().point(pl);
            }
            RadarService.gI().RadarSetAmount(pl, card.Id, card.Amount, card.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, card.Id, card.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);
        }
    }

    private void RuongNgocRong(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            // Số ngẫu nhiên từ 0 đến 100 để quyết định tỷ lệ
            int random = Util.nextInt(0, 100);

            int itemBlackGoku;

            // 85% xuất hiện item 20, 19, 18, 17
            if (random < 85) {
                int[] itemList = {20, 19, 18, 17};  // Các item có tỉ lệ xuất hiện 85%
                itemBlackGoku = itemList[Util.nextInt(0, itemList.length - 1)];
            } // 10% xuất hiện item 16
            else if (random < 95) {
                itemBlackGoku = 16;  // Item 16 có tỉ lệ 10%
            } // 5% xuất hiện item 14 hoặc 15
            else {
                itemBlackGoku = Util.nextInt(14, 15);  // Item 14 hoặc 15 có tỉ lệ 5%
            }

            // Tạo vật phẩm mới từ ID đã chọn
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);

            // Kiểm tra nếu người chơi có item 1561 (chìa khóa)
            Item item1561 = InventoryService.gI().findItem(pl.inventory.itemsBag, 1561);
            if (item1561 != null) {
                // Trừ vật phẩm và thêm vật phẩm mới vào túi đồ
                InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
                InventoryService.gI().subQuantityItemsBag(pl, item1561, 1); // Trừ 1 Item Box
                InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
                InventoryService.gI().sendItemBags(pl); // Gửi cập nhật túi đồ
                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Bạn không có chìa khoá vàng");
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

}
