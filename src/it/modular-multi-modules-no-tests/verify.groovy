import it.Verifier

def verifier = new Verifier(basedir.toPath())

verifier.verifyBadLines()

// TODO add verifier.verifyLogMatches invocation

verifier.isOk()
