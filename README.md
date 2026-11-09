# GTQualityNeo

**English** | [简体中文](README_zh.md)

GTQualityNeo is a quality-of-life add-on for the NeoForge port of GregTech 6.

> [!WARNING]
> GregTech 6 for NeoForge is under development. Compatibility can change as the port evolves.

[Features](#features) · [Configuration](#configuration) · [Build](#build-from-source)

## Features

### Jade integration

GTQualityNeo adds details for GregTech machines and blocks:

- **Machines and storage:** machine state, recipe progress, stored items and fluids.
- **Processing and power:** crucible temperature and materials, boiler and combustion data, engine and turbine information.
- **Reactors and world blocks:** reactor fuel and estimated heat, plus selected world-generation and processing details.
- **Mining hints:** GT6 tool names and mining-state information.

Jade integration can be disabled with `jadeIntegration = false`;

### LDLib2 interfaces (experimental)

The client requires LDLib2 `26.1.2.43` or newer. Most UI will be Modern style.

### JEI integration

- Merge selected recipe displays and crafting-table recipes.
- Use JEI indexing for machine recipes.
- Drag ghost items and fluids from JEI into filters.
- Preview multiblock structures and query GregTech resource generation.
- Display fluid and ore-dictionary recipe variants.

These features can be configured individually.

> **Recipe indexing:** `jeiRecipeQueryOptimization` (default: `true`) prepares recipes for JEI indexing to speed up queries. It may slightly increase world startup time.

### REI integration

- Added the REI Compatibility, with all JEI functions resserved. 

If you don't like JEI, install this mod and you can use REI instead now. (If you choose REI, you can have much faster world loading performance)

### World loading performance

Installed this mod, the world loading time can be reduced by approximately 40%. (Measured at `jeiRecipeQueryOptimization` is true)

### Quality-of-life changes

- Select mold shapes with a chisel and select circuit values with a selector circuit.
- Use configurable range modes with mining drills.
- Configure one-way fluid pipe connections and interact with fluid containers in machine GUIs.
- Add a creative fluid tank with a vanilla-style LDLib2 interface and enable interaction with machine faces that are obstructed.
- Allow hopper extraction from resin bags; provide fire immunity with a complete GT6 heat-hazmat suit and improve protection from a complete universal hazmat suit.
- Switch mass-storage items between material forms with a chisel.
- Adjust scaffold climbing speed and scaffold collision, and update hopper selection and collision shapes.
- Allow closed, dry trapdoors to provide air to combustion boxes.
- Display modern GT6 tool durability and energy bars.
- Show GT6 loading stages and progress in the upper-left corner while entering a world; hide the indicator when loading finishes.
- And more...

All of these changes are configurable.

### LDLib2 interface (experimental)

The client requires LDLib2 `26.1.2.43` or a later `26.1.x` version and uses Modern Modular UI for most GT6 GUIs.

### Optional mod integrations

- **AE2:** adds an ME powder unifying storage cell for mass-storage forms; the cell supports advanced settings in the cell workbench.
- **FTB Quests:** only for GT6W items in the default `match_components=none` mode, task matching uses item plus subtype and ignores other component differences. Explicit `fuzzy` and `strict` modes retain FTB Quests' normal matching behavior.

### Resource scanners

MV, HV, EV, and IV resource scanners support GT6 EU charging.

## Configuration

Config files are generated under the instance's `config/` directory. Most features are enabled by default and can be switched off individually.

### `gtqualityneo-common.toml`

- `jadeIntegration`
- `universalHazmatEnhancement`

### `gtqualityneo-client.toml`

- `gtmToolBars`
- `worldLoadingProgress`
- `jeiWorldgenDisplay`
- `jeiMultiblockPreview`
- `hideInvisibleMaterials`
- `jeiFluidVariants`
- `jeiOreDictVariants`
- `jeiMergeCraftingTable`
- `scaffoldClimbUpSpeed`
- `scaffoldClimbDownSpeed`

### `gtqualityneo-server.toml`

- `allowObstructedInteraction`
- `guiFluidInteraction`
- `drillRangeModes`
- `oneWayFluidPipes`
- `allowSapBagHopperExtraction`
- `heatHazmatFireImmunity`
- `circuitSelectorGui`
- `moldShapeSelector`
- `massStorageFormSwitching`
- `creativeTank`
- `scaffoldCollisionChanges`
- `hopperShapeChanges`
- `jeiFilterGhostIngredients`

The default scaffold climb speeds are `0.14` up and `0.15` down.

## Build from source

Use a JDK 25 toolchain. The Gradle build expects the GregTech jar at `libs/gregtech6-6.0.0-alpha.18.jar` by default. Place the jar there before building.

### Build

```powershell
.\gradlew.bat build
```

### Custom GregTech dependency

Alternatively, pass an explicit GregTech jar path:

```powershell
.\gradlew.bat build '-PgregtechJar=D:/path/to/gregtech6-6.0.0-alpha.18.jar'
```

### Development client

To use a separate GregTech jar for the development runtime:

```powershell
.\gradlew.bat runClient '-PgregtechRuntimeJar=libs/gregtech6-6.0.0-alpha.18.jar'
```
