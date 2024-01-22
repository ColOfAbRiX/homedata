val scala3Version = "3.3.1"

lazy val root =
  project
    .in(file("."))
    .settings(
      name         := "homeData",
      version      := "0.1.0",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "co.fs2"                        %% "fs2-core"      % "3.9.3",
        "co.fs2"                        %% "fs2-io"        % "3.9.3",
        "org.json4s"                    %% "json4s-native" % "4.1.0-M4",
        "com.softwaremill.sttp.client4" %% "core"          % "4.0.0-M8",
        "io.github.arainko"             %% "ducktape"      % "0.1.11",
        "org.scalameta"                 %% "munit"         % "0.7.29" % Test,
        "org.typelevel"                 %% "cats-core"     % "2.10.0",
        "org.typelevel"                 %% "cats-effect"   % "3.5.2"
      )
    )
