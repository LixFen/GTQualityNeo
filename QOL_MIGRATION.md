# GTQuality QoL 迁移

| 原功能 | 目标版替代或差异 | 迁移决策 |
| --- | --- | --- |
| WDMla HUD、库存、流体、采掘 | 已迁移 Jade；GT6 的基础 Jade 注册仍保留 | 复用已有实现 |
| NEI Recipe Catalyst | GT6_JEI_Plugin.registerRecipeCatalysts 已注册机器入口 | 复用 GT6，不重复注册 |
| NEI 高级工作台填充 | GT6 JEI 插件无 transfer handler；GT6 工作台有真实及配方模板槽 | 新增 JEI 填充，服务端处理，允许使用工作台储存槽 |
| 模具凿子界面 | 原有逐格雕刻仍在；无列表界面，模具状态为 protected | 新增潜行凿子选择界面；仅空模具、有效形状、权限及距离通过时更新 |
| 模具及储物桶提示 | 原版只提供基本 GT6 提示 | 新增中英文操作说明 |
| GUI 流体容器操作 | ContainerCommon.clicked 忽略 Slot_Holo；没有容器到流体槽的转换 | 注入现有菜单，避免替换菜单工厂；支持普通及 Shift 批量点击，输出槽只取液 |
| 创造储罐及 NEI 幻影选择 | 无等价可配置无限流体源；NeoForge 26.1 使用事务式 ResourceHandler | 新增现代注册的创造储罐；共享每 tick 输出预算；JEI 幻影选择；保存和掉落保留组件 |
| 遮挡交互 | CS.OBSTRUCTION_CHECKS 仍控制 WD.obstructed，默认开启 | 在 WD.obstructed 入口使用同步配置跳过检查；关闭选项时沿用 GT6，避免跨存档污染静态开关 |
| 树脂袋自动提取 | SapBag 的侧面槽仍为空，canExtractItem2 返回 false | 精确注入原两个方法，保留配置开关 |
| 储物形态切换 | 储物桶自动统一形态仍在，但无凿子循环切换 | 注入凿子操作，沿用形态顺序、容量限制、材料余量和缓存清理 |
| 脚手架移动 | GT6 仍标记为梯子；未提供视角加速 | 迁移视角/前进输入语义；仅本地受控客户端加速，避免服务端重复位移；服务端配置同步 |
| 脚手架立柱碰撞 | 原四根立柱仍存在，现代碰撞桥读取其 AABB 列表 | 迁移移除远侧两根立柱的精确规则 |
| 闭合活板门进气 | 三种燃烧箱仍要求 WD.hasCollide=false 和 WD.oxygen=true | 只替换燃烧箱检查，现代 TrapDoorBlock.OPEN 代替旧元数据位 |
| 漏斗选取与碰撞 | 模型轮廓 AABB 与现代 VoxelShape 有差异；旧 collisionRayTrace 不再是实际入口 | 在现代形状入口提供组合轮廓和 Cover；持扳手/螺丝刀仍可选整块 |
| 小型 Cover 选取范围 | shrunkBox 仍因任意 Cover 扩展到整块 | 只针对火把/压力阀 Cover 合并实际边界，其他 Cover 沿用 GT6 |
| 耐热服火焰免疫 | UT 的热量伤害防护不覆盖所有原版火伤 | 在可取消的伤害事件中对 is_fire 标签和 GT6 全套防护判定执行免疫 |
| GTM 工具状态条 | GT6 渲染器已有旧图标覆层，但没有连续双状态条 | 在现代 GUI 装饰层绘制耐久/电量，跳过旧覆层；保留开关及 DuraDisplay 避让 |
| 多方块坩埚顶部纹理 | 旧 IIcon UV 裁剪不存在；GT6QuadBuilder.putFace/corners 已按实际墙体几何边界取 UV | 复用现代渲染器，不再额外裁剪 |
| Angelica Unicode 修复 | 旧 FontRenderer/Angelica API 不存在；现代字体渲染不走该调用 | 不移植已失去适用对象的补丁 |

新代码只写入 GTQualityNeo，不修改 GT6 参考工程。客户端界面与渲染代码独立于服务端注册。迁移完成情况及验证记录在 README 中更新；本表记录迁移前的决定，不代替验收结果。

## 实现时确认的差异

- GT6 的 `OreDictMaterialStack.load` 对缺少材料标签的模具也返回对象，导致空模具被当作装有材料。插件只在缺少/空材料标签时恢复原来的 `null` 语义；真实材料不改动。
- GT6 的 `FluidTankGT.asResourceHandler` 将实际数量压到 `int`。GUI 转移用现代事务处理临时容器槽，成功后才修改 GT6 原生 `long` 槽，避免截断既有库存。
- NeoForge 的 `ItemAccess.forStack` 不能替换底层 Item，所以桶需要使用可替换物品的临时槽位。这里保留模拟/失败不改变原容器和槽的语义。
- 原 1.7.10 没有含水方块。现代含水活板门不作为进气口；闭合且未含水的活板门仍按原功能允许进气。
- 物品形态切换保留现代数据组件，例如自定义名称，但恢复目标形态自己的 GT6 subtype 和身份数据，防止变形后出现错误材料或物品。
- 模具选择包校验主手凿子、空模具、合法形状、已加载区块、交互距离及世界权限；创造储罐配置包校验当前菜单、窗口编号、距离和权限。服务端执行操作，客户端只发送请求。
- 创造储罐保留全部原操作及速率语义，改用标准现代方块、组件和菜单注册；目前外观为紫色方块，界面采用现代按钮和物品槽。没有合成配方。JEI 和 Jade 均可移除，核心 QoL 不依赖它们。
- 保留原 GaiaTweaks 模组 ID `gaiablossom` 的脚手架默认值避让，以及 DuraDisplay 的工具条避让。当前测试环境均未安装这些模组；没有据此声称兼容其未测试的现代移植版本。
