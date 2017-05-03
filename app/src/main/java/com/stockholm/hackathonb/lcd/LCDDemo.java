package com.stockholm.hackathonb.lcd;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;

/**
 * Created by Son Bui on 12/28/2016.
 */

public class LCDDemo extends Activity {
    private static final String TAG = LCDDemo.class.getSimpleName();
    private LiquidCrystal mLcd;

    private static final String GPIO_LCD_RS = "BCM19";
    private static final String GPIO_LCD_EN = "BCM26";

    private static final String GPIO_LCD_D4 = "BCM21";
    private static final String GPIO_LCD_D5 = "BCM20";
    private static final String GPIO_LCD_D6 = "BCM16";
    private static final String GPIO_LCD_D7 = "BCM12";
    int heart[] = {
            0b00000,
            0b01010,
            0b11111,
            0b11111,
            0b11111,
            0b01110,
            0b00100,
            0b00000
    };

    int smiley[] = {
            0b00000,
            0b00000,
            0b01010,
            0b00000,
            0b00000,
            0b10001,
            0b01110,
            0b00000
    };
    int frownie[] = {
            0b00000,
            0b00000,
            0b01010,
            0b00000,
            0b00000,
            0b00000,
            0b01110,
            0b10001
    };

    int armsDown[] = {
            0b00100,
            0b01010,
            0b00100,
            0b00100,
            0b01110,
            0b10101,
            0b00100,
            0b01010
    };

    int armsUp[] = {
            0b00100,
            0b01010,
            0b00100,
            0b10101,
            0b01110,
            0b00100,
            0b00100,
            0b01010
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //B1.Kiểm tra hỗ trợ API
        PeripheralManagerService manager = new PeripheralManagerService();//
        List<String> portList = manager.getGpioList();//Liệt kê danh sách tất cả các cổng
        if (portList.isEmpty()) {
            Log.i(TAG, "Thiết bị không hỗ trợ GPIO API.");//Nếu không có cổng nào thì in nội dung lên Logcat
            return;
        } else {
            Log.i(TAG, "Danh sách hỗ trợ: " + portList);//In danh sách các hỗ trợ lên Logcat
        }
        try {
            mLcd = new LiquidCrystal(GPIO_LCD_RS, GPIO_LCD_EN, GPIO_LCD_D4, GPIO_LCD_D5, GPIO_LCD_D6, GPIO_LCD_D7);
            mLcd.begin(16, 2);   // Cài đặt thông số sử dụng là LCD16x2
            mLcd.createChar(0, smiley);// ghi ký tự mới vào Gram
            mLcd.setCursor(0, 0);
            mLcd.print("fViDieuKhien.xyznAndroid Thingxbs");
            mLcd.setCursor(15, 1);
            mLcd.write((byte) 0);//Goi ky tu luu trong GRAM tai vi tru so 0
            mLcd.setCursor(14, 1);
            mLcd.binkCursorOn();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mLcd != null) {
            try {
                mLcd.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing", e);
            } finally {
                mLcd = null;
            }
        }
    }
}