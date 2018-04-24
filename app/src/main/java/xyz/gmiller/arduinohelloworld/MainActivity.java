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

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION = "xyz.gmiller.arduinohelloworld.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbDeviceConnection connection;
    UsbSerialDevice serialPort;
    EditText serialText;
    Button beginButton, clearButton, sendButton;
    ScrollView scrollView;
    TextView textView;
    PendingIntent permissionIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            this.finish();
            return;
        }

        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        serialText = findViewById(R.id.serialText);
        scrollView = findViewById(R.id.scrollView);
        beginButton = findViewById(R.id.beginButton);
        clearButton = findViewById(R.id.clearButton);
        sendButton = findViewById(R.id.sendButton);
        textView = findViewById(R.id.textView);

        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        requestPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.disconnect();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch(IllegalArgumentException ex) {
            //noop
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onClickBegin(View view) {
        requestPermission();
    }

    private synchronized void requestPermission() {
        tvAppend(textView, "Looking for devices\n");
        HashMap<String,UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            if (usbDevices.entrySet().size() == 1) {
                device = usbDevices.values().iterator().next();
                tvAppend(textView, String.format("Device found: VID %s, PID %s\n", device.getVendorId(), device.getProductId()));
                if (usbManager.hasPermission(device)) {
                    setupArduino();
                } else {
                    usbManager.requestPermission(device, permissionIntent);
                }
            }
        }
    }

    public void onClickSend(View view) {
        if (serialPort != null) {
            serialPort.write(serialText.getText().toString().getBytes());
            serialText.setText("");
        }
    }

    public void onClickClear(View view) {
        textView.setText("");
    }

    private void disconnect() {
        if (serialPort != null) {
            tvAppend(textView, "Serial port disconnected\n");
            serialPort.close();
            serialPort = null;
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                setupArduino();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                tvAppend(textView, "USB device attached\n");
                if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    requestPermission();
                } else {
                    setupArduino();
                }
            } else if (intent.getAction().equals((UsbManager.ACTION_USB_DEVICE_DETACHED))) {
                tvAppend(textView, "USB device detached\n");
                disconnect();
            }
        }
    };

    public final UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            String data = null;
            try {
                data = new String(bytes, "UTF-8");
                tvAppend(textView, data);
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        }
    };

    private synchronized void setupArduino() {
        connection = usbManager.openDevice(device);
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort != null) {
            if (serialPort.open()) {
                serialPort.setBaudRate(9600);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serialPort.read(mCallback);
                tvAppend(textView, "Success: Serial connection opened!\n");
            } else {
                tvAppend(textView, "Error: Serial connection not opened\n");
            }
        } else {
            tvAppend(textView, "Error: Serial port is null\n");
        }
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
}
