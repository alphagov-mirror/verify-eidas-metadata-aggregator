package uk.gov.ida.metadataaggregator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.PKIXParametersProvider;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;
import uk.gov.ida.saml.metadata.ExpiredCertificateMetadataFilter;
import uk.gov.ida.saml.metadata.PKIXSignatureValidationFilterProvider;

import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.security.KeyStore;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Functions.identity;

public class CountryMetadataValidatingResolver implements CountryMetadataSource {
    private final Map<String, JWK> trustAnchors;

    public CountryMetadataValidatingResolver(KeyStore trustStore) throws CertificateException, SignatureException, ParseException, JOSEException {
        trustAnchors = new EidasTrustAnchorResolver(URI.create("value"), ClientBuilder.newClient(), trustStore)
                .getTrustAnchors().stream()
                .collect(Collectors.toMap(JWK::getKeyID, identity()));
    }

    @Override
    public String downloadMetadata(String url) {
        JWK trustAnchor = trustAnchors.get(url);

        KeyStore metadataTrustStore = trustAnchor.getKeyStore();
        PKIXSignatureValidationFilterProvider pkixSignatureValidationFilterProvider = new PKIXSignatureValidationFilterProvider(metadataTrustStore);

        List<MetadataFilter> metadataFilters = new ArrayList();
        metadataFilters.add(pkixSignatureValidationFilterProvider.get());
        metadataFilters.add(new ExpiredCertificateMetadataFilter());

        CertificateChainValidator certificateChainValidator = new CertificateChainValidator(new PKIXParametersProvider(), new X509CertificateFactory());


        return null;
    }
}
