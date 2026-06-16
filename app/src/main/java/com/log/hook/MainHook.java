package com.log.hook;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    // 日志根目录
    private static final String LOG_ROOT = "/sdcard/GlobalLog/";
    private static SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
    private static SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 跳过系统框架自身，减少冗余日志
        if (lpparam.packageName.equals("android")) return;

        Class<?> logCls = XposedHelpers.findAndClass("android.util.Log", lpparam.classLoader);
        // 拦截全部日志方法
        hookLogMethod(logCls, "v", String.class, String.class, lpparam);
        hookLogMethod(logCls, "d", String.class, String.class, lpparam);
        hookLogMethod(logCls, "i", String.class, String.class, lpparam);
        hookLogMethod(logCls, "w", String.class, String.class, lpparam);
        hookLogMethod(logCls, "e", String.class, String.class, lpparam);
        hookLogMethod(logCls, "wtf", String.class, String.class, lpparam);
    }

    /**
     * 统一Hook Log打印方法
     */
    private void hookLogMethod(Class<?> logCls, String methodName, Class<?> p1, Class<?> p2, XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(logCls, methodName, p1, p2, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String level = methodName.toUpperCase();
                String tag = (String) param.args[0];
                String msg = (String) param.args[1];
                String pkg = lpparam.packageName;
                long time = System.currentTimeMillis();
                String timeStr = sdfTime.format(new Date(time));

                // 拼接日志内容
                String logLine = String.format("[%s][%s][%s] %s: %s\n", timeStr, pkg, level, tag, msg);
                writeLogToFile(logLine);
            }
        });
    }

    /**
     * 写入本地日志文件，按日期分文件
     */
    private void writeLogToFile(String content) {
        try {
            File rootDir = new File(LOG_ROOT);
            if (!rootDir.exists()) rootDir.mkdirs();
            String day = sdfDay.format(new Date());
            File logFile = new File(LOG_ROOT + day + ".log");

            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(logFile, true));
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // 写入失败不阻塞原APP运行
        }
    }
}

