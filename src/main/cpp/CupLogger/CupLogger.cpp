#include "CupLogger.h"
#include "Arduino.h"

// replace these for non-Arduino (if Serial is not available)
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

char* _logEnv = ",,";

unsigned long noTimeDelta() {
    return 0;
}

unsigned long (*_logDeltaTimeFunc)() = noTimeDelta;
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

void log(const char* text, int value) {
  int len = logStart();
  len += serialPrint(text);
  len += serialPrint(value);
  logEnd(len);
}

void log(const char* text, double value) {
  int len = logStart();
  len += serialPrint(text);
  len += serialPrint(value);
  logEnd(len);
}

int logReadingName(const char* name) {
  int len = serialPrint(',');
  len += serialPrint(name);
  len += serialPrint(':');
  return len;
}

void reading(const char* name, int value) {
  int len = logStart();
  len += logReadingName(name);
  len += serialPrint(value);
  logEnd(len);
}

void reading(const char* name, double value) {
  int len = logStart();
  len += logReadingName(name);
  len += serialPrint(value);
  logEnd(len);
}

void readings(const char* name1, double value1,
              const char* name2, double value2) {
  int len = logStart();
  len += logReadingName(name1);
  len += serialPrint(value1);
  len += logReadingName(name2);
  len += serialPrint(value2);
  logEnd(len);
}

void readings(const char* name1, double value1,
              const char* name2, double value2,
              const char* name3, double value3) {
  int len = logStart();
  len += logReadingName(name1);
  len += serialPrint(value1);
  len += logReadingName(name2);
  len += serialPrint(value2);
  len += logReadingName(name3);
  len += serialPrint(value3);
  logEnd(len);
}

void readings(const char* name1, double value1, const char* name2, double value2,
              const char* name3, double value3, const char* name4, double value4) {
  int len = logStart();
  len += logReadingName(name1);
  len += serialPrint(value1);
  len += logReadingName(name2);
  len += serialPrint(value2);
  len += logReadingName(name3);
  len += serialPrint(value3);
  len += logReadingName(name4);
  len += serialPrint(value4);
  logEnd(len);
}