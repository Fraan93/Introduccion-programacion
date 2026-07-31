package com.fraan.kroma.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.premiumStore: DataStore<Preferences> by preferencesDataStore(name = "premium")

/** Subscription plans offered by the app. [durationDays] == null means lifetime. */
enum class PremiumPlan(
    val id: String,
    val title: String,
    val price: String,
    val period: String,
    val durationDays: Long?
) {
    WEEKLY("weekly", "Semanal", "2,99 €", "por semana", 7),
    MONTHLY("monthly", "Mensual", "7,99 €", "por mes", 30),
    LIFETIME("lifetime", "De por vida", "14,99 €", "pago único", null)
}

/**
 * Stores the user's premium state locally.
 *
 * NOTE: purchases here are SIMULATED (no real money is charged). To charge real
 * subscriptions the app must be published on Google Play and this class replaced
 * by / connected to the Google Play Billing library, keeping the same interface:
 * [isPremium], [activate], [deactivate].
 */
class PremiumRepository(private val context: Context) {

    private val planKey = stringPreferencesKey("plan")
    private val activatedAtKey = longPreferencesKey("activated_at")

    /** True while a plan is active (lifetime, or within its duration window). */
    val isPremium: Flow<Boolean> = context.premiumStore.data.map { prefs ->
        val plan = PremiumPlan.entries.firstOrNull { it.id == prefs[planKey] }
            ?: return@map false
        val duration = plan.durationDays ?: return@map true // lifetime
        val activatedAt = prefs[activatedAtKey] ?: return@map false
        System.currentTimeMillis() < activatedAt + TimeUnit.DAYS.toMillis(duration)
    }

    /** Currently stored plan (may be expired; combine with [isPremium]). */
    val activePlan: Flow<PremiumPlan?> = context.premiumStore.data.map { prefs ->
        PremiumPlan.entries.firstOrNull { it.id == prefs[planKey] }
    }

    suspend fun activate(plan: PremiumPlan) {
        context.premiumStore.edit { prefs ->
            prefs[planKey] = plan.id
            prefs[activatedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun deactivate() {
        context.premiumStore.edit { it.clear() }
    }
}
