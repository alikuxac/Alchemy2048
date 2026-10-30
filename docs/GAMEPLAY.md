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
3. The machine consumes 1 starter item, charges the base FE cost, and places two Tier 1 tiles on the board.

## 4. Controls and Movement

- Movement: `W` (up), `S` (down), `A` (left), `D` (right), or arrow keys.
- Merging: sliding two identical adjacent tiers together merges them into the next tier ($\text{Tier} + 1$).
- Spawning: every legal move spawns a new Tier 1 or Tier 2 tile in an empty slot.
- Victory: reaching Tier 11 (value 2048) claims all tiles directly to the player inventory and resets the board.

## 5. Claiming Rewards

- Claim Max: awards only the highest-tier item on the board, discarding lower tiers, and resets the board.
- Claim All: converts every tile on the board into its corresponding item and clears the grid.
- Game Over: triggers when all 16 cells fill with no valid moves remaining. The board locks until Claim Max or Claim All is selected.

## 6. Multiplayer and Mutex Locks

- Mutex lock: `activePlayerUUID` restricts board access to one player. If the current player moves farther than 8 blocks away or disconnects, the server clears the lock.
- Ownership: the player who starts a board is stored as `boardOwnerUUID`.
- Anti-sniping: when a non-owner claims another player's board, `coopTaxRatio` (default 50%) destroys a percentage of the items. Setting `enableContributionRatio = true` distributes rewards according to the percentage of total moves each player made.
