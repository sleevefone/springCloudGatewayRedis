import { useRoutes } from './composables/useRoutes.js';
import { useViewAndForm } from './composables/useViewAndForm.js';
import { useApiClients } from './composables/useApiClients.js';

window.onload = async function () {
    // 1. Securely get Vue and its functions from the global scope.
    const { createApp, ref, reactive, computed } = Vue;
    const axiosInstance = window.axios;

    // --- Robust Component Loading ---
    const fetchTemplate = async (path) => {
        const response = await fetch(path);
        if (!response.ok) throw new Error(`Failed to fetch template: ${path}`);
        return await response.text();
    };

    const [routeListTemplate, routeFormTemplate, apiClientListTemplate] = await Promise.all([
        fetchTemplate('./components/RouteList.html'),
        fetchTemplate('./components/RouteForm.html'),
        fetchTemplate('./components/ApiClientList.html')
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

    const ApiClientList = {
        template: apiClientListTemplate,
        props: ['clients', 'loading'],
        emits: ['create-client', 'delete-client', 'update-client-status'],
        setup() {
            const newClientDescription = ref('');
            return { newClientDescription };
        }
    };

    // --- Main App Definition (The Orchestrator) ---
    const app = createApp({
        components: {
            RouteList,
            RouteForm,
            ApiClientList
        },
        setup() {
            // --- State for Main Menu Navigation ---
            const activeMenu = ref('routes'); // 'routes' or 'apiClients'

            // 2. Correctly package dependencies for injection.
            const vueDeps = { ref, reactive, computed };

            // 3. Inject ALL dependencies into our composables.
            const routesManager = useRoutes(vueDeps, axiosInstance);
            const viewAndFormManager = useViewAndForm(vueDeps);
            const apiClientsManager = useApiClients(vueDeps, axiosInstance);

            const currentComponent = computed(() => {
                if (activeMenu.value === 'routes') {
                    return viewAndFormManager.currentComponent.value;
                }
                return 'ApiClientList';
            });

            const API_BASE_URL = '/admin/routes';

            // --- Cross-Module Logic ---
            const handleRouteSubmit = async (formData) => {
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
                    routesManager.fetchRoutes();
                } catch (error) {
                    alert('Failed to save route. Check JSON format.');
                    console.error(error);
                }
            };
            
            const handleRouteToggle = async (route) => {
                try {
                    await axios.post(API_BASE_URL, route);
                } catch (error) {
                    alert('Failed to update route status.');
                    route.enabled = !route.enabled;
                    console.error(error);
                }
            };

            // --- Return all state and methods to the template ---
            return {
                activeMenu,
                currentComponent,
                // Explicitly map properties to avoid name collisions
                // Route Management
                routes: routesManager.routes,
                routeLoading: routesManager.loading,
                routeSearchQuery: routesManager.searchQuery,
                handleRouteSearch: routesManager.handleSearch,
                handleRouteReset: routesManager.handleReset,
                handleRouteDelete: routesManager.handleDelete,
                handleRouteToggle,
                // Route Form
                ...viewAndFormManager,
                handleRouteSubmit,
                // API Client Management
                clients: apiClientsManager.clients,
                clientLoading: apiClientsManager.loading,
                handleClientCreate: apiClientsManager.createClient,
                handleClientDelete: apiClientsManager.deleteClient,
                handleClientUpdateStatus: apiClientsManager.updateClientStatus,
            };
        }
    });

    // No longer using ElementPlus, so this is removed.
    // app.use(ElementPlus);

    app.mount('#app');
};
