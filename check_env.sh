#!/bin/bash

echo "Checking environment variables..."
echo "GOOGLE_OAUTH_CLIENT_ID length: ${#GOOGLE_OAUTH_CLIENT_ID}"
echo "GOOGLE_OAUTH_CLIENT_SECRET length: ${#GOOGLE_OAUTH_CLIENT_SECRET}"

# Print first few characters for identification
echo "GOOGLE_OAUTH_CLIENT_ID starts with: ${GOOGLE_OAUTH_CLIENT_ID:0:10}..."
echo "GOOGLE_OAUTH_CLIENT_SECRET starts with: ${GOOGLE_OAUTH_CLIENT_SECRET:0:10}..."

# Check if there's a mix-up
if [[ $GOOGLE_OAUTH_CLIENT_ID == *"GOCSPX"* ]]; then
  echo "WARNING: GOOGLE_OAUTH_CLIENT_ID contains 'GOCSPX', which is typically found in client secrets, not client IDs"
fi

if [[ $GOOGLE_OAUTH_CLIENT_SECRET != *"GOCSPX"* ]]; then
  echo "WARNING: GOOGLE_OAUTH_CLIENT_SECRET does not contain 'GOCSPX', which is typically found in client secrets"
fi