# Verify eIDAS metadata aggregator

This repository contains a script that curls metadata from different EU countries and uploads it to an S3 bucket.

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
