package io.github.aw1y2z.sesame.model.task.antForest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import io.github.aw1y2z.sesame.util.Log;
import io.github.aw1y2z.sesame.util.Status;

/**
 * 青春特权 · 学生签到领红包（移植自 Sesame-GR 的 {@code Privilege#studentSignInRedEnvelope}）。
 * <p>
 * <b>与 GR 的差异（逐点择优结果）</b>：GR 的 {@code Privilege} 类同时包含「青春特权森林道具」
 * 与「学生签到红包」两块。前者 Sesame-M 已有更完整的实现（{@code AntForestV2.YOUTH_FOREST_ROUTES}
 * + {@code queryYouthPrivilegeTaskList} / {@code receiveYouthPrivilegeTaskAward}），故不复用 GR 的写法；
 * 后者 M 完全没有，故单独拆到本类。
 * <p>
 * 规则与 GR 一致：5 点后才执行；查询结果动作为 {@code DO_TASK} 表示今日无签到资格，直接标记今日完成；
 * 否则执行签到，10 点前签到记为 double，之后记为 single。
 */
public final class StudentCheckIn {

    private static final String TAG = "StudentCheckIn";

    private static final String FLAG_STUDENT_TASK = "youth_privilege_student_task";
    private static final String PREFIX_SIGN = "青春特权🧧";

    private static final String RPC_SUCCESS = "SUCCESS";

    private static final int SIGN_START_HOUR = 5;
    private static final int SIGN_END_HOUR = 10;

    private StudentCheckIn() {
    }

    public static void studentSignInRedEnvelope() {
        if (!isSignInTimeValid()) {
            Log.record(PREFIX_SIGN + "5点前不执行签到");
            return;
        }

        if (Status.hasFlagToday(FLAG_STUDENT_TASK)) {
            Log.record(PREFIX_SIGN + "今日已完成签到");
            return;
        }

        try {
            processStudentSignIn();
        } catch (Exception e) {
            Log.error("学生签到异常" + e);
        }
    }

    private static boolean isSignInTimeValid() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= SIGN_START_HOUR;
    }

    private static void processStudentSignIn() {
        String response = AntForestRpcCall.studentQqueryCheckInModel();
        JSONObject result;
        try {
            result = new JSONObject(response);
        } catch (JSONException e) {
            Log.error("学生签到模型解析失败" + e);
            return;
        }

        if (!RPC_SUCCESS.equals(result.optString("resultCode"))) {
            Log.record(PREFIX_SIGN + " 查询失败：" + result.optString("resultDesc"));
            return;
        }

        JSONObject checkInInfo = result.optJSONObject("studentCheckInInfo");
        if (checkInInfo == null || "DO_TASK".equals(checkInInfo.optString("action"))) {
            Status.flagToday(FLAG_STUDENT_TASK);
            return;
        }

        executeStudentSignIn();
    }

    private static void executeStudentSignIn() {
        try {
            String tag = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < SIGN_END_HOUR ? "double" : "single";
            String response = AntForestRpcCall.studentCheckin();
            JSONObject result = new JSONObject(response);
            handleSignInResult(result, tag);
        } catch (JSONException e) {
            Log.error("学生签到失败：" + e.getMessage());
        }
    }

    private static void handleSignInResult(JSONObject result, String tag) {
        String code = result.optString("resultCode");
        String desc = result.optString("resultDesc");

        if (RPC_SUCCESS.equals(code)) {
            Status.flagToday(FLAG_STUDENT_TASK);
            Log.forest(PREFIX_SIGN + tag + desc);
        } else {
            String errorMsg = desc;
            if (desc.contains("不匹配")) {
                errorMsg += "可能账户不符合条件";
            }
            Log.error(PREFIX_SIGN + tag + " 失败：" + errorMsg);
        }
    }
}
