import uniffi.bdk.*

class LogProgress : BdkProgress {
    override fun update(progress: Float, message: String?) {
        println("Syncing..")
    }
}

class NullProgress : BdkProgress {
    override fun update(progress: Float, message: String?) {}
}

fun getConfirmedTransaction(
        wallet: OnlineWalletInterface,
        transactionId: String
): ConfirmedTransaction? {
    wallet.sync(NullProgress(), null)
    return wallet.getTransactions()
            .stream()
            .filter({ it.id.equals(transactionId) })
            .findFirst()
            .orElse(null)
}

fun main(args: Array<String>) {
    println("Configuring an in-memory wallet on electrum..")
    val descriptor = "pkh(cSQPHDBwXGjVzWRqAHm6zfvQhaTuj1f2bFH58h55ghbjtFwvmeXR)"
    val amount = 1000uL
    val recipient = "tb1ql7w62elx9ucw4pj5lgw4l028hmuw80sndtntxt"
    val db = DatabaseConfig.Memory("")
    val client =
            BlockchainConfig.Electrum(
                    ElectrumConfig("ssl://electrum.blockstream.info:60002", null, 5u, null, 10u)
            )
    val wallet = OnlineWallet(descriptor, Network.TESTNET, db, client)
    wallet.sync(LogProgress(), null)
    println("Initial wallet balance: ${wallet.getBalance()}")
    println("Please send $amount satoshis to address: ${wallet.getNewAddress()}")
    readLine()
    wallet.sync(LogProgress(), null)
    println("New wallet balance: ${wallet.getBalance()}")
    println("Press Enter to return funds")
    readLine()
    println(
            "Creating a PSBT with recipient $recipient and amount $amount satoshis..."
    )
    val transaction = PartiallySignedBitcoinTransaction(wallet, recipient, amount)
    println("Signing the transaction...")
    wallet.sign(transaction)
    println("Broadcasting the signed transaction...")
    val transactionId = wallet.broadcast(transaction)
    println("Broadcasted transaction with id $transactionId")
    println("Confirming transaction...")
    var confirmedTransaction = getConfirmedTransaction(wallet, transactionId)
    while (confirmedTransaction == null) {
        confirmedTransaction = getConfirmedTransaction(wallet, transactionId)
    }
    println("Confirmed transaction: $confirmedTransaction")
    val transactions = wallet.getTransactions()
    println("Listing all ${transactions.size} transactions...")
    transactions.sortedByDescending { it.timestamp }.forEach { println(it) }
    println("Final wallet balance: ${wallet.getBalance()}")
}