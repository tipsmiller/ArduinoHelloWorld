package xyz.gmiller.arduinohelloworld;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements Arduino.ArduinoListener {
    EditText serialText;
    Button beginButton, clearButton, sendButton;
    ScrollView scrollView;
    TextView textView;
    Arduino arduino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            this.finish();
            return;
        }

        setContentView(R.layout.activity_main);

        serialText = findViewById(R.id.serialText);
        scrollView = findViewById(R.id.scrollView);
        beginButton = findViewById(R.id.beginButton);
        clearButton = findViewById(R.id.clearButton);
        sendButton = findViewById(R.id.sendButton);
        textView = findViewById(R.id.textView);

        arduino = new Arduino(this, (UsbManager) getSystemService(Context.USB_SERVICE), this);
        arduino.tryConnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arduino.destroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onClickBegin(View view) {
        arduino.tryConnect();
    }

    public void onClickSend(View view) {
        if (arduino != null) {
            arduino.sendMessage(serialText.getText().toString());
            serialText.setText("");
        }
    }

    public void onClickClear(View view) {
        textView.setText("");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onMessageReceived(String message) {
        tvAppend(textView, message);
    }
}
