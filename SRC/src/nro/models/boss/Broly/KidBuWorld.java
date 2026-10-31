package nro.models.boss.Broly;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.SkillService;
import nro.models.services.TaskService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

public class KidBuWorld extends Boss {

    private boolean killedByPlayer = false;

    public KidBuWorld() throws Exception {
        super(BossID.KID_BU_MAP - Util.nextInt(1, 100000), false, true, new BossData(
                "Kid Bư",
                ConstPlayer.XAYDA,
                new short[]{439, 440, 441, -1, -1, -1},
                500,
                new int[]{2_500},
                new int[]{5, 13, 20, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38},
                new int[][]{
                    {Skill.TAI_TAO_NANG_LUONG, 1, 1000}, {Skill.TAI_TAO_NANG_LUONG, 2, 1000}, {Skill.TAI_TAO_NANG_LUONG, 3, 1000}, {Skill.TAI_TAO_NANG_LUONG, 4, 1000}, {Skill.TAI_TAO_NANG_LUONG, 5, 1000}, {Skill.TAI_TAO_NANG_LUONG, 6, 1000}, {Skill.TAI_TAO_NANG_LUONG, 7, 100},
                    {Skill.DRAGON, 1, 1000}, {Skill.DRAGON, 2, 1000}, {Skill.DRAGON, 3, 1000}, {Skill.DRAGON, 4, 1000}, {Skill.DRAGON, 5, 1000}, {Skill.DRAGON, 6, 1000}, {Skill.DRAGON, 7, 1000},
                    {Skill.DEMON, 1, 1000}, {Skill.DEMON, 2, 1000}, {Skill.DEMON, 3, 1000}, {Skill.DEMON, 4, 1000}, {Skill.DEMON, 5, 1000}, {Skill.DEMON, 6, 1000}, {Skill.DEMON, 7, 1000},
                    {Skill.GALICK, 1, 1000}, {Skill.GALICK, 2, 1000}, {Skill.GALICK, 3, 1000}, {Skill.GALICK, 4, 1000}, {Skill.GALICK, 5, 1000}, {Skill.GALICK, 6, 1000}, {Skill.GALICK, 7, 1000},
                    {Skill.KAMEJOKO, 1, 1000}, {Skill.KAMEJOKO, 2, 1000}, {Skill.KAMEJOKO, 3, 1000}, {Skill.KAMEJOKO, 4, 1000}, {Skill.KAMEJOKO, 5, 1000}, {Skill.KAMEJOKO, 6, 1000}, {Skill.KAMEJOKO, 7, 1000},
                    {Skill.MASENKO, 1, 1000}, {Skill.MASENKO, 2, 1000}, {Skill.MASENKO, 3, 1000}, {Skill.MASENKO, 4, 1000}, {Skill.MASENKO, 5, 1000}, {Skill.MASENKO, 6, 1000}, {Skill.MASENKO, 7, 1000},
                    {Skill.ANTOMIC, 1, 1000}, {Skill.ANTOMIC, 2, 1000}, {Skill.ANTOMIC, 3, 1000}, {Skill.ANTOMIC, 4, 1000}, {Skill.ANTOMIC, 5, 1000}, {Skill.ANTOMIC, 6, 1000}, {Skill.ANTOMIC, 7, 1000}
                },
                new String[]{"|-1|Hahahaha! Ta sẽ hủy diệt tất cả!"},
                new String[]{
                    "|-1|Ugaaa! Hahahaha!",
                    "|-1|Ai dám cản đường Kid Bư?!",
                    "|-1|Vào hết đây!!!"
                },
                new String[]{"|-1|Hừm... Ta sẽ biến hình!"},
                300
        ));
    }

    @Override
    public void active() {
        super.active();
        if (this.nPoint.hpMax == 7_500_000) {
            this.leaveMap();
        }
    }

    @Override
    public void initBase() {
        super.initBase();
        this.killedByPlayer = false;
        this.name = "Kid Bư " + Util.nextInt(10, 100);
        // Máu khởi điểm x5 so với Broly thường: 2.500 - 500.000 HP
        this.nPoint.hpMax = Util.nextInt(2_500, 500_000);
        this.nPoint.hpg = this.nPoint.hpMax;
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dameg = Math.max(50, this.nPoint.hpMax / 100);
        this.nPoint.dame = this.nPoint.dameg;
        this.nPoint.crit = Util.nextInt(50);
        this.bossDamageReductionFactor = 1;
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (plAtt != null && plAtt.idNRNM != -1) {
                return 1;
            }
            if (Util.isTrue(1, 10)) {
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
                this.tangChiSo();
                SkillService.gI().useSkill(this, null, null, -1, null);
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            boolean isTuSat = plAtt != null && plAtt.playerSkill != null
                    && plAtt.playerSkill.skillSelect != null
                    && plAtt.playerSkill.skillSelect.template.id == Skill.TU_SAT;
            boolean isSpecialSkill = piercing || SkillUtil.isUseSkillSpecial(plAtt)
                    || (this.effectSkill != null && this.effectSkill.isBinh);
            if (!isTuSat && !isSpecialSkill) {
                long maxDamage = Math.max(1L, (long) this.nPoint.hpMax / 100L);
                if (damage > maxDamage) {
                    damage = maxDamage;
                }
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return (int) damage;
        }
        return 0;
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(7, this.playerSkill.skills.size() - 1));
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
                    if (Util.isTrue(1, 100)) {
                        this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
                        this.tangChiSo();
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

    private void tangChiSo() {
        long hpMax = this.nPoint.hpMax;
        int rand = Util.nextInt(10, 100);
        hpMax = hpMax + hpMax / rand < 80_353_885L ? hpMax + hpMax / rand : 80_353_885L;
        this.nPoint.hpg = (int) hpMax;
        this.nPoint.hpMax = (int) hpMax;
        this.nPoint.dame = (int) (hpMax / 10L);
    }

    @Override
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public void die(Player plKill) {
        this.killedByPlayer = true;
        this.changeStatus(BossStatus.DIE);
    }

    @Override
    public void leaveMap() {
        Zone zone = this.zone;
        int x = this.location.x;
        int y = this.location.y;
        boolean canSpawnMabu = false;
        if (zone != null && zone.zoneId >= 1 && (this.killedByPlayer || (this.nPoint != null && this.nPoint.hpMax == 7_500_000))) {
            boolean enoughHp = this.nPoint != null && this.nPoint.hpMax >= 1_000_000;
            boolean lowChanceBelow1M = Util.isTrue(5, 100);
            canSpawnMabu = enoughHp || lowChanceBelow1M;
        }
        this.killedByPlayer = false;
        this.firstEncounterPlayerTimeMs = 0L;
        ChangeMapService.gI().exitMap(this);
        if (canSpawnMabu) {
            try {
                new MabuMapWorld(zone, x, y);
            } catch (Exception ex) {
            }
        }
        this.lastZone = null;
        this.secondsRest = 300;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }
}

