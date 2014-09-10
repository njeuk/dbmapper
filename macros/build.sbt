name := "dbmapper-macros"

organization := "com.github.njeuk"

version := "2.0-SNAPSHOT"

scalaVersion := "2.11.2"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.github.mauricio" %% "postgresql-async" % "0.2.14",
  "org.scala-lang.modules" %% "scala-async" % "0.9.1",
  "org.scala-lang" % "scala-reflect" % "2.11.2",
  "com.vividsolutions" % "jts" % "1.13",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)



