# GTQualityNeo

面向 GregTech 6 NeoForge 移植版的 GTQuality 附属模组。

基于 [NeoForge 官方 26.1.2 ModDevGradle 模板](https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle)。

| 依赖 | 当前目标 |
| --- | --- |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.109 |
| Java | 25 |
| GregTech 6 NeoForge | 6.0.0-alpha.11 |
| Jade（可选） | 26.1.8+neoforge |

## Jade 联动

- Jade 基础机器运行、空闲及停止状态。
- 配方进度条、加工中的实际物品与流体产物、完成后的配方产物。
- 机器库存（过滤 GT6 流体显示物品），替换同一机器的 Jade 通用库存段以免重复。
- 熔炼坩埚、多方块坩埚的温度、熔毁温度和材料组成；模具的生产类型、进液方向及红石控制。
- 锅炉压力、效率、热量和蒸汽输出；固体、液体及流化床燃烧箱的燃料、灰烬、燃烧时间和输出。
- 蒸汽引擎、蒸汽涡轮、液体发动机的储能、输入、输出及燃料消耗。
- 反应堆燃料棒、剩余寿命、中子数、冷却液和估算热量；转轴、电线的实际传输量。
- 岩石内容物、流体泉种类与生成间隔、砧耐久及大小端、搅拌盆和筛选台输入输出、灌木生长与产物、焦炉运行状态。
- 储物桶只显示内容槽；箱子聚合同类物品；四格抽屉按正面命中区域显示对应分区。
- 流体储存使用 GT6 原生槽位与 `long` 数量/容量，包含小型容器，避免现代能力视图的整数截断。
- 通过多方块部件查看各类控制器；服务端解析控制器并传回类型与数据，客户端不必加载其区块。
- 采掘提示沿用 GT6 内置工具图标和等级，补充适用工具名称及 GT6 手持工具有效性、当前可采掘状态。
- 中文和英文翻译；Jade 设置分别控制机器详情、特殊设备详情和采掘详情。

数据使用 Jade 的注册表感知网络编码，保留物品和流体的数据组件。模组需要安装在服务端和客户端；Jade 也需要双方安装才能同步机器详情。

配置 `config/gtqualityneo-common.toml` 中的 `jadeIntegration=false` 可关闭本插件的全部 Jade 注册，重启生效；GT6 自带的 Jade 注册仍由 GT6 管理。

显示内容对应 GTQuality 当前的 WDMla 联动；Jade 布局和配置界面沿用 Jade 自身的机制。

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

## 后续迁移范围

WDMla 联动迁移已完成代码实现。GTQuality 的模具交互界面、GUI 流体交互、创造储罐、碰撞修正、工具状态条和 JEI 工作台填充不属于本次 HUD 迁移，尚未实现。

## 验收

已通过 `gradlew build`、9 项自动化测试及独立服务器加载检查：GTQualityNeo Jade 插件成功注册，服务器到达 `Done` 状态。

客户端启动检查通过：新增 Jade 注册成功，资源重载完成，未出现插件加载异常。另已验证移除开发运行环境中的 Jade 后，独立服务器仍可加载 GTQualityNeo 并到达 `Done` 状态。

测试覆盖：库存聚合不修改原物品、不同数据组件不合并、整数溢出、储物桶槽位筛选、四格抽屉区域与边界、模具温度/方向/红石模式、多方块控制器快照、无控制器部件，以及超过 `int` 范围的流体数量和容量。测试通过 NeoForge 的临时服务器加载数据组件后执行。

运行日志中 Jade 26.1.8 的 `LootTableMineableCollector` 报 `Registry minecraft:loot_table not found`，未阻止启动；尚未验证它对采掘提示的影响。

仍需要在真实客户端和独立服务器中验证：无配方、加工、断能、手动停机、产物堵塞、完成、保存重载、多方块部件、包含数据组件的产物，以及禁用或移除 Jade。客户端显示效果尚未完成游戏内验收。

## 来源

- [GTQuality](https://github.com/LixFen/GTQuality) 的现有 WDMla 业务逻辑，MIT；版权文本见 LICENSE。
- NeoForge MDK 的模板许可见 TEMPLATE_LICENSE.txt。
- GregTech 与 Jade 作为外部依赖，不复制其实现或编译镜像。
