package com.separ.alert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirspaceNoticePolicyTest {
    private fun notice(
        source: NoticeSource = NoticeSource.IRAN_AIM_OFFICIAL,
        kind: NoticeKind = NoticeKind.CLOSURE,
        scope: NoticeScope = NoticeScope.WHOLE_FIR,
        from: Long = 1_000L,
        until: Long = 5_000L
    ) = AirspaceNotice(
        id = "A1234/26",
        source = source,
        firCode = "OIIX",
        kind = kind,
        scope = scope,
        validFromEpochMillis = from,
        validUntilEpochMillis = until,
        title = "Test notice",
        sourceReference = "Iran AIM NOTAM"
    )

    @Test fun officialWholeFirClosureCanReachCriticalLevel() {
        val result = AirspaceNoticePolicy.evaluate(notice(), nowEpochMillis = 2_000L)
        assertTrue(result.accepted)
        assertEquals(3, result.level)
    }

    @Test fun localClosureCannotBecomeWholeFirCriticalAlert() {
        val result = AirspaceNoticePolicy.evaluate(
            notice(scope = NoticeScope.LOCAL_AREA), nowEpochMillis = 2_000L
        )
        assertTrue(result.accepted)
        assertEquals(2, result.level)
    }

    @Test fun fr24DataNeverTriggersSafetyAlert() {
        val result = AirspaceNoticePolicy.evaluate(
            notice(source = NoticeSource.FLIGHTRADAR24), nowEpochMillis = 2_000L
        )
        assertFalse(result.accepted)
    }

    @Test fun easaRiskBulletinIsNotAClosureOrder() {
        val result = AirspaceNoticePolicy.evaluate(
            notice(source = NoticeSource.EASA_OFFICIAL, kind = NoticeKind.RISK_ADVISORY),
            nowEpochMillis = 2_000L
        )
        assertTrue(result.accepted)
        assertEquals(2, result.level)
    }

    @Test fun expiredNotamIsRejected() {
        val result = AirspaceNoticePolicy.evaluate(notice(until = 1_900L), nowEpochMillis = 2_000L)
        assertFalse(result.accepted)
    }

    @Test fun differentFirIsRejected() {
        val n = notice().copy(firCode = "ORBB")
        assertFalse(AirspaceNoticePolicy.evaluate(n, nowEpochMillis = 2_000L).accepted)
    }
}
