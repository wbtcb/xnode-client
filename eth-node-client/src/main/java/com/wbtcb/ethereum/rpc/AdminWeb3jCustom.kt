package com.wbtcb.ethereum.rpc

import com.wbtcb.ethereum.rpc.response.ImportRawKeyResponse
import org.web3j.protocol.Web3jService
import org.web3j.protocol.admin.Admin
import org.web3j.protocol.admin.JsonRpc2_0Admin
import org.web3j.protocol.core.Request

class AdminWeb3jCustom(web3jService: Web3jService) : JsonRpc2_0Admin(web3jService), Admin, AdminCustom {

    override fun importRawKey(privateKey: String, passphrase: String): Request<Any?, ImportRawKeyResponse> {
        return Request(
            "personal_importRawKey",
            listOf(privateKey, passphrase),
            web3jService,
            ImportRawKeyResponse::class.java
        )
    }

    companion object {

        fun build(web3jService: Web3jService): AdminCustom {
            return AdminWeb3jCustom(web3jService)
        }
    }
}
