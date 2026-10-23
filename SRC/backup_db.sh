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
    # Giữ lại 14 ngày gần nhất đối với bản backup theo giờ/ngày để tránh đầy ổ cứng
    find "$BACKUP_DIR" -name "${DB_NAME}_20*.sql.gz" -mtime +14 -delete
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] Backup MySQL thành công: $BACKUP_FILE"
else
    rm -f "$BACKUP_FILE"
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] Backup MySQL thất bại!"
fi
