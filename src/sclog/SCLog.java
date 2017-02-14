/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Single Class Logger without compile time dependency.
 * 
 * Dynamic detect log4j, commons-logging and java.util.logging. Use
 * <code>-Dorg.ellab.sclog.log=log4j,commons,java</code> to define the preferred logging framework.
 * <p>
 * WARNING: This class is not designed for performance. Used for testing / simple application only.
 * </p>
 * 
 * @author http://twitter.com/angusdev
 * @version 1.0
 */
public class SCLog {
    private static SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String[] LEVEL = { "fatal", "error", "warn", "info", "debug" };

    private static Class<?> logClass;
    private static String logType;
    // [0] log(String)
    // [1] log(Object)
    // [2] log(String, Throwable)
    // [3] log(Object, Throwable)
    private static Method[][] logMethod = new Method[5][];
    private static Method createMethod;

    private Object log;

    private SCLog(Object log) {
        this.log = log;
    }

    public static SCLog getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static SCLog getLogger(String name) {
        String preferredLog = System.getProperty("org.ellab.sclog.log");

        if ("log4j".equals(logType) || "log4j".equals(preferredLog) || preferredLog == null
                || preferredLog.trim().length() == 0) {
            try {
                if (logType == null) {
                    final String[] level = { "fatal", "error", "warn", "info", "debug" };
                    logClass = Class.forName("org.apache.log4j.Category");
                    createMethod = logClass.getMethod("getInstance", String.class);
                    for (int i = 0; i < level.length; i++) {
                        logMethod[i] = new Method[4];
                        logMethod[i][1] = logClass.getMethod(level[i], Object.class);
                        logMethod[i][3] = logClass.getMethod(level[i], Object.class, Throwable.class);
                    }
                    logType = "log4j";
                }
                return new SCLog(createMethod.invoke(null, name));
            }
            catch (Exception ex) {
                ;
            }
        }
        if ("commons".equals(logType) || "commons".equals(preferredLog) || "commons-logging".equals(preferredLog)
                || preferredLog == null || preferredLog.trim().length() == 0) {
            try {
                if (logType == null) {
                    final String[] level = { "fatal", "error", "warn", "info", "debug" };
                    logClass = Class.forName("org.apache.commons.logging.LogFactory");
                    createMethod = logClass.getMethod("getLog", String.class);
                    Class clazz2 = Class.forName("org.apache.commons.logging.Log");
                    for (int i = 0; i < level.length; i++) {
                        logMethod[i] = new Method[4];
                        logMethod[i][1] = clazz2.getMethod(level[i], Object.class);
                        logMethod[i][3] = clazz2.getMethod(level[i], Object.class, Throwable.class);
                    }
                    logType = "commons-logging";
                }
                return new SCLog(createMethod.invoke(null, name));
            }
            catch (Exception ex) {
                ;
            }
        }
        if ("java".equals(logType) || "java".equals(preferredLog) || preferredLog == null
                || preferredLog.trim().length() == 0) {
            try {
                if (logType == null) {
                    final String[] level = { "severe", "severe", "warning", "info", "fine" };
                    logClass = Class.forName("java.util.logging.Logger");
                    createMethod = logClass.getMethod("getLogger", String.class);
                    for (int i = 0; i < level.length; i++) {
                        logMethod[i] = new Method[4];
                        logMethod[i][0] = logClass.getMethod(level[i], String.class);
                    }
                    logType = "java";
                }
                return new SCLog(createMethod.invoke(null, name));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new SCLog(null);
    }

    private void log(int level, Object obj) {
        try {
            if (logType != null && log != null && logMethod != null) {
                if (logMethod[level][1] != null) {
                    logMethod[level][1].invoke(log, obj);
                }
                else {
                    logMethod[level][0].invoke(log, obj != null ? obj.toString() : obj);
                }
                return;
            }
        }
        catch (Exception ex) {
            ;
        }

        System.out.println(datefmt.format(new Date(System.currentTimeMillis())) + " [" + LEVEL[level] + "] " + obj);
    }

    private void log(int level, Object obj, Throwable throwable) {
        try {
            if (logType != null && log != null && logMethod != null) {
                if (logMethod[level][3] != null) {
                    logMethod[level][3].invoke(log, obj, throwable);
                }
                else if (logMethod[level][2] != null) {
                    logMethod[level][2].invoke(log, obj != null ? obj.toString() : obj, throwable);
                }
                else {
                    log(level,
                            (obj != null ? obj.toString() : "")
                                    + (throwable != null ? (" (" + throwable.getClass().getName() + " "
                                            + throwable.getMessage() + ")") : ""));
                }
                return;
            }
        }
        catch (Exception ex) {
            ;
        }

        System.out.println(datefmt.format(new Date(System.currentTimeMillis())) + " [" + LEVEL[level] + "] " + obj);
        throwable.printStackTrace(System.out);
    }

    public void fatal(Object obj) {
        log(0, obj);
    }

    public void fatal(Object obj, Throwable throwable) {
        log(0, obj, throwable);
    }

    public void error(Object obj) {
        log(1, obj);
    }

    public void error(Object obj, Throwable throwable) {
        log(1, obj, throwable);
    }

    public void warn(Object obj) {
        log(2, obj);
    }

    public void warn(Object obj, Throwable throwable) {
        log(2, obj, throwable);
    }

    public void info(Object obj) {
        log(3, obj);
    }

    public void info(Object obj, Throwable throwable) {
        log(3, obj, throwable);
    }

    public void debug(Object obj) {
        log(4, obj);
    }

    public void debug(Object obj, Throwable throwable) {
        log(4, obj, throwable);
    }

    public static void main(String[] args) throws Exception {
        SCLog log = SCLog.getLogger(SCLog.class);
        log.fatal(logType + " fatal");
        log.fatal(logType + " fatal", new Exception("fatal exception"));
        log.error(logType + " error");
        log.error(logType + " error", new Exception("error exception"));
        log.warn(logType + " warn");
        log.warn(logType + " warn", new Exception("warn exception"));
        log.info(logType + " info");
        log.info(logType + " info", new Exception("info exception"));
        log.debug(logType + " debug");
        log.debug(logType + " debug", new Exception("debug exception"));
    }
}
