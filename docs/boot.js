/*
 * Command Quest - waking the API.
 *
 * The page is served from a CDN and the API from a free container that stops
 * after about fifteen minutes idle. Render does not refuse requests to a
 * stopped container: it accepts the connection, holds it while the container
 * boots, and then proxies it through. So a fetch does not fail fast and report
 * a problem - it simply hangs, for longer than anyone will wait at a page that
 * looks finished.
 *
 * This file therefore imposes its own deadline on each attempt and retries,
 * showing which of "asleep" and "down" is happening. When the lessons arrive
 * it hands them to app.js, which fills them in wherever the player has got to.
 * Nobody has to reload to discover the server woke up.
 */
(() => {
  'use strict';

  // ---------------------------------------------------------------- tuning

  const DEADLINE        = 90_000;   // whole sequence, not per stage
  const ATTEMPT_TIMEOUT = 15_000;   // one request
  const BACKOFF         = [1000, 2000, 4000, 6000, 8000];
  const REVEAL_AFTER    = 800;      // don't flash the panel at a warm server

  const NOTE = 'Free plan — the server sleeps when idle. The first visit takes about 30 seconds.';

  // ---------------------------------------------------------------- sequence

  function formatElapsed(ms) {
    const total = Math.max(0, Math.floor(ms / 1000));
    const mins = Math.floor(total / 60);
    const secs = total % 60;
    return mins ? `${mins}m ${secs}s` : `${secs}s`;
  }

  function backoffFor(attempt) {
    // Clamped rather than indexed straight in: past the end of the table this
    // would be undefined, which reaches setTimeout as NaN and fires at once,
    // turning the backoff into a hot loop against a server that is struggling.
    return BACKOFF[Math.min(attempt, BACKOFF.length - 1)];
  }

  /*
   * Runs each stage in turn, retrying it until it answers or the shared
   * deadline passes.
   *
   * The clock and the sleep are parameters rather than globals so the tests can
   * supply fakes. That is not ceremony: the assertion that matters most - that
   * this gives up instead of retrying forever - is otherwise untestable except
   * by waiting ninety real seconds for it.
   */
  async function runSequence({
    fetchFn, now, sleep, onStage, stages,
    deadline = DEADLINE, attemptTimeout = ATTEMPT_TIMEOUT
  }) {
    const started = now();
    const spent = () => now() - started >= deadline;
    const results = {};

    for (const stage of stages) {
      onStage(stage.id, 'active');
      let attempt = 0;

      for (;;) {
        if (spent()) {
          onStage(stage.id, 'failed');
          return { ok: false, failedAt: stage.id, results };
        }

        try {
          results[stage.id] = await stage.run(fetchFn, attemptTimeout);
          onStage(stage.id, 'done');
          break;
        } catch {
          if (spent()) {
            onStage(stage.id, 'failed');
            return { ok: false, failedAt: stage.id, results };
          }
          await sleep(backoffFor(attempt));
          attempt += 1;
        }
      }
    }

    return { ok: true, results };
  }

  // Node loads this file for its tests, where there is no document to wire to.
  if (typeof module === 'object' && module.exports) {
    module.exports = { formatElapsed, backoffFor, runSequence };
    return;
  }

  // ---------------------------------------------------------------- browser

  // Resolved here rather than in app.js because this script runs first, and one
  // definition beats two that can drift. Set window.CQ_API before this script
  // to point somewhere else.
  const API = window.CQ_API || (
    ['localhost', '127.0.0.1'].includes(location.hostname)
      ? 'http://localhost:8080'
      : 'https://command-quest-1.onrender.com'
  );

  const $ = (id) => document.getElementById(id);

  async function get(path, timeout) {
    // AbortSignal.timeout rather than an AbortController paired with
    // setTimeout: same intent, none of the cleanup bookkeeping.
    const res = await fetch(`${API}${path}`, { signal: AbortSignal.timeout(timeout) });
    if (!res.ok) throw new Error(`${path} returned ${res.status}`);
    return res;
  }

  const STAGES = [
    {
      id: 'contact',
      run: (_fetchFn, timeout) => get('/api/health', timeout)
    },
    {
      id: 'lessons',
      run: async (_fetchFn, timeout) => {
        const body = await (await get('/api/lessons', timeout)).json();
        // An empty set would leave the game unplayable while looking loaded,
        // which is the failure this whole file exists to stop happening.
        if (!body || !Object.keys(body).length) throw new Error('no lessons in response');
        return body;
      }
    }
  ];

  function mark(id, state) {
    const step = document.querySelector(`#boot-steps [data-step="${id}"]`);
    if (step) step.className = `is-${state}`;
  }

  function reset() {
    $('boot-title').textContent = 'waking server';
    $('boot-elapsed').textContent = '0s';
    $('boot-note').textContent = NOTE;
    $('boot-retry').hidden = true;
    $('boot').classList.remove('is-failed');
    document.querySelectorAll('#boot-steps li').forEach((li) => { li.className = ''; });
  }

  async function start(onReady) {
    reset();
    const started = performance.now();

    const reveal = setTimeout(() => { $('boot').hidden = false; }, REVEAL_AFTER);
    const tick = setInterval(() => {
      $('boot-elapsed').textContent = formatElapsed(performance.now() - started);
    }, 1000);

    const outcome = await runSequence({
      fetchFn: fetch,
      // Monotonic. Date.now() follows the wall clock, so an NTP correction or a
      // laptop resuming from sleep could run the counter backwards.
      now: () => performance.now(),
      sleep: (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
      onStage: mark,
      stages: STAGES
    });

    clearTimeout(reveal);
    clearInterval(tick);

    if (outcome.ok) {
      $('boot').hidden = true;
      onReady(outcome.results.lessons);
      return;
    }

    $('boot').hidden = false;
    $('boot').classList.add('is-failed');
    $('boot-title').textContent = 'server unreachable';
    $('boot-elapsed').textContent = formatElapsed(performance.now() - started);
    $('boot-note').textContent =
      'The API may be down rather than asleep. Making an account still works — it is saved in this browser.';
    $('boot-retry').hidden = false;
    $('boot-retry').onclick = () => start(onReady);
  }

  window.CQBoot = { API, start };
})();
