package com.wbtcb.ethereum.extension

import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger

fun BigInteger.toEth(): BigDecimal = Convert.fromWei(this.toString(), Convert.Unit.ETHER)

fun BigDecimal.toWei(): BigInteger = Convert.toWei(this.toString(), Convert.Unit.ETHER).toBigInteger()
