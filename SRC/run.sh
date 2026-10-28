#!/bin/bash
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
cd "$(dirname "$0")"
SCRIPT_DIR="$(pwd)"

# Tự động dọn dẹp ổ cứng VPS triệt để (tránh lỗi No space left on device lúc 2h sáng)
apt-get clean >/dev/null 2>&1 || true
apt-get autoremove -y --purge >/dev/null 2>&1 || true
truncate -s 0 /var/log/btmp /var/log/wtmp /var/log/syslog /var/log/auth.log /var/log/kern.log /var/log/dpkg.log /var/log/ufw.log /var/log/mysql/*.log 2>/dev/null || true
rm -rf /var/cache/apt/archives/*.deb /var/cache/apt/archives/partial/* /var/lib/apt/lists/* /tmp/*.tmp /tmp/hsperfdata_* /var/tmp/* /var/crash/* /var/lib/systemd/coredump/* /root/.cache/* /root/NRO/backups/.git 2>/dev/null || true
journalctl --vacuum-size=10M >/dev/null 2>&1 || true
find /var/log -type f \( -name "*.gz" -o -name "*.1" -o -name "*.old" \) -delete 2>/dev/null || true
if [ -d "/etc/mysql/mysql.conf.d" ] && [ ! -f "/etc/mysql/mysql.conf.d/disable_binlog.cnf" ]; then
    printf "[mysqld]\nskip-log-bin\n" > /etc/mysql/mysql.conf.d/disable_binlog.cnf 2>/dev/null || true
fi
mysql -e "PURGE BINARY LOGS BEFORE NOW(); SET GLOBAL binlog_expire_logs_seconds = 600;" >/dev/null 2>&1 || true
rm -f /var/lib/mysql/binlog.0* /var/lib/mysql/mysql-bin.0* 2>/dev/null || true
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
    TODAY_FILE="/root/NRO/backups/team2026_$(date +'%Y-%m-%d').sql.gz"
    if [ ! -s "$TODAY_FILE" ] || [ ! -d "/root/NRO/backups/.git" ]; then
        ./backup_db.sh >> /root/NRO/backups/backup.log 2>&1 &
    fi
fi

if [ -f "dist/NgocRongOnline.jar" ]; then
    cp -f "dist/NgocRongOnline.jar" "20.jar"
fi

# Tự động tìm hoặc cài đặt javac trên VPS nếu chưa có để luôn biên dịch được code mới nhất
JAVAC_BIN=$(command -v javac 2>/dev/null || true)
if [ -z "$JAVAC_BIN" ]; then
    for candidate in /usr/lib/jvm/*/bin/javac; do
        if [ -x "$candidate" ]; then
            JAVAC_BIN="$candidate"
            break
        fi
    done
fi
if [ -z "$JAVAC_BIN" ] && command -v apt-get >/dev/null 2>&1; then
    echo "Đang cài đặt trình biên dịch javac (JDK Headless) cho VPS..."
    JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    apt-get update -qq >/dev/null 2>&1 || true
    if [ -n "$JAVA_VER" ] && [ "$JAVA_VER" -ge 8 ] 2>/dev/null; then
        DEBIAN_FRONTEND=noninteractive apt-get install -y -qq "openjdk-${JAVA_VER}-jdk-headless" >/dev/null 2>&1 || \
        DEBIAN_FRONTEND=noninteractive apt-get install -y -qq default-jdk-headless >/dev/null 2>&1 || true
    else
        DEBIAN_FRONTEND=noninteractive apt-get install -y -qq default-jdk-headless >/dev/null 2>&1 || true
    fi
    JAVAC_BIN=$(command -v javac 2>/dev/null || true)
fi

if [ -n "$JAVAC_BIN" ]; then
    mkdir -p build/classes
    OUTDATED_JAVA=""
    while IFS= read -r -d '' jfile; do
        rel="${jfile#src/}"
        cfile="build/classes/${rel%.java}.class"
        if [ ! -f "$cfile" ] || [ "$jfile" -nt "$cfile" ]; then
            OUTDATED_JAVA="$OUTDATED_JAVA $jfile"
        fi
    done < <(find src -name "*.java" -print0 2>/dev/null)

    if [ -n "$OUTDATED_JAVA" ]; then
        echo "Đang biên dịch toàn bộ mã nguồn mới cập nhật sang .class..."
        $JAVAC_BIN -encoding UTF-8 -proc:none -implicit:none -cp "build/classes:lib/*:20.jar:src" -d build/classes $OUTDATED_JAVA && touch build/classes/.compiled_stamp
    fi
fi

OLD_PIDS=$(pgrep -f "java.*nro.models.server.ServerManager" || true)
if [ -n "$OLD_PIDS" ]; then
    echo "Đang lưu dữ liệu và đóng tiến trình Server cũ (PID: $OLD_PIDS)..."
    kill -15 $OLD_PIDS 2>/dev/null || true
    for i in $(seq 1 12); do
        if ! pgrep -f "java.*nro.models.server.ServerManager" >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    REMAIN_PIDS=$(pgrep -f "java.*nro.models.server.ServerManager" || true)
    if [ -n "$REMAIN_PIDS" ]; then
        echo "Đang buộc đóng tiến trình bị treo (PID: $REMAIN_PIDS)..."
        kill -9 $REMAIN_PIDS 2>/dev/null || true
        sleep 1
    fi
fi

# Tối ưu Kernel Linux: ưu tiên giữ Game & MySQL trên RAM vật lý tốc độ cao (swappiness=20), chỉ đẩy sang 3GB Swap khi tràn RAM (tránh nghẽn I/O đĩa gây lag)
sysctl -w vm.swappiness=20 >/dev/null 2>&1 || true
sysctl -w vm.overcommit_memory=1 >/dev/null 2>&1 || true
sysctl -w vm.overcommit_ratio=100 >/dev/null 2>&1 || true

# Cấp phát Heap gọn nhẹ (1024MB) và thu gom rác (G1GC) liên tục để RAM luôn nhẹ (~200-350MB)
TOTAL_RAM_MB=$(awk '/MemTotal/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null || echo 1536)
TOTAL_SWAP_MB=$(awk '/SwapTotal/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null || echo 3072)
MAX_HEAP_MB=1024

if [ "$1" = "--fg" ]; then
    exec java -server \
      -XX:ActiveProcessorCount=2 \
      -XX:ParallelGCThreads=2 \
      -XX:ConcGCThreads=1 \
      -Xms256M -Xmx${MAX_HEAP_MB}M \
      -XX:-UseGCOverheadLimit \
      -XX:MinHeapFreeRatio=10 \
      -XX:MaxHeapFreeRatio=25 \
      -XX:+UseG1GC \
      -XX:InitiatingHeapOccupancyPercent=30 \
      -XX:MaxGCPauseMillis=50 \
      -XX:+UseStringDeduplication \
      -Djava.awt.headless=true \
      -Dfile.encoding=UTF-8 \
      -cp "build/classes:lib/mysql-connector-java-5.1.49.jar:lib/*:20.jar" \
      nro.models.server.ServerManager
fi

echo "Đang khởi động Ngọc Rồng VIAM (Heap gọn nhẹ: 256MB ~ ${MAX_HEAP_MB}MB trên tổng ${TOTAL_RAM_MB}MB RAM + ${TOTAL_SWAP_MB}MB Swap)..."
nohup java -server \
  -XX:ActiveProcessorCount=2 \
  -XX:ParallelGCThreads=2 \
  -XX:ConcGCThreads=1 \
  -Xms256M -Xmx${MAX_HEAP_MB}M \
  -XX:-UseGCOverheadLimit \
  -XX:MinHeapFreeRatio=10 \
  -XX:MaxHeapFreeRatio=25 \
  -XX:+UseG1GC \
  -XX:InitiatingHeapOccupancyPercent=30 \
  -XX:MaxGCPauseMillis=50 \
  -XX:+UseStringDeduplication \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8 \
  -cp "build/classes:lib/mysql-connector-java-5.1.49.jar:lib/*:20.jar" \
  nro.models.server.ServerManager > "$SCRIPT_DIR/server.log" 2>&1 < /dev/null &
NEW_PID=$!
disown $NEW_PID 2>/dev/null || true

sleep 8
echo "=========================================================="
echo "Server đã chạy ngầm thành công! (PID: $NEW_PID)"
echo "Bạn có thể đóng cửa sổ SSH thoải mái, Server vẫn hoạt động 24/7."
echo "Xem log trực tiếp: tail -f /root/NRO/SRC/server.log"
echo "=========================================================="
tail -n 30 "$SCRIPT_DIR/server.log" 2>/dev/null || true
