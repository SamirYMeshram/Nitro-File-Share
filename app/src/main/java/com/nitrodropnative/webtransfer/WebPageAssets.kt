package com.nitrodropnative.webtransfer

internal object WebPageAssets {
    fun passwordHtml(error: String = ""): String {
        val safeError = escape(error)
        return """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>NitroDrop Password</title>
  <style>
    :root { color-scheme: dark; --bg:#071014; --card:rgba(255,255,255,.08); --line:rgba(255,255,255,.13); --text:#f2fbff; --muted:#8fa5af; --cyan:#60f2ff; --bad:#ff7b7b; }
    * { box-sizing:border-box; }
    body { margin:0; min-height:100vh; display:grid; place-items:center; font-family:Inter,ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Arial; background:radial-gradient(circle at 15% 0%, rgba(96,242,255,.18), transparent 34%), radial-gradient(circle at 85% 10%, rgba(125,255,165,.10), transparent 28%), var(--bg); color:var(--text); }
    .card { width:min(420px, calc(100% - 32px)); border:1px solid var(--line); border-radius:28px; background:linear-gradient(135deg, rgba(255,255,255,.12), rgba(255,255,255,.05)); box-shadow:0 24px 80px rgba(0,0,0,.32); padding:24px; backdrop-filter:blur(14px); }
    .tag { color:var(--muted); font-weight:650; text-transform:uppercase; letter-spacing:.14em; font-size:12px; }
    h1 { margin:8px 0 8px; font-size:42px; letter-spacing:-.06em; line-height:.95; }
    p { color:var(--muted); line-height:1.55; }
    input { width:100%; height:58px; border-radius:18px; border:1px solid var(--line); background:rgba(255,255,255,.07); color:var(--text); padding:0 18px; font-size:28px; text-align:center; letter-spacing:.25em; font-weight:900; }
    button { width:100%; margin-top:14px; border:0; border-radius:18px; background:var(--cyan); color:#041216; font-weight:900; min-height:52px; padding:0 18px; cursor:pointer; }
    .bad { color:var(--bad); min-height:22px; }
  </style>
</head>
<body>
  <form class="card" method="post" action="/auth" autocomplete="off">
    <div class="tag">NitroDrop Native</div>
    <h1>Enter code</h1>
    <p>Type the 3-digit password shown on the phone to open this transfer session.</p>
    <input name="password" inputmode="numeric" pattern="[0-9]{3}" maxlength="3" autofocus required />
    <button type="submit">Unlock</button>
    <p class="bad">$safeError</p>
  </form>
</body>
</html>
""".trimIndent()
    }

    fun indexHtml(): String {
        return """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>NitroDrop Web Transfer</title>
  <style>
    :root { color-scheme: dark; --bg:#071014; --card:rgba(255,255,255,.08); --line:rgba(255,255,255,.12); --text:#f2fbff; --muted:#8fa5af; --cyan:#60f2ff; --green:#7DFFA5; --bad:#ff7b7b; --warn:#ffd37d; }
    * { box-sizing:border-box; }
    body { margin:0; min-height:100vh; font-family:Inter,ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Arial; background:radial-gradient(circle at 15% 0%, rgba(96,242,255,.18), transparent 34%), radial-gradient(circle at 85% 10%, rgba(125,255,165,.10), transparent 28%), var(--bg); color:var(--text); }
    main { width:min(1040px, 100%); margin:0 auto; padding:32px 18px; }
    header { display:flex; justify-content:space-between; align-items:flex-start; gap:18px; margin-bottom:22px; }
    h1 { margin:0; font-size:clamp(34px, 7vw, 64px); letter-spacing:-.06em; line-height:.95; }
    h2 { margin:0; font-size:22px; }
    p { color:var(--muted); line-height:1.55; }
    .tag { color:var(--muted); font-weight:650; text-transform:uppercase; letter-spacing:.14em; font-size:12px; }
    .grid { display:grid; grid-template-columns:1.25fr .75fr; gap:16px; }
    @media (max-width:760px) { .grid { grid-template-columns:1fr; } header { display:block; } .toolbar { align-items:stretch; } .toolbar button { width:100%; } }
    .card { border:1px solid var(--line); border-radius:28px; background:linear-gradient(135deg, rgba(255,255,255,.12), rgba(255,255,255,.05)); box-shadow:0 24px 80px rgba(0,0,0,.32); padding:20px; backdrop-filter:blur(14px); }
    .toolbar { display:flex; flex-wrap:wrap; gap:10px; justify-content:space-between; align-items:center; margin:14px 0 12px; }
    .toolbar .left, .toolbar .right { display:flex; flex-wrap:wrap; gap:10px; align-items:center; }
    .file { display:grid; grid-template-columns:minmax(0,1fr) auto; gap:12px; align-items:center; padding:14px; border:1px solid var(--line); border-radius:20px; margin:10px 0; background:rgba(255,255,255,.04); transition:opacity .2s, border-color .2s, background .2s; }
    .file.downloaded { border-color:rgba(125,255,165,.38); background:rgba(125,255,165,.07); }
    .file strong { display:block; overflow-wrap:anywhere; }
    .name-row { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
    .meta { color:var(--muted); font-size:13px; margin-top:4px; }
    .badge { display:none; color:#041216; background:var(--green); border-radius:999px; padding:4px 8px; font-size:12px; font-weight:900; white-space:nowrap; }
    .downloaded .badge { display:inline-flex; align-items:center; gap:4px; }
    button, .button { border:0; border-radius:18px; background:var(--cyan); color:#041216; font-weight:850; min-height:46px; padding:0 18px; cursor:pointer; text-decoration:none; display:inline-flex; align-items:center; justify-content:center; white-space:nowrap; }
    button.secondary, .button.secondary { background:rgba(255,255,255,.10); color:var(--text); border:1px solid var(--line); }
    button.ghost { background:transparent; color:var(--muted); border:1px solid var(--line); }
    button:disabled { opacity:.48; cursor:not-allowed; }
    input[type=file] { width:100%; border:1px dashed var(--line); border-radius:20px; padding:18px; color:var(--muted); background:rgba(255,255,255,.04); }
    .speed { font-size:clamp(42px, 10vw, 86px); font-weight:950; letter-spacing:-.08em; color:var(--cyan); line-height:.9; }
    .small { font-size:13px; color:var(--muted); }
    .bar { width:100%; height:12px; border-radius:999px; background:rgba(255,255,255,.10); overflow:hidden; margin:14px 0; }
    .fill { height:100%; width:0%; border-radius:999px; background:linear-gradient(90deg, var(--cyan), var(--green)); transition:width .2s linear; }
    .row { display:flex; justify-content:space-between; gap:12px; align-items:center; }
    .stats { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; }
    .stat { border:1px solid var(--line); background:rgba(255,255,255,.04); border-radius:18px; padding:14px; }
    .stat b { display:block; font-size:18px; margin-top:6px; }
    .warn { color:var(--warn); }
    .ok { color:var(--green); }
    .bad { color:var(--bad); }
    .count { color:var(--muted); font-size:13px; }
  </style>
</head>
<body>
<main>
  <header>
    <div>
      <div class="tag">NitroDrop Native</div>
      <h1>Web Transfer</h1>
      <p>Phone ↔ PC transfer over your local Wi‑Fi. No cloud, no PC app, no account.</p>
    </div>
    <div class="card"><div class="tag">Session</div><p class="ok">Unlocked on this browser</p><p class="small">Stop the server on the phone when finished.</p></div>
  </header>

  <section class="grid">
    <div class="card">
      <div class="row"><h2>Download phone files</h2><span class="count" id="fileCount">0 files</span></div>
      <div class="toolbar">
        <div class="left">
          <button id="downloadAll">Download all missing</button>
          <button class="secondary" id="refreshFiles">Refresh</button>
        </div>
        <div class="right">
          <button class="ghost" id="resetDownloaded">Reset downloaded marks</button>
        </div>
      </div>
      <div id="files"></div>
      <p class="small">Downloaded files are marked with ✓. “Download all missing” skips marked files to avoid duplicate downloads.</p>
    </div>

    <div class="card">
      <h2>Upload files to phone</h2>
      <p class="small">Choose files on this computer and send them to Downloads/NitroDrop on the phone.</p>
      <input id="upload" type="file" multiple />
      <p class="small">Files are written as .nitro_part first, then finalized after completion.</p>
      <button id="startUpload">Start upload</button>
    </div>
  </section>

  <section class="card" style="margin-top:16px;">
    <div class="row"><div><div class="tag">Live speed</div><div class="speed"><span id="speed">0.00</span></div><div class="small">MB/s</div></div><div id="status" class="small">Idle</div></div>
    <div class="bar"><div id="fill" class="fill"></div></div>
    <div class="stats">
      <div class="stat"><span class="small">Progress</span><b id="progress">0%</b></div>
      <div class="stat"><span class="small">Transferred</span><b id="bytes">0 B</b></div>
      <div class="stat"><span class="small">ETA</span><b id="eta">--</b></div>
    </div>
  </section>
</main>
<script>
const CHUNK_SIZE = 16 * 1024 * 1024;
const filesEl = document.getElementById('files');
const fileCountEl = document.getElementById('fileCount');
const uploadEl = document.getElementById('upload');
const startUploadEl = document.getElementById('startUpload');
const refreshFilesEl = document.getElementById('refreshFiles');
const downloadAllEl = document.getElementById('downloadAll');
const resetDownloadedEl = document.getElementById('resetDownloaded');
const speedEl = document.getElementById('speed');
const fillEl = document.getElementById('fill');
const progressEl = document.getElementById('progress');
const bytesEl = document.getElementById('bytes');
const etaEl = document.getElementById('eta');
const statusEl = document.getElementById('status');
let lastBytes = 0;
let lastTs = performance.now();
let knownFileFingerprint = '';
let currentFiles = [];
let sessionKey = 'default';
let downloadAllBusy = false;

function fmtBytes(bytes) {
  const units = ['B','KB','MB','GB','TB']; let value = Math.max(0, bytes || 0); let i = 0;
  while (value >= 1024 && i < units.length - 1) { value /= 1024; i++; }
  return i === 0 ? Math.round(value) + ' B' : value.toFixed(2) + ' ' + units[i];
}
function eta(sec) { if (!isFinite(sec) || sec < 0) return '--'; const m=Math.floor(sec/60), s=Math.floor(sec%60); return m>0 ? m + 'm ' + s + 's' : s + 's'; }
function setProgress(done, total, label) {
  const now = performance.now();
  const diff = Math.max(0, done - lastBytes);
  const dt = Math.max(1, now - lastTs) / 1000;
  if (now - lastTs >= 220 || done === total) {
    const bps = diff / dt;
    speedEl.textContent = (bps / 1024 / 1024).toFixed(2);
    lastBytes = done; lastTs = now;
    const pct = total > 0 ? Math.min(100, done / total * 100) : 0;
    fillEl.style.width = pct.toFixed(1) + '%';
    progressEl.textContent = Math.floor(pct) + '%';
    bytesEl.textContent = fmtBytes(done) + ' / ' + fmtBytes(total);
    etaEl.textContent = eta(bps > 0 ? (total - done) / bps : -1);
    statusEl.textContent = label;
  }
}
function storageKey() { return 'nitrodrop_downloaded_' + sessionKey; }
function localDownloadedSet() {
  try { return new Set(JSON.parse(localStorage.getItem(storageKey()) || '[]')); }
  catch (_) { return new Set(); }
}
function saveLocalDownloaded(set) {
  try { localStorage.setItem(storageKey(), JSON.stringify(Array.from(set))); } catch (_) {}
}
function isDownloaded(file) {
  return !!file.downloaded || localDownloadedSet().has(file.id);
}
function markDownloaded(id) {
  const set = localDownloadedSet();
  set.add(id);
  saveLocalDownloaded(set);
}
function clearLocalDownloaded() {
  try { localStorage.removeItem(storageKey()); } catch (_) {}
}
function downloadUrl(file) {
  return '/d/' + encodeURIComponent(file.id);
}
function updateDownloadAllButton() {
  const missing = currentFiles.filter(f => !isDownloaded(f)).length;
  downloadAllEl.disabled = downloadAllBusy || currentFiles.length === 0 || missing === 0;
  downloadAllEl.textContent = missing > 0 ? 'Download all missing (' + missing + ')' : 'All downloaded';
  fileCountEl.textContent = currentFiles.length + (currentFiles.length === 1 ? ' file' : ' files');
}
function renderFiles(files) {
  currentFiles = files || [];
  filesEl.innerHTML = '';
  if (!currentFiles.length) {
    filesEl.innerHTML = '<p class="warn">No phone files selected in the Android app. Upload from PC still works.</p>';
    updateDownloadAllButton();
    return;
  }
  for (const f of currentFiles) {
    const downloaded = isDownloaded(f);
    const row = document.createElement('div');
    row.className = 'file' + (downloaded ? ' downloaded' : '');
    const left = document.createElement('div');
    const nameRow = document.createElement('div');
    nameRow.className = 'name-row';
    const name = document.createElement('strong');
    name.textContent = f.name || 'nitrodrop_file';
    const badge = document.createElement('span');
    badge.className = 'badge';
    badge.textContent = '✓ Downloaded';
    nameRow.appendChild(name);
    nameRow.appendChild(badge);
    const meta = document.createElement('div');
    meta.className = 'meta';
    meta.textContent = fmtBytes(f.size) + ' • ' + (f.mimeType || 'application/octet-stream');
    left.appendChild(nameRow);
    left.appendChild(meta);
    const link = document.createElement('a');
    link.className = 'button' + (downloaded ? ' secondary' : '');
    link.textContent = downloaded ? 'Download again' : 'Download';
    link.href = downloadUrl(f);
    link.addEventListener('click', () => {
      markDownloaded(f.id);
      setTimeout(() => renderFiles(currentFiles), 150);
      statusEl.textContent = 'Download started: ' + (f.name || 'file');
    });
    row.appendChild(left);
    row.appendChild(link);
    filesEl.appendChild(row);
  }
  updateDownloadAllButton();
}
async function loadFiles(force) {
  try {
    const res = await fetch('/api/files', { cache: 'no-store' });
    if (res.status === 401 || res.status === 403) { location.reload(); return; }
    if (!res.ok) throw new Error('File list error ' + res.status);
    const data = await res.json();
    if (data.sessionId && data.sessionId !== sessionKey) {
      sessionKey = data.sessionId;
      knownFileFingerprint = '';
    }
    const files = data.files || [];
    const nextFingerprint = JSON.stringify(files.map(f => [f.id, f.name, f.size, f.mimeType, !!f.downloaded]));
    if (force || nextFingerprint !== knownFileFingerprint) {
      knownFileFingerprint = nextFingerprint;
      renderFiles(files);
      if (data.updatedAt) statusEl.textContent = 'File list updated';
    } else {
      currentFiles = files;
      updateDownloadAllButton();
    }
  } catch (err) {
    statusEl.innerHTML = '<span class="bad">' + String(err.message || err) + '</span>';
  }
}
function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }
async function downloadAllMissing() {
  if (downloadAllBusy) return;
  const missing = currentFiles.filter(f => !isDownloaded(f));
  if (!missing.length) {
    statusEl.innerHTML = '<span class="ok">No missing files. Everything is already marked downloaded.</span>';
    updateDownloadAllButton();
    return;
  }
  downloadAllBusy = true;
  updateDownloadAllButton();
  try {
    for (let i = 0; i < missing.length; i++) {
      const f = missing[i];
      statusEl.textContent = 'Starting download ' + (i + 1) + ' of ' + missing.length + ': ' + (f.name || 'file');
      markDownloaded(f.id);
      renderFiles(currentFiles);
      const a = document.createElement('a');
      a.href = downloadUrl(f);
      a.download = '';
      a.style.display = 'none';
      document.body.appendChild(a);
      a.click();
      setTimeout(() => a.remove(), 5000);
      await sleep(1200);
    }
    statusEl.innerHTML = '<span class="ok">Download all missing started. Already marked files were skipped.</span>';
  } finally {
    downloadAllBusy = false;
    updateDownloadAllButton();
    setTimeout(() => loadFiles(true), 2500);
  }
}
async function resetDownloadedMarks() {
  clearLocalDownloaded();
  try { await fetch('/api/downloaded/reset', { method: 'POST', cache: 'no-store' }); } catch (_) {}
  statusEl.textContent = 'Downloaded marks reset';
  await loadFiles(true);
}
function putChunk(file, sessionId, offset, chunk) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const url = '/u?sessionId=' + encodeURIComponent(sessionId) + '&fileName=' + encodeURIComponent(file.name) + '&fileSize=' + file.size + '&offset=' + offset;
    xhr.open('PUT', url, true);
    xhr.setRequestHeader('Content-Type', 'application/octet-stream');
    xhr.onload = () => xhr.status >= 200 && xhr.status < 300 ? resolve(xhr.responseText) : reject(new Error(xhr.status + ' ' + xhr.responseText));
    xhr.onerror = () => reject(new Error('Network error'));
    xhr.upload.onprogress = ev => { if (ev.lengthComputable) setProgress(offset + ev.loaded, file.size, 'Uploading ' + file.name); };
    xhr.send(chunk);
  });
}
async function uploadFile(file) {
  const sessionId = crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random().toString(16).slice(2);
  let offset = 0;
  lastBytes = 0; lastTs = performance.now();
  while (offset < file.size) {
    const end = Math.min(file.size, offset + CHUNK_SIZE);
    await putChunk(file, sessionId, offset, file.slice(offset, end));
    offset = end;
    setProgress(offset, file.size, 'Uploading ' + file.name);
  }
  setProgress(file.size, file.size, 'Completed ' + file.name);
}
startUploadEl.addEventListener('click', async () => {
  const list = Array.from(uploadEl.files || []);
  if (!list.length) { statusEl.textContent = 'Choose files first'; return; }
  startUploadEl.disabled = true;
  try { for (const file of list) await uploadFile(file); statusEl.innerHTML = '<span class="ok">Upload complete</span>'; }
  catch (err) { statusEl.innerHTML = '<span class="bad">' + String(err.message || err) + '</span>'; }
  finally { startUploadEl.disabled = false; }
});
refreshFilesEl.addEventListener('click', () => loadFiles(true));
downloadAllEl.addEventListener('click', downloadAllMissing);
resetDownloadedEl.addEventListener('click', resetDownloadedMarks);
loadFiles(true);
setInterval(() => loadFiles(false), 2000);
</script>
</body>
</html>
""".trimIndent()
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
