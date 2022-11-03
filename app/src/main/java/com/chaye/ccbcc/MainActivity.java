package com.chaye.ccbcc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.common.utils.JSONUtils;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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

    private static final String TAG = "MainActivity";
    private TextView textViewData, tvserverjiang, tvGuanjianzi, tvShuliang, tvJiange;
    private Button buttonStart, buttonStop;
    private final int NOTIFY_ID = 0x123;            //通知的ID
    NotificationManager notificationManager;
    NotificationCompat.Builder notification;
    MediaPlayer mMediaPlayer;
    AssetFileDescriptor afd;

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        textViewData = findViewById(R.id.textViewData);
        textViewData.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvJiange = findViewById(R.id.jiange);
        tvserverjiang = findViewById(R.id.serverjiang);
        tvGuanjianzi = findViewById(R.id.guanjianzi);
        tvShuliang = findViewById(R.id.shuliang);
        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(this);
        buttonStop = findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(this);
        mMediaPlayer = new MediaPlayer();
        try {
            afd = getAssets().openFd("tipsringtone.mp3");
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String CHANEL_ID = "my_channel_01";
        String CHANEL_NAME = "我是渠道名字8.0need";
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //Android 8.0开始要设置通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANEL_ID, CHANEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        //创建通知
        notification = new NotificationCompat.Builder(context, CHANEL_ID)
                .setContentTitle("普大喜奔日上")
                .setContentText("点击开始按钮触发监控")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
//                .setOngoing(true)
//                .setAutoCancel(true);
        notificationManager.notify(NOTIFY_ID, notification.build());
    }

    boolean isRun;

    //https://jxjkhd.kerlala.com/activity/duihuan/getDList/91/lPYDaE3N
//    String url = "https://jxjkhd.kerlala.com/activity/duihuan/getDList/91/lPYDaE3N/"; JIANHANG CCB
    String rishangURL = "https://shfw.spdbccc.com.cn/api/shfw-mall/good/query-detail/"; // PU DA XI BEN, RISHANG YOUHUIQUAN
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
//                    tvserverjiang.setHint("满足条件了,但是你没写通知key,好亏啊");
                    try {
                        mMediaPlayer.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

    public void startGetdata() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getDataByPOST(rishangURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            try {
                getDataByPOST(rishangURL);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private static final String DEF_CHATSET = "UTF-8";
    private static int DEF_CONN_TIMEOUT = 30000;

    public static String doPOST(String strUrl) throws Exception {

        String params = "{\"prodNo\":\"PF20220728000057\",\"canal\":\"2\"}";

        HttpURLConnection conn = null;
        BufferedReader reader = null;
        String rs = null;
        try {
            StringBuffer sb = new StringBuffer();
            URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setUseCaches(false);
            conn.setConnectTimeout(DEF_CONN_TIMEOUT);
            conn.setInstanceFollowRedirects(false);
            conn.connect();
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.write(params.getBytes());
            out.flush();
            int code = conn.getResponseCode();
            Logger.debug("doPOST postRequestCode: " + code);
            InputStream is = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, DEF_CHATSET));
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                sb.append(strRead);
            }
            rs = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        Logger.debug("doPOST  " + rs);
        return rs;
    }


    public void getDataByPOST(String url) {
//        HttpPost httpPost = new HttpPost(url);
//        CloseableHttpClient httpClient = HttpClients.createDefault();
        String timestamp = System.currentTimeMillis() + "";
//        String websiteDatetime = getWebsiteDatetime("http://www.baidu.com");
//        httpPost.addHeader("Timestamp", timestamp);


        try {
//            CloseableHttpResponse response = httpClient.execute(httpPost);
//            String responseJson = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
            String responseJson = doPOST(url);
            int responesecode = JSONUtils.getInt(responseJson, "code", -1);
            if (responesecode == 0) {
                JSONObject dataObject = JSONUtils.getJSONObject(responseJson, "data", null);
                StringBuilder showString = new StringBuilder();
                showString.append(formatTime()).append("\n\n");
                showString.append(dataObject.toString());
                int jsonObject_prodRemainingStock = dataObject.getInt("prodRemainingStock");
                String jsonObject_activeButtonName = dataObject.getString("activeButtonName");
                Logger.debug("jsonObject_prodRemainingStock " + jsonObject_prodRemainingStock);
                Logger.debug("jsonObject_activeButtonName " + jsonObject_activeButtonName);

                String shuliang = "0";
                String guanjian = "";
                if (tvGuanjianzi == null || tvGuanjianzi.toString().isEmpty()) {
                    guanjian = "";
                } else {
                    guanjian = tvGuanjianzi.getText().toString();
                }

                if (tvShuliang == null || tvShuliang.toString().isEmpty()) {
                    shuliang = "0";
                } else {
                    shuliang = tvShuliang.getText().toString();
                    if (shuliang.isEmpty()) {
                        shuliang = "0";
                    }
                }

                if (jsonObject_prodRemainingStock > Integer.parseInt(shuliang) || !jsonObject_activeButtonName.equals("已售罄")) {
                    if (tvserverjiang != null && tvserverjiang.length() > 10) {
                        Logger.debug("response  serverjiang != null");
                        String serverjiangkey = tvserverjiang.getText().toString();

                        if (!guanjian.isEmpty()) {
                            sendNotify("https://sc.ftqq.com/" + serverjiangkey + ".send?text=" + jsonObject_activeButtonName + ",数量:" + jsonObject_prodRemainingStock + "..." + formatTime());
                        }
                    } else {
                        notification.setContentText("有货了!! = " + jsonObject_activeButtonName + "," + jsonObject_prodRemainingStock);
                        //设置发送时间
                        notification.setWhen(System.currentTimeMillis());
                        notificationManager.notify(NOTIFY_ID, notification.build());
                        Logger.debug(".obtainMessage(4)");
                        handler.obtainMessage(4).sendToTarget();
                    }
                } else {
                    showString.append("还不够呢\n");
                }
                handler.obtainMessage(1, showString).sendToTarget();
                handler.obtainMessage(2).sendToTarget();
            } else {
                handler.obtainMessage(3, "code != 0 出错了呀").sendToTarget();

            }
        } catch (Exception e) {
            e.printStackTrace();
            handler.obtainMessage(3, "完了完了 出错了呀").sendToTarget();
        }
    }


    int tempNotify = 0;

    public void getDataByGET(String url) {
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
                    shuli = "0";
                    tvShuliang.setText("0");
                } else {
                    shuli = tvShuliang.getText().toString();
                    if (shuli.isEmpty()) {
                        shuli = "0";
                        tvShuliang.setText("0");
                    }
                }


                if (name.contains(guanjian)) {
                    if (guanjian.length() > 1) {
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
                notification.setContentText("正在监控中");
                notification.setWhen(System.currentTimeMillis());
                notificationManager.notify(NOTIFY_ID, notification.build());
                buttonStart.setVisibility(View.GONE);
                buttonStop.setVisibility(View.VISIBLE);
                listenerSignal(this);
                break;
            case R.id.buttonStop:
                isRun = false;
                buttonStart.setVisibility(View.VISIBLE);
                buttonStop.setVisibility(View.GONE);
                notification.setContentText("已停止.点击开始按钮触发监控");
                notification.setWhen(System.currentTimeMillis());
                notificationManager.notify(NOTIFY_ID, notification.build());

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