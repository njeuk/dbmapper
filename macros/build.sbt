name := "dbmapper-macros"

organization := "com.github.njeuk"

description := "Scala Macros used by dbmapper"

version := "2.3"

scalaVersion := "2.11.2"

scalacOptions += "-feature"

publishMavenStyle := true

bintrayPublishSettings

bintray.Keys.repository in bintray.Keys.bintray := "maven"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintray.Keys.bintrayOrganization in bintray.Keys.bintray := None

libraryDependencies ++= Seq(
  "com.github.mauricio" %% "postgresql-async" % "0.2.14",
  "org.scala-lang.modules" %% "scala-async" % "0.9.1",
  "org.scala-lang" % "scala-reflect" % "2.11.2",
  "com.vividsolutions" % "jts" % "1.13",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)



