package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    public TextView txv,TDS,O2,PH,temp;
    private final String TAG = "Mqtt";
    /* 设备三元组信息 */
    final private String PRODUCTKEY = "ibbkznChKyz";
    final private String DEVICENAME = "apps";
    final private String DEVICESECRET = "73556a3dab1f6ce4b9fd7725d962c2b8";

    /* 阿里云Mqtt服务器域名 */
    final String host = "tcp://" + PRODUCTKEY + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:443";
    private String clientId;
    private String userName;
    private String passWord;

    MqttAndroidClient mqttAndroidClient;
    /* 自动Topic, 用于上报消息 */
    final private String PUB_TOPIC="/sys/"+PRODUCTKEY+"/"+DEVICENAME+"/thing/event/property/post";
   // final private String PUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/update";
    /* 自动Topic, 用于接受消息 */
    final private String SUB_TOPIC = "/sys/"+PRODUCTKEY+"/"+DEVICENAME+"/thing/service/property/set";
   //final private String SUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/get";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txv = (TextView) findViewById(R.id.txv);
        TDS = (TextView) findViewById(R.id.TDS);
        O2 = (TextView) findViewById(R.id.O2);
        temp = (TextView)findViewById(R.id.temp);
        PH = (TextView) findViewById(R.id.PH);
        try {
            mqtt_init();
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }
    private void mqtt_init() throws MqttException {
        /* 获取Mqtt建连信息clientId, username, password */
        AiotMqttOption aiotMqttOption = new AiotMqttOption().getMqttOption(PRODUCTKEY, DEVICENAME, DEVICESECRET);
        if (aiotMqttOption == null) {
            Log.e(TAG, "device info error");
        } else {
            clientId = aiotMqttOption.getClientId();
            userName = aiotMqttOption.getUsername();
            passWord = aiotMqttOption.getPassword();
        }
        /* 创建MqttConnectOptions对象并配置username和password */
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(passWord.toCharArray());

        /* 创建MqttAndroidClient对象, 并设置回调接口 */
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), host, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "connection lost");
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG, "topic: " + topic + ", msg: " + new String(message.getPayload()));
                Toast.makeText(MainActivity.this,message.toString(), Toast.LENGTH_SHORT).show();
               // txv.setText();接收消息，解析JSON数据
               // txv.setText(message.toString());
                try{
                     String str = message.toString();
                     JSONObject jsonObject = new JSONObject(str);      //新建一个Json对象
                     String par = jsonObject.getString("items"); //将items键值转为json对象

                     JSONObject jsonObject_temp = new JSONObject(par);
                     JSONObject jsonObject_ph = new JSONObject(par);
//                     JSONObject jsonObject_o2 = new JSONObject(par);
                     JSONObject jsonObject_tds = new JSONObject(par);

                     String temper = jsonObject_temp.getString("temperature");//获取温度下键值
                     JSONObject jsonObject2 = new JSONObject(temper);
                     float temp_val_f = (float) jsonObject2.getDouble("value");

                    String ph_val_s = jsonObject_temp.getString("pH");           //获取PH下键值
                    JSONObject second_ph_json = new JSONObject(ph_val_s);
                    float ph_val_f = (float) second_ph_json.getDouble("value");

                    String o2_val_s = jsonObject_temp.getString("O2");
                    JSONObject second_O2_json = new JSONObject(o2_val_s);
                    float o2_val_f = (float) second_O2_json.getDouble("value");

                    String tds_val_s = jsonObject_tds.getString("TDS");
                    JSONObject second_tds_json = new JSONObject(tds_val_s);
                    float tds_val_f = (float) second_tds_json.getDouble("value");
                   //txv.setText(String.valueOf(tds_val_f)+"ppm");

                    TDS.setText(String.valueOf(tds_val_f)+"ppm");
                    O2.setText(String.valueOf(o2_val_f)+"mg\\L");
                    temp.setText(String.valueOf(temp_val_f)+"℃");
                    PH.setText(String.valueOf(ph_val_f));

                } catch (Exception e) {

                    throw new RuntimeException(e);
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "msg delivered");
            }
        });
        /* Mqtt建连 */
        try {
            mqttAndroidClient.connect(mqttConnectOptions,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "connect succeed");
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    subscribeTopic(SUB_TOPIC);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "connect failed");
                    Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        /* 通过按键发布消息 */
        Button pubButton = findViewById(R.id.publish);

        Button btn = findViewById(R.id.btn);  //btn关灯

        String str_on = "{\"params\":{\"Led\":0}}";
        String str_off = "{\"params\":{\"Led\":1}}";
        pubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              //  publishMessage("12");
                publishMessage(str_on);
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage(str_off);
            }
        });
    }
    /**
     * 订阅特定的主题
     * @param topic mqtt主题
     */
    public void subscribeTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribed succeed");
                    Toast.makeText(MainActivity.this, "订阅成功", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribed failed");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    /**
     * 向默认的主题/user/update发布消息
     * @param payload 消息载荷
     */
    public void publishMessage(String payload) {
        try {
            if (mqttAndroidClient.isConnected() == false) {
                mqttAndroidClient.connect();
            }
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(PUB_TOPIC, message,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "publish succeed!");
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "publish failed!");
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }
}