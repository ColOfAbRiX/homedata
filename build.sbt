import org.typelevel.scalacoptions.ScalacOptions
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy

val scala3Version = "3.7.4"

val caseInsensitiveVersion = "1.4.0"
val catsEffectVersion      = "3.5.4"
val catsVersion            = "2.12.0"
val circeCoreVersion       = "0.14.10"
val circeGenericVersion    = "0.14.10"
val coreVersion            = "4.0.0-M8"
val cuttlefishVersion      = "1.0.0"
val declineVersion         = "2.4.1"
val declinioVersion        = "1.0.0"
val ducktapeVersion        = "0.1.11"
val enumeratumVersion      = "1.7.5"
val fs2DataVersion         = "1.11.2"
val fs2ThrottlerVersion    = "1.0.12"
val fs2Version             = "3.9.3"
val h4sbtVersion           = "1.0.0"
val http4sClientVersion    = "0.23.24"
val json4sNativeVersion    = "4.1.0-M4"
val log4catsVersion        = "2.7.0"
val logbackClassicVersion  = "1.3.4"
val pureconfigVersion      = "0.17.4"
val scalatestVersion       = "3.2.17"
val scodecBitsVersion      = "1.1.38"
val sttpVersion            = "4.0.0-M8"
val vaultVersion           = "3.5.0"

Global / run / fork           := true
Global / onChangedBuildSource := ReloadOnSourceChanges

Global / tpolecatExcludeOptions        ++= Set(ScalacOptions.warnUnusedLocals)
homedata / tpolecatExcludeOptions      ++= Set(ScalacOptions.warnUnusedImports)
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
      name                 := "homedata",
      version              := "0.1.0",
      organization         := "com.colofabrix.scala.homedata",
      scalaVersion         := scala3Version,
      scalacOptions        += "-preview",
      libraryDependencies ++= List(
        "ch.qos.logback"         % "logback-classic"    % logbackClassicVersion % Runtime,
        "co.fs2"                %% "fs2-core"           % fs2Version,
        "co.fs2"                %% "fs2-io"             % fs2Version,
        "com.colofabrix.scala"  %% "cuttlefish"         % cuttlefishVersion,
        "com.colofabrix.scala"  %% "declinio"           % declinioVersion,
        "com.colofabrix.scala"  %% "h4sbl"              % h4sbtVersion,
        "com.github.pureconfig" %% "pureconfig-core"    % pureconfigVersion,
        "com.monovore"          %% "decline"            % declineVersion,
        "dev.kovstas"           %% "fs2-throttler"      % fs2ThrottlerVersion,
        "io.circe"              %% "circe-parser"       % circeCoreVersion      % Test,
        "io.github.arainko"     %% "ducktape"           % ducktapeVersion,
        "org.http4s"            %% "http4s-core"        % http4sClientVersion,
        "org.scalatest"         %% "scalatest"          % scalatestVersion      % Test,
        "org.typelevel"         %% "cats-core"          % catsVersion,
        "org.typelevel"         %% "cats-effect-kernel" % catsEffectVersion,
        "org.typelevel"         %% "cats-effect-std"    % catsEffectVersion,
        "org.typelevel"         %% "cats-effect"        % catsEffectVersion,
        "org.typelevel"         %% "cats-kernel"        % catsVersion,
        "org.typelevel"         %% "log4cats-core"      % log4catsVersion,
        "org.typelevel"         %% "log4cats-slf4j"     % log4catsVersion,
      ),
      assembly / mainClass             := Some("com.colofabrix.scala.homedata.Main"),
      assembly / assemblyJarName       := s"homedata_${version.value}_${scalaVersion.value}.jar",
      assembly / test                  := {},
      assembly / assemblyMergeStrategy := {
        case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
        case path                                    => (ThisBuild / assemblyMergeStrategy).value(path)
      },
    )

lazy val timeflux =
  project
    .in(file("timeflux"))
    .settings(
      name                 := "timeflux",
      version              := "0.1.0",
      organization         := "com.colofabrix.scala.timeflux",
      scalaVersion         := scala3Version,
      scalacOptions        += "-preview",
      libraryDependencies ++= List(
        "co.fs2"                %% "fs2-core"            % fs2Version       % Runtime,
        "co.fs2"                %% "fs2-io"              % fs2Version,
        "com.colofabrix.scala"  %% "h4sbl"               % h4sbtVersion,
        "com.github.pureconfig" %% "pureconfig-core"     % pureconfigVersion,
        "io.circe"              %% "circe-core"          % circeCoreVersion,
        "io.github.arainko"     %% "ducktape"            % ducktapeVersion,
        "org.gnieh"             %% "fs2-data-csv"        % fs2DataVersion,
        "org.gnieh"             %% "fs2-data-text"       % fs2DataVersion,
        "org.http4s"            %% "http4s-circe"        % http4sClientVersion,
        "org.http4s"            %% "http4s-client"       % http4sClientVersion,
        "org.http4s"            %% "http4s-core"         % http4sClientVersion,
        "org.http4s"            %% "http4s-ember-client" % http4sClientVersion,
        "org.scalatest"         %% "scalatest"           % scalatestVersion % Test,
        "org.scodec"            %% "scodec-bits"         % scodecBitsVersion,
        "org.typelevel"         %% "case-insensitive"    % caseInsensitiveVersion,
        "org.typelevel"         %% "cats-core"           % catsVersion,
        "org.typelevel"         %% "cats-effect-kernel"  % catsEffectVersion,
        "org.typelevel"         %% "cats-effect-std"     % catsEffectVersion,
        "org.typelevel"         %% "cats-effect"         % catsEffectVersion,
        "org.typelevel"         %% "cats-kernel"         % catsVersion,
        "org.typelevel"         %% "log4cats-core"       % log4catsVersion,
        "org.typelevel"         %% "log4cats-slf4j"      % log4catsVersion,
      ),
    )

lazy val tado4s =
  project
    .in(file("tado4s"))
    .settings(
      name                 := "tado4s",
      version              := "0.1.0",
      organization         := "com.colofabrix.scala.tado4s",
      scalaVersion         := scala3Version,
      scalacOptions        += "-preview",
      libraryDependencies ++= List(
        "co.fs2"                %% "fs2-core"            % fs2Version,
        "co.fs2"                %% "fs2-io"              % fs2Version,
        "com.beachape"          %% "enumeratum-circe"    % enumeratumVersion,
        "com.beachape"          %% "enumeratum"          % enumeratumVersion,
        "com.colofabrix.scala"  %% "h4sbl"               % h4sbtVersion,
        "com.github.pureconfig" %% "pureconfig-core"     % pureconfigVersion,
        "io.circe"              %% "circe-core"          % circeCoreVersion,
        "io.circe"              %% "circe-parser"        % circeCoreVersion % Test,
        "org.http4s"            %% "http4s-circe"        % http4sClientVersion,
        "org.http4s"            %% "http4s-client"       % http4sClientVersion,
        "org.http4s"            %% "http4s-core"         % http4sClientVersion,
        "org.http4s"            %% "http4s-ember-client" % http4sClientVersion,
        "org.scalatest"         %% "scalatest"           % scalatestVersion % Test,
        "org.scodec"            %% "scodec-bits"         % scodecBitsVersion,
        "org.typelevel"         %% "case-insensitive"    % caseInsensitiveVersion,
        "org.typelevel"         %% "cats-core"           % catsVersion,
        "org.typelevel"         %% "cats-effect-kernel"  % catsEffectVersion,
        "org.typelevel"         %% "cats-effect-std"     % catsEffectVersion,
        "org.typelevel"         %% "cats-effect"         % catsEffectVersion,
        "org.typelevel"         %% "cats-kernel"         % catsVersion,
        "org.typelevel"         %% "log4cats-core"       % log4catsVersion,
        "org.typelevel"         %% "log4cats-slf4j"      % log4catsVersion,
      ),
    )
