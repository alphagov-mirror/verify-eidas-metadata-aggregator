package uk.gov.ida.metadataaggregator.core;

import static uk.gov.ida.metadataaggregator.util.HexUtils.decodeString;
import static uk.gov.ida.metadataaggregator.util.HexUtils.encodeString;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;

public class HexEncodedMetadataStore implements MetadataStore {
  private final MetadataStore downstreamStore;

  public HexEncodedMetadataStore(MetadataStore downstreamStore) {
    this.downstreamStore = downstreamStore;
  }

  @Override
  public void upload(String name, EntityDescriptor metadata) throws MetadataStoreException {
    downstreamStore.upload(encodeString(name), metadata);
  }

  @Override
  public void delete(String name) throws MetadataStoreException {
    downstreamStore.delete(encodeString(name));
	}

	@Override
	public List<String> list() throws MetadataStoreException {
		try {
      List<String> hexList = downstreamStore.list();
      List<String> decodedList = new ArrayList<>(hexList.size());
      for (String hexString : hexList) {
        decodedList.add(decodeString(hexString));
      }
      return decodedList;
    } catch (DecoderException e) {
      throw new MetadataStoreException("Could not decode bucket filename", e);
    }
	}

}