package uk.gov.ida.metadataaggregator.metadatasource;

import com.amazonaws.util.StringInputStream;
import com.google.common.collect.ImmutableList;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;
import uk.gov.ida.saml.metadata.ExpiredCertificateMetadataFilter;
import uk.gov.ida.saml.metadata.PKIXSignatureValidationFilterProvider;
import uk.gov.ida.saml.metadata.factories.MetadataResolverFactory;

import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Functions.identity;

public class CountryMetadataValidatingResolver implements CountryMetadataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryMetadataValidatingResolver.class);
    private static final String JKS = "JKS";

    private final Map<String, JWK> trustAnchors;

    private CountryMetadataValidatingResolver(Map<String, JWK> trustAnchors) {
        this.trustAnchors = trustAnchors;
    }

    public static CountryMetadataValidatingResolver build(AggregatorConfig testObject, String password, String eidasTrustAnchorUriString) throws MetadataSourceException {
        KeyStore trustStore;
        try (InputStream stream = new StringInputStream(testObject.getKeyStore())) {
            trustStore = KeyStore.getInstance(JKS);
            trustStore.load(stream, password.toCharArray());
        } catch (IOException e) {
            throw new MetadataSourceException("Unable to read key store string", e);
        } catch (GeneralSecurityException e) {
            throw new MetadataSourceException("Error building trust store", e);
        }

        EidasTrustAnchorResolver trustAnchorResolver = new EidasTrustAnchorResolver(
                URI.create(eidasTrustAnchorUriString),
                ClientBuilder.newClient(),
                trustStore);

        Map<String, JWK> trustAnchors;
        try {
            trustAnchors =
                    trustAnchorResolver
                            .getTrustAnchors()
                            .stream()
                            .collect(Collectors.toMap(JWK::getKeyID, identity()));
        } catch (GeneralSecurityException | ParseException | JOSEException e) {
            LOGGER.error("Error creating CountryMetadataValidatingResolver. Exception: {} Message: {}", e.getClass(), e.getMessage());
            throw new MetadataSourceException("Error creating CountryMetadataValidatingResolver", e);
        }

        return new CountryMetadataValidatingResolver(trustAnchors);
    }

    @Override
    public String downloadMetadata(String url) throws MetadataSourceException {
        MetadataResolver metadataResolver = metadataResolver(url);

        CriteriaSet criteria = new CriteriaSet();
        criteria.add(new EntityIdCriterion(url));

        EntityDescriptor entityDescriptor;
        try {
            entityDescriptor = metadataResolver.resolveSingle(criteria);
        } catch (ResolverException e) {
            throw new MetadataSourceException("Unable to resolve metadatasource from " + url, e);
        }

        if (entityDescriptor == null || entityDescriptor.getDOM() == null) return null;
        return serialise(entityDescriptor.getDOM());
    }

    private MetadataResolver metadataResolver(String url) {
        PKIXSignatureValidationFilterProvider pkixSignatureValidationFilterProvider =
                new PKIXSignatureValidationFilterProvider(trustAnchors.get(url).getKeyStore());

        List<MetadataFilter> metadataFilters =
                ImmutableList.of(
                        pkixSignatureValidationFilterProvider.get(),
                        new ExpiredCertificateMetadataFilter()
                );

        return new MetadataResolverFactory().create(
                ClientBuilder.newClient(),
                URI.create(url),
                metadataFilters,
                0,
                0
        );
    }

    private String serialise(Element node) {
        Document document = node.getOwnerDocument();
        DOMImplementationLS domImplLS = (DOMImplementationLS) document
                .getImplementation();
        LSSerializer serializer = domImplLS.createLSSerializer();
        return serializer.writeToString(node);
    }
}
