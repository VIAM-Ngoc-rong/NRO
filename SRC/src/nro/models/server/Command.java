package nro.models.server;

import nro.models.services.PlayerService;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.consts.ConstNpc;
import nro.models.managers.GiftCodeManager;
import nro.models.item.Item;
import java.util.ArrayList;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.network.SessionManager;
import nro.models.services.ItemService;
import nro.models.services.PetService;
import nro.models.services.Service;
import nro.models.services_func.Input;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.NpcService;
import nro.models.services.InventoryService;
import nro.models.utils.SystemMetrics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import nro.models.Bot.BotAttackplayer;
import nro.models.Bot.BotManager;
import nro.models.consts.ConstPlayer;
import nro.models.data.LocalManager;
import nro.models.services.TaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class Command {

    private static Command instance;

    private final Map<String, Consumer<Player>> adminCommands = new HashMap<>();
    private final Map<String, BiConsumer<Player, String>> parameterizedCommands = new java.util.LinkedHashMap<>();

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    private Command() {
        initAdminCommands();
        initParameterizedCommands();
    }

    private void initAdminCommands() {
        adminCommands.put("item", player -> Input.gI().createFormGiveItem(player));
        adminCommands.put("brl", player -> BrolyManager.gI().showListBoss(player));
        adminCommands.put("getitem", player -> Input.gI().createFormGetItem(player));
        adminCommands.put("hs", player -> Service.gI().releaseCooldownSkill(player));
        adminCommands.put("d", player -> Service.gI().setPos(player, player.location.x, player.location.y + 10));
        adminCommands.put("a", player -> NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, -1,
                "|0|Time start: " + ServerManager.timeStart
                + "\nClients: " + Client.gI().getPlayers().size()
                + "\n Sessions: " + SessionManager.gI().getNumSession()
                + "\nThreads: " + Thread.activeCount()
                + " luồng" + "\n" + SystemMetrics.ToString(),
                "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "Đóng"));
        adminCommands.put("smdt", player -> {
            if (player.pet == null) {
                Service.gI().sendThongBao(player, "Bạn chưa có đệ tử");
                return;
            }
            Service.gI().addSMTN(player.pet, (byte) 2, 1_500_000L, false);
            Service.gI().sendThongBao(player, "Đã tăng 1.500.000 sức mạnh & tiềm năng cho đệ tử!");
        });
        adminCommands.put("updt", player -> {
            if (player.pet == null) {
                Service.gI().sendThongBao(player, "Bạn chưa có đệ tử");
                return;
            }
            Service.gI().addSMTN(player.pet, (byte) 2, 1_500_000L, false);
            Service.gI().sendThongBao(player, "Đã tăng 1.500.000 sức mạnh & tiềm năng cho đệ tử!");
        });
        adminCommands.put("baotri", player -> {
            Maintenance.gI().startCountdown();
            Service.gI().sendThongBao(player, "Đã kích hoạt đếm ngược bảo trì!");
        });
    }

    private void initParameterizedCommands() {
        parameterizedCommands.put("m", (player, text) -> {
            String arg = text.substring(1).trim();
            if (!arg.matches("^\\d+$")) {
                Service.gI().chat(player, text);
                return;
            }
            try {
                int mapId = Integer.parseInt(arg);
                ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Sai định dạng map ID!");
            }
        });

        adminCommands.put("toado", player -> {
            Service.gI().sendThongBaoOK(player, "x: " + player.location.x + " - y: " + player.location.y);
        });

        adminCommands.put("1", player -> {
            NpcService.gI().createMenuConMeo(player, 206783, 206783, "|7| Menu bot\n"
                    + "Player online : " + Client.gI().getPlayers().size() + "\n"
                    + "\b|1|Thread: " + Thread.activeCount() + "\n"
                    + "\n Sessions: " + SessionManager.gI().getNumSession() + "\n"
                    + "Bot online : " + BotManager.gI().bot.size(),
                    "Bot\nPem Quái", "Bot\nBán Item", "Bot\nSăn Boss", "Bot\nAttack Player");
        });

        adminCommands.put("2", player -> {
            player.originalName = player.name;
            PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_ALL);
            player.originalName = player.name;
            Service.gI().Send_Caitrang(player);
            BotAttackplayer bot = new BotAttackplayer((short) 1624, (short) 1628, (short) 1629, 1, "đánh nhau không?", (short) 0);
            bot.player = player;
            bot.zone = player.zone;
            bot.location.x = player.location.x;
            bot.location.y = player.location.y;

            player.zone.addPlayer(bot);
            BotManager.gI().bot.add(bot);

            for (Player p : player.zone.getPlayers()) {
                if (p.session != null) {
                    Service.gI().sendAppear(bot, p);
                    Service.gI().sendInfoCharMoiToMe(p, bot);
                }
            }

            if (player.session != null) {
                Service.gI().Send_Info_NV(player);
            }

            bot.update();

            ServerNotify.gI().notify("Đã gọi bot tấn công người chơi!");
        });

        adminCommands.put("b", player -> {
            Input.gI().createFormSenditem1(player);
        });

        parameterizedCommands.put("n", (player, text) -> {
            String arg = text.substring(1).trim();
            if (!arg.matches("^\\d+$")) {
                Service.gI().chat(player, text);
                return;
            }
            try {
                int idTask = Integer.parseInt(arg);
                player.playerTask.taskMain.id = idTask - 1;
                player.playerTask.taskMain.index = 0;
                TaskService.gI().sendNextTaskMain(player);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai định dạng task ID!");
            }
        });
        // dmg <value>
        parameterizedCommands.put("dm", (player, text) -> {
            String arg = text.substring(2).trim();
            if (!arg.matches("^\\d+$")) {
                Service.gI().chat(player, text);
                return;
            }
            try {
                int dmg = Integer.parseInt(arg);
                player.nPoint.dameg = dmg;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET DAMAGE = " + dmg);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: dmg <số>");
            }
        });

        // hpg <value>
        parameterizedCommands.put("hp", (player, text) -> {
            String arg = text.substring(2).trim();
            if (!arg.matches("^\\d+$")) {
                Service.gI().chat(player, text);
                return;
            }
            try {
                int hpg = Integer.parseInt(arg);
                player.nPoint.hpg = hpg;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET HP GỐC = " + hpg);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: hpg <số>");
            }
        });

        // ki <value>
        parameterizedCommands.put("ki", (player, text) -> {
            String arg = text.substring(2).trim();
            if (!arg.matches("^\\d+$")) {
                Service.gI().chat(player, text);
                return;
            }
            try {
                int ki = Integer.parseInt(arg);
                player.nPoint.mpg = ki;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET KI GỐC = " + ki);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: ki <số>");
            }
        });
        // upp <power> (đặt trước "up" để không bị "up" bắt nhầm prefix)
        parameterizedCommands.put("upp", (player, text) -> {
            String arg = text.substring(3).trim();
            if (!arg.matches("^-?\\d+$")) {
                Service.gI().chat(player, text);
                return;
            }
            try {
                if (player.pet == null) {
                    Service.gI().sendThongBao(player, "Bạn chưa có đệ tử");
                    return;
                }

                long power = Long.parseLong(arg);
                Service.gI().addSMTN(player.pet, (byte) 2, power, false);
                Service.gI().sendThongBao(
                    player,
                    "UP TNSM cho đệ tử = " + power
                );
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: upp <số>");
            }
        });
        // up <power>
        parameterizedCommands.put("up", (player, text) -> {
            String arg = text.substring(2).trim();
            if (!arg.matches("^-?\\d+$")) {
                Service.gI().chat(player, text);
                return;
            }
            try {
                long power = Long.parseLong(arg);
                Service.gI().addSMTN(player, (byte) 2, power, false);
                Service.gI().sendThongBao(player, "UP SMTN = " + power);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: up <số>");
            }
        });
        parameterizedCommands.put("henbaotri", (player, text) -> {
            try {
                String arg = text.replace("henbaotri", "").trim();
                String[] parts = arg.split(":");
                int h = Integer.parseInt(parts[0].trim());
                int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                AutoMaintenance.hours = h;
                AutoMaintenance.mins = m;
                AutoMaintenance.AutoMaintenance = true;
                Service.gI().sendThongBao(player, String.format("Đã hẹn bảo trì tự động hằng ngày lúc %02d:%02d", h, m));
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp! Dùng: henbaotri <giờ:phút> (VD: henbaotri 05:00)");
            }
        });


        parameterizedCommands.put("dctt", (player, text) -> {
            String arg = text.substring(4).trim();
            if (!arg.isEmpty() && !arg.matches("^\\d+$")) {
                Service.gI().chat(player, text);
                return;
            }
            try {
                short param = arg.isEmpty() ? (short) 145 : Short.parseShort(arg);
                nro.models.intrinsic.Intrinsic intrinsic = nro.models.services.IntrinsicService.gI().getIntrinsicById(6);
                if (intrinsic != null) {
                    player.playerIntrinsic.intrinsic = intrinsic;
                    player.playerIntrinsic.intrinsic.param1 = param;
                    nro.models.services.IntrinsicService.gI().sendInfoIntrinsic(player);
                    Service.gI().sendThongBao(player, "Đã cài Nội tại Dịch Chuyển Tức Thời +" + param + "%!");
                }
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp! Dùng: dctt <phần_trăm> (VD: dctt 145)");
            }
        });

        parameterizedCommands.put("nt ", (player, text) -> {
            try {
                String[] parts = text.trim().split("\\s+");
                int id = Integer.parseInt(parts[1]);
                short param1 = parts.length >= 3 ? Short.parseShort(parts[2]) : 150;
                short param2 = parts.length >= 4 ? Short.parseShort(parts[3]) : 0;
                nro.models.intrinsic.Intrinsic intrinsic = nro.models.services.IntrinsicService.gI().getIntrinsicById(id);
                if (intrinsic != null) {
                    player.playerIntrinsic.intrinsic = intrinsic;
                    player.playerIntrinsic.intrinsic.param1 = param1;
                    player.playerIntrinsic.intrinsic.param2 = param2;
                    nro.models.services.IntrinsicService.gI().sendInfoIntrinsic(player);
                    Service.gI().sendThongBao(player, "Đã cài Nội tại: " + intrinsic.getName());
                } else {
                    Service.gI().sendThongBao(player, "Không tìm thấy nội tại ID: " + id);
                }
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp! Dùng: nt <id> <chỉ_số> (VD: nt 6 145)");
            }
        });

        parameterizedCommands.put("i ", (player, text) -> {
            try {
                String[] split = text.split(" ");
                if (split.length < 2) {
                    Service.gI().sendThongBao(player, "Cú pháp: i <itemId> <số lượng> [option:value...]");
                    return;
                }

                int itemId = Integer.parseInt(split[1]);
                int quantity = split.length >= 3 ? Integer.parseInt(split[2]) : 1;

                List<Item.ItemOption> customOptions = new ArrayList<>();
                for (int i = 3; i < split.length; i++) {
                    if (split[i].contains(":")) {
                        String[] optSplit = split[i].split(":");
                        int optionId = Integer.parseInt(optSplit[0]);
                        int optionValue = Integer.parseInt(optSplit[1]);
                        customOptions.add(new Item.ItemOption(optionId, optionValue));
                    }
                }

                for (int i = 0; i < quantity; i++) {
                    Item item = ItemService.gI().createNewItem((short) itemId);
                    if (!customOptions.isEmpty()) {
                        item.itemOptions = new ArrayList<>(customOptions);
                    } else {
                        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
                        if (!ops.isEmpty()) {
                            item.itemOptions = ops;
                        }
                    }
                    InventoryService.gI().addItemBag(player, item);
                }

                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "GET " + quantity + " x " + ItemService.gI().getTemplate(itemId).name + " [" + itemId + "] SUCCESS!");

            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi cú pháp! Dùng: i <itemId> <số lượng> [optionId:value]");
            }
        });
    }

    public void chat(Player player, String text) {
        String cleanedText = text.trim();
        if (cleanedText.isEmpty()) {
            return;
        }
        if (!check(player, cleanedText)) {
            Service.gI().chat(player, cleanedText);
        }
    }

    public boolean check(Player player, String text) {
        if (player.isAdmin()) {
            if (adminCommands.containsKey(text)) {
                adminCommands.get(text).accept(player);
                return true;
            }

            for (Map.Entry<String, BiConsumer<Player, String>> entry : parameterizedCommands.entrySet()) {
                if (text.startsWith(entry.getKey())) {
                    entry.getValue().accept(player, text);
                    return true;
                }
            }
        }

        if (text.startsWith("ten con la ")) {
            PetService.gI().changeNamePet(player, text.replace("ten con la ", ""));
            return true;
        }

        String lowerText = text.toLowerCase().trim();
        if (lowerText.matches("^(tab\\s*\\d+|t\\d+)$")) {
            try {
                int tabNum = Integer.parseInt(lowerText.replaceAll("[^0-9]", ""));
                int totalTabs = player.inventory.getTotalBagTabs();
                if (tabNum >= 1 && tabNum <= totalTabs) {
                    player.inventory.switchBagTab(tabNum - 1);
                    InventoryService.gI().sendItemBags(player);
                    int startSlot = (tabNum - 1) * nro.models.player.Inventory.BAG_TAB_SIZE + 1;
                    int endSlot = Math.min(player.inventory.itemsBag.size(), tabNum * nro.models.player.Inventory.BAG_TAB_SIZE);
                    Service.gI().sendThongBao(player, "Đã chuyển sang Hành trang Tab " + tabNum + "/" + totalTabs + " (Ô " + startSlot + " - " + endSlot + ")!");
                } else {
                    Service.gI().sendThongBao(player, "Bạn hiện có " + totalTabs + " Tab Hành trang (Gõ 'tab 1' đến 'tab " + totalTabs + "')!");
                }
            } catch (Exception ignored) {
            }
            return true;
        }

        switch (lowerText) {
            case "tab", "tui", "tuidong", "hanhtrang" -> {
                nro.models.npc.NpcFactory.openMenuBagTab(player);
                return true;
            }
            case "autodau", "ad", "buffdau", "auto dau", "buff dau" -> {
                player.isAutoBuffPea = !player.isAutoBuffPea;
                Service.gI().sendThongBao(player, "Auto Buff Đậu (Sư phụ & Đệ tử): " + (player.isAutoBuffPea ? "[Bật]" : "[Tắt]"));
                return true;
            }
            case "treoquai", "tq", "autoban", "chongday", "upquai" -> {
                player.isAutoCleanBag = !player.isAutoCleanBag;
                if (player.isAutoCleanBag) {
                    player.autoCleanTrashAndSellItems();
                }
                Service.gI().sendThongBao(player, "Up treo quái (Chống đầy hành trang & Tự bán đồ rác): " + (player.isAutoCleanBag ? "[Bật]" : "[Tắt]"));
                return true;
            }
            case "boss", "dsboss", "sanboss" -> {
                ServerNotify.gI().sendActiveBossesToPlayer(player);
                nro.models.boss.Boss_Manager.BossManager.gI().showListBoss(player);
                return true;
            }
            case "auto", "menu", "tienich", "caidat", "hotro" -> {
                nro.models.npc.NpcFactory.openMenuTienIch(player);
                return true;
            }
        }

        if (player.pet != null) {
            switch (text) {
                case "di theo", "follow" ->
                    player.pet.changeStatus(Pet.FOLLOW);
                case "bao ve", "protect" ->
                    player.pet.changeStatus(Pet.PROTECT);
                case "tan cong", "attack" ->
                    player.pet.changeStatus(Pet.ATTACK);
                case "ve nha", "go home" ->
                    player.pet.changeStatus(Pet.GOHOME);
                case "bien hinh" ->
                    player.pet.transform();
                case "sach tuyet ky" -> {
                    for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                        Item item = player.inventory.itemsBag.get(i);
                        if (item != null && item.isNotNullItem() && item.template.type == 25) {
                            if (player.pet.nPoint != null && player.pet.nPoint.power >= 1500000) {
                                Item old = InventoryService.gI().putItemBody(player.pet, item);
                                player.inventory.itemsBag.set(i, old);
                                InventoryService.gI().sendItemBags(player);
                                InventoryService.gI().sendItemBody(player);
                                Service.gI().Send_Caitrang(player.pet);
                                Service.gI().Send_Caitrang(player);
                                Service.gI().showInfoPet(player);
                                Service.gI().sendThongBao(player, "Đã dùng " + item.template.name + " cho đệ tử");
                            } else {
                                Service.gI().sendThongBaoOK(player, "Đệ tử cần đạt 1tr5 sức mạnh để trang bị.");
                            }
                            break;
                        }
                    }
                }
            }
        }
        return false;
    }
}
