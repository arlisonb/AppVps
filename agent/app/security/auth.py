"""Autenticação JWT e controle de acesso."""

import time
from collections import defaultdict
from datetime import datetime, timedelta, timezone

from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from passlib.context import CryptContext

from app.config import Settings, get_settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
security = HTTPBearer(auto_error=False)

# Rate limiting e blacklist em memória
_failed_attempts: dict[str, list[float]] = defaultdict(list)
_blacklist: dict[str, float] = {}
_request_counts: dict[str, list[float]] = defaultdict(list)


def _client_ip(request: Request) -> str:
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        return forwarded.split(",")[0].strip()
    if request.client:
        return request.client.host
    return "unknown"


def check_blacklist(request: Request, settings: Settings) -> None:
    ip = _client_ip(request)
    if ip in _blacklist:
        if time.time() < _blacklist[ip]:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="IP bloqueado temporariamente",
            )
        del _blacklist[ip]


def check_rate_limit(request: Request, settings: Settings) -> None:
    ip = _client_ip(request)
    now = time.time()
    window = 60.0
    _request_counts[ip] = [t for t in _request_counts[ip] if now - t < window]
    if len(_request_counts[ip]) >= settings.rate_limit_per_minute:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Limite de requisições excedido",
        )
    _request_counts[ip].append(now)


def record_failed_attempt(request: Request, settings: Settings) -> None:
    ip = _client_ip(request)
    now = time.time()
    window = 300.0
    _failed_attempts[ip] = [t for t in _failed_attempts[ip] if now - t < window]
    _failed_attempts[ip].append(now)
    if len(_failed_attempts[ip]) >= settings.max_failed_attempts:
        _blacklist[ip] = now + (settings.blacklist_duration_minutes * 60)
        _failed_attempts[ip].clear()


def create_access_token(data: dict, settings: Settings, expires_delta: timedelta | None = None) -> str:
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + (
        expires_delta or timedelta(minutes=settings.jwt_expire_minutes)
    )
    to_encode.update({"exp": expire, "type": "access"})
    return jwt.encode(to_encode, settings.jwt_secret, algorithm=settings.jwt_algorithm)


def create_refresh_token(data: dict, settings: Settings) -> str:
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(days=30)
    to_encode.update({"exp": expire, "type": "refresh"})
    return jwt.encode(to_encode, settings.jwt_secret, algorithm=settings.jwt_algorithm)


def verify_token(token: str, settings: Settings, expected_type: str = "access") -> dict:
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
        if payload.get("type") != expected_type:
            raise HTTPException(status_code=401, detail="Tipo de token inválido")
        return payload
    except JWTError as exc:
        raise HTTPException(status_code=401, detail="Token inválido ou expirado") from exc


async def get_current_user(
    request: Request,
    credentials: HTTPAuthorizationCredentials | None = Depends(security),
    settings: Settings = Depends(get_settings),
) -> dict:
    check_blacklist(request, settings)
    check_rate_limit(request, settings)

    if not credentials:
        record_failed_attempt(request, settings)
        raise HTTPException(status_code=401, detail="Credenciais não fornecidas")

    return verify_token(credentials.credentials, settings, "access")
