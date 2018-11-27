package org.slf4j.impl;

/**
 * Represents object that will receive logging confiugration changes.
 */
public interface ZorkaLoggerInput {

    String getName();

    void setLogLevel(int logLevel);

    void setTrapper(ZorkaTrapper trapper);
}
