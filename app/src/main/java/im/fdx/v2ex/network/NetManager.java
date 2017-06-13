package im.fdx.v2ex.network;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.Gson;

import org.greenrobot.greendao.annotation.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.fdx.v2ex.R;
import im.fdx.v2ex.model.MemberModel;
import im.fdx.v2ex.model.NotificationModel;
import im.fdx.v2ex.ui.details.ReplyModel;
import im.fdx.v2ex.ui.main.TopicModel;
import im.fdx.v2ex.ui.node.NodeModel;
import im.fdx.v2ex.utils.ContentUtils;
import im.fdx.v2ex.utils.TimeUtil;

import static im.fdx.v2ex.network.NetManager.Source.FROM_HOME;
import static im.fdx.v2ex.network.NetManager.Source.FROM_NODE;
import static java.lang.Integer.parseInt;

/**
 * Created by a708 on 15-8-13.
 * 用于对API和网页处理的类
 */


public class NetManager {

    private static final String TAG = NetManager.class.getSimpleName();

    public static final String HTTPS_V2EX_BASE = "https://www.v2ex.com";

    @SuppressWarnings("unused")
    public static final String API_HOT = HTTPS_V2EX_BASE + "/api/topics/hot.json";
    @SuppressWarnings("unused")
    public static final String API_LATEST = HTTPS_V2EX_BASE + "/api/topics/latest.json";

    //以下,接受参数： name: 节点名
    @SuppressWarnings("unused")
    public static final String API_NODE = HTTPS_V2EX_BASE + "/api/nodes/show.json";


    public static final String API_TOPIC = HTTPS_V2EX_BASE + "/api/topics/show.json";

    public static final String DAILY_CHECK = HTTPS_V2EX_BASE + "/mission/daily";

    public static final String SIGN_UP_URL = HTTPS_V2EX_BASE + "/signup";

    public static final String SIGN_IN_URL = HTTPS_V2EX_BASE + "/signin";
    //以下,接受以下参数之一：
    //    username: 用户名
    //    id: 用户在 V2EX 的数字 ID
    public static final String API_USER = HTTPS_V2EX_BASE + "/api/members/show.json";
    //以下接收参数：
    //     topic_id: 主题ID
    // fdx_comment: 坑爹，官网没找到。怪不得没法子
    @SuppressWarnings("unused")
    @Deprecated
    public static final String API_REPLIES = HTTPS_V2EX_BASE + "/api/replies/show.json";

    public static final String URL_ALL_NODE = HTTPS_V2EX_BASE + "/api/nodes/all.json";

    public static @NotNull
    List<NotificationModel> parseToNotifications(Document html) {
        Element body = html.body();
        Elements items = body.getElementsByAttributeValueStarting("id", "n_");
        if (items == null) {
            return Collections.emptyList();
        }

        List<NotificationModel> notificationModels = new ArrayList<>();
        for (Element item : items) {
            NotificationModel notification = new NotificationModel();

            String notificationId = item.attr("id").substring(2);
            notification.setId(notificationId);

            Element contentElement = item.getElementsByClass("payload").first();
            String content = "";
            if (contentElement != null) {
                content = contentElement.text();
            }

            String time = item.getElementsByClass("snow").first().text();
            notification.setTime(time);// 2/6


            Element memberElement = item.getElementsByTag("a").first();
            String username = memberElement.attr("href").replace("/member/", "");
            String avatarUrl = memberElement.getElementsByClass("avatar").first().attr("src");
            MemberModel memberModel = new MemberModel();
            memberModel.setUsername(username);
            memberModel.setAvatar_normal(avatarUrl);
            notification.setMember(memberModel); // 3/6

            TopicModel topicModel = new TopicModel();

            Element topicElement = item.getElementsByClass("fade").first();
//            <a href="/t/348757#reply1">交互式《线性代数》学习资料</a>
            String href = topicElement.getElementsByAttributeValueStarting("href", "/t/").first().attr("href");

            topicModel.setTitle(topicElement.getElementsByAttributeValueStarting("href", "/t/").first().text());

            Pattern p = Pattern.compile("(?<=/t/)\\d+(?=#)");
            Matcher matcher = p.matcher(href);
            if (matcher.find()) {
                String topicId = matcher.group();
                topicModel.setId(topicId);
            }

            Pattern p2 = Pattern.compile("(?<=reply)d+\\b");
            Matcher matcher2 = p2.matcher(href);
            if (matcher2.find()) {
                String replies = matcher2.group();
                notification.setReplyPosition(replies);
            }
            notification.setTopic(topicModel); //4/6
            String type = topicElement.ownText();
            notification.setType(type);
            notification.setContent(content); //1/6
            notificationModels.add(notification);
        }
        return notificationModels;
    }

    public enum Source {
        FROM_HOME, FROM_NODE
    }

    public static Gson myGson = new Gson();

    public static List<TopicModel> parseTopicLists(Document html, Source source) {
        ArrayList<TopicModel> topics = new ArrayList<>();

        //用okhttp模拟登录的话获取不到Content内容，必须再一步。

//        Document html = Jsoup.parse(string);
        Element body = html.body();

        Elements items = null;
        switch (source) {
            case FROM_HOME:
                items = body.getElementsByClass("cell item");
                break;
            case FROM_NODE:
                items = body.getElementsByAttributeValueStarting("class", "cell from");
                break;
        }
        for (Element item :
                items) {
            TopicModel topicModel = new TopicModel();
            String title = item.getElementsByClass("item_title").first().text();

            String linkWithReply = item.getElementsByClass("item_title").first()
                    .getElementsByTag("a").first().attr("href");
            int replies = Integer.parseInt(linkWithReply.split("reply")[1]);
            Pattern p = Pattern.compile("(?<=/t/)\\d+");
            Matcher matcher = p.matcher(linkWithReply);

            String id;
            if (matcher.find()) {
                id = matcher.group();
            } else {
                return Collections.emptyList();
            }


            NodeModel nodeModel = new NodeModel();
            if (source == FROM_HOME) {
                //  <a class="node" href="/go/career">职场话题</a>
                String nodeTitle = item.getElementsByClass("node").text();
                String nodeName = item.getElementsByClass("node").attr("href").substring(4);
                nodeModel.setTitle(nodeTitle);
                nodeModel.setName(nodeName);

            } else if (source == FROM_NODE) {
                Element header = body.getElementsByClass("header").first();
                String strHeader = header.text();
                String nodeTitle = "";
                if (strHeader.contains("›")) {
                    nodeTitle = strHeader.split("›")[1].split(" ")[1].trim();
                }

                Elements elements = html.head().getElementsByTag("script");
                Element script = elements.last();
                //注意，script 的tag 不含 text。
                String strScript = script.html();
                String nodeName = strScript.split("\"")[1];
                nodeModel.setTitle(nodeTitle);
                nodeModel.setName(nodeName);
            }


            topicModel.setNode(nodeModel);

            MemberModel memberModel = new MemberModel();
//            <a href="/member/wineway">
// <img src="//v2" class="avatar" border="0" align="default" style="max-width: 48px; max-height: 48px;"></a>
            String username = item.getElementsByTag("a").first().attr("href").substring(8);

            String avatarLarge = item.getElementsByClass("avatar").attr("src");
            memberModel.setUsername(username);
            memberModel.setAvatar_large(avatarLarge);
            memberModel.setAvatar_normal(avatarLarge.replace("large", "normal"));


            String smallItem = item.getElementsByClass("small fade").first().text();
            long created;
            if (!smallItem.contains("最后回复")) {
                created = -1L;
            } else {
                String createdOriginal = "";
                switch (source) {
                    case FROM_HOME:
                        createdOriginal = smallItem.split("•")[2];
                        break;
                    case FROM_NODE:
                        createdOriginal = smallItem.split("•")[1];
                        break;
                }
                created = TimeUtil.toLong(createdOriginal);
            }
            topicModel.setReplies(replies);
            topicModel.setContent("");
            topicModel.setContentRendered("");
            topicModel.setTitle(title);

            topicModel.setMember(memberModel);
            topicModel.setId(id);
            topicModel.setCreated(created);
            topics.add(topicModel);
        }
        return topics;
    }

    public static NodeModel parseToNode(Document html) {

        NodeModel nodeModel = new NodeModel();
//        Document html = Jsoup.parse(response);
        Element body = html.body();
        Element header = body.getElementsByClass("header").first();
        Element contentElement = header.getElementsByClass("f12 gray").first();
        String content = contentElement == null ? "" : contentElement.text();
        String number = header.getElementsByTag("strong").first().text();
        String strHeader = header.ownText().trim();

        if (header.getElementsByTag("img").first() != null) {
            String avatarLarge = header.getElementsByTag("img").first().attr("src");
            nodeModel.setAvatar_large(avatarLarge);
            nodeModel.setAvatar_normal(avatarLarge.replace("large", "normal"));
        }

        Elements elements = html.head().getElementsByTag("script");
        Element script = elements.last();
        //注意，script 的tag 不含 text。
        String strScript = script.html();
        String nodeName = strScript.split("\"")[1];

        nodeModel.setName(nodeName);
        nodeModel.setTitle(strHeader);
        nodeModel.setTopics(Integer.parseInt(number));
        nodeModel.setHeader(content);

        return nodeModel;
    }

    @SuppressWarnings("unused")
    public static int[] getPageValue(Element body) {

        int currentPage = 0;
        int totalPage = 0;
        Element pageInput = body.getElementsByClass("page_input").first();
        if (pageInput == null) {
            return new int[]{-1, -1};

        }
        try {
            currentPage = Integer.parseInt(pageInput.attr("value"));
            totalPage = Integer.parseInt(pageInput.attr("max"));
        } catch (NumberFormatException ignored) {

        }
        return new int[]{currentPage, totalPage};


    }

    /**
     * @param body    网页
     * @param topicId todo 可以省略
     * @return TopicModel
     */
    @NonNull
    public static TopicModel parseResponseToTopic(Element body, String topicId) {
        TopicModel topicModel = new TopicModel(topicId);

        String title = body.getElementsByTag("h1").text();
        Element contentElement = body.getElementsByClass("topic_content").first();

        String content = contentElement == null ? "" : contentElement.text();
        String contentRendered = contentElement == null ? "" : contentElement.html();
        String createdUnformed = body.getElementsByClass("header").
                first().getElementsByClass("gray").first().ownText(); // · 44 分钟前用 iPhone 发布 · 192 次点击 &nbsp;

        String time = createdUnformed.split("·")[1];
        long created = TimeUtil.toLong(time);
//long created = -1L;

        String replyNum = "";
        Elements grays = body.getElementsByClass("gray");
        boolean hasReply = false;
        for (Element gray :
                grays) {
            if (gray.text().contains("回复") && gray.text().contains("|")) {
                String wholeText = gray.text();
                int index = wholeText.indexOf("回复");
                replyNum = wholeText.substring(0, index - 1);
                if (!TextUtils.isEmpty(replyNum)) {
                    hasReply = true;
                }
                break;
            }
        }

        int replies;
        if (!hasReply) {
            replies = 0;
        } else {
            replies = parseInt(replyNum);
        }

        topicModel.setContentRendered(ContentUtils.INSTANCE.format(contentRendered)); //done

        MemberModel member = new MemberModel();
        String username = body.getElementsByClass("header").first()
                .getElementsByAttributeValueStarting("href", "/member/").text();
        member.setUsername(username);
        String largeAvatar = body.getElementsByClass("header").first()
                .getElementsByClass("avatar").attr("src");
        member.setAvatar_large(largeAvatar);
        member.setAvatar_normal(largeAvatar.replace("large", "normal"));
//                            member.setId(0);

        Element nodeElement = body.getElementsByClass("header").first()
                .getElementsByAttributeValueStarting("href", "/go/").first();
        String nodeName = nodeElement.attr("href").replace("/go/", "");
        String nodeTitle = nodeElement.text();
        NodeModel nodeModel = new NodeModel();
        nodeModel.setName(nodeName);
        nodeModel.setTitle(nodeTitle);

        topicModel.setMember(member); //done
        topicModel.setReplies(replies); //done
        topicModel.setCreated(created); //done
        topicModel.setTitle(title); //done
        topicModel.setContent(content); //done
        topicModel.setNode(nodeModel);//done
        return topicModel;
    }

    public static ArrayList<ReplyModel> parseResponseToReplay(Element body) {

        ArrayList<ReplyModel> replyModels = new ArrayList<>();

        Elements items = body.getElementsByAttributeValueStarting("id", "r_");
        for (Element item :
                items) {

//            <div id="r_4157549" class="cell">
            String id = item.id().substring(2);

            ReplyModel replyModel = new ReplyModel();
            MemberModel memberModel = new MemberModel();
            String avatar = item.getElementsByClass("avatar").attr("src");
            String username = item.getElementsByTag("strong").first().
                    getElementsByAttributeValueStarting("href", "/member/").first().text();

            memberModel.setAvatar_large(avatar);
            memberModel.setAvatar_normal(avatar.replace("large", "normal"));
            memberModel.setUsername(username);

            String thanksOriginal = item.getElementsByClass("small fade").text();
            int thanks;
            if (thanksOriginal.equals("")) {
                thanks = 0;
            } else {
                thanks = parseInt(thanksOriginal.replace("♥ ", ""));
            }

            boolean isThank = false;
            Element thanked = item.getElementsByClass("thank_area thanked").first();
            if (thanked != null && "感谢已发送".equals(thanked.text())) {
                isThank = true;
            }

            replyModel.setThanked(isThank);


            String createdOriginal = item.getElementsByClass("fade small").text();
            Element replyContent = item.getElementsByClass("reply_content").first();
            replyModel.setCreated(TimeUtil.toLong(createdOriginal));
            replyModel.setMember(memberModel);
            replyModel.setThanks(thanks);


            replyModel.setId(id);
            replyModel.setContent(replyContent.text());
            replyModel.setContent_rendered(ContentUtils.INSTANCE.format(replyContent.html()));
            replyModels.add(replyModel);
        }
        return replyModels;

    }

    public static void dealError(final Context context, final int errorCode) {

        if (context instanceof Activity)
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (errorCode) {
                        case -1:
                            Toast.makeText(context, context.getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                            break;
                        case 302:

                            Toast.makeText(context, context.getString(R.string.error_auth_failure), Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Toast.makeText(context, context.getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });
    }

    public static void dealError(Context context) {
        dealError(context, -1);
    }

    public static String parseOnce(Element body) {

        Element onceElement = body.getElementsByAttributeValue("name", "once").first();
        if (onceElement != null) {
            return onceElement.attr("value");
        }

        return null;
    }


    public static String parseToVerifyCode(Element body) {

//        <a href="/favorite/topic/349111?t=eghsuwetutngpadqplmlnmbndvkycaft" class="tb">加入收藏</a>
        Element element = null;
        try {
            element = body.getElementsByClass("topic_buttons").first().getElementsByTag("a").first();
        } catch (NullPointerException e) {
            return null;
        }
        if (element != null) {
            Pattern p = Pattern.compile("(?<=favorite/topic/\\d{1,10}\\?t=)\\w+");

            Matcher matcher = p.matcher(element.outerHtml());
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    public static ArrayList<NodeModel> parseToNode(String string) {
        Element element = Jsoup.parse(string).body();

        ArrayList<NodeModel> nodeModels = new ArrayList<>();
        Elements items = element.getElementsByClass("grid_item");

        for (Element item : items) {
            NodeModel nodeModel = new NodeModel();
            String id = item.attr("id").substring(2);
            nodeModel.setId(id);

            String title = item.getElementsByTag("div").first().ownText().trim();
            nodeModel.setTitle(title);
            String name = item.attr("href").replace("/go/", "");
            nodeModel.setName(name);

            String num = item.getElementsByTag("span").first().ownText().trim();

            nodeModel.setTopics(parseInt(num));

            String imageUrl = item.getElementsByTag("img").first().attr("src");
            nodeModel.setAvatar_large(imageUrl);
            nodeModels.add(nodeModel);
        }

        return nodeModels;
    }
}