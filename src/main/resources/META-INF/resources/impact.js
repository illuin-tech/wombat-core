/* ── Local time helpers ── */
function formatLocalRange(startIso, endIso) {
  var opts = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
  var fmt = function(iso) { return iso ? new Date(iso).toLocaleString(navigator.language, opts) : '—'; };
  return fmt(startIso) + ' → ' + fmt(endIso);
}

/* ── Init date range picker and header from UTC data attrs ── */
(function() {
  function pad(n) { return (n < 10 ? '0' : '') + n; }
  function localInputFmt(d) {
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
      + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
  }
  function isoToDate(iso) { return iso ? new Date(iso) : null; }

  var picker = document.getElementById('dateRangePicker');
  var inFrom = document.getElementById('inputFrom');
  var inTo   = document.getElementById('inputTo');

  if (picker && window.easepick) {
    var fromDate = isoToDate(picker.dataset.fromUtc);
    var toDate   = isoToDate(picker.dataset.toUtc);
    var maxSpanMs = parseInt(picker.dataset.maxSpanMs, 10) || 0;
    var maxDays = maxSpanMs ? Math.max(1, Math.ceil(maxSpanMs / 86400000)) : 0;

    function nowMinus(hours) { return new Date(Date.now() - hours * 3600000); }
    var presets = {
      'Last hour':    [nowMinus(1),   new Date()],
      'Last 24h':     [nowMinus(24),  new Date()],
      'Last 7 days':  [nowMinus(168), new Date()],
      'Last 30 days': [nowMinus(720), new Date()]
    };

    var ep = new easepick.create({
      element: picker,
      css: [
        'https://cdn.jsdelivr.net/npm/@easepick/bundle@1.2.1/dist/index.css',
        '/easepick-theme.css'
      ],
      zIndex: 10,
      grid: window.matchMedia('(max-width: 720px)').matches ? 1 : 2,
      calendars: window.matchMedia('(max-width: 720px)').matches ? 1 : 2,
      format: 'YYYY-MM-DD HH:mm',
      plugins: ['RangePlugin', 'TimePlugin', 'PresetPlugin', 'LockPlugin'],
      RangePlugin: {
        tooltip: true,
        startDate: fromDate || undefined,
        endDate: toDate || undefined,
        repick: true,
        delimiter: ' → '
      },
      TimePlugin: { format: 'HH:mm', stepMinutes: 5 },
      PresetPlugin: { customPreset: presets, position: 'bottom' },
      LockPlugin: {
        filter: function(date, picked) {
          if (!maxSpanMs || !picked || picked.length !== 1) return false;
          var anchorMs = picked[0].toJSDate().getTime();
          var dMs = date.toJSDate().getTime();
          return Math.abs(dMs - anchorMs) > maxSpanMs;
        }
      },
      setup: function(p) {
        var clamping = false;
        p.on('select', function(e) {
          var start = e.detail.start;
          var end = e.detail.end;
          if (clamping) {
            clamping = false;
            if (start) inFrom.value = localInputFmt(start);
            if (end)   inTo.value   = localInputFmt(end);
            return;
          }
          if (start && end && maxSpanMs && (end.getTime() - start.getTime()) > maxSpanMs) {
            var clampedStart = new Date(end.getTime() - maxSpanMs);
            inFrom.value = localInputFmt(clampedStart);
            inTo.value   = localInputFmt(end);
            clamping = true;
            setTimeout(function() { ep.setDateRange(clampedStart, end); }, 0);
            return;
          }
          if (start) inFrom.value = localInputFmt(start);
          if (end)   inTo.value   = localInputFmt(end);
        });
      }
    });

    if (fromDate) inFrom.value = localInputFmt(fromDate);
    if (toDate)   inTo.value   = localInputFmt(toDate);
  }

  var meta = document.getElementById('headerMeta');
  if (meta) meta.textContent = formatLocalRange(meta.dataset.start, meta.dataset.end);
})();

/* ── Chart defaults ── */
var cssVar = function(name, fallback) {
  var v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return v || fallback;
};
var GREEN  = cssVar('--accent-green',  '#16a34a');
var ORANGE = cssVar('--accent-orange', '#ea580c');
var BLUE   = cssVar('--accent-blue',   '#2563eb');
var YELLOW = cssVar('--accent-yellow', '#ca8a04');
var PURPLE = '#7c3aed', TEAL  = '#0891b2';
var BORDER = cssVar('--border',      '#e3ede8');
var TEXT   = cssVar('--text-muted',  '#4d7060');
var palette = [GREEN, BLUE, YELLOW, TEAL, PURPLE, ORANGE, '#db2777', '#0284c7'];
Chart.defaults.color = TEXT; Chart.defaults.borderColor = BORDER;
Chart.defaults.font.family = "'DM Sans', sans-serif"; Chart.defaults.font.size = 12; Chart.defaults.font.weight = '500';

/* ── Overview charts ── */
var hbarH = Math.max(160, serviceLabels.length * 36 + 80);
document.querySelectorAll('.chart-wrap.hbar').forEach(function(el) { el.style.height = hbarH + 'px'; });

new Chart(document.getElementById('chartSharesDonut'), {
  type: 'doughnut',
  data: {
    labels: sharesLabels,
    datasets: [{ data: sharesValues, backgroundColor: palette.map(function(c) { return c + 'cc'; }), borderColor: palette, borderWidth: 1, hoverOffset: 8 }]
  },
  options: {
    responsive: true, maintainAspectRatio: false, cutout: '62%',
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { label: function(ctx) { return ' ' + (ctx.parsed * 100).toFixed(1) + '%'; } } }
    }
  }
});

(function() {
  var legendEl = document.getElementById('sharesLegend');
  sharesLabels.forEach(function(label, i) {
    var item = document.createElement('div');
    item.className = 'shares-legend-item';
    item.innerHTML =
      '<span class="shares-legend-dot" style="background:' + palette[i % palette.length] + '"></span>' +
      '<span class="shares-legend-label">' + label + '</span>' +
      '<span class="shares-legend-pct">' + (sharesValues[i] * 100).toFixed(1) + '%</span>';
    legendEl.appendChild(item);
  });
})();

function makeHBar(id, datasets, unit, color) {
  return new Chart(document.getElementById(id), {
    type: 'bar',
    data: { labels: serviceLabels, datasets: datasets },
    options: {
      indexAxis: 'y', responsive: true, maintainAspectRatio: false,
      plugins: { legend: { position: 'top', labels: { boxWidth: 10, padding: 14 } }, tooltip: { mode: 'index' } },
      scales: {
        x: { stacked: true, grid: { color: BORDER }, title: { display: true, text: unit, color: color } },
        y: { stacked: true, grid: { color: BORDER } }
      }
    }
  });
}

[{ id: 'chartGwp', emb: gwpEmbedded, use: gwpUse, color: GREEN,  unit: 'kgCO2eq' },
 { id: 'chartPe',  emb: peEmbedded,  use: peUse,  color: BLUE,   unit: 'MJ'      },
 { id: 'chartAdp', emb: adpEmbedded, use: adpUse, color: YELLOW, unit: 'kgSbeq'  }
].forEach(function(m) {
  makeHBar(m.id, [
    { label: 'Embedded', data: m.emb, backgroundColor: m.color + 'cc', borderColor: m.color, borderWidth: 1 },
    { label: 'Use',      data: m.use, backgroundColor: m.color + '33', borderColor: m.color, borderWidth: 1 }
  ], m.unit, m.color);
});

/* ── Asset picker (single-select, auto-navigates on change) ── */
(function() {
  var picker   = document.getElementById('assetPicker');
  var trigger  = document.getElementById('assetTrigger');
  var dropdown = document.getElementById('assetDropdown');

  function updateLabel() {
    var sel = document.querySelector('#assetList input[name="assets"]:checked');
    document.getElementById('assetCount').textContent = sel ? (sel.dataset.label || sel.value) : '';
  }
  function toggleDropdown(open) {
    trigger.classList.toggle('open', open);
    dropdown.classList.toggle('open', open);
  }

  trigger.addEventListener('click', function(e) { e.stopPropagation(); toggleDropdown(!dropdown.classList.contains('open')); });
  document.addEventListener('click', function(e) { if (!picker.contains(e.target)) toggleDropdown(false); });
  document.addEventListener('keydown', function(e) { if (e.key === 'Escape') toggleDropdown(false); });

  document.querySelectorAll('#assetList input[name="assets"]').forEach(function(b) {
    b.addEventListener('change', function() {
      var p = new URLSearchParams();
      var from = document.getElementById('inputFrom').value;
      var to   = document.getElementById('inputTo').value;
      if (from) p.set('from', localInputToUtc(from));
      if (to)   p.set('to',   localInputToUtc(to));
      p.set('assets', b.value);
      window.location.href = '?' + p.toString();
    });
  });
  updateLabel();
})();

/* ── Container picker ── */
(function() {
  var picker   = document.getElementById('containerPicker');
  var trigger  = document.getElementById('containerTrigger');
  var dropdown = document.getElementById('containerDropdown');
  var search   = document.getElementById('containerSearch');

  function updateCount() {
    var boxes = document.querySelectorAll('#containerList input');
    document.getElementById('containerCount').textContent =
      Array.from(boxes).filter(function(b) { return b.checked; }).length + ' / ' + boxes.length + ' containers';
  }
  function toggleDropdown(open) {
    trigger.classList.toggle('open', open);
    dropdown.classList.toggle('open', open);
    if (open) { search.value = ''; search.dispatchEvent(new Event('input')); search.focus(); }
  }

  trigger.addEventListener('click', function(e) { e.stopPropagation(); toggleDropdown(!dropdown.classList.contains('open')); });
  document.addEventListener('click', function(e) { if (!picker.contains(e.target)) toggleDropdown(false); });
  document.addEventListener('keydown', function(e) { if (e.key === 'Escape') toggleDropdown(false); });

  search.addEventListener('input', function() {
    var q = search.value.toLowerCase();
    document.querySelectorAll('#containerList .container-item').forEach(function(item) {
      item.classList.toggle('hidden', !!q && !item.querySelector('span').textContent.toLowerCase().includes(q));
    });
  });

  document.getElementById('btnSelectAll').addEventListener('click', function() {
    document.querySelectorAll('#containerList .container-item:not(.hidden) input').forEach(function(b) { b.checked = true; });
    updateCount();
  });
  document.getElementById('btnSelectNone').addEventListener('click', function() {
    document.querySelectorAll('#containerList .container-item:not(.hidden) input').forEach(function(b) { b.checked = false; });
    updateCount();
  });
  document.querySelectorAll('#containerList input').forEach(function(b) { b.addEventListener('change', updateCount); });
  updateCount();
})();

/* ── Reset ── */
document.getElementById('btnReset').addEventListener('click', function() {
  window.location.href = window.location.pathname;
});

/* ── Form submit ── */
document.getElementById('filterForm').addEventListener('submit', function(e) {
  e.preventDefault();
  var p = new URLSearchParams();
  var from = document.getElementById('inputFrom').value;
  var to   = document.getElementById('inputTo').value;
  var picker = document.getElementById('dateRangePicker');
  var maxSpanMs = picker ? parseInt(picker.dataset.maxSpanMs, 10) : 0;
  if (from && to && maxSpanMs) {
    var fromMs = new Date(from).getTime();
    var toMs   = new Date(to).getTime();
    if (toMs - fromMs > maxSpanMs) {
      from = (function(d) {
        function pad(n) { return (n < 10 ? '0' : '') + n; }
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
          + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
      })(new Date(toMs - maxSpanMs));
    }
  }
  if (from) p.set('from', localInputToUtc(from));
  if (to)   p.set('to',   localInputToUtc(to));
  document.querySelectorAll('#assetList input[name="assets"]:checked').forEach(function(b) { p.append('assets', b.value); });
  document.querySelectorAll('#containerList input:checked').forEach(function(b) { p.append('containers', b.value); });
  window.location.href = '?' + p.toString();
});

/* ── Per-service collapsible detail charts ── */
var initializedRows = {};

function makeDetailChart(id, embedded, use, color, unit) {
  new Chart(document.getElementById(id), {
    type: 'bar',
    data: {
      labels: ['Embedded', 'Use'],
      datasets: [{ data: [embedded, use], backgroundColor: [color + 'cc', color + '55'], borderColor: [color, color], borderWidth: 1 }]
    },
    options: {
      indexAxis: 'y', responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { mode: 'index' } },
      scales: {
        x: { grid: { color: BORDER }, title: { display: true, text: unit, color: color } },
        y: { grid: { display: false }, ticks: { color: TEXT, font: { size: 12, weight: '600' } } }
      }
    }
  });
}

document.querySelectorAll('.service-summary').forEach(function(row) {
  row.addEventListener('click', function() {
    var idx    = parseInt(row.dataset.idx, 10);
    var detail = document.getElementById('detail-' + idx);
    var open   = !detail.classList.toggle('collapsed');
    row.querySelector('.row-chevron').classList.toggle('open', open);
    if (open && !initializedRows[idx]) {
      var i = idx - 1;
      [{ id: 'detailGwp-', emb: gwpEmbedded[i], use: gwpUse[i],  color: GREEN,  unit: 'kgCO2eq' },
       { id: 'detailPe-',  emb: peEmbedded[i],  use: peUse[i],   color: BLUE,   unit: 'MJ'      },
       { id: 'detailAdp-', emb: adpEmbedded[i], use: adpUse[i],  color: YELLOW, unit: 'kgSbeq'  }
      ].forEach(function(m) { makeDetailChart(m.id + idx, m.emb, m.use, m.color, m.unit); });
      initializedRows[idx] = true;
    }
  });
});
