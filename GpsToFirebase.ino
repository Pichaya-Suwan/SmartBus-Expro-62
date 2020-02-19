#include <SoftwareSerial.h>
#include <TinyGPS.h>
#include <ESP8266WiFi.h>
#include <MicroGear.h>
#include <SPI.h>
#include <SD.h>
#include <FirebaseArduino.h>

// WiFi Config
const char* ssid     = "";
const char* password = "";

// FireBase Config
#define FIREBASE_HOST "test-database-cd409.firebaseio.com"
#define FIREBASE_AUTH "yY4naBUfgLG4uhpUbEOJvWZHSaWBESu3YTi0vXzv"

WiFiClient client;


/* This sample code demonstrates the normal use of a TinyGPS object.
   It requires the use of SoftwareSerial, and assumes that you have a
   4800-baud serial GPS device hooked up on pins 4(rx) and 3(tx).
*/

TinyGPS gps;
SoftwareSerial ss(4, 3);

void setup()
{
  Serial.begin(115200);
  ss.begin(4800);

  if (WiFi.begin(ssid, password)) {
        while (WiFi.status() != WL_CONNECTED) {
            delay(500);
            Serial.print(".");
        }
    }

    Serial.println("WiFi connected");  
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());


    Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);


}

void loop()
{
  bool newData = false;
  unsigned long chars;
  unsigned short sentences, failed;

  // For one second we parse GPS data and report some key values
  for (unsigned long start = millis(); millis() - start < 1000;)
  {
    while (ss.available())
    {
      char c = ss.read();
      // Serial.write(c); // uncomment this line if you want to see the GPS data flowing
      if (gps.encode(c)) // Did a new valid sentence come in?
        newData = true;
    }
  }

  if (newData)
  {
    float flat, flon;
    unsigned long age;
    gps.f_get_position(&flat, &flon, &age);
    Serial.print("LAT=");
    Serial.print(flat == TinyGPS::GPS_INVALID_F_ANGLE ? 0.0 : flat, 6);
    Serial.print(" LON=");
    Serial.print(flon == TinyGPS::GPS_INVALID_F_ANGLE ? 0.0 : flon, 6);
     
     Firebase.setString("bus_1", String(flat)+"/"+String(flon));
    if (Firebase.failed()) {
      Serial.print("set /number failed:");
      Serial.println(Firebase.error());
      delay(500);  
      return;
    }
    Serial.print("set /number to ");
    Serial.println(Firebase.getString("bus_1"));  
  }
  

  
}
