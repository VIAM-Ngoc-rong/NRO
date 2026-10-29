package nro.models.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import nro.models.data.LocalManager;
import nro.models.event_list.TopUp;
import nro.models.event_list.TrungThu;
import nro.models.event_list.HungVuong;
import nro.models.event_list.Christmas;
import nro.models.event_list.Halloween;
import nro.models.event_list.LunarNewYear;
import nro.models.event_list.Default;
import nro.models.event_list.InternationalWomensDay;
import nro.models.server.Manager;
import nro.models.utils.Logger;

public class EventManager {

    private static EventManager instance;

    public static boolean LUNNAR_NEW_YEAR = false;

    public static boolean INTERNATIONAL_WOMANS_DAY = false;

    public static boolean CHRISTMAS = false;

    public static boolean HALLOWEEN = true;

    public static boolean HUNG_VUONG = false;

    public static boolean TRUNG_THU = false;

    public static boolean TOP_UP = false;

    public static boolean DOUBLE_GOLD = false;

    public static boolean SUMMER_NUOC_MIA = false;

    public static boolean CHILDREN_DAY = false;

    public static boolean COOKING_BANH = false;

    public static boolean NEW_YEAR_FIREWORK = false;

    public static boolean NGU_HANH_SON = false;

    public static int EVENT_DROP_RATE = 5;

    public static EventManager gI() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    public static synchronized void ensureConfigTable(Connection con) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "CREATE TABLE IF NOT EXISTS admin_runtime_config ("
                + "cfg_key VARCHAR(64) NOT NULL PRIMARY KEY, "
                + "cfg_val VARCHAR(255) NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
            ps.executeUpdate();
        }
    }

    public static synchronized void loadRuntimeConfig() {
        try (Connection con = LocalManager.getConnection()) {
            ensureConfigTable(con);
            boolean hasAnyRow = false;
            try (PreparedStatement ps = con.prepareStatement("SELECT cfg_key, cfg_val FROM admin_runtime_config");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasAnyRow = true;
                    String k = rs.getString("cfg_key");
                    String v = rs.getString("cfg_val");
                    if (k == null || v == null) {
                        continue;
                    }
                    boolean b = "1".equals(v) || "true".equalsIgnoreCase(v);
                    switch (k) {
                        case "halloween" -> HALLOWEEN = b;
                        case "christmas" -> CHRISTMAS = b;
                        case "lunnar_new_year" -> LUNNAR_NEW_YEAR = b;
                        case "trung_thu" -> TRUNG_THU = b;
                        case "hung_vuong" -> HUNG_VUONG = b;
                        case "womans_day" -> INTERNATIONAL_WOMANS_DAY = b;
                        case "summer_nuoc_mia" -> SUMMER_NUOC_MIA = b;
                        case "children_day" -> CHILDREN_DAY = b;
                        case "cooking_banh" -> COOKING_BANH = b;
                        case "firework" -> NEW_YEAR_FIREWORK = b;
                        case "top_up" -> TOP_UP = b;
                        case "double_gold" -> DOUBLE_GOLD = b;
                        case "ngu_hanh_son" -> NGU_HANH_SON = b;
                        case "event_drop_rate" -> {
                            try {
                                EVENT_DROP_RATE = Math.max(0, Math.min(100, Integer.parseInt(v.trim())));
                            } catch (Exception ignored) {
                            }
                        }
                        case "exp_rate" -> {
                            try {
                                int exp = Math.max(1, Math.min(127, Integer.parseInt(v.trim())));
                                Manager.RATE_EXP_SERVER = (byte) exp;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
            if (!hasAnyRow) {
                saveRuntimeConfig();
            } else {
                Logger.success("Đã khôi phục trạng thái Sự Kiện & Tỷ lệ EXP từ MySQL (admin_runtime_config)\n");
            }
        } catch (Exception e) {
            Logger.error("Không thể tải cấu hình Sự Kiện từ MySQL: " + e.getMessage() + "\n");
        }
    }

    public static synchronized void saveRuntimeConfig() {
        try {
            nro.models.server.ServerManager.saveServerUptime();
        } catch (Exception ignored) {
        }
        try (Connection con = LocalManager.getConnection()) {
            ensureConfigTable(con);
            String sql = "INSERT INTO admin_runtime_config (cfg_key, cfg_val) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE cfg_val = VALUES(cfg_val)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                putCfg(ps, "halloween", HALLOWEEN ? "1" : "0");
                putCfg(ps, "christmas", CHRISTMAS ? "1" : "0");
                putCfg(ps, "lunnar_new_year", LUNNAR_NEW_YEAR ? "1" : "0");
                putCfg(ps, "trung_thu", TRUNG_THU ? "1" : "0");
                putCfg(ps, "hung_vuong", HUNG_VUONG ? "1" : "0");
                putCfg(ps, "womans_day", INTERNATIONAL_WOMANS_DAY ? "1" : "0");
                putCfg(ps, "summer_nuoc_mia", SUMMER_NUOC_MIA ? "1" : "0");
                putCfg(ps, "children_day", CHILDREN_DAY ? "1" : "0");
                putCfg(ps, "cooking_banh", COOKING_BANH ? "1" : "0");
                putCfg(ps, "firework", NEW_YEAR_FIREWORK ? "1" : "0");
                putCfg(ps, "top_up", TOP_UP ? "1" : "0");
                putCfg(ps, "double_gold", DOUBLE_GOLD ? "1" : "0");
                putCfg(ps, "ngu_hanh_son", NGU_HANH_SON ? "1" : "0");
                putCfg(ps, "event_drop_rate", String.valueOf(EVENT_DROP_RATE));
                putCfg(ps, "exp_rate", String.valueOf(Manager.RATE_EXP_SERVER));
                ps.executeBatch();
            }
        } catch (Exception e) {
            Logger.error("Không thể lưu cấu hình Sự Kiện vào MySQL: " + e.getMessage() + "\n");
        }
    }

    private static void putCfg(PreparedStatement ps, String key, String val) throws Exception {
        ps.setString(1, key);
        ps.setString(2, val);
        ps.addBatch();
    }

    public void init() {
        loadRuntimeConfig();
        new Default().init();
        if (LUNNAR_NEW_YEAR) {
            new LunarNewYear().init();
        }
        if (INTERNATIONAL_WOMANS_DAY) {
            new InternationalWomensDay().init();
        }
        if (HALLOWEEN) {
            new Halloween().init();
        }
        if (CHRISTMAS) {
            new Christmas().init();
        }
        if (HUNG_VUONG) {
            new HungVuong().init();
        }
        if (TRUNG_THU) {
            new TrungThu().init();
        }
        if (TOP_UP) {
            new TopUp().init();
        }
    }
}
