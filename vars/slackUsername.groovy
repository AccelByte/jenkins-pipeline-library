def call(String token,String email){
  def username = sh (
    returnStdout: true,
    script: """
        export USER_TMP=\$(mktemp)
        curl -o \${USER_TMP} -X GET "https://slack.com/api/users.lookupByEmail?token=${token}&email=${email}"
        USERNAME=\$(cat \${USER_TMP} | jq -r '.user.name') 
        echo "@\${USERNAME}"
    """
  )
  return username
}