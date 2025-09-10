/**
 * =====================================================================================
 * React-Based Gateway Admin Dashboard
 * =====================================================================================
 * 这是一个完全使用 React 重写的、单文件的管理后台前端应用。
 * 它不依赖任何构建工具，直接在浏览器中通过 Babel 进行转译。
 * 
 * 核心架构思想：
 * 1. **组件化**: 尽管所有代码都在一个文件中，但我们通过创建不同的 React 组件，
 *    在逻辑上将“菜单”、“路由管理页面”、“API客户端管理页面”等功能分离开来。
 * 2. **状态提升 (Lifting State Up)**: 将需要在多个组件间共享的状态（如此处的主菜单选项 `activeMenu`），
 *    提升到它们最近的共同父组件（`App` 组件）中进行管理。
 * 3. **单向数据流 (One-Way Data Flow)**: 数据通过 `props` 从父组件单向地流向子组件。
 *    子组件通过调用父组件传递下来的函数（如 `onMenuClick`）来通知父组件更新状态。
 * 4. **Hooks**: 使用 `useState`, `useEffect`, `useCallback` 等 React Hooks 来管理组件的内部状态和生命周期。
 * =====================================================================================
 */

// 从 React 的全局对象中解构出我们需要的核心API
const { useState, useEffect, useCallback, memo } = React;

// --- 1. API 调用层 ---
// 这一部分封装了所有与后端交互的网络请求。它们是纯粹的函数，不包含任何UI逻辑。
const apiClient = {
    // 获取所有路由，支持查询
    getRoutes: (query = '') => axios.get(`/__gateway/admin/routes?query=${query}`),
    // 保存或更新一条路由
    saveRoute: (route) => axios.post('/__gateway/admin/routes', route),
    // 删除一条路由
    deleteRoute: (id) => axios.delete(`/__gateway/admin/routes/${id}`),
    
    // 获取所有API客户端，支持查询
    getClients: (query = '') => axios.get(`/__gateway/admin/api-clients?query=${query}`),
    // 创建一个新的API客户端
    createClient: (description) => axios.post('/__gateway/admin/api-clients', { description }),
    // 更新一个API客户端的状态
    updateClient: (client) => axios.put(`/__gateway/admin/api-clients/${client.id}`, client),
    // 删除一个API客户端
    deleteClient: (id) => axios.delete(`/__gateway/admin/api-clients/${id}`),
};

// --- 2. 路由管理页面组件 ---
function RouteManagement() {
    const [routes, setRoutes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [editingRoute, setEditingRoute] = useState(null); // null | {} | {routeData}

    const fetchRoutes = useCallback(async (query) => {
        setLoading(true);
        setError(null);
        try {
            const response = await apiClient.getRoutes(query);
            setRoutes(response.data);
        } catch (err) {
            setError('Failed to load routes.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchRoutes();
    }, [fetchRoutes]);

    const handleSearch = () => fetchRoutes(searchQuery);
    const handleReset = () => { setSearchQuery(''); fetchRoutes(''); };
    const handleDelete = async (id) => {
        if (window.confirm('Are you sure?')) {
            await apiClient.deleteRoute(id).catch(() => alert('Failed to delete route.'));
            fetchRoutes(searchQuery);
        }
    };
    const handleSave = async (route) => {
        try {
            await apiClient.saveRoute(route);
            setEditingRoute(null);
            fetchRoutes(searchQuery);
        } catch (err) {
            alert('Failed to save route. Check JSON format.');
        }
    };

    if (editingRoute) {
        return <RouteForm route={editingRoute} onSave={handleSave} onCancel={() => setEditingRoute(null)} />;
    }

    return (
        <div>
            <div className="header">
                <h1>Route Management</h1>
                <div style={{ display: 'flex', gap: '10px' }}>
                    <input type="text" value={searchQuery} onChange={e => setSearchQuery(e.target.value)} placeholder="Search by ID or URI..." />
                    <button onClick={handleSearch}>Query</button>
                    <button onClick={handleReset}>Reset</button>
                </div>
                <button className="primary" onClick={() => setEditingRoute({})}>+ Create Route</button>
            </div>
            {loading && <p>Loading...</p>}
            {error && <p style={{ color: 'red' }}>{error}</p>}
            <table>
                <thead><tr><th>ID</th><th>URI</th><th>Order</th><th>Enabled</th><th>Actions</th></tr></thead>
                <tbody>
                    {routes.map(route => (
                        <tr key={route.id}>
                            <td>{route.id}</td>
                            <td>{route.uri}</td>
                            <td>{route.order}</td>
                            <td>{String(route.enabled)}</td>
                            <td>
                                <button onClick={() => setEditingRoute(route)}>Edit</button>
                                <button className="danger" onClick={() => handleDelete(route.id)}>Delete</button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

// --- 3. 路由表单组件 ---
function RouteForm({ route, onSave, onCancel }) {
    const [formData, setFormData] = useState({});

    useEffect(() => {
        setFormData({
            ...route,
            predicatesJson: JSON.stringify(route.predicates || [], null, 2),
            filtersJson: JSON.stringify(route.filters || [], null, 2),
        });
    }, [route]);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    };

    const handleSubmit = () => {
        try {
            const dataToSave = { ...formData, predicates: JSON.parse(formData.predicatesJson), filters: JSON.parse(formData.filtersJson) };
            delete dataToSave.predicatesJson;
            delete dataToSave.filtersJson;
            onSave(dataToSave);
        } catch (e) {
            alert('Invalid JSON in Predicates or Filters.');
        }
    };

    return (
        <div className="form-area">
            <h1>{formData.id ? 'Edit Route' : 'Create Route'}</h1>
            <div><label>Route ID</label><input name="id" value={formData.id || ''} onChange={handleChange} disabled={!!formData.id} /></div>
            <div><label>URI</label><input name="uri" value={formData.uri || ''} onChange={handleChange} /></div>
            <div><label>Order</label><input name="order" type="number" value={formData.order || 0} onChange={handleChange} /></div>
            <div><label><input name="enabled" type="checkbox" checked={!!formData.enabled} onChange={handleChange} /> Enabled</label></div>
            <div><label>Predicate Description</label><textarea name="predicateDescription" value={formData.predicateDescription || ''} onChange={handleChange}></textarea></div>
            <div><label>Predicates (JSON)</label><textarea name="predicatesJson" value={formData.predicatesJson || '[]'} onChange={handleChange} style={{height: '120px'}}></textarea></div>
            <div><label>Filter Description</label><textarea name="filterDescription" value={formData.filterDescription || ''} onChange={handleChange}></textarea></div>
            <div><label>Filters (JSON)</label><textarea name="filtersJson" value={formData.filtersJson || '[]'} onChange={handleChange} style={{height: '120px'}}></textarea></div>
            <div className="form-actions">
                <button className="primary" onClick={handleSubmit}>Save</button>
                <button onClick={onCancel}>Cancel</button>
            </div>
        </div>
    );
}

// --- 4. API客户端管理页面组件 ---
function ApiClientManagement() {
    const [clients, setClients] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [newClientDesc, setNewClientDesc] = useState('');

    const fetchClients = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await apiClient.getClients();
            setClients(response.data);
        } catch (err) {
            setError('Failed to load API clients.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchClients(); }, [fetchClients]);

    const handleCreate = async () => {
        if (!newClientDesc.trim()) return;
        await apiClient.createClient(newClientDesc).catch(() => alert('Failed to create client.'));
        setNewClientDesc('');
        fetchClients();
    };

    const handleDelete = async (id) => {
        if (window.confirm('Are you sure?')) {
            await apiClient.deleteClient(id).catch(() => alert('Failed to delete client.'));
            fetchClients();
        }
    };

    return (
        <div>
            <div className="header"><h1>API Client Management</h1></div>
            <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
                <input type="text" value={newClientDesc} onChange={e => setNewClientDesc(e.target.value)} placeholder="New client description..." style={{ flexGrow: 1 }} />
                <button className="primary" onClick={handleCreate}>+ Create New Client</button>
            </div>
            {loading && <p>Loading...</p>}
            {error && <p style={{ color: 'red' }}>{error}</p>}
            <table>
                <thead><tr><th>ID</th><th>Description</th><th>AppKey</th><th>SecretKey</th><th>Enabled</th><th>Actions</th></tr></thead>
                <tbody>
                    {clients.map(client => (
                        <tr key={client.id}>
                            <td>{client.id}</td>
                            <td>{client.description}</td>
                            <td>{client.appKey}</td>
                            <td>{client.secretKey}</td>
                            <td>{String(client.enabled)}</td>
                            <td><button className="danger" onClick={() => handleDelete(client.id)}>Delete</button></td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

// --- 5. 侧边栏组件 ---
const Sidebar = memo(({ activeMenu, onMenuClick }) => {
    const menuItems = ['Route Management', 'API Clients'];
    return (
        <div className="sidebar">
            <ul className="menu-list">
                {menuItems.map(item => (
                    <li key={item} 
                        className={activeMenu === item ? 'active' : ''} 
                        onClick={() => onMenuClick(item)}>
                        {item}
                    </li>
                ))}
            </ul>
        </div>
    );
});

// --- 6. 应用根组件 ---
function App() {
    const [activeMenu, setActiveMenu] = useState('Route Management');

    return (
        <div className="app-container">
            <Sidebar activeMenu={activeMenu} onMenuClick={setActiveMenu} />
            <div className="main-content">
                <div className="content-wrapper">
                    {activeMenu === 'Route Management' && <RouteManagement />}
                    {activeMenu === 'API Clients' && <ApiClientManagement />}
                </div>
            </div>
        </div>
    );
}

// --- 7. 渲染应用 ---
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
