package nro.models.boss.Broly;

import java.util.List;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.PetService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.services.TaskService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

public class SieuBroly18h extends Boss {

    private static final int[] LEVEL_12_EQUIPS = {
        240, 241, 244, 245, 248, 249, 252, 253,
        256, 257, 260, 261, 264, 265, 268, 269,
        272, 273, 276, 277, 280, 281
    };

    public SieuBroly18h(Zone targetZone) throws Exception {
        super(BossID.SIEU_BROLY_18H - Util.nextInt(1, 100000), false, false, new BossData(
                "SSJ Broly",
                ConstPlayer.XAYDA,
                new short[]{1083, 1084, 1085, -1, -1, -1},
                50_000,
                new int[]{50_000_000},
                new int[]{5, 13, 20, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38},
                new int[][]{
                    {Skill.DRAGON, 7, 1000},
                    {Skill.DEMON, 7, 1000},
                    {Skill.GALICK, 7, 1000},
                    {Skill.KAMEJOKO, 7, 1000},
                    {Skill.MASENKO, 7, 1000},
                    {Skill.ANTOMIC, 7, 1000}
                },
                new String[]{"|-1|Grrr! Sức mạnh Siêu Xayda Huyền Thoại đây!"},
                new String[]{
                    "|-1|Haha! Ta sẽ nghiền nát tất cả!",
                    "|-1|Sức mạnh của ta là vô địch!",
                    "|-1|Vào hết đây!!!"
                },
                new String[]{"|-1|Không thể nào..."},
                1
        ));
        this.zoneFinal = targetZone;
        this.changeStatus(BossStatus.RESPAWN);
    }

    @Override
    public void initBase() {
        super.initBase();
        this.nPoint.hpg = Util.nextInt(50_000_000, 100_000_000);
        this.nPoint.hpMax = this.nPoint.hpg;
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dameg = Math.max(20_000, this.nPoint.hpMax / 1000);
        this.nPoint.dame = this.nPoint.dameg;
        this.nPoint.crit = 25;
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
            // Đánh theo % máu giống Super Broly nhưng giảm dần từ 0.5% xuống 0.1% theo lượng máu hiện tại
            boolean isTuSat = plAtt != null && plAtt.playerSkill != null
                    && plAtt.playerSkill.skillSelect != null
                    && plAtt.playerSkill.skillSelect.template.id == Skill.TU_SAT;
            if (!piercing && !isTuSat && this.nPoint.hpMax > 0) {
                double hpRatio = Math.max(0.0, Math.min(1.0, (double) this.nPoint.hp / (double) this.nPoint.hpMax));
                double maxPercent = 0.001 + (0.004 * hpRatio); // 0.5% ở 100% HP giảm dần về 0.1% khi cạn máu
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
        if (plOwner.pet == null) {
            PetService.gI().createNormalPet(plOwner);
        }
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

        // 10% tỷ lệ rớt Quả Trứng Đệ Tử Broly (ID 1826)
        if (Util.isTrue(10, 100)) {
            ItemMap eggMap = new ItemMap(this.zone, 1826, 1, x, y, plOwner.id);
            Service.gI().dropItemMap(this.zone, eggMap);
        }
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

