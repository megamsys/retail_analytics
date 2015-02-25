val appName = "MeglyticsVisualizer"

val appVersion = "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

val sparkVersion = "1.2.0"

val akkaVersion = "2.3.9" // override Akka to be this version to match the one in Spark

val appDependencies = Seq(
  jdbc,
  anorm,
  cache,
  // HTTP client
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  // HTML parser
  "org.jodd" % "jodd-lagarto" % "3.6.3",
  // Spark
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,  
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "com.stackmob" %% "newman" % "1.3.5"
)

val root = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    libraryDependencies ++= appDependencies,
    sbt.Keys.resolvers += "Sonatype Snapshots" at Opts.resolver.sonatypeSnapshots.root,
    sbt.Keys.resolvers += "Sonatype Releases" at Opts.resolver.sonatypeStaging.root,
    sbt.Keys.resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    sbt.Keys.resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases",
    sbt.Keys.resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots",
    sbt.Keys.resolvers += "Twitter Repo" at "http://maven.twttr.com", // finagle
    sbt.Keys.resolvers += "Spray repo" at "http://repo.spray.io", //spray client used in newman.
    sbt.Keys.resolvers += "Spy Repository" at "http://files.couchbase.com/maven2" // required to resolve `spymemcached`, the plugin's dependency.
    )


//play.Project.playScalaSettings

