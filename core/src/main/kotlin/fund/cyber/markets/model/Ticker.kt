package fund.cyber.markets.model


import com.datastax.driver.mapping.annotations.ClusteringColumn
import com.datastax.driver.mapping.annotations.Frozen
import com.datastax.driver.mapping.annotations.PartitionKey
import com.datastax.driver.mapping.annotations.Table
import fund.cyber.markets.dto.TokensPair
import java.math.BigDecimal
import java.util.*


@Table(keyspace = "markets", name = "ticker",
        readConsistency = "QUORUM", writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false, caseSensitiveTable = false)
data class Ticker(

    @ClusteringColumn(0)
    var exchange: String?,

    @Frozen
    @PartitionKey(0)
    var tokensPair: TokensPair?,
    var timestampFrom: Date?,

    @ClusteringColumn(1)
    var timestampTo: Date?,

    @PartitionKey(1)
    var windowDuration: Long,
    var baseAmount: BigDecimal,
    var quoteAmount: BigDecimal,
    var price: BigDecimal,
    var minPrice: BigDecimal?,
    var maxPrice: BigDecimal?,
    var tradeCount: Long
) {

    constructor(windowDuration: Long) : this(null, null, null, null, windowDuration, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, 0)

    fun add(trade: Trade): Ticker {

        if (!validTrade(trade)) {
            return this
        }
        if (exchange == null) {
            exchange = trade.exchange
        }
        if (tokensPair == null) {
            tokensPair = trade.pair
        }

        quoteAmount = quoteAmount.plus(trade.quoteAmount)
        baseAmount = baseAmount.plus(trade.baseAmount)

        minPrice =
                if (minPrice == null)
                    trade.quoteAmount.div(trade.baseAmount)
                else
                    minPrice?.min(trade.quoteAmount.div(trade.baseAmount))

        maxPrice =
                if (maxPrice == null)
                    trade.quoteAmount.div(trade.baseAmount)
                else
                    maxPrice?.max(trade.quoteAmount.div(trade.baseAmount))

        tradeCount++

        return this
    }

    fun add(ticker: Ticker): Ticker {

        quoteAmount = quoteAmount.plus(ticker.quoteAmount)
        baseAmount = baseAmount.plus(ticker.baseAmount)

        if (tokensPair == null) {
            tokensPair = ticker.tokensPair
        }
        if (exchange == null) {
            exchange = ticker.exchange
        }

        minPrice =
                if (minPrice == null)
                    ticker.minPrice
                else
                    this.minPrice?.min(ticker.minPrice)

        maxPrice =
                if (maxPrice == null)
                    ticker.maxPrice
                else
                    this.maxPrice?.max(ticker.maxPrice)

        tradeCount += ticker.tradeCount

        return this
    }

    fun calcPrice(): Ticker {
        if (!(quoteAmount.compareTo(BigDecimal.ZERO) == 0 || baseAmount.compareTo(BigDecimal.ZERO) == 0)) {
            price = quoteAmount.div(baseAmount)
        }

        return this
    }

    fun setExchangeString(exchange: String) : Ticker {
        this.exchange = exchange

        return this
    }

    fun setTimestamps(millisFrom: Long, millisTo: Long) : Ticker {
        timestampFrom = Date(millisFrom)
        timestampTo = Date(millisTo)

        return this
    }

    private fun validTrade(trade: Trade): Boolean {
        return !(trade.baseAmount == null
                || trade.quoteAmount == null
                || trade.pair == null
                || trade.quoteAmount.compareTo(BigDecimal.ZERO) == 0
                || trade.baseAmount.compareTo(BigDecimal.ZERO) == 0)
    }

}
