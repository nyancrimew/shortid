// Copyright (c) 2019 Till Kottmann. All rights reserved.
// See the LICENSE file in the project root for licensing information.

// Go Implementation:
// Copyright (c) 2016-2017. Oleg Sklyar & teris.io: https://github.com/teris-io/shortid
// MIT-license as found in the LICENSE file.

// Original algorithm:
// Copyright (c) 2015 Dylan Greene, contributors: https://github.com/dylang/shortid.
// MIT-license as found in the LICENSE file.

// Seed computation: based on The Central Randomizer 1.3
// Copyright (c) 1997 Paul Houle (houle@msc.cornell.edu)
package ch.deletescape.shortid

import ch.deletescape.shortid.utils.hasDuplicates
import ch.deletescape.shortid.utils.nanoseconds
import ch.deletescape.shortid.utils.utcDate
import java.lang.Exception
import java.security.SecureRandom
import kotlin.math.log2
import kotlin.random.Random

/**
 * ShortId Generator with a given worker number [0,31], alphabet (64 unique symbols) and a seed value (used to shuffle the alphabet).
 * The worker number should be different for multiple or distributed processes generating Ids into the same data space.
 * The seed, on contrary, should be identical.
 */
class ShortId(
    val worker: Long,
    alphabet: String,
    seed: Long
) {
    val abc: ShortId.Abc
    /**
     * Ids can be generated for 34 years since this date
     */
    private val epoch = utcDate(2016, 0, 1, 0, 0).nanoseconds()
    /**
     * Ms since epoch for the last ID
     */
    private var ms: Long = 0
    /**
     * Request count within the same ms
     */
    private var count: Long = 0

    init {
        if (worker > 31 || worker < 0) {
            throw IllegalArgumentException("expected worker in the range [0,31]")
        }
        abc = Abc(alphabet, seed)
    }

    /**
     * Generate a new short Id.
     */
    fun generate() = generateInternal(null, epoch)

    // This should only be used for testing or via the public generate() fun
    internal fun generateInternal(tm: Long?, epoch: Long): String {
        val (ms, count) = getMsAndCounter(tm, epoch)
        var idChars = CharArray(9)
        // First 8 Symbols
        abc.encode(ms, 8, 5).copyInto(idChars)
        idChars[8] = abc.encode(worker, 1, 5)[0]
        if (count > 0) {
            // Extend only if really needed
            idChars += abc.encode(count, 0, 6)
        }
        return String(idChars)
    }

    /**
     * Get current ms and counter synchronized. Supplied times should be in Nanoseconds.
     */
    @Synchronized
    private fun getMsAndCounter(tm: Long?, epoch: Long): Pair<Long, Long> {
        val ms = if (tm != null) {
            (tm - epoch) / 1000000
        } else {
            (System.nanoTime() - epoch) / 1000000
        }
        if (ms == this.ms) {
            count++
        } else {
            count = 0
            this.ms = ms
        }
        return this.ms to this.count
    }

    override fun toString(): String {
        return "ShortId(worker=$worker, epoch=$epoch, abc=$abc)"
    }

    /**
     * Abc represents a shuffled alphabed to be used for Id representation
     */
    class Abc constructor(
        abc: String,
        seed: Long
    ) {
        private val alphabet: CharArray

        init {
            if (abc.length != ShortId.DefaultABC.length) {
                throw IllegalArgumentException("alphabet must contain ${ShortId.DefaultABC.length} unique characters")
            }
            val chrs = abc.toCharArray()
            if (chrs.hasDuplicates()) {
                throw IllegalArgumentException("alphabet must contain unique characters only")
            }
            alphabet = shuffle(chrs, seed)
        }

        /**
         * Encode encodes a given value into a slice of runes of length nsymbols. In case nsymbols==0, the
         * length of the result is automatically computed from data. Even if fewer symbols is required to
         * encode the data than nsymbols, all positions are used encoding 0 where required to guarantee
         * uniqueness in case further data is added to the sequence. The value of digits [4,6] represents
         * represents n in 2^n, which defines how much randomness flows into the algorithm: 4 -- every value
         * can be represented by 4 symbols in the alphabet (permitting at most 16 values), 5 -- every value
         * can be represented by 2 symbols in the alphabet (permitting at most 32 values), 6 -- every value
         * is represented by exactly 1 symbol with no randomness (permitting 64 values).
         */
        fun encode(value: Long, nSymbols: Int, digits: Int): CharArray {
            if (digits < 4 || 6 < digits) {
                throw IllegalArgumentException("allowed digits range [4,6], found $digits")
            }

            var computedSize = 1L
            if (value >= 1) {
                computedSize = log2(value.toFloat()).toLong() / digits + 1
            }
            val nSymb = when {
                nSymbols == 0 -> computedSize
                nSymbols < computedSize -> throw IllegalArgumentException("canott accommodate data, need $computedSize digits, got $nSymbols")
                else -> nSymbols.toLong()
            }.toInt()

            val mask = 1 shl digits - 1
            val random = IntArray(nSymb)
            // no random component if digits == 6
            if (digits < 6) {
                maskedRandomInts(nSymb, 0x3f - mask).copyInto(random)
            }
            val res = CharArray(nSymb)
            for (i in 0 until res.size) {
                val shift = digits * i
                val index = ((value shr shift).toInt() and mask) or random[i]
                res[i] = alphabet[index]
            }
            return res
        }

        private fun maskedRandomInts(size: Int, mask: Int): IntArray {
            val ints = IntArray(size)
            val bytes = ByteArray(size)
            try {
                SecureRandom().nextBytes(bytes)
                bytes.forEachIndexed { i, b ->
                    ints[i] = b.toInt() and mask
                }
            } catch (e: Exception) {
                for (i in 0 until ints.size) {
                    ints[i] = Random.nextInt() and mask
                }
            }
            return ints
        }

        override fun toString(): String {
            return "Abc(alphabet=\"${String(alphabet)}\")"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Abc

            if (!alphabet.contentEquals(other.alphabet)) return false

            return true
        }

        override fun hashCode(): Int {
            return alphabet.contentHashCode()
        }

        companion object {
            internal fun shuffle(alphabet: CharArray, seed: Long): CharArray {
                val source = alphabet.toMutableList()
                val out = mutableListOf<Char>()
                var s = seed
                while (source.size > 0) {
                    s = (s * 9301 + 49297) % 233280
                    val i = ((s * source.size) / 233280).toInt()
                    out += source[i]
                    source.removeAt(i)
                }
                return out.toCharArray()
            }
        }
    }

    companion object {
        /**
         * The default, URL-friendly alphabet.
         */
        const val DefaultABC = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-"

        private var defaultInstance = ShortId(0, DefaultABC, 1)

        /**
         * Get the default short Id generator initialised with the default alphabet, worker=0 and seed=1.
         * This default can be overwritten using setDefault.
         */
        @JvmStatic
        fun getDefault(): ShortId = defaultInstance

        /**
         * Overwrites the default generator.
         */
        @JvmStatic
        fun setDefault(sid: ShortId) {
            defaultInstance = sid
        }

        /**
         * Generate a new short Id using the default generator.
         */
        @JvmStatic
        fun generateDefault() = getDefault().generate()
    }
}

