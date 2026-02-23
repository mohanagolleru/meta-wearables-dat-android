/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

import java.util.Properties
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import org.gradle.api.GradleException

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val localProperties =
  Properties().apply {
    val localPropertiesPath = rootDir.toPath() / "local.properties"
    if (localPropertiesPath.exists()) {
      load(localPropertiesPath.inputStream())
    }
  }

// GitHub Packages creds (prefer env vars, fallback to local.properties)
val githubUser: String? =
  System.getenv("GITHUB_ACTOR")
    ?: System.getenv("USERNAME")
    ?: localProperties.getProperty("github_user")

val githubToken: String? =
  System.getenv("GITHUB_TOKEN")
    ?: System.getenv("TOKEN")
    ?: localProperties.getProperty("github_token")

// Fail early with a clean message instead of a confusing Gradle auth error
if (githubUser.isNullOrBlank() || githubToken.isNullOrBlank()) {
  throw GradleException(
    """
    Missing GitHub Packages credentials.

    Set either:
      - env vars: GITHUB_ACTOR and GITHUB_TOKEN
    or:
      - local.properties: github_user=YOUR_GITHUB_USERNAME, github_token=YOUR_TOKEN

    Token needs at least read:packages. Do not commit local.properties.
    """.trimIndent()
  )
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
      credentials {
        username = githubUser
        password = githubToken
      }
    }
  }
}

rootProject.name = "CameraAccess"
include(":app")

