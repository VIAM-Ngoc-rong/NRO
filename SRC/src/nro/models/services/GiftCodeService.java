package nro.models.services;

import nro.models.managers.GiftCodeManager;
import nro.models.player_system.GiftCode;
import nro.models.item.Item;
import java.util.Set;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;

/**
 *
 * @author By Mr Blue
 * 
 */

public class GiftCodeService {

    private static GiftCodeService instance;

    public static GiftCodeService gI() {
        if (instance == null) {
            instance = new GiftCodeService();
        }
        return instance;
    }

    public void giftCode(Player player, String code) {
        GiftCode giftcode = GiftCodeManager.gI().checkUseGiftCode(player, code);
        if (giftcode == null) {
            return;
        }
        Set<Integer> keySet = giftcode.detail.keySet();
        String textGift = "|0|Chúc mừng! Bạn vừa nhận được quà từ Giftcode [" + giftcode.code + "]:\n";
        boolean moneyChanged = false;
        for (Integer key : keySet) {
            int idItem = key;
            int quantity = giftcode.detail.get(key);

            switch (idItem) {
                case -1 -> {
                    player.inventory.gold = Math.min(player.inventory.gold + (long) quantity, nro.models.player.Inventory.LIMIT_GOLD);
                    textGift += "|2|+" + nro.models.utils.Util.numberToMoney(quantity) + " Vàng\n";
                    moneyChanged = true;
                }
                case -2 -> {
                    player.inventory.gem = (int) Math.min((long) player.inventory.gem + quantity, 2_000_000_000L);
                    textGift += "|3|+" + nro.models.utils.Util.numberToMoney(quantity) + " Ngọc xanh\n";
                    moneyChanged = true;
                }
                case -3 -> {
                    player.inventory.ruby = (int) Math.min((long) player.inventory.ruby + quantity, 2_000_000_000L);
                    textGift += "|4|+" + nro.models.utils.Util.numberToMoney(quantity) + " Hồng ngọc\n";
                    moneyChanged = true;
                }
                case -4 -> {
                    if (player.getSession() != null) {
                        player.getSession().ruongSkh = Math.max(0, player.getSession().ruongSkh + quantity);
                        try (java.sql.Connection con = nro.models.data.LocalManager.getConnection();
                             java.sql.PreparedStatement ps = con.prepareStatement("UPDATE account SET ruong_skh = GREATEST(0, ruong_skh + ?) WHERE id = ?")) {
                            ps.setInt(1, quantity);
                            ps.setInt(2, player.getSession().userId);
                            ps.executeUpdate();
                        } catch (Exception ignored) {
                        }
                    }
                    textGift += "|7|+" + quantity + " Rương SKH Tân Thủ (Mở tại Ông Gôhan/Moori/Paragus)\n";
                }
                default -> {
                    Item itemGift = ItemService.gI().createNewItem((short) idItem, quantity);
                    if (itemGift != null && itemGift.isNotNullItem()) {
                        java.util.ArrayList<Item.ItemOption> opts = giftcode.option.get(key);
                        if (opts != null && !opts.isEmpty()) {
                            itemGift.itemOptions.clear();
                            for (Item.ItemOption opt : opts) {
                                if (opt != null && opt.optionTemplate != null) {
                                    itemGift.itemOptions.add(new Item.ItemOption(opt.optionTemplate.id, opt.param));
                                }
                            }
                        } else if (itemGift.itemOptions.isEmpty()) {
                            itemGift.itemOptions.add(new Item.ItemOption(73, 0));
                        }
                        InventoryService.gI().addItemBag(player, itemGift);
                        textGift += "|1|x" + quantity + " " + itemGift.template.name + "\n";
                    }
                }
            }
        }
        if (moneyChanged) {
            Service.gI().sendMoney(player);
        }
        InventoryService.gI().sendItemBags(player);
        NpcService.gI().createTutorial(player, 1139, textGift);
    }

}
