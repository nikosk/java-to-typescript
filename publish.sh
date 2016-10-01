mvn versions:set -DgenerateBackupPoms=false
mvn -DaltDeploymentRepository=repo::default::file:releases/ clean deploy
