/************************************************************************************************************************
 *      _____               ____          _                   _____                      _    _____                     *
 *     |  __ \             / __ \        | |                 / ____|                    | |  / ____|                    *
 *     | |  | |_ __ ___   | |  | |_ __ __| | ___ _ __ ___   | (___  _ __ ___   __ _ _ __| |_| |     __ _ _ __   ___     *
 *     | |  | | '__/ __|  | |  | | '__/ _` |/ _ \ '__/ __|   \___ \| '_ ` _ \ / _` | '__| __| |    / _` | '_ \ / _ \    *
 *     | |__| | |  \__ \  | |__| | | | (_| |  __/ |  \__ \   ____) | | | | | | (_| | |  | |_| |___| (_| | | | |  __/    *
 *     |_____/|_|  |___/   \____/|_|  \__,_|\___|_|  |___/  |_____/|_| |_| |_|\__,_|_|   \__|\_____\__,_|_| |_|\___|    *
 *     |  __ \      | |          | |                                                                                    *
 *     | |  | | __ _| |_ __ _    | |     ___   __ _  __ _  ___ _ __                                                     *
 *     | |  | |/ _` | __/ _` |   | |    / _ \ / _` |/ _` |/ _ \ '__|                                                    *
 *     | |__| | (_| | || (_| |   | |___| (_) | (_| | (_| |  __/ |                                                       *
 *     |_____/ \__,_|\__\__,_|   |______\___/ \__, |\__, |\___|_|                                                       *
 *                                             __/ | __/ |                                                              *
 *                                            |___/ |___/                                                               *
 *                                                                                                                      *
 ************************************************************************************************************************
 ************************************************************************************************************************
 *                                                                                                                      *
 *  This Android application aims to provide the people at Doctor's Orders with a simple tool for using the cane for    *
 *  testing purposes. It uses the Android phone Bluetooth connection to connect to the cane, and then displays in real  *
 *  time the data coming from it.                                                                                       *
 *                                                                                                                      *
 ***********************************************************************************************************************/


/* Setting the working Package to Drs Orders Logger  */
package com.drsorders.logger;

/* All the needed Android Libraries are loaded below */

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Those Java libraries are used for extracting */
/* the datas from the Bluetooth port            */


/* MainActivity: for ease-of-use sake, all the logger app is designed around a unique activity     *
* This activity is then divided in several tabs stored in fragments.                               *
* The fragments are handled by a static subclass in order to simplify the sharing of data         */
public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, bluetoothDialog.Event
{
    private static final String TAG = "drsorders";
    // The HISTORY_SIZE variable defines the number of point showing
    // simultaneously on a plot
    private static final int HISTORY_SIZE = 100;
    
    // The isConnected flag handles the bluetooth connection state
    private boolean isConnected = false;
    // This BluetoothSocket is used to initiate the bluetooth communication
    private static BluetoothSocket bluetoothSocket;

    // series are used to store points to be displayed
    private static SimpleXYSeries[] series = new SimpleXYSeries[64];
    // graphs are used to display one or several series
    private static XYPlot[] graphs = new XYPlot[16];

    // maxWeight stores the highest weight measured since the
    // launch of the application
    // weightThreshold is used to define the max amount of weight
    // allowed before echoing a warning
    private static int maxWeight = 0;
    private static int weightThreshold = 60;


    private static int cmdValue, thrValue;
    private static String customCommand;
    private static boolean[] logFlag = new boolean[3];
    private static final int logSD=0,logUSB=1,logBt=2;


    private static DialogFragment btFragment;

    // This dummy String table is used to test the RegEx detection
    private static String[] dummy = {
            "|0015061817280004,-00131,+00076,+00051,+00296,+00016,+17020,-03936|",
            "|0015061817280005,-00126,+00083,+00052,+00220,-00068,+17024,-03936|",
            "|0015061817280006,-00137,+00072,+00043,+00216,-00008,+17232,-03936|",
            "|0015061817280007,-00128,+00077,+00047,+00332,-00092,+16992,-03904|",
            "|0015061817280008,-00136,+00086,+00040,+00280,+00012,+17068,-03936|",
            "|0015061817280009,-00140,+00076,+00046,+00312,-00064,+17088,-03904|",
            "|0015061817280010,-00133,+00082,+00044,+00280,+00032,+17088,-03936|",
            "|0015061817280011,-00127,+00067,+00050,+00304,+00028,+17112,-03936|",
            "|0015061817280012,-00132,+00081,+00049,+00232,-00036,+17144,-03936|",
            "|0015061817280013,-00115,+00087,+00047,+00260,-00104,+16880,-03936|",
            "|0015061817280014,-00138,+00079,+00050,+00284,-00088,+17184,-03904|",
            "|0015061817280015,-00126,+00090,+00054,+00416,-00040,+16840,-03936|",
            "|0015061817280016,-00149,+00076,+00045,+00264,-00032,+17296,-03904|",
            "|0015061817280017,-00137,+00072,+00044,+00272,-00024,+17040,-03904|",
            "|0015061817280018,-00133,+00077,+00049,+00316,-00116,+17164,-03936|",
            "|0015061817280019,-00135,+00076,+00039,+00312,-00032,+17088,-03968|",
            "|0015061817280020,-00122,+00074,+00050,+00440,-00084,+17200,-03936|",
            "|0015061817280021,-00135,+00082,+00050,+00176,+00028,+17068,-03936|",
            "|0015061817280022,-00142,+00082,+00053,+00208,-00068,+17124,-03936|",
            "|0015061817280023,-00142,+00084,+00049,+00408,-00020,+17064,-03936|",
            "|0015061817280024,-00139,+00075,+00041,+00360,-00108,+17060,-03904|",
            "|0015061817280025,-00129,+00081,+00049,+00296,+00004,+17168,-03904|",
            "|0015061817280026,-00121,+00078,+00044,+00340,-00076,+17092,-03904|",
            "|0015061817280027,-00128,+00079,+00053,+00372,-00044,+16924,-03936|",
            "|0015061817280028,-00123,+00076,+00047,+00364,-00032,+17204,-03936|",
            "|0015061817280029,-00125,+00073,+00055,+00356,+00016,+17112,-03936|",
            "|0015061817280030,-00128,+00075,+00047,+00504,-00024,+17140,-03904|",
            "|0015061817280031,-00134,+00074,+00047,+00364,-00024,+17172,-03936|",
            "|0015061817280032,-00141,+00075,+00046,+00276,-00036,+17140,-03904|",
            "|0015061817280033,-00141,+00075,+00046,+00276,-00036,+17140,-03904|",
            "|0015061817280034,-00129,+00075,+00046,+00196,+00016,+17112,-03936|",
            "|0015061817280035,-00120,+00074,+00047,+00328,+00016,+16968,-03936|",
            "|0015061817280036,-00140,+00070,+00048,+00264,+00036,+17208,-03936|",
            "|0015061817280037,-00115,+00074,+00047,+00284,-00048,+17172,-03936|",
            "|0015061817280038,-00131,+00072,+00045,+00256,-00028,+17116,-03936|",
            "|0015061817280039,-00136,+00080,+00050,+00280,-00088,+17020,-03904|",
            "|0015061817280040,-00125,+00079,+00055,+00228,+00124,+17028,-03936|",
            "|0015061817280041,-00138,+00075,+00050,+00380,-00040,+17164,-03936|",
            "|0015061817280042,-00139,+00072,+00044,+00332,-00036,+17052,-03904|",
            "|0015061817280043,-00121,+00073,+00053,+00228,-00164,+17164,-03936|",
            "|0015061817280044,-00135,+00084,+00053,+00264,+00036,+17080,-03936|",
            "|0015061817280045,-00134,+00078,+00046,+00268,-00100,+17096,-03936|",
            "|0015061817280046,-00144,+00083,+00045,+00300,-00060,+16968,-03936|",
            "|0015061817280047,-00141,+00071,+00052,+00256,+00004,+17132,-03904|",
            "|0015061817280048,-00136,+00066,+00055,+00268,-00068,+17240,-03936|",
            "|0015061817280049,-00132,+00073,+00039,+00284,+00008,+17116,-03936|",
            "|0015061817280050,-00137,+00069,+00058,+00212,-00088,+17004,-03904|",
            "|0015061817280051,-00131,+00083,+00058,+00356,-00020,+17000,-03936|",
            "|0015061817280052,-00147,+00073,+00049,+00244,-00088,+17176,-03936|",
            "|0015061817280053,-00130,+00074,+00053,+00332,-00080,+17100,-03936|",
            "|0015061817280054,-00127,+00070,+00049,+00312,-00032,+17172,-03936|",
            "|0015061817280055,-00118,+00075,+00051,+00328,+00044,+16864,-03936|",
            "|0015061817280056,-00130,+00065,+00050,+00240,+00064,+17112,-03936|",
            "|0015061817280057,-00127,+00073,+00057,+00372,-00044,+17072,-03936|",
            "|0015061817280059,-00120,+00078,+00047,+00304,+00004,+17160,-03936|",
            "|0015061817280060,-00118,+00073,+00049,+00252,-00064,+16976,-03936|",
            "|0015061817280061,-00139,+00077,+00050,+00360,+00020,+17060,-03936|",
            "|0015061817280062,-00122,+00081,+00047,+00340,-00044,+16972,-03936|",
            "|0015061817280063,-00127,+00069,+00042,+00352,+00024,+17032,-03936|",
            "|0015061817280064,-00129,+00079,+00044,+00240,+00004,+16944,-03904|"
    };

    // The following are Threads and Runnables used to keep the app
    // updated
    private static Thread uiThread;
    private static ConnectedThread btThread;
    private static updateGraph data;

    // The mainActivity implements multiple pages
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // When swiping between different sections, select the corresponding
        // tab
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }


        /* Next we start constructing the graphs, stating by allocating the series  */

        // Series 0 to 2 contain the accelerometer graphs
        series[0] = new SimpleXYSeries("X");
        series[1] = new SimpleXYSeries("Y");
        series[2] = new SimpleXYSeries("Z");

        // Series 3 contains the accel X against accel Y
        series[3] = new SimpleXYSeries("X/Y");

        // Series 4 to 6 contain the gyroscope graphs
        series[4] = new SimpleXYSeries("X");
        series[5] = new SimpleXYSeries("Y");
        series[6] = new SimpleXYSeries("Z");

        // Series 7 contains the weight history
        series[7] = new SimpleXYSeries("Weight");

        /* */
        data = new updateGraph();
  }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_connect:

                if(!isConnected) {
                    btFragment = new bluetoothDialog();
                    btFragment.show(getSupportFragmentManager(), "Bluetooth");
                    /*
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBluetoothAdapter == null) {
                        // Device does not support Bluetooth
                        Log.d(TAG,"Sorry, No Bluetooth antenna found!");
                    }
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, 1);
                    }


                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    // If there are paired devices
                    if (pairedDevices.size() > 0) {
                        // Loop through paired devices
                        for (BluetoothDevice device : pairedDevices) {
                            // Search for a K-Netic device
                            if(device.getName().contains("K-Netic")) {
                                Log.d(TAG,device.getAddress());
                                for (ParcelUuid uuid: device.getUuids()) {
                                    uuid.getUuid();
                                    Log.d(TAG, "UUID: " + uuid.getUuid().toString());
                                    try {
                                        bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid.getUuid());

                                        bluetoothSocket.connect();

                                        if(bluetoothSocket.isConnected()) {
                                            btThread = new ConnectedThread(bluetoothSocket);
                                            btThread.start();
                                            isConnected = true;

                                            Log.d(TAG,"Connected to device");

                                            item.setTitle("Disconnect");

                                            break;
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }*/
                }   else {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    isConnected = false;
                    btThread.interrupt();
                    item.setTitle("Connect to Device");
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }



    @Override
    public void onResume() {
        uiThread = new Thread(data);
        uiThread.start();

        super.onResume();
    }

    @Override
    public void onPause() {
        data.stopThread();

        super.onPause();
    }


    @Override
    public void sendUuids(BluetoothDevice device) {
        for(ParcelUuid uuid : device.getUuids()) {
                uuid.getUuid();
                Log.d(TAG, "UUID: " + uuid.getUuid().toString());
                try {
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid.getUuid());

                    bluetoothSocket.connect();

                    if(bluetoothSocket.isConnected()) {
                        btThread = new ConnectedThread(bluetoothSocket);
                        btThread.start();
                        isConnected = true;

                        Log.d(TAG,"Connected to device");

                        //item.setTitle("Disconnect");
                        btFragment.dismiss();

                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    class updateGraph implements Runnable {
        private boolean keepRunning = false;
        private int i = 0;

        public void stopThread() {
            keepRunning = false;
        }



        //@Override
        public void run() {
            try {
                keepRunning = true;

                while (keepRunning) {
                    Thread.sleep(50); // decrease or remove to speed up the refresh rate.

                    // redraw the Plots:
                    for(int i=0;i<16;i++) {
                        if(graphs[i]!=null) {
                            graphs[i].redraw();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public enum FSMState{
        init,
        sepOrLSB,
        LSB,
        syncing,
        synced
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private String bufferedStr = "";


        public FSMState state;
        public int frameLength;
        public int nbValue;

        public int errorCounter = 0;

        private int nbValues = 0;
        private String regEx;

        private Pattern frame;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            state = FSMState.init;
        }


        /*  Simple function used to extract the date from       */
        /*  a frame                                             */
        /*  returns the time in seconds                         */
        /*  Only works for the time, not the date               */
        double getDate(String date) {
            double hour = 0;
            double minute = 0;
            double sec = 0;
            double csec = 0;

            int inc = 0;


            hour += date.charAt(8 + inc)-'0';
            hour *= 10;
            hour += date.charAt(9 + inc)-'0';


            minute += date.charAt(10 + inc)-'0';
            minute *= 10;
            minute += date.charAt(11 + inc)-'0';

            sec += date.charAt(12 + inc)-'0';
            sec *= 10;
            sec += date.charAt(13 + inc)-'0';

            csec += date.charAt(14 + inc)-'0';
            csec *= 10;
            csec += date.charAt(15 + inc)-'0';

            Date d = new Date();
            d.setTime(d.UTC(0, 0, 0, (int) hour, (int) minute, (int) sec));
            return d.getTime()+csec*10;
        }

        public short extractValue(String val) {
            return ((short)
                    (((((short) val.charAt(1))<<8)&0xFF00)                 // Take the LSB, and shift it to MSB position
                    |
                    (~val.charAt(0))&0x00FF)                               // Take the MSB, invert the bits and put it in LSB position
            );
        }

        public void run() {

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    byte[] tmp = new byte[2];
                    byte[] tmpMSB = new byte[1];
                    byte[] tmpLSB = new byte[1];

                    switch(state.toString()) {
                        case "init":
                            if(mmInStream.available()>0) {
                                frameLength = 0;
                                nbValue = 0;
                                errorCounter = 0;

                                mmInStream.read(tmp,0,1);
                                if(tmp[0]==0x7F)
                                    state = FSMState.sepOrLSB;
                            }
                            break;
                        case "sepOrLSB":
                            if(mmInStream.available()>0) {
                                mmInStream.read(tmp,0,1);
                                if(tmp[0]==0x7F) {
                                    state = FSMState.syncing;
                                } else {
                                    mmInStream.read(tmp,0,1);
                                    state = FSMState.LSB;
                                }
                            }
                            break;
                        case "LSB":
                            if(mmInStream.available()>1) {
                                mmInStream.read(tmpMSB,0,1);
                                mmInStream.read(tmpLSB,0,1);

                                if(tmpMSB[0]==0x7F && tmpLSB[0]==0x7F) {
                                    state = FSMState.syncing;
                                } else {
                                    errorCounter++;
                                }

                                if(errorCounter>100) {
                                    state = FSMState.init;
                                }
                            }
                            break;
                        case "syncing":
                            errorCounter = 0;
                            if(mmInStream.available()>1) {
                                mmInStream.read(tmpMSB,0,1);
                                mmInStream.read(tmpLSB,0,1);

                                if(tmpMSB[0]==0x7F && tmpLSB[0]==0x7F) {
                                    state = FSMState.synced;
                                    regEx = "";
                                    for(int i=0;i<nbValue;i++) {
                                        regEx+="(..)";
                                    }
                                    regEx+="\\x7F\\x7F";

                                    frame = Pattern.compile(regEx,Pattern.DOTALL);
                                } else {
                                    frameLength+=2;
                                    nbValue+=1;
                                }
                            }
                            break;
                        case "synced":
                            if(mmInStream.available()>frameLength+2) {
                                String mStr = "";
                                for(int i=0;i<nbValue;i++) {
                                    mmInStream.read(tmp,0,2);

                                    mStr += (char)tmp[0];
                                    mStr += (char)tmp[1];
                                }
                                mmInStream.read(tmp,0,2);

                                mStr += (char)tmp[0];
                                mStr += (char)tmp[1];


                                // First of all we check if the frame matches the regEx
                                // If it does, we update the different values with the one
                                // from the frame
                                Matcher matcher = frame.matcher(mStr);

                                if(matcher.find()) {
                                    errorCounter = 0;
                                    if (series[0].size() > HISTORY_SIZE) {
                                        series[0].removeFirst();
                                        series[1].removeFirst();
                                        series[2].removeFirst();
                                        series[4].removeFirst();
                                        series[5].removeFirst();
                                        series[6].removeFirst();
                                        series[7].removeFirst();
                                    }

                                    if(series[3].size()>5) {
                                        series[3].removeFirst();
                                        series[3].removeFirst();
                                        series[3].removeFirst();
                                        series[3].removeFirst();
                                        series[3].removeFirst();
                                        series[3].removeFirst();
                                    }
                                    series[3].addLast(extractValue(matcher.group(7)) - 2, extractValue(matcher.group(8)) + 2);
                                    series[3].addLast(extractValue(matcher.group(7)) + 2, extractValue(matcher.group(8)) - 2);
                                    series[3].addLast(extractValue(matcher.group(7)) - 2, extractValue(matcher.group(8)) - 2);
                                    series[3].addLast(extractValue(matcher.group(7)) + 2, extractValue(matcher.group(8)) + 2);
                                    series[3].addLast(extractValue(matcher.group(7)) - 2, extractValue(matcher.group(8)) + 2);
                                    series[3].addLast(extractValue(matcher.group(7)) + 2, extractValue(matcher.group(8)) - 2);

                                    int hour = (matcher.group(2).charAt(1) >>> 2);
                                    int min = (matcher.group(2).charAt(0));

                                    int sec = ((matcher.group(3).charAt(1)) >>> 2);
                                    int csec = (matcher.group(3).charAt(0));
                                    sec = sec%60;

                                    Date d = new Date();
                                    d.setTime(d.UTC(0, 0, 0, hour, min, sec));
                                    double ms =  d.getTime()+csec*10;
                                    series[0].addLast(ms, extractValue(matcher.group(7)));
                                    series[1].addLast(ms, extractValue(matcher.group(8)));
                                    series[2].addLast(ms, extractValue(matcher.group(9)));
                                    series[4].addLast(ms, extractValue(matcher.group(4)));
                                    series[5].addLast(ms, extractValue(matcher.group(5)));
                                    series[6].addLast(ms, extractValue(matcher.group(6)));
                                    series[7].addLast(ms, 600-extractValue(matcher.group(10)));
                                } else {
                                    errorCounter++;
                                    if(errorCounter>10)
                                        state = FSMState.init;
                                }
                            break;
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }




    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return "Weight Sensor".toUpperCase(l);
                case 1:
                    return "Accelerometer".toUpperCase(l);
                case 2:
                    return "Gyroscope".toUpperCase(l);
                case 3:
                    return "Commands".toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private updateFragment update;
        View rootView;
        private Thread updateThread;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            int pageNumber = (getArguments().getInt(ARG_SECTION_NUMBER)) - 1;

            switch(pageNumber) {
                case 0:
                    rootView = inflater.inflate(R.layout.weight_frag, container, false);

                    graphs[7] = (XYPlot) rootView.findViewById(R.id.weightDisplay);

                    graphs[7].setRangeBoundaries(0, 600, BoundaryMode.FIXED);
                    graphs[7].setDomainBoundaries(0, 600, BoundaryMode.AUTO);
                    graphs[7].setRangeStepValue(10);
                    graphs[7].setDomainStepValue(5);
                    graphs[7].addSeries(series[7], new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, null));
                    graphs[7].setDomainValueFormat(new Format() {
                        // create a simple date format that draws on the year portion of our timestamp.
                        // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
                        // for a full description of SimpleDateFormat.
                        private SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");

                        @Override
                        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                            // because our timestamps are in seconds and SimpleDateFormat expects milliseconds
                            // we multiply our timestamp by 1000:
                            long timestamp = ((Number) obj).longValue();
                            Date date = new Date(timestamp);
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            return dateFormat.format(date, toAppendTo, pos);
                        }

                        @Override
                        public Object parseObject(String source, ParsePosition pos) {
                            return null;
                        }
                    });

                    graphs[7].setBorderStyle(Plot.BorderStyle.ROUNDED,(float)10,(float)10);


                    graphs[7].getGraphWidget().getDomainGridLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[7].getGraphWidget().getDomainOriginLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[7].getGraphWidget().getDomainSubGridLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[7].getGraphWidget().getRangeGridLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[7].getGraphWidget().getRangeOriginLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[7].getGraphWidget().getRangeSubGridLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[7].getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
                    graphs[7].getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
                    graphs[7].getBackgroundPaint().setColor(Color.WHITE);
                    graphs[7].getLegendWidget().setWidth((float) 0.8);
                    graphs[7].getLegendWidget().setMarginLeft(100);
                    graphs[7].setTicksPerRangeLabel(3);

                    break;

                case 1:
                    rootView = inflater.inflate(R.layout.accel_frag, container, false);

                    graphs[0] = (XYPlot) rootView.findViewById(R.id.xyzAxis);
                    graphs[1] = (XYPlot) rootView.findViewById(R.id.XYPlot);

                    graphs[0].setDomainValueFormat(new Format() {
                        // create a simple date format that draws on the year portion of our timestamp.
                        // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
                        // for a full description of SimpleDateFormat.
                        private SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");

                        @Override
                        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                            // because our timestamps are in seconds and SimpleDateFormat expects milliseconds
                            // we multiply our timestamp by 1000:
                            long timestamp = ((Number) obj).longValue();
                            Date date = new Date(timestamp);
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            return dateFormat.format(date, toAppendTo, pos);
                        }

                        @Override
                        public Object parseObject(String source, ParsePosition pos) {
                            return null;
                        }
                    });


                    graphs[0].setRangeBoundaries(-180, 360, BoundaryMode.AUTO);
                    graphs[0].setDomainBoundaries(0, 300, BoundaryMode.AUTO);
                    graphs[0].setRangeStepValue(10);
                    graphs[0].setDomainStepValue(5);
                    graphs[0].addSeries(series[0], new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, null));
                    graphs[0].addSeries(series[1], new LineAndPointFormatter(Color.rgb(100, 200, 100), null, null, null));
                    graphs[0].addSeries(series[2], new LineAndPointFormatter(Color.rgb(200, 100, 100), null, null, null));


                    graphs[1].setRangeBoundaries(-16000, 16000, BoundaryMode.FIXED);
                    graphs[1].setDomainBoundaries(-16000, 16000, BoundaryMode.FIXED);
                    graphs[1].setRangeStepValue(5);
                    graphs[1].setDomainStepValue(5);
                    LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                            Color.rgb(200, 100, 100), null, null, null);
                    formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
                    formatter1.getLinePaint().setStrokeWidth(15);
                    graphs[1].addSeries(series[3], formatter1);

                    for(int i=0;i<2;i++) {
                        graphs[i].setBorderStyle(Plot.BorderStyle.ROUNDED, (float) 10, (float) 10);
                        graphs[i].getGraphWidget().getDomainGridLinePaint().setColor(Color.rgb(195, 195, 195));
                        graphs[i].getGraphWidget().getDomainOriginLinePaint().setColor(Color.rgb(195, 195, 195));
                        graphs[i].getGraphWidget().getDomainSubGridLinePaint().setColor(Color.rgb(195, 195, 195));
                        graphs[i].getGraphWidget().getRangeGridLinePaint().setColor(Color.rgb(195, 195, 195));
                        graphs[i].getGraphWidget().getRangeOriginLinePaint().setColor(Color.rgb(195, 195, 195));
                        graphs[i].getGraphWidget().getRangeSubGridLinePaint().setColor(Color.rgb(195, 195, 195));
                        graphs[i].getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
                        graphs[i].getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
                        graphs[i].getBackgroundPaint().setColor(Color.WHITE);
                        graphs[i].getLegendWidget().setWidth((float) 0.8);
                        graphs[i].getLegendWidget().setMarginLeft(100);
                        graphs[i].setTicksPerRangeLabel(3);
                    }
                break;

                case 2:
                    rootView = inflater.inflate(R.layout.gyro_frag, container, false);

                    graphs[2] = (XYPlot) rootView.findViewById(R.id.gyroXYZAxis);

                    graphs[2].setRangeBoundaries(-180, 360, BoundaryMode.AUTO);
                    graphs[2].setDomainBoundaries(0, 300, BoundaryMode.AUTO);
                    graphs[2].setRangeStepValue(10);
                    graphs[2].setDomainStepValue(5);
                    graphs[2].addSeries(series[4], new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, null));
                    graphs[2].addSeries(series[5], new LineAndPointFormatter(Color.rgb(100, 200, 100), null, null, null));
                    graphs[2].addSeries(series[6], new LineAndPointFormatter(Color.rgb(200, 100, 100), null, null, null));
                    graphs[2].setDomainValueFormat(new Format() {
                        // create a simple date format that draws on the year portion of our timestamp.
                        // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
                        // for a full description of SimpleDateFormat.
                        private SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");

                        @Override
                        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                            // because our timestamps are in seconds and SimpleDateFormat expects milliseconds
                            // we multiply our timestamp by 1000:
                            long timestamp = ((Number) obj).longValue();
                            Date date = new Date(timestamp);
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            return dateFormat.format(date, toAppendTo, pos);
                        }

                        @Override
                        public Object parseObject(String source, ParsePosition pos) {
                            return null;
                        }
                    });

                    graphs[2].setBorderStyle(Plot.BorderStyle.ROUNDED, (float) 10, (float) 10);

                    graphs[2].getGraphWidget().getDomainGridLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[2].getGraphWidget().getDomainOriginLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[2].getGraphWidget().getDomainSubGridLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[2].getGraphWidget().getRangeGridLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[2].getGraphWidget().getRangeOriginLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[2].getGraphWidget().getRangeSubGridLinePaint().setColor(Color.rgb(195, 195, 195));
                    graphs[2].getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
                    graphs[2].getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
                    graphs[2].getBackgroundPaint().setColor(Color.WHITE);
                    graphs[2].getLegendWidget().setWidth((float) 0.8);
                    graphs[2].getLegendWidget().setMarginLeft(100);
                    graphs[2].setTicksPerRangeLabel(3);


                break;

                case 3:
                    rootView = inflater.inflate(R.layout.param_frag, container, false);

                break;

                default:

                break;
            }

            update = new updateFragment();

            return rootView;
        }


        @Override
        public void onResume() {
            updateThread = new Thread(update);
            updateThread.start();

            super.onResume();
        }

        @Override
        public void onPause() {
            update.stopThread();
            super.onPause();
        }

        class updateFragment implements Runnable {
            private boolean keepRunning = false;

            public void stopThread() {
                keepRunning = false;
            }

            //@Override
            public void run() {
                try {
                    keepRunning = true;

                    while (keepRunning) {
                        Thread.sleep(200);

                        int pageNumber = (getArguments().getInt(ARG_SECTION_NUMBER)) - 1;

                        switch(pageNumber) {
                            case 0:
                                if(getActivity()!=null)
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ProgressBar pB = (ProgressBar) rootView.findViewById(R.id.vertical_progressbar);
                                        View max = rootView.findViewById(R.id.maxWeight);
                                        View cur = rootView.findViewById(R.id.trsWeight);

                                        pB.hasFocus();
                                        if(series[7].size()>2)
                                            pB.setProgress((series[7].getY(series[7].size()-3).intValue()/6));




                                        float totHeight = pB.getHeight();
                                        float curHeight = weightThreshold*totHeight/100;
                                        cur.setTranslationY(totHeight - curHeight + 5);
                                        curHeight = pB.getProgress()*totHeight/100;

                                        if(pB.getProgress()>weightThreshold) {
                                            TextView overweight = (TextView) rootView.findViewById(R.id.weightWarning);
                                            overweight.setText("Too much weight on the cane !");
                                            overweight.setTextColor(Color.rgb(200, 100, 100));
                                        } else {
                                            TextView overweight = (TextView) rootView.findViewById(R.id.weightWarning);
                                            overweight.setText("No overweight detected");
                                            overweight.setTextColor(Color.rgb(25,25,25));
                                        }

                                        if(pB.getProgress()>maxWeight) {
                                            maxWeight = pB.getProgress();
                                            max.setTranslationY(totHeight - curHeight + 5);
                                        }
                                    }
                                });
                            break;

                            case 3:
                                try{
                                    cmdValue = Integer.parseInt(((TextView)rootView.findViewById(R.id.cmdValue)).getText().toString());
                                } catch(NumberFormatException e) {
                                    cmdValue = 0;
                                }
                                if(cmdValue>65535)
                                    cmdValue = 65535;

                                thrValue = ((SeekBar)rootView.findViewById(R.id.weightSeek)).getProgress();

                                logFlag[logSD] = ((CheckBox)rootView.findViewById(R.id.SDChk)).isChecked();
                                logFlag[logUSB] = ((CheckBox)rootView.findViewById(R.id.USBChk)).isChecked();
                                logFlag[logBt] = ((CheckBox)rootView.findViewById(R.id.BTChk)).isChecked();
                                customCommand = ((TextView) rootView.findViewById(R.id.customCmd)).getText().toString();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    /* Button click functions               */
    public void syncRTC(View v) {
        Date d = new Date();

        if(isConnected)
            btThread.write(("#YRS"+String.format("%05d",d.getYear()-100)).getBytes());
            btThread.write(".".getBytes());
            btThread.write(("#MTH"+String.format("%05d",d.getMonth())).getBytes());
            btThread.write(".".getBytes());
            btThread.write(("#DYS"+String.format("%05d",d.getDay())).getBytes());
            btThread.write(".".getBytes());
            btThread.write(("#HRS"+String.format("%05d",d.getHours())).getBytes());
            btThread.write(".".getBytes());
            btThread.write(("#MNS"+String.format("%05d",d.getMinutes())).getBytes());
            btThread.write(".".getBytes());
            btThread.write(("#SEC"+String.format("%05d",d.getSeconds())).getBytes());
            btThread.write(".".getBytes());
    }

    public void sendCommand(View v) {
        String str = "";
        switch(v.getId()) {
            case R.id.FRQ:
                str = "FRQ";
                break;
            case R.id.BST:
                str = "BST";
                break;
            case R.id.STR:
                str = "STR";
                break;
        }

        if(isConnected)
            btThread.write(("#" + str + String.format("%05d", cmdValue)).getBytes());
    }

    public void sendCustomCommand(View v) {
        if(isConnected)
            btThread.write((customCommand).getBytes());
    }

    public void sendLogSettings(View v) {
        if(isConnected)
            btThread.write(("#SDC0000"+(logFlag[logSD]?'1':'0')).getBytes());
            btThread.write(".".getBytes());
            btThread.write(("#USB0000"+(logFlag[logUSB]?'1':'0')).getBytes());
            btThread.write(".".getBytes());
            btThread.write(("#BLU0000"+(logFlag[logBt]?'1':'0')).getBytes());
            btThread.write(".".getBytes());
    }

    public void sendWeightThreshold(View v) {
        Log.d(TAG,("#THR" + String.format("%05d", thrValue)));
        if(isConnected) {
            btThread.write(("#THR" + String.format("%05d", thrValue)).getBytes());
            weightThreshold = thrValue/6;
        }
    }



}

