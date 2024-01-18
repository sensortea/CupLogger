// Example where logging helpers copied from CupLogger.cpp
/*** Logging helpers START ***/
inline int serialPrint(char val) {
  return Serial.print(val);
}

inline int serialPrint(const char* val) {
  return Serial.print(val);
}

inline int serialPrint(int val) {
  return Serial.print(val);
}

inline int serialPrint(long unsigned int val) {
  return Serial.print(val);
}

inline int serialPrint(double val) {
  return Serial.print(val);
}

inline void serialPrintln(int val) {
  Serial.println(val);
}

unsigned long noTimeDelta() {
    return 0;
}

char* _logEnv;

unsigned long (*_logDeltaTimeFunc)();
unsigned long prevTime = 0;

void logEnv(const char* env = ",,", unsigned long (*timeFunc)() = noTimeDelta) {
   _logEnv = env;
   _logDeltaTimeFunc = timeFunc;
}

int logStart() {
  unsigned long curTime = _logDeltaTimeFunc();
  int len = serialPrint(_logEnv);
  len += serialPrint(',');
  if (curTime - prevTime != 0) { // todo: is this worth saving 1 printed byte?
      len += serialPrint(curTime - prevTime);
      prevTime = curTime;
  }
  len += serialPrint(',');
  return len;
}

void logEnd(int len) {
  serialPrint(',');
  serialPrintln(len);
}

void log(const char* text) {
  int len = logStart();
  len += serialPrint(text);
  logEnd(len);
}

int logReadingName(const char* name) {
  int len = serialPrint(',');
  len += serialPrint(name);
  len += serialPrint(':');
  return len;
}

void reading(const char* name, double value) {
  int len = logStart();
  len += logReadingName(name);
  len += serialPrint(value);
  logEnd(len);
}
/*** Logging helpers END ***/

void setup() {
  Serial.begin(9600);
  logEnv("MyProgram,1.0,DHT11", millis);
  log("Setup is done!");
}

void loop() {
  reading("temperature", 15.0);
  delay(1000);
}
