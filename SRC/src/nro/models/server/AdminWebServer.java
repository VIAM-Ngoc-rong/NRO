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
import java.sql.Timestamp;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.ChristmasEventManager;
import nro.models.boss.Boss_Manager.HalloweenEventManager;
import nro.models.boss.Boss_Manager.HungVuongEventManager;
import nro.models.boss.Boss_Manager.LunarNewYearEventManager;
import nro.models.boss.Boss_Manager.TrungThuEventManager;
import nro.models.consts.BossStatus;
import nro.models.data.LocalManager;
import nro.models.database.PlayerDAO;
import nro.models.event.EventManager;
import nro.models.intrinsic.Intrinsic;
import nro.models.item.Item;
import nro.models.managers.GiftCodeManager;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.player_system.GiftCode;
import nro.models.player_system.Template;
import nro.models.services.ClanService;
import nro.models.services.IntrinsicService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.shop_ky_gui.ConsignShopManager;
import nro.models.task.SubTaskMain;
import nro.models.task.TaskMain;
import nro.models.utils.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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

    private static final int MAX_BOSS_KILL_HISTORY = 150;
    private static final List<BossKillRecord> BOSS_KILL_HISTORY = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean bossKillStatsHydrated = false;
    private static volatile String cachedIntrinsicsJson = null;
    private static volatile String cachedTasksJson = null;
    private static volatile long lastAutoGcTimeMs = 0L;

    public static class BossKillRecord {
        public final long timeMs;
        public final int bossId;
        public final String bossName;
        public final long hpMax;
        public final String killerName;
        public final boolean isBot;
        public final String mapName;
        public final int mapId;
        public final int zoneId;

        public BossKillRecord(long timeMs, int bossId, String bossName, long hpMax, String killerName, boolean isBot, String mapName, int mapId, int zoneId) {
            this.timeMs = timeMs;
            this.bossId = bossId;
            this.bossName = bossName != null ? bossName : "";
            this.hpMax = hpMax;
            this.killerName = killerName != null ? killerName : "";
            this.isBot = isBot;
            this.mapName = mapName != null ? mapName : "";
            this.mapId = mapId;
            this.zoneId = zoneId;
        }
    }

    private static void ensureBossKillTable(Connection con) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "CREATE TABLE IF NOT EXISTS boss_kill_history ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "time_ms BIGINT NOT NULL, "
                + "boss_id INT NOT NULL, "
                + "boss_name VARCHAR(128) NOT NULL, "
                + "hp_max BIGINT NOT NULL, "
                + "killer_name VARCHAR(128) NOT NULL, "
                + "is_bot TINYINT(1) NOT NULL DEFAULT 0, "
                + "map_name VARCHAR(128) NOT NULL, "
                + "map_id INT NOT NULL DEFAULT -1, "
                + "zone_id INT NOT NULL DEFAULT -1"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(
                "ALTER TABLE account ADD COLUMN total_play_sec BIGINT NOT NULL DEFAULT 0")) {
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE account SET total_play_sec = GREATEST(60, LEAST(86400 * 30, TIMESTAMPDIFF(SECOND, last_time_login, last_time_logout))) "
                + "WHERE total_play_sec = 0 AND last_time_logout > last_time_login")) {
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE account a INNER JOIN player p ON p.account_id = a.id "
                + "SET a.total_play_sec = GREATEST(60, LEAST(14400, TIMESTAMPDIFF(SECOND, a.create_time, GREATEST(a.last_time_login, a.last_time_logout)))) "
                + "WHERE a.total_play_sec = 0")) {
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    public static void loadBossKillHistoryFromDb() {
        try (Connection con = LocalManager.getConnection()) {
            ensureBossKillTable(con);
            List<BossKillRecord> loaded = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT time_ms, boss_id, boss_name, hp_max, killer_name, is_bot, map_name, map_id, zone_id "
                    + "FROM boss_kill_history ORDER BY id DESC LIMIT " + MAX_BOSS_KILL_HISTORY);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loaded.add(new BossKillRecord(
                            rs.getLong("time_ms"),
                            rs.getInt("boss_id"),
                            rs.getString("boss_name"),
                            rs.getLong("hp_max"),
                            rs.getString("killer_name"),
                            rs.getInt("is_bot") == 1,
                            rs.getString("map_name"),
                            rs.getInt("map_id"),
                            rs.getInt("zone_id")
                    ));
                }
            }
            synchronized (BOSS_KILL_HISTORY) {
                BOSS_KILL_HISTORY.clear();
                BOSS_KILL_HISTORY.addAll(loaded);
            }
        } catch (Exception e) {
            Logger.error("Không thể tải lịch sử tiêu diệt Boss từ MySQL: " + e.getMessage() + "\n");
        }
    }

    private static void hydrateBossKillStatsIfNeeded(List<Boss> allBosses) {
        if (bossKillStatsHydrated || allBosses == null || allBosses.isEmpty()) {
            return;
        }
        bossKillStatsHydrated = true;
        List<BossKillRecord> snapshot;
        synchronized (BOSS_KILL_HISTORY) {
            snapshot = new ArrayList<>(BOSS_KILL_HISTORY);
        }
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            BossKillRecord rec = snapshot.get(i);
            for (Boss b : allBosses) {
                if (b != null && (int) b.id == rec.bossId) {
                    if (rec.timeMs >= b.lastKilledTimeMs) {
                        b.lastKilledTimeMs = rec.timeMs;
                        b.lastKillerName = rec.killerName;
                        b.lastKilledMapName = rec.mapName;
                        b.lastKilledZoneId = rec.zoneId;
                    }
                    b.totalKilledCount++;
                    break;
                }
            }
        }
    }

    public static void recordBossKillHistory(long timeMs, int bossId, String bossName, long hpMax,
            String killerName, boolean isBot, String mapName, int mapId, int zoneId) {
        BossKillRecord rec = new BossKillRecord(timeMs, bossId, bossName, hpMax, killerName, isBot, mapName, mapId, zoneId);
        synchronized (BOSS_KILL_HISTORY) {
            BOSS_KILL_HISTORY.add(0, rec);
            while (BOSS_KILL_HISTORY.size() > MAX_BOSS_KILL_HISTORY) {
                BOSS_KILL_HISTORY.remove(BOSS_KILL_HISTORY.size() - 1);
            }
        }
        Thread.startVirtualThread(() -> {
            try (Connection con = LocalManager.getConnection()) {
                ensureBossKillTable(con);
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO boss_kill_history (time_ms, boss_id, boss_name, hp_max, killer_name, is_bot, map_name, map_id, zone_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setLong(1, rec.timeMs);
                    ps.setInt(2, rec.bossId);
                    ps.setString(3, rec.bossName.length() > 120 ? rec.bossName.substring(0, 120) : rec.bossName);
                    ps.setLong(4, rec.hpMax);
                    ps.setString(5, rec.killerName.length() > 120 ? rec.killerName.substring(0, 120) : rec.killerName);
                    ps.setInt(6, rec.isBot ? 1 : 0);
                    ps.setString(7, rec.mapName.length() > 120 ? rec.mapName.substring(0, 120) : rec.mapName);
                    ps.setInt(8, rec.mapId);
                    ps.setInt(9, rec.zoneId);
                    ps.executeUpdate();
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static String serializeBossKillHistory() {
        List<BossKillRecord> snapshot;
        synchronized (BOSS_KILL_HISTORY) {
            snapshot = new ArrayList<>(BOSS_KILL_HISTORY);
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < snapshot.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            BossKillRecord r = snapshot.get(i);
            sb.append("{")
                    .append("\"timeMs\":").append(r.timeMs)
                    .append(",\"bossId\":").append(r.bossId)
                    .append(",\"bossName\":").append(jsonStr(r.bossName))
                    .append(",\"hpMax\":").append(r.hpMax)
                    .append(",\"killerName\":").append(jsonStr(r.killerName))
                    .append(",\"isBot\":").append(r.isBot)
                    .append(",\"mapName\":").append(jsonStr(r.mapName))
                    .append(",\"mapId\":").append(r.mapId)
                    .append(",\"zoneId\":").append(r.zoneId)
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public static void start() {
        try {
            loadBossKillHistoryFromDb();
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", WEB_PORT), 0);
            server.createContext("/", AdminWebServer::handleDashboardHtml);
            server.createContext("/api/status", AdminWebServer::handleStatus);
            server.createContext("/api/accounts", AdminWebServer::handleAccounts);
            server.createContext("/api/items", AdminWebServer::handleItems);
            server.createContext("/api/system", AdminWebServer::handleSystem);
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
        exchange.getResponseHeaders().set("Connection", "close");
    }

    /**
     * Multiplexes HTTP requests arriving on the main game port (14445) directly
     * to the local HttpServer (127.0.0.1:14446) without spawning extra threads.
     */
    public static void relaySocketToLocalHttp(java.net.Socket clientSocket, InputStream clientIn) {
        try {
            clientSocket.setSoTimeout(3000);
            try (java.net.Socket local = new java.net.Socket("127.0.0.1", WEB_PORT)) {
                local.setSoTimeout(5000);
                OutputStream localOut = local.getOutputStream();
                InputStream localIn = local.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();

                java.io.ByteArrayOutputStream reqBuf = new java.io.ByteArrayOutputStream(2048);
                byte[] tmp = new byte[2048];
                int headerEnd = -1;
                int contentLength = 0;
                while (headerEnd == -1 && reqBuf.size() < 32768) {
                    int n = clientIn.read(tmp);
                    if (n <= 0) {
                        break;
                    }
                    reqBuf.write(tmp, 0, n);
                    byte[] arr = reqBuf.toByteArray();
                    for (int i = 0; i <= arr.length - 4; i++) {
                        if (arr[i] == '\r' && arr[i + 1] == '\n' && arr[i + 2] == '\r' && arr[i + 3] == '\n') {
                            headerEnd = i + 4;
                            String hdrs = new String(arr, 0, headerEnd, StandardCharsets.ISO_8859_1);
                            for (String line : hdrs.split("\r\n")) {
                                if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
                                    try {
                                        contentLength = Integer.parseInt(line.substring(15).trim());
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                while (headerEnd != -1 && reqBuf.size() < headerEnd + contentLength && reqBuf.size() < 65536) {
                    int n = clientIn.read(tmp, 0, Math.min(tmp.length, (headerEnd + contentLength) - reqBuf.size()));
                    if (n <= 0) {
                        break;
                    }
                    reqBuf.write(tmp, 0, n);
                }

                localOut.write(reqBuf.toByteArray());
                localOut.flush();
                local.shutdownOutput();

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
        sendJson(exchange, 200, "{\"ok\":true,\"service\":\"Ngoc Rong VIAM Admin API\"}");
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
        long nowGcCheck = System.currentTimeMillis();
        long rawUsedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        if (rawUsedMb > 400 && (nowGcCheck - lastAutoGcTimeMs) > 20000L) {
            lastAutoGcTimeMs = nowGcCheck;
            System.gc();
        }
        long usedRamMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxRamMb = rt.maxMemory() / (1024 * 1024);
        int cpuCores = rt.availableProcessors();
        long sessionUptimeSec = ServerManager.getSessionUptimeSeconds();
        long totalUptimeSec = Math.max(sessionUptimeSec, ServerManager.getTotalUptimeSeconds());
        double uptimeHours = ServerManager.getTotalUptimeHours();
        int threadsCount = Thread.activeCount();

        List<Player> onlinePlayers = new ArrayList<>(Client.gI().getPlayers());
        List<Boss> allBosses = new ArrayList<>(BossManager.gI().getBosses());
        int botCount = BotManager.gI().bot != null ? BotManager.gI().bot.size() : 0;

        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"ok\":true");
        sb.append(",\"serverName\":").append(jsonStr(ServerManager.NAME));
        sb.append(",\"timeStart\":").append(jsonStr(ServerManager.timeStart != null ? ServerManager.timeStart : ""));
        sb.append(",\"firstTimeStart\":").append(jsonStr(ServerManager.firstStartTime != null ? ServerManager.firstStartTime : ""));
        sb.append(",\"uptimeSec\":").append(totalUptimeSec);
        sb.append(",\"totalUptimeSec\":").append(totalUptimeSec);
        sb.append(",\"sessionUptimeSec\":").append(sessionUptimeSec);
        sb.append(",\"uptimeHours\":").append(uptimeHours);
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
            int ruongSkh = pl.getSession() != null ? pl.getSession().ruongSkh : 5;
            int inId = (pl.playerIntrinsic != null && pl.playerIntrinsic.intrinsic != null) ? pl.playerIntrinsic.intrinsic.id : 0;
            int inP1 = (pl.playerIntrinsic != null && pl.playerIntrinsic.intrinsic != null) ? pl.playerIntrinsic.intrinsic.param1 : 0;
            int inP2 = (pl.playerIntrinsic != null && pl.playerIntrinsic.intrinsic != null) ? pl.playerIntrinsic.intrinsic.param2 : 0;
            String inName = (pl.playerIntrinsic != null && pl.playerIntrinsic.intrinsic != null && inId > 0)
                    ? pl.playerIntrinsic.intrinsic.getName() : "Chưa mở";
            int taskId = (pl.playerTask != null && pl.playerTask.taskMain != null) ? pl.playerTask.taskMain.id : 0;
            int taskIndex = (pl.playerTask != null && pl.playerTask.taskMain != null) ? pl.playerTask.taskMain.index : 0;
            String taskName = formatTaskInfo(taskId, taskIndex);
            long sessionPlaySec = pl.getSession() != null ? pl.getSession().getCurrentSessionPlaySec() : 0L;
            long totalPlaySec = pl.getSession() != null ? pl.getSession().getLiveTotalPlaySec() : sessionPlaySec;
            double playHours = Math.round((totalPlaySec / 3600.0) * 100.0) / 100.0;
            sb.append("{");
            sb.append("\"id\":").append(pl.id);
            sb.append(",\"name\":").append(jsonStr(pl.name));
            sb.append(",\"gender\":").append(jsonStr(genderName));
            sb.append(",\"genderId\":").append(pl.gender);
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
            sb.append(",\"ruongSkh\":").append(ruongSkh);
            sb.append(",\"mapName\":").append(jsonStr(mapName));
            sb.append(",\"mapId\":").append(mapId);
            sb.append(",\"zoneId\":").append(zoneId);
            sb.append(",\"hasPet\":").append(pl.pet != null);
            sb.append(",\"petPower\":").append(pl.pet != null && pl.pet.nPoint != null ? pl.pet.nPoint.power : 0);
            sb.append(",\"intrinsicId\":").append(inId);
            sb.append(",\"intrinsicParam1\":").append(inP1);
            sb.append(",\"intrinsicParam2\":").append(inP2);
            sb.append(",\"intrinsicName\":").append(jsonStr(inName));
            sb.append(",\"taskId\":").append(taskId);
            sb.append(",\"taskIndex\":").append(taskIndex);
            sb.append(",\"taskName\":").append(jsonStr(taskName));
            sb.append(",\"sessionPlaySec\":").append(sessionPlaySec);
            sb.append(",\"totalPlaySec\":").append(totalPlaySec);
            sb.append(",\"playHours\":").append(playHours);
            sb.append("}");
        }
        sb.append("]");

        // Bosses array
        hydrateBossKillStatsIfNeeded(allBosses);
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
            long nowMs = System.currentTimeMillis();
            long spawnTimeMs = (alive && b.lastTimeSpawnOnMap > 0) ? b.lastTimeSpawnOnMap : 0L;
            long aliveSec = spawnTimeMs > 0 ? Math.max(0L, (nowMs - spawnTimeMs) / 1000L) : 0L;
            Boss restRef = (b.parentBoss != null && b.secondsRest <= 0) ? b.parentBoss : b;
            int secondsRest = restRef.secondsRest;
            long lastRestMs = restRef.lastTimeRest;
            long respawnRemainSec = (!alive && secondsRest > 0 && lastRestMs > 0)
                    ? Math.max(0L, (lastRestMs + secondsRest * 1000L - nowMs) / 1000L) : 0L;

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
            sb.append(",\"spawnTimeMs\":").append(spawnTimeMs);
            sb.append(",\"aliveSec\":").append(aliveSec);
            sb.append(",\"respawnRemainSec\":").append(respawnRemainSec);
            sb.append(",\"secondsRest\":").append(secondsRest);
            sb.append(",\"hasParent\":").append(b.parentBoss != null);
            sb.append(",\"lastKilledTimeMs\":").append(b.lastKilledTimeMs);
            sb.append(",\"lastKillerName\":").append(jsonStr(b.lastKillerName != null ? b.lastKillerName : ""));
            sb.append(",\"lastKilledMapName\":").append(jsonStr(b.lastKilledMapName != null ? b.lastKilledMapName : ""));
            sb.append(",\"lastKilledZoneId\":").append(b.lastKilledZoneId);
            sb.append(",\"totalKilledCount\":").append(b.totalKilledCount);
            sb.append("}");
        }
        sb.append("]");
        sb.append(",\"aliveBossCount\":").append(aliveBossCount);
        sb.append(",\"totalBossCount\":").append(visibleBossCount);
        sb.append(",\"bossKillHistory\":").append(serializeBossKillHistory());
        sb.append(",\"giftcodes\":").append(serializeGiftCodes());
        sb.append(",\"intrinsics\":").append(serializeIntrinsics());
        sb.append(",\"tasks\":").append(serializeTasks());
        sb.append(",\"events\":").append(serializeEvents());
        sb.append("}");

        sendJson(exchange, 200, sb.toString());
    }

    private static String getGiftItemDisplayName(int itemId) {
        return switch (itemId) {
            case -1 -> "Vàng";
            case -2 -> "Ngọc xanh";
            case -3 -> "Hồng ngọc";
            case -4 -> "Rương SKH Tân Thủ";
            default -> {
                Template.ItemTemplate tpl = ItemService.gI().getTemplate(itemId);
                yield tpl != null && tpl.name != null ? tpl.name : ("Item #" + itemId);
            }
        };
    }

    private static String serializeGiftCodes() {
        List<GiftCode> list = new ArrayList<>(GiftCodeManager.gI().listGiftCode);
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (GiftCode gc : list) {
            if (gc == null || gc.code == null) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            long createMs = gc.datecreate != null ? gc.datecreate.getTime() : 0L;
            long expMs = gc.dateexpired != null ? gc.dateexpired.getTime() : 0L;
            sb.append("{")
                    .append("\"id\":").append(gc.id)
                    .append(",\"code\":").append(jsonStr(gc.code))
                    .append(",\"countLeft\":").append(gc.countLeft)
                    .append(",\"dateCreateMs\":").append(createMs)
                    .append(",\"dateExpiredMs\":").append(expMs)
                    .append(",\"expired\":").append(gc.timeCode())
                    .append(",\"items\":[");
            boolean firstItem = true;
            if (gc.detail != null) {
                for (Map.Entry<Integer, Integer> entry : gc.detail.entrySet()) {
                    Integer itemId = entry.getKey();
                    Integer qty = entry.getValue();
                    if (itemId == null || qty == null) {
                        continue;
                    }
                    if (!firstItem) {
                        sb.append(",");
                    }
                    firstItem = false;
                    sb.append("{")
                            .append("\"id\":").append(itemId)
                            .append(",\"name\":").append(jsonStr(getGiftItemDisplayName(itemId)))
                            .append(",\"quantity\":").append(qty)
                            .append(",\"options\":[");
                    ArrayList<Item.ItemOption> opts = gc.option != null ? gc.option.get(itemId) : null;
                    if (opts != null) {
                        for (int k = 0; k < opts.size(); k++) {
                            Item.ItemOption opt = opts.get(k);
                            if (opt == null || opt.optionTemplate == null) {
                                continue;
                            }
                            if (k > 0) {
                                sb.append(",");
                            }
                            sb.append("{")
                                    .append("\"id\":").append(opt.optionTemplate.id)
                                    .append(",\"name\":").append(jsonStr(opt.optionTemplate.name != null ? opt.optionTemplate.name : ""))
                                    .append(",\"param\":").append(opt.param)
                                    .append("}");
                        }
                    }
                    sb.append("]}");
                }
            }
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String serializeEvents() {
        return "{\"halloween\":" + EventManager.HALLOWEEN
                + ",\"christmas\":" + EventManager.CHRISTMAS
                + ",\"lunarNewYear\":" + EventManager.LUNNAR_NEW_YEAR
                + ",\"lunnar_new_year\":" + EventManager.LUNNAR_NEW_YEAR
                + ",\"trungThu\":" + EventManager.TRUNG_THU
                + ",\"trung_thu\":" + EventManager.TRUNG_THU
                + ",\"hungVuong\":" + EventManager.HUNG_VUONG
                + ",\"hung_vuong\":" + EventManager.HUNG_VUONG
                + ",\"womensDay\":" + EventManager.INTERNATIONAL_WOMANS_DAY
                + ",\"womans_day\":" + EventManager.INTERNATIONAL_WOMANS_DAY
                + ",\"summerNuocMia\":" + EventManager.SUMMER_NUOC_MIA
                + ",\"summer_nuoc_mia\":" + EventManager.SUMMER_NUOC_MIA
                + ",\"childrenDay\":" + EventManager.CHILDREN_DAY
                + ",\"children_day\":" + EventManager.CHILDREN_DAY
                + ",\"cookingBanh\":" + EventManager.COOKING_BANH
                + ",\"cooking_banh\":" + EventManager.COOKING_BANH
                + ",\"firework\":" + EventManager.NEW_YEAR_FIREWORK
                + ",\"topUp\":" + EventManager.TOP_UP
                + ",\"top_up\":" + EventManager.TOP_UP
                + ",\"doubleGold\":" + EventManager.DOUBLE_GOLD
                + ",\"double_gold\":" + EventManager.DOUBLE_GOLD
                + ",\"nguHanhSon\":" + EventManager.NGU_HANH_SON
                + ",\"ngu_hanh_son\":" + EventManager.NGU_HANH_SON
                + ",\"dropRate\":" + EventManager.EVENT_DROP_RATE
                + ",\"drop_rate\":" + EventManager.EVENT_DROP_RATE
                + "}";
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

        String sql = "SELECT a.*, "
                + "p.id AS player_id, p.name AS player_name, p.gender, p.data_inventory, p.data_point, p.data_intrinsic, p.data_task "
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
                        int accId = rs.getInt("id");
                        String username = rs.getString("username");
                        String password = rs.getString("password");
                        boolean isAdmin = rs.getBoolean("is_admin");
                        boolean ban = rs.getBoolean("ban");
                        boolean active = rs.getBoolean("active");
                        int vnd = rs.getInt("vnd");
                        int tongnap = rs.getInt("tongnap");
                        int ruongSkh = 5;
                        try {
                            ruongSkh = rs.getInt("ruong_skh");
                        } catch (Exception ignored) {
                        }
                        long totalPlaySec = 0L;
                        try {
                            totalPlaySec = rs.getLong("total_play_sec");
                        } catch (Exception ignored) {
                        }
                        long sessionPlaySec = 0L;
                        String lastLogin = rs.getString("last_time_login");
                        long playerId = rs.getLong("player_id");
                        String playerName = rs.getString("player_name");
                        int gender = rs.getInt("gender");

                        long gold = 0, gem = 0, ruby = 0, power = 0;
                        int inId = 0, inP1 = 0, inP2 = 0;
                        String inName = "Chưa mở";
                        int taskId = 0, taskIndex = 0;
                        Player livePl = Client.gI().getPlayerByUser(accId);
                        boolean isOnline = (livePl != null);

                        if (isOnline && livePl.inventory != null && livePl.nPoint != null) {
                            gold = livePl.inventory.gold;
                            gem = livePl.inventory.gem;
                            ruby = livePl.inventory.ruby;
                            power = livePl.nPoint.power;
                            if (livePl.getSession() != null) {
                                vnd = livePl.getSession().vnd;
                                ruongSkh = livePl.getSession().ruongSkh;
                                sessionPlaySec = livePl.getSession().getCurrentSessionPlaySec();
                                totalPlaySec = Math.max(totalPlaySec, livePl.getSession().getLiveTotalPlaySec());
                            }
                            if (livePl.playerIntrinsic != null && livePl.playerIntrinsic.intrinsic != null) {
                                inId = livePl.playerIntrinsic.intrinsic.id;
                                inP1 = livePl.playerIntrinsic.intrinsic.param1;
                                inP2 = livePl.playerIntrinsic.intrinsic.param2;
                                if (inId > 0) {
                                    inName = livePl.playerIntrinsic.intrinsic.getName();
                                }
                            }
                            if (livePl.playerTask != null && livePl.playerTask.taskMain != null) {
                                taskId = livePl.playerTask.taskMain.id;
                                taskIndex = livePl.playerTask.taskMain.index;
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
                            try {
                                String inStr = rs.getString("data_intrinsic");
                                if (inStr != null && !inStr.isEmpty()) {
                                    JSONArray arr = (JSONArray) JSONValue.parse(inStr);
                                    if (arr != null && arr.size() >= 3) {
                                        inId = Integer.parseInt(String.valueOf(arr.get(0)));
                                        inP1 = Integer.parseInt(String.valueOf(arr.get(1)));
                                        inP2 = Integer.parseInt(String.valueOf(arr.get(2)));
                                        inName = formatIntrinsicInfo(inId, inP1, inP2);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                            try {
                                String taskStr = rs.getString("data_task");
                                if (taskStr != null && !taskStr.isEmpty()) {
                                    JSONArray arr = (JSONArray) JSONValue.parse(taskStr);
                                    if (arr != null && arr.size() >= 2) {
                                        taskId = Integer.parseInt(String.valueOf(arr.get(0)));
                                        taskIndex = Integer.parseInt(String.valueOf(arr.get(1)));
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        String taskName = playerId > 0 ? formatTaskInfo(taskId, taskIndex) : "Chưa có";
                        String genderName = playerName == null ? "Chưa tạo NV" : (gender == 0 ? "Trái Đất" : (gender == 1 ? "Namek" : "Xayda"));
                        double playHours = Math.round((totalPlaySec / 3600.0) * 100.0) / 100.0;
                        sb.append("{");
                        sb.append("\"accId\":").append(accId);
                        sb.append(",\"username\":").append(jsonStr(username));
                        sb.append(",\"password\":").append(jsonStr(password));
                        sb.append(",\"isAdmin\":").append(isAdmin);
                        sb.append(",\"ban\":").append(ban);
                        sb.append(",\"active\":").append(active);
                        sb.append(",\"vnd\":").append(vnd);
                        sb.append(",\"tongnap\":").append(tongnap);
                        sb.append(",\"ruongSkh\":").append(ruongSkh);
                        sb.append(",\"lastLogin\":").append(jsonStr(lastLogin != null ? lastLogin : ""));
                        sb.append(",\"playerId\":").append(playerId);
                        sb.append(",\"playerName\":").append(jsonStr(playerName != null ? playerName : "(Chưa tạo nhân vật)"));
                        sb.append(",\"gender\":").append(jsonStr(genderName));
                        sb.append(",\"genderId\":").append(gender);
                        sb.append(",\"power\":").append(power);
                        sb.append(",\"gold\":").append(gold);
                        sb.append(",\"gem\":").append(gem);
                        sb.append(",\"ruby\":").append(ruby);
                        sb.append(",\"online\":").append(isOnline);
                        sb.append(",\"intrinsicId\":").append(inId);
                        sb.append(",\"intrinsicParam1\":").append(inP1);
                        sb.append(",\"intrinsicParam2\":").append(inP2);
                        sb.append(",\"intrinsicName\":").append(jsonStr(inName));
                        sb.append(",\"taskId\":").append(taskId);
                        sb.append(",\"taskIndex\":").append(taskIndex);
                        sb.append(",\"taskName\":").append(jsonStr(taskName));
                        sb.append(",\"sessionPlaySec\":").append(sessionPlaySec);
                        sb.append(",\"totalPlaySec\":").append(totalPlaySec);
                        sb.append(",\"playHours\":").append(playHours);
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
        sb.append("],\"intrinsics\":").append(serializeIntrinsics()).append("}");
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
             PreparedStatement ps = con.prepareStatement("SELECT p.id, p.account_id, p.name, p.gender, p.data_point, p.data_inventory, a.vnd, a.tongnap, a.total_play_sec FROM player p LEFT JOIN account a ON a.id = p.account_id LIMIT 300");
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
                int accId = rs.getInt("account_id");
                long totalPlaySec = 0L;
                try {
                    totalPlaySec = rs.getLong("total_play_sec");
                } catch (Exception ignored) {
                }
                Player livePl = Client.gI().getPlayerByUser(accId);
                if (livePl != null && livePl.getSession() != null) {
                    totalPlaySec = Math.max(totalPlaySec, livePl.getSession().getLiveTotalPlaySec());
                    if (livePl.nPoint != null) {
                        power = Math.max(power, livePl.nPoint.power);
                    }
                }
                double playHours = Math.round((totalPlaySec / 3600.0) * 100.0) / 100.0;
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
                row.put("totalPlaySec", totalPlaySec);
                row.put("playHours", playHours);
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
            sb.append(",\"totalPlaySec\":").append(r.get("totalPlaySec"));
            sb.append(",\"playHours\":").append(r.get("playHours"));
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
        sb.append(",\"events\":").append(serializeEvents());
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
                    int intrinsicId = (int) parseLongSafe(params.get("intrinsicId"), 0);
                    int intrinsicParam1 = (int) parseLongSafe(params.get("intrinsicParam1"), 0);
                    int intrinsicParam2 = (int) parseLongSafe(params.get("intrinsicParam2"), 0);
                    int ruongSkh = (int) parseLongSafe(params.get("ruongSkh"), 0);

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
                        if (ruongSkh > 0 && pl.getSession() != null) {
                            try (Connection con = LocalManager.getConnection();
                                 PreparedStatement ps = con.prepareStatement("UPDATE account SET ruong_skh = GREATEST(0, ruong_skh + ?) WHERE id = ?")) {
                                ps.setInt(1, ruongSkh);
                                ps.setInt(2, pl.getSession().userId);
                                ps.executeUpdate();
                            } catch (Exception ignored) {
                            }
                            pl.getSession().ruongSkh = Math.max(0, pl.getSession().ruongSkh + ruongSkh);
                            given.append(" +").append(ruongSkh).append(" Rương SKH (Hiện có: ").append(pl.getSession().ruongSkh).append(")");
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
                        if (intrinsicId > 0) {
                            String inApplied = applyIntrinsicToPlayer(pl, intrinsicId, intrinsicParam1, intrinsicParam2);
                            if (inApplied != null) {
                                given.append(" +Nội tại [").append(inApplied).append("]");
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
                case "give_skh_box" -> {
                    String target = params.getOrDefault("playerTarget", params.getOrDefault("playerId", "")).trim();
                    int accId = (int) parseLongSafe(params.get("accId"), -1);
                    int addCount = (int) parseLongSafe(params.get("count"), 5);
                    if (addCount == 0) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Số lượng Rương SKH phải khác 0!\"}");
                        return;
                    }

                    if ("ALL".equalsIgnoreCase(target)) {
                        int count = 0;
                        try (Connection con = LocalManager.getConnection();
                             PreparedStatement ps = con.prepareStatement("UPDATE account SET ruong_skh = GREATEST(0, ruong_skh + ?) WHERE id = ?")) {
                            for (Player p : new ArrayList<>(Client.gI().getPlayers())) {
                                if (p != null && !p.isBot && p.getSession() != null) {
                                    ps.setInt(1, addCount);
                                    ps.setInt(2, p.getSession().userId);
                                    ps.addBatch();
                                    p.getSession().ruongSkh = Math.max(0, p.getSession().ruongSkh + addCount);
                                    Service.gI().sendThongBao(p, "Admin vừa tặng bạn +" + addCount + " Rương SKH! (Hiện có: " + p.getSession().ruongSkh + " rương tại Ông Gohan/Moori/Paragus)");
                                    count++;
                                }
                            }
                            if (count > 0) {
                                ps.executeBatch();
                            }
                        }
                        if (count == 0) {
                            sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Hiện không có người chơi nào đang Online!\"}");
                            return;
                        }
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã tặng +" + addCount + " Rương SKH cho toàn bộ " + count + " người chơi đang Online!") + "}");
                        return;
                    }

                    Player livePl = null;
                    if (accId > 0) {
                        livePl = Client.gI().getPlayerByUser(accId);
                    }
                    if (livePl == null && !target.isEmpty()) {
                        long pid = parseLongSafe(target, -1);
                        if (pid > 0) {
                            livePl = Client.gI().getPlayer(pid);
                        }
                        if (livePl == null) {
                            livePl = Client.gI().getPlayer(target);
                        }
                        if (livePl == null) {
                            for (Player p : new ArrayList<>(Client.gI().getPlayers())) {
                                if (p != null && p.name != null && p.name.equalsIgnoreCase(target)) {
                                    livePl = p;
                                    break;
                                }
                            }
                        }
                    }

                    if (livePl != null && livePl.getSession() != null) {
                        accId = livePl.getSession().userId;
                    }

                    int updatedRows = 0;
                    int newTotal = 0;
                    try (Connection con = LocalManager.getConnection()) {
                        if (accId <= 0 && !target.isEmpty()) {
                            long pid = parseLongSafe(target, -1);
                            String findSql = pid > 0
                                    ? "SELECT account_id FROM player WHERE id = ? LIMIT 1"
                                    : "SELECT account_id FROM player WHERE LOWER(name) = LOWER(?) LIMIT 1";
                            try (PreparedStatement psFind = con.prepareStatement(findSql)) {
                                if (pid > 0) {
                                    psFind.setLong(1, pid);
                                } else {
                                    psFind.setString(1, target);
                                }
                                try (ResultSet rs = psFind.executeQuery()) {
                                    if (rs.next()) {
                                        accId = rs.getInt("account_id");
                                    }
                                }
                            }
                        }
                        if (accId > 0) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET ruong_skh = GREATEST(0, ruong_skh + ?) WHERE id = ?")) {
                                ps.setInt(1, addCount);
                                ps.setInt(2, accId);
                                updatedRows = ps.executeUpdate();
                            }
                            try (PreparedStatement psGet = con.prepareStatement("SELECT ruong_skh FROM account WHERE id = ? LIMIT 1")) {
                                psGet.setInt(1, accId);
                                try (ResultSet rs = psGet.executeQuery()) {
                                    if (rs.next()) {
                                        newTotal = rs.getInt("ruong_skh");
                                    }
                                }
                            }
                        }
                    }

                    if (livePl != null && livePl.getSession() != null) {
                        livePl.getSession().ruongSkh = newTotal;
                        Service.gI().sendThongBao(livePl, "Admin vừa tặng bạn " + (addCount >= 0 ? "+" : "") + addCount + " Rương SKH! (Hiện có: " + newTotal + " rương tại Ông Gohan/Moori/Paragus)");
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã tặng " + (addCount >= 0 ? "+" : "") + addCount + " Rương SKH cho " + livePl.name + " (Hiện có: " + newTotal + " rương)!") + "}");
                    } else if (updatedRows > 0) {
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã cộng " + (addCount >= 0 ? "+" : "") + addCount + " Rương SKH vào Database cho [" + (target.isEmpty() ? ("TK #" + accId) : target) + "] (Hiện có: " + newTotal + " rương)!") + "}");
                    } else {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":" + jsonStr("Không tìm thấy nhân vật hoặc tài khoản [" + target + "]!") + "}");
                    }
                }
                case "set_intrinsic" -> {
                    String target = params.getOrDefault("playerTarget", params.getOrDefault("playerId", "")).trim();
                    int accId = (int) parseLongSafe(params.get("accId"), -1);
                    int intrinsicId = (int) parseLongSafe(params.get("intrinsicId"), 0);
                    int param1 = (int) parseLongSafe(params.get("param1"), 0);
                    int param2 = (int) parseLongSafe(params.get("param2"), 0);

                    if (intrinsicId <= 0) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Vui lòng chọn Nội tại cần buff!\"}");
                        return;
                    }
                    Intrinsic tpl = IntrinsicService.gI().getIntrinsicById(intrinsicId);
                    if (tpl == null) {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Không tìm thấy mẫu Nội tại #" + intrinsicId + "!\"}");
                        return;
                    }
                    short p1 = (short) (param1 > 0 ? param1 : tpl.paramTo1);
                    short p2 = (short) (param2 > 0 ? param2 : tpl.paramTo2);

                    if ("ALL".equalsIgnoreCase(target)) {
                        int count = 0;
                        String appliedName = "";
                        for (Player p : new ArrayList<>(Client.gI().getPlayers())) {
                            if (p != null && !p.isBot) {
                                String n = applyIntrinsicToPlayer(p, intrinsicId, p1, p2);
                                if (n != null) {
                                    appliedName = n;
                                    Service.gI().sendThongBao(p, "Admin vừa tặng bạn Nội tại: " + n);
                                    count++;
                                }
                            }
                        }
                        if (count == 0) {
                            sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Hiện không có người chơi nào đang Online!\"}");
                            return;
                        }
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã buff Nội tại [" + appliedName + "] cho toàn bộ " + count + " người chơi đang Online!") + "}");
                        return;
                    }

                    // 1. Thử tìm người chơi đang Online trước
                    Player livePl = null;
                    if (accId > 0) {
                        livePl = Client.gI().getPlayerByUser(accId);
                    }
                    if (livePl == null && !target.isEmpty()) {
                        long pid = parseLongSafe(target, -1);
                        if (pid > 0) {
                            livePl = Client.gI().getPlayer(pid);
                        }
                        if (livePl == null) {
                            livePl = Client.gI().getPlayer(target);
                        }
                        if (livePl == null) {
                            for (Player p : new ArrayList<>(Client.gI().getPlayers())) {
                                if (p != null && p.name != null && p.name.equalsIgnoreCase(target)) {
                                    livePl = p;
                                    break;
                                }
                            }
                        }
                    }

                    if (livePl != null) {
                        String appliedName = applyIntrinsicToPlayer(livePl, intrinsicId, p1, p2);
                        Service.gI().sendThongBao(livePl, "Admin vừa tặng bạn Nội tại: " + appliedName);
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã buff trực tiếp Nội tại [" + appliedName + "] cho " + livePl.name + " (Đang Online)!") + "}");
                        return;
                    }

                    // 2. Nếu người chơi đang Offline, cập nhật trực tiếp vào bảng player trong MySQL
                    String dbDataIntrinsic = "[" + intrinsicId + "," + p1 + "," + p2 + ",0,false,0,0,0]";
                    String formattedName = formatIntrinsicInfo(intrinsicId, p1, p2);
                    int updatedRows = 0;
                    String foundPlayerName = target;

                    try (Connection con = LocalManager.getConnection()) {
                        long pid = parseLongSafe(target, -1);
                        if (pid > 0) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE player SET data_intrinsic = ? WHERE id = ?")) {
                                ps.setString(1, dbDataIntrinsic);
                                ps.setLong(2, pid);
                                updatedRows = ps.executeUpdate();
                            }
                        }
                        if (updatedRows == 0 && accId > 0) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE player SET data_intrinsic = ? WHERE account_id = ?")) {
                                ps.setString(1, dbDataIntrinsic);
                                ps.setInt(2, accId);
                                updatedRows = ps.executeUpdate();
                            }
                        }
                        if (updatedRows == 0 && !target.isEmpty()) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE player SET data_intrinsic = ? WHERE LOWER(name) = LOWER(?)")) {
                                ps.setString(1, dbDataIntrinsic);
                                ps.setString(2, target);
                                updatedRows = ps.executeUpdate();
                            }
                        }
                    }

                    if (updatedRows > 0) {
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã lưu Nội tại [" + formattedName + "] vào Database cho nhân vật [" + (foundPlayerName.isEmpty() ? ("TK #" + accId) : foundPlayerName) + "] (Đang Offline)!") + "}");
                    } else {
                        sendJson(exchange, 404, "{\"ok\":false,\"message\":" + jsonStr("Không tìm thấy nhân vật [" + target + "] trong Server hoặc Database!") + "}");
                    }
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
                case "skip_task" -> {
                    String target = params.getOrDefault("playerTarget", params.getOrDefault("playerId", "")).trim();
                    int accId = (int) parseLongSafe(params.get("accId"), -1);
                    String mode = params.getOrDefault("mode", "next_sub").trim();
                    int reqTaskId = (int) parseLongSafe(params.get("taskId"), 0);
                    int reqTaskIndex = (int) parseLongSafe(params.get("taskIndex"), 0);

                    if ("ALL".equalsIgnoreCase(target)) {
                        int count = 0;
                        String lastTaskName = "";
                        for (Player p : new ArrayList<>(Client.gI().getPlayers())) {
                            if (p != null && !p.isBot && p.playerTask != null && p.playerTask.taskMain != null) {
                                lastTaskName = applyTaskToPlayer(p, mode, reqTaskId, reqTaskIndex);
                                count++;
                            }
                        }
                        if (count == 0) {
                            sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Hiện không có người chơi nào đang Online!\"}");
                            return;
                        }
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã nhảy nhiệm vụ cho toàn bộ " + count + " người chơi đang Online (" + lastTaskName + ")!") + "}");
                        return;
                    }

                    // 1. Thử tìm người chơi đang Online trước
                    Player livePl = null;
                    if (accId > 0) {
                        livePl = Client.gI().getPlayerByUser(accId);
                    }
                    if (livePl == null && !target.isEmpty()) {
                        long pid = parseLongSafe(target, -1);
                        if (pid > 0) {
                            livePl = Client.gI().getPlayer(pid);
                        }
                        if (livePl == null) {
                            livePl = Client.gI().getPlayer(target);
                        }
                        if (livePl == null) {
                            for (Player p : new ArrayList<>(Client.gI().getPlayers())) {
                                if (p != null && p.name != null && p.name.equalsIgnoreCase(target)) {
                                    livePl = p;
                                    break;
                                }
                            }
                        }
                    }

                    if (livePl != null && livePl.playerTask != null && livePl.playerTask.taskMain != null) {
                        String newTaskInfo = applyTaskToPlayer(livePl, mode, reqTaskId, reqTaskIndex);
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã cập nhật nhiệm vụ cho " + livePl.name + " thành: " + newTaskInfo + " (Đang Online)!") + "}");
                        return;
                    }

                    // 2. Nếu người chơi đang Offline, cập nhật trực tiếp data_task trong MySQL
                    try (Connection con = LocalManager.getConnection()) {
                        long pid = parseLongSafe(target, -1);
                        String findSql;
                        if (pid > 0) {
                            findSql = "SELECT id, name, gender, data_task FROM player WHERE id = ? LIMIT 1";
                        } else if (accId > 0) {
                            findSql = "SELECT id, name, gender, data_task FROM player WHERE account_id = ? LIMIT 1";
                        } else if (!target.isEmpty()) {
                            findSql = "SELECT id, name, gender, data_task FROM player WHERE LOWER(name) = LOWER(?) LIMIT 1";
                        } else {
                            sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Vui lòng chọn hoặc nhập Tên Nhân Vật / ID!\"}");
                            return;
                        }

                        long foundPid = -1;
                        String foundName = target;
                        int gender = 0;
                        int curTaskId = 0;
                        int curIndex = 0;

                        try (PreparedStatement ps = con.prepareStatement(findSql)) {
                            if (pid > 0) {
                                ps.setLong(1, pid);
                            } else if (accId > 0) {
                                ps.setInt(1, accId);
                            } else {
                                ps.setString(1, target);
                            }
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    foundPid = rs.getLong("id");
                                    foundName = rs.getString("name");
                                    gender = rs.getInt("gender");
                                    String tStr = rs.getString("data_task");
                                    if (tStr != null && !tStr.isEmpty()) {
                                        JSONArray arr = (JSONArray) JSONValue.parse(tStr);
                                        if (arr != null && arr.size() >= 2) {
                                            curTaskId = Integer.parseInt(String.valueOf(arr.get(0)));
                                            curIndex = Integer.parseInt(String.valueOf(arr.get(1)));
                                        }
                                    }
                                }
                            }
                        }

                        if (foundPid <= 0) {
                            sendJson(exchange, 404, "{\"ok\":false,\"message\":" + jsonStr("Không tìm thấy nhân vật [" + (target.isEmpty() ? ("TK #" + accId) : target) + "] trong Database!") + "}");
                            return;
                        }

                        int[] nextState = computeNextTaskState(curTaskId, curIndex, gender, mode, reqTaskId, reqTaskIndex);
                        String newDataTask = "[" + nextState[0] + "," + nextState[1] + ",0]";
                        try (PreparedStatement up = con.prepareStatement("UPDATE player SET data_task = ? WHERE id = ?")) {
                            up.setString(1, newDataTask);
                            up.setLong(2, foundPid);
                            up.executeUpdate();
                        }
                        String info = formatTaskInfo(nextState[0], nextState[1]);
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã lưu nhiệm vụ mới [" + info + "] vào Database cho [" + foundName + "] (Đang Offline)!") + "}");
                    }
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
                case "clear_boss_kill_history" -> {
                    synchronized (BOSS_KILL_HISTORY) {
                        BOSS_KILL_HISTORY.clear();
                    }
                    for (Boss b : new ArrayList<>(BossManager.gI().getBosses())) {
                        if (b != null) {
                            b.lastKilledTimeMs = 0L;
                            b.lastKillerName = "";
                            b.lastKilledMapName = "";
                            b.lastKilledZoneId = -1;
                            b.totalKilledCount = 0;
                        }
                    }
                    Thread.startVirtualThread(() -> {
                        try (Connection con = LocalManager.getConnection();
                             PreparedStatement ps = con.prepareStatement("TRUNCATE TABLE boss_kill_history")) {
                            ps.executeUpdate();
                        } catch (Exception ignored) {
                        }
                    });
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Đã xóa toàn bộ lịch sử tiêu diệt Boss!\"}");
                }
                case "save_giftcode" -> {
                    String rawCode = params.getOrDefault("code", "").trim();
                    if (rawCode.isEmpty()) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Vui lòng nhập tên mã Giftcode!\"}");
                        return;
                    }
                    int countLeft = (int) Math.max(1, Math.min(999_999_999L, parseLongSafe(params.get("countLeft"), 999999)));
                    long daysValid = Math.max(1L, Math.min(3650L, parseLongSafe(params.get("daysValid"), 365)));
                    boolean announce = "1".equals(params.get("announce")) || "true".equalsIgnoreCase(params.get("announce"));
                    String itemsJsonRaw = params.getOrDefault("itemsJson", "[]").trim();

                    JSONArray parsedItems = (JSONArray) JSONValue.parse(itemsJsonRaw);
                    if (parsedItems == null || parsedItems.isEmpty()) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Danh sách vật phẩm trong Giftcode không được để trống!\"}");
                        return;
                    }

                    HashMap<Integer, Integer> detailMap = new HashMap<>();
                    HashMap<Integer, ArrayList<Item.ItemOption>> optionMap = new HashMap<>();
                    StringBuilder detailDbJson = new StringBuilder("[");
                    List<String> summaryParts = new ArrayList<>();
                    boolean firstDetail = true;

                    for (Object obj : parsedItems) {
                        if (!(obj instanceof JSONObject jObj)) {
                            continue;
                        }
                        int itemId = (int) parseLongSafe(jObj.get("id") != null ? jObj.get("id").toString() : "", -999);
                        int qty = (int) Math.max(1, Math.min(999_999_999L, parseLongSafe(jObj.get("quantity") != null ? jObj.get("quantity").toString() : "1", 1)));
                        if (itemId < -4) {
                            continue;
                        }
                        if (itemId >= 0 && ItemService.gI().getTemplate(itemId) == null) {
                            sendJson(exchange, 400, "{\"ok\":false,\"message\":" + jsonStr("Không tìm thấy vật phẩm có ID #" + itemId + " trong dữ liệu Server!") + "}");
                            return;
                        }
                        ArrayList<Item.ItemOption> optList = new ArrayList<>();
                        Object optsRaw = jObj.get("options");
                        if (optsRaw instanceof JSONArray optArr) {
                            for (Object oObj : optArr) {
                                if (oObj instanceof JSONObject oJson) {
                                    int optId = (int) parseLongSafe(oJson.get("id") != null ? oJson.get("id").toString() : "-1", -1);
                                    int optParam = (int) parseLongSafe(oJson.get("param") != null ? oJson.get("param").toString() : "0", 0);
                                    if (optId >= 0) {
                                        optList.add(new Item.ItemOption(optId, optParam));
                                    }
                                }
                            }
                        }
                        if (itemId >= 0 && optList.isEmpty()) {
                            optList.add(new Item.ItemOption(30, 0));
                        }
                        detailMap.put(itemId, qty);
                        optionMap.put(itemId, optList);

                        if (!firstDetail) {
                            detailDbJson.append(",");
                        }
                        firstDetail = false;
                        detailDbJson.append("{\"id\":").append(itemId).append(",\"quantity\":").append(qty).append(",\"options\":[");
                        for (int k = 0; k < optList.size(); k++) {
                            if (k > 0) {
                                detailDbJson.append(",");
                            }
                            Item.ItemOption io = optList.get(k);
                            int oId = io.optionTemplate != null ? io.optionTemplate.id : 30;
                            detailDbJson.append("{\"id\":").append(oId).append(",\"param\":").append(io.param).append("}");
                        }
                        detailDbJson.append("]}");
                        summaryParts.add("x" + qty + " " + getGiftItemDisplayName(itemId));
                    }
                    detailDbJson.append("]");

                    if (detailMap.isEmpty()) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Không có vật phẩm hợp lệ nào trong Giftcode!\"}");
                        return;
                    }

                    long nowMs = System.currentTimeMillis();
                    long maxTimestampMs = 2145888000_000L; // 2037-12-31 safely within MySQL 32-bit TIMESTAMP
                    long expireMs = Math.min(nowMs + daysValid * 86400_000L, maxTimestampMs);
                    if (expireMs <= nowMs) {
                        expireMs = maxTimestampMs;
                    }
                    Timestamp tsCreate = new Timestamp(Math.min(nowMs, maxTimestampMs - 86400_000L));
                    Timestamp tsExpire = new Timestamp(expireMs);
                    int dbId = -1;

                    try (Connection con = LocalManager.getConnection()) {
                        GiftCode existing = null;
                        for (GiftCode gc : GiftCodeManager.gI().listGiftCode) {
                            if (gc != null && gc.code != null && gc.code.equalsIgnoreCase(rawCode)) {
                                existing = gc;
                                break;
                            }
                        }
                        if (existing != null) {
                            dbId = existing.id;
                            try (PreparedStatement ps = con.prepareStatement(
                                    "UPDATE giftcode SET code = ?, count_left = ?, detail = ?, datecreate = ?, expired = ? WHERE id = ?")) {
                                ps.setString(1, rawCode);
                                ps.setInt(2, countLeft);
                                ps.setString(3, detailDbJson.toString());
                                ps.setTimestamp(4, tsCreate);
                                ps.setTimestamp(5, tsExpire);
                                ps.setInt(6, dbId);
                                ps.executeUpdate();
                            }
                            existing.code = rawCode;
                            existing.countLeft = countLeft;
                            existing.detail = detailMap;
                            existing.option = optionMap;
                            existing.datecreate = tsCreate;
                            existing.dateexpired = tsExpire;
                        } else {
                            dbId = 1;
                            try (PreparedStatement psMax = con.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM giftcode");
                                 ResultSet rsMax = psMax.executeQuery()) {
                                if (rsMax.next()) {
                                    dbId = rsMax.getInt(1);
                                }
                            }
                            try (PreparedStatement ps = con.prepareStatement(
                                    "INSERT INTO giftcode (id, code, count_left, detail, datecreate, expired) VALUES (?, ?, ?, ?, ?, ?)")) {
                                ps.setInt(1, dbId);
                                ps.setString(2, rawCode);
                                ps.setInt(3, countLeft);
                                ps.setString(4, detailDbJson.toString());
                                ps.setTimestamp(5, tsCreate);
                                ps.setTimestamp(6, tsExpire);
                                ps.executeUpdate();
                            }
                            GiftCode newGc = new GiftCode();
                            newGc.id = dbId;
                            newGc.code = rawCode;
                            newGc.countLeft = countLeft;
                            newGc.detail = detailMap;
                            newGc.option = optionMap;
                            newGc.datecreate = tsCreate;
                            newGc.dateexpired = tsExpire;
                            GiftCodeManager.gI().listGiftCode.add(0, newGc);
                        }
                    } catch (Exception e) {
                        sendJson(exchange, 500, "{\"ok\":false,\"message\":" + jsonStr("Lỗi lưu Giftcode vào MySQL: " + e.getMessage()) + "}");
                        return;
                    }

                    String giftSummary = String.join(", ", summaryParts);
                    if (announce) {
                        Service.gI().sendThongBaoAllPlayer("[MÃ QUÀ TẶNG MỚI] Nhập ngay Giftcode [" + rawCode + "] tại Ông Gôhan/Moori/Paragus hoặc Santa để nhận: " + giftSummary + "!");
                    }
                    ServerNotify.gI().broadcastNotifyTab();
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã lưu & kích hoạt Giftcode [" + rawCode + "] (" + giftSummary + ") thành công!") + "}");
                }
                case "delete_giftcode" -> {
                    String targetCode = params.getOrDefault("code", "").trim();
                    int targetId = (int) parseLongSafe(params.get("id"), -1);
                    GiftCode removed = null;
                    for (int i = GiftCodeManager.gI().listGiftCode.size() - 1; i >= 0; i--) {
                        GiftCode gc = GiftCodeManager.gI().listGiftCode.get(i);
                        if (gc != null && ((targetId > 0 && gc.id == targetId) || (!targetCode.isEmpty() && gc.code != null && gc.code.equalsIgnoreCase(targetCode)))) {
                            removed = GiftCodeManager.gI().listGiftCode.remove(i);
                            break;
                        }
                    }
                    try (Connection con = LocalManager.getConnection()) {
                        if (targetId > 0) {
                            try (PreparedStatement ps = con.prepareStatement("DELETE FROM giftcode WHERE id = ?")) {
                                ps.setInt(1, targetId);
                                ps.executeUpdate();
                            }
                        } else if (!targetCode.isEmpty()) {
                            try (PreparedStatement ps = con.prepareStatement("DELETE FROM giftcode WHERE code = ?")) {
                                ps.setString(1, targetCode);
                                ps.executeUpdate();
                            }
                        }
                    } catch (Exception e) {
                        sendJson(exchange, 500, "{\"ok\":false,\"message\":" + jsonStr("Lỗi xóa Giftcode khỏi MySQL: " + e.getMessage()) + "}");
                        return;
                    }
                    ServerNotify.gI().broadcastNotifyTab();
                    String codeLabel = removed != null ? removed.code : targetCode;
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã xóa mã Giftcode [" + codeLabel + "] thành công!") + "}");
                }
                case "reset_giftcode_usage" -> {
                    String targetCode = params.getOrDefault("code", "").trim();
                    if (targetCode.isEmpty()) {
                        sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Thiếu mã Giftcode cần làm mới lượt nhận!\"}");
                        return;
                    }
                    for (Player pl : new ArrayList<>(Client.gI().getPlayers())) {
                        if (pl != null && pl.giftCode != null && pl.giftCode.rewards != null) {
                            pl.giftCode.rewards.removeIf(c -> c != null && c.equalsIgnoreCase(targetCode));
                        }
                    }
                    try (Connection con = LocalManager.getConnection();
                         PreparedStatement ps = con.prepareStatement("UPDATE player SET giftcode = REPLACE(giftcode, ?, '') WHERE giftcode LIKE ?")) {
                        ps.setString(1, "\"" + targetCode + "\"");
                        ps.setString(2, "%" + targetCode + "%");
                        ps.executeUpdate();
                    } catch (Exception ignored) {
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã làm mới lượt nhận mã [" + targetCode + "] — toàn bộ người chơi có thể nhập lại mã này!") + "}");
                }
                case "set_exp_rate" -> {
                    int rate = (int) Math.max(1, Math.min(127, parseLongSafe(params.get("rate"), 1)));
                    Manager.RATE_EXP_SERVER = (byte) rate;
                    EventManager.saveRuntimeConfig();
                    Service.gI().sendThongBaoAllPlayer("[THÔNG BÁO] Admin vừa điều chỉnh tỷ lệ Sức Mạnh / Tiềm Năng toàn Server lên x" + rate + "!");
                    ServerNotify.gI().broadcastNotifyTab();
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã cập nhật Tỷ lệ EXP toàn Server thành x" + rate + " (đã lưu cố định vào MySQL)!") + "}");
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
                    int addRuongSkh = (int) parseLongSafe(params.get("addRuongSkh"), 0);
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
                        if (addRuongSkh != 0) {
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET ruong_skh = GREATEST(0, ruong_skh + ?) WHERE id = ?")) {
                                ps.setInt(1, addRuongSkh);
                                ps.setInt(2, accId);
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
                        if (addRuongSkh != 0) {
                            livePl.getSession().ruongSkh = Math.max(0, livePl.getSession().ruongSkh + addRuongSkh);
                            Service.gI().sendThongBao(livePl, "Admin vừa cộng " + (addRuongSkh > 0 ? "+" : "") + addRuongSkh + " Rương SKH vào tài khoản của bạn! (Hiện có: " + livePl.getSession().ruongSkh + " rương)");
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
                        try (PreparedStatement psMode = con.prepareStatement("SET SESSION sql_mode = ''")) {
                            psMode.executeUpdate();
                        } catch (Exception ignored) {
                        }
                        try (PreparedStatement check = con.prepareStatement("SELECT id FROM account WHERE username = ? LIMIT 1")) {
                            check.setString(1, u);
                            try (ResultSet rs = check.executeQuery()) {
                                if (rs.next()) {
                                    sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Tên tài khoản này đã tồn tại!\"}");
                                    return;
                                }
                            }
                        }
                        try (PreparedStatement ins = con.prepareStatement(
                                "INSERT INTO account (username, password, active, vnd, tongnap, ruong_skh) VALUES (?, ?, ?, ?, ?, 5)")) {
                            ins.setString(1, u);
                            ins.setString(2, p);
                            ins.setInt(3, active == 1 ? 1 : 0);
                            ins.setInt(4, vnd);
                            ins.setInt(5, vnd);
                            ins.executeUpdate();
                        } catch (Exception exFallback) {
                            try (PreparedStatement ins2 = con.prepareStatement(
                                    "INSERT INTO account (username, password, active, vnd, tongnap) VALUES (?, ?, ?, ?, ?)")) {
                                ins2.setString(1, u);
                                ins2.setString(2, p);
                                ins2.setInt(3, active == 1 ? 1 : 0);
                                ins2.setInt(4, vnd);
                                ins2.setInt(5, vnd);
                                ins2.executeUpdate();
                            }
                        }
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã tạo tài khoản mới [" + u + "] thành công (Mặc định +5 Rương SKH)!") + "}");
                }
                case "account_delete" -> {
                    int accId = (int) parseLongSafe(params.get("accId"), -1);
                    long playerId = parseLongSafe(params.get("playerId"), -1);

                    if (accId <= 0 && playerId > 0) {
                        Player onlineByPid = Client.gI().getPlayer(playerId);
                        if (onlineByPid != null && onlineByPid.getSession() != null) {
                            accId = onlineByPid.getSession().userId;
                        }
                    }

                    String deletedUsername = "";
                    String deletedPlayerName = "";

                    try (Connection con = LocalManager.getConnection()) {
                        if (accId <= 0 && playerId > 0) {
                            try (PreparedStatement psFind = con.prepareStatement("SELECT account_id FROM player WHERE id = ? LIMIT 1")) {
                                psFind.setLong(1, playerId);
                                try (ResultSet rs = psFind.executeQuery()) {
                                    if (rs.next()) {
                                        accId = rs.getInt("account_id");
                                    }
                                }
                            }
                        }

                        if (accId <= 0) {
                            sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Không tìm thấy ID tài khoản cần xóa!\"}");
                            return;
                        }

                        try (PreparedStatement psInfo = con.prepareStatement(
                                "SELECT a.username, p.id AS pid, p.name AS pname FROM account a LEFT JOIN player p ON p.account_id = a.id WHERE a.id = ? LIMIT 1")) {
                            psInfo.setInt(1, accId);
                            try (ResultSet rs = psInfo.executeQuery()) {
                                if (rs.next()) {
                                    deletedUsername = rs.getString("username");
                                    deletedPlayerName = rs.getString("pname");
                                    if (playerId <= 0) {
                                        playerId = rs.getLong("pid");
                                    }
                                } else {
                                    sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Tài khoản #" + accId + " không tồn tại trong Database!\"}");
                                    return;
                                }
                            }
                        }
                    }

                    // Nếu người chơi đang Online, kick khỏi Server trước khi xóa dữ liệu
                    Player livePl = Client.gI().getPlayerByUser(accId);
                    if (livePl == null && playerId > 0) {
                        livePl = Client.gI().getPlayer(playerId);
                    }
                    if (livePl != null) {
                        if (deletedPlayerName == null || deletedPlayerName.isEmpty()) {
                            deletedPlayerName = livePl.name;
                        }
                        if (livePl.getSession() != null) {
                            Client.gI().kickSession(livePl.getSession());
                        }
                    }

                    // Xóa nhân vật và tài khoản trong MySQL
                    try (Connection con = LocalManager.getConnection()) {
                        try (PreparedStatement delPl = con.prepareStatement("DELETE FROM player WHERE account_id = ?")) {
                            delPl.setInt(1, accId);
                            delPl.executeUpdate();
                        }
                        if (playerId > 0) {
                            try (PreparedStatement delPlById = con.prepareStatement("DELETE FROM player WHERE id = ?")) {
                                delPlById.setLong(1, playerId);
                                delPlById.executeUpdate();
                            }
                        }
                        try (PreparedStatement delAcc = con.prepareStatement("DELETE FROM account WHERE id = ?")) {
                            delAcc.setInt(1, accId);
                            delAcc.executeUpdate();
                        }
                    }

                    String label = (deletedUsername != null && !deletedUsername.isEmpty() ? deletedUsername : ("#" + accId))
                            + (deletedPlayerName != null && !deletedPlayerName.isEmpty() ? (" - NV: " + deletedPlayerName) : "");
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã xóa vĩnh viễn tài khoản [" + label + "] khỏi Server!") + "}");
                }
                case "event_toggle" -> {
                    String evKey = params.getOrDefault("event", "").trim();
                    String stateRaw = params.containsKey("state") ? params.get("state") : params.get("enabled");
                    boolean state = "1".equals(stateRaw) || "true".equalsIgnoreCase(stateRaw);
                    String evLabel;
                    switch (evKey) {
                        case "halloween" -> {
                            EventManager.HALLOWEEN = state;
                            evLabel = "Sự kiện Halloween (Rơi Bí Ngô 1-7 Sao & Boss Bí Ma/Ma Trơi/Dơi)";
                            if (state) {
                                if (BossManager.gI().getBossById(BossID.BIMA) == null) {
                                    BossManager.gI().createBoss(BossID.BIMA, 5);
                                    BossManager.gI().createBoss(BossID.MATROI, 5);
                                    BossManager.gI().createBoss(BossID.DOI, 5);
                                }
                            } else {
                                removeEventBosses(HalloweenEventManager.gI(), BossID.BIMA, BossID.MATROI, BossID.DOI);
                            }
                        }
                        case "christmas" -> {
                            EventManager.CHRISTMAS = state;
                            evLabel = "Sự kiện Giáng Sinh Noel (Rơi Tất Vớ/Hộp Quà & Boss Ông Già Noel)";
                            if (state) {
                                if (BossManager.gI().getBossById(BossID.ONG_GIA_NOEL) == null) {
                                    BossManager.gI().createBoss(BossID.ONG_GIA_NOEL, 10);
                                }
                            } else {
                                removeEventBosses(ChristmasEventManager.gI(), BossID.ONG_GIA_NOEL);
                            }
                        }
                        case "lunarNewYear", "lunnar_new_year", "tet" -> {
                            EventManager.LUNNAR_NEW_YEAR = state;
                            evLabel = "Sự kiện Tết Nguyên Đán (Rơi Bao Lì Xì & Boss Lân Con)";
                            if (state) {
                                if (BossManager.gI().getBossById(BossID.LAN_CON) == null) {
                                    BossManager.gI().createBoss(BossID.LAN_CON, 10);
                                }
                            } else {
                                removeEventBosses(LunarNewYearEventManager.gI(), BossID.LAN_CON);
                            }
                        }
                        case "trungThu", "trung_thu" -> {
                            EventManager.TRUNG_THU = state;
                            evLabel = "Sự kiện Trung Thu (Rơi Hộp Quà Trung Thu & Boss Khỉ Đột/Nguyệt Thần)";
                            if (state) {
                                if (BossManager.gI().getBossById(BossID.KHIDOT) == null) {
                                    BossManager.gI().createBoss(BossID.KHIDOT, 5);
                                    BossManager.gI().createBoss(BossID.NGUYETTHAN, 5);
                                }
                            } else {
                                removeEventBosses(TrungThuEventManager.gI(), BossID.KHIDOT, BossID.NGUYETTHAN, BossID.NHATTHAN);
                            }
                        }
                        case "hungVuong", "hung_vuong" -> {
                            EventManager.HUNG_VUONG = state;
                            evLabel = "Sự kiện Giỗ Tổ Hùng Vương (Rơi Dưa Hấu & Boss Thủy Tinh/Sơn Tinh)";
                            if (state) {
                                if (BossManager.gI().getBossById(BossID.THUY_TINH) == null) {
                                    BossManager.gI().createBoss(BossID.THUY_TINH, 5);
                                    BossManager.gI().createBoss(BossID.SON_TINH, 5);
                                }
                            } else {
                                removeEventBosses(HungVuongEventManager.gI(), BossID.THUY_TINH, BossID.SON_TINH);
                            }
                        }
                        case "womensDay", "womans_day" -> {
                            EventManager.INTERNATIONAL_WOMANS_DAY = state;
                            evLabel = "Sự kiện Quốc Tế Phụ Nữ (Rơi Bông Hoa)";
                        }
                        case "summerNuocMia", "summer_nuoc_mia" -> {
                            EventManager.SUMMER_NUOC_MIA = state;
                            evLabel = "Sự kiện Mùa Hè Nước Mía & Kem Trái Cây (Rơi Khúc Mía, Nước Đá, Kem Trái Cây & Đua Top tại ChiChi)";
                        }
                        case "childrenDay", "children_day" -> {
                            EventManager.CHILDREN_DAY = state;
                            evLabel = "Sự kiện Thiếu Nhi 1/6 (Rơi Hộp Quà Thiếu Nhi & Đua Top tại ChiChi)";
                        }
                        case "cookingBanh", "cooking_banh" -> {
                            EventManager.COOKING_BANH = state;
                            evLabel = "Sự kiện Nấu Bánh Chưng & Bánh Dầy (Rơi Cơm Nếp, Đậu Xanh, Thịt Tươi, Chả Lụa tại Nồi Bánh)";
                        }
                        case "firework" -> {
                            EventManager.NEW_YEAR_FIREWORK = state;
                            evLabel = "Sự kiện Pháo Hoa Lễ Hội (Rơi Pháo Bông & Pháo Bông VIP)";
                        }
                        case "topUp", "top_up" -> {
                            EventManager.TOP_UP = state;
                            evLabel = "Sự kiện Khuyến Mãi Nạp (Top Up)";
                        }
                        case "doubleGold", "double_gold" -> {
                            EventManager.DOUBLE_GOLD = state;
                            evLabel = "Sự kiện X2 Vàng Rơi Từ Quái Toàn Server";
                        }
                        case "nguHanhSon", "ngu_hanh_son" -> {
                            EventManager.NGU_HANH_SON = state;
                            evLabel = "Sự kiện Ngũ Hành Sơn (x5 TNSM & Rơi Quả Hồng Đào / Chữ Giải Phong Ấn)";
                            if (!state) {
                                for (Player pl : new ArrayList<>(Client.gI().getPlayers())) {
                                    if (pl != null && !pl.isAdmin() && pl.zone != null && MapService.gI().isMapNguHanhSon(pl.zone.map.mapId)) {
                                        Service.gI().sendThongBao(pl, "Sự kiện Ngũ Hành Sơn đã đóng, bạn được đưa về nhà!");
                                        ChangeMapService.gI().changeMapBySpaceShip(pl, 21 + pl.gender, -1, -1);
                                    }
                                }
                            }
                        }
                        default -> {
                            sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Sự kiện không hợp lệ!\"}");
                            return;
                        }
                    }
                    EventManager.saveRuntimeConfig();
                    String statusStr = state ? "BẬT" : "TẮT";
                    Service.gI().sendThongBaoAllPlayer("[SỰ KIỆN] Admin vừa " + statusStr + " " + evLabel + "!");
                    ServerNotify.gI().broadcastNotifyTab();
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã " + statusStr + " " + evLabel + " (đã lưu cố định vào MySQL)!") + ",\"events\":" + serializeEvents() + "}");
                }
                case "event_drop_rate" -> {
                    int rate = (int) Math.max(1, Math.min(100, parseLongSafe(params.get("rate"), 5)));
                    EventManager.EVENT_DROP_RATE = rate;
                    EventManager.saveRuntimeConfig();
                    ServerNotify.gI().broadcastNotifyTab();
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã cập nhật tỷ lệ rơi Vật phẩm Sự kiện từ quái thành " + rate + "% (đã lưu vào MySQL)!") + ",\"events\":" + serializeEvents() + "}");
                }
                case "event_spawn_boss" -> {
                    String type = params.getOrDefault("type", params.getOrDefault("event", "")).trim();
                    int count = (int) Math.max(1, Math.min(20, parseLongSafe(params.get("count"), 5)));
                    String bossName;
                    switch (type) {
                        case "halloween" -> {
                            BossManager.gI().createBoss(BossID.BIMA, count);
                            BossManager.gI().createBoss(BossID.MATROI, count);
                            BossManager.gI().createBoss(BossID.DOI, count);
                            bossName = "Bí Ma, Ma Trơi & Dơi (x" + count + " mỗi loại)";
                        }
                        case "christmas" -> {
                            BossManager.gI().createBoss(BossID.ONG_GIA_NOEL, count);
                            bossName = "Ông Già Noel (x" + count + ")";
                        }
                        case "lunarNewYear", "lunnar_new_year", "tet" -> {
                            BossManager.gI().createBoss(BossID.LAN_CON, count);
                            bossName = "Lân Con (x" + count + ")";
                        }
                        case "trungThu", "trung_thu" -> {
                            BossManager.gI().createBoss(BossID.KHIDOT, count);
                            BossManager.gI().createBoss(BossID.NGUYETTHAN, count);
                            bossName = "Khỉ Đột & Nguyệt Thần (x" + count + " mỗi loại)";
                        }
                        case "hungVuong", "hung_vuong" -> {
                            BossManager.gI().createBoss(BossID.THUY_TINH, count);
                            BossManager.gI().createBoss(BossID.SON_TINH, count);
                            bossName = "Thủy Tinh & Sơn Tinh (x" + count + " mỗi loại)";
                        }
                        default -> {
                            sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Loại Boss sự kiện không hợp lệ!\"}");
                            return;
                        }
                    }
                    Service.gI().sendThongBaoAllPlayer("[SỰ KIỆN] Admin vừa triệu hồi Boss Sự Kiện: " + bossName + "!");
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã triệu hồi thành công Boss Sự Kiện: " + bossName + "!") + "}");
                }
                case "event_give_point" -> {
                    int pts = (int) parseLongSafe(params.get("points"), 100);
                    String target = params.getOrDefault("target", params.getOrDefault("playerTarget", "all")).trim();
                    if ("all".equalsIgnoreCase(target)) {
                        int count = 0;
                        for (Player pl : new ArrayList<>(Client.gI().getPlayers())) {
                            if (pl != null && !pl.isBot && pl.event != null) {
                                pl.event.addEventPoint(pts);
                                pl.point_sukien += pts;
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được +" + pts + " Điểm Sự Kiện từ Admin! (Hiện có: " + pl.event.getEventPoint() + " điểm)");
                                count++;
                            }
                        }
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã tặng +" + pts + " Điểm Sự Kiện cho toàn bộ " + count + " người chơi đang Online!") + "}");
                    } else {
                        Player pl = Client.gI().getPlayer(target);
                        if (pl == null) {
                            try {
                                pl = Client.gI().getPlayer(Long.parseLong(target));
                            } catch (Exception ignored) {
                            }
                        }
                        if (pl == null || pl.event == null) {
                            sendJson(exchange, 404, "{\"ok\":false,\"message\":\"Không tìm thấy người chơi đang Online với tên/ID: " + target + "\"}");
                            return;
                        }
                        pl.event.addEventPoint(pts);
                        pl.point_sukien += pts;
                        Service.gI().sendThongBao(pl, "Bạn vừa nhận được +" + pts + " Điểm Sự Kiện từ Admin! (Hiện có: " + pl.event.getEventPoint() + " điểm)");
                        sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã cộng +" + pts + " Điểm Sự Kiện cho [" + pl.name + "] (Tổng: " + pl.event.getEventPoint() + " điểm)!") + "}");
                    }
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
                    EventManager.saveRuntimeConfig();
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":" + jsonStr("Đã lưu thành công dữ liệu của " + savedCount + " người chơi, Bang hội, Ký gửi & Cấu hình Sự Kiện vào MySQL!") + "}");
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
                    EventManager.saveRuntimeConfig();
                    Service.gI().sendThongBaoAllPlayer("[THÔNG BÁO] Server đang cập nhật phiên bản mới và khởi động lại, vui lòng đăng nhập lại sau 10 giây!");
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Đang tự động kéo code mới từ GitHub (git pull) và khởi động lại Server! Vui lòng đợi khoảng 8-12 giây để Server bật lại...\"}");

                    new Thread(() -> {
                        try {
                            Thread.sleep(800);
                            new ProcessBuilder("/bin/bash", "-c",
                                    "nohup /bin/bash -c 'apt-get clean >/dev/null 2>&1; truncate -s 0 /var/log/btmp /var/log/wtmp /var/log/syslog /var/log/auth.log /var/log/kern.log /var/log/mysql/*.log 2>/dev/null; rm -rf /var/cache/apt/archives/*.deb /var/lib/apt/lists/* /root/.cache/* /var/crash/* /root/NRO/backups/.git 2>/dev/null; journalctl --vacuum-size=10M >/dev/null 2>&1; mysql -e \"PURGE BINARY LOGS BEFORE NOW(); SET GLOBAL binlog_expire_logs_seconds = 600;\" >/dev/null 2>&1; cd /root/NRO/SRC && git fetch --depth=1 origin main && git reset --hard origin/main && git reflog expire --expire=now --all >/dev/null 2>&1 && git gc --prune=now >/dev/null 2>&1 && sed -i \"s/\\r$//\" run.sh backup_db.sh && chmod +x run.sh backup_db.sh && ./run.sh' > /root/NRO/SRC/update.log 2>&1 < /dev/null &")
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

    private static String applyIntrinsicToPlayer(Player pl, int intrinsicId, int param1, int param2) {
        if (pl == null || intrinsicId <= 0) {
            return null;
        }
        Intrinsic tpl = IntrinsicService.gI().getIntrinsicById(intrinsicId);
        if (tpl == null) {
            return null;
        }
        short p1 = (short) (param1 > 0 ? Math.min(Short.MAX_VALUE, param1) : tpl.paramTo1);
        short p2 = (short) (param2 > 0 ? Math.min(Short.MAX_VALUE, param2) : tpl.paramTo2);
        if (pl.playerIntrinsic != null) {
            pl.playerIntrinsic.intrinsic = new Intrinsic(tpl);
            pl.playerIntrinsic.intrinsic.param1 = p1;
            pl.playerIntrinsic.intrinsic.param2 = p2;
            pl.playerIntrinsic.countOpen = 0;
            IntrinsicService.gI().sendInfoIntrinsic(pl);
        }
        if (pl.nPoint != null) {
            pl.nPoint.calPoint();
            Service.gI().point(pl);
        }
        PlayerDAO.updatePlayer(pl);
        return (pl.playerIntrinsic != null && pl.playerIntrinsic.intrinsic != null)
                ? pl.playerIntrinsic.intrinsic.getName()
                : tpl.name;
    }

    private static TaskMain findRawTaskTemplate(int taskId) {
        if (Manager.TASKS == null) {
            return null;
        }
        for (TaskMain tm : Manager.TASKS) {
            if (tm != null && tm.id == taskId) {
                return tm;
            }
        }
        return null;
    }

    private static int getMaxTaskTemplateId() {
        int maxId = 31;
        if (Manager.TASKS != null) {
            for (TaskMain tm : Manager.TASKS) {
                if (tm != null && tm.id > maxId) {
                    maxId = tm.id;
                }
            }
        }
        return maxId;
    }

    private static int[] computeNextTaskState(int curTaskId, int curIndex, int gender, String mode, int reqTaskId, int reqTaskIndex) {
        int maxTaskId = getMaxTaskTemplateId();
        int newTaskId = curTaskId;
        int newIndex = curIndex;

        if ("set_task".equalsIgnoreCase(mode)) {
            newTaskId = Math.max(0, Math.min(maxTaskId, reqTaskId));
            if (newTaskId >= 4 && newTaskId <= 6) {
                newTaskId = Math.max(0, Math.min(2, gender)) + 4;
            }
            TaskMain tpl = findRawTaskTemplate(newTaskId);
            int maxSub = (tpl != null && tpl.subTasks != null && !tpl.subTasks.isEmpty()) ? tpl.subTasks.size() - 1 : 0;
            newIndex = Math.max(0, Math.min(maxSub, reqTaskIndex));
        } else if ("next_main".equalsIgnoreCase(mode)) {
            if (curTaskId == 3) {
                newTaskId = Math.max(0, Math.min(2, gender)) + 4;
            } else if (curTaskId >= 4 && curTaskId <= 6) {
                newTaskId = 7;
            } else {
                newTaskId = Math.min(maxTaskId, curTaskId + 1);
            }
            newIndex = 0;
        } else {
            // next_sub
            TaskMain tpl = findRawTaskTemplate(curTaskId);
            int subSize = (tpl != null && tpl.subTasks != null) ? tpl.subTasks.size() : 1;
            if (curIndex + 1 < subSize) {
                newIndex = curIndex + 1;
            } else {
                if (curTaskId == 3) {
                    newTaskId = Math.max(0, Math.min(2, gender)) + 4;
                } else if (curTaskId >= 4 && curTaskId <= 6) {
                    newTaskId = 7;
                } else {
                    newTaskId = Math.min(maxTaskId, curTaskId + 1);
                }
                newIndex = 0;
            }
        }
        return new int[]{newTaskId, newIndex};
    }

    private static String applyTaskToPlayer(Player pl, String mode, int reqTaskId, int reqTaskIndex) {
        if (pl == null || pl.playerTask == null || pl.playerTask.taskMain == null) {
            return "Không rõ";
        }
        int curTaskId = pl.playerTask.taskMain.id;
        int curIndex = pl.playerTask.taskMain.index;
        int[] next = computeNextTaskState(curTaskId, curIndex, pl.gender, mode, reqTaskId, reqTaskIndex);
        TaskMain newTm = TaskService.gI().getTaskMainById(pl, next[0]);
        if (newTm != null) {
            int maxSub = (newTm.subTasks != null && !newTm.subTasks.isEmpty()) ? newTm.subTasks.size() - 1 : 0;
            newTm.index = Math.max(0, Math.min(maxSub, next[1]));
            if (newTm.subTasks != null && !newTm.subTasks.isEmpty()) {
                newTm.subTasks.get(newTm.index).count = 0;
            }
            pl.playerTask.taskMain = newTm;
            TaskService.gI().sendTaskMain(pl);
            String stepName = (newTm.subTasks != null && newTm.index < newTm.subTasks.size())
                    ? newTm.subTasks.get(newTm.index).name
                    : newTm.name;
            Service.gI().sendThongBao(pl, "Admin vừa chuyển nhiệm vụ của bạn sang: NV #" + newTm.id + " - " + newTm.name + " (Bước " + (newTm.index + 1) + ": " + stepName + ")");
        }
        PlayerDAO.updatePlayer(pl);
        return formatTaskInfo(pl.playerTask.taskMain.id, pl.playerTask.taskMain.index);
    }

    private static String formatTaskInfo(int taskId, int taskIndex) {
        TaskMain tpl = findRawTaskTemplate(taskId);
        if (tpl == null) {
            return "NV #" + taskId + " (Bước " + (taskIndex + 1) + ")";
        }
        int totalSub = (tpl.subTasks != null && !tpl.subTasks.isEmpty()) ? tpl.subTasks.size() : 1;
        String cleanName = tpl.name != null ? tpl.name : ("Nhiệm vụ #" + taskId);
        return "NV #" + taskId + ": " + cleanName + " (" + (taskIndex + 1) + "/" + totalSub + ")";
    }

    private static String serializeTasks() {
        if (cachedTasksJson != null) {
            return cachedTasksJson;
        }
        if (Manager.TASKS == null || Manager.TASKS.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(4096);
        sb.append("[");
        boolean first = true;
        for (TaskMain tm : Manager.TASKS) {
            if (tm == null) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            int subCount = tm.subTasks != null ? tm.subTasks.size() : 0;
            sb.append("{");
            sb.append("\"id\":").append(tm.id);
            sb.append(",\"name\":").append(jsonStr(tm.name != null ? tm.name : ("Nhiệm vụ #" + tm.id)));
            sb.append(",\"subCount\":").append(subCount);
            sb.append(",\"subTasks\":[");
            if (tm.subTasks != null) {
                for (int i = 0; i < tm.subTasks.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    SubTaskMain stm = tm.subTasks.get(i);
                    sb.append(jsonStr(stm != null && stm.name != null ? stm.name : ("Bước " + (i + 1))));
                }
            }
            sb.append("]");
            sb.append("}");
        }
        sb.append("]");
        cachedTasksJson = sb.toString();
        return cachedTasksJson;
    }

    private static String formatIntrinsicInfo(int id, int p1, int p2) {
        if (id <= 0) {
            return "Chưa mở";
        }
        Intrinsic tpl = IntrinsicService.gI().getIntrinsicById(id);
        if (tpl == null) {
            return "Nội tại #" + id + " (+" + p1 + "%)";
        }
        tpl.param1 = (short) p1;
        tpl.param2 = (short) p2;
        return tpl.getName();
    }

    private static String serializeIntrinsics() {
        if (cachedIntrinsicsJson != null) {
            return cachedIntrinsicsJson;
        }
        if (Manager.INTRINSICS == null || Manager.INTRINSICS.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(2048);
        sb.append("[");
        boolean first = true;
        for (Intrinsic in : Manager.INTRINSICS) {
            if (in == null || in.id <= 0) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            String gName = in.gender == 0 ? "Trái Đất" : (in.gender == 1 ? "Namek" : (in.gender == 2 ? "Xayda" : "Tất cả"));
            sb.append("{");
            sb.append("\"id\":").append(in.id);
            sb.append(",\"name\":").append(jsonStr(in.name));
            sb.append(",\"desc\":").append(jsonStr(in.getDescription()));
            sb.append(",\"paramFrom1\":").append(in.paramFrom1);
            sb.append(",\"paramTo1\":").append(in.paramTo1);
            sb.append(",\"paramFrom2\":").append(in.paramFrom2);
            sb.append(",\"paramTo2\":").append(in.paramTo2);
            sb.append(",\"genderId\":").append(in.gender);
            sb.append(",\"gender\":").append(jsonStr(gName));
            sb.append("}");
        }
        sb.append("]");
        cachedIntrinsicsJson = sb.toString();
        return cachedIntrinsicsJson;
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

    private static void removeEventBosses(BossManager specificMgr, int... bossIds) {
        if (specificMgr != null) {
            for (Boss b : new ArrayList<>(specificMgr.getBosses())) {
                if (b != null) {
                    try {
                        ChangeMapService.gI().exitMap(b);
                    } catch (Exception ignored) {
                    }
                    specificMgr.removeBoss(b);
                }
            }
        }
        for (Boss b : new ArrayList<>(BossManager.gI().getBosses())) {
            if (b != null) {
                for (int id : bossIds) {
                    if (b.id == id) {
                        try {
                            ChangeMapService.gI().exitMap(b);
                        } catch (Exception ignored) {
                        }
                        BossManager.gI().removeBoss(b);
                        break;
                    }
                }
            }
        }
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
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
        String clean = s.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
        StringBuilder sb = new StringBuilder(clean.length() + 16);
        sb.append('"');
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> {}
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c >= 0x20) {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
