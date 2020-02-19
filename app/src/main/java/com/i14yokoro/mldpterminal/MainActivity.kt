package com.i14yokoro.mldpterminal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.graphics.Point
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.i14yokoro.mldpterminal.bluetooth.MldpBluetoothScanActivity
import com.i14yokoro.mldpterminal.bluetooth.MldpBluetoothService
import com.i14yokoro.mldpterminal.terminalview.GestureListener
import com.i14yokoro.mldpterminal.terminalview.InputListener
import com.i14yokoro.mldpterminal.terminalview.TerminalView
import kotlin.experimental.and


/**
 * TODO 画面保持
 */
class MainActivity : AppCompatActivity(), InputListener, GestureListener {

    private lateinit var termView: TerminalView//ディスプレイ

    // 設定保存用
    private lateinit var prefs: SharedPreferences
    private var state = State.STARTING

    private var bleDeviceName: String = "\u0000"
    private var bleDeviceAddress: String = "\u0000" // 接続先の情報
    private lateinit var connectTimeoutHandler: Handler
    private var bleService: MldpBluetoothService? = null

    private var bleAutoConnect: Boolean = false //自動接続するか

    private var btnCtl = false              // CTLボタンを押したらtrue
    lateinit var termBuffer: TerminalBuffer

    private var isEscapeSequence = false    // エスケープシーケンスを受信するとtrue
    private var isReceiving = false

    private var isANSIEscapeSequence = false
    private var isTeCEscapeSequence = false

    private var stack = 0  // 処理待ちの文字数

    private var escapeString: StringBuilder  = StringBuilder()// 受信したエスケープシーケンスを格納

    private lateinit var buttonBar: LinearLayout
    private val anchor = IntArray(2)


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())

        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        wm.defaultDisplay.getSize(p)

        buttonBar = findViewById(R.id.linearLayout)

        buttonBar.getLocationOnScreen(anchor)
        termView = findViewById(R.id.main_display)
        termView.buttonBarBottom = anchor[1]
        // FIXME よくない
        termView.screenColumnSize = p.x
        termView.screenRowSize = p.y

        termBuffer = TerminalBuffer(termView.screenColumnSize, termView.screenRowSize)
        termView.termBuffer = termBuffer
        termView.escapeSequence = EscapeSequence(termBuffer)

        val metrics = resources.displayMetrics
        termView.setTitleBarSize(metrics.density)

        termView.setInputListener(this)
        termView.setGestureListener(this)
        termView.invalidate()

        state = State.STARTING
        connectTimeoutHandler = Handler()

        buttonBar = findViewById(R.id.linearLayout)

        findViewById<View>(R.id.btn_up).setOnClickListener {
            if (state == State.CONNECTED) {
                writeMLDP("\u001b" + "[A")
            }
        }

        findViewById<View>(R.id.btn_down).setOnClickListener {
            if (state == State.CONNECTED) {
                writeMLDP("\u001b" + "[B")
            }
        }

        findViewById<View>(R.id.btn_right).setOnClickListener {
            if (state == State.CONNECTED) {
                writeMLDP("\u001b" + "[C")
            }
        }
        findViewById<View>(R.id.btn_left).setOnClickListener {
            if (state == State.CONNECTED) {
                writeMLDP("\u001b" + "[D")
            }
        }

        findViewById<View>(R.id.btn_esc).setOnClickListener { if (state == State.CONNECTED) writeMLDP("\u001b") }
        findViewById<View>(R.id.btn_tab).setOnClickListener { if (state == State.CONNECTED) writeMLDP("\u0009") }
        findViewById<View>(R.id.btn_ctl).setOnClickListener { btnCtl = true }

        //SDK23以降はBLEをスキャンするのに位置情報が必要
        if (Build.VERSION.SDK_INT in 23..28) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        } else if(29 <= Build.VERSION.SDK_INT){
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }

        //自動接続
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        bleAutoConnect = prefs.getBoolean(PREFS_AUTO_CONNECT, false)
        if (bleAutoConnect) {
            bleDeviceName = prefs.getString(PREFS_NAME, "\u0000")!!
            bleDeviceAddress = prefs.getString(PREFS_ADDRESS, "\u0000")!!
        }

        termView.setOnKeyListener { _, i, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_DEL) {
                if (state == State.CONNECTED) {
                    writeMLDP("\u0008")
                } else {
                    termView.cursor.x--
                    termView.invalidate()
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
            menu.findItem(R.id.menu_connect).isVisible = true
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

    override fun onKey(text: Char) {
        if(isReceiving){
            return
        }
        var sendCtl = false
        val sendText = text.toString()

        if (state == State.CONNECTED) {
            if (btnCtl) {
                if (text in '\u0020'..'\u007f') {
                    val sendB = byteArrayOf((text.toByte() and 0x1f))
                    writeMLDP(sendB)
                    btnCtl = false
                    sendCtl = true
                }
                btnCtl = false
            }
            if (!sendCtl) {
                writeMLDP(sendText)
            }
            return
        }

        inputProcess(text)
    }

    override fun onDown() {
        if(termView.isFocusable) {
            showKeyboard()
        }
        buttonBar.getLocationOnScreen(anchor)
    }

    override fun onMove() {
        hideKeyboard()
    }

    private val bleServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MldpBluetoothService.ACTION_BLE_CONNECTED -> {
                    connectTimeoutHandler.removeCallbacks(abortConnection)
                    Log.i(TAG, "Received intent ACTION_BLE_CONNECTED")
                    state = State.CONNECTED
                    updateConnectionState()
                    Handler().postDelayed({  writeMLDP("MLDP\r\nApp:on\r\n") }, 500)
                }
                MldpBluetoothService.ACTION_BLE_DISCONNECTED -> {
                    Log.i(TAG, "Received intent ACTION_BLE_DISCONNECTED")
                    state = State.DISCONNECTED
                    updateConnectionState()
                }
                MldpBluetoothService.ACTION_BLE_DATA_RECEIVED -> {
                    val receivedData = intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_DATA)
                            ?: return

                    val splitData = receivedData.toCharArray()
                    isReceiving = true

                    // Log.e("Main", receivedData)
                    stack += splitData.size

                    for ((cnt, char) in splitData.withIndex()) {
                        when (char.toInt()) {
                            0x08  // KEY_BS
                            -> {
                                termView.cursor.x--
                                termView.invalidate()
                            }
                            0x09   // KEY_HT
                            -> {
                                if (termView.cursor.x + (4 - termView.cursor.x % 4) < termView.screenColumnSize) {
                                    termView.escapeSequence.moveRight(termView.cursor, 4 - termView.cursor.x % 4)
                                } else {
                                    termView.cursor.x += (termView.screenColumnSize-1)
                                }
                                termView.invalidate()
                            }
                            0x7f    // KEY_DEL
                            -> {
                            }
                            0x0a    // KEY_LF
                            -> {
                                inputProcess('\n')
                            }
                            0x0d    // KEY_CR
                            -> {
                                termView.cursor.x = 0
                            }
                            0x1b  // KEY_ESC
                            -> {
                                isEscapeSequence = true
                                escapeString.setLength(0)
                            }
                            else
                            -> {
                                if (isEscapeSequence) {
                                    escapeString.append(splitData[cnt])

                                    if(splitData[cnt] == '['){
                                        isANSIEscapeSequence = true
                                    } else if(splitData[cnt] == '?'){
                                        isTeCEscapeSequence = true
                                    } else if(isANSIEscapeSequence){
                                        val data = splitData[cnt].toString()
                                        if (data.matches("[A-HJKSTfm]".toRegex())) {
                                            checkANSIEscapeSequence()
                                            clearEscapeSequence()
                                        } else {
                                            if(!data.matches("[0-9;]".toRegex())){
                                                clearEscapeSequence()
                                            }
                                        }
                                    } else if(isTeCEscapeSequence){
                                        if (splitData[cnt].toString().matches("[s]".toRegex())) {
                                            checkTeCEscapeSequence()
                                            clearEscapeSequence()
                                        } else {
                                            // when receive number
                                            //if(!splitData[cnt].matches("[0-9;]".toRegex())){
                                            clearEscapeSequence()
                                            //}
                                        }
                                    } else {
                                        clearEscapeSequence()
                                    }

                                } else {
                                    if (splitData[cnt] == '\u0020') {
                                        splitData[cnt] = ' '
                                    }
                                    inputProcess(splitData[cnt])
                                }
                            }
                        }
                        stack--
                    }
                    isReceiving = false
                }
            }
        }
    }

    private fun clearEscapeSequence(){
        isEscapeSequence = false
        isTeCEscapeSequence = false
        isANSIEscapeSequence = false
        escapeString.clear()
    }

    // エスケープシーケンスの処理
    private fun checkANSIEscapeSequence() {
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

        termView.ansiEscapeSequence(mode, move, hMove)
        termView.invalidate()
    }

    // TODO Viewに移動
    // エスケープシーケンスの処理
    private fun checkTeCEscapeSequence() {
        val esStr = escapeString.toString()

        val length = esStr.length
        val mode = esStr[length - 1]

        if (mode == 's') {
            writeMLDP("\u001b?${termView.screenColumnSize},${termView.screenRowSize}s")
        } else {
            termView.tecEscapeSequence(mode)
        }
    }

    // 切断
    private val abortConnection = Runnable {
        bleService?.disconnect()
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

    // 接続状況
    private enum class State {
        STARTING, ENABLING, SCANNING, CONNECTING, CONNECTED, DISCONNECTED, DISCONNECTING
    } //state

    // 接続
    private fun connectWithAddress(address: String): Boolean {
        state = State.CONNECTING
        updateConnectionState()
        connectTimeoutHandler.postDelayed(abortConnection, CONNECT_TIME)
        return bleService!!.connect(address)
    }

    // MLDPに文字列を送信
    fun writeMLDP(data: Any) {
        bleService?.writeMLDP(data)
    }

    // 周りにあるBLEをスキャン
    private fun startScan() {
        bleService?.disconnect()
        state = State.DISCONNECTING
        updateConnectionState()

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
                    //printNotSendingText(LF + "connect to " + bleDeviceName)
                }
                State.DISCONNECTING -> {
                    //printNotSendingText(LF + "disconnected from " + bleDeviceName)
                }
                State.CONNECTING -> {}
            }

            invalidateOptionsMenu()
        }
    }

    // 新しい行を追加
    private fun printNotSendingText(text: String) {
        for (element in text) {
            inputProcess(element)
        }
        inputProcess(LF)
    }

    // キーボードを表示させる
    private fun showKeyboard() {
        buttonBar.getLocationOnScreen(anchor)

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        termView.isFocusable = true
        termView.isFocusableInTouchMode = true
        termView.requestFocus()
        imm.showSoftInput(termView, InputMethodManager.SHOW_IMPLICIT)

        buttonBar.getLocationOnScreen(anchor)
    }

    //　キーボードを隠す
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(termView.windowToken, 0)
    }


    private fun resize(newRowSize: Int, newColumnSize: Int) {
        termBuffer.resize(newRowSize, newColumnSize)
    }

    // strをリストに格納
    private fun inputProcess(input: Char) {
        if ((input in '\u0020'..'\u007f') || input == '\u000a' || input == '\u000d'){

            termBuffer.topRow = if(termBuffer.totalLines < termBuffer.screenRowSize) {
                0
            } else {
                termBuffer.totalLines - termBuffer.screenRowSize
            }

            val oldX = termView.cursor.x
            // input
            if (input != LF) {
                // 上書き
                termBuffer.setText(termView.cursor.x, termView.getCurrentRow(), input)
                termBuffer.setColor(termView.cursor.x, termView.getCurrentRow(), termBuffer.charColor)
                termView.cursor.x++
            }
            // LFか右端での入力があったときの時
            if (input == LF || oldX+1 == termBuffer.screenColumnSize) {

                if (termView.getCurrentRow() + 1 == termBuffer.totalLines) {
                    termBuffer.addRow()
                }
                termView.cursor.x = 0
                termView.cursor.y++
            }

            termView.invalidate()
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
