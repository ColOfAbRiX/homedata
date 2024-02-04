import org.typelevel.scalacoptions.ScalacOptions

val scala3Version = "3.3.1"

Compile / run / fork          := true
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / tpolecatExcludeOptions ++=
  Set(
    ScalacOptions.warnUnusedNoWarn,
    // ScalacOptions.warnUnusedImplicits,
    ScalacOptions.warnUnusedExplicits,
    ScalacOptions.warnUnusedImports,
    ScalacOptions.warnUnusedLocals,
    // ScalacOptions.warnUnusedParams,
    ScalacOptions.warnUnusedPatVars,
    ScalacOptions.warnUnusedPrivates,
  )

lazy val root =
  project
    .in(file("."))
    .aggregate(homedata, timeflux, tado4s, restbee)
    .settings(
      name         := "root",
      version      := "0.1.0",
      organization := "com.colofabrix.scala",
      scalaVersion := scala3Version,
    )

lazy val homedata =
  project
    .in(file("homedata"))
    .dependsOn(timeflux, tado4s)
    .settings(
      name         := "homedata",
      version      := "0.1.0",
      organization := "com.colofabrix.scala.homedata",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "co.fs2"                        %% "fs2-core"        % "3.9.3",
        "co.fs2"                        %% "fs2-io"          % "3.9.3",
        "com.github.pureconfig"         %% "pureconfig-cats" % "0.17.4",
        "com.github.valskalla"          %% "odin-core"       % "0.13.0",
        "com.softwaremill.sttp.client4" %% "core"            % "4.0.0-M8",
        "io.github.arainko"             %% "ducktape"        % "0.1.11",
        "org.http4s"                    %% "http4s-client"   % "0.23.24",
        "org.json4s"                    %% "json4s-native"   % "4.1.0-M4",
        "org.scalatest"                 %% "scalatest"       % "3.2.17" % Test,
        "org.typelevel"                 %% "cats-core"       % "2.10.0",
        "org.typelevel"                 %% "cats-effect"     % "3.5.2",
      ),
    )

lazy val restbee =
  project
    .in(file("restbee"))
    .settings(
      name         := "rest-bee",
      version      := "0.1.0",
      organization := "com.colofabrix.scala.restbee",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "co.fs2"        %% "fs2-core"      % "3.9.3",
        "org.http4s"    %% "http4s-client" % "0.23.24",
        "org.typelevel" %% "cats-core"     % "2.10.0",
        "org.typelevel" %% "cats-effect"   % "3.5.2",
      ),
    )

lazy val timeflux =
  project
    .in(file("timeflux"))
    .dependsOn(restbee)
    .settings(
      name         := "timeflux",
      version      := "0.1.0",
      organization := "com.colofabrix.scala.timeflux",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "co.fs2"               %% "fs2-core"            % "3.9.3",
        "com.github.valskalla" %% "odin-core"           % "0.13.0",
        "io.circe"             %% "circe-core"          % "0.14.6",
        "io.circe"             %% "circe-fs2"           % "0.14.1",
        "io.circe"             %% "circe-generic"       % "0.14.6",
        "io.circe"             %% "circe-parser"        % "0.14.6",
        "org.http4s"           %% "http4s-circe"        % "0.23.24",
        "org.http4s"           %% "http4s-ember-client" % "0.23.24",
        "org.scalatest"        %% "scalatest"           % "3.2.17" % Test,
        "org.typelevel"        %% "cats-core"           % "2.10.0",
        "org.typelevel"        %% "cats-effect"         % "3.5.2",
      ),
    )

lazy val tado4s =
  project
    .in(file("tado4s"))
    .settings(
      name         := "tado4s",
      version      := "0.1.0",
      organization := "com.colofabrix.scala.tado4s",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "com.github.pureconfig" %% "pureconfig-cats"     % "0.17.4",
        "com.github.valskalla"  %% "odin-core"           % "0.13.0",
        "io.circe"              %% "circe-core"          % "0.14.6",
        "io.circe"              %% "circe-generic"       % "0.14.6",
        "io.github.arainko"     %% "ducktape"            % "0.1.11",
        "org.http4s"            %% "http4s-circe"        % "0.23.24",
        "org.http4s"            %% "http4s-client"       % "0.23.24",
        "org.http4s"            %% "http4s-dsl"          % "0.23.24",
        "org.http4s"            %% "http4s-ember-client" % "0.23.24",
        "org.typelevel"         %% "cats-core"           % "2.10.0",
        "org.typelevel"         %% "cats-effect"         % "3.5.2",
        "org.scalatest"         %% "scalatest"           % "3.2.17" % Test,
      ),
    )
