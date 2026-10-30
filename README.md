# Alchemy 2048

**Alchemy 2048** is an alchemical puzzle and resource transmutation mod for **Minecraft 1.21.1** (NeoForge & Forge).

Combine identical materials on a $4 \times 4$ alchemical matrix using the classic 2048 mechanics to transmute base blocks into rare ores, precious gems, and endgame gear.

---

## ✨ Features
- **Authentic 2048 Sliding Puzzle**: Slide tiles using `W-A-S-D` or Arrow keys to merge identical tiers into greater alchemical elements.
- **Data-Driven Evolution Trees**: Easily customize, balance, and register progression ladders using standard datapack JSON recipes or KubeJS.
- **Energy Driven**: Powered by Forge Energy (FE) with exponential move scaling.
- **Customizable Client Themes**: 4 visual themes (`Alchemy Dark`, `Steampunk Brass`, `Minimal Slate`, `Retro 2048`) and configurable tile number styles.
- **Full Internationalization (i18n)**: Out-of-the-box support for English (`en_us`), Vietnamese (`vi_vn`), Simplified Chinese (`zh_cn`), and Russian (`ru_ru`).
- **Server/Client Config Separation**: Complete control over energy costs, capacities, co-op taxes, distance mutexes, and visual options.
- **Enterprise-Grade Anti-Dupe & Exploit Security**: Piston immunity, chunk-unload resilience, mutex player locks, and hot-reload safe ejection.

---

## 📚 Documentation
Explore the full documentation in the [`docs/`](docs/) directory:
- 🎮 [**Gameplay Guide**](docs/GAMEPLAY.md): Crafting recipes, controls, claiming strategies, and co-op penalties.
- ⚙️ [**Configuration Guide**](docs/CONFIG.md): Complete list of Server and Client options and recommended profiles.
- 📜 [**Recipes & Datapacks**](docs/RECIPES_AND_DATAPACKS.md): How to design custom evolution trees, tag starters, priority resolution, and blacklist tags.
- 🧪 [**KubeJS Integration**](docs/KUBEJS.md): Server scripting reference with copy-paste examples for modpack creators.
- 🛡️ [**Technical Architecture & Security**](docs/ARCHITECTURE.md): Exploit mitigations, mutex locks, and network synchronization details.

---

## 🛠️ Building & Developing
```bash
# Clone the repository
git clone https://github.com/alikuxac/alchemy2048.git
cd alchemy2048

# Run test suite
./gradlew test

# Build mod jars (NeoForge & Forge)
./gradlew build
```

---

## 📄 License

### Code & Project
Licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0). See the [LICENSE](LICENSE) file for details.

### 3D Item Models
Custom 3D item models are licensed under Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International (CC BY-NC-ND 4.0).
