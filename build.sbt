import AssemblyKeys._

seq(assemblySettings: _*)

name := "EXProxy"

version := "0.8.0"

scalaVersion := "2.10.1"

scalacOptions ++= Seq("-unchecked", "-deprecation" )

mainClass in assembly := Some("com.exproxy.ControlPanel")

jarName in assembly := "exproxy.jar"

libraryDependencies ++= Seq(
   "commons-codec" % "commons-codec" % "1.7"
  ,"commons-logging" % "commons-logging" % "1.1.2"
  ,"commons-collections" % "commons-collections" % "3.2.1"
//  ,"org.apache.httpcomponents"      % "httpclient"         % "4.1.3"
  ,"commons-httpclient" % "commons-httpclient" % "3.1"
  ,"org.scalatest" %% "scalatest" % "1.9.1" % "test"
  ,"junit" % "junit" % "4.10" % "test"
)


initialCommands in console := """import com.exproxy._"""

sourceGenerators in Compile <+= 
 (sourceManaged in Compile, version, name, jarName in assembly) map {
  (dir, version, projectname, jarexe) =>
  val file = dir / "dummy" / "MetaInfo.scala"
  IO.write(file,
  """package com.exproxy
    |object MetaInfo { 
    |  val version="%s"
    |  val project="%s"
    |  val jarbasename="%s"
    |}
    |""".stripMargin.format(version, projectname, jarexe.split("[.]").head) )
  Seq(file)
}

