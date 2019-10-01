#!/usr/bin/env bash

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then

  echo -e "Running release script...\n"
  echo -e "Publishing javadocs and artifacts...\n"
  cd $HOME

  rsync -r --quiet $HOME/build/internetpolice-eu/BungeeAdminTools/target/mvn-repo/ \
  doolm@shell.xs4all.nl:WWW/.m2/

  echo -e "Publishing javadocs...\n"

  rsync -r --delete --quiet $HOME/build/internetpolice-eu/BungeeAdminTools/target/site/apidocs/ \
  doolm@shell.xs4all.nl:WWW/javadocs/release/BungeeAdminTools/

  echo -e "Publishing final plugin release...\n"

  rsync -r --quiet $HOME/build/internetpolice-eu/BungeeAdminTools/target/BungeeAdminTools-*.jar \
  doolm@shell.xs4all.nl:WWW/downloads/BungeeAdminTools/

fi
