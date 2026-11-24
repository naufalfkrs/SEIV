package com.seiv.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uji aturan scoring sesuai sumber: UW (PHQ-9, GAD-7), NIMH ASQ, CAPE-P15 (Núñez 2021). */
class ScreeningScoringTest {

    /** Jawaban "aman" untuk semua kuesioner, lalu diubah per kasus. */
    private fun base(): MutableList<MutableList<Int?>> = mutableListOf(
        MutableList(20) { 0 },                 // SRQ-20: Tidak
        MutableList<Int?>(10) { 0 }.also { it[9] = null }, // PHQ-9 + kesulitan
        MutableList<Int?>(8) { 0 }.also { it[7] = null },  // GAD-7 + kesulitan
        mutableListOf(0, 0, 0, 0, null, null),  // ASQ
        MutableList(15) { 0 },                 // CAPE-P15: "Tidak pernah"
        MutableList(10) { 4 },                 // CD-RISC-10: "Hampir selalu benar"
    )

    @Test fun phqBands() {
        assertEquals(Severity.MINIMAL, phqSeverity(4))
        assertEquals(Severity.RINGAN, phqSeverity(5))
        assertEquals(Severity.RINGAN, phqSeverity(9))
        assertEquals(Severity.SEDANG, phqSeverity(10))
        assertEquals(Severity.SEDANG_BERAT, phqSeverity(15))
        assertEquals(Severity.BERAT, phqSeverity(20))
        assertEquals(Severity.BERAT, phqSeverity(27))
    }

    @Test fun gadBands() {
        assertEquals(Severity.MINIMAL, gadSeverity(4))
        assertEquals(Severity.RINGAN, gadSeverity(9))
        assertEquals(Severity.SEDANG, gadSeverity(14))
        assertEquals(Severity.BERAT, gadSeverity(15))
    }

    @Test fun phqAndGadAreSums() {
        val a = base()
        a[1] = mutableListOf(1, 2, 3, 0, 0, 0, 0, 0, 1, 2)
        a[2] = mutableListOf(3, 3, 2, 0, 0, 0, 0, null)
        val r = scoreScreening(a)
        assertEquals(7, r.phq)
        assertEquals(1, r.phqItem9)
        assertEquals(2, r.phqDifficulty) // dicatat, tidak dihitung ke skor
        assertEquals(8, r.gad)
        assertEquals(3, r.careLevel())   // butir 9 > 0 → perlu penilaian risiko
    }

    @Test fun gadEightNeedsFurtherAssessment() {
        val a = base(); a[2] = mutableListOf(2, 2, 2, 2, 0, 0, 0, null)
        assertEquals(2, scoreScreening(a).careLevel())
        a[2] = mutableListOf(2, 2, 2, 1, 0, 0, 0, null)
        assertEquals(1, scoreScreening(a).careLevel())
    }

    @Test fun asqNegative() {
        val r = scoreScreening(base())
        assertEquals(Severity.NEGATIF, asqSeverity(r))
        assertEquals(0, r.careLevel())
    }

    @Test fun asqNonAcutePositive() {
        val a = base(); a[3] = mutableListOf(1, 0, 0, 0, null, 0)
        val r = scoreScreening(a)
        assertEquals(Severity.POSITIF, asqSeverity(r))
        assertEquals(3, r.careLevel())
    }

    @Test fun asqAcutePositive() {
        val a = base(); a[3] = mutableListOf(0, 0, 1, 0, null, 1)
        val r = scoreScreening(a)
        assertEquals(Severity.AKUT, asqSeverity(r))
        assertTrue(r.asqAcute == true)
    }

    @Test fun asqPastAttemptException() {
        val a = base(); a[3] = mutableListOf(0, 0, 0, 1, 1, 0) // percobaan > 1 tahun lalu
        val r = scoreScreening(a)
        assertEquals(Severity.POSITIF, asqSeverity(r))
        assertTrue(asqOnlyPastAttempt(r))
        a[3] = mutableListOf(0, 0, 0, 1, 0, 0) // dalam 12 bulan terakhir
        assertFalse(asqOnlyPastAttempt(scoreScreening(a)))
    }

    @Test fun capeScoredOneToFiveAndAveraged() {
        val r = scoreScreening(base())
        assertEquals(15, r.cape)          // semua "Tidak pernah" = 1 × 15
        assertEquals(1.0, capeMean(r.cape!!), 1e-9)
        assertEquals(Severity.CAPE_NEGATIF, capeSeverity(r.cape!!))
        assertEquals(5, r.capePi); assertEquals(7, r.capeBe); assertEquals(3, r.capePa)
    }

    @Test fun capeCutoff147() {
        // rata-rata 1,47 → total 22,05; total 22 (1,467) negatif, total 23 (1,533) positif
        assertEquals(Severity.CAPE_NEGATIF, capeSeverity(22))
        assertEquals(Severity.CAPE_POSITIF, capeSeverity(23))
        val a = base(); a[4] = MutableList(15) { if (it < 8) 1 else 0 } // 8 butir "Jarang"
        val r = scoreScreening(a)
        assertEquals(23, r.cape)
        assertEquals(Severity.CAPE_POSITIF, capeSeverity(r.cape!!))
        assertEquals(2, r.careLevel())
    }

    @Test fun cdriscIsSumZeroToForty() {
        assertEquals(40, scoreScreening(base()).cdrisc)
        val a = base(); a[5] = MutableList(10) { 0 }
        val r = scoreScreening(a)
        assertEquals(0, r.cdrisc)
        assertEquals(Severity.RESILIEN_RENDAH, cdriscSeverity(r.cdrisc!!))
    }
}
