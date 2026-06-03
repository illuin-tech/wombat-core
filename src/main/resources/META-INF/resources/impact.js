/* ── Local time helpers ── */
function formatLocalRange(startIso, endIso) {
  var opts = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
  var fmt = function(iso) { return iso ? new Date(iso).toLocaleString(navigator.language, opts) : '—'; };
  return fmt(startIso) + ' → ' + fmt(endIso);
}

/* ── Init datetime inputs and header from UTC data attrs ── */
(function() {
  var inFrom = document.getElementById('inputFrom');
  var inTo   = document.getElementById('inputTo');
  if (inFrom.dataset.utc) inFrom.value = utcToLocalInput(inFrom.dataset.utc);
  if (inTo.dataset.utc)   inTo.value   = utcToLocalInput(inTo.dataset.utc);
  var meta = document.getElementById('headerMeta');
  meta.textContent = formatLocalRange(meta.dataset.start, meta.dataset.end);
})();

/* ── Chart defaults ── */
var GREEN  = '#16a34a', ORANGE = '#ea580c', BLUE = '#2563eb';
var YELLOW = '#ca8a04', PURPLE = '#7c3aed', TEAL  = '#0891b2';
var BORDER = '#e3ede8', TEXT   = '#4d7060';
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

/* ── Profile picker (auto-navigates on change) ── */
(function() {
  document.getElementById('profileSelect').addEventListener('change', function() {
    var p = new URLSearchParams();
    var from = document.getElementById('inputFrom').value;
    var to   = document.getElementById('inputTo').value;
    if (from) p.set('from', localInputToUtc(from));
    if (to)   p.set('to',   localInputToUtc(to));
    var selCluster = document.querySelector('#clusterList input[name="clusters"]:checked');
    if (selCluster) p.append('clusters', selCluster.value);
    p.set('profileId', this.value);
    window.location.href = '?' + p.toString();
  });
})();

/* ── Cluster picker (single-select, auto-navigates on change) ── */
(function() {
  var picker   = document.getElementById('clusterPicker');
  var trigger  = document.getElementById('clusterTrigger');
  var dropdown = document.getElementById('clusterDropdown');

  function updateLabel() {
    var sel = document.querySelector('#clusterList input[name="clusters"]:checked');
    document.getElementById('clusterCount').textContent = sel ? (sel.dataset.label || sel.value) : '';
  }
  function toggleDropdown(open) {
    trigger.classList.toggle('open', open);
    dropdown.classList.toggle('open', open);
  }

  trigger.addEventListener('click', function(e) { e.stopPropagation(); toggleDropdown(!dropdown.classList.contains('open')); });
  document.addEventListener('click', function(e) { if (!picker.contains(e.target)) toggleDropdown(false); });
  document.addEventListener('keydown', function(e) { if (e.key === 'Escape') toggleDropdown(false); });

  document.querySelectorAll('#clusterList input[name="clusters"]').forEach(function(b) {
    b.addEventListener('change', function() {
      var p = new URLSearchParams();
      var from = document.getElementById('inputFrom').value;
      var to   = document.getElementById('inputTo').value;
      if (from) p.set('from', localInputToUtc(from));
      if (to)   p.set('to',   localInputToUtc(to));
      var profileId = document.getElementById('profileSelect').value;
      if (profileId) p.set('profileId', profileId);
      p.append('clusters', b.value);
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
  if (from) p.set('from', localInputToUtc(from));
  if (to)   p.set('to',   localInputToUtc(to));
  var profileId = document.getElementById('profileSelect').value;
  if (profileId) p.set('profileId', profileId);
  var selCluster = document.querySelector('#clusterList input[name="clusters"]:checked');
  if (selCluster) p.append('clusters', selCluster.value);
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
