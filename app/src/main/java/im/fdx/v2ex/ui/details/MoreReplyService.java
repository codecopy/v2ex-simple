package im.fdx.v2ex.ui.details;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.elvishew.xlog.XLog;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;

import im.fdx.v2ex.network.HttpHelper;
import im.fdx.v2ex.network.NetManager;
import okhttp3.Request;

/**
 * 为什么不用Thread？感觉Thread不高级
 */
public class MoreReplyService extends IntentService {

    public MoreReplyService(String name) {
        super(name);
    }

    public MoreReplyService() {
        this("what");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (intent == null) {
            return;
        }
        getAllMore(intent);
    }

    private void getAllMore(@NotNull Intent intent) {
        int totalPage = intent.getIntExtra("page", -1);
        String topicId = intent.getStringExtra("topic_id");
        boolean isToBottom = intent.getBooleanExtra("bottom", false);

        XLog.tag("DetailsActivity").d(totalPage + " | " + topicId);
        if (totalPage == -1 || topicId == null) {
            return;
        }

        try {
            for (int i = 2; i <= totalPage; i++) {
                okhttp3.Response response = HttpHelper.INSTANCE.getOK_CLIENT().newCall(new Request.Builder()
                        .headers(HttpHelper.INSTANCE.getBaseHeaders())
                        .url(NetManager.HTTPS_V2EX_BASE + "/t/" + topicId + "?p=" + i)
                        .build()).execute();
                Document body = Jsoup.parse(response.body().string());
                ArrayList<ReplyModel> replies = NetManager.parseResponseToReplay(body);
                String token = NetManager.parseToVerifyCode(body);

                XLog.tag("DetailsActivity").d(replies.get(0).getContent());
                Intent it = new Intent();
                it.setAction("im.fdx.v2ex.reply");
                it.putExtra("token", token);
                it.putParcelableArrayListExtra("replies", replies);

                if (i == totalPage && isToBottom) {
                    it.putExtra("bottom", true);
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(it);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void getOneMore(@NotNull Intent intent) {

        int totalPage = intent.getIntExtra("page", -1);
        long topicId = intent.getLongExtra("topic_id", -1);
        int currentPage = intent.getIntExtra("currentPage", -1);

        XLog.tag("DetailsActivity").d(totalPage + "<----totalPage | topicId :" + topicId + " |current :  " + currentPage);
        if (totalPage == -1 || topicId == -1L || currentPage == -1) {
            return;
        }

        try {
            okhttp3.Response response = HttpHelper.INSTANCE.getOK_CLIENT().newCall(new Request.Builder()
                    .headers(HttpHelper.INSTANCE.getBaseHeaders())
                    .url(NetManager.HTTPS_V2EX_BASE + "/t/" + topicId + "?p=" + currentPage)
                    .build()).execute();
            Document body = Jsoup.parse(response.body().string());
            ArrayList<ReplyModel> replies = NetManager.parseResponseToReplay(body);
            String token = NetManager.parseToVerifyCode(body);

            XLog.tag("DetailsActivity").d(replies.get(0).getContent());
            Intent it = new Intent();
            it.setAction("im.fdx.v2ex.reply");
            it.putExtra("token", token);
            it.putParcelableArrayListExtra("replies", replies);

            if (currentPage == totalPage) {
                it.putExtra("bottom", true);
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(it);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}