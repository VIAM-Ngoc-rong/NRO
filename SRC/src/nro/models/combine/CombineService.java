package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import java.io.IOException;
import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.map.service.NpcManager;
import nro.models.combine.PhanRaTrangBiKichHoat.PhanRaTrangBi;
import nro.models.services.InventoryService;

/**
 *
 * @author By Mr Blue
 */

public class CombineService {
    
    public static final byte MAX_STAR_ITEM = 9;
    public static final byte MAX_LEVEL_ITEM = 8;
    public static final int DANG_SINH_LE = 8999;
    
    private static final byte OPEN_TAB_COMBINE = 0;
    private static final byte REOPEN_TAB_COMBINE = 1;
    private static final byte COMBINE_SUCCESS = 2;
    private static final byte COMBINE_FAIL = 3;
    private static final byte COMBINE_DRAGON_BALL = 5;
    public static final byte OPEN_ITEM = 6;
    
    public static final int LAM_PHEP_NHAP_DA = 512;
    public static final int EP_SAO_TRANG_BI = 500;
    public static final int PHA_LE_HOA_TRANG_BI = 501;
    public static final int CHUYEN_HOA_TRANG_BI_VANG = 994;
    public static final int CHUYEN_HOA_TRANG_BI_NGOC = 995;
    public static final int PHAN_RA_TRANG_BI_KH = 996;
    public static final int TAI_TAO_CAPSULE_KH = 997;
    public static final int DAP_DO_KICH_HOAT = 998;
    public static final int PHAN_RA_DO_SAO = 999;
    public static final int TINH_LUYEN_DA_NGU_SAC = 1000;
    public static final int XOA_THUOC_TINH_SKH = 1001;
    public static final int NANG_CAP_SAO_PHA_LE = 100;
    public static final int DANH_BONG_SAO_PHA_LE = 101;
    public static final int CUONG_HOA_LO_SAO_PHA_LE = 102;
    public static final int TAO_DA_HEMATITE = 103;
    
    public static final int GIAM_DINH_SACH = 104;
    public static final int TAY_SACH = 105;
    public static final int NANG_CAP_SACH_TUYET_KY = 106;
    public static final int HOI_PHUC_SACH = 107;
    public static final int PHAN_RA_SACH = 108;
    public static final int DUI_DUC = 109;
    public static final int DA_MAI = 110;
    public static final int NANG_CAP_VAT_PHAM = 510;
    public static final int NANG_CAP_BONG_TAI = 511;
    public static final int NHAP_NGOC_RONG = 513;
    public static final int PHAN_RA_DO_THAN_LINH = 514;
    public static final int CHE_TAO_TRANG_BI_THIEN_SU = 515;
    public static final int NANG_CHI_SO_BONG_TAI = 517;
    public static final int NANG_CAP_BONG_TAI3 = 455;
    public static final int NANG_CHI_SO_BONG_TAI3 = 457;
    private static CombineService instance;
    
    public final Npc baHatMit;
    public final Npc whis;
    
    private CombineService() {
        this.baHatMit = NpcManager.getNpc(ConstNpc.BA_HAT_MIT);
        this.whis = NpcManager.getNpc(ConstNpc.WHIS);
    }
    
    public static CombineService gI() {
        if (instance == null) {
            instance = new CombineService();
        }
        return instance;
    }

    /**
     * Hiển thị thông tin đập đồ
     *
     * @param player
     * @param index
     */
    public void showInfoCombine(Player player, int[] index) {
        if (player.combineNew == null) {
            return;
        }
        java.util.List<Item> prevItems = new java.util.ArrayList<>(player.combineNew.itemsCombine);
        java.util.Map<Integer, Integer> prevCountByTemplate = new java.util.HashMap<>();
        for (Item prev : prevItems) {
            if (prev != null && prev.isNotNullItem() && prev.template != null) {
                prevCountByTemplate.merge((int) prev.template.id, 1, Integer::sum);
            }
        }

        player.combineNew.clearItemCombine();
        boolean hadDuplicateOrInvalid = false;
        boolean clickedTabSwitch = false;

        if (index != null && index.length > 0) {
            java.util.Set<Integer> addedBagIndices = new java.util.HashSet<>();
            for (int i = 0; i < index.length; i++) {
                int clientIdx = index[i];
                if (!player.inventory.displayedBagIndices.isEmpty() && clientIdx >= player.inventory.displayedBagIndices.size()) {
                    clickedTabSwitch = true;
                    hadDuplicateOrInvalid = true;
                    continue;
                }
                int bagIndex = player.inventory.resolveBagIndex(clientIdx);
                if (bagIndex >= 0 && bagIndex < player.inventory.itemsBag.size()) {
                    Item item = player.inventory.itemsBag.get(bagIndex);
                    if (item != null && item.isNotNullItem() && item.template != null && item.template.id == 1998) {
                        clickedTabSwitch = true;
                        hadDuplicateOrInvalid = true;
                        continue;
                    }
                    if (item != null && item.isNotNullItem() && item.quantity > 0 && item.template != null) {
                        boolean alreadyAddedRef = false;
                        for (Item existing : player.combineNew.itemsCombine) {
                            if (existing == item) {
                                alreadyAddedRef = true;
                                break;
                            }
                        }
                        if (addedBagIndices.add(bagIndex) && !alreadyAddedRef) {
                            player.combineNew.itemsCombine.add(item);
                        } else {
                            hadDuplicateOrInvalid = true;
                        }
                    } else {
                        hadDuplicateOrInvalid = true;
                    }
                } else {
                    clickedTabSwitch = true;
                    hadDuplicateOrInvalid = true;
                }
            }
        }

        if (clickedTabSwitch) {
            switchBagTabInCombine(player);
            return;
        }

        if (hadDuplicateOrInvalid) {
            reOpenItemCombine(player);
        }
        switch (player.combineNew.typeCombine) {
            case EP_SAO_TRANG_BI:
                EpSaoTrangBi.showInfoCombine(player);
                break;
            case PHA_LE_HOA_TRANG_BI:
                PhaLeHoaTrangBi.showInfoCombine(player);
                break;
            case CHUYEN_HOA_TRANG_BI_VANG:
                ChuyenHoaTrangBi_Vang.chuyenHoaTrangBiVang(player);
                break;
            case CHUYEN_HOA_TRANG_BI_NGOC:
                ChuyenHoaTrangBi_Ngoc.chuyenHoaTrangBiNgoc(player);
                break;
            case PHAN_RA_TRANG_BI_KH:
                PhanRaTrangBi.showInfoCombine(player);
                break;
            case TAI_TAO_CAPSULE_KH:
                TaiTaoCapsuleKichHoat.showInfoCombine(player);
                break;
            case DAP_DO_KICH_HOAT:
                DapDoKichHoat.showInfoCombine(player);
                break;
            case PHAN_RA_DO_SAO:
                PhanRaDoSao.showInfoCombine(player);
                break;
            case TINH_LUYEN_DA_NGU_SAC:
                TinhLuyenDaNguSac.showInfoCombine(player);
                break;
            case PHAN_RA_DO_THAN_LINH:
                TaiCheDoThan.showInfoCombine(player);
                break;
            case XOA_THUOC_TINH_SKH:
                XoaThuocTinhSKH.showInfoCombine(player);
                break;
            case NHAP_NGOC_RONG:
                NhapNgocRong.showInfoCombine(player);
                break;
            case NANG_CAP_VAT_PHAM:
                NangCapVatPham.showInfoCombine(player);
                break;
            case NANG_CAP_BONG_TAI:
                NangCapBongTai.showInfoCombine(player);
                break;
            case NANG_CHI_SO_BONG_TAI:
                NangChiSoBongTai.showInfoCombine(player);
                break;
            case NANG_CAP_BONG_TAI3:
                NangCapBongTai3.showInfoCombine(player);
                break;
            case NANG_CHI_SO_BONG_TAI3:
                NangChiSoBongTai3.showInfoCombine(player);
                break;    
            case NANG_CAP_SAO_PHA_LE:
                NangCapSaoPhaLe.showInfoCombine(player);
                break;
            case DANH_BONG_SAO_PHA_LE:
                DanhBongSaoPhaLe.showInfoCombine(player);
                break;
            case CUONG_HOA_LO_SAO_PHA_LE:
                CuongHoaLoSaoPhaLe.showInfoCombine(player);
                break;
            case TAO_DA_HEMATITE:
                TaoDaHematite.showInfoCombine(player);
                break;
            case GIAM_DINH_SACH:
                GiamDinhSach.showInfoCombine(player);
                break;
            case TAY_SACH:
                TaySach.showInfoCombine(player);
                break;
            case NANG_CAP_SACH_TUYET_KY:
                NangCapSachTuyetKy.showInfoCombine(player);
                break;
            case HOI_PHUC_SACH:
                HoiPhucSach.showInfoCombine(player);
                break;
            case PHAN_RA_SACH:
                PhanRaSach.showInfoCombine(player);
                break;
            case CHE_TAO_TRANG_BI_THIEN_SU:
                CheTaoTrangBiThienSu.showInfoCombine(player);
                break;
            case DUI_DUC:
                CheTaoDuiDuc.showInfoCombine(player);
                break;
            case DA_MAI:
                TaoDaMai.showInfoCombine(player);
                break;
        }
    }

    /**
     * Bắt đầu đập đồ - điều hướng từng loại đập đồ
     *
     * @param player
     */
    public void startCombine(Player player) {
        switch (player.combineNew.typeCombine) {
            case EP_SAO_TRANG_BI:
                EpSaoTrangBi.epSaoTrangBi(player);
                break;
            case PHA_LE_HOA_TRANG_BI:
                PhaLeHoaTrangBi.phaLeHoa(player);
                break;
            case CHUYEN_HOA_TRANG_BI_VANG:
                ChuyenHoaTrangBi_Vang.thucHienChuyenHoa(player);
                break;
            case CHUYEN_HOA_TRANG_BI_NGOC:
                ChuyenHoaTrangBi_Ngoc.thucHienChuyenHoa(player);
                break;
            case PHAN_RA_TRANG_BI_KH:
                PhanRaTrangBi.ThucHienPhanRa(player);
                break;
            case TAI_TAO_CAPSULE_KH:
                TaiTaoCapsuleKichHoat.thucHienTaiTao(player);
                break;
            case DAP_DO_KICH_HOAT:
                DapDoKichHoat.thucHienDapDo(player);
                break;
            case PHAN_RA_DO_SAO:
                PhanRaDoSao.thucHienPhanRa(player);
                break;
            case TINH_LUYEN_DA_NGU_SAC:
                TinhLuyenDaNguSac.thucHienTinhLuyen(player);
                break;
            case PHAN_RA_DO_THAN_LINH:
                TaiCheDoThan.thucHienTaiChe(player);
                break;
            case XOA_THUOC_TINH_SKH:
                XoaThuocTinhSKH.thucHienXoaSKH(player);
                break;
            case NHAP_NGOC_RONG:
                NhapNgocRong.nhapNgocRong(player);
                break;
            case NANG_CAP_VAT_PHAM:
                NangCapVatPham.nangCapVatPham(player);
                break;
            case NANG_CAP_BONG_TAI:
                NangCapBongTai.nangCapBongTai(player);
                break;
            case NANG_CHI_SO_BONG_TAI:
                NangChiSoBongTai.nangChiSoBongTai(player);
                break;
            case NANG_CAP_BONG_TAI3:
                NangCapBongTai3.nangCapBongTai(player);
                break;
            case NANG_CHI_SO_BONG_TAI3:
                NangChiSoBongTai3.nangChiSoBongTai(player);
                break;
            case NANG_CAP_SAO_PHA_LE:
                NangCapSaoPhaLe.nangCapSaoPhaLe(player);
                break;
            case DANH_BONG_SAO_PHA_LE:
                DanhBongSaoPhaLe.danhBongSaoPhaLe(player);
                break;
            case CUONG_HOA_LO_SAO_PHA_LE:
                CuongHoaLoSaoPhaLe.cuongHoaLoSaoPhaLe(player);
                break;
            case TAO_DA_HEMATITE:
                TaoDaHematite.taoDaHematite(player);
                break;
            case GIAM_DINH_SACH:
                GiamDinhSach.giamDinhSach(player);
                break;
            case TAY_SACH:
                TaySach.taySach(player);
                break;
            case NANG_CAP_SACH_TUYET_KY:
                NangCapSachTuyetKy.nangCapSachTuyetKy(player);
                break;
            case HOI_PHUC_SACH:
                HoiPhucSach.hoiPhucSach(player);
                break;
            case PHAN_RA_SACH:
                PhanRaSach.phanRaSach(player);
                break;
            case CHE_TAO_TRANG_BI_THIEN_SU:
                CheTaoTrangBiThienSu.CheTaoTS(player);
                break;
            case DUI_DUC:
                CheTaoDuiDuc.CheTaoDuiDuc(player);
                break;
            case DA_MAI:
                TaoDaMai.CheTaoDuiDuc(player);
                break;
        }
        
        player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        player.combineNew.clearParamCombine();
        player.combineNew.lastTimeCombine = System.currentTimeMillis();
        
    }
    
    public void startCombineVip(Player player, int n) {
        if (player == null || player.combineNew == null) {
            return;
        }
        int type = player.combineNew.typeCombine;
        if (type == PHAN_RA_TRANG_BI_KH || type == PHAN_RA_DO_SAO
                || type == PHAN_RA_DO_THAN_LINH || type == DAP_DO_KICH_HOAT) {
            if (n == 10) {
                selectAllValidItemsInCurrentTab(player);
                openTabCombine(player, type, true);
                return;
            } else if (n == 100) {
                switchBagTabInCombine(player);
                return;
            }
        }
        switch (type) {
            case PHA_LE_HOA_TRANG_BI:
                PhaLeHoaTrangBi.phaLeHoa(player, n);
                break;
            case EP_SAO_TRANG_BI:
                EpSaoTrangBi.epSaoTrangBi(player, n);
                break;
            case NANG_CAP_VAT_PHAM:
                NangCapVatPham.nangCapVatPham(player, n);
                break;
            case NHAP_NGOC_RONG:
                NhapNgocRong.nhapNgocRong(player, true);
                break;
        }

        player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        player.combineNew.clearParamCombine();
        player.combineNew.lastTimeCombine = System.currentTimeMillis();

    }

    private boolean isItemValidForBulkCombine(int type, Item item) {
        if (item == null || !item.isNotNullItem() || item.quantity <= 0 || item.template == null || item.template.id == 1998) {
            return false;
        }
        return switch (type) {
            case PHAN_RA_TRANG_BI_KH -> PhanRaTrangBi.isValidItem(item);
            case PHAN_RA_DO_SAO -> PhanRaDoSao.isValidItem(item);
            case PHAN_RA_DO_THAN_LINH -> item.isDTL();
            case DAP_DO_KICH_HOAT -> DapDoKichHoat.isItemKichHoat(item);
            default -> false;
        };
    }

    private boolean autoExpandSameTemplateItems(Player player, java.util.Map<Integer, Integer> prevCountByTemplate) {
        int type = player.combineNew.typeCombine;
        if (type != PHAN_RA_TRANG_BI_KH && type != PHAN_RA_DO_SAO
                && type != PHAN_RA_DO_THAN_LINH && type != DAP_DO_KICH_HOAT) {
            return false;
        }
        java.util.List<Item> currentList = player.combineNew.itemsCombine;
        if (currentList.isEmpty()) {
            return false;
        }
        java.util.Map<Integer, Integer> curCountByTemplate = new java.util.HashMap<>();
        for (Item it : currentList) {
            if (it != null && it.isNotNullItem() && it.template != null) {
                curCountByTemplate.merge((int) it.template.id, 1, Integer::sum);
            }
        }

        boolean addedAny = false;
        int limit = player.inventory.getActiveTabSize();
        for (java.util.Map.Entry<Integer, Integer> entry : curCountByTemplate.entrySet()) {
            int tempId = entry.getKey();
            int curCount = entry.getValue();
            int prevCount = prevCountByTemplate.getOrDefault(tempId, 0);
            // Nếu người chơi vừa bỏ bớt 1 món cùng loại ra thì tôn trọng lựa chọn bỏ chọn của họ
            if (prevCount > curCount) {
                continue;
            }
            for (int i = 0; i < limit && i < player.inventory.itemsBag.size(); i++) {
                Item candidate = player.inventory.itemsBag.get(i);
                if (candidate == null || !candidate.isNotNullItem() || candidate.template == null
                        || candidate.template.id != tempId) {
                    continue;
                }
                if (!isItemValidForBulkCombine(type, candidate)) {
                    continue;
                }
                boolean exists = false;
                for (Item ex : currentList) {
                    if (ex == candidate) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    if (type == DAP_DO_KICH_HOAT) {
                        int skhCount = 0;
                        for (Item ex : currentList) {
                            if (DapDoKichHoat.isItemKichHoat(ex)) {
                                skhCount++;
                            }
                        }
                        if (skhCount >= 3 || currentList.size() >= 4) {
                            break;
                        }
                    }
                    currentList.add(candidate);
                    addedAny = true;
                }
            }
        }
        return addedAny;
    }

    public int countValidBulkItemsInCurrentTab(Player player) {
        if (player == null || player.combineNew == null || player.inventory == null) {
            return 0;
        }
        int type = player.combineNew.typeCombine;
        int count = 0;
        int limit = player.inventory.getActiveTabSize();
        for (int i = 0; i < limit && i < player.inventory.itemsBag.size(); i++) {
            Item it = player.inventory.itemsBag.get(i);
            if (isItemValidForBulkCombine(type, it)) {
                count++;
            }
        }
        return count;
    }

    public void selectAllValidItemsInCurrentTab(Player player) {
        if (player == null || player.combineNew == null || player.inventory == null) {
            return;
        }
        int type = player.combineNew.typeCombine;
        int limit = player.inventory.getActiveTabSize();
        for (int i = 0; i < limit && i < player.inventory.itemsBag.size(); i++) {
            Item it = player.inventory.itemsBag.get(i);
            if (!isItemValidForBulkCombine(type, it)) {
                continue;
            }
            boolean exists = false;
            for (Item ex : player.combineNew.itemsCombine) {
                if (ex == it) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                if (type == DAP_DO_KICH_HOAT) {
                    int skhCount = 0;
                    for (Item ex : player.combineNew.itemsCombine) {
                        if (DapDoKichHoat.isItemKichHoat(ex)) {
                            skhCount++;
                        }
                    }
                    if (skhCount >= 3 || player.combineNew.itemsCombine.size() >= 4) {
                        break;
                    }
                }
                player.combineNew.itemsCombine.add(it);
            }
        }
    }

    public void switchBagTabInCombine(Player player) {
        if (player == null || player.inventory == null || player.combineNew == null) {
            return;
        }
        int totalTabs = player.inventory.getTotalBagTabs();
        int nextTab = (player.inventory.currentBagTab + 1) % Math.max(1, totalTabs);
        player.inventory.switchBagTab(nextTab);
        InventoryService.gI().sendItemBags(player);
        openTabCombine(player, player.combineNew.typeCombine, true);
        nro.models.services.Service.gI().sendThongBao(player,
                "Đã chuyển sang Hành trang Trang " + (nextTab + 1) + "/" + totalTabs + " để tiếp tục chọn đồ!");
    }

    /**
     * Mở tab đập đồ
     *
     * @param player
     * @param type kiểu đập đồ
     */
    public void openTabCombine(Player player, int type) {
        openTabCombine(player, type, false);
    }

    public void openTabCombine(Player player, int type, boolean keepExistingSelection) {
        if (!keepExistingSelection) {
            player.combineNew.setTypeCombine(type);
            InventoryService.gI().sendItemBags(player);
        } else {
            player.combineNew.typeCombine = type;
        }
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(OPEN_TAB_COMBINE);
            msg.writer().writeUTF(getTextInfoTabCombine(type));
            msg.writer().writeUTF(getTextTopTabCombine(type));
            if (player.idMark.getNpcChose() != null) {
                msg.writer().writeShort(player.idMark.getNpcChose().tempId);
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
        if (!player.combineNew.itemsCombine.isEmpty()) {
            reOpenItemCombine(player);
        }
    }

    /**
     * Hiệu ứng mở item
     *
     * @param player
     * @param icon1
     * @param icon2
     */
    public void sendAddItemCombine(Player player, int npcId, Item... items) {
        Message msg;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("null");
            msg.writer().writeUTF("null");
            msg.writer().writeShort(npcId);
            player.sendMessage(msg);
            msg.cleanup();
            java.util.List<Byte> validIndices = new java.util.ArrayList<>();
            if (items != null) {
                for (Item item : items) {
                    if (item != null && item.isNotNullItem() && item.quantity > 0) {
                        byte clientIdx = InventoryService.gI().getIndexBag(player, item);
                        if (clientIdx >= 0 && !validIndices.contains(clientIdx)) {
                            validIndices.add(clientIdx);
                        }
                    }
                }
            }
            msg = new Message(-81);
            msg.writer().writeByte(1);
            msg.writer().writeByte(validIndices.size());
            for (byte idx : validIndices) {
                msg.writer().writeByte(idx);
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }
    
    public void sendEffectOpenItem(Player player, short icon1, short icon2) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(OPEN_ITEM);
            msg.writer().writeShort(icon1);
            msg.writer().writeShort(icon2);
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }
    
    public void sendEffectCombineItem(Player player, byte type, short icon1, short icon2) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(type);
            switch (type) {
                case 0:
                    msg.writer().writeUTF("");
                    msg.writer().writeUTF("");
                    break;
                case 1:
                    msg.writer().writeByte(0);
                    msg.writer().writeByte(-1);
                    break;
                case 2: // success 0 eff 0
                case 3: // success 1 eff 0
                    break;
                case 4: // success 0 eff 1
                    msg.writer().writeShort(icon1);
                    break;
                case 5: // success 0 eff 2
                    msg.writer().writeShort(icon1);
                    break;
                case 6: // success 0 eff 3
                    msg.writer().writeShort(icon1);
                    msg.writer().writeShort(icon2);
                    break;
                case 7: // success 0 eff 4
                    msg.writer().writeShort(icon1);
                    break;
                case 8: // success 1 eff 4
                    // Lam do ts
                    break;
            }
            msg.writer().writeShort(-1); // id npc
            msg.writer().writeShort(-1); // x
            msg.writer().writeShort(-1); // y
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }
    
    private int getTempIdItemC0(int gender, int type) {
        if (type == 4) {
            return 12;
        }
        switch (gender) {
            case 0:
                switch (type) {
                    case 0:
                        return 0;
                    case 1:
                        return 6;
                    case 2:
                        return 21;
                    case 3:
                        return 27;
                }
                break;
            case 1:
                switch (type) {
                    case 0:
                        return 1;
                    case 1:
                        return 7;
                    case 2:
                        return 22;
                    case 3:
                        return 28;
                }
                break;
            case 2:
                switch (type) {
                    case 0:
                        return 2;
                    case 1:
                        return 8;
                    case 2:
                        return 23;
                    case 3:
                        return 29;
                }
                break;
        }
        return -1;
    }
    //Trả về tên đồ c0

    private String getNameItemC0(int gender, int type) {
        if (type == 4) {
            return "Rada cấp 1";
        }
        switch (gender) {
            case 0:
                switch (type) {
                    case 0:
                        return "Áo vải 3 lỗ";
                    case 1:
                        return "Quần vải đen";
                    case 2:
                        return "Găng thun đen";
                    case 3:
                        return "Giầy nhựa";
                }
                break;
            case 1:
                switch (type) {
                    case 0:
                        return "Áo sợi len";
                    case 1:
                        return "Quần sợi len";
                    case 2:
                        return "Găng sợi len";
                    case 3:
                        return "Giầy sợi len";
                }
                break;
            case 2:
                switch (type) {
                    case 0:
                        return "Áo vải thô";
                    case 1:
                        return "Quần vải thô";
                    case 2:
                        return "Găng vải thô";
                    case 3:
                        return "Giầy vải thô";
                }
                break;
        }
        return "";
    }

    /**
     * Hiệu ứng đập đồ thành công
     *
     * @param player
     */
    public boolean CheckSlot(Item trangBi, int starEmpty) {
        if (starEmpty < 8) {
            return true;
        }
        for (ItemOption io : trangBi.itemOptions) {
            if (starEmpty == 8 && io.optionTemplate.id == 228) {
                return io.param >= 8;
            } else if (starEmpty == 9 && io.optionTemplate.id == 228) {
                return io.param >= 9;
            }
        }
        
        return false;
        
    }
    
    public void sendEffSuccessVip(Player player, int iconID) {
        Message msg;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(7);
            msg.writer().writeShort(iconID);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }
    
    public void sendEffectSuccessCombine(Player player) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(COMBINE_SUCCESS);
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Hiệu ứng đập đồ thất bại
     *
     * @param player
     */
    public void sendEffFailVip(Player player) {
        try {
            Message msg;
            msg = new Message(-81);
            msg.writer().writeByte(8);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }
    
    public void sendEffectFailCombine(Player player) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(COMBINE_FAIL);
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Gửi lại danh sách đồ trong tab combine
     *
     * @param player
     */
    public void reOpenItemCombine(Player player) {
        if (player == null || player.combineNew == null || player.inventory == null) {
            return;
        }
        Message msg = null;
        try {
            java.util.List<Item> validItems = new java.util.ArrayList<>();
            java.util.List<Byte> validClientIndices = new java.util.ArrayList<>();
            for (Item it : player.combineNew.itemsCombine) {
                if (it == null || !it.isNotNullItem() || it.quantity <= 0
                        || (it.template != null && it.template.id == 1998)) {
                    continue;
                }
                boolean alreadyAdded = false;
                for (Item existing : validItems) {
                    if (existing == it) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (alreadyAdded) {
                    continue;
                }
                byte clientIdx = InventoryService.gI().getIndexBag(player, it);
                if (clientIdx >= 0 && !validClientIndices.contains(clientIdx)) {
                    validItems.add(it);
                    validClientIndices.add(clientIdx);
                }
            }
            player.combineNew.itemsCombine.clear();
            player.combineNew.itemsCombine.addAll(validItems);

            msg = new Message(-81);
            msg.writer().writeByte(REOPEN_TAB_COMBINE);
            msg.writer().writeByte(validClientIndices.size());
            for (byte idx : validClientIndices) {
                msg.writer().writeByte(idx);
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Hiệu ứng ghép ngọc rồng
     *
     * @param player
     * @param icon
     */
    public void sendEffectCombineDB(Player player, short icon) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(COMBINE_DRAGON_BALL);
            msg.writer().writeShort(icon);
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }
    
    private String getTextTopTabCombine(int type) {
        switch (type) {
            case EP_SAO_TRANG_BI:
                return "Ta sẽ phù phép\ncho trang bị của ngươi\ntrở lên mạnh mẽ";
            case PHA_LE_HOA_TRANG_BI:
                return "Ta sẽ phù phép\ncho trang bị của ngươi\ntrở thành trang bị pha lê";
            case CHUYEN_HOA_TRANG_BI_NGOC:
            case CHUYEN_HOA_TRANG_BI_VANG:
                return "Lưu ý trang bị mới\nphải hơn trang bị gốc\n1 bậc";
            case PHAN_RA_TRANG_BI_KH:
                return "Ta sẽ phù phép\nphân rã Đồ Kích Hoạt thành Vàng,\nHồng Ngọc, Đá Ngũ Sắc, Khoáng TC & Thức Ăn";
            case TAI_TAO_CAPSULE_KH:
                return "Ta sẽ phù phép\ntái tạo thành 1 viên\nCapsule kích hoạt tự chọn";
            case DAP_DO_KICH_HOAT:
                return "Ta sẽ phù phép\n3 món đồ Kích Hoạt + 1 Đồ Phôi\nthành 1 món đồ Kích Hoạt theo Phôi";
            case PHAN_RA_DO_SAO:
                return "Ta sẽ phù phép\nphân rã Đồ Sao nhận Đá Ngũ Sắc,\nVàng, Hồng Ngọc, SPL & Hematite";
            case TINH_LUYEN_DA_NGU_SAC:
                return "Ta sẽ phù phép Đá Ngũ Sắc\nđể Đổi Quà Quý hoặc\nTinh Luyện Cải Trang mạnh mẽ hơn";
            case NHAP_NGOC_RONG:
                return "Ta sẽ phù phép\ncho 7 viên Ngọc Rồng\nthành 1 viên Ngọc Rồng cấp cao";
            case NANG_CAP_VAT_PHAM:
                return "Ta sẽ phù phép cho trang bị của ngươi trở lên mạnh mẽ";
            case PHAN_RA_DO_THAN_LINH:
                return "Ta sẽ phù phép\ntái chế Đồ Thần Linh thành Vàng,\nHồng Ngọc, Đá Ngũ Sắc & Thức Ăn";
            case XOA_THUOC_TINH_SKH:
                return "Ta sẽ phù phép\nxóa sạch thuộc tính SKH\ngiữ nguyên cấp [+] và Sao Pha Lê";
            case NANG_CAP_BONG_TAI:
                return "Ta sẽ phù phép\ncho bông tai Porata của ngươi\nthành cấp 2";
            case NANG_CHI_SO_BONG_TAI:
                return "Ta sẽ phù phép\ncho bông tai Porata cấp 2 của ngươi\ncó 1 chỉ số ngẫu nhiên";
            case NANG_CAP_BONG_TAI3:
                return "Ta sẽ phù phép\ncho bông tai Porata của ngươi\nthành cấp 3";
            case NANG_CHI_SO_BONG_TAI3:
                return "Ta sẽ phù phép\ncho bông tai Porata cấp 3 của ngươi\ncó 2 chỉ số ngẫu nhiên";    
            case NANG_CAP_SAO_PHA_LE:
                return "Ta sẽ phù phép\nnâng cấp Sao Pha Lê\nthành cấp 2";
            case DANH_BONG_SAO_PHA_LE:
                return "Đánh bóng\nSao pha lê cấp 2";
            case CUONG_HOA_LO_SAO_PHA_LE:
                return "Cường hóa\nÔ Sao Pha Lê";
            case TAO_DA_HEMATITE:
                return "Ta sẽ phù phép\ntạo đá hematite";
            case DUI_DUC:
                return "Ta sẽ phù phép\ntạo dùi đục";
            case DA_MAI:
                return "Ta sẽ phù phép\ntạo đá mài";
            case GIAM_DINH_SACH:
                return "Ta sẽ phù phép\ngiám định sách đó cho ngươi";
            case TAY_SACH:
                return "Ta sẽ phù phép\ntẩy sách đó cho ngươi";
            case NANG_CAP_SACH_TUYET_KY:
                return "Ta sẽ phù phép\nnâng cấp Sách Tuyệt Kỹ cho ngươi";
            case HOI_PHUC_SACH:
                return "Ta sẽ phù phép\nphục hồi sách cho ngươi";
            case PHAN_RA_SACH:
                return "Ta sẽ phù phép\nphân rã sách đó cho ngươi";
            case CHE_TAO_TRANG_BI_THIEN_SU:
                return "Chế tạo\ntrang bị thiên sứ";
            case LAM_PHEP_NHAP_DA:
                return "Ta sẽ phù phép\n"
                        + "cho 10 mảnh đá vụn\n"
                        + "trở thành 1 đá nâng cấp";
            default:
                return "";
        }
    }
    
    private String getTextInfoTabCombine(int type) {
        switch (type) {
            case EP_SAO_TRANG_BI:
                return "Chọn trang bị\n(Áo, quần, găng, giày hoặc rađa) có ô đặt sao pha lê\nChọn loại sao pha lê\n"
                        + "Sau đó chọn 'Nâng cấp'";
            case PHA_LE_HOA_TRANG_BI:
                return "Chọn trang bị\n(Áo, quần, găng, giày hoặc rađa)\nSau đó chọn 'Nâng cấp'";
            case PHAN_RA_TRANG_BI_KH:
                return "Vào hành trang\nChọn 1 hoặc nhiều Trang bị Kích Hoạt cần phân rã\nNhận Vàng, Hồng Ngọc, Đá Ngũ Sắc, Khoáng Tái Chế và Thức Ăn Hủy Diệt tùy theo sức mạnh yêu cầu của trang bị";
            case TAI_TAO_CAPSULE_KH:
                return "Vào hành trang\nChọn 3 khoáng tái chế\nChọn 1 Capsule vỡ\nSau đó chọn 'Tái tạo'";
            case DAP_DO_KICH_HOAT:
                return "Vào hành trang\nChọn 3 món đồ Kích Hoạt bất kỳ\nChọn thêm 1 món Đồ Phôi (VD: Găng Thần Linh)\nCần 100 triệu vàng -> Ra đúng món Đồ Phôi có SKH bất kỳ";
            case PHAN_RA_DO_SAO:
                return "Vào hành trang\nChọn 1 hoặc nhiều trang bị có Sao Pha Lê (1s - 9s)\nPhân rã miễn phí nhận Đá Ngũ Sắc, Vàng, Hồng Ngọc, Sao Pha Lê, Đá Hematite và Capsule Kích Hoạt";
            case TINH_LUYEN_DA_NGU_SAC:
                return "Cách 1 (Đổi quà): Chọn x10 Đá Ngũ Sắc + 100Tr Vàng -> Nhận NR Tím 5-7s, Hồng Ngọc, SPL Cấp 2 hoặc Capsule KH\n"
                        + "Cách 2 (Tinh luyện): Chọn 1 Cải trang vĩnh viễn + x5 Đá Ngũ Sắc + 200Tr Vàng -> +1~2% SĐ/HP/KI (Tối đa 45%)";
            case CHUYEN_HOA_TRANG_BI_NGOC:
            case CHUYEN_HOA_TRANG_BI_VANG:
                return "Vào hành trang\nChọn trang bị gốc\n(Áo,quần,găng,giày hoặc rada)\ntừ cấp[+4] trở lên\nChọn tiếp trang bị mới\nchưa nâng cấp cần nhập thể\nsau đó chọn 'Nâng cấp'";
            case NHAP_NGOC_RONG:
                return "Vào hành trang\nChọn 7 viên ngọc cùng sao\nSau đó chọn 'Làm phép'";
            case NANG_CAP_VAT_PHAM:
                return "vào hành trang\nChọn trang bị\n(Áo, quần, găng, giày hoặc rađa)\nChọn loại đá để nâng cấp\n"
                        + "Sau đó chọn 'Nâng cấp'";
            case PHAN_RA_DO_THAN_LINH:
                return "Vào hành trang\nChọn 1 hoặc nhiều món Đồ Thần Linh cần tái chế\nNhận 200-400Tr Vàng, 50-100 Hồng Ngọc, 3-5 Đá Ngũ Sắc, 1-2 Khoáng Tái Chế và 10-20 Thức Ăn Hủy Diệt mỗi món";
            case XOA_THUOC_TINH_SKH:
                return "Vào hành trang\nChọn 1 trang bị đang có thuộc tính Set Kích Hoạt (SKH)\nCần 200 triệu vàng -> Xóa sạch dòng SKH, giữ nguyên cấp [+], chỉ số gốc và toàn bộ Sao Pha Lê";
            case CHE_TAO_TRANG_BI_THIEN_SU:
                return "vào hành trang\nChọn 1 công thức và công thức Vip\nkèm 1 đá nâng, 1 đá may mắn\n và 999 mảnh thiên sứ\n "
                        + "Ta sẽ cho ra đồ thiên sứ từ 0-15% chỉ số\n"
                        + "Sau đó chọn 'Nâng Cấp'";
            case NANG_CAP_BONG_TAI:
                return "Vào hành trang\nChọn bông tai Porata\nChọn mảnh bông tai để nâng cấp, Số lượng 9999 cái"
                        + "\nSau đó chọn 'Nâng cấp'";
            case NANG_CHI_SO_BONG_TAI:
                return "Vào hành trang\nChọn bông tai Porata\nChọn mảnh hồn porata số lượng 99"
                        + "\ncái và đá xanh lam để nâng cấp.\nSau đó chọn 'Nâng cấp chỉ số'";
            case NANG_CAP_BONG_TAI3:
                return "Vào hành trang\nChọn bông tai Porata cấp 2\nChọn mảnh bông tai để nâng cấp, Số lượng 19999 cái"
                        + "\nSau đó chọn 'Nâng cấp'";
            case NANG_CHI_SO_BONG_TAI3:
                return "Vào hành trang\nChọn bông tai Porata Cấp 3\nChọn mảnh hồn porata số lượng 199"
                        + "\ncái và đá xanh lam để nâng cấp.\nSau đó chọn 'Nâng cấp chỉ số'";    
            case NANG_CAP_SAO_PHA_LE:
                return "Vào hành trang\nChọn đá Hematite\nChọn loại sao pha lê (cấp 1)\nSau đó chọn 'Nâng cấp'";
            case DANH_BONG_SAO_PHA_LE:
                return "Vào hành trang\nChọn loại sao pha lê cấp 2 có từ 2 viên trở lên\nChọn 1 đá mài\nSau đó chọn 'Đánh bóng'";
            case CUONG_HOA_LO_SAO_PHA_LE:
                return "Vào hành trang\nChọn trang bị có Ô sao thứ 8 trở lên chưa cường hóa\nChọn đá Hematite\nChọn dùi đục\nSau đó chọn 'Cường hóa'";
            case TAO_DA_HEMATITE:
                return "Vào hành trang\nChọn 5 sao pha lê cấp 1 cùng màu\nChọn 'Tạo đá Hematite'";
            case DUI_DUC:
                return "Vào hành trang\nChọn 5 viên đá Hematite\nChọn 'Tạo dùi đục'";
            case DA_MAI:
                return "Vào hành trang\nChọn 5 Dùi Đục\nChọn 'Tạo đá mài'";
            case GIAM_DINH_SACH:
                return "Vào hành trang chọn\n1 sách cần giám định";
            case TAY_SACH:
                return "Vào hành trang chọn\n1 sách cần tẩy";
            case NANG_CAP_SACH_TUYET_KY:
                return "Vào hành trang chọn\nSách Tuyệt Kỹ 1 cần nâng cấp và 10 Kìm bấm giấy";
            case HOI_PHUC_SACH:
                return "Vào hành trang chọn\nCác Sách Tuyệt Kỹ cần phục hồi";
            case PHAN_RA_SACH:
                return "Vào hành trang chọn\n1 sách cần phân rã";
            case LAM_PHEP_NHAP_DA:
                return "Vào hành trang\n"
                        + "Chọn 10 mảnh đá vụn\n"
                        + "Chọn 1 bình nước phép\n"
                        + "(mua tại Uron ở trạm tàu vũ trụ)\n"
                        + "Sau đó chọn 'Làm phép'";
            default:
                return "";
        }
    }
}
