package uk.gov.ida.metadataaggregator.exceptions;

public class MetadataStoreException extends Exception {
    public MetadataStoreException(String s, Throwable e) {
        super(e);
    }

    public MetadataStoreException(String s) {
        super(s);
    }
}
