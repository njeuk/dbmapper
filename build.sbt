name := "dbmapper"

organization := "com.github.njeuk"

version := "2.0-SNAPSHOT"

scalaVersion := "2.11.2"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.github.mauricio" %% "postgresql-async" % "0.2.14",
  "org.scala-lang.modules" %% "scala-async" % "0.9.1",
  "com.vividsolutions" % "jts" % "1.13",
  "com.typesafe" % "config" % "1.2.1",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)

lazy val macros = project.in(file("macros"))

lazy val root = project.in(file(".")).dependsOn(macros)






