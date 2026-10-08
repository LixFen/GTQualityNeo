# GTQualityNeo

面向 GregTech 6 NeoForge 移植版的 GTQuality 附属模组。

> [!WARNING]
> 由于GT6w正在高速开发中
> 本模组仅供参考，个人使用

## Jade 联动

- Jade 基础机器运行、空闲及停止状态。
- 配方进度条、加工中的实际物品与流体产物、完成后的配方产物。
- 物品存储，流体显示
- 熔炼坩埚、多方块坩埚的温度、熔毁温度和内部材料；模具的生产类型、进液方向及红石控制。
- 锅炉压力、效率、热量和蒸汽输出；固体、液体及流化床燃烧箱的燃料、灰烬、燃烧时间和输出。
- 蒸汽引擎、蒸汽涡轮、液体发动机的储能、输入、输出及燃料消耗。
- 反应堆燃料棒、剩余寿命、中子数、冷却液和估算热量；转轴、电线的实际传输量。
- 岩石内容物、流体泉种类与生成间隔、砧耐久及大小端、搅拌盆和筛选台输入输出、灌木生长与产物、焦炉运行状态。
- 储物桶只显示内容槽；四格抽屉显示对应分区内存储的物品。
- 采掘提示沿用 GT6 内置工具图标和等级，补充适用工具名称及 GT6 手持工具有效性、当前可采掘状态。

模组需要安装在服务端和客户端；Jade 也需要双方安装才能同步机器详情。

配置 `config/gtqualityneo-common.toml` 中的 `jadeIntegration=false` 可关闭本插件的全部 Jade 注册，重启生效；GT6 自带的 Jade 注册仍由 GT6 管理。

## QoL 功能

迁移前的替代实现、行为差异及取舍见 [QOL_MIGRATION.md](QOL_MIGRATION.md)。

- 手持凿子潜行右键空模具，分页选择形状或清空；上次选择优先显示。
- 手持选择器电路 Shift + 右键空气或方块，打开 0–24 号选择界面；
- 基础机器 GUI 的流体槽支持GUI内交互
- 创造流体储罐：无合成配方，通过创造物品栏或 JEI 获取；右键配置，可拖入 JEI 幻影流体、GT6 流体显示物品或点击光标流体容器选择，不消耗选择用的物品。空光标右键流体槽或点击 X 清空。
- 储罐速率为 `0–2147483647 L/tick`，默认 `1000`；`0` 停止输出。自动输出默认关闭，开启后推送六面邻接能力；注意：不连接管道也会推送流体。
- 按配置允许遮挡机器面交互、树脂袋侧面自动提取、GT6 全套耐热服火焰伤害免疫。全套通用防护服也具有同样的防火功能，不依赖护甲/耐久增强开关。
- 全套通用防护服提供 20 点护甲（10 格），实际护甲属性参与减伤；脱下一件恢复原有护甲。每件最大耐久由 128 提升至 512，已有损耗和组件保持不变；关闭增强后恢复原上限，损耗超过 128 的装备可能在下次受损时损坏。
- 凿子切换储物桶/存储器的材料形态，保留材料余量。
- 视角控制脚手架额外爬升/下降速度；移除远侧两根立柱碰撞。
- 未含水的闭合活板门可作为燃烧箱的进气口；
- 现代漏斗组合选取/碰撞形状，含 Cover；持扳手、活动扳手或螺丝刀时整块选取。小型火把/压力阀 Cover 使用实际合并边界。
- GT6 工具 GUI 连续耐久/充能双状态条；识别 DuraDisplay 时避让。
- JEI 高级工作台配方填充，支持 GT6 和原版合成分类，允许从工作台储存槽与玩家背包取材；不对工作台的第二个储存界面提供合成填充。多人游戏使用此功能需要服务端也安装 JEI。
- 物品及流体过滤器支持从 JEI 拖入幻影条件，不消耗物品或改变光标堆叠；流体过滤支持 GT6 流体显示物品、JEI 流体及流体容器。悬停过滤槽显示提示。
- JEI 的 GT6 资源生成查询覆盖岩层矿、岩层交界、深板岩、小型矿、基岩矿、旧式大型矿脉、矿团、Coltan、流体矿床及地表资源。矿石可查询配方和用途，宿主岩石、指示花及流体可查询关联页面；预览包含伴生矿与几率。形态自动轮换，Shift 暂停，悬停图标滚轮切换；预览网格和长说明可滚动，长群系列表可展开，顶部入口可查看所有页面。高度同时显示旧坐标配置和当前维度的 GT6 换算参考，次数按现代世界高度缩放。页面读取客户端规则，不扫描矿床；当前维度换算参考不代表该规则在此维度启用，具体看维度列表，服务端配置和实际地形也可能不同。


| 配置 | 文件类型 | 默认值 |
| --- | --- | --- |
| `jadeIntegration` | `gtqualityneo-common.toml` | `true`，注册开关需重启 |
| `gtmToolBars` | `gtqualityneo-client.toml` | `true` |
| `allowObstructedInteraction` | `gtqualityneo-server.toml` | `true` |
| `guiFluidInteraction` | 同上 | `true` |
| `allowSapBagHopperExtraction` | 同上 | `true` |
| `heatHazmatFireImmunity` | 同上 | `true` |
| `circuitSelectorGui` | 同上 | `true` |
| `universalHazmatEnhancement` | `gtqualityneo-common.toml` | `true`，两端保持一致，修改后重启 |
| `jeiWorldgenDisplay` | `gtqualityneo-client.toml` | `true`，重启生效 |
| `scaffoldClimbUpSpeed` | 同上 | `0.14`；检测到 `gaiablossom` 时默认 `0` |
| `scaffoldClimbDownSpeed` | 同上 | `0.15` |


## 构建

```powershell
cd D:\CodeReference\gregtech6_w
.\gradlew.bat jar
```

将其 `build/libs/gregtech6-6.0.0-alpha.12.jar` 放入本项目 `libs/`，然后运行：

```powershell
.\gradlew.bat build
```

也可直接指定依赖位置：

```powershell
.\gradlew.bat build '-PgregtechJar=D:/CodeReference/gregtech6_w/build/libs/gregtech6-6.0.0-alpha.12.jar'
```

如需单独指定开发运行时依赖：

```powershell
.\gradlew.bat build '-PgregtechRuntimeJar=libs/gregtech6-6.0.0-alpha.12.jar'
```

## 来源

- [GTQuality](https://github.com/LixFen/GTQuality) 的现有业务逻辑
- NeoForge MDK
