package uk.gov.ida.metadataaggregator.configuration;

public class ConfigSourceException extends Exception {
    public ConfigSourceException(String s, Throwable e) {
        super(s, e);
    }

    public ConfigSourceException(String s) {
        super(s);
    }
}
