submitProjectName := "scalashop"

scalaVersion := "2.11.5"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-optimise",
  "-Yinline-warnings"
)

javaOptions in run += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

(fork in Test) := false

fork := true

// See documentation in ProgFunBuild.scala
projectDetailsMap := {
  val depsCollections = Seq(
    "com.storm-enroute" %% "scalameter-core" % "0.6",
    "com.github.scala-blitz" %% "scala-blitz" % "1.1",
    "com.storm-enroute" %% "scalameter" % "0.6" % "test"
  )
  val currentCourseId = "parprog-001"
  Map(
    "example"      ->  ProjectDetails(
                         packageName = "example",
                         assignmentPartId = "gTzFogNl",
                         maxScore = 10d,
                         styleScoreRatio = 0.0,
                         courseId=currentCourseId),
    "scalashop"    -> ProjectDetails(
                         packageName = "scalashop",
                         assignmentPartId = "CCCm3IPa",
                         maxScore = 10d,
                         styleScoreRatio = 0.0,
                         courseId=currentCourseId,
                         depsCollections)
)}
