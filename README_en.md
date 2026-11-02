# GTQualityNeo

GTQualityNeo is a quality-of-life add-on for the NeoForge port of GregTech 6. It adds gameplay and interface improvements, uses LDLib2 for client GUIs, and provides optional integrations with Jade, JEI, Applied Energistics 2 (AE2), and FTB Quests.

> **Development status:** GregTech 6 for NeoForge is under active development. This project is intended for reference and personal use; compatibility can change as the port evolves.

## Features

### Jade integration

When Jade is installed, GTQualityNeo adds details for GregTech machines and blocks, including machine state and recipe progress, stored items and fluids, crucible temperature and materials, boiler and combustion data, engine and turbine information, reactor fuel and estimated heat, and details for selected world-generation and processing blocks. It also adds GT6 tool names and mining-state information to mining hints.

Install GTQualityNeo and Jade on both the client and server for synchronized machine details. Jade integration can be disabled with `jadeIntegration = false` in `config/gtqualityneo-common.toml`; restart the game for the registration change to take effect. GregTech's own Jade registrations are controlled by GregTech.

### JEI integration

With JEI installed, the mod merges selected recipe displays, improves machine recipe lookup, supports filter ghost items\fluids from JEI, adds multiblock structure previews, and provides GregTech resource-generation queries. It also supports fluid and ore-dictionary recipe variants and merged crafting-table recipes. These features can be configured individually.

GTQualityNeo owns machine category registration, pages, lookup plugins and machine catalysts, while retaining GT6 category IDs and recipe data. GT6 still answers each lookup (focus expansion, on-demand generation, matching, and the custom hammer/crucible queries); GTQualityNeo only narrows its full recipe scan to input/output index candidates and skips repeated template generation. Existing workbench, smoking and blasting integrations keep their original paths. Opening an entire category still requires generating its complete recipe list.

JEI code is organized into `machine`, `cache`, `compat` and `crafting` packages. Compatibility adapters carry a warning at the top: recheck registration hooks, matching rules and recipe lifecycle hooks after upgrading GT6w.

### Quality-of-life changes

- Select mold shapes with a chisel and select circuit values with a selector circuit.
- Use configurable range modes with mining drills.
- Configure one-way fluid pipe connections and interact with fluid containers in machine GUIs.
- Add a creative fluid tank with a vanilla-style LDLib2 interface and enable interaction with machine faces that are obstructed.
- Allow hopper extraction from resin bags; provide fire immunity with a complete GT6 heat-hazmat suit and improve protection from a complete universal hazmat suit.
- Switch mass-storage items between material forms with a chisel.
- Adjust scaffold climbing speed and scaffold collision, and update hopper selection and collision shapes.
- Allow closed, dry trapdoors to provide air to combustion boxes.
- Display GT6 tool durability and energy bars.
- Show GT6 loading stages and progress in the upper-left corner while entering a world; hide the indicator when loading finishes.

### LDLib2 interface (experimental)

The client requires LDLib2 `26.1.2.43` or a later `26.1.x` version and uses Modern Modular UI for most GT6 GUIs.

### Optional mod integrations

- **AE2:** adds a ME powder consolidation storage cell for mass-storage forms; the cell supports advanced settings in the cell workbench.
- **FTB Quests:** for GT6W items in the default `match_components=none` mode, task matching uses item plus subtype and ignores other component differences. Explicit `fuzzy` and `strict` modes retain FTB Quests' normal matching behavior. Non-GT6W items and filter tasks keep their normal behavior.

### Resource scanners

Includes the GT6 Scanner Modern functionality: MV/HV/EV/IV scanners with GT6 EU charging and tiered crafting recipes, available in the Tools and GTQualityNeo creative tabs.

## Configuration

Config files are generated under the instance's `config/` directory. Most features are enabled by default and can be switched off individually.

| File | Settings |
| --- | --- |
| `gtqualityneo-common.toml` | `jadeIntegration`, `universalHazmatEnhancement` |
| `gtqualityneo-client.toml` | `gtmToolBars`, `jeiWorldgenDisplay`, `jeiMultiblockPreview`, `jeiFluidVariants`, `jeiOreDictVariants`, `jeiMergeCraftingTable`, `jeiMachineRecipeLookup`, `scaffoldClimbUpSpeed`, `scaffoldClimbDownSpeed` |
| `gtqualityneo-server.toml` | `allowObstructedInteraction`, `guiFluidInteraction`, `drillRangeModes`, `oneWayFluidPipes`, `allowSapBagHopperExtraction`, `heatHazmatFireImmunity`, `circuitSelectorGui`, `moldShapeSelector`, `massStorageFormSwitching`, `creativeTank`, `scaffoldCollisionChanges`, `hopperShapeChanges`, `jeiFilterGhostIngredients` |

The default scaffold climb speeds are `0.14` up and `0.15` down. The Jade registration switch requires a restart to take effect.

## Requirements and compatibility

The project targets Minecraft `26.1.2`, NeoForge `26.1.2.109`, and GregTech 6 NeoForge `6.0.0-alpha.17`. GregTech 6 and NeoForge are required, and LDLib2 is required on the client. Jade, JEI, AE2, and FTB Quests are optional integrations. The project metadata declares the minimum integration versions; see `src/main/templates/META-INF/neoforge.mods.toml` for version ranges.

## Build from source

Use a JDK 25 toolchain. The Gradle build expects the GregTech jar at `libs/gregtech6-6.0.0-alpha.17.jar` by default. Place the jar there, then run:

```powershell
.\gradlew.bat build
```

Alternatively, pass an explicit GregTech jar path:

```powershell
.\gradlew.bat build '-PgregtechJar=D:/path/to/gregtech6-6.0.0-alpha.17.jar'
```

To use a separate GregTech jar for the development runtime:

```powershell
.\gradlew.bat runClient '-PgregtechRuntimeJar=libs/gregtech6-6.0.0-alpha.17.jar'
```

The Gradle project also provides `runServer`, `runGameTestServer`, and `runData` development tasks.

## Sources

- Existing business logic from [GTQuality](https://github.com/LixFen/GTQuality)
- [NeoForge MDK](https://github.com/NeoForgeMDKs/MDK-1.21-ModDevGradle)
