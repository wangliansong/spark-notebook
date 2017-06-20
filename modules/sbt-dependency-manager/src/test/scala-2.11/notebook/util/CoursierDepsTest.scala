package notebook.util

import com.datafellas.utils.Repos
import coursier.{Attributes, Dependency, Module}
import coursier.maven.MavenRepository
import org.scalatest._
import org.sonatype.aether.repository.RemoteRepository

// FIXME: missing scala-style exclusions
class CoursierDepsTest extends WordSpec with Matchers with BeforeAndAfterAll with Inside {
  val sparkVersion = "10.0.0"
  val version = "0.1.1"
  val classifier = "some-classifier"
  val scalaVer = "2.11"

  // based on ReplCalculator
  val remotes: List[RemoteRepository] = List(Repos.mavenLocal, Repos.central, Repos.sparkPackages, Repos.oss)

  val (repos, deps) = CoursierDeps.parseCoursierDependencies(
    cp =
      s"""|com.someorg % java-artifact % $version
          |com.someorg %% scala-artifact % $version
          |com.someorg %% scala-artifact-cl % $version classifier $classifier
          |- org.excludeme : excluded-artifact-mavenlike : _
          |- org.excluded.org : _ : _
          |- org.excludeme % excluded-artifact-scalalike % _
          |""".stripMargin,
    remotes = remotes,
    sparkVersion = "2.1.1"
  )

  "yield repos" in {
    repos shouldBe Seq(
      MavenRepository(Repos.mavenLocal.getUrl),
      MavenRepository("http://repo1.maven.org/maven2/"),
      MavenRepository("http://dl.bintray.com/spark-packages/maven/"),
      MavenRepository("https://oss.sonatype.org/content/repositories/releases/")
    )
  }

  "yield dependencies with excludes (ignoring version in exclude)" in {
    val expectedExcludes = Set(
      ("org.excludeme", "excluded-artifact-mavenlike"),
      // '*' denotes an exclude by org, see https://goo.gl/mKGKoz
      ("org.excluded.org", "*")
    )
    deps shouldBe Set(
      Dependency(Module("com.someorg", "java-artifact"), version, exclusions = expectedExcludes),
      Dependency(Module("com.someorg", s"scala-artifact_$scalaVer"), version, exclusions = expectedExcludes),
      Dependency(Module("com.someorg", s"scala-artifact-cl_$scalaVer"), version, exclusions = expectedExcludes, attributes = Attributes(classifier=classifier))
    )
  }
}