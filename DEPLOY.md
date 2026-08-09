# Deploying Command Quest

The front end and the API are hosted separately, on purpose.

Free container plans put the service to sleep after about fifteen minutes idle
and take tens of seconds to wake. If the same server delivered the HTML, a
visitor would sit in front of a blank page for that whole time. Serving the page
from a CDN makes the first paint instant, and `docs/boot.js` wakes the API in
the background, showing the player what it is waiting for and filling the
lessons in whenever they arrive.

```
  Browser ──▶ GitHub Pages        index.html, app.js, style.css   (instant, free)
       │
       └────▶ Render (Docker)     Spring Boot API                  (sleeps when idle)
```

## What is already done

- `Dockerfile` — multi-stage, non-root, container-aware JVM flags
- `.dockerignore`
- `render.yaml` — service definition, so settings live in git rather than a dashboard
- `docs/` — the static site, ready for Pages
- CORS restricted to named origins, configurable per environment

## What needs you

Both steps need an account, so they cannot be scripted from here.

### 1. Deploy the API to Render

1. Sign in at <https://render.com> with your GitHub account.
2. **New → Blueprint**, pick `zubairmuwwakil/command-quest`, branch `main`.
   Render reads `render.yaml` and fills in the rest.
3. Deploy, and wait for the first build. It takes a few minutes — the Docker
   layer cache is cold and Maven downloads the dependency tree once.
4. Note the URL it gives you, something like
   `https://command-quest-1.onrender.com`.
5. Check it: `curl https://YOUR-URL/api/health` should return `{"status":"ok"}`.

Any other container host works the same way — Fly.io, Koyeb, Railway. The
Dockerfile is not Render-specific; only `render.yaml` is.

### 2. Publish the front end to GitHub Pages

1. In the repository: **Settings → Pages**.
2. Source: **Deploy from a branch**. Branch: `main`, folder: `/docs`.
3. Save. The site appears at
   `https://zubairmuwwakil.github.io/command-quest/` within a minute or two.

### 3. Point the two at each other

Two values must agree, and the app fails in a confusing way if they do not.

**The page needs the API's address.** `docs/app.js` falls back to
`https://command-quest-1.onrender.com`. If Render gave you a different URL,
edit the `API` constant near the top of that file.

**The API needs the page's origin.** In `render.yaml`, set
`COMMANDQUEST_ALLOWED_ORIGINS` to your Pages origin — the scheme and host only,
no path:

```
https://zubairmuwwakil.github.io
```

Not `https://zubairmuwwakil.github.io/command-quest/`. An origin has no path,
and including one silently fails to match.

## Checking it worked

Open the Pages URL and complete one lesson. Then, in the browser's developer
console:

| Symptom | Cause |
|---|---|
| `blocked by CORS policy` | `COMMANDQUEST_ALLOWED_ORIGINS` does not match the Pages origin exactly |
| `Mixed Content` | The API URL is `http://`. Pages is HTTPS-only, so the API must be too |
| First command hangs ~50s, then works | Normal. The container was asleep. The warm-up ping shortens this but cannot remove it |
| 404 on `app.js` | An asset path lost its leading `./`. Pages serves from a subpath |
| Lessons never load | The API is down or the URL is wrong. Check `/api/health` directly |

## Running it locally

Two terminals:

```bash
./mvnw spring-boot:run
```

```bash
cd docs && python3 -m http.server 5500
```

Then open <http://localhost:5500>. `app.js` points at `localhost:8080`
automatically when served from localhost, and the default CORS configuration
already allows port 5500.

## The cold start, honestly

The delay is still there. `docs/boot.js` does not remove it — it makes it
legible, which is a different and smaller claim than this file used to make.

An earlier version said a warm-up ping "hides the delay". It did not. The ping
was fired and forgotten, nothing observed its result, and the page went on
looking finished and inert while the container booted. The one message that
would have explained the wait was written into an element inside a hidden
parent, so it never rendered once. A visitor's only way to learn the server had
woken was to reload and see.

What happens now, on a cold URL:

1. The gate paints immediately. Making an account or logging in works straight
   away — accounts are `localStorage` and never touch the server.
2. After 800ms, if the API has not answered, a boot panel appears under the
   buttons: the stage being attempted, a tick mark per stage completed, and an
   elapsed counter.
3. Each request carries a 15-second deadline of its own. Render holds
   connections to a sleeping container open rather than refusing them, so a
   plain `fetch` would hang rather than fail, and nothing would ever advance.
   Failed attempts back off — one second, two, four, six, then eight.
4. When the lessons arrive they are filled in wherever the player has got to,
   including into a game they already entered. Nobody reloads.
5. After ninety seconds — roughly three times a typical wake — it stops and
   says the API may be down rather than asleep, and offers **Try again**.

If the wait itself matters, and for a live demo or an interviewer clicking the
link it might, the options remain a paid instance (about $7/month) or an
external uptime pinger hitting `/api/health` every ten minutes.

An uptime pinger on a free plan is worth thinking about rather than doing
reflexively: it keeps a container running continuously to serve nobody, and
some providers consider that abuse of a free tier.

### Checking the boot screen by hand

The retry engine is covered by `test/boot.test.js` (`node --test test/*.test.js`),
which drives it with a fake clock so the ninety-second deadline is asserted in
under a millisecond. The parts that need a browser are these:

| Path | How |
|---|---|
| Cold start | Leave the service idle 15+ minutes, then load the page |
| Warm start | Reload at once — the panel must **not** flash |
| Failure and retry | Run `window.CQ_API = 'https://example.invalid'` before the scripts, reload, wait 90s |
| Late arrival | Throttle to Slow 3G, choose *Continue as guest* immediately — the lesson panel must fill in without a reload |
