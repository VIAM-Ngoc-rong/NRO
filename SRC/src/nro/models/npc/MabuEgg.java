package nro.models.npc;

import nro.models.map.service.ChangeMapService;
import nro.models.services.PetService;
import nro.models.player.Player;
import nro.models.utils.Util;
import nro.models.network.Message;
import nro.models.services.Service;
import nro.models.utils.Logger;

public class MabuEgg {

    private static final long DEFAULT_TIME_DONE = 86_400_000L; // 1 ngày (24h)

    private Player player;
    public long lastTimeCreate;
    public long timeDone;
    public int typeEgg = 0; // 0: Trứng Mabư, 1: Trứng Broly

    private final short id = 50;

    public MabuEgg(Player player, long lastTimeCreate, long timeDone) {
        this(player, lastTimeCreate, timeDone, 0);
    }

    public MabuEgg(Player player, long lastTimeCreate, long timeDone, int typeEgg) {
        this.player = player;
        this.lastTimeCreate = lastTimeCreate;
        this.timeDone = timeDone;
        this.typeEgg = typeEgg;
    }

    public static void createMabuEgg(Player player) {
        player.mabuEgg = new MabuEgg(player, System.currentTimeMillis(), DEFAULT_TIME_DONE, 0);
    }

    public static void createBrolyEgg(Player player) {
        player.mabuEgg = new MabuEgg(player, System.currentTimeMillis(), DEFAULT_TIME_DONE, 1);
    }

    public void sendMabuEgg() {
        Message msg;
        try {

            msg = new Message(-122);
            msg.writer().writeShort(this.id);
            msg.writer().writeByte(1);
            msg.writer().writeShort(4664);
            msg.writer().writeByte(0);
            msg.writer().writeInt(this.getSecondDone());
            this.player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(MabuEgg.class, e);
        }
    }

    public int getSecondDone() {
        int seconds = (int) ((lastTimeCreate + timeDone - System.currentTimeMillis()) / 1000);
        return seconds > 0 ? seconds : 0;
    }

    public void openEgg(int gender) {
        if (this.typeEgg == 1) {
            if (this.player.pet == null || this.player.pet.typePet != 1) {
                Service.gI().sendThongBao(player, "Bắt buộc phải có Đệ tử Mabư trước mới mở được Trứng Broly!");
                return;
            }
            try {
                destroyEgg();
                Thread.sleep(4000);
                PetService.gI().changeBrolyPet(this.player, gender);
                ChangeMapService.gI().changeMapInYard(this.player, this.player.gender * 7, -1, Util.nextInt(300, 500));
                player.mabuEgg = null;
            } catch (Exception e) {
            }
            return;
        }
        if (this.player.pet != null) {
            try {
                destroyEgg();
                Thread.sleep(4000);
                if (this.player.pet == null) {
                    PetService.gI().createMabuPet(this.player, gender);
                } else {
                    PetService.gI().changeMabuPet(this.player, gender);
                }
                ChangeMapService.gI().changeMapInYard(this.player, this.player.gender * 7, -1, Util.nextInt(300, 500));
                player.mabuEgg = null;
            } catch (Exception e) {
            }
        } else {
            Service.gI().sendThongBao(player, "Bắt buộc phải có Đệ tử trước mới mở được Trứng Mabư!");
        }
    }

    public void destroyEgg() {
        try {
            Message msg = new Message(-117);
            msg.writer().writeByte(101);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
        this.player.mabuEgg = null;
    }

    public void subTimeDone(int d, int h, int m, int s) {
        this.timeDone -= ((d * 24 * 60 * 60 * 1000) + (h * 60 * 60 * 1000) + (m * 60 * 1000) + (s * 1000));
        this.sendMabuEgg();
    }

    public void dispose() {
        this.player = null;
    }
}
