(() => {
  "use strict";

  const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB per part, comfortably under the 25MB server-side multipart limit
  const SESSION_KEY = "askaboutme_admin_session";
  const AUTO_REFRESH_MS = 15000;

  /* ============================== state ============================== */

  const state = {
    apiBase: "",
    authHeader: "",
    username: "",
    files: [],
    search: "",
  };

  let autoRefreshTimer = null;

  /* ============================== dom refs ============================== */

  const el = {
    loginScreen: document.getElementById("login-screen"),
    loginForm: document.getElementById("login-form"),
    apiBaseInput: document.getElementById("api-base"),
    usernameInput: document.getElementById("username"),
    passwordInput: document.getElementById("password"),
    loginError: document.getElementById("login-error"),
    loginSubmit: document.getElementById("login-submit"),

    dashboard: document.getElementById("dashboard"),
    whoami: document.getElementById("whoami"),
    logoutBtn: document.getElementById("logout-btn"),
    refreshBtn: document.getElementById("refresh-btn"),

    dropzone: document.getElementById("dropzone"),
    fileInput: document.getElementById("file-input"),
    browseBtn: document.getElementById("browse-btn"),
    uploadProgressList: document.getElementById("upload-progress-list"),

    searchInput: document.getElementById("search-input"),
    filesGrid: document.getElementById("files-grid"),
    filesEmpty: document.getElementById("files-empty"),

    confirmModal: document.getElementById("confirm-modal"),
    confirmMessage: document.getElementById("confirm-message"),
    confirmCancel: document.getElementById("confirm-cancel"),
    confirmDelete: document.getElementById("confirm-delete"),

    toastContainer: document.getElementById("toast-container"),
  };

  /* ============================== api layer ============================== */

  class ApiError extends Error {}

  async function apiRequest(path, options = {}) {
    let response;
    try {
      response = await fetch(state.apiBase + path, {
        ...options,
        headers: {
          Authorization: state.authHeader,
          ...(options.headers || {}),
        },
      });
    } catch (networkErr) {
      throw new ApiError(
        `Could not reach ${state.apiBase} - check the backend URL and that the server is running.`
      );
    }

    if (response.status === 401) {
      endSession();
      throw new ApiError("Unauthorized");
    }

    if (!response.ok) {
      let message = `Request failed (${response.status})`;
      try {
        const body = await response.json();
        if (body && body.message) message = body.message;
      } catch (_) {
        /* body wasn't JSON - keep the generic message */
      }
      throw new ApiError(message);
    }

    if (response.status === 204) return null;
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }

  const api = {
    listFiles: () => apiRequest("/api/v1/files"),

    initiateUpload: (fileName, contentType, fileSize) =>
      apiRequest("/api/v1/files/initiate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fileName, contentType, fileSize }),
      }),

    uploadPart: (fileId, partNumber, blob) => {
      const form = new FormData();
      form.append("file", blob, "chunk");
      return apiRequest(`/api/v1/files/${fileId}/parts/${partNumber}`, {
        method: "POST",
        body: form,
      });
    },

    completeUpload: (fileId, parts) =>
      apiRequest(`/api/v1/files/${fileId}/complete`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ parts }),
      }),

    deleteFile: (fileId) =>
      apiRequest(`/api/v1/files/${fileId}`, { method: "DELETE" }),
  };

  /* ============================== .env config ============================== */

  /**
   * Static frontend, no build step - so there's no bundler to inject a real .env at build time.
   * Instead we fetch the plain-text .env file at runtime (same origin as index.html, served
   * alongside it) and parse simple KEY=VALUE lines. Falls back silently to the blank/default
   * login form if .env is missing (e.g. a fresh checkout that only has .env.example).
   */
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
      if (!res.ok) return null;
      return parseEnv(await res.text());
    } catch (_) {
      return null;
    }
  }

  /* ============================== session ============================== */

  function startSession({ apiBase, username, password }) {
    state.apiBase = apiBase.replace(/\/+$/, "");
    state.username = username;
    state.authHeader = "Basic " + btoa(`${username}:${password}`);
    sessionStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ apiBase: state.apiBase, username, authHeader: state.authHeader })
    );
  }

  function endSession() {
    sessionStorage.removeItem(SESSION_KEY);
    state.apiBase = "";
    state.authHeader = "";
    state.username = "";
    state.files = [];
    stopAutoRefresh();
    showDashboard(false);
  }

  function restoreSession() {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return false;
    try {
      const saved = JSON.parse(raw);
      state.apiBase = saved.apiBase;
      state.username = saved.username;
      state.authHeader = saved.authHeader;
      return true;
    } catch (_) {
      return false;
    }
  }

  /* ============================== view toggling ============================== */

  function showDashboard(show) {
    el.dashboard.hidden = !show;
    el.loginScreen.hidden = show;
    if (show) {
      el.whoami.textContent = state.username;
    }
  }

  /* ============================== login flow ============================== */

  el.loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    setLoginBusy(true);
    hideLoginError();

    const apiBase = el.apiBaseInput.value.trim().replace(/\/+$/, "");
    const username = el.usernameInput.value.trim();
    const password = el.passwordInput.value;

    if (!apiBase || !username || !password) {
      setLoginBusy(false);
      showLoginError("All fields are required.");
      return;
    }

    startSession({ apiBase, username, password });

    try {
      const files = await api.listFiles();
      state.files = files || [];
      showDashboard(true);
      renderFiles();
      startAutoRefresh();
    } catch (err) {
      endSession();
      const message = err.message === "Unauthorized" ? "Invalid username or password." : err.message;
      showLoginError(message || "Login failed.");
    } finally {
      setLoginBusy(false);
    }
  });

  function setLoginBusy(busy) {
    el.loginSubmit.disabled = busy;
    el.loginSubmit.querySelector(".btn-label").hidden = busy;
    el.loginSubmit.querySelector(".spinner").hidden = !busy;
  }

  function showLoginError(msg) {
    el.loginError.textContent = msg;
    el.loginError.hidden = false;
  }

  function hideLoginError() {
    el.loginError.hidden = true;
  }

  el.logoutBtn.addEventListener("click", () => {
    endSession();
  });

  /* ============================== file list rendering ============================== */

  const EXT_ICON_PATH =
    '<path d="M14 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10Z"/><path d="M14 4v6h6"/>';

  function formatBytes(bytes) {
    if (bytes == null) return "-";
    if (bytes === 0) return "0 B";
    const units = ["B", "KB", "MB", "GB"];
    const i = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
    return `${(bytes / 1024 ** i).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
  }

  function formatDate(iso) {
    if (!iso) return "-";
    const d = new Date(iso);
    return d.toLocaleString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  function statusBadgeClass(status) {
    switch (status) {
      case "COMPLETED":
        return "badge-success";
      case "FAILED":
        return "badge-danger";
      case "UPLOADING":
      case "PROCESSING":
        return "badge-warning";
      default:
        return "badge-neutral";
    }
  }

  function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str || "";
    return div.innerHTML;
  }

  function renderFiles() {
    const query = state.search.trim().toLowerCase();
    const filtered = query
      ? state.files.filter((f) => f.fileName.toLowerCase().includes(query))
      : state.files;

    el.filesGrid.innerHTML = "";
    el.filesEmpty.hidden = state.files.length > 0;

    if (state.files.length > 0 && filtered.length === 0) {
      el.filesGrid.innerHTML = `<p style="color:var(--text-muted);font-size:13px;">No files match "${escapeHtml(
        query
      )}".</p>`;
      return;
    }

    for (const file of filtered) {
      el.filesGrid.appendChild(fileCard(file));
    }
  }

  function fileCard(file) {
    const card = document.createElement("div");
    card.className = "file-card";
    card.innerHTML = `
      <div class="file-card-top">
        <span class="file-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">${EXT_ICON_PATH}</svg></span>
        <div class="file-meta">
          <div class="file-name" title="${escapeHtml(file.fileName)}">${escapeHtml(file.fileName)}</div>
          <div class="file-sub">${formatBytes(file.fileSize)} &middot; ${formatDate(file.createdAt)}</div>
        </div>
      </div>
      <div class="badge-row">
        <span class="badge ${statusBadgeClass(file.status)}">${escapeHtml(file.status)}</span>
        <span class="badge ${statusBadgeClass(file.embeddedStatus)}">embed: ${escapeHtml(file.embeddedStatus)}</span>
      </div>
      <div class="file-card-actions">
        <button class="btn btn-ghost btn-preview" ${file.previewUrl ? "" : "disabled"}>Preview</button>
        <button class="btn btn-danger btn-delete">Delete</button>
      </div>
    `;

    card.querySelector(".btn-preview").addEventListener("click", () => {
      if (file.previewUrl) window.open(file.previewUrl, "_blank", "noopener");
    });
    card.querySelector(".btn-delete").addEventListener("click", () => confirmDelete(file));

    return card;
  }

  el.searchInput.addEventListener("input", (e) => {
    state.search = e.target.value;
    renderFiles();
  });

  async function refreshFiles({ silent = false } = {}) {
    try {
      const files = await api.listFiles();
      state.files = files || [];
      renderFiles();
    } catch (err) {
      if (err.message === "Unauthorized") {
        showToast("Session expired. Please log in again.", "error");
      } else if (!silent) {
        showToast(err.message || "Failed to load files.", "error");
      }
    }
  }

  el.refreshBtn.addEventListener("click", () => refreshFiles());

  function startAutoRefresh() {
    stopAutoRefresh();
    autoRefreshTimer = setInterval(() => refreshFiles({ silent: true }), AUTO_REFRESH_MS);
  }

  function stopAutoRefresh() {
    if (autoRefreshTimer) clearInterval(autoRefreshTimer);
    autoRefreshTimer = null;
  }

  /* ============================== delete flow ============================== */

  let pendingDeleteFile = null;

  function confirmDelete(file) {
    pendingDeleteFile = file;
    el.confirmMessage.textContent = `"${file.fileName}" will be removed from storage and the vector store. This cannot be undone.`;
    el.confirmModal.hidden = false;
  }

  function closeConfirm() {
    el.confirmModal.hidden = true;
    pendingDeleteFile = null;
  }

  el.confirmCancel.addEventListener("click", closeConfirm);
  el.confirmModal.addEventListener("click", (e) => {
    if (e.target === el.confirmModal) closeConfirm();
  });

  el.confirmDelete.addEventListener("click", async () => {
    if (!pendingDeleteFile) return;
    const file = pendingDeleteFile;
    el.confirmDelete.disabled = true;

    try {
      await api.deleteFile(file.id);
      state.files = state.files.filter((f) => f.id !== file.id);
      renderFiles();
      showToast(`"${file.fileName}" deleted.`, "success");
    } catch (err) {
      const message = err.message === "Unauthorized" ? "Session expired. Please log in again." : err.message;
      showToast(message || "Failed to delete file.", "error");
    } finally {
      el.confirmDelete.disabled = false;
      closeConfirm();
    }
  });

  /* ============================== upload flow ============================== */

  el.browseBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    el.fileInput.click();
  });
  el.dropzone.addEventListener("click", (e) => {
    if (e.target.closest(".upload-progress-list")) return;
    el.fileInput.click();
  });

  el.fileInput.addEventListener("change", () => {
    handleFiles(el.fileInput.files);
    el.fileInput.value = "";
  });

  ["dragenter", "dragover"].forEach((evt) =>
    el.dropzone.addEventListener(evt, (e) => {
      e.preventDefault();
      el.dropzone.classList.add("drag-over");
    })
  );
  ["dragleave", "drop"].forEach((evt) =>
    el.dropzone.addEventListener(evt, (e) => {
      e.preventDefault();
      el.dropzone.classList.remove("drag-over");
    })
  );
  el.dropzone.addEventListener("drop", (e) => {
    if (e.dataTransfer.files.length) handleFiles(e.dataTransfer.files);
  });

  function handleFiles(fileList) {
    Array.from(fileList).forEach((file) => {
      if (file.size === 0) {
        showToast(`"${file.name}" is empty and can't be uploaded.`, "error");
        return;
      }
      uploadFile(file);
    });
  }

  async function uploadFile(file) {
    const progress = createProgressItem(file.name);

    try {
      const init = await api.initiateUpload(file.name, file.type || "application/octet-stream", file.size);
      const totalChunks = Math.max(1, Math.ceil(file.size / CHUNK_SIZE));
      const parts = [];

      for (let i = 0; i < totalChunks; i++) {
        const start = i * CHUNK_SIZE;
        const chunk = file.slice(start, start + CHUNK_SIZE);
        const result = await api.uploadPart(init.fileId, i + 1, chunk);
        parts.push({ partNumber: result.partNumber, etag: result.etag });
        progress.update(Math.round(((i + 1) / totalChunks) * 95)); // hold back 5% for /complete
      }

      await api.completeUpload(init.fileId, parts);
      progress.done();
      showToast(`"${file.name}" uploaded.`, "success");
      refreshFiles();
    } catch (err) {
      const message = err.message === "Unauthorized" ? "Session expired. Please log in again." : err.message;
      progress.error(message || "Upload failed.");
      showToast(`Failed to upload "${file.name}": ${message || "unknown error"}`, "error");
    }
  }

  function createProgressItem(name) {
    const item = document.createElement("div");
    item.className = "upload-progress-item";
    item.innerHTML = `
      <div class="upload-progress-top">
        <span class="upload-progress-name" title="${escapeHtml(name)}">${escapeHtml(name)}</span>
        <span class="upload-progress-pct">0%</span>
      </div>
      <div class="upload-progress-track"><div class="upload-progress-fill"></div></div>
      <div class="upload-progress-error-text" hidden></div>
    `;
    el.uploadProgressList.prepend(item);

    const fill = item.querySelector(".upload-progress-fill");
    const pct = item.querySelector(".upload-progress-pct");
    const errText = item.querySelector(".upload-progress-error-text");

    return {
      update(percent) {
        fill.style.width = percent + "%";
        pct.textContent = percent + "%";
      },
      done() {
        fill.style.width = "100%";
        pct.textContent = "Done";
        item.classList.add("done");
        setTimeout(() => item.remove(), 4000);
      },
      error(message) {
        item.classList.add("error");
        errText.textContent = message;
        errText.hidden = false;
      },
    };
  }

  /* ============================== toasts ============================== */

  function showToast(message, type = "info") {
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    el.toastContainer.appendChild(toast);
    setTimeout(() => toast.remove(), 5000);
  }

  /* ============================== init ============================== */

  (async function init() {
    const env = await loadEnvConfig();
    if (env) {
      if (env.API_BASE) el.apiBaseInput.value = env.API_BASE;
      if (env.ADMIN_USERNAME) el.usernameInput.value = env.ADMIN_USERNAME;
      if (env.ADMIN_PASSWORD) el.passwordInput.value = env.ADMIN_PASSWORD;
    }

    if (restoreSession()) {
      try {
        const files = await api.listFiles();
        state.files = files || [];
        showDashboard(true);
        renderFiles();
        startAutoRefresh();
        return;
      } catch (_) {
        endSession();
      }
    }

    showDashboard(false);

    // Auto sign in when .env supplies full credentials, so the form doesn't have to be
    // submitted by hand on every load. The fields stay pre-filled either way, so a wrong/missing
    // .env value just leaves the user on the (now pre-filled) login form to fix and submit manually.
    if (env && env.API_BASE && env.ADMIN_USERNAME && env.ADMIN_PASSWORD) {
      if (el.loginForm.requestSubmit) {
        el.loginForm.requestSubmit();
      } else {
        el.loginForm.dispatchEvent(new Event("submit", { cancelable: true }));
      }
    }
  })();
})();
