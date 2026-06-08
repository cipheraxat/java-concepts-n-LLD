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
    style="monokai",
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


def breadcrumbs_html(crumbs: list[tuple[str, str | None]], prefix: str) -> str:
    parts = ['<nav class="breadcrumbs" aria-label="Breadcrumb">']
    for i, (label, href) in enumerate(crumbs):
        if i > 0:
            parts.append('<span class="sep">/</span>')
        if href:
            parts.append(f'<a href="{prefix}{href}">{html.escape(label)}</a>')
        else:
            parts.append(f'<span class="current">{html.escape(label)}</span>')
    parts.append("</nav>")
    return "".join(parts)


def page_shell(
    title: str,
    body: str,
    nav_html: str,
    depth: int = 0,
    subtitle: str = "",
    crumbs: list[tuple[str, str | None]] | None = None,
    hero: str = "",
) -> str:
    prefix = "../" * depth
    subtitle_html = f'<p class="page-subtitle">{html.escape(subtitle)}</p>' if subtitle else ""
    crumbs_html = breadcrumbs_html(crumbs, prefix) if crumbs else ""
    header_html = hero or (
        f'<header class="page-header"><h1>{html.escape(title)}</h1>{subtitle_html}</header>'
    )
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{html.escape(title)} — Java SDE2 Lessons</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700;1,9..40,400&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="{prefix}assets/style.css">
  <link rel="stylesheet" href="{prefix}assets/pygments.css">
</head>
<body>
  <header class="topbar">
    <button class="nav-toggle" aria-label="Toggle navigation" onclick="document.body.classList.toggle('nav-open')">☰</button>
    <a class="topbar-brand" href="{prefix}index.html">
      <span class="brand-icon">Jv</span>
      Java SDE2 Prep
    </a>
    <div class="topbar-actions">
      <a class="topbar-link" href="https://github.com/cipheraxat/java-concepts-n-LLD" target="_blank" rel="noopener">GitHub ↗</a>
    </div>
  </header>
  <div class="nav-overlay" onclick="document.body.classList.remove('nav-open')"></div>
  <div class="layout">
    <nav class="sidebar">{nav_html}</nav>
    <main class="content">
      {crumbs_html}
      {header_html}
      {body}
      <footer class="page-footer">
        <a href="{prefix}index.html">← All lessons</a>
        <span>32 demos · 14 topics</span>
      </footer>
    </main>
  </div>
  <div class="toast" id="toast">Copied!</div>
  <script>
    function copyCode(btn) {{
      var code = document.querySelector('.highlight pre');
      if (!code) return;
      navigator.clipboard.writeText(code.innerText).then(function() {{
        btn.textContent = 'Copied!';
        btn.classList.add('copied');
        var t = document.getElementById('toast');
        t.classList.add('show');
        setTimeout(function() {{
          btn.textContent = 'Copy';
          btn.classList.remove('copied');
          t.classList.remove('show');
        }}, 2000);
      }});
    }}
    document.querySelectorAll('pre code').forEach(function(block) {{
      block.setAttribute('tabindex', '0');
    }});
  </script>
</body>
</html>"""


def build_nav(current: Path | None = None) -> str:
    lines = ['<p class="sidebar-label">Lessons</p>', '<ul class="nav-list">']
    for folder, label in TOPIC_ORDER:
        topic_dir = LESSONS / folder
        if not topic_dir.is_dir():
            continue
        topic_slug = slugify(folder)
        topic_href = f"topics/{topic_slug}.html"
        topic_num = label.split("—")[0].strip() if "—" in label else ""
        topic_name = label.split("—", 1)[1].strip() if "—" in label else label
        java_files = sorted(topic_dir.glob("*.java"))

        has_active_lesson = any(
            current is not None
            and current.name == f"{jf.stem}.html"
            and current.parent.name == topic_slug
            for jf in java_files
        )
        active_topic = current and (
            f"topics/{topic_slug}.html" in str(current) or has_active_lesson
        )
        open_attr = " open" if active_topic else ""

        lines.append(f'<li class="nav-topic{" active" if active_topic else ""}">')
        lines.append(f'<details{open_attr}>')
        lines.append(
            f'<summary>'
            f'<span class="topic-num">{html.escape(topic_num)}</span>'
            f'{html.escape(topic_name)}'
            f'<a class="topic-link" href="{topic_href}" onclick="event.stopPropagation()">overview</a>'
            f'</summary>'
        )
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
        lines.append("</details></li>")
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
        topic_slug = slugify(folder)
        cards = ['<section class="lesson-cards"><p class="section-title">Source Files</p><div class="card-grid">']
        for java_file in java_files:
            lesson_path = f"../lessons/{topic_slug}/{java_file.stem}.html"
            cards.append(
                f'<a class="lesson-card" href="{lesson_path}">'
                f'<span class="card-title">{html.escape(java_file.stem)}</span>'
                f'<span class="card-meta">{html.escape(java_file.name)}</span></a>'
            )
        cards.append("</div></section>")
        body = readme_html + "\n".join(cards)

        out = topics_dir / f"{topic_slug}.html"
        depth = depth_for(out)
        crumbs = [
            ("Home", "index.html"),
            (topic_name := (label.split("—", 1)[1].strip() if "—" in label else label), None),
        ]
        page = page_shell(
            label,
            body,
            prefixed_nav(nav, depth),
            depth,
            folder,
            crumbs=crumbs,
        )
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
                '<div class="code-window-bar">'
                '<div class="window-dots"><span></span><span></span><span></span></div>'
                f'<span class="code-filename">{html.escape(java_file.name)}</span>'
                '<div class="code-meta">'
                '<button type="button" onclick="copyCode(this)">Copy</button>'
                "</div></div>"
                f"{highlighted}"
                "</section>"
            )
            out = out_dir / f"{java_file.stem}.html"
            depth = depth_for(out)
            topic_name = label.split("—", 1)[1].strip() if "—" in label else label
            crumbs = [
                ("Home", "index.html"),
                (topic_name, f"topics/{topic_slug}.html"),
                (java_file.stem, None),
            ]
            page = page_shell(
                java_file.stem,
                body,
                prefixed_nav(nav, depth),
                depth,
                f"{label} · {java_file.name}",
                crumbs=crumbs,
            )
            out.write_text(page, encoding="utf-8")
            written.append((label, java_file.stem, f"lessons/{topic_slug}/{java_file.stem}.html"))

    return written


def write_index(nav: str, lessons: list[tuple[str, str, str]]) -> None:
    main_readme = LESSONS / "README.md"
    intro = ""
    if main_readme.exists():
        intro = f'<section class="readme">{render_markdown(main_readme.read_text())}</section>'

    topic_count = sum(
        1 for folder, _ in TOPIC_ORDER if (LESSONS / folder).is_dir()
    )
    lesson_count = len(lessons)

    hero = f"""<section class="hero">
  <span class="hero-badge">Interview Ready</span>
  <h1>Core Java for SDE2 Interviews</h1>
  <p>Theory, annotated code, and answers to the most common Java interview questions — all in one place.</p>
  <div class="hero-stats">
    <div class="hero-stat"><strong>{lesson_count}</strong><span>Code demos</span></div>
    <div class="hero-stat"><strong>{topic_count}</strong><span>Topics</span></div>
    <div class="hero-stat"><strong>SDE2</strong><span>Difficulty level</span></div>
  </div>
</section>"""

    cards = ['<section class="topic-grid"><p class="section-title">Browse Topics</p><div class="card-grid">']
    for folder, label in TOPIC_ORDER:
        if not (LESSONS / folder).is_dir():
            continue
        topic_slug = slugify(folder)
        count = len(list((LESSONS / folder).glob("*.java")))
        topic_num = label.split("—")[0].strip() if "—" in label else ""
        topic_name = label.split("—", 1)[1].strip() if "—" in label else label
        cards.append(
            f'<a class="topic-card" href="topics/{topic_slug}.html">'
            f'<span class="card-num">{html.escape(topic_num)}</span>'
            f'<span class="card-title">{html.escape(topic_name)}</span>'
            f'<span class="card-meta">{count} demo{"s" if count != 1 else ""}</span></a>'
        )
    cards.append("</div></section>")

    body = intro + "\n".join(cards)
    page = page_shell("Core Java — SDE2 Interview Prep", body, nav, 0, hero=hero)
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
