package nro.models.player;

import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.mob.Mob;
import nro.models.services.ItemService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.InventoryService;
import nro.models.map.service.MapService;
import nro.models.utils.Util;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemTimeService;
import nro.models.skill.Skill;

/**
 *
 * @author By Mr Blue
 *
 */
public class EffectSkin {

    public static final String[] textOdo = new String[]{
        "Hôi quá, tránh xa ta ra", "Biến đi", "Trời ơi đồ ở dơ",
        "Thúi quá", "Mùi gì hôi quá"
    };

    public static final String[] textXinbato = new String[]{
        "Im đi, ông xinbato", "Thôi ông câm mẹ mồm đi", "Phân tâm quá",
        "Phân tâm quá", " Phân tâm quá"
    };

    private static final String[] textThoBulma = new String[]{
        "Wow, sexy quá"
    };

    private static final String[] textBuffSD = new String[]{
        "Wow! Sexy quá",};

    @Setter
    private Player player;
    private int tlNeDon;

    public EffectSkin(Player player) {
        this.player = player;
        this.xHPKI = 1;
        this.xDame = 1;
    }

    public long lastTimeAttack;
    public long lastTimeVoHinh;
    private long lastTimeOdo;
    private long lastTimeXinbato;
    private long lastTimeltdb;
    private long lastTimeThoBulma;
    private long lastTimeMaPhongBa;
    private long lastTimeXenHutHpKi;

    public long lastTimeAddTimeTrainArmor;
    public long lastTimeSubTimeTrainArmor;

    public boolean isVoHinh;

    public long lastTimeXHPKI;
    public int xHPKI;

    public long lastTimeXDame;
    public int xDame;

    public long lastTimeUpdateCTHT;

    private long lastTimeTanHinh;
    private long lastTimeLamCham;
    private long lastTimeHoaDa;
    private long lastTimeHalloween;
    public long lastTimeXChuong;
    public boolean isXChuong;
    public boolean isXDame;
    private long lastTimeTokuda;
    private long lastTimeSocolaXQ;
    private long lastTimeMpHoiCute;
    private long lastTimeLanhXQ;
    private long lastTimeDoremonBuff;

    private boolean isSameTeam(Player p1, Player p2) {
        if (p1 == null || p2 == null) {
            return true;
        }
        if (p1.equals(p2) || p1.id == p2.id) {
            return true;
        }
        if (p1.isPet && ((Pet) p1).master != null && ((Pet) p1).master.equals(p2)) {
            return true;
        }
        if (p2.isPet && ((Pet) p2).master != null && ((Pet) p2).master.equals(p1)) {
            return true;
        }
        if (p1.isNewPet && ((NewPet) p1).master != null && ((NewPet) p1).master.equals(p2)) {
            return true;
        }
        if (p2.isNewPet && ((NewPet) p2).master != null && ((NewPet) p2).master.equals(p1)) {
            return true;
        }
        return false;
    }

    private boolean isBrolyCharging(Player pl) {
        return pl instanceof nro.models.boss.Boss b && b.isBrolyBoss() && b.isUsingTTNL;
    }

    public void update() {
        updateVoHinh();
        if (!this.player.isDie() && this.player.zone != null && !MapService.gI().isMapOffline(this.player.zone.map.mapId)) {
            updateOdo();
            updateXinbato();
            updateThoBulma();
            updateMaPhongBa();
            updateXenHutXungQuanh();
            updateTanHinh();
            updateHoaDa();
            updateLamCham();
            updateXChuong();
            updateSocolaXQ();
            updateMpHoiCute();
            updateDoremonBuff();
            updateLanhXungQuanh();
//            updateHalloween();
        }
        if (!this.player.isBoss && !this.player.isPet && !player.isNewPet) {
            updateTrainArmor();
        }
        if (xHPKI != 1 && Util.canDoWithTime(lastTimeXHPKI, 1800000)) {
            xHPKI = 1;
            Service.gI().point(player);
        }
        if (xDame != 1 && Util.canDoWithTime(lastTimeXDame, 1800000)) {
            xDame = 1;
            Service.gI().point(player);
        }
        updateCTHaiTac();
    }

    private void updateCTHaiTac() {
        if (this.player.setClothes != null && this.player.setClothes.ctHaiTac != -1
                && this.player.zone != null
                && Util.canDoWithTime(lastTimeUpdateCTHT, 5000)) {
            int count = 0;
            int[] cts = new int[9];
            cts[this.player.setClothes.ctHaiTac - 618] = this.player.setClothes.ctHaiTac;
            List<Player> players = new ArrayList<>();
            players.add(player);
            try {
                for (Player pl : player.zone.getNotBosses()) {
                    if (!player.equals(pl) && pl.setClothes.ctHaiTac != -1 && Util.getDistance(player, pl) <= 300) {
                        cts[pl.setClothes.ctHaiTac - 618] = pl.setClothes.ctHaiTac;
                        players.add(pl);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < cts.length; i++) {
                if (cts[i] != 0) {
                    count++;
                }
            }
            for (Player pl : players) {
                Item ct = pl.inventory.itemsBody.get(5);
                if (ct.isNotNullItem() && ct.template.id >= 618 && ct.template.id <= 626) {
                    for (Item.ItemOption io : ct.itemOptions) {
                        if (io.optionTemplate.id == 147
                                || io.optionTemplate.id == 77
                                || io.optionTemplate.id == 103) {
                            io.param = count * 3;
                        }
                    }
                }
                if (!pl.isPet && !pl.isNewPet && Util.canDoWithTime(lastTimeUpdateCTHT, 5000)) {
                    InventoryService.gI().sendItemBody(pl);
                }
                pl.effectSkin.lastTimeUpdateCTHT = System.currentTimeMillis();
            }
        }
    }

    private void updateXenHutXungQuanh() {
        try {
            if (this.player.nPoint != null) {
                int param = this.player.nPoint.tlHutHpMpXQ;
                if (param > 0) {
                    if (!this.player.isDie() && Util.canDoWithTime(lastTimeXenHutHpKi, 5000)) {
                        long hpHut = 0;
                        long mpHut = 0;
                        List<Player> players = new ArrayList<>();
                        List<Player> playersMap = this.player.zone.getHumanoids();
                        for (int i = playersMap.size() - 1; i >= 0; i--) {
                            Player pl = playersMap.get(i);
                            if (pl != null && pl.nPoint != null && !isSameTeam(this.player, pl) && !pl.isNewPet && !pl.isDie()
                                    && Util.getDistance(this.player, pl) <= 250) {
                                players.add(pl);
                            }
                        }
                        for (Mob mob : this.player.zone.mobs) {
                            if (mob != null && !mob.isDie() && mob.point.gethp() > 1) {
                                if (Util.getDistance(this.player, mob) <= 250) {
                                    int subHp = (int) ((long) mob.point.getHpFull() * param / 100L);
                                    if (subHp >= mob.point.gethp()) {
                                        subHp = mob.point.gethp() - 1;
                                    }
                                    if (subHp > 0) {
                                        hpHut += subHp;
                                        mob.injured(this.player, subHp, false);
                                    }
                                }
                            }
                        }
                        for (Player pl : players) {
                            int subHp = (int) ((long) pl.nPoint.hpMax * param / 100L);
                            int subMp = (int) ((long) pl.nPoint.mpMax * param / 100L);
                            if (pl.isBoss) {
                                subHp = Math.min(subHp, Math.min(500_000, Math.max(1, pl.nPoint.hpMax / 100)));
                                subMp = Math.min(subMp, Math.min(500_000, Math.max(1, pl.nPoint.mpMax / 100)));
                            }
                            if (subHp >= pl.nPoint.hp) {
                                subHp = pl.nPoint.hp - 1;
                            }
                            if (subMp >= pl.nPoint.mp) {
                                subMp = pl.nPoint.mp - 1;
                            }
                            if (subHp > 0) {
                                hpHut += subHp;
                                pl.injured(null, subHp, true, false);
                            }
                            if (subMp > 0) {
                                mpHut += subMp;
                                pl.nPoint.subMP(subMp);
                            }
                            PlayerService.gI().sendInfoHpMpMoney(pl);
                            Service.gI().Send_Info_NV(pl);
                        }
                        if (hpHut > 0 || mpHut > 0) {
                            PlayerService.gI().hoiPhuc(this.player,
                                    (int) Math.min(Integer.MAX_VALUE, hpHut),
                                    (int) Math.min(Integer.MAX_VALUE, mpHut));
                        }
                        this.lastTimeXenHutHpKi = System.currentTimeMillis();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateOdo() {
        try {
            if (this.player.nPoint != null) {
                int param = this.player.nPoint.tlHpGiamODo;
                if (param > 0) {
                    if (Util.canDoWithTime(lastTimeOdo, 8000)) {
                        for (Mob mob : this.player.zone.mobs) {
                            if (mob != null && !mob.isDie() && mob.point.gethp() > 1
                                    && Util.getDistance(this.player, mob) <= 250) {
                                int subHp = (int) ((long) mob.point.getHpFull() * param / 100L);
                                if (subHp >= mob.point.gethp()) {
                                    subHp = mob.point.gethp() - 1;
                                }
                                if (subHp > 0) {
                                    mob.injured(this.player, subHp, false);
                                }
                            }
                        }
                        List<Player> playersMap = this.player.zone.getHumanoids();
                        for (int i = playersMap.size() - 1; i >= 0; i--) {
                            Player pl = playersMap.get(i);
                            if (pl != null && pl.nPoint != null && !isSameTeam(this.player, pl) && !pl.isNewPet && !pl.isDie()
                                    && Util.getDistance(this.player, pl) <= 250) {
                                int subHp = (int) ((long) pl.nPoint.hpMax * param / 100L);
                                if (pl.isBoss) {
                                    subHp = Math.min(subHp, Math.min(500_000, Math.max(1, pl.nPoint.hpMax / 100)));
                                }
                                if (subHp >= pl.nPoint.hp) {
                                    subHp = pl.nPoint.hp - 1;
                                }
                                if (subHp > 0) {
                                    pl.injured(null, subHp, true, false);
                                }
                                Service.gI().chat(pl, textOdo[Util.nextInt(0, textOdo.length - 1)]);
                                PlayerService.gI().sendInfoHpMpMoney(pl);
                                Service.gI().Send_Info_NV(pl);
                            }

                        }
                        this.lastTimeOdo = System.currentTimeMillis();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateXinbato() {
        try {
            if (this.player.nPoint == null || !this.player.nPoint.isXinbato) {
                return;
            }

            if (!Util.canDoWithTime(lastTimeXinbato, 10_000)) {
                return;
            }

            int addNeDon = 25;
            this.player.nPoint.tlNeDonBuffXinbato = addNeDon;
            this.player.nPoint.timeXinbatoBuff = System.currentTimeMillis();

            for (Mob mob : this.player.zone.mobs) {
                if (mob != null && !mob.isDie() && Util.getDistance(this.player, mob) <= 250) {
                    mob.timeAttack = 3500;
                }
            }

            List<Player> playersMap = this.player.zone.getHumanoids();
            for (int i = playersMap.size() - 1; i >= 0; i--) {
                Player pl = playersMap.get(i);
                if (pl != null && pl.nPoint != null && !this.player.equals(pl) && !pl.isNewPet && !pl.isDie()
                        && Util.getDistance(this.player, pl) <= 250) {
                    if (isSameTeam(this.player, pl) || !pl.isBoss) {
                        int baseNeDon = pl.nPoint.tlNeDon;
                        int buffNeDon = Math.min(addNeDon, 90 - baseNeDon);
                        if (buffNeDon > 0) {
                            pl.nPoint.tlNeDonBuffXinbato = buffNeDon;
                            pl.nPoint.timeXinbatoBuff = System.currentTimeMillis();
                        }
                    }
                    Service.gI().chat(pl, textXinbato[Util.nextInt(0, textXinbato.length - 1)]);
                    PlayerService.gI().sendInfoHpMpMoney(pl);
                    Service.gI().Send_Info_NV(pl);
                }
            }

            this.lastTimeXinbato = System.currentTimeMillis();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateThoBulma() {
        try {
            if (this.player.nPoint != null && this.player.nPoint.tlSexyDame > 0) {
                if (Util.canDoWithTime(lastTimeThoBulma, 10000)) {
                    List<Player> playersMap = this.player.zone.getNotBosses();
                    for (int i = playersMap.size() - 1; i >= 0; i--) {
                        Player pl = playersMap.get(i);
                        if (pl != null && pl.nPoint != null && !pl.isBoss && !pl.isDie()
                                && Util.getDistance(this.player, pl) <= 250) {
                            if (!this.player.equals(pl)) {
                                if (player.nPoint.isThoBulma) {
                                    Service.gI().chat(pl, textThoBulma[Util.nextInt(0, textThoBulma.length - 1)]);
                                } else {
                                    Service.gI().chat(pl, textBuffSD[Util.nextInt(0, textBuffSD.length - 1)]);
                                }
                            }
                            EffectSkillService.gI().setDameBuff(pl, 12000, player.nPoint.tlSexyDame);
                        }
                    }
                    this.lastTimeThoBulma = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSocolaXQ() {
        try {
            if (this.player.nPoint != null && this.player.nPoint.isSocolaXQ) {
                if (Util.canDoWithTime(lastTimeSocolaXQ, 15000)) {
                    for (Mob mob : this.player.zone.mobs) {
                        if (mob != null && !mob.isDie() && Util.getDistance(this.player, mob) <= 250) {
                            EffectSkillService.gI().sendMobToSocola(this.player, mob, 8000);
                        }
                    }
                    List<Player> playersMap = this.player.zone.getHumanoids();
                    for (int i = playersMap.size() - 1; i >= 0; i--) {
                        Player pl = playersMap.get(i);
                        if (pl != null && pl.nPoint != null && !isSameTeam(this.player, pl) && !pl.isNewPet && !pl.isDie()
                                && !isBrolyCharging(pl)
                                && Util.getDistance(this.player, pl) <= 250) {
                            int duration = pl.isBoss ? 5000 : 10000;
                            EffectSkillService.gI().setSocola(pl, System.currentTimeMillis(), duration);
                            Service.gI().Send_Caitrang(pl);
                            ItemTimeService.gI().sendItemTime(pl, 4133, duration / 1000);
                            Service.gI().chat(pl, "Biến thành Sôcôla rồi!");
                        }
                    }
                    this.lastTimeSocolaXQ = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMpHoiCute() {
        try {
            if (this.player.nPoint != null && this.player.nPoint.mpHoiCute > 0) {
                if (Util.canDoWithTime(lastTimeMpHoiCute, 3000)) {
                    int percent = this.player.nPoint.mpHoiCute;
                    List<Player> playersMap = this.player.zone.getNotBosses();
                    for (int i = playersMap.size() - 1; i >= 0; i--) {
                        Player pl = playersMap.get(i);
                        if (pl != null && pl.nPoint != null && !pl.isBoss && !pl.isDie()
                                && Util.getDistance(this.player, pl) <= 250) {
                            if (pl.nPoint.mp < pl.nPoint.mpMax) {
                                long mpAdd = Math.max(1L, (long) pl.nPoint.mpMax * percent / 100L);
                                PlayerService.gI().hoiPhuc(pl, 0, mpAdd);
                            }
                        }
                    }
                    this.lastTimeMpHoiCute = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDoremonBuff() {
        try {
            if (this.player.nPoint != null
                    && (this.player.nPoint.tlHpHoiBanThanVaDongDoi > 0 || this.player.nPoint.tlMpHoiBanThanVaDongDoi > 0)) {
                if (Util.canDoWithTime(lastTimeDoremonBuff, 8000)) {
                    int hpPct = this.player.nPoint.tlHpHoiBanThanVaDongDoi;
                    int mpPct = this.player.nPoint.tlMpHoiBanThanVaDongDoi;
                    List<Player> playersMap = this.player.zone.getNotBosses();
                    for (int i = playersMap.size() - 1; i >= 0; i--) {
                        Player pl = playersMap.get(i);
                        if (pl != null && pl.nPoint != null && !pl.isBoss && !pl.isDie()
                                && Util.getDistance(this.player, pl) <= 250) {
                            long hpAdd = (hpPct > 0 && pl.nPoint.hp < pl.nPoint.hpMax)
                                    ? Math.max(1L, (long) pl.nPoint.hpMax * hpPct / 100L) : 0L;
                            long mpAdd = (mpPct > 0 && pl.nPoint.mp < pl.nPoint.mpMax)
                                    ? Math.max(1L, (long) pl.nPoint.mpMax * mpPct / 100L) : 0L;
                            if (hpAdd > 0 || mpAdd > 0) {
                                PlayerService.gI().hoiPhuc(pl, hpAdd, mpAdd);
                            }
                        }
                    }
                    this.lastTimeDoremonBuff = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLanhXungQuanh() {
        try {
            if (this.player.nPoint != null && this.player.nPoint.tlLanhXQ > 0) {
                if (Util.canDoWithTime(lastTimeLanhXQ, 10000)) {
                    int param = this.player.nPoint.tlLanhXQ;
                    for (Mob mob : this.player.zone.mobs) {
                        if (mob != null && !mob.isDie() && mob.point.gethp() > 1
                                && Util.getDistance(this.player, mob) <= 250) {
                            int subHp = (int) ((long) mob.point.getHpFull() * param / 100L);
                            if (subHp >= mob.point.gethp()) {
                                subHp = mob.point.gethp() - 1;
                            }
                            if (subHp > 0) {
                                mob.injured(this.player, subHp, false);
                            }
                            mob.timeAttack = 3500;
                        }
                    }
                    List<Player> playersMap = this.player.zone.getHumanoids();
                    for (int i = playersMap.size() - 1; i >= 0; i--) {
                        Player pl = playersMap.get(i);
                        if (pl != null && pl.nPoint != null && !isSameTeam(this.player, pl) && !pl.isNewPet && !pl.isDie()
                                && !pl.nPoint.isKhongLanh
                                && Util.getDistance(this.player, pl) <= 250) {
                            int subHp = (int) ((long) pl.nPoint.hpMax * param / 100L);
                            if (pl.isBoss) {
                                subHp = Math.min(subHp, Math.min(500_000, Math.max(1, pl.nPoint.hpMax / 100)));
                            }
                            if (subHp >= pl.nPoint.hp) {
                                subHp = pl.nPoint.hp - 1;
                            }
                            if (subHp > 0) {
                                pl.injured(null, subHp, true, false);
                            }
                            Service.gI().chat(pl, "Lạnh quá!");
                            EffectSkillService.gI().setIsLamCham(pl, 5000);
                            PlayerService.gI().sendInfoHpMpMoney(pl);
                            Service.gI().Send_Info_NV(pl);
                        }
                    }
                    this.lastTimeLanhXQ = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTanHinh() {
        try {
            if (this.player.nPoint != null && this.player.nPoint.isTanHinh) {
                if (Util.canDoWithTime(lastTimeTanHinh, 5000)) {
                    EffectSkillService.gI().setIsTanHinh(player, 2000);
                    this.lastTimeTanHinh = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateHoaDa() {
        try {
            if (this.player.nPoint != null && this.player.nPoint.isHoaDa) {
                if (Util.canDoWithTime(lastTimeHoaDa, 15000)) {
                    for (Mob mob : this.player.zone.mobs) {
                        if (mob != null && !mob.isDie() && Util.getDistance(this.player, mob) <= 250) {
                            mob.effectSkill.startStun(System.currentTimeMillis(), 5000);
                            EffectSkillService.gI().sendEffectMob(this.player, mob, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.STONE_EFFECT);
                            EffectSkillService.gI().sendEffectMob(this.player, mob, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.BLIND_EFFECT);
                        }
                    }
                    List<Player> playersMap = this.player.zone.getHumanoids();
                    for (int i = playersMap.size() - 1; i >= 0; i--) {
                        Player pl = playersMap.get(i);
                        if (pl != null && pl.nPoint != null && !isSameTeam(this.player, pl) && !pl.isNewPet && !pl.isDie()
                                && !isBrolyCharging(pl)
                                && Util.getDistance(this.player, pl) <= 250) {
                            EffectSkillService.gI().setIsStone(pl, pl.isBoss ? 4000 : 6000);
                            Service.gI().chat(pl, "Bị hóa đá rồi!");
                        }

                    }
                    this.lastTimeHoaDa = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLamCham() {
        try {
            if (this.player.nPoint != null && this.player.nPoint.isLamCham) {
                if (Util.canDoWithTime(lastTimeLamCham, 10000)) {
                    for (Mob mob : this.player.zone.mobs) {
                        if (mob != null && !mob.isDie() && Util.getDistance(this.player, mob) <= 250) {
                            mob.timeAttack = 4000;
                        }
                    }
                    List<Player> playersMap = this.player.zone.getHumanoids();
                    for (int i = playersMap.size() - 1; i >= 0; i--) {
                        Player pl = playersMap.get(i);
                        if (pl != null && pl.nPoint != null && !isSameTeam(this.player, pl) && !pl.isNewPet && !pl.isDie()
                                && Util.getDistance(this.player, pl) <= 250) {
                            Service.gI().chat(pl, "Nặng quá!");
                            EffectSkillService.gI().setIsLamCham(pl, 5000);
                        }

                    }
                    this.lastTimeLamCham = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateXChuong() {
        try {
            if (this.player.nPoint != null && this.player.nPoint.xChuong != 0) {
                if (Util.canDoWithTime(lastTimeXChuong, 60000)) {
                    this.isXChuong = true;
                    this.lastTimeXChuong = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMaPhongBa() {
        try {
            if (this.player.effectSkill != null && this.player.effectSkill.isBinh && this.player.effectSkill.playerUseMafuba != null) {
                if (Util.canDoWithTime(lastTimeMaPhongBa, 500) && this.player.effectSkill.playerUseMafuba.playerSkill != null) {
                    double param = this.player.effectSkill.playerUseMafuba.playerSkill.getSkillbyId(Skill.MA_PHONG_BA).point * (this.player.effectSkill.typeBinh == 0 ? 1 : 2);
                    int subHp = (int) ((long) this.player.effectSkill.playerUseMafuba.nPoint.hpMax * param / 100);
                    if (subHp >= this.player.nPoint.hp) {
                        subHp = Math.abs(this.player.nPoint.hp - 100);
                    }
                    PlayerService.gI().sendInfoHpMpMoney(this.player);
                    Service.gI().Send_Info_NV(this.player);
                    this.player.injured(this.player.effectSkill.playerUseMafuba, subHp, true, false);
                    this.lastTimeMaPhongBa = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private void updateHalloween() {
    //     try {
    //         if (this.player.effectSkill != null && this.player.effectSkill.isHalloween) {
    //             if (Util.canDoWithTime(lastTimeHalloween, 10000)) {
    //                 List<Player> playersMap = this.player.zone.getNotBosses();
    //                 for (int i = playersMap.size() - 1; i >= 0; i--) {
    //                     Player pl = playersMap.get(i);
    //                     if (pl != null && pl.nPoint != null && !this.player.equals(pl) && pl.effectSkill != null && !pl.effectSkill.isHalloween && !pl.isPet && !pl.isDie()
    //                             && Util.getDistance(this.player, pl) <= 200) {
    //                         EffectSkillService.gI().setIsHalloween(pl, -1, 1800000);
    //                     }
    //                 }
    //                 this.lastTimeHalloween = System.currentTimeMillis();
    //             }
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
    //giáp tập luyện
    private void updateTrainArmor() {
        if (Util.canDoWithTime(lastTimeAddTimeTrainArmor, 60000) && !Util.canDoWithTime(lastTimeAttack, 30000)) {
            if (this.player.nPoint.wearingTrainArmor) {
                for (Item.ItemOption io : this.player.inventory.trainArmor.itemOptions) {
                    if (io.optionTemplate.id == 9) {
                        if (io.param < 1000) {
                            io.param++;
                            InventoryService.gI().sendItemBody(player);
                        }
                        break;
                    }
                }
            }
            this.lastTimeAddTimeTrainArmor = System.currentTimeMillis();
        }
        if (Util.canDoWithTime(lastTimeSubTimeTrainArmor, 60000)) {
            boolean updatedBagTrainArmor = false;
            for (Item item : this.player.inventory.itemsBag) {
                if (item.isNotNullItem()) {
                    if (ItemService.gI().isTrainArmor(item)) {
                        for (Item.ItemOption io : item.itemOptions) {
                            if (io.optionTemplate.id == 9) {
                                if (io.param > 0) {
                                    io.param--;
                                    updatedBagTrainArmor = true;
                                }
                            }
                        }
                    }
                } else {
                    break;
                }
            }
            for (Item item : this.player.inventory.itemsBox) {
                if (item.isNotNullItem()) {
                    if (ItemService.gI().isTrainArmor(item)) {
                        for (Item.ItemOption io : item.itemOptions) {
                            if (io.optionTemplate.id == 9) {
                                if (io.param > 0) {
                                    io.param--;
                                }
                            }
                        }
                    }
                } else {
                    break;
                }
            }
            this.lastTimeSubTimeTrainArmor = System.currentTimeMillis();
            if (updatedBagTrainArmor) {
                InventoryService.gI().sendItemBags(player);
            }
            Service.gI().point(this.player);
        }
    }

    private void updateVoHinh() {
        if (this.player.nPoint != null && this.player.nPoint.wearingVoHinh) {
            if (Util.canDoWithTime(lastTimeAttack, 5000)) {
                isVoHinh = Util.canDoWithTime(lastTimeVoHinh, 5000);
            } else {
                isVoHinh = false;
            }
        } else {
            isVoHinh = false;
        }
    }

    public void dispose() {
        this.player = null;
    }
}
