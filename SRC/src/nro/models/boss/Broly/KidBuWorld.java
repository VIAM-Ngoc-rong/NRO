package nro.models.boss.Broly;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.TaskService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

public class KidBuWorld extends Boss {

    public KidBuWorld() throws Exception {
        super(BossID.KID_BU_MAP - Util.nextInt(1, 100000), false, true, new BossData(
                "Kid Bư",
                ConstPlayer.XAYDA,
                new short[]{439, 440, 441, -1, -1, -1},
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
    public void initBase() {
        super.initBase();
        // Lượng máu giống với cơ chế của Broly thường: 1.000.000 - 20.000.000 HP
        this.nPoint.hpg = Util.nextInt(1_000_000, 20_000_000);
        this.nPoint.hpMax = this.nPoint.hpg;
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dameg = Math.max(5_000, this.nPoint.hpMax / 1000);
        this.nPoint.dame = this.nPoint.dameg;
        this.nPoint.crit = 20;
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
            damage = this.nPoint.subDameInjureWithDeff(damage);
            // Đánh vào Kid Bư tối đa 5% máu mỗi đòn (chiêu đặc biệt gấp 3 = 15%)
            boolean isTuSat = plAtt != null && plAtt.playerSkill != null
                    && plAtt.playerSkill.skillSelect != null
                    && plAtt.playerSkill.skillSelect.template.id == Skill.TU_SAT;
            boolean isSpecialSkill = SkillUtil.isUseSkillSpecial(plAtt)
                    || (this.effectSkill != null && this.effectSkill.isBinh);
            if (!isTuSat && !isSpecialSkill && this.nPoint.hpMax > 0) {
                long maxDamage = Math.max(1L, (long) this.nPoint.hpMax * 5L / 100L);
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
    }

    @Override
    public void die(Player plKill) {
        Zone currentZone = this.zone;
        super.die(plKill);
        // Khi bị giết, Kid Bư có tỷ lệ (50%) biến thành Mabư Mập ở khu bất kỳ trong map
        if (currentZone != null && currentZone.map != null && !currentZone.map.zones.isEmpty() && Util.isTrue(50, 100)) {
            try {
                int minIdx = currentZone.map.zones.size() > 1 ? 1 : 0;
                int randIdx = Util.nextInt(minIdx, currentZone.map.zones.size() - 1);
                Zone randomZoneInMap = currentZone.map.zones.get(randIdx);
                new MabuMapWorld(randomZoneInMap);
            } catch (Exception e) {
            }
        }
    }
}

