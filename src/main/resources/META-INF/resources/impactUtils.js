/* ── Theme: apply before paint to prevent FOUC ── */
(function() {
  var stored = null;
  try { stored = localStorage.getItem('wombat.theme'); } catch (e) { }
  document.documentElement.setAttribute('data-theme', stored || 'light');
})();

function toggleTheme() {
  var next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  try { localStorage.setItem('wombat.theme', next); } catch (e) { }
}

document.addEventListener('DOMContentLoaded', function() {
  var btn = document.getElementById('themeToggle');
  if (btn) btn.addEventListener('click', toggleTheme);
});

function utcToLocalInput(iso) {
  if (!iso) return '';
  var d = new Date(iso);
  var off = d.getTimezoneOffset() * 60000;
  return new Date(d - off).toISOString().slice(0, 16);
}

function localInputToUtc(local) {
  if (!local) return '';
  return new Date(local).toISOString().slice(0, 16);
}

/* ── Local time range label ── */
function formatLocalRange(startIso, endIso) {
  var opts = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
  var fmt = function(iso) { return iso ? new Date(iso).toLocaleString(navigator.language, opts) : '—'; };
  return fmt(startIso) + ' → ' + fmt(endIso);
}

/* ── Shared date range picker (easepick) + header meta ──
   Used by every screen that exposes #dateRangePicker (the global report and the error page),
   so the date range control stays identical across them. ── */
function initDateRangePicker() {
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
}

document.addEventListener('DOMContentLoaded', initDateRangePicker);
