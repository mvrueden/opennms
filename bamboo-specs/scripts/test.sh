#!/usr/bin/env bash

# Prepare test environment
echo "Preparing Test environment"
psql -U postgres -h localhost -c 'drop database if exists opennms' || exit
psql -U postgres -h localhost -c 'create database opennms' || exit

# Tests
echo "Testing"
mvn verify -DupdatePolicy=never -DskipTests=false -DskipITs=false --batch-mode || exit
mvn verify -DupdatePolicy=never -DskipTests=false -DskipITs=false --batch-mode --file opennms-full-assembly/pom.xml || exit
