package com.i14yokoro.tecterminal;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
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
import java.util.ArrayList;

/**
 * TODO 接続中，打ったもじはRNにおくるだけでandroid上には表示しない．
 * TODO ctlキー（押したらふらぐたて）
 * TODO 入力文字が横幅を超えた場合は自動でリストに追加するようにする
 * TODO スクロールしたとき一番下の行が空白でカーソルが残るのを直す
 * TODO 英文字が行をまたぐときに自動で改行するのをやめさせる
 */
public class MainActivity extends AppCompatActivity{

    private static final String TAG = "debug***";

    private final String LF = System.getProperty("line.separator"); //システムの改行コードを検出
    private EditText inputEditText; //ディスプレイのEdittext
    private String inputStrText; //入力したテキスト

    private static final String PREFS = "PREFS";
    private static final String PREFS_NAME = "NAME";
    private static final String PREFS_ADDRESS = "ADDR";
    private static final String PREFS_AUTO_CONNECT = "AUTO";

    private static final int REQ_CODE_SCAN_ACTIVITY = 1;
    private static final int REQ_CODE_ENABLE_BT = 2;

    private static final long CONNECT_TIME = 5000; //スキャンする時間
    private Handler connectTimeoutHandler;
    private MldpBluetoothService bleService;
    private RowItem rowItem;
    private ArrayList<RowItem> items;

    private String bleDeviceName, bleDeviceAddress;

    private boolean bleAutoConnect;

    private SharedPreferences prefs;

    private enum State {STARTING, ENABLING, SCANNING, CONNECTING, CONNECTED, DISCONNECTED, DISCONNECTING} //state

    private State state = State.STARTING;

    private String escapeMoveNum = ""; //escapeシーケンスできたString型の数字を保存

    private Editable editable;
    private EscapeSequence escapeSequence;

    float r;
    private int currCursor = 0;

    private String lineText = "";
    private String RNtext = "";
    private int maxRowLength;
    private int maxColumnLength;
    private int position;
    private int eStart;
    private int eCount;

    private boolean btn_ctl = false;


    private boolean escPuttingFlag = false; //escキーがおされたらtrue
    private boolean squarePuttingFlag = false;
    private boolean escapeMoveFlag = false; //escFlagがtrueでエスケープシーケンスがおくられて来た時true
    private boolean editingFlag = true;
    private boolean enterPutFlag = true;
    private boolean isBtn_ctl = false;
    private boolean isBtn_esc = false;

    private String[][] display;

    private int topRow = 0;
    private boolean receivingFlag = true; //RN側に送りたくないものがあるときはfalseにする

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputEditText = (EditText) findViewById(R.id.main_display);
        inputEditText.setCustomSelectionActionModeCallback(mActionModeCallback);

        inputEditText.addTextChangedListener(mInputTextWatcher);

        maxRowLength = getMaxRowLength();
        maxColumnLength = getMaxColumnLength();

        items = new ArrayList<>();
        rowItem = new RowItem(items.size(), "", false, true);
        items.add(rowItem);

        inputEditText.setTransformationMethod(WordBreakTransformationMethod.getInstance());

        escapeSequence = new EscapeSequence(this, items, maxRowLength, maxColumnLength); //今のContentを渡す

        state = State.STARTING;
        inputStrText = inputEditText.getText().toString();
        connectTimeoutHandler = new Handler();

        //display = new String[maxColumnLength][maxRowLength];

        findViewById(R.id.btn_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\u001b" + "[1A");
                }
                else {
                    currCursor = inputEditText.getSelectionStart();
                    //if (items.get(getSelectRow()).isWritable()){
                        escapeSequence.moveUp();
                        //if(!items.get(getSelectRow()).isWritable()){
                        //    inputEditText.setSelection(currCursor);
                       // }
                   // }
                }
                /*
                if(topRow-1 >= 0){
                    topRow--;
                    changeDisplay();
                }*/
            }
        });

        findViewById(R.id.btn_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\u001b" + "[1B");
                }
                else {
                    currCursor = inputEditText.getSelectionStart();
                    if (items.get(getSelectRow()).isWritable()){
                        escapeSequence.moveDown();
                        if(!items.get(getSelectRow()).isWritable()){
                            inputEditText.setSelection(currCursor);
                        }
                    }
                }
                /*
                if(topRow + 1 <= items.size()){
                    topRow++;
                    changeDisplay();
                }*/
            }
        });

        findViewById(R.id.btn_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\u001b" + "[1C");
                }
                else {
                    currCursor = inputEditText.getSelectionStart();
                    if (items.get(getSelectRow()).isWritable()){
                        escapeSequence.moveRight();
                        if(!items.get(getSelectRow()).isWritable()){
                            inputEditText.setSelection(currCursor);
                        }
                    }
                }
            }
        });
        findViewById(R.id.btn_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\u001b" + "[1D");
                }
                else {
                    currCursor = inputEditText.getSelectionStart();
                    if (items.get(getSelectRow()).isWritable()){
                        escapeSequence.moveLeft();
                        if(!items.get(getSelectRow()).isWritable()){
                            inputEditText.setSelection(currCursor);
                        }
                    }
                }
            }
        });

        findViewById(R.id.btn_esc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test();
                isBtn_esc = true;
                Log.d(TAG, "max column: " + maxColumnLength);
            }
        });

        findViewById(R.id.btn_ctl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = inputEditText.getOffsetForPosition(0,0);
                inputEditText.setSelection(position);
                isBtn_ctl = true;
               // termDisplay.changeDisplay(getTopPositionRow());

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
            int oldY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // タップした時に ScrollViewのScrollY座標を保持
                        oldY = (int)event.getRawY();
                        Log.d(TAG, "old Y: " + oldY);
                        Log.d(TAG, "action down");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 指を動かした時に、現在のscrollY座標とoldYを比較して、違いがあるならスクロール状態とみなす
                        Log.d(TAG, "action move");
                        Log.d(TAG, "getScrollY" + v.getScrollY());
                        if (oldY > event.getRawY()) {
                            Log.v(TAG, "scrollView up");

                            if(topRow - 1 >= 0){
                                topRow--;
                                escapeSequence.setTop(topRow);
                                changeDisplay();
                            }
                        }
                        if (oldY < event.getRawY()){
                            Log.d(TAG, "scroll down");
                            if(topRow + 1 <= items.size()){
                                topRow++;
                                escapeSequence.setTop(topRow);
                                changeDisplay();
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:

                        break;
                    default:
                        break;
                }
                showKeyboard();
                float x = event.getX();
                float y = event.getY();
                int touchPosition = inputEditText.getOffsetForPosition(x, y);
                if(touchPosition > 0){
                    inputEditText.setSelection(touchPosition);
                }
                //Log.d(TAG, "selectRow : " + Long.toString(items.get(getSelectRow()).getId()) + "wtitable :" + items.get(getSelectRow()).isWritable() );
                    if (!items.get(getSelectRow()).isWritable()) {
                        //inputEditText.setSelection(currCursor);
                        return true;
                    }
                    else{
                        currCursor = inputEditText.getSelectionStart();
                        return false;
                    }
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
            //before_str = inputEditText.getText().toString();
            if(count > before) {
                if(receivingFlag)
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
            if (editingFlag && enterPutFlag) {
                if(items.get(getSelectRow()).isWritable()) {
                    currCursor = inputEditText.getSelectionStart();
                }
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
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length() < 1) return;
            String str = s.subSequence(eStart, eStart + eCount).toString();//入力文字

            Log.d(TAG, "afterTextChange");
            if (str.matches("[\\p{ASCII}]*") /*&& items.get(getSelectRow()).isWritable()*/ ) {
                if (receivingFlag)lineText += str;

                    if (enterPutFlag) {
                        Log.d(TAG, "ASCII code/ " + str);

                        if(lineText.length() >= getMaxRowLength()){
                            enterPutFlag = false;
                            receivingFlag = false;
                            addList(lineText);
                            inputEditText.append(LF);
                            enterPutFlag = true;
                            receivingFlag = true;
                            lineText = "";
                        }

                        if (str.equals(LF)) {
                            enterPutFlag = false;
                            //inputEditText.setText(inputStrText);
                            inputEditText.setSelection(inputEditText.length());
                            //inputEditText.append(LF);
                            enterPutFlag = true;

                            addList(lineText);

                            Log.d(TAG, "lineText is " + lineText);
                            Log.d(TAG, "linetext length is " + lineText.length());

                            lineText = "";
                            //dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                        }
                    }
            }
            else {
                if(editingFlag) {
                    editingFlag = false;
                    inputEditText.setText(inputStrText);
                    inputEditText.setSelection(position);
                    editingFlag = true;
                }
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
                        Log.d(TAG, "coming HEX : " + str);
                        bleService.writeMLDP(str + " ");
                    }

                    int startSel = inputEditText.getSelectionStart();
                    int endSel = inputEditText.getSelectionEnd();
                    switch (str) {
                        case KeyHexString.KEY_DEL:
                            if(startSel >= 1) {
                                inputEditText.getText().delete(startSel - 1, endSel);
                            }
                            escPuttingFlag = false;
                            escapeMoveFlag = false;
                            break;
                        case KeyHexString.KEY_ENTER:
                            //editingFlag = false;
                            //addList(RNtext);
                            inputEditText.append(LF);
                            changeDisplay();
                            RNtext = "";
                            escPuttingFlag = false;
                            squarePuttingFlag = false;
                            escapeMoveFlag = false;
                            break;
                        case KeyHexString.KEY_ESC:
                            Log.d(TAG, "receive esc");
                            escPuttingFlag = true;
                            squarePuttingFlag = false;
                            escapeMoveFlag = false;
                            break;
                        case KeyHexString.KEY_SQUARE_LEFT:
                            Log.d(TAG, "receive square");
                            if(escPuttingFlag) {
                                squarePuttingFlag = true;
                                break;
                            }
                            escPuttingFlag = false;
                            escapeMoveFlag = false;
                        default:
                            //2桁の入力を可能にする　できた
                            int h1 = 1;
                            boolean Hflag = false;
                            if (squarePuttingFlag && data.matches("[0-9]")) {
                                Log.d(TAG, "move flag is true");
                                escapeMoveNum += data;
                                escapeMoveFlag = true;
                                //squarePuttingFlag = false;
                                break;
                            }
                            if(squarePuttingFlag && data.equals(";")){
                                if(escapeMoveFlag) {
                                    h1 = Integer.parseInt(escapeMoveNum);
                                }else {
                                    h1 = 1;
                                }
                                escapeMoveNum = "";
                                Hflag = true;
                                escapeMoveFlag = false;
                                break;
                            }
                            if(Hflag && !data.matches("[Hf]")){
                                escapeMoveNum = "";
                                escapeMoveFlag = false;
                                squarePuttingFlag = false;
                                escPuttingFlag = false;
                                break;
                            }
                            if(squarePuttingFlag && data.matches("[A-HJKSTf]")){
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
                                if(str.equals(KeyHexString.KEY_H) || str.equals(KeyHexString.KEY_f)){
                                    if(!Hflag){
                                        h1 = 1;
                                        move = 1;
                                    }
                                    escapeSequence.moveSelection(h1, move);
                                    Hflag = false;
                                }
                                if(str.equals(KeyHexString.KEY_J)){

                                }
                                if (str.equals(KeyHexString.KEY_K)){

                                }
                                if (str.equals(KeyHexString.KEY_S)){
                                    receivingFlag = false;
                                    if (topRow + move <= items.size()) {
                                        escapeSequence.scrollNext(move);
                                        topRow = topRow + move;
                                        escapeSequence.setTop(topRow);
                                    }
                                    receivingFlag = true;
                                }
                                if (str.equals(KeyHexString.KEY_T)){
                                    receivingFlag = false;
                                    if(topRow - move >= 0) {
                                        escapeSequence.scrollBack(move);
                                        topRow = topRow - move;
                                        escapeSequence.setTop(topRow);
                                    }
                                    receivingFlag = true;
                                }
                                escapeMoveFlag = false;
                                squarePuttingFlag = false;
                                escPuttingFlag = false;
                                break;
                            }
                            RNtext = RNtext + data;

                            editable = inputEditText.getText();
                            escPuttingFlag = false;
                            squarePuttingFlag = false;
                            receivingFlag = false;
                            lineText += data;
                            editable.replace(Math.min(startSel, endSel), Math.max(startSel, endSel), data);
                            //input.append(str);
                            receivingFlag = true;
                            inputEditText.setSelection(inputEditText.getText().length());
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
                        //inputEditText.removeTextChangedListener(mInputTextWatcher);
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
                        //inputEditText.addTextChangedListener(mInputTextWatcher);
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
                    if(!connectWithAddress(bleDeviceAddress)){
                        addNewLine("connect is failed");
                    }
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

    private void addNewLine(String newText) {
        inputEditText.append(newText);
        inputEditText.append(LF);
        addList(newText + LF);
        inputEditText.setSelection(inputEditText.getText().length());
    }

    private void addList(String newText){
        //before_str = inputEditText.getText().toString();
        String text; //一行の文字列を格納
        String str = newText; //ちぎるやつ
        boolean hasNext; //次の行がある場合はtrue
        int rowNum = str.length()/maxRowLength; //入力に使われる行数
        if(str.length()%maxRowLength > 0){
            rowNum++;
        }
        for(int i = rowNum; i > 0; i--) {
            if (i > 1) {
                hasNext = true;
                text = str.substring(0, maxRowLength);
                str = str.substring(maxRowLength, str.length());
            } else { //あと１行
                hasNext = false;
                text = str;
            }
            rowItem = new RowItem(items.size()-1, text, hasNext, false);
            items.set(items.size()-1, rowItem);
            rowItem = new RowItem(items.size(), "", false, true);
            items.add(rowItem);
            if(items.size() > maxColumnLength && topRow+1 <= items.size() && inputEditText.getLineCount() > maxColumnLength){
                topRow++;
            }
            Log.d(TAG, "add list /" + text + " length /" + text.length());
        }
        escapeSequence.setTop(topRow);
    }

    private int getMaxRowLength(){
        //int weight = input.getWidth();
        WindowManager wm =  (WindowManager)getSystemService(WINDOW_SERVICE);
        Display disp = null;
        if (wm != null) {
            disp = wm.getDefaultDisplay();
        }

        int dispWidth = 0;
        if (disp != null) {
            dispWidth = disp.getWidth();
        }
        Log.d(TAG, "display width is " + Integer.toString(dispWidth));
        int textWidth = getTextWidth();
        return dispWidth/textWidth;
    }

    private int getMaxColumnLength(){
        WindowManager wm =  (WindowManager)getSystemService(WINDOW_SERVICE);
        Display disp = null;
        if (wm != null) {
            disp = wm.getDefaultDisplay();
        }
        int dispHeight = 0;
        if (disp != null) {
            dispHeight = disp.getHeight();
        }

        int height = dispHeight - 100;
        int text = (int)getTextHeight();
        Log.d(TAG, Float.toString(height/text));

        return (height/text)-1;
    }

    private int getTextWidth(){

        // TypefaceがMonospace 「" "」の幅を取得
        Paint paint = new Paint();
        paint.setTextSize(inputEditText.getTextSize());
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        int textWidth = (int)paint.measureText(" ");

        Log.d(TAG, "text width is " + Integer.toString(textWidth));
        return textWidth;
    }

    private float getTextHeight(){

        // TypefaceがMonospace 「" "」の幅を取得
        Paint paint = new Paint();
        paint.setTextSize(inputEditText.getTextSize());
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textHeight = Math.abs(fontMetrics.top) + Math.abs(fontMetrics.bottom);

        Log.d(TAG, "text Height is " + textHeight);
        return textHeight;
    }

    //選択中の行番号を返す

    private int getSelectRow() {
        int count = 0;
        int start = inputEditText.getSelectionStart() + 1;
        int row = 0;
        if (start == 0) {
            return 0;
        }
        for (; count < start; row++) {
            if (row + topRow < items.size()) {
                count += items.get(row+topRow).getText().length();
            } else break;
        }
        if(row == 0) return 0;
        return row - 1;
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null)
            if (imm != null) {
                imm.showSoftInput(v, 0);
            }
    }
    private void test(){
        for(int i = 0; i < 100; i++){
            addNewLine(Integer.toString(i));
        }
        changeDisplay();
    }
    private void changeDisplay(){
        receivingFlag = false;
        escapeSequence.changeDisplay();
        receivingFlag = true;
        //showDisplay();
    }
    private void showDisplay(){
        Log.d(TAG, "******showDisplay******");

        for(int i = 0; i < inputEditText.getLineCount(); i++){
            Log.d(TAG, (items.get(i + topRow).getText()));
        }
    }
}
