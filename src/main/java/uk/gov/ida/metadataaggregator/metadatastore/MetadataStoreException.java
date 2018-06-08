package uk.gov.ida.metadataaggregator.metadatastore;

public class MetadataStoreException extends Exception {
    public MetadataStoreException(String s, Throwable e) {
        super(e);
    }

    public MetadataStoreException(String s) {
        super(s);
    }
}
