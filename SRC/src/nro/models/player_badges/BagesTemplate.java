package nro.models.player_badges;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.server.Manager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author By Mr Blue
 *
 */
public class BagesTemplate {

    public static final int TOP1_BADGE_ID_BASE = 9901; // 9901: Top 1 Sức Mạnh, 9902: Top 1 Nhiệm Vụ, 9903: Top 1 Đòn Đánh

    public int id;
    public int idEffect;
    public int idItem;
    public String NAME;
    public List<Item.ItemOption> options = new ArrayList<>();

    public static boolean isTop1DailyBadgeId(int idBadGes) {
        return idBadGes >= TOP1_BADGE_ID_BASE && idBadGes <= TOP1_BADGE_ID_BASE + 2;
    }

    public static BagesTemplate getTop1BaseTemplate(int category) {
        if (Manager.BAGES_TEMPLATES == null || Manager.BAGES_TEMPLATES.isEmpty()) {
            return null;
        }
        int idx = Math.max(0, Math.min(category, Manager.BAGES_TEMPLATES.size() - 1));
        return Manager.BAGES_TEMPLATES.get(idx);
    }

    public static int getVisualEffectId(int idBadGes) {
        if (isTop1DailyBadgeId(idBadGes)) {
            BagesTemplate base = getTop1BaseTemplate(idBadGes - TOP1_BADGE_ID_BASE);
            return base != null ? base.idEffect : -1;
        }
        return idBadGes;
    }

    public static int getTop1BadgeItemId(int category) {
        BagesTemplate base = getTop1BaseTemplate(category);
        return base != null ? base.idItem : -1;
    }

    public static int findIdItemByIdIdEffect(int idEffect) {
        if (isTop1DailyBadgeId(idEffect)) {
            return getTop1BadgeItemId(idEffect - TOP1_BADGE_ID_BASE);
        }
        for (BagesTemplate data : Manager.BAGES_TEMPLATES) {
            if (data.idEffect == idEffect) {
                return data.idItem;
            }
        }
        return -1;
    }

    public static int fineIdEffectbyIdItem(int idItem) {
        for (BagesTemplate data : Manager.BAGES_TEMPLATES) {
            if (data.idItem == idItem) {
                return data.idEffect;
            }
        }
        return -1;
    }

    public static BagesTemplate fineBadgesbyIdItem(int idItem) {
        for (BagesTemplate data : Manager.BAGES_TEMPLATES) {
            if (data.idItem == idItem) {
                return data;
            }
        }
        return null;
    }

    public static List<Integer> listEffect(Player player) {
        Set<Integer> setIdItem = new HashSet<>();
        if (player == null || player.dataBadges == null) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        for (BadgesData data : player.dataBadges) {
            if (data == null || now >= data.timeofUseBadges) {
                continue;
            }
            if (isTop1DailyBadgeId(data.idBadGes)) {
                int itemId = getTop1BadgeItemId(data.idBadGes - TOP1_BADGE_ID_BASE);
                if (itemId != -1) {
                    setIdItem.add(itemId);
                }
                continue;
            }
            for (BagesTemplate temp : Manager.BAGES_TEMPLATES) {
                if (temp.idEffect == data.idBadGes) {
                    setIdItem.add(temp.idItem);
                }
            }
        }
        return new ArrayList<>(setIdItem);
    }

    public static List<Item.ItemOption> sendListItemOption(Player player) {
        if (player == null || player.dataBadges == null || player.dataBadges.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        long now = System.currentTimeMillis();
        List<Item.ItemOption> listOptions = new ArrayList<>();
        boolean addedTop1DailyBonus = false;
        for (BadgesData data : player.dataBadges) {
            if (data != null && data.isUse && now < data.timeofUseBadges) {
                if (isTop1DailyBadgeId(data.idBadGes)) {
                    if (!addedTop1DailyBonus) {
                        listOptions.add(new Item.ItemOption(50, 5));  // +5% Sức đánh
                        listOptions.add(new Item.ItemOption(77, 5));  // +5% HP
                        listOptions.add(new Item.ItemOption(103, 5)); // +5% KI
                        addedTop1DailyBonus = true;
                    }
                    continue;
                }
                for (BagesTemplate temp : Manager.BAGES_TEMPLATES) {
                    if (data.idBadGes == temp.idEffect) {
                        listOptions.addAll(temp.options);
                    }
                }
            }
        }
        return listOptions;
    }

}
