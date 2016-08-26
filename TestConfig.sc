/** Configuration for the test file */

// Print commands before running them
val debug = false

// Skip previously successful tests
val skipSuccessfulTests = true

// Skip list for test that have some problem that will not be fixed soon
// First element is the name of the test, second is the reason why it is being skipped
val skipList: Seq[(String, String)] = Seq(
  "Forge - OptiML - testOnly StreamSuiteCompiler" -> "OutOfMemoryError: GC overhead limit exceeded",
  "Forge - OptiML - Example14" -> "Need to implement restaged apps",
  "Forge - OptiML - Example15" -> "Need to implement restaged apps",
  "Forge - OptiML - GDAInterpreter" -> "Needs input",
  "Forge - OptiML - GDACompiler - running" -> "Needs input",
  "Forge - OptiML - GibbsInterpreter" -> "Needs input",
  "Forge - OptiML - GibbsCompiler - running" -> "Needs input",
  "Forge - OptiML - KDDNNInterpreter" -> "Needs input",
  "Forge - OptiML - KDDNNCompiler - running" -> "Needs input",
  "Forge - OptiML - LogRegInterpreter" -> "Needs input",
  "Forge - OptiML - LogRegCompiler - running" -> "Needs input",
  "Forge - OptiML - MiniMSMInterpreter" -> "Needs input",
  "Forge - OptiML - MiniMSMCompiler - running" -> "Needs input",
  "Forge - OptiML - QPSCDInterpreter" -> "Needs input",
  "Forge - OptiML - QPSCDCompiler - running" -> "Needs input",
  "Forge - OptiML - RNTNInterpreter" -> "Needs input",
  "Forge - OptiML - RNTNCompiler" -> "Something goes horribly wrong",
  "Forge - OptiML - SVMInterpreter" -> "Needs input",
  "Forge - OptiML - SVMCompiler - running" -> "Needs input",
  "Forge - OptiML - TopNInterpreter" -> "Needs input",
  "Forge - OptiML - TopNCompiler - running" -> "Needs input",
  "Forge - OptiML - kMeansInterpreter" -> "Needs input",
  "Forge - OptiML - kMeansCompiler - running" -> "Needs input",
  "Forge - OptiML - kNNInterpreter - running" -> "Needs input",
  "Forge - OptiML - kNNCompiler - running" -> "Needs input",
  "Forge - OptiML - CGInterpreter - running" -> "Needs input",
  "Forge - OptiML - CGCompiler - running" -> "Needs input",
  "Forge - OptiML - ScratchpadRunner - running" -> "Needs input",
  
  "Forge - OptiQL - GeneInterpreter" -> "Needs input",
  "Forge - OptiQL - GeneCompiler - running" -> "Needs input",
  
  "Forge - OptiGraph - BCInterpreter" -> "Needs input",
  "Forge - OptiGraph - BCCompiler - running" -> "Needs input",
  "Forge - OptiGraph - CommunityDetectionInterpreter" -> "Needs input",
  "Forge - OptiGraph - CommunityDetectionCompiler - running" -> "Needs input",
  "Forge - OptiGraph - PageRankInterpreter" -> "Needs input",
  "Forge - OptiGraph - PageRankCompiler - running" -> "Needs input",
  "Forge - OptiGraph - UndirectedTriangleCountingInterpreter" -> "Needs input",
  "Forge - OptiGraph - UndirectedTriangleCountingCompiler - running" -> "Needs input",

  "Forge - OptiWrangler - ExamplesInterpreter" -> "Needs input",
  "Forge - OptiWrangler - ExamplesCompiler - running" -> "Needs input",

  "Forge - MetaMeta" -> "Don't know what it is?",

  "Forge - OptiML - testOnly ValidationSuiteCompiler" -> "violating ordering of effects !!!!"
  )

// Path where to save successful tests from previous runs
val testCache = cwd / ".testcache"

// Check if all forge generate dsl's tests & apps have been run
val checkAllRun = true

// Stop after failure of a test
val stopOnError = true

// Play a sound when the execution terminates (useful for long runnning tests)
val playSounds = true

// Run only the tests, ignore apps
val runOnlyTests = true

// Configuration folders
val hyper = cwd
val delite = cwd / 'delite
val forge = cwd / 'forge
val lms = cwd / "virtualization-lms-core"

// Tpch data
val tpchData = delite / 'bin / 'tpch

val runtimes = Seq(Scala, CPP)
lazy val dsls = Seq(simpleIntVector, simpleVector, optiml, optiql, optigraph, optiwrangler, optila, metameta)

// The different runtimes we can compile to
trait Runtime { def name: String }
object CPP extends Runtime { val name = "cpp" }
object Scala extends Runtime { val name = "scala" }

// A forge dsl 
case class Dsl(name: String, runner: String, elems: DslProgram*)

// Simple interface to represent things that can run in the context of a DSL
trait DslProgram

// Run all tests no matter which ones are specified
object AllTests extends DslProgram

// Run one test
case class Test(name: String) extends DslProgram

trait AppType
object Staged extends AppType
object Interpreted extends AppType
case class App(typ: AppType, name: String, args: Seq[Shellable]) extends DslProgram

def StagedApp(name: String, args: Shellable*) = App(Staged, name, args)
def InterpretedApp(name: String, args: Shellable*) = App(Interpreted, name, args)

// Forge dsls
val simpleIntVector = Dsl(name = "SimpleIntVector", runner = "ppl.dsl.forge.examples.SimpleIntVectorDSLRunner",
  InterpretedApp("HelloSimpleIntInterpreter"), StagedApp("HelloSimpleIntCompiler"))

val simpleVector = Dsl(name = "SimpleVector", runner = "ppl.dsl.forge.examples.SimpleVectorDSLRunner",
  InterpretedApp("HelloSimpleInterpreter"), StagedApp("HelloSimpleCompiler"))

val optiml = Dsl(name = "OptiML", runner = "ppl.dsl.forge.dsls.optiml.OptiMLDSLRunner",
  Test("DenseVectorSuiteInterpreter"), Test("DenseVectorSuiteCompiler"),
  Test("ImageSuiteInterpreter"), Test("ImageSuiteCompiler"),
  Test("DenseLinearAlgebraSuiteInterpreter"), Test("DenseLinearAlgebraSuiteCompiler"),
  Test("ValidationSuiteInterpreter"), Test("ValidationSuiteCompiler"),
  Test("LanguageSuiteInterpreter"), Test("LanguageSuiteCompiler"),
  Test("FeatureSuiteInterpreter"), Test("FeatureSuiteCompiler"),
  Test("IOSuiteInterpreter"), Test("IOSuiteCompiler"),
  Test("SparseLinearAlgebraSuiteInterpreter"), Test("SparseLinearAlgebraSuiteCompiler"),
  Test("ArithmeticConversionSuiteInterpreter"), Test("ArithmeticConversionSuiteCompiler"),
  Test("SparseVectorSuiteInterpreter"), Test("SparseVectorSuiteCompiler"),
  Test("BasicMathSuiteInterpreter"), Test("BasicMathSuiteCompiler"),
  Test("SparseMatrixSuiteInterpreter"), Test("SparseMatrixSuiteCompiler"),
  Test("StreamSuiteInterpreter"), Test("StreamSuiteCompiler"),
  Test("ComplexSuiteInterpreter"), Test("ComplexSuiteCompiler"),
  Test("DenseMatrixSuiteInterpreter"), Test("DenseMatrixSuiteCompiler"),
  InterpretedApp("ARFFInterpreter"), StagedApp("ARFFCompiler"),
  InterpretedApp("Example1Interpreter"), StagedApp("Example1Compiler"),
  InterpretedApp("Example2Interpreter"), StagedApp("Example2Compiler"),
  InterpretedApp("Example3Interpreter"), StagedApp("Example3Compiler"),
  InterpretedApp("Example4Interpreter"), StagedApp("Example4Compiler"),
  InterpretedApp("Example5Interpreter"), StagedApp("Example5Compiler"),
  InterpretedApp("Example6Interpreter"), StagedApp("Example6Compiler"),
  InterpretedApp("Example7Interpreter"), StagedApp("Example7Compiler"),
  InterpretedApp("Example8Interpreter"), StagedApp("Example8Compiler"),
  InterpretedApp("Example10Interpreter"), StagedApp("Example10Compiler"),
  InterpretedApp("Example11Interpreter"), StagedApp("Example11Compiler"),
  InterpretedApp("Example12Interpreter"), StagedApp("Example12Compiler"),
  InterpretedApp("Example13Interpreter"), StagedApp("Example13Compiler"),
  InterpretedApp("Example14"),
  InterpretedApp("Example15"),
  InterpretedApp("GDAInterpreter"), StagedApp("GDACompiler"),
  InterpretedApp("GibbsInterpreter"), StagedApp("GibbsCompiler"),
  InterpretedApp("KDDNNInterpreter"), StagedApp("KDDNNCompiler"),
  InterpretedApp("LogRegInterpreter"), StagedApp("LogRegCompiler"),
  InterpretedApp("MiniMSMInterpreter"), StagedApp("MiniMSMCompiler"),
  InterpretedApp("NBInterpreter"), StagedApp("NBCompiler"),
  InterpretedApp("NNInterpreter"), StagedApp("NNCompiler"),
  InterpretedApp("NeuronIdInterpreter"), StagedApp("NeuronIdCompiler"),
  InterpretedApp("PDIPSolverInterpreter"), StagedApp("PDIPSolverCompiler"),
  InterpretedApp("QPSCDInterpreter"), StagedApp("QPSCDCompiler"),
  InterpretedApp("RBMInterpreter"), StagedApp("RBMCompiler"),
  InterpretedApp("RNTNInterpreter"), StagedApp("RNTNCompiler"),
  InterpretedApp("SVMInterpreter"), StagedApp("SVMCompiler"),
  InterpretedApp("TopNInterpreter"), StagedApp("TopNCompiler"),
  InterpretedApp("kMeansInterpreter"), StagedApp("kMeansCompiler"),
  InterpretedApp("kNNInterpreter"), StagedApp("kNNCompiler"),
  InterpretedApp("CGInterpreter"), StagedApp("CGCompiler"),
  StagedApp("CGFunction"),
  StagedApp("IrisClassifyRunner"),
  StagedApp("ScratchpadRunner"),
  StagedApp("ConvLayerCompiler"))

val optiql = Dsl(name = "OptiQL", runner = "ppl.dsl.forge.dsls.optiql.OptiQLDSLRunner",
  Test("QuerySuiteInterpreter"), Test("QuerySuiteCompiler"),
  InterpretedApp("TPCHQ1Interpreter", tpchData), StagedApp("TPCHQ1Compiler", tpchData),
  InterpretedApp("TPCHQ6Interpreter", tpchData), StagedApp("TPCHQ6Compiler", tpchData),
  InterpretedApp("TPCHQ14Interpreter", tpchData), StagedApp("TPCHQ14Compiler", tpchData),
  InterpretedApp("GeneInterpreter"), StagedApp("GeneCompiler"))

val optigraph = Dsl(name = "OptiGraph", runner = "ppl.dsl.forge.dsls.optigraph.OptiGraphDSLRunner",
  Test("GraphSuiteInterpreter"), Test("GraphSuiteCompiler"),
  InterpretedApp("BCInterpreter"), StagedApp("BCCompiler"),
  InterpretedApp("CommunityDetectionInterpreter"), StagedApp("CommunityDetectionCompiler"),
  StagedApp("CommunityDetectionJar"),
  InterpretedApp("PageRankInterpreter"), StagedApp("PageRankCompiler"),
  InterpretedApp("UndirectedTriangleCountingInterpreter"), StagedApp("UndirectedTriangleCountingCompiler"))

val optiwrangler = Dsl(name = "OptiWrangler", runner = "ppl.dsl.forge.dsls.optiwrangler.OptiWranglerDSLRunner",
  InterpretedApp("ExamplesInterpreter"), StagedApp("ExamplesCompiler"))

val optila = Dsl(name = "OptiLA", runner = "ppl.dsl.forge.dsls.optila.OptiLADSLRunner")

val metameta = Dsl(name = "MetaMeta", runner = "ppl.dsl.forge.examples.MetaMetaDSLRunner")
