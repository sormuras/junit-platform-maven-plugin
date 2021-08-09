import it.Verifier

def verifier = new Verifier(basedir.toPath())

verifier.verifyBadLines()

verifier.verifyLogMatches ">> BEGIN >>",
  "[DEBUG]   (f) isolation = ABSOLUTE",
  ">> A bunch of log lines... >>",
  "\\Q[DEBUG]   dependency-scope-compile-classpath:dummy-module-one -> dependency-scope-compile-classpath:dummy-module-one:jar:0:compile\\E.*",
  "\\Q[DEBUG]   dependency-scope-compile-classpath:dummy-module-two -> dependency-scope-compile-classpath:dummy-module-two:jar:0:provided\\E.*",
  ">> A bunch of log lines... >>",
  "[INFO] BUILD SUCCESS",
  ">> END. >>"

verifier.isOk()
