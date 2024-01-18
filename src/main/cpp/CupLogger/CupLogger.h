#ifndef CUPLOGGER_H
#define CUPLOGGER_H

unsigned long noTimeDelta();

void logEnv(const char* env = ",,", unsigned long (*timeFunc)() = noTimeDelta);

void log(const char* text);
void log(const char* text, int value);
void reading(const char* name, int value);
void reading(const char* name, double value);
void readings(const char* name1, double value1, const char* name2, double value2);
void readings(const char* name1, double value1, const char* name2, double value2, const char* name3, double value3);
void readings(const char* name1, double value1, const char* name2, double value2,
              const char* name3, double value3, const char* name4, double value4);

#endif // CUPLOGGER_H