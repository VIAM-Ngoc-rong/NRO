package nro.models.task;

import nro.models.consts.ConstTask;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class SideTask {

    public SideTaskTemplate template;

    public int count;

    public int maxCount;

    public int level;

    public int leftTask;

    public long receivedTime;

    public boolean notify0;
    public boolean notify10;
    public boolean notify20;
    public boolean notify30;
    public boolean notify40;
    public boolean notify50;
    public boolean notify60;
    public boolean notify70;
    public boolean notify80;
    public boolean notify90;

    public void reset() {
        this.template = null;
        this.count = 0;
        this.level = 0;
        this.notify0 = false;
        this.notify10 = false;
        this.notify20 = false;
        this.notify30 = false;
        this.notify40 = false;
        this.notify50 = false;
        this.notify60 = false;
        this.notify70 = false;
        this.notify80 = false;
        this.notify90 = false;
    }

    public SideTask() {
        this.leftTask = ConstTask.MAX_SIDE_TASK;
    }

    public void renew() {
        if (Util.isAfterMidnight(receivedTime)) {
            this.leftTask = ConstTask.MAX_SIDE_TASK;
            this.receivedTime = System.currentTimeMillis();
        }
    }

    public boolean isDone() {
        return this.count >= this.maxCount;
    }

    public String getName() {
        if (this.template != null) {
            return this.template.name.replaceAll("%1", String.valueOf(maxCount));
        }
        return "Hiện tại không có nhiệm vụ nào";
    }

    public String getMapLocation() {
        if (this.template == null) {
            return "Không rõ";
        }
        return switch (this.template.id) {
            case 0 -> "Đồi hoa cúc, Thung lũng tre, Rừng nấm (Trái Đất)";
            case 1 -> "Đồi nấm tím, Thị trấn Moori, Thung lũng Maima (Namếc)";
            case 2 -> "Đồi hoang, Làng Plant, Rừng nguyên sinh (Xayda)";
            case 3 -> "Thung lũng tre, Rừng nấm, Rừng xương (Trái Đất)";
            case 4 -> "Thị trấn Moori, Thung lũng Maima, Vực maima (Namếc)";
            case 5 -> "Làng Plant, Rừng nguyên sinh, Rừng thông Xayda (Xayda)";
            case 6 -> "Rừng nấm, Rừng xương (Trái Đất)";
            case 7 -> "Thung lũng Maima, Vực maima (Namếc)";
            case 8 -> "Rừng nguyên sinh, Rừng thông Xayda (Xayda)";
            case 9 -> "Rừng xương, Rừng Bamboo, Rừng dương xỉ (Trái Đất)";
            case 10 -> "Vực maima, Núi hoa vàng, Núi hoa tím (Namếc)";
            case 11 -> "Rừng thông Xayda, Rừng cọ, Rừng đá (Xayda)";
            case 12 -> "Rừng Bamboo, Rừng dương xỉ (Trái Đất)";
            case 13 -> "Núi hoa vàng, Núi hoa tím (Namếc)";
            case 14 -> "Rừng cọ, Rừng đá (Xayda)";
            case 15 -> "Đảo Kamê, Nam Kamê (Trái Đất)";
            case 16 -> "Đảo Guru, Nam Guru (Namếc)";
            case 17 -> "Vách núi đen, Thung lũng đen (Xayda)";
            case 18 -> "Nam Kamê, Đảo Bulông (Trái Đất)";
            case 19 -> "Nam Guru, Đông Nam Guru (Namếc)";
            case 20 -> "Thung lũng đen, Bờ vực đen (Xayda)";
            case 21 -> "Đông Karin (Trái Đất)";
            case 22 -> "Thung lũng Namếc (Namếc)";
            case 23 -> "Thành phố Vegeta (Xayda)";
            case 24 -> "Đông Karin (Trái Đất), Thung lũng Nappa, Vực cấm (Nappa)";
            case 25 -> "Thung lũng Namếc (Namếc), Tây Karin (Trái Đất)";
            case 26 -> "Thành phố Vegeta (Xayda)";
            case 27 -> "Thung lũng Nappa, Vực cấm, Núi Appule (Nappa)";
            case 28 -> "Vực cấm, Núi Appule (Nappa)";
            case 29 -> "Núi Appule, Căn cứ Raspberry, Thung lũng Raspberry (Nappa)";
            case 30 -> "Căn cứ Raspberry, Thung lũng Raspberry (Nappa)";
            case 31 -> "Thung lũng Raspberry, Núi dây leo, Núi cây quỷ (Nappa)";
            case 32 -> "Núi dây leo, Núi cây quỷ (Nappa)";
            case 33 -> "Núi cây quỷ, Trại lính Fide (Nappa)";
            case 34 -> "Trại lính Fide, Trại quỷ già (Nappa)";
            case 35 -> "Trại quỷ già, Vực chết (Nappa)";
            case 36 -> "Thung lũng chết (Nappa)";
            case 37 -> "Trại lính Fide, Vực chết, Thung lũng chết, Đồi cây Fide (Nappa)";
            case 38 -> "Núi đá, Rừng đá, Hang quỷ chim, Núi khỉ đen (Nappa)";
            case 39 -> "Đồi cây Fide, Khe núi tử thần (Nappa)";
            case 40 -> "Đồi cây Fide, Khe núi tử thần, Núi đá (Nappa)";
            case 41 -> "Khe núi tử thần, Núi đá, Rừng đá, Hang quỷ chim (Nappa)";
            case 42 -> "Hang quỷ chim, Núi khỉ đen (Nappa)";
            case 43 -> "Núi khỉ đen, Hang khỉ đen (Nappa)";
            case 44 -> "Hang khỉ đen, Núi khỉ đỏ (Nappa)";
            case 45 -> "Núi khỉ vàng (Nappa)";
            case 46 -> "Thành phố phía đông (Tương Lai)";
            case 47 -> "Thành phố phía nam (Tương Lai)";
            case 48 -> "Đảo Balê (Tương Lai)";
            case 49 -> "Cao nguyên (Tương Lai)";
            case 50 -> "Thành phố phía bắc (Tương Lai)";
            case 51 -> "Ngọn núi phía bắc (Tương Lai)";
            case 52 -> "Thung lũng phía bắc (Tương Lai)";
            case 53 -> "Thị trấn Ginder (Tương Lai)";
            case 54 -> "Cánh đồng tuyết, Rừng tuyết (Cold) / Bắc thánh địa";
            case 55 -> "Rừng tuyết, Núi tuyết, Dòng sông băng (Cold) / Bắc thánh địa";
            case 56 -> "Dòng sông băng, Rừng băng, Hang băng (Cold) / Nam thánh Địa";
            case 57 -> "Rừng băng, Hang băng (Cold) / Bắc & Nam thánh địa";
            case 58 -> "Mọi bản đồ đánh quái";
            default -> "Các bản đồ tương ứng";
        };
    }

    public String getLevel() {
        switch (this.level) {
            case ConstTask.EASY:
                return "dễ";
            case ConstTask.NORMAL:
                return "bình thường";
            case ConstTask.HARD:
                return "khó";
            case ConstTask.VERY_HARD:
                return "rất khó";
            case ConstTask.HELL:
                return "địa ngục";
            default:
                return "null";
        }
    }

    public int getPercentProcess() {
        if (this.count >= this.maxCount) {
            return 100;
        }
        return (int) ((long) count * 100 / maxCount);
    }
}
