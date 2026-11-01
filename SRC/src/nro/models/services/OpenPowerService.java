package nro.models.services;

import nro.models.item.Item;
import nro.models.player.NPoint;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class OpenPowerService {

    public static final int COST_SPEED_OPEN_LIMIT_POWER = 1000;
    public static final short ITEM_THOI_VANG_ID = 457;

    private static OpenPowerService i;

    private OpenPowerService() {

    }

    public static OpenPowerService gI() {
        if (i == null) {
            i = new OpenPowerService();
        }
        return i;
    }

    public boolean isHighTierLimit(byte curLimit) {
        return curLimit >= 9;
    }

    public int getRequiredGoldBars(byte curLimit) {
        return switch (curLimit) {
            case 9 -> 20;   // Mở lên Bậc 10 (100 Tỷ SM)
            case 10 -> 50;  // Mở lên Bậc 11 (120 Tỷ SM)
            case 11 -> 100; // Mở lên Bậc 12 (150 Tỷ SM)
            default -> 0;
        };
    }

    public int getRequiredRuby(byte curLimit) {
        return switch (curLimit) {
            case 9 -> 1000;  // Mở lên Bậc 10 (100 Tỷ SM)
            case 10 -> 2000; // Mở lên Bậc 11 (120 Tỷ SM)
            case 11 -> 5000; // Mở lên Bậc 12 (150 Tỷ SM)
            default -> 0;
        };
    }

    public int getTotalGoldBarInBag(Player master) {
        if (master == null || master.inventory == null || master.inventory.itemsBag == null) {
            return 0;
        }
        int total = 0;
        for (Item item : master.inventory.itemsBag) {
            if (item != null && item.isNotNullItem() && item.template.id == ITEM_THOI_VANG_ID) {
                total += item.quantity;
            }
        }
        return total;
    }

    private void subGoldBarInBag(Player master, int amount) {
        if (amount <= 0 || master == null || master.inventory == null || master.inventory.itemsBag == null) {
            return;
        }
        int remain = amount;
        for (Item item : master.inventory.itemsBag) {
            if (item != null && item.isNotNullItem() && item.template.id == ITEM_THOI_VANG_ID) {
                int take = Math.min(remain, item.quantity);
                InventoryService.gI().subQuantityItemsBag(master, item, take);
                remain -= take;
                if (remain <= 0) {
                    break;
                }
            }
        }
    }

    public String getMenuDescription(Player target) {
        byte curLimit = target.nPoint.limitPower;
        long nextLimit = target.nPoint.getPowerNextLimit();
        String who = target.isPet ? "đệ tử" : "bản thân";
        if (!isHighTierLimit(curLimit)) {
            return "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của " + who + " lên "
                    + Util.numberToMoney(nextLimit);
        }
        long reqPower = target.nPoint.getPowerLimit();
        int reqGoldBar = getRequiredGoldBars(curLimit);
        int reqRuby = getRequiredRuby(curLimit);
        StringBuilder sb = new StringBuilder();
        sb.append("Ta sẽ truyền năng lượng giúp con đột phá giới hạn sức mạnh của ").append(who)
                .append(" lên mốc ").append(Util.numberToMoney(nextLimit)).append("\n")
                .append("Điều kiện yêu cầu:\n")
                .append("- Đạt tối thiểu: ").append(Util.numberToMoney(reqPower)).append(" SM\n");
        if (target.isPet) {
            sb.append("- Sư phụ đã mở mốc ").append(Util.numberToMoney(nextLimit)).append(" SM\n");
        }
        sb.append("- Chi phí: ").append(reqGoldBar).append(" Thỏi vàng & ")
                .append(Util.numberToMoney(reqRuby)).append(" Hồng ngọc");
        return sb.toString();
    }

    public String getSpeedButtonLabel(byte curLimit, String normalCurrency) {
        if (!isHighTierLimit(curLimit)) {
            return "Nâng ngay\n" + Util.numberToMoney(COST_SPEED_OPEN_LIMIT_POWER) + " " + normalCurrency;
        }
        return "Đột phá ngay\n" + getRequiredGoldBars(curLimit) + " TV + "
                + Util.numberToMoney(getRequiredRuby(curLimit)) + " HN";
    }

    public boolean checkAndConsumeHighTierConditions(Player target) {
        Player master = target.isPet ? ((Pet) target).master : target;
        byte curLimit = target.nPoint.limitPower;
        long reqPower = target.nPoint.getPowerLimit();
        long nextLimit = target.nPoint.getPowerNextLimit();

        if (target.nPoint.power < reqPower) {
            String msg = target.isPet
                    ? "Đệ tử cần đạt đủ " + Util.numberToMoney(reqPower) + " sức mạnh mới có thể mở mốc " + Util.numberToMoney(nextLimit)
                    : "Bạn cần đạt đủ " + Util.numberToMoney(reqPower) + " sức mạnh mới có thể mở mốc " + Util.numberToMoney(nextLimit);
            Service.gI().sendThongBao(master, msg);
            return false;
        }

        if (target.isPet && master.nPoint.limitPower <= curLimit) {
            Service.gI().sendThongBao(master, "Sư phụ phải mở giới hạn sức mạnh lên mốc "
                    + Util.numberToMoney(nextLimit) + " trước đệ tử!");
            return false;
        }

        int reqGoldBar = getRequiredGoldBars(curLimit);
        int reqRuby = getRequiredRuby(curLimit);
        int curGoldBar = getTotalGoldBarInBag(master);
        int curRuby = master.inventory.ruby;

        if (curGoldBar < reqGoldBar) {
            Service.gI().sendThongBao(master, "Bạn không đủ Thỏi vàng! Cần " + reqGoldBar
                    + " Thỏi vàng (còn thiếu " + (reqGoldBar - curGoldBar) + " Thỏi vàng)");
            return false;
        }

        if (curRuby < reqRuby) {
            Service.gI().sendThongBao(master, "Bạn không đủ Hồng ngọc! Cần " + Util.numberToMoney(reqRuby)
                    + " Hồng ngọc (còn thiếu " + Util.numberToMoney(reqRuby - curRuby) + " Hồng ngọc)");
            return false;
        }

        subGoldBarInBag(master, reqGoldBar);
        master.inventory.ruby -= reqRuby;
        InventoryService.gI().sendItemBags(master);
        Service.gI().sendMoney(master);
        return true;
    }

    public boolean openPowerBasic(Player player) {
        byte curLimit = player.nPoint.limitPower;
        if (curLimit < NPoint.MAX_LIMIT) {
            if (isHighTierLimit(curLimit)) {
                return openPowerSpeed(player);
            }
            if (!player.itemTime.isOpenPower && player.nPoint.canOpenPower()) {
                player.itemTime.isOpenPower = true;
                player.itemTime.lastTimeOpenPower = System.currentTimeMillis();
                ItemTimeService.gI().sendAllItemTime(player);
                return true;
            } else {
                Service.gI().sendThongBao(player, "Sức mạnh của bạn không đủ để thực hiện");
                return false;
            }
        } else {
            Service.gI().sendThongBao(player, "Sức mạnh của bạn đã đạt tới mức tối đa");
            return false;
        }
    }

    public boolean openPowerSpeed(Player player) {
        Player master = player.isPet ? ((Pet) player).master : player;
        byte curLimit = player.nPoint.limitPower;
        if (curLimit < NPoint.MAX_LIMIT) {
            if (isHighTierLimit(curLimit)) {
                if (!checkAndConsumeHighTierConditions(player)) {
                    return false;
                }
            }
            player.nPoint.limitPower++;
            if (!player.isPet) {
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã được tăng lên mốc "
                        + Util.numberToMoney(player.nPoint.getPowerLimit()));
            } else {
                Service.gI().point(master);
                Service.gI().sendThongBao(master, "Giới hạn sức mạnh của đệ tử đã được tăng lên mốc "
                        + Util.numberToMoney(player.nPoint.getPowerLimit()));
            }
            return true;
        } else {
            if (!player.isPet) {
                Service.gI().sendThongBao(player, "Sức mạnh của bạn đã đạt tới mức tối đa");
            } else {
                Service.gI().sendThongBao(master, "Sức mạnh của đệ tử đã đạt tới mức tối đa");
            }
            return false;
        }
    }

}
