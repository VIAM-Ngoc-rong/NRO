package nro.models.boss;

import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.boss.Boss_Manager.LunarNewYearEventManager;
import nro.models.boss.Boss_Manager.GasDestroyManager;
import nro.models.boss.Boss_Manager.RedRibbonHQManager;
import nro.models.boss.Boss_Manager.SkillSummonedManager;
import nro.models.boss.Boss_Manager.TreasureUnderSeaManager;
import nro.models.boss.Boss_Manager.FinalBossManager;
import nro.models.boss.Boss_Manager.HalloweenEventManager;
import nro.models.boss.Boss_Manager.ChristmasEventManager;
import nro.models.boss.Boss_Manager.TrungThuEventManager;
import nro.models.boss.Boss_Manager.YardartManager;
import nro.models.boss.Boss_Manager.SnakeWayManager;
import nro.models.boss.Boss_Manager.HungVuongEventManager;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.AppearType;
import nro.models.consts.BossStatus;
import nro.models.consts.BossType;
import nro.models.consts.ConstPlayer;
import static nro.models.consts.BossStatus.ACTIVE;
import static nro.models.consts.BossStatus.AFK;
import static nro.models.consts.BossStatus.CHAT_E;
import static nro.models.consts.BossStatus.CHAT_S;
import static nro.models.consts.BossStatus.DIE;
import static nro.models.consts.BossStatus.JOIN_MAP;
import static nro.models.consts.BossStatus.LEAVE_MAP;
import static nro.models.consts.BossStatus.RESPAWN;
import static nro.models.consts.BossStatus.REST;
import static nro.models.consts.BossType.BROLY;
import static nro.models.consts.BossType.CHRISTMAS_EVENT;
import static nro.models.consts.BossType.FINAL;
import static nro.models.consts.BossType.HALLOWEEN_EVENT;
import static nro.models.consts.BossType.HUNGVUONG_EVENT;
import static nro.models.consts.BossType.PHOBAN;
import static nro.models.consts.BossType.PHOBANBDKB;
import static nro.models.consts.BossType.PHOBANCDRD;
import static nro.models.consts.BossType.PHOBANDT;
import static nro.models.consts.BossType.PHOBANKGHD;
import static nro.models.consts.BossType.SKILLSUMMONED;
import static nro.models.consts.BossType.TET_EVENT;
import static nro.models.consts.BossType.TRUNGTHU_EVENT;
import static nro.models.consts.BossType.YARDART;
import nro.models.network.Message;
import java.util.List;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.server.ServerNotify;
import nro.models.map.service.MapService;
import nro.models.map.ItemMap;
import nro.models.item.Item;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.services.TaskService;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Logger;
import nro.models.utils.SkillUtil;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;
import nro.models.interfaces.IBoss;
import java.io.IOException;

public class Boss extends Player implements IBoss {

    public int currentLevel = -1;
    public final BossData[] data;

    public BossStatus bossStatus;

    protected Zone lastZone;

    public long lastTimeRest;
    public int secondsRest;

    protected long lastTimeChatS;
    protected int timeChatS;
    protected byte indexChatS;

    protected long lastTimeChatE;
    protected int timeChatE;
    protected byte indexChatE;

    protected long lastTimeChatM;
    protected int timeChatM;

    protected long lastTimeTargetPlayer;
    protected int timeTargetPlayer;
    protected Player playerTarger;

    public Boss parentBoss;
    public Boss[][] bossAppearTogether;

    public Zone zoneFinal = null;

    public Player playerReward;

    public int lv;

    public int error;

    public boolean prepareBom;

    public boolean isNotifyDisabled;
    public boolean isZone01SpawnDisabled;

    public long lastTimeSpawnOnMap;
    public volatile long lastKilledTimeMs;
    public volatile String lastKillerName = "";
    public volatile String lastKilledMapName = "";
    public volatile int lastKilledZoneId = -1;
    public volatile int totalKilledCount = 0;
    public boolean droppedBelow50Hp;
    public boolean usedTTNL;
    public boolean isUsingTTNL;
    public long lastTimeStartTTNL;
    public long lastTimeTickTTNL;
    public int ticksTTNL;

    public long lastTimeAttacked;
    public long lastRecordedHp;
    public boolean pendingIdleDespawn;
    public long timeTriggerIdleDespawn;
    public boolean pendingNotifyJoinMap;
    public long timeTriggerNotifyJoinMap;

    public Boss(int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        this(id, data);
        this.isNotifyDisabled = isNotifyDisabled;
        this.isZone01SpawnDisabled = isZone01SpawnDisabled;
    }

    public Boss(BossType bossType, int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        this(bossType, id, data);
        this.isNotifyDisabled = isNotifyDisabled;
        this.isZone01SpawnDisabled = isZone01SpawnDisabled;
    }

    public Boss(int id, BossData... data) throws Exception {
        this.id = id;
        this.isBoss = true;
        if (data == null || data.length == 0) {
            throw new Exception("Dữ liệu boss không hợp lệ");
        }
        this.data = data;
        this.name = this.data[0].getName();
        int cfgRest = this.data[0].getSecondsRest();
        this.secondsRest = (cfgRest >= 300 && cfgRest < 86400) ? Util.nextInt(300, 600) : cfgRest;
        this.bossStatus = BossStatus.REST;
        BossManager.gI().addBoss(this);

        this.bossAppearTogether = new Boss[this.data.length][];
        for (int i = 0; i < this.bossAppearTogether.length; i++) {
            if (this.data[i].getBossesAppearTogether() != null) {
                this.bossAppearTogether[i] = new Boss[this.data[i].getBossesAppearTogether().length];
                for (int j = 0; j < this.data[i].getBossesAppearTogether().length; j++) {
                    Boss boss = BossManager.gI().createBoss(this.data[i].getBossesAppearTogether()[j]);
                    if (boss != null) {
                        boss.parentBoss = this;
                        boss.lv = j;
                        this.bossAppearTogether[i][j] = boss;
                    }
                }
            }
        }
    }

    public Boss(BossType bossType, int id, BossData... data) throws Exception {
        this.id = id;
        this.isBoss = true;
        if (data == null || data.length == 0) {
            throw new Exception("Dữ liệu boss không hợp lệ");
        }
        this.data = data;
        this.name = this.data[0].getName();
        int cfgRest = this.data[0].getSecondsRest();
        this.secondsRest = (cfgRest >= 300 && cfgRest < 86400) ? Util.nextInt(300, 600) : cfgRest;
        this.bossStatus = BossStatus.REST;
        switch (bossType) {
            case YARDART ->
                YardartManager.gI().addBoss(this);
            case FINAL ->
                FinalBossManager.gI().addBoss(this);
            case SKILLSUMMONED ->
                SkillSummonedManager.gI().addBoss(this);
            case BROLY ->
                BrolyManager.gI().addBoss(this);
            case PHOBAN ->
                OtherBossManager.gI().addBoss(this);
            case PHOBANDT ->
                RedRibbonHQManager.gI().addBoss(this);
            case PHOBANBDKB ->
                TreasureUnderSeaManager.gI().addBoss(this);
            case PHOBANCDRD ->
                SnakeWayManager.gI().addBoss(this);
            case PHOBANKGHD ->
                GasDestroyManager.gI().addBoss(this);
            case TRUNGTHU_EVENT -> {
                TrungThuEventManager.gI().addBoss(this);
                BossManager.gI().addBoss(this);
            }
            case HALLOWEEN_EVENT -> {
                HalloweenEventManager.gI().addBoss(this);
                BossManager.gI().addBoss(this);
            }
            case CHRISTMAS_EVENT -> {
                ChristmasEventManager.gI().addBoss(this);
                BossManager.gI().addBoss(this);
            }
            case HUNGVUONG_EVENT -> {
                HungVuongEventManager.gI().addBoss(this);
                BossManager.gI().addBoss(this);
            }
            case TET_EVENT -> {
                LunarNewYearEventManager.gI().addBoss(this);
                BossManager.gI().addBoss(this);
            }
        }

        this.bossAppearTogether = new Boss[this.data.length][];
        for (int i = 0; i < this.bossAppearTogether.length; i++) {
            if (this.data[i].getBossesAppearTogether() != null) {
                this.bossAppearTogether[i] = new Boss[this.data[i].getBossesAppearTogether().length];
                for (int j = 0; j < this.data[i].getBossesAppearTogether().length; j++) {
                    Boss boss = BossManager.gI().createBoss(this.data[i].getBossesAppearTogether()[j]);
                    if (boss != null) {
                        boss.parentBoss = this;
                        this.bossAppearTogether[i][j] = boss;
                    }
                }
            }
        }
    }

    public int bossDamageReductionFactor = 1;

    public boolean isWorldBossForHpBoost() {
        switch ((int) this.id) {
            case BossID.KUKU, BossID.MAP_DAU_DINH, BossID.RAMBO:
                return true;
            default:
                return isBossFromTDSTNappaOnwards();
        }
    }

    @Override
    public void initBase() {
        BossData data = this.data[this.currentLevel];
        this.name = String.format(data.getName(), Util.nextInt(0, 100));
        this.gender = data.getGender();
        this.nPoint.mpg = 31_07_2002;
        this.nPoint.dameg = data.getDame();
        long rawHp = data.getHp()[Util.nextInt(0, data.getHp().length - 1)];
        if (isWorldBossForHpBoost() && rawHp >= 500_000L) {
            long boostedHp = rawHp * 3L;
            this.nPoint.hpg = (int) Math.min(2_100_000_000L, boostedHp);
            this.bossDamageReductionFactor = 1;
        } else {
            this.nPoint.hpg = (int) Math.min(2_100_000_000L, rawHp);
            this.bossDamageReductionFactor = 1;
        }
        this.nPoint.hp = nPoint.hpg;
        this.nPoint.calPoint();
        this.initSkill();
        this.resetBase();
    }

    protected void initSkill() {
        for (Skill skill : this.playerSkill.skills) {
            skill.dispose();
        }
        this.playerSkill.skills.clear();
        this.playerSkill.skillSelect = null;
        int[][] skillTemps = data[this.currentLevel].getSkillTemp();
        for (int[] skillTemp : skillTemps) {
            Skill skill = SkillUtil.createSkill(skillTemp[0], skillTemp[1]);
            if (skillTemp.length == 3) {
                skill.coolDown = skillTemp[2];
            }
            this.playerSkill.skills.add(skill);
        }
    }

    protected void resetBase() {
        this.lastTimeChatS = 0;
        this.lastTimeChatE = 0;
        this.timeChatS = 0;
        this.timeChatE = 0;
        this.indexChatS = 0;
        this.indexChatE = 0;
        long now = System.currentTimeMillis();
        this.lastTimeSpawnOnMap = now;
        this.lastTimeAttacked = now;
        this.lastRecordedHp = this.nPoint != null ? this.nPoint.hp : 0;
        this.pendingIdleDespawn = false;
        this.timeTriggerIdleDespawn = 0;
        this.pendingNotifyJoinMap = false;
        this.timeTriggerNotifyJoinMap = 0;
        this.droppedBelow50Hp = false;
        this.usedTTNL = false;
        this.isUsingTTNL = false;
        this.lastTimeStartTTNL = 0;
        this.ticksTTNL = 0;
    }

    public boolean isTieuDoiBojack() {
        return this.id == BossID.BUJIN || this.id == BossID.KOGU || this.id == BossID.ZANGYA
                || this.id == BossID.BIDO || this.id == BossID.BOJACK || this.id == BossID.SUPER_BOJACK;
    }

    public void startBossTTNL() {
        if (this.isUsingTTNL || this.isDie() || this.zone == null) {
            return;
        }
        if (!this.isTieuDoiBojack() && this.lastTimeStartTTNL > 0 && !Util.canDoWithTime(this.lastTimeStartTTNL, 60000)) {
            return;
        }
        if (this.effectSkill != null && (this.effectSkill.anTroi || this.effectSkill.isHaveEffectSkill() || this.effectSkill.isBinh || this.effectSkill.isSocola)) {
            if (!this.isTieuDoiBojack()) {
                this.usedTTNL = true;
                this.lastTimeStartTTNL = System.currentTimeMillis();
            }
            return;
        }
        this.usedTTNL = true;
        this.isUsingTTNL = true;
        this.lastTimeStartTTNL = System.currentTimeMillis();
        this.lastTimeTickTTNL = System.currentTimeMillis();
        this.ticksTTNL = 0;
        EffectSkillService.gI().sendEffectStartCharge(this);
        EffectSkillService.gI().sendEffectCharge(this);
        this.chat("Tái tạo năng lượng!");
    }

    public boolean isBrolyBoss() {
        return this.id == BossID.BROLY || this.id == BossID.SUPER_BROLY
                || (this.name != null && this.name.toLowerCase().contains("broly"));
    }

    public boolean isExcludedFrom5MinReset() {
        if (this.isBrolyBoss()) {
            return true;
        }
        if (this.zone != null && this.zone.map != null) {
            int mapId = this.zone.map.mapId;
            if (MapService.gI().isMapOffline(mapId)
                    || MapService.gI().isMapPhoBan(mapId)
                    || MapService.gI().isMapMaBu(mapId)
                    || MapService.gI().isMapMabu2H(mapId)
                    || MapService.gI().isMapYardart(mapId)
                    || MapService.gI().isMapBossFinal(mapId)
                    || mapId == 113 || mapId == 112 || mapId == 129) {
                return true;
            }
        }
        return false;
    }

    public void recordBossAttacked() {
        long now = System.currentTimeMillis();
        this.lastTimeAttacked = now;
        Boss root = this.parentBoss != null ? this.parentBoss : this;
        root.lastTimeAttacked = now;
        if (root.bossAppearTogether != null && root.currentLevel >= 0
                && root.currentLevel < root.bossAppearTogether.length
                && root.bossAppearTogether[root.currentLevel] != null) {
            for (Boss sub : root.bossAppearTogether[root.currentLevel]) {
                if (sub != null) {
                    sub.lastTimeAttacked = now;
                }
            }
        }
    }

    public long getGroupLastTimeAttacked() {
        Boss root = this.parentBoss != null ? this.parentBoss : this;
        long latest = Math.max(this.lastTimeAttacked, root.lastTimeAttacked);
        if (latest <= 0) {
            latest = Math.max(this.lastTimeSpawnOnMap, root.lastTimeSpawnOnMap);
        }
        if (root.bossAppearTogether != null && root.currentLevel >= 0
                && root.currentLevel < root.bossAppearTogether.length
                && root.bossAppearTogether[root.currentLevel] != null) {
            for (Boss sub : root.bossAppearTogether[root.currentLevel]) {
                if (sub != null && sub.bossStatus != BossStatus.REST) {
                    latest = Math.max(latest, sub.lastTimeAttacked);
                }
            }
        }
        return latest;
    }

    public boolean isPendingIdleDespawn() {
        Boss root = this.parentBoss != null ? this.parentBoss : this;
        return this.pendingIdleDespawn || root.pendingIdleDespawn;
    }

    public boolean hasRealPlayerInZone() {
        if (this.zone == null) {
            return false;
        }
        List<Player> players = this.zone.getNotBosses();
        if (players == null || players.isEmpty()) {
            return false;
        }
        for (int i = players.size() - 1; i >= 0; i--) {
            Player pl = players.get(i);
            if (pl != null && !pl.isBot && (pl.isPl() || pl.isPet)) {
                return true;
            }
        }
        return false;
    }

    public void despawnIdleBossGroup() {
        Boss root = this.parentBoss != null ? this.parentBoss : this;
        long now = System.currentTimeMillis();
        root.pendingIdleDespawn = false;
        root.pendingNotifyJoinMap = false;
        if (root.bossAppearTogether != null && root.currentLevel >= 0
                && root.currentLevel < root.bossAppearTogether.length
                && root.bossAppearTogether[root.currentLevel] != null) {
            for (Boss sub : root.bossAppearTogether[root.currentLevel]) {
                if (sub != null) {
                    sub.pendingIdleDespawn = false;
                    sub.pendingNotifyJoinMap = false;
                    if (sub.zone != null) {
                        ChangeMapService.gI().exitMap(sub);
                    }
                    sub.lastZone = null;
                    if (sub.data != null) {
                        sub.currentLevel = sub.data.length;
                    }
                    sub.lastTimeRest = now;
                    sub.changeStatus(BossStatus.REST);
                }
            }
        }
        if (root.data != null) {
            root.currentLevel = root.data.length;
        }
        root.leaveMap();
    }

    public boolean hasGroupDroppedBelow50Hp() {
        if (this.droppedBelow50Hp || (this.nPoint != null && this.nPoint.hpMax > 0 && this.nPoint.hp < this.nPoint.hpMax / 2)) {
            this.droppedBelow50Hp = true;
            return true;
        }
        Boss root = this.parentBoss != null ? this.parentBoss : this;
        if (root.droppedBelow50Hp || (root.nPoint != null && root.nPoint.hpMax > 0 && root.nPoint.hp < root.nPoint.hpMax / 2)) {
            return true;
        }
        if (root.bossAppearTogether != null && root.currentLevel >= 0 && root.currentLevel < root.bossAppearTogether.length
                && root.bossAppearTogether[root.currentLevel] != null) {
            for (Boss sub : root.bossAppearTogether[root.currentLevel]) {
                if (sub != null && sub.currentLevel >= 0 && sub.bossStatus != BossStatus.REST
                        && (sub.isDie() || sub.droppedBelow50Hp
                        || (sub.nPoint != null && sub.nPoint.hpMax > 0 && sub.nPoint.hp < sub.nPoint.hpMax / 2))) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getIdleResetTimeMs() {
        long maxHp = this.nPoint != null ? Math.max(this.nPoint.hpMax, this.nPoint.hpg) : 0;
        if (this.bossAppearTogether != null && this.currentLevel >= 0
                && this.currentLevel < this.bossAppearTogether.length
                && this.bossAppearTogether[this.currentLevel] != null) {
            for (Boss sub : this.bossAppearTogether[this.currentLevel]) {
                if (sub != null && sub.nPoint != null) {
                    maxHp = Math.max(maxHp, Math.max(sub.nPoint.hpMax, sub.nPoint.hpg));
                }
            }
        }
        return 600_000L;
    }

    private int getSafeLevelIndex() {
        if (this.data == null || this.data.length == 0) {
            return 0;
        }
        if (this.currentLevel < 0) {
            return 0;
        }
        if (this.currentLevel >= this.data.length) {
            return this.data.length - 1;
        }
        return this.currentLevel;
    }

    //.outfit.
    @Override
    public short getHead() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][0];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        }
        return this.data[getSafeLevelIndex()].getOutfit()[0];
    }

    @Override
    public short getBody() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][1];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        }
        return this.data[getSafeLevelIndex()].getOutfit()[1];
    }

    @Override
    public short getLeg() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][2];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        }
        return this.data[getSafeLevelIndex()].getOutfit()[2];

    }

    @Override
    public short getFlagBag() {
        return this.data[getSafeLevelIndex()].getOutfit()[3];
    }

    @Override
    public byte getAura() {
        return (byte) this.data[this.currentLevel].getOutfit()[4];
    }

    @Override
    public byte getEffFront() {
        return (byte) this.data[this.currentLevel].getOutfit()[5];
    }

    public Zone getMapJoin() {
        int mapId = this.data[this.currentLevel].getMapJoin()[Util.nextInt(0, this.data[this.currentLevel].getMapJoin().length - 1)];
        Zone map = MapService.gI().getMapWithRandZone(mapId);
        return map;
    }

    @Override
    public void changeStatus(BossStatus status) {
        if (status == BossStatus.ACTIVE && this.bossStatus != BossStatus.ACTIVE) {
            long now = System.currentTimeMillis();
            this.lastTimeSpawnOnMap = now;
            this.lastTimeAttacked = now;
            this.lastRecordedHp = this.nPoint != null ? this.nPoint.hp : 0;
        }
        this.bossStatus = status;
    }

    @Override
    public Player getPlayerAttack() {
        if (this.zone == null) {
            return null;
        }
        if (this.playerTarger != null && (this.playerTarger.isDie() || !this.zone.equals(this.playerTarger.zone))) {
            this.playerTarger = null;
        }
        if (this.playerTarger == null || Util.canDoWithTime(this.lastTimeTargetPlayer, this.timeTargetPlayer)) {
            this.playerTarger = this.zone.getRandomPlayerInMap();
            this.lastTimeTargetPlayer = System.currentTimeMillis();
            this.timeTargetPlayer = Util.nextInt(5000, 7000);
        }
        if (this.playerTarger != null && this.playerTarger.isPet && ((Pet) this.playerTarger).master != null && ((Pet) this.playerTarger).master.equals(this)) {
            this.playerTarger = null;
        }
        return this.playerTarger;
    }

    @Override
    public void changeToTypePK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.PK_ALL);
    }

    @Override
    public void changeToTypeNonPK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.NON_PK);
    }

    @Override
    public void updateInfo() {
        super.update();
    }

    @Override
    public void update() {
        if (prepareBom) {
            return;
        }
        super.update();
        if (this.pendingNotifyJoinMap) {
            if (this.isDie() || this.zone == null || this.zone.map == null
                    || this.bossStatus == BossStatus.REST || this.bossStatus == BossStatus.DIE
                    || this.bossStatus == BossStatus.CHAT_E || this.bossStatus == BossStatus.LEAVE_MAP) {
                this.pendingNotifyJoinMap = false;
            } else if (Util.canDoWithTime(this.timeTriggerNotifyJoinMap, 60_000L)) {
                this.pendingNotifyJoinMap = false;
                if (canSendNotify()) {
                    ServerNotify.gI().notify("BOSS " + this.name + " vừa xuất hiện tại " + this.zone.map.mapName);
                }
            }
        }
        this.nPoint.mp = this.nPoint.mpg;
        if (!this.isDie() && this.nPoint != null && this.nPoint.hpMax > 0) {
            if (this.lastRecordedHp > 0 && this.nPoint.hp < this.lastRecordedHp) {
                this.recordBossAttacked();
            }
            this.lastRecordedHp = this.nPoint.hp;
            if (this.nPoint.hp < this.nPoint.hpMax / 2) {
                this.droppedBelow50Hp = true;
            }
            if (this.isTieuDoiBojack()) {
                if (!this.isUsingTTNL && this.nPoint.hp > 0 && this.nPoint.hp < 2000) {
                    this.startBossTTNL();
                }
            } else if (!this.isUsingTTNL && this.nPoint.hp > 0 && this.nPoint.hp < (this.nPoint.hpMax * 30L / 100L)
                    && (this.lastTimeStartTTNL == 0 || Util.canDoWithTime(this.lastTimeStartTTNL, 60000))) {
                this.startBossTTNL();
            }
        }
        if (this.isUsingTTNL) {
            if (this.isDie() || this.zone == null
                    || (this.effectSkill != null && (this.effectSkill.anTroi || this.effectSkill.isHaveEffectSkill() || this.effectSkill.isBinh || this.effectSkill.isSocola))) {
                this.isUsingTTNL = false;
                if (this.zone != null) {
                    EffectSkillService.gI().sendEffectStopCharge(this);
                }
            } else {
                long targetHp = this.isTieuDoiBojack() ? Math.min(this.nPoint.hpMax, 5000L) : (this.nPoint.hpMax / 2);
                if (Util.canDoWithTime(this.lastTimeTickTTNL, 1000)) {
                    this.lastTimeTickTTNL = System.currentTimeMillis();
                    this.ticksTTNL++;
                    int remainingTicks = Math.max(1, 5 - this.ticksTTNL + 1);
                    long needHeal = targetHp - this.nPoint.hp;
                    if (needHeal > 0) {
                        long healAmount = Math.max(1L, needHeal / remainingTicks);
                        PlayerService.gI().hoiPhuc(this, healAmount, 0);
                    }
                    EffectSkillService.gI().sendEffectCharge(this);
                    this.chat("Phục hồi năng lượng " + this.nPoint.getCurrPercentHP() + "%");
                }
                if (Util.canDoWithTime(this.lastTimeStartTTNL, 5000) || this.ticksTTNL >= 5) {
                    if (this.nPoint.hp < targetHp) {
                        PlayerService.gI().hoiPhuc(this, targetHp - this.nPoint.hp, 0);
                    }
                    this.isUsingTTNL = false;
                    EffectSkillService.gI().sendEffectStopCharge(this);
                    this.chat("Phục hồi năng lượng 50%!");
                }
                return;
            }
        }
        if (this.effectSkill == null || this.effectSkill.isHaveEffectSkill() || (this.newSkill != null && this.newSkill.isStartSkillSpecial)) {
            return;
        }
        switch (this.bossStatus) {
            case REST ->
                this.rest();
            case RESPAWN -> {
                this.respawn();
                this.changeStatus(BossStatus.JOIN_MAP);
            }
            case JOIN_MAP ->
                this.joinMap();
            case CHAT_S -> {
                if (chatS()) {
                    this.doneChatS();
                    this.lastTimeChatM = System.currentTimeMillis();
                    this.timeChatM = 5000;
                    if (this.bossStatus != BossStatus.AFK) {
                        this.changeStatus(BossStatus.ACTIVE);
                    }
                }
            }
            case AFK ->
                this.afk();
            case ACTIVE -> {
                this.chatM();
                if (this.effectSkill.isCharging && !Util.isTrue(1, 20) || this.effectSkill.useTroi) {
                    return;
                }
                this.active();
            }
            case DIE ->
                this.changeStatus(BossStatus.CHAT_E);
            case CHAT_E -> {
                if (chatE()) {
                    this.doneChatE();
                    this.changeStatus(BossStatus.LEAVE_MAP);
                }
            }
            case LEAVE_MAP ->
                this.leaveMap();
        }
    }

    @Override
    public void rest() {
        int nextLevel = this.currentLevel + 1;
        if (nextLevel >= this.data.length) {
            nextLevel = 0;
        }
        if (this.data[nextLevel].getTypeAppear() == AppearType.DEFAULT_APPEAR
                && Util.canDoWithTime(lastTimeRest, secondsRest * 1000L)) {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void afk() {

    }

    @Override
    public void respawn() {
        this.currentLevel++;
        if (this.currentLevel >= this.data.length) {
            this.currentLevel = 0;
        }
        this.initBase();
        this.changeToTypeNonPK();
    }

    @Override
    public void joinMap() {
        if (zoneFinal != null) {
            joinMapByZone(zoneFinal);
            this.notifyJoinMap();
            this.changeStatus(BossStatus.CHAT_S);
            this.wakeupAnotherBossWhenAppear();
            return;
        }
        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }
        if (this.zone == null) {
            this.zone = getMapJoin();
        }
        if (this.zone != null) {
            try {
                if (this.currentLevel == 0) {
                    if (this.parentBoss == null) {
                        int totalZones = this.zone.map.zones.size();
                        int minZone = (this.isZone01SpawnDisabled && totalZones > 2) ? 2 : (totalZones > 1 ? 1 : 0);
                        int zoneid = minZone < totalZones - 1 ? Util.nextInt(minZone, totalZones - 1) : minZone;
                        int checked = 0;
                        int candidateCount = Math.max(1, totalZones - minZone);
                        while (checked < candidateCount
                                && (!this.zone.map.zones.get(zoneid).getBosses().isEmpty()
                                || this.zone.map.zones.get(zoneid).getNumOfPlayers() > 10)) {
                            zoneid++;
                            if (zoneid >= totalZones) {
                                zoneid = minZone;
                            }
                            checked++;
                        }
                        this.zone = this.zone.map.zones.get(zoneid);
                        int x = this.zone.map.mapWidth > 100 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : Util.nextInt(100);
                        int y = this.zone.map.yPhysicInTop(x, 100);
                        ChangeMapService.gI().changeMap(this, this.zone, x, y);
                    } else {
                        int x = this.parentBoss.location.x - (this.lv + 1) * 30;
                        int y = this.zone.map.yPhysicInTop(x, 100);
                        ChangeMapService.gI().changeMap(this, this.zone, x, y);
                    }
                    this.wakeupAnotherBossWhenAppear();
                } else {
                    ChangeMapService.gI().changeMap(this, this.zone, this.location.x, this.location.y);
                }
                Service.gI().sendFlagBag(this);
                this.notifyJoinMap();
                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
                if (error < 5) {
                    Logger.error("Lỗi : " + e + "\n");
                    error++;
                }
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    public void joinMapByZone(Zone zone) {
        if (zone != null) {
            this.zone = zone;
            int x = this.zone.map.mapWidth > 100 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : Util.nextInt(100);
            int y = this.zone.map.yPhysicInTop(x, 100);
            ChangeMapService.gI().changeMap(this, this.zone, x, y);
        }
    }

    protected void notifyJoinMap() {
        if (canSendNotify()) {
            this.pendingNotifyJoinMap = true;
            this.timeTriggerNotifyJoinMap = System.currentTimeMillis();
        }
    }

    public boolean canSendNotify() {
        return this.zone != null && this.zone.map != null
                && this.name != null && !this.name.isEmpty()
                && !(this.isNotifyDisabled
                || this.id == BossID.SOI_HEC_QUYN1 || this.id == BossID.SOI_HEC_QUYN
                || this.id == BossID.AN_TROM || this.id == BossID.O_DO1
                || this.zone.map.mapId == 140
                || this.zone.map.mapId == 111
                || MapService.gI().isMapPhoBan(this.zone.map.mapId)
                || MapService.gI().isMapMaBu(this.zone.map.mapId)
                || MapService.gI().isMapMabu2H(this.zone.map.mapId)
                || MapService.gI().isMapYardart(this.zone.map.mapId)
                || MapService.gI().isMapBlackBallWar(this.zone.map.mapId));
    }

    @Override
    public boolean chatS() {
        if (Util.canDoWithTime(lastTimeChatS, timeChatS)) {
            if (this.indexChatS == this.data[this.currentLevel].getTextS().length) {
                return true;
            }
            String textChat = this.data[this.currentLevel].getTextS()[this.indexChatS];
            int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
            textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
            if (!this.chat(prefix, textChat)) {
                return false;
            }
            this.lastTimeChatS = System.currentTimeMillis();
            this.timeChatS = textChat.length() * 100;
            if (this.timeChatS > 2000) {
                this.timeChatS = 2000;
            }
            this.indexChatS++;
        }
        return false;
    }

    @Override
    public void doneChatS() {

    }

    @Override
    public void chatM() {
        if (this.typePk == ConstPlayer.NON_PK) {
            return;
        }
        if (this.data[this.currentLevel].getTextM().length == 0) {
            return;
        }
        if (!Util.canDoWithTime(this.lastTimeChatM, this.timeChatM)) {
            return;
        }
        String textChat = this.data[this.currentLevel].getTextM()[Util.nextInt(0, this.data[this.currentLevel].getTextM().length - 1)];
        int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
        textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
        this.chat(prefix, textChat);
        this.lastTimeChatM = System.currentTimeMillis();
        this.timeChatM = Util.nextInt(3000, 20000);
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    protected long lastTimeAttack;

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void checkPlayerDie(Player player) {
        if (player.isDie()) {

        }
    }

    protected int getRangeCanAttackWithSkillSelect() {
        int skillId = this.playerSkill.skillSelect.template.id;
        if (skillId == Skill.KAMEJOKO || skillId == Skill.MASENKO || skillId == Skill.ANTOMIC) {
            return Skill.RANGE_ATTACK_CHIEU_CHUONG;
        } else if (skillId == Skill.DRAGON || skillId == Skill.DEMON || skillId == Skill.GALICK || skillId == Skill.LIEN_HOAN || skillId == Skill.KAIOKEN) {
            return Skill.RANGE_ATTACK_CHIEU_DAM;
        }
        return 500;
    }

    @Override
    protected void setDie(Player plAtt) {
        super.setDie(plAtt);
        recordBossKill(plAtt);
    }

    public void recordBossKill(Player plKill) {
        try {
            if (plKill == null || plKill == this) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - this.lastKilledTimeMs < 2000L) {
                return;
            }
            if (this.id == BossID.DEATH_BEAM_1 || this.id == BossID.DEATH_BEAM_2
                    || this.id == BossID.DEATH_BEAM_3 || this.id == BossID.DEATH_BEAM_4
                    || this.id == BossID.DEATH_BEAM_5 || "$".equals(this.name)) {
                return;
            }
            int mapId = (this.zone != null && this.zone.map != null) ? this.zone.map.mapId : -1;
            if (mapId != -1 && MapService.gI().isMapOffline(mapId)) {
                return;
            }
            String mapName = (this.zone != null && this.zone.map != null) ? this.zone.map.mapName : "Không rõ";
            int zoneId = (this.zone != null) ? this.zone.zoneId : -1;
            long hpMax = (this.nPoint != null) ? this.nPoint.hpMax : 0L;

            Player killerOwner = (plKill.isPet && ((Pet) plKill).master != null) ? ((Pet) plKill).master : plKill;
            boolean killedByBot = killerOwner.isBot;
            boolean killedByPet = plKill.isPet;
            String killerDisplay = killerOwner.name != null ? killerOwner.name : "Ẩn danh";
            if (killedByPet) {
                killerDisplay = killerDisplay + " (Đệ tử)";
            }

            this.lastKilledTimeMs = now;
            this.lastKillerName = killerDisplay;
            this.lastKilledMapName = mapName;
            this.lastKilledZoneId = zoneId;
            this.totalKilledCount++;

            String bName = this.name != null ? this.name
                    : (this.data != null && this.data.length > 0 ? this.data[0].getName() : ("Boss #" + this.id));
            nro.models.server.AdminWebServer.recordBossKillHistory(
                    now, (int) this.id, bName, hpMax, killerDisplay, killedByBot, mapName, mapId, zoneId
            );
        } catch (Exception ignored) {
        }
    }

    @Override
    public void die(Player plKill) {
        this.pendingNotifyJoinMap = false;
        this.recordBossAttacked();
        this.recordBossKill(plKill);
        Player plOwner = plKill != null && plKill.isPet ? ((Pet) plKill).master : plKill;
        if (plOwner != null && !plOwner.isBot && plOwner.isPl()) {
            plOwner.pointSanBoss++;
            Service.gI().sendThongBao(plOwner, "Bạn nhận được 1 điểm săn Boss (Hiện có: " + plOwner.pointSanBoss + " điểm)");
        }
        if (plKill != null
                && (this.zone.map.mapId != 140 || !MapService.gI().isMapMaBu(this.zone.map.mapId)
                || !MapService.gI().isMapDoanhTrai(this.zone.map.mapId)
                || !MapService.gI().isMapBanDoKhoBau(this.zone.map.mapId))) {
            if (!plKill.isBot) {
                reward(plKill);
                dropExtraBossReward(plKill);
            }
            ServerNotify.gI().notify(plKill.name + ": Đã tiêu diệt được " + this.name + " mọi người đều ngưỡng mộ.");
            this.changeStatus(BossStatus.DIE);
        } else {
            if (plKill != null && !plKill.isBot) {
                reward(plKill);
                dropExtraBossReward(plKill);
            }
            this.changeStatus(BossStatus.DIE);
        }
    }

    public boolean isDailyEarlyBoss() {
        switch ((int) this.id) {
            case BossID.BUJIN, BossID.KOGU, BossID.ZANGYA, BossID.BIDO, BossID.BOJACK, BossID.SUPER_BOJACK,
                 BossID.SO_4_NM, BossID.SO_3_NM, BossID.SO_2_NM, BossID.SO_1_NM, BossID.TIEU_DOI_TRUONG_NM:
                return true;
            default:
                return false;
        }
    }

    public boolean isMiniBoss() {
        return this.id == BossID.AN_TROM || this.id == BossID.O_DO1
                || this.id == BossID.SOI_HEC_QUYN1 || this.id == BossID.SOI_HEC_QUYN;
    }

    public boolean isBossFromNappaOnwards() {
        if (isDailyEarlyBoss() || isMiniBoss() || isBrolyBoss()) {
            return false;
        }
        switch ((int) this.id) {
            case BossID.KUKU, BossID.MAP_DAU_DINH, BossID.RAMBO:
                return true;
            default:
                return isBossFromTDSTNappaOnwards();
        }
    }

    public boolean isBossFromTDSTNappaOnwards() {
        if (isDailyEarlyBoss() || isMiniBoss() || isBrolyBoss()) {
            return false;
        }
        switch ((int) this.id) {
            case BossID.SO_4, BossID.SO_3, BossID.SO_2, BossID.SO_1, BossID.TIEU_DOI_TRUONG, BossID.TDST,
                 BossID.FIDE, BossID.COOLER, BossID.ANDROID_19, BossID.DR_KORE,
                 BossID.ANDROID_13, BossID.ANDROID_14, BossID.ANDROID_15,
                 BossID.PIC, BossID.POC, BossID.KING_KONG,
                 BossID.XEN_BO_HUNG, BossID.SIEU_BO_HUNG,
                 BossID.XEN_CON_1, BossID.XEN_CON_2, BossID.XEN_CON_3, BossID.XEN_CON_4, BossID.XEN_CON_5, BossID.XEN_CON_6, BossID.XEN_CON_7,
                 BossID.BLACK_GOKU, BossID.ZAMASU, BossID.CUMBER, BossID.GOLDEN_FRIEZA,
                 BossID.BASIL, BossID.LAVENDER, BossID.BERGAMO, BossID.MABU_14H,
                 BossID.DEATH_BEAM_1, BossID.DEATH_BEAM_2, BossID.DEATH_BEAM_3, BossID.DEATH_BEAM_4, BossID.DEATH_BEAM_5,
                 BossID.HIT, BossID.CHILL_1, BossID.CHILL_2, BossID.HATCHIYACK, BossID.DR_LYCHEE,
                 BossID.DRABURA, BossID.BUI_BUI, BossID.YA_CON, BossID.MABU_12H, BossID.DRABURA_2, BossID.BUI_BUI_2, BossID.GOKU, BossID.CADIC, BossID.DRABURA_3,
                 BossID.MABU, BossID.SUPERBU, BossID.BABY, BossID.CADIC_M:
                return true;
            default:
                break;
        }
        if (this.zone != null && this.zone.map != null) {
            int mapId = this.zone.map.mapId;
            if (MapService.gI().isMapNappa(mapId) || MapService.gI().isMapTuongLai(mapId) || MapService.gI().isMapCold(mapId)
                    || MapService.gI().isMapNgucTu(mapId) || MapService.gI().isMapMaBu(mapId)
                    || MapService.gI().isMapMabu2H(mapId) || MapService.gI().isMapCadic(mapId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBossFromFutureOnwards() {
        if (isDailyEarlyBoss() || isMiniBoss() || isBrolyBoss()) {
            return false;
        }
        switch ((int) this.id) {
            case BossID.BLACK_GOKU, BossID.ZAMASU, BossID.CUMBER, BossID.COOLER,
                 BossID.BASIL, BossID.LAVENDER, BossID.BERGAMO, BossID.MABU_14H,
                 BossID.XEN_BO_HUNG, BossID.SIEU_BO_HUNG,
                 BossID.XEN_CON_1, BossID.XEN_CON_2, BossID.XEN_CON_3, BossID.XEN_CON_4, BossID.XEN_CON_5, BossID.XEN_CON_6, BossID.XEN_CON_7,
                 BossID.ANDROID_19, BossID.DR_KORE, BossID.ANDROID_13, BossID.ANDROID_14, BossID.ANDROID_15,
                 BossID.PIC, BossID.POC, BossID.KING_KONG,
                 BossID.GOLDEN_FRIEZA, BossID.HIT, BossID.CHILL_1, BossID.CHILL_2,
                 BossID.MABU_12H, BossID.MABU, BossID.SUPERBU, BossID.BABY, BossID.CADIC_M:
                return true;
            default:
                break;
        }
        if (this.zone != null && this.zone.map != null) {
            int mapId = this.zone.map.mapId;
            if (MapService.gI().isMapTuongLai(mapId) || MapService.gI().isMapCold(mapId)
                    || MapService.gI().isMapNgucTu(mapId) || MapService.gI().isMapMaBu(mapId)
                    || MapService.gI().isMapMabu2H(mapId) || MapService.gI().isMapCadic(mapId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDailyBossDropNgocRongTim() {
        switch ((int) this.id) {
            case BossID.BUJIN, BossID.KOGU, BossID.ZANGYA, BossID.BIDO, BossID.BOJACK, BossID.SUPER_BOJACK,
                 BossID.SO_4_NM, BossID.SO_3_NM, BossID.SO_2_NM, BossID.SO_1_NM, BossID.TIEU_DOI_TRUONG_NM,
                 BossID.SO_4, BossID.SO_3, BossID.SO_2, BossID.SO_1, BossID.TIEU_DOI_TRUONG, BossID.TDST,
                 BossID.KUKU, BossID.MAP_DAU_DINH, BossID.RAMBO, BossID.FIDE,
                 BossID.ANDROID_19, BossID.DR_KORE, BossID.ANDROID_13, BossID.ANDROID_14, BossID.ANDROID_15,
                 BossID.PIC, BossID.POC, BossID.KING_KONG:
                return true;
            default:
                return false;
        }
    }

    protected void dropExtraBossReward(Player plKill) {
        if (plKill == null || this.zone == null || this.zone.map == null || isMiniBoss()) {
            return;
        }
        Player plOwner = plKill.isPet ? ((Pet) plKill).master : plKill;
        if (plOwner == null) {
            plOwner = plKill;
        }
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

        // Rớt Hồng Ngọc (ID 861) khi tiêu diệt Boss (phục vụ mua Cải trang tối ưu hành tinh >1000 Hồng ngọc)
        long bossHpForRuby = this.nPoint != null ? Math.max(1L, (long) this.nPoint.hpMax) : 1L;
        int rubyDrop;
        if (this.isDailyEarlyBoss()) {
            // Tiểu đội Bojack & TDST Namek: rớt 20 - 30 Hồng Ngọc / 1 Boss
            rubyDrop = Util.nextInt(20, 30);
        } else if (bossHpForRuby < 50_000_000L) {
            rubyDrop = Util.nextInt(1, 3);
        } else if (bossHpForRuby <= 300_000_000L) {
            rubyDrop = Util.nextInt(3, 6);
        } else {
            rubyDrop = Util.nextInt(6, 12);
        }
        ItemMap rubyMap = new ItemMap(this.zone, 861, rubyDrop, x + Util.nextInt(-20, 20), y, plOwner.id);
        Service.gI().dropItemMap(this.zone, rubyMap);

        // 0. Đánh Boss rớt 5 cục vàng
        long bossHp = this.nPoint != null ? Math.max(1L, (long) this.nPoint.hpMax) : 1L;
        int piles = 5;
        long baseGoldPerPile;
        if (this.id == BossID.SO_4 || this.id == BossID.SO_3 || this.id == BossID.SO_2 || this.id == BossID.SO_1) {
            baseGoldPerPile = 1_000_000L;
        } else if (bossHp < 20_000_000L) {
            baseGoldPerPile = 500_000L;
        } else if (bossHp <= 45_000_000L) {
            baseGoldPerPile = 1_000_000L;
        } else if (bossHp < 100_000_000L) {
            baseGoldPerPile = 1_500_000L;
        } else if (bossHp < 300_000_000L) {
            baseGoldPerPile = 2_000_000L + (bossHp - 100_000_000L) / 200L;
        } else if (bossHp < 1_000_000_000L) {
            baseGoldPerPile = 4_000_000L + (bossHp - 300_000_000L) / 350L;
        } else {
            baseGoldPerPile = 8_000_000L + Math.min(2_000_000L, (bossHp - 1_000_000_000L) / 500L);
        }
        if (TimeUtil.isWeekend()) {
            baseGoldPerPile *= 2L;
        }
        int goldPerPile = (int) Math.min(2_000_000_000L, Math.max(100_000L, baseGoldPerPile));
        for (int i = 0; i < piles; i++) {
            ItemMap goldMap = new ItemMap(this.zone, 190, goldPerPile, x + (i - 2) * 18, y, plOwner.id);
            Service.gI().dropItemMap(this.zone, goldMap);
        }

        // 1. Chỉ các Boss từ Nappa trở đi mới có cơ hội rơi Đồ (Trang bị).
        // Các Boss hằng ngày như TDST Namek, Bojack không rơi đồ, chỉ rơi Ngọc Rồng.
        boolean droppedEquip = false;
        boolean canDropEquip = isBossFromNappaOnwards();

        if (canDropEquip && isBossFromFutureOnwards()) {
            long hpBoss = Math.max(1L, (long) this.nPoint.hpMax);
            int ratePer1000 = (int) Math.min(250L, 20L + (hpBoss / 10_000_000L));
            if (Util.isTrue(ratePer1000, 1000)) {
                ItemMap godItem = ItemService.gI().randDoTLBoss(this.zone, 1, x, y, plOwner.id);
                if (godItem != null) {
                    Service.gI().dropItemMap(this.zone, godItem);
                    droppedEquip = true;
                }
            }
        }

        if (canDropEquip && !droppedEquip && Util.isTrue(50, 100)) {
            int group = Util.nextInt(1, 100) <= 70 ? 0 : 1;
            int[][] shopHighValueDrops = {
                {230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277},
                {254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 278, 279, 280, 281}
            };
            int dropItemId = shopHighValueDrops[group][Util.nextInt(0, shopHighValueDrops[group].length - 1)];
            ItemMap shopItemMap = new ItemMap(this.zone, dropItemId, 1, x, y, plOwner.id);
            List<Item.ItemOption> shopOps = ItemService.gI().getListOptionItemShop((short) dropItemId);
            shopOps.forEach(option -> option.param = (int) (option.param * Util.nextInt(100, 115) / 100.0));
            shopItemMap.options.addAll(shopOps);
            int randStar = Util.nextInt(1, 100);
            int stars = randStar <= 75 ? Util.nextInt(1, 3) : (randStar <= 95 ? Util.nextInt(4, 5) : 6);
            shopItemMap.options.add(new Item.ItemOption(107, stars));
            Service.gI().dropItemMap(this.zone, shopItemMap);
            droppedEquip = true;
        }

        // 2. Khi rớt Ngọc Rồng (hoặc Boss chỉ rớt Ngọc Rồng):
        // - Một khi rớt Ngọc Rồng Tím (1832/1833) thì KHÔNG rớt Ngọc Rồng Thường và ngược lại.
        // - Khi rớt Ngọc Rồng Thường: chỉ rớt từ 4 sao trở lên (1s, 2s, 3s, 4s - ID 14..17), tuyệt đối KHÔNG rớt 5s trở xuống (18..20).
        // - Các Boss hằng ngày như TDST Namek, Bojack: tỷ lệ rớt 1-2s thấp.
        if (!droppedEquip) {
            boolean canDropPurpleBall = isDailyBossDropNgocRongTim() || isBossFromNappaOnwards();
            boolean dropPurpleBall = canDropPurpleBall && Util.isTrue(50, 100);

            if (dropPurpleBall) {
                int nrTimId = Util.isTrue(50, 100) ? 1832 : 1833;
                ItemMap nrTimMap = new ItemMap(this.zone, nrTimId, 1, x + Util.nextInt(-25, 25), y, plOwner.id);
                Service.gI().dropItemMap(this.zone, nrTimMap);
            } else {
                int nroId;
                int rdNro = Util.nextInt(1, 100);
                if (isDailyEarlyBoss() || !isBossFromNappaOnwards()) {
                    // Boss hằng ngày (TDST Namek, Bojack, ...): chỉ rớt 1s-4s nhưng tỷ lệ 1s-2s thấp
                    if (rdNro <= 3) {
                        nroId = 14; // Ngọc rồng 1 sao (3%)
                    } else if (rdNro <= 10) {
                        nroId = 15; // Ngọc rồng 2 sao (7%)
                    } else if (rdNro <= 45) {
                        nroId = 16; // Ngọc rồng 3 sao (35%)
                    } else {
                        nroId = 17; // Ngọc rồng 4 sao (55%)
                    }
                } else {
                    // Các Boss từ Nappa trở đi: chỉ rớt từ 4 sao trở lên (1s, 2s, 3s, 4s), không rớt 5s-7s
                    if (rdNro <= 10) {
                        nroId = 14; // Ngọc rồng 1 sao (10%)
                    } else if (rdNro <= 25) {
                        nroId = 15; // Ngọc rồng 2 sao (15%)
                    } else if (rdNro <= 60) {
                        nroId = 16; // Ngọc rồng 3 sao (35%)
                    } else {
                        nroId = 17; // Ngọc rồng 4 sao (40%)
                    }
                }
                ItemMap nroMap = new ItemMap(this.zone, nroId, 1, x, y, plOwner.id);
                Service.gI().dropItemMap(this.zone, nroMap);
            }
        }
    }

    @Override
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public boolean chatE() {
        if (Util.canDoWithTime(lastTimeChatE, timeChatE)) {
            if (this.indexChatE == this.data[this.currentLevel].getTextE().length) {
                return true;
            }
            String textChat = this.data[this.currentLevel].getTextE()[this.indexChatE];
            int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
            textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
            if (!this.chat(prefix, textChat)) {
                return false;
            }
            this.lastTimeChatE = System.currentTimeMillis();
            this.timeChatE = textChat.length() * 100;
            if (this.timeChatE > 2000) {
                this.timeChatE = 2000;
            }
            this.indexChatE++;
        }
        return false;
    }

    @Override
    public void doneChatE() {

    }

    @Override
    public void leaveMap() {
        this.pendingNotifyJoinMap = false;
        this.pendingIdleDespawn = false;
        if (this.currentLevel < this.data.length - 1) {
            this.lastZone = this.zone;
            this.changeStatus(BossStatus.RESPAWN);
        } else {
            ChangeMapService.gI().exitMap(this);
            this.lastZone = null;
            int cfgRest = this.data[0].getSecondsRest();
            if (cfgRest >= 300 && cfgRest < 86400) {
                this.secondsRest = Util.nextInt(300, 600);
            }
            this.lastTimeRest = System.currentTimeMillis();
            this.changeStatus(BossStatus.REST);
        }
        this.wakeupAnotherBossWhenDisappear();
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (this.isPendingIdleDespawn()) {
                return 0;
            }
            if (plAtt != null && !plAtt.equals(this)) {
                this.recordBossAttacked();
                setTemporaryEnemies(plAtt);
                plAtt.setTemporaryEnemies(this);
            }
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (plAtt != null && plAtt.idNRNM != -1) {
                return 1;
            }
            this.nPoint.subHP(damage);

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }

            return (int) damage;
        } else {
            return 0;
        }
    }

    @Override
    public void moveToPlayer(Player pl) {
        if (pl.location != null) {
            moveTo(pl.location.x, pl.location.y);
        }
    }

    @Override
    public void moveTo(int x, int y) {
        byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
        byte move = (byte) Util.nextInt(40, 60);
        PlayerService.gI().playerMove(this, this.location.x + (dir == 1 ? move : -move), y + (Util.isTrue(3, 10) ? -50 : 0));
    }

    public void chat(String text) {
        Service.gI().chat(this, text);
    }

    protected boolean chat(int prefix, String textChat) {
        if (prefix == -1) {
            this.chat(textChat);
        } else if (prefix == -2) {
            if (this.zone != null) {
                Player plMap = this.zone.getRandomPlayerInMap();
                if (plMap != null && !plMap.isDie() && Util.getDistance(this, plMap) <= 600) {
                    Service.gI().chat(plMap, textChat);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (prefix == -3) {
            if (this.parentBoss != null && !this.parentBoss.isDie()) {
                this.parentBoss.chat(textChat);
            }
        } else if (prefix >= 0) {
            if (this.bossAppearTogether != null && this.bossAppearTogether[this.currentLevel] != null) {
                Boss boss = this.bossAppearTogether[this.currentLevel][prefix];
                if (!boss.isDie()) {
                    boss.chat(textChat);
                }
            } else if (this.parentBoss != null && this.parentBoss.bossAppearTogether != null
                    && this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] != null) {
                Boss boss = this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel][prefix];
                if (!boss.isDie()) {
                    boss.chat(textChat);
                }
            }
        }
        return true;
    }

    @Override
    public void wakeupAnotherBossWhenAppear() {
        if (this.bossAppearTogether == null || this.bossAppearTogether[this.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
            int nextLevelBoss = boss.currentLevel + 1;
            if (nextLevelBoss >= boss.data.length) {
                nextLevelBoss = 0;
            }
            if (boss.data[nextLevelBoss].getTypeAppear() == AppearType.CALL_BY_ANOTHER) {
                if (boss.zone != null) {
                    boss.leaveMap();
                }
            }
            if (boss.data[nextLevelBoss].getTypeAppear() == AppearType.APPEAR_WITH_ANOTHER) {
                if (boss.zone != null) {
                    boss.leaveMap();
                }
                boss.changeStatus(BossStatus.RESPAWN);
            }
        }
    }

    @Override
    public void wakeupAnotherBossWhenDisappear() {
    }

    @Override
    public void autoLeaveMap() {

    }

    public void leaveMapNew() {
        // Không tự biến mất theo thời gian; Boss chỉ biến mất khi bị tiêu diệt
    }

    @Override
    public void setBom(Player plAtt) {
        try {
            if (!prepareBom) {
                prepareBom = true;
                this.nPoint.hp = 1;
                long lastTime = System.currentTimeMillis();
                //gồng tự sát
                Service.gI().chat(Boss.this, "Rồi, rồi, mày xong rồi!");
                Message msg;
                try {
                    msg = new Message(-45);
                    msg.writer().writeByte(7);
                    msg.writer().writeInt((int) Boss.this.id);
                    msg.writer().writeShort(104);
                    msg.writer().writeShort(2000);
                    Service.gI().sendMessAllPlayerInMap(Boss.this, msg);
                    msg.cleanup();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (prepareBom) {
                    if (Util.canDoWithTime(lastTime, 2500)) {
                        setDie(this);
                        die(plAtt);
                        long dame = Boss.this.nPoint.hpMax;
                        for (Mob mob : Boss.this.zone.mobs) {
                            mob.injured(Boss.this, dame, true);
                        }
                        List<Player> playersMap = Boss.this.zone.getNotBosses();
                        if (!MapService.gI().isMapOffline(Boss.this.zone.map.mapId)) {
                            //Sử dụng vòng for lặp ngược để hạn chế lỗi đồng bộ
                            for (int i = playersMap.size() - 1; i >= 0; i--) {
                                Player pl = playersMap.get(i);
                                if (!Boss.this.equals(pl)) {
                                    pl.injured(Boss.this, dame, false, false);
                                    PlayerService.gI().sendInfoHpMpMoney(pl);
                                    Service.gI().Send_Info_NV(pl);
                                }
                            }
                        }
                        prepareBom = false;
                    } else {
                        nro.models.utils.Functions.sleep(100);
                    }
                }
            }
        } catch (Exception e) {
            if (prepareBom) {
                prepareBom = false;
            }
            setDie(this);
            die(plAtt);
        }
    }

}
