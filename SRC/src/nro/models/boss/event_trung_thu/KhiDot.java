package nro.models.boss.event_trung_thu;


import nro.models.boss.Boss;
import nro.models.boss.BossesData;
import nro.models.boss.BossID;
import static nro.models.consts.BossType.TRUNGTHU_EVENT;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.Service;
import nro.models.utils.Util;

public class KhiDot extends Boss {

    public KhiDot() throws Exception {
        super(TRUNGTHU_EVENT, BossID.KHIDOT, true, true, BossesData.KHIDOT);
    }

    @Override
    public void reward(Player plKill) {
        int y = this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24);
        ItemMap it = new ItemMap(this.zone, 1045, 1, this.location.x, y, plKill.id);
        Service.gI().dropItemMap(this.zone, it);
        ItemMap box = new ItemMap(this.zone, 736, 1, this.location.x + 15, y, plKill.id);
        Service.gI().dropItemMap(this.zone, box);
        if (plKill != null && plKill.event != null) {
            plKill.event.addEventPoint(2);
            Service.gI().sendThongBao(plKill, "Hạ Khỉ Đột nhận +2 Điểm Sự Kiện!");
        }
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
                damage = damage / 1;
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
    public void joinMap() {
        super.joinMap(); //To change body of generated methods, choose Tools | Templates.
        st = System.currentTimeMillis();
    }

    private long st;

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
