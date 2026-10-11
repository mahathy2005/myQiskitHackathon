package com.sheshield.app;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class YouTubeVideoAdapter extends RecyclerView.Adapter<YouTubeVideoAdapter.VideoViewHolder> {

    private final List<YouTubeVideo> videoList;

    public YouTubeVideoAdapter(List<YouTubeVideo> videoList, Lifecycle lifecycle) {
        this.videoList = videoList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_youtube_card, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        YouTubeVideo video = videoList.get(position);

        if (holder.tvCategory != null) holder.tvCategory.setText(video.getCategory());
        if (holder.tvTitle != null) holder.tvTitle.setText(video.getTitle());
        if (holder.tvDescription != null) holder.tvDescription.setText(video.getDescription());

        holder.loadVideo(video.getVideoId());
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvTitle, tvDescription;
        WebView webViewVideo;

        @SuppressLint("SetJavaScriptEnabled")
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            webViewVideo = itemView.findViewById(R.id.webViewVideo);

            if (webViewVideo != null) {
                WebSettings settings = webViewVideo.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setDatabaseEnabled(true);
                settings.setMediaPlaybackRequiresUserGesture(false);
                settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

                webViewVideo.setWebChromeClient(new WebChromeClient());
                webViewVideo.setWebViewClient(new WebViewClient());
            }
        }

        public void loadVideo(String videoId) {
            if (webViewVideo == null || videoId == null) return;

            // Using youtube-nocookie.com domain bypasses YouTube Error 152-4 in WebViews
            String html = "<!DOCTYPE html><html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
                    "<style>" +
                    "  * { margin:0; padding:0; box-sizing:border-box; }" +
                    "  body, html { width:100%; height:100%; background-color:#000000; overflow:hidden; }" +
                    "  iframe { width:100%; height:100%; border:none; }" +
                    "</style>" +
                    "</head><body>" +
                    "<iframe src='https://www.youtube-nocookie.com/embed/" + videoId + "?autoplay=0&rel=0&playsinline=1' " +
                    "allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture' " +
                    "allowfullscreen></iframe>" +
                    "</body></html>";

            webViewVideo.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null);
        }
    }
}