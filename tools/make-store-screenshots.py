"""
Builds the captioned Play Store screenshots from the raw app captures.

    python tools/make-store-screenshots.py

Reads   store/screenshots/0N-*.png            (1080x2400, from StoreScreenshotTest)
Writes  store/screenshots/captioned/0N-*.png  (1080x1920, ready to upload)

Why 1080x1920 and not the raw 1080x2400: Play rejects a phone screenshot whose
long side is more than twice its short side, and 2400/1080 is 2.22. 9:16 is
also the shape Play recommends.

Why captions at all: most installs are decided on the first screenshot, without
scrolling, and burned-in captions are what actually get read (docs/16).

Each image is a caption on the app's dark brand colour, over the real screen
with the status bar and nav bar cropped off. The screen bleeds off the bottom
edge, which keeps the UI large enough to read on a listing thumbnail.

Copy rules (docs/04 §1): no exclamation marks, no emoji. The script refuses both.
"""

from PIL import Image, ImageDraw, ImageFont
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "store", "screenshots")
OUT = os.path.join(SRC, "captioned")

W, H = 1080, 1920

# Same palette as tools/make-feature-graphic.py, so the listing reads as one set.
BG_TOP = (11, 18, 32)
BG_BOTTOM = (21, 30, 50)
TEXT = (240, 244, 250)
MUTED = (165, 176, 194)
FRAME = (60, 72, 94)

# Rows of the 1080x2400 capture to keep. Measured, not guessed: the status bar
# icons end at row 48 and the nav bar starts below the Optimize button, whose
# bottom edge is row 2231. Re-measure if the capture device changes.
CROP_TOP = 72
CROP_BOTTOM = 2262

SHOT_WIDTH = 860
SHOT_TOP = 440
CORNER = 40

# Upload order, not filename order (docs/16): the first image decides most installs.
#
# One line each. A thumbnail shrinks the image to a few centimetres, and a
# second line with one word on it reads as a mistake.
SHOTS = [
    ("01-cut-plan.png",
     "Fewer bars. Less offcut.",
     "Saw blade width counted on every cut"),
    ("05-fractional-inches.png",
     'Type 1 5/16" straight in',
     "No converting to decimals"),
    ("02-parts-metric.png",
     "Metric or imperial, per job",
     "Each piece once, with how many you need"),
    ("03-stock.png",
     "Enter what you can buy",
     "6 m bars, 20 ft tube, 8 ft studs"),
    ("04-jobs.png",
     "Works with no signal",
     "No account. Jobs stay on your phone."),
]


def load_font(names, size):
    for name in names:
        for base in (r"C:\Windows\Fonts", "/usr/share/fonts/truetype/dejavu"):
            path = os.path.join(base, name)
            if os.path.exists(path):
                return ImageFont.truetype(path, size)
    sys.exit("No usable font found; install Segoe UI, Arial or DejaVu Sans.")


bold = lambda s: load_font(["segoeuib.ttf", "arialbd.ttf", "DejaVuSans-Bold.ttf"], s)
regular = lambda s: load_font(["segoeui.ttf", "arial.ttf", "DejaVuSans.ttf"], s)


def wrap(draw, text, font, max_width):
    """Greedy word wrap. Returns the lines."""
    lines, line = [], ""
    for word in text.split():
        trial = f"{line} {word}".strip()
        if draw.textlength(trial, font=font) <= max_width:
            line = trial
        else:
            lines.append(line)
            line = word
    lines.append(line)
    return lines


def centred(draw, lines, font, top, fill, line_height):
    for i, line in enumerate(lines):
        x = (W - draw.textlength(line, font=font)) / 2
        draw.text((x, top + i * line_height), line, font=font, fill=fill)
    return top + len(lines) * line_height


def build(filename, title, subtitle):
    for text in (title, subtitle):
        if "!" in text:
            sys.exit(f"{filename}: no exclamation marks in product copy")

    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    for y in range(H):
        t = y / H
        d.line([(0, y), (W, y)], fill=tuple(
            int(a + (b - a) * t) for a, b in zip(BG_TOP, BG_BOTTOM)))

    title_font, sub_font = bold(76), regular(40)
    title_lines = wrap(d, title, title_font, W - 120)
    sub_lines = wrap(d, subtitle, sub_font, W - 160)
    if len(title_lines) > 1 or len(sub_lines) > 1:
        sys.exit(f"{filename}: caption wraps to a second line; shorten it")

    y = centred(d, title_lines, title_font, 140, TEXT, 92)
    bottom = centred(d, sub_lines, sub_font, y + 22, MUTED, 54)
    if bottom > SHOT_TOP - 40:
        sys.exit(f"{filename}: caption runs into the screenshot")

    shot = Image.open(os.path.join(SRC, filename)).convert("RGB")
    if shot.size != (1080, 2400):
        sys.exit(f"{filename}: expected a 1080x2400 capture, got {shot.size}")
    shot = shot.crop((0, CROP_TOP, shot.width, CROP_BOTTOM))
    scale = SHOT_WIDTH / shot.width
    shot = shot.resize((SHOT_WIDTH, round(shot.height * scale)), Image.LANCZOS)

    x = (W - SHOT_WIDTH) // 2
    # Rounded top corners; the bottom bleeds off the canvas so it needs none.
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, shot.width - 1, shot.height + CORNER], radius=CORNER, fill=255)
    d.rounded_rectangle(
        [x - 3, SHOT_TOP - 3, x + SHOT_WIDTH + 2, H + CORNER],
        radius=CORNER + 3, fill=FRAME)
    img.paste(shot, (x, SHOT_TOP), mask)

    os.makedirs(OUT, exist_ok=True)
    # 24-bit PNG, no alpha: Play rejects screenshots with an alpha channel.
    img.save(os.path.join(OUT, filename), "PNG")
    return filename


if __name__ == "__main__":
    for order, (filename, title, subtitle) in enumerate(SHOTS, start=1):
        build(filename, title, subtitle)
        print(f"upload #{order}: captioned/{filename}  -  {title}")
