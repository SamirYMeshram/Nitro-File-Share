package com.nitrodropnative.core.permissions

data class PermissionState(
    val missingPermissions: List<String> = emptyList(),
    val canTransfer: Boolean = missingPermissions.isEmpty(),
    val notificationPermissionGranted: Boolean = true
)
