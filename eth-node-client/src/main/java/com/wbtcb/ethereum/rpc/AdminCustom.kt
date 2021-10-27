package com.wbtcb.ethereum.rpc

import com.wbtcb.ethereum.rpc.response.ImportRawKeyResponse
import org.web3j.protocol.admin.Admin
import org.web3j.protocol.core.Request

interface AdminCustom : Admin {

    fun importRawKey(privateKey: String, passphrase: String): Request<Any?, ImportRawKeyResponse>
}
