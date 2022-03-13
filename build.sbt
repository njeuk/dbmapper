

name := "dbmapper"

organization := "com.github.njeuk"

description := "Scala Asynchronous Postgres datamapper and data table gateway"

version := "3.7"

scalaVersion := "2.12.11"

scalacOptions += "-feature"

publishMavenStyle := true

enablePlugins(SiteScaladocPlugin)

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:njeuk/dbmapper.git"

//seq(bintraySettings:_*)

//bintray.Keys.repository in bintray.Keys.bintray := "maven"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

//bintray.Keys.bintrayOrganization in bintray.Keys.bintray := None

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-async" % "0.9.7",
  "com.vividsolutions" % "jts" % "1.13",
  "com.typesafe" % "config" % "1.3.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.github.njeuk" %% "dbmapper-macros" % "3.5",
  "org.slf4j" % "slf4j-api" % "1.7.22",
  "joda-time" % "joda-time" % "2.9.7",
  "org.joda" % "joda-convert" % "1.8.1",
  "io.netty" % "netty-all" % "4.1.6.Final",
  "org.javassist" % "javassist" % "3.21.0-GA",
  "org.scalatest" % "scalatest_2.12" % "3.0.3" % "test"
)






