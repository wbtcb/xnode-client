package com.wbtcb.ethereum.types.erc20

import org.slf4j.LoggerFactory
import org.web3j.abi.TypeDecoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Uint256
import java.lang.reflect.Method
import java.math.BigInteger

class ERC20ContractTransactionInput constructor(
    private val input: String
) {

    fun getTo(): String? = try {
        val to = input.substring(10, 74)
        (getDecoderMethod().invoke(null, to, 0, Address::class.java) as Address).value
    } catch (ex: Exception) {
        logger.warn("Cannot decode address from input $input")
        null
    }

    fun getValue(): BigInteger? = try {
        val value = input.substring(74)
        (getDecoderMethod().invoke(null, value, 0, Uint256::class.java) as Uint256).value
    } catch (ex: Exception) {
        logger.warn("Cannot decode value from input $input")
        null
    }

    private fun getDecoderMethod(): Method {
        return TypeDecoder::class
            .java
            .getDeclaredMethod(
                "decode",
                String::class.java,
                Int::class.javaPrimitiveType,
                Class::class.java
            )
            .apply { isAccessible = true }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ERC20ContractTransactionInput::class.java)
    }
}
