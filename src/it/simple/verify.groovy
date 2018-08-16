assert new File( basedir, "target/test-classes/JupiterTest.class" ).exists()
assert new File( basedir, "target/test-classes/JupiterTests.class" ).exists()
assert new File( basedir, "target/test-classes/TestJupiter.class" ).exists()

def log = new File( basedir, 'build.log' ).getText('UTF-8')

assert log.contains( '[INFO] Launching JUnit Platform...' )
assert log.contains( '[INFO] Successfully executed: 3 test(s)' )
