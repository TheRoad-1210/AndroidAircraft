package com.example.androidaircraft.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.androidaircraft.R;
import com.example.androidaircraft.application.PlayerAdapter;
import com.example.androidaircraft.player.Player;
import com.example.androidaircraft.player.PlayerDaoImpl;
import com.example.androidaircraft.util.CompareScore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RankListActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayList<Player> datas = new ArrayList<>();
    private PlayerAdapter playerAdapter ;
    private Socket socket;
    private PrintWriter out;
    private PlayerDaoImpl playerDao =  new PlayerDaoImpl();
    private BufferedReader s;

    private static Object LOCK;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rank_list);

        initDatas();


        listView = findViewById(R.id.rank_list);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                AlertDialog alertDialog = new AlertDialog.Builder(RankListActivity.this)
                        .setTitle("???????????????????????????")
                        .setMessage("???????????????????????????")
                        .setPositiveButton("??????", new DialogInterface.OnClickListener() {//??????"Yes"??????
                            @Override
                            public void onClick(DialogInterface dialogInterface, int j) {

                                datas.remove(i);
                                playerAdapter.notifyDataSetChanged();
                                new Thread(new Out()).start();
                            }
                        })

                        .setNegativeButton("??????", new DialogInterface.OnClickListener() {//????????????
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .create();
                alertDialog.show();
            }
        });
    }

    public class Connect implements Runnable{

        @Override
        public void run() {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(MainActivity.IP,9999),5000);

                /**
                 * ???????????????????????????
                 */

                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),true);
                out.println("rankList" + "");
                /**
                 * ??????????????????????????????????????????????????????
                 */
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                try {
                        playerDao.players.addAll ((ArrayList<Player>) in.readObject());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                datas.addAll(playerDao.players);

                /*
                 * ??????????????????????????????
                 */
                boolean flag = true;
                for (Player p : datas){
                    if(p.time.equals(Player.getInstance().time)){
                        flag = false;
                        break;
                    }
                }
                if (flag){
                    datas.add(Player.getInstance());
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            new Thread(new Out()).start();
            //??????
            CompareScore cs = new CompareScore();
            datas.sort(cs);

            playerAdapter= new PlayerAdapter(RankListActivity.this,datas);
            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    listView.setAdapter(playerAdapter);
                }
            });

            /**
             * ??????????????????
             */
            for(Player p :datas){
                System.out.println(p.name+"\n");
            }
        }
    }

    public class Out implements Runnable{

        @Override
        public void run() {
            socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(MainActivity.IP,9999),5000);
                  /*
                ??????????????????????????????
                 */
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),true);
                out.println("get rankList"+"");
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                /*
                ?????????????????????
                 */
                s = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
                String i = s.readLine();

                if (i != null){

                    oos.writeObject(datas);//????????????????????????
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread(new Save()).start();

        }
    }

    public class Save implements Runnable{

        @Override
        public void run() {
            socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(MainActivity.IP,9999),5000);
                  /*
                ??????????????????????????????
                 */
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),true);
                out.println("save"+"");
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                /*
                ?????????????????????
                 */
                s = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
                String i = s.readLine();

                for(Player p : RegisterActivity.save){
                    if(p.name.equals(Player.getInstance().name)){
                        RegisterActivity.save.remove(p);
                        RegisterActivity.save.add(Player.getInstance());
                        break;
                    }
                }

                if (i != null){
                    oos.writeObject(RegisterActivity.save);//????????????????????????
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void initDatas(){
        new Thread(new Connect()).start();
    }

}
