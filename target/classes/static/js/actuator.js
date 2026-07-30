const ACTUATOR_BASE = '/actuator';

document.addEventListener('DOMContentLoaded', () => {
    loadWhoAmI();
    loadHealth();
    loadInfo();
    loadMetrics();
    loadMappings();
    loadEnv();
    loadBeans();
});

document.getElementById('btnRefreshAll').addEventListener('click', () => {
    loadHealth();
    loadInfo();
    loadMetrics();
    loadMappings();
    loadEnv();
    loadBeans();
});

async function loadWhoAmI() {
    try {
        const me = await getJson('/api/me');
        document.getElementById('navWhoAmI').innerHTML =
            `<i class="bi bi-person-circle me-1"></i>${escapeHtml(me.username)}`;
    } catch {
        // harmless if unreachable - nav just won't show a name
    }
}

// ---- small helpers --------------------------------------------------------

async function getJson(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

function statusBadgeClass(status) {
    switch (status) {
        case 'UP': return 'text-bg-success';
        case 'DOWN': return 'text-bg-danger';
        case 'OUT_OF_SERVICE': return 'text-bg-warning';
        default: return 'text-bg-secondary';
    }
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str ?? '';
    return div.innerHTML;
}

function errorPanel(message) {
    return `<div class="alert alert-danger py-2 mb-0"><i class="bi bi-exclamation-triangle me-1"></i>${escapeHtml(message)}</div>`;
}

/** Flattens a nested object into dot-notation {key: value} pairs, for display. */
function flatten(obj, prefix = '', out = {}) {
    for (const [key, value] of Object.entries(obj ?? {})) {
        const path = prefix ? `${prefix}.${key}` : key;
        if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
            flatten(value, path, out);
        } else {
            out[path] = Array.isArray(value) ? value.join(', ') : value;
        }
    }
    return out;
}

// ---- HEALTH ----------------------------------------------------------------

async function loadHealth() {
    const overallEl = document.getElementById('healthOverall');
    const componentsEl = document.getElementById('healthComponents');
    try {
        const health = await getJson(`${ACTUATOR_BASE}/health`);
        overallEl.innerHTML = `
            <span class="badge fs-6 ${statusBadgeClass(health.status)}">${health.status}</span>
            <span class="text-muted ms-2">overall application status</span>`;

        const components = health.components || {};
        componentsEl.innerHTML = Object.entries(components).map(([name, comp]) => `
            <div class="col-md-4">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h6 class="card-title mb-0">${escapeHtml(name)}</h6>
                            <span class="badge ${statusBadgeClass(comp.status)}">${comp.status}</span>
                        </div>
                        ${comp.details ? `<pre class="small text-muted mb-0 bg-light p-2 rounded">${escapeHtml(JSON.stringify(comp.details, null, 2))}</pre>` : ''}
                    </div>
                </div>
            </div>`).join('') || '<div class="col-12 text-muted">No sub-components reported.</div>';
    } catch (err) {
        overallEl.innerHTML = errorPanel(`Couldn't load health: ${err.message}`);
        componentsEl.innerHTML = '';
    }
}

// ---- INFO --------------------------------------------------------------

async function loadInfo() {
    const rowsEl = document.getElementById('infoRows');
    try {
        const info = await getJson(`${ACTUATOR_BASE}/info`);
        const flat = flatten(info);
        const entries = Object.entries(flat);
        rowsEl.innerHTML = entries.length
            ? entries.map(([k, v]) => `<tr><td class="text-muted" style="width:320px">${escapeHtml(k)}</td><td>${escapeHtml(String(v))}</td></tr>`).join('')
            : `<tr><td class="text-muted">Nothing published under <code>info.*</code> yet &mdash; add <code>info.*</code> keys to application.properties.</td></tr>`;
    } catch (err) {
        rowsEl.innerHTML = `<tr><td>${errorPanel(`Couldn't load info: ${err.message}`)}</td></tr>`;
    }
}

// ---- METRICS ----------------------------------------------------------

const QUICK_METRICS = ['jvm.memory.used', 'jvm.memory.max', 'system.cpu.usage', 'http.server.requests'];
let allMetricNames = [];

async function loadMetrics() {
    const quickEl = document.getElementById('metricQuickStats');
    const listEl = document.getElementById('metricList');

    quickEl.innerHTML = (await Promise.all(QUICK_METRICS.map(renderQuickStat))).join('');

    try {
        const index = await getJson(`${ACTUATOR_BASE}/metrics`);
        allMetricNames = index.names || [];
        renderMetricList(allMetricNames);
    } catch (err) {
        listEl.innerHTML = errorPanel(`Couldn't load metric list: ${err.message}`);
    }
}

async function renderQuickStat(name) {
    try {
        const data = await getJson(`${ACTUATOR_BASE}/metrics/${encodeURIComponent(name)}`);
        const measurement = data.measurements?.[0];
        const value = measurement ? formatMetricValue(name, measurement.value) : '&mdash;';
        return `
            <div class="col-md-3">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="text-muted small text-truncate" title="${escapeHtml(name)}">${escapeHtml(name)}</div>
                        <div class="fs-4">${value}</div>
                    </div>
                </div>
            </div>`;
    } catch {
        return ''; // metric not present on this JVM/run - just skip the card
    }
}

function formatMetricValue(name, value) {
    if (name.includes('memory')) return `${(value / (1024 * 1024)).toFixed(1)} MB`;
    if (name.includes('cpu.usage')) return `${(value * 100).toFixed(1)}%`;
    return Number.isInteger(value) ? value : value.toFixed(3);
}

function renderMetricList(names) {
    const listEl = document.getElementById('metricList');
    listEl.innerHTML = names.map(name => `
        <button type="button" class="list-group-item list-group-item-action" onclick="showMetricDetail('${name}')">
            ${escapeHtml(name)}
        </button>`).join('') || '<div class="text-muted p-2">No metrics match.</div>';
}

document.getElementById('metricFilter').addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    renderMetricList(allMetricNames.filter(n => n.toLowerCase().includes(q)));
});

async function showMetricDetail(name) {
    const detailEl = document.getElementById('metricDetail');
    detailEl.innerHTML = '<span class="text-muted">Loading&hellip;</span>';
    try {
        const data = await getJson(`${ACTUATOR_BASE}/metrics/${encodeURIComponent(name)}`);
        const measurements = (data.measurements || []).map(m =>
            `<tr><td>${escapeHtml(m.statistic)}</td><td>${m.value}</td></tr>`).join('');
        const tags = (data.availableTags || []).map(t =>
            `<div class="mb-2"><strong>${escapeHtml(t.tag)}</strong>: ${t.values.map(escapeHtml).join(', ')}</div>`).join('');

        detailEl.innerHTML = `
            <h6>${escapeHtml(data.name)}</h6>
            <table class="table table-sm w-auto"><tbody>${measurements}</tbody></table>
            ${tags ? `<h6 class="mt-3">Available tags</h6>${tags}` : ''}`;
    } catch (err) {
        detailEl.innerHTML = errorPanel(`Couldn't load ${name}: ${err.message}`);
    }
}

// ---- MAPPINGS -----------------------------------------------------------

let allMappingRows = [];

async function loadMappings() {
    const tbody = document.getElementById('mappingRows');
    try {
        const data = await getJson(`${ACTUATOR_BASE}/mappings`);
        const contexts = data.contexts || {};
        allMappingRows = [];

        for (const ctx of Object.values(contexts)) {
            const dispatchers = ctx.mappings?.dispatcherServlets || {};
            for (const handlers of Object.values(dispatchers)) {
                for (const m of handlers) {
                    const conditions = m.details?.requestMappingConditions;
                    const methods = conditions?.methods?.length ? conditions.methods.join(', ') : 'ANY';
                    const patterns = conditions?.patterns?.patterns?.length
                        ? conditions.patterns.patterns.join(', ')
                        : (m.predicate || '');
                    allMappingRows.push({ methods, patterns, handler: m.handler || '' });
                }
            }
        }
        renderMappingRows(allMappingRows);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="3">${errorPanel(`Couldn't load mappings: ${err.message}`)}</td></tr>`;
    }
}

function renderMappingRows(rows) {
    const tbody = document.getElementById('mappingRows');
    tbody.innerHTML = rows.map(r => `
        <tr>
            <td><span class="badge text-bg-light border">${escapeHtml(r.methods)}</span></td>
            <td><code>${escapeHtml(r.patterns)}</code></td>
            <td class="text-muted small">${escapeHtml(r.handler)}</td>
        </tr>`).join('') || '<tr><td colspan="3" class="text-muted">No mappings match.</td></tr>';
}

document.getElementById('mappingFilter').addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    renderMappingRows(allMappingRows.filter(r =>
        r.patterns.toLowerCase().includes(q) || r.handler.toLowerCase().includes(q)));
});

// ---- ENV ------------------------------------------------------------------

async function loadEnv() {
    const accordionEl = document.getElementById('envSourcesAccordion');
    try {
        const data = await getJson(`${ACTUATOR_BASE}/env`);
        const sources = data.propertySources || [];
        accordionEl.innerHTML = sources.map((src, i) => {
            const props = Object.entries(src.properties || {});
            const rows = props.map(([key, val]) => `
                <tr><td class="text-muted">${escapeHtml(key)}</td><td class="text-break">${escapeHtml(String(val.value))}</td></tr>`).join('');
            return `
                <div class="accordion-item">
                    <h2 class="accordion-header">
                        <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#src-${i}">
                            ${escapeHtml(src.name)} <span class="badge text-bg-secondary ms-2">${props.length}</span>
                        </button>
                    </h2>
                    <div id="src-${i}" class="accordion-collapse collapse" data-bs-parent="#envSourcesAccordion">
                        <div class="accordion-body p-0">
                            <div class="table-responsive" style="max-height:320px">
                                <table class="table table-sm mb-0"><tbody>${rows}</tbody></table>
                            </div>
                        </div>
                    </div>
                </div>`;
        }).join('');
    } catch (err) {
        accordionEl.innerHTML = errorPanel(`Couldn't load environment: ${err.message}`);
    }
}

let envSearchTimeout;
document.getElementById('envSearch').addEventListener('input', (e) => {
    clearTimeout(envSearchTimeout);
    const query = e.target.value.trim();
    const resultEl = document.getElementById('envSearchResult');
    if (!query) {
        resultEl.innerHTML = '';
        return;
    }
    envSearchTimeout = setTimeout(async () => {
        try {
            const data = await getJson(`${ACTUATOR_BASE}/env/${encodeURIComponent(query)}`);
            if (!data.property) {
                resultEl.innerHTML = `<div class="text-muted">No active property resolves for "${escapeHtml(query)}".</div>`;
                return;
            }
            const others = (data.propertySources || [])
                .filter(s => s.property?.value !== undefined && s.name !== data.property.source)
                .map(s => `<div class="small text-muted">${escapeHtml(s.name)}: <code>${escapeHtml(String(s.property.value))}</code></div>`)
                .join('');
            resultEl.innerHTML = `
                <div class="alert alert-primary py-2 mb-1">
                    <strong>${escapeHtml(data.property.source)}</strong> = <code>${escapeHtml(String(data.property.value))}</code>
                </div>
                ${others ? `<div class="ms-1">Also defined in:${others}</div>` : ''}`;
        } catch {
            resultEl.innerHTML = `<div class="text-muted">No active property resolves for "${escapeHtml(query)}".</div>`;
        }
    }, 250);
});

// ---- BEANS ----------------------------------------------------------------

let allBeans = [];

async function loadBeans() {
    const tbody = document.getElementById('beanRows');
    const countEl = document.getElementById('beanCount');
    try {
        const data = await getJson(`${ACTUATOR_BASE}/beans`);
        const contexts = data.contexts || {};
        allBeans = [];
        for (const ctx of Object.values(contexts)) {
            for (const [name, bean] of Object.entries(ctx.beans || {})) {
                allBeans.push({
                    name,
                    scope: bean.scope || 'singleton',
                    type: bean.type || '',
                    dependencies: bean.dependencies || [],
                });
            }
        }
        renderBeanRows(allBeans);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4">${errorPanel(`Couldn't load beans: ${err.message}`)}</td></tr>`;
        countEl.textContent = '';
    }
}

function renderBeanRows(beans) {
    const tbody = document.getElementById('beanRows');
    const countEl = document.getElementById('beanCount');
    tbody.innerHTML = beans.map(b => `
        <tr>
            <td>${escapeHtml(b.name)}</td>
            <td><span class="badge text-bg-light border">${escapeHtml(b.scope)}</span></td>
            <td class="text-muted small text-truncate d-inline-block" style="max-width:520px" title="${escapeHtml(b.type)}">${escapeHtml(b.type)}</td>
            <td class="text-end">${b.dependencies.length}</td>
        </tr>`).join('') || '<tr><td colspan="4" class="text-muted">No beans match.</td></tr>';
    countEl.textContent = `${beans.length} bean${beans.length === 1 ? '' : 's'}`;
}

document.getElementById('beanFilter').addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    renderBeanRows(allBeans.filter(b =>
        b.name.toLowerCase().includes(q) || b.type.toLowerCase().includes(q)));
});
