# Alchemy 2048: Configuration

Configuration files split into server settings (`alchemy2048-server.toml`) and client settings (`alchemy2048-client.toml`).

## 1. Server Configuration (`ServerConfig`)

File path: `config/alchemy2048-server.toml` (or the world save directory). Options reside in the `[gameplay]` table. Energy capacity and transfer limits update without restarting the server.

| Parameter | Type | Default | Valid Range | Description |
| :--- | :--- | :--- | :--- | :--- |
| `baseEnergyCost` | Integer | `20` | `0` - `10,000` | Base Forge Energy (FE) cost for Tier 0 and Tier 1 moves. Scales by $2^{\text{Tier} - 1}$ at higher tiers. |
| `energyCapacity` | Integer | `50000` | `1,000` - `1,000,000` | Maximum internal FE buffer for the Alchemy Matrix. |
| `maxEnergyReceive` | Integer | `2500` | `0` - `100,000` | Maximum FE accepted per tick. |
| `mutexDistanceBlocks` | Double | `8.0` | `2.0` - `64.0` | Maximum distance a player can step from the block before releasing the interaction lock. |
| `enableContributionRatio`| Boolean | `false` | `true` / `false` | Scales multiplayer claims by the share of moves each player made on the board. |
| `coopTaxRatio` | Double | `0.5` | `0.0` - `1.0` | Item loss rate applied when a non-owner claims a board and `enableContributionRatio` is false. |

### Presets
- Casual / Singleplayer:
  ```toml
  baseEnergyCost = 20
  enableContributionRatio = false
  coopTaxRatio = 0.0
  ```
- Hardcore / Dedicated Server:
  ```toml
  baseEnergyCost = 50
  energyCapacity = 100000
  maxEnergyReceive = 1000
  enableContributionRatio = true
  ```

## 2. Client Configuration (`ClientConfig`)

File path: `config/alchemy2048-client.toml`.

GUI buttons for theme (`🎨`) and numbering (`#`) modify the active session only. Changes must be saved to `alchemy2048-client.toml` to persist across restarts.

| Parameter | Type | Default | Options | Description |
| :--- | :--- | :--- | :--- | :--- |
| `theme` | Enum | `"dark"` | `dark`, `steampunk`, `minimal`, `retro_2048` | UI color scheme. |
| `numberDisplay` | Enum | `"exponential"` | `exponential`, `tier_index`, `none` | Number format: powers of two (`2`, `4`), tier index (`T1`, `T2`), or hidden (`none`). |
| `playUiSounds` | Boolean | `true` | `true` / `false` | Enables move, merge, and claim sound effects. |
| `showTileTooltip` | Boolean | `true` | `true` / `false` | Displays item names and tiers on tile hover. |

## 3. Themes
- `dark`: Charcoal background (`#2B2B2B`) with gold accents (`#FFD700`).
- `steampunk`: Bronze and copper paneling with warm orange progress bars.
- `minimal`: Slate background with cyan accents (`#61AFEF`).
- `retro_2048`: Classic 2048 beige and orange palette.
