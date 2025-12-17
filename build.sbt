ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.codepowered" // The groupID (often a reversed domain name)

ThisBuild / scalaVersion := "2.13.18"

lazy val root = (project in file("."))
  .settings(
    name := "logstash-jms-mongodb",
    libraryDependencies ++= Seq(

      "org.scalactic" %% "scalactic" % "3.2.18", // Apache 2.0

      "org.apache.pekko" %% "pekko-actor" % "1.0.3", // Apache 2.0
      "org.apache.pekko" %% "pekko-stream" % "1.0.3", // Apache 2.0
      "org.apache.pekko" %% "pekko-connectors-jms" % "1.0.2", // Apache 2.0

      "javax.jms" % "jms-api" % "1.1-rev-1", // CDDL 1.0
      "org.apache.activemq" % "activemq-broker" % "5.19.1", // Apache 2.0
      "org.apache.activemq" % "activemq-client" % "5.19.1", // Apache 2.0

      "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.1", // Apache 2.0
      "org.mongodb.scala" %% "mongo-scala-driver" % "5.1.2", // Apache 2.0

      "org.apache.avro" % "avro" % "1.11.4", // Apache 2.0

      "ch.qos.logback" % "logback-classic" % "1.5.22", // EPL v1.0 and LGPL 2.1 (Dual Licensed)
      "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.23.1", // Apache 2.0
      "org.slf4j" % "log4j-over-slf4j" % "2.0.13", // MIT License
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5", // Apache 2.0

      "org.scalatest" %% "scalatest" % "3.2.18" % Test, // Apache 2.0
    )
  )
