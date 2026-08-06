#!/bin/bash
# Nginx + SSL (Let's Encrypt) para VPS Guardian Agent
# Uso: sudo ./setup-nginx-ssl.sh vps.meuappagenda.com.br seu@email.com

set -euo pipefail

DOMAIN="${1:-vps.meuappagenda.com.br}"
EMAIL="${2:-admin@${DOMAIN}}"
AGENT_PORT="${AGENT_PORT:-8443}"

if [ "$EUID" -ne 0 ]; then
    echo "Execute como root: sudo $0 dominio email"
    exit 1
fi

echo "=== Configurando Nginx + SSL para $DOMAIN ==="

apt-get update
apt-get install -y nginx certbot python3-certbot-nginx

cat > /etc/nginx/sites-available/vps-guardian << EOF
server {
    listen 80;
    server_name ${DOMAIN};

    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name ${DOMAIN};

    ssl_certificate     /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    client_max_body_size 10M;

    location / {
        proxy_pass http://127.0.0.1:${AGENT_PORT};
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }
}
EOF

# Certificado SSL (primeira execução)
if [ ! -d "/etc/letsencrypt/live/${DOMAIN}" ]; then
    # Config temporária só HTTP para validação
    cat > /etc/nginx/sites-available/vps-guardian-temp << EOF
server {
    listen 80;
    server_name ${DOMAIN};
    location / {
        proxy_pass http://127.0.0.1:${AGENT_PORT};
    }
}
EOF
    ln -sf /etc/nginx/sites-available/vps-guardian-temp /etc/nginx/sites-enabled/vps-guardian
    rm -f /etc/nginx/sites-enabled/default
    nginx -t && systemctl reload nginx

    certbot certonly --nginx -d "$DOMAIN" --email "$EMAIL" --agree-tos --non-interactive
fi

ln -sf /etc/nginx/sites-available/vps-guardian /etc/nginx/sites-enabled/vps-guardian
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
systemctl enable nginx

# Renovação automática
systemctl enable certbot.timer 2>/dev/null || true

echo ""
echo "=== SSL configurado com sucesso ==="
echo "URL:  https://${DOMAIN}/health"
echo "API:  https://${DOMAIN}/api/v1/"
echo ""
echo "Teste: curl https://${DOMAIN}/health"
