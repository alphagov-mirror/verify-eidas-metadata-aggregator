package uk.gov.ida.metadataaggregator;

import certificates.values.CACertificates;
import com.google.common.base.Throwables;
import com.nimbusds.jose.jwk.JWK;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.w3c.dom.Element;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.saml.core.test.PemCertificateStrings;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.IdpSsoDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.KeyInfoBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.SignatureBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.X509CertificateBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.X509DataBuilder;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;
import uk.gov.ida.saml.metadata.test.factories.metadata.TestCredentialFactory;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
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
    private final EntityDescriptor STUB_COUNTRY_ONE_METADATA = idpEntityDescriptor(TestEntityIds.STUB_COUNTRY_ONE);
    private final EntityDescriptor STUB_COUNTRY_TWO_METADATA = idpEntityDescriptor(TestEntityIds.STUB_COUNTRY_TWO);

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

    private static KeyStore loadKeyStore(String... certificates) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        for (String certificate : certificates) {
            Certificate cert = certificateFactory.generateCertificate(IOUtils.toInputStream(certificate));
            keyStore.setEntry(cert.toString(), new KeyStore.TrustedCertificateEntry(cert), new KeyStore.PasswordProtection(null));
        }
        return keyStore;
    }

    public EntityDescriptor idpEntityDescriptor(String idpEntityId) {
        KeyDescriptor keyDescriptor = buildKeyDescriptor(idpEntityId);
        IDPSSODescriptor idpssoDescriptor = IdpSsoDescriptorBuilder.anIdpSsoDescriptor().addKeyDescriptor(keyDescriptor).withoutDefaultSigningKey().build();
        TestCredentialFactory idpCredentialFactory = new TestCredentialFactory(TestCertificateStrings.STUB_COUNTRY_PUBLIC_PRIMARY_CERT, TestCertificateStrings.STUB_COUNTRY_PUBLIC_PRIMARY_PRIVATE_KEY);
        Signature s = SignatureBuilder.aSignature().withX509Data(TestCertificateStrings.STUB_COUNTRY_PUBLIC_PRIMARY_CERT).withSigningCredential(idpCredentialFactory.getSigningCredential()).build();
        try {
            return sign(EntityDescriptorBuilder.anEntityDescriptor()
                    .withEntityId(idpEntityId)
                    .withIdpSsoDescriptor(idpssoDescriptor)
                    .withValidUntil(DateTime.now().plusWeeks(2))
                    .setAddDefaultSpServiceDescriptor(false)
                    .build(), s);
        } catch (MarshallingException | SignatureException | SecurityException e) {
            throw Throwables.propagate(e);
        }
    }


    private KeyDescriptor buildKeyDescriptor(String entityId) {
        String certificate = TestCertificateStrings.PUBLIC_SIGNING_CERTS.get(entityId);
        X509Certificate x509Certificate = X509CertificateBuilder.aX509Certificate().withCert(certificate).build();
        X509Data build = X509DataBuilder.aX509Data().withX509Certificate(x509Certificate).build();
        KeyInfo signing_one = KeyInfoBuilder.aKeyInfo().withKeyName("signing_one").withX509Data(build).build();
        return KeyDescriptorBuilder.aKeyDescriptor().withKeyInfo(signing_one).build();
    }

    public <T extends SignableSAMLObject> T sign(T signableSAMLObject, Signature signature) throws MarshallingException, SignatureException, SecurityException {
        signableSAMLObject.setSignature(signature);
        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(signableSAMLObject).marshall(signableSAMLObject);
        Signer.signObject(signableSAMLObject.getSignature());

        return signableSAMLObject;
    }
}
