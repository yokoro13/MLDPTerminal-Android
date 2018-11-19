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
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.ToDoubleBiFunction;

/**
 * TODO
 * 一番した以外で文字が追加できるのをなおす
 * 文字消去
 * 文字の幅を一定にする
 * 画面を回転させた時のデータ保持(onCreateがもう一回発動するから全部消える?)
 * onKey => textWatcherへの移行
 * エンターを押したとき，右側に空白がはいるなら，入らなくなるまでいれる（いちいち一行の文字数を取る必要がなくなる）
 */
public class MainActivity extends AppCompatActivity{

    private static final String TAG = "debug***";

    private boolean connecting = false; //接続中かどうか（true:接続中  , false:切断中）

    private final String LF = System.getProperty("line.separator"); //システムの改行コードを検出
    private EditText input; //ディスプレイのEdittext
    private String inputText; //入力したテキスト
    private ArrayList<RowListItem> items; //コンソール画面の行情報が入る
    private ArrayList<BleDevice> bleDevices; //スキャンしたデバイス情報が入る

    private boolean able_edit = true; //編集可能か

    private int lineCount; //行数
    private String before_str = ""; //１つ前の行までのテキスト情報
    private String rowText; //今の行のテキスト情報

    private RowListItem rowListItem; //行情報

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

    State state = State.STARTING;

    //コマンド用
    private String[] command;
    private final String SCAN_COMMAND = "scan";
    private final String CONNECT_COMMAND = "connect";
    private final String DISCONNECT_COMMAND = "disconnect";
    private final String ADDRESS_COMMAND = "address";

    private Boolean escFlag = false; //escキーがおされたらtrue
    private Boolean moveFlag = false; //escFlagがtrueでエスケープシーケンスがおくられて来た時true
    private int move = 0; //escapeシーケンス用
    private String moveStr = ""; //escapeシーケンスできたString型の数字を保存

    private String comingText = ""; //RNからきたテキストを一行分保存する

    Editable editable;
    private int start,end;
    EscapeSequence escapeSequence;

    int r;
    private int currCursol = 0;
    private Boolean pushEnter = false;

    private String lineText = "";
    private int maxChar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        input = (EditText) findViewById(R.id.main_display);

        //コピー，ペースト，カット，すべて選択が機能的にいらないので削除
        input.setCustomSelectionActionModeCallback(mActionModeCallback);

        input.addTextChangedListener(mInputTextWatcher);

        maxChar = getMaxRowLength();
        items = new ArrayList<>();
        bleDevices = new ArrayList<>();

        rowListItem = new RowListItem(items.size(), "", false, true); //一行目を追加
        items.add(rowListItem);

        escapeSequence = new EscapeSequence(this, items); //今のContentを渡す

        findViewById(R.id.btn_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Rect result = new Rect();
                //r = input.getHeight()/
                r = input.getWidth()/(int)input.getTextSize();//LineHeight();//LineCount();//Bounds(1, new Rect(0,0,0,0));
                //Paint paint = new Paint();
                //input.meas
                addNewLine(Integer.toString(r));
                //escapeSequence.moveLeft(1);
            }
        });

        findViewById(R.id.btn_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                escapeSequence.moveRight(1);
            }
        });

        findViewById(R.id.btn_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                escapeSequence.moveUp(1);
            }
        });

        findViewById(R.id.btn_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                escapeSequence.moveDown(1);
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

        state = State.STARTING;

        inputText = input.getText().toString();

        connectTimeoutHandler = new Handler();

        //registerReceiver(bleServiceReceiver, bleServiceIntentFilter());

        //画面タッチされた時のおべんと
        //カーソル下のピンクなくす
        input.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (input.getSelectionStart() < before_str.length()) {
                        input.setSelection(currCursol);
                        return true;
                    }
                    else{
                        currCursol = input.getSelectionStart();
                    }

                //}
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
        if(bleAutoConnect == true) {
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
            menu.removeItem(android.R.id.replaceText);
            menu.removeItem(android.R.id.autofill);
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
            menu.removeItem(android.R.id.replaceText);
            menu.removeItem(android.R.id.autofill);
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
                if(able_edit)
                    bleService.writeMLDP(cs.subSequence(start + before, start + count).toString());
                    //bleService.writeMLDP(cs.subSequence(start + before, start + count).toString().getBytes());
                else able_edit = true;

            }
        }

        public void afterTextChanged(Editable edtbl) {
        }
    };

    int position;
    boolean editFlag = true;
    boolean deleteFlag = true;
    private final TextWatcher mInputTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (editFlag) {
                currCursol = input.getSelectionStart();
                inputText = s.toString(); //おされた瞬間のテキストを保持
                lineCount = input.getLineCount();

                position = start;

                if (position == 0 && 0 < count && 0 < after) {
                    position = count;
                }
            }

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            /**
             * TODO
             * ここのwriteableを使いたい
             * 一番したの団は必ずtrue
             */

            if(before > 0 && input.getSelectionStart() > 0) { //削除される文字が存在するとき
                if (s.toString().substring(input.getSelectionStart() - 1).equals(LF)
                        || !items.get(getSelectRow()).isWritable()) { //その前の文字が改行コードまたは，編集可能じゃなかったら
                    Log.d(TAG, "deleteFlag is false");
                    deleteFlag = false;//削除フラグをおる
                }
            }
        }

        //TODO ループにはいっているので直す
        @Override
        public void afterTextChanged(Editable s) {
            String str = s.toString().substring(start);//入力文字
            lineText += str;

            Log.d(TAG, "afterTextChange");
            if (str.matches("[\\p{ASCII}]*") && deleteFlag) {
                Log.d(TAG, "ASCII code/ " + str);
                if (str.equals(LF)) {
                    Log.d(TAG, "lineText is " + lineText);
                    for(int i = lineText.length()%maxChar; i < maxChar; i++){
                        input.append(" ");
                        lineText += " ";
                    }
                    rowText = inputText.substring(before_str.length(), inputText.length());
                    before_str = inputText;
                    addList(lineText);
                    lineText = "";
                    displayList();
                    //dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                }

            }
            else {
                editFlag = false;
                input.setText(inputText);
                input.setSelection(currCursol);
                lineText = lineText.substring(0, lineText.length()-1);
                editFlag = true;
                deleteFlag = true;
            }
        }
    };

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

                    /**ASCII 以外の入力制限フロー
                     * 入力文字を１６進数にし．Stringにキャスト
                     *
                     */
                    start = input.getSelectionStart();
                    end = input.getSelectionEnd();
                    switch (str) {
                        case KeyHexString.KEY_RIGHT:
                            escapeSequence.moveRight(1);
                            escFlag = false;
                            moveFlag = false;
                            break;
                        case KeyHexString.KEY_LEFT:
                            escapeSequence.moveLeft(1);
                            escFlag = false;
                            moveFlag = false;
                            break;
                        case KeyHexString.KEY_DEL:
                            if(start >= 1) {
                                input.getText().delete(start - 1, end);
                            }
                            escFlag = false;
                            moveFlag = false;
                            break;
                        case KeyHexString.KEY_ENTER:
                            addList(comingText);
                            comingText = "";
                            input.append(LF);
                            escFlag = false;
                            moveFlag = false;
                            displayList();
                            break;
                        case KeyHexString.KEY_ESC:
                            escFlag = true;
                            moveFlag = false;
                            break;
                        case "7a":
                            if(escFlag){
                                input.append("esc + Z");
                                escFlag = false;
                                break;
                            }
                        default:
                            /**
                             * 2桁の入力を可能にする　できた
                             */
                            if (escFlag && data.matches("[0-9]")) {
                                Log.d(TAG, "move flag is true");
                                moveStr += data;
                                moveFlag = true;
                                break;
                            }
                            if(escFlag && data.matches("[A-H]")){
                                if(moveFlag) {
                                    move = Integer.parseInt(moveStr);
                                }else {
                                    move = 1;
                                }
                                moveStr = "";

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
                                    escapeSequence.moveSelection(move, move);
                                }
                                moveFlag = false;
                                escFlag = false;
                                break;
                            }

                            editable = input.getText();
                            able_edit = false;
                            editable.replace(Math.min(start, end), Math.max(start, end), data);
                            //input.append(str);
                            comingText += data;
                            input.setSelection(input.getText().length());
                            //able_edit = true;
                            escFlag = false;
                            moveFlag = false;

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
                        input.removeTextChangedListener(mOutgoingTextWatcher);
                        input.addTextChangedListener(mOutgoingTextWatcher);
                        break;
                    case CONNECTED:
                        addNewLine("connected to name: " + bleDeviceName + "    ---address: " + bleDeviceAddress);
                        setProgressBarIndeterminateVisibility(false);
                        break;
                    case DISCONNECTING:
                        addNewLine("disconnected " + bleDeviceName + "  ---address: " + bleDeviceAddress);
                        input.removeTextChangedListener(mOutgoingTextWatcher);
                        setProgressBarIndeterminateVisibility(false);
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
                if(bleAutoConnect == false  || bleDeviceAddress == null) {
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
            if (bleService.isBluetoothRadioEnabled()) {
                if(bleAutoConnect == false  || bleDeviceAddress == null) {
                    //startScan();
                }
            }
            else {
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

    //Listの中身を表示
    private void displayList() {
        for (int i = 0; i < items.size(); i++) {
            Log.d("debug***", Integer.toString(items.get(i).getText().length()));
        }
    }

    private void addNewLine(String newStr) {
        input.append(newStr + LF);
        rowText = inputText.substring(before_str.length(), inputText.length());
        before_str = inputText;
        addList(rowText);
        input.setSelection(input.getText().length());
    }

    /**
     * list設計
     * 1行に入る文字数を調べる. maxchar input/maxchar == rowNum
     * maxchar > input -> hasNext is true. subString(0, maxchar) and subString(maxchar, input.length)
     * else : hasNext is false. subString (0, input.length).
     * list <- (id, text, hasNext, writable).
     */

    private void addList(String row){
        //TODO 一行に入る最大文字数の求め方を考える.
        Log.d(TAG, "max char is " + Integer.toString(maxChar));
        String text; //一行の文字列を格納
        String str = row; //ちぎるやつ

        int rowNum = str.length()/maxChar; //入力に使われる行数
        if(str.length()%maxChar > 0){
            rowNum++;
        }
        Boolean hasNext = false; //次の行がある場合はtrue

        for(int i = rowNum; i > 0; i--) {
            if (str.length() > maxChar) {
                hasNext = true;
                text = str.substring(0, maxChar);
                str = str.substring(maxChar+1, str.length() - 1);
            } else {
                hasNext = false;
                text = str;
            }
            Log.d(TAG, "add list");
            rowListItem = new RowListItem(items.size()-1, text, hasNext, false);
            items.set(items.size()-1, rowListItem);
            rowListItem = new RowListItem(items.size(), "", false, true);
            items.add(rowListItem);
            Log.d(TAG, "add list /" + str);
        }

    }

    private int getSelectRow(){
        int count = 0;
        int start = input.getSelectionStart();
        int row = 0;
        for (; count <= start;row++){ //TODO あってるか微妙
            //Log.d(TAG, "getSelectRow");
            if(row < items.size()-1 ){
                //Log.d(TAG, Integer.toString(row) + " : " + Integer.toString(count));
                count += items.get(row).getText().length();
            }
            else break;
        }
        return row;
    }

    //TODO 一行に入る最大文字数の求め方を考える.
    private int getMaxRowLength(){
        int weight = input.getWidth(); //画面の横幅
        Log.d(TAG, "display weight is " + Integer.toString(weight));
        int size = (int)input.getTextSize();
        Log.d(TAG, "text size is " + Integer.toString(size));
        //int maxChar = weight/size; //1行に入る文字数

        int maxChar = 42;
        return maxChar;
    }

}
