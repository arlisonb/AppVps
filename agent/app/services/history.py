"""Histórico de eventos e auditoria."""

import json
import uuid
from datetime import datetime, timezone
from pathlib import Path

from app.models.schemas import HistoryEntry


class HistoryService:
    """Registra e consulta histórico de eventos."""

    def __init__(self, history_dir: Path) -> None:
        self.history_dir = history_dir
        self.history_dir.mkdir(parents=True, exist_ok=True)
        self._audit_file = history_dir / "audit.jsonl"

    def record(
        self,
        event_type: str,
        target: str,
        description: str,
        metadata: dict | None = None,
    ) -> HistoryEntry:
        entry = HistoryEntry(
            id=str(uuid.uuid4())[:8],
            event_type=event_type,
            target=target,
            description=description,
            metadata=metadata or {},
            timestamp=datetime.now(timezone.utc),
        )

        with open(self._audit_file, "a", encoding="utf-8") as f:
            f.write(entry.model_dump_json() + "\n")

        return entry

    def get_history(
        self,
        limit: int = 100,
        event_type: str | None = None,
        target: str | None = None,
    ) -> list[HistoryEntry]:
        if not self._audit_file.exists():
            return []

        entries: list[HistoryEntry] = []
        try:
            lines = self._audit_file.read_text(encoding="utf-8").strip().splitlines()
            for line in reversed(lines):
                if not line:
                    continue
                try:
                    entry = HistoryEntry.model_validate_json(line)
                    if event_type and entry.event_type != event_type:
                        continue
                    if target and entry.target != target:
                        continue
                    entries.append(entry)
                    if len(entries) >= limit:
                        break
                except Exception:
                    continue
        except OSError:
            pass

        return entries
