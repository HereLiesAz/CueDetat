package com.hereliesaz.cuedetat.billing

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FossEntitlementRepositoryTest {

    @Test
    fun reportsActiveWithFossSource() {
        val repo = FossEntitlementRepository()
        val entitlement = repo.entitlement.value
        assertTrue("FOSS must always report entitled", entitlement.active)
        assertEquals(EntitlementSource.FOSS_BUILD, entitlement.source)
    }

    @Test
    fun productDetailsEmitsNotApplicable() = runBlocking {
        val repo = FossEntitlementRepository()
        val first = repo.productDetails().first()
        assertEquals(ProductDetailsState.NotApplicable, first)
    }
}
