# GTQualityNeo

面向 GregTech 6 NeoForge 移植版的 GTQuality 附属模组。

> [!WARNING] 由于GT6w正在高速开发中，本模组仅供参考，个人使用

## Jade 联动

- Jade 基础机器运行、空闲及停止状态。
- 配方进度条、加工中的实际物品与流体产物、完成后的配方产物。
- 物品存储，流体显示
- 坩埚的温度、熔毁温度和内部材料；模具的生产类型、进液方向及红石控制。
- 锅炉压力、效率、热量和蒸汽输出；固体、液体及流化床燃烧箱的燃料、灰烬、燃烧时间和输出。
- 蒸汽引擎、蒸汽涡轮、液体发动机的储能、输入、输出及燃料消耗。
- 反应堆燃料棒、剩余寿命、中子数、冷却液和估算热量；转轴、电线的实际传输量。
- 岩石内容物、流体泉种类与生成间隔、砧耐久及大小端、搅拌盆和筛选台输入输出、灌木生长与产物、焦炉运行状态。
- 储物桶只显示内容槽；四格抽屉显示对应分区内存储的物品。
- 采掘提示沿用 GT6 内置工具图标和等级，补充适用工具名称及 GT6 手持工具有效性、当前可采掘状态。

模组需要安装在服务端和客户端；Jade 也需要双方安装才能同步机器详情。

`jadeIntegration=false` 可关闭本插件的全部 Jade 注册

## LDLib2 界面（试验）

客户端必须安装 LDLib2 `26.1.2.43` 或更高，GT6 大部分 GUI 将使用 Modern Modular UI。

## JEI 兼容

- 按类型合并了大量配方
- 使用 JEI 原生索引查询机器配方
- `jeiRecipeQueryOptimization`：优化配方查询效率，默认开启。关闭后恢复 GT6 原生分类和按需查询；机器变体合并、工作台分类合并随之停用。修改后重启客户端。
- 接管了配方注册
- 机器配方批量生成时合并配置保存，每份配置在该配方表生成结束后写入一次。

## QoL 功能

- GT6 世界加载期间，左上角显示当前加载内容和阶段进度，完成后自动隐藏。可在 `config/gtqualityneo-client.toml` 中设置 `worldLoadingProgress = false` 关闭。
- 添加了资源扫描仪
- 手持凿子潜行右键模具选择形状
- 手持选择器电路 Shift + 右键空气选号
- 采矿电钻范围采掘
- 允许流体管道单向传递
- 机器 GUI 的流体槽支持 GUI 内交互
- 创造流体储罐（LDLib2 原版风格界面）
- 按配置允许遮挡机器面交互
- 树脂袋允许自动化
- GT6 全套耐热服火焰伤害免疫
- 全套通用防护服提供 20 点护甲
- 凿子切换储物桶/存储器的材料形态
- 视角控制脚手架额外爬升/下降速度；移除远侧两根立柱碰撞。
- 未含水的闭合活板门可作为燃烧箱的进气口；
- 现代漏斗选取/碰撞形状箱
- 更现代的能量条，电池显示能量
- JEI 工作台页面合并
- 物品及流体过滤器支持从 JEI 拖入幻影条件
- JEI 多方块结构预览
- JEI 的 GT6 资源生成查询

与独立模组 GT6 Scanner Modern（`gt6scan`）不兼容，使用前需在客户端和服务端移除原模组。旧版四级扫描仪会在读取存档时自动转换为 GTQualityNeo 对应版本，保留 EU 电量、自定义名称和其他组件，保存后使用新的物品 ID。


| 配置 | 文件类型 | 默认值 |
| --- | --- | --- |
| `jadeIntegration` | `gtqualityneo-common.toml` | `true`，注册开关需重启 |
| `gtmToolBars` | `gtqualityneo-client.toml` | `true` |
| `worldLoadingProgress` | `gtqualityneo-client.toml` | `true` |
| `allowObstructedInteraction` | `gtqualityneo-server.toml` | `true` |
| `guiFluidInteraction` | 同上 | `true` |
| `drillRangeModes` | 同上 | `true` |
| `oneWayFluidPipes` | 同上 | `true` |
| `allowSapBagHopperExtraction` | 同上 | `true` |
| `heatHazmatFireImmunity` | 同上 | `true` |
| `circuitSelectorGui` | 同上 | `true` |
| `moldShapeSelector` | 同上 | `true` |
| `massStorageFormSwitching` | 同上 | `true` |
| `creativeTank` | 同上 | `true` |
| `scaffoldCollisionChanges` | 同上 | `true` |
| `hopperShapeChanges` | 同上 | `true` |
| `jeiFilterGhostIngredients` | 同上 | `true` |
| `universalHazmatEnhancement` | `gtqualityneo-common.toml` | `true` |
| `jeiWorldgenDisplay` | `gtqualityneo-client.toml` | `true` |
| `jeiMultiblockPreview` | `gtqualityneo-client.toml` | `true` |
| `jeiFluidVariants` | 同上 | `true` |
| `jeiOreDictVariants` | 同上 | `true` |
| `jeiMergeCraftingTable` | 同上 | `true` |
| `scaffoldClimbUpSpeed` | 同上 | `0.14` |
| `scaffoldClimbDownSpeed` | 同上 | `0.15` |


## AE2 联动

新增 **ME 粉末归并存储元件**

- 储物桶/存储器的元件形态，可在元件工作台高级配置

## FTB Quests 联动

安装 FTB Quests 后，GT6W 物品任务在默认的 `match_components=none` 模式下匹配 **Item + subtype**，忽略其他组件差异

任务手动选择 `fuzzy` 或 `strict` 时，保留 FTB Quests 原有组件匹配规则；非 GT6W 物品和过滤器任务也保持原有行为。已有 `strict` 任务若需要放宽匹配，应改为 `none`。

## 构建

开发环境默认使用 [GT6 alpha.17](https://github.com/wolfram0108/gregtech6_w/releases/tag/v6.0.0-alpha.17)。将 `gregtech6-6.0.0-alpha.17.jar` 放入本项目 `libs/`，然后运行：

```powershell
.\gradlew.bat build
```

也可直接指定依赖位置：

```powershell
.\gradlew.bat build '-PgregtechJar=D:/CodeReference/gregtech6_w/build/libs/gregtech6-6.0.0-alpha.17.jar'
```

如需单独指定开发运行时依赖：

```powershell
.\gradlew.bat runClient '-PgregtechRuntimeJar=libs/gregtech6-6.0.0-alpha.17.jar'
```

## 来源

- [GTQuality](https://github.com/LixFen/GTQuality) 的现有业务逻辑
- `D:\CodeReference\gt6scan-modern` 的扫描仪实现与资源，MIT 许可证见 `GT6SCAN_LICENSE.txt`
- NeoForge MDK
