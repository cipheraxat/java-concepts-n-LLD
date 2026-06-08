#!/usr/bin/env python3
"""Generate static HTML site from Java lesson files for GitHub Pages."""

from __future__ import annotations

import html
import re
import shutil
from dataclasses import dataclass, field
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
    style="native",
    linenos="table",
    cssclass="highlight",
    anchorlinenos=False,
    wrapcode=True,
)

SECTION_RE = re.compile(r"^\s*//\s*─+\s*(\d+)\.\s*(.+?)\s*─+", re.MULTILINE)


@dataclass
class LessonMeta:
    title: str = ""
    intro_lines: list[str] = field(default_factory=list)
    interview_questions: list[str] = field(default_factory=list)


@dataclass
class LessonSection:
    number: int
    title: str
    concept: str
    code: str
    slug: str


def slugify(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


TOPIC_NAMES = {folder: label.split("—", 1)[1].strip() if "—" in label else label for folder, label in TOPIC_ORDER}
TOPIC_SLUGS = {folder: slugify(folder) for folder, _ in TOPIC_ORDER}

STUDY_PATH = [
    ("Week 1", ["oop_concepts", "collections_framework", "string_handling", "exception_handling"]),
    ("Week 2", ["java8_features", "generics", "solid_principles"]),
    ("Week 3", ["multithreading_concurrency", "design_patterns", "jvm_internals"]),
]


def topic_parts(label: str) -> tuple[str, str]:
    if "—" in label:
        num, name = label.split("—", 1)
        return num.strip(), name.strip()
    return "", label


def clean_comment_block(text: str) -> str:
    lines = []
    for line in text.splitlines():
        line = re.sub(r"^\s*\*\s?", "", line)
        lines.append(line.rstrip())
    return "\n".join(lines).strip()


def parse_javadoc(source: str) -> LessonMeta:
    meta = LessonMeta()
    match = re.search(r"/\*\*(.*?)\*/", source, re.DOTALL)
    if not match:
        return meta

    raw = clean_comment_block(match.group(1))
    lines = [ln.strip() for ln in raw.splitlines()]

    if lines:
        meta.title = lines[0]

    in_interview = False
    for line in lines[1:]:
        if not line:
            continue
        lower = line.lower()
        if "interview" in lower and ("sde2" in lower or "question" in lower):
            in_interview = True
            continue
        if in_interview:
            if line.startswith("=>"):
                if meta.interview_questions:
                    meta.interview_questions[-1] += " =>" + line[2:].strip()
                continue
            if line.startswith("-") or line.startswith("•") or "?" in line:
                q = line.lstrip("-• ").strip()
                if q:
                    meta.interview_questions.append(q)
            elif line[0].isdigit() and "." in line[:4]:
                in_interview = False
                meta.intro_lines.append(line)
        else:
            if not line.startswith("LESSON") or len(meta.intro_lines) > 0:
                meta.intro_lines.append(line)

    return meta


def extract_section_concept(section_lines: list[str]) -> str:
    text = "\n".join(section_lines)
    block = re.search(r"/\*(.*?)\*/", text, re.DOTALL)
    if block:
        return clean_comment_block(block.group(1))
    return ""


def parse_sections(source: str) -> list[LessonSection]:
    lines = source.splitlines()
    markers: list[tuple[int, int, str]] = []
    for i, line in enumerate(lines):
        m = SECTION_RE.match(line)
        if m:
            markers.append((i, int(m.group(1)), m.group(2).strip()))

    if not markers:
        return []

    sections: list[LessonSection] = []
    for idx, (start, num, title) in enumerate(markers):
        end = markers[idx + 1][0] if idx + 1 < len(markers) else len(lines)
        section_lines = lines[start:end]
        concept = extract_section_concept(section_lines)
        code = "\n".join(section_lines)
        sections.append(
            LessonSection(
                number=num,
                title=title,
                concept=concept,
                code=code,
                slug=f"section-{num}",
            )
        )
    return sections


def format_concept_html(text: str) -> str:
    if not text:
        return ""
    escaped = html.escape(text)
    paragraphs = [p.strip() for p in escaped.split("\n\n") if p.strip()]
    if not paragraphs:
        paragraphs = [escaped]
    return "".join(f'<p>{p.replace(chr(10), "<br>")}</p>' for p in paragraphs)


def format_interview_html(questions: list[str]) -> str:
    if not questions:
        return ""
    items = []
    for q in questions:
        escaped = html.escape(q)
        if "=>" in escaped:
            parts = escaped.split("=>", 1)
            items.append(
                f"<li><strong>{parts[0].strip()}</strong> "
                f'<span class="answer">→ {parts[1].strip()}</span></li>'
            )
        else:
            items.append(f"<li>{escaped}</li>")
    return (
        '<aside class="interview-callout">'
        '<div class="callout-label">◎ SDE2 Interview Focus</div>'
        f"<ul>{''.join(items)}</ul></aside>"
    )


def highlight_java(source: str) -> str:
    return highlight(source, JavaLexer(), FORMATTER)


def code_block(source: str, label: str = "Java") -> str:
    highlighted = highlight_java(source)
    return (
        '<div class="code-section">'
        '<div class="code-bar">'
        f'<span class="code-lang">{html.escape(label)}</span>'
        '<button type="button" onclick="copyCode(this)">Copy</button>'
        "</div>"
        f"{highlighted}</div>"
    )


def render_markdown(text: str) -> str:
    return markdown.markdown(text, extensions=["tables", "fenced_code"])


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
    has_toc: bool = False,
) -> str:
    prefix = "../" * depth
    subtitle_html = f'<p class="page-subtitle">{html.escape(subtitle)}</p>' if subtitle else ""
    crumbs_html = breadcrumbs_html(crumbs, prefix) if crumbs else ""
    header_html = hero or (
        f'<header class="page-header"><h1>{html.escape(title)}</h1>{subtitle_html}</header>'
    )
    content_cls = "content has-toc" if has_toc else "content"
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{html.escape(title)} — Java SDE2 Lessons</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500&family=Lora:ital,wght@0,400;0,500;0,600;1,400&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="{prefix}assets/style.css">
  <link rel="stylesheet" href="{prefix}assets/pygments.css">
</head>
<body>
  <header class="topbar">
    <button class="nav-toggle" aria-label="Toggle navigation" onclick="document.body.classList.toggle('nav-open')">☰</button>
    <a class="topbar-brand" href="{prefix}index.html">
      <span class="brand-mark">Jv</span>
      Java Lessons
    </a>
    <div class="topbar-spacer"></div>
    <a class="topbar-link" href="https://github.com/cipheraxat/java-concepts-n-LLD" target="_blank" rel="noopener">Source</a>
  </header>
  <div class="nav-overlay" onclick="document.body.classList.remove('nav-open')"></div>
  <div class="layout">
    <nav class="sidebar">{nav_html}</nav>
    <main class="{content_cls}">
      {crumbs_html}
      {header_html}
      {body}
      <footer class="page-footer">
        <a href="{prefix}index.html">← All lessons</a>
        <span>32 lessons · 14 topics</span>
      </footer>
    </main>
  </div>
  <div class="toast" id="toast">Copied to clipboard</div>
  <script src="{prefix}assets/app.js"></script>
</body>
</html>"""


def build_nav(current: Path | None = None) -> str:
    lines = [
        '<div class="progress-block">',
        '<div class="progress-label">Your progress</div>',
        '<div class="progress-bar"><div class="progress-fill" id="progress-fill"></div></div>',
        '<div class="progress-text" id="progress-text">0 / 32 explored</div>',
        "</div>",
        '<input type="search" class="nav-search" id="nav-search" placeholder="Search lessons…" aria-label="Search lessons">',
        '<p class="sidebar-label">Curriculum</p>',
        '<ul class="nav-list">',
    ]
    for folder, label in TOPIC_ORDER:
        topic_dir = LESSONS / folder
        if not topic_dir.is_dir():
            continue
        topic_slug = slugify(folder)
        topic_href = f"topics/{topic_slug}.html"
        topic_num, topic_name = topic_parts(label)
        java_files = sorted(topic_dir.glob("*.java"))

        has_active = any(
            current is not None
            and current.name == f"{jf.stem}.html"
            and current.parent.name == topic_slug
            for jf in java_files
        )
        active_topic = current and (
            f"topics/{topic_slug}.html" in str(current) or has_active
        )
        open_attr = " open" if active_topic else ""

        lines.append(f'<li class="nav-topic{" active" if active_topic else ""}">')
        lines.append(f"<details{open_attr}>")
        lines.append(
            f"<summary>"
            f'<span class="topic-num">{html.escape(topic_num)}</span>'
            f"{html.escape(topic_name)}"
            f'<a class="topic-link" href="{topic_href}" onclick="event.stopPropagation()">guide</a>'
            f"</summary>"
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
                    f"<li{cls}>"
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


def build_lesson_order() -> list[tuple[str, str, str, str]]:
    """Return (topic_slug, topic_label, lesson_stem, lesson_href_suffix)."""
    order: list[tuple[str, str, str, str]] = []
    for folder, label in TOPIC_ORDER:
        topic_dir = LESSONS / folder
        if not topic_dir.is_dir():
            continue
        topic_slug = slugify(folder)
        for java_file in sorted(topic_dir.glob("*.java")):
            order.append((
                topic_slug,
                label,
                java_file.stem,
                f"lessons/{topic_slug}/{java_file.stem}.html",
            ))
    return order


def lesson_nav_html(
    current_href: str, depth: int, order: list[tuple[str, str, str, str]]
) -> str:
    prefix = "../" * depth
    idx = next((i for i, o in enumerate(order) if o[3] == current_href), -1)
    if idx < 0:
        return ""

    parts = ['<nav class="lesson-nav">']
    if idx > 0:
        prev = order[idx - 1]
        parts.append(
            f'<a class="prev" href="{prefix}{prev[3]}">'
            f'<span class="nav-label">Previous</span>'
            f'<span class="nav-title">{html.escape(prev[2])}</span></a>'
        )
    else:
        parts.append('<span class="placeholder"></span>')

    if idx < len(order) - 1:
        nxt = order[idx + 1]
        parts.append(
            f'<a class="next" href="{prefix}{nxt[3]}">'
            f'<span class="nav-label">Next</span>'
            f'<span class="nav-title">{html.escape(nxt[2])}</span></a>'
        )
    else:
        parts.append('<span class="placeholder"></span>')

    parts.append("</nav>")
    return "".join(parts)


def build_lesson_body(
    source: str,
    java_file: Path,
    meta: LessonMeta,
    sections: list[LessonSection],
) -> tuple[str, str, bool]:
    """Returns (body_html, toc_html, has_toc)."""
    line_count = len(source.splitlines())
    read_mins = max(1, line_count // 40)

    intro_html = ""
    if meta.intro_lines:
        intro_paras = format_concept_html("\n".join(meta.intro_lines))
        intro_html = f'<div class="lesson-intro">{intro_paras}</div>'

    interview_html = format_interview_html(meta.interview_questions)

    meta_pills = (
        f'<div class="lesson-meta">'
        f'<span class="meta-pill accent">{html.escape(java_file.stem)}</span>'
        f'<span class="meta-pill">{line_count} lines</span>'
        f'<span class="meta-pill">~{read_mins} min read</span>'
        f"</div>"
    )

    if sections:
        toc_items = "".join(
            f'<li><a href="#{s.slug}">{html.escape(s.title)}</a></li>'
            for s in sections
        )
        toc_html = (
            '<aside class="lesson-toc">'
            '<div class="toc-label">In this lesson</div>'
            f"<ol>{toc_items}</ol></aside>"
        )

        section_html_parts = []
        for s in sections:
            concept = format_concept_html(s.concept)
            concept_block = (
                f'<div class="concept-block">{concept}</div>' if concept else ""
            )
            section_html_parts.append(
                f'<article class="lesson-section" id="{s.slug}">'
                f'<div class="section-header">'
                f'<span class="section-num">{s.number}</span>'
                f"<h2>{html.escape(s.title)}</h2></div>"
                f"{concept_block}"
                f"{code_block(s.code)}"
                f"</article>"
            )

        full_source = (
            '<details class="full-source">'
            "<summary>View complete source file</summary>"
            f'<div class="full-source-panel">{code_block(source, java_file.name)}</div>'
            "</details>"
        )

        body = (
            meta_pills
            + intro_html
            + interview_html
            + '<div class="lesson-page">'
            + '<div class="lesson-main">'
            + "".join(section_html_parts)
            + full_source
            + "</div>"
            + toc_html
            + "</div>"
        )
        return body, "", True

    # Fallback: no sections — single code view
    body = (
        meta_pills
        + intro_html
        + interview_html
        + code_block(source, java_file.name)
    )
    return body, "", False


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
        _, topic_name = topic_parts(label)

        cards = ['<section class="lesson-cards"><h2 class="section-title">Lessons</h2><div class="card-grid">']
        for java_file in java_files:
            source = java_file.read_text(encoding="utf-8")
            meta = parse_javadoc(source)
            desc = meta.intro_lines[0] if meta.intro_lines else java_file.stem
            lesson_path = f"../lessons/{topic_slug}/{java_file.stem}.html"
            cards.append(
                f'<a class="lesson-card" href="{lesson_path}">'
                f'<span class="card-title">{html.escape(java_file.stem)}</span>'
                f'<span class="card-meta">{html.escape(desc[:80])}</span></a>'
            )
        cards.append("</div></section>")
        body = readme_html + "\n".join(cards)

        out = topics_dir / f"{topic_slug}.html"
        depth = depth_for(out)
        crumbs = [("Home", "index.html"), (topic_name, None)]
        page = page_shell(
            topic_name,
            body,
            prefixed_nav(nav, depth),
            depth,
            label,
            crumbs=crumbs,
        )
        out.write_text(page, encoding="utf-8")


def write_lesson_pages(
    nav: str, order: list[tuple[str, str, str, str]]
) -> list[tuple[str, str, str]]:
    written: list[tuple[str, str, str]] = []

    for folder, label in TOPIC_ORDER:
        topic_dir = LESSONS / folder
        if not topic_dir.is_dir():
            continue
        topic_slug = slugify(folder)
        out_dir = DOCS / "lessons" / topic_slug
        out_dir.mkdir(parents=True, exist_ok=True)
        _, topic_name = topic_parts(label)

        for java_file in sorted(topic_dir.glob("*.java")):
            source = java_file.read_text(encoding="utf-8")
            meta = parse_javadoc(source)
            sections = parse_sections(source)
            body, _, has_toc = build_lesson_body(source, java_file, meta, sections)

            href_suffix = f"lessons/{topic_slug}/{java_file.stem}.html"
            depth = depth_for(out_dir / f"{java_file.stem}.html")
            crumbs = [
                ("Home", "index.html"),
                (topic_name, f"topics/{topic_slug}.html"),
                (java_file.stem, None),
            ]
            body += lesson_nav_html(href_suffix, depth, order)

            display_title = meta.title or java_file.stem
            page = page_shell(
                display_title,
                body,
                prefixed_nav(build_nav(out_dir / f"{java_file.stem}.html"), depth),
                depth,
                f"{topic_name} · {java_file.name}",
                crumbs=crumbs,
                has_toc=has_toc,
            )
            out_path = out_dir / f"{java_file.stem}.html"
            out_path.write_text(page, encoding="utf-8")
            written.append((label, java_file.stem, href_suffix))

    return written


def study_path_html() -> str:
    parts = [
        '<section class="study-path">',
        '<h2>Suggested study path</h2>',
        '<div class="path-grid">',
    ]
    for week, folders in STUDY_PATH:
        links = " → ".join(
            f'<a href="topics/{TOPIC_SLUGS[f]}.html">{html.escape(TOPIC_NAMES[f])}</a>'
            for f in folders
            if f in TOPIC_SLUGS
        )
        parts.append(
            f'<div class="path-card">'
            f'<div class="path-week">{html.escape(week)}</div>'
            f"<p>{links}</p></div>"
        )
    parts.append("</div></section>")
    return "".join(parts)


def write_index(nav: str, lessons: list[tuple[str, str, str]]) -> None:
    main_readme = LESSONS / "README.md"
    intro = ""
    if main_readme.exists():
        intro = (
            '<details class="curriculum-details">'
            "<summary>Full curriculum guide</summary>"
            f'<section class="readme">{render_markdown(main_readme.read_text())}</section>'
            "</details>"
        )

    topic_count = sum(1 for folder, _ in TOPIC_ORDER if (LESSONS / folder).is_dir())
    lesson_count = len(lessons)

    hero = f"""<section class="hero">
  <span class="hero-eyebrow">SDE2 Interview Preparation</span>
  <h1>Learn Java, one concept at a time</h1>
  <p class="hero-lead">Structured lessons with theory, interview questions, and annotated code — designed for focused study, not just reading source files.</p>
  <div class="hero-stats">
    <div class="hero-stat"><strong>{lesson_count}</strong><span>Interactive lessons</span></div>
    <div class="hero-stat"><strong>{topic_count}</strong><span>Core topics</span></div>
    <div class="hero-stat"><strong>Q&A</strong><span>Interview focus</span></div>
  </div>
</section>"""

    cards = ['<section class="topic-grid"><h2 class="section-title">Explore topics</h2><div class="card-grid">']
    for folder, label in TOPIC_ORDER:
        if not (LESSONS / folder).is_dir():
            continue
        topic_slug = slugify(folder)
        count = len(list((LESSONS / folder).glob("*.java")))
        topic_num, topic_name = topic_parts(label)
        cards.append(
            f'<a class="topic-card" href="topics/{topic_slug}.html">'
            f'<span class="card-num">{html.escape(topic_num)}</span>'
            f'<span class="card-title">{html.escape(topic_name)}</span>'
            f'<span class="card-meta">{count} lesson{"s" if count != 1 else ""}</span></a>'
        )
    cards.append("</div></section>")

    body = hero + study_path_html() + intro + "\n".join(cards)
    page = page_shell("Core Java — SDE2 Interview Prep", body, nav, 0, hero=hero)
    (DOCS / "index.html").write_text(page, encoding="utf-8")


def copy_assets() -> None:
    assets = DOCS / "assets"
    assets.mkdir(parents=True, exist_ok=True)
    for name in ("style.css", "app.js"):
        src = ROOT / "scripts" / "assets" / name
        if src.exists():
            (assets / name).write_text(src.read_text(encoding="utf-8"), encoding="utf-8")
    (assets / "pygments.css").write_text(FORMATTER.get_style_defs(".highlight"), encoding="utf-8")


def main() -> None:
    if DOCS.exists():
        shutil.rmtree(DOCS)
    DOCS.mkdir(parents=True)

    copy_assets()
    (DOCS / ".nojekyll").touch()
    order = build_lesson_order()
    lessons = write_lesson_pages(build_nav(), order)
    write_topic_pages(build_nav())
    write_index(build_nav(), lessons)
    print(f"Generated {len(lessons)} lesson pages in {DOCS}")


if __name__ == "__main__":
    main()
