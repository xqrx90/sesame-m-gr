package io.github.aw1y2z.sesame.mtop.bridge;

import io.github.aw1y2z.sesame.util.Log;
import io.github.aw1y2z.sesame.util.XHelpers;

/**
 * 主动获取 mtop 服务实例。
 * 走框架服务查询（LauncherApplicationAgent → MicroApplicationContext → findServiceByInterface），
 * 不依赖任何既有请求，因此可以在任意时机调用。
 * <p>
 * 双检锁保证只解析一次；失败会打日志并返回 null（调用方自行处理）。
 */
public class MtopHook {
    private static final String TAG = "MtopHook";
    private static volatile Object instance;

    public static Object resolveInstance(ClassLoader loader) {
        Object local = instance;
        if (local != null) return local;
        synchronized (MtopHook.class) {
            if (instance != null) return instance;
            try {
                Class<?> laa = XHelpers.findClass(
                        "com.alipay.mobile.framework.LauncherApplicationAgent", loader);
                Object app = XHelpers.callStaticMethod(laa, "getInstance");
                Object ctx = XHelpers.callMethod(app, "getMicroApplicationContext");
                Object svc = XHelpers.callMethod(ctx, "findServiceByInterface",
                        "com.alipay.android.phone.mobilesdk.mtop.MtopService");
                if (svc != null) {
                    instance = svc;
                } else {
                    Log.i(TAG, "MtopService 为 null（findServiceByInterface 未返回）");
                }
                return svc;
            } catch (Throwable t) {
                Log.i(TAG, "resolveInstance 失败: " + t);
                Log.printStackTrace(TAG, t);
                return null;
            }
        }
    }
}
