import org.typelevel.scalacoptions.ScalacOptions
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy

val scala3Version = "3.7.4"

val catsEffectVersion     = "3.5.4"
val catsVersion           = "2.12.0"
val circeCoreVersion      = "0.14.10"
val cuttlefishVersion     = "1.0.0"
val declineVersion        = "2.4.1"
val declinioVersion       = "1.0.0"
val ducktapeVersion       = "0.1.11"
val fs2ThrottlerVersion   = "1.0.12"
val fs2Version            = "3.9.3"
val h4sblVersion          = "1.0.0"
val http4sClientVersion   = "0.23.24"
val log4catsVersion       = "2.7.0"
val logbackClassicVersion = "1.3.4"
val pureconfigVersion     = "0.17.4"
val scalatestVersion      = "3.2.17"
val tado4sVersion         = "1.0.0"
val timefluxVersion       = "1.0.0"

Global / run / fork           := true
Global / onChangedBuildSource := ReloadOnSourceChanges

Global / tpolecatExcludeOptions ++= Set(ScalacOptions.warnUnusedLocals)
Global / tpolecatExcludeOptions ++= Set(ScalacOptions.warnUnusedImports)
Test / tpolecatScalacOptions     := Set.empty

addCommandAlias("styleApply", "; scalafix OrganizeImports; scalafmtAll")
addCommandAlias("styleCheck", "; scalafix --check; scalafmtCheckAll")

lazy val root =
  project
    .in(file("."))
    .settings(
      name                 := "homedata",
      version              := "0.1.0",
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
      semanticdbEnabled                := true,
      semanticdbVersion                := scalafixSemanticdb.revision,
      assembly / mainClass             := Some("com.colofabrix.scala.homedata.Main"),
      assembly / assemblyJarName       := s"homedata_${version.value}_${scalaVersion.value}.jar",
      assembly / test                  := {},
      assembly / assemblyMergeStrategy := {
        case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
        case path                                    => (ThisBuild / assemblyMergeStrategy).value(path)
      },
    )
