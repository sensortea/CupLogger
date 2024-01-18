// todo: think through error hanlding and propagating to user
function setSerialConnectionName(serialNumber, name) {
    fetch(httpEndpoint + '/setSerialConnectionName', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({serialNumber: serialNumber, name: name})
    })
        .then(response => response.text())
        .then(result => console.log(result))
        .catch(error => console.error('Error:', error));
}

function setSerialConnectionBaudRate(serialNumber, baudRate) {
    fetch(httpEndpoint + '/setSerialConnectionBaudRate', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({serialNumber: serialNumber, baudRate: baudRate})
    })
        .then(response => response.text())
        .then(result => console.log(result))
        .catch(error => console.error('Error:', error));
}

function setSerialConnectionDataCapture(serialNumber, dataCaptureOn) {
    fetch(httpEndpoint + '/setSerialConnectionDataCapture', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({serialNumber: serialNumber, dataCaptureOn: dataCaptureOn})
    })
        .then(response => response.text())
        .then(result => console.log(result))
        .catch(error => console.error('Error:', error));
}