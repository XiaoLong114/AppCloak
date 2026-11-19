#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 AppCloak 的 mipmap 位图图标（不依赖 Pillow，纯标准库手写 PNG）。

用法：python tools/generate_icons.py
生成后覆盖 app/src/main/res/mipmap-*/ic_launcher*.png
"""
import os
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def in_cloak(x, y):
    """判断 108x108 视口坐标是否落在斗篷区域内。"""
    # 兜帽（上半部分椭圆）
    if ((x - 54.0) / 17.0) ** 2 + ((y - 45.0) / 16.0) ** 2 <= 1.0 and y <= 56.0:
        return True
    # 斗篷身体（向下张开的梯形）
    if 50.0 <= y <= 76.0:
        left = 37.0 - (y - 50.0) * 0.13
        right = 71.0 + (y - 50.0) * 0.13
        if left <= x <= right:
            return True
    return False


def in_eye(x, y):
    return (((x - 48.5) ** 2 + (y - 50.0) ** 2) <= 3.2 ** 2) or (
        ((x - 59.5) ** 2 + (y - 50.0) ** 2) <= 3.2 ** 2
    )


def png_chunk(tag, data):
    return (
        struct.pack(">I", len(data))
        + tag
        + data
        + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    )


def build_png(size, round_icon):
    raw = bytearray()
    for py in range(size):
        # PNG 每行前置 filter type 0
        raw.append(0)
        for px in range(size):
            if round_icon:
                cx = cy = size / 2.0
                inside = (px - cx) ** 2 + (py - cy) ** 2 <= (size / 2.0) ** 2
            else:
                rad = size * 0.22
                dx = max(rad - px, px - (size - 1 - rad), 0.0)
                dy = max(rad - py, py - (size - 1 - rad), 0.0)
                inside = dx * dx + dy * dy <= rad * rad

            if not inside:
                raw.extend((0, 0, 0, 0))
                continue

            t = (px + py) / (2.0 * max(1, size - 1))
            r = int(0x4A + (0x1B - 0x4A) * t)
            g = int(0x2E + (0x10 - 0x2E) * t)
            b = int(0x8F + (0x33 - 0x8F) * t)

            ux = px * 108.0 / size
            uy = py * 108.0 / size
            if in_cloak(ux, uy) and not in_eye(ux, uy):
                r = g = b = 0xFF

            raw.extend((r, g, b, 0xFF))

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", ihdr)
        + png_chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + png_chunk(b"IEND", b"")
    )


def main():
    for folder, size in DENSITIES.items():
        target = os.path.join(RES, folder)
        os.makedirs(target, exist_ok=True)
        for name, round_icon in (("ic_launcher.png", False), ("ic_launcher_round.png", True)):
            path = os.path.join(target, name)
            with open(path, "wb") as f:
                f.write(build_png(size, round_icon))
            print("generated", os.path.relpath(path, ROOT), f"{size}x{size}")


if __name__ == "__main__":
    main()
