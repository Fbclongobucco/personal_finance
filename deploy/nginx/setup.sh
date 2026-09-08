#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_NAME="personal-finance-app.conf"
SRC="$SCRIPT_DIR/$CONF_NAME"
DEST="/etc/nginx/sites-available/$CONF_NAME"
LINK="/etc/nginx/sites-enabled/$CONF_NAME"

if [ ! -f "$SRC" ]; then
    echo "Config not found: $SRC" >&2
    exit 1
fi

echo "Installing $CONF_NAME to $DEST"
sudo cp "$SRC" "$DEST"

if [ ! -L "$LINK" ]; then
    echo "Enabling site"
    sudo ln -s "$DEST" "$LINK"
else
    echo "Site already enabled"
fi

echo "Testing nginx configuration"
sudo nginx -t

echo "Reloading nginx"
sudo systemctl reload nginx

echo "Done."
