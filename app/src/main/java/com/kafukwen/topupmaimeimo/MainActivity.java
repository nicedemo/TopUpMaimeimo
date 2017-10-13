package com.kafukwen.topupmaimeimo;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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

    private final static String GET_PROXY_HOST_1 = "http://www.kuaidaili.com/free/inha/";
    private final static String GET_PROXY_HOST_2 = "http://www.xicidaili.com/wt/"; // better
    private final static int PROXY_LIST_PAGE_NUM = 10;

    private EditText mShareLink;
    private TextView mLog;
    private NestedScrollView mScrollView;
    private StringBuilder mLogSb = new StringBuilder();
    private List<ProxyModel> mProxyList = new ArrayList<>();

    private AlertDialog.Builder mBuilder;
    private String getProxyHost = GET_PROXY_HOST_2;

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
        mLog = findViewById(R.id.log);
        mScrollView = findViewById(R.id.scrollView);

        findViewById(R.id.top_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topUp(mShareLink.getText().toString());
            }
        });

        findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLogSb.delete(0, mLogSb.length() - 1);
                mLog.setText(mLogSb);
            }
        });

        mBuilder = new AlertDialog.Builder(MainActivity.this)
                .setItems(new CharSequence[]{GET_PROXY_HOST_1, GET_PROXY_HOST_2}
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == 0) {
                                    getProxyHost = GET_PROXY_HOST_1;
                                } else {
                                    getProxyHost = GET_PROXY_HOST_2;
                                }
                            }
                        });

        findViewById(R.id.switch_proxy_pool).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBuilder.create().show();
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

        for (int i = 0; i < mProxyList.size(); i++) {

            if (get(link, mProxyList.get(i)) == 200) {
                mLogSb.append("top up success! : ").append(mProxyList.get(i).toString()).append("\n");
            } else {
                mLogSb.append("top up fail! : ").append(mProxyList.get(i).toString()).append("\n");
            }

            mHandler.sendEmptyMessage(200);

            Thread.sleep(2000);
        }

        mLogSb.append("work done!");
    }

    private void getProxyList() throws InterruptedException {
        mLogSb.append("获取代理ip\n");

        String cssQueryTag;
        int ipColIndex;
        int portColIndex;

        if (GET_PROXY_HOST_1.equals(getProxyHost)) {
            cssQueryTag = "tbody";
            ipColIndex = 0;
            portColIndex = 1;
        } else {
            cssQueryTag = "table";
            ipColIndex = 1;
            portColIndex = 2;
        }

        for (int index = 1; index <= PROXY_LIST_PAGE_NUM; index++) {
            Document doc;
            try {
                doc = Jsoup.connect(getProxyHost + index).get();
            } catch (IOException e) {
                e.printStackTrace();
                mLogSb.append("page error : ").append(getProxyHost + index).append("\n");
                mHandler.sendEmptyMessage(200);
                continue;

            }
            if (doc.select(cssQueryTag).size() == 0) {
                mLogSb.append("page error : ").append(getProxyHost + index).append("\n");
                mHandler.sendEmptyMessage(200);
                continue;
            }

            Element tbody = doc.select(cssQueryTag).get(0);
            Elements rows = tbody.select("tr");

            for (int i = 1; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td");
                String ip = cols.get(ipColIndex).text();
                int port = Integer.parseInt(cols.get(portColIndex).text());
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
