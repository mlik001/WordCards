"""生成 WordCards 的桌面图标（各密度 PNG + 预览图）。

为什么用脚本而不是直接放设计师给的 PNG：
1. 图标是几何图形，脚本生成可复现、可改色、可改尺寸；
2. 自适应图标（API 26+）的矢量层在 drawable/*.xml 里，这里的 PNG 只作为
   API 24~25 的兜底与商店/文档预览，两套必须视觉一致，脚本能保证这一点。

用法：
    python tools/make_launcher_icons.py
"""

from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
DOCS = ROOT / "docs"

# 品牌色，与 ui/theme/Color.kt、drawable/ic_launcher_background.xml 保持一致
BLUE_START = (82, 109, 255)   # #526DFF
BLUE_END = (46, 61, 168)      # #2E3DA8
BRAND_BLUE = (67, 97, 238)    # #4361EE
GREEN = (16, 185, 129)        # #10B981
WHITE = (255, 255, 255)
SHADOW = (0, 0, 0, 26)

UNIT = 108          # 设计稿坐标空间：与矢量 108dp viewport 一致
SS = 6              # 超采样倍数，最后缩回目标尺寸做抗锯齿
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def u(value: float) -> float:
    """设计稿单位 → 超采样像素。"""
    return value * SS


def gradient_square(size: int) -> Image.Image:
    """沿左上→右下做线性渐变，和矢量里的 gradient 参数一一对应。"""
    img = Image.new("RGB", (size, size))
    draw = ImageDraw.Draw(img)
    for i in range(size * 2):
        t = i / (size * 2 - 1)
        color = tuple(
            round(BLUE_START[c] + (BLUE_END[c] - BLUE_START[c]) * t) for c in range(3)
        )
        # 逐条 45° 对角线填充
        draw.line([(i, 0), (0, i)], fill=color)
        draw.line([(size, i - size), (i - size, size)], fill=color)
    return img


def draw_glyph(img: Image.Image) -> None:
    """在渐变底上画「单词卡 + 对勾」。坐标与 drawable/ic_launcher_foreground.xml 对应。"""
    # 阴影层（自适应图标里没有 elevation，只能自己画一层）
    shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [u(39), u(39), u(75), u(73)], radius=u(6), fill=SHADOW
    )
    img.alpha_composite(shadow)

    draw = ImageDraw.Draw(img)
    # 卡片
    draw.rounded_rectangle([u(33), u(36), u(75), u(70)], radius=u(6), fill=WHITE)

    # 三条释义线（由长到短）
    for y, x_end in ((46.5, 68), (54, 61), (61.5, 51)):
        draw.line(
            [(u(41), u(y)), (u(x_end), u(y))],
            fill=BRAND_BLUE,
            width=int(u(3)),
        )

    # 掌握标记：绿色圆 + 白勾
    r = u(9)
    cx, cy = u(70), u(59.5)
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=GREEN)
    draw.line(
        [(u(66.2), u(59.6)), (u(69), u(62.6)), (u(73.9), u(56.6))],
        fill=WHITE,
        width=int(u(2.4)),
        joint="curve",
    )


def render(mask: str) -> Image.Image:
    """渲染一张超采样母图，按 mask 裁成圆角方形或圆形。"""
    size = UNIT * SS
    base = gradient_square(size).convert("RGBA")
    draw_glyph(base)

    mask_img = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask_img)
    if mask == "circle":
        mask_draw.ellipse([0, 0, size - 1, size - 1], fill=255)
    else:
        # 旧版图标惯例：约 22% 圆角，接近 Android 启动器的方形遮罩
        mask_draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.22), fill=255)

    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(base, (0, 0), mask_img)
    return out


def main() -> None:
    square = render("rounded")
    circle = render("circle")

    for density, px in SIZES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        square.resize((px, px), Image.LANCZOS).save(folder / "ic_launcher.png")
        circle.resize((px, px), Image.LANCZOS).save(folder / "ic_launcher_round.png")
        print(f"{folder.name}: {px}x{px}")

    DOCS.mkdir(exist_ok=True)
    square.resize((512, 512), Image.LANCZOS).save(DOCS / "app-icon-512.png")
    print("docs/app-icon-512.png: 512x512")


if __name__ == "__main__":
    main()
