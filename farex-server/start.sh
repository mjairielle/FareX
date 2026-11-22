#!/bin/sh
# Container start (Dockerfile CMD). The data folder holds the accounts, cards and ledger. A container's own disk is
# wiped on every deploy, so on Railway it must be a volume: one is used wherever it is mounted
# (Railway sets RAILWAY_VOLUME_MOUNT_PATH), else DATA_DIR (/app/data).
DATA_DIR="${RAILWAY_VOLUME_MOUNT_PATH:-$DATA_DIR}"
export DATA_DIR
mkdir -p "$DATA_DIR"
if [ -n "$RAILWAY_ENVIRONMENT_NAME$RAILWAY_ENVIRONMENT" ] && [ -z "$RAILWAY_VOLUME_MOUNT_PATH" ]; then
  echo "WARNING: no Railway volume attached. Accounts, cards and balances are lost on every deploy."
  echo "WARNING: in Railway, attach a volume to this service (mount path /app/data)."
fi
cp -n /app/default-data/* "$DATA_DIR"/ 2>/dev/null || true   # route files it lacks; never overwrites admin edits
if [ "$(id -u)" = 0 ] && command -v su >/dev/null; then
  # A volume is mounted owned by root: give it to the server's user, then drop root (no su: stays root, still works)
  chown -R farex:farex "$DATA_DIR"
  exec su -s /bin/sh farex -c 'exec java -cp "farex-server.jar:libs/*" farex.server.Main'
fi
exec java -cp "farex-server.jar:libs/*" farex.server.Main
