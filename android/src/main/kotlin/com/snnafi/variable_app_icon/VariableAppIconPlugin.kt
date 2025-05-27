package com.snnafi.variable_app_icon

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.app.Activity;
import android.app.Application
import android.os.Bundle

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


/** VariableAppIconPlugin */
class VariableAppIconPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  private var activity: Activity? = null
  private var binaryMessenger: BinaryMessenger? = null

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel

  private var pendingIconId: String? = null
  private var iconList: List<String>? = null

  private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
    override fun onActivityStopped(act: Activity) {
      if (act == activity) {
        maybeChangeIcon()
      }
    }

    override fun onActivityCreated(act: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(act: Activity) {}
    override fun onActivityResumed(act: Activity) {}
    override fun onActivityPaused(act: Activity) {}
    override fun onActivitySaveInstanceState(act: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(act: Activity) {}
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    binaryMessenger = flutterPluginBinding.binaryMessenger
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "changeAppIcon") {

      pendingIconId = call.argument<String>("androidIconId")
      iconList = call.argument<List<String>>("androidIcons")

      result.success("Success")
    } else {
      result.notImplemented()
    }
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    this.activity = binding.activity as Activity
    channel = MethodChannel(binaryMessenger!!, "variable_app_icon")
    channel.setMethodCallHandler(this)
    this.activity?.application?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onDetachedFromActivityForConfigChanges() {}

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

  override fun onDetachedFromActivity() {}

  private fun maybeChangeIcon() {
    val ctx = activity?.applicationContext ?: return
    val iconId = pendingIconId ?: return
    val icons = iconList ?: return
    val pm = ctx.packageManager

    for (i in icons) {
      val componentName = ComponentName(ctx.packageName, i)

      val newState = when {
        i == iconId -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        i == "appicon.DEFAULT" -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        else -> PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
      }

      pm.setComponentEnabledSetting(
        componentName,
        newState,
        PackageManager.DONT_KILL_APP
      )
    }

    // чистим після роботи
    pendingIconId = null
    iconList = null
  }

}
