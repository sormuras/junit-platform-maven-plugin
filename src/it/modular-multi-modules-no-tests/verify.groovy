import it.Verifier

def verifier = new Verifier(basedir.toPath())

verifier.verifyBadLines()

verifier.verifyLogMatches ">> BEGIN >>",
  "    <failIfNoTests>false</failIfNoTests>",
  ">> A bunch of log lines... >>",
  "\\Q[DEBUG] Patched directory \\E.*/module-with-only-sources/target/junit-platform/patched-test-runtime was not found and failIfNoTests is set to false",
  ">> A bunch of log lines... >>",
  "[INFO] BUILD SUCCESS",
  ">> END. >>"

verifier.isOk()
