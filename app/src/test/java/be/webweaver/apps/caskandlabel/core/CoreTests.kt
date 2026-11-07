package be.webweaver.apps.caskandlabel.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale

class ScoreTest {
    @Test fun validRange() {
        assertEquals(19, Score.all.size)
        assertNull(Score.ofHalfSteps(1))
        assertNull(Score.ofHalfSteps(21))
        assertEquals(15, Score.of(7.5)?.halfSteps)
        assertNull(Score.of(7.25))
        assertNull(Score.of(0.5))
    }

    @Test fun formatsWithLocaleComma() {
        val nl = Locale.forLanguageTag("nl-BE")
        assertEquals("7,5", Score.ofHalfSteps(15)!!.format(nl))
        assertEquals("8", Score.ofHalfSteps(16)!!.format(nl))
        assertEquals("7.5", Score.ofHalfSteps(15)!!.format(Locale.ENGLISH))
    }

    @Test fun averages() {
        val nl = Locale.forLanguageTag("nl-BE")
        assertNull(Score.formatAverage(emptyList(), nl))
        assertEquals("7", Score.formatAverage(listOf(14), nl))
        assertEquals("7,3", Score.formatAverage(listOf(14, 15, 15), nl))
    }
}

class BottleTitleTest {
    private fun title(name: String, age: Int?) = BottleTitle.title(name, age) { "$it jaar" }

    @Test fun addsAgeOnlyWhenMissing() {
        assertEquals("Glenmorangie The Lasanta 15 jaar", title("Glenmorangie The Lasanta", 15))
        assertEquals("Glenmorangie The Lasanta 15", title("Glenmorangie The Lasanta 15", 15))
        assertEquals("Talisker 2015 Distillers 10 jaar", title("Talisker 2015 Distillers", 10))
        assertEquals("Ardbeg Uigeadail", title("Ardbeg Uigeadail", null))
    }
}

class InviteCodeTest {
    @Test fun parsesAllForms() {
        assertEquals("K7M2QX", InviteCode.parse("K7M2QX"))
        assertEquals("K7M2QX", InviteCode.parse(" k7m-2qx "))
        assertEquals("K7M2QX", InviteCode.parse("caskandlabel://join/K7M2QX"))
        assertTrue(InviteCode.isComplete("k7m 2qx"))
        assertFalse(InviteCode.isComplete("K7M2Q"))
        assertEquals("K7M-2QX", InviteCode.formatted("K7M2QX"))
    }
}

class VocabularyTest {
    @Test fun flavorProfileRules() {
        assertEquals("Krachtig & Rokerig", Vocabulary.flavorProfile(true, emptyList(), emptyList()))
        assertEquals("Vol & Rond", Vocabulary.flavorProfile(false, listOf("Sherry Cask"), emptyList()))
        assertEquals("Fruitig & Kruidig", Vocabulary.flavorProfile(null, listOf("Bourbon Cask"), listOf("citrus")))
        assertEquals("Licht & Bloemig", Vocabulary.flavorProfile(null, emptyList(), listOf("bloemig")))
        assertNull(Vocabulary.flavorProfile(null, emptyList(), emptyList()))
    }
}

class JournalTest {
    private val t0 = Instant.parse("2026-11-01T20:00:00Z")
    private fun tasting(id: String, bottle: String, title: String, score: Int, days: Long, group: String? = null) =
        JournalTasting(id, bottle, title, "u1", group, score, t0.plusSeconds(days * 86_400))

    private val tastings = listOf(
        tasting("1", "b1", "Ardbeg 10", 16, 0),
        tasting("2", "b1", "Ardbeg 10", 14, 2, group = "g1"),
        tasting("3", "b2", "Aberlour 12", 18, 1, group = "g2"),
    )

    @Test fun oneRowPerBottleNewestFirst() {
        val rows = Journal.rows(tastings)
        assertEquals(listOf("b1", "b2"), rows.map { it.bottleId })
        assertEquals(2, rows[0].count)
        assertEquals(15.0, rows[0].averageHalfSteps, 0.0)
    }

    @Test fun scopeKeepsGroupsApart() {
        assertEquals(listOf("b1"), Journal.rows(tastings, GroupScope.Private).map { it.bottleId })
        assertEquals(1, Journal.rows(tastings, GroupScope.Group("g1")).single().count)
        assertEquals(listOf("b2"), Journal.rows(tastings, GroupScope.Group("g2")).map { it.bottleId })
    }

    @Test fun searchAndSort() {
        assertEquals(listOf("b2"), Journal.rows(tastings, query = "aber").map { it.bottleId })
        assertEquals(listOf("b2", "b1"), Journal.rows(tastings, sort = JournalSort.SCORE).map { it.bottleId })
        assertEquals(listOf("b2", "b1"), Journal.rows(tastings, sort = JournalSort.NAME).map { it.bottleId })
    }
}
