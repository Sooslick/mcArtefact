package ru.sooslick.artefact.outlaw.util;

import ru.sooslick.artefact.ArtefactPlugin;

import java.util.logging.Logger;

/**
 * Utility class for logging
 */
public class LoggerUtil {

    private static final String PREFIX = "[DEBUG] ";
    private static final Logger LOG = ArtefactPlugin.getInstance().getLogger();
    private static final DebugLogger DEBUG_NORMAL = LOG::fine;
    private static final DebugLogger DEBUG_FORCED = (msg) -> LOG.info(PREFIX + msg);

    private static DebugLogger logFunc = DEBUG_NORMAL;

    //disable constructor for Utility class
    private LoggerUtil() {
    }

    /**
     * Define method for debug logging
     */
    public static void setupLevel(boolean debugMode) {
        logFunc = debugMode ? DEBUG_FORCED : DEBUG_NORMAL;
    }

    /**
     * Log string at fine level or fake debug log at info level if debugMode is enabled
     *
     * @param msg string to log
     */
    //just logs message at fine level in normal mode.
    //For plugin's debug mode forces debug messages at info level
    public static void debug(String msg) {
        logFunc.debug(msg);
    }

    /**
     * Log string at normal level
     *
     * @param msg string to log
     */
    public static void info(String msg) {
        LOG.info(msg);
    }

    /**
     * Log string at warning level
     *
     * @param msg string to log
     */
    public static void warn(String msg) {
        LOG.warning(msg);
    }

    @FunctionalInterface
    private interface DebugLogger {
        void debug(String msg);
    }
}
