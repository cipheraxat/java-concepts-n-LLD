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
    (
        "Week 1 — Foundations",
        "Master OOP, Collections, Strings & Exceptions. These come up in every Java interview.",
        ["oop_concepts", "collections_framework", "string_handling", "exception_handling"],
    ),
    (
        "Week 2 — Modern Java",
        "Streams, Generics & SOLID show you write production-quality Java, not just syntax.",
        ["java8_features", "generics", "solid_principles"],
    ),
    (
        "Week 3 — Advanced",
        "Concurrency, Patterns & JVM separate senior candidates. Expect deep follow-ups.",
        ["multithreading_concurrency", "design_patterns", "jvm_internals"],
    ),
]

TOPIC_INTERVIEW_META: dict[str, dict[str, object]] = {
    "oop_concepts": {
        "priority": "must-know",
        "frequency": "Every round",
        "hook": "Interviewers probe OOP to see if you understand design — not just syntax. Expect equals/hashCode, overriding rules, and abstract class vs interface.",
        "signals": ["Can you explain runtime polymorphism?", "Why override hashCode with equals?", "Abstract class vs interface — when which?"],
    },
    "collections_framework": {
        "priority": "must-know",
        "frequency": "Almost every interview",
        "hook": "HashMap internals is the #1 Java collections question. Know collision handling, load factor, and ConcurrentHashMap differences cold.",
        "signals": ["How does HashMap work internally?", "ArrayList vs LinkedList?", "Fail-fast vs fail-safe?"],
    },
    "generics": {
        "priority": "high",
        "frequency": "Common at SDE2",
        "hook": "PECS and type erasure trip up many candidates. Interviewers use generics to test depth beyond basic collections usage.",
        "signals": ["What is PECS?", "Why can't you create new T()?", "Wildcard vs type parameter?"],
    },
    "exception_handling": {
        "priority": "high",
        "frequency": "Common",
        "hook": "Checked vs unchecked, try-with-resources, and custom exceptions — quick questions that reveal real-world experience.",
        "signals": ["Checked vs unchecked exceptions?", "What is try-with-resources?", "Can you catch Error?"],
    },
    "multithreading_concurrency": {
        "priority": "must-know",
        "frequency": "Senior / SDE2 rounds",
        "hook": "The highest-signal Java topic for SDE2. Thread pools, volatile, synchronized, deadlock — expect whiteboard follow-ups.",
        "signals": ["synchronized vs ReentrantLock?", "What does volatile do?", "How to prevent deadlock?"],
    },
    "java8_features": {
        "priority": "must-know",
        "frequency": "Very common",
        "hook": "Stream API and Optional are daily interview staples. Know lazy evaluation, map vs flatMap, and orElse vs orElseGet.",
        "signals": ["map vs flatMap?", "Optional.orElse vs orElseGet?", "Functional interfaces?"],
    },
    "design_patterns": {
        "priority": "high",
        "frequency": "SDE2+ rounds",
        "hook": "Not just naming patterns — interviewers want trade-offs. Singleton thread-safety and Strategy vs if-else are classics.",
        "signals": ["Thread-safe Singleton?", "Builder vs constructor?", "Strategy vs inheritance?"],
    },
    "jvm_internals": {
        "priority": "high",
        "frequency": "Senior rounds",
        "hook": "Heap vs stack, GC basics, and class loading separate candidates who've debugged production issues from those who haven't.",
        "signals": ["Heap vs stack?", "What triggers GC?", "What is type erasure?"],
    },
    "string_handling": {
        "priority": "medium",
        "frequency": "Warm-up questions",
        "hook": "String immutability and String pool questions are fast filters — answer confidently in under 60 seconds.",
        "signals": ["Why is String immutable?", "String vs StringBuilder?", "What is the string pool?"],
    },
    "solid_principles": {
        "priority": "high",
        "frequency": "Design rounds",
        "hook": "Interviewers ask SOLID to evaluate design thinking. Have a real example for each letter — not textbook definitions.",
        "signals": ["Explain each SOLID principle", "SRP violation example?", "DIP in Spring/DI?"],
    },
    "access_modifiers": {
        "priority": "medium",
        "frequency": "Quick filters",
        "hook": "Seems basic but protected-across-packages and override visibility rules catch people who've only memorized a table.",
        "signals": ["Four access levels?", "Protected across packages?", "Override visibility rules?"],
    },
    "static_and_final": {
        "priority": "high",
        "frequency": "Common traps",
        "hook": "static vs instance, final semantics, and effectively final are trap questions — interviewers love catching shallow answers.",
        "signals": ["Why is main() static?", "Does final make objects immutable?", "Effectively final?"],
    },
    "inner_classes": {
        "priority": "medium",
        "frequency": "Occasional",
        "hook": "Less frequent but tests language depth. Know when to use static nested vs inner, and anonymous class use cases.",
        "signals": ["Static nested vs inner class?", "Why can inner class access outer private?", "Anonymous inner class use?"],
    },
    "wrapper_classes": {
        "priority": "medium",
        "frequency": "Trap questions",
        "hook": "Integer cache (-128 to 127) and == vs equals on wrappers are classic trick questions. Know them before any interview.",
        "signals": ["Integer cache range?", "== vs equals on Integer?", "Autoboxing pitfalls?"],
    },
}

MUST_KNOW_CHAPTERS = [
    "collections_framework",
    "multithreading_concurrency",
    "java8_features",
    "oop_concepts",
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
        if re.search(r"\binterview\b", lower):
            in_interview = True
            continue
        if in_interview:
            if line.startswith("=>"):
                if meta.interview_questions:
                    meta.interview_questions[-1] += " => " + line[2:].strip()
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


ASCII_BOX_RE = re.compile(r"[┌┐└┘├┤┬┴┼│─╭╮╰╯╞╡╪╫]")


def format_concept_html(text: str) -> str:
    if not text:
        return ""
    escaped = html.escape(text)
    blocks = [b.strip() for b in re.split(r"\n\n+", escaped) if b.strip()]
    if not blocks:
        blocks = [escaped]
    parts: list[str] = []
    for block in blocks:
        if ASCII_BOX_RE.search(block) or (
            block.count("\n") >= 2 and re.search(r"^\s{2,}\S", block, re.MULTILINE)
        ):
            parts.append(f'<pre class="concept-pre">{block}</pre>')
        else:
            parts.append(f'<p>{block.replace(chr(10), "<br>")}</p>')
    return "".join(parts)


def clean_section_code(code: str, has_concept: bool) -> str:
    cleaned = SECTION_RE.sub("", code, count=1)
    if has_concept:
        cleaned = re.sub(r"/\*.*?\*/", "", cleaned, flags=re.DOTALL)
    lines = []
    for line in cleaned.splitlines():
        stripped = line.strip()
        if stripped.startswith("// ───"):
            continue
        lines.append(line)
    return "\n".join(lines).strip()


def is_meaningful_code(code: str) -> bool:
    for line in code.splitlines():
        s = line.strip()
        if not s or s.startswith("//") or s.startswith("/*") or s.startswith("*"):
            continue
        return True
    return False


def split_concept_and_interview(concept: str) -> tuple[str, list[str]]:
    if not concept:
        return "", []
    body_lines: list[str] = []
    interview_lines: list[str] = []
    for line in concept.splitlines():
        stripped = line.strip()
        if re.match(r"^Interview[:\s]", stripped, re.I):
            q = re.sub(r"^Interview[:\s]*", "", stripped, flags=re.I).strip().strip('"')
            if q:
                interview_lines.append(q)
        else:
            body_lines.append(line)
    return "\n".join(body_lines).strip(), interview_lines


def extract_cheatsheet(readme_text: str) -> list[tuple[str, str]]:
    """Pull Q&A rows from 'Key Interview Points' markdown table."""
    rows: list[tuple[str, str]] = []
    if "Key Interview Points" not in readme_text:
        return rows
    section = readme_text.split("Key Interview Points", 1)[1]
    section = re.split(r"\n## ", section)[0]
    for line in section.splitlines():
        if not line.startswith("|") or "---" in line or "Concept" in line or "Question" in line:
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) >= 2 and cells[0] and cells[1]:
            rows.append((cells[0], cells[1]))
    return rows


def format_cheatsheet_html(rows: list[tuple[str, str]], title: str = "Quick recall sheet") -> str:
    if not rows:
        return ""
    cards = []
    for q, a in rows:
        cards.append(
            f'<div class="recall-card">'
            f'<div class="recall-q">{html.escape(q)}</div>'
            f'<div class="recall-a">{html.escape(a)}</div>'
            f"</div>"
        )
    return (
        f'<section class="cheatsheet-section">'
        f'<h2 class="section-title">{html.escape(title)}</h2>'
        f'<p class="section-hint">Memorize these short answers — expand with code examples in each lesson.</p>'
        f'<div class="recall-grid">{"".join(cards)}</div>'
        f"</section>"
    )


def format_mock_qa_html(questions: list[str], title: str = "Questions you'll likely face") -> str:
    if not questions:
        return ""
    cards = []
    for q in questions:
        if "=>" in q:
            parts = q.split("=>", 1)
            question = html.escape(parts[0].strip().strip('"'))
            answer = html.escape(parts[1].strip())
            cards.append(
                f'<div class="mock-qa-card">'
                f'<div class="mock-q"><span class="qa-tag">Q</span><p>{question}</p></div>'
                f'<div class="mock-a"><span class="qa-tag answer">30-sec answer</span><p>{answer}</p></div>'
                f"</div>"
            )
        else:
            question = html.escape(q.strip().strip('"'))
            cards.append(
                f'<div class="mock-qa-card">'
                f'<div class="mock-q"><span class="qa-tag">Q</span><p>{question}</p></div>'
                f'<div class="mock-a mock-a-prompt"><span class="qa-tag answer">Your turn</span>'
                f"<p>Answer aloud before scrolling to the code proof below.</p></div>"
                f"</div>"
            )
    return (
        '<section class="mock-qa-section">'
        f'<div class="callout-label">🎤 {html.escape(title)}</div>'
        f'<div class="mock-qa-grid">{"".join(cards)}</div>'
        "</section>"
    )


def instructor_note_html(text: str, label: str = "Instructor note") -> str:
    return (
        f'<aside class="instructor-note">'
        f'<div class="instructor-label">{html.escape(label)}</div>'
        f"<p>{html.escape(text)}</p></aside>"
    )


def interview_angle_html(questions: list[str]) -> str:
    if not questions:
        return ""
    items = "".join(f"<li>{html.escape(q)}</li>" for q in questions)
    return (
        '<div class="interview-angle">'
        '<div class="angle-label">Likely follow-up here</div>'
        f"<ul>{items}</ul></div>"
    )


def study_protocol_html() -> str:
    return (
        '<section class="study-protocol">'
        "<h2 class=\"section-title\">How to practice this chapter</h2>"
        "<ol class=\"protocol-steps\">"
        "<li><strong>Read the question</strong> — cover the answer and respond aloud in 30–60 seconds.</li>"
        "<li><strong>Check the quick answer</strong> — fix gaps in your explanation immediately.</li>"
        "<li><strong>Walk through the code</strong> — point to the lines that prove your answer.</li>"
        "<li><strong>Handle a follow-up</strong> — ask yourself <em>why</em> and <em>what if</em> (edge cases, trade-offs).</li>"
        "</ol></section>"
    )


def priority_badge(priority: str) -> str:
    labels = {
        "must-know": "Must-know",
        "high": "High yield",
        "medium": "Good to know",
    }
    return f'<span class="priority-badge {priority}">{labels.get(priority, priority)}</span>'


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
    rendered = markdown.markdown(text, extensions=["tables", "fenced_code"])
    rendered = re.sub(r"<table>", '<div class="table-wrap"><table>', rendered)
    rendered = rendered.replace("</table>", "</table></div>")
    rendered = re.sub(
        r"<pre><code>",
        '<div class="readme-code"><pre><code>',
        rendered,
    )
    rendered = rendered.replace("</code></pre>", "</code></pre></div>")
    return rendered


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
  <title>{html.escape(title)} — Java Interview Prep</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="{prefix}assets/pygments.css">
  <link rel="stylesheet" href="{prefix}assets/style.css">
</head>
<body>
  <header class="topbar">
    <button class="nav-toggle" aria-label="Toggle navigation" onclick="document.body.classList.toggle('nav-open')">☰</button>
    <a class="topbar-brand" href="{prefix}index.html">
      <span class="brand-mark">Ji</span>
      Java Interview Prep
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
        <span>32 interview chapters · 14 topics</span>
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
        '<div class="progress-text" id="progress-text">0 / 32 chapters done</div>',
        "</div>",
        '<input type="search" class="nav-search" id="nav-search" placeholder="Search interview topics…" aria-label="Search">',
        '<p class="sidebar-label">Interview chapters</p>',
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
            f'<a class="topic-link" href="{topic_href}" onclick="event.stopPropagation()">brief</a>'
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
            f'<span class="nav-label">Previous chapter</span>'
            f'<span class="nav-title">{html.escape(prev[2])}</span></a>'
        )
    else:
        parts.append('<span class="placeholder"></span>')

    if idx < len(order) - 1:
        nxt = order[idx + 1]
        parts.append(
            f'<a class="next" href="{prefix}{nxt[3]}">'
            f'<span class="nav-label">Next chapter</span>'
            f'<span class="nav-title">{html.escape(nxt[2])}</span></a>'
        )
    else:
        parts.append('<span class="placeholder"></span>')

    parts.append("</nav>")
    return "".join(parts)


def count_section_interview_hints(sections: list[LessonSection]) -> int:
    total = 0
    for s in sections:
        _, hints = split_concept_and_interview(s.concept)
        total += len(hints)
    return total


def build_lesson_body(
    source: str,
    java_file: Path,
    meta: LessonMeta,
    sections: list[LessonSection],
    topic_name: str = "",
) -> tuple[str, str, bool]:
    """Returns (body_html, toc_html, has_toc)."""
    line_count = len(source.splitlines())
    read_mins = max(1, line_count // 40)
    q_count = len(meta.interview_questions) + count_section_interview_hints(sections)

    intro_html = ""
    if meta.intro_lines:
        intro_paras = format_concept_html("\n".join(meta.intro_lines))
        intro_html = (
            f'<div class="lesson-intro">'
            f'<div class="intro-label">What interviewers test</div>'
            f"{intro_paras}</div>"
        )

    instructor_html = instructor_note_html(
        f"This chapter covers {topic_name or 'core Java'} concepts that show up as direct questions "
        f"or follow-ups. Don't just read — answer each question aloud, then use the code as proof.",
        "How to use this chapter",
    )

    mock_qa_html = format_mock_qa_html(meta.interview_questions)

    meta_pills = (
        f'<div class="lesson-meta">'
        f'<span class="meta-pill accent">Interview chapter</span>'
        f'<span class="meta-pill">{q_count} likely question{"s" if q_count != 1 else ""}</span>'
        f'<span class="meta-pill">~{read_mins} min drill</span>'
        f"</div>"
    )

    if sections:
        toc_items = ['<li><a href="#mock-questions">Likely questions</a></li>'] + [
            f'<li><a href="#{s.slug}">{html.escape(s.title)}</a></li>'
            for s in sections
        ]
        toc_html = (
            '<aside class="lesson-toc">'
            '<div class="toc-label">Interview map</div>'
            f"<ol>{''.join(toc_items)}</ol></aside>"
        )

        section_html_parts = []
        for s in sections:
            concept_body, interview_hints = split_concept_and_interview(s.concept)
            concept_html = format_concept_html(concept_body)
            concept_block = (
                f'<div class="concept-block">'
                f'<div class="concept-label">Concept</div>{concept_html}</div>'
                if concept_html
                else ""
            )
            angle_html = interview_angle_html(interview_hints)
            cleaned_code = clean_section_code(s.code, bool(s.concept))
            code_html = ""
            if is_meaningful_code(cleaned_code):
                code_html = (
                    '<div class="code-proof">'
                    '<div class="code-proof-label">Code proof — cite this in your answer</div>'
                    f"{code_block(cleaned_code)}</div>"
                )
            section_html_parts.append(
                f'<article class="lesson-section" id="{s.slug}">'
                f'<div class="section-header">'
                f'<span class="section-num">{s.number}</span>'
                f"<h2>{html.escape(s.title)}</h2></div>"
                f"{angle_html}"
                f"{concept_block}"
                f"{code_html}"
                f"</article>"
            )

        full_source = (
            '<details class="full-source">'
            "<summary>Full runnable source (reference)</summary>"
            f'<div class="full-source-panel">{code_block(source, java_file.name)}</div>'
            "</details>"
        )

        mock_section = (
            f'<div id="mock-questions">{mock_qa_html}</div>' if mock_qa_html else ""
        )

        body = (
            meta_pills
            + instructor_html
            + intro_html
            + '<div class="lesson-page">'
            + '<div class="lesson-main">'
            + mock_section
            + "".join(section_html_parts)
            + study_protocol_html()
            + full_source
            + "</div>"
            + toc_html
            + "</div>"
        )
        return body, "", True

    body = (
        meta_pills
        + instructor_html
        + intro_html
        + format_mock_qa_html(meta.interview_questions)
        + study_protocol_html()
        + '<div class="code-proof"><div class="code-proof-label">Code proof</div>'
        + code_block(source, java_file.name)
        + "</div>"
    )
    return body, "", False


def topic_chapter_brief(folder: str, topic_name: str) -> str:
    meta = TOPIC_INTERVIEW_META.get(folder, {})
    hook = str(meta.get("hook", f"Core interview questions on {topic_name}."))
    frequency = str(meta.get("frequency", "Common"))
    priority = str(meta.get("priority", "high"))
    signals = meta.get("signals", [])
    signal_html = ""
    if signals:
        items = "".join(f"<li>{html.escape(s)}</li>" for s in signals[:3])
        signal_html = (
            f'<div class="signal-block"><div class="signal-label">Interviewer signals</div>'
            f"<ul>{items}</ul></div>"
        )
    return (
        f'<section class="chapter-brief">'
        f"{priority_badge(priority)}"
        f'<span class="frequency-pill">{html.escape(frequency)}</span>'
        f"<p class=\"chapter-hook\">{html.escape(hook)}</p>"
        f"{signal_html}</section>"
    )


def write_topic_pages(nav: str) -> None:
    topics_dir = DOCS / "topics"
    topics_dir.mkdir(parents=True, exist_ok=True)

    for folder, label in TOPIC_ORDER:
        topic_dir = LESSONS / folder
        if not topic_dir.is_dir():
            continue
        readme_path = topic_dir / "README.md"
        readme_text = readme_path.read_text() if readme_path.exists() else ""
        cheatsheet = format_cheatsheet_html(extract_cheatsheet(readme_text))
        readme_html = ""
        if readme_text:
            readme_html = (
                '<details class="curriculum-details">'
                "<summary>Full chapter notes</summary>"
                f'<section class="readme">{render_markdown(readme_text)}</section>'
                "</details>"
            )

        java_files = sorted(topic_dir.glob("*.java"))
        topic_slug = slugify(folder)
        _, topic_name = topic_parts(label)

        cards = [
            '<section class="lesson-cards">',
            '<h2 class="section-title">Practice drills</h2>',
            '<p class="section-hint">Each drill = likely interview questions + code you can cite on a whiteboard.</p>',
            '<div class="card-grid">',
        ]
        for java_file in java_files:
            source = java_file.read_text(encoding="utf-8")
            meta = parse_javadoc(source)
            sections = parse_sections(source)
            q_count = len(meta.interview_questions) + count_section_interview_hints(sections)
            desc = meta.intro_lines[0] if meta.intro_lines else "Interview Q&A with code proof"
            lesson_path = f"../lessons/{topic_slug}/{java_file.stem}.html"
            cards.append(
                f'<a class="lesson-card" href="{lesson_path}">'
                f'<span class="card-title">{html.escape(java_file.stem)}</span>'
                f'<span class="card-meta">{q_count} questions · {html.escape(desc[:70])}</span></a>'
            )
        cards.append("</div></section>")
        body = (
            topic_chapter_brief(folder, topic_name)
            + cheatsheet
            + "\n".join(cards)
            + readme_html
        )

        out = topics_dir / f"{topic_slug}.html"
        depth = depth_for(out)
        crumbs = [("Home", "index.html"), (topic_name, None)]
        page = page_shell(
            f"Chapter: {topic_name}",
            body,
            prefixed_nav(nav, depth),
            depth,
            "Interview brief + practice drills",
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
            body, _, has_toc = build_lesson_body(
                source, java_file, meta, sections, topic_name
            )

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
        '<h2 class="section-title">3-week interview prep plan</h2>',
        '<p class="section-hint">Follow this order if your interview is coming up. Each week builds on the last.</p>',
        '<div class="path-grid">',
    ]
    for week, advice, folders in STUDY_PATH:
        links = " → ".join(
            f'<a href="topics/{TOPIC_SLUGS[f]}.html">{html.escape(TOPIC_NAMES[f])}</a>'
            for f in folders
            if f in TOPIC_SLUGS
        )
        parts.append(
            f'<div class="path-card">'
            f'<div class="path-week">{html.escape(week)}</div>'
            f'<p class="path-advice">{html.escape(advice)}</p>'
            f"<p class=\"path-topics\">{links}</p></div>"
        )
    parts.append("</div></section>")
    return "".join(parts)


def must_know_html() -> str:
    cards = []
    for folder in MUST_KNOW_CHAPTERS:
        if folder not in TOPIC_SLUGS:
            continue
        meta = TOPIC_INTERVIEW_META.get(folder, {})
        cards.append(
            f'<a class="must-know-card" href="topics/{TOPIC_SLUGS[folder]}.html">'
            f'<span class="must-know-tag">Must-know</span>'
            f'<span class="card-title">{html.escape(TOPIC_NAMES[folder])}</span>'
            f'<span class="card-meta">{html.escape(str(meta.get("frequency", "")))}</span></a>'
        )
    return (
        '<section class="must-know-section">'
        '<h2 class="section-title">Start here — highest-frequency topics</h2>'
        '<p class="section-hint">If you only have a few days, cover these chapters first.</p>'
        f'<div class="card-grid">{"".join(cards)}</div>'
        "</section>"
    )


def instructor_welcome_html() -> str:
    return (
        '<section class="instructor-welcome">'
        "<h2 class=\"section-title\">Your interview coach</h2>"
        "<p>I'm structuring this guide the way I'd run a 1:1 Java interview prep session:</p>"
        "<ol class=\"coach-steps\">"
        "<li><strong>Read the likely question</strong> — no peeking at the answer.</li>"
        "<li><strong>Answer out loud</strong> — 30 to 60 seconds, like you're in the room.</li>"
        "<li><strong>Check the quick answer</strong> — fix anything you missed or got wrong.</li>"
        "<li><strong>Prove it with code</strong> — interviewers love when you tie concepts to implementation.</li>"
        "</ol></section>"
    )


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
  <span class="hero-eyebrow">Java SDE2 Interview Coach</span>
  <h1>Answer like you've done this before</h1>
  <p class="hero-lead">Not a textbook — a drill book. Every chapter opens with the questions interviewers actually ask, gives you a 30-second answer, then shows the code that backs it up.</p>
  <div class="hero-stats">
    <div class="hero-stat"><strong>{lesson_count}</strong><span>Practice drills</span></div>
    <div class="hero-stat"><strong>{topic_count}</strong><span>Interview chapters</span></div>
    <div class="hero-stat"><strong>Q→A→Code</strong><span>Study method</span></div>
  </div>
</section>"""

    cards = [
        '<section class="topic-grid">',
        '<h2 class="section-title">All interview chapters</h2>',
        '<p class="section-hint">Each chapter = interview brief, quick-recall sheet, and code drills.</p>',
        '<div class="card-grid">',
    ]
    for folder, label in TOPIC_ORDER:
        if not (LESSONS / folder).is_dir():
            continue
        topic_slug = slugify(folder)
        count = len(list((LESSONS / folder).glob("*.java")))
        topic_num, topic_name = topic_parts(label)
        tmeta = TOPIC_INTERVIEW_META.get(folder, {})
        priority = str(tmeta.get("priority", ""))
        badge = priority_badge(priority) if priority else ""
        cards.append(
            f'<a class="topic-card" href="topics/{topic_slug}.html">'
            f'<span class="card-num">{html.escape(topic_num)}</span>'
            f"{badge}"
            f'<span class="card-title">{html.escape(topic_name)}</span>'
            f'<span class="card-meta">{count} drill{"s" if count != 1 else ""} · '
            f'{html.escape(str(tmeta.get("frequency", "Common")))}</span></a>'
        )
    cards.append("</div></section>")

    body = instructor_welcome_html() + must_know_html() + study_path_html() + intro + "\n".join(cards)
    page = page_shell("Java Interview Prep", body, nav, 0, hero=hero)
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
