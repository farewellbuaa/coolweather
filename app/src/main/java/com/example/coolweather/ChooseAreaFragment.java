package com.example.coolweather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

    public static final String HTTP_GUOLIN_TECH_API_CHINA = "http://guolin.tech/api/china/";
    private static final String TAG =  "ChooseAreaFragment";

    private Button backButton;
    private TextView titleTextView;
    private RecyclerView recyclerView;

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private Province selectedProvince;
    private City selectedCity;
    private County selectedCounty;

    private List<String> dataList = new ArrayList<String>();

    private Adapter adapter = new Adapter(dataList);

    private ProgressDialog progressDialog;
    private static final int TYPE_PROVINCE = 0;
    private static final int TYPE_CITY = 1;
    private static final int TYPE_COUNTY = 2;

    private static final int ON_PROVINCE_LEVEL = 0;
    private static final int ON_CITY_LEVEL = 1;
    private static final int ON_COUNTY_LEVEL = 2;

    private int current_level;

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private List<String> list;

        public Adapter(List<String> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public Adapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            final Holder holder = new Holder(view);
            holder.holderView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getAdapterPosition();
                    switch (current_level){
                        case ON_PROVINCE_LEVEL:
                            selectedProvince = provinceList.get(position);
                            queryCities();
                            break;
                        case ON_CITY_LEVEL:
                            selectedCity = cityList.get(position);
                            queryCounties();
                            break;
                        case ON_COUNTY_LEVEL:
                            break;
                    }
                }
            });
            return holder;
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
            public View holderView;
            public TextView text_view;
            public Holder(@NonNull View view) {
                super(view);
                holderView = view;
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
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (current_level){
                    case ON_COUNTY_LEVEL:
                        queryCities();
                        break;
                    case ON_CITY_LEVEL:
                        queryProvinces();
                        break;
                    case ON_PROVINCE_LEVEL:
                        break;
                }
            }
        });
        titleTextView = view.findViewById(R.id.title_text);
        recyclerView = view.findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        queryProvinces();
    }

    private void queryProvinces() {
        provinceList =  DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            titleTextView.setText("中国");
            backButton.setVisibility(View.GONE);

            dataList.clear();
            for (Province p : provinceList){
                dataList.add(p.getProvinceName());
            }

            current_level = ON_PROVINCE_LEVEL;
            adapter.notifyDataSetChanged();
        }else{
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, TYPE_PROVINCE);
        }
    }

    private void queryCities(){
        cityList =  DataSupport.where("provinceId = ?", String.valueOf(selectedProvince.getProvinceCode())).find(City.class);
        if (cityList.size()>0){

            titleTextView.setText(selectedProvince.getProvinceName());
            backButton.setVisibility(View.VISIBLE);

            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            current_level = ON_CITY_LEVEL;
        }else{
            String address = HTTP_GUOLIN_TECH_API_CHINA + selectedProvince.getProvinceCode() ;
            queryFromServer(address, TYPE_CITY);
        }
    }

    private void queryCounties(){

        countyList = DataSupport.where("cityId = ?", String.valueOf(selectedCity.getCityCode())).find(County.class);

        if(countyList.size()>0){
            titleTextView.setText(selectedCity.getCityName());
            backButton.setVisibility(View.VISIBLE);

            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }

            current_level = ON_COUNTY_LEVEL;
            adapter.notifyDataSetChanged();
        } else {
            String address = HTTP_GUOLIN_TECH_API_CHINA + selectedProvince.getProvinceCode() + "/" + selectedCity.getCityCode();
            queryFromServer(address, TYPE_COUNTY);
        }
    }

    private void queryFromServer(String address, final int type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String jsonString = response.body().string();
                boolean result = false;

                switch (type){
                    case TYPE_PROVINCE:
                        result = Utility.handleProvinceResponse(jsonString);
                        break;
                    case TYPE_CITY:
                        result = Utility.handleCityResponse(jsonString, selectedProvince.getProvinceCode());
                        break;
                    case TYPE_COUNTY:
                        result = Utility.handleCountyResponse(jsonString, selectedCity.getCityCode());
                        break;
                }

                if (result){
                    closeProgressDialog();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (type){
                                case TYPE_PROVINCE:
                                    queryProvinces();
                                    break;
                                case TYPE_CITY:
                                    queryCities();
                                    break;
                                case TYPE_COUNTY:
                                    queryCounties();
                                    break;
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog==null){
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog!=null)
            progressDialog.dismiss();
    }
}
