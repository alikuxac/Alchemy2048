# Alchemy 2048: Gameplay

Alchemy 2048 runs a $4 \times 4$ sliding tile matrix inside Minecraft. Players feed starter items (such as cobblestone or saplings) into an Alchemy Matrix block and combine matching tiers to produce higher-tier items up to tier 11 (value 2048).

## 1. Crafting and Block Properties

Crafting grid:
- Center: 1 Crafting Table
- Top, bottom, left, right: 4 Pistons facing inward
- Corners: 4 Copper Ingots

Block behavior:
- Pickaxe required to break.
- Piston-proof (`PushReaction.BLOCK`) to prevent movement exploits.
- Breaking the block drops any active items and remnants on the ground.

## 2. Power Requirements

The block requires Forge Energy (FE) to slide tiles:
- Buffer capacity: 50,000 FE (default).
- Max receive rate: 2,500 FE/t (default).
- Slide cost:
  $$\text{Cost}(\text{Tier}) = \min(\text{BaseCost} \times 2^{\text{Tier} - 1}, \text{Capacity})$$
  - Tier 0 and Tier 1: 20 FE
  - Tier 2: 40 FE
  - Tier 3: 80 FE
  - Tier 10: 10,240 FE
  - Tier 11: 20,480 FE

If internal FE is lower than the required move cost, moves are rejected and the HUD displays `EMPTY`.

## 3. Starting a Game

1. Right-click the Alchemy Matrix block.
2. On an empty board, the matrix area displays a 16-slot preset drawer. Scroll the mouse wheel to navigate pages if more than 16 valid recipes exist.
3. Select a preset with an available starter item in inventory. The machine consumes 1 starter item, charges the base FE cost, and places two Tier 1 tiles on the board. Creative mode bypasses item consumption.

## 4. Controls, Animation, and Sound

- Movement: `W` (up), `S` (down), `A` (left), `D` (right), or arrow keys.
- Animations: Tiles slide along their movement vectors using client-side linear interpolation (100 ms duration). Merged tiles expand to 115% scale before returning to standard size.
- Sound effects: Successful moves trigger mechanical latch sounds. Merges play high-pitch copper strikes that scale with tier. Blocked moves or empty power trigger a steam extinguish hiss.
- Merging: sliding two identical adjacent tiers merges them into $\text{Tier} + 1$.
- Spawning: each valid slide consumes 1 starter item from player inventory and spawns a Tier 1 or Tier 2 tile in a free cell. If the player carries 0 starter items, the machine rejects the move. Creative mode bypasses item consumption.
- Victory: reaching Tier 11 (value 2048) claims all tiles directly without gacha loss and resets the board.

## 5. Claiming Rewards and Game Over

The right-side control panel contains action buttons and status readouts:
- Extract (Claim Highest): Awards the single highest-tier item on the board and resets the grid. This action has zero failure risk.
- Reset (Claim All): Only usable during an active game or at Game Over (disabled on an empty board). Converts each board tile into items. In Survival mode, each non-empty tile rolls against `gachaFailRate` (default 50% loss chance) to shatter. Creative mode guarantees a 100% yield.
- Game Over: Triggers when all 16 cells fill with no valid moves remaining. Displays a red Game Over indicator, changes the status readout to `OVER`, and disables directional inputs until the player claims or resets.

## 6. Multiplayer and Mutex Locks

- Mutex lock: `activePlayerUUID` restricts board access to one player. If the current player moves farther than 8 blocks away or disconnects, the server clears the lock.
- Ownership: the player who starts a board is stored as `boardOwnerUUID`.
- Anti-sniping: when a non-owner claims another player's board, `coopTaxRatio` (default 50%) destroys a percentage of the items. Setting `enableContributionRatio = true` distributes rewards according to the percentage of total moves each player made.
