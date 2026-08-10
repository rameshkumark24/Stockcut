"""
Generates the 1024x500 Play Store feature graphic.

Why a script and not an image file: the graphic is the CutPlanBar visual, and the
bar it draws is a REAL optimizer result (the same 6000 mm job as screenshot 1),
not a decorative approximation. Keeping it generated means the marketing artwork
cannot drift away from what the app actually produces.

    python tools/make-feature-graphic.py

Play requires exactly 1024x500 PNG or JPEG, under 15 MB, no alpha channel.
"""

from PIL import Image, ImageDraw, ImageFont
import os

W, H = 1024, 500

# Pulled from the app's dark theme so the artwork matches the product.
BG = (11, 18, 32)
BAR_A = (21, 62, 117)
BAR_B = (13, 38, 74)
OFFCUT = (58, 63, 72)
TEXT = (240, 244, 250)
MUTED = (150, 162, 180)
ACCENT = (94, 178, 122)


def load_font(names, size):
    for name in names:
        for base in (
            r"C:\Windows\Fonts",
            "/usr/share/fonts/truetype/dejavu",
        ):
            path = os.path.join(base, name)
            if os.path.exists(path):
                return ImageFont.truetype(path, size)
    return ImageFont.load_default()


bold = lambda s: load_font(["segoeuib.ttf", "arialbd.ttf", "DejaVuSans-Bold.ttf"], s)
regular = lambda s: load_font(["segoeui.ttf", "arial.ttf", "DejaVuSans.ttf"], s)

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

# Subtle vertical lift so the flat background does not read as a broken image.
for y in range(H):
    t = y / H
    d.line([(0, y), (W, y)], fill=(
        int(BG[0] + 10 * t), int(BG[1] + 12 * t), int(BG[2] + 18 * t)))

# ── The cut plan bar: a real 6000 mm bar, 2400 + 2400 + 1190, offcut 1 ──────
# Same numbers as screenshot 1, so the graphic and the listing agree.
STOCK = 6000
KERF = 3
pieces = [2400, 2400, 1190]

bar_x, bar_w, bar_h = 64, W - 128, 96
bar_y = 250

cursor = bar_x
for i, piece in enumerate(pieces):
    seg_w = bar_w * piece / STOCK
    colour = BAR_A if i % 2 == 0 else BAR_B
    d.rectangle([cursor, bar_y, cursor + seg_w, bar_y + bar_h], fill=colour)
    label = str(piece)
    f = bold(30)
    tw = d.textlength(label, font=f)
    d.text((cursor + seg_w / 2 - tw / 2, bar_y + bar_h / 2 - 20), label, font=f, fill=TEXT)
    cursor += seg_w
    if i < len(pieces) - 1:
        cursor += bar_w * KERF / STOCK

# Whatever is left is the offcut, drawn to scale rather than to taste.
if cursor < bar_x + bar_w:
    d.rectangle([cursor, bar_y, bar_x + bar_w, bar_y + bar_h], fill=OFFCUT)

# ── Type ───────────────────────────────────────────────────────────────────
d.text((64, 92), "StockCut", font=bold(76), fill=TEXT)
d.text((64, 182), "Cut list optimizer for metal and timber",
       font=regular(31), fill=MUTED)

# Not a repeat of the numbers already inside the bar — this line says what the
# bar IS, which the segments cannot: the stock length and the blade width that
# was subtracted between every cut.
d.text((64, bar_y + bar_h + 26), "6000 mm bar  ·  3 mm blade", font=regular(26), fill=MUTED)
tail = "2.0% waste"
f = bold(26)
d.text((W - 64 - d.textlength(tail, font=f), bar_y + bar_h + 26), tail, font=f, fill=ACCENT)

out = os.path.join(os.path.dirname(__file__), "..", "store", "feature-graphic.png")
img.save(os.path.abspath(out), "PNG")
print(f"wrote {os.path.abspath(out)}  {img.size[0]}x{img.size[1]}")
