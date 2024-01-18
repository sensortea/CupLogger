import cuplogger

baseDir = '/tmp/base_dir'
serialNumber = '242353135363516111A2'
startEpochMs = 1705318466234  # Example start time in epoch milliseconds
endEpochMs = 1705418466234    # Example end time in epoch milliseconds

# 1. Find files

# returns ['data/2024_01_15/242353135363516111A2/2024_01_15_113426.txt', ...]
files = cuplogger.findDataFiles(baseDir, serialNumber, startEpochMs, endEpochMs)
print(files)

# 2a. Parse individual lines

# returns DataRecord(epoch_ms=1705318467715, raw_text='1705318467715,Loop #0\n', parsed_event=None)
record = cuplogger.parse_record('1705318467715,Loop #0\n')
print(record)

# 2b. Scan records

# prints
# DataRecord(epoch_ms=1705318467715, raw_text='Loop #0\n', parsed_event=None)
# ...
cuplogger.scan(lambda record: print(record), baseDir, serialNumber, startEpochMs, endEpochMs)