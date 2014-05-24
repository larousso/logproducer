name := "logproducer"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.netflix.rxjava" % "rxjava-scala" % "0.17.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "2.5"
)     

play.Project.playScalaSettings
