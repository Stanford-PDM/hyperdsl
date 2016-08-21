#!/usr/bin/env amm

import $file.TestConfig, TestConfig._
import $file.TestUtils, TestUtils._

/**
 * Run all the tests in hyperdsl
 */

// Override % with the one that understands debug
val % = new Command(Vector.empty, Map.empty, executeInteractiveDebug)

// Check that all of the data we need is present before starting
if (!(exists ! tpchData) || !(stat ! tpchData).isDir) {
  error(s"Could not find tpch data in $tpchData")
}

section("Delite") {
  test("Ops tests") {
    % sbt ("project delite-test", "testOnly ppl.tests.scalatest.delite.DeliteOpSuite", DELITE_HOME = delite)
  }

  test("OptiQL tests") {
    % sbt ("project optiql", "test")
  }

  app("SimpleVector dsl") {
    % sbt ("project simple", "run")
    mv.over("out.deg", "simple.deg")
    %(delite / 'bin / 'delite, "simple", HYPER_HOME = hyper)
  }

  section("TCPHQ apps") {
    % sbt ("project apps", "compile")
    for (id <- Seq(1, 2, 3, 4, 6, 14)) app(s"TPCHQ$id") {
      section("stage") {
        %(delite / 'bin / 'delitec, s"ppl.apps.dataquery.tpch.TPCHQ$id", LMS_HOME = lms, HYPER_HOME = cwd)
      }
      section("run") {
        %(delite / 'bin / 'delite, s"ppl.apps.dataquery.tpch.TPCHQ$id", tpchData, HYPER_HOME = cwd)
      }
    }
  }
}

section("Forge") {

  for (dsl <- dsls) section(dsl.name) {

    section(s"update") {
      %(forge / 'bin / 'update, dsl.runner, dsl.name, FORGE_HOME = forge, LMS_HOME = lms, HYPER_HOME = cwd, DELITE_HOME = delite)
    }

    cd ! 'published / dsl.name
    for (elem <- dsl.elems) elem match {

      case AllTests => test("tests") {
        % sbt (s"project ${dsl.name}-tests", "test", DELITE_HOME = delite, LMS_HOME = lms)
      }

      case Test(name) => test(s"testOnly $name") {
        % sbt (s"project ${dsl.name}-tests", s"testOnly $name", DELITE_HOME = delite, LMS_HOME = lms)
      }

      case App(Staged, name, args) => app(name) {
        section("staging") {
          %('bin / 'delitec, name, DELITE_HOME = delite)
        }
        section("running") {
          runEnv(%)('bin / 'delite)(escape(name) +: args)(Seq("DELITE_HOME" -> escape(delite)))
        }
      }

      case App(Interpreted, name, args) => app(name) {
        runEnv(%)('bin / 'delitec)(escape(name) +: args)(Seq("DELITE_HOME" -> escape(delite)))
      }
    }

    cd ! hyper
  }
}

makeReport