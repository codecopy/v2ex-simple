@file:Suppress("DEPRECATION")

package im.fdx.v2ex.ui.main

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import com.elvishew.xlog.XLog
import com.esafirm.imagepicker.features.ImagePicker
import com.google.gson.reflect.TypeToken
import com.toptoche.searchablespinnerlibrary.SearchableSpinner
import im.fdx.v2ex.R
import im.fdx.v2ex.network.Api
import im.fdx.v2ex.network.HttpHelper
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.ui.details.DetailsActivity
import im.fdx.v2ex.ui.node.NodeModel
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.openImagePicker
import im.fdx.v2ex.utils.extensions.setUpToolbar
import okhttp3.*
import org.jetbrains.anko.toast
import java.io.IOException
import java.util.*
import java.util.regex.Pattern


class NewTopicActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<NodeModel>
    private lateinit var mNodename: String
    private lateinit var etTitle: TextInputEditText
    private lateinit var etContent: EditText
    private lateinit var spinner: SearchableSpinner

    private var mTitle: String = ""
    private var mContent: String = ""
    private var once: String? = null
    private val nodeModels = ArrayList<NodeModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_topic)
        setUpToolbar(getString(R.string.new_top))

        etTitle = findViewById(R.id.et_title)
        etContent = findViewById(R.id.et_content)

        spinner = findViewById(R.id.search_spinner_node)
        spinner.setTitle(getString(R.string.choose_node))
        spinner.setPositiveButton(getString(R.string.close))
        adapter = ArrayAdapter(this, R.layout.simple_list_item, nodeModels)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                mNodename = (parent.getItemAtPosition(position) as NodeModel).name
                XLog.tag(TAG).d(mNodename)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        HttpHelper.OK_CLIENT
                .newCall(Request.Builder().url(NetManager.URL_ALL_NODE).build())
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        NetManager.dealError(this@NewTopicActivity)
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        val type = object : TypeToken<ArrayList<NodeModel>>() {}.type
                        val nodes = NetManager.myGson.fromJson<ArrayList<NodeModel>>(response.body()!!.string(), type)
                        runOnUiThread {
                            adapter.addAll(nodes)
                            adapter.notifyDataSetChanged()
                            setNode(intent)
                        }

                    }
                })

        parseIntent(intent)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            // Get a list of picked images
            val images = ImagePicker.getImages(data)

            images.forEach { image ->
                Api.uploadImage(image.path, image.name, { s, i ->
                    runOnUiThread {
                        when (i) {
                            0 -> {
                                etContent.append("![image]($s)\n") //todo 做到点击删除。那就完美了
                                etContent.setSelection(etContent.length())
                            }
                            1 -> toast("网络错误")
                            2 -> toast(s?.msg ?: "上传失败")
                        }
                    }
                })
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private fun parseIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                etContent.setText(sharedText)
            }
        }
    }

    private fun setNode(intent: Intent) {
        if (intent.getStringExtra(Keys.KEY_NODE_NAME) != null) {
            mNodename = intent.getStringExtra(Keys.KEY_NODE_NAME)
            XLog.d(mNodename + "|" + spinner.count + "|" + adapter.count)
            val nodeTitle: String?
            for (i in 0 until adapter.count) {
                if (mNodename == adapter.getItem(i)?.name) {
                    XLog.d("yes, $i")
                    nodeTitle = adapter.getItem(i)?.title
                    XLog.d(nodeTitle)
                    spinner.setSelection(i)
                    XLog.d(spinner.selectedItemPosition)
                    break
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 123, 1, "send")
                .setIcon(R.drawable.ic_send_white_24dp)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, 124, 0, "upload")
                .setIcon(R.drawable.ic_image)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            123 -> {

                mTitle = etTitle.text.toString()
                mContent = etContent.text.toString()

                when {
                    mTitle.isEmpty() -> toast("标题和内容不能为空")
                    mContent.isEmpty() -> toast("标题和内容不能为空")
                    mTitle.length > 120 -> toast("标题字数超过限制")
                    mContent.length > 20000 -> toast("主题内容不能超过 20000 个字符")
                    mNodename.isEmpty() -> toast(getString(R.string.choose_node))
                    else -> postNew()
                }
            }

            124 -> {
                openImagePicker()
            }

        }

        return true
    }


    private fun postNew() {

        HttpHelper.OK_CLIENT.newCall(Request.Builder()
                .url("https://www.v2ex.com/new")
                .build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                NetManager.dealError(this@NewTopicActivity)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {

                if (response.code() != 200) {
                    NetManager.dealError(this@NewTopicActivity, response.code())
                    return
                }
                once = getOnce(response)
                if (once == null) {
                    return
                }

                val requestBody = FormBody.Builder()
                        .add("title", mTitle)
                        .add("content", mContent)
                        .add("node_name", mNodename)
                        .add("once", once!!)
                        .build()

                HttpHelper.OK_CLIENT.newCall(Request.Builder()
                        .url("https://www.v2ex.com/new")
                        .post(requestBody)
                        .build()).enqueue(object : Callback {
                    override fun onFailure(call1: Call, e: IOException) {
                        NetManager.dealError(this@NewTopicActivity)
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call1: Call, response1: Response) {

                        if (response1.code() == 302) {

                            val location = response1.header("Location")
                            val p = Pattern.compile("(?<=/t/)(\\d+)")
                            val matcher = p.matcher(location!!)
                            val topic: String
                            if (matcher.find()) {
                                topic = matcher.group()
                                XLog.tag(TAG).d(topic)
                                val intent = Intent(this@NewTopicActivity, DetailsActivity::class.java)
                                intent.putExtra(Keys.KEY_TOPIC_ID, topic)
                                startActivity(intent)
                            }
                            finish()
                        }
                    }
                })
            }
        })
    }

    private fun getOnce(response: Response): String? {
        val p = Pattern.compile("(?<=<input type=\"hidden\" name=\"once\" value=\")(\\d+)")
        val matcher = p.matcher(response.body()!!.string())
        if (!matcher.find()) {
            runOnUiThread { toast("无法发布主题，请退出后重试") }
            return null
        }
        return matcher.group()
    }


    companion object {

        private val TAG = NewTopicActivity::class.java.simpleName
    }
}
