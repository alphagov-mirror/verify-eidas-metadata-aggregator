package uk.gov.ida.metadataaggregator.metadatasource;

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

import ch.qos.logback.core.util.Duration;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;
import uk.gov.ida.saml.metadata.ExpiredCertificateMetadataFilter;
import uk.gov.ida.saml.metadata.PKIXSignatureValidationFilterProvider;
import uk.gov.ida.saml.metadata.factories.MetadataResolverFactory;

import javax.inject.Provider;
import javax.ws.rs.client.ClientBuilder;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Functions.identity;

public class CountryMetadataResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryMetadataResolver.class);

    // We don't want to refresh ever, but cannot specify zero milliseconds as it can lead to negative durations
    // and also can't specify Long.MAX_VALUE because it'll give bad values when subtracted. So just specify a long time.
    private static final Long REFRESH_DELAY = Duration.buildByDays(365).getMilliseconds();

    private final Map<String, JWK> trustAnchors;
    private ClientBuilder clientBuilder;

    public CountryMetadataResolver(Map<String, JWK> trustAnchors, ClientBuilder clientBuilder) {
        this.trustAnchors = trustAnchors;
        this.clientBuilder = clientBuilder;
    }

    public static CountryMetadataResolver fromTrustAnchor(EidasTrustAnchorResolver trustAnchorResolver) throws MetadataSourceException {
        Map<String, JWK> trustAnchors;
        try {
            trustAnchors =
                trustAnchorResolver
                    .getTrustAnchors()
                    .stream()
                    .collect(Collectors.toMap(JWK::getKeyID, identity()));
        } catch (GeneralSecurityException | ParseException | JOSEException e) {
            LOGGER.error("Error creating CountryMetadataResolver", e);
            throw new MetadataSourceException("Error creating CountryMetadataResolver", e);
        }

        return new CountryMetadataResolver(trustAnchors, ClientBuilder.newBuilder());
    }

    public EntityDescriptor downloadMetadata(URL url) throws MetadataSourceException {
        MetadataResolver metadataResolver;
        try {
            metadataResolver = metadataResolver(url);
        } catch (URISyntaxException e) {
            throw new MetadataSourceException("Metadata URL is invalid", e);
        }

        CriteriaSet criteria = new CriteriaSet();
        criteria.add(new EntityIdCriterion(url.toString()));

        EntityDescriptor entityDescriptor;
        try {
            entityDescriptor = metadataResolver.resolveSingle(criteria);
        } catch (ResolverException e) {
            throw new MetadataSourceException("Unable to resolve metadatasource from " + url, e);
        }

        if (entityDescriptor == null) {
            throw new MetadataSourceException(String.format("Entity Descriptor: %s could not be found in metadata file", url));
        }

        return entityDescriptor;
    }

    private MetadataResolver metadataResolver(URL url) throws MetadataSourceException, URISyntaxException {

        if (trustAnchors.get(url.toString()) == null) {
            throw new MetadataSourceException(String.format("Trust Anchor doesn't contain: %s", url));
        }

        Provider<SignatureValidationFilter> pkixSignatureValidationFilterProvider =
            new PKIXSignatureValidationFilterProvider(trustAnchors.get(url.toString()).getKeyStore());

        List<MetadataFilter> metadataFilters =
            ImmutableList.of(
                pkixSignatureValidationFilterProvider.get(),
                new ExpiredCertificateMetadataFilter()
            );

        return new MetadataResolverFactory().create(
            clientBuilder.build(),
            url.toURI(),
            metadataFilters,
            REFRESH_DELAY,
            REFRESH_DELAY
        );
    }
}
