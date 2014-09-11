#!/usr/bin/env sh

echo "preparing postgresql configs"

psql -c 'create database dbmappersamples;' -U postgres
psql -c "alter database dbmappersamples set timezone to 'GMT'" -U postgres


