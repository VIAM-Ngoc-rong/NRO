package nro.models.boss;


import nro.models.consts.AppearType;
import lombok.Data;

@Data
public class BossData {

    public static final int DEFAULT_APPEAR = 0;
    public static final int APPEAR_WITH_ANOTHER = 1;
    public static final int ANOTHER_LEVEL = 2;

    private String name;

    private byte gender;

    private short[] outfit;

    private int dame;

    private int[] hp;

    private long[] hpLong;

    private boolean useExactLongHp;

    private int[] mapJoin;

    private int[][] skillTemp;

    private String[] textS;

    private String[] textM;

    private String[] textE;

    private int secondsRest;

    private AppearType typeAppear;

    private int[] bossesAppearTogether;

    public String getName() {
        return this.name;
    }

    public byte getGender() {
        return this.gender;
    }

    public short[] getOutfit() {
        return this.outfit;
    }

    public int getDame() {
        return this.dame;
    }

    public int[] getHp() {
        return this.hp;
    }

    public boolean isUseExactLongHp() {
        return this.useExactLongHp;
    }

    public int[] getMapJoin() {
        return this.mapJoin;
    }

    public int[][] getSkillTemp() {
        return this.skillTemp;
    }

    public String[] getTextS() {
        return this.textS;
    }

    public String[] getTextM() {
        return this.textM;
    }

    public String[] getTextE() {
        return this.textE;
    }

    public int getSecondsRest() {
        return this.secondsRest;
    }

    public AppearType getTypeAppear() {
        return this.typeAppear;
    }

    public int[] getBossesAppearTogether() {
        return this.bossesAppearTogether;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public void setOutfit(short[] outfit) {
        this.outfit = outfit;
    }

    public void setDame(int dame) {
        this.dame = dame;
    }

    public void setHp(int[] hp) {
        this.hp = hp;
    }

    public void setHpLong(long[] hpLong) {
        this.hpLong = hpLong;
    }

    public void setUseExactLongHp(boolean useExactLongHp) {
        this.useExactLongHp = useExactLongHp;
    }

    public void setMapJoin(int[] mapJoin) {
        this.mapJoin = mapJoin;
    }

    public void setSkillTemp(int[][] skillTemp) {
        this.skillTemp = skillTemp;
    }

    public void setTextS(String[] textS) {
        this.textS = textS;
    }

    public void setTextM(String[] textM) {
        this.textM = textM;
    }

    public void setTextE(String[] textE) {
        this.textE = textE;
    }

    public void setSecondsRest(int secondsRest) {
        this.secondsRest = secondsRest;
    }

    public void setTypeAppear(AppearType typeAppear) {
        this.typeAppear = typeAppear;
    }

    public void setBossesAppearTogether(int[] bossesAppearTogether) {
        this.bossesAppearTogether = bossesAppearTogether;
    }

    public long[] getHpLong() {
        if (this.hpLong != null && this.hpLong.length > 0) {
            return this.hpLong;
        }
        if (this.hp == null || this.hp.length == 0) {
            return new long[]{1000L};
        }
        long[] res = new long[this.hp.length];
        for (int i = 0; i < this.hp.length; i++) {
            res[i] = this.hp[i];
        }
        return res;
    }

    private static int[] clampLongHpToIntArray(long[] hpLong) {
        if (hpLong == null || hpLong.length == 0) {
            return new int[]{1000};
        }
        int[] clamped = new int[hpLong.length];
        for (int i = 0; i < hpLong.length; i++) {
            clamped[i] = (int) Math.min(2_000_000_000L, Math.max(1L, hpLong[i]));
        }
        return clamped;
    }

    private BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE) {
        this.name = name;
        this.gender = gender;
        this.outfit = outfit;
        this.dame = dame;
        this.hp = hp;
        this.mapJoin = mapJoin;
        this.skillTemp = skillTemp;
        this.textS = textS;
        this.textM = textM;
        this.textE = textE;
        this.secondsRest = 0;
        this.typeAppear = AppearType.DEFAULT_APPEAR;
    }

    public BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE, int secondsRest) {
        this(name, gender, outfit, dame, hp, mapJoin, skillTemp, textS, textM, textE);
        this.secondsRest = secondsRest;
    }

    public BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE, int secondsRest, int[] bossesAppearTogether) {
        this(name, gender, outfit, dame, hp, mapJoin, skillTemp, textS, textM, textE, secondsRest);
        this.bossesAppearTogether = bossesAppearTogether;
    }

    public BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE, AppearType typeAppear) {
        this(name, gender, outfit, dame, hp, mapJoin, skillTemp, textS, textM, textE);
        this.typeAppear = typeAppear;
    }

    public BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE, int secondsRest, AppearType typeAppear) {
        this(name, gender, outfit, dame, hp, mapJoin, skillTemp, textS, textM, textE, secondsRest);
        this.typeAppear = typeAppear;
    }

    public BossData(String name, byte gender, short[] outfit, int dame, long[] hpLong,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE, int secondsRest) {
        this(name, gender, outfit, dame, clampLongHpToIntArray(hpLong), mapJoin, skillTemp, textS, textM, textE, secondsRest);
        this.hpLong = hpLong;
        this.useExactLongHp = true;
    }

    public BossData(String name, byte gender, short[] outfit, int dame, long[] hpLong,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE, int secondsRest, int[] bossesAppearTogether) {
        this(name, gender, outfit, dame, clampLongHpToIntArray(hpLong), mapJoin, skillTemp, textS, textM, textE, secondsRest, bossesAppearTogether);
        this.hpLong = hpLong;
        this.useExactLongHp = true;
    }

    public BossData(String name, byte gender, short[] outfit, int dame, long[] hpLong,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE, AppearType typeAppear) {
        this(name, gender, outfit, dame, clampLongHpToIntArray(hpLong), mapJoin, skillTemp, textS, textM, textE, typeAppear);
        this.hpLong = hpLong;
        this.useExactLongHp = true;
    }

    public BossData(String name, byte gender, short[] outfit, int dame, long[] hpLong,
            int[] mapJoin, int[][] skillTemp, String[] textS, String[] textM,
            String[] textE, int secondsRest, AppearType typeAppear) {
        this(name, gender, outfit, dame, clampLongHpToIntArray(hpLong), mapJoin, skillTemp, textS, textM, textE, secondsRest, typeAppear);
        this.hpLong = hpLong;
        this.useExactLongHp = true;
    }
}
