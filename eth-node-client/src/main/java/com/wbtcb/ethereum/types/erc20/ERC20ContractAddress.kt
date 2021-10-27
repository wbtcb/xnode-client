package com.wbtcb.ethereum.types.erc20

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.ClientTransactionManager
import java.math.BigInteger

class ERC20ContractAddress constructor(
    private val web3j: Web3j,
    private val contractAddress: String
) {

    private val manager by lazy { ClientTransactionManager(web3j, null) }

    fun getName(): String? {
        val function = Function(
            FUNC_NAME,
            listOf(),
            listOf<TypeReference<*>>(object : TypeReference<Utf8String?>() {})
        )
        return sendCall(function).firstOrNull()?.value as String?
    }

    fun getSymbol(): String? {
        val function = Function(
            FUNC_SYMBOL,
            listOf(),
            listOf<TypeReference<*>>(object : TypeReference<Utf8String?>() {})
        )
        return sendCall(function).firstOrNull()?.value as String?
    }

    fun getDecimals(): BigInteger? {
        val function = Function(
            FUNC_DECIMALS,
            listOf(),
            listOf<TypeReference<*>>(object : TypeReference<Uint8>() {})
        )
        return sendCall(function).firstOrNull()?.value as BigInteger?
    }

    fun getTotalSupply(): BigInteger? {
        val function = Function(
            FUNC_TOTAL_SUPPLY,
            listOf(),
            listOf<TypeReference<*>>(object : TypeReference<Uint256>() {})
        )
        return sendCall(function).firstOrNull()?.value as BigInteger?
    }

    private fun sendCall(function: Function): MutableList<Type<Any>> {
        val encodedFunction = FunctionEncoder.encode(function)
        val result = manager.sendCall(contractAddress, encodedFunction, DefaultBlockParameterName.LATEST)
        return FunctionReturnDecoder.decode(result, function.outputParameters)
    }

    companion object {
        const val FUNC_NAME = "name"
        const val FUNC_SYMBOL = "symbol"
        const val FUNC_DECIMALS = "decimals"
        const val FUNC_TOTAL_SUPPLY = "totalSupply"
    }
}
