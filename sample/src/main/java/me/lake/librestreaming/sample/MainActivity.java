package me.lake.librestreaming.sample;

import static java.util.Arrays.stream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_STREAM = 1;

    RadioGroup rg_direction;
    RadioGroup rg_mode;
    EditText et_url;
    Button startView;
    boolean authorized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_url = (EditText) findViewById(R.id.et_url);
        rg_direction = (RadioGroup) findViewById(R.id.rg_direction);
        rg_mode = (RadioGroup) findViewById(R.id.rg_mode);
        startView = findViewById(R.id.btn_start);
        startView.setOnClickListener(v -> {
            if (authorized) {
                start();
            } else {
                Snackbar.make(MainActivity.this.getWindow().getDecorView().getRootView(), "streaming need permissions!", Snackbar.LENGTH_LONG)
                        .setAction("auth", v1 -> verifyPermissions()).show();
            }
        });

        verifyPermissions();

        rg_direction.check(R.id.rb_port);
        rg_mode.check(R.id.rb_hard);
        et_url.setText("rtmp://wansu-global-push-rtmp-latency-stag.17app.co/vod/cb439984-1901-44e6-bfa9-1279b5f50831?id=cb439984-1901-44e6-bfa9-1279b5f50831&wsPRI=1&wsSecret=e463de5633b3e48ec3c6e91f45c521a5&wsTime=1757470786");

        startView.postDelayed(() -> {
            if (authorized) {
                startView.performClick();
            }
        }, 5_000L);
    }

    private void start() {
        Intent intent;
        boolean isport = false;
        if (rg_direction.getCheckedRadioButtonId() == R.id.rb_port) {
            isport = true;
        }
        if (rg_mode.getCheckedRadioButtonId() == R.id.rb_hard) {
            intent = new Intent(MainActivity.this, HardStreamingActivity.class);
        } else {
            intent = new Intent(MainActivity.this, SoftStreamingActivity.class);
        }
        intent.putExtra(BaseStreamingActivity.DIRECTION, isport);
        intent.putExtra(BaseStreamingActivity.RTMPADDR, et_url.getText().toString());
        startActivity(intent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STREAM) {
            authorized = stream(grantResults).allMatch(result -> result == PackageManager.PERMISSION_GRANTED);
        }
    }


    public void verifyPermissions() {
        int cameraPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
        int recordAudioPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO);
        int writeExternalStoragePermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (cameraPermission != PackageManager.PERMISSION_GRANTED ||
                recordAudioPermission != PackageManager.PERMISSION_GRANTED ||
                writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    getRequestPermissions(),
                    REQUEST_STREAM
            );
            authorized = false;
        } else {
            authorized = true;
        }
    }

    private String[] getRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        } else {
            return new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }
}