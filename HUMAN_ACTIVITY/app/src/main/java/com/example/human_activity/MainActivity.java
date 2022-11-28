package com.example.human_activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tensorflow.Tensor;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.DoubleAccumulator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ListAdapter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import com.example.human_activity.ml.Coverted;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    Button turnonbtbutton,view1,view2;
    ListView list;
    String model ="HC-06";
    ArrayList devlist = new ArrayList();
    ArrayList devlistMac = new ArrayList();
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private static final String TAG = "MainActivity";

    private static List<Float> ax,ay,az;
    private static List<Float> gx,gy,gz;
    private  static  List<String> temp;
    private static final int TIME_STAMP = 250;
    private ActivityClassifier classifier;
    private float[] results;

    Interpreter tflite;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
        } catch (Exception ex){
            ex.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null){
            Toast.makeText(this,"Bluetooth not supported",Toast.LENGTH_SHORT).show();
            finish();
        }
        initLayout1();

        turnonbtbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turnonbt();
                selectDevice();

            }
        });

    }

    private void selectDevice() {
        if(btAdapter.isEnabled()){
            setContentView(R.layout.layout2);
            initLayout2();
            list();
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    checkBTdevice(i);

                }
            });
        }

    }

    private void checkBTdevice(int i) {
        if (btAdapter.getRemoteDevice(devlistMac.get(i).toString()).getName().equals(model)){
            setContentView(R.layout.layout4);
            initLayout4();
            readData(devlistMac.get(i));
        } else{
            Toast.makeText(MainActivity.this,"Please select a viable HAR sensor",Toast.LENGTH_SHORT).show();

        }
    }

    private void readData(Object o) {
        BluetoothSocket btSocket = null;
        int counter = 0;

        do {
            try {
                btSocket = btAdapter.getRemoteDevice(o.toString()).createRfcommSocketToServiceRecord(mUUID);
                System.out.println(btSocket);
                btSocket.connect();
                System.out.println(btSocket.isConnected());
            } catch (IOException e) {
                System.out.println("Fail");
                Toast.makeText(MainActivity.this,"Failed to connect",Toast.LENGTH_SHORT).show();
            }
            counter++;
        } while(!btSocket.isConnected() && counter < 3);

        InputStream inputStream = null;
        try{


            inputStream = btSocket.getInputStream();
            inputStream.skip(inputStream.available());

            int column = 0;
            int start = 0;
            long startTime = 0;
            ax=new ArrayList<>(); ay=new ArrayList<>(); az=new ArrayList<>();
            gx=new ArrayList<>(); gy=new ArrayList<>(); gz=new ArrayList<>();
            temp = new ArrayList<>();

            while(1==1){

                byte b = (byte)inputStream.read();
                //System.out.print((char)b);

                if (start != 0) {
                    if ((char) b == ',') {

                        if (column==0){
                            ax.add(Float.parseFloat(String.join(",",temp).replace(",","")));

                        }else if(column==1){
                            ay.add(Float.parseFloat(String.join(",",temp).replace(",","")));
                        }else if(column==2){
                            az.add(Float.parseFloat(String.join(",",temp).replace(",","")));
                        }else if(column==3){
                            gx.add(Float.parseFloat(String.join(",",temp).replace(",","")));
                        }else if(column==4){
                            gy.add(Float.parseFloat(String.join(",",temp).replace(",","")));
                        }
                        // System.out.println(ax);
                        column++;
                        temp.clear();
                    } else if ((char) b == '!') {
                        gz.add(Float.parseFloat(String.join(",",temp).replace(",","")));
                        temp.clear();
                        column = 0;
                        if (ax.size()>= TIME_STAMP){
                            long endTime = System.currentTimeMillis();
                            System.out.println("Total execution time: " + (endTime - startTime));
                        }
                        predictActivity();
                        startTime = System.currentTimeMillis();

                    } else {
                        if ((char) b != '\n'){
                            temp.add(String.valueOf((char) b));
                        }
                    }

                }
                if ((char) b == '\n'){
                    start = 1;

                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        try {
            btSocket.close();
            System.out.println(btSocket.isConnected());
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    private void initLayout1() {
        turnonbtbutton = findViewById(R.id.turnOnBt);
    }
    private void initLayout2() {
        list = findViewById(R.id.list_of_devices);
    }
    private void initLayout3() {
        view1 = findViewById(R.id.button1L3);
        view2 = findViewById(R.id.button2L4);
    }
    private void initLayout4() {

        view2 = findViewById(R.id.button2L4);

    }
    public void turnonbt() {
        Intent getVisable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(getVisable,0);
    }
    public void list() {
        pairedDevices = btAdapter.getBondedDevices();


        for(BluetoothDevice bt: pairedDevices) {
            devlistMac.add(bt);
            devlist.add(bt.getName());
        }
        Toast.makeText(this,"Showing devices",Toast.LENGTH_SHORT);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,devlist);
        list.setAdapter(adapter);
    }
    private void predictActivity() {

        List<Float> data=new ArrayList<>();
        if (ax.size() >= TIME_STAMP && ay.size() >= TIME_STAMP && az.size() >= TIME_STAMP
                && gx.size() >= TIME_STAMP && gy.size() >= TIME_STAMP && gz.size() >= TIME_STAMP) {
            data.addAll(ax.subList(0,TIME_STAMP));
            data.addAll(ay.subList(0,TIME_STAMP));
            data.addAll(az.subList(0,TIME_STAMP));

            data.addAll(gx.subList(0,TIME_STAMP));
            data.addAll(gy.subList(0,TIME_STAMP));
            data.addAll(gz.subList(0,TIME_STAMP));

            System.out.println(data);
            System.out.println(data.size());

            Log.d(TAG, "predictActivities: Data in data (combined)" + data);
            try {
                Coverted model = Coverted.newInstance(getApplicationContext());
            float[] input = toFloatArray(data);
            //Log.d(TAG, "predictActivities: toFloatArray: " +  input.);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, TIME_STAMP, 6}, DataType.FLOAT32);

            // creating the TensorBuffer for inputting the float array
            //TensorBuffer tensorBuffer = TensorBuffer.createDynamic(DataType.FLOAT32);
            //tensorBuffer.loadArray(toFloatArray(data));

            // ByteBuffer byteBuffer = tensorBuffer.getBuffer();
            inputFeature0.loadArray(toFloatArray(data));


            // Runs model inference and gets result.
                Coverted.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                // Releases model resources if no longer used.
                System.out.println("Predictions");
                System.out.print(outputFeature0.getFloatArray()[0]);
                System.out.println("downstairs");
                System.out.print(outputFeature0.getFloatArray()[1]);
                System.out.println("sitting");
                System.out.print(outputFeature0.getFloatArray()[2]);
                System.out.println("standing");
                System.out.print(outputFeature0.getFloatArray()[3]);
                System.out.println("upstair");
                System.out.print(outputFeature0.getFloatArray()[4]);
                System.out.println("walking");
                model.close();


                //clear the list for the next prediction



            } catch (IOException e) {
                // TODO Handle the exception
            }

            ax.clear(); ay.clear(); az.clear();
            gx.clear(); gy.clear(); gz.clear();
            data.clear();


        }
    }

    private float round(float value, int decimal_places) {
        BigDecimal bigDecimal=new BigDecimal(Float.toString(value));
        bigDecimal = bigDecimal.setScale(decimal_places, BigDecimal.ROUND_HALF_UP);
        return bigDecimal.floatValue();
    }

    private float[] toFloatArray(List<Float> data){
        int i = 0;

        float[] array = new float[data.size()];
        for (Float f: data){
            array[i++] = (f !=null ? f: Float.NaN);
        }
        //Log.d(TAG, "toFloatArray: " + array);
        return array;
    }

}