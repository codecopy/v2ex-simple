package im.fdx.v2ex.ui.details;

import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.elvishew.xlog.XLog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.fdx.v2ex.MyApp;
import im.fdx.v2ex.R;
import im.fdx.v2ex.model.BaseModel;
import im.fdx.v2ex.ui.main.TopicModel;
import im.fdx.v2ex.network.HttpHelper;
import im.fdx.v2ex.network.NetManager;
import im.fdx.v2ex.utils.HintUI;
import im.fdx.v2ex.utils.Keys;
import im.fdx.v2ex.utils.SmoothLayoutManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static im.fdx.v2ex.network.HttpHelper.OK_CLIENT;
import static im.fdx.v2ex.network.HttpHelper.baseHeaders;
import static im.fdx.v2ex.network.NetManager.dealError;
import static im.fdx.v2ex.utils.Keys.ACTION_LOGIN;
import static im.fdx.v2ex.utils.Keys.ACTION_LOGOUT;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

public class DetailsActivity extends AppCompatActivity {


    public static final int POSITION_START = 0;
    private static final String TAG = DetailsActivity.class.getSimpleName();
    private static final int MSG_OK_GET_TOPIC = 0;
    private static final int MSG_ERROR_AUTH = 1;
    private static final int MSG_ERROR_IO = 2;
    private static final int MSG_GO_TO_BOTTOM = 3;
    private static final int MSG_GET_MORE_REPLY = 4;

    private DetailsAdapter.AdapterCallback callback = new DetailsAdapter.AdapterCallback() {
        @Override
        public void onMethodCallback() {
            getMoreRepliesByOrder(1, false);
        }
    };
    RecyclerView rvDetail;
    private SwipeRefreshLayout mSwipe;
    private ImageView ivSend;
    //    private EditText etSendReply;
    private DetailsAdapter mAdapter;
    private List<BaseModel> mAllContent = new ArrayList<>();
    private String token;
    private String mTopicUrl;
    private Menu mMenu;
    private boolean isFavored;
    private long mTopicId = -1L;
    private String once;
    private TopicModel topicHeader;
    private Toolbar toolbar;
    private ActionBar actionBar;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            XLog.tag(TAG).d("get in lbc:" + intent.getAction());
            if (intent.getAction().equals("im.fdx.v2ex.event.login")) {
                invalidateOptionsMenu();
                addFootView();
            } else if (intent.getAction().equals("im.fdx.v2ex.event.logout")) {
                invalidateOptionsMenu();
                removeFootView();
            } else if (intent.getAction().equals("im.fdx.v2ex.reply")) {
                XLog.tag("HEHE").d("MSG_GET  LocalBroadCast");
                token = intent.getStringExtra("token");
                List<ReplyModel> rm = intent.getParcelableArrayListExtra("replies");
                mAllContent.addAll(rm);
                mAdapter.notifyDataSetChanged();

                if (intent.getBooleanExtra("bottom", false)) {
                    rvDetail.scrollToPosition(mAllContent.size() - 1);
                }
            }
        }

    };
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_OK_GET_TOPIC:
                case MSG_ERROR_IO:
                    mSwipe.setRefreshing(false);
                    break;
                case MSG_ERROR_AUTH:
                    HintUI.t(DetailsActivity.this, "该主题需要登录查看");
                    DetailsActivity.this.finish();
                    break;
                case MSG_GO_TO_BOTTOM:
                    rvDetail.scrollToPosition(mAllContent.size() - 1);
                    break;
                case MSG_GET_MORE_REPLY:
                    XLog.tag("HEHE").d("MSG_GET_MORE_REPLY");
                    List<ReplyModel> rm = (List<ReplyModel>) msg.obj;
                    mAllContent.addAll(rm);
                    mAdapter.notifyDataSetChanged();
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        IntentFilter filter = new IntentFilter(ACTION_LOGIN);
        filter.addAction(ACTION_LOGOUT);
        filter.addAction("im.fdx.v2ex.reply");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

//        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.ctl);

        if (MyApp.getInstance().isLogin()) {
            addFootView();
        } else {
            removeFootView();
        }
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }

        //I add parentActivity in Manifest, so I do not need below code ? NONONONONO---NEEDED
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        rvDetail = (RecyclerView) findViewById(R.id.detail_recycler_view);
        final LinearLayoutManager mLayoutManager = new SmoothLayoutManager(this);
        rvDetail.setLayoutManager(mLayoutManager);
        rvDetail.smoothScrollToPosition(POSITION_START);
        //// 这个Scroll 到顶部的bug，卡了我一个星期，用了SO上的方法，自定义了一个LinearLayoutManager
        rvDetail.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && etSendReply.hasFocus()) {
//                    etSendReply.clearFocus();
//                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //// TODO: 2017/5/3

            }
        });

        rvDetail.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                if (mLayoutManager.findFirstVisibleItemPosition() != 0) {
                    Log.d(TAG, ":::");
                    if (topicHeader != null) {
                        toolbar.setTitle(topicHeader.getTitle());
                    }
                } else {
                    toolbar.setTitle("");
                }

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
//                Log.d(TAG, String.valueOf(dx) +":::"+ dy);
            }
        });

        mAdapter = new DetailsAdapter(DetailsActivity.this, mAllContent, callback);
        rvDetail.setAdapter(mAdapter);

        mSwipe = (SwipeRefreshLayout) findViewById(R.id.swipe_details);
        mSwipe.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getRepliesPageOne(mTopicId, false);
            }

        });

        ivSend = (ImageView) findViewById(R.id.iv_send);
//        etSendReply = (EditText) findViewById(R.id.et_post_reply);
//        etSendReply.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//
//                XLog.tag(TAG).d("hasFocus" + hasFocus);
//                if (!hasFocus) {
//                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
//                }
//            }
//        });
//
//        etSendReply.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//                if (TextUtils.isEmpty(s)) {
//                    ivSend.setClickable(false);
//                    ivSend.setImageResource(R.drawable.ic_send_black_24dp);
//                    //// TODO: 2017/5/2
//
//                } else {
//                    ivSend.setClickable(true);
//                    ivSend.setImageResource(R.drawable.ic_send_white_24dp);
//
//                }
//            }
//        });
        parseIntent(getIntent());

        //以下是设置刷新按钮的位置，暂时不用
//        final DisplayMetrics metrics = getResources().getDisplayMetrics();
//        int start = (int) (40 * metrics.density);
//        mSwipe.setProgressViewOffset(false, -start, (int) (start*1.5));
    }

    private void removeFootView() {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.foot_container);
        linearLayout.setVisibility(View.GONE);
    }

    private void addFootView() {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.foot_container);
        linearLayout.setVisibility(View.VISIBLE);
    }


    private void parseIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            String scheme = data.getScheme(); //不需要
            String host = data.getHost(); //不需要判断，在manifest中已指定
            List<String> params = data.getPathSegments();
            try {
                mTopicId = Long.parseLong(params.get(1));
            } catch (NumberFormatException e) {
                return;
            }
        } else if (intent.getParcelableExtra("model") != null) {
            TopicModel topicModel = intent.getParcelableExtra("model");
            mAllContent.add(0, topicModel);
            mTopicId = topicModel.getId();
        } else if (intent.getLongExtra(Keys.KEY_TOPIC_ID, -1L) != -1L) {
            mTopicId = intent.getLongExtra(Keys.KEY_TOPIC_ID, -1L);
        }


        getRepliesPageOne(mTopicId, false);
        mTopicUrl = NetManager.HTTPS_V2EX_BASE + "/t/" + mTopicId;

        XLog.tag(TAG).d(mTopicId + ": " + mTopicUrl);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseIntent(intent);
    }

    private void getRepliesPageOne(final long topicId, final boolean scrollToBottom) {
        mSwipe.setRefreshing(true);
        OK_CLIENT.newCall(new Request.Builder().headers(HttpHelper.baseHeaders)
                .url(NetManager.HTTPS_V2EX_BASE + "/t/" + topicId + "?p=" + "1")
                .build()).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                handler.sendEmptyMessage(MSG_ERROR_IO);
                XLog.tag("DetailsActivity").d("failed " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                //权限问题，需要登录
                int code = response.code();
                if (code == 302) {
                    handler.sendEmptyMessage(MSG_ERROR_AUTH);
                    return;
                }
                if (code != 200) {
                    dealError(DetailsActivity.this, code);
                    return;
                }

                String bodyStr = response.body().string();
                Element body = Jsoup.parse(bodyStr);

                topicHeader = NetManager.parseResponseToTopic(body, topicId);
                List<ReplyModel> repliesOne = NetManager.parseResponseToReplay(body);


                if (MyApp.getInstance().isLogin()) {
                    token = NetManager.parseToVerifyCode(body);
                    XLog.tag(TAG).d("verify" + token);
                    if (token != null) {
                        mAdapter.setVerifyCode(token);
                    }
                    isFavored = parseIsFavored(body);

                    XLog.tag(TAG).d("isfavored" + String.valueOf(isFavored));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFavored) {
                                mMenu.findItem(R.id.menu_favor).setIcon(R.drawable.ic_favorite_white_24dp);
                            } else {
                                mMenu.findItem(R.id.menu_favor).setIcon(R.drawable.ic_favorite_border_white_24dp);
                            }
                        }
                    });
                }

                once = NetManager.parseOnce(body);
                mAllContent.clear();
                mAllContent.add(0, topicHeader);
                mAllContent.addAll(repliesOne);

                XLog.tag(TAG).d("get first page done, next is get more page");
                final int totalPage = NetManager.getTotalPage(body);  // [2,3]
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                        mSwipe.setRefreshing(false);
                        if (totalPage == 1 && scrollToBottom) {
                            handler.sendEmptyMessage(MSG_GO_TO_BOTTOM);
                        }
                    }
                });

                if (totalPage != -1) {
                    XLog.tag(TAG).d(totalPage);
                    getMoreRepliesByOrder(totalPage, scrollToBottom);
                }
            }
        });
    }

    private boolean parseIsFavored(Element body) {
        Pattern p = Pattern.compile("un(?=favorite/topic/\\d{1,10}\\?t=)");
        Matcher matcher = p.matcher(body.outerHtml());
        return matcher.find();
    }

    private void getMoreRepliesByOrder(int totalPage, boolean scrollToBottom) {
        Intent intentGetMoreReply = new Intent(DetailsActivity.this, MoreReplyService.class);
//        intentGetMoreReply.setAction(ACTION_GET_REPLY);
        intentGetMoreReply.putExtra("page", totalPage);
        intentGetMoreReply.putExtra("topic_id", mTopicId);
        intentGetMoreReply.putExtra("bottom", scrollToBottom);
        startService(intentGetMoreReply);
        XLog.tag(TAG).d("yes I startIntentService");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
        if (MyApp.getInstance().isLogin()) {
            menu.findItem(R.id.menu_favor).setVisible(true);
            menu.findItem(R.id.menu_reply).setVisible(true);
        } else {
            menu.findItem(R.id.menu_favor).setVisible(false);
            menu.findItem(R.id.menu_reply).setVisible(false);
        }
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_favor:
                if (isFavored) {
                    unFavor();
                } else {
                    favor();
                }
                break;
            case R.id.menu_reply:
//                etSendReply.requestFocus();
//                ObjectAnimator animator = ObjectAnimator.ofFloat(etSendReply, "translationX", 0, 50, -50, 0, 50, 0);
//                animator.setDuration(600);
//                animator.start();
                break;
            case R.id.menu_refresh:
                mSwipe.setRefreshing(true);
                getRepliesPageOne(mTopicId, false);
                break;
            case R.id.menu_item_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "来自V2EX的帖子：" + ((TopicModel) mAllContent.get(0)).getTitle() + "   "
                        + NetManager.HTTPS_V2EX_BASE + "/t/" + ((TopicModel) mAllContent.get(0)).getId());
                sendIntent.setType("text/plain");
                // createChooser 中有三大好处，自定义title
                startActivity(Intent.createChooser(sendIntent, "分享到"));
                break;
            case R.id.menu_item_open_in_browser:

                Long topicId = ((TopicModel) mAllContent.get(0)).getId();
                String url = NetManager.HTTPS_V2EX_BASE + "/t/" + topicId;
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage("com.android.chrome");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    intent.setPackage(null);
                    startActivity(intent);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void unFavor() {
        HttpHelper.OK_CLIENT.newCall(new Request.Builder()
                .headers(HttpHelper.baseHeaders)
                .url(NetManager.HTTPS_V2EX_BASE + "/unfavorite/topic/" + mTopicId + "?t=" + token)
                .get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 302) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            HintUI.t(DetailsActivity.this, "取消收藏成功");

                            getRepliesPageOne(mTopicId, false);
                        }
                    });
                }
            }
        });
    }

    private void favor() {
        HttpHelper.OK_CLIENT.newCall(new Request.Builder()
                .headers(HttpHelper.baseHeaders)
                .url(NetManager.HTTPS_V2EX_BASE + "/favorite/topic/" + mTopicId + "?t=" + token)
                .get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                dealError(DetailsActivity.this);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 302) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            HintUI.t(DetailsActivity.this, "收藏成功");
                            getRepliesPageOne(mTopicId, false);
                        }
                    });
                }
            }
        });
    }


//    public void postReply(View view) {
//        etSendReply.clearFocus();
//        XLog.tag(TAG).d("I clicked");
//        final String content = etSendReply.getText().toString();
//        RequestBody requestBody = new FormBody.Builder()
//                .add("content", content)
//                .add("once", once)
//                .build();
//        HttpHelper.OK_CLIENT.newCall(new Request.Builder()
//                .headers(baseHeaders)
//                .header("Origin", NetManager.HTTPS_V2EX_BASE)
//                .header("Referer", NetManager.HTTPS_V2EX_BASE + "/t/" + mTopicId)
//                .header("Content-Type", "application/x-www-form-urlencoded")
//                .url(NetManager.HTTPS_V2EX_BASE + "/t/" + mTopicId)
//                .post(requestBody)
//                .build()).enqueue(new Callback() {
//
//            @Override
//            public void onFailure(Call call, IOException e) {
//                dealError(DetailsActivity.this);
//            }
//
//            @Override
//            public void onResponse(Call call, okhttp3.Response response) throws IOException {
//                if (response.code() == 302) {
//                    XLog.tag(TAG).d("成功发布");
//
//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//
//                            etSendReply.setText("");
//
//                        }
//                    });
//                    getRepliesPageOne(mTopicId, true);
//                }
//            }
//        });
//    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XLog.tag(TAG).d("onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }
}
