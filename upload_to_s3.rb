#!/usr/bin/env ruby

require 'aws-sdk-s3'
require 'yaml'
require 'cgi'

aws_region = ENV['AWS_REGION']
bucket_name = ENV['BUCKET_NAME']
encryption_algorithm = 'AES256'
metadata_list_file = ENV['METADATA_FILE']

abort('Please supply AWS_REGION, BUCKET_NAME, METADATA_FILE as environment variables') unless aws_re
gion && bucket_name && metadata_list_file

puts "*"*50
puts "PUSHING METADATA FROM FILE #{metadata_list_file} TO #{bucket_name} IN #{aws_region}"
puts "*"*50

metadata_list = YAML.load_file File.join(__dir__, metadata_list_file)
abort("Error: File supplied is not valid YAML") unless metadata_list

s3 = Aws::S3::Resource.new(region: aws_region)
updated_metadata = []

metadata_list.each do |country, url|
  s3_object_key = CGI.escape url
  updated_metadata << s3_object_key
  obj = s3.bucket(bucket_name).object(s3_object_key)
  metadata = `curl -A "Mozilla" --max-time 15 #{url}`
  obj.put(body: metadata, server_side_encryption: encryption_algorithm, acl: 'public-read', content_type: 'application/samlmetadata+xml')
end

s3.bucket(bucket_name).objects.each do |object|
  object.delete if !updated_metadata.include? object.key
end
