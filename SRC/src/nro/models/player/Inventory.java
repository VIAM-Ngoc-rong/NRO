package nro.models.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nro.models.consts.ConstTaskBadges;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.task.BadgesTaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class Inventory {

    public static final long LIMIT_GOLD = 100_000_000_000L;
    public static final int DEFAULT_ITEMS_BAG = 1000;
    public static final int MAX_ITEMS_BAG = 1000;
    public static final int BAG_TAB_SIZE = 200;
    public static final int BAG_PAGE_SIZE = 200;
    public static final int BAG_VIEW_SIZE = 120;
    public static final int BAG_SUBPAGE_SIZE = 100;
    public static final int NUM_QUICK_TABS = 5;
    public static final int MAX_ITEMS_BOX = 120;

    public static class BagList extends ArrayList<Item> {
        private final Inventory owner;

        public BagList(Inventory owner) {
            this.owner = owner;
        }

        public int getFirstChunkSize() {
            return this.size();
        }

        public Inventory getOwner() {
            return owner;
        }
    }

    public int currentBagTab = 0;
    public int currentBagSubPage = 0;
    public boolean moveItemMode = false;
    public int pendingMoveBagIndex = -1;
    public final List<Integer> displayedBagIndices = new ArrayList<>();
    public Item trainArmor;
    public List<String> giftCode;
    public List<Item> itemsBody;
    public List<Item> itemsBag;
    public List<Item> itemsBox;

    public List<Item> itemsBoxCrackBall;
    public List<Item> itemsDaBan;

    public long gold;
    public int gem;
    public int ruby;
    public int coupon;
    public int event;
    public Iterable<Item> items;

    public Inventory() {
        itemsBody = new ArrayList<>();
        itemsBag = new BagList(this);
        itemsBox = new ArrayList<>();
        itemsBoxCrackBall = new ArrayList<>();
        itemsDaBan = new ArrayList<>();
        giftCode = new ArrayList<>();
    }

    public int getTotalBagTabs() {
        return NUM_QUICK_TABS;
    }

    public int getTabSize(int tabIndex) {
        if (this.itemsBag == null || tabIndex < 0) {
            return 0;
        }
        int perTab = Math.max(BAG_PAGE_SIZE, (this.itemsBag.size() + NUM_QUICK_TABS - 1) / NUM_QUICK_TABS);
        int start = tabIndex * perTab;
        if (start >= this.itemsBag.size()) {
            return 0;
        }
        return Math.min(perTab, this.itemsBag.size() - start);
    }

    public int getActiveTabSize() {
        return this.itemsBag != null ? this.itemsBag.size() : 0;
    }

    public synchronized void restoreBagLogicalOrder() {
        // Bể chung 1 túi: không còn xoay vật lý danh sách itemsBag
    }

    public synchronized boolean switchBagTab(int newTab) {
        if (this.itemsBag == null) {
            return false;
        }
        if (newTab < 0 || newTab >= NUM_QUICK_TABS) {
            return false;
        }
        this.currentBagTab = newTab;
        this.currentBagSubPage = 0;
        return true;
    }

    public Item getBagItemInLogicalOrder(int logicalIndex) {
        if (this.itemsBag == null || logicalIndex < 0 || logicalIndex >= this.itemsBag.size()) {
            return null;
        }
        return this.itemsBag.get(logicalIndex);
    }

    public int getUsedSlotsInBag() {
        if (this.itemsBag == null) {
            return 0;
        }
        int used = 0;
        for (Item it : this.itemsBag) {
            if (it != null && it.isNotNullItem()) {
                used++;
            }
        }
        return used;
    }

    public int getUsedSlotsInTab(int tabIndex) {
        if (this.itemsBag == null || tabIndex < 0 || tabIndex >= NUM_QUICK_TABS) {
            return 0;
        }
        int start = tabIndex * BAG_VIEW_SIZE;
        int end = Math.min(this.itemsBag.size(), start + BAG_VIEW_SIZE);
        int used = 0;
        for (int i = start; i < end; i++) {
            Item it = this.itemsBag.get(i);
            if (it != null && it.isNotNullItem()) {
                used++;
            }
        }
        return used;
    }

    public int resolveBagIndex(int clientIndex) {
        if (clientIndex < 0) {
            return clientIndex;
        }
        if (!this.displayedBagIndices.isEmpty()) {
            if (clientIndex < this.displayedBagIndices.size()) {
                return this.displayedBagIndices.get(clientIndex);
            }
            return -1;
        }
        return clientIndex;
    }

    public int getGem() {
        return this.gem;
    }

    public int getParam(Item it, int id) {
        for (ItemOption op : it.itemOptions) {
            if (op != null && op.optionTemplate.id == id) {
                return op.param;
            }
        }
        return 0;
    }

    public boolean haveOption(List<Item> l, int index, int id) {
        Item it = l.get(index);
        if (it != null && it.isNotNullItem()) {
            return it.itemOptions.stream().anyMatch(op -> op != null && op.optionTemplate.id == id);
        }
        return false;
    }

    public void subGem(int num) {
        this.gem -= num;
    }

    public void subGold(long num) {
        this.gold -= num;
        if (this.gold < 0) {
            this.gold = 0;
        }
    }

    public void addGold(long gold) {
        this.gold += gold;
        if (this.gold > LIMIT_GOLD) {
            this.gold = LIMIT_GOLD;
        }
    }

    public void dispose() {
        if (this.trainArmor != null) {
            this.trainArmor.dispose();
        }
        this.trainArmor = null;
        if (this.itemsBody != null) {
            for (Item it : this.itemsBody) {
                it.dispose();
            }
            this.itemsBody.clear();
        }
        if (this.itemsBag != null) {
            for (Item it : this.itemsBag) {
                it.dispose();
            }
            this.itemsBag.clear();
        }
        if (this.itemsBox != null) {
            for (Item it : this.itemsBox) {
                it.dispose();
            }
            this.itemsBox.clear();
        }
        if (this.itemsBoxCrackBall != null) {
            for (Item it : this.itemsBoxCrackBall) {
                it.dispose();
            }
            this.itemsBoxCrackBall.clear();
        }
        if (this.itemsDaBan != null) {
            for (Item it : this.itemsDaBan) {
                it.dispose();
            }
            this.itemsDaBan.clear();
        }
        this.itemsBody = null;
        this.itemsBag = null;
        this.itemsBox = null;
        this.itemsBoxCrackBall = null;
        this.itemsDaBan = null;
    }

    public void checkAndUpdateMeRongBadges(Player player) {
        Set<Integer> checkedItemIds = new HashSet<>();

        List<List<Item>> inventories = Arrays.asList(
                this.itemsBag,
                this.itemsBox,
                this.itemsBody
        );

        for (List<Item> inventory : inventories) {
            for (Item item : inventory) {
                if (item != null && isPermanent(item)) {
                    int itemId = item.template.id;
                    if (itemId >= 1765 && itemId <= 1771 && !checkedItemIds.contains(itemId)) {
                        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.ME_RONG, 1);
                        checkedItemIds.add(itemId);
                    }
                }
            }
        }
    }

    private boolean isPermanent(Item item) {
        return item != null && item.getOptionById(93) == null;
    }
}
