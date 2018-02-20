#!/usr/bin/env ruby

require 'aws-sdk-s3'
require 'yaml'

bucket_name = 'govukverify-eidas-metadata-aggregator-dev'
encryption_algorithm = 'AES256'

s3 = Aws::S3::Resource.new(region: 'eu-west-1')

abort("Error: No metadata YAML file supplied") unless ARGV[0]

metadata = YAML.load_file ARGV[0]

abort("Error: File supplied is not valid YAML") unless metadata

metadata.each do |country, url|
  obj = s3.bucket(bucket_name).object(country)
  obj.put(body: url, server_side_encryption: encryption_algorithm)
end

