package im.fdx.v2ex.ui.main

import android.content.Context
import androidx.core.content.edit
import androidx.core.os.bundleOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import im.fdx.v2ex.R
import im.fdx.v2ex.myApp
import im.fdx.v2ex.pref
import im.fdx.v2ex.ui.Tab
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.Keys.PREF_TAB
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*


/**
 * Created by fdx on 2015/10/15.
 * 从MainActivity分离出来. 用了FragmentStatePagerAdapter 替代FragmentPagerAdapter，才可以动态切换Fragment
 * 弃用了Volley 和 模拟web + okhttp
 */
internal class MyViewPagerAdapter(fm: androidx.fragment.app.FragmentManager, private val mContext: Context) : androidx.fragment.app.FragmentStatePagerAdapter(fm) {

    private val mFragments = ArrayList<TopicsFragment>()
    private val tabList = mutableListOf<Tab>()
    init {
        initFragment()
    }

    fun initFragment() {
        tabList.clear()
        mFragments.clear()

        var str = pref.getString(PREF_TAB, null)
        if (str == null) {
            val tabTitles = mContext.resources.getStringArray(R.array.v2ex_favorite_tab_titles)
            val tabPaths = mContext.resources.getStringArray(R.array.v2ex_favorite_tab_paths)

            val list = mutableListOf<Tab>()
            list.addAll(tabTitles.map {
                Tab(it, "")
            })

            list.forEachWithIndex { a, b ->
                b.path = tabPaths[a]
            }
            val savedList = Gson().toJson(list)

            mContext.defaultSharedPreferences.edit {
                putString(PREF_TAB, savedList)
            }

            str = savedList
        }

        val turnsType = object : TypeToken<List<Tab>>() {}.type
        val list = Gson().fromJson<List<Tab>>(str, turnsType)

        for (it in list) {
            if (!myApp.isLogin && it.path == "recent") {
                continue
            }
            mFragments.add(TopicsFragment().apply { arguments = bundleOf(Keys.KEY_TAB to it.path) })
            tabList.add(it)
        }
    }

    override fun getItem(position: Int) = mFragments[position]
    override fun getCount() = tabList.size
    override fun getPageTitle(position: Int) = tabList[position].title

}
