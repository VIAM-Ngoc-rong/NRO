package nro.models.shop_ky_gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.map.service.NpcService;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

/**
 *
 * @author By Mr Blue
 * 
 */

public class ConsignShopService {

    private static ConsignShopService instance;

    public static ConsignShopService gI() {
        if (instance == null) {
            instance = new ConsignShopService();
        }
        return instance;
    }

    private List<ConsignItem> getItemKyGui2(Player pl, byte tab, byte to, byte max) {
        List<ConsignItem> filtered = ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && it.tab == tab && !it.isBuy)
                .sorted(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed())
                .collect(Collectors.toList());

        List<ConsignItem> result = new ArrayList<>();
        for (int i = to; i <= max && i < filtered.size(); i++) {
            result.add(filtered.get(i));
        }
        return result;
    }

    private List<ConsignItem> getItemKyGui(Player pl, byte tab, byte... max) {
        List<ConsignItem> filtered = ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && it.tab == tab && !it.isBuy && it.player_sell != pl.id)
                .sorted(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed())
                .collect(Collectors.toList());

        List<ConsignItem> result = new ArrayList<>();

        if (max.length == 2) {
            int from = max[0], to = max[1];
            for (int i = from; i < to && i < filtered.size(); i++) {
                result.add(filtered.get(i));
            }
        } else if (max.length == 1) {
            int limit = max[0];
            for (int i = 0; i < limit && i < filtered.size(); i++) {
                result.add(filtered.get(i));
            }
        } else {
            return filtered;
        }

        return result;
    }

    private List<ConsignItem> getItemKyGui() {
        return ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && !it.isBuy)
                .sorted(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed())
                .collect(Collectors.toList());
    }

    private boolean isKyGui(Item item) {
        switch (item.template.type) {
            case 27:
                switch (item.template.id) {
                    case 921:
                    case 1155:
                    case 1156:
                    case 568:
                        return true;
                }
                return false;
            case 21:
            case 72:
                return true;
        }
        for (int i = 0; i < item.itemOptions.size(); i++) {
            if (item.itemOptions.get(i).optionTemplate.id == 86) {
                return true;
            }
        }
        return false;
    }

    private int getTotalThoiVang(Player pl) {
        int total = 0;
        if (pl == null || pl.inventory == null || pl.inventory.itemsBag == null) {
            return 0;
        }
        for (Item item : pl.inventory.itemsBag) {
            if (item != null && item.isNotNullItem() && item.template.id == 457 && item.quantity > 0) {
                total += item.quantity;
            }
        }
        return total;
    }

    private boolean SubThoiVang(Player pl, int quantity) {
        if (quantity <= 0 || getTotalThoiVang(pl) < quantity) {
            return false;
        }
        int remaining = quantity;
        for (Item item : pl.inventory.itemsBag) {
            if (item != null && item.isNotNullItem() && item.template.id == 457 && item.quantity > 0) {
                int take = Math.min(item.quantity, remaining);
                nro.models.services.InventoryService.gI().subQuantityItemsBag(pl, item, take);
                remaining -= take;
                if (remaining <= 0) {
                    break;
                }
            }
        }
        nro.models.services.InventoryService.gI().sendItemBags(pl);
        return true;
    }

    public void buyItem(Player pl, int id) {
        if (pl.nPoint.power < 17000000000L) {
            Service.gI().sendThongBao(pl, "Yêu cầu sức mạnh lớn hơn 17 tỷ");
            openShopKyGui(pl);
            return;
        }
        ConsignItem it = getItemBuy(id);
        if (it == null || it.isBuy) {
            Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc đã được bán");
            return;
        }
        if (it.player_sell == pl.id) {
            Service.gI().sendThongBao(pl, "Không thể mua vật phẩm bản thân đăng bán");
            openShopKyGui(pl);
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Hành trang không còn chỗ trống để nhận vật phẩm!");
            openShopKyGui(pl);
            return;
        }
        boolean isBuy = false;
        if (it.goldSell > 0) {
            if (SubThoiVang(pl, it.goldSell)) {
                isBuy = true;
            } else {
                Service.gI().sendThongBao(pl, "Bạn không đủ " + it.goldSell + " Thỏi Vàng để mua vật phẩm này!");
                isBuy = false;
            }
        } else if (it.gemSell > 0) {
            if (pl.inventory.ruby >= it.gemSell) {
                pl.inventory.ruby -= it.gemSell;
                isBuy = true;
            } else {
                Service.gI().sendThongBao(pl, "Bạn không đủ " + it.gemSell + " Hồng Ngọc để mua vật phẩm này!");
                isBuy = false;
            }
        }
        Service.gI().sendMoney(pl);
        if (isBuy) {
            Item item = ItemService.gI().createNewItem(it.itemId);
            item.quantity = it.quantity;
            item.itemOptions.clear();
            item.itemOptions.addAll(it.options);
            it.isBuy = true;
            InventoryService.gI().addItemBag(pl, item);
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Bạn đã mua thành công " + item.template.name);
            ConsignShopManager.gI().save();
            openShopKyGui(pl);
        }
    }

    public ConsignItem getItemBuy(int id) {
        for (ConsignItem it : getItemKyGui()) {
            if (it != null && it.id == id) {
                return it;
            }
        }
        return null;
    }

    public ConsignItem getItemBuy(Player pl, int id) {
        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && it.id == id && it.player_sell == pl.id) {
                return it;
            }
        }
        return null;
    }

    public void openShopKyGui(Player pl, byte index, int page) {
        if (page > getItemKyGui(pl, index).size()) {
            return;
        }
        Message msg = null;
        try {
            msg = new Message(-100);
            msg.writer().writeByte(index);
            List<ConsignItem> items = getItemKyGui(pl, index);
            List<ConsignItem> itemsSend = getItemKyGui2(pl, index, (byte) (page * 20), (byte) (page * 20 + 20));
            byte tab = (byte) (items.size() / 20 > 0 ? (items.size() / 20) + 1 : 1);
            msg.writer().writeByte(tab); // max page
            msg.writer().writeByte(page);
            msg.writer().writeByte(itemsSend.size());
            for (int j = 0; j < itemsSend.size(); j++) {
                ConsignItem itk = itemsSend.get(j);
                Item it = ItemService.gI().createNewItem(itk.itemId);
                it.itemOptions.clear();
                if (itk.options.isEmpty()) {
                    it.itemOptions.add(new ItemOption(73, 0));
                } else {
                    it.itemOptions.addAll(itk.options);
                }
                msg.writer().writeShort(it.template.id);
                msg.writer().writeShort(itk.id);
                msg.writer().writeInt(itk.goldSell);
                msg.writer().writeInt(itk.gemSell);
                msg.writer().writeByte(0); // buy type
                if (pl.getSession().version >= 222) {
                    msg.writer().writeInt(itk.quantity);
                } else {
                    msg.writer().writeByte(itk.quantity);
                }
                msg.writer().writeByte(itk.player_sell == pl.id ? 1 : 0); // isMe
                msg.writer().writeByte(it.itemOptions.size());
                for (int a = 0; a < it.itemOptions.size(); a++) {
                    msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                    msg.writer().writeShort(it.itemOptions.get(a).param);
                }
                msg.writer().writeByte(0);
            }
            pl.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }

    public void upItemToTop(Player pl, int id) {
        ConsignItem it = getItemBuy(id);
        if (it == null || it.isBuy) {
            Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc đã được bán");
            return;
        }
        if (it.player_sell != pl.id) {
            Service.gI().sendThongBao(pl, "Vật phẩm không thuộc quyền sở hữu");
            openShopKyGui(pl);
            return;
        }
        pl.idMark.setIdItemUpTop(id);
        NpcService.gI().createMenuConMeo(pl, ConstNpc.UP_TOP_ITEM, -1, "Bạn có muốn đưa vật phẩm ['" + ItemService.gI().createNewItem(it.itemId).template.name + "'] của bản thân lên trang đầu?\nYêu cầu 2 Thỏi Vàng.", "Đồng ý", "Từ Chối");
    }

    public void StartupItemToTop(Player pl) {
        if (!SubThoiVang(pl, 2)) {
            Service.gI().sendThongBao(pl, "Bạn cần có ít nhất 2 Thỏi Vàng để đưa vật phẩm lên trang đầu");
            return;
        }

        for (ConsignItem its : ConsignShopManager.gI().listItem) {
            if (its.id == pl.idMark.getIdItemUpTop()) {
                its.isUpTop = 1;
                Service.gI().sendThongBao(pl, "Đưa vật phẩm lên trang đầu thành công");
                break;
            }
        }

        ConsignShopManager.gI().listItem.sort(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed());

        ConsignShopManager.gI().save();
        openShopKyGui(pl);
    }

    public void claimOrDel(Player pl, byte action, int id) {
        ConsignItem it = getItemBuy(pl, id);
        switch (action) {
            case 1: // hủy vật phẩm
                if (it == null || it.isBuy) {
                    Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc đã được bán");
                    return;
                }
                if (it.player_sell != pl.id) {
                    Service.gI().sendThongBao(pl, "Vật phẩm không thuộc quyền sở hữu");
                    openShopKyGui(pl);
                    return;
                }
                if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
                    Service.gI().sendThongBao(pl, "Hành trang không còn chỗ trống để nhận lại vật phẩm!");
                    return;
                }
                Item item = ItemService.gI().createNewItem(it.itemId);
                item.quantity = it.quantity;
                item.itemOptions.clear();
                item.itemOptions.addAll(it.options);
                if (ConsignShopManager.gI().listItem.remove(it)) {
                    InventoryService.gI().addItemBag(pl, item);
                    InventoryService.gI().sendItemBags(pl);
                    Service.gI().sendMoney(pl);
                    ConsignShopManager.gI().save();
                    Service.gI().sendThongBao(pl, "Hủy bán vật phẩm thành công");
                    openShopKyGui(pl);
                }
                break;
            case 2: // nhận tiền (Thỏi Vàng hoặc Hồng Ngọc)
                if (it == null || !it.isBuy) {
                    Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc chưa được bán");
                    return;
                }
                if (it.player_sell != pl.id) {
                    Service.gI().sendThongBao(pl, "Vật phẩm không thuộc quyền sở hữu");
                    openShopKyGui(pl);
                    return;
                }
                String rewardMsg = "";
                if (it.goldSell > 0) {
                    if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
                        Service.gI().sendThongBao(pl, "Hành trang cần ít nhất 1 ô trống để nhận Thỏi Vàng!");
                        return;
                    }
                    int tvReceive = Math.max(1, it.goldSell - it.goldSell * 10 / 100);
                    Item tvAdd = ItemService.gI().createNewItem((short) 457, tvReceive);
                    InventoryService.gI().addItemBag(pl, tvAdd);
                    InventoryService.gI().sendItemBags(pl);
                    rewardMsg = "Bạn đã nhận được " + tvReceive + " Thỏi Vàng (sau thuế 10%)";
                } else if (it.gemSell > 0) {
                    int rubyReceive = Math.max(1, it.gemSell - it.gemSell * 10 / 100);
                    pl.inventory.ruby += rubyReceive;
                    rewardMsg = "Bạn đã nhận được " + rubyReceive + " Hồng Ngọc (sau thuế 10%)";
                }
                if (ConsignShopManager.gI().listItem.remove(it)) {
                    Service.gI().sendMoney(pl);
                    ConsignShopManager.gI().save();
                    Service.gI().sendThongBao(pl, rewardMsg.isEmpty() ? "Bạn đã bán vật phẩm thành công" : rewardMsg);
                    openShopKyGui(pl);
                }
                break;
        }
    }

    public List<ConsignItem> getItemCanKiGui(Player pl) {
        List<ConsignItem> its = new ArrayList<>();
        ConsignShopManager.gI().listItem.stream().filter((it) -> (it != null && it.player_sell == pl.id)).forEachOrdered((it) -> {
            its.add(it);
        });
        pl.inventory.itemsBag.stream().filter((it) -> (itemCanConsign(it))).forEachOrdered((it) -> {
            int idxBag = InventoryService.gI().getIndexBag(pl, it);
            if (idxBag != -1) {
                its.add(new ConsignItem(idxBag, it.template.id, (int) pl.id, (byte) 4, -1, -1, it.quantity, (byte) -1, it.itemOptions, false));
            }
        });
        return its;
    }

    public boolean itemCanConsign(Item it) {
        if (it == null || !it.isNotNullItem() || it.template == null || it.quantity <= 0) {
            return false;
        }
        if (it.template.id == 457 || nro.models.data.ItemData.isQuickTabButtonTemplateId(it.template.id)) {
            return false;
        }
        if (it.itemOptions != null) {
            for (ItemOption op : it.itemOptions) {
                if (op != null && op.optionTemplate != null
                        && (op.optionTemplate.id == 30 || op.optionTemplate.id == 93)) {
                    return false;
                }
            }
            for (ItemOption op : it.itemOptions) {
                if (op != null && op.optionTemplate != null
                        && (op.optionTemplate.id == 86 || op.optionTemplate.id == 87)) {
                    return true;
                }
            }
        }
        int type = it.template.type;
        return (type >= 0 && type <= 6)
                || type == 11 || type == 12 || type == 14 || type == 15
                || type == 27 || type == 29 || type == 30 || type == 32 || type == 33
                || (it.template.id >= 14 && it.template.id <= 20);
    }

    public int getMaxId() {
        try {
            List<Integer> id = new ArrayList<>();
            ConsignShopManager.gI().listItem.stream().filter((it) -> (it != null)).forEachOrdered((it) -> {
                id.add(it.id);
            });

            if (id.isEmpty()) {
                return 0;
            }

            return Collections.max(id);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public byte getTabKiGui(Item it) {
        if (it.template.type == 0 || it.template.type == 1) {
            return 0; // Áo Quần
        } else if (it.template.type == 2) {
            return 1; // Găng Tay
        } else if (it.template.type == 3 || it.template.type == 4 || it.template.type == 5 || it.template.type == 11) {
            return 2; // Phụ Kiện (Giày, Rada, Cải trang, Đeo lưng)
        } else {
            return 3; // Linh tinh
        }
    }

    public void KiGui(Player pl, int id, int money, byte moneyType, int quantity) {
        try {
            int bagIndex = pl.inventory.resolveBagIndex(id);
            if (bagIndex < 0 || bagIndex >= pl.inventory.itemsBag.size()) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }
            Item bagItem = pl.inventory.itemsBag.get(bagIndex);
            if (bagItem == null || !bagItem.isNotNullItem() || !itemCanConsign(bagItem)) {
                Service.gI().sendThongBao(pl, "Vật phẩm này không thể ký gửi!");
                openShopKyGui(pl);
                return;
            }
            if (money <= 0 || quantity <= 0 || quantity > bagItem.quantity) {
                Service.gI().sendThongBao(pl, "Số lượng hoặc mức giá không hợp lệ!");
                openShopKyGui(pl);
                return;
            }
            if (quantity > 99) {
                Service.gI().sendThongBao(pl, "Ký gửi tối đa x99 mỗi lần!");
                openShopKyGui(pl);
                return;
            }

            Item it = ItemService.gI().copyItem(bagItem);
            switch (moneyType) {
                case 0: // Ký gửi bằng Thỏi Vàng
                    if (money < 1 || money > 100_000) {
                        Service.gI().sendThongBao(pl, "Mức giá ký gửi từ 1 đến 100.000 Thỏi Vàng!");
                        openShopKyGui(pl);
                        return;
                    }
                    // Nếu vật phẩm đang bán chính là Thỏi Vàng hoặc trừ 1 Thỏi Vàng làm phí
                    if (!SubThoiVang(pl, 1)) {
                        Service.gI().sendThongBao(pl, "Bạn cần có ít nhất 1 Thỏi Vàng để làm phí đăng bán!");
                        openShopKyGui(pl);
                        return;
                    }
                    InventoryService.gI().subQuantityItemsBag(pl, bagItem, quantity);
                    ConsignShopManager.gI().listItem.add(new ConsignItem(getMaxId() + 1, it.template.id, (int) pl.id, getTabKiGui(it), money, -1, quantity, (byte) 0, it.itemOptions, false));
                    InventoryService.gI().sendItemBags(pl);
                    openShopKyGui(pl);
                    Service.gI().sendMoney(pl);
                    Service.gI().sendThongBao(pl, "Đăng bán thành công với giá " + money + " Thỏi Vàng!");
                    ConsignShopManager.gI().save();
                    break;
                case 1: // Ký gửi bằng Hồng Ngọc
                    if (money < 1 || money > 1_000_000) {
                        Service.gI().sendThongBao(pl, "Mức giá ký gửi từ 1 đến 1.000.000 Hồng Ngọc!");
                        openShopKyGui(pl);
                        return;
                    }
                    if (!SubThoiVang(pl, 1)) {
                        Service.gI().sendThongBao(pl, "Bạn cần có ít nhất 1 Thỏi Vàng để làm phí đăng bán!");
                        openShopKyGui(pl);
                        return;
                    }
                    InventoryService.gI().subQuantityItemsBag(pl, bagItem, quantity);
                    ConsignShopManager.gI().listItem.add(new ConsignItem(getMaxId() + 1, it.template.id, (int) pl.id, getTabKiGui(it), -1, money, quantity, (byte) 0, it.itemOptions, false));
                    InventoryService.gI().sendItemBags(pl);
                    openShopKyGui(pl);
                    Service.gI().sendMoney(pl);
                    Service.gI().sendThongBao(pl, "Đăng bán thành công với giá " + money + " Hồng Ngọc!");
                    ConsignShopManager.gI().save();
                    break;
                default:
                    Service.gI().sendThongBao(pl, "Có lỗi xảy ra");
                    openShopKyGui(pl);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openShopKyGui(Player pl) {
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(2);
            msg.writer().writeByte(5);
            for (byte i = 0; i < 5; i++) {
                if (i == 4) {
                    msg.writer().writeUTF(ConsignShopManager.gI().tabName[i]);
                    msg.writer().writeByte(0);
                    msg.writer().writeByte(getItemCanKiGui(pl).size());
                    for (int j = 0; j < getItemCanKiGui(pl).size(); j++) {
                        ConsignItem itk = getItemCanKiGui(pl).get(j);
                        if (itk == null) {
                            continue;
                        }
                        Item it = ItemService.gI().createNewItem(itk.itemId);
                        it.itemOptions.clear();
                        if (itk.options.isEmpty()) {
                            it.itemOptions.add(new ItemOption(73, 0));
                        } else {
                            it.itemOptions.addAll(itk.options);
                        }
                        msg.writer().writeShort(it.template.id);
                        msg.writer().writeShort(itk.id);
                        msg.writer().writeInt(itk.goldSell);
                        msg.writer().writeInt(itk.gemSell);
                        if (getItemBuy(pl, itk.id) == null) {
                            msg.writer().writeByte(0); // buy type
                        } else if (itk.isBuy) {
                            msg.writer().writeByte(2);
                        } else {
                            msg.writer().writeByte(1);
                        }
                        msg.writer().writeInt(itk.quantity);
                        msg.writer().writeByte(1); // isMe
                        msg.writer().writeByte(it.itemOptions.size());
                        for (int a = 0; a < it.itemOptions.size(); a++) {
                            msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                            msg.writer().writeShort(it.itemOptions.get(a).param);
                        }
                        msg.writer().writeByte(0);
                        msg.writer().writeByte(0);
                    }
                } else {
                    List<ConsignItem> items = getItemKyGui(pl, i);
                    List<ConsignItem> itemsSend = getItemKyGui2(pl, i, (byte) 0, (byte) 20);
                    msg.writer().writeUTF(ConsignShopManager.gI().tabName[i]);
                    byte tab = (byte) (items.size() / 20 > 0 ? (items.size() / 20) + 1 : 1);
                    msg.writer().writeByte(tab); // max page
                    msg.writer().writeByte(itemsSend.size());
                    for (int j = 0; j < itemsSend.size(); j++) {
                        ConsignItem itk = itemsSend.get(j);
                        Item it = ItemService.gI().createNewItem(itk.itemId);
                        it.itemOptions.clear();
                        if (itk.options.isEmpty()) {
                            it.itemOptions.add(new ItemOption(73, 0));
                        } else {
                            it.itemOptions.addAll(itk.options);
                        }
                        msg.writer().writeShort(it.template.id);
                        msg.writer().writeShort(itk.id);
                        msg.writer().writeInt(itk.goldSell);
                        msg.writer().writeInt(itk.gemSell);
                        msg.writer().writeByte(0); // buy type
                        msg.writer().writeInt(itk.quantity);
                        msg.writer().writeByte(itk.player_sell == pl.id ? 1 : 0); // isMe     
                        msg.writer().writeByte(it.itemOptions.size());
                        for (int a = 0; a < it.itemOptions.size(); a++) {
                            msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                            msg.writer().writeShort(it.itemOptions.get(a).param);
                        }
                        msg.writer().writeByte(0);
                        msg.writer().writeByte(0);
                    }
                }
            }
            pl.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }
}
