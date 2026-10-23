package nro.models.boss.BadiWolves;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.ConstTaskBadges;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.EffectSkillService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Util;

public class Bergamo extends Boss {

    private long st;

    public Bergamo() throws Exception {
        super(BossID.BERGAMO, false, true, BossesData.SOI_BERGAMO);
    }

    @Override
    public void initBase() {
        super.initBase();
        resolveOutfitFromDb("bergamo");
    }

    private void resolveOutfitFromDb(String keyword) {
        try {
            for (Template.ItemTemplate it : Manager.ITEM_TEMPLATES) {
                if (it != null && it.type == 5 && it.head > 0 && it.body > 0 && it.leg > 0
                        && it.name != null && it.name.toLowerCase().contains(keyword)) {
                    this.data[this.currentLevel].getOutfit()[0] = (short) it.head;
                    this.data[this.currentLevel].getOutfit()[1] = (short) it.body;
                    this.data[this.currentLevel].getOutfit()[2] = (short) it.leg;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
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
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
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
}
