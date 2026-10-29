package nro.models.services;

import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.npc.MabuEgg;
import nro.models.player.Inventory;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.map.service.ChangeMapService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nro.models.consts.ConstTaskBadges;
import nro.models.data.ItemData;
import nro.models.services_dungeon.BlackBallWarService;
import nro.models.map.service.ItemMapService;
import nro.models.task.BadgesTaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class InventoryService {

    private static InventoryService I;

    public static InventoryService gI() {
        if (InventoryService.I == null) {
            InventoryService.I = new InventoryService();
        }
        return InventoryService.I;
    }

    private void __________________Tìm_kiếm_item_____________________________() {
        //**********************************************************************
    }

    public Item findItem(List<Item> list, int tempId) {
        try {
            for (Item item : list) {
                if (item.isNotNullItem() && item.template.id == tempId) {
                    return item;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Item findItemBody(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBody, tempId);
    }

    public Item findItemBag(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBag, tempId);
    }

    public Item findItemBox(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBox, tempId);
    }

    public boolean isExistItem(List<Item> list, int tempId) {
        try {
            return this.findItem(list, tempId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isExistItemBody(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBody, tempId);
    }

    public boolean isExistItemBag(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBag, tempId);
    }

    public boolean isExistItemBox(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBox, tempId);
    }

    private void __________________Sao_chép_danh_sách_item__________________() {
        //**********************************************************************
    }

    public List<Item> copyList(List<Item> items) {
        List<Item> list = new ArrayList<>();
        for (Item item : items) {
            list.add(ItemService.gI().copyItem(item));
        }
        return list;
    }

    public List<Item> copyItemsBody(Player player) {
        return copyList(player.inventory.itemsBody);
    }

    public List<Item> copyItemsBag(Player player) {
        return copyList(player.inventory.itemsBag);
    }

    public List<Item> copyItemsBox(Player player) {
        return copyList(player.inventory.itemsBox);
    }

    private void __________________Vứt_bỏ_item______________________________() {
        //**********************************************************************
    }

    public static final int OPTION_ID_LOCKED_ITEM = 39;
    public static final int OPTION_ID_EQUIP_SET = 209;
    public static final int MAX_EQUIP_SETS = 5;

    public static boolean isItemLocked(Item item) {
        if (item == null || !item.isNotNullItem() || item.itemOptions == null) {
            return false;
        }
        for (Item.ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == OPTION_ID_LOCKED_ITEM) {
                return true;
            }
        }
        return false;
    }

    public static void setItemLock(Item item, boolean locked) {
        if (item == null || !item.isNotNullItem()) {
            return;
        }
        if (item.itemOptions == null) {
            item.itemOptions = new ArrayList<>();
        }
        item.itemOptions.removeIf(io -> io != null && io.optionTemplate != null && io.optionTemplate.id == OPTION_ID_LOCKED_ITEM);
        if (locked) {
            item.itemOptions.add(new Item.ItemOption(OPTION_ID_LOCKED_ITEM, 1));
        }
    }

    public static boolean toggleItemLock(Item item) {
        boolean newState = !isItemLocked(item);
        setItemLock(item, newState);
        return newState;
    }

    public static boolean isLockableEquipment(Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return false;
        }
        if (ItemData.isQuickTabButtonTemplateId(item.template.id)) {
            return false;
        }
        int type = item.template.type;
        return (type >= 0 && type <= 5)
                || type == 11 || type == 21 || type == 23 || type == 24
                || type == 25 || type == 32 || type == 35 || type == 36 || type == 72
                || item.isDTL() || item.isDHD() || item.isDTS()
                || (type == 27 && !item.template.isUpToUp);
    }

    public static boolean isItemInEquipSet(Item item, int setIndex) {
        if (item == null || !item.isNotNullItem() || item.itemOptions == null || setIndex < 1 || setIndex > MAX_EQUIP_SETS) {
            return false;
        }
        for (Item.ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id == OPTION_ID_EQUIP_SET && io.param == setIndex) {
                return true;
            }
        }
        return false;
    }

    public static void removeItemFromEquipSet(Item item, int setIndex) {
        if (item == null || !item.isNotNullItem() || item.itemOptions == null) {
            return;
        }
        item.itemOptions.removeIf(io -> io != null && io.optionTemplate != null
                && io.optionTemplate.id == OPTION_ID_EQUIP_SET && io.param == setIndex);
    }

    public static void addItemToEquipSet(Item item, int setIndex) {
        if (item == null || !item.isNotNullItem() || setIndex < 1 || setIndex > MAX_EQUIP_SETS) {
            return;
        }
        if (item.itemOptions == null) {
            item.itemOptions = new ArrayList<>();
        }
        removeItemFromEquipSet(item, setIndex);
        item.itemOptions.add(new Item.ItemOption(OPTION_ID_EQUIP_SET, setIndex));
    }

    public void clearEquipSet(Player player, int setIndex) {
        if (player == null || player.inventory == null || setIndex < 1 || setIndex > MAX_EQUIP_SETS) {
            return;
        }
        for (Item it : player.inventory.itemsBody) {
            removeItemFromEquipSet(it, setIndex);
        }
        for (Item it : player.inventory.itemsBag) {
            removeItemFromEquipSet(it, setIndex);
        }
        for (Item it : player.inventory.itemsBox) {
            removeItemFromEquipSet(it, setIndex);
        }
        if (player.pet != null && player.pet.inventory != null) {
            for (Item it : player.pet.inventory.itemsBody) {
                removeItemFromEquipSet(it, setIndex);
            }
        }
        sendItemBody(player);
        sendItemBags(player);
    }

    public List<Item> getItemsInEquipSet(Player player, int setIndex) {
        List<Item> result = new ArrayList<>();
        if (player == null || player.inventory == null || setIndex < 1 || setIndex > MAX_EQUIP_SETS) {
            return result;
        }
        for (Item it : player.inventory.itemsBody) {
            if (isItemInEquipSet(it, setIndex)) {
                result.add(it);
            }
        }
        if (player.pet != null && player.pet.inventory != null) {
            for (Item it : player.pet.inventory.itemsBody) {
                if (isItemInEquipSet(it, setIndex)) {
                    result.add(it);
                }
            }
        }
        for (Item it : player.inventory.itemsBag) {
            if (isItemInEquipSet(it, setIndex)) {
                result.add(it);
            }
        }
        return result;
    }

    public String getEquipSetSummary(Player player, int setIndex) {
        List<Item> list = getItemsInEquipSet(player, setIndex);
        if (list.isEmpty()) {
            return "Chưa thiết lập (0 món)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(list.size()).append(" món (");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(list.get(i).template.name);
        }
        sb.append(")");
        return sb.toString();
    }

    public String saveCurrentBodyToEquipSet(Player player, int setIndex, boolean fromPet) {
        if (player == null || player.inventory == null || setIndex < 1 || setIndex > MAX_EQUIP_SETS) {
            return "Set đồ không hợp lệ!";
        }
        if (fromPet && (player.pet == null || player.pet.inventory == null)) {
            return "Bạn hiện chưa có Đệ tử để lấy trang bị!";
        }
        List<Item> srcBody = fromPet ? player.pet.inventory.itemsBody : player.inventory.itemsBody;
        List<Item> validItems = new ArrayList<>();
        for (Item it : srcBody) {
            if (it != null && it.isNotNullItem() && it.template != null) {
                validItems.add(it);
            }
        }
        if (validItems.isEmpty()) {
            return (fromPet ? "Đệ tử" : "Sư phụ") + " hiện chưa mặc trang bị nào để lưu vào Set " + setIndex + "!";
        }
        clearEquipSet(player, setIndex);
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < validItems.size(); i++) {
            Item it = validItems.get(i);
            addItemToEquipSet(it, setIndex);
            setItemLock(it, true);
            if (i > 0) {
                names.append(", ");
            }
            names.append(it.template.name);
        }
        sendItemBody(player);
        sendItemBags(player);
        return "Đã lưu " + validItems.size() + " món của " + (fromPet ? "Đệ tử" : "Sư phụ")
                + " vào Set Đồ số " + setIndex + " (đồng thời tự động Khóa bảo vệ): " + names;
    }

    private static int getTargetBodySlot(Player targetPl, Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return -1;
        }
        return switch (item.template.type) {
            case 0, 1, 2, 3, 4, 5 -> item.template.type;
            case 32 -> 6;
            case 23, 24 -> targetPl.isPet ? -1 : 9;
            case 11 -> 8;
            case 27 -> 7;
            case 25 -> targetPl.isPet ? 7 : 10;
            default -> -1;
        };
    }

    private static boolean canEquipOnTarget(Player targetPl, Item item, List<String> skipReasons) {
        if (targetPl == null || item == null || !item.isNotNullItem() || item.template == null) {
            return false;
        }
        int slot = getTargetBodySlot(targetPl, item);
        if (slot < 0) {
            return false;
        }
        if (item.template.gender < 3 && item.template.gender != targetPl.gender) {
            if (skipReasons != null) {
                skipReasons.add(item.template.name + " (khác hành tinh)");
            }
            return false;
        }
        long powerRequire = item.template.strRequire;
        if (item.itemOptions != null) {
            for (Item.ItemOption io : item.itemOptions) {
                if (io != null && io.optionTemplate != null && io.optionTemplate.id == 21) {
                    powerRequire = io.param * 1_000_000_000L;
                    break;
                }
            }
        }
        if (targetPl.nPoint.power < powerRequire) {
            if (skipReasons != null) {
                skipReasons.add(item.template.name + " (chưa đủ SM)");
            }
            return false;
        }
        return true;
    }

    public String equipSetForTarget(Player player, int setIndex, boolean forPet) {
        if (player == null || player.inventory == null || setIndex < 1 || setIndex > MAX_EQUIP_SETS) {
            return "Set đồ không hợp lệ!";
        }
        if (forPet) {
            if (player.pet == null || player.pet.inventory == null) {
                return "Bạn hiện chưa có Đệ tử!";
            }
            if (player.pet.nPoint.power < 1_500_000L) {
                return "Đệ tử phải đạt 1tr5 sức mạnh mới có thể mặc trang bị!";
            }
        }
        List<Item> allSetItems = getItemsInEquipSet(player, setIndex);
        if (allSetItems.isEmpty()) {
            return "Set đồ số " + setIndex + " chưa được thiết lập (hoặc trang bị đang cất trong Rương đồ)!";
        }

        Player targetPl = forPet ? player.pet : player;
        Player otherPl = forPet ? player : player.pet;
        List<Item> targetBody = targetPl.inventory.itemsBody;
        int alreadyWorn = 0;
        int newlyEquipped = 0;
        List<String> skipReasons = new ArrayList<>();

        for (Item it : targetBody) {
            if (isItemInEquipSet(it, setIndex)) {
                alreadyWorn++;
            }
        }

        // 1. Mặc các món của Set đang nằm trong Hành trang
        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            Item bagItem = player.inventory.itemsBag.get(i);
            if (!isItemInEquipSet(bagItem, setIndex)) {
                continue;
            }
            if (!canEquipOnTarget(targetPl, bagItem, skipReasons)) {
                continue;
            }
            Item swapped = putItemBody(targetPl, bagItem);
            player.inventory.itemsBag.set(i, swapped);
            newlyEquipped++;
        }

        // 2. Mặc các món của Set đang nằm trên người nhân vật còn lại (Sư phụ <-> Đệ tử)
        if (otherPl != null && otherPl.inventory != null) {
            List<Item> otherBody = otherPl.inventory.itemsBody;
            for (int j = 0; j < otherBody.size(); j++) {
                Item otherItem = otherBody.get(j);
                if (!isItemInEquipSet(otherItem, setIndex)) {
                    continue;
                }
                if (!canEquipOnTarget(targetPl, otherItem, skipReasons)) {
                    continue;
                }
                Item oldOnTarget = putItemBody(targetPl, otherItem);
                if (oldOnTarget != null && oldOnTarget.isNotNullItem()) {
                    if (canEquipOnTarget(otherPl, oldOnTarget, null) && getTargetBodySlot(otherPl, oldOnTarget) == j) {
                        otherBody.set(j, oldOnTarget);
                    } else {
                        otherBody.set(j, ItemService.gI().createItemNull());
                        Item leftover = putItemBag(player, oldOnTarget);
                        if (leftover != null && leftover.isNotNullItem()) {
                            // Nếu hành trang đầy thì trả lại vị trí cũ
                            putItemBody(targetPl, oldOnTarget);
                            otherBody.set(j, otherItem);
                            newlyEquipped--;
                            skipReasons.add("Hành trang đầy");
                        }
                    }
                } else {
                    otherBody.set(j, ItemService.gI().createItemNull());
                }
                newlyEquipped++;
            }
        }

        player.setClothes.setup();
        if (player.pet != null) {
            player.pet.setClothes.setup();
            player.pet.nPoint.calPoint();
        }
        player.nPoint.calPoint();

        sendItemBags(player);
        sendItemBody(player);
        Service.gI().point(player);
        Service.gI().Send_Caitrang(player);
        Service.gI().sendFlagBag(player);
        if (player.pet != null) {
            Service.gI().Send_Caitrang(player.pet);
            Service.gI().sendFlagBag(player.pet);
            Service.gI().showInfoPet(player);
        }

        String targetName = forPet ? "Đệ tử" : "Sư phụ";
        StringBuilder msg = new StringBuilder();
        if (newlyEquipped > 0 || alreadyWorn > 0) {
            msg.append("Đã mặc Set Đồ số ").append(setIndex).append(" cho ").append(targetName)
                    .append(" (Mặc mới: ").append(newlyEquipped).append(" món, Đang mặc sẵn: ").append(alreadyWorn).append(" món)!");
        } else {
            msg.append("Không thể mặc Set Đồ số ").append(setIndex).append(" cho ").append(targetName).append("!");
        }
        if (!skipReasons.isEmpty()) {
            msg.append(" Bỏ qua: ").append(String.join(", ", skipReasons));
        }
        return msg.toString();
    }

    public void throwItem(Player player, int where, int index) {
        Item itemThrow = null;
        if (where == 0) {
            if (index < 0 || index >= player.inventory.itemsBody.size()) {
                return;
            }
            itemThrow = player.inventory.itemsBody.get(index);
            if (isItemLocked(itemThrow)) {
                Service.gI().sendThongBao(player, "Trang bị [" + itemThrow.template.name + "] đang được KHÓA bảo vệ! Hãy mở khóa trong Menu Tiện Ích nếu muốn vứt.");
                return;
            }
            removeItemBody(player, index);
            sendItemBody(player);
            Service.gI().Send_Caitrang(player);
        } else if (where == 1) {
            if (index < 0 || index >= player.inventory.itemsBag.size()) {
                return;
            }
            itemThrow = player.inventory.itemsBag.get(index);
            if (isItemLocked(itemThrow)) {
                Service.gI().sendThongBao(player, "Trang bị [" + itemThrow.template.name + "] đang được KHÓA bảo vệ! Hãy mở khóa trong Menu Tiện Ích nếu muốn vứt.");
                return;
            }
            if (itemThrow.template != null && itemThrow.template.id == 570) {
                Service.gI().sendThongBao(player, "Không thể bỏ vật phẩm này.");
                return;
            }
            if (itemThrow.template != null && itemThrow.template.id != 457) {
                removeItemBag(player, index);
                sortItems(player.inventory.itemsBag);
                sendItemBags(player);
            } else {
                Service.gI().sendThongBao(player, "Không thể vứt bỏ Thỏi vàng!");
            }
        }
        if (itemThrow == null) {
            return;
        }
    }

    private void __________________Xoá_bỏ_item______________________________() {
        //**********************************************************************
    }

    public void removeItem(List<Item> items, int index) {
        Item item = ItemService.gI().createItemNull();
        items.set(index, item);
    }

    public void removeItem(List<Item> items, Item item) {
        if (item == null) {
            return;
        }
        Item it = ItemService.gI().createItemNull();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(item)) {
                items.set(i, it);
                item.dispose();
                break;
            }
        }
    }

    public void removeItemBag(Player player, int index) {
        this.removeItem(player.inventory.itemsBag, index);
    }

    public void removeItemBag(Player player, Item item) {
        this.removeItem(player.inventory.itemsBag, item);
    }

    public void removeItemBody(Player player, int index) {
        this.removeItem(player.inventory.itemsBody, index);
    }

    public void removeItemPetBody(Player player, int index) {
        this.removeItemBody(player.pet, index);
    }

    public void removeItemBox(Player player, int index) {
        this.removeItem(player.inventory.itemsBox, index);
    }

    private void __________________Giảm_số_lượng_item_______________________() {
        //**********************************************************************
    }

    public void subQuantityItemsBag(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBag, item, quantity);
    }

    public void subQuantityItemsBody(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBody, item, quantity);
    }

    public void subQuantityItemsBox(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBox, item, quantity);
    }

    public void subQuantityItem(List<Item> items, Item item, int quantity) {
        if (item != null) {
            for (Item it : items) {
                if (item.equals(it)) {
                    it.quantity -= quantity;
                    if (it.quantity <= 0) {
                        this.removeItem(items, item);
                    }
                    break;
                }
            }
        }
    }

    private void __________________Sắp_xếp_danh_sách_item___________________() {
        //**********************************************************************
    }

    private static boolean hasOnlyPlaceholderOptions(List<ItemOption> opts) {
        if (opts == null || opts.isEmpty()) {
            return true;
        }
        for (ItemOption io : opts) {
            if (io != null && io.optionTemplate != null) {
                int id = io.optionTemplate.id;
                if (id != 30 && id != 73 && id != 174 && id != 86 && id != 87) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isStackableItem(Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return false;
        }
        int id = item.template.id;
        int type = item.template.type;
        // Không gộp các vật phẩm đặc biệt đơn chiếc
        if (id == 570 || id == 454 || id == 921 || id == 1819 || id == 193 || id == 194 || id == 361 || id == 379 || id == 992) {
            return false;
        }
        if (isItemIncrementalOption(item)[0] != -1) {
            return false;
        }
        // Không bao giờ gộp trang bị mặc trên người / đệ tử / thú cưỡi / sách tuyệt kỹ
        switch (type) {
            case 0, 1, 2, 3, 4, 5, 11, 21, 23, 24, 25, 32, 35, 36, 72 -> {
                return false;
            }
            case 27 -> {
                if (item.itemOptions != null) {
                    for (ItemOption io : item.itemOptions) {
                        if (io != null && io.optionTemplate != null) {
                            int opId = io.optionTemplate.id;
                            if (opId == 50 || opId == 77 || opId == 103 || opId == 5 || opId == 14
                                    || opId == 94 || opId == 108 || opId == 102 || opId == 107) {
                                return false;
                            }
                        }
                    }
                }
                if (item.template.isUpToUp) {
                    return true;
                }
                return hasOnlyPlaceholderOptions(item.itemOptions);
            }
            default -> {
                return true;
            }
        }
    }

    private boolean tryMergeStack(Item target, Item source) {
        if (target == null || source == null || target == source || !target.isNotNullItem() || !source.isNotNullItem()) {
            return false;
        }
        if (target.template == null || source.template == null || target.template.id != source.template.id) {
            return false;
        }
        sanitizeStarOptions(target);
        sanitizeStarOptions(source);
        int[] idParamT = isItemIncrementalOption(target);
        int[] idParamS = isItemIncrementalOption(source);
        if (idParamT[0] != -1 || idParamS[0] != -1) {
            int optId = idParamT[0] != -1 ? idParamT[0] : idParamS[0];
            if ((idParamT[0] == -1 || idParamT[0] == optId) && (idParamS[0] == -1 || idParamS[0] == optId)) {
                int valT = idParamT[0] != -1 ? Math.max(idParamT[1], target.quantity) : Math.max(1, target.quantity);
                int valS = idParamS[0] != -1 ? Math.max(idParamS[1], source.quantity) : Math.max(1, source.quantity);
                if (valT > 0 && valS > 0 && valT < 30000) {
                    int canAdd = 30000 - valT;
                    int add = Math.min(canAdd, valS);
                    ItemOption found = null;
                    for (ItemOption io : target.itemOptions) {
                        if (io != null && io.optionTemplate != null && io.optionTemplate.id == optId) {
                            found = io;
                            break;
                        }
                    }
                    if (found != null) {
                        found.param = valT + add;
                    } else {
                        target.itemOptions.add(new ItemOption(optId, valT + add));
                    }
                    if (target.quantity > 1) {
                        target.quantity = 1;
                    }
                    if (valS <= canAdd) {
                        source.quantity = 0;
                        return true;
                    } else {
                        int remain = valS - add;
                        for (ItemOption io : source.itemOptions) {
                            if (io != null && io.optionTemplate != null && io.optionTemplate.id == optId) {
                                io.param = remain;
                                break;
                            }
                        }
                        source.quantity = (idParamS[0] != -1) ? 1 : remain;
                        return false;
                    }
                }
            }
            return false;
        }
        if (!isStackableItem(target) || !isStackableItem(source)) {
            return false;
        }
        if (hasOnlyPlaceholderOptions(target.itemOptions) && !hasOnlyPlaceholderOptions(source.itemOptions)) {
            target.itemOptions.clear();
            for (ItemOption io : source.itemOptions) {
                target.itemOptions.add(new ItemOption(io));
            }
        }
        int maxStack = ((target.template.id >= 1066 && target.template.id <= 1070) || target.template.id == 457
                || target.template.id == 610 || target.template.type == 14 || target.template.id == 2048
                || (target.template.id > 2049 && target.template.id < 2056) || target.template.id == 821
                || target.template.id == 2075) ? 100_000_000 : 99999;
        if (target.quantity >= maxStack) {
            return false;
        }
        int canAdd = maxStack - target.quantity;
        if (source.quantity <= canAdd) {
            target.quantity += source.quantity;
            source.quantity = 0;
            return true;
        } else {
            target.quantity = maxStack;
            source.quantity -= canAdd;
            return false;
        }
    }

    public void sortItems(List<Item> list) {
        int totalSize = list.size();
        if (totalSize <= 1) {
            return;
        }
        // 1. Gộp các vật phẩm trùng ID có thể cộng dồn trong toàn bộ danh sách
        for (int i = 0; i < totalSize - 1; i++) {
            Item current = list.get(i);
            if (current == null || !current.isNotNullItem()) {
                continue;
            }
            for (int j = i + 1; j < totalSize; j++) {
                Item candidate = list.get(j);
                if (candidate == null || !candidate.isNotNullItem()) {
                    continue;
                }
                if (tryMergeStack(current, candidate)) {
                    list.set(j, ItemService.gI().createItemNull());
                }
            }
        }

        if (list instanceof Inventory.BagList) {
            int maxTabSlots = Inventory.NUM_QUICK_TABS * Inventory.BAG_VIEW_SIZE; // 600 ô (5 Tab x 120 ô)
            // Nếu có vật phẩm nằm ngoài ô 600, đưa vào ô trống đầu tiên trong 5 Tab (0..599)
            if (totalSize > maxTabSlots) {
                for (int i = maxTabSlots; i < totalSize; i++) {
                    Item overflow = list.get(i);
                    if (overflow != null && overflow.isNotNullItem()) {
                        for (int k = 0; k < maxTabSlots; k++) {
                            Item slot = list.get(k);
                            if (slot == null || !slot.isNotNullItem()) {
                                list.set(k, overflow);
                                list.set(i, ItemService.gI().createItemNull());
                                break;
                            }
                        }
                    }
                }
            }
            // 2. Dồn các ô trống ĐỘC LẬP trong phạm vi từng Tab (mỗi Tab 120 ô) để đồ ở Tab nào giữ nguyên ở Tab đó
            for (int chunkStart = 0; chunkStart < totalSize; chunkStart += Inventory.BAG_VIEW_SIZE) {
                int chunkEnd = Math.min(totalSize, chunkStart + Inventory.BAG_VIEW_SIZE);
                for (int i = chunkStart; i < chunkEnd - 1; i++) {
                    Item current = list.get(i);
                    if (current == null || !current.isNotNullItem()) {
                        int indexSwap = -1;
                        for (int j = i + 1; j < chunkEnd; j++) {
                            Item candidate = list.get(j);
                            if (candidate != null && candidate.isNotNullItem()) {
                                indexSwap = j;
                                break;
                            }
                        }
                        if (indexSwap != -1) {
                            list.set(i, list.get(indexSwap));
                            list.set(indexSwap, ItemService.gI().createItemNull());
                        } else {
                            break;
                        }
                    }
                }
            }
            return;
        }

        // 2. Dồn các ô trống cho Rương đồ (itemsBox) và danh sách thông thường
        for (int i = 0; i < totalSize - 1; i++) {
            Item current = list.get(i);
            if (current == null || !current.isNotNullItem()) {
                int indexSwap = -1;
                for (int j = i + 1; j < totalSize; j++) {
                    Item candidate = list.get(j);
                    if (candidate != null && candidate.isNotNullItem()) {
                        indexSwap = j;
                        break;
                    }
                }
                if (indexSwap != -1) {
                    list.set(i, list.get(indexSwap));
                    list.set(indexSwap, ItemService.gI().createItemNull());
                } else {
                    break;
                }
            }
        }
    }

    public static boolean isImportantUtilityItem(Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return false;
        }
        int id = item.template.id;
        int type = item.template.type;
        return type == 6 || type == 12
                || id == 454 || id == 921 || id == 1819
                || id == 193 || id == 194
                || id == 457 || id == 992
                || id == 361 || id == 379;
    }

    public boolean moveItemToTab(Player player, int sourceBagIndex, int targetTab) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null) {
            return false;
        }
        if (sourceBagIndex < 0 || sourceBagIndex >= player.inventory.itemsBag.size()
                || targetTab < 0 || targetTab >= Inventory.NUM_QUICK_TABS) {
            return false;
        }
        Item sourceItem = player.inventory.itemsBag.get(sourceBagIndex);
        if (sourceItem == null || !sourceItem.isNotNullItem() || sourceItem.template == null) {
            player.inventory.pendingMoveBagIndex = -1;
            return false;
        }
        String itemName = sourceItem.template.name;
        int sourceTab = sourceBagIndex / Inventory.BAG_VIEW_SIZE;
        if (sourceTab == targetTab) {
            player.inventory.pendingMoveBagIndex = -1;
            sendItemBags(player);
            Service.gI().sendThongBao(player, itemName + " đã nằm ở Trang " + (targetTab + 1) + " rồi!");
            return false;
        }
        int targetStart = targetTab * Inventory.BAG_VIEW_SIZE;
        int targetEnd = Math.min(player.inventory.itemsBag.size(), targetStart + Inventory.BAG_VIEW_SIZE);

        // Tìm ô trống đầu tiên trong Tab đích
        for (int i = targetStart; i < targetEnd; i++) {
            Item slot = player.inventory.itemsBag.get(i);
            if (slot == null || !slot.isNotNullItem()) {
                player.inventory.itemsBag.set(i, sourceItem);
                player.inventory.itemsBag.set(sourceBagIndex, ItemService.gI().createItemNull());
                player.inventory.pendingMoveBagIndex = -1;
                sortItems(player.inventory.itemsBag);
                sendItemBags(player);
                Service.gI().sendThongBao(player, "Đã chuyển [" + itemName + "] từ Trang "
                        + (sourceTab + 1) + " sang Trang " + (targetTab + 1) + "!");
                return true;
            }
        }

        // Nếu Tab đích đã đầy 120/120 ô: Tự động hoán đổi (swap) với món cuối cùng của Tab đích
        int swapIdx = targetEnd - 1;
        for (int i = targetEnd - 1; i >= targetStart; i--) {
            Item cand = player.inventory.itemsBag.get(i);
            if (cand != null && cand.isNotNullItem() && !isImportantUtilityItem(cand)) {
                swapIdx = i;
                break;
            }
        }
        Item swappedItem = player.inventory.itemsBag.get(swapIdx);
        String swappedName = (swappedItem != null && swappedItem.isNotNullItem() && swappedItem.template != null)
                ? swappedItem.template.name : "vật phẩm";
        player.inventory.itemsBag.set(swapIdx, sourceItem);
        player.inventory.itemsBag.set(sourceBagIndex, swappedItem);
        player.inventory.pendingMoveBagIndex = -1;
        sortItems(player.inventory.itemsBag);
        sendItemBags(player);
        Service.gI().sendThongBao(player, "Trang " + (targetTab + 1) + " đã đầy 120 ô: Đã chuyển ["
                + itemName + "] sang Trang " + (targetTab + 1) + " (đổi chỗ với [" + swappedName + "])!");
        return true;
    }

    public int moveUtilityItemsToFirstTab(Player player) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null) {
            return 0;
        }
        int moved = 0;
        int firstTabEnd = Math.min(player.inventory.itemsBag.size(), Inventory.BAG_VIEW_SIZE);
        for (int i = firstTabEnd; i < player.inventory.itemsBag.size(); i++) {
            Item it = player.inventory.itemsBag.get(i);
            if (isImportantUtilityItem(it)) {
                boolean placed = false;
                for (int k = 0; k < firstTabEnd; k++) {
                    Item slot = player.inventory.itemsBag.get(k);
                    if (slot == null || !slot.isNotNullItem()) {
                        player.inventory.itemsBag.set(k, it);
                        player.inventory.itemsBag.set(i, ItemService.gI().createItemNull());
                        placed = true;
                        moved++;
                        break;
                    }
                }
                if (!placed) {
                    for (int k = firstTabEnd - 1; k >= 0; k--) {
                        Item cand = player.inventory.itemsBag.get(k);
                        if (!isImportantUtilityItem(cand)) {
                            player.inventory.itemsBag.set(k, it);
                            player.inventory.itemsBag.set(i, cand);
                            moved++;
                            break;
                        }
                    }
                }
            }
        }
        if (moved > 0) {
            player.inventory.pendingMoveBagIndex = -1;
            sortItems(player.inventory.itemsBag);
            sendItemBags(player);
        }
        return moved;
    }

    public Item finditemBongHoa(Player player, int soluong) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && (item.template.id == 589) && item.quantity >= soluong) {
                return item;
            }
        }
        return null;
    }

    public void sortItemv2(List<Item> items) {
        int index = 0;
        for (Item item : items) {
            if (item != null && item.quantity > 0) {
                items.set(index, item);
                index++;
            }
        }
        for (int i = index; i < items.size(); i++) {
            items.set(i, null);
        }
    }

    private void __________________Thao_tác_tháo_mặc_item___________________() {
        //**********************************************************************
    }

    private Item putItemBag(Player player, Item item) {
        if (item != null && item.isNotNullItem() && (isStackableItem(item) || isItemIncrementalOption(item)[0] != -1)) {
            if (addItemList(player.inventory.itemsBag, item) && item.quantity == 0) {
                return ItemService.gI().createItemNull();
            }
        }
        int activeTab = Math.max(0, Math.min(Inventory.NUM_QUICK_TABS - 1, player.inventory.currentBagTab));
        int tabStart = activeTab * Inventory.BAG_VIEW_SIZE;
        int tabEnd = Math.min(player.inventory.itemsBag.size(), tabStart + Inventory.BAG_VIEW_SIZE);
        for (int i = tabStart; i < tabEnd; i++) {
            if (!player.inventory.itemsBag.get(i).isNotNullItem()) {
                player.inventory.itemsBag.set(i, item);
                return ItemService.gI().createItemNull();
            }
        }
        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            if (!player.inventory.itemsBag.get(i).isNotNullItem()) {
                player.inventory.itemsBag.set(i, item);
                return ItemService.gI().createItemNull();
            }
        }
        return item;
    }

    private Item putItemBox(Player player, Item item) {
        if (item != null && item.isNotNullItem() && (isStackableItem(item) || isItemIncrementalOption(item)[0] != -1)) {
            if (addItemList(player.inventory.itemsBox, item) && item.quantity == 0) {
                return ItemService.gI().createItemNull();
            }
        }
        for (int i = 0; i < player.inventory.itemsBox.size(); i++) {
            if (!player.inventory.itemsBox.get(i).isNotNullItem()) {
                player.inventory.itemsBox.set(i, item);
                Item sItem = ItemService.gI().createItemNull();
                return sItem;
            }
        }
        return item;
    }

    public Item putItemBody(Player player, Item item) {
        byte type = item.getType();
        Item sItem = item;
        if (!item.isNotNullItem()) {
            return sItem;
        }

        // Kiểm tra các loại item hợp lệ
        switch (item.template.type) {
            case 0, 1, 2, 3, 4, 5, 32, 23, 24, 11, 27, 25 -> {
            }
            default -> {
                Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Trang bị không phù hợp!1");
                return sItem;
            }
        }

        // Kiểm tra giới tính của vật phẩm
        if (item.template.gender < 3 && item.template.gender != player.gender) {
            Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Trang bị không phù hợp!");
            return sItem;
        }

        // Kiểm tra các vật phẩm có ID đặc biệt
        if (item.getId() == 691 || item.getId() == 692 || item.getId() == 693) {
            List<Item> itemsBody = player.inventory.itemsBody;
            if (itemsBody.get(0).isNotNullItem() && itemsBody.get(5).isNotNullItem()) {
                Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Vui lòng cởi áo để có thể sử dụng!");
                return sItem;
            }
        }

        // Kiểm tra yêu cầu sức mạnh
        long powerRequire = item.template.strRequire;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 21) {
                powerRequire = io.param * 1000000000L;
                break;
            }
        }
        if (player.nPoint.power < powerRequire) {
            Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Sức mạnh không đủ yêu cầu!");
            return sItem;
        }
        handleOption210(item);
        checkOption231(item);
        int index = -1;
        switch (item.template.type) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                index = item.template.type;
                break;
            case 32:
                index = 6;
                break;
            case 23:
            case 24:
                index = 9;
                break;
            case 11:
                index = 8;
                break;
            case 27:
                index = 7;
                break;
            case 25:
                index = player.isPet ? 7 : 10;
                break;
        }
        // Cho phép mọi loại Đệ tử mặc Đeo lưng (type 11 -> ô 8), Sách Tuyệt Kỹ (type 25 -> ô 7) & Linh Thú (type 27 -> ô 7)
        if (player.isPet && (item.template.type == 23 || item.template.type == 24)) {
            Player recipient = ((Pet) player).master;
            if (recipient == null) {
                recipient = player;
            }
            Service.gI().sendThongBaoOK(recipient, "Đệ tử không thể sử dụng vật phẩm này!");
            return sItem;
        }

        if (player.isPet) {
            Pet pet = (Pet) player;
            while (pet.inventory.itemsBody.size() <= index) {
                pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
        } else {
            while (player.inventory.itemsBody.size() <= index) {
                player.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
        }

        sItem = player.inventory.itemsBody.get(index);
        player.inventory.itemsBody.set(index, item);
        return sItem;

    }

    public void itemBagToBody(Player player, int index) {
        if (index < 0) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = player.inventory.itemsBag.get(index);
        if (item.isNotNullItem()) {
            player.inventory.itemsBag.set(index, putItemBody(player, item));
            sendItemBags(player);
            sendItemBody(player);
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player);
            Service.gI().sendFlagBag(player);
        }
    }

    public void itemBodyToBag(Player player, int index) {
        Item item = player.inventory.itemsBody.get(index);
        if (item.isNotNullItem()) {
            if (index == 12) {
                Service.gI().sendPetFollow(player, (short) 0);
            }
            if (index == 7 && !player.isPet && item.template.type != 25) {
                if (player.newPet != null) {
                    ChangeMapService.gI().exitMap(player.newPet);
                    player.newPet.dispose();
                    player.newPet = null;
                }
            }
            player.inventory.itemsBody.set(index, putItemBag(player, item));
            Service.gI().player(player);
            sendItemBags(player);
            sendItemBody(player);
            player.zone.load_Me_To_Another(player);
            player.zone.load_Another_To_Me(player);
            Service.gI().Send_Caitrang(player);
            Service.gI().sendFlagBag(player);
            Service.gI().point(player);
        }
    }

    public void itemBagToPetBody(Player player, int index) {
        try {
            if (player.pet != null && player.pet.nPoint.power >= 1500000) {
                Item item = player.inventory.itemsBag.get(index);
                if (item.isNotNullItem()) {
                    Item itemSwap = putItemBody(player.pet, item);
                    player.inventory.itemsBag.set(index, itemSwap);
                    sendItemBags(player);
                    sendItemBody(player);
                    if (!itemSwap.equals(item)) {
                        Service.gI().point(player);
                        Service.gI().showInfoPet(player);
                    }
                    Service.gI().Send_Caitrang(player.pet);
                    Service.gI().Send_Caitrang(player);
                    Service.gI().sendFlagBag(player.pet);
                    Service.gI().sendFlagBag(player);
                }
            } else {
                Service.gI().sendThongBao(player, "Đệ tử phải đạt 1tr5 sức mạnh mới có thể mặc");
            }
        } catch (Exception E) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    public void itemPetBodyToBag(Player player, int index) {
        Item item = player.pet.inventory.itemsBody.get(index);
        if (item.isNotNullItem()) {
            player.pet.inventory.itemsBody.set(index, putItemBag(player, item));
            sendItemBags(player);
            sendItemBody(player);
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player.pet);
            Service.gI().Send_Caitrang(player);
            Service.gI().sendFlagBag(player.pet);
            Service.gI().sendFlagBag(player);
            Service.gI().showInfoPet(player);
        }
    }

    public void itemBoxToBodyOrBag(Player player, int index) {
        if (index < 0 || index >= player.inventory.itemsBox.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = player.inventory.itemsBox.get(index);
        if (item != null && item.isNotNullItem()) {
            boolean done = false;
            if (item.template.type >= 0 && item.template.type <= 5 || item.template.type == 32) {
                Item itemBody = player.inventory.itemsBody.get(item.template.type == 32 ? 6 : item.template.type);
                if (!itemBody.isNotNullItem()) {
                    if (item.template.gender == player.gender || item.template.gender == 3) {
                        long powerRequire = item.template.strRequire;
                        for (Item.ItemOption io : item.itemOptions) {
                            if (io.optionTemplate.id == 21) {
                                powerRequire = io.param * 1000000000L;
                                break;
                            }
                        }
                        if (powerRequire <= player.nPoint.power) {
                            player.inventory.itemsBody.set(item.template.type == 32 ? 6 : item.template.type, item);
                            player.inventory.itemsBox.set(index, itemBody);
                            done = true;

                            sortItems(player.inventory.itemsBox);
                            sendItemBody(player);
                            sendItemBags(player);
                            sendItemBox(player);
                            Service.gI().point(player);
                            Service.gI().Send_Caitrang(player);
                        }
                    }
                }
            }
            if (!done) {
                if (addItemBag(player, item)) {
                    if (item.quantity == 0) {
                        Item sItem = ItemService.gI().createItemNull();
                        player.inventory.itemsBox.set(index, sItem);
                    }
                    sortItems(player.inventory.itemsBag);
                    sortItems(player.inventory.itemsBox);
                    sendItemBags(player);
                    sendItemBox(player);
                } else {
                    Service.gI().sendThongBao(player, "Hành trang đã đầy!");
                }
            }
        }
    }

    public void itemBagToBox(Player player, int index) {
        if (index < 0 || index >= player.inventory.itemsBag.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = player.inventory.itemsBag.get(index);
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == 457) {
                Service.gI().sendThongBao(player, "Không thể cất vàng vào rương");
                return;
            }

            boolean added = addItemBox(player, item);

            if (added) {
                if (item.quantity == 0) {
                    Item sItem = ItemService.gI().createItemNull();
                    player.inventory.itemsBag.set(index, sItem);
                }
                sortItems(player.inventory.itemsBag);
                sortItems(player.inventory.itemsBox);
                sendItemBags(player);
                sendItemBox(player);
            } else {
                Service.gI().sendThongBao(player, "Rương đồ đã đầy!");
            }
        }
    }

    public void itemBodyToBox(Player player, int index) {
        if (index < 0 || index >= player.inventory.itemsBody.size()) {
            return;
        }
        Item item = player.inventory.itemsBody.get(index);
        if (item.isNotNullItem()) {
            player.inventory.itemsBody.set(index, putItemBox(player, item));
            sortItems(player.inventory.itemsBag);
            sortItems(player.inventory.itemsBox);
            sendItemBody(player);
            sendItemBox(player);
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player);
        }
    }

    private void __________________Gửi_danh_sách_item_cho_người_chơi________() {
        //**********************************************************************
    }

    public static void sanitizeStarOptions(Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return;
        }
        if (item.itemOptions == null) {
            item.itemOptions = new ArrayList<>();
        }
        if (item.template.id >= 441 && item.template.id <= 447 && hasOnlyPlaceholderOptions(item.itemOptions)) {
            item.itemOptions.clear();
            switch (item.template.id) {
                case 441 -> item.itemOptions.add(new Item.ItemOption(95, 5));
                case 442 -> item.itemOptions.add(new Item.ItemOption(96, 5));
                case 443 -> item.itemOptions.add(new Item.ItemOption(97, 5));
                case 444 -> item.itemOptions.add(new Item.ItemOption(98, 3));
                case 445 -> item.itemOptions.add(new Item.ItemOption(99, 3));
                case 446 -> item.itemOptions.add(new Item.ItemOption(100, 5));
                case 447 -> item.itemOptions.add(new Item.ItemOption(101, 5));
            }
            return;
        }
        if (item.itemOptions.isEmpty()) {
            return;
        }
        int maxStar = 0;
        boolean has228 = false;
        for (Item.ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null) {
                if (io.optionTemplate.id == 107) {
                    maxStar = io.param;
                } else if (io.optionTemplate.id == 228) {
                    has228 = true;
                }
            }
        }
        if (has228 && maxStar <= 7) {
            item.itemOptions.removeIf(io -> io != null && io.optionTemplate != null && io.optionTemplate.id == 228);
        }
    }

    public void sendItemBags(Player player) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null) {
            return;
        }
        while (player.inventory.itemsBag.size() < Inventory.DEFAULT_ITEMS_BAG) {
            player.inventory.itemsBag.add(ItemService.gI().createItemNull());
        }
        for (Item it : player.inventory.itemsBag) {
            sanitizeStarOptions(it);
        }
        sortItems(player.inventory.itemsBag);
        if (player.getSession() == null) {
            return;
        }
        Message msg;
        try {
            List<Integer> displayIndices = player.inventory.displayedBagIndices;
            displayIndices.clear();

            // Hàng đầu tiên (6 ô: 0..5) ngay dưới khung Trang bị là thanh nút bấm nhanh: [1] [2] [3] [4] [5] [⇄]
            for (int t = 0; t < Inventory.NUM_QUICK_TABS; t++) {
                displayIndices.add(-101 - t); // -101..-105 -> Trang 1..5 (mỗi trang 120 ô độc lập)
            }
            displayIndices.add(-106); // -106 -> Nút [⇄] Chuyển đồ giữa các Tab & Tiện ích túi đồ

            int usedBag = 0;
            for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                Item item = player.inventory.itemsBag.get(i);
                if (item != null && item.isNotNullItem() && item.template != null
                        && !nro.models.data.ItemData.isQuickTabButtonTemplateId(item.template.id)) {
                    usedBag++;
                }
            }

            if (player.inventory.pendingMoveBagIndex >= 0) {
                if (player.inventory.pendingMoveBagIndex >= player.inventory.itemsBag.size()
                        || player.inventory.itemsBag.get(player.inventory.pendingMoveBagIndex) == null
                        || !player.inventory.itemsBag.get(player.inventory.pendingMoveBagIndex).isNotNullItem()) {
                    player.inventory.pendingMoveBagIndex = -1;
                }
            }

            // Hiển thị ĐÚNG các vật phẩm thuộc Tab đang chọn (1..5, mỗi Tab độc lập 120 ô = 20 hàng x 6 cột):
            int activeTab = Math.max(0, Math.min(Inventory.NUM_QUICK_TABS - 1, player.inventory.currentBagTab));
            player.inventory.currentBagTab = activeTab;
            int bankOffset = 0;
            int tabStart = activeTab * Inventory.BAG_VIEW_SIZE;
            int tabEnd = Math.min(player.inventory.itemsBag.size(), tabStart + Inventory.BAG_VIEW_SIZE);

            for (int i = tabStart; i < tabEnd && displayIndices.size() < 126; i++) {
                Item item = player.inventory.itemsBag.get(i);
                if (item != null && item.isNotNullItem() && item.template != null
                        && !nro.models.data.ItemData.isQuickTabButtonTemplateId(item.template.id)) {
                    displayIndices.add(i);
                }
            }

            // Đảm bảo mọi vật phẩm đang chọn trong Bà Hạt Mít luôn nằm trong danh sách hiển thị (< 126)
            boolean isCombining = player.combineNew != null && player.combineNew.itemsCombine != null && !player.combineNew.itemsCombine.isEmpty();
            if (isCombining) {
                for (Item combItem : player.combineNew.itemsCombine) {
                    if (combItem != null && combItem.isNotNullItem()) {
                        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                            if (player.inventory.itemsBag.get(i) == combItem && !displayIndices.contains(i)) {
                                if (displayIndices.size() >= 126) {
                                    displayIndices.set(displayIndices.size() - 1, i);
                                } else {
                                    displayIndices.add(i);
                                }
                                break;
                            }
                        }
                    }
                }
            }

            int totalEntries = displayIndices.size();
            int displaySlots = 126;

            java.util.Map<Short, Integer> equipCounts = new java.util.HashMap<>();
            short[] slotTempIds = new short[totalEntries];
            for (int idx = 0; idx < totalEntries; idx++) {
                int bagIdx = displayIndices.get(idx);
                if (bagIdx <= -101 && bagIdx >= -106) {
                    int btnIdx = -101 - bagIdx; // 0..5
                    slotTempIds[idx] = nro.models.data.ItemData.getQuickTabButtonTemplateId(btnIdx);
                    continue;
                }
                if (bagIdx < 0 || bagIdx >= player.inventory.itemsBag.size()) {
                    slotTempIds[idx] = -1;
                    continue;
                }
                Item item = player.inventory.itemsBag.get(bagIdx);
                if (item == null || !item.isNotNullItem() || item.template == null) {
                    slotTempIds[idx] = -1;
                    continue;
                }
                short realTempId = item.template.id;
                slotTempIds[idx] = realTempId;
            }

            int curTab = activeTab + 1;
            int maxBag = Inventory.NUM_QUICK_TABS * Inventory.BAG_VIEW_SIZE;
            String pendingItemName = null;
            if (player.inventory.moveItemMode && player.inventory.pendingMoveBagIndex >= 0
                    && player.inventory.pendingMoveBagIndex < player.inventory.itemsBag.size()) {
                Item pend = player.inventory.itemsBag.get(player.inventory.pendingMoveBagIndex);
                if (pend != null && pend.isNotNullItem() && pend.template != null) {
                    pendingItemName = pend.template.name;
                }
            }

            msg = new Message(-36);
            msg.writer().writeByte(0);
            msg.writer().writeByte(displaySlots);
            for (int idx = 0; idx < totalEntries; idx++) {
                int bagIdx = displayIndices.get(idx);
                if (bagIdx <= -101 && bagIdx >= -105) {
                    int tabNum = -100 - bagIdx; // 1..5
                    int tabUsed = player.inventory.getUsedSlotsInTab(tabNum - 1);
                    boolean isCur = (tabNum == curTab);
                    msg.writer().writeShort(slotTempIds[idx]);
                    msg.writer().writeInt(1);
                    if (pendingItemName != null) {
                        msg.writer().writeUTF("Chuyển [" + pendingItemName + "] -> Trang " + tabNum
                                + " (Đang có " + tabUsed + "/120 món)");
                        msg.writer().writeUTF("Bấm Sử dụng vào nút này để chuyển ngay sang Trang " + tabNum + "!");
                    } else {
                        msg.writer().writeUTF("Trang " + tabNum + " (Đang có " + tabUsed + "/120 món)"
                                + (isCur ? " [Đang mở]" : " [Bấm Sử dụng để mở Trang " + tabNum + "]"));
                        msg.writer().writeUTF("Bấm nút thứ 6 [+] ở góc phải hàng đầu để Chuyển Đồ giữa các Tab");
                    }
                    if (isCur) {
                        msg.writer().writeByte(3);
                        msg.writer().writeByte(102); // Hiển thị số 1★..5★ ở góc trên bên trái ô
                        msg.writer().writeShort(tabNum);
                        msg.writer().writeByte(73);  // Tô nền Xanh Lá nổi bật cho Trang đang mở
                        msg.writer().writeShort(tabNum);
                        msg.writer().writeByte(31);  // Số lượng vật phẩm trong Tab
                        msg.writer().writeShort(tabUsed);
                    } else {
                        msg.writer().writeByte(2);
                        msg.writer().writeByte(102); // Hiển thị số 1★..5★ ở góc trên bên trái ô
                        msg.writer().writeShort(tabNum);
                        msg.writer().writeByte(31);
                        msg.writer().writeShort(tabUsed);
                    }
                    continue;
                }
                if (bagIdx == -106) {
                    boolean isMove = player.inventory.moveItemMode;
                    msg.writer().writeShort(slotTempIds[idx]);
                    msg.writer().writeInt(1);
                    if (isMove) {
                        msg.writer().writeUTF("Chế độ Chuyển Đồ Sang Tab Khác: [ĐANG BẬT]"
                                + (pendingItemName != null ? " (Đang chọn: " + pendingItemName + ")" : ""));
                        msg.writer().writeUTF("B1: Bấm vào món đồ cần chuyển -> B2: Bấm nút 1★..5★! (Bấm lại nút này để Tắt)");
                        msg.writer().writeByte(3);
                        msg.writer().writeByte(73); // Tô nền Xanh Lá khi đang BẬT chế độ Chuyển Đồ
                        msg.writer().writeShort(1);
                        msg.writer().writeByte(31);
                        msg.writer().writeShort(usedBag);
                        msg.writer().writeByte(30);
                        msg.writer().writeShort(0);
                    } else {
                        msg.writer().writeUTF("Chuyển Đồ Giữa Các Tab [Đang Tắt] (Tổng: " + usedBag + "/" + maxBag + " ô)");
                        msg.writer().writeUTF("Bấm Sử dụng để BẬT chuyển đồ sang Tab khác (Bỏ ra đất: Mở Menu)");
                        msg.writer().writeByte(2);
                        msg.writer().writeByte(31);
                        msg.writer().writeShort(usedBag);
                        msg.writer().writeByte(30);
                        msg.writer().writeShort(0);
                    }
                    continue;
                }
                Item item = player.inventory.itemsBag.get(bagIdx);
                if (item == null || !item.isNotNullItem()) {
                    msg.writer().writeShort(-1);
                    continue;
                }
                boolean isSelectedMove = player.inventory.moveItemMode && bagIdx == player.inventory.pendingMoveBagIndex;
                msg.writer().writeShort(slotTempIds[idx]);
                msg.writer().writeInt(item.quantity);
                msg.writer().writeUTF((isSelectedMove ? "[ĐANG CHỌN CHUYỂN TAB] " : "") + item.getInfo());
                msg.writer().writeUTF(isSelectedMove
                        ? "Hãy bấm nút 1★..5★ ở hàng đầu để chuyển sang Tab đó (Hoặc bấm lại món này để chọn Tab)!"
                        : item.getContent());
                int extraOpt = isSelectedMove ? 1 : 0;
                msg.writer().writeByte(item.itemOptions.size() + extraOpt); //options
                if (isSelectedMove) {
                    msg.writer().writeByte(73); // Tô nền Xanh Lá cho món đồ đang chọn để chuyển Tab
                    msg.writer().writeShort(1);
                }
                for (int j = 0; j < item.itemOptions.size(); j++) {
                    Item.ItemOption io = item.itemOptions.get(j);
                    if (io.optionTemplate.id == 213) {
                        int opId = 213;
                        int param = io.param;
                        if (param > 1_000_000) {
                            opId = 223;
                            param /= 1_000_000;
                        } else if (param > 1000) {
                            opId = 222;
                            param /= 1000;
                        }
                        msg.writer().writeByte(opId);
                        msg.writer().writeShort(param);
                    } else {
                        msg.writer().writeByte(io.optionTemplate.id);
                        msg.writer().writeShort(io.param);
                    }
                }
            }

            for (int empty = totalEntries; empty < displaySlots; empty++) {
                msg.writer().writeShort(-1);
            }

            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void sendItemBody(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }
        if (player.getSession() != null) {
            Message msg;
            try {
                if (player.nPoint != null) {
                    player.nPoint.calPoint();
                    player.nPoint.lastSentTlSDCM = player.nPoint.tlSDCM;
                }
                List<Item> itemsBody = player.inventory.itemsBody;
                int rawBodySdcm = 0;
                int carrierIdx = -1;
                int firstNonNullIdx = -1;
                for (int i = 0; i < itemsBody.size(); i++) {
                    Item it = itemsBody.get(i);
                    if (it != null && it.isNotNullItem() && it.itemOptions != null) {
                        if (firstNonNullIdx == -1) {
                            firstNonNullIdx = i;
                        }
                        for (Item.ItemOption io : it.itemOptions) {
                            if (io == null || io.optionTemplate == null) {
                                continue;
                            }
                            int opId = io.optionTemplate.id;
                            if (opId == 5) {
                                rawBodySdcm += io.param;
                            }
                            if (carrierIdx == -1 && i < 5 && ((opId >= 237 && opId <= 248) || (opId >= 127 && opId <= 144))) {
                                carrierIdx = i;
                            }
                        }
                    }
                }
                if (carrierIdx == -1) {
                    carrierIdx = firstNonNullIdx;
                }
                int bonusSdcm = (player.nPoint != null) ? Math.max(0, player.nPoint.tlSDCM - rawBodySdcm) : 0;

                msg = new Message(-37);
                msg.writer().writeByte(0);
                msg.writer().writeShort(player.getHead());
                msg.writer().writeByte(itemsBody.size());
                for (int idx = 0; idx < itemsBody.size(); idx++) {
                    Item item = itemsBody.get(idx);
                    if (!item.isNotNullItem()) {
                        msg.writer().writeShort(-1);
                    } else {
                        sanitizeStarOptions(item);
                        msg.writer().writeShort(item.template.id);
                        msg.writer().writeInt(item.quantity);
                        msg.writer().writeUTF(item.getInfo());
                        msg.writer().writeUTF(item.getContent());
                        List<Item.ItemOption> itemOptions = item.itemOptions;
                        boolean addBonusHere = (idx == carrierIdx && bonusSdcm > 0);
                        boolean hasExistingOp5 = false;
                        if (addBonusHere) {
                            for (Item.ItemOption io : itemOptions) {
                                if (io != null && io.optionTemplate != null && io.optionTemplate.id == 5) {
                                    hasExistingOp5 = true;
                                    break;
                                }
                            }
                        }
                        int optCountToWrite = itemOptions.size() + (addBonusHere && !hasExistingOp5 ? 1 : 0);
                        msg.writer().writeByte(optCountToWrite);
                        boolean mergedOp5 = false;
                        for (Item.ItemOption itemOption : itemOptions) {
                            if (itemOption.optionTemplate.id == 213) {
                                int opId = 213;
                                int param = itemOption.param;
                                if (param > 1_000_000) {
                                    opId = 223;
                                    param /= 1_000_000;
                                } else if (param > 1000) {
                                    opId = 222;
                                    param /= 1000;
                                }
                                msg.writer().writeByte(opId);
                                msg.writer().writeShort(param);
                            } else if (addBonusHere && !mergedOp5 && itemOption.optionTemplate.id == 5) {
                                mergedOp5 = true;
                                msg.writer().writeByte(5);
                                msg.writer().writeShort(itemOption.param + bonusSdcm);
                            } else {
                                msg.writer().writeByte(itemOption.optionTemplate.id);
                                msg.writer().writeShort(itemOption.param);
                            }
                        }
                        if (addBonusHere && !hasExistingOp5) {
                            msg.writer().writeByte(5);
                            msg.writer().writeShort(bonusSdcm);
                        }
                    }
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
            }
        }
        Service.gI().Send_Caitrang(player);
    }

    public void sendItemBox(Player player) {
        sendItemBoxSilent(player);
        this.openBox(player);
    }

    public void sendItemBoxSilent(Player player) {
        if (player == null || player.getSession() == null || player.inventory == null) {
            return;
        }
        for (Item it : player.inventory.itemsBox) {
            sanitizeStarOptions(it);
        }
        sortItems(player.inventory.itemsBox);
        Message msg;
        try {
            msg = new Message(-35);
            msg.writer().writeByte(0);
            int boxSize = Math.min(player.inventory.itemsBox.size(), 120);
            msg.writer().writeByte(boxSize);
            for (int i = 0; i < boxSize; i++) {
                Item it = player.inventory.itemsBox.get(i);
                msg.writer().writeShort(it.isNotNullItem() ? it.template.id : -1);
                if (it.isNotNullItem()) {
                    msg.writer().writeInt(it.quantity);
                    msg.writer().writeUTF(it.getInfo());
                    msg.writer().writeUTF(it.getContent());
                    msg.writer().writeByte(it.itemOptions.size());
                    for (Item.ItemOption io : it.itemOptions) {
                        if (io.optionTemplate.id == 213) {
                            int opId = 213;
                            int param = io.param;
                            if (param > 1_000_000) {
                                opId = 223;
                                param /= 1_000_000;
                            } else if (param > 1000) {
                                opId = 222;
                                param /= 1000;
                            }
                            msg.writer().writeByte(opId);
                            msg.writer().writeShort(param);
                        } else {
                            msg.writer().writeByte(io.optionTemplate.id);
                            msg.writer().writeShort(io.param);
                        }
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void openBox(Player player) {
        Message msg;
        try {
            msg = new Message(-35);
            msg.writer().writeByte(1);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    private void __________________Thêm_vật_phẩm_vào_danh_sách______________() {
        //**********************************************************************
    }

    private boolean addItemSpecial(Player player, Item item) {
        //bùa
        if (item.template.type == 13) {
            int min = 0;
            try {
                String tagShopBua = player.idMark.getShopOpen().tagName;
                if (tagShopBua.equals("BUA_1H")) {
                    min = 60;
                } else if (tagShopBua.equals("BUA_8H")) {
                    min = 60 * 8;
                } else if (tagShopBua.equals("BUA_1M")) {
                    min = 60 * 24 * 30;
                }
            } catch (Exception e) {
            }
            player.charms.addTimeCharms(item.template.id, min);
            return true;
        }

        switch (item.template.id) {
            case 453: //tàu tennis
                player.haveTennisSpaceShip = true;
                return true;
            case 74: //đùi gà nướng
                player.nPoint.setFullHpMp();
                PlayerService.gI().sendInfoHpMp(player);
                return true;
        }
        return false;
    }

    public boolean addItemBag(Player player, Item item) {
        //ngọc rồng đen
        if (ItemMapService.gI().isBlackBall(item.template.id)) {
            return BlackBallWarService.gI().pickBlackBall(player, item);
        }

        //ngọc rồng namek
        if (ItemMapService.gI().isNamecBall(item.template.id) || ItemMapService.gI().isNamecBallStone(item.template.id)) {
            return NgocRongNamecService.gI().pickNamekBall(player, item);
        }
        if (addItemSpecial(player, item)) {
            return true;
        }

        //gold, gem, ruby
        switch (item.template.type) {
            case 9:
                if (player.inventory.gold + item.quantity <= Inventory.LIMIT_GOLD) {
                    if (player.effectSkill.isChibi && player.typeChibi == 0) {
                        player.inventory.gold += item.quantity;
                    }
                    player.inventory.gold += item.quantity;
                    Service.gI().sendMoney(player);
                    return true;
                } else {
                    Service.gI().sendThongBao(player, "Vàng sau khi nhặt quá giới hạn cho phép");
                    return false;
                }
            case 10:
                long gem = (long) player.inventory.gem + (long) item.quantity;
                if (gem > Integer.MAX_VALUE) {
                    gem = Integer.MAX_VALUE;
                }
                player.inventory.gem = (int) gem;
                Service.gI().sendMoney(player);
                return true;
            case 34:
                long ruby = (long) player.inventory.ruby + (long) item.quantity;
                if (ruby > Integer.MAX_VALUE) {
                    ruby = Integer.MAX_VALUE;
                }
                player.inventory.ruby = (int) ruby;
                Service.gI().sendMoney(player);
                return true;
        }

        //mở rộng hành trang - rương đồ
        if (item.template.id == 517) {
            if (player.inventory.itemsBag.size() < Inventory.MAX_ITEMS_BAG) {
                int addSlots = Math.min(10, Inventory.MAX_ITEMS_BAG - player.inventory.itemsBag.size());
                player.inventory.restoreBagLogicalOrder();
                for (int i = 0; i < addSlots; i++) {
                    player.inventory.itemsBag.add(ItemService.gI().createItemNull());
                }
                sendItemBags(player);
                Service.gI().sendThongBaoOK(player, "Hành trang của bạn đã được mở rộng thêm " + addSlots + " ô!\nTổng: "
                        + player.inventory.itemsBag.size() + "/" + Inventory.MAX_ITEMS_BAG + " ô (Đang mở Trang "
                        + (player.inventory.currentBagTab + 1) + "/" + player.inventory.getTotalBagTabs() + ")");
                return true;
            } else {
                Service.gI().sendThongBaoOK(player, "Hành trang của bạn đã đạt tối đa (" + Inventory.MAX_ITEMS_BAG + " ô / 5 Trang)");
                return false;
            }
        } else if (item.template.id == 518) {
            if (player.inventory.itemsBox.size() < Inventory.MAX_ITEMS_BOX) {
                int addSlots = Math.min(10, Inventory.MAX_ITEMS_BOX - player.inventory.itemsBox.size());
                for (int i = 0; i < addSlots; i++) {
                    player.inventory.itemsBox.add(ItemService.gI().createItemNull());
                }
                sendItemBox(player);
                Service.gI().sendThongBaoOK(player, "Rương đồ của bạn đã được mở rộng thêm " + addSlots + " ô!\nTổng: "
                        + player.inventory.itemsBox.size() + "/" + Inventory.MAX_ITEMS_BOX + " ô");
                return true;
            } else {
                Service.gI().sendThongBaoOK(player, "Rương đồ của bạn đã đạt tối đa (" + Inventory.MAX_ITEMS_BOX + " ô)");
                return false;
            }
        }
        // Check item rồng nhí vĩnh viễn
        if (item.template.id >= 1765 && item.template.id <= 1771) {
            boolean check_options = false;
            for (Item.ItemOption op : item.itemOptions) {
                if (op.optionTemplate != null && op.optionTemplate.id == 93) {
                    check_options = true;
                    break;
                }
            }
            if (!check_options) {
                BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.ME_RONG, 1);
            }
        }
        return addItemList(player.inventory.itemsBag, item);
    }

    public boolean addItemBox(Player player, Item item) {
        return addItemList(player.inventory.itemsBox, item);
    }

    public boolean addItemList(List<Item> items, Item itemAdd) {
        if (itemAdd == null || !itemAdd.isNotNullItem()) {
            return false;
        }
        if (itemAdd.itemOptions.isEmpty()) {
            itemAdd.itemOptions.add(new Item.ItemOption(73, 0));
        }
        sanitizeStarOptions(itemAdd);

        boolean mergedAny = false;
        for (Item it : items) {
            if (it != null && it.isNotNullItem() && it != itemAdd && itemAdd.quantity > 0) {
                int beforeQty = itemAdd.quantity;
                int[] beforeInc = isItemIncrementalOption(itemAdd);
                if (tryMergeStack(it, itemAdd)) {
                    itemAdd.quantity = 0;
                    return true;
                }
                int[] afterInc = isItemIncrementalOption(itemAdd);
                if (itemAdd.quantity < beforeQty || (beforeInc[0] != -1 && afterInc[1] < beforeInc[1])) {
                    mergedAny = true;
                }
            }
        }

        //add item vào ô mới nếu còn dư hoặc không thể cộng dồn
        if (itemAdd.quantity > 0) {
            if (items instanceof Inventory.BagList bagList && bagList.getOwner() != null) {
                int activeTab = Math.max(0, Math.min(Inventory.NUM_QUICK_TABS - 1, bagList.getOwner().currentBagTab));
                int tabStart = activeTab * Inventory.BAG_VIEW_SIZE;
                int tabEnd = Math.min(items.size(), tabStart + Inventory.BAG_VIEW_SIZE);
                for (int i = tabStart; i < tabEnd; i++) {
                    if (!items.get(i).isNotNullItem()) {
                        items.set(i, ItemService.gI().copyItem(itemAdd));
                        itemAdd.quantity = 0;
                        return true;
                    }
                }
            }
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).isNotNullItem()) {
                    items.set(i, ItemService.gI().copyItem(itemAdd));
                    itemAdd.quantity = 0;
                    return true;
                }
            }
        }
        return mergedAny;
    }

    public static boolean checkListsEqual(List<ItemOption> list1, List<ItemOption> list2) {
        if (list1 == null && list2 == null) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        List<ItemOption> f1 = new ArrayList<>();
        for (ItemOption io : list1) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id != 30 && io.optionTemplate.id != 73 && io.optionTemplate.id != 174) {
                f1.add(io);
            }
        }
        List<ItemOption> f2 = new ArrayList<>();
        for (ItemOption io : list2) {
            if (io != null && io.optionTemplate != null && io.optionTemplate.id != 30 && io.optionTemplate.id != 73 && io.optionTemplate.id != 174) {
                f2.add(io);
            }
        }
        if (f1.size() != f2.size()) {
            return false;
        }
        boolean[] matched = new boolean[f2.size()];
        for (ItemOption io1 : f1) {
            boolean found = false;
            for (int j = 0; j < f2.size(); j++) {
                if (!matched[j] && io1.optionTemplate.id == f2.get(j).optionTemplate.id && io1.param == f2.get(j).param) {
                    matched[j] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private void __________________Kiểm_tra_điều_kiện_vật_phẩm______________() {
        //**********************************************************************
    }

    /**
     * Kiểm tra vật phẩm có phải là vật phẩm tăng chỉ số option hay không
     *
     * @param item
     * @return id option tăng chỉ số - param
     */
    private int[] isItemIncrementalOption(Item item) {
        for (Item.ItemOption io : item.itemOptions) {
            switch (io.optionTemplate.id) {
                case 1:
                    return new int[]{io.optionTemplate.id, io.param};
                case 31:
                    return new int[]{io.optionTemplate.id, io.param};
            }
        }
        return new int[]{-1, -1};
    }

    private void __________________Kiểm_tra_danh_sách_còn_chỗ_trống_________() {
        //**********************************************************************
    }

    public byte getCountEmptyBag(Player player) {
        return getCountEmptyListItem(player.inventory.itemsBag);
    }

    public int getCountEmptyBagTotal(Player player) {
        int count = 0;
        for (Item item : player.inventory.itemsBag) {
            if (!item.isNotNullItem()) {
                count++;
            }
        }
        return count;
    }

    public byte getCountEmptyListItem(List<Item> list) {
        int count = 0;
        for (Item item : list) {
            if (!item.isNotNullItem()) {
                count++;
            }
        }
        return (byte) Math.min(count, Byte.MAX_VALUE);
    }

    public byte getIndexBag(Player pl, Item it) {
        if (pl == null || pl.inventory == null || it == null || !it.isNotNullItem()) {
            return -1;
        }
        List<Integer> displayed = pl.inventory.displayedBagIndices;
        if (!displayed.isEmpty()) {
            for (int i = 0; i < displayed.size(); i++) {
                int idx = displayed.get(i);
                if (idx >= 0 && idx < pl.inventory.itemsBag.size()) {
                    Item item = pl.inventory.itemsBag.get(idx);
                    if (item != null && item.isNotNullItem() && it.equals(item)) {
                        return (byte) i;
                    }
                }
            }
            return -1;
        }
        int limit = pl.inventory.getActiveTabSize();
        for (int i = 0; i < limit && i < pl.inventory.itemsBag.size(); ++i) {
            Item item = pl.inventory.itemsBag.get(i);
            if (item != null && item.isNotNullItem() && it.equals(item)) {
                return (byte) i;
            }
        }
        return -1;
    }

    public boolean finditemWoodChest(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 570) {
                return false;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 570) {
                return false;
            }
        }
        return true;
    }

    public int getParam(Player player, int idoption, int itemID) {
        int total = 0;
        for (Item it : player.inventory.itemsBag) {
            if (it != null && it.itemOptions != null && it.isNotNullItem() && it.template.id == itemID) {
                int optVal = 0;
                for (ItemOption iop : it.itemOptions) {
                    if (iop != null && iop.optionTemplate != null && iop.optionTemplate.id == idoption) {
                        optVal = iop.param;
                        break;
                    }
                }
                if (idoption == 31) {
                    total += Math.max(optVal, it.quantity);
                } else if (optVal > 0) {
                    return optVal;
                }
            }
        }
        return total;
    }

    public void subParamItemsBag(Player player, int itemID, int idoption, int param) {
        int remaining = param;
        List<Item> itemsToRemove = new ArrayList<>();
        for (Item it : player.inventory.itemsBag) {
            if (it != null && it.isNotNullItem() && it.template.id == itemID && remaining > 0) {
                ItemOption targetOp = null;
                if (it.itemOptions != null) {
                    for (ItemOption op : it.itemOptions) {
                        if (op != null && op.optionTemplate != null && op.optionTemplate.id == idoption) {
                            targetOp = op;
                            break;
                        }
                    }
                }
                if (targetOp != null && (idoption != 31 || targetOp.param >= it.quantity)) {
                    int take = Math.min(remaining, targetOp.param);
                    targetOp.param -= take;
                    remaining -= take;
                    if (idoption == 31 && it.quantity > 1) {
                        it.quantity = Math.max(1, targetOp.param);
                    }
                    if (targetOp.param <= 0) {
                        itemsToRemove.add(it);
                    }
                } else if (idoption == 31) {
                    int take = Math.min(remaining, it.quantity);
                    it.quantity -= take;
                    remaining -= take;
                    if (targetOp != null) {
                        targetOp.param = it.quantity;
                    }
                    if (it.quantity <= 0) {
                        itemsToRemove.add(it);
                    }
                }
            }
        }
        for (Item itemRemove : itemsToRemove) {
            removeItem(player.inventory.itemsBag, itemRemove);
        }
    }

    public boolean findItemBongTaiCap2(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 921) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 921) {
                return true;
            }
        }
        return false;
    }
    public boolean findItemBongTaiCap3(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 1819) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 1819) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemBongTai(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && (item.template.id == 921 || item.template.id == 454)) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && (item.template.id == 921 || item.template.id == 454)) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemSkinQuyLaoKame(Player player) {
        for (Item item : player.inventory.itemsBody) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemNTK(Player player) {
        if (player.isPl()) {
            for (Item item : player.inventory.itemsBag) {
                if (item.isNotNullItem() && item.template.id == 992) {
                    return true;
                }
            }
            for (Item item : player.inventory.itemsBox) {
                if (item.isNotNullItem() && item.template.id == 992) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean findItemTVC(Player player) {
        if (player.isPl()) {
            for (Item item : player.inventory.itemsBag) {
                if (item.isNotNullItem() && item.template.id == 2077) {
                    return true;
                }
            }
            for (Item item : player.inventory.itemsBox) {
                if (item.isNotNullItem() && item.template.id == 2077) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean findItemTatVoGiangSinh(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 649) {
                return true;
            }
        }
        return false;
    }

    public boolean findSenzu(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.type == 6) {
                return true;
            }
        }
        return false;
    }

    public boolean fullSetThan(Player player) {
        for (int i = 0; i < 5; i++) {
            Item item = player.inventory.itemsBody.get(i);
            if (item == null || item.template == null || item.template.level != 13) {
                return false;
            }
        }
        return true;
    }

    public boolean x99ThucAn(Player player) {
        Item doAn = player.inventory.itemsBag.stream().filter(it -> it != null && it.template != null && (it.template.id == 663 || it.template.id == 664 || it.template.id == 665 || it.template.id == 666 || it.template.id == 667) && it.quantity >= 99).findFirst().orElse(null);
        return doAn != null;
    }

    public boolean canOpenBillShop(Player player) {
        return fullSetThan(player) && x99ThucAn(player);
    }

    public boolean optionCanUpgrade(int id) {
        return id == 0 || id == 22 || id == 23 || id == 14 || id == 27 || id == 28 || id == 47;
    }

    public int getIndexItem(Player player, List<Item> items, Item item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    public int getIndexItemBag(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBag, item);
    }

    public int getIndexItemBody(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBody, item);
    }

    public int getIndexItemBox(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBox, item);
    }

    private void handleOption210(Item item) {
        if (item != null && item.template != null && item.template.type == 5) {
            ItemService.gI().upgradeFlatCostumeIfNeeded(item);
            return;
        }
        for (int i = 0; i < item.itemOptions.size(); i++) {
            Item.ItemOption io = item.itemOptions.get(i);
            if (io.optionTemplate.id == 210) {
                int option210Index = i;
                item.itemOptions.remove(i);
                int numberOfOptionsToAdd = io.param;
                int[] allOptions = {5, 14, 94, 95, 96, 98, 99, 108};
                List<Integer> selectedOptions = new ArrayList<>();
                while (selectedOptions.size() < numberOfOptionsToAdd && selectedOptions.size() < allOptions.length) {
                    int randomIndex = (int) (Math.random() * allOptions.length);
                    int selectedOption = allOptions[randomIndex];
                    if (!selectedOptions.contains(selectedOption)) {
                        selectedOptions.add(selectedOption);
                    }
                }
                List<Item.ItemOption> newOptions = new ArrayList<>();
                for (int option : selectedOptions) {
                    int newParam = (option == 14) ? (4 + (int) (Math.random() * 6)) : (6 + (int) (Math.random() * 10));
                    newOptions.add(new Item.ItemOption(option, newParam));
                }
                item.itemOptions.addAll(option210Index, newOptions);
                break;
            }
        }
    }

    public boolean findItem(Player player, int id) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == id) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == id) {
                return true;
            }
        }
        return false;
    }

    private void checkOption231(Item item) {
        for (int i = 0; i < item.itemOptions.size(); i++) {
            Item.ItemOption io = item.itemOptions.get(i);
            if (io.optionTemplate.id == 231) {
                item.itemOptions.remove(i);
                double randomValue = Math.random();
                if (randomValue <= 0.99) {
                    int[] validParams = {3, 7, 15, 21};
                    int selectedParam = validParams[(int) (Math.random() * validParams.length)];
                    item.itemOptions.add(new Item.ItemOption(93, selectedParam));
                }
                break;
            }
        }
    }
}
