package src.com.hoho.android.usbserial.examples;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.examples.BuildConfig;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;

public class SerialService extends Service {
    public SerialService() {}

    private final LocalBinder mBinder = new LocalBinder();
    private Handler mBackgroundHandler;


    private SerialInputOutputManager mSerialManager;
    private UsbSerialPort mSerialPort;

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("SerialService Thread");
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        stopMonitoringSerialPort(true);
    }

    public void monitorSerialPort(@NonNull UsbSerialPort sPort) {
        Log.i(BuildConfig.TAG, "Starting io manager ..");

        //Create the serial io manager.
        mSerialManager = new SerialInputOutputManager(sPort);

        //Start listening on a background thread. Using the single threaded handler.
        mBackgroundHandler.post(mSerialManager);
    }

    public void stopMonitoringSerialPort(boolean closePort){
        if(mSerialManager != null){
            Log.i(BuildConfig.TAG, "Stopping io manager ..");

            //Stop the serial io manager.
            mSerialManager.stop();

            //Release the reference.
            mSerialManager = null;
        }

        if(closePort && mSerialPort != null){
            try {
                mSerialPort.close();
                mSerialPort = null;
            }catch (IOException ex){
                Log.e(BuildConfig.TAG, "Error Closing Port", ex);
            }
        }
    }

    public class LocalBinder extends Binder {
        public SerialService getService() { return SerialService.this; }
    }
}
