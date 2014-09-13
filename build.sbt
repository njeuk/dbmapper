import bintray.AttrMap
import bintray._
import scoverage.ScoverageSbtPlugin.instrumentSettings
import org.scoverage.coveralls.CoverallsPlugin.coverallsSettings

name := "dbmapper"

organization := "com.github.njeuk"

description := "Scala Asynchronous Postgres datamapper and data table gateway"

version := "2.1"

scalaVersion := "2.11.2"

scalacOptions += "-feature"

publishMavenStyle := true

instrumentSettings

coverallsSettings

bintrayPublishSettings

bintray.Keys.repository in bintray.Keys.bintray := "maven"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintray.Keys.bintrayOrganization in bintray.Keys.bintray := None

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






