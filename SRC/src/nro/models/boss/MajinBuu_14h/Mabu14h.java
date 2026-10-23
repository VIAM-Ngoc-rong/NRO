package nro.models.boss.MajinBuu_14h;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.AppearType;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstTaskBadges;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemTimeService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.task.BadgesTaskService;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

/**
 * Boss Mabư 2h Chiều (14h): Mabư 2h -> Super Bư -> Bư Tênk -> Bư Han -> Kid Bư.
 * Tự động xuất hiện trong khung giờ 14h-16h chiều (hoặc bất cứ lúc nào khi Admin gọi từ Web Dashboard).
 */
public class Mabu14h extends Boss {

    private long st;
    private long lastTimeSocola;

    public Mabu14h() throws Exception {
        super(BossID.MABU_14H, false, true,
                BossesData.MABU_14H_1,
                BossesData.MABU_14H_2,
                BossesData.MABU_14H_3,
                BossesData.MABU_14H_4,
                BossesData.MABU_14H_5);
    }

    private boolean isSpawnTime14h() {
        int hour = TimeUtil.getCurrHour();
        return hour >= 14 && hour <= 15;
    }

    @Override
    public void rest() {
        if (!isSpawnTime14h()) {
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
    public void joinMap() {
        this.name = this.data[this.currentLevel].getName();
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void attack() {
        if (this.zone != null && Util.canDoWithTime(lastTimeSocola, 15000)) {
            lastTimeSocola = System.currentTimeMillis();
            for (Player pl : this.zone.getNotBosses()) {
                if (pl != null && !pl.isDie() && Util.isTrue(1, 4)) {
                    this.chat("Úm ba la xì bùa thành Socola!");
                    EffectSkillService.gI().setSocola(pl, System.currentTimeMillis(), 15000);
                    Service.gI().Send_Caitrang(pl);
                    ItemTimeService.gI().sendItemTime(pl, 4133, 15);
                }
            }
        }
        super.attack();
    }

    @Override
    public void reward(Player plKill) {
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.TRUM_SAN_BOSS, 1);
        int diem = 5;
        plKill.event.addEventPoint(diem);
        Service.gI().sendThongBao(plKill, "+5 Point");
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
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
            if (this.bossDamageReductionFactor > 1) {
                damage = Math.max(1L, damage / this.bossDamageReductionFactor);
            }
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
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
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }
}
