/*
 * Command Quest - browser front end.
 *
 * The server is stateless: it never remembers a player between requests. This
 * file therefore owns the whole game state - the folder tree, where the player
 * is standing, and which lessons they have passed - and sends it along with
 * every command. That is what makes a sleeping free-tier container harmless.
 *
 * Accounts also live entirely here. No password or code is ever sent to the
 * server; its only job is to pick whose saved progress to load.
 */
(() => {
  'use strict';

  // ---------------------------------------------------------------- config

  // Same-origin during local development; the deployed API otherwise. Set
  // window.CQ_API before this script to point somewhere else.
  const API = window.CQ_API || (
    ['localhost', '127.0.0.1'].includes(location.hostname)
      ? 'http://localhost:8080'
      : 'https://command-quest-1.onrender.com'
  );

  const SEED = { name: 'root', files: ['todo.md', 'notes.txt'], subFolders: {} };
  const GUEST = '__guest__';

  // ---------------------------------------------------------------- storage

  const store = {
    users:    () => JSON.parse(localStorage.getItem('cq.users') || '{}'),
    saveUsers: (u) => localStorage.setItem('cq.users', JSON.stringify(u)),
    save: (user, data) => localStorage.setItem(`cq.profile.${user}`, JSON.stringify(data)),
    load: (user) => {
      try { return JSON.parse(localStorage.getItem(`cq.profile.${user}`)); }
      catch { return null; }
    },
    clear: (user) => localStorage.removeItem(`cq.profile.${user}`)
  };

  // ---------------------------------------------------------------- state

  let lessons = {};          // id -> {title, body, example}
  let lessonIds = [];
  let state = null;          // { user, lessonId, tree, path, done[] }

  const $ = (id) => document.getElementById(id);

  // ---------------------------------------------------------------- gate

  function showGate() {
    $('gate').hidden = false;
    $('game').hidden = true;
    $('gate-choices').hidden = false;
    $('gate-form').hidden = true;
    $('gate-error').hidden = true;
  }

  let gateMode = 'new';

  document.querySelectorAll('[data-gate]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const mode = btn.dataset.gate;

      if (mode === 'guest') { startSession(GUEST); return; }
      if (mode === 'back') { showGate(); return; }

      gateMode = mode;
      $('gate-form-title').textContent = mode === 'new' ? 'Make an account' : 'Log in';
      $('gate-choices').hidden = true;
      $('gate-form').hidden = false;
      $('gate-error').hidden = true;
      $('gate-username').focus();
    });
  });

  $('gate-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const name = $('gate-username').value.trim();
    const pin = $('gate-pin').value.trim();
    const fail = (msg) => { $('gate-error').textContent = msg; $('gate-error').hidden = false; };

    // The console version accepts an empty username; this does not.
    if (!name) return fail('Please choose a username.');
    if (!/^\d{4}$/.test(pin)) return fail('The code must be exactly 4 digits.');

    const users = store.users();

    if (gateMode === 'new') {
      if (users[name]) return fail('That username is taken. Try another.');
      users[name] = { pin };
      store.saveUsers(users);
    } else {
      if (!users[name] || users[name].pin !== pin) return fail('Wrong username or code.');
    }

    startSession(name);
  });

  $('signout').addEventListener('click', () => {
    if (state && state.user === GUEST) store.clear(GUEST);   // guests leave nothing behind
    state = null;
    $('gate-username').value = '';
    $('gate-pin').value = '';
    showGate();
  });

  // ---------------------------------------------------------------- session

  function startSession(user) {
    const saved = store.load(user);

    state = (saved && saved.tree)
      ? { user, ...saved }
      : { user, lessonId: lessonIds[0] || 'touch', tree: structuredClone(SEED), path: [], done: [] };

    state.user = user;

    $('gate').hidden = true;
    $('game').hidden = false;
    $('who').textContent = user === GUEST ? 'guest' : user;

    $('term').innerHTML = '';
    print('Welcome to Command Quest. Pick a lesson, then type the command.', 'term-hint');

    renderLessonTabs();
    selectLesson(state.lessonId);
    renderTree();
    $('term-input').focus();
  }

  function persist() {
    if (!state) return;
    const { user, ...rest } = state;
    store.save(user, rest);
  }

  // ---------------------------------------------------------------- lessons

  function renderLessonTabs() {
    const tabs = $('lesson-tabs');
    tabs.innerHTML = '';

    lessonIds.forEach((id) => {
      const tab = document.createElement('button');
      tab.className = 'tab' + (state.done.includes(id) ? ' done' : '');
      tab.textContent = id;
      tab.setAttribute('role', 'tab');
      tab.setAttribute('aria-selected', String(id === state.lessonId));
      tab.addEventListener('click', () => selectLesson(id));
      tabs.appendChild(tab);
    });

    const dots = $('progress-dots');
    dots.replaceChildren(...lessonIds.map((id) => {
      const dot = document.createElement('span');
      dot.className = 'dot' + (state.done.includes(id) ? ' on' : '');
      return dot;
    }));
    $('progress-label').textContent = `${state.done.length}/${lessonIds.length || 4}`;
  }

  function selectLesson(id) {
    if (!lessons[id]) return;
    state.lessonId = id;

    $('lesson-title').textContent = lessons[id].title;
    $('lesson-body').textContent = lessons[id].body;
    $('lesson-example').textContent = lessons[id].example;

    renderLessonTabs();
    $('term-input').focus();
    persist();
  }

  // ---------------------------------------------------------------- terminal

  function print(text, cls) {
    const line = document.createElement('p');
    line.className = 'term-line' + (cls ? ' ' + cls : '');
    line.textContent = text;
    $('term').appendChild(line);
    $('term').scrollTop = $('term').scrollHeight;
  }

  $('term-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const input = $('term-input');
    const typed = input.value.trim();
    if (!typed) return;
    input.value = '';
    print(typed, 'term-echo');
    send(typed);
  });

  // ---------------------------------------------------------------- file tree

  function renderTree(changed) {
    const root = $('tree');
    root.innerHTML = '';
    root.appendChild(nodeFor(state.tree, changed));
    $('breadcrumb').textContent = ['root', ...state.path].join('/');
  }

  /*
   * Built with createElement and textContent throughout, never innerHTML.
   *
   * Folder and file names are player input: they are typed into the terminal,
   * sent to the server, and come straight back. The command patterns happen to
   * reject angle brackets today, but a validation rule is a poor place to rest
   * an escaping guarantee - textContent cannot be talked into parsing markup,
   * so the guarantee holds no matter what the patterns later allow.
   */
  function nodeFor(folder, changed) {
    const li = document.createElement('li');

    const label = document.createElement('span');
    label.className = 'is-folder';
    label.textContent = folder.name + '/';
    li.appendChild(label);

    const files = [...(folder.files || [])].sort();
    const subs = Object.keys(folder.subFolders || {}).sort();
    const ul = document.createElement('ul');

    if (!files.length && !subs.length) {
      const empty = document.createElement('li');
      empty.className = 'is-empty';
      empty.textContent = 'empty';
      ul.appendChild(empty);
      li.appendChild(ul);
      return li;
    }

    subs.forEach((name) => {
      const child = nodeFor(folder.subFolders[name], changed);
      if (name === changed) child.classList.add('just-added');
      ul.appendChild(child);
    });

    files.forEach((name) => {
      const item = document.createElement('li');
      item.textContent = name;
      if (name === changed) item.classList.add('just-added');
      ul.appendChild(item);
    });

    li.appendChild(ul);
    return li;
  }

  // ---------------------------------------------------------------- status

  function status(message, isError) {
    const bar = $('status');
    if (!message) { bar.hidden = true; return; }
    bar.textContent = message;
    bar.className = 'status' + (isError ? ' error' : '');
    bar.hidden = false;
  }

  // ---------------------------------------------------------------- api

  async function send(typed) {
    // The name the player is trying to create, so the tree can flash it.
    const target = typed.split(/\s+/)[1];
    const slow = setTimeout(() => status('Waking the server — this can take up to a minute on a free plan…'), 1200);

    try {
      const res = await fetch(`${API}/api/command`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          lessonId: state.lessonId,
          command: typed,
          path: state.path,
          state: state.tree
        })
      });

      clearTimeout(slow);

      if (!res.ok) {
        const detail = await res.json().catch(() => ({}));
        print(detail.error || `The server refused that (HTTP ${res.status}).`, 'term-no');
        return;
      }

      status(null);
      const data = await res.json();

      print(data.output, data.correct ? 'term-ok' : 'term-no');
      if (data.hint) print(`Try: ${data.hint}`, 'term-hint');

      state.tree = data.state;
      state.path = data.path || [];

      if (data.correct && !state.done.includes(state.lessonId)) {
        state.done.push(state.lessonId);
        renderLessonTabs();
      }

      renderTree(data.correct ? target : undefined);
      persist();

    } catch (err) {
      clearTimeout(slow);
      status('Cannot reach the server. It may be starting up — try again in a moment.', true);
      print('Network error.', 'term-no');
    }
  }

  // ---------------------------------------------------------------- boot

  async function boot() {
    // Fire and forget: this starts a sleeping container warming up while the
    // player is still reading, so the first real command does not stall.
    fetch(`${API}/api/health`).catch(() => {});

    try {
      const res = await fetch(`${API}/api/lessons`);
      lessons = await res.json();
      lessonIds = Object.keys(lessons);
    } catch {
      status('Cannot reach the server, so lessons could not load. Refresh to retry.', true);
    }

    showGate();
  }

  boot();
})();
