package nro.models.npc_list;

import nro.models.clan.Clan;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import java.util.ArrayList;
import nro.models.npc.Npc;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.map.service.NpcService;
import nro.models.services.OpenPowerService;
import nro.models.services.RewardService;
import nro.models.services.Service;
import nro.models.shop.ShopService;
import nro.models.services.TaskService;
import nro.models.map.service.ChangeMapService;
import nro.models.services_func.Input;
import nro.models.services.PlayerService;
import nro.models.skill.Skill;
import nro.models.utils.Logger;
import nro.models.utils.SkillUtil;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

public class TruongLaoGuru extends Npc {

    public TruongLaoGuru(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                ArrayList<String> menu = new ArrayList<>();
                if (player.gender == ConstPlayer.NAMEC) {
                    menu.add("Nhiệm vụ");
                    menu.add("Học\nKỹ năng");
                }
                menu.add("Mở giới hạn\nSức mạnh");

                String[] menus = menu.toArray(String[]::new);
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Chào con, ta rất vui khi gặp được con\nCon muốn làm gì nào ?", menus);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                handleBaseMenu(player, select);
            } else if (player.idMark.getIndexMenu() == 12) {
                handleSkillLearning(player, select);
            } else if (player.idMark.getIndexMenu() == 20) {
                handleOpenPowerMenu(player, select);
            } else if (player.idMark.getIndexMenu() == ConstNpc.OPEN_POWER_MYSEFT) {
                switch (select) {
                    case 0 -> {
                        if (player.nPoint.limitPower < NPoint.MAX_LIMIT) {
                            OpenPowerService.gI().openPowerBasic(player);
                        } else {
                            Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã đạt tới mức tối đa hiện tại");
                        }
                    }
                    case 1 -> {
                        if (player.nPoint.limitPower < NPoint.MAX_LIMIT) {
                            if (player.inventory.gem >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                                if (OpenPowerService.gI().openPowerSpeed(player)) {
                                    player.inventory.gem -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                                    Service.gI().sendMoney(player);
                                }
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Bạn không đủ ngọc để mở, còn thiếu "
                                        + Util.numberToMoney((OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gem)) + " ngọc");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã đạt tới mức tối đa hiện tại");
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.OPEN_POWER_PET) {
                if (select == 0) {
                    if (player.pet != null && player.pet.nPoint.limitPower < NPoint.MAX_LIMIT) {
                        if (player.inventory.gem >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                            if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                                player.inventory.gem -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                                Service.gI().sendMoney(player);
                            }
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Bạn không đủ ngọc để mở, còn thiếu "
                                    + Util.numberToMoney((OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gem)) + " ngọc");
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Giới hạn sức mạnh của đệ tử đã đạt tới mức tối đa hiện tại");
                    }
                }
            }
        }
    }

    private void handleBaseMenu(Player player, int select) {
        if (player.gender != ConstPlayer.NAMEC) {
            if (select == 0) {
                this.createOtherMenu(player, 20,
                        "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                        "Bản thân", "Đệ tử", "Từ chối");
            }
            return;
        }
        switch (select) {
            case 0 ->
                NpcService.gI().createTutorial(player, tempId, avartar, player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
            case 1 ->
                handleSkillLearningMenu(player);
            case 2 ->
                this.createOtherMenu(player, 20,
                        "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                        "Bản thân", "Đệ tử", "Từ chối");
        }
    }

    private void handleOpenPowerMenu(Player player, int select) {
        switch (select) {
            case 0 -> {
                if (player.nPoint.limitPower < NPoint.MAX_LIMIT) {
                    this.createOtherMenu(player, ConstNpc.OPEN_POWER_MYSEFT,
                            "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của bản thân lên "
                            + Util.numberToMoney(player.nPoint.getPowerNextLimit()),
                            "Nâng\ngiới hạn\nsức mạnh",
                            "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " ngọc",
                            "Đóng");
                } else {
                    this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Sức mạnh của con đã đạt tới giới hạn hiện tại",
                            "Đóng");
                }
            }
            case 1 -> {
                if (player.pet != null) {
                    if (player.pet.nPoint.limitPower < NPoint.MAX_LIMIT) {
                        this.createOtherMenu(player, ConstNpc.OPEN_POWER_PET,
                                "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của đệ tử lên "
                                + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit()),
                                "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " ngọc", "Đóng");
                    } else {
                        this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Sức mạnh của đệ con đã đạt tới giới hạn hiện tại",
                                "Đóng");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                }
            }
        }
    }

    private void handleSkillLearningMenu(Player player) {
        if (player.LearnSkill.Time != -1) {
            long ngoc = 5;
            var time = player.LearnSkill.Time - System.currentTimeMillis();
            if (time / 600_000 >= 2) {
                ngoc += time / 600_000;
            }
            long goldCost = ngoc * 1_000_000L;
            String[] subName = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId).name.split("");
            byte level = Byte.parseByte(subName[subName.length - 1]);
            createOtherMenu(player, 12,
                    "Con đang học kỹ năng\n" + SkillUtil.findSkillTemplate(SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId)).name
                    + " cấp " + level + "\nThời gian còn lại " + TimeUtil.getTime(time),
                    "Học Cấp tốc\n" + Util.numberToMoney(goldCost) + " vàng", "Huỷ", "Bỏ qua");
        } else {
            ShopService.gI().opendShop(player, "QUY_LAO", false);
        }
    }

    private void handleSkillLearning(Player player, int select) {
        switch (select) {
            case 0 -> {
                var time = player.LearnSkill.Time - System.currentTimeMillis();
                long ngoc = 5;
                if (time / 600_000 >= 2) {
                    ngoc += time / 600_000;
                }
                long goldCost = ngoc * 1_000_000L;
                if (player.inventory.gold < goldCost) {
                    Service.gI().sendThongBao(player, "Bạn không có đủ vàng");
                    return;
                }
                player.inventory.subGold(goldCost);
                player.LearnSkill.Time = -1;
                learnSkill(player);
            }
            case 1 ->
                createOtherMenu(player, 13, "Con có muốn huỷ học kỹ năng này và nhận lại 50% số tiềm năng không ?", "Ok", "Đóng");
        }
    }

    private void learnSkill(Player player) {
        try {
            String[] subName = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId).name.split("");
            byte level = Byte.parseByte(subName[subName.length - 1]);
            Skill curSkill = SkillUtil.getSkillByItemID(player, player.LearnSkill.ItemTemplateSkillId);

            if (curSkill.point == 0) {
                player.BoughtSkill.add((int) player.LearnSkill.ItemTemplateSkillId);
                curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId), level);
                SkillUtil.setSkill(player, curSkill);
                var msg = Service.gI().messageSubCommand((byte) 62);
                msg.writer().writeShort(curSkill.skillId);
                player.sendMessage(msg);
                msg.cleanup();
                PlayerService.gI().sendInfoHpMpMoney(player);
            } else {
                curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId), level);
                player.BoughtSkill.add((int) player.LearnSkill.ItemTemplateSkillId);
                SkillUtil.setSkill(player, curSkill);
                var msg = Service.gI().messageSubCommand((byte) 62);
                msg.writer().writeShort(curSkill.skillId);
                player.sendMessage(msg);
                msg.cleanup();
                PlayerService.gI().sendInfoHpMpMoney(player);
            }
        } catch (Exception e) {
            Logger.log(e.toString());
        }
    }
}
