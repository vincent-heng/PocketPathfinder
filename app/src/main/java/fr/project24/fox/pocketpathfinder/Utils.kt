package fr.project24.fox.pocketpathfinder

import android.content.Context
import android.net.ConnectivityManager

object Utils {
    fun isNetwork(ctx : Context) : Boolean {
        val cs = ctx.getSystemService(Context.CONNECTIVITY_SERVICE)
        if (cs is ConnectivityManager) {
            return cs.activeNetworkInfo.isConnected
        }
        return false
    }
}