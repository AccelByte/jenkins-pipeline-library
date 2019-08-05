def call(status, credentialsId, namespace){
  def commitHash = sh (
    returnStdout: true,
    script: "git rev-parse HEAD"
  )

  def repoName = sh (
    returnStdout: true,
    script: """
      basename \$(git remote get-url origin) | sed -e "s/.git\$//"
    """
  )

  def buildResult = null
  switch(status) {
    case "SUCCESS":
      buildResult = "SUCCESSFUL"
      break
    case "INPROGRESS":
      buildResult = "INPROGRESS"
      break
    case "FAILURE":
      buildResult = "FAILED"
      break
    default:
      return
    break
  }

  node ("master"){
    bitbucketPost(buildResult, commitHash.trim(), repoName.trim(), credentialsId, namespace)
  }
}

def bitbucketPost(status, commitHash, repoName, credentialsId, namespace){
  def rootApi = "https://api.bitbucket.org/2.0/repositories"
  def response = httpRequest(
    url : "${rootApi}/${namespace}/${repoName}/commit/${commitHash}/statuses/build",
    httpMode: "POST",
    requestBody: "{\"state\": \"${status}\", \"key\": \"build\", \"name\": \"${BUILD_TAG}\", \"description\":\"Jenkins Build\",\"url\": \"${env.BUILD_URL}/console\" }",
    contentType: "APPLICATION_JSON",
    authentication: credentialsId,
    validResponseCodes: '200:201'
  )
}
