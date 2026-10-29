package nro.models.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import nro.models.database.HistoryTransactionDAO;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.boss.Boss_Manager.TreasureUnderSeaManager;
import nro.models.boss.Boss_Manager.SnakeWayManager;
import nro.models.boss.Boss_Manager.RedRibbonHQManager;
import nro.models.boss.Boss_Manager.GasDestroyManager;
import nro.models.boss.Boss_Manager.YardartManager;
import nro.models.boss.Boss_Manager.SkillSummonedManager;
import nro.models.interfaces.ISession;
import nro.models.network.Network;
import nro.models.network.MyKeyHandler;
import nro.models.network.MySession;
import nro.models.network.SessionManager;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.utils.Logger;
import nro.models.utils.TimeUtil;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nro.models.matches.giai_dau.The23rdMartialArtCongressManager;
import nro.models.matches.giai_dau.DeathOrAliveArenaManager;
import nro.models.matches.giai_dau.WorldMartialArtsTournamentManager;
import nro.models.network.MessageSendCollect;
import nro.models.managers.SuperRankManager;
import nro.models.interfaces.ISessionAcceptHandler;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.event.EventManager;
import nro.models.Bot.BotManager;
import nro.models.boss.Boss_Manager.FinalBossManager;
import nro.models.data.LocalManager;
import nro.models.managers.ShenronEventManager;
import nro.models.minigame.ChonAiDay_Gem;
import nro.models.minigame.ChonAiDay_Gold;
import nro.models.minigame.ConSoMayManGem;
import nro.models.minigame.ConSoMayManGold;
import nro.models.services.ClanService;
import nro.models.services.TaskService;
import nro.models.shop.ShopTab;
import nro.models.shop_ky_gui.ConsignShopManager;

/**
 *
 * @author By Mr Blue
 *
 */
public class ServerManager {

    public static String timeStart;
    public static String firstStartTime;
    public static final long SESSION_START_MS = System.currentTimeMillis();
    private static volatile long accumulatedUptimeSec = 0L;
    private static volatile long lastUptimeCheckpointMs = System.currentTimeMillis();
    private static final String UPTIME_FILE_PATH = "data/server_uptime.properties";
    public static final Map<Object, Object> CLIENTS = new HashMap<>();
    public static String NAME_SERVER = "Ngọc Rồng VIAM";
    public static String DOMAIN = "Server 1";
    public static String NAME = "Ngọc Rồng VIAM";
    public static String IP = "103.179.190.242";
    public static int PORT = 14445;
    public static int EVENT_SEVER = 0;
    private static ServerManager instance;
    public static volatile boolean isRunning = true;
    private static volatile boolean isSavedOnClose = false;
    private ScheduledExecutorService topUpdater;

    public static synchronized void loadServerUptime() {
        long dbUptimeSec = 0L;
        String dbFirstStart = null;
        try (Connection con = LocalManager.getConnection()) {
            EventManager.ensureConfigTable(con);
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT cfg_key, cfg_val FROM admin_runtime_config WHERE cfg_key IN ('total_uptime_sec', 'first_time_start')");
                 java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String k = rs.getString("cfg_key");
                    String v = rs.getString("cfg_val");
                    if ("total_uptime_sec".equals(k) && v != null) {
                        try {
                            dbUptimeSec = Math.max(0L, Long.parseLong(v.trim()));
                        } catch (Exception ignored) {
                        }
                    } else if ("first_time_start".equals(k) && v != null && !v.trim().isEmpty()) {
                        dbFirstStart = v.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        long fileUptimeSec = 0L;
        String fileFirstStart = null;
        java.io.File f = new java.io.File(UPTIME_FILE_PATH);
        if (f.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
                Properties props = new Properties();
                props.load(fis);
                String val = props.getProperty("total_uptime_sec");
                if (val != null) {
                    fileUptimeSec = Math.max(0L, Long.parseLong(val.trim()));
                }
                String fs = props.getProperty("first_time_start");
                if (fs != null && !fs.trim().isEmpty()) {
                    fileFirstStart = fs.trim();
                }
            } catch (Exception ignored) {
            }
        }

        long bestUptime = Math.max(dbUptimeSec, fileUptimeSec);
        if (bestUptime <= 0L) {
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT COALESCE(MAX(total_play_sec), 0) FROM account");
                 java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bestUptime = Math.max(0L, rs.getLong(1));
                }
            } catch (Exception ignored) {
            }
        }

        accumulatedUptimeSec = bestUptime;
        lastUptimeCheckpointMs = System.currentTimeMillis();
        if (dbFirstStart != null && !dbFirstStart.isEmpty()) {
            firstStartTime = dbFirstStart;
        } else if (fileFirstStart != null && !fileFirstStart.isEmpty()) {
            firstStartTime = fileFirstStart;
        } else {
            firstStartTime = (timeStart != null && !timeStart.isEmpty())
                    ? timeStart
                    : TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
        }
        saveServerUptime();
        Logger.success("Đã khôi phục Thời Gian Hoạt Động Server: " + getFormattedUptimeHours() + " (Cộng dồn qua các đợt bảo trì/cập nhật)\n");
    }

    public static synchronized void saveServerUptime() {
        long now = System.currentTimeMillis();
        long deltaSec = Math.max(0L, (now - lastUptimeCheckpointMs) / 1000L);
        if (deltaSec > 0L) {
            accumulatedUptimeSec += deltaSec;
            lastUptimeCheckpointMs += deltaSec * 1000L;
        }
        if (firstStartTime == null || firstStartTime.isEmpty()) {
            firstStartTime = (timeStart != null && !timeStart.isEmpty())
                    ? timeStart
                    : TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
        }
        try {
            java.io.File f = new java.io.File(UPTIME_FILE_PATH);
            if (f.getParentFile() != null && !f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            Properties props = new Properties();
            props.setProperty("total_uptime_sec", String.valueOf(accumulatedUptimeSec));
            props.setProperty("first_time_start", firstStartTime);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(f)) {
                props.store(fos, "Persistent Server Uptime");
            }
        } catch (Exception ignored) {
        }
        try (Connection con = LocalManager.getConnection()) {
            EventManager.ensureConfigTable(con);
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO admin_runtime_config (cfg_key, cfg_val) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE cfg_val = VALUES(cfg_val)")) {
                ps.setString(1, "total_uptime_sec");
                ps.setString(2, String.valueOf(accumulatedUptimeSec));
                ps.addBatch();
                ps.setString(1, "first_time_start");
                ps.setString(2, firstStartTime);
                ps.addBatch();
                ps.executeBatch();
            }
        } catch (Exception ignored) {
        }
    }

    public static long getTotalUptimeSeconds() {
        long uncheckpointed = Math.max(0L, (System.currentTimeMillis() - lastUptimeCheckpointMs) / 1000L);
        return Math.max(0L, accumulatedUptimeSec + uncheckpointed);
    }

    public static long getSessionUptimeSeconds() {
        return Math.max(0L, (System.currentTimeMillis() - SESSION_START_MS) / 1000L);
    }

    public static double getTotalUptimeHours() {
        return Math.round((getTotalUptimeSeconds() / 3600.0) * 100.0) / 100.0;
    }

    public static String getFormattedUptimeHours() {
        long totalSec = getTotalUptimeSeconds();
        long h = totalSec / 3600L;
        long m = (totalSec % 3600L) / 60L;
        long s = totalSec % 60L;
        double hoursVal = totalSec / 3600.0;
        String hoursStr = hoursVal >= 10
                ? String.format(Locale.US, "%.1f", hoursVal).replaceAll("\\.0$", "")
                : String.format(Locale.US, "%.2f", hoursVal).replaceAll("\\.00$", "").replaceAll("(\\.\\d)0$", "$1");
        if (h > 0) {
            return hoursStr + " giờ (" + h + "g " + m + "p)";
        } else if (m > 0) {
            return hoursStr + " giờ (" + m + "p " + s + "s)";
        }
        return s + " giây";
    }

    public void init() {
        Manager.gI();
        //TaskService.gI().loadTask();
        HistoryTransactionDAO.deleteHistory();
    }

    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
            instance.init();
        }
        return instance;
    }

    public static void main(String[] args) {
        try {
            timeStart = TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
            //ShopTab.loadItem();
            new Thread(() -> {
                try {
                    ServerManager.gI().run();
                } catch (Exception e) {
                    Logger.logException(ServerManager.class, e);
                }
            }, "ServerMain").start();

            activeCommandLine();
        } catch (Exception e) {
            Logger.logException(ServerManager.class, e);
        }
    }

    public static void logException(Class<?> clazz, Exception e) {
        System.err.println("Lỗi tại " + clazz.getSimpleName() + ": " + e.getMessage());
        e.printStackTrace();
    }

    public void run() {
        try {
            isRunning = true;
            loadServerUptime();
            Runtime.getRuntime().addShutdownHook(new Thread(this::saveAllDataOnShutdown, "ShutdownSaveHook"));
            activeServerSocket();
            AdminWebServer.start();

            // Gửi các nhiệm vụ cập nhật theo từng dịch vụ
            new Thread(NgocRongNamecService.gI(), "Update NRNM").start();
            new Thread(SuperRankManager.gI(), "Update Super Rank").start();
            new Thread(The23rdMartialArtCongressManager.gI(), "Update DHVT23").start();
            new Thread(DeathOrAliveArenaManager.gI(), "Update Võ Đài Sinh Tử").start();
            new Thread(WorldMartialArtsTournamentManager.gI(), "Update WMAT").start();
            EventManager.loadRuntimeConfig();
            AutoMaintenance.gI().start();
            new Thread(ShenronEventManager.gI(), "Update Shenron").start();

            BossManager.gI().loadBoss();
            Manager.MAPS.forEach(nro.models.map.Map::initBoss);
            EventManager.gI().init();
            BossManager.gI().staggerInitialBossSpawns();

            new Thread(BossManager.gI(), "Update boss").start();
            new Thread(YardartManager.gI(), "Update yardart boss").start();
            new Thread(FinalBossManager.gI(), "Update final boss").start();
            new Thread(SkillSummonedManager.gI(), "Update skill-summoned boss").start();
            new Thread(BrolyManager.gI(), "Update broly boss").start();
            new Thread(OtherBossManager.gI(), "Update other boss").start();
            new Thread(RedRibbonHQManager.gI(), "Update red ribbon hq boss").start();
            new Thread(TreasureUnderSeaManager.gI(), "Update treasure under sea boss").start();
            new Thread(SnakeWayManager.gI(), "Update snake way boss").start();
            new Thread(GasDestroyManager.gI(), "Update gas destroy boss").start();

            new Thread(BotManager.gI(), "Thread Bot Game").start();
            new Thread(ChonAiDay_Gem.gI(), "Thread MiniGame").start();
            new Thread(ChonAiDay_Gold.gI(), "Thread MiniGame").start();
            new Thread(ConSoMayManGold.gI(), "ConSoMayManGoldThread").start();
            new Thread(ConSoMayManGem.gI(), "ConSoMayManGemThread").start();

            startTopUpdater();
        } catch (Exception e) {
            Logger.logException(this.getClass(), e);
        }
    }

    private void startTopUpdater() {
        topUpdater = Executors.newSingleThreadScheduledExecutor();
        topUpdater.scheduleAtFixedRate(() -> {
            if (shouldUpdateTop()) {
                updateTop();
                Manager.resetTopFlags();
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);

        // Định kỳ mỗi 30 giây lưu tổng thời gian hoạt động của Server (cộng dồn qua bảo trì & cập nhật)
        topUpdater.scheduleAtFixedRate(ServerManager::saveServerUptime, 30, 30, TimeUnit.SECONDS);

        // Định kỳ mỗi 60 giây tự động dọn rác bộ nhớ RAM (GC) và kết nối ngắt để RAM luôn ổn định ~200-350MB 24/7
        topUpdater.scheduleAtFixedRate(this::runPeriodicMemoryCleanup, 60, 60, TimeUnit.SECONDS);

        // Định kỳ mỗi 15 phút tự động dọn rác log hệ thống, MySQL binlog và server.log để chống tràn ổ cứng (No space left on device) lúc nửa đêm
        topUpdater.scheduleAtFixedRate(this::runPeriodicDiskCleanup, 5, 15, TimeUnit.MINUTES);
    }

    private void runPeriodicMemoryCleanup() {
        try {
            SessionManager.gI().cleanupSessions();
            for (nro.models.map.Map map : Manager.MAPS) {
                if (map == null || map.zones == null) {
                    continue;
                }
                for (nro.models.map.Zone z : map.zones) {
                    if (z == null || z.mobs == null) {
                        continue;
                    }
                    for (int i = 0; i < z.mobs.size(); i++) {
                        nro.models.mob.Mob m = z.mobs.get(i);
                        if (m != null && m.temporaryEnemies != null && !m.temporaryEnemies.isEmpty()) {
                            m.temporaryEnemies.removeIf(p -> p == null || p.zone != z || p.getSession() == null || !p.getSession().isConnected());
                        }
                    }
                }
            }
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            if (usedMb > 380) {
                System.gc();
            }
        } catch (Exception ignored) {
        }
    }

    private void runPeriodicDiskCleanup() {
        try {
            java.io.File srvLog = new java.io.File("server.log");
            if (srvLog.exists() && srvLog.length() > 5 * 1024 * 1024L) {
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(srvLog, "rw")) {
                    long len = raf.length();
                    int keep = 256 * 1024;
                    byte[] tail = new byte[keep];
                    raf.seek(Math.max(0, len - keep));
                    int read = raf.read(tail);
                    raf.setLength(0);
                    if (read > 0) {
                        raf.write(tail, 0, read);
                    }
                } catch (Exception ignored) {
                }
            }
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "DELETE FROM boss_kill_history WHERE id < (SELECT min_id FROM (SELECT MIN(id) AS min_id FROM (SELECT id FROM boss_kill_history ORDER BY id DESC LIMIT 300) t) t2)")) {
                ps.executeUpdate();
            } catch (Exception ignored) {
            }
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win")) {
                new ProcessBuilder("/bin/bash", "-c",
                        "truncate -s 0 /var/log/btmp /var/log/wtmp /var/log/syslog /var/log/auth.log /var/log/kern.log /var/log/dpkg.log /var/log/ufw.log /var/log/mysql/*.log 2>/dev/null || true; "
                        + "find /var/log -type f \\( -name '*.gz' -o -name '*.1' -o -name '*.old' \\) -delete 2>/dev/null || true; "
                        + "journalctl --vacuum-size=10M >/dev/null 2>&1 || true; "
                        + "rm -rf /tmp/*.tmp /var/tmp/* /var/crash/* /root/.cache/* /root/NRO/backups/.git 2>/dev/null || true; "
                        + "mysql -e 'PURGE BINARY LOGS BEFORE NOW(); SET GLOBAL binlog_expire_logs_seconds = 600;' >/dev/null 2>&1 || true")
                        .start();
            }
        } catch (Exception ignored) {
        }
    }

    private boolean shouldUpdateTop() {
        return Manager.isTopMaydamChanged
                || Manager.isTopSukienChanged
                || Manager.isTopSukien1Changed
                || Manager.isTopSukien2Changed
                || Manager.isTopWhisChanged;
    }

    private void stopTopUpdater() {
        if (topUpdater != null && !topUpdater.isShutdown()) {
            topUpdater.shutdown();
            System.out.println("Top updater stopped.");
        }
    }

    private void updateTop() {
        try (Connection con = LocalManager.gI().getConnection()) {
            if (Manager.isTopMaydamChanged) {
                Manager.Topmaydam = Manager.realTop(Manager.queryTopmaydam, con);
            }
            if (Manager.isTopSukienChanged) {
                Manager.Topsukien = Manager.realTop(Manager.queryTopsukien, con);
            }
            if (Manager.isTopSukien1Changed) {
                Manager.Topsukien1 = Manager.realTop(Manager.queryTopsukien1, con);
            }
            if (Manager.isTopSukien2Changed) {
                Manager.Topsukien1 = Manager.realTop(Manager.queryTopsukien1, con);
            }
            if (Manager.isTopWhisChanged) {
                Manager.Topwhis = Manager.realTop(Manager.queryTopwhis, con);
            }

            Manager.resetTopFlags();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void activeServerSocket() {
        try {
            Network.gI().init().setAcceptHandler(new ISessionAcceptHandler() {
                @Override
                public void sessionInit(ISession is) {
                    if (!canConnectWithIp(is.getIP())) {
                        is.disconnect();
                        return;
                    }
                    is.setMessageHandler(Controller.gI())
                            .setSendCollect(new MessageSendCollect())
                            .setKeyHandler(new MyKeyHandler())
                            .startCollect();
                }

                @Override
                public void sessionDisconnect(ISession session) {
                    Client.gI().kickSession((MySession) session);
                    disconnect((MySession) session);
                }
            }).setTypeSessioClone(MySession.class)
                    .setDoSomeThingWhenClose(() -> {
                        Logger.error("SERVER CLOSE\n");
                        System.exit(0);
                    })
                    .start(PORT);
        } catch (Exception e) {
            Logger.error("Lỗi khi khởi động máy chủ: " + e.getMessage());
        }
    }

    private boolean canConnectWithIp(String ipAddress) {
        Object o = CLIENTS.get(ipAddress);
        if (o == null) {
            CLIENTS.put(ipAddress, 1);
            return true;
        } else {
            int n = Integer.parseInt(String.valueOf(o));
            if (n < Manager.MAX_PER_IP) {
                n++;
                CLIENTS.put(ipAddress, n);
                return true;
            } else {
                return false;
            }
        }
    }

    public void disconnect(MySession session) {
        Object o = CLIENTS.get(session.getIP());
        if (o != null) {
            int n = Integer.parseInt(String.valueOf(o));
            n--;
            if (n < 0) {
                n = 0;
            }
            CLIENTS.put(session.getIP(), n);
        }
    }

    public void resetNhanQuaHangNgay() {
        String url = "jdbc:mysql://localhost:3306/ngocrong";
        String username = "root";
        String password = "";
        String resetJson = "[1,1,\"1970-01-01T00:00:00\"]";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "UPDATE player SET checkNhanQua = ? WHERE checkNhanQua != ?";
            PreparedStatement statement = conn.prepareStatement(sql);

            statement.setString(1, resetJson);
            statement.setString(2, resetJson);

            int rowsUpdated = statement.executeUpdate();
            Logger.success("Đã reset nhận quà hằng ngày cho " + rowsUpdated + " người chơi với dữ liệu: " + resetJson);
        } catch (SQLException e) {
            System.err.println("Lỗi reset nhận quà hằng ngày: " + e.getMessage());
        }
    }

    public synchronized void saveAllDataOnShutdown() {
        if (isSavedOnClose) {
            return;
        }
        isSavedOnClose = true;
        isRunning = false;
        try {
            saveServerUptime();
        } catch (Exception ignored) {
        }
        try {
            EventManager.saveRuntimeConfig();
        } catch (Exception ignored) {
        }
        try {
            ClanService.gI().close();
        } catch (Exception e) {
            Logger.error("Lỗi save clan!\n");
        }
        try {
            ConsignShopManager.gI().save();
        } catch (Exception e) {
            Logger.error("Lỗi save shop ký gửi!\n");
        }
        try {
            Client.gI().close();
        } catch (Exception e) {
            Logger.error("Lỗi save player!\n");
        }
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win") && new java.io.File("backup_db.sh").exists()) {
                Process p = new ProcessBuilder("/bin/bash", "./backup_db.sh")
                        .directory(new java.io.File("."))
                        .start();
                p.waitFor(15, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Logger.error("Lỗi backup database khi đóng server: " + e.getMessage() + "\n");
        }
        Logger.success("SUCCESSFULLY SAVED ALL DATA!\n");
    }

    public void close() {
        saveAllDataOnShutdown();
        Logger.success("SUCCESSFULLY MAINTENANCE!\n");

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("cmd /c start restart_server.bat");
            } else {
                new ProcessBuilder("/bin/bash", "-c", "sleep 2 && nohup ./run.sh --fg > server.log 2>&1 &")
                        .directory(new java.io.File("."))
                        .start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static void activeCommandLine() {
        try {
            Scanner sc = new Scanner(System.in);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                switch (line) {
                    case "bt":
                        Maintenance.gI().startSeconds(5);
                        break;
                    case "bat":
                        AutoMaintenance.AutoMaintenance = true;
                        System.out.println("Đã bật chế độ bảo trì tự động.");
                        break;
                    case "tat":
                        AutoMaintenance.AutoMaintenance = false;
                        System.out.println("Đã tắt chế độ bảo trì tự động.");
                        break;
                    case "run":
                        try {
                            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "run.bat");
                            pb.inheritIO();
                            pb.start();
                            System.out.println("Đã chạy run.bat");
                        } catch (IOException e) {
                            System.out.println("Lỗi khi chạy run.bat: " + e.getMessage());
                        }
                        break;
                    default:
                        System.out.println("Lệnh không hợp lệ.");
                        break;
                }
            }
        } catch (Exception ignored) {
        }
        // Khi chạy ngầm bằng nohup (stdin = /dev/null), giữ luồng main sống mãi mãi
        while (isRunning) {
            try {
                Thread.sleep(60000L);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
