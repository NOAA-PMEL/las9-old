#!/bin/sh
cd projects/admin-console
./node_modules/.bin/ng build --configuration production --base-href .
cd ../..
rm -rf src/main/webapps/admin/*
cp -R projects/admin-console/dist/admin-console/* src/main/webapp/admin/.
cp projects/admin-console/dist/admin-console/index.html ./grails-app/views/admin/index.gsp
