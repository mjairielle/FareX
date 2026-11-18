package farex.server;

/** The admin web page. One file, no build step, plain JS. Text is set with textContent only, so user names cannot inject script. */
final class AdminPage {
    static final String HTML = """
<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>FareX Admin</title>
<style>
body{font-family:system-ui,sans-serif;background:#111;color:#eee;margin:0;padding:16px;max-width:980px;margin:auto}
h1{color:#c6ff3d;margin:0 0 12px} h2{margin-top:0}
nav button,.btn{background:#c6ff3d;color:#111;border:0;padding:8px 14px;margin:2px;cursor:pointer;font-weight:600}
nav button.off{background:#2a2a2a;color:#eee}
input,select,textarea{background:#1c1c1c;color:#eee;border:1px solid #444;padding:8px;margin:4px 0;width:100%;box-sizing:border-box}
table{border-collapse:collapse;width:100%} td,th{border-bottom:1px solid #333;padding:6px;text-align:left;font-size:14px}
.card{background:#1c1c1c;padding:14px;margin:10px 0}
.err{color:#ff6b6b;min-height:20px} .ok{color:#c6ff3d}
.row{display:flex;gap:8px;flex-wrap:wrap} .row>*{flex:1;min-width:140px}
</style></head><body>
<h1>FareX Admin</h1>
<div id="login" class="card">
  <input id="em" placeholder="Admin email"><input id="pw" type="password" placeholder="Password">
  <button class="btn" id="go">Login</button>
</div>
<div id="app" style="display:none">
  <nav id="tabs"></nav><div id="msg" class="err"></div><div id="view"></div>
</div>
<script>
const NL = String.fromCharCode(10);
let token = sessionStorage.getItem('fx');
const $ = id => document.getElementById(id);
const el = (tag, text, attrs) => { const e = document.createElement(tag); if (text !== undefined) e.textContent = text; Object.assign(e, attrs || {}); return e; };

async function api(method, path, params) {
  const q = new URLSearchParams(params || {}).toString();
  const r = await fetch(method === 'GET' && q ? path + '?' + q : path, {
    method, headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/x-www-form-urlencoded' },
    body: method === 'POST' ? q : undefined });
  const t = await r.text();
  if (r.status === 401) { sessionStorage.removeItem('fx'); location.reload(); }
  if (!r.ok) { let m = t; try { m = JSON.parse(t).error; } catch (e) {} throw new Error(m); }
  return t;
}
const rows = t => t.split(NL).filter(l => l.length).map(l => l.split('|'));
const say = (m, good) => { $('msg').className = good ? 'ok' : 'err'; $('msg').textContent = m; };
const run = async f => { try { say(''); await f(); } catch (e) { say(e.message); } };

function table(head, data, actions) {
  const t = el('table'), h = el('tr');
  head.concat(actions ? [''] : []).forEach(x => h.appendChild(el('th', x)));
  t.appendChild(h);
  data.forEach(r => {
    const tr = el('tr');
    r.forEach(c => tr.appendChild(el('td', c)));
    if (actions) { const td = el('td'); actions(r).forEach(b => td.appendChild(b)); tr.appendChild(td); }
    t.appendChild(tr);
  });
  return t;
}
const btn = (label, f) => { const b = el('button', label, { className: 'btn' }); b.onclick = () => run(f); return b; };
const field = (ph, val) => el('input', undefined, { placeholder: ph, value: val || '' });

const views = {
  async Dashboard(v) {
    const s = JSON.parse(await api('GET', '/admin/summary')).summary;
    v.appendChild(el('pre', s, { className: 'card' }));
  },
  async Users(v) {
    const data = rows(await api('GET', '/admin/api/users'));
    v.appendChild(table(['Name', 'Email', 'Role', 'Discount', 'Status'], data.map(r => [r[1], r[2], r[3], r[4], r[6]]), (r) => {
      const u = data.find(x => x[2] === r[1]); const out = [];
      if (u[3] === 'PASSENGER') {
        const sel = el('select'); ['REGULAR', 'STUDENT', 'SENIOR', 'PWD'].forEach(o => sel.appendChild(el('option', o)));
        sel.value = u[4]; sel.style.width = 'auto';
        out.push(sel, btn('Set discount', async () => { await api('POST', '/admin/api/user/type', { id: u[0], type: sel.value }); say('Saved', true); show('Users'); }));
      }
      if (u[3] !== 'ADMIN') out.push(btn(u[6] === 'suspended' ? 'Reinstate' : 'Suspend', async () => {
        await api('POST', '/admin/api/user/status', { id: u[0], action: u[6] === 'suspended' ? 'reinstate' : 'suspend' }); show('Users'); }));
      return out;
    }));
  },
  async Vehicles(v) {
    const data = rows(await api('GET', '/admin/api/users')).filter(r => r[3] === 'OPERATOR');
    v.appendChild(el('p', 'One operator account per jeepney. Retire a vehicle by suspending its operator.'));
    v.appendChild(table(['Plate', 'Operator', 'Email', 'Status'], data.map(r => [r[5], r[1], r[2], r[6]]), (r) => {
      const u = data.find(x => x[2] === r[2]);
      return [btn(u[6] === 'suspended' ? 'Reinstate' : 'Retire', async () => {
        await api('POST', '/admin/api/user/status', { id: u[0], action: u[6] === 'suspended' ? 'reinstate' : 'suspend' }); show('Vehicles'); })];
    }));
  },
  async Routes(v) {
    v.appendChild(table(['Route', 'Stops'], rows(await api('GET', '/admin/api/routes'))));
    const c = el('div', undefined, { className: 'card' });
    const id = field('Route id, e.g. R4');
    const ta = el('textarea', undefined, { rows: 5, placeholder: 'One stop per line: name, lat, lng' + NL + 'Cubao, 14.6191, 121.0509' });
    c.append(el('h2', 'Add route'), id, ta, btn('Add route', async () => {
      const stops = ta.value.split(NL).filter(l => l.trim()).join(';');
      await api('POST', '/admin/api/route/add', { id: id.value, stops }); say('Route added', true); show('Routes'); }));
    v.appendChild(c);
  },
  async Fares(v) {
    const d = rows(await api('GET', '/admin/api/fares'));
    v.appendChild(table(['Effective', 'Base (centavos)', 'Rate per km (centavos)', 'Published', 'Base km'], d));
    const c = el('div', undefined, { className: 'card' });
    const base = field('Base fare in centavos, 1400 = P14.00'), rate = field('Rate per km after the base distance, 200 = P2.00');
    const baseKm = field('Base distance in km, 4 jeepney / 5 bus (blank keeps current)');
    const eff = field('Effective date', new Date().toISOString().slice(0, 10)); eff.type = 'date';
    c.append(el('h2', 'Publish new fare table'), base, rate, baseKm, eff, btn('Publish', async () => {
      await api('POST', '/admin/api/fares/publish', { base: base.value, rate: rate.value, baseKm: baseKm.value, effective: eff.value }); say('Published', true); show('Fares'); }));
    v.appendChild(c);
  },
  async Promotions(v) {
    v.appendChild(table(['Name', 'Multiplier', 'Start', 'End'], rows(await api('GET', '/admin/api/promos'))));
    const c = el('div', undefined, { className: 'card' });
    const name = field('Name'), mult = field('Point multiplier, 1 to 10, e.g. 2');
    const s = field('Start'), e = field('End'); s.type = 'date'; e.type = 'date';
    c.append(el('h2', 'Create promotion'), name, mult, s, e, btn('Activate', async () => {
      await api('POST', '/admin/api/promo', { name: name.value, multiplier: mult.value, start: s.value, end: e.value }); say('Activated', true); show('Promotions'); }));
    v.appendChild(c);
  }
};

async function show(name) {
  const v = $('view'); v.textContent = '';
  [...$('tabs').children].forEach(b => b.className = b.textContent === name ? '' : 'off');
  await run(() => views[name](v));
}

function start() {
  $('login').style.display = 'none'; $('app').style.display = 'block';
  Object.keys(views).forEach(n => { const b = el('button', n); b.onclick = () => show(n); $('tabs').appendChild(b); });
  const out = el('button', 'Log out', { className: 'off' }); out.onclick = () => { sessionStorage.removeItem('fx'); location.reload(); };
  $('tabs').appendChild(out);
  show('Dashboard');
}

$('go').onclick = async () => {
  try {
    const r = await fetch('/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ email: $('em').value, password: $('pw').value }).toString() });
    const j = await r.json();
    if (!r.ok) throw new Error(j.error);
    if (j.role !== 'ADMIN') throw new Error('Admin accounts only');
    token = j.token; sessionStorage.setItem('fx', token); start();
  } catch (e) { alert(e.message); }
};
if (token) start();
</script></body></html>
""";

    private AdminPage() {}
}
