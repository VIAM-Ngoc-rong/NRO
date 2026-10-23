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
        $JAVAC_BIN -encoding UTF-8 -cp "src:build/classes:lib/*:20.jar" -d build/classes $OUTDATED_JAVA && touch build/classes/.compiled_stamp
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

# Tối ưu Kernel Linux cho phép JVM lấy tối đa cả RAM vật lý lẫn toàn bộ Swap (3GB Swap + 1.5GB RAM)
sysctl -w vm.swappiness=100 >/dev/null 2>&1 || true
sysctl -w vm.overcommit_memory=1 >/dev/null 2>&1 || true
sysctl -w vm.overcommit_ratio=100 >/dev/null 2>&1 || true

# Tự động tính tổng bộ nhớ (RAM vật lý + Swap) để cấp tối đa Heap cho NRO
TOTAL_RAM_MB=$(awk '/MemTotal/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null || echo 1536)
TOTAL_SWAP_MB=$(awk '/SwapTotal/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null || echo 3072)
TOTAL_COMBINED_MB=$((TOTAL_RAM_MB + TOTAL_SWAP_MB))
# Chừa ~650MB cho OS + MySQL + JVM Metaspace, toàn bộ phần còn lại (RAM + Swap, khoảng ~3800MB-3900MB) cấp hết cho NRO Heap
MAX_HEAP_MB=$((TOTAL_COMBINED_MB - 650))
if [ "$MAX_HEAP_MB" -lt 2048 ]; then
    MAX_HEAP_MB=3072
fi

if [ "$1" = "--fg" ]; then
    exec java -server \
      -XX:ActiveProcessorCount=2 \
      -XX:ParallelGCThreads=2 \
      -XX:ConcGCThreads=1 \
      -Xms512M -Xmx${MAX_HEAP_MB}M \
      -XX:-UseGCOverheadLimit \
      -XX:MinHeapFreeRatio=10 \
      -XX:MaxHeapFreeRatio=30 \
      -XX:+UseG1GC \
      -XX:MaxGCPauseMillis=100 \
      -XX:+UseStringDeduplication \
      -Djava.awt.headless=true \
      -Dfile.encoding=UTF-8 \
      -cp "build/classes:lib/mysql-connector-java-5.1.49.jar:lib/*:20.jar" \
      nro.models.server.ServerManager
fi

echo "Đang khởi động Ngọc Rồng VIAM (Bung tối đa RAM + Swap: Heap 512MB ~ ${MAX_HEAP_MB}MB trên tổng ${TOTAL_RAM_MB}MB RAM + ${TOTAL_SWAP_MB}MB Swap)..."
nohup java -server \
  -XX:ActiveProcessorCount=2 \
  -XX:ParallelGCThreads=2 \
  -XX:ConcGCThreads=1 \
  -Xms512M -Xmx${MAX_HEAP_MB}M \
  -XX:-UseGCOverheadLimit \
  -XX:MinHeapFreeRatio=10 \
  -XX:MaxHeapFreeRatio=30 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
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
