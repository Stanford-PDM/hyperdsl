
/** Utilities for the TestAll script */
import $ivy.`com.lihaoyi::fansi:0.1.3`, fansi._
import $file.TestConfig

val config = TestConfig
/* Prefix for all the logs */

val prefix = "TestAll-"

/** Print an error to the console and exit */
def error(s: String) = {
  Console.err.println("[" + Color.Red(s"${prefix}error") + "] " + s)
  if (config.stopOnError && config.playSounds) {
    % afplay (cwd / "error.wav")
    System.exit(1)
  }
}

/** Warning */
def warn(s: String) = {
  Console.err.println("[" + Color.Yellow(s"${prefix}warning") + "] " + s)
}

def success(s: String) = {
  Console.out.println("[" + Color.Green(s"${prefix}success") + "] " + s)
}

/** Utility function to print information to the console */
def info(s: String) = {
  Console.out.println(s"[${prefix}info] " + s)
}

def printdbg(s: String) = if (config.debug) {
  Console.err.println("[" + Color.Blue(s"${prefix}debug") + "] " + s)
}

/* Helper functions to work around the limitation of the % syntax of ammonite */
// Force implicit conversions
def escape(s: String): Shellable = s
def escape(p: Path): Shellable = p
implicit def toShellableSeq(seq: Seq[String]): Seq[Shellable] = seq.map(escape)
def runEnv[T](runner: Command[T])(prog: Shellable)(args: Seq[Shellable])(env: Seq[(String, Shellable)]) = {
  val runCmd: Seq[(String, Shellable)] = (prog +: args).map(part => "" -> part)
  runner.applyDynamicNamed("apply")((runCmd ++ env): _*)
}
def run[T](runner: Command[T])(prog: Shellable)(args: Seq[Shellable]) = runEnv(runner)(prog)(args)(Seq.empty)

/** Ask sbt to spit out a list from it's configuration */
def getSbtList(args: String*): Seq[String] = {
  val res = runEnv(%%)("sbt")(args)(Seq("LMS_HOME" -> config.lms, "DELITE_HOME" -> config.delite))
  val lines = res.out.lines
  val listLine = lines.filter(_.contains("List")).head
  val cleaned = fansi.Str(listLine).plainText
  val data = cleaned.stripPrefix("[info] List(").stripSuffix(")")
  if (data.trim.isEmpty) Seq.empty else data.split(",").map(_.trim)
}

/** Don't mess up the working directory */
def resetWD[T](block: => T): T = {
  val saved = wd
  val result = block
  cd ! saved
  result
}

/** Find out all the apps in a forge generated dsl */
def discoverApps(dslName: String): Seq[String] = resetWD {
  cd(cwd / 'published / dslName)
  getSbtList(s"project $dslName-apps", "show discoveredMainClasses")
}

/** Find out all the tests in a forge generated dsl */
def discoverTests(dslName: String): Seq[String] = resetWD {
  cd(cwd / 'published / dslName)
  getSbtList(s"project $dslName-tests", "show definedTestNames")
}

/** Find out all of the dsls that forge can generate */
def discoverForgeDsls: Seq[String] = {
  getSbtList("project forge", "show discoveredMainClasses")
}

/* Check that nothing was forgotten run */
def reportMissingTests: Unit = {
  val forgeDslsRunners = discoverForgeDsls
  val missingDsls = forgeDslsRunners.filterNot(name => config.dsls.exists(_.runner == name))

  if (missingDsls.isEmpty) {
    success(s"No dsls seems to be missing from tests")
  } else {
    missingDsls.foreach { dsl => warn(s"Could not find dsl in config: $dsl") }
  }
  for (dsl <- config.dsls) try {
    if (!dsl.elems.contains(config.AllTests)) {
      val tests = discoverTests(dsl.name)
      val testsRun = dsl.elems.collect { case config.Test(name) => name }
      val missingTests = tests.filterNot(name => testsRun.contains(name))
      if (missingTests.isEmpty) {
        success(s"All test were executed for ${dsl.name}")
      } else {
        missingTests.foreach {
          test => warn(s"Could not find test in config: $test for ${dsl.name}")
        }
      }

    }

    val apps = discoverApps(dsl.name)
    val appsRun = dsl.elems.collect { case config.App(_, name, _) => name }
    val missingApps = apps.filterNot(name => appsRun.contains(name))
    if (missingApps.isEmpty) {
      success(s"No missing apps for ${dsl.name}")
    } else {
      missingApps.foreach {
        app => warn(s"Could not find app in config: $app for ${dsl.name}")
      }
    }

    val interpretedApps = dsl.elems.collect { case config.App(config.Interpreted, name, _) => name }
    val degsGenerated = ls('published / dsl.name) |? (_.ext == "deg") | (_.name.stripSuffix(".deg"))
    val missclassified = interpretedApps.intersect(degsGenerated)
    missclassified.foreach {
      app => warn(s"Possible config mistake: $app was marked as Interpreted but generated a deg file.")
    }
  } catch {
    case e: Throwable => error(s"Could not retrieve information for dsl ${dsl.name}")
  }
}

/** Print command if debug is enabled */
def executeInteractiveDebug(wd: Path, cmd: Command[_]) = {
  val cmdString = cmd.cmd.map(elem => s""""$elem"""").mkString(" ")
  val env = cmd.envArgs.map { case (k, v) => s"$k -> $v" }.mkString(", ")
  printdbg(s"Path = $wd")
  printdbg(s"Command = $cmdString")
  printdbg(s"Env = $env")
  Shellout.executeInteractive(wd, cmd)
}

/** Load tests that have already succeeded */
var passedTests: Seq[String] = {
  if (config.skipSuccessfulTests && exists ! config.testCache) {
    (read ! config.testCache).lines.filter(_.nonEmpty).map(_.trim).toSeq
  } else {
    Seq.empty
  }
}

/** Strucure for the tests */
var currentPath: Seq[String] = Seq()
var failedTests: Seq[String] = Seq()
var skippedTests: Seq[(String, String)] = Seq()
def isPrefix(englobing: String, subScope: String): Boolean = {
  subScope.startsWith(englobing)
}

// Run a test
def test(title: String)(block: => Unit) = section(title)(block)
def app(title: String)(block: => Unit) = {
  if (config.runOnlyTests) {
    val fullName = (currentPath :+ title).mkString(" - ")
    info(s"Skipping non test: $fullName")
    skippedTests :+= (fullName, "Skipping non-tests")
  } else {
    section(title)(block)
  }
}

def section(title: String)(block: => Unit) = {
  currentPath :+= title
  val fullName = currentPath.mkString(" - ")
  if (config.skipSuccessfulTests && passedTests.contains(fullName)) {
    info(s"Skipping successful test: $fullName")
  } else if (config.skipList.exists(_._1 == fullName)) {
    val reason = config.skipList.filter(_._1 == fullName).head._2
    warn(s"Skipping test: $fullName ($reason)")
    skippedTests :+= (fullName, reason)
  } else {
    try {
      info(s"Running: $fullName")
      block
      if (failedTests.exists(failed => isPrefix(fullName, failed))) {
        // a subtask has failed, so we cannot succeed
        throw new InteractiveShelloutException()
      } else {
        success(fullName)
        passedTests :+= fullName
        write.over(config.testCache, passedTests.mkString("\n"))
      }
    } catch {
      case e: InteractiveShelloutException =>
        error(s"Failed: $fullName")
        // if we don't fail on error we need to make sure the englobing scope fails too
        failedTests :+= fullName
    }
  }
  currentPath = currentPath.init
}

/** Keep only the tests that are at the lowest scope */
def filterPrefixes(tests: Seq[String]): Seq[String] = {
  tests.filterNot(prefix => tests.exists(test => isPrefix(prefix, test) && prefix != test))
}

/** Report the results of the run */
def makeReport: Unit = {

  if (failedTests.isEmpty) {
    success("All tests passed!")
    if (config.playSounds) % afplay (cwd / "success.wav")
  } else {
    error("Some tests failed!")
    if (config.playSounds) % afplay (cwd / "error.wav")
    val passed = filterPrefixes(passedTests)
    val failed = filterPrefixes(failedTests)
    for (test <- passed) {
      success(s"Passed: $test")
    }
    for (test <- failed) {
      error(s"Failed: $test")
    }
    for ((test, reason) <- skippedTests) {
      warn(s"Skipped: $test ($reason)")
    }
    info(s"Total number of tests run: ${passed.length + failed.length}")
    success(s"Number of test passed: ${passed.length}")
    error(s"Number of test failed: ${failed.length}")
  }

  // Report anything that seems to be missing in config file
  if (config.checkAllRun) {
    info("Looking for tests that might be missing ... ")
    reportMissingTests
  }
}
