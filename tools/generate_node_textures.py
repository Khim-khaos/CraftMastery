import math
import os
import struct
import zlib

WIDTH = HEIGHT = 64
CENTER = (WIDTH - 1) / 2.0
RADIUS = 28.0
EDGE_WIDTH = 2.5

OUTPUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(__file__)),
    "src",
    "main",
    "resources",
    "assets",
    "craftmastery",
    "textures",
    "gui",
)
os.makedirs(OUTPUT_DIR, exist_ok=True)


def lerp(a, b, t):
    return int(a + (b - a) * t)


def blend_color(inner, outer, t):
    return (
        lerp(inner[0], outer[0], t),
        lerp(inner[1], outer[1], t),
        lerp(inner[2], outer[2], t),
    )


def write_png(path, pixels):
    def chunk(chunk_type, data):
        return (
            struct.pack(">I", len(data))
            + chunk_type
            + data
            + struct.pack(">I", zlib.crc32(chunk_type + data) & 0xFFFFFFFF)
        )

    ihdr = struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 6, 0, 0, 0)
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        raw.extend(row)
    compressed = zlib.compress(bytes(raw), 9)

    png = bytearray()
    png.extend(b"\x89PNG\r\n\x1a\n")
    png.extend(chunk(b"IHDR", ihdr))
    png.extend(chunk(b"IDAT", compressed))
    png.extend(chunk(b"IEND", b""))

    with open(path, "wb") as fh:
        fh.write(png)


def generate_node(path, inner_color, outer_color, edge_color, highlight_color, highlight_strength):
    pixels = []
    for y in range(HEIGHT):
        row = bytearray()
        for x in range(WIDTH):
            dx = x - CENTER
            dy = y - CENTER
            distance = math.hypot(dx, dy)
            if distance > RADIUS:
                row.extend((0, 0, 0, 0))
                continue

            t = min(max(distance / RADIUS, 0.0), 1.0)
            r, g, b = blend_color(inner_color, outer_color, t)

            if RADIUS - EDGE_WIDTH <= distance <= RADIUS:
                r, g, b = edge_color

            hx = (x - 18) / RADIUS
            hy = (y - 18) / RADIUS
            highlight = max(0.0, 1.0 - (hx * hx + hy * hy)) * highlight_strength
            if highlight > 0:
                r = min(255, int(r + highlight * (highlight_color[0] - r)))
                g = min(255, int(g + highlight * (highlight_color[1] - g)))
                b = min(255, int(b + highlight * (highlight_color[2] - b)))

            row.extend((int(r), int(g), int(b), 255))
        pixels.append(row)

    write_png(path, pixels)


if __name__ == "__main__":
    studied_path = os.path.join(OUTPUT_DIR, "node_studied.png")
    if not os.path.exists(studied_path):
        generate_node(
            studied_path,
            inner_color=(102, 187, 106),  # #66BB6A
            outer_color=(46, 125, 50),  # #2E7D32
            edge_color=(200, 230, 201),  # #C8E6C9
            highlight_color=(236, 255, 241),
            highlight_strength=0.45,
        )

    unlocked_path = os.path.join(OUTPUT_DIR, "node_unlocked.png")
    if not os.path.exists(unlocked_path):
        generate_node(
            unlocked_path,
            inner_color=(255, 213, 79),  # #FFD54F
            outer_color=(255, 152, 0),  # #FF9800
            edge_color=(255, 224, 178),  # #FFE0B2
            highlight_color=(255, 249, 230),
            highlight_strength=0.35,
        )
