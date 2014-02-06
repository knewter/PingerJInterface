package com.isotope11.pingerjinterface;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    static final String TAG = "PingerJInterfacetag";
    static final String COOKIE = "test";
    static Context context;
    static OtpNode self;
    static OtpMbox mbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.context = getApplicationContext();

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            final Button copyButton = (Button) rootView.findViewById(R.id.copyButton);
            final Button launchErlangButton = (Button) rootView.findViewById(R.id.launchErlangButton);
            final Button pingButton = (Button) rootView.findViewById(R.id.pingButton);
            final Button listFilesButton = (Button) rootView.findViewById(R.id.listFilesButton);

            copyButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    copyErlangOntoFs();
                    makeExecutable("/erlang/bin/epmd");
                    makeExecutable("/erlang/bin/erl");
                }
            });

            launchErlangButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchEpmd();
                    launchErlangNode();
                }
            });

            pingButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    doPing();
                }
            });

            listFilesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listFiles();
                }
            });

            return rootView;
        }

        public void copyErlangOntoFs() {
            Log.d(TAG, "copyErlangOntoFs start");

            InputStream erlangZipFileInputStream = null;
            try {
                erlangZipFileInputStream = getActivity().getApplicationContext().getAssets().open("erlang_R16B.zip");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Decompress unzipper = new Decompress(erlangZipFileInputStream, MainActivity.context.getFilesDir().getPath() + "/");
            unzipper.unzip();

            Log.d(TAG, "copyErlangOntoFs done");
        }

        public void doPing() {
            String remoteNodeName = "server@192.168.1.136";

            Pinger ping = new Pinger();
            ping.execute(remoteNodeName);
        }

        public void makeExecutable(String path) {
            this.doCommand("/system/bin/chmod 777 " + MainActivity.context.getFilesDir().getPath() + path);
        }

        public void doCommand(String command) {
            try {
                // Executes the command.
                Process process = Runtime.getRuntime().exec(command);

                // Reads stdout.
                // NOTE: You can write to stdin of the command using
                //       process.getOutputStream().
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                int read;
                char[] buffer = new char[4096];
                StringBuffer output = new StringBuffer();
                while ((read = reader.read(buffer)) > 0) {
                    output.append(buffer, 0, read);
                }
                reader.close();

                // Waits for the command to finish.
                process.waitFor();

                // send output to the log
                Log.d(TAG, output.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void launchEpmd() {
            this.doCommand(MainActivity.context.getFilesDir().getPath() + "/erlang/bin/epmd -daemon");
        }

        public void launchErlangNode() {
            String ip = Utils.getIPAddress(true);
            this.doCommand(MainActivity.context.getFilesDir().getPath() + "/erlang/bin/erl -name foo@" + ip + " -setcookie " + COOKIE);
        }

        public void listFiles() {
            Log.d(TAG, MainActivity.context.getFilesDir().getPath());
            this.doCommand("/system/bin/ls -al " + MainActivity.context.getFilesDir().getPath());
        }

        public class Pinger extends AsyncTask<Object, Void, String> {
            static final String TAG = "PingerJInterfacetag";

            @Override
            protected String doInBackground(Object... params) {
                String remoteNodeName = (String) params[0];
                ping(remoteNodeName);
                return "k...";
            }

            public void ping(String remoteNodeName){
                Log.d(TAG, "pinging " + remoteNodeName);

                try {
                    if(self == null){
                        self = new OtpNode("mynode", COOKIE);
                        mbox = self.createMbox("facserver");

                        if (self.ping(remoteNodeName, 2000)) {
                            System.out.println("remote is up");
                        } else {
                            System.out.println("remote is not up");
                            return;
                        }
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }


                OtpErlangObject[] msg = new OtpErlangObject[2];
                msg[0] = mbox.self();
                msg[1] = new OtpErlangAtom("ping");
                OtpErlangTuple tuple = new OtpErlangTuple(msg);
                mbox.send("pong", remoteNodeName, tuple);
                Log.d(TAG, "pinging complete");
            }

        }
    }

}
