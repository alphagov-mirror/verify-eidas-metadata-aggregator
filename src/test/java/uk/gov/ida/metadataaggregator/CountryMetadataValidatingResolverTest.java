package uk.gov.ida.metadataaggregator;

import com.nimbusds.jose.jwk.JWK;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.impl.SignatureValidationFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.w3c.dom.Element;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.saml.metadata.factories.MetadataResolverFactory;

import javax.inject.Provider;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountryMetadataValidatingResolverTest {

    private CountryMetadataValidatingResolver metadataValidatingResolver;
    private MetadataResolverFactory metadataResolverFactory = mock(MetadataResolverFactory.class);
    private MetadataResolver metadataResolver = mock(MetadataResolver.class);
    private JWK trustAnchor = mock(JWK.class);
    private Map<String, JWK> trustAnchorMap;
    private Element metadata = mock(Element.class);
    private EntityDescriptor entityDescriptor = mock(EntityDescriptor.class);

    @Mock
    private Provider<SignatureValidationFilter> signatureValidationFilterProvider;

    private final String metadataUrl = "http://metadataurl1.com";


    @Before
    public void setUp() {
        trustAnchorMap = new HashMap<String, JWK>();
        metadataValidatingResolver = new CountryMetadataValidatingResolver(trustAnchorMap, metadataResolverFactory, ks -> signatureValidationFilterProvider);
        when(metadataResolverFactory.create(any(), any(), any(), anyLong(), anyLong())).thenReturn(metadataResolver);
        when(trustAnchor.getKeyStore()).thenReturn(mock(KeyStore.class));
        when(entityDescriptor.getDOM()).thenReturn(metadata);
        when(signatureValidationFilterProvider.get()).thenReturn(mock(SignatureValidationFilter.class));
    }

    @Test
    public void shouldThrowWhenMetadataUrlIsNotInTrustAnchor() {
        assertThatThrownBy(() -> metadataValidatingResolver.downloadMetadata(metadataUrl)).isInstanceOf(MetadataSourceException.class);
    }

    @Test
    public void shouldThrowWhenUnableToDownload() throws ResolverException {
        trustAnchorMap.put(metadataUrl, trustAnchor);
        when(metadataResolver.resolveSingle(any())).thenThrow(ResolverException.class);

        assertThatThrownBy(() -> metadataValidatingResolver.downloadMetadata(metadataUrl)).isInstanceOf(MetadataSourceException.class);
    }

    @Test
    public void shouldThrowIfEntityIdIsNotInDownloadedMetadata() throws ResolverException {
        trustAnchorMap.put(metadataUrl, trustAnchor);
        when(metadataResolver.resolveSingle(any())).thenReturn(null);

        assertThatThrownBy(() -> metadataValidatingResolver.downloadMetadata(metadataUrl)).isInstanceOf(MetadataSourceException.class);
    }

    @Test
    public void shouldReturnElementWhenValidEntityDescriptorIsResolved() throws ResolverException, MetadataSourceException {
        trustAnchorMap.put(metadataUrl, trustAnchor);
        when(metadataResolver.resolveSingle(any())).thenReturn(entityDescriptor);

        Element returnedElement = metadataValidatingResolver.downloadMetadata(metadataUrl);
        assertThat(returnedElement).isEqualTo(metadata);
    }
}
