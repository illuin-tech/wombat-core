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
