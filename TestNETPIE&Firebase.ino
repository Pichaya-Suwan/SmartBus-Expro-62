#include <ESP8266WiFi.h>
#include <MicroGear.h>
#include <BH1750FVI.h>
#include <SPI.h>
#include <SD.h>
#include <FirebaseArduino.h>
//#include "FirebaseESP8266.h"

// WiFi Config
const char* ssid     = "マイルシナ";
const char* password = "0993651189";

// FireBase Config
#define FIREBASE_HOST "test-database-cd409.firebaseio.com"
#define FIREBASE_AUTH "yY4naBUfgLG4uhpUbEOJvWZHSaWBESu3YTi0vXzv"

//NETPIE Config
#define APPID   "SmartBusu"
#define KEY     "MZJlQB4lLA8mf7y"
#define SECRET  "98Zltj3ZA3A4q1G02mzHGLiti"
#define ALIAS   "esp8266"

WiFiClient client;

int timer = 0;
MicroGear microgear(client);

int count = 1;

/* If a new message arrives, do this */
void onMsghandler(char *topic, uint8_t* msg, unsigned int msglen) {
    Serial.print("Incoming message --> ");
    msg[msglen] = '\0';
    Serial.println((char *)msg);
}

void onFoundgear(char *attribute, uint8_t* msg, unsigned int msglen) {
    Serial.print("Found new member --> ");
    for (int i=0; i<msglen; i++)
        Serial.print((char)msg[i]);
    Serial.println();  
}

void onLostgear(char *attribute, uint8_t* msg, unsigned int msglen) {
    Serial.print("Lost member --> ");
    for (int i=0; i<msglen; i++)
        Serial.print((char)msg[i]);
    Serial.println();
}

/* When a microgear is connected, do this */
void onConnected(char *attribute, uint8_t* msg, unsigned int msglen) {
    Serial.println("Connected to NETPIE...");
    /* Set the alias of this microgear ALIAS */
    microgear.setAlias(ALIAS);
}


void setup() {
    /* Add Event listeners */
    /* Call onMsghandler() when new message arraives */
    microgear.on(MESSAGE,onMsghandler);

    /* Call onFoundgear() when new gear appear */
    microgear.on(PRESENT,onFoundgear);

    /* Call onLostgear() when some gear goes offline */
    microgear.on(ABSENT,onLostgear);

    /* Call onConnected() when NETPIE connection is established */
    microgear.on(CONNECTED,onConnected);

    Serial.begin(115200);
    Serial.println("Starting...");

    /* Initial WIFI, this is just a basic method to configure WIFI on ESP8266.                       */
    /* You may want to use other method that is more complicated, but provide better user experience */
    if (WiFi.begin(ssid, password)) {
        while (WiFi.status() != WL_CONNECTED) {
            delay(500);
            Serial.print(".");
        }
    }

    Serial.println("WiFi connected");  
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());

    /* Initial with KEY, SECRET and also set the ALIAS here */
    microgear.init(KEY,SECRET,ALIAS);

    /* connect to NETPIE to a specific APPID */
    microgear.connect(APPID);


    Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);

    
}

void loop() {
    /* To check if the microgear is still connected */
    if (microgear.connected()) {
        Serial.println("connected");

        /* Call this method regularly otherwise the connection may be lost */
        microgear.loop();

        if (timer >= 1000) {
            Serial.println("Publish...");

            /* Chat with the microgear named ALIAS which is myself */
            microgear.chat(ALIAS,count);
            timer = 0;
        } 
        else timer += 100;
    }
    else {
        Serial.println("connection lost, reconnect...");
        if (timer >= 5000) {
            microgear.connect(APPID);
            timer = 0;
        }
        else timer += 100;
    }

    Firebase.setInt("num", count);
    if (Firebase.failed()) {
      Serial.print("set /number failed:");
      Serial.println(Firebase.error());
      delay(500);  
      return;
    }
    Serial.print("set /number to ");
    Serial.println(Firebase.getInt("num"));
    count++;
    delay(1000);
}
