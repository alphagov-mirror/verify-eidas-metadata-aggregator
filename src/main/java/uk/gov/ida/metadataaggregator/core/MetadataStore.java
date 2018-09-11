package uk.gov.ida.metadataaggregator.core;

import java.util.List;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;

public interface MetadataStore {
    void upload(String name, EntityDescriptor metadata) throws MetadataStoreException;
    EntityDescriptor download(String name) throws MetadataStoreException;
    void delete(String name) throws MetadataStoreException;
    List<String> list() throws MetadataStoreException;
}

