package io.github.aw1y2z.sesame.mtop.bridge;

import io.github.aw1y2z.sesame.mtop.intervallimit.MtopIntervalLimit;
import io.github.aw1y2z.sesame.util.Log;
import io.github.aw1y2z.sesame.util.XHelpers;

import java.lang.reflect.Method;

/**
 * MTOP 通用调用器（已接入限流）。
 * <p>
 * 原理：走反射拿支付宝进程内的 MtopService 实例，直接构造 fastjson 参数调用
 * {@code syncRequestFastJson}，从而绕开「必须先有一条真实业务请求才拿得到 sender」的限制。
 * 全程使用宿主进程内已有的 fastjson / MethodEnum，不引入任何新依赖。
 * <p>
 * 默认调用（读类接口，GET、不 WUA、15 秒超时，无 headers）：
 * <pre>
 * String resp = MtopCaller.call(loader,
 *     "mtop.tmall.farm.commonapi.get",
 *     "{\"bizParam\":\"{}\",\"bizType\":\"plantTree.showFruitList\"}");
 * </pre>
 * 高级调用（带 headers，headersJson 形如 {"asac":"xxx"}）：
 * <pre>
 * String resp = MtopCaller.call(loader, apiName, dataText,
 *     "{\"asac\":\"xxx\"}", false, true, 15000);
 * </pre>
 * 返回体 JSON 字符串；失败返回 null（会打日志），不抛异常。
 */
public class MtopCaller {

    private static final String TAG = "MtopCaller";

    private static volatile Class<?> jsonClsCache;
    private static volatile Method putCache;
    private static volatile Class<?> methodEnumClsCache;
    private static volatile Method parseObjectMethodCache;

    public static String call(ClassLoader loader, String apiName, String dataText) {
        return call(loader, apiName, dataText, (String) null, false, false, 15000);
    }

    public static String call(ClassLoader loader, String apiName, String dataText,
                              boolean usePost, boolean needWua, int timeoutMs) {
        return call(loader, apiName, dataText, (String) null, usePost, needWua, timeoutMs);
    }

    public static String call(ClassLoader loader, String apiName, String dataText,
                              String headersJson,
                              boolean usePost, boolean needWua, int timeoutMs) {
        MtopIntervalLimit.enterIntervalLimit(apiName);

        try {
            Object svc = MtopHook.resolveInstance(loader);
            if (svc == null) {
                Log.i(TAG, "call 失败：MtopService 为 null，api=" + apiName);
                return null;
            }

            Class<?> jsonCls = ensureJsonCls(loader);
            if (jsonCls == null) {
                Log.i(TAG, "call 失败：fastjson JSONObject 不可用，api=" + apiName);
                return null;
            }
            Method put = ensurePut(jsonCls);
            if (put == null) {
                Log.i(TAG, "call 失败：JSONObject.put 不可用，api=" + apiName);
                return null;
            }

            Object map = XHelpers.newInstance(jsonCls);
            put.invoke(map, "apiName", apiName);
            put.invoke(map, "apiVersion", "1.0");
            put.invoke(map, "dataText", dataText);
            put.invoke(map, "usePost", resolveMethodEnum(loader, usePost));
            put.invoke(map, "type", "originaljson");
            put.invoke(map, "timeout", timeoutMs > 0 ? timeoutMs : 15000);
            put.invoke(map, "needHttps", Boolean.TRUE);
            put.invoke(map, "needSpdy", Boolean.TRUE);
            put.invoke(map, "needWua", needWua ? Boolean.TRUE : Boolean.FALSE);
            put.invoke(map, "needEcodeSign", Boolean.FALSE);
            put.invoke(map, "shouldSkipAutoLogin", Boolean.FALSE);
            put.invoke(map, "ignore_aliauth_autologin", Boolean.FALSE);
            put.invoke(map, "isvOpenAppkey", "DEFAULT_AUTH");
            put.invoke(map, "isvAccessToken", "");
            put.invoke(map, "customHost", "");
            put.invoke(map, "ttid", "");
            put.invoke(map, "tb_eagleeyex_scm_project", "");

            Object headersObj;
            if (headersJson != null && !headersJson.isEmpty()) {
                headersObj = parseJson(loader, headersJson);
                if (headersObj == null) {
                    headersObj = XHelpers.newInstance(jsonCls);
                }
            } else {
                headersObj = XHelpers.newInstance(jsonCls);
            }
            put.invoke(map, "headers", headersObj);

            Object ret = XHelpers.callMethod(svc, "syncRequestFastJson", map);
            if (ret == null) {
                Log.i(TAG, "call 返回 null，api=" + apiName);
                return null;
            }

            // MtopResponse 形态：优先取 second（body），其次 first（header）；都没有就直接 toString
            try {
                Object second = XHelpers.getObjectField(ret, "second");
                if (second != null) return String.valueOf(second);
                Object first = XHelpers.getObjectField(ret, "first");
                if (first != null) return String.valueOf(first);
            } catch (Throwable ignored) {
            }
            return String.valueOf(ret);
        } catch (Throwable t) {
            Log.i(TAG, "call 异常，api=" + apiName + " : " + t);
            Log.printStackTrace(TAG, t);
            return null;
        }
    }

    private static Class<?> ensureJsonCls(ClassLoader loader) {
        if (jsonClsCache != null) return jsonClsCache;
        try {
            Class<?> c = XHelpers.findClass("com.alibaba.fastjson.JSONObject", loader);
            jsonClsCache = c;
            return c;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method ensurePut(Class<?> jsonCls) {
        if (putCache != null) return putCache;
        try {
            Method m = jsonCls.getMethod("put", String.class, Object.class);
            putCache = m;
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object resolveMethodEnum(ClassLoader loader, boolean usePost) {
        try {
            if (methodEnumClsCache == null) {
                methodEnumClsCache = XHelpers.findClass(
                        "mtopsdk.mtop.domain.MethodEnum", loader);
            }
            return XHelpers.getStaticObjectField(
                    methodEnumClsCache, usePost ? "POST" : "GET");
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object parseJson(ClassLoader loader, String json) {
        try {
            if (parseObjectMethodCache == null) {
                Class<?> jsonParserCls = XHelpers.findClass(
                        "com.alibaba.fastjson.JSON", loader);
                parseObjectMethodCache = jsonParserCls.getMethod("parseObject", String.class);
            }
            return parseObjectMethodCache.invoke(null, json);
        } catch (Throwable t) {
            Log.i(TAG, "parseJson 失败: " + t);
            return null;
        }
    }
}
