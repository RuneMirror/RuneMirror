# Prush Sync (Host/Guest)

## Overview

This implements a synchronized multi-client control system using two RuneLite plugins:

- **Prush Host**: captures semantic interactions from the host client and broadcasts them.
- **Prush Guest**: receives interactions and replays them using RuneLite APIs.

Design philosophy is intentionally aligned with the existing **Axiom** plugin:

- Local network connection
- `Gson` serialization
- All client interaction occurs on `ClientThread`
- No OS-level mouse movement
- No `Robot` / AWT input simulation
- No screen coordinates

## Axiom Patterns Reused

- **Local server concept**: Axiom starts a local HTTP server from `startUp()`. Here, the Guest starts a local TCP listener from `startUp()`.
- **Deterministic data model**: Axiom passes stable primitives around (ids/opcodes/params). Here we broadcast a deterministic `PrushAction` model.
- **ClientThread safety**: Axiom uses `clientThread.invoke(...)` before touching client state. Guest replays actions only from `ClientThread`.

## Action Schema

All mirrored events are serialized into a single action type (for now): `MENU_ACTION`.

`PrushAction` fields:

- `v`: protocol version (currently `1`)
- `seq`: monotonically increasing sequence number assigned by host
- `tick`: host `client.getTickCount()` at capture time
- `type`: action type (`MENU_ACTION`)
- `param0`, `param1`, `opcode`, `identifier`, `itemId`, `option`, `target`: values taken from `MenuEntry`

### Why this works for widgets + login

RuneLite fires `MenuOptionClicked` for:

- Widget clicks (eg `CC_OP`, `WIDGET_*` actions)
- NPC/object interactions
- Various UI interactions

So mirroring `MenuOptionClicked` as a `client.menuAction(...)` replay is the most direct, semantic, RuneLite-native replication channel.

## Host Plugin Responsibilities

- Subscribe to `MenuOptionClicked`
- Convert clicked `MenuEntry` into a `PrushAction`
- Broadcast JSON lines over TCP connections to all configured guests

Config:

- `Enabled`: turn mirroring on/off
- `Guest targets`: comma-separated `host:port` list
- `Connect timeout (ms)`

## Guest Plugin Responsibilities

- Start a TCP server (one connection at a time, host connects)
- Parse each JSON line into a `PrushAction`
- Validate quickly and drop invalid/late actions
- Replay using `client.menuAction(...)` **on `ClientThread`**

Config:

- `Enabled`: turn execution on/off
- `Listen port`: TCP port for host to connect to
- `Max tick lag`: drop actions older than N ticks

## Example Flow

### Host clicks a login widget (eg "Existing User")

1. Host client generates `MenuOptionClicked`.
2. Host builds `PrushAction`:
   - `type=MENU_ACTION`
   - `opcode` is the clicked `MenuAction` (often `CC_OP` or a `WIDGET_*`)
   - `param0/param1/identifier/...` copied from `MenuEntry`
   - `tick` copied from `client.getTickCount()`
3. Host broadcasts JSON line to all guests.
4. Guests receive JSON, validate it, then invoke:
   - `client.menuAction(param0, param1, MenuAction.of(opcode), identifier, itemId, option, target)`
5. Guests execute on their `ClientThread`.

## Known Limitations / Desync Cases

- **Tick-perfect execution is best-effort**: This implementation drops overly-late actions, but does not currently delay execution until a future tick.
- **Guest state must match host state**: If a guest is in a different interface state, the same `menuAction` may do nothing or do something else.
- **Instanced regions / dialogs**: Any UI divergence (random events, unexpected dialogs, instanced offsets) can cause desync.
- **NPC/object index differences**: Some `identifier` values are indices (eg NPC index). If host/guest scenes differ, indices can differ.
- **Single connection**: Guest server accepts one host connection at a time (simple by design).

## Folder/Classes

- `net.runelite.client.plugins.prushsync`
  - `PrushActionType`
  - `PrushAction`
  - `PrushSyncGson`

- `net.runelite.client.plugins.prushhost`
  - `PrushHostPlugin`
  - `PrushHostConfig`
  - `PrushHostBroadcaster`

- `net.runelite.client.plugins.prushguest`
  - `PrushGuestPlugin`
  - `PrushGuestConfig`
  - `PrushGuestServer`
