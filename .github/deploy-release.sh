#!/usr/bin/env bash

echo -e "Running release script...\n"
echo -e "Publishing javadocs and artifacts...\n"
cd $HOME

rsync -r --quiet -e "ssh -p 2222 -o StrictHostKeyChecking=no" \
$HOME/work/BungeeAdminTools/BungeeAdminTools/target/mvn-repo/ \
travis@travis.internetpolice.eu:WWW/repo/

echo -e "Publishing javadocs...\n"

rsync -r --delete --quiet -e "ssh -p 2222 -o StrictHostKeyChecking=no" \
$HOME/work/BungeeAdminTools/BungeeAdminTools/target/site/apidocs/ \
travis@travis.internetpolice.eu:WWW/javadocs/BungeeAdminTools/release/

echo -e "Publishing final plugin release...\n"

rsync -r --quiet -e "ssh -p 2222 -o StrictHostKeyChecking=no" \
$HOME/work/BungeeAdminTools/BungeeAdminTools/target/BungeeAdminTools-*.jar \
travis@travis.internetpolice.eu:WWW/downloads/BungeeAdminTools/
