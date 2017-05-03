package com.stockholm.hackathonb.lcd;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Son Bui on 12/28/2016.
 */
public class LiquidCrystal implements AutoCloseable {
    // commands
    private static final int LCD_CLEARDISPLAY = 0x01;
    private static final int LCD_RETURNHOME = 0x02;
    private static final int LCD_ENTRYMODESET = 0x04;
    private static final int LCD_DISPLAYCONTROL = 0x08;
    private static final int LCD_CURSORSHIFT = 0x10;
    private static final int LCD_FUNCTIONSET = 0x20;
    private static final int LCD_SETCGRAMADDR = 0x40;
    private static final int LCD_SETDDRAMADDR = 0x80;

    // flags for display entry mode
    private static final int LCD_ENTRYRIGHT = 0x00;
    private static final int LCD_ENTRYLEFT = 0x02;
    private static final int LCD_ENTRYSHIFTINCREMENT = 0x01;
    private static final int LCD_ENTRYSHIFTDECREMENT = 0x00;

    // flags for display on/off control
    private static final int LCD_DISPLAYON = 0x04;
    private static final int LCD_DISPLAYOFF = 0x00;
    private static final int LCD_CURSORON = 0x02;
    private static final int LCD_CURSOROFF = 0x00;
    private static final int LCD_BLINKON = 0x01;
    private static final int LCD_BLINKOFF = 0x00;
    private static final int LCD_BLINKCURSOR = 0x0F;
    private static final int LCD_BLINKCURSOROFF = 0x0C;

    // flags for display/cursor shift
    private static final int LCD_DISPLAYMOVE = 0x08;
    private static final int LCD_CURSORMOVE = 0x00;
    private static final int LCD_MOVERIGHT = 0x04;
    private static final int LCD_MOVELEFT = 0x00;

    // flags for function set
    private static final int LCD_8BITMODE = 0x10;
    private static final int LCD_4BITMODE = 0x00;
    private static final int LCD_2LINE = 0x08;
    private static final int LCD_1LINE = 0x00;
    private static final int LCD_5x10DOTS = 0x04;
    private static final int LCD_5x8DOTS = 0x00;
    //
    private String _rs_pin; // LOW: command.  HIGH: character.
    private String _rw_pin; // LOW: write to LCD.  HIGH: read from LCD.
    private String _enable_pin; // activated by a HIGH pulse.
    private String[] _data_pins = new String[8];

    private int _displayfunction;
    private int _displaycontrol;
    private int _displaymode;

    private int _initialized;

    private int _numlines;
    private int[] _row_offsets = new int[4];

    private PeripheralManagerService manager = new PeripheralManagerService();
    private Map<String, Gpio> gpioPin = new HashMap<>();
    private static final int INPUT = 0x10;
    private static final int INPUT_PULLUP = 0x11;
    private static final int INPUT_PULLDOWN = 0x12;
    private static final int OUTPUT = 0x00;
    private static final int OUTPUT_PULLUP = 0x01;
    private static final int OUTPUT_PULLDOWN = 0x02;
    private boolean LOW = false;
    private boolean HIGH = true;
    private boolean enableSleep = false;
    private int lastLine = 0;
    private int lastPos = 0;

    public LiquidCrystal(String rs, String rw, String enable,
                         String d0, String d1, String d2, String d3,
                         String d4, String d5, String d6, String d7) {
        try {
            init(0, rs, rw, enable, d0, d1, d2, d3, d4, d5, d6, d7);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LiquidCrystal(String rs, String enable,
                         String d0, String d1, String d2, String d3,
                         String d4, String d5, String d6, String d7) {
        try {
            init(0, rs, null, enable, d0, d1, d2, d3, d4, d5, d6, d7);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LiquidCrystal(String rs, String rw, String enable,
                         String d0, String d1, String d2, String d3) {
        try {
            init(1, rs, rw, enable, d0, d1, d2, d3, null, null, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LiquidCrystal(String rs, String enable,
                         String d0, String d1, String d2, String d3) {
        try {
            init(1, rs, null, enable, d0, d1, d2, d3, null, null, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pinMode(String pinKey, int mode) throws IOException {
        if (gpioPin.get(pinKey) == null) {
            Gpio pin = manager.openGpio(pinKey);
            if ((mode == OUTPUT) || (mode == OUTPUT_PULLDOWN)) {
                pin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                pin.setActiveType(Gpio.ACTIVE_HIGH);
                LOW = false;
                HIGH = true;
            }
            if (mode == OUTPUT_PULLUP) {
                pin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                pin.setActiveType(Gpio.ACTIVE_LOW);
                LOW = true;
                HIGH = false;
            } else if ((mode == INPUT) || (mode == INPUT_PULLUP)) {
                pin.setDirection(Gpio.DIRECTION_IN);
                pin.setActiveType(Gpio.ACTIVE_LOW);
                LOW = true;
                HIGH = false;
            } else if (mode == INPUT_PULLDOWN) {
                pin.setDirection(Gpio.DIRECTION_IN);
                pin.setActiveType(Gpio.ACTIVE_HIGH);
                LOW = false;
                HIGH = true;
            }
            gpioPin.put(pinKey, pin);
        }
    }

    private void digitalWrite(String pin, boolean isActive) throws IOException {
        if (gpioPin.get(pin) != null)
            gpioPin.get(pin).setValue(isActive);
    }

    private void delayMicroseconds(long us) {
        long ms = 1;
        if (us > 1000)
            ms = Math.round(us / 1000);
        if (enableSleep) SystemClock.sleep(ms);

    }

    public void init(int fourbitmode, String rs, String rw, String enable,
                     String d0, String d1, String d2, String d3,
                     String d4, String d5, String d6, String d7) throws IOException {
        _rs_pin = rs;
        if (!TextUtils.isEmpty(rw)) _rw_pin = rw;
        _enable_pin = enable;

        _data_pins[0] = d0;
        _data_pins[1] = d1;
        _data_pins[2] = d2;
        _data_pins[3] = d3;
        if (!TextUtils.isEmpty(d4)) _data_pins[4] = d4;
        if (!TextUtils.isEmpty(d5)) _data_pins[5] = d5;
        if (!TextUtils.isEmpty(d6)) _data_pins[6] = d6;
        if (!TextUtils.isEmpty(d7)) _data_pins[7] = d7;

        if (fourbitmode != 0)
            _displayfunction = LCD_4BITMODE | LCD_1LINE | LCD_5x8DOTS;
        else
            _displayfunction = LCD_8BITMODE | LCD_1LINE | LCD_5x8DOTS;

        begin(16, 1);
    }


    public void begin(int cols, int lines) throws IOException {
        begin(cols, lines, LCD_5x8DOTS);
    }
    public void begin(int cols, int lines, int dotsize) throws IOException {
        if (lines > 1) {
            _displayfunction |= LCD_2LINE;
        }
        _numlines = lines;

        setRowOffsets(0x00, 0x40, 0x00 + cols, 0x40 + cols);

        // for some 1 line displays you can select a 10 pixel high font
        if ((dotsize != LCD_5x8DOTS) && (lines == 1)) {
            _displayfunction |= LCD_5x10DOTS;
        }

        pinMode(_rs_pin, OUTPUT);
        // we can save 1 pin by not using RW. Indicate by passing 255 instead of pin#
        if (!TextUtils.isEmpty(_rw_pin)) {
            pinMode(_rw_pin, OUTPUT);
        }
        pinMode(_enable_pin, OUTPUT);

        // Do these once, instead of every time a character is drawn for speed reasons.
        for (int i = 0; i < (((_displayfunction & LCD_8BITMODE) != 0) ? 8 : 4); ++i) {
            pinMode(_data_pins[i], OUTPUT);
        }

        // SEE PAGE 45/46 FOR INITIALIZATION SPECIFICATION!
        // according to datasheet, we need at least 40ms after power rises above 2.7V
        // before sending commands. Arduino can turn on way before 4.5V so we'll wait 50
        delayMicroseconds(50000);
        // Now we pull both RS and R/W low to begin commands
        digitalWrite(_rs_pin, LOW);
        digitalWrite(_enable_pin, LOW);
        if (!TextUtils.isEmpty(_rw_pin)) {
            digitalWrite(_rw_pin, LOW);
        }
//put the LCD into 4 bit or 8 bit mode
        //if (!(_displayfunction & LCD_8BITMODE)) {
        if ((_displayfunction & LCD_8BITMODE) == 0) {
            // this is according to the hitachi HD44780 datasheet
            // figure 24, pg 46

            // we start in 8bit mode, try to set 4 bit mode
            write4bits(0x03);
            delayMicroseconds(4500); // wait min 4.1ms

            // second try
            write4bits(0x03);
            delayMicroseconds(4500); // wait min 4.1ms

            // third go!
            write4bits(0x03);
            delayMicroseconds(150);

            // finally, set to 4-bit interface
            write4bits(0x02);
        } else {
            // this is according to the hitachi HD44780 datasheet
            // page 45 figure 23

            // Send function set command sequence
            command(LCD_FUNCTIONSET | _displayfunction);
            delayMicroseconds(4500);  // wait more than 4.1ms

// second try
            command(LCD_FUNCTIONSET | _displayfunction);
            delayMicroseconds(150);

            // third go
            command(LCD_FUNCTIONSET | _displayfunction);
        }

        // finally, set # lines, font size, etc.
        command(LCD_FUNCTIONSET | _displayfunction);

        // turn the display on with no cursor or blinking default
        _displaycontrol = LCD_DISPLAYON | LCD_CURSOROFF | LCD_BLINKOFF;
        display();

        // clear it off
        clear();

        // Initialize to default text direction (for romance languages)
        _displaymode = LCD_ENTRYLEFT | LCD_ENTRYSHIFTDECREMENT;
        // set the entry mode
        command(LCD_ENTRYMODESET | _displaymode);

    }private void setRowOffsets(int row0, int row1, int row2, int row3) {
        _row_offsets[0] = row0;
        _row_offsets[1] = row1;
        _row_offsets[2] = row2;
        _row_offsets[3] = row3;
    }

    public void print(String message) throws IOException {
        for (int i = 0; i < message.length(); i++) {
            //  write(message.charAt(i));
            putChar(message.charAt(i));
        }
    }

    public void clear() throws IOException {
        lastPos = 0;
        lastLine = 0;
        command(LCD_CLEARDISPLAY);  // clear display, set cursor position to zero
        delayMicroseconds(2000);  // this command takes a long time!
    }

    public void home() throws IOException {
        command(LCD_RETURNHOME);  // set cursor position to zero
        delayMicroseconds(2000);  // this command takes a long time!
    }
    public void setCursor(int col, int row) throws IOException {

//        const size_t max_lines = sizeof(_row_offsets) / sizeof(*_row_offsets);
//        if (row >= max_lines) {
//            row = max_lines - 1;    // we count rows starting w/0
//        }
        if (row >= _numlines) {
            row = _numlines - 1;    // we count rows starting w/0
        }
        lastPos = col;
        lastLine = row;
        command(LCD_SETDDRAMADDR | (col + _row_offsets[row]));
    }

    public void noDisplay() throws IOException {
        _displaycontrol &= ~LCD_DISPLAYON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public void display() throws IOException {
        _displaycontrol |= LCD_DISPLAYON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public void noCursor() throws IOException {
        _displaycontrol &= ~LCD_CURSORON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }
    public void cursor() throws IOException {
        _displaycontrol |= LCD_CURSORON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public void binkCursorOn() throws IOException {
        _displaycontrol |= LCD_BLINKCURSOR;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public void binkCursorOff() throws IOException {
        _displaycontrol |= LCD_BLINKCURSOROFF;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public void noBlink() throws IOException {
        _displaycontrol &= ~LCD_BLINKON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public void blink() throws IOException {
        _displaycontrol |= LCD_BLINKON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public void scrollDisplayLeft() throws IOException {
        command(LCD_CURSORSHIFT | LCD_DISPLAYMOVE | LCD_MOVELEFT);
    }

    public void scrollDisplayRight() throws IOException {
        command(LCD_CURSORSHIFT | LCD_DISPLAYMOVE | LCD_MOVERIGHT);
    }

    public void leftToRight() throws IOException {
        _displaymode |= LCD_ENTRYLEFT;
        command(LCD_ENTRYMODESET | _displaymode);
    }

    public void rightToLeft() throws IOException {
        _displaymode &= ~LCD_ENTRYLEFT;
        command(LCD_ENTRYMODESET | _displaymode);
    }

    public void autoscroll() throws IOException {
        _displaymode |= LCD_ENTRYSHIFTINCREMENT;
        command(LCD_ENTRYMODESET | _displaymode);
    }

    public void noAutoscroll() throws IOException {
        _displaymode &= ~LCD_ENTRYSHIFTINCREMENT;
        command(LCD_ENTRYMODESET | _displaymode);
    }

    public void createChar(int location, int charmap[]) throws IOException {
        location &= 0x7; // we only have 8 locations 0-7
        command(LCD_SETCGRAMADDR | (location << 3));
        for (int i = 0; i < 8; i++) {
            write(charmap[i]);
        }
    }

    public void command(int value) throws IOException {
        send(value, LOW);
    }

    public long write(int value) throws IOException {
        send(value, HIGH);
        lastPos++;
        return 1; // assume sucess
    }

    public void putChar(int data) throws IOException {
        switch (data) {
            case 'r': //Goto currenr Line
                setCursor(0, lastLine);
                System.out.println("lastLine : " + lastLine);
                break;

            case 'f':
                clear();
                break;
            case 'n':
                lastLine++;
                setCursor(0, lastLine);
                System.out.println("lastLine : " + lastLine);
                break;
            case 'b':
                command(0x10);
                lastPos--;
                System.out.println("lastPos : " + lastPos);
                break;
            default:
                write(data);
                break;


        }
    }

    private void send(int value, boolean mode) throws IOException {
        digitalWrite(_rs_pin, mode);

        // if there is a RW pin indicated, set it low to Write
        if (!TextUtils.isEmpty(_rw_pin)) {
            digitalWrite(_rw_pin, LOW);
        }

        if ((_displayfunction & LCD_8BITMODE) != 0) {
            write8bits(value);
        } else {
            write4bits(value >> 4);
            write4bits(value);
        }
    }

    private void pulseEnable() throws IOException {
        digitalWrite(_enable_pin, LOW);
        delayMicroseconds(1);
        digitalWrite(_enable_pin, HIGH);
        delayMicroseconds(1);    // enable pulse must be >450ns
        digitalWrite(_enable_pin, LOW);
        delayMicroseconds(100);   // commands need > 37us to settle
    }
    private void write4bits(int value) throws IOException {
        for (int i = 0; i < 4; i++) {
            digitalWrite(_data_pins[i], ((value >> i) & 0x01) != 0);
        }

        pulseEnable();
    }

    private void write8bits(int value) throws IOException {
        for (int i = 0; i < 8; i++) {
            digitalWrite(_data_pins[i], ((value >> i) & 0x01) != 0);
        }

        pulseEnable();
    }

    @Override

    public void close() throws Exception {
        try {
            for (String key : gpioPin.keySet()) {
                System.out.println("pin : " + key);
                gpioPin.get(key).close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            gpioPin.clear();
        }


    }
}

