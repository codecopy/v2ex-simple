package im.fdx.v2ex.ui.fragment;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import im.fdx.v2ex.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class NodeFragment extends Fragment {


    public NodeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_node, container, false);
    }


}
