package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;

public class QuaTrung extends Npc {

    private final int COST_AP_TRUNG_NHANH = 1000000000;

    public QuaTrung(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == (21 + player.gender) && player.mabuEgg != null) {
                player.mabuEgg.sendMabuEgg();
                String eggTitle = player.mabuEgg.typeEgg == 2
                        ? "|7|TRỨNG ĐỆ TỬ CẤP 4: SÁT THỦ HIT (Hợp thể +45% chỉ số)\n|2|Yêu cầu: Đang sở hữu Đệ tử Broly"
                        : player.mabuEgg.typeEgg == 1
                        ? "|7|TRỨNG ĐỆ TỬ BROLY (Hợp thể +30% chỉ số)\n|2|Yêu cầu: Đang sở hữu Đệ tử Mabư"
                        : "|7|TRỨNG ĐỆ TỬ MABƯ\n|2|Yêu cầu: Đang sở hữu Đệ tử";
                if (player.mabuEgg.getSecondDone() != 0) {
                    this.createOtherMenu(player, ConstNpc.CAN_NOT_OPEN_EGG, eggTitle,
                            "Hủy bỏ\ntrứng", "Mở ngay\n" + Util.numberToMoney(COST_AP_TRUNG_NHANH) + " vàng",
                            "Đóng");
                } else {
                    this.createOtherMenu(player, ConstNpc.CAN_OPEN_EGG, eggTitle, "Nở", "Hủy bỏ\ntrứng",
                            "Đóng");
                }
            } else {
                Service.gI().sendThongBao(player, "Bạn chưa có Quả Trứng!");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == (21 + player.gender) && player.mabuEgg != null) {
                int typeEgg = player.mabuEgg.typeEgg;
                String eggName = typeEgg == 2 ? "Trứng Hit" : (typeEgg == 1 ? "Trứng Broly" : "Trứng Mabư");
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.CAN_NOT_OPEN_EGG -> {
                        if (select == 0) {
                            this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                    "Bạn có chắc chắn muốn hủy bỏ " + eggName + "?", "Đồng ý", "Từ chối");
                        } else if (select == 1) {
                            if (typeEgg == 2 && (player.pet == null || player.pet.typePet != 5)) {
                                Service.gI().sendThongBao(player, "Bắt buộc phải có Đệ tử Broly trước mới mở được Trứng Hit!");
                                return;
                            }
                            if (typeEgg == 1 && (player.pet == null || player.pet.typePet != 1)) {
                                Service.gI().sendThongBao(player, "Bắt buộc phải có Đệ tử Mabư trước mới mở được Trứng Broly!");
                                return;
                            }
                            if (typeEgg == 0 && player.pet == null) {
                                Service.gI().sendThongBao(player, "Bắt buộc phải có Đệ tử trước mới mở được Trứng Mabư!");
                                return;
                            }
                            if (player.inventory.gold >= COST_AP_TRUNG_NHANH) {
                                player.inventory.gold -= COST_AP_TRUNG_NHANH;
                                player.mabuEgg.timeDone = 0;
                                Service.gI().sendMoney(player);
                                player.mabuEgg.sendMabuEgg();
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Bạn không đủ vàng để thực hiện, còn thiếu "
                                        + Util.numberToMoney(
                                                (COST_AP_TRUNG_NHANH - player.inventory.gold))
                                        + " vàng");
                            }
                        }
                    }
                    case ConstNpc.CAN_OPEN_EGG -> {
                        switch (select) {
                            case 0 -> {
                                if (typeEgg == 2) {
                                    if (player.pet == null || player.pet.typePet != 5) {
                                        Service.gI().sendThongBao(player, "Bắt buộc phải có Đệ tử Broly trước mới mở được Trứng Hit!");
                                        return;
                                    }
                                    this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_EGG,
                                            "Bạn có chắc chắn cho Trứng Hit nở?\nĐệ tử Broly của bạn sẽ được nâng cấp thành Đệ tử Cấp 4 Sát Thủ Hit (1tr5 SM, Hợp thể +45% chỉ số)",
                                            "Đệ Hit\nTrái Đất", "Đệ Hit\nNamếc", "Đệ Hit\nXayda", "Từ chối");
                                } else if (typeEgg == 1) {
                                    if (player.pet == null || player.pet.typePet != 1) {
                                        Service.gI().sendThongBao(player, "Bắt buộc phải có Đệ tử Mabư trước mới mở được Trứng Broly!");
                                        return;
                                    }
                                    this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_EGG,
                                            "Bạn có chắc chắn cho Trứng Broly nở?\nĐệ tử Mabư của bạn sẽ được nâng cấp thành Đệ tử Broly (1tr5 SM, Hợp thể +30% chỉ số)",
                                            "Đệ Broly\nTrái Đất", "Đệ Broly\nNamếc", "Đệ Broly\nXayda", "Từ chối");
                                } else {
                                    if (player.pet == null) {
                                        Service.gI().sendThongBao(player, "Bắt buộc phải có Đệ tử trước mới mở được Trứng Mabư!");
                                        return;
                                    }
                                    this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_EGG,
                                            "Bạn có chắc chắn cho trứng nở?\nĐệ tử của bạn sẽ được thay thế bằng đệ Mabư",
                                            "Đệ mabư\nTrái Đất", "Đệ mabư\nNamếc", "Đệ mabư\nXayda", "Từ chối");
                                }
                            }
                            case 1 ->
                                this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                        "Bạn có chắc chắn muốn hủy bỏ " + eggName + "?", "Đồng ý", "Từ chối");
                        }
                    }
                    case ConstNpc.CONFIRM_OPEN_EGG -> {
                        switch (select) {
                            case 0 ->
                                player.mabuEgg.openEgg(ConstPlayer.TRAI_DAT);
                            case 1 ->
                                player.mabuEgg.openEgg(ConstPlayer.NAMEC);
                            case 2 ->
                                player.mabuEgg.openEgg(ConstPlayer.XAYDA);
                            default -> {
                            }
                        }
                    }
                    case ConstNpc.CONFIRM_DESTROY_EGG -> {
                        if (select == 0) {
                            player.mabuEgg.destroyEgg();
                        }
                    }
                }
            }
        }
    }
}
