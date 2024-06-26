package org.ole.planet.myplanet.utilities

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import org.ole.planet.myplanet.MainApplication.Companion.context
import org.ole.planet.myplanet.ui.dashboard.DashboardFragment
import org.ole.planet.myplanet.utilities.Constants.PREFS_NAME
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object NetworkUtils {
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    @JvmStatic
    fun isWifiEnabled(): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    @JvmStatic
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @JvmStatic
    fun isWifiBluetoothEnabled(): Boolean {
        return isBluetoothEnabled() || isWifiEnabled()
    }

    @JvmStatic
    fun isBluetoothEnabled(): Boolean {
        val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled
    }

    @JvmStatic
    fun getCurrentNetworkId(context: Context): Int {
        var ssid = -1
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connManager.activeNetwork
        val capabilities = connManager.getNetworkCapabilities(network)
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
            val connectionInfo: WifiInfo? = wifiManager?.connectionInfo
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
                ssid = connectionInfo.networkId
            }
        }
        return ssid
    }

    @JvmStatic
    fun isNetworkConnected(): Boolean {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connManager.activeNetwork
        return network != null
    }


    @JvmStatic
    fun getUniqueIdentifier(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val buildId = Build.ID
        return androidId + "_" + buildId
    }

    @JvmStatic
    fun getMacAddr(): String {
        try {
            val all = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                return getAddress(nif)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }

    private fun getAddress(nif: NetworkInterface): String {
        val macBytes = nif.hardwareAddress ?: return ""
        val res1 = StringBuilder()
        for (b in macBytes) {
            res1.append(String.format("%02X:", b))
        }
        if (res1.isNotEmpty()) {
            res1.deleteCharAt(res1.length - 1)
        }
        return res1.toString()
    }

    @JvmStatic
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model.uppercase(Locale.ROOT)
        } else {
            "$manufacturer $model".uppercase(Locale.ROOT)
        }
    }

    @JvmStatic
    fun getCustomDeviceName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("customDeviceName", "") ?: ""
    }
}