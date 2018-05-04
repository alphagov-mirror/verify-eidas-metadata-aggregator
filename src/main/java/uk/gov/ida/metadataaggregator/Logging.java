package uk.gov.ida.metadataaggregator;

import java.text.MessageFormat;

class Logging {
    static void log(String message, Throwable e, Object... args) {
        log(message + ": " + e.getMessage(), args);
        e.printStackTrace();
    }

    static void log(String message, Object... args) {
        System.out.println(MessageFormat.format(message, args));
    }
}
