#!/usr/bin/env bash

echo -e "Running staging script...\n"
echo -e "Publishing javadocs and artifacts...\n"
cd $HOME

rsync -r --quiet -e "ssh -p 2222 -o StrictHostKeyChecking=no" \
$HOME/work/internetpolice-eu/BungeeAdminTools/target/mvn-repo/ \
travis@travis.internetpolice.eu:WWW/repo/

echo -e "Publishing javadocs...\n"

rsync -r --delete --quiet -e "ssh -p 2222 -o StrictHostKeyChecking=no" \
$HOME/work/internetpolice-eu/BungeeAdminTools/target/site/apidocs/ \
travis@travis.internetpolice.eu:WWW/javadocs/BungeeAdminTools/master/
