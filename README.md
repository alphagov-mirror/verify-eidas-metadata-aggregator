# Verify eIDAS metadata aggregator
This repository is designed to support a service that can be activated to 
refresh the eIDAS country metadata that is needed for the proxy node of each participating 
country to interact with Verify.  The repository updates the metadata by going to country 
URLs which do not change and which are maintained in a configuration file eg:
```integration-metadata.yml```.

It has three main points of extensibility:
* **configuration source**: which hides details such as whether it's using a local file or 
getting them from a remote location
* **country meta data source**: which hides the means by which metadata are 
accessed (eg: using curl)
* **metadata store**: which hides what kind of storage holds the metadata (eg: S3 bucket, 
local file system)


## Usage

The script takes a yaml file that lists metadata url and uploads the url content to the s3 bucket.

Sample metadata file:
```
eu-country-1: <url>
eu-country-2: <url>
```

In order to upload the objects to the S3 bucket, you need to be authenticated by setting the environment variables 
```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_SESSION_TOKEN
```
