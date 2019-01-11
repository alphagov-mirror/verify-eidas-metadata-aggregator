#!/usr/bin/env ruby

require 'aws-sdk-s3'
require 'cgi'
require 'json'

aws_region = ENV['AWS_REGION']
bucket_name = ENV['BUCKET_NAME']
environment = ENV['ENVIRONMENT']

unless aws_region && bucket_name && environment
    abort('Please supply AWS_REGION, BUCKET_NAME, ENVIRONMENT as environment variables')
end

metadata_location = "./src/main/resources/#{environment}/MetadataSourceConfiguration.json"

puts "*"*50
puts "PUSHING METADATA FROM FILE #{metadata_location} TO #{bucket_name} IN #{aws_region}"
puts "*"*50


metadata_json_list = File.read(metadata_location)
country_url_list = JSON.parse(metadata_json_list)
country_list = country_url_list['metadataUrls']

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

country_list.each do |country, url|
    puts "Pushing #{url} to S3 bucket"
    s3_object_key = url.unpack('H*')[0].downcase
    updated_metadata << s3_object_key
    aws_put s3.bucket(bucket_name).object(s3_object_key)
end

s3.bucket(bucket_name).objects.each do |object|
  object.delete if !updated_metadata.include? object.key
end
