'use strict';

const KEYS = { denoms: 'cd.denoms', history: 'cd.history', draft: 'cd.draft', settings: 'cd.settings' };

const DEFAULT_DENOMS = [
  ...[1000, 500, 200, 100, 50, 20].map(v => ({ value: v, type: 'billete' })),
  ...[10, 5, 2, 1, 0.5].map(v => ({ value: v, type: 'moneda' })),
];

const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
const defaultDenoms = () => DEFAULT_DENOMS.map(d => ({ id: uid(), ...d, visible: true }));

function load(key, fallback) {
  try {
    const v = JSON.parse(localStorage.getItem(key));
    return v ?? fallback;
  } catch {
    return fallback;
  }
}
const save = (key, value) => localStorage.setItem(key, JSON.stringify(value));

const state = {
  denoms: load(KEYS.denoms, null) || defaultDenoms(),
  history: load(KEYS.history, []),
  draft: load(KEYS.draft, {}),
  settings: Object.assign({ symbol: '$' }, load(KEYS.settings, {})),
};

const $ = sel => document.querySelector(sel);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

// Montos en centavos para evitar errores de punto flotante
const toCents = v => Math.round(v * 100);
const fmt2 = new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fmtInt = new Intl.NumberFormat(undefined);
const money = (cents, symbol = state.settings.symbol) => symbol + fmt2.format(cents / 100);
const denomLabel = (v, symbol = state.settings.symbol) =>
  symbol + (Number.isInteger(v) ? fmtInt.format(v) : fmt2.format(v));

const sortedDenoms = () => [...state.denoms].sort((a, b) => b.value - a.value || a.type.localeCompare(b.type));
const visibleDenoms = () => sortedDenoms().filter(d => d.visible);

function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.remove('show'), 2000);
}

/* ---------- Contar ---------- */

function computeCount() {
  const items = [];
  let total = 0, pieces = 0, bills = 0, coins = 0;
  for (const d of visibleDenoms()) {
    const qty = state.draft[d.id] || 0;
    if (!qty) continue;
    const cents = toCents(d.value) * qty;
    items.push({ value: d.value, type: d.type, qty, cents });
    total += cents;
    pieces += qty;
    if (d.type === 'billete') bills += qty; else coins += qty;
  }
  return { items, total, pieces, bills, coins };
}

function renderCount() {
  const list = $('#countList');
  const denoms = visibleDenoms();
  list.innerHTML = denoms.map(d => {
    const qty = state.draft[d.id] || 0;
    return `
      <li class="denom ${d.type}" data-id="${d.id}">
        <div class="dlabel">
          <span class="tag">${d.type === 'billete' ? 'Billete' : 'Moneda'}</span>
          <strong>${esc(denomLabel(d.value))}</strong>
        </div>
        <div class="qty">
          <button class="step" data-step="-1" aria-label="Restar">&minus;</button>
          <input type="number" inputmode="numeric" min="0" step="1" placeholder="0" value="${qty || ''}">
          <button class="step" data-step="1" aria-label="Sumar">+</button>
        </div>
        <div class="sub">${esc(money(toCents(d.value) * qty))}</div>
      </li>`;
  }).join('');
  $('#countEmpty').classList.toggle('hidden', denoms.length > 0);
  updateTotals();
}

function updateTotals() {
  const { total, pieces, bills, coins } = computeCount();
  $('#grandTotal').textContent = money(total);
  $('#statPieces').textContent = fmtInt.format(pieces);
  $('#statBills').textContent = fmtInt.format(bills);
  $('#statCoins').textContent = fmtInt.format(coins);
}

function setQty(li, qty) {
  const id = li.dataset.id;
  const d = state.denoms.find(x => x.id === id);
  if (!d) return;
  qty = Math.max(0, Math.floor(Number(qty) || 0));
  if (qty) state.draft[id] = qty; else delete state.draft[id];
  save(KEYS.draft, state.draft);
  li.querySelector('.sub').textContent = money(toCents(d.value) * qty);
  updateTotals();
}

$('#countList').addEventListener('input', e => {
  if (e.target.matches('input')) setQty(e.target.closest('li'), e.target.value);
});

$('#countList').addEventListener('click', e => {
  const btn = e.target.closest('.step');
  if (!btn) return;
  const li = btn.closest('li');
  const input = li.querySelector('input');
  const qty = Math.max(0, (parseInt(input.value, 10) || 0) + Number(btn.dataset.step));
  input.value = qty || '';
  setQty(li, qty);
});

$('#countList').addEventListener('focusin', e => {
  if (e.target.matches('input')) e.target.select();
});

$('#btnClear').addEventListener('click', () => {
  if (!Object.keys(state.draft).length) return;
  if (!confirm('¿Limpiar la cuenta actual?')) return;
  state.draft = {};
  save(KEYS.draft, state.draft);
  $('#countName').value = '';
  renderCount();
});

$('#btnSave').addEventListener('click', () => {
  const { items, total, pieces } = computeCount();
  if (!total) return toast('Ingresa al menos una cantidad');
  state.history.unshift({
    id: uid(),
    date: new Date().toISOString(),
    name: $('#countName').value.trim(),
    symbol: state.settings.symbol,
    total, pieces, items,
  });
  save(KEYS.history, state.history);
  state.draft = {};
  save(KEYS.draft, state.draft);
  $('#countName').value = '';
  renderCount();
  renderHistory();
  toast('Cuenta guardada');
});

/* ---------- Historial ---------- */

const fmtDate = iso => new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });

function renderHistory() {
  $('#historyList').innerHTML = state.history.map(h => `
    <li data-id="${h.id}">
      <div>
        <strong>${esc(h.name || 'Cuenta sin nombre')}</strong>
        <small>${esc(fmtDate(h.date))} · ${fmtInt.format(h.pieces)} piezas</small>
      </div>
      <span class="amount">${esc(money(h.total, h.symbol))}</span>
    </li>`).join('');
  $('#historyEmpty').classList.toggle('hidden', state.history.length > 0);
  $('#btnClearHistory').classList.toggle('hidden', state.history.length === 0);
}

let currentDetail = null;

$('#historyList').addEventListener('click', e => {
  const li = e.target.closest('li');
  if (!li) return;
  const h = state.history.find(x => x.id === li.dataset.id);
  if (!h) return;
  currentDetail = h;
  $('#detailTitle').textContent = h.name || 'Cuenta sin nombre';
  $('#detailDate').textContent = fmtDate(h.date);
  $('#detailRows').innerHTML = h.items.map(i => `
    <tr>
      <td>${esc(denomLabel(i.value, h.symbol))} <small class="muted">${i.type === 'billete' ? 'B' : 'M'}</small></td>
      <td>${fmtInt.format(i.qty)}</td>
      <td>${esc(money(i.cents, h.symbol))}</td>
    </tr>`).join('');
  $('#detailPieces').textContent = fmtInt.format(h.pieces);
  $('#detailTotal').textContent = money(h.total, h.symbol);
  $('#detailDialog').showModal();
});

$('#btnDetailClose').addEventListener('click', () => $('#detailDialog').close());

$('#btnDetailDelete').addEventListener('click', () => {
  if (!currentDetail || !confirm('¿Eliminar esta cuenta del historial?')) return;
  state.history = state.history.filter(x => x.id !== currentDetail.id);
  save(KEYS.history, state.history);
  $('#detailDialog').close();
  renderHistory();
  toast('Cuenta eliminada');
});

$('#btnDetailLoad').addEventListener('click', () => {
  if (!currentDetail) return;
  state.draft = {};
  for (const item of currentDetail.items) {
    let d = state.denoms.find(x => x.value === item.value && x.type === item.type);
    if (!d) {
      d = { id: uid(), value: item.value, type: item.type, visible: true };
      state.denoms.push(d);
    }
    d.visible = true;
    state.draft[d.id] = item.qty;
  }
  save(KEYS.denoms, state.denoms);
  save(KEYS.draft, state.draft);
  $('#countName').value = currentDetail.name || '';
  $('#detailDialog').close();
  renderAll();
  showView('count');
  toast('Cuenta cargada en el contador');
});

$('#btnClearHistory').addEventListener('click', () => {
  if (!confirm('¿Borrar todo el historial? Esta acción no se puede deshacer.')) return;
  state.history = [];
  save(KEYS.history, state.history);
  renderHistory();
});

/* ---------- Denominaciones ---------- */

function renderDenoms() {
  $('#denomList').innerHTML = sortedDenoms().map(d => `
    <li data-id="${d.id}">
      <label>
        <input type="checkbox" ${d.visible ? 'checked' : ''}>
        <span class="tag ${d.type}">${d.type === 'billete' ? 'Billete' : 'Moneda'}</span>
        <strong>${esc(denomLabel(d.value))}</strong>
      </label>
      <button class="icon-btn" aria-label="Eliminar denominación">&times;</button>
    </li>`).join('') || '<li class="muted">Sin denominaciones</li>';
  $('#symbolInput').value = state.settings.symbol;
}

$('#denomList').addEventListener('change', e => {
  if (!e.target.matches('input[type=checkbox]')) return;
  const d = state.denoms.find(x => x.id === e.target.closest('li').dataset.id);
  d.visible = e.target.checked;
  save(KEYS.denoms, state.denoms);
  renderCount();
});

$('#denomList').addEventListener('click', e => {
  if (!e.target.closest('.icon-btn')) return;
  const id = e.target.closest('li').dataset.id;
  const d = state.denoms.find(x => x.id === id);
  if (!confirm(`¿Eliminar la denominación ${denomLabel(d.value)}?`)) return;
  state.denoms = state.denoms.filter(x => x.id !== id);
  delete state.draft[id];
  save(KEYS.denoms, state.denoms);
  save(KEYS.draft, state.draft);
  renderAll();
});

$('#addForm').addEventListener('submit', e => {
  e.preventDefault();
  const value = Math.round(parseFloat($('#newValue').value) * 100) / 100;
  const type = $('#newType').value;
  if (!(value > 0)) return toast('Valor inválido');
  if (state.denoms.some(d => d.value === value && d.type === type)) return toast('Esa denominación ya existe');
  state.denoms.push({ id: uid(), value, type, visible: true });
  save(KEYS.denoms, state.denoms);
  $('#newValue').value = '';
  renderAll();
  toast(`Agregado: ${denomLabel(value)}`);
});

$('#btnReset').addEventListener('click', () => {
  if (!confirm('¿Restaurar las denominaciones predeterminadas? Se eliminarán las personalizadas y la cuenta actual.')) return;
  state.denoms = defaultDenoms();
  state.draft = {};
  save(KEYS.denoms, state.denoms);
  save(KEYS.draft, state.draft);
  renderAll();
});

$('#symbolInput').addEventListener('input', e => {
  state.settings.symbol = e.target.value.trim();
  save(KEYS.settings, state.settings);
  renderCount();
  $('#denomList').querySelectorAll('li').forEach(li => {
    const d = state.denoms.find(x => x.id === li.dataset.id);
    if (d) li.querySelector('strong').textContent = denomLabel(d.value);
  });
});

/* ---------- Navegación ---------- */

function showView(name) {
  document.querySelectorAll('.view').forEach(v => v.classList.toggle('active', v.id === `view-${name}`));
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.view === name));
  window.scrollTo(0, 0);
}

document.querySelectorAll('.tab').forEach(t => t.addEventListener('click', () => showView(t.dataset.view)));

function renderAll() {
  renderCount();
  renderHistory();
  renderDenoms();
}

save(KEYS.denoms, state.denoms);
renderAll();

if ('serviceWorker' in navigator && location.protocol === 'https:' && !window.Capacitor) {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
