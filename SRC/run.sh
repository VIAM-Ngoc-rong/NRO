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

if command -v javac >/dev/null 2>&1; then
    mkdir -p build/classes
    if [ -f "build/classes/.compiled_stamp" ]; then
        CHANGED_JAVA=$(find src -name "*.java" -newer build/classes/.compiled_stamp 2>/dev/null)
    else
        CHANGED_JAVA=$(find src -name "*.java" 2>/dev/null)
    fi
    if [ -n "$CHANGED_JAVA" ]; then
        echo "Đang biên dịch mã nguồn mới cập nhật..."
        javac -encoding UTF-8 -cp "build/classes:lib/*:20.jar" -d build/classes $CHANGED_JAVA && touch build/classes/.compiled_stamp
    fi
fi

OLD_PIDS=$(pgrep -f "java.*nro.models.server.ServerManager" || true)
if [ -n "$OLD_PIDS" ]; then
    echo "Đang lưu dữ liệu và đóng tiến trình Server cũ (PID: $OLD_PIDS)..."
    kill -15 $OLD_PIDS 2>/dev/null || true
    sleep 4
fi

if [ "$1" = "--fg" ]; then
    exec nice -n 5 java -server \
      -XX:ActiveProcessorCount=2 \
      -XX:ParallelGCThreads=2 \
      -XX:ConcGCThreads=1 \
      -Xms384M -Xmx640M \
      -XX:MaxMetaspaceSize=96M \
      -Xss256k \
      -XX:+UseG1GC \
      -XX:MaxGCPauseMillis=40 \
      -XX:+UseStringDeduplication \
      -Djava.awt.headless=true \
      -Dfile.encoding=UTF-8 \
      -cp "build/classes:lib/mysql-connector-java-5.1.49.jar:lib/*:20.jar" \
      nro.models.server.ServerManager
fi

echo "Đang khởi động Ngọc Rồng VIAM (2 Cores CPU + 640MB Heap, ưu tiên nhường SmartLab x5)..."
nohup nice -n 5 java -server \
  -XX:ActiveProcessorCount=2 \
  -XX:ParallelGCThreads=2 \
  -XX:ConcGCThreads=1 \
  -Xms384M -Xmx640M \
  -XX:MaxMetaspaceSize=96M \
  -Xss256k \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=40 \
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
