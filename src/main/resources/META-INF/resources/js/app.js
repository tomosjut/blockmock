// Tab switching
document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        const tabName = tab.dataset.tab;

        // Update tab buttons
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');

        // Update tab content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(tabName).classList.add('active');

        // Load data for the tab
        if (tabName === 'dashboard') loadDashboard();
        else if (tabName === 'templates') loadTemplates();
        else if (tabName === 'scenarios') loadScenarios();
        else if (tabName === 'metrics') loadMetrics();
        else if (tabName === 'blocks') loadBlocks();
        else if (tabName === 'endpoints') loadEndpoints();
        else if (tabName === 'logs') loadLogs();
    });
});

// Load dashboard data
async function loadDashboard() {
    try {
        const [endpoints, stats] = await Promise.all([
            fetch('/api/endpoints').then(r => r.json()),
            fetch('/api/logs/stats').then(r => r.json())
        ]);

        document.getElementById('stat-endpoints').textContent = endpoints.length;
        document.getElementById('stat-total-requests').textContent = stats.matched + stats.unmatched;
        document.getElementById('stat-matched').textContent = stats.matched;
        document.getElementById('stat-unmatched').textContent = stats.unmatched;
    } catch (error) {
        console.error('Error loading dashboard:', error);
    }
}

// Load endpoints
async function loadEndpoints() {
    try {
        const endpoints = await fetch('/api/endpoints').then(r => r.json());
        const container = document.getElementById('endpoints-list');

        if (endpoints.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>Nog geen mock endpoints</h3>
                    <p>Klik op "Nieuwe Mock" om te beginnen</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="table">
                <table>
                    <thead>
                        <tr>
                            <th>Naam</th>
                            <th>Protocol</th>
                            <th>Pattern</th>
                            <th>Details</th>
                            <th>Status</th>
                            <th>Acties</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${endpoints.map(endpoint => {
                            let details = '-';
                            if (endpoint.httpConfig) {
                                details = `${endpoint.httpConfig.method} <code>${endpoint.httpConfig.path}</code>`;
                            } else if (endpoint.sftpConfig) {
                                details = `${endpoint.sftpConfig.operation} <code>${endpoint.sftpConfig.pathPattern}</code>`;
                            } else if (endpoint.amqpConfig) {
                                const brokerLabel = endpoint.amqpConfig.brokerType === 'ARTEMIS' ? 'Artemis' :
                                                   endpoint.amqpConfig.brokerType === 'IBM_MQ' ? 'IBM MQ' : 'RabbitMQ';
                                details = `${brokerLabel} - ${endpoint.amqpConfig.operation} <code>${endpoint.amqpConfig.exchangeName}</code>`;
                            } else if (endpoint.sqlConfig) {
                                details = `${endpoint.sqlConfig.databaseType} <code>${endpoint.sqlConfig.databaseName}</code>`;
                            }

                            return `
                            <tr>
                                <td><strong>${endpoint.name}</strong></td>
                                <td><span class="badge info">${endpoint.protocol}</span></td>
                                <td>${endpoint.pattern}</td>
                                <td>${details}</td>
                                <td>
                                    ${endpoint.enabled
                                        ? '<span class="badge success">Actief</span>'
                                        : '<span class="badge error">Inactief</span>'}
                                </td>
                                <td>
                                    <button class="btn btn-small btn-primary" onclick="showEditEndpoint(${endpoint.id})" title="Bewerken">
                                        ✏️
                                    </button>
                                    <button class="btn btn-small btn-secondary" onclick="toggleEndpoint(${endpoint.id})" title="${endpoint.enabled ? 'Deactiveren' : 'Activeren'}">
                                        ${endpoint.enabled ? '⏸️' : '▶️'}
                                    </button>
                                    <button class="btn btn-small btn-danger" onclick="deleteEndpoint(${endpoint.id})" title="Verwijderen">
                                        🗑️
                                    </button>
                                </td>
                            </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (error) {
        console.error('Error loading endpoints:', error);
        document.getElementById('endpoints-list').innerHTML = `
            <div class="empty-state">
                <h3>Error bij laden endpoints</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

// Load logs
async function loadLogs() {
    try {
        const logs = await fetch('/api/logs/recent?limit=100').then(r => r.json());
        const container = document.getElementById('logs-list');

        if (logs.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>Nog geen request logs</h3>
                    <p>Logs verschijnen hier zodra er requests binnenkomen</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="table">
                <table>
                    <thead>
                        <tr>
                            <th>Tijd</th>
                            <th>Protocol</th>
                            <th>Method</th>
                            <th>Path</th>
                            <th>Status</th>
                            <th>Status Code</th>
                            <th>Client IP</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${logs.map(log => `
                            <tr onclick="showLogDetail(${log.id})">
                                <td>${new Date(log.receivedAt).toLocaleString('nl-NL')}</td>
                                <td><span class="badge info">${log.protocol}</span></td>
                                <td>${log.requestMethod || '-'}</td>
                                <td><code>${log.requestPath || '-'}</code></td>
                                <td>
                                    ${log.matched
                                        ? '<span class="badge success">Matched</span>'
                                        : '<span class="badge error">Unmatched</span>'}
                                </td>
                                <td>${log.responseStatusCode || '-'}</td>
                                <td>${log.clientIp || '-'}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (error) {
        console.error('Error loading logs:', error);
        document.getElementById('logs-list').innerHTML = `
            <div class="empty-state">
                <h3>Error bij laden logs</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

// Toggle endpoint
async function toggleEndpoint(id) {
    try {
        await fetch(`/api/endpoints/${id}/toggle`, { method: 'POST' });
        loadEndpoints();
    } catch (error) {
        alert('Error bij toggle endpoint: ' + error.message);
    }
}

// Delete endpoint
async function deleteEndpoint(id) {
    if (!confirm('Weet je zeker dat je deze mock wilt verwijderen?')) return;

    try {
        await fetch(`/api/endpoints/${id}`, { method: 'DELETE' });
        loadEndpoints();
        loadDashboard();
    } catch (error) {
        alert('Error bij verwijderen endpoint: ' + error.message);
    }
}

// Refresh logs
function refreshLogs() {
    loadLogs();
}

// Clear logs
async function clearLogs() {
    if (!confirm('Weet je zeker dat je alle logs wilt wissen?')) return;

    try {
        await fetch('/api/logs', { method: 'DELETE' });
        loadLogs();
        loadDashboard();
    } catch (error) {
        alert('Error bij wissen logs: ' + error.message);
    }
}

let currentEndpointId = null;
let responses = [];
let responseIdCounter = 0;

// Show create endpoint
function showCreateEndpoint() {
    currentEndpointId = null;
    responses = [];
    responseIdCounter = 0;

    document.getElementById('modal-title').textContent = 'Nieuwe Mock Endpoint';
    document.getElementById('endpoint-form').reset();
    document.getElementById('endpoint-enabled').checked = true;

    // Add initial response
    addResponse();

    // Initialize protocol fields (this will set correct required attributes)
    toggleProtocolFields();

    document.getElementById('endpoint-modal').classList.add('active');
}

// Add response card
function addResponse() {
    const responseId = responseIdCounter++;
    const response = {
        id: responseId,
        name: `Response ${responses.length + 1}`,
        priority: responses.length,
        matchHeaders: {},
        matchQueryParams: {},
        matchBody: '',
        responseStatusCode: 200,
        responseBody: '',
        responseHeaders: {},
        responseDelayMs: 0
    };

    responses.push(response);
    renderResponses();
}

// Remove response
function removeResponse(responseId) {
    responses = responses.filter(r => r.id !== responseId);
    renderResponses();
}

// Move response up
function moveResponseUp(responseId) {
    const index = responses.findIndex(r => r.id === responseId);
    if (index > 0) {
        [responses[index], responses[index - 1]] = [responses[index - 1], responses[index]];
        renderResponses();
    }
}

// Move response down
function moveResponseDown(responseId) {
    const index = responses.findIndex(r => r.id === responseId);
    if (index < responses.length - 1) {
        [responses[index], responses[index + 1]] = [responses[index + 1], responses[index]];
        renderResponses();
    }
}

// Render responses
function renderResponses() {
    const container = document.getElementById('responses-container');
    const hasCatchAll = responses.some(r => !r.matchBody && (!r.matchHeaders || Object.keys(r.matchHeaders).length === 0) && (!r.matchQueryParams || Object.keys(r.matchQueryParams).length === 0));

    container.innerHTML = responses.map((response, index) => `
        <div class="response-card" data-response-id="${response.id}">
            <div class="response-card-header">
                <h4>${response.name}</h4>
                <div class="response-card-actions">
                    <button type="button" class="btn btn-small btn-secondary" onclick="moveResponseUp(${response.id})" ${index === 0 ? 'disabled' : ''} title="Omhoog">⬆️</button>
                    <button type="button" class="btn btn-small btn-secondary" onclick="moveResponseDown(${response.id})" ${index === responses.length - 1 ? 'disabled' : ''} title="Omlaag">⬇️</button>
                    <button type="button" class="btn btn-small btn-danger" onclick="removeResponse(${response.id})" ${responses.length === 1 ? 'disabled' : ''} title="Verwijderen">🗑️</button>
                </div>
            </div>
            <div class="response-card-body">
                <div class="response-section">
                    <h5>Matching Criteria</h5>
                    <div class="form-group">
                        <label>Priority</label>
                        <input type="number" class="response-priority" data-response-id="${response.id}" value="${response.priority}" min="0" max="100">
                        <small>Hogere priority wordt eerst gecontroleerd (0 = laagste)</small>
                    </div>
                    <div class="form-group">
                        <label>Match Headers (JSON)</label>
                        <textarea class="response-match-headers" data-response-id="${response.id}" rows="2" placeholder='{"Authorization": "Bearer token"}'>${JSON.stringify(response.matchHeaders || {}, null, 2)}</textarea>
                        <small>Alleen matchen als deze headers aanwezig zijn</small>
                    </div>
                    <div class="form-group">
                        <label>Match Query Params (JSON)</label>
                        <textarea class="response-match-query" data-response-id="${response.id}" rows="2" placeholder='{"userId": "123"}'>${JSON.stringify(response.matchQueryParams || {}, null, 2)}</textarea>
                        <small>Alleen matchen als deze query parameters aanwezig zijn</small>
                    </div>
                    <div class="form-group">
                        <label>Match Body (Text/Regex)</label>
                        <textarea class="response-match-body" data-response-id="${response.id}" rows="2" placeholder='Exacte text of regex pattern'>${response.matchBody || ''}</textarea>
                        <small>Leeg = altijd match, anders exacte text of /regex/ pattern</small>
                    </div>
                </div>
                <div class="response-section">
                    <h5>Response Configuration</h5>
                    <div class="form-row">
                        <div class="form-group">
                            <label>Status Code *</label>
                            <input type="number" class="response-status" data-response-id="${response.id}" value="${response.responseStatusCode}" required min="100" max="599">
                        </div>
                        <div class="form-group">
                            <label>Delay (ms)</label>
                            <input type="number" class="response-delay" data-response-id="${response.id}" value="${response.responseDelayMs}" min="0" max="30000">
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Response Body</label>
                        <textarea class="response-body" data-response-id="${response.id}" rows="4" placeholder='{"message": "Success"}'>${response.responseBody || ''}</textarea>
                    </div>
                    <div class="form-group">
                        <label>Response Headers (JSON)</label>
                        <textarea class="response-headers" data-response-id="${response.id}" rows="2" placeholder='{"Content-Type": "application/json"}'>${JSON.stringify(response.responseHeaders || {}, null, 2)}</textarea>
                    </div>
                </div>
            </div>
        </div>
    `).join('');

    // Add warning if no catch-all response exists
    if (!hasCatchAll && responses.length > 0) {
        container.innerHTML += `
            <div class="alert alert-warning" style="margin-top: 1rem;">
                💡 <strong>Tip:</strong> Dit endpoint heeft geen catch-all response.
                Requests die niet matchen met de criteria krijgen een 404 error.
                Voeg een response toe met lege match criteria (priority 0) als fallback.
            </div>
        `;
    }

    // Attach event listeners to update response data
    attachResponseListeners();
}

// Attach event listeners to response fields
function attachResponseListeners() {
    // Status code
    document.querySelectorAll('.response-status').forEach(input => {
        input.addEventListener('change', (e) => {
            const responseId = parseInt(e.target.dataset.responseId);
            const response = responses.find(r => r.id === responseId);
            if (response) response.responseStatusCode = parseInt(e.target.value);
        });
    });

    // Delay
    document.querySelectorAll('.response-delay').forEach(input => {
        input.addEventListener('change', (e) => {
            const responseId = parseInt(e.target.dataset.responseId);
            const response = responses.find(r => r.id === responseId);
            if (response) response.responseDelayMs = parseInt(e.target.value);
        });
    });

    // Priority
    document.querySelectorAll('.response-priority').forEach(input => {
        input.addEventListener('change', (e) => {
            const responseId = parseInt(e.target.dataset.responseId);
            const response = responses.find(r => r.id === responseId);
            if (response) response.priority = parseInt(e.target.value);
        });
    });

    // Body
    document.querySelectorAll('.response-body').forEach(textarea => {
        textarea.addEventListener('change', (e) => {
            const responseId = parseInt(e.target.dataset.responseId);
            const response = responses.find(r => r.id === responseId);
            if (response) response.responseBody = e.target.value;
        });
    });

    // Headers
    document.querySelectorAll('.response-headers').forEach(textarea => {
        textarea.addEventListener('change', (e) => {
            const responseId = parseInt(e.target.dataset.responseId);
            const response = responses.find(r => r.id === responseId);
            if (response) {
                try {
                    response.responseHeaders = e.target.value.trim() ? JSON.parse(e.target.value) : {};
                } catch (err) {
                    // Keep old value on parse error
                }
            }
        });
    });

    // Match headers
    document.querySelectorAll('.response-match-headers').forEach(textarea => {
        textarea.addEventListener('change', (e) => {
            const responseId = parseInt(e.target.dataset.responseId);
            const response = responses.find(r => r.id === responseId);
            if (response) {
                try {
                    response.matchHeaders = e.target.value.trim() ? JSON.parse(e.target.value) : {};
                } catch (err) {
                    // Keep old value on parse error
                }
            }
        });
    });

    // Match query params
    document.querySelectorAll('.response-match-query').forEach(textarea => {
        textarea.addEventListener('change', (e) => {
            const responseId = parseInt(e.target.dataset.responseId);
            const response = responses.find(r => r.id === responseId);
            if (response) {
                try {
                    response.matchQueryParams = e.target.value.trim() ? JSON.parse(e.target.value) : {};
                } catch (err) {
                    // Keep old value on parse error
                }
            }
        });
    });

    // Match body
    document.querySelectorAll('.response-match-body').forEach(textarea => {
        textarea.addEventListener('change', (e) => {
            const responseId = parseInt(e.target.dataset.responseId);
            const response = responses.find(r => r.id === responseId);
            if (response) response.matchBody = e.target.value;
        });
    });
}

// Show edit endpoint
async function showEditEndpoint(id) {
    currentEndpointId = id;
    responses = [];
    responseIdCounter = 0;

    document.getElementById('modal-title').textContent = 'Mock Endpoint Bewerken';

    try {
        const endpoint = await fetch(`/api/endpoints/${id}`).then(r => r.json());

        document.getElementById('endpoint-name').value = endpoint.name || '';
        document.getElementById('endpoint-description').value = endpoint.description || '';
        document.getElementById('endpoint-protocol').value = endpoint.protocol;
        document.getElementById('endpoint-pattern').value = endpoint.pattern;
        document.getElementById('endpoint-enabled').checked = endpoint.enabled;

        if (endpoint.httpConfig) {
            document.getElementById('http-method').value = endpoint.httpConfig.method;
            document.getElementById('http-path').value = endpoint.httpConfig.path;
            document.getElementById('http-path-regex').checked = endpoint.httpConfig.pathRegex;
        }

        // Load responses
        if (endpoint.responses && endpoint.responses.length > 0) {
            endpoint.responses.forEach((resp, index) => {
                const responseId = responseIdCounter++;
                responses.push({
                    id: responseId,
                    name: resp.name || `Response ${index + 1}`,
                    priority: resp.priority || index,
                    matchHeaders: resp.matchHeaders || {},
                    matchQueryParams: resp.matchQueryParams || {},
                    matchBody: resp.matchBody || '',
                    responseStatusCode: resp.responseStatusCode || 200,
                    responseBody: resp.responseBody || '',
                    responseHeaders: resp.responseHeaders || {},
                    responseDelayMs: resp.responseDelayMs || 0
                });
            });
        } else {
            // Add default response if none exist
            addResponse(true);
        }

        renderResponses();
        toggleProtocolFields();
        document.getElementById('endpoint-modal').classList.add('active');
    } catch (error) {
        alert('Error bij laden endpoint: ' + error.message);
    }
}

// Close endpoint modal
function closeEndpointModal() {
    document.getElementById('endpoint-modal').classList.remove('active');
    currentEndpointId = null;
}

// Toggle protocol-specific fields
function toggleProtocolFields() {
    const protocol = document.getElementById('endpoint-protocol').value;
    const httpFields = document.getElementById('http-fields');
    const sftpFields = document.getElementById('sftp-fields');
    const amqpFields = document.getElementById('amqp-fields');
    const sqlFields = document.getElementById('sql-fields');
    const mqttFields = document.getElementById('mqtt-fields');
    const nosqlFields = document.getElementById('nosql-fields');
    const kafkaFields = document.getElementById('kafka-fields');
    const grpcFields = document.getElementById('grpc-fields');
    const websocketFields = document.getElementById('websocket-fields');

    // Helper function to disable required fields in a container
    function disableRequiredFields(container) {
        container.querySelectorAll('[required]').forEach(field => {
            field.removeAttribute('required');
            field.dataset.wasRequired = 'true';
        });
    }

    // Helper function to enable required fields in a container
    function enableRequiredFields(container) {
        container.querySelectorAll('[data-was-required="true"]').forEach(field => {
            field.setAttribute('required', 'required');
        });
    }

    // Hide all and disable required fields
    [httpFields, sftpFields, amqpFields, sqlFields, mqttFields, nosqlFields, kafkaFields, grpcFields, websocketFields].forEach(fieldSet => {
        fieldSet.style.display = 'none';
        disableRequiredFields(fieldSet);
    });

    // Show relevant and enable required fields
    if (protocol === 'HTTP' || protocol === 'HTTPS') {
        httpFields.style.display = 'block';
        enableRequiredFields(httpFields);
    } else if (protocol === 'SFTP') {
        sftpFields.style.display = 'block';
        enableRequiredFields(sftpFields);
    } else if (protocol === 'AMQP') {
        amqpFields.style.display = 'block';
        enableRequiredFields(amqpFields);
    } else if (protocol === 'SQL') {
        sqlFields.style.display = 'block';
        enableRequiredFields(sqlFields);
    } else if (protocol === 'MQTT') {
        mqttFields.style.display = 'block';
        enableRequiredFields(mqttFields);
    } else if (protocol === 'NOSQL') {
        nosqlFields.style.display = 'block';
        enableRequiredFields(nosqlFields);
    } else if (protocol === 'KAFKA') {
        kafkaFields.style.display = 'block';
        enableRequiredFields(kafkaFields);
    } else if (protocol === 'GRPC') {
        grpcFields.style.display = 'block';
        enableRequiredFields(grpcFields);
    } else if (protocol === 'WEBSOCKET') {
        websocketFields.style.display = 'block';
        enableRequiredFields(websocketFields);
    }
}

// Toggle SFTP authentication fields
function toggleSftpAuth() {
    const allowAnonymous = document.getElementById('sftp-allow-anonymous').checked;
    const authFields = document.getElementById('sftp-auth-fields');
    authFields.style.display = allowAnonymous ? 'none' : 'block';
}

// Save endpoint
async function saveEndpoint(event) {
    event.preventDefault();

    try {
        const protocol = document.getElementById('endpoint-protocol').value;

        // Build endpoint object
        const endpoint = {
            name: document.getElementById('endpoint-name').value,
            description: document.getElementById('endpoint-description').value || null,
            protocol: protocol,
            pattern: document.getElementById('endpoint-pattern').value,
            enabled: document.getElementById('endpoint-enabled').checked
        };

        // Add protocol-specific configuration
        if (protocol === 'HTTP' || protocol === 'HTTPS') {
            endpoint.httpConfig = {
                method: document.getElementById('http-method').value,
                path: document.getElementById('http-path').value,
                pathRegex: document.getElementById('http-path-regex').checked
            };
            endpoint.responses = responses.map((r, index) => ({
                name: r.name,
                priority: index,
                matchHeaders: Object.keys(r.matchHeaders || {}).length > 0 ? r.matchHeaders : null,
                matchQueryParams: Object.keys(r.matchQueryParams || {}).length > 0 ? r.matchQueryParams : null,
                matchBody: r.matchBody || null,
                responseStatusCode: r.responseStatusCode,
                responseBody: r.responseBody || null,
                responseHeaders: Object.keys(r.responseHeaders || {}).length > 0 ? r.responseHeaders : null,
                responseDelayMs: r.responseDelayMs
            }));
        } else if (protocol === 'SFTP') {
            endpoint.sftpConfig = {
                port: parseInt(document.getElementById('sftp-port').value),
                operation: document.getElementById('sftp-operation').value,
                pathPattern: document.getElementById('sftp-path').value,
                pathIsRegex: document.getElementById('sftp-path-regex').checked,
                username: document.getElementById('sftp-username').value || null,
                password: document.getElementById('sftp-password').value || null,
                allowAnonymous: document.getElementById('sftp-allow-anonymous').checked,
                mockResponseContent: document.getElementById('sftp-response-content').value || null,
                success: document.getElementById('sftp-success').checked
            };
            endpoint.responses = [];
        } else if (protocol === 'AMQP') {
            endpoint.amqpConfig = {
                brokerType: document.getElementById('amqp-broker-type').value,
                host: document.getElementById('amqp-host').value,
                port: parseInt(document.getElementById('amqp-port').value),
                virtualHost: document.getElementById('amqp-vhost').value,
                operation: document.getElementById('amqp-operation').value,
                exchangeName: document.getElementById('amqp-exchange-name').value,
                exchangeType: document.getElementById('amqp-exchange-type').value,
                exchangeDurable: document.getElementById('amqp-exchange-durable').checked,
                exchangeAutoDelete: document.getElementById('amqp-exchange-autodelete').checked,
                queueName: document.getElementById('amqp-queue-name').value || null,
                queueDurable: document.getElementById('amqp-queue-durable').checked,
                queueExclusive: document.getElementById('amqp-queue-exclusive').checked,
                queueAutoDelete: false,
                routingKey: document.getElementById('amqp-routing-key').value || null,
                bindingPattern: document.getElementById('amqp-binding-pattern').value || null,
                username: document.getElementById('amqp-username').value || null,
                password: document.getElementById('amqp-password').value || null,
                mockMessageContent: document.getElementById('amqp-message-content').value || null,
                autoReply: document.getElementById('amqp-auto-reply').checked,
                replyDelayMs: parseInt(document.getElementById('amqp-reply-delay').value)
            };
            endpoint.responses = [];
        } else if (protocol === 'SQL') {
            endpoint.sqlConfig = {
                databaseType: document.getElementById('sql-database-type').value,
                databaseName: document.getElementById('sql-database-name').value,
                username: document.getElementById('sql-username').value || null,
                password: document.getElementById('sql-password').value || null,
                initScript: document.getElementById('sql-init-script').value || null,
                queryMocks: []
            };
            endpoint.responses = [];
        } else if (protocol === 'NOSQL') {
            endpoint.noSqlConfig = {
                databaseType: document.getElementById('nosql-database-type').value,
                host: document.getElementById('nosql-host').value,
                port: parseInt(document.getElementById('nosql-port').value),
                databaseName: document.getElementById('nosql-database-name').value || null,
                username: document.getElementById('nosql-username').value || null,
                password: document.getElementById('nosql-password').value || null
            };
            endpoint.responses = [];
        } else if (protocol === 'KAFKA') {
            endpoint.kafkaConfig = {
                topicName: document.getElementById('kafka-topic').value,
                operation: document.getElementById('kafka-operation').value,
                bootstrapServers: document.getElementById('kafka-bootstrap-servers').value,
                groupId: document.getElementById('kafka-group-id').value || null
            };
            endpoint.responses = [];
        } else if (protocol === 'GRPC') {
            endpoint.grpcConfig = {
                serviceName: document.getElementById('grpc-service-name').value,
                methodName: document.getElementById('grpc-method-name').value,
                port: parseInt(document.getElementById('grpc-port').value),
                isClientStreaming: document.getElementById('grpc-client-streaming').checked,
                isServerStreaming: document.getElementById('grpc-server-streaming').checked
            };
            endpoint.responses = [];
        } else if (protocol === 'WEBSOCKET') {
            endpoint.webSocketConfig = {
                path: document.getElementById('websocket-path').value,
                operation: document.getElementById('websocket-operation').value,
                maxConnections: parseInt(document.getElementById('websocket-max-connections').value),
                messageFormat: document.getElementById('websocket-message-format').value
            };
            endpoint.responses = [];
        }

        // Save via API
        let response;
        if (currentEndpointId) {
            // Update existing
            response = await fetch(`/api/endpoints/${currentEndpointId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(endpoint)
            });
        } else {
            // Create new
            response = await fetch('/api/endpoints', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(endpoint)
            });
        }

        if (!response.ok) {
            throw new Error('Failed to save endpoint: ' + response.statusText);
        }

        closeEndpointModal();
        loadEndpoints();
        loadDashboard();

        // Show success message
        const action = currentEndpointId ? 'bijgewerkt' : 'aangemaakt';
        showNotification(`Mock endpoint ${action}!`, 'success');

    } catch (error) {
        alert('Error bij opslaan endpoint: ' + error.message);
    }
}

// Show notification
function showNotification(message, type = 'info') {
    // Simple notification for now
    console.log(`[${type}] ${message}`);
}

// Show log detail
async function showLogDetail(id) {
    try {
        const log = await fetch(`/api/logs/${id}`).then(r => r.json());

        document.getElementById('log-time').textContent = new Date(log.receivedAt).toLocaleString('nl-NL');
        document.getElementById('log-protocol').textContent = log.protocol;
        document.getElementById('log-method').textContent = log.requestMethod || '-';
        document.getElementById('log-path').textContent = log.requestPath || '-';
        document.getElementById('log-client-ip').textContent = log.clientIp || '-';
        document.getElementById('log-matched').innerHTML = log.matched
            ? '<span class="badge success">Matched</span>'
            : '<span class="badge error">Unmatched</span>';

        document.getElementById('log-request-headers').textContent =
            log.requestHeaders ? JSON.stringify(log.requestHeaders, null, 2) : '';
        document.getElementById('log-query-params').textContent =
            log.requestQueryParams ? JSON.stringify(log.requestQueryParams, null, 2) : '';
        document.getElementById('log-request-body').textContent = log.requestBody || '';

        document.getElementById('log-status-code').textContent = log.responseStatusCode || '-';
        document.getElementById('log-delay').textContent = log.responseDelayMs ? `${log.responseDelayMs}ms` : '0ms';
        document.getElementById('log-response-headers').textContent =
            log.responseHeaders ? JSON.stringify(log.responseHeaders, null, 2) : '';
        document.getElementById('log-response-body').textContent = log.responseBody || '';

        document.getElementById('log-detail-modal').classList.add('active');
    } catch (error) {
        alert('Error bij laden log details: ' + error.message);
    }
}

// Close log detail modal
function closeLogDetailModal() {
    document.getElementById('log-detail-modal').classList.remove('active');
}

// Auto-refresh for logs
let autoRefreshInterval = null;

function startAutoRefresh() {
    if (autoRefreshInterval) return; // Already running

    autoRefreshInterval = setInterval(() => {
        // Only refresh if logs tab is active
        const logsTab = document.getElementById('logs');
        if (logsTab.classList.contains('active')) {
            loadLogs();
            loadDashboard(); // Also update stats
        }
    }, 5000); // Refresh every 5 seconds
}

function stopAutoRefresh() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
        autoRefreshInterval = null;
    }
}

// Enhanced tab switching with auto-refresh
document.querySelectorAll('.tab').forEach(tab => {
    const originalClickListener = tab.onclick;
    tab.onclick = function() {
        if (originalClickListener) originalClickListener.call(this);

        const tabName = tab.dataset.tab;
        if (tabName === 'logs') {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }

        // Load blocks when blocks tab is opened
        if (tabName === 'blocks') {
            loadBlocks();
        }

        // Load templates when templates tab is opened
        if (tabName === 'templates') {
            loadTemplates();
        }
    };
});

// === BLOCKS FUNCTIONALITY ===

let currentBlockId = null;
let allEndpoints = [];
let selectedEndpointIds = [];

// Load all blocks
async function loadBlocks() {
    try {
        const blocks = await fetch('/api/blocks').then(r => r.json());
        const container = document.getElementById('blocks-list');

        if (blocks.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>Nog geen blocks</h3>
                    <p>Maak een block aan om mock endpoints te groeperen</p>
                </div>
            `;
            return;
        }

        container.innerHTML = blocks.map(block => `
            <div class="block-card" style="border-left-color: ${block.color}">
                <div class="block-card-header">
                    <div class="block-card-title">
                        <h3>${block.name}</h3>
                        ${block.description ? `<p>${block.description}</p>` : ''}
                    </div>
                    <div class="block-card-actions">
                        <button class="btn btn-small btn-primary" onclick="showEditBlock(${block.id})" title="Bewerken">✏️</button>
                        <button class="btn btn-small btn-success" onclick="startBlock(${block.id})" title="Start Block">▶️</button>
                        <button class="btn btn-small btn-secondary" onclick="stopBlock(${block.id})" title="Stop Block">⏸️</button>
                        <button class="btn btn-small btn-danger" onclick="deleteBlock(${block.id})" title="Verwijderen">🗑️</button>
                    </div>
                </div>
                <div class="block-card-stats">
                    <div class="block-stat">
                        <span class="block-stat-label">Endpoints:</span>
                        <span class="block-stat-value">${block.endpointCount || 0}</span>
                    </div>
                    <div class="block-stat">
                        <span class="block-stat-label">Actief:</span>
                        <span class="block-stat-value">${block.activeEndpointCount || 0}</span>
                    </div>
                </div>
                <div class="block-endpoints-list" id="block-${block.id}-endpoints">
                    <small style="color: #6b7280;">Laden...</small>
                </div>
            </div>
        `).join('');

        // Load endpoints for each block
        for (const block of blocks) {
            loadBlockEndpoints(block.id);
        }

    } catch (error) {
        console.error('Error loading blocks:', error);
        document.getElementById('blocks-list').innerHTML = `
            <div class="empty-state">
                <h3>Error bij laden blocks</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

// Load endpoints for a specific block
async function loadBlockEndpoints(blockId) {
    try {
        const endpoints = await fetch(`/api/blocks/${blockId}/endpoints`).then(r => r.json());
        const container = document.getElementById(`block-${blockId}-endpoints`);

        if (endpoints.length === 0) {
            container.innerHTML = '<small style="color: #9ca3af;">Geen endpoints in deze block</small>';
            return;
        }

        container.innerHTML = endpoints.map(endpoint => `
            <span class="block-endpoint-badge ${endpoint.enabled ? 'active' : ''}">
                ${endpoint.name}
            </span>
        `).join('');
    } catch (error) {
        console.error(`Error loading endpoints for block ${blockId}:`, error);
    }
}

// Show create block modal
async function showCreateBlock() {
    currentBlockId = null;
    selectedEndpointIds = [];

    document.getElementById('block-modal-title').textContent = 'Nieuwe Block';
    document.getElementById('block-form').reset();
    document.getElementById('block-color').value = '#667eea';

    // Load all endpoints for selection
    await loadEndpointsForSelection();

    document.getElementById('block-modal').classList.add('active');
}

// Show edit block modal
async function showEditBlock(id) {
    currentBlockId = id;

    document.getElementById('block-modal-title').textContent = 'Block Bewerken';

    try {
        const [block, blockEndpoints] = await Promise.all([
            fetch(`/api/blocks/${id}`).then(r => r.json()),
            fetch(`/api/blocks/${id}/endpoints`).then(r => r.json())
        ]);

        document.getElementById('block-name').value = block.name || '';
        document.getElementById('block-description').value = block.description || '';
        document.getElementById('block-color').value = block.color || '#667eea';

        // Set selected endpoint IDs
        selectedEndpointIds = blockEndpoints.map(e => e.id);

        // Load all endpoints for selection
        await loadEndpointsForSelection();

        document.getElementById('block-modal').classList.add('active');
    } catch (error) {
        alert('Error bij laden block: ' + error.message);
    }
}

// Load all endpoints for selection
async function loadEndpointsForSelection() {
    try {
        allEndpoints = await fetch('/api/endpoints').then(r => r.json());
        const container = document.getElementById('block-endpoints-selection');

        if (allEndpoints.length === 0) {
            container.innerHTML = '<p style="color: #9ca3af; text-align: center; padding: 20px;">Geen endpoints beschikbaar</p>';
            return;
        }

        container.innerHTML = allEndpoints.map(endpoint => `
            <div class="endpoint-checkbox-item">
                <input type="checkbox"
                       id="endpoint-${endpoint.id}"
                       value="${endpoint.id}"
                       ${selectedEndpointIds.includes(endpoint.id) ? 'checked' : ''}
                       onchange="toggleEndpointSelection(${endpoint.id})">
                <label class="endpoint-checkbox-label" for="endpoint-${endpoint.id}">
                    <strong>${endpoint.name}</strong>
                    <div class="endpoint-checkbox-meta">
                        ${endpoint.httpConfig ? `${endpoint.httpConfig.method} ${endpoint.httpConfig.path}` : endpoint.protocol}
                    </div>
                </label>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading endpoints for selection:', error);
    }
}

// Toggle endpoint selection
function toggleEndpointSelection(endpointId) {
    if (selectedEndpointIds.includes(endpointId)) {
        selectedEndpointIds = selectedEndpointIds.filter(id => id !== endpointId);
    } else {
        selectedEndpointIds.push(endpointId);
    }
}

// Close block modal
function closeBlockModal() {
    document.getElementById('block-modal').classList.remove('active');
    currentBlockId = null;
    selectedEndpointIds = [];
}

// Save block
async function saveBlock(event) {
    event.preventDefault();

    try {
        const block = {
            name: document.getElementById('block-name').value,
            description: document.getElementById('block-description').value || null,
            color: document.getElementById('block-color').value
        };

        let savedBlock;

        if (currentBlockId) {
            // Update existing
            const response = await fetch(`/api/blocks/${currentBlockId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(block)
            });

            if (!response.ok) throw new Error('Failed to update block');
            savedBlock = await response.json();
        } else {
            // Create new
            const response = await fetch('/api/blocks', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(block)
            });

            if (!response.ok) throw new Error('Failed to create block');
            savedBlock = await response.json();
        }

        // Now sync endpoints
        // First get current endpoints
        const currentEndpoints = currentBlockId
            ? await fetch(`/api/blocks/${savedBlock.id}/endpoints`).then(r => r.json())
            : [];
        const currentEndpointIds = currentEndpoints.map(e => e.id);

        // Add new endpoints
        for (const endpointId of selectedEndpointIds) {
            if (!currentEndpointIds.includes(endpointId)) {
                await fetch(`/api/blocks/${savedBlock.id}/endpoints/${endpointId}`, {
                    method: 'POST'
                });
            }
        }

        // Remove endpoints that were deselected
        for (const endpointId of currentEndpointIds) {
            if (!selectedEndpointIds.includes(endpointId)) {
                await fetch(`/api/blocks/${savedBlock.id}/endpoints/${endpointId}`, {
                    method: 'DELETE'
                });
            }
        }

        closeBlockModal();
        loadBlocks();

    } catch (error) {
        alert('Error bij opslaan block: ' + error.message);
    }
}

// Delete block
async function deleteBlock(id) {
    if (!confirm('Weet je zeker dat je deze block wilt verwijderen?')) return;

    try {
        await fetch(`/api/blocks/${id}`, { method: 'DELETE' });
        loadBlocks();
    } catch (error) {
        alert('Error bij verwijderen block: ' + error.message);
    }
}

// Start block (enable all endpoints)
async function startBlock(id) {
    try {
        const response = await fetch(`/api/blocks/${id}/start`, { method: 'POST' });
        if (!response.ok) throw new Error('Failed to start block');

        loadBlocks();
        loadDashboard(); // Update stats

        // Show notification
        console.log('[success] Block gestart - alle endpoints zijn nu actief');
    } catch (error) {
        alert('Error bij starten block: ' + error.message);
    }
}

// Stop block (disable all endpoints)
async function stopBlock(id) {
    try {
        const response = await fetch(`/api/blocks/${id}/stop`, { method: 'POST' });
        if (!response.ok) throw new Error('Failed to stop block');

        loadBlocks();
        loadDashboard(); // Update stats

        // Show notification
        console.log('[info] Block gestopt - alle endpoints zijn nu inactief');
    } catch (error) {
        alert('Error bij stoppen block: ' + error.message);
    }
}

// === TEMPLATES FUNCTIONALITY ===

let templates = [];

// Load templates
async function loadTemplates() {
    try {
        templates = await fetch('/api/templates').then(r => r.json());
        const container = document.getElementById('templates-list');

        if (templates.length === 0) {
            container.innerHTML = '<p>Geen templates beschikbaar</p>';
            return;
        }

        const icons = {
            'rest-api': '🌐',
            'graphql': '📊',
            'oauth2': '🔐',
            'webhook': '🪝',
            'sftp-server': '📁',
            'message-queue': '📬',
            'sql-database': '🗄️'
        };

        container.innerHTML = templates.map(template => `
            <div class="template-card" onclick="useTemplate('${template.id}')">
                <div class="template-card-header">
                    <div class="template-card-icon">${icons[template.id] || '📦'}</div>
                    <div class="template-card-title">${template.name}</div>
                </div>
                <div class="template-card-protocol">${template.protocol}</div>
                <div class="template-card-description">${template.description}</div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading templates:', error);
        document.getElementById('templates-list').innerHTML = '<p>Error loading templates</p>';
    }
}

// Use template to create endpoint
async function useTemplate(templateId) {
    try {
        const response = await fetch(`/api/templates/${templateId}`);
        const template = await response.json();

        // Import the template as a new endpoint
        const importResponse = await fetch('/api/import-export/import-single', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(template)
        });

        if (!importResponse.ok) {
            throw new Error('Failed to create endpoint from template');
        }

        const created = await importResponse.json();
        alert(`Endpoint "${created.name}" aangemaakt!`);

        // Switch to endpoints tab and reload
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        document.querySelector('[data-tab="endpoints"]').classList.add('active');
        document.getElementById('endpoints').classList.add('active');

        loadEndpoints();
        loadDashboard();
    } catch (error) {
        alert('Error bij gebruiken template: ' + error.message);
    }
}

// === IMPORT/EXPORT FUNCTIONALITY ===

// Export all endpoints
async function exportAllEndpoints() {
    try {
        const response = await fetch('/api/import-export/export');
        const data = await response.json();

        // Create download link
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'blockmock-export-' + new Date().toISOString().split('T')[0] + '.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        console.log('[success] Endpoints geëxporteerd');
    } catch (error) {
        alert('Error bij exporteren endpoints: ' + error.message);
    }
}

// Export single endpoint
async function exportEndpoint(id) {
    try {
        const response = await fetch(`/api/import-export/export/${id}`);
        const data = await response.json();

        // Create download link
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'endpoint-' + id + '.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        console.log('[success] Endpoint geëxporteerd');
    } catch (error) {
        alert('Error bij exporteren endpoint: ' + error.message);
    }
}

// Import endpoints from file
async function importEndpoints(event) {
    const file = event.target.files[0];
    if (!file) return;

    try {
        const text = await file.text();
        const data = JSON.parse(text);

        // Check if it's array or single object
        const endpoints = Array.isArray(data) ? data : [data];

        const response = await fetch('/api/import-export/import', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(endpoints)
        });

        if (!response.ok) {
            throw new Error('Import failed: ' + response.statusText);
        }

        const result = await response.json();
        alert(`Successfully imported ${result.imported} endpoint(s)`);

        // Reset file input and reload endpoints
        event.target.value = '';
        loadEndpoints();
        loadDashboard();
    } catch (error) {
        alert('Error bij importeren endpoints: ' + error.message);
        event.target.value = '';
    }
}

// ===================================
// SCENARIOS FUNCTIONS
// ===================================

let currentScenarioId = null;
let scenarioStepCounter = 0;

// Load scenarios
async function loadScenarios() {
    try {
        const scenarios = await fetch('/api/scenarios').then(r => r.json());
        const container = document.getElementById('scenarios-list');

        if (scenarios.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>Nog geen scenarios</h3>
                    <p>Klik op "Nieuw Scenario" om te beginnen</p>
                </div>
            `;
            return;
        }

        container.innerHTML = scenarios.map(scenario => `
            <div class="scenario-card" style="border-left: 4px solid ${scenario.color}">
                <div class="scenario-header">
                    <div>
                        <h3>${scenario.name}</h3>
                        ${scenario.description ? `<p>${scenario.description}</p>` : ''}
                    </div>
                    <div class="scenario-actions">
                        <button class="btn btn-primary" onclick="executeScenario(${scenario.id})">▶️ Uitvoeren</button>
                        <button class="btn btn-small btn-secondary" onclick="showEditScenario(${scenario.id})" title="Bewerken">✏️</button>
                        <button class="btn btn-small btn-danger" onclick="deleteScenario(${scenario.id})" title="Verwijderen">🗑️</button>
                    </div>
                </div>
                <div class="scenario-steps">
                    <h4>Steps (${scenario.steps.length}):</h4>
                    <ol>
                        ${scenario.steps.map(step => {
                            let stepDesc = '';
                            if (step.action === 'ENABLE') {
                                stepDesc = `Enable endpoint: ${step.mockEndpoint?.name || 'Unknown'}`;
                            } else if (step.action === 'DISABLE') {
                                stepDesc = `Disable endpoint: ${step.mockEndpoint?.name || 'Unknown'}`;
                            } else if (step.action === 'DELAY') {
                                stepDesc = `Wait ${step.delayMs}ms`;
                            }
                            return `<li>${stepDesc}</li>`;
                        }).join('')}
                    </ol>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading scenarios:', error);
    }
}

// Show create scenario modal
function showCreateScenario() {
    currentScenarioId = null;
    scenarioStepCounter = 0;
    document.getElementById('scenario-modal-title').textContent = 'Nieuw Scenario';
    document.getElementById('scenario-form').reset();
    document.getElementById('scenario-color').value = '#667eea';
    document.getElementById('scenario-steps-container').innerHTML = '';
    document.getElementById('scenario-modal').style.display = 'block';
}

// Show edit scenario modal
async function showEditScenario(id) {
    try {
        const scenario = await fetch(`/api/scenarios/${id}`).then(r => r.json());
        currentScenarioId = id;
        scenarioStepCounter = 0;

        document.getElementById('scenario-modal-title').textContent = 'Scenario Bewerken';
        document.getElementById('scenario-name').value = scenario.name;
        document.getElementById('scenario-description').value = scenario.description || '';
        document.getElementById('scenario-color').value = scenario.color;

        // Load steps
        const stepsContainer = document.getElementById('scenario-steps-container');
        stepsContainer.innerHTML = '';

        for (const step of scenario.steps) {
            await addScenarioStepWithData(step);
        }

        document.getElementById('scenario-modal').style.display = 'block';
    } catch (error) {
        alert('Error loading scenario: ' + error.message);
    }
}

// Close scenario modal
function closeScenarioModal() {
    document.getElementById('scenario-modal').style.display = 'none';
}

// Add scenario step
async function addScenarioStep() {
    await addScenarioStepWithData(null);
}

async function addScenarioStepWithData(stepData) {
    const stepId = scenarioStepCounter++;
    const container = document.getElementById('scenario-steps-container');

    // Load all endpoints for selection
    const endpoints = await fetch('/api/endpoints').then(r => r.json());

    const stepDiv = document.createElement('div');
    stepDiv.className = 'scenario-step';
    stepDiv.id = `step-${stepId}`;
    stepDiv.innerHTML = `
        <div class="step-header">
            <h4>Step ${stepId + 1}</h4>
            <button type="button" class="btn btn-small btn-danger" onclick="removeScenarioStep(${stepId})">✖</button>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label for="step-action-${stepId}">Action *</label>
                <select id="step-action-${stepId}" required onchange="toggleStepFields(${stepId})">
                    <option value="ENABLE" ${stepData?.action === 'ENABLE' ? 'selected' : ''}>Enable Endpoint</option>
                    <option value="DISABLE" ${stepData?.action === 'DISABLE' ? 'selected' : ''}>Disable Endpoint</option>
                    <option value="DELAY" ${stepData?.action === 'DELAY' ? 'selected' : ''}>Delay</option>
                </select>
            </div>
            <div class="form-group" id="step-endpoint-group-${stepId}" style="${stepData?.action === 'DELAY' ? 'display: none;' : ''}">
                <label for="step-endpoint-${stepId}">Endpoint *</label>
                <select id="step-endpoint-${stepId}">
                    <option value="">Select endpoint...</option>
                    ${endpoints.map(e => `
                        <option value="${e.id}" ${stepData?.mockEndpoint?.id === e.id ? 'selected' : ''}>
                            ${e.name} (${e.protocol})
                        </option>
                    `).join('')}
                </select>
            </div>
            <div class="form-group" id="step-delay-group-${stepId}" style="${stepData?.action !== 'DELAY' ? 'display: none;' : ''}">
                <label for="step-delay-${stepId}">Delay (ms) *</label>
                <input type="number" id="step-delay-${stepId}" value="${stepData?.delayMs || 1000}" min="0">
            </div>
        </div>
        <div class="form-group">
            <label for="step-description-${stepId}">Description</label>
            <input type="text" id="step-description-${stepId}" value="${stepData?.description || ''}" placeholder="Optional description">
        </div>
    `;

    container.appendChild(stepDiv);
}

function toggleStepFields(stepId) {
    const action = document.getElementById(`step-action-${stepId}`).value;
    const endpointGroup = document.getElementById(`step-endpoint-group-${stepId}`);
    const delayGroup = document.getElementById(`step-delay-group-${stepId}`);

    if (action === 'DELAY') {
        endpointGroup.style.display = 'none';
        delayGroup.style.display = 'block';
    } else {
        endpointGroup.style.display = 'block';
        delayGroup.style.display = 'none';
    }
}

function removeScenarioStep(stepId) {
    document.getElementById(`step-${stepId}`).remove();
}

// Save scenario
async function saveScenario(event) {
    event.preventDefault();

    const name = document.getElementById('scenario-name').value;
    const description = document.getElementById('scenario-description').value;
    const color = document.getElementById('scenario-color').value;

    // Collect steps
    const steps = [];
    const stepsContainer = document.getElementById('scenario-steps-container');
    const stepDivs = stepsContainer.querySelectorAll('.scenario-step');

    stepDivs.forEach((stepDiv, index) => {
        const stepId = stepDiv.id.split('-')[1];
        const action = document.getElementById(`step-action-${stepId}`).value;
        const endpointId = document.getElementById(`step-endpoint-${stepId}`)?.value;
        const delayMs = document.getElementById(`step-delay-${stepId}`)?.value;
        const stepDescription = document.getElementById(`step-description-${stepId}`).value;

        const step = {
            stepOrder: index,
            action: action,
            description: stepDescription || null
        };

        if (action === 'DELAY') {
            step.delayMs = parseInt(delayMs);
        } else {
            if (endpointId) {
                step.mockEndpoint = { id: parseInt(endpointId) };
            }
        }

        steps.push(step);
    });

    const scenario = {
        name,
        description: description || null,
        color,
        steps
    };

    try {
        const url = currentScenarioId ? `/api/scenarios/${currentScenarioId}` : '/api/scenarios';
        const method = currentScenarioId ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(scenario)
        });

        if (!response.ok) {
            throw new Error('Failed to save scenario');
        }

        closeScenarioModal();
        loadScenarios();
    } catch (error) {
        alert('Error saving scenario: ' + error.message);
    }
}

// Delete scenario
async function deleteScenario(id) {
    if (!confirm('Weet je zeker dat je dit scenario wilt verwijderen?')) {
        return;
    }

    try {
        const response = await fetch(`/api/scenarios/${id}`, { method: 'DELETE' });
        if (!response.ok) {
            throw new Error('Failed to delete scenario');
        }
        loadScenarios();
    } catch (error) {
        alert('Error deleting scenario: ' + error.message);
    }
}

// Execute scenario
async function executeScenario(id) {
    if (!confirm('Weet je zeker dat je dit scenario wilt uitvoeren?')) {
        return;
    }

    try {
        const response = await fetch(`/api/scenarios/${id}/execute`, { method: 'POST' });
        if (!response.ok) {
            throw new Error('Failed to execute scenario');
        }
        alert('Scenario succesvol uitgevoerd!');
        loadEndpoints();
        loadDashboard();
    } catch (error) {
        alert('Error executing scenario: ' + error.message);
    }
}

// ===================================
// METRICS FUNCTIONS
// ===================================

// Load metrics
async function loadMetrics() {
    try {
        const metrics = await fetch('/api/metrics').then(r => r.json());
        const container = document.getElementById('metrics-list');

        if (metrics.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>Nog geen metrics</h3>
                    <p>Metrics worden automatisch verzameld wanneer endpoints worden gebruikt</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="table">
                <table>
                    <thead>
                        <tr>
                            <th>Endpoint</th>
                            <th>Protocol</th>
                            <th>Status</th>
                            <th>Total Requests</th>
                            <th>Matched</th>
                            <th>Unmatched</th>
                            <th>Success Rate</th>
                            <th>Avg Response Time</th>
                            <th>Last Request</th>
                            <th>Acties</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${metrics.map(m => `
                            <tr>
                                <td><strong>${m.endpointName}</strong></td>
                                <td><span class="badge info">${m.protocol}</span></td>
                                <td>
                                    ${m.enabled
                                        ? '<span class="badge success">Actief</span>'
                                        : '<span class="badge error">Inactief</span>'}
                                </td>
                                <td>${m.totalRequests}</td>
                                <td><span class="badge success">${m.matchedRequests}</span></td>
                                <td><span class="badge error">${m.unmatchedRequests}</span></td>
                                <td>${m.successRate}</td>
                                <td>${m.averageResponseTimeMs}ms</td>
                                <td>${m.lastRequestAt ? new Date(m.lastRequestAt).toLocaleString('nl-NL') : 'Never'}</td>
                                <td>
                                    <button class="btn btn-small btn-danger" onclick="resetEndpointMetrics(${m.endpointId})" title="Reset">
                                        🔄
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (error) {
        console.error('Error loading metrics:', error);
    }
}

// Refresh metrics
async function refreshMetrics() {
    await loadMetrics();
}

// Reset all metrics
async function resetAllMetrics() {
    if (!confirm('Weet je zeker dat je alle metrics wilt resetten?')) {
        return;
    }

    try {
        const response = await fetch('/api/metrics/reset-all', { method: 'POST' });
        if (!response.ok) {
            throw new Error('Failed to reset metrics');
        }
        loadMetrics();
        loadDashboard();
    } catch (error) {
        alert('Error resetting metrics: ' + error.message);
    }
}

// Reset endpoint metrics
async function resetEndpointMetrics(id) {
    if (!confirm('Weet je zeker dat je de metrics voor dit endpoint wilt resetten?')) {
        return;
    }

    try {
        const response = await fetch(`/api/metrics/${id}/reset`, { method: 'POST' });
        if (!response.ok) {
            throw new Error('Failed to reset endpoint metrics');
        }
        loadMetrics();
        loadDashboard();
    } catch (error) {
        alert('Error resetting endpoint metrics: ' + error.message);
    }
}

// Update AMQP defaults based on broker type
function updateAmqpDefaults() {
    const brokerType = document.getElementById('amqp-broker-type').value;
    const portField = document.getElementById('amqp-port');
    const vhostField = document.getElementById('amqp-vhost');
    const vhostLabel = document.getElementById('amqp-vhost-label');
    const vhostHint = document.getElementById('amqp-vhost-hint');
    const brokerHint = document.getElementById('amqp-broker-hint');

    if (brokerType === 'RABBITMQ') {
        portField.value = '5672';
        vhostField.value = '/';
        vhostField.placeholder = '/';
        vhostLabel.textContent = 'Virtual Host';
        vhostHint.textContent = 'RabbitMQ virtual host (default: /)';
        brokerHint.textContent = 'RabbitMQ - Default poort: 5672, Protocol: AMQP 0.9.1';
    } else if (brokerType === 'ARTEMIS') {
        portField.value = '61616';
        vhostField.value = '/';
        vhostField.placeholder = '/';
        vhostLabel.textContent = 'Virtual Host';
        vhostHint.textContent = 'Artemis ondersteunt geen virtual hosts (gebruik /)';
        brokerHint.textContent = 'Apache Artemis - Default poort: 61616, Protocol: JMS 2.0';
    } else if (brokerType === 'IBM_MQ') {
        portField.value = '1414';
        vhostField.value = 'QM1';
        vhostField.placeholder = 'QM1';
        vhostLabel.textContent = 'Queue Manager';
        vhostHint.textContent = 'IBM MQ Queue Manager naam (bijv. QM1)';
        brokerHint.textContent = 'IBM MQ - Default poort: 1414, Protocol: JMS via IBM MQ';
    }
}

// Initialize on page load
loadDashboard();
startAutoRefresh(); // Start auto-refresh (will only work when logs tab is active)
