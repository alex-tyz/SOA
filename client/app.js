'use strict';

const STORAGE_KEY = 'soa-lab2-settings-v2';
const PROXY_URL = new URL('api.php', window.location.href).href;
const DEFAULTS = {
    marineUrl: PROXY_URL + '?service=marine',
    starshipUrl: PROXY_URL + '?service=starship'
};

const FIELDS = [
    {param: 'id', label: 'id'},
    {param: 'name', label: 'Имя'},
    {param: 'coordinates.x', label: 'Координата X'},
    {param: 'coordinates.y', label: 'Координата Y'},
    {param: 'creationDate', label: 'Дата создания (ГГГГ-ММ-ДД)'},
    {param: 'health', label: 'Здоровье'},
    {param: 'loyal', label: 'Лоялен', bool: true},
    {param: 'achievements', label: 'Достижения'},
    {param: 'weaponType', label: 'Оружие', weapon: true},
    {param: 'chapter.name', label: 'Орден'},
    {param: 'chapter.parentLegion', label: 'Легион ордена'}
];

const COLUMNS = [
    ['id', m => m.id],
    ['Имя', m => m.name],
    ['X', m => m.coordinates && m.coordinates.x],
    ['Y', m => m.coordinates && m.coordinates.y],
    ['Создан', m => m.creationDate],
    ['Здоровье', m => m.health],
    ['Лоялен', m => m.loyal === 'true' ? 'да' : m.loyal === 'false' ? 'нет' : null],
    ['Достижения', m => m.achievements],
    ['Оружие', m => m.weaponType],
    ['Орден', m => m.chapter && m.chapter.name],
    ['Легион', m => m.chapter && m.chapter.parentLegion]
];

const state = {page: 1, size: 10, totalPages: 0};
let settings = loadSettings();
let editingId = null;

const $ = selector => document.querySelector(selector);

function el(tag, props, ...children) {
    const node = Object.assign(document.createElement(tag), props);
    node.append(...children.flat().filter(child => child != null));
    return node;
}

function loadSettings() {
    try {
        return {...DEFAULTS, ...JSON.parse(localStorage.getItem(STORAGE_KEY))};
    } catch (e) {
        return {...DEFAULTS};
    }
}

function saveSettings() {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch (e) {
    }
}

class ApiError extends Error {
    constructor(status, message, details = [], url = '', network = false) {
        super(message);
        Object.assign(this, {status, details, url, network});
    }
}

function parseXml(body) {
    const doc = new DOMParser().parseFromString(body, 'application/xml');
    if (doc.querySelector('parsererror')) {
        throw new ApiError(0, 'Сервис вернул некорректный XML');
    }
    return doc;
}

function children(node, name) {
    return node ? [...node.children].filter(c => c.nodeName === name) : [];
}

function child(node, name) {
    return children(node, name)[0] || null;
}

function text(node, name) {
    const found = child(node, name);
    return found ? found.textContent : null;
}

function parseMarine(node) {
    const coordinates = child(node, 'coordinates');
    const chapter = child(node, 'chapter');
    return {
        id: text(node, 'id'),
        name: text(node, 'name'),
        coordinates: coordinates && {x: text(coordinates, 'x'), y: text(coordinates, 'y')},
        creationDate: text(node, 'creationDate'),
        health: text(node, 'health'),
        loyal: text(node, 'loyal'),
        achievements: text(node, 'achievements'),
        weaponType: text(node, 'weaponType'),
        chapter: chapter && {name: text(chapter, 'name'), parentLegion: text(chapter, 'parentLegion')}
    };
}

function parseMarines(wrapper) {
    return children(wrapper, 'spaceMarine').map(parseMarine);
}

function parsePage(doc) {
    const root = doc.documentElement;
    return {
        marines: parseMarines(child(root, 'spaceMarines')),
        page: Number(text(root, 'page')),
        totalElements: Number(text(root, 'totalElements')),
        totalPages: Number(text(root, 'totalPages'))
    };
}

function errorFromResponse(status, body, url) {
    try {
        const root = parseXml(body).documentElement;
        if (root.nodeName === 'error') {
            const details = children(child(root, 'details'), 'detail').map(d => d.textContent);
            return new ApiError(status, text(root, 'message'), details, url);
        }
    } catch (e) {
    }
    return new ApiError(status, `Сервис ответил кодом ${status}`, [], url);
}

async function request(baseUrl, path, {method = 'GET', body = null, query = null} = {}) {
    const base = new URL(baseUrl);
    let url;
    let transportMethod = method;
    if (base.pathname.endsWith('/api.php') && base.searchParams.has('service')) {
        base.searchParams.set('path', path);
        if (query) {
            base.searchParams.set('query', query.toString());
        }
        if (method === 'PUT' || method === 'DELETE') {
            base.searchParams.set('method', method);
            transportMethod = 'POST';
        }
        url = base.href;
    } else {
        url = baseUrl.replace(/\/+$/, '') + path + (query ? '?' + query : '');
    }
    const headers = {Accept: 'application/xml'};
    if (body !== null) {
        headers['Content-Type'] = 'application/xml';
    }
    let response;
    try {
        response = await fetch(url, {method: transportMethod, headers, body});
    } catch (e) {
        throw new ApiError(0, 'Не удалось связаться с сервисом', [], url, true);
    }
    const payload = await response.text();
    if (!response.ok) {
        throw errorFromResponse(response.status, payload, url);
    }
    return payload ? parseXml(payload) : null;
}

const marineApi = (path, options) => request(settings.marineUrl, path, options);
const starshipApi = (path, options) => request(settings.starshipUrl, path, options);

function xmlElement(name, value) {
    const escaped = String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return `<${name}>${escaped}</${name}>`;
}

function buildMarineXml(v) {
    const optional = (name, value) => value === '' ? '' : xmlElement(name, value);
    const chapter = v.chapterName === '' && v.parentLegion === '' ? ''
        : '<chapter>' + xmlElement('name', v.chapterName) + optional('parentLegion', v.parentLegion) + '</chapter>';
    return '<?xml version="1.0" encoding="UTF-8"?><spaceMarine>'
        + xmlElement('name', v.name)
        + '<coordinates>' + optional('x', v.x) + optional('y', v.y) + '</coordinates>'
        + optional('health', v.health)
        + optional('loyal', v.loyal)
        + optional('achievements', v.achievements)
        + optional('weaponType', v.weaponType)
        + chapter
        + '</spaceMarine>';
}

function showMessage(kind, ...content) {
    const box = el('div', {className: 'message ' + kind}, ...content);
    $('#messages').replaceChildren(box);
    box.scrollIntoView({block: 'nearest'});
}

function showInfo(textValue) {
    showMessage('ok', textValue);
}

function showError(error) {
    const e = error instanceof ApiError ? error : new ApiError(0, String(error));
    const content = [el('strong', {}, e.status ? `Ошибка ${e.status}` : 'Ошибка')];
    if (e.message) {
        content.push(el('p', {}, e.message));
    }
    if (e.details.length) {
        content.push(el('ul', {}, e.details.map(d => el('li', {}, d))));
    }
    if (e.network) {
        content.push(el('p', {}, 'Проверьте доступность сервиса и повторите действие.'));
    }
    showMessage('error', ...content);
}

async function run(action) {
    $('#messages').replaceChildren();
    try {
        await action();
    } catch (error) {
        showError(error);
    }
}

function onSubmit(selector, handler) {
    $(selector).addEventListener('submit', event => {
        event.preventDefault();
        run(() => handler(event));
    });
}

function marinesTable(marines, withActions) {
    if (!marines.length) {
        return el('p', {className: 'empty'}, 'Ничего не найдено');
    }
    const head = el('tr', {}, COLUMNS.map(([title]) => el('th', {}, title)), withActions ? el('th') : null);
    const rows = marines.map(marine => el('tr', {},
        COLUMNS.map(([, value]) => el('td', {}, value(marine) ?? '—')),
        withActions ? el('td', {className: 'actions'},
            el('button', {type: 'button', className: 'secondary', onclick: () => fillEditor(marine)}, 'Изменить'),
            el('button', {type: 'button', className: 'danger', onclick: () => deleteMarine(marine)}, 'Удалить')
        ) : null));
    return el('table', {}, el('thead', {}, head), el('tbody', {}, rows));
}

function resultCard(title, content) {
    return [el('h3', {}, title), content];
}

function buildFilters() {
    $('#filters').replaceChildren(...FIELDS.map(field => {
        let input;
        if (field.bool) {
            input = el('select', {id: 'f-' + field.param},
                el('option', {value: ''}, 'любое'),
                el('option', {value: 'true'}, 'да'),
                el('option', {value: 'false'}, 'нет'));
        } else {
            input = el('input', {type: 'text', id: 'f-' + field.param});
            if (field.weapon) {
                input.setAttribute('list', 'weapons');
            }
        }
        return el('label', {}, field.label, input);
    }));
}

function addSortRow() {
    const field = el('select', {}, FIELDS.map(f => el('option', {value: f.param}, f.label)));
    const direction = el('select', {},
        el('option', {value: ''}, 'по возрастанию'),
        el('option', {value: '-'}, 'по убыванию'));
    const row = el('div', {className: 'sort-row'}, field, direction,
        el('button', {type: 'button', className: 'secondary', onclick: () => row.remove()}, 'Убрать'));
    $('#sort-rows').append(row);
}

function buildQuery() {
    const query = new URLSearchParams({page: state.page, size: state.size});
    for (const row of $('#sort-rows').children) {
        const [field, direction] = row.querySelectorAll('select');
        query.append('sort', direction.value + field.value);
    }
    for (const field of FIELDS) {
        const value = document.getElementById('f-' + field.param).value;
        if (value !== '') {
            query.set(field.param, value);
        }
    }
    return query;
}

async function loadMarines() {
    const data = parsePage(await marineApi('/space-marines', {query: buildQuery()}));
    if (!data.marines.length && data.totalElements > 0 && state.page > 1) {
        state.page = data.totalPages;
        return loadMarines();
    }
    state.totalPages = data.totalPages;
    $('#marines-table').replaceChildren(marinesTable(data.marines, true));
    $('#page-info').textContent = data.totalElements
        ? `Страница ${data.page} из ${data.totalPages}, всего ${data.totalElements}`
        : 'Нет данных';
    $('#page-first').disabled = $('#page-prev').disabled = state.page <= 1;
    $('#page-next').disabled = $('#page-last').disabled = state.page >= state.totalPages;
}

const reload = () => loadMarines().catch(showError);

function goToPage(page) {
    state.page = Math.max(1, page);
    run(loadMarines);
}

function applyFilters() {
    const size = Number($('#page-size').value);
    state.size = Number.isInteger(size) && size >= 1 ? size : 10;
    state.page = 1;
    run(loadMarines);
}

function resetFilters() {
    for (const field of FIELDS) {
        document.getElementById('f-' + field.param).value = '';
    }
    $('#sort-rows').replaceChildren();
    $('#page-size').value = 10;
    applyFilters();
}

function fillEditor(marine) {
    editingId = marine ? marine.id : null;
    $('#editor-title').textContent = marine ? `Изменить десантника #${marine.id}` : 'Добавить десантника';
    const values = marine ? {
        name: marine.name,
        x: marine.coordinates && marine.coordinates.x,
        y: marine.coordinates && marine.coordinates.y,
        health: marine.health,
        loyal: marine.loyal,
        achievements: marine.achievements,
        weaponType: marine.weaponType,
        chapterName: marine.chapter && marine.chapter.name,
        parentLegion: marine.chapter && marine.chapter.parentLegion
    } : {loyal: 'true'};
    for (const input of $('#editor-form').elements) {
        if (input.name) {
            input.value = values[input.name] ?? '';
        }
    }
    if (marine) {
        $('#editor-title').scrollIntoView();
    }
}

async function saveMarine(event) {
    const values = Object.fromEntries([...event.target.elements].filter(i => i.name).map(i => [i.name, i.value]));
    const body = buildMarineXml(values);
    const creating = editingId === null;
    const doc = creating
        ? await marineApi('/space-marines', {method: 'POST', body})
        : await marineApi('/space-marines/' + encodeURIComponent(editingId), {method: 'PUT', body});
    const saved = parseMarine(doc.documentElement);
    fillEditor(null);
    showInfo(creating ? `Десантник создан, id=${saved.id}` : `Десантник #${saved.id} обновлён`);
    reload();
}

function deleteMarine(marine) {
    if (!confirm(`Удалить десантника #${marine.id} «${marine.name}»?`)) {
        return;
    }
    run(async () => {
        await marineApi('/space-marines/' + encodeURIComponent(marine.id), {method: 'DELETE'});
        showInfo(`Десантник #${marine.id} удалён`);
        reload();
    });
}

async function deleteByWeapon() {
    const doc = await marineApi('/space-marines/by-weapon-type/' + encodeURIComponent($('#extra-weapon').value),
        {method: 'DELETE'});
    $('#extra-result').replaceChildren(...resultCard('Удалён десантник', marinesTable([parseMarine(doc.documentElement)], false)));
    reload();
}

async function searchByAchievements() {
    const query = new URLSearchParams({substring: $('#extra-substring').value});
    const doc = await marineApi('/space-marines/by-achievements', {query});
    const marines = parseMarines(doc.documentElement);
    $('#extra-result').replaceChildren(...resultCard(`Найдено: ${marines.length}`, marinesTable(marines, false)));
}

async function showUniqueAchievements() {
    const doc = await marineApi('/space-marines/achievements/unique');
    const values = children(doc.documentElement, 'achievement').map(a => a.textContent);
    $('#extra-result').replaceChildren(...resultCard(`Уникальных значений: ${values.length}`,
        values.length ? el('ul', {}, values.map(v => el('li', {}, v))) : el('p', {className: 'empty'}, 'Значений нет')));
}

async function loadOnShip() {
    $('#ship-result').replaceChildren();
    const path = `/starship/${encodeURIComponent($('#load-ship').value)}/load/${encodeURIComponent($('#load-marine').value)}`;
    const root = (await starshipApi(path, {method: 'POST'})).documentElement;
    const marines = parseMarines(child(root, 'spaceMarines'));
    showInfo('Десантник погружен на корабль');
    $('#ship-result').replaceChildren(...resultCard(`Корабль #${text(root, 'id')}: на борту ${marines.length}`,
        marinesTable(marines, false)));
}

async function unloadShip() {
    $('#ship-result').replaceChildren();
    const doc = await starshipApi(`/starship/${encodeURIComponent($('#unload-ship').value)}/unload-all`, {method: 'POST'});
    const marines = parseMarines(doc.documentElement);
    showInfo(marines.length ? `Высажено десантников: ${marines.length}` : 'На корабле никого не было');
    $('#ship-result').replaceChildren(...resultCard('Высаженные десантники', marinesTable(marines, false)));
}

function syncSettingsForm() {
    $('#marine-url').value = settings.marineUrl;
    $('#starship-url').value = settings.starshipUrl;
}

async function saveSettingsForm() {
    const marineUrl = $('#marine-url').value.trim();
    const starshipUrl = $('#starship-url').value.trim();
    try {
        new URL(marineUrl);
        new URL(starshipUrl);
    } catch (e) {
        throw new ApiError(0, 'Адрес сервиса указан неверно');
    }
    settings = {marineUrl, starshipUrl};
    saveSettings();
    syncSettingsForm();
    await loadMarines();
}

function init() {
    buildFilters();
    syncSettingsForm();
    $('#filters-apply').addEventListener('click', applyFilters);
    $('#filters-reset').addEventListener('click', resetFilters);
    $('#filters').addEventListener('keydown', event => event.key === 'Enter' && applyFilters());
    $('#sort-add').addEventListener('click', addSortRow);
    $('#page-size').addEventListener('change', applyFilters);
    $('#page-first').addEventListener('click', () => goToPage(1));
    $('#page-prev').addEventListener('click', () => goToPage(state.page - 1));
    $('#page-next').addEventListener('click', () => goToPage(state.page + 1));
    $('#page-last').addEventListener('click', () => goToPage(state.totalPages));
    $('#editor-cancel').addEventListener('click', () => fillEditor(null));
    $('#extra-unique').addEventListener('click', () => run(showUniqueAchievements));
    onSubmit('#settings-form', saveSettingsForm);
    onSubmit('#editor-form', saveMarine);
    onSubmit('#extra-delete-weapon', deleteByWeapon);
    onSubmit('#extra-search', searchByAchievements);
    onSubmit('#ship-load', loadOnShip);
    onSubmit('#ship-unload', unloadShip);
    fillEditor(null);
    run(loadMarines);
}

init();
