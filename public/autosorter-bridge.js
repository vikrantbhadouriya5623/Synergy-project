/* =====================================================================
 * autosorter-bridge.js
 * Connects the AutoSorter dashboard to the Java backend over HTTP + JSON (REST).
 * Loaded AFTER the page's own script, so it overrides the simulated functions
 * (triggerStartup, setMachineState, testFeedNext, triggerCalibration, confirmLogout)
 * with real network calls. No HTML structure is changed.
 * ===================================================================== */
(function () {
  'use strict';

  // Same-origin when served by the Java server; falls back to localhost:8080 if opened as a file.
  const API = window.AUTOSORTER_API !== undefined
    ? window.AUTOSORTER_API
    : (location.protocol.startsWith('http') ? '' : 'http://localhost:8080');

  const TABS = ['tab-dashboard', 'tab-machine-status', 'tab-colour-detection', 'tab-sensors', 'tab-components', 'tab-oop'];
  const $ = (id) => document.getElementById(id);
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  let token = null;
  let pollTimer = null;
  let polling = false;
  let lastServerState = 'RUNNING';
  let renderedState = null;
  let renderedStage = -1;
  let componentsLoaded = false;
  let backendDown = false;

  // Keep the page's original renderer for the status badge / banner / animation pause.
  const renderState = window.setMachineState;
  const originalSwitchTab = window.switchTab;

  /* ------------------------------ HTTP helper ------------------------------ */
  async function api(path, method, body) {
    method = method || 'GET';
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = 'Bearer ' + token;
    const res = await fetch(API + path, {
      method, headers,
      body: method === 'GET' ? undefined : JSON.stringify(body || {}),
    });
    let data = null;
    try { data = await res.json(); } catch (_) { /* empty body */ }
    if (res.status === 401 && token) forceLogout(data && data.message);
    if (!res.ok) throw new Error((data && data.message) || ('HTTP ' + res.status));
    return data;
  }

  function toast(msg, bad) {
    let t = $('ab-toast');
    if (!t) {
      t = document.createElement('div');
      t.id = 'ab-toast';
      t.style.cssText = 'position:fixed;right:16px;bottom:16px;z-index:100;max-width:360px;padding:10px 14px;' +
        'border-radius:3px;font:600 12px "JetBrains Mono",monospace;color:#fff;box-shadow:0 4px 12px rgba(15,23,42,.25);transition:opacity .3s';
      document.body.appendChild(t);
    }
    t.style.background = bad ? '#DC2626' : '#0F172A';
    t.style.opacity = '1';
    t.textContent = msg;
    clearTimeout(t._h);
    t._h = setTimeout(() => { t.style.opacity = '0'; }, 3200);
  }

  const setText = (id, v) => { const el = $(id); if (el && el.textContent !== String(v)) el.textContent = v; };

  /* ------------------------------ LOGIN / LOGOUT ------------------------------ */
  window.triggerStartup = async function () {
    const email = $('login-email').value.trim();
    const pass = $('login-password').value.trim();
    const alertBox = $('login-alert');
    const alertText = alertBox.querySelector('span:last-child');
    if (!email || !pass) {
      alertText.textContent = 'Please enter valid terminal credentials.';
      alertBox.classList.remove('hidden');
      return;
    }
    try {
      const r = await api('/api/auth/login', 'POST', { email: email, password: pass });
      token = r.token;
      alertBox.classList.add('hidden');
    } catch (e) {
      alertText.textContent = e.message === 'Failed to fetch' ? 'Backend unreachable - is the Java server running?' : e.message;
      alertBox.classList.remove('hidden');
      return;
    }
    const modal = $('startup-modal');
    modal.classList.remove('hidden');
    await Promise.all([sleep(1500), pollOnce(true)]);
    modal.classList.add('hidden');
    $('login-view').classList.add('hidden');
    startPolling();
  };

  function forceLogout(msg) {
    stopPolling();
    token = null;
    $('login-view').classList.remove('hidden');
    if (msg) {
      const box = $('login-alert');
      box.querySelector('span:last-child').textContent = msg;
      box.classList.remove('hidden');
    }
  }

  window.confirmLogout = async function () {
    if (!confirm('Disconnect from station and return to login terminal?')) return;
    try { await api('/api/auth/logout', 'POST'); } catch (_) { /* ignore */ }
    forceLogout();
  };

  /* ------------------------------ MACHINE COMMANDS ------------------------------ */
  async function command(path, okMsg) {
    try {
      const r = await api(path, 'POST');
      toast(okMsg || r.message);
      await pollOnce(true);
      return r;
    } catch (e) {
      toast(e.message, true);
      return null;
    }
  }

  // The page calls setMachineState('RUNNING' | 'PAUSED' | 'STOPPED') from its buttons.
  window.setMachineState = function (state) {
    if (state === 'STOPPED') return command('/api/machine/estop');
    if (state === 'PAUSED') return command('/api/machine/pause');
    return command(lastServerState === 'EMERGENCY_HALT' ? '/api/machine/reset' : '/api/machine/start');
  };

  window.testFeedNext = function () { return command('/api/machine/feed-next'); };

  /* ------------------------------ CALIBRATION MODAL ------------------------------ */
  window.triggerCalibration = async function () {
    let result;
    try { result = await api('/api/machine/calibrate', 'POST'); }
    catch (e) { toast(e.message, true); return; }

    const modal = $('calibration-modal');
    const list = $('calib-steps-list');
    const spinner = $('calib-spinner');
    const head = $('calib-header-status');
    const sumText = $('calib-summary-text');
    const pct = $('calib-progress-num');
    const steps = result.steps;

    modal.classList.remove('hidden');
    spinner.classList.add('animate-spin');
    head.textContent = 'RUNNING';
    sumText.textContent = 'Executing mechatronic test pulse...';
    list.innerHTML = steps.map((s, i) =>
      '<div class="flex justify-between items-center text-secondary opacity-50" id="cs-' + i + '"><span>' + s.label +
      '</span><span class="font-bold text-secondary">WAITING</span></div>').join('');

    for (let i = 0; i < steps.length; i++) {
      const row = $('cs-' + i);
      row.classList.remove('opacity-50');
      row.lastElementChild.className = 'font-bold text-amber-500';
      row.lastElementChild.textContent = 'CALIBRATING';
      await sleep(450);
      const ok = steps[i].status === 'OK';
      row.lastElementChild.className = 'font-bold ' + (ok ? 'text-emerald-600' : 'text-red-600');
      row.lastElementChild.textContent = ok ? 'OK' : 'FAILED';
      pct.textContent = Math.round(((i + 1) / steps.length) * 100) + '%';
    }
    spinner.classList.remove('animate-spin');
    head.textContent = 'COMPLETE';
    sumText.textContent = 'All ' + steps.length + ' subsystems calibrated.';
    pollOnce(true);
  };

  window.closeCalibrationModal = function () { $('calibration-modal').classList.add('hidden'); };

  /* ------------------------------ SVG ZONE HIGHLIGHT (was undefined in the page) ------------------------------ */
  window.highlightZone = function (zone) {
    const map = { ultrasonic: 'zone-ultrasonic', optical: 'zone-optical', classification: 'zone-optical',
                  servo: 'zone-servo', bins: 'zone-bins', hopper: 'zone-hopper' };
    const el = $(map[zone]);
    if (!el) return;
    el.style.filter = 'drop-shadow(0 0 6px #FF6B00)';
    setTimeout(() => { el.style.filter = ''; }, 1600);
  };

  /* ------------------------------ TAB SWITCH ------------------------------ */
  window.switchTab = function (tabId) {
    originalSwitchTab(tabId);
    if (token) pollOnce(true);
  };

  function activeTab() {
    for (const t of TABS) {
      const el = $('content-' + t);
      if (el && !el.classList.contains('hidden')) return t;
    }
    return 'tab-dashboard';
  }

  /* ------------------------------ POLLING LOOP ------------------------------ */
  function startPolling() { stopPolling(); pollTimer = setInterval(() => pollOnce(false), 1000); }
  function stopPolling() { if (pollTimer) clearInterval(pollTimer); pollTimer = null; }

  async function pollOnce(force) {
    if (!token || (polling && !force)) return;
    polling = true;
    try {
      const dash = await api('/api/dashboard');
      renderDashboard(dash);
      const tab = activeTab();
      if (tab === 'tab-machine-status') renderMachineStatus(await api('/api/machine/status'));
      else if (tab === 'tab-colour-detection') renderColour(await api('/api/colour'));
      else if (tab === 'tab-sensors') renderSensors(await api('/api/sensors'));
      else if (tab === 'tab-components' && !componentsLoaded) renderComponents(await api('/api/components'));
      if (backendDown) { backendDown = false; renderedState = null; toast('Backend link restored'); }
    } catch (e) {
      if (!backendDown && e.message === 'Failed to fetch') {
        backendDown = true;
        const b = $('top-status-badge');
        b.className = 'flex items-center gap-2 px-2.5 py-1 bg-amber-100 border border-amber-500 text-amber-800 rounded text-label-code font-label-code';
        setText('top-status-text', 'BACKEND LINK LOST - RETRYING');
      }
    } finally {
      polling = false;
    }
  }

  /* ------------------------------ RENDERERS ------------------------------ */
  function renderDashboard(d) {
    // global machine state -> reuse the page's own renderer
    lastServerState = d.machine.state;
    if (renderedState !== d.machine.state) {
      renderedState = d.machine.state;
      renderState(d.machine.state === 'EMERGENCY_HALT' ? 'STOPPED' : d.machine.state);
    }
    setText('active-cycle-display', 'ACTIVE CYCLE: #' + d.pipeline.activeCycle);
    if (renderedStage !== d.pipeline.currentStage) { renderedStage = d.pipeline.currentStage; renderPipeline(d.pipeline); }

    // counters + bins
    setText('total-sorted-counter', Number(d.counters.total).toLocaleString());
    d.counters.bins.forEach((b) => {
      setText('count-' + b.key, b.count);
      setText('pct-' + b.key, b.percent + '%');
      const bar = $('bar-' + b.key);
      if (bar) bar.style.width = b.percent + '%';
    });

    // sensor cards
    const t = d.sensors.tcs3200, u = d.sensors.hcsr04, p = d.sensors.pir;
    const tcs = $('card-tcs-color');
    if (tcs) tcs.innerHTML = '<span class="w-3 h-3 rounded-full" style="background-color:' + t.classificationHex + '"></span> ' + t.rgb.display;
    setText('card-tcs-class', t.classification);
    setText('card-ultrasonic-dist', u.distanceCm.toFixed(1) + ' cm');
    setText('card-echo-time', u.echoTimeUs + ' \u03bcs');
    const pir = $('card-pir-detect');
    if (pir) {
      pir.textContent = 'Detection: ' + p.detection;
      pir.className = 'text-label-telemetry font-label-telemetry font-bold ' + (p.detection === 'ACTIVE' ? 'text-tertiary' : 'text-secondary');
    }
    setText('svg-prox-label', 'PROXIMITY [' + u.distanceCm.toFixed(1) + 'cm]');

    // analytics
    const a = d.analytics;
    setText('an-accuracy', a.accuracyPercent.toFixed(1) + '%');
    setText('an-drift', '< ' + Math.max(0.1, Math.ceil(a.calibrationDriftPercent * 10) / 10).toFixed(1) + '%');
    setText('an-throughput', a.throughputUnitsPerMin);
    setText('an-throughput-label', a.throughputLabel);
    setText('an-duty-text', a.servoDutyPercent + '% (' + a.servoDutyLabel + ')');
    const bar = $('an-duty-bar');
    if (bar) bar.style.width = a.servoDutyPercent + '%';
    setText('telemetry-live-readout', d.telemetryLine);
  }

  const STAGE_ZONES = ['ultrasonic', 'optical', 'classification', 'servo', 'bins'];
  function renderPipeline(pl) {
    const box = $('pipeline-stages-container');
    if (!box) return;
    box.innerHTML = pl.stages.map((s) => {
      const active = s.number === pl.currentStage;
      const base = 'p-2.5 bg-surface-container-low rounded flex flex-col items-center cursor-pointer transition hover:bg-surface-container-high ';
      const cls = base + (active ? 'border-2 border-primary-container shadow-sm' : 'border border-surface-dim');
      const lab = active ? 'text-primary-container' : 'text-secondary';
      const sub = active
        ? '<span class="text-label-code font-label-code text-primary-container mt-1 font-bold flex items-center gap-1"><span class="w-1.5 h-1.5 rounded-full bg-primary-container animate-pulse"></span> ' + s.subtitle + '</span>'
        : '<span class="text-label-code font-label-code ' + (s.number === 5 ? 'text-tertiary' : 'text-secondary') + ' mt-1">' + s.subtitle + '</span>';
      return '<div class="' + cls + '" id="stage-card-' + s.number + '" onclick="highlightZone(\'' + STAGE_ZONES[s.number - 1] + '\')">' +
        '<span class="text-label-code font-label-code ' + lab + ' font-bold">STAGE 0' + s.number + '</span>' +
        '<span class="font-title-md text-title-md text-on-surface font-semibold">' + s.title + '</span>' + sub + '</div>';
    }).join('');
  }

  function renderMachineStatus(m) {
    const grid = document.querySelector('#content-tab-machine-status .grid');
    const mt = m.metrics;
    const vals = [
      [mt.chassisTempC.toFixed(1) + ' \u00b0C', 'Thermal margin: +' + mt.thermalMarginC.toFixed(1) + '\u00b0C'],
      [mt.busVoltageV.toFixed(2) + ' V', null],
      [mt.logicSupplyV.toFixed(2) + ' V', null],
      [mt.cycleRuntime, null],
    ];
    vals.forEach((v, i) => {
      const card = grid.children[i];
      card.children[1].textContent = v[0];
      if (v[1]) card.children[2].textContent = v[1];
    });
    const rows = document.querySelectorAll('#content-tab-machine-status tbody tr');
    m.actuators.forEach((a, i) => {
      const tr = rows[i];
      if (!tr) return;
      tr.children[2].textContent = a.currentValue;
      const fb = tr.children[4];
      fb.textContent = a.feedback;
      fb.className = 'p-3 font-bold ' + (a.feedback === 'Stopped' || a.feedback === 'Unpowered' ? 'text-red-600' : 'text-emerald-600');
      const btn = tr.querySelector('button');
      const idPrefix = a.id.split(' ')[0];
      btn.textContent = a.jogLabel;
      btn.onclick = function () { command('/api/machine/actuators/' + encodeURIComponent(idPrefix) + '/jog'); };
    });
  }

  function renderColour(c) {
    const cards = document.querySelector('#content-tab-colour-detection .grid').children;
    c.channels.forEach((ch, i) => {
      const card = cards[i];
      card.children[1].innerHTML = ch.raw + ' <span class="text-body-sm font-normal text-secondary">RAW</span>';
      card.children[2].textContent = ch.rangeText;
      card.children[3].firstElementChild.style.width = ch.percent + '%';
    });
  }

  function renderSensors(s) {
    const panels = document.querySelectorAll('#content-tab-sensors .grid > div');
    // ultrasonic waveform
    const us = panels[0];
    us.querySelector('.text-primary-container').textContent = s.ultrasonic.targetCm.toFixed(1) + ' cm TARGET';
    Array.from(us.querySelector('.h-36').children).forEach((bar, i) => { bar.style.height = (s.ultrasonic.waveform[i] || 0) + '%'; });
    const foot = us.lastElementChild.children;
    foot[0].textContent = s.ultrasonic.windowLabel;
    foot[1].textContent = 'Current Distance: ' + s.ultrasonic.targetCm.toFixed(1) + ' cm';
    // optical spectrum
    const op = panels[1];
    op.querySelector('.text-emerald-600').textContent = s.optical.peakNm + ' nm PEAK';
    Array.from(op.querySelector('.h-36').children).forEach((col, i) => {
      const b = s.optical.bars[i];
      col.firstElementChild.style.height = Math.max(10, b.percent * 1.1) + 'px';
      col.firstElementChild.textContent = b.percent + '%';
    });
    const f2 = op.lastElementChild.children;
    f2[0].textContent = s.optical.scalingText;
    f2[1].textContent = 'Signal SNR: ' + s.optical.snrDb.toFixed(1) + ' dB';
  }

  const BADGE = ['blue', 'emerald', 'purple', 'cyan', 'rose', 'orange', 'amber', 'indigo', 'violet'];
  function renderComponents(list) {
    const grid = document.querySelector('#content-tab-components .grid');
    grid.innerHTML = list.map((c, i) => {
      const col = BADGE[i % BADGE.length];
      const specs = c.specs.map((sp) => '<div>' + sp.label + ': <span class="font-bold text-on-surface">' + sp.value + '</span></div>').join('');
      return '<div class="p-4 bg-surface-container-low border border-surface-dim rounded hover:border-primary-container transition">' +
        '<div class="flex justify-between items-center mb-2"><span class="font-headline-md text-title-md text-on-surface font-bold">' + c.name + '</span>' +
        '<span class="px-2 py-0.5 bg-' + col + '-100 text-' + col + '-800 text-label-code font-label-code rounded">' + c.badge + '</span></div>' +
        '<p class="text-body-sm font-body-sm text-secondary mb-3">' + c.description + '</p>' +
        '<div class="text-label-code font-label-code space-y-1 border-t border-surface-dim pt-2 text-secondary">' + specs + '</div></div>';
    }).join('');
    componentsLoaded = true;
  }
})();
