"""
Renders the 512x512 Play Store icon from the app's own adaptive-icon sources.

Play requires a 512x512 PNG for the listing, and this app has NO raster icon at
all — the launcher icon is an adaptive icon defined purely in XML (a vector
foreground over a colour background). So there is nothing to "export"; the store
icon has to be rendered.

It is generated rather than drawn by hand so the store icon and the launcher
icon cannot drift apart. Someone changing the app icon and forgetting the store
asset is the normal way those two end up different.

    python tools/make-store-icon.py

The parser deliberately understands only the one path shape this icon uses
(`M x,y h W v H h -W z`, a closed axis-aligned rectangle) and RAISES on anything
else, rather than silently rendering a wrong icon if the artwork ever gets more
complicated. A loud failure is recoverable; a quietly wrong store icon is not.
That guard earned its keep immediately: the first version of this regex omitted
the closing `h -W` segment and matched nothing, which surfaced as an error
instead of an empty orange square.
"""

import os
import re
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw

ANDROID = "{http://schemas.android.com/apk/res/android}"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")

SIZE = 512  # Play's required store icon size.

# Render the adaptive icon's SAFE ZONE, not its full viewport.
#
# An adaptive icon is a 108x108 canvas of which only the central 72x72 is
# guaranteed visible — every launcher masks the rest away. So the full viewport
# is NOT what a user sees: rendered whole, the artwork occupies 18%-82% of the
# width, while on a real home screen it spans nearly the entire icon.
#
# Using the full viewport would make the store icon noticeably emptier than the
# icon the same user then finds on their phone, and emptier than every
# competitor's in the search results. Cropping to the safe zone makes the store
# listing and the launcher agree, which is the property that actually matters.
SAFE_ZONE = 72.0

# `M x,y h W v H h -W z` — the closed axis-aligned rectangle this icon uses.
# The trailing `h -W` is part of the shape; matching only `h W v H z` silently
# fails on every path in the file, which is what the first version of this
# script did.
RECT = re.compile(
    r"^M(-?[\d.]+),(-?[\d.]+)h(-?[\d.]+)v(-?[\d.]+)h(-?[\d.]+)z$", re.IGNORECASE
)


def parse_argb(value: str):
    """#RRGGBB or #AARRGGBB -> (r, g, b, a)."""
    v = value.lstrip("#")
    if len(v) == 6:
        r, g, b = (int(v[i:i + 2], 16) for i in (0, 2, 4))
        return (r, g, b, 255)
    if len(v) == 8:
        a, r, g, b = (int(v[i:i + 2], 16) for i in (0, 2, 4, 6))
        return (r, g, b, a)
    raise ValueError(f"unsupported colour {value!r}")


def background_colour() -> tuple:
    tree = ET.parse(os.path.join(RES, "values", "colors.xml"))
    for node in tree.getroot().iter("color"):
        if node.get("name") == "ic_launcher_background":
            return parse_argb(node.text.strip())
    raise SystemExit("ic_launcher_background not found in colors.xml")


def foreground_rects():
    """[(x, y, w, h, rgba)] in viewport units, plus the viewport size."""
    path = os.path.join(RES, "drawable", "ic_launcher_foreground.xml")
    root = ET.parse(path).getroot()
    vw = float(root.get(ANDROID + "viewportWidth"))
    vh = float(root.get(ANDROID + "viewportHeight"))

    rects = []
    for node in root.iter("path"):
        data = node.get(ANDROID + "pathData", "").replace(" ", "")
        match = RECT.match(data)
        if not match:
            raise SystemExit(
                "This renderer only understands axis-aligned rectangles, and the "
                f"icon now contains: {data!r}\n"
                "Refusing to guess — update this script, or export the store icon "
                "by hand, rather than shipping an icon that does not match the app."
            )
        x, y, w, h, back = (float(g) for g in match.groups())
        if abs(back + w) > 1e-6:
            raise SystemExit(
                f"{data!r} is not a closed rectangle: it opens {w} wide but "
                f"returns {back}. Refusing to guess at the intended shape."
            )
        rects.append((x, y, w, h, parse_argb(node.get(ANDROID + "fillColor"))))
    if not rects:
        raise SystemExit("no paths found in ic_launcher_foreground.xml")
    return rects, vw, vh


def main():
    bg = background_colour()
    rects, vw, vh = foreground_rects()

    scale = SIZE / SAFE_ZONE
    inset = (vw - SAFE_ZONE) / 2.0  # viewport units trimmed from each edge

    # Opaque background: Play shows the icon on varying surfaces and applies its
    # own rounding, so a transparent icon reads as a rendering fault.
    img = Image.new("RGBA", (SIZE, SIZE), bg)

    for x, y, w, h, rgba in rects:
        # Each piece is drawn on its own layer and alpha-composited, so the
        # semi-transparent offcut blends against the orange exactly as Android
        # composites it — rather than being approximated with a flat colour.
        layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        ImageDraw.Draw(layer).rectangle(
            [
                (x - inset) * scale,
                (y - inset) * scale,
                (x + w - inset) * scale,
                (y + h - inset) * scale,
            ],
            fill=rgba,
        )
        img = Image.alpha_composite(img, layer)

    out = os.path.join(ROOT, "store", "play-store-icon-512.png")
    img.convert("RGB").save(out, "PNG")
    print(f"wrote {out}  {img.size[0]}x{img.size[1]}  "
          f"({os.path.getsize(out) / 1024:.0f} KB, limit 1024 KB)")
    print(f"  viewport {vw:g}x{vh:g}, cropped to the {SAFE_ZONE:g}x{SAFE_ZONE:g} safe zone, "
          f"{len(rects)} shapes, background #{bg[0]:02X}{bg[1]:02X}{bg[2]:02X}")


if __name__ == "__main__":
    main()
