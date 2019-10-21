package ch.deletescape.shortid.utils

/**
 * Searches this char array for any duplicates using a bit map
 */
fun CharArray.hasDuplicates(): Boolean {
    val bitmap = BooleanArray(256)
    forEach {
        if (bitmap[it.toInt()]) return true
        else bitmap[it.toInt()] = true
    }
    return false
}