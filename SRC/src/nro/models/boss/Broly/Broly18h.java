package nro.models.boss.Broly;

import java.util.List;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.AppearType;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.PetService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

public class Broly18h extends Boss {

    private static final int[] LEVEL_12_EQUIPS = {
        240, 241, 244, 245, 248, 249, 252, 253,
        256, 257, 260, 261, 264, 265, 268, 269,
        272, 273, 276, 277, 280, 281
    };

    public Broly18h() throws Exception {
        super(BossID.BROLY_18H - Util.nextInt(1, 100000), false, true, new BossData(
                "Broly",
                ConstPlayer.XAYDA,
                new short[]{1080, 1081, 1082, -1, -1, -1},
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
                new String[]{"|-1|Grrr! Kakarot đâu rồi?!"},
                new String[]{
                    "|-1|Haha! Ta sẽ giết hết các ngươi!",
                    "|-1|Sức mạnh của ta đang tăng lên từng giây!",
                    "|-1|Vào hết đây!!!"
                },
                new String[]{"|-1|Grrr! Cơn giận của ta đã đạt đến cực hạn!"},
                300
        ));
    }

    private boolean isSpawnTime18h() {
        return true;
    }

    @Override
    public void rest() {
        if (!isSpawnTime18h()) {
            return;
        }
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
    public void active() {
        super.active();
    }

    @Override
    public void initBase() {
        super.initBase();
        // Máu dạng thường: 1.000.000 - 20.000.000 HP
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
            // Đánh theo % máu giống Super Broly nhưng giảm dần từ 0.5% xuống 0.1% (chiêu đặc biệt gấp 3 lần %)
            boolean isTuSat = plAtt != null && plAtt.playerSkill != null
                    && plAtt.playerSkill.skillSelect != null
                    && plAtt.playerSkill.skillSelect.template.id == Skill.TU_SAT;
            boolean isSpecialSkill = SkillUtil.isUseSkillSpecial(plAtt)
                    || (this.effectSkill != null && this.effectSkill.isBinh);
            if (!isTuSat && this.nPoint.hpMax > 0) {
                double hpRatio = Math.max(0.0, Math.min(1.0, (double) this.nPoint.hp / (double) this.nPoint.hpMax));
                double maxPercent = (0.001 + (0.004 * hpRatio)) * (isSpecialSkill ? 3.0 : 1.0);
                long maxDamage = Math.max(1L, (long) (this.nPoint.hpMax * maxPercent));
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
        // Hạ Boss Broly thường -> nhận Đệ tử thường (nếu chưa có đệ tử)
        if (plOwner.pet == null) {
            PetService.gI().createNormalPet(plOwner);
        }
    }

    @Override
    public void die(Player plKill) {
        Zone currentZone = this.zone;
        super.die(plKill);
        // Sau khi bị tiêu diệt, xuất hiện Siêu Broly ở 1 khu bất kỳ trong cùng Map
        if (currentZone != null && currentZone.map != null && !currentZone.map.zones.isEmpty()) {
            try {
                int minIdx = currentZone.map.zones.size() > 1 ? 1 : 0;
                int randIdx = Util.nextInt(minIdx, currentZone.map.zones.size() - 1);
                Zone randomZoneInMap = currentZone.map.zones.get(randIdx);
                new SieuBroly18h(randomZoneInMap);
            } catch (Exception e) {
            }
        }
    }
}

