package com.alad1nks.oquturbo.feature.memorygrid.model

import com.alad1nks.oquturbo.feature.memorygrid.navigation.MemoryGridRoute
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class MemoryGridSerializationTest {
    @Test
    fun navigationTypeLookupFindsSerializerAndPreservesModeNames() {
        val descriptor = serializer(typeOf<MemoryGridGameMode>()).descriptor

        assertEquals(SerialKind.ENUM, descriptor.kind)
        assertEquals(3, descriptor.elementsCount)
        assertEquals(
            listOf("Route", "Reverse", "Flash"),
            (0 until descriptor.elementsCount).map(descriptor::getElementName),
        )
    }

    @Test
    fun typedRouteModeMatchesRuntimeTypeLookup() {
        val route = MemoryGridRoute.serializer().descriptor
        val mode = serializer(typeOf<MemoryGridGameMode>()).descriptor

        assertEquals(1, route.elementsCount)
        assertEquals("mode", route.getElementName(0))
        assertEquals(mode, route.getElementDescriptor(0))
    }
}
