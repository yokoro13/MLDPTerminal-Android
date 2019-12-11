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
import android.graphics.Point
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.StrictMode
import android.support.v4.text.HtmlCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

import com.i14yokoro.mldpterminal.bluetooth.MldpBluetoothScanActivity
import com.i14yokoro.mldpterminal.bluetooth.MldpBluetoothService

import java.nio.charset.StandardCharsets
import kotlin.experimental.and
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var inputEditText: EditText//ディスプレイのEditText

    // 設定保存用
    private lateinit var prefs: SharedPreferences
    private var state = State.STARTING

    private var bleDeviceName: String = "\u0000"
    private var bleDeviceAddress: String = "\u0000" // 接続先の情報
    private lateinit var connectTimeoutHandler: Handler
    private var bleService: MldpBluetoothService? = null

    private var bleAutoConnect: Boolean = false //自動接続するか

    private lateinit var escapeSequence: EscapeSequence
    private lateinit var termBuffer: TerminalBuffer

    private var eStart: Int = 0
    private var eCount: Int = 0

    private lateinit var escapeString: StringBuilder // 受信したエスケープシーケンスを格納

    private var screenRowSize: Int = 0
    private var screenColumnSize: Int = 0

    private var isMovingCursor = false      // カーソル移動中ならtrue
    private var btnCtl = false              // CTLボタンを押したらtrue
    private var isNotSending = false        // RN側に送りたくないものがあるときはfalseにする
    private var isDisplaying = false        // 画面更新中はtrue
    private var isSending = false           // RNにデータを送信しているときtrue
    private var isEscapeSequence = false    // エスケープシーケンスを受信するとtrue

    private var stack = 0  // 処理待ちの文字数

    private val handler = Handler()
    private val time = 3 //

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
            var sendCtl = false

            if (state == State.CONNECTED && count > before) {
                if (!isNotSending) {
                    val send = s.subSequence(start + before, start + count).toString()
                    isSending = true
                    if (btnCtl) {
                        if (send.matches("[\\x5f-\\x7e]".toRegex())) {
                            val sendB = byteArrayOf((send.toByteArray()[0] and 0x1f))
                            bleService!!.writeMLDP(sendB)
                            btnCtl = false
                            sendCtl = true
                        }
                        btnCtl = false
                    }
                    if (!sendCtl) {
                        bleService!!.writeMLDP(send)
                    }
                }
            }
        }

        override fun afterTextChanged(s: Editable) {
            if (s.isEmpty()){
                return
            }
            val str = s.subSequence(eStart, eStart + eCount).toString()//入力文字

            handler.removeCallbacks(updateDisplay)
            if (!isSending) {
                if (!isDisplaying) {
                    inputProcess(str)
                    handler.postDelayed(updateDisplay, time.toLong())
                }
            } else {
                isSending = false
            }

        }
    }

    private val bleServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MldpBluetoothService.ACTION_BLE_CONNECTED -> {
                    connectTimeoutHandler.removeCallbacks(abortConnection)
                    Log.i(TAG, "Received intent ACTION_BLE_CONNECTED")
                    state = State.CONNECTED
                    updateConnectionState()
                    Handler().postDelayed({  bleService!!.writeMLDP("MLDP\r\nApp:on\r\n") }, 500)
                }
                MldpBluetoothService.ACTION_BLE_DISCONNECTED -> {
                    Log.i(TAG, "Received intent ACTION_BLE_DISCONNECTED")
                    state = State.DISCONNECTED
                    updateConnectionState()
                }
                MldpBluetoothService.ACTION_BLE_DATA_RECEIVED -> {
                    val receivedData = intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_DATA)
                            ?: return
                    var cnt = 1
                    Log.d("debug****", "receivedData$receivedData")

                    val splitData = receivedData.split("".toRegex()).toTypedArray()

                    handler.removeCallbacks(updateDisplay)
                    stack += splitData.size - 2
                    Log.d("stack***", "stackLength$stack")
                    val utfArray = receivedData.toByteArray(StandardCharsets.UTF_8)

                    for (charCode in utfArray) {
                        when (charCode) {
                            0x08.toByte()    // KEY_BS
                            -> termBuffer.cursorX--
                            0x09.toByte()    // KEY_HT
                            -> if (termBuffer.cursorX + (8 - termBuffer.cursorX % 8) < screenRowSize) {
                                escapeSequence.moveRight(8 - termBuffer.cursorX % 8)
                            } else {
                                termBuffer.cursorX += (screenRowSize-1)
                            }
                            0x7f.toByte()    // KEY_DEL
                            -> {
                            }
                            0x0a.toByte()    // KEY_LF
                            -> {
                                isNotSending = true
                                inputProcess("\n")
                                isNotSending = false
                            }
                            0x0d.toByte()    // KEY_CR
                            -> {
                                termBuffer.cursorX = 0
                                moveToSavedCursor()
                            }
                            0x1b.toByte()   // KEY_ESC
                            -> {
                                isEscapeSequence = true
                                escapeString.setLength(0)
                            }
                            else -> if (isEscapeSequence) {
                                escapeString.append(splitData[cnt])
                                if (splitData[cnt].matches("[A-HJKSTZfm]".toRegex())) {
                                    checkEscapeSequence()
                                    isEscapeSequence = false
                                }
                            } else {
                                if (cnt <= receivedData.length) {
                                    if (splitData[cnt] == "\u0020") {
                                        splitData[cnt] = " "
                                    }
                                    isNotSending = true
                                    inputProcess(splitData[cnt])
                                    isNotSending = false
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

    // エスケープシーケンスの処理
    private fun checkEscapeSequence() {
        val esStr = escapeString.toString()

        val length = esStr.length
        val mode = esStr[length - 1]
        var move = 1
        var hMove = 1
        val semicolonPos: Int

        if (length != 2) {
            if (mode != 'H') {
                move = Integer.parseInt(esStr.substring(1, length - 1))
            } else {
                semicolonPos = esStr.indexOf(";")
                if (semicolonPos != 1) {
                    move = Integer.parseInt(esStr.substring(1, semicolonPos))
                }
                if (esStr[semicolonPos + 1] != 'H' || esStr[semicolonPos + 1] != 'f') {
                    hMove = Integer.parseInt(esStr.substring(semicolonPos + 1, length - 1))
                }
            }
        }

        when (mode) {
            'A' -> escapeSequence.moveUp(move)
            'B' -> escapeSequence.moveDown(move)
            'C' -> escapeSequence.moveRight(move)
            'D' -> escapeSequence.moveLeft(move)
            'E' -> escapeSequence.moveDownToRowLead(move)
            'F' -> escapeSequence.moveUpToRowLead(move)
            'G' -> escapeSequence.moveCursor(move-1)
            'H', 'f' -> escapeSequence.moveCursor(move-1, hMove-1)
            'J' -> escapeSequence.clearDisplay(move)
            'K' -> escapeSequence.clearRow(move)
            'S' -> escapeSequence.scrollNext(move)
            'T' -> escapeSequence.scrollBack(move)
            'Z' -> {
                isSending = true
                bleService!!.writeMLDP(maxRowLength.toString())
                bleService!!.writeMLDP(maxColumnLength.toString())
            }
            'm' -> {
                escapeSequence.selectGraphicRendition(move)
                inputEditText.setTextColor(termBuffer.charColor)
            }
            else -> {
            }
        }
        escapeString.clear()
    }

    // 切断
    private val abortConnection = Runnable {
        if (state == State.CONNECTING) {
            bleService!!.disconnect()
        }
    }

    // bluetoothのserviceを使う
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

    // １行に収まる文字数を返す
    private val maxRowLength: Int
        get() {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val p = Point()
            wm.defaultDisplay.getSize(p)

            return p.x / textWidth
        }

    // １列に収まる文字数を返す
    private val maxColumnLength: Int
        get() {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val p = Point()
            wm.defaultDisplay.getSize(p)

            val height = p.y - 100
            val text = textHeight.toInt()

            return height / text - 1
        }

    // テキストの文字の横幅を返す
    private// TypefaceがMonospace 「" "」の幅を取得
    val textWidth: Int
        get() {
            val paint = Paint()
            paint.textSize = inputEditText.textSize
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            return paint.measureText(" ").toInt()
        }

    // テキストの文字の高さを返す
    private val textHeight: Float
        get() {
            val paint = Paint()
            paint.textSize = inputEditText.textSize
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            val fontMetrics = paint.fontMetrics
            return abs(fontMetrics.top) + abs(fontMetrics.bottom)
        }

    // 選択中の行番号を返す
    private val currentRow: Int
        get() = termBuffer.currentRow

    // 現在の行のテキストを返す
    private val currentRowText: String
        get() = termBuffer.getRowText(currentRow)

    // 画面更新を非同期で行う
    private val updateDisplay = {
        display()
        moveToSavedCursor()
    }

    // 接続状況
    private enum class State {
        STARTING, ENABLING, SCANNING, CONNECTING, CONNECTED, DISCONNECTED, DISCONNECTING
    } //state

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputEditText = findViewById(R.id.main_display)
        inputEditText.customSelectionActionModeCallback = mActionModeCallback
        inputEditText.addTextChangedListener(mInputTextWatcher)
        inputEditText.setTextIsSelectable(false)

        screenRowSize = maxRowLength
        screenColumnSize = maxColumnLength
        termBuffer = TerminalBuffer(screenRowSize, screenColumnSize)
        escapeSequence = EscapeSequence(termBuffer)

        escapeString = StringBuilder()
        state = State.STARTING
        connectTimeoutHandler = Handler()


        findViewById<View>(R.id.btn_up).setOnClickListener {
            if (state == State.CONNECTED) {
                bleService!!.writeMLDP("\u001b" + "[A")
            } else {
                escapeSequence.moveUp(1)
            }
            moveToSavedCursor()
        }

        findViewById<View>(R.id.btn_down).setOnClickListener {
            if (state == State.CONNECTED) {
                bleService!!.writeMLDP("\u001b" + "[B")
            } else {
                escapeSequence.moveDown(1)
            }
            moveToSavedCursor()
        }

        findViewById<View>(R.id.btn_right).setOnClickListener {
            if (state == State.CONNECTED) {
                bleService!!.writeMLDP("\u001b" + "[C")
            } else {
                escapeSequence.moveRight(1)
            }
            moveToSavedCursor()
        }
        findViewById<View>(R.id.btn_left).setOnClickListener {
            if (state == State.CONNECTED) {
                bleService!!.writeMLDP("\u001b" + "[D")
            } else {
                escapeSequence.moveLeft(1)
            }
            moveToSavedCursor()
        }

        findViewById<View>(R.id.btn_esc).setOnClickListener { if (state == State.CONNECTED) bleService!!.writeMLDP("\u001b") }
        findViewById<View>(R.id.btn_tab).setOnClickListener { if (state == State.CONNECTED) bleService!!.writeMLDP("\u0009") }
        findViewById<View>(R.id.btn_ctl).setOnClickListener { btnCtl = true }

        //SDK23以降はBLEをスキャンするのに位置情報が必要
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }

        //自動接続
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        bleAutoConnect = prefs.getBoolean(PREFS_AUTO_CONNECT, false)
        if (bleAutoConnect) {
            bleDeviceName = prefs.getString(PREFS_NAME, "\u0000")
            bleDeviceAddress = prefs.getString(PREFS_ADDRESS, "\u0000")
        }

        //画面タッチされた時のイベント
        inputEditText.setOnTouchListener(object : View.OnTouchListener {
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
                    else -> {
                    }
                }
                return false
            }
        })

        inputEditText.setOnKeyListener { _, i, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_DEL) {
                if (state == State.CONNECTED) {
                    bleService!!.writeMLDP("\u0008")
                } else {
                    termBuffer.cursorX--
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
        val editor = prefs.edit()
        editor.clear()
        if (bleAutoConnect) {
            editor.putString(PREFS_NAME, bleDeviceName)
            editor.putString(PREFS_ADDRESS, bleDeviceAddress)
        }
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
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
            if (bleDeviceAddress != "\u0000") {
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
                connectWithAddress(bleDeviceAddress)
                return true
            }

            R.id.menu_disconnect -> {
                state = State.DISCONNECTING
                updateConnectionState()
                bleService!!.disconnect()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // 接続
    private fun connectWithAddress(address: String): Boolean {
        state = State.CONNECTING
        updateConnectionState()
        connectTimeoutHandler.postDelayed(abortConnection, CONNECT_TIME)
        return bleService!!.connect(address)
    }

    // 周りにあるBLEをスキャン
    private fun startScan() {
        if (bleService != null) {
            bleService!!.disconnect()
            state = State.DISCONNECTING
            updateConnectionState()
        }

        val bleServiceIntent = Intent(this@MainActivity, MldpBluetoothService::class.java)
        this.bindService(bleServiceIntent, bleServiceConnection, Context.BIND_AUTO_CREATE)

        val bleScanActivityIntent = Intent(this@MainActivity, MldpBluetoothScanActivity::class.java)
        startActivityForResult(bleScanActivityIntent, REQ_CODE_SCAN_ACTIVITY)
    }

    // bluetoothの接続状況を更新
    private fun updateConnectionState() {
        runOnUiThread {
            when (state) {
                State.STARTING, State.ENABLING, State.SCANNING, State.DISCONNECTED -> {
                    stack = 0
                }
                State.CONNECTED -> {
                    printNotSendingText(LF + "connect to " + bleDeviceName)
                }
                State.DISCONNECTING -> {
                    printNotSendingText(LF + "disconnected from " + bleDeviceName)
                }
                State.CONNECTING -> {}
            }

            invalidateOptionsMenu()
        }
    }

    // 別Activityからの処理結果をうけとる
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQ_CODE_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (!bleAutoConnect || bleDeviceAddress == "\u0000") {
                    startScan()
                }
            }
            return
        } else if (requestCode == REQ_CODE_SCAN_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                bleDeviceAddress = intent!!.getStringExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_ADDRESS)
                bleDeviceName = intent.getStringExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_NAME)
                bleAutoConnect = intent.getBooleanExtra(MldpBluetoothScanActivity.INTENT_EXTRA_SCAN_AUTO_CONNECT, false)
                if (bleDeviceAddress == "\u0000") {
                    state = State.DISCONNECTED
                    updateConnectionState()
                } else {
                    state = State.CONNECTING
                    updateConnectionState()
                    if (!connectWithAddress(bleDeviceAddress)) {
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

    // 新しい行を追加
    private fun printNotSendingText(text: String) {
        isNotSending = true
        for (element in text) {
            inputEditText.append(element.toString())
        }
        inputEditText.append("\n")
        termBuffer.cursorX = 0
        isNotSending = false
    }

    // キーボードを表示させる
    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = currentFocus
        if (v != null)
            imm.showSoftInput(v, 0)
    }

    //　キーボードを隠す
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = currentFocus
        if (v != null)
            imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    // ディスプレイに文字を表示する
    private fun display() {
        isDisplaying = true
        isNotSending = true

        inputEditText.text.clear()

        if (!termBuffer.isColorChange) {
            inputEditText.append(termBuffer.makeScreenString())
        } else {
            inputEditText.append(HtmlCompat.fromHtml(termBuffer.makeScreenString(), HtmlCompat.FROM_HTML_MODE_COMPACT))
        }

        isDisplaying = false
        isNotSending = false
    }

    // カーソルを保持している座標に移動させる
    private fun moveToSavedCursor() {
        if (!isMovingCursor) {
            isMovingCursor = true

            val cursor: Int = termBuffer.cursorY * termBuffer.screenRowSize + termBuffer.cursorX

            Log.d("debug***", "cursor$cursor")
            if (cursor >= 0) {
                inputEditText.setSelection(cursor)
            }
            isMovingCursor = false
        }
    }

    // 画面を上にスクロールする
    private fun scrollUp() {
        if (termBuffer.totalColumns > screenColumnSize) {
            //表示する一番上の行を１つ上に
            termBuffer.moveTopRow(-1)
            // カーソルが画面内にある
            if (cursorIsInScreen()) {
                setEditable(true)
                termBuffer.cursorY = termBuffer.currentRow - termBuffer.topRow
            } else { //画面外
                setEditable(false)
            }
            if (stack == 0) {
                display()
                moveToSavedCursor()
            }
        }
    }

    // 画面を下にスクロールする
    private fun scrollDown() {
        if (termBuffer.totalColumns > screenColumnSize) {
            // 一番下の行までしか表示させない
            if (termBuffer.topRow + screenColumnSize < termBuffer.totalColumns) {
                //表示する一番上の行を１つ下に
                termBuffer.moveTopRow(1)
                if (cursorIsInScreen()) {
                    setEditable(true)
                    termBuffer.cursorY = termBuffer.currentRow - termBuffer.topRow
                } else {
                    setEditable(false)
                }
                if (stack == 0) {
                    display()
                    moveToSavedCursor()
                }
            }
        }
    }

    private fun cursorIsInScreen(): Boolean{
        return (termBuffer.topRow <= termBuffer.currentRow && termBuffer.currentRow <= termBuffer.topRow + screenColumnSize - 1)
    }

    // 画面の編集許可
    private fun setEditable(editable: Boolean) {
        if (editable) {
            focusable()
            termBuffer.isOutOfScreen = false
        } else {
            inputEditText.isFocusable = false
            termBuffer.isOutOfScreen = true
        }
    }

    private fun focusable() {
        inputEditText.isFocusable = true
        inputEditText.isFocusableInTouchMode = true
        inputEditText.requestFocus()

    }

    private fun resize(newRowSize: Int, newColumnSize: Int) {
        termBuffer.screenRowSize = newRowSize
        termBuffer.screenColumnSize = newColumnSize
        termBuffer.resize()
    }

    // strをリストに格納
    private fun inputProcess(str: String) {
        if (str.matches("[\\x20-\\x7f\\x0a\\x0d]".toRegex())) {

            // カーソルが画面外で入力があると入力位置に移動
            if (termBuffer.isOutOfScreen) {
                focusable()
                termBuffer.topRow = termBuffer.currentRow - termBuffer.cursorY
                moveToSavedCursor()
            }

            // 入力行の長さを超えた場所は空白をつめる
            if (termBuffer.cursorX >= currentRowText.length) {
                for (x in currentRowText.length until termBuffer.cursorX){
                    termBuffer.setText(termBuffer.cursorX, currentRow, ' ')
                    termBuffer.setColor(termBuffer.cursorX, currentRow, termBuffer.charColor)
                }
            }

            // input
            val inputStr = str[0]

            if (inputStr != LF) {
                // 上書き
                termBuffer.setText(termBuffer.cursorX, currentRow, inputStr)
                termBuffer.setColor(termBuffer.cursorX, currentRow, termBuffer.charColor)
                termBuffer.cursorX++
            }
            
            // LFか右端での入力があったときの時
            if (inputStr == LF || currentRowText.length >= screenRowSize) {
                termBuffer.cursorX = 0
                termBuffer.incrementCurrentRow()

                if(currentRow == termBuffer.totalColumns){
                    termBuffer.addRow()
                }

                // スクロールの処理
                if (termBuffer.cursorY + 1 == screenColumnSize) {
                    scrollDown()
                }
                termBuffer.cursorY++

            }
        }
    }

    companion object {

        private const val LF = '\n'
        private const val TAG = "debug***"
        private const val PREFS = "PREFS"
        private const val PREFS_NAME = "NAME"
        private const val PREFS_ADDRESS = "ADDR"
        private const val PREFS_AUTO_CONNECT = "AUTO"

        // BLE
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
