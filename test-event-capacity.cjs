// Execute the actual creation handlers with a minimal DOM, without a browser or live DB.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');

async function checkPage(filename, modal) {
  const html = fs.readFileSync(path.join(__dirname, 'src/main/resources/static/organizer', filename), 'utf8');
  const source = [...html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)]
    .filter(match => !/\bsrc\s*=/.test(match[1])).map(match => match[2]).join('\n');
  const elements = new Map();
  const calls = [];
  const toasts = [];
  const element = id => {
    if (!elements.has(id)) elements.set(id, { value: '', checked: false, style: {}, listeners: {},
      addEventListener(name, fn) { this.listeners[name] = fn; }, reset() {} });
    return elements.get(id);
  };
  const context = vm.createContext({
    document: { getElementById: element, addEventListener() {}, body: { style: {} } },
    window: { location: {} }, console, requireAuth() {}, getUser() { return null; },
    setTimeout() {}, showToast(...args) { toasts.push(args); },
    async apiCall(url, method, payload) {
      calls.push({ url, method, payload });
      return [];
    }
  });
  vm.runInContext(source, context, { filename });
  const ids = modal ? {
    title: 'create-event-title', category: 'create-event-category', location: 'create-event-location',
    startTime: 'create-event-start', endTime: 'create-event-end', maxAttendees: 'create-event-max',
    budget: 'create-event-budget', bannerUrl: 'create-event-banner', description: 'create-event-desc',
    hasSeatMap: 'create-hasSeatMap', seatRows: 'create-seatRows', seatCols: 'create-seatCols'
  } : Object.fromEntries(['title', 'category', 'location', 'startTime', 'endTime', 'maxAttendees',
    'budget', 'bannerUrl', 'description', 'hasSeatMap', 'seatRows', 'seatCols'].map(id => [id, id]));
  for (const [key, value] of Object.entries({ title: 'Capacity audit', category: '1', location: 'Room A',
    startTime: '2030-01-01T10:00', endTime: '2030-01-01T12:00', maxAttendees: '100',
    seatRows: '2', seatCols: '3' })) element(ids[key]).value = value;

  element(ids.hasSeatMap).checked = true;
  context[modal ? 'toggleCreateSeatMapInputs' : 'toggleSeatMapInputs'](true);
  assert.equal(element(ids.maxAttendees).value, '100', 'Enabling seats currently leaves the capacity input unchanged');

  const submit = modal ? context.handleCreateEvent : element('event-create-form').listeners.submit;
  await submit({ preventDefault() {} });
  let creation = calls.find(call => call.url === '/organizer/events' && call.method === 'POST');
  assert.ok(creation, JSON.stringify(toasts));
  assert.equal(creation.payload.hasSeatMap, true);
  assert.equal(creation.payload.seatRows * creation.payload.seatCols, 6);
  assert.equal(creation.payload.maxAttendees, 100, 'The handler sends the entered value; backend performs the override');

  calls.length = 0;
  element(ids.hasSeatMap).checked = false;
  await submit({ preventDefault() {} });
  creation = calls.find(call => call.url === '/organizer/events' && call.method === 'POST');
  assert.ok(creation, JSON.stringify(toasts));
  assert.equal(creation.payload.maxAttendees, 100);
  assert.equal(creation.payload.seatRows, 0);
  assert.equal(creation.payload.seatCols, 0);
  console.log(`${filename}: 3 scenarios passed (toggle, seat-map payload, no-seat-map payload)`);
}

(async () => {
  await checkPage('event-form.html', false);
  await checkPage('events.html', true);
  console.log('6/6 frontend handler scenarios passed; minimal DOM simulation, not a browser test.');
})().catch(error => { console.error(error); process.exitCode = 1; });
