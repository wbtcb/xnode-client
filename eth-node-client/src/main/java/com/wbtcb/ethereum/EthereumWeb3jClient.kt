package com.wbtcb.ethereum

import com.wbtcb.ethereum.extension.toEth
import com.wbtcb.ethereum.extension.toWei
import com.wbtcb.ethereum.rpc.AdminWeb3jCustom
import com.wbtcb.ethereum.types.EthereumAddress
import com.wbtcb.ethereum.types.EthereumBlockTransaction
import com.wbtcb.ethereum.types.EthereumBlockTransactionReceipt
import com.wbtcb.ethereum.types.EthereumTransactionLog
import com.wbtcb.ethereum.types.erc20.ERC20ContractAddress
import com.wbtcb.ethereum.types.erc20.ERC20ContractTransactionInput
import com.wbtcb.ethereum.util.DateUtil.epochToDate
import io.reactivex.Flowable
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDUtils
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.slf4j.LoggerFactory
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.http.HttpService
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date

class EthereumWeb3jClient(
    private val url: String,
    private val collectionMnemonicCode: String,
    private val collectionPassphrase: String,
    private val collectionAccountPassword: String,
    private val userMnemonicCode: String,
    private val userPassphrase: String,
    private val userAccountPassword: String
) : EthereumClient {

    private val client by lazy {
        AdminWeb3jCustom.build(HttpService(url))
    }

    override fun initCollectionAddress(): String {
        try {
            logger.info("Generate new ethereum collection address at index 0")

            // Generate new private key by derivation path index
            val privateKey = generateDeterministicKey(collectionMnemonicCode, collectionPassphrase, 0).privateKeyAsHex

            // Import into geth keytore
            val result = client
                .importRawKey(privateKey, collectionPassphrase)
                .send()

            if (result.hasError()) {
                logger.error(result.error.message)
                throw IllegalArgumentException(result.error.message)
            }

            val publicAddress = result.publicKey()

            logger.info("New ethereum collection address has been generated: $publicAddress")

            return publicAddress
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    override fun getCollectionAddress(): String {
        val key = generateDeterministicKey(collectionMnemonicCode, collectionPassphrase, 0)
        return "0x${Keys.getAddress(Sign.publicKeyFromPrivate(key.privKey))}"
    }

    override fun getNewAddress(index: Int): EthereumAddress {
        try {
            logger.info("Generate new ethereum address at index $index")

            // Generate new private key by derivation path index
            val privateKey = generateDeterministicKey(userMnemonicCode, userPassphrase, index).privateKeyAsHex

            // Import into geth keytore
            val result = client
                .importRawKey(privateKey, userAccountPassword)
                .send()

            if (result.hasError()) {
                logger.error(result.error.message)
                throw IllegalArgumentException(result.error.message)
            }

            val publicAddress = result.publicKey()

            logger.info("New ethereum address has been generated: $publicAddress")

            return EthereumAddress(publicAddress, String.format(DERIVATION_PATH, index), index)
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    override fun isValidAddress(address: String): Boolean {
        logger.info("Validate ethereum address")
        return WalletUtils.isValidAddress(address)
    }

    override fun getAddressBalance(address: String, confirmed: Boolean): BigDecimal {
        try {

            logger.info("Get ethereum address balance: $address")

            if (!isValidAddress(address)) {
                throw IllegalArgumentException("Address is invalid: $address")
            }

            val result = client
                .ethGetBalance(
                    address,
                    if (confirmed) DefaultBlockParameterName.LATEST else DefaultBlockParameterName.PENDING
                )
                .send()

            if (result.hasError()) {
                logger.error(result.error.message)
                throw IllegalArgumentException(result.error.message)
            }

            val balance = result.balance.toEth()

            logger.info("Ethereum address balance is: $balance ETH")

            return balance
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    override fun sendFromCollectionAddress(addressTo: String, amount: BigDecimal, fee: BigDecimal): String {
        try {

            val price = estimatePrice(fee.toWei())
            val limit = GAS_LIMIT_ZERO_DATA_TRANSACTION
            val collectionAddress = getCollectionAddress()

            logger.info("Send ethereum from collection address $collectionAddress, to address $addressTo, amount: $amount, amount: $price, amount: $limit, ")

            if (!isValidAddress(addressTo)) {
                throw IllegalArgumentException("Address to is invalid: $addressTo")
            }

            if (amount <= BigDecimal.ZERO) {
                throw IllegalArgumentException("Amount must be > 0")
            }

            val result = client
                .personalSendTransaction(
                    Transaction.createEtherTransaction(
                        collectionAddress,
                        getTransactionNonce(collectionAddress),
                        price,
                        limit,
                        addressTo,
                        amount.toWei()
                    ),
                    collectionAccountPassword
                )
                .send()

            if (result.hasError()) {
                logger.error(result.error.message)
                throw IllegalArgumentException(result.error.message)
            }

            val transactionHash = result.transactionHash

            logger.info("Ethereum has been sent with transaction hash: $transactionHash")

            return transactionHash
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    override fun sweepTotalBalanceToCollectionAccount(addressFrom: String, fee: BigDecimal): String {
        try {

            val price = estimatePrice(fee.toWei())
            val limit = GAS_LIMIT_ZERO_DATA_TRANSACTION
            val balance = getAddressBalance(addressFrom, true)
            val amount = balance.subtract(fee)

            logger.info("Sweep total balance $balance eth of address $addressFrom to collection address with fee $fee eth.")

            if (amount <= BigDecimal.ZERO) {
                throw IllegalArgumentException("Total balance - fee must be > 0")
            }

            val result = client
                .personalSendTransaction(
                    Transaction.createEtherTransaction(
                        addressFrom,
                        getTransactionNonce(addressFrom),
                        price,
                        limit,
                        getCollectionAddress(),
                        amount.toWei()
                    ),
                    userAccountPassword
                )
                .send()

            if (result.hasError()) {
                logger.error(result.error.message)
                throw IllegalArgumentException(result.error.message)
            }

            val transactionHash = result.transactionHash

            logger.info("Ethereum has been swept with transaction hash: $transactionHash")

            return transactionHash
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    override fun replayBlockTransactions(replayBlockCount: Long): Flowable<List<EthereumBlockTransaction>> {
        val startBlock = getCurrentBlock() - BigInteger.valueOf(replayBlockCount)

        return client
            .replayPastAndFutureBlocksFlowable(DefaultBlockParameterNumber(startBlock), true)
            .map { response ->
                try {
                    val block = response.block
                    val currentBlock = getCurrentBlock()

                    block.transactions.map { transaction ->
                        transaction as EthBlock.TransactionObject

                        EthereumBlockTransaction(
                            transaction.hash,
                            transaction.from,
                            transaction.to,
                            transaction.value.toEth(),
                            (currentBlock - transaction.blockNumber).toLong(),
                            transaction.input,
                            block.timestamp.epochToDate()
                        )
                    }
                } catch (ex: IOException) {
                    logger.error(ex.message)
                    throw IllegalArgumentException(ex.message)
                }
            }
    }

    override fun replayLogERC20Transactions(replayBlockCount: Long): Flowable<EthereumTransactionLog> {
        return client
            .ethLogFlowable(getERC20TokenFilter(replayBlockCount))
            .map { response ->
                try {
                    EthereumTransactionLog(
                        response.transactionHash,
                        response.address
                    )
                } catch (ex: IOException) {
                    logger.error(ex.message)
                    throw IllegalArgumentException(ex.message)
                }
            }
    }

    override fun getTransaction(hash: String): EthereumBlockTransaction {
        try {
            val transaction = client
                .ethGetTransactionByHash(hash)
                .send()

            if (transaction.hasError()) {
                logger.error(transaction.error.message)
                throw IllegalArgumentException(transaction.error.message)
            }

            val transactionBlock = client
                .ethGetBlockByNumber(DefaultBlockParameterNumber(transaction.result.blockNumber), false)
                .send()

            if (transactionBlock.hasError()) {
                logger.error(transactionBlock.error.message)
                throw IllegalArgumentException(transactionBlock.error.message)
            }

            return EthereumBlockTransaction(
                transaction.result.hash,
                transaction.result.from,
                transaction.result.to,
                transaction.result.value.toEth(),
                (getCurrentBlock() - transactionBlock.result.number).toLong(),
                transaction.result.input,
                transactionBlock.result.timestamp.epochToDate()
            )
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    override fun getTransactionReceipt(hash: String): EthereumBlockTransactionReceipt {
        try {
            val result = client
                .ethGetTransactionReceipt(hash)
                .send()

            if (result.hasError()) {
                logger.error(result.error.message)
                throw IllegalArgumentException(result.error.message)
            }

            return EthereumBlockTransactionReceipt(
                result.result.transactionHash,
                result.result.from,
                result.result.to,
                (getCurrentBlock() - result.result.blockNumber).toLong()
            )
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    override fun estimateTransactionFee(): BigDecimal {
        return estimateTransactionFee(getGasPrice())
    }

    override fun estimateTransactionFee(price: BigInteger): BigDecimal {
        return (GAS_LIMIT_ZERO_DATA_TRANSACTION * price).toEth()
    }

    private fun estimatePrice(fee: BigInteger): BigInteger {
        return fee / GAS_LIMIT_ZERO_DATA_TRANSACTION
    }

    override fun getTransactionFee(hash: String): BigDecimal {
        try {
            val transaction = client
                .ethGetTransactionByHash(hash)
                .send()

            if (transaction.hasError()) {
                logger.error(transaction.error.message)
                throw IllegalArgumentException(transaction.error.message)
            }

            val transactionReceipt = client
                .ethGetTransactionReceipt(hash)
                .send()

            if (transactionReceipt.hasError()) {
                logger.error(transactionReceipt.error.message)
                throw IllegalArgumentException(transactionReceipt.error.message)
            }

            return (transactionReceipt.result.gasUsed * transaction.result.gasPrice).toEth()
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    override fun getERC20Address(contractAddress: String): ERC20ContractAddress {
        return ERC20ContractAddress(client, contractAddress)
    }

    override fun decodeERC20Input(input: String): ERC20ContractTransactionInput {
        return ERC20ContractTransactionInput(input)
    }

    private fun getERC20TokenFilter(replayBlockCount: Long): EthFilter {
        val startBlock = getCurrentBlock() - BigInteger.valueOf(replayBlockCount)

        // ERC-20 definition
        val event = Event("Transfer",
            listOf(
                object : TypeReference<Address>(true) {},
                object : TypeReference<Address>(true) {},
                object : TypeReference<Uint256>(false) {}
            )
        )

        val filter = EthFilter(
            DefaultBlockParameterNumber(startBlock),
            DefaultBlockParameterName.LATEST,
            listOf()
        )

        filter.addSingleTopic(EventEncoder.encode(event))

        return filter
    }

    private fun getTransactionNonce(address: String): BigInteger {
        try {
            val result = client
                .ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                .send()

            if (result.hasError()) {
                logger.error(result.error.message)
                throw IllegalArgumentException(result.error.message)
            }

            return result.transactionCount
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    private fun getGasPrice(): BigInteger {
        try {
            logger.info("Get gas price")

            val result = client
                .ethGasPrice()
                .send()

            if (result.hasError()) {
                logger.error(result.error.message)
                throw IllegalArgumentException(result.error.message)
            }

            val price = result.gasPrice

            logger.info("Gas price is: $price wei ${price.toEth()} eth")

            return price
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    private fun getCurrentBlock(): BigInteger {
        try {
            return client
                .ethBlockNumber()
                .send()
                .blockNumber
        } catch (ex: IOException) {
            logger.error(ex.message)
            throw IllegalArgumentException(ex.message)
        }
    }

    private fun generateDeterministicKey(mnemonicCode: String, passphrase: String, index: Int): DeterministicKey {
        val seed = DeterministicSeed(mnemonicCode, null, passphrase, Date().time)
        val chain = DeterministicKeyChain.builder().seed(seed).build()
        return chain.getKeyByPath(HDUtils.parsePath(String.format(DERIVATION_PATH, index)), true)
    }

    companion object {
        /**
         * https://ethereum.github.io/yellowpaper/paper.pdf - page 26
         * G-transaction 21000 Paid for every transaction (standard transaction without data).
         * For transactions with data use formula: gasLimit = G-transaction (21000) + G-txdatanonzero (16) × dataByteLength
         * */
        private val GAS_LIMIT_ZERO_DATA_TRANSACTION = BigInteger("21000")

        private const val DERIVATION_PATH = "M/44H/60H/0H/0/%d"

        private val logger = LoggerFactory.getLogger(EthereumWeb3jClient::class.java)
    }
}
