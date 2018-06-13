package uk.gov.ida.metadataaggregator.metadatasource;

public class MetadataSourceException extends Exception {
    public MetadataSourceException(String s, Throwable e) {
        super(s, e);
    }

    public MetadataSourceException(String s) {
        super(s);
    }
}
