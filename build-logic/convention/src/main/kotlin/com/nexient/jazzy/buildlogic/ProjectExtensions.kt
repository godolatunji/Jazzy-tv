package com.nexient.jazzy.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

internal object JazzySdk {
    const val COMPILE = 37
    const val MIN = 28
    const val TARGET = 37
    val JAVA = JavaVersion.VERSION_17
}

/** Namespace derived from the Gradle path: `:core:model` -> `com.nexient.jazzy.core.model`. */
internal fun Project.jazzyNamespace(): String =
    "com.nexient.jazzy" + path.replace(':', '.').replace('-', '_')
