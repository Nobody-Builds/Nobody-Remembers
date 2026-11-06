"""Builds every app logo asset from design-assets/logo-source.webp.

    python -I design-assets/build_logo.py        (needs Pillow, numpy, scipy)

Writes:
  design-assets/logo.png                         transparent master (also used by the README)
  app/src/main/res/drawable-nodpi/logo.png       in-app logo, light theme
  app/src/main/res/drawable-night-nodpi/logo.png dark theme: Paper sticker border, as the kit does for the builder
  app/src/main/res/mipmap-*/ic_launcher_foreground.png, ic_launcher_monochrome.png
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
PAPER = (245, 247, 251)
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def cut_out(path: Path) -> Image.Image:
    img = np.asarray(Image.open(path).convert("RGB")).astype(np.float64)
    bg = np.median(np.concatenate([img[:20].reshape(-1, 3), img[-20:].reshape(-1, 3)]), axis=0)
    d = np.abs(img - bg).max(axis=2)
    labels, _ = ndimage.label(d < 30)
    edge = np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))
    region = np.isin(labels, edge[edge > 0])
    # Background trapped inside the drawing (between the legs, by the neck) goes too.
    inner, n = ndimage.label((d < 8) & ~region)
    sizes = ndimage.sum(np.ones_like(d), inner, range(1, n + 1))
    region |= np.isin(inner, np.flatnonzero(sizes >= 30) + 1)
    # Unmix anti-aliased edges against the nearest solid colour so no light halo is left.
    _, (iy, ix) = ndimage.distance_transform_edt(region, return_indices=True)
    fg = img[iy, ix]
    diff = fg - bg
    a = ((img - bg) * diff).sum(axis=2) / np.maximum((diff * diff).sum(axis=2), 1e-6)
    alpha = np.where(region, np.clip(a, 0, 1), 1.0)
    alpha[region & (d < 6)] = 0
    rgb = np.where(region[..., None], fg, img)
    im = Image.fromarray(np.dstack([rgb, alpha * 255]).round().clip(0, 255).astype(np.uint8), "RGBA")
    return im.crop(im.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox())


def sticker(im: Image.Image, width: int) -> Image.Image:
    """The kit's dark-surface treatment: a Paper border around the whole drawing."""
    a = np.asarray(im.getchannel("A")) > 40
    grown = ndimage.binary_dilation(a, iterations=width)
    pad = width + 2
    out = Image.new("RGBA", (im.width + 2 * pad, im.height + 2 * pad))
    border = np.zeros((out.height, out.width), bool)
    border[pad:pad + im.height, pad:pad + im.width] = grown
    border = Image.fromarray(border.astype(np.uint8) * 255).filter(ImageFilter.GaussianBlur(1))
    out.paste(Image.new("RGBA", out.size, PAPER + (255,)), (0, 0), border)
    out.alpha_composite(im, (pad, pad))
    return out


def monochrome(im: Image.Image) -> Image.Image:
    """Outlines and the question mark only, for Android's themed icons."""
    px = np.asarray(im).astype(int)
    r, g, b, a = px[..., 0], px[..., 1], px[..., 2], px[..., 3]
    ink = (r + g + b < 150) & (a > 128)  # outlines only; the suit (~200) stays open
    question = (b > r + 60) & (b > 150) & (a > 128)
    mask = (ink | question).astype(np.uint8) * 255
    out = Image.new("RGBA", im.size, (255, 255, 255, 0))
    out.putalpha(Image.fromarray(mask))
    return out


def icon_layer(art: Image.Image, px: int, scale: float, lift: float) -> Image.Image:
    w = round(px * scale)
    h = round(art.height * w / art.width)
    layer = Image.new("RGBA", (px, px))
    small = art.resize((w, h), Image.LANCZOS)
    layer.alpha_composite(small, ((px - w) // 2, round((px - h) / 2 - lift * px)))
    return layer


def main():
    art = cut_out(ROOT / "design-assets/logo-source.webp")
    master = art.resize((1024, round(art.height * 1024 / art.width)), Image.LANCZOS)
    master.save(ROOT / "design-assets/logo.png", optimize=True)

    inapp = art.resize((640, round(art.height * 640 / art.width)), Image.LANCZOS)
    (RES / "drawable-nodpi").mkdir(exist_ok=True)
    (RES / "drawable-night-nodpi").mkdir(exist_ok=True)
    inapp.save(RES / "drawable-nodpi/logo.png", optimize=True)
    sticker(inapp, 7).save(RES / "drawable-night-nodpi/logo.png", optimize=True)

    # Adaptive icon: 108 dp canvas, launchers show the middle 72 dp and guarantee a 66 dp circle.
    # 0.6 keeps the hard hat and the question mark inside that circle; only the trail ends get trimmed.
    mono = monochrome(art)
    for name, k in DENSITIES.items():
        px = round(108 * k)
        out = RES / f"mipmap-{name}"
        out.mkdir(exist_ok=True)
        icon_layer(art, px, 0.6, 0.0).save(out / "ic_launcher_foreground.png", optimize=True)
        icon_layer(mono, px, 0.6, 0.0).save(out / "ic_launcher_monochrome.png", optimize=True)


if __name__ == "__main__":
    main()
