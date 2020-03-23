#include <TinyGPS++.h>
#include <SoftwareSerial.h>
#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>


// WiFi Config
const char* ssid     = "WIFI_SSID";
const char* password = "WIFI_PASSWORD";

// FireBase Config
#define FIREBASE_HOST "test-database-cd409.firebaseio.com"
#define FIREBASE_AUTH "yY4naBUfgLG4uhpUbEOJvWZHSaWBESu3YTi0vXzv"


WiFiClient client;

int s_check = 0;

static const int RXPin = 4, TXPin = 5;
static const uint32_t GPSBaud = 9600;

// The TinyGPS++ object
TinyGPSPlus gps;

// The serial connection to the GPS device
SoftwareSerial ss(RXPin, TXPin);

void setup()
{
  Serial.begin(115200);
  ss.begin(GPSBaud);
  WiFi.softAPdisconnect (true);
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
    if (Firebase.failed()) {
      Serial.print("Error connecting to Firebase");
      Serial.println(Firebase.error());
    }

  Serial.print(F("Testing TinyGPS++ library v. ")); Serial.println(TinyGPSPlus::libraryVersion());
  Serial.println();
}

void loop()
{
  // This sketch displays information every time a new sentence is correctly encoded.
  while (ss.available() > 0)
    if (gps.encode(ss.read()))
      displayInfo();

  if (millis() > 5000 && gps.charsProcessed() < 10)
  {
    Serial.println(F("No GPS detected: check wiring."));
    while(true);
  }
}

void displayInfo()
{
  Serial.print(F("Location: ")); 
  if (gps.location.isValid())
  {
    Serial.print(gps.location.lat(), 6);
    Serial.print(F(","));
    Serial.print(gps.location.lng(), 6);
    Serial.println();

    //set LatLng
    Firebase.setString("bus_1/LatLng", String(gps.location.lat(),6)+"/"+String(gps.location.lng(),6));
    if (Firebase.failed()) {
      Serial.print("set /LatLng failed:");
      Serial.println(Firebase.error());
      delay(1000);  
      return;
    }
    Serial.print("set /bus_1/LatLng to ");
    Serial.println(Firebase.getString("bus_1/LatLng"));
  }
  else
  {
    Serial.print(F("INVALID"));
  }

  Serial.println();

  Serial.print(F("Date : "));
  if (gps.date.isValid())
  {
    Serial.print(gps.date.month());
    Serial.print(F("/"));
    Serial.print(gps.date.day());
    Serial.print(F("/"));
    Serial.print(gps.date.year());

    //set Date
    Firebase.setString("bus_1/Date", String(gps.date.year()) + "/" + String(gps.date.month()) + "/" + String(gps.date.day()));
    if (Firebase.failed()) {
      Serial.print("set /Date failed:");
      Serial.println(Firebase.error());
      delay(1000);  
      return;
    }
    Serial.println();
    Serial.print("set /bus_1/Date to ");
    Serial.println(Firebase.getString("bus_1/Date"));
  
  }
  else
  {
    Serial.print(F("INVALID"));
  }


  Serial.print(F("Time : "));
  if (gps.time.isValid())
  {
    if (gps.time.hour() < 10) Serial.print(F("0"));
    Serial.print(gps.time.hour());
    Serial.print(F(":"));
    if (gps.time.minute() < 10) Serial.print(F("0"));
    Serial.print(gps.time.minute());
    Serial.print(F(":"));
    if (gps.time.second() < 10) Serial.print(F("0"));
    Serial.print(gps.time.second());

    //set Time
    Firebase.setString("bus_1/Time", String(gps.time.hour()+7) + ":" + String(gps.time.minute()) + ":" + String(gps.time.second()));
    if (Firebase.failed()) {
      Serial.print("set /Time failed:");
      Serial.println(Firebase.error());
      delay(1000);  
      return;
    }
    Serial.println();
    Serial.print("set /bus_1/Time to ");
    Serial.println(Firebase.getString("bus_1/Time"));
  }
  else
  {
    Serial.print(F("INVALID"));
  }

  Serial.println();

  if(s_check == 0){
    s_check = 1;  
  }
  else{
    s_check = 0;  
  }
  Firebase.setInt("status", s_check);
    if (Firebase.failed()) {
      Serial.print("set /status failed:");
      Serial.println(Firebase.error());
      delay(1000);  
      return;
    }

  Serial.println();  
  
  delay(1000);
}
