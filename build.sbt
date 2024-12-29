import org.typelevel.scalacoptions.ScalacOptions

val scala3Version = "3.6.2"

val catsEffectVersion     = "3.5.2"
val catsVersion           = "2.10.0"
val circeCoreVersion      = "0.14.10"
val circeGenericVersion   = "0.14.10"
val coreVersion           = "4.0.0-M8"
val ducktapeVersion       = "0.1.11"
val enumeratumVersion     = "1.7.5"
val fs2ThrottlerVersion   = "1.0.12"
val fs2Version            = "3.9.3"
val http4sClientVersion   = "0.23.24"
val json4sNativeVersion   = "4.1.0-M4"
val log4catsCoreVersion   = "2.7.0"
val logbackClassicVersion = "1.3.4"
val pureconfigCatsVersion = "0.17.4"
val scalatestVersion      = "3.2.17"
val sttpVersion           = "4.0.0-M8"

Global / run / fork           := true
Global / onChangedBuildSource := ReloadOnSourceChanges

Global / tpolecatExcludeOptions ++= Set(ScalacOptions.warnUnusedLocals)
homedata / Test / tpolecatScalacOptions := Set.empty
tado4s / Test / tpolecatScalacOptions   := Set.empty
timeflux / Test / tpolecatScalacOptions := Set.empty

lazy val root =
  project
    .in(file("."))
    .aggregate(homedata, timeflux, tado4s)
    .settings(
      name              := "root",
      version           := "0.1.0",
      organization      := "com.colofabrix.scala",
      scalaVersion      := scala3Version,
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision,
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
        "ch.qos.logback"                 % "logback-classic" % logbackClassicVersion,
        "co.fs2"                        %% "fs2-core"        % fs2Version,
        "co.fs2"                        %% "fs2-io"          % fs2Version,
        "com.github.pureconfig"         %% "pureconfig-cats" % pureconfigCatsVersion,
        "com.softwaremill.sttp.client4" %% "core"            % sttpVersion,
        "dev.kovstas"                   %% "fs2-throttler"   % fs2ThrottlerVersion,
        "io.github.arainko"             %% "ducktape"        % ducktapeVersion,
        "org.http4s"                    %% "http4s-client"   % http4sClientVersion,
        "org.json4s"                    %% "json4s-native"   % json4sNativeVersion,
        "org.scalatest"                 %% "scalatest"       % scalatestVersion % Test,
        "org.typelevel"                 %% "cats-core"       % catsVersion,
        "org.typelevel"                 %% "cats-effect"     % catsEffectVersion,
        "org.typelevel"                 %% "log4cats-core"   % log4catsCoreVersion,
      ),
    )

lazy val timeflux =
  project
    .in(file("timeflux"))
    .settings(
      name         := "timeflux",
      version      := "0.1.0",
      organization := "com.colofabrix.scala.timeflux",
      scalaVersion := scala3Version,
      libraryDependencies ++= List(
        "ch.qos.logback"         % "logback-classic"     % logbackClassicVersion,
        "co.fs2"                %% "fs2-core"            % fs2Version,
        "com.github.pureconfig" %% "pureconfig-cats"     % pureconfigCatsVersion,
        "io.circe"              %% "circe-core"          % circeCoreVersion,
        "io.circe"              %% "circe-generic"       % circeCoreVersion,
        "io.circe"              %% "circe-parser"        % circeCoreVersion,
        "io.github.arainko"     %% "ducktape"            % ducktapeVersion,
        "org.http4s"            %% "http4s-circe"        % http4sClientVersion,
        "org.http4s"            %% "http4s-ember-client" % http4sClientVersion,
        "org.scalatest"         %% "scalatest"           % scalatestVersion % Test,
        "org.typelevel"         %% "cats-core"           % catsVersion,
        "org.typelevel"         %% "cats-effect"         % catsEffectVersion,
        "org.typelevel"         %% "log4cats-core"       % log4catsCoreVersion,
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
        "ch.qos.logback"         % "logback-classic"     % logbackClassicVersion,
        "com.beachape"          %% "enumeratum"          % enumeratumVersion,
        "com.beachape"          %% "enumeratum-circe"    % enumeratumVersion,
        "com.github.pureconfig" %% "pureconfig-cats"     % pureconfigCatsVersion,
        "io.circe"              %% "circe-core"          % circeCoreVersion,
        "io.circe"              %% "circe-generic"       % circeCoreVersion,
        "io.circe"              %% "circe-parser"        % circeCoreVersion % Test,
        "io.github.arainko"     %% "ducktape"            % ducktapeVersion,
        "org.http4s"            %% "http4s-circe"        % http4sClientVersion,
        "org.http4s"            %% "http4s-client"       % http4sClientVersion,
        "org.http4s"            %% "http4s-dsl"          % http4sClientVersion,
        "org.http4s"            %% "http4s-ember-client" % http4sClientVersion,
        "org.scalatest"         %% "scalatest"           % scalatestVersion % Test,
        "org.typelevel"         %% "cats-core"           % catsVersion,
        "org.typelevel"         %% "cats-effect"         % catsEffectVersion,
        "org.typelevel"         %% "log4cats-core"       % log4catsCoreVersion,
      ),
    )
