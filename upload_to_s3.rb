#!/usr/bin/env ruby

require 'aws-sdk-s3'
require 'yaml'
require 'cgi'

aws_region = ENV['AWS_REGION']
bucket_name = ENV['BUCKET_NAME']
metadata_list_file = ENV['METADATA_FILE']

abort('Please supply AWS_REGION, BUCKET_NAME, METADATA_FILE as environment variables') unless aws_region && bucket_name && metadata_list_file

puts "*"*50
puts "PUSHING METADATA FROM FILE #{metadata_list_file} TO #{bucket_name} IN #{aws_region}"
puts "*"*50

metadata_list = YAML.load_file File.join(__dir__, metadata_list_file)
abort("Error: File supplied is not valid YAML") unless metadata_list

def metadata_content url, tls_cert
  puts "USING TLS CERT: #{tls_cert}" if tls_cert
  optional_tls_cert = "--cacert #{tls_cert}" if tls_cert
  `curl #{optional_tls_cert} -A "Mozilla" --max-time 15 #{url}`
end

def aws_put object, content
  object.put(
    body: content,
    server_side_encryption: 'AES256',
    acl: 'public-read',
    content_type: 'application/samlmetadata+xml'
  )
end

s3 = Aws::S3::Resource.new(region: aws_region)
updated_metadata = []

metadata_list.each do |country, metadata|
  url = metadata['url']
  s3_object_key = url.unpack('H*')[0].downcase
  updated_metadata << s3_object_key
  aws_put s3.bucket(bucket_name).object(s3_object_key), metadata_content(url, metadata['tls_cert'])
end

s3.bucket(bucket_name).objects.each do |object|
  object.delete if !updated_metadata.include? object.key
end
