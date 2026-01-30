# Project status: Host/Guest multi-client mirroring (custom RuneLite)

## What we’ve built so far

### Host/Guest plugins exist and are functional at a basic level

Located in:

`/RuneLiteCustom/runelite-client/src/main/java/net/runelite/client/plugins/`

- **[Host]** `prushhost/`
  - `PrushHostPlugin`
    - Captures interactions primarily via `@Subscribe MenuOptionClicked`
    - Mirrors:
      - `MENU_ACTION` (generic RuneLite menu replay)
      - `WALK_WORLD` (special-cased walk based on scene->world conversion)
      - `DIALOG_CONTINUE` (space bar maps to widget-continue)
  - `PrushHostBroadcaster`
    - TCP client that connects to multiple configured guests (`host:port` list)
    - Sends newline-delimited JSON payloads
  - `PrushHostConfig`
    - `enabled`
    - `guestTargets` (comma-separated `host:port`)
    - `connectTimeoutMs`

- **[Guest]** `prushguest/`
  - `PrushGuestPlugin`
    - Runs a TCP server locally
    - Receives JSON lines, parses to `PrushAction`
    - Validates:
      - protocol version
      - enabled flag
      - drop if tick lag exceeds `maxTickLag`
    - Replays actions on `ClientThread` using `client.menuAction(...)`
  - `PrushGuestServer`
    - Simple single-connection TCP accept loop
  - `PrushGuestConfig`
    - `enabled`
    - `listenPort`
    - `maxTickLag`

- **[Shared model]** `prushsync/`
  - `PrushActionType` (`MENU_ACTION`, `WALK_WORLD`, `DIALOG_CONTINUE`)
  - `PrushAction` (protocol fields + menu action fields + optional world fields)
  - `PrushSyncGson` (currently a plain `new GsonBuilder().create()`)

### Documentation exists

- `prushsync/README_PrushSync.md` describes the philosophy and action schema.

## What we’re currently doing

- **De-scoping** the repo: separating Host/Guest mirroring + custom RuneLite work from unrelated projects (FlipManager, etc.).
- Preparing to move this into a **new clean project folder** focused only on custom RuneLite + Host/Guest.

## Goal

A clean, maintainable **custom RuneLite build** where:

- One client runs **Host mode** and mirrors semantic inputs.
- One or more clients run **Guest mode** and deterministically replay those actions.
- The system is robust enough for:
  - login flows
  - navigation
  - interacting with widgets
  - basic NPC/object interactions

## What’s next (recommended order)

### 1) Create the new clean repo with only RuneLiteCustom + prush* plugins

- Copy the `RuneLiteCustom` Maven multi-module tree
- Ensure the build tooling is intact (Maven wrapper or documented Maven requirement)

### 2) Improve reliability and reduce desync

- **[Tick alignment]** optionally queue actions and execute on a target tick instead of immediate best-effort
- **[State gating]** drop/ignore actions when guest state obviously doesn’t match (login screen vs in-game, etc.)
- **[Connection robustness]** guest should accept reconnects cleanly, host should retry connections

### 3) Expand mirrored input coverage

- Mirror additional high-value events beyond `MenuOptionClicked`:
  - camera
  - keybind-based actions
  - inventory actions not captured as menu events (if any)

### 4) Decide on architecture

- **[Short term]** keep this as an in-tree plugin in a RuneLite fork (fast iteration).
- **[Long term]** consider refactoring to an external plugin repo if you want easier updates and less fork maintenance.

## Open questions for you (so the new repo matches your intent)

- Do you want **one combined plugin** with a Host/Guest toggle, or keep **two plugins** (`Prush Host` and `Prush Guest`)?
- Should Guests be **LAN-only** always, or do you want optional remote / encrypted transport later?
