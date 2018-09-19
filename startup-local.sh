#!/bin/bash

export TRUST_ANCHOR_URI="https://verify-joint-metadata.cloudapps.digital/trust-anchor.jws"
export TRUSTSTORE="../ida-hub-acceptance-tests/truststores/ida_truststore.ts"
export LOG_PATH="/tmp"
export TRUSTSTORE_PASSWORD=`cat ../ida-hub-acceptance-tests/configuration/test-rp-msa.yml | grep trustStorePassword | awk '{ print $2; }'`
export HOURS_BETWEEN_EACH_RUN=1
export ENVIRONMENT="joint"
export AWS_REGION="eu-west-2"

./gradlew clean build installDist

./build/install/verify-eidas-metadata-aggregator/bin/verify-eidas-metadata-aggregator server configuration/metadata-aggregator.yml
