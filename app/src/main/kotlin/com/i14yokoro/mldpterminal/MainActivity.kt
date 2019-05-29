package com.i14yokoro.mldpterminal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.Html
import android.text.SpannableString
import android.text.TextWatcher
import android.util.Log
import android.view.ActionMode
import android.view.Display
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

import java.nio.charset.StandardCharsets
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {

    private val lf = '\n'
    private var inputEditText: EditText? = null //ディスプレイのEditText
    private var connectTimeoutHandler: Handler? = null
    private var bleService: MldpBluetoothService? = null

    private var bleDeviceName: String? = null
    private var bleDeviceAddress: String? = null

    private var bleAutoConnect: Boolean = false

    private var prefs: SharedPreferences? = null
    private var escapeState = EscapeState.NONE

    private var state = State.STARTING

    private var escapeMoveNum = "" //escapeシーケンスできたString型の数字を保存
    private var clear = ""
    private var h1: Int = 0

    private var escapeSequence: EscapeSequence? = null
    private var termDisplay: TermDisplay? = null

    private var eStart: Int = 0
    private var eCount: Int = 0

    private var result = ""
    private var spannable: SpannableString? = null

    private var displayRowSize: Int = 0
    private var displayColumnSize: Int = 0

    private var escapeMoveFlag = false //escFlagがtrueでエスケープシーケンスがおくられて来た時true
    private var isReceivingH = false //エスケープシーケンスのHを受信したらtrue
    private var isMovingCursor = false //カーソル移動中ならtrue
    private var isBtnCtl = false //CTLボタンを押したらtrue
    private var isNotSending = false //RN側に送りたくないものがあるときはfalseにする
    private var isDisplaying = false //画面更新中はtrue
    private var isSending = false //RNにデータを送信しているときtrue
    private var isOverWriting = false //文字を上書きするときtrue
    //private boolean isOutOfScreen = false; //カーソルが画面外か

    private var stack = 0

    private val handler = Handler()
    private val time = 3

    private val mActionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.removeItem(android.R.id.paste)
            menu.removeItem(android.R.id.cut)
            menu.removeItem(android.R.id.copy)
            menu.removeItem(android.R.id.selectAll)
            menu.removeItem(android.R.id.addToDictionary)
            menu.removeItem(android.R.id.startSelectingText)
            menu.removeItem(android.R.id.selectTextMode)
            return false
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.removeItem(android.R.id.paste)
            menu.removeItem(android.R.id.cut)
            menu.removeItem(android.R.id.copy)
            menu.removeItem(android.R.id.selectAll)
            menu.removeItem(android.R.id.addToDictionary)
            menu.removeItem(android.R.id.startSelectingText)
            menu.removeItem(android.R.id.selectTextMode)
            menu.close()
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {

        }
    }

    private val mInputTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            eStart = start//文字列のスタート位置
            eCount = count//追加される文字

            if (state == State.CONNECTED && count > before) {
                if (!isNotSending) {
                    val send = s.subSequence(start + before, start + count).toString()
                    Log.d("RNsend", send)
                    isSending = true
                    if (isBtnCtl) {
                        if (send.matches("[\\x5f-\\x7e]".toRegex())) {
                            val sendB = byteArrayOf((send.toByteArray()[0] and 0x1f))
                            bleService!!.writeMLDP(sendB)
                            isBtnCtl = false
                        }
                        isBtnCtl = false
                    }
                    bleService!!.writeMLDP(send)
                }
            }
        }

        override fun afterTextChanged(s: Editable) {
            if (s.isEmpty()) return
            val str = s.subSequence(eStart, eStart + eCount).toString()//入力文字

            handler.removeCallbacks(updateDisplay)
            if (str.matches("[\\x20-\\x7f\\x0a\\x0d]".toRegex()) && !isSending) {
                if (!isDisplaying) {
                    addList(str)
                    handler.postDelayed(updateDisplay, time.toLong())
                }
            } else { //ASCIIじゃなければ入力前の状態にもどす
                isSending = false
            }
        }
    }

    private val bleServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MldpBluetoothService.ACTION_BLE_CONNECTED -> {
                    connectTimeoutHandler!!.removeCallbacks(abortConnection)
                    Log.i(TAG, "Received intent  ACTION_BLE_CONNECTED")
                    state = State.CONNECTED
                    updateConnectionState()

                }
                MldpBluetoothService.ACTION_BLE_DISCONNECTED -> {
                    Log.i(TAG, "Received intent ACTION_BLE_DISCONNECTED")

                    state = State.DISCONNECTED
                    updateConnectionState()
                }
                MldpBluetoothService.ACTION_BLE_DATA_RECEIVED -> {
                    //Log.d(TAG, "Received intent ACTION_BLE_DATA_RECEIVED");
                    val data = intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_DATA)
                            ?: return


                    var cnt = 1
                    Log.d("debug****", "data$data")

                    val strings = data.split("".toRegex()).toTypedArray()

                    //timer.cancel();
                    handler.removeCallbacks(updateDisplay)
                    stack += strings.size - 2
                    Log.d("stack***", "stackLength$stack")
                    val utf = data.toByteArray(StandardCharsets.UTF_8)

                    for (b in utf) {
                        when (b) {
                            KeyHexString.KEY_BS -> {
                                moveCursorX(-1)
                                escapeState = EscapeState.NONE
                                escapeMoveFlag = false
                            }
                            KeyHexString.KEY_HT -> {
                                if (termDisplay!!.cursorX + (8 - termDisplay!!.cursorX % 8) < displayRowSize) {
                                    escapeSequence!!.moveRight(8 - termDisplay!!.cursorX % 8)
                                } else {
                                    moveCursorX(displayRowSize - 1)
                                }
                                escapeState = EscapeState.NONE
                                escapeMoveFlag = false
                            }
                            KeyHexString.KEY_DEL -> {
                                escapeState = EscapeState.NONE
                                escapeMoveFlag = false
                            }
                            KeyHexString.KEY_LF -> {
                                Log.d("debug****", "KEY_LF")
                                isNotSending = true
                                addList("\n")
                                isNotSending = false
                                escapeState = EscapeState.NONE
                                escapeMoveFlag = false
                            }
                            KeyHexString.KEY_CR -> {
                                Log.d("debug****", "KEY_CR")
                                termDisplay!!.cursorX = 0
                                moveToSavedCursor()
                            }
                            KeyHexString.KEY_ESC -> {
                                Log.d(TAG, "receive esc")
                                escapeState = EscapeState.ESCAPE
                                escapeMoveFlag = false
                            }
                            KeyHexString.KEY_SQUARE_LEFT -> {
                                Log.d(TAG, "receive square")
                                escapeState = if (escapeState == EscapeState.ESCAPE) {
                                    EscapeState.SQUARE
                                } else {
                                    EscapeState.NONE
                                }
                                escapeMoveFlag = false

                                if (escapeState == EscapeState.SQUARE && strings[cnt].matches("[0-9]".toRegex())) {
                                    Log.d(TAG, "move flag is true")
                                    escapeMoveNum += strings[cnt]
                                    clear += strings[cnt]
                                    if (Integer.parseInt(escapeMoveNum) > 1000 || Integer.parseInt(clear) > 1000) {
                                        escapeMoveNum = "1000"
                                        clear = "1000"
                                    }
                                    escapeMoveFlag = true
                                }
                                if (escapeState == EscapeState.SQUARE && strings[cnt] == ";") {
                                    h1 = if (escapeMoveFlag) {
                                        Integer.parseInt(escapeMoveNum)
                                    } else {
                                        1
                                    }
                                    escapeMoveNum = ""
                                    clear = ""
                                    isReceivingH = true
                                    escapeMoveFlag = false
                                }
                                if (isReceivingH && !strings[cnt].matches("[Hf]".toRegex())) {
                                    escapeMoveNum = ""
                                    clear = ""
                                    escapeMoveFlag = false
                                    escapeState = EscapeState.NONE
                                }
                                if (escapeState == EscapeState.SQUARE) {
                                    if (strings[cnt].matches("[A-HJKSTZfm]".toRegex())) {
                                        //escapeシーケンス用
                                        val move: Int
                                        val clearNum: Int
                                        if (escapeMoveFlag) {
                                            move = Integer.parseInt(escapeMoveNum)
                                            clearNum = Integer.parseInt(clear)
                                        } else {
                                            move = 1
                                            clearNum = 0
                                        }
                                        escapeMoveNum = ""

                                        Log.d(TAG, "moveFlag true && A-H")

                                        checkSequence(b, move, clearNum)

                                        escapeMoveFlag = false
                                        escapeState = EscapeState.NONE
                                    }
                                }
                                escapeState = EscapeState.NONE

                                if (cnt <= data.length) {
                                    if (strings[cnt] == "\u0020") {
                                        strings[cnt] = " "
                                    }

                                    isNotSending = true
                                    addList(strings[cnt])
                                    isNotSending = false
                                    escapeMoveFlag = false
                                }
                            }
                            else -> {
                                if (escapeState == EscapeState.SQUARE && strings[cnt].matches("[0-9]".toRegex())) {
                                    Log.d(TAG, "move flag is true")
                                    escapeMoveNum += strings[cnt]
                                    clear += strings[cnt]
                                    if (Integer.parseInt(escapeMoveNum) > 1000 || Integer.parseInt(clear) > 1000) {
                                        escapeMoveNum = "1000"
                                        clear = "1000"
                                    }
                                    escapeMoveFlag = true
                                }
                                if (escapeState == EscapeState.SQUARE && strings[cnt] == ";") {
                                    h1 = if (escapeMoveFlag) {
                                        Integer.parseInt(escapeMoveNum)
                                    } else {
                                        1
                                    }
                                    escapeMoveNum = ""
                                    clear = ""
                                    isReceivingH = true
                                    escapeMoveFlag = false
                                }
                                if (isReceivingH && !strings[cnt].matches("[Hf]".toRegex())) {
                                    escapeMoveNum = ""
                                    clear = ""
                                    escapeMoveFlag = false
                                    escapeState = EscapeState.NONE
                                }
                                if (escapeState == EscapeState.SQUARE) {
                                    if (strings[cnt].matches("[A-HJKSTZfm]".toRegex())) {
                                        val move: Int
                                        val clearNum: Int
                                        if (escapeMoveFlag) {
                                            move = Integer.parseInt(escapeMoveNum)
                                            clearNum = Integer.parseInt(clear)
                                        } else {
                                            move = 1
                                            clearNum = 0
                                        }
                                        escapeMoveNum = ""
                                        Log.d(TAG, "moveFlag true && A-H")
                                        checkSequence(b, move, clearNum)
                                        escapeMoveFlag = false
                                        escapeState = EscapeState.NONE
                                    }
                                }
                                escapeState = EscapeState.NONE
                                if (cnt <= data.length) {
                                    if (strings[cnt] == "\u0020") {
                                        strings[cnt] = " "
                                    }
                                    isNotSending = true
                                    addList(strings[cnt])
                                    isNotSending = false
                                    escapeMoveFlag = false
                                }
                            }
                        }
                        stack--
                        Log.d("stack***", "stackLength$stack")
                        cnt++
                        if (stack == 0) {
                            Log.d("TermDisplay****", "stack is 0")
                            handler.postDelayed(updateDisplay, time.toLong())
                        }

                    }
                }
            }
        }

    }

    private val abortConnection = Runnable {
        if (state == State.CONNECTING) {
            bleService!!.disconnect()
        }
    }

    private val bleServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            val binder = service as MldpBluetoothService.LocalBinder
            bleService = binder.service
            if (!bleService!!.isBluetoothRadioEnabled) {
                state = State.ENABLING
                updateConnectionState()
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bleService = null
        }
    }

    private val maxRowLength: Int
        get() {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val disp: Display? = wm.defaultDisplay

            var dispWidth = 0
            if (disp != null) {
                dispWidth = disp.width
            }
            val textWidth = textWidth
            return dispWidth / textWidth
        }

    private val maxColumnLength: Int
        get() {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val disp: Display? = wm.defaultDisplay
            var dispHeight = 0
            if (disp != null) {
                dispHeight = disp.height
            }

            val height = dispHeight - 100
            val text = textHeight.toInt()

            return height / text - 1
        }

    private// TypefaceがMonospace 「" "」の幅を取得
    val textWidth: Int
        get() {
            val paint = Paint()
            paint.textSize = inputEditText!!.textSize
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            return paint.measureText(" ").toInt()
        }

    private val textHeight: Float
        get() {
            val paint = Paint()
            paint.textSize = inputEditText!!.textSize
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            val fontMetrics = paint.fontMetrics
            return Math.abs(fontMetrics.top) + Math.abs(fontMetrics.bottom)
        }

    //選択中の行番号を返す
    private val selectRowIndex: Int
        get() = if (termDisplay!!.cursorY + termDisplay!!.topRow <= 0) {
            0
        } else termDisplay!!.cursorY + termDisplay!!.topRow

    private val selectLineText: String
        get() = termDisplay!!.getRowText(selectRowIndex)

    private val updateDisplay = {
        changeDisplay()
        moveToSavedCursor()
    }

    private enum class State {
        STARTING, ENABLING, SCANNING, CONNECTING, CONNECTED, DISCONNECTED, DISCONNECTING
    } //state

    private enum class EscapeState {
        NONE, ESCAPE, SQUARE
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputEditText = findViewById<View>(R.id.main_display) as EditText
        inputEditText!!.customSelectionActionModeCallback = mActionModeCallback
        inputEditText!!.addTextChangedListener(mInputTextWatcher)
        displayRowSize = maxRowLength
        displayColumnSize = maxColumnLength
        termDisplay = TermDisplay(displayRowSize, displayColumnSize)
        Log.d(TAG, "maxRow " + Integer.toString(maxRowLength) + "maxColumn" + Integer.toString(maxColumnLength))

        escapeSequence = EscapeSequence(termDisplay!!) //今のContentを渡す

        state = State.STARTING
        connectTimeoutHandler = Handler()

        inputEditText!!.setTextIsSelectable(false)

        findViewById<View>(R.id.btn_up).setOnClickListener {
            if (state == State.CONNECTED) {
                bleService!!.writeMLDP("\u001b" + "[A")
                if (termDisplay!!.cursorY > 0) {
                    moveToSavedCursor()
                }
            }
            escapeSequence!!.moveUp(1)
            moveToSavedCursor()
        }

        findViewById<View>(R.id.btn_down).setOnClickListener {
            if (state == State.CONNECTED) {
                bleService!!.writeMLDP("\u001b" + "[B")
                if (termDisplay!!.cursorY < inputEditText!!.lineCount - 1) {
                    moveToSavedCursor()
                }
            }
            escapeSequence!!.moveDown(1)
            moveToSavedCursor()
        }

        findViewById<View>(R.id.btn_right).setOnClickListener {
            if (state == State.CONNECTED) {
                bleService!!.writeMLDP("\u001b" + "[C")
            }
            if (selectRowIndex == termDisplay!!.totalColumns - 1) {
                if (termDisplay!!.cursorX < termDisplay!!.getRowLength(selectRowIndex)) {
                    moveToSavedCursor()
                }
            }
        }
        findViewById<View>(R.id.btn_left).setOnClickListener {
            if (state == State.CONNECTED) {
                bleService!!.writeMLDP("\u001b" + "[D")
            }
            if (selectRowIndex == termDisplay!!.totalColumns - 1) {
                if (termDisplay!!.cursorX > 0) {
                    moveToSavedCursor()
                }
            }
        }

        findViewById<View>(R.id.btn_esc).setOnClickListener { if (state == State.CONNECTED) bleService!!.writeMLDP("\u001b") }

        findViewById<View>(R.id.btn_tab).setOnClickListener { if (state == State.CONNECTED) bleService!!.writeMLDP("\u0009") }

        findViewById<View>(R.id.btn_ctl).setOnClickListener { isBtnCtl = true }

        //SDK23以降はBLEをスキャンするのに位置情報が必要
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }

        //自動接続
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs != null) {
            bleAutoConnect = prefs!!.getBoolean(PREFS_AUTO_CONNECT, false)
            if (bleAutoConnect) {
                bleDeviceName = prefs!!.getString(PREFS_NAME, null)
                bleDeviceAddress = prefs!!.getString(PREFS_ADDRESS, null)
            }
        }

        //画面タッチされた時のイベント
        inputEditText!!.setOnTouchListener(object : View.OnTouchListener {
            var oldY: Int = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // タップした時に ScrollViewのScrollY座標を保持
                        oldY = event.rawY.toInt()
                        Log.d(TAG, "action down")
                        showKeyboard()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // 指を動かした時に、現在のscrollY座標とoldYを比較して、違いがあるならスクロール状態とみなす
                        Log.d(TAG, "action move")
                        hideKeyboard()
                        if (oldY > event.rawY) {
                            scrollDown()
                        }
                        if (oldY < event.rawY) {
                            scrollUp()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                    }
                    else -> {
                    }
                }
                return false
            }
        })

        inputEditText!!.setOnKeyListener { _, i, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_DEL) {
                if (state == State.CONNECTED) {
                    bleService!!.writeMLDP("\u0008")
                } else {
                    moveCursorX(-1)
                    moveToSavedCursor()
                }
                return@setOnKeyListener true
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(bleServiceReceiver, bleServiceIntentFilter())
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bleServiceReceiver)
    }

    // デバイスデータを保存する
    public override fun onStop() {
        super.onStop()
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs!!.edit()
        editor.clear()
        if (bleAutoConnect) {
            editor.putString(PREFS_NAME, bleDeviceName)
            editor.putString(PREFS_ADDRESS, bleDeviceAddress)
        }
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        addNewLine("onDestroy")
        unbindService(bleServiceConnection)
        bleService = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_terminal_menu, menu)
        if (state == State.CONNECTED) {
            menu.findItem(R.id.menu_disconnect).isVisible = true
            menu.findItem(R.id.menu_connect).isVisible = false
        } else {
            menu.findItem(R.id.menu_disconnect).isVisible = false
            if (bleDeviceAddress != null) {
                menu.findItem(R.id.menu_connect).isVisible = true
            } else {
                menu.findItem(R.id.menu_connect).isVisible = true
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                startScan()
                return true
            }

            R.id.menu_connect -> {
                bleDeviceAddress?.let { connectWithAddress(it) }
                return true
            }

            R.id.menu_disconnect -> {
                state = State.DISCONNECTING
                updateConnectionState()
                bleService!!.disconnect()
                //unbindService(bleServiceConnection);
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkSequence(b: Byte, move1: Int, clearNum: Int) {
        var move = move1
        when (b) {
            KeyHexString.KEY_A -> {
                if (move >= displayColumnSize)
                    move = displayColumnSize - 1
                escapeSequence!!.moveUp(move)
                clearNum()
            }
            KeyHexString.KEY_B -> {
                if (move >= displayColumnSize)
                    move = displayColumnSize - 1
                escapeSequence!!.moveDown(move)
                Log.d("TermDisplay****", "cursorY" + termDisplay!!.cursorY)
                clearNum()
            }
            KeyHexString.KEY_C -> {
                if (move >= displayRowSize)
                    move = displayRowSize - 1
                escapeSequence!!.moveRight(move)
                clearNum()
            }
            KeyHexString.KEY_D -> {
                if (move >= displayRowSize)
                    move = displayRowSize - 1
                escapeSequence!!.moveLeft(move)
                clearNum()
            }
            KeyHexString.KEY_E -> {
                if (move >= displayColumnSize)
                    move = displayColumnSize - 1
                escapeSequence!!.moveDownToRowLead(move)
                clearNum()
            }
            KeyHexString.KEY_F -> {
                if (move >= displayColumnSize)
                    move = displayColumnSize - 1
                escapeSequence!!.moveUpToRowLead(move)
                clearNum()
            }
            KeyHexString.KEY_G -> {
                if (move >= displayRowSize)
                    move = displayRowSize - 1
                escapeSequence!!.moveSelection(move)
                clearNum()
            }
            KeyHexString.KEY_H -> {
                if (!isReceivingH) {
                    h1 = 1
                    move = 1
                }
                if (h1 >= displayColumnSize)
                    h1 = displayColumnSize - 1
                if (move >= displayRowSize)
                    move = displayRowSize - 1
                escapeSequence!!.moveSelection(h1, move)
                isReceivingH = false
                clearNum()
            }
            KeyHexString.KEY_J -> {
                escapeSequence!!.clearDisplay(clearNum)
                clearNum()
            }
            KeyHexString.KEY_K -> {
                escapeSequence!!.clearRow(clearNum)
                clearNum()
            }
            KeyHexString.KEY_S -> {
                escapeSequence!!.scrollNext(move)
                clearNum()
            }
            KeyHexString.KEY_T -> {
                escapeSequence!!.scrollBack(move)
                clearNum()
            }
            KeyHexString.KEY_Z -> {
                //TODO 画面のサイズを送信するエスケープシーケンスの実装
                isSending = true
                bleService!!.writeMLDP(Integer.toString(maxRowLength))
                bleService!!.writeMLDP(Integer.toString(maxColumnLength))
            }
            KeyHexString.KEY_f -> {
                if (!isReceivingH) {
                    h1 = 1
                    move = 1
                }
                if (h1 >= displayColumnSize)
                    h1 = displayColumnSize - 1
                if (move >= displayRowSize)
                    move = displayRowSize - 1
                escapeSequence!!.moveSelection(h1, move)
                isReceivingH = false
                clearNum()
            }
            KeyHexString.KEY_m -> {
                escapeSequence!!.selectGraphicRendition(move)
                inputEditText!!.setTextColor(termDisplay!!.defaultColor)
                clearNum()
            }
            else -> {
            }
        }
    }

    private fun clearNum() {
        escapeMoveNum = ""
        clear = ""
    }

    private fun connectWithAddress(address: String): Boolean {
        state = State.CONNECTING
        updateConnectionState()
        connectTimeoutHandler!!.postDelayed(abortConnection, CONNECT_TIME)
        return bleService!!.connect(address)
    }

    private fun startScan() {

        if (bleService != null) {
            bleService!!.disconnect()
            state = State.DISCONNECTING
            updateConnectionState()
        }

        //else {
        val bleServiceIntent = Intent(this@MainActivity, MldpBluetoothService::class.java)
        this.bindService(bleServiceIntent, bleServiceConnection, Context.BIND_AUTO_CREATE)
        //}

        val bleScanActivityIntent = Intent(this@MainActivity, MldpBluetoothScanActivity::class.java)
        startActivityForResult(bleScanActivityIntent, REQ_CODE_SCAN_ACTIVITY)
    }

    private fun updateConnectionState() {
        runOnUiThread {
            when (state) {
                State.STARTING, State.ENABLING, State.SCANNING, State.DISCONNECTED -> {
                    stack = 0
                    // setProgressBarIndeterminateVisibility(false)
                }
                State.CONNECTING -> setProgressBarIndeterminateVisibility(true)
                State.CONNECTED -> {
                    isNotSending = true
                    addNewLine(lf + "connect to " + bleDeviceName)
                    // setProgressBarIndeterminateVisibility(false)
                }
                State.DISCONNECTING -> {
                    // setProgressBarIndeterminateVisibility(false)
                    isNotSending = true
                    addNewLine(lf + "disconnected from " + bleDeviceName)
                }
            }//bleService.writeMLDP("MLDP\r\nApp:on\r\n");

            invalidateOptionsMenu()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQ_CODE_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (!bleAutoConnect || bleDeviceAddress == null) {
                    startScan()
                }
            }
            return
        } else if (requestCode == REQ_CODE_SCAN_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                bleDeviceAddress = intent!!.getStringExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_ADDRESS)
                bleDeviceName = intent.getStringExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_NAME)
                bleAutoConnect = intent.getBooleanExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_AUTO_CONNECT, false)
                if (bleDeviceAddress == null) {
                    state = State.DISCONNECTED
                    updateConnectionState()
                } else {
                    state = State.CONNECTING
                    updateConnectionState()
                    if (!connectWithAddress(bleDeviceAddress!!)) {
                        Log.d(TAG, "connect is failed")
                    }
                }
            } else {
                state = State.DISCONNECTED
                updateConnectionState()
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    private fun addNewLine(newText: String) {
        for (i in 0 until newText.length) {
            isNotSending = true
            inputEditText!!.append(Character.toString(newText[i]))
        }
        inputEditText!!.append("\n")
        termDisplay!!.cursorX = 0
        isNotSending = false
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = currentFocus
        if (v != null)
            imm.showSoftInput(v, 0)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = currentFocus
        if (v != null)
            imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun changeDisplay() {
        isDisplaying = true
        isNotSending = true

        inputEditText!!.text.clear()
        if (!termDisplay!!.isColorChange) {
            inputEditText!!.append(termDisplay!!.createDisplay())
        } else {
            spannable = SpannableString(termDisplay!!.createDisplay())
            result = HtmlParser.toHtml(spannable!!)
            inputEditText!!.append(Html.fromHtml(result))
        }
        //Log.d("TermDisplay****", termDisplay.createDisplay() + "END");

        isDisplaying = false
        isNotSending = false
    }

    private fun moveCursorX(x: Int) {
        termDisplay!!.cursorX = termDisplay!!.cursorX + x
    }

    private fun moveCursorY(y: Int) {
        termDisplay!!.cursorY = termDisplay!!.cursorY + y
    }

    private fun moveToSavedCursor() {
        if (!isMovingCursor) {
            isMovingCursor = true

            val cursor: Int = getRowLength(termDisplay!!.cursorX, termDisplay!!.cursorY)

            Log.d("debug***", "cursor$cursor")
            if (cursor >= 0) {
                inputEditText!!.setSelection(cursor)
            }
            isMovingCursor = false
        }
    }

    private fun getRowLength(x1: Int, y: Int): Int {
        var x = x1
        var length = 0
        val rowLength = termDisplay!!.getRowLength(termDisplay!!.topRow + y)
        val rowText = termDisplay!!.getRowText(termDisplay!!.topRow + y)

        for (i in 0 until y) {
            length += termDisplay!!.getRowLength(termDisplay!!.topRow + i)
            if (termDisplay!!.getRowLength(termDisplay!!.topRow + i) == 0) {
                length++
            } else {
                if (!termDisplay!!.getRowText(termDisplay!!.topRow + i).contains("\n")) {
                    length++
                }
            }
        }
        if (rowLength == 0) x = 0 //空

        if (x > rowLength) { //移動先の文字数がカーソルXよりも短い
            x = if (rowText.contains("\n")) {
                rowLength - 1
            } else {
                rowLength
            }
        }

        if (x == rowLength && rowText.lastIndexOf(lf) != -1) { //カーソルXが移動先の文字数と等しくて，改行コードが存在する
            x = rowLength - 1
        }

        length += x
        if (length > inputEditText!!.length()) {
            length = inputEditText!!.length()
        }
        return length
    }

    private fun scrollUp() {
        if (termDisplay!!.topRow - 1 >= 0) {
            //表示する一番上の行を１つ上に
            termDisplay!!.addTopRow(-1)
            // カーソルが画面内にある
            if (termDisplay!!.topRow < termDisplay!!.currRow && termDisplay!!.currRow < termDisplay!!.topRow + displayColumnSize) {
                setEditable(true)
                moveCursorY(1)
            } else { //画面外
                // 0のときは表示させる
                if (termDisplay!!.topRow == 0) {
                    setEditable(true)
                } else {
                    setEditable(false)
                }
            }
            if (stack == 0) {
                changeDisplay()
                moveToSavedCursor()
            }
        }
    }

    private fun scrollDown() {
        if (termDisplay!!.totalColumns > displayColumnSize) {
            if (termDisplay!!.topRow + displayColumnSize < termDisplay!!.totalColumns) {
                //表示する一番上の行を１つ下に
                termDisplay!!.addTopRow(1)
                if (termDisplay!!.topRow <= termDisplay!!.currRow && termDisplay!!.currRow < termDisplay!!.topRow + displayColumnSize - 1) {
                    setEditable(true)
                    moveCursorY(-1)
                } else {
                    // 一番したのときは表示させる
                    if (termDisplay!!.currRow == termDisplay!!.topRow + displayColumnSize - 1) {
                        setEditable(true)
                    } else {
                        setEditable(false)
                    }
                }
                if (stack == 0) {
                    changeDisplay()
                    moveToSavedCursor()
                }
            }
        }
    }

    private fun setEditable(b: Boolean) {
        if (b) {
            inputEditText!!.isFocusable = true
            inputEditText!!.isFocusableInTouchMode = true
            inputEditText!!.requestFocus()
            termDisplay!!.isOutOfScreen = false
        } else {
            inputEditText!!.isFocusable = false
            termDisplay!!.isOutOfScreen = true
        }
    }

    private fun addList(str: String) {
        if (str.matches("[\\x20-\\x7f\\x0a\\x0d]".toRegex())) {

            if (termDisplay!!.isOutOfScreen) {
                inputEditText!!.isFocusable = true
                inputEditText!!.isFocusableInTouchMode = true
                inputEditText!!.requestFocus()
                termDisplay!!.topRow = termDisplay!!.currRow - termDisplay!!.cursorY
                moveToSavedCursor()
            }
            if (termDisplay!!.cursorX > selectLineText.length) {
                termDisplay!!.cursorX = selectLineText.length
                if (selectLineText.contains("\n")) {
                    moveCursorX(-1)
                }
            }
            val inputStr = str[0]
            if (termDisplay!!.cursorX == termDisplay!!.getRowLength(selectRowIndex)) { //カーソルが行の一番最後
                Log.d("termDisplay****", "set")
                if (selectRowIndex == termDisplay!!.totalColumns - 1) { //一番したの行
                    termDisplay!!.setTextItem(inputStr, termDisplay!!.defaultColor)
                } else {
                    if (!selectLineText.contains("\n")) {
                        termDisplay!!.addTextItem(selectRowIndex, inputStr, termDisplay!!.defaultColor)
                    }
                }
                moveCursorX(1)
            } else { //insert
                Log.d("termDisplay****", "insert")
                if (inputStr != lf) { //LFじゃない
                    termDisplay!!.changeTextItem(termDisplay!!.cursorX, selectRowIndex, inputStr, termDisplay!!.defaultColor)
                    if (termDisplay!!.cursorX + 1 < displayRowSize) {
                        isOverWriting = true
                        moveCursorX(1)
                    } else {
                        isOverWriting = false
                    }

                } else { //LF
                    if (selectRowIndex == termDisplay!!.totalColumns - 1 && !selectLineText.contains("\n")) {
                        if (termDisplay!!.getRowLength(selectRowIndex) + 1 < displayRowSize) {
                            termDisplay!!.addTextItem(selectRowIndex, inputStr, termDisplay!!.defaultColor)
                        }
                    }
                }
            }

            Log.d(TAG, "ASCII code/ $str")
            if (inputStr == lf) {
                termDisplay!!.cursorX = 0
                if (termDisplay!!.cursorY + 1 >= displayColumnSize) {
                    scrollDown()
                }
                if (termDisplay!!.cursorY < displayColumnSize) {
                    moveCursorY(1)
                }
            }

            if (selectLineText.length >= displayRowSize && !selectLineText.contains("\n") && !isOverWriting) {
                termDisplay!!.cursorX = 0
                if (inputEditText!!.lineCount >= displayColumnSize) {
                    scrollDown()
                }

                if (termDisplay!!.cursorY + 1 < displayColumnSize) {

                    moveCursorY(1)
                }
            }
            isOverWriting = false
        }
    }

    companion object {

        private const val TAG = "debug***"

        private const val PREFS = "PREFS"
        private const val PREFS_NAME = "NAME"
        private const val PREFS_ADDRESS = "ADDR"
        private const val PREFS_AUTO_CONNECT = "AUTO"

        private const val REQ_CODE_SCAN_ACTIVITY = 1
        private const val REQ_CODE_ENABLE_BT = 2

        private const val CONNECT_TIME: Long = 5000 //タイムアウトする時間

        //通すActionを記述
        private fun bleServiceIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(MldpBluetoothService.ACTION_BLE_REQ_ENABLE_BT)
            intentFilter.addAction(MldpBluetoothService.ACTION_BLE_CONNECTED)
            intentFilter.addAction(MldpBluetoothService.ACTION_BLE_DISCONNECTED)
            intentFilter.addAction(MldpBluetoothService.ACTION_BLE_DATA_RECEIVED)
            return intentFilter
        }
    }
}
