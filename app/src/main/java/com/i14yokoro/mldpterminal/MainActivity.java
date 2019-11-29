package com.i14yokoro.mldpterminal;

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
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.i14yokoro.mldpterminal.bluetooth.MldpBluetoothScanActivity;
import com.i14yokoro.mldpterminal.bluetooth.MldpBluetoothService;
import com.i14yokoro.mldpterminal.display.EscapeSequence;
import com.i14yokoro.mldpterminal.display.HtmlParser;
import com.i14yokoro.mldpterminal.display.TermDisplay;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "debug***";

    private final char LF = '\n';
    private EditText inputEditText; // ディスプレイのEditText

    // PREFS用
    private static final String PREFS = "PREFS";
    private static final String PREFS_NAME = "NAME";
    private static final String PREFS_ADDRESS = "ADDR";
    private static final String PREFS_AUTO_CONNECT = "AUTO";

    // BLE
    private static final int REQ_CODE_SCAN_ACTIVITY = 1;
    private static final int REQ_CODE_ENABLE_BT = 2;

    // 接続状況
    private enum State {STARTING, ENABLING, SCANNING, CONNECTING, CONNECTED, DISCONNECTED, DISCONNECTING} //state
    private State state = State.STARTING;

    private String bleDeviceName, bleDeviceAddress; // 接続先の情報

    private static final long CONNECT_TIME = 5000; //タイムアウトする時間
    private Handler connectTimeoutHandler;
    private MldpBluetoothService bleService;

    private boolean bleAutoConnect; //自動接続するか

    private EscapeSequence escapeSequence;  // エスケープシーケンスの操作
    private TermDisplay termDisplay;        // 画面の操作

    private int strStart, addStrCount;

    private StringBuilder sequenceString;     // エスケープシーケンスの文字列を保存
    private String result = "";
    private SpannableString spannable;

    private int displayRowSize, displayColumnSize;  // 画面サイズ

    private boolean isMovingCursor = false;     // カーソル移動中ならtrue
    private boolean isBtn_ctl = false;          // CTLボタンを押したらtrue
    private boolean isNotSending = false;       // RN側に送りたくないものがあるときはfalseにする
    private boolean isDisplaying = false;       // 画面更新中はtrue
    private boolean isSending = false;          // RNにデータを送信しているときtrue
    private boolean isOverWriting = false;      // 文字を上書きするときtrue

    private int stack = 0;  // 処理待ちの文字数

    private final Handler handler = new Handler();
    private final int time = 3; //

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputEditText = findViewById(R.id.main_display);
        inputEditText.setCustomSelectionActionModeCallback(mActionModeCallback);
        inputEditText.addTextChangedListener(mInputTextWatcher);
        inputEditText.setTextIsSelectable(false);

        displayRowSize = getMaxRowLength();
        displayColumnSize = getMaxColumnLength();
        termDisplay = new TermDisplay(displayRowSize, displayColumnSize);

        Log.d(TAG, "maxRow "+ getMaxRowLength() +"maxColumn" + getMaxColumnLength());

        termBuffer = new TerminalBuffer(screenRowSize, screenColumnSize);
        escapeSequence = new EscapeSequence(termBuffer); //今のContentを渡す

        connectTimeoutHandler = new Handler();

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

        findViewById(R.id.btn_up).setOnClickListener(v -> {
            if(state == State.CONNECTED) {
                bleService.writeMLDP("\u001b" + "[A");
                if(termBuffer.getCursorY() > 0){
                    moveToSavedCursor();
                }
            }
            escapeSequence.moveUp(1);
            moveToSavedCursor();
        });

        findViewById(R.id.btn_down).setOnClickListener(v -> {
            if(state == State.CONNECTED) {
                bleService.writeMLDP("\u001b" + "[B");
                if(termBuffer.getCursorY() < inputEditText.getLineCount()-1) {
                    moveToSavedCursor();
                }
            }
            escapeSequence.moveDown(1);
            moveToSavedCursor();
        });

        findViewById(R.id.btn_right).setOnClickListener(v -> {
            if(state == State.CONNECTED) {
                bleService.writeMLDP("\u001b" + "[C");
            }
            if (getSelectRowIndex() == termBuffer.getTotalColumns()-1) {
                if (termBuffer.getCursorX() < termBuffer.getRowLength(getSelectRowIndex())) {
                    moveToSavedCursor();
                }
            }
        });
        findViewById(R.id.btn_left).setOnClickListener(v -> {
            if(state == State.CONNECTED) {
                bleService.writeMLDP("\u001b" + "[D");
            }
            if (getSelectRowIndex() == termBuffer.getTotalColumns()-1) {
                if (termBuffer.getCursorX() > 0) {
                    moveToSavedCursor();
                }
            }
        });

        findViewById(R.id.btn_esc).setOnClickListener(v -> {
            if(state == State.CONNECTED) bleService.writeMLDP("\u001b");
        });

        findViewById(R.id.btn_tab).setOnClickListener(view -> {
            if (state == State.CONNECTED) bleService.writeMLDP("\u0009");
        });

        findViewById(R.id.btn_ctl).setOnClickListener(view -> isBtn_ctl = true);

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
                        Log.d(TAG, "action down");
                        showKeyboard();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // 指を動かした時に、現在のscrollY座標とoldYを比較して、違いがあるならスクロール状態とみなす
                        Log.d(TAG, "action move");
                        hideKeyboard();
                        if (oldY > event.getRawY()) {
                            scrollDown();
                        }
                        if (oldY < event.getRawY()){
                            scrollUp();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        inputEditText.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_DEL) {
                if (state == State.CONNECTED) {
                    bleService.writeMLDP("\u0008");
                } else {
                    moveCursorX(-1);
                    moveToSavedCursor();
                }
                return true;
            }
            return false;
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
        editor.apply();
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
                //unbindService(bleServiceConnection);
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
            menu.close();
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };

    private final TextWatcher mInputTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            eStart = start;//文字列のスタート位置
            eCount = count;//追加される文字

            if (state == State.CONNECTED && count > before) {
                if (!isNotSending) {
                    String send = s.subSequence(start + before, start + count).toString();
                    Log.d("RNsend", send);
                    isSending = true;
                    if (isBtn_ctl){
                        if (send.matches("[\\x5f-\\x7e]")) {
                            byte[] sendB = {(byte) (send.getBytes()[0] & 0x1f)};
                            bleService.writeMLDP(sendB);
                            isBtn_ctl = false;
                            sendCtl = true;
                            }
                        isBtn_ctl = false;
                    }
                    if (!sendCtl) {
                        bleService.writeMLDP(send);
                    } else {
                        sendCtl = false;
                    }
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length() < 1) return;
            String str = s.subSequence(eStart, eStart + eCount).toString();//入力文字

            handler.removeCallbacks(UpdateDisplay);
            if (str.matches("[\\x20-\\x7f\\x0a\\x0d]") && !isSending) {
                if (!isDisplaying) {
                    addList(str);
                    handler.postDelayed(UpdateDisplay, time);
                }
            }
            else { //ASCIIじゃなければ入力前の状態にもどす
                isSending = false;
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
                Log.i(TAG, "Received intent  ACTION_BLE_CONNECTED");
                state = State.CONNECTED;
                updateConnectionState();

            }
            else if (MldpBluetoothService.ACTION_BLE_DISCONNECTED.equals(action)) {
                Log.i(TAG, "Received intent ACTION_BLE_DISCONNECTED");
                state = State.DISCONNECTED;
                updateConnectionState();
            }
            else if (MldpBluetoothService.ACTION_BLE_DATA_RECEIVED.equals(action)) {
                String receivedData = intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_DATA);
                if (receivedData == null) {
                    return;
                }
                int cnt = 1;
                Log.d("debug****", "receivedData" + receivedData);

                String[] splitData = receivedData.split("", -1);

                handler.removeCallbacks(UpdateDisplay);
                stack += splitData.length-2;
                Log.d("stack***", "stackLength" + stack);
                byte[] utf = receivedData.getBytes(StandardCharsets.UTF_8);

                for (byte charCode : utf) {
                    switch (charCode) {
                        case 0x08:   // KEY_BS
                            moveCursorX(-1);
                            break;
                        case 0x09:   // KEY_HT
                            if (termBuffer.getCursorX() + (8- termBuffer.getCursorX()%8) < screenRowSize){
                                escapeSequence.moveRight(8 - termBuffer.getCursorX()%8);
                            } else {
                                moveCursorX(screenRowSize -1);
                            }
                            break;
                        case 0x7f:   // KEY_DEL
                            break;
                        case 0x0a:   // KEY_LF
                            Log.d("debug****", "KEY_LF");
                            isNotSending = true;
                            addList("\n");
                            isNotSending = false;
                            break;
                        case 0x0d:   // KEY_CR
                            Log.d("debug****", "KEY_CR");
                            termBuffer.setCursorX(0);
                            moveToSavedCursor();
                            break;
                        case 0x1b:  // KEY_ESC
                            Log.d(TAG, "receive esc");
                            isEscapeSequence = true;
                            escapeString.setLength(0);
                            break;
                        default:
                            if (isEscapeSequence){
                                escapeString.append(splitData[cnt]);
                                if (splitData[cnt].matches("[A-HJKSTZfm]")){
                                    checkEscapeSequence();
                                    isEscapeSequence = false;
                                }
                            } else {
                                if (cnt <= receivedData.length()) {
                                    if (splitData[cnt].equals("\u0020")) {
                                        splitData[cnt] = " ";
                                    }
                                    isNotSending = true;
                                    addList(splitData[cnt]);
                                    isNotSending = false;
                                }
                            }
                            break;
                    }
                    stack--;
                    Log.d("stack***", "stackLength" + stack);
                    cnt++;
                    if (stack == 0){
                        Log.d("TermDisplay****", "stack is 0");
                        handler.postDelayed(UpdateDisplay, time);
                    }
                }
            }
        }

    };

    // エスケープシーケンスの処理
    private void checkEscapeSequence(){
        int length = escapeString.length();
        char mode = escapeString.charAt(length-1);
        int move = 1, h_move = 1;
        int semicolonPos;

        if (length != 2){
            if (!(mode == 'H' || mode == 'f')){
                move = Integer.parseInt(escapeString.substring(1, length-2));
            } else {
                semicolonPos = escapeString.indexOf(";");
                if(semicolonPos != 2){
                    move = Integer.parseInt(escapeString.substring(2, semicolonPos-1));
                }
                if (escapeString.charAt(semicolonPos+1) != 'H' || escapeString.charAt(semicolonPos+1) != 'f'){
                    h_move = Integer.parseInt(escapeString.substring(semicolonPos+1, length-2));
                }
            }
        }

        switch (mode) {
            case 'A':
                escapeSequence.moveUp(move);
                break;
            case 'B':
                escapeSequence.moveDown(move);
                break;
            case 'C':
                escapeSequence.moveRight(move);
                break;
            case 'D':
                escapeSequence.moveLeft(move);
                break;
            case 'E':
                escapeSequence.moveDownToRowLead(move);
                break;
            case 'F':
                escapeSequence.moveUpToRowLead(move);
                break;
            case 'G':
                escapeSequence.moveCursor(move);
                break;
            case 'H':
            case 'f':
                escapeSequence.moveCursor(h_move, move);
                break;
            case 'J':
                escapeSequence.clearDisplay(move);
                break;
            case 'K':
                escapeSequence.clearRow(move);
                break;
            case 'S':
                escapeSequence.scrollNext(move);
                break;
            case 'T':
                escapeSequence.scrollBack(move);
                break;
            case 'Z':
                //TODO 画面のサイズを送信するエスケープシーケンスの実装
                isSending = true;
                bleService.writeMLDP(Integer.toString(getMaxRowLength()));
                bleService.writeMLDP(Integer.toString(getMaxColumnLength()));
                break;
            case 'm':
                escapeSequence.selectGraphicRendition(move);
                inputEditText.setTextColor(termBuffer.getCharColor());
                break;
            default:
                break;
        }
    }

    // 接続
    private boolean connectWithAddress(String address) {
        state = State.CONNECTING;
        updateConnectionState();                                                                    
        connectTimeoutHandler.postDelayed(abortConnection, CONNECT_TIME);
        return bleService.connect(address);
    }

    // 切断
    private final Runnable abortConnection = new Runnable() {
        @Override
        public void run() {
            if (state == State.CONNECTING) {
                bleService.disconnect();                      							            
            }
        }
    };

    // 周りにあるBLEをスキャン
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

    // bluetoothの接続状況を更新
    private void updateConnectionState() {
        runOnUiThread(() -> {
            switch (state) {
                case STARTING:
                case ENABLING:
                case SCANNING:
                case DISCONNECTED:
                    stack = 0;
                    setProgressBarIndeterminateVisibility(false);
                    break;
                case CONNECTING:
                    setProgressBarIndeterminateVisibility(true);
                    break;
                case CONNECTED:
                    isNotSending = true;
                    addNewLine(LF + "connect to " + bleDeviceName);
                    setProgressBarIndeterminateVisibility(false);
                    //bleService.writeMLDP("MLDP\r\nApp:on\r\n");
                    break;
                case DISCONNECTING:
                    setProgressBarIndeterminateVisibility(false);
                    isNotSending = true;
                    addNewLine(LF + "disconnected from " + bleDeviceName);
                    break;
                default:
                    state = State.STARTING;
                    setProgressBarIndeterminateVisibility(false);
                    break;
            }

            invalidateOptionsMenu();
        });
    }

    // 別Activityからの処理結果をうけとる
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
                        Log.d(TAG, "connect is failed");
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

    // bluetoothのserviceを使う
    private final ServiceConnection bleServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MldpBluetoothService.LocalBinder binder = (MldpBluetoothService.LocalBinder) service;
            bleService = binder.getService();
            if (!bleService.isBluetoothRadioEnabled()) {
                state = State.ENABLING;
                updateConnectionState();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    // 新しい行を追加
    private void addNewLine(String newText){
        for(int i = 0; i < newText.length(); i++){
            isNotSending = true;
            inputEditText.append(Character.toString(newText.charAt(i)));
        }
        inputEditText.append("\n");
        termBuffer.setCursorX(0);
        isNotSending = false;
    }

    // １行に収まる文字数を返す
    private int getMaxRowLength(){
        WindowManager wm =  (WindowManager)getSystemService(WINDOW_SERVICE);
        Display disp = null;
        Point p = new Point();

        if (wm != null) {
            disp = wm.getDefaultDisplay();
        }

        if (disp != null) {
            disp.getSize(p);
        }

        return p.x/getTextWidth();
    }

    // １列に収まる文字数を返す
    private int getMaxColumnLength(){
        WindowManager wm =  (WindowManager)getSystemService(WINDOW_SERVICE);
        Display disp = null;
        Point p = new Point();
        if (wm != null) {
            disp = wm.getDefaultDisplay();
        }
        if (disp != null) {
            disp.getSize(p);
        }

        int height = p.y - 100;
        int text = (int)getTextHeight();

        return (height/text)-1;
    }

    // テキストの文字の横幅を返す
    private int getTextWidth(){
        // TypefaceがMonospace 「" "」の幅を取得
        Paint paint = new Paint();
        paint.setTextSize(inputEditText.getTextSize());
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        return (int)paint.measureText(" ");
    }

    // テキストの文字の高さを返す
    private float getTextHeight(){
        Paint paint = new Paint();
        paint.setTextSize(inputEditText.getTextSize());
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return Math.abs(fontMetrics.top) + Math.abs(fontMetrics.bottom);
    }

    // 選択中の行番号を返す
    private int getSelectRowIndex() {
        if(termBuffer.getCursorY() + termBuffer.getTopRow() <= 0){
            return 0;
        }
        return termBuffer.getCursorY() + termBuffer.getTopRow();
    }

    // キーボードを表示させる
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null)
            if (imm != null) {
                imm.showSoftInput(v, 0);
            }
    }

    //　キーボードを隠す
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null)
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(),0);
            }
    }

    // ディスプレイに文字を表示する
    private void changeDisplay(){
        isDisplaying = true;
        isNotSending = true;

        inputEditText.getText().clear();
        if (!termBuffer.isColorChange()){
            inputEditText.append(termBuffer.createDisplay());
        } else {
            spannable = new SpannableString(termBuffer.createDisplay());
            result = HtmlParser.INSTANCE.toHtml(spannable);
            inputEditText.append(Html.fromHtml(result));
        }
        isDisplaying = false;
        isNotSending = false;
    }

    // 現在の行のテキストを返す
    private String getSelectRowText(){
        return termBuffer.getRowText(getSelectRowIndex());
    }

    // カーソルを横方向にx移動させる
    private void moveCursorX(int x){
        termBuffer.setCursorX(termBuffer.getCursorX() + x);
    }

    // カーソルを縦方向にy移動させる
    private void moveCursorY(int y){
        termBuffer.setCursorY(termBuffer.getCursorY() + y);
    }

    // カーソルを保持している座標に移動させる
    private void moveToSavedCursor(){
        if(!isMovingCursor) {
            isMovingCursor = true;

            int cursor;
            cursor = getCursorPosition(termBuffer.getCursorX(), termBuffer.getCursorY());

            Log.d("debug***", "cursor" + cursor);
            if (cursor >= 0) {
                inputEditText.setSelection(cursor);
            }
            isMovingCursor = false;
        }
    }

    // カーソルの座標からポジションを返す
    private int getCursorPosition(int x, int y){
        int length = 0;
        int rowLength = termBuffer.getRowLength(termBuffer.getTopRow() + y);
        String rowText = termBuffer.getRowText(termBuffer.getTopRow() + y);

        for (int i = 0; i < y; i++){
            length = length + termBuffer.getRowLength(termBuffer.getTopRow() + i);
            if (termBuffer.getRowLength(termBuffer.getTopRow() + i) == 0){
                length++;
            } else {
                if (!termBuffer.getRowText(termBuffer.getTopRow() + i).contains("\n")) {
                    length++;
                }
            }
        }
        if(rowLength == 0) x = 0; //空

        if (x > rowLength){ //移動先の文字数がカーソルXよりも短い
            if (rowText.contains("\n")) {
                x = rowLength - 1;
            } else {
                x = rowLength;
            }
        }

        if(x == rowLength && rowText.lastIndexOf(LF) != -1){ //カーソルXが移動先の文字数と等しくて，改行コードが存在する
            x = rowLength-1;
        }

        length = length + x;
        if (length > inputEditText.length()){
            length = inputEditText.length();
        }
        return length;
    }

    // 画面を上にスクロールする
    private void scrollUp(){
        if (termBuffer.getTotalColumns() > screenColumnSize) {
            if (termBuffer.getTopRow() - 1 >= 0) {
                //表示する一番上の行を１つ上に
                termBuffer.moveTopRow(-1);
                // カーソルが画面内にある
                if (termBuffer.getTopRow() <= termBuffer.getCurrRow() && termBuffer.getCurrRow() < termBuffer.getTopRow() + screenColumnSize) {
                    setEditable(true);
                    moveCursorY(1);
                } else { //画面外
                    // 0のときは表示させる
                    if (termBuffer.getTopRow() == 0) {
                        setEditable(true);
                    } else {
                        setEditable(false);
                    }
                }
                if (stack == 0) {
                    changeDisplay();
                    moveToSavedCursor();
                }
            }
        }
    }

    // 画面を下にスクロールする
    private void scrollDown(){
        if (termBuffer.getTotalColumns() > screenColumnSize) {
            // 一番下の行までしか表示させない
            if (termBuffer.getTopRow() + screenColumnSize < termBuffer.getTotalColumns()) {
                //表示する一番上の行を１つ下に
                termBuffer.moveTopRow(1);
                if (termBuffer.getTopRow() < termBuffer.getCurrRow() && termBuffer.getCurrRow() <= termBuffer.getTopRow() + screenColumnSize -1){
                    setEditable(true);
                    moveCursorY(-1);
                } else {
                    // 一番したのときは表示させる
                    if (termBuffer.getCurrRow() == termBuffer.getTopRow() + screenColumnSize -1){
                        setEditable(true);
                    } else {
                        setEditable(false);
                    }
                }
                if (stack == 0){
                    changeDisplay();
                    moveToSavedCursor();
                }
            }
        }
    }

    // 画面に書き込めなくする
    private void setEditable(boolean b){
        if (b){
            inputEditText.setFocusable(true);
            inputEditText.setFocusableInTouchMode(true);
            inputEditText.requestFocus();
            termBuffer.setOutOfScreen(false);
        } else {
            inputEditText.setFocusable(false);
            termBuffer.setOutOfScreen(true);
        }
    }

    // 画面更新を非同期で行う
    private final Runnable UpdateDisplay = () -> {
        changeDisplay();
        moveToSavedCursor();
    };

    private void focusable(){
        inputEditText.setFocusable(true);
        inputEditText.setFocusableInTouchMode(true);
        inputEditText.requestFocus();

    }

    // strをリストに格納
    private void addList(String str){
        if (str.matches("[\\x20-\\x7f\\x0a\\x0d]")){

            // カーソルが画面外で入力があると入力位置に移動
            if (termBuffer.isOutOfScreen()) {
                focusable();
                termBuffer.setTopRow(termBuffer.getCurrRow() - termBuffer.getCursorY());
                moveToSavedCursor();
            }

            // FIXME わからん 右端で入力があったらカーソル移動させない？
            if (termBuffer.getCursorX() > getSelectRowText().length()) {
                termBuffer.setCursorX(getSelectRowText().length());
                if (getSelectRowText().contains("\n")){
                    moveCursorX(-1);
                }
            }

            //
            char inputStr = str.charAt(0);

            // カーソルが入力文字列の右端
            if (termBuffer.getCursorX() == termBuffer.getRowLength(getSelectRowIndex())) {
                Log.d("termBuffer****", "set");

                // 入力行が一番したの行ならそのまま入力
                if (getSelectRowIndex() == termBuffer.getTotalColumns() - 1) {
                    termBuffer.addText(getSelectRowIndex(), inputStr, termBuffer.getCharColor());
                } else {
                    // 入力行が一番下じゃないかつ改行コードがないなら文字を追加
                    if (!getSelectRowText().contains("\n")) {
                        termBuffer.addText(getSelectRowIndex(), inputStr, termBuffer.getCharColor());
                    }
                }
                // カーソルを移動
                moveCursorX(1);
            } else { //insert
                // カーソルが入力文字列の途中
                Log.d("termBuffer****", "overwrite");
                // 入力が改行じゃなければ文字を上書き
                if (inputStr != LF) {
                    // 上書き
                    termBuffer.setText(termBuffer.getCursorX(), getSelectRowIndex(), inputStr);
                    // FIXME どういうこと？
                    if (termBuffer.getCursorX() + 1 < screenRowSize) {
                        isOverWriting = true;
                        moveCursorX(1);
                    } else {
                        isOverWriting = false;
                    }

                } else { //LF
                    // 途中で行を変える場合
                    // 入力位置が一番下で改行がなければ
                    if (getSelectRowIndex() == termBuffer.getTotalColumns() - 1 && !getSelectRowText().contains("\n")) {
                        // 入力後の文字数が画面サイズより小さい
                        if (termBuffer.getRowLength(getSelectRowIndex()) + 1 < screenRowSize) {
                            // FIXME ???? 一番最後に改行を追加(ターミナルの場合途中で改行の場合次の行にいくぽい)
                            termBuffer.addText(getSelectRowIndex(), inputStr, termBuffer.getCharColor());
                        }
                    }
                }
            }

            Log.d(TAG, "ASCII code/ " + str);
            // スクロールの処理
            if (inputStr == LF) {
                termBuffer.setCursorX(0);
                if (termBuffer.getCursorY() + 1 >= screenColumnSize) {
                    scrollDown();
                }
                if (termBuffer.getCursorY() < screenColumnSize) {
                    moveCursorY(1);
                }
            }

            // 右端での入力があったときの時のスクロール
            if (getSelectRowText().length() >= screenRowSize && !getSelectRowText().contains("\n") && !isOverWriting) {
                termBuffer.setCursorX(0);
                if (inputEditText.getLineCount() >= screenColumnSize) {
                    scrollDown();
                }

                if (termBuffer.getCursorY() + 1 < screenColumnSize) {
                    moveCursorY(1);
                }
            }
            isOverWriting = false;
        }
    }
}
