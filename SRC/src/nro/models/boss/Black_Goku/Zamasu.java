package nro.models.boss.Black_Goku;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.ConstPlayer;
import nro.models.consts.ConstTaskBadges;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.EffectSkillService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.services.TaskService;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Util;

public class Zamasu extends Boss {

    private long st;
    private int timeLeaveMap;

    public Zamasu() throws Exception {
        super(BossID.ZAMASU, false, true, BossesData.ZAMASU, BossesData.ZAMASU_HOP_THE);
    }

    @Override
    public void initBase() {
        super.initBase();
        if (this.currentLevel == 0) {
            try {
                for (Template.ItemTemplate it : Manager.ITEM_TEMPLATES) {
                    if (it != null && it.id == 1553 && it.head > 0 && it.body > 0 && it.leg > 0) {
                        this.data[0].getOutfit()[0] = (short) it.head;
                        this.data[0].getOutfit()[1] = (short) it.body;
                        this.data[0].getOutfit()[2] = (short) it.leg;
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void reward(Player plKill) {
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.TRUM_SAN_BOSS, 1);
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        if (Util.isTrue(15, 100)) {
            ItemMap optionalItemMap = new ItemMap(this.zone, 992, 1, x, y, plKill.id);
            Service.gI().dropItemMap(zone, optionalItemMap);
        }
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        int diem = 5;
        plKill.event.addEventPoint(diem);
        Service.gI().sendThongBao(plKill, "+5 Point");
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
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
        } else {
            return 0;
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, timeLeaveMap)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
            timeLeaveMap = 900000;
        }
    }

    @Override
    public void joinMap() {
        this.name = this.data[this.currentLevel].getName();
        super.joinMap();
        st = System.currentTimeMillis();
        timeLeaveMap = 900000;
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
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                int dis = Util.getDistance(this, pl);
                if (dis > 450) {
                    move(pl.location.x - 24, pl.location.y);
                } else if (dis > 100) {
                    int dir = (this.location.x - pl.location.x < 0 ? 1 : -1);
                    int move = Util.nextInt(50, 100);
                    move(this.location.x + (dir == 1 ? move : -move), pl.location.y);
                } else {
                    if (Util.isTrue(30, 100)) {
                        int move = Util.nextInt(50);
                        move(pl.location.x + (Util.nextInt(0, 1) == 1 ? move : -move), this.location.y);
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

