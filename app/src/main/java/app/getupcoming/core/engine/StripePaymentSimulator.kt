package app.getupcoming.core.engine

import java.util.UUID

data class StripeCardInput(
    val cardNumber: String,
    val expMonth: String,
    val expYear: String,
    val cvc: String,
    val cardholderName: String
)

sealed class PaymentResult {
    data class Success(
        val paymentIntentId: String,
        val amountInCents: Int,
        val currency: String,
        val last4: String,
        val cardBrand: String,
        val receiptUrl: String
    ) : PaymentResult()

    data class Error(val message: String) : PaymentResult()
}

object StripePaymentSimulator {

    fun processPayment(
        amountInCents: Int,
        currency: String = "usd",
        cardInput: StripeCardInput
    ): PaymentResult {
        if (amountInCents <= 0) {
            return PaymentResult.Success(
                paymentIntentId = "free_${UUID.randomUUID().toString().take(12)}",
                amountInCents = 0,
                currency = currency,
                last4 = "0000",
                cardBrand = "Free",
                receiptUrl = "https://getupcoming.app/receipts/free"
            )
        }

        val sanitizedNumber = cardInput.cardNumber.replace(" ", "").replace("-", "")
        if (sanitizedNumber.length < 13) {
            return PaymentResult.Error("Please enter a valid card number (13-19 digits).")
        }

        if (cardInput.cvc.length < 3) {
            return PaymentResult.Error("Please enter a valid 3 or 4 digit CVC security code.")
        }

        val cardBrand = when {
            sanitizedNumber.startsWith("4") -> "Visa"
            sanitizedNumber.startsWith("5") -> "Mastercard"
            sanitizedNumber.startsWith("3") -> "Amex"
            else -> "Card"
        }

        val last4 = if (sanitizedNumber.length >= 4) sanitizedNumber.takeLast(4) else "4242"
        val paymentIntentId = "pi_${UUID.randomUUID().toString().replace("-", "").take(24)}"

        return PaymentResult.Success(
            paymentIntentId = paymentIntentId,
            amountInCents = amountInCents,
            currency = currency,
            last4 = last4,
            cardBrand = cardBrand,
            receiptUrl = "https://dashboard.stripe.com/test/payments/$paymentIntentId"
        )
    }

    fun formatPrice(amountInCents: Int, currency: String = "USD"): String {
        val dollars = amountInCents / 100
        val cents = amountInCents % 100
        return if (cents == 0) {
            "$$dollars"
        } else {
            String.format(java.util.Locale.US, "$%d.%02d", dollars, cents)
        }
    }
}
