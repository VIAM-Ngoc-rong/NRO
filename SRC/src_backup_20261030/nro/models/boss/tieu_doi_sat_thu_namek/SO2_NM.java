package nro.models.boss.tieu_doi_sat_thu_namek;


import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import java.util.List;
import nro.models.boss.BossesData;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

public class SO2_NM extends Boss {

    private long st;

    public SO2_NM() throws Exception {
        super(BossID.SO_2_NM, false, true, BossesData.SO_2_NM);
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.currentLevel == 1) {
            return;
        }
        super.moveTo(x, y);
    }

   @Override
    public void reward(Player plKill) {
        super.reward(plKill);
        Service.gI().dropItemMap(this.zone, new ItemMap(zone, 77, Util.nextInt(1, 3), this.location.x + Util.nextInt(-30, 30), this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));
        int diem = 1;
        plKill.event.addEventPoint(diem);
        Service.gI().sendThongBao(plKill, "+1 Point");
    }
    @Override
    protected void notifyJoinMap() {
        if (this.currentLevel == 1) {
            return;
        }
        super.notifyJoinMap();
    }

    @Override
    public void doneChatS() {
        this.changeStatus(BossStatus.AFK);
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
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if (boss.id == BossID.SO_1_NM && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

}
