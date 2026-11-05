package nro.models.shop;

import nro.models.consts.ConstAchievement;
import nro.models.item.Item;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;
import nro.models.network.Message;
import nro.models.item.Item.ItemOption;
import java.util.ArrayList;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import java.util.List;
import nro.models.npc.MagicTree;
import nro.models.player_badges.BadgesData;
import nro.models.player_badges.BadgesService;
import nro.models.player_badges.BagesTemplate;
import nro.models.services.AchievementService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_func.Input;
import nro.models.services_func.BuyBackService;
import nro.models.services_func.UseItem;
import nro.models.map.service.NpcService;
import nro.models.task.BadgesTaskService;
import nro.models.utils.SkillUtil;
import nro.models.utils.TimeUtil;

/**
 *
 * @author By Mr Blue
 *
 */
public class ShopService {

    private static final byte COST_GOLD = 0;
    private static final byte COST_GEM = 1;
    private static final byte COST_RUBY = 3;
    private static final byte COST_COUPON = 4;

    private static final byte NORMAL_SHOP = 0;
    private static final byte SPEC_SHOP = 3;
    private static final byte KINANG_SHOP = 1;
    private int eventPointPrice;

    private static ShopService I;

    public static ShopService gI() {
        if (ShopService.I == null) {
            ShopService.I = new ShopService();
        }
        return ShopService.I;
    }

    public void opendShop(Player player, String tagName, boolean allGender) {
        if (tagName.equals("ITEMS_LUCKY_ROUND")) {
            openShopType4(player, tagName, player.inventory.itemsBoxCrackBall);
            return;
        } else if (tagName.equals("ITEMS_DABAN")) {
            openShopType8(player, tagName, player.inventory.itemsDaBan);
            return;
        }
        try {
            Shop shop = this.getShop(tagName);
            for (TabShop tabShop : shop.tabShops) {
                for (ItemShop item : tabShop.itemShops) {
                    switch (item.temp.id) {
                        case 379:// máy dò capsule kỳ bí
                            if ("BUNMA_FUTURE".equals(tagName)) {
                                item.cost = 50_000_000;
                                item.typeSell = COST_GOLD;
                            }
                            break;
                        case 1627:// hành trang
                            if (player.inventory.itemsBag.size() >= 35) {
                                item.cost = ((player.inventory.itemsBag.size() - 35) + 1) * 2;
                            } else {
                                item.cost = 1;
                            }
                            break;
                    }
                }
            }
            shop = this.resolveShop(player, shop, allGender);
            switch (shop.typeShop) {
                case KINANG_SHOP:
                    openShopType1(player, shop);
                    break;
                case NORMAL_SHOP:
                    openShopType0(player, shop);
                    break;
                case SPEC_SHOP:
                    openShopType3(player, shop);
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Service.gI().sendThongBao(player, ex.getMessage());
        }
    }

    private Shop getShop(String tagName) throws Exception {
        for (Shop s : Manager.SHOPS) {
            if (s.tagName != null && s.tagName.equals(tagName)) {
                return s;
            }
        }
        throw new Exception("Shop " + tagName + " không tồn tại!");
    }

    private Shop resolveShop(Player player, Shop shop, boolean allGender) {
        if (shop.tagName != null && (shop.tagName.equals("BUA_1H") || shop.tagName.equals("BUA_8H") || shop.tagName.equals("BUA_1M"))) {
            return this.resolveShopBua(player, new Shop(shop));
        }
        return allGender ? new Shop(shop) : new Shop(shop, player);
    }

    private Shop resolveShopBua(Player player, Shop s) {
        for (TabShop tabShop : s.tabShops) {
            for (ItemShop item : tabShop.itemShops) {
                long min = 0;
                switch (item.temp.id) {
                    case 213:
                        long timeTriTue = player.charms.tdTriTue;
                        long current = System.currentTimeMillis();
                        min = (timeTriTue - current) / 60000;

                        break;
                    case 214:
                        min = (player.charms.tdManhMe - System.currentTimeMillis()) / 60000;
                        break;
                    case 215:
                        min = (player.charms.tdDaTrau - System.currentTimeMillis()) / 60000;
                        break;
                    case 216:
                        min = (player.charms.tdOaiHung - System.currentTimeMillis()) / 60000;
                        break;
                    case 217:
                        min = (player.charms.tdBatTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 218:
                        min = (player.charms.tdDeoDai - System.currentTimeMillis()) / 60000;
                        break;
                    case 219:
                        min = (player.charms.tdThuHut - System.currentTimeMillis()) / 60000;
                        break;
                    case 522:
                        min = (player.charms.tdDeTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 671:
                        min = (player.charms.tdTriTue3 - System.currentTimeMillis()) / 60000;
                        break;
                    case 672:
                        min = (player.charms.tdTriTue4 - System.currentTimeMillis()) / 60000;
                        break;
                }
                if (min > 0) {
                    item.options.clear();
                    if (min >= 1440) {
                        item.options.add(new Item.ItemOption(63, (int) min / 1440));
                    } else if (min >= 60) {
                        item.options.add(new Item.ItemOption(64, (int) min / 60));
                    } else {
                        item.options.add(new Item.ItemOption(65, (int) min));
                    }
                }
            }
        }
        return s;
    }

    private void openShopType0(Player player, Shop shop) {
        if (shop != null) {
            player.idMark.setShopOpen(shop);
            player.idMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(NORMAL_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        if (itemShop.typeSell == COST_GOLD) {
                            msg.writer().writeInt(itemShop.cost);
                            msg.writer().writeInt(0);
                        } else if (itemShop.typeSell == COST_GEM) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_RUBY) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_COUPON) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        }
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopDanhHieu(Player player, Shop shop) {
        if (shop != null) {
            player.idMark.setShopOpen(shop);
            player.idMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(NORMAL_SHOP);
                msg.writer().writeByte(2);
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        if (itemShop.typeSell == COST_GOLD) {
                            msg.writer().writeInt(itemShop.cost);
                            msg.writer().writeInt(0);
                        } else if (itemShop.typeSell == COST_GEM) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_RUBY) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_COUPON) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        }
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }

                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType1(Player player, Shop shop) {
        if (shop != null) {
            player.idMark.setShopOpen(shop);
            player.idMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(KINANG_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        String[] subName = itemShop.temp.name.split("");
                        byte level = Byte.parseByte(subName[subName.length - 1]);

                        var skillTemplateId = SkillUtil.getTempSkillSkillByItemID(itemShop.temp.id);
                        var costPotential = SkillUtil.findSkillTemplate(skillTemplateId).skillss.stream()
                                .filter(s -> s.point == level)
                                .findFirst()
                                .map(s -> (int) s.powRequire) // Ép kiểu Long -> int
                                .orElse(0); // Giá trị mặc định là int
                        msg.writer().writeLong(costPotential);

                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);

                        msg.writer().writeByte(0);

                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType3(Player player, Shop shop) {
        player.idMark.setShopOpen(shop);
        player.idMark.setTagNameShop(shop.tagName);
        if (shop != null) {
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(SPEC_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        msg.writer().writeShort(itemShop.iconSpec);
                        msg.writer().writeInt(itemShop.cost);
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType4(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.idMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(4);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Phần\nthưởng");
            int size4 = Math.min(items.size(), 120);
            msg.writer().writeByte(size4);
            for (int i = 0; i < size4; i++) {
                Item item = items.get(i);
                msg.writer().writeShort(item.template.id);
                msg.writer().writeUTF("|7| LUCKY REWARD");
                msg.writer().writeByte(item.itemOptions.size() + 1);
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                //số lượng
                msg.writer().writeByte(31);
                msg.writer().writeShort(item.quantity);
                //
                msg.writer().writeByte(1);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void openShopType8(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.idMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(8);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Mua lại");
            int size4 = Math.min(items.size(), 120);
            msg.writer().writeByte(size4);
            for (int i = 0; i < size4; i++) {
                Item item = items.get(i);
                int giamualaingoc = item.template.gem / 2;
                int giamualaivang = giamualaingoc == 0 ? (int) item.template.gold / 2 > 0 ? (int) item.template.gold / 2 : item.quantity * 100 : 0;
                msg.writer().writeShort(item.template.id);
                msg.writer().writeInt(giamualaivang);
                msg.writer().writeInt(giamualaingoc);
                msg.writer().writeInt(item.quantity);
                msg.writer().writeByte(item.itemOptions.size());
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                msg.writer().writeByte(0);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void takeItem(Player player, byte type, int tempId) {
        String tagName = player.idMark.getTagNameShop();
        if (tagName == null || tagName.length() <= 0) {
            return;
        }
        if (tagName.equals("ITEMS_LUCKY_ROUND")) {
            getItemSideBoxLuckyRound(player, player.inventory.itemsBoxCrackBall, type, tempId);
            return;
        } else if (tagName.equals("ITEMS_REWARD")) {
            return;
        } else if (tagName.equals("ITEMS_DABAN")) {
            buyItemDaBan(player, player.inventory.itemsDaBan, type, tempId);
            return;
        } else if (tagName.equals("BILL")) {
            buyItemHD(player, tempId);
            return;
        }

        if (player.idMark.getShopOpen() == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (tagName.equals("BUA_1H") || tagName.equals("BUA_8H") || tagName.equals("BUA_1M")) {
            buyItemBua(player, tempId);
        } else if (tagName.equals("SANTA_HEAD")) {
            Item itS = ItemService.gI().createNewItem((short) tempId);
            player.head = (short) itS.template.head;
            Service.gI().Send_Caitrang(player);
            Service.gI().point(player);
            Service.gI().sendThongBao(player, "Đổi kiểu tóc thành công");
        } else {
            buyItem(player, tempId);
        }
        Service.gI().sendMoney(player);
    }

    private boolean subMoneyByItemShop(Player player, ItemShop is) {
        int gold = 0;
        int gem = 0;
        int ruby = 0;
        int coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD ->
                gold = is.cost;
            case COST_GEM ->
                gem = is.cost;
            case COST_RUBY ->
                ruby = is.cost;
            case COST_COUPON ->
                coupon = is.cost;
        }
        Item pGG = ruby > 0 ? InventoryService.gI().findItem(player.inventory.itemsBag, 459) : null;
        boolean useCoupon50 = (pGG != null && pGG.isNotNullItem() && pGG.quantity >= 1);
        int originalRuby = ruby;
        if (useCoupon50) {
            ruby = Math.max(1, ruby / 2);
        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng");
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc xanh");
            return false;
        } else if (player.inventory.ruby < ruby) {
            if (useCoupon50) {
                Service.gI().sendThongBao(player, "Bạn không có đủ Hồng ngọc (Đã giảm 50% bằng Phiếu giảm giá còn " + ruby + " Hồng ngọc, hiện có " + player.inventory.ruby + ")");
            } else {
                Service.gI().sendThongBao(player, "Bạn không có đủ Hồng ngọc (cần " + ruby + " Hồng ngọc, hiện có " + player.inventory.ruby + ")");
            }
            return false;
        } else if (player.inventory.coupon < coupon) {
            Service.gI().sendThongBao(player, "Bạn không có đủ điểm");
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= gem;
        player.inventory.ruby -= ruby;
        player.inventory.coupon -= coupon;
        if (useCoupon50) {
            InventoryService.gI().subQuantityItemsBag(player, pGG, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Đã dùng 1 Phiếu giảm giá (-50%): chỉ tốn " + ruby + " Hồng ngọc (Giá gốc: " + originalRuby + ")");
        }
        Service.gI().sendMoney(player);
        return true;
    }

    private boolean subMoneyByItemShopV2(Player player, ItemShop is) {
        int gold = 0;
        int gem = 0;
        int ruby = 0;
        int coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD ->
                gold = is.cost;
            case COST_GEM ->
                gem = is.cost;
            case COST_RUBY ->
                ruby = is.cost;
            case COST_COUPON ->
                coupon = is.cost;
        }
        Item pGG = ruby > 0 ? InventoryService.gI().findItem(player.inventory.itemsBag, 459) : null;
        boolean useCoupon50 = (pGG != null && pGG.isNotNullItem() && pGG.quantity >= 1);
        int originalRuby = ruby;
        if (useCoupon50) {
            ruby = Math.max(1, ruby / 2);
        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(gold - player.inventory.gold));
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ ngọc, còn thiếu " + Util.numberToMoney(gem - player.inventory.gem));
            return false;
        } else if (player.inventory.ruby < ruby) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ Hồng ngọc, còn thiếu " + Util.numberToMoney(ruby - player.inventory.ruby));
            return false;
        } else if (player.inventory.coupon < coupon) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ điểm, còn thiếu " + Util.numberToMoney(coupon - player.inventory.coupon));
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= gem;
        player.inventory.ruby -= ruby;
        player.inventory.coupon -= coupon;
        if (useCoupon50) {
            InventoryService.gI().subQuantityItemsBag(player, pGG, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Đã dùng 1 Phiếu giảm giá (-50%): chỉ tốn " + ruby + " Hồng ngọc (Giá gốc: " + originalRuby + ")");
        }
        Service.gI().sendMoney(player);
        return true;
    }

    /**
     * Mua bùa
     *
     * @param player người chơi
     * @param itemTempId id template vật phẩm
     */
    private void buyItemBua(Player player, int itemTempId) {
        Shop shop = player.idMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (!subMoneyByItemShop(player, is)) {
            return;
        }
        InventoryService.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
        InventoryService.gI().sendItemBags(player);
        opendShop(player, shop.tagName, true);
    }

    /**
     * Mua vật phẩm trong cửa hàng
     *
     * @param player người chơi
     * @param itemTempId id template vật phẩm
     */
    private void learnKyNang(Player pl, ItemShop is) {

        if (pl.nPoint.power < is.temp.strRequire) {
            Service.gI().sendThongBao(pl, "Sức mạnh của bạn không đủ");
            return;
        }
        if (pl.nPoint.tiemNang < is.cost) {
            Service.gI().sendThongBao(pl, "Bạn không đủ tiềm năng để học chiêu thức này");
            return;
        }
        var skillPlayer = pl.playerSkill.getSkillbyId(SkillUtil.getSkillByItemID(pl, is.temp.id).template.id);
        String[] subName = is.temp.name.split("");
        byte level = Byte.parseByte(subName[subName.length - 1]);
        if (skillPlayer != null) {

            if (skillPlayer.point >= level) {
                Service.gI().sendThongBao(pl, "Bạn đã học kỹ năng này rồi");
                return;

            }
            if (level - skillPlayer.point != 1) {
                Service.gI().sendThongBao(pl, "Bạn chưa thể học kỹ năng này");
                return;
            }
        }
        if (pl.BoughtSkill.contains(is.temp.id)) {
            Service.gI().sendThongBao(pl, "Bạn đã học kỹ năng này rồi");
            return;
        }
        ArrayList<String> menu = new ArrayList<>();
        menu.add("Yes");
        menu.add("No");
        String[] menus = menu.toArray(String[]::new);
        long[] time = new long[]{900000, 1800000, 3600000, 86400000, 259200000, 604800000, 1296000000};
        var timeStudy = "";
        var timeLong = time[level - 1];
        switch (level) {
            case 0:
            case 1:
            case 2:
                timeStudy = TimeUtil.convertMillisecondToMinute(timeLong);
                break;
            case 3:
                timeStudy = TimeUtil.convertMillisecondToHour(timeLong);
                break;
            default:
                timeStudy = TimeUtil.convertMillisecondToDay(timeLong);
                break;
        }
        var skillTemplateId = SkillUtil.getTempSkillSkillByItemID(is.temp.id);

        var potential = SkillUtil.findSkillTemplate(skillTemplateId).skillss.stream()
                .filter(s -> s.point == level)
                .findFirst()
                .map(s -> (int) s.powRequire) // Ép kiểu Long -> int
                .orElse(0); // Giá trị mặc định là int
        String text = "Con có muốn học kỹ năng " + SkillUtil.findSkillTemplate(SkillUtil.getTempSkillSkillByItemID(is.temp.id)).name + " cấp " + level + "\nCần " + potential + " điểm tiềm năng và thời gian học là " + timeStudy;
        pl.LearnSkill.ItemTemplateSkillId = is.temp.id;
        pl.LearnSkill.Time = -1;
        pl.LearnSkill.Potential = potential;

        NpcService.gI().createMenuConMeo(pl, 671, NpcService.gI().getAvatar(13 + pl.gender), text, menus);
    }

    public void buyItem(Player player, int itemTempId) {
        Shop shop = player.idMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        int[][] listDauThan = {{13, 293}, {60, 294}, {61, 295}, {62, 296}, {63, 297}, {64, 298}, {65, 299}, {352, 596}, {523, 597}};

        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }

        // Mua bằng phiếu giảm giá (-50% Hồng Ngọc)
        if (is.tabShop.id == 30) {
            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            if (pGG != null && pGG.isNotNullItem() && pGG.quantity >= 1) {
                int originalRuby = is.cost > 0 ? is.cost : 1000;
                int discountedRuby = Math.max(1, originalRuby / 2);
                if (player.inventory.ruby < discountedRuby) {
                    Service.gI().sendThongBao(player, "Bạn không đủ Hồng ngọc (Đã giảm 50% còn " + discountedRuby + " Hồng ngọc, hiện có " + player.inventory.ruby + ")");
                    return;
                }
                player.inventory.ruby -= discountedRuby;
                Item item = ItemService.gI().createItemFromItemShop(is);
                InventoryService.gI().subQuantityItemsBag(player, pGG, 1);
                InventoryService.gI().addItemBag(player, item);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name + " (-" + discountedRuby + " Hồng ngọc, đã giảm 50%)" + formatRolledStatsSummary(item));
            } else {
                Service.gI().sendThongBao(player, "Bạn không có Phiếu giảm giá!");
            }
            return;
        }

        // Đổi danh hiệu
        if (is.tabShop.id == 44) {
            buyDanhHieu(player, is);
            return;
        }

        // Đổi danh hiệu khác
        if (is.tabShop.id == 45) {
            changeDanhHieu(player, is);
            return;
        }

        // Shop chipi
        if (is.tabShop.id == 49) {
            Item item = ItemService.gI().createItemFromItemShop(is);
            if (Util.isTrue(5, 100)) {
                item.itemOptions.add(new ItemOption(73, 0)); // HSD vĩnh viễn
            } else {
                item.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7))); // HSD 3-7 ngày
            }
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            return;
        }

        // Shop kỹ năng
        if (shop.typeShop == ShopService.KINANG_SHOP) {
            learnKyNang(player, is);
            return;
        }

        // Kiểm tra giới hạn mở rộng hành trang / rương đồ trước khi trừ tiền
        if (itemTempId == 517) {
            if (player.inventory.itemsBag.size() >= Inventory.MAX_ITEMS_BAG) {
                Service.gI().sendThongBaoOK(player, "Hành trang của bạn đã đạt tối đa (" + Inventory.MAX_ITEMS_BAG + " ô / 5 Tab)!");
                return;
            }
        } else if (itemTempId == 518) {
            if (player.inventory.itemsBox.size() >= Inventory.MAX_ITEMS_BOX) {
                Service.gI().sendThongBaoOK(player, "Rương đồ của bạn đã đạt tối đa (" + Inventory.MAX_ITEMS_BOX + " ô)!");
                return;
            }
        } else if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            // Hành trang đầy (bỏ qua nếu mua mở rộng hành trang 517 hoặc rương đồ 518)
            Service.gI().sendThongBao(player, "Hành trang đã đầy");
            return;
        }

        // Kiểm tra auto train
        if (itemTempId == 1524 || itemTempId == 1523 || itemTempId == 521) {
            if (!checkAutoTrainPurchase(player, itemTempId)) {
                return;
            }
        }

        // Đổi đeo lưng bằng điểm sự kiện (Hạn sử dụng 5 ngày)
        if (is.tabShop.id == 59 || "SHOP_DOI_DIEM".equals(shop.tagName)) {
            int eventPointPrice = is.cost > 0 ? is.cost : 200;

            if (player.event.getEventPoint() < eventPointPrice) {
                Service.gI().sendThongBao(player, "Không đủ điểm sự kiện! Cần " + eventPointPrice
                        + " điểm (Hiện có: " + player.event.getEventPoint() + " điểm)");
                return;
            }

            player.event.subEventPoint(eventPointPrice);
            Item item = ItemService.gI().createItemFromItemShop(is);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Đã đổi " + is.temp.name + " (HSD 5 ngày, -" + eventPointPrice
                    + " điểm SK)" + formatRolledStatsSummary(item));
            return;
        }
        // Mua đeo lưng Vĩnh Viễn bằng Hồng Ngọc
        if (is.tabShop.id == 590 || "SHOP_DEO_LUNG_RUBY".equals(shop.tagName)) {
            if (!subMoneyByItemShop(player, is)) {
                return;
            }
            Item item = ItemService.gI().createItemFromItemShop(is);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Đã mua " + is.temp.name + " Vĩnh Viễn (-" + is.cost
                    + " Hồng ngọc)" + formatRolledStatsSummary(item));
            return;
        }
        // Đổi bằng điểm Capsule Bang
        if (is.tabShop.id == 60 || is.tabShop.id == 61 || is.tabShop.id == 62) {
            int capsuleClanPointPrice = 0;

            switch (is.temp.id) {
                case 1794:
                case 1204:
                case 1423:
                case 1438:
                case 1439:
                case 987:
                case 1635:
                    capsuleClanPointPrice = 1;
                    break;
                case 1790:
                    capsuleClanPointPrice = 2;
                    break;
                case 1791:
                    capsuleClanPointPrice = 3;
                    break;
                case 1792:
                    capsuleClanPointPrice = 4;
                    break;
                case 1634:
                    capsuleClanPointPrice = 10;
                    break;
                case 1620:
                case 1748:
                case 1750:
                case 1729:
                case 1727:
                case 1714:
                case 1683:
                case 1682:
                case 1668:
                case 1629:
                case 1630:
                case 1631:
                case 1573:
                case 1550:
                case 1551:
                case 1541:
                case 1563:
                case 1724:
                case 1733:
                case 1734:
                case 1749:
                    capsuleClanPointPrice = 50;
                    break;
                default:
                    capsuleClanPointPrice = -1;
                    break;
            }

            if (capsuleClanPointPrice <= 0) {
                Service.gI().sendThongBao(player, "Vật phẩm này không thể mua bằng điểm Capsule Bang.");
                return;
            }

            if (player.clan == null) {
                Service.gI().sendThongBao(player, "Bạn không có trong bang hội!");
                return;
            }

            if (player.clan.capsuleClan < capsuleClanPointPrice) {
                Service.gI().sendThongBao(player, "Bang hội không đủ điểm Capsule Bang để mua vật phẩm này!");
                return;
            }

            player.clan.capsuleClan -= capsuleClanPointPrice;

            Item item = ItemService.gI().createItemFromItemShop(is);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);

            Service.gI().sendThongBao(player, "Đã đổi " + is.temp.name + " bằng " + capsuleClanPointPrice + " điểm Capsule Bang của bang hội.");
            return;
        }

        // Nếu vật phẩm hỗ trợ cộng dồn (nguyên liệu, đá, sao pha lê, item hỗ trợ...) -> mở bảng nhập số lượng mua
        if (isEligibleForBulkBuy(shop, is)) {
            player.buyItemShopTempId = itemTempId;
            Input.gI().createFormBuyItemShop(player, is.temp.name, getPriceDescription(shop, is));
            return;
        }

        // Shop thường
        if (shop.typeShop == ShopService.NORMAL_SHOP) {
            if (!subMoneyByItemShop(player, is)) {
                return;
            }
        } // Shop đặc biệt
        else if (shop.typeShop == ShopService.SPEC_SHOP) {
            if ("SANTA".equals(shop.tagName) || "SANTA_CAITRANG".equals(shop.tagName)) {
                if (!subMoneyByItemShop(player, is)) {
                    return;
                }
            } else if (!this.subIemByItemShop(player, is)) {
                return;
            }
        }

        // Tạo item và xử lý đặc biệt
        Item item = ItemService.gI().createItemFromItemShop(is);
        item = buyMagicPean(player, listDauThan, item);
        if (item.template.id == 1523 || item.template.id == 1524) {
            item = ItemService.gI().createNewItem((short) 521);
            item.itemOptions.addAll(is.options);
        } else if (item.template.id == 380 && is.cost == 50_000_000) {
            item = ItemService.gI().createNewItem((short) 380, 99);
            item.itemOptions.add(new ItemOption(30, 0));
        }
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBags(player);
        String extraRollMsg = "";
        boolean isRubyBuy = is.typeSell == COST_RUBY
                || (is.iconSpec > 0 && ItemService.gI().getItemIdByIcon((short) is.iconSpec) == 861)
                || (is.tabShop != null && (is.tabShop.id == 18 || is.tabShop.id == 1801 || is.tabShop.id == 590))
                || (item.template != null && (item.template.type == 5 || item.template.type == 11));
        if (isRubyBuy) {
            extraRollMsg = formatRolledStatsSummary(item);
        } else if (item.itemOptions != null) {
            for (ItemOption io : item.itemOptions) {
                if (io.optionTemplate != null) {
                    if (io.optionTemplate.id == 159) {
                        extraRollMsg = " (Ngẫu nhiên: +" + io.param + "% ST chưởng)";
                        break;
                    } else if (io.optionTemplate.id == 153) {
                        extraRollMsg = " (Ngẫu nhiên: +" + io.param + "% ST phát nổ)";
                        break;
                    } else if (io.optionTemplate.id == 66) {
                        extraRollMsg = " (Ngẫu nhiên: +" + io.param + "% ST Laze)";
                        break;
                    }
                }
            }
        }
        Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name + extraRollMsg);

        if (itemTempId == 1523 || itemTempId == 1524 || itemTempId == 521) {
            updateAutoTrainPurchase(player, itemTempId);
            Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name);
        }
    }

    public boolean isEligibleForBulkBuy(Shop shop, ItemShop is) {
        if (shop == null || is == null || is.temp == null) {
            return false;
        }
        if (shop.typeShop != NORMAL_SHOP && shop.typeShop != SPEC_SHOP) {
            return false;
        }
        String tag = shop.tagName != null ? shop.tagName : "";
        if ("SHOP_DOI_DIEM".equals(tag) || "SHOP_DEO_LUNG_RUBY".equals(tag)
                || "BILL".equals(tag) || "SANTA_HEAD".equals(tag)
                || "BUA_1H".equals(tag) || "BUA_8H".equals(tag) || "BUA_1M".equals(tag)) {
            return false;
        }
        if (is.tabShop != null) {
            int tabId = is.tabShop.id;
            if (tabId == 30 || tabId == 44 || tabId == 45 || tabId == 49
                    || tabId == 59 || tabId == 590 || tabId == 60 || tabId == 61 || tabId == 62) {
                return false;
            }
        }
        int id = is.temp.id;
        // Các vật phẩm mua 1 lần / giới hạn / đặc biệt
        if (id == 517 || id == 518 || id == 1523 || id == 1524 || id == 521
                || id == 453 || id == 74 || id == 454 || id == 921 || id == 1819
                || id == 193 || id == 194 || id == 361 || id == 379 || id == 992 || id == 570
                || (id == 380 && is.cost == 50_000_000)
                || (id >= 293 && id <= 299) || id == 596 || id == 597) {
            return false;
        }
        int type = is.temp.type;
        // Loại trừ toàn bộ trang bị, cải trang, đeo lưng, bùa, pet, thú cưỡi, giáp tập luyện, sách, danh hiệu, linh thú
        switch (type) {
            case 0, 1, 2, 3, 4, 5, 11, 13, 21, 23, 24, 25, 32, 35, 36, 72 -> {
                return false;
            }
        }
        // Nếu vật phẩm có các dòng chỉ số chiến đấu hoặc HSD ngày (như Linh thú type 27, vật phẩm random chỉ số) -> mua 1 món
        if (is.options != null) {
            for (ItemOption io : is.options) {
                if (io != null && io.optionTemplate != null) {
                    int opId = io.optionTemplate.id;
                    if (opId == 50 || opId == 77 || opId == 103 || opId == 0 || opId == 6 || opId == 7
                            || opId == 14 || opId == 22 || opId == 23 || opId == 27 || opId == 28 || opId == 47
                            || opId == 5 || opId == 94 || opId == 108 || opId == 159 || opId == 153 || opId == 66
                            || opId == 93 || opId == 210) {
                        return false;
                    }
                    if (type != 30 && (opId >= 95 && opId <= 101)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String getPriceDescription(Shop shop, ItemShop is) {
        if (shop == null || is == null) {
            return "";
        }
        if (shop.typeShop == NORMAL_SHOP || "SANTA".equals(shop.tagName) || "SANTA_CAITRANG".equals(shop.tagName)) {
            return switch (is.typeSell) {
                case COST_GOLD -> Util.numberToMoney(is.cost) + " vàng";
                case COST_GEM -> Util.numberToMoney(is.cost) + " ngọc xanh";
                case COST_RUBY -> Util.numberToMoney(is.cost) + " Hồng ngọc";
                case COST_COUPON -> Util.numberToMoney(is.cost) + " điểm";
                default -> Util.numberToMoney(is.cost);
            };
        }
        if (shop.typeShop == SPEC_SHOP) {
            short itSpec = ItemService.gI().getItemIdByIcon((short) is.iconSpec);
            Item itS = ItemService.gI().createNewItem(itSpec);
            if (itS != null && itS.template != null) {
                return switch (itS.template.id) {
                    case 76, 188, 189, 190 -> Util.numberToMoney(is.cost) + " vàng";
                    case 77 -> Util.numberToMoney(is.cost) + " ngọc xanh";
                    case 861 -> Util.numberToMoney(is.cost) + " Hồng ngọc";
                    default -> Util.numberToMoney(is.cost) + " " + itS.template.name;
                };
            }
        }
        return Util.numberToMoney(is.cost);
    }

    public void buyItemWithQuantity(Player player, int itemTempId, int quantity) {
        if (player == null || quantity <= 0 || quantity > 999) {
            return;
        }
        Shop shop = player.idMark.getShopOpen();
        if (shop == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null || is.temp == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy");
            return;
        }

        if (shop.typeShop == ShopService.NORMAL_SHOP) {
            if (!subMoneyByItemShop(player, is, quantity)) {
                return;
            }
        } else if (shop.typeShop == ShopService.SPEC_SHOP) {
            if ("SANTA".equals(shop.tagName) || "SANTA_CAITRANG".equals(shop.tagName)) {
                if (!subMoneyByItemShop(player, is, quantity)) {
                    return;
                }
            } else if (!this.subIemByItemShop(player, is, quantity)) {
                return;
            }
        } else {
            return;
        }

        Item item = ItemService.gI().createItemFromItemShop(is);
        int totalAdded = quantity;
        boolean hasIncrementalOpt = false;
        if (item.itemOptions != null) {
            for (ItemOption io : item.itemOptions) {
                if (io != null && io.optionTemplate != null && (io.optionTemplate.id == 31 || io.optionTemplate.id == 1)) {
                    int baseParam = Math.max(1, io.param);
                    totalAdded = baseParam * quantity;
                    io.param = totalAdded;
                    item.quantity = (io.optionTemplate.id == 31) ? 1 : item.quantity;
                    hasIncrementalOpt = true;
                    break;
                }
            }
        }
        if (!hasIncrementalOpt) {
            int baseQty = Math.max(1, item.quantity);
            totalAdded = baseQty * quantity;
            item.quantity = totalAdded;
        }

        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Mua thành công x" + totalAdded + " " + is.temp.name);
    }

    private boolean subMoneyByItemShop(Player player, ItemShop is, int quantity) {
        long gold = 0;
        long gem = 0;
        long ruby = 0;
        long coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD -> gold = (long) is.cost * quantity;
            case COST_GEM -> gem = (long) is.cost * quantity;
            case COST_RUBY -> ruby = (long) is.cost * quantity;
            case COST_COUPON -> coupon = (long) is.cost * quantity;
        }
        Item pGG = ruby > 0 ? InventoryService.gI().findItem(player.inventory.itemsBag, 459) : null;
        int discountCount = (pGG != null && pGG.isNotNullItem() && pGG.quantity > 0) ? Math.min(quantity, pGG.quantity) : 0;
        long originalRuby = ruby;
        if (discountCount > 0) {
            long discountedUnit = Math.max(1, is.cost / 2);
            ruby = (long) discountCount * discountedUnit + (long) (quantity - discountCount) * is.cost;
        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng (cần " + Util.numberToMoney(gold) + " vàng)");
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc xanh (cần " + Util.numberToMoney(gem) + " ngọc xanh)");
            return false;
        } else if (player.inventory.ruby < ruby) {
            if (discountCount > 0) {
                Service.gI().sendThongBao(player, "Bạn không có đủ Hồng ngọc (Đã dùng " + discountCount + " Phiếu giảm giá 50% còn " + ruby + " Hồng ngọc, hiện có " + player.inventory.ruby + ")");
            } else {
                Service.gI().sendThongBao(player, "Bạn không có đủ Hồng ngọc (cần " + ruby + " Hồng ngọc, hiện có " + player.inventory.ruby + ")");
            }
            return false;
        } else if (player.inventory.coupon < coupon) {
            Service.gI().sendThongBao(player, "Bạn không có đủ điểm (cần " + coupon + " điểm)");
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= (int) gem;
        player.inventory.ruby -= (int) ruby;
        player.inventory.coupon -= (int) coupon;
        if (discountCount > 0) {
            InventoryService.gI().subQuantityItemsBag(player, pGG, discountCount);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Đã dùng " + discountCount + " Phiếu giảm giá (-50%): chỉ tốn " + ruby + " Hồng ngọc (Giá gốc: " + originalRuby + ")");
        }
        Service.gI().sendMoney(player);
        return true;
    }

    private boolean subIemByItemShop(Player pl, ItemShop itemShop, int quantity) {
        short itSpec = ItemService.gI().getItemIdByIcon((short) itemShop.iconSpec);
        long buySpec = (long) itemShop.cost * quantity;
        Item itS = ItemService.gI().createNewItem(itSpec);
        switch (itS.template.id) {
            case 76:
            case 188:
            case 189:
            case 190:
                if (pl.inventory.gold >= buySpec) {
                    pl.inventory.gold -= buySpec;
                    Service.gI().sendMoney(pl);
                    return true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn không đủ vàng để mua vật phẩm (cần " + Util.numberToMoney(buySpec) + " vàng)");
                    return false;
                }
            case 77:
                if (pl.inventory.gem >= buySpec) {
                    pl.inventory.gem -= (int) buySpec;
                    Service.gI().sendMoney(pl);
                    return true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn không đủ ngọc để mua vật phẩm (cần " + Util.numberToMoney(buySpec) + " ngọc)");
                    return false;
                }
            case 861: {
                Item pGG = InventoryService.gI().findItem(pl.inventory.itemsBag, 459);
                int discountCount = (pGG != null && pGG.isNotNullItem() && pGG.quantity > 0) ? Math.min(quantity, pGG.quantity) : 0;
                long originalRuby = buySpec;
                if (discountCount > 0) {
                    long discountedUnit = Math.max(1, itemShop.cost / 2);
                    buySpec = (long) discountCount * discountedUnit + (long) (quantity - discountCount) * itemShop.cost;
                }
                if (pl.inventory.ruby >= buySpec) {
                    pl.inventory.ruby -= (int) buySpec;
                    if (discountCount > 0) {
                        InventoryService.gI().subQuantityItemsBag(pl, pGG, discountCount);
                        InventoryService.gI().sendItemBags(pl);
                        Service.gI().sendThongBao(pl, "Đã dùng " + discountCount + " Phiếu giảm giá (-50%): chỉ tốn " + buySpec + " Hồng ngọc (Giá gốc: " + originalRuby + ")");
                    }
                    Service.gI().sendMoney(pl);
                    return true;
                } else {
                    if (discountCount > 0) {
                        Service.gI().sendThongBao(pl, "Bạn không có đủ Hồng ngọc (Đã giảm 50% còn " + buySpec + " Hồng ngọc, hiện có " + pl.inventory.ruby + ")");
                    } else {
                        Service.gI().sendThongBao(pl, "Bạn không có đủ Hồng ngọc (cần " + buySpec + " Hồng ngọc, hiện có " + pl.inventory.ruby + ")");
                    }
                    return false;
                }
            }
            default: {
                long totalHave = 0;
                for (Item bagItem : pl.inventory.itemsBag) {
                    if (bagItem != null && bagItem.isNotNullItem() && bagItem.template.id == itSpec) {
                        totalHave += bagItem.quantity;
                    }
                }
                if (totalHave <= 0) {
                    Service.gI().sendThongBao(pl, "Không tìm thấy " + itS.template.name);
                    return false;
                }
                if (totalHave < buySpec) {
                    Service.gI().sendThongBao(pl, "Bạn không có đủ " + buySpec + " " + itS.template.name + " (hiện có " + totalHave + ")");
                    return false;
                }
                int remaining = (int) buySpec;
                for (Item bagItem : new ArrayList<>(pl.inventory.itemsBag)) {
                    if (remaining <= 0) {
                        break;
                    }
                    if (bagItem != null && bagItem.isNotNullItem() && bagItem.template.id == itSpec) {
                        int take = Math.min(remaining, bagItem.quantity);
                        InventoryService.gI().subQuantityItemsBag(pl, bagItem, take);
                        remaining -= take;
                    }
                }
                return true;
            }
        }
    }

    private String formatRolledStatsSummary(Item item) {
        if (item == null || item.itemOptions == null || item.itemOptions.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ItemOption io : item.itemOptions) {
            if (io == null || io.optionTemplate == null) {
                continue;
            }
            int id = io.optionTemplate.id;
            int p = io.param;
            switch (id) {
                case 50 -> sb.append("SĐ +").append(p).append("%, ");
                case 77 -> sb.append("HP +").append(p).append("%, ");
                case 103 -> sb.append("KI +").append(p).append("%, ");
                case 159 -> sb.append("ST Chưởng +").append(p).append("%, ");
                case 153 -> sb.append("ST Nổ +").append(p).append("%, ");
                case 66 -> sb.append("ST Laze +").append(p).append("%, ");
                case 94 -> sb.append("Giáp +").append(p).append("%, ");
                case 95 -> sb.append("Hút HP +").append(p).append("%, ");
                case 96 -> sb.append("Hút KI +").append(p).append("%, ");
                case 98, 99 -> sb.append("XG +").append(p).append("%, ");
                case 101 -> sb.append("TNSM +").append(p).append("%, ");
                case 108 -> sb.append("Né +").append(p).append("%, ");
                case 14 -> sb.append("CM +").append(p).append("%, ");
                case 5 -> sb.append("SĐCM +").append(p).append("%, ");
            }
        }
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
            return " [Random: " + sb.toString() + "]";
        }
        return "";
    }

    private boolean checkAutoTrainPurchase(Player player, int itemTempId) {
        int autoTrainState = player.autoTrainState;
        if (itemTempId == 1524 && autoTrainState != 2) {
            Service.gI().sendThongBao(player, "Bạn cần mua Tự động luyện tập 2 trước!");
            return false;
        } else if (itemTempId == 1523 && autoTrainState != 1) {  // sửa lại thành 'else if'
            Service.gI().sendThongBao(player, "Bạn cần mua Tự động luyện tập 1 trước!");
            return false;
        }

        return true;
    }

    private void updateAutoTrainPurchase(Player player, int itemTempId) {

        if (itemTempId == 1524) {
            player.autoTrainState = 0;

        } // Nếu mua Tự động luyện tập 1 (với ID 521)
        else if (itemTempId == 521) {
            // Nếu player đã có autoTrainState = 2, không thay đổi trạng thái nữa
            if (player.autoTrainState != 2) {
                player.autoTrainState = 1;
            }
        } else if (itemTempId == 1523) {
            player.autoTrainState = 2;
        }
    }

    private void buyDanhHieu(Player pl, ItemShop is) {
        int idEffect = BagesTemplate.fineIdEffectbyIdItem(is.temp.id);
        int percent = BadgesTaskService.sendPercenBadgesTask(pl, idEffect);

        if (percent < 100) {
            Service.gI().sendThongBao(pl, "Bạn chưa mở khóa danh hiệu này");
            return;
        }

        for (BadgesData badge : pl.dataBadges) {
            if (badge.idBadGes == idEffect) {
                Service.gI().sendThongBao(pl, "Bạn đã sở hữu danh hiệu này rồi");
                return;
            }
        }

        BadgesData danhHieu = new BadgesData(pl, idEffect, 30);
        pl.dataBadges.add(danhHieu);

        BagesTemplate template = BagesTemplate.fineBadgesbyIdItem(is.temp.id);
        String badgeName = template != null ? template.NAME : "không rõ";

        Service.gI().sendThongBao(pl, "Bạn đã nhận được danh hiệu " + badgeName);
    }

    private void changeDanhHieu(Player pl, ItemShop is) {
        if (pl.lastTimeChangeBadges - System.currentTimeMillis() > 0) {
            Service.gI().sendThongBao(pl, "Vui lòng đợi " + (pl.lastTimeChangeBadges - System.currentTimeMillis()) / 1000 + " giây nữa");
            return;
        }
        if (pl.badges.idBadges == BagesTemplate.fineIdEffectbyIdItem(is.temp.id)) {
            Service.gI().sendThongBao(pl, "Danh hiệu đang được sữ dụng, hãy chọn danh hiệu khác");
            pl.lastTimeChangeBadges = System.currentTimeMillis() + 3000;
            return;
        }
        BadgesService.turnOnBadges(pl, BagesTemplate.fineIdEffectbyIdItem(is.temp.id));
        Service.gI().sendThongBao(pl, "Đã đổi danh hiệu sang " + is.temp.name);
        pl.lastTimeChangeBadges = System.currentTimeMillis() + 3000;
        pl.nPoint.calPoint();
        pl.nPoint.setHp((int) pl.nPoint.hpMax);
        pl.nPoint.setMp((int) pl.nPoint.mpMax);
        pl.nPoint.setDame((int) pl.nPoint.dame);
        Service.gI().point(pl);
        Service.gI().Send_Info_NV(pl);
    }

    private boolean subIemByItemShop(Player pl, ItemShop itemShop) {
        boolean isBuy = false;
        short itSpec = ItemService.gI().getItemIdByIcon((short) itemShop.iconSpec);
        int buySpec = itemShop.cost;
        Item itS = ItemService.gI().createNewItem(itSpec);
        switch (itS.template.id) {
            case 76:
            case 188:
            case 189:
            case 190:
                if (pl.inventory.gold >= buySpec) {
                    pl.inventory.gold -= buySpec;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không Đủ Vàng Để Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            case 77:
                if (pl.inventory.gem >= buySpec) {
                    pl.inventory.gem -= buySpec;
                    Service.gI().sendMoney(pl);
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không Đủ Ngọc Để Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            case 861: {
                Item pGG = InventoryService.gI().findItem(pl.inventory.itemsBag, 459);
                boolean useCoupon50 = (pGG != null && pGG.isNotNullItem() && pGG.quantity >= 1);
                int originalRuby = buySpec;
                if (useCoupon50) {
                    buySpec = Math.max(1, buySpec / 2);
                }
                if (pl.inventory.ruby >= buySpec) {
                    pl.inventory.ruby -= buySpec;
                    if (useCoupon50) {
                        InventoryService.gI().subQuantityItemsBag(pl, pGG, 1);
                        InventoryService.gI().sendItemBags(pl);
                        Service.gI().sendThongBao(pl, "Đã dùng 1 Phiếu giảm giá (-50%): chỉ tốn " + buySpec + " Hồng ngọc (Giá gốc: " + originalRuby + ")");
                    }
                    Service.gI().sendMoney(pl);
                    isBuy = true;
                } else {
                    if (useCoupon50) {
                        Service.gI().sendThongBao(pl, "Bạn không có đủ Hồng ngọc (Đã giảm 50% còn " + buySpec + " Hồng ngọc, hiện có " + pl.inventory.ruby + ")");
                    } else {
                        Service.gI().sendThongBao(pl, "Bạn không có đủ Hồng ngọc (cần " + buySpec + " Hồng ngọc, hiện có " + pl.inventory.ruby + ")");
                    }
                    isBuy = false;
                }
                break;
            }
            default:
                if (InventoryService.gI().findItemBag(pl, itSpec) == null || !InventoryService.gI().findItemBag(pl, itSpec).isNotNullItem()) {
                    Service.gI().sendThongBao(pl, "Không tìm thấy " + itS.template.name);
                    isBuy = false;
                } else if (InventoryService.gI().findItemBag(pl, itSpec).quantity < buySpec) {
                    Service.gI().sendThongBao(pl, "Bạn không có đủ " + buySpec + " " + itS.template.name);
                    isBuy = false;
                } else {
                    InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBag(pl, itSpec), buySpec);
                    isBuy = true;
                }
                break;
        }
        return isBuy;
    }

    public void showConfirmSellItem(Player pl, int where, int index) {
        Item item = null;
        if (where == 0) {
            if (index < 0) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }
            item = pl.inventory.itemsBody.get(index);
        } else {
            if (pl.getSession().version < 220) {
                index -= (pl.inventory.itemsBody.size() - 7);
            }
            int bagIndex = pl.inventory.resolveBagIndex(index);
            if (bagIndex < 0 || bagIndex >= pl.inventory.itemsBag.size()) {
                return;
            }
            item = pl.inventory.itemsBag.get(bagIndex);
        }
        if (item != null && item.isNotNullItem()) {
            if (InventoryService.isItemLocked(item)) {
                Service.gI().sendThongBao(pl, "Trang bị [" + item.template.name + "] đang được KHÓA bảo vệ! Hãy mở khóa trong Menu Tiện Ích nếu muốn bán.");
                return;
            }
            if (item.template.id == 570) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm này");
                return;
            }
            int quantity = item.quantity;
            int cost = item.template.gold;
            if (item.template.id == 457) {
                if (quantity >= 1) {
                    Input.gI().createFormBanSLL(pl);
                    return;
                }
                quantity = 1;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            String text = "Bạn có muốn bán\nx" + quantity
                    + " " + item.template.name + "\nvới giá là " + Util.numberToMoney(cost) + " vàng?";
            Message msg = null;
            try {
                msg = new Message(7);
                msg.writer().writeByte(where);
                msg.writer().writeShort(index);
                msg.writer().writeUTF(text);
                pl.sendMessage(msg);
            } catch (Exception e) {
            e.printStackTrace();
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    public void sellItem(Player pl, int where, int index) {
        if (pl.idMark.getShopOpen() == null || pl.idMark.getTagNameShop() == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        if (index < 0) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        Item item = null;
        if (where == 0) {
            item = pl.inventory.itemsBody.get(index);
        } else {
            int bagIndex = pl.inventory.resolveBagIndex(index);
            if (bagIndex < 0 || bagIndex >= pl.inventory.itemsBag.size()) {
                return;
            }
            item = pl.inventory.itemsBag.get(bagIndex);
        }
        if (item != null) {
            if (InventoryService.isItemLocked(item)) {
                Service.gI().sendThongBao(pl, "Trang bị [" + item.template.name + "] đang được KHÓA bảo vệ! Hãy mở khóa trong Menu Tiện Ích nếu muốn bán.");
                return;
            }
            if (item.template.id == 570) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm này");
                return;
            }
            if (InventoryService.gI().getParam(pl, 93, item.template.id) > 0) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm có hạn sử dụng");
                return;
            }
            int quantity = item.quantity;
            int cost = item.template.gold;
            if (item.template.id == 457) {
                quantity = 1;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            if (pl.inventory.gold + cost > Inventory.LIMIT_GOLD) {
                Service.gI().sendThongBao(pl, "Vàng sau khi bán vượt quá giới hạn");
                return;
            }
            pl.inventory.gold += cost;
            Service.gI().sendMoney(pl);
            Service.gI().sendThongBao(pl, "Đã bán " + item.template.name
                    + " thu được " + Util.numberToMoney(cost) + " vàng");

            //Add vật phẩm đã bán
            if (item.template.id != 457) {
                BuyBackService.gI().addItem(pl, item);
            }
            if (where == 0) {
                InventoryService.gI().subQuantityItemsBody(pl, item, quantity);
                InventoryService.gI().sendItemBody(pl);
                Service.gI().Send_Caitrang(pl);
            } else {
                InventoryService.gI().subQuantityItemsBag(pl, item, quantity);
                InventoryService.gI().sendItemBags(pl);
            }
            if ("BUNMA".equals(pl.idMark.getTagNameShop())
                    || "DENDE".equals(pl.idMark.getTagNameShop())
                    || "APPULE".equals(pl.idMark.getTagNameShop())) {
                AchievementService.gI().checkDoneTask(pl, ConstAchievement.TRUM_NHAT_VE_CHAI);
            }
        } else {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void getItemSideBoxLuckyRound(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        if (index < 0 || index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        switch (type) {
            case 0: //nhận
                if (item.isNotNullItem()) {
                    if (InventoryService.gI().getCountEmptyBag(player) != 0) {
                        InventoryService.gI().addItemBag(player, item);
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        InventoryService.gI().sendItemBags(player);
                        items.remove(index);
                    } else {
                        Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                }
                break;
            case 1: //xóa
                items.remove(index);
                Service.gI().sendThongBao(player, "Xóa vật phẩm thành công");
                break;
            case 2: //nhận hết
                for (int i = items.size() - 1; i >= 0; i--) {
                    item = items.get(i);
                    if (InventoryService.gI().addItemBag(player, item)) {
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        items.remove(i);
                    }
                }
                InventoryService.gI().sendItemBags(player);
                break;
        }
        openShopType4(player, player.idMark.getTagNameShop(), items);
    }

    private void buyItemDaBan(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        if (index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        int giamualaingoc = item.template.gem / 2;
        int giamualaivang = giamualaingoc == 0 ? (int) item.template.gold / 2 > 0 ? (int) item.template.gold / 2 : item.quantity * 100 : 0;
        if (giamualaivang > 0 && player.inventory.gold < giamualaivang) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng!");
            return;
        }
        if (giamualaingoc > 0 && player.inventory.gem < giamualaingoc) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc xanh!");
            return;
        }
        player.inventory.gem -= giamualaingoc;
        player.inventory.gold -= giamualaivang;
        Service.gI().sendMoney(player);
        if (item.isNotNullItem()) {
            if (InventoryService.gI().getCountEmptyBag(player) != 0) {
                InventoryService.gI().addItemBag(player, item);
                Service.gI().sendThongBao(player,
                        "Bạn nhận được " + (item.template.id == 189
                                ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                InventoryService.gI().sendItemBags(player);
                items.remove(index);
            } else {
                Service.gI().sendThongBao(player, "Hành trang đã đầy");
            }
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
        openShopType8(player, player.idMark.getTagNameShop(), items);
    }

    private void buyItemHD(Player player, int itemTempId) {
        Shop shop = player.idMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = ItemService.gI().createItemFromItemShop(is);
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy, không thể chứa thêm.");
            return;
        }
        if (!subMoneyByItemShopV2(player, is)) {
            return;
        }
        if (item.template.level == 14) {
            Item doAn = player.inventory.itemsBag.stream().filter(it -> it != null && it.template != null && (it.template.id == 663 || it.template.id == 664 || it.template.id == 665 || it.template.id == 666 || it.template.id == 667) && it.quantity >= 99).findFirst().orElse(null);
            if (doAn != null) {
                InventoryService.gI().subQuantityItemsBag(player, doAn, 99);
            } else {
                Service.gI().sendThongBao(player, "Không có đủ thức ăn");
                return;
            }
        }
        if (player.inventory.itemsBody.get(0) != null || player.inventory.itemsBody.get(1) != null || player.inventory.itemsBody.get(2) != null || player.inventory.itemsBody.get(3) != null || player.inventory.itemsBody.get(4) != null || player.inventory.itemsBody.get(5) != null) {
            Item dothan = player.inventory.itemsBody.stream().filter(it -> it != null && it.template != null && it.template.level == 13).findFirst().orElse(null);
            if (dothan == null) {
                Service.gI().sendThongBao(player, "Không có đủ set thần");
                return;
            }
        }
        int param = 0;
        if (item.template.level == 14) {
            int random = Util.nextInt(1, 100); // Số ngẫu nhiên từ 1 đến 100

            if (random <= 1) {
                param = 15; // 1%
            } else if (random <= 15) {
                param = Util.nextInt(11, 14); // 14% (2-15)
            } else if (random <= 35) {
                param = Util.nextInt(7, 10); // 20% (16-35)
            } else if (random <= 60) {
                param = Util.nextInt(4, 6); // 25% (36-55)
            } else {
                param = Util.nextInt(0, 3); // 40% còn lại
            }
        }

        List<ItemOption> itemoptions = new ArrayList<>();
        if (!item.itemOptions.isEmpty()) {
            for (ItemOption ios : item.itemOptions) {
                if (item.template.level == 14 && InventoryService.gI().optionCanUpgrade(ios.optionTemplate.id) && param > 0) {
                    int id = ios.optionTemplate.id;
                    int param1 = ios.param + (ios.param * param) / 100;
                    itemoptions.add(new ItemOption(id, param1));
                } else if (ios.optionTemplate.id != 164) {
                    itemoptions.add(new ItemOption(ios.optionTemplate.id, ios.param));
                }
            }
        } else {
            itemoptions.add(new ItemOption(73, (short) 0));
        }
        itemoptions.add(new ItemOption(30, (short) 0));
        item.itemOptions.clear();
        item.itemOptions.addAll(itemoptions);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBags(player);
        String extraRollMsg = "";
        if (item.itemOptions != null) {
            for (ItemOption io : item.itemOptions) {
                if (io.optionTemplate != null) {
                    if (io.optionTemplate.id == 159) {
                        extraRollMsg = " (Ngẫu nhiên: +" + io.param + "% ST chưởng)";
                        break;
                    } else if (io.optionTemplate.id == 153) {
                        extraRollMsg = " (Ngẫu nhiên: +" + io.param + "% ST phát nổ)";
                        break;
                    } else if (io.optionTemplate.id == 66) {
                        extraRollMsg = " (Ngẫu nhiên: +" + io.param + "% ST Laze)";
                        break;
                    }
                }
            }
        }
        Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name + extraRollMsg);
    }

    private Item buyMagicPean(Player player, int[][] listDauThan, Item item) {
        for (int i = 0; i < listDauThan.length; i++) {
            if (item.template.id == listDauThan[i][1]) {
                item = ItemService.gI().createNewItem((short) listDauThan[i][0]);
                item.itemOptions.add(new Item.ItemOption(player.magicTree.level - 1 > 1 ? 2 : 48, MagicTree.PEA_PARAM[player.magicTree.level - 1]));
                item.quantity = 30;
                return item;
            }
        }
        return item;
    }

}
