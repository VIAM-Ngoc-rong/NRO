#!/bin/bash
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKUP_DIR="/root/NRO/backups"
mkdir -p "$BACKUP_DIR"

DB_HOST="127.0.0.1"
DB_PORT="3306"
DB_NAME="team2026"
DB_USER="nro_user"
DB_PASS="NroGame@2026"

TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
DATE_ONLY=$(date +"%Y-%m-%d")
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql.gz"
DAILY_FILE="$BACKUP_DIR/${DB_NAME}_${DATE_ONLY}.sql.gz"
LATEST_FILE="$BACKUP_DIR/${DB_NAME}_latest.sql.gz"

# Dọn dẹp ổ cứng và bản backup cũ trước khi nén để tránh lỗi No space left on device
truncate -s 0 /var/log/btmp /var/log/wtmp /var/log/syslog /var/log/auth.log /var/log/kern.log /var/log/mysql/*.log 2>/dev/null || true
rm -rf /root/NRO/backups/.git /tmp/*.tmp /var/tmp/* /root/.cache/* 2>/dev/null || true
mysql -e "PURGE BINARY LOGS BEFORE NOW();" >/dev/null 2>&1 || true
ls -1t "$BACKUP_DIR"/${DB_NAME}_20*.sql.gz 2>/dev/null | tail -n +2 | xargs -r rm -f 2>/dev/null || true

mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
  --single-transaction --quick --routines --triggers "$DB_NAME" 2>/dev/null \
  | sed -e 's/utf8mb4_0900_ai_ci/utf8mb4_general_ci/g' \
        -e 's/utf8mb4_0900_as_cs/utf8mb4_bin/g' \
        -e 's/utf8mb3_general_ci/utf8mb4_general_ci/g' \
        -e 's/utf8mb3_unicode_ci/utf8mb4_unicode_ci/g' \
        -e 's/CHARACTER SET utf8mb3/CHARACTER SET utf8mb4/g' \
        -e 's/CHARSET=utf8mb3/CHARSET=utf8mb4/g' \
  | gzip > "$BACKUP_FILE"

if [ -s "$BACKUP_FILE" ]; then
    cp -f "$BACKUP_FILE" "$DAILY_FILE"
    mv -f "$BACKUP_FILE" "$LATEST_FILE"
    ls -1t "$BACKUP_DIR"/${DB_NAME}_20*.sql.gz 2>/dev/null | tail -n +3 | xargs -r rm -f 2>/dev/null || true
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] Backup MySQL thành công: $DAILY_FILE"

    if [ -f "/root/.nro_git_token" ]; then
        GIT_TOKEN=$(tr -d ' \r\n' < /root/.nro_git_token)
        if [ -n "$GIT_TOKEN" ]; then
            cd "$BACKUP_DIR" || exit 0
            rm -rf .git
            git init -b main >/dev/null 2>&1 || git init >/dev/null 2>&1
            git checkout -B main >/dev/null 2>&1
            git config user.name "NRO Auto Backup"
            git config user.email "backup@ngocrongviam.local"
            git remote add origin "https://x-access-token:${GIT_TOKEN}@github.com/VIAM-Ngoc-rong/NRO-Database-Backup.git"

            git add "${DB_NAME}_latest.sql.gz" "${DB_NAME}_${DATE_ONLY}.sql.gz" 2>/dev/null
            git commit -m "Backup MySQL ${TIMESTAMP}" >/dev/null 2>&1 || true
            if git push -u origin main --force >/dev/null 2>&1; then
                echo "[$(date +'%Y-%m-%d %H:%M:%S')] Đã đẩy Backup lên GitHub Private (VIAM-Ngoc-rong/NRO-Database-Backup) thành công!"
            else
                echo "[$(date +'%Y-%m-%d %H:%M:%S')] Lỗi khi đẩy Backup lên GitHub!"
            fi
            rm -rf .git
        fi
    fi
else
    rm -f "$BACKUP_FILE"
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] Backup MySQL thất bại!"
fi
