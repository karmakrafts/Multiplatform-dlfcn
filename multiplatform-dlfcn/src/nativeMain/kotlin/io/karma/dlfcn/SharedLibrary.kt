/*
 * Copyright 2024 Karma Krafts & associates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalForeignApi::class)

package io.karma.dlfcn

import dlfcn.RTLD_LAZY
import dlfcn.RTLD_NOW
import dlfcn.dlclose
import dlfcn.dlopen
import dlfcn.dlsym
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret

internal expect val C_STD_LIB: String

enum class LinkMode(internal val flag: Int) {
    // @formatter:off
    IMMEDIATE(RTLD_NOW),
    LAZY     (RTLD_LAZY)
    // @formatter:on
}

class SharedLibrary internal constructor(
    val name: String,
    val linkMode: LinkMode,
    val handle: CValuesRef<*>,
) : AutoCloseable {
    companion object {
        fun open(
            name: String,
            linkMode: LinkMode = LinkMode.LAZY
        ): SharedLibrary? {
            val handle = dlopen(name, linkMode.flag)
            return if (handle == null) null
            else SharedLibrary(name, linkMode, handle)
        }

        fun openCStdLib(): SharedLibrary? = open(C_STD_LIB)
    }

    override fun close() {
        dlclose(handle)
    }

    fun findFunctionOrNull(name: String): COpaquePointer? = dlsym(handle, name)

    inline fun <reified T : Function<*>> findFunctionOrNull(name: String): CPointer<CFunction<T>>? {
        return findFunctionOrNull(name)?.reinterpret()
    }

    inline fun <reified T : Function<*>> findFunction(name: String): CPointer<CFunction<T>> {
        return requireNotNull(findFunctionOrNull(name)) { "Could not find function ${this.name}:$name" }.reinterpret()
    }
}