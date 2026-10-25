package nro.models.boss.Broly;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

public class MabuMapWorld extends Boss {

    public MabuMapWorld(Zone targetZone) throws Exception {
        super(BossID.MABU_MAP - Util.nextInt(1, 100000), false, false, new BossData(
                "Mabư Mập",
                ConstPlayer.XAYDA,
                new short[]{297, 298, 299, -1, -1, -1},
                15_000,
                new int[]{10_000_000},
                new int[]{5, 13, 20, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38},
                new int[][]{
                    {Skill.DRAGON, 7, 1000},
                    {Skill.DEMON, 7, 1000},
                    {Skill.GALICK, 7, 1000},
                    {Skill.KAMEJOKO, 7, 1000},
                    {Skill.MASENKO, 7, 1000},
                    {Skill.ANTOMIC, 7, 1000}
                },
                new String[]{"|-1|Bư! Bư! Biến tất cả thành kẹo socola!"},
                new String[]{
                    "|-1|Bư đói bụng quá!",
                    "|-1|Măm măm! Ngon quá!",
                    "|-1|Bư! Bư! Bư!"
                },
                new String[]{"|-1|Bư đau quá..."},
                1
        ));
        this.zoneFinal = targetZone;
        this.changeStatus(BossStatus.RESPAWN);
    }

    @Override
    public void initBase() {
        super.initBase();
        // Lượng máu giống cơ chế của Broly thường: 1.000.000 - 20.000.000 HP
        this.nPoint.hpg = Util.nextInt(1_000_000, 20_000_000);
        this.nPoint.hpMax = this.nPoint.hpg;
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dameg = Math.max(5_000, this.nPoint.hpMax / 1000);
        this.nPoint.dame = this.nPoint.dameg;
        this.nPoint.crit = 20;
        this.bossDamageReductionFactor = 1;
    }

    @Override
    public void joinMap() {
        if (this.zoneFinal != null) {
            this.zone = this.zoneFinal;
            int x = this.zone.map.mapWidth > 100 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : Util.nextInt(100);
            int y = this.zone.map.yPhysicInTop(x, 100);
            ChangeMapService.gI().changeMap(this, this.zone, x, y);
            Service.gI().sendFlagBag(this);
            this.notifyJoinMap();
            this.changeStatus(BossStatus.CHAT_S);
        } else {
            super.joinMap();
        }
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
            damage = this.nPoint.subDameInjureWithDeff(damage);
            // Đánh vào Mabư Mập tối đa 5% máu mỗi đòn (chiêu đặc biệt gấp 3 = 15%)
            boolean isTuSat = plAtt != null && plAtt.playerSkill != null
                    && plAtt.playerSkill.skillSelect != null
                    && plAtt.playerSkill.skillSelect.template.id == Skill.TU_SAT;
            boolean isSpecialSkill = SkillUtil.isUseSkillSpecial(plAtt)
                    || (this.effectSkill != null && this.effectSkill.isBinh);
            if (!isTuSat && this.nPoint.hpMax > 0) {
                long maxDamage = Math.max(1L, (long) this.nPoint.hpMax * (isSpecialSkill ? 15L : 5L) / 100L);
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
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        if (plKill == null || this.zone == null) {
            return;
        }
        Player plOwner = plKill.isPet ? ((Pet) plKill).master : plKill;
        if (plOwner == null) {
            plOwner = plKill;
        }
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

        // Khi tiêu diệt Mabư Mập sẽ rớt ra Trứng Mabư (ID 568)
        ItemMap eggMap = new ItemMap(this.zone, 568, 1, x, y, plOwner.id);
        Service.gI().dropItemMap(this.zone, eggMap);
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        BossManager.gI().removeBoss(this);
        this.dispose();
    }
}

