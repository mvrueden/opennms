#!/usr/bin/env bash

# Prepare test environment
echo "Preparing Test environment"
psql -U postgres -h localhost -c 'drop database if exists opennms' || exit
psql -U postgres -h localhost -c 'create database opennms' || exit
psql -U postgres -h localhost -tAc "SELECT 1 FROM pg_roles WHERE rolname='opennms'" | grep -q 1 || psql -U postgres -h localhost -c 'create user opennms' || exit

# Tests
echo "Testing"
mvn verify -DupdatePolicy=never -DskipTests=false -DskipITs=false --batch-mode || exit
mvn verify -DupdatePolicy=never -DskipTests=false -DskipITs=false --batch-mode --file opennms-full-assembly/pom.xml || exit
