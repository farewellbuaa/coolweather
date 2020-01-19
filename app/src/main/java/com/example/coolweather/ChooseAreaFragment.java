package com.example.coolweather;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    private Button backButton;
    private TextView titleTextView;
    private RecyclerView recyclerView;

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private List<String> dataList = new ArrayList<String>();

    private Adapter adapter = new Adapter(dataList);

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private List<String> list;

        public Adapter(List<String> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public Adapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Adapter.Holder holder, int position) {
            String address = list.get(position);
            holder.text_view.setText(address);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            public TextView text_view;
            public Holder(@NonNull View view) {
                super(view);
                text_view = view.findViewById(android.R.id.text1);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.choose_area, container, false);
        backButton = view.findViewById(R.id.back_button);
        titleTextView = view.findViewById(R.id.title_text);
        recyclerView = view.findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        HttpUtil.sendOkHttpRequest("http://guolin.tech/api/china", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("ChooseAreaFragment", "onFailure: ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("ChooseAreaFragment", "onResponse: ");

                final String jsonString = response.body().string();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Utility.handleProvinceResponse(jsonString)){
                            dataList.clear();
                            provinceList = DataSupport.findAll(Province.class);
                            for (Province p : provinceList){
                                dataList.add(p.getProvinceName());
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                });

            }
        });

    }
}
