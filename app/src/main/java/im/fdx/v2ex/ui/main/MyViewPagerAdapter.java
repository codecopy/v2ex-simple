package im.fdx.v2ex.ui.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import im.fdx.v2ex.MyApp;
import im.fdx.v2ex.R;
import im.fdx.v2ex.utils.Keys;

import static im.fdx.v2ex.MyApp.USE_WEB;
import static im.fdx.v2ex.MyApp.USE_API;

/**
 * Created by fdx on 2015/10/15.
 * 从MainActivity分离出来. 用了FragmentStatePagerAdapter 替代FragmentPagerAdapter，才可以动态切换Fragment
 * 目前实现了两种视图。用JsonAPI+Volley 和 模拟web + okhttp
 */
class MyViewPagerAdapter extends FragmentStatePagerAdapter {

    private List<TopicsFragment> mFragments = new ArrayList<>();
    private List<String> mTabTitles = new ArrayList<>();
    private Context mContext;

    public MyViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.mContext = context;
        initFragment();
    }

    public synchronized void initFragment() {

        if (MyApp.getInstance().getHttpMode() == USE_WEB) {
            //        web homepage tab topic
            mTabTitles.clear();
            mFragments.clear();
            String[] tabTitles = mContext.getResources().getStringArray(R.array.v2ex_favorite_tab_titles);
            String[] tabPaths = mContext.getResources().getStringArray(R.array.v2ex_favorite_tab_paths);
            for (int i = 0; i < tabPaths.length; ++i) {
                TopicsFragment fragment = new TopicsFragment();
                Bundle bundle = new Bundle();
                bundle.putString(Keys.KEY_TAB, tabPaths[i]);
                fragment.setArguments(bundle);
                mFragments.add(fragment);
                mTabTitles.add(tabTitles[i]);
            }
        } else if (MyApp.getInstance().getHttpMode() == USE_API) {
            //Latest topic
            mTabTitles.clear();
            mFragments.clear();
            TopicsFragment latestTopicsFragment = new TopicsFragment();
            Bundle bundleLatest = new Bundle();
            bundleLatest.putInt(Keys.KEY_COLUMN_ID, TopicsFragment.LATEST_TOPICS);
            latestTopicsFragment.setArguments(bundleLatest);
            mFragments.add(latestTopicsFragment);
            mTabTitles.add(mContext.getString(R.string.tab_title_latest));

            //Top10 topic
            TopicsFragment Top10TopicsFragment = new TopicsFragment();
            Bundle bundleTop10 = new Bundle();
            bundleTop10.putInt(Keys.KEY_COLUMN_ID, TopicsFragment.TOP_10_TOPICS);

            Top10TopicsFragment.setArguments(bundleTop10);
            mFragments.add(Top10TopicsFragment);
            mTabTitles.add(mContext.getString(R.string.tab_title_top10));
        }
    }

    @Override
    public Fragment getItem(int position) {

        return mFragments.get(position);
    }


    @Override
    public int getCount() {
        return mTabTitles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles.get(position);
    }

}
