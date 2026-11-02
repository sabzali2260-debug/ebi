package com.separ.alert

/**
 * Source-aware alert gate.
 *
 * A radar track gap, a third-party map label, and an operator risk bulletin must never
 * be promoted into a verified closure of the entire Tehran FIR.
 */
enum class NoticeSource {
    IRAN_AIM_OFFICIAL,
    EASA_OFFICIAL,
    SECONDARY_AGGREGATOR,
    FLIGHTRADAR24
}

enum class NoticeKind {
    INFORMATION,
    RESTRICTION,
    RISK_ADVISORY,
    CLOSURE,
    UNKNOWN
}

enum class NoticeScope {
    AIRPORT,
    LOCAL_AREA,
    WHOLE_FIR,
    UNKNOWN
}

data class AirspaceNotice(
    val id: String,
    val source: NoticeSource,
    val firCode: String,
    val kind: NoticeKind,
    val scope: NoticeScope,
    val validFromEpochMillis: Long,
    val validUntilEpochMillis: Long,
    val title: String,
    val sourceReference: String
)

data class AlertDecision(
    val accepted: Boolean,
    val level: Int,
    val reason: String
)

object AirspaceNoticePolicy {
    private const val TEHRAN_FIR = "OIIX"

    fun evaluate(notice: AirspaceNotice, nowEpochMillis: Long = System.currentTimeMillis()): AlertDecision {
        if (notice.id.isBlank() || notice.title.isBlank() || notice.sourceReference.isBlank()) {
            return reject("شناسه یا ارجاع منبع ناقص است.")
        }
        if (notice.validFromEpochMillis <= 0L || notice.validUntilEpochMillis <= notice.validFromEpochMillis) {
            return reject("بازهٔ اعتبار اطلاعیه معتبر نیست.")
        }
        if (nowEpochMillis < notice.validFromEpochMillis) return reject("اعتبار اطلاعیه هنوز شروع نشده است.")
        if (nowEpochMillis >= notice.validUntilEpochMillis) return reject("اطلاعیه منقضی شده است.")
        if (!notice.firCode.equals(TEHRAN_FIR, ignoreCase = true)) {
            return reject("اطلاعیه به FIR تهران (OIIX) مربوط نیست.")
        }

        return when (notice.source) {
            NoticeSource.FLIGHTRADAR24 ->
                reject("دادهٔ رادار پرواز برای تشخیص هشدار ایمنی یا بسته‌شدن حریم هوایی مجاز نیست.")

            NoticeSource.SECONDARY_AGGREGATOR ->
                when (notice.kind) {
                    NoticeKind.INFORMATION -> accept(1, "منبع ثانویه؛ فقط اطلاع‌رسانی.")
                    NoticeKind.RESTRICTION, NoticeKind.RISK_ADVISORY ->
                        accept(1, "منبع ثانویه؛ نیازمند تطبیق با منبع رسمی.")
                    NoticeKind.CLOSURE ->
                        reject("منبع ثانویه به‌تنهایی نمی‌تواند بسته‌شدن حریم هوایی را تأیید کند.")
                    NoticeKind.UNKNOWN -> reject("نوع اطلاعیه قابل تشخیص نیست.")
                }

            NoticeSource.EASA_OFFICIAL ->
                when (notice.kind) {
                    NoticeKind.RISK_ADVISORY -> accept(2, "بولتن ریسک رسمی برای اپراتورهای مشمول؛ دستور عمومی بستن FIR نیست.")
                    NoticeKind.INFORMATION -> accept(1, "اطلاعیهٔ رسمی؛ نه اعلام بسته‌شدن FIR.")
                    NoticeKind.RESTRICTION, NoticeKind.CLOSURE ->
                        reject("بولتن EASA جایگزین NOTAM صادرشده توسط مرجع AIS برای اعلام بسته‌شدن FIR نیست.")
                    NoticeKind.UNKNOWN -> reject("نوع بولتن EASA قابل تشخیص نیست.")
                }

            NoticeSource.IRAN_AIM_OFFICIAL ->
                when (notice.kind) {
                    NoticeKind.INFORMATION -> accept(1, "اطلاعیهٔ رسمی Iran AIM.")
                    NoticeKind.RESTRICTION, NoticeKind.RISK_ADVISORY ->
                        accept(2, "محدودیت/هشدار رسمی؛ دامنهٔ آن باید روی نقشهٔ منطقه‌ای دیده شود.")
                    NoticeKind.CLOSURE -> {
                        if (notice.scope == NoticeScope.WHOLE_FIR) {
                            accept(3, "بسته‌شدن کل FIR فقط با اطلاعیهٔ رسمی و دامنهٔ سراسری تأیید شده است.")
                        } else {
                            accept(2, "بسته‌شدن محلی/فرودگاهی؛ به معنی بسته‌شدن کل آسمان ایران نیست.")
                        }
                    }
                    NoticeKind.UNKNOWN -> reject("نوع NOTAM رسمی به‌طور مطمئن طبقه‌بندی نشده است.")
                }
        }
    }

    private fun accept(level: Int, reason: String) = AlertDecision(true, level, reason)
    private fun reject(reason: String) = AlertDecision(false, 0, reason)
}
