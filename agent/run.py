"""Entry point do agente VPS Guardian."""

import uvicorn

from app.config import get_settings


def main():
    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        ssl_certfile=settings.ssl_certfile,
        ssl_keyfile=settings.ssl_keyfile,
        reload=False,
        log_level="info",
    )


if __name__ == "__main__":
    main()
