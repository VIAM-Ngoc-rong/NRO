package nro.models.npc;

import java.util.ArrayList;
import nro.models.skill.Skill;
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
                                } else if (!nro.models.event.EventManager.NGU_HANH_SON) {
                                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                            "A di đà phật! Sự kiện Ngũ Hành Sơn hiện đang tạm đóng.\nKhi nào sự kiện khai mở, bần tăng sẽ dẫn thí chủ đi tu luyện!",
                                            "Đóng");
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
                                } else if (nro.models.event.EventManager.NGU_HANH_SON) {
                                    ChangeMapService.gI().changeMapBySpaceShip(player, 122, -1, -1);
                                } else {
                                    Service.gI().sendThongBao(player, "Sự kiện Ngũ Hành Sơn hiện đang đóng!");
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
                                openMenuDoiDoNhanh(player);
                            }
                            case 1 -> {
                                openMenuKhoaTrangBi(player);
                            }
                            case 2 -> {
                                openMenuAutoTreoMay(player);
                            }
                            case 3 -> {
                                nro.models.server.ServerNotify.gI().sendActiveBossesToPlayer(player);
                                BossManager.gI().showListBoss(player);
                            }
                            case 4 -> {
                                openMenuQuanLyDeTu(player);
                            }
                            case 5 -> {
                                openMenuCongNhanh(player);
                            }
                            case 6 -> {
                                if (player.isAutoHuntBoss) {
                                    player.stopAutoHuntBoss();
                                    openMenuTienIch(player);
                                } else {
                                    Service.gI().openFlagUI(player);
                                }
                            }
                        }
                    }
                    case 9916 -> {
                        switch (select) {
                            case 0 -> {
                                player.isAutoBuffPea = !player.isAutoBuffPea;
                                Service.gI().sendThongBao(player, "Auto Buff Đậu (Sư phụ & Đệ tử): " + (player.isAutoBuffPea ? "[Bật]" : "[Tắt]"));
                                openMenuAutoTreoMay(player);
                            }
                            case 1 -> {
                                player.isAutoCleanBag = !player.isAutoCleanBag;
                                if (player.isAutoCleanBag) {
                                    player.autoCleanTrashAndSellItems();
                                }
                                Service.gI().sendThongBao(player, "Auto Treo Quái (Chống đầy túi & tự bán đồ rác): " + (player.isAutoCleanBag ? "[Bật]" : "[Tắt]"));
                                openMenuAutoTreoMay(player);
                            }
                            case 2 -> {
                                boolean enableBoth = !(player.isAutoBuffPea && player.isAutoCleanBag);
                                player.isAutoBuffPea = enableBoth;
                                player.isAutoCleanBag = enableBoth;
                                if (enableBoth) {
                                    player.autoCleanTrashAndSellItems();
                                }
                                Service.gI().sendThongBao(player, (enableBoth ? "Đã BẬT" : "Đã TẮT") + " cả Auto Buff Đậu & Auto Treo Quái!");
                                openMenuAutoTreoMay(player);
                            }
                            case 3 -> {
                                player.buyAndActivateAutoHuntBossTime();
                                openMenuAutoTreoMay(player);
                            }
                            case 4 -> {
                                nro.models.server.ServerNotify.gI().sendActiveBossesToPlayer(player);
                                BossManager.gI().showListBoss(player);
                            }
                            case 5 -> {
                                openMenuSelectAutoHuntSkills(player);
                            }
                            default -> {
                                openMenuTienIch(player);
                            }
                        }
                    }
                    case 9907 -> {
                        switch (select) {
                            case 0 -> openMenuMacNhanhSetDo(player, false);
                            case 1 -> openMenuMacNhanhSetDo(player, true);
                            case 2 -> openMenuChonSetDeLuu(player);
                            case 3 -> openMenuKhoaTrangBi(player);
                            case 4 -> {
                                int count = lockAllEquippedItems(player, true);
                                Service.gI().sendThongBao(player, "Đã KHÓA bảo vệ tất cả " + count + " trang bị đang mặc trên Sư phụ & Đệ tử!");
                                openMenuDoiDoNhanh(player);
                            }
                            case 5 -> openMenuBagTab(player);
                            default -> openMenuTienIch(player);
                        }
                    }
                    case 9908 -> {
                        if (select >= 0 && select < InventoryService.MAX_EQUIP_SETS) {
                            String msg = InventoryService.gI().equipSetForTarget(player, select + 1, false);
                            Service.gI().sendThongBao(player, msg);
                        } else if (select == InventoryService.MAX_EQUIP_SETS) {
                            openMenuChonSetDeLuu(player);
                        } else {
                            openMenuDoiDoNhanh(player);
                        }
                    }
                    case 9909 -> {
                        if (select >= 0 && select < InventoryService.MAX_EQUIP_SETS) {
                            String msg = InventoryService.gI().equipSetForTarget(player, select + 1, true);
                            Service.gI().sendThongBao(player, msg);
                        } else if (select == InventoryService.MAX_EQUIP_SETS) {
                            openMenuChonSetDeLuu(player);
                        } else {
                            openMenuDoiDoNhanh(player);
                        }
                    }
                    case 9911 -> {
                        if (select >= 0 && select < InventoryService.MAX_EQUIP_SETS) {
                            openMenuChiTietThietLapSet(player, select + 1);
                        } else {
                            openMenuDoiDoNhanh(player);
                        }
                    }
                    case 9912 -> {
                        int setIdx = getPendingEquipSetIndex(player);
                        switch (select) {
                            case 0 -> {
                                String msg = InventoryService.gI().saveCurrentBodyToEquipSet(player, setIdx, false);
                                Service.gI().sendThongBao(player, msg);
                                openMenuChiTietThietLapSet(player, setIdx);
                            }
                            case 1 -> {
                                String msg = InventoryService.gI().saveCurrentBodyToEquipSet(player, setIdx, true);
                                Service.gI().sendThongBao(player, msg);
                                openMenuChiTietThietLapSet(player, setIdx);
                            }
                            case 2 -> {
                                String msg = InventoryService.gI().equipSetForTarget(player, setIdx, false);
                                Service.gI().sendThongBao(player, msg);
                            }
                            case 3 -> {
                                String msg = InventoryService.gI().equipSetForTarget(player, setIdx, true);
                                Service.gI().sendThongBao(player, msg);
                            }
                            case 4 -> {
                                InventoryService.gI().clearEquipSet(player, setIdx);
                                Service.gI().sendThongBao(player, "Đã xóa thiết lập của Set Đồ số " + setIdx + "!");
                                openMenuChiTietThietLapSet(player, setIdx);
                            }
                            default -> openMenuChonSetDeLuu(player);
                        }
                    }
                    case 9910 -> {
                        switch (select) {
                            case 0 -> openMenuKhoaDoSuPhu(player);
                            case 1 -> openMenuKhoaDoDeTu(player);
                            case 2 -> openMenuKhoaDoHanhTrang(player, 0);
                            case 3 -> {
                                int count = lockAllEquippedItems(player, true);
                                Service.gI().sendThongBao(player, "Đã KHÓA bảo vệ tất cả " + count + " trang bị đang mặc trên Sư phụ & Đệ tử!");
                                openMenuKhoaTrangBi(player);
                            }
                            case 4 -> {
                                int count = lockAllEquippedItems(player, false);
                                Service.gI().sendThongBao(player, "Đã MỞ KHÓA cho " + count + " trang bị đang mặc trên Sư phụ & Đệ tử!");
                                openMenuKhoaTrangBi(player);
                            }
                            default -> openMenuDoiDoNhanh(player);
                        }
                    }
                    case 9913 -> {
                        List<Item> bodyItems = getNonNullItems(player.inventory.itemsBody);
                        if (select >= 0 && select < bodyItems.size()) {
                            Item target = bodyItems.get(select);
                            boolean locked = InventoryService.toggleItemLock(target);
                            InventoryService.gI().sendItemBody(player);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, (locked ? "Đã KHÓA bảo vệ: " : "Đã MỞ KHÓA: ") + target.template.name);
                            openMenuKhoaDoSuPhu(player);
                        } else {
                            openMenuKhoaTrangBi(player);
                        }
                    }
                    case 9914 -> {
                        if (player.pet == null || player.pet.inventory == null) {
                            openMenuKhoaTrangBi(player);
                            return;
                        }
                        List<Item> petItems = getNonNullItems(player.pet.inventory.itemsBody);
                        if (select >= 0 && select < petItems.size()) {
                            Item target = petItems.get(select);
                            boolean locked = InventoryService.toggleItemLock(target);
                            InventoryService.gI().sendItemBody(player);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI(). showInfoPet(player);
                            Service.gI().sendThongBao(player, (locked ? "Đã KHÓA bảo vệ đồ Đệ tử: " : "Đã MỞ KHÓA đồ Đệ tử: ") + target.template.name);
                            openMenuKhoaDoDeTu(player);
                        } else {
                            openMenuKhoaTrangBi(player);
                        }
                    }
                    case 9915 -> {
                        int page = getPendingLockBagPage(player);
                        List<Item> bagEquips = getLockableBagItemsInCurrentTab(player);
                        int pageSize = 6;
                        int totalPages = Math.max(1, (bagEquips.size() + pageSize - 1) / pageSize);
                        page = Math.max(0, Math.min(page, totalPages - 1));
                        int start = page * pageSize;
                        int end = Math.min(bagEquips.size(), start + pageSize);
                        int countOnPage = end - start;
                        if (select >= 0 && select < countOnPage) {
                            Item target = bagEquips.get(start + select);
                            boolean locked = InventoryService.toggleItemLock(target);
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendThongBao(player, (locked ? "Đã KHÓA bảo vệ: " : "Đã MỞ KHÓA: ") + target.template.name);
                            openMenuKhoaDoHanhTrang(player, page);
                        } else if (totalPages > 1 && select == countOnPage) {
                            openMenuKhoaDoHanhTrang(player, (page + 1) % totalPages);
                        } else {
                            openMenuKhoaTrangBi(player);
                        }
                    }
                    case 9905 -> {
                        int bossIdx = player.pendingAutoBossIndex;
                        nro.models.boss.Boss boss = BossManager.gI().getBoss(bossIdx);
                        switch (select) {
                            case 0 -> {
                                if (boss != null) {
                                    player.toggleAutoHuntBoss(bossIdx);
                                    if (player.hasActiveAutoHuntBossTime()) {
                                        BossManager.gI().showListBoss(player);
                                    } else {
                                        openMenuSelectBossAction(player, bossIdx);
                                    }
                                }
                            }
                            case 1 -> {
                                player.toggleAutoHuntAllBoss();
                                if (!player.hasActiveAutoHuntBossTime()) {
                                    openMenuSelectBossAction(player, bossIdx);
                                }
                            }
                            case 2 -> {
                                player.buyAndActivateAutoHuntBossTime();
                                openMenuSelectBossAction(player, bossIdx);
                            }
                            case 3 -> {
                                openMenuSelectAutoHuntSkills(player);
                            }
                            case 4 -> {
                                if (boss != null && boss.zone != null && !boss.isDie()) {
                                    int targetMapId = boss.zone.map.mapId;
                                    if (!ChangeMapService.gI().canPlayerJoinMap(player, targetMapId)) {
                                        Service.gI().sendThongBao(player, "Bạn chưa qua được bản đồ " + boss.zone.map.mapName + " (chưa hoàn thành nhiệm vụ yêu cầu)!");
                                        return;
                                    }
                                    nro.models.map.Zone zone0 = nro.models.map.service.MapService.gI().getZoneByMapIDAndZoneID(targetMapId, 0);
                                    if (zone0 != null) {
                                        if (player.zone != null && !nro.models.map.service.MapService.gI().isHome(player.zone.map.mapId)
                                                && !nro.models.map.service.MapService.gI().isMapPhoBan(player.zone.map.mapId)
                                                && !nro.models.map.service.MapService.gI().isMapMaBu(player.zone.map.mapId)
                                                && !nro.models.map.service.MapService.gI().isMapBlackBallWar(player.zone.map.mapId)
                                                && !nro.models.map.service.MapService.gI().isMapOffline(player.zone.map.mapId)) {
                                            player.mapBeforeCapsule = player.zone;
                                        }
                                        player.changeMapVIP = true;
                                        ChangeMapService.gI().changeMapBySpaceShip(player, zone0, -1);
                                        Service.gI().sendThongBao(player, "Đã dịch chuyển tới " + boss.zone.map.mapName + " (Khu 0). Hãy dò từ Khu 1 trở đi để tìm " + boss.name + "!");
                                    } else {
                                        Service.gI().sendThongBao(player, "Không thể dịch chuyển đến " + boss.zone.map.mapName + "!");
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Boss này hiện chưa xuất hiện! Bạn có thể bấm 'Thêm Vào DS Tự Săn' để tự đến khi Boss hồi sinh.");
                                }
                            }
                            case 5 -> {
                                if (player.isAutoHuntBoss) {
                                    player.stopAutoHuntBoss();
                                } else {
                                    BossManager.gI().showListBoss(player);
                                }
                            }
                            case 6 -> {
                                BossManager.gI().showListBoss(player);
                            }
                        }
                    }
                    case 9906 -> {
                        List<Skill> learned = new ArrayList<>();
                        if (player.playerSkill != null && player.playerSkill.skills != null) {
                            for (Skill s : player.playerSkill.skills) {
                                if (s != null && s.skillId != -1 && s.point > 0 && s.template != null
                                        && s.template.id != Skill.TRI_THUONG && s.template.id != Skill.TAI_TAO_NANG_LUONG) {
                                    learned.add(s);
                                }
                            }
                        }
                        if (select == 0) {
                            player.resetAutoHuntSkillsToShortcut();
                            openMenuSelectAutoHuntSkills(player);
                        } else if (select >= 1 && select <= learned.size()) {
                            Skill chosen = learned.get(select - 1);
                            player.toggleAutoHuntSkill(chosen.template.id);
                            openMenuSelectAutoHuntSkills(player);
                        } else if (select == learned.size() + 1) {
                            if (player.pendingAutoBossIndex >= 0) {
                                openMenuSelectBossAction(player, player.pendingAutoBossIndex);
                            } else {
                                BossManager.gI().showListBoss(player);
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
                            openMenuTienIch(player);
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
                                openMenuMacNhanhSetDo(player, true);
                            case 7 ->
                                openMenuTienIch(player);
                        }
                    }
                }
            }
        };
    }

    public static void openMenuSelectBossAction(Player player, int bossIndex) {
        if (player == null) {
            return;
        }
        nro.models.boss.Boss boss = BossManager.gI().getBoss(bossIndex);
        if (boss == null || boss.data == null || boss.data.length == 0) {
            Service.gI().sendThongBao(player, "Không tìm thấy thông tin Boss!");
            return;
        }
        player.pendingAutoBossIndex = bossIndex;
        int lv = (boss.currentLevel >= 0 && boss.currentLevel < boss.data.length) ? boss.currentLevel : 0;
        String bossName = boss.data[lv].getName();
        boolean isAlive = boss.zone != null && !boss.isDie();
        boolean isHuntingThis = player.isAutoHuntBoss && player.isMatchingAutoBoss(boss);

        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== TÙY CHỌN TỰ ĐỘNG SĂN NHIỀU BOSS ===\n");
        sb.append("|2|Boss đang chọn: |1|").append(bossName).append("\n");
        sb.append("|2|Trạng thái: ").append(isAlive
                ? ("|1|Đang xuất hiện tại " + boss.zone.map.mapName + "\n")
                : "|7|Đang chờ hồi sinh (30-60s)\n");
        sb.append("|2|Thời gian Máy Dò Săn Boss: ").append(player.hasActiveAutoHuntBossTime()
                ? ("|1|" + player.getAutoHuntBossTimeRemainingText() + "\n")
                : "|7|[Chưa kích hoạt - Cần 200Tr Vàng / 1 giờ]\n");
        sb.append("|2|DS Đang Tự Săn: ").append(player.isAutoHuntBoss
                ? ("|1|[" + player.getAutoHuntBossSummary() + "]\n")
                : "|7|[Đang Tắt]\n");
        sb.append("|2|Chiêu Săn Boss: |1|").append(player.getAutoHuntSkillSummary()).append("\n");
        sb.append("|0|(Mua Máy Dò Săn Boss tại Shop Santa hoặc bấm 'Mua Thời Gian' bên dưới - 200Tr Vàng/1h, cộng dồn khi mua nhiều lần!)");

        String opt0 = (isHuntingThis && !player.isAutoHuntAllBoss)
                ? ("Bỏ Khỏi DS\n" + bossName)
                : ("Thêm Vào DS\n" + bossName);
        String opt1 = player.isAutoHuntAllBoss ? "Tắt Săn\nTất Cả Boss" : "Bật Săn\nTất Cả Boss";
        String optBuy = "Mua Thời Gian\n(200Tr Vàng/1h)";
        String opt2 = "Chọn Chiêu\nSăn Boss";
        String opt3 = isAlive ? ("Đến Khu 0\n" + boss.zone.map.mapName) : "Đến Khu 0\n(Chờ Boss ra)";
        if (player.isAutoHuntBoss) {
            NpcService.gI().createMenuConMeo(player, 9905, -1, sb.toString(),
                    opt0, opt1, optBuy, opt2, opt3, "Tắt Toàn Bộ\nTự Săn", "Danh sách\nBoss", "Đóng");
        } else {
            NpcService.gI().createMenuConMeo(player, 9905, -1, sb.toString(),
                    opt0, opt1, optBuy, opt2, opt3, "Danh sách\nBoss", "Đóng");
        }
    }

    public static void openMenuSelectAutoHuntSkills(Player player) {
        if (player == null) {
            return;
        }
        List<Skill> learned = new ArrayList<>();
        if (player.playerSkill != null && player.playerSkill.skills != null) {
            for (Skill s : player.playerSkill.skills) {
                if (s != null && s.skillId != -1 && s.point > 0 && s.template != null
                        && s.template.id != Skill.TRI_THUONG && s.template.id != Skill.TAI_TAO_NANG_LUONG) {
                    learned.add(s);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== CÀI ĐẶT CHIÊU TỰ ĐỘNG SĂN BOSS ===\n");
        sb.append("|2|Chế độ hiện tại: |1|")
                .append(player.autoHuntCustomSkillIds.isEmpty() ? "Tự động theo Ô Kỹ Năng (Shortcut)" : "Tự chọn chiêu")
                .append("\n");
        sb.append("|2|Các chiêu đang dùng: |1|").append(player.getAutoHuntSkillSummary()).append("\n");
        sb.append("|0|(Bấm vào từng chiêu bên dưới để Bật/Tắt khi Săn Boss, hoặc chọn 'Dùng Theo Ô Kỹ Năng' để dùng tất cả chiêu bạn gán trên màn hình!)");

        List<String> menuOpts = new ArrayList<>();
        menuOpts.add("Dùng Theo\nÔ Kỹ Năng");
        for (Skill s : learned) {
            boolean active = player.isSkillSelectedForAutoHunt(s.template.id);
            menuOpts.add(s.template.name + "\n" + (active ? "[Đang Bật]" : "[Đang Tắt]"));
        }
        menuOpts.add("Quay lại");
        menuOpts.add("Đóng");
        NpcService.gI().createMenuConMeo(player, 9906, -1, sb.toString(), menuOpts.toArray(new String[0]));
    }

    public static void openMenuTienIch(Player player) {
        int maxBag = nro.models.player.Inventory.MAX_ITEMS_BAG;
        String autoStatus = (player.isAutoBuffPea && player.isAutoCleanBag) ? "[Đậu+Treo: Bật]"
                : player.isAutoBuffPea ? "[Auto Đậu: Bật]"
                : player.isAutoCleanBag ? "[Treo Quái: Bật]" : "[Đang Tắt]";
        String npcSay = "|7|=== BẢNG ĐIỀU KHIỂN TIỆN ÍCH (PHÍM X) ===\n"
                + "|2|Thời gian Server hoạt động: |1|" + nro.models.server.ServerManager.getFormattedUptimeHours() + "\n"
                + "|2|Auto Buff Đậu (Sư phụ & Đệ): " + (player.isAutoBuffPea ? "|1|[Đang Bật]" : "|7|[Đang Tắt]")
                + " |2|| Auto Treo Quái: " + (player.isAutoCleanBag ? "|1|[Đang Bật]\n" : "|7|[Đang Tắt]\n")
                + "|2|Auto Săn Boss: " + (player.isAutoHuntBoss ? ("|1|[" + player.getAutoHuntBossSummary() + "]") : "|7|[Đang Tắt]")
                + " |2|| Máy Dò Boss: " + (player.hasActiveAutoHuntBossTime() ? ("|1|" + player.getAutoHuntBossTimeRemainingText() + "\n") : "|7|[Chưa kích hoạt]\n")
                + "|2|Hành trang: |1|" + player.inventory.getUsedSlotsInBag() + "/" + maxBag + " ô |2|| Điểm Săn Boss: |1|" + player.pointSanBoss + " điểm\n"
                + "|0|(Mẹo: Chat 's1'..'s5' mặc nhanh Set 1..5 Sư phụ | Chat 'd1'..'d5' mặc nhanh Set 1..5 Đệ tử!)";
        NpcService.gI().createMenuConMeo(player, 9900, -1, npcSay,
                "Trang Bị &\nSet Đồ",
                "Khóa Bảo Vệ\nTrang Bị",
                "Auto Treo Máy\n" + autoStatus,
                "Danh Sách &\nTự Săn Boss",
                "Quản Lý\nĐệ Tử",
                "Cộng Nhanh\nChỉ Số",
                player.isAutoHuntBoss ? "Tắt Tự Săn\nBoss" : "Đổi Cờ\nPK");
    }

    public static void openMenuAutoTreoMay(Player player) {
        if (player == null) {
            return;
        }
        boolean bothOn = player.isAutoBuffPea && player.isAutoCleanBag;
        String npcSay = "|7|=== CÀI ĐẶT AUTO TREO MÁY & DỌN TÚI ===\n"
                + "|2|• Auto Buff Đậu (Sư phụ & Đệ tử): " + (player.isAutoBuffPea ? "|1|[ĐANG BẬT]\n" : "|7|[ĐANG TẮT]\n")
                + "|2|• Auto Treo Quái (Chống đầy túi & tự bán đồ rác): " + (player.isAutoCleanBag ? "|1|[ĐANG BẬT]\n" : "|7|[ĐANG TẮT]\n")
                + "|2|• Auto Săn Boss: " + (player.isAutoHuntBoss ? ("|1|[" + player.getAutoHuntBossSummary() + "]\n") : "|7|[ĐANG TẮT]\n")
                + "|2|• Thời gian Máy Dò Săn Boss: " + (player.hasActiveAutoHuntBossTime() ? ("|1|" + player.getAutoHuntBossTimeRemainingText() + "\n") : "|7|[Chưa kích hoạt - 200Tr Vàng/1h]\n")
                + "|2|• Chiêu Săn Boss: |1|" + player.getAutoHuntSkillSummary() + "\n"
                + "|0|(Lưu ý: Các trang bị đã KHÓA bảo vệ hoặc nằm trong Set Đồ 1..5 sẽ không bao giờ bị tự động bán!)";
        NpcService.gI().createMenuConMeo(player, 9916, -1, npcSay,
                "Auto Đậu\n" + (player.isAutoBuffPea ? "[Tắt]" : "[Bật]"),
                "Treo Quái\n" + (player.isAutoCleanBag ? "[Tắt]" : "[Bật]"),
                bothOn ? "Tắt Cả Hai\n(Đậu + Treo)" : "Bật Cả Hai\n(Đậu + Treo)",
                "Mua Giờ Săn\n(200Tr Vàng/1h)",
                "Danh Sách &\nTự Săn Boss",
                "Chọn Chiêu\nSăn Boss",
                "Quay lại");
    }

    private static int getPendingEquipSetIndex(Player player) {
        if (player != null && player.idMark != null && player.idMark.getTagNameShop() != null
                && player.idMark.getTagNameShop().startsWith("EQUIP_SET_")) {
            try {
                return Integer.parseInt(player.idMark.getTagNameShop().substring("EQUIP_SET_".length()));
            } catch (Exception ignored) {
            }
        }
        return 1;
    }

    private static void setPendingEquipSetIndex(Player player, int setIndex) {
        if (player != null && player.idMark != null) {
            player.idMark.setTagNameShop("EQUIP_SET_" + setIndex);
        }
    }

    private static int getPendingLockBagPage(Player player) {
        if (player != null && player.idMark != null && player.idMark.getTagNameShop() != null
                && player.idMark.getTagNameShop().startsWith("LOCK_BAG_PAGE_")) {
            try {
                return Integer.parseInt(player.idMark.getTagNameShop().substring("LOCK_BAG_PAGE_".length()));
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private static void setPendingLockBagPage(Player player, int page) {
        if (player != null && player.idMark != null) {
            player.idMark.setTagNameShop("LOCK_BAG_PAGE_" + page);
        }
    }

    public static void openMenuDoiDoNhanh(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }
        int curTab = player.inventory.currentBagTab + 1;
        int totalTabs = player.inventory.getTotalBagTabs();
        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== QUẢN LÝ TRANG BỊ: ĐỔI ĐỒ NHANH & KHÓA BẢO VỆ ===\n");
        for (int i = 1; i <= InventoryService.MAX_EQUIP_SETS; i++) {
            sb.append("|2|• Set ").append(i).append(": |1|")
                    .append(InventoryService.gI().getEquipSetSummary(player, i)).append("\n");
        }
        sb.append("|0|• Chat nhanh 's1'..'s5' (Mặc cho Sư phụ) hoặc 'd1'..'d5' (Mặc cho Đệ tử).\n")
                .append("• Trang bị đã KHÓA sẽ chống vứt nhầm, chống bán Shop và chống phân rã!");
        NpcService.gI().createMenuConMeo(player, 9907, -1, sb.toString(),
                "Mặc Nhanh\nCho Sư Phụ",
                "Mặc Nhanh\nCho Đệ Tử",
                "Thiết Lập /\nLưu Set Đồ",
                "Khóa / Mở Khóa\nTrang Bị",
                "Khóa TẤT CẢ\nĐồ Đang Mặc",
                "Quay lại");
    }

    public static void openMenuMacNhanhSetDo(Player player, boolean forPet) {
        if (player == null || player.inventory == null) {
            return;
        }
        String targetLabel = forPet ? "ĐỆ TỬ" : "SƯ PHỤ";
        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== MẶC NHANH SET ĐỒ CHO ").append(targetLabel).append(" ===\n");
        for (int i = 1; i <= InventoryService.MAX_EQUIP_SETS; i++) {
            sb.append("|2|• Set ").append(i).append(": |1|")
                    .append(InventoryService.gI().getEquipSetSummary(player, i)).append("\n");
        }
        sb.append("|0|Chọn Set đồ bên dưới để mặc ngay cho ").append(forPet ? "Đệ tử" : "Sư phụ").append(":");
        NpcService.gI().createMenuConMeo(player, forPet ? 9909 : 9908, -1, sb.toString(),
                "Mặc Set 1\n(" + (forPet ? "Đệ Tử" : "Sư Phụ") + ")",
                "Mặc Set 2\n(" + (forPet ? "Đệ Tử" : "Sư Phụ") + ")",
                "Mặc Set 3\n(" + (forPet ? "Đệ Tử" : "Sư Phụ") + ")",
                "Mặc Set 4\n(" + (forPet ? "Đệ Tử" : "Sư Phụ") + ")",
                "Mặc Set 5\n(" + (forPet ? "Đệ Tử" : "Sư Phụ") + ")",
                "Thiết Lập /\nLưu Set Đồ",
                "Quay lại");
    }

    public static void openMenuChonSetDeLuu(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== THIẾT LẬP & LƯU SET ĐỒ NHANH (SET 1 .. SET 5) ===\n");
        for (int i = 1; i <= InventoryService.MAX_EQUIP_SETS; i++) {
            sb.append("|2|• Set ").append(i).append(": |1|")
                    .append(InventoryService.gI().getEquipSetSummary(player, i)).append("\n");
        }
        sb.append("|0|Hãy chọn Set đồ bạn muốn thiết lập (lưu đồ đang mặc của Sư phụ hoặc Đệ tử vào Set đó):");
        NpcService.gI().createMenuConMeo(player, 9911, -1, sb.toString(),
                "Thiết Lập\nSet Đồ 1",
                "Thiết Lập\nSet Đồ 2",
                "Thiết Lập\nSet Đồ 3",
                "Thiết Lập\nSet Đồ 4",
                "Thiết Lập\nSet Đồ 5",
                "Quay lại");
    }

    public static void openMenuChiTietThietLapSet(Player player, int setIndex) {
        if (player == null || player.inventory == null) {
            return;
        }
        setPendingEquipSetIndex(player, setIndex);
        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== CẤU HÌNH SET ĐỒ SỐ ").append(setIndex).append(" ===\n");
        sb.append("|2|Trang bị trong Set ").append(setIndex).append(": |1|")
                .append(InventoryService.gI().getEquipSetSummary(player, setIndex)).append("\n");
        sb.append("|0|Lưu ý: Khi lưu Set đồ, các trang bị trong Set sẽ tự động được KHÓA bảo vệ để tránh vứt/bán/phân rã nhầm!");
        NpcService.gI().createMenuConMeo(player, 9912, -1, sb.toString(),
                "Lưu Đồ Sư Phụ\nvào Set " + setIndex,
                "Lưu Đồ Đệ Tử\nvào Set " + setIndex,
                "Mặc Set " + setIndex + "\nCho Sư Phụ",
                "Mặc Set " + setIndex + "\nCho Đệ Tử",
                "Xóa Thiết Lập\nSet " + setIndex,
                "Quay lại");
    }

    private static List<Item> getNonNullItems(List<Item> list) {
        List<Item> res = new ArrayList<>();
        if (list == null) {
            return res;
        }
        for (Item it : list) {
            if (it != null && it.isNotNullItem() && it.template != null) {
                res.add(it);
            }
        }
        return res;
    }

    private static List<Item> getLockableBagItemsInCurrentTab(Player player) {
        List<Item> res = new ArrayList<>();
        if (player == null || player.inventory == null || player.inventory.itemsBag == null) {
            return res;
        }
        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            Item it = player.inventory.itemsBag.get(i);
            if (InventoryService.isLockableEquipment(it)) {
                res.add(it);
            }
        }
        return res;
    }

    private static int lockAllEquippedItems(Player player, boolean locked) {
        int count = 0;
        if (player == null || player.inventory == null) {
            return count;
        }
        for (Item it : player.inventory.itemsBody) {
            if (it != null && it.isNotNullItem()) {
                InventoryService.setItemLock(it, locked);
                count++;
            }
        }
        if (player.pet != null && player.pet.inventory != null) {
            for (Item it : player.pet.inventory.itemsBody) {
                if (it != null && it.isNotNullItem()) {
                    InventoryService.setItemLock(it, locked);
                    count++;
                }
            }
        }
        InventoryService.gI().sendItemBody(player);
        InventoryService.gI().sendItemBags(player);
        if (player.pet != null) {
            Service.gI().showInfoPet(player);
        }
        return count;
    }

    public static void openMenuKhoaTrangBi(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }
        int lockedMaster = 0;
        int totalMaster = 0;
        for (Item it : player.inventory.itemsBody) {
            if (it != null && it.isNotNullItem()) {
                totalMaster++;
                if (InventoryService.isItemLocked(it)) {
                    lockedMaster++;
                }
            }
        }
        int lockedPet = 0;
        int totalPet = 0;
        if (player.pet != null && player.pet.inventory != null) {
            for (Item it : player.pet.inventory.itemsBody) {
                if (it != null && it.isNotNullItem()) {
                    totalPet++;
                    if (InventoryService.isItemLocked(it)) {
                        lockedPet++;
                    }
                }
            }
        }
        int lockedBag = 0;
        for (Item it : player.inventory.itemsBag) {
            if (it != null && it.isNotNullItem() && InventoryService.isItemLocked(it)) {
                lockedBag++;
            }
        }
        int curTab = player.inventory.currentBagTab + 1;
        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== KHÓA BẢO VỆ TRANG BỊ (CHỐNG VỨT / BÁN / PHÂN RÃ NHẦM) ===\n");
        sb.append("|2|• Đồ Sư phụ đang mặc: |1|Đã khóa ").append(lockedMaster).append("/").append(totalMaster).append(" món\n");
        sb.append("|2|• Đồ Đệ tử đang mặc: |1|").append(player.pet != null ? ("Đã khóa " + lockedPet + "/" + totalPet + " món\n") : "Chưa có Đệ tử\n");
        sb.append("|2|• Trang bị khóa trong Hành trang: |1|").append(lockedBag).append(" món\n");
        sb.append("|0|Trang bị đã khóa sẽ KHÔNG THỂ vứt ra đất, bán vào Shop, tự động bán rác hoặc phân rã tại Bà Hạt Mít!");
        NpcService.gI().createMenuConMeo(player, 9910, -1, sb.toString(),
                "Khóa/Mở Đồ\nSư Phụ Mặc",
                "Khóa/Mở Đồ\nĐệ Tử Mặc",
                "Khóa/Mở Đồ\nHành Trang",
                "Khóa TẤT CẢ\nĐồ Đang Mặc",
                "Mở Khóa TẤT CẢ\nĐồ Đang Mặc",
                "Quay lại");
    }

    public static void openMenuKhoaDoSuPhu(Player player) {
        List<Item> bodyItems = getNonNullItems(player.inventory.itemsBody);
        if (bodyItems.isEmpty()) {
            Service.gI().sendThongBao(player, "Sư phụ hiện chưa mặc trang bị nào!");
            openMenuKhoaTrangBi(player);
            return;
        }
        StringBuilder sb = new StringBuilder("|7|=== KHÓA / MỞ KHÓA ĐỒ SƯ PHỤ ĐANG MẶC ===\n");
        List<String> opts = new ArrayList<>();
        for (Item it : bodyItems) {
            boolean locked = InventoryService.isItemLocked(it);
            sb.append(locked ? "|1|[ĐÃ KHÓA] " : "|7|[Chưa khóa] ").append(it.template.name).append("\n");
            opts.add(it.template.name + "\n" + (locked ? "[ĐÃ KHÓA]" : "[Chưa khóa]"));
        }
        sb.append("|0|Bấm vào trang bị bên dưới để Khóa hoặc Mở khóa ngay:");
        opts.add("Quay lại");
        NpcService.gI().createMenuConMeo(player, 9913, -1, sb.toString(), opts.toArray(new String[0]));
    }

    public static void openMenuKhoaDoDeTu(Player player) {
        if (player.pet == null || player.pet.inventory == null) {
            Service.gI().sendThongBao(player, "Bạn hiện chưa có Đệ tử!");
            openMenuKhoaTrangBi(player);
            return;
        }
        List<Item> petItems = getNonNullItems(player.pet.inventory.itemsBody);
        if (petItems.isEmpty()) {
            Service.gI().sendThongBao(player, "Đệ tử hiện chưa mặc trang bị nào!");
            openMenuKhoaTrangBi(player);
            return;
        }
        StringBuilder sb = new StringBuilder("|7|=== KHÓA / MỞ KHÓA ĐỒ ĐỆ TỬ ĐANG MẶC ===\n");
        List<String> opts = new ArrayList<>();
        for (Item it : petItems) {
            boolean locked = InventoryService.isItemLocked(it);
            sb.append(locked ? "|1|[ĐÃ KHÓA] " : "|7|[Chưa khóa] ").append(it.template.name).append("\n");
            opts.add(it.template.name + "\n" + (locked ? "[ĐÃ KHÓA]" : "[Chưa khóa]"));
        }
        sb.append("|0|Bấm vào trang bị bên dưới để Khóa hoặc Mở khóa ngay:");
        opts.add("Quay lại");
        NpcService.gI().createMenuConMeo(player, 9914, -1, sb.toString(), opts.toArray(new String[0]));
    }

    public static void openMenuKhoaDoHanhTrang(Player player, int page) {
        List<Item> bagEquips = getLockableBagItemsInCurrentTab(player);
        int curTab = player.inventory.currentBagTab + 1;
        if (bagEquips.isEmpty()) {
            Service.gI().sendThongBao(player, "Không có trang bị nào ở Hành trang Trang " + curTab + "!");
            openMenuKhoaTrangBi(player);
            return;
        }
        int pageSize = 6;
        int totalPages = Math.max(1, (bagEquips.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, totalPages - 1));
        setPendingLockBagPage(player, page);
        int start = page * pageSize;
        int end = Math.min(bagEquips.size(), start + pageSize);

        StringBuilder sb = new StringBuilder();
        sb.append("|7|=== KHÓA / MỞ KHÓA TRANG BỊ TRONG HÀNH TRANG (TAB ").append(curTab)
                .append(" - MỤC ").append(page + 1).append("/").append(totalPages).append(") ===\n");
        List<String> opts = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Item it = bagEquips.get(i);
            boolean locked = InventoryService.isItemLocked(it);
            sb.append(locked ? "|1|[ĐÃ KHÓA] " : "|7|[Chưa khóa] ").append(it.template.name).append("\n");
            opts.add(it.template.name + "\n" + (locked ? "[ĐÃ KHÓA]" : "[Chưa khóa]"));
        }
        sb.append("|0|Bấm vào trang bị bên dưới để Khóa hoặc Mở khóa:");
        if (totalPages > 1) {
            opts.add("Trang kế\n(" + ((page + 1) % totalPages + 1) + "/" + totalPages + ")");
        }
        opts.add("Quay lại");
        NpcService.gI().createMenuConMeo(player, 9915, -1, sb.toString(), opts.toArray(new String[0]));
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
                    "Bạn hiện chưa có Đệ tử.\nHãy đi săn Super Broly hoặc Broly 18h để thu phục Đệ tử!",
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
                "Đổi Đồ Nhanh\nCho Đệ Tử",
                "Quay lại");
    }

}
