import it.Verifier

def verifier = new Verifier(basedir.toPath())

verifier.verifyBadLines();

verifier.verifyLogMatches ">> BEGIN >>",
  "[INFO] Launching JUnit Platform " + junitPlatformVersion + "...",
  ">> Platform executes tests...>>",
  "[INFO] BUILD SUCCESS",
  ">> END. >>"

verifier.isOk()
