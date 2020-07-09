val grpcVersion = "1.30.2"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

ThisBuild / scalaVersion := "2.13.2"

val zioVersion = "1.0.0-RC21-2"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.2.1-RC1"

lazy val root = (project in file("."))
  .aggregate(
    protos.jvm,
    protos.js,
    server,
    client,
    webapp
  )

lazy val protos = crossProject(JSPlatform, JVMPlatform)
  .in(file("protos"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true)          -> (sourceManaged in Compile).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value
    ),
    PB.protoSources in Compile := Seq(
      (baseDirectory in ThisBuild).value / "protos" / "src" / "main" / "protobuf"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %%% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    )
  )

lazy val server = project
  .dependsOn(protos.jvm)
  .settings(
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "io.grpc"               % "grpc-netty"           % grpcVersion,
      "io.grpc"               % "grpc-services"        % grpcVersion
    ),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test"          % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt"      % zioVersion % "test"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    run / fork := true
  )

lazy val client = project
  .dependsOn(protos.jvm)
  .settings(
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "io.grpc"               % "grpc-netty"           % grpcVersion,
    ),
    run / fork := true
  )

lazy val webapp = project
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(ScalaJSBundlerPlugin)
  .dependsOn(protos.js)
  .settings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0",
      "org.scala-js"      %%% "scalajs-dom"     % "1.0.0"
    ),
    scalaJSUseMainModuleInitializer := true,

    // Workaround for
    // https://github.com/scalacenter/scalajs-bundler/issues/356
    Compile / fastOptJS / webpack := {
        val result = (Compile / fastOptJS / webpack).value
        result.foreach {
            (af: Attributed[File]) => 
                IO.chmod("rw-r--r--", af.data)
        }
        result
    }
  )
