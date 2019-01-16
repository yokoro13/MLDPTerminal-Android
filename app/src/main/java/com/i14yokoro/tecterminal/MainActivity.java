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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
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
 * TODO RN側からきた文字の処理の仕方を考える
 */
public class MainActivity extends AppCompatActivity{

    private static final String TAG = "debug***";

    private final char LF = '\n'; //システムの改行コードを検出
    private EditText inputEditText; //ディスプレイのEdittext

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
    private enum EscapeState {NONE, ESCAPE, SQUARE}
    private EscapeState escapeState = EscapeState.NONE;

    private State state = State.STARTING;

    private String escapeMoveNum = ""; //escapeシーケンスできたString型の数字を保存
    private String clear = "";
    private int h1;

    private Editable editable;
    private EscapeSequence escapeSequence;
    private TermDisplay termDisplay;

    float r;

    private int eStart, eCount, eBefore;

    private boolean escapeMoveFlag = false; //escFlagがtrueでエスケープシーケンスがおくられて来た時true
    private boolean editingFlag = true;
    private boolean enterPutFlag = true;

    private boolean Hflag = false;

    private boolean selectionMovingFlag = false;

    private boolean isBtn_ctl = false;

    private boolean receivingFlag = true; //RN側に送りたくないものがあるときはfalseにする
    private boolean displayingFlag = false;

    private boolean isWriting = false;

    private String result = "";
    private SpannableString spannable;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputEditText = (EditText) findViewById(R.id.main_display);
        inputEditText.setCustomSelectionActionModeCallback(mActionModeCallback);
        inputEditText.addTextChangedListener(mInputTextWatcher);

        termDisplay = new TermDisplay(getMaxRowLength(), getMaxColumnLength());

        Log.d(TAG, "maxRow "+Integer.toString(getMaxRowLength()) +"maxColumn" + Integer.toString(getMaxColumnLength()));

        escapeSequence = new EscapeSequence(termDisplay); //今のContentを渡す

        state = State.STARTING;
        connectTimeoutHandler = new Handler();

        findViewById(R.id.btn_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\u001b" + "[1A");
                    if(termDisplay.getCursorY() > 0){
                        moveToSavedCursor();
                    }
                }
                moveToSavedCursor();
            }
        });

        findViewById(R.id.btn_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\u001b" + "[1B");
                    if(termDisplay.getCursorY() < inputEditText.getLineCount()-1) {
                        //escapeSequence.moveDown();
                        moveToSavedCursor();
                    }
                }
                moveToSavedCursor();
            }
        });

        findViewById(R.id.btn_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\u001b" + "[1C");
                }
                if (getSelectRowIndex() == termDisplay.getTotalColumns()-1) {
                    if (termDisplay.getCursorX() < termDisplay.getRowLength(getSelectRowIndex())) {
                        moveToSavedCursor();
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
                if (getSelectRowIndex() == termDisplay.getTotalColumns()-1) {
                    if (termDisplay.getCursorX() > 0) {
                        moveToSavedCursor();
                    }
                }
            }
        });

        findViewById(R.id.btn_esc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == State.CONNECTED) {
                    bleService.writeMLDP("\u001b");
                }
                Log.d("termDisplay**", "display size" + termDisplay.getDisplaySize());
            }
        });

        findViewById(R.id.btn_ctl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBtn_ctl = true;
                moveToSavedCursor();
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
                        Log.d(TAG, "action down");
                        showKeyboard();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // 指を動かした時に、現在のscrollY座標とoldYを比較して、違いがあるならスクロール状態とみなす
                        Log.d(TAG, "action move");
                        hideKeyboard();
                        if (oldY > event.getRawY()) {
                            scrollUp();
                        }
                        if (oldY < event.getRawY()){
                            scrollDown();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    default:
                        break;
                }
                return false;
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

    private final TextWatcher mInputTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            eStart = start;//文字列のスタート位置
            eCount = count;//追加される文字
            eBefore = before;//削除すう

            if (state == State.CONNECTED && count > before) {
                if (receivingFlag) {
                    String send = s.subSequence(start + before, start + count).toString();
                    Log.d("RNsend", send);
                    isWriting = true;
                    if (isBtn_ctl){
                        switch (send){
                            case "C":
                                send = "\u0003";
                        }
                    }
                    bleService.writeMLDP(send);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length() < 1) return;
            String str = s.subSequence(eStart, eStart + eCount).toString();//入力文字

            Log.d(TAG, "afterTextChange");

            if (str.matches("[\\x20-\\x7f\\x0a\\u232b]") && !isWriting/*&& items.get(getSelectRow()).isWritable()*/ ) {
                if (!displayingFlag) {
                    if (enterPutFlag) { //無限ループ防止
                        if (termDisplay.getCursorX() > getSelectLineText().length()) {
                            termDisplay.setCursorX(getSelectLineText().length());
                        }
                        if (eBefore > 0) {
                            //TODO 削除キナ会なおす
                            if (termDisplay.getCursorX() > 0) {
                                moveCursorX(-1);
                            }
                            changeDisplay();
                        }
                        for (int i = 0; i < str.length(); i++) { // strの先頭から1文字ずつString型にして取り出す
                            char inputStr = str.charAt(i);

                            if (termDisplay.getCursorX() == termDisplay.getRowLength(getSelectRowIndex())) {
                                Log.d("termDisplay****", "set");
                                if(getSelectRowIndex() == termDisplay.getTotalColumns()-1) {
                                    termDisplay.setTextItem(inputStr, termDisplay.getDefaultColor());
                                } else {
                                    termDisplay.addTextItem(getSelectRowIndex(), inputStr, termDisplay.getDefaultColor());
                                }
                                moveCursorX(1);
                            } else { //insert
                                Log.d("termDisplay****", "insert");
                                if(inputStr != LF) { //LFじゃない
                                    termDisplay.changeTextItem(termDisplay.getCursorX(), getSelectRowIndex(), inputStr, termDisplay.getDefaultColor());
                                    moveCursorX(1);
                                } else { //LF
                                    if(termDisplay.getRowLength(getSelectRowIndex())+1 < termDisplay.getDisplayRowSize()){ //
                                        termDisplay.addTextItem(getSelectRowIndex(), inputStr, termDisplay.getDefaultColor());
                                    }
                                }
                                changeDisplay();
                            }

                            Log.d(TAG, "ASCII code/ " + str);
                            if (inputStr == LF) {
                                termDisplay.setCursorX(0);
                                if (inputEditText.getLineCount() > termDisplay.getDisplayColumnSize()) {
                                    scrollDown();
                                }
                                if (termDisplay.getCursorY() < termDisplay.getDisplayColumnSize()) {
                                    moveCursorY(1);
                                    moveToSavedCursor();
                                }
                            }

                            Log.d("termDisplay**", "row length " + Integer.toString(getSelectLineText().length()));
                            if (getSelectLineText().length() >= termDisplay.getDisplayRowSize() && !getSelectLineText().contains("\n")) {
                                enterPutFlag = false;
                                displayingFlag = true;
                                receivingFlag = false;
                                //inputEditText.append(LF);
                                changeDisplay();
                                enterPutFlag = true;
                                displayingFlag = false;
                                receivingFlag = true;

                                termDisplay.setCursorX(0);
                                if (inputEditText.getLineCount() > termDisplay.getDisplayColumnSize()) {
                                    scrollDown();
                                }

                                termDisplay.setCursorX(0);
                                if (termDisplay.getCursorY() + 1 < termDisplay.getDisplayColumnSize()) {
                                    moveCursorY(1);
                                    moveToSavedCursor();
                                }
                            }
                        }
                    }
                }
            }
            else { //ASCIIじゃなければ入力前の状態にもどす
                if(editingFlag) {
                    editingFlag = false;
                    changeDisplay();
                    editingFlag = true;
                }
                isWriting = false;
            }
            moveToSavedCursor();
            inputEditText.getText().setSpan(watcher, 0, inputEditText.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
    };

    final SpanWatcher watcher = new SpanWatcher() {
        @Override
        public void onSpanAdded(final Spannable text, final Object what, final int start, final int end) {
        }

        @Override
        public void onSpanRemoved(final Spannable text, final Object what, final int start, final int end) {
        }

        @Override
        public void onSpanChanged(final Spannable text, final Object what, final int ostart, final int oend, final int nstart, final int nend) {
            if (what == Selection.SELECTION_START) {
                // Selection start changed from ostart to nstart.
                Log.d(TAG, "selection start is changed");
                moveToSavedCursor();
            } else if (what == Selection.SELECTION_END) {
                // Selection end changed from ostart to nstart.
                Log.d(TAG, "selection end is changed");
                moveToSavedCursor();
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
                //Log.d(TAG, "Received intent ACTION_BLE_DATA_RECEIVED");
                String data = intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_DATA);

                //Log.d("RNreceive", data);

                int cnt = 0;

                //FIXME エスケープシーケンスの英語部分が表示されてしまう
                 if (data != null) {
                     String[] strings = data.split("", -1);
                     //Log.d("debug****", "str[]" + strings[cnt+1]);
                     String str="";
                     byte[] utf = data.getBytes(StandardCharsets.UTF_8);
                     for (byte b : utf) {
                         str = Integer.toHexString(b & 0xff);

                         switch (str) {
                             case KeyHexString.KEY_DEL:
                                 escapeState = EscapeState.NONE;
                                 escapeMoveFlag = false;
                                 break;
                             case KeyHexString.KEY_ENTER:
                                 editable = inputEditText.getText();
                                 receivingFlag = false;
                                 editable.replace(termDisplay.getCursorX(), termDisplay.getCursorX(), "\n");
                                 receivingFlag = true;

                                 changeDisplay();
                                 escapeState = EscapeState.NONE;
                                 escapeMoveFlag = false;
                                 break;
                             case KeyHexString.KEY_ESC:
                                 Log.d(TAG, "receive esc");
                                 escapeState = EscapeState.ESCAPE;
                                 escapeMoveFlag = false;
                                 break;
                             case KeyHexString.KEY_SQUARE_LEFT:
                                 Log.d(TAG, "receive square");
                                 if (escapeState == EscapeState.ESCAPE) {
                                     escapeState = EscapeState.SQUARE;
                                     break;
                                 } else {
                                     escapeState = EscapeState.NONE;
                                 }
                                 escapeMoveFlag = false;
                             default:

                                 if (escapeState == EscapeState.SQUARE && strings[cnt+1].matches("[0-9]")) {
                                     Log.d(TAG, "move flag is true");
                                     escapeMoveNum += strings[cnt+1];
                                     clear += strings[cnt+1];
                                     if (Integer.parseInt(escapeMoveNum) > 1000 || Integer.parseInt(clear) > 1000) {
                                         escapeMoveNum = "1000";
                                         clear = "1000";
                                     }
                                     escapeMoveFlag = true;
                                     break;
                                 }
                                 if (escapeState == EscapeState.SQUARE && strings[cnt+1].equals(";")) {
                                     if (escapeMoveFlag) {
                                         h1 = Integer.parseInt(escapeMoveNum);
                                     } else {
                                         h1 = 1;
                                     }
                                     escapeMoveNum = "";
                                     clear = "";
                                     Hflag = true;
                                     escapeMoveFlag = false;
                                     break;
                                 }
                                 if (Hflag && !strings[cnt+1].matches("[Hf]")) {
                                     escapeMoveNum = "";
                                     clear = "";
                                     escapeMoveFlag = false;
                                     escapeState = EscapeState.NONE;
                                     break;
                                 }
                                 if (escapeState == EscapeState.SQUARE) {
                                     if (strings[cnt+1].matches("[A-HJKSTfm]")) {
                                         //escapeシーケンス用
                                         int move;
                                         int clearNum;
                                         if (escapeMoveFlag) {
                                             move = Integer.parseInt(escapeMoveNum);
                                             clearNum = Integer.parseInt(clear);
                                         } else {
                                             move = 1;
                                             clearNum = 0;
                                         }
                                         escapeMoveNum = "";

                                         Log.d(TAG, "moveFlag true && A-H");
                                         switch (str) {
                                             case KeyHexString.KEY_A:
                                                 if (move >= termDisplay.getDisplayColumnSize())
                                                     move = termDisplay.getDisplayColumnSize() - 1;
                                                 escapeSequence.moveUp(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_B:
                                                 if (move >= termDisplay.getDisplayColumnSize())
                                                     move = termDisplay.getDisplayColumnSize() - 1;
                                                 escapeSequence.moveDown(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_C:
                                                 if (move >= termDisplay.getDisplayRowSize())
                                                     move = termDisplay.getDisplayRowSize() - 1;
                                                 escapeSequence.moveRight(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_D:
                                                 if (move >= termDisplay.getDisplayRowSize())
                                                     move = termDisplay.getDisplayRowSize() - 1;
                                                 escapeSequence.moveLeft(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_E:
                                                 if (move >= termDisplay.getDisplayColumnSize())
                                                     move = termDisplay.getDisplayColumnSize() - 1;
                                                 escapeSequence.moveDownToRowLead(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_F:
                                                 if (move >= termDisplay.getDisplayColumnSize())
                                                     move = termDisplay.getDisplayColumnSize() - 1;
                                                 escapeSequence.moveUpToRowLead(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_G:
                                                 if (move >= termDisplay.getDisplayRowSize())
                                                     move = termDisplay.getDisplayRowSize() - 1;
                                                 escapeSequence.moveSelection(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_H:
                                                 if (!Hflag) {
                                                     h1 = 1;
                                                     move = 1;
                                                 }
                                                 if (h1 >= termDisplay.getDisplayColumnSize())
                                                     h1 = termDisplay.getDisplayColumnSize() - 1;
                                                 if (move >= termDisplay.getDisplayRowSize())
                                                     move = termDisplay.getDisplayRowSize() - 1;
                                                 escapeSequence.moveSelection(h1, move);
                                                 Hflag = false;
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_J:
                                                 escapeSequence.clearDisplay(clearNum);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_K:
                                                 escapeSequence.clearRow(clearNum);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_S:
                                                 escapeSequence.scrollNext(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_T:
                                                 escapeSequence.scrollBack(move);
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_f:
                                                 if (!Hflag) {
                                                     h1 = 1;
                                                     move = 1;
                                                 }
                                                 if (h1 >= termDisplay.getDisplayColumnSize())
                                                     h1 = termDisplay.getDisplayColumnSize() - 1;
                                                 if (move >= termDisplay.getDisplayRowSize())
                                                     move = termDisplay.getDisplayRowSize() - 1;
                                                 escapeSequence.moveSelection(h1, move);
                                                 Hflag = false;
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             case KeyHexString.KEY_m:
                                                 escapeSequence.selectGraphicRendition(move);
                                                 inputEditText.setTextColor(Color.parseColor(Integer.toString(termDisplay.getDefaultColor())));
                                                 escapeMoveNum = "";
                                                 clear = "";
                                                 break;
                                             default:
                                                 break;
                                         }
                                         escapeMoveFlag = false;
                                         escapeState = EscapeState.NONE;
                                         break;
                                     }
                                 }

                                 editable = inputEditText.getText();
                                 escapeState = EscapeState.NONE;
                                 receivingFlag = false;

                                 String string;
                                 string = Character.toString(data.charAt(cnt));
                                 if (string.equals("\u0020")) {
                                     string = " ";
                                 }

                                 receivingFlag = false;
                                 editable.replace(termDisplay.getCursorX(), termDisplay.getCursorX(), string);
                                 receivingFlag = true;
                                 escapeMoveFlag = false;

                                 break;
                         }
                         cnt++;

                     }
                     changeDisplay();
                     moveToSavedCursor();
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
                        break;
                    case CONNECTED:
                        receivingFlag = false;
                        addNewLine(LF + "connected to " + bleDeviceName);
                        setProgressBarIndeterminateVisibility(false);
                        break;
                    case DISCONNECTING:
                        setProgressBarIndeterminateVisibility(false);
                        receivingFlag = false;
                        addNewLine(LF + "disconnected from " + bleDeviceName);
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

    private void addNewLine(String newText){
        receivingFlag = false;
        for(int i = 0; i < newText.length(); i++){
            inputEditText.append(Character.toString(newText.charAt(i)));
        }
        inputEditText.append("\n");
        termDisplay.setCursorX(0);
        receivingFlag = true;
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

        Log.d(TAG, "display height is " + dispHeight);

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

        Paint paint = new Paint();
        paint.setTextSize(inputEditText.getTextSize());
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textHeight = Math.abs(fontMetrics.top) + Math.abs(fontMetrics.bottom);

        Log.d(TAG, "text Height is " + textHeight);
        return textHeight;
    }

    //選択中の行番号を返す
    private int getSelectRowIndex() {
        Log.d("select row index", "enter");
        if(termDisplay.getCursorY() + termDisplay.getTopRow() <= 0){
            return 0;
        }
        return termDisplay.getCursorY() + termDisplay.getTopRow();
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null)
            if (imm != null) {
                imm.showSoftInput(v, 0);
            }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null)
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(),0);
            }
    }

    private void changeDisplay(){
        displayingFlag = true;
        enterPutFlag = false;
        receivingFlag = false;

        if (!termDisplay.isColorChange()){
            inputEditText.setText(termDisplay.createDisplay_());
        } else {
            spannable = new SpannableString(termDisplay.createDisplay_());
            result = HtmlParser.toHtml(spannable);
            inputEditText.setText(Html.fromHtml(result));
        }
        enterPutFlag = true;
        displayingFlag = false;
        receivingFlag = true;
    }

    private String getSelectLineText(){
        Log.d("select row index", "select line ");
        return termDisplay.getRowText(getSelectRowIndex());
    }

    private void moveCursorX(int x){
        termDisplay.setCursorX(termDisplay.getCursorX() + x);
    }

    private void moveCursorY(int y){
        termDisplay.setCursorY(termDisplay.getCursorY() + y);
    }

    private void moveToSavedCursor(){
        if(!selectionMovingFlag) {
            selectionMovingFlag = true;
            Log.d(TAG, "moveToSavedCursor()");

            int cursor;
            cursor = getRowLength(termDisplay.getCursorX(), termDisplay.getCursorY());

            //Log.d("coordinate**", "moved to " + termDisplay.getCursorX() + ", " + termDisplay.getCursorY());
            if (cursor >= 0) {
                inputEditText.setSelection(cursor);
                //Log.d("coordinate**", "cursor" + cursor);
            }
            selectionMovingFlag = false;
        }
    }

    private int getRowLength(int x, int y){
        int length = 0;
        for (int i = 0; i < y; i++){
            length = length + termDisplay.getRowLength(termDisplay.getTopRow() + i);
            //Log.d("coordinate**", "contents: " + termDisplay.getRowText(termDisplay.getTopRow() + i) +
              //                              "length: " + termDisplay.getRowLength(termDisplay.getTopRow() + i));
            if (termDisplay.getRowLength(termDisplay.getTopRow() + i) == 0){
                length++;
            } else {
                if (!termDisplay.getRowText(termDisplay.getTopRow() + i).contains("\n")) {
                    length++;
                }
            }
        }
        int rowLength = termDisplay.getRowLength(termDisplay.getTopRow() + y);
        String rowText = termDisplay.getRowText(termDisplay.getTopRow() + y);
        if(rowLength == 0) x = 0;        //空
        if (x > rowLength){        //移動先の文字数がカーソルXよりも短い

            if (rowText.contains("\n")) {
                x = rowLength - 1;
            } else {
                x = rowLength;
            }
        }
        if(x == rowLength && rowText.lastIndexOf(LF) != -1){        //カーソルXが移動先の文字数と等しくて，改行コードが存在する

            x = rowLength-1;
        }
        //Log.d("coordinate**", "length: " + x);
        length = length + x;
        if (length > inputEditText.length()){
            length = inputEditText.length();
        }
        return length;
    }

    private void scrollUp(){
        if(termDisplay.getTopRow() -1 >= 0){
            if (termDisplay.getCursorY() + 1 < termDisplay.getDisplayColumnSize()) {
                moveCursorY(1);
            }
            termDisplay.addTopRow(-1);
            changeDisplay();
        }
    }

    private void scrollDown(){
        if(termDisplay.getTopRow() + 1 < termDisplay.getTotalColumns() ){
            moveCursorY(-1);
            termDisplay.addTopRow(1);
            changeDisplay();
        }
    }
}
