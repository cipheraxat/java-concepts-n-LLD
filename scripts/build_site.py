#!/usr/bin/env python3
"""Generate static HTML site from Java lesson files for GitHub Pages."""

from __future__ import annotations

import html
import re
import shutil
from pathlib import Path

import markdown
from pygments import highlight
from pygments.formatters import HtmlFormatter
from pygments.lexers import JavaLexer

ROOT = Path(__file__).resolve().parent.parent
LESSONS = ROOT / "lessons"
DOCS = ROOT / "docs"

TOPIC_ORDER = [
    ("oop_concepts", "01 — OOP Concepts"),
    ("collections_framework", "02 — Collections Framework"),
    ("generics", "03 — Generics"),
    ("exception_handling", "04 — Exception Handling"),
    ("multithreading_concurrency", "05 — Multithreading & Concurrency"),
    ("java8_features", "06 — Java 8+ Features"),
    ("design_patterns", "07 — Design Patterns"),
    ("jvm_internals", "08 — JVM Internals"),
    ("string_handling", "09 — String Handling"),
    ("solid_principles", "10 — SOLID Principles"),
    ("access_modifiers", "11 — Access Modifiers"),
    ("static_and_final", "12 — static & final"),
    ("inner_classes", "13 — Inner Classes"),
    ("wrapper_classes", "14 — Wrapper Classes"),
]

FORMATTER = HtmlFormatter(
    linenos="table",
    hl_lines="",
    cssclass="highlight",
    anchorlinenos=False,
    wrapcode=True,
)


def slugify(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def rel_path(from_file: Path, to_file: Path) -> str:
    return Path(
        Path(*[".."] * len(from_file.parent.relative_to(DOCS).parts)),
        to_file.relative_to(DOCS),
    ).as_posix()


def highlight_java(source: str) -> str:
    return highlight(source, JavaLexer(), FORMATTER)


def render_markdown(text: str) -> str:
    return markdown.markdown(
        text,
        extensions=["tables", "fenced_code", "toc"],
    )


def page_shell(
    title: str,
    body: str,
    nav_html: str,
    depth: int = 0,
    subtitle: str = "",
) -> str:
    prefix = "../" * depth
    subtitle_html = f'<p class="page-subtitle">{html.escape(subtitle)}</p>' if subtitle else ""
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{html.escape(title)} — Java SDE2 Lessons</title>
  <link rel="stylesheet" href="{prefix}assets/style.css">
  <link rel="stylesheet" href="{prefix}assets/pygments.css">
</head>
<body>
  <button class="nav-toggle" aria-label="Toggle navigation" onclick="document.body.classList.toggle('nav-open')">☰</button>
  <div class="layout">
    <nav class="sidebar">{nav_html}</nav>
    <main class="content">
      <header class="page-header">
        <h1>{html.escape(title)}</h1>
        {subtitle_html}
      </header>
      {body}
      <footer class="page-footer">
        <a href="{prefix}index.html">← Back to index</a>
      </footer>
    </main>
  </div>
  <script>
    document.querySelectorAll('pre code').forEach(function(block) {{
      block.setAttribute('tabindex', '0');
    }});
  </script>
</body>
</html>"""


def build_nav(current: Path | None = None) -> str:
    lines = ['<div class="nav-brand"><a href="index.html">Java SDE2</a></div>', '<ul class="nav-list">']
    for folder, label in TOPIC_ORDER:
        topic_dir = LESSONS / folder
        if not topic_dir.is_dir():
            continue
        topic_slug = slugify(folder)
        topic_href = f"topics/{topic_slug}.html"
        active_topic = current and f"topics/{topic_slug}.html" in str(current)
        lines.append(
            f'<li class="nav-topic{" active" if active_topic else ""}">'
            f'<a href="{topic_href}">{html.escape(label)}</a>'
        )
        java_files = sorted(topic_dir.glob("*.java"))
        if java_files:
            lines.append('<ul class="nav-lessons">')
            for java_file in java_files:
                lesson_href = f"lessons/{topic_slug}/{java_file.stem}.html"
                active = (
                    current is not None
                    and current.name == f"{java_file.stem}.html"
                    and current.parent.name == topic_slug
                )
                cls = ' class="active"' if active else ""
                lines.append(
                    f'<li{cls}>'
                    f'<a href="{lesson_href}">{html.escape(java_file.stem)}</a></li>'
                )
            lines.append("</ul>")
        lines.append("</li>")
    lines.append("</ul>")
    return "\n".join(lines)


def depth_for(path: Path) -> int:
    return len(path.relative_to(DOCS).parts) - 1


def prefixed_nav(nav: str, depth: int) -> str:
    if depth == 0:
        return nav
    prefix = "../" * depth
    return re.sub(r'href="(?!https?://|#)', f'href="{prefix}', nav)


def write_topic_pages(nav: str) -> None:
    topics_dir = DOCS / "topics"
    topics_dir.mkdir(parents=True, exist_ok=True)

    for folder, label in TOPIC_ORDER:
        topic_dir = LESSONS / folder
        if not topic_dir.is_dir():
            continue
        readme = topic_dir / "README.md"
        readme_html = ""
        if readme.exists():
            readme_html = f'<section class="readme">{render_markdown(readme.read_text())}</section>'

        java_files = sorted(topic_dir.glob("*.java"))
        cards = ['<section class="lesson-cards"><h2>Source Files</h2><div class="card-grid">']
        topic_slug = slugify(folder)
        for java_file in java_files:
            lesson_path = f"../lessons/{topic_slug}/{java_file.stem}.html"
            cards.append(
                f'<a class="lesson-card" href="{lesson_path}">'
                f'<span class="card-title">{html.escape(java_file.stem)}</span>'
                f'<span class="card-meta">.java</span></a>'
            )
        cards.append("</div></section>")
        body = readme_html + "\n".join(cards)

        out = topics_dir / f"{topic_slug}.html"
        depth = depth_for(out)
        page = page_shell(label, body, prefixed_nav(nav, depth), depth, folder)
        out.write_text(page, encoding="utf-8")


def write_lesson_pages(nav: str) -> list[tuple[str, str, str]]:
    written: list[tuple[str, str, str]] = []

    for folder, label in TOPIC_ORDER:
        topic_dir = LESSONS / folder
        if not topic_dir.is_dir():
            continue
        topic_slug = slugify(folder)
        out_dir = DOCS / "lessons" / topic_slug
        out_dir.mkdir(parents=True, exist_ok=True)

        for java_file in sorted(topic_dir.glob("*.java")):
            source = java_file.read_text(encoding="utf-8")
            highlighted = highlight_java(source)
            body = (
                '<section class="code-section">'
                f'<div class="code-meta"><span>{html.escape(java_file.name)}</span>'
                f'<button type="button" onclick="navigator.clipboard.writeText(document.querySelector(\'pre code\').innerText)">Copy</button>'
                "</div>"
                f"{highlighted}"
                "</section>"
            )
            out = out_dir / f"{java_file.stem}.html"
            depth = depth_for(out)
            page = page_shell(
                java_file.stem,
                body,
                prefixed_nav(nav, depth),
                depth,
                f"{label} · {java_file.name}",
            )
            out.write_text(page, encoding="utf-8")
            written.append((label, java_file.stem, f"lessons/{topic_slug}/{java_file.stem}.html"))

    return written


def write_index(nav: str, lessons: list[tuple[str, str, str]]) -> None:
    main_readme = LESSONS / "README.md"
    intro = ""
    if main_readme.exists():
        intro = f'<section class="readme">{render_markdown(main_readme.read_text())}</section>'

    cards = ['<section class="topic-grid"><h2>All Topics</h2><div class="card-grid">']
    for folder, label in TOPIC_ORDER:
        if not (LESSONS / folder).is_dir():
            continue
        topic_slug = slugify(folder)
        count = len(list((LESSONS / folder).glob("*.java")))
        cards.append(
            f'<a class="topic-card" href="topics/{topic_slug}.html">'
            f'<span class="card-title">{html.escape(label)}</span>'
            f'<span class="card-meta">{count} file{"s" if count != 1 else ""}</span></a>'
        )
    cards.append("</div></section>")

    body = intro + "\n".join(cards)
    page = page_shell("Core Java — SDE2 Interview Prep", body, nav, 0, "Syntax-highlighted lesson source")
    (DOCS / "index.html").write_text(page, encoding="utf-8")


def copy_assets() -> None:
    assets = DOCS / "assets"
    assets.mkdir(parents=True, exist_ok=True)
    style_src = ROOT / "scripts" / "assets" / "style.css"
    if style_src.exists():
        (assets / "style.css").write_text(style_src.read_text(encoding="utf-8"), encoding="utf-8")
    (assets / "pygments.css").write_text(FORMATTER.get_style_defs(".highlight"), encoding="utf-8")


def main() -> None:
    if DOCS.exists():
        shutil.rmtree(DOCS)
    DOCS.mkdir(parents=True)

    copy_assets()
    (DOCS / ".nojekyll").touch()
    nav = build_nav()
    lessons = write_lesson_pages(nav)
    write_topic_pages(nav)
    write_index(nav, lessons)
    print(f"Generated {len(lessons)} lesson pages in {DOCS}")


if __name__ == "__main__":
    main()
