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
            populateBackItemsIntoShopDoiDiem(list);
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
        return list;
    }

    private static int getRubyCostForPlanetCostume(int tempId) {
        return switch (tempId) {
            case 710 -> 1250;  // Quy Lão Kamê (26% SĐ, 25% HP, 28% KI)
            case 738 -> 1350;  // Dr. Lychee (26% SĐ, 26% HP, 34% KI)
            case 1744 -> 1350; // Cađíc SSJ2 M (26% SĐ, 34% HP, 25% KI)
            case 711 -> 1500;  // Jacky Chun (27% SĐ, 26% HP, 28% KI, Kháng TDHS)
            case 1567 -> 1650; // CT Frieren (27% SĐ, 26% HP, 36% KI)
            case 1693 -> 1650; // Cađíc SSJ4 (27% SĐ, 36% HP, 26% KI)
            case 1553 -> 1950; // CT Goku SSJ4 (32% SĐ, 28% HP, 27% KI, Hút 15%)
            case 898 -> 1950;  // Cải trang Zamasu (32% SĐ, 28% HP, 27% KI, Hút 15%)
            case 1783 -> 1950; // CT Broly (32% SĐ, 28% HP, 27% KI, Hút 15%)
            case 1454 -> 2200; // Super Black Goku Rose (32% SĐ, 28% HP, 28% KI, Hút 18%)
            case 1590 -> 2500; // CT Goku SSJ Blue (33% SĐ, 28% HP, 28% KI, Hút 20%, XGCC 15%)
            case 1396 -> 2500; // Cải trang Super Píc (33% SĐ, 28% HP, 28% KI, Hút 20%, XGCC 15%)
            case 1275 -> 2500; // Cumber SSJ (33% SĐ, 28% HP, 28% KI, Hút 20%, XGCC 15%)
            case 1697 -> 2600; // CT Gogeta (33% SĐ, 28% HP, 28% KI, Hút 20%, CM 10%)
            default -> 1500;
        };
    }

    private static void populateBackItemsIntoShopDoiDiem(List<Shop> shops) {
        Shop doiDiemShop = null;
        for (Shop s : shops) {
            if ("SHOP_DOI_DIEM".equals(s.tagName)) {
                doiDiemShop = s;
                break;
            }
        }
        if (doiDiemShop == null) {
            return;
        }

        int iconEventPoint = 15451;
        for (TabShop t : doiDiemShop.tabShops) {
            for (ItemShop is : t.itemShops) {
                if (is.iconSpec > 0) {
                    iconEventPoint = is.iconSpec;
                    break;
                }
            }
        }

        List<ItemTemplate> backTemplates = new ArrayList<>();
        for (ItemTemplate temp : Manager.ITEM_TEMPLATES) {
            if (temp != null && temp.type == 11 && temp.part != -1 && temp.iconID > 0
                    && temp.name != null && !temp.name.trim().isEmpty()) {
                backTemplates.add(temp);
            }
        }
        if (backTemplates.isEmpty()) {
            return;
        }

        ItemTemplate rubyTemp = ItemService.gI().getTemplate((short) 861);
        int iconRuby = rubyTemp != null ? rubyTemp.iconID : 7743;

        doiDiemShop.tabShops.clear();
        int pageSize = 50;
        int totalPages = (int) Math.ceil(backTemplates.size() / (double) pageSize);
        int autoId = 200000;

        Shop rubyBackShop = new Shop();
        rubyBackShop.id = 9998;
        rubyBackShop.npcId = doiDiemShop.npcId;
        rubyBackShop.tagName = "SHOP_DEO_LUNG_RUBY";
        rubyBackShop.typeShop = 3; // SPEC_SHOP hiển thị icon Hồng Ngọc

        for (int p = 0; p < totalPages; p++) {
            TabShop tab = new TabShop();
            tab.shop = doiDiemShop;
            tab.id = 59;
            tab.name = totalPages == 1 ? "Đeo Lưng\n(5 Ngày)" : ("Đeo Lưng " + (p + 1) + "\n(5 Ngày)");

            TabShop rubyTab = new TabShop();
            rubyTab.shop = rubyBackShop;
            rubyTab.id = 590;
            rubyTab.name = totalPages == 1 ? "Đeo Lưng\n(Vĩnh Viễn)" : ("Đeo Lưng " + (p + 1) + "\n(Vĩnh Viễn)");

            int start = p * pageSize;
            int end = Math.min(backTemplates.size(), start + pageSize);
            for (int idx = start; idx < end; idx++) {
                ItemTemplate temp = backTemplates.get(idx);
                ItemShop is = new ItemShop();
                is.id = autoId++;
                is.tabShop = tab;
                is.temp = temp;
                is.isNew = true;
                is.typeSell = 3;
                is.iconSpec = iconEventPoint;

                ItemShop rubyIs = new ItemShop();
                rubyIs.id = autoId++;
                rubyIs.tabShop = rubyTab;
                rubyIs.temp = temp;
                rubyIs.isNew = true;
                rubyIs.typeSell = 3; // COST_RUBY
                rubyIs.iconSpec = iconRuby;

                // Phân bậc điểm sự kiện (HSD 5 ngày) và Hồng Ngọc (Vĩnh viễn)
                if (idx < backTemplates.size() / 4) {
                    is.cost = 150;
                    is.options.add(new Item.ItemOption(50, 12));  // SĐ ngẫu nhiên 6-12%
                    is.options.add(new Item.ItemOption(77, 12));  // HP ngẫu nhiên 6-12%
                    is.options.add(new Item.ItemOption(103, 12)); // KI ngẫu nhiên 6-12%
                    is.options.add(new Item.ItemOption(14, 6));   // +1~2 dòng phụ ngẫu nhiên
                    is.options.add(new Item.ItemOption(93, 5));   // Hạn sử dụng 5 ngày
                    is.options.add(new Item.ItemOption(30, 0));

                    rubyIs.cost = 1050;
                    rubyIs.options.add(new Item.ItemOption(50, 12));
                    rubyIs.options.add(new Item.ItemOption(77, 12));
                    rubyIs.options.add(new Item.ItemOption(103, 12));
                    rubyIs.options.add(new Item.ItemOption(14, 6));
                    rubyIs.options.add(new Item.ItemOption(73, 0)); // Vĩnh viễn
                    rubyIs.options.add(new Item.ItemOption(30, 0));
                } else if (idx < backTemplates.size() / 2) {
                    is.cost = 250;
                    is.options.add(new Item.ItemOption(50, 16));  // SĐ ngẫu nhiên 8-16%
                    is.options.add(new Item.ItemOption(77, 16));  // HP ngẫu nhiên 8-16%
                    is.options.add(new Item.ItemOption(103, 16)); // KI ngẫu nhiên 8-16%
                    is.options.add(new Item.ItemOption(14, 8));
                    is.options.add(new Item.ItemOption(5, 10));
                    is.options.add(new Item.ItemOption(93, 5));   // Hạn sử dụng 5 ngày
                    is.options.add(new Item.ItemOption(30, 0));

                    rubyIs.cost = 1250;
                    rubyIs.options.add(new Item.ItemOption(50, 16));
                    rubyIs.options.add(new Item.ItemOption(77, 16));
                    rubyIs.options.add(new Item.ItemOption(103, 16));
                    rubyIs.options.add(new Item.ItemOption(14, 8));
                    rubyIs.options.add(new Item.ItemOption(5, 10));
                    rubyIs.options.add(new Item.ItemOption(73, 0)); // Vĩnh viễn
                    rubyIs.options.add(new Item.ItemOption(30, 0));
                } else if (idx < (backTemplates.size() * 3) / 4) {
                    is.cost = 400;
                    is.options.add(new Item.ItemOption(50, 20));  // SĐ ngẫu nhiên 10-20%
                    is.options.add(new Item.ItemOption(77, 20));  // HP ngẫu nhiên 10-20%
                    is.options.add(new Item.ItemOption(103, 20)); // KI ngẫu nhiên 10-20%
                    is.options.add(new Item.ItemOption(5, 14));
                    is.options.add(new Item.ItemOption(95, 12));
                    is.options.add(new Item.ItemOption(93, 5));   // Hạn sử dụng 5 ngày
                    is.options.add(new Item.ItemOption(30, 0));

                    rubyIs.cost = 1500;
                    rubyIs.options.add(new Item.ItemOption(50, 20));
                    rubyIs.options.add(new Item.ItemOption(77, 20));
                    rubyIs.options.add(new Item.ItemOption(103, 20));
                    rubyIs.options.add(new Item.ItemOption(5, 14));
                    rubyIs.options.add(new Item.ItemOption(95, 12));
                    rubyIs.options.add(new Item.ItemOption(73, 0)); // Vĩnh viễn
                    rubyIs.options.add(new Item.ItemOption(30, 0));
                } else {
                    is.cost = 600;
                    is.options.add(new Item.ItemOption(50, 25));  // SĐ ngẫu nhiên 12-25%
                    is.options.add(new Item.ItemOption(77, 25));  // HP ngẫu nhiên 12-25%
                    is.options.add(new Item.ItemOption(103, 25)); // KI ngẫu nhiên 12-25%
                    is.options.add(new Item.ItemOption(5, 18));
                    is.options.add(new Item.ItemOption(95, 15));
                    is.options.add(new Item.ItemOption(14, 12));
                    is.options.add(new Item.ItemOption(93, 5));   // Hạn sử dụng 5 ngày
                    is.options.add(new Item.ItemOption(30, 0));

                    rubyIs.cost = 1800;
                    rubyIs.options.add(new Item.ItemOption(50, 25));
                    rubyIs.options.add(new Item.ItemOption(77, 25));
                    rubyIs.options.add(new Item.ItemOption(103, 25));
                    rubyIs.options.add(new Item.ItemOption(5, 18));
                    rubyIs.options.add(new Item.ItemOption(95, 15));
                    rubyIs.options.add(new Item.ItemOption(14, 12));
                    rubyIs.options.add(new Item.ItemOption(73, 0)); // Vĩnh viễn
                    rubyIs.options.add(new Item.ItemOption(30, 0));
                }
                tab.itemShops.add(is);
                rubyTab.itemShops.add(rubyIs);
            }
            doiDiemShop.tabShops.add(tab);
            rubyBackShop.tabShops.add(rubyTab);
        }
        shops.add(rubyBackShop);
    }

    private static void populateAllCostumesIntoSanta(List<Shop> shops) {
        Shop santaShop = null;

        for (Shop s : shops) {
            if ("SANTA".equals(s.tagName)) {
                santaShop = s;
                break;
            }
        }

        if (santaShop == null) {
            return;
        }

        // Tạo 2 danh sách Cải trang tinh tuyển:
        // 1) CT Hành Tinh (Tối ưu cho Trái Đất, Namek, Xayda - giá 1.000 Hồng ngọc, chỉ số cân bằng 25-30%)
        // 2) CT Hỗ Trợ (Up đệ tử, Kháng lạnh, Hóa đá, Sôcôla, Hút HP/KI... - giá 50 Ngọc xanh, chỉ số 18-22%)
        List<ItemShop> planetCostumes = new ArrayList<>();
        List<ItemShop> supportCostumes = new ArrayList<>();
        int autoId = 100000;

        int[][] planetDefs = {
            // --- TRÁI ĐẤT: Nhánh Chưởng (Ngẫu nhiên 50% - 150% sát thương chưởng khi mua) ---
            {710,  50, 26, 77, 25, 103, 28, 159, 150, 98, 15, 14, 10},       // Quy Lão Kamê (50-150% ST chưởng)
            {711,  50, 27, 77, 26, 103, 28, 159, 150, 98, 15, 116, 0},       // Jacky Chun (50-150% ST chưởng, Kháng TDHS)

            // --- TRÁI ĐẤT: Nhánh Đánh KOK / Cận Chiến (Tăng nhẹ %SĐ + Hút HP, KI) ---
            {1553, 50, 32, 77, 28, 103, 27, 95, 15, 96, 15, 5, 15, 14, 10},  // Goku SSJ4 (32% SĐ, Hút 15% HP/KI)
            {1590, 50, 33, 77, 28, 103, 28, 95, 20, 96, 20, 5, 15, 99, 15},  // Goku SSJ Blue (33% SĐ, Hút 20% HP/KI)

            // --- NAMEK: Nhánh Đánh Tay / Liên Hoàn (Giống KOK Trái Đất: Tăng nhẹ %SĐ + Hút HP, KI) ---
            {898,  50, 32, 77, 28, 103, 27, 95, 15, 96, 15, 5, 15, 14, 10},  // Zamasu (32% SĐ, Hút 15% HP/KI)
            {1396, 50, 33, 77, 28, 103, 28, 95, 20, 96, 20, 5, 15, 99, 15},  // Super Píc (33% SĐ, Hút 20% HP/KI)

            // --- NAMEK: Nhánh Chuyên KI / Laze (%KI cao hơn + Ngẫu nhiên 25% - 75% ST Laze khi mua) ---
            {738,  50, 26, 77, 26, 103, 34, 66, 75, 98, 15},                 // Dr. Lychee (34% KI, 25-75% ST Laze)
            {1567, 50, 27, 77, 26, 103, 36, 66, 75, 98, 18},                 // Frieren (36% KI, 25-75% ST Laze)

            // --- XAYDA: Nhánh Đánh Tay / Biến Khỉ (Giống KOK Trái Đất: Tăng nhẹ %SĐ + Hút HP, KI) ---
            {1783, 50, 32, 77, 28, 103, 27, 95, 15, 96, 15, 5, 15, 14, 10},  // Broly (32% SĐ, Hút 15% HP/KI)
            {1275, 50, 33, 77, 28, 103, 28, 95, 20, 96, 20, 5, 15, 99, 15},  // Cumber SSJ (33% SĐ, Hút 20% HP/KI)
            {1454, 50, 32, 77, 28, 103, 28, 95, 18, 96, 18, 5, 15, 14, 10},  // Super Black Goku Rose (32% SĐ, Hút 18% HP/KI)
            {1697, 50, 33, 77, 28, 103, 28, 95, 20, 96, 20, 5, 15, 14, 10},  // Gogeta (33% SĐ, Hút 20% HP/KI)

            // --- XAYDA: Nhánh Đánh Bom (%HP cao hơn + Ngẫu nhiên 25% - 75% ST Nổ khi mua) ---
            {1744, 50, 26, 77, 34, 103, 25, 153, 75, 97, 12},                // Cađíc SSJ2 M (34% HP, 25-75% ST Nổ)
            {1693, 50, 27, 77, 36, 103, 26, 153, 75, 97, 15}                 // Cađíc SSJ4 (36% HP, 25-75% ST Nổ)
        };

        ItemTemplate rubyTemp = ItemService.gI().getTemplate((short) 861);
        int iconRuby = rubyTemp != null ? rubyTemp.iconID : 7743;
        ItemTemplate gemTemp = ItemService.gI().getTemplate((short) 77);
        int iconGem = gemTemp != null ? gemTemp.iconID : 932;

        for (int[] def : planetDefs) {
            ItemTemplate temp = ItemService.gI().getTemplate((short) def[0]);
            if (temp != null) {
                ItemShop is = new ItemShop();
                is.id = autoId++;
                is.temp = temp;
                is.isNew = true;
                is.typeSell = 3; // Bán bằng Hồng ngọc (COST_RUBY)
                is.cost = getRubyCostForPlanetCostume(def[0]); // Giá > 1.000 Hồng ngọc theo chỉ số
                is.iconSpec = iconRuby;
                for (int i = 1; i + 1 < def.length; i += 2) {
                    is.options.add(new Item.ItemOption(def[i], def[i + 1]));
                }
                planetCostumes.add(is);
            }
        }

        int[][] supportDefs = {
            // --- UP ĐỆ TỬ & SƯ PHỤ ---
            {547,  50, 20, 77, 20, 103, 20, 160, 30, 101, 25},            // Tôn Ngộ Không Đệ Tử (+30% TNSM Đệ)
            {548,  50, 18, 77, 24, 103, 20, 160, 30, 80, 15},             // Bát Giới Đệ Tử (+30% TNSM Đệ)
            {1302, 50, 20, 77, 22, 103, 22, 160, 30, 94, 12},             // Sa Tăng Đệ Tử (+30% TNSM Đệ)
            {1362, 50, 20, 77, 20, 103, 20, 101, 40, 160, 30, 100, 80},   // Thầy Giáo (+40% TNSM, +30% TNSM Đệ)

            // --- KHÁNG LẠNH (ĐI MAP COLD) ---
            {629,  50, 22, 77, 22, 103, 22, 106, 0, 5, 10},               // Fide Vàng (Kháng lạnh)
            {878,  50, 23, 77, 22, 103, 22, 106, 0, 5, 10},               // Cooler Vàng (Kháng lạnh)
            {937,  50, 21, 77, 24, 103, 21, 106, 0, 29, 0, 80, 15},       // Mabư Noel (Kháng lạnh + Sôcôla)

            // --- HIỆU ỨNG PK & SĂN BOSS ---
            {1437, 50, 20, 77, 24, 103, 20, 26, 0, 94, 12},               // Bông Băng Vàng (Hóa đá)
            {1205, 50, 22, 77, 22, 103, 22, 26, 0, 106, 0},               // Drabura Frost (Hóa đá + Kháng lạnh)
            {578,  50, 20, 77, 25, 103, 20, 29, 0, 80, 10},               // Mabư Mập (Biến Sôcôla)
            {549,  50, 21, 77, 21, 103, 21, 8, 4, 14, 5},                 // Xên Con (Hút 4% HP, KI mỗi 5s)
            {525,  50, 20, 77, 20, 103, 22, 3, 100, 4, 2},                // Android 20 (Vô hiệu chưởng thành KI)
            {455,  50, 18, 77, 20, 103, 18, 109, 10},                     // Ở Dơ (Hôi giảm 10% HP xung quanh)
            {458,  50, 19, 77, 19, 103, 19, 108, 20, 111, 0},             // Xinbatô (+20% Né đòn)
            {463,  50, 20, 77, 20, 103, 20, 116, 0},                      // Thỏ Đại Ca (Kháng TDHS)
            {449,  50, 20, 77, 18, 103, 18, 25, 0, 105, 0},               // Người Vô Hình (Tàng hình)
            {1557, 50, 22, 77, 22, 103, 22, 117, 15, 162, 3},             // Hắc Mị Nương (Đẹp +15% SĐ xung quanh)
            {806,  50, 20, 77, 22, 103, 22, 173, 12, 94, 10},             // Đôrêmon (Hồi 12% HP/KI đồng đội)

            // --- VÕ PHỤC YARDRAT (3 HÀNH TINH) ---
            {592,  50, 20, 77, 20, 103, 20, 33, 1, 108, 15},              // Võ phục Yardrat Trái Đất
            {593,  50, 20, 77, 20, 103, 20, 33, 1, 108, 15},              // Võ phục Yardrat Namek
            {594,  50, 20, 77, 20, 103, 20, 33, 1, 108, 15}               // Võ phục Yardrat Xayda
        };

        for (int[] def : supportDefs) {
            ItemTemplate temp = ItemService.gI().getTemplate((short) def[0]);
            if (temp != null) {
                ItemShop is = new ItemShop();
                is.id = autoId++;
                is.temp = temp;
                is.isNew = true;
                is.typeSell = 1; // Bán bằng Ngọc xanh
                is.cost = 50;    // Giá 50 Ngọc xanh
                is.iconSpec = iconGem;
                for (int i = 1; i + 1 < def.length; i += 2) {
                    is.options.add(new Item.ItemOption(def[i], def[i + 1]));
                }
                supportCostumes.add(is);
            }
        }

        List<TabShop> newSantaTabs = new ArrayList<>();
        boolean insertedCostumeTabs = false;

        boolean addedCskb = false;
        for (TabShop oldTab : santaShop.tabShops) {
            if (oldTab.id == 18) {
                if (!insertedCostumeTabs) {
                    TabShop tabPlanet = new TabShop();
                    tabPlanet.shop = santaShop;
                    tabPlanet.id = 18;
                    tabPlanet.name = "CT Hành Tinh\n(Hồng Ngọc)";
                    for (ItemShop is : planetCostumes) {
                        is.tabShop = tabPlanet;
                        tabPlanet.itemShops.add(is);
                    }
                    newSantaTabs.add(tabPlanet);

                    TabShop tabSupport = new TabShop();
                    tabSupport.shop = santaShop;
                    tabSupport.id = 1800;
                    tabSupport.name = "CT Hỗ Trợ\n(Đệ/Cold/PK)";
                    for (ItemShop is : supportCostumes) {
                        is.tabShop = tabSupport;
                        tabSupport.itemShops.add(is);
                    }
                    newSantaTabs.add(tabSupport);

                    insertedCostumeTabs = true;
                }
            } else {
                if (!addedCskb) {
                    ItemTemplate cskbTemp = ItemService.gI().getTemplate(380);
                    if (cskbTemp != null) {
                        ItemShop cskbShop = new ItemShop();
                        cskbShop.id = autoId++;
                        cskbShop.tabShop = oldTab;
                        cskbShop.temp = cskbTemp;
                        cskbShop.isNew = true;
                        cskbShop.typeSell = 0; // Bán bằng Vàng
                        cskbShop.cost = 50_000_000; // 50tr Vàng
                        cskbShop.iconSpec = 0;
                        cskbShop.options.add(new Item.ItemOption(31, 99)); // Số lượng 99
                        cskbShop.options.add(new Item.ItemOption(30, 0));
                        oldTab.itemShops.add(0, cskbShop);
                    }
                    ItemTemplate thoiVangTemp = ItemService.gI().getTemplate(457);
                    if (thoiVangTemp != null) {
                        ItemShop tvShop = new ItemShop();
                        tvShop.id = autoId++;
                        tvShop.tabShop = oldTab;
                        tvShop.temp = thoiVangTemp;
                        tvShop.isNew = true;
                        tvShop.typeSell = 0; // Bán bằng Vàng
                        tvShop.cost = 500_000_000; // 500tr Vàng / 1 Thỏi vàng
                        tvShop.iconSpec = 0;
                        tvShop.options.add(new Item.ItemOption(73, 0));
                        oldTab.itemShops.add(0, tvShop);
                    }
                    addedCskb = true;
                }
                newSantaTabs.add(oldTab);
            }
        }

        if (!insertedCostumeTabs) {
            TabShop tabPlanet = new TabShop();
            tabPlanet.shop = santaShop;
            tabPlanet.id = 18;
            tabPlanet.name = "CT Hành Tinh\n(Hồng Ngọc)";
            for (ItemShop is : planetCostumes) {
                is.tabShop = tabPlanet;
                tabPlanet.itemShops.add(is);
            }
            newSantaTabs.add(tabPlanet);

            TabShop tabSupport = new TabShop();
            tabSupport.shop = santaShop;
            tabSupport.id = 1800;
            tabSupport.name = "CT Hỗ Trợ\n(Đệ/Cold/PK)";
            for (ItemShop is : supportCostumes) {
                is.tabShop = tabSupport;
                tabSupport.itemShops.add(is);
            }
            newSantaTabs.add(tabSupport);
        }

        santaShop.tabShops = newSantaTabs;

        // Tạo thêm shop SPEC_SHOP ("SANTA_CAITRANG") để hiển thị chuẩn icon Hồng Ngọc / Ngọc Xanh trên client
        Shop santaCostumeShop = new Shop();
        santaCostumeShop.id = 9999;
        santaCostumeShop.npcId = santaShop.npcId;
        santaCostumeShop.tagName = "SANTA_CAITRANG";
        santaCostumeShop.typeShop = 3; // SPEC_SHOP hiển thị iconSpec (Hồng Ngọc / Ngọc Xanh)

        TabShop specTabPlanet = new TabShop();
        specTabPlanet.shop = santaCostumeShop;
        specTabPlanet.id = 1801;
        specTabPlanet.name = "CT Hành Tinh\n(Hồng Ngọc)";
        for (ItemShop is : planetCostumes) {
            ItemShop copy = new ItemShop(is);
            copy.tabShop = specTabPlanet;
            specTabPlanet.itemShops.add(copy);
        }
        santaCostumeShop.tabShops.add(specTabPlanet);

        TabShop specTabSupport = new TabShop();
        specTabSupport.shop = santaCostumeShop;
        specTabSupport.id = 1802;
        specTabSupport.name = "CT Hỗ Trợ\n(Đệ/Cold/PK)";
        for (ItemShop is : supportCostumes) {
            ItemShop copy = new ItemShop(is);
            copy.tabShop = specTabSupport;
            specTabSupport.itemShops.add(copy);
        }
        santaCostumeShop.tabShops.add(specTabSupport);

        shops.add(santaCostumeShop);
    }

    private static void addOpts(ItemShop is, int... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            is.options.add(new Item.ItemOption(pairs[i], pairs[i + 1]));
        }
    }

    private static void applyClassicCostumeOptions(ItemShop is, ItemTemplate temp) {
        int id = temp.id;
        switch (id) {
            // Avatar thường & VIP
            case 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210,
                 412, 413, 414, 415, 416, 417, 418, 419, 420,
                 866, 867, 868, 872, 873, 894, 895, 896 ->
                addOpts(is, 77, 10, 103, 10, 14, 3);
            case 227, 228, 229 ->
                addOpts(is, 50, 12, 77, 12, 103, 12, 14, 5);
            case 598, 599, 600 ->
                addOpts(is, 50, 15, 77, 15, 103, 15, 101, 20);
            case 682, 686, 688 ->
                addOpts(is, 155, 100);
            case 683, 687, 690 ->
                addOpts(is, 156, 50);
            case 684, 685, 689 ->
                addOpts(is, 157, 50);
            case 761, 762, 763 ->
                addOpts(is, 50, 12, 77, 15, 80, 15);
            case 1063, 1064, 1065 ->
                addOpts(is, 50, 20, 77, 20, 103, 20, 94, 15);
            case 1146, 1147, 1148, 1266, 1267, 1268, 1269, 1270, 1271, 1389, 1390, 1391, 1469, 1470, 1471 ->
                addOpts(is, 50, 20, 77, 20, 103, 20, 14, 8, 101, 20);

            // Cải trang kinh điển đời đầu (Santa gốc)
            case 282 -> addOpts(is, 50, 10, 77, 15, 103, 15, 47, 50);               // Thần Vũ Trụ
            case 283 -> addOpts(is, 77, 20, 80, 10, 50, 10);                        // Yajirobe
            case 284 -> addOpts(is, 50, 15, 77, 10, 14, 5, 166, 20);                // Tàu Pảy Pảy
            case 285 -> addOpts(is, 50, 10, 77, 10, 103, 20, 108, 10);              // Thần Mèo Karin
            case 286 -> addOpts(is, 50, 10, 77, 15, 103, 20);                       // Thượng Đế
            case 287 -> addOpts(is, 50, 10, 103, 20, 81, 10);                       // Mr. Pôpô
            case 288 -> addOpts(is, 50, 12, 77, 12, 14, 3);                         // Kuku
            case 289 -> addOpts(is, 50, 14, 77, 10, 14, 4);                         // Rambô
            case 290 -> addOpts(is, 50, 12, 103, 12, 14, 2, 94, 8);                 // Số 2
            case 291 -> addOpts(is, 50, 12, 77, 10, 108, 10, 114, 15);              // Ninja Áo Tím
            case 292 -> addOpts(is, 50, 12, 77, 12, 14, 2, 94, 8, 114, 20);         // Số 3

            // Nón Noel
            case 386, 389, 392 -> addOpts(is, 77, 15, 103, 15, 106, 0);             // Nón Noel Xám
            case 387, 390, 393 -> addOpts(is, 50, 20, 80, 30, 103, 15, 106, 0);     // Nón Noel Đỏ
            case 388, 391, 394 -> addOpts(is, 77, 30, 80, 50, 106, 0);              // Nón Noel Xanh

            // Fide đại ca cấp 1, 2, 3 & Fide Vàng & Cooler Vàng & Frost 1, 2, 3 & Chill
            case 405 -> addOpts(is, 50, 15, 77, 15, 103, 15, 170, 10);              // Fide cấp 1
            case 406 -> addOpts(is, 50, 18, 77, 18, 103, 18, 170, 15);              // Fide cấp 2
            case 407 -> addOpts(is, 50, 20, 77, 20, 103, 20, 170, 20);              // Fide cấp 3
            case 629 -> addOpts(is, 50, 26, 77, 26, 103, 26, 5, 15, 106, 0, 170, 20); // Fide Vàng
            case 630 -> addOpts(is, 50, 18, 77, 18, 103, 18, 106, 0, 169, 0, 179, 0); // Frost 1
            case 631 -> addOpts(is, 50, 20, 77, 20, 103, 20, 106, 0, 169, 0, 179, 0); // Frost 2
            case 632 -> addOpts(is, 50, 23, 77, 23, 103, 23, 106, 0, 169, 0, 179, 0); // Frost 3
            case 878 -> addOpts(is, 50, 27, 77, 26, 103, 25, 5, 15, 106, 0, 179, 0);  // Cooler Vàng
            case 879 -> addOpts(is, 50, 24, 77, 24, 103, 22, 28, 10, 106, 0, 179, 0); // Thống Chế Kilo
            case 985 -> addOpts(is, 50, 24, 77, 24, 103, 24, 5, 10, 106, 0, 179, 0);  // Chill
            case 986 -> addOpts(is, 50, 26, 77, 26, 103, 26, 5, 15, 106, 0, 179, 0);  // Chill cấp 2

            // Chi Chi, Bum ma, Lý Tiểu Nương, Thỏ Bunma
            case 408, 580 -> addOpts(is, 50, 20, 77, 17, 103, 17, 117, 12);         // Chi Chi
            case 724 -> addOpts(is, 50, 20, 77, 17, 103, 17, 162, 2);               // Chi Chi Cute
            case 409, 581 -> addOpts(is, 50, 17, 77, 20, 103, 20, 117, 12);         // Bum ma
            case 410, 582 -> addOpts(is, 50, 20, 77, 18, 103, 20, 117, 15, 100, 30);// Lý Tiểu Nương
            case 464, 584 -> addOpts(is, 50, 20, 77, 20, 103, 20, 117, 15);         // Thỏ Bunma

            // Póc, Píc, King Kong & Android Sát Thủ
            case 411, 583, 633, 634 -> addOpts(is, 50, 18, 77, 18, 103, 18, 94, 8, 82, 0, 150, 0);
            case 1234, 1235, 1236 -> addOpts(is, 50, 24, 77, 24, 103, 24, 94, 10, 150, 0, 194, 3);
            case 1396 -> addOpts(is, 50, 27, 77, 27, 103, 27, 94, 12, 82, 0, 150, 0); // Super Píc

            // Sơn Tinh, Thủy Tinh, Hùng Vương, Mị Nương
            case 421 -> addOpts(is, 50, 20, 77, 22, 103, 18, 94, 10);               // Sơn Tinh
            case 422 -> addOpts(is, 50, 20, 77, 18, 103, 22, 98, 10);               // Thủy Tinh
            case 860 -> addOpts(is, 50, 24, 77, 24, 103, 24, 117, 15, 162, 2);      // Mị Nương
            case 1557, 1761 -> addOpts(is, 50, 28, 77, 28, 103, 28, 5, 18, 117, 15, 236, 10); // Hắc Mị Nương / Mị Hoàng Kim
            case 1762 -> addOpts(is, 50, 30, 77, 30, 103, 30, 5, 15, 94, 15);       // Hùng Vương
            case 1763 -> addOpts(is, 50, 28, 77, 30, 103, 26, 94, 15, 97, 10);      // Sơn Tinh mới
            case 1764 -> addOpts(is, 50, 28, 77, 26, 103, 30, 98, 15, 5, 15);       // Thủy Tinh mới

            // Băng Bojack
            case 423, 424, 425, 426 -> addOpts(is, 50, 18, 77, 18, 103, 18, 98, 5, 99, 5); // Bujin, Kogu, Zangya, Bido
            case 427 -> addOpts(is, 50, 21, 77, 21, 103, 21, 98, 10, 99, 10);       // Bojack
            case 428 -> addOpts(is, 50, 24, 77, 24, 103, 24, 98, 15, 99, 15, 5, 10);// Super Bojack

            // Tiểu đội sát thủ Ginyu
            case 429, 616 -> addOpts(is, 50, 15, 77, 18, 103, 15, 14, 3, 94, 8);    // Số 4
            case 430 -> addOpts(is, 50, 16, 77, 16, 103, 16, 14, 3, 94, 8, 114, 25);// Số 3
            case 431 -> addOpts(is, 50, 17, 77, 16, 103, 17, 14, 3, 94, 8);         // Số 2
            case 432, 615 -> addOpts(is, 50, 18, 77, 20, 103, 15, 14, 3, 94, 10, 114, 30); // Số 1
            case 433, 617 -> addOpts(is, 50, 20, 77, 20, 103, 20, 14, 5, 94, 10);   // Tiểu Đội Trưởng Ginyu

            // Võ đài Bà Hạt Mít & Thỏ Đại Ca
            case 448, 588 -> addOpts(is, 50, 20, 77, 20, 103, 20, 104, 50, 8, 5);   // Đracula
            case 449 -> addOpts(is, 50, 22, 77, 15, 103, 15, 25, 0, 105, 0);        // Người Vô Hình
            case 450, 586 -> addOpts(is, 50, 18, 77, 25, 103, 18, 26, 0, 106, 0);   // Bông Băng
            case 1437 -> addOpts(is, 50, 25, 77, 30, 103, 25, 26, 0, 94, 15, 100, 50); // Bông Băng Vàng
            case 451, 587 -> addOpts(is, 50, 22, 77, 22, 103, 18, 100, 30, 14, 8);  // Vua Quỷ Satan
            case 452 -> addOpts(is, 50, 18, 77, 20, 103, 20, 101, 20, 115, 0);      // Thỏ Đầu Bạc
            case 455 -> addOpts(is, 50, 20, 77, 20, 103, 15, 109, 10, 110, 0);      // Ở Dơ
            case 458 -> addOpts(is, 50, 20, 77, 18, 103, 18, 108, 15, 111, 0, 110, 0); // Xinbatô
            case 461 -> addOpts(is, 50, 20, 77, 18, 103, 18, 114, 100, 101, 20, 110, 0); // Sói Hẹc Quyn
            case 463 -> addOpts(is, 50, 20, 77, 20, 103, 20, 115, 0, 116, 0);       // Thỏ Đại Ca

            // Android 19, 20, 13, 14, 15 & Xên Bọ Hung 1, 2, 3, Xên Con
            case 524 -> addOpts(is, 50, 18, 77, 20, 103, 22, 3, 80, 4, 1, 5, 10);   // Android 19
            case 525 -> addOpts(is, 50, 20, 77, 22, 103, 25, 3, 100, 4, 2, 5, 15);  // Android 20
            case 526 -> addOpts(is, 50, 18, 77, 18, 103, 18, 8, 1);                 // Xên Bọ Hung 1
            case 527 -> addOpts(is, 50, 20, 77, 20, 103, 20, 8, 2);                 // Xên Bọ Hung 2
            case 528 -> addOpts(is, 50, 22, 77, 22, 103, 22, 8, 3);                 // Xên Bọ Hung 3
            case 549 -> addOpts(is, 50, 24, 77, 22, 103, 22, 8, 4, 14, 8);          // Xên Con
            case 550 -> addOpts(is, 50, 20, 77, 20, 103, 20, 19, 24, 94, 10);       // Android 13
            case 551 -> addOpts(is, 50, 19, 77, 22, 103, 18, 19, 22, 94, 10);       // Android 14
            case 552 -> addOpts(is, 50, 18, 77, 18, 103, 22, 19, 20, 94, 10);       // Android 15

            // Tây Du Ký
            case 544, 545, 546 -> addOpts(is, 50, 25, 77, 22, 103, 22, 14, 10, 101, 20); // Tôn Ngộ Không
            case 547 -> addOpts(is, 50, 25, 77, 22, 103, 22, 79, 15, 101, 25);      // Tôn Ngộ Không (đệ tử)
            case 548 -> addOpts(is, 50, 22, 77, 28, 103, 20, 79, 12, 80, 15);       // Bát Giới (đệ tử)
            case 1302 -> addOpts(is, 50, 23, 77, 25, 103, 25, 79, 12, 94, 12);      // Sa Tăng (đệ tử)

            // Majin Buu Saga
            case 575 -> addOpts(is, 50, 19, 77, 22, 103, 18, 24, 0);                // Bui Bui
            case 576 -> addOpts(is, 50, 21, 77, 18, 103, 21, 25, 0, 119, 5);        // Yacôn
            case 577 -> addOpts(is, 50, 22, 77, 20, 103, 20, 26, 0);                // Drabura
            case 578 -> addOpts(is, 50, 22, 77, 30, 103, 20, 29, 0, 80, 10);        // Mabư Mập
            case 765 -> addOpts(is, 50, 25, 77, 24, 103, 24, 80, 10, 81, 10, 29, 0, 177, 0); // Gohan Bư
            case 932 -> addOpts(is, 50, 25, 77, 25, 103, 25, 79, 15, 29, 0);        // Uub
            case 937 -> addOpts(is, 50, 24, 77, 25, 103, 24, 80, 15, 106, 0, 186, 0); // Mabư Noel
            case 1205 -> addOpts(is, 50, 26, 77, 26, 103, 26, 26, 0, 106, 0, 229, 10); // Drabura Frost
            case 1255 -> addOpts(is, 50, 26, 77, 24, 103, 24, 97, 8, 29, 0, 160, 30); // Mabư Còm

            // Siêu Thần, Yardrat, Hợp Thể, VIP, Chan Xư, Lão Cận
            case 591, 905, 907, 911 -> addOpts(is, 50, 26, 77, 26, 103, 26, 5, 15, 108, 15); // Siêu Thần
            case 592, 593, 594 -> addOpts(is, 50, 20, 77, 20, 103, 20, 33, 1, 108, 20);      // Võ phục Yardrat
            case 601, 602, 603, 639, 640, 641 -> addOpts(is, 50, 25, 77, 25, 103, 25, 5, 12, 38, 0); // Hợp Thể
            case 604, 605, 606 -> addOpts(is, 50, 25, 77, 25, 103, 25, 14, 10, 106, 0);      // Cải trang VIP
            case 607, 608, 609 -> addOpts(is, 50, 20, 77, 20, 103, 20, 19, 21, 116, 0);      // Chan Xư, Lão Cận, Xayda
            case 885, 1566 -> addOpts(is, 50, 28, 77, 30, 103, 28, 5, 15, 38, 0);            // CT Lích Tên béo
            case 906 -> addOpts(is, 50, 23, 77, 23, 103, 23, 14, 10, 108, 15);               // Mighty Mask

            // Dr. Slum (Arale, Gatchan, Obotchaman)
            case 612, 613, 614 -> addOpts(is, 50, 20, 77, 20, 103, 20, 114, 33, 145, 0, 146, 0);

            // Băng Hải Tặc Mũ Rơm
            case 618 -> addOpts(is, 50, 25, 77, 25, 103, 25, 14, 10, 94, 10, 149, 0); // Luffy
            case 619 -> addOpts(is, 50, 26, 77, 22, 103, 20, 5, 15, 99, 10, 149, 0);  // Zoro
            case 620 -> addOpts(is, 50, 24, 77, 24, 103, 22, 114, 30, 108, 15, 149, 0); // Sanji
            case 621 -> addOpts(is, 50, 22, 77, 20, 103, 24, 32, 0, 121, 5, 149, 0);  // Brook
            case 622 -> addOpts(is, 50, 20, 77, 28, 103, 24, 80, 15, 81, 15, 149, 0); // Chopper
            case 623 -> addOpts(is, 50, 22, 77, 22, 103, 25, 117, 12, 100, 50, 149, 0); // Nami
            case 624 -> addOpts(is, 50, 23, 77, 26, 103, 20, 94, 15, 47, 200, 149, 0); // Franky
            case 625 -> addOpts(is, 50, 22, 77, 20, 103, 25, 18, 25, 14, 10, 149, 0); // Usopp
            case 626 -> addOpts(is, 50, 23, 77, 23, 103, 25, 117, 15, 123, 5, 149, 0); // Robin

            // Nhóm Piláp (Piláp, Mai, Su)
            case 635, 636, 637 -> addOpts(is, 50, 20, 77, 20, 103, 20, 100, 50, 151, 0, 152, 0);

            // Halloween (Ma Trơi, Dơi Nhí, Bộ Xương, Bill Bí Ngô, Caulifla Dơi, Marron, Poc Satan, Bunma Phù Thủy, Biden Dracula)
            case 642, 643, 644, 645, 646 -> addOpts(is, 50, 20, 77, 20, 103, 20, 32, 0, 163, 0);
            case 739 -> addOpts(is, 50, 27, 77, 25, 103, 25, 5, 15, 32, 0, 163, 0);   // Bill Bí Ngô
            case 742 -> addOpts(is, 50, 26, 77, 25, 103, 25, 8, 5, 32, 0, 163, 0);    // Caulifla Dơi
            case 1103 -> addOpts(is, 50, 22, 77, 22, 103, 22, 148, 20, 8, 4);         // Marron
            case 1104 -> addOpts(is, 50, 24, 77, 24, 103, 24, 14, 10, 163, 0);        // Poc Satan
            case 1105 -> addOpts(is, 50, 24, 77, 24, 103, 25, 108, 15, 117, 12, 163, 0); // Bunma Phù Thủy
            case 1106 -> addOpts(is, 50, 25, 77, 25, 103, 24, 97, 10, 8, 5);          // Biden Dracula

            // Saibamen
            case 647 -> addOpts(is, 50, 20, 77, 25, 103, 20, 153, 50);

            // Các Nữ Chiến Binh & Thủy Thủ Mặt Trăng
            case 675, 676, 677, 678, 679, 680, 681 -> addOpts(is, 50, 23, 77, 23, 103, 23, 117, 12, 108, 10);
            case 760 -> addOpts(is, 50, 25, 77, 25, 103, 25, 117, 15, 162, 3);        // Thủy Thủ Mặt Trăng

            // Quy Lão Kamê, Jacky Chun, Urôn
            case 710, 711 -> addOpts(is, 50, 24, 77, 22, 103, 28, 159, 2);            // Quy Lão Kamê, Jacky Chun
            case 719 -> addOpts(is, 50, 20, 77, 25, 103, 22, 100, 50, 80, 10);        // Urôn
            case 1698 -> addOpts(is, 50, 26, 77, 28, 103, 25, 114, 35, 94, 15, 5, 10);// Urôn Trư Bát Giới
            case 1700 -> addOpts(is, 50, 27, 77, 28, 103, 28, 117, 15, 94, 12);       // Hằng Nga
            case 1705 -> addOpts(is, 50, 27, 77, 27, 103, 27, 5, 15, 117, 15);        // Chi Chi Võ Đài

            // Hatchiyack, Dr. Lychee, 3 Sói Tam Giác
            case 729 -> addOpts(is, 50, 27, 77, 27, 103, 25, 5, 15, 97, 10);          // Hatchiyack
            case 730, 731, 732 -> addOpts(is, 50, 24, 77, 24, 103, 24, 79, 10, 114, 25, 169, 0); // Sói Basil, Lavender, Bergamo
            case 738 -> addOpts(is, 50, 25, 77, 25, 103, 28, 5, 15, 98, 15);          // Dr. Lychee

            // Nón Chuột, Trâu, Hổ, Mèo, Đầu Trâu, Mặt Ngựa, Lân, Rồng
            case 754, 755, 756 -> addOpts(is, 50, 20, 77, 20, 103, 20, 100, 50, 14, 8);
            case 843, 844, 845, 853, 854, 855, 856, 857, 858 -> addOpts(is, 50, 25, 77, 28, 103, 22, 94, 12);
            case 846, 847, 848, 941, 946, 947, 1198, 1199, 1200 -> addOpts(is, 50, 18, 77, 18, 103, 18, 14, 8, 100, 30);
            case 948, 952, 953 -> addOpts(is, 50, 27, 77, 26, 103, 25, 5, 15, 14, 10);
            case 1201, 1684, 1685 -> addOpts(is, 50, 27, 77, 27, 103, 27, 5, 15, 100, 50);
            case 1371, 1380, 1381 -> addOpts(is, 50, 25, 77, 25, 103, 25, 79, 15, 160, 30);
            case 1658, 1659, 1660, 1661, 1662, 1663 -> addOpts(is, 50, 22, 77, 22, 103, 22, 114, 25, 94, 10);

            // Bộ Đôrêmon (Đôrêmon, Xuka, Nobita, Xekô, Chaien)
            case 806 -> addOpts(is, 50, 25, 77, 25, 103, 25, 173, 15, 80, 10, 81, 10);// Đôrêmon
            case 819 -> addOpts(is, 50, 23, 77, 24, 103, 25, 117, 15, 162, 2);        // Xuka
            case 862 -> addOpts(is, 50, 23, 77, 22, 103, 23, 18, 30, 108, 20);        // Nobita
            case 863 -> addOpts(is, 50, 23, 77, 22, 103, 24, 100, 80, 111, 0);        // Xekô
            case 864 -> addOpts(is, 50, 26, 77, 28, 103, 20, 94, 15, 119, 5);         // Chaien

            // Noel các năm (824..827, 922..924, 1155..1157, 1424, 1436)
            case 824, 825, 826, 827, 922, 923, 924, 1155, 1156, 1157, 1424, 1436 ->
                addOpts(is, 50, 25, 77, 25, 103, 25, 80, 15, 106, 0, 117, 12);

            // Cadic, Gohan, Pocolo, Goku Thời Trang, Tarzan, Kimono
            case 875, 876, 877, 938, 939, 940, 1174, 1175, 1176 ->
                addOpts(is, 50, 25, 77, 25, 103, 25, 5, 12, 117, 10);
            case 945 -> addOpts(is, 50, 25, 77, 26, 103, 22, 19, 25, 114, 25);        // Goku Tarzan
            case 951 -> addOpts(is, 50, 28, 77, 28, 103, 25, 19, 30, 5, 15);          // Siêu Goku Tarzan

            // Black Goku, Zamasu, Hit
            case 883 -> addOpts(is, 50, 27, 77, 26, 103, 26, 5, 15, 180, 5);          // Black Gohan Rose
            case 884 -> addOpts(is, 50, 28, 77, 24, 103, 24, 5, 20, 99, 15);          // Hit
            case 898 -> addOpts(is, 50, 26, 77, 30, 103, 28, 80, 15, 81, 15, 94, 10); // Zamasu
            case 904 -> addOpts(is, 50, 29, 77, 28, 103, 28, 5, 18, 14, 10);          // Black Goku SSJ White
            case 1454 -> addOpts(is, 50, 32, 77, 30, 103, 30, 5, 20, 14, 12);         // Super Black Goku Rose
            case 1731 -> addOpts(is, 50, 33, 77, 30, 103, 30, 5, 20, 14, 15);         // CT Black Goku Rose
            case 1732 -> addOpts(is, 50, 30, 77, 32, 103, 30, 5, 18, 14, 12);         // CT Black Goku

            // Gohan áo xanh, Póc áo đỏ, Búp bê Arale
            case 912, 913, 914 -> addOpts(is, 50, 23, 77, 23, 103, 23, 148, 20, 184, 3);

            // Đội bóng đá Số 1 - 11
            case 968, 969, 970, 971, 972, 973, 974, 975, 976, 977, 978 ->
                addOpts(is, 50, 23, 77, 23, 103, 23, 148, 25, 193, 2);

            // Mùa hè / Đi biển
            case 1010, 1011, 1012, 1657, 1667, 1803, 1804 ->
                addOpts(is, 50, 25, 77, 25, 103, 25, 117, 15, 194, 3);

            // Broly các phiên bản
            case 1018, 1019, 1020 -> addOpts(is, 50, 26, 77, 28, 103, 24, 5, 12, 97, 8);
            case 1450 -> addOpts(is, 50, 28, 77, 30, 103, 25, 5, 15, 97, 10);         // Broly Base
            case 1783, 1784 -> addOpts(is, 50, 32, 77, 34, 103, 28, 5, 20, 97, 12);   // Broly / Broly Red

            // Thỏ Hồng Bunma, Thỏ Đỏ Chi Chi, Thỏ Đen Android 18
            case 1041, 1042, 1043 -> addOpts(is, 50, 24, 77, 24, 103, 24, 148, 20, 117, 12, 196, 3);

            // Thanh Gươm Diệt Quỷ (Tanjiro, Inosuke, Zenitsu, Nezuko)
            case 1087 -> addOpts(is, 50, 27, 77, 25, 103, 25, 5, 20, 165, 15, 205, 5);// Tanjiro
            case 1088 -> addOpts(is, 50, 26, 77, 28, 103, 22, 94, 15, 99, 12, 205, 5);// Inosuke Hashibira
            case 1089 -> addOpts(is, 50, 25, 77, 27, 103, 22, 94, 12, 99, 10, 205, 5);// Inosuke
            case 1090 -> addOpts(is, 50, 27, 77, 23, 103, 25, 114, 40, 108, 15, 5, 18, 205, 5); // Zenitsu
            case 1091 -> addOpts(is, 50, 26, 77, 27, 103, 26, 80, 15, 81, 15, 117, 12, 205, 5); // Nezuko

            // Bunma Thời Trang & Lý Tiểu Nương Hầu Gái / Bikini
            case 1208, 1209, 1210 -> addOpts(is, 50, 25, 77, 25, 103, 25, 117, 12, 184, 3);
            case 1309, 1476, 1483 -> addOpts(is, 50, 26, 77, 26, 103, 26, 117, 15, 100, 50);
            case 1503, 1504, 1512 -> addOpts(is, 50, 27, 77, 27, 103, 27, 5, 15, 117, 15);

            // Xe triều đình, Yajirobe Zoro, Cumber, Nguyệt Thần, Nhật Thần
            case 1257 -> addOpts(is, 50, 24, 77, 26, 103, 24, 114, 40, 94, 12);       // Xe triều đình
            case 1265 -> addOpts(is, 50, 26, 77, 25, 103, 22, 5, 15, 80, 15, 99, 10); // Yajirobe Zoro
            case 1274 -> addOpts(is, 50, 30, 77, 32, 103, 25, 5, 15, 97, 10);         // Cumber
            case 1275 -> addOpts(is, 50, 33, 77, 35, 103, 28, 5, 20, 97, 15);         // Cumber SSJ
            case 1276 -> addOpts(is, 50, 28, 77, 28, 103, 30, 35, 10, 108, 15);       // Nguyệt thần
            case 1277 -> addOpts(is, 50, 30, 77, 30, 103, 28, 36, 10, 5, 15);         // Nhật thần

            // Bộ Thầy Giáo
            case 1362, 1378, 1379, 1383, 1384, 1385 ->
                addOpts(is, 50, 25, 77, 25, 103, 25, 101, 30, 160, 30);

            // Super Xayda God, Thần Tài, Dragon
            case 1442 -> addOpts(is, 50, 29, 77, 28, 103, 28, 5, 18, 14, 10);
            case 1484, 1485, 1486 -> addOpts(is, 50, 25, 77, 25, 103, 25, 100, 100, 236, 15);
            case 1498, 1499, 1500 -> addOpts(is, 50, 28, 77, 28, 103, 28, 5, 15, 94, 12);

            // SSJ4, Gogeta, Frieren, Himmel, Pikkon
            case 1553 -> addOpts(is, 50, 33, 77, 32, 103, 32, 5, 20, 14, 12);         // Goku SSJ4
            case 1693 -> addOpts(is, 50, 33, 77, 32, 103, 32, 5, 20, 94, 12);         // Cađíc SSJ4
            case 1697 -> addOpts(is, 50, 35, 77, 35, 103, 35, 5, 25, 38, 0);          // Gogeta
            case 1567 -> addOpts(is, 50, 30, 77, 26, 103, 35, 98, 20, 5, 18);         // Frieren
            case 1632 -> addOpts(is, 50, 32, 77, 30, 103, 28, 99, 20, 114, 35);       // Himmel
            case 1674 -> addOpts(is, 50, 28, 77, 28, 103, 28, 5, 15, 108, 15);        // Pikkon

            // Bộ sưu tập Goku & Cađíc qua các cấp SSJ
            case 1588, 1741 -> addOpts(is, 50, 24, 77, 24, 103, 24, 5, 10, 101, 20);  // Base
            case 1589, 1742 -> addOpts(is, 50, 26, 77, 25, 103, 25, 5, 12, 14, 8);    // SSJ1
            case 1595, 1743 -> addOpts(is, 50, 27, 77, 26, 103, 26, 5, 14, 14, 10);   // SSJ2
            case 1744 -> addOpts(is, 50, 28, 77, 27, 103, 26, 5, 16, 153, 50);        // Cađíc SSJ2 M
            case 1587, 1745 -> addOpts(is, 50, 29, 77, 27, 103, 27, 5, 18, 98, 10);   // SSJ3
            case 1593 -> addOpts(is, 50, 30, 77, 29, 103, 29, 5, 18, 80, 15);         // SSJ God
            case 1590, 1746 -> addOpts(is, 50, 32, 77, 30, 103, 30, 5, 20, 14, 12);   // SSJ Blue

            // Bộ Boss Nhí & Thiếu Nhi
            case 1599 -> addOpts(is, 50, 24, 77, 22, 103, 22, 170, 15, 79, 10);       // Fide nhí
            case 1600 -> addOpts(is, 50, 23, 77, 23, 103, 23, 8, 3, 79, 10);          // Xên nhí
            case 1601 -> addOpts(is, 50, 22, 77, 26, 103, 22, 29, 0, 79, 10);         // Mabư nhí
            case 1602 -> addOpts(is, 50, 24, 77, 24, 103, 24, 162, 2, 79, 10);        // Android 21 kid
            case 1807 -> addOpts(is, 50, 28, 77, 28, 103, 28, 5, 15, 114, 30);        // Goku SSJ4 kid
            case 1808 -> addOpts(is, 50, 28, 77, 28, 103, 25, 5, 15, 79, 15);         // Kid Jiren

            // Baby Saga & Các Cải Trang Sự Kiện Mới (1779..1797)
            case 1779, 1782 -> addOpts(is, 50, 26, 77, 26, 103, 26, 117, 15, 162, 3); // Buma, Chichi
            case 1780, 1781, 1796, 1797 -> addOpts(is, 50, 28, 77, 28, 103, 28, 5, 15, 14, 10);
            case 1785, 1786, 1788 -> addOpts(is, 50, 30, 77, 30, 103, 28, 5, 18, 8, 5); // Baby, Vegeta Baby, Baby Khỉ

            default -> addOpts(is, 50, 25, 77, 25, 103, 25, 14, 10);
        }
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
                if (itemShop.temp != null && itemShop.temp.type == 11) {
                    itemShop.options.removeIf(io -> io.optionTemplate != null && io.optionTemplate.id == 93);
                    boolean isRubyPerm = (itemShop.typeSell == 3 && tabShop.id != 59);
                    if (!isRubyPerm) {
                        itemShop.options.add(new Item.ItemOption(93, 5));
                    }
                }
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
