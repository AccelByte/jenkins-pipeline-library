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

def getLinkJiraTicket(){
    def commitId = sh (
        returnStdout: true,
        script: """
          export commitId=`git rev-parse --short HEAD`
        """
    )
    def repoName = sh (
	returnStdout: true,
	script: """
	  export repoName=`basename git rev-parse --show-toplevel`
	"""
    )
    def linkJira = sh (
        returnStdout: true,
        script: """
          curl -s --user ${USERNAME}:${PASSWORD} https://api.bitbucket.org/2.0/repositories/accelbyte/${repoName}/commit/${commitId}?q=summary | jq -r ".rendered.message.html" |  tr ' ' '\n' | grep href= | grep accelbyte.atlassian.net/browse | awk -F'href=' '{print \$2}' | sort -u | sed 's/"//g'
        """
    )
    return linkJira
}

def generateMessage(String status){
  def commitMessage = commitMessage()
  def commitUser = commitUser()
  def buildUser = buildUser()
  def linkJiraTicket = getLinkJiraTicket()
  return """
Job: ${env.JOB_NAME}
Job URL: ${env.BUILD_URL}
Build run by: ${buildUser}
Commit by: ${commitUser}
Commit message: ${commitMessage}
Jira Ticket: ${linkJiraTicket}
Status: ${status}
"""
}
