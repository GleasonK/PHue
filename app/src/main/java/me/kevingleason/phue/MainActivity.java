package me.kevingleason.phue;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity {
    public static final int COLOR_RED   = 0;
    public static final int COLOR_BLUE  = 1;
    public static final int COLOR_GREEN = 2;

    SeekBar mRedSeek, mGreenSeek, mBlueSeek;
    LinearLayout mRGBValueHolder;

    private Pubnub mPubNub;
    public static final String PUBLISH_KEY = "pub-c-3a6515b4-cc50-4515-82ea-80d76f361027";
    public static final String SUBSCRIBE_KEY = "sub-c-a6b102dc-00a0-11e5-8fd4-0619f8945a4f";
    public static final String CHANNEL = "phue";

    private long lastUpdate = System.currentTimeMillis();
    private boolean pHueOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPubNub();

        // Get Seek Bars
        mRedSeek   = (SeekBar) findViewById(R.id.red_seek);
        mGreenSeek = (SeekBar) findViewById(R.id.green_seek);
        mBlueSeek  = (SeekBar) findViewById(R.id.blue_seek);
        mRGBValueHolder = (LinearLayout) findViewById(R.id.rgb_value_holder);

        // Setup Seek Bars
        setupSeekBar(mRedSeek,   COLOR_RED);
        setupSeekBar(mGreenSeek, COLOR_GREEN);
        setupSeekBar(mBlueSeek,  COLOR_BLUE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void initPubNub(){
        this.mPubNub = new Pubnub(
                PUBLISH_KEY,
                SUBSCRIBE_KEY
        );
        this.mPubNub.setUUID("AndroidPHue");
        subscribe();
    }

    public void publish(int red, int green, int blue){
        JSONObject js = new JSONObject();
        try {
            js.put("RED",   red);
            js.put("GREEN", green);
            js.put("BLUE",  blue);
        } catch (JSONException e) { e.printStackTrace(); }

        Callback callback = new Callback() {
            public void successCallback(String channel, Object response) {
                Log.d("PUBNUB",response.toString());
            }
            public void errorCallback(String channel, PubnubError error) {
                Log.d("PUBNUB",error.toString());
            }
        };
        this.mPubNub.publish(CHANNEL, js, callback);
    }


    public void subscribe(){
        try {
            this.mPubNub.subscribe(CHANNEL, new Callback() {
                @Override
                public void connectCallback(String channel, Object message) {
                    Log.d("PUBNUB","SUBSCRIBE : CONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                @Override
                public void disconnectCallback(String channel, Object message) {
                    Log.d("PUBNUB","SUBSCRIBE : DISCONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                public void reconnectCallback(String channel, Object message) {
                    Log.d("PUBNUB","SUBSCRIBE : RECONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("PUBNUB","SUBSCRIBE : " + channel + " : "
                            + message.getClass() + " : " + message.toString());
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.d("PUBNUB","SUBSCRIBE : ERROR on channel " + channel
                            + " : " + error.toString());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setupSeekBar(SeekBar seekBar, final int colorID){
        seekBar.setMax(255);        // Seek bar values goes 0-255
        seekBar.setProgress(255);   // Set the knob to 255 to start
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                publish(mRedSeek.getProgress(), mGreenSeek.getProgress(), mBlueSeek.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                publish(mRedSeek.getProgress(), mGreenSeek.getProgress(), mBlueSeek.getProgress());
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                TextView colorValueText;
                switch (colorID){  // Get the TextView identified by the colorID
                    case COLOR_RED:
                        colorValueText = (TextView)findViewById(R.id.red_value);
                        break;
                    case COLOR_GREEN:
                        colorValueText = (TextView) findViewById(R.id.green_value);
                        break;
                    case COLOR_BLUE:
                        colorValueText = (TextView)findViewById(R.id.blue_value);
                        break;
                    default:
                        Log.e("SetupSeek","Invalid color.");
                        return;
                }
                colorValueText.setText(String.valueOf(progress));  // Update the 0-255 text
                int red   = mRedSeek.getProgress();     // Get Red value 0-255
                int green = mGreenSeek.getProgress();   // Get Grn value 0-255
                int blue  = mBlueSeek.getProgress();    // Get Blu value 0-255
                updateRGBViewHolderColor(red, green, blue); // Change the background of the viewholder

                long now = System.currentTimeMillis();    // Only allow 1 pub every 100 milliseconds
                if (now - lastUpdate > 100 && fromUser) { // Threshold and only send when user sliding
                    lastUpdate = now;
                    publish(red, green, blue);          // Stream RGB values to the Pi
                }
            }
        });
    }

    public void lightOff(View view){
        publish(0, 0, 0);
        setRGBSeeks(0, 0, 0);
    }

    public void lightOn(View view){
        publish(255, 255, 255);
        setRGBSeeks(255, 255, 255);
    }

    public void pHueOn(View view){
        this.pHueOn = true;

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    for (int i = 0; i < 720; i+=3) {
                        if (!pHueOn) return;
                        int r = posSinWave(50, i, 0.5);
                        int g = posSinWave(50, i, 1);
                        int b = posSinWave(50, i, 2);
                        publish(r, g, b);
                        setRGBSeeks(r, g, b);
                        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }
            }
        });
        t.start();

    }

    public int posSinWave(int amplitude, int angle, double frequency) {
        return (int)((amplitude + (amplitude * Math.sin(Math.toRadians(angle) * frequency)))/100.0*255);
    }

    public void setRGBSeeks(int red, int green, int blue){
        mRedSeek.setProgress(red);
        mGreenSeek.setProgress(green);
        mBlueSeek.setProgress(blue);
    }

    private void updateRGBViewHolderColor(int red, int green, int blue){
        int alpha = 255; // No opacity
        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        mRGBValueHolder.setBackgroundColor(color);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        this.pHueOn = false;
        return super.dispatchTouchEvent(ev);
    }

}
