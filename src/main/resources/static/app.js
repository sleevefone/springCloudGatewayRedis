import { useRoutes } from './composables/useRoutes.js';
import { useViewAndForm } from './composables/useViewAndForm.js';
import { useApiClients } from './composables/useApiClients.js';

window.onload = async function () {
    const { createApp, ref, reactive, computed } = Vue;
    const axiosInstance = window.axios;

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

    const RouteList = { template: routeListTemplate, props: ['routes', 'loading', 'searchQuery'], emits: ['create-route', 'edit-route', 'delete-route', 'update:searchQuery', 'query', 'reset', 'toggle-enabled'] };
    const RouteForm = { template: routeFormTemplate, props: ['formData', 'title', 'isEditMode'], emits: ['save-route', 'cancel', 'add-filter', 'remove-filter'] };
    const ApiClientList = { template: apiClientListTemplate, props: ['clients', 'loading'], emits: ['create-client', 'delete-client', 'update-client-status'], setup: () => ({ newClientDescription: ref('') }) };

    const app = createApp({
        components: { RouteList, RouteForm, ApiClientList },
        setup() {
            const vueDeps = { ref, reactive, computed };
            const routesManager = useRoutes(vueDeps, axiosInstance);
            const viewAndFormManager = useViewAndForm(vueDeps);
            const apiClientsManager = useApiClients(vueDeps, axiosInstance);

            const activeMenu = ref('routes');

            // Centralized navigation logic that fetches data on activation
            const selectMenu = async (menu) => {
                activeMenu.value = menu;
                if (menu === 'routes') {
                    await routesManager.fetchRoutes();
                } else if (menu === 'apiClients') {
                    await apiClientsManager.fetchClients();
                }
            };

            const currentComponent = computed(() => {
                if (activeMenu.value === 'routes') return viewAndFormManager.currentComponent.value;
                if (activeMenu.value === 'apiClients') return 'ApiClientList';
                return null;
            });

            // --- All handlers now force a re-fetch from the backend to ensure UI consistency ---

            const handleRouteSubmit = async (formData) => {
                try {
                    const payload = { ...formData, predicates: JSON.parse(formData.predicatesJson), filters: formData.filters.map(f => ({ ...f, args: JSON.parse(f.argsJson || '{}') })) };
                    delete payload.predicatesJson;
                    payload.filters.forEach(f => delete f.argsJson);
                    if (!viewAndFormManager.isEditMode.value && !payload.id) delete payload.id;
                    await axios.post('/admin/routes', payload);
                    alert(`Route ${viewAndFormManager.isEditMode.value ? 'updated' : 'created'} successfully.`);
                    viewAndFormManager.showListView();
                    await routesManager.fetchRoutes(); // Force refresh
                } catch (error) {
                    alert('Failed to save route. Check JSON format.');
                    console.error(error);
                }
            };

            const handleRouteToggle = async (route) => {
                try {
                    const updatedRoute = { ...route, enabled: !route.enabled };
                    await axios.post('/admin/routes', updatedRoute);
                    await routesManager.fetchRoutes(); // Force refresh
                } catch (error) {
                    alert('Failed to update route status.');
                    console.error(error);
                }
            };

            const handleRouteDelete = async (id) => {
                if (!confirm('Are you sure to delete this route?')) return;
                try {
                    await axios.delete(`/admin/routes/${id}`);
                    alert('Route deleted successfully.');
                    await routesManager.fetchRoutes(); // Force refresh
                } catch (error) {
                    alert('Failed to delete route.');
                    console.error(error);
                }
            };

            const handleClientCreate = async (description) => {
                await apiClientsManager.createClient(description); // The composable already refetches
            };

            const handleClientDelete = async (id) => {
                await apiClientsManager.deleteClient(id); // The composable already refetches
            };

            const handleClientUpdateStatus = async (client) => {
                const originalStatus = client.enabled;
                try {
                    // The API expects the *new* state, so we don't need to toggle it here.
                    await apiClientsManager.updateClientStatus(client);
                } catch (error) {
                    // No need to revert, the re-fetch will get the correct state from the server.
                }
                await apiClientsManager.fetchClients(); // Force refresh
            };

            // Initial Load
            selectMenu('routes');

            return {
                activeMenu, selectMenu, currentComponent,
                // Route Management
                routes: routesManager.routes,
                routeLoading: routesManager.loading,
                routeSearchQuery: routesManager.searchQuery,
                handleRouteSearch: routesManager.handleSearch,
                handleRouteReset: routesManager.handleReset,
                handleRouteDelete,
                handleRouteToggle,
                // Route Form
                ...viewAndFormManager,
                handleRouteSubmit,
                // API Client Management
                clients: apiClientsManager.clients,
                clientLoading: apiClientsManager.loading,
                handleClientCreate,
                handleClientDelete,
                handleClientUpdateStatus,
            };
        }
    });

    app.mount('#app');
};
