char* logEnv = "MyProgram,1.0,DHT11,";

void log(const char* message) {
  int len = Serial.print(logEnv);
  len += Serial.print(","); // skipping timeDelta
  len += Serial.print(message);
  Serial.print(",");
  Serial.println(len);
}

void reading(const char* name, float value) {
  int len = Serial.print(logEnv);
  len += Serial.print(",,"); // skipping timeDelta and log message
  len += Serial.print(name);
  len += Serial.print(":");
  len += Serial.print(value);
  Serial.print(",");
  Serial.println(len);
}

void setup() {
  Serial.begin(9600);
  log("Setup is done!");
}

void loop() {
  reading("temperature", 15);
  delay(1000);
}