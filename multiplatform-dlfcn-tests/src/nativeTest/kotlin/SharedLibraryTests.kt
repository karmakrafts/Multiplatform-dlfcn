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

import io.karma.dlfcn.SharedLibrary
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import platform.posix.size_t
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private fun MemScope.allocCString(value: String): CPointer<ByteVar> {
    return allocArrayOf(value.encodeToByteArray())
}

@Test
fun `Load and unload of libc`() {
    val lib = SharedLibrary.openCStdLib()
    assertNotNull(lib)
    lib.close()
}

@Test
fun `Perform various calls into libc`() = memScoped {
    val lib = SharedLibrary.openCStdLib()
    assertNotNull(lib)
    val buffer = allocArray<ByteVar>(64)
    lib.findFunction<(CPointer<ByteVar>, CPointer<ByteVar>, CPointer<ByteVar>) -> Int>("sprintf")(
        buffer,
        allocCString("%s from C!"),
        allocCString("Hello world")
    )
    val length = lib.findFunction<(CPointer<ByteVar>) -> size_t>("strlen")(buffer)
    val bufferValue = ByteArray(length.convert()).apply {
        usePinned { pinnedArray ->
            memcpy(pinnedArray.addressOf(0), buffer, length)
        }
    }.decodeToString()
    assertEquals("Hello world from C!", bufferValue)
    lib.close()
}