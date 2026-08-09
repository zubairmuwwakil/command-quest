/*
 * Tests for the API wake sequence in docs/boot.js.
 *
 * Node's built-in runner, so the front end gains a test suite without gaining
 * a package.json, a node_modules, or a build step. Run with:
 *
 *   node --test test/
 *
 * The engine takes its clock and its sleep as parameters, which is what lets
 * these tests drive a fake clock. Asserting that the sequence gives up after
 * ninety seconds should not cost ninety seconds - a suite that slow is a suite
 * nobody runs.
 */
'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { formatElapsed, backoffFor, runSequence } = require('../docs/boot.js');

/*
 * A clock that only moves when the sequence sleeps. Retries therefore spend
 * the budget exactly as they would in a browser, without any real waiting.
 */
function fakeClock() {
  let t = 0;
  return {
    now: () => t,
    sleep: async (ms) => { t += ms; }
  };
}

const ok = (id, run) => ({ id, run });

test('formatElapsed counts seconds below a minute and minutes above', () => {
  assert.equal(formatElapsed(0), '0s');
  assert.equal(formatElapsed(14_000), '14s');
  assert.equal(formatElapsed(59_000), '59s');
  // The boundary a naive implementation gets wrong, reporting "60s".
  assert.equal(formatElapsed(60_000), '1m 0s');
  assert.equal(formatElapsed(112_000), '1m 52s');
});

test('backoffFor repeats its last interval instead of running off the end', () => {
  assert.equal(backoffFor(0), 1000);
  assert.equal(backoffFor(1), 2000);
  assert.equal(backoffFor(4), 8000);
  // Past the end of the table. undefined here would reach setTimeout as NaN,
  // which fires immediately - a retry loop with no gap between attempts.
  assert.equal(backoffFor(9), 8000);
  assert.equal(backoffFor(100), 8000);
});

test('a server that answers at once runs both stages and returns the lessons', async () => {
  const clock = fakeClock();
  const lessons = { touch: { title: 'Make a file' } };

  const outcome = await runSequence({
    now: clock.now,
    sleep: clock.sleep,
    onStage: () => {},
    stages: [ok('contact', async () => true), ok('lessons', async () => lessons)],
    deadline: 90_000
  });

  assert.equal(outcome.ok, true);
  assert.deepEqual(outcome.results.lessons, lessons);
});

test('a stage that fails three times still succeeds, backing off between attempts', async () => {
  const clock = fakeClock();
  const slept = [];
  let attempts = 0;

  const outcome = await runSequence({
    now: clock.now,
    sleep: async (ms) => { slept.push(ms); await clock.sleep(ms); },
    onStage: () => {},
    stages: [ok('contact', async () => {
      attempts += 1;
      if (attempts < 4) throw new Error('container still booting');
      return true;
    })],
    deadline: 90_000
  });

  assert.equal(outcome.ok, true);
  assert.equal(attempts, 4);
  assert.deepEqual(slept, [1000, 2000, 4000]);
});

test('a server that never answers gives up at the deadline rather than retrying forever', async () => {
  const clock = fakeClock();
  let attempts = 0;

  const outcome = await runSequence({
    now: clock.now,
    sleep: clock.sleep,
    onStage: () => {},
    stages: [ok('contact', async () => {
      attempts += 1;
      throw new Error('no response');
    })],
    deadline: 90_000
  });

  assert.equal(outcome.ok, false);
  assert.equal(outcome.failedAt, 'contact');
  assert.ok(clock.now() >= 90_000, `should spend the whole budget, spent ${clock.now()}`);
  assert.ok(attempts < 50, `retries should be bounded, made ${attempts}`);
});

test('a stage failing partway through reports that stage, not the first one', async () => {
  const clock = fakeClock();

  const outcome = await runSequence({
    now: clock.now,
    sleep: clock.sleep,
    onStage: () => {},
    stages: [
      ok('contact', async () => true),
      ok('lessons', async () => { throw new Error('502'); })
    ],
    deadline: 90_000
  });

  assert.equal(outcome.ok, false);
  assert.equal(outcome.failedAt, 'lessons');
});

test('stages are reported active then done, in order', async () => {
  const clock = fakeClock();
  const seen = [];

  await runSequence({
    now: clock.now,
    sleep: clock.sleep,
    onStage: (id, state) => seen.push(`${id}:${state}`),
    stages: [ok('contact', async () => true), ok('lessons', async () => ({}))],
    deadline: 90_000
  });

  assert.deepEqual(seen, [
    'contact:active', 'contact:done',
    'lessons:active', 'lessons:done'
  ]);
});

test('a failing stage is reported failed, so the panel can mark it', async () => {
  const clock = fakeClock();
  const seen = [];

  await runSequence({
    now: clock.now,
    sleep: clock.sleep,
    onStage: (id, state) => seen.push(`${id}:${state}`),
    stages: [ok('contact', async () => { throw new Error('down'); })],
    deadline: 5_000
  });

  assert.equal(seen[0], 'contact:active');
  assert.equal(seen.at(-1), 'contact:failed');
});
