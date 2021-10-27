package com.wbtcb.ethereum.rpc.response

import org.web3j.protocol.core.Response

class ImportRawKeyResponse : Response<String?>() {

    fun publicKey(): String = result!!
}
