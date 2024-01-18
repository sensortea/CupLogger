// data = {sn: data: [[epochMs, raw_line, event]], startEpochMs: , endEpochMs: } // todo: maybe parse on the fly to avoid high memory use?
// todo: more efficient event: [0:<programId>,1:<programVersion>,2:<deviceConfig>,3:<timeDelta>,4:<logMessage>, 5:{<readingName>: <readingValue>}]
let data = {};

// todo: think through error hanlding and propagating to user
async function fetchData(url, input = {}) {
    try {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(input)
        });

        if (!response.ok) {
            // Attempt to read the error message from the response
            const errorBody = await response.text();
            const errorMessage = errorBody ? errorBody + response.statusText : response.statusText;
            console.error(errorMessage);

            return null;
        }

        return await response;
    } catch (error) {
        console.error('Error fetching data:', error);
        // Handle the error appropriately
        // You might want to rethrow the error or return a default value
        return {};
    }
}

// todo: seems like we just need method that loads up until now millisecond (ie. one endEpochMs param or no param)
//       (-10ms maybe just in case ;))
// ensures we only load and append data after the epochMs of already loaded data
async function loadAndAppendData(sn, startEpochMs, endEpochMs) {
    let snData = data[sn].data;
    // NOTE: assuming we always append to the end;
    let currEndEpochMs = data[sn].maxEpochMs;
    // NOTE: just trying to fill in data since last fetched datapoint
    startEpochMs = currEndEpochMs === 0 ? startEpochMs : currEndEpochMs;
    if (startEpochMs >= endEpochMs) {
        // nothing new to load
        return;
    }
    var response = await fetchData(
        httpEndpoint + '/getData',
        {
            serialNumber: sn,
            startEpochMs: startEpochMs,
            endEpochMs: endEpochMs
        }) || "[]";
    var fetched = await response.json();
    data[sn].fetchedBytes += parseInt(response.headers.get("Content-Length"));

    // Remove all entries from startEpochMs onwards (previous load could load partial data)
    while (snData.length > 0 && snData[snData.length - 1][0] >= startEpochMs) {
        snData.pop();
    }
    fetched.forEach(dataEvent => {
        currEndEpochMs = currEndEpochMs < dataEvent.epochMs ? dataEvent.epochMs : currEndEpochMs;
        snData.push([dataEvent.epochMs, dataEvent.rawText, dataEvent.parsedEvent]);
    });
    data[sn].maxEpochMs = currEndEpochMs;
}

async function loadData(sn, startEpochMs, endEpochMs) {
    data[sn] = {data: [], minEpochMs: 0, maxEpochMs: 0, fetchedBytes: 0};
    // todo: always load at least whole timeline interval to make sure timeline shows correctly?
    await loadAndAppendData(sn, startEpochMs, endEpochMs);
}

function getIndex(arr, epochMs) {
    let start = 0;
    let end = arr.length - 1;
    let closestIndex = -1;

    while (start <= end) {
        let mid = Math.floor((start + end) / 2);
        let currentEpochMs = arr[mid][0];

        if (currentEpochMs === epochMs) {
            return mid; // Found the exact match
        } else if (currentEpochMs < epochMs) {
            closestIndex = mid; // Update closest index
            start = mid + 1;
        } else {
            end = mid - 1;
        }
    }

    // If the exact match wasn't found, closestIndex will have the index of the entry just before the target epochMs
    return closestIndex;
}

// scannerFunc(epochMs, rawText, parsedEvent or null)
function scanLoadedData(sn, startEpochMs, endEpochMs, scannerFunc) {
    let arr = data[sn].data;
    if (arr === undefined) {
        return;
    }
    // todo: check min max epochMs in data[sn] and maybe exit early
    let startIdx = getIndex(arr, startEpochMs);
    // advance to +1 if not exact epochMs found
    if (startIdx === -1 || arr[startIdx][0] < startEpochMs) {
        startIdx += 1;
    }
    let endIdx = getIndex(arr, endEpochMs);
    for (var i = startIdx; i <= endIdx; i++) {
        let r = arr[i];
        scannerFunc(r[0], r[1], r[2]);
    }
}

function getTimeline(sn, startEpochMs, endEpochMs) {
    // [[epochMs, dataAvail=1, update=""]]
    let points = [];
    let intervalMs;
    // picking resolution based on time range. needs to be a bit bigger than default intervals, as we append to them
    if (startEpochMs - endEpochMs <= 8 * HOUR_IN_MS) {
        intervalMs = 10 * 1000;
    } else if (startEpochMs - endEpochMs <= 2 * DAY_IN_MS) {
        intervalMs = 60 * 1000;
    } else {
        intervalMs = 5 * 60 * 1000;
    }

    // first point
    let firstIntervalEpochMs = Math.floor(startEpochMs / intervalMs) * intervalMs;
    // fill all with empty points
    // +1 because ...
    let intervalsCount = Math.floor(endEpochMs / intervalMs) - Math.floor(startEpochMs / intervalMs) + 1;
    for (var i = 0; i < intervalsCount; i++) {
        points.push([firstIntervalEpochMs + i * intervalMs, NaN, NaN]);
    }

    let prevEvent;
    // todo: use firstIntervalEpochMs instead of startEpochMs to make sure full interval data is considered?
    scanLoadedData(sn, startEpochMs, endEpochMs, function (epochMs, rawText, event) {
        let intervalIdx = Math.floor((epochMs - firstIntervalEpochMs) / intervalMs);
        points[intervalIdx][1] = 1;
        if (event !== undefined) {
            if (prevEvent !== undefined && (
                prevEvent.programId !== event.programId ||
                prevEvent.programVersion !== event.programVersion ||
                prevEvent.deviceConfig !== event.deviceConfig)) {

                points[intervalIdx][2] = 1;
            }
            prevEvent = event;
        }
    })
    // adding "edge point" at end
    points.push([endEpochMs + 1, NaN, NaN]);
    return points;
}

function getReadings(sn, startEpochMs, endEpochMs) {
    const readings = new Set();
    scanLoadedData(sn, startEpochMs, endEpochMs, function (epochMs, rawText, event) {
        if (event === undefined) {
            return;
        }
        // Object.keys(event[IDX_READINGS]).forEach(key => {
        event.readingIds.forEach(key => {
            readings.add(key);
        });
    });
    return  Array.from(readings).sort();
}

function getReadingsValues(sn, readings, startEpochMs, endEpochMs) {
    if (readings.length === 1) {
        // much more efficient execution for single series
        return getReadingValues(sn, readings[0], startEpochMs, endEpochMs);
    }
    let result = [];
    scanLoadedData(sn, startEpochMs, endEpochMs, function (epochMs, rawText, event) {
        if (event === undefined) {
            return;
        }
        let point = [epochMs];
        for (reading of readings) {
            // todo: with map looks more efficient.. or do the other way: loop through event.readingIds and add those needed?
            let value;
            for (var i = 0; i < event.readingIds.length; i++) {
                if (event.readingIds[i] === reading) {
                    value = event.readingValues[i];
                    break;
                }
            }
            point.push(value === undefined ? null : value);
            // point.push(value);
        }

        result.push(point);
    });

    return result;
}

function getReadingValues(sn, reading, startEpochMs, endEpochMs) {
    let result = [];
    scanLoadedData(sn, startEpochMs, endEpochMs, function (epochMs, rawText, event) {
        if (event === undefined) {
            return;
        }
        let value;
        for (var i = 0; i < event.readingIds.length; i++) {
            if (event.readingIds[i] === reading) {
                value = event.readingValues[i];
                result.push([epochMs, value]);
                break;
            }
        }
    });

    return result;
}