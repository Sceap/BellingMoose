package com.drsorders.logger;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Jeremie on 7/10/2015.
 */
public class bluetoothDialog extends DialogFragment implements View.OnClickListener {
    public static AlertDialog d;
    public Event mEvent;
    public View rootView;
    public Button rescanBtn;

    private ArrayList<BluetoothDevice> uuidList;

    public interface Event {
        public void sendUuids(BluetoothDevice device);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mEvent = (Event) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }
    @Override
    public void onStart(){
        super.onStart();
        d = (AlertDialog)getDialog();
        if(d != null)
        {

        }


        WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
        lp.dimAmount=1.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        getDialog().getWindow().setAttributes(lp);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();


        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d("drsorders", "Sorry, No Bluetooth antenna found!");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        rootView = inflater.inflate(R.layout.bluetooth_dialog, null);
        rescanBtn = (Button)rootView.findViewById(R.id.rescanBtn);
        rescanBtn.setOnClickListener(this);

        rescan();

        builder.setView(rootView);
        return builder.create();
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            mEvent.sendUuids(uuidList.get(arg2));
        }
    };

    @Override
    public void onClick(View v) {
        if(R.id.rescanBtn == v.getId()) {
            rescan();
        }
    }

    public void rescan() {
        ListView deviceList = (ListView) rootView.findViewById(R.id.bluetoothDevices);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();
        uuidList = new ArrayList();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                list.add(device.getName() + "\n" + device.getAddress());
                uuidList.add(device);
            }
        }

        final ArrayAdapter adapter = new ArrayAdapter(getActivity(),android.R.layout.simple_list_item_1, list);
        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked
    }

}
