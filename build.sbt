val scala3Version = "3.3.1"

Global / onChangedBuildSource    := ReloadOnSourceChanges

lazy val root =
  project
    .in(file("."))
    .dependsOn(reflux)
    .aggregate(reflux)
    .settings(
      name         := "homeData",
      version      := "0.1.0",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "co.fs2"                        %% "fs2-core"      % "3.9.3",
        "co.fs2"                        %% "fs2-io"        % "3.9.3",
        "com.softwaremill.sttp.client4" %% "core"          % "4.0.0-M8",
        "io.github.arainko"             %% "ducktape"      % "0.1.11",
        "org.json4s"                    %% "json4s-native" % "4.1.0-M4",
        "org.scalameta"                 %% "munit"         % "0.7.29" % Test,
        "org.typelevel"                 %% "cats-core"     % "2.10.0",
        "org.typelevel"                 %% "cats-effect"   % "3.5.2",
        // "com.lihaoyi"                   %% "upickle"       % "3.1.3",
      ),
    )

lazy val reflux =
  project
    .in(file("reflux"))
    .settings(
      name         := "reflux",
      version      := "0.1.0",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "org.typelevel"                         %% "cats-core"             % "2.10.0",
        "org.typelevel"                         %% "cats-effect"           % "3.5.2",
        "org.http4s"                            %% "http4s-ember-client"   % "0.23.24",
        "co.fs2"                                %% "fs2-core"              % "3.9.3",
        "org.scalatest"                         %% "scalatest"             % "3.2.17",
        "com.lihaoyi"                           %% "upickle"               % "3.1.3",
        "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.26.2",
        "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.26.2" % "compile-internal",
        "org.json4s"                            %% "json4s-native"         % "4.1.0-M4",
      ),
    )
