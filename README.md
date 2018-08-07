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

## Future Development - DropWizard Application

The eIDAS metadata aggregator is currently undergoing transformation to run as a DropWizard application. This README file will be updated when the changes are completed.

## Support and responsible disclosure

If you think you have discovered a security issue in this code please email [disclosure@digital.cabinet-office.gov.uk](mailto:disclosure@digital.cabinet-office.gov.uk) with details.

For non-security related bugs and feature requests please [raise an issue](https://github.com/alphagov/verify-eidas-metadata-aggregator/issues/new) in the GitHub issue tracker.

## License

[MIT](https://github.com/alphagov/verify-eidas-metadata-aggregator/blob/master/LICENCE)
