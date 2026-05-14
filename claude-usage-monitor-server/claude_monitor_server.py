#!/usr/bin/env python3
"""
Claude Code CLI usage monitor server.
Reads ~/.claude/projects/ JSONL session files and exposes a REST API
so the Android companion app can display usage stats and billing-period progress.

Usage:
    python3 claude_monitor_server.py [--host 0.0.0.0] [--port 8765] [--claude-dir ~/.claude]
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone, timedelta
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from urllib.parse import urlparse, parse_qs
import threading
import re

# ---------------------------------------------------------------------------
# Pricing table (USD per million tokens)
# ---------------------------------------------------------------------------
PRICING = {
    "claude-opus-4-7":      {"input": 15.00, "output": 75.00, "cache_write": 18.75, "cache_read": 1.50},
    "claude-opus-4-5":      {"input": 15.00, "output": 75.00, "cache_write": 18.75, "cache_read": 1.50},
    "claude-sonnet-4-6":    {"input":  3.00, "output": 15.00, "cache_write":  3.75, "cache_read": 0.30},
    "claude-sonnet-4-5":    {"input":  3.00, "output": 15.00, "cache_write":  3.75, "cache_read": 0.30},
    "claude-haiku-4-5":     {"input":  0.80, "output":  4.00, "cache_write":  1.00, "cache_read": 0.08},
    "claude-haiku-4-5-20251001": {"input": 0.80, "output": 4.00, "cache_write": 1.00, "cache_read": 0.08},
    "_default":             {"input":  3.00, "output": 15.00, "cache_write":  3.75, "cache_read": 0.30},
}

# Max plan token limits (approximate "equivalent input tokens" per month)
# Based on Anthropic's published usage tiers.
MAX_PLAN_LIMITS = {
    "pro":    45_000_000,
    "max5x":  225_000_000,
    "max20x": 900_000_000,
    "custom": None,
}


def pricing_for(model: str) -> dict:
    if not model:
        return PRICING["_default"]
    for key in PRICING:
        if key == "_default":
            continue
        if model.startswith(key) or key in model:
            return PRICING[key]
    return PRICING["_default"]


def calc_cost(model: str, input_tok: int, output_tok: int,
              cache_write: int, cache_read: int) -> float:
    p = pricing_for(model)
    return (
        input_tok    * p["input"]       +
        output_tok   * p["output"]      +
        cache_write  * p["cache_write"] +
        cache_read   * p["cache_read"]
    ) / 1_000_000


# ---------------------------------------------------------------------------
# JSONL parsing
# ---------------------------------------------------------------------------

def _parse_ts(ts_str: str) -> datetime:
    """Parse ISO-8601 timestamp string to UTC datetime."""
    if not ts_str:
        return datetime.min.replace(tzinfo=timezone.utc)
    ts_str = ts_str.rstrip("Z")
    try:
        dt = datetime.fromisoformat(ts_str)
    except ValueError:
        return datetime.min.replace(tzinfo=timezone.utc)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt


def _project_display_name(project_dir_name: str) -> str:
    """Convert '-home-user-aiounifi' style dir name to readable project path."""
    return project_dir_name.replace("-", "/").lstrip("/")


def parse_session_file(jsonl_path: Path) -> dict:
    """
    Parse a single session JSONL file and return aggregated stats.
    Returns None if the file contains no assistant usage entries.
    """
    session_id = jsonl_path.stem
    input_tok = output_tok = cache_write = cache_read = 0
    model = None
    start_time = None
    last_time = None
    git_branch = None
    cwd = None
    user_msgs = 0
    assistant_msgs = 0

    try:
        with open(jsonl_path, encoding="utf-8") as f:
            for raw_line in f:
                raw_line = raw_line.strip()
                if not raw_line:
                    continue
                try:
                    entry = json.loads(raw_line)
                except json.JSONDecodeError:
                    continue

                ts = _parse_ts(entry.get("timestamp", ""))
                if start_time is None or ts < start_time:
                    start_time = ts
                if last_time is None or ts > last_time:
                    last_time = ts

                if not git_branch and entry.get("gitBranch"):
                    git_branch = entry["gitBranch"]
                if not cwd and entry.get("cwd"):
                    cwd = entry["cwd"]

                if entry.get("type") == "user":
                    user_msgs += 1

                if entry.get("type") == "assistant":
                    assistant_msgs += 1
                    msg = entry.get("message", {})
                    if not model and msg.get("model"):
                        model = msg["model"]
                    usage = msg.get("usage", {})
                    input_tok    += usage.get("input_tokens", 0)
                    output_tok   += usage.get("output_tokens", 0)
                    cache_write  += usage.get("cache_creation_input_tokens", 0)
                    cache_read   += usage.get("cache_read_input_tokens", 0)
    except OSError:
        return None

    if assistant_msgs == 0:
        return None

    cost = calc_cost(model or "", input_tok, output_tok, cache_write, cache_read)

    return {
        "session_id": session_id,
        "model": model or "unknown",
        "start_time": start_time.isoformat() if start_time else None,
        "last_activity": last_time.isoformat() if last_time else None,
        "input_tokens": input_tok,
        "output_tokens": output_tok,
        "cache_write_tokens": cache_write,
        "cache_read_tokens": cache_read,
        "total_tokens": input_tok + output_tok + cache_write + cache_read,
        "estimated_cost_usd": round(cost, 6),
        "git_branch": git_branch,
        "cwd": cwd,
        "user_messages": user_msgs,
        "assistant_messages": assistant_msgs,
    }


def load_all_data(claude_dir: Path) -> dict:
    """
    Walk ~/.claude/projects/ and aggregate data from all session JSONL files.
    Returns a dict with projects list and flat sessions list.
    """
    projects_dir = claude_dir / "projects"
    projects = {}
    all_sessions = []

    if not projects_dir.exists():
        return {"projects": [], "sessions": []}

    for project_dir in sorted(projects_dir.iterdir()):
        if not project_dir.is_dir():
            continue

        display_path = _project_display_name(project_dir.name)
        project_name = Path(display_path).name or display_path
        proj_sessions = []

        for jsonl_file in sorted(project_dir.glob("*.jsonl")):
            session = parse_session_file(jsonl_file)
            if session is None:
                continue
            session["project"] = project_name
            session["project_path"] = display_path
            proj_sessions.append(session)
            all_sessions.append(session)

        if proj_sessions:
            proj_input = sum(s["input_tokens"] for s in proj_sessions)
            proj_output = sum(s["output_tokens"] for s in proj_sessions)
            proj_cache_w = sum(s["cache_write_tokens"] for s in proj_sessions)
            proj_cache_r = sum(s["cache_read_tokens"] for s in proj_sessions)
            proj_cost = sum(s["estimated_cost_usd"] for s in proj_sessions)
            projects[project_name] = {
                "name": project_name,
                "path": display_path,
                "sessions": len(proj_sessions),
                "input_tokens": proj_input,
                "output_tokens": proj_output,
                "cache_write_tokens": proj_cache_w,
                "cache_read_tokens": proj_cache_r,
                "total_tokens": proj_input + proj_output + proj_cache_w + proj_cache_r,
                "estimated_cost_usd": round(proj_cost, 6),
            }

    all_sessions.sort(key=lambda s: s.get("last_activity") or "", reverse=True)
    return {
        "projects": sorted(projects.values(), key=lambda p: p["total_tokens"], reverse=True),
        "sessions": all_sessions,
    }


def compute_stats(data: dict, billing_start: datetime | None, plan_limit: int | None) -> dict:
    """Compute aggregate stats and billing-period progress."""
    sessions = data["sessions"]
    now = datetime.now(timezone.utc)

    # All-time aggregates
    total_input = sum(s["input_tokens"] for s in sessions)
    total_output = sum(s["output_tokens"] for s in sessions)
    total_cache_w = sum(s["cache_write_tokens"] for s in sessions)
    total_cache_r = sum(s["cache_read_tokens"] for s in sessions)
    total_cost = sum(s["estimated_cost_usd"] for s in sessions)

    # Billing period aggregates
    period_input = period_output = period_cache_w = period_cache_r = period_cost = 0
    if billing_start:
        for s in sessions:
            last = _parse_ts(s.get("last_activity") or "")
            if last and last >= billing_start:
                period_input   += s["input_tokens"]
                period_output  += s["output_tokens"]
                period_cache_w += s["cache_write_tokens"]
                period_cache_r += s["cache_read_tokens"]
                period_cost    += s["estimated_cost_usd"]

    # Today aggregates
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    today_input = today_output = today_cache_w = today_cache_r = today_cost = 0
    for s in sessions:
        last = _parse_ts(s.get("last_activity") or "")
        if last and last >= today_start:
            today_input   += s["input_tokens"]
            today_output  += s["output_tokens"]
            today_cache_w += s["cache_write_tokens"]
            today_cache_r += s["cache_read_tokens"]
            today_cost    += s["estimated_cost_usd"]

    # Models used
    models_used = list({s["model"] for s in sessions if s.get("model") != "unknown"})

    # Billing period progress
    period_total_tokens = period_input + period_output + period_cache_w + period_cache_r
    usage_pct = None
    days_remaining = None
    billing_end = None
    if billing_start:
        # Billing period is 1 calendar month from start day
        start_day = billing_start.day
        next_month = billing_start.month + 1 if billing_start.month < 12 else 1
        next_year = billing_start.year if billing_start.month < 12 else billing_start.year + 1
        try:
            billing_end = billing_start.replace(year=next_year, month=next_month)
        except ValueError:
            billing_end = billing_start.replace(
                year=next_year, month=next_month,
                day=1
            ) + timedelta(days=start_day - 1)
        days_remaining = max(0, (billing_end - now).total_seconds() / 86400)
        if plan_limit:
            usage_pct = min(100.0, period_total_tokens / plan_limit * 100)

    return {
        "total_input_tokens":       total_input,
        "total_output_tokens":      total_output,
        "total_cache_write_tokens": total_cache_w,
        "total_cache_read_tokens":  total_cache_r,
        "total_tokens":             total_input + total_output + total_cache_w + total_cache_r,
        "total_estimated_cost_usd": round(total_cost, 4),
        "total_sessions":           len(sessions),
        "total_projects":           len(data["projects"]),
        "today_input_tokens":       today_input,
        "today_output_tokens":      today_output,
        "today_cache_write_tokens": today_cache_w,
        "today_cache_read_tokens":  today_cache_r,
        "today_tokens":             today_input + today_output + today_cache_w + today_cache_r,
        "today_estimated_cost_usd": round(today_cost, 4),
        "period_input_tokens":      period_input,
        "period_output_tokens":     period_output,
        "period_tokens":            period_total_tokens,
        "period_estimated_cost_usd": round(period_cost, 4),
        "period_usage_pct":         round(usage_pct, 2) if usage_pct is not None else None,
        "plan_token_limit":         plan_limit,
        "billing_start":            billing_start.isoformat() if billing_start else None,
        "billing_end":              billing_end.isoformat() if billing_end else None,
        "days_remaining_in_period": round(days_remaining, 1) if days_remaining is not None else None,
        "models_used":              sorted(models_used),
        "last_updated":             now.isoformat(),
    }


# ---------------------------------------------------------------------------
# HTTP server
# ---------------------------------------------------------------------------

class Handler(BaseHTTPRequestHandler):
    # Injected by server startup
    claude_dir: Path = None
    billing_start: datetime = None
    plan_limit: int = None

    def log_message(self, fmt, *args):
        print(f"[{self.address_string()}] {fmt % args}", file=sys.stderr)

    def _send_json(self, payload: dict, status: int = 200):
        body = json.dumps(payload, default=str).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _send_error(self, message: str, status: int = 400):
        self._send_json({"error": message}, status)

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        qs = parse_qs(parsed.query)

        # Allow per-request billing_start and plan_limit overrides via query params
        billing_start = self.billing_start
        plan_limit = self.plan_limit
        if "billing_start" in qs:
            try:
                billing_start = datetime.fromisoformat(qs["billing_start"][0])
                if billing_start.tzinfo is None:
                    billing_start = billing_start.replace(tzinfo=timezone.utc)
            except ValueError:
                pass
        if "plan_limit" in qs:
            try:
                plan_limit = int(qs["plan_limit"][0])
            except ValueError:
                pass

        if path == "/health":
            self._send_json({"status": "ok", "claude_dir": str(self.claude_dir)})

        elif path == "/stats":
            data = load_all_data(self.claude_dir)
            stats = compute_stats(data, billing_start, plan_limit)
            self._send_json(stats)

        elif path == "/projects":
            data = load_all_data(self.claude_dir)
            self._send_json({"projects": data["projects"]})

        elif path == "/sessions":
            data = load_all_data(self.claude_dir)
            limit = int(qs.get("limit", ["100"])[0])
            offset = int(qs.get("offset", ["0"])[0])
            sessions = data["sessions"][offset: offset + limit]
            self._send_json({
                "sessions": sessions,
                "total": len(data["sessions"]),
                "limit": limit,
                "offset": offset,
            })

        elif path == "/all":
            data = load_all_data(self.claude_dir)
            stats = compute_stats(data, billing_start, plan_limit)
            self._send_json({
                "stats": stats,
                "projects": data["projects"],
                "sessions": data["sessions"][:50],
            })

        else:
            self._send_error(f"Unknown endpoint: {path}", 404)


def run(host: str, port: int, claude_dir: Path,
        billing_start: datetime | None, plan_limit: int | None):
    # Inject config into handler class
    Handler.claude_dir = claude_dir
    Handler.billing_start = billing_start
    Handler.plan_limit = plan_limit

    server = HTTPServer((host, port), Handler)
    print(f"Claude Monitor Server running on http://{host}:{port}")
    print(f"  Claude dir : {claude_dir}")
    print(f"  Billing start: {billing_start or 'not set (pass ?billing_start=YYYY-MM-DD)'}")
    print(f"  Plan limit : {plan_limit or 'not set (pass ?plan_limit=N)'}")
    print("Endpoints: /health  /stats  /projects  /sessions  /all")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down.")
        server.shutdown()


def main():
    parser = argparse.ArgumentParser(description="Claude Code usage monitor server")
    parser.add_argument("--host", default="0.0.0.0", help="Bind address (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=8765, help="Port (default: 8765)")
    parser.add_argument("--claude-dir", default=str(Path.home() / ".claude"),
                        help="Path to ~/.claude directory")
    parser.add_argument("--billing-start", default=None,
                        help="Billing period start date YYYY-MM-DD (e.g. 2026-05-01)")
    parser.add_argument("--plan", default=None,
                        choices=list(MAX_PLAN_LIMITS.keys()),
                        help="Claude Max plan tier for usage-limit tracking")
    parser.add_argument("--plan-limit", type=int, default=None,
                        help="Custom token limit for billing period (overrides --plan)")
    args = parser.parse_args()

    claude_dir = Path(args.claude_dir).expanduser()
    if not claude_dir.exists():
        print(f"ERROR: Claude directory not found: {claude_dir}", file=sys.stderr)
        sys.exit(1)

    billing_start = None
    if args.billing_start:
        try:
            billing_start = datetime.fromisoformat(args.billing_start).replace(tzinfo=timezone.utc)
        except ValueError:
            print(f"ERROR: Invalid --billing-start date: {args.billing_start}", file=sys.stderr)
            sys.exit(1)

    plan_limit = args.plan_limit
    if plan_limit is None and args.plan:
        plan_limit = MAX_PLAN_LIMITS.get(args.plan)

    run(args.host, args.port, claude_dir, billing_start, plan_limit)


if __name__ == "__main__":
    main()
