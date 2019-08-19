def call( awsSecretCreds,
          gpgSecretCreds,
          kubeConfigUrl,
          deploymentConfig,
          deploymentFiles)
{
withAWS(
  credentials: awsSecretCreds,
  region:"us-west-2") {
    withCredentials([string(credentialsId: gpgSecretCreds, variable: 'SECRET')]){
      withEnv(["KUBECONFIG_PASSWORD=$SECRET"]){
        sh """
          aws s3 cp s3://${kubeConfigUrl} kube.config.gpg
          mkdir -p ~/.gnupg
          echo "no-tty" >> ~/.gnupg/gpg.conf
          chmod 600 ~/.gnupg
          echo \$KUBECONFIG_PASSWORD | \
          gpg --batch --passphrase-fd 0 -o /tmp/kube.config -d kube.config.gpg
        """
      }
    }
    withEnv(["KUBECONFIG=/tmp/kube.config"]) {
      deploymentFiles.each() {
        configureYaml(deploymentConfig, it)
        sh """
          kubectl apply -f ${it} -n ${deployTarget}
          kubectl replace -f ${it} -n ${deployTarget} --force
        """
      }
    }
  }
}

def configureYaml(config, yamlFile){
  def cfg = config.inspect()
  sh """
    echo ${cfg} > /tmp/configfile.tmp
    sed -i 's#, #\\n#g' /tmp/configfile.tmp
    sed -i 's#[][]##g' /tmp/configfile.tmp
    sed "s/^/s#</" /tmp/configfile.tmp | sed "s/:/>#/" | sed "s/\$/#g/" > /tmp/sedtmpl.sed
    name=\$(cat /tmp/sedtmpl.sed | grep DEPLOYMENT_NAME | tr -d \\')
    sed -i "/DEPLOYMENT_NAME/c\\\\\$name" /tmp/sedtmpl.sed
    sed -f /tmp/sedtmpl.sed -i ${yamlFile}
    replaceInteger=\$(cat ${yamlFile} | grep replicas | sed "s/'//g")
    sed -i "s/.*replicas:.*/\${replaceInteger}/" ${yamlFile}
  """
}
