(function () {
  "use strict";

  // Sidebar search
  var search = document.getElementById("nav-search");
  if (search) {
    search.addEventListener("input", function () {
      var q = search.value.toLowerCase().trim();
      document.querySelectorAll(".nav-topic").forEach(function (topic) {
        var visible = false;
        var summary = topic.querySelector("summary");
        var text = summary ? summary.textContent.toLowerCase() : "";
        topic.querySelectorAll(".nav-lessons li").forEach(function (li) {
          var match = !q || li.textContent.toLowerCase().includes(q) || text.includes(q);
          li.style.display = match ? "" : "none";
          if (match) visible = true;
        });
        topic.style.display = visible || !q ? "" : "none";
        if (q && visible) {
          var details = topic.querySelector("details");
          if (details) details.open = true;
        }
      });
    });
  }

  // Copy code (section or full)
  window.copyCode = function (btn) {
    var section = btn.closest(".code-section, .full-source-panel");
    var pre = section ? section.querySelector("pre") : document.querySelector(".highlight pre");
    if (!pre) return;
    navigator.clipboard.writeText(pre.innerText).then(function () {
      btn.textContent = "Copied!";
      btn.classList.add("copied");
      var toast = document.getElementById("toast");
      if (toast) toast.classList.add("show");
      setTimeout(function () {
        btn.textContent = "Copy";
        btn.classList.remove("copied");
        if (toast) toast.classList.remove("show");
      }, 2000);
    });
  };

  // TOC scroll spy
  var toc = document.querySelector(".lesson-toc");
  if (toc) {
    var links = toc.querySelectorAll("a");
    var sections = [];
    links.forEach(function (a) {
      var id = a.getAttribute("href").slice(1);
      var el = document.getElementById(id);
      if (el) sections.push({ link: a, el: el });
    });

    function onScroll() {
      var current = sections[0];
      sections.forEach(function (s) {
        if (s.el.getBoundingClientRect().top <= 120) current = s;
      });
      links.forEach(function (a) { a.classList.remove("active"); });
      if (current) current.link.classList.add("active");
    }

    window.addEventListener("scroll", onScroll, { passive: true });
    onScroll();
  }

  // Mark visited lessons
  var path = window.location.pathname;
  if (path.includes("/lessons/")) {
    try {
      var visited = JSON.parse(localStorage.getItem("visitedLessons") || "[]");
      if (!visited.includes(path)) {
        visited.push(path);
        localStorage.setItem("visitedLessons", JSON.stringify(visited));
      }
      var total = document.querySelectorAll(".nav-lessons a").length;
      var count = visited.length;
      var prog = document.getElementById("progress-fill");
      var progText = document.getElementById("progress-text");
      if (prog) prog.style.width = Math.min(100, (count / total) * 100) + "%";
      if (progText) progText.textContent = count + " / " + total + " explored";

      document.querySelectorAll(".nav-lessons a").forEach(function (a) {
        var href = a.getAttribute("href");
        if (visited.some(function (v) { return v.endsWith(href); })) {
          a.classList.add("visited");
        }
      });
    } catch (e) { /* ignore */ }
  }
})();
