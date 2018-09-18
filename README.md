# Verify eIDAS metadata aggregator
This repository is designed to support a service that can be activated to 
refresh the eIDAS country metadata that is needed for the proxy node of each participating 
country to interact with Verify.  The repository updates the metadata by going to country 
URLs which do not change and which are maintained in a configuration file eg:
```integration-metadata.yml```.

## Current Implementation - Ruby Script

It has three main points of extensibility:
* **configuration source**: which hides details such as whether it's using a local file or 
getting them from a remote location
* **country meta data source**: which hides the means by which metadata are 
accessed (eg: using curl)
* **metadata store**: which hides what kind of storage holds the metadata (eg: S3 bucket, 
local file system)


### Usage

The `upload_to_s3.rb` script takes a yaml file that lists metadata url and uploads the url content to the s3 bucket.

Sample metadata file:
```
country-1: <url>
country-2: <url>
```

In order to upload the objects to the S3 bucket, you need to be authenticated by setting the environment variables 
```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_SESSION_TOKEN
```

## DropWizard Application

The eIDAS metadata aggregator is currently undergoing transformation to run as a DropWizard application. This README file will be updated when the changes are completed.

### Building and running

To build for use run:
```
./gradlew clean build installDist
```

Then to run it:
```
./build/install/verify-eidas-metadata-aggregator/bin/verify-eidas-metadata-aggregator server configuration/metadata-aggregator.yml
```

The configuration file reads some environment variables. Depending on your environment you could also need to set, as an example:
```
export TRUST_ANCHOR_URI="https://verify-joint-metadata.cloudapps.digital/trust-anchor.jws"
export TRUSTSTORE="../ida-hub-acceptance-tests/truststores/ida_truststore.ts"
export LOG_PATH="/tmp"`cat ../ida-hub-acceptance-tests/configuration/test-rp-msa.yml | grep trustStorePassword | awk '{ print $2; }'`
export HOURS_BETWEEN_EACH_RUN=1
export ENVIRONMENT="joint"
export AWS_REGION="eu-west-2"
```

To access the results of aggregations and reconciliation, in a browser visit http://localhost:51201/healthcheck.

## Support and responsible disclosure

If you think you have discovered a security issue in this code please email [disclosure@digital.cabinet-office.gov.uk](mailto:disclosure@digital.cabinet-office.gov.uk) with details.

For non-security related bugs and feature requests please [raise an issue](https://github.com/alphagov/verify-eidas-metadata-aggregator/issues/new) in the GitHub issue tracker.

## License

[MIT](https://github.com/alphagov/verify-eidas-metadata-aggregator/blob/master/LICENCE)
