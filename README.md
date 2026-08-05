# VPS Guardian

Sistema completo de monitoramento e gerenciamento remoto de servidores VPS Linux.

## Arquitetura

```
AppVps/
├── agent/          # Agente FastAPI (instalado em cada VPS)
├── android/        # App Android nativo (Kotlin + Compose)
└── docs/           # Documentação
```

### Componentes

| Componente | Tecnologia | Descrição |
|---|---|---|
| **App Android** | Kotlin, Jetpack Compose, MVVM, Room, Retrofit | Monitoramento mobile com push notifications |
| **Agente VPS** | Python, FastAPI, psutil | Coleta métricas e gerencia serviços remotamente |

## Agente VPS (FastAPI)

### Instalação rápida

```bash
# Na VPS (como root)
git clone <repo> /opt/vps-guardian
cd /opt/vps-guardian/agent
chmod +x install.sh
sudo ./install.sh
```

O script gera automaticamente um **token seguro** — guarde-o para configurar no app.

### Instalação via Docker

```bash
cd agent
docker build -t vps-guardian-agent .
docker run -d \
  --name vps-guardian \
  -p 8443:8443 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e API_TOKEN=seu-token-aqui \
  --restart unless-stopped \
  vps-guardian-agent
```

### Endpoints da API

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/v1/auth/token` | Autenticação com token do agente |
| POST | `/api/v1/auth/refresh` | Renovar JWT |
| GET | `/api/v1/inventory` | Inventário completo (VPS + serviços) |
| GET | `/api/v1/metrics` | Métricas em tempo real |
| GET | `/api/v1/boot-check` | Detecção de reinicialização |
| POST | `/api/v1/restart` | Restart autorizado |
| POST | `/api/v1/logs` | Logs de serviço |
| GET | `/api/v1/health-check/{id}` | Health check HTTP |
| GET | `/api/v1/history` | Histórico de eventos |
| GET | `/api/v1/alerts` | Alertas ativos |

### Descoberta automática

Ao conectar, o agente executa automaticamente:

- `hostname`, `uname`, `uptime`
- `systemctl list-units` (Systemd)
- `pm2 list` (PM2)
- `docker ps`, `docker compose ps` (Docker)
- `ss -tulpn` (portas)
- `free -h`, `df -h` (recursos)
- `ps aux` (processos)

### Segurança do agente

- Autenticação JWT com refresh token
- Rate limiting (120 req/min)
- Blacklist de IP após 5 tentativas inválidas
- Whitelist de comandos de restart
- HTTPS obrigatório em produção

## App Android

### Requisitos

- Android Studio Ladybug ou superior
- JDK 17
- Android SDK 35
- Dispositivo com Android 8.0+ (API 26)

### Build

```bash
cd android
./gradlew assembleDebug
```

### Funcionalidades

- **Dashboard** — Cartões coloridos por status (verde/amarelo/vermelho/cinza)
- **Descoberta automática** — Cadastro com Nome, IP, Porta e Token
- **Serviços** — Lista com filtros, restart remoto, logs
- **Métricas** — CPU, RAM, Disco, Rede, Temperatura
- **Alertas** — Push notifications para eventos críticos
- **Histórico** — Auditoria completa de ações
- **Segurança** — AES-256, biometria, PIN, sessão com expiração
- **Temas** — Claro, Escuro, AMOLED

### Estrutura (Clean Architecture)

```
app/src/main/java/com/vpsguardian/app/
├── domain/          # Models, Repository interfaces
├── data/            # Room, Retrofit, Repository impl
├── presentation/    # Compose UI, ViewModels
├── security/        # AES, Biometria
├── workers/         # WorkManager (monitoramento background)
├── services/        # Foreground service
└── di/              # Hilt modules
```

## Fluxo de uso

1. Instale o agente na VPS com `install.sh`
2. Copie o token gerado
3. No app Android, toque em **+** e cadastre: Nome, IP, Porta, Token
4. O app conecta, autentica via JWT e descobre todos os serviços
5. Monitore em tempo real pelo dashboard
6. Receba push notifications quando algo sair do ar

## Tipos de serviço suportados

Docker, Docker Compose, PM2, Systemd, FastAPI, NodeJS, Python, React, NextJS, Redis, PostgreSQL, MySQL, MariaDB, MongoDB, Nginx, Apache, Traefik, RabbitMQ, MinIO, Portainer, WPPConnect, Evolution API, Typebot, Supabase, OpenWebUI, Ollama e Serviço Personalizado.

## Licença

Projeto privado — todos os direitos reservados.
