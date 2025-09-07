package com.example.tcc.view.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tcc.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DashboardFragment extends Fragment {

    // ATUALIZE SEMPRE para a URL PÚBLICA ATUAL do Metabase (ngrok muda!):
    private static final String BASE_PUBLIC_URL =
            "https://545a5301f367.ngrok-free.app/public/dashboard/36a81a27-2960-48eb-bb49-b4f981ca27ed";

    private String buildingId;

    public DashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
        }

        WebView webView = view.findViewById(R.id.webViewDashboard);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        String url = BASE_PUBLIC_URL;
        if (!TextUtils.isEmpty(buildingId)) {
            try {
                String enc = URLEncoder.encode(buildingId, "UTF-8");
                url = BASE_PUBLIC_URL + "?building=" + buildingId;
            } catch (UnsupportedEncodingException ignored) {}
        } else {
        }

        webView.loadUrl(url);
        return view;
    }
}
