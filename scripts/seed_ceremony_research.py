#!/usr/bin/env python3
"""Seed ceremony invitation JPEGs into the templates table via the API.

Sources may be Figma/HTML during research. The stored asset is always JPEG.
"""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESEARCH = Path(__file__).resolve().parent / "ceremony_invitation_research.json"
CACHE = Path(__file__).resolve().parent / ".template-cache" / "ceremony-jpg"
LIBRARY_EVENT_ID = "00000000-0000-0000-0000-000000000000"
USER_AGENT = "InviteFlowSeeder/1.0 (ceremony jpeg templates)"

CATEGORY_TO_EVENT_TYPE = {
    "Wedding": "WEDDING",
    "Engagement": "CELEBRATION",
    "Send-off": "SEND_OFF",
    "Graduation": "GRADUATION",
    "Gala": "GALA",
    "Award Ceremony": "CORPORATE",
    "Anniversary": "ANNIVERSARY",
    "Formal Dinner": "GALA",
    "Corporate Ceremony": "CORPORATE",
    "Religious Ceremony": "CHURCH",
    "General Event": "OTHER",
    "Other": "OTHER",
}

DEFAULT_FIELDS = [
    {
        "fieldType": "EVENT_NAME",
        "x": 12.0,
        "y": 14.0,
        "width": 76.0,
        "height": 8.0,
        "fontSize": 28,
        "fontColor": "#1A1A1A",
        "alignment": "CENTER",
        "fontWeight": "BOLD",
        "fontFamily": "Serif",
    },
    {
        "fieldType": "GUEST_NAME",
        "x": 12.0,
        "y": 40.0,
        "width": 76.0,
        "height": 10.0,
        "fontSize": 36,
        "fontColor": "#1A1A1A",
        "alignment": "CENTER",
        "fontWeight": "BOLD",
        "fontFamily": "Serif",
    },
    {
        "fieldType": "QR_CODE",
        "x": 38.0,
        "y": 72.0,
        "width": 24.0,
        "height": 24.0,
        "fontSize": 12,
        "fontColor": "#1A1A1A",
        "alignment": "CENTER",
        "fontWeight": "NORMAL",
        "fontFamily": "SansSerif",
        "qrSize": 180,
    },
]


def log(msg: str) -> None:
    print(msg, flush=True)


def sql_quote(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def delete_previous_research_rows() -> None:
    names = [str(item.get("template_title") or "") for item in json.loads(RESEARCH.read_text())]
    extra = [
        "Wedding Invitation (Orchid Theme)",
        "Wedding Invitation (Community)",
        "Wedding Invite",
        "Wedding Invitation",
        "Wedding Invitation Card Minimal",
        "Engagement Invitation",
        "Modern HTML/CSS Wedding Invitation",
        "Undangan Pernikahan HTML Template",
        "Wedding Invite Starter Kit",
        "Weddingly Free Digital Invitation",
        "Wedding Invite Website Template",
        "Graduation Invitation Template",
        "JSON-Driven Event Invitation Template",
        "Floral Oval Wedding Invitation",
        "Orchid Digital Wedding Invitation",
        "Mobile Wedding Invitation",
        "Floral Engagement Invitation",
        "Graduation Ceremony Invitation",
    ]
    all_names = sorted({n for n in names + extra if n})
    in_list = ", ".join(sql_quote(n) for n in all_names)
    sql = f"DELETE FROM templates WHERE template_name IN ({in_list});"
    try:
        subprocess.check_call(["mysql", "-u", "root", "invitation_system", "-e", sql])
        log("Removed previous research rows from templates.")
    except Exception as exc:
        log(f"Could not delete previous rows: {exc}")


def api_json(base: str, method: str, path: str, token: str | None = None, body: dict | None = None) -> tuple[int, object]:
    import ssl
    import urllib.error
    import urllib.request

    ctx = ssl.create_default_context()
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {"User-Agent": USER_AGENT, "Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60, context=ctx) as res:
            raw = res.read()
            return res.status, json.loads(raw.decode("utf-8")) if raw else {}
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        try:
            parsed = json.loads(raw.decode("utf-8"))
        except Exception:
            parsed = raw.decode("utf-8", errors="replace")
        return exc.code, parsed


def sign_in(base: str, email: str, password: str) -> str:
    code, payload = api_json(base, "POST", "/api/v1/auth/login", body={"email": email, "password": password})
    if code == 200 and isinstance(payload, dict) and payload.get("token"):
        return str(payload["token"])
    code, payload = api_json(
        base,
        "POST",
        "/api/v1/auth/register",
        body={
            "firstName": "Template",
            "lastName": "Seeder",
            "email": email,
            "password": password,
            "role": "EVENT_MANAGER",
        },
    )
    if code in (200, 201) and isinstance(payload, dict) and payload.get("token"):
        return str(payload["token"])
    raise SystemExit(f"Could not sign in to seed templates ({code}): {payload}")


def upload_jpeg(base: str, token: str, item: dict, jpeg_path: Path) -> tuple[int, object]:
    event_type = CATEGORY_TO_EVENT_TYPE.get(str(item.get("category") or "Other"), "OTHER")
    name = str(item["template_title"])
    schema = {
        "template_title": name,
        "category": item.get("category"),
        "file_format": "JPEG",
        "direct_download_url": item.get("direct_download_url"),
        "license_type": item.get("license_type"),
        "preview_image_url": "",
        "source": "Official template preview rendered to JPEG",
        "license": item.get("license_type"),
        "pageUrl": item.get("direct_download_url"),
    }
    curl = [
        "curl",
        "-sS",
        "-A",
        USER_AGENT,
        "-w",
        "\nHTTP_STATUS:%{http_code}",
        "-X",
        "POST",
        base + "/api/v1/templates",
        "-H",
        f"Authorization: Bearer {token}",
        "-F",
        f"templateName={name}",
        "-F",
        f"eventType={event_type}",
        "-F",
        f"eventId={LIBRARY_EVENT_ID}",
        "-F",
        f"content={json.dumps(schema, ensure_ascii=False)}",
        "-F",
        f"file=@{jpeg_path};type=image/jpeg;filename={jpeg_path.name}",
    ]
    raw = subprocess.check_output(curl, timeout=120)
    text = raw.decode("utf-8", errors="replace")
    body, _, status = text.rpartition("\nHTTP_STATUS:")
    code = int(status.strip() or "0")
    try:
        return code, json.loads(body)
    except Exception:
        return code, body


def seed_fields(base: str, token: str, template_id: str) -> None:
    configs = [{**field, "templateId": template_id} for field in DEFAULT_FIELDS]
    code, payload = api_json(base, "POST", f"/api/v1/templates/{template_id}/fields", token=token, body=configs)
    if code not in (200, 201):
        log(f"  field layout skipped ({code}): {payload}")


def main() -> int:
    base = "http://127.0.0.1:8080"
    email = "seeder@inviteflow.local"
    password = "secret12"
    rows = json.loads(RESEARCH.read_text(encoding="utf-8"))
    delete_previous_research_rows()
    token = sign_in(base, email, password)
    created = 0
    for item in rows:
        jpeg_name = str(item.get("jpeg_file") or "")
        jpeg_path = CACHE / jpeg_name
        if not jpeg_path.exists() or jpeg_path.stat().st_size < 1024:
            log(f"skip missing jpeg: {jpeg_name}")
            continue
        if jpeg_path.suffix.lower() not in {".jpg", ".jpeg"}:
            log(f"skip non-jpeg: {jpeg_path}")
            continue
        code, payload = upload_jpeg(base, token, item, jpeg_path)
        if code not in (200, 201) or not isinstance(payload, dict) or not payload.get("id"):
            log(f"upload failed ({code}): {item.get('template_title')} → {payload}")
            continue
        template_id = str(payload["id"])
        mime = str(payload.get("mimeType") or "")
        if "jpeg" not in mime and "jpg" not in mime:
            log(f"  warning: stored mime is {mime}")
        seed_fields(base, token, template_id)
        created += 1
        log(f"seeded JPEG {item['template_title']} → {template_id} ({payload.get('mimeType')} {payload.get('fileSize')} bytes)")
    log(f"\nSeeded {created} JPEG templates.")
    return 0 if created else 1


if __name__ == "__main__":
    sys.exit(main())
