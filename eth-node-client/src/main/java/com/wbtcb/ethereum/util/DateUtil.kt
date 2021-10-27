package com.wbtcb.ethereum.util

import java.math.BigInteger
import java.util.Date

object DateUtil {

    fun BigInteger.epochToDate(): Date = Date(this.toLong() * 1000)
}
