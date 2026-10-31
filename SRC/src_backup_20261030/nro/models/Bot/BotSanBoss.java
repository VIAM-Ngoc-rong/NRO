package nro.models.Bot;

import java.util.Comparator;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import nro.models.consts.ConstPlayer;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.map.service.ChangeMapService;
import nro.models.mob.Mob;
import nro.models.skill.Skill;
import nro.models.map.service.MapService;
import nro.models.services.PlayerService;
import nro.models.services.SkillService;
import nro.models.utils.Util;

public class BotSanBoss {

    public Bot bot;

    public Boss bossAttack;

    public long lastTimeSkill1;

    public BotSanBoss(Bot b) {
        this.bot = b;
    }

    public void update() {
        this.SanBot();
    }

    public boolean isMap(int mapId) {
        return (MapService.gI().isMapDoanhTrai(mapId) || MapService.gI().isMapBlackBallWar(mapId)
                || MapService.gI().isMapBanDoKhoBau(mapId) || MapService.gI().isMapMaBu(mapId)
                || MapService.gI().isMapCold(mapId)
                || MapService.gI().isMapConDuongRanDoc(mapId));
    }

    public void GetBoss(int status) {
        if (this.bossAttack == null || this.bossAttack.isDie() || this.bossAttack.zone == null) {
            List<Boss> allBosses = BossManager.gI().getBosses();
            if (allBosses == null || allBosses.isEmpty()) {
                this.bossAttack = null;
                return;
            }
            List<Boss> validBosses = new ArrayList<>();
            for (int i = 0; i < allBosses.size(); i++) {
                Boss b = allBosses.get(i);
                if (b == null || b.isDie() || b.zone == null || b.zone.map == null) {
                    continue;
                }
                int bId = (int) b.id;
                boolean isBojackTeam = bId == nro.models.boss.BossID.BUJIN || bId == nro.models.boss.BossID.KOGU
                        || bId == nro.models.boss.BossID.ZANGYA || bId == nro.models.boss.BossID.BIDO
                        || bId == nro.models.boss.BossID.BOJACK || bId == nro.models.boss.BossID.SUPER_BOJACK;
                if (!isBojackTeam
                        && !this.isMap(b.zone.map.mapId)
                        && !b.zone.isFullPlayer()
                        && b.zone.mobs != null
                        && b.zone.mobs.size() >= 1) {
                    validBosses.add(b);
                }
            }
            if (!validBosses.isEmpty()) {
                this.bossAttack = validBosses.get(Util.nextInt(0, validBosses.size() - 1));
                if (this.bot.zone != this.bossAttack.zone) {
                    ChangeMapService.gI().goToMap(this.bot, this.bossAttack.zone);
                    if (this.bot.zone != null) {
                        this.bot.zone.load_Me_To_Another(this.bot);
                    }
                }
            } else {
                this.bossAttack = null;
            }
        }
    }

    public void GetSkil() {
        if (this.bot == null || this.bot.playerSkill == null || this.bot.playerSkill.skills.size() < 2) {
            return;
        }
        if (Util.isTrue(50, 100)) {
            this.bot.playerSkill.skillSelect = this.bot.playerSkill.skills.get(0);
        } else {
            this.bot.playerSkill.skillSelect = this.bot.playerSkill.skills.get(1);
        }
        if (this.lastTimeSkill1 < System.currentTimeMillis() - 50000) {
            switch (this.bot.gender) {
                case ConstPlayer.XAYDA:
                    this.bot.useSkill(Skill.BIEN_KHI);
                    break;
                case ConstPlayer.TRAI_DAT:
                    this.bot.useSkill(Skill.QUA_CAU_KENH_KHI);
                    break;
                case ConstPlayer.NAMEC:
                    this.bot.useSkill(Skill.MAKANKOSAPPO);
                    break;
            }
            this.lastTimeSkill1 = System.currentTimeMillis() - new Random().nextInt(150000);
        }
    }

    public void SanBot() {
        this.GetBoss(0);
        this.GetSkil();
        if (this.bossAttack != null && !this.bossAttack.isDie() && this.bossAttack.zone != null && this.bot.zone != null) {
            if (this.bot.UseLastTimeSkill()) {
                PlayerService.gI().playerMove(this.bot, this.bossAttack.location.x, this.bossAttack.location.y);
                SkillService.gI().useSkill(this.bot, this.bossAttack, null, 0, null);
            }
        }
    }
}
