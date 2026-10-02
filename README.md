# GTQualityNeo

面向 GregTech 6 NeoForge 移植版的 GTQuality 附属模组。


> [!WARNING]
> 由于GT6w正在高速开发中
> 本模组仅供参考，个人使用

## Jade 联动

- Jade 基础机器运行、空闲及停止状态。
- 配方进度条、加工中的实际物品与流体产物、完成后的配方产物。
- 机器库存（过滤 GT6 流体显示物品），替换同一机器的 Jade 通用库存段以免重复。
- 熔炼坩埚、多方块坩埚的温度、熔毁温度和材料组成；模具的生产类型、进液方向及红石控制。
- 锅炉压力、效率、热量和蒸汽输出；固体、液体及流化床燃烧箱的燃料、灰烬、燃烧时间和输出。
- 蒸汽引擎、蒸汽涡轮、液体发动机的储能、输入、输出及燃料消耗。
- 反应堆燃料棒、剩余寿命、中子数、冷却液和估算热量；转轴、电线的实际传输量。
- 岩石内容物、流体泉种类与生成间隔、砧耐久及大小端、搅拌盆和筛选台输入输出、灌木生长与产物、焦炉运行状态。
- 储物桶只显示内容槽；箱子聚合同类物品；四格抽屉按正面命中区域显示对应分区内存储的物品。
- 流体储存使用 GT6 原生槽位与 `long` 数量/容量，包含小型容器，避免现代能力视图的整数截断。
- 通过多方块部件查看各类控制器；服务端解析控制器并传回类型与数据，客户端不必加载其区块。
- 采掘提示沿用 GT6 内置工具图标和等级，补充适用工具名称及 GT6 手持工具有效性、当前可采掘状态。
- 中文和英文翻译；Jade 设置分别控制机器详情、特殊设备详情和采掘详情。

数据使用 Jade 的注册表感知网络编码，保留物品和流体的数据组件。模组需要安装在服务端和客户端；Jade 也需要双方安装才能同步机器详情。

配置 `config/gtqualityneo-common.toml` 中的 `jadeIntegration=false` 可关闭本插件的全部 Jade 注册，重启生效；GT6 自带的 Jade 注册仍由 GT6 管理。

## QoL 功能

迁移前的替代实现、行为差异及取舍见 [QOL_MIGRATION.md](QOL_MIGRATION.md)。

- 手持凿子潜行右键空模具，分页选择形状或清空；上次选择优先显示。
- 基础机器 GUI 的流体槽支持光标容器灌注和取液，Shift 批量处理；输出槽只允许取液。失败不消耗容器或流体，GT6 大数量槽不经整数截断。
- 创造流体储罐：无合成配方，通过创造物品栏或 JEI 获取；右键配置，可拖入 JEI 幻影流体、GT6 流体显示物品或点击光标流体容器选择，不消耗选择用的物品。空光标右键流体槽或点击 X 清空。
- 储罐速率为 `0–2147483647 L/tick`，默认 `1000`；`0` 停止输出。自动输出默认关闭，开启后推送六面邻接能力；自动推送和管道抽取共用每 tick 的总预算。流体组件、速率及开关随存档和方块掉落保留。
- 按配置允许遮挡机器面交互、树脂袋侧面自动提取、GT6 全套耐热服火焰伤害免疫。
- 凿子切换储物桶/存储器的材料形态，保留材料余量、容量限制和自定义名称等现代组件。
- 视角控制脚手架额外爬升/下降，保留潜行停止语义；移除远侧两根立柱碰撞。
- 未含水的闭合活板门可作为三类燃烧箱的进气口；现代含水活板门不允许进气。
- 现代漏斗组合选取/碰撞形状，含 Cover；持扳手、活动扳手或螺丝刀时整块选取。小型火把/压力阀 Cover 使用实际合并边界。
- GT6 工具 GUI 连续耐久/充能双状态条；识别 DuraDisplay 时避让。
- JEI 高级工作台配方填充，支持 GT6 和原版合成分类，允许从工作台储存槽与玩家背包取材；不对工作台的第二个储存界面提供合成填充。

配置文件由 NeoForge 生成：

| 配置 | 文件类型 | 默认值 |
| --- | --- | --- |
| `jadeIntegration` | `gtqualityneo-common.toml` | `true`，注册开关需重启 |
| `gtmToolBars` | `gtqualityneo-client.toml` | `true` |
| `allowObstructedInteraction` | `gtqualityneo-server.toml` | `true` |
| `guiFluidInteraction` | 同上 | `true` |
| `allowSapBagHopperExtraction` | 同上 | `true` |
| `heatHazmatFireImmunity` | 同上 | `true` |
| `scaffoldClimbUpSpeed` | 同上 | `0.14`；检测到 `gaiablossom` 时默认 `0` |
| `scaffoldClimbDownSpeed` | 同上 | `0.15` |

服务端配置同步到客户端；速度范围为 `0–2`，设为 `0` 可关闭对应额外移动。关闭遮挡选项时重新沿用 GT6 自身的判断，不修改其全局静态开关。

## 构建

先设置 `JAVA_HOME` 指向 Java 25。构建 GregTech 参考项目：

```powershell
cd D:\CodeReference\gregtech6_w
.\gradlew.bat jar
```

将其 `build/libs/gregtech6-6.0.0-alpha.11.jar` 放入本项目 `libs/`，然后运行：

```powershell
.\gradlew.bat build
```

也可直接指定依赖位置：

```powershell
.\gradlew.bat build '-PgregtechJar=D:/CodeReference/gregtech6_w/build/libs/gregtech6-6.0.0-alpha.11.jar'
```

`runClient` / `runServer` 使用同一 GregTech jar，并由 Gradle 获取 Jade。GregTech 与 Jade 不会打包进 GTQualityNeo。

客户端使用 `run/`，独立服务器使用 `run/server/`，避免同时运行时争用日志和配置文件。

Windows 的 `gradlew.bat` 自动将 Unix-domain socket 临时目录设置为项目内的 `.gradle/socket-tmp`，避免 `Unable to establish loopback connection`。该设置只影响本次启动的进程及其子进程，不修改全局环境变量。

## 来源

- [GTQuality](https://github.com/LixFen/GTQuality) 的现有业务逻辑
- NeoForge MDK