import it.Verifier

def verifier = new Verifier(basedir.toPath())

verifier.verifyReadable "pom.xml",
  "module-with-both-sources-and-tests/pom.xml",
  "module-with-both-sources-and-tests/src/main/java/module-info.java",
  "module-with-both-sources-and-tests/src/test/java/module-info.test",
  "module-with-only-sources/pom.xml",
  "module-with-only-sources/src/main/java/module-info.java"

verifier.verifyNotExists "module-with-only-sources/src/test/java/module-info.test"

verifier.isOk()
