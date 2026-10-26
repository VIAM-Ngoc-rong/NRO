package nro.models.npc;

import nro.models.npc_list.Whis;
import nro.models.npc_list.LinhCanh;
import nro.models.npc_list.Bulma;
import nro.models.npc_list.Cargo;
import nro.models.npc_list.ThanVuTru;
import nro.models.npc_list.Tapion;
import nro.models.npc_list.DocNhan;
import nro.models.npc_list.TruongLaoGuru;
import nro.models.npc_list.Rong5Sao;
import nro.models.npc_list.Santa;
import nro.models.npc_list.Cui;
import nro.models.npc_list.ToSuKaio;
import nro.models.npc_list.Kibit;
import nro.models.npc_list.Appule;
import nro.models.npc_list.QuaTrung;
import nro.models.npc_list.KyGui;
import nro.models.npc_list.Rong7Sao;
import nro.models.npc_list.Potage;
import nro.models.npc_list.Babiday;
import nro.models.npc_list.OngGohan;
import nro.models.npc_list.DrDrief;
import nro.models.npc_list.Karin;
import nro.models.npc_list.LyTieuNuong;
import nro.models.npc_list.Rong1Sao;
import nro.models.npc_list.MrPoPo;
import nro.models.npc_list.Bill;
import nro.models.npc_list.Rong6Sao;
import nro.models.npc_list.Bardock;
import nro.models.npc_list.Uron;
import nro.models.npc_list.VuaVegeta;
import nro.models.npc_list.GokuSSJ;
import nro.models.npc_list.RongOmega;
import nro.models.npc_list.BulmaTuongLai;
import nro.models.npc_list.GhiDanh;
import nro.models.npc_list.QuyLaoKame;
import nro.models.npc_list.Calick;
import nro.models.npc_list.Rong2Sao;
import nro.models.npc_list.BaHatMit;
import nro.models.npc_list.RuongDo;
import nro.models.npc_list.ThuongDe;
import nro.models.npc_list.OngParagus;
import nro.models.npc_list.GiuMaDauBo;
import nro.models.npc_list.Vados;
import nro.models.npc_list.TrongTai;
import nro.models.npc_list.Rong4Sao;
import nro.models.npc_list.Rong3Sao;
import nro.models.npc_list.BoMong;
import nro.models.npc_list.Dende;
import nro.models.npc_list.Jaco;
import nro.models.npc_list.QuocVuong;
import nro.models.npc_list.Osin;
import nro.models.npc_list.DauThan;
import nro.models.npc_list.OngMoori;
import nro.models.npc_list.DaiThienSu;
import nro.models.npc_list.GokuSSJ2;
import nro.models.services.ClanService;
import nro.models.services.Service;
import nro.models.services.ItemService;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.services.IntrinsicService;
import nro.models.services.InventoryService;
import nro.models.map.service.NpcService;
import nro.models.services.PetService;
import nro.models.services.PlayerService;
import nro.models.services.FriendAndEnemyService;
import nro.models.consts.ConstNpc;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.clan.Clan;
import nro.models.event.VuaHung;
import nro.models.event.NoiBanh;
import java.util.HashMap;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.item.Item;
import nro.models.matches.PVPService;
import nro.models.server.Client;
import nro.models.server.Maintenance;
import nro.models.server.Manager;
import nro.models.services_func.Input;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import nro.models.services_dungeon.SuperDivineWaterService;
import nro.models.services.SubMenuService;
import nro.models.services.shenron.Shenron_Service;
import java.util.List;
import nro.models.event.XeNuocMia;
import nro.models.npc_list.ChiChi;
import nro.models.npc_list.DrMyuu;
import nro.models.npc_list.DuaHau;
import nro.models.npc_list.ToriBot;
import nro.models.services.shenron.SummonDragon;
import static nro.models.services.shenron.SummonDragon.SHENRON_1_STAR_WISHES_1;
import static nro.models.services.shenron.SummonDragon.SHENRON_1_STAR_WISHES_2;
import static nro.models.services.shenron.SummonDragon.SHENRON_1_STAR_WISHES_3;
import static nro.models.services.shenron.SummonDragon.SHENRON_SAY;
import nro.models.services.shenron.SummonDragonNamek;
import nro.models.shop_ky_gui.ConsignShopService;

public class NpcFactory {

    public static final java.util.Map<Long, Object> PLAYERID_OBJECT = new HashMap<>();

    public static Npc createNPC(int mapId, int status, int cx, int cy, int tempId) {
        int avatar = Manager.NPC_TEMPLATES.get(tempId).avatar;
        try {
            return switch (tempId) {
                case ConstNpc.GHI_DANH ->
                    new GhiDanh(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TRONG_TAI ->
                    new TrongTai(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.POTAGE ->
                    new Potage(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.MR_POPO ->
                    new MrPoPo(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.QUY_LAO_KAME ->
                    new QuyLaoKame(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TRUONG_LAO_GURU ->
                    new TruongLaoGuru(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.VUA_VEGETA ->
                    new VuaVegeta(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CUA_HANG_KY_GUI ->
                    new KyGui(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.ONG_GOHAN ->
                    new OngGohan(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.ONG_MOORI ->
                    new OngMoori(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.ONG_PARAGUS ->
                    new OngParagus(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BUNMA ->
                    new Bulma(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DENDE ->
                    new Dende(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.APPULE ->
                    new Appule(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DR_DRIEF ->
                    new DrDrief(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CARGO ->
                    new Cargo(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CUI ->
                    new Cui(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.SANTA ->
                    new Santa(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.URON ->
                    new Uron(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BA_HAT_MIT ->
                    new BaHatMit(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RUONG_DO ->
                    new RuongDo(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DR_MYUU ->
                    new DrMyuu(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DAU_THAN ->
                    new DauThan(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CALICK ->
                    new Calick(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.JACO ->
                    new Jaco(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.THUONG_DE ->
                    new ThuongDe(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.VADOS ->
                    new Vados(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.THAN_VU_TRU ->
                    new ThanVuTru(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.KIBIT ->
                    new Kibit(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.OSIN ->
                    new Osin(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TORIBOT ->
                    new ToriBot(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BABIDAY ->
                    new Babiday(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.LY_TIEU_NUONG ->
                    new LyTieuNuong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.LINH_CANH ->
                    new LinhCanh(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.QUA_TRUNG ->
                    new QuaTrung(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.QUOC_VUONG ->
                    new QuocVuong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BUNMA_TL ->
                    new BulmaTuongLai(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_OMEGA ->
                    new RongOmega(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_1S ->
                    new Rong1Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_2S ->
                    new Rong2Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_3S ->
                    new Rong3Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_4S ->
                    new Rong4Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_5S ->
                    new Rong5Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_6S ->
                    new Rong6Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_7S ->
                    new Rong7Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DAI_THIEN_SU ->
                    new DaiThienSu(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.WHIS ->
                    new Whis(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BILL ->
                    new Bill(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BO_MONG ->
                    new BoMong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.THAN_MEO_KARIN ->
                    new Karin(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.GOKU_SSJ ->
                    new GokuSSJ(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.GOKU_SSJ_2 ->
                    new GokuSSJ2(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TAPION ->
                    new Tapion(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DOC_NHAN ->
                    new DocNhan(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.GIUMA_DAU_BO ->
                    new GiuMaDauBo(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TO_SU_KAIO ->
                    new ToSuKaio(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CHI_CHI ->
                    new ChiChi(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.HUNG_VUONG ->
                    new VuaHung(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DUA_HAU ->
                    new DuaHau(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.NOI_BANH ->
                    new NoiBanh(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.XE_NUOC_MIA ->
                    new XeNuocMia(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BARDOCK ->
                    new Bardock(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DUONG_TANG, ConstNpc.NGO_KHONG ->
                    new Npc(mapId, status, cx, cy, tempId, avatar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                if (this.mapId >= 122 && this.mapId <= 124) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "A di đà phật! Nơi đây linh khí hội tụ (x5 TNSM).\nThí chủ có muốn quay về nhà không?",
                                            "Về nhà", "Ở lại");
                                } else {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "A di đà phật! Thí chủ có muốn cùng bần tăng đến Ngũ Hành Sơn tu luyện (x5 TNSM) không?",
                                            "Đến\nNgũ Hành Sơn", "Từ chối");
                                }
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player) && player.idMark.isBaseMenu() && select == 0) {
                                if (this.mapId >= 122 && this.mapId <= 124) {
                                    ChangeMapService.gI().changeMapBySpaceShip(player, 21 + player.gender, -1, -1);
                                } else {
                                    ChangeMapService.gI().changeMapBySpaceShip(player, 122, -1, -1);
                                }
                            }
                        }
                    };
                default ->
                    new Npc(mapId, status, cx, cy, tempId, avatar) {
                        @Override
                        public void openBaseMenu(Player player) {
                            if (canOpenNpc(player)) {
                                super.openBaseMenu(player);
                            }
                        }

                        @Override
                        public void confirmMenu(Player player, int select) {
                            if (canOpenNpc(player)) {
                            }
                        }
                    };
            };
        } catch (Exception e) {
            Logger.logException(NpcFactory.class,
                    e, "Lỗi load npc");
            return null;
        }
    }

    public static void createNpcRongThieng() {
        new Npc(-1, -1, -1, -1, ConstNpc.RONG_THIENG, -1) {
            @Override
            public void confirmMenu(Player player, int select) {
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.IGNORE_MENU:
                        break;
                    case ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM:
                        SummonDragonNamek.gI().showConfirmShenron(player, player.idMark.getIndexMenu(), (byte) select);
                        break;
                    case ConstNpc.SHENRON_NAMEK_CONFIRM:
                        if (select == 0) {
                            SummonDragonNamek.gI().confirmWish();
                        } else if (select == 1) {
                            SummonDragonNamek.gI().sendBlackGokuhesNamec(player);
                        }
                        break;
                    case ConstNpc.SHOW_SHENRON_EVENT_CONFIRM:
                        if (player.shenronEvent != null) {
                            player.shenronEvent.showConfirmShenron((byte) select);
                        }
                        break;
                    case ConstNpc.SHENRON_EVENT_CONFIRM:
                        if (player.shenronEvent != null) {
                            if (select == 0) {
                                player.shenronEvent.confirmWish();
                            } else if (select == 1) {
                                player.shenronEvent.sendBlackGokuhesShenron();
                            }
                        }
                        break;
                    case ConstNpc.SHENRON_CONFIRM:
                        if (select == 0) {
                            SummonDragon.gI().confirmWish();
                        } else if (select == 1) {
                            SummonDragon.gI().reOpenShenronWishes(player);
                        }
                        break;
                    case ConstNpc.SHENRON_1_1:
                        if (player.idMark.getIndexMenu() == ConstNpc.SHENRON_1_1 && select == SHENRON_1_STAR_WISHES_1.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_2, SHENRON_SAY, SHENRON_1_STAR_WISHES_2);
                            break;
                        }
                    case ConstNpc.SHENRON_1_2:
                        if (player.idMark.getIndexMenu() == ConstNpc.SHENRON_1_2 && select == SHENRON_1_STAR_WISHES_2.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_3, SHENRON_SAY, SHENRON_1_STAR_WISHES_3);
                            break;
                        }
                    case ConstNpc.SHENRON_1_3:
                        if (player.idMark.getIndexMenu() == ConstNpc.SHENRON_1_3 && select == SHENRON_1_STAR_WISHES_3.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_1, SHENRON_SAY, SHENRON_1_STAR_WISHES_1);
                            break;
                        }
                    default:
                        SummonDragon.gI().showConfirmShenron(player, player.idMark.getIndexMenu(), (byte) select);
                        break;
                }
            }
        };
    }

    public static void createNpcConMeo() {
        new Npc(-1, -1, -1, -1, ConstNpc.CON_MEO, 351) {
            @Override
            public void confirmMenu(Player player, int select) {
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.IGNORE_MENU -> {
                        // Không làm gì
                    }

                    case ConstNpc.SUMMON_SHENRON_EVENT -> {
                        if (select == 0) {
                            Shenron_Service.gI().summonShenron(player);
                        }
                    }
                    case ConstNpc.MAKE_MATCH_PVP -> {
                        if (Maintenance.isRunning) {
                        }
                        PVPService.gI().sendInvitePVP(player, (byte) select);
                    }
                    case ConstNpc.MAKE_FRIEND -> {
                        if (select == 0) {
                            Object playerId = PLAYERID_OBJECT.get(player.id);
                            if (playerId != null) {
                                try {
                                    FriendAndEnemyService.gI().acceptMakeFriend(player,
                                            Integer.parseInt(String.valueOf(playerId)));
                                } catch (NumberFormatException e) {
                                }
                            }
                        }
                    }
                    case 206783 -> {
                        switch (select) {
                            case 0 ->
                                Input.gI().createFormBotQuai(player);
                            case 1 ->
                                Input.gI().createFormBotItem(player);
                            case 2 ->
                                Input.gI().createFormBotBoss(player);
                            case 3 ->
                                Input.gI().createFormBotAttackPlayer(player);
                            default ->
                                Service.gI().sendThongBao(player, "Lựa chọn không hợp lệ.");
                        }
                    }
                    case ConstNpc.CAPSULE_KICH_HOAT -> {
                        try {
                            switch (select) {
                                case 0: // Áo
                                    ItemService.gI().OpenSKH(player, 1655, 0);
                                    break;
                                case 1: // Quần
                                    ItemService.gI().OpenSKH(player, 1655, 1);
                                    break;
                                case 2: // Găng
                                    ItemService.gI().OpenSKH(player, 1655, 2);
                                    break;
                                case 3: // Giày
                                    ItemService.gI().OpenSKH(player, 1655, 3);
                                    break;
                                case 4: // Rada
                                    ItemService.gI().OpenSKH(player, 1655, 4);
                                    break;
                                default:
                                    Service.gI().sendThongBao(player, "Lựa chọn không hợp lệ.");
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    case 900 -> {
                        if (select == 0) {
                            Input.gI().createFormFindPlayer1(player);
                        }
                    }
                    case ConstNpc.REVENGE -> {
                        if (select == 0) {
                            PVPService.gI().acceptRevenge(player);
                        }
                    }
                    case ConstNpc.TUTORIAL_SUMMON_DRAGON -> {
                        if (select == 0) {
                            NpcService.gI().createTutorial(player, -1, SummonDragon.SUMMON_SHENRON_TUTORIAL);
                        }
                    }
                    case ConstNpc.SUMMON_SHENRON -> {
                        if (select == 0) {
                            NpcService.gI().createTutorial(player, -1, SummonDragon.SUMMON_SHENRON_TUTORIAL);
                        } else if (select == 1) {
                            SummonDragon.gI().summonShenron(player);
                        }
                    }

                    case ConstNpc.MENU_OPTION_USE_ITEM726 -> {
                        if (select == 0) {
                            SuperDivineWaterService.gI().joinMapThanhThuy(player);
                        }
                    }
                    case ConstNpc.MENU_SIEU_THAN_THUY -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMap(player, 46, -1, Util.nextInt(300, 400), 408);
                        }
                    }
                    case ConstNpc.TAP_TU_DONG_CONFIRM -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapBySpaceShip(player, player.lastMapOffline, player.lastZoneOffline, player.lastXOffline);
                        }
                    }
                    case ConstNpc.INTRINSIC -> {
                        switch (select) {
                            case 0 ->
                                openMenuTienIch(player);
                            case 1 ->
                                IntrinsicService.gI().showAllIntrinsic(player);
                            case 2 ->
                                IntrinsicService.gI().showConfirmOpen(player);
                            case 3 ->
                                IntrinsicService.gI().showConfirmOpenVip(player);
                            default -> {
                            }
                        }
                    }
                    case ConstNpc.CONFIRM_OPEN_INTRINSIC -> {
                        if (select == 0) {
                            IntrinsicService.gI().open(player);
                        }
                    }
                    case ConstNpc.CONFIRM_OPEN_INTRINSIC_VIP -> {
                        if (select == 0) {
                            IntrinsicService.gI().openVip(player);
                        }
                    }
                    case ConstNpc.CONFIRM_LEAVE_CLAN -> {
                        if (select == 0) {
                            ClanService.gI().leaveClan(player);
                        }
                    }
                    case ConstNpc.CONFIRM_NHUONG_PC -> {
                        if (select == 0) {
                            ClanService.gI().phongPc(player, (int) PLAYERID_OBJECT.get(player.id));
                        }
                    }

                    case ConstNpc.BAN_PLAYER -> {
                        if (select == 0) {
                            PlayerService.gI().banPlayer((Player) PLAYERID_OBJECT.get(player.id));
                            Service.gI().sendThongBao(player, "Ban người chơi " + ((Player) PLAYERID_OBJECT.get(player.id)).name + " thành công");
                        }
                    }
                    case ConstNpc.BUFF_PET -> {
                        if (select == 0) {
                            Player pl = (Player) PLAYERID_OBJECT.get(player.id);
                            if (pl.pet == null) {
                                PetService.gI().createNormalPet(pl);
                                Service.gI().sendThongBao(player, "Phát đệ tử cho " + ((Player) PLAYERID_OBJECT.get(player.id)).name + " thành công");
                            }
                        }
                    }
                    case ConstNpc.SUB_MENU -> {
                        Player pl = (Player) PLAYERID_OBJECT.get(player.id);
                        switch (select) {
                            case 0 ->
                                SubMenuService.gI().controller(player, (int) pl.id, SubMenuService.OTT);
                            case 1 ->
                                SubMenuService.gI().controller(player, (int) pl.id, SubMenuService.CUU_SAT);

                        }
                    }

                    case ConstNpc.BUY_BACK -> {
//                        if (select == 0) {
//                            Player pl = (Player) PLAYERID_OBJECT.get(player.id);
//                            BuyBackService.gI().buyItem(player, pl);
//                        }
                    }
                    case ConstNpc.MENU_ADMIN -> {
                        switch (select) {
                            case 0 -> {
                                for (int i = 14; i <= 20; i++) {
                                    Item item = ItemService.gI().createNewItem((short) i);
                                    InventoryService.gI().addItemBag(player, item);
                                }
                                InventoryService.gI().sendItemBags(player);
                            }
                            case 1 -> {
                                PetService.gI().createNormalPet(player, null);
                            }
                            case 2 -> {
                                if (player.isAdmin()) {
                                    System.out.println(player.name + " Đang bảo trì game!");
                                    Maintenance.gI().startSeconds(5);
                                }
                            }
                            case 3 ->
                                Input.gI().createFormFindPlayer(player);
                            case 4 ->
                                BossManager.gI().showListBoss(player);
                        }
                    }
                    case ConstNpc.CONFIRM_DISSOLUTION_CLAN -> {
                        switch (select) {
                            case 0 -> {
                                Clan clan = player.clan;
                                clan.deleteDB(clan.id);
                                Manager.CLANS.remove(clan);
                                player.clan = null;
                                player.clanMember = null;
                                ClanService.gI().sendMyClan(player);
                                ClanService.gI().sendClanId(player);
                                Service.gI().sendThongBao(player, "Đã giải tán bang hội.");
                            }

                        }
                    }
                    case 671 -> {
                        switch (select) {
                            case 0 -> {
                                long[] time = new long[]{900000, 1800000, 3600000, 86400000, 259200000, 604800000, 1296000000};
                                var bb = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId);
                                String[] subName = bb.name.split("");
                                byte level = Byte.parseByte(subName[subName.length - 1]);
                                player.LearnSkill.Time = time[level - 1] + System.currentTimeMillis();
                                player.nPoint.tiemNang -= player.LearnSkill.Potential;
                                Service.gI().point(player);
                                Service.gI().ClosePanel(player);
                                NpcService.gI().createTutorial(player, NpcService.gI().getAvatar(13 + player.gender), "Con đã học thành công, hãy cố gắng chờ đợi nha");
                                break;
                            }
                            case 1 -> {

                                break;
                            }
                        }
                    }
                    case ConstNpc.event3 -> {
                        Item gm = InventoryService.gI().findItemBag(player, 1505);
                        int giayMauCount = (gm != null) ? gm.quantity : 0;

                        if (giayMauCount < 99) {
                            Service.gI().sendThongBaoOK(player, "Bạn chưa có đủ 99 Giấy Màu");
                            break;
                        }

                        InventoryService.gI().subQuantityItemsBag(player, gm, 99);

                        Item tv = ItemService.gI().createNewItem((short) 1506, 1);
                        InventoryService.gI().addItemBag(player, tv);
                        InventoryService.gI().sendItemBags(player);

                        break;
                    }
                    case ConstNpc.event3_1 -> {
                        Item socola = InventoryService.gI().findItemBag(player, 1507);
                        Item hoahonggiay = InventoryService.gI().findItemBag(player, 1508);
                        Item notrangtri = InventoryService.gI().findItemBag(player, 1509);
                        Item hopdungqua = InventoryService.gI().findItemBag(player, 1506);

                        if (socola == null || socola.quantity < 5) {
                            Service.gI().sendThongBaoOK(player, "Bạn chưa đủ 5 Socola.");
                            break;
                        }

                        if (hoahonggiay == null || hoahonggiay.quantity < 30) {
                            Service.gI().sendThongBaoOK(player, "Bạn chưa đủ 30 Hoa Hồng Giấy.");
                            break;
                        }

                        if (notrangtri == null || notrangtri.quantity < 1) {
                            Service.gI().sendThongBaoOK(player, "Bạn chưa đủ 1 Nơ Trang Trí.");
                            break;
                        }

                        if (hopdungqua == null || hopdungqua.quantity < 1) {
                            Service.gI().sendThongBaoOK(player, "Bạn chưa đủ 1 Hộp Đựng Quà.");
                            break;
                        }

                        switch (select) {
                            case 0 -> {
                                Item hopqua_01 = ItemService.gI().createNewItem((short) 1510, 1);
                                InventoryService.gI().subQuantityItemsBag(player, socola, 5);
                                InventoryService.gI().subQuantityItemsBag(player, hoahonggiay, 30);
                                InventoryService.gI().subQuantityItemsBag(player, hopdungqua, 1);
                                InventoryService.gI().addItemBag(player, hopqua_01);
                                InventoryService.gI().sendItemBags(player);

                                Service.gI().sendThongBaoOK(player, "Bạn nhận được quà");
                            }
                            case 1 -> {
                                Item hopqua_02 = ItemService.gI().createNewItem((short) 1511, 1);
                                InventoryService.gI().subQuantityItemsBag(player, socola, 5);
                                InventoryService.gI().subQuantityItemsBag(player, hoahonggiay, 30);
                                InventoryService.gI().subQuantityItemsBag(player, hopdungqua, 1);
                                InventoryService.gI().subQuantityItemsBag(player, notrangtri, 1);
                                InventoryService.gI().addItemBag(player, hopqua_02);
                                InventoryService.gI().sendItemBags(player);

                                Service.gI().sendThongBaoOK(player, "Bạn nhận được quà");
                            }

                        }

                        break;
                    }

                    case ConstNpc.CONFIRM_REMOVE_ALL_ITEM_LUCKY_ROUND -> {
                        if (select == 0) {
                            for (int i = 0; i < player.inventory.itemsBoxCrackBall.size(); i++) {
                                player.inventory.itemsBoxCrackBall.set(i, ItemService.gI().createItemNull());
                            }
                            player.inventory.itemsBoxCrackBall.clear();
                            Service.gI().sendThongBao(player, "Đã xóa hết vật phẩm trong rương");
                        }
                    }

                    case ConstNpc.MENU_FIND_PLAYER -> {
                        Player p = (Player) PLAYERID_OBJECT.get(player.id);
                        if (p != null) {
                            switch (select) {
                                case 0 -> {
                                    if (p.zone != null) {
                                        ChangeMapService.gI().changeMapYardrat(player, p.zone, p.location.x, p.location.y);
                                    }
                                }
                                case 1 -> {
                                    if (p.zone != null) {
                                        ChangeMapService.gI().changeMap(p, player.zone, player.location.x, player.location.y);
                                    }
                                }
                                case 2 ->
                                    Input.gI().createFormChangeName(player, p);
                                case 3 -> {
                                    String[] selects = new String[]{"Đồng ý", "Hủy"};
                                    NpcService.gI().createMenuConMeo(player, ConstNpc.BAN_PLAYER, -1,
                                            "Bạn có chắc chắn muốn ban " + p.name, selects, p);
                                }
                                case 4 -> {
                                    Service.gI().sendThongBao(player, "Kik người chơi " + p.name + " thành công");
                                    Client.gI().getPlayers().remove(p);
                                    Client.gI().kickSession(p.getSession());
                                }
                            }
                        }
                    }
                    case ConstNpc.CONFIRM_TELE_NAMEC -> {
                        if (select == 0) {
                            NgocRongNamecService.gI().teleportToNrNamec(player);
                            player.inventory.subGold(10_000_000);
                            Service.gI().sendMoney(player);
                        }
                    }
                    case ConstNpc.MA_BAO_VE -> {
                        if (select == 0) {
                            if (player.mbv == 0) {
                                if (player.inventory.gold >= 500_000) {
                                    player.inventory.gold -= 500_000;
                                    Service.gI().sendMoney(player);
                                    player.mbv = player.idMark.getMbv();
                                    player.baovetaikhoan = true;
                                    Service.gI().sendThongBao(player, "Kích hoạt thành công, tài khoản đang được bảo vệ");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ tiền để kích hoạt bảo vệ tài khoản");
                                }
                            } else {
                                if (player.baovetaikhoan) {
                                    player.baovetaikhoan = false;
                                    Service.gI().sendThongBao(player, "Chức năng bảo vệ tài khoản đang tắt");
                                } else {
                                    player.baovetaikhoan = true;
                                    Service.gI().sendThongBao(player, "Tài khoản đang được bảo vệ");
                                }
                            }
                        }
                    }
                    case ConstNpc.UP_TOP_ITEM -> {
                        if (select == 0) {
                            if (player.inventory.gold >= 5_000_000 && player.idMark.getIdItemUpTop() != -1) {
                                nro.models.shop_ky_gui.ConsignItem it = ConsignShopService.gI().getItemBuy(player.idMark.getIdItemUpTop());
                                if (it == null || it.isBuy) {
                                    Service.gI().sendThongBao(player, "Vật phẩm không tồn tại hoặc đã được bán");
                                    return;
                                }
                                if (it.player_sell != player.id) {
                                    Service.gI().sendThongBao(player, "Vật phẩm không thuộc quyền sở hữu");
                                    ConsignShopService.gI().openShopKyGui(player);
                                    return;
                                }
                                player.inventory.subGold(5_000_000);
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Thành công");
                                it.isUpTop += 1;
                                ConsignShopService.gI().openShopKyGui(player);
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ vàng");
                                player.idMark.setIdItemUpTop(-1);
                            }
                        }
                    }
                    case ConstNpc.RUONG_GO -> {
                        int i = player.indexWoodChest;
                        if (i < 0) {
                            return;
                        }
                        Item itemWoodChest = player.itemsWoodChest.get(i);
                        player.indexWoodChest--;
                        String info = "|1|" + itemWoodChest.template.name;
                        String info2 = "\n|2|";
                        if (!itemWoodChest.itemOptions.isEmpty()) {
                            for (Item.ItemOption io : itemWoodChest.itemOptions) {
                                if (io.optionTemplate.id != 102 && io.optionTemplate.id != 73) {
                                    info2 += io.getOptionString() + "\n";
                                }
                            }
                        }
                        info = (info2.length() > "\n|2|".length() ? (info + info2).trim() : info.trim()) + "\n|0|" + itemWoodChest.template.description;
                        NpcService.gI().createMenuConMeo(player, ConstNpc.RUONG_GO, -1, "Bạn nhận được\n"
                                + info.trim(), "OK" + (i > 0 ? " [" + i + "]" : ""));
                    }
                    case ConstNpc.MENU_XUONG_TANG_DUOI -> {
                        if (player.fightMabu.pointMabu >= player.fightMabu.POINT_MAX && player.zone.map.mapId != 120) {
                            ChangeMapService.gI().changeMap(player, player.zone.map.mapIdNextMabu((short) player.zone.map.mapId), -1, -1, 100);
                        }
                    }
                    case 9900 -> {
                        switch (select) {
                            case 0 -> {
                                player.isAutoBuffPea = !player.isAutoBuffPea;
                                Service.gI().sendThongBao(player, "Auto Buff Đậu (Sư phụ & Đệ tử): " + (player.isAutoBuffPea ? "[Bật]" : "[Tắt]"));
                                openMenuTienIch(player);
                            }
                            case 1 -> {
                                player.isAutoCleanBag = !player.isAutoCleanBag;
                                if (player.isAutoCleanBag) {
                                    player.autoCleanTrashAndSellItems();
                                }
                                Service.gI().sendThongBao(player, "Auto Treo Quái (Chống đầy túi & tự bán đồ rác): " + (player.isAutoCleanBag ? "[Bật]" : "[Tắt]"));
                                openMenuTienIch(player);
                            }
                            case 2 -> {
                                nro.models.server.ServerNotify.gI().sendActiveBossesToPlayer(player);
                                BossManager.gI().showListBoss(player);
                            }
                            case 3 -> {
                                openMenuCongNhanh(player);
                            }
                            case 4 -> {
                                openMenuQuanLyDeTu(player);
                            }
                            case 5 -> {
                                openMenuBagTab(player);
                            }
                            case 6 -> {
                                Service.gI().openFlagUI(player);
                            }
                        }
                    }
                    case 9903 -> {
                        int totalTabs = nro.models.player.Inventory.NUM_QUICK_TABS;
                        if (select >= 0 && select < totalTabs) {
                            if (player.inventory.moveItemMode && player.inventory.pendingMoveBagIndex >= 0) {
                                InventoryService.gI().moveItemToTab(player, player.inventory.pendingMoveBagIndex, select);
                                return;
                            }
                            player.inventory.switchBagTab(select);
                            InventoryService.gI().sendItemBags(player);
                            InventoryService.gI().sendItemBody(player);
                            Service.gI().sendThongBao(player, "Đã chuyển sang Trang " + (select + 1) + "/5 (Đang có "
                                    + player.inventory.getUsedSlotsInTab(select) + "/120 món)!");
                        } else if (select == totalTabs) {
                            player.inventory.moveItemMode = !player.inventory.moveItemMode;
                            player.inventory.pendingMoveBagIndex = -1;
                            InventoryService.gI().sendItemBags(player);
                            if (player.inventory.moveItemMode) {
                                Service.gI().sendThongBao(player, "Đã BẬT chế độ Chuyển Đồ! Mở Hành trang, bấm vào món đồ cần chuyển rồi bấm nút 1★..5★ ở hàng đầu.");
                            } else {
                                Service.gI().sendThongBao(player, "Đã TẮT chế độ Chuyển Đồ giữa các Tab!");
                            }
                        } else if (select == totalTabs + 1) {
                            int moved = InventoryService.gI().moveUtilityItemsToFirstTab(player);
                            if (moved > 0) {
                                Service.gI().sendThongBao(player, "Đã gom " + moved + " vật phẩm tiện ích (Đậu, Bông tai, Capsule, Vàng, Ngọc rồng) về Trang 1!");
                            } else {
                                Service.gI().sendThongBao(player, "Các vật phẩm tiện ích đều đã nằm ở Trang 1!");
                            }
                        } else {
                            openMenuTienIch(player);
                        }
                    }
                    case 9904 -> {
                        int totalTabs = nro.models.player.Inventory.NUM_QUICK_TABS;
                        int srcIdx = player.inventory.pendingMoveBagIndex;
                        if (select >= 0 && select < totalTabs) {
                            if (srcIdx >= 0 && srcIdx < player.inventory.itemsBag.size()) {
                                InventoryService.gI().moveItemToTab(player, srcIdx, select);
                            } else {
                                Service.gI().sendThongBao(player, "Chưa chọn vật phẩm cần chuyển!");
                            }
                        } else if (select == totalTabs) {
                            int moved = InventoryService.gI().moveUtilityItemsToFirstTab(player);
                            if (moved > 0) {
                                Service.gI().sendThongBao(player, "Đã gom " + moved + " vật phẩm tiện ích về Trang 1!");
                            } else {
                                Service.gI().sendThongBao(player, "Các vật phẩm tiện ích đều đã nằm ở Trang 1!");
                            }
                        } else if (select == totalTabs + 1) {
                            player.inventory.moveItemMode = false;
                            player.inventory.pendingMoveBagIndex = -1;
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, "Đã TẮT chế độ Chuyển Đồ giữa các Tab!");
                        }
                    }
                    case 9901 -> {
                        switch (select) {
                            case 0 -> {
                                player.nPoint.increasePoint((byte) 0, (short) 1000);
                                openMenuCongNhanh(player);
                            }
                            case 1 -> {
                                player.nPoint.increasePoint((byte) 0, (short) 30000);
                                openMenuCongNhanh(player);
                            }
                            case 2 -> {
                                player.nPoint.increasePoint((byte) 1, (short) 1000);
                                openMenuCongNhanh(player);
                            }
                            case 3 -> {
                                player.nPoint.increasePoint((byte) 1, (short) 30000);
                                openMenuCongNhanh(player);
                            }
                            case 4 -> {
                                player.nPoint.increasePoint((byte) 2, (short) 1000);
                                openMenuCongNhanh(player);
                            }
                            case 5 -> {
                                player.nPoint.increasePoint((byte) 2, (short) 30000);
                                openMenuCongNhanh(player);
                            }
                            case 6 -> {
                                openMenuTienIch(player);
                            }
                        }
                    }
                    case 9902 -> {
                        if (player.pet == null) {
                            if (select == 0) {
                                PetService.gI().createNormalPet(player);
                                Service.gI().sendThongBao(player, "Bạn vừa nhận được Đệ tử miễn phí!");
                            } else {
                                openMenuTienIch(player);
                            }
                            return;
                        }
                        switch (select) {
                            case 0 ->
                                player.pet.changeStatus(nro.models.player.Pet.FOLLOW);
                            case 1 ->
                                player.pet.changeStatus(nro.models.player.Pet.PROTECT);
                            case 2 ->
                                player.pet.changeStatus(nro.models.player.Pet.ATTACK);
                            case 3 ->
                                player.pet.changeStatus(nro.models.player.Pet.GOHOME);
                            case 4 ->
                                player.pet.transform();
                            case 5 -> {
                                boolean found = false;
                                for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                                    Item item = player.inventory.itemsBag.get(i);
                                    if (item != null && item.isNotNullItem() && item.template.type == 25) {
                                        found = true;
                                        if (player.pet.nPoint != null && player.pet.nPoint.power >= 1500000) {
                                            Item old = InventoryService.gI().putItemBody(player.pet, item);
                                            player.inventory.itemsBag.set(i, old);
                                            InventoryService.gI().sendItemBags(player);
                                            InventoryService.gI().sendItemBody(player);
                                            Service.gI().Send_Caitrang(player.pet);
                                            Service.gI().Send_Caitrang(player);
                                            Service.gI().showInfoPet(player);
                                            Service.gI().sendThongBao(player, "Đã trang bị " + item.template.name + " cho đệ tử!");
                                        } else {
                                            Service.gI().sendThongBaoOK(player, "Đệ tử cần đạt 1tr5 sức mạnh để trang bị.");
                                        }
                                        break;
                                    }
                                }
                                if (!found) {
                                    Service.gI().sendThongBao(player, "Không tìm thấy Sách Tuyệt Kỹ trong hành trang!");
                                }
                            }
                            case 6 ->
                                openMenuTienIch(player);
                        }
                    }
                }
            }
        };
    }

    public static void openMenuTienIch(Player player) {
        int curTab = player.inventory.currentBagTab + 1;
        int totalTabs = player.inventory.getTotalBagTabs();
        String npcSay = "|7|=== BẢNG ĐIỀU KHIỂN TIỆN ÍCH ===\n"
                + "|2|Auto Buff Đậu (Sư phụ & Đệ): " + (player.isAutoBuffPea ? "|1|[Đang Bật]" : "|7|[Đang Tắt]") + "\n"
                + "|2|Auto Treo Quái (Chống đầy túi): " + (player.isAutoCleanBag ? "|1|[Đang Bật]" : "|7|[Đang Tắt]") + "\n"
                + "|2|Hành trang: |1|Trang " + curTab + "/" + totalTabs + " (" + player.inventory.getUsedSlotsInBag() + "/" + player.inventory.itemsBag.size() + " ô - 200 ô/Trang)\n"
                + "|2|Điểm Săn Boss hiện có: |1|" + player.pointSanBoss + " điểm\n"
                + "|0|(Mẹo: Bấm thẳng nút 1★ 2★ 3★ 4★ 5★ ở đầu Hành trang để đổi Trang nhanh)";
        NpcService.gI().createMenuConMeo(player, 9900, -1, npcSay,
                "Auto Đậu\n" + (player.isAutoBuffPea ? "[Tắt]" : "[Bật]"),
                "Treo Quái\n" + (player.isAutoCleanBag ? "[Tắt]" : "[Bật]"),
                "Danh sách\nBoss",
                "Cộng nhanh\nChỉ số",
                "Quản lý\nĐệ tử",
                "Tab Túi Đồ\n[Tab " + curTab + "/" + totalTabs + "]",
                "Đổi Cờ\nPK",
                "Đóng");
    }

    public static void openMenuBagTab(Player player) {
        int totalTabs = nro.models.player.Inventory.NUM_QUICK_TABS;
        int curTab = player.inventory.currentBagTab + 1;
        int totalUsed = player.inventory.getUsedSlotsInBag();
        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== QUẢN LÝ & CHUYỂN ĐỒ GIỮA CÁC TAB (1 - 2 - 3 - 4 - 5) ===\n");
        sb.append("|2|Đang mở: |1|Trang ").append(curTab).append("/").append(totalTabs)
                .append(" |2|(Tổng vật phẩm: |1|").append(totalUsed).append("/600 ô|2|)\n");
        sb.append("|2|Chế độ Chuyển Đồ: ").append(player.inventory.moveItemMode ? "|1|[ĐANG BẬT]\n" : "|7|[ĐANG TẮT]\n");
        for (int t = 0; t < totalTabs; t++) {
            int tabUsed = player.inventory.getUsedSlotsInTab(t);
            sb.append(t == player.inventory.currentBagTab ? "|1|► " : "|2|• ")
                    .append("Trang ").append(t + 1).append(": ").append(tabUsed).append("/120 ô");
            if (t % 2 == 1 || t == totalTabs - 1) {
                sb.append("\n");
            } else {
                sb.append("   ");
            }
        }
        sb.append("|0|(Mẹo: Bấm nút thứ 6 [+] ở đầu Hành trang -> Bấm vào món đồ -> Bấm nút 1★..5★ để chuyển Tab!)");

        String[] options = new String[totalTabs + 3];
        for (int t = 0; t < totalTabs; t++) {
            int tabUsed = player.inventory.getUsedSlotsInTab(t);
            options[t] = (t == player.inventory.currentBagTab ? "[Đang mở]\n" : "")
                    + "Trang " + (t + 1) + "\n(" + tabUsed + "/120 ô)";
        }
        options[totalTabs] = "Chuyển Đồ\n" + (player.inventory.moveItemMode ? "[Tắt]" : "[Bật]");
        options[totalTabs + 1] = "Gom Tiện Ích\nvề Trang 1";
        options[totalTabs + 2] = "Menu\nTiện Ích";
        NpcService.gI().createMenuConMeo(player, 9903, -1, sb.toString(), options);
    }

    public static void openMenuMoveSingleItem(Player player, int bagIndex) {
        if (player == null || player.inventory == null || bagIndex < 0 || bagIndex >= player.inventory.itemsBag.size()) {
            return;
        }
        Item item = player.inventory.itemsBag.get(bagIndex);
        if (item == null || !item.isNotNullItem() || item.template == null) {
            openMenuBagTab(player);
            return;
        }
        player.inventory.pendingMoveBagIndex = bagIndex;
        int totalTabs = nro.models.player.Inventory.NUM_QUICK_TABS;
        int srcTab = bagIndex / nro.models.player.Inventory.BAG_VIEW_SIZE;
        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== CHUYỂN VẬT PHẨM SANG TAB KHÁC ===\n");
        sb.append("|2|Vật phẩm đang chọn: |1|").append(item.template.name)
                .append(item.quantity > 1 ? " (x" + item.quantity + ")" : "").append("\n");
        sb.append("|2|Vị trí hiện tại: |1|Trang ").append(srcTab + 1).append("\n");
        sb.append("|0|Hãy chọn Trang đích bên dưới để chuyển vật phẩm này tới:");

        String[] options = new String[totalTabs + 3];
        for (int t = 0; t < totalTabs; t++) {
            int tabUsed = player.inventory.getUsedSlotsInTab(t);
            options[t] = (t == srcTab ? "[Trang hiện tại]\n" : "Chuyển sang\n")
                    + "Trang " + (t + 1) + " (" + tabUsed + "/120)";
        }
        options[totalTabs] = "Gom Tiện Ích\nvề Trang 1";
        options[totalTabs + 1] = "Tắt chế độ\nChuyển Đồ";
        options[totalTabs + 2] = "Đóng";
        NpcService.gI().createMenuConMeo(player, 9904, -1, sb.toString(), options);
    }

    public static void openMenuCongNhanh(Player player) {
        String npcSay = "|7|=== CỘNG NHANH CHỈ SỐ GỐC ===\n"
                + "|2|Tiềm năng hiện có: " + Util.numberToMoney(player.nPoint.tiemNang) + "\n"
                + "|1|HP gốc: " + Util.numberToMoney(player.nPoint.hpg) + " / " + Util.numberToMoney(player.nPoint.getHpMpLimit()) + "\n"
                + "|1|KI gốc: " + Util.numberToMoney(player.nPoint.mpg) + " / " + Util.numberToMoney(player.nPoint.getHpMpLimit()) + "\n"
                + "|1|Sức đánh gốc: " + Util.numberToMoney(player.nPoint.dameg) + " / " + Util.numberToMoney(player.nPoint.getDameLimit());
        NpcService.gI().createMenuConMeo(player, 9901, -1, npcSay,
                "+20.000\nHP gốc",
                "Max HP\nTới giới hạn",
                "+20.000\nKI gốc",
                "Max KI\nTới giới hạn",
                "+1.000\nSức đánh",
                "Max S.Đánh\nTới giới hạn",
                "Quay lại");
    }

    public static void openMenuQuanLyDeTu(Player player) {
        if (player.pet == null) {
            NpcService.gI().createMenuConMeo(player, 9902, -1,
                    "Bạn hiện chưa có Đệ tử.\nBạn có muốn nhận ngay 1 Đệ tử miễn phí không?",
                    "Nhận Đệ tử\nMiễn phí",
                    "Quay lại");
            return;
        }
        String npcSay = "|7|=== QUẢN LÝ ĐỆ TỬ ===\n"
                + "|2|Tên: " + player.pet.name + " | Sức mạnh: " + Util.numberToMoney(player.pet.nPoint.power) + "\n"
                + "|1|HP: " + Util.numberToMoney(player.pet.nPoint.hpMax)
                + " | KI: " + Util.numberToMoney(player.pet.nPoint.mpMax)
                + " | SĐ: " + Util.numberToMoney(player.pet.nPoint.dame);
        NpcService.gI().createMenuConMeo(player, 9902, -1, npcSay,
                "Đi theo",
                "Bảo vệ",
                "Tấn công",
                "Về nhà",
                "Biến hình",
                "Mặc Sách\nTuyệt Kỹ",
                "Quay lại");
    }

}
