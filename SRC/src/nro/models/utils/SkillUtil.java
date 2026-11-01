package nro.models.utils;

import java.util.List;
import nro.models.player.Player;
import nro.models.skill.NClass;
import nro.models.skill.Skill;
import nro.models.player_system.Template.SkillTemplate;
import nro.models.server.Manager;

/**
 *
 * @author By Mr Blue
 * 
 */

public class SkillUtil {

    private final static NClass nClassTD;
    private final static NClass nClassNM;
    private final static NClass nClassXD;

    static {
        nClassTD = Manager.NCLASS.get(0);
        nClassNM = Manager.NCLASS.get(1);
        nClassXD = Manager.NCLASS.get(2);
    }

    public static Skill createSkill(int tempId, int level) {
        Skill skill = null;
        SkillTemplate template = findSkillTemplate(tempId);
        if (template != null) {
            List<Skill> skills = template.skillss;
            if (skills != null && level >= 1 && level <= skills.size()) {
                skill = skills.get(level - 1);
            }
        }
        return skill != null ? new Skill(skill) : null;
    }

    public static SkillTemplate findSkillTemplate(int tempId) {
        SkillTemplate template = nClassTD.getSkillTemplate(tempId);
        if (template == null) {
            template = nClassNM.getSkillTemplate(tempId);
        }
        if (template == null) {
            template = nClassXD.getSkillTemplate(tempId);
        }
        return template;
    }

    public static List<Skill> findSkills(int tempId) {
        List skills = nClassTD.getSkills(tempId);
        if (skills == null) {
            skills = nClassNM.getSkills(tempId);
        }
        if (skills == null) {
            skills = nClassXD.getSkills(tempId);
        }
        return skills;
    }

    public static List<Skill> findPointSkill(int skillId) {
        List skills = nClassTD.getSkills(skillId);

        return skills;
    }

    public static Skill createEmptySkill() {
        Skill skill = new Skill();
        skill.skillId = -1;
        return skill;
    }

    public static Skill createSkillLevel0(int tempId) {
        Skill skill = createEmptySkill();
        skill.template = new SkillTemplate();
        skill.template.id = (byte) tempId;
        return skill;
    }

    public static boolean isUseSkillDam(Player player) {
        int skillId = player.playerSkill.skillSelect.template.id;
        return (skillId == Skill.DRAGON || skillId == Skill.DEMON
                || skillId == Skill.GALICK || skillId == Skill.KAIOKEN
                || skillId == Skill.LIEN_HOAN);
    }

    public static boolean isUseSkillChuong(Player player) {
        int skillId = player.playerSkill.skillSelect.template.id;
        return (skillId == Skill.KAMEJOKO || skillId == Skill.MASENKO || skillId == Skill.ANTOMIC);
    }

    public static boolean isUseSkillSpecial(Player player) {
        if (player == null) {
            return false;
        }
        if (player.newSkill != null && player.newSkill.isStartSkillSpecial
                && player.newSkill.skillSelect != null && player.newSkill.skillSelect.template != null) {
            int specialId = player.newSkill.skillSelect.template.id;
            if (specialId == Skill.SUPER_KAME || specialId == Skill.LIEN_HOAN_CHUONG || specialId == Skill.MA_PHONG_BA) {
                return true;
            }
        }
        if (player.playerSkill != null && player.playerSkill.skillSelect != null
                && player.playerSkill.skillSelect.template != null) {
            int skillId = player.playerSkill.skillSelect.template.id;
            return (skillId == Skill.SUPER_KAME || skillId == Skill.LIEN_HOAN_CHUONG || skillId == Skill.MA_PHONG_BA);
        }
        return false;
    }

    public static int getTimeMonkey(int level) { //thời gian tồn tại khỉ v
        return (level + 5) * 10000;
    }

    public static int getPercentHpMonkey(int level) { //tỉ lệ máu khỉ cộng thêm v
        return (level + 3) * 10;
    }

    public static int getPercentDameMonkey(int level) { //tỉ lệ dam khỉ cộng thêm v
        return (level + 3);
    }

    public static int getTimeStun(int level) { //thời gian choáng thái dương hạ san v
        return (level + 2) * 1000;
    }

    public static int getTimeSocola() {
        return 30000;
    }

    public static int getTimeShield(int level) { //thời gian tồn tại khiên v
        return (level + 2) * 5000;
    }

    public static int getTimeTroi(int level) { //thời gian trói v
        return level * 5000;
    }

    public static int getTimeDCTT(int level) { //thời gian choáng dịch chuyển tức thời v
        return (level + 1) * 500;
    }

    public static int getTimeThoiMien(int level) { //thời gian thôi miên
        return (level + 4) * 1000;
    }

    public static int getRangeStun(int level) { //phạm vi thái dương hạ san
        return 120 + level * 30;
    }

    public static int getRangeBom(int level) { //phạm vi tự sát
        return 400 + level * 30;
    }

    public static int getRangeQCKK(int level) { //phạm vi quả cầu kênh khi
        return 350 + level * 30;
    }

    public static int getPercentHPHuytSao(int level) { //tỉ lệ máu huýt sáo cộng thêm v
        return (level + 3) * 10;
    }

    public static int getPercentTriThuong(int level) { //tỉ lệ máu hồi phục trị thương v
        return (level + 9) * 5;
    }

    public static int getPercentCharge(int level) { //tỉ lệ hp ttnl
        return level + 3;
    }

    public static int getTempMobMe(int level) {
        int[] temp = {8, 11, 32, 25, 43, 49, 50};
        return temp[level - 1];
    }

    public static int getTimeSurviveMobMe(int level) { //thời gian trứng tồn tại
        return getTimeMonkey(level) * 2;
    }

    public static long getHPMobMe(long hpMaxPlayer, int level) {
        long[] perHPs = {30, 40, 50, 60, 70, 80, 90};
        return hpMaxPlayer * perHPs[level - 1] / 100L;
    }

    public static Skill getSkillbyId(Player player, int id) {
        for (Skill skill : player.playerSkill.skills) {
            if (skill != null && skill.template != null && skill.template.id == id) {
                return skill;
            }
        }
        return null;
    }

    public static boolean upSkillPet(List<Skill> skills, int index) {
        if (skills == null || index < 0 || index >= skills.size()
                || skills.get(index) == null || skills.get(index).skillId == -1
                || skills.get(index).template == null) {
            return false;
        }
        int tempId = skills.get(index).template.id;
        int level = skills.get(index).point + 1;
        if (level > 7) {
            return false;
        }
        Skill skill = null;
        SkillTemplate template = findSkillTemplate(tempId);
        if (template != null) {
            List<Skill> skillss = template.skillss;
            if (skillss != null && level >= 1 && level <= skillss.size()) {
                skill = skillss.get(level - 1);
            }
        }
        skill = new Skill(skill);
        if (index == 1) {
            skill.coolDown = 1000;
        }
        skills.set(index, skill);
        return true;
    }

    public static byte getTempSkillSkillByItemID(int id) {
        if (id >= 66 && id <= 72) {
            return Skill.DRAGON;
        } else if (id >= 79 && id <= 84 || id == 86) {
            return Skill.DEMON;
        } else if (id >= 87 && id <= 93) {
            return Skill.GALICK;
        } else if (id >= 94 && id <= 100) {
            return Skill.KAMEJOKO;
        } else if (id >= 101 && id <= 107) {
            return Skill.MASENKO;
        } else if (id >= 108 && id <= 114) {
            return Skill.ANTOMIC;
        } else if (id >= 115 && id <= 121) {
            return Skill.THAI_DUONG_HA_SAN;
        } else if (id >= 122 && id <= 128) {
            return Skill.TRI_THUONG;
        } else if (id >= 129 && id <= 135) {
            return Skill.TAI_TAO_NANG_LUONG;
        } else if (id >= 300 && id <= 306) {
            return Skill.KAIOKEN;
        } else if (id >= 307 && id <= 313) {
            return Skill.QUA_CAU_KENH_KHI;
        } else if (id >= 314 && id <= 320) {
            return Skill.BIEN_KHI;
        } else if (id >= 321 && id <= 327) {
            return Skill.TU_SAT;
        } else if (id >= 328 && id <= 334) {
            return Skill.MAKANKOSAPPO;
        } else if (id >= 335 && id <= 341) {
            return Skill.DE_TRUNG;
        } else if (id >= 434 && id <= 440) {
            return Skill.KHIEN_NANG_LUONG;
        } else if (id >= 474 && id <= 480) {
            return Skill.SOCOLA;
        } else if (id >= 481 && id <= 487) {
            return Skill.LIEN_HOAN;
        } else if (id >= 488 && id <= 494) {
            return Skill.DICH_CHUYEN_TUC_THOI;
        } else if (id >= 495 && id <= 501) {
            return Skill.THOI_MIEN;
        } else if (id >= 502 && id <= 508) {
            return Skill.TROI;
        } else if (id >= 509 && id <= 515) {
            return Skill.HUYT_SAO;
        } else {
            return switch (id) {
                case 1319 -> Skill.DRAGON;
                case 1320 -> Skill.KAMEJOKO;
                case 1321 -> Skill.THAI_DUONG_HA_SAN;
                case 1322 -> Skill.KAIOKEN;
                case 1323 -> Skill.QUA_CAU_KENH_KHI;
                case 1324 -> Skill.DICH_CHUYEN_TUC_THOI;
                case 1325 -> Skill.THOI_MIEN;
                case 1326 -> Skill.DEMON;
                case 1327 -> Skill.MASENKO;
                case 1328 -> Skill.TRI_THUONG;
                case 1329 -> Skill.MAKANKOSAPPO;
                case 1330 -> Skill.DE_TRUNG;
                case 1331 -> Skill.SOCOLA;
                case 1332 -> Skill.LIEN_HOAN;
                case 1333 -> Skill.GALICK;
                case 1334 -> Skill.ANTOMIC;
                case 1335 -> Skill.TAI_TAO_NANG_LUONG;
                case 1336 -> Skill.BIEN_KHI;
                case 1337 -> Skill.TU_SAT;
                case 1338 -> Skill.TROI;
                case 1339 -> Skill.HUYT_SAO;
                case 1340 -> Skill.KHIEN_NANG_LUONG;
                case 1341 -> Skill.SUPER_KAME;
                case 1342 -> Skill.MA_PHONG_BA;
                case 1343 -> Skill.LIEN_HOAN_CHUONG;
                default -> -1;
            };
        }
    }

    public static Skill getSkillByItemID(Player pl, int tempId) {
        byte skillTempId = getTempSkillSkillByItemID(tempId);
        if (skillTempId != -1) {
            return getSkillbyId(pl, skillTempId);
        }
        return null;
    }

    public static void setSkill(Player pl, Skill skill) {
        boolean checkskill = false;
        for (int i = 0; i < pl.playerSkill.skills.size(); i++) {
            if (pl.playerSkill.skills.get(i).template.id == skill.template.id) {
                checkskill = true;
                pl.playerSkill.skills.set(i, skill);
                break;
            }
        }
        if (!checkskill) {
            pl.playerSkill.skills.add(skill);
        }
    }

    public static byte getTyleSkillAttack(Skill skill) {
        switch (skill.template.id) {
            case Skill.TRI_THUONG:
                return 2;
            case Skill.KAMEJOKO:
            case Skill.MASENKO:
            case Skill.ANTOMIC:
                return 1;
            default:
                return 0;
        }
    }
}
