package com.readplus.data.source

class NaturalOrderComparator : Comparator<String> {
    private val regex = Regex("(\\d+|\\D+)")
    override fun compare(a: String, b: String): Int {
        val partsA = regex.findAll(a).map { it.value }.toList()
        val partsB = regex.findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(partsA.size, partsB.size)) {
            val pa = partsA[i]; val pb = partsB[i]
            val cmp = if (pa.all { it.isDigit() } && pb.all { it.isDigit() }) {
                pa.toLong().compareTo(pb.toLong())
            } else {
                pa.compareTo(pb, ignoreCase = true)
            }
            if (cmp != 0) return cmp
        }
        return partsA.size - partsB.size
    }
}