"""Utilitários para execução de comandos no sistema."""

import asyncio
import json
import re
from typing import Any


async def run_command(
    command: str | list[str],
    timeout: int = 30,
    shell: bool = True,
) -> tuple[int, str, str]:
    """Executa comando e retorna (returncode, stdout, stderr)."""
    if isinstance(command, str):
        proc = await asyncio.create_subprocess_shell(
            command,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
    else:
        proc = await asyncio.create_subprocess_exec(
            *command,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )

    try:
        stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout=timeout)
    except asyncio.TimeoutError:
        proc.kill()
        return -1, "", "Timeout"

    return (
        proc.returncode or 0,
        stdout.decode("utf-8", errors="replace").strip(),
        stderr.decode("utf-8", errors="replace").strip(),
    )


def safe_json_loads(text: str, default: Any = None) -> Any:
    if not text:
        return default
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return default


def extract_port(text: str) -> int | None:
    """Extrai primeira porta de strings como '0.0.0.0:8080' ou ':3000'."""
    match = re.search(r":(\d{1,5})(?:\s|$|/|,)", text)
    if match:
        port = int(match.group(1))
        if 1 <= port <= 65535:
            return port
    return None


def normalize_service_name(name: str) -> str:
    return re.sub(r"[^a-zA-Z0-9_-]", "_", name.lower()).strip("_")
