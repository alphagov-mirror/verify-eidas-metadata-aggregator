package uk.gov.ida.metadataaggregator;

import java.text.MessageFormat;

public class Logging {
    public static void log(String message, Throwable e, Object... args) {
        log(message + ": " + e.getMessage(), args);
        e.printStackTrace();
    }

    static void log(String message, Object... args) {
        System.out.println(MessageFormat.format(message, args));
    }
}
