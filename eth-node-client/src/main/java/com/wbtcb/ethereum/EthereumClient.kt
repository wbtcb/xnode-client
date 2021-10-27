package com.wbtcb.ethereum

import com.wbtcb.ethereum.types.EthereumAddress
import com.wbtcb.ethereum.types.EthereumBlockTransaction
import com.wbtcb.ethereum.types.EthereumBlockTransactionReceipt
import com.wbtcb.ethereum.types.EthereumTransactionLog
import com.wbtcb.ethereum.types.erc20.ERC20ContractAddress
import com.wbtcb.ethereum.types.erc20.ERC20ContractTransactionInput
import io.reactivex.Flowable
import java.math.BigDecimal
import java.math.BigInteger

interface EthereumClient {

    /**
     *
     * Only for initial setup. Can be called only once.
     */
    fun initCollectionAddress(): String

    fun getCollectionAddress(): String

    fun getNewAddress(index: Int): EthereumAddress

    fun isValidAddress(address: String): Boolean

    fun getAddressBalance(address: String, confirmed: Boolean): BigDecimal

    fun sendFromCollectionAddress(addressTo: String, amount: BigDecimal, fee: BigDecimal): String

    fun sweepTotalBalanceToCollectionAccount(addressFrom: String, fee: BigDecimal): String

    fun replayBlockTransactions(replayBlockCount: Long): Flowable<List<EthereumBlockTransaction>>

    fun replayLogERC20Transactions(replayBlockCount: Long): Flowable<EthereumTransactionLog>

    fun getTransaction(hash: String): EthereumBlockTransaction

    fun getTransactionReceipt(hash: String): EthereumBlockTransactionReceipt

    fun estimateTransactionFee(): BigDecimal

    fun estimateTransactionFee(price: BigInteger): BigDecimal

    fun getTransactionFee(hash: String): BigDecimal

    fun getERC20Address(contractAddress: String): ERC20ContractAddress

    fun decodeERC20Input(input: String): ERC20ContractTransactionInput
}
