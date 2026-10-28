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
    public static final int MENU_CLAN_INTRINSIC = 7102;
    public static final int MENU_CLAN_AURA = 7103;
    public static final int MENU_SPECTATOR_BET = 7104;
    public static final int MENU_TOP_CLAN = 7105;
    public static final int MENU_LEADER_TACTIC = 7106;
    public static final int MENU_SOLO_ARENA = 7107;
    public static final int MENU_SOLO_CHALLENGE = 7108;
    public static final int MENU_TOP_SOLO = 7109;

    public static final int WIN_INTRINSIC_POINTS = 10;
    public static final int LOSE_INTRINSIC_POINTS = 2;

    public static final int BET_GOLD_BAR_AMOUNT = 5;
    public static final int BET_REWARD_GOLD_BAR = 9; // 180% của 5 Thỏi vàng
    public static final int BET_RUBY_AMOUNT = 200;
    public static final int BET_REWARD_RUBY = 360;   // 180% của 200 Hồng ngọc

    public static final int FEE_TYPE_GOLD_BAR = 1; // 20 Thỏi vàng (Bang) / 10 Thỏi vàng (Cá nhân)
    public static final int FEE_TYPE_RUBY = 2;     // 1.000 Hồng ngọc (Bang) / 500 Hồng ngọc (Cá nhân)

    public static final int FEE_GOLD_BAR_AMOUNT = 20;
    public static final int FEE_RUBY_AMOUNT = 1000;
    public static final int REWARD_GOLD_BAR_AMOUNT = 30; // 150% của 20 Thỏi vàng
    public static final int REWARD_RUBY_AMOUNT = 1500;   // 150% của 1.000 Hồng ngọc

    public static final int SOLO_FEE_GOLD_BAR_AMOUNT = 10;
    public static final int SOLO_FEE_RUBY_AMOUNT = 500;
    public static final int SOLO_REWARD_GOLD_BAR_AMOUNT = 15; // 150% của 10 Thỏi vàng
    public static final int SOLO_REWARD_RUBY_AMOUNT = 750;    // 150% của 500 Hồng ngọc

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
        public final boolean isSolo;

        public ClanRegistration(int clanId, String clanName, long leaderId, String leaderName,
                List<Long> fighterIds, List<String> fighterNames, int feeType) {
            this(clanId, clanName, leaderId, leaderName, fighterIds, fighterNames, feeType, false);
        }

        public ClanRegistration(int clanId, String clanName, long leaderId, String leaderName,
                List<Long> fighterIds, List<String> fighterNames, int feeType, boolean isSolo) {
            this.clanId = clanId;
            this.clanName = clanName;
            this.leaderId = leaderId;
            this.leaderName = leaderName;
            this.fighterIds = new ArrayList<>(fighterIds);
            this.fighterNames = new ArrayList<>(fighterNames);
            this.feeType = feeType;
            this.registeredAt = System.currentTimeMillis();
            this.isSolo = isSolo;
        }
    }

    public static class SoloChallengeInvite {
        public final long challengerId;
        public final String challengerName;
        public final long targetId;
        public final int feeType;
        public final long createdAt;

        public SoloChallengeInvite(long challengerId, String challengerName, long targetId, int feeType) {
            this.challengerId = challengerId;
            this.challengerName = challengerName;
            this.targetId = targetId;
            this.feeType = feeType;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private final List<ClanRegistration> waitingQueue = new CopyOnWriteArrayList<>();
    private final List<ClanRegistration> soloWaitingQueue = new CopyOnWriteArrayList<>();
    private final List<ClanArenaMatch> activeMatches = new CopyOnWriteArrayList<>();
    private final Map<Integer, List<Long>> draftLineups = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> candidateCacheByLeader = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> soloChallengeTargetCache = new ConcurrentHashMap<>();
    private final Map<Long, Integer> soloChallengeFeeTypeCache = new ConcurrentHashMap<>();
    private final Map<Long, SoloChallengeInvite> pendingSoloInvitesByTarget = new ConcurrentHashMap<>();
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

            // 1b. Dọn hàng chờ PK Cá Nhân nếu người chơi rời khỏi map 134
            for (ClanRegistration sReg : new ArrayList<>(soloWaitingQueue)) {
                if (!hasAnyAvailableFighterInArena(sReg)) {
                    soloWaitingQueue.remove(sReg);
                    refundRegistrationFee(sReg, "Đã hủy đăng ký PK Cá Nhân 1vs1 do bạn rời khỏi võ đài (Đã hoàn trả 100% lệ phí).");
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

            // 2b. Tự động ghép ngẫu nhiên 2 đấu thủ PK Cá Nhân (1vs1) đang chờ
            if (soloWaitingQueue.size() >= 2) {
                List<ClanRegistration> shuffledSolo = new ArrayList<>(soloWaitingQueue);
                Collections.shuffle(shuffledSolo);
                ClanRegistration sReg1 = shuffledSolo.get(0);
                ClanRegistration sReg2 = shuffledSolo.get(1);
                soloWaitingQueue.remove(sReg1);
                soloWaitingQueue.remove(sReg2);
                startMatchBetween(sReg1, sReg2);
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

        // Đưa các đấu thủ / thành viên đang đứng ở map 134 về cùng khu vực thi đấu để theo dõi & lên sàn
        if (reg1.isSolo) {
            moveFighterInArenaToZone(reg1.leaderId, matchZone);
        } else {
            moveClanMembersInArenaToZone(reg1.clanId, matchZone);
        }
        if (reg2.isSolo) {
            moveFighterInArenaToZone(reg2.leaderId, matchZone);
        } else {
            moveClanMembersInArenaToZone(reg2.clanId, matchZone);
        }

        ClanArenaMatch match = new ClanArenaMatch(reg1, reg2, matchZone);
        activeMatches.add(match);
    }

    private void moveFighterInArenaToZone(long playerId, Zone targetZone) {
        if (targetZone == null) {
            return;
        }
        Player pl = Client.gI().getPlayer(playerId);
        if (pl != null && pl.zone != null && pl.zone.map != null && pl.zone.map.mapId == MAP_ID
                && pl.zone.zoneId != targetZone.zoneId) {
            ChangeMapService.gI().changeZone(pl, targetZone.zoneId);
        }
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
            if (!m.isSoloMatch() && (m.reg1.clanId == clanId || m.reg2.clanId == clanId)) {
                return m;
            }
        }
        return null;
    }

    public ClanArenaMatch getActiveSoloMatchOfPlayer(long playerId) {
        for (ClanArenaMatch m : activeMatches) {
            if (m.isSoloMatch() && (m.reg1.leaderId == playerId || m.reg2.leaderId == playerId)) {
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

    public ClanRegistration getSoloWaitingRegistration(long playerId) {
        for (ClanRegistration reg : soloWaitingQueue) {
            if (reg.leaderId == playerId) {
                return reg;
            }
        }
        return null;
    }

    public boolean isPlayerBusyInArena(long playerId) {
        if (getSoloWaitingRegistration(playerId) != null) {
            return true;
        }
        for (ClanRegistration reg : waitingQueue) {
            if (reg.fighterIds.contains(playerId)) {
                return true;
            }
        }
        for (ClanArenaMatch m : activeMatches) {
            if (m.reg1.fighterIds.contains(playerId) || m.reg2.fighterIds.contains(playerId)) {
                return true;
            }
        }
        return false;
    }

    public void openMenuTrongTai(Npc npc, Player player) {
        if (player == null) {
            return;
        }
        ClanRegistration soloWait = getSoloWaitingRegistration(player.id);
        String soloBtnLabel = (soloWait != null) ? "PK Cá Nhân\n(Đang chờ)" : "PK Cá Nhân\n(Lôi Đài 1vs1)";

        StringBuilder sb = new StringBuilder();
        sb.append("|7|ĐẠI HỘI VÕ THUẬT BANG HỘI & CÁ NHÂN\n");
        sb.append("|1|• Đặc quyền tại giải: Cường hóa x100 HP & KI, x5 Sức đánh\n");
        sb.append("• Thể thức: Đấu Đội Bang Hội (Liên đấu) hoặc Lôi Đài Cá Nhân (1vs1)\n");
        sb.append("• Luật: Ai chết sẽ thua (Giới hạn 5 phút đếm ngược / hiệp)\n");
        sb.append("• Thưởng: Bên thắng nhận 150% lệ phí + Điểm Sự Kiện, Săn Boss & Nội Tại Bang!\n");
        sb.append("|2|Chờ ghép Bang: ").append(waitingQueue.size())
                .append(" | Chờ 1vs1: ").append(soloWaitingQueue.size())
                .append(" | Đang đấu: ").append(activeMatches.size()).append("\n");

        ClanArenaMatch activeSoloMatch = getActiveSoloMatchOfPlayer(player.id);
        if (activeSoloMatch != null) {
            sb.append("|7|Bạn đang trong trận PK Cá Nhân 1vs1 tại Khu ").append(activeSoloMatch.zone.zoneId).append("!\n");
            sb.append(activeSoloMatch.getStatusSummary()).append("\n");
        } else if (soloWait != null) {
            sb.append("|7|Bạn đang chờ ghép cặp PK Cá Nhân 1vs1 (")
                    .append(soloWait.feeType == FEE_TYPE_GOLD_BAR ? "10 Thỏi vàng" : "500 Hồng ngọc").append(")!\n");
        }

        if (player.clan == null) {
            sb.append("|6|Bạn chưa có Bang hội. Bạn có thể tham gia ngay [PK Cá Nhân (Lôi Đài 1vs1)] hoặc Cổ vũ Đặt cược!");
            npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                    soloBtnLabel, "Cổ vũ\nĐặt cược", "BXH\nBang Hội", "Thể lệ\nGiải đấu", "Về\nĐảo Kame", "Đóng");
            return;
        }

        Clan clan = player.clan;
        sb.append("|2|Bang [").append(clan.name).append("] - Điểm Nội Tại Bang: ")
                .append(clan.pointNoiTai).append(" điểm (Thắng: ").append(clan.winPKBang).append(" trận)");
        if (clan.isTop1PKBang()) {
            sb.append(" [TOP 1 ĐỆ NHẤT BANG]");
        }
        sb.append("\n");

        ClanArenaMatch activeMatch = getActiveMatchOfClan(clan.id);
        if (activeMatch != null) {
            sb.append("|2|Đang thi đấu với Bang [")
                    .append(activeMatch.getOpponentClanName(clan.id)).append("] tại Khu ")
                    .append(activeMatch.zone.zoneId).append("!\n");
            sb.append(activeMatch.getStatusSummary());
            if (clan.isLeader(player)) {
                npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                        "Thẻ Chiến Thuật\n(1 lần/trận)",
                        soloBtnLabel,
                        "Cổ vũ\nĐặt cược",
                        "Nội Tại\nBang Hội",
                        "Hào Quang\nBang Hội",
                        "BXH\nBang Hội",
                        "Thể lệ\nGiải đấu",
                        "Về\nĐảo Kame");
            } else {
                npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                        soloBtnLabel,
                        "Cổ vũ\nĐặt cược",
                        "Nội Tại\nBang Hội",
                        "Hào Quang\nBang Hội",
                        "BXH\nBang Hội",
                        "Thể lệ\nGiải đấu",
                        "Về\nĐảo Kame");
            }
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
                        "Hủy đăng ký\n(Hoàn 100%)", soloBtnLabel, "Cổ vũ\nĐặt cược", "Nội Tại\nBang Hội", "Hào Quang\nBang Hội", "BXH\nBang Hội", "Thể lệ\nGiải đấu", "Về\nĐảo Kame");
            } else {
                npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                        soloBtnLabel, "Cổ vũ\nĐặt cược", "Nội Tại\nBang Hội", "Hào Quang\nBang Hội", "BXH\nBang Hội", "Thể lệ\nGiải đấu", "Về\nĐảo Kame");
            }
            return;
        }

        if (clan.isLeader(player)) {
            List<Long> draft = getCleanedDraftLineup(clan, player);
            if (draft.isEmpty()) {
                sb.append("|6|Thứ tự thi đấu Bang: Chưa chọn (Bấm 'Chọn đội hình' để đấu Bang, hoặc bấm 'PK Cá Nhân' để đấu 1vs1)");
            } else {
                sb.append("|2|Thứ tự lên sàn đã chọn (").append(draft.size()).append(" người):\n")
                        .append(formatDraftLineup(draft));
            }
            npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                    soloBtnLabel,
                    "Chọn đội hình\n(" + draft.size() + " người)",
                    "ĐK Bang Hội\n(20 Thỏi vàng)",
                    "ĐK Bang Hội\n(1.000 Hồng ngọc)",
                    "Nội Tại\nBang Hội",
                    "Hào Quang\nBang Hội",
                    "Cổ vũ\nĐặt cược",
                    "BXH\nBang Hội",
                    "Thể lệ\nGiải đấu",
                    "Về\nĐảo Kame");
        } else {
            List<Long> draft = draftLineups.getOrDefault(clan.id, Collections.emptyList());
            sb.append("|1|Bạn có thể bấm [PK Cá Nhân (Lôi Đài 1vs1)] để thi đấu đơn ngay, hoặc chờ Bang Chủ đăng ký đấu Bang.\n");
            if (!draft.isEmpty()) {
                sb.append("Đội hình Bang dự kiến: ").append(formatDraftLineup(draft));
            }
            npc.createOtherMenu(player, MENU_MAIN, sb.toString(),
                    soloBtnLabel, "Cổ vũ\nĐặt cược", "Nội Tại\nBang Hội", "Hào Quang\nBang Hội", "BXH\nBang Hội", "Thể lệ\nGiải đấu", "Về\nĐảo Kame");
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
        if (indexMenu == MENU_CLAN_INTRINSIC) {
            handleClanIntrinsicConfirm(npc, player, select);
            return;
        }
        if (indexMenu == MENU_CLAN_AURA) {
            handleClanAuraConfirm(npc, player, select);
            return;
        }
        if (indexMenu == MENU_SPECTATOR_BET) {
            handleSpectatorBetConfirm(npc, player, select);
            return;
        }
        if (indexMenu == MENU_TOP_CLAN) {
            openMenuTrongTai(npc, player);
            return;
        }
        if (indexMenu == MENU_LEADER_TACTIC) {
            handleLeaderTacticConfirm(npc, player, select);
            return;
        }
        if (indexMenu == MENU_SOLO_ARENA) {
            handleSoloArenaConfirm(npc, player, select);
            return;
        }
        if (indexMenu == MENU_SOLO_CHALLENGE) {
            handleSoloChallengeSelectConfirm(npc, player, select);
            return;
        }
        if (indexMenu == MENU_TOP_SOLO) {
            openSoloArenaMenu(npc, player);
            return;
        }
        if (indexMenu != MENU_MAIN) {
            return;
        }

        if (player.clan == null) {
            switch (select) {
                case 0 -> openSoloArenaMenu(npc, player);
                case 1 -> openSpectatorBetMenu(npc, player);
                case 2 -> openTopClanMenu(npc, player);
                case 3 -> showTutorial(npc, player);
                case 4 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
            }
            return;
        }

        Clan clan = player.clan;
        ClanArenaMatch activeMatch = getActiveMatchOfClan(clan.id);
        if (activeMatch != null) {
            if (clan.isLeader(player)) {
                switch (select) {
                    case 0 -> openLeaderTacticMenu(npc, player, activeMatch);
                    case 1 -> openSoloArenaMenu(npc, player);
                    case 2 -> openSpectatorBetMenu(npc, player);
                    case 3 -> openClanIntrinsicMenu(npc, player);
                    case 4 -> openClanAuraMenu(npc, player);
                    case 5 -> openTopClanMenu(npc, player);
                    case 6 -> showTutorial(npc, player);
                    case 7 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
                }
            } else {
                switch (select) {
                    case 0 -> openSoloArenaMenu(npc, player);
                    case 1 -> openSpectatorBetMenu(npc, player);
                    case 2 -> openClanIntrinsicMenu(npc, player);
                    case 3 -> openClanAuraMenu(npc, player);
                    case 4 -> openTopClanMenu(npc, player);
                    case 5 -> showTutorial(npc, player);
                    case 6 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
                }
            }
            return;
        }

        ClanRegistration waitingReg = getWaitingRegistration(clan.id);
        if (waitingReg != null) {
            if (clan.isLeader(player)) {
                switch (select) {
                    case 0 -> cancelRegistration(npc, player, waitingReg);
                    case 1 -> openSoloArenaMenu(npc, player);
                    case 2 -> openSpectatorBetMenu(npc, player);
                    case 3 -> openClanIntrinsicMenu(npc, player);
                    case 4 -> openClanAuraMenu(npc, player);
                    case 5 -> openTopClanMenu(npc, player);
                    case 6 -> showTutorial(npc, player);
                    case 7 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
                }
            } else {
                switch (select) {
                    case 0 -> openSoloArenaMenu(npc, player);
                    case 1 -> openSpectatorBetMenu(npc, player);
                    case 2 -> openClanIntrinsicMenu(npc, player);
                    case 3 -> openClanAuraMenu(npc, player);
                    case 4 -> openTopClanMenu(npc, player);
                    case 5 -> showTutorial(npc, player);
                    case 6 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
                }
            }
            return;
        }

        if (clan.isLeader(player)) {
            switch (select) {
                case 0 -> openSoloArenaMenu(npc, player);
                case 1 -> openLineupSelectionMenu(npc, player);
                case 2 -> registerClan(npc, player, FEE_TYPE_GOLD_BAR);
                case 3 -> registerClan(npc, player, FEE_TYPE_RUBY);
                case 4 -> openClanIntrinsicMenu(npc, player);
                case 5 -> openClanAuraMenu(npc, player);
                case 6 -> openSpectatorBetMenu(npc, player);
                case 7 -> openTopClanMenu(npc, player);
                case 8 -> showTutorial(npc, player);
                case 9 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
            }
        } else {
            switch (select) {
                case 0 -> openSoloArenaMenu(npc, player);
                case 1 -> openSpectatorBetMenu(npc, player);
                case 2 -> openClanIntrinsicMenu(npc, player);
                case 3 -> openClanAuraMenu(npc, player);
                case 4 -> openTopClanMenu(npc, player);
                case 5 -> showTutorial(npc, player);
                case 6 -> ChangeMapService.gI().changeMapNonSpaceship(player, 5, 200, 288);
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
                int amount = reg.isSolo ? SOLO_FEE_GOLD_BAR_AMOUNT : FEE_GOLD_BAR_AMOUNT;
                Item goldBar = ItemService.gI().createNewItem((short) 457, amount);
                InventoryService.gI().addItemBag(receiver, goldBar);
                InventoryService.gI().sendItemBags(receiver);
            } else {
                int amount = reg.isSolo ? SOLO_FEE_RUBY_AMOUNT : FEE_RUBY_AMOUNT;
                receiver.inventory.ruby += amount;
                PlayerService.gI().sendInfoHpMpMoney(receiver);
            }
            Service.gI().sendThongBao(receiver, reasonMsg);
        }
    }

    private SoloChallengeInvite getValidSoloInviteFor(Player target) {
        if (target == null) {
            return null;
        }
        SoloChallengeInvite invite = pendingSoloInvitesByTarget.get(target.id);
        if (invite == null) {
            return null;
        }
        if (System.currentTimeMillis() - invite.createdAt > 90_000L) {
            pendingSoloInvitesByTarget.remove(target.id);
            return null;
        }
        Player challenger = Client.gI().getPlayer(invite.challengerId);
        if (challenger == null || challenger.zone == null || challenger.zone.map == null
                || challenger.zone.map.mapId != MAP_ID || isPlayerBusyInArena(challenger.id)) {
            pendingSoloInvitesByTarget.remove(target.id);
            return null;
        }
        return invite;
    }

    private void openSoloArenaMenu(Npc npc, Player player) {
        if (player == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("|7|LÔI ĐÀI PK CÁ NHÂN (1 VS 1)\n");
        sb.append("|1|• Đặc quyền trên đài: Cường hóa x100 HP & KI, x5 Sức đánh, cấm Đậu thần & hồi sinh\n");
        sb.append("• Thể thức: Đấu đơn 1vs1 (Tự do cho mọi người chơi, kể cả không có Bang hoặc cùng Bang)\n");
        sb.append("• Lệ phí: 10 Thỏi vàng (Thắng nhận 15 TV) hoặc 500 Hồng ngọc (Thắng nhận 750 HN)\n");
        sb.append("• Thưởng thêm khi thắng: +15 Điểm SK, +15 Điểm Săn Boss, +1 Thắng Đại Hội (+3 Điểm Nội Tại Bang nếu có Bang)\n");
        sb.append("|2|Thành tích Đại Hội của bạn: ").append(player.martialArtsTournamentWins)
                .append(" trận thắng | Hàng chờ 1vs1: ").append(soloWaitingQueue.size()).append(" người\n");

        SoloChallengeInvite invite = getValidSoloInviteFor(player);
        if (invite != null) {
            String feeTxt = (invite.feeType == FEE_TYPE_GOLD_BAR) ? (SOLO_FEE_GOLD_BAR_AMOUNT + " Thỏi vàng") : (SOLO_FEE_RUBY_AMOUNT + " Hồng ngọc");
            sb.append("|7|⚔ LỜI THÁCH ĐẤU: [").append(invite.challengerName).append("] đang thách đấu 1vs1 với bạn (Cược: ").append(feeTxt).append(")!\n");
        }

        ClanRegistration mySoloWait = getSoloWaitingRegistration(player.id);
        ArrayList<String> buttons = new ArrayList<>();
        if (invite != null) {
            String feeShort = (invite.feeType == FEE_TYPE_GOLD_BAR) ? (SOLO_FEE_GOLD_BAR_AMOUNT + " TV") : (SOLO_FEE_RUBY_AMOUNT + " HN");
            buttons.add("Chấp nhận\n" + invite.challengerName + "\n(" + feeShort + ")");
        }

        if (mySoloWait != null) {
            sb.append("|2|Bạn ĐÃ ĐĂNG KÝ hàng chờ 1vs1 (")
                    .append(mySoloWait.feeType == FEE_TYPE_GOLD_BAR ? "10 Thỏi vàng" : "500 Hồng ngọc")
                    .append("). Đang chờ đối thủ tiếp theo...");
            buttons.add("Hủy đăng ký\n(Hoàn 100%)");
            buttons.add("BXH Cao Thủ\nCá Nhân");
            buttons.add("Quay lại");
        } else {
            buttons.add("Ghép 1vs1\n(10 Thỏi vàng)");
            buttons.add("Ghép 1vs1\n(500 Hồng ngọc)");
            buttons.add("Thách đấu\n(10 Thỏi vàng)");
            buttons.add("Thách đấu\n(500 Hồng ngọc)");
            buttons.add("BXH Cao Thủ\nCá Nhân");
            buttons.add("Quay lại");
        }

        npc.createOtherMenu(player, MENU_SOLO_ARENA, sb.toString(), buttons.toArray(String[]::new));
    }

    private void handleSoloArenaConfirm(Npc npc, Player player, int select) {
        if (player == null) {
            return;
        }
        SoloChallengeInvite invite = getValidSoloInviteFor(player);
        ClanRegistration mySoloWait = getSoloWaitingRegistration(player.id);
        int offset = 0;

        if (invite != null) {
            if (select == 0) {
                acceptSoloChallenge(npc, player, invite);
                return;
            }
            offset = 1;
        }

        int adj = select - offset;
        if (mySoloWait != null) {
            switch (adj) {
                case 0 -> cancelSoloRegistration(npc, player, mySoloWait);
                case 1 -> openTopSoloMenu(npc, player);
                default -> openMenuTrongTai(npc, player);
            }
        } else {
            switch (adj) {
                case 0 -> registerSolo(npc, player, FEE_TYPE_GOLD_BAR);
                case 1 -> registerSolo(npc, player, FEE_TYPE_RUBY);
                case 2 -> openSoloChallengeSelectMenu(npc, player, FEE_TYPE_GOLD_BAR);
                case 3 -> openSoloChallengeSelectMenu(npc, player, FEE_TYPE_RUBY);
                case 4 -> openTopSoloMenu(npc, player);
                default -> openMenuTrongTai(npc, player);
            }
        }
    }

    private synchronized void registerSolo(Npc npc, Player player, int feeType) {
        if (player == null) {
            return;
        }
        if (isPlayerBusyInArena(player.id)) {
            Service.gI().sendThongBao(player, "Bạn đã đăng ký trong hàng chờ hoặc đang có trận đấu trên võ đài!");
            return;
        }
        if (!deductSoloFee(player, feeType)) {
            return;
        }

        ClanRegistration sReg = new ClanRegistration(
                (int) (-player.id),
                player.name,
                player.id,
                player.name,
                Collections.singletonList(player.id),
                Collections.singletonList(player.name),
                feeType,
                true
        );
        soloWaitingQueue.add(sReg);

        String feeStr = (feeType == FEE_TYPE_GOLD_BAR) ? (SOLO_FEE_GOLD_BAR_AMOUNT + " Thỏi vàng") : (SOLO_FEE_RUBY_AMOUNT + " Hồng ngọc");
        Service.gI().sendThongBao(player, "Đăng ký PK Cá Nhân 1vs1 thành công (" + feeStr + ")! Đang chờ đối thủ...");
        if (player.zone != null && player.zone.getNpc() != null) {
            Service.gI().chat(player.zone.getNpc(), "Cao thủ [" + player.name + "] vừa đăng ký Lôi Đài Cá Nhân 1vs1 (" + feeStr + ")! Ai dám lên đài tỉ thí?");
        }
        openSoloArenaMenu(npc, player);
    }

    private synchronized void cancelSoloRegistration(Npc npc, Player player, ClanRegistration sReg) {
        if (soloWaitingQueue.remove(sReg)) {
            refundRegistrationFee(sReg, "Bạn đã hủy đăng ký PK Cá Nhân 1vs1 và nhận lại 100% lệ phí.");
            openSoloArenaMenu(npc, player);
        } else {
            Service.gI().sendThongBao(player, "Bạn đã được ghép trận, không thể hủy đăng ký!");
        }
    }

    private boolean deductSoloFee(Player player, int feeType) {
        if (feeType == FEE_TYPE_GOLD_BAR) {
            Item goldBar = InventoryService.gI().findItemBag(player, 457);
            if (goldBar == null || goldBar.quantity < SOLO_FEE_GOLD_BAR_AMOUNT) {
                Service.gI().sendThongBao(player, "Bạn không đủ " + SOLO_FEE_GOLD_BAR_AMOUNT + " Thỏi vàng để tham gia PK Cá Nhân 1vs1!");
                return false;
            }
            InventoryService.gI().subQuantityItemsBag(player, goldBar, SOLO_FEE_GOLD_BAR_AMOUNT);
            InventoryService.gI().sendItemBags(player);
            return true;
        } else {
            if (player.inventory.ruby < SOLO_FEE_RUBY_AMOUNT) {
                Service.gI().sendThongBao(player, "Bạn không đủ " + Util.numberToMoney(SOLO_FEE_RUBY_AMOUNT) + " Hồng ngọc để tham gia PK Cá Nhân 1vs1!");
                return false;
            }
            player.inventory.ruby -= SOLO_FEE_RUBY_AMOUNT;
            PlayerService.gI().sendInfoHpMpMoney(player);
            return true;
        }
    }

    private void openSoloChallengeSelectMenu(Npc npc, Player challenger, int feeType) {
        if (challenger == null) {
            return;
        }
        if (isPlayerBusyInArena(challenger.id)) {
            Service.gI().sendThongBao(challenger, "Bạn đang trong hàng chờ hoặc đang thi đấu!");
            return;
        }
        List<Player> candidates = new ArrayList<>();
        for (Player pl : Client.gI().getPlayers()) {
            if (pl != null && pl.isPl() && pl.id != challenger.id
                    && pl.zone != null && pl.zone.map != null && pl.zone.map.mapId == MAP_ID
                    && !isPlayerBusyInArena(pl.id)) {
                candidates.add(pl);
            }
        }
        if (candidates.isEmpty()) {
            Service.gI().sendThongBao(challenger, "Hiện không có người chơi rảnh rỗi nào khác trong map Đại Hội để thách đấu! Bạn có thể bấm 'Ghép 1vs1' để chờ đối thủ.");
            openSoloArenaMenu(npc, challenger);
            return;
        }

        List<Long> targetIds = new ArrayList<>();
        ArrayList<String> buttons = new ArrayList<>();
        for (int i = 0; i < candidates.size() && i < 6; i++) {
            Player t = candidates.get(i);
            targetIds.add(t.id);
            buttons.add("Thách đấu\n" + t.name);
        }
        buttons.add("Quay lại");
        soloChallengeTargetCache.put(challenger.id, targetIds);
        soloChallengeFeeTypeCache.put(challenger.id, feeType);

        String feeTxt = (feeType == FEE_TYPE_GOLD_BAR) ? (SOLO_FEE_GOLD_BAR_AMOUNT + " Thỏi vàng") : (SOLO_FEE_RUBY_AMOUNT + " Hồng ngọc");
        StringBuilder sb = new StringBuilder();
        sb.append("|7|CHỌN ĐỐI THỦ THÁCH ĐẤU 1VS1\n");
        sb.append("|2|Mức cược thách đấu: ").append(feeTxt).append(" (Người thắng nhận 150%!)\n");
        sb.append("|1|Chọn tên người chơi đang có mặt tại võ đài để gửi lời mời tỉ thí trực tiếp:");
        npc.createOtherMenu(challenger, MENU_SOLO_CHALLENGE, sb.toString(), buttons.toArray(String[]::new));
    }

    private void handleSoloChallengeSelectConfirm(Npc npc, Player challenger, int select) {
        if (challenger == null) {
            return;
        }
        List<Long> targetIds = soloChallengeTargetCache.getOrDefault(challenger.id, Collections.emptyList());
        int feeType = soloChallengeFeeTypeCache.getOrDefault(challenger.id, FEE_TYPE_GOLD_BAR);
        if (select < 0 || select >= targetIds.size()) {
            openSoloArenaMenu(npc, challenger);
            return;
        }
        long targetId = targetIds.get(select);
        Player target = Client.gI().getPlayer(targetId);
        if (target == null || target.zone == null || target.zone.map == null || target.zone.map.mapId != MAP_ID) {
            Service.gI().sendThongBao(challenger, "Đối thủ đã rời khỏi map Đại Hội Võ Thuật!");
            openSoloArenaMenu(npc, challenger);
            return;
        }
        if (isPlayerBusyInArena(target.id)) {
            Service.gI().sendThongBao(challenger, "Đối thủ đang trong trận đấu hoặc hàng chờ khác!");
            openSoloArenaMenu(npc, challenger);
            return;
        }

        SoloChallengeInvite invite = new SoloChallengeInvite(challenger.id, challenger.name, target.id, feeType);
        pendingSoloInvitesByTarget.put(target.id, invite);

        String feeTxt = (feeType == FEE_TYPE_GOLD_BAR) ? (SOLO_FEE_GOLD_BAR_AMOUNT + " Thỏi vàng") : (SOLO_FEE_RUBY_AMOUNT + " Hồng ngọc");
        Service.gI().sendThongBao(challenger, "Đã gửi lời thách đấu 1vs1 (" + feeTxt + ") tới [" + target.name + "]! Hãy chờ đối phương xác nhận tại Trọng Tài.");
        Service.gI().sendThongBao(target, "⚔ Cao thủ [" + challenger.name + "] vừa gửi lời thách đấu 1vs1 (" + feeTxt + ") cho bạn! Bấm vào Trọng Tài -> PK Cá Nhân để chấp nhận!");
        if (challenger.zone != null && challenger.zone.getNpc() != null) {
            Service.gI().chat(challenger.zone.getNpc(), "⚔ [" + challenger.name + "] vừa gửi chiến thư 1vs1 (" + feeTxt + ") tới [" + target.name + "]! Dám nhận lời không?");
        }
    }

    private synchronized void acceptSoloChallenge(Npc npc, Player target, SoloChallengeInvite invite) {
        pendingSoloInvitesByTarget.remove(target.id);
        Player challenger = Client.gI().getPlayer(invite.challengerId);
        if (challenger == null || challenger.zone == null || challenger.zone.map == null || challenger.zone.map.mapId != MAP_ID) {
            Service.gI().sendThongBao(target, "Người thách đấu đã rời khỏi võ đài!");
            return;
        }
        if (isPlayerBusyInArena(challenger.id) || isPlayerBusyInArena(target.id)) {
            Service.gI().sendThongBao(target, "Một trong hai đấu thủ đang bận trong trận đấu hoặc hàng chờ khác!");
            return;
        }
        if (!deductSoloFee(challenger, invite.feeType)) {
            Service.gI().sendThongBao(target, "Người thách đấu [" + challenger.name + "] không còn đủ lệ phí để bắt đầu trận đấu!");
            return;
        }
        if (!deductSoloFee(target, invite.feeType)) {
            // Hoàn lại cho challenger nếu target không đủ lệ phí
            ClanRegistration tempRefund = new ClanRegistration((int) (-challenger.id), challenger.name,
                    challenger.id, challenger.name, Collections.singletonList(challenger.id),
                    Collections.singletonList(challenger.name), invite.feeType, true);
            refundRegistrationFee(tempRefund, "Đối thủ [" + target.name + "] không đủ lệ phí nhận lời thách đấu, đã hoàn lại 100% lệ phí cho bạn.");
            return;
        }

        ClanRegistration reg1 = new ClanRegistration(
                (int) (-challenger.id), challenger.name, challenger.id, challenger.name,
                Collections.singletonList(challenger.id), Collections.singletonList(challenger.name),
                invite.feeType, true
        );
        ClanRegistration reg2 = new ClanRegistration(
                (int) (-target.id), target.name, target.id, target.name,
                Collections.singletonList(target.id), Collections.singletonList(target.name),
                invite.feeType, true
        );
        startMatchBetween(reg1, reg2);
    }

    private void openTopSoloMenu(Npc npc, Player player) {
        List<Player> onlinePlayers = new ArrayList<>(Client.gI().getPlayers());
        onlinePlayers.removeIf(p -> p == null || !p.isPl());
        onlinePlayers.sort((a, b) -> Long.compare(b.martialArtsTournamentWins, a.martialArtsTournamentWins));

        StringBuilder sb = new StringBuilder();
        sb.append("|7|BẢNG XẾP HẠNG CAO THỦ ĐẠI HỘI (CÁ NHÂN)\n");
        sb.append("|2|Thành tích của bạn: ").append(player.martialArtsTournamentWins).append(" trận thắng\n");
        sb.append("|1|(Mỗi trận thắng PK Cá Nhân 1vs1 hoặc thắng hiệp PK Bang Hội đều được +1 trận thắng Đại Hội và tính vào Nhiệm vụ KOL!)\n");
        if (onlinePlayers.isEmpty()) {
            sb.append("Chưa có dữ liệu cao thủ.");
        } else {
            int limit = Math.min(10, onlinePlayers.size());
            for (int i = 0; i < limit; i++) {
                Player p = onlinePlayers.get(i);
                String clanTag = (p.clan != null) ? (" [Bang " + p.clan.name + "]") : " [Tự do]";
                String prefix = (i == 0 && p.martialArtsTournamentWins > 0) ? "|7|[TOP 1 CAO THỦ] " : ("|1|Hạng " + (i + 1) + ": ");
                sb.append(prefix).append(p.name).append(clanTag)
                        .append(" - Thắng: ").append(p.martialArtsTournamentWins).append(" trận\n");
            }
        }
        npc.createOtherMenu(player, MENU_TOP_SOLO, sb.toString(), "Quay lại");
    }

    private void rewardWinnerSolo(ClanRegistration winReg, ClanRegistration loseReg) {
        Player winner = Client.gI().getPlayer(winReg.leaderId);
        Player loser = Client.gI().getPlayer(loseReg.leaderId);

        String rewardText;
        if (winReg.feeType == FEE_TYPE_GOLD_BAR) {
            rewardText = SOLO_REWARD_GOLD_BAR_AMOUNT + " Thỏi vàng (150%)";
            if (winner != null) {
                Item goldBar = ItemService.gI().createNewItem((short) 457, SOLO_REWARD_GOLD_BAR_AMOUNT);
                InventoryService.gI().addItemBag(winner, goldBar);
                InventoryService.gI().sendItemBags(winner);
            }
        } else {
            rewardText = Util.numberToMoney(SOLO_REWARD_RUBY_AMOUNT) + " Hồng ngọc (150%)";
            if (winner != null) {
                winner.inventory.ruby += SOLO_REWARD_RUBY_AMOUNT;
                PlayerService.gI().sendInfoHpMpMoney(winner);
            }
        }

        if (winner != null) {
            if (winner.event != null) {
                winner.event.addEventPoint(15);
            }
            winner.pointSanBoss += 15;
            if (winner.clan != null) {
                winner.clan.pointNoiTai += 3;
                winner.clan.saveNoiTaiBang();
            }
            Service.gI().sendThongBao(winner, "Chúc mừng bạn chiến thắng Lôi Đài 1vs1! Nhận " + rewardText + ", +15 Điểm SK, +15 Điểm Săn Boss"
                    + (winner.clan != null ? " & +3 Điểm Nội Tại Bang!" : "!"));
        }

        if (loser != null) {
            if (loser.event != null) {
                loser.event.addEventPoint(5);
            }
            loser.pointSanBoss += 5;
            if (loser.clan != null) {
                loser.clan.pointNoiTai += 1;
                loser.clan.saveNoiTaiBang();
            }
            Service.gI().sendThongBao(loser, "Bạn đã thất bại trước [" + winReg.leaderName + "] tại Lôi Đài 1vs1 và nhận khuyến khích +5 Điểm SK, +5 Điểm Săn Boss!");
        }

        ServerNotify.gI().notify("Lôi Đài Cá Nhân 1vs1: Cao thủ [" + winReg.leaderName + "] đã đánh bại [" + loseReg.leaderName + "], nhận " + rewardText + " & +15 Điểm SK/Boss!");
    }

    private int getIntrinsicUpgradeCost(int currentLv) {
        return 5 + currentLv * 5;
    }

    private void openClanIntrinsicMenu(Npc npc, Player player) {
        if (player.clan == null) {
            return;
        }
        Clan c = player.clan;
        StringBuilder sb = new StringBuilder();
        sb.append("|7|NỘI TẠI BANG HỘI - [").append(c.name).append("]\n");
        sb.append("|2|Điểm Nội Tại Bang hiện có: ").append(c.pointNoiTai).append(" điểm\n");
        sb.append("(Thắng PK Bang Hội: +").append(WIN_INTRINSIC_POINTS).append(" điểm | Tham gia: +").append(LOSE_INTRINSIC_POINTS).append(" điểm)\n");
        sb.append("|1|• HP Gốc: Cấp ").append(c.lvHpAdd).append("/20 (+").append(Util.numberToMoney(c.lvHpAdd * 50_000L)).append(" HP)\n");
        sb.append("• KI Gốc: Cấp ").append(c.lvMpAdd).append("/20 (+").append(Util.numberToMoney(c.lvMpAdd * 50_000L)).append(" KI)\n");
        sb.append("• SĐ Gốc: Cấp ").append(c.lvDameAdd).append("/20 (+").append(Util.numberToMoney(c.lvDameAdd * 2_500L)).append(" SĐ)\n");
        sb.append("• % HP: Cấp ").append(c.lvTlHp).append("/15 (+").append(c.lvTlHp * 2).append("% HP)\n");
        sb.append("• % KI: Cấp ").append(c.lvTlMp).append("/15 (+").append(c.lvTlMp * 2).append("% KI)\n");
        sb.append("• % Sức Đánh: Cấp ").append(c.lvTlDame).append("/15 (+").append(c.lvTlDame * 2).append("% SĐ)\n");
        sb.append("• % TNSM & Vàng: Cấp ").append(c.lvTlTNSM).append("/15 (+").append(c.lvTlTNSM * 5).append("% TNSM & Vàng)\n");
        sb.append("• % Chí Mạng: Cấp ").append(c.lvCrit).append("/10 (+").append(c.lvCrit).append("% CRIT)");

        if (!c.isLeader(player)) {
            npc.createOtherMenu(player, MENU_CLAN_INTRINSIC, sb.toString(), "Quay lại");
            return;
        }

        npc.createOtherMenu(player, MENU_CLAN_INTRINSIC, sb.toString(),
                c.lvHpAdd < 20 ? "Nâng HP\n(" + getIntrinsicUpgradeCost(c.lvHpAdd) + " đ)" : "HP Gốc\n(Đã Max)",
                c.lvMpAdd < 20 ? "Nâng KI\n(" + getIntrinsicUpgradeCost(c.lvMpAdd) + " đ)" : "KI Gốc\n(Đã Max)",
                c.lvDameAdd < 20 ? "Nâng SĐ\n(" + getIntrinsicUpgradeCost(c.lvDameAdd) + " đ)" : "SĐ Gốc\n(Đã Max)",
                c.lvTlHp < 15 ? "Nâng %HP\n(" + getIntrinsicUpgradeCost(c.lvTlHp) + " đ)" : "%HP\n(Đã Max)",
                c.lvTlMp < 15 ? "Nâng %KI\n(" + getIntrinsicUpgradeCost(c.lvTlMp) + " đ)" : "%KI\n(Đã Max)",
                c.lvTlDame < 15 ? "Nâng %SĐ\n(" + getIntrinsicUpgradeCost(c.lvTlDame) + " đ)" : "%SĐ\n(Đã Max)",
                c.lvTlTNSM < 15 ? "Nâng TNSM\n& Vàng (" + getIntrinsicUpgradeCost(c.lvTlTNSM) + " đ)" : "TNSM & Vàng\n(Đã Max)",
                c.lvCrit < 10 ? "Nâng %CRIT\n(" + getIntrinsicUpgradeCost(c.lvCrit) + " đ)" : "%CRIT\n(Đã Max)",
                "Quay lại");
    }

    private void handleClanIntrinsicConfirm(Npc npc, Player player, int select) {
        if (player.clan == null || !player.clan.isLeader(player) || select < 0 || select >= 8) {
            openMenuTrongTai(npc, player);
            return;
        }
        Clan c = player.clan;
        int curLv;
        int maxLv;
        String statName;
        switch (select) {
            case 0 -> { curLv = c.lvHpAdd; maxLv = 20; statName = "HP Gốc (+50.000 HP)"; }
            case 1 -> { curLv = c.lvMpAdd; maxLv = 20; statName = "KI Gốc (+50.000 KI)"; }
            case 2 -> { curLv = c.lvDameAdd; maxLv = 20; statName = "Sức Đánh Gốc (+2.500 SĐ)"; }
            case 3 -> { curLv = c.lvTlHp; maxLv = 15; statName = "% HP (+2% HP)"; }
            case 4 -> { curLv = c.lvTlMp; maxLv = 15; statName = "% KI (+2% KI)"; }
            case 5 -> { curLv = c.lvTlDame; maxLv = 15; statName = "% Sức Đánh (+2% SĐ)"; }
            case 6 -> { curLv = c.lvTlTNSM; maxLv = 15; statName = "% TNSM & Vàng (+5%)"; }
            default -> { curLv = c.lvCrit; maxLv = 10; statName = "% Chí Mạng (+1% CRIT)"; }
        }
        if (curLv >= maxLv) {
            Service.gI().sendThongBao(player, "Chỉ số " + statName + " của Bang đã đạt cấp tối đa (" + maxLv + ")!");
            openClanIntrinsicMenu(npc, player);
            return;
        }
        int cost = getIntrinsicUpgradeCost(curLv);
        if (c.pointNoiTai < cost) {
            Service.gI().sendThongBao(player, "Bang không đủ Điểm Nội Tại! Cần " + cost + " điểm (Hiện có: " + c.pointNoiTai + " điểm).");
            openClanIntrinsicMenu(npc, player);
            return;
        }

        c.pointNoiTai -= cost;
        switch (select) {
            case 0 -> c.lvHpAdd++;
            case 1 -> c.lvMpAdd++;
            case 2 -> c.lvDameAdd++;
            case 3 -> c.lvTlHp++;
            case 4 -> c.lvTlMp++;
            case 5 -> c.lvTlDame++;
            case 6 -> c.lvTlTNSM++;
            case 7 -> c.lvCrit++;
        }
        c.saveNoiTaiBang();
        c.refreshAllMembersStatsAndAura();
        notifyClanMembers(c.id, "Bang Chủ " + player.name + " đã nâng cấp Nội Tại Bang [" + statName + "] lên Cấp " + (curLv + 1) + "!");
        openClanIntrinsicMenu(npc, player);
    }

    private static final int[] CLAN_AURA_COSTS = {20, 50, 100, 200, 350};
    private static final String[] CLAN_AURA_NAMES = {
        "Hào Quang Tân Tinh (+2% HP, KI, SĐ)",
        "Hào Quang Thanh Long (+4% HP, KI, SĐ)",
        "Hào Quang Kim Quang (+6% HP, KI, SĐ)",
        "Hào Quang Hắc Ám (+8% HP, KI, SĐ)",
        "Hào Quang Bá Vương (+12% HP, KI, SĐ)"
    };

    private void openClanAuraMenu(Npc npc, Player player) {
        if (player.clan == null) {
            return;
        }
        Clan c = player.clan;
        String curAura = (c.levelHaoQuangBang > 0 && c.levelHaoQuangBang <= 5)
                ? ("Cấp " + c.levelHaoQuangBang + " - " + CLAN_AURA_NAMES[c.levelHaoQuangBang - 1])
                : "Chưa mở khóa";
        StringBuilder sb = new StringBuilder();
        sb.append("|7|HÀO QUANG BANG HỘI - [").append(c.name).append("]\n");
        sb.append("|2|Điểm Nội Tại Bang hiện có: ").append(c.pointNoiTai).append(" điểm\n");
        sb.append("Hào Quang Bang đang kích hoạt: ").append(curAura).append("\n");
        sb.append("|1|(Tất cả thành viên trong Bang đều phát sáng Hào Quang và nhận chỉ số!)\n");
        for (int i = 0; i < 5; i++) {
            sb.append("• Cấp ").append(i + 1).append(" (").append(CLAN_AURA_COSTS[i]).append(" điểm): ")
                    .append(CLAN_AURA_NAMES[i]).append("\n");
        }

        if (!c.isLeader(player)) {
            npc.createOtherMenu(player, MENU_CLAN_AURA, sb.toString(), "Quay lại");
            return;
        }

        npc.createOtherMenu(player, MENU_CLAN_AURA, sb.toString(),
                "Hào Quang\nCấp 1 (20đ)",
                "Hào Quang\nCấp 2 (50đ)",
                "Hào Quang\nCấp 3 (100đ)",
                "Hào Quang\nCấp 4 (200đ)",
                "Hào Quang\nCấp 5 (350đ)",
                "Quay lại");
    }

    private void handleClanAuraConfirm(Npc npc, Player player, int select) {
        if (player.clan == null || !player.clan.isLeader(player) || select < 0 || select >= 5) {
            openMenuTrongTai(npc, player);
            return;
        }
        Clan c = player.clan;
        int targetLv = select + 1;
        if (c.levelHaoQuangBang >= targetLv) {
            Service.gI().sendThongBao(player, "Bang của bạn đã sở hữu Hào Quang Bang Cấp " + c.levelHaoQuangBang + " rồi!");
            openClanAuraMenu(npc, player);
            return;
        }
        int cost = CLAN_AURA_COSTS[select];
        if (c.pointNoiTai < cost) {
            Service.gI().sendThongBao(player, "Bang không đủ Điểm Nội Tại! Cần " + cost + " điểm (Hiện có: " + c.pointNoiTai + " điểm).");
            openClanAuraMenu(npc, player);
            return;
        }
        c.pointNoiTai -= cost;
        c.levelHaoQuangBang = targetLv;
        c.saveNoiTaiBang();
        c.refreshAllMembersStatsAndAura();
        String auraName = CLAN_AURA_NAMES[select];
        notifyClanMembers(c.id, "Chúc mừng! Bang Chủ " + player.name + " đã mở khóa [" + auraName + "] cho toàn bộ thành viên Bang!");
        ServerNotify.gI().notify("Bang [" + c.name + "] vừa kích hoạt " + auraName + " rực rỡ khắp máy chủ!");
        openClanAuraMenu(npc, player);
    }

    private void rewardWinnerClan(ClanRegistration winReg, ClanRegistration loseReg) {
        try {
            Clan winClan = nro.models.server.Manager.getClanById(winReg.clanId);
            if (winClan != null) {
                winClan.winPKBang++;
                winClan.pointNoiTai += WIN_INTRINSIC_POINTS;
                winClan.saveNoiTaiBang();
            }
        } catch (Exception ignored) {
        }
        try {
            Clan loseClan = nro.models.server.Manager.getClanById(loseReg.clanId);
            if (loseClan != null) {
                loseClan.pointNoiTai += LOSE_INTRINSIC_POINTS;
                loseClan.saveNoiTaiBang();
            }
        } catch (Exception ignored) {
        }
        try {
            // Cập nhật lại chỉ số cho toàn bộ các Bang để hiệu ứng Top 1 Đệ Nhất Bang được áp dụng ngay
            for (Clan c : nro.models.server.Manager.CLANS) {
                if (c != null) {
                    c.refreshAllMembersStatsAndAura();
                }
            }
        } catch (Exception ignored) {
        }

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
            Service.gI().sendThongBao(receiver, "Chúc mừng Bang [" + winReg.clanName + "] chiến thắng! Nhận " + rewardText + " & +" + WIN_INTRINSIC_POINTS + " Điểm Nội Tại Bang!");
        }
        notifyClanMembers(winReg.clanId, "Bang [" + winReg.clanName + "] đã chiến thắng! Nhận +" + WIN_INTRINSIC_POINTS + " Điểm Nội Tại Bang & " + rewardText + " (trao cho Bang Chủ)!");
        notifyClanMembers(loseReg.clanId, "Bang [" + loseReg.clanName + "] đã thất bại trước Bang [" + winReg.clanName + "] và nhận +" + LOSE_INTRINSIC_POINTS + " Điểm Nội Tại Bang khuyến khích!");
        ServerNotify.gI().notify("Đại Hội Võ Thuật Bang Hội: Bang [" + winReg.clanName + "] đã đánh bại Bang [" + loseReg.clanName + "], nhận 150% lệ phí & +" + WIN_INTRINSIC_POINTS + " Điểm Nội Tại Bang!");
    }

    private void openTopClanMenu(Npc npc, Player player) {
        List<Clan> sorted = new ArrayList<>(nro.models.server.Manager.CLANS);
        sorted.sort((a, b) -> {
            if (b.winPKBang != a.winPKBang) {
                return Integer.compare(b.winPKBang, a.winPKBang);
            }
            if (b.levelHaoQuangBang != a.levelHaoQuangBang) {
                return Integer.compare(b.levelHaoQuangBang, a.levelHaoQuangBang);
            }
            return Integer.compare(b.pointNoiTai, a.pointNoiTai);
        });

        StringBuilder sb = new StringBuilder();
        sb.append("|7|BẢNG XẾP HẠNG THIÊN HẠ ĐỆ NHẤT BANG\n");
        sb.append("|2|Đặc quyền Bang TOP 1: Toàn bộ TV nhận +5% HP, +5% KI, +5% SĐ, +10% TNSM!\n");
        if (sorted.isEmpty()) {
            sb.append("|1|Hiện chưa có Bang hội nào trên Bảng xếp hạng.");
        } else {
            int limit = Math.min(10, sorted.size());
            for (int i = 0; i < limit; i++) {
                Clan c = sorted.get(i);
                String prefix = (i == 0 && c.winPKBang > 0) ? "|7|[TOP 1 - ĐỆ NHẤT BANG] " : ("|1|Hạng " + (i + 1) + ": ");
                sb.append(prefix).append("Bang [").append(c.name).append("] - Thắng: ")
                        .append(c.winPKBang).append(" trận | Hào Quang Cấp ").append(c.levelHaoQuangBang)
                        .append(" | Nội Tại: ").append(c.pointNoiTai).append("đ\n");
            }
        }
        npc.createOtherMenu(player, MENU_TOP_CLAN, sb.toString(), "Quay lại");
    }

    private ClanArenaMatch getMatchForSpectator(Player player) {
        if (player == null) {
            return null;
        }
        ClanArenaMatch m = getActiveMatchInZone(player.zone);
        if (m == null && !activeMatches.isEmpty()) {
            m = activeMatches.get(0);
        }
        return m;
    }

    private void openSpectatorBetMenu(Npc npc, Player player) {
        ClanArenaMatch match = getMatchForSpectator(player);
        if (match == null) {
            Service.gI().sendThongBao(player, "Hiện tại chưa có trận đấu nào đang diễn ra trên võ đài để cổ vũ / đặt cược!");
            return;
        }
        String sideLabel = match.isSoloMatch() ? "Đấu thủ " : "Bang ";
        StringBuilder sb = new StringBuilder();
        sb.append("|7|CỔ VŨ & ĐẶT CƯỢC KHÁN ĐÀI (KHU ").append(match.zone.zoneId).append(")\n");
        sb.append("|2|Cặp đấu: ").append(sideLabel).append("[").append(match.reg1.clanName).append("] VS ")
                .append(sideLabel).append("[").append(match.reg2.clanName).append("]\n");
        sb.append("|1|• Mức cược: 5 Thỏi vàng (Thắng nhận 9 Thỏi vàng - 180%)\n");
        sb.append("• Hoặc: 200 Hồng ngọc (Thắng nhận 360 Hồng ngọc - 180%)\n");
        sb.append("• Lưu ý: Chỉ nhận đặt cược trước khi Hiệp 1 kết thúc!\n");

        SpectatorBet existing = match.spectatorBets.get(player.id);
        if (existing != null) {
            sb.append("|2|Bạn đã đặt cược cổ vũ cho ").append(sideLabel).append("[").append(existing.targetClanName).append("] (")
                    .append(existing.betType == FEE_TYPE_GOLD_BAR ? "5 Thỏi vàng" : "200 Hồng ngọc").append(")!");
            npc.createOtherMenu(player, MENU_SPECTATOR_BET, sb.toString(), "Quay lại");
            return;
        }

        if (!match.canAcceptSpectatorBet()) {
            sb.append("|6|Trận đấu đã qua Hiệp 1, cổng đặt cược của trận này đã đóng!");
            npc.createOtherMenu(player, MENU_SPECTATOR_BET, sb.toString(), "Quay lại");
            return;
        }

        npc.createOtherMenu(player, MENU_SPECTATOR_BET, sb.toString(),
                "Cược [" + match.reg1.clanName + "]\n(5 Thỏi vàng)",
                "Cược [" + match.reg1.clanName + "]\n(200 Hồng ngọc)",
                "Cược [" + match.reg2.clanName + "]\n(5 Thỏi vàng)",
                "Cược [" + match.reg2.clanName + "]\n(200 Hồng ngọc)",
                "Quay lại");
    }

    private void handleSpectatorBetConfirm(Npc npc, Player player, int select) {
        ClanArenaMatch match = getMatchForSpectator(player);
        if (match == null || select < 0 || select > 3) {
            openMenuTrongTai(npc, player);
            return;
        }
        if (match.spectatorBets.containsKey(player.id)) {
            Service.gI().sendThongBao(player, "Bạn đã đặt cược trong trận đấu này rồi!");
            openMenuTrongTai(npc, player);
            return;
        }
        if (!match.canAcceptSpectatorBet()) {
            Service.gI().sendThongBao(player, "Đã hết thời gian đặt cược (Chỉ được cược trước khi Hiệp 1 kết thúc)!");
            openMenuTrongTai(npc, player);
            return;
        }

        ClanRegistration targetReg = (select == 0 || select == 1) ? match.reg1 : match.reg2;
        int betType = (select == 0 || select == 2) ? FEE_TYPE_GOLD_BAR : FEE_TYPE_RUBY;

        // Không cho phép đấu thủ đang tham chiến tự đặt cược cho đối phương (chống bán độ)
        if ((match.reg1.fighterIds.contains(player.id) && targetReg == match.reg2)
                || (match.reg2.fighterIds.contains(player.id) && targetReg == match.reg1)) {
            Service.gI().sendThongBao(player, "Bạn là đấu thủ trong trận này, chỉ có thể đặt cược cho chính phe của mình!");
            return;
        }

        if (betType == FEE_TYPE_GOLD_BAR) {
            Item goldBar = InventoryService.gI().findItemBag(player, 457);
            if (goldBar == null || goldBar.quantity < BET_GOLD_BAR_AMOUNT) {
                Service.gI().sendThongBao(player, "Bạn cần có ít nhất " + BET_GOLD_BAR_AMOUNT + " Thỏi vàng để đặt cược!");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, goldBar, BET_GOLD_BAR_AMOUNT);
            InventoryService.gI().sendItemBags(player);
        } else {
            if (player.inventory.ruby < BET_RUBY_AMOUNT) {
                Service.gI().sendThongBao(player, "Bạn cần có ít nhất " + BET_RUBY_AMOUNT + " Hồng ngọc để đặt cược!");
                return;
            }
            player.inventory.ruby -= BET_RUBY_AMOUNT;
            PlayerService.gI().sendInfoHpMpMoney(player);
        }

        String sideLabel = match.isSoloMatch() ? "Đấu thủ " : "Bang ";
        SpectatorBet bet = new SpectatorBet(player.id, player.name, targetReg.clanId, targetReg.clanName, betType);
        match.spectatorBets.put(player.id, bet);
        String betStr = (betType == FEE_TYPE_GOLD_BAR) ? "5 Thỏi vàng" : "200 Hồng ngọc";
        Service.gI().sendThongBao(player, "Đã đặt cược " + betStr + " cổ vũ cho " + sideLabel + "[" + targetReg.clanName + "]! Dự đoán đúng nhận 180%!");
        match.broadcastInZone("Khán giả " + player.name + " vừa đặt cược " + betStr + " cổ vũ cho " + sideLabel + "[" + targetReg.clanName + "]!");
        openSpectatorBetMenu(npc, player);
    }

    private void openLeaderTacticMenu(Npc npc, Player leader, ClanArenaMatch match) {
        if (leader.clan == null || !leader.clan.isLeader(leader) || match == null) {
            openMenuTrongTai(npc, leader);
            return;
        }
        boolean used = match.hasUsedLeaderTactic(leader.clan.id);
        StringBuilder sb = new StringBuilder();
        sb.append("|7|THẺ CHIẾN THUẬT CỦA BANG CHỦ (1 LẦN / TRẬN)\n");
        sb.append("|1|• Quyền trợ giúp 1 - Tiếp Sức Bang Hội: Hồi ngay +35% HP & KI tối đa và xóa toàn bộ thời gian hồi chiêu cho đấu thủ đang trên sàn.\n");
        sb.append("• Quyền trợ giúp 2 - Đổi Tướng Chiến Thuật: Rút đấu thủ đang gặp bất lợi trên sàn xuống cuối hàng chờ và cử người tiếp theo lên thay thế ngay lập tức.\n");
        sb.append("|2|Trạng thái: ").append(used ? "ĐÃ SỬ DỤNG TRONG TRẬN NÀY" : "SẴN SÀNG SỬ DỤNG (Còn 1 lần)");

        if (used) {
            npc.createOtherMenu(leader, MENU_LEADER_TACTIC, sb.toString(), "Quay lại");
        } else {
            npc.createOtherMenu(leader, MENU_LEADER_TACTIC, sb.toString(),
                    "Tiếp Sức\n(+35% HP/KI)",
                    "Đổi Tướng\nChiến Thuật",
                    "Quay lại");
        }
    }

    private void handleLeaderTacticConfirm(Npc npc, Player leader, int select) {
        if (leader.clan == null || !leader.clan.isLeader(leader)) {
            openMenuTrongTai(npc, leader);
            return;
        }
        ClanArenaMatch match = getActiveMatchOfClan(leader.clan.id);
        if (match == null || match.hasUsedLeaderTactic(leader.clan.id) || (select != 0 && select != 1)) {
            openMenuTrongTai(npc, leader);
            return;
        }
        if (select == 0) {
            match.executeLeaderTacticHeal(leader);
        } else {
            match.executeLeaderTacticSwap(leader);
        }
    }

    private void notifyClanMembers(int clanId, String msg) {
        for (Player pl : Client.gI().getPlayers()) {
            if (pl != null && pl.clan != null && pl.clan.id == clanId) {
                Service.gI().sendThongBao(pl, msg);
            }
        }
    }

    private void showTutorial(Npc npc, Player player) {
        String tutorial = "THỂ LỆ ĐẠI HỘI VÕ THUẬT BANG HỘI & CÁ NHÂN:\n"
                + "1. Đặc quyền trên sàn đấu: Cường hóa x100 HP & KI, x5 Sức đánh, cấm Đậu thần & hồi sinh tại chỗ, mỗi hiệp 5 phút.\n"
                + "2. Chế độ PK Cá Nhân (1vs1): Bất kỳ người chơi nào cũng có thể đăng ký Ghép 1vs1 ngẫu nhiên hoặc Thách đấu trực tiếp người trong map (10 Thỏi vàng / 500 Hồng ngọc -> Thắng nhận 150% + 15 Điểm SK/Boss + 1 Thắng Đại Hội KOL).\n"
                + "3. Chế độ PK Bang Hội (Đấu Đội): Chủ bang xếp thứ tự thành viên lên sàn (20 Thỏi vàng / 1.000 Hồng ngọc -> Thắng nhận 150% + 10 Điểm Nội Tại Bang).\n"
                + "4. Cơ chế Trảm Tướng (Đấu Bang): Thắng 1 hiệp hồi +25% HP & KI. Hạ 2/3/4+ người liên tiếp nhận thưởng nóng Liên Trảm.\n"
                + "5. Bang Chủ có 1 Thẻ Chiến Thuật / trận (Tiếp Sức +35% HP/KI hoặc Đổi Tướng).\n"
                + "6. Khán giả đứng dưới đài được Đặt cược cổ vũ (ăn 180%). Bang Top 1 BXH nhận danh hiệu Thiên Hạ Đệ Nhất Bang (+5% HP/KI/SĐ, +10% TNSM)!";
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

    public static class SpectatorBet {
        public final long playerId;
        public final String playerName;
        public final int targetClanId;
        public final String targetClanName;
        public final int betType;

        public SpectatorBet(long playerId, String playerName, int targetClanId, String targetClanName, int betType) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.targetClanId = targetClanId;
            this.targetClanName = targetClanName;
            this.betType = betType;
        }
    }

    /**
     * Phiên thi đấu giữa 2 Bang hội hoặc 2 Đấu thủ Cá nhân trên sàn đấu.
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

        private boolean usedTacticClan1 = false;
        private boolean usedTacticClan2 = false;
        private final Map<Long, Integer> winStreaks = new ConcurrentHashMap<>();
        public final Map<Long, SpectatorBet> spectatorBets = new ConcurrentHashMap<>();

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

        public boolean isSoloMatch() {
            return reg1.isSolo || reg2.isSolo;
        }

        private void initMatch() {
            if (isSoloMatch()) {
                broadcastInZone("Cặp đấu Lôi Đài Cá Nhân (1vs1) đã được ghép: [" + reg1.leaderName + "] VS [" + reg2.leaderName + "]! Khán giả có thể bấm vào Trọng Tài để Cổ vũ / Đặt cược (nhận 180%)!");
            } else {
                broadcastInZone("Cặp đấu PK Bang Hội đã được ghép: Bang [" + reg1.clanName + "] VS Bang [" + reg2.clanName + "]! Khán giả có thể bấm vào Trọng Tài để Cổ vũ / Đặt cược (nhận 180%)!");
            }
            prepareNextRound(true, true);
        }

        public boolean canAcceptSpectatorBet() {
            return state != STATE_FINISHED && roundNumber <= 1 && (state == STATE_PREPARE_ROUND || state == STATE_FIGHTING);
        }

        public boolean hasUsedLeaderTactic(int clanId) {
            return (clanId == reg1.clanId) ? usedTacticClan1 : usedTacticClan2;
        }

        public void executeLeaderTacticHeal(Player leader) {
            boolean isClan1 = (leader.clan.id == reg1.clanId);
            Player myFighter = isClan1 ? fighter1 : fighter2;
            if (state != STATE_FIGHTING || myFighter == null || !isFighterPresentInZone(myFighter) || myFighter.isDie()) {
                Service.gI().sendThongBao(leader, "Chỉ có thể dùng Tiếp Sức khi đấu thủ của Bang đang giao chiến trên sàn!");
                return;
            }
            if (isClan1) {
                usedTacticClan1 = true;
            } else {
                usedTacticClan2 = true;
            }
            long addHp = Math.max(1, (long) (myFighter.nPoint.hpMax * 0.35));
            long addMp = Math.max(1, (long) (myFighter.nPoint.mpMax * 0.35));
            myFighter.nPoint.setHp(Math.min(myFighter.nPoint.hpMax, myFighter.nPoint.hp + addHp));
            myFighter.nPoint.setMp(Math.min(myFighter.nPoint.mpMax, myFighter.nPoint.mp + addMp));
            EffectSkillService.gI().removeStun(myFighter);
            Service.gI().releaseCooldownSkill(myFighter);
            PlayerService.gI().sendInfoHpMp(myFighter);

            String msg = "⚡ THẺ CHIẾN THUẬT [TIẾP SỨC]: Bang Chủ " + leader.name + " đã tiếp sức cho " + myFighter.name + " hồi +35% HP & KI và xóa toàn bộ hồi chiêu!";
            refereeChat(msg);
            broadcastInZone(msg);
        }

        public void executeLeaderTacticSwap(Player leader) {
            boolean isClan1 = (leader.clan.id == reg1.clanId);
            ClanRegistration myReg = isClan1 ? reg1 : reg2;
            Player myFighter = isClan1 ? fighter1 : fighter2;
            int myIdx = isClan1 ? index1 : index2;

            if (state != STATE_FIGHTING || myFighter == null || !isFighterPresentInZone(myFighter) || myFighter.isDie()) {
                Service.gI().sendThongBao(leader, "Chỉ có thể Đổi Tướng khi đấu thủ của Bang đang thi đấu trên sàn!");
                return;
            }
            if (myIdx + 1 >= myReg.fighterIds.size()) {
                Service.gI().sendThongBao(leader, "Bang của bạn không còn đấu thủ dự bị nào phía sau để đổi tướng!");
                return;
            }
            if (isClan1) {
                usedTacticClan1 = true;
            } else {
                usedTacticClan2 = true;
            }

            stopCurrentPvp();
            ItemTimeService.gI().sendItemTime(myFighter, 3779, 0);
            myFighter.isPKDHVT = false;
            myFighter.lostByDeath = false;
            Service.gI().setPos(myFighter, Util.nextInt(250, 480), 360);

            // Đưa đấu thủ vừa rút xuống cuối hàng chờ để bảo toàn mạng và cử người kế tiếp lên
            myReg.fighterIds.add(myFighter.id);
            myReg.fighterNames.add(myFighter.name);
            if (isClan1) {
                index1++;
                fighter1 = null;
            } else {
                index2++;
                fighter2 = null;
            }

            String msg = "🔄 THẺ CHIẾN THUẬT [ĐỔI TƯỚNG]: Bang Chủ " + leader.name + " đã rút " + myFighter.name + " về cuối hàng chờ và tung đấu thủ tiếp theo lên sàn!";
            refereeChat(msg);
            broadcastInZone(msg);

            state = STATE_BETWEEN_ROUNDS;
            stateTimer = 3;
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
                broadcastInZone("Cả 2 bên đều không còn đấu thủ tại võ đài. Trận đấu kết thúc hòa!");
                refundRegistrationFee(reg1, "Trận đấu kết thúc hòa, hoàn trả 100% lệ phí.");
                refundRegistrationFee(reg2, "Trận đấu kết thúc hòa, hoàn trả 100% lệ phí.");
                refundSpectatorBets();
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
                    if (isSoloMatch()) {
                        refereeChat("Lôi Đài Cá Nhân 1vs1: [" + fighter1.name + "] VS [" + fighter2.name + "]!");
                    } else {
                        refereeChat("Hiệp " + roundNumber + ": [" + reg1.clanName + "] " + fighter1.name
                                + " (#" + (index1 + 1) + ") VS [" + reg2.clanName + "] " + fighter2.name + " (#" + (index2 + 1) + ")!");
                    }
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

            if (loser != null) {
                winStreaks.remove(loser.id);
                if (loser.zone != null) {
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
            }

            if (winner != null && winner.zone != null) {
                winner.martialArtsTournamentWins++;
                ItemTimeService.gI().sendItemTime(winner, 3779, 0);
                if (!isSoloMatch()) {
                    // Cơ chế Trảm Tướng: Hồi 25% HP & 25% KI sau mỗi hiệp thắng
                    long healHp = Math.max(1, winner.nPoint.hpMax / 4);
                    long healMp = Math.max(1, winner.nPoint.mpMax / 4);
                    winner.nPoint.setHp(Math.min(winner.nPoint.hpMax, winner.nPoint.hp + healHp));
                    winner.nPoint.setMp(Math.min(winner.nPoint.mpMax, winner.nPoint.mp + healMp));
                    PlayerService.gI().sendInfoHpMp(winner);

                    int streak = winStreaks.getOrDefault(winner.id, 0) + 1;
                    winStreaks.put(winner.id, streak);
                    rewardWinStreak(winner, streak);
                }
            }

            if (loser == fighter1) {
                fighter1 = null;
            } else if (loser == fighter2) {
                fighter2 = null;
            }

            state = STATE_BETWEEN_ROUNDS;
            stateTimer = 4;
        }

        private void rewardWinStreak(Player winner, int streak) {
            if (streak == 1) {
                Service.gI().sendThongBao(winner, "Thắng Hiệp " + roundNumber + "! Bạn được hồi phục +25% HP & KI để nghênh đón đối thủ tiếp theo!");
                return;
            }
            String clanName = winner.clan != null ? winner.clan.name : "";
            if (streak == 2) {
                if (winner.event != null) {
                    winner.event.addEventPoint(10);
                }
                winner.pointSanBoss += 10;
                String msg = "🔥 SONG SÁT! " + winner.name + " (Bang [" + clanName + "]) hạ gục 2 đấu thủ liên tiếp, hồi 25% HP/KI và nhận nóng +10 Điểm Sự Kiện, +10 Điểm Săn Boss!";
                refereeChat(msg);
                broadcastInZone(msg);
            } else if (streak == 3) {
                if (winner.event != null) {
                    winner.event.addEventPoint(25);
                }
                winner.pointSanBoss += 25;
                winner.inventory.ruby += 100;
                PlayerService.gI().sendInfoHpMpMoney(winner);
                if (winner.clan != null) {
                    winner.clan.pointNoiTai += 3;
                    winner.clan.saveNoiTaiBang();
                }
                String msg = "⚡ TAM LIÊN TRẢM! " + winner.name + " (Bang [" + clanName + "]) càn quét 3 đối thủ liên tiếp, nhận nóng +100 Hồng ngọc, +25 Điểm SK/Boss & +3 Điểm Nội Tại Bang!";
                refereeChat(msg);
                broadcastInZone(msg);
                ServerNotify.gI().notify(msg);
            } else if (streak >= 4) {
                if (winner.event != null) {
                    winner.event.addEventPoint(50);
                }
                winner.pointSanBoss += 50;
                winner.inventory.ruby += 200;
                PlayerService.gI().sendInfoHpMpMoney(winner);
                if (winner.clan != null) {
                    winner.clan.pointNoiTai += 5;
                    winner.clan.saveNoiTaiBang();
                }
                String msg = "👑 ĐỘC CÔ CẦU BẠI! " + winner.name + " (Bang [" + clanName + "]) đạt chuỗi " + streak + " LIÊN TRẢM không đối thủ, nhận nóng +200 Hồng ngọc, +50 Điểm SK/Boss & +5 Điểm Nội Tại Bang!";
                refereeChat(msg);
                broadcastInZone(msg);
                ServerNotify.gI().notify(msg);
            }
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
            String winMsg = isSoloMatch()
                    ? ("CHUNG CUỘC LÔI ĐÀI 1VS1: Cao thủ [" + winReg.leaderName + "] đã xuất sắc đánh bại [" + loseReg.leaderName + "] và giành chiến thắng!")
                    : ("CHUNG CUỘC: Bang [" + winReg.clanName + "] đã xuất sắc đánh bại Bang [" + loseReg.clanName + "] và giành chức Vô Địch PK Bang Hội!");
            refereeChat(winMsg);
            broadcastInZone(winMsg);

            resetFighterAfterMatch(fighter1);
            resetFighterAfterMatch(fighter2);

            if (isSoloMatch()) {
                rewardWinnerSolo(winReg, loseReg);
            } else {
                rewardWinnerClan(winReg, loseReg);
            }
            settleSpectatorBets(winReg.clanId, winReg.clanName);
            cleanupMatch();
        }

        private void settleSpectatorBets(int winClanId, String winClanName) {
            String sideLabel = isSoloMatch() ? "Đấu thủ " : "Bang ";
            for (SpectatorBet bet : spectatorBets.values()) {
                Player bettor = Client.gI().getPlayer(bet.playerId);
                if (bettor == null) {
                    continue;
                }
                if (bet.targetClanId == winClanId) {
                    if (bet.betType == FEE_TYPE_GOLD_BAR) {
                        Item goldBar = ItemService.gI().createNewItem((short) 457, BET_REWARD_GOLD_BAR);
                        InventoryService.gI().addItemBag(bettor, goldBar);
                        InventoryService.gI().sendItemBags(bettor);
                        Service.gI().sendThongBao(bettor, "Dự đoán chính xác " + sideLabel + "[" + winClanName + "] chiến thắng! Bạn nhận được " + BET_REWARD_GOLD_BAR + " Thỏi vàng (180%)!");
                    } else {
                        bettor.inventory.ruby += BET_REWARD_RUBY;
                        PlayerService.gI().sendInfoHpMpMoney(bettor);
                        Service.gI().sendThongBao(bettor, "Dự đoán chính xác " + sideLabel + "[" + winClanName + "] chiến thắng! Bạn nhận được " + BET_REWARD_RUBY + " Hồng ngọc (180%)!");
                    }
                } else {
                    Service.gI().sendThongBao(bettor, "Rất tiếc! " + sideLabel + "[" + bet.targetClanName + "] bạn cổ vũ đã thất bại. Chúc bạn may mắn ở trận tiếp theo!");
                }
            }
            spectatorBets.clear();
        }

        private void refundSpectatorBets() {
            for (SpectatorBet bet : spectatorBets.values()) {
                Player bettor = Client.gI().getPlayer(bet.playerId);
                if (bettor == null) {
                    continue;
                }
                if (bet.betType == FEE_TYPE_GOLD_BAR) {
                    Item goldBar = ItemService.gI().createNewItem((short) 457, BET_GOLD_BAR_AMOUNT);
                    InventoryService.gI().addItemBag(bettor, goldBar);
                    InventoryService.gI().sendItemBags(bettor);
                } else {
                    bettor.inventory.ruby += BET_RUBY_AMOUNT;
                    PlayerService.gI().sendInfoHpMpMoney(bettor);
                }
                Service.gI().sendThongBao(bettor, "Trận đấu kết thúc hòa, đã hoàn trả 100% tiền đặt cược cổ vũ cho bạn!");
            }
            spectatorBets.clear();
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
