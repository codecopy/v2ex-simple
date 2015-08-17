package im.fdx.v2ex.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import im.fdx.v2ex.R;
import im.fdx.v2ex.V2exJsonManager;
import im.fdx.v2ex.ui.adapter.MainRecyclerViewAdapter;

public class MainActivity extends Activity {


    RecyclerView mRecyclerView;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManger;

    //测试用的string
    private String[] mydataset = new String[]{"呵呵","哈哈","我去年买了个表"};


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //找出recyclerview,并赋予变量
        mRecyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);


        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));//这里用线性显示 类似于listview

        //将要显示的数据mydataset传入MainRecyclerViewAdapter,生成一个我们能用的mAdapter
        mAdapter = new MainRecyclerViewAdapter(mydataset);
        //然后显示.打工告成
        mRecyclerView.setAdapter(mAdapter);

    }
}
