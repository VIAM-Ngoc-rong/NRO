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
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN HALLOWEEN:\n"
                    + "  1. Bí Ngô 1 Sao -> 7 Sao (ID 702 - 708):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Gom đủ 7 quả Bí Ngô (từ 1 sao đến 7 sao) trong Hành trang rồi bấm 'Sử dụng' vào bất kỳ quả nào để triệu hồi Rồng Xương Halloween ước thưởng.\n"
                    + "  2. Bí Ngô (585), Kẹo Bàn Tay (901), Bánh Gato Nhện (902):\n"
                    + "     • Cách kiếm: Tiêu diệt Boss Bí Ma, Ma Trơi, Dơi (xuất hiện ngẫu nhiên trên các map).\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang để hóa giải hiệu ứng biến hình Halloween và nhận ngẫu nhiên: 2 - 8Tr Vàng, 10 - 30 Ngọc Xanh, 5 - 20 Hồng Ngọc, Cuồng Nộ/Bổ Huyết/Bổ Khí Cấp 2, Cải trang Halloween (Bí Ngô, Xương Khô, Ma Trơi, Dơi Nhí) + 1 Điểm Sự Kiện.");
        }
        if (EventManager.CHRISTMAS) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN GIÁNG SINH (NOEL):\n"
                    + "  1. Tất, Vớ Giáng Sinh (ID 649):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Giữ Tất vớ (649) trong Hành trang khi đánh quái sẽ rớt thêm Hộp Quà Giáng Sinh (648); HOẶC gom đủ x10 Tất vớ bấm 'Sử dụng' trực tiếp trong Hành trang để ghép thành 1 Hộp Quà Giáng Sinh (648).\n"
                    + "  2. Hộp Quà Giáng Sinh (ID 648):\n"
                    + "     • Cách kiếm: Đánh quái khi có Tất vớ, ghép từ 10 Tất vớ, hoặc nhặt từ Boss Ông Già Noel.\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang để mở ra: Cải trang Noel VIP (387-393), Pet Tuần Lộc (936), Cờ Cây Thông (822), Ván bay Xe Trượt Tuyết (746), Ngọc Rồng Băng 1-7 Sao (925-931), Kẹo Giáng Sinh (533), Ngọc Xanh, Đá Bảo Vệ, Vật phẩm Cấp 2 + 1 Điểm Sự Kiện.\n"
                    + "  3. Kẹo Giáng Sinh (ID 533):\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang nhận 1 - 5Tr Vàng, 5 - 15 Ngọc Xanh hoặc 5 - 15 Hồng Ngọc + 1 Điểm Sự Kiện.\n"
                    + "  4. Bộ Ngọc Rồng Băng 1 Sao -> 7 Sao (ID 925 - 931):\n"
                    + "     • Cách dùng: Gom đủ 7 viên Ngọc Rồng Băng (1-7 sao) bấm 'Sử dụng' để gọi Rồng Thần Băng Giáng Sinh ước Cải trang Noel 30-90 ngày, 20.000 Hồng Ngọc, 1.000 Ngọc Xanh hoặc Kỹ năng Đệ tử.");
        }
        if (EventManager.LUNNAR_NEW_YEAR) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN TẾT NGUYÊN ĐÁN:\n"
                    + "  1. Bao Lì Xì (ID 717):\n"
                    + "     • Cách kiếm: Đánh quái trên tất cả các bản đồ hoặc tiêu diệt Boss Lân Con.\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang nhận ngẫu nhiên: 2 - 10Tr Vàng, 10 - 50 Ngọc Xanh, 5 - 25 Hồng Ngọc, Thỏi Vàng (457), Cỏ 4 Lá (1230), Đá Ngũ Sắc (674), Đá Bảo Vệ (987) hoặc Cuồng Nộ/Bổ Huyết/Bổ Khí Cấp 2 + 1 Điểm Sự Kiện.\n"
                    + "  2. Boss Lân Con:\n"
                    + "     • Dẫn dụ hoặc tiêu diệt Lân Con để nhận Bao Lì Xì (717), Hồng Ngọc và Điểm Sự Kiện.");
        }
        if (EventManager.TRUNG_THU) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN TẾT TRUNG THU:\n"
                    + "  1. Hộp Quà Trung Thu (ID 736):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang (cần ít nhất 2 ô trống) để mở ra: Ngọc Xanh, Đá Bảo Vệ, Ván bay/Phụ kiện/Cải trang Lễ Hội, Cuồng Nộ/Bổ Huyết/Bổ Khí + 1 Điểm Sự Kiện.\n"
                    + "  2. Săn Boss Trung Thu (Khỉ Đột, Nguyệt Thần, Nhật Thần):\n"
                    + "     • Boss Khỉ Đột: Tiêu diệt nhận 5.000 Hồng Ngọc + 1 Điểm Sự Kiện.\n"
                    + "     • Boss Nguyệt Thần & Nhật Thần: Tiêu diệt rơi Cải trang Nguyệt Thần (1276) / Cải trang Nhật Thần (1277) chỉ số 22-40% SĐ/HP/KI + 5 Điểm Sự Kiện.");
        }
        if (EventManager.HUNG_VUONG) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN GIỖ TỔ HÙNG VƯƠNG:\n"
                    + "  1. Dưa Hấu (ID 569) & Tem (ID 534):\n"
                    + "     • Cách kiếm: Đánh quái rơi Dưa Hấu & Tem (hoặc trồng Dưa Hấu tại NPC Vua Hùng).\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp vào Dưa Hấu hoặc Tem trong Hành trang (tiêu hao 1 Dưa Hấu + 1 Tem) nhận ngay 10 - 25 Ngọc Xanh + 1 Điểm Sự Kiện; HOẶC đem đổi gói lớn tại NPC Vua Hùng (tối đa 250 Ngọc Xanh).\n"
                    + "  2. Bộ Sính Lễ: Ngà Voi (563), Cựa Gà (565), Hồng Mao (567):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ hoặc săn Boss Sơn Tinh, Thủy Tinh.\n"
                    + "     • Cách dùng: Gom đủ bộ 9 Ngà Voi + 9 Cựa Gà + 9 Hồng Mao + 1Tr Vàng, bấm 'Sử dụng' trực tiếp vào 1 trong 3 vật phẩm trong Hành trang (hoặc dâng tại NPC Vua Hùng) để nhận Hộp Quà Thường (1776) hoặc Hộp Quà VIP (1777) + 1 Điểm Sự Kiện.\n"
                    + "  3. Hộp Quà Thường (1776) & Hộp Quà VIP (1777):\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trong Hành trang mở ra Ngọc Rồng 3-7 Sao, Cải trang Hùng Vương / Sơn Tinh / Thủy Tinh VIP (1761-1764, 1731-1732), Thú cưỡi & Phụ kiện VIP + 1 Điểm Sự Kiện.");
        }
        if (EventManager.INTERNATIONAL_WOMANS_DAY) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN QUỐC TẾ PHỤ NỮ 8/3:\n"
                    + "  1. Bông Hoa (ID 589) & Hoa Hồng (ID 610):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Gom đủ x10 Bông Hoa (589) hoặc x10 Hoa Hồng (610) rồi bấm 'Sử dụng' trực tiếp trong Hành trang để nhận ngẫu nhiên: 10 - 30 Ngọc Xanh, 5 - 15 Hồng Ngọc, 2 - 5Tr Vàng, Cỏ 4 Lá (1230), Đá Bảo Vệ (987) hoặc Hộp Quà Thường (1776) / Hộp Quà VIP (1777) + 1 Điểm Sự Kiện.\n"
                    + "     • Ngoài ra có thể gặp NPC Lý Tiểu Nương để đổi quà sự kiện.");
        }
        if (EventManager.SUMMER_NUOC_MIA) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN MÙA HÈ NƯỚC MÍA:\n"
                    + "  1. Khúc Mía (ID 1612) & Nước Đá (ID 1613):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Gom đủ 5 Khúc Mía + 2 Nước Đá + 5Tr Vàng, bấm 'Sử dụng' trực tiếp vào Khúc Mía hoặc Nước Đá trong Hành trang (hoặc gặp NPC Xe Nước Mía) để ép thành 1 Ly Nước Mía (1614/1615/1616) + 1 Điểm Sự Kiện.\n"
                    + "  2. Ly Nước Mía Nhỏ/Vừa/Lớn (ID 1614, 1615, 1616):\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang để uống, nhận hiệu ứng tăng +10% HP, +10% KI, +10% Sức Đánh trong 10 - 30 phút.\n"
                    + "  3. Kem Trái Cây (ID 1609):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang để nhận Cải trang / Vật phẩm Mùa Hè VIP (1803, 1804, 1806), cộng +1 điểm Kem Trái Cây đua Top tại NPC ChiChi & +1 Điểm Sự Kiện.");
        }
        if (EventManager.CHILDREN_DAY) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN QUỐC TẾ THIẾU NHI 1/6:\n"
                    + "  1. Hộp Quà Thiếu Nhi (ID 1608):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang để mở ra Cải trang & Phụ kiện Thiếu Nhi VIP (1807, 1599, 1600, 1601, 1602) chỉ số 15-26% SĐ/HP/KI, đồng thời cộng +1 điểm Quà Thiếu Nhi đua Top tại NPC ChiChi & +1 Điểm Sự Kiện.");
        }
        if (EventManager.COOKING_BANH) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN NẤU BÁNH CHƯNG - BÁNH DẦY:\n"
                    + "  1. Nguyên liệu rơi từ quái: Cơm Nếp (1571), Đậu Xanh (1569), Thịt Tươi (1568), Bột Gạo (1572), Muối Tiêu (1570), Chả Lụa (1573).\n"
                    + "  2. Cách nấu Bánh Chưng (ID 1579):\n"
                    + "     • Gom đủ 10 Cơm Nếp + 10 Đậu Xanh + 10 Thịt Tươi, bấm 'Sử dụng' trực tiếp vào Đậu Xanh hoặc Thịt Tươi (hoặc Cơm Nếp) trong Hành trang để nấu ngay 1 Bánh Chưng (hoặc nấu tại NPC Nồi Bánh).\n"
                    + "  3. Cách nấu Bánh Dầy (ID 1578):\n"
                    + "     • Gom đủ 10 Cơm Nếp + 5 Bột Gạo + 5 Muối Tiêu + 1 Chả Lụa, bấm 'Sử dụng' trực tiếp vào Bột Gạo, Muối Tiêu hoặc Chả Lụa trong Hành trang để nấu ngay 1 Bánh Dầy.\n"
                    + "  4. Cách dùng Bánh Chưng (1579) & Bánh Dầy (1578):\n"
                    + "     • Bấm 'Sử dụng' trực tiếp trong Hành trang (hoặc dâng tại NPC Vua Hùng) để nhận Hộp Quà Thường (1776) hoặc Hộp Quà VIP (1777) + 1 Điểm Sự Kiện.");
        }
        if (EventManager.NEW_YEAR_FIREWORK) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN PHÁO HOA LỄ HỘI:\n"
                    + "  1. Pháo Bông Thường (ID 1575):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang để đốt pháo hoa trên bản đồ, nhận ngay 500.000 - 2.000.000 Vàng + 1 Điểm Sự Kiện.\n"
                    + "  2. Pháo Bông VIP (ID 1576):\n"
                    + "     • Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                    + "     • Cách dùng: Bấm 'Sử dụng' trực tiếp trong Hành trang để bắn pháo hoa VIP, nhận ngay 2.000.000 - 5.000.000 Vàng + 10 - 30 Ngọc Xanh + 1 Điểm Sự Kiện.");
        }
        if (EventManager.TOP_UP) {
            activeEvents.add("★ [ĐANG DIỄN RA] SỰ KIỆN KHUYẾN MÃI NẠP (TOP UP):\n"
                    + "  • Nhận thêm ưu đãi và quà mốc nạp tại NPC Santa (Đảo Kame).");
        }

        liveSb.append("=== SỰ KIỆN & HƯỚNG DẪN VẬT PHẨM SỰ KIỆN ĐANG CHẠY ===\n");
        if (activeEvents.isEmpty()) {
            liveSb.append("Hiện đang tạm nghỉ giữa các đợt sự kiện lễ hội. Các hoạt động săn Boss, Doanh Trại, Bản Đồ Kho Báu, Khí Gas, Mabư 12h/14h, Sao Đen 20h, Frieza Vàng 21h vẫn hoạt động bình thường!\n");
        } else {
            for (String ev : activeEvents) {
                liveSb.append(ev).append("\n\n");
            }
            liveSb.append("💡 Lưu ý Điểm Sự Kiện: Mỗi lần sử dụng Vật Phẩm Sự Kiện hoặc hạ Boss Sự Kiện sẽ tích lũy Điểm Sự Kiện. Tích đủ 2.000 điểm có thể đến gặp NPC Quy Lão Kame (Đảo Kame) để đổi thưởng lớn!\n");
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

        entries.add(new String[]{"Sự Kiện & VPSK Đang Chạy", liveSb.toString().trim()});

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
