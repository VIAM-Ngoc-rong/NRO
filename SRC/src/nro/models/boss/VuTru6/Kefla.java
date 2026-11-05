package nro.models.boss.VuTru6;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.ConstPlayer;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.utils.Util;

public class Kefla extends Boss {

    public Kefla() throws Exception {
        super(BossID.KEFLA, BossesData.KEFLA_1, BossesData.KEFLA_2);
    }

    @Override
    public void reward(Player plKill) {
        super.reward(plKill);
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (this.isPendingIdleDespawn()) {
            return 0;
        }
        if (plAtt != null && !plAtt.equals(this)) {
            this.recordBossAttacked();
        }
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
                damage = damage / 4;
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

