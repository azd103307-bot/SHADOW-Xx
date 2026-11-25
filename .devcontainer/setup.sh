#!/usr/bin/env bash
set -euo pipefail
sudo apt-get update
sudo apt-get install -y --no-install-recommends ffmpeg imagemagick git curl
sudo rm -rf /var/lib/apt/lists/*
if ! command -v uv >/dev/null 2>&1; then
  curl -LsSf https://astral.sh/uv/install.sh | sh
  export PATH="$HOME/.local/bin:$PATH"
fi
if [ ! -d "$HOME/MoneyPrinterTurbo/.git" ]; then
  git clone https://github.com/harry0703/MoneyPrinterTurbo.git "$HOME/MoneyPrinterTurbo"
fi
cd "$HOME/MoneyPrinterTurbo"
uv sync --frozen
if [ ! -f config.toml ]; then cp config.example.toml config.toml; fi
echo
echo "MANSER setup finished."
echo "Add provider API keys to ~/MoneyPrinterTurbo/config.toml."
echo "Then run: cd ~/MoneyPrinterTurbo && uv run python main.py"
echo "The API listens on port 8080."
