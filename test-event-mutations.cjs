const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.join(__dirname, 'src/main/resources/static');

async function checkPage(page) {
  const html = fs.readFileSync(path.join(root, page), 'utf8');
  const elements = new Map([...html.matchAll(/\bid="([^"]+)"/g)].map(match => [match[1], {
    value: '', checked: false, innerHTML: '', style: { display: 'none' }, addEventListener() {},
    classList: { add() {}, remove() {} }
  }]));
  const calls = [], toasts = [];
  let confirmations = 0;
  const context = vm.createContext({ document: {
    getElementById(id) { return elements.get(id) || null; }, addEventListener() {},
    querySelectorAll() { return []; }, body: { style: {} }
  }, window: {}, console, requireAuth() {}, getUser() { return null; },
    formatDateTime(value) { return value; }, setTimeout() {},
    showToast(message) { toasts.push(message); }, confirm() { confirmations++; return true; },
    async apiCall(url, method, payload) { calls.push({ url, method, payload }); return []; }
  });
  for (const match of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)) {
    const src = match[1].match(/src="([^"]+)"/);
    if (!src) vm.runInContext(match[2], context);
    else if (src[1] === '/js/event-permissions.js')
      vm.runInContext(fs.readFileSync(path.join(root, src[1]), 'utf8'), context);
  }
  const future = new Date(Date.now() + 86400000).toISOString().slice(0, 19);
  const past = new Date(Date.now() - 86400000).toISOString().slice(0, 19);
  const base = { id: 1, title: 'Event', startTime: future, endTime: '2035-01-01T12:00:00',
    status: 'OPEN_FOR_REGISTRATION', canModify: true, registeredAttendees: 0,
    categoryId: 1, maxAttendees: 10 };
  let scenarios = 0;
  for (const locked of [
    { ...base, canModify: false, modificationBlockedReason: 'Already has registration history' },
    { ...base, startTime: past },
    { ...base, status: 'ONGOING' }
  ]) {
    context.testEvent = locked;
    vm.runInContext('allEventsData = [testEvent]', context);
    context.renderEventsTable([locked]);
    const buttons = elements.get('approval-table-body').innerHTML.match(/<button\b[^>]*>/g) || [];
    const mutations = buttons.filter(button => /openEditEventModal|deleteEvent/.test(button));
    assert.equal(mutations.length, 2);
    assert.ok(mutations.every(button => /\bdisabled\b/.test(button)), `${page}: locked mutation buttons must be disabled`);
    context.openEditEventModal(1);
    assert.equal(elements.get('event-edit-modal').style.display, 'none');
    await context.deleteEvent(1, 'Event');
    assert.equal(confirmations, 0);
    assert.equal(calls.length, 0);
    assert.ok(toasts.length >= 2);
    scenarios++;
  }
  context.testEvent = base;
  vm.runInContext('allEventsData = [testEvent]', context);
  context.renderEventsTable([base]);
  const unlocked = elements.get('approval-table-body').innerHTML.match(/<button\b[^>]*>/g) || [];
  assert.ok(unlocked.filter(button => /openEditEventModal|deleteEvent/.test(button)).every(button => !/\bdisabled\b/.test(button)));
  assert.equal(elements.has('edit-event-max'), false, `${page}: edit form must not expose capacity`);
  context.openEditEventModal(1);
  assert.equal(elements.get('event-edit-modal').style.display, 'flex');
  await context.saveEventEdit({ preventDefault() {} });
  const update = calls.find(call => call.method === 'PUT');
  assert.ok(update, JSON.stringify(toasts));
  assert.equal(Object.hasOwn(update.payload, 'maxAttendees'), false);
  scenarios++;
  console.log(`${page}: ${scenarios} mutation scenarios passed`);
  return scenarios;
}
(async () => {
  const count = await checkPage('organizer/events.html') + await checkPage('admin/index.html');
  console.log(`${count}/${count} scenarios passed using actual handlers and a simulated DOM.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
