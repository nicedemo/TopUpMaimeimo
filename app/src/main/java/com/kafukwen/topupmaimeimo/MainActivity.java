package com.kafukwen.topupmaimeimo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String GET_PROXY_HOST = "http://www.kuaidaili.com/free/inha/";
    private final static String TEST_PROXY = "https://www.baidu.com/";
    private final static int PROXY_LIST_PAGE_NUM = 10;

    private EditText mShareLink;
    private Button mTopUp;
    private TextView mLog;
    private TextView mClear;
    private NestedScrollView mScrollView;
    private StringBuilder mLogSb = new StringBuilder();
    private List<ProxyModel> mProxyList = new ArrayList<>();
    private List<ProxyModel> mUsefulProxyList = new ArrayList<>();

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mLog.setText(mLogSb);
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mShareLink = findViewById(R.id.share_link);
        mShareLink.setHint("请输入分享链接");
        mTopUp = findViewById(R.id.top_up);
        mLog = findViewById(R.id.log);
        mClear = findViewById(R.id.clear);
        mScrollView = findViewById(R.id.scrollView);

        mTopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topUp(mShareLink.getText().toString());
            }
        });

        mClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLogSb.delete(0, mLogSb.length() - 1);
                mLog.setText(mLogSb);
            }
        });
    }

    private void topUp(final String link) {
        if (link != null && link.length() > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        getProxyList();

                        getUsefulProxyList();

                        topUpAction(link);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            Toast.makeText(this, "请输入分享链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void topUpAction(String link) throws InterruptedException {
        mLogSb.append("开始请求分享连接\n");

        for (int i = 0; i < mUsefulProxyList.size(); i++) {

            if (get(link, mUsefulProxyList.get(i)) == 200) {
                mLogSb.append("top up success! : ").append(mUsefulProxyList.get(i).toString()).append("\n");
            } else {
                mLogSb.append("top up fail! : ").append(mUsefulProxyList.get(i).toString()).append("\n");
            }

            mHandler.sendEmptyMessage(200);

            Thread.sleep(2000);
        }

        mLogSb.append("work done!");
    }

    private void getUsefulProxyList() throws InterruptedException {
        mLogSb.append("测试代理ip\n");

        for (int i = 0; i < mProxyList.size(); i++) {
            ProxyModel proxy = mProxyList.get(i);
            if (get(TEST_PROXY, proxy) == 200) {
                mLogSb.append("connect success: ").append(proxy.toString()).append("\n");
                mUsefulProxyList.add(proxy);
            } else {
                mLogSb.append("connect time out: ").append(proxy.toString()).append("\n");
            }

            mHandler.sendEmptyMessage(200);

            Thread.sleep(2000);
        }
    }

    private void getProxyList() throws InterruptedException {
        mLogSb.append("获取代理ip\n");

        for (int index = 1; index <= PROXY_LIST_PAGE_NUM; index++) {
            Document doc = null;
            try {
                doc = Jsoup.connect(GET_PROXY_HOST + index).get();
            } catch (IOException e) {
                e.printStackTrace();
                mLogSb.append("page error : ").append(GET_PROXY_HOST + index).append("\n");
                mHandler.sendEmptyMessage(200);
                continue;

            }
            if (doc.select("tbody").size() == 0) {
                mLogSb.append("page error : ").append(GET_PROXY_HOST + index).append("\n");
                mHandler.sendEmptyMessage(200);
                continue;
            }

            Element tbody = doc.select("tbody").get(0);
            Elements rows = tbody.select("tr");

            for (int i = 1; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td");
                String ip = cols.get(0).text();
                int port = Integer.parseInt(cols.get(1).text());
                mLogSb.append(ip).append(":").append(port).append("\n");
                mProxyList.add(new ProxyModel(ip, port));
            }

            mHandler.sendEmptyMessage(200);

            Thread.sleep(5000);
        }
    }

    private int get(String urlStr, ProxyModel proxy) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxy.ip, proxy.port)));
            conn.setRequestMethod("GET");
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(10000);
            conn.connect();

            int requestCode = conn.getResponseCode();
            return requestCode;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 400;
    }

    private class ProxyModel {
        public ProxyModel(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public String toString() {
            return this.ip + ":" + this.port;
        }

        private final String ip;
        private final int port;
    }
}
