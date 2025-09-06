import { useRoutes } from './composables/useRoutes.js';
import { useViewAndForm } from './composables/useViewAndForm.js';

window.onload = async function () {
    // 1. Securely get Vue functions from the global scope.
    const { createApp } = Vue;

    // --- Robust Component Loading ---
    const fetchTemplate = async (path) => {
        const response = await fetch(path);
        if (!response.ok) throw new Error(`Failed to fetch template: ${path}`);
        return await response.text();
    };

    const [routeListTemplate, routeFormTemplate] = await Promise.all([
        fetchTemplate('./components/RouteList.html'),
        fetchTemplate('./components/RouteForm.html')
    ]);

    const RouteList = {
        template: routeListTemplate,
        props: ['routes', 'loading', 'searchQuery'],
        emits: ['create-route', 'edit-route', 'delete-route', 'update:searchQuery', 'query', 'reset', 'toggle-enabled'],
    };

    const RouteForm = {
        template: routeFormTemplate,
        props: ['formData', 'title', 'isEditMode'],
        emits: ['save-route', 'cancel', 'add-filter', 'remove-filter'],
    };

    // --- Main App Definition (The Orchestrator) ---
    const app = createApp({
        components: {
            RouteList,
            RouteForm,
        },
        setup() {
            // 2. Inject the dependencies into our composables.
            const routesManager = useRoutes(Vue);
            const viewAndFormManager = useViewAndForm(Vue);

            const API_BASE_URL = '/admin/routes';

            // --- Cross-Module Logic ---
            const handleToggleEnabled = async (route) => {
                try {
                    // The v-model on the switch already updated the route object in the list
                    await axios.post(API_BASE_URL, route);
                    // No alert needed for a simple toggle, to keep the UI clean
                } catch (error) {
                    alert('Failed to update route status.');
                    // Revert the switch state on failure
                    route.enabled = !route.enabled;
                    console.error(error);
                }
            };

            const handleSubmit = async (formData) => {
                try {
                    const payload = {
                        ...formData,
                        predicates: JSON.parse(formData.predicatesJson),
                        filters: formData.filters.map(f => ({ ...f, args: JSON.parse(f.argsJson || '{}') }))
                    };
                    
                    delete payload.predicatesJson;
                    payload.filters.forEach(f => delete f.argsJson);

                    if (!viewAndFormManager.isEditMode.value && !payload.id) delete payload.id;

                    await axios.post(API_BASE_URL, payload);
                    alert(`Route ${viewAndFormManager.isEditMode.value ? 'updated' : 'created'} successfully.`);
                    
                    viewAndFormManager.showListView();
                    routesManager.fetchRoutes(); // Tell the routes module to refresh
                } catch (error) {
                    alert('Failed to save route. Check JSON format.');
                    console.error(error);
                }
            };

            // 4. Return everything to the template
            return {
                ...routesManager,
                ...viewAndFormManager,
                handleSubmit,
                handleToggleEnabled
            };
        }
    });

    // We need to re-enable ElementPlus for the <el-table> and <el-switch>
    app.use(ElementPlus);

    app.mount('#app');
};
