import org.typelevel.scalacoptions.ScalacOptions
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy

val scala3Version = "3.8.4"

val catsEffectVersion     = "3.6.1"
val catsVersion           = "2.13.0"
val circeCoreVersion      = "0.14.13"
val cuttlefishVersion     = "2.0.0"
val declineVersion        = "2.5.0"
val declinioVersion       = "2.0.0"
val fs2ThrottlerVersion   = "1.0.14"
val fs2Version            = "3.12.0"
val h4sblVersion          = "1.2.0"
val http4sClientVersion   = "0.23.30"
val log4catsVersion       = "2.7.1"
val logbackClassicVersion = "1.5.18"
val pureconfigVersion     = "0.17.9"
val scalatestVersion      = "3.2.19"
val tado4sVersion         = "2.0.0"
val timefluxVersion       = "2.0.0"

Global / run / fork           := true
Global / onChangedBuildSource := ReloadOnSourceChanges

Global / tpolecatExcludeOptions ++= Set(ScalacOptions.warnUnusedLocals)
Global / tpolecatExcludeOptions ++= Set(ScalacOptions.warnUnusedImports)
Test / tpolecatScalacOptions     := Set.empty

addCommandAlias(
  "styleApply",
  "; set ThisBuild / scalacOptions += \"-Wunused:all\"; scalafixEnable; scalafixAll; session clear; scalafmtAll",
)
addCommandAlias(
  "styleCheck",
  "; set ThisBuild / scalacOptions += \"-Wunused:all\"; scalafixEnable; scalafixAll --check; session clear; scalafmtCheckAll",
)

lazy val root =
  project
    .in(file("."))
    .settings(
      name                 := "homedata",
      version              := "0.2.0",
      organization         := "com.colofabrix.scala",
      scalaVersion         := scala3Version,
      scalacOptions        += "-preview",
      libraryDependencies ++= List(
        "ch.qos.logback"         % "logback-classic"    % logbackClassicVersion % Runtime,
        "co.fs2"                %% "fs2-core"           % fs2Version,
        "co.fs2"                %% "fs2-io"             % fs2Version,
        "com.colofabrix.scala"  %% "cuttlefish"         % cuttlefishVersion,
        "com.colofabrix.scala"  %% "declinio"           % declinioVersion,
        "com.colofabrix.scala"  %% "h4sbl"              % h4sblVersion,
        "com.colofabrix.scala"  %% "tado4s"             % tado4sVersion,
        "com.colofabrix.scala"  %% "timeflux"           % timefluxVersion,
        "com.github.pureconfig" %% "pureconfig-core"    % pureconfigVersion,
        "com.monovore"          %% "decline"            % declineVersion,
        "dev.kovstas"           %% "fs2-throttler"      % fs2ThrottlerVersion,
        "io.circe"              %% "circe-parser"       % circeCoreVersion      % Test,
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
      semanticdbEnabled                := true,
      semanticdbVersion                := scalafixSemanticdb.revision,
      assembly / mainClass             := Some("com.colofabrix.scala.homedata.Main"),
      assembly / assemblyJarName       := s"homedata_${version.value}_${scalaVersion.value}.jar",
      assembly / test                  := {},
      assembly / assemblyMergeStrategy := {
        case "module-info.class"                     => MergeStrategy.discard
        case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
        case path                                    => (ThisBuild / assemblyMergeStrategy).value(path)
      },
    )
