#!/usr/bin/env python3
"""从 App 的 sounds_remote.json 同步网页播放器（docs/player.html）的内嵌声音清单。

用法: python3 scripts/gen_player_sounds.py
在 App 中添加/修改远程声音后运行，网页播放器自动跟上。
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP_JSON = ROOT / "app" / "src" / "main" / "assets" / "sounds_remote.json"
PLAYER_HTML = ROOT / "docs" / "player.html"


def build_inline(data: dict) -> str:
    out = {
        "version": data.get("version"),
        "categories": [
            {
                "id": c.get("id"),
                "name": c.get("name"),
                "nameEn": c.get("nameEn"),
                "nameZhTW": c.get("nameZhTW"),
                "nameKo": c.get("nameKo"),
                "nameJa": c.get("nameJa"),
                "nameRu": c.get("nameRu"),
            }
            for c in data.get("categories", [])
        ],
        "sounds": [
            {
                "id": s.get("id"),
                "name": s.get("name"),
                "nameEn": s.get("nameEn"),
                "nameZhTW": s.get("nameZhTW"),
                "nameKo": s.get("nameKo"),
                "nameJa": s.get("nameJa"),
                "nameRu": s.get("nameRu"),
                "category": s.get("category"),
                "remoteUrl": s.get("remoteUrl"),
                "isSeamless": s.get("isSeamless", True),
                "format": s.get("format", "ogg"),
            }
            for s in data.get("sounds", [])
        ],
    }
    return "const BUILTIN_DATA = " + json.dumps(out, ensure_ascii=False, separators=(",", ":")) + ";"


def main() -> int:
    if not APP_JSON.exists():
        print(f"错误: 找不到 {APP_JSON}", file=sys.stderr)
        return 1
    if not PLAYER_HTML.exists():
        print(f"错误: 找不到 {PLAYER_HTML}", file=sys.stderr)
        return 1

    data = json.loads(APP_JSON.read_text(encoding="utf-8"))
    inline = build_inline(data)

    html = PLAYER_HTML.read_text(encoding="utf-8")
    new_html, n = re.subn(
        r"const BUILTIN_DATA = \{.*?\};\n",
        inline + "\n",
        html,
        count=1,
        flags=re.DOTALL,
    )
    if n != 1:
        print("错误: 未找到现有 BUILTIN_DATA 块，请手动检查 player.html", file=sys.stderr)
        return 1

    PLAYER_HTML.write_text(new_html, encoding="utf-8")
    sounds = len(data.get("sounds", []))
    print(f"已同步 {sounds} 个声音到 {PLAYER_HTML.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
