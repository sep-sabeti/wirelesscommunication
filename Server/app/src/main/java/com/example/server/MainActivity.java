package com.example.server;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    public static String messageToBeSent;
    public static String messageReceived;
    public static int PORT_NUMBER;
    public static Socket socket = null;
    public static final byte[] buffer = new byte[1024];
    public static ServerSocket serverSocket = null;
    public static final int iteration = 1000;
    EditText message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView text = findViewById(R.id.recievedText);
        EditText portNumber = findViewById(R.id.portNumber);
        Button communicate = findViewById(R.id.communicate);
        Button network = findViewById(R.id.network);
        Button status = findViewById(R.id.status);
        Button reset = findViewById(R.id.reset);
        Handler handler = new Handler();
        message = findViewById(R.id.sendText);
        setTitle("Server");

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        PORT_NUMBER = Integer.parseInt(portNumber.getText().toString());

        network.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serverSocket != null){
                    if(serverSocket.isBound()){
                        Toast.makeText(MainActivity.this, "Already connected to port  " + serverSocket.getLocalPort() , Toast.LENGTH_SHORT).show();
                    } else if(!serverSocket.isBound()){
                        try {
                            serverSocket.close();
                            Toast.makeText(MainActivity.this, "Closed the unbound serverSocket" , Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Serversocket cannot be unbound" , Toast.LENGTH_SHORT).show();
                        }
                    }

                } else{
                    try{
                        Toast.makeText(MainActivity.this,"Starting server",Toast.LENGTH_LONG).show();
                        Server server = new Server();
                        server.run();
                    }catch (Exception e){
                        Toast.makeText(MainActivity.this,"Server not getting started",Toast.LENGTH_LONG).show();
                    }
                    }
            }
        });


        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serverSocket != null){
                    try {
                        serverSocket.close();
                        Toast.makeText(MainActivity.this, "Closed server socket", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(socket !=null){
                    try {
                        socket.close();
                        Toast.makeText(MainActivity.this, "Closed socket", Toast.LENGTH_SHORT).show();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(socket == null && serverSocket == null){
                    Toast.makeText(MainActivity.this, "everything is null", Toast.LENGTH_SHORT).show();
                }
               else if(socket == null && serverSocket != null ){
                   if(!String.valueOf(serverSocket.getLocalPort()).equals(PORT_NUMBER)){
                       try {
                           socket = serverSocket.accept();
                           Toast.makeText(MainActivity.this, "socket is created on port " + socket.getLocalPort(), Toast.LENGTH_SHORT).show();
                       } catch (IOException e) {
                           e.printStackTrace();
                           Toast.makeText(MainActivity.this, "socket creation failed", Toast.LENGTH_SHORT).show();
                       }
                   }

                }
                else if (socket != null && socket.isConnected()) {
                    Toast.makeText(MainActivity.this, "Connected to port" + socket.getLocalPort(), Toast.LENGTH_SHORT).show();
                } else {
                        Toast.makeText(MainActivity.this, "Failure", Toast.LENGTH_SHORT).show();
                    }
                }
        });

        communicate.setOnClickListener(v -> {
            int count = 0;
            if(socket != null && socket.isConnected()){
                long Time0 = System.currentTimeMillis();
                while(count< iteration){
                    try {
                        SendData sendData = new SendData();
                        sendData.execute();
                        ReceiveData receiveData = new ReceiveData();
                        receiveData.execute();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,"SendData failed",Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    count++;
                }
                long Time1 = System.currentTimeMillis();
                handler.post(() -> text.setText(messageReceived));
                Toast.makeText(MainActivity.this,String.valueOf(Time1 - Time0) + "  " + count ,Toast.LENGTH_LONG).show();
            } else{
                Toast.makeText(MainActivity.this,"Not listening to this connection",Toast.LENGTH_LONG).show();
            }
        });
    }

    class Server implements Runnable {
        public Server(){
        }
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT_NUMBER);
                Toast.makeText(MainActivity.this,"Started listening on port " + PORT_NUMBER ,Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this,"ServerSocket creation failed",Toast.LENGTH_LONG).show();
            }
        }
    }

    class SendData extends AsyncTask<Void,Void,Void> {
        public SendData() {
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                messageToBeSent = message.getText().toString();
                DataOutputStream dataOutputStream;
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(messageToBeSent);
                Toast.makeText(MainActivity.this, "SendData succeeded", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

     class ReceiveData extends AsyncTask<Void,Void,Void>{
        public ReceiveData(){
        }
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                int bytes;
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                bytes = dataInputStream.read(buffer);
                messageReceived = new String((byte[]) buffer, 0, bytes);
                Toast.makeText(MainActivity.this,"ReceiveData succeeded",Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
               }
            return null;
        }
    }
}

