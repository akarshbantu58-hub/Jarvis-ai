package com.jarvisai

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher

object PermissionCenter {
    fun requiredRuntimePermissions(): Array<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    fun missingPermissions(context: Context): Array<String> = requiredRuntimePermissions().filter {
        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }.toTypedArray()

    fun requestMissing(launcher: ActivityResultLauncher<Array<String>>, context: Context) {
        val missing = missingPermissions(context)
        if (missing.isNotEmpty()) launcher.launch(missing)
    }

    fun hasAssistantRole(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
    }

    fun requestAssistantRole(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true &&
                !roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                activity.startActivityForResult(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                    requestCode
                )
            }
        }
    }

    fun openAppDetails(context: Context) {
        context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        })
    }
}
