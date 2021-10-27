package com.wbtcb.ethereum.types

import java.math.BigDecimal
import java.util.Date

class EthereumBlockTransaction(
    val hash: String,
    val from: String?,
    val to: String?,
    val amount: BigDecimal,
    val confirmations: Long?,
    val input: String?,
    val timestamp: Date
)
