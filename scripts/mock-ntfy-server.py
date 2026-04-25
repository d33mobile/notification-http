#!/usr/bin/env python3
"""
Tiny ntfy-style HTTP sink for smoke-testing the webhook delivery path.
Every POST is appended (one JSON line) to a log file; non-2xx responses can
be requested via /fail next, then /ok next.

Usage:
  python3 mock-ntfy-server.py <port> <log-file>
"""
import json
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

if len(sys.argv) != 3:
    print(__doc__, file=sys.stderr)
    sys.exit(2)

PORT = int(sys.argv[1])
LOG_PATH = sys.argv[2]
NEXT_STATUS = {"code": 200}


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        return  # silence access log

    def do_POST(self):
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length).decode("utf-8", errors="replace")
        record = {
            "ts": time.time(),
            "path": self.path,
            "title": self.headers.get("Title"),
            "tags": self.headers.get("Tags"),
            "auth": self.headers.get("Authorization"),
            "body": body,
        }
        with open(LOG_PATH, "a") as f:
            f.write(json.dumps(record) + "\n")
        code = NEXT_STATUS["code"]
        self.send_response(code)
        self.end_headers()
        self.wfile.write(b"")

    def do_GET(self):
        # /fail/503 → next POST returns 503; /ok → next POST returns 200
        if self.path.startswith("/fail/"):
            NEXT_STATUS["code"] = int(self.path.split("/")[-1])
        elif self.path == "/ok":
            NEXT_STATUS["code"] = 200
        self.send_response(200)
        self.end_headers()
        self.wfile.write(f"next={NEXT_STATUS['code']}\n".encode())


with ThreadingHTTPServer(("0.0.0.0", PORT), Handler) as srv:
    print(f"mock-ntfy listening on :{PORT}, log -> {LOG_PATH}")
    srv.serve_forever()
