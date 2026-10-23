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

mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
  --single-transaction --quick --routines --triggers "$DB_NAME" 2>/dev/null | gzip > "$BACKUP_FILE"

if [ -s "$BACKUP_FILE" ]; then
    cp -f "$BACKUP_FILE" "$DAILY_FILE"
    cp -f "$BACKUP_FILE" "$LATEST_FILE"
    find "$BACKUP_DIR" -name "${DB_NAME}_20*.sql.gz" -mtime +14 -delete
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] Backup MySQL thành công: $BACKUP_FILE"

    if [ -f "/root/.nro_git_token" ]; then
        GIT_TOKEN=$(tr -d ' \r\n' < /root/.nro_git_token)
        if [ -n "$GIT_TOKEN" ]; then
            cd "$BACKUP_DIR" || exit 0
            if [ ! -d ".git" ]; then
                git init -b main >/dev/null 2>&1 || git init >/dev/null 2>&1
                git checkout -B main >/dev/null 2>&1
            fi
            git config user.name "NRO Auto Backup"
            git config user.email "backup@ngocrongviam.local"
            git remote remove origin >/dev/null 2>&1 || true
            git remote add origin "https://x-access-token:${GIT_TOKEN}@github.com/VIAM-Ngoc-rong/NRO-Database-Backup.git"

            git add "${DB_NAME}_latest.sql.gz" "${DB_NAME}_${DATE_ONLY}.sql.gz" 2>/dev/null
            git commit -m "Backup MySQL ${TIMESTAMP}" >/dev/null 2>&1 || true
            if git push -u origin main --force >/dev/null 2>&1; then
                echo "[$(date +'%Y-%m-%d %H:%M:%S')] Đã đẩy Backup lên GitHub Private (VIAM-Ngoc-rong/NRO-Database-Backup) thành công!"
            else
                echo "[$(date +'%Y-%m-%d %H:%M:%S')] Lỗi khi đẩy Backup lên GitHub!"
            fi
        fi
    fi
else
    rm -f "$BACKUP_FILE"
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] Backup MySQL thất bại!"
fi
