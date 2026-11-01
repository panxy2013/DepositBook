#!/usr/bin/env python3
"""校验每个演示每一步都有中文 + 英文配音文件，防止打包漏配音。"""
import re
import os
import sys

KT = "app/src/main/java/com/pxy/depositbook/ui/screen/help/DemoPlayerScreen.kt"
VOICE = "app/src/main/assets/help/demo/voice"

s = open(KT, encoding="utf-8").read()
block = s[s.index("val DEMOS"):s.index("/** 教学页里的演示入口")]
parts = re.split(r'\n    "(\w+)" to DemoData\(', block)

missing = []
total = 0
for i in range(1, len(parts), 2):
    key, body = parts[i], parts[i + 1]
    n = len(re.findall(r"DemoStep\(", body))
    for idx in range(n):
        for suffix in ("", "_en"):
            total += 1
            f = os.path.join(VOICE, f"{key}_{idx}{suffix}.mp3")
            if not (os.path.exists(f) and os.path.getsize(f) > 2000):
                missing.append(f"{key}_{idx}{suffix}")

extra = [
    f for f in os.listdir(VOICE)
    if f.endswith(".mp3") and re.fullmatch(r"\w+_\d+(_en)?\.mp3", f) is None
]

print(f"checked {total} clips")
if missing:
    print("MISSING:", ", ".join(missing))
if extra:
    print("UNEXPECTED:", ", ".join(extra))
if missing or extra:
    sys.exit(1)
print("all good")
