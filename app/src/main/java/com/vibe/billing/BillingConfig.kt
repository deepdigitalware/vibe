package com.vibe.billing

object BillingConfig {
    const val CALLER_COST_PER_MINUTE = 10
    const val RECEIVER_EARNING_PER_MINUTE = 5
    const val BLOCK_DURATION_SECONDS = 60
    
    // Legacy alias to avoid breaking existing references immediately
    const val RATE_PER_BLOCK_RUPEES = CALLER_COST_PER_MINUTE
}
