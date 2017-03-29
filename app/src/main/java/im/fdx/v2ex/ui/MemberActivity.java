package im.fdx.v2ex.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.elvishew.xlog.XLog;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.fdx.v2ex.BuildConfig;
import im.fdx.v2ex.R;
import im.fdx.v2ex.model.MemberModel;
import im.fdx.v2ex.model.TopicModel;
import im.fdx.v2ex.network.HttpHelper;
import im.fdx.v2ex.network.NetManager;
import im.fdx.v2ex.network.VolleyHelper;
import im.fdx.v2ex.ui.main.TopicsRVAdapter;
import im.fdx.v2ex.utils.HintUI;
import im.fdx.v2ex.utils.Keys;
import im.fdx.v2ex.utils.TimeHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import static com.elvishew.xlog.XLog.tag;
import static im.fdx.v2ex.network.NetManager.*;


/**
 * 获取user的主题，依然使用api的方式
 */
public class MemberActivity extends AppCompatActivity {


    public static final String TAG = MemberActivity.class.getSimpleName();
    private final ImageLoader imageLoader = VolleyHelper.getInstance().getImageLoader();
    private TextView mTvUsername;
    private ImageView mIvAvatar;
    private TextView mTvId;
    private TextView mTvUserCreated;
    private TextView mTvIntro;
    private TextView mTvLocation;
    private TextView mTvBitcoin;
    private TextView mTvGithub;
    private TextView mTvTwitter;
    private TextView mTvWebsite;
    public int mHttpMode = 2;
    private static final int MSG_GET_USER_INFO = 0;
    private static final int MSG_GET_TOPIC = 1;
    private List<TopicModel> mTopics = new ArrayList<>();

    private String username;
    private ActionBar actionBar;
    private TopicsRVAdapter mAdapter;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_GET_USER_INFO) {
                showUser((String) msg.obj);
            } else if (msg.what == MSG_GET_TOPIC) {
                mAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
//                HintUI.t(ProfileActivity.this,"get topic data");
            }
            return false;
        }
    });
    private Toolbar toolbar;
    private String urlTopic;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private AppBarLayout appBarLayout;
    private CardView cardView;
    private MemberModel member;
    private String tBlock;
    private String onceForFollow;
    private Boolean isBlock;
    private Boolean isFollowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mTvUsername = (TextView) findViewById(R.id.tv_username_profile);
        mIvAvatar = (ImageView) findViewById(R.id.iv_avatar_profile);
        mTvId = ((TextView) findViewById(R.id.tv_id));
        mTvUserCreated = (TextView) findViewById(R.id.tv_created);
        mTvIntro = (TextView) findViewById(R.id.tv_intro);


        mTvLocation = ((TextView) findViewById(R.id.tv_location));
        mTvBitcoin = (TextView) findViewById(R.id.tv_bitcoin);
        mTvGithub = (TextView) findViewById(R.id.tv_github);
        mTvTwitter = (TextView) findViewById(R.id.tv_twitter);
        mTvWebsite = (TextView) findViewById(R.id.tv_website);

        {
            mTvLocation.setVisibility(View.GONE);
            mTvBitcoin.setVisibility(View.GONE);
            mTvGithub.setVisibility(View.GONE);
            mTvTwitter.setVisibility(View.GONE);
            mTvWebsite.setVisibility(View.GONE);
        }


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_of_topic);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getTopicsByUsername();
            }
        });

        appBarLayout = (AppBarLayout) findViewById(R.id.al_profile);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.ctl_profile);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

                int maxScroll = appBarLayout.getTotalScrollRange();
                float percentage = (float) Math.abs(verticalOffset) / (float) maxScroll;
                handleAlphaOnTitle(percentage);
            }
        });

        cardView = (CardView) findViewById(R.id.cv);

        RecyclerView rv = (RecyclerView) findViewById(R.id.rv_topics_of_user);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MemberActivity.this);

        rv.setLayoutManager(layoutManager);
        mAdapter = new TopicsRVAdapter(this, mTopics);
        rv.setAdapter(mAdapter);
        parseIntent(getIntent());


    }

    private void handleAlphaOnTitle(float percentage) {
        if (percentage > 0.8f && percentage <= 1f) {
            cardView.setVisibility(View.INVISIBLE);
            collapsingToolbarLayout.setTitle(username);//设置title不显示
        } else if (percentage <= 0.8f && percentage >= 0f) {
            cardView.setVisibility(View.VISIBLE);
            collapsingToolbarLayout.setTitle("");//设置title不显示
        }

    }


    // 设置渐变的动画
    public static void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }

    private void parseIntent(Intent intent) {

        Uri appLinkData = intent.getData();
        String urlUserInfo = "";
        if (appLinkData != null) {
            String scheme = appLinkData.getScheme();
            String host = appLinkData.getHost();
            List<String> params = appLinkData.getPathSegments();
            if (host.contains("v2ex.com") && params.get(0).contains("member")) {
                username = params.get(1);
                urlUserInfo = API_USER + "?username=" + username;
            }
        } else if (intent.getExtras() != null) {
            username = getIntent().getExtras().getString(Keys.KEY_USERNAME);
            urlUserInfo = API_USER + "?username=" + username;
        } else if (BuildConfig.DEBUG) {
            username = "Livid";
            urlUserInfo = API_USER + "?username=" + username;  //Livid's profile
        }
        urlTopic = API_TOPIC + "?username=" + username;

        Log.i(TAG, urlUserInfo + "\n" + urlTopic);
        //// TODO: 2017/3/20 可以改成一步，分析下性能
            getUserInfo(urlUserInfo);
            getTopicsByUsername();
        getBlockAndFollow();
    }

    private void getBlockAndFollow() {
        String webUrl = "https://www.v2ex.com/member/" + username;
        HttpHelper.OK_CLIENT.newCall(new Request.Builder().headers(HttpHelper.baseHeaders)
                .url(webUrl)
                .get().build()).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                NetManager.dealError();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    String html = response.body().string();
                    Element body = Jsoup.parse(html).body();

                    isBlock = parseIsBlock(html);
                    isFollowed = parseIsFollowed(html);
                    tBlock = parseToBlock(html);
                    onceForFollow = parseToOnce(html);
                }
            }
        });
    }

    private Boolean parseIsFollowed(String html) {


        Pattern pFollow = Pattern.compile("un(?=follow/\\d{1,8}\\?once=)");
        Matcher matcher = pFollow.matcher(html);
        return matcher.find();

    }

    private Boolean parseIsBlock(String html) {
        Pattern pFollow = Pattern.compile("un(?=block/\\d{1,8}\\?t=)");
        Matcher matcher = pFollow.matcher(html);
        return matcher.find();
    }

    private String parseToOnce(String html) {

//        <input type="button" value="加入特别关注"
// onclick="if (confirm('确认要开始关注 SoulGem？'))
// { location.href = '/follow/209351?once=61676'; }" class="super special button">

        Pattern pFollow = Pattern.compile("(?<=follow/\\d{1,8}\\?once=)//d{1,10}");
        Matcher matcher = pFollow.matcher(html);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String parseToBlock(String html) {
//        <input type="button" value="Block" onclick="if (confirm('确认要屏蔽 SoulGem？'))
// { location.href = '/block/209351?t=1490028444'; }" class="super normal button">
        Pattern pFollow = Pattern.compile("(?<=block/\\d{1,8}\\?t=)//d{1,20}");
        Matcher matcher = pFollow.matcher(html);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void getUserInfo(String urlUserInfo) {
        HttpHelper.OK_CLIENT.newCall(new Request.Builder().headers(HttpHelper.baseHeaders)
                .url(urlUserInfo).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                tag("profile").e("网络异常");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HintUI.t(MemberActivity.this, "网络异常");
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                String body = response.body().string();
                Message.obtain(handler, MSG_GET_USER_INFO, body).sendToTarget();
            }
        });
    }

    private void getTopicsByUsername() {

        HttpHelper.OK_CLIENT.newCall(new Request.Builder().headers(HttpHelper.baseHeaders)
                .url(urlTopic).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {

                String body = response.body().string();
                Type type = new TypeToken<ArrayList<TopicModel>>() {
                }.getType();
                List<TopicModel> topicModels = myGson.fromJson(body, type);

                mAdapter.updateData(topicModels);
                XLog.tag("profile").i(topicModels.get(0).getTitle());
                Message.obtain(handler, MSG_GET_TOPIC).sendToTarget();
            }
        });
    }

    public void openTwitter(View view) {
        if (TextUtils.isEmpty(member.getTwitter())) {
            return;
        }
        Intent intent;
        try {
            // get the Twitter app if possible
            this.getPackageManager().getPackageInfo("com.twitter.android", 0);
            intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("twitter://user?screen_name=" + member.getTwitter()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            // no Twitter app, revert to browser
            intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://twitter.com/" + member.getTwitter()));
        }
        this.startActivity(intent);
    }

    public void openWeb(View view) {
        if (TextUtils.isEmpty(member.getWebsite())) {
            return;
        }
        String text;
        if (!member.getWebsite().contains("http")) {
            text = "http://" + member.getWebsite();
        } else {
            text = member.getWebsite();
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
        startActivity(intent);
    }

    public void openGithub(View view) {
        if (TextUtils.isEmpty(member.getGithub())) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.github.com/" + member.getGithub()));
        startActivity(intent);
    }

    private void showUser(String response) {
        member = myGson.fromJson(response, MemberModel.class);
        mTvUsername.setText(member.getUsername());

//        toolbar.setTitle(member.getUsername());

        Picasso.with(this).load(member.getAvatarLargeUrl()).into(mIvAvatar);
        mTvId.setText(getString(R.string.the_n_member, member.getId()));
        mTvIntro.setText(member.getBio());
        mTvUserCreated.setText(TimeHelper.getAbsoluteTime(Long.parseLong(member.getCreated())));

        boolean debug_view = false;
        if (debug_view || TextUtils.isEmpty(member.getBtc())) {
            mTvBitcoin.setVisibility(View.GONE);
        } else {
            mTvBitcoin.setVisibility(View.VISIBLE);
            mTvBitcoin.setText(member.getBtc());
        }
        if (debug_view || TextUtils.isEmpty(member.getGithub())) {
            mTvGithub.setVisibility(View.GONE);
        } else {
            mTvGithub.setVisibility(View.VISIBLE);
            mTvGithub.setText(member.getGithub());
        }

        if (debug_view || TextUtils.isEmpty(member.getLocation())) {
            mTvLocation.setVisibility(View.GONE);
        } else {
            mTvLocation.setVisibility(View.VISIBLE);
            mTvLocation.setText(member.getLocation());
        }

        if (debug_view || TextUtils.isEmpty(member.getTwitter())) {
            mTvTwitter.setVisibility(View.GONE);
        } else {
            mTvTwitter.setVisibility(View.VISIBLE);
            mTvTwitter.setText(member.getTwitter());
        }

        if (debug_view || TextUtils.isEmpty(member.getWebsite())) {
            mTvWebsite.setVisibility(View.GONE);
        } else {
            mTvWebsite.setVisibility(View.VISIBLE);
            mTvWebsite.setText(member.getWebsite());

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        parseIntent(intent);


    }

}
