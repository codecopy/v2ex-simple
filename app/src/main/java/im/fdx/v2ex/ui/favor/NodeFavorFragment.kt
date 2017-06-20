package im.fdx.v2ex.ui.favor

import android.app.Fragment
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.elvishew.xlog.XLog
import im.fdx.v2ex.R
import im.fdx.v2ex.network.HttpHelper
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.ui.node.AllNodesAdapter
import im.fdx.v2ex.utils.extensions.showNoContent
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 */
class NodeFavorFragment : Fragment() {


    private val url = "https://www.v2ex.com/my/nodes"
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var adapter: AllNodesAdapter
    private lateinit var flContainer: FrameLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        XLog.d("NodeFavorFragment onCreateView")
        val view = inflater.inflate(R.layout.fragment_tab_article, container, false)
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_container)
        adapter = AllNodesAdapter(activity, true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(activity, 3)
        swipe = view.findViewById(R.id.swipe_container)
        flContainer = view.findViewById(R.id.fl_container)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipe.isRefreshing = true
        HttpHelper.OK_CLIENT.newCall(Request.Builder()
                .headers(HttpHelper.baseHeaders)
                .url(url)
                .build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                NetManager.dealError(activity)
                activity.runOnUiThread {
                    swipe.isRefreshing = false
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {

                if (response.code() != 200) {
                    NetManager.dealError(activity, response.code())
                    return
                }
                val nodeModels = NetManager.parseToNode(response.body()!!.string())
                if (nodeModels == null || nodeModels.isEmpty()) {
                    activity.runOnUiThread {
                        flContainer.showNoContent()
                        swipe.isRefreshing = false
                    }
                    return
                }

                adapter.clear()
                adapter.addAll(nodeModels)
                activity.runOnUiThread {
                    adapter.notifyDataSetChanged()
                    swipe.isRefreshing = false
                }
            }
        })
    }

}