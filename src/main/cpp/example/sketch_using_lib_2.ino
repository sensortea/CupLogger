// Example with a bit more going on
#include "CupLogger.h"

const char* TEMP = "temp";
const char* HUMI = "humi";
const char* SIN = "sin";
const char* TAN = "tan";

void setup() {
  // Override default logging config
  logEnv("Temp+Humi,1.1,DHT11", millis);

  // Configure to use serial port
  Serial.begin(9600);
}

int i = 0;

void loop() {
  i++;
  if (i > 1000) {
    i = 0;
  }
  // Record readings
  readings(TEMP, 10 + i % 15, HUMI, 32 + i * i % 45);
  reading(SIN, 10 * sin(i * 0.5));
  reading(TAN, tan(i * 0.3));

  // Log message
  log("Inside loop #", i);

  delay(1000);
}