package com.lancador;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;



public class LancadorControll extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private LancadorControll.Connected connected = LancadorControll.Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private String newline = TextUtil.newline_crlf;
    private boolean ligado = false;
    private boolean clicked = false;
    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");



    }

    @Override
    public void onDestroy() {
        if (connected != LancadorControll.Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lancador_controll, container, false);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch  btn_ligando = view.findViewById(R.id.btn_ligar);
        TextView text_vec = view.findViewById(R.id.textSeV);

        Button bnt_maximo = view.findViewById(R.id.buttonVmax);
        Button bnt_med = view.findViewById(R.id.buttonVmed);
        Button bnt_min = view.findViewById(R.id.buttonVMin);



        btn_ligando.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (b){
                    boolean a = send("9");
                    if (a){
                        btn_ligando.setText("Ligado");
                        ligado=true;
                    }
                    else{
                        btn_ligando.setChecked(false);
                        ligado=false;
                    };
                }
                else {
                    boolean a = send("2");
                    if (a){
                        btn_ligando.setText("Desligado");
                        ligado=false;
                    }else{
                        btn_ligando.setChecked(false);
                        ligado=false;
                    };
                }

            }
        });




            bnt_maximo.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    clicked = true;
                    if(ligado) {

                        send("1");
                        text_vec.setText("Velocidade : Máxima");
                    }else{
                        Toast.makeText(getActivity(), "Conecte ao Bluetooth", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            bnt_med.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    clicked = true;
                    if(ligado){
                        send("3");
                        text_vec.setText("Velocidade : Média");
                    }
                    else{
                        Toast.makeText(getActivity(), "Conecte ao Bluetooth", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            bnt_min.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    clicked = true;
                    if(ligado) {

                        send("4");
                        text_vec.setText("Velocidade : Mínima");
                    }else{
                        Toast.makeText(getActivity(), "Conecte ao Bluetooth", Toast.LENGTH_SHORT).show();
                    }
                }
            });

//        View ligarButton = view.findViewById(R.id.buttonligar);
//        ligarButton.setOnClickListener(v -> send("1"));
//
//        View DesligarButton = view.findViewById(R.id.buttonDesligar);
//        DesligarButton.setOnClickListener(v -> send("2"));
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = LancadorControll.Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = LancadorControll.Connected.False;
        service.disconnect();
    }

    private boolean send(String str) {
        if(connected != LancadorControll.Connected.True) {

            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str+newline ).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
        return true;
    }

    private void receive(byte[] data) {
        String msg = new String(data);
        if(newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
            // don't show CR as ^M if directly before LF
            msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
            boolean pendingNewline = msg.charAt(msg.length() - 1) == '\r';
        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = LancadorControll.Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }
}



