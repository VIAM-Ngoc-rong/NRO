package nro.models.server;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import nro.models.event.EventManager;
import nro.models.services.Service;
import nro.models.utils.Logger;

/**
 *
 * @author By Mr Blue
 *
 */
public class AutoMaintenance extends Thread {

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static volatile boolean AutoMaintenance = true;
    public static volatile int hours = 8; // Mặc định 08:00 sáng giờ Việt Nam
    public static volatile int mins = 0;
    public static volatile String lastMaintOrUpdateDate = "";
    private static AutoMaintenance instance;
    public static volatile boolean isRunning;
    private long lastAnnouncementTime = 0;
    private boolean hasResetAfterMaintenance = false;
    private volatile boolean hasAnnounced = false;
    private volatile long announceStartTime = 0L;

    public static AutoMaintenance gI() {
        if (instance == null) {
            instance = new AutoMaintenance();
        }
        return instance;
    }

    public static String getTodayVN() {
        return LocalDate.now(VN_ZONE).toString();
    }

    public static boolean isSkippedToday() {
        String today = getTodayVN();
        return today.equals(lastMaintOrUpdateDate);
    }

    public static void markUpdatedToday() {
        lastMaintOrUpdateDate = getTodayVN();
    }

    public static void clearSkipToday() {
        lastMaintOrUpdateDate = "";
    }

    public void resetCountdownState() {
        this.hasAnnounced = false;
        this.announceStartTime = 0L;
        this.lastAnnouncementTime = 0L;
        this.hasResetAfterMaintenance = false;
    }

    @Override
    public void run() {
        try {
            LocalTime now = LocalTime.now(VN_ZONE);
            LocalTime sched = LocalTime.of(Math.max(0, Math.min(23, hours)), Math.max(0, Math.min(59, mins)));
            int nowSec = now.toSecondOfDay();
            int schedSec = sched.toSecondOfDay();
            if (schedSec > nowSec && (schedSec - nowSec) <= 6 * 3600) {
                if (!isSkippedToday()) {
                    markUpdatedToday();
                    EventManager.saveRuntimeConfig();
                    Logger.success("Server vừa khởi động/cập nhật mới hôm nay -> Tự động bỏ qua lịch bảo trì "
                            + String.format("%02d:%02d", hours, mins) + " ngày " + lastMaintOrUpdateDate + "\n");
                }
            }
        } catch (Exception ignored) {
        }

        while (true) {
            try {
                if (AutoMaintenance) {
                    LocalTime currentTime = LocalTime.now(VN_ZONE);
                    LocalTime maintenanceTime = LocalTime.of(Math.max(0, Math.min(23, hours)), Math.max(0, Math.min(59, mins)));
                    LocalTime maintenanceEndTime = maintenanceTime.plusMinutes(5);

                    if (!hasAnnounced
                            && !isSkippedToday()
                            && currentTime.isAfter(maintenanceTime.minusSeconds(1))
                            && currentTime.isBefore(maintenanceEndTime)) {
                        announceStartTime = System.currentTimeMillis();
                        hasAnnounced = true;
                        hasResetAfterMaintenance = false;
                        Logger.log(Logger.PURPLE, "Bắt đầu đếm ngược bảo trì định kỳ (5 phút)");
                    }

                    if (hasAnnounced) {
                        long elapsed = System.currentTimeMillis() - announceStartTime;
                        long totalWait = 5 * 60 * 1000L;
                        long remainingMs = Math.max(0L, totalWait - elapsed);

                        long remainingSeconds = remainingMs / 1000L;
                        long minutesLeft = remainingSeconds / 60L;
                        long secondsLeft = remainingSeconds % 60L;

                        if (lastAnnouncementTime == 0 || System.currentTimeMillis() - lastAnnouncementTime >= 15_000L) {
                            Service.gI().sendThongBaoAllPlayer(
                                    String.format("Máy chủ sẽ bảo trì định kỳ trong %d phút %d giây nữa. Vui lòng thoát game để tránh mất dữ liệu.",
                                            minutesLeft, secondsLeft));
                            Logger.log(Logger.PURPLE, "Thông báo bảo trì tự động đã gửi.");
                            lastAnnouncementTime = System.currentTimeMillis();
                        }

                        if (elapsed >= totalWait) {
                            if (!Maintenance.isRunning && !isRunning) {
                                Logger.log(Logger.PURPLE, "Đang tiến hành quá trình bảo trì tự động...");
                                markUpdatedToday();
                                EventManager.saveRuntimeConfig();
                                Maintenance.gI().startCountdown();
                                isRunning = true;
                            }
                        }
                    }

                    if (currentTime.isAfter(maintenanceEndTime) && !hasResetAfterMaintenance) {
                        hasAnnounced = false;
                        lastAnnouncementTime = 0;
                        isRunning = false;
                        hasResetAfterMaintenance = true;
                        Logger.log(Logger.PURPLE, "Reset AutoMaintenance sau thời gian bảo trì.\n");
                    }
                } else {
                    hasAnnounced = false;
                }

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception ignored) {
            }
        }
    }
}
