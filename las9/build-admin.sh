#!/bin/sh
cd projects/admin-console
ng build --base-href .
cd ../..
rm -rf src/main/webapps/admin/*
cp -R projects/admin-console/dist/admin-console/* src/main/webapp/admin/.
