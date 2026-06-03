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
