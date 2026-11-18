#!/usr/bin/env python3
"""Programmatic iOS 27 Dark Icon & System Mask Renderer.

Generates 192x192 px and 432x432 px icons using exact Superellipse (n=5)
squircles, a deep dark gradient (#18181B -> #09090B), a 1px glass top/outer
rim (rgba(255,255,255,0.12) -> rgba(255,255,255,0.03)), and vibrant original
app symbols from `icon_symbols.py`.
"""

import argparse
import json
import math
import shutil
from pathlib import Path
from xml.sax.saxutils import escape

import cairo

from icon_symbols import SYMBOL_REGISTRY, hex_rgba

SIZES = (192, 432)
SQUIRCLE_N = 5.0
SQUIRCLE_RADIUS_RATIO = 0.468  # 93.6% of canvas, crisp anti-aliased edge


def build_squircle_path(ctx: cairo.Context, cx: float, cy: float, radius: float,
                        n: float = SQUIRCLE_N, steps: int = 360) -> None:
    """Trace an exact superellipse |x/r|^n + |y/r|^n = 1 path."""
    exp = 2.0 / n
    for i in range(steps):
        t = 2.0 * math.pi * i / steps
        ct = math.cos(t)
        st = math.sin(t)
        x = cx + radius * math.copysign(abs(ct) ** exp, ct)
        y = cy + radius * math.copysign(abs(st) ** exp, st)
        if i == 0:
            ctx.move_to(x, y)
        else:
            ctx.line_to(x, y)
    ctx.close_path()


def draw_dark_squircle_background(ctx: cairo.Context, size: int) -> None:
    cx = cy = size / 2.0
    radius = size * SQUIRCLE_RADIUS_RATIO
    rim_width = max(1.0, size / 192.0)

    ctx.save()
    build_squircle_path(ctx, cx, cy, radius)
    ctx.clip()

    # Deep dark vertical gradient (#18181B -> #09090B)
    bg_grad = cairo.LinearGradient(0, cy - radius, 0, cy + radius)
    bg_grad.add_color_stop_rgba(0.0, *hex_rgba("#18181B", 1.0))
    bg_grad.add_color_stop_rgba(1.0, *hex_rgba("#09090B", 1.0))
    ctx.set_source(bg_grad)
    ctx.paint()

    # Subtle top glass reflection sheen
    sheen = cairo.RadialGradient(cx, cy - radius * 0.78, radius * 0.05,
                                 cx, cy - radius * 0.45, radius * 0.95)
    sheen.add_color_stop_rgba(0.0, 1.0, 1.0, 1.0, 0.045)
    sheen.add_color_stop_rgba(1.0, 1.0, 1.0, 1.0, 0.0)
    ctx.set_source(sheen)
    ctx.paint()
    ctx.restore()

    # 1px glass light border (inset by half stroke width so it stays crisp inside)
    ctx.save()
    build_squircle_path(ctx, cx, cy, radius - rim_width * 0.5)
    rim_grad = cairo.LinearGradient(0, cy - radius, 0, cy + radius)
    rim_grad.add_color_stop_rgba(0.0, 1.0, 1.0, 1.0, 0.12)
    rim_grad.add_color_stop_rgba(0.45, 1.0, 1.0, 1.0, 0.06)
    rim_grad.add_color_stop_rgba(1.0, 1.0, 1.0, 1.0, 0.03)
    ctx.set_source(rim_grad)
    ctx.set_line_width(rim_width)
    ctx.stroke()
    ctx.restore()


def render_app_icon(symbol_key: str, size: int, out_path: Path) -> None:
    surface = cairo.ImageSurface(cairo.FORMAT_ARGB32, size, size)
    ctx = cairo.Context(surface)
    ctx.set_antialias(cairo.ANTIALIAS_BEST)

    draw_dark_squircle_background(ctx, size)

    # Render symbol in 0..100 coordinate space centered inside squircle
    ctx.save()
    build_squircle_path(ctx, size / 2.0, size / 2.0, size * SQUIRCLE_RADIUS_RATIO)
    ctx.clip()

    scale = size / 100.0
    ctx.scale(scale, scale)
    SYMBOL_REGISTRY[symbol_key](ctx)
    ctx.restore()

    out_path.parent.mkdir(parents=True, exist_ok=True)
    surface.write_to_png(str(out_path))


def render_system_masks(size: int, out_dir: Path) -> None:
    """Generate icon_back.png, icon_mask.png (MagicOS), icon_mask_launcher.png (ADW), and icon_upon.png."""
    out_dir.mkdir(parents=True, exist_ok=True)
    cx = cy = size / 2.0
    radius = size * SQUIRCLE_RADIUS_RATIO
    rim_width = max(1.0, size / 192.0)

    # 1. icon_back.png: deep dark squircle background
    back_surf = cairo.ImageSurface(cairo.FORMAT_ARGB32, size, size)
    back_ctx = cairo.Context(back_surf)
    back_ctx.set_antialias(cairo.ANTIALIAS_BEST)
    draw_dark_squircle_background(back_ctx, size)
    back_surf.write_to_png(str(out_dir / "icon_back.png"))

    # 2. icon_mask.png (MagicOS DST_IN convention: opaque white inside inset squircle, transparent outside)
    # Inset ratio (0.82) cuts away typical OEM square/bright app icon backgrounds while keeping the center symbol.
    mask_surf = cairo.ImageSurface(cairo.FORMAT_ARGB32, size, size)
    mask_ctx = cairo.Context(mask_surf)
    mask_ctx.set_antialias(cairo.ANTIALIAS_BEST)
    build_squircle_path(mask_ctx, cx, cy, radius * 0.82)
    mask_ctx.set_source_rgba(1.0, 1.0, 1.0, 1.0)
    mask_ctx.fill()
    mask_surf.write_to_png(str(out_dir / "icon_mask.png"))

    # 3. icon_mask_launcher.png (Nova/ADW DST_OUT convention: transparent inside inset squircle, opaque outside)
    adw_surf = cairo.ImageSurface(cairo.FORMAT_ARGB32, size, size)
    adw_ctx = cairo.Context(adw_surf)
    adw_ctx.set_antialias(cairo.ANTIALIAS_BEST)
    adw_ctx.set_source_rgba(0.0, 0.0, 0.0, 1.0)
    adw_ctx.paint()
    adw_ctx.set_operator(cairo.OPERATOR_CLEAR)
    build_squircle_path(adw_ctx, cx, cy, radius * 0.82)
    adw_ctx.fill()
    adw_surf.write_to_png(str(out_dir / "icon_mask_launcher.png"))

    # 4. icon_upon.png: subtle edge darkening vignette + 1px top/outer glass light rim
    upon_surf = cairo.ImageSurface(cairo.FORMAT_ARGB32, size, size)
    upon_ctx = cairo.Context(upon_surf)
    upon_ctx.set_antialias(cairo.ANTIALIAS_BEST)

    upon_ctx.save()
    build_squircle_path(upon_ctx, cx, cy, radius)
    upon_ctx.clip()
    vignette = cairo.RadialGradient(cx, cy, radius * 0.56, cx, cy, radius * 1.02)
    vignette.add_color_stop_rgba(0.0, 9 / 255.0, 9 / 255.0, 11 / 255.0, 0.0)
    vignette.add_color_stop_rgba(0.75, 9 / 255.0, 9 / 255.0, 11 / 255.0, 0.18)
    vignette.add_color_stop_rgba(1.0, 9 / 255.0, 9 / 255.0, 11 / 255.0, 0.52)
    upon_ctx.set_source(vignette)
    upon_ctx.paint()
    upon_ctx.restore()

    upon_ctx.save()
    build_squircle_path(upon_ctx, cx, cy, radius - rim_width * 0.5)
    rim_grad = cairo.LinearGradient(0, cy - radius, 0, cy + radius)
    rim_grad.add_color_stop_rgba(0.0, 1.0, 1.0, 1.0, 0.12)
    rim_grad.add_color_stop_rgba(0.45, 1.0, 1.0, 1.0, 0.06)
    rim_grad.add_color_stop_rgba(1.0, 1.0, 1.0, 1.0, 0.03)
    upon_ctx.set_source(rim_grad)
    upon_ctx.set_line_width(rim_width)
    upon_ctx.stroke()
    upon_ctx.restore()

    upon_surf.write_to_png(str(out_dir / "icon_upon.png"))


def to_drawable_name(package: str) -> str:
    return "ic_" + package.replace(".", "_").lower()


def sync_iconpack_module(apps: list, build_dir: Path, repo_root: Path) -> None:
    """Populate iconpack-apk drawables and launcher XML configs (appfilter, drawable, theme_resources)."""
    iconpack_res = repo_root / "iconpack-apk" / "app" / "src" / "main" / "res"
    drawable_dir = iconpack_res / "drawable-nodpi"
    drawable_xxxhdpi = iconpack_res / "drawable-xxxhdpi"
    xml_dir = iconpack_res / "xml"
    assets_dir = repo_root / "iconpack-apk" / "app" / "src" / "main" / "assets"

    for d in (drawable_dir, drawable_xxxhdpi, xml_dir, assets_dir):
        d.mkdir(parents=True, exist_ok=True)

    # Copy 192px icons into drawable-nodpi & 432px into drawable-xxxhdpi for crisp scaling
    src_192 = build_dir / "192"
    src_432 = build_dir / "432"

    # Masks (using DST_OUT mask for standard Android launchers)
    shutil.copy2(src_192 / "masks" / "icon_back.png", drawable_dir / "icon_back.png")
    shutil.copy2(src_192 / "masks" / "icon_mask_launcher.png", drawable_dir / "icon_mask.png")
    shutil.copy2(src_192 / "masks" / "icon_upon.png", drawable_dir / "icon_upon.png")

    appfilter_lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<resources>",
        '    <iconback img1="icon_back" />',
        '    <iconmask img1="icon_mask" />',
        '    <iconupon img1="icon_upon" />',
        '    <scale factor="0.78" />',
        "",
    ]
    drawable_lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<resources>",
        '    <version>1</version>',
    ]
    theme_res_lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<Theme version="1">',
        '    <Label value="iOS 27 Dark" />',
        '    <Wallpaper image="wallpaper_ios27_dark" />',
        '    <LockScreenWallpaper image="wallpaper_ios27_dark" />',
        '    <ThemePreview image="preview_iconpack" />',
        '    <DockMenuAppIcon selector="true" />',
    ]

    categories = {
        "system_honor": ("Honor & System", []),
        "international_de": ("International & Deutsch", []),
        "china": ("China Apps", []),
    }

    seen_drawables = set()
    for app in apps:
        pkg = app["package"]
        name = app["name"]
        cat = app.get("category", "international_de")
        drw = to_drawable_name(pkg)

        if drw not in seen_drawables:
            shutil.copy2(src_192 / "icons" / f"{pkg}.png", drawable_dir / f"{drw}.png")
            shutil.copy2(src_432 / "icons" / f"{pkg}.png", drawable_xxxhdpi / f"{drw}.png")
            seen_drawables.add(drw)
            categories.setdefault(cat, ("Sonstige", []))[1].append((drw, name))

        all_pkgs = [pkg] + app.get("aliases", [])
        for p in all_pkgs:
            appfilter_lines.append(
                f'    <item component="ComponentInfo{{{escape(p)}/}}" drawable="{drw}" />'
            )
            theme_res_lines.append(
                f'    <AppIcon name="{escape(p)}" image="{drw}" />'
            )

    appfilter_lines.append("</resources>")
    theme_res_lines.append("</Theme>")

    for _, (title, items) in categories.items():
        if not items:
            continue
        drawable_lines.append(f'    <category title="{escape(title)}" />')
        for drw, _ in items:
            drawable_lines.append(f'    <item drawable="{drw}" />')
    drawable_lines.append("</resources>")

    appfilter_xml = "\n".join(appfilter_lines) + "\n"
    drawable_xml = "\n".join(drawable_lines) + "\n"
    theme_res_xml = "\n".join(theme_res_lines) + "\n"

    for target_dir in (xml_dir, assets_dir):
        (target_dir / "appfilter.xml").write_text(appfilter_xml, encoding="utf-8")
        (target_dir / "drawable.xml").write_text(drawable_xml, encoding="utf-8")
        (target_dir / "theme_resources.xml").write_text(theme_res_xml, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Build iOS 27 Dark icons and masks.")
    parser.add_argument("--no-sync-iconpack", action="store_true",
                        help="Skip syncing generated PNGs/XMLs into iconpack-apk/")
    args = parser.parse_args()

    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent
    apps_path = script_dir / "apps.json"
    build_dir = script_dir / "build"

    data = json.loads(apps_path.read_text(encoding="utf-8"))
    apps = data["apps"]

    # Validate symbol keys
    missing = [a["icon"] for a in apps if a["icon"] not in SYMBOL_REGISTRY]
    if missing:
        raise SystemExit(f"Missing symbol renderers for: {sorted(set(missing))}")

    print(f"Rendering {len(apps)} apps + 3 system masks in resolutions {SIZES}...")

    for size in SIZES:
        icons_out = build_dir / str(size) / "icons"
        masks_out = build_dir / str(size) / "masks"
        render_system_masks(size, masks_out)
        for app in apps:
            pkg = app["package"]
            render_app_icon(app["icon"], size, icons_out / f"{pkg}.png")

    if not args.no_sync_iconpack:
        sync_iconpack_module(apps, build_dir, repo_root)
        print("Synced drawables and XML configs to iconpack-apk/.")

    print(f"Done! Rendered {len(apps)} primary icons at 192x192 & 432x432 px.")


if __name__ == "__main__":
    main()
