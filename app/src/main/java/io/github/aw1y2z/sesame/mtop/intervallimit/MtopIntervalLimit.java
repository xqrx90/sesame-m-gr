package io.github.aw1y2z.sesame.mtop.intervallimit;

import io.github.aw1y2z.sesame.rpc.intervallimit.DefaultIntervalLimit;
import io.github.aw1y2z.sesame.rpc.intervallimit.FixedOrRangeIntervalLimit;
import io.github.aw1y2z.sesame.rpc.intervallimit.IntervalLimit;
import io.github.aw1y2z.sesame.util.TimeUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MTOP 接口限流表。
 * 与 {@code RpcIntervalLimit} 结构一致，但独立存储，互不影响。
 * <p>
 * 与 {@code RpcIntervalLimit#addIntervalLimit} 的差异：此处重复注册用 put 覆盖而不抛异常。
 * 原因：MTOP 直调是在业务代码里按需注册的，同一 api 被多个模块重复注册属正常情况，
 * 抛异常会把宿主进程搞崩；限流表本身用覆盖语义也不会有副作用。
 */
public class MtopIntervalLimit {

    private static final IntervalLimit DEFAULT_LIMIT = new DefaultIntervalLimit(50);

    private static final Map<String, IntervalLimit> MTOP_LIMIT_MAP = new ConcurrentHashMap<>();

    public static synchronized void addIntervalLimit(String apiName, Integer interval) {
        MTOP_LIMIT_MAP.put(apiName, new DefaultIntervalLimit(interval));
    }

    public static synchronized void addIntervalLimit(String apiName, String fixedOrRange) {
        MTOP_LIMIT_MAP.put(apiName, new FixedOrRangeIntervalLimit(fixedOrRange, 0, Integer.MAX_VALUE));
    }

    public static synchronized void addIntervalLimit(String apiName, IntervalLimit intervalLimit) {
        MTOP_LIMIT_MAP.put(apiName, intervalLimit);
    }

    public static synchronized void updateIntervalLimit(String apiName, Integer interval) {
        MTOP_LIMIT_MAP.put(apiName, new DefaultIntervalLimit(interval));
    }

    public static synchronized void updateIntervalLimit(String apiName, IntervalLimit intervalLimit) {
        MTOP_LIMIT_MAP.put(apiName, intervalLimit);
    }

    public static synchronized void clearIntervalLimit() {
        MTOP_LIMIT_MAP.clear();
    }

    public static void enterIntervalLimit(String apiName) {
        IntervalLimit intervalLimit = MTOP_LIMIT_MAP.get(apiName);
        if (intervalLimit == null) {
            intervalLimit = DEFAULT_LIMIT;
        }
        synchronized (intervalLimit) {
            long sleep = intervalLimit.getInterval() - System.currentTimeMillis() + intervalLimit.getTime();
            if (sleep > 0) {
                TimeUtil.sleep(sleep);
            }
            intervalLimit.setTime(System.currentTimeMillis());
        }
    }
}
