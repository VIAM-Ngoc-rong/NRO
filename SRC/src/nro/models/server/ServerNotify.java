package nro.models.server;

import nro.models.event.EventManager;
import nro.models.managers.GiftCodeManager;
import nro.models.player_system.GiftCode;
import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.services.Service;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;
import java.util.ArrayList;
import java.util.List;

public class ServerNotify extends Thread {

    private long lastTimeGK;

    private final List<String> notifies;

    private static ServerNotify i;

    private ServerNotify() {
        this.notifies = new ArrayList<>();
        this.start();
    }

    public static ServerNotify gI() {
        if (i == null) {
            i = new ServerNotify();
        }
        return i;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                while (!notifies.isEmpty()) {
                    ThongBao(notifies.remove(0));
                }
                if (Util.canDoWithTime(this.lastTimeGK, 500000)) {
                    ThongBao("Chào mừng bạn đã đến server Ngọc Rồng VIAM");
                    this.lastTimeGK = System.currentTimeMillis();
                }
            } catch (Exception ignored) {

            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void ThongBao(String text) {
        Message msg;
        try {
            msg = new Message(93);
            msg.writer().writeUTF(text);
            Service.gI().sendMessAllPlayer(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void notify(String text) {
        if (!Client.gI().getPlayers().isEmpty()) {
            this.notifies.add(text);
        }
    }

    public void sendActiveBossesToPlayer(Player player) {
        if (player == null || player.session == null) {
            return;
        }
        try {
            List<nro.models.boss.Boss> allBosses = new ArrayList<>(nro.models.boss.Boss_Manager.BossManager.gI().getBosses());
            allBosses.addAll(nro.models.boss.Boss_Manager.BrolyManager.gI().getBosses());
            allBosses.addAll(nro.models.boss.Boss_Manager.FinalBossManager.gI().getBosses());
            for (nro.models.boss.Boss boss : allBosses) {
                if (boss != null && boss.zone != null && boss.zone.map != null && !boss.isDie() && boss.canSendNotify()) {
                    Message msg = new Message(93);
                    msg.writer().writeUTF("BOSS " + boss.name + " vừa xuất hiện tại " + boss.zone.map.mapName);
                    player.sendMessage(msg);
                    msg.cleanup();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private List<String[]> buildNotifyEntries() {
        List<String[]> entries = new ArrayList<>();

        // 1. Mục Trạng thái Sự kiện & Giftcode đang hoạt động trực tiếp (Live)
        StringBuilder liveSb = new StringBuilder();
        liveSb.append("• Tỷ lệ TNSM toàn Server: x").append(Manager.RATE_EXP_SERVER).append("\n");
        liveSb.append("• Vàng rơi từ quái: ").append((EventManager.DOUBLE_GOLD || TimeUtil.isWeekend()) ? "ĐANG X2 VÀNG TOÀN SERVER" : "Mặc định (Cuối tuần tự động X2)").append("\n");
        liveSb.append("• Tỷ lệ rơi Vật Phẩm Sự Kiện: ").append(EventManager.EVENT_DROP_RATE).append("%\n\n");

        List<String> activeEvents = new ArrayList<>();
        if (EventManager.HALLOWEEN) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Halloween: Đánh quái rơi Bí Ngô 1-7 Sao (gọi Rồng Xương Halloween) & săn Boss Bí Ma, Ma Trơi, Dơi.");
        }
        if (EventManager.CHRISTMAS) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Giáng Sinh: Đánh quái rơi Tất Vớ (649) & Hộp Quà Giáng Sinh (648) + săn Boss Ông Già Noel.");
        }
        if (EventManager.LUNNAR_NEW_YEAR) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Tết Nguyên Đán: Đánh quái rơi Bao Lì Xì (717 - bấm Sử dụng mở Vàng, Ngọc, Hồng Ngọc, Cỏ 4 Lá, Đá Ngũ Sắc) & săn Boss Lân Con.");
        }
        if (EventManager.TRUNG_THU) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Trung Thu: Đánh quái rơi Hộp Quà Trung Thu (736 - bấm Sử dụng mở quà) & săn Boss Khỉ Đột, Nguyệt Thần, Nhật Thần.");
        }
        if (EventManager.HUNG_VUONG) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Giỗ Tổ Hùng Vương: Rơi Dưa Hấu, Tem, Ngà Voi, Cựa Gà, Hồng Mao (bấm Sử dụng trực tiếp hoặc gặp Vua Hùng đổi Ngọc & Hộp Quà VIP) + Boss Sơn Tinh, Thủy Tinh.");
        }
        if (EventManager.INTERNATIONAL_WOMANS_DAY) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Quốc Tế Phụ Nữ 8/3: Đánh quái rơi Bông Hoa (589 - gom x10 bấm Sử dụng nhận Ngọc, Hồng Ngọc, Hộp Quà VIP).");
        }
        if (EventManager.SUMMER_NUOC_MIA) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Mùa Hè Nước Mía: Rơi Khúc Mía (1612), Nước Đá (1613), Kem Trái Cây (1609). Bấm Sử dụng trực tiếp hoặc gặp Xe Nước Mía & đua Top tại ChiChi.");
        }
        if (EventManager.CHILDREN_DAY) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Quốc Tế Thiếu Nhi 1/6: Đánh quái rơi Hộp Quà Thiếu Nhi (1608 - bấm Sử dụng nhận Cải trang VIP & điểm đua Top tại ChiChi).");
        }
        if (EventManager.COOKING_BANH) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Nấu Bánh Chưng / Bánh Dầy: Rơi Cơm Nếp, Đậu Xanh, Thịt Tươi, Bột Gạo, Muối Tiêu, Chả Lụa. Bấm Sử dụng nguyên liệu để nấu ngay hoặc gặp Nồi Bánh.");
        }
        if (EventManager.NEW_YEAR_FIREWORK) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Pháo Hoa Lễ Hội: Đánh quái rơi Pháo Bông (1575) & Pháo Bông VIP (1576) - bấm Sử dụng nổ pháo hoa nhận 500K - 2Tr Vàng.");
        }
        if (EventManager.TOP_UP) {
            activeEvents.add("- [ĐANG BẬT] Sự kiện Khuyến Mãi Nạp (Top Up).");
        }

        liveSb.append("=== SỰ KIỆN ĐANG MỞ TRÊN SERVER ===\n");
        if (activeEvents.isEmpty()) {
            liveSb.append("Hiện đang tạm nghỉ giữa các đợt sự kiện lễ hội. Các hoạt động săn Boss, Doanh Trại, Bản Đồ Kho Báu, Khí Gas, Mabư 12h/14h, Sao Đen 20h, Frieza Vàng 21h vẫn hoạt động bình thường!\n");
        } else {
            for (String ev : activeEvents) {
                liveSb.append(ev).append("\n");
            }
        }

        // Hiển thị danh sách Giftcode đang còn lượt nhập
        try {
            ArrayList<GiftCode> codes = GiftCodeManager.gI().listGiftCode;
            if (codes != null && !codes.isEmpty()) {
                StringBuilder codeSb = new StringBuilder();
                for (GiftCode gc : codes) {
                    if (gc != null && gc.code != null && gc.countLeft > 0 && !gc.timeCode()) {
                        codeSb.append("• Mã: ").append(gc.code.toUpperCase())
                              .append(" (Còn ").append(gc.countLeft >= 99999 ? "Vô hạn" : gc.countLeft).append(" lượt)\n");
                    }
                }
                if (codeSb.length() > 0) {
                    liveSb.append("\n=== MÃ QUÀ TẶNG (GIFTCODE) ĐANG MỞ ===\n");
                    liveSb.append("Nhập tại Ông Gôhan / Ông Moori / Ông Paragus hoặc Santa (Đảo Kame):\n");
                    liveSb.append(codeSb);
                }
            }
        } catch (Exception ignored) {
        }

        entries.add(new String[]{"Sự Kiện & Code Đang Mở (Live)", liveSb.toString().trim()});

        // 2. Hướng dẫn chi tiết cách dùng Vật Phẩm Sự Kiện (VPSK)
        entries.add(new String[]{
            "Hướng Dẫn Vật Phẩm Sự Kiện (VPSK)",
            "Tất cả Vật Phẩm Sự Kiện (VPSK) rơi từ quái đều có thể bấm 'Sử dụng' trực tiếp trong Hành trang hoặc đổi tại NPC:\n"
            + "1. Halloween: Đủ 7 viên Bí Ngô (1-7 sao) bấm Sử dụng gọi Rồng Xương Halloween.\n"
            + "2. Giáng Sinh: Giữ Tất vớ (649) để nhặt Hộp quà (648) hoặc dùng 10 Tất vớ ghép 1 Hộp quà.\n"
            + "3. Tết Nguyên Đán: Bấm Sử dụng Bao Lì Xì (717) nhận Vàng, Ngọc Xanh, Hồng Ngọc, Thỏi Vàng, Cỏ 4 Lá, Đá Ngũ Sắc.\n"
            + "4. Trung Thu: Bấm Sử dụng Hộp quà Trung Thu (736) mở quà ngẫu nhiên.\n"
            + "5. Giỗ Tổ Hùng Vương: Dùng 1 Dưa Hấu + 1 Tem nhận 10-25 Ngọc Xanh; dùng bộ 9 Ngà Voi + 9 Cựa Gà + 9 Hồng Mao + 1Tr Vàng nhận Hộp Quà Thường/VIP.\n"
            + "6. Quốc Tế Phụ Nữ 8/3: Gom 10 Bông Hoa (589) bấm Sử dụng nhận Ngọc, Hồng Ngọc, Cỏ 4 Lá, Hộp Quà VIP.\n"
            + "7. Mùa Hè Nước Mía: Dùng 5 Khúc mía + 2 Nước đá + 5Tr Vàng ép Ly Nước Mía; dùng Kem Trái Cây (1609) nhận Cải trang VIP.\n"
            + "8. Thiếu Nhi 1/6: Dùng Hộp Quà Thiếu Nhi (1608) nhận Cải trang VIP & điểm đua Top tại ChiChi.\n"
            + "9. Nấu Bánh: Dùng 10 Cơm nếp + 10 Đậu xanh + 10 Thịt tươi nấu Bánh Chưng; hoặc 10 Cơm nếp + 5 Bột gạo + 5 Muối tiêu + 1 Chả lụa nấu Bánh Dầy.\n"
            + "10. Pháo Hoa: Dùng Pháo Bông (1575/1576) bắn pháo hoa nhận 500K - 2Tr Vàng."
        });

        // 3. Hướng dẫn Tuyệt Kỹ Whis & Săn Hồng Ngọc
        entries.add(new String[]{
            "Bí Kíp Tuyệt Kỹ & Săn Hồng Ngọc",
            "• Săn Hồng Ngọc từ Boss Hằng Ngày:\n"
            + "  - Tiểu Đội Bojack (Bujin, Kogu, Zangya, Bido, Bojack, Super Bojack) & Tiểu Đội Sát Thủ Namek (Số 4, Số 3, Số 2, Số 1, Tiểu Đội Trưởng): Rớt 20 - 30 Hồng Ngọc / 1 Boss.\n"
            + "• Cày Bí Kíp Tuyệt Kỹ (ID 1229) & Trang Sách Cũ (ID 1281):\n"
            + "  - Đánh quái tại Hành Tinh Bill (Map 154) hoặc Hành Tinh Ngục Tù (Map 155).\n"
            + "  - Tỷ lệ rớt 50% (tăng lên 75% - 90% khi mặc Set Hủy Diệt & dùng Cỏ 4 Lá), mỗi lần rớt x1 - x5 quyển (cả Đệ tử đánh cũng rớt).\n"
            + "• Học Tuyệt Kỹ (Skill 9) tại NPC Whis (Hành Tinh Bill):\n"
            + "  - Mở khóa cấp 1: Cần x999 Bí kíp tuyệt kỹ + 100Tr Vàng (tỷ lệ 100%).\n"
            + "  - Nâng cấp bậc 2 -> 9: Cần x99 Bí kíp tuyệt kỹ + 100Tr Vàng.\n"
            + "• Chế Tạo Sách Tuyệt Kỹ tại Bà Hạt Mít (Đảo Kame):\n"
            + "  - Đóng Cuốn Sách Cũ: x999 Trang sách cũ + 1 Bìa sách -> 1 Cuốn sách cũ.\n"
            + "  - Đổi Sách Tuyệt Kỹ 1: 10 Cuốn sách cũ + 1 Kìm bấm giấy (dùng Con dấu đạt 100%)."
        });

        // 4. Hướng dẫn Hành trang 5 Tab & Auto Treo Quái
        entries.add(new String[]{
            "Hành Trang 500 Ô & Auto Treo Quái",
            "• Hành Trang Đa Tab (Mặc định 200 ô, tối đa 500 ô / 5 Tab):\n"
            + "  - Gõ chat 't1', 't2', 't3', 't4', 't5' hoặc bấm Nút Lá Cờ (auto) -> Tab Túi Đồ để chuyển Tab ngay cả khi túi đang đầy.\n"
            + "  - Khi Tab hiện tại đầy 100/100 ô, vật phẩm nhặt mới tự động tràn sang Tab còn trống tiếp theo.\n"
            + "• Auto Treo Quái & Lọc Đồ Rác:\n"
            + "  - Gõ chat 'treoquai' hoặc 'tq' để bật tự động bán đồ rác khi đầy túi (khóa bảo vệ tuyệt đối 100% Đồ SKH, Đồ Thần Linh, Hủy Diệt, Thiên Sứ và Đồ có lỗ Sao Pha Lê)."
        });

        // 5. Thêm các thông báo cấu hình trong bảng notify của MySQL (nếu có)
        if (Manager.NOTIFY != null) {
            for (String raw : Manager.NOTIFY) {
                if (raw != null && raw.contains("<>")) {
                    String[] arr = raw.split("<>", 2);
                    if (arr.length == 2 && !arr[0].trim().isEmpty()) {
                        entries.add(new String[]{arr[0].trim(), arr[1].trim()});
                    }
                }
            }
        }

        return entries;
    }

    public void sendNotifyTab(Player player) {
        if (player == null || player.getSession() == null) {
            return;
        }
        Message msg = null;
        try {
            List<String[]> entries = buildNotifyEntries();
            int count = Math.min(entries.size(), 50);
            msg = new Message(50);
            msg.writer().writeByte(count);
            for (int i = 0; i < count; i++) {
                String[] arr = entries.get(i);
                msg.writer().writeShort(i);
                msg.writer().writeUTF(arr[0]);
                msg.writer().writeUTF(arr[1]);
            }
            player.sendMessage(msg);
        } catch (Exception ignored) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void broadcastNotifyTab() {
        try {
            List<Player> players = new ArrayList<>(Client.gI().getPlayers());
            for (Player pl : players) {
                if (pl != null && pl.isPl() && !pl.isBot && pl.getSession() != null) {
                    sendNotifyTab(pl);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
