package im.fdx.v2ex.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.content.edit
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import im.fdx.v2ex.R
import im.fdx.v2ex.pref
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.Keys.PREF_TAB
import im.fdx.v2ex.utils.extensions.setUpToolbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_tab_setting.*
import kotlinx.android.synthetic.main.item_text_switch.*
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.toast
import java.util.*


class TabSettingActivity : BaseActivity() {


  val curlist: MutableList<Tab> = mutableListOf()
  lateinit var adapter: DefaultAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_tab_setting)

    setUpToolbar("栏目设置")

    val str = pref.getString(Keys.PREF_TAB, null)
    val turnsType = object : TypeToken<List<Tab>>() {}.type
    val savedList = Gson().fromJson<List<Tab>>(str, turnsType)
    if (savedList != null) {
      curlist.addAll(savedList)
    } else {
      curlist.addAll(initTab())
    }

    val manager = androidx.recyclerview.widget.GridLayoutManager(this, 4)
    recyclerView.layoutManager = manager
    adapter = DefaultAdapter(curlist)
    recyclerView.adapter = adapter

    val helper = ItemTouchHelper(object : ItemTouchHelper.Callback() {

      override fun getMovementFlags(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder): Int {
        //首先回调的方法 返回int表示是否监听该方向
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT//侧滑删除
        return makeMovementFlags(dragFlags, 0)
      }

      override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean {
        //滑动事件
        Collections.swap(curlist, viewHolder.adapterPosition, target.adapterPosition)
        adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return false
      }

      override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
        //侧滑事件
      }
    })
    helper.attachToRecyclerView(recyclerView)

  }


  fun initTab(): MutableList<Tab> {
    val tabTitles = resources.getStringArray(R.array.v2ex_favorite_tab_titles)
    val tabPaths = resources.getStringArray(R.array.v2ex_favorite_tab_paths)

    val list = mutableListOf<Tab>()
    list.addAll(tabTitles.map {
      Tab(it, "")
    })

    list.forEachWithIndex { a, b ->
      b.path = tabPaths[a]
    }
    return list
  }


  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_tab_setting, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_reset -> reset()
      R.id.menu_save -> save()
    }

    return true
  }


  private fun reset() {
    curlist.clear()
    curlist.addAll(initTab())
    adapter.notifyDataSetChanged()

  }


  private fun save() {
    if (curlist.isEmpty()) {
      toast("至少保留一个")
      return
    }
    val savedList = Gson().toJson(curlist)

    pref.edit {
      putString(PREF_TAB, savedList)
    }

    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("im.fdx.v2ex.main_page_setting_change"))

    finish()

  }


}

data class Tab(var title: String, var path: String)

class DefaultAdapter(val list: MutableList<Tab>) : androidx.recyclerview.widget.RecyclerView.Adapter<DefaultAdapter.VH>() {

  override fun onCreateViewHolder(parent: ViewGroup, p1: Int): VH {
    return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_text_switch, parent, false))
  }

  override fun getItemCount(): Int {
    return list.size
  }

  override fun onBindViewHolder(vh: VH, position: Int) {
    vh.tv.text = list[position].title

    vh.ivDelete.setOnClickListener {
      list.removeAt(vh.adapterPosition)
      notifyItemRemoved(vh.adapterPosition)
    }
  }

  class VH(override val containerView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(containerView), LayoutContainer

}