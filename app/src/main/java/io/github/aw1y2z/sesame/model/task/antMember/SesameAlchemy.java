package io.github.aw1y2z.sesame.model.task.antMember;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

import io.github.aw1y2z.sesame.util.Log;
import io.github.aw1y2z.sesame.util.MessageUtil;
import io.github.aw1y2z.sesame.util.Status;
import io.github.aw1y2z.sesame.util.TimeUtil;

/**
 * 支付宝-芝麻炼金（芝麻信用-攒芝麻分-芝麻炼金）+ 芝麻积分宝箱。
 * <p>
 * <b>移植来源与合并策略（逐点择优）</b>：移植自 Sesame-GR 的 {@code SesameAlchemy}，但只取其
 * 「炼金本体」部分。GR 原类的另一半是「炼金入口金豆夺宝」（金豆苗收取 / 签到 / 任务 / 芝麻粒换金豆），
 * 这部分 Sesame-M 的 {@code model/task/goldenbeans} 包已经用 {@code GoldenBeansEntry.ALL}
 * 同时覆盖了「农场」与「炼金」两个入口（含签到、任务、兑换额度与剩余芝麻粒下限），
 * 再搬 GR 那套会造成两套配置各管一半、彼此打架，故<b>不移植、直接复用 M</b>。
 * <p>
 * 本类实现：
 * <ul>
 *     <li>炼金每日签到（sceneCode=alchemy；M 原本只做了 zml 芝麻粒签到，没有 alchemy）</li>
 *     <li>自动炼金（每轮检查剩余次数即炼金，体力不足/满级时停止，循环限速 + 死循环保护）</li>
 *     <li>限时补贴任务（早/中/晚饭时段内自动完成）</li>
 *     <li>阶段奖励领取（炼金次数达标后的次日礼包，awardAvailable 时 claimAward）</li>
 *     <li>炼金道具（queryAvailableItems → useItem，如 BOTTLE）</li>
 *     <li>芝麻积分主页宝箱（WAIT_CLAIM 时开启）</li>
 * </ul>
 */
public class SesameAlchemy {

    private static final String TAG = "SesameAlchemy";

    /**
     * 芝麻炼金主入口，由 AntMember 调用。
     *
     * @param doAlchemy  自动炼金（消耗芝麻粒，含限时补贴与道具使用）
     * @param doSign     炼金每日签到（sceneCode=alchemy）
     * @param doPointBox 芝麻积分主页宝箱
     */
    public static void run(boolean doAlchemy, boolean doSign, boolean doPointBox) {
        try {
            if (doAlchemy || doSign) {
                alchemy(doSign, doAlchemy);
            }
            if (doPointBox) {
                pointTreasureBox();
            }
        } catch (Throwable t) {
            Log.i(TAG, "run err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // ==================== 芝麻积分宝箱 ====================

    /**
     * 芝麻积分主页宝箱：hasBox 且 status=WAIT_CLAIM 时开启，开启后进入倒计时（HANGING）
     */
    private static void pointTreasureBox() {
        try {
            JSONObject jo = new JSONObject(AlchemyRpcCall.queryPointTreasureBox());
            TimeUtil.sleep(500);
            if (!MessageUtil.checkResultCode(TAG, jo)) {
                return;
            }
            JSONObject data = jo.optJSONObject("data");
            if (data == null || !data.optBoolean("hasBox", false)) {
                return;
            }
            if (!"WAIT_CLAIM".equals(data.optString("status"))) {
                return;
            }
            JSONObject openJo = new JSONObject(AlchemyRpcCall.openPointTreasureBox());
            TimeUtil.sleep(500);
            if (MessageUtil.checkResultCode(TAG, openJo)) {
                JSONObject openData = openJo.optJSONObject("data");
                int reward = openData != null
                        ? openData.optInt("rewardAmount", data.optInt("rewardAmount", 0))
                        : data.optInt("rewardAmount", 0);
                Log.other("芝麻积分🎁宝箱#获得[" + reward + "芝麻粒]");
            }
        } catch (Throwable t) {
            Log.i(TAG, "pointTreasureBox err:");
            Log.printStackTrace(TAG, t);
        }
    }

    // ==================== 芝麻炼金 ====================

    /**
     * 芝麻炼金：签到 + 自动炼金（内含限时补贴） + 阶段奖励
     */
    private static void alchemy(boolean doSign, boolean doAlchemy) {
        try {
            // 每日签到（场景码alchemy，复用会员签到RPC，独立去重防重复请求）
            if (doSign && !Status.hasFlagToday("SesameAlchemy::alchemySign")) {
                if (alchemySign()) {
                    Status.flagToday("SesameAlchemy::alchemySign");
                }
                TimeUtil.sleep(500);
            }

            // 自动炼金（每轮检查，无当日标记）
            if (doAlchemy) {
                // 限时补贴任务（早/中/晚饭，时段内完成，跟随自动炼金执行）
                if (!Status.hasFlagToday("SesameAlchemy::timeLimited")) {
                    if (timeLimitedTask()) {
                        Status.flagToday("SesameAlchemy::timeLimited");
                    }
                    TimeUtil.sleep(500);
                }
                doAlchemyLoop();
            }

            // 阶段奖励（炼金次数达标后的次日礼包）
            if (doAlchemy && !Status.hasFlagToday("SesameAlchemy::stageAward")) {
                if (claimStageAward()) {
                    Status.flagToday("SesameAlchemy::stageAward");
                }
            }
        } catch (Throwable t) {
            Log.i(TAG, "alchemy err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 炼金每日签到：queryTaskLists(sceneCode=alchemy) 当日 CAN_COMPLETE 时 completeTask
     *
     * @return true=已处理完（成功或无可签），false=本轮失败待重试
     */
    private static boolean alchemySign() {
        try {
            JSONObject checkInJo = new JSONObject(AntMemberRpcCall.alchemyQueryCheckIn("alchemy"));
            TimeUtil.sleep(500);
            if (!MessageUtil.checkResultCode(TAG, checkInJo)) {
                return false;
            }
            JSONObject data = checkInJo.optJSONObject("data");
            JSONObject currentDay = data == null ? null : data.optJSONObject("currentDateCheckInTaskVO");
            if (currentDay == null) {
                return true;
            }
            String status = currentDay.optString("status");
            if (!"CAN_COMPLETE".equals(status)) {
                return true;
            }
            String checkInDate = currentDay.optString("checkInDate");
            if (checkInDate.isEmpty()) {
                return true;
            }
            JSONObject completeJo = new JSONObject(AntMemberRpcCall.zmCheckInCompleteTask(checkInDate, "alchemy"));
            TimeUtil.sleep(500);
            if (MessageUtil.checkResultCode(TAG, completeJo)) {
                int num = 0;
                JSONObject prize = completeJo.optJSONObject("data");
                if (prize != null) {
                    num = prize.optInt("zmlNum",
                            prize.optJSONObject("prize") != null ? prize.optJSONObject("prize").optInt("num", 0) : 0);
                    if (num == 0) {
                        num = prize.optInt("awardPoint", 0);
                    }
                }
                Log.other("芝麻炼金📅签到#获得[" + num + "]");
                return true;
            }
            return false;
        } catch (Throwable t) {
            Log.i(TAG, "alchemySign err:");
            Log.printStackTrace(TAG, t);
            return false;
        }
    }

    /**
     * 限时补贴任务（早/中/晚饭）：当天时段内 state=1 时 completeTask
     *
     * @return true=已处理完（成功或无任务），false=本轮失败待重试
     */
    private static boolean timeLimitedTask() {
        try {
            JSONObject jo = new JSONObject(AlchemyRpcCall.queryTimeLimitedTask());
            TimeUtil.sleep(500);
            if (!MessageUtil.checkResultCode(TAG, jo)) {
                return false;
            }
            JSONObject data = jo.optJSONObject("data");
            JSONObject taskVO = data == null ? null : data.optJSONObject("timeLimitedTaskVO");
            if (taskVO == null || taskVO.optBoolean("tomorrow", false)) {
                return true;
            }
            int startHour = taskVO.optInt("startHour", -1);
            int endHour = taskVO.optInt("endHour", -1);
            int state = taskVO.optInt("state", 0);
            String templateId = taskVO.optString("templateId");
            if (templateId.isEmpty() || state != 1) {
                return true;
            }
            // 仅在任务时段内提交（如晚饭补贴19~21时）
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (startHour >= 0 && endHour > startHour && (hour < startHour || hour >= endHour)) {
                return true;
            }
            String title = taskVO.optString("longTitle", taskVO.optString("shortTitle", "限时补贴"));
            JSONObject completeJo = new JSONObject(AlchemyRpcCall.completeTimeLimitedTask(templateId));
            TimeUtil.sleep(500);
            if (MessageUtil.checkResultCode(TAG, completeJo)) {
                JSONObject completeData = completeJo.optJSONObject("data");
                // 奖励以completeTask响应data.zmlNum为准（抓包验证），缺失回退queryTask的rewardAmount
                int award = completeData != null
                        ? completeData.optInt("zmlNum", taskVO.optInt("rewardAmount", 0))
                        : taskVO.optInt("rewardAmount", 0);
                Log.other("芝麻炼金⏰限时补贴[" + title + "]#获得[" + award + "芝麻粒]");
            }
            return true;
        } catch (Throwable t) {
            Log.i(TAG, "timeLimitedTask err:");
            Log.printStackTrace(TAG, t);
            return false;
        }
    }

    /**
     * 自动炼金：每轮检查（无当日标记），有剩余次数即循环炼金；
     * 体力不足（staminaExhausted/staminaCurrent不足）或满级（levelFull）时停止
     */
    private static void doAlchemyLoop() {
        try {
            JSONObject home = new JSONObject(AlchemyRpcCall.queryHome());
            TimeUtil.sleep(500);
            if (!MessageUtil.checkResultCode(TAG, home)) {
                return;
            }
            JSONObject data = home.optJSONObject("data");
            if (data == null) {
                return;
            }
            int remaining = data.optInt("alchemyNum", 0);
            if (data.optBoolean("capReached", false)) {
                remaining = 0;
            }
            if (remaining <= 0) {
                // 今日次数已用完，无操作
                return;
            }
            // 体力不够不炼金（staminaExhausted为服务端体力耗尽标志）
            int staminaCurrent = data.optInt("staminaCurrent", -1);
            if (data.optBoolean("staminaExhausted", false) || staminaCurrent == 0) {
                Log.i(TAG, "体力不足#剩余体力[" + staminaCurrent + "]，本轮跳过炼金");
                return;
            }
            // 炼金前使用可用道具（如BOTTLE瓶子）
            useAlchemyItems();

            long goldGot = 0;
            int done = 0;
            int levelUpCount = 0;
            boolean levelFullStopped = false;
            int dailyCap = data.optInt("alchemyDailyCap", 0);
            int staminaCost = -1; // 单次炼金体力消耗，首次响应校准（观测值5）
            // 死循环保护：剩余次数+富余量
            int guard = remaining + 5;
            while (remaining > 0 && guard-- > 0) {
                // 体力不够下一次炼金时停止
                if (staminaCost > 0 && staminaCurrent >= 0 && staminaCurrent < staminaCost) {
                    Log.i(TAG, "体力不足#剩余体力[" + staminaCurrent + "]#本轮已炼[" + done + "次]，停止");
                    break;
                }
                JSONObject alchemyJo = new JSONObject(AlchemyRpcCall.alchemy());
                TimeUtil.sleep(500);
                if (!MessageUtil.checkResultCode(TAG, alchemyJo)) {
                    break;
                }
                JSONObject alchemyData = alchemyJo.optJSONObject("data");
                if (alchemyData == null || !alchemyData.optBoolean("success", true)) {
                    break;
                }
                // 用响应的累计已炼次数回校剩余次数（炼金响应.alchemyNum=累计，与queryHome语义相反），
                // 防止本地计数漂移（如用户在App内手动炼金插队消耗次数导致多发一次失败请求）
                if (dailyCap > 0 && alchemyData.has("alchemyNum")) {
                    int left = dailyCap - alchemyData.optInt("alchemyNum", 0);
                    if (left >= 0 && left < remaining) {
                        remaining = left;
                    }
                }
                // 满级不再炼金（服务端可能仍下发剩余次数，继续炼无效）
                if (alchemyData.optBoolean("levelFull", false)) {
                    levelFullStopped = true;
                    break;
                }
                goldGot += alchemyData.optLong("goldNum", 0);
                done++;
                if (alchemyData.optBoolean("levelUp", false)) {
                    levelUpCount++;
                }
                remaining--;
                // 校准体力消耗并更新剩余体力（响应staminaCurrent为炼金后剩余值）
                staminaCost = alchemyData.optInt("staminaUsedV2", staminaCost > 0 ? staminaCost : 5);
                staminaCurrent = alchemyData.optInt("staminaCurrent", staminaCurrent);
            }
            // 炼金后重新查询主页拿最新状态（金币余额/等级/芝麻粒余额）
            JSONObject newHome = new JSONObject(AlchemyRpcCall.queryHome());
            TimeUtil.sleep(500);
            if (MessageUtil.checkResultCode(TAG, newHome) && newHome.optJSONObject("data") != null) {
                data = newHome.optJSONObject("data");
            }
            if (done > 0) {
                summaryAlchemy(data, done, goldGot, levelUpCount);
            }
            if (levelFullStopped) {
                Log.i(TAG, "已满级#本轮炼金[" + done + "次]后停止");
            }
        } catch (Throwable t) {
            Log.i(TAG, "doAlchemyLoop err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 使用炼金可用道具（queryAvailableItems bizScene=ALCHEMY，逐个useItem）
     */
    private static void useAlchemyItems() {
        try {
            JSONObject jo = new JSONObject(AlchemyRpcCall.queryAvailableItems());
            TimeUtil.sleep(500);
            if (!MessageUtil.checkResultCode(TAG, jo)) {
                return;
            }
            JSONObject data = jo.optJSONObject("data");
            JSONArray items = data == null ? null : data.optJSONArray("items");
            if (items == null) {
                return;
            }
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String itemId = item.optString("itemId");
                String itemType = item.optString("itemType");
                if (itemId.isEmpty()) {
                    continue;
                }
                JSONObject useJo = new JSONObject(AlchemyRpcCall.useItem(itemId, itemType));
                TimeUtil.sleep(500);
                if (MessageUtil.checkResultCode(TAG, useJo)) {
                    Log.other("芝麻炼金🧪使用道具[" + itemType + "]");
                }
            }
        } catch (Throwable t) {
            Log.i(TAG, "useAlchemyItems err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 炼金概况输出：一条日志带炼金次数/产出/等级/余额
     */
    private static void summaryAlchemy(JSONObject data, int done, long goldGot, int levelUpCount) {
        try {
            if (data == null) {
                return;
            }
            long goldBalance = data.optLong("goldBalance", 0);
            int level = data.optInt("currentLevel", 0);
            long zmlBalance = data.optLong("zmlBalance", 0);
            int finishCount = data.optInt("finishAlchemyCount", 0);
            Log.other("芝麻炼金⚗️炼金[" + done + "次]#获得[" + goldGot + "金币]#今日累计[" + finishCount + "次]"
                    + (levelUpCount > 0 ? "#升级[" + levelUpCount + "级→lv" + level + "]" : "")
                    + "#金币余额[" + goldBalance + "]#芝麻粒余额[" + zmlBalance + "]");
        } catch (Throwable t) {
            Log.i(TAG, "summaryAlchemy err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 炼金阶段奖励：queryEntryList 中 ALCHEMY_STAGE_REWARD.awardAvailable 时 claimAward
     *
     * @return true=已处理完（成功或无可领），false=本轮失败待重试
     */
    private static boolean claimStageAward() {
        try {
            JSONObject jo = new JSONObject(AlchemyRpcCall.queryEntryList());
            TimeUtil.sleep(500);
            if (!MessageUtil.checkResultCode(TAG, jo)) {
                return false;
            }
            JSONObject data = jo.optJSONObject("data");
            JSONArray entryList = data == null ? null : data.optJSONArray("entryList");
            if (entryList == null) {
                return true;
            }
            for (int i = 0; i < entryList.length(); i++) {
                JSONObject entry = entryList.optJSONObject(i);
                if (entry == null || !"ALCHEMY_STAGE_REWARD".equals(entry.optString("entryCode"))) {
                    continue;
                }
                JSONObject nextDayAwardDTO = entry.optJSONObject("nextDayAwardDTO");
                if (nextDayAwardDTO == null || !nextDayAwardDTO.optBoolean("awardAvailable", false)) {
                    return true;
                }
                JSONObject claimJo = new JSONObject(AlchemyRpcCall.claimAward());
                TimeUtil.sleep(500);
                if (MessageUtil.checkResultCode(TAG, claimJo)) {
                    Log.other("芝麻炼金🏆阶段奖励#获得[" + nextDayAwardDTO.optInt("pointValue", 0) + "]");
                }
                return true;
            }
            return true;
        } catch (Throwable t) {
            Log.i(TAG, "claimStageAward err:");
            Log.printStackTrace(TAG, t);
            return false;
        }
    }
}
