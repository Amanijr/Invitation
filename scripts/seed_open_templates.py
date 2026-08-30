#!/usr/bin/env python3
"""Seed InviteFlow with openly licensed invitation cards from the web.

Source: Wikimedia Commons and Openverse (CC0, Public Domain, CC BY, CC BY-SA).
Those licenses allow reuse and modification — required because the press
prints a guest name and QR onto the card.

This does not scrape Canva, Envato, Freepik, or other commercial shops,
and it does not generate artwork.

Usage:
  python3 scripts/seed_open_templates.py
  python3 scripts/seed_open_templates.py --dry-run
  python3 scripts/seed_open_templates.py --per-type 2 --email you@studio.com --password secret12
"""

from __future__ import annotations

import argparse
import json
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(__file__).resolve().parent / ".template-cache"
ATTRIBUTION = Path(__file__).resolve().parent / "open-template-attribution.json"
USER_AGENT = "InviteFlowSeeder/1.0 (open-license invitation templates; seeder@inviteflow.local)"
LIBRARY_EVENT_ID = "00000000-0000-0000-0000-000000000000"

ALLOWED_LICENSE_NEEDLES = (
    "public domain",
    "pdm",
    "cc0",
    "cc by ",
    "cc-by",
    "cc by-sa",
    "cc-by-sa",
    "cc by 4.0",
    "cc by-sa 4.0",
    "cc by 3.0",
    "cc by-sa 3.0",
)

BLOCKED_LICENSE_NEEDLES = ("nd", "no deriv", "nc", "noncommercial", "non-commercial")

SEARCH_QUERIES = {
    "WEDDING": ["wedding invitation card"],
    "SEND_OFF": ["bridal invitation card", "ladies invitation card"],
    "BIRTHDAY": ["birthday invitation card", "birthday card vintage"],
    "GRADUATION": ["graduation invitation card", "commencement invitation"],
    "CHURCH": ["church invitation card", "church service card"],
    "ANNIVERSARY": ["anniversary invitation card", "banquet invitation"],
    "CORPORATE": ["gala invitation card", "banquet invitation card"],
    "CONFERENCE": ["conference invitation", "art league meeting card"],
    "SEMINAR": ["seminar invitation"],
    "MEETING": ["meeting invitation card"],
    "PARTY": ["party invitation card"],
    "GALA": ["gala invitation card"],
    "EXPO": ["exhibition invitation card"],
    "FUNERAL": ["funeral memorial card", "memorial card"],
    "OTHER": ["invitation card ornamental"],
}

# Curated Commons files: designed invitation cards (scans/flats), not product photos.
# Prefer portrait stationery. Titles are Wikimedia file names without the "File:" prefix.
FALLBACK_FILES = {
    "WEDDING": [
        "A marriage festival invitation card .jpg",
        "Wedding invitation card.png",
        "Contoh undangan pernikahan.jpg",
        "Invitation card.jpg",
        "Weddin Invitation.jpg",
        "Wedding Invitation page 2.jpg",
        "Red bomb.jpg",
        "Anderzon - von Reis wedding invite 1876.jpg",
    ],
    "SEND_OFF": [
        "A Desk Book on the Etiquette of Social Stationery Invitation53.png",
        "A Desk Book on the Etiquette of Social Stationery Invitation54.png",
        "Bengali New Year invitation card.jpg",
        "Brides 0056.jpg",
    ],
    "BIRTHDAY": [
        "30th Birthday Card.jpg",
        "100th birthday card Fords.jpg",
        "Party invitation (2).jpg",
    ],
    "GRADUATION": [
        "Invitation card (Wikisource-guj).jpg",
        "Dayakar rao book.jpg",
        "Inaugural invitation 2009.jpg",
    ],
    "CHURCH": [
        "Invitation card from a temple in Erlin 01.jpg",
        "Santa Messa Crismale.jpg",
        "Museovirasto invitation card kutsukortti.jpg",
    ],
    "ANNIVERSARY": [
        "William Reece - banquet invitation centennial celebrations.jpg",
        "China TV 10th Anniversary Tea Party invitation and envelope.jpg",
        "Invitation card from age of 60th anniversary of FC Dorog.jpg",
    ],
    "CORPORATE": [
        "Aldo Samaritani Cav.d.Lav.-invito Quirinale-2 giugno 1967.png",
        "Inaugural invitation 2009.jpg",
        "Invitation to the Lord Mayor's banquet 1902 designed by George W. Eve.jpg",
    ],
    "CONFERENCE": [
        "1896-kilohane art league meeting card.jpg",
        "Invitation to 1909 concert in Istanbul.jpg",
        "Bengali Wikipedia 10th Anniversary Celebration, 2015 INVITATION CARD in English.jpg",
    ],
    "SEMINAR": [
        "Dénes Irén – Bibliotheca Officina kiállítási meghívó (1946).jpg",
        "Invitation card (Wikisource-guj).jpg",
    ],
    "MEETING": [
        "A Desk Book on the Etiquette of Social Stationery Invitation57.png",
        "Flament, uitnodiging stadhuis, RAL P-0458-001.jpg",
        "19130313-ClubAmisNature-NE.png",
    ],
    "PARTY": [
        "FRIDGE nightclub, Brixton, 1985 invitation card. (pink).jpg",
        "Party invitation (2).jpg",
        "Az első táncház meghívója 6.ker Liszt Ferenc tér 2 1972.jpg",
    ],
    "GALA": [
        "Invitation pour la cérémonie d'ouverture du Algiers International Film Festival (recto).jpg",
        "Inaugural invitation 2009.jpg",
        "Centurion Card Invitation Set.JPG",
    ],
    "EXPO": [
        "Kunst Museuum Miler Ramirez Expo.jpg",
        "Tony Pájaro exhibit Manhattan 1991.jpeg",
        "Dénes Irén – Ferencvárosi Pincetárlat kiállítási meghívó (1975).jpg",
        "Naga bazar invitation card.jpg",
    ],
    "FUNERAL": [
        "Anna Maria Fry memorial card.png",
        "A F Pirie funeral card.jpg",
        "Ford WWII Memorial Invitation 2006.jpg",
    ],
    "OTHER": [
        "Naga bazar invitation card.jpg",
        "Jaroslav Křišťan - pozvánka.jpg",
        "Invitation card.jpg",
    ],
}

SHORT_NAMES = {
    "A marriage festival invitation card .jpg": "Festival wedding card",
    "Wedding invitation card.png": "Letterpress wedding card",
    "Contoh undangan pernikahan.jpg": "Vintage wedding undangan",
    "Invitation card.jpg": "Ivory invitation card",
    "Weddin Invitation.jpg": "Folded wedding card",
    "Wedding Invitation page 2.jpg": "Wedding card inner leaf",
    "Red bomb.jpg": "Red wedding card",
    "Anderzon - von Reis wedding invite 1876.jpg": "1876 wedding card",
    "A Desk Book on the Etiquette of Social Stationery Invitation53.png": "Ladies’ sitting card",
    "A Desk Book on the Etiquette of Social Stationery Invitation54.png": "At-home card",
    "Bengali New Year invitation card.jpg": "New year sitting card",
    "Brides 0056.jpg": "Bridal card",
    "30th Birthday Card.jpg": "Thirtieth birthday card",
    "100th birthday card Fords.jpg": "Centenary birthday card",
    "Party invitation (2).jpg": "House party card",
    "Invitation card (Wikisource-guj).jpg": "Hall invitation card",
    "Dayakar rao book.jpg": "Launch invitation",
    "Inaugural invitation 2009.jpg": "Inaugural card",
    "Invitation card from a temple in Erlin 01.jpg": "Temple invitation",
    "Santa Messa Crismale.jpg": "Chrism mass card",
    "Museovirasto invitation card kutsukortti.jpg": "Museum service card",
    "William Reece - banquet invitation centennial celebrations.jpg": "Centennial banquet card",
    "China TV 10th Anniversary Tea Party invitation and envelope.jpg": "Anniversary tea card",
    "Invitation card from age of 60th anniversary of FC Dorog.jpg": "Sixtieth anniversary card",
    "Aldo Samaritani Cav.d.Lav.-invito Quirinale-2 giugno 1967.png": "Quirinale invitation",
    "Invitation to the Lord Mayor's banquet 1902 designed by George W. Eve.jpg": "Lord Mayor’s banquet card",
    "1896-kilohane art league meeting card.jpg": "Art league meeting card",
    "Invitation to 1909 concert in Istanbul.jpg": "Concert invitation",
    "Bengali Wikipedia 10th Anniversary Celebration, 2015 INVITATION CARD in English.jpg": "Conference invitation",
    "Dénes Irén – Bibliotheca Officina kiállítási meghívó (1946).jpg": "Seminar invitation",
    "A Desk Book on the Etiquette of Social Stationery Invitation57.png": "Meeting card",
    "Flament, uitnodiging stadhuis, RAL P-0458-001.jpg": "Town hall invitation",
    "19130313-ClubAmisNature-NE.png": "Club meeting card",
    "FRIDGE nightclub, Brixton, 1985 invitation card. (pink).jpg": "Pink party card",
    "Az első táncház meghívója 6.ker Liszt Ferenc tér 2 1972.jpg": "Dance-house card",
    "Invitation pour la cérémonie d'ouverture du Algiers International Film Festival (recto).jpg": "Festival opening card",
    "Centurion Card Invitation Set.JPG": "Gala invitation set",
    "Kunst Museuum Miler Ramirez Expo.jpg": "Expo invitation",
    "Tony Pájaro exhibit Manhattan 1991.jpeg": "Exhibition card",
    "Dénes Irén – Ferencvárosi Pincetárlat kiállítási meghívó (1975).jpg": "Gallery invitation",
    "Naga bazar invitation card.jpg": "Ornamental invitation",
    "Anna Maria Fry memorial card.png": "Memorial card",
    "A F Pirie funeral card.jpg": "Funeral card",
    "Ford WWII Memorial Invitation 2006.jpg": "Memorial invitation",
    "Jaroslav Křišťan - pozvánka.jpg": "Studio invitation",
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


def ssl_context() -> ssl.SSLContext:
    ctx = ssl.create_default_context()
    try:
        import certifi

        ctx.load_verify_locations(certifi.where())
    except Exception:
        pass
    return ctx


def http_get(url: str, timeout: int = 45) -> bytes:
    headers = {
        "User-Agent": USER_AGENT,
        "Accept": "application/json,image/*,*/*;q=0.8",
    }
    last_error: Exception | None = None
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=timeout, context=ssl_context()) as res:
                return res.read()
        except Exception as exc:
            last_error = exc
        try:
            raw = subprocess.check_output(
                [
                    "curl",
                    "-sSL",
                    "-A",
                    USER_AGENT,
                    "-H",
                    "Accept: application/json,image/*,*/*;q=0.8",
                    "--max-time",
                    str(timeout),
                    "-w",
                    "\nHTTP_STATUS:%{http_code}",
                    url,
                ],
                stderr=subprocess.DEVNULL,
            )
            text = raw.decode("latin-1")
            body, _, status = text.rpartition("\nHTTP_STATUS:")
            code = int(status.strip() or "0")
            if code == 200:
                return body.encode("latin-1")
            if code in (403, 429):
                time.sleep(2 ** attempt)
                continue
            last_error = RuntimeError(f"HTTP {code}")
        except Exception as exc:
            last_error = exc
            if "exit status 56" in str(exc) or "CERTIFICATE_VERIFY_FAILED" in str(exc):
                break
        time.sleep(1.2 * (attempt + 1))
    raise RuntimeError(f"GET failed for {url}: {last_error}")


def commons_api(params: dict) -> dict:
    url = "https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(params)
    raw = http_get(url)
    if raw.startswith(b"You are making too many requests"):
        raise RuntimeError("Wikimedia rate limit — wait a minute and retry")
    return json.loads(raw.decode("utf-8"))


def license_allowed(short_name: str | None) -> bool:
    if not short_name:
        return False
    text = short_name.lower().replace("_", " ")
    if "-nd" in text or "no deriv" in text:
        return False
    if "-nc" in text or "nc-" in text or "noncommercial" in text or "non-commercial" in text:
        return False
    return any(n in text for n in ALLOWED_LICENSE_NEEDLES)


def file_info(title: str) -> dict | None:
    if not title.startswith("File:"):
        title = f"File:{title}"
    data = commons_api(
        {
            "action": "query",
            "format": "json",
            "titles": title,
            "prop": "imageinfo",
            "iiprop": "url|size|mime|extmetadata",
        }
    )
    pages = (data.get("query") or {}).get("pages") or {}
    page = next(iter(pages.values()), None)
    if not page or page.get("missing") is not None:
        return None
    info = (page.get("imageinfo") or [None])[0]
    if not isinstance(info, dict):
        return None
    meta = info.get("extmetadata") or {}
    def meta_val(key: str) -> str:
        raw = meta.get(key)
        if isinstance(raw, dict):
            return str(raw.get("value") or "")
        return str(raw or "")

    license_name = meta_val("LicenseShortName")
    mime = str(info.get("mime") or "")
    width = int(info.get("width") or 0)
    height = int(info.get("height") or 0)
    url = str(info.get("url") or "")
    if not url or not mime.startswith("image/"):
        return None
    if mime not in ("image/jpeg", "image/png", "image/jpg"):
        return None
    if min(width, height) < 400:
        return None
    if not license_allowed(license_name):
        return None
    return {
        "title": page.get("title") or title,
        "url": url,
        "mime": mime,
        "width": width,
        "height": height,
        "license": license_name,
        "artist": meta_val("Artist"),
        "credit": meta_val("Credit"),
        "attribution": meta_val("Attribution"),
        "license_url": meta_val("LicenseUrl"),
        "description": meta_val("ImageDescription"),
        "page_url": f"https://commons.wikimedia.org/wiki/{urllib.parse.quote(page.get('title') or title)}",
    }


def search_commons(query: str, limit: int = 8) -> list[str]:
    data = commons_api(
        {
            "action": "query",
            "format": "json",
            "generator": "search",
            "gsrsearch": query,
            "gsrnamespace": "6",
            "gsrlimit": str(limit),
        }
    )
    pages = (data.get("query") or {}).get("pages") or {}
    titles = [p.get("title") for p in pages.values() if p.get("title")]
    return [t for t in titles if isinstance(t, str)]


OPENVERSE_OK = {"cc0", "pdm", "by", "by-sa"}


def search_openverse(query: str, limit: int = 8) -> list[dict]:
    params = {
        "q": query,
        "license": "cc0,pdm,by,by-sa",
        "extension": "jpg,png",
        "page_size": str(limit),
        "mature": "false",
    }
    url = "https://api.openverse.org/v1/images/?" + urllib.parse.urlencode(params)
    data = json.loads(http_get(url).decode("utf-8"))
    out: list[dict] = []
    for item in data.get("results") or []:
        if not isinstance(item, dict):
            continue
        license_key = str(item.get("license") or "").lower()
        if license_key not in OPENVERSE_OK:
            continue
        file_url = str(item.get("url") or "")
        if not file_url:
            continue
        width = int(item.get("width") or 0)
        height = int(item.get("height") or 0)
        if width and height and min(width, height) < 400:
            continue
        filetype = str(item.get("filetype") or "jpg").lower()
        mime = "image/png" if filetype == "png" else "image/jpeg"
        title = str(item.get("title") or "Invitation card")
        hay = f"{title} {item.get('foreign_landing_url') or ''}".lower()
        if not any(
            needle in hay
            for needle in ("invit", "inbjudan", "kutsukortti", "faire-part", "einladung")
        ):
            continue
        out.append(
            {
                "title": f"File:{title}",
                "url": file_url,
                "mime": mime,
                "width": width or 1200,
                "height": height or 1600,
                "license": f"CC {license_key.upper()} {item.get('license_version') or ''}".strip(),
                "artist": str(item.get("creator") or ""),
                "credit": str(item.get("source") or "Openverse"),
                "attribution": "",
                "license_url": str(item.get("license_url") or ""),
                "description": title,
                "page_url": str(item.get("foreign_landing_url") or item.get("detail_url") or file_url),
            }
        )
    return out


def card_rank(info: dict) -> tuple:
    width = int(info.get("width") or 0)
    height = int(info.get("height") or 0)
    portrait = 1 if height >= width else 0
    return (portrait, min(width, height), max(width, height))


def collect_infos(event_type: str, per_type: int) -> list[dict]:
    infos: list[dict] = []
    seen: set[str] = set()

    def keep(info: dict | None) -> None:
        if not info:
            return
        key = str(info.get("url") or "").lower()
        if not key or key in seen:
            return
        seen.add(key)
        infos.append(info)

    for name in FALLBACK_FILES.get(event_type, []):
        title = name if name.startswith("File:") else f"File:{name}"
        try:
            keep(file_info(title))
        except Exception as exc:
            log(f"  commons skip {title}: {exc}")
        time.sleep(0.35)

    if len(infos) < per_type:
        for query in SEARCH_QUERIES.get(event_type, []):
            try:
                for title in search_commons(query, limit=8):
                    keep(file_info(title))
                    time.sleep(0.5)
            except Exception as exc:
                log(f"  search skipped ({query}): {exc}")

    if len(infos) < per_type:
        for query in SEARCH_QUERIES.get(event_type, []):
            try:
                for item in search_openverse(query):
                    keep(item)
            except Exception as exc:
                log(f"  openverse skip ({query}): {exc}")
            time.sleep(0.4)

    infos.sort(key=card_rank, reverse=True)
    return infos[:per_type]


def download_file(info: dict, dest_dir: Path) -> Path:
    dest_dir.mkdir(parents=True, exist_ok=True)
    ext = ".png" if "png" in info["mime"] else ".jpg"
    slug = "".join(ch if ch.isalnum() else "-" for ch in info["title"])[:80].strip("-")
    path = dest_dir / f"{slug}{ext}"
    if not path.exists() or path.stat().st_size < 1024:
        data = http_get(info["url"])
        if len(data) < 1024:
            raise RuntimeError(f"download too small: {info['title']}")
        path.write_bytes(data)
    return path


def api_json(base: str, method: str, path: str, token: str | None = None, body: dict | None = None) -> tuple[int, object]:
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {"User-Agent": USER_AGENT, "Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60, context=ssl_context()) as res:
            raw = res.read()
            return res.status, json.loads(raw.decode("utf-8")) if raw else {}
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        try:
            parsed = json.loads(raw.decode("utf-8"))
        except Exception:
            parsed = raw.decode("utf-8", errors="replace")
        return exc.code, parsed


def api_multipart(base: str, path: str, token: str, fields: dict[str, str], file_path: Path, mime: str) -> tuple[int, object]:
    curl = [
        "curl",
        "-sS",
        "-A",
        USER_AGENT,
        "-w",
        "\nHTTP_STATUS:%{http_code}",
        "-X",
        "POST",
        base + path,
        "-H",
        f"Authorization: Bearer {token}",
    ]
    for key, value in fields.items():
        curl.extend(["-F", f"{key}={value}"])
    curl.extend(["-F", f"file=@{file_path};type={mime}"])
    raw = subprocess.check_output(curl, timeout=120)
    text = raw.decode("utf-8", errors="replace")
    status_line, _, body = text.rpartition("\nHTTP_STATUS:")
    status = int(body.strip() or "0")
    try:
        return status, json.loads(status_line)
    except Exception:
        return status, status_line


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


MYSQL_ENUM_FALLBACK = {
    "SEND_OFF": "WEDDING",
    "GRADUATION": "OTHER",
    "CHURCH": "OTHER",
    "ANNIVERSARY": "OTHER",
    "SEMINAR": "CONFERENCE",
    "MEETING": "CONFERENCE",
    "PARTY": "OTHER",
    "GALA": "CORPORATE",
    "EXPO": "OTHER",
}


def widen_event_type_columns() -> None:
    sql = (
        "ALTER TABLE events MODIFY event_type VARCHAR(32) NOT NULL; "
        "ALTER TABLE templates MODIFY event_type VARCHAR(32) NULL;"
    )
    try:
        subprocess.check_call(
            ["mysql", "-u", "root", "invitation_system", "-e", sql],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        log("Widened events.event_type and templates.event_type to VARCHAR(32).")
    except Exception:
        log("Could not widen event_type columns — new occasion types may map to older enum values.")


def existing_library(base: str) -> list[dict]:
    code, payload = api_json(base, "GET", "/api/v1/templates")
    if code != 200 or not isinstance(payload, list):
        return []
    return [
        item
        for item in payload
        if isinstance(item, dict) and str(item.get("eventId") or "") == LIBRARY_EVENT_ID
    ]


def replace_library(base: str, token: str) -> None:
    for item in existing_library(base):
        template_id = str(item.get("id") or "")
        if not template_id:
            continue
        code, payload = api_json(base, "DELETE", f"/api/v1/templates/{template_id}", token=token)
        log(f"  removed library card {item.get('templateName')} ({code})")


def existing_names(base: str) -> set[str]:
    code, payload = api_json(base, "GET", "/api/v1/templates")
    if code != 200 or not isinstance(payload, list):
        return set()
    return {str(item.get("templateName") or "") for item in payload}


def seed_fields(base: str, token: str, template_id: str) -> None:
    configs = [{**field, "templateId": template_id} for field in DEFAULT_FIELDS]
    code, payload = api_json(base, "POST", f"/api/v1/templates/{template_id}/fields", token=token, body=configs)
    if code not in (200, 201):
        log(f"  field layout skipped ({code}): {payload}")


def display_name(info: dict, event_type: str) -> str:
    raw_title = str(info["title"]).replace("File:", "")
    short = SHORT_NAMES.get(raw_title)
    if short:
        return short
    title = raw_title.rsplit(".", 1)[0].replace("_", " ").strip()
    if len(title) > 48:
        title = title[:45].rstrip() + "…"
    return f"{title} ({event_type.title().replace('_', ' ')})"


def main() -> int:
    parser = argparse.ArgumentParser(description="Seed openly licensed invitation cards into InviteFlow")
    parser.add_argument("--api", default="http://127.0.0.1:8080")
    parser.add_argument("--email", default="seeder@inviteflow.local")
    parser.add_argument("--password", default="secret12")
    parser.add_argument("--per-type", type=int, default=2)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument(
        "--replace-library",
        action="store_true",
        help="Remove previously seeded library cards (eventId all-zero) before posting new ones",
    )
    args = parser.parse_args()

    CACHE.mkdir(parents=True, exist_ok=True)
    event_types = list(SEARCH_QUERIES.keys())
    harvested: list[dict] = []

    log("Collecting openly licensed invitation cards from Wikimedia Commons and Openverse…")
    for event_type in event_types:
        log(f"\n{event_type}")
        for info in collect_infos(event_type, args.per_type):
            try:
                path = download_file(info, CACHE / event_type)
            except Exception as exc:
                log(f"  skip download {info.get('title')}: {exc}")
                continue
            record = {**info, "eventType": event_type, "localPath": str(path)}
            harvested.append(record)
            log(f"  kept {info['title']} [{info['license']}] {info['width']}×{info['height']}")

    ATTRIBUTION.write_text(json.dumps(harvested, indent=2), encoding="utf-8")
    log(f"\nWrote {len(harvested)} records to {ATTRIBUTION}")

    if args.dry_run:
        log("Dry run — not posting to the API.")
        return 0
    if not harvested:
        log("Nothing to seed.")
        return 1

    widen_event_type_columns()
    token = sign_in(args.api.rstrip("/"), args.email, args.password)
    if args.replace_library:
        log("Replacing previous library cards…")
        replace_library(args.api.rstrip("/"), token)
    known = existing_names(args.api.rstrip("/"))
    created = 0
    for record in harvested:
        name = display_name(record, record["eventType"])
        if name in known:
            log(f"already on desk: {name}")
            continue
        attribution = json.dumps(
            {
                "source": "Wikimedia Commons",
                "license": record.get("license"),
                "pageUrl": record.get("page_url"),
                "artist": record.get("artist"),
                "credit": record.get("credit"),
            },
            ensure_ascii=False,
        )
        event_type = record["eventType"]
        status, payload = api_multipart(
            args.api.rstrip("/"),
            "/api/v1/templates",
            token,
            {
                "templateName": name,
                "eventType": event_type,
                "eventId": LIBRARY_EVENT_ID,
                "content": attribution,
            },
            Path(record["localPath"]),
            record["mime"],
        )
        if status not in (200, 201) and event_type in MYSQL_ENUM_FALLBACK:
            mapped = MYSQL_ENUM_FALLBACK[event_type]
            log(f"  retry {name} as {mapped} (column still an old enum)")
            status, payload = api_multipart(
                args.api.rstrip("/"),
                "/api/v1/templates",
                token,
                {
                    "templateName": name,
                    "eventType": mapped,
                    "eventId": LIBRARY_EVENT_ID,
                    "content": attribution,
                },
                Path(record["localPath"]),
                record["mime"],
            )
            event_type = mapped
        if status not in (200, 201) or not isinstance(payload, dict) or not payload.get("id"):
            log(f"upload failed ({status}): {name} → {payload}")
            continue
        template_id = str(payload["id"])
        seed_fields(args.api.rstrip("/"), token, template_id)
        created += 1
        log(f"seeded {name} → {template_id}")

    log(f"\nSeeded {created} press templates.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
