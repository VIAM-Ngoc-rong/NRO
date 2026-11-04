package nro.models.server;

import nro.models.radar.OptionCard;
import nro.models.services.RadarService;
import nro.models.radar.RadarCard;
import nro.models.data.LocalManager;
import nro.models.consts.ConstPlayer;
import nro.models.consts.ConstMap;
import nro.models.data.DataGame;
import nro.models.database.ShopDAO;
import nro.models.player_system.Template.*;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import static nro.models.data.DataGame.MAP_MOUNT_NUM;
import nro.models.player_system.GiftCode;
import nro.models.managers.GiftCodeManager;
import nro.models.intrinsic.Intrinsic;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.map.WayPoint;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.shop.Shop;
import nro.models.skill.NClass;
import nro.models.skill.Skill;
import nro.models.task.SideTaskTemplate;
import nro.models.task.SubTaskMain;
import nro.models.task.TaskMain;
import nro.models.map.service.MapService;
import nro.models.utils.Logger;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nro.models.map.Zone;
import nro.models.matches.TOP;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import nro.models.npc.NonInteractiveNPC;
import nro.models.player.Player;
import nro.models.player_badges.BagesTemplate;
import nro.models.shop_ky_gui.ConsignItem;
import nro.models.shop_ky_gui.ConsignShopManager;
import nro.models.task.BadgesTaskTemplate;
import nro.models.task.ClanTaskTemplate;
import nro.models.utils.FileIO;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public final class Manager {

    private static Manager instance;
    public static long timeRealTop = 0;
    public static byte SERVER = 1;
    public static byte SECOND_WAIT_LOGIN = 0;
    public static int MAX_PER_IP = 1000;
    public static int MAX_PLAYER = 2000;
    public static byte RATE_EXP_SERVER = 1;
    public static boolean LOCAL = false;
    public static boolean TEST = false;
    public static boolean DAO_AUTO_UPDATER = false;
    public static MapTemplate[] MAP_TEMPLATES;
    public static final List<nro.models.map.Map> MAPS = new ArrayList<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService executor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    public static final List<ItemOptionTemplate> ITEM_OPTION_TEMPLATES = new ArrayList<>();
    public static final List<ArrHead2Frames> ARR_HEAD_2_FRAMES = new ArrayList<>();
    public static final Map<String, Byte> IMAGES_BY_NAME = new HashMap<>();
    public static final List<ItemTemplate> ITEM_TEMPLATES = new ArrayList<>();
    public static final List<MobTemplate> MOB_TEMPLATES = new ArrayList<>();
    public static final List<NpcTemplate> NPC_TEMPLATES = new ArrayList<>();
    public static final List<TaskMain> TASKS = new ArrayList<>();
    public static final List<SideTaskTemplate> SIDE_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<ClanTaskTemplate> CLAN_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<AchievementTemplate> ACHIEVEMENT_TEMPLATE = new ArrayList<>();
    public static final List<Intrinsic> INTRINSICS = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_TD = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_NM = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_XD = new ArrayList<>();
    public static final List<HeadAvatar> HEAD_AVATARS = new ArrayList<>();
    public static final List<BgItem> BG_ITEMS = new ArrayList<>();
    public static final List<FlagBag> FLAGS_BAGS = new ArrayList<>();
    public static final List<NClass> NCLASS = new ArrayList<>();
    public static final List<Npc> NPCS = new ArrayList<>();
    public static List<Shop> SHOPS = new ArrayList<>();
    public static final List<Clan> CLANS = new ArrayList<>();
    public static final List<String> NOTIFY = new ArrayList<>();
    public static final List<BadgesTaskTemplate> TASKS_BADGES_TEMPLATE = new ArrayList<>();
    public static final List<BagesTemplate> BAGES_TEMPLATES = new ArrayList<>();
    public static final short[][] trangBiKichHoat = {{0, 6, 21, 27}, {1, 7, 22, 28}, {2, 8, 23, 29}};
    public static List<TOP> Topsukien;
    public static List<TOP> Topsukien1;
    public static List<TOP> Topsukien2 = new ArrayList<>();
    public static List<TOP> Topwhis;
    public static List<TOP> Topmaydam;
    public static final String queryTopmaydam = "SELECT id, point_maydam, total_damage_maydam FROM player ORDER BY point_maydam DESC LIMIT 100";
    public static final String queryTopsukien1 = "SELECT id, point_sukien1 FROM player ORDER BY point_sukien1 DESC LIMIT 100";
    public static final String queryTopsukien2 = "SELECT id, point_sukien2 FROM player ORDER BY point_sukien2 DESC LIMIT 100";
    public static final String queryTopwhis = "SELECT id, thachdauwhis FROM player ORDER BY thachdauwhis DESC LIMIT 100";
    public static final String queryTopsukien = "SELECT id, point_sukien FROM player ORDER BY point_sukien DESC LIMIT 100";
    public static boolean isTopMaydamChanged = false;
    public static boolean isTopSukienChanged = false;
    public static boolean isTopSukien1Changed = false;
    public static boolean isTopSukien2Changed = false;
    public static boolean isTopWhisChanged = false;

    public static Manager gI() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    public static boolean hasNewTopScores() {
        return isTopMaydamChanged || isTopSukien2Changed || isTopSukienChanged || isTopSukien1Changed || isTopWhisChanged;
    }

    public static void resetTopFlags() {
        isTopMaydamChanged = false;
        isTopSukienChanged = false;
        isTopSukien1Changed = false;
        isTopSukien2Changed = false;
        isTopWhisChanged = false;
    }

    public class MapBgDataManager {

        private static final Map<Integer, byte[]> cacheBgItem = new ConcurrentHashMap<>();

        public static byte[] getBgItem(int mapId) {
            return cacheBgItem.computeIfAbsent(mapId, id -> {
                try {
                    return FileIO.readFile("data/map/item_bg_map_data1/" + id);
                } catch (Exception e) {
                    return null;
                }
            });
        }
    }

    public static boolean isExactly11AM() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return hour == 8 && minute == 13;
    }

    private Manager() {
        try {
            loadProperties();

        } catch (IOException ex) {
            Logger.logException(Manager.class, ex, "Lỗi load properites");
            System.exit(0);
        }

        this.loadDatabase();
        NpcFactory.createNpcConMeo();
        NpcFactory.createNpcRongThieng();
        this.initMap();
    }

    private void initMap() {
        int[][] tileTyleTop = readTileIndexTileType(ConstMap.TILE_TOP);
        for (MapTemplate mapTemp : MAP_TEMPLATES) {
            int[][] tileMap = readTileMap(mapTemp.id);
            int[] tileTop = tileTyleTop[mapTemp.tileId - 1];
            nro.models.map.Map map = new nro.models.map.Map(
                    mapTemp.id, mapTemp.name, mapTemp.planetId, mapTemp.tileId,
                    mapTemp.bgId, mapTemp.bgType, mapTemp.type, tileMap, tileTop,
                    mapTemp.zones, mapTemp.maxPlayerPerZone, mapTemp.wayPoints
            );
            MAPS.add(map);
            map.initMob(mapTemp.mobTemp, mapTemp.mobLevel, mapTemp.mobHp, mapTemp.mobX, mapTemp.mobY);
            map.initNpc(mapTemp.npcId, mapTemp.npcX, mapTemp.npcY);
        }
        new NonInteractiveNPC().initNonInteractiveNPC();

        final List<Callable<Void>> zoneTasks = new ArrayList<>();
        List<Zone> currentBatch = new ArrayList<>(16);
        for (nro.models.map.Map map : MAPS) {
            for (Zone zone : map.zones) {
                currentBatch.add(zone);
                if (currentBatch.size() >= 16) {
                    final List<Zone> finalBatch = new ArrayList<>(currentBatch);
                    zoneTasks.add(() -> {
                        for (Zone z : finalBatch) {
                            z.update();
                        }
                        return null;
                    });
                    currentBatch.clear();
                }
            }
        }
        if (!currentBatch.isEmpty()) {
            final List<Zone> finalBatch = new ArrayList<>(currentBatch);
            zoneTasks.add(() -> {
                for (Zone z : finalBatch) {
                    z.update();
                }
                return null;
            });
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
                for (Callable<Void> task : zoneTasks) {
                    completionService.submit(task);
                }
                for (int i = 0; i < zoneTasks.size(); i++) {
                    completionService.take();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Player> players = Client.gI().getPlayers();
                for (int i = 0; i < players.size(); i++) {
                    Player pl = players.get(i);
                    if (pl != null && pl.isPl() && !pl.isBot && pl.isAutoPlayActive()) {
                        pl.updateServerAutoAttack();
                    }
                }
            } catch (Exception ignored) {
            }
        }, 400, 400, TimeUnit.MILLISECONDS);

        Logger.success(Logger.RED + "Update maps thread started success!\n");
    }

    private static void ensureCustomParts(Connection con) {
        try (PreparedStatement psPart = con.prepareStatement(
                "INSERT INTO part (id, TYPE, DATA) VALUES "
                + "(2099, 0, '[[25010,3,-16],[2955,0,-16],[2955,0,-16]]'), "
                + "(2100, 1, '[[25011,-22,-37],[25011,-18,-44],[25013,-23,-45],[25014,-9,-45],[25012,-20,-42],[25013,-21,-44],[25014,-12,-44],[25013,-19,-36],[25014,-5,-32],[25017,-49,-30],[25015,-15,-42],[25016,-22,-40],[25012,-17,-33],[25016,-20,-43],[25015,-14,-43],[25017,-51,-40],[25018,-16,-37]]'), "
                + "(2101, 2, '[[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0],[2955,0,0]]') "
                + "ON DUPLICATE KEY UPDATE TYPE = VALUES(TYPE), DATA = VALUES(DATA)")) {
            psPart.executeUpdate();
        } catch (Exception e) {
            Logger.logException(Manager.class, e);
        }
    }

    public static void loadPart() {
        JSONValue jv = new JSONValue();
        JSONArray dataArray = null;
        JSONObject dataObject = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try (Connection con = LocalManager.getConnection();) {
            //load part
            ensureCustomParts(con);
            ps = con.prepareStatement("select * from part order by id asc");
            rs = ps.executeQuery();
            List<Part> parts = new ArrayList<>();
            while (rs.next()) {
                Part part = new Part();
                part.id = rs.getShort("id");
                part.type = rs.getByte("type");
                dataArray = (JSONArray) jv.parse(rs.getString("data").replaceAll("\\\"", ""));
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONArray pd = (JSONArray) jv.parse(String.valueOf(dataArray.get(j)));
                    part.partDetails.add(new PartDetail(Short.parseShort(String.valueOf(pd.get(0))),
                            Byte.parseByte(String.valueOf(pd.get(1))),
                            Byte.parseByte(String.valueOf(pd.get(2)))));
                    pd.clear();
                }
                parts.add(part);
                dataArray.clear();
            }
            DataOutputStream dos = new DataOutputStream(new FileOutputStream("data/update_data/part"));
            dos.writeShort(parts.size());
            for (Part part : parts) {
                dos.writeByte(part.type);
                for (PartDetail partDetail : part.partDetails) {
                    dos.writeShort(partDetail.iconId);
                    dos.writeByte(partDetail.dx);
                    dos.writeByte(partDetail.dy);
                }
            }
            dos.flush();
            dos.close();
        } catch (Exception e) {
            System.err.print("\nError at 299\n");
            e.printStackTrace();
        }
    }

    private void loadDatabase() {
        long st = System.currentTimeMillis();
        JSONArray dataArray;
        JSONObject dataObject;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try (Connection ConnectionDatabase = LocalManager.getConnection()) {
            //load part
            ensureCustomParts(ConnectionDatabase);
            ps = ConnectionDatabase.prepareStatement("select * from part order by id asc");
            rs = ps.executeQuery();
            List<Part> parts = new ArrayList<>();
            while (rs.next()) {
                Part part = new Part();
                part.id = rs.getShort("id");
                part.type = rs.getByte("type");
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data").replaceAll("\\\"", ""));
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONArray pd = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                    part.partDetails.add(new PartDetail(Short.parseShort(String.valueOf(pd.get(0))),
                            Byte.parseByte(String.valueOf(pd.get(1))),
                            Byte.parseByte(String.valueOf(pd.get(2)))));
                    pd.clear();
                }
                parts.add(part);
                dataArray.clear();
            }
            DataOutputStream dos = new DataOutputStream(new FileOutputStream("data/update_data/part"));
            dos.writeShort(parts.size());
            for (Part part : parts) {
                dos.writeByte(part.type);
                for (PartDetail partDetail : part.partDetails) {
                    dos.writeShort(partDetail.iconId);
                    dos.writeByte(partDetail.dx);
                    dos.writeByte(partDetail.dy);
                }
            }
            dos.flush();
            Logger.success(Logger.PURPLE + "Successfully loaded part (" + parts.size() + ")\n");

            //load bg item template
            ps = ConnectionDatabase.prepareStatement("select * from bg_item_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                BgItem bgItem = new BgItem();
                bgItem.id = rs.getInt("id");
                bgItem.layer = rs.getByte("layer");
                bgItem.dx = rs.getShort("dx");
                bgItem.dy = rs.getShort("dy");
                bgItem.idImage = rs.getShort("image_id");
                BG_ITEMS.add(bgItem);
            }
            Logger.success(Logger.RED + "Successfully loaded bg item template (" + BG_ITEMS.size() + ")\n");

            //load array head 2 frames
            ps = ConnectionDatabase.prepareStatement("select * from array_head_2_frames");
            rs = ps.executeQuery();
            while (rs.next()) {
                ArrHead2Frames arrHead2Frames = new ArrHead2Frames();
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data"));
                for (int i = 0; i < dataArray.size(); i++) {
                    arrHead2Frames.frames.add(Integer.valueOf(dataArray.get(i).toString()));
                }
                ARR_HEAD_2_FRAMES.add(arrHead2Frames);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded arr head 2 frames (" + ARR_HEAD_2_FRAMES.size() + ")\n");

            //load clan
            ps = ConnectionDatabase.prepareStatement("select * from clan");
            rs = ps.executeQuery();
            while (rs.next()) {
                Clan clan = new Clan();
                clan.id = rs.getInt("id");
                clan.name = rs.getString("name");
                clan.name2 = rs.getString("name_2");
                clan.slogan = rs.getString("slogan");
                clan.imgId = rs.getByte("img_id");
                clan.powerPoint = rs.getLong("power_point");
                clan.maxMember = rs.getByte("max_member");
                clan.capsuleClan = rs.getInt("clan_point");
                clan.level = rs.getByte("level");
                if (clan.level < 1) {
                    clan.level = 1;
                }
                clan.createTime = (int) (rs.getTimestamp("create_time").getTime() / 1000);
                dataArray = (JSONArray) JSONValue.parse(rs.getString("members"));
                for (int i = 0; i < dataArray.size(); i++) {
                    dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                    ClanMember cm = new ClanMember();
                    cm.clan = clan;
                    cm.id = Integer.parseInt(String.valueOf(dataObject.get("id")));
                    cm.name = String.valueOf(dataObject.get("name"));
                    cm.head = Short.parseShort(String.valueOf(dataObject.get("head")));
                    cm.body = Short.parseShort(String.valueOf(dataObject.get("body")));
                    cm.leg = Short.parseShort(String.valueOf(dataObject.get("leg")));
                    cm.role = Byte.parseByte(String.valueOf(dataObject.get("role")));
                    cm.donate = Integer.parseInt(String.valueOf(dataObject.get("donate")));
                    cm.receiveDonate = Integer.parseInt(String.valueOf(dataObject.get("receive_donate")));
                    cm.memberPoint = Integer.parseInt(String.valueOf(dataObject.get("member_point")));
                    cm.clanPoint = Integer.parseInt(String.valueOf(dataObject.get("clan_point")));
                    cm.joinTime = Integer.parseInt(String.valueOf(dataObject.get("join_time")));
                    cm.timeAskPea = Long.parseLong(String.valueOf(dataObject.get("ask_pea_time")));
                    try {
                        cm.powerPoint = Long.parseLong(String.valueOf(dataObject.get("power")));
                    } catch (NumberFormatException e) {
                    }
                    clan.addClanMember(cm);
                }
                dataArray = (JSONArray) JSONValue.parse(rs.getString("thanhTichBDKB"));
                if (!dataArray.isEmpty()) {
                    clan.levelDoneBanDoKhoBau = Integer.parseInt(String.valueOf(dataArray.get(0)));
                    clan.thoiGianHoanThanhBDKB = Long.parseLong(String.valueOf(dataArray.get(1)));
                }
                dataArray.clear();
                clan.loadNoiTaiFromJson(rs.getString("tops"));
                CLANS.add(clan);
            }

            ps = ConnectionDatabase.prepareStatement("select id from clan order by id desc limit 1");
            rs = ps.executeQuery();
            if (rs.first()) {
                Clan.NEXT_ID = rs.getInt("id") + 1;
            }

            Logger.success(Logger.RED + "Successfully loaded clan (" + CLANS.size() + "), clan next id: " + Clan.NEXT_ID + "\n");

            //load skill
            ps = ConnectionDatabase.prepareStatement("select * from skill_template order by nclass_id, slot");
            rs = ps.executeQuery();
            byte nClassId = -1;
            NClass nClass = null;
            while (rs.next()) {
                byte id = rs.getByte("nclass_id");
                if (id != nClassId) {
                    nClassId = id;
                    nClass = new NClass();
                    nClass.name = id == ConstPlayer.TRAI_DAT ? "Trái Đất" : id == ConstPlayer.NAMEC ? "Namếc" : "Xayda";
                    nClass.classId = nClassId;
                    NCLASS.add(nClass);
                }
                SkillTemplate skillTemplate = new SkillTemplate();
                skillTemplate.classId = nClassId;
                skillTemplate.id = rs.getByte("id");
                skillTemplate.name = rs.getString("name");
                skillTemplate.maxPoint = rs.getByte("max_point");
                skillTemplate.manaUseType = rs.getByte("mana_use_type");
                skillTemplate.type = rs.getByte("type");
                skillTemplate.iconId = rs.getShort("icon_id");
                skillTemplate.damInfo = rs.getString("dam_info");
                nClass.skillTemplatess.add(skillTemplate);

                dataArray = (JSONArray) JSONValue.parse(
                        rs.getString("skills")
                                .replaceAll("\\[\"", "[")
                                .replaceAll("\"\\[", "[")
                                .replaceAll("\"\\]", "]")
                                .replaceAll("\\]\"", "]")
                                .replaceAll("\\}\",\"\\{", "},{")
                );
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONObject dts = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(j)));
                    Skill skill = new Skill();
                    skill.template = skillTemplate;
                    skill.skillId = Short.parseShort(String.valueOf(dts.get("id")));
                    skill.point = Byte.parseByte(String.valueOf(dts.get("point")));
                    skill.powRequire = Long.parseLong(String.valueOf(dts.get("power_require")));
                    skill.manaUse = Integer.parseInt(String.valueOf(dts.get("mana_use")));
                    skill.coolDown = Integer.parseInt(String.valueOf(dts.get("cool_down")));
                    skill.dx = Integer.parseInt(String.valueOf(dts.get("dx")));
                    skill.dy = Integer.parseInt(String.valueOf(dts.get("dy")));
                    skill.maxFight = Integer.parseInt(String.valueOf(dts.get("max_fight")));
                    skill.damage = Short.parseShort(String.valueOf(dts.get("damage")));
                    skill.price = Short.parseShort(String.valueOf(dts.get("price")));
                    skill.moreInfo = String.valueOf(dts.get("info"));
                    skillTemplate.skillss.add(skill);
                }
            }
            Logger.success(Logger.PURPLE + "Successfully loaded skill (" + NCLASS.size() + ")\n");

            //load head avatar
            ps = ConnectionDatabase.prepareStatement("select * from head_avatar");
            rs = ps.executeQuery();
            while (rs.next()) {
                HeadAvatar headAvatar = new HeadAvatar(rs.getInt("head_id"), rs.getInt("avatar_id"));
                HEAD_AVATARS.add(headAvatar);
            }
            Logger.success(Logger.RED + "Successfully loaded head avatar (" + HEAD_AVATARS.size() + ")\n");

            //load flag bag
            ps = ConnectionDatabase.prepareStatement("select * from flag_bag");
            rs = ps.executeQuery();
            while (rs.next()) {
                FlagBag flagBag = new FlagBag();
                flagBag.id = rs.getInt("id");
                flagBag.name = rs.getString("name");
                flagBag.gold = rs.getInt("gold");
                flagBag.gem = rs.getInt("gem");
                flagBag.iconId = rs.getShort("icon_id");
                String rawIconData = rs.getString("icon_data");
                String[] iconData = (rawIconData != null && !rawIconData.trim().isEmpty())
                        ? rawIconData.split(",") : new String[0];
                List<Short> validEffects = new ArrayList<>();
                for (String s : iconData) {
                    try {
                        int val = Integer.parseInt(s.trim());
                        if (val != -1 && val != 0) {
                            validEffects.add((short) Math.abs(val));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (flagBag.iconId <= 0 && !validEffects.isEmpty()) {
                    flagBag.iconId = validEffects.get(0);
                }
                if (validEffects.isEmpty() && flagBag.iconId > 0) {
                    validEffects.add(flagBag.iconId);
                }
                flagBag.iconEffect = new short[validEffects.size()];
                for (int j = 0; j < validEffects.size(); j++) {
                    flagBag.iconEffect[j] = validEffects.get(j);
                }
                FLAGS_BAGS.add(flagBag);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded flag bag (" + FLAGS_BAGS.size() + ")\n");

            //load intrinsic
            ps = ConnectionDatabase.prepareStatement("select * from intrinsic order by id asc");
            rs = ps.executeQuery();
            java.util.Set<Integer> loadedIntrinsicIds = new java.util.HashSet<>();
            while (rs.next()) {
                Intrinsic intrinsic = new Intrinsic();
                intrinsic.id = rs.getByte("id");
                if (!loadedIntrinsicIds.add(intrinsic.id)) {
                    continue;
                }
                intrinsic.name = rs.getString("name");
                intrinsic.paramFrom1 = rs.getShort("param_from_1");
                intrinsic.paramTo1 = rs.getShort("param_to_1");
                intrinsic.paramFrom2 = rs.getShort("param_from_2");
                intrinsic.paramTo2 = rs.getShort("param_to_2");
                intrinsic.icon = rs.getShort("icon");
                intrinsic.gender = rs.getByte("gender");
                switch (intrinsic.gender) {
                    case ConstPlayer.TRAI_DAT ->
                        INTRINSIC_TD.add(intrinsic);
                    case ConstPlayer.NAMEC ->
                        INTRINSIC_NM.add(intrinsic);
                    case ConstPlayer.XAYDA ->
                        INTRINSIC_XD.add(intrinsic);
                    default -> {
                        INTRINSIC_TD.add(intrinsic);
                        INTRINSIC_NM.add(intrinsic);
                        INTRINSIC_XD.add(intrinsic);
                    }
                }
                INTRINSICS.add(intrinsic);
            }
            Logger.success(Logger.RED + "Successfully loaded intrinsic (" + INTRINSICS.size() + ")\n");

            // Tự động khởi tạo / cập nhật chuỗi Nhiệm vụ chính mở rộng (NV 29 -> NV 34 + mốc hoàn thành NV 35)
            ensureExtendedStoryTasks(ConnectionDatabase);

            //load task
            ps = ConnectionDatabase.prepareStatement("SELECT id, task_main_template.name, detail, "
                    + "task_sub_template.name AS 'sub_name', max_count, notify, npc_id, map "
                    + "FROM task_main_template JOIN task_sub_template ON task_main_template.id = "
                    + "task_sub_template.task_main_id ORDER BY task_main_template.id ASC, task_sub_template.ducvupro ASC");
            rs = ps.executeQuery();
            int taskId = -1;
            TaskMain task = null;
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id != taskId) {
                    taskId = id;
                    task = new TaskMain();
                    task.id = taskId;
                    task.name = rs.getString("name");
                    task.detail = rs.getString("detail");
                    TASKS.add(task);
                }
                SubTaskMain subTask = new SubTaskMain();
                subTask.name = rs.getString("sub_name");
                subTask.maxCount = rs.getShort("max_count");
                subTask.notify = rs.getString("notify");
                subTask.npcId = rs.getByte("npc_id");
                subTask.mapId = rs.getShort("map");
                task.subTasks.add(subTask);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded task (" + TASKS.size() + ")\n");

            //load side task
            ps = ConnectionDatabase.prepareStatement("select * from side_task_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                SideTaskTemplate sideTask = new SideTaskTemplate();
                sideTask.id = rs.getInt("id");
                sideTask.name = rs.getString("name");
                String[] mc1 = rs.getString("max_count_lv1").split("-");
                String[] mc2 = rs.getString("max_count_lv2").split("-");
                String[] mc3 = rs.getString("max_count_lv3").split("-");
                String[] mc4 = rs.getString("max_count_lv4").split("-");
                String[] mc5 = rs.getString("max_count_lv5").split("-");
                sideTask.count[0][0] = Integer.parseInt(mc1[0]);
                sideTask.count[0][1] = Integer.parseInt(mc1[1]);
                sideTask.count[1][0] = Integer.parseInt(mc2[0]);
                sideTask.count[1][1] = Integer.parseInt(mc2[1]);
                sideTask.count[2][0] = Integer.parseInt(mc3[0]);
                sideTask.count[2][1] = Integer.parseInt(mc3[1]);
                sideTask.count[3][0] = Integer.parseInt(mc4[0]);
                sideTask.count[3][1] = Integer.parseInt(mc4[1]);
                sideTask.count[4][0] = Integer.parseInt(mc5[0]);
                sideTask.count[4][1] = Integer.parseInt(mc5[1]);
                SIDE_TASKS_TEMPLATE.add(sideTask);
            }
            Logger.success(Logger.RED + "Successfully loaded side task (" + SIDE_TASKS_TEMPLATE.size() + ")\n");

            // load task badges
            ps = ConnectionDatabase.prepareStatement("select * from task_badges_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                BadgesTaskTemplate badgesTaskTemplate = new BadgesTaskTemplate();
                badgesTaskTemplate.id = rs.getInt("id");
                badgesTaskTemplate.name = rs.getString("NAME");
                badgesTaskTemplate.count = rs.getInt("maxCount");
                badgesTaskTemplate.idbadgesReward = rs.getInt("idbadgesReward");
                TASKS_BADGES_TEMPLATE.add(badgesTaskTemplate);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded task badges (" + TASKS_BADGES_TEMPLATE.size() + ")\n");

            //load clan task
            ps = ConnectionDatabase.prepareStatement("select * from clan_task_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ClanTaskTemplate clanTask = new ClanTaskTemplate();
                clanTask.id = rs.getInt("id");
                clanTask.name = rs.getString("name");
                String[] mc1 = rs.getString("max_count_lv1").split("-");
                String[] mc2 = rs.getString("max_count_lv2").split("-");
                String[] mc3 = rs.getString("max_count_lv3").split("-");
                String[] mc4 = rs.getString("max_count_lv4").split("-");
                String[] mc5 = rs.getString("max_count_lv5").split("-");
                clanTask.count[0][0] = Integer.parseInt(mc1[0]);
                clanTask.count[0][1] = Integer.parseInt(mc1[1]);
                clanTask.count[1][0] = Integer.parseInt(mc2[0]);
                clanTask.count[1][1] = Integer.parseInt(mc2[1]);
                clanTask.count[2][0] = Integer.parseInt(mc3[0]);
                clanTask.count[2][1] = Integer.parseInt(mc3[1]);
                clanTask.count[3][0] = Integer.parseInt(mc4[0]);
                clanTask.count[3][1] = Integer.parseInt(mc4[1]);
                clanTask.count[4][0] = Integer.parseInt(mc5[0]);
                clanTask.count[4][1] = Integer.parseInt(mc5[1]);
                CLAN_TASKS_TEMPLATE.add(clanTask);
            }
            Logger.success(Logger.RED + "Successfully loaded clan task (" + CLAN_TASKS_TEMPLATE.size() + ")\n");

            // Auto-migrate account.ruong_skh (Mặc định mỗi người chơi có 5 Rương SKH)
            try (PreparedStatement psAlterSkh = ConnectionDatabase.prepareStatement(
                    "ALTER TABLE account ADD COLUMN ruong_skh INT NOT NULL DEFAULT 5")) {
                psAlterSkh.executeUpdate();
            } catch (Exception ignored) {
            }
            try (PreparedStatement psAlterEmail = ConnectionDatabase.prepareStatement(
                    "ALTER TABLE account MODIFY COLUMN email VARCHAR(255) NOT NULL DEFAULT ''")) {
                psAlterEmail.executeUpdate();
            } catch (Exception ignored) {
            }
            try (PreparedStatement psAlterMaxDame = ConnectionDatabase.prepareStatement(
                    "ALTER TABLE player ADD COLUMN max_dame_hit BIGINT NOT NULL DEFAULT 0")) {
                psAlterMaxDame.executeUpdate();
            } catch (Exception ignored) {
            }
            try (PreparedStatement psAlterBag = ConnectionDatabase.prepareStatement(
                    "ALTER TABLE player MODIFY COLUMN items_bag LONGTEXT NOT NULL")) {
                psAlterBag.executeUpdate();
            } catch (Exception ignored) {
            }

            // Điều chỉnh phần thưởng Thành Tích Bò Mộng hợp lý (Thưởng bằng Thỏi Vàng)
            int[][] balancedAchievements = {
                {1, 5},    // Gia nhập Vệ Binh (340k SM) -> 5 Thỏi vàng
                {2, 10},   // Sức mạnh siêu cấp (1.5Tr SM) -> 10 Thỏi vàng
                {3, 10},   // Nông dân chăm chỉ (Đậu thần cấp 5) -> 10 Thỏi vàng
                {4, 20},   // Trăm trận trăm thắng (Thắng 100 người) -> 20 Thỏi vàng
                {5, 10},   // Nội công cao cường (Chưởng 2000 phát) -> 10 Thỏi vàng
                {6, 10},   // Khinh công thành thạo (Bay 20000m) -> 10 Thỏi vàng
                {7, 15},   // Thợ săn thiện xạ (Hạ 1000 quái bay) -> 15 Thỏi vàng
                {8, 10},   // Tập luyện bài bản (Hạ 1000 người rơm) -> 10 Thỏi vàng
                {9, 30},   // Hoạt động chăm chỉ (Online 120 giờ) -> 30 Thỏi vàng
                {10, 30},  // Hỗ trợ đồng đội (Cho 10000 đậu) -> 30 Thỏi vàng
                {11, 15},  // Trùm nhặt ve chai (Bán 200 đồ) -> 15 Thỏi vàng
                {12, 20},  // Lần đầu nạp ngọc (150 ngọc) -> 20 Thỏi vàng
                {13, 20},  // Đánh bại siêu quái (Hạ 100 siêu quái) -> 20 Thỏi vàng
                {14, 20},  // Thánh hồi sinh (Hồi sinh 200 lần) -> 20 Thỏi vàng
                {15, 20},  // Kỹ năng thành thạo (Dùng chiêu đặc biệt 1000 lần) -> 20 Thỏi vàng
                {16, 20},  // Trùm nhặt ngọc (Nhặt 1000 ngọc) -> 20 Thỏi vàng
                {17, 15},  // Đạt 15 triệu sức mạnh -> 15 Thỏi vàng
                {18, 100}, // Tuyệt kỹ thành thạo (Dùng tuyệt kỹ 7749 lần) -> 100 Thỏi vàng
                {19, 30},  // Chăm sóc đặc biệt (Được Namek hồi sinh 2000 lần) -> 30 Thỏi vàng
                {20, 100}  // Trùm kết liễu Boss (Hạ 2000 Boss) -> 100 Thỏi vàng
            };
            try (PreparedStatement psUpdAch = ConnectionDatabase.prepareStatement(
                    "UPDATE achievement_template SET money = ? WHERE id = ?")) {
                for (int[] pair : balancedAchievements) {
                    psUpdAch.setInt(1, pair[1]);
                    psUpdAch.setInt(2, pair[0]);
                    psUpdAch.addBatch();
                }
                psUpdAch.executeBatch();
            } catch (Exception ignored) {
            }

            //load achievement template
            ps = ConnectionDatabase.prepareStatement("select * from achievement_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ACHIEVEMENT_TEMPLATE.add(new AchievementTemplate(rs.getString("info1"), rs.getString("info2"), rs.getInt("money"), rs.getLong("max_count")));
            }
            Logger.success(Logger.PURPLE + "Successfully loaded achievement (" + ACHIEVEMENT_TEMPLATE.size() + ")\n");

            int batchSize = 750;
            int offset = 0;

            try (PreparedStatement psUpdateEgg = ConnectionDatabase.prepareStatement(
                    "UPDATE item_template SET TYPE = 27, gender = 3, NAME = 'Quả Trứng Broly', "
                    + "description = 'Sử dụng để ấp Đệ tử Broly tại nhà (Hợp thể +30% chỉ số)', "
                    + "level = 1, icon_id = 15128, part = -1, is_up_to_up = 0 WHERE id = 1826")) {
                psUpdateEgg.executeUpdate();
            } catch (Exception ignored) {
            }

            try (PreparedStatement psUpdateHitEgg = ConnectionDatabase.prepareStatement(
                    "UPDATE item_template SET TYPE = 27, gender = 3, NAME = 'Quả Trứng Hit', "
                    + "description = 'Ấp tại nhà nở Đệ tử Cấp 4 Sát Thủ Hit (Hợp thể +45% chỉ số)', "
                    + "level = 1, icon_id = 25008, part = -1, is_up_to_up = 0 WHERE id = 1834")) {
                psUpdateHitEgg.executeUpdate();
            } catch (Exception ignored) {
            }

            try (PreparedStatement psUpdateBagSwitch = ConnectionDatabase.prepareStatement(
                    "UPDATE item_template SET TYPE = 27, gender = 3, NAME = 'Chuyển Trang Hành Trang', "
                    + "description = 'Bấm Sử dụng để chuyển sang Trang Hành Trang khác hoặc mở thêm Trang', level = 1, icon_id = 4340, part = -1, is_up_to_up = 0 WHERE id = 1998")) {
                psUpdateBagSwitch.executeUpdate();
            } catch (Exception ignored) {
            }

            try (PreparedStatement psUpdateNameCard = ConnectionDatabase.prepareStatement(
                    "UPDATE item_template SET TYPE = 27, gender = 3, NAME = 'Thẻ đổi tên', "
                    + "description = 'Sử dụng để đổi tên nhân vật', level = 1, icon_id = 4030, part = -1, is_up_to_up = 1 WHERE id IN (1999, 2006)")) {
                psUpdateNameCard.executeUpdate();
            } catch (Exception ignored) {
            }

            try (PreparedStatement psUpdateAutoBossItem = ConnectionDatabase.prepareStatement(
                    "UPDATE item_template SET TYPE = 29, gender = 3, NAME = 'Máy Dò Săn Boss', "
                    + "description = 'Sử dụng để mở khóa Tự Động Săn Boss trong 60 phút (cộng dồn thời gian khi dùng nhiều lần)', "
                    + "level = 1, icon_id = 12834, part = -1, is_up_to_up = 1 WHERE id = 1115")) {
                psUpdateAutoBossItem.executeUpdate();
            } catch (Exception ignored) {
            }

            try (PreparedStatement psUpdateStackable = ConnectionDatabase.prepareStatement(
                    "UPDATE item_template SET is_up_to_up = 1 WHERE id IN (933, 934, 935, 1115, 1655, 1664, 1665, 1666, 1671, 1672, 1775, 1820, 1823, 1999, 2006) "
                    + "OR TYPE IN (6, 12, 14, 29, 31, 33)")) {
                psUpdateStackable.executeUpdate();
            } catch (Exception ignored) {
            }

            try (PreparedStatement psFixCostumeParts = ConnectionDatabase.prepareStatement(
                    "UPDATE item_template SET "
                    + "head = CASE id WHEN 1593 THEN 1467 WHEN 1040 THEN 1092 WHEN 1596 THEN 1470 WHEN 1597 THEN 1473 ELSE head END, "
                    + "body = CASE id WHEN 1593 THEN 1450 WHEN 1040 THEN 1093 WHEN 1596 THEN 1471 WHEN 1597 THEN 1474 ELSE body END, "
                    + "leg  = CASE id WHEN 1593 THEN 1451 WHEN 1040 THEN 1094 WHEN 1596 THEN 1472 WHEN 1597 THEN 1475 ELSE leg  END "
                    + "WHERE id IN (1040, 1593, 1596, 1597)")) {
                psFixCostumeParts.executeUpdate();
            } catch (Exception ignored) {
            }

            ensurePurpleDragonBallIcons();
            for (int star = 1; star <= 7; star++) {
                int itemId = 1826 + star; // 1827..1833
                int iconId = 25000 + star; // 25001..25007
                try (PreparedStatement psUpdateNRT = ConnectionDatabase.prepareStatement(
                        "UPDATE item_template SET TYPE = 12, gender = 3, NAME = ?, "
                        + "description = 'Thu thập đủ 7 viên để gọi Rồng Thần Tím hoặc ép vào trang bị', "
                        + "level = 0, icon_id = ?, part = -1, is_up_to_up = 1 WHERE id = ?")) {
                    psUpdateNRT.setString(1, "Ngọc Rồng Tím " + star + " sao");
                    psUpdateNRT.setInt(2, iconId);
                    psUpdateNRT.setInt(3, itemId);
                    psUpdateNRT.executeUpdate();
                } catch (Exception ignored) {
                }
            }

            try {
                while (true) {
                    ps = ConnectionDatabase.prepareStatement("SELECT * FROM item_template LIMIT ? OFFSET ?");
                    ps.setInt(1, batchSize);
                    ps.setInt(2, offset);
                    rs = ps.executeQuery();
                    if (!rs.next()) {
                        break;
                    }
                    do {
                        ItemTemplate itemTemp = new ItemTemplate();
                        itemTemp.id = rs.getShort("id");
                        itemTemp.type = rs.getByte("type");
                        itemTemp.gender = rs.getByte("gender");
                        itemTemp.name = rs.getString("name");
                        itemTemp.description = rs.getString("description");
                        itemTemp.level = rs.getByte("level");
                        itemTemp.iconID = rs.getShort("icon_id");
                        itemTemp.part = rs.getShort("part");
                        if (itemTemp.id == 78 && itemTemp.part > 255) {
                            itemTemp.part = 28;
                        } else if (itemTemp.id == 1706 && itemTemp.part > 255) {
                            itemTemp.part = 143;
                        }
                        itemTemp.isUpToUp = rs.getBoolean("is_up_to_up");
                        if (itemTemp.id == 1115) {
                            itemTemp.type = 29;
                            itemTemp.gender = 3;
                            itemTemp.name = "Máy Dò Săn Boss";
                            itemTemp.description = "Sử dụng để mở khóa Tự Động Săn Boss trong 60 phút (cộng dồn thời gian khi dùng nhiều lần)";
                            itemTemp.level = 1;
                            itemTemp.iconID = 12834;
                            itemTemp.part = -1;
                            itemTemp.isUpToUp = true;
                        } else if (itemTemp.id == 1826) {
                            itemTemp.type = 27;
                            itemTemp.gender = 3;
                            itemTemp.name = "Quả Trứng Broly";
                            itemTemp.description = "Ấp tại nhà nở Đệ tử Cấp 3 Broly (Hợp thể +30% chỉ số)";
                            itemTemp.level = 1;
                            itemTemp.iconID = 15128;
                            itemTemp.part = -1;
                            itemTemp.isUpToUp = false;
                        } else if (itemTemp.id >= 1827 && itemTemp.id <= 1833) {
                            int star = itemTemp.id - 1826;
                            itemTemp.type = 12;
                            itemTemp.gender = 3;
                            itemTemp.name = "Ngọc Rồng Tím " + star + " sao";
                            itemTemp.description = "Thu thập đủ 7 viên để gọi Rồng Thần Tím hoặc ép vào trang bị";
                            itemTemp.level = 0;
                            itemTemp.iconID = (short) (25000 + star);
                            itemTemp.part = -1;
                            itemTemp.isUpToUp = true;
                        } else if (itemTemp.id == 1834) {
                            itemTemp.type = 27;
                            itemTemp.gender = 3;
                            itemTemp.name = "Quả Trứng Hit";
                            itemTemp.description = "Ấp tại nhà nở Đệ tử Cấp 4 Sát Thủ Hit (Hợp thể +45% chỉ số)";
                            itemTemp.level = 1;
                            itemTemp.iconID = 25008;
                            itemTemp.part = -1;
                            itemTemp.isUpToUp = false;
                        }
                        if (itemTemp.id == 1655 || itemTemp.id == 1664 || itemTemp.id == 1665 || itemTemp.id == 1666
                                || itemTemp.id == 1671 || itemTemp.id == 1672 || itemTemp.id == 1775 || itemTemp.id == 1823
                                || itemTemp.id == 1045 || itemTemp.id == 1608 || itemTemp.id == 1609 || itemTemp.id == 1115
                                || itemTemp.type == 6 || itemTemp.type == 12 || itemTemp.type == 14
                                || itemTemp.type == 29 || itemTemp.type == 31 || itemTemp.type == 33) {
                            itemTemp.isUpToUp = true;
                        }
                        itemTemp.strRequire = rs.getInt("power_require");
                        itemTemp.gold = rs.getInt("gold");
                        itemTemp.gem = rs.getInt("gem");
                        itemTemp.head = rs.getInt("head");
                        itemTemp.body = rs.getInt("body");
                        itemTemp.leg = rs.getInt("leg");
                        if (itemTemp.id == 1593) {
                            itemTemp.head = 1467;
                            itemTemp.body = 1450;
                            itemTemp.leg = 1451;
                        } else if (itemTemp.id == 1040) {
                            itemTemp.head = 1092;
                            itemTemp.body = 1093;
                            itemTemp.leg = 1094;
                        } else if (itemTemp.id == 1596) {
                            itemTemp.head = 1470;
                            itemTemp.body = 1471;
                            itemTemp.leg = 1472;
                        } else if (itemTemp.id == 1597) {
                            itemTemp.head = 1473;
                            itemTemp.body = 1474;
                            itemTemp.leg = 1475;
                        }

                        ITEM_TEMPLATES.add(itemTemp);
                    } while (rs.next());
                    offset += batchSize;
                }

                for (ItemTemplate itemTemp : ITEM_TEMPLATES) {
                    if (itemTemp != null && itemTemp.type == 11 && itemTemp.part >= 0 && itemTemp.part <= 255 && itemTemp.iconID > 0) {
                        boolean exists = false;
                        for (FlagBag fb : FLAGS_BAGS) {
                            if (fb.id == itemTemp.part) {
                                exists = true;
                                if (fb.iconId <= 0) {
                                    fb.iconId = itemTemp.iconID;
                                }
                                if (fb.iconEffect == null || fb.iconEffect.length == 0) {
                                    fb.iconEffect = new short[]{fb.iconId};
                                }
                                break;
                            }
                        }
                        if (!exists) {
                            FlagBag autoFb = new FlagBag();
                            autoFb.id = itemTemp.part;
                            autoFb.name = itemTemp.name != null ? itemTemp.name : ("Đeo lưng " + itemTemp.id);
                            autoFb.gold = 0;
                            autoFb.gem = 0;
                            autoFb.iconId = itemTemp.iconID;
                            autoFb.iconEffect = new short[]{itemTemp.iconID};
                            FLAGS_BAGS.add(autoFb);
                        }
                    }
                }

                // Tự động đăng ký mọi headId (Cải trang, Part type=0, Hợp thể Porata 1/2/3, Gogeta, Đệ tử...)
                // vào HEAD_AVATARS để client không bao giờ nhận getAvatar(headId) == -1 gây IndexOutOfRangeException ở Panel.paintTopInfo
                java.util.Set<Integer> registeredHeads = new java.util.HashSet<>();
                for (HeadAvatar ha : HEAD_AVATARS) {
                    if (ha.headId == 520) {
                        ha.avatarId = 8127;
                    } else if (ha.avatarId <= 0 || ha.avatarId >= 32767) {
                        ha.avatarId = 522;
                    }
                    registeredHeads.add(ha.headId);
                }
                for (ItemTemplate itemTemp : ITEM_TEMPLATES) {
                    if (itemTemp != null && itemTemp.head >= 0 && itemTemp.iconID > 0 && itemTemp.iconID < 32767) {
                        if (registeredHeads.add(itemTemp.head)) {
                            HEAD_AVATARS.add(new HeadAvatar(itemTemp.head, itemTemp.iconID));
                        }
                    }
                }
                for (Part part : parts) {
                    if (part != null && part.type == 0 && part.id >= 0) {
                        if (registeredHeads.add((int) part.id)) {
                            int iconId = (!part.partDetails.isEmpty() && part.partDetails.get(0).iconId > 0 && part.partDetails.get(0).iconId < 32767)
                                    ? part.partDetails.get(0).iconId : 522;
                            HEAD_AVATARS.add(new HeadAvatar(part.id, iconId));
                        }
                    }
                }
                if (registeredHeads.add(520)) {
                    HEAD_AVATARS.add(new HeadAvatar(520, 8127));
                }
                int[] specialHeads = {
                    192, 195, 196, 197, 198, 199, 200,
                    282, 285, 288, 297, 303, 304, 305,
                    380, 383, 391, 412, 454, 520, 545, 546, 547,
                    651, 654, 760, 867, 870, 873, 876, 946,
                    1080, 1083, 1218, 1422, 1578, 1743,
                    1856, 1861, 1866, 2082, 2083, 2084, 2100
                };
                for (int sh : specialHeads) {
                    if (registeredHeads.add(sh)) {
                        HEAD_AVATARS.add(new HeadAvatar(sh, 522));
                    }
                }

                Logger.success(Logger.RED + "Successfully loaded map item template (" + ITEM_TEMPLATES.size() + " items)\n");

            } catch (SQLException e) {
                Logger.error("Error loading item templates: " + e.getMessage());
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                } catch (SQLException e) {
                    Logger.error("Error closing resources: " + e.getMessage());
                }
            }

            //load item option template
            try (PreparedStatement psUpdateOpt = ConnectionDatabase.prepareStatement(
                    "UPDATE item_option_template SET NAME = CASE id "
                    + "WHEN 66 THEN '+#% sát thương chiêu Laze' "
                    + "WHEN 73 THEN '' "
                    + "WHEN 153 THEN '+#% sát thương chiêu phát nổ' "
                    + "WHEN 159 THEN '+#% sát thương đòn chưởng' "
                    + "WHEN 210 THEN '# dòng chỉ số bí ẩn' "
                    + "WHEN 237 THEN 'Set Nail chiến binh Namếc (+12% HP/Giáp, +8% SĐ mỗi món)' "
                    + "WHEN 238 THEN '$[2] +24% HP/Giáp, +16% SĐ, +15% SĐ chí mạng' "
                    + "WHEN 239 THEN '$[4] +48% HP/Giáp, +32% SĐ, +20% SĐCM, -25% hồi & +50% SĐ Masenko' "
                    + "WHEN 240 THEN '$[5] +80% HP/Giáp, +50% SĐ, +30% SĐCM, -50% hồi & +100% SĐ Masenko' "
                    + "WHEN 241 THEN 'Set Cađic M (+10% SĐ, +8% HP/Giáp mỗi món)' "
                    + "WHEN 242 THEN '$[2] +20% SĐ, +16% HP/Giáp, +10% SĐCM, Tăng phạm vi phát nổ' "
                    + "WHEN 243 THEN '$[4] +40% SĐ, +32% HP/Giáp, +15% SĐCM, +15% SĐ phát nổ' "
                    + "WHEN 244 THEN '$[5] +60% SĐ, +50% HP/Giáp, +25% SĐCM, +30% SĐ phát nổ' "
                    + "WHEN 245 THEN 'Set Thần Vũ Trụ Kaio (+10% SĐ/HP/Giáp, +4% CM mỗi món)' "
                    + "WHEN 246 THEN '$[2] +20% SĐ/HP/Giáp, +8% CM, +10% SĐ chí mạng' "
                    + "WHEN 247 THEN '$[4] +40% SĐ/HP/Giáp, +16% CM, +15% SĐCM, Giảm hao HP & +50% SĐ Kaioken' "
                    + "WHEN 248 THEN '$[5] +60% SĐ/HP/Giáp, +20% CM, +25% SĐCM, Giảm hao HP & +100% SĐ Kaioken' "
                    + "ELSE NAME END WHERE id IN (66, 73, 153, 159, 210, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248)")) {
                psUpdateOpt.executeUpdate();
            } catch (Exception ignored) {
            }
            ps = ConnectionDatabase.prepareStatement("select id, name from item_option_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ItemOptionTemplate optionTemp = new ItemOptionTemplate();
                optionTemp.id = rs.getInt("id");
                optionTemp.name = rs.getString("name");
                switch (optionTemp.id) {
                    case 39 -> optionTemp.name = "$[ĐÃ KHÓA BẢO VỆ - Chống vứt/bán/phân rã]";
                    case 66 -> optionTemp.name = "+#% sát thương chiêu Laze";
                    case 73 -> optionTemp.name = "";
                    case 138 -> optionTemp.name = "$+100% HP (đủ 5 món)";
                    case 142 -> optionTemp.name = "$+100% KI, +50% SĐ Laze (đủ 5 món)";
                    case 153 -> optionTemp.name = "+#% sát thương chiêu phát nổ";
                    case 159 -> optionTemp.name = "+#% sát thương đòn chưởng";
                    case 209 -> optionTemp.name = "$[Set Đồ Nhanh #]";
                    case 210 -> optionTemp.name = "# dòng chỉ số bí ẩn";
                    case 237 -> optionTemp.name = "Set Nail chiến binh Namếc (+12% HP/Giáp, +8% SĐ mỗi món)";
                    case 238 -> optionTemp.name = "$[2] +24% HP/Giáp, +16% SĐ, +15% SĐ chí mạng";
                    case 239 -> optionTemp.name = "$[4] +48% HP/Giáp, +32% SĐ, +20% SĐCM, -25% hồi & +50% SĐ Masenko";
                    case 240 -> optionTemp.name = "$[5] +80% HP/Giáp, +50% SĐ, +30% SĐCM, -50% hồi & +100% SĐ Masenko";
                    case 241 -> optionTemp.name = "Set Cađic M (+10% SĐ, +8% HP/Giáp mỗi món)";
                    case 242 -> optionTemp.name = "$[2] +20% SĐ, +16% HP/Giáp, +10% SĐCM, Tăng phạm vi phát nổ";
                    case 243 -> optionTemp.name = "$[4] +40% SĐ, +32% HP/Giáp, +15% SĐCM, +15% SĐ phát nổ";
                    case 244 -> optionTemp.name = "$[5] +60% SĐ, +50% HP/Giáp, +25% SĐCM, +30% SĐ phát nổ";
                    case 245 -> optionTemp.name = "Set Thần Vũ Trụ Kaio (+10% SĐ/HP/Giáp, +4% CM mỗi món)";
                    case 246 -> optionTemp.name = "$[2] +20% SĐ/HP/Giáp, +8% CM, +10% SĐ chí mạng";
                    case 247 -> optionTemp.name = "$[4] +40% SĐ/HP/Giáp, +16% CM, +15% SĐCM, Giảm hao HP & +50% SĐ Kaioken";
                    case 248 -> optionTemp.name = "$[5] +60% SĐ/HP/Giáp, +20% CM, +25% SĐCM, Giảm hao HP & +100% SĐ Kaioken";
                }
                ITEM_OPTION_TEMPLATES.add(optionTemp);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded map item option template (" + ITEM_OPTION_TEMPLATES.size() + ")\n");

            //load shop
            SHOPS = ShopDAO.getShops(ConnectionDatabase);
            Logger.success(Logger.RED + "Successfully loaded shop (" + SHOPS.size() + ")\n");

            //load notify
            ps = ConnectionDatabase.prepareStatement("select * from notify order by id desc");
            rs = ps.executeQuery();
            while (rs.next()) {
                NOTIFY.add(rs.getString("name") + "<>" + rs.getString("text"));
            }
            Logger.success(Logger.PURPLE + "Successfully loaded notify (" + NOTIFY.size() + ")\n");

            //load image by name
            ps = ConnectionDatabase.prepareStatement("select name, n_frame from img_by_name");
            rs = ps.executeQuery();
            while (rs.next()) {
                IMAGES_BY_NAME.put(rs.getString("name"), rs.getByte("n_frame"));
            }
            Logger.success(Logger.RED + "Successfully loaded images by name (" + IMAGES_BY_NAME.size() + ")\n");

            //Load mount
            for (ItemTemplate item : ITEM_TEMPLATES) {
                if (item.type == 23 && getNFrameImageByName("mount_" + item.part + "_0") != 0) {
                    MAP_MOUNT_NUM.put(item.id, (short) (item.part + 30000));
                }
            }
            Logger.success(Logger.PURPLE + "Successfully loaded mount (" + MAP_MOUNT_NUM.size() + ")\n");

            //Load item ki gui
            ps = ConnectionDatabase.prepareStatement("SELECT * FROM shop_ky_gui");
            rs = ps.executeQuery();
            while (rs.next()) {
                int i = rs.getInt("id");
                int idPl = rs.getInt("player_id");
                byte tab = rs.getByte("tab");
                short itemId = rs.getShort("item_id");
                int gold = rs.getInt("gold");
                int gem = rs.getInt("gem");
                int quantity = rs.getInt("quantity");
                byte isUp = rs.getByte("isUpTop");
                boolean isBuy = rs.getByte("isBuy") == 1;
                List<Item.ItemOption> op = new ArrayList<>();
                JSONArray jsa2 = (JSONArray) JSONValue.parse(rs.getString("itemOption"));
                for (int j = 0; j < jsa2.size(); ++j) {
                    JSONObject jso2 = (JSONObject) jsa2.get(j);
                    int idOptions = Integer.parseInt(jso2.get("id").toString());
                    int param = Integer.parseInt(jso2.get("param").toString());
                    op.add(new Item.ItemOption(idOptions, param));
                }
                ConsignShopManager.gI().listItem.add(new ConsignItem(i, itemId, idPl, tab, gold, gem, quantity, isUp, op, isBuy));
            }
            Logger.success(Logger.RED + "Successfully loaded Consign Item (" + ConsignShopManager.gI().listItem.size() + ")\n");

            //load mob template
            ensureVampaPlanetData(ConnectionDatabase);
            ps = ConnectionDatabase.prepareStatement("select * from mob_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                MobTemplate mobTemp = new MobTemplate();
                mobTemp.id = rs.getByte("id");
                mobTemp.type = rs.getByte("type");
                mobTemp.name = rs.getString("name");
                mobTemp.hp = rs.getInt("hp");
                mobTemp.rangeMove = rs.getByte("range_move");
                mobTemp.speed = rs.getByte("speed");
                mobTemp.dartType = rs.getByte("dart_type");
                mobTemp.percentDame = rs.getByte("percent_dame");
                mobTemp.percentTiemNang = rs.getByte("percent_tiem_nang");
                MOB_TEMPLATES.add(mobTemp);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded mob template (" + MOB_TEMPLATES.size() + ")\n");

            //load npc template
            ps = ConnectionDatabase.prepareStatement("select * from npc_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                NpcTemplate npcTemp = new NpcTemplate();
                npcTemp.id = rs.getByte("id");
                npcTemp.name = rs.getString("name");
                npcTemp.head = rs.getShort("head");
                npcTemp.body = rs.getShort("body");
                npcTemp.leg = rs.getShort("leg");
                npcTemp.avatar = rs.getInt("avatar");
                NPC_TEMPLATES.add(npcTemp);
            }
            Logger.success(Logger.RED + "Successfully loaded npc template (" + NPC_TEMPLATES.size() + ")\n");
            ps = ConnectionDatabase.prepareStatement("select * from data_badges");
            rs = ps.executeQuery();
            while (rs.next()) {
                BagesTemplate template = new BagesTemplate();
                template.id = rs.getInt("id");
                template.idEffect = rs.getInt("idEffect");
                template.idItem = rs.getInt("idItem");
                template.NAME = rs.getString("NAME");

                JSONArray option = (JSONArray) JSONValue.parse(rs.getString("Options"));;
                if (option != null) {
                    for (int u = 0; u < option.size(); u++) {
                        JSONObject jsonobject = (JSONObject) option.get(u);
                        int optionId = Integer.parseInt(jsonobject.get("id").toString());
                        int param = Integer.parseInt(jsonobject.get("param").toString());
                        template.options.add(new Item.ItemOption(optionId, param));
                    }
                }
                BAGES_TEMPLATES.add(template);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded badges template (" + BAGES_TEMPLATES.size() + ")\n");
            //load map template
            ps = ConnectionDatabase.prepareStatement("select count(id) from map_template");
            rs = ps.executeQuery();
            if (rs.first()) {
                int countRow = rs.getShort(1);
                MAP_TEMPLATES = new MapTemplate[countRow];
                ps = ConnectionDatabase.prepareStatement("select * from map_template");
                rs = ps.executeQuery();
                short i = 0;
                while (rs.next()) {
                    MapTemplate mapTemplate = new MapTemplate();
                    int mapId = rs.getInt("id");
                    String mapName = rs.getString("name");
                    mapTemplate.id = mapId;
                    mapTemplate.name = mapName;
                    mapTemplate.type = rs.getByte("type");
                    mapTemplate.planetId = rs.getByte("planet_id");
                    mapTemplate.bgType = rs.getByte("bg_type");
                    mapTemplate.tileId = rs.getByte("tile_id");
                    mapTemplate.bgId = rs.getByte("bg_id");
                    mapTemplate.zones = rs.getByte("zones");
                    mapTemplate.maxPlayerPerZone = rs.getByte("max_player");
                    //load waypoints
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("waypoints")
                            .replaceAll("\\[\"\\[", "[[")
                            .replaceAll("\\]\"\\]", "]]")
                            .replaceAll("\",\"", ",")
                    );
                    for (int j = 0; j < dataArray.size(); j++) {
                        WayPoint wp = new WayPoint();
                        JSONArray dtwp = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        wp.name = String.valueOf(dtwp.get(0));
                        wp.minX = Short.parseShort(String.valueOf(dtwp.get(1)));
                        wp.minY = Short.parseShort(String.valueOf(dtwp.get(2)));
                        wp.maxX = Short.parseShort(String.valueOf(dtwp.get(3)));
                        wp.maxY = Short.parseShort(String.valueOf(dtwp.get(4)));
                        wp.isEnter = Byte.parseByte(String.valueOf(dtwp.get(5))) == 1;
                        wp.isOffline = Byte.parseByte(String.valueOf(dtwp.get(6))) == 1;
                        wp.goMap = Short.parseShort(String.valueOf(dtwp.get(7)));
                        wp.goX = Short.parseShort(String.valueOf(dtwp.get(8)));
                        wp.goY = Short.parseShort(String.valueOf(dtwp.get(9)));
                        mapTemplate.wayPoints.add(wp);
                        dtwp.clear();
                    }
                    dataArray.clear();
                    //load mobs
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("mobs").replaceAll("\\\"", ""));
                    mapTemplate.mobTemp = new byte[dataArray.size()];
                    mapTemplate.mobLevel = new byte[dataArray.size()];
                    mapTemplate.mobHp = new int[dataArray.size()];
                    mapTemplate.mobX = new short[dataArray.size()];
                    mapTemplate.mobY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtm = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.mobTemp[j] = Byte.parseByte(String.valueOf(dtm.get(0)));
                        mapTemplate.mobLevel[j] = Byte.parseByte(String.valueOf(dtm.get(1)));
                        mapTemplate.mobHp[j] = Integer.parseInt(String.valueOf(dtm.get(2)));
                        mapTemplate.mobX[j] = Short.parseShort(String.valueOf(dtm.get(3)));
                        mapTemplate.mobY[j] = Short.parseShort(String.valueOf(dtm.get(4)));
                        dtm.clear();
                    }
                    dataArray.clear();
                    //load npcs
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("npcs").replaceAll("\\\"", ""));
                    mapTemplate.npcId = new byte[dataArray.size()];
                    mapTemplate.npcX = new short[dataArray.size()];
                    mapTemplate.npcY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtn = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.npcId[j] = Byte.parseByte(String.valueOf(dtn.get(0)));
                        mapTemplate.npcX[j] = Short.parseShort(String.valueOf(dtn.get(1)));
                        mapTemplate.npcY[j] = Short.parseShort(String.valueOf(dtn.get(2)));
                        dtn.clear();
                    }
                    dataArray.clear();
                    if (mapId == ConstMap.DAI_HOI_VO_THUAT_BANG_HOI) {
                        mapTemplate.name = "Đại hội võ thuật";
                        mapTemplate.type = ConstMap.MAP_NORMAL;
                        mapTemplate.planetId = 2;
                        mapTemplate.bgType = 0;
                        mapTemplate.tileId = 23;
                        mapTemplate.bgId = 8;
                        mapTemplate.zones = 10;
                        mapTemplate.maxPlayerPerZone = 35;
                        mapTemplate.wayPoints.clear();
                        mapTemplate.mobTemp = new byte[0];
                        mapTemplate.mobLevel = new byte[0];
                        mapTemplate.mobHp = new int[0];
                        mapTemplate.mobX = new short[0];
                        mapTemplate.mobY = new short[0];
                        mapTemplate.npcId = new byte[]{22, 22};
                        mapTemplate.npcX = new short[]{295, 474};
                        mapTemplate.npcY = new short[]{360, 360};
                    }
                    MAP_TEMPLATES[i++] = mapTemplate;
                }
                Logger.success(Logger.RED + "Successfully loaded map template (" + MAP_TEMPLATES.length + ")\n");
            }

            ps = ConnectionDatabase.prepareStatement("select * from radar");
            rs = ps.executeQuery();
            while (rs.next()) {
                RadarCard rd = new RadarCard();
                rd.Id = rs.getShort("id");
                rd.IconId = rs.getShort("iconId");
                rd.Rank = rs.getByte("rank");
                rd.Max = rs.getByte("max");
                rd.Type = rs.getByte("type");
                rd.Template = rs.getShort("mob_id");
                rd.Name = rs.getString("name");
                rd.Info = rs.getString("info");
                JSONArray arr = (JSONArray) JSONValue.parse(rs.getString("body"));
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject ob = (JSONObject) arr.get(i);
                    if (ob != null) {
                        rd.Head = Short.parseShort(ob.get("head").toString());
                        rd.Body = Short.parseShort(ob.get("body").toString());
                        rd.Leg = Short.parseShort(ob.get("leg").toString());
                        rd.Bag = Short.parseShort(ob.get("bag").toString());
                    }
                }
                rd.Options.clear();
                arr = (JSONArray) JSONValue.parse(rs.getString("options"));
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject ob = (JSONObject) arr.get(i);
                    if (ob != null) {
                        rd.Options.add(new OptionCard(Integer.parseInt(ob.get("id").toString()), Short.parseShort(ob.get("param").toString()), Byte.parseByte(ob.get("activeCard").toString())));
                    }
                }
                rd.Require = rs.getShort("require");
                rd.RequireLevel = rs.getShort("require_level");
                rd.AuraId = rs.getShort("aura_id");
                RadarService.gI().RADAR_TEMPLATE.add(rd);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded radar template (" + RadarService.gI().RADAR_TEMPLATE.size() + ")\n");

            //Load giftcode
            ps = ConnectionDatabase.prepareStatement("SELECT * FROM giftcode");
            rs = ps.executeQuery();
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                if (giftcode.countLeft == -1) {
                    giftcode.countLeft = 999999999;
                }
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                JSONArray jar = (JSONArray) JSONValue.parse(rs.getString("detail"));
                if (jar != null) {
                    for (int i = 0; i < jar.size(); ++i) {
                        JSONObject jsonObj = (JSONObject) jar.get(i);

                        int id = Integer.parseInt(jsonObj.get("id").toString());
                        int quantity = Integer.parseInt(jsonObj.get("quantity").toString());

                        JSONArray option = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();

                        if (option != null) {
                            for (int u = 0; u < option.size(); u++) {
                                JSONObject jsonobject = (JSONObject) option.get(u);
                                int optionId = Integer.parseInt(jsonobject.get("id").toString());
                                int param = Integer.parseInt(jsonobject.get("param").toString());
                                optionList.add(new Item.ItemOption(optionId, param));
                            }
                        }

                        giftcode.option.put(id, optionList);
                        giftcode.detail.put(id, quantity);
                    }
                }

                GiftCodeManager.gI().listGiftCode.add(giftcode);
            }
            Logger.success(Logger.RED + "Successfully loaded giftcode (" + GiftCodeManager.gI().listGiftCode.size() + ")\n");

            Topsukien = realTop(queryTopsukien, ConnectionDatabase);
            Logger.success(Logger.PURPLE + "Successfully Top Su Kien (" + Topsukien.size() + ")\n");
            Topsukien1 = realTop(queryTopsukien1, ConnectionDatabase);
            Topwhis = realTop(queryTopwhis, ConnectionDatabase);
            Logger.success(Logger.RED + "Successfully top Thach Dau Whis (" + Topwhis.size() + ")\n");
            Topmaydam = realTop(queryTopmaydam, ConnectionDatabase);
            Manager.timeRealTop = System.currentTimeMillis();

        } catch (Exception e) {
            Logger.logException(Manager.class, e, "Database loading error");
            System.exit(0);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }

        Logger.log(Logger.PURPLE, "Total database loading time: " + (System.currentTimeMillis() - st) + " (ms)\n");

    }

    public static List<TOP> realTop(String query, Connection con) {
        List<TOP> tops = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                TOP top = TOP.builder()
                        .id_player(rs.getInt("id"))
                        .build();

                if (query.equals(Manager.queryTopsukien)) {
                    int point = rs.getInt("point_sukien");
                    top.setInfo1(point + " điểm");
                    top.setInfo2(point + " điểm");

                } else if (query.equals(Manager.queryTopsukien1)) {
                    int point1 = rs.getInt("point_sukien1");
                    top.setInfo1(point1 + " điểm");
                    top.setInfo2(point1 + " điểm");

                } else if (query.equals(Manager.queryTopsukien2)) {
                    int point2 = rs.getInt("point_sukien2");
                    top.setInfo1(point2 + " điểm");
                    top.setInfo2(point2 + " điểm");

                } else if (query.equals(Manager.queryTopwhis)) {
                    int whis = rs.getInt("thachdauwhis");
                    top.setInfo1(whis + " Level");
                    top.setInfo2(whis + " Level");

                } else if (query.equals(Manager.queryTopmaydam)) {
                    int maydam = rs.getInt("point_maydam");
                    long totalDame = rs.getLong("total_damage_maydam");
                    top.setInfo1(maydam + " điểm");
                    top.setInfo2(Util.formatNumber(totalDame) + " sát thương");
                }

                tops.add(top);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tops.isEmpty()) {
            System.out.println("No data found for the query: " + query);
        }

        return tops;
    }

    public void loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("Config.properties"));
        Object value;
        if ((value = properties.get("server.sv")) != null) {
            SERVER = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.name")) != null) {
            String name = String.valueOf(value);
            ServerManager.NAME = name.equals("Local") ? " Local" : name;
        }
        if ((value = properties.get("server.port")) != null) {
            ServerManager.PORT = Integer.parseInt(String.valueOf(value));
        }
        String linkServer = "";
        if ((value = properties.get("server.ip")) != null) {
            ServerManager.IP = String.valueOf(value);
            linkServer += ServerManager.NAME + ":" + ServerManager.IP + ":" + ServerManager.PORT + ":0,";
        }
        for (int i = 1; i <= 10; i++) {
            value = properties.get("server.sv" + i);
            if (value != null) {
                linkServer += String.valueOf(value) + ":0,";
            }
        }
        DataGame.LINK_IP_PORT = linkServer.substring(0, linkServer.length() - 1);
        if ((value = properties.get("server.waitlogin")) != null) {
            SECOND_WAIT_LOGIN = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.maxperip")) != null) {
            MAX_PER_IP = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.maxplayer")) != null) {
            MAX_PLAYER = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.expserver")) != null) {
            RATE_EXP_SERVER = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.local")) != null) {
            LOCAL = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("server.test")) != null) {
            TEST = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("server.daoautoupdater")) != null) {
            DAO_AUTO_UPDATER = String.valueOf(value).equalsIgnoreCase("true");
        }
    }

    /**
     * @param tileTypeFocus tile type: top, bot, left, right...
     * @return [tileMapId][tileType]
     */
    private int[][] readTileIndexTileType(int tileTypeFocus) {
        int[][] tileIndexTileType = null;
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_set_info"));
            int numTileMap = dis.readByte();
            tileIndexTileType = new int[numTileMap][];
            for (int i = 0; i < numTileMap; i++) {
                int numTileOfMap = dis.readByte();
                for (int j = 0; j < numTileOfMap; j++) {
                    int tileType = dis.readInt();
                    int numIndex = dis.readByte();
                    if (tileType == tileTypeFocus) {
                        tileIndexTileType[i] = new int[numIndex];
                    }
                    for (int k = 0; k < numIndex; k++) {
                        int typeIndex = dis.readByte();
                        if (tileType == tileTypeFocus) {
                            tileIndexTileType[i][k] = typeIndex;

                        }
                    }
                }
            }
        } catch (IOException e) {
            Logger.logException(MapService.class, e);
        }
        return tileIndexTileType;
    }

    /**
     * @param mapId mapId
     * @return tile map for paint
     */
    private int[][] readTileMap(int mapId) {
        int[][] tileMap = null;
        try {
            try (DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_map_data/" + mapId))) {
                int w = dis.readByte();
                int h = dis.readByte();
                tileMap = new int[h][w];
                for (int[] tm : tileMap) {
                    for (int j = 0; j < tm.length; j++) {
                        tm[j] = dis.readByte();
                    }
                }
            }
        } catch (IOException e) {
        }
        return tileMap;
    }

    public static Clan getClanById(int id) throws Exception {
        for (Clan clan : CLANS) {
            if (clan.id == id) {
                return clan;
            }
        }
        throw new Exception("Không tìm thấy clan id: " + id);
    }

    public static void addClan(Clan clan) {
        CLANS.add(clan);
    }

    public static int getNumClan() {
        return CLANS.size();

    }

    public static MobTemplate getMobTemplateByTemp(int mobTempId) {
        for (MobTemplate mobTemp : MOB_TEMPLATES) {
            if (mobTemp.id == mobTempId) {
                return mobTemp;
            }
        }
        return null;
    }

    public static byte getNFrameImageByName(String name) {
        Object n = IMAGES_BY_NAME.get(name);
        if (n != null) {
            return Byte.parseByte(String.valueOf(n));
        } else {
            return 0;
        }
    }

    private void ensureVampaPlanetData(Connection con) {
        try {
            Object[][] customMobs = {
                {80, "Bọ Cạp Vampa", 8_000_000},
                {81, "Thằn Lằn Vampa", 12_000_000},
                {107, "Khủng Long Vampa", 18_000_000},
                {108, "Mãng Xà Vampa", 25_000_000},
                {86, "Dị Nhân Mắt Đỏ", 30_000_000},
                {87, "Chiến Binh Tuffle", 40_000_000},
                {88, "Rôbốt Đột Biến Đỏ", 55_000_000},
                {89, "Rôbốt Đột Biến Xanh", 75_000_000},
                {97, "Chiến Binh Vũ Trụ 6 Đỏ", 45_000_000},
                {98, "Chiến Binh Vũ Trụ 6 Xanh", 60_000_000},
                {105, "Dị Thú Vũ Trụ 6 Tím", 80_000_000},
                {106, "Dị Thú Vũ Trụ 6 Cam", 100_000_000},
                {94, "Toppo (Vũ Trụ 11)", 80_000_000},
                {96, "Janemba", 100_000_000}
            };
            try (PreparedStatement psMob = con.prepareStatement(
                    "UPDATE mob_template SET NAME = ?, hp = ? WHERE id = ?")) {
                for (Object[] m : customMobs) {
                    psMob.setString(1, (String) m[1]);
                    psMob.setInt(2, (Integer) m[2]);
                    psMob.setInt(3, (Integer) m[0]);
                    psMob.addBatch();
                }
                psMob.executeBatch();
            }

            try (PreparedStatement psNpc14 = con.prepareStatement(
                    "UPDATE map_template SET npcs = '[[9,396,408],[6,252,408],[74,286,409],[84,953,408],[83,640,408]]' WHERE id = 14");
                 PreparedStatement psNpc154 = con.prepareStatement(
                    "UPDATE map_template SET npcs = '[[44,810,312],[55,645,312],[56,725,312],[58,560,312]]' WHERE id = 154");
                 PreparedStatement psHeadHit = con.prepareStatement(
                    "INSERT INTO head_avatar (head_id, avatar_id) VALUES (520, 8127), (2099, 25010) ON DUPLICATE KEY UPDATE avatar_id = VALUES(avatar_id)")) {
                psNpc14.executeUpdate();
                psNpc154.executeUpdate();
                psHeadHit.executeUpdate();
            } catch (Exception ignored) {
            }

            Object[][] customMaps = {
                // Hành Tinh Vampa (Map 160, 161, 162, 163) -> Tileset 31, BG 18 (Nguyên bản Rừng Nguyên Thủy)
                {160, "Hang Động Vampa", 15, 15, "[0,2,0,9,8]", 31, 18, 0, 2, 0,
                    "[[\"Bìa Rừng Vampa\",1416,1560,1440,1584,0,0,161,60,312]]",
                    "[[80,21,8000000,132,168],[80,21,8000000,540,168],[80,21,8000000,708,600],[80,21,8000000,1212,648],[80,21,8000000,876,1080],[80,21,8000000,1308,144],[81,22,12000000,348,168],[81,22,12000000,756,168],[81,22,12000000,372,1320],[81,22,12000000,1080,1584],[81,22,12000000,1308,1824],[81,22,12000000,684,1824],[81,22,12000000,492,576],[81,22,12000000,156,888],[80,21,8000000,996,1824],[80,21,8000000,1140,1704],[80,21,8000000,348,576],[81,22,12000000,1068,288]]",
                    "[[56,1248,1584]]"},
                {161, "Bìa Rừng Vampa", 15, 15, "[0,0,0,29,16]", 31, 18, 0, 2, 0,
                    "[[\"Hang Động Vampa\",0,288,24,312,0,0,160,1350,1584],[\"Rừng Cổ Đại Vampa\",1896,312,1920,336,0,0,162,60,288]]",
                    "[[80,21,8000000,108,528],[80,21,8000000,852,792],[81,22,12000000,564,144],[81,22,12000000,1116,288],[81,22,12000000,1500,96],[80,21,8000000,900,144],[107,23,18000000,1476,336],[81,22,12000000,1116,600],[107,23,18000000,1500,1032],[81,22,12000000,900,1104],[81,22,12000000,324,552],[107,23,18000000,1740,960],[81,22,12000000,1212,144],[80,21,8000000,1812,96],[107,23,18000000,468,936],[107,23,18000000,1812,648],[80,21,8000000,804,336]]",
                    "[]"},
                {162, "Rừng Cổ Đại Vampa", 15, 15, "[0,0,0,29,16]", 31, 18, 0, 2, 0,
                    "[[\"Bìa Rừng Vampa\",0,264,24,288,0,0,161,1860,336],[\"Thung Lũng Vampa\",1896,240,1920,264,0,0,163,60,816]]",
                    "[[107,23,18000000,1308,144],[107,23,18000000,1140,384],[81,22,12000000,876,744],[81,22,12000000,276,528],[81,22,12000000,636,744],[81,22,12000000,492,96],[81,22,12000000,492,456],[107,23,18000000,396,792],[107,23,18000000,468,1080],[81,22,12000000,204,1032],[108,24,25000000,132,1272],[108,24,25000000,444,1272],[108,24,25000000,804,1272],[107,23,18000000,1236,648],[107,23,18000000,1788,720],[108,24,25000000,1380,912],[108,24,25000000,1356,1152],[108,24,25000000,1044,1272],[108,24,25000000,1596,1152]]",
                    "[]"},
                {163, "Thung Lũng Vampa", 15, 15, "[0,0,0,29,16]", 31, 18, 0, 2, 0,
                    "[[\"Rừng Cổ Đại Vampa\",0,792,24,816,0,0,162,1860,264]]",
                    "[[107,23,18000000,444,528],[107,23,18000000,396,144],[108,24,25000000,804,744],[108,24,25000000,1188,744],[107,23,18000000,1788,168],[107,23,18000000,1692,456],[108,24,25000000,1812,816],[108,24,25000000,588,744],[108,24,25000000,1524,816],[108,24,25000000,420,816]]",
                    "[]"},

                // Phương án A: Chuỗi Phòng Thí Nghiệm Tiến Sĩ Myuu (Map 166, 167, 172, 173)
                // Map 166 & 167 dùng Tileset 30, BG 17; Map 172 dùng Tileset 2, BG 1; Map 173 dùng Tileset 11, BG 0
                {166, "Phòng Thí Nghiệm Myuu", 15, 15, "[0,2,0,30,17]", 30, 17, 0, 2, 0,
                    "[[\"Khu Chế Tạo Baby\",1032,480,1056,504,0,0,167,60,576]]",
                    "[[86,21,30000000,240,504],[86,21,30000000,384,504],[86,21,30000000,528,504],[87,22,40000000,672,504],[87,22,40000000,816,504],[87,22,40000000,936,504],[86,21,30000000,360,240],[87,22,40000000,456,240],[86,21,30000000,552,240],[87,22,40000000,648,240]]",
                    "[[83,120,504]]"},
                {167, "Khu Chế Tạo Baby", 15, 15, "[0,2,0,30,17]", 30, 17, 0, 2, 0,
                    "[[\"Phòng Thí Nghiệm Myuu\",0,552,24,576,0,0,166,984,504],[\"Lõi Nghiên Cứu Tuffle\",1056,552,1080,576,0,0,172,60,360]]",
                    "[[86,21,30000000,168,576],[87,22,40000000,312,576],[87,22,40000000,456,576],[88,23,55000000,600,576],[88,23,55000000,744,576],[88,23,55000000,888,576],[87,22,40000000,600,336],[88,23,55000000,768,336],[87,22,40000000,912,336],[88,23,55000000,432,120]]",
                    "[]"},
                {172, "Lõi Nghiên Cứu Tuffle", 15, 15, "[0,2,0,2,1]", 2, 1, 0, 2, 0,
                    "[[\"Khu Chế Tạo Baby\",24,336,48,360,0,0,167,1000,576],[\"Buồng Ấp Chúa Tể Baby\",1032,432,1056,456,0,0,173,60,336]]",
                    "[[87,22,40000000,144,360],[88,23,55000000,240,360],[88,23,55000000,336,360],[88,23,55000000,504,456],[89,24,75000000,600,456],[89,24,75000000,816,456],[89,24,75000000,912,456],[88,23,55000000,984,456],[89,24,75000000,528,264],[89,24,75000000,624,264]]",
                    "[]"},
                {173, "Buồng Ấp Chúa Tể Baby", 15, 15, "[0,2,0,11,0]", 11, 0, 0, 2, 0,
                    "[[\"Lõi Nghiên Cứu Tuffle\",0,312,24,336,0,0,172,984,456]]",
                    "[[88,23,55000000,144,336],[88,23,55000000,224,336],[89,24,75000000,304,336],[89,24,75000000,384,336],[89,24,75000000,464,336],[89,24,75000000,544,336],[89,24,75000000,624,336],[89,24,75000000,704,336],[89,24,75000000,784,336],[89,24,75000000,864,336]]",
                    "[]"},

                // Phương án B: Hành Tinh Hủy Diệt Vũ Trụ 6 (Map 169, 170, 171) -> Tileset 29, BG 16 (Giống Hành Tinh Bill)
                {169, "Hành Tinh Champa", 15, 15, "[0,0,0,29,16]", 29, 16, 0, 0, 0,
                    "[[\"Thung Lũng Vũ Trụ 6\",1200,192,1248,216,0,0,170,60,168]]",
                    "[[97,22,45000000,96,336],[97,22,45000000,216,336],[98,23,60000000,336,336],[97,22,45000000,264,96],[98,23,60000000,360,96],[97,22,45000000,672,216],[98,23,60000000,792,216],[98,23,60000000,912,216],[97,22,45000000,1032,216],[98,23,60000000,1128,216]]",
                    "[[58,560,216]]"},
                {170, "Thung Lũng Vũ Trụ 6", 15, 15, "[0,0,0,29,16]", 29, 16, 0, 0, 0,
                    "[[\"Hành Tinh Champa\",0,144,24,168,0,0,169,1152,216],[\"Lãnh Địa Sát Thủ Hit\",1200,216,1248,240,0,0,171,60,168]]",
                    "[[97,22,45000000,168,216],[98,23,60000000,264,264],[98,23,60000000,360,384],[105,24,80000000,456,384],[105,24,80000000,552,384],[106,25,100000000,648,384],[105,24,80000000,768,312],[106,25,100000000,840,312],[105,24,80000000,936,288],[106,25,100000000,1080,240]]",
                    "[]"},
                {171, "Lãnh Địa Sát Thủ Hit", 15, 15, "[0,0,0,29,16]", 29, 16, 0, 0, 0,
                    "[[\"Thung Lũng Vũ Trụ 6\",0,144,24,168,0,0,170,1152,240]]",
                    "[[98,23,60000000,168,456],[105,24,80000000,240,456],[106,25,100000000,312,456],[98,23,60000000,384,456],[105,24,80000000,456,456],[106,25,100000000,528,456],[106,25,100000000,600,456],[105,24,80000000,720,168],[106,25,100000000,804,168],[106,25,100000000,888,168]]",
                    "[]"}
            };
            try (PreparedStatement psMap = con.prepareStatement(
                    "INSERT INTO map_template (id, NAME, zones, max_player, data, type, planet_id, bg_type, tile_id, bg_id, waypoints, mobs, npcs) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE NAME = VALUES(NAME), zones = VALUES(zones), max_player = VALUES(max_player), "
                    + "data = VALUES(data), type = VALUES(type), planet_id = VALUES(planet_id), bg_type = VALUES(bg_type), tile_id = VALUES(tile_id), bg_id = VALUES(bg_id), "
                    + "waypoints = VALUES(waypoints), mobs = VALUES(mobs), npcs = VALUES(npcs)")) {
                for (Object[] row : customMaps) {
                    psMap.setInt(1, (Integer) row[0]);
                    psMap.setString(2, (String) row[1]);
                    psMap.setInt(3, (Integer) row[2]);
                    psMap.setInt(4, (Integer) row[3]);
                    psMap.setString(5, (String) row[4]);
                    psMap.setInt(6, (Integer) row[7]);
                    psMap.setInt(7, (Integer) row[8]);
                    psMap.setInt(8, (Integer) row[9]);
                    psMap.setInt(9, (Integer) row[5]);
                    psMap.setInt(10, (Integer) row[6]);
                    psMap.setString(11, (String) row[10]);
                    psMap.setString(12, (String) row[11]);
                    psMap.setString(13, (String) row[12]);
                    psMap.addBatch();
                }
                psMap.executeBatch();
            }
        } catch (Exception e) {
            Logger.logException(Manager.class, e);
        }
    }

    private void ensureExtendedStoryTasks(Connection con) {
        try {
            Object[][] mainTasks = {
                {29, "Cuộc chiến cuối cùng với Ma Bư",
                    "Ma Bư đã biến đổi thành các hình thái đáng sợ hơn!\n"
                    + "Hãy kêu gọi đồng bang cùng tiêu diệt bè lũ Xên con, Mabư Mập và Kid Bư.\n"
                    + "- Đi cùng đồng bang trong khu: X2-X3 tiến độ quái & tính chung hạ Boss!\n"
                    + "- Thưởng: 10 Thỏi vàng + 500 Hồng ngọc + 50Tr SM/TN"},
                {30, "Sự trả thù của Đế chế Băng Giá",
                    "Cooler cùng Fide Hoàng Kim đã kéo quân xâm lược!\n"
                    + "Hãy cùng các thành viên trong Bang hội tiến vào Hành tinh Cold tiêu diệt chúng.\n"
                    + "- Đi cùng đồng bang trong khu: X2-X3 tiến độ quái & tính chung hạ Boss!\n"
                    + "- Thưởng: 20 Thỏi vàng + 1.000 Hồng ngọc + 100Tr SM/TN"},
                {31, "Kế hoạch Không Người Trần",
                    "Thế giới Tương Lai đang lâm nguy trước Black Goku (Super Saiyan Rosé) và Zamasu!\n"
                    + "Hãy lập tổ đội cùng đồng bang giải cứu Tương Lai.\n"
                    + "- Đi cùng đồng bang trong khu: X2-X3 tiến độ quái & tính chung hạ Boss!\n"
                    + "- Thưởng: 30 Thỏi vàng + 1.500 Hồng ngọc + 150Tr SM/TN"},
                {32, "Giải Đấu Sức Mạnh - Bộ 3 Sói",
                    "Bước vào Giải Đấu Đa Vũ Trụ! Đối đầu Bộ 3 Sói nguy hiểm nhất Vũ Trụ 9: Basil, Lavender và Bergamo.\n"
                    + "- Đi cùng đồng bang trong khu: Tính chung tiến độ hạ Boss (3+ đồng bang nhận X2 điểm Boss)!\n"
                    + "- Thưởng: 40 Thỏi vàng + 2.000 Hồng ngọc + 200Tr SM/TN"},
                {33, "Phòng Thí Nghiệm Myuu - Baby Vegeta",
                    "Tại Phòng Thí Nghiệm Bí Mật của Tiến Sĩ Myuu, ký sinh trùng Tuffle Baby đã chiếm xác Vegeta và hóa Khỉ Đột Vàng!\n"
                    + "Hãy gặp Dr. Myuu tại Làng Kakarot để tiến vào Phòng Thí Nghiệm tiêu diệt chúng.\n"
                    + "- Đi cùng đồng bang trong khu: X2-X3 tiến độ quái & tính chung hạ Boss!\n"
                    + "- Thưởng: 50 Thỏi vàng + 2.500 Hồng ngọc + 250Tr SM/TN"},
                {34, "Saiyan Tà Ác Cổ Đại - Cumber",
                    "Tại Hành Tinh Ngục Tù, Saiyan Tà Ác cổ đại Cumber đã phá vỡ phong ấn!\n"
                    + "Hãy sát cánh cùng những chiến binh mạnh nhất tiêu diệt Cumber và Super Cumber.\n"
                    + "- Đi cùng đồng bang trong khu: X2-X3 tiến độ quái & tính chung hạ Boss!\n"
                    + "- Thưởng: 60 Thỏi vàng + 3.000 Hồng ngọc + 300Tr SM/TN"},
                {35, "Hành Tinh Vampa - Broly Cổ Đại",
                    "Tại Hành Tinh Vampa hoang dã, nguồn năng lượng của Broly Cổ Đại đã thức tỉnh!\n"
                    + "Hãy gặp Whis tại Hành Tinh Bill để đến Vampa, diệt Dị Thú và hạ gục SSJ Broly.\n"
                    + "- Đi cùng đồng bang trong khu: X2-X3 tiến độ quái & tính chung hạ Boss!\n"
                    + "- Thưởng: 80 Thỏi vàng + 5.000 Hồng ngọc + 500Tr SM/TN"},
                {36, "Vũ Trụ 6 - Sát Thủ Huyền Thoại Hit",
                    "Gặp Thiên Sứ Vados tại Hành Tinh Bill để tiến sang Hành Tinh Hủy Diệt Vũ Trụ 6!\n"
                    + "Đối đầu Siêu Xayda Kefla, Thần Hủy Diệt Champa và Sát Thủ Huyền Thoại Hit (rơi 100% Quả Trứng Hit - ấp Đệ Tử Cấp 4 Hợp Thể +45% chỉ số).\n"
                    + "- Đi cùng đồng bang trong khu: X2-X3 tiến độ quái & tính chung hạ Boss!\n"
                    + "- Thưởng: 100 Thỏi vàng + 8.000 Hồng ngọc + 1 Tỷ SM/TN"},
                {37, "Huyền Thoại Đa Vũ Trụ",
                    "Xin chúc mừng! Bạn đã vượt qua mọi thử thách khắc nghiệt nhất của Đa Vũ Trụ và trở thành Huyền Thoại!"}
            };

            try (PreparedStatement psMain = con.prepareStatement(
                    "INSERT INTO task_main_template (id, NAME, detail) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE NAME = VALUES(NAME), detail = VALUES(detail)")) {
                for (Object[] row : mainTasks) {
                    psMain.setInt(1, (Integer) row[0]);
                    psMain.setString(2, (String) row[1]);
                    psMain.setString(3, (String) row[2]);
                    psMain.addBatch();
                }
                psMain.executeBatch();
            }

            try (PreparedStatement psDelSub = con.prepareStatement(
                    "DELETE FROM task_sub_template WHERE task_main_id >= 29 AND task_main_id <= 37")) {
                psDelSub.executeUpdate();
            }

            Object[][] subTasks = {
                // NV 29: Cuộc chiến cuối cùng với Ma Bư (10 TV + 500 HN)
                {29, "Nói chuyện với Ôsin tại Đại hội võ thuật", 1, "Hãy đến Đại hội võ thuật gặp Ôsin nhận nhiệm vụ", 44, 52, 125},
                {29, "Cùng đồng bang diệt 1200 Xên con cấp 8", 1200, "Đi cùng đồng bang trong khu để X2-X3 tốc độ!", -1, 100, 126},
                {29, "Phối hợp hạ 5 Kid Bư (hoặc Mabư 14h)", 5, "Đi cùng đồng bang trong khu sẽ được tính chung hạ Boss!", -1, -1, 127},
                {29, "Phối hợp hạ 3 Mabư Mập (hoặc Super Bư)", 3, "Tiếp tục cùng đồng đội truy lùng Mabư Mập / Super Bư", -1, -1, 128},
                {29, "Báo cáo chiến công với Ôsin", 1, "Quay về Đại hội võ thuật báo cáo với Ôsin", 44, 52, 129},

                // NV 30: Sự trả thù của Đế chế Băng Giá (20 TV + 1000 HN)
                {30, "Đến Cánh đồng tuyết (Hành tinh Cold)", 1, "Di chuyển tới Cánh đồng tuyết tại Hành tinh Cold", -1, 105, 130},
                {30, "Cùng đồng bang diệt 1500 Abo & Kado tại Cold", 1500, "Đi cùng đồng bang trong khu để X2-X3 tốc độ!", -1, 106, 131},
                {30, "Phối hợp tiêu diệt 3 Cooler cấp 1", 3, "Cùng đồng bang săn Cooler cấp 1 tại Hành tinh Cold", -1, 107, 132},
                {30, "Phối hợp tiêu diệt 3 Cooler cấp 2", 3, "Tiếp tục tiêu diệt Cooler cấp 2 (Final Form)", -1, 108, 133},
                {30, "Phối hợp tiêu diệt 3 Fide Hoàng Kim", 3, "Hợp sức tiêu diệt Golden Frieza (Fide Hoàng Kim)", -1, -1, 134},
                {30, "Báo cáo với %10", 1, "Quay về %11 báo cáo với %10", -5, -9, 135},

                // NV 31: Kế hoạch Không Người Trần (30 TV + 1500 HN)
                {31, "Đến Tương Lai gặp Bunma Tương Lai", 1, "Đến Tương Lai nói chuyện với Bunma", 37, 102, 136},
                {31, "Cùng đồng bang diệt 2000 Tai Tím & Đá Xanh", 2000, "Đi cùng đồng bang trong khu để X2-X3 tốc độ!", -1, 110, 137},
                {31, "Phối hợp tiêu diệt 4 Black Goku", 4, "Cùng đồng đội tiêu diệt Black Goku tại Tương Lai", -1, 92, 138},
                {31, "Phối hợp tiêu diệt 4 Super Black Goku", 4, "Tiếp tục tiêu diệt Super Black Goku (Rosé)", -1, 93, 139},
                {31, "Phối hợp tiêu diệt 4 Thần Zamasu", 4, "Hợp sức đánh bại Thần bất tử Zamasu", -1, 94, 140},
                {31, "Báo cáo với Bunma Tương Lai", 1, "Quay về gặp Bunma Tương Lai nhận thưởng", 37, 102, 141},

                // NV 32: Giải Đấu Sức Mạnh - Bộ 3 Sói Vũ Trụ 9 (40 TV + 2000 HN)
                {32, "Gặp Ôsin nhận lệnh Giải Đấu Sức Mạnh", 1, "Đến Đại hội võ thuật gặp Ôsin", 44, 52, 142},
                {32, "Phối hợp hạ 4 Sói Basil", 4, "Cùng đồng bang đánh bại Sói Basil", -1, -1, 143},
                {32, "Phối hợp hạ 4 Sói Lavender", 4, "Cùng đồng bang đánh bại Sói Lavender", -1, -1, 144},
                {32, "Phối hợp hạ 4 Sói Bergamo", 4, "Cùng đồng bang đánh bại Sói Khổng Lồ Bergamo", -1, -1, 145},
                {32, "Báo cáo chiến thắng với Ôsin", 1, "Quay về báo cáo với Ôsin tại Đại hội võ thuật", 44, 52, 146},

                // NV 33: Phòng Thí Nghiệm Myuu - Baby Vegeta (50 TV + 2500 HN)
                {33, "Cùng đồng bang diệt 2500 Dị Nhân tại Phòng Thí Nghiệm Myuu", 2500, "Gặp Dr. Myuu tại Làng Kakarot để vào Phòng Thí Nghiệm (X2-X3 cùng bang)", 83, 166, 147},
                {33, "Phối hợp tiêu diệt 4 Baby Vegeta", 4, "Cùng đồng đội tiêu diệt Baby Vegeta tại Phòng Thí Nghiệm Myuu", -1, 166, 148},
                {33, "Phối hợp tiêu diệt 4 Super Baby 2", 4, "Tiếp tục tiêu diệt Super Baby 2 tại Khu Chế Tạo Baby", -1, 167, 149},
                {33, "Phối hợp tiêu diệt 4 Baby Khỉ Đột Vàng", 4, "Hợp lực hạ gục Khỉ Đột Vàng Baby tại Lõi Nghiên Cứu", -1, 172, 150},
                {33, "Báo cáo với %10", 1, "Quay về %11 báo cáo với %10", -5, -9, 151},

                // NV 34: Saiyan Tà Ác Cổ Đại - Cumber (60 TV + 3000 HN)
                {34, "Tiến vào Hành Tinh Ngục Tù", 1, "Tìm đường tiến vào Hành Tinh Ngục Tù", -1, 155, 152},
                {34, "Cùng đồng bang diệt 3000 quái Ngục Tù", 3000, "Diệt Khỉ Lông Xanh & Taburine Đỏ tại Ngục Tù (X2-X3 cùng bang)", -1, 155, 153},
                {34, "Phối hợp tiêu diệt 5 Saiyan Tà Ác Cumber", 5, "Cùng đồng đội hạ gục Cumber tại Hành Tinh Ngục Tù", -1, 155, 154},
                {34, "Phối hợp tiêu diệt 5 Super Cumber", 5, "Hợp sức đánh bại Super Cumber tối thượng!", -1, 155, 155},
                {34, "Báo cáo với Bunma Tương Lai", 1, "Quay về báo cáo với Bunma Tương Lai!", 37, 102, 156},

                // NV 35: Hành Tinh Vampa - Broly Cổ Đại (80 TV + 5000 HN)
                {35, "Gặp Whis tại Hành Tinh Bill để đến Vampa", 1, "Đến Hành Tinh Bill (Map 154) gặp Whis chọn Đến Hành Tinh Vampa", 56, 154, 157},
                {35, "Cùng đồng bang diệt 2500 Dị Thú Vampa", 2500, "Diệt Bọ Cạp, Thằn Lằn, Khủng Long, Mãng Xà tại Vampa (X2-X3 cùng bang)", -1, 160, 158},
                {35, "Phối hợp đánh bại 5 Broly Cổ Đại", 5, "Cùng đồng bang hạ gục 5 Boss Broly Cổ Đại tại Hành Tinh Vampa", -1, 161, 159},
                {35, "Phối hợp đánh bại 2 SSJ Broly Huyền Thoại", 2, "Hạ gục SSJ Broly tại Hành Tinh Vampa (Rơi 100% Trứng Broly)", -1, 162, 160},
                {35, "Báo cáo chiến công với %10", 1, "Quay về %11 báo cáo với %10 hoàn tất huyền thoại!", -5, -9, 161},

                // NV 36: Vũ Trụ 6 - Sát Thủ Huyền Thoại Hit (100 TV + 8000 HN)
                {36, "Gặp Vados tại Hành Tinh Bill để đến Vũ Trụ 6", 1, "Đến Hành Tinh Bill (Map 154) gặp Vados chọn Đến Hành Tinh Vũ Trụ 6", 58, 154, 162},
                {36, "Cùng đồng bang diệt 3000 Chiến Binh Vũ Trụ 6", 3000, "Diệt Chiến Binh Vũ Trụ 6 tại Hành Tinh Champa (X2-X3 cùng bang)", -1, 169, 163},
                {36, "Phối hợp đánh bại 4 Siêu Xayda Kefla", 4, "Cùng đồng bang hạ gục Caulifla / Kefla tại Vũ Trụ 6", -1, 169, 164},
                {36, "Phối hợp đánh bại 4 Thần Hủy Diệt Champa / Toppo", 4, "Hợp sức đánh bại Thần Hủy Diệt Champa hoặc Toppo (Vũ Trụ 11) tại Vũ Trụ 6", -1, 170, 165},
                {36, "Phối hợp đánh bại 3 Sát Thủ Hit", 3, "Hạ gục Sát Thủ Hit tại Lãnh Địa Hit (Rơi 100% Quả Trứng Hit - Đệ Cấp 4)", -1, 171, 166},
                {36, "Báo cáo chiến công với %10", 1, "Quay về %11 báo cáo với %10 hoàn tất huyền thoại Đa Vũ Trụ!", -5, -9, 167},

                // NV 37: Hoàn thành toàn bộ nhiệm vụ
                {37, "Bạn đã hoàn thành toàn bộ nhiệm vụ cốt truyện!", -1, "Chúc mừng Huyền Thoại Đa Vũ Trụ!", -1, -1, 168}
            };

            try (PreparedStatement psSub = con.prepareStatement(
                    "INSERT INTO task_sub_template (task_main_id, NAME, max_count, notify, npc_id, map, ducvupro) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                for (Object[] sub : subTasks) {
                    psSub.setInt(1, (Integer) sub[0]);
                    psSub.setString(2, (String) sub[1]);
                    psSub.setInt(3, (Integer) sub[2]);
                    psSub.setString(4, (String) sub[3]);
                    psSub.setInt(5, (Integer) sub[4]);
                    psSub.setInt(6, (Integer) sub[5]);
                    psSub.setInt(7, (Integer) sub[6]);
                    psSub.addBatch();
                }
                psSub.executeBatch();
            }
        } catch (Exception e) {
            Logger.logException(Manager.class, e);
        }
    }

    private void ensurePurpleDragonBallIcons() {
        int[] baseIcons = {419, 420, 421, 422, 423, 424, 425, 15128};
        for (int zoom = 1; zoom <= 4; zoom++) {
            for (int i = 0; i < baseIcons.length; i++) {
                int srcIcon = baseIcons[i];
                int dstIcon = 25001 + i;
                File dstFile = new File("data/icon/x" + zoom + "/" + dstIcon + ".png");
                if (dstFile.exists()) {
                    continue;
                }
                File srcFile = new File("data/icon/x" + zoom + "/" + srcIcon + ".png");
                if (!srcFile.exists()) {
                    continue;
                }
                try {
                    BufferedImage src = ImageIO.read(srcFile);
                    if (src == null) {
                        continue;
                    }
                    BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < src.getHeight(); y++) {
                        for (int x = 0; x < src.getWidth(); x++) {
                            int argb = src.getRGB(x, y);
                            int a = (argb >> 24) & 0xFF;
                            if (a == 0) {
                                dst.setRGB(x, y, 0);
                                continue;
                            }
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;
                            float[] hsb = Color.RGBtoHSB(r, g, b, null);
                            // Shift yellow/orange/red hues into purple/magenta spectrum
                            float newHue = (hsb[0] + 0.68f) % 1.0f;
                            float newSat = Math.min(1.0f, hsb[1] * 1.15f);
                            int rgb = Color.HSBtoRGB(newHue, newSat, hsb[2]);
                            dst.setRGB(x, y, (a << 24) | (rgb & 0x00FFFFFF));
                        }
                    }
                    ImageIO.write(dst, "png", dstFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

}
