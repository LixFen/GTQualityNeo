# GTQuality QoL 迁移

| 原功能 | 目标版替代或差异 | 做法 |
| --- | --- | --- |
| WDMla HUD、库存、流体、采掘 | 已迁移 Jade；GT6 的基础 Jade 注册仍保留 | 复用已有实现 |
| NEI Recipe Catalyst | GT6_JEI_Plugin.registerRecipeCatalysts 已注册机器入口 | 复用 GT6，不重复注册 |
| NEI 高级工作台填充 | GT6 JEI 插件无 transfer handler；GT6 工作台有真实及配方模板槽 | 新增 JEI 填充，服务端处理，允许使用工作台储存槽 |
| 模具凿子界面 | 原有逐格雕刻仍在；无列表界面，模具状态为 protected | 通过完整 NBT 持久化 API 更新形状；仅空模具、有效形状、权限及距离通过时更新 |
| 模具及储物桶提示 | 原版只提供基本 GT6 提示 | 新增中英文操作说明 |
| GUI 流体容器操作 | ContainerCommon.clicked 忽略 Slot_Holo；没有容器到流体槽的转换 | 客户端界面事件发送请求，服务端校验当前菜单、槽位及权限；支持普通及 Shift 批量点击，输出槽只取液 |
| 创造储罐及 NEI 幻影选择 | 无等价可配置无限流体源；NeoForge 26.1 使用事务式 ResourceHandler | 新增现代注册的创造储罐；共享每 tick 输出预算；JEI 幻影选择；保存和掉落保留组件 |
| 遮挡交互 | CS.OBSTRUCTION_CHECKS 仍控制 WD.obstructed，默认开启 | 通过配置及服务端/客户端生命周期事件临时修改公开开关；关闭选项、卸载配置及退出时恢复原值 |
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
