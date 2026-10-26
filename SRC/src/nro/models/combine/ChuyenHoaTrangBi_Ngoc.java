package nro.models.combine;

import nro.models.player.Player;

/**
 * Chuyển hóa Trang bị tại Bà Hạt Mít - Đảo Kame:
 * Đồng bộ hoàn toàn sang cơ chế Chuyển hóa bằng 2 tỷ vàng (chuyển Sao Pha Lê, Cấp [+] và SKH sang Phôi mới).
 */
public class ChuyenHoaTrangBi_Ngoc {

    public static void chuyenHoaTrangBiNgoc(Player player) {
        ChuyenHoaTrangBi_Vang.chuyenHoaTrangBiVang(player);
    }

    public static void thucHienChuyenHoa(Player player) {
        ChuyenHoaTrangBi_Vang.thucHienChuyenHoa(player);
    }
}
