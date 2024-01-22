import org.typelevel.scalacoptions.ScalacOptions

val scala3Version = "3.3.1"

Global / onChangedBuildSource  := ReloadOnSourceChanges
Compile / run / fork           := true

Test / tpolecatExcludeOptions ++= ScalacOptions.warnUnusedOptions + ScalacOptions.warnNonUnitStatement
tpolecatDevModeOptions ~= { opts =>
  opts.filterNot(Set(ScalacOptions.warnUnusedOptions))
}

lazy val root =
  project
    .in(file("."))
    .dependsOn(timeflux, tado4s)
    .aggregate(timeflux, tado4s, restbee)
    .settings(
      name         := "homeData",
      version      := "0.1.0",
      organization := "com.colofabrix.scala.homedata",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "co.fs2"                        %% "fs2-core"        % "3.9.3",
        "co.fs2"                        %% "fs2-io"          % "3.9.3",
        "com.github.valskalla"          %% "odin-core"       % "0.13.0",
        "com.softwaremill.sttp.client4" %% "core"            % "4.0.0-M8",
        "io.github.arainko"             %% "ducktape"        % "0.1.11",
        "org.json4s"                    %% "json4s-native"   % "4.1.0-M4",
        "com.github.pureconfig"         %% "pureconfig-cats" % "0.17.4",
        "org.scalameta"                 %% "munit"           % "0.7.29" % Test,
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
        "com.github.valskalla"  %% "odin-core"           % "0.13.0",
        "com.github.pureconfig" %% "pureconfig-cats"     % "0.17.4",
        "io.github.arainko"     %% "ducktape"            % "0.1.11",
        "io.circe"              %% "circe-core"          % "0.14.6",
        "io.circe"              %% "circe-generic"       % "0.14.6",
        "org.http4s"            %% "http4s-circe"        % "0.23.24",
        "org.http4s"            %% "http4s-dsl"          % "0.23.24",
        "org.http4s"            %% "http4s-ember-client" % "0.23.24",
        "org.typelevel"         %% "cats-core"           % "2.10.0",
        "org.typelevel"         %% "cats-effect"         % "3.5.2",
        "org.scalatest"         %% "scalatest"           % "3.2.17" % Test,
      ),
    )
