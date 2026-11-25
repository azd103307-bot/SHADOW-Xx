#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${HOME}/MoneyPrinterTurbo"
if ! command -v git >/dev/null 2>&1; then
  echo "ERROR: git is not installed in this Studio."
  exit 1
fi

if [ ! -d "${APP_DIR}/.git" ]; then
  git clone --depth 1 https://github.com/harry0703/MoneyPrinterTurbo.git "${APP_DIR}"
else
  git -C "${APP_DIR}" pull --ff-only
fi

if ! command -v uv >/dev/null 2>&1; then
  python -m pip install --user uv
  export PATH="${HOME}/.local/bin:${PATH}"
fi

cd "${APP_DIR}"
uv sync --frozen

if [ ! -f config.toml ]; then
  cp config.example.toml config.toml
  echo
  echo "Created config.toml from config.example.toml."
  echo "Add your own provider keys to config.toml before generating videos."
fi

echo
echo "Starting MoneyPrinterTurbo API on 0.0.0.0:8080"
echo "In Lightning AI, expose port 8080 with the API Builder or Ports plugin."
echo "Then set the resulting HTTPS URL in MANSER AI > Settings."
exec uv run python main.py
