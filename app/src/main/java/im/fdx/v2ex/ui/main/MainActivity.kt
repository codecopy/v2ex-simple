package im.fdx.v2ex.ui.main

import android.content.*
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.ColorInt
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.TabLayout
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.graphics.ColorUtils
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.elvishew.xlog.XLog
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import im.fdx.v2ex.BuildConfig
import im.fdx.v2ex.MyApp
import im.fdx.v2ex.R
import im.fdx.v2ex.R.id.nav_testNotify
import im.fdx.v2ex.UpdateService
import im.fdx.v2ex.network.HttpHelper
import im.fdx.v2ex.network.NetManager.DAILY_CHECK
import im.fdx.v2ex.network.NetManager.HTTPS_V2EX_BASE
import im.fdx.v2ex.ui.*
import im.fdx.v2ex.ui.favor.FavorActivity
import im.fdx.v2ex.ui.node.AllNodesActivity
import im.fdx.v2ex.utils.HintUI
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.TimeUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    internal lateinit var mDrawer: DrawerLayout
    private var mViewPager: ViewPager? = null
    private var mAdapter: MyViewPagerAdapter? = null
    private var navigationView: NavigationView? = null
    private val shortcutId = "create_topic"
    private var vitent: Intent? = null
    private val listener: ViewPager.OnPageChangeListener? = null
    private lateinit var fab: FloatingActionButton
    private var shortcutManager: ShortcutManager? = null
    private val shortcutIds = listOf("create_topic")
    private var createTopicInfo: ShortcutInfo? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var isGetNotification: Boolean = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val action = intent.action
            XLog.tag(TAG).d("getAction: " + action)
            when (action) {
                Keys.ACTION_LOGIN -> {
                    showIcon(true)
                    val username = intent.getStringExtra(Keys.KEY_USERNAME)
                    val avatar = intent.getStringExtra(Keys.KEY_AVATAR)
                    setUserInfo(username, avatar)
                    fab.show()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        shortcutManager?.addDynamicShortcuts(listOfNotNull<ShortcutInfo>(createTopicInfo))
                    }
                }
                Keys.ACTION_LOGOUT -> {
                    showIcon(false)
                    removeUserInfo()
                    fab.hide()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        shortcutManager?.removeDynamicShortcuts(shortcutIds)
                    }
                }
                Keys.ACTION_GET_NOTIFICATION -> {
                    isGetNotification = true
                    invalidateOptionsMenu()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_nav_drawer)
        XLog.tag(TAG).d("onCreate")

        val intentFilter = IntentFilter()
        intentFilter.addAction(Keys.ACTION_LOGIN)
        intentFilter.addAction(Keys.ACTION_LOGOUT)
        intentFilter.addAction(Keys.ACTION_GET_NOTIFICATION)

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter)

        val mToolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(mToolbar)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            shortcutManager = getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            val intent = Intent(this, NewTopicActivity::class.java)
            intent.action = "android.intent.action.MAIN"
            createTopicInfo = ShortcutInfo.Builder(this, shortcutId)
                    .setActivity(componentName)
                    .setShortLabel(getString(R.string.create_topic))
                    .setLongLabel(getString(R.string.create_topic))
                    .setIntent(intent)
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_create))
                    .build()
            if (MyApp.get().isLogin()) {
                shortcutManager?.addDynamicShortcuts(listOfNotNull<ShortcutInfo>(createTopicInfo))

            } else {
                shortcutManager?.removeDynamicShortcuts(shortcutIds)
            }
        }


        mDrawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val mDrawToggle = ActionBarDrawerToggle(this, mDrawer,
                mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        mDrawer.addDrawerListener(mDrawToggle)
        mDrawToggle.syncState()

        mDrawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View?) {
                val menu = navigationView!!.menu
                (0..menu.size() - 1).forEach { j -> menu.getItem(j).isChecked = false }
            }
        })

        navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView?.setNavigationItemSelectedListener(this)
        fab = findViewById(R.id.fab_main) as FloatingActionButton
        fab.setOnClickListener { startActivity(Intent(this@MainActivity, NewTopicActivity::class.java)) }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        if (MyApp.get().isLogin()) {
            showIcon(true)

            val username = sharedPreferences.getString(Keys.KEY_USERNAME, "")
            val avatar = sharedPreferences.getString(Keys.KEY_AVATAR, "")
            XLog.tag(TAG).d(username + "//// " + avatar)
            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(avatar)) {
                setUserInfo(username, avatar)
            }

            shrinkFab()

        } else {

            showIcon(false)
            fab.hide()
        }

        mViewPager = findViewById(R.id.viewpager_main) as ViewPager
        mAdapter = MyViewPagerAdapter(fragmentManager, this@MainActivity)
        mViewPager!!.adapter = mAdapter

        val mTabLayout = findViewById(R.id.sliding_tabs) as TabLayout


        //这句话可以省略，主要用于如果在其他地方对tablayout自定义title的话，
        // 忽略自定义，只从pageAdapter中获取title
        //        mTabLayout.setTabsFromPagerAdapter(mAdapter);
        //内部实现就是加入一堆的listener给viewpager，不用自己实现
        mTabLayout.setupWithViewPager(mViewPager)

        mTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {}

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                (mAdapter!!.getItem(tab.position) as TopicsFragment).scrollToTop()
            }
        })

        if (!BuildConfig.DEBUG) {
            navigationView!!.menu.removeItem(nav_testNotify)
            navigationView!!.menu.removeItem(R.id.nav_testMenu2)
        }

        vitent = Intent(this@MainActivity, UpdateService::class.java)
        vitent!!.action = Keys.ACTION_START_NOTIFICATION
        if (MyApp.get().isLogin() && isOpenMessage) {
            startService(vitent)
        }

    }

    private fun showIcon(visible: Boolean) {
        navigationView!!.menu.findItem(R.id.nav_daily).isVisible = visible
        navigationView!!.menu.findItem(R.id.nav_favor).isVisible = visible
        this@MainActivity.invalidateOptionsMenu()
    }


    private fun shrinkFab() {
        fab!!.animate().rotation(360f)
                .setDuration(1000).start()
    }


    private fun setUserInfo(username: String, avatar: String) {
        val tvMyName = navigationView!!.getHeaderView(0).findViewById(R.id.tv_my_username) as TextView
        tvMyName.text = username
        val ivMyAvatar = navigationView!!.getHeaderView(0).findViewById(R.id.iv_my_avatar) as CircleImageView
        ivMyAvatar.setOnClickListener {
            val intent = Intent(this@MainActivity, MemberActivity::class.java)
            intent.putExtra(Keys.KEY_USERNAME, username)
            startActivity(intent)
        }

        Picasso.with(this@MainActivity).load(avatar).into(ivMyAvatar)

    }

    private fun removeUserInfo() {
        val tvMyName = findViewById(R.id.tv_my_username) as TextView
        tvMyName.text = ""
        val imageView = findViewById(R.id.iv_my_avatar) as CircleImageView
        imageView.setImageDrawable(null)
        imageView.visibility = View.INVISIBLE

    }

    override fun onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        if (MyApp.get().isLogin()) {
            menu.findItem(R.id.menu_login).isVisible = false
            menu.findItem(R.id.menu_notification).isVisible = true
            //            XLog.tag(TAG).d("invisible");
        } else {
            menu.findItem(R.id.menu_login).isVisible = true
            menu.findItem(R.id.menu_notification).isVisible = false
            //            XLog.tag(TAG).d("visible");
        }

        if (isGetNotification) {
            menu.findItem(R.id.menu_notification).icon = resources.getDrawable(R.drawable.ic_notification_with_red_point, theme)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_login -> startActivityForResult(Intent(this@MainActivity, LoginActivity::class.java), LOG_IN)
            R.id.menu_notification -> {
                item.icon = resources.getDrawable(R.drawable.ic_notifications_white_24dp, theme)
                startActivity(Intent(this, NotificationActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_daily -> dailyCheck()
            R.id.nav_node -> startActivity(Intent(this, AllNodesActivity::class.java))
            R.id.nav_favor -> {
                val intentFavor = Intent(this, FavorActivity::class.java)
                startActivity(intentFavor)
            }
            R.id.nav_testMenu2 -> startActivity(Intent(this, WebViewActivity::class.java))
            R.id.nav_testNotify -> {
            }
            R.id.nav_share -> {
                //                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                val intentShare = Intent(Intent.ACTION_SEND)
                intentShare.type = "text/plain"
                intentShare.putExtra(Intent.EXTRA_TEXT, "V2ex:" + "market://details?id=" + packageName)
                startActivity(Intent.createChooser(intentShare, getString(R.string.share_to)))
            }
            R.id.nav_feedback -> {
                val intentData = Intent(Intent.ACTION_SEND)

                intentData.type = "message/rfc822"
                intentData.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>(Keys.AUTHOR_EMAIL))
                intentData.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
                intentData.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_hint) + "\n")
                try {
                    intentData.`package` = "com.google.android.apps.inbox"
                    startActivity(intentData)
                } catch (ex: ActivityNotFoundException) {
                    intentData.`package` = null
                    intentData.data = Uri.parse("mailto:${Keys.AUTHOR_EMAIL}")
                    startActivity(intentData)
                    //                    Toast.makeText(MainActivity.this, "There are no email clients installed.",
                    //                            Toast.LENGTH_SHORT).show();
                }

            }
            R.id.nav_setting -> {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }
        mDrawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun dailyCheck() {
        HttpHelper.OK_CLIENT.newCall(Request.Builder()
                .headers(HttpHelper.baseHeaders)
                .url(DAILY_CHECK).get().build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "daily mission failed")
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {

                if (response.code() == 302) {
                    runOnUiThread { HintUI.t(this@MainActivity, " 还未登录，请先登录") }
                    return
                }

                val body = response.body()!!.string()

                if (body.contains("每日登录奖励已领取")) {
                    XLog.tag("MainActivity").w("已领取")
                    runOnUiThread { HintUI.t(this@MainActivity, "已领取，明天再来") }
                    return
                }

                val once = parseDailyOnce(body)

                if (once == null) {
                    XLog.tag(TAG).e("null once")
                    return
                }
                postDailyCheck(once)
            }
        })
    }

    private fun postDailyCheck(once: String) {
        HttpHelper.OK_CLIENT.newCall(Request.Builder().headers(HttpHelper.baseHeaders)
                .url(HTTPS_V2EX_BASE + "/mission/daily/redeem?once=" + once)
                .build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "daily mission failed")
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                Log.w("MainActivity", "daily check ok")
                runOnUiThread { HintUI.t(this@MainActivity, "领取成功") }
            }
        })
    }

    private fun parseDailyOnce(string: String): String? {

        val body = Jsoup.parse(string).body()
        val onceElement = body.getElementsByAttributeValue("value", "领取 X 铜币").first() ?: return null
//        location.href = '/mission/daily/redeem?once=83270';
        val onceOriginal = onceElement.attr("onClick")
        return TimeUtil.getNum(onceOriginal)
    }

    override fun onResume() {
        super.onResume()
        XLog.tag(TAG).d("onResume")

        //        bindService(intent);
    }

    override fun onPause() {
        super.onPause()
        XLog.tag(TAG).d("onPause")
    }

    override fun onRestart() {
        super.onRestart()
        XLog.tag(TAG).d("onRestart")
    }

    override fun onStop() {
        super.onStop()
        XLog.tag(TAG).d("onStop")

    }

    override fun onDestroy() {
        super.onDestroy()
        if (MyApp.get().isLogin() && isOpenMessage && !isBackground) {
            stopService(intent)
        }
        XLog.tag(TAG).d("onDestroy")
        mViewPager!!.clearOnPageChangeListeners()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private val isBackground: Boolean
        get() = sharedPreferences.getBoolean("pref_background_msg", false)

    private val isOpenMessage: Boolean
        get() = sharedPreferences.getBoolean("pref_msg", true)

    companion object {

        private val TAG = "MainActivity"
        private val LOG_IN_SUCCEED = 1
        private val LOG_IN = 0
        private val ID_ITEM_CHECK = 33

        @ColorInt
        fun adjustColorForStatusBar(@ColorInt color: Int): Int {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(color, hsl)

            // darken the color by 7.5%
            var lightness = hsl[2] * 0.925f
            // constrain lightness to be within [0–1]
            lightness = Math.max(0f, Math.min(1f, lightness))
            hsl[2] = lightness
            return ColorUtils.HSLToColor(hsl)
        }
    }

}

