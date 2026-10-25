package nro.models.matches.dai_hoi_vo_thuat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nro.models.clan.Clan;
import nro.models.consts.ConstMap;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.map.service.NpcService;
import nro.models.matches.DHVT;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.server.Maintenance;
import nro.models.server.ServerNotify;
import nro.models.services.EffectSkillService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.ItemTimeService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 * Hệ thống Đại Hội Võ Thuật Bang Hội (PK Bang Hội tại map 134).
 */
public class ClanMartialArtsService {

    public static final int MAP_ID = ConstMap.DAI_HOI_VO_THUAT_BANG_HOI; // 134
    public static final int MENU_MAIN = ConstNpc.BASE_MENU;
    public static final int MENU_SELECT_LINEUP = 7101;

    public static final int FEE_TYPE_GOLD_BAR = 1; // 20 Thỏi vàng
    public static final int FEE_TYPE_RUBY = 2;     // 1.000 Hồng ngọc

    public static final int FEE_GOLD_BAR_AMOUNT = 20;
    public static final int FEE_RUBY_AMOUNT = 1000;
    public static final int REWARD_GOLD_BAR_AMOUNT = 30; // 150% của 20 Thỏi vàng
    public static final int REWARD_RUBY_AMOUNT = 1500;   // 150% của 1.000 Hồng ngọc

    public static final int ROUND_DURATION_SECONDS = 300; // 5 phút đếm ngược

    private static ClanMartialArtsService instance;

    public static ClanMartialArtsService gI() {
        if (instance == null) {
            instance = new ClanMartialArtsService();
        }
        return instance;
    }

    public static class ClanRegistration {
        public final int clanId;
        public final String clanName;
        public final long leaderId;
        public final String leaderName;
        public final List<Long> fighterIds;
        public final List<String> fighterNames;
        public final int feeType;
        public final long registeredAt;

        public ClanRegistration(int clanId, String clanName, long leaderId, String leaderName,
                List<Long> fighterIds, List<String> fighterNames, int feeType) {
            this.clanId = clanId;
            this.clanName = clanName;
            this.leaderId = leaderId;
            this.leaderName = leaderName;
            this.fighterIds = new ArrayList<>(fighterIds);
            this.fighterNames = new ArrayList<>(fighterNames);
            this.feeType = feeType;
            this.registeredAt = System.currentTimeMillis();
        }
    }

    private final List<ClanRegistration> waitingQueue = new CopyOnWriteArrayList<>();
    private final List<ClanArenaMatch> activeMatches = new CopyOnWriteArrayList<>();
    private final Map<Integer, List<Long>> draftLineups = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> candidateCacheByLeader = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private ClanMartialArtsService() {
        scheduler.scheduleAtFixedRate(this::updateSystem, 1, 1, TimeUnit.SECONDS);
    }

    public void goToClanArenaMap(Player player) {
        if (player == null) {
            return;
        }
        Zone targetZone = MapService.gI().getMapCanJoin(player, MAP_ID, 0);
        if (targetZone == null) {
            targetZone = MapService.gI().getMapWithRandZone(MAP_ID);
        }
        if (targetZone != null) {
            ChangeMapService.gI().changeMapNonSpaceship(player, MAP_ID, Util.nextInt(250, 480), 360);
        } else {
            Service.gI().sendThongBao(player, "Hiện tại võ đài đang bảo trì, vui lòng thử lại sau!");
        }
    }

    private synchronized void updateSystem() {
        if (Maintenance.isRunning) {
            return;
        }
        try {
            // 1. Dọn các bang hội trong hàng chờ nếu không còn ai có mặt tại map 134
            for (ClanRegistration reg : new ArrayList<>(waitingQueue)) {
                if (!hasAnyAvailableFighterInArena(reg)) {
                    waitingQueue.remove(reg);
                    refundRegistrationFee(reg, "Bang [" + reg.clanName + "] đã bị hủy đăng ký do toàn bộ đấu thủ rời khỏi võ đài (Đã hoàn trả lệ phí).");
                }
            }

            // 2. Tự động ghép ngẫu nhiên 2 bang hội đang chờ
            if (waitingQueue.size() >= 2) {
                List<ClanRegistration> shuffled = new ArrayList<>(waitingQueue);
                Collections.shuffle(shuffled);
                ClanRegistration reg1 = shuffled.get(0);
                ClanRegistration reg2 = shuffled.get(1);
                waitingQueue.remove(reg1);
                waitingQueue.remove(reg2);
                startMatchBetween(reg1, reg2);
            }

            // 3. Cập nhật tất cả các trận đấu đang diễn ra
            for (ClanArenaMatch match : new ArrayList<>(activeMatches)) {
                try {
                    match.updateOneSecond();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasAnyAvailableFighterInArena(ClanRegistration reg) {
        for (Long fId : reg.fighterIds) {
            Player pl = Client.gI().getPlayer(fId);
            if (pl != null && pl.zone != null && pl.zone.map != null && pl.zone.map.mapId == MAP_ID) {
                return true;
            }
        }
        return false;
    }

    private void startMatchBetween(ClanRegistration reg1, ClanRegistration reg2) {
        nro.models.map.Map arenaMap = MapService.gI().getMapById(MAP_ID);
        if (arenaMap == null || arenaMap.zones.isEmpty()) {
            refundRegistrationFee(reg1, "Không tìm thấy khu vực thi đấu, đã hoàn lệ phí.");
            refundRegistrationFee(reg2, "Không tìm thấy khu vực thi đấu, đã hoàn lệ phí.");
            return;
        }

        Zone matchZone = null;
        for (Zone z : arenaMap.zones) {
            if (getActiveMatchInZone(z) == null) {
                matchZone = z;
                break;
            }
        }
        if (matchZone == null) {
            matchZone = arenaMap.zones.get(0);
        }

        // Đưa các thành viên của 2 bang đang đứng ở map 134 về cùng khu vực thi đấu để theo dõi & lên sàn
        moveClanMembersInArenaToZone(reg1.clanId, matchZone);
        moveClanMembersInArenaToZone(reg2.clanId, matchZone);

        ClanArenaMatch match = new ClanArenaMatch(reg1, reg2, matchZone);
        activeMatches.add(match);
    }

    private void moveClanMembersInArenaToZone(int clanId, Zone targetZone) {
        if (targetZone == null) {
            return;
        }
        for (Player pl : Client.gI().getPlayers()) {
            if (pl != null && pl.clan != null && pl.clan.id == clanId
                    && pl.zone != null && pl.zone.map != null && pl.zone.map.mapId == MAP_ID
                    && pl.zone.zoneId != targetZone.zoneId) {
                ChangeMapService.gI().changeZone(pl, targetZone.zoneId);
            }
        }
    }

    public ClanArenaMatch getActiveMatchInZone(Zone zone) {
        if (zone == null) {
            return null;
        }
        for (ClanArenaMatch m : activeMatches) {
            if (m.zone != null && m.zone.zoneId == zone.zoneId) {
                return m;
            }
        }
        return null;
    }

    public ClanArenaMatch getActiveMatchOfClan(int clanId) {
        for (ClanArenaMatch m : activeMatches) {
            if (m.reg1.clanId == clanId || m.reg2.clanId == clanId) {
                return m;
            }
        }
        return null;
    }

    public ClanRegistration getWaitingRegistration(int clanId) {
        for (ClanRegistration reg : waitingQueue) {
            if (reg.clanId == clanId) {
                return reg;
            }
        }
        return null;
    }

    public void openMenuTrongTai(Npc npc, Player player) {
        if (player == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("|7|ĐẠI HỘI VÕ THUẬT BANG HỘI\n");
        sb.append("|1|• Đặc quyền tại giải: Cường hóa x100 HP & KI, x5 Sức đánh\n");
        sb.append("• Thể thức: Lôi đài liên đấu lần lượt theo thứ tự Chủ bang chọn\n");
        sb.append("• Luật: Ai chết sẽ thua (Giới hạn 5 phút đếm ngược / hiệp)\n");
        sb.append("• Lệ phí: 20 Thỏi vàng hoặc 1.000 Hồng ngọc (Bang thắng nhận 150%!)\n");
        sb.append("|2|Số Bang đang chờ ghép cặp: ").append(waitingQueue.size())
                .append(" | Số trận đang đấu: ").append(activeMatches.size()).append("\n");

        if (player.clan == null) {
            sb.append("|6|Bạn hiện chưa có Bang hội. Hãy gia nhập hoặc lập Bang hội để tham chiến!");
            npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                    "Thể lệ\nGiải đấu", "Về\nĐảo Kame", "Đóng");
            return;
        }

        Clan clan = player.clan;
        ClanArenaMatch activeMatch = getActiveMatchOfClan(clan.id);
        if (activeMatch != null) {
            sb.append("|2|Bang [").append(clan.name).append("] đang thi đấu với Bang [")
                    .append(activeMatch.getOpponentClanName(clan.id)).append("] tại Khu ")
                    .append(activeMatch.zone.zoneId).append("!\n");
            sb.append(activeMatch.getStatusSummary());
            npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                    "Thể lệ\nGiải đấu", "Về\nĐảo Kame", "Đóng");
            return;
        }

        ClanRegistration waitingReg = getWaitingRegistration(clan.id);
        if (waitingReg != null) {
            sb.append("|2|Bang [").append(clan.name).append("] ĐÃ ĐĂNG KÝ thành công!\n");
            sb.append("• Lệ phí đã đóng: ").append(waitingReg.feeType == FEE_TYPE_GOLD_BAR ? "20 Thỏi vàng" : "1.000 Hồng ngọc").append("\n");
            sb.append("• Thứ tự lên đài: ").append(String.join(" -> ", waitingReg.fighterNames)).append("\n");
            sb.append("|1|Đang chờ Bang hội khác đăng ký để Trọng Tài ghép cặp ngẫu nhiên...");
            if (clan.isLeader(player)) {
                npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                        "Hủy đăng ký\n(Hoàn 100%)", "Thể lệ\nGiải đấu", "Về\nĐảo Kame", "Đóng");
            } else {
                npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                        "Thể lệ\nGiải đấu", "Về\nĐảo Kame", "Đóng");
            }
            return;
        }

        if (clan.isLeader(player)) {
            List<Long> draft = getCleanedDraftLineup(clan, player);
            sb.append("|2|Bang hội: [").append(clan.name).append("] (Bạn là Bang Chủ)\n");
            if (draft.isEmpty()) {
                sb.append("|6|Thứ tự thi đấu: Chưa chọn (Hãy bấm 'Chọn đội hình' để xếp thứ tự lên sàn)");
            } else {
                sb.append("|2|Thứ tự lên sàn đã chọn (").append(draft.size()).append(" người):\n")
                        .append(formatDraftLineup(draft));
            }
            npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                    "Chọn đội hình\n(" + draft.size() + " người)",
                    "Đăng ký\n(20 Thỏi vàng)",
                    "Đăng ký\n(1.000 Hồng ngọc)",
                    "Thể lệ\nGiải đấu",
                    "Về\nĐảo Kame");
        } else {
            List<Long> draft = draftLineups.getOrDefault(clan.id, Collections.emptyList());
            sb.append("|2|Bang hội: [").append(clan.name).append("]\n");
            sb.append("|1|Chỉ Bang Chủ mới có thể đăng ký và xếp thứ tự thi đấu.\n");
            if (!draft.isEmpty()) {
                sb.append("Đội hình dự kiến: ").append(formatDraftLineup(draft));
            }
            npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                    "Thể lệ\nGiải đấu", "Về\nĐảo Kame", "Đóng");
        }
    }

    public void confirmMenuTrongTai(Npc npc, Player player, int select) {
        if (player == null) {
            return;
        }
        int indexMenu = player.idMark.getIndexMenu();
        if (indexMenu == MENU_SELECT_LINEUP) {
            handleLineupSelection(npc, player, select);
            return;
        }
        if (indexMenu != MENU_MAIN) {
            return;
        }

        if (player.clan == null) {
            switch (select) {
                case 0 -> showTutorial(npc, player);
                case 1 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
            }
            return;
        }

        Clan clan = player.clan;
        ClanArenaMatch activeMatch = getActiveMatchOfClan(clan.id);
        if (activeMatch != null) {
            switch (select) {
                case 0 -> showTutorial(npc, player);
                case 1 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
            }
            return;
        }

        ClanRegistration waitingReg = getWaitingRegistration(clan.id);
        if (waitingReg != null) {
            if (clan.isLeader(player)) {
                switch (select) {
                    case 0 -> cancelRegistration(npc, player, waitingReg);
                    case 1 -> showTutorial(npc, player);
                    case 2 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
                }
            } else {
                switch (select) {
                    case 0 -> showTutorial(npc, player);
                    case 1 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
                }
            }
            return;
        }

        if (clan.isLeader(player)) {
            switch (select) {
                case 0 -> openLineupSelectionMenu(npc, player);
                case 1 -> registerClan(npc, player, FEE_TYPE_GOLD_BAR);
                case 2 -> registerClan(npc, player, FEE_TYPE_RUBY);
                case 3 -> showTutorial(npc, player);
                case 4 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
            }
        } else {
            switch (select) {
                case 0 -> showTutorial(npc, player);
                case 1 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
            }
        }
    }

    private void openLineupSelectionMenu(Npc npc, Player leader) {
        if (leader.clan == null || !leader.clan.isLeader(leader)) {
            return;
        }
        Clan clan = leader.clan;
        List<Long> draft = getCleanedDraftLineup(clan, leader);

        List<Player> availableMembers = getUnselectedClanMembersInArena(clan, draft);
        List<Long> candidateIds = new ArrayList<>();
        ArrayList<String> buttons = new ArrayList<>();

        for (int i = 0; i < availableMembers.size() && i < 6; i++) {
            Player mem = availableMembers.get(i);
            candidateIds.add(mem.id);
            buttons.add("Chọn #" + (draft.size() + 1) + "\n" + mem.name);
        }
        candidateCacheByLeader.put(leader.id, candidateIds);

        if (!availableMembers.isEmpty()) {
            buttons.add("Chọn tất cả\nTV tại map");
        }
        if (!draft.isEmpty()) {
            buttons.add("Xóa\nChọn lại");
        }
        buttons.add("Hoàn tất\nĐội hình");

        StringBuilder sb = new StringBuilder();
        sb.append("|7|XẾP THỨ TỰ LÊN SÀN ĐẤU - BANG [").append(clan.name).append("]\n");
        sb.append("|1|(Thành viên phải đang có mặt tại map Đại Hội Võ Thuật)\n");
        if (draft.isEmpty()) {
            sb.append("|6|Chưa có ai trong danh sách. Hãy bấm chọn từng thành viên theo thứ tự #1, #2, #3...\n");
        } else {
            sb.append("|2|Thứ tự ra sân hiện tại (").append(draft.size()).append(" người):\n")
                    .append(formatDraftLineup(draft)).append("\n");
        }
        sb.append("|1|Số thành viên trong map chưa chọn: ").append(availableMembers.size());

        npc.createOtherMenu(leader, MENU_SELECT_LINEUP, sb.toString(), buttons.toArray(String[]::new));
    }

    private void handleLineupSelection(Npc npc, Player leader, int select) {
        if (leader.clan == null || !leader.clan.isLeader(leader)) {
            return;
        }
        Clan clan = leader.clan;
        List<Long> draft = getCleanedDraftLineup(clan, leader);
        List<Long> candidateIds = candidateCacheByLeader.getOrDefault(leader.id, Collections.emptyList());
        List<Player> availableMembers = getUnselectedClanMembersInArena(clan, draft);

        int idx = 0;
        if (select >= 0 && select < candidateIds.size()) {
            long pickedId = candidateIds.get(select);
            Player pickedPl = Client.gI().getPlayer(pickedId);
            if (pickedPl != null && !draft.contains(pickedId)) {
                draft.add(pickedId);
                draftLineups.put(clan.id, draft);
                Service.gI().sendThongBao(leader, "Đã xếp " + pickedPl.name + " vào vị trí ra sân số #" + draft.size());
            }
            openLineupSelectionMenu(npc, leader);
            return;
        }
        idx += candidateIds.size();

        if (!availableMembers.isEmpty()) {
            if (select == idx) {
                for (Player mem : availableMembers) {
                    if (!draft.contains(mem.id)) {
                        draft.add(mem.id);
                    }
                }
                draftLineups.put(clan.id, draft);
                Service.gI().sendThongBao(leader, "Đã thêm toàn bộ " + draft.size() + " thành viên vào danh sách thi đấu!");
                openMenuTrongTai(npc, leader);
                return;
            }
            idx++;
        }

        if (!draft.isEmpty()) {
            if (select == idx) {
                draft.clear();
                draftLineups.put(clan.id, draft);
                Service.gI().sendThongBao(leader, "Đã xóa danh sách, hãy chọn lại từ vị trí #1!");
                openLineupSelectionMenu(npc, leader);
                return;
            }
            idx++;
        }

        // Nút Hoàn tất Đội hình -> Quay về menu chính để bấm Đăng ký
        openMenuTrongTai(npc, leader);
    }

    private synchronized void registerClan(Npc npc, Player leader, int feeType) {
        if (leader == null || leader.clan == null || !leader.clan.isLeader(leader)) {
            Service.gI().sendThongBao(leader, "Chỉ Chủ bang hội mới có quyền đăng ký thi đấu!");
            return;
        }
        Clan clan = leader.clan;
        if (getWaitingRegistration(clan.id) != null || getActiveMatchOfClan(clan.id) != null) {
            Service.gI().sendThongBao(leader, "Bang hội của bạn đã đăng ký hoặc đang trong trận đấu!");
            return;
        }

        List<Long> draft = getCleanedDraftLineup(clan, leader);
        if (draft.isEmpty()) {
            Service.gI().sendThongBao(leader, "Hãy chọn danh sách thành viên và thứ tự thi đấu trước khi đăng ký!");
            openLineupSelectionMenu(npc, leader);
            return;
        }

        List<String> fighterNames = new ArrayList<>();
        for (Long id : draft) {
            Player pl = Client.gI().getPlayer(id);
            if (pl != null) {
                fighterNames.add(pl.name);
            }
        }
        if (fighterNames.isEmpty()) {
            Service.gI().sendThongBao(leader, "Không có đấu thủ nào trong danh sách đang có mặt tại võ đài!");
            return;
        }

        // Kiểm tra và trừ lệ phí
        if (feeType == FEE_TYPE_GOLD_BAR) {
            Item goldBar = InventoryService.gI().findItemBag(leader, 457);
            if (goldBar == null || goldBar.quantity < FEE_GOLD_BAR_AMOUNT) {
                Service.gI().sendThongBao(leader, "Bạn không đủ " + FEE_GOLD_BAR_AMOUNT + " Thỏi vàng để đóng lệ phí thi đấu!");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(leader, goldBar, FEE_GOLD_BAR_AMOUNT);
            InventoryService.gI().sendItemBags(leader);
        } else {
            if (leader.inventory.ruby < FEE_RUBY_AMOUNT) {
                Service.gI().sendThongBao(leader, "Bạn không đủ " + Util.numberToMoney(FEE_RUBY_AMOUNT) + " Hồng ngọc để đóng lệ phí thi đấu!");
                return;
            }
            leader.inventory.ruby -= FEE_RUBY_AMOUNT;
            PlayerService.gI().sendInfoHpMpMoney(leader);
        }

        ClanRegistration reg = new ClanRegistration(
                clan.id, clan.name, leader.id, leader.name,
                draft, fighterNames, feeType
        );
        waitingQueue.add(reg);

        String feeStr = (feeType == FEE_TYPE_GOLD_BAR) ? (FEE_GOLD_BAR_AMOUNT + " Thỏi vàng") : (FEE_RUBY_AMOUNT + " Hồng ngọc");
        Service.gI().sendThongBao(leader, "Đăng ký PK Bang Hội thành công (" + feeStr + ")! Đang chờ ghép cặp ngẫu nhiên...");
        if (leader.zone != null && leader.zone.getNpc() != null) {
            Service.gI().chat(leader.zone.getNpc(), "Bang [" + clan.name + "] vừa đăng ký tham chiến với " + draft.size() + " đấu thủ! Bang nào dám nghênh chiến?");
        }
        notifyClanMembers(clan.id, "Bang Chủ " + leader.name + " đã đăng ký PK Bang Hội! Đội hình ra sân: " + String.join(" -> ", fighterNames));
    }

    private synchronized void cancelRegistration(Npc npc, Player leader, ClanRegistration reg) {
        if (waitingQueue.remove(reg)) {
            refundRegistrationFee(reg, "Bạn đã hủy đăng ký PK Bang Hội và nhận lại 100% lệ phí.");
            openMenuTrongTai(npc, leader);
        } else {
            Service.gI().sendThongBao(leader, "Bang hội đã được ghép trận, không thể hủy đăng ký!");
        }
    }

    private void refundRegistrationFee(ClanRegistration reg, String reasonMsg) {
        Player receiver = Client.gI().getPlayer(reg.leaderId);
        if (receiver == null) {
            for (Long fId : reg.fighterIds) {
                Player p = Client.gI().getPlayer(fId);
                if (p != null) {
                    receiver = p;
                    break;
                }
            }
        }
        if (receiver != null) {
            if (reg.feeType == FEE_TYPE_GOLD_BAR) {
                Item goldBar = ItemService.gI().createNewItem((short) 457, FEE_GOLD_BAR_AMOUNT);
                InventoryService.gI().addItemBag(receiver, goldBar);
                InventoryService.gI().sendItemBags(receiver);
            } else {
                receiver.inventory.ruby += FEE_RUBY_AMOUNT;
                PlayerService.gI().sendInfoHpMpMoney(receiver);
            }
            Service.gI().sendThongBao(receiver, reasonMsg);
        }
    }

    private void rewardWinnerClan(ClanRegistration winReg, ClanRegistration loseReg) {
        Player receiver = Client.gI().getPlayer(winReg.leaderId);
        if (receiver == null) {
            for (Long fId : winReg.fighterIds) {
                Player p = Client.gI().getPlayer(fId);
                if (p != null) {
                    receiver = p;
                    break;
                }
            }
        }
        String rewardText;
        if (winReg.feeType == FEE_TYPE_GOLD_BAR) {
            rewardText = REWARD_GOLD_BAR_AMOUNT + " Thỏi vàng (Hoàn trả 150%)";
            if (receiver != null) {
                Item goldBar = ItemService.gI().createNewItem((short) 457, REWARD_GOLD_BAR_AMOUNT);
                InventoryService.gI().addItemBag(receiver, goldBar);
                InventoryService.gI().sendItemBags(receiver);
            }
        } else {
            rewardText = Util.numberToMoney(REWARD_RUBY_AMOUNT) + " Hồng ngọc (Hoàn trả 150%)";
            if (receiver != null) {
                receiver.inventory.ruby += REWARD_RUBY_AMOUNT;
                PlayerService.gI().sendInfoHpMpMoney(receiver);
            }
        }
        if (receiver != null) {
            Service.gI().sendThongBao(receiver, "Chúc mừng Bang [" + winReg.clanName + "] chiến thắng! Nhận thưởng " + rewardText + "!");
        }
        notifyClanMembers(winReg.clanId, "Bang [" + winReg.clanName + "] đã chiến thắng Bang [" + loseReg.clanName + "] tại Đại Hội Võ Thuật! Phần thưởng 150% lệ phí (" + rewardText + ") đã trao cho Bang Chủ!");
        notifyClanMembers(loseReg.clanId, "Bang [" + loseReg.clanName + "] đã thất bại trước Bang [" + winReg.clanName + "] tại Đại Hội Võ Thuật Bang Hội. Hãy phục thù ở trận sau!");
        ServerNotify.gI().notify("Đại Hội Võ Thuật Bang Hội: Bang [" + winReg.clanName + "] đã xuất sắc đánh bại Bang [" + loseReg.clanName + "] và nhận thưởng 150% lệ phí!");
    }

    private void notifyClanMembers(int clanId, String msg) {
        for (Player pl : Client.gI().getPlayers()) {
            if (pl != null && pl.clan != null && pl.clan.id == clanId) {
                Service.gI().sendThongBao(pl, msg);
            }
        }
    }

    private void showTutorial(Npc npc, Player player) {
        String tutorial = "THỂ LỆ ĐẠI HỘI VÕ THUẬT BANG HỘI:\n"
                + "1. Chủ bang hội nói chuyện với Trọng Tài để chọn danh sách & thứ tự thành viên lên sàn đấu.\n"
                + "2. Đóng lệ phí thi đấu: 20 Thỏi vàng hoặc 1.000 Hồng ngọc.\n"
                + "3. Trọng Tài sẽ tự động ghép cặp ngẫu nhiên 2 Bang hội đã đăng ký.\n"
                + "4. Đấu thủ lần lượt lên sàn đấu theo đúng thứ tự Chủ bang đã chọn. Tại đây người chơi được x100 HP & KI, x5 Sức đánh.\n"
                + "5. Mỗi hiệp giới hạn 5 phút đếm ngược. Ai chết hoặc rơi khỏi đài sẽ thua hiệp đó và người tiếp theo của Bang sẽ lên thay.\n"
                + "6. Bang chiến thắng chung cuộc sẽ được hoàn trả 150% lệ phí (30 Thỏi vàng hoặc 1.500 Hồng ngọc)!";
        NpcService.gI().createTutorial(player, npc.tempId, npc.avartar, tutorial);
    }

    private List<Long> getCleanedDraftLineup(Clan clan, Player leader) {
        List<Long> list = draftLineups.computeIfAbsent(clan.id, k -> new ArrayList<>());
        list.removeIf(id -> {
            Player pl = Client.gI().getPlayer(id);
            return pl == null || pl.clan == null || pl.clan.id != clan.id
                    || pl.zone == null || pl.zone.map == null || pl.zone.map.mapId != MAP_ID;
        });
        return list;
    }

    private List<Player> getUnselectedClanMembersInArena(Clan clan, List<Long> draft) {
        List<Player> result = new ArrayList<>();
        for (Player pl : clan.membersInGame) {
            if (pl != null && pl.zone != null && pl.zone.map != null && pl.zone.map.mapId == MAP_ID
                    && !draft.contains(pl.id)) {
                result.add(pl);
            }
        }
        return result;
    }

    private String formatDraftLineup(List<Long> draft) {
        List<String> parts = new ArrayList<>();
        int order = 1;
        for (Long id : draft) {
            Player pl = Client.gI().getPlayer(id);
            if (pl != null) {
                parts.add("#" + (order++) + " " + pl.name);
            }
        }
        return String.join(" -> ", parts);
    }

    /**
     * Phiên thi đấu giữa 2 Bang hội trên sàn đấu.
     */
    public class ClanArenaMatch {

        public final ClanRegistration reg1;
        public final ClanRegistration reg2;
        public final Zone zone;
        private final Player referee;

        private int index1 = 0;
        private int index2 = 0;
        private int roundNumber = 0;

        private Player fighter1;
        private Player fighter2;
        private DHVT activePvp;

        private int state = STATE_PREPARE_ROUND;
        private int stateTimer = 6;
        private int roundTimeRemaining = ROUND_DURATION_SECONDS;

        private static final int STATE_PREPARE_ROUND = 0;
        private static final int STATE_FIGHTING = 1;
        private static final int STATE_BETWEEN_ROUNDS = 2;
        private static final int STATE_FINISHED = 3;

        public ClanArenaMatch(ClanRegistration reg1, ClanRegistration reg2, Zone zone) {
            this.reg1 = reg1;
            this.reg2 = reg2;
            this.zone = zone;
            this.referee = zone != null ? zone.getNpc() : null;
            initMatch();
        }

        private void initMatch() {
            broadcastInZone("Cặp đấu PK Bang Hội đã được ghép: Bang [" + reg1.clanName + "] VS Bang [" + reg2.clanName + "]! Trận đấu chuẩn bị bắt đầu!");
            prepareNextRound(true, true);
        }

        public String getOpponentClanName(int myClanId) {
            return (reg1.clanId == myClanId) ? reg2.clanName : reg1.clanName;
        }

        public String getStatusSummary() {
            String f1Name = fighter1 != null ? fighter1.name : "Đang chờ";
            String f2Name = fighter2 != null ? fighter2.name : "Đang chờ";
            return "• Hiệp " + roundNumber + ": " + f1Name + " vs " + f2Name
                    + "\n• Thời gian hiệp còn: " + roundTimeRemaining + " giây";
        }

        public void updateOneSecond() {
            if (state == STATE_FINISHED) {
                return;
            }

            // Giữ khán giả đứng dưới khán đài (y = 360), không cho nhảy lên sàn đấu phá trận
            keepSpectatorsOffStage();

            if (state == STATE_PREPARE_ROUND) {
                handlePrepareRoundTick();
            } else if (state == STATE_FIGHTING) {
                handleFightingTick();
            } else if (state == STATE_BETWEEN_ROUNDS) {
                stateTimer--;
                if (stateTimer <= 0) {
                    prepareNextRound(fighter1 == null, fighter2 == null);
                }
            }
        }

        private void keepSpectatorsOffStage() {
            if (zone == null) {
                return;
            }
            for (Player pl : new ArrayList<>(zone.getPlayers())) {
                if (pl == null || !pl.isPl()) {
                    continue;
                }
                if (pl.equals(fighter1) || pl.equals(fighter2)) {
                    continue;
                }
                // Sàn đấu ở độ cao y <= 300, x từ 140 đến 640
                if (pl.location.y <= 300 && pl.location.x >= 140 && pl.location.x <= 640) {
                    Service.gI().setPos(pl, Util.nextInt(250, 480), 360);
                    Service.gI().sendThongBao(pl, "Khu vực trên đài dành cho 2 đấu thủ đang thi đấu! Hãy đứng dưới khán đài cổ vũ hoặc chờ tới lượt.");
                }
            }
        }

        private Player pollNextValidFighter(ClanRegistration reg, boolean isClan1) {
            int idx = isClan1 ? index1 : index2;
            while (idx < reg.fighterIds.size()) {
                long fId = reg.fighterIds.get(idx);
                Player pl = Client.gI().getPlayer(fId);
                if (pl != null && pl.zone != null && pl.zone.map != null && pl.zone.map.mapId == MAP_ID) {
                    if (isClan1) {
                        index1 = idx;
                    } else {
                        index2 = idx;
                    }
                    return pl;
                }
                idx++;
            }
            if (isClan1) {
                index1 = idx;
            } else {
                index2 = idx;
            }
            return null;
        }

        private void prepareNextRound(boolean refreshFighter1, boolean refreshFighter2) {
            fighter1 = pollNextValidFighter(reg1, true);
            fighter2 = pollNextValidFighter(reg2, false);

            if (fighter1 == null && fighter2 == null) {
                // Cả 2 bên đều rời map -> hòa, hoàn trả lệ phí
                broadcastInZone("Cả 2 Bang hội đều không còn đấu thủ tại võ đài. Trận đấu kết thúc hòa!");
                refundRegistrationFee(reg1, "Trận đấu kết thúc hòa, hoàn trả 100% lệ phí.");
                refundRegistrationFee(reg2, "Trận đấu kết thúc hòa, hoàn trả 100% lệ phí.");
                cleanupMatch();
                return;
            }
            if (fighter1 == null) {
                finishMatch(reg2, reg1);
                return;
            }
            if (fighter2 == null) {
                finishMatch(reg1, reg2);
                return;
            }

            roundNumber++;
            state = STATE_PREPARE_ROUND;
            stateTimer = 6;
            roundTimeRemaining = ROUND_DURATION_SECONDS;

            // Đảm bảo cả 2 đấu thủ ở đúng khu thi đấu
            if (fighter1.zone.zoneId != zone.zoneId) {
                ChangeMapService.gI().changeZone(fighter1, zone.zoneId);
            }
            if (fighter2.zone.zoneId != zone.zoneId) {
                ChangeMapService.gI().changeZone(fighter2, zone.zoneId);
            }

            prepareFighterForStage(fighter1, 334, 264, refreshFighter1);
            prepareFighterForStage(fighter2, 434, 264, refreshFighter2);

            if (referee != null) {
                PlayerService.gI().playerMove(referee, 385, 264);
            }
        }

        private void prepareFighterForStage(Player fighter, int x, int y, boolean fullHealAndResetCd) {
            if (fighter.isDie()) {
                Service.gI().hsChar(fighter, fighter.nPoint.hpMax, fighter.nPoint.mpMax);
            }
            fighter.nPoint.calPoint();
            if (fullHealAndResetCd) {
                fighter.nPoint.setFullHpMp();
                Service.gI().releaseCooldownSkill(fighter);
            }
            Service.gI().point(fighter);
            PlayerService.gI().sendInfoHpMp(fighter);
            Service.gI().Send_Info_NV(fighter);
            PlayerService.gI().changeAndSendTypePK(fighter, ConstPlayer.NON_PK);
            Service.gI().setPos0(fighter, x, y);
            EffectSkillService.gI().startStun(fighter, System.currentTimeMillis(), 6000);
        }

        private void handlePrepareRoundTick() {
            if (!isFighterPresentInZone(fighter1) || !isFighterPresentInZone(fighter2)) {
                handleFightingTick();
                return;
            }
            switch (stateTimer) {
                case 6 -> {
                    Service.gI().setPos0(fighter1, 334, 264);
                    Service.gI().setPos0(fighter2, 434, 264);
                    refereeChat("Hiệp " + roundNumber + ": [" + reg1.clanName + "] " + fighter1.name
                            + " (#" + (index1 + 1) + ") VS [" + reg2.clanName + "] " + fighter2.name + " (#" + (index2 + 1) + ")!");
                }
                case 4 -> refereeChat("Cường hóa x100 HP & KI, x5 Sức đánh! Thời gian thi đấu: 5 phút!");
                case 2 -> refereeChat("Mọi người ổn định chỗ ngồi! 3... 2... 1...");
                case 1 -> {
                    refereeChat("TRẬN ĐẤU BẮT ĐẦU!");
                    if (referee != null) {
                        PlayerService.gI().playerMove(referee, 385, 360);
                    }
                    EffectSkillService.gI().removeStun(fighter1);
                    EffectSkillService.gI().removeStun(fighter2);

                    fighter1.isPKDHVT = true;
                    fighter2.isPKDHVT = true;
                    fighter1.lostByDeath = false;
                    fighter2.lostByDeath = false;
                    fighter1.totalDamageTaken = 0;
                    fighter2.totalDamageTaken = 0;

                    PlayerService.gI().changeAndSendTypePK(fighter1, ConstPlayer.PK_PVP);
                    PlayerService.gI().changeAndSendTypePK(fighter2, ConstPlayer.PK_PVP);
                    Service.gI().sendPVP(fighter1, fighter2);
                    Service.gI().sendPVP(fighter2, fighter1);
                    activePvp = new DHVT(fighter1, fighter2);

                    // Đồng hồ đếm ngược 5 phút (300 giây)
                    ItemTimeService.gI().sendItemTime(fighter1, 3779, ROUND_DURATION_SECONDS);
                    ItemTimeService.gI().sendItemTime(fighter2, 3779, ROUND_DURATION_SECONDS);

                    state = STATE_FIGHTING;
                    roundTimeRemaining = ROUND_DURATION_SECONDS;
                    return;
                }
            }
            stateTimer--;
        }

        private void handleFightingTick() {
            boolean f1Valid = isFighterPresentInZone(fighter1);
            boolean f2Valid = isFighterPresentInZone(fighter2);

            if (!f1Valid && !f2Valid) {
                index1++;
                index2++;
                onRoundEnd(null, null, "Cả 2 đấu thủ đều rời khỏi võ đài!");
                return;
            }
            if (!f1Valid) {
                index1++;
                onRoundEnd(fighter2, fighter1, (fighter1 != null ? fighter1.name : "Đấu thủ") + " đã bỏ cuộc rời map!");
                return;
            }
            if (!f2Valid) {
                index2++;
                onRoundEnd(fighter1, fighter2, (fighter2 != null ? fighter2.name : "Đấu thủ") + " đã bỏ cuộc rời map!");
                return;
            }

            // Kiểm tra chết
            if (fighter1.isDie() || fighter1.lostByDeath) {
                index1++;
                onRoundEnd(fighter2, fighter1, fighter1.name + " đã gục ngã! " + fighter2.name + " thắng Hiệp " + roundNumber + "!");
                return;
            }
            if (fighter2.isDie() || fighter2.lostByDeath) {
                index2++;
                onRoundEnd(fighter1, fighter2, fighter2.name + " đã gục ngã! " + fighter1.name + " thắng Hiệp " + roundNumber + "!");
                return;
            }

            // Kiểm tra rơi khỏi sàn đấu (y > 312 hoặc x ngoài [145, 635])
            if (isFallenOffRing(fighter1)) {
                index1++;
                onRoundEnd(fighter2, fighter1, fighter1.name + " đã rơi khỏi võ đài! " + fighter2.name + " thắng Hiệp " + roundNumber + "!");
                return;
            }
            if (isFallenOffRing(fighter2)) {
                index2++;
                onRoundEnd(fighter1, fighter2, fighter2.name + " đã rơi khỏi võ đài! " + fighter1.name + " thắng Hiệp " + roundNumber + "!");
                return;
            }

            // Đếm ngược 5 phút (300 giây)
            roundTimeRemaining--;
            if (roundTimeRemaining == 240 || roundTimeRemaining == 180 || roundTimeRemaining == 120 || roundTimeRemaining == 60) {
                refereeChat("Hiệp " + roundNumber + " còn " + (roundTimeRemaining / 60) + " phút!");
            } else if (roundTimeRemaining == 30 || roundTimeRemaining == 10) {
                refereeChat("Hiệp " + roundNumber + " chỉ còn " + roundTimeRemaining + " giây!");
            }

            if (roundTimeRemaining <= 0) {
                double hpRatio1 = (double) fighter1.nPoint.hp / Math.max(1, fighter1.nPoint.hpMax);
                double hpRatio2 = (double) fighter2.nPoint.hp / Math.max(1, fighter2.nPoint.hpMax);
                if (hpRatio1 > hpRatio2 || (hpRatio1 == hpRatio2 && fighter1.totalDamageTaken <= fighter2.totalDamageTaken)) {
                    index2++;
                    onRoundEnd(fighter1, fighter2, "Hết 5 phút thi đấu! " + fighter1.name + " có ưu thế máu/sát thương và thắng Hiệp " + roundNumber + "!");
                } else {
                    index1++;
                    onRoundEnd(fighter2, fighter1, "Hết 5 phút thi đấu! " + fighter2.name + " có ưu thế máu/sát thương và thắng Hiệp " + roundNumber + "!");
                }
            }
        }

        private boolean isFighterPresentInZone(Player pl) {
            return pl != null && pl.zone != null && pl.zone.equals(zone);
        }

        private boolean isFallenOffRing(Player pl) {
            return pl.location.x < 145 || pl.location.x > 635 || pl.location.y > 312;
        }

        private void onRoundEnd(Player winner, Player loser, String reasonChat) {
            stopCurrentPvp();
            refereeChat(reasonChat);
            broadcastInZone(reasonChat);

            if (loser != null && loser.zone != null) {
                EffectSkillService.gI().removeStun(loser);
                ItemTimeService.gI().sendItemTime(loser, 3779, 0);
                if (loser.isDie()) {
                    Service.gI().hsChar(loser, loser.nPoint.hpMax, loser.nPoint.mpMax);
                } else {
                    loser.nPoint.setFullHpMp();
                    PlayerService.gI().sendInfoHpMp(loser);
                }
                if (loser.zone.equals(zone)) {
                    Service.gI().setPos(loser, Util.nextInt(250, 480), 360);
                }
                loser.isPKDHVT = false;
                loser.lostByDeath = false;
            }

            if (winner != null && winner.zone != null) {
                ItemTimeService.gI().sendItemTime(winner, 3779, 0);
            }

            if (loser == fighter1) {
                fighter1 = null;
            } else if (loser == fighter2) {
                fighter2 = null;
            }

            state = STATE_BETWEEN_ROUNDS;
            stateTimer = 4;
        }

        private void stopCurrentPvp() {
            if (activePvp != null) {
                activePvp.dispose();
                activePvp = null;
            }
            if (fighter1 != null && fighter1.zone != null) {
                PlayerService.gI().changeAndSendTypePK(fighter1, ConstPlayer.NON_PK);
                Service.gI().sendPlayerVS(fighter1, fighter2, (byte) 0);
            }
            if (fighter2 != null && fighter2.zone != null) {
                PlayerService.gI().changeAndSendTypePK(fighter2, ConstPlayer.NON_PK);
                Service.gI().sendPlayerVS(fighter2, fighter1, (byte) 0);
            }
        }

        private void finishMatch(ClanRegistration winReg, ClanRegistration loseReg) {
            state = STATE_FINISHED;
            stopCurrentPvp();

            if (referee != null) {
                PlayerService.gI().playerMove(referee, 385, 264);
            }
            String winMsg = "CHUNG CUỘC: Bang [" + winReg.clanName + "] đã xuất sắc đánh bại Bang [" + loseReg.clanName + "] và giành chức Vô Địch PK Bang Hội!";
            refereeChat(winMsg);
            broadcastInZone(winMsg);

            resetFighterAfterMatch(fighter1);
            resetFighterAfterMatch(fighter2);

            rewardWinnerClan(winReg, loseReg);
            cleanupMatch();
        }

        private void resetFighterAfterMatch(Player pl) {
            if (pl != null && pl.zone != null) {
                EffectSkillService.gI().removeStun(pl);
                ItemTimeService.gI().sendItemTime(pl, 3779, 0);
                if (pl.isDie()) {
                    Service.gI().hsChar(pl, pl.nPoint.hpMax, pl.nPoint.mpMax);
                } else {
                    pl.nPoint.setFullHpMp();
                    PlayerService.gI().sendInfoHpMp(pl);
                }
                PlayerService.gI().changeAndSendTypePK(pl, ConstPlayer.NON_PK);
                if (pl.zone.equals(zone)) {
                    Service.gI().setPos(pl, Util.nextInt(250, 480), 360);
                }
                pl.isPKDHVT = false;
                pl.lostByDeath = false;
            }
        }

        private void cleanupMatch() {
            state = STATE_FINISHED;
            activeMatches.remove(this);
        }

        private void refereeChat(String text) {
            if (referee != null) {
                Service.gI().chat(referee, text);
            }
        }

        private void broadcastInZone(String text) {
            if (zone == null) {
                return;
            }
            for (Player pl : zone.getPlayers()) {
                if (pl != null && pl.isPl()) {
                    Service.gI().sendThongBao(pl, text);
                }
            }
        }
    }
}
