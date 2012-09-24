
package com.sungeo.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity {
    private ListView mListView;
    private Button openBtn, closeBtn;
    private ArrayAdapter<String> mDevicesArrayAdapter;
    private BluetoothAdapter mBtAdapter;
    protected MsgHandler mMsgHandler;
    private Toast mToast;
    private static String mAddress = null;
    public final static int START_FIND_BT = 0;
    public final static int CONNECT_BT = 1;
    public final static int CONNECT_SUCESS = 2;
    public final static int MSG_STR = 3;
    public final static int CONNECT_FAIL = 4;
    public final static int CONNECT_LOST = 5;
    public final static int SEND_SUCESS = 6;
    public final static int SEND_FAIL = 7;
    
    private final int RECONNECT_COUNT = 10;
    private int mReConnectCounter = 0;

    private BluetoothService mBtService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.listView1);
        mDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mListView.setAdapter(mDevicesArrayAdapter);
        openBtn = (Button) findViewById(R.id.button1);
        openBtn.setOnClickListener(mOnClickListener);
        closeBtn = (Button) findViewById(R.id.button2);
        closeBtn.setOnClickListener(mOnClickListener);
        mListView.setOnItemClickListener(mOnItemClickListener);

        registerReceiver();

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        initMsgHandler();
        initMacAddress();
        mBtService = new BluetoothService(this, mMsgHandler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = true;

        menu.add(0, 0, 0, "重新连接");
        menu.add(1, 1, 1, "忘记");

        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        int itemId = item.getItemId();

        if (itemId == 0) {
            deconnectBt();
            connect();
        } else if (itemId == 1) {
            clearMacAddress();
        }
        return ret;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBtService != null && mBtService.getState() == BluetoothService.STATE_CONNECTED) {
            return;
        }
        Message msg = mMsgHandler.obtainMessage();
        msg.what = START_FIND_BT;
        mMsgHandler.sendMessage(msg);
    }

    @Override
    public void onPause() {
        super.onPause();
        deconnectBt();
    }
    
    @Override
    public void onDestroy() {
        cancelToast();
        super.onDestroy();
    }

    private void openBt() {
        if (!mBtAdapter.isEnabled()) {
            // 弹出对话框提示用户是后打开
            // Intent enabler = new
            // Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // startActivityForResult(enabler, REQUEST_ENABLE);
            // 不做提示，强行打开
            mBtAdapter.enable();
        } else {
            showToastStr("蓝牙已处于开启状态！");
            Message msg = mMsgHandler.obtainMessage();
            msg.what = MainActivity.CONNECT_BT;
            mMsgHandler.sendMessage(msg);
        }
    }

    private void connect() {
        if (mBtService != null) {
            mBtService.start();
        }
        if (mAddress == null) {
            boolean flag = mBtAdapter.startDiscovery();
            if (flag) {
                showToastStr("开始搜索设备……");
            } else {
                showToastStr("开始搜索失败");
            }   
        } else {
            BluetoothDevice device = null;
            if (BluetoothAdapter.checkBluetoothAddress(mAddress)) {
                device = mBtAdapter.getRemoteDevice(mAddress);
            } else {
                return;
            }

            if (device == null) {
                return;
            }
            mBtService.connect(device, true);
        }
    }

    private void deconnectBt() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }

        mReceiver = null;
        if (mBtService != null) {
            mBtService.stop();
        }
        mBtAdapter.disable();
    }

    private void sendCodeByBt(String code) {

        byte[] cmd = code.getBytes();

        mBtService.write(cmd);
    }

    private int getReConnectCounter() {
        return mReConnectCounter;
    }
    
    private void clearReConnectCounter() {
        mReConnectCounter = 0;
    }
    
    private void setReConnectCounter() {
        mReConnectCounter ++;
    }
    
    private int getReConnectNum() {
        return RECONNECT_COUNT;
    }
    
    private void initMsgHandler() {
        mMsgHandler = new MsgHandler(this);
    }

    private void initMacAddress() {
        SharedPreferences sharedata = getSharedPreferences("bt_mac_file", Activity.MODE_PRIVATE);

        mAddress = sharedata.getString("bt_mac", null);
    }

    private void saveMacAddress() {
        SharedPreferences myShare = getSharedPreferences("bt_mac_file", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = myShare.edit();

        editor.putString("bt_mac", mAddress);
        editor.commit();
    }

    private void clearMacAddress() {
        SharedPreferences myShare = getSharedPreferences("bt_mac_file", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = myShare.edit();
        editor.clear();
        editor.commit();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    public void showBtn() {
        mListView.setVisibility(View.GONE);
        if (mAddress.equals("00:15:FF:F2:4C:2E")) {
             openBtn.setVisibility(View.VISIBLE);
             closeBtn.setVisibility(View.VISIBLE);
        } else {
            openBtn.setVisibility(View.VISIBLE);
            openBtn.setText("LED闪一下");
        }
    }
    
    public void stopService() {
        if (mBtService != null) {
            mBtService.stop();
        }
    }
    
    private void cancelToast() {
        if (mToast == null)
            return;
        mToast.cancel();
        mToast = null;
    }
    
    public void showToastStr(String str) {
        if (str == null) {
            return;
        }
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(str);
        strBuf.append(" ");
       
        if (mToast == null) {
            mToast = Toast.makeText(MainActivity.this, strBuf.toString(), Toast.LENGTH_SHORT);
            
        } else {
            //mToast.cancel();
            mToast = null;
            mToast = Toast.makeText(MainActivity.this, strBuf.toString(), Toast.LENGTH_SHORT);
        }

        mToast.show();
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Button btn = (Button) v;
            if (btn == openBtn) {
                if (mAddress.equals("00:15:FF:F2:4C:2E")) {
                    sendCodeByBt("112233");
                } else {
                    sendCodeByBt("r");
                }
                
            } else if (btn == closeBtn) {
                sendCodeByBt("112244");
            }
        }
    };

    OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mBtAdapter.cancelDiscovery();
            int len = 0;
            if (mDevicesArrayAdapter == null)
                return;
            len = mDevicesArrayAdapter.getCount();
            if (position < 0 || position >= len)
                return;

            String tempStr = mDevicesArrayAdapter.getItem(position);
            String[] tmp = tempStr.split("\n");
            int length = 0;
            if (tmp == null) {
                return;
            }
            length = tmp.length;
            if (length != 2) {
                return;
            }
            mAddress = tmp[1];
            saveMacAddress();
            connect();
        }
    };

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            // 找到设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device == null)
                    return;
                
                Log.v("sungeobt", "find device:" + device.getName()
                            + device.getAddress());
                mDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                showToastStr("发现蓝牙设备");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {// 搜索完成
                setTitle("搜索完成");
                showToastStr("搜索完成");
                if (mDevicesArrayAdapter.getCount() == 0) {
                    Log.v("sungeobt", "find over");
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                showToastStr("绑定状态改变");
            } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
                if (mode != BluetoothAdapter.SCAN_MODE_NONE) {
                    
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_ON) {
                    showToastStr("蓝牙打开成功！");
                    connect();
                }
            }
        }
    };

    static class MsgHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        MsgHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity theActivity = mActivity.get();
            switch (msg.what) {
                case MSG_STR:
                    String str = (String) msg.obj;
                    theActivity.showToastStr(str);
                    break;
                case START_FIND_BT:
                    theActivity.openBt();
                    break;
                case CONNECT_BT:
                    theActivity.connect();
                    break;
                case CONNECT_SUCESS:
                    theActivity.showToastStr("连接成功！");
                    theActivity.showBtn();
                    break;
                case CONNECT_FAIL:
                    if (theActivity.getReConnectCounter() >= theActivity.getReConnectNum()) {
                        theActivity.showToastStr("连接失败，请退出程序重新连接");
                        theActivity.clearReConnectCounter();
                        return;
                    } else {
                        theActivity.setReConnectCounter();
                    }
                    theActivity.showToastStr("连接失败，重新连接！");
                    theActivity.stopService();
                    theActivity.connect();
                    break;
                case SEND_SUCESS:
                    theActivity.showToastStr("发送成功！");
                    break;
                case SEND_FAIL:
                    theActivity.showToastStr("发送失败！");
                    break;
                case CONNECT_LOST:
                    theActivity.showToastStr("连接丟失！");
                    break;
                default:
                    break;
            }
        };
    }
}
