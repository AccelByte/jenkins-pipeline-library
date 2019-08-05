def call(String buildResult, String channel){
  if ( buildResult == "SUCCESS" ) {
    slackSend color: "good",
    channel: channel,
    message: generateMessage("SUCCESSFULL")
  }
  else if( buildResult == "FAILURE" ) {
    slackSend color: "danger",
    channel: channel,
    message: generateMessage("FAILED")
  }
  else if( buildResult == "UNSTABLE" ) {
    slackSend color: "warning",
    channel: channel,
    message: generateMessage("UNSTABLE")
  }
  else {
    slackSend color: "danger",
    channel: channel,
    message: generateMessage("UNCLEAR")
  }
}

def commitMessage(){
  def message = sh (
    returnStdout: true,
    script: """
      git show -s --format=%s%b `git log --pretty=format:'%h' -n 1`
    """
  )
  return message
}

def commitUser(){
  def user = sh (
    returnStdout: true,
    script: """
      git show -s --format='%ae' `git log --pretty=format:'%h' -n 1`
    """
  )
  return user
}

def buildUser(){
  wrap([$class: 'BuildUser']) {
    if (env.BUILD_USER==null) {
      return "Bitbucket-Trigger"
    }
    return "${BUILD_USER}"
  }
}

def generateMessage(String status){
  def commitMessage = commitMessage()
  def commitUser = commitUser()
  def buildUser = buildUser()
  return """
Job: ${env.JOB_NAME}
Job URL: ${env.BUILD_URL}
Build run by: ${buildUser}
Commit by: ${commitUser}
Commit message: ${commitMessage}
Status: ${status}
"""
}
