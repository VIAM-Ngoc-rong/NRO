package nro.models.data;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nro.models.player_system.Template.ArrHead2Frames;
import nro.models.player_system.Template.ItemOptionTemplate;
import nro.models.server.Manager;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player.Inventory;
import nro.models.player_system.Template;

public class ItemData {

    public static final int SPLIT_TEMPLATE_ID = 1150;
    public static final int VIRTUAL_BTN_BASE_ID = 2200;
    public static final int ALIAS_EQUIP_COPIES = 1;

    private static volatile int cachedBaseTemplateCount = -1;
    private static volatile byte[] cachedPart2Payload = null;
    private static volatile int cachedPart2EndId = -1;
    private static final Map<Integer, short[]> EQUIP_ALIAS_MAP = new HashMap<>();

    private static synchronized void ensurePart2Built() {
        int baseSize = Manager.ITEM_TEMPLATES.size();
        if (baseSize <= 0) {
            return;
        }
        if (cachedPart2Payload != null && cachedBaseTemplateCount == baseSize) {
            return;
        }
        try {
            EQUIP_ALIAS_MAP.clear();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(60000);
            DataOutputStream dos = new DataOutputStream(baos);
            int start = Math.min(SPLIT_TEMPLATE_ID, baseSize);
            int currentId = start;

            // 1. Ghi toàn bộ item template thật từ start .. baseSize - 1
            for (int i = start; i < baseSize; i++) {
                Template.ItemTemplate it = Manager.ITEM_TEMPLATES.get(i);
                String name = (it != null && it.name != null) ? it.name : "";
                String desc = (it != null && it.description != null) ? it.description : "";
                int maxDesc = dos.size() > 42000 ? 10 : 22;
                if (desc.length() > maxDesc) {
                    desc = desc.substring(0, maxDesc);
                }
                dos.writeByte(it != null ? it.type : 27);
                dos.writeByte(it != null ? it.gender : 3);
                dos.writeUTF(name);
                dos.writeUTF(desc);
                dos.writeByte(it != null ? it.level : 1);
                dos.writeInt(it != null ? it.strRequire : 0);
                dos.writeShort(it != null ? it.iconID : 4340);
                dos.writeShort(it != null ? it.part : -1);
                dos.writeBoolean(it != null && it.isUpToUp);
                currentId++;
            }

            // Đệm (pad) từ baseSize lên VIRTUAL_BTN_BASE_ID (2200) để cả DB Local (2000) và DB VPS (2156)
            // đều có 6 nút bấm nhanh ở ĐÚNG cố định ID 2200..2205!
            short bagIcon = 4340;
            if (baseSize > 517 && Manager.ITEM_TEMPLATES.get(517) != null && Manager.ITEM_TEMPLATES.get(517).iconID > 0) {
                bagIcon = Manager.ITEM_TEMPLATES.get(517).iconID;
            }
            while (currentId < VIRTUAL_BTN_BASE_ID) {
                dos.writeByte(27);
                dos.writeByte(3);
                dos.writeUTF("");
                dos.writeUTF("");
                dos.writeByte(1);
                dos.writeInt(0);
                dos.writeShort(bagIcon);
                dos.writeShort(-1);
                dos.writeBoolean(false);
                currentId++;
            }

            // 2. Ghi 6 nút bấm nhanh ở đầu Hành Trang tại ID cố định (2200 .. 2205):
            //    [1] [2] [3] [4] [5] [+]
            for (int t = 0; t < Inventory.NUM_QUICK_TABS; t++) {
                int tabNum = t + 1;
                int sSlot = t * Inventory.BAG_VIEW_SIZE + 1;
                int eSlot = (t + 1) * Inventory.BAG_VIEW_SIZE;
                dos.writeByte(27);
                dos.writeByte(3);
                dos.writeUTF("Nút [" + tabNum + "] - Trang " + tabNum + " (Ô " + sSlot + "-" + eSlot + ")");
                dos.writeUTF("Bể chung 1000 ô. Bấm Sử dụng để mở ngay Trang " + tabNum + "!");
                dos.writeByte(1);
                dos.writeInt(0);
                dos.writeShort(bagIcon);
                dos.writeShort(-1);
                dos.writeBoolean(false);
                currentId++;
            }
            // Nút thứ 6 [+] (ID 2205): Mở rộng & Quản lý bể chung
            dos.writeByte(27);
            dos.writeByte(3);
            dos.writeUTF("Nút [+] Mở Rộng & Tiện Ích Bể Chung");
            dos.writeUTF("Bấm Sử dụng / Chọn để mở thêm ô túi đồ hoặc bật Menu Tiện Ích");
            dos.writeByte(1);
            dos.writeInt(0);
            dos.writeShort(bagIcon);
            dos.writeShort(-1);
            dos.writeBoolean(false);
            currentId++;

            // 3. Ghi bản sao (alias) cho các trang bị (type 0..4) để Bà Hạt Mít không bao giờ báo "Đã có vật phẩm này"
            List<Template.ItemTemplate> equipTemplates = new ArrayList<>();
            for (int i = 0; i < baseSize; i++) {
                Template.ItemTemplate it = Manager.ITEM_TEMPLATES.get(i);
                if (it != null && it.type >= 0 && it.type <= 4) {
                    equipTemplates.add(it);
                    EQUIP_ALIAS_MAP.put((int) it.id, new short[ALIAS_EQUIP_COPIES]);
                }
            }
            outer:
            for (int copy = 0; copy < ALIAS_EQUIP_COPIES; copy++) {
                for (Template.ItemTemplate it : equipTemplates) {
                    if (dos.size() > 28000 || currentId >= 32000) {
                        break outer;
                    }
                    String name = it.name != null ? it.name : "";
                    dos.writeByte(it.type);
                    dos.writeByte(it.gender);
                    dos.writeUTF(name);
                    dos.writeUTF("");
                    dos.writeByte(it.level);
                    dos.writeInt(it.strRequire);
                    dos.writeShort(it.iconID);
                    dos.writeShort(it.part);
                    dos.writeBoolean(false);
                    short[] aliases = EQUIP_ALIAS_MAP.get((int) it.id);
                    if (aliases != null) {
                        aliases[copy] = (short) currentId;
                    }
                    currentId++;
                }
            }
            dos.flush();
            cachedPart2Payload = baos.toByteArray();
            cachedPart2EndId = currentId;
            cachedBaseTemplateCount = baseSize;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static short getQuickTabButtonTemplateId(int btnIndex) {
        ensurePart2Built();
        int btnBase = Math.max(VIRTUAL_BTN_BASE_ID, Manager.ITEM_TEMPLATES.size());
        if (btnIndex >= 0 && btnIndex <= Inventory.NUM_QUICK_TABS && cachedPart2EndId >= btnBase + btnIndex + 1) {
            return (short) (btnBase + btnIndex);
        }
        return 1998;
    }

    public static boolean isQuickTabButtonTemplateId(int tempId) {
        int base = Manager.ITEM_TEMPLATES.size();
        int btnBase = Math.max(VIRTUAL_BTN_BASE_ID, base);
        return tempId == 1998
                || (tempId >= VIRTUAL_BTN_BASE_ID && tempId <= VIRTUAL_BTN_BASE_ID + Inventory.NUM_QUICK_TABS)
                || (base > 0 && tempId >= base && tempId <= base + Inventory.NUM_QUICK_TABS)
                || (tempId >= btnBase && tempId <= btnBase + Inventory.NUM_QUICK_TABS)
                || (tempId >= 24989 && tempId <= 24995);
    }

    public static short getEquipAliasTemplateId(short realTempId, int duplicateOccurrence) {
        if (duplicateOccurrence <= 0) {
            return realTempId;
        }
        ensurePart2Built();
        short[] aliases = EQUIP_ALIAS_MAP.get((int) realTempId);
        if (aliases != null) {
            int idx = Math.min(duplicateOccurrence - 1, aliases.length - 1);
            if (idx >= 0 && aliases[idx] > 0) {
                return aliases[idx];
            }
        }
        return realTempId;
    }

    public static void updateItem(MySession session) {
        int split = Math.min(SPLIT_TEMPLATE_ID, Manager.ITEM_TEMPLATES.size());
        updateItemOptionItemplate(session);
        updateItemArrHead2FTemplate(session);
        updateItemTemplate(session, split);
        updateItemTemplate(session, split, Manager.ITEM_TEMPLATES.size());
    }

    public static void updateItemOptionItemplate(MySession session) {
        Message msg;
        try {
            msg = new Message(-28);
            msg.writer().writeByte(8);
            msg.writer().writeByte(DataGame.vsItem); //vcitem
            msg.writer().writeByte(0); //update option
            msg.writer().writeByte(Manager.ITEM_OPTION_TEMPLATES.size());
            for (ItemOptionTemplate io : Manager.ITEM_OPTION_TEMPLATES) {
                String optName = io.name;
                if (io.id == 73 && (optName == null || optName.isEmpty())) {
                    optName = "$Đang mở Trang # ở đầu Bể Chung";
                }
                msg.writer().writeUTF(optName != null ? optName : "");
                msg.writer().writeByte(io.type);
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateItemTemplate(MySession session, int count) {
        Message msg;
        try {
            msg = new Message(-28);
            msg.writer().writeByte(8);

            msg.writer().writeByte(DataGame.vsItem); //vcitem
            msg.writer().writeByte(1); //reload itemtemplate
            msg.writer().writeShort(count);
            for (int i = 0; i < count; i++) {
                Template.ItemTemplate itemTemplate = Manager.ITEM_TEMPLATES.get(i);
                String name = itemTemplate.name != null ? itemTemplate.name : "";
                String desc = itemTemplate.description != null ? itemTemplate.description : "";
                int maxDesc = msg.writer().size() > 48000 ? 12 : 36;
                if (desc.length() > maxDesc) {
                    desc = desc.substring(0, maxDesc);
                }
                msg.writer().writeByte(itemTemplate.type);
                msg.writer().writeByte(itemTemplate.gender);
                msg.writer().writeUTF(name);
                msg.writer().writeUTF(desc);
                msg.writer().writeByte(itemTemplate.level);
                msg.writer().writeInt(itemTemplate.strRequire);
                msg.writer().writeShort(itemTemplate.iconID);
                msg.writer().writeShort(itemTemplate.part);
                msg.writer().writeBoolean(itemTemplate.isUpToUp);
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateItemTemplate(MySession session, int start, int end) {
        Message msg;
        try {
            ensurePart2Built();
            msg = new Message(-28);
            msg.writer().writeByte(8);

            msg.writer().writeByte(DataGame.vsItem); //vcitem
            msg.writer().writeByte(2); //add itemtemplate
            if (cachedPart2Payload != null && cachedPart2EndId > start) {
                msg.writer().writeShort(start);
                msg.writer().writeShort(cachedPart2EndId);
                msg.writer().write(cachedPart2Payload);
            } else {
                msg.writer().writeShort(start);
                msg.writer().writeShort(end);
                for (int i = start; i < end; i++) {
                    Template.ItemTemplate it = Manager.ITEM_TEMPLATES.get(i);
                    String name = it.name != null ? it.name : "";
                    String desc = it.description != null ? it.description : "";
                    int maxDesc = msg.writer().size() > 54000 ? 12 : 36;
                    if (desc.length() > maxDesc) {
                        desc = desc.substring(0, maxDesc);
                    }
                    msg.writer().writeByte(it.type);
                    msg.writer().writeByte(it.gender);
                    msg.writer().writeUTF(name);
                    msg.writer().writeUTF(desc);
                    msg.writer().writeByte(it.level);
                    msg.writer().writeInt(it.strRequire);
                    msg.writer().writeShort(it.iconID);
                    msg.writer().writeShort(it.part);
                    msg.writer().writeBoolean(it.isUpToUp);
                }
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateItemArrHead2FTemplate(MySession session) {
        Message msg;
        try {
            msg = new Message(-28);
            msg.writer().writeByte(8);
            msg.writer().writeByte(DataGame.vsItem); //vcitem
            msg.writer().writeByte(100);
            msg.writer().writeShort(Manager.ARR_HEAD_2_FRAMES.size());
            for (ArrHead2Frames io : Manager.ARR_HEAD_2_FRAMES) {
                msg.writer().writeByte(io.frames.size());
                for (int i : io.frames) {
                    msg.writer().writeShort(i);
                }
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
