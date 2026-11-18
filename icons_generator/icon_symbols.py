"""Programmatic PyCairo vector symbol renderers (0..100 coordinate space).

All symbols are drawn from scratch using geometric primitives, gradients,
and paths — zero external or copyrighted graphics.
"""

import math
import cairo


def hex_rgba(hex_color: str, alpha: float = 1.0):
    h = hex_color.lstrip("#")
    r = int(h[0:2], 16) / 255.0
    g = int(h[2:4], 16) / 255.0
    b = int(h[4:6], 16) / 255.0
    return (r, g, b, alpha)


def set_hex(ctx: cairo.Context, hex_color: str, alpha: float = 1.0):
    ctx.set_source_rgba(*hex_rgba(hex_color, alpha))


def linear_grad(ctx: cairo.Context, x0, y0, x1, y1, stops):
    pat = cairo.LinearGradient(x0, y0, x1, y1)
    for offset, col in stops:
        if isinstance(col, tuple):
            pat.add_color_stop_rgba(offset, *col)
        else:
            pat.add_color_stop_rgba(offset, *hex_rgba(col))
    ctx.set_source(pat)


def radial_grad(ctx: cairo.Context, cx, cy, r0, r1, stops):
    pat = cairo.RadialGradient(cx, cy, r0, cx, cy, r1)
    for offset, col in stops:
        if isinstance(col, tuple):
            pat.add_color_stop_rgba(offset, *col)
        else:
            pat.add_color_stop_rgba(offset, *hex_rgba(col))
    ctx.set_source(pat)


def rounded_rect(ctx: cairo.Context, x, y, w, h, r):
    r = min(r, w / 2.0, h / 2.0)
    ctx.new_sub_path()
    ctx.arc(x + w - r, y + r, r, -math.pi / 2, 0)
    ctx.arc(x + w - r, y + h - r, r, 0, math.pi / 2)
    ctx.arc(x + r, y + h - r, r, math.pi / 2, math.pi)
    ctx.arc(x + r, y + r, r, math.pi, 3 * math.pi / 2)
    ctx.close_path()


def draw_text_centered(ctx: cairo.Context, text: str, cx: float, cy: float,
                       size: float, color="#FFFFFF", weight=cairo.FONT_WEIGHT_BOLD,
                       slant=cairo.FONT_SLANT_NORMAL, family="sans-serif"):
    ctx.save()
    ctx.select_font_face(family, slant, weight)
    ctx.set_font_size(size)
    ext = ctx.text_extents(text)
    x = cx - (ext.width / 2.0 + ext.x_bearing)
    y = cy - (ext.height / 2.0 + ext.y_bearing)
    set_hex(ctx, color) if isinstance(color, str) else ctx.set_source_rgba(*color)
    ctx.move_to(x, y)
    ctx.show_text(text)
    ctx.restore()


def draw_pill_badge(ctx: cairo.Context, bg_stops, symbol_fn):
    rounded_rect(ctx, 22, 22, 56, 56, 15)
    linear_grad(ctx, 22, 22, 78, 78, bg_stops)
    ctx.fill()
    symbol_fn(ctx)


# --- Individual Symbol Renderers (coordinate space 0..100) ---

def sym_camera(ctx: cairo.Context):
    # Sleek silver/dark metallic iOS camera body with multi-layered sapphire lens
    rounded_rect(ctx, 21, 31, 58, 42, 9)
    linear_grad(ctx, 21, 31, 21, 73, [(0.0, "#D1D5DB"), (1.0, "#6B7280")])
    ctx.fill()
    rounded_rect(ctx, 37, 25, 26, 9, 4)
    linear_grad(ctx, 37, 25, 37, 34, [(0.0, "#E5E7EB"), (1.0, "#9CA3AF")])
    ctx.fill()
    # Outer dark ring
    ctx.arc(50, 52, 15.5, 0, 2 * math.pi)
    set_hex(ctx, "#18181B")
    ctx.fill()
    # Lens glass
    ctx.arc(50, 52, 11.5, 0, 2 * math.pi)
    radial_grad(ctx, 47, 49, 1, 12, [(0.0, "#38BDF8"), (0.55, "#1E3A8A"), (1.0, "#09090B")])
    ctx.fill()
    # Specular glint
    ctx.arc(46, 48, 3.2, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF", 0.55)
    ctx.fill()
    # Flash dot
    ctx.arc(69, 38, 2.6, 0, 2 * math.pi)
    set_hex(ctx, "#FBBF24")
    ctx.fill()


def sym_photos(ctx: cairo.Context):
    # Classic 8-petal multicolored Apple Photos flower
    colors = [
        "#FF9F0A", "#FF3B30", "#FF2D55", "#AF52DE",
        "#5856D6", "#0A84FF", "#30D158", "#FFD60A"
    ]
    for i, col in enumerate(colors):
        angle = i * (2 * math.pi / 8) - math.pi / 2
        ctx.save()
        ctx.translate(50, 50)
        ctx.rotate(angle)
        rounded_rect(ctx, -6.5, -28, 13, 21, 6.5)
        set_hex(ctx, col, 0.92)
        ctx.fill()
        ctx.restore()
    ctx.arc(50, 50, 4.5, 0, 2 * math.pi)
    set_hex(ctx, "#18181B")
    ctx.fill()


def sym_contacts(ctx: cairo.Context):
    # Colored index tabs on right + silhouette card
    tabs = ["#0A84FF", "#30D158", "#FF9F0A", "#FF3B30"]
    for i, col in enumerate(tabs):
        rounded_rect(ctx, 68, 25 + i * 12.5, 9, 10.5, 2.5)
        set_hex(ctx, col)
        ctx.fill()
    rounded_rect(ctx, 23, 24, 48, 52, 7)
    linear_grad(ctx, 23, 24, 23, 76, [(0.0, "#3A3A3C"), (1.0, "#2C2C2E")])
    ctx.fill()
    ctx.arc(47, 42, 9.5, 0, 2 * math.pi)
    set_hex(ctx, "#E5E7EB")
    ctx.fill()
    ctx.save()
    rounded_rect(ctx, 23, 24, 48, 52, 7)
    ctx.clip()
    ctx.arc(47, 68, 17, math.pi, 2 * math.pi)
    set_hex(ctx, "#E5E7EB")
    ctx.fill()
    ctx.restore()


def sym_phone(ctx: cairo.Context):
    # Classic vibrant green receiver
    ctx.save()
    ctx.translate(50, 50)
    ctx.rotate(-math.pi / 4)
    # Receiver arc shape
    ctx.new_sub_path()
    ctx.move_to(-20, -4)
    ctx.curve_to(-20, -16, 20, -16, 20, -4)
    ctx.line_to(22, 6)
    ctx.curve_to(22, 12, 11, 12, 10, 6)
    ctx.line_to(9, 1)
    ctx.curve_to(4, -2, -4, -2, -9, 1)
    ctx.line_to(-10, 6)
    ctx.curve_to(-11, 12, -22, 12, -22, 6)
    ctx.close_path()
    linear_grad(ctx, -20, -15, 20, 15, [(0.0, "#4ADE80"), (1.0, "#16A34A")])
    ctx.fill()
    ctx.restore()


def sym_messages(ctx: cairo.Context):
    # Green speech bubble
    ctx.save()
    ctx.arc(50, 47, 25, 0, 2 * math.pi)
    linear_grad(ctx, 25, 22, 75, 72, [(0.0, "#4ADE80"), (1.0, "#22C55E")])
    ctx.fill()
    # Tail
    ctx.move_to(35, 65)
    ctx.line_to(26, 75)
    ctx.line_to(45, 70)
    ctx.close_path()
    set_hex(ctx, "#22C55E")
    ctx.fill()
    # Typing dots
    for dx in (-9, 0, 9):
        ctx.arc(50 + dx, 47, 2.8, 0, 2 * math.pi)
        set_hex(ctx, "#FFFFFF", 0.92)
        ctx.fill()
    ctx.restore()


def sym_clock(ctx: cairo.Context):
    # iOS dark clock face with white hands & red second hand
    ctx.arc(50, 50, 29, 0, 2 * math.pi)
    set_hex(ctx, "#2C2C2E")
    ctx.fill()
    ctx.arc(50, 50, 26.5, 0, 2 * math.pi)
    set_hex(ctx, "#141416")
    ctx.fill()
    for i in range(12):
        ang = i * math.pi / 6
        r1, r2 = (21, 24.5) if i % 3 == 0 else (22.5, 24.5)
        ctx.set_line_width(1.8 if i % 3 == 0 else 1.0)
        set_hex(ctx, "#E5E7EB" if i % 3 == 0 else "#71717A")
        ctx.move_to(50 + r1 * math.cos(ang), 50 + r1 * math.sin(ang))
        ctx.line_to(50 + r2 * math.cos(ang), 50 + r2 * math.sin(ang))
        ctx.stroke()
    # Hour hand (10:10)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    ctx.set_line_width(3.0)
    set_hex(ctx, "#FFFFFF")
    ctx.move_to(50, 50)
    ctx.line_to(50 - 11, 50 - 7)
    ctx.stroke()
    # Minute hand
    ctx.set_line_width(2.4)
    ctx.move_to(50, 50)
    ctx.line_to(50 + 15, 50 - 11)
    ctx.stroke()
    # Red second hand
    ctx.set_line_width(1.3)
    set_hex(ctx, "#FF3B30")
    ctx.move_to(50, 55)
    ctx.line_to(50, 26)
    ctx.stroke()
    ctx.arc(50, 50, 2.3, 0, 2 * math.pi)
    set_hex(ctx, "#FF3B30")
    ctx.fill()


def sym_calculator(ctx: cairo.Context):
    rounded_rect(ctx, 27, 21, 46, 58, 9)
    set_hex(ctx, "#27272A")
    ctx.fill()
    # Display
    rounded_rect(ctx, 32, 26, 36, 11, 3)
    set_hex(ctx, "#111113")
    ctx.fill()
    # Buttons
    cols = ["#52525B", "#52525B", "#FF9F0A"]
    for row in range(3):
        for col in range(3):
            x = 32 + col * 13
            y = 42 + row * 11
            ctx.arc(x + 5, y + 4.5, 4.5, 0, 2 * math.pi)
            set_hex(ctx, cols[col])
            ctx.fill()


def sym_files(ctx: cairo.Context):
    # Classic Apple blue folder
    ctx.move_to(22, 34)
    ctx.line_to(41, 34)
    ctx.line_to(46, 40)
    ctx.line_to(78, 40)
    ctx.line_to(78, 72)
    ctx.line_to(22, 72)
    ctx.close_path()
    set_hex(ctx, "#0284C7")
    ctx.fill()
    rounded_rect(ctx, 22, 39, 56, 34, 6)
    linear_grad(ctx, 22, 39, 22, 73, [(0.0, "#38BDF8"), (1.0, "#0A84FF")])
    ctx.fill()


def sym_appmarket(ctx: cairo.Context):
    # Shopping bag with A / App Store blue-violet motif
    rounded_rect(ctx, 26, 33, 48, 42, 9)
    linear_grad(ctx, 26, 33, 74, 75, [(0.0, "#38BDF8"), (1.0, "#2563EB")])
    ctx.fill()
    # Handle
    ctx.set_line_width(4.2)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#E5E7EB")
    ctx.arc(50, 34, 10, math.pi, 2 * math.pi)
    ctx.stroke()
    # Center smile / A-bar
    ctx.set_line_width(4.0)
    set_hex(ctx, "#FFFFFF")
    ctx.arc(50, 50, 10, 0.25 * math.pi, 0.75 * math.pi)
    ctx.stroke()


def sym_settings(ctx: cairo.Context):
    # Mechanical gear in metallic silver + blue accent ring
    ctx.save()
    ctx.translate(50, 50)
    teeth = 10
    for i in range(teeth):
        ctx.save()
        ctx.rotate(i * (2 * math.pi / teeth))
        rounded_rect(ctx, -4.2, -29, 8.4, 10, 2.2)
        set_hex(ctx, "#9CA3AF")
        ctx.fill()
        ctx.restore()
    ctx.arc(0, 0, 22.5, 0, 2 * math.pi)
    linear_grad(ctx, -20, -20, 20, 20, [(0.0, "#E5E7EB"), (1.0, "#6B7280")])
    ctx.fill()
    ctx.arc(0, 0, 14, 0, 2 * math.pi)
    set_hex(ctx, "#27272A")
    ctx.fill()
    ctx.arc(0, 0, 8, 0, 2 * math.pi)
    set_hex(ctx, "#0A84FF")
    ctx.fill()
    ctx.restore()


def sym_calendar(ctx: cairo.Context):
    rounded_rect(ctx, 24, 24, 52, 52, 10)
    set_hex(ctx, "#F4F4F5")
    ctx.fill()
    # Red top header
    ctx.save()
    rounded_rect(ctx, 24, 24, 52, 52, 10)
    ctx.clip()
    ctx.rectangle(24, 24, 52, 16)
    set_hex(ctx, "#FF3B30")
    ctx.fill()
    ctx.restore()
    draw_text_centered(ctx, "18", 50, 57, 25, "#18181B")


def sym_notes(ctx: cairo.Context):
    rounded_rect(ctx, 25, 23, 50, 54, 9)
    set_hex(ctx, "#F4F4F5")
    ctx.fill()
    ctx.save()
    rounded_rect(ctx, 25, 23, 50, 54, 9)
    ctx.clip()
    ctx.rectangle(25, 23, 50, 15)
    linear_grad(ctx, 25, 23, 25, 38, [(0.0, "#FBBF24"), (1.0, "#F59E0B")])
    ctx.fill()
    ctx.restore()
    ctx.set_line_width(2.0)
    set_hex(ctx, "#A1A1AA")
    for y in (46, 55, 64):
        ctx.move_to(32, y)
        ctx.line_to(68, y)
        ctx.stroke()


def sym_health(ctx: cairo.Context):
    # Pink-red Apple Health heart
    ctx.save()
    ctx.translate(50, 53)
    ctx.move_to(0, 18)
    ctx.curve_to(-24, 4, -26, -18, -11, -21)
    ctx.curve_to(-3, -23, 0, -15, 0, -12)
    ctx.curve_to(0, -15, 3, -23, 11, -21)
    ctx.curve_to(26, -18, 24, 4, 0, 18)
    linear_grad(ctx, 0, -22, 0, 18, [(0.0, "#FF375F"), (1.0, "#FF2D55")])
    ctx.fill()
    ctx.restore()


def sym_id_shield(ctx: cairo.Context):
    ctx.move_to(50, 23)
    ctx.line_to(73, 32)
    ctx.curve_to(73, 55, 64, 68, 50, 77)
    ctx.curve_to(36, 68, 27, 55, 27, 32)
    ctx.close_path()
    linear_grad(ctx, 27, 23, 73, 77, [(0.0, "#38BDF8"), (1.0, "#0A84FF")])
    ctx.fill()
    ctx.arc(50, 44, 7, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    ctx.arc(50, 61, 10, math.pi, 2 * math.pi)
    ctx.fill()


def sym_safari_compass(ctx: cairo.Context):
    ctx.arc(50, 50, 28, 0, 2 * math.pi)
    linear_grad(ctx, 22, 22, 78, 78, [(0.0, "#38BDF8"), (1.0, "#0284C7")])
    ctx.fill()
    # Needle
    ctx.save()
    ctx.translate(50, 50)
    ctx.rotate(math.pi / 4)
    ctx.move_to(0, -21)
    ctx.line_to(5.5, 0)
    ctx.line_to(-5.5, 0)
    ctx.close_path()
    set_hex(ctx, "#FF3B30")
    ctx.fill()
    ctx.move_to(0, 21)
    ctx.line_to(5.5, 0)
    ctx.line_to(-5.5, 0)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    ctx.restore()


def sym_mail(ctx: cairo.Context):
    rounded_rect(ctx, 22, 31, 56, 38, 7)
    linear_grad(ctx, 22, 31, 78, 69, [(0.0, "#38BDF8"), (1.0, "#0A84FF")])
    ctx.fill()
    ctx.set_line_width(2.8)
    ctx.set_line_join(cairo.LINE_JOIN_ROUND)
    set_hex(ctx, "#FFFFFF")
    ctx.move_to(24, 34)
    ctx.line_to(50, 53)
    ctx.line_to(76, 34)
    ctx.stroke()


def sym_music(ctx: cairo.Context):
    # Red-pink note
    rounded_rect(ctx, 24, 24, 52, 52, 13)
    linear_grad(ctx, 24, 24, 76, 76, [(0.0, "#FA2D48"), (1.0, "#FB5C74")])
    ctx.fill()
    set_hex(ctx, "#FFFFFF")
    ctx.arc(40, 61, 6, 0, 2 * math.pi)
    ctx.fill()
    ctx.arc(60, 57, 6, 0, 2 * math.pi)
    ctx.fill()
    ctx.set_line_width(4.2)
    ctx.move_to(44.5, 61)
    ctx.line_to(44.5, 37)
    ctx.line_to(64.5, 33)
    ctx.line_to(64.5, 57)
    ctx.stroke()


def sym_video_player(ctx: cairo.Context):
    rounded_rect(ctx, 23, 27, 54, 46, 10)
    linear_grad(ctx, 23, 27, 77, 73, [(0.0, "#A855F7"), (1.0, "#6366F1")])
    ctx.fill()
    ctx.move_to(43, 39)
    ctx.line_to(64, 50)
    ctx.line_to(43, 61)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_weather(ctx: cairo.Context):
    # Blue sky tile + yellow sun + soft white cloud
    rounded_rect(ctx, 23, 23, 54, 54, 14)
    linear_grad(ctx, 23, 23, 23, 77, [(0.0, "#38BDF8"), (1.0, "#0284C7")])
    ctx.fill()
    ctx.arc(41, 42, 11, 0, 2 * math.pi)
    linear_grad(ctx, 30, 31, 52, 53, [(0.0, "#FDE047"), (1.0, "#F59E0B")])
    ctx.fill()
    set_hex(ctx, "#FFFFFF", 0.95)
    ctx.arc(47, 56, 9, 0, 2 * math.pi)
    ctx.arc(57, 53, 10, 0, 2 * math.pi)
    ctx.arc(65, 58, 7, 0, 2 * math.pi)
    ctx.fill()
    rounded_rect(ctx, 38, 54, 34, 11, 5.5)
    ctx.fill()


def sym_compass(ctx: cairo.Context):
    ctx.arc(50, 50, 28, 0, 2 * math.pi)
    set_hex(ctx, "#27272A")
    ctx.fill()
    ctx.set_line_width(1.6)
    for i in range(24):
        ang = i * math.pi / 12
        r1 = 21 if i % 6 == 0 else 23.5
        set_hex(ctx, "#FF3B30" if i == 18 else "#A1A1AA")
        ctx.move_to(50 + r1 * math.cos(ang), 50 + r1 * math.sin(ang))
        ctx.line_to(50 + 26 * math.cos(ang), 50 + 26 * math.sin(ang))
        ctx.stroke()
    ctx.move_to(50, 30)
    ctx.line_to(56, 50)
    ctx.line_to(44, 50)
    ctx.close_path()
    set_hex(ctx, "#FF3B30")
    ctx.fill()
    ctx.move_to(50, 70)
    ctx.line_to(56, 50)
    ctx.line_to(44, 50)
    ctx.close_path()
    set_hex(ctx, "#E5E7EB")
    ctx.fill()


def sym_voice_memo(ctx: cairo.Context):
    # Waveform bars in red-orange
    bars = [12, 24, 38, 20, 44, 30, 18, 34, 14]
    ctx.set_line_width(3.4)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    for i, h in enumerate(bars):
        x = 26 + i * 6
        set_hex(ctx, "#FF3B30" if i % 2 == 0 else "#FF6961")
        ctx.move_to(x, 50 - h / 2)
        ctx.line_to(x, 50 + h / 2)
        ctx.stroke()


def sym_wallet(ctx: cairo.Context):
    rounded_rect(ctx, 23, 27, 54, 46, 8)
    set_hex(ctx, "#27272A")
    ctx.fill()
    for y, col in [(31, "#0A84FF"), (36, "#FF9F0A"), (41, "#30D158"), (46, "#FF3B30")]:
        rounded_rect(ctx, 27, y, 46, 16, 4)
        set_hex(ctx, col)
        ctx.fill()
    rounded_rect(ctx, 23, 48, 54, 25, 6)
    set_hex(ctx, "#3F3F46")
    ctx.fill()


def sym_themes(ctx: cairo.Context):
    # Paint palette / brush strokes
    colors = ["#FF3B30", "#FF9F0A", "#30D158", "#0A84FF", "#AF52DE"]
    for i, col in enumerate(colors):
        ctx.save()
        ctx.translate(50, 50)
        ctx.rotate((i - 2) * 0.38)
        rounded_rect(ctx, -6, -26, 12, 34, 6)
        set_hex(ctx, col, 0.9)
        ctx.fill()
        ctx.restore()


def sym_shield_boost(ctx: cairo.Context):
    ctx.move_to(50, 22)
    ctx.line_to(74, 31)
    ctx.curve_to(74, 56, 65, 69, 50, 78)
    ctx.curve_to(35, 69, 26, 56, 26, 31)
    ctx.close_path()
    linear_grad(ctx, 26, 22, 74, 78, [(0.0, "#34D399"), (1.0, "#059669")])
    ctx.fill()
    # Bolt
    ctx.move_to(52, 34)
    ctx.line_to(41, 52)
    ctx.line_to(50, 52)
    ctx.line_to(47, 66)
    ctx.line_to(59, 47)
    ctx.line_to(50, 47)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_tips(ctx: cairo.Context):
    ctx.arc(50, 44, 15, 0, 2 * math.pi)
    linear_grad(ctx, 35, 29, 65, 59, [(0.0, "#FDE047"), (1.0, "#F59E0B")])
    ctx.fill()
    rounded_rect(ctx, 43, 57, 14, 12, 3)
    set_hex(ctx, "#9CA3AF")
    ctx.fill()
    rounded_rect(ctx, 45, 70, 10, 4, 2)
    set_hex(ctx, "#6B7280")
    ctx.fill()


def sym_search(ctx: cairo.Context):
    ctx.set_line_width(5.5)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#38BDF8")
    ctx.arc(46, 46, 15, 0, 2 * math.pi)
    ctx.stroke()
    ctx.move_to(57, 57)
    ctx.line_to(71, 71)
    ctx.stroke()


def sym_play_store(ctx: cairo.Context):
    # Colorful 4-triangle Play prism
    # Left cyan/blue
    ctx.move_to(32, 24)
    ctx.line_to(54, 50)
    ctx.line_to(32, 76)
    ctx.close_path()
    set_hex(ctx, "#38BDF8")
    ctx.fill()
    # Top green
    ctx.move_to(32, 24)
    ctx.line_to(63, 41)
    ctx.line_to(54, 50)
    ctx.close_path()
    set_hex(ctx, "#4ADE80")
    ctx.fill()
    # Bottom red
    ctx.move_to(32, 76)
    ctx.line_to(54, 50)
    ctx.line_to(63, 59)
    ctx.close_path()
    set_hex(ctx, "#F87171")
    ctx.fill()
    # Right yellow
    ctx.move_to(63, 41)
    ctx.line_to(74, 50)
    ctx.line_to(63, 59)
    ctx.line_to(54, 50)
    ctx.close_path()
    set_hex(ctx, "#FBBF24")
    ctx.fill()


def sym_whatsapp(ctx: cairo.Context):
    # Green bubble ring + phone handset
    ctx.arc(50, 49, 24, 0, 2 * math.pi)
    linear_grad(ctx, 26, 25, 74, 73, [(0.0, "#4ADE80"), (1.0, "#16A34A")])
    ctx.fill()
    ctx.move_to(34, 66)
    ctx.line_to(26, 75)
    ctx.line_to(43, 71)
    ctx.close_path()
    set_hex(ctx, "#16A34A")
    ctx.fill()
    # White receiver silhouette
    ctx.save()
    ctx.translate(50, 49)
    ctx.rotate(-math.pi / 4)
    ctx.move_to(-12, -3)
    ctx.curve_to(-12, -10, 12, -10, 12, -3)
    ctx.line_to(13, 4)
    ctx.curve_to(13, 7, 7, 7, 6, 4)
    ctx.line_to(6, 0)
    ctx.curve_to(2, -2, -2, -2, -6, 0)
    ctx.line_to(-6, 4)
    ctx.curve_to(-7, 7, -13, 7, -13, 4)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    ctx.restore()


def sym_whatsapp_business(ctx: cairo.Context):
    sym_whatsapp(ctx)
    draw_text_centered(ctx, "B", 68, 30, 16, "#FFFFFF")


def sym_telegram(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    linear_grad(ctx, 23, 23, 77, 77, [(0.0, "#38BDF8"), (1.0, "#0284C7")])
    ctx.fill()
    # Paper plane
    ctx.move_to(64, 36)
    ctx.line_to(32, 49)
    ctx.line_to(43, 53)
    ctx.line_to(59, 41)
    ctx.line_to(47, 55)
    ctx.line_to(57, 63)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_signal(ctx: cairo.Context):
    ctx.set_line_width(3.0)
    ctx.set_dash([5.0, 4.0])
    set_hex(ctx, "#3B82F6")
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    ctx.stroke()
    ctx.set_dash([])
    ctx.arc(50, 49, 20, 0, 2 * math.pi)
    set_hex(ctx, "#2563EB")
    ctx.fill()
    ctx.move_to(37, 63)
    ctx.line_to(31, 70)
    ctx.line_to(45, 67)
    ctx.close_path()
    ctx.fill()


def sym_instagram(ctx: cairo.Context):
    # Classic Instagram sunset gradient camera glyph
    rounded_rect(ctx, 24, 24, 52, 52, 15)
    linear_grad(ctx, 24, 76, 76, 24, [
        (0.0, "#FACC15"), (0.35, "#F97316"), (0.7, "#EC4899"), (1.0, "#8B5CF6")
    ])
    ctx.fill()
    ctx.set_line_width(3.8)
    set_hex(ctx, "#FFFFFF")
    rounded_rect(ctx, 32, 32, 36, 36, 10)
    ctx.stroke()
    ctx.arc(50, 50, 9.5, 0, 2 * math.pi)
    ctx.stroke()
    ctx.arc(61.5, 38.5, 2.4, 0, 2 * math.pi)
    ctx.fill()


def sym_tiktok(ctx: cairo.Context):
    # Cyan + Magenta + White offset music note
    def draw_note(dx, dy, col):
        ctx.save()
        ctx.translate(dx, dy)
        set_hex(ctx, col)
        ctx.set_line_width(6.2)
        ctx.set_line_cap(cairo.LINE_CAP_ROUND)
        ctx.arc(43, 60, 9.5, 0.35 * math.pi, 1.85 * math.pi)
        ctx.stroke()
        ctx.move_to(52.5, 60)
        ctx.line_to(52.5, 27)
        ctx.stroke()
        ctx.arc(65, 27, 12.5, math.pi * 0.55, math.pi)
        ctx.stroke()
        ctx.restore()

    draw_note(-2.5, -2.0, "#25F4EE")
    draw_note(2.5, 2.0, "#FE2C55")
    draw_note(0, 0, "#FFFFFF")


def sym_x_twitter(ctx: cairo.Context):
    set_hex(ctx, "#FFFFFF")
    ctx.set_line_width(5.2)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    ctx.move_to(29, 27)
    ctx.line_to(71, 73)
    ctx.stroke()
    ctx.move_to(71, 27)
    ctx.line_to(29, 73)
    ctx.stroke()
    # Accent parallel stroke for X stylized look
    ctx.set_line_width(2.6)
    ctx.move_to(35, 27)
    ctx.line_to(76, 73)
    ctx.stroke()


def sym_youtube(ctx: cairo.Context):
    rounded_rect(ctx, 21, 30, 58, 40, 12)
    set_hex(ctx, "#FF0000")
    ctx.fill()
    ctx.move_to(44, 41)
    ctx.line_to(62, 50)
    ctx.line_to(44, 59)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_youtube_music(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#FF0000")
    ctx.fill()
    ctx.set_line_width(2.2)
    set_hex(ctx, "#FFFFFF")
    ctx.arc(50, 50, 17, 0, 2 * math.pi)
    ctx.stroke()
    ctx.move_to(44, 42)
    ctx.line_to(60, 50)
    ctx.line_to(44, 58)
    ctx.close_path()
    ctx.fill()


def sym_spotify(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#1DB954")
    ctx.fill()
    set_hex(ctx, "#09090B")
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    for r, w, y in [(26, 4.2, 46), (20, 3.6, 53), (14, 3.0, 59)]:
        ctx.set_line_width(w)
        ctx.arc(48, y + 12, r, -2.05, -0.95)
        ctx.stroke()


def sym_chrome(ctx: cairo.Context):
    # Classic 3-sector ring + blue center
    ctx.save()
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    ctx.clip()
    # Red top
    ctx.move_to(50, 50)
    ctx.arc(50, 50, 30, -math.pi * 0.95, -math.pi * 0.1)
    ctx.close_path()
    set_hex(ctx, "#EA4335")
    ctx.fill()
    # Yellow right
    ctx.move_to(50, 50)
    ctx.arc(50, 50, 30, -math.pi * 0.1, math.pi * 0.55)
    ctx.close_path()
    set_hex(ctx, "#FBBC05")
    ctx.fill()
    # Green left
    ctx.move_to(50, 50)
    ctx.arc(50, 50, 30, math.pi * 0.55, math.pi * 1.05)
    ctx.close_path()
    set_hex(ctx, "#34A853")
    ctx.fill()
    ctx.restore()
    ctx.arc(50, 50, 12, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    ctx.arc(50, 50, 9.5, 0, 2 * math.pi)
    set_hex(ctx, "#4285F4")
    ctx.fill()


def sym_gmail(ctx: cairo.Context):
    ctx.set_line_width(7.5)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    ctx.set_line_join(cairo.LINE_JOIN_ROUND)
    # Left blue post
    set_hex(ctx, "#4285F4")
    ctx.move_to(26, 66)
    ctx.line_to(26, 36)
    ctx.stroke()
    # Left-top red
    set_hex(ctx, "#EA4335")
    ctx.move_to(26, 36)
    ctx.line_to(50, 54)
    ctx.stroke()
    # Right-top yellow
    set_hex(ctx, "#FBBC05")
    ctx.move_to(50, 54)
    ctx.line_to(74, 36)
    ctx.stroke()
    # Right green post
    set_hex(ctx, "#34A853")
    ctx.move_to(74, 36)
    ctx.line_to(74, 66)
    ctx.stroke()


def sym_google_maps(ctx: cairo.Context):
    # Multi-color Google Maps pin
    ctx.save()
    ctx.move_to(50, 77)
    ctx.curve_to(32, 57, 29, 46, 29, 40)
    ctx.arc(50, 40, 21, math.pi, 2 * math.pi)
    ctx.curve_to(71, 46, 68, 57, 50, 77)
    ctx.close_path()
    linear_grad(ctx, 29, 20, 71, 77, [(0.0, "#EA4335"), (0.45, "#FBBC05"), (1.0, "#34A853")])
    ctx.fill()
    ctx.restore()
    ctx.arc(50, 40, 8.5, 0, 2 * math.pi)
    set_hex(ctx, "#18181B")
    ctx.fill()
    ctx.arc(50, 40, 5.5, 0, 2 * math.pi)
    set_hex(ctx, "#4285F4")
    ctx.fill()


def sym_trade_republic(ctx: cairo.Context):
    # Crisp geometric upward chart + white TR mark
    ctx.set_line_width(4.5)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    ctx.set_line_join(cairo.LINE_JOIN_ROUND)
    set_hex(ctx, "#FFFFFF")
    ctx.move_to(25, 62)
    ctx.line_to(42, 46)
    ctx.line_to(54, 55)
    ctx.line_to(75, 31)
    ctx.stroke()
    ctx.move_to(63, 31)
    ctx.line_to(75, 31)
    ctx.line_to(75, 43)
    ctx.stroke()
    ctx.arc(75, 31, 3.0, 0, 2 * math.pi)
    set_hex(ctx, "#30D158")
    ctx.fill()


def sym_paypal(ctx: cairo.Context):
    draw_text_centered(ctx, "P", 45, 47, 42, "#0070BA")
    draw_text_centered(ctx, "P", 54, 54, 42, "#003087" if False else "#38BDF8")


def sym_sparkasse(ctx: cairo.Context):
    # Classic Sparkasse red S-dot mark
    rounded_rect(ctx, 25, 25, 50, 50, 12)
    set_hex(ctx, "#FF0000")
    ctx.fill()
    ctx.arc(50, 38, 5.2, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    draw_text_centered(ctx, "S", 50, 57, 26, "#FFFFFF")


def sym_n26(ctx: cairo.Context):
    rounded_rect(ctx, 25, 29, 50, 42, 8)
    linear_grad(ctx, 25, 29, 75, 71, [(0.0, "#48C9B0"), (1.0, "#14B8A6")])
    ctx.fill()
    draw_text_centered(ctx, "N26", 50, 50, 20, "#09090B")


def sym_amazon(ctx: cairo.Context):
    draw_text_centered(ctx, "a", 50, 42, 40, "#FFFFFF")
    ctx.set_line_width(3.6)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FF9900")
    ctx.arc(50, 42, 23, 0.28 * math.pi, 0.72 * math.pi)
    ctx.stroke()
    ctx.move_to(63, 57)
    ctx.line_to(68, 61)
    ctx.line_to(61, 66)
    ctx.stroke()


def sym_netflix(ctx: cairo.Context):
    # Iconic 3-ribbon red N
    set_hex(ctx, "#B91C1C")
    ctx.rectangle(33, 25, 10, 50)
    ctx.fill()
    ctx.rectangle(57, 25, 10, 50)
    ctx.fill()
    ctx.move_to(33, 25)
    ctx.line_to(43, 25)
    ctx.line_to(67, 75)
    ctx.line_to(57, 75)
    ctx.close_path()
    set_hex(ctx, "#E50914")
    ctx.fill()


def sym_chatgpt(ctx: cairo.Context):
    # Interlocked emerald/teal geometric knot
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    linear_grad(ctx, 23, 23, 77, 77, [(0.0, "#10A37F"), (1.0, "#0D8A6B")])
    ctx.fill()
    ctx.set_line_width(2.8)
    set_hex(ctx, "#FFFFFF")
    for i in range(6):
        ang = i * math.pi / 3
        ctx.save()
        ctx.translate(50, 50)
        ctx.rotate(ang)
        rounded_rect(ctx, -6, -18, 12, 20, 6)
        ctx.stroke()
        ctx.restore()


def sym_discord(ctx: cairo.Context):
    # Blurple game controller / Clyde badge
    rounded_rect(ctx, 23, 30, 54, 40, 15)
    set_hex(ctx, "#5865F2")
    ctx.fill()
    ctx.arc(41, 50, 5.2, 0, 2 * math.pi)
    ctx.arc(59, 50, 5.2, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_reddit(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#FF4500")
    ctx.fill()
    # Antenna + ears + face
    set_hex(ctx, "#FFFFFF")
    ctx.arc(50, 54, 14, 0, 2 * math.pi)
    ctx.fill()
    ctx.arc(36, 48, 4.5, 0, 2 * math.pi)
    ctx.arc(64, 48, 4.5, 0, 2 * math.pi)
    ctx.fill()
    ctx.set_line_width(2.5)
    ctx.move_to(50, 40)
    ctx.line_to(56, 31)
    ctx.line_to(62, 33)
    ctx.stroke()
    ctx.arc(63, 33, 3.0, 0, 2 * math.pi)
    ctx.fill()
    # Eyes
    set_hex(ctx, "#FF4500")
    ctx.arc(44.5, 53, 2.5, 0, 2 * math.pi)
    ctx.arc(55.5, 53, 2.5, 0, 2 * math.pi)
    ctx.fill()


def sym_github(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#F4F4F5")
    ctx.fill()
    # Octocat-inspired stylized silhouette in dark
    set_hex(ctx, "#18181B")
    ctx.arc(50, 48, 13, 0, 2 * math.pi)
    ctx.fill()
    # Ears
    ctx.move_to(38, 42)
    ctx.line_to(37, 32)
    ctx.line_to(45, 37)
    ctx.close_path()
    ctx.fill()
    ctx.move_to(62, 42)
    ctx.line_to(63, 32)
    ctx.line_to(55, 37)
    ctx.close_path()
    ctx.fill()
    rounded_rect(ctx, 43, 57, 14, 19, 5)
    ctx.fill()


def sym_google_g(ctx: cairo.Context):
    ctx.set_line_width(8.5)
    ctx.set_line_cap(cairo.LINE_CAP_BUTT)
    for a0, a1, col in [
        (-math.pi * 0.85, -math.pi * 0.15, "#EA4335"),
        (-math.pi * 1.15, -math.pi * 0.85, "#FBBC05"),
        (math.pi * 0.4, math.pi * 0.85, "#34A853"),
        (-0.02, math.pi * 0.4, "#4285F4"),
    ]:
        set_hex(ctx, col)
        ctx.arc(50, 50, 20, a0, a1)
        ctx.stroke()
    set_hex(ctx, "#4285F4")
    ctx.rectangle(50, 46, 23, 8)
    ctx.fill()


def sym_google_photos(ctx: cairo.Context):
    # Pinwheel 4-semicircle flower
    petals = [
        (50, 38, math.pi, 2 * math.pi, "#EA4335"),
        (62, 50, -math.pi / 2, math.pi / 2, "#FBBC05"),
        (50, 62, 0, math.pi, "#34A853"),
        (38, 50, math.pi / 2, 3 * math.pi / 2, "#4285F4"),
    ]
    for cx, cy, a0, a1, col in petals:
        ctx.move_to(cx, cy)
        ctx.arc(cx, cy, 13, a0, a1)
        ctx.close_path()
        set_hex(ctx, col)
        ctx.fill()


def sym_google_drive(ctx: cairo.Context):
    ctx.set_line_width(8.0)
    ctx.set_line_join(cairo.LINE_JOIN_ROUND)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FBBC05")
    ctx.move_to(50, 28)
    ctx.line_to(71, 64)
    ctx.stroke()
    set_hex(ctx, "#34A853")
    ctx.move_to(50, 28)
    ctx.line_to(29, 64)
    ctx.stroke()
    set_hex(ctx, "#4285F4")
    ctx.move_to(29, 64)
    ctx.line_to(71, 64)
    ctx.stroke()


def sym_google_calendar(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 9)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    ctx.set_line_width(6.0)
    set_hex(ctx, "#4285F4")
    ctx.move_to(28, 28)
    ctx.line_to(72, 28)
    ctx.stroke()
    set_hex(ctx, "#34A853")
    ctx.move_to(28, 72)
    ctx.line_to(62, 72)
    ctx.stroke()
    draw_text_centered(ctx, "31", 50, 52, 24, "#4285F4")


def sym_translate(ctx: cairo.Context):
    rounded_rect(ctx, 24, 26, 32, 32, 6)
    set_hex(ctx, "#4285F4")
    ctx.fill()
    draw_text_centered(ctx, "A", 40, 42, 20, "#FFFFFF")
    rounded_rect(ctx, 44, 42, 32, 32, 6)
    set_hex(ctx, "#E5E7EB")
    ctx.fill()
    # Geometric CJK-inspired radical stroke (no font dependency)
    ctx.set_line_width(2.6)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#18181B")
    ctx.move_to(60, 48)
    ctx.line_to(60, 52)
    ctx.move_to(51, 52)
    ctx.line_to(69, 52)
    ctx.move_to(65, 52)
    ctx.curve_to(63, 60, 56, 65, 52, 67)
    ctx.move_to(55, 54)
    ctx.curve_to(58, 60, 64, 64, 68, 67)
    ctx.stroke()


def sym_keep(ctx: cairo.Context):
    rounded_rect(ctx, 27, 25, 46, 50, 9)
    set_hex(ctx, "#FBBC05")
    ctx.fill()
    ctx.arc(50, 46, 8.5, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    rounded_rect(ctx, 45, 56, 10, 8, 2)
    ctx.fill()


def sym_authenticator(ctx: cairo.Context):
    ctx.arc(50, 50, 26, 0, 2 * math.pi)
    set_hex(ctx, "#52525B")
    ctx.fill()
    ctx.arc(50, 50, 17, 0, 2 * math.pi)
    set_hex(ctx, "#18181B")
    ctx.fill()
    for a, col in [(0, "#EA4335"), (math.pi / 2, "#FBBC05"), (math.pi, "#34A853"), (3 * math.pi / 2, "#4285F4")]:
        ctx.arc(50 + 20 * math.cos(a), 50 + 20 * math.sin(a), 4, 0, 2 * math.pi)
        set_hex(ctx, col)
        ctx.fill()


def sym_meet(ctx: cairo.Context):
    rounded_rect(ctx, 23, 33, 38, 34, 7)
    set_hex(ctx, "#34A853")
    ctx.fill()
    ctx.move_to(61, 42)
    ctx.line_to(77, 33)
    ctx.line_to(77, 67)
    ctx.line_to(61, 58)
    ctx.close_path()
    set_hex(ctx, "#4285F4")
    ctx.fill()


def sym_apple_music(ctx: cairo.Context):
    sym_music(ctx)


def sym_shazam(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    linear_grad(ctx, 23, 23, 77, 77, [(0.0, "#38BDF8"), (1.0, "#0284C7")])
    ctx.fill()
    ctx.set_line_width(4.8)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FFFFFF")
    ctx.arc(46, 47, 10, math.pi * 0.9, math.pi * 2.15)
    ctx.stroke()
    ctx.arc(54, 53, 10, -math.pi * 0.1, math.pi * 1.15)
    ctx.stroke()


def sym_soundcloud(ctx: cairo.Context):
    set_hex(ctx, "#FF5500")
    ctx.arc(56, 52, 13, 0, 2 * math.pi)
    ctx.arc(43, 55, 9, 0, 2 * math.pi)
    ctx.fill()
    rounded_rect(ctx, 40, 53, 30, 12, 6)
    ctx.fill()
    ctx.set_line_width(3.0)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    for x, y0 in [(26, 51), (31, 47), (36, 45)]:
        ctx.move_to(x, y0)
        ctx.line_to(x, 64)
        ctx.stroke()


def sym_vlc(ctx: cairo.Context):
    # Orange traffic cone with white stripes
    ctx.move_to(44, 24)
    ctx.line_to(56, 24)
    ctx.line_to(68, 70)
    ctx.line_to(32, 70)
    ctx.close_path()
    set_hex(ctx, "#FF6600")
    ctx.fill()
    set_hex(ctx, "#FFFFFF")
    ctx.move_to(40, 42)
    ctx.line_to(60, 42)
    ctx.line_to(62, 50)
    ctx.line_to(38, 50)
    ctx.close_path()
    ctx.fill()
    rounded_rect(ctx, 25, 68, 50, 7, 3.5)
    set_hex(ctx, "#EA580C")
    ctx.fill()


def sym_twitch(ctx: cairo.Context):
    ctx.move_to(28, 25)
    ctx.line_to(72, 25)
    ctx.line_to(72, 56)
    ctx.line_to(58, 70)
    ctx.line_to(46, 70)
    ctx.line_to(38, 77)
    ctx.line_to(38, 70)
    ctx.line_to(28, 70)
    ctx.close_path()
    set_hex(ctx, "#9146FF")
    ctx.fill()
    rounded_rect(ctx, 43, 37, 5, 14, 2)
    rounded_rect(ctx, 56, 37, 5, 14, 2)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_snapchat(ctx: cairo.Context):
    rounded_rect(ctx, 24, 24, 52, 52, 14)
    set_hex(ctx, "#FFFC00")
    ctx.fill()
    ctx.arc(50, 45, 11, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    rounded_rect(ctx, 36, 48, 28, 16, 8)
    ctx.fill()


def sym_facebook(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#1877F2")
    ctx.fill()
    draw_text_centered(ctx, "f", 52, 54, 46, "#FFFFFF")


def sym_fb_messenger(ctx: cairo.Context):
    ctx.arc(50, 48, 25, 0, 2 * math.pi)
    linear_grad(ctx, 25, 23, 75, 73, [(0.0, "#FF5280"), (0.5, "#A033FF"), (1.0, "#0099FF")])
    ctx.fill()
    ctx.move_to(36, 53)
    ctx.line_to(48, 42)
    ctx.line_to(53, 48)
    ctx.line_to(64, 43)
    ctx.line_to(52, 55)
    ctx.line_to(47, 49)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_linkedin(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 10)
    set_hex(ctx, "#0A66C2")
    ctx.fill()
    draw_text_centered(ctx, "in", 50, 52, 28, "#FFFFFF")


def sym_pinterest(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#E60023")
    ctx.fill()
    draw_text_centered(ctx, "P", 50, 52, 34, "#FFFFFF")


def sym_threads(ctx: cairo.Context):
    draw_text_centered(ctx, "@", 50, 49, 44, "#FFFFFF")


def sym_bluesky(ctx: cairo.Context):
    set_hex(ctx, "#0085FF")
    for sx in (-1, 1):
        ctx.save()
        ctx.translate(50, 50)
        ctx.scale(sx, 1)
        ctx.move_to(0, -3)
        ctx.curve_to(10, -22, 27, -20, 23, -3)
        ctx.curve_to(21, 5, 13, 7, 8, 5)
        ctx.curve_to(16, 8, 17, 22, 6, 20)
        ctx.curve_to(2, 19, 0, 12, 0, 7)
        ctx.close_path()
        ctx.fill()
        ctx.restore()


def sym_claude(ctx: cairo.Context):
    # Warm terracotta starburst
    set_hex(ctx, "#D97757")
    ctx.set_line_width(4.8)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    for i in range(10):
        ang = i * math.pi / 5
        r = 24 if i % 2 == 0 else 17
        ctx.move_to(50, 50)
        ctx.line_to(50 + r * math.cos(ang), 50 + r * math.sin(ang))
        ctx.stroke()


def sym_gemini(ctx: cairo.Context):
    # 4-point blue-purple sparkle
    ctx.move_to(50, 22)
    ctx.curve_to(53, 44, 56, 47, 78, 50)
    ctx.curve_to(56, 53, 53, 56, 50, 78)
    ctx.curve_to(47, 56, 44, 53, 22, 50)
    ctx.curve_to(44, 47, 47, 44, 50, 22)
    ctx.close_path()
    linear_grad(ctx, 22, 22, 78, 78, [(0.0, "#60A5FA"), (0.5, "#818CF8"), (1.0, "#C084FC")])
    ctx.fill()


def sym_perplexity(ctx: cairo.Context):
    # Teal geometric crystal prism
    ctx.set_line_width(3.4)
    ctx.set_line_join(cairo.LINE_JOIN_ROUND)
    set_hex(ctx, "#20B8CD")
    ctx.move_to(50, 23)
    ctx.line_to(50, 77)
    ctx.move_to(28, 35)
    ctx.line_to(72, 65)
    ctx.move_to(72, 35)
    ctx.line_to(28, 65)
    ctx.stroke()
    rounded_rect(ctx, 34, 34, 32, 32, 4)
    ctx.stroke()


def sym_deepl(ctx: cairo.Context):
    # DeepL navy hexagon + white network node
    ctx.move_to(50, 23)
    ctx.line_to(73, 36)
    ctx.line_to(73, 64)
    ctx.line_to(50, 77)
    ctx.line_to(27, 64)
    ctx.line_to(27, 36)
    ctx.close_path()
    set_hex(ctx, "#0F2B46")
    ctx.fill()
    ctx.set_line_width(2.5)
    set_hex(ctx, "#38BDF8")
    ctx.stroke_preserve()
    set_hex(ctx, "#FFFFFF")
    ctx.arc(50, 42, 4, 0, 2 * math.pi)
    ctx.arc(41, 57, 4, 0, 2 * math.pi)
    ctx.arc(59, 57, 4, 0, 2 * math.pi)
    ctx.fill()


def sym_protonmail(ctx: cairo.Context):
    rounded_rect(ctx, 24, 30, 52, 40, 8)
    linear_grad(ctx, 24, 30, 76, 70, [(0.0, "#8B5CF6"), (1.0, "#6D28D9")])
    ctx.fill()
    ctx.set_line_width(3.2)
    set_hex(ctx, "#DDD6FE")
    ctx.move_to(26, 34)
    ctx.line_to(50, 53)
    ctx.line_to(74, 34)
    ctx.stroke()


def sym_protonvpn(ctx: cairo.Context):
    ctx.move_to(26, 30)
    ctx.line_to(74, 42)
    ctx.line_to(44, 74)
    ctx.close_path()
    linear_grad(ctx, 26, 30, 74, 74, [(0.0, "#4ADE80"), (1.0, "#6D28D9")])
    ctx.fill()


def sym_bitwarden(ctx: cairo.Context):
    ctx.move_to(50, 23)
    ctx.line_to(72, 31)
    ctx.curve_to(72, 54, 63, 68, 50, 77)
    ctx.curve_to(37, 68, 28, 54, 28, 31)
    ctx.close_path()
    set_hex(ctx, "#175DDC")
    ctx.fill()
    ctx.move_to(50, 28)
    ctx.line_to(66, 34)
    ctx.curve_to(66, 52, 59, 63, 50, 70)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_onepassword(ctx: cairo.Context):
    ctx.arc(50, 50, 26, 0, 2 * math.pi)
    set_hex(ctx, "#0A84FF")
    ctx.fill()
    ctx.arc(50, 50, 18, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    rounded_rect(ctx, 46, 35, 8, 30, 3)
    set_hex(ctx, "#18181B")
    ctx.fill()


def sym_ms_teams(ctx: cairo.Context):
    ctx.arc(62, 37, 6, 0, 2 * math.pi)
    set_hex(ctx, "#7B83EB")
    ctx.fill()
    rounded_rect(ctx, 48, 45, 24, 22, 5)
    set_hex(ctx, "#5059C9")
    ctx.fill()
    rounded_rect(ctx, 26, 35, 32, 32, 7)
    set_hex(ctx, "#464EB8")
    ctx.fill()
    draw_text_centered(ctx, "T", 42, 51, 20, "#FFFFFF")


def sym_outlook(ctx: cairo.Context):
    rounded_rect(ctx, 34, 30, 42, 40, 6)
    set_hex(ctx, "#0364B8")
    ctx.fill()
    rounded_rect(ctx, 24, 34, 30, 32, 6)
    set_hex(ctx, "#0078D4")
    ctx.fill()
    draw_text_centered(ctx, "O", 39, 50, 20, "#FFFFFF")


def sym_onedrive(ctx: cairo.Context):
    set_hex(ctx, "#0364B8")
    ctx.arc(43, 50, 14, 0, 2 * math.pi)
    ctx.fill()
    set_hex(ctx, "#28A8EA")
    ctx.arc(58, 53, 12, 0, 2 * math.pi)
    ctx.fill()
    rounded_rect(ctx, 31, 51, 39, 14, 7)
    set_hex(ctx, "#50E6FF")
    ctx.fill()


def sym_notion(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 8)
    set_hex(ctx, "#F4F4F5")
    ctx.fill()
    draw_text_centered(ctx, "N", 50, 51, 32, "#18181B")


def sym_obsidian(ctx: cairo.Context):
    ctx.move_to(52, 23)
    ctx.line_to(72, 42)
    ctx.line_to(61, 75)
    ctx.line_to(35, 70)
    ctx.line_to(29, 45)
    ctx.close_path()
    linear_grad(ctx, 29, 23, 72, 75, [(0.0, "#A855F7"), (1.0, "#4C1D95")])
    ctx.fill()


def sym_todoist(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 12)
    set_hex(ctx, "#E44332")
    ctx.fill()
    ctx.set_line_width(4.0)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FFFFFF")
    for dy in (-10, 0, 10):
        ctx.move_to(34, 48 + dy)
        ctx.line_to(44, 54 + dy)
        ctx.line_to(65, 42 + dy)
        ctx.stroke()


def sym_slack(ctx: cairo.Context):
    # 4-color hash pills
    items = [
        (33, 42, 16, 7, "#36C5F0"),
        (51, 42, 7, 16, "#2EB67D"),
        (51, 51, 16, 7, "#ECB22E"),
        (42, 42, 7, 16, "#E01E5A"),
    ]
    for x, y, w, h, col in items:
        rounded_rect(ctx, x, y, w, h, 3.5)
        set_hex(ctx, col)
        ctx.fill()
    for cx, cy, col in [(36, 35, "#36C5F0"), (63, 35, "#2EB67D"), (63, 64, "#ECB22E"), (36, 64, "#E01E5A")]:
        ctx.arc(cx, cy, 4, 0, 2 * math.pi)
        set_hex(ctx, col)
        ctx.fill()


def sym_zoom(ctx: cairo.Context):
    rounded_rect(ctx, 22, 22, 56, 56, 15)
    set_hex(ctx, "#2D8CFF")
    ctx.fill()
    rounded_rect(ctx, 30, 39, 28, 22, 5)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    ctx.move_to(60, 44)
    ctx.line_to(71, 38)
    ctx.line_to(71, 62)
    ctx.line_to(60, 56)
    ctx.close_path()
    ctx.fill()


def sym_duolingo(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 14)
    set_hex(ctx, "#58CC02")
    ctx.fill()
    set_hex(ctx, "#FFFFFF")
    ctx.arc(40, 47, 7, 0, 2 * math.pi)
    ctx.arc(60, 47, 7, 0, 2 * math.pi)
    ctx.fill()
    set_hex(ctx, "#18181B")
    ctx.arc(41, 47, 3.2, 0, 2 * math.pi)
    ctx.arc(59, 47, 3.2, 0, 2 * math.pi)
    ctx.fill()
    set_hex(ctx, "#F59E0B")
    ctx.move_to(46, 53)
    ctx.line_to(54, 53)
    ctx.line_to(50, 61)
    ctx.close_path()
    ctx.fill()


def sym_kindle(ctx: cairo.Context):
    rounded_rect(ctx, 30, 24, 40, 52, 6)
    set_hex(ctx, "#1D4ED8")
    ctx.fill()
    draw_text_centered(ctx, "K", 50, 50, 28, "#FFFFFF")


def sym_audible(ctx: cairo.Context):
    rounded_rect(ctx, 24, 24, 52, 52, 13)
    set_hex(ctx, "#F7991C")
    ctx.fill()
    ctx.set_line_width(3.4)
    set_hex(ctx, "#FFFFFF")
    for r in (8, 15, 22):
        ctx.arc(50, 58, r, math.pi * 1.15, math.pi * 1.85)
        ctx.stroke()


def sym_disney_plus(ctx: cairo.Context):
    rounded_rect(ctx, 24, 24, 52, 52, 14)
    linear_grad(ctx, 24, 24, 76, 76, [(0.0, "#113CCF"), (1.0, "#041657")])
    ctx.fill()
    ctx.set_line_width(3.0)
    set_hex(ctx, "#60A5FA")
    ctx.arc(50, 54, 20, math.pi * 1.1, math.pi * 1.9)
    ctx.stroke()
    draw_text_centered(ctx, "D+", 50, 54, 22, "#FFFFFF")


def sym_prime_video(ctx: cairo.Context):
    rounded_rect(ctx, 24, 24, 52, 52, 14)
    set_hex(ctx, "#00A8E1")
    ctx.fill()
    ctx.move_to(43, 39)
    ctx.line_to(62, 50)
    ctx.line_to(43, 61)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_steam(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    linear_grad(ctx, 50, 23, 50, 77, [(0.0, "#1B2838"), (1.0, "#1D4ED8")])
    ctx.fill()
    ctx.set_line_width(4.5)
    set_hex(ctx, "#FFFFFF")
    ctx.arc(57, 41, 8, 0, 2 * math.pi)
    ctx.stroke()
    ctx.arc(41, 58, 6, 0, 2 * math.pi)
    ctx.fill()
    ctx.move_to(41, 58)
    ctx.line_to(54, 44)
    ctx.stroke()


def sym_playstation(ctx: cairo.Context):
    draw_text_centered(ctx, "PS", 50, 50, 28, "#0070D1")


def sym_xbox(ctx: cairo.Context):
    ctx.arc(50, 50, 26, 0, 2 * math.pi)
    set_hex(ctx, "#107C10")
    ctx.fill()
    ctx.set_line_width(4.2)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FFFFFF")
    ctx.move_to(35, 35)
    ctx.line_to(65, 65)
    ctx.move_to(65, 35)
    ctx.line_to(35, 65)
    ctx.stroke()


def sym_uber(ctx: cairo.Context):
    rounded_rect(ctx, 27, 27, 46, 46, 10)
    set_hex(ctx, "#FFFFFF")
    ctx.stroke_preserve()
    ctx.set_line_width(2.5)
    set_hex(ctx, "#27272A")
    ctx.fill()
    draw_text_centered(ctx, "Uber", 50, 50, 17, "#FFFFFF")


def sym_airbnb(ctx: cairo.Context):
    ctx.set_line_width(4.2)
    ctx.set_line_join(cairo.LINE_JOIN_ROUND)
    set_hex(ctx, "#FF5A5F")
    ctx.move_to(50, 26)
    ctx.curve_to(62, 45, 72, 62, 62, 70)
    ctx.curve_to(55, 75, 45, 75, 38, 70)
    ctx.curve_to(28, 62, 38, 45, 50, 26)
    ctx.stroke()
    ctx.arc(50, 56, 6, 0, 2 * math.pi)
    ctx.stroke()


def sym_booking(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 12)
    set_hex(ctx, "#003580")
    ctx.fill()
    draw_text_centered(ctx, "B.", 50, 51, 26, "#FFFFFF")


def sym_db_navigator(ctx: cairo.Context):
    rounded_rect(ctx, 24, 30, 52, 40, 8)
    set_hex(ctx, "#EC0016")
    ctx.fill()
    rounded_rect(ctx, 29, 35, 42, 30, 5)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    draw_text_centered(ctx, "DB", 50, 50, 22, "#EC0016")


def sym_dhl(ctx: cairo.Context):
    rounded_rect(ctx, 22, 34, 56, 32, 7)
    set_hex(ctx, "#FFCC00")
    ctx.fill()
    draw_text_centered(ctx, "DHL", 50, 50, 18, "#D40511")


def sym_kleinanzeigen(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#86B817")
    ctx.fill()
    draw_text_centered(ctx, "K", 50, 51, 30, "#FFFFFF")


def sym_lieferando(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 14)
    set_hex(ctx, "#FF8000")
    ctx.fill()
    ctx.move_to(50, 31)
    ctx.line_to(67, 45)
    ctx.line_to(63, 45)
    ctx.line_to(63, 65)
    ctx.line_to(37, 65)
    ctx.line_to(37, 45)
    ctx.line_to(33, 45)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_revolut(ctx: cairo.Context):
    draw_text_centered(ctx, "R", 50, 51, 42, "#FFFFFF")


def sym_wise(ctx: cairo.Context):
    ctx.arc(50, 50, 26, 0, 2 * math.pi)
    set_hex(ctx, "#9FE870")
    ctx.fill()
    draw_text_centered(ctx, "W", 50, 51, 26, "#163300")


def sym_ing(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 12)
    set_hex(ctx, "#FF6200")
    ctx.fill()
    draw_text_centered(ctx, "ING", 50, 50, 20, "#FFFFFF")


def sym_dkb(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 12)
    set_hex(ctx, "#148DEA")
    ctx.fill()
    draw_text_centered(ctx, "DKB", 50, 50, 18, "#FFFFFF")


def sym_klarna(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 14)
    set_hex(ctx, "#FFB3C7")
    ctx.fill()
    draw_text_centered(ctx, "K.", 50, 51, 26, "#0A0B09")


def sym_komoot(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#6AA127")
    ctx.fill()
    ctx.move_to(50, 32)
    ctx.line_to(67, 65)
    ctx.line_to(50, 57)
    ctx.line_to(33, 65)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_strava(ctx: cairo.Context):
    ctx.move_to(44, 28)
    ctx.line_to(58, 56)
    ctx.line_to(49, 56)
    ctx.line_to(44, 46)
    ctx.line_to(39, 56)
    ctx.line_to(30, 56)
    ctx.close_path()
    set_hex(ctx, "#FC4C02")
    ctx.fill()
    ctx.move_to(57, 56)
    ctx.line_to(64, 72)
    ctx.line_to(71, 56)
    ctx.close_path()
    set_hex(ctx, "#FB923C")
    ctx.fill()


def sym_garmin(ctx: cairo.Context):
    ctx.move_to(50, 28)
    ctx.line_to(71, 66)
    ctx.line_to(29, 66)
    ctx.close_path()
    set_hex(ctx, "#007CC3")
    ctx.fill()


def sym_firefox(ctx: cairo.Context):
    ctx.arc(50, 50, 25, 0, 2 * math.pi)
    linear_grad(ctx, 25, 25, 75, 75, [(0.0, "#FACC15"), (0.5, "#F97316"), (1.0, "#EA580C")])
    ctx.fill()
    ctx.arc(53, 52, 13, 0, 2 * math.pi)
    linear_grad(ctx, 40, 39, 66, 65, [(0.0, "#8B5CF6"), (1.0, "#2563EB")])
    ctx.fill()


def sym_brave(ctx: cairo.Context):
    ctx.move_to(33, 29)
    ctx.line_to(50, 24)
    ctx.line_to(67, 29)
    ctx.line_to(71, 48)
    ctx.line_to(50, 76)
    ctx.line_to(29, 48)
    ctx.close_path()
    linear_grad(ctx, 29, 24, 71, 76, [(0.0, "#FB542B"), (1.0, "#DC2626")])
    ctx.fill()
    ctx.move_to(41, 43)
    ctx.line_to(50, 38)
    ctx.line_to(59, 43)
    ctx.line_to(50, 62)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_duckduckgo(ctx: cairo.Context):
    ctx.arc(50, 50, 26, 0, 2 * math.pi)
    set_hex(ctx, "#DE5833")
    ctx.fill()
    ctx.arc(48, 49, 13, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    rounded_rect(ctx, 48, 50, 14, 7, 3.5)
    set_hex(ctx, "#FBBF24")
    ctx.fill()


def sym_termux(ctx: cairo.Context):
    rounded_rect(ctx, 24, 28, 52, 44, 8)
    set_hex(ctx, "#27272A")
    ctx.fill()
    ctx.set_line_width(3.6)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#4ADE80")
    ctx.move_to(34, 42)
    ctx.line_to(44, 50)
    ctx.line_to(34, 58)
    ctx.stroke()
    ctx.move_to(49, 58)
    ctx.line_to(63, 58)
    ctx.stroke()


def sym_home_assistant(ctx: cairo.Context):
    ctx.move_to(50, 24)
    ctx.line_to(74, 46)
    ctx.line_to(68, 46)
    ctx.line_to(68, 72)
    ctx.line_to(32, 72)
    ctx.line_to(32, 46)
    ctx.line_to(26, 46)
    ctx.close_path()
    set_hex(ctx, "#18BCF2")
    ctx.fill()
    ctx.arc(50, 56, 4, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


# --- China Apps ---

def sym_wechat(ctx: cairo.Context):
    set_hex(ctx, "#07C160")
    ctx.arc(43, 45, 17, 0, 2 * math.pi)
    ctx.fill()
    ctx.move_to(31, 58)
    ctx.line_to(27, 67)
    ctx.line_to(39, 61)
    ctx.close_path()
    ctx.fill()
    set_hex(ctx, "#10B981")
    ctx.arc(59, 55, 14, 0, 2 * math.pi)
    ctx.fill()
    set_hex(ctx, "#09090B")
    ctx.arc(38, 43, 2.3, 0, 2 * math.pi)
    ctx.arc(48, 43, 2.3, 0, 2 * math.pi)
    ctx.arc(55, 53, 2.0, 0, 2 * math.pi)
    ctx.arc(63, 53, 2.0, 0, 2 * math.pi)
    ctx.fill()


def sym_alipay(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 12)
    set_hex(ctx, "#1677FF")
    ctx.fill()
    # Geometric stylized '支' / Z-cross strokes (font-independent)
    ctx.set_line_width(3.6)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    ctx.set_line_join(cairo.LINE_JOIN_ROUND)
    set_hex(ctx, "#FFFFFF")
    ctx.move_to(50, 33)
    ctx.line_to(50, 44)
    ctx.move_to(35, 39)
    ctx.line_to(65, 39)
    ctx.move_to(38, 46)
    ctx.line_to(62, 46)
    ctx.curve_to(57, 56, 46, 62, 35, 65)
    ctx.move_to(41, 49)
    ctx.curve_to(49, 57, 58, 62, 67, 65)
    ctx.stroke()


def sym_weibo(ctx: cairo.Context):
    ctx.arc(46, 56, 17, 0, 2 * math.pi)
    set_hex(ctx, "#E6162D")
    ctx.fill()
    ctx.arc(46, 56, 9, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    ctx.arc(47, 56, 4.5, 0, 2 * math.pi)
    set_hex(ctx, "#18181B")
    ctx.fill()
    ctx.set_line_width(3.0)
    set_hex(ctx, "#FF9F0A")
    ctx.arc(60, 39, 10, -math.pi * 0.45, 0)
    ctx.stroke()
    ctx.arc(60, 39, 16, -math.pi * 0.45, 0)
    ctx.stroke()


def sym_taobao(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 13)
    linear_grad(ctx, 25, 25, 75, 75, [(0.0, "#FF9000"), (1.0, "#FF5000")])
    ctx.fill()
    # Geometric Taobao-inspired shopping bag + swirl mark
    ctx.set_line_width(3.6)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FFFFFF")
    ctx.arc(50, 39, 7.5, math.pi, 2 * math.pi)
    ctx.stroke()
    rounded_rect(ctx, 35, 40, 30, 24, 6)
    ctx.stroke()
    ctx.arc(50, 52, 5.5, 0.2 * math.pi, 1.75 * math.pi)
    ctx.stroke()


def sym_baidu_maps(ctx: cairo.Context):
    rounded_rect(ctx, 24, 24, 52, 52, 13)
    set_hex(ctx, "#3385FF")
    ctx.fill()
    ctx.arc(50, 44, 10, 0, 2 * math.pi)
    set_hex(ctx, "#EF4444")
    ctx.fill()
    ctx.arc(50, 44, 4, 0, 2 * math.pi)
    set_hex(ctx, "#FFFFFF")
    ctx.fill()
    ctx.set_line_width(3.2)
    set_hex(ctx, "#4ADE80")
    ctx.move_to(32, 65)
    ctx.line_to(68, 57)
    ctx.stroke()


def sym_xiaohongshu(ctx: cairo.Context):
    rounded_rect(ctx, 24, 29, 52, 42, 10)
    set_hex(ctx, "#FF2442")
    ctx.fill()
    draw_text_centered(ctx, "RED", 50, 50, 18, "#FFFFFF")


def sym_bilibili(ctx: cairo.Context):
    ctx.set_line_width(3.2)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FB7299")
    ctx.move_to(38, 25)
    ctx.line_to(45, 33)
    ctx.move_to(62, 25)
    ctx.line_to(55, 33)
    ctx.stroke()
    rounded_rect(ctx, 24, 33, 52, 39, 9)
    ctx.set_line_width(3.6)
    ctx.stroke()
    ctx.move_to(37, 48)
    ctx.line_to(43, 52)
    ctx.move_to(63, 48)
    ctx.line_to(57, 52)
    ctx.stroke()


def sym_qq(ctx: cairo.Context):
    # Stylized penguin badge
    ctx.arc(50, 46, 17, 0, 2 * math.pi)
    set_hex(ctx, "#F4F4F5")
    ctx.fill()
    ctx.arc(50, 61, 15, 0, 2 * math.pi)
    ctx.fill()
    set_hex(ctx, "#EF4444")
    rounded_rect(ctx, 33, 50, 34, 7, 3.5)
    ctx.fill()
    set_hex(ctx, "#F59E0B")
    rounded_rect(ctx, 43, 44, 14, 6, 3)
    ctx.fill()
    set_hex(ctx, "#18181B")
    ctx.arc(44, 39, 2.5, 0, 2 * math.pi)
    ctx.arc(56, 39, 2.5, 0, 2 * math.pi)
    ctx.fill()


def sym_jd(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 13)
    set_hex(ctx, "#E1251B")
    ctx.fill()
    draw_text_centered(ctx, "JD", 50, 51, 24, "#FFFFFF")


def sym_meituan(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 13)
    set_hex(ctx, "#FFC300")
    ctx.fill()
    # Geometric kangaroo/ear silhouette in dark
    set_hex(ctx, "#18181B")
    ctx.arc(50, 53, 13, 0, 2 * math.pi)
    ctx.fill()
    ctx.move_to(38, 46)
    ctx.line_to(37, 34)
    ctx.line_to(47, 41)
    ctx.close_path()
    ctx.fill()
    ctx.move_to(62, 46)
    ctx.line_to(63, 34)
    ctx.line_to(53, 41)
    ctx.close_path()
    ctx.fill()
    set_hex(ctx, "#FFC300")
    ctx.arc(45, 52, 2.2, 0, 2 * math.pi)
    ctx.arc(55, 52, 2.2, 0, 2 * math.pi)
    ctx.fill()


def sym_didi(ctx: cairo.Context):
    ctx.set_line_width(6.0)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FF7E00")
    ctx.arc(50, 51, 19, 0, math.pi)
    ctx.stroke()
    ctx.move_to(31, 44)
    ctx.line_to(69, 44)
    ctx.stroke()


def sym_amap(ctx: cairo.Context):
    ctx.move_to(50, 24)
    ctx.line_to(72, 71)
    ctx.line_to(50, 60)
    ctx.line_to(28, 71)
    ctx.close_path()
    linear_grad(ctx, 28, 24, 72, 71, [(0.0, "#38BDF8"), (1.0, "#2563EB")])
    ctx.fill()


def sym_netease_music(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#C20C0C")
    ctx.fill()
    ctx.set_line_width(4.2)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    set_hex(ctx, "#FFFFFF")
    ctx.arc(50, 53, 12, 0, math.pi * 1.45)
    ctx.stroke()
    ctx.move_to(50, 41)
    ctx.line_to(50, 33)
    ctx.line_to(60, 35)
    ctx.stroke()


def sym_qq_music(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#31C27C")
    ctx.fill()
    ctx.arc(46, 58, 7, 0, 2 * math.pi)
    set_hex(ctx, "#FACC15")
    ctx.fill()
    ctx.set_line_width(4.5)
    set_hex(ctx, "#FACC15")
    ctx.move_to(51, 58)
    ctx.line_to(51, 33)
    ctx.line_to(63, 37)
    ctx.stroke()


def sym_tripcom(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#287DFA")
    ctx.fill()
    ctx.set_line_width(4.0)
    set_hex(ctx, "#FFFFFF")
    ctx.move_to(35, 50)
    ctx.line_to(65, 39)
    ctx.line_to(54, 65)
    ctx.line_to(48, 52)
    ctx.close_path()
    ctx.fill()


def sym_pinduoduo(ctx: cairo.Context):
    rounded_rect(ctx, 25, 25, 50, 50, 14)
    set_hex(ctx, "#E02E24")
    ctx.fill()
    # Geometric twin-ring / shared heart badge
    ctx.set_line_width(3.6)
    set_hex(ctx, "#FFFFFF")
    ctx.arc(43, 48, 8.5, 0, 2 * math.pi)
    ctx.stroke()
    ctx.arc(57, 48, 8.5, 0, 2 * math.pi)
    ctx.stroke()
    ctx.arc(50, 57, 6.0, 0, math.pi)
    ctx.stroke()


def sym_feishu(ctx: cairo.Context):
    ctx.move_to(26, 40)
    ctx.line_to(56, 26)
    ctx.line_to(74, 46)
    ctx.line_to(44, 52)
    ctx.close_path()
    set_hex(ctx, "#00D6B9")
    ctx.fill()
    ctx.move_to(26, 40)
    ctx.line_to(44, 52)
    ctx.line_to(64, 72)
    ctx.line_to(32, 62)
    ctx.close_path()
    set_hex(ctx, "#3370FF")
    ctx.fill()


def sym_dingtalk(ctx: cairo.Context):
    ctx.arc(50, 50, 27, 0, 2 * math.pi)
    set_hex(ctx, "#0089FF")
    ctx.fill()
    ctx.move_to(34, 38)
    ctx.line_to(66, 46)
    ctx.line_to(50, 57)
    ctx.line_to(58, 69)
    ctx.line_to(39, 55)
    ctx.close_path()
    set_hex(ctx, "#FFFFFF")
    ctx.fill()


def sym_deepseek(ctx: cairo.Context):
    # DeepSeek electric blue whale-wave emblem
    ctx.arc(50, 50, 26, 0, 2 * math.pi)
    linear_grad(ctx, 24, 24, 76, 76, [(0.0, "#60A5FA"), (1.0, "#2563EB")])
    ctx.fill()
    set_hex(ctx, "#FFFFFF")
    ctx.arc(47, 51, 12, math.pi * 0.85, math.pi * 2.1)
    ctx.fill()
    ctx.move_to(58, 49)
    ctx.line_to(68, 42)
    ctx.line_to(66, 56)
    ctx.close_path()
    ctx.fill()


def sym_ios27_widgets(ctx: cairo.Context):
    # Mini 4-tile dark widget mosaic in Apple accent colors
    tiles = [
        (26, 26, 22, 22, "#0A84FF"),
        (52, 26, 22, 22, "#FF3B30"),
        (26, 52, 22, 22, "#30D158"),
        (52, 52, 22, 22, "#FF9F0A"),
    ]
    for x, y, w, h, col in tiles:
        rounded_rect(ctx, x, y, w, h, 6)
        set_hex(ctx, col)
        ctx.fill()


SYMBOL_REGISTRY = {
    "camera": sym_camera,
    "photos": sym_photos,
    "contacts": sym_contacts,
    "phone": sym_phone,
    "messages": sym_messages,
    "clock": sym_clock,
    "calculator": sym_calculator,
    "files": sym_files,
    "appmarket": sym_appmarket,
    "settings": sym_settings,
    "calendar": sym_calendar,
    "notes": sym_notes,
    "health": sym_health,
    "id_shield": sym_id_shield,
    "safari_compass": sym_safari_compass,
    "mail": sym_mail,
    "music": sym_music,
    "video_player": sym_video_player,
    "weather": sym_weather,
    "compass": sym_compass,
    "voice_memo": sym_voice_memo,
    "wallet": sym_wallet,
    "themes": sym_themes,
    "shield_boost": sym_shield_boost,
    "tips": sym_tips,
    "search": sym_search,
    "play_store": sym_play_store,
    "whatsapp": sym_whatsapp,
    "whatsapp_business": sym_whatsapp_business,
    "telegram": sym_telegram,
    "signal": sym_signal,
    "instagram": sym_instagram,
    "tiktok": sym_tiktok,
    "x_twitter": sym_x_twitter,
    "youtube": sym_youtube,
    "youtube_music": sym_youtube_music,
    "spotify": sym_spotify,
    "chrome": sym_chrome,
    "gmail": sym_gmail,
    "google_maps": sym_google_maps,
    "trade_republic": sym_trade_republic,
    "paypal": sym_paypal,
    "sparkasse": sym_sparkasse,
    "n26": sym_n26,
    "amazon": sym_amazon,
    "netflix": sym_netflix,
    "chatgpt": sym_chatgpt,
    "discord": sym_discord,
    "reddit": sym_reddit,
    "github": sym_github,
    "google_g": sym_google_g,
    "google_photos": sym_google_photos,
    "google_drive": sym_google_drive,
    "google_calendar": sym_google_calendar,
    "translate": sym_translate,
    "keep": sym_keep,
    "authenticator": sym_authenticator,
    "meet": sym_meet,
    "apple_music": sym_apple_music,
    "shazam": sym_shazam,
    "soundcloud": sym_soundcloud,
    "vlc": sym_vlc,
    "twitch": sym_twitch,
    "snapchat": sym_snapchat,
    "facebook": sym_facebook,
    "fb_messenger": sym_fb_messenger,
    "linkedin": sym_linkedin,
    "pinterest": sym_pinterest,
    "threads": sym_threads,
    "bluesky": sym_bluesky,
    "claude": sym_claude,
    "gemini": sym_gemini,
    "perplexity": sym_perplexity,
    "deepl": sym_deepl,
    "protonmail": sym_protonmail,
    "protonvpn": sym_protonvpn,
    "bitwarden": sym_bitwarden,
    "onepassword": sym_onepassword,
    "ms_teams": sym_ms_teams,
    "outlook": sym_outlook,
    "onedrive": sym_onedrive,
    "notion": sym_notion,
    "obsidian": sym_obsidian,
    "todoist": sym_todoist,
    "slack": sym_slack,
    "zoom": sym_zoom,
    "duolingo": sym_duolingo,
    "kindle": sym_kindle,
    "audible": sym_audible,
    "disney_plus": sym_disney_plus,
    "prime_video": sym_prime_video,
    "steam": sym_steam,
    "playstation": sym_playstation,
    "xbox": sym_xbox,
    "uber": sym_uber,
    "airbnb": sym_airbnb,
    "booking": sym_booking,
    "db_navigator": sym_db_navigator,
    "dhl": sym_dhl,
    "kleinanzeigen": sym_kleinanzeigen,
    "lieferando": sym_lieferando,
    "revolut": sym_revolut,
    "wise": sym_wise,
    "ing": sym_ing,
    "dkb": sym_dkb,
    "klarna": sym_klarna,
    "komoot": sym_komoot,
    "strava": sym_strava,
    "garmin": sym_garmin,
    "firefox": sym_firefox,
    "brave": sym_brave,
    "duckduckgo": sym_duckduckgo,
    "termux": sym_termux,
    "home_assistant": sym_home_assistant,
    "wechat": sym_wechat,
    "alipay": sym_alipay,
    "weibo": sym_weibo,
    "taobao": sym_taobao,
    "baidu_maps": sym_baidu_maps,
    "xiaohongshu": sym_xiaohongshu,
    "bilibili": sym_bilibili,
    "qq": sym_qq,
    "jd": sym_jd,
    "meituan": sym_meituan,
    "didi": sym_didi,
    "amap": sym_amap,
    "netease_music": sym_netease_music,
    "qq_music": sym_qq_music,
    "tripcom": sym_tripcom,
    "pinduoduo": sym_pinduoduo,
    "feishu": sym_feishu,
    "dingtalk": sym_dingtalk,
    "deepseek": sym_deepseek,
    "ios27_widgets": sym_ios27_widgets,
}
