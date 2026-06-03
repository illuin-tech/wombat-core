document.getElementById('filterForm').addEventListener('submit', function(e) {
  e.preventDefault();
  var from = document.getElementById('inputFrom').value;
  var to   = document.getElementById('inputTo').value;
  var params = new URLSearchParams();
  if (from) params.set('from', localInputToUtc(from));
  if (to)   params.set('to',   localInputToUtc(to));
  window.location.href = '?' + params.toString();
});
