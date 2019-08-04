def call(String version) {
  def tagged = sh (
    returnStdout: true,
    script: """
      if git rev-parse ${version} &> /dev/null ; \
      then echo true; else echo false; fi
    """
  )
  if (tagged.trim() == "true") return true
  return false
}
