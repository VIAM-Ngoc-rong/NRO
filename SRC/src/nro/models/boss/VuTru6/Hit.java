package nro.models.boss.VuTru6;

import java.util.ArrayList;
import java.util.List;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.ConstPlayer;
import nro.models.map.ItemMap;
import nro.models.services_func.EffectMapService;
import nro.models.network.Message;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemTimeService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public class Hit extends Boss {

    private long lastTimeNgungDong;
    private long lastTimeTatSat;
    private long lastTimeWipeAll;
    private boolean isChargingWipeAll;
    private long lastTimeStartChargeWipe;
    private boolean triggered75Wipe;
    private boolean triggered50Wipe;
    private boolean triggered25Wipe;

    public Hit() throws Exception {
        super(BossID.HIT, BossesData.HIT);
    }

    @Override
    protected void resetBase() {
        super.resetBase();
        long now = System.currentTimeMillis();
        this.lastTimeNgungDong = now;
        this.lastTimeTatSat = now;
        this.lastTimeWipeAll = now;
        this.isChargingWipeAll = false;
        this.lastTimeStartChargeWipe = 0L;
        this.triggered75Wipe = false;
        this.triggered50Wipe = false;
        this.triggered25Wipe = false;
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

    /**
     * 1. Kỹ năng NGƯNG ĐỌNG THỜI GIAN (Time-Skip Freeze):
     * Đóng băng toàn bộ người chơi & đệ tử trong khu vực suốt 4 giây (bỏ qua kháng TDHS).
     */
    private void castNgungDongThoiGian() {
        if (this.zone == null) {
            return;
        }
        List<Player> targets = new ArrayList<>(this.zone.getNotBosses());
        boolean hitAny = false;
        long now = System.currentTimeMillis();
        int stunDurationMs = 4000;
        for (Player pl : targets) {
            if (pl != null && !pl.isDie() && !pl.isBoss) {
                EffectSkillService.gI().startStun(pl, now, stunDurationMs);
                if (pl.isPl()) {
                    ItemTimeService.gI().sendItemTime(pl, 3779, stunDurationMs / 1000);
                    Service.gI().sendThongBao(pl, "⏱️ Sát Thủ Hit thi triển Ngưng Đọng Thời Gian! Bạn bị đóng băng 4 giây!");
                }
                hitAny = true;
            }
        }
        if (hitAny) {
            EffectMapService.gI().sendEffectMapToAllInMap(this, 65, 3, 1, this.location.x, this.location.y, -1);
            this.chat("⏱️ NGƯNG ĐỌNG THỜI GIAN! Không gian và thời gian trong khu vực này đã bị đóng băng!");
        }
    }

    /**
     * 2. Kỹ năng CHIÊU TẤT SÁT (Time-Skip Lethal Heart Strike):
     * Dịch chuyển tức thời áp sát mục tiêu, phá Khiên Năng Lượng và hạ gục tức khắc (100% HP).
     */
    private void castChieuTatSat(Player target) {
        if (target == null || target.isDie() || this.zone == null) {
            return;
        }
        PlayerService.gI().playerMove(this, target.location.x, target.location.y);
        if (target.effectSkill != null && target.effectSkill.isShielding) {
            EffectSkillService.gI().breakShield(target);
        }
        this.chat("💀 CHIÊU TẤT SÁT: ĐÒN XUYÊN TIM VÔ HÌNH! Vĩnh biệt [" + target.name + "]!");
        long lethalDamage = Math.max(2_000_000_000L, target.nPoint.getHpLong() + 50_000_000L);
        int dameHit = target.injured(this, lethalDamage, true, false);
        sendBossSkillHitPacket(target, dameHit);
        if (target.isPl()) {
            Service.gI().sendThongBao(target, "💀 Bạn đã trúng Chiêu Tất Sát Xuyên Tim của Sát Thủ Hit!");
        }
    }

    /**
     * 3. Kỹ năng TIÊU DIỆT TOÀN BỘ NGƯỜI CHƠI (Lĩnh Vực Thời Gian Tuyệt Diệt):
     * Gồng 2.5 giây cảnh báo rồi xóa sổ toàn bộ người chơi & đệ tử trong khu vực.
     */
    private void startChargeTieuDietToanBo() {
        if (this.zone == null || this.isChargingWipeAll) {
            return;
        }
        this.isChargingWipeAll = true;
        this.lastTimeStartChargeWipe = System.currentTimeMillis();
        this.lastTimeWipeAll = System.currentTimeMillis();
        EffectSkillService.gI().sendEffectStartCharge(this);
        EffectSkillService.gI().sendEffectCharge(this);
        this.chat("⏳ LĨNH VỰC THỜI GIAN TUYỆT DIỆT! Sau 2.5 giây ta sẽ TIÊU DIỆT TOÀN BỘ NGƯỜI CHƠI trong khu vực!");
        for (Player pl : new ArrayList<>(this.zone.getNotBosses())) {
            if (pl != null && pl.isPl() && !pl.isDie()) {
                Service.gI().sendThongBao(pl, "⚠️ CẢNH BÁO: Sát Thủ Hit đang tụ khí TIÊU DIỆT TOÀN BỘ NGƯỜI CHƠI sau 2.5 giây!");
            }
        }
    }

    private void executeTieuDietToanBoNguoiChoi() {
        this.isChargingWipeAll = false;
        this.lastTimeWipeAll = System.currentTimeMillis();
        if (this.zone == null) {
            return;
        }
        EffectSkillService.gI().sendEffectStopCharge(this);
        EffectMapService.gI().sendEffectMapToAllInMap(this, 65, 3, 1, this.location.x, this.location.y, -1);
        this.chat("🌌 THỜI GIAN ĐỨT ĐOẠN — TIÊU DIỆT TOÀN BỘ NGƯỜI CHƠI!!!");

        List<Player> allTargets = new ArrayList<>(this.zone.getHumanoids());
        for (Player pl : allTargets) {
            if (pl == null || pl.isBoss || pl.isDie()) {
                continue;
            }
            if (pl.effectSkill != null && pl.effectSkill.isShielding) {
                EffectSkillService.gI().breakShield(pl);
            }
            long wipeDamage = Math.max(2_000_000_000L, pl.nPoint.getHpLong() + 100_000_000L);
            int dameHit = pl.injured(this, wipeDamage, true, false);
            if (!pl.isDie()) {
                pl.nPoint.setHp(0);
                pl.setDie();
            }
            sendBossSkillHitPacket(pl, dameHit);
            if (pl.isPl()) {
                PlayerService.gI().sendInfoHpMpMoney(pl);
                Service.gI().sendThongBao(pl, "🌌 Sát Thủ Hit đã kích hoạt Lĩnh Vực Thời Gian Tuyệt Diệt — Tiêu diệt toàn bộ người chơi!");
            }
            Service.gI().Send_Info_NV(pl);
            Service.gI().reload_HP_NV(pl);
        }
    }

    private void sendBossSkillHitPacket(Player plInjure, int dameHit) {
        Message msg = null;
        try {
            byte skillId = (this.playerSkill != null && this.playerSkill.skillSelect != null)
                    ? (byte) this.playerSkill.skillSelect.skillId
                    : (byte) Skill.DRAGON;
            msg = new Message(-60);
            msg.writer().writeInt((int) this.id);
            msg.writer().writeByte(skillId);
            msg.writer().writeByte(1);
            msg.writer().writeInt((int) plInjure.id);
            msg.writer().writeByte(1);
            msg.writer().writeByte(0);
            msg.writer().writeInt(dameHit > 0 ? dameHit : 2_000_000_000);
            msg.writer().writeBoolean(plInjure.isDie());
            msg.writer().writeBoolean(true);
            Service.gI().sendMessAllPlayerInMap(this, msg);
            Service.gI().reload_HP_NV(plInjure);
        } catch (Exception ignored) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        if (this.isDie() || this.zone == null) {
            return;
        }
        // Nếu đang tụ khí Tiêu Diệt Toàn Bộ Người Chơi
        if (this.isChargingWipeAll) {
            EffectSkillService.gI().sendEffectCharge(this);
            if (Util.canDoWithTime(this.lastTimeStartChargeWipe, 2500)) {
                executeTieuDietToanBoNguoiChoi();
            }
            return;
        }

        // Kiểm tra ngưỡng máu 75%, 50%, 25% hoặc chu kỳ 25 giây để tung chiêu Tiêu Diệt Toàn Bộ Người Chơi
        int pctHp = this.nPoint.getCurrPercentHP();
        boolean thresholdWipe = false;
        if (!this.triggered75Wipe && pctHp <= 75) {
            this.triggered75Wipe = true;
            thresholdWipe = true;
        } else if (!this.triggered50Wipe && pctHp <= 50) {
            this.triggered50Wipe = true;
            thresholdWipe = true;
        } else if (!this.triggered25Wipe && pctHp <= 25) {
            this.triggered25Wipe = true;
            thresholdWipe = true;
        }
        if ((thresholdWipe || Util.canDoWithTime(this.lastTimeWipeAll, 25000)) && this.hasRealPlayerInZone()) {
            startChargeTieuDietToanBo();
            return;
        }

        // Chu kỳ 14 giây: Ngưng Đọng Thời Gian toàn khu vực
        if (Util.canDoWithTime(this.lastTimeNgungDong, 14000) && this.hasRealPlayerInZone()) {
            this.lastTimeNgungDong = System.currentTimeMillis();
            castNgungDongThoiGian();
            return;
        }

        // Chu kỳ 11 giây: Chiêu Tất Sát Xuyên Tim 1 mục tiêu
        if (Util.canDoWithTime(this.lastTimeTatSat, 11000)) {
            Player plTarget = getPlayerAttack();
            if (plTarget != null && !plTarget.isDie()) {
                this.lastTimeTatSat = System.currentTimeMillis();
                castChieuTatSat(plTarget);
                return;
            }
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
