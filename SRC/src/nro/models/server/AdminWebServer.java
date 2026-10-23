package nro.models.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import nro.models.Bot.Bot;
import nro.models.Bot.BotManager;
import nro.models.Bot.NewBot;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.data.LocalManager;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.services.ClanService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.shop_ky_gui.ConsignShopManager;
import nro.models.utils.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/**
 * Full-featured built-in HTTP Admin Control Center (Port 14446 & multiplexed on 14445).
 * Zero external dependencies, near-zero RAM footprint, connects directly with local admin_dashboard.html.
 */
public class AdminWebServer {

    public static final int WEB_PORT = 14446;
    private static final String DEFAULT_ADMIN_KEY = "viam2026";
    private static final long START_TIME_MS = System.currentTimeMillis();
    private static HttpServer server;

    public static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", WEB_PORT), 0);
            server.createContext("/", AdminWebServer::handleDashboardHtml);
            server.createContext("/api/status", AdminWebServer::handleStatus);
            server.createContext("/api/accounts", AdminWebServer::handleAccounts);
            server.createContext("/api/items", AdminWebServer::handleItems);
            server.createContext("/api/system", AdminWebServer::handleSystem);
            server.createContext("/api/action", AdminWebServer::handleAction);
            server.createContext("/api/backup/download", AdminWebServer::handleBackupDownload);
            server.setExecutor(Executors.newFixedThreadPool(3));
            server.start();
            Logger.success("Admin Web Monitor API started on port " + WEB_PORT + "\n");
        } catch (Exception e) {
            Logger.error("Không thể khởi động AdminWebServer trên port " + WEB_PORT + ": " + e.getMessage() + "\n");
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Admin-Key");
        exchange.getResponseHeaders().set("Connection", "close");
    }

    /**
     * Multiplexes HTTP requests arriving on the main game port (14445) directly
     * to the local HttpServer (127.0.0.1:14446) so external cloud firewalls never block the dashboard.
     */
    public static void relaySocketToLocalHttp(java.net.Socket clientSocket, InputStream clientIn) {
        try {
            clientSocket.setSoTimeout(8000);
            try (java.net.Socket local = new java.net.Socket("127.0.0.1", WEB_PORT)) {
                local.setSoTimeout(8000);
                OutputStream localOut = local.getOutputStream();
                InputStream localIn = local.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();

                Thread reqPipe = new Thread(() -> {
                    try {
                        byte[] buf = new byte[4096];
                        int n;
                        while ((n = clientIn.read(buf)) != -1) {
                            localOut.write(buf, 0, n);
                            localOut.flush();
                        }
                    } catch (Exception ignored) {
                    }
                }, "AdminHttpRelayReq");
                reqPipe.setDaemon(true);
                reqPipe.start();

                byte[] buf = new byte[8192];
                int n;
                while ((n = localIn.read(buf)) != -1) {
                    clientOut.write(buf, 0, n);
                }
                clientOut.flush();
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean checkAuth(HttpExchange exchange, Map<String, String> params) throws IOException {
        String key = params.getOrDefault("key", "");
        if (key.isEmpty()) {
            List<String> headerKeys = exchange.getRequestHeaders().get("X-Admin-Key");
            if (headerKeys != null && !headerKeys.isEmpty()) {
                key = headerKeys.get(0);
            }
        }
        if (!DEFAULT_ADMIN_KEY.equals(key)) {
            sendJson(exchange, 401, "{\"ok\":false,\"message\":\"Sai mã bảo mật Admin (Key)!\"}");
            return false;
        }
        return true;
    }

    private static void handleDashboardHtml(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        File f1 = new File("admin_dashboard.html");
        File f2 = new File("../admin_dashboard.html");
        File target = f1.exists() ? f1 : (f2.exists() ? f2 : null);
        if (target != null) {
            byte[] bytes = Files.readAllBytes(target.toPath());
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            String fallback = "<html><body style='font-family:sans-serif;background:#0f172a;color:#fff;padding:24px;'>"
                    + "<h2>Ngọc Rồng VIAM - Admin Monitor API đang hoạt động!</h2>"
                    + "<p>Hãy mở file <b>E:\\NRO\\admin_dashboard.html</b> trên máy tính của bạn để giám sát Server.</p>"
                    + "</body></html>";
            byte[] bytes = fallback.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static void handleStatus(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        if (!checkAuth(exchange, params)) {
            return;
        }

        Runtime rt = Runtime.getRuntime();
        long usedRamMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxRamMb = rt.maxMemory() / (1024 * 1024);
        int cpuCores = rt.availableProcessors();
        long uptimeSec = (System.currentTimeMillis() - START_TIME_MS) / 1000;
        int threadsCount = Thread.activeCount();

        List<Player> onlinePlayers = new ArrayList<>(Client.gI().getPlayers());
        List<Boss> allBosses = new ArrayList<>(BossManager.gI().getBosses());
        int botCount = BotManager.gI().bot != null ? BotManager.gI().bot.size() : 0;

        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"ok\":true");
        sb.append(",\"serverName\":").append(jsonStr(ServerManager.NAME));
        sb.append(",\"timeStart\":").append(jsonStr(ServerManager.timeStart != null ? ServerManager.timeStart : ""));
        sb.append(",\"uptimeSec\":").append(uptimeSec);
        sb.append(",\"usedRamMb\":").append(usedRamMb);
        sb.append(",\"maxRamMb\":").append(maxRamMb);
        sb.append(",\"cpuCores\":").append(cpuCores);
        sb.append(",\"threadsCount\":").append(threadsCount);
        sb.append(",\"onlineCount\":").append(onlinePlayers.size());
        sb.append(",\"expRate\":").append(Manager.RATE_EXP_SERVER);
        sb.append(",\"botCount\":").append(botCount);
        sb.append(",\"autoMaintenance\":").append(AutoMaintenance.AutoMaintenance);

        // Online players array
        sb.append(",\"players\":[");
        boolean firstPl = true;
        for (Player pl : onlinePlayers) {
            if (pl == null || pl.nPoint == null || pl.inventory == null) {
                continue;
            }
            if (!firstPl) {
                sb.append(",");
            }
            firstPl = false;
            String mapName = (pl.zone != null && pl.zone.map != null) ? pl.zone.map.mapName : "Không rõ";
            int mapId = (pl.zone != null && pl.zone.map != null) ? pl.zone.map.mapId : -1;
            int zoneId = (pl.zone != null) ? pl.zone.zoneId : -1;
            String genderName = pl.gender == 0 ? "Trái Đất" : (pl.gender == 1 ? "Namek" : "Xayda");
            int vnd = pl.getSession() != null ? pl.getSession().vnd : 0;
            sb.append("{");
            sb.append("\"id\":").append(pl.id);
            sb.append(",\"name\":").append(jsonStr(pl.name));
            sb.append(",\"gender\":").append(jsonStr(genderName));
            sb.append(",\"power\":").append(pl.nPoint.power);
            sb.append(",\"hp\":").append(pl.nPoint.hp);
            sb.append(",\"hpMax\":").append(pl.nPoint.hpMax);
            sb.append(",\"mp\":").append(pl.nPoint.mp);
            sb.append(",\"mpMax\":").append(pl.nPoint.mpMax);
            sb.append(",\"dame\":").append(pl.nPoint.dame);
            sb.append(",\"gold\":").append(pl.inventory.gold);
            sb.append(",\"gem\":").append(pl.inventory.gem);
            sb.append(",\"ruby\":").append(pl.inventory.ruby);
            sb.append(",\"vnd\":").append(vnd);
            sb.append(",\"mapName\":").append(jsonStr(mapName));
            sb.append(",\"mapId\":").append(mapId);
            sb.append(",\"zoneId\":").append(zoneId);
            sb.append(",\"hasPet\":").append(pl.pet != null);
            sb.append(",\"petPower\":").append(pl.pet != null && pl.pet.nPoint != null ? pl.pet.nPoint.power : 0);
            sb.append("}");
        }
        sb.append("]");

        // Bosses array
        sb.append(",\"bosses\":[");
        boolean firstBoss = true;
        int aliveBossCount = 0;
        int visibleBossCount = 0;
        for (int i = 0; i < allBosses.size(); i++) {
            Boss b = allBosses.get(i);
            if (b == null || b.data == null || b.data.length == 0) {
                continue;
            }
            if (b.id == nro.models.boss.BossID.DEATH_BEAM_1 || b.id == nro.models.boss.BossID.DEATH_BEAM_2
                    || b.id == nro.models.boss.BossID.DEATH_BEAM_3 || b.id == nro.models.boss.BossID.DEATH_BEAM_4
                    || b.id == nro.models.boss.BossID.DEATH_BEAM_5 || "$".equals(b.data[0].getName())) {
                continue;
            }
            if ((b.id >= nro.models.boss.BossID.XEN_CON_7 && b.id <= nro.models.boss.BossID.XEN_CON_1) && (b.zone == null || b.isDie())) {
                continue;
            }
            if (b.data[0].getMapJoin() != null && b.data[0].getMapJoin().length > 0) {
                int firstMap = b.data[0].getMapJoin()[0];
                if (nro.models.map.service.MapService.gI().isMapBossFinal(firstMap)
                        || nro.models.map.service.MapService.gI().isMapYardart(firstMap)
                        || nro.models.map.service.MapService.gI().isMapMaBu(firstMap)
                        || nro.models.map.service.MapService.gI().isMapMabu2H(firstMap)) {
                    continue;
                }
            }
            visibleBossCount++;
            if (!firstBoss) {
                sb.append(",");
            }
            firstBoss = false;
            boolean alive = b.zone != null && !b.isDie();
            if (alive) {
                aliveBossCount++;
            }
            int lv = (b.currentLevel >= 0 && b.currentLevel < b.data.length) ? b.currentLevel : 0;
            String bName = b.name != null ? b.name : b.data[lv].getName();
            long hp = (b.nPoint != null && alive) ? b.nPoint.hp : 0;
            long hpMax = (b.nPoint != null) ? b.nPoint.hpMax : 0;
            String mapName = (alive && b.zone.map != null) ? b.zone.map.mapName : "Chưa xuất hiện";
            int mapId = (alive && b.zone.map != null) ? b.zone.map.mapId : -1;
            int zoneId = alive ? b.zone.zoneId : -1;
            String statusStr = b.bossStatus != null ? b.bossStatus.name() : "REST";

            sb.append("{");
            sb.append("\"index\":").append(i);
            sb.append(",\"id\":").append(b.id);
            sb.append(",\"name\":").append(jsonStr(bName));
            sb.append(",\"alive\":").append(alive);
            sb.append(",\"hp\":").append(hp);
            sb.append(",\"hpMax\":").append(hpMax);
            sb.append(",\"mapName\":").append(jsonStr(mapName));
            sb.append(",\"mapId\":").append(mapId);
            sb.append(",\"zoneId\":").append(zoneId);
            sb.append(",\"status\":").append(jsonStr(statusStr));
            sb.append("}");
        }
        sb.append("]");
        sb.append(",\"aliveBossCount\":").append(aliveBossCount);
        sb.append(",\"totalBossCount\":").append(visibleBossCount);
        sb.append("}");

        sendJson(exchange, 200, sb.toString());
    }

    /**
     * Queries all accounts & characters in MySQL (both Online & Offline).
     */
    private static void handleAccounts(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        if (!checkAuth(exchange, params)) {
            return;
        }

        String q = params.getOrDefault("q", "").trim();
        StringBuilder sb = new StringBuilder(8192);
        sb.append("{\"ok\":true,\"accounts\":[");

        String sql = "SELECT a.id AS acc_id, a.username, a.password, a.is_admin, a.ban, a.active, a.vnd, a.tongnap, a.last_time_login, "
                + "p.id AS player_id, p.name AS player_name, p.gender, p.data_inventory, p.data_point "
                + "FROM account a LEFT JOIN player p ON p.account_id = a.id ";
        if (!q.isEmpty()) {
            sql += "WHERE a.username LIKE ? OR p.name LIKE ? OR CAST(a.id AS CHAR) = ? ";
        }
        sql += "ORDER BY a.id DESC LIMIT 100";

        boolean first = true;
        int totalAccounts = 0;
        try (Connection con = LocalManager.getConnection()) {
            try (PreparedStatement countPs = con.prepareStatement("SELECT COUNT(*) FROM account");
                 ResultSet crs = countPs.executeQuery()) {
                if (crs.next()) {
                    totalAccounts = crs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (!q.isEmpty()) {
                    String like = "%" + q + "%";
                    ps.setString(1, like);
                    ps.setString(2, like);
                    ps.setString(3, q);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (!first) {
                            sb.append(",");
                        }
                        first = false;
                        int accId = rs.getInt("acc_id");
                        String username = rs.getString("username");
                        String password = rs.getString("password");
                        boolean isAdmin = rs.getBoolean("is_admin");
                        boolean ban = rs.getBoolean("ban");
                        boolean active = rs.getBoolean("active");
                        int vnd = rs.getInt("vnd");
                        int tongnap = rs.getInt("tongnap");
                        String lastLogin = rs.getString("last_time_login");
                        long playerId = rs.getLong("player_id");
                        String playerName = rs.getString("player_name");
                        int gender = rs.getInt("gender");

                        long gold = 0, gem = 0, ruby = 0, power = 0;
                        Player livePl = Client.gI().getPlayerByUser(accId);
                        boolean isOnline = (livePl != null);

                        if (isOnline && livePl.inventory != null && livePl.nPoint != null) {
                            gold = livePl.inventory.gold;
                            gem = livePl.inventory.gem;
                            ruby = livePl.inventory.ruby;
                            power = livePl.nPoint.power;
                            if (livePl.getSession() != null) {
                                vnd = livePl.getSession().vnd;
                            }
                        } else {
                            try {
                                String invStr = rs.getString("data_inventory");
                                if (invStr != null && !invStr.isEmpty()) {
                                    JSONArray arr = (JSONArray) JSONValue.parse(invStr);
                                    if (arr != null && arr.size() >= 3) {
                                        gold = Long.parseLong(String.valueOf(arr.get(0)));
                                        gem = Long.parseLong(String.valueOf(arr.get(1)));
                                        ruby = Long.parseLong(String.valueOf(arr.get(2)));
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                            try {
                                String ptStr = rs.getString("data_point");
                                if (ptStr != null && !ptStr.isEmpty()) {
                                    JSONArray arr = (JSONArray) JSONValue.parse(ptStr);
                                    if (arr != null && arr.size() >= 2) {
                                        power = Long.parseLong(String.valueOf(arr.get(1)));
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        String genderName = playerName == null ? "Chưa tạo NV" : (gender == 0 ? "Trái Đất" : (gender == 1 ? "Namek" : "Xayda"));
                        sb.append("{");
                        sb.append("\"accId\":").append(accId);
                        sb.append(",\"username\":").append(jsonStr(username));
                        sb.append(",\"password\":").append(jsonStr(password));
                        sb.append(",\"isAdmin\":").append(isAdmin);
                        sb.append(",\"ban\":").append(ban);
                        sb.append(",\"active\":").append(active);
                        sb.append(",\"vnd\":").append(vnd);
                        sb.append(",\"tongnap\":").append(tongnap);
                        sb.append(",\"lastLogin\":").append(jsonStr(lastLogin != null ? lastLogin : ""));
                        sb.append(",\"playerId\":").append(playerId);
                        sb.append(",\"playerName\":").append(jsonStr(playerName != null ? playerName : "(Chưa tạo nhân vật)"));
                        sb.append(",\"gender\":").append(jsonStr(genderName));
                        sb.append(",\"power\":").append(power);
                        sb.append(",\"gold\":").append(gold);
                        sb.append(",\"gem\":").append(gem);
                        sb.append(",\"ruby\":").append(ruby);
                        sb.append(",\"online\":").append(isOnline);
                        sb.append("}");
                    }
                }
            }
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"ok\":false,\"message\":" + jsonStr("Lỗi truy vấn Database: " + e.getMessage()) + "}");
            return;
        }

        sb.append("],\"totalAccounts\":").append(totalAccounts).append("}");
        sendJson(exchange, 200, sb.toString());
    }

    /**
     * Searches Manager.ITEM_TEMPLATES and Manager.ITEM_OPTION_TEMPLATES in-memory.
     */
    private static void handleItems(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        if (!checkAuth(exchange, params)) {
            return;
        }

        String rawQ = params.getOrDefault("q", "").trim();
        String normQ = normalizeSearch(rawQ);
        int typeFilter = (int) parseLongSafe(params.get("type"), -1);

        StringBuilder sb = new StringBuilder(131072);
        sb.append("{\"ok\":true,\"totalItems\":").append(Manager.ITEM_TEMPLATES.size());
        sb.append(",\"totalOptions\":").append(Manager.ITEM_OPTION_TEMPLATES.size());
        sb.append(",\"items\":[");

        int count = 0;
        for (Template.ItemTemplate it : Manager.ITEM_TEMPLATES) {
            if (it == null) {
                continue;
            }
            if (typeFilter >= 0 && it.type != typeFilter) {
                continue;
            }
            if (!normQ.isEmpty()) {
                String idStr = String.valueOf(it.id);
                String normName = normalizeSearch(it.name);
                String normDesc = normalizeSearch(it.description);
                if (!idStr.equals(normQ) && !normName.contains(normQ) && !normDesc.contains(normQ)) {
                    continue;
                }
            }
            if (count > 0) {
                sb.append(",");
            }
            String gName = it.gender == 0 ? "Trái Đất" : (it.gender == 1 ? "Namek" : (it.gender == 2 ? "Xayda" : "Tất cả"));
            sb.append("{");
            sb.append("\"id\":").append(it.id);
            sb.append(",\"name\":").append(jsonStr(it.name));
            sb.append(",\"type\":").append(it.type);
            sb.append(",\"gender\":").append(jsonStr(gName));
            sb.append(",\"description\":").append(jsonStr(it.description));
            sb.append(",\"powerRequire\":").append(it.strRequire);
            sb.append("}");
            count++;
        }
        sb.append("],\"options\":[");

        int optCount = 0;
        for (Template.ItemOptionTemplate iot : Manager.ITEM_OPTION_TEMPLATES) {
            if (iot == null) {
                continue;
            }
            if (!normQ.isEmpty()) {
                String idStr = String.valueOf(iot.id);
                String normName = normalizeSearch(iot.name);
                if (!idStr.equals(normQ) && !normName.contains(normQ)) {
                    continue;
                }
            }
            if (optCount > 0) {
                sb.append(",");
            }
            sb.append("{");
            sb.append("\"id\":").append(iot.id);
            sb.append(",\"name\":").append(jsonStr(iot.name));
            sb.append("}");
            optCount++;
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    /**
     * Returns Server Rankings, Bot list, and Backup files / logs.
     */
    private static void handleSystem(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        if (!checkAuth(exchange, params)) {
            return;
        }

        StringBuilder sb = new StringBuilder(8192);
        sb.append("{\"ok\":true");
        sb.append(",\"expRate\":").append(Manager.RATE_EXP_SERVER);
        sb.append(",\"botCount\":").append(BotManager.gI().bot != null ? BotManager.gI().bot.size() : 0);

        // Top players by power
        List<Map<String, Object>> rankList = new ArrayList<>();
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT p.id, p.name, p.gender, p.data_point, p.data_inventory, a.vnd, a.tongnap FROM player p LEFT JOIN account a ON a.id = p.account_id LIMIT 300");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long power = 0, gold = 0, gem = 0, ruby = 0;
                try {
                    JSONArray pt = (JSONArray) JSONValue.parse(rs.getString("data_point"));
                    if (pt != null && pt.size() >= 2) {
                        power = Long.parseLong(String.valueOf(pt.get(1)));
                    }
                } catch (Exception ignored) {
                }
                try {
                    JSONArray inv = (JSONArray) JSONValue.parse(rs.getString("data_inventory"));
                    if (inv != null && inv.size() >= 3) {
                        gold = Long.parseLong(String.valueOf(inv.get(0)));
                        gem = Long.parseLong(String.valueOf(inv.get(1)));
                        ruby = Long.parseLong(String.valueOf(inv.get(2)));
                    }
                } catch (Exception ignored) {
                }
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("name", rs.getString("name"));
                int g = rs.getInt("gender");
                row.put("gender", g == 0 ? "Trái Đất" : (g == 1 ? "Namek" : "Xayda"));
                row.put("power", power);
                row.put("gold", gold);
                row.put("gem", gem);
                row.put("ruby", ruby);
                row.put("vnd", rs.getInt("vnd"));
                row.put("tongnap", rs.getInt("tongnap"));
                rankList.add(row);
            }
        } catch (Exception ignored) {
        }

        rankList.sort((a, b) -> Long.compare((Long) b.get("power"), (Long) a.get("power")));
        sb.append(",\"topPlayers\":[");
        for (int i = 0; i < Math.min(20, rankList.size()); i++) {
            if (i > 0) {
                sb.append(",");
            }
            Map<String, Object> r = rankList.get(i);
            sb.append("{");
            sb.append("\"rank\":").append(i + 1);
            sb.append(",\"id\":").append(r.get("id"));
            sb.append(",\"name\":").append(jsonStr((String) r.get("name")));
            sb.append(",\"gender\":").append(jsonStr((String) r.get("gender")));
            sb.append(",\"power\":").append(r.get("power"));
            sb.append(",\"gold\":").append(r.get("gold"));
            sb.append(",\"gem\":").append(r.get("gem"));
            sb.append(",\"ruby\":").append(r.get("ruby"));
            sb.append(",\"vnd\":").append(r.get("vnd"));
            sb.append(",\"tongnap\":").append(r.get("tongnap"));
            sb.append("}");
        }
        sb.append("]");

        // Backup files & log
        sb.append(",\"backups\":[");
        File backupDir = new File("/root/NRO/backups");
        if (backupDir.exists() && backupDir.isDirectory()) {
            File[] files = backupDir.listFiles((d, name) -> name.endsWith(".sql.gz"));
            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                for (int i = 0; i < Math.min(10, files.length); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    File f = files[i];
                    sb.append("{");
                    sb.append("\"name\":").append(jsonStr(f.getName()));
                    sb.append(",\"sizeKb\":").append(f.length() / 1024);
                    sb.append(",\"time\":").append(jsonStr(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(f.lastModified()))));
                    sb.append("}");
                }
            }
        }
        sb.append("]");

        String logTail = "";
        File logFile = new File("/root/NRO/backups/backup.log");
        if (logFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
                int from = Math.max(0, lines.size() - 20);
                logTail = String.join("\n", lines.subList(from, lines.size()));
            } catch (Exception ignored) {
            }
        }
        boolean hasGitToken = new File("/root/.nro_git_token").exists() && new File("/root/.nro_git_token").length() > 5;
        sb.append(",\"hasGitToken\":").append(hasGitToken);
        sb.append(",\"backupLog\":").append(jsonStr(logTail));

        String srvLogTail = "";
        File srvLogFile = new File("server.log");
        if (srvLogFile.exists()) {
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(srvLogFile, "r")) {
                long len = raf.length();
                int readBytes = (int) Math.min(len, 16384L);
                if (readBytes > 0) {
                    raf.seek(len - readBytes);
                    byte[] buf = new byte[readBytes];
                    raf.readFully(buf);
                    String raw = new String(buf, StandardCharsets.UTF_8).replace("\0", "");
                    String[] lines = raw.split("\\r?\\n");
                    int from = Math.max(0, lines.length - 30);
                    StringBuilder logSb = new StringBuilder();
                    for (int i = from; i < lines.length; i++) {
                        if (i > from) {
                            logSb.append("\n");
                        }
                        logSb.append(lines[i]);
                    }
                    srvLogTail = logSb.toString();
                }
            } catch (Exception ignored) {
            }
        }
        sb.append(",\"serverLog\":").append(jsonStr(srvLogTail));
        sb.append("}");
        sendJson(exchange, 200, sb.toString());
    }

    private static void handleAction(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                params.putAll(parseQuery(body));
            }
        }
        if (!checkAuth(exchange, params)) {
            return;
        }

        String action = params.getOrDefault("action", "");
        try {
            switch (action) {
                case "announce" -> {
                    String text = params.getOrDefault("text", "").trim();
                    if (text.isEmpty()) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Nội dung thông báo trống!\"}");
                        return;
                    }
                    Service.gI().sendThongBaoAllPlayer("[ADMIN] " + text);
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Đã gửi thông báo tới toàn bộ người chơi đang online!\"}");
                }
                case "give_item" -> {
                    String target = params.getOrDefault("playerTarget", params.getOrDefault("playerId", "")).trim();
                    long gold = parseLongSafe(params.get("gold"), 0);
                    int gem = (int) parseLongSafe(params.get("gem"), 0);
                    int ruby = (int) parseLongSafe(params.get("ruby"), 0);
                    int itemId = (int) parseLongSafe(params.get("itemId"), -1);
                    int quantity = (int) Math.max(1, parseLongSafe(params.get("quantity"), 1));
                    String customOptions = params.getOrDefault("options", "").trim();

                    List<Player> recipients = new ArrayList<>();
                    if ("ALL".equalsIgnoreCase(target)) {
                        for (Player p : new ArrayList<>(Client.gI().getPlayers())) {
                            if (p != null && !p.isBot) {
                                recipients.add(p);
                            }
                        }
                        if (recipients.isEmpty()) {
                            sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Hiện không có người chơi nào đang Online để phát quà toàn Server!\"}");
                            return;
                        }
                    } else {
                        Player pl = null;
                        long pid = parseLongSafe(target, -1);
                        if (pid > 0) {
                            pl = Client.gI().getPlayer(pid);
                        }
                        if (pl == null && !target.isEmpty()) {
                            pl = Client.gI().getPlayer(target);
                            if (pl == null) {
                                for (Player p : new ArrayList<>(Client.gI().getPlayers())) {
                                    if (p != null && p.name != null && p.name.equalsIgnoreCase(target)) {
                                        pl = p;
                                        break;
                                    }
                                }
                            }
                        }
                        if (pl == null) {
                            sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Không tìm thấy người chơi [" + target + "] đang Online! Hãy kiểm tra lại Tên nhân vật / ID hoặc chọn từ danh sách Online.\"}");
                            return;
                        }
                        recipients.add(pl);
                    }

                    StringBuilder givenDesc = new StringBuilder();
                    for (Player pl : recipients) {
                        StringBuilder given = new StringBuilder();
                        if (gold > 0) {
                            pl.inventory.gold = Math.min(Inventory.LIMIT_GOLD, pl.inventory.gold + gold);
                            given.append(" +").append(gold).append(" Vàng");
                        }
                        if (gem > 0) {
                            pl.inventory.gem = (int) Math.min(Integer.MAX_VALUE, (long) pl.inventory.gem + gem);
                            given.append(" +").append(gem).append(" Ngọc Xanh");
                        }
                        if (ruby > 0) {
                            pl.inventory.ruby = (int) Math.min(Integer.MAX_VALUE, (long) pl.inventory.ruby + ruby);
                            given.append(" +").append(ruby).append(" Hồng Ngọc");
                        }
                        if (itemId > 0) {
                            Item item = ItemService.gI().createNewItem((short) itemId, quantity);
                            if (item != null && item.template != null) {
                                if (!customOptions.isEmpty()) {
                                    for (String pair : customOptions.split("[,;]")) {
                                        String[] kv = pair.trim().split("[:=]");
                                        if (kv.length == 2) {
                                            int optId = (int) parseLongSafe(kv[0], -1);
                                            int optParam = (int) parseLongSafe(kv[1], 0);
                                            if (optId >= 0) {
                                                item.itemOptions.add(new Item.ItemOption(optId, optParam));
                                            }
                                        }
                                    }
                                } else {
                                    List<Item.ItemOption> shopOps = ItemService.gI().getListOptionItemShop((short) itemId);
                                    if (shopOps != null && !shopOps.isEmpty()) {
                                        item.itemOptions.addAll(shopOps);
                                    }
                                }
                                InventoryService.gI().addItemBag(pl, item);
                                InventoryService.gI().sendItemBags(pl);
                                given.append(" +x").append(quantity).append(" ").append(item.template.name);
                            }
                        }
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBao(pl, "Admin vừa trao thưởng cho bạn:" + given);
                        if (givenDesc.length() == 0) {
                            givenDesc.append(given);
                        }
                    }

                    String targetLabel = recipients.size() > 1
                            ? ("Tất cả " + recipients.size() + " người chơi Online")
                            : recipients.get(0).name;
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã trao cho [" + targetLabel + "]:" + givenDesc) + "}");
                }
                case "heal_player" -> {
                    long playerId = Long.parseLong(params.getOrDefault("playerId", "-1"));
                    Player pl = Client.gI().getPlayer(playerId);
                    if (pl == null || pl.nPoint == null) {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Người chơi không online!\"}");
                        return;
                    }
                    PlayerService.gI().hoiPhuc(pl, pl.nPoint.hpMax, pl.nPoint.mpMax);
                    Service.gI().sendThongBao(pl, "Admin vừa hồi phục 100% HP & KI cho bạn!");
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã hồi đầy 100% HP & KI cho " + pl.name + "!") + "}");
                }
                case "inspect_player" -> {
                    long playerId = Long.parseLong(params.getOrDefault("playerId", "-1"));
                    Player pl = Client.gI().getPlayer(playerId);
                    if (pl == null || pl.inventory == null) {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Người chơi không online!\"}");
                        return;
                    }
                    StringBuilder sb = new StringBuilder(4096);
                    sb.append("{\"ok\":true,\"name\":").append(jsonStr(pl.name));
                    sb.append(",\"body\":").append(serializeItemList(pl.inventory.itemsBody));
                    sb.append(",\"bag\":").append(serializeItemList(pl.inventory.itemsBag));
                    sb.append(",\"box\":").append(serializeItemList(pl.inventory.itemsBox));
                    sb.append("}");
                    sendJson(exchange, 200, sb.toString());
                }
                case "kick" -> {
                    long playerId = Long.parseLong(params.getOrDefault("playerId", "-1"));
                    Player pl = Client.gI().getPlayer(playerId);
                    if (pl == null || pl.getSession() == null) {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Người chơi không online!\"}");
                        return;
                    }
                    String pName = pl.name;
                    Client.gI().kickSession(pl.getSession());
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã lưu dữ liệu và kick " + pName + " thành công!") + "}");
                }
                case "spawn_boss" -> {
                    int bossIndex = (int) parseLongSafe(params.get("bossIndex"), -1);
                    Boss boss = BossManager.gI().getBoss(bossIndex);
                    if (boss == null) {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Không tìm thấy Boss!\"}");
                        return;
                    }
                    if (boss.zone != null && !boss.isDie()) {
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Boss " + boss.name + " hiện đang sống tại " + boss.zone.map.mapName + " khu " + boss.zone.zoneId) + "}");
                        return;
                    }
                    boss.changeStatus(BossStatus.RESPAWN);
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã gọi hồi sinh Boss #" + bossIndex + " ngay lập tức!") + "}");
                }
                case "kill_boss" -> {
                    int bossIndex = (int) parseLongSafe(params.get("bossIndex"), -1);
                    Boss boss = BossManager.gI().getBoss(bossIndex);
                    if (boss == null) {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Không tìm thấy Boss!\"}");
                        return;
                    }
                    if (boss.zone == null || boss.isDie()) {
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Boss này hiện không có trên bản đồ!\"}");
                        return;
                    }
                    String bName = boss.name;
                    boss.leaveMap();
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã thu hồi / tiêu diệt Boss " + bName + " khỏi bản đồ!") + "}");
                }
                case "spawn_all_bosses" -> {
                    int count = 0;
                    for (Boss b : new ArrayList<>(BossManager.gI().getBosses())) {
                        if (b != null && (b.zone == null || b.isDie())) {
                            b.changeStatus(BossStatus.RESPAWN);
                            count++;
                        }
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã kích hoạt hồi sinh toàn bộ " + count + " Boss đang chờ!") + "}");
                }
                case "set_exp_rate" -> {
                    int rate = (int) Math.max(1, Math.min(127, parseLongSafe(params.get("rate"), 1)));
                    Manager.RATE_EXP_SERVER = (byte) rate;
                    Service.gI().sendThongBaoAllPlayer("[THÔNG BÁO] Admin vừa điều chỉnh tỷ lệ Sức Mạnh / Tiềm Năng toàn Server lên x" + rate + "!");
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã cập nhật Tỷ lệ EXP toàn Server thành x" + rate + " ngay lập tức!") + "}");
                }
                case "spawn_bot" -> {
                    int botType = (int) parseLongSafe(params.get("botType"), 0);
                    int count = (int) Math.max(1, Math.min(50, parseLongSafe(params.get("count"), 5)));
                    NewBot.gI().runBot(botType, null, count);
                    String typeName = botType == 0 ? "Bot Train Quái" : (botType == 2 ? "Bot Săn Boss" : "Bot PK");
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã gọi thêm " + count + " " + typeName + " vào các bản đồ!") + "}");
                }
                case "clear_bots" -> {
                    List<Bot> bots = new ArrayList<>(BotManager.gI().bot);
                    int removed = bots.size();
                    BotManager.gI().bot.clear();
                    for (Bot b : bots) {
                        if (b != null) {
                            try {
                                ChangeMapService.gI().exitMap(b);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã thu hồi toàn bộ " + removed + " Bot khỏi Server!") + "}");
                }
                case "account_update" -> {
                    int accId = (int) parseLongSafe(params.get("accId"), -1);
                    if (accId <= 0) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"ID tài khoản không hợp lệ!\"}");
                        return;
                    }
                    int addVnd = (int) parseLongSafe(params.get("addVnd"), 0);
                    int addTongnap = (int) parseLongSafe(params.get("addTongnap"), 0);
                    String newPass = params.getOrDefault("password", "").trim();
                    int active = (int) parseLongSafe(params.get("active"), -1);
                    int ban = (int) parseLongSafe(params.get("ban"), -1);
                    int isAdmin = (int) parseLongSafe(params.get("isAdmin"), -1);

                    try (Connection con = LocalManager.getConnection()) {
                        if (addVnd != 0 || addTongnap != 0) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET vnd = GREATEST(0, vnd + ?), tongnap = GREATEST(0, tongnap + ?) WHERE id = ?")) {
                                ps.setInt(1, addVnd);
                                ps.setInt(2, addTongnap);
                                ps.setInt(3, accId);
                                ps.executeUpdate();
                            }
                        }
                        if (!newPass.isEmpty()) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET password = ? WHERE id = ?")) {
                                ps.setString(1, newPass);
                                ps.setInt(2, accId);
                                ps.executeUpdate();
                            }
                        }
                        if (active == 0 || active == 1) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET active = ? WHERE id = ?")) {
                                ps.setInt(1, active);
                                ps.setInt(2, accId);
                                ps.executeUpdate();
                            }
                        }
                        if (ban == 0 || ban == 1) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET ban = ? WHERE id = ?")) {
                                ps.setInt(1, ban);
                                ps.setInt(2, accId);
                                ps.executeUpdate();
                            }
                        }
                        if (isAdmin == 0 || isAdmin == 1) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET is_admin = ? WHERE id = ?")) {
                                ps.setInt(1, isAdmin);
                                ps.setInt(2, accId);
                                ps.executeUpdate();
                            }
                        }
                    }

                    // Nếu tài khoản đang online thì cập nhật trực tiếp phiên hiện tại
                    Player livePl = Client.gI().getPlayerByUser(accId);
                    if (livePl != null && livePl.getSession() != null) {
                        if (addVnd != 0) {
                            livePl.getSession().vnd = Math.max(0, livePl.getSession().vnd + addVnd);
                            Service.gI().sendThongBao(livePl, "Admin vừa cộng " + addVnd + " VNĐ vào tài khoản của bạn!");
                        }
                        if (addTongnap != 0) {
                            livePl.getSession().tongnap = Math.max(0, livePl.getSession().tongnap + addTongnap);
                        }
                        if (active == 0 || active == 1) {
                            livePl.getSession().actived = (active == 1);
                            if (active == 1) {
                                Service.gI().sendThongBao(livePl, "Tài khoản của bạn đã được Admin Mở Thành Viên!");
                            }
                        }
                        if (isAdmin == 0 || isAdmin == 1) {
                            livePl.getSession().isAdmin = (isAdmin == 1);
                        }
                        if (ban == 1) {
                            Client.gI().kickSession(livePl.getSession());
                        }
                    }

                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã cập nhật tài khoản #" + accId + " trong MySQL thành công!") + "}");
                }
                case "account_create" -> {
                    String u = params.getOrDefault("username", "").trim();
                    String p = params.getOrDefault("password", "").trim();
                    int vnd = (int) Math.max(0, parseLongSafe(params.get("vnd"), 0));
                    int active = (int) parseLongSafe(params.get("active"), 1);
                    if (u.isEmpty() || p.isEmpty()) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Tài khoản và mật khẩu không được để trống!\"}");
                        return;
                    }
                    try (Connection con = LocalManager.getConnection()) {
                        try (PreparedStatement check = con.prepareStatement("SELECT id FROM account WHERE username = ? LIMIT 1")) {
                            check.setString(1, u);
                            try (ResultSet rs = check.executeQuery()) {
                                if (rs.next()) {
                                    sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Tên tài khoản này đã tồn tại!\"}");
                                    return;
                                }
                            }
                        }
                        try (PreparedStatement ins = con.prepareStatement("INSERT INTO account (username, password, active, vnd, tongnap) VALUES (?, ?, ?, ?, ?)")) {
                            ins.setString(1, u);
                            ins.setString(2, p);
                            ins.setInt(3, active == 1 ? 1 : 0);
                            ins.setInt(4, vnd);
                            ins.setInt(5, vnd);
                            ins.executeUpdate();
                        }
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã tạo tài khoản mới [" + u + "] thành công!") + "}");
                }
                case "save_all" -> {
                    int savedCount = 0;
                    for (Player pl : new ArrayList<>(Client.gI().getPlayers())) {
                        if (pl != null) {
                            PlayerDAO.updatePlayer(pl);
                            savedCount++;
                        }
                    }
                    try {
                        ClanService.gI().close();
                    } catch (Exception ignored) {
                    }
                    try {
                        ConsignShopManager.gI().save();
                    } catch (Exception ignored) {
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã lưu thành công dữ liệu của " + savedCount + " người chơi, Bang hội & Ký gửi vào MySQL!") + "}");
                }
                case "backup_now" -> {
                    String token = params.getOrDefault("gitToken", "").trim();
                    if (!token.isEmpty()) {
                        Files.writeString(new File("/root/.nro_git_token").toPath(), token + "\n", StandardCharsets.UTF_8);
                    }
                    String os = System.getProperty("os.name").toLowerCase();
                    if (!os.contains("win") && new File("backup_db.sh").exists()) {
                        Process p = new ProcessBuilder("/bin/bash", "-c", "./backup_db.sh >> /root/NRO/backups/backup.log 2>&1")
                                .directory(new File("."))
                                .start();
                        p.waitFor(25, TimeUnit.SECONDS);
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Đã chạy Backup MySQL và đẩy lên GitHub Private! Hãy xem kết quả ở khung Nhật ký Backup.\"}");
                    } else {
                        sendJson(exchange, 200, "{\"ok\":false,\"message\":\"Chức năng chạy backup_db.sh chỉ hoạt động trên VPS Linux!\"}");
                    }
                }
                case "update_and_restart" -> {
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        sendJson(exchange, 200, "{\"ok\":false,\"message\":\"Chức năng tự động Git Pull & Khởi động lại chỉ dành cho VPS Linux!\"}");
                        return;
                    }
                    for (Player pl : new ArrayList<>(Client.gI().getPlayers())) {
                        if (pl != null) {
                            PlayerDAO.updatePlayer(pl);
                        }
                    }
                    try {
                        ClanService.gI().close();
                    } catch (Exception ignored) {
                    }
                    try {
                        ConsignShopManager.gI().save();
                    } catch (Exception ignored) {
                    }
                    Service.gI().sendThongBaoAllPlayer("[THÔNG BÁO] Server đang cập nhật phiên bản mới và khởi động lại, vui lòng đăng nhập lại sau 10 giây!");
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Đang tự động kéo code mới từ GitHub (git pull) và khởi động lại Server! Vui lòng đợi khoảng 8-12 giây để Server bật lại...\"}");

                    new Thread(() -> {
                        try {
                            Thread.sleep(800);
                            new ProcessBuilder("/bin/bash", "-c",
                                    "nohup /bin/bash -c 'cd /root/NRO/SRC && git fetch origin && git reset --hard origin/main && sed -i \"s/\\r$//\" run.sh backup_db.sh && chmod +x run.sh backup_db.sh && ./run.sh' > /root/NRO/SRC/update.log 2>&1 < /dev/null &")
                                    .directory(new File("."))
                                    .start();
                        } catch (Exception ignored) {
                        }
                    }, "WebAutoUpdater").start();
                }
                default -> sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Hành động không hợp lệ!\"}");
            }
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"ok\":false,\"message\":" + jsonStr("Lỗi: " + e.getMessage()) + "}");
        }
    }

    private static String serializeItemList(List<Item> items) {
        if (items == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (it == null || !it.isNotNullItem() || it.template == null) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("{");
            sb.append("\"slot\":").append(i);
            sb.append(",\"id\":").append(it.template.id);
            sb.append(",\"name\":").append(jsonStr(it.template.name));
            sb.append(",\"quantity\":").append(it.quantity);
            sb.append(",\"options\":").append(jsonStr(it.getOptionInfo()));
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String normalizeSearch(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String norm = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd');
    }

    private static void handleBackupDownload(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        if (!checkAuth(exchange, params)) {
            return;
        }
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win") && new File("backup_db.sh").exists()) {
                Process p = new ProcessBuilder("/bin/bash", "./backup_db.sh")
                        .directory(new File("."))
                        .start();
                p.waitFor(20, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }

        File backupFile = new File("/root/NRO/backups/team2026_latest.sql.gz");
        if (!backupFile.exists() || backupFile.length() == 0) {
            sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Chưa tìm thấy file backup trên VPS!\"}");
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/gzip");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"team2026_latest.sql.gz\"");
        exchange.sendResponseHeaders(200, backupFile.length());
        try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(backupFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    private static long parseLongSafe(String val, long def) {
        if (val == null || val.trim().isEmpty()) {
            return def;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String jsonStr(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }
}
