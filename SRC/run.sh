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
