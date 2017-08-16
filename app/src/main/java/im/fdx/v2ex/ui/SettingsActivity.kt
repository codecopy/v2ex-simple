package im.fdx.v2ex.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.*
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.elvishew.xlog.XLog
import im.fdx.v2ex.AlarmReceiver
import im.fdx.v2ex.MyApp
import im.fdx.v2ex.R
import im.fdx.v2ex.UpdateService
import im.fdx.v2ex.network.HttpHelper
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.setUpToolbar
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast

class SettingsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setUpToolbar()
        fragmentManager.beginTransaction()
                .add(R.id.container, SettingsFragment())
                .commit()

    }


    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        internal lateinit var sharedPreferences: SharedPreferences
        private var listPreference: ListPreference? = null

        private var count: Int = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.preference)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

            when {
                MyApp.get().isLogin() -> {

                    addPreferencesFromResource(R.xml.preference_login)
                    findPreference("group_user").title = sharedPreferences.getString("username", getString(R.string.user))
                    findPreference(Keys.PREF_LOGOUT).onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        val alert = AlertDialog.Builder(activity)
                                .setTitle("确定要退出吗")
                                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                                .setPositiveButton(R.string.ok) { dialog, _ ->
                                    HttpHelper.myCookieJar.clear()
                                    MyApp.get().setLogin(false)
                                    LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(Keys.ACTION_LOGOUT))
                                    findPreference(Keys.PREF_LOGOUT).isEnabled = false
                                    dialog.dismiss()
                                    activity.finish()
                                    activity.toast("已退出登录")
                                }.create()
                        alert.show()
                        true
                    }

                    listPreference = findPreference("pref_msg_period") as ListPreference
                    if (listPreference?.entry != null) {
                        listPreference?.summary = listPreference?.entry//初始化时设置summary
                    }

                    if (!sharedPreferences.getBoolean("pref_msg", false)) {
                        findPreference("pref_msg_period").isEnabled = false
                        findPreference("pref_background_msg").isEnabled = false
                    }
                }
            }

            findPreference(Keys.PREF_RATES).onPreferenceClickListener = Preference.OnPreferenceClickListener {
                try {
                    val uri = Uri.parse("market://details?id=" + activity.packageName)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    activity.toast("没有找到V2EX客户端")
                }

                true
            }

            val manager = activity.packageManager
            val info: PackageInfo
            try {
                info = manager.getPackageInfo(activity.packageName, 0)
                findPreference("pref_version").summary = info.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }


            val ha = resources.getStringArray(R.array.j)
            count = 7
            findPreference("pref_version").onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (count < 0) {
                    count = 7
                    activity.longToast(ha[(System.currentTimeMillis() / 100 % ha.size).toInt()])
                }
                count--
                true
            }

        }

        @Suppress("unused")
        private fun addSettings() {
            val screen = this.preferenceScreen // "null". See onViewCreated.

            // Create the Preferences Manually - so that the key can be set programatically.
            val category = PreferenceCategory(screen.context)
            category.title = "Channel Configuration"
            category.order = 0
            screen.addPreference(category)

            val checkBoxPref = CheckBoxPreference(screen.context)
            checkBoxPref.key = "_ENABLED"
            checkBoxPref.title = "Enabled"
            checkBoxPref.summary = "CCCC"
            checkBoxPref.isChecked = true

            category.addPreference(checkBoxPref)
        }

        override fun onResume() {
            super.onResume()
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        }

        override fun onPause() {
            super.onPause()
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            Log.w("PREF", key)
            val intent = Intent(activity, UpdateService::class.java)
            when (key) {
                "pref_msg" ->

                    if (sharedPreferences.getBoolean(key, false)) {
                        activity.startService(intent)
                        findPreference("pref_msg_period").isEnabled = true
                        findPreference("pref_background_msg").isEnabled = true
                    } else {
                        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(AlarmReceiver.notifyID)
                        activity.stopService(intent)
                        findPreference("pref_msg_period").isEnabled = false
                        findPreference("pref_background_msg").isEnabled = false

                    }
                "pref_background_msg" -> {
                }
                "pref_msg_period" -> {
                    listPreference?.summary = listPreference?.entry
                    activity.startService(intent)
                    XLog.d("pref_msg_period changed")
                }

                "pref_add_row" -> {
                }
            }
        }

    }
}
