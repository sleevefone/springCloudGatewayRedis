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

            const selectMenu = async (menu) => {
                activeMenu.value = menu;
            };

            const currentComponent = computed(() => {
                if (activeMenu.value === 'routes') return viewAndFormManager.currentComponent.value;
                if (activeMenu.value === 'apiClients') return 'ApiClientList';
                return null;
            });

            const handleRouteSubmit = async (formData) => {
                try {
                    const payload = { ...formData, predicates: JSON.parse(formData.predicatesJson), filters: formData.filters.map(f => ({ ...f, args: JSON.parse(f.argsJson || '{}') })) };
                    delete payload.predicatesJson;
                    payload.filters.forEach(f => delete f.argsJson);
                    if (!viewAndFormManager.isEditMode.value && !payload.id) delete payload.id;
                    await axios.post('/admin/routes', payload);
                    alert(`Route ${viewAndFormManager.isEditMode.value ? 'updated' : 'created'} successfully.`);
                    viewAndFormManager.showListView();
                    await routesManager.fetchRoutes();
                } catch (error) {
                    alert('Failed to save route. Check JSON format.');
                    console.error(error);
                }
            };

            const handleRouteToggle = async (route) => {
                try {
                    const updatedRoute = { ...route, enabled: !route.enabled };
                    await axios.post('/admin/routes', updatedRoute);
                    await routesManager.fetchRoutes();
                } catch (error) {
                    alert('Failed to update route status.');
                    console.error(error);
                }
            };

            // Initial Load
            routesManager.fetchRoutes();

            return {
                activeMenu, 
                selectMenu,
                currentComponent,
                
                // **CRITICAL FIX: Explicitly map all properties to avoid name collisions**
                // Route Management
                routes: routesManager.routes,
                routeLoading: routesManager.loading,
                routeSearchQuery: routesManager.searchQuery,
                handleRouteSearch: routesManager.handleSearch,
                handleRouteReset: routesManager.handleReset,
                handleRouteDelete: routesManager.handleDelete,
                handleRouteToggle,
                
                // Route Form
                form: viewAndFormManager.form,
                formTitle: viewAndFormManager.formTitle,
                isEditMode: viewAndFormManager.isEditMode,
                showCreateForm: viewAndFormManager.showCreateForm,
                showEditForm: viewAndFormManager.showEditForm,
                showListView: viewAndFormManager.showListView,
                addFilterToForm: viewAndFormManager.addFilterToForm,
                removeFilterFromForm: viewAndFormManager.removeFilterFromForm,
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

    app.mount('#app');
};
