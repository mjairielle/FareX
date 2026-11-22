package farex.server;

/**
 * The admin web page. One file, no build step, plain JS, no outside libraries (works offline at a defense).
 * Text is set with textContent only, so user names cannot inject script. Charts are SVG drawn here.
 * No backslashes in the script: this is a Java text block, where they would be read as Java escapes.
 */
final class AdminPage {
    static final String HTML = """
<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>FareX Admin</title>
<style>
:root{--bg:#0f0f0f;--panel:#1a1a1a;--panel2:#232323;--line:#2e2e2e;--text:#eee;--muted:#9a9a9a;--lime:#c6ff3d;--cyan:#38bdf8;--red:#ff6b6b;--amber:#fbbf24}
*{box-sizing:border-box} body{margin:0;font-family:system-ui,Segoe UI,sans-serif;background:var(--bg);color:var(--text);font-size:14px}
button{font:inherit;cursor:pointer} h2{margin:0 0 4px;font-size:22px} h3{margin:0 0 10px;font-size:15px}
.btn{background:var(--lime);color:#111;border:0;padding:7px 12px;border-radius:6px;font-weight:700;margin:2px}
.btn.dark{background:var(--panel2);color:var(--text);border:1px solid var(--line)}
.btn.warn{background:#3a1d1d;color:var(--red);border:1px solid #5a2a2a}
input,select,textarea{background:var(--panel2);color:var(--text);border:1px solid var(--line);border-radius:6px;padding:8px;font:inherit;width:100%}
#login{max-width:360px;margin:12vh auto;background:var(--panel);padding:24px;border-radius:12px}
#login h1{color:var(--lime);margin:0 0 14px} #login input{margin:6px 0} #login .btn{width:100%;margin-top:10px;padding:10px}
#app{display:none;min-height:100vh}
#side{position:fixed;inset:0 auto 0 0;width:200px;background:#141414;border-right:1px solid var(--line);padding:18px 10px;display:flex;flex-direction:column;gap:4px}
#side .logo{color:var(--lime);font-weight:800;font-size:20px;padding:0 10px 14px;white-space:nowrap}
#side button{background:none;border:0;color:var(--text);text-align:left;padding:10px 12px;border-radius:8px}
#side button.on{background:var(--panel2);color:var(--lime);font-weight:700}
#side .grow{flex:1} #side .out{color:var(--muted)}
main{margin-left:200px;padding:22px 26px;max-width:1200px}
.sub{color:var(--muted);margin-bottom:14px} #msg{min-height:20px;margin:4px 0 8px} .err{color:var(--red)} .ok{color:var(--lime)}
.card{background:var(--panel);border:1px solid var(--line);border-radius:10px;padding:14px;margin-bottom:14px}
.tiles{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:12px;margin-bottom:14px}
.tile{background:var(--panel);border:1px solid var(--line);border-radius:10px;padding:12px 14px}
.tile .k{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.04em}
.tile .v{font-size:24px;font-weight:800;margin:4px 0 2px} .tile .s{color:var(--muted);font-size:12px}
.tile.alert{border-color:var(--amber)} .tile.alert .v{color:var(--amber)}
.grid2{display:grid;grid-template-columns:1fr 1fr;gap:14px} .grid2>.card{margin-bottom:0}
.bar{display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin-bottom:14px}
.seg button{background:var(--panel2);color:var(--text);border:1px solid var(--line);padding:6px 12px}
.seg button:first-child{border-radius:6px 0 0 6px} .seg button:last-child{border-radius:0 6px 6px 0}
.seg button.on{background:var(--lime);color:#111;font-weight:700}
.legend{display:flex;gap:14px;color:var(--muted);font-size:12px;margin-bottom:6px} .dot{display:inline-block;width:10px;height:10px;border-radius:2px;margin-right:5px;vertical-align:-1px}
.tablewrap{overflow-x:auto} table{border-collapse:collapse;width:100%} th{color:var(--muted);font-weight:600;font-size:12px;text-transform:uppercase}
td,th{border-bottom:1px solid var(--line);padding:8px 6px;text-align:left;vertical-align:top} td.num,th.num{text-align:right}
.badge{display:inline-block;padding:2px 8px;border-radius:99px;font-size:12px;font-weight:700}
.b-active{background:#1f2e10;color:var(--lime)} .b-pending{background:#33290d;color:var(--amber)} .b-suspended{background:#3a1d1d;color:var(--red)}
.b-load{background:#10273a;color:var(--cyan)} .b-fare{background:#1f2e10;color:var(--lime)}
.filters{display:grid;grid-template-columns:2fr 1fr 1fr;gap:8px;margin-bottom:10px}
.route{border-bottom:1px solid var(--line);padding:9px 6px;cursor:pointer} .route:hover{background:var(--panel2)}
.route .id{color:var(--lime);font-weight:700;margin-right:8px} .route .n{color:var(--muted);font-size:12px} .route .stops{color:var(--muted);margin-top:6px;font-size:13px}
.form{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:8px;align-items:end} .form label{color:var(--muted);font-size:12px}
.empty{color:var(--muted);padding:10px 0}
@media (max-width:760px){
  #side{position:static;width:auto;flex-direction:row;overflow-x:auto;padding:8px;border-right:0;border-bottom:1px solid var(--line)}
  #side .logo{padding:8px 10px} #side .grow{display:none} main{margin-left:0;padding:14px} .grid2{grid-template-columns:1fr} .filters{grid-template-columns:1fr}
}
</style></head><body>
<div id="login">
  <h1>FareX Admin</h1>
  <input id="em" placeholder="Admin email" autocomplete="username"><input id="pw" type="password" placeholder="Password" autocomplete="current-password">
  <button class="btn" id="go">Log in</button><div id="lerr" class="err"></div>
</div>
<div id="app">
  <nav id="side"><div class="logo">FareX Admin</div></nav>
  <main><h2 id="title"></h2><div id="subtitle" class="sub"></div><div id="msg"></div><div id="view"></div></main>
</div>
<script>
const NL = String.fromCharCode(10), PESO = String.fromCharCode(8369);
let token = sessionStorage.getItem('fx'), current = '', period = 7, timer = null;
const $ = id => document.getElementById(id);
const el = (tag, text, attrs) => { const e = document.createElement(tag); if (text !== undefined && text !== null) e.textContent = text; Object.assign(e, attrs || {}); return e; };
const kids = (parent, ...nodes) => { nodes.forEach(n => n && parent.appendChild(n)); return parent; };

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
const peso = c => PESO + (Number(c) / 100).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const when = iso => new Date(iso).toLocaleString('en-PH', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
const btn = (label, f, cls) => { const b = el('button', label, { className: 'btn' + (cls ? ' ' + cls : '') }); b.onclick = () => run(f); return b; };
const badge = (text, cls) => el('span', text, { className: 'badge ' + cls });
const card = (title, ...nodes) => kids(el('div', null, { className: 'card' }), title ? el('h3', title) : null, ...nodes);

function table(head, data, cell) {
  const wrap = el('div', null, { className: 'tablewrap' }), t = el('table'), h = el('tr');
  head.forEach(x => h.appendChild(el('th', x[0], { className: x[1] || '' })));
  t.appendChild(h);
  data.forEach(r => { const tr = el('tr'); head.forEach((x, i) => { const td = el('td', null, { className: x[1] || '' }); const v = cell(r, i); if (v instanceof Node) td.appendChild(v); else if (Array.isArray(v)) v.forEach(n => td.appendChild(n)); else td.textContent = v; tr.appendChild(td); }); t.appendChild(tr); });
  if (!data.length) { const tr = el('tr'); tr.appendChild(el('td', 'Nothing here yet.', { className: 'empty', colSpan: head.length })); t.appendChild(tr); }
  return kids(wrap, t);
}

// Status action: Approve a waiting operator, Reinstate a blocked account, or Suspend / Retire (asks first)
function statusButton(u, active, view) {
  const st = u[6];
  if (st === 'pending') return btn('Approve', async () => { await api('POST', '/admin/api/user/status', { id: u[0], action: 'reinstate' }); say('Approved ' + u[1], true); show(view); });
  if (st === 'suspended') return btn('Reinstate', async () => { await api('POST', '/admin/api/user/status', { id: u[0], action: 'reinstate' }); say('Reinstated ' + u[1], true); show(view); }, 'dark');
  return btn(active, async () => {
    const what = active === 'Retire' ? 'Retire jeepney ' + u[5] + '? Its operator can no longer collect fares. They can still ride as a passenger.'
                                     : 'Suspend ' + u[1] + ' (' + u[2] + ')? This blocks every role of this email until reinstated.';
    if (!confirm(what)) return;
    await api('POST', '/admin/api/user/status', { id: u[0], action: active.toLowerCase() }); say(active === 'Retire' ? 'Retired' : 'Suspended', true); show(view);
  }, 'warn');
}
const statusBadge = st => badge(st, 'b-' + st);

// ----- charts: plain SVG -----
const SVGNS = 'http://www.w3.org/2000/svg';
function svg(tag, attrs, text) { const e = document.createElementNS(SVGNS, tag); for (const k in attrs) e.setAttribute(k, attrs[k]); if (text !== undefined) e.textContent = text; return e; }
// series: [{name, color, values}]; labels: one per day; fmt: axis label format; whole: counts, so steps are whole numbers
function barChart(labels, series, fmt, whole) {
  const W = 560, H = 220, L = 54, B = 26, T = 10, plotW = W - L - 8, plotH = H - B - T;
  const top = Math.max(1, ...series.flatMap(s => s.values));
  const max = whole ? Math.max(4, Math.ceil(top / 4) * 4) : top;
  const s = svg('svg', { viewBox: '0 0 ' + W + ' ' + H, width: '100%', role: 'img' });
  for (let g = 0; g <= 4; g++) {
    const y = T + plotH - plotH * g / 4;
    s.appendChild(svg('line', { x1: L, x2: W - 8, y1: y, y2: y, stroke: '#2e2e2e' }));
    s.appendChild(svg('text', { x: L - 6, y: y + 4, 'text-anchor': 'end', fill: '#9a9a9a', 'font-size': 11 }, fmt(max * g / 4)));
  }
  const slot = plotW / labels.length, barW = Math.max(2, Math.min(22, slot * 0.8 / series.length));
  const every = Math.ceil(labels.length / 10);
  labels.forEach((lab, i) => {
    series.forEach((ser, j) => {
      const v = ser.values[i], h = plotH * v / max, x = L + slot * i + (slot - barW * series.length) / 2 + barW * j;
      const r = svg('rect', { x, y: T + plotH - h, width: barW, height: Math.max(h, v > 0 ? 1 : 0), fill: ser.color, rx: 2 });
      r.appendChild(svg('title', {}, lab + ' ' + ser.name + ': ' + fmt(v)));
      s.appendChild(r);
    });
    if (i % every === 0) s.appendChild(svg('text', { x: L + slot * i + slot / 2, y: H - 8, 'text-anchor': 'middle', fill: '#9a9a9a', 'font-size': 11 }, lab));
  });
  return s;
}
const legend = series => kids(el('div', null, { className: 'legend' }), ...series.map(x => kids(el('span'), el('span', null, { className: 'dot', style: 'background:' + x.color }), document.createTextNode(x.name))));

// ----- views -----
const views = {
  Dashboard: { sub: 'Money, trips and accounts at a glance. Updates every 30 seconds.', async render(v) {
    const d = rows(await api('GET', '/admin/api/stats', { days: period }));
    const K = Object.fromEntries(d.filter(r => r[0] === 'K').map(r => [r[1], Number(r[2])]));
    const days = d.filter(r => r[0] === 'D'), recent = d.filter(r => r[0] === 'A'), ops = d.filter(r => r[0] === 'O'), waiting = d.filter(r => r[0] === 'P');
    const span = period === 1 ? 'today' : 'last ' + period + ' days';

    const seg = el('div', null, { className: 'seg' });
    [[1, 'Today'], [7, '7 days'], [30, '30 days']].forEach(([n, label]) => { const b = el('button', label, { className: n === period ? 'on' : '' }); b.onclick = () => { period = n; show('Dashboard'); }; seg.appendChild(b); });
    v.appendChild(kids(el('div', null, { className: 'bar' }), seg, el('span', 'Updated ' + new Date().toLocaleTimeString('en-PH'), { className: 'sub', style: 'margin:0' }), btn('Refresh', () => show('Dashboard'), 'dark')));

    const tile = (k, val, sub, alert) => kids(el('div', null, { className: 'tile' + (alert ? ' alert' : '') }), el('div', k, { className: 'k' }), el('div', val, { className: 'v' }), el('div', sub, { className: 's' }));
    v.appendChild(kids(el('div', null, { className: 'tiles' }),
      tile('Loaded', peso(K.loadCents), K.loads + ' loads, ' + span),
      tile('Fares collected', peso(K.fareCents), K.trips + ' trips, ' + span),
      tile('Passengers', K.passengers, K.cards + ' cards linked'),
      tile('Operators', K.operators, K.pending + ' waiting for approval', K.pending > 0),
      tile('Reward points', K.pointsEarned, K.pointsRedeemed + ' redeemed, ' + span),
      tile('All time', peso(K.loadAll), peso(K.fareAll) + ' in fares'),
      tile('Blocked accounts', K.suspended, 'suspended or retired', K.suspended > 0)));

    const labels = days.map(r => period === 1 ? 'Today' : new Date(r[1] + 'T00:00').toLocaleDateString('en-PH', { month: 'short', day: 'numeric' }));
    const money = [{ name: 'Loaded', color: '#38bdf8', values: days.map(r => Number(r[2])) }, { name: 'Fares', color: '#c6ff3d', values: days.map(r => Number(r[3])) }];
    const trips = [{ name: 'Trips', color: '#c6ff3d', values: days.map(r => Number(r[4])) }];
    v.appendChild(kids(el('div', null, { className: 'grid2', style: 'margin-bottom:14px' }),
      card('Money per day', legend(money), barChart(labels, money, c => PESO + Math.round(c / 100))),
      card('Trips per day', legend(trips), barChart(labels, trips, n => String(n), true))));

    const attention = waiting.length
      ? table([['Operator'], ['Email'], ['Plate'], ['', 'num']], waiting, (r, i) => i === 0 ? r[2] : i === 1 ? r[3] : i === 2 ? r[4]
          : btn('Approve', async () => { await api('POST', '/admin/api/user/status', { id: r[1], action: 'reinstate' }); say('Approved ' + r[2], true); show('Dashboard'); }))
      : el('div', 'No operators waiting. All clear.', { className: 'empty' });
    const blocked = K.suspended > 0 ? btn('See ' + K.suspended + ' blocked accounts', () => { usersFilter.status = 'suspended'; show('Users'); }, 'dark') : null;
    v.appendChild(card('Needs attention', attention, blocked));

    v.appendChild(kids(el('div', null, { className: 'grid2' }),
      card('Recent activity', table([['When'], ['Type'], ['Who'], ['Amount', 'num']], recent, (r, i) =>
        i === 0 ? when(r[1]) : i === 1 ? badge(r[2], r[2] === 'Load' ? 'b-load' : 'b-fare')
        : i === 2 ? kids(el('div'), el('div', r[3]), el('div', 'Card ' + r[4] + (r[6] ? ' · ' + r[6] : ''), { className: 'sub', style: 'margin:0;font-size:12px' }))
        : peso(r[5]))),
      card('Top operators, ' + span, table([['Operator'], ['Plate'], ['Trips', 'num'], ['Fares', 'num']], ops, (r, i) => i === 0 ? r[1] : i === 1 ? r[2] : i === 2 ? r[4] : peso(r[3])))));
  }},

  Users: { sub: 'Every account. One email can be a passenger and an operator (two rows).', async render(v) {
    const data = rows(await api('GET', '/admin/api/users'));
    const q = el('input', null, { placeholder: 'Search name or email', value: usersFilter.q });
    const role = el('select'); [['', 'All roles'], ['PASSENGER', 'Passengers'], ['OPERATOR', 'Operators'], ['ADMIN', 'Admins']].forEach(([k, t]) => role.appendChild(el('option', t, { value: k })));
    const st = el('select'); [['', 'All statuses'], ['active', 'Active'], ['pending', 'Waiting for approval'], ['suspended', 'Suspended / retired']].forEach(([k, t]) => st.appendChild(el('option', t, { value: k })));
    role.value = usersFilter.role; st.value = usersFilter.status;
    const count = el('div', null, { className: 'sub' }), holder = el('div');
    const draw = () => {
      Object.assign(usersFilter, { q: q.value, role: role.value, status: st.value });
      const term = q.value.trim().toLowerCase();
      const shown = data.filter(u => (!term || (u[1] + ' ' + u[2]).toLowerCase().includes(term)) && (!role.value || u[3] === role.value) && (!st.value || u[6] === st.value));
      count.textContent = 'Showing ' + shown.length + ' of ' + data.length;
      holder.textContent = '';
      holder.appendChild(table([['Name'], ['Email'], ['Role'], ['Discount'], ['Status'], ['']], shown, (u, i) => {
        if (i === 0) return u[1]; if (i === 1) return u[2]; if (i === 2) return u[3] === 'OPERATOR' ? 'OPERATOR · ' + u[5] : u[3];
        if (i === 3) {
          if (u[3] !== 'PASSENGER') return '';
          const sel = el('select', null, { style: 'width:auto' }); ['REGULAR', 'STUDENT', 'SENIOR', 'PWD'].forEach(o => sel.appendChild(el('option', o)));
          sel.value = u[4];
          sel.onchange = () => run(async () => { await api('POST', '/admin/api/user/type', { id: u[0], type: sel.value }); say(u[1] + ' is now ' + sel.value, true); u[4] = sel.value; });
          return sel;
        }
        if (i === 4) return statusBadge(u[6]);
        return u[3] === 'ADMIN' ? '' : statusButton(u, 'Suspend', 'Users');
      }));
    };
    [q, role, st].forEach(x => x.oninput = draw);
    v.appendChild(card(null, kids(el('div', null, { className: 'filters' }), q, role, st), count, holder));
    draw();
  }},

  Vehicles: { sub: 'One operator account per jeepney. Retire stops only the jeep; the owner can still ride as a passenger.', async render(v) {
    const data = rows(await api('GET', '/admin/api/users')).filter(r => r[3] === 'OPERATOR');
    v.appendChild(card(null, table([['Plate'], ['Operator'], ['Email'], ['Status'], ['']], data, (u, i) =>
      i === 0 ? u[5] : i === 1 ? u[1] : i === 2 ? u[2] : i === 3 ? statusBadge(u[6]) : statusButton(u, 'Retire', 'Vehicles'))));
  }},

  Cards: { sub: 'Every linked card and its balance. Load cash: take the money at the counter, then add it here.', async render(v) {
    const data = rows(await api('GET', '/admin/api/cards'));
    const q = el('input', null, { placeholder: 'Search card ID, name or email', value: cardsFilter });
    const count = el('div', null, { className: 'sub' }), holder = el('div');
    const load = async (uid, who) => {
      const amount = prompt('Cash received for card ' + uid + (who ? ' (' + who + ')' : '') + ', in pesos (5 to 5,000):');
      if (amount === null) return;
      const c = Math.round(Number(amount.replace(',', '')) * 100);
      if (!(c >= 500 && c <= 500000)) throw new Error('Enter an amount from 5 to 5,000 pesos.');
      if (!confirm('Add ' + peso(c) + ' to card ' + uid + '? Only do this after receiving the cash.')) return;
      const r = JSON.parse(await api('POST', '/admin/api/load', { uid, cents: c }));
      say('Loaded ' + peso(c) + ' to card ' + uid + '. New balance ' + peso(r.cents) + '.', true);
      show('Cards');
    };
    // The card stops belonging to anyone; its money stays on the card and goes with it to whoever adds it next
    const unlink = async (uid, who, cents) => {
      if (!confirm('Unlink card ' + uid + ' from ' + who + '? Its balance (' + peso(cents) + ') stays on the card, for whoever adds it next.')) return;
      await api('POST', '/admin/api/card/unlink', { uid });
      say('Card ' + uid + ' unlinked. Anyone can add it in the app now.', true);
      show('Cards');
    };
    const draw = () => {
      cardsFilter = q.value;
      const term = q.value.trim().toLowerCase();
      const shown = data.filter(r => !term || (r[0] + ' ' + r[1] + ' ' + r[2]).toLowerCase().includes(term));
      count.textContent = 'Showing ' + shown.length + ' of ' + data.length + ' cards';
      holder.textContent = '';
      holder.appendChild(table([['Card ID'], ['Owner'], ['Balance', 'num'], ['Status'], ['']], shown, (r, i) =>
        i === 0 ? r[0] : i === 1 ? kids(el('div'), el('div', r[1]), el('div', r[2], { className: 'sub', style: 'margin:0;font-size:12px' }))
        : i === 2 ? peso(r[3]) : i === 3 ? (r[4] === 'true' ? badge('frozen', 'b-suspended') : badge('active', 'b-active'))
        : kids(el('div'), r[4] === 'true' ? null : btn('Load cash', () => load(r[0], r[1])), btn('Unlink', () => unlink(r[0], r[1], r[3]), 'warn'))));
    };
    q.oninput = draw;
    v.appendChild(card(null, q, count, holder));
    draw();
    const uid = el('input', null, { placeholder: 'Card ID (printed on the card or shown in the app)' });
    v.appendChild(card('Load a card by its ID', uid, btn('Load cash', () => load(uid.value.trim(), ''))));
  }},

  Routes: { sub: 'Jeepney routes from OpenStreetMap. Click a route to see its stops.', async render(v) {
    const data = rows(await api('GET', '/admin/api/routes'));
    const q = el('input', null, { placeholder: 'Search route, name or stop' });
    const count = el('div', null, { className: 'sub' }), list = el('div');
    const draw = () => {
      const term = q.value.trim().toLowerCase();
      const shown = data.filter(r => !term || r.join(' ').toLowerCase().includes(term));
      count.textContent = shown.length + ' of ' + data.length + ' routes' + (shown.length > 60 ? ', first 60 shown: search to narrow' : '');
      list.textContent = '';
      shown.slice(0, 60).forEach(r => {
        const stops = r[1].split(' > ');
        const row = kids(el('div', null, { className: 'route' }), el('span', r[0], { className: 'id' }), el('span', r[2] && r[2] !== r[0] ? r[2] : ''), el('div', stops.length + ' stops · ' + stops[0] + ' → ' + stops[stops.length - 1], { className: 'n' }));
        row.onclick = () => { const open = row.querySelector('.stops'); if (open) open.remove(); else row.appendChild(el('div', stops.join(' → '), { className: 'stops' })); };
        list.appendChild(row);
      });
    };
    q.oninput = draw;
    v.appendChild(card(null, q, count, list));
    draw();
    const id = el('input', null, { placeholder: 'Route id, e.g. R4' });
    const ta = el('textarea', null, { rows: 5, placeholder: 'One stop per line: name, lat, lng' + NL + 'Cubao, 14.62134, 121.05299' });
    v.appendChild(card('Add a route', id, ta, btn('Add route', async () => {
      const stops = ta.value.split(NL).filter(l => l.trim()).join(';');
      await api('POST', '/admin/api/route/add', { id: id.value, stops }); say('Route ' + id.value + ' added', true); show('Routes'); })));
  }},

  Fares: { sub: 'Fare = base fare up to the base distance, then a rate per km after it. Students, seniors and PWD get 20% off automatically.', async render(v) {
    const d = rows(await api('GET', '/admin/api/fares'));
    v.appendChild(card('Fare tables', table([['Effective'], ['Base fare', 'num'], ['Per km after base', 'num'], ['Base distance', 'num'], ['Published']], d, (r, i) =>
      i === 0 ? (r[0] === 'Current' ? 'In use now' : r[0]) : i === 1 ? peso(r[1]) : i === 2 ? peso(r[2]) : i === 3 ? (r[4] ? r[4] + ' km' : '') : (r[3] === '-' ? '' : r[3]))));
    const base = el('input', null, { type: 'number', step: '0.25', min: '0', placeholder: '14.00' });
    const rate = el('input', null, { type: 'number', step: '0.25', min: '0', placeholder: '2.00' });
    const baseKm = el('input', null, { type: 'number', step: '0.5', min: '0', placeholder: 'Blank keeps current' });
    const eff = el('input', null, { type: 'date', value: new Date().toISOString().slice(0, 10) });
    const lab = (t, input) => kids(el('div'), el('label', t), input);
    v.appendChild(card('Publish a new fare table', kids(el('div', null, { className: 'form' }),
      lab('Base fare (' + PESO + ')', base), lab('Per km after base (' + PESO + ')', rate), lab('Base distance (km)', baseKm), lab('Effective date', eff),
      btn('Publish', async () => {
        const b = Math.round(Number(base.value) * 100), r = Math.round(Number(rate.value) * 100);
        if (!(b > 0) || !(r >= 0) || base.value === '' || rate.value === '') throw new Error('Enter the base fare and the rate per km in pesos.');
        if (!confirm('Publish ' + peso(b) + ' base + ' + peso(r) + ' per km, effective ' + eff.value + '? Every ride from that date is charged this.')) return;
        await api('POST', '/admin/api/fares/publish', { base: b, rate: r, baseKm: baseKm.value, effective: eff.value }); say('Published', true); show('Fares');
      }))));
  }},

  Promotions: { sub: 'Point multipliers for a date range, e.g. 2x points on holidays.', async render(v) {
    v.appendChild(card('Promotions', table([['Name'], ['Multiplier', 'num'], ['Start'], ['End']], rows(await api('GET', '/admin/api/promos')), (r, i) => i === 1 ? r[1] + 'x' : r[i])));
    const name = el('input', null, { placeholder: 'Name, e.g. Holiday double points' }), mult = el('input', null, { type: 'number', min: '1', max: '10', step: '0.5', placeholder: '2' });
    const s = el('input', null, { type: 'date' }), e = el('input', null, { type: 'date' });
    const lab = (t, input) => kids(el('div'), el('label', t), input);
    v.appendChild(card('Create a promotion', kids(el('div', null, { className: 'form' }), lab('Name', name), lab('Multiplier (1 to 10)', mult), lab('Start', s), lab('End', e),
      btn('Activate', async () => { await api('POST', '/admin/api/promo', { name: name.value, multiplier: mult.value, start: s.value, end: e.value }); say('Activated', true); show('Promotions'); }))));
  }}
};
const usersFilter = { q: '', role: '', status: '' };
let cardsFilter = '';

async function show(name) {
  current = name;
  [...$('side').querySelectorAll('button[data-v]')].forEach(b => b.className = b.dataset.v === name ? 'on' : '');
  $('title').textContent = name; $('subtitle').textContent = views[name].sub;
  const fresh = el('div');
  try { await views[name].render(fresh); } catch (e) { say(e.message); }   // keeps an "Approved ..." message from the action that led here
  if (current === name) { $('view').replaceWith(fresh); fresh.id = 'view'; }   // swap at once: no flicker on auto refresh
}

function start() {
  $('login').style.display = 'none'; $('app').style.display = 'block';
  Object.keys(views).forEach(n => { const b = el('button', n); b.dataset.v = n; b.onclick = () => { say(''); show(n); }; $('side').appendChild(b); });
  $('side').appendChild(el('div', null, { className: 'grow' }));
  const out = el('button', 'Log out', { className: 'out' }); out.onclick = () => { sessionStorage.removeItem('fx'); location.reload(); };
  $('side').appendChild(out);
  show('Dashboard');
  // every 30 s, only while the Dashboard is open and the browser tab is visible
  timer = setInterval(() => { if (current === 'Dashboard' && !document.hidden) show('Dashboard'); }, 30000);
}

async function login() {
  try {
    const r = await fetch('/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ email: $('em').value, password: $('pw').value }).toString() });
    const j = await r.json();
    if (!r.ok) throw new Error(j.error);
    if (j.role !== 'ADMIN') throw new Error('Admin accounts only');
    token = j.token; sessionStorage.setItem('fx', token); start();
  } catch (e) { $('lerr').textContent = e.message; }
}
$('go').onclick = login;
$('pw').onkeydown = e => { if (e.key === 'Enter') login(); };
if (token) start();
</script></body></html>
""";

    private AdminPage() {}
}
