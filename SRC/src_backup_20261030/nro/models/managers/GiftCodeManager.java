package nro.models.managers;
import nro.models.data.LocalManager;
import nro.models.player_system.GiftCode;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import java.util.ArrayList;
import nro.models.services.InventoryService;

public class GiftCodeManager {

    public String name;
    public final ArrayList<GiftCode> listGiftCode = new ArrayList<>();

    private static GiftCodeManager instance;

    public static GiftCodeManager gI() {
        if (instance == null) {
            instance = new GiftCodeManager();
        }
        return instance;
    }

    public GiftCode checkUseGiftCode(Player player, String code) {
        if (code == null || code.trim().isEmpty()) {
            Service.gI().sendThongBaoOK(player, "Vui lòng nhập mã Giftcode!");
            return null;
        }
        String cleanCode = code.trim();
        for (GiftCode giftCode : listGiftCode) {
            if (giftCode != null && giftCode.code != null && giftCode.code.equalsIgnoreCase(cleanCode)) {
                if (giftCode.timeCode()) {
                    Service.gI().sendThongBaoOK(player, "Giftcode này đã hết hạn sử dụng!");
                    return null;
                }
                if (giftCode.countLeft <= 0) {
                    Service.gI().sendThongBaoOK(player, "Giftcode đã hết lượt sử dụng!");
                    return null;
                }
                if (giftCode.isUsedGiftCode(player) || player.giftCode.isUsedGiftCode(giftCode.code.toLowerCase()) || player.giftCode.isUsedGiftCode(giftCode.code.toUpperCase())) {
                    Service.gI().sendThongBaoOK(player, "Bạn đã nhận mã quà tặng [" + giftCode.code + "] trước đó rồi!");
                    return null;
                }
                int bagSlotsNeeded = 0;
                for (Integer itemId : giftCode.detail.keySet()) {
                    if (itemId != null && itemId >= 0) {
                        bagSlotsNeeded++;
                    }
                }
                if (bagSlotsNeeded > 0 && InventoryService.gI().getCountEmptyBag(player) < bagSlotsNeeded) {
                    Service.gI().sendThongBaoOK(player, "Cần tối thiểu " + bagSlotsNeeded + " ô hành trang trống để nhận quà!");
                    return null;
                }
                giftCode.countLeft -= 1;
                player.giftCode.add(giftCode.code);
                updateGiftCode(giftCode);
                return giftCode;
            }
        }
        Service.gI().sendThongBaoOK(player, "Mã Giftcode không chính xác hoặc không tồn tại!");
        return null;
    }

    public void updateGiftCode(GiftCode giftcode) {
        try {
            LocalManager.executeUpdate("update giftcode set count_left = ? where id = ?", giftcode.countLeft, giftcode.id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkInfomationGiftCode(Player p) {
        StringBuilder sb = new StringBuilder();
        for (GiftCode giftCode : listGiftCode) {
            sb.append("Code: ").append(giftCode.code).append(", Số lượng còn lại: ").append(giftCode.countLeft).append("\b")
                    .append("Ngày tạo: ")
                    .append(giftCode.datecreate).append(", Ngày hết hạn: ").append(giftCode.dateexpired)
                    .append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        NpcService.gI().createTutorial(p, 5073, sb.toString());
    }

}