package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.shell;
import static com.hijacker.MainActivity.shell3_in;
import static com.hijacker.MainActivity.su_thread;

public class InstallFirmwareDialog extends DialogFragment {
    View view;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.install_firmware, null);

        builder.setView(view);
        builder.setTitle(R.string.install_nexmon_title);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setPositiveButton("Install", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        return builder.create();
    }
    @Override
    public void onStart() {
        super.onStart();
        //Override positiveButton action to dismiss the fragment only when the directories exist, not on error
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(shell==null){
                        su_thread.start();
                        try{
                            //Wait for su shells to spawn
                            su_thread.join();
                        }catch(InterruptedException ignored){}
                    }
                    String firm_location = ((EditText)view.findViewById(R.id.firm_location)).getText().toString();
                    String util_location = ((EditText)view.findViewById(R.id.util_location)).getText().toString();

                    File firm = new File(firm_location);
                    File util = new File(util_location);
                    if(!firm.exists()){
                        Toast.makeText(getActivity().getApplicationContext(), "Directory for Firmware doesn't exist", Toast.LENGTH_SHORT).show();
                    }else if(!(new File(firm_location + "/fw_bcmdhd.bin").exists())){
                        Toast.makeText(getActivity().getApplicationContext(), "There is no fw_bcmdhd.bin in there", Toast.LENGTH_SHORT).show();
                    }else if(!util.exists()){
                        Toast.makeText(getActivity().getApplicationContext(), "Directory for utility doesn't exist", Toast.LENGTH_SHORT).show();
                    }else{
                        if(debug){
                            Log.d("InstallFirmwareDialog", "Installing firmware in " + firm_location);
                            Log.d("InstallFirmwareDialog", "Installing utility in " + util_location);
                        }
                        shell3_in.print("busybox mount -o rw,remount,rw /system\n");
                        shell3_in.flush();
                        extract("fw_bcmdhd.bin", firm_location);
                        extract("nexutil", util_location);
                        shell3_in.print("busybox mount -o ro,remount,ro /system\n");
                        shell3_in.flush();
                        Toast.makeText(getActivity().getApplicationContext(), "Installed firmware and utility", Toast.LENGTH_LONG).show();
                        dismiss();
                    }
                }
            });
        }
    }
    void extract(String filename, String dest){
        File f = new File(path, filename);      //no permissions to write at dest so extract at local directory and then move to target
        dest = dest + '/' + filename;
        if(!f.exists()){
            try{
                InputStream in = getResources().getAssets().open(filename);
                FileOutputStream out = new FileOutputStream(f);
                byte[] buf = new byte[1024];
                int len;
                while((len = in.read(buf))>0){
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                shell3_in.print("mv " + path + '/' + filename + " " + dest + '\n');
                shell3_in.print("chmod 755 " + dest + '\n');
                shell3_in.flush();
            }catch(IOException e){
                Log.e("FileProvider", "Exception copying from assets", e);
            }
        }
    }
}