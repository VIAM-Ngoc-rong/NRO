package nro.models.boss.VuTru6;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.ConstPlayer;
import nro.models.map.ItemMap;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.Service;
import nro.models.utils.Util;

public class Hit extends Boss {

    public Hit() throws Exception {
        super(BossID.HIT, BossesData.HIT);
    }

    @Override
    public void reward(Player plKill) {
        super.reward(plKill);
        if (plKill == null || this.zone == null) {
            return;
        }
        Player plOwner = plKill.isPet ? ((Pet) plKill).master : plKill;
        if (plOwner == null) {
            plOwner = plKill;
        }
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

        // Khi tiêu diệt Sát Thủ Hit 100% rớt Quả Trứng Hit (ID 1834) - Ấp Đệ Tử Cấp 4
        ItemMap eggMap = new ItemMap(this.zone, 1834, 1, x, y, plOwner.id);
        Service.gI().dropItemMap(this.zone, eggMap);
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
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Bước Nhảy Thời Gian — Xí hụt!");
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

