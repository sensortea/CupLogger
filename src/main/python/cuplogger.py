import os
import datetime
import time
from dataclasses import dataclass
from typing import Dict, List, Optional

def findDataFiles(baseDir, serialNumber, startEpochMs, endEpochMs, fileFormat = '.txt'):
    # adjust the end date for the 10-minute interval
    fileIntervalMs = 10 * 60 * 1000
    startEpochMs = (startEpochMs // fileIntervalMs) * fileIntervalMs
    endEpochMs = (endEpochMs // fileIntervalMs) * fileIntervalMs + fileIntervalMs - 1

    startDate = datetime.datetime.fromtimestamp(startEpochMs / 1000.0, tz=datetime.timezone.utc)
    endDate = datetime.datetime.fromtimestamp(endEpochMs / 1000.0, tz=datetime.timezone.utc)

    # Format the dates to match the directory and file structure
    startDir = startDate.strftime('%Y_%m_%d')
    endDir = endDate.strftime('%Y_%m_%d')
    startFile = startDate.strftime('%Y_%m_%d_%H%M%S')
    endFile = endDate.strftime('%Y_%m_%d_%H%M%S')

    result = []

    dataDir = os.path.join(baseDir, 'data')

    # List all directories in the base directory and filter based on the date range
    availableDirs = [d for d in os.listdir(dataDir) if os.path.isdir(os.path.join(dataDir, d))]
    filteredDirs = [d for d in availableDirs if startDir <= d <= endDir]

    for dirName in filteredDirs:
        dirPath = os.path.join(dataDir, dirName, serialNumber)

        # Check if the directory exists and iterate through files in the directory
        if os.path.exists(dirPath):
            for file in os.listdir(dirPath):
                if file.endswith(fileFormat):
                    # Use simple string comparison to check if the file's date and time are within the range
                    if startFile <= file <= endFile:
                        result.append(os.path.join(dirPath, file))

    return sorted(result)

@dataclass
class Event:
    program_id: str
    program_version: str
    device_config: str
    time_delta: int
    log_message: str
    readings: Dict[str, float]

@dataclass
class DataRecord:
    epoch_ms: int
    raw_text: Optional[str] = None
    parsed_event: Optional[Event] = None

def parse_record(line: str) -> Optional[DataRecord]:
    try:
        parts = line.split(",")
        try:
            epoch_ms = int(parts[0])
            raw_text = line[len(parts[0])+1:]
            if len(parts) < 7:
                return DataRecord(epoch_ms, raw_text=raw_text)

            readings = {r.split(":")[0]: float(r.split(":")[1]) for r in parts[6:-1]}

            length_check = int(parts[-1])
            if len(line) - len(parts[0]) - 1 - len(parts[-1]) - 1 != length_check:
                return DataRecord(epoch_ms, raw_text=raw_text)

            event = Event(parts[1], parts[2], parts[3], int(parts[4] or 0), parts[5], readings)
            return DataRecord(epoch_ms, parsed_event=event)
        except Exception as e:
            return DataRecord(epoch_ms, raw_text=raw_text)
    except Exception as e:
        return None

def scan(consumeFunction, baseDir, serialNumber, startEpochMs, endEpochMs, fileFormat = '.txt'):
    files = findDataFiles(baseDir, serialNumber, startEpochMs, endEpochMs, fileFormat)
    scanFiles(files, startEpochMs, endEpochMs, consumeFunction)

def scanFiles(filelist, startEpochMs, endEpochMs, consumeFunction):
    for file in filelist:
        with open(file, 'rb') as f:
            for line_bytes in f:
                try:
                    line = line_bytes.decode('utf-8')
                    record = parse_record(line)
                    if record and record.epoch_ms >= startEpochMs and record.epoch_ms < endEpochMs:
                        consumeFunction(record)
                except UnicodeDecodeError:
                    continue

if __name__ == "__main__":
    # Quick parse tests
    line = "1704573206646,Temp+Humi,1.1,DHT11,39,Inside loop #43,39" # wrong length
    record = parse_record(line)
    assert record.raw_text and not record.parsed_event;

    line = "1704573206646,Temp+Humi,1.1,DHT11_39,Inside loop #43,38" # bad comma
    record = parse_record(line)
    assert record.raw_text and not record.parsed_event;

    line = "1704573208216,Temp+Humi,1.1,DHT11,18,,tan,0.31,32" # bad reading (should be 'tan:')
    record = parse_record(line)
    assert record.raw_text and not record.parsed_event;

    line = "1704573209370,Temp+Humi,1.1,DHT11,39,Inside loop #2,37" # log line
    record = parse_record(line)
    assert not record.raw_text and record.parsed_event;

    line = "1704573208216,Temp+Humi,1.1,DHT11,18,,tan:0.31,32" # single reading
    record = parse_record(line)
    assert not record.raw_text and record.parsed_event;

    line = "1704573210329,Temp+Humi,1.1,DHT11,1043,,temp:13.00,humi:41.00,47" # multiple readings
    record = parse_record(line)
    assert not record.raw_text and record.parsed_event;
