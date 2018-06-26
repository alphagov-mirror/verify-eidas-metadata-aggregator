package uk.gov.ida.metadataaggregator;

import certificates.values.CACertificates;
import com.nimbusds.jose.jwk.JWK;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.credential.Credential;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.saml.core.test.PemCertificateStrings;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.metadata.KeyStoreLoader;
import uk.gov.ida.saml.metadata.test.factories.metadata.EntityDescriptorFactory;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;
import uk.gov.ida.saml.metadata.test.factories.metadata.TestCredentialFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountryMetadataValidatingResolverTest {

    private CountryMetadataValidatingResolver metadataValidatingResolver;
    private JWK trustAnchor = mock(JWK.class);
    private Map<String, JWK> trustAnchorMap;

    private MetadataFactory metadataFactory = new MetadataFactory();
    private ClientBuilder clientBuilder = mock(ClientBuilder.class);
    private Client client = mock(Client.class);

    private URL STUB_COUNTRY_ONE_METADATA_LOCATION;
    private final Credential STUB_COUNTRY_ONE_CREDENTIAL = new TestCredentialFactory(
        TestCertificateStrings.STUB_COUNTRY_PUBLIC_PRIMARY_CERT,
        TestCertificateStrings.STUB_COUNTRY_PUBLIC_PRIMARY_PRIVATE_KEY).getSigningCredential();
    private final Credential STUB_COUNTRY_TWO_CREDENTIAL = new TestCredentialFactory(
        TestCertificateStrings.STUB_COUNTRY_PUBLIC_SECONDARY_CERT,
        TestCertificateStrings.STUB_COUNTRY_PUBLIC_SECONDARY_PRIVATE_KEY).getSigningCredential();
    private final EntityDescriptor STUB_COUNTRY_ONE_METADATA = new EntityDescriptorFactory().signedIdpEntityDescriptor(TestEntityIds.STUB_COUNTRY_ONE, STUB_COUNTRY_ONE_CREDENTIAL);
    private final EntityDescriptor STUB_COUNTRY_TWO_METADATA = new EntityDescriptorFactory().signedIdpEntityDescriptor(TestEntityIds.STUB_COUNTRY_TWO, STUB_COUNTRY_TWO_CREDENTIAL);

    @BeforeClass
    public static void classSetUp() throws InitializationException {
        InitializationService.initialize();
    }

    @Before
    public void setUp() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        STUB_COUNTRY_ONE_METADATA_LOCATION  = new URL(TestEntityIds.STUB_COUNTRY_ONE);

        trustAnchorMap = new HashMap<String, JWK>();
        metadataValidatingResolver = new CountryMetadataValidatingResolver(trustAnchorMap, clientBuilder);
        when(trustAnchor.getKeyStore()).thenReturn(loadKeyStore(CACertificates.TEST_ROOT_CA, CACertificates.TEST_METADATA_CA, PemCertificateStrings.STUB_COUNTRY_PUBLIC_SIGNING_CERT));
        when(clientBuilder.build()).thenReturn(client);
    }

    private OngoingStubbing<String> whenRequest(URL metadataUri) throws URISyntaxException {
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder invocationBuilder = mock(Invocation.Builder.class);
        when(client.target(metadataUri.toURI())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(invocationBuilder);
        return when(invocationBuilder.get(String.class));
    }

    @Test
    public void shouldThrowWhenMetadataUrlIsNotInTrustAnchor() throws URISyntaxException {
        whenRequest(STUB_COUNTRY_ONE_METADATA_LOCATION).thenReturn(metadataFactory.singleEntityMetadata(STUB_COUNTRY_ONE_METADATA));
        assertThatThrownBy(() -> metadataValidatingResolver.downloadMetadata(STUB_COUNTRY_ONE_METADATA_LOCATION)).isInstanceOf(MetadataSourceException.class);
    }

    @Test
    public void shouldThrowWhenUnableToDownload() throws URISyntaxException {
        trustAnchorMap.put(STUB_COUNTRY_ONE_METADATA_LOCATION.toString(), trustAnchor);
        whenRequest(STUB_COUNTRY_ONE_METADATA_LOCATION).thenThrow(WebApplicationException.class);

        assertThatThrownBy(() -> metadataValidatingResolver.downloadMetadata(STUB_COUNTRY_ONE_METADATA_LOCATION)).isInstanceOf(MetadataSourceException.class);
    }

    @Test
    public void shouldThrowIfEntityIdIsNotInDownloadedMetadata() throws URISyntaxException {
        trustAnchorMap.put(STUB_COUNTRY_ONE_METADATA_LOCATION.toString(), trustAnchor);
        whenRequest(STUB_COUNTRY_ONE_METADATA_LOCATION).thenReturn(metadataFactory.singleEntityMetadata(STUB_COUNTRY_TWO_METADATA));

        assertThatThrownBy(() -> metadataValidatingResolver.downloadMetadata(STUB_COUNTRY_ONE_METADATA_LOCATION)).isInstanceOf(MetadataSourceException.class);
    }

    @Test
    public void shouldReturnElementWhenValidEntityDescriptorIsResolved() throws MetadataSourceException, URISyntaxException {
        trustAnchorMap.put(STUB_COUNTRY_ONE_METADATA_LOCATION.toString(), trustAnchor);
        whenRequest(STUB_COUNTRY_ONE_METADATA_LOCATION).thenReturn(metadataFactory.singleEntityMetadata(STUB_COUNTRY_ONE_METADATA));
        EntityDescriptor returnedElement = metadataValidatingResolver.downloadMetadata(STUB_COUNTRY_ONE_METADATA_LOCATION);

        assertThat(returnedElement.getEntityID()).isEqualTo(STUB_COUNTRY_ONE_METADATA.getEntityID());
        assertThat(returnedElement.getSignature().getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0).getValue())
                .isEqualTo(TestCertificateStrings.STUB_COUNTRY_PUBLIC_PRIMARY_CERT);
    }

    private static KeyStore loadKeyStore(String... certificateStrings) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        KeyStoreLoader keyStoreLoader = new KeyStoreLoader();
        List<Certificate> certificates = new ArrayList<>(certificateStrings.length);

        for (String certificateString : certificateStrings) {
            certificates.add(certificateFactory.generateCertificate(IOUtils.toInputStream(certificateString)));
        }

        return keyStoreLoader.load(certificates);
    }
}
