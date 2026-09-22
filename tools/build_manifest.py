#!/usr/bin/env python3
"""
Rebuilds app/src/main/assets/pdfs/ and app/src/main/assets/manifest.json
from a zip of "Surah_<num>_<Name>[_Part<N>][_Final].pdf" files.

Handles:
  - Surahs split across multiple "PartN" PDFs (kept in numeric order)
  - Duplicate/renamed files ("(1)" suffix) -> deduped by content
  - Competing files for the same surah+part (e.g. "PartN" and "PartN_Final"
    both present) -> the newer/"_Final" one wins
  - Single-file surahs named "..._Complete.pdf" or "..._Clean.pdf"

Usage:
    pip install pypdf
    python3 tools/build_manifest.py path/to/Quran_with_Tafseel.zip

Run this again any time you add, replace, or fill in missing surah PDFs --
it regenerates the whole assets folder from scratch, so just re-zip your
full, updated set of files and point this script at it.
"""
import re
import sys
import zipfile
import hashlib
import json
import os
from collections import defaultdict

try:
    from pypdf import PdfReader
except ImportError:
    sys.exit("Missing dependency. Run: pip install pypdf")

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets")

FILENAME_RE = re.compile(
    r'^Surah_(\d{3})_(.+?)(?:_Part(\d+))?(_Final)?(?:\s*\((\d+)\))?\.pdf$',
    re.IGNORECASE,
)
COMPLETE_SUFFIX_RE = re.compile(r'_(Complete|Clean)$', re.IGNORECASE)


def main():
    if len(sys.argv) != 2:
        sys.exit(f"Usage: python3 {sys.argv[0]} <path-to-zip>")
    zip_path = sys.argv[1]

    zf = zipfile.ZipFile(zip_path)
    entries = []
    for info in zf.infolist():
        name = os.path.basename(info.filename)
        if not name.lower().endswith('.pdf'):
            continue
        m = FILENAME_RE.match(name)
        if not m:
            print(f"WARNING: could not parse filename, skipping: {name}")
            continue
        surah_num = int(m.group(1))
        surah_name_raw = m.group(2)
        part = m.group(3)
        is_final = bool(m.group(4))
        dup_idx = m.group(5)
        surah_name = COMPLETE_SUFFIX_RE.sub('', surah_name_raw)
        data = zf.read(info.filename)
        entries.append(dict(
            zip_path=info.filename, name=name, surah_num=surah_num,
            surah_name=surah_name, part_num=int(part) if part else 1,
            is_final=is_final, dup_idx=int(dup_idx) if dup_idx else 0,
            mtime=info.date_time, md5=hashlib.md5(data).hexdigest(),
        ))

    print(f"Parsed {len(entries)} PDF files from {zip_path}")

    groups = defaultdict(list)
    for e in entries:
        groups[(e['surah_num'], e['part_num'])].append(e)

    chosen = []
    for key, group in groups.items():
        seen_md5, uniq = {}, []
        for e in group:
            if e['md5'] in seen_md5:
                continue
            seen_md5[e['md5']] = e
            uniq.append(e)
        if len(uniq) == 1:
            chosen.append(uniq[0])
            continue
        finals = [e for e in uniq if e['is_final']]
        pool = finals if finals else uniq
        winner = sorted(pool, key=lambda e: e['mtime'], reverse=True)[0]
        chosen.append(winner)
        print(f"  Conflict for Surah {key[0]} Part {key[1]}: "
              f"{[e['name'] for e in uniq]} -> kept {winner['name']}")

    chosen.sort(key=lambda e: (e['surah_num'], e['part_num']))

    pdfs_out = os.path.join(ASSETS_DIR, "pdfs")
    if os.path.isdir(pdfs_out):
        for f in os.listdir(pdfs_out):
            os.remove(os.path.join(pdfs_out, f))
    os.makedirs(pdfs_out, exist_ok=True)

    manifest, total_pages = [], 0
    for e in chosen:
        data = zf.read(e['zip_path'])
        safe_name = f"{e['surah_num']:03d}_{e['part_num']:02d}_{e['surah_name']}.pdf"
        outpath = os.path.join(pdfs_out, safe_name)
        with open(outpath, 'wb') as f:
            f.write(data)
        try:
            pages = len(PdfReader(outpath).pages)
        except Exception as ex:
            print(f"ERROR reading {safe_name}: {ex}")
            pages = 0
        manifest.append(dict(
            surah_num=e['surah_num'],
            surah_name=e['surah_name'].replace('_', ' '),
            part_num=e['part_num'],
            file=safe_name,
            pages=pages,
            start_page_index=total_pages,
        ))
        total_pages += pages

    with open(os.path.join(ASSETS_DIR, "manifest.json"), "w") as f:
        json.dump(dict(total_pages=total_pages, files=manifest), f, indent=2)

    missing = sorted(set(range(1, 115)) - {e['surah_num'] for e in chosen})
    print(f"\nWrote {len(manifest)} files, {total_pages} total pages "
          f"to {ASSETS_DIR}")
    if missing:
        print(f"Still missing surah numbers: {missing}")
    else:
        print("All 114 surahs present.")


if __name__ == "__main__":
    main()
