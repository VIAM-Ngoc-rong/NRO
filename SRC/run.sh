#!/bin/bash
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
cd "$(dirname "$0")"
SCRIPT_DIR="$(pwd)"

# 1. Dừng Watchdog cũ và đóng tiến trình Server cũ TRƯỚC TIÊN để giải phóng RAM và Port 14445/14446
touch "$SCRIPT_DIR/.stop_watchdog" 2>/dev/null || true
if [ -f "$SCRIPT_DIR/.watchdog.pid" ]; then
    OLD_WD_PID=$(cat "$SCRIPT_DIR/.watchdog.pid" 2>/dev/null || true)
    if [ -n "$OLD_WD_PID" ]; then
        kill -9 $OLD_WD_PID 2>/dev/null || true
    fi
    rm -f "$SCRIPT_DIR/.watchdog.pid" 2>/dev/null || true
fi

OLD_PIDS=$(pgrep -f "java.*(nro.models.server.ServerManager|20.jar|NgocRongOnline.jar)" || true)
if [ -n "$OLD_PIDS" ]; then
    echo "Đang lưu dữ liệu và đóng tiến trình Server cũ (PID: $OLD_PIDS)..."
    kill -15 $OLD_PIDS 2>/dev/null || true
    for i in $(seq 1 12); do
        if ! pgrep -f "java.*(nro.models.server.ServerManager|20.jar|NgocRongOnline.jar)" >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    REMAIN_PIDS=$(pgrep -f "java.*(nro.models.server.ServerManager|20.jar|NgocRongOnline.jar)" || true)
    if [ -n "$REMAIN_PIDS" ]; then
        echo "Đang buộc đóng tiến trình bị treo (PID: $REMAIN_PIDS)..."
        kill -9 $REMAIN_PIDS 2>/dev/null || true
        sleep 1
    fi
fi

# 1.5. Tự động kéo code & bản build mới nhất từ GitHub (sau khi đã đóng tiến trình Java cũ)
if command -v git >/dev/null 2>&1; then
    echo "Đang tự động cập nhật code mới nhất từ GitHub (origin/main)..."
    git fetch --depth=1 origin main >/dev/null 2>&1 && git reset --hard origin/main >/dev/null 2>&1 || true
    sed -i 's/\r$//' "$SCRIPT_DIR/run.sh" "$SCRIPT_DIR/backup_db.sh" 2>/dev/null || true
    chmod +x "$SCRIPT_DIR/run.sh" "$SCRIPT_DIR/backup_db.sh" 2>/dev/null || true
fi

# 2. Tự động dọn dẹp ổ cứng VPS gọn nhẹ (tránh lỗi No space left on device)
apt-get clean >/dev/null 2>&1 || true
truncate -s 0 /var/log/btmp /var/log/wtmp /var/log/syslog /var/log/auth.log /var/log/kern.log /var/log/dpkg.log /var/log/ufw.log /var/log/mysql/*.log 2>/dev/null || true
rm -rf /var/cache/apt/archives/*.deb /var/cache/apt/archives/partial/* /var/lib/apt/lists/* /tmp/*.tmp /tmp/hsperfdata_* /var/tmp/* /var/crash/* /var/lib/systemd/coredump/* /root/.cache/* /root/NRO/backups/.git 2>/dev/null || true
journalctl --vacuum-size=10M >/dev/null 2>&1 || true
find /var/log -type f \( -name "*.gz" -o -name "*.1" -o -name "*.old" \) -delete 2>/dev/null || true
if [ -d "/etc/mysql/mysql.conf.d" ]; then
    if ! grep -q "performance_schema=OFF" /etc/mysql/mysql.conf.d/disable_binlog.cnf 2>/dev/null; then
        printf "[mysqld]\nskip-log-bin\nperformance_schema=OFF\ninnodb_buffer_pool_size=128M\nmax_connections=100\n" > /etc/mysql/mysql.conf.d/disable_binlog.cnf 2>/dev/null || true
        systemctl restart mysql >/dev/null 2>&1 || service mysql restart >/dev/null 2>&1 || true
    fi
fi
mkdir -p lib 2>/dev/null || true
if [ -f "data/map/tile_set_Info" ] && [ ! -f "data/map/tile_set_info" ]; then
    cp -f "data/map/tile_set_Info" "data/map/tile_set_info"
fi
if [ ! -s "lib/mysql-connector-java-5.1.49.jar" ]; then
    echo "Đang tải thư viện MySQL Connector 5.1.49..."
    curl -sL "https://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.49/mysql-connector-java-5.1.49.jar" -o "lib/mysql-connector-java-5.1.49.jar" || \
    wget -qO "lib/mysql-connector-java-5.1.49.jar" "https://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.49/mysql-connector-java-5.1.49.jar" || true
fi
mysql -e "PURGE BINARY LOGS BEFORE NOW(); SET GLOBAL binlog_expire_logs_seconds = 600; SET GLOBAL max_connections = 100;" >/dev/null 2>&1 || true
rm -f /var/lib/mysql/binlog.0* /var/lib/mysql/mysql-bin.0* 2>/dev/null || true

# 3. Đảm bảo dịch vụ MySQL đang chạy và tài khoản nro_user sẵn sàng trên VPS
systemctl start mysql >/dev/null 2>&1 || service mysql start >/dev/null 2>&1 || true
mysql -e "CREATE USER IF NOT EXISTS 'nro_user'@'127.0.0.1' IDENTIFIED BY 'NroGame@2026'; CREATE USER IF NOT EXISTS 'nro_user'@'localhost' IDENTIFIED BY 'NroGame@2026'; ALTER USER 'nro_user'@'127.0.0.1' IDENTIFIED BY 'NroGame@2026'; ALTER USER 'nro_user'@'localhost' IDENTIFIED BY 'NroGame@2026'; GRANT ALL PRIVILEGES ON *.* TO 'nro_user'@'127.0.0.1' WITH GRANT OPTION; GRANT ALL PRIVILEGES ON *.* TO 'nro_user'@'localhost' WITH GRANT OPTION; FLUSH PRIVILEGES;" >/dev/null 2>&1 || true
if [ -f "Config.properties" ]; then
    sed -i 's/^database\.user=.*/database.user=nro_user/' Config.properties 2>/dev/null || true
    sed -i 's/^database\.pass=.*/database.pass=NroGame@2026/' Config.properties 2>/dev/null || true
    sed -i 's/^database\.max=.*/database.max=20/' Config.properties 2>/dev/null || true
fi
mkdir -p /root/NRO/backups 2>/dev/null || true
if [ -d "/root/NRO/backups" ]; then
    ls -1t /root/NRO/backups/team2026_20*.sql.gz 2>/dev/null | tail -n +2 | xargs -r rm -f 2>/dev/null || true
fi
if [ -f "$SCRIPT_DIR/server.log" ] && [ "$(stat -c%s "$SCRIPT_DIR/server.log" 2>/dev/null || echo 0)" -gt 5242880 ]; then
    tail -n 500 "$SCRIPT_DIR/server.log" > "$SCRIPT_DIR/server.log.tmp" && mv -f "$SCRIPT_DIR/server.log.tmp" "$SCRIPT_DIR/server.log"
fi

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
    if command -v crontab >/dev/null 2>&1; then
        (crontab -l 2>/dev/null | grep -v "backup_db.sh"; echo "50 4 * * * $SCRIPT_DIR/backup_db.sh >> /root/NRO/backups/backup.log 2>&1") | crontab - 2>/dev/null || true
    fi
fi

if [ -f "dist/NgocRongOnline.jar" ]; then
    cp -f "dist/NgocRongOnline.jar" "20.jar"
fi

# Tối ưu Kernel Linux: đẩy các tiến trình nền ít dùng (SmartLab/OS cache) sang 3GB Swap (swappiness=60) để nhường tối đa RAM vật lý cho NRO
sysctl -w vm.swappiness=60 >/dev/null 2>&1 || true
sysctl -w vm.vfs_cache_pressure=100 >/dev/null 2>&1 || true
sysctl -w vm.overcommit_memory=1 >/dev/null 2>&1 || true
sysctl -w vm.overcommit_ratio=100 >/dev/null 2>&1 || true

# Bảo vệ MySQL tuyệt đối khỏi OOM Killer
MYSQL_PID_GUARD=$(pgrep -x mysqld | head -n 1 || true)
if [ -n "$MYSQL_PID_GUARD" ] && [ -w "/proc/$MYSQL_PID_GUARD/oom_score_adj" ]; then
    echo -900 > "/proc/$MYSQL_PID_GUARD/oom_score_adj" 2>/dev/null || true
fi

# Cấp phát tối đa cho NRO (960MB Heap + ~220MB Non-Heap = ~1180MB, tận dụng RAM vật lý + đẩy nền sang Swap)
TOTAL_RAM_MB=$(awk '/MemTotal/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null || echo 1536)
TOTAL_SWAP_MB=$(awk '/SwapTotal/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null || echo 3072)
MAX_HEAP_MB=960
if [ "$TOTAL_RAM_MB" -ge 2000 ] 2>/dev/null; then
    MAX_HEAP_MB=$(( TOTAL_RAM_MB * 75 / 100 ))
fi

rm -f "$SCRIPT_DIR/.stop_watchdog" 2>/dev/null || true

if [ "$1" = "--fg" ]; then
    exec nice -n -10 java -server \
      -XX:ActiveProcessorCount=2 \
      -XX:ParallelGCThreads=2 \
      -XX:ConcGCThreads=1 \
      -Xms256M -Xmx${MAX_HEAP_MB}M \
      -XX:MaxMetaspaceSize=128M \
      -Xss512k \
      -XX:-UseGCOverheadLimit \
      -XX:MinHeapFreeRatio=10 \
      -XX:MaxHeapFreeRatio=25 \
      -XX:+UseG1GC \
      -XX:+UnlockExperimentalVMOptions \
      -XX:G1NewSizePercent=10 \
      -XX:G1MaxNewSizePercent=25 \
      -XX:+ExplicitGCInvokesConcurrent \
      -XX:InitiatingHeapOccupancyPercent=35 \
      -XX:MaxGCPauseMillis=50 \
      -XX:+UseStringDeduplication \
      -Djava.awt.headless=true \
      -Dfile.encoding=UTF-8 \
      -cp "build/classes:lib/mysql-connector-java-5.1.49.jar:lib/*:20.jar" \
      nro.models.server.ServerManager
fi

echo "Đang khởi động Ngọc Rồng VIAM kèm Watchdog 24/7 (Ưu tiên tối đa NRO: Heap 256MB ~ ${MAX_HEAP_MB}MB trên tổng ${TOTAL_RAM_MB}MB RAM + ${TOTAL_SWAP_MB}MB Swap)..."
nohup /bin/bash -c "
  while [ ! -f \"$SCRIPT_DIR/.stop_watchdog\" ]; do
    echo \"[\$(date '+%Y-%m-%d %H:%M:%S')] Khởi chạy JVM ServerManager (Ưu tiên CPU -10 & OOM Guard -900)...\" >> \"$SCRIPT_DIR/server.log\"
    nice -n -10 java -server \
      -XX:ActiveProcessorCount=2 \
      -XX:ParallelGCThreads=2 \
      -XX:ConcGCThreads=1 \
      -Xms256M -Xmx${MAX_HEAP_MB}M \
      -XX:MaxMetaspaceSize=128M \
      -Xss512k \
      -XX:-UseGCOverheadLimit \
      -XX:MinHeapFreeRatio=10 \
      -XX:MaxHeapFreeRatio=25 \
      -XX:+UseG1GC \
      -XX:+UnlockExperimentalVMOptions \
      -XX:G1NewSizePercent=10 \
      -XX:G1MaxNewSizePercent=25 \
      -XX:+ExplicitGCInvokesConcurrent \
      -XX:InitiatingHeapOccupancyPercent=35 \
      -XX:MaxGCPauseMillis=50 \
      -XX:+UseStringDeduplication \
      -Djava.awt.headless=true \
      -Dfile.encoding=UTF-8 \
      -cp \"build/classes:lib/mysql-connector-java-5.1.49.jar:lib/*:20.jar\" \
      nro.models.server.ServerManager >> \"$SCRIPT_DIR/server.log\" 2>&1 < /dev/null &
    JAVA_CHILD_PID=\$!
    if [ -w \"/proc/\$JAVA_CHILD_PID/oom_score_adj\" ]; then
      echo -900 > \"/proc/\$JAVA_CHILD_PID/oom_score_adj\" 2>/dev/null || true
    fi
    ionice -c 2 -n 0 -p \$JAVA_CHILD_PID >/dev/null 2>&1 || true
    wait \$JAVA_CHILD_PID
    EXIT_CODE=\$?
    if [ -f \"$SCRIPT_DIR/.stop_watchdog\" ]; then
      break
    fi
    echo \"[\$(date '+%Y-%m-%d %H:%M:%S')] [WATCHDOG] Server vừa dừng (Mã thoát: \$EXIT_CODE). Đang tự động git pull và khởi động lại sau 3 giây...\" >> \"$SCRIPT_DIR/server.log\"
    if command -v git >/dev/null 2>&1; then
      git fetch --depth=1 origin main >/dev/null 2>&1 && git reset --hard origin/main >/dev/null 2>&1 || true
    fi
    if [ -f \"$SCRIPT_DIR/dist/NgocRongOnline.jar\" ]; then
      cp -f \"$SCRIPT_DIR/dist/NgocRongOnline.jar\" \"$SCRIPT_DIR/20.jar\" 2>/dev/null || true
    fi
    sleep 3
  done
" >/dev/null 2>&1 < /dev/null &
WD_PID=$!
echo "$WD_PID" > "$SCRIPT_DIR/.watchdog.pid"
disown $WD_PID 2>/dev/null || true

sleep 8
NEW_PID=$(ps -C java -o pid= 2>/dev/null | head -n 1 | tr -d ' ' || echo "$WD_PID")
echo "=========================================================="
echo "Server đã chạy ngầm thành công! (Java PID: $NEW_PID | Watchdog PID: $WD_PID)"
echo "Bạn có thể đóng cửa sổ SSH thoải mái, Server vẫn hoạt động 24/7."
echo "Xem log trực tiếp: tail -f /root/NRO/SRC/server.log"
echo "=========================================================="
tail -n 30 "$SCRIPT_DIR/server.log" 2>/dev/null || true
