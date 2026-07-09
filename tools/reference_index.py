#!/usr/bin/env python3
"""Fast path/name index for HBM parity references.

This tool answers one question: where does a class, asset, recipe, or system
name appear across the configured reference tiers?

It does not choose behavior. The canonical hierarchy document still decides
which tier is authoritative for each type of question.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path
from typing import Any


SCRIPT_DIR = Path(__file__).resolve().parent
CONFIG_PATH = SCRIPT_DIR / "references.json"
INDEX_PATH = SCRIPT_DIR / ".reference_index.json"
REPO_ROOT = SCRIPT_DIR.parent
DEFAULT_EXTRACT_ROOT = REPO_ROOT / "extracted"


def load_config() -> dict[str, Any]:
    if not CONFIG_PATH.exists():
        print(f"[!] No config at {CONFIG_PATH}.")
        print("    Copy references.example.json to references.json and fill in local paths.")
        sys.exit(1)
    with CONFIG_PATH.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def index_archive(tier_key: str, path: str) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    with zipfile.ZipFile(path, "r") as archive:
        for info in archive.infolist():
            if info.is_dir():
                continue
            entries.append({
                "tier": tier_key,
                "container": path,
                "container_kind": "archive",
                "entry_path": info.filename,
                "name": os.path.basename(info.filename),
                "size": info.file_size,
            })
    return entries


def index_dir(tier_key: str, root: str) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    root_path = Path(root)
    ignored_dirs = {".git", ".gradle", "build", "out", "node_modules", ".idea", "run", "extracted"}
    ignored_files = {"references.json", ".reference_index.json"}
    for dirpath, dirnames, filenames in os.walk(root_path):
        dirnames[:] = [directory for directory in dirnames if directory not in ignored_dirs]
        for filename in filenames:
            if filename in ignored_files:
                continue
            full_path = Path(dirpath) / filename
            rel_path = full_path.relative_to(root_path)
            try:
                size = full_path.stat().st_size
            except OSError:
                size = -1
            entries.append({
                "tier": tier_key,
                "container": str(root_path),
                "container_kind": "dir",
                "entry_path": str(rel_path).replace("\\", "/"),
                "name": filename,
                "size": size,
            })
    return entries


def cmd_build(_args: argparse.Namespace) -> None:
    config = load_config()
    all_entries: list[dict[str, Any]] = []
    for tier_key, meta in config.items():
        path = meta["path"]
        kind = meta.get("kind", "dir")
        label = meta.get("label", tier_key)
        if not os.path.exists(path):
            print(f"[!] {tier_key} ({label}): path not found, skipping: {path}")
            continue
        print(f"[.] Indexing {tier_key} ({label}) as {kind}: {path}")
        try:
            entries = index_archive(tier_key, path) if kind in ("jar", "zip") else index_dir(tier_key, path)
        except Exception as exc:
            print(f"    [!] failed to index {tier_key}: {exc}")
            continue
        print(f"    -> {len(entries)} entries")
        all_entries.extend(entries)

    with INDEX_PATH.open("w", encoding="utf-8") as handle:
        json.dump({"config": config, "entries": all_entries}, handle)
    print(f"\nIndexed {len(all_entries)} total entries across {len(config)} configured tiers.")
    print(f"Wrote index to {INDEX_PATH}")


def load_index() -> dict[str, Any]:
    if not INDEX_PATH.exists():
        print("[!] No index found. Run `build` first.")
        sys.exit(1)
    with INDEX_PATH.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def matches(entry: dict[str, Any], query: str, regex_mode: bool) -> bool:
    haystack = entry["entry_path"]
    if regex_mode:
        import re
        return re.search(query, haystack, re.IGNORECASE) is not None
    return query.lower() in haystack.lower()


def cmd_search(args: argparse.Namespace) -> None:
    data = load_index()
    results = [entry for entry in data["entries"] if matches(entry, args.query, args.regex)]
    if args.tier:
        results = [entry for entry in results if entry["tier"] == args.tier]

    if not results:
        print(f"No matches for '{args.query}'.")
        return

    for entry in results[: args.limit]:
        label = data["config"].get(entry["tier"], {}).get("label", entry["tier"])
        print(f"[{entry['tier']:<16}] {label}")
        print(f"    {entry['entry_path']}  ({entry['size']} bytes)")
    print(f"\n{min(len(results), args.limit)} shown of {len(results)} match(es).")


def cmd_compare(args: argparse.Namespace) -> None:
    data = load_index()
    by_tier: dict[str, list[dict[str, Any]]] = {}
    for entry in data["entries"]:
        if matches(entry, args.query, args.regex):
            by_tier.setdefault(entry["tier"], []).append(entry)

    print(f"Query: '{args.query}'\n")
    for tier_key in data["config"].keys():
        label = data["config"][tier_key].get("label", tier_key)
        hits = by_tier.get(tier_key, [])
        marker = "YES" if hits else "no match"
        print(f"[{marker:>9}] {tier_key:<16} {label}")
        for entry in hits[: args.limit]:
            print(f"                  -> {entry['entry_path']}")
        if len(hits) > args.limit:
            print(f"                  ... and {len(hits) - args.limit} more")
    print()


def cmd_extract(args: argparse.Namespace) -> None:
    data = load_index()
    config = data["config"]
    if args.tier not in config:
        print(f"[!] Unknown tier '{args.tier}'. Known tiers: {list(config.keys())}")
        sys.exit(1)

    meta = config[args.tier]
    kind = meta.get("kind", "dir")
    container = meta["path"]
    out_dir = Path(args.out) if args.out else DEFAULT_EXTRACT_ROOT / args.tier
    out_dir.mkdir(parents=True, exist_ok=True)

    entry_path = args.entry_path
    if kind in ("jar", "zip"):
        with zipfile.ZipFile(container, "r") as archive:
            try:
                data_bytes = archive.read(entry_path)
            except KeyError:
                print(f"[!] '{entry_path}' not found inside {container}")
                sys.exit(1)
        out_file = out_dir / Path(entry_path).name
        with out_file.open("wb") as handle:
            handle.write(data_bytes)
        print(f"Extracted to {out_file}")

        if out_file.suffix == ".class":
            decompiler_cmd = meta.get("decompiler_cmd")
            if decompiler_cmd:
                command = decompiler_cmd.format(input=str(out_file), outputdir=str(out_dir))
                print(f"Running decompiler: {command}")
                subprocess.run(command, shell=True, check=False)
            else:
                print("[i] Extracted a compiled .class file. Add decompiler_cmd to this tier to auto-decompile.")
    else:
        source = Path(container) / entry_path
        if not source.exists():
            print(f"[!] '{source}' does not exist")
            sys.exit(1)
        out_file = out_dir / Path(entry_path).name
        shutil.copy2(source, out_file)
        print(f"Copied to {out_file}")


def cmd_tiers(_args: argparse.Namespace) -> None:
    config = load_config()
    print("Configured tiers:")
    for key, meta in config.items():
        exists = "OK" if os.path.exists(meta["path"]) else "MISSING"
        print(f"  {key:<16} [{exists:^7}] {meta.get('label', '')} ({meta.get('kind', 'dir')}) {meta['path']}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    build = sub.add_parser("build", help="Rebuild the reference index")
    build.set_defaults(func=cmd_build)

    search = sub.add_parser("search", help="Search all tiers for a path/name fragment")
    search.add_argument("query")
    search.add_argument("--tier")
    search.add_argument("--regex", action="store_true")
    search.add_argument("--limit", type=int, default=50)
    search.set_defaults(func=cmd_search)

    compare = sub.add_parser("compare", help="Show presence/absence across tiers")
    compare.add_argument("query")
    compare.add_argument("--regex", action="store_true")
    compare.add_argument("--limit", type=int, default=5)
    compare.set_defaults(func=cmd_compare)

    extract = sub.add_parser("extract", help="Extract one file from a configured tier")
    extract.add_argument("tier")
    extract.add_argument("entry_path")
    extract.add_argument("--out")
    extract.set_defaults(func=cmd_extract)

    tiers = sub.add_parser("tiers", help="List configured tiers and path status")
    tiers.set_defaults(func=cmd_tiers)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
