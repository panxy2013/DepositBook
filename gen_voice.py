#!/usr/bin/env python3
"""为每个演示的每一步生成内置配音（中文 + 英文）。

命名：assets/help/demo/voice/{demoKey}_{stepIndex}.mp3      （中文）
     assets/help/demo/voice/{demoKey}_{stepIndex}_en.mp3   （英文）

不能用图片名：多个演示步骤共用同一张截图，但讲解内容不同。
"""
import asyncio
import os
import re
import sys

import edge_tts

KT = "app/src/main/java/com/pxy/depositbook/ui/screen/help/DemoPlayerScreen.kt"
OUT = "app/src/main/assets/help/demo/voice"

VOICES = {
    "zh": ("zh-CN-XiaoxiaoNeural", "-5%"),
    "en": ("en-US-AriaNeural", "-5%"),
}

src = open(KT, encoding="utf-8").read()
block = src[src.index("val DEMOS"):src.index("/** 教学页里的演示入口")]
parts = re.split(r'\n    "(\w+)" to DemoData\(', block)

items = []  # (key, idx, img, title_zh, zh, en)
for i in range(1, len(parts), 2):
    key, body = parts[i], parts[i + 1]
    steps = re.findall(
        r'DemoStep\(\s*"(\w+)",\s*floatArrayOf\([^)]*\),\s*"([^"]*)",\s*"[^"]*",\s*"([^"]*)",\s*"([^"]*)"',
        body,
    )
    for idx, (img, title, zh, en) in enumerate(steps):
        items.append((key, idx, img, title, zh, en))


def clean(text: str, lang: str) -> str:
    t = text
    t = t.replace("「", "").replace("」", "")
    t = t.replace("✓", "对勾" if lang == "zh" else "check mark")
    t = t.replace("＋", "加" if lang == "zh" else "plus")
    t = t.replace("×", "叉" if lang == "zh" else "cross")
    t = t.replace("***", "星号" if lang == "zh" else "asterisks")
    t = t.replace("→", "到" if lang == "zh" else " to ")
    t = t.replace("＝", "等于" if lang == "zh" else " equals ")
    t = re.sub(r"\\s+", " ", t)
    t = re.sub(r"\s+", " ", t).strip()
    return t


async def gen(name: str, text: str, lang: str) -> bool:
    path = os.path.join(OUT, f"{name}.mp3")
    if os.path.exists(path) and os.path.getsize(path) > 2000:
        return True
    voice, rate = VOICES[lang]
    for attempt in range(4):
        try:
            await edge_tts.Communicate(text, voice, rate=rate).save(path)
            if os.path.getsize(path) > 2000:
                print(f"ok   {name}  {os.path.getsize(path) // 1024}KB")
                return True
        except Exception as e:  # noqa: BLE001
            print(f"retry {name} ({attempt + 1}): {e}")
            await asyncio.sleep(2)
    print(f"FAIL {name}")
    return False


async def main() -> int:
    os.makedirs(OUT, exist_ok=True)
    langs = sys.argv[1:] or ["zh", "en"]
    sem = asyncio.Semaphore(3)
    tasks = []
    for key, idx, img, title, zh, en in items:
        if "zh" in langs:
            tasks.append(gen(f"{key}_{idx}", clean(zh, "zh"), "zh"))
        if "en" in langs:
            tasks.append(gen(f"{key}_{idx}_en", clean(en, "en"), "en"))

    async def wrap(coro):
        async with sem:
            return await coro

    res = await asyncio.gather(*[wrap(t) for t in tasks])
    ok = sum(1 for r in res if r)
    print(f"done {ok}/{len(tasks)}")
    return 0 if ok == len(tasks) else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
