package com.jarvisai

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class JarvisDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "JARVIS device administrator enabled.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "JARVIS device administrator disabled.", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun component(context: Context): ComponentName =
            ComponentName(context, JarvisDeviceAdminReceiver::class.java)
    }
}

object DeviceAdminHelper {
    fun isEnabled(context: Context): Boolean {
        val manager = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
            ?: return false
        return manager.isAdminActive(JarvisDeviceAdminReceiver.component(context))
    }

    fun request(context: Context) {
        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, JarvisDeviceAdminReceiver.component(context))
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "JARVIS does not use administrator access to bypass Android security. Enable it only if you want the optional device-management hooks."
            )
        }
        context.startActivity(intent)
    }
}
