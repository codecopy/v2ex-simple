package im.fdx.v2ex.ui.details;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.elvishew.xlog.XLog;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import im.fdx.v2ex.MyApp;
import im.fdx.v2ex.R;
import im.fdx.v2ex.model.BaseModel;
import im.fdx.v2ex.ui.main.TopicModel;
import im.fdx.v2ex.network.HttpHelper;
import im.fdx.v2ex.network.NetManager;
import im.fdx.v2ex.network.VolleyHelper;
import im.fdx.v2ex.ui.main.TopicsRVAdapter;
import im.fdx.v2ex.utils.HintUI;
import im.fdx.v2ex.utils.TimeUtil;
import im.fdx.v2ex.view.GoodTextView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.media.CamcorderProfile.get;

/**
 * Created by fdx on 15-9-7.
 * 详情页的Adapter。
 */
public class DetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_FOOTER = 2;
    private static final String TAG = DetailsAdapter.class.getSimpleName();
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private AdapterCallback callback;
    private TopicModel mHeader;
    private List<ReplyModel> mReplyList = new ArrayList<>();
    private Context mContext;
    private List<BaseModel> mAllList;
    private String verifyCode;

    public DetailsAdapter(Context context, TopicModel header, List<ReplyModel> replyList) {
        mContext = context;
        mHeader = header;
        mReplyList = replyList;
    }

    public DetailsAdapter(Context context, List<BaseModel> allList, AdapterCallback callback) {
        mContext = context;
        mAllList = allList;
        this.callback = callback;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_details_topic_view, parent, false);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 30);
            view.setLayoutParams(lp);

            return new MainDetailViewHolder(view);
        } else if (viewType == TYPE_ITEM) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_reply_view, parent, false);
            return new ItemViewHolder(view);
        } else if (viewType == TYPE_FOOTER) {
            View view = new TextView(mContext);
            return new FooterViewHolder(view);
        }
        throw new RuntimeException(" No type that matches " + viewType + " + Make sure using types correctly");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (getItemViewType(position) == TYPE_HEADER) {

            MainDetailViewHolder mainHolder = (MainDetailViewHolder) holder;
            final TopicModel topic = ((TopicModel) mAllList.get(position));
            mainHolder.tvTitle.setText(topic.getTitle());
            mainHolder.tvContent.setSelected(true);
            mainHolder.tvContent.setGoodText(topic.getContent_rendered());
            Log.i(TAG, topic.getContent_rendered());
            Log.i(TAG, topic.getContent());
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                mainHolder.tvContent.setTransitionName("header");
//            }

//            mainHolder.tvReplyNumber.setText(String.valueOf(topic.getReplies()));
            mainHolder.tvAuthor.setText(topic.getMember().getUsername());
            mainHolder.tvNode.setText(topic.getNode().getTitle());
            mainHolder.tvCreated.setText(TimeUtil.getRelativeTime(topic.getCreated()));
            Picasso.with(mContext).load(topic.getMember().getAvatarNormalUrl()).into(mainHolder.ivAvatar);

            TopicsRVAdapter.MyOnClickListener l = new TopicsRVAdapter.MyOnClickListener(mContext, topic);
            mainHolder.tvNode.setOnClickListener(l);
            mainHolder.ivAvatar.setOnClickListener(l);

        } else if (getItemViewType(position) == TYPE_FOOTER) {
            TextView tvMore = (TextView) ((FooterViewHolder) holder).itemView;
            tvMore.setText("加载更多");
            tvMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onMethodCallback();
                }
            });
        } else if (getItemViewType(position) == TYPE_ITEM) {
            final ItemViewHolder itemVH = (ItemViewHolder) holder;
            final ReplyModel replyItem = (ReplyModel) mAllList.get(position);
            if (position == getItemCount() - 1) {
                itemVH.divider.setVisibility(View.GONE);
            }

            if (MyApp.getInstance().isLogin()) {
                itemVH.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        MenuInflater menuInflater = ((Activity) mContext).getMenuInflater();
                        menuInflater.inflate(R.menu.menu_reply, menu);

                        MenuItem.OnMenuItemClickListener menuListener = new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.menu_reply:

                                        EditText editText = (EditText) ((Activity) mContext).findViewById(R.id.et_post_reply);
                                        String text = String.format("@%s ", replyItem.getMember().getUsername());
                                        if (!editText.getText().toString().contains(text)) {
                                            SpannableString spanString = new SpannableString(text);
                                            ForegroundColorSpan span = null;
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                                span = new ForegroundColorSpan(mContext.getColor(R.color.primary));
                                            } else {
                                                span = new ForegroundColorSpan(Color.BLACK);
                                            }
                                            spanString.setSpan(span, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                            editText.append(spanString);
                                        }
                                        editText.setSelection(editText.length());
                                        editText.requestFocus();
                                        return true;
                                    case R.id.menu_thank:

                                        XLog.tag(TAG).d("hehe" + verifyCode);
                                        if (verifyCode == null) {
                                            return true;
                                        }
                                        RequestBody body = new FormBody.Builder().add("t", verifyCode).build();

                                        HttpHelper.OK_CLIENT.newCall(new Request.Builder()
                                                .headers(HttpHelper.baseHeaders)
                                                .url("https://www.v2ex.com/thank/reply/" + replyItem.getId())
                                                .post(body)
                                                .build()).enqueue(new Callback() {
                                            @Override
                                            public void onFailure(Call call, IOException e) {
                                                NetManager.dealError(mContext);
                                            }

                                            @Override
                                            public void onResponse(Call call, Response response) throws IOException {

                                                if (response.code() == 200) {
                                                    ((Activity) mContext).runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            HintUI.t(mContext, "感谢成功");
                                                            itemVH.tvThanks.setText(String.format(mContext.getResources().
                                                                    getString(R.string.show_thanks), replyItem.getThanks() + 1));
                                                        }
                                                    });
                                                } else {
                                                    NetManager.dealError(mContext, response.code());
                                                }
                                            }
                                        });

                                        break;
                                    case R.id.menu_copy:
                                        XLog.d("I click menu copy");
                                        ClipboardManager manager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                        manager.setPrimaryClip(ClipData.newPlainText("wtf", replyItem.getContent()));
                                        break;
                                }
                                return false;
                            }
                        };

                        menu.findItem(R.id.menu_reply).setOnMenuItemClickListener(menuListener);
                        menu.findItem(R.id.menu_thank).setOnMenuItemClickListener(menuListener);
                        menu.findItem(R.id.menu_copy).setOnMenuItemClickListener(menuListener);
                    }
                });

                itemVH.tvReplyTime.setText(TimeUtil.getRelativeTime(replyItem.getCreated()));
                itemVH.tvReplier.setText(replyItem.getMember().getUsername());
//                itemVH.tvThanks.setText(String.format(mContext.getResources().
//                        getString(R.string.show_thanks), replyItem.getThanks()));
                itemVH.tvThanks.setText(String.valueOf(replyItem.getThanks()));
                itemVH.tvContent.setGoodText(replyItem.getContent_rendered());
                itemVH.tvRow.setText(String.valueOf(position));
                Picasso.with(mContext).load(replyItem.getMember().getAvatarNormalUrl()).into(itemVH.ivUserAvatar);
                itemVH.ivUserAvatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent itProfile = new Intent("im.fdx.v2ex.intent.profile");
                        itProfile.putExtra("username", replyItem.getMember().getUsername());
                        mContext.startActivity(itProfile);
                    }
                });
            }
        }
    }


    @Override
    public int getItemCount() {
        return mAllList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        } else if (position == getItemCount() - 1) {
            return TYPE_FOOTER;
        } else
            return TYPE_ITEM;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    //我重用了MainAdapter中的MainViewHolder

    public static class MainDetailViewHolder extends RecyclerView.ViewHolder {

        public TextView tvTitle;
        public GoodTextView tvContent;
        public TextView tvCreated;
        public TextView tvAuthor;
        public CircleImageView ivAvatar;
        public TextView tvNode;
        public View container;

        public MainDetailViewHolder(View root) {
            super(root);
            container = root;
            tvTitle = (TextView) root.findViewById(R.id.tv_title);
            tvContent = (GoodTextView) root.findViewById(R.id.tv_content);
            tvCreated = (TextView) root.findViewById(R.id.tv_created);
            tvAuthor = (TextView) root.findViewById(R.id.tv_author);
            ivAvatar = (CircleImageView) root.findViewById(R.id.iv_avatar_profile);
            tvNode = (TextView) root.findViewById(R.id.tv_node);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView tvReplier;
        TextView tvReplyTime;
        GoodTextView tvContent;
        TextView tvRow;
        TextView tvThanks;
        CircleImageView ivUserAvatar;
        View divider;

        public ItemViewHolder(View itemView) {
            super(itemView);

            tvReplier = (TextView) itemView.findViewById(R.id.tv_replier);
            tvReplyTime = (TextView) itemView.findViewById(R.id.tv_reply_time);
            tvContent = (GoodTextView) itemView.findViewById(R.id.tv_reply_content);
            tvRow = (TextView) itemView.findViewById(R.id.tv_reply_row);
            ivUserAvatar = (CircleImageView) itemView.findViewById(R.id.iv_reply_avatar);
            tvThanks = (TextView) itemView.findViewById(R.id.tv_thanks);
            divider = itemView.findViewById(R.id.divider);

        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface AdapterCallback {
        void onMethodCallback();
    }
}
