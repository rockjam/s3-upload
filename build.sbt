name := "s3-upload"
version := "0.1-SNAPSHOT"
organization := "com.github.rockjam"
sbtVersion := "0.13.8"
scalaVersion := "2.11.6"
incOptions := incOptions.value.withNameHashing(true)
updateOptions := updateOptions.value.withCachedResolution(true)
Revolver.settings

scalacOptions ++= Seq(
    "-Xlint",
    "-deprecation",
    "-Xfatal-warnings",
    "-Ydelambdafy:method",
    "-target:jvm-1.7",
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps"
)
libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.9" exclude("org.scala-lang", "scala-library"),
    "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M5"
      exclude("com.typesafe.akka", "akka-actor_2.11")
      exclude("org.scala-lang", "scala-library"),
    "com.typesafe.akka" %% "akka-http-experimental" % "1.0-M5" exclude("org.scala-lang", "scala-library"),
    "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-M5"
      exclude("org.scala-lang", "scala-library")
      exclude("org.scala-lang", "scala-reflect")
)