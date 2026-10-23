#!/bin/bash
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
cd "$(dirname "$0")"
SCRIPT_DIR="$(pwd)"

# Mở port 14446 cho Web Giám Sát Admin (nếu VPS bật UFW / iptables)
if command -v ufw >/dev/null 2>&1; then
    ufw allow 14446/tcp >/dev/null 2>&1 || true
fi
if command -v iptables >/dev/null 2>&1; then
    iptables -C INPUT -p tcp --dport 14446 -j ACCEPT >/dev/null 2>&1 || iptables -I INPUT -p tcp --dport 14446 -j ACCEPT >/dev/null 2>&1 || true
fi

if [ -f "backup_db.sh" ]; then
    sed -i 's/\r$//' backup_db.sh 2>/dev/null || true
    chmod +x backup_db.sh
    # Tự động đăng ký cron backup mỗi ngày lúc 04:50 sáng nếu chưa có
    if command -v crontab >/dev/null 2>&1; then
        (crontab -l 2>/dev/null | grep -v "backup_db.sh"; echo "50 4 * * * $SCRIPT_DIR/backup_db.sh >> /root/NRO/backups/backup.log 2>&1") | crontab - 2>/dev/null || true
    fi
    # Chạy 1 bản backup ngay khi khởi động server nếu hôm nay chưa có bản backup nào hoặc chưa khởi tạo git backup
    TODAY_FILE="/root/NRO/backups/team2026_$(date +'%Y-%m-%d').sql.gz"
    if [ ! -s "$TODAY_FILE" ] || [ ! -d "/root/NRO/backups/.git" ]; then
        ./backup_db.sh >> /root/NRO/backups/backup.log 2>&1 &
    fi
fi

if [ -f "dist/NgocRongOnline.jar" ]; then
    cp -f "dist/NgocRongOnline.jar" "20.jar"
fi

# Mặc định tự động chạy ngầm (nohup) để tắt SSH không bị sập Server
if [ "$1" != "--fg" ]; then
    OLD_PID=$(pgrep -f "nro.models.server.ServerManager" || true)
    if [ -n "$OLD_PID" ]; then
        echo "Đang lưu dữ liệu và tắt tiến trình Server cũ (PID: $OLD_PID)..."
        pkill -TERM -f "nro.models.server.ServerManager" 2>/dev/null || true
        sleep 4
    fi
    echo "Đang khởi động Ngọc Rồng VIAM ở chế độ chạy ngầm (Background)..."
    nohup "$SCRIPT_DIR/run.sh" --fg > "$SCRIPT_DIR/server.log" 2>&1 < /dev/null &
    NEW_PID=$!
    sleep 4
    echo "=========================================================="
    echo "Server đã chạy ngầm thành công! (PID: $NEW_PID)"
    echo "Bạn có thể đóng cửa sổ SSH thoải mái, Server vẫn hoạt động 24/7."
    echo "Xem log trực tiếp bất kỳ lúc nào bằng lệnh: tail -f /root/NRO/SRC/server.log"
    echo "=========================================================="
    tail -n 15 "$SCRIPT_DIR/server.log" 2>/dev/null || true
    exit 0
fi

exec java -server \
  -Xms256M -Xmx512M \
  -XX:MaxMetaspaceSize=96M \
  -Xss256k \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:+UseStringDeduplication \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8 \
  -cp "lib/mysql-connector-java-5.1.49.jar:lib/*:20.jar" \
  nro.models.server.ServerManager
