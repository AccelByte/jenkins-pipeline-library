def call(status, credentialsId, namespace){
  def commitHashes = sh (
    returnStdout: true,
    script: "git submodule foreach --quiet 'git rev-parse HEAD'"
  ).split("\n")

  def repos = sh (
    returnStdout: true,
    script: """
      git submodule foreach --quiet 'basename \$(git remote get-url origin) | sed -e "s/.git\$//"'
    """
  ).split("\n")

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
    for(int i=0; i < repos.size(); i++) {
      bitbucketPost(buildResult, commitHashes[i].trim(), repos[i].trim(), credentialsId, namespace)      
    }
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
