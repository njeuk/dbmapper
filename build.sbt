import bintray.AttrMap
import bintray._

name := "dbmapper"

organization := "com.github.njeuk"

description := "Scala Asynchronous Postgres datamapper and data table gateway"

version := "2.8"

scalaVersion := "2.11.8"

scalacOptions += "-feature"

publishMavenStyle := true

site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:njeuk/dbmapper.git"

seq(bintraySettings:_*)

bintray.Keys.repository in bintray.Keys.bintray := "maven"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintray.Keys.bintrayOrganization in bintray.Keys.bintray := None

libraryDependencies ++= Seq(
  "com.github.mauricio" %% "postgresql-async" % "0.2.18",
  "org.scala-lang.modules" %% "scala-async" % "0.9.1",
  "com.vividsolutions" % "jts" % "1.13",
  "com.typesafe" % "config" % "1.2.1",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "com.github.njeuk" %% "dbmapper-macros" % "2.7.9",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)






