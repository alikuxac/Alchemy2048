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
2. Select a recipe from the preset panel on the left (matches items in your inventory).
3. The machine consumes 1 starter item from inventory, charges the base FE cost, and places two Tier 1 tiles on the board. Creative players bypass starter item costs.

## 4. Controls and Movement

- Movement: `W` (up), `S` (down), `A` (left), `D` (right), or arrow keys.
- Merging: sliding two identical adjacent tiers together merges them into the next tier ($\text{Tier} + 1$).
- Spawning: every legal move consumes 1 starter item from player inventory and spawns a new Tier 1 or Tier 2 tile in an empty slot. If the inventory has 0 starter items, the machine blocks the move and shows an alert. Creative mode bypasses consumption.
- Victory: reaching Tier 11 (value 2048) claims all tiles directly to the player inventory and resets the board.

## 5. Claiming Rewards and Game Over

- Claim Max: awards the highest-tier item on the board, discards lower tiers, and resets the board.
- Claim All: converts every tile on the board into its item and clears the grid.
- Early cash-out: players can claim at any time during active play using the side panel buttons.
- Game Over: triggers when all 16 cells fill with no valid moves left.
  - The board switches to a Game Over modal with red borders, hiding background tiles to keep actions readable.
  - Claim Max and Claim All buttons move directly onto the board modal.
  - The side panel switches from `PLAYING` to `GAME OVER` and displays the highest tier item achieved in a gold-bordered slot.
  - The energy display shows `OVER` and blocks further movement inputs.

## 6. Multiplayer and Mutex Locks

- Mutex lock: `activePlayerUUID` restricts board access to one player. If the current player moves farther than 8 blocks away or disconnects, the server clears the lock.
- Ownership: the player who starts a board is stored as `boardOwnerUUID`.
- Anti-sniping: when a non-owner claims another player's board, `coopTaxRatio` (default 50%) destroys a percentage of the items. Setting `enableContributionRatio = true` distributes rewards according to the percentage of total moves each player made.
