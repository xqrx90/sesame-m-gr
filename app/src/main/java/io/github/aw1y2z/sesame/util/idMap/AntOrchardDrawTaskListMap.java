package io.github.aw1y2z.sesame.util.idMap;

import java.util.Map;

import io.github.aw1y2z.sesame.util.FileUtil;

/**
 * 果园「抽奖任务」清单缓存（任务 id → 任务名）。
 * <p>从 Sesame-GR 移植，写入方式对齐 M 的 {@link StringMapStore} 统一实现。
 */
public class AntOrchardDrawTaskListMap {

    private static final StringMapStore STORE =
            new StringMapStore(ignoredUserId -> FileUtil.getAntOrchardDrawTaskListMapFile());

    public static Map<String, String> getMap() {
        return STORE.getMap();
    }

    public static String get(String key) {
        return STORE.get(key);
    }

    public static void add(String key, String value) {
        STORE.add(key, value);
    }

    public static void remove(String key) {
        STORE.remove(key);
    }

    public static void load() {
        STORE.load(null);
    }

    public static boolean save() {
        return STORE.save(null);
    }

    public static void clear() {
        STORE.clear();
    }

}
