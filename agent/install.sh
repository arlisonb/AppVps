#!/bin/bash
set -euo pipefail

INSTALL_DIR="/opt/vps-guardian"
SERVICE_NAME="vps-guardian"
USER="vps-guardian"

echo "=== VPS Guardian Agent - Instalação ==="

# Verificar root
if [ "$EUID" -ne 0 ]; then
    echo "Execute como root: sudo $0"
    exit 1
fi

# Criar usuário
if ! id "$USER" &>/dev/null; then
    useradd -r -s /bin/false -d "$INSTALL_DIR" "$USER"
    echo "Usuário $USER criado"
fi

# Criar diretórios
mkdir -p "$INSTALL_DIR" /var/lib/vps-guardian/history /etc/vps-guardian
cp -r . "$INSTALL_DIR/"
chown -R "$USER:$USER" "$INSTALL_DIR" /var/lib/vps-guardian

# Instalar dependências Python
cd "$INSTALL_DIR"
python3 -m venv venv
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt

# Configuração
if [ ! -f /etc/vps-guardian/.env ]; then
    TOKEN=$(openssl rand -hex 32)
    JWT_SECRET=$(openssl rand -hex 32)
    cp .env.example /etc/vps-guardian/.env
    sed -i "s/change-me-use-openssl-rand-hex-32/$TOKEN/" /etc/vps-guardian/.env
    sed -i "s/change-me-use-openssl-rand-hex-32/$JWT_SECRET/" /etc/vps-guardian/.env
    echo ""
    echo "========================================="
    echo "  TOKEN DO AGENTE (guarde com segurança):"
    echo "  $TOKEN"
    echo "========================================="
    echo ""
fi

# Systemd service
cat > /etc/systemd/system/${SERVICE_NAME}.service << EOF
[Unit]
Description=VPS Guardian Agent
After=network.target docker.service
Wants=docker.service

[Service]
Type=simple
User=$USER
Group=$USER
WorkingDirectory=$INSTALL_DIR
EnvironmentFile=/etc/vps-guardian/.env
ExecStart=$INSTALL_DIR/venv/bin/python run.py
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

# Permissões para monitoramento
SupplementaryGroups=docker

[Install]
WantedBy=multi-user.target
EOF

# Permissões para systemctl, docker, pm2
usermod -aG docker,systemd-journal "$USER" 2>/dev/null || true

systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
systemctl start "$SERVICE_NAME"

echo ""
echo "=== Instalação concluída ==="
echo "Status: systemctl status $SERVICE_NAME"
echo "Logs:   journalctl -u $SERVICE_NAME -f"
echo "Porta:  8443 (configure firewall e TLS)"
