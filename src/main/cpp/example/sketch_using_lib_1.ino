#include "CupLogger.h"

void setup() {
  Serial.begin(9600);
  logEnv("MyProgram,1.0,DHT11", millis); // optional
  log("Setup is done!");
}

void loop() {
  reading("temperature", 15);
  delay(1000);
}