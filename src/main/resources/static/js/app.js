const API_BASE = '/api/products';
const { useEffect, useMemo, useRef, useState } = React;
const { createRoot } = ReactDOM;

const availableLanguages = ['en', 'fr'];

async function loadTranslations(locale) {
    const response = await fetch(`/i18n/${locale}.json`);
    if (!response.ok) {
        throw new Error(`Unable to load locale ${locale}`);
    }
    return response.json();
}

function App() {
    const [lang, setLang] = useState('en');
    const [translations, setTranslations] = useState({});
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [user, setUser] = useState(null);
    const [productForm, setProductForm] = useState({ id: '', name: '', description: '', price: '', quantity: '' });
    const [editingProductId, setEditingProductId] = useState(null);
    const [formError, setFormError] = useState('');
    const [historyProduct, setHistoryProduct] = useState(null);
    const [historyRevisions, setHistoryRevisions] = useState([]);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [historyError, setHistoryError] = useState('');
    const [toastMessage, setToastMessage] = useState('');
    const [toastVariant, setToastVariant] = useState('success');

    const productModalRef = useRef(null);
    const historyModalRef = useRef(null);
    const toastRef = useRef(null);
    const modalControllers = useRef({});

    useEffect(() => {
        let active = true;
        loadTranslations(lang)
            .then(bundle => {
                if (active) {
                    setTranslations(bundle);
                }
            })
            .catch(() => {
                if (active) {
                    setTranslations({});
                }
            });
        return () => {
            active = false;
        };
    }, [lang]);

    const t = key => translations[key] || key;
    const locale = lang === 'fr' ? 'fr-FR' : 'en-US';
    const currencyFormatter = useMemo(() => new Intl.NumberFormat(locale, { style: 'currency', currency: 'USD' }), [locale]);
    const dateFormatter = useMemo(() => new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' }), [locale]);

    useEffect(() => {
        modalControllers.current.productModal = new bootstrap.Modal(productModalRef.current);
        modalControllers.current.historyModal = new bootstrap.Modal(historyModalRef.current);
        modalControllers.current.toast = new bootstrap.Toast(toastRef.current, { delay: 3500 });
        loadProducts();
        loadWhoAmI();
    }, []);

    useEffect(() => {
        if (!toastMessage) {
            return;
        }
        modalControllers.current.toast?.show();
    }, [toastMessage]);

    async function loadWhoAmI() {
        try {
            const me = await apiGet('/api/me');
            setUser(me);
        } catch {
            setUser(null);
        }
    }

    async function loadProducts() {
        setLoading(true);
        setError('');
        try {
            const result = await apiGet(API_BASE);
            setProducts(result);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    function openProductModal(product = null) {
        if (product) {
            setEditingProductId(product.id);
            setProductForm({
                id: product.id,
                name: product.name || '',
                description: product.description || '',
                price: product.price ?? '',
                quantity: product.quantity ?? ''
            });
        } else {
            setEditingProductId(null);
            setProductForm({ id: '', name: '', description: '', price: '', quantity: '' });
        }
        setFormError('');
        modalControllers.current.productModal?.show();
    }

    function closeProductModal() {
        modalControllers.current.productModal?.hide();
        setFormError('');
    }

    async function handleSubmit(evt) {
        evt.preventDefault();
        const trimmedName = productForm.name.trim();
        const price = Number(productForm.price);
        const quantity = Number(productForm.quantity);

        if (!trimmedName || Number.isNaN(price) || Number.isNaN(quantity)) {
            setFormError(t('requiredHint'));
            return;
        }

        const payload = {
            name: trimmedName,
            description: productForm.description,
            price,
            quantity
        };

        try {
            if (editingProductId) {
                await apiSend(`${API_BASE}/${editingProductId}`, 'PUT', payload);
                showToast(t('updatedMessage'), 'success');
            } else {
                await apiSend(API_BASE, 'POST', payload);
                showToast(t('createdMessage'), 'success');
            }
            closeProductModal();
            await loadProducts();
        } catch (err) {
            setFormError(err.message);
        }
    }

    async function handleDelete(id) {
        if (!window.confirm(t('deleteConfirm'))) {
            return;
        }
        try {
            await apiSend(`${API_BASE}/${id}`, 'DELETE');
            showToast(t('deletedMessage'), 'success');
            await loadProducts();
        } catch (err) {
            showToast(err.message, 'danger');
        }
    }

    async function openHistoryModal(product) {
        setHistoryProduct(product);
        setHistoryLoading(true);
        setHistoryError('');
        setHistoryRevisions([]);
        modalControllers.current.historyModal?.show();
        try {
            const revisions = await apiGet(`${API_BASE}/${product.id}/history`);
            setHistoryRevisions(revisions);
        } catch (err) {
            setHistoryError(err.message);
        } finally {
            setHistoryLoading(false);
        }
    }

    function showToast(message, variant) {
        setToastMessage(message);
        setToastVariant(variant);
    }

    function getFieldLabel(fieldName) {
        const key = {
            name: 'fieldName',
            description: 'fieldDescription',
            price: 'fieldPrice',
            quantity: 'fieldQuantity'
        }[fieldName] || fieldName;
        return t(key);
    }

    return (
        <>
            <nav className="navbar navbar-expand-lg navbar-dark bg-dark border-bottom border-primary border-3">
                <div className="container-fluid py-2">
                    <span className="navbar-brand fw-semibold">
                        <i className="bi bi-clock-history text-primary me-1"></i>{t('appTitle')}
                    </span>
                    <div className="ms-auto d-flex align-items-center gap-2 flex-wrap">
                        <div className="btn-group btn-group-sm language-switcher" role="group" aria-label={t('language')}>
                            {availableLanguages.map(language => (
                                <button
                                    key={language}
                                    type="button"
                                    className={`btn btn-outline-light ${lang === language ? 'active' : ''}`}
                                    onClick={() => setLang(language)}
                                >
                                    {language === 'en' ? t('english') : t('french')}
                                </button>
                            ))}
                        </div>
                        <a className="nav-link text-light" href="/swagger-ui/index.html" target="_blank">
                            <i className="bi bi-file-earmark-code me-1"></i>{t('apiDocs')}
                        </a>
                        {user?.roles?.includes('ROLE_ACTUATOR_ADMIN') ? (
                            <a className="nav-link text-light" href="actuator.html" target="_blank">
                                <i className="bi bi-activity me-1"></i>{t('actuator')}
                            </a>
                        ) : null}
                        <a className="nav-link text-light" href="/h2-console" target="_blank">
                            <i className="bi bi-database me-1"></i>{t('h2Console')}
                        </a>
                        {user ? <span className="navbar-text text-light-emphasis small">
                            <i className="bi bi-person-circle me-1"></i>{user.username}
                        </span> : null}
                        <a className="nav-link text-light" href="/logout">
                            <i className="bi bi-box-arrow-right me-1"></i>{t('signOut')}
                        </a>
                    </div>
                </div>
            </nav>

            <main className="container-fluid py-4 px-4">
                <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                    <div>
                        <h4 className="mb-0">{t('products')}</h4>
                        <small className="text-muted">{t('appSubtitle')}</small>
                    </div>
                    <button className="btn btn-primary" onClick={() => openProductModal(null)}>
                        <i className="bi bi-plus-lg me-1"></i>{t('addProduct')}
                    </button>
                </div>

                <div className="card card-surface shadow-sm">
                    <div className="card-body p-0">
                        <div className="table-responsive">
                            <table className="table table-hover align-middle mb-0">
                                <thead className="table-light">
                                    <tr>
                                        <th style={{ width: '60px' }}>{t('tableId')}</th>
                                        <th>{t('tableName')}</th>
                                        <th>{t('tableDescription')}</th>
                                        <th className="text-end" style={{ width: '120px' }}>{t('tablePrice')}</th>
                                        <th className="text-end" style={{ width: '90px' }}>{t('tableQty')}</th>
                                        <th>{t('tableAddedBy')}</th>
                                        <th className="text-end" style={{ width: '190px' }}>{t('tableActions')}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {loading ? (
                                        <tr><td colSpan="7" className="text-center text-muted py-4">{t('loading')}</td></tr>
                                    ) : error ? (
                                        <tr><td colSpan="7" className="text-center text-danger py-4">{t('loadError')} {error}</td></tr>
                                    ) : products.length === 0 ? (
                                        <tr><td colSpan="7" className="text-center text-muted py-4">{t('noProducts')}</td></tr>
                                    ) : products.map(product => (
                                        <tr key={product.id}>
                                            <td className="text-muted">{product.id}</td>
                                                <td className="fw-medium">{product.name}</td>
                                            <td className="text-muted">{product.description || '—'}</td>
                                            <td className="text-end">{currencyFormatter.format(Number(product.price || 0))}</td>
                                            <td className="text-end">{product.quantity}</td>
                                            <td>{product.addedBy?.name || '—'}</td>
                                            <td className="text-end">
                                                <div className="btn-group btn-group-sm">
                                                    <button className="btn btn-outline-secondary" title={t('history')} onClick={() => openHistoryModal(product)}>
                                                        <i className="bi bi-clock-history"></i>
                                                    </button>
                                                    <button className="btn btn-outline-secondary" title={t('edit')} onClick={() => openProductModal(product)}>
                                                        <i className="bi bi-pencil"></i>
                                                    </button>
                                                    <button className="btn btn-outline-danger" title={t('delete')} onClick={() => handleDelete(product.id)}>
                                                        <i className="bi bi-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        <button command="show-modal" commandfor="my-dialog">Open dialog</button>

<dialog id="my-dialog">
  <p>This dialog was opened using an invoker command.</p>
  <button commandfor="my-dialog" command="close">Close</button>
</dialog>
                        </div>
                    </div>
                </div>
            </main>

            <div className="modal fade" ref={productModalRef} tabIndex="-1" aria-hidden="true">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <form onSubmit={handleSubmit}>
                            <div className="modal-header">
                                <h5 className="modal-title">{editingProductId ? t('modalEditTitle') : t('modalCreateTitle')}</h5>
                                <button type="button" className="btn-close" onClick={closeProductModal}></button>
                            </div>
                            <div className="modal-body">
                                <div className="mb-3">
                                    <label className="form-label" htmlFor="productName">{t('modalName')}</label>
                                    <input id="productName" className="form-control" name="name" value={productForm.name} onChange={evt => setProductForm({ ...productForm, name: evt.target.value })} required maxLength="255" />
                                </div>
                                <div className="mb-3">
                                    <label className="form-label" htmlFor="productDescription">{t('modalDescription')}</label>
                                    <textarea id="productDescription" className="form-control" name="description" value={productForm.description} onChange={evt => setProductForm({ ...productForm, description: evt.target.value })} rows="2" maxLength="500"></textarea>
                                </div>
                                <div className="row">
                                    <div className="col mb-3">
                                        <label className="form-label" htmlFor="productPrice">{t('modalPrice')}</label>
                                        <div className="input-group">
                                            <span className="input-group-text">$</span>
                                            <input id="productPrice" type="number" className="form-control" name="price" value={productForm.price} onChange={evt => setProductForm({ ...productForm, price: evt.target.value })} required min="0" step="0.01" />
                                        </div>
                                    </div>
                                    <div className="col mb-3">
                                        <label className="form-label" htmlFor="productQuantity">{t('modalQty')}</label>
                                        <input id="productQuantity" type="number" className="form-control" name="quantity" value={productForm.quantity} onChange={evt => setProductForm({ ...productForm, quantity: evt.target.value })} required min="0" step="1" />
                                    </div>
                                </div>
                                {formError ? <div className="alert alert-danger py-2 mb-0">{formError}</div> : null}
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-outline-secondary" onClick={closeProductModal}>{t('cancel')}</button>
                                <button type="submit" className="btn btn-primary">
                                    <i className="bi bi-check-lg me-1"></i>{t('save')}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <div className="modal fade" ref={historyModalRef} tabIndex="-1" aria-hidden="true">
                <div className="modal-dialog modal-lg">
                    <div className="modal-content">
                        <div className="modal-header">
                            <h5 className="modal-title">
                                <i className="bi bi-clock-history me-1"></i>{t('historyTitle')} — {historyProduct?.name || `#${historyProduct?.id || ''}`}
                            </h5>
                            <button type="button" className="btn-close" onClick={() => modalControllers.current.historyModal?.hide()}></button>
                        </div>
                        <div className="modal-body">
                            {historyLoading ? (
                                <div className="text-center text-muted py-3">{t('historyLoading')}</div>
                            ) : historyError ? (
                                <div className="text-center text-danger py-3">{t('historyError')} {historyError}</div>
                            ) : historyRevisions.length === 0 ? (
                                <div className="text-center text-muted py-3">{t('historyEmpty')}</div>
                            ) : historyRevisions.map(rev => {
                                const badgeClass = rev.revisionType === 'ADD' ? 'text-bg-success' : rev.revisionType === 'DEL' ? 'text-bg-danger' : 'text-bg-primary';
                                const changeText = rev.revisionType === 'ADD' ? t('revisionCreated') : rev.revisionType === 'DEL' ? t('revisionDeleted') : t('revisionModified');
                                return (
                                    <div className="history-card" key={rev.revisionNumber}>
                                        <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                                            <div>
                                                <span className={`badge ${badgeClass}`}>{rev.revisionType}</span>
                                                <span className="ms-2 fw-semibold">{t('revisionLabel')} {rev.revisionNumber}</span>
                                            </div>
                                            <div className="text-muted small text-end">
                                                <div>{dateFormatter.format(new Date(rev.revisionTimestamp))}</div>
                                                <div><i className="bi bi-person me-1"></i>{rev.username || t('system')}</div>
                                            </div>
                                        </div>
                                        <div className="small text-muted mb-2">{changeText}</div>
                                        <ul className="mb-0">
                                            {(rev.changes || []).length === 0 ? <li>{t('historyEmpty')}</li> : rev.changes.map(change => (
                                                <li key={`${rev.revisionNumber}-${change.field}`}><strong>{getFieldLabel(change.field)}</strong>: {change.fromValue ?? '—'} → {change.toValue ?? '—'}</li>
                                            ))}
                                        </ul>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>
            </div>

            <div className={`toast-container position-fixed bottom-0 end-0 p-3 ${toastMessage ? 'd-block' : ''}`}>
                <div ref={toastRef} className={`toast align-items-center border-0 text-bg-${toastVariant === 'danger' ? 'danger' : 'success'}`} role="alert">
                    <div className="d-flex">
                        <div className="toast-body">{toastMessage}</div>
                        <button type="button" className="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                    </div>
                </div>
            </div>
        </>
    );
}

async function apiGet(url) {
    const res = await fetch(url);
    if (!res.ok) {
        throw new Error(await extractErrorMessage(res));
    }
    return res.json();
}

async function apiSend(url, method, body) {
    const res = await fetch(url, {
        method,
        headers: body ? { 'Content-Type': 'application/json' } : undefined,
        body: body ? JSON.stringify(body) : undefined,
    });
    if (!res.ok) {
        throw new Error(await extractErrorMessage(res));
    }
    return res.status === 204 ? null : res.json().catch(() => null);
}

async function extractErrorMessage(res) {
    try {
        const text = await res.text();
        return text || `HTTP ${res.status}`;
    } catch {
        return `HTTP ${res.status}`;
    }
}

createRoot(document.getElementById('root')).render(<App />);
