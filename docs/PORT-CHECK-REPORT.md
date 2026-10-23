# 移植自检报告

生成时间：2026-10-23
基线：`aw1y2z/Sesame-M`（工作区最新代码，未改动原仓库）
产出：`sesame-m-gr`，`applicationId = io.github.xqrx90.sesame.mgr`

---

## 一、规模

| 项 | 数值 |
| --- | --- |
| Java 源文件 | 251 |
| Kotlin 源文件 | 10 |
| 相对 Sesame-M **新增**文件 | 16 |
| 相对 Sesame-M **删除**文件 | 0 |
| 提交数 | 5 |
| 最近一次移植提交改动 | 18 文件 / +1226 / −4 行 |

> 「−4 行」全部是参数化改造（`AntFarm` 领先蛋数、`FileUtil.clearLog`），**没有删除任何 M 的功能语句**。

## 二、新增文件清单（16）

### MTOP 直调层（3）
- `mtop/bridge/MtopCaller.java`、`mtop/bridge/MtopHook.java`、`mtop/intervallimit/MtopIntervalLimit.java`

### 独立功能模块（11）
- `model/task/omegakoiTown/OmegakoiTown.java`、`OmegakoiTownRpcCall.java` —— 锦鲤小镇
- `model/task/antOrchard/AntOrchardAutoHarvest.java` —— 果园自动收种
- `model/task/antMember/MerchantService.java`、`MerchantServiceRpcCall.java` —— 商家服务
- `model/task/antMember/SesameAlchemy.java`、`AlchemyRpcCall.java` —— 芝麻炼金
- `model/task/antForest/StudentCheckIn.java` —— 学生签到红包
- `model/normal/answerAI/AnswerAIInterface.java`、`GeminiAI.java`、`TongyiAI.java` —— AI 服务商

### 配套实体 / 名单（2）
- `entity/AlipayAntOrchardDrawTaskList.java`、`util/idMap/AntOrchardDrawTaskListMap.java`

## 三、静态校验结果

| 检查项 | 结果 |
| --- | --- |
| GR 旧命名空间残留（`lazyimmortal` / `XposedHelpers`） | **0 处** |
| `package` 与目录一致性 | 251/251 通过 |
| 大括号平衡（单遍词法扫描，正确跳过注释/字符串/文本块） | 251/251 平衡 |
| 新增 RPC 签名与调用点逐一比对 | 23/23 匹配 |
| 新增方法重复定义扫描 | 0 重复 |
| `ModelOrder` 注册表 | 15 条，新增 Model 均已注册 |
| 未注册 Model 子类 | 0 |
| 跨类符号存在性（`XHelpers` / `Log` / `TimeUtil` / `IntervalLimit` 等） | 全部存在 |

### 已修正的签名不一致（逐条比对中发现）

| 调用点 | GR 版签名 | M 版签名 | 处理 |
| --- | --- | --- | --- |
| `AntMemberRpcCall.finishSesameTask` | `(String recordId)` | `(String recordId, String extCheck)` | 改调 2 参版本并传 `null`，请求体与 GR 完全一致 |
| `AntOrchardRpcCall.finishTaskantorchard` | 需新增 | **M 已有**同签名同端点 | 删除重复实现，直接复用 M |
| `MessageUtil.checkSuccess` | 返回 `boolean` | 返回 `Boolean`（装箱） | 调用处均为 `if` 条件，自动拆箱，无需改动 |

## 四、与 Sesame-M 的兼容性

| 项 | 处理 |
| --- | --- |
| `applicationId` | `io.github.xqrx90.sesame.mgr`（独立，可与 M / GR / 官方版共存） |
| `namespace` | 保持 `io.github.aw1y2z.sesame` 不变（类全限定名、`ComponentName` 均未改） |
| 配置目录 | `sesame-MGR`（M 为 `sesame-M`，互不覆盖） |
| 广播 action | `io.github.xqrx90.sesame.mgr.status` |
| 通知渠道 ID | `io.github.xqrx90.sesame.mgr.ANTFOREST_NOTIFY_CHANNEL` |
| `versionCode` | `1000000 + gitCommitCount`（原为纯 commit 数，新仓库会退化成 1 导致无法覆盖安装） |
| 签名 | 仓库内固定 `keystore/debug.keystore`（PKCS12，alias `androiddebugkey`），CI 每次产出一致签名 |
| ABI 拆分 | 已移除 —— 本项目无 `.so`，ABI 拆分只会产出内容相同的重复包 |

## 五、未能本地验证的部分

| 项 | 原因 | 补救 |
| --- | --- | --- |
| `assembleNormalDebug` 编译 | 本机**无 JDK、无 Android SDK**，`java: command not found` | 由 GitHub Actions（JDK 17 + Android SDK）实际编译；编译报错按 CI 日志修正 |
| 实机运行 / RPC 联通性 | 需支付宝宿主环境与真实账号 | 提供 debug 包给用户自测；每个新开关默认 **false**，不影响原有行为 |

> 所有移植开关默认值均为 **false**（或「保持 M 原行为」的中性值），因此即使某项逻辑有偏差，
> 只要不主动开启就不会影响既有功能。

## 六、风险提示

1. **`switchNewMonopolyMap` 未移植**：GR 该逻辑内嵌在 `monopoly()` 主体约百行，需重构 M 方法主体，已记入判定表待后续单独处理。
2. **抽奖时序差异**：GR 的 `orchardDrawListTask` 末尾会顺带把抽奖次数抽完；本项目只做「做任务换次数」，
   抽奖交给 M 已有的「抽抽乐(阿肥寻宝记)」开关，避免同轮重复抽奖。若两个开关都开，抽奖由后者统一驱动。
3. **能量消耗型开关**：健康岛「兑好礼」与 PK 赛「保护古树」都会消耗能量，已分别加「单日兑换上限」与
   「段位未知则整轮跳过」的保护，但仍建议先小范围开启观察日志。

---

# 第二轮：全量代码自检（跨文件连通性 + 缺陷修复）

第一轮是「移植过程中的逐点比对」，第二轮把范围扩大到**全部 251 个 java + 10 个 kt**，
目标是回答一个问题：*代码之间到底通不通？*

## 七、方法：自建静态分析器，并用注入用例证明它有效

本机无 JDK / Android SDK，无法编译，所以用 Python 写了 4 个词法级分析器
（能正确跳过行/块注释、字符串、字符字面量、文本块，避免把注释里的代码当成代码）：

| 工具 | 作用 |
| --- | --- |
| `java_analyzer.py` | 全仓符号表（package / import / 类 / 方法名+参数类型 / 字段）+ 跨类调用解析 |
| `bugscan.py` | JSON 硬取、资源未关闭、空 catch、switch 穿透 |
| `fields_check.py` | 开关字段「声明 vs 注册」一致性、`setDependsOn` 目标存在性 |
| `code_audit.py` | 开关 code 唯一性、与参考仓的差集 |

> **关键一步：证明分析器不是"僵尸通过"。** 每个检查项都造了最小注入用例，
> 确认它能抓出该抓的、且不误报不该抓的。例如：
> `A.missing(1)` → 报"方法不存在"；`A.ok()` → 报"参数数不符"；`A.name(1,2)` → 同样报出；
> 缺 import 的跨包调用 → 报出；全限定类名写错 → 报出；嵌套枚举 `values()` → 不误报；
> `case 1:` 无 break → 报出，而 `case 1: case 2:`（合并标签）与带 `break`/`return` 的 → 不报。

分析器本身迭代修正了 7 处盲区，这 7 处每修一个，误报就从数百条降到个位：
Kotlin 无分号导致 `package` 吞掉整个文件、`void`/基本类型被当关键字而丢弃方法定义、
泛型尖括号里的逗号被当成参数分隔符、枚举隐式方法与业务方法同名、
`import android.util.Log` 遮蔽仓内同名类、`companion object` 与嵌套 data class 构造器、
继承链上的静态方法。

## 八、检查矩阵与结果

| 检查项 | 结果 | 说明 |
| --- | --- | --- |
| 跨类调用目标方法是否存在 | **0 错误** | 含继承链、父类静态方法、嵌套类 |
| 调用点参数个数与方法签名是否匹配 | **0 警告** | 兼容 varargs 与泛型 |
| 跨包引用是否漏 `import` | **0 错误** | 同包/通配符/静态导入均正确识别 |
| 本项目包下的全限定引用 | **0 错误** | 类名均在仓内存在 |
| 同签名方法重复定义（编译错误） | **0** | 按「参数类型签名」判重，不误报合法重载 |
| `ModelTask` 子类是否都已注册 | **0 遗漏** | 13 个子类 + `AnswerAI`/`BaseModel` 全部在 `ModelOrder` |
| 开关 code 唯一性 | **4 处跨 Model 重复，均无害** | 配置按 `modelCode` 隔离存储，不同模块同名不串号 |
| `setDependsOn` 目标 code 是否存在 | **0** | 联动链全部有效 |
| JSON 硬取未在 try 内 | **0** | 移植代码统一用 `optXxx` + 空判 |
| 流/Reader 打开后未关闭 | **0** | 均为 try-with-resources 或 try/finally 手动 close |
| switch case 穿透 | **0** | — |
| 大括号平衡 / `package` 与目录一致 | **251/251 通过** | 词法级扫描 |
| 空 catch 块 | 33 处（**均为有意忽略**） | 集中在 hook 层 `catch (Throwable ignored) {}`，属正常写法 |
| **RPC 端点：本仓 vs 上游 Sesame-M** | **575 个同名方法，0 处不一致** | 硬证明：移植过程**没有改动上游任何既有 RPC**，新增的 20 个方法全部是追加 |
| RPC 端点：本仓 vs Sesame-GR | 613 个同名方法，17 处不一致 | 逐条核对后：15 处是 M 与 GR 的固有演进差异（如 M 更新过参数格式）或字符串换行导致的切分差异；1 处 `joinSesameTask` 的 `sceneCode=zml` 上游 M 本来就有；**1 处是真问题**，见第九章第 4 条 |

## 九、本轮发现并修复的问题

| # | 位置 | 问题 | 处理 |
| --- | --- | --- | --- |
| 1 | `AntMember.promise()` | `promiseList` 的配置项在 `getFields()` 中被注释停用 → 字段恒 `null`，但 577 行直接 `promiseList.getValue()`，**无判空** | 补 `promiseList != null && getValue() != null`：保持停用现状，恢复入口时不再 NPE |
| 2 | `AntSports.queryProjectList()` | 同类问题：`donateCharityCoinAmount.getValue()`、`donateCharityCoinType.getValue()` 直接拆箱，字段恒 `null` 必 NPE | 前者加判空提前 `return`，后者加 `== null ||` 按「只捐一次」处理 |
| 3 | `StudentCheckIn` | 阶段 2 已移植该类，但**全仓零引用**——移植了却没接上，功能实际用不了 | 在 `AntForestV2` 新增开关「青春特权 \| 学生签到红包」（默认 **false**，与 GR 上游把该调用注释掉的状态一致），接回执行链 |
| 4 | `AntForestRpcCall.queryMonopolyMapPage()` | **本轮我自己的移植偏差**：该方法是新增的，但传参复用了 M 的 `monopolyPayload()`，其 `source` 是 `monopoly_home_board`；而 GR 原版此接口用的是 `monopoly_home_animal`。两者属于不同功能域，用错会导致地图查询被服务端拒绝 | 改为严格照 GR 硬编码 `monopoly_home_animal` + 随机 `uniqueId`，并在注释里说明为何**不**复用 `monopolyPayload()` |

> 问题 1、2 是 **Sesame-M 上游自带的缺陷**（GR 同样存在）：功能被注释停用后，执行代码没跟着清理。
> 两处目前都处于不可达分支（`promise()` / `queryProjectList()` 均无调用点），所以此前不会真的崩，
> 但一旦恢复调用入口就会立刻 NPE，属于定时炸弹。

## 十、判定为「无害 / 等价复用」而不改动的项

| 项 | 结论 |
| --- | --- |
| 10 个「死字段」（仅声明、无任何引用） | 如 `AntOrchard.orchardPlantNew`、`AntSports.energyStrategy`、`AntOrchard.driveAnimal*` 等，**GR 上游同样存在**（已逐项核对），属历史残留，不影响运行 |
| 6 处「声明未注册但被引用」 | `AntForestV2.doubleClickType` / `doubleCardConstant`、`AntMember.promiseList`、`AntSports.donateCharityCoin*`。前三处的使用点原本就有 `== null` 保护，后两处已在本轮补上 |
| 与 GR 的 28 项开关差集 | 逐项核对后 **无真实功能缺口**：≥20 项由 M 的等价实现覆盖（如 GR `debugMode` → M `AppConfig.enableDebugLog`；GR `goldenBeans*` 6 项 → M 独立 `goldenbeans` 模块的 `goldenBeansAutoManureExchange`/`ManureExchangeLimit`/`SesameExchangeLimit`/`GameDraw` 等）；`competitionStarNum`/`competitionDailyLimit` 是 GR 那套偷榜算法的参数，M 用 `competitionLeadEggs` 实现了同类能力；`promise`/`promiseList` 两端都已停用；`switchNewMonopolyMap` 为已记录的未移植项 |
| 4 处 code 重复 | `assistFriend`、`assistFriendList`、`drawGameCenterAward`、`executeInterval` 分布在不同的 Model 内，配置按 `modelCode` 隔离，不会串号 |

## 十一、仍未本地验证的部分

| 项 | 原因 |
| --- | --- |
| 实际编译 | 本机无 JDK / 无 Android SDK，只能由 GitHub Actions（JDK 17 + Android SDK）真正编译 |
| RPC 联通性 | 需支付宝宿主环境与真实账号 |

第二轮已在**静态可判定范围内做到零错误**：跨类调用、参数、方法名、import、注册表、字段联动
全部闭合。剩余风险集中在「运行时宿主接口是否如预期」这一类，只能靠实机验证。

## 十二、上游同步记录（2026-10-23）

### 12.1 两仓最新状态核查

用 GitHub API 直接查远端，避免浅克隆本地 log 的截断误判：

| 仓库 | 远端最新提交 | 时间 | 与本仓基线的关系 |
| --- | --- | --- | --- |
| `Dragon813/Sesame-GR`（origin/main） | `fe0b5650` | 2026-10-20 | **无新提交**，本仓已是该版本 |
| `aw1y2z/Sesame-M`（origin/MIUIX-api102） | `6c476554` | 2026-10-22 | 新增 **1 个**提交，本仓基线为 `fdf58306`（10-21） |

即：本轮只有 Sesame-M 需要同步，GR 侧无更新。

### 12.2 上游提交 `6c476554` 逐行审阅

提交信息：**修复新版保护地任务列表不全**，改动 2 文件 4 行。

| 改动点 | 内容 | 审阅结论 |
| --- | --- | --- |
| `AntForestRpcCall.listMonopolyTasks` | `regionCode` 从顶层移入 `extend` 对象 | **认可**。MTOP 的业务参数走 `extend` 是本站点一贯约定——GR 自己的 `listTaskopengreen()`（青春特权）也是 `{"extend":{"businessSource":...,"osType":...,"version":...}}`。放顶层时服务端读不到区域，只回残缺列表 |
| `AntForestV2` 同步调用点 | `if (monopolyTasks.getValue() && MonopolyTaskListMap.getMap().isEmpty())` → `if (monopolyTasks.getValue())` | **认可，但确认过代价**。见 12.3 |

### 12.3 对「每轮都同步」的副作用核查

去掉 `isEmpty()` 判断后，`initMonopolyTaskListMap()` 每轮都会执行，代价逐项核过：

| 潜在顾虑 | 核查结果 |
| --- | --- |
| 是否在紧循环里？ | **不是**。`run()`（AntForestV2:452）是单次直线方法，无 `while`；每个模型周期只进一次 |
| 每轮多几次 RPC？ | **2 次**（`queryMonopolyEntryInfo` 取 regionCode + `listTaskopengreen` 取任务）。同轮 `monopolyTask()` 本来就要打这两个接口，占比可忽略 |
| 写盘开销？ | `StringMapStore.save()` 每轮整份覆盖写一次。次数等同于模型周期（非秒级），可接受 |
| 名单会不会被清空？ | **不会**。`add()` 是 `ConcurrentHashMap.put` 按键去重，`load()` 先 clear 再从文件整份载入，用户已产生的名单不会丢 |
| UI 读到瞬时空表？ | 理论窗口存在（`load()` 的 clear→putAll 之间），但只在配置页渲染瞬间，微秒级，实际不可见 |

**结论：改动本身无害，收益明确**——`listMonopolyTasks` 有两个消费方，除了黑名单候选
（`initMonopolyTaskListMap`），**真正的任务执行循环 `monopolyTask()` 也调它**。
所以这个修复同时治好了「保护地任务少做」，不只是候选列表少几项。

### 12.4 同步后的差异基线

同步完成后，把 M 新版整个源码树导出，与本仓做 `diff -rq` 全量比对：

```
差异 36 处 = 23 个已修改文件（本仓移植改动） + 13 个仅本仓存在的文件（移植新增）
无「仅 M 有」的文件 → 上游一个文件都没漏
```

### 12.5 同步后重跑全部静态检查

| 检查项 | 结果 |
| --- | --- |
| 同签名方法重复定义 | 0 |
| 跨类调用目标不存在 | 0 |
| 参数数量不匹配 | 0 |
| 漏 import / 全限定类写错 | 0 |
| JSON 硬取未在 try 内 | 0 |
| 资源未关闭 | 0 |
| switch case 穿透 | 0 |
| `setDependsOn` 断链 | 0 |
| 空 catch | 33（全部为 hook 层 `catch (Throwable ignored) {}`，有意静默） |
