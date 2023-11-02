import it.Verifier

def verifier = new Verifier(basedir.toPath())

// verifier.verifyBadLines();

verifier.verifyLogMatches ">> BEGIN >>",
  "[INFO] Launching JUnit Platform " + junitPlatformVersion + "...",
  ">> Platform executes tests...>>",
  "\\Q[WARNING] WARNING: A Java agent has been loaded dynamically\\E.+",
  "[WARNING] WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning",
  "[WARNING] WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information",
  "[WARNING] WARNING: Dynamic loading of agents will be disallowed by default in a future release",
  ">> ... >>",
  "[INFO] BUILD SUCCESS",
  ">> END. >>"

verifier.isOk()
