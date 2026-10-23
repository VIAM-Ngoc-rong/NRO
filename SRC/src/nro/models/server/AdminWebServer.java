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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.services.ClanService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.shop_ky_gui.ConsignShopManager;
import nro.models.utils.Logger;

/**
 * Lightweight built-in HTTP Admin API & Dashboard Server (Port 14446).
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
            server.createContext("/api/action", AdminWebServer::handleAction);
            server.createContext("/api/backup/download", AdminWebServer::handleBackupDownload);
            server.setExecutor(Executors.newFixedThreadPool(2));
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
        long uptimeSec = (System.currentTimeMillis() - START_TIME_MS) / 1000;
        int threadsCount = Thread.activeCount();

        List<Player> onlinePlayers = new ArrayList<>(Client.gI().getPlayers());
        List<Boss> allBosses = new ArrayList<>(BossManager.gI().getBosses());

        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"ok\":true");
        sb.append(",\"serverName\":").append(jsonStr(ServerManager.NAME));
        sb.append(",\"timeStart\":").append(jsonStr(ServerManager.timeStart != null ? ServerManager.timeStart : ""));
        sb.append(",\"uptimeSec\":").append(uptimeSec);
        sb.append(",\"usedRamMb\":").append(usedRamMb);
        sb.append(",\"maxRamMb\":").append(maxRamMb);
        sb.append(",\"threadsCount\":").append(threadsCount);
        sb.append(",\"onlineCount\":").append(onlinePlayers.size());

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
        for (int i = 0; i < allBosses.size(); i++) {
            Boss b = allBosses.get(i);
            if (b == null || b.data == null || b.data.length == 0) {
                continue;
            }
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
        sb.append(",\"totalBossCount\":").append(allBosses.size());
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
                    long playerId = Long.parseLong(params.getOrDefault("playerId", "-1"));
                    Player pl = Client.gI().getPlayer(playerId);
                    if (pl == null) {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Người chơi không còn online!\"}");
                        return;
                    }
                    long gold = parseLongSafe(params.get("gold"), 0);
                    int gem = (int) parseLongSafe(params.get("gem"), 0);
                    int ruby = (int) parseLongSafe(params.get("ruby"), 0);
                    int itemId = (int) parseLongSafe(params.get("itemId"), -1);
                    int quantity = (int) Math.max(1, parseLongSafe(params.get("quantity"), 1));

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
                            List<Item.ItemOption> shopOps = ItemService.gI().getListOptionItemShop((short) itemId);
                            if (shopOps != null && !shopOps.isEmpty()) {
                                item.itemOptions.addAll(shopOps);
                            }
                            InventoryService.gI().addItemBag(pl, item);
                            InventoryService.gI().sendItemBags(pl);
                            given.append(" +x").append(quantity).append(" ").append(item.template.name);
                        }
                    }
                    Service.gI().sendMoney(pl);
                    Service.gI().sendThongBao(pl, "Admin vừa trao thưởng cho bạn:" + given);
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã trao cho " + pl.name + ":" + given) + "}");
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
                    String os = System.getProperty("os.name").toLowerCase();
                    if (!os.contains("win") && new File("backup_db.sh").exists()) {
                        Process p = new ProcessBuilder("/bin/bash", "./backup_db.sh")
                                .directory(new File("."))
                                .start();
                        p.waitFor(20, TimeUnit.SECONDS);
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Đã tạo bản Backup MySQL mới nhất tại /root/NRO/backups/team2026_latest.sql.gz!\"}");
                    } else {
                        sendJson(exchange, 200, "{\"ok\":false,\"message\":\"Chức năng chạy backup_db.sh chỉ hoạt động trên VPS Linux!\"}");
                    }
                }
                default -> sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Hành động không hợp lệ!\"}");
            }
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"ok\":false,\"message\":" + jsonStr("Lỗi: " + e.getMessage()) + "}");
        }
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
