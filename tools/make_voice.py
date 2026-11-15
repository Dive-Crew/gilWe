#!/usr/bin/env python3
"""
길 위 낭독 음성 파일 생성기 (macOS 전용).

Courses.kt 의 코스 소개와 이야기 원고(한/영)를 문장 단위로 나눠 macOS `say` 로 음성 파일을 만든다.
- 출력: app/src/main/assets/voice/{ko,en}/<item>_<n>.m4a  (AAC)
- 목록: app/src/main/assets/voice/manifest.json
  { "<itemId>": { "ko": [{"text": ..., "file": ..., "ms": ...}], "en": [...] } }
  itemId: 코스 소개는 "intro:<courseId>", 이야기는 이야기 id (예: "hs3")
앱은 문장 단위로 재생하므로, 내비 안내로 끊겼다가 돌아오면 그 문장 처음부터 다시 읽는다.

사용: /usr/bin/python3 -I tools/make_voice.py
원고(Courses.kt)를 고치면 다시 실행한다. 상용 버전에서는 감수 거친 성우/TTS 음성으로 같은 구조의 파일을 바꿔 넣으면 된다.
"""
import json
import os
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
COURSES = os.path.join(ROOT, "app/src/main/java/pleos/gilwe/data/Courses.kt")
EXTRAS = os.path.join(ROOT, "app/src/main/java/pleos/gilwe/data/Extras.kt")
OUT = os.path.join(ROOT, "app/src/main/assets/voice")
VOICES = {"ko": "Yuna", "en": "Samantha"}
RATE = {"ko": 175, "en": 170}

T = r'T\(\s*"([^"]*)",\s*"([^"]*)"\s*,?\s*\)'


def sentences(text: str):
    parts = re.split(r'(?<=[.!?。])\s+', text.strip())
    return [p.strip() for p in parts if p.strip()]


def items():
    src = open(COURSES, encoding="utf-8").read()
    out = {}
    # 코스 소개: id → introTitle → intro
    for m in re.finditer(r'id = "(\w+)",.*?introTitle = ' + T + r',\s*intro = ' + T, src, re.S):
        cid, t_en, t_ko, i_en, i_ko = m.groups()
        out[f"intro:{cid}"] = {"en": [t_en] + sentences(i_en), "ko": [t_ko] + sentences(i_ko)}
    # 이야기: Story("id", 제목, 부제, 원고, ...)
    for m in re.finditer(r'Story\("(\w+)",\s*' + T + r',\s*' + T + r',\s*' + T, src, re.S):
        sid, ti_en, ti_ko, _s_en, _s_ko, sc_en, sc_ko = m.groups()
        out[sid] = {"en": [ti_en] + sentences(sc_en), "ko": [ti_ko] + sentences(sc_ko)}
    # 고정 블록(마무리): 도착 전 "꼭 볼 3곳" — Extras.outro() 와 같은 문장
    ex = open(EXTRAS, encoding="utf-8").read()
    for m in re.finditer(r'"(\w+)" to listOf\(\s*\n((?:\s*WalkSpot\(.*\n)+)', ex):
        cid, body = m.groups()
        names = re.findall(r'WalkSpot\(' + T, body)
        if len(names) < 3:
            continue
        en = [n[0] for n in names[:3]]
        ko = [n[1] for n in names[:3]]
        out[f"outro:{cid}"] = {
            "en": [f"When you arrive, three places are worth seeing: {en[0]}, {en[1]} and {en[2]}.", "Explore them on foot after you park."],
            "ko": [f"도착하면 꼭 볼 세 곳이에요. {ko[0]}, {ko[1]}, {ko[2]}.", "주차한 뒤 걸어서 둘러보세요."],
        }
    return out


def duration_ms(path: str) -> int:
    info = subprocess.run(["afinfo", path], capture_output=True, text=True).stdout
    m = re.search(r"estimated duration:\s*([\d.]+)", info)
    return int(float(m.group(1)) * 1000) if m else 0


def main():
    data = items()
    if not data:
        sys.exit("원고를 찾지 못했어요: " + COURSES)
    manifest = {}
    jobs = []
    for item_id, langs in data.items():
        safe = item_id.replace(":", "_")
        manifest[item_id] = {}
        for lang, lines in langs.items():
            os.makedirs(os.path.join(OUT, lang), exist_ok=True)
            manifest[item_id][lang] = [None] * len(lines)
            for n, line in enumerate(lines):
                jobs.append((item_id, lang, n, line, f"{lang}/{safe}_{n}.m4a"))

    def run(job):
        item_id, lang, n, line, rel = job
        path = os.path.join(OUT, rel)
        subprocess.run(
            ["say", "-v", VOICES[lang], "-r", str(RATE[lang]), "-o", path,
             "--file-format=m4af", "--data-format=aac", line],
            check=True,
        )
        manifest[item_id][lang][n] = {"text": line, "file": rel, "ms": duration_ms(path)}

    # 문장마다 say 를 병렬로 돌린다 (순서대로 돌리면 수십 분 걸림)
    with ThreadPoolExecutor(max_workers=8) as ex:
        list(ex.map(run, jobs))
    for item_id in manifest:
        print(item_id, {k: len(v) for k, v in manifest[item_id].items()})
    with open(os.path.join(OUT, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=1)
    print("items:", len(manifest))


if __name__ == "__main__":
    main()
