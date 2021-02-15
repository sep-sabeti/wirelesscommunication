package com.example.client;

import androidx.annotation.LongDef;
import androidx.annotation.MainThread;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    EditText textToBeSent, ipAddress , portNumber;
    TextView receivedText;
    Button start, network, status , reset;
    public static InetAddress ip;
    public static int port;
    public static String messageToBeSent;
    public static String messageReceived;
    public Socket socket = null;
    public static final byte[] buffer = new byte[1024];
    public static final int ITERATION = 1000;
    public static final String FINISHED_STATUS = "Finished";
    public static Boolean isClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToBeSent = findViewById(R.id.textToBeSent);
        ipAddress = findViewById(R.id.ipAddress);
        receivedText = findViewById(R.id.receivedText);
        portNumber = findViewById(R.id.portNumber);
        start = findViewById(R.id.start);
        network = findViewById(R.id.findNetwork);
        status = findViewById(R.id.status);
        final Handler handler = new Handler();
        reset = findViewById(R.id.reset);

        // We need to either create a new Thread OR use the below line
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        setTitle("Client-8");


        status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(socket == null){
                    Toast.makeText(MainActivity.this, "Socket is null", Toast.LENGTH_SHORT).show();
                } else if(socket != null && socket.isConnected()){
                    Toast.makeText(MainActivity.this, "Socket is connected", Toast.LENGTH_SHORT).show();
                } else if (socket != null && !socket.isConnected()){
                    Toast.makeText(MainActivity.this, "Socket is not connected", Toast.LENGTH_SHORT).show();
                } else{
                    Toast.makeText(MainActivity.this,"Failure",Toast.LENGTH_SHORT).show();
                }
            }
        });


    reset.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this,"Socket is already closed",Toast.LENGTH_SHORT).show();
            }
        }
    });


        network.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        if (isClicked == false) {
            try {
                port = Integer.parseInt(portNumber.getText().toString());
                ip = InetAddress.getByName(ipAddress.getText().toString());
                Client client = new Client();
                client.run();
                socket = client.socketGetter();
                if (socket != null && socket.isConnected()) {
                    Toast.makeText(MainActivity.this, "Connection succeeded on port " + ip.getHostName(), Toast.LENGTH_SHORT).show();
                    isClicked = true;
                } else if (socket != null && !socket.isConnected()) {
                    Toast.makeText(MainActivity.this, "socket is not connected ", Toast.LENGTH_SHORT).show();
                } else if (socket == null) {
                    Toast.makeText(MainActivity.this, "socket is not created ", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "You already clicked", Toast.LENGTH_SHORT).show();
        }

    }
});

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int Count = 0;
                messageToBeSent = textToBeSent.getText().toString();

                    while (Count <= ITERATION) {

                        long time0 = System.currentTimeMillis();

                        ReceiveData receiveData = new ReceiveData(socket , Count);
                        receiveData.run();
                        if(!receiveData.isAlive()){
                            SendData sendData = new SendData(socket);
                            sendData.run();
                            long time1 = System.currentTimeMillis();
                            long rtt = (time1 - time0) / 2 ;
                            Log.d("Latency", "" + rtt);
                            Count++;
                        }
                            }
                handler.post(() -> receivedText.setText(messageReceived));
                isClicked = false;

            }
        });
    }

    class Client implements Runnable{
        Socket socket;
        public Client(){
        }

        @Override
        public void run() {
            try {
                socket = new Socket(ip,port);
                Toast.makeText(MainActivity.this,"created my socket",Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this,"Task failed in creating a socket",Toast.LENGTH_SHORT).show();
            }
        }

        public Socket socketGetter(){
            return this.socket;
        }
    }


    class SendData extends Thread{
        Socket socket;

        public SendData(Socket socket) {
            this.socket = socket;

        }

        @Override
        public void run() {
            DataOutputStream dataOutputStream = null;
            try {
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();;
            }
            try {
                dataOutputStream.writeUTF(messageToBeSent);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();;
            }
        }
    }


//
//    class SendData extends AsyncTask<Socket,Void,String> {
//
//        Socket socket;
//
//        public SendData(Socket socket) {
//        this.socket = socket;
//        }
//
//
//        @Override
//        protected String doInBackground(Socket... params) {
//            try {
//                DataOutputStream dataOutputStream;
//                dataOutputStream = new DataOutputStream(socket.getOutputStream());
//                dataOutputStream.writeUTF(messageToBeSent);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return FINISHED_STATUS;
//        }
//    }


    class ReceiveData extends Thread {
        Socket socket;
        int iteration;

        public ReceiveData(Socket socket , int iteration){
            this.socket = socket;
            this.iteration = iteration;
        }
        @Override
        public void run() {
            int bytes;
            DataInputStream dataInputStream = null;
            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                bytes = dataInputStream.read(buffer);
                if(iteration != ITERATION){
                    messageReceived = new String((byte[]) buffer, 0, bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    class ReceiveData extends AsyncTask<Socket,Void,String>{
//        Socket socket;
//
//        public ReceiveData(Socket socket){
//            this.socket = socket;
//        }
//        @Override
//        protected String doInBackground(Socket... params) {
//            try {
//                int bytes;
//                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
//                bytes = dataInputStream.read(buffer);
//                messageReceived = new String((byte[]) buffer, 0, bytes);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return FINISHED_STATUS;
//        }
//        @Override
//        protected void onPostExecute(String s) {
//            super.onPostExecute(s);
//            Toast.makeText(MainActivity.this, "Data received", Toast.LENGTH_SHORT).show();
//        }
//
//    }
}