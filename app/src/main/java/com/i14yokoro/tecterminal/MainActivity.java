package com.i14yokoro.tecterminal;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.nio.charset.StandardCharsets;

/**
 * TODO 画面を回転させた時のデータ保持(onCreateがもう一回発動するから全部消える?)
 * TODO ４０になるまで空白をいれる形式だと色々まずいので，移動してgetSelect%maxChar<nだったら空白をその分いれるようにする
 * TODO 接続中，打ったもじはRNにおくるだけでandroid上には表示しない．
 * TODO ctlキー（押したらふらぐたて）
 */
public class MainActivity extends AppCompatActivity{

    private static final String TAG = "debug***";

    private final String LF = System.getProperty("line.separator"); //システムの改行コードを検出
    private EditText inputEditText; //ディスプレイのEdittext
    private String inputStrText; //入力したテキスト

    private String before_str = ""; //１つ前の行までのテキスト情報

    private static final String PREFS = "PREFS";
    private static final String PREFS_NAME = "NAME";
    private static final String PREFS_ADDRESS = "ADDR";
    private static final String PREFS_AUTO_CONNECT = "AUTO";

    private static final int REQ_CODE_SCAN_ACTIVITY = 1;
    private static final int REQ_CODE_ENABLE_BT = 2;

    private static final long CONNECT_TIME = 5000; //スキャンする時間
    private Handler connectTimeoutHandler;
    private MldpBluetoothService bleService;

    private String bleDeviceName, bleDeviceAddress;

    private boolean bleAutoConnect;

    private SharedPreferences prefs;

    private enum State {STARTING, ENABLING, SCANNING, CONNECTING, CONNECTED, DISCONNECTED, DISCONNECTING} //state

    private State state = State.STARTING;

    private String escapeMoveNum = ""; //escapeシーケンスできたString型の数字を保存

    //private String comingText = ""; //RNからきたテキストを一行分保存する

    private Editable editable;
    private EscapeSequence escapeSequence;

    float r;
    private int currCursor = 0;

    private String lineText = "";
    private int maxRowLength;
    private int position;
    private int eStart;
    private int eCount;

    private boolean escPuttingFlag = false; //escキーがおされたらtrue
    private boolean escapeMoveFlag = false; //escFlagがtrueでエスケープシーケンスがおくられて来た時true
    private boolean editingFlag = true;
    private boolean deletePutFlag = true;
    private boolean enterPutFlag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputEditText = (EditText) findViewById(R.id.main_display);
        inputEditText.setCustomSelectionActionModeCallback(mActionModeCallback);

        inputEditText.addTextChangedListener(mInputTextWatcher);
        maxRowLength = getMaxRowLength();
        escapeSequence = new EscapeSequence(this, getMaxRowLength()); //今のContentを渡す
        state = State.STARTING;
        inputStrText = inputEditText.getText().toString();
        connectTimeoutHandler = new Handler();

        findViewById(R.id.btn_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\\x1b[1A");
                }
                else {
                    if ((inputEditText.getSelectionStart()/maxRowLength - 1) != inputEditText.getLineCount()-1){
                        return;
                    }
                    escapeSequence.moveUp();
                }
            }
        });

        findViewById(R.id.btn_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\\x1b[1B");
                }
                else {
                    if ((inputEditText.getSelectionStart()/maxRowLength + 1) != inputEditText.getLineCount()-1){
                        return;
                    }
                    escapeSequence.moveDown();
                }
            }
        });

        findViewById(R.id.btn_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\\x1b[1C");
                }
                else {
                    if ((inputEditText.getSelectionStart() + 1)/maxRowLength != inputEditText.getLineCount()-1){
                        return;
                    }
                    escapeSequence.moveRight();
                }
            }
        });

        findViewById(R.id.btn_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\\x1b[1D");
                }
                else {
                    if ((inputEditText.getSelectionStart() - 1)/maxRowLength != inputEditText.getLineCount()-1){
                        return;
                    }
                    escapeSequence.moveLeft();
                }
            }
        });

        findViewById(R.id.btn_esc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r = inputEditText.getWidth()/inputEditText.getTextSize();
                addNewLine(Float.toString(r));
            }
        });

        //SDK23以降はBLEをスキャンするのに位置情報が必要
        if(Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        //自動接続
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs != null) {
            bleAutoConnect = prefs.getBoolean(PREFS_AUTO_CONNECT, false);
            if (bleAutoConnect) {
                bleDeviceName = prefs.getString(PREFS_NAME, null);
                bleDeviceAddress = prefs.getString(PREFS_ADDRESS, null);
            }
        }

        //画面タッチされた時のイベント
        inputEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                    if (inputEditText.getSelectionStart() < before_str.length()) {
                        inputEditText.setSelection(currCursor);
                        showKeyboard();
                        return true;
                    }
                    else{
                        currCursor = inputEditText.getSelectionStart();
                        showKeyboard();
                    }

                return true;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(bleServiceReceiver, bleServiceIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bleServiceReceiver);
    }

    // デバイスデータを保存する
    @Override
    public void onStop() {
        super.onStop();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        if(bleAutoConnect) {
            editor.putString(PREFS_NAME, bleDeviceName);
            editor.putString(PREFS_ADDRESS, bleDeviceAddress);
        }
        editor.commit();
    }     

    @Override
    protected void onDestroy() {
        super.onDestroy();
        addNewLine("onDestroy");
        unbindService(bleServiceConnection);
        bleService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_terminal_menu, menu);
        if (state == State.CONNECTED) {
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_connect).setVisible(false);
        } else {
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            if (bleDeviceAddress != null) {
                menu.findItem(R.id.menu_connect).setVisible(true);
            }
            else {
                menu.findItem(R.id.menu_connect).setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                startScan();
                return true;

            case R.id.menu_connect:
                if(bleDeviceAddress != null) {
                    connectWithAddress(bleDeviceAddress);
                }
                return true;

            case R.id.menu_disconnect:
                state = State.DISCONNECTING;
                updateConnectionState();
                bleService.disconnect();
                unbindService(bleServiceConnection);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.removeItem(android.R.id.paste);
            menu.removeItem(android.R.id.cut);
            menu.removeItem(android.R.id.copy);
            menu.removeItem(android.R.id.selectAll);
            menu.removeItem(android.R.id.addToDictionary);
            menu.removeItem(android.R.id.startSelectingText);
            menu.removeItem(android.R.id.selectTextMode);
            //menu.removeItem(android.R.id.replaceText);
            //menu.removeItem(android.R.id.autofill);
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.removeItem(android.R.id.paste);
            menu.removeItem(android.R.id.cut);
            menu.removeItem(android.R.id.copy);
            menu.removeItem(android.R.id.selectAll);
            menu.removeItem(android.R.id.addToDictionary);
            menu.removeItem(android.R.id.startSelectingText);
            menu.removeItem(android.R.id.selectTextMode);
            //menu.removeItem(android.R.id.replaceText);
            //menu.removeItem(android.R.id.autofill);
            menu.close();
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };

    private final TextWatcher mOutgoingTextWatcher = new TextWatcher() {

        public void beforeTextChanged(CharSequence cs, int i, int i1, int i2) {
        }
        public void onTextChanged(CharSequence cs, int start, int before, int count) {
            if(count > before) {
                if(editingFlag)
                    bleService.writeMLDP(cs.subSequence(start + before, start + count).toString());
                    //bleService.writeMLDP(cs.subSequence(start + before, start + count).toString().getBytes());
                //else editFlag = true;

            }
        }

        public void afterTextChanged(Editable edtbl) {
        }
    };


    private final TextWatcher mInputTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (editingFlag && deletePutFlag) {
                currCursor = inputEditText.getSelectionStart();
                inputStrText = s.toString(); //おされた瞬間のテキストを保持

                position = start;

                if (position == 0 && 0 < count && 0 < after) {
                    position = count;
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            eStart = start;
            eCount = count;
            if (before > 0 && inputEditText.getSelectionStart() > 0) { //削除される文字が存在するとき
                if (s.subSequence(inputEditText.getSelectionStart() - 1, inputEditText.getSelectionStart()).equals(LF)) { //その前の文字が改行コードまたは，編集可能じゃなかったら
                    Log.d(TAG, "deleteFlag is false");
                    deletePutFlag = false;//削除フラグをおる
                }
            }
        }

        //TODO ループにはいっているので直す
        //TODO 一番した以外で文字が追加したときに改行が入るのを直す

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length() < 1) return;
            String str = s.subSequence(s.length()-1, s.length()).toString();//入力文字

            Log.d(TAG, "afterTextChange");
            //TODO ２文字以上の判定ができない？
            if (str.matches("[\\p{ASCII}]*") && deletePutFlag ) {
                lineText += str;
                Log.d(TAG, "ASCII code/ " + str);
                if (str.equals(LF)) {
                    if (enterPutFlag) {
                        Log.d(TAG, "lineText is " + lineText);
                        enterPutFlag = false; //最後に改行をいれるのでループしないように
                        editingFlag = false;
                        inputEditText.setText(inputStrText);//エンター入力前にもどす
                        inputEditText.setSelection(currCursor);
                        lineText = lineText.substring(0, lineText.length() - 1);
                        addSpace();
                        inputEditText.append(LF);

                        before_str = inputStrText;
                        Log.d(TAG, "linetext length is " + Integer.toString(lineText.length()));
                        lineText = "";
                        //dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                    }
                    else {
                        enterPutFlag = true;
                    }
                }

            }
            else {
                editingFlag = false;
                inputEditText.setText(inputStrText);
                inputEditText.setSelection(position);
                lineText = lineText.substring(0, lineText.length()-1);
                editingFlag = true;
                deletePutFlag = true;
            }
        }
    };

    //通すActionを記述
    private static IntentFilter bleServiceIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MldpBluetoothService.ACTION_BLE_REQ_ENABLE_BT);
        intentFilter.addAction(MldpBluetoothService.ACTION_BLE_CONNECTED);
        intentFilter.addAction(MldpBluetoothService.ACTION_BLE_DISCONNECTED);
        intentFilter.addAction(MldpBluetoothService.ACTION_BLE_DATA_RECEIVED);
        return intentFilter;
    }

    private final BroadcastReceiver bleServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MldpBluetoothService.ACTION_BLE_CONNECTED.equals(action)) {
                connectTimeoutHandler.removeCallbacks(abortConnection);
                Log.d(TAG, "Received intent  ACTION_BLE_CONNECTED");
                state = State.CONNECTED;
                updateConnectionState();
            }
            else if (MldpBluetoothService.ACTION_BLE_DISCONNECTED.equals(action)) {
                Log.d(TAG, "Received intent ACTION_BLE_DISCONNECTED");

                state = State.DISCONNECTED;
                updateConnectionState();
            }
            else if (MldpBluetoothService.ACTION_BLE_DATA_RECEIVED.equals(action)) {
                Log.d(TAG, "Received intent ACTION_BLE_DATA_RECEIVED");
                String data = intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_DATA);

                if (data != null) {
                    String str="";
                    byte[] utf = data.getBytes(StandardCharsets.UTF_8);
                    for (byte b : utf){
                        str = Integer.toHexString(b & 0xff);
                    }

                    int startSel = inputEditText.getSelectionStart();
                    int endSel = inputEditText.getSelectionEnd();
                    switch (str) {
                        case KeyHexString.KEY_RIGHT:
                            escapeSequence.moveRight(1);
                            escPuttingFlag = false;
                            escapeMoveFlag = false;
                            break;
                        case KeyHexString.KEY_LEFT:
                            escapeSequence.moveLeft(1);
                            escPuttingFlag = false;
                            escapeMoveFlag = false;
                            break;
                        case KeyHexString.KEY_DEL:
                            if(startSel >= 1) {
                                inputEditText.getText().delete(startSel - 1, endSel);
                            }
                            escPuttingFlag = false;
                            escapeMoveFlag = false;
                            break;
                        case KeyHexString.KEY_ENTER:
                            editingFlag = false;
                            //input.append(LF);
                            escPuttingFlag = true;
                            escPuttingFlag = false;
                            escapeMoveFlag = false;
                            break;
                        case KeyHexString.KEY_ESC:
                            escPuttingFlag = true;
                            escapeMoveFlag = false;
                            break;
                        case "7a":
                            if(escPuttingFlag){
                                inputEditText.append("esc + Z");
                                escPuttingFlag = false;
                                break;
                            }
                        default:
                            //2桁の入力を可能にする　できた
                            if (escPuttingFlag && data.matches("[0-9]")) {
                                Log.d(TAG, "move flag is true");
                                escapeMoveNum += data;
                                escapeMoveFlag = true;
                                break;
                            }
                            if(escPuttingFlag && data.matches("[A-H]")){
                                //escapeシーケンス用
                                int move;
                                if(escapeMoveFlag) {
                                    move = Integer.parseInt(escapeMoveNum);
                                }else {
                                    move = 1;
                                }
                                escapeMoveNum = "";

                                Log.d(TAG, "moveFlag true && A-H");
                                if(str.equals(KeyHexString.KEY_A)){
                                    escapeSequence.moveUp(move);
                                }
                                if(str.equals(KeyHexString.KEY_B)){
                                    escapeSequence.moveDown(move);
                                }
                                if(str.equals(KeyHexString.KEY_C)){
                                    escapeSequence.moveRight(move);
                                }
                                if(str.equals(KeyHexString.KEY_D)){
                                    escapeSequence.moveLeft(move);
                                }
                                if(str.equals(KeyHexString.KEY_E)){
                                    escapeSequence.moveRowDown(move);
                                }
                                if(str.equals(KeyHexString.KEY_F)){
                                    escapeSequence.moveRowUp(move);
                                }
                                if(str.equals(KeyHexString.KEY_G)){
                                    escapeSequence.moveSelection(move);
                                }
                                if(str.equals(KeyHexString.KEY_H)){
                                    //TODO ここの引数２個なんとかする
                                    escapeSequence.moveSelection(move, move);
                                }
                                escapeMoveFlag = false;
                                escPuttingFlag = false;
                                break;
                            }

                            editable = inputEditText.getText();
                            escPuttingFlag = false;
                            editable.replace(Math.min(startSel, endSel), Math.max(startSel, endSel), data);
                            //input.append(str);
                            inputEditText.setSelection(inputEditText.getText().length());
                            escPuttingFlag = true;
                            escPuttingFlag = false;
                            escapeMoveFlag = false;

                            break;
                    }
                }
            }
        }
    };


    private boolean connectWithAddress(String address) {
        state = State.CONNECTING;
        updateConnectionState();                                                                    
        connectTimeoutHandler.postDelayed(abortConnection, CONNECT_TIME);
        return bleService.connect(address);                                                         
    }

    private Runnable abortConnection = new Runnable() {
        @Override
        public void run() {
            if (state == State.CONNECTING) {
                bleService.disconnect();                      							            
            }
        }
    };

    private void startScan() {

        if(bleService != null) {
            bleService.disconnect();
            state = State.DISCONNECTING;
            updateConnectionState();
        }

        //else {
        Intent bleServiceIntent = new Intent(MainActivity.this, MldpBluetoothService.class);
        this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);
        //}

        final Intent bleScanActivityIntent = new Intent(MainActivity.this, MldpBluetoothScanActivity.class);
        startActivityForResult(bleScanActivityIntent, REQ_CODE_SCAN_ACTIVITY);
    }

    private void updateConnectionState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case STARTING:
                    case ENABLING:
                    case SCANNING:
                    case DISCONNECTED:
                        setProgressBarIndeterminateVisibility(false);
                        break;
                    case CONNECTING:
                        setProgressBarIndeterminateVisibility(true);
                        inputEditText.removeTextChangedListener(mOutgoingTextWatcher);
                        inputEditText.removeTextChangedListener(mInputTextWatcher);
                        inputEditText.addTextChangedListener(mOutgoingTextWatcher);
                        break;
                    case CONNECTED:
                        addNewLine("connected to " + bleDeviceName);
                        setProgressBarIndeterminateVisibility(false);
                        break;
                    case DISCONNECTING:
                        setProgressBarIndeterminateVisibility(false);
                        addNewLine("disconnected from " + bleDeviceName);
                        inputEditText.removeTextChangedListener(mOutgoingTextWatcher);
                        inputEditText.addTextChangedListener(mInputTextWatcher);
                        break;
                    default:
                        state = State.STARTING;
                        setProgressBarIndeterminateVisibility(false);
                        break;
                }

                invalidateOptionsMenu();
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQ_CODE_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if(!bleAutoConnect || bleDeviceAddress == null) {
                    startScan();
                }
            }
            return;
        }
        else if(requestCode == REQ_CODE_SCAN_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                bleDeviceAddress = intent.getStringExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_ADDRESS);
                bleDeviceName = intent.getStringExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_NAME);
                bleAutoConnect = intent.getBooleanExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_AUTO_CONNECT, false);
                if(bleDeviceAddress == null) {
                    state = State.DISCONNECTED;
                    updateConnectionState();
                }
                else {
                    state = State.CONNECTING;
                    updateConnectionState();
                    connectWithAddress(bleDeviceAddress);
                }
            }
            else {
                state = State.DISCONNECTED;
                updateConnectionState();
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private final ServiceConnection bleServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MldpBluetoothService.LocalBinder binder = (MldpBluetoothService.LocalBinder) service;
            bleService = binder.getService();
            Log.d(TAG, "bleService");
            if (!bleService.isBluetoothRadioEnabled()) {
                state = State.ENABLING;
                updateConnectionState();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT);
                Log.d(TAG, "Requesting user to enable Bluetooth radio");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    private void addNewLine(String newStr) {
        inputEditText.append(newStr);
        inputEditText.append(LF);
        before_str = inputStrText;
        inputEditText.setSelection(inputEditText.getText().length());
    }

    private int getMaxRowLength(){
        //int weight = input.getWidth();
        WindowManager wm =  (WindowManager)getSystemService(WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();

        int dispWidth = disp.getWidth();
        Log.d(TAG, "display weight is " + Integer.toString(dispWidth));

        // 文字サイズが30pxで，TypefaceがMonospace 「" "」の幅を取得
        Paint paint = new Paint();
        paint.setTextSize(30);
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        int textWidth = (int)paint.measureText(" ");

        Log.d(TAG, "text size is " + Integer.toString(textWidth));

        return dispWidth/textWidth;
    }

    //端っこのいくまでスペース追加
    private void addSpace() {
        editingFlag = false;

        StringBuilder space = new StringBuilder();
        Log.d(TAG, "add Space");
        for (int i = lineText.length() % maxRowLength; i <= maxRowLength; i++) {
            space.append(" ");
        }
        inputEditText.append(space.toString());
        lineText += space;
        editingFlag = true;
    }

    private String getLineString(){
        return null;
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null)
            imm.showSoftInput(v, 0);
    }
}
