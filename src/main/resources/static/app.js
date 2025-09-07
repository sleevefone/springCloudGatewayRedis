import { useRoutes } from './composables/useRoutes.js';
import { useViewAndForm } from './composables/useViewAndForm.js';
import { useApiClients } from './composables/useApiClients.js';

window.onload = async function () {
    const { createApp, ref, reactive, computed } = Vue;
    const axiosInstance = window.axios;
    // Get ElMessage for modern, non-blocking notifications
    const { ElMessage } = window.ElementPlus;

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
    const ApiClientList = { template: apiClientListTemplate, props: ['clients', 'loading', 'searchQuery'], emits: ['create-client', 'delete-client', 'update-client-status', 'update:searchQuery', 'query', 'reset'], setup: () => ({ newClientDescription: ref('') }) };

    const app = createApp({
        components: { RouteList, RouteForm, ApiClientList },
        setup() {
            const vueDeps = { ref, reactive, computed };
            // Inject ElMessage into the composables that need it
            const routesManager = useRoutes(vueDeps, axiosInstance, ElMessage);
            const viewAndFormManager = useViewAndForm(vueDeps);
            const apiClientsManager = useApiClients(vueDeps, axiosInstance, ElMessage);

            const activeMenu = ref('routes');

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

            const handleRouteSubmit = async (formData) => {
                try {
                    const payload = { ...formData, predicates: JSON.parse(formData.predicatesJson), filters: formData.filters.map(f => ({ ...f, args: JSON.parse(f.argsJson || '{}') })) };
                    delete payload.predicatesJson;
                    payload.filters.forEach(f => delete f.argsJson);
                    if (!viewAndFormManager.isEditMode.value && !payload.id) delete payload.id;
                    await axios.post('/admin/routes', payload);
                    ElMessage.success(`Route ${viewAndFormManager.isEditMode.value ? 'updated' : 'created'} successfully.`);
                    viewAndFormManager.showListView();
                    await routesManager.fetchRoutes();
                } catch (error) {
                    ElMessage.error('Failed to save route. Check JSON format.');
                    console.error(error);
                }
            };

            const handleRouteToggle = async (route) => {
                try {
                    const updatedRoute = { ...route, enabled: !route.enabled };
                    await axios.post('/admin/routes', updatedRoute);
                    ElMessage.success(`Route ${updatedRoute.id} status updated.`);
                    await routesManager.fetchRoutes();
                } catch (error) {
                    ElMessage.error('Failed to update route status.');
                    console.error(error);
                }
            };

            // Initial Load
            selectMenu('routes');

            return {
                activeMenu,
                selectMenu,
                currentComponent,

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
                clientSearchQuery: apiClientsManager.searchQuery,
                handleClientSearch: apiClientsManager.handleSearch,
                handleClientReset: apiClientsManager.handleReset,
                handleClientCreate: apiClientsManager.createClient,
                handleClientDelete: apiClientsManager.deleteClient,
                handleClientUpdateStatus: apiClientsManager.updateClientStatus,
            };
        }
    });

    // Re-enable Element Plus to make ElMessage and ElPopconfirm available globally
    app.use(ElementPlus);
    app.mount('#app');
};
