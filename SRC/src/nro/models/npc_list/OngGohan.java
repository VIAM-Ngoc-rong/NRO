package nro.models.npc_list;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nro.models.consts.ConstNpc;
import nro.models.data.LocalManager;
import nro.models.database.MrBlue;
import nro.models.item.Item;
import nro.models.map.service.ChangeMapService;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.task.SubTaskMain;
import nro.models.task.TaskMain;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

public class OngGohan extends Npc {

    public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Con cố gắng theo Quy Lão Kame học thành tài, đừng lo lắng cho ta.\n(Gõ 'auto' vào chat để mở nhanh Menu Tiện Ích ở mọi nơi)",
                        "Nhận\n200k Ngọc",
                        "Nhận Đậu\n10 x99",
                        "Nhận Quà\nTân Thủ",
                        "Rương SKH\nTân Thủ",
                        "Bảng\nXếp Hạng",
                        "Tiện ích\nTự động",
                        "Nhập\nGiftcode");
            }
        }
    }

    protected void openMenuBangXepHang(Player player) {
        this.createOtherMenu(player, 9905,
                "|7|=== BẢNG XẾP HẠNG & PHẦN THƯỞNG TOP NGÀY ===\n"
                + "|2|Vinh danh những chiến binh xuất sắc nhất toàn máy chủ!\n"
                + "|1|Đặc quyền TOP 1 mỗi bảng hằng ngày: Nhận 1 Danh Hiệu Top 1 (Tăng +5% HP, +5% SĐ, +5% KI - HSD 1 ngày)!\n"
                + "|0|Hãy chọn hạng mục con muốn xem hoặc nhận thưởng:",
                "Top\nSức Mạnh",
                "Top\nNhiệm Vụ",
                "Top Đòn Đánh\nCao Nhất",
                "Nhận Thưởng\nTop 1 Ngày",
                "Đóng");
    }

    private static class HomeTopRow {
        int id;
        String name;
        byte gender;
        short head;
        short body;
        short leg;
        long power;
        long tiemNang;
        int taskId;
        int taskIndex;
        int taskCount;
        long maxDameHit;
        long curDame;
    }

    private Map<Integer, HomeTopRow> loadAllHomeTopRows() {
        Map<Integer, HomeTopRow> rowMap = new HashMap<>();
        try (Connection con = LocalManager.getConnection()) {
            try (PreparedStatement psAlter = con.prepareStatement(
                    "ALTER TABLE player ADD COLUMN max_dame_hit BIGINT NOT NULL DEFAULT 0")) {
                psAlter.executeUpdate();
            } catch (Exception ignored) {
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id, name, gender, head, data_point, data_task, max_dame_hit FROM player");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HomeTopRow row = new HomeTopRow();
                    row.id = rs.getInt("id");
                    row.name = rs.getString("name");
                    row.gender = rs.getByte("gender");
                    row.head = rs.getShort("head");
                    try {
                        row.maxDameHit = rs.getLong("max_dame_hit");
                    } catch (Exception ignored) {
                    }
                    try {
                        JSONArray dp = (JSONArray) JSONValue.parse(rs.getString("data_point"));
                        if (dp != null && dp.size() > 7) {
                            row.power = Long.parseLong(String.valueOf(dp.get(1)));
                            row.tiemNang = Long.parseLong(String.valueOf(dp.get(2)));
                            row.curDame = Long.parseLong(String.valueOf(dp.get(7)));
                        }
                    } catch (Exception ignored) {
                    }
                    try {
                        JSONArray dt = (JSONArray) JSONValue.parse(rs.getString("data_task"));
                        if (dt != null && dt.size() >= 3) {
                            row.taskId = Integer.parseInt(String.valueOf(dt.get(0)));
                            row.taskIndex = Integer.parseInt(String.valueOf(dt.get(1)));
                            row.taskCount = Integer.parseInt(String.valueOf(dt.get(2)));
                        }
                    } catch (Exception ignored) {
                    }
                    if (row.maxDameHit < row.curDame) {
                        row.maxDameHit = row.curDame;
                    }
                    rowMap.put(row.id, row);
                }
            }
        } catch (Exception e) {
            Logger.logException(OngGohan.class, e, "Lỗi đọc dữ liệu BXH tại nhà");
        }

        for (Player live : new ArrayList<>(Client.gI().getPlayers())) {
            if (live == null || !live.isPl() || live.isBot || live.nPoint == null) {
                continue;
            }
            int pid = (int) live.id;
            HomeTopRow row = rowMap.computeIfAbsent(pid, k -> new HomeTopRow());
            row.id = pid;
            row.name = live.name;
            row.gender = live.gender;
            row.head = live.getHead();
            row.body = live.getBody();
            row.leg = live.getLeg();
            row.power = live.nPoint.power;
            row.tiemNang = live.nPoint.tiemNang;
            row.curDame = live.nPoint.dame;
            row.maxDameHit = Math.max(row.maxDameHit, Math.max(live.max_dame_hit, live.nPoint.dame));
            if (live.playerTask != null && live.playerTask.taskMain != null) {
                row.taskId = live.playerTask.taskMain.id;
                row.taskIndex = live.playerTask.taskMain.index;
                if (live.playerTask.taskMain.subTasks != null
                        && row.taskIndex >= 0
                        && row.taskIndex < live.playerTask.taskMain.subTasks.size()) {
                    row.taskCount = live.playerTask.taskMain.subTasks.get(row.taskIndex).count;
                }
            }
        }
        return rowMap;
    }

    private List<HomeTopRow> getSortedTopList(Map<Integer, HomeTopRow> rowMap, int type) {
        List<HomeTopRow> list = new ArrayList<>(rowMap.values());
        switch (type) {
            case 0 -> // Top Sức Mạnh
                list.sort((a, b) -> {
                    int cmp = Long.compare(b.power, a.power);
                    return cmp != 0 ? cmp : Long.compare(b.tiemNang, a.tiemNang);
                });
            case 1 -> // Top Nhiệm Vụ
                list.sort((a, b) -> {
                    int c1 = Integer.compare(b.taskId, a.taskId);
                    if (c1 != 0) return c1;
                    int c2 = Integer.compare(b.taskIndex, a.taskIndex);
                    if (c2 != 0) return c2;
                    int c3 = Integer.compare(b.taskCount, a.taskCount);
                    return c3 != 0 ? c3 : Long.compare(b.power, a.power);
                });
            default -> // Top Đòn Đánh Cao Nhất
                list.sort((a, b) -> {
                    int cmp = Long.compare(b.maxDameHit, a.maxDameHit);
                    if (cmp != 0) return cmp;
                    int cmp2 = Long.compare(b.curDame, a.curDame);
                    return cmp2 != 0 ? cmp2 : Long.compare(b.power, a.power);
                });
        }
        return list;
    }

    private int findPlayerRank(List<HomeTopRow> sortedList, long playerId) {
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).id == (int) playerId) {
                return i + 1;
            }
        }
        return -1;
    }

    private void ensureTopDailyRewardTable(Connection con) {
        try (PreparedStatement ps = con.prepareStatement(
                "CREATE TABLE IF NOT EXISTS top_daily_reward_claims ("
                + "claim_date VARCHAR(16) NOT NULL, "
                + "category INT NOT NULL, "
                + "player_id INT NOT NULL, "
                + "account_id INT NOT NULL, "
                + "rank_pos INT NOT NULL, "
                + "claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (claim_date, category, player_id))")) {
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private String getTodayDateKey() {
        return java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString();
    }

    private boolean hasClaimedTopDailyReward(Connection con, String dateKey, int category, int playerId, int accountId, int rankPos) {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM top_daily_reward_claims WHERE claim_date = ? AND category = ? AND (player_id = ? OR account_id = ? OR rank_pos = ?) LIMIT 1")) {
            ps.setString(1, dateKey);
            ps.setInt(2, category);
            ps.setInt(3, playerId);
            ps.setInt(4, accountId);
            ps.setInt(5, rankPos);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    protected void openMenuThuongTopNgay(Player player) {
        if (player == null || player.getSession() == null) {
            return;
        }
        Map<Integer, HomeTopRow> rowMap = loadAllHomeTopRows();
        int rankPower = findPlayerRank(getSortedTopList(rowMap, 0), player.id);
        int rankTask = findPlayerRank(getSortedTopList(rowMap, 1), player.id);
        int rankDame = findPlayerRank(getSortedTopList(rowMap, 2), player.id);

        String today = getTodayDateKey();
        boolean claimedPower = false;
        boolean claimedTask = false;
        boolean claimedDame = false;
        int accId = player.getSession().userId;

        try (Connection con = LocalManager.getConnection()) {
            ensureTopDailyRewardTable(con);
            claimedPower = hasClaimedTopDailyReward(con, today, 0, (int) player.id, accId, 1);
            claimedTask = hasClaimedTopDailyReward(con, today, 1, (int) player.id, accId, 1);
            claimedDame = hasClaimedTopDailyReward(con, today, 2, (int) player.id, accId, 1);
        } catch (Exception ignored) {
        }

        String stPower = (rankPower == 1)
                ? ("Hạng 1" + (claimedPower ? " [Đã nhận hôm nay]" : " [CÓ THỂ NHẬN]"))
                : (rankPower > 0 ? "Hạng " + rankPower + " (Chỉ thưởng Top 1)" : "Chưa xếp hạng");
        String stTask = (rankTask == 1)
                ? ("Hạng 1" + (claimedTask ? " [Đã nhận hôm nay]" : " [CÓ THỂ NHẬN]"))
                : (rankTask > 0 ? "Hạng " + rankTask + " (Chỉ thưởng Top 1)" : "Chưa xếp hạng");
        String stDame = (rankDame == 1)
                ? ("Hạng 1" + (claimedDame ? " [Đã nhận hôm nay]" : " [CÓ THỂ NHẬN]"))
                : (rankDame > 0 ? "Hạng " + rankDame + " (Chỉ thưởng Top 1)" : "Chưa xếp hạng");

        String info = "|7|=== PHẦN THƯỞNG TOP 1 HẰNG NGÀY (" + today + ") ===\n"
                + "|2|• Top Sức Mạnh: " + stPower + "\n"
                + "• Top Nhiệm Vụ: " + stTask + "\n"
                + "• Top Đòn Đánh: " + stDame + "\n"
                + "|1|--- PHẦN THƯỞNG ĐẶC QUYỀN TOP 1 MỖI NGÀY ---\n"
                + "• Chỉ dành riêng cho người chơi đứng TOP 1 mỗi bảng\n"
                + "• Phần thưởng: 1 Danh Hiệu Top 1 (Tăng +5% HP, +5% SĐ, +5% KI)\n"
                + "• Thời hạn sử dụng: 1 ngày (24 giờ kể từ lúc nhận)";

        this.createOtherMenu(player, 9906, info,
                "Nhận Danh Hiệu\nTop 1 Sức Mạnh",
                "Nhận Danh Hiệu\nTop 1 Nhiệm Vụ",
                "Nhận Danh Hiệu\nTop 1 Đòn Đánh",
                "Quay lại");
    }

    protected void claimDailyTopReward(Player player, int category) {
        if (player == null || player.getSession() == null || category < 0 || category > 2) {
            return;
        }
        String catName = switch (category) {
            case 0 -> "Top Sức Mạnh";
            case 1 -> "Top Nhiệm Vụ";
            default -> "Top Đòn Đánh Cao Nhất";
        };

        Map<Integer, HomeTopRow> rowMap = loadAllHomeTopRows();
        List<HomeTopRow> sorted = getSortedTopList(rowMap, category);
        int rank = findPlayerRank(sorted, player.id);

        if (rank != 1) {
            Service.gI().sendThongBao(player, "Bạn hiện đứng Hạng " + (rank > 0 ? rank : "N/A") + " bảng " + catName + ". Chỉ duy nhất TOP 1 mới được nhận Danh Hiệu hằng ngày!");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Vui lòng dọn trống ít nhất 1 ô hành trang trước khi nhận Danh Hiệu Top 1!");
            return;
        }

        String today = getTodayDateKey();
        int accId = player.getSession().userId;

        try (Connection con = LocalManager.getConnection()) {
            ensureTopDailyRewardTable(con);
            if (hasClaimedTopDailyReward(con, today, category, (int) player.id, accId, 1)) {
                Service.gI().sendThongBao(player, "Hôm nay vị trí TOP 1 bảng " + catName + " đã nhận Danh Hiệu ngày rồi! Hãy quay lại vào ngày mai.");
                return;
            }

            try (PreparedStatement psIns = con.prepareStatement(
                    "INSERT INTO top_daily_reward_claims (claim_date, category, player_id, account_id, rank_pos) VALUES (?, ?, ?, ?, ?)")) {
                psIns.setString(1, today);
                psIns.setInt(2, category);
                psIns.setInt(3, (int) player.id);
                psIns.setInt(4, accId);
                psIns.setInt(5, 1);
                psIns.executeUpdate();
            }
        } catch (Exception e) {
            Logger.logException(OngGohan.class, e, "Lỗi nhận thưởng Top 1 hằng ngày: " + player.name);
            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi nhận thưởng Top 1 ngày, vui lòng thử lại!");
            return;
        }

        long expireTime = System.currentTimeMillis() + 86_400_000L;
        int top1BadgeId = nro.models.player_badges.BagesTemplate.TOP1_BADGE_ID_BASE + category;
        int visualEffId = nro.models.player_badges.BagesTemplate.getVisualEffectId(top1BadgeId);
        int badgeItemId = nro.models.player_badges.BagesTemplate.getTop1BadgeItemId(category);

        if (player.dataBadges != null) {
            for (nro.models.player.BadgesData bd : player.dataBadges) {
                bd.isUse = false;
            }
            nro.models.player.BadgesData existing = null;
            for (nro.models.player.BadgesData bd : player.dataBadges) {
                if (bd.idBadGes == top1BadgeId) {
                    existing = bd;
                    break;
                }
            }
            if (existing != null) {
                existing.timeofUseBadges = expireTime;
                existing.isUse = true;
            } else {
                player.dataBadges.add(new nro.models.player.BadgesData(top1BadgeId, expireTime, true));
            }
        }

        if (player.badges != null && visualEffId != -1) {
            player.badges.idBadges = visualEffId;
            player.badges.timeofUseBadges = expireTime;
            Service.gI().sendBadgesPlayer(player, 5, visualEffId);
        }

        Item badgeItem = ItemService.gI().createNewItem((short) badgeItemId, 1);
        badgeItem.itemOptions.clear();
        badgeItem.itemOptions.add(new Item.ItemOption(50, 5));
        badgeItem.itemOptions.add(new Item.ItemOption(77, 5));
        badgeItem.itemOptions.add(new Item.ItemOption(103, 5));
        badgeItem.itemOptions.add(new Item.ItemOption(93, 1));
        badgeItem.itemOptions.add(new Item.ItemOption(30, 0));
        InventoryService.gI().addItemBag(player, badgeItem);
        InventoryService.gI().sendItemBags(player);

        if (player.nPoint != null) {
            player.nPoint.calPoint();
            Service.gI().point(player);
        }

        String rewardMsg = "Chúc mừng TOP 1 [" + catName + "]! Bạn nhận được Danh Hiệu Top 1 (" + badgeItem.template.name + ": +5% HP, +5% SĐ, +5% KI - HSD 1 ngày) và đã tự động kích hoạt!";
        Service.gI().sendThongBao(player, rewardMsg);
        nro.models.server.ServerNotify.gI().notify("Vinh danh TOP 1 Ngày: Chiến binh [" + player.name
                + "] (Quán quân " + catName + ") vừa nhận Danh Hiệu Top 1 (+5% HP, SĐ, KI) tại nhà!");
        openMenuThuongTopNgay(player);
    }

    protected void showHomeLeaderboard(Player viewer, int type) {
        try {
            Map<Integer, HomeTopRow> rowMap = loadAllHomeTopRows();
            List<HomeTopRow> list = getSortedTopList(rowMap, type);

            int limit = Math.min(50, list.size());
            String title = switch (type) {
                case 0 -> "Top Sức Mạnh";
                case 1 -> "Top Nhiệm Vụ";
                default -> "Top Đòn Đánh Cao Nhất";
            };

            Message msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF(title);
            msg.writer().writeByte(limit);

            for (int i = 0; i < limit; i++) {
                HomeTopRow r = list.get(i);
                Player live = Client.gI().getPlayer(r.id);
                if (live != null) {
                    r.head = live.getHead();
                    r.body = live.getBody();
                    r.leg = live.getLeg();
                } else {
                    Player offPl = MrBlue.loadById(r.id);
                    if (offPl != null) {
                        r.head = offPl.getHead();
                        r.body = offPl.getBody();
                        r.leg = offPl.getLeg();
                        if (r.curDame < offPl.nPoint.dame) {
                            r.curDame = offPl.nPoint.dame;
                        }
                        if (r.maxDameHit < r.curDame) {
                            r.maxDameHit = r.curDame;
                        }
                        offPl.dispose();
                    } else {
                        if (r.head <= 0) {
                            r.head = (short) (r.gender == 0 ? 64 : r.gender == 1 ? 9 : 6);
                        }
                        r.body = (short) (r.gender == 1 ? 59 : 57);
                        r.leg = (short) (r.gender == 1 ? 60 : 58);
                    }
                }

                String planet = r.gender == 0 ? "Trái Đất" : r.gender == 1 ? "Namếc" : "Xayda";
                String info1;
                String info2;
                if (type == 0) {
                    info1 = "Sức mạnh: " + Util.numberToMoney(r.power);
                    info2 = "Tiềm năng: " + Util.numberToMoney(r.tiemNang) + " (" + planet + ")";
                } else if (type == 1) {
                    String taskName = "Nhiệm vụ " + r.taskId;
                    String subName = "Bước " + (r.taskIndex + 1);
                    int maxCount = 0;
                    for (TaskMain tm : Manager.TASKS) {
                        if (tm != null && tm.id == r.taskId) {
                            taskName = tm.name;
                            if (tm.subTasks != null && r.taskIndex >= 0 && r.taskIndex < tm.subTasks.size()) {
                                SubTaskMain st = tm.subTasks.get(r.taskIndex);
                                subName = st.name;
                                maxCount = st.maxCount;
                            }
                            break;
                        }
                    }
                    info1 = "NV " + r.taskId + "." + (r.taskIndex + 1) + ": " + taskName;
                    info2 = subName + (maxCount > 0 ? (" (" + r.taskCount + "/" + maxCount + ")") : "");
                } else {
                    info1 = "Đòn cao nhất: " + Util.formatNumber(r.maxDameHit) + " ST";
                    info2 = "Sức đánh: " + Util.formatNumber(r.curDame) + " (" + planet + ")";
                }

                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(r.id);
                msg.writer().writeShort(r.head);
                if (viewer.getSession() != null && viewer.getSession().version > 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(r.body);
                msg.writer().writeShort(r.leg);
                msg.writer().writeUTF(r.name);
                msg.writer().writeUTF(info1);
                msg.writer().writeUTF(info2);
            }

            viewer.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(OngGohan.class, e, "Lỗi hiển thị BXH tại nhà");
        }
    }

    protected void openMenuRuongSKHTanThu(Player player) {
        int ruongSkh = 5;
        if (player.getSession() != null) {
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT ruong_skh FROM account WHERE id = ? LIMIT 1")) {
                ps.setInt(1, player.getSession().userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ruongSkh = rs.getInt("ruong_skh");
                        player.getSession().ruongSkh = ruongSkh;
                    }
                }
            } catch (Exception ignored) {
                ruongSkh = player.getSession().ruongSkh;
            }
        }
        String[] setOptions = switch (player.gender) {
            case 0 -> new String[]{
                "Set Songoku\n(Kamejoko)",
                "Set T.Xin Hăng\n(Thái Dương HS)",
                "Set Krillin\n(Quả Cầu KK)",
                "Set Kaio\n(Kaioken)",
                "Đóng"
            };
            case 1 -> new String[]{
                "Set Ốc Tiêu\n(Liên Hoàn)",
                "Set Daimao\n(Đẻ Trứng)",
                "Set Picolo\n(Laze +50%)",
                "Set CB Nail\n(Masenko)",
                "Đóng"
            };
            default -> new String[]{
                "Set Kakarot\n(Galick)",
                "Set Cadic\n(x5 Biến Khỉ)",
                "Set Nappa\n(+80% HP)",
                "Set Cadic M\n(Tự Phát Nổ)",
                "Đóng"
            };
        };
        this.createOtherMenu(player, 9904,
                "|7|=== RƯƠNG SKH VẢI THÔ CƠ BẢN TÙY CHỌN ===\n"
                + "|2|Số Rương SKH hiện có của con: " + ruongSkh + " rương (Mặc định Tân Thủ: 5 rương)\n"
                + "|1|Mỗi Rương SKH mở ra trọn bộ 5 món Vải Thô cơ bản (Áo, Quần, Găng, Giày, Rada cấp 1) kích hoạt 100% sát thương!\n"
                + "|0|Hãy chọn Set Kích Hoạt phù hợp với lối chơi của con:",
                setOptions);
    }

    protected void claimStarterSKHSet(Player player, int select) {
        if (select < 0 || select > 3) {
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 5) {
            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 5 ô trống để nhận trọn bộ 5 món SKH Vải Thô!");
            return;
        }
        if (player.getSession() == null) {
            return;
        }
        int userId = player.getSession().userId;
        try (Connection con = LocalManager.getConnection()) {
            int ruongSkh = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT ruong_skh FROM account WHERE id = ? LIMIT 1")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ruongSkh = rs.getInt("ruong_skh");
                    }
                }
            }
            if (ruongSkh <= 0) {
                player.getSession().ruongSkh = 0;
                Service.gI().sendThongBao(player, "Bạn đã mở hết Rương SKH (Còn 0 rương)! Hãy liên hệ Admin để được tặng thêm Rương SKH.");
                return;
            }
            int remainSkh = ruongSkh - 1;
            try (PreparedStatement psUpdate = con.prepareStatement("UPDATE account SET ruong_skh = ? WHERE id = ?")) {
                psUpdate.setInt(1, remainSkh);
                psUpdate.setInt(2, userId);
                psUpdate.executeUpdate();
            }
            player.getSession().ruongSkh = remainSkh;

            int gender = player.gender < 0 || player.gender > 2 ? 0 : player.gender;
            int[][] skhTable = {
                {129, 127, 128, 245}, // Trái Đất: Songoku, Thiên Xin Hăng, Krillin, Thần Vũ Trụ Kaio
                {131, 132, 130, 237}, // Namek: Ốc Tiêu, Pikkoro Daimao, Picolo, Chiến Binh Nail
                {133, 134, 135, 241}  // Xayda: Kakarot, Cadic, Nappa, Cadic M
            };
            String[][] skhNames = {
                {"Set Songoku", "Set Thiên Xin Hăng", "Set Krillin", "Set Thần Vũ Trụ Kaio"},
                {"Set Ốc Tiêu", "Set Pikkoro Daimao", "Set Picolo", "Set Chiến Binh Nail"},
                {"Set Kakarot", "Set Cadic", "Set Nappa", "Set Cadic M"}
            };
            short[][] basicEquipIds = {
                {0, 6, 21, 27, 12}, // Trái Đất: Áo vải 3 lỗ, Quần vải đen, Găng vải đen, Giày nhựa, Rada cấp 1
                {1, 7, 22, 28, 12}, // Namek: Áo sợi len, Quần sợi len, Găng sợi len, Giày sợi len, Rada cấp 1
                {2, 8, 23, 29, 12}  // Xayda: Áo vải thô, Quần vải thô, Găng vải thô, Giày vải thô, Rada cấp 1
            };
            int[][] baseOptionTable = {
                {47, 3},  // Áo: Giáp +3
                {6, 30},  // Quần: HP +30
                {0, 15},  // Găng: Sức đánh +15
                {7, 30},  // Giày: KI +30
                {14, 1}   // Rada: Chí mạng +1%
            };

            int skhId = skhTable[gender][select];
            int[] subSkhIds = ItemService.gI().getOptionIdsBySKH(skhId);
            for (int i = 0; i < 5; i++) {
                Item it = ItemService.gI().createNewItem(basicEquipIds[gender][i], 1);
                it.itemOptions.clear();
                it.itemOptions.add(new Item.ItemOption(baseOptionTable[i][0], baseOptionTable[i][1]));
                it.itemOptions.add(new Item.ItemOption(skhId, 1));
                for (int subSkhId : subSkhIds) {
                    it.itemOptions.add(new Item.ItemOption(subSkhId, 1));
                }
                it.itemOptions.add(new Item.ItemOption(30, 1)); // Không thể giao dịch
                InventoryService.gI().addItemBag(player, it);
            }
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã nhận trọn bộ 5 món Vải Thô " + skhNames[gender][select] + " (Còn lại: " + remainSkh + " Rương SKH)!");
        } catch (Exception e) {
            Logger.logException(OngGohan.class, e, "Lỗi nhận rương SKH tân thủ: " + player.name);
            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi nhận Rương SKH, vui lòng thử lại!");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.getIndexMenu() == 9904) {
                claimStarterSKHSet(player, select);
                return;
            }
            if (player.idMark.getIndexMenu() == 9905) {
                if (select >= 0 && select <= 2) {
                    showHomeLeaderboard(player, select);
                } else if (select == 3) {
                    openMenuThuongTopNgay(player);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 9906) {
                if (select >= 0 && select <= 2) {
                    claimDailyTopReward(player, select);
                } else if (select == 3) {
                    openMenuBangXepHang(player);
                }
                return;
            }
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 6 -> {
                        nro.models.services_func.Input.gI().createFormGiftCode(player);
                    }
                    case 5 -> {
                        nro.models.npc.NpcFactory.openMenuTienIch(player);
                    }
                    case 4 -> {
                        openMenuBangXepHang(player);
                    }
                    case 3 -> {
                        openMenuRuongSKHTanThu(player);
                    }
                    case 0 -> {
                        long newGem = Math.min((long) player.inventory.gem + 200_000L, 2_000_000_000L);
                        player.inventory.gem = (int) newGem;
                        Service.gI().sendMoney(player);
                        Service.gI().sendThongBao(player, "Bạn vừa nhận được 200.000 Ngọc xanh!");
                    }
                    case 1 -> {
                        Item dau10 = ItemService.gI().createNewItem((short) 595, 99);
                        dau10.itemOptions.add(new Item.ItemOption(2, 256));
                        if (InventoryService.gI().addItemBag(player, dau10)) {
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Bạn vừa nhận được x99 Đậu thần cấp 10!");
                        } else {
                            Service.gI().sendThongBao(player, "Hành trang đã đầy, vui lòng dọn trống ít nhất 1 ô!");
                        }
                    }
                    case 2 -> {
                        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống để nhận Quà Tân Thủ!");
                            return;
                        }
                        if (player.getSession() == null) {
                            return;
                        }
                        int userId = player.getSession().userId;
                        try (Connection con = LocalManager.getConnection()) {
                            int giftFlags = 0;
                            try (PreparedStatement ps = con.prepareStatement("SELECT is_gift_box FROM account WHERE id = ? LIMIT 1")) {
                                ps.setInt(1, userId);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        giftFlags = rs.getInt("is_gift_box");
                                    }
                                }
                            }
                            if ((giftFlags & 1) != 0) {
                                Service.gI().sendThongBao(player, "Mỗi tài khoản chỉ được nhận Quà Tân Thủ duy nhất 1 lần!");
                                return;
                            }
                            try (PreparedStatement psUpdate = con.prepareStatement("UPDATE account SET is_gift_box = ? WHERE id = ?")) {
                                psUpdate.setInt(1, giftFlags | 1);
                                psUpdate.setInt(2, userId);
                                psUpdate.executeUpdate();
                            }
                            long newGem = Math.min((long) player.inventory.gem + 1_000_000L, 2_000_000_000L);
                            player.inventory.gem = (int) newGem;
                            Service.gI().sendMoney(player);

                            Item thoiVang = ItemService.gI().createNewItem((short) 457, 99);
                            InventoryService.gI().addItemBag(player, thoiVang);
                            InventoryService.gI().sendItemBags(player);

                            Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã nhận Quà Tân Thủ: x99 Thỏi vàng và 1.000.000 Ngọc!");
                        } catch (Exception e) {
                            Logger.logException(OngGohan.class, e, "Lỗi nhận quà tân thủ: " + player.name);
                            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi nhận quà, vui lòng thử lại!");
                        }
                    }
                }
            }
        }
    }
}
