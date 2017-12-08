package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    public static String REQUESTED_API_VERSION = "1.6.99";

    private static final String LOGGER_FACTORY_CLASS_NAME = ZorkaLoggerFactory.class.getName();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return ZorkaLoggerFactory.getInstance();
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return LOGGER_FACTORY_CLASS_NAME;
    }
}
