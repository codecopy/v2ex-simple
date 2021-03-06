@file:Suppress("DEPRECATION")

package im.fdx.v2ex.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import com.esafirm.imagepicker.features.ImagePicker
import im.fdx.v2ex.R
import im.fdx.v2ex.network.*
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.details.TopicActivity
import im.fdx.v2ex.ui.node.AllNodesActivity
import im.fdx.v2ex.ui.node.Node
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.Keys.KEY_TO_CHOOSE_NODE
import im.fdx.v2ex.utils.extensions.logd
import im.fdx.v2ex.utils.extensions.openImagePicker
import im.fdx.v2ex.utils.extensions.setUpToolbar
import kotlinx.android.synthetic.main.activity_create_topic.*
import okhttp3.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.io.IOException
import java.util.regex.Pattern


class NewTopicActivity : BaseActivity() {

    private var mNodename: String = ""
  private lateinit var etTitle: com.google.android.material.textfield.TextInputEditText
    private lateinit var etContent: EditText

    private var mTitle: String = ""
    private var mContent: String = ""
    private var once: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_topic)
        setUpToolbar(getString(R.string.new_top))

        etTitle = findViewById(R.id.et_title)
        etContent = findViewById(R.id.et_content)


      search_spinner_node.setOnClickListener {
        startActivityForResult<AllNodesActivity>(requestNode, KEY_TO_CHOOSE_NODE to true)
      }

        parseIntent(intent)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            // Get a list of picked images
            val images = ImagePicker.getImages(data)

            images.forEach { image ->
                Api.uploadImage(image.path, image.name) { s, i ->
                    runOnUiThread {
                        when (i) {
                            0 -> {
                                etContent.append("![image](${s?.url})\n") //todo 做到点击删除。那就完美了
                                etContent.setSelection(etContent.length())
                            }
                            1 -> toast("网络错误")
                            2 -> toast(s?.msg ?: "上传失败")
                        }
                    }
                }
            }

        } else if (requestCode == NewTopicActivity.requestNode && resultCode == Activity.RESULT_OK && data != null) {
          val nodeInfo = data.getParcelableExtra<Node>("extra_node")
          mNodename = nodeInfo.name
          search_spinner_node.text = "${nodeInfo.name} | ${nodeInfo.title}"
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 123, 1, "send")
                .setIcon(R.drawable.ic_send_primary_24dp)
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
                  mNodename.isEmpty() -> toast(R.string.choose_node)
                    else -> postNew(item)
                }
            }

            124 -> {
                openImagePicker(this)
            }

        }

        return true
    }


    private fun postNew(item: MenuItem) {

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val iv = inflater.inflate(R.layout.iv_refresh, null) as ImageView
        val rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh)
        rotation.repeatCount = Animation.INFINITE
        iv.startAnimation(rotation)
        item.actionView = iv

      vCall("https://www.v2ex.com/new").start(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                resetIcon(item)
                NetManager.dealError(this@NewTopicActivity)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {

                if (response.code() != 200) {
                    resetIcon(item)
                    NetManager.dealError(this@NewTopicActivity, response.code())
                    return
                }
                once = Parser(response.body()!!.string()).getOnceNum2()
                if (once == null) {
                    toast("发布主题失败，请退出app重试")
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
                        resetIcon(item)
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
                              logd(topic)
                                val intent = Intent(this@NewTopicActivity, TopicActivity::class.java)
                                intent.putExtra(Keys.KEY_TOPIC_ID, topic)
                                startActivity(intent)
                            }
                            finish()
                        } else {
                            resetIcon(item)
                            val errorMsg = Parser(response.body()!!.string()).getErrorMsg()
                            longToast(errorMsg)
                        }
                    }
                })
            }
        })
    }

    private fun resetIcon(item: MenuItem) {
        runOnUiThread {
          item.setIcon(R.drawable.ic_send_primary_24dp)
        }
    }

    companion object {
      const val requestNode = 123
    }
}
