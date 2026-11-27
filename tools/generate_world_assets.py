#!/usr/bin/env python3
"""Generate high-resolution, original, offline game-world textures."""
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw
import math

OUT = Path("app/src/main/assets/worlds")
OUT.mkdir(parents=True, exist_ok=True)

def noise(rng, h, w, scale=11.0):
    return rng.normal(0.0, scale, (h, w, 1))

def save_rgb(path, channels):
    arr = np.concatenate(channels, axis=2).clip(0, 255).astype(np.uint8)
    Image.fromarray(arr, "RGB").save(path, format="PNG", compress_level=1, optimize=False)

def make_ground(name, seed, size=2048):
    rng = np.random.default_rng(seed)
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    n = noise(rng, size, size, 13.0)
    fine = noise(rng, size, size, 6.0)
    wave = (np.sin(x / 23.0) + np.sin(y / 31.0) + np.sin((x+y) / 47.0)) * 2.6
    if name == "city":
        aggregate = rng.normal(0, 8, (size, size, 1))
        base = 72 + wave[..., None] + n + aggregate + fine
        # Fine aggregate and occasional hairline road wear.
        cracks = ((np.sin((x + 0.22*y) / 61.0) > .997) | (np.sin((y - .13*x) / 83.0) > .999))
        base -= cracks[..., None] * 34
        save_rgb(OUT / "city_ground.png", [base*.98, base, base*1.04])
    elif name == "stadium":
        mowing = np.sin(x / 92.0) * 8.0
        blades = rng.normal(0, 12, (size, size, 1))
        base = 82 + mowing[..., None] + wave[..., None] + n + blades + fine
        save_rgb(OUT / "football_ground.png", [base*.47, base*1.32, base*.52])
    elif name == "forest":
        patches = (np.sin(x/79.0) * np.cos(y/67.0) + np.sin((x-y)/113.0)) * 10
        pebbles = rng.normal(0, 15, (size, size, 1))
        base = 91 + patches[..., None] + n + pebbles + fine
        save_rgb(OUT / "forest_ground.png", [base*1.28, base*.91, base*.56])
    elif name == "flight":
        cloud = (np.sin(x/119.0) + np.cos(y/83.0) + np.sin((x+y)/151.0)) * 15
        base = 172 + cloud[..., None] + n + fine
        save_rgb(OUT / "flight_ground.png", [base*.77, base*.91, base*1.04])
    else:
        bands = (np.sin(x/103.0 + np.sin(y/121.0)) + np.cos(y/91.0)) * 19
        stars = rng.random((size, size, 1)) > .997
        base = 42 + bands[..., None] + n + fine
        save_rgb(OUT / "space_ground.png", [base*1.40 + stars[...,0,None]*155, base*.60, base*1.72 + stars[...,0,None]*190])

def make_sky(name, seed, w=1280, h=640):
    rng = np.random.default_rng(seed)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    t = (y / max(1, h-1))[..., None]
    grain = rng.normal(0, 4.2, (h, w, 1))
    cloud = (np.sin(x/71.0 + y/47.0) + np.cos(x/113.0-y/59.0) + np.sin((x+y)/137.0)) / 3.0
    if name == "city":
        top = np.array([13, 35, 70], np.float32)
        bottom = np.array([245, 130, 72], np.float32)
        arr = top*(1-t) + bottom*t + grain
        arr += (np.maximum(0, cloud)[...,None] * 22)
        img = Image.fromarray(arr.clip(0,255).astype("uint8"), "RGB")
        d = ImageDraw.Draw(img)
        for bx in range(0, w, 37):
            bh = int(35 + ((bx*17 + seed*13) % 100))
            d.rectangle((bx, h-bh, bx+25, h), fill=(17, 27, 43))
            for wy in range(h-bh+8, h-6, 13):
                d.line((bx+5, wy, bx+8, wy), fill=(205, 157, 83), width=2)
        img.save(OUT / "city_sky.png", "PNG", compress_level=1)
        return
    elif name == "football":
        top=np.array([38,116,190],np.float32); bottom=np.array([172,218,245],np.float32)
        arr=top*(1-t)+bottom*t+grain+np.maximum(0,cloud)[...,None]*17
    elif name == "forest":
        top=np.array([22,44,70],np.float32); bottom=np.array([232,133,82],np.float32)
        arr=top*(1-t)+bottom*t+grain+np.maximum(0,cloud)[...,None]*16
    elif name == "flight":
        top=np.array([32,101,183],np.float32); bottom=np.array([183,222,245],np.float32)
        arr=top*(1-t)+bottom*t+grain+np.maximum(0,cloud)[...,None]*33
    else:
        top=np.array([4,7,25],np.float32); bottom=np.array([49,17,83],np.float32)
        neb=(np.sin(x/97.0)+np.cos(y/61.0)+np.sin((x-y)/127.0))[...,None]
        arr=top*(1-t)+bottom*t+grain
        arr += np.concatenate([np.maximum(0,neb)*28,np.maximum(0,neb)*9,np.maximum(0,neb)*43],axis=2)
        stars=rng.random((h,w))>.996
        arr += stars[...,None]*210
    Image.fromarray(arr.clip(0,255).astype("uint8"), "RGB").save(OUT / f"{name}_sky.png", "PNG", compress_level=1, optimize=False)

def make_detail(name, seed, size=2048):
    # A high-frequency surface micro-detail map, sampled subtly by the 3D material shader.
    rng = np.random.default_rng(seed)
    raw = rng.integers(0, 256, (size, size, 3), dtype=np.uint8)
    # Add soft, world-specific roughness variation without turning the map into a flat noise fill.
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    broad = ((np.sin(x/37.0 + seed) + np.cos(y/43.0) + np.sin((x-y)/59.0)) * 13.0)
    raw = np.clip(raw.astype(np.float32) * .72 + 34.0 + broad[..., None], 0, 255).astype(np.uint8)
    Image.fromarray(raw, "RGB").save(OUT / f"{name}_detail.png", "PNG", compress_level=1, optimize=False)

worlds = ["city", "football", "forest", "flight", "space"]
for i, world in enumerate(worlds, 1):
    make_ground(world, 700+i)
    make_sky(world, 1700+i)
    make_detail(world, 2700+i)

total = sum(p.stat().st_size for p in OUT.glob("*.png"))
print(f"Generated {len(list(OUT.glob('*.png')))} original world textures: {total:,} bytes ({total/1024/1024:.1f} MiB)")
if total < 50_000_000:
    raise SystemExit("World texture pack is below the requested 50 MB; refusing an undersized build.")
