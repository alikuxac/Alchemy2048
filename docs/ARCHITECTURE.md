# Alchemy 2048: Technical Architecture and Security

## 1. Network and State Architecture

### Client-Server Synchronization
- Client (`AlchemyMatrixScreen`) sends three payload types:
  - `SelectPresetPayload`: starts a game with a chosen recipe ID and inventory slot.
  - `MoveGridPayload`: slides the board in a cardinal direction (`UP`, `DOWN`, `LEFT`, `RIGHT`).
  - `ClaimPayload`: claims rewards (`MODE_HIGHEST` or `MODE_ALL`).
- Server: game logic (`Grid2048Logic`), tile generation, score tracking, energy deduction, item spawning, and NBT serialization run on the logical server in `AlchemyMatrixBlockEntity`.
- Data sync: the client container receives grid state, score, game over status, energy stored, max energy, and next move cost through 23 tracked integer fields in `ContainerData`.

## 2. Exploit Mitigation and Anti-Dupe Measures

| Attack Vector | Threat | Mitigation |
| :--- | :--- | :--- |
| Piston Displacement | Moving the block entity with pistons during a game desyncs state and duplicates items. | `PushReaction.BLOCK` set in `BlockBehaviour.Properties`. Pistons cannot move the Alchemy Matrix. |
| Concurrent Moves / Race Condition | Two players send packets simultaneously to duplicate merges or claim twice. | Mutex lock: `activePlayerUUID` restricts grid interaction to one player at a time. Other players receive an in-use notification. |
| Distance Disconnect Exploitation | Player walks away or disconnects while holding the mutex lock. | Distance check on every access: if the active player exceeds `mutexDistanceBlocks` (default `8.0`), dies, or disconnects, the server clears the lock. |
| Starter Item Exhaustion / Desync | Player drops starter items mid-game to slide without costs. | Server checks player inventory before each move and removes 1 matching item. If count is 0, the move rejects and alerts the player. Creative mode bypasses deduction. |
| Tile Overflow | Merging beyond 127 in signed `byte` overflows to `-128`. | `Grid2048Logic` caps merge values at 126 and awards victory at Tier 11. |
| Block Break Dupe | Breaking the block while transmuting to recover both starter materials and outputs. | `onRemove` in `AlchemyMatrixBlock` triggers `dropInventoryAndRemnants`, dropping all active board tiles as items and clearing the entity. |
| Packet Spam / Macro Flood | Sending move packets faster than energy tick updates to bypass power costs. | Server verifies energy before each slide. Packets without sufficient energy are dropped without state mutation. |
| Empty Board Reset Spam | Triggering Claim All on a blank board to cycle server state. | Server enforces `!isGridEmpty()` before allowing Claim All operations. |
| ItemStack Mutation on Claim | Adding items to player inventory mutates stack count to zero, turning chat hover names to air. | Server snapshots the item's `Component` display name before calling inventory insertion routines. |

## 3. Client Presentation Engine

- Tile interpolation: `TileInterpolationEngine` simulates slides and merge transitions locally upon key press. Tiles interpolate over 100 ms via linear lerp.
- Scale pop: Tiles formed by merging expand to 115% scale for 120 ms to provide tactile feedback without waiting for server network responses.
- Reactive state reconciler: When `ContainerData` arrives from the server, the client smoothly replaces pending interpolation states with the authoritative grid.
