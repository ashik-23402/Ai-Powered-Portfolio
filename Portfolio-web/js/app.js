(() => {
  "use strict";

  const THEME_KEY = "portfolio_theme";
  const DEFAULT_API_BASE = "http://localhost:8080";
  const ASK_ENDPOINT = "/api/v1/ask";

  let configuredApiBase = DEFAULT_API_BASE;

  /* ============================== .env config ==============================
     Static site, no build step, so there's no bundler to inject a real .env
     at build time. We fetch the plain-text .env file at runtime (served
     alongside index.html) and parse simple KEY=VALUE lines to read API_BASE.
     Falls back silently to DEFAULT_API_BASE if .env is missing. */

  function parseEnv(text) {
    const result = {};
    text.split(/\r?\n/).forEach((line) => {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith("#")) return;
      const idx = trimmed.indexOf("=");
      if (idx === -1) return;
      const key = trimmed.slice(0, idx).trim();
      let value = trimmed.slice(idx + 1).trim();
      if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
        value = value.slice(1, -1);
      }
      result[key] = value;
    });
    return result;
  }

  async function loadEnvConfig() {
    try {
      const res = await fetch(".env", { cache: "no-store" });
      if (!res.ok) return;
      const env = parseEnv(await res.text());
      if (env.API_BASE) configuredApiBase = env.API_BASE.replace(/\/+$/, "");
    } catch (_) {
      /* fall back to DEFAULT_API_BASE */
    }
  }

  /* ============================== theme ============================== */

  const themeToggle = document.getElementById("theme-toggle");

  function applyTheme(theme) {
    if (theme === "light" || theme === "dark") {
      document.documentElement.setAttribute("data-theme", theme);
    } else {
      document.documentElement.removeAttribute("data-theme");
    }
  }

  function systemPrefersDark() {
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  }

  function initTheme() {
    const saved = localStorage.getItem(THEME_KEY);
    applyTheme(saved);
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme") || (systemPrefersDark() ? "dark" : "light");
    const next = current === "dark" ? "light" : "dark";
    applyTheme(next);
    localStorage.setItem(THEME_KEY, next);
  }

  themeToggle.addEventListener("click", toggleTheme);

  /* ============================== mobile nav ============================== */

  const navToggle = document.getElementById("nav-toggle");
  const navLinks = document.getElementById("nav-links");

  function setNavOpen(open) {
    navLinks.classList.toggle("is-open", open);
    navToggle.setAttribute("aria-expanded", String(open));
  }

  navToggle.addEventListener("click", () => setNavOpen(!navLinks.classList.contains("is-open")));
  navLinks.querySelectorAll("a").forEach((link) => link.addEventListener("click", () => setNavOpen(false)));

  /* ============================== icon library ==============================
     Small set of hand-drawn, generic feather-style icons reused across the
     skills grid so every technology gets a consistent, legible glyph. */

  const ICONS = {
    coffee: '<path d="M4 8h13a3 3 0 0 1 0 6h-1"/><path d="M4 8v7a4 4 0 0 0 4 4h5a4 4 0 0 0 4-4v-1"/><path d="M8 2c-.5 1 .5 1.5 0 3M12 2c-.5 1 .5 1.5 0 3"/>',
    leaf: '<path d="M11 20A7 7 0 0 1 4 13c0-6 5-11 16-11 0 11-5 16-11 16Z"/><path d="M4 20 14 10"/>',
    cloud: '<path d="M17.5 19H8a5 5 0 1 1 1.3-9.8A6 6 0 0 1 21 10.5 4 4 0 0 1 17.5 19Z"/>',
    database: '<ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5"/><path d="M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"/>',
    gitBranch: '<circle cx="6" cy="6" r="2.5"/><circle cx="6" cy="18" r="2.5"/><circle cx="18" cy="9" r="2.5"/><path d="M6 8.5V15.5"/><path d="M18 11.5V13a4 4 0 0 1-4 4H8.5"/>',
    layers: '<path d="m12 2 9 5-9 5-9-5 9-5Z"/><path d="m3 12 9 5 9-5"/><path d="m3 17 9 5 9-5"/>',
    container: '<path d="M21 8 12 3 3 8l9 5 9-5Z"/><path d="M3 8v9l9 5 9-5V8"/><path d="M12 13v9"/>',
    lock: '<rect x="4" y="11" width="16" height="9" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
    queue: '<path d="M4 6h16M4 12h10M4 18h13"/>',
    monitor: '<rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/><path d="m7 11 3-3 3 2 4-4"/>',
    infinity: '<path d="M8.5 8a4 4 0 1 0 0 8c2.5 0 4-2.5 7-8a4 4 0 1 1 0 8c-3 0-4.5-5.5-7-8Z"/>',
    terminal: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="m7 9 3 3-3 3M13 15h4"/>',
    workflow: '<circle cx="6" cy="6" r="2.5"/><circle cx="18" cy="18" r="2.5"/><circle cx="18" cy="6" r="2.5"/><path d="M8.5 6h7M18 8.5v7M8.5 7.5 15.5 16"/>',
    table: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 10h18M9 4v16"/>',
    server: '<rect x="3" y="4" width="18" height="6" rx="1.5"/><rect x="3" y="14" width="18" height="6" rx="1.5"/><path d="M7 7h.01M7 17h.01"/>',
    network: '<circle cx="12" cy="5" r="2.2"/><circle cx="5" cy="19" r="2.2"/><circle cx="19" cy="19" r="2.2"/><path d="m10.2 6.9-3.4 9.8M13.8 6.9l3.4 9.8M7.2 19h9.6"/>',
    sparkle: '<path d="M12 3c.6 3.4 2.6 5.4 6 6-3.4.6-5.4 2.6-6 6-.6-3.4-2.6-5.4-6-6 3.4-.6 5.4-2.6 6-6Z"/><path d="M19 15c.2 1.1.9 1.8 2 2-1.1.2-1.8.9-2 2-.2-1.1-.9-1.8-2-2 1.1-.2 1.8-.9 2-2Z"/>',
    bot: '<rect x="4" y="8" width="16" height="12" rx="3"/><path d="M12 8V4M9 4h6"/><circle cx="9" cy="14" r="1.3"/><circle cx="15" cy="14" r="1.3"/><path d="M9 18h6"/>',
  };

  function iconSvg(key, strokeWidth) {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${strokeWidth || 1.6}">${ICONS[key] || ICONS.terminal}</svg>`;
  }

  /* ============================== content data ==============================
     Sourced from the resume, LinkedIn export and project write-ups in
     Portfolio-web/. Kept as data here so the markup stays generic and easy
     to extend with a new skill, role or project. */

  const SKILLS = [
    { name: "Java", icon: "coffee" },
    { name: "Spring Boot", icon: "leaf" },
    { name: "Spring AI", icon: "sparkle" },
    { name: "Spring Cloud", icon: "cloud" },
    { name: "Hibernate / JPA", icon: "layers" },
    { name: "MySQL", icon: "database" },
    { name: "Microservices", icon: "network" },
    { name: "AWS", icon: "cloud" },
    { name: "AWS EC2", icon: "server" },
    { name: "AWS DynamoDB", icon: "table" },
    { name: "Amazon SQS", icon: "queue" },
    { name: "Docker", icon: "container" },
    { name: "Git", icon: "gitBranch" },
    { name: "Redis", icon: "infinity" },
    { name: "Spring Security / JWT", icon: "lock" },
    { name: "CloudWatch", icon: "monitor" },
    { name: "CI/CD", icon: "workflow" },
    { name: "Agentic Development", icon: "bot" },
    { name: "Claude Code", icon: "terminal" },
  ];

  const EXPERIENCE = [
    {
      role: "Assistant Software Engineer",
      company: "Exabyting · FinTech",
      dates: "Apr 2025 – Present",
      icon: "workflow",
      bullets: [
        "Design and implement high-performance backend APIs for bKash's 24x7 Loan Application & Approval platform.",
        "Re-architected loan processing from synchronous to asynchronous using Amazon SQS and Spring Boot listeners.",
        "Implemented distributed synchronization with Redis locks to prevent race conditions during concurrent approvals.",
        "Run whiteboarding sessions to break requirements down and distribute implementation across the team.",
      ],
      tags: ["Java", "Spring Boot", "AWS SQS", "Redis", "CloudWatch"],
    },
    {
      role: "Associate Software Engineer",
      company: "Exabyting · E-Commerce, AI",
      dates: "Oct 2024 – Apr 2025",
      icon: "leaf",
      bullets: [
        "Built and optimized backend features for Empatic, a survey-based insights platform with an AI chatbot.",
        "Integrated Amazon SQS to offload heavy tasks to asynchronous processing, improving app performance.",
        "Replaced redundant Spring Data JPA queries with custom JPQL and batch processing to cut database load.",
        "Reduced Docker image size via multi-stage builds, improving deployment efficiency.",
      ],
      tags: ["Spring Boot", "JPQL", "Docker", "AWS CloudWatch"],
    },
    {
      role: "Software Engineer Trainee",
      company: "Exabyting · E-Commerce, Healthcare, AI",
      dates: "Apr 2024 – Oct 2024",
      icon: "terminal",
      bullets: [
        "Completed hands-on training in the software development lifecycle and clean code practices.",
        "Learned to design, develop and maintain scalable software solutions across multiple domains.",
        "Contributed to live production projects, applying training directly to real client codebases.",
      ],
      tags: ["Java", "Spring Boot", "Clean Code"],
    },
  ];

  const PROJECTS = [
    {
      title: "24x7 Loan Application & Approval System",
      desc: "A digital loan processing platform letting bKash merchants and agents apply for loans anytime, with a multi-level DSO / distributor / bank approval workflow.",
      highlights: [
        { q: "Slow, tightly-coupled synchronous processing", a: "Decoupled submission from backend processing with Amazon SQS and Spring Boot listeners for better scalability and response time." },
        { q: "High volumes of concurrent applications", a: "Queue-driven architecture with background consumers processing independently for reliable, high-throughput handling." },
        { q: "Race conditions during approval & disbursement", a: "Distributed synchronization using Redis locks to serialize processing per loan application across instances." },
        { q: "Sudden spikes in concurrent requests", a: "Horizontal pod scaling and load-balanced processing across up to 20 pods." },
      ],
      tags: ["Java", "Spring Boot", "AWS SQS", "Redis", "Kubernetes"],
    },
    {
      title: "Empatic",
      desc: "A survey-based platform helping organizations gather insights, send SMS announcements, and analyze results — with an AI chatbot for policy queries, mood assessment and task creation.",
      highlights: [
        { q: "Heavy, time-consuming tasks blocking requests", a: "Integrated Amazon SQS to queue and offload tasks to asynchronous processing." },
        { q: "Limited observability into logs and metrics", a: "Integrated Spring Boot with AWS CloudWatch for logs, metrics and alarms." },
        { q: "Unnecessary queries from Spring Data JPA", a: "Replaced redundant JPA queries with custom JPQL and batch processing for bulk operations." },
        { q: "Oversized Docker images", a: "Multi-stage builds: compile in one stage, ship a minimal runtime image in the next." },
      ],
      tags: ["Spring Boot", "React", "AWS SQS", "CloudWatch", "Nginx"],
    },
    {
      title: "AskAboutMe — AI-Powered Portfolio",
      desc: "The retrieval-augmented Q&A system behind this very site: upload documents about yourself, they're chunked and embedded into a vector store, and an /ask endpoint answers natural-language questions about them using Gemini, grounded only in what was uploaded. The \"Ask AI about me\" button above talks to it directly.",
      highlights: [
        { q: "Answers need to stay grounded, not hallucinated", a: "Built a RAG pipeline: pgvector similarity search over embedded document chunks, then Gemini answers strictly from the retrieved context - and says it doesn't know rather than guessing." },
        { q: "Ingesting documents without blocking uploads", a: "Chunked multipart upload lifecycle (initiate / parts / complete) with a scheduled background job that embeds completed files into pgvector within about a minute." },
        { q: "Protecting a public AI endpoint from abuse", a: "Implemented an in-memory per-IP rate limiter as a servlet filter, returning 429 with a consistent error contract that both frontends translate into a friendly message." },
        { q: "Reliable cleanup after deletion", a: "Soft-delete plus best-effort cleanup of storage and vector rows, retried by a ShedLock-coordinated scheduled job until it succeeds." },
      ],
      tags: ["Spring Boot", "Spring AI", "pgvector", "Gemini", "MinIO", "Docker"],
      link: "https://github.com/ashik-23402/Ai-Powered-Portfolio",
    },
    {
      title: "MedCare",
      desc: "A community healthcare app connecting people to blood donors, ambulance services, AI-driven medical consultations and fundraising support. Owned the entire backend.",
      highlights: [
        { q: "Role-based authentication", a: "JWT-based authentication with Spring Security role-based authorization for each API." },
        { q: "Complex domain relationships", a: "Modeled a complex relational schema with Spring Data JPA." },
      ],
      tags: ["Spring Boot", "Spring Security", "JWT", "JPA"],
      link: "https://github.com/Jakaria44/MedCare",
    },
  ];

  const CHAT_SUGGESTIONS = [
    "What does Ashikur do at Exabyting?",
    "Tell me about the bKash loan project",
    "What's his experience with AWS?",
    "What did he build in MedCare?",
  ];

  /* ============================== render: skills ============================== */

  const skillsGrid = document.getElementById("skills-grid");
  skillsGrid.innerHTML = SKILLS.map(
    (s) => `
    <div class="skill-card">
      <span class="skill-icon">${iconSvg(s.icon)}</span>
      <span class="skill-name">${escapeHtml(s.name)}</span>
    </div>`
  ).join("");

  /* ============================== render: timeline ============================== */

  const timeline = document.getElementById("timeline");
  timeline.innerHTML = EXPERIENCE.map(
    (item) => `
    <li class="timeline-item">
      <span class="timeline-dot">${iconSvg(item.icon, 1.8)}</span>
      <div class="timeline-header">
        <span class="timeline-role">${escapeHtml(item.role)}</span>
        <span class="timeline-company">${escapeHtml(item.company)}</span>
        <span class="timeline-dates">${escapeHtml(item.dates)}</span>
      </div>
      <ul class="timeline-list">
        ${item.bullets.map((b) => `<li>${escapeHtml(b)}</li>`).join("")}
      </ul>
      <div class="timeline-tags">
        ${item.tags.map((t) => `<span class="tag">${escapeHtml(t)}</span>`).join("")}
      </div>
    </li>`
  ).join("");

  /* ============================== render: projects ============================== */

  const projectsGrid = document.getElementById("projects-grid");
  projectsGrid.innerHTML = PROJECTS.map(
    (p) => `
    <article class="project-card">
      <div class="project-head">
        <span class="project-title">${escapeHtml(p.title)}</span>
      </div>
      <p class="project-desc">${escapeHtml(p.desc)}</p>
      <div class="project-highlights">
        ${p.highlights
          .map(
            (h) => `
          <details>
            <summary>${escapeHtml(h.q)}</summary>
            <p>${escapeHtml(h.a)}</p>
          </details>`
          )
          .join("")}
      </div>
      <div class="project-tags">
        ${p.tags.map((t) => `<span class="tag">${escapeHtml(t)}</span>`).join("")}
      </div>
      ${
        p.link
          ? `<a class="project-link" href="${escapeAttr(p.link)}" target="_blank" rel="noopener noreferrer">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><path d="M15 3h6v6M10 14 21 3"/></svg>
              View on GitHub
             </a>`
          : ""
      }
    </article>`
  ).join("");

  function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
  }
  function escapeAttr(str) {
    return escapeHtml(str);
  }

  /* ============================== footer year ============================== */

  document.getElementById("footer-year").textContent = new Date().getFullYear();

  /* ============================== toasts ============================== */

  const toastContainer = document.getElementById("toast-container");
  function showToast(message, variant) {
    const toast = document.createElement("div");
    toast.className = "toast" + (variant === "danger" ? " toast-danger" : "");
    toast.textContent = message;
    toastContainer.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
  }

  /* ============================== ask AI chat widget ============================== */

  const chatFab = document.getElementById("chat-fab");
  const chatPanel = document.getElementById("chat-panel");
  const chatClose = document.getElementById("chat-close");
  const chatMessages = document.getElementById("chat-messages");
  const chatForm = document.getElementById("chat-form");
  const chatInput = document.getElementById("chat-input");
  const chatSend = document.getElementById("chat-send");
  const chatSuggestions = document.getElementById("chat-suggestions");
  const chatStatus = document.getElementById("chat-status");

  let chatStarted = false;
  let chatPending = false;

  function openChat() {
    chatPanel.hidden = false;
    chatFab.setAttribute("aria-expanded", "true");
    if (!chatStarted) startChat();
    chatInput.focus();
  }

  function closeChat() {
    chatPanel.hidden = true;
    chatFab.setAttribute("aria-expanded", "false");
  }

  function toggleChat() {
    if (chatPanel.hidden) openChat();
    else closeChat();
  }

  chatFab.addEventListener("click", toggleChat);
  chatClose.addEventListener("click", closeChat);
  document.querySelectorAll("#nav-ask-btn, #hero-ask-btn").forEach((btn) => btn.addEventListener("click", openChat));

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && !chatPanel.hidden) closeChat();
  });

  function startChat() {
    chatStarted = true;
    appendBotMessage(
      "Hi! I'm an AI assistant grounded in Ashikur's real resume and project notes. Ask me anything about his experience, skills or projects."
    );
    chatSuggestions.innerHTML = CHAT_SUGGESTIONS.map(
      (q) => `<button type="button" class="chat-chip">${escapeHtml(q)}</button>`
    ).join("");
    chatSuggestions.querySelectorAll(".chat-chip").forEach((chip) => {
      chip.addEventListener("click", () => {
        chatInput.value = chip.textContent;
        submitQuestion();
      });
    });
  }

  function scrollChatToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  function appendUserMessage(text) {
    const el = document.createElement("div");
    el.className = "chat-msg chat-msg-user";
    el.innerHTML = `<div class="chat-msg-bubble">${escapeHtml(text)}</div>`;
    chatMessages.appendChild(el);
    scrollChatToBottom();
  }

  function appendBotMessage(text, isError) {
    const el = document.createElement("div");
    el.className = "chat-msg chat-msg-bot" + (isError ? " chat-msg-error" : "");
    el.innerHTML = `
      <span class="chat-msg-icon">${iconSvg("terminal", 2)}</span>
      <div class="chat-msg-bubble">${escapeHtml(text)}</div>`;
    chatMessages.appendChild(el);
    scrollChatToBottom();
  }

  function appendTypingIndicator() {
    const el = document.createElement("div");
    el.className = "chat-msg chat-msg-bot";
    el.id = "chat-typing-indicator";
    el.innerHTML = `
      <span class="chat-msg-icon">${iconSvg("terminal", 2)}</span>
      <div class="chat-msg-bubble"><span class="chat-typing"><span></span><span></span><span></span></span></div>`;
    chatMessages.appendChild(el);
    scrollChatToBottom();
  }

  function removeTypingIndicator() {
    const el = document.getElementById("chat-typing-indicator");
    if (el) el.remove();
  }

  function setChatPending(pending) {
    chatPending = pending;
    chatSend.disabled = pending;
    chatInput.disabled = pending;
    chatStatus.textContent = pending ? "Thinking…" : "Answers grounded in his real experience";
  }

  const AI_UNAVAILABLE_MESSAGE = "The AI service is currently unavailable. Please try again after some time.";
  const RATE_LIMITED_MESSAGE = "You're asking questions a bit fast - please wait a moment and try again.";

  class AskApiError extends Error {
    constructor(message, status) {
      super(message);
      this.status = status;
    }
  }

  async function askBackend(question) {
    const res = await fetch(`${configuredApiBase}${ASK_ENDPOINT}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ question }),
    });

    if (!res.ok) {
      // The backend's raw `message` (validation detail, stack-trace-adjacent text, etc.) is
      // logged for debugging but never shown to visitors - see friendlyErrorMessage below.
      let detail = `Request failed (${res.status})`;
      try {
        const body = await res.json();
        if (body && body.message) detail = body.message;
      } catch (_) {
        /* non-JSON error body, keep default detail */
      }
      throw new AskApiError(detail, res.status);
    }

    const data = await res.json();
    return data.answer;
  }

  function friendlyErrorMessage(err) {
    if (err instanceof AskApiError && err.status === 429) return RATE_LIMITED_MESSAGE;
    return AI_UNAVAILABLE_MESSAGE;
  }

  async function submitQuestion() {
    const question = chatInput.value.trim();
    if (!question || chatPending) return;

    appendUserMessage(question);
    chatInput.value = "";
    setChatPending(true);
    appendTypingIndicator();

    try {
      const answer = await askBackend(question);
      removeTypingIndicator();
      appendBotMessage(answer || "I don't have an answer for that right now.");
    } catch (err) {
      console.error("Ask AI request failed:", err);
      removeTypingIndicator();
      appendBotMessage(friendlyErrorMessage(err), true);
      showToast("Couldn't reach the AI assistant", "danger");
    } finally {
      setChatPending(false);
      chatInput.focus();
    }
  }

  chatForm.addEventListener("submit", (e) => {
    e.preventDefault();
    submitQuestion();
  });

  /* ============================== init ============================== */

  initTheme();
  loadEnvConfig();
})();
