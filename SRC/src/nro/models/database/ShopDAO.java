package nro.models.database;
import nro.models.item.Item;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.server.Manager;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;
import nro.models.services.ItemService;
import nro.models.utils.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ShopDAO {

    public static List<Shop> getShops(Connection con) {
        List<Shop> list = new ArrayList<>();
        try {
            PreparedStatement ps = con.prepareStatement("select * from shop order by npc_id asc");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Shop shop = new Shop();
                shop.id = rs.getInt("id");
                shop.npcId = rs.getByte("npc_id");
                shop.tagName = rs.getString("tag_name");
                shop.typeShop = rs.getByte("type_shop");
                loadShopTab(con, shop);
                list.add(shop);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
            populateAllCostumesIntoSanta(list);
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
        return list;
    }

    private static void populateAllCostumesIntoSanta(List<Shop> shops) {
        Shop santaShop = null;
        Map<Short, ItemShop> existingCostumeShops = new HashMap<>();

        for (Shop s : shops) {
            if ("SANTA".equals(s.tagName)) {
                santaShop = s;
            }
            for (TabShop tab : s.tabShops) {
                for (ItemShop is : tab.itemShops) {
                    if (is.temp != null && is.temp.type == 5 && !existingCostumeShops.containsKey(is.temp.id)) {
                        existingCostumeShops.put(is.temp.id, is);
                    }
                }
            }
        }

        if (santaShop == null) {
            return;
        }

        // Thu thập toàn bộ cải trang (type == 5, có đủ head, body, leg) theo thứ tự ID từ cũ đến mới
        List<ItemShop> allCostumes = new ArrayList<>();
        int autoId = 100000;
        for (ItemTemplate temp : Manager.ITEM_TEMPLATES) {
            if (temp != null && temp.type == 5 && temp.head != -1 && temp.body != -1 && temp.leg != -1) {
                ItemShop itemShop = new ItemShop();
                itemShop.id = autoId++;
                itemShop.temp = temp;
                itemShop.isNew = true;
                itemShop.typeSell = 1; // Bán bằng Ngọc xanh
                itemShop.cost = 10;    // Giá mặc định 10 Ngọc
                itemShop.iconSpec = 0;

                ItemShop existing = existingCostumeShops.get(temp.id);
                if (existing != null && existing.options != null && !existing.options.isEmpty()) {
                    for (Item.ItemOption op : existing.options) {
                        itemShop.options.add(new Item.ItemOption(op));
                    }
                    if (existing.typeSell == 0 || existing.typeSell == 1) {
                        itemShop.typeSell = existing.typeSell;
                        itemShop.cost = existing.cost;
                    }
                } else {
                    itemShop.options.add(new Item.ItemOption(50, 30));  // +30% Sức đánh
                    itemShop.options.add(new Item.ItemOption(77, 30));  // +30% HP
                    itemShop.options.add(new Item.ItemOption(103, 30)); // +30% KI
                }
                allCostumes.add(itemShop);
            }
        }

        // Chia thành các Tab (mỗi Tab tối đa 90 cải trang để không vượt giới hạn byte 127 của Client)
        int maxPerTab = 90;
        List<TabShop> newSantaTabs = new ArrayList<>();
        boolean insertedCostumeTabs = false;
        int nextTabId = 1800;

        for (TabShop oldTab : santaShop.tabShops) {
            if (oldTab.id == 18) {
                if (!insertedCostumeTabs) {
                    int tabIndex = 1;
                    for (int i = 0; i < allCostumes.size(); i += maxPerTab) {
                        TabShop ctTab = new TabShop();
                        ctTab.shop = santaShop;
                        ctTab.id = (tabIndex == 1) ? 18 : (nextTabId++);
                        ctTab.name = "Cải Trang\n" + tabIndex;
                        int end = Math.min(i + maxPerTab, allCostumes.size());
                        for (int j = i; j < end; j++) {
                            ItemShop is = allCostumes.get(j);
                            is.tabShop = ctTab;
                            ctTab.itemShops.add(is);
                        }
                        newSantaTabs.add(ctTab);
                        tabIndex++;
                    }
                    insertedCostumeTabs = true;
                }
            } else {
                newSantaTabs.add(oldTab);
            }
        }

        if (!insertedCostumeTabs && !allCostumes.isEmpty()) {
            int tabIndex = 1;
            for (int i = 0; i < allCostumes.size(); i += maxPerTab) {
                TabShop ctTab = new TabShop();
                ctTab.shop = santaShop;
                ctTab.id = (tabIndex == 1) ? 18 : (nextTabId++);
                ctTab.name = "Cải Trang\n" + tabIndex;
                int end = Math.min(i + maxPerTab, allCostumes.size());
                for (int j = i; j < end; j++) {
                    ItemShop is = allCostumes.get(j);
                    is.tabShop = ctTab;
                    ctTab.itemShops.add(is);
                }
                newSantaTabs.add(ctTab);
                tabIndex++;
            }
        }

        santaShop.tabShops = newSantaTabs;
    }

    private static void loadShopTab(Connection con, Shop shop) {
        try {
            PreparedStatement ps = con.prepareStatement("select * from tab_shop where shop_id = ? order by id");
            ps.setInt(1, shop.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TabShop tab = new TabShop();
                tab.shop = shop;
                tab.id = rs.getInt("id");
                tab.name = rs.getString("name").replaceAll("<>", "\n");
                loadItemShop(con, tab);
                shop.tabShops.add(tab);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }

    private static void loadItemShop(Connection con, TabShop tabShop) {
        try {
            PreparedStatement ps = con.prepareStatement("select * from item_shop where is_sell = 1 and tab_id = ? "
                    + "order by create_time desc");
            int id = tabShop.id;
            if (id >= 41 && id <= 43){//10,11,12
                id -= 31;
            }
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemShop itemShop = new ItemShop();
                itemShop.tabShop = tabShop;
                itemShop.id = rs.getInt("id");
                itemShop.temp = ItemService.gI().getTemplate(rs.getShort("temp_id"));
                itemShop.isNew = rs.getBoolean("is_new");
                itemShop.cost = rs.getInt("cost");
                itemShop.iconSpec = rs.getInt("icon_spec");
                itemShop.typeSell = rs.getByte("type_sell");
                loadItemShopOption(con, itemShop);
                tabShop.itemShops.add(itemShop);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }

    private static void loadItemShopOption(Connection con, ItemShop itemShop) {
        try {
            PreparedStatement ps = con.prepareStatement("select * from item_shop_option where item_shop_id = ?");
            ps.setInt(1, itemShop.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                itemShop.options.add(new Item.ItemOption(rs.getInt("option_id"), rs.getInt("param")));
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }
}
