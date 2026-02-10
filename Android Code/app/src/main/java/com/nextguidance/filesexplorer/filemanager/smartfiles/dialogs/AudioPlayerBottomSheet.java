package com.nextguidance.filesexplorer.filemanager.smartfiles.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.AudioInfo;

public class AudioPlayerBottomSheet extends BottomSheetDialogFragment {

    private AudioInfo audioInfo;
    private ExoPlayer player;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime, tvFilename;
    private ImageView btnPlayPause;
    private Handler handler = new Handler();
    private Runnable updateProgressAction;

    public AudioPlayerBottomSheet(AudioInfo audioInfo) {
        this.audioInfo = audioInfo;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_audio_player, container, false);

        tvFilename = view.findViewById(R.id.player_filename);
        seekBar = view.findViewById(R.id.player_seekbar);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvTotalTime = view.findViewById(R.id.tv_total_time);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        
        tvFilename.setText(audioInfo.displayName);
        
        view.findViewById(R.id.btn_close_player).setOnClickListener(v -> dismiss());

        setupPlayer();
        setupControls();

        return view;
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(getContext()).build();
        MediaItem mediaItem = MediaItem.fromUri(audioInfo.derivedUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);

        updateProgressAction = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    long currentPos = player.getCurrentPosition();
                    long duration = player.getDuration();
                    seekBar.setProgress((int) ((currentPos * 100) / (duration > 0 ? duration : 1)));
                    tvCurrentTime.setText(formatTime(currentPos));
                    tvTotalTime.setText(formatTime(duration));
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateProgressAction);
    }

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            } else {
                player.play();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long newPos = (player.getDuration() * progress) / 100;
                    player.seekTo(newPos);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private String formatTime(long millis) {
        if (millis < 0) return "00:00";
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        handler.removeCallbacks(updateProgressAction);
    }
}
