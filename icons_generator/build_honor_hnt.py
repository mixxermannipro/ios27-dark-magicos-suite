#!/usr/bin/env python3
"""Packages iOS 27 Dark icons, system masks, description.xml, and previews
into an installable Honor MagicOS `.hnt` (and `.hwt`) theme archive.
"""

import io
import json
import shutil
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

import cairo
from PIL import Image

from icon_symbols import draw_text_centered, hex_rgba, linear_grad, rounded_rect


def generate_description_xml(theme_meta: dict) -> str:
    title = escape(theme_meta.get("name", "iOS 27 Dark"))
    author = escape(theme_meta.get("author", "iOS 27 Dark Suite"))
    version = escape(theme_meta.get("version", "1.0.0"))
    return f"""<?xml version="1.0" encoding="utf-8"?>
<HwTheme>
    <title>{title}</title>
    <title-cn>iOS 27 深色主题</title-cn>
    <author>{author}</author>
    <designer>{author}</designer>
    <screen>FHD+</screen>
    <version>{version}</version>
    <font>Default</font>
    <font-cn>默认</font-cn>
    <briefinfo>iOS 27 Dark Suite: Originale App-Symbole auf tiefdunklem Squircle-Hintergrund (#18181B bis #09090B) mit 1px Glas-Lichtrand und Auto-Masking für alle Apps.</briefinfo>
</HwTheme>
"""


def build_icons_archive_bytes(apps: list, build_192_dir: Path) -> bytes:
    """Create an uncompressed ZIP archive named `icons` for Honor MagicOS Theme Manager."""
    buf = io.BytesIO()
    icons_dir = build_192_dir / "icons"
    masks_dir = build_192_dir / "masks"

    with zipfile.ZipFile(buf, mode="w", compression=zipfile.ZIP_STORED) as zf:
        # Mandatory system masks (MagicOS DST_IN convention)
        for mask_name in ("icon_back.png", "icon_mask.png", "icon_upon.png"):
            zf.write(masks_dir / mask_name, arcname=mask_name)

        written = set()
        for app in apps:
            primary_pkg = app["package"]
            src_png = icons_dir / f"{primary_pkg}.png"
            for pkg in [primary_pkg] + app.get("aliases", []):
                arc = f"{pkg}.png"
                if arc not in written:
                    zf.write(src_png, arcname=arc)
                    written.add(arc)

    return buf.getvalue()


def render_wallpaper_png(width: int, height: int, out_path: Path) -> None:
    """Render a sleek dark iOS 27 abstract mesh wallpaper programmatically."""
    surface = cairo.ImageSurface(cairo.FORMAT_ARGB32, width, height)
    ctx = cairo.Context(surface)

    linear_grad(ctx, 0, 0, width, height, [
        (0.0, "#050507"),
        (0.5, "#0B0D14"),
        (1.0, "#060608"),
    ])
    ctx.paint()

    # Subtle ambient dark-blue / violet / emerald depth orbs
    orbs = [
        (width * 0.25, height * 0.28, width * 0.55, "#0A84FF", 0.16),
        (width * 0.78, height * 0.52, width * 0.50, "#5E5CE6", 0.14),
        (width * 0.42, height * 0.78, width * 0.52, "#30D158", 0.08),
    ]
    for ox, oy, rad, col, alpha in orbs:
        pat = cairo.RadialGradient(ox, oy, 0, ox, oy, rad)
        pat.add_color_stop_rgba(0.0, *hex_rgba(col, alpha))
        pat.add_color_stop_rgba(1.0, *hex_rgba(col, 0.0))
        ctx.set_source(pat)
        ctx.paint()

    out_path.parent.mkdir(parents=True, exist_ok=True)
    surface.write_to_png(str(out_path))


def render_preview_grid(title: str, subtitle: str, apps_slice: list,
                        icons_dir: Path, out_jpg: Path,
                        width: int = 1080, height: int = 2160) -> None:
    """Render a high-res Honor Theme preview screen showing a 4x5 grid of iOS 27 Dark icons."""
    surface = cairo.ImageSurface(cairo.FORMAT_ARGB32, width, height)
    ctx = cairo.Context(surface)

    # Dark wallpaper backdrop
    linear_grad(ctx, 0, 0, 0, height, [(0.0, "#070709"), (0.5, "#0D0E14"), (1.0, "#050507")])
    ctx.paint()

    pat = cairo.RadialGradient(width * 0.5, height * 0.22, 20, width * 0.5, height * 0.22, width * 0.7)
    pat.add_color_stop_rgba(0.0, *hex_rgba("#0A84FF", 0.14))
    pat.add_color_stop_rgba(1.0, *hex_rgba("#0A84FF", 0.0))
    ctx.set_source(pat)
    ctx.paint()

    # Header card
    draw_text_centered(ctx, title, width / 2, 175, 58, "#FFFFFF")
    draw_text_centered(ctx, subtitle, width / 2, 245, 30, "#A1A1AA", weight=cairo.FONT_WEIGHT_NORMAL)

    # Mock iOS 27 dark frosted glass clock/weather widget banner at top
    rounded_rect(ctx, 76, 320, width - 152, 260, 48)
    ctx.set_source_rgba(22 / 255.0, 22 / 255.0, 24 / 255.0, 0.92)
    ctx.fill_preserve()
    ctx.set_source_rgba(44 / 255.0, 44 / 255.0, 46 / 255.0, 1.0)
    ctx.set_line_width(3)
    ctx.stroke()
    draw_text_centered(ctx, "16:27", 250, 435, 92, "#FFFFFF")
    draw_text_centered(ctx, "MITTWOCH, 18. NOV", 250, 515, 24, "#FF3B30")
    draw_text_centered(ctx, "12°  Klar", 790, 435, 52, "#FFFFFF")
    draw_text_centered(ctx, "H: 15°  T: 6°", 790, 505, 26, "#AEAEB2", weight=cairo.FONT_WEIGHT_NORMAL)

    # 4 columns x 5 rows icon grid
    cols = 4
    icon_draw_size = 156
    x_start = 112
    x_step = (width - 224 - icon_draw_size) / (cols - 1)
    y_start = 635
    y_step = 248

    for idx, app in enumerate(apps_slice[:20]):
        r = idx // cols
        c = idx % cols
        x = x_start + c * x_step
        y = y_start + r * y_step
        png_path = icons_dir / f"{app['package']}.png"
        if png_path.exists():
            img_surf = cairo.ImageSurface.create_from_png(str(png_path))
            ctx.save()
            ctx.translate(x, y)
            scale = icon_draw_size / img_surf.get_width()
            ctx.scale(scale, scale)
            ctx.set_source_surface(img_surf, 0, 0)
            ctx.paint()
            ctx.restore()
        label = app["name"][:13]
        draw_text_centered(ctx, label, x + icon_draw_size / 2, y + icon_draw_size + 30,
                           23, "#E4E4E7", weight=cairo.FONT_WEIGHT_NORMAL)

    # Dock pill at bottom
    rounded_rect(ctx, 64, height - 236, width - 128, 182, 54)
    ctx.set_source_rgba(24 / 255.0, 24 / 255.0, 27 / 255.0, 0.88)
    ctx.fill_preserve()
    ctx.set_source_rgba(255 / 255.0, 255 / 255.0, 255 / 255.0, 0.08)
    ctx.set_line_width(2)
    ctx.stroke()

    dock_pkgs = ["com.hihonor.phone", "com.android.browser", "com.hihonor.mms", "com.hihonor.camera"]
    for idx, dpkg in enumerate(dock_pkgs):
        png_path = icons_dir / f"{dpkg}.png"
        if png_path.exists():
            img_surf = cairo.ImageSurface.create_from_png(str(png_path))
            dx = x_start + idx * x_step
            dy = height - 223
            ctx.save()
            ctx.translate(dx, dy)
            scale = icon_draw_size / img_surf.get_width()
            ctx.scale(scale, scale)
            ctx.set_source_surface(img_surf, 0, 0)
            ctx.paint()
            ctx.restore()

    # Convert Cairo ARGB surface -> JPEG via Pillow
    out_jpg.parent.mkdir(parents=True, exist_ok=True)
    raw = bytes(surface.get_data())
    pil_img = Image.frombuffer("RGBA", (width, height), raw, "raw", "BGRA", 0, 1).convert("RGB")
    pil_img.save(str(out_jpg), format="JPEG", quality=92)


def main() -> None:
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent
    build_dir = script_dir / "build"
    build_192 = build_dir / "192"
    build_432 = build_dir / "432"
    output_dir = repo_root / "output"
    output_dir.mkdir(parents=True, exist_ok=True)

    if not (build_192 / "icons").exists():
        raise SystemExit("Run build_icons.py first so icons exist under icons_generator/build/192/.")

    data = json.loads((script_dir / "apps.json").read_text(encoding="utf-8"))
    apps = data["apps"]
    theme_meta = data.get("theme", {})

    # 1. Generate description.xml & uncompressed `icons` archive
    description_xml = generate_description_xml(theme_meta)
    icons_archive = build_icons_archive_bytes(apps, build_192)

    # 2. Generate preview images from high-res 432x432 icons
    preview_dir = build_dir / "preview"
    icons_432 = build_432 / "icons"

    system_apps = [a for a in apps if a.get("category") == "system_honor"]
    intl_apps = [a for a in apps if a.get("category") == "international_de"]
    china_apps = [a for a in apps if a.get("category") == "china"]

    p0 = preview_dir / "preview_icons_0.jpg"
    p1 = preview_dir / "preview_icons_1.jpg"
    cover = preview_dir / "cover.jpg"
    unlock = preview_dir / "preview_unlock_0.jpg"
    small = preview_dir / "icon_small.jpg"

    render_preview_grid(
        "iOS 27 Dark",
        "Honor MagicOS · Originale Symbole · Dunkler Squircle",
        system_apps[:8] + intl_apps[:12],
        icons_432,
        p0,
    )
    render_preview_grid(
        "iOS 27 Dark",
        "Internationale, Deutsche & China-Apps",
        intl_apps[12:24] + china_apps[:8],
        icons_432,
        p1,
    )
    shutil.copy2(p0, cover)
    shutil.copy2(p0, unlock)

    with Image.open(cover) as im:
        im_small = im.resize((360, 720), Image.Resampling.LANCZOS)
        im_small.save(small, format="JPEG", quality=88)

    # Sync wallpaper & preview assets for iconpack-apk as well
    iconpack_nodpi = repo_root / "iconpack-apk" / "app" / "src" / "main" / "res" / "drawable-nodpi"
    if iconpack_nodpi.exists():
        render_wallpaper_png(1080, 2340, iconpack_nodpi / "wallpaper_ios27_dark.png")
        with Image.open(p0) as im:
            im.resize((540, 1080), Image.Resampling.LANCZOS).save(
                iconpack_nodpi / "preview_iconpack.png", format="PNG"
            )

    # 3. Pack final Honor .hnt and .hwt theme packages
    hnt_path = output_dir / "iOS27_Dark_MagicOS.hnt"
    hwt_path = output_dir / "iOS27_Dark_MagicOS.hwt"

    with zipfile.ZipFile(hnt_path, mode="w", compression=zipfile.ZIP_STORED) as zf:
        zf.writestr("description.xml", description_xml.encode("utf-8"))
        zf.writestr("icons", icons_archive)
        for pfile in sorted(preview_dir.iterdir()):
            zf.write(pfile, arcname=f"preview/{pfile.name}")

    shutil.copy2(hnt_path, hwt_path)

    with zipfile.ZipFile(io.BytesIO(icons_archive), "r") as izf:
        icon_count = len(izf.namelist())

    print(f"Created {hnt_path} ({hnt_path.stat().st_size // 1024} KB) with {icon_count} entries inside `icons` archive.")
    print(f"Created copy {hwt_path}.")


if __name__ == "__main__":
    main()
