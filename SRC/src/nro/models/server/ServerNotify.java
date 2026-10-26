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
                    String activeBanner = getActiveEventBannerText();
                    if (!activeBanner.isEmpty()) {
                        ThongBao(activeBanner);
                    } else {
                        ThongBao("Chào mừng bạn đã đến server Ngọc Rồng VIAM");
                    }
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

    public String getActiveEventBannerText() {
        List<String> names = new ArrayList<>();
        if (EventManager.HALLOWEEN) {
            names.add("Halloween");
        }
        if (EventManager.CHRISTMAS) {
            names.add("Giáng Sinh (Noel)");
        }
        if (EventManager.LUNNAR_NEW_YEAR) {
            names.add("Tết Nguyên Đán");
        }
        if (EventManager.TRUNG_THU) {
            names.add("Tết Trung Thu");
        }
        if (EventManager.HUNG_VUONG) {
            names.add("Giỗ Tổ Hùng Vương");
        }
        if (EventManager.INTERNATIONAL_WOMANS_DAY) {
            names.add("Quốc Tế Phụ Nữ 8/3");
        }
        if (EventManager.SUMMER_NUOC_MIA) {
            names.add("Mùa Hè Nước Mía");
        }
        if (EventManager.CHILDREN_DAY) {
            names.add("Quốc Tế Thiếu Nhi 1/6");
        }
        if (EventManager.COOKING_BANH) {
            names.add("Nấu Bánh Chưng - Bánh Dầy");
        }
        if (EventManager.NEW_YEAR_FIREWORK) {
            names.add("Pháo Hoa Lễ Hội");
        }
        if (EventManager.TOP_UP) {
            names.add("Khuyến Mãi Nạp (Top Up)");
        }
        if (names.isEmpty()) {
            return "";
        }
        return "[SỰ KIỆN ĐANG DIỄN RA] " + String.join(" | ", names)
                + " (Rớt VPSK: " + EventManager.EVENT_DROP_RATE + "%). Xem hướng dẫn cách chơi & dùng vật phẩm tại Menu -> Chức năng -> Thông báo!";
    }

    public void sendActiveBossesToPlayer(Player player) {
        if (player == null || player.session == null) {
            return;
        }
        try {
            String activeBanner = getActiveEventBannerText();
            if (!activeBanner.isEmpty()) {
                Message msgEvent = new Message(93);
                msgEvent.writer().writeUTF(activeBanner);
                player.sendMessage(msgEvent);
                msgEvent.cleanup();
            }
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
        String dropRateInfo = "Tỷ lệ rơi Vật Phẩm Sự Kiện từ quái: " + EventManager.EVENT_DROP_RATE + "% (rơi tại tất cả các bản đồ).\n";
        String pointShopInfo = "\n[3] ĐỔI ĐIỂM SỰ KIỆN LẤY QUÀ VIP:\n"
                + "• Mỗi lần sử dụng Vật Phẩm Sự Kiện hoặc tiêu diệt Boss Sự Kiện sẽ cộng Điểm Sự Kiện.\n"
                + "• Đem Điểm Sự Kiện đến gặp NPC Quy Lão Kame tại Đảo Kame (Map 5) -> chọn 'Đổi điểm sự kiện' (200 điểm/lần) để đổi Phụ kiện Đeo Lưng & Vật phẩm Sự Kiện VIP!";

        // Chỉ hiển thị chi tiết các Sự Kiện ĐANG CHẠY HIỆN TẠI ngay đầu bảng Thông Báo
        if (EventManager.HALLOWEEN) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Halloween",
                "★ SỰ KIỆN ĐANG DIỄN RA: LỄ HỘI HALLOWEEN ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Hoạt động 1: Đánh quái trên tất cả các map thu thập trọn bộ Bí Ngô 1 Sao -> 7 Sao (ID 702 - 708) để gọi Rồng Xương Halloween.\n"
                + "• Hoạt động 2: Săn 3 Boss Halloween gồm Bí Ma, Ma Trơi, Dơi xuất hiện ngẫu nhiên trên các bản đồ (gây hiệu ứng hóa Bộ Xương, Ma Trơi, Dơi cho người chơi xung quanh).\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Bộ Bí Ngô 1 Sao -> 7 Sao (ID 702, 703, 704, 705, 706, 707, 708):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các map hoặc hạ Boss Bí Ma, Ma Trơi, Dơi.\n"
                + "   - Cách sử dụng: Gom đủ 7 quả Bí Ngô từ 1 Sao đến 7 Sao trong Hành trang -> bấm 'Sử dụng' vào bất kỳ quả nào để triệu hồi Rồng Xương Halloween ước 1 trong 5 điều ước:\n"
                + "     + (1) Đổi Chiêu 3, 4 Đệ Tử | (2) Thay đổi Nội Tại (yêu cầu 10 Tỷ SM) | (3) Cải trang Siêu Thần 90 ngày | (4) Cải trang Black Gohan Rose 90 ngày | (5) Tăng 10% HP, KI, SĐ trong 30 phút.\n"
                + "2. Bí Ngô (ID 585), Kẹo Bàn Tay (ID 901), Bánh Gato Nhện (ID 902):\n"
                + "   - Cách kiếm: Tiêu diệt Boss Bí Ma (rơi Bí Ngô 585), Boss Ma Trơi (rơi Kẹo Bàn Tay 901), Boss Dơi (rơi Bánh Gato Nhện 902) + 2 Điểm Sự Kiện/Boss.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang để hóa giải hiệu ứng biến hình Halloween và nhận ngẫu nhiên:\n"
                + "     + 2.000.000 - 8.000.000 Vàng, 10 - 30 Ngọc Xanh, 5 - 20 Hồng Ngọc.\n"
                + "     + Cuồng Nộ Cấp 2 (1150), Bổ Huyết Cấp 2 (1152), Bổ Khí Cấp 2 (1151).\n"
                + "     + Cải trang Hóa Trang Halloween (586, 587, 588, 739) chỉ số 18-25% SĐ/HP/KI.\n"
                + "     + Cộng ngay +1 Điểm Sự Kiện mỗi lần sử dụng."
                + pointShopInfo
            });
        }

        if (EventManager.CHRISTMAS) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Giáng Sinh",
                "★ SỰ KIỆN ĐANG DIỄN RA: LỄ HỘI GIÁNG SINH (NOEL) ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Hoạt động 1: Đánh quái nhặt Tất, Vớ Giáng Sinh (649) và Hộp Quà Giáng Sinh (648).\n"
                + "• Hoạt động 2: Săn Boss Ông Già Noel xuất hiện trên các bản đồ tự nhiên, tự động rải Hộp Quà Giáng Sinh (648) cho người chơi nhặt.\n"
                + "• Hoạt động 3: Thu thập đủ 7 viên Ngọc Rồng Băng (925 - 931) để triệu hồi Rồng Thần Băng Giáng Sinh.\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Tất, Vớ Giáng Sinh (ID 649):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                + "   - Cách sử dụng (2 cách):\n"
                + "     + Cách 1: Chỉ cần giữ Tất, Vớ (649) trong Hành trang khi đánh quái sẽ kích hoạt rơi trực tiếp Hộp Quà Giáng Sinh (648).\n"
                + "     + Cách 2: Gom đủ x10 Tất, Vớ (649) rồi bấm 'Sử dụng' trực tiếp trong Hành trang để ghép ngay thành 1 Hộp Quà Giáng Sinh (648).\n"
                + "2. Hộp Quà Giáng Sinh (ID 648):\n"
                + "   - Cách kiếm: Đánh quái khi có Tất vớ, ghép từ 10 Tất vớ, hoặc nhặt từ Boss Ông Già Noel.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang (cần 1 ô trống) để mở ra:\n"
                + "     + Cải trang Noel (387-393), Pet Tuần Lộc Nhí (936), Cờ Cây Thông (822), Ván bay Xe Trượt Tuyết (746).\n"
                + "     + Ngọc Rồng Băng 1 Sao -> 7 Sao (925 - 931), Kẹo Giáng Sinh (533), Ngọc Xanh, Sao Pha Lê, Đá Nâng Cấp + 1 Điểm Sự Kiện.\n"
                + "3. Kẹo Giáng Sinh (ID 533):\n"
                + "   - Cách kiếm: Mở từ Hộp Quà Giáng Sinh (648).\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang nhận 1 - 5Tr Vàng, 5 - 15 Ngọc Xanh hoặc 5 - 15 Hồng Ngọc + 1 Điểm Sự Kiện.\n"
                + "4. Bộ Ngọc Rồng Băng 1 Sao -> 7 Sao (ID 925, 926, 927, 928, 929, 930, 931):\n"
                + "   - Cách kiếm: Mở từ Hộp Quà Giáng Sinh (648).\n"
                + "   - Cách sử dụng: Gom đủ 7 viên Ngọc Rồng Băng (1 -> 7 sao) trong Hành trang rồi bấm 'Sử dụng' vào bất kỳ viên nào để gọi Rồng Thần Băng ước 1 trong 5 điều ước riêng biệt:\n"
                + "     + (1) Cải trang Noel VIP (35% SĐ/HP/KI, Kháng lạnh, HSD 30-90 ngày) | (2) +20.000 Hồng Ngọc & +10 Điểm Sự Kiện | (3) +1.000 Ngọc Xanh & 10 Thỏi Vàng | (4) Đổi ngẫu nhiên Chiêu 2, 3, 4 Đệ Tử | (5) Quần đang mặc lên 1 cấp (tối đa cấp 7) & +10% HP, KI, SĐ (30 phút).\n"
                + "5. Tuần Lộc Nhí (ID 936):\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trong Hành trang để trang bị làm Pet đi theo tăng chỉ số."
                + pointShopInfo
            });
        }

        if (EventManager.LUNNAR_NEW_YEAR) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Tết Nguyên Đán",
                "★ SỰ KIỆN ĐANG DIỄN RA: TẾT NGUYÊN ĐÁN ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Hoạt động 1: Đánh quái trên tất cả các bản đồ nhặt Bao Lì Xì (ID 717).\n"
                + "• Hoạt động 2: Săn và dẫn Boss Lân Con trên các map về giao cho NPC Quy Lão Kame tại Đảo Kame (Map 5).\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Bao Lì Xì (ID 717):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang nhận ngẫu nhiên:\n"
                + "     + 2.000.000 - 10.000.000 Vàng, 10 - 50 Ngọc Xanh, 5 - 25 Hồng Ngọc.\n"
                + "     + Thỏi Vàng (457), Cỏ 4 Lá (1230), Đá Ngũ Sắc (674), Đá Bảo Vệ (987) hoặc Cuồng Nộ/Bổ Huyết/Bổ Khí Cấp 2.\n"
                + "     + Cộng ngay +1 Điểm Sự Kiện mỗi lần mở Bao Lì Xì.\n"
                + "2. Cách chơi hoạt động Dẫn Boss Lân Con:\n"
                + "   - Bước 1: Tìm Boss Lân Con trên các bản đồ, đánh Lân Con đến khi cạn HP (hoặc chat 'thang'), Lân Con sẽ đi theo sau bạn.\n"
                + "   - Bước 2: Dẫn Lân Con chạy bộ qua các điểm chuyển map về đến Đảo Kame (Map 5) (Lưu ý: Không dùng Capsule dịch chuyển vì Lân Con sẽ bị lạc).\n"
                + "   - Bước 3: Bấm vào NPC Quy Lão Kame (Đảo Kame) -> chọn 'Giao Lân con' để nhận ngay Ván Bay / Thú Cưỡi Tết VIP (733, 734, 743, 849, 920) tăng HP/KI/SĐ + 5 Điểm Sự Kiện!"
                + pointShopInfo
            });
        }

        if (EventManager.TRUNG_THU) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Tết Trung Thu",
                "★ SỰ KIỆN ĐANG DIỄN RA: TẾT TRUNG THU ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Hoạt động 1: Đánh quái tại tất cả các bản đồ nhặt Hộp Quà 5 Sao / Trung Thu (ID 736).\n"
                + "• Hoạt động 2: Săn bộ 3 Boss Trung Thu gồm Khỉ Đột, Nguyệt Thần và Nhật Thần xuất hiện trên các bản đồ.\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Hộp Quà 5 Sao / Hộp Quà Trung Thu (ID 736):\n"
                + "   - Cách kiếm: Đánh quái trên tất cả các map hoặc tiêu diệt Boss Khỉ Đột.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang (cần ít nhất 2 ô trống) để mở ra:\n"
                + "     + Ngọc Xanh, Đá Bảo Vệ, Ván bay/Phụ kiện/Cải trang Lễ Hội, Cuồng Nộ/Bổ Huyết/Bổ Khí + 1 Điểm Sự Kiện.\n"
                + "2. Đuôi Khỉ (ID 1045):\n"
                + "   - Cách kiếm: Tiêu diệt Boss Khỉ Đột (rơi Đuôi Khỉ 1045 + Hộp Quà 736 + 5.000 Hồng Ngọc + 2 Điểm Sự Kiện).\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang để kích hoạt buff tăng +10% Sức Đánh và +10% HP trong 10 phút.\n"
                + "3. Cải Trang Nguyệt Thần (ID 1276) & Cải Trang Nhật Thần (ID 1277):\n"
                + "   - Cách kiếm: Tiêu diệt Boss Nguyệt Thần và Boss Nhật Thần (nhận Cải trang 1276/1277 chỉ số 22-40% SĐ/HP/KI + 5 Điểm Sự Kiện).\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trong Hành trang để mặc Cải trang."
                + pointShopInfo
            });
        }

        if (EventManager.HUNG_VUONG) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Giỗ Tổ Hùng Vương",
                "★ SỰ KIỆN ĐANG DIỄN RA: GIỖ TỔ HÙNG VƯƠNG ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Hoạt động 1: Đánh quái nhặt Dưa Hấu (569) & Tem Mai An Tiêm (1558), hoặc trồng Dưa Hấu tại NPC Vua Hùng.\n"
                + "• Hoạt động 2: Thu thập bộ Sính Lễ: Ngà Voi (1220), Cựa Gà (1221), Hồng Mao (1222) từ quái và Boss Sơn Tinh - Thủy Tinh.\n"
                + "• Hoạt động 3: Săn Boss Sơn Tinh & Boss Thủy Tinh nhận Cải trang + Sính lễ + Điểm Sự Kiện.\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Dưa Hấu (ID 569) & Tem Chứng Nhận Mai An Tiêm (ID 1558):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các map hoặc trồng dưa tại NPC Vua Hùng.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp vào Dưa Hấu (569) hoặc Tem (1558) trong Hành trang (tiêu hao 1 Dưa Hấu + 1 Tem) nhận ngay 10 - 25 Ngọc Xanh + 1 Điểm Sự Kiện; HOẶC gặp NPC Vua Hùng để đổi gói lớn (tối đa 250 Ngọc Xanh).\n"
                + "2. Bộ Sính Lễ: Ngà Voi (ID 1220), Cựa Gà (ID 1221), Hồng Mao (ID 1222):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các map hoặc tiêu diệt Boss Sơn Tinh, Thủy Tinh (rơi Cải trang 421/422 + 1-3 Sính lễ + 5 Điểm Sự Kiện).\n"
                + "   - Cách sử dụng: Gom đủ 9 Ngà Voi (1220) + 9 Cựa Gà (1221) + 9 Hồng Mao (1222) + 1.000.000 Vàng -> bấm 'Sử dụng' trực tiếp vào 1 trong 3 vật phẩm trong Hành trang (hoặc dâng tại NPC Vua Hùng) để nhận Hộp Quà Sự Kiện (1776) hoặc Hộp Quà Sự Kiện VIP (1777) + 1 Điểm Sự Kiện.\n"
                + "3. Hộp Quà Sự Kiện (ID 1776) & Hộp Quà Sự Kiện VIP (ID 1777):\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang:\n"
                + "     + Hộp Quà Sự Kiện (1776): Mở ra Ngọc Rồng 3-7 Sao hoặc Phụ kiện Búa Sơn Tinh (1772), Bát Thủy Tinh (1773) + 1 Điểm Sự Kiện.\n"
                + "     + Hộp Quà Sự Kiện VIP (1777): Mở ra Cải trang VIP (1761-1764, 1731-1732) chỉ số 15-26% SĐ/HP/KI + 1 Điểm Sự Kiện."
                + pointShopInfo
            });
        }

        if (EventManager.COOKING_BANH) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Nấu Bánh Chưng - Bánh Dầy",
                "★ SỰ KIỆN ĐANG DIỄN RA: NẤU BÁNH CHƯNG - BÁNH DẦY ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Hoạt động 1: Đánh quái trên tất cả các bản đồ nhặt 6 loại Nguyên Liệu Nấu Bánh: Chả Lụa (1544), Muối Tiêu (1545), Cơm Nếp (1546), Bột Gạo (1547), Đậu Xanh (1548), Thịt Tươi (1549).\n"
                + "• Hoạt động 2: Nấu Bánh Chưng Lang Liêu (1556) và Bánh Dầy (1542) trực tiếp trong Hành trang hoặc tại NPC Nồi Bánh.\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Công thức & Cách nấu Bánh Chưng Lang Liêu (ID 1556):\n"
                + "   - Nguyên liệu cần: x10 Cơm Nếp (1546) + x10 Đậu Xanh (1548) + x10 Thịt Tươi (1549) + 5.000.000 Vàng (nếu nấu tại Nồi Bánh).\n"
                + "   - Cách nấu: Bấm 'Sử dụng' trực tiếp vào Đậu Xanh (1548), Thịt Tươi (1549) hoặc Cơm Nếp (1546) ngay trong Hành trang (miễn phí Vàng, nhận ngay 1 Bánh Chưng 1556); HOẶC nấu tại NPC Nồi Bánh.\n"
                + "2. Công thức & Cách nấu Bánh Dầy (ID 1542):\n"
                + "   - Nguyên liệu cần: x10 Cơm Nếp (1546) + x5 Bột Gạo (1547) + x5 Muối Tiêu (1545) + x1 Chả Lụa (1544).\n"
                + "   - Cách nấu: Bấm 'Sử dụng' trực tiếp vào Bột Gạo (1547), Muối Tiêu (1545) hoặc Chả Lụa (1544) ngay trong Hành trang (nhận ngay 1 Bánh Dầy 1542); HOẶC nấu tại NPC Nồi Bánh.\n"
                + "3. Cách sử dụng Bánh Dầy (ID 1542) & Bánh Chưng Lang Liêu (ID 1556):\n"
                + "   - Cách 1: Bấm 'Sử dụng' trực tiếp vào Bánh Dầy (1542) hoặc Bánh Chưng (1556) trong Hành trang để nhận ngay Hộp Quà Sự Kiện (1776) hoặc Hộp Quà Sự Kiện VIP (1777) + 1 Điểm Sự Kiện.\n"
                + "   - Cách 2: Gom đủ 9 Bánh Chưng (1556) + 9 Bánh Dầy (1542) + 1.000.000 Vàng đem dâng tại NPC Vua Hùng."
                + pointShopInfo
            });
        }

        if (EventManager.INTERNATIONAL_WOMANS_DAY) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Quốc Tế Phụ Nữ 8/3",
                "★ SỰ KIỆN ĐANG DIỄN RA: QUỐC TẾ PHỤ NỮ 8/3 ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Đánh quái trên tất cả các bản đồ thu thập Bông Hoa (ID 589) và Hoa Hồng (ID 610) để đổi quà sự kiện.\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Bông Hoa (ID 589) & Hoa Hồng (ID 610):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                + "   - Cách sử dụng:\n"
                + "     + Cách 1: Gom đủ x10 Bông Hoa (589) hoặc x10 Hoa Hồng (610) -> bấm 'Sử dụng' trực tiếp trong Hành trang để nhận ngẫu nhiên: 10 - 30 Ngọc Xanh, 5 - 15 Hồng Ngọc, 2 - 5Tr Vàng, Cỏ 4 Lá (1230), Đá Bảo Vệ (987) hoặc Hộp Quà Sự Kiện (1776) / Hộp Quà VIP (1777) + 1 Điểm Sự Kiện.\n"
                + "     + Cách 2: Đem Bông Hoa đến gặp NPC Lý Tiểu Nương để đổi quà sự kiện."
                + pointShopInfo
            });
        }

        if (EventManager.SUMMER_NUOC_MIA) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Mùa Hè Nước Mía",
                "★ SỰ KIỆN ĐANG DIỄN RA: MÙA HÈ NƯỚC MÍA ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Hoạt động 1: Đánh quái trên tất cả các bản đồ nhặt Khúc Mía (1612), Nước Đá (1613) và Kem Trái Cây (1609).\n"
                + "• Hoạt động 2: Ép Ly Nước Mía tăng 10% chỉ số và ăn Kem Trái Cây đua Top tại NPC ChiChi.\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Khúc Mía (ID 1612) & Nước Đá (ID 1613):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                + "   - Cách sử dụng: Gom đủ x5 Khúc Mía (1612) + x2 Nước Đá (1613) + 5.000.000 Vàng -> bấm 'Sử dụng' trực tiếp vào Khúc Mía hoặc Nước Đá trong Hành trang (hoặc gặp NPC Xe Nước Mía) để ép thành 1 Ly Nước Mía (1614, 1615 hoặc 1616) + 1 Điểm Sự Kiện.\n"
                + "2. Ly Nước Mía Nhỏ / Vừa / Lớn (ID 1614, 1615, 1616):\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang để uống, kích hoạt buff tăng +10% HP, +10% KI, +10% Sức Đánh trong 10 - 30 phút.\n"
                + "3. Kem Trái Cây (ID 1609):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang (cần 1 ô trống) để nhận Vật phẩm / Cải trang Mùa Hè VIP (1803, 1804, 1806), cộng +1 điểm Kem Trái Cây đua Top tại NPC ChiChi & +1 Điểm Sự Kiện."
                + pointShopInfo
            });
        }

        if (EventManager.CHILDREN_DAY) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Thiếu Nhi 1/6",
                "★ SỰ KIỆN ĐANG DIỄN RA: QUỐC TẾ THIẾU NHI 1/6 ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Đánh quái trên tất cả các bản đồ nhặt Hộp Quà Thiếu Nhi (ID 1608) mở Cải trang & Phụ kiện VIP, tích điểm đua Top tại NPC ChiChi.\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Hộp Quà Thiếu Nhi (ID 1608):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang (cần 1 ô trống) để mở ngẫu nhiên:\n"
                + "     + Cải trang & Phụ kiện Thiếu Nhi VIP (1807, 1599, 1600, 1601, 1602) chỉ số 15-26% Sức Đánh, HP, KI.\n"
                + "     + Cộng +1 điểm Quà Thiếu Nhi đua Top tại NPC ChiChi & +1 Điểm Sự Kiện."
                + pointShopInfo
            });
        }

        if (EventManager.NEW_YEAR_FIREWORK) {
            entries.add(new String[]{
                "[ĐANG MỞ] Sự Kiện Pháo Hoa Lễ Hội",
                "★ SỰ KIỆN ĐANG DIỄN RA: PHÁO HOA LỄ HỘI ★\n"
                + "• " + dropRateInfo + "\n"
                + "[1] SỰ KIỆN HIỆN TẠI ĐANG CÓ GÌ?\n"
                + "• Đánh quái trên tất cả các bản đồ nhặt Pháo Bông Thường (ID 1575) và Pháo Bông VIP (ID 1576).\n\n"
                + "[2] CÁCH CHƠI & CÁCH SỬ DỤNG TỪNG VẬT PHẨM SỰ KIỆN:\n"
                + "1. Pháo Bông Thường (ID 1575):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang để bắn pháo hoa rực rỡ trên bản đồ, nhận ngay 500.000 - 2.000.000 Vàng + 1 Điểm Sự Kiện.\n"
                + "2. Pháo Bông VIP (ID 1576):\n"
                + "   - Cách kiếm: Đánh quái tại tất cả các bản đồ.\n"
                + "   - Cách sử dụng: Bấm 'Sử dụng' trực tiếp trong Hành trang để bắn pháo hoa VIP, nhận ngay 2.000.000 - 5.000.000 Vàng + 10 - 30 Ngọc Xanh + 1 Điểm Sự Kiện."
                + pointShopInfo
            });
        }

        if (EventManager.TOP_UP) {
            entries.add(new String[]{
                "[ĐANG MỞ] Khuyến Mãi Nạp (Top Up)",
                "★ SỰ KIỆN ĐANG DIỄN RA: KHUYẾN MÃI NẠP (TOP UP) ★\n"
                + "• Khi quy đổi và nạp tại Server trong thời gian sự kiện sẽ nhận thêm ưu đãi và quà mốc nạp tại NPC Santa (Đảo Kame)."
            });
        }

        // Mục Tổng quan Tỷ lệ Server & Giftcode đang mở
        StringBuilder liveSb = new StringBuilder();
        liveSb.append("• Thời gian Server đã hoạt động: ").append(ServerManager.getFormattedUptimeHours()).append(" (Cộng dồn liên tục)\n");
        liveSb.append("• Tỷ lệ TNSM toàn Server: x").append(Manager.RATE_EXP_SERVER).append("\n");
        liveSb.append("• Vàng rơi từ quái: ").append((EventManager.DOUBLE_GOLD || TimeUtil.isWeekend()) ? "ĐANG X2 VÀNG TOÀN SERVER" : "Mặc định (Cuối tuần tự động X2)").append("\n");
        liveSb.append("• Tỷ lệ rơi Vật Phẩm Sự Kiện: ").append(EventManager.EVENT_DROP_RATE).append("%\n\n");

        String banner = getActiveEventBannerText();
        if (banner.isEmpty()) {
            liveSb.append("• Trạng thái Sự Kiện: Hiện không có sự kiện lễ hội nào đang bật. Các hoạt động săn Boss, Doanh Trại, Bản Đồ Kho Báu, Khí Gas, Mabư 12h/14h, Sao Đen 20h, Frieza Vàng 21h vẫn diễn ra bình thường!\n");
        } else {
            liveSb.append("• ").append(banner).append("\n");
            liveSb.append("• Xem chi tiết cách chơi và cách sử dụng từng vật phẩm ở ngay Tab Sự Kiện bên trên!\n");
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

        entries.add(new String[]{"Tỷ Lệ Server & Giftcode", liveSb.toString().trim()});

        // 2. Quy luật Giải đấu PK Bang Hội & Hệ thống Nội Tại / Hào Quang Bang
        entries.add(new String[]{
            "PK Bang Hội & Nội Tại Bang",
            "★ ĐẠI HỘI VÕ THUẬT BANG HỘI (PK BANG HỘI) ★\n"
            + "• Cách tham gia: Gặp NPC Quy Lão Kame tại Đảo Kame (Map 5) -> chọn 'PK Bang Hội' để dịch chuyển tới Võ Đài Bang Hội (Map 134).\n\n"
            + "[1] QUY LUẬT & THỂ THỨC THI ĐẤU:\n"
            + "• Đăng ký & Xếp đội hình: Chủ Bang gặp Trọng Tài tại Võ Đài để chọn danh sách và xếp thứ tự thành viên lên sàn (#1 -> #2 -> #3...).\n"
            + "• Lệ phí đăng ký: 20 Thỏi vàng hoặc 1.000 Hồng ngọc. Khi có từ 2 Bang đăng ký, Trọng Tài sẽ tự động ghép cặp ngẫu nhiên.\n"
            + "• Cường hóa trên đài: Đấu thủ bước lên sàn đấu được cường hóa x100 HP & KI, x5 Sức đánh.\n"
            + "• Luật công bằng: Cấm ăn Đậu thần & cấm hồi sinh tại chỗ trên sàn đấu. Giới hạn 5 phút đếm ngược mỗi hiệp (ai chết hoặc rơi khỏi đài sẽ thua hiệp đó, hết 5 phút tính theo %HP còn lại).\n"
            + "• Phần thưởng chung cuộc: Bang chiến thắng nhận hoàn trả 150% lệ phí (30 Thỏi vàng hoặc 1.500 Hồng ngọc) + 10 Điểm Nội Tại Bang (Bang thua nhận +2 Điểm Nội Tại Bang khuyến khích).\n\n"
            + "[2] CƠ CHẾ TRẢM TƯỚNG & THƯỞNG NÓNG LIÊN TRẢM:\n"
            + "• Trảm Tướng: Đấu thủ thắng 1 hiệp và trụ lại trên sàn được hồi +25% HP & 25% KI để nghênh chiến người tiếp theo.\n"
            + "• Chuỗi Liên Trảm:\n"
            + "  - Hạ 2 người liên tiếp (Song Sát): Thưởng nóng +10 Điểm Sự Kiện, +10 Điểm Săn Boss.\n"
            + "  - Hạ 3 người liên tiếp (Tam Liên Trảm): Thưởng nóng +100 Hồng ngọc, +25 Điểm SK, +25 Điểm Săn Boss & +3 Điểm Nội Tại Bang.\n"
            + "  - Hạ 4+ người liên tiếp (Độc Cô Cầu Bại): Thưởng nóng +200 Hồng ngọc, +50 Điểm SK, +50 Điểm Săn Boss & +5 Điểm Nội Tại Bang.\n\n"
            + "[3] THẺ CHIẾN THUẬT CỦA BANG CHỦ & ĐẶT CƯỢC KHÁN ĐÀI:\n"
            + "• Thẻ Chiến Thuật (1 lần duy nhất / trận tại Trọng Tài):\n"
            + "  - Tiếp Sức Bang Hội: Hồi ngay +35% HP & KI tối đa và xóa toàn bộ hồi chiêu cho đấu thủ đang trên sàn.\n"
            + "  - Đổi Tướng Chiến Thuật: Rút đấu thủ đang bất lợi trên sàn xuống cuối hàng chờ và cử người kế tiếp lên thay.\n"
            + "• Cổ Vũ / Đặt Cược Khán Giả: Người chơi đứng dưới đài có thể cược 5 Thỏi vàng hoặc 200 Hồng ngọc tại Trọng Tài trước khi Hiệp 1 kết thúc. Dự đoán đúng Bang thắng nhận 180% (9 Thỏi vàng hoặc 360 Hồng ngọc)!\n\n"
            + "[4] ĐIỂM NỘI TẠI BANG, HÀO QUANG BANG & TOP 1 ĐỆ NHẤT BANG:\n"
            + "• Nâng Nội Tại Bang (tại Trọng Tài): Dùng Điểm Nội Tại Bang để nâng vĩnh viễn cho toàn bộ thành viên: HP Gốc (tối đa +1Tr HP), KI Gốc (tối đa +1Tr KI), SĐ Gốc (tối đa +50k SĐ), %HP (+30%), %KI (+30%), %SĐ (+30%), %TNSM & Vàng (+75%), %Chí Mạng (+10%).\n"
            + "• Hào Quang Bang Hội (5 Cấp): Mở khóa vòng Hào Quang rực rỡ dưới chân toàn bộ thành viên Bang và cộng thêm từ +2% đến +12% HP, KI, Sức đánh.\n"
            + "• Danh hiệu Thiên Hạ Đệ Nhất Bang (Top 1 BXH PK Bang): Toàn bộ thành viên Bang giữ Top 1 trận thắng PK Bang Hội được cộng thêm +5% HP, +5% KI, +5% Sức đánh và +10% TNSM!"
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
