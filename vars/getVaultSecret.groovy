def call(path, secretKey, vaultUrl, vaultCredentialId){
  def secrets = [
    [$class: "VaultSecret", path: path, secretValues: [
    [$class: "VaultSecretValue", envVar: "secret", vaultKey: secretKey]]]
  ]
  def configuration = [$class: "VaultConfiguration",
                        vaultUrl: vaultUrl,
                        vaultCredentialId: vaultCredentialId]
  def retval = null
  wrap([$class: "VaultBuildWrapper", configuration: configuration, vaultSecrets: secrets]) {
    retval = env.secret
  }
  return retval
}
