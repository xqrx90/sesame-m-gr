# Sesame-MGR（芝麻粒-MGR）

[![License](https://img.shields.io/github/license/xqrx90/sesame-m-gr.svg)](LICENSE)

> **Sesame-M 的代码底座 + Sesame-GR 的能力补全**，一个"逐点择优"的合体版本。

本项目的定位很简单：**以 Sesame-M（`aw1y2z/Sesame-M`）为底座**——保留它现代化的 libxposed API 102、
Compose/Miuix UI、日志体系与构建链；**凡是 Sesame-M 没有、而 Sesame-GR（`Dragon813/Sesame-GR`）具备的能力，
逐项补齐**。合并不是"整体搬过来"，而是每个功能点单独比对、逐条择优，避免两套实现打架。

`applicationId` 为 `io.github.xqrx90.sesame.mgr`，配置目录为 `sesame-MGR`，可与芝麻粒-M、芝麻粒-GR
以及官方版**三版共存安装、互不覆盖**。

## 为了大家的资金安全与个人信息安全，强烈建议
1. 不要使用任何未开放源代码的修改版！
2. 不要使用任何未开放源代码的修改版！！
3. 不要使用任何未开放源代码的修改版！！！

---

## 一、血缘与合体方式

```
XQuickEnergy (constanline / pansong291)
        │
   Sesame-TK (TKaxv-7S)
        │
   ├── Sesame-GR (Dragon813, 2025-10)   Xposed API 82 / XML+AppCompat UI / 闭源 .so + Vue3 Web
   │
   └── Sesame-M  (aw1y2z, 2026-06)      libxposed API 102 / Compose+Miuix / 全开源
              │
              ▼
        ★ Sesame-MGR（本项目）★
        底座 = M，能力 = M ∪ GR（逐点择优）
```

| 维度 | 取自 | 说明 |
| --- | --- | --- |
| 运行框架 / 构建链 / UI / 日志体系 | **M** | libxposed 102、AGP 9.2.1、compileSdk 37、Compose+Miuix |
| 缺失的开关与执行链 | **GR** | 逐个比对后补齐，见下方「移植清单」 |
| 同名同义功能 | **M** | M 的实现普遍更新、更完整，GR 版不引入 |

## 二、从 Sesame-GR 移植过来的能力

> 完整逐项判定（含"为什么这一项不移植"）见 **[docs/PORT-MATRIX.md](docs/PORT-MATRIX.md)**。
> 判定口径：`移植` / `增强` / `复用` / `废弃` / `未移植`。

### 新增功能模块

| 模块 | 能力 | 说明 |
| --- | --- | --- |
| **MTOP 直调层** | `mtop/bridge/*` | 反射调用支付宝进程内 MtopService，与 RPC 通道互补 |
| **锦鲤小镇** | `OmegakoiTown` | 小镇任务 / 签到 / 房产兑换（独立 Model，6 小时轮询） |
| **果园自动种植** | `AntOrchardAutoHarvest` | 自动收果树 / 自动种果树（含种子优选） |
| **商家服务** | `MerchantService` | 服务商签到、开门打卡（6:00–12:00 窗口） |
| **芝麻炼金** | `SesameAlchemy` | 消耗芝麻粒炼金、炼金签到、芝麻积分宝箱、限时任务、道具使用 |
| **AI 服务商** | `GeminiAI` / `TongyiAI` | AI 答题可选自定义 / Gemini / 通义千问 |

### 挂到已有模块上的开关

| 所在模块 | 新增开关 |
| --- | --- |
| 运动 | 健康岛 \| 健康能量兑好礼（含单日上限） |
| 海洋 | 海洋摸鱼 \| 兑换科学证书 |
| 保护 | PK赛 \| 保护古树（自动解析当前赛季号与用户段位） |
| 物种 | 帮抽交换 \| 用历史最多卡片 |
| 森林 | 合成动物碎片、新版保护地 \| 领取证书、新版保护地 \| 提前兑换未开始保护地证书 |
| 农场 | 自动收果树 / 自动种果树、农场寻宝 \| 做任务换抽奖次数 + 任务黑白名单 |
| 会员 | 消费金 \| 签到、芝麻炼金 \| 炼金 / 签到、芝麻粒 \| 芝麻炼金任务、芝麻积分 \| 宝箱、商家服务 \| 签到 / 开门打卡 |
| 基础 | 保留之前日志天数(除runtime)，默认 0 = 只留当天（保持 M 原行为） |

### 移植时顺手修掉的 GR 缺陷

移植不是照抄。以下几处 GR 源码里本身就是坏的，本项目一并修正：

1. **健康岛「健康能量兑好礼」**：GR 的 `neverLandBenefitList` 字段声明了却从未 `addField` 进
   `getFields()`，运行时恒为 `null`，`contains()` 必然 NPE —— 该功能在 GR 里**从未真正跑通**。
   本项目去掉白名单，改用「剩余数量 + 状态 + 能量」三重判定 + 单日兑换上限。
2. **海洋「兑换科学证书」**：GR 直接取 `properties` 的子字段，响应缺字段时 NPE。已补空判。
3. **森林「动物碎片合成」**：GR 用 `getJSONArray` 硬取 `animalProps`，字段缺失即抛异常。改用 `optJSONArray` + 空判。

### 明确不移植（并说明理由）

| GR 项 | 原因 |
| --- | --- |
| 金豆夺宝 6 项开关 | M 的 `goldenbeans` 独立模型是它的**超集**（多了挖矿、金豆商城、芝麻粒换豆） |
| 炼金金豆 4 项开关 | M 的 `GoldenBeansEntry.ALCHEMY`(ZHIMA) 已覆盖签到 + 任务 + 兑换 |
| 基础开关 7 项（抓包/气泡/常驻通知/电池权限等） | M 已迁到 `AppConfig` 全局配置层，粒度更细 |
| 黄金票 | M 有 `enableGoldTicketTask/Sign/Consume/Browse` 四个开关，是 GR 单项的超集 |
| `competitionStarNum`（保底模式目标星星数） | M 的爱心鸡结号按"榜首差额"定向捐蛋，无"星星 / 爱心值"模型 |
| `switchNewMonopolyMap`（切换最新可保护地图） | GR 该逻辑内嵌在 `monopoly()` 主体中约百行，照搬需重构 M 的方法主体 |
| `antInsurance` / `closeCaptchaDialogVPN` | GR 源码中已整行注释，功能未启用 |

## 三、关于「单 flavor（无 compatible 兜底线）」

Sesame-GR 的构建脚本里有 `normal` / `compatible` 两个 flavor。很多同源项目用 `compatible` 作为
"依赖版本冲突时的兜底包"（通常是降低某个库的版本，比如把 Jackson 从 2.x 高版本降到能跑的低版本）。

**本项目的实测结论：GR 的 `compatible` 在源码层面没有任何差异。**

- `app/src/` 下只有 `main` 一个源集，**没有**任何 flavor 专属源码目录；
- 全仓 **0 处**引用 `BuildConfig.FLAVOR` / `BuildConfig.FLAVOR_app`；
- 变量 `applicationType` 只被 `build.gradle` 用于拼接 APK 文件名；
- 唯一的实质差异是 **Jackson 2.18.2 → 2.13.5**（仅改版本号，不改一行代码）。

所以本项目**保持单 flavor**——移植一个"只改依赖版本号"的 flavor 没有意义。等价补偿写在下面的排错章节。

### 如果安装后出现 Jackson 相关崩溃（`NoSuchMethodError` / `NoClassDefFoundError`）

这通常是宿主（支付宝）进程里已有旧版 Jackson 造成的类冲突。按下面顺序处理：

1. **优先确认崩溃栈里出现的类**是否为 `com.fasterxml.jackson.*`；确认是再往下做。
2. 编辑 `app/build.gradle`，把 Jackson 版本从 `2.22.x` 降到 `2.13.5`：
   ```gradle
   // 原：ext.jacksonVersion = '2.22.3'（或实际行）
   // 改：
   ext.jacksonVersion = '2.13.5'
   ```
3. 重新构建。若仍冲突，说明宿主里的版本更低，继续降到 `2.9.10`。
4. 极端情况下可在 `app/proguard-rules.pro` 里保留（不混淆）Jackson 相关类，避免反射调用被裁剪。

> 换版本后请**完整卸载再安装**，不要覆盖安装，避免旧 dex 残留。

## 四、主要功能

感谢蚂蚁森林对绿化事业的贡献，也为祖国的绿化事业出一份微薄之力。

配置页按分组与模块两级组织（**新增开关会自动出现在界面上，无需改 UI 代码**）：

| 分组 | 模块 | 主要功能 |
| --- | --- | --- |
| 基础 | 基础 | 执行间隔、定时执行与定时唤醒、只收能量时段、超时重启、异常等待、配置与日志备份份数、RPC 请求列表 |
| 森林 | 森林 | 收取自己与好友能量、一键收取、不收取名单、Pk 榜与 1V1 能量挑战、金球、浇水与返水、复活能量、能量雨、道具收集与赠送、自动续用保护罩、合种浇水、森林乐园、寻宝与抽抽乐助力、**动物碎片合成**、保护地巡护、**新版保护地领证书**、各类每日任务与礼盒 |
| 保护 | 保护 | 合种、碳中和、保护森林、保护动物、保护地、保护海洋、**PK赛保护古树** |
| 保护 | 古树 | 保护古树（按区划代码列表逐区保护，可限定仅星期一、三、五运行） |
| 物种 | 物种 | 物种任务、道具（抽卡 / 万能卡片 / 图鉴勋章）、帮抽卡片、**帮抽后自动互换**、赠送卡片 |
| 海洋 | 海洋 | 海洋任务、清理海域、万能拼图、潘多拉海域、海洋摸鱼、**兑换科学证书** |
| 庄园 | 庄园 | 饲料与道具、投喂与帮喂、召唤 / 雇佣 / 遣返小鸡、每日捐蛋与爱心鸡结号、小鸡乐园 / 厨房 / 日记、收麦子与送麦子、装扮焕新 |
| 新村 | 新村 | 摆摊与收摊、贴罚单、丢肥料与收肥料、请走小摊、新村任务、助力就业岗位、解锁新村新店 |
| 农场 | 农场 | 农场任务、农场施肥（含一键施肥 5 次）、**自动收 / 种果树**、**农场寻宝任务**、农场抽抽乐、农场乐园、摇钱树、分享助力 |
| 金豆 | 金豆夺宝 | 自动签到、自动挖矿、自动完成任务与领奖、金豆乐园抽奖与游戏奖励、肥料换豆 / 芝麻粒换豆（含单日上限）、金豆商城 |
| 运动 | 运动 | 行走路线（含全主题路线）、运动任务、收运动币、抢好友、文体中心、同步步数、行走捐、健康岛、**健康能量兑好礼** |
| 会员 | 会员 | 会员任务与签到、会员积分、芝麻粒与芝麻粒任务、攒芝麻分进度、游戏中心、我的快递、黄金票、福利金、**消费金签到**、**芝麻炼金 / 签到 / 宝箱 / 攒粒任务**、**商家服务** |
| 经营 | 经营 | 打卡、收取、捐助 |
| 其他 | 锦鲤小镇 | **小镇任务、签到、房产兑换**（GR 移植） |
| AI答 | AI答 | 自定义 AI / **Gemini** / **通义千问** 三选一答题（自填接口与令牌，可测试连通性） |

> 分组与模块构成取自 `ModelGroup`、`ModelOrder`；每一项的开关、默认值、取值范围与依赖关系见
> [配置项说明](docs/配置项说明.md)。

## 五、技术栈 / 使用的框架

- **模块运行框架**: [libxposed](https://github.com/libxposed/api)（`io.github.libxposed:api`、`:service`，均 102.0.0），由 [LSPosed](https://github.com/LSPosed/LSPosed) 等兼容框架加载
- **UI**: Jetpack Compose + [Miuix](https://github.com/compose-miuix-ui/miuix) 0.9.4（Xiaomi HyperOS 设计风格组件库）；activity-compose 1.13.0、appcompat 1.8.0、material-icons-extended 1.7.8
- **网络**: OkHttp 4.12.0、NanoHTTPD 2.3.1
- **JSON / 日志 / 注解**: Jackson 2.22（databind 2.22.3、annotations 2.22）、XLog 1.11.1、Lombok 1.18.48
- **构建**: Gradle 9.4.1 / AGP 9.2.1 / Kotlin 2.4.20 / JDK 17

## 六、版本与安装

- `applicationId`：`io.github.xqrx90.sesame.mgr`（与芝麻粒-M / GR / 官方版三版共存）
- 配置目录：`Android/media/com.eg.android.AlipayGphone/sesame-MGR/`
- `versionCode = 1000000 + gitCommitCount`，保证覆盖升级不回退
- 调试包使用仓库内固定 keystore（`keystore/debug.keystore`）签名，CI 每次产出的包可互相覆盖升级

### 下载

| 渠道 | 地址 | 说明 |
| --- | --- | --- |
| 稳定版 | [Releases](https://github.com/xqrx90/sesame-m-gr/releases) | 按 tag 发布的构建 |
| 最新调试包 | [debug-latest](https://github.com/xqrx90/sesame-m-gr/releases/tag/debug-latest) | 跟随 `main` 最新提交，Pre-release |

每个 APK 旁都附有同名 `.sha256` 校验文件，下载后可直接对比。
要求 **Android 8.0（API 26）** 及以上；首次安装需在 LSPosed 里勾选作用域「支付宝」。

发版：打 tag → GitHub Release 发布 → workflow 自动构建并上传 APK；也可用 `workflow_dispatch` 手动出
`debug-latest` 预发布包。CI **只发 Release，不使用 artifact**（避免累积占满 Actions 配额）。

## 七、文档

| 文档 | 内容 |
| --- | --- |
| [移植判定表](docs/PORT-MATRIX.md) | GR → MGR 的 51 项开关逐项判定、依据、施工顺序，以及「为什么不移植这一项」的完整理由 |

> 各模块配置项的 code / 显示名称 / 默认值 / 依赖关系，可从源码 `getFields()` 直接查阅；
> `docs/tools/gen_fields.ps1` 可批量导出。如文档与实现不一致，**以源码为准**。

## 八、免责声明

1. 本 APP 是为了学习研究用，不得进行任何形式的转发、发布、传播。
2. 请于 24 小时内卸载本 APP。若使用期间造成任何损失，作者不负任何责任。
3. 本 APP 不篡改、不修改、不获取任何个人信息及其支付宝信息。
4. 本 APP 使用者因为违反本声明的规定而触犯中华人民共和国法律的，一切后果自负，作者不承担任何责任。
5. 凡以任何方式直接、间接使用 APP 者，视为自愿接受本声明的约束。
6. 本 APP 如无意中侵犯了某个媒体或个人的知识产权，请来信或来电告之，作者将立即删除。

## 九、授权说明

本项目是 [Sesame-M](https://github.com/aw1y2z/Sesame-M) 与 [Sesame-GR](https://github.com/Dragon813/Sesame-GR)
的衍生作品，二者均继承自 [TKaxv-7S 版 Sesame](https://github.com/SenOffical/Sesame-TK)、
[constanline 版 XQuickEnergy](https://github.com/constanline/XQuickEnergy) 与
[pansong291 版 XQuickEnergy](https://github.com/pansong291/XQuickEnergy)。

遵循前述所有上游项目的协议，**禁止**用于任何商业用途、禁止二次修改后**闭源**发布。

第三方组件及其对应的许可证原文已收录于 [licenses/](licenses/) 目录：

| 组件 / 项目 | 许可证 | 文件 |
| --- | --- | --- |
| 本项目自身 | GPL-3.0 | [LICENSE](LICENSE) |
| [aw1y2z/Sesame-M](https://github.com/aw1y2z/Sesame-M) | GPL-3.0 | [licenses/LICENSE-aw1y2z-sesame-m.txt](licenses/LICENSE-aw1y2z-sesame-m.txt) |
| [Dragon813/Sesame-GR](https://github.com/Dragon813/Sesame-GR) | GPL-3.0 | [licenses/LICENSE-dragon813-sesame-gr.txt](licenses/LICENSE-dragon813-sesame-gr.txt) |
| [LSPosed/LSPosed](https://github.com/LSPosed/LSPosed) | GPL-3.0 | [licenses/LICENSE-lsposed.txt](licenses/LICENSE-lsposed.txt) |
| [constanline/XQuickEnergy](https://github.com/constanline/XQuickEnergy) | Apache-2.0 | [licenses/LICENSE-xquickenergy-constanline.txt](licenses/LICENSE-xquickenergy-constanline.txt) |
| [pansong291/XQuickEnergy](https://github.com/pansong291/XQuickEnergy) | Apache-2.0 | [licenses/LICENSE-xquickenergy-pansong291.txt](licenses/LICENSE-xquickenergy-pansong291.txt) |
| [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) | Apache-2.0 | [licenses/LICENSE-miuix.txt](licenses/LICENSE-miuix.txt) |
| [libxposed/api](https://github.com/libxposed/api) | Apache-2.0 | [licenses/LICENSE-libxposed-api.txt](licenses/LICENSE-libxposed-api.txt) |

## 十、特别说明

- 本模块完全免费开源，没有任何收费，请勿二次贩卖。
- 本项目**不支持**合并任何通过修改数据而**实际获利**的功能 PR。
- 鉴于项目的特殊性，本项目可能在任何时间**停止更新**或**删除**。

## 十一、特别感谢

- 感谢芝麻粒生态的维护者与贡献者们（TKaxv-7S、Dragon813、LazyImmortal、Fansirsqi、aw1y2z 等）的无私付出。
