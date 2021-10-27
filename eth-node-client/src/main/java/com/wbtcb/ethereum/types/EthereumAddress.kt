package com.wbtcb.ethereum.types

data class EthereumAddress(
    val address: String,
    val derivationPath: String,
    val derivationIndex: Int
)
