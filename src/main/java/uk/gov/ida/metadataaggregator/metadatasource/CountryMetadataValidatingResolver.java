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
import org.opensaml.saml.metadata.resolver.filter.impl.SignatureValidationFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;
import uk.gov.ida.saml.metadata.ExpiredCertificateMetadataFilter;
import uk.gov.ida.saml.metadata.PKIXSignatureValidationFilterProvider;
import uk.gov.ida.saml.metadata.factories.MetadataResolverFactory;

import javax.inject.Provider;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Functions.identity;

public class CountryMetadataValidatingResolver implements CountryMetadataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryMetadataValidatingResolver.class);
    private static final String JKS = "JKS";

    private final Map<String, JWK> trustAnchors;
    private MetadataResolverFactory metadataResolverFactory;
    private final Function<KeyStore, Provider<SignatureValidationFilter>> stringToProvider;

    public CountryMetadataValidatingResolver(Map<String, JWK> trustAnchors, MetadataResolverFactory metadataResolverFactory, Function<KeyStore, Provider<SignatureValidationFilter>> stringToProvider) {
        this.trustAnchors = trustAnchors;
        this.metadataResolverFactory = metadataResolverFactory;
        this.stringToProvider = stringToProvider;
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

        return new CountryMetadataValidatingResolver(trustAnchors, new MetadataResolverFactory(), PKIXSignatureValidationFilterProvider::new);
    }

    @Override
    public Element downloadMetadata(String url) throws MetadataSourceException {
        MetadataResolver metadataResolver = metadataResolver(url);

        CriteriaSet criteria = new CriteriaSet();
        criteria.add(new EntityIdCriterion(url));

        EntityDescriptor entityDescriptor;
        try {
            entityDescriptor = metadataResolver.resolveSingle(criteria);
        } catch (ResolverException e) {
            throw new MetadataSourceException("Unable to resolve metadatasource from " + url, e);
        }

        if (entityDescriptor == null || entityDescriptor.getDOM() == null){
            throw new MetadataSourceException(String.format("Entity Descriptor: {} could not be found in metadata file", url));
        }

        return entityDescriptor.getDOM();
    }

    private MetadataResolver metadataResolver(String url) throws MetadataSourceException {

        if (trustAnchors.get(url) == null){
            throw new MetadataSourceException(String.format("Trust Anchor doesn't contain: {}", url));
        }

        Provider<SignatureValidationFilter> pkixSignatureValidationFilterProvider =
                stringToProvider.apply(trustAnchors.get(url).getKeyStore());

        List<MetadataFilter> metadataFilters =
                ImmutableList.of(
                        pkixSignatureValidationFilterProvider.get(),
                        new ExpiredCertificateMetadataFilter()
                );

        return  metadataResolverFactory.create(
                ClientBuilder.newClient(),
                URI.create(url),
                metadataFilters,
                0,
                0
        );
    }
}
