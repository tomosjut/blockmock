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
                                details = `${endpoint.amqpConfig.operation} <code>${endpoint.amqpConfig.exchangeName}</code>`;
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

    // Add default response
    addResponse(true);

    document.getElementById('endpoint-modal').classList.add('active');
}

// Add response card
function addResponse(isDefault = false) {
    const responseId = responseIdCounter++;
    const response = {
        id: responseId,
        name: isDefault ? 'Default Response' : `Response ${responseId + 1}`,
        priority: isDefault ? 0 : responses.length,
        matchHeaders: {},
        matchQueryParams: {},
        matchBody: '',
        responseStatusCode: 200,
        responseBody: '',
        responseHeaders: {},
        responseDelayMs: 0,
        isDefault: isDefault
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
    container.innerHTML = responses.map((response, index) => `
        <div class="response-card ${response.isDefault ? 'default' : ''}" data-response-id="${response.id}">
            <div class="response-card-header">
                <h4>${response.isDefault ? '🌟 ' : ''}${response.name}</h4>
                <div class="response-card-actions">
                    ${!response.isDefault ? `
                        <button type="button" class="btn btn-small btn-secondary" onclick="moveResponseUp(${response.id})" ${index === 0 ? 'disabled' : ''} title="Omhoog">⬆️</button>
                        <button type="button" class="btn btn-small btn-secondary" onclick="moveResponseDown(${response.id})" ${index === responses.length - 1 ? 'disabled' : ''} title="Omlaag">⬇️</button>
                        <button type="button" class="btn btn-small btn-danger" onclick="removeResponse(${response.id})" title="Verwijderen">🗑️</button>
                    ` : ''}
                </div>
            </div>
            <div class="response-card-body">
                ${!response.isDefault ? `
                <div class="response-section">
                    <h5>Matching Criteria</h5>
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
                ` : ''}
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
                    responseDelayMs: resp.responseDelayMs || 0,
                    isDefault: index === 0 && (!resp.matchHeaders || Object.keys(resp.matchHeaders).length === 0) &&
                               (!resp.matchQueryParams || Object.keys(resp.matchQueryParams).length === 0) &&
                               !resp.matchBody
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

    // Hide all
    httpFields.style.display = 'none';
    sftpFields.style.display = 'none';
    amqpFields.style.display = 'none';
    sqlFields.style.display = 'none';

    // Show relevant
    if (protocol === 'HTTP' || protocol === 'HTTPS') {
        httpFields.style.display = 'block';
    } else if (protocol === 'SFTP') {
        sftpFields.style.display = 'block';
    } else if (protocol === 'AMQP') {
        amqpFields.style.display = 'block';
    } else if (protocol === 'SQL') {
        sqlFields.style.display = 'block';
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

// Initialize on page load
loadDashboard();
startAutoRefresh(); // Start auto-refresh (will only work when logs tab is active)
