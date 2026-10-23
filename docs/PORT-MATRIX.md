# 43 项功能缺口 · 逐项择优判定表

> 本表是「阶段 3 · 父文件嫁接」的施工依据。判定原则：**只做加法**，不改 M 既有语句的语义；
> 凡 M 已有等效或更强实现，一律复用 M，不引入 GR 的旧实现（避免两套逻辑打架）。

## 判定口径

| 标记 | 含义 |
|------|------|
| **移植** | GR 有、M 无，且可纯新增接入，不触碰 M 既有调用链 |
| **复用** | M 已有等效/更强实现，GR 此项不移植（可能仅补一个入口开关） |
| **废弃** | GR 自身已注释/已下线，M 无对应机制，不移植 |
| **增强** | 移植到 M 既有方法上，但默认值取「保持 M 现状」，行为零变化 |

累计：GR 独有开关 51 项 → 移植 22 · 增强 1 · 复用 22 · 废弃 6

---

## A. BaseModel（基础配置）— 9 项

| # | GR 开关 | GR 中文名 | 判定 | 依据 |
|---|---------|-----------|------|------|
| 1 | `newRpc` | 使用新接口(最低支持v10.3.96.8100) | **复用** | M 的 `AppConfig.newRpc` 已存在；且 M 全量走新接口，无旧接口分支 |
| 2 | `showToast` | 气泡提示 | **复用** | M 的 `AppConfig.showToast` |
| 3 | `toastOffsetY` | 气泡纵向偏移 | **复用** | M 的 `AppConfig.toastOffsetY` |
| 4 | `batteryPerm` | 为支付宝申请后台运行权限 | **复用** | M 的 `AppConfig.batteryPerm` + `PermissionUtil.checkOrRequestBatteryPermissions` |
| 5 | `enableOnGoing` | 开启状态栏禁删 | **复用** | M 的 `AppConfig.enableOnGoing`，`NotificationUtil` 已 `setOngoing(true)` |
| 6 | `recordLog` | 记录日志 | **复用** | M 用 `AppConfig.enableForestLog/enableGoldenBeansLog/enableFarmLog/enableOtherLog` 四开关细分，粒度更细 |
| 7 | `debugMode` | 开启抓包(含小程序数据) | **复用** | M 的 `AppConfig.enableDebugLog` + `ApplicationHook` / `MiniProgramHook` 抓包链路 |
| 8 | `backupLogDays` | 保留之前日志天数(除runtime) | **增强** | M 的 `FileUtil.clearLog()` 当前只保留当天日志，全删历史；GR 可按天保留。**默认值取 0 = 保持 M 现状** |
| 9 | `closeCaptchaDialogVPN` | 关闭请检查是否使用了代理软件或VPN | **废弃** | GR 源码整行注释，功能未启用 |

## B. AntMember（会员 / 芝麻 / 商家）— 14 项

| # | GR 开关 | GR 中文名 | 判定 | 依据 |
|---|---------|-----------|------|------|
| 10 | `merchantSignIn` | 商家服务 \| 签到 | **移植** | 阶段 2 已建 `MerchantService.taskListQueryV2()`，仅缺开关与分发 |
| 11 | `merchantKMDK` | 商家服务 \| 开门打卡 | **移植** | 阶段 2 已建 `MerchantService.merchantKMDK()` |
| 12 | `signinCalendar` | 消费金 \| 签到 | **移植** | M 全仓 0 命中；需补 `signinCalendar()` + `openBoxAward()` 两个 RPC |
| 13 | `pointTreasureBox` | 芝麻积分 \| 宝箱 | **移植** | 阶段 2 已建 `SesameAlchemy.pointTreasureBox()` |
| 14 | `sesameAlchemy` | 芝麻炼金 \| 消耗芝麻粒炼金 | **移植** | 阶段 2 已建 `SesameAlchemy.run(doAlchemy, …)` |
| 15 | `sesameAlchemySign` | 芝麻炼金 \| 签到 | **移植** | 阶段 2 已建 `SesameAlchemy.alchemySign()` |
| 16 | `sesameAlchemyGrainTask` | 芝麻粒 \| 芝麻炼金任务 | **移植** | 独立的攒粒任务跑批（join→feedBack→push），M 的 `collectSesame` 只做领取不跑任务 |
| 17 | `sesameAlchemyGoldenBean` | 炼金金豆 \| 任务签到领奖 | **复用** | M 的 `goldenbeans` 模型 `GoldenBeansEntry.ALCHEMY`(ZHIMA) 已覆盖签到+任务+领奖 |
| 18 | `sesameAlchemyBeanExchange` | 炼金金豆 \| 芝麻粒换金豆 | **复用** | M 的 `goldenBeansAutoSesameExchange` → `GoldenBeansExchange.exchangeSesame` |
| 19 | `sesameAlchemyBeanLimit` | 炼金金豆 \| 当日兑换芝麻粒上限 | **复用** | M 的 `goldenBeansSesameExchangeLimit` |
| 20 | `sesameAlchemyReserveZml` | 炼金金豆 \| 剩余芝麻粒下限 | **复用** | M 用「单日上限」而非「余额下限」做保护，语义等价、M 方案更保守 |
| 21 | `enableGoldTicket` | 黄金票 \| 签到 | **复用** | M 有 `enableGoldTicketTask/Sign/Consume/Browse` 四开关，是 GR 单项的超集 |
| 22 | `antInsurance` | 蚂蚁保 \| 开启 | **废弃** | GR 字段与调用点均注释；`AntInsurance` 类无实际执行入口 |
| 23 | `antInsuranceOptions` | 蚂蚁保 \| 选项 | **废弃** | 同上 |

## C. AntOrchard（芭芭农场果园 / 金豆）— 9 项

| # | GR 开关 | GR 中文名 | 判定 | 依据 |
|---|---------|-----------|------|------|
| 24 | `orchardAutoHarvest` | 自动收果树 \| 开启 | **移植** | 阶段 2 已建 `AntOrchardAutoHarvest.harvest()`，缺开关 |
| 25 | `orchardAutoPlant` | 自动种果树 \| 开启 | **移植** | 阶段 2 已建 `AntOrchardAutoHarvest.autoPlant()`，缺开关 |
| 26 | `orchardDrawListTask` | 农场抽抽乐任务 | **移植** | 跑任务换抽奖次数；需补 `orchardDrawListTask()` + `finishTaskantorchard()` 两 RPC，其余复用 M 已有 `enterDrawActivityantorchard/receiveDrawTaskAwardantorchard/drawSyncantorchard/drawantorchard` |
| 27 | `AutoAntOrchardDrawTaskList` | 农场抽抽乐任务 \| 推荐黑白名单 | **移植** | 阶段 2 已建 `AntOrchardDrawTaskListMap` + `AlipayAntOrchardDrawTaskList` |
| 28 | `AntOrchardDrawTaskList` | 农场抽抽乐任务 \| 黑名单列表 | **移植** | 同上 |
| 29 | `goldenBeans` | 金豆夺宝 \| 开启 | **复用** | M 的 `goldenbeans` 独立模型已完整实现（见 30-34） |
| 30 | `goldenBeansTask` | 金豆完成任务领奖 | **复用** | M 的 `goldenBeansAutoTask` + `GoldenBeansTasks` |
| 31 | `GoldenBeansGameCenter` | 农场金豆乐园 \| 游戏宝箱 | **复用** | M 的 `goldenBeansGameDraw` + `GoldenBeansGameCenter` |
| 32 | `goldenBeanExchangeManure` | 金豆夺宝 \| 肥料换金豆 | **复用** | M 的 `goldenBeansAutoManureExchange` → `GoldenBeansExchange.exchangeManure` |
| 33 | `goldenBeanDailyManureLimit` | 金豆夺宝 \| 当日兑换肥料上限 | **复用** | M 的 `goldenBeansManureExchangeLimit` |
| 34 | `goldenBeanReserveManure` | 金豆夺宝 \| 剩余肥料下限 | **复用** | 同 #20，M 用单日上限保护 |

## D. AntForestV2（蚂蚁森林 / 保护地）— 5 项

| # | GR 开关 | GR 中文名 | 判定 | 依据 |
|---|---------|-----------|------|------|
| 35 | `combineAnimalPiece` | 合成动物碎片 | **移植** | M 无；RPC `alipay.antforest.forest.h5.combineAnimalPiece` 缺失，需补 |
| 36 | `newMonopolyExchangeTree` | 新版保护地 \| 领取证书 | **移植** | M 有 9 个 monopoly RPC，独缺 `newMonopolyExchangeTree`，需补 |
| 37 | `preNewMonopolyExchangeTree` | 新版保护地 \| 提前兑换未开始保护地证书 | **移植** | 是 #36 的从属开关，`setDependsOn("newMonopolyExchangeTree")` |
| 38 | `switchNewMonopolyMap` | 新版保护地 \| 切换最新可保护地图 | **移植** | M 缺 `queryMonopolyMapPage` + `switchMonopolyMap` 两 RPC，需补 |
| 39 | `partnerteamWaterMax` | 组队合种浇水上限（0不限) | **移植** | M 的 `partnerteamWaterNum` 是「每日目标」，无累计上限概念 |

## E. AntFarm（蚂蚁庄园 / 爱心鸡结号）— 4 项

| # | GR 开关 | GR 中文名 | 判定 | 依据 |
|---|---------|-----------|------|------|
| 40 | `competitionLeadEggs` | 激进模式 \| 捐至榜首领先蛋数 | **增强** | M 的 `stealRankS2()` 已硬编码 `rank1Donation - myDonation + 1`，`+1` 即此项。**默认值 1 = 与 M 现状完全一致** |
| 41 | `competitionDailyLimit` | 排位捐蛋 \| 捐蛋上限(0不限) | **复用** | M 的 `competitionStealLimit`（0 不限，0~1000），按最终捐献量判定 |
| 42 | `competitionStealMinutes` | 激进模式 \| 霸榜提前分钟数 | **废弃** | M 的 S2 模型是「爱心蛋排位」，无「霸榜」机制 |
| 43 | `stealRankMinutes` | 激进模式 \| 偷榜提前分钟数 | **复用** | M 的 `competitionStealMinutes`（默认 30，0~240）等效；是否偷榜由 `competitionStealRank` 布尔开关控制 |
| 44 | `competitionStarNum` | 保底模式 \| 目标星星数或爱心值 | **废弃** | M 的 S2 排位按「排行榜第一名差额」定向捐蛋，无「星星/爱心值保底」模型；强插会与 `stealRankS2` 的判定链冲突 |

## F. 独立模块（各 1 项）

| # | 所在文件 | GR 开关 | GR 中文名 | 判定 | 依据 |
|---|----------|---------|-----------|------|------|
| 45 | AntSports | `QUERY_ITEM_LIST` | 健康岛 \| 健康能量兑好礼 | **移植** | M 健康岛有 SIGN/TASK_CENTER/BUBBLE_TASK/WALK_GRID，独缺兑好礼 |
| 46 | AntOcean | `exchangeProject` | 海洋摸鱼 \| 兑换科学证书 | **移植** | M 海洋摸鱼有任务/黑白名单，缺证书兑换 |
| 47 | ProtectEcology | `protectAncientMinNum` | PK赛 \| 保护古树 | **移植** | M 有合种/马拉松/古树医生，缺 PK 赛保护古树 |
| 48 | AntDodo | `useMaxExchangeFriendCard` | 帮抽交换 \| 用历史最多卡片 | **移植** | M 有 `collectToFriendType/List`，缺「用历史最多卡片」策略 |

## G. AnswerAI（已合并）— 5 项

| # | GR 开关 | 判定 | 说明 |
|---|---------|------|------|
| 49 | `useGeminiAI`（ChoiceModelField） | **已合并** | 与 #51/#52 合并为单一 `aiProvider` 单选（CUSTOM=0/GEMINI=1/TONGYI=2） |
| 50 | `useGeminiAIToken` | **已合并** | 保留为 `geminiAIToken` |
| 51 | `setTongyiAIToken` / `getTongyiAIToken` | **已合并** | 保留为 `tongyiAIToken` |

---

## 施工顺序

1. **A 段**：`backupLogDays`（增强，默认 0）
2. **F 段**：4 个单开关（各自 RPC 若缺则补）
3. **E 段**：`competitionLeadEggs`（默认 1）
4. **D 段**：5 项（补 4 个 RPC）
5. **C 段**：5 项（补 2 个 RPC + 跑批逻辑）
6. **B 段**：7 项（补 4 个 RPC + 跑批逻辑）
7. 每段完成后跑 `port-tools/check.sh` 静态校验
