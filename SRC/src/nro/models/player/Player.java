package nro.models.player;

import java.time.LocalDateTime;
import nro.models.utils.Functions;
import nro.models.npc.NonInteractiveNPC;
import nro.models.radar.Card;
import nro.models.radar.RadarCard;
import nro.models.services.RadarService;
import nro.models.services_dungeon.MajinBuuService;
import nro.models.skill.PlayerSkill;
import java.util.List;
import nro.models.clan.Clan;
import nro.models.intrinsic.IntrinsicPlayer;
import nro.models.item.Item;
import nro.models.item.ItemTime;
import nro.models.npc.MagicTree;
import nro.models.consts.ConstPlayer;
import nro.models.consts.ConstTask;
import nro.models.npc.MabuEgg;
import nro.models.mob.MobMe;
import nro.models.data.DataGame;
import nro.models.clan.ClanMember;
import nro.models.consts.ConstAchievement;
import nro.models.map.Zone;
import nro.models.interfaces.IPVP;
import nro.models.matches.TYPE_LOSE_PVP;
import nro.models.skill.Skill;
import nro.models.services.Service;
import nro.models.network.MySession;
import nro.models.task.TaskPlayer;
import nro.models.network.Message;
import nro.models.server.Client;
import nro.models.services.EffectSkillService;
import nro.models.services.FlagBagService;
import nro.models.services.FriendAndEnemyService;
import nro.models.map.service.MapService;
import nro.models.services.PetService;
import nro.models.services.PlayerService;
import nro.models.services.TaskService;
import nro.models.map.service.ChangeMapService;
import nro.models.combine.Combine;
import nro.models.consts.ConstDailyGift;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import nro.models.services_dungeon.BlackBallWarService;
import nro.models.matches.giai_dau.The23rdMartialArtCongressManager;
import nro.models.map.ItemMap;
import nro.models.map.MaBuHold;
import nro.models.map.phoban.MajinBuu14H;
import nro.models.services_dungeon.SuperDivineWaterService;
import nro.models.matches.dai_hoi_vo_thuat.The23rdMartialArtCongress;
import nro.models.daily_Giftcode.DailyGiftData;
import nro.models.daily_Giftcode.DailyGiftService;
import nro.models.services.InventoryService;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.server.Maintenance;
import nro.models.services.shenron.Shenron_Event;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import nro.models.minigame.ChonAiDay_Gem;
import nro.models.minigame.ChonAiDay_Gold;
import nro.models.npc.DuaHauEgg;
import nro.models.player_badges.Badges;
import nro.models.player_badges.BadgesData;
import nro.models.services.ItemTimeService;
import nro.models.task.BadgesTask;
import nro.models.task.BadgesTaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class Player implements Runnable {

    public long lastTimeEatPea;
    public boolean isAutoBuffPea = true;
    public boolean isAutoCleanBag = false;
    public long lastTimeAutoCleanBag;
    public boolean isAutoHuntBoss = false;
    public int pendingAutoBossIndex = -1;
    public int targetAutoBossIndex = -1;
    public long targetAutoBossId = -1;
    public String targetAutoBossName = "";
    public int autoHuntScanZone = 0;
    public long lastTimeAutoHuntAction;
    public long lastTimeAutoHuntAttack;
    public long lastTimeAutoHuntWaitNotify;
    public boolean hasSentInitialBossNotify = false;
    public long lastTimeSyncPetInfo;
    public Map<Integer, Long> activeEffects = new HashMap<>();
    @Setter
    @Getter
    public MySession session;

    public MySession getSession() {
        return this.session;
    }

    public void setSession(MySession session) {
        this.session = session;
    }
    public long id;
    public String name;
    public byte gender;
    public boolean isNewMember;
    public short head;
    public short body;
    public short leg;
    public int deltaTime;
    public byte typePk;
    public byte cFlag;
    public boolean haveTennisSpaceShip;
    public Badges badges;
    public boolean isBot;
    public PlayerEvent event;
    public boolean isCopy;
    public int point_sukien;
    public int point_sukien1;
    public int point_sukien2;
    public int thachdauwhis = 0;
    public int DuaHau;
    public int point_vuahung;
    public int point_maydam;
    public long total_damage_maydam;
    public boolean powerReduced = false;
    public String originalName;
    public boolean beforeDispose;

    public int mbv = 0;
    public boolean baovetaikhoan;
    public long mbvtime;

    public int timeGohome;

    public long lastUpdateGohomeTime;

    public boolean goHome;

    public long lastPkCommesonTime;

    public boolean callBossPocolo;
    public Zone zoneSieuThanhThuy;
    public boolean winSTT;
    public long lastTimeWinSTT;
    public long lastTimeUpdateSTT;

    public MajinBuu14H maBu2H;
    public boolean isMabuHold;
    public MaBuHold maBuHold;
    public int precentMabuHold;
    public boolean isPhuHoMapMabu;

    public boolean danhanthoivang;
    public long lastRewardGoldBarTime;
    public boolean isMayDoSucManh = false;

    public int timesPerDayBDKB = 0;
    public long lastTimeJoinBDKB;

    public boolean joinCDRD;
    public long lastTimeJoinCDRD;
    public boolean talkToThuongDe;
    public boolean talkToThanMeo;
    public long timeChangeMap144;
    public Date firstTimeLogin;
    public long lastTimeJoinDT;

    public int typeChibi;
    public long lastTimeChibi;
    public long lastTimeUpdateChibi;

    public String captcha = "";
    public boolean doesNotAttack;
    public long lastTimePlayerNotAttack;
    public int timeNotAttack = 1800000;

    public boolean isPet;
    public boolean isNewPet;
    public boolean isNewPet1;
    public boolean isBoss;
    public boolean isPlayer;
    public IPVP pvp;
    public byte maxTime = 30;
    public byte type = 0;
    public boolean isOffline = false;

    public String notify = null;

    public int mapIdBeforeLogout;
    public List<Zone> mapBlackBall;
    public List<Zone> mapMaBu;

    public List<Player> temporaryEnemies = new ArrayList<>();
    public List<String> textRuongGo = new ArrayList<>();
    public Zone zone;
    public Zone mapBeforeCapsule;
    public List<Zone> mapCapsule;
    public Pet pet;
    public NewPet newPet;
    public MobMe mobMe;
    public Location location;
    public SetClothes setClothes;
    public EffectSkill effectSkill;
    public MabuEgg mabuEgg;
    public DuaHauEgg DuaHauEgg;
    public TaskPlayer playerTask;
    public ItemTime itemTime;
    public Fusion fusion;
    public MagicTree magicTree;
    public IntrinsicPlayer playerIntrinsic;
    public Inventory inventory;
    public PlayerSkill playerSkill;
    public Combine combineNew;
    public IDMark idMark;
    public Charms charms;
    public EffectSkin effectSkin;
    public NPoint nPoint;
    public RewardBlackBall rewardBlackBall;
    public FightMabu fightMabu;
    public NewSkill newSkill;
    public Satellite satellite;
    public Achievement achievement;
    public GiftCode giftCode;
    public Traning traning;

    public Clan clan;
    public ClanMember clanMember;

    public List<Friend> friends;
    public List<Enemy> enemies;

    public boolean justRevived;
    public long lastTimeRevived;

    public long timeChangeZone;
    public long lastUseOptionTime;

    public short idNRNM = -1;
    public short idGo = -1;
    public long lastTimePickNRNM;

    public List<Card> Cards = new ArrayList<>();

    public int levelWoodChest;
    public long goldChallenge;
    public long rubyChallenge;
    public long lastTimeRewardWoodChest;
    public List<Item> itemsWoodChest = new ArrayList<>();
    public int indexWoodChest;
    public long lastTimePKDHVT23;

    public boolean lostByDeath;

    public boolean isPKDHVT;

    public int xSend;
    public int ySend;
    public boolean isFly;

    // shenron event
    public long lastTimeShenronAppeared;
    public boolean isShenronAppear;
    public Shenron_Event shenronEvent;

    // vo dai sinh tu
    public long lastTimePKVoDaiSinhTu;
    public boolean haveRewardVDST;
    public int thoiVangVoDaiSinhTu;
    public long timePKVDST;

    public int binhChonHatMit;
    public int binhChonPlayer;
    public Zone zoneBinhChon;

    public ItemEvent itemEvent;

    public int levelLuyenTap;
    public boolean isThachDau;
    public int tnsmLuyenTap;
    public boolean dangKyTapTuDong;
    public long lastTimeOffline;
    public int mapIdDangTapTuDong;
    public int lastMapOffline;
    public int lastZoneOffline;
    public int lastXOffline;
    public String thongBaoTapTuDong;
    public boolean teleTapTuDong;
    public byte vip;
    public long timevip;
    public int timesPerDayCuuSat;
    public long lastTimeCuuSat;
    public boolean nhanVangNangVIP;
    public boolean nhanDeTuNangVIP;
    public boolean nhanSKHVIP;
    public Item itemLoa = null;

    public long totalDamageTaken;
    public boolean thongBaoChangeMap;
    public String textThongBaoChangeMap;
    public boolean thongBaoThua;
    public String textThongBaoThua;
    public SuperRank superRank;

    public int mamcay;
    public long timemamcay;

    public boolean canReward;
    public boolean changeMapVIP;
    public boolean haveReward;

    public int tayThong;

    public List<Item> itemsTradeWVP = new ArrayList<>();
    public long goldTradeWVP;
    public boolean tradeWVP;
    public long plIdWVP;

    private DropItem dropItem;
    public int eventPointType1;
    public int eventPointType2;
    public int eventPointType3;
    public int eventPointType4;
    public int eventPointType5;
    public int eventPointType6;
    public int pointSanBoss = 0;
    public int levelHaoQuang = 0;
    public boolean checkDailyReward;
    public boolean checkTopReward1;
    public boolean checkTopReward2;
    public boolean checkTopReward3;
    private String lastChatMessage;
    public List<BadgesData> dataBadges = new ArrayList<>();
    public List<BadgesTask> dataTaskBadges = new ArrayList<>();
    public long lastTimeChangeBadges;
    public int autoTrainState = 0;
    public List<Integer> BoughtSkill = new ArrayList<>();
    public LearnSkill LearnSkill;
    public List<DailyGiftData> dailyGiftData = new ArrayList<>();
    public int gemNormar;
    public int gemVIP;
    public int goldNormar;
    public int goldVIP;
    public int id_CSMM_Gold;
    public String nameClan;
    public int levelBDKBDone;
    public long timeBDKBDone;
    public long lastTimeUpdateTopBDKB;
    public int levelKhiGasDone;
    public long timeKhiGasDone;
    public long lastTimeUpdateTopKhiGas;
    public int levelCDRDDone;
    public long timeCDRDDone;
    public long lastTimeUpdateTopCDRD;
    public long thoiGianBatDauTrong;
    public int soLuongDuaTrongHomNay;
    public int rewardEggs;
    private long duaHauStartTime;
    public Player menuPlayer;
    public boolean hasReducedPower = false;
    public long originalPower = -1;
    public boolean receivedKilis = false;
    public int kolQuestStage;
    public int kolVIPQuestStage;
    public long martialArtsTournamentWins;
    public long destronGas70CompletionCount;
    public long dailySuperHardQuestCompletionCount;
    public long bossBabyDefeatParticipationCount;
    public long monsterKillCountAutoTrain;
    public int vipPurchaseCount;
    public long lastClanCheckIn = 0;
    public long lastChallengeGauTuongCuopTime;
    public long lastChallengeGauTuopCuopTime;
    public LocalDateTime lastCheckIn;

    public Player() {
        LearnSkill = new LearnSkill();
        lastUseOptionTime = System.currentTimeMillis();
        location = new Location();
        nPoint = new NPoint(this);
        inventory = new Inventory();
        playerSkill = new PlayerSkill(this);
        setClothes = new SetClothes(this);
        effectSkill = new EffectSkill(this);
        fusion = new Fusion(this);
        playerIntrinsic = new IntrinsicPlayer();
        rewardBlackBall = new RewardBlackBall(this);
        fightMabu = new FightMabu(this);
        idMark = new IDMark();
        this.idMark = new IDMark();
        combineNew = new Combine();
        playerTask = new TaskPlayer();
        friends = new ArrayList<>();
        enemies = new ArrayList<>();
        itemTime = new ItemTime(this);
        charms = new Charms(this);
        effectSkin = new EffectSkin(this);
        newSkill = new NewSkill(this);
        satellite = new Satellite();
        achievement = new Achievement(this);
        giftCode = new GiftCode();
        traning = new Traning();
        itemEvent = new ItemEvent(this);
        superRank = new SuperRank(this);
        dropItem = new DropItem(this);
        event = new PlayerEvent(this);
        badges = new Badges();
    }

    //--------------------------------------------------------------------------
    public boolean isDie() {
        if (this.nPoint != null && this.nPoint.hp <= 0) {
            if (this.zone != null && MapService.gI().isMapTuongLai(this.zone.map.mapId) && !this.hasReducedPower) {
                if (this.originalPower == -1) {
                    this.originalPower = this.nPoint.power;
                }
                long reducedPower = (long) (this.originalPower * 0.99);
                this.nPoint.power = reducedPower;
                this.hasReducedPower = true;
            }
            return true;
        }
        return false;
    }

    public void sendMessage(Message msg) {
        if (this.session != null) {
            session.sendMessage(msg);
        }
    }

    public boolean isPl() {
        return isPlayer && !isBot && !isPet && !isBoss && !isNewPet && !isNewPet1 && !(this instanceof NonInteractiveNPC);
    }

    @Override
    public void run() {
        Functions.sleep(500);
        while (!Maintenance.isRunning && session != null && session.isConnected() && this.name != null) {
            long st = System.currentTimeMillis();
            update();
            Functions.sleep(Math.max(1000 - (System.currentTimeMillis() - st), 10));
        }
    }

    public void start() {
        // Đã được cập nhật định kỳ mỗi 1 giây qua Zone.udPlayer() trong Manager.scheduler,
        // không tạo thêm SingleThreadExecutor riêng cho từng player để tránh rò rỉ thread và chạy update() 2 lần/giây.
    }

    public boolean isTrashEquipToAutoSell(Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return false;
        }
        int type = item.template.type;
        if (type < 0 || type > 4) {
            return false;
        }
        int id = item.template.id;
        if (item.template.level >= 13
                || (id >= 555 && id <= 567)
                || (id >= 650 && id <= 662)
                || (id >= 1048 && id <= 1062)) {
            return false;
        }
        if (item.itemOptions != null) {
            for (Item.ItemOption io : item.itemOptions) {
                if (io == null || io.optionTemplate == null) {
                    continue;
                }
                int opId = io.optionTemplate.id;
                if (opId == 107 && io.param > 0) {
                    return false; // Có lỗ sao pha lê
                }
                if ((opId >= 127 && opId <= 144) || (opId >= 237 && opId <= 248) || opId == 210) {
                    return false; // Đồ kích hoạt (SKH)
                }
                if (opId == 72 && io.param > 0) {
                    return false; // Đã ép cấp [+1] trở lên
                }
                if (opId >= 34 && opId <= 36) {
                    return false; // Tinh ấn / Nguyệt ấn / Nhật ấn
                }
            }
        }
        return true;
    }

    public boolean isUnnecessaryJunkItem(Item item) {
        if (item == null || !item.isNotNullItem() || item.template == null) {
            return false;
        }
        int id = item.template.id;
        if (id == 73 && TaskService.gI().getIdTask(this) > ConstTask.TASK_2_0) {
            return true;
        }
        if (item.template.type == 7 && id >= 66 && id <= 72) {
            return true;
        }
        return false;
    }

    public void autoCleanTrashAndSellItems() {
        if (this.inventory == null || this.inventory.itemsBag == null) {
            return;
        }
        boolean cleanJunkToo = InventoryService.gI().getCountEmptyBag(this) <= 3;
        int cleanedCount = 0;
        for (int i = 0; i < this.inventory.itemsBag.size(); i++) {
            Item item = this.inventory.itemsBag.get(i);
            if (isTrashEquipToAutoSell(item) || (cleanJunkToo && isUnnecessaryJunkItem(item))) {
                long goldGain = Math.max(2000L, (long) item.template.gold / 2L) * Math.max(1, item.quantity);
                this.inventory.addGold(goldGain);
                InventoryService.gI().removeItemBag(this, i);
                cleanedCount++;
            }
        }
        if (cleanedCount > 0) {
            InventoryService.gI().sortItems(this.inventory.itemsBag);
            InventoryService.gI().sendItemBags(this);
            Service.gI().sendMoney(this);
        }
    }

    public void update() {
        if (!this.beforeDispose) {
            try {
                if (this.zone != null || (!this.isPl() && this.zone == null)) {
                    if (itemTime != null) {
                        itemTime.update();
                    }
                    if (charms != null) {
                        charms.update();
                    }
                    if (magicTree != null) {
                        magicTree.update();
                    }
                    if (this.zone != null && hasEffect(this, 7143)) {
                        activeEffects.entrySet().removeIf(entry -> System.currentTimeMillis() >= entry.getValue());
                        this.spreadEffectToNearbyPlayers();
                    }
                    if (this.isPl() && this.zone != null && this.zone.map.mapId == this.gender + 21 && (TaskService.gI().getIdTask(this) == ConstTask.TASK_0_0 || TaskService.gI().getIdTask(this) == ConstTask.TASK_0_1)) {
                        this.playerTask.taskMain.index = 2;
                        TaskService.gI().sendTaskMain(this);
                    }
                    if (this.zone != null && this.zone.map.mapId == (21 + this.gender)) {
                        if (this.mabuEgg != null) {
                            this.mabuEgg.sendMabuEgg();
                        }
                        if (this.DuaHauEgg != null) {
                            this.DuaHauEgg.sendDuaHauEgg();
                        }
                    }
                }
                if ((this.zone != null && !MapService.gI().isHome(this.zone.map.mapId)) || (!this.isPl() && this.zone == null)) {
                    if (isPl() && idMark != null && idMark.isBan() && Util.canDoWithTime(idMark.getLastTimeBan(), 5000)) {
                        Client.gI().kickSession(session);
                        return;
                    }
                    if (nPoint != null) {
                        nPoint.update();
                    }
                    if (fusion != null) {
                        fusion.update();
                    }
                    if (effectSkill != null) {
                        effectSkill.update();
                    }
                    if (mobMe != null) {
                        mobMe.update();
                    }
                    if (effectSkin != null) {
                        effectSkin.update();
                    }
                    if (pet != null) {
                        pet.update();
                    }
                    if (this.isPl() && !this.isBot && !this.isDie()) {
                        if (!this.hasSentInitialBossNotify && this.zone != null && this.session != null) {
                            this.hasSentInitialBossNotify = true;
                            nro.models.server.ServerNotify.gI().sendActiveBossesToPlayer(this);
                        }
                        if (this.isAutoBuffPea && Util.canDoWithTime(this.lastTimeEatPea, 3000)) {
                            boolean needBuff = (this.nPoint.hp <= this.nPoint.hpMax * 25L / 100L)
                                    || (this.nPoint.mp <= this.nPoint.mpMax * 25L / 100L)
                                    || (this.nPoint.stamina <= this.nPoint.maxStamina * 20 / 100);
                            if (!needBuff && this.pet != null && !this.pet.isDie() && this.pet.nPoint != null) {
                                needBuff = (this.pet.nPoint.hp <= this.pet.nPoint.hpMax * 25L / 100L)
                                        || (this.pet.nPoint.mp <= this.pet.nPoint.mpMax * 25L / 100L)
                                        || (this.pet.nPoint.stamina <= this.pet.nPoint.maxStamina * 20 / 100);
                            }
                            if (needBuff) {
                                nro.models.services_func.UseItem.gI().eatPea(this);
                            }
                        }
                        if (this.isAutoCleanBag && Util.canDoWithTime(this.lastTimeAutoCleanBag, 5000)) {
                            this.lastTimeAutoCleanBag = System.currentTimeMillis();
                            this.autoCleanTrashAndSellItems();
                        }
                    }
                    if (this.isPl() && !this.isBot && this.isAutoHuntBoss) {
                        this.updateAutoHuntBoss();
                    }
                    if (event != null) {
                        event.update();
                    }
                    if (newPet != null) {
                        newPet.update();
                    }
                    if (satellite != null) {
                        satellite.update();
                    }
                    if (this.nPoint.timeXinbatoBuff + 10_000 < System.currentTimeMillis()) {
                        this.nPoint.tlNeDonBuffXinbato = 0;
                    }
                    if (this.isPl() && !this.isBot && !this.isDie() && this.effectSkill != null && !this.effectSkill.isChibi && Util.canDoWithTime(lastTimeChibi, 180000)) {
                        if (Util.isTrue(10, 100) && !MapService.gI().isMapBlackBallWar(this.zone.map.mapId)) {
                            EffectSkillService.gI().setChibi(this, 300000);
                        }
                        lastTimeChibi = System.currentTimeMillis();
                    }
                    if (this.isPl() && !this.isBot && !this.isDie() && this.effectSkill != null && this.effectSkill.isChibi && Util.canDoWithTime(lastTimeUpdateChibi, 1000)) {
                        if (this.typeChibi == 1) {
                            if (this.nPoint.mp < this.nPoint.mpMax) {
                                if (this.nPoint.mpMax - this.nPoint.mp < this.nPoint.mpMax / 10) {
                                    this.nPoint.mp = this.nPoint.mpMax;
                                } else {
                                    this.nPoint.mp += this.nPoint.mpMax / 10;
                                }
                            }
                            PlayerService.gI().sendInfoMp(this);
                        } else if (this.typeChibi == 3) {
                            if (this.nPoint.hp < this.nPoint.hpMax) {
                                if (this.nPoint.hpMax - this.nPoint.hp < this.nPoint.hpMax / 10) {
                                    this.nPoint.hp = this.nPoint.hpMax;
                                } else {
                                    this.nPoint.hp += this.nPoint.hpMax / 10;
                                }
                            }
                            PlayerService.gI().sendInfoHp(this);
                        }
                        lastTimeUpdateChibi = System.currentTimeMillis();
                    }
                    if (this.isPl() && this.achievement != null) {
                        this.achievement.done(ConstAchievement.HOAT_DONG_CHAM_CHI, 1000);
                    }
                    if (this.isPl()) {
                        Calendar calendar = Calendar.getInstance();
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        if (hour >= 22 && hour < 23) {
                            if (zone.map.mapId == 126) {
                                //  ChangeMapService.gI().changeMapNonSpaceship(this, 19, 1000 + Util.nextInt(-100, 100), 360);
                            }
                        }
                        TaskService.gI().sendUpdateCountSubTask(this);
                        autoSendBadges();
                        BadgesTaskService.updateDoneTask(this);
                        sendTextTimeDaiLyGift();
                    }
                    if (this.isPl() && this.effectSkill != null && this.effectSkill.isMabuHold) {
                        this.nPoint.subHP(this.nPoint.hpMax / 100);
                        if (Util.isTrue(1, 10)) {
                            Service.gI().chat(this, "Cứu tôi với");
                        }
                        PlayerService.gI().sendInfoHp(this);
                        if (this.precentMabuHold > 15) {
                            EffectSkillService.gI().removeMabuHold(this);
                        }
                        if (this.nPoint.hp <= 0) {
                            EffectSkillService.gI().removeMabuHold(this);
                            setDie();
                        }
                    }

                    if (this.zone != null && this.effectSkin != null && this.effectSkin.xHPKI > 1 && !MapService.gI().isMapBlackBallWar(this.zone.map.mapId)) {
                        this.effectSkin.xHPKI = 1;
                        this.nPoint.calPoint();
                        Service.gI().point(this);
                    }

                    if (this.zone != null && this.effectSkin != null && this.effectSkin.xDame > 1 && !MapService.gI().isMapBlackBallWar(this.zone.map.mapId)) {
                        this.effectSkin.xDame = 1;
                        this.nPoint.calPoint();
                        Service.gI().point(this);
                    }

                    if (this.isPl() && this.zone != null) {
                        fixBlackBallWar();
                    }

                    if (this.isPhuHoMapMabu && this.zone != null && !MapService.gI().isMapMabu2H(this.zone.map.mapId)) {
                        this.isPhuHoMapMabu = false;
                        this.nPoint.calPoint();
                        Service.gI().point(this);
                        Service.gI().Send_Info_NV(this);
                        Service.gI().Send_Caitrang(this);
                    }

                    if (this.isPl() && this.clan != null && this.clan.ConDuongRanDoc != null
                            && this.joinCDRD && this.clan.ConDuongRanDoc.allMobsDead
                            && this.talkToThanMeo && this.zone.map.mapId == 47
                            && Util.canDoWithTime(timeChangeMap144, 5000)) {
                        ChangeMapService.gI().changeMapYardrat(this, this.clan.ConDuongRanDoc.getMapById(144), 300 + Util.nextInt(-100, 100), 312);
                        this.timeChangeMap144 = System.currentTimeMillis();
                    }
                    if (this.isPl() && this.zone != null && !MapService.gI().isMapMaBu(this.zone.map.mapId) && (this.cFlag == 9 || this.cFlag == 10)) {
                        Service.gI().changeFlag(this, 0);
                    }

                    if (this.isPl() && this.superRank != null) {
                        if (Util.isAfterMidnight(this.superRank.lastRewardTime)) {
                            this.superRank.reward();
                        }
                    }

                    if (this.isPl() && this.zone != null && MapService.gI().isMapMaBu(this.zone.map.mapId) && this.cFlag != 9 && this.cFlag != 10) {
                        Service.gI().changeFlag(this, Util.nextInt(9, 10));
                    }
                    if (dropItem != null) {
                        dropItem.update();
                    }
                    MajinBuuService.gI().update(this);
                    SuperDivineWaterService.gI().update(this);
                    if (!isBoss && this.idMark != null && this.idMark.isGotoFuture() && Util.canDoWithTime(this.idMark.getLastTimeGoToFuture(), 60000)) {
                        ChangeMapService.gI().changeMapBySpaceShip(this, 102, -1, Util.nextInt(60, 200));
                        this.idMark.setGotoFuture(false);
                    }
                }
            } catch (Exception e) {
                Logger.logException(Player.class, e, "Lỗi tại player: " + this.name);
            }
        }
    }

    public void autoSendBadges() {
        Iterator<BadgesData> iterator = dataBadges.iterator();
        while (iterator.hasNext()) {
            BadgesData data = iterator.next();
            if (System.currentTimeMillis() >= data.timeofUseBadges) {
                iterator.remove();
            } else if (data.isUse) {
                badges.idBadges = data.idBadGes;
            }
        }

        if (badges.idBadges != -1 && Util.canDoWithTime(badges.lastTimeSendBadges, 10000)) {
            Service.gI().sendBadgesPlayer(this, 5, badges.idBadges);
            badges.lastTimeSendBadges = System.currentTimeMillis();
            this.nPoint.update();
            Service.gI().point(this);
        }
    }

    private static final short[][] idOutfitFusion = {
        {380, 381, 382},
        {383, 384, 385},
        {391, 392, 393},
        {870, 871, 872},
        {873, 874, 875},
        {867, 868, 869},
        {1866, 1859, 1860}, //td btc3
        {1861, 1864, 1865}, //nm btc3
        {1856, 1859, 1860} 
    };

    public static final short[][] idOutfitGod = {
        {-1, 472, 473}, {-1, 476, 477}, {-1, 474, 475}
    };

    public static final short[][][] idOutfitHalloween = {
        {
            {545, 548, 549}, {547, 548, 549}, {546, 548, 549}
        },
        {
            {2082, 2085, 2086}, {2084, 2085, 2086}, {2083, 2085, 2086}
        },
        {
            {760, 761, 762}, {760, 761, 762}, {760, 761, 762}
        },
        {
            {654, 655, 656}, {654, 655, 656}, {654, 655, 656}
        },
        {
            {651, 652, 653}, {651, 652, 653}, {651, 652, 653}
        }};

    public static final short[][] idOutfitMafuba = {
        {1218, 1219, 1220}, {1218, 1219, 1220}, {1218, 1219, 1220}
    };

    public String percentGold(int type) {
        try {
            if (type == 0) {
                double denominator = ChonAiDay_Gold.gI().goldNormar;
                if (denominator != 0) {
                    double percent = ((double) this.goldNormar / denominator) * 100;
                    return String.valueOf(Math.ceil(percent));
                } else {
                    return "0";
                }
            } else if (type == 1) {
                double denominator = ChonAiDay_Gold.gI().goldVip;
                if (denominator != 0) {
                    double percent = ((double) this.goldVIP / denominator) * 100;
                    return String.valueOf(Math.ceil(percent));
                } else {
                    return "0";
                }
            }
        } catch (ArithmeticException e) {
            return "0";
        }
        return "0";
    }

    public String percentGem(int type) {
        try {
            if (type == 0) {
                double denominator3 = ChonAiDay_Gem.gI().gemNormar;
                if (denominator3 != 0) {
                    double percent = ((double) this.gemNormar / denominator3) * 100;
                    return String.valueOf(Math.ceil(percent));
                } else {
                    return "0";
                }
            } else if (type == 1) {
                double denominator3 = ChonAiDay_Gem.gI().gemVip;
                if (denominator3 != 0) {
                    double percent = ((double) this.gemVIP / denominator3) * 100;
                    return String.valueOf(Math.ceil(percent));
                } else {
                    return "0";
                }
            } else {
                return "0";
            }
        } catch (ArithmeticException | NullPointerException e) {
            // Xử lý nếu có lỗi
            return "0";
        }
    }

    public int getHat() {
        return -1;
    }

    public byte getAura() {
        if (!isPl()) {
            return -1;
        }
        if (this.clan != null && this.clan.levelHaoQuangBang > 0) {
            return (byte) Math.min(4, this.clan.levelHaoQuangBang - 1);
        }
        if (this.Cards == null || this.Cards.isEmpty()) {
            return -1;
        }
        for (Card card : this.Cards) {
            if (card != null && (card.Id == 956 || card.Id == 1792 || card.Id == 1793 || card.Id == 1791 || card.Id == 1204 || card.Id == 1142) && card.Level > 1) {
                RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(r -> r.Id == card.Id).findFirst().orElse(null);
                if (radarTemplate != null) {
                    return (byte) radarTemplate.AuraId;
                }
            }
        }
        return -1;
    }

    public byte getEffFront() {
        byte eff = -1;
        if (this.levelHaoQuang > 0) {
            eff = switch (this.levelHaoQuang) {
                case 1 -> 6;
                case 2 -> 7;
                default -> 8;
            };
        }
        if (this.inventory == null) {
            return eff;
        }
        if (this.inventory.itemsBody.isEmpty() || this.inventory.itemsBody.size() < 10) {
            return eff;
        }
        int levelAo = 0;
        Item.ItemOption optionLevelAo = null;
        int levelQuan = 0;
        Item.ItemOption optionLevelQuan = null;
        int levelGang = 0;
        Item.ItemOption optionLevelGang = null;
        int levelGiay = 0;
        Item.ItemOption optionLevelGiay = null;
        int levelNhan = 0;
        Item.ItemOption optionLevelNhan = null;
        Item itemAo = this.inventory.itemsBody.get(0);
        Item itemQuan = this.inventory.itemsBody.get(1);
        Item itemGang = this.inventory.itemsBody.get(2);
        Item itemGiay = this.inventory.itemsBody.get(3);
        Item itemNhan = this.inventory.itemsBody.get(4);
        for (Item.ItemOption io : itemAo.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelAo = io.param;
                optionLevelAo = io;
                break;
            }
        }
        for (Item.ItemOption io : itemQuan.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelQuan = io.param;
                optionLevelQuan = io;
                break;
            }
        }
        for (Item.ItemOption io : itemGang.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelGang = io.param;
                optionLevelGang = io;
                break;
            }
        }
        for (Item.ItemOption io : itemGiay.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelGiay = io.param;
                optionLevelGiay = io;
                break;
            }
        }
        for (Item.ItemOption io : itemNhan.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelNhan = io.param;
                optionLevelNhan = io;
                break;
            }
        }
        byte setEff = -1;
        if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 8 && levelQuan >= 8 && levelGang >= 8 && levelGiay >= 8 && levelNhan >= 8) {
            setEff = 8;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 7 && levelQuan >= 7 && levelGang >= 7 && levelGiay >= 7 && levelNhan >= 7) {
            setEff = 7;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 6 && levelQuan >= 6 && levelGang >= 6 && levelGiay >= 6 && levelNhan >= 6) {
            setEff = 6;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 5 && levelQuan >= 5 && levelGang >= 5 && levelGiay >= 5 && levelNhan >= 5) {
            setEff = 5;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 4 && levelQuan >= 4 && levelGang >= 4 && levelGiay >= 4 && levelNhan >= 4) {
            setEff = 4;
        }
        return (byte) Math.max(eff, setEff);
    }

    public short getHead() {
        if (this.isPl() && this.pet != null && this.fusion.typeFusion == ConstPlayer.HOP_THE_GOGETA || this.fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2 || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
            Item item = inventory.itemsBody.get(5);
            Item petItem = pet.inventory.itemsBody.get(5);
            boolean hasItem1 = item.isNotNullItem() && (item.template.id == 1693 || item.template.id == 1553);
            boolean hasItem2 = petItem.isNotNullItem() && (petItem.template.id == 1693 || petItem.template.id == 1553);
            boolean sameItem = item.isNotNullItem() && petItem.isNotNullItem() && item.template.id == petItem.template.id;
            if (hasItem1 && hasItem2 && !sameItem) {
                return 1578;
            }
        }
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][0];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 454;
        }
        if (effectSkill != null && effectSkill.isHalloween) {
            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][0];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 412;
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION) {
            if (nPoint != null && nPoint.isGogeta) {
                return 2100;
            } else if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
//                if (this.pet.typePet == 1) {
//                    return idOutfitFusion[3 + this.gender][0];
//                }
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                if (nPoint != null && nPoint.levelBT == 3) {
                    return idOutfitFusion[3 + this.gender][0];
                }
                return idOutfitFusion[3 + this.gender][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                if (nPoint != null && nPoint.levelBT == 4) {
                    return idOutfitFusion[6 + this.gender][0];
                }
                return idOutfitFusion[6 + this.gender][0];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int headId = inventory.itemsBody.get(5).template.head;
            if (headId != -1) {
                return (short) headId;
            }
        }
        return this.head;
    }

    public short getBody() {
        if (this.isPl() && this.pet != null && this.fusion.typeFusion == ConstPlayer.HOP_THE_GOGETA || this.fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2 || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
            Item item = inventory.itemsBody.get(5);
            Item petItem = pet.inventory.itemsBody.get(5);

            boolean hasItem1 = item.isNotNullItem() && (item.template.id == 1693 || item.template.id == 1553);
            boolean hasItem2 = petItem.isNotNullItem() && (petItem.template.id == 1693 || petItem.template.id == 1553);
            boolean sameItem = item.isNotNullItem() && petItem.isNotNullItem() && item.template.id == petItem.template.id;
            if (hasItem1 && hasItem2 && !sameItem) {
                return 1581;
            }
        }
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][1];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 455;
        }
        if (effectSkill != null && effectSkill.isHalloween) {
            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][1];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 413;
        } else if (isPhuHoMapMabu && fusion != null && fusion.typeFusion == ConstPlayer.NON_FUSION) {
            return idOutfitGod[this.gender][1];
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION) {
            if (nPoint != null && nPoint.isGogeta) {
                return 2101;
            } else if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
//                if (this.pet.typePet == 1) {
//                    return idOutfitFusion[3 + this.gender][1];
//                }
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                if (nPoint != null && nPoint.levelBT == 3) {
                    return idOutfitFusion[3 + this.gender][1];
                }
                return idOutfitFusion[3 + this.gender][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                if (nPoint != null && nPoint.levelBT == 4) {
                    return idOutfitFusion[6 + this.gender][1];
                }
                return idOutfitFusion[6 + this.gender][1];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int body = inventory.itemsBody.get(5).template.body;
            if (body != -1) {
                return (short) body;
            }
        }
        if (inventory != null && inventory.itemsBody.get(0).isNotNullItem()) {
            return inventory.itemsBody.get(0).template.part;
        }
        return (short) (gender == ConstPlayer.NAMEC ? 59 : 57);
    }

    public short getLeg() {
        if (this.isPl() && this.pet != null && this.fusion.typeFusion == ConstPlayer.HOP_THE_GOGETA || this.fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2 || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
            Item item = inventory.itemsBody.get(5);
            Item petItem = pet.inventory.itemsBody.get(5);

            boolean hasItem1 = item.isNotNullItem() && (item.template.id == 1693 || item.template.id == 1553);
            boolean hasItem2 = petItem.isNotNullItem() && (petItem.template.id == 1693 || petItem.template.id == 1553);
            boolean sameItem = item.isNotNullItem() && petItem.isNotNullItem() && item.template.id == petItem.template.id;
            if (hasItem1 && hasItem2 && !sameItem) {
                return 1582;
            }
        }
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][2];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 456;
        }
        if (effectSkill != null && effectSkill.isHalloween) {
            return idOutfitHalloween[effectSkill.idOutfitHalloween][this.gender][2];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 414;
        } else if (isPhuHoMapMabu && fusion != null && fusion.typeFusion == ConstPlayer.NON_FUSION) {
            return idOutfitGod[this.gender][2];
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION) {
            if (nPoint != null && nPoint.isGogeta) {
                return 2102;
            } else if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
//                if (this.pet.typePet == 1) {
//                    return idOutfitFusion[3 + this.gender][2];
//                }
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                if (nPoint != null && nPoint.levelBT == 3) {
                    return idOutfitFusion[3 + this.gender][2];
                }
                return idOutfitFusion[3 + this.gender][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                if (nPoint != null && nPoint.levelBT == 4) {
                    return idOutfitFusion[6 + this.gender][2];
                }
                return idOutfitFusion[6 + this.gender][2];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int leg = inventory.itemsBody.get(5).template.leg;
            if (leg != -1) {
                return (short) leg;
            }
        }
        if (inventory != null && inventory.itemsBody.get(1).isNotNullItem()) {
            return inventory.itemsBody.get(1).template.part;
        }
        return (short) (gender == 1 ? 60 : 58);
    }

    public short getFlagBag() {
        if (this.idMark.isHoldBlackBall()) {
            return 31;
        } else if (this.idNRNM >= 353 && this.idNRNM <= 359) {
            return 30;
        }
        if (TaskService.gI().getIdTask(this) == ConstTask.TASK_3_2) {
            return 28;
        }
        if (this.inventory.itemsBody.size() >= 11) {
            if (this.inventory.itemsBody.get(8).isNotNullItem()) {
                short part = this.inventory.itemsBody.get(8).template.part;
                if (part >= 0 && part <= 255 && FlagBagService.gI().getFlagBag(part) != null) {
                    return part;
                }
            }
        }
        if (this.isPet && this.inventory.itemsBody.size() >= 8) {
            if (this.inventory.itemsBody.get(7).isNotNullItem()) {
                short part = this.inventory.itemsBody.get(7).template.part;
                if (part >= 0 && part <= 255 && FlagBagService.gI().getFlagBag(part) != null) {
                    return part;
                }
            }
        }
        if (this.clan != null && this.clan.imgId >= 0) {
            return (short) this.clan.imgId;
        }
        return -1;
    }

    public short getMount() {
        if (this.inventory.itemsBody.isEmpty() || this.inventory.itemsBody.size() < 10) {
            return -1;
        }
        Item item = this.inventory.itemsBody.get(9);
        if (!item.isNotNullItem()) {
            return -1;
        }
        if (item.template.type == 24 || item.template.type == 23) {
            if (item.template.gender == 3 || item.template.gender == this.gender) {
                return item.template.id;
            } else {
                return -1;
            }
        } else {
            if (item.template.id < 500) {
                return item.template.id;
            } else {
                Short value = (Short) DataGame.MAP_MOUNT_NUM.get(item.template.id);
                return value != null ? value : -1;
            }
        }
    }

    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (plAtt != null && !plAtt.equals(this)) {
                setTemporaryEnemies(plAtt);
                plAtt.setTemporaryEnemies(this);
            }

            if (plAtt != null && plAtt.playerSkill.skillSelect != null && !plAtt.isBoss && MapService.gI().isMapMaBu(this.zone.map.mapId)) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN ->
                        damage = damage > this.nPoint.hpMax / 20 ? this.nPoint.hpMax / 20 : damage;
                }
            }
            if (plAtt != null && plAtt.isBoss) {
                this.effectSkin.isVoHinh = false;
                this.effectSkin.lastTimeVoHinh = System.currentTimeMillis();
            }
            if (plAtt != null && plAtt.effectSkill != null && plAtt.effectSkill.isBinh
                    && !Util.canDoWithTime(plAtt.effectSkill.lastTimeUpBinh, 3000)) {
                return 0;
            }
            if (plAtt != null && plAtt.isPl() && this.maBuHold != null && this.zone != null && this.zone.map.mapId == 128) {
                this.precentMabuHold++;
                damage = 1;
            }
            if (plAtt != null && plAtt.idNRNM != -1 && (this.isBoss || this.isNewPet)) {
                return 1;
            }
            if (plAtt != null && (plAtt.idNRNM != -1 || this.idNRNM != -1) && plAtt.clan != null && this.clan != null && plAtt.clan == this.clan) {
                Service.gI().chatJustForMe(plAtt, this, "Ê cùng bang mà");
                return 0;
            }
            if (!Util.canDoWithTime(this.lastTimeRevived, 1500)) {
                return 0;
            }

            if (plAtt != null && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC -> {
                        if (this.nPoint.voHieuChuong > 0) {
                            PlayerService.gI().hoiPhuc(this, 0, damage * this.nPoint.voHieuChuong / 100L);
                            return 0;
                        }
                    }
                }
            }
            if (this.nPoint.tlHoiMpKhiBiDanh > 0 && damage > 0) {
                long mpHoi = (long) this.nPoint.mpMax * this.nPoint.tlHoiMpKhiBiDanh / 100L;
                if (mpHoi > 0) {
                    PlayerService.gI().hoiPhuc(this, 0, mpHoi);
                }
            }

            int tlGiap = this.nPoint.tlGiap;
            int tlNeDon = this.nPoint.tlNeDon;
            int tlNeDonXinbato = this.nPoint.tlNeDonXinbato;

            if (plAtt != null && !isMobAttack && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN, Skill.QUA_CAU_KENH_KHI, Skill.MAKANKOSAPPO, Skill.DICH_CHUYEN_TUC_THOI ->
                        tlNeDon -= plAtt.nPoint.tlchinhxac;
                    default ->
                        tlNeDon = 0;
                }

                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC -> {
                        if (tlGiap - plAtt.nPoint.tlxgc >= 0) {
                            tlGiap -= plAtt.nPoint.tlxgc;
                        } else {
                            tlGiap = 0;
                        }
                    }
                    case Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN -> {
                        if (tlGiap - plAtt.nPoint.tlxgcc >= 0) {
                            tlGiap -= plAtt.nPoint.tlxgcc;
                        } else {
                            tlGiap = 0;
                        }
                    }
                }
            }

            if (piercing) {
                tlGiap = 0;
            }

            if (tlNeDon > 90) {
                tlNeDon = 90;
            }
            if (tlNeDonXinbato > 90) {
                tlNeDonXinbato = 90;
            }
            if (tlGiap > 86) {
                tlGiap = 86;
            }

            if (Util.isTrue(tlNeDon, 100)) {
                return 0;
            }
            boolean hasXinbato = this.effectSkill != null && this.effectSkill.isXinbato;
            if (hasXinbato) {
                if (tlNeDonXinbato > 90) {
                    tlNeDonXinbato = 90;
                }
                if (Util.isTrue(tlNeDonXinbato, 100)) {
                    return 0; // né thành công với Xinbato
                }
            }

            damage -= ((damage / 100) * tlGiap);

            if (!piercing) {
                damage = this.nPoint.subDameInjureWithDeff(damage);
            }

            boolean isUseGX = false;
            if (!piercing && plAtt != null && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN, Skill.QUA_CAU_KENH_KHI, Skill.MAKANKOSAPPO, Skill.DICH_CHUYEN_TUC_THOI ->
                        isUseGX = true;
                }
            }
            if ((isUseGX || isMobAttack) && this.itemTime != null) {
                if (this.itemTime.isUseGiapXen && !this.itemTime.isUseGiapXen2) {
                    damage /= 2;
                }
                if (this.itemTime.isUseGiapXen2) {
                    damage = damage / 100 * 40;
                }
            }

            if (!piercing && effectSkill.isShielding && !isMobAttack) {
                if (this.idMark != null) {
                    this.idMark.setDamePST((int) Math.min(damage, 2_147_483_647L));
                }
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
                if (MapService.gI().isMapPhoBan(this.zone.map.mapId)) {
                    damage = 10;
                }
            }
            damage = Math.min(damage, 2_147_483_647);
            if (isMobAttack && this.charms.tdBatTu > System.currentTimeMillis() && damage >= this.nPoint.hp) {
                damage = this.nPoint.hp - 1;
            }

            if (this.zone.map.mapId == 129) {
                if (damage >= this.nPoint.hp) {
                    this.lostByDeath = true;
                    The23rdMartialArtCongress mc = The23rdMartialArtCongressManager.gI().getMC(zone);
                    if (mc != null) {
                        mc.die();
                    }
                    return 0;
                }
            }
            if (this.zone.map.mapId == 51) {
                this.totalDamageTaken += damage;
            }
            if (this.zone.map.mapId == 134) {
                if (this.nPoint != null && this.nPoint.clanArenaHpOverflowScale > 1.0) {
                    damage = Math.max(1, (long) (damage / this.nPoint.clanArenaHpOverflowScale));
                }
                this.totalDamageTaken += damage;
            }
            this.nPoint.subHP(damage);
            if ((plAtt != null || isMobAttack) && isDie() && !isBoss && !isNewPet && !isNewPet1) {
                if (plAtt != null && this.isPl()) {
                    //TaskService.gI().checkDoneTaskPK(plAtt);
                    if (this.idMark != null && this.idMark.isHoldBlackBall()) {
                        //   TaskService.gI().checkDoneTaskNRSD(plAtt);
                    }
                }
                if (Util.isTrue(this.nPoint.tlBom, 100)) {
                    setBom(plAtt);
                } else {
                    setDie(plAtt);
                }
            }

            return (int) damage;
        } else {
            return 0;
        }
    }

    public void setTemporaryEnemies(Player pl) {
        if (!temporaryEnemies.contains(pl)) {
            temporaryEnemies.add(pl);
        }
    }

    protected void setBom(Player plAtt) {
        setDie(plAtt);
    }

    public void setDie() {
        this.setDie(null);
    }

    protected void setDie(Player plAtt) {
        if (this.isPl()) {
            long vangtru = this.nPoint.power / 1000000;
            if (vangtru > 32000) {
                vangtru = 32000;
            }
            int vang = (int) vangtru - Util.nextInt(10, 100);

            if (this.inventory.gold >= vang && vang >= 1) {
                this.inventory.gold -= vang;
                Service.gI().sendMoney(this);
                vang = vang * 95 / 100;
                if (vang < 10000) {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 189, vang, this.location.x, this.location.y, this.id));
                } else if (vang < 20000) {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 188, vang, this.location.x, this.location.y, this.id));
                } else {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 190, vang, this.location.x, this.location.y, this.id));
                }
            }
        }
        int mapid = this.zone.map.mapId;
        double PhanTramSucManhBiTru = 0.0;
        if (MapService.gI().isMapTuongLai(mapid)) {
            PhanTramSucManhBiTru = 0.001;
        } else if (MapService.gI().isMapCold(mapid)) {
            PhanTramSucManhBiTru = 0.001;
        }
        if (PhanTramSucManhBiTru > 0) {
            int dieukien = (int) (this.nPoint.power * PhanTramSucManhBiTru);
            dieukien = dieukien < 1 ? 1 : dieukien;
            if (this.nPoint.power >= dieukien) {
                this.nPoint.power -= dieukien;
                Service.gI().point(this);
            }
        }
        if (this.effectSkin.xHPKI > 1) {
            this.effectSkin.xHPKI = 1;
            Service.gI().point(this);
        }
        if (this.effectSkin.xDame > 1) {
            this.effectSkin.xDame = 1;
            Service.gI().point(this);
        }
        this.playerSkill.prepareQCKK = false;
        this.playerSkill.prepareLaze = false;
        this.playerSkill.prepareTuSat = false;
        this.effectSkill.removeSkillEffectWhenDie();
        nPoint.setHp(0);
        nPoint.setMp(0);
        if (this.mobMe != null) {
            this.mobMe.mobMeDie();
            this.mobMe.dispose();
            this.mobMe = null;
        }
        Service.gI().charDie(this);
        if (!this.isPet && !this.isBot && !this.isNewPet && !this.isNewPet1 && !this.isBoss && plAtt != null && !plAtt.isPet && !plAtt.isNewPet && !plAtt.isNewPet1 && !plAtt.isBoss) {
            if (!plAtt.itemTime.isUseAnDanh) {
                FriendAndEnemyService.gI().addEnemy(this, plAtt);
            }
        }
        this.typePk = 0;
        if (this.pvp != null && this.zone.map.mapId != 140) {
            this.pvp.lose(this, TYPE_LOSE_PVP.DEAD);
        }
        BlackBallWarService.gI().dropBlackBall(this);
        NgocRongNamecService.gI().dropNamekBall(this);
    }

    public void setClanMember() {
        if (this.clanMember != null) {
            this.clanMember.powerPoint = this.nPoint.power;
            this.clanMember.head = this.getHead();
            this.clanMember.body = this.getBody();
            this.clanMember.leg = this.getLeg();
        }
    }

    public boolean isAdmin() {
        return this.session != null && this.session.isAdmin;
    }

    public void setJustRevivaled() {
        this.justRevived = true;
        this.lastTimeRevived = System.currentTimeMillis();
    }

    public boolean isActive() {
        return (this.isPl() && this.session != null && this.session.actived) || (this.isPet && ((Pet) this).master.session != null && ((Pet) this).master.session.actived);
    }

    public void sendNewPet() {
        if (isPl() && inventory != null && inventory.itemsBody.get(7) != null) {
            Item it = inventory.itemsBody.get(7);
            if (it != null && it.isNotNullItem() && newPet == null) {
                switch (it.template.id) {
                    case 892 -> {
                        PetService.Pet2(this, 882, 883, 884);
                        Service.gI().point(this);
                    }
                    case 893 -> {
                        PetService.Pet2(this, 885, 886, 887);
                        Service.gI().point(this);
                    }
                    case 908 -> {
                        PetService.Pet2(this, 891, 892, 893);
                        Service.gI().point(this);
                    }
                    case 909 -> {
                        PetService.Pet2(this, 894, 895, 896);
                        Service.gI().point(this);
                    }
                    case 910 -> {
                        PetService.Pet2(this, 897, 898, 899);
                        Service.gI().point(this);
                    }
                    case 916 -> {
                        PetService.Pet2(this, 925, 926, 927);
                        Service.gI().point(this);
                    }
                    case 917 -> {
                        PetService.Pet2(this, 928, 929, 930);
                        Service.gI().point(this);
                    }
                    case 918 -> {
                        PetService.Pet2(this, 931, 932, 933);
                        Service.gI().point(this);
                    }
                    case 919 -> {
                        PetService.Pet2(this, 934, 935, 936);
                        Service.gI().point(this);
                    }
                    case 936 -> {
                        PetService.Pet2(this, 718, 719, 720);
                        Service.gI().point(this);
                    }
                    case 942 -> {
                        PetService.Pet2(this, 966, 967, 968);
                        Service.gI().point(this);
                    }
                    case 943 -> {
                        PetService.Pet2(this, 969, 970, 971);
                        Service.gI().point(this);
                    }
                    case 944 -> {
                        PetService.Pet2(this, 972, 973, 974);
                        Service.gI().point(this);
                    }
                    case 967 -> {
                        PetService.Pet2(this, 1050, 1051, 1052);
                        Service.gI().point(this);
                    }
                    case 1008 -> {
                        PetService.Pet2(this, 1074, 1075, 1076);
                        Service.gI().point(this);
                    }
                    case 1039 -> {
                        PetService.Pet2(this, 1089, 1090, 1091);
                        Service.gI().point(this);
                    }
                    case 1040 -> {
                        PetService.Pet2(this, 1092, 1093, 1094);
                        Service.gI().point(this);
                    }
                    case 1046 -> {
                        PetService.Pet2(this, -1, -1, -1);
                        Service.gI().point(this);
                    }
                    case 1107 -> {
                        PetService.Pet2(this, 1155, 1156, 1157);
                        Service.gI().point(this);
                    }
                    case 1114 -> {
                        PetService.Pet2(this, 1158, 1159, 1160);
                        Service.gI().point(this);
                    }
                    case 1188 -> {
                        PetService.Pet2(this, 1183, 1184, 1185);
                        Service.gI().point(this);
                    }
                    case 1202 -> {
                        PetService.Pet2(this, 1201, 1202, 1203);
                        Service.gI().point(this);
                    }
                    case 1203 -> {
                        PetService.Pet2(this, 1201, 1202, 1203);
                        Service.gI().point(this);
                    }
                    case 1207 -> {
                        PetService.Pet2(this, 1077, 1078, 1079);
                        Service.gI().point(this);
                    }
                    case 1224 -> {
                        PetService.Pet2(this, 1227, 1228, 1229);
                        Service.gI().point(this);
                    }
                    case 1225 -> {
                        PetService.Pet2(this, 1233, 1234, 1235);
                        Service.gI().point(this);
                    }
                    case 1226 -> {
                        PetService.Pet2(this, 1230, 1231, 1232);
                        Service.gI().point(this);
                    }
                    case 1243 -> {
                        PetService.Pet2(this, 1245, 1246, 1247);
                        Service.gI().point(this);
                    }
                    case 1244 -> {
                        PetService.Pet2(this, 1248, 1249, 1250);
                        Service.gI().point(this);
                    }
                    case 1256 -> {
                        PetService.Pet2(this, 1267, 1268, 1269);
                        Service.gI().point(this);
                    }
                    case 1318 -> {
                        PetService.Pet2(this, 1299, 1300, 1301);
                        Service.gI().point(this);
                    }
                    case 1347 -> {
                        PetService.Pet2(this, 1302, 1303, 1304);
                        Service.gI().point(this);
                    }
                    case 1414 -> {
                        PetService.Pet2(this, 1341, 1342, 1343);
                        Service.gI().point(this);
                    }
                    case 1435 -> {
                        PetService.Pet2(this, 1347, 1348, 1349);
                        Service.gI().point(this);
                    }
                    case 1452 -> {
                        PetService.Pet2(this, 1365, 1366, 1367);
                        Service.gI().point(this);
                    }
                    case 1458 -> {
                        PetService.Pet2(this, 1368, 1369, 1370);
                        Service.gI().point(this);
                    }
                    case 1482 -> {
                        PetService.Pet2(this, 1398, 1399, 1400);
                        Service.gI().point(this);
                    }
                    case 1497 -> {
                        PetService.Pet2(this, 1401, 1402, 1403);
                        Service.gI().point(this);
                    }
                    case 1550 -> {
                        PetService.Pet2(this, 1428, 1429, 1430);
                        Service.gI().point(this);
                    }
                    case 1551 -> {
                        PetService.Pet2(this, 1425, 1426, 1427);
                        Service.gI().point(this);
                    }
                    case 1564 -> {
                        PetService.Pet2(this, 1437, 1438, 1439);
                        Service.gI().point(this);
                    }
                    case 1568 -> {
                        PetService.Pet2(this, 1443, 1444, 1445);
                        Service.gI().point(this);
                    }
                    case 1573 -> {
                        PetService.Pet2(this, 1446, 1447, 1448);
                        Service.gI().point(this);
                    }
                    case 1596 -> {
                        PetService.Pet2(this, 1473, 1474, 1475);
                        Service.gI().point(this);
                    }
                    case 1597 -> {
                        PetService.Pet2(this, 1473, 1474, 1475);
                        Service.gI().point(this);
                    }
                    case 1611 -> {
                        PetService.Pet2(this, 1488, 1494, 1495);
                        Service.gI().point(this);
                    }
                    case 1620 -> {
                        PetService.Pet2(this, 1496, 1497, 1498);
                        Service.gI().point(this);
                    }
                    case 1621 -> {
                        PetService.Pet2(this, 1496, 1497, 1498);
                        Service.gI().point(this);
                    }
                    case 1622 -> {
                        PetService.Pet2(this, 1488, 1489, 1490);
                        Service.gI().point(this);
                    }
                    case 1629 -> {
                        PetService.Pet2(this, 1505, 1506, 1507);
                        Service.gI().point(this);
                    }
                    case 1630 -> {
                        PetService.Pet2(this, 1508, 1509, 1510);
                        Service.gI().point(this);
                    }
                    case 1631 -> {
                        PetService.Pet2(this, 1513, 1516, 1517);
                        Service.gI().point(this);
                    }
                    case 1633 -> {
                        PetService.Pet2(this, 1523, 1524, 1525);
                        Service.gI().point(this);
                    }
                    case 1654 -> {
                        PetService.Pet2(this, 1526, 1529, 1530);
                        Service.gI().point(this);
                    }
                    case 1668 -> {
                        PetService.Pet2(this, 1550, 1551, 1552);
                        Service.gI().point(this);
                    }
                    case 1682 -> {
                        PetService.Pet2(this, 1558, 1559, 1560);
                        Service.gI().point(this);
                    }
                    case 1683 -> {
                        PetService.Pet2(this, 1561, 1562, 1563);
                        Service.gI().point(this);
                    }
                    case 1686 -> {
                        PetService.Pet2(this, 1572, 1573, 1574);
                        Service.gI().point(this);
                    }
                    case 1750 -> {
                        PetService.Pet2(this, 1464, 1465, 1466);
                        Service.gI().point(this);
                    }
                    case 1765 -> {
                        PetService.Pet2(this, 1662, 1663, 1764);
                        Service.gI().point(this);
                    }
                    case 1729 -> {
                        PetService.Pet2(this, 1621, 1622, 1623);
                        Service.gI().point(this);
                    }
                    case 1727 -> {
                        PetService.Pet2(this, 1616, 1617, 1618);
                        Service.gI().point(this);
                    }
                    case 1789 -> {
                        PetService.Pet2(this, 1724, 1725, 1726);
                        Service.gI().point(this);
                    }
                    case 1766 -> {
                        PetService.Pet2(this, 1665, 1666, 1667);
                        Service.gI().point(this);
                    }
                    case 1767 -> {
                        PetService.Pet2(this, 1668, 1669, 1670);
                        Service.gI().point(this);
                    }
                    case 1768 -> {
                        PetService.Pet2(this, 1671, 1672, 1673);
                        Service.gI().point(this);
                    }
                    case 1769 -> {
                        PetService.Pet2(this, 1674, 1675, 1676);
                        Service.gI().point(this);
                    }
                    case 1770 -> {
                        PetService.Pet2(this, 1677, 1678, 1679);
                        Service.gI().point(this);
                    }
                    case 1771 -> {
                        PetService.Pet2(this, 1680, 1681, 1682);
                        Service.gI().point(this);
                    }
                }
            }
        }
    }

    private void fixBlackBallWar() {
        int x = this.location.x;
        int y = this.location.y;
        switch (this.zone.map.mapId) {
            case 85, 86, 87, 88, 89, 90, 91 -> {
                if (this.isPl()) {
                    if (x < 24 || x > this.zone.map.mapWidth - 24 || y < 0 || y > this.zone.map.mapHeight - 24) {
                        if (MapService.gI().getWaypointPlayerIn(this) == null) {
                            Service.gI().resetPoint(this, x, this.zone.map.yPhysicInTop(this.location.x, 100));
                            this.nPoint.hp -= this.nPoint.hpMax / 10;
                            PlayerService.gI().sendInfoHp(this);
                            return;
                        }
                    }
                    int yTop = this.zone.map.yPhysicInTop(this.location.x, this.location.y);
                    if (yTop >= this.zone.map.mapHeight - 24) {
                        Service.gI().resetPoint(this, x, this.zone.map.yPhysicInTop(this.location.x, 100));
                        this.nPoint.hp -= this.nPoint.hpMax / 10;
                        PlayerService.gI().sendInfoHp(this);
                    }
                }
            }
        }
    }

    public void move(int _toX, int _toY) {
        if (this.isBoss && this.zone != null && this.zone.map != null
                && !(this instanceof nro.models.boss.Boss b && !b.isSubjectToSafeMapBounds())) {
            int[] safeLoc = this.zone.map.getSafeBossLocation(this.zone, _toX, _toY);
            _toX = safeLoc[0];
            _toY = safeLoc[1];
        }
        if (_toX != this.location.x) {
            this.location.x = _toX;
        }
        if (_toY != this.location.y) {
            this.location.y = _toY;
        }
        MapService.gI().sendPlayerMove(this);
    }

    public long getDuaHauStartTime() {
        return duaHauStartTime;
    }

    public void setDuaHauStartTime(long duaHauStartTime) {
        this.duaHauStartTime = duaHauStartTime;
    }

    public void dispose() {
        if (itemsTradeWVP != null) {
            if (!itemsTradeWVP.isEmpty()) {
                for (Item item : itemsTradeWVP) {
                    InventoryService.gI().addItemBag(this, item);
                }
            }
            itemsTradeWVP.clear();
            itemsTradeWVP = null;
        }
        if (pet != null) {
            pet.dispose();
            pet = null;
        }
        if (newPet != null) {
            newPet.dispose();
            newPet = null;
        }
        if (mapBlackBall != null) {
            mapBlackBall.clear();
            mapBlackBall = null;
        }
        zone = null;
        mapBeforeCapsule = null;
        if (mapMaBu != null) {
            mapMaBu.clear();
            mapMaBu = null;
        }
        mapBeforeCapsule = null;
        if (mapCapsule != null) {
            mapCapsule.clear();
            mapCapsule = null;
        }
        if (mobMe != null) {
            mobMe.dispose();
            mobMe = null;
        }
        location = null;
        if (setClothes != null) {
            setClothes.dispose();
            setClothes = null;
        }
        if (effectSkill != null) {
            effectSkill.dispose();
            effectSkill = null;
        }
        if (mabuEgg != null) {
            mabuEgg.dispose();
            mabuEgg = null;
        }
        if (DuaHauEgg != null) {
            DuaHauEgg.dispose();
            DuaHauEgg = null;
        }
        if (playerTask != null) {
            playerTask.dispose();
            playerTask = null;
        }
        if (itemTime != null) {
            itemTime.dispose();
            itemTime = null;
        }
        if (fusion != null) {
            fusion.dispose();
            fusion = null;
        }
        if (magicTree != null) {
            magicTree.dispose();
            magicTree = null;
        }
        if (playerIntrinsic != null) {
            playerIntrinsic.dispose();
            playerIntrinsic = null;
        }
        if (inventory != null) {
            inventory.dispose();
            inventory = null;
        }
        if (playerSkill != null) {
            playerSkill.dispose();
            playerSkill = null;
        }
        if (combineNew != null) {
            combineNew.dispose();
            combineNew = null;
        }
        if (idMark != null) {
            idMark.dispose();
            idMark = null;
        }
        if (charms != null) {
            charms.dispose();
            charms = null;
        }
        if (effectSkin != null) {
            effectSkin.dispose();
            effectSkin = null;
        }
        if (nPoint != null) {
            nPoint.dispose();
            nPoint = null;
        }
        if (rewardBlackBall != null) {
            rewardBlackBall.dispose();
            rewardBlackBall = null;
        }
        if (pvp != null) {
            pvp.dispose();
            pvp = null;
        }
        if (superRank != null) {
            superRank.dispose();
            superRank = null;
        }
        if (dropItem != null) {
            dropItem.dispose();
            dropItem = null;
        }
        if (satellite != null) {
            satellite = null;
        }
        if (achievement != null) {
            achievement.dispose();
            achievement = null;
        }
        if (giftCode != null) {
            giftCode.dispose();
            giftCode = null;
        }
        if (traning != null) {
            traning = null;
        }
        if (mapCapsule != null) {
            mapCapsule.clear();
            mapCapsule = null;
        }
        if (Cards != null) {
            Cards.clear();
            Cards = null;
        }
        if (itemsWoodChest != null) {
            itemsWoodChest.clear();
            itemsWoodChest = null;
        }
        if (friends != null) {
            friends.clear();
            friends = null;
        }
        if (enemies != null) {
            enemies.clear();
            enemies = null;
        }
        if (temporaryEnemies != null) {
            temporaryEnemies.clear();
            temporaryEnemies = null;
        }
        itemsWoodChest = null;
        Cards = null;
        itemEvent = null;
        maBu2H = null;
        maBuHold = null;
        zoneSieuThanhThuy = null;
        thongBaoTapTuDong = null;
        notify = null;
        clan = null;
        clanMember = null;
        friends = null;
        enemies = null;
        session = null;
        if (newSkill != null) {
            newSkill.dispose();
            newSkill = null;
        }
        name = null;
        textThongBaoChangeMap = null;
        textThongBaoThua = null;
    }

    public String getLastChatMessage() {
        return lastChatMessage;  // Trả về tin nhắn cuối cùng
    }

    public void setLastChatMessage(String message) {
        this.lastChatMessage = message;  // Cập nhật tin nhắn cuối cùng
    }

    public boolean hasEffect(Player player, int effectId) {
        Long effectEndTime = player.activeEffects.get(effectId);
        return effectEndTime != null && System.currentTimeMillis() < effectEndTime;
    }

    public void spreadEffectToNearbyPlayers() {
        if (hasEffect(this, 7143)) {
            try {
                List<Player> playersMap = this.zone.getNotBosses();
                for (Player targetPlayer : playersMap) {
                    if (targetPlayer != null && !targetPlayer.isDie() && targetPlayer != this
                            && !hasEffect(targetPlayer, 7143) && Util.getDistance(this, targetPlayer) <= 200) {

                        long effectDuration = 10000; // Hiệu ứng kéo dài 10 giây
                        long effectEndTime = System.currentTimeMillis() + effectDuration;
                        targetPlayer.activeEffects.put(7143, effectEndTime);
                        ItemTimeService.gI().sendItemTime(targetPlayer, 7143, (int) (effectDuration / 1000));
                        Service.gI().chat(targetPlayer, "Bạn đã bị lây hiệu ứng từ " + this.name + "!");

                        System.out.println("[DEBUG] Hiệu ứng lan sang: " + targetPlayer.name);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("[DEBUG] Không có hiệu ứng 7143, không lan ra cho người chơi khác.");
        }
    }

    public long lastTimeSendTextTime;

    public void sendTextTimeDaiLyGift() {
        if (Util.canDoWithTime(lastTimeSendTextTime, 300000)) {
            if (DailyGiftService.checkDailyGift(this, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                ItemTimeService.gI().sendTextTime(this, itemTime.TEXT_NHAN_BUA_MIEN_PHI, "Nhận ngẫu nhiên bùa 1h mỗi ngày tại Bà Hạt Mít ở vách núi", 30);
            }
            lastTimeSendTextTime = System.currentTimeMillis();
        }
    }

    public Player(String name) {
        this.name = name;
        this.originalName = name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public void resetName() {
        if (originalName == null) {
            return;
        }
        this.name = this.originalName;
    }

    public boolean isMabuPet(Player player) {
        return player.pet != null && player.pet.typePet == 1;
    }

    public void startAutoHuntBoss(int bossIndex) {
        nro.models.boss.Boss boss = nro.models.boss.Boss_Manager.BossManager.gI().getBoss(bossIndex);
        if (boss == null || boss.data == null || boss.data.length == 0) {
            Service.gI().sendThongBao(this, "Không tìm thấy thông tin Boss!");
            return;
        }
        int lv = (boss.currentLevel >= 0 && boss.currentLevel < boss.data.length) ? boss.currentLevel : 0;
        this.isAutoHuntBoss = true;
        this.targetAutoBossIndex = bossIndex;
        this.targetAutoBossId = boss.id;
        this.targetAutoBossName = boss.data[lv].getName();
        this.autoHuntScanZone = 0;
        this.lastTimeAutoHuntAction = System.currentTimeMillis() - 1800L;
        this.lastTimeAutoHuntAttack = 0;
        this.lastTimeAutoHuntWaitNotify = System.currentTimeMillis();
        this.isAutoBuffPea = true;
        Service.gI().sendThongBao(this, "Đã BẬT Tự Động Săn Boss: " + this.targetAutoBossName
                + "! Hệ thống sẽ tự đến map, dò từng khu và bật Tàn Sát khi Boss xuất hiện.");
    }

    public void stopAutoHuntBoss() {
        this.isAutoHuntBoss = false;
        this.targetAutoBossIndex = -1;
        this.targetAutoBossId = -1;
        this.targetAutoBossName = "";
        this.autoHuntScanZone = 0;
        Service.gI().sendThongBao(this, "Đã TẮT chế độ Tự Động Săn Boss!");
    }

    private static long getBossGroupKey(long bossId) {
        return switch ((int) bossId) {
            case nro.models.boss.BossID.SO_4, nro.models.boss.BossID.SO_3, nro.models.boss.BossID.SO_2,
                 nro.models.boss.BossID.SO_1, nro.models.boss.BossID.TIEU_DOI_TRUONG -> 1001;
            case nro.models.boss.BossID.SO_4_NM, nro.models.boss.BossID.SO_3_NM, nro.models.boss.BossID.SO_2_NM,
                 nro.models.boss.BossID.SO_1_NM, nro.models.boss.BossID.TIEU_DOI_TRUONG_NM -> 1002;
            case nro.models.boss.BossID.ANDROID_19, nro.models.boss.BossID.DR_KORE -> 1003;
            case nro.models.boss.BossID.ANDROID_13, nro.models.boss.BossID.ANDROID_14, nro.models.boss.BossID.ANDROID_15 -> 1004;
            case nro.models.boss.BossID.PIC, nro.models.boss.BossID.POC, nro.models.boss.BossID.KING_KONG -> 1005;
            case nro.models.boss.BossID.BUJIN, nro.models.boss.BossID.KOGU, nro.models.boss.BossID.ZANGYA,
                 nro.models.boss.BossID.BIDO, nro.models.boss.BossID.BOJACK, nro.models.boss.BossID.SUPER_BOJACK -> 1006;
            case nro.models.boss.BossID.BASIL, nro.models.boss.BossID.LAVENDER, nro.models.boss.BossID.BERGAMO -> 1007;
            case nro.models.boss.BossID.SIEU_BO_HUNG, nro.models.boss.BossID.XEN_CON_1, nro.models.boss.BossID.XEN_CON_2,
                 nro.models.boss.BossID.XEN_CON_3, nro.models.boss.BossID.XEN_CON_4, nro.models.boss.BossID.XEN_CON_5,
                 nro.models.boss.BossID.XEN_CON_6, nro.models.boss.BossID.XEN_CON_7 -> 1008;
            default -> bossId;
        };
    }

    public boolean isMatchingAutoBoss(nro.models.boss.Boss b) {
        if (b == null || !this.isAutoHuntBoss || this.targetAutoBossId == -1) {
            return false;
        }
        if (b.id == this.targetAutoBossId) {
            return true;
        }
        if (b.parentBoss != null && b.parentBoss.id == this.targetAutoBossId) {
            return true;
        }
        if (getBossGroupKey(b.id) == getBossGroupKey(this.targetAutoBossId)) {
            return true;
        }
        return false;
    }

    private void autoPickBossDropsInZone() {
        if (this.zone == null || this.zone.items == null || this.zone.items.isEmpty()) {
            return;
        }
        List<ItemMap> itemsSnapshot = new ArrayList<>(this.zone.items);
        for (ItemMap it : itemsSnapshot) {
            if (it == null || it.itemTemplate == null) {
                continue;
            }
            if (it.playerId == this.id || it.playerId == -1) {
                boolean isCurrency = it.itemTemplate.type == 9 || it.itemTemplate.type == 10 || it.itemTemplate.type == 34;
                if (isCurrency || InventoryService.gI().getCountEmptyBag(this) > 0) {
                    nro.models.map.service.ItemMapService.gI().pickItem(this, it.itemMapId, true);
                }
            }
        }
    }

    private nro.models.boss.Boss findTargetAutoBoss() {
        // 1. Ưu tiên tìm Boss mục tiêu đang ở ngay trong Khu hiện tại
        if (this.zone != null) {
            nro.models.boss.Boss waitingInZone = null;
            for (Player p : this.zone.getBosses()) {
                if (p instanceof nro.models.boss.Boss b && !b.isDie() && isMatchingAutoBoss(b)) {
                    if (b.typePk == ConstPlayer.PK_ALL || nro.models.services.SkillService.gI().canAttackPlayer(this, b)) {
                        return b;
                    }
                    if (waitingInZone == null) {
                        waitingInZone = b;
                    }
                }
            }
            if (waitingInZone != null) {
                return waitingInZone;
            }
        }

        // 2. Kiểm tra đúng instance đã chọn nếu đang sống
        nro.models.boss.Boss indexed = nro.models.boss.Boss_Manager.BossManager.gI().getBoss(this.targetAutoBossIndex);
        if (indexed != null && indexed.zone != null && !indexed.isDie() && isMatchingAutoBoss(indexed)) {
            if (indexed.typePk == ConstPlayer.PK_ALL) {
                return indexed;
            }
        }

        // 3. Quét toàn bộ danh sách Boss tìm con đang mở PK (hoặc đang sống) cùng nhóm
        nro.models.boss.Boss fallbackAlive = null;
        for (nro.models.boss.Boss b : nro.models.boss.Boss_Manager.BossManager.gI().getBosses()) {
            if (b != null && b.zone != null && !b.isDie() && isMatchingAutoBoss(b)) {
                if (b.typePk == ConstPlayer.PK_ALL) {
                    return b;
                }
                if (fallbackAlive == null) {
                    fallbackAlive = b;
                }
            }
        }
        return fallbackAlive;
    }

    public void updateAutoHuntBoss() {
        try {
            if (!this.isAutoHuntBoss || this.targetAutoBossId == -1 || this.zone == null) {
                return;
            }
            // Nếu nhân vật bị Boss đánh chết trong lúc treo săn -> tự động hồi sinh tại chỗ sau 3 giây
            if (this.isDie()) {
                if (Util.canDoWithTime(this.lastTimeAutoHuntAction, 3000)) {
                    this.lastTimeAutoHuntAction = System.currentTimeMillis();
                    Service.gI().hsChar(this, this.nPoint.hpMax, this.nPoint.mpMax);
                }
                return;
            }

            // Luôn tự nhặt vật phẩm rơi từ Boss trong khu hiện tại trước
            autoPickBossDropsInZone();

            nro.models.boss.Boss targetBoss = findTargetAutoBoss();
            if (targetBoss == null || targetBoss.zone == null || targetBoss.zone.map == null) {
                if (Util.canDoWithTime(this.lastTimeAutoHuntWaitNotify, 15000)) {
                    this.lastTimeAutoHuntWaitNotify = System.currentTimeMillis();
                    Service.gI().sendThongBao(this, "[Tự Săn Boss] Đang chờ " + this.targetAutoBossName + " xuất hiện...");
                }
                return;
            }

            int targetMapId = targetBoss.zone.map.mapId;
            // B1: Nếu đang ở khác bản đồ -> Dịch chuyển xuống mặt đất tại Khu 0 của bản đồ Boss
            if (this.zone.map.mapId != targetMapId) {
                if (Util.canDoWithTime(this.lastTimeAutoHuntAction, 2200)) {
                    this.lastTimeAutoHuntAction = System.currentTimeMillis();
                    this.autoHuntScanZone = 0;
                    Zone zone0 = MapService.gI().getZoneByMapIDAndZoneID(targetMapId, 0);
                    if (zone0 != null) {
                        int targetX = (targetBoss.location.x > 50 && targetBoss.location.x < zone0.map.mapWidth - 50)
                                ? targetBoss.location.x : 200;
                        this.changeMapVIP = true;
                        ChangeMapService.gI().changeMapInYard(this, zone0, targetX);
                        Service.gI().sendThongBao(this, "[Tự Săn Boss] Đã đến " + targetBoss.zone.map.mapName + " (Khu 0), bắt đầu dò khu tìm " + targetBoss.name + "...");
                    }
                }
                return;
            }

            // B2: Đã ở đúng bản đồ nhưng chưa đúng Khu -> Tự động dò lần lượt từng Khu (0 -> 1 -> 2...)
            if (this.zone != targetBoss.zone) {
                if (Util.canDoWithTime(this.lastTimeAutoHuntAction, 2000)) {
                    this.lastTimeAutoHuntAction = System.currentTimeMillis();
                    int numZones = this.zone.map.zones.size();
                    if (numZones > 1) {
                        int startNext = (this.zone.zoneId + 1) % numZones;
                        for (int step = 0; step < numZones; step++) {
                            int candZoneId = (startNext + step) % numZones;
                            Zone candZone = this.zone.map.zones.get(candZoneId);
                            if (candZone != null && !candZone.isFullPlayer()) {
                                this.autoHuntScanZone = candZoneId;
                                int targetX = (targetBoss.location.x > 50 && targetBoss.location.x < candZone.map.mapWidth - 50)
                                        ? targetBoss.location.x : this.location.x;
                                this.changeMapVIP = true;
                                this.idMark.setLastTimeChangeZone(System.currentTimeMillis());
                                ChangeMapService.gI().changeMapInYard(this, candZone, targetX);
                                if (candZone == targetBoss.zone) {
                                    Service.gI().sendThongBao(this, "[Tự Săn Boss] Đã tìm thấy " + targetBoss.name + " tại Khu " + candZoneId + "! Bắt đầu Tàn Sát!");
                                } else {
                                    Service.gI().sendThongBao(this, "[Tự Săn Boss] Đang dò Khu " + candZoneId + "/" + (numZones - 1) + " tìm " + targetBoss.name + "...");
                                }
                                break;
                            }
                        }
                    }
                }
                return;
            }

            // B3: Đã ở đúng Khu có Boss -> Bật Đệ tử Tấn công, áp sát Boss trên mặt đất và Tàn Sát!
            if (this.pet != null && !this.pet.isDie() && this.pet.status != Pet.ATTACK && this.pet.status != Pet.GOHOME) {
                this.pet.changeStatus(Pet.ATTACK);
            }

            if (Util.getDistance(this, targetBoss) > 120 && Util.canDoWithTime(this.lastTimeAutoHuntAction, 2000)) {
                this.lastTimeAutoHuntAction = System.currentTimeMillis();
                int bx = targetBoss.location.x + Util.nextInt(-25, 25);
                if (bx < 50) {
                    bx = 50;
                } else if (bx > this.zone.map.mapWidth - 50) {
                    bx = this.zone.map.mapWidth - 50;
                }
                int by = this.zone.map.yPhysicInTop(bx, targetBoss.location.y);
                PlayerService.gI().playerMove(this, bx, by);
                Service.gI().resetPoint(this, bx, by);
            }

            if (nro.models.services.SkillService.gI().canAttackPlayer(this, targetBoss)
                    && Util.canDoWithTime(this.lastTimeAutoHuntAttack, 350)) {
                this.lastTimeAutoHuntAttack = System.currentTimeMillis();
                if (this.playerSkill != null && this.playerSkill.skills != null) {
                    Skill chosenSkill = null;
                    // Ưu tiên bật Khiên năng lượng nếu có và đã hồi chiêu
                    if (this.effectSkill != null && !this.effectSkill.isShielding) {
                        for (Skill s : this.playerSkill.skills) {
                            if (s != null && s.skillId != -1 && s.template != null
                                    && s.template.id == Skill.KHIEN_NANG_LUONG
                                    && Util.canDoWithTime(s.lastTimeUseThisSkill, s.coolDown)) {
                                chosenSkill = s;
                                break;
                            }
                        }
                    }
                    // Chọn kỹ năng tấn công (Liên hoàn / Đấm / Chưởng) đã hồi chiêu
                    if (chosenSkill == null) {
                        int[] attackSkillPriority = {
                            Skill.LIEN_HOAN, Skill.DRAGON, Skill.DEMON, Skill.GALICK,
                            Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.KAIOKEN
                        };
                        for (int prefId : attackSkillPriority) {
                            for (Skill s : this.playerSkill.skills) {
                                if (s != null && s.skillId != -1 && s.template != null
                                        && s.template.id == prefId
                                        && Util.canDoWithTime(s.lastTimeUseThisSkill, s.coolDown)) {
                                    if (prefId == Skill.KAIOKEN && this.nPoint.hp <= this.nPoint.hpMax * 20L / 100L) {
                                        continue;
                                    }
                                    chosenSkill = s;
                                    break;
                                }
                            }
                            if (chosenSkill != null) {
                                break;
                            }
                        }
                    }
                    if (chosenSkill != null) {
                        this.playerSkill.skillSelect = chosenSkill;
                        nro.models.services.SkillService.gI().useSkill(this, targetBoss, null, -1, null);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

}
