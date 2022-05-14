package com.chaye.ccbcc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.common.utils.JSONUtils;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textViewData, tvserverjiang, tvGuanjianzi, tvShuliang, tvJiange;
    private Button buttonStart, buttonStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewData = findViewById(R.id.textViewData);
        tvJiange = findViewById(R.id.jiange);
        tvserverjiang = findViewById(R.id.serverjiang);
        tvGuanjianzi = findViewById(R.id.guanjianzi);
        tvShuliang = findViewById(R.id.shuliang);
        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(this);
        buttonStop = findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(this);
    }

    boolean isRun;

    //https://jxjkhd.kerlala.com/activity/duihuan/getDList/91/lPYDaE3N
    String url = "https://jxjkhd.kerlala.com/activity/duihuan/getDList/91/lPYDaE3N/";
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 1:
                    textViewData.setText(msg.obj.toString());
                    break;

                case 2:
                    int jiange = 5;
                    if (!tvJiange.getText().toString().isEmpty()) {
                        jiange = Integer.parseInt(tvJiange.getText().toString());
                    }
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isRun) {

                                startGetdata();
                            }
                        }
                    }, jiange * 1000);
                    break;

                case 3:
                    textViewData.setText(msg.obj.toString());
                    isRun = false;
                    buttonStart.setVisibility(View.VISIBLE);
                    buttonStop.setVisibility(View.GONE);
                    break;
                case 4:
                    tvserverjiang.setHint("满足条件了,但是你没写通知key,好亏啊");
                    break;
            }
            super.handleMessage(msg);
        }
    };


    public void startGetdata() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getData(url);
            }
        }).start();

    }

    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            getData(url);
        }
    };


    private static String getWebsiteDatetime(String webUrl) {
        try {
            URL url = new URL(webUrl);// 取得资源对象
            URLConnection uc = url.openConnection();// 生成连接对象
            uc.connect();// 发出连接
            long ld = uc.getDate();// 读取网站日期时间
            Date date = new Date(ld);// 转换为标准时间对象
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);// 输出北京时间
            return sdf.format(date);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    int tempNotify = 0;

    public void getData(String url) {
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String timestamp = System.currentTimeMillis() + "";
//        String websiteDatetime = getWebsiteDatetime("http://www.baidu.com");
        httpGet.addHeader("Timestamp", timestamp);

        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            String responseJson = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
//            String responseJson = HttpClientHelper.sendPost(url);
            Logger.debug("response generateSn length=" + responseJson.length() + ",content=" + responseJson);

            JSONArray data = JSONUtils.getJSONArray(responseJson, "data", null);
            StringBuilder showString = new StringBuilder();
            for (int i = 0; i < data.length(); i++) {
                if (i % 2 == 0) {
                    showString.append("\n");
                }

                JSONObject jsonObject1 = data.getJSONObject(i);
                String name = jsonObject1.getString("name");
                String remain_num = jsonObject1.getString("remain_num");
                showString.append(name).append(":").append(remain_num).append("\t\t\t\t");

                String shuli = "1";
                String guanjian = "";
                if (tvGuanjianzi == null || tvGuanjianzi.toString().isEmpty()) {
                    guanjian = "";
                } else {
                    guanjian = tvGuanjianzi.getText().toString();
                }

                if (tvShuliang == null || tvShuliang.toString().isEmpty()) {
                    shuli = "1";
                } else {
                    shuli = tvShuliang.getText().toString();
                    if (shuli.isEmpty()) {
                        shuli = "1";
                    }
                }


                if (name.contains(guanjian)) {
                    if(guanjian.length()>1){
                        if (Integer.parseInt(remain_num) > Integer.parseInt(shuli)) {
                            if (tempNotify != Integer.parseInt(remain_num)) {
                                if (tvserverjiang != null && tvserverjiang.length() > 10) {
                                    Logger.debug("response  serverjiang != null");
                                    String serverjiangkey = tvserverjiang.getText().toString();

                                    if (!guanjian.isEmpty()) {
                                        sendNotify("https://sc.ftqq.com/" + serverjiangkey + ".send?text=" + name + ",数量:" + remain_num + "..." + formatTime());
                                    }

                                    tempNotify = Integer.parseInt(remain_num);
                                } else {
                                    handler.obtainMessage(4).sendToTarget();
                                }
                            }
                        } else {
                            showString.append("还不够呢\n");
                        }
                    }
                }
            }
            showString.append("\n" + formatTime());
            handler.obtainMessage(1, showString).sendToTarget();
            handler.obtainMessage(2).sendToTarget();
        } catch (Exception e) {
            e.printStackTrace();

            handler.obtainMessage(3, "完了完了 出错了呀").sendToTarget();
        }

    }

    public String formatTime() {
        Date date = new Date();
        String str = "yyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(str);
        return sdf.format(date);
    }


    public String sendNotify(String URL) {
        HttpURLConnection conn = null;
        InputStream is = null;
        BufferedReader br = null;
        StringBuilder result = new StringBuilder();
        try {
            //创建远程url连接对象
            URL url = new URL(URL);
            //通过远程url连接对象打开一个连接，强转成HTTPURLConnection类
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            //设置连接超时时间和读取超时时间
            conn.setRequestProperty("Accept", "application/json");
            //发送请求
            conn.connect();
            //通过conn取得输入流，并使用Reader读取
            if (200 == conn.getResponseCode()) {
                is = conn.getInputStream();
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                    System.out.println(line);
                }
            } else {
                System.out.println("ResponseCode is an error code:" + conn.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            conn.disconnect();
        }
        return result.toString();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonStart:
                isRun = true;
                tvserverjiang.setHint("不填写的话没有实时推送");
                startGetdata();
                buttonStart.setVisibility(View.GONE);
                buttonStop.setVisibility(View.VISIBLE);
                listenerSignal(this);
                break;
            case R.id.buttonStop:
                isRun = false;

                buttonStart.setVisibility(View.VISIBLE);
                buttonStop.setVisibility(View.GONE);
                break;
        }
    }
    public TelephonyManager telephoneManager;
    public PhoneStateListener phoneStateListener;

    public void listenerSignal(Context context) {
        telephoneManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                Logger.debug("onServiceStateChanged++++");
                super.onServiceStateChanged(serviceState);
            }


            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                Logger.debug("onSignalStrengthsChanged++++");
                super.onSignalStrengthsChanged(signalStrength);

                int level = signalStrength.getLevel();//Call requires API level 23
                int asu = getMethod(signalStrength, "getAsuLevel");//Hide
                int dbm = getMethod(signalStrength, "getDbm");//Hide
            }
        };
    }


    int getMethod(SignalStrength signalStrength, String name) {
        Class<?> clazz = signalStrength.getClass();
        Method method = null;
        int i = 0;
        try {
            method = clazz.getMethod(name);//getDbm
            Object result = method.invoke(signalStrength);
            i = Integer.parseInt(String.valueOf(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

}