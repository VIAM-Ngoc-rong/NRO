package nro.models.services.shenron;

import nro.models.network.Message;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import nro.models.player.Player;
import lombok.Getter;
import lombok.Setter;
import nro.models.map.Zone;
import nro.models.server.Client;
import nro.models.services.ItemService;
import nro.models.services.InventoryService;
import nro.models.services.ItemTimeService;
import nro.models.services.PlayerService;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import nro.models.services.IntrinsicService;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class Shenron_Event {

    @Setter
    @Getter
    private Player player;

    @Setter
    @Getter
    private Zone zone;

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Zone getZone() {
        return this.zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public long playerId;
    public boolean isPlayerDisconnect;
    public byte select;
    public int shenronType;
    public boolean leaveMap;

    public static final byte WISHED = 0;
    public static final byte TIME_UP = 1;

    public static final byte DRAGON_EVENT = 1;

    public long lastTimeShenronWait;
    public static int timeResummonShenron = 10000;
    public static int timeShenronWait = 60000;

    public static final String SHENRONEVENT_SAY
            = "Ta là Rồng Xương Halloween! Ta sẽ ban cho ngươi 1 điều ước, ngươi có 5 phút, hãy chọn đi:\n"
            + "1) Đổi skill 3, 4 đệ tử (có thể trùng skill trước đó).\n"
            + "2) Thay đổi nội tại (Yêu cầu 10 Tỷ Sức mạnh).\n"
            + "3) Cải trang Siêu Thần HSD 90 ngày.\n"
            + "4) Cải trang Black Gohan Rose HSD 90 ngày.\n"
            + "5) Tăng 10% HP, KI, SĐ trong 30 phút.";

    public static final String SHENRON_TIM_SAY
            = "Ta là Rồng Thần Tím! Ta sẽ ban cho ngươi 1 điều ước cao cấp, hãy chọn đi:\n"
            + "1) Giàu có: +20 Tỷ Vàng & 50 Thỏi vàng.\n"
            + "2) Chí mạng gốc +2% (Tối đa 12%).\n"
            + "3) Găng tay đang đeo lên 1 cấp (Tối đa cấp 8).\n"
            + "4) Đổi ngẫu nhiên Chiêu 2, 3, 4 Đệ tử.\n"
            + "5) Tăng 10% HP, KI, SĐ trong 60 phút & 5 Đá Hematite.";

    public static final String SHENRON_BANG_SAY
            = "Ta là Rồng Thần Băng Giáng Sinh! Ta sẽ ban cho ngươi 1 điều ước lễ hội, hãy chọn đi:\n"
            + "1) Cải trang Noel VIP (35% SĐ/HP/KI, Kháng lạnh, HSD 30-90 ngày).\n"
            + "2) Lễ hội Hồng Ngọc: +20.000 Hồng Ngọc & +10 Điểm Sự Kiện.\n"
            + "3) Phú quý Giáng Sinh: +1.000 Ngọc Xanh & 10 Thỏi Vàng.\n"
            + "4) Đổi ngẫu nhiên Chiêu 2, 3, 4 Đệ tử.\n"
            + "5) Quần đang mặc lên 1 cấp (Tối đa cấp 7) & +10% HP, KI, SĐ (30 phút).";

    public static final String[] SHENRON_WISHES
            = new String[]{"Điều ước 1", "Điều ước 2", "Điều ước 3", "Điều ước 4", "Điều ước 5"};

    public boolean shenronLeave;

    public void update() {
        try {
            if (!shenronLeave) {
                if (isPlayerDisconnect) {
                    Player pl = Client.gI().getPlayer(playerId);
                    if (pl != null) {
                        player = pl;
                        if (player.zone != null && player.zone.map.mapId != 0 && player.zone.map.mapId != 7 && player.zone.map.mapId != 14
                                && player.zone.map.mapId != 21 && player.zone.map.mapId != 22 && player.zone.map.mapId != 23) {
                            player.shenronEvent = this;
                            zone = player.zone;
                            player.idMark.setShenronType(shenronType);
                            isPlayerDisconnect = false;
                            reSummonShenron();
                        }
                    }
                }
                if (Util.canDoWithTime(lastTimeShenronWait, timeShenronWait)) {
                    leaveMap = true;
                    NpcService.gI().createMenuRongThieng(player, ConstNpc.IGNORE_MENU, "Còn cái nịt =))\nCó không ước mất đừng tìm.", "Xin vĩnh biệt cụ........");
                    shenronLeave();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reSummonShenron() {
        activeShenron(true, DRAGON_EVENT);
        sendBlackGokuhesShenron();
    }

    public void sendBlackGokuhesShenron() {
        String say = switch (player.idMark.getShenronType()) {
            case 1 -> SHENRON_TIM_SAY;
            case 2 -> SHENRON_BANG_SAY;
            default -> SHENRONEVENT_SAY;
        };
        NpcService.gI().createMenuRongThieng(player, ConstNpc.SHOW_SHENRON_EVENT_CONFIRM, say, SHENRON_WISHES);
    }

    public void showConfirmShenron(byte select) {
        this.select = select;
        String wish = null;
        if (select >= 0 && select < SHENRON_WISHES.length) {
            wish = SHENRON_WISHES[select];
        }
        NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_EVENT_CONFIRM, "Ngươi có chắc muốn ước?", wish, "Từ chối");
    }

    public void activeShenron(boolean appear, byte type) {
        Message msg;
        try {
            msg = new Message(-83);
            msg.writer().writeByte(appear ? 0 : (byte) 1);
            if (appear) {
                msg.writer().writeShort(player.zone.map.mapId);
                msg.writer().writeShort(player.zone.map.bgId);
                msg.writer().writeByte(player.zone.zoneId);
                msg.writer().writeInt((int) player.id);
                msg.writer().writeUTF("null");
                msg.writer().writeShort(player.location.x);
                msg.writer().writeShort(player.location.y);
                msg.writer().writeByte(type);
                playerId = player.id;
                shenronType = player.idMark.getShenronType();
                zone.shenronType = shenronType;
                lastTimeShenronWait = System.currentTimeMillis();
                player.isShenronAppear = true;
            }
            Service.gI().sendMessAllPlayerInMap(player, msg);
        } catch (Exception e) {
        }
    }

    public void confirmWish() {
        switch (player.idMark.getShenronType()) {
            case 0: // Rồng Xương Halloween (Bí Ngô 1-7 sao)
                switch (this.select) {
                    case 0: //thay chiêu 3-4 đệ tử
                        if (player.pet != null) {
                            if (player.pet.playerSkill.skills.get(2).skillId != -1) {
                                player.pet.openSkill3();
                                if (player.pet.playerSkill.skills.get(3).skillId != -1) {
                                    player.pet.openSkill4();
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 3 chứ!");
                                sendBlackGokuhesShenron();
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 1:
                        if (player.getSession().player.nPoint.power >= 10_000_000_000L) {
                            IntrinsicService.gI().doinoitai(player);
                        } else {
                            Service.gI().sendThongBao(player, "Yêu cầu đạt 10 Tỷ Sức Mạnh!");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 2:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            byte gender = this.player.gender;
                            Item avtVip = ItemService.gI().createNewItem((short) (gender == ConstPlayer.TRAI_DAT ? 905
                                    : gender == ConstPlayer.NAMEC ? 907 : 911));
                            avtVip.itemOptions.add(new Item.ItemOption(50, 22));
                            avtVip.itemOptions.add(new Item.ItemOption(47, 400));
                            avtVip.itemOptions.add(new Item.ItemOption(108, 30));
                            avtVip.itemOptions.add(new Item.ItemOption(33, 1));
                            avtVip.itemOptions.add(new Item.ItemOption(93, 90));
                            InventoryService.gI().addItemBag(player, avtVip);
                            InventoryService.gI().sendItemBags(player);
                        } else {
                            Service.gI().sendThongBao(player, "Hành trang đã đầy");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 3:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            Item avtVip = ItemService.gI().createNewItem((short) 883);
                            avtVip.itemOptions.add(new Item.ItemOption(50, 24));
                            avtVip.itemOptions.add(new Item.ItemOption(14, 3));
                            avtVip.itemOptions.add(new Item.ItemOption(103, 19));
                            avtVip.itemOptions.add(new Item.ItemOption(80, 10));
                            avtVip.itemOptions.add(new Item.ItemOption(93, 90));
                            InventoryService.gI().addItemBag(player, avtVip);
                            InventoryService.gI().sendItemBags(player);
                        } else {
                            Service.gI().sendThongBao(player, "Hành trang đã đầy");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 4:// Tăng hp, ki, sd
                        int timeRX = player.itemTime.timeRX / 1000 + 1800;
                        int maxTimeInSeconds = 32767;
                        if (timeRX >= maxTimeInSeconds) {
                            Service.gI().sendThongBao(player, "Ước ít thôi con :v");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        player.itemTime.isUseRX = true;
                        player.itemTime.timeRX = timeRX * 1000;
                        player.itemTime.lastTimeUseRX = System.currentTimeMillis();
                        ItemTimeService.gI().sendItemTime(player, 6581, timeRX);
                        player.nPoint.calPoint();
                        player.nPoint.setHp((int) player.nPoint.hpMax);
                        player.nPoint.setMp((int) player.nPoint.mpMax);
                        Service.gI().point(player);
                        Service.gI().Send_Info_NV(player);
                        break;
                }
                break;
            case 2: // Rồng Thần Băng Giáng Sinh (Ngọc Rồng Băng 1-7 sao: 925-931)
                switch (this.select) {
                    case 0: // Cải trang Noel VIP (35% SĐ/HP/KI, Kháng lạnh, HSD 30-90 ngày)
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            byte gender = this.player.gender;
                            short ctId = (short) (gender == ConstPlayer.TRAI_DAT ? 387
                                    : gender == ConstPlayer.NAMEC ? 390 : 393);
                            Item ctNoel = ItemService.gI().createNewItem(ctId);
                            ctNoel.itemOptions.add(new Item.ItemOption(50, 35));
                            ctNoel.itemOptions.add(new Item.ItemOption(77, 35));
                            ctNoel.itemOptions.add(new Item.ItemOption(103, 35));
                            ctNoel.itemOptions.add(new Item.ItemOption(80, 20));
                            ctNoel.itemOptions.add(new Item.ItemOption(106, 0));
                            ctNoel.itemOptions.add(new Item.ItemOption(93, Util.nextInt(30, 90)));
                            InventoryService.gI().addItemBag(player, ctNoel);
                            InventoryService.gI().sendItemBags(player);
                        } else {
                            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 1: // +20.000 Hồng Ngọc & +10 Điểm Sự Kiện
                        this.player.inventory.ruby += 20000;
                        this.player.point_sukien += 10;
                        this.player.event.addEventPoint(10);
                        PlayerService.gI().sendInfoHpMpMoney(this.player);
                        Service.gI().sendThongBao(player, "Bạn nhận được 20.000 Hồng Ngọc và +10 Điểm Sự Kiện!");
                        break;
                    case 2: // +1.000 Ngọc Xanh & 10 Thỏi Vàng
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            this.player.inventory.gem += 1000;
                            PlayerService.gI().sendInfoHpMpMoney(this.player);
                            Item thoiVang = ItemService.gI().createNewItem((short) 457, 10);
                            InventoryService.gI().addItemBag(player, thoiVang);
                            InventoryService.gI().sendItemBags(player);
                        } else {
                            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 3: // Đổi ngẫu nhiên Chiêu 2, 3, 4 Đệ tử
                        if (player.pet != null) {
                            if (player.pet.playerSkill.skills.get(1).skillId != -1) {
                                player.pet.openSkill2();
                                if (player.pet.playerSkill.skills.get(2).skillId != -1) {
                                    player.pet.openSkill3();
                                }
                                if (player.pet.playerSkill.skills.get(3).skillId != -1) {
                                    player.pet.openSkill4();
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải mở chiêu 2 chứ!");
                                sendBlackGokuhesShenron();
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 4: // Quần đang mặc lên 1 cấp (Tối đa cấp 7) & +10% HP, KI, SĐ (30 phút)
                        Item item = this.player.inventory.itemsBody.get(1);
                        if (item.isNotNullItem()) {
                            int level = 0;
                            for (Item.ItemOption io : item.itemOptions) {
                                if (io.optionTemplate.id == 72) {
                                    level = io.param;
                                    if (level < 7) {
                                        io.param++;
                                    }
                                    break;
                                }
                            }
                            if (level < 7) {
                                if (level == 0) {
                                    item.itemOptions.add(new Item.ItemOption(72, 1));
                                }
                                for (Item.ItemOption io : item.itemOptions) {
                                    if (InventoryService.gI().optionCanUpgrade(io.optionTemplate.id)) {
                                        io.param += (io.param * 10 / 100);
                                    }
                                }
                                InventoryService.gI().sendItemBody(player);
                            } else {
                                Service.gI().sendThongBao(player, "Quần của ngươi đã đạt cấp tối đa (Cấp 7)");
                                sendBlackGokuhesShenron();
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Ngươi hiện tại có mặc quần đâu!");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        int timeRB = player.itemTime.timeRX / 1000 + 1800;
                        if (timeRB >= 32767) {
                            timeRB = 32766;
                        }
                        player.itemTime.isUseRX = true;
                        player.itemTime.timeRX = timeRB * 1000;
                        player.itemTime.lastTimeUseRX = System.currentTimeMillis();
                        ItemTimeService.gI().sendItemTime(player, 6581, timeRB);
                        player.nPoint.calPoint();
                        player.nPoint.setHp((int) player.nPoint.hpMax);
                        player.nPoint.setMp((int) player.nPoint.mpMax);
                        Service.gI().point(player);
                        Service.gI().Send_Info_NV(player);
                        break;
                }
                break;
            case 1: // Rồng Thần Tím (Ngọc Rồng Tím 1-7 sao)
                switch (this.select) {
                    case 0: // +20 Tỷ Vàng & 50 Thỏi vàng
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            this.player.inventory.addGold(20_000_000_000L);
                            PlayerService.gI().sendInfoHpMpMoney(this.player);
                            Item thoiVang = ItemService.gI().createNewItem((short) 457, 50);
                            InventoryService.gI().addItemBag(player, thoiVang);
                            InventoryService.gI().sendItemBags(player);
                        } else {
                            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 1: // Chí mạng gốc +2% (Tối đa 12%)
                        if (this.player.nPoint.critdragon < 12) {
                            this.player.nPoint.critdragon = (byte) Math.min(12, this.player.nPoint.critdragon + 2);
                            Service.gI().point(player);
                        } else {
                            Service.gI().sendThongBao(player, "Chí mạng Rồng Thần của ngươi đã đạt tối đa (12%)!");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 2: // Găng tay đang đeo lên 1 cấp (Tối đa cấp 8)
                        Item gang = this.player.inventory.itemsBody.get(2);
                        if (gang.isNotNullItem()) {
                            int level = 0;
                            for (Item.ItemOption io : gang.itemOptions) {
                                if (io.optionTemplate.id == 72) {
                                    level = io.param;
                                    if (level < 8) {
                                        io.param++;
                                    }
                                    break;
                                }
                            }
                            if (level < 8) {
                                if (level == 0) {
                                    gang.itemOptions.add(new Item.ItemOption(72, 1));
                                }
                                for (Item.ItemOption io : gang.itemOptions) {
                                    if (io.optionTemplate.id == 0) {
                                        io.param += (io.param * 10 / 100);
                                        break;
                                    }
                                }
                                InventoryService.gI().sendItemBody(player);
                                Service.gI().point(player);
                            } else {
                                Service.gI().sendThongBao(player, "Găng tay của ngươi đã đạt cấp tối đa (Cấp 8)");
                                sendBlackGokuhesShenron();
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Ngươi hiện tại có đeo găng tay đâu!");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 3: // Đổi ngẫu nhiên Chiêu 2, 3, 4 Đệ tử
                        if (player.pet != null) {
                            if (player.pet.playerSkill.skills.get(1).skillId != -1) {
                                player.pet.openSkill2();
                                if (player.pet.playerSkill.skills.get(2).skillId != -1) {
                                    player.pet.openSkill3();
                                }
                                if (player.pet.playerSkill.skills.get(3).skillId != -1) {
                                    player.pet.openSkill4();
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải mở chiêu 2 chứ!");
                                sendBlackGokuhesShenron();
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        break;
                    case 4: // Tăng 10% HP, KI, SĐ trong 60 phút & 5 Đá Hematite
                        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
                            Service.gI().sendThongBao(player, "Hành trang cần ít nhất 1 ô trống");
                            sendBlackGokuhesShenron();
                            return;
                        }
                        int timeRX = player.itemTime.timeRX / 1000 + 3600;
                        int maxTimeInSeconds = 32767;
                        if (timeRX >= maxTimeInSeconds) {
                            timeRX = maxTimeInSeconds - 1;
                        }
                        player.itemTime.isUseRX = true;
                        player.itemTime.timeRX = timeRX * 1000;
                        player.itemTime.lastTimeUseRX = System.currentTimeMillis();
                        ItemTimeService.gI().sendItemTime(player, 6581, timeRX);
                        player.nPoint.calPoint();
                        player.nPoint.setHp((int) player.nPoint.hpMax);
                        player.nPoint.setMp((int) player.nPoint.mpMax);
                        Service.gI().point(player);
                        Service.gI().Send_Info_NV(player);
                        Item hematite = ItemService.gI().createNewItem((short) 1423, 5);
                        InventoryService.gI().addItemBag(player, hematite);
                        InventoryService.gI().sendItemBags(player);
                        break;
                }
                break;
        }
        shenronLeave();
    }

    public void shenronLeave() {
        if (!shenronLeave) {
            shenronLeave = true;
            if (player != null && player.zone != null) {
                player.shenronEvent = null;
                if (!leaveMap) {
                    NpcService.gI().createTutorial(player, 0, "Điều ước của ngươi đã được thực hiện...tạm biệt");
                }
                activeShenron(false, DRAGON_EVENT);
                player.isShenronAppear = false;
                select = -1;
            }
            zone.shenronType = -1;
            player.lastTimeShenronAppeared = System.currentTimeMillis();
            Shenron_Manager.gI().remove(this);
        }
    }
}
