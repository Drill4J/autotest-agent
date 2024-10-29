package com.epam.drill.autotest.logging

/**
 * Object for logging configuration initialization
 */
expect object LoggingConfiguration {

    /**
     * Read default logging configuration from /logging.properties file for JVM
     * or initialize default configuration for native
     */
    fun readDefaultConfiguration()

    /**
     * Set logging levels per logger for JVM or set global logging level for native
     * (using any of "", "com", "com.epam", "com.epam.drill" logger names)
     *
     * @param  levels
     *         list of level pairs, e.g. ("", "INFO"), ("com.epam.drill", "TRACE")
     */
    fun setLoggingLevels(levels: List<Pair<String, String>>)

    /**
     * Set logging levels per logger for JVM or set global logging level for native
     * (using any of "", "com", "com.epam", "com.epam.drill" logger names)
     *
     * @param  levels
     *         semicolon-separated string of level pairs,
     *         e.g. "INFO" or "=INFO;com.epam.drill=TRACE;something=INFO"
     */
    fun setLoggingLevels(levels: String)

    /**
     * Set output file (or console output) for all loggers
     *
     * @param  filename
     *         filename of log file, if set to 'null' console output will be used
     */
    fun setLoggingFilename(filename: String?)

    /**
     * Get previously configured output file (or 'null' for console output)
     *
     * @return filename of log file or 'null' for console output
     */
    fun getLoggingFilename(): String?

    /**
     * Set length limit for log messages
     *
     * @param  messageLimit
     *         maximum length for log messages
     */
    fun setLogMessageLimit(messageLimit: Int)

    /**
     * Get length limit for log messages
     *
     * @return maximum length for log messages
     */
    fun getLogMessageLimit(): Int

}